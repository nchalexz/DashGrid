package net.prototux.aare.vpm

import net.prototux.aare.R
import java.util.Locale

/** VPM's display categories, the six tabs of its extended screen. */
enum class VpmCategory(val id: String, val labelRes: Int) {
    ENGINE("engine", R.string.vpm_cat_engine),
    TEMPERATURES("temperatures", R.string.vpm_cat_temperatures),
    FUEL("fuel", R.string.vpm_cat_fuel),
    GEARBOX("gearbox", R.string.vpm_cat_gearbox),
    TURBO("turbo", R.string.vpm_cat_turbo),
    DIAGNOSTICS("diagnostics", R.string.vpm_cat_diagnostics);

    companion object {
        fun byId(id: String?): VpmCategory? = entries.firstOrNull { it.id == id?.trim() }
    }
}

/** The pictogram inside a gauge, as VPM's `GaugeIcon`. */
enum class VpmGaugeIcon { NONE, COOLANT, BOOST, OIL }

/**
 * How DashGrid shows one signal of VPM's main screen: its localized label and unit, the scale of a
 * gauge and VPM's warning thresholds.
 *
 * [raw] values arrive from VPM already in display units (bar, °C, l/100 km), so [factor] is only a
 * compatibility hook for a provider that sends catalog units; it stays 1 for contract v1.
 */
data class VpmMetric(
    val id: String,
    val labelRes: Int,
    val unitRes: Int?,
    val decimals: Int,
    val category: VpmCategory,
    val factor: Double = 1.0,
    val scaleMin: Double = 0.0,
    val scaleMax: Double = 100.0,
    val warnFrom: Double? = null,
    val criticalFrom: Double? = null,
    val icon: VpmGaugeIcon = VpmGaugeIcon.NONE,
    /** Other signals that carry the same value when the primary one is silent (VAG blocks). */
    val alternatives: List<String> = emptyList(),
) {
    val allIds: List<String> get() = listOf(id) + alternatives

    fun display(raw: Double, factorOverride: Double = factor): Double = raw * factorOverride

    fun fraction(displayValue: Double?): Float? {
        if (displayValue == null || scaleMax <= scaleMin) return null
        return ((displayValue - scaleMin) / (scaleMax - scaleMin)).coerceIn(0.0, 1.0).toFloat()
    }

    fun warning(displayValue: Double?): Boolean =
        displayValue != null && warnFrom != null && displayValue >= warnFrom

    fun critical(displayValue: Double?): Boolean =
        displayValue != null && criticalFrom != null && displayValue >= criticalFrom
}

/** Why a reading has no number. Kept apart so "no data" never looks like a zero. */
enum class VpmReadingState { OK, WAITING, STALE, OFFLINE }

/** One reading of a main-screen metric, drawn by a gauge or a tile. */
data class VpmReading(
    val metric: VpmMetric,
    val text: String,
    val display: Double?,
    val state: VpmReadingState,
) {
    val isOk: Boolean get() = state == VpmReadingState.OK

    /** A number VPM still holds, just not refreshed: drawn dimmed, never replaced by a dash. */
    val isStale: Boolean get() = state == VpmReadingState.STALE

    /** Stale numbers keep their needle position, faded: the last known value is still readable. */
    val fraction: Float? get() = if (isOk || isStale) metric.fraction(display) else null

    val warning: Boolean get() = isOk && metric.warning(display)

    val critical: Boolean get() = isOk && metric.critical(display)
}

/** VPM's number style: a comma decimal and no locale surprises, as its own cockpit draws it. */
fun formatVpmNumber(value: Double, decimals: Int): String =
    String.format(Locale.ROOT, "%.${decimals}f", value).replace('.', ',')

/**
 * Reads [metric] from [snapshot]. Missing data is never a zero: a silent car shows "—" (waiting),
 * a never-read signal shows "—" (waiting), and a snapshot that can't be drawn (VPM disconnected)
 * shows the same with "no link".
 *
 * A stale signal keeps its number, as in VPM 0.6.2: the last known value is drawn dimmed with a
 * stale marker, because a dash would mean "VPM never read this", which is a different fact.
 * [VpmSignalStatus.NEVER_READ] and a missing value are the only reasons for a dash.
 */
fun readVpmMetric(metric: VpmMetric, snapshot: VpmSnapshot?): VpmReading {
    val offline = snapshot == null || !snapshot.live
    val dash = VpmReading(metric, "—", null, if (offline) VpmReadingState.OFFLINE else VpmReadingState.WAITING)
    if (snapshot == null) return dash
    val signal = snapshot.signalFor(metric) ?: return dash
    if (signal.status == VpmSignalStatus.NEVER_READ) return dash
    val display = signal.raw?.let { metric.display(it) }
    val text = signal.value ?: display?.let { formatVpmNumber(it, metric.decimals) } ?: return dash
    // A stale number survives a dropped link: VPM keeps the last value and says so with
    // status=stale, so the cockpit can show it dimmed instead of blanking the whole screen.
    // A "valid" number without a live link is not drawn: it is too old to pass as current.
    val state = when {
        signal.status == VpmSignalStatus.STALE -> VpmReadingState.STALE
        snapshot.live -> VpmReadingState.OK
        else -> return dash
    }
    return VpmReading(metric, text, display, state)
}

/**
 * The occurrence of a signal that belongs to the metric's own category.
 *
 * Contract v1 may carry one id in two tabs with different display units: `engine.map_kpa` is kPa
 * under Engine and bar under Turbo, and the Turbo occurrence is the one the absolute-boost gauge
 * needs. The first occurrence is the fallback for a provider that sends only one.
 */
private fun VpmSnapshot.signalFor(metric: VpmMetric): VpmSignal? {
    val candidates = signals.filter { it.id in metric.allIds }
    return candidates.firstOrNull { vpmSignalCategory(it) == metric.category } ?: candidates.firstOrNull()
}

/** The metric DashGrid knows by id, including VPM's alternative signal ids. */
object VpmMetrics {
    val coolant = VpmMetric(
        id = "engine.coolant_c",
        labelRes = R.string.vpm_metric_coolant,
        unitRes = R.string.vpm_unit_c,
        decimals = 0,
        category = VpmCategory.TEMPERATURES,
        scaleMin = 40.0,
        scaleMax = 120.0,
        warnFrom = 105.0,
        criticalFrom = 115.0,
        icon = VpmGaugeIcon.COOLANT,
    )

    /**
     * VPM 0.6.2's middle gauge: absolute intake pressure (MAP), the number the car's own console
     * shows. VPM's `raw` is already bar (its MetricSpec applies the kPa → bar factor of 0.01 before
     * the bridge), so [factor] stays 1 and applying 0.01 again would under-read by a hundred.
     */
    val boostAbs = VpmMetric(
        id = "engine.map_kpa",
        labelRes = R.string.vpm_metric_boost_abs,
        unitRes = R.string.vpm_unit_bar,
        decimals = 2,
        category = VpmCategory.TURBO,
        scaleMin = 0.0,
        scaleMax = 2.5,
        warnFrom = 2.0,
        criticalFrom = 2.3,
        icon = VpmGaugeIcon.BOOST,
    )

    /** Relative boost, still a Turbo tile: VPM's 0.6.2 main screen no longer draws it as a gauge. */
    val boost = VpmMetric(
        id = "engine.boost_kpa",
        labelRes = R.string.vpm_metric_boost,
        unitRes = R.string.vpm_unit_bar,
        decimals = 2,
        category = VpmCategory.TURBO,
        scaleMin = -1.0,
        scaleMax = 2.0,
        warnFrom = 1.75,
        criticalFrom = 1.9,
        icon = VpmGaugeIcon.BOOST,
    )

    val oil = VpmMetric(
        id = "engine.oil_c",
        labelRes = R.string.vpm_metric_oil,
        unitRes = R.string.vpm_unit_c,
        decimals = 0,
        category = VpmCategory.TEMPERATURES,
        scaleMin = 40.0,
        scaleMax = 150.0,
        warnFrom = 130.0,
        criticalFrom = 145.0,
        icon = VpmGaugeIcon.OIL,
        // A diesel often doesn't answer PID 0x5C; VPM reads the oil temperature from these blocks.
        alternatives = listOf("vag.7e0.11be.oil_temp", "vag.714.202f.oil_temp"),
    )

    val speed = VpmMetric(
        id = "vehicle.speed",
        labelRes = R.string.vpm_metric_speed,
        unitRes = R.string.vpm_unit_kmh,
        decimals = 0,
        category = VpmCategory.ENGINE,
    )

    val rpm = VpmMetric(
        id = "engine.rpm",
        labelRes = R.string.vpm_metric_rpm,
        unitRes = R.string.vpm_unit_rpm,
        decimals = 0,
        category = VpmCategory.ENGINE,
    )

    val gear = VpmMetric(
        id = "vehicle.gear",
        labelRes = R.string.vpm_metric_gear,
        unitRes = null,
        decimals = 0,
        category = VpmCategory.GEARBOX,
        alternatives = listOf("vag.7e1.210f.gear"),
    )

    val fuelUse = VpmMetric(
        id = "fuel.l100_km",
        labelRes = R.string.vpm_metric_fuel_use,
        unitRes = R.string.vpm_unit_l100,
        decimals = 1,
        category = VpmCategory.FUEL,
    )

    val fuel = VpmMetric(
        id = "fuel.level_pct",
        labelRes = R.string.vpm_metric_fuel,
        unitRes = R.string.vpm_unit_percent,
        decimals = 0,
        category = VpmCategory.FUEL,
    )

    /** DSG oil temperature from the gearbox block; VPM's first tile. */
    val dsgTemp = VpmMetric(
        id = "vag.7e1.2104.atf_temp",
        labelRes = R.string.vpm_metric_dsg,
        unitRes = R.string.vpm_unit_c,
        decimals = 0,
        category = VpmCategory.GEARBOX,
        scaleMin = 30.0,
        scaleMax = 140.0,
        warnFrom = 110.0,
        criticalFrom = 125.0,
    )

    val intakeAir = VpmMetric(
        id = "engine.intake_air_c",
        labelRes = R.string.vpm_metric_intake,
        unitRes = R.string.vpm_unit_c,
        decimals = 0,
        category = VpmCategory.TEMPERATURES,
        scaleMin = 0.0,
        scaleMax = 100.0,
    )

    val egt = VpmMetric(
        id = "vag.7e0.10fb.egt1",
        labelRes = R.string.vpm_metric_egt,
        unitRes = R.string.vpm_unit_c,
        decimals = 0,
        category = VpmCategory.TEMPERATURES,
        scaleMin = 0.0,
        scaleMax = 900.0,
        warnFrom = 700.0,
        criticalFrom = 850.0,
    )

    val voltage = VpmMetric(
        id = "electrical.voltage_v",
        labelRes = R.string.vpm_metric_voltage,
        unitRes = R.string.vpm_unit_v,
        decimals = 1,
        category = VpmCategory.ENGINE,
        scaleMin = 0.0,
        scaleMax = 20.0,
    )

    /** Rail pressure in bar, again already converted by VPM's 0.01 factor before the bridge. */
    val rail = VpmMetric(
        id = "fuel.rail_kpa",
        labelRes = R.string.vpm_metric_rail,
        unitRes = R.string.vpm_unit_bar,
        decimals = 0,
        category = VpmCategory.FUEL,
        scaleMin = 0.0,
        scaleMax = 2500.0,
    )

    /** Three gauges of the main screen, exactly VPM 0.6.2's set and order. */
    val gauges: List<VpmMetric> = listOf(coolant, boostAbs, oil)

    /** Five tiles of the main screen, exactly VPM 0.6.2's set and order. */
    val mainTiles: List<VpmMetric> = listOf(dsgTemp, intakeAir, egt, voltage, rail)

    private val all: List<VpmMetric> =
        listOf(coolant, boostAbs, boost, oil, speed, rpm, gear, fuelUse, fuel, dsgTemp, intakeAir, egt, voltage, rail)

    /** Ids (primary and alternative) to the metric DashGrid localizes itself. */
    private val byId: Map<String, VpmMetric> = buildMap {
        all.forEach { metric -> metric.allIds.forEach { put(it, metric) } }
    }

    fun of(id: String): VpmMetric? = byId[id]
}

/** The category of a VPM signal: VPM's own, or the known metric's, or Engine as a last resort. */
fun vpmSignalCategory(signal: VpmSignal): VpmCategory =
    VpmCategory.byId(signal.category) ?: VpmMetrics.of(signal.id)?.category ?: VpmCategory.ENGINE

/** The signals of one category, in VPM's own display order (the contract preserves it). */
fun VpmSnapshot.signalsIn(category: VpmCategory): List<VpmSignal> =
    signals.filter { vpmSignalCategory(it) == category }
