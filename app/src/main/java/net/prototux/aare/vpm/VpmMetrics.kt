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

    val fraction: Float? get() = if (isOk) metric.fraction(display) else null

    val warning: Boolean get() = isOk && metric.warning(display)

    val critical: Boolean get() = isOk && metric.critical(display)

}

/** VPM's number style: a comma decimal and no locale surprises, as its own cockpit draws it. */
fun formatVpmNumber(value: Double, decimals: Int): String =
    String.format(Locale.ROOT, "%.${decimals}f", value).replace('.', ',')

/**
 * Reads [metric] from [snapshot]. Missing data is never a zero: a silent car shows "—" (waiting),
 * a stale value shows "—" with a stale state, and a snapshot that can't be drawn (VPM disconnected)
 * shows the same with "no link".
 */
fun readVpmMetric(metric: VpmMetric, snapshot: VpmSnapshot?): VpmReading {
    val dash = VpmReading(metric, "—", null, if (snapshot == null || !snapshot.live) VpmReadingState.OFFLINE else VpmReadingState.WAITING)
    if (snapshot == null || !snapshot.live) return dash
    val signal = metric.allIds.firstNotNullOfOrNull { snapshot.signal(it) } ?: return dash
    val raw = signal.raw
    val display = raw?.let { metric.display(it) }
    val text = signal.value ?: display?.let { formatVpmNumber(it, metric.decimals) } ?: "—"
    if (signal.stale) return VpmReading(metric, "—", display, VpmReadingState.STALE)
    if (display == null && signal.value == null) return dash
    return VpmReading(metric, text, display, VpmReadingState.OK)
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

    val boost = VpmMetric(
        id = "engine.boost_kpa",
        labelRes = R.string.vpm_metric_boost,
        unitRes = R.string.vpm_unit_bar,
        decimals = 2,
        category = VpmCategory.ENGINE,
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

    /** Three gauges of the main screen, exactly VPM's set and order. */
    val gauges: List<VpmMetric> = listOf(coolant, boost, oil)

    /** Five tiles of the main screen, exactly VPM's set and order. */
    val mainTiles: List<VpmMetric> = listOf(speed, rpm, gear, fuelUse, fuel)

    private val all: List<VpmMetric> = listOf(coolant, boost, oil, speed, rpm, gear, fuelUse, fuel)

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
