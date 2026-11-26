package net.prototux.aare.ui.car.obd

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import net.prototux.aare.R
import net.prototux.aare.obdapp.ObdApp
import net.prototux.aare.obdapp.ObdAppSettings
import net.prototux.aare.ui.car.carClickable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** One of the big dials: what it shows, its range and where its red zone starts. */
private class Dial(
    val value: Float?,
    val min: Float,
    val max: Float,
    val red: Float?,
    val label: Int,
    val unit: String,
    val decimals: Int = 0,
    /** The pictogram in the dial, next to the number. */
    val icon: ImageVector = CockpitIcons.gauge,
)

/** The Cockpit's numbers and small labels: Titillium Web (OFL), a technical sans close to the cluster faces. */
internal val CockpitFont = androidx.compose.ui.text.font.FontFamily(
    androidx.compose.ui.text.font.Font(R.font.titillium_web_light, androidx.compose.ui.text.font.FontWeight.Light),
    androidx.compose.ui.text.font.Font(R.font.titillium_web_regular, androidx.compose.ui.text.font.FontWeight.Normal),
    androidx.compose.ui.text.font.Font(R.font.titillium_web_semibold, androidx.compose.ui.text.font.FontWeight.SemiBold),
)

/** The look the Cockpit draws itself in; the other one stays one line away in [CockpitStyle]. */
internal val CockpitLook = CockpitStyle.HYBRID

/**
 * A live app pane is smaller than a head unit's own screen: there the Cockpit shows the glanceable
 * card (three dials, five tiles) instead of its tab bar.
 */
internal fun cockpitIsCompact(width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp) =
    width < 800.dp || height < 420.dp

/** Three looks for the dials: VW (wide arcs, thick needle), Porsche (hairline, long thin needle), hybrid (Porsche's fine ticks, VW's thick needle). */
internal enum class CockpitStyle { VW, PORSCHE, HYBRID }

/** The groups the full page's tiles are split into; the compact card has no groups. */
private enum class Group(val label: Int) {
    ENGINE(R.string.obd_group_engine),
    TEMPS(R.string.obd_group_temps),
    FUEL(R.string.obd_group_fuel),
    GEARBOX(R.string.obd_group_gearbox),
}

/** The dials a car can show, in the order the chevrons walk them. */
private val DIAL_IDS = listOf("coolant", "oil", "intake", "boost", "load", "egt1", "egt2", "fuel", "battery", "rail")

/** What the three dials show until the driver picks something else. */
internal val DefaultDials = listOf("coolant", "boost", "oil")

private fun dialFor(id: String, snap: ObdApp.Snapshot): Dial? {
    val r = snap.readings ?: return null
    return when (id) {
        "coolant" -> r.coolant?.let { Dial(it, 40f, 120f, 110f, R.string.obd_coolant, "°C", icon = CockpitIcons.coolant) }
        "oil" -> r.oil?.let { Dial(it, 40f, 150f, 130f, R.string.obd_oil, "°C", icon = CockpitIcons.oil) }
        "intake" -> r.intakeAir?.let { Dial(it, 0f, 80f, 60f, R.string.obd_intake_air, "°C", icon = CockpitIcons.air) }
        "boost" -> if (snap.showsBoost) Dial(snap.gauge ?: r.boost, -1f, 2f, 1.6f, R.string.obd_boost, "bar", 2, CockpitIcons.turbine) else null
        "load" -> r.load?.let { Dial(it, 0f, 100f, null, R.string.obd_load, "%", icon = CockpitIcons.gauge) }
        "egt1" -> r.egt1?.let { Dial(it, 0f, 900f, 750f, R.string.obd_egt1, "°C", icon = CockpitIcons.flame) }
        "egt2" -> r.egt2?.let { Dial(it, 0f, 900f, 750f, R.string.obd_egt2, "°C", icon = CockpitIcons.flame) }
        "fuel" -> r.fuel?.let { Dial(it, 0f, 100f, null, R.string.obd_fuel, "%", icon = CockpitIcons.fuel) }
        "battery" -> r.battery?.let { Dial(it, 10f, 16f, null, R.string.obd_battery, "V", 1, CockpitIcons.battery) }
        "rail" -> r.railKpa?.let { Dial(it / 100f, 0f, 2000f, null, R.string.obd_rail, "bar", icon = CockpitIcons.injector) }
        else -> null
    }
}

/** The dials this car can show right now, in [DIAL_IDS] order. */
internal fun dialIds(snap: ObdApp.Snapshot): List<String> = DIAL_IDS.filter { dialFor(it, snap) != null }

/**
 * The three dials to draw: the saved ones that the car still reports, then the first of the rest,
 * so a dial never ends up empty.
 */
internal fun pickDialIds(snap: ObdApp.Snapshot, saved: List<String>, count: Int = 3): List<String> {
    val available = dialIds(snap)
    val out = ArrayList<String>()
    for (id in saved) if (id in available && id !in out) out += id
    for (id in available) {
        if (out.size >= count) break
        if (id !in out) out += id
    }
    return out.take(count)
}

/** The next dial for [slot]: the next one the car reports that the other slots don't already show. */
internal fun cycleDial(snap: ObdApp.Snapshot, current: List<String>, slot: Int): List<String> {
    val available = dialIds(snap)
    if (available.isEmpty()) return current
    val used = current.filterIndexed { i, _ -> i != slot }.toSet()
    val from = available.indexOf(current.getOrNull(slot).orEmpty())
    val next = (1..available.size).map { available[(from + it).mod(available.size)] }.firstOrNull { it !in used }
        ?: return current
    return current.toMutableList().also { if (slot in it.indices) it[slot] = next else it += next }
}

/**
 * The compact card for a live app pane: three native-style dials over five main tiles. The same
 * set as the full page, only the numbers a driver glances at: speed, revs, gear, consumption,
 * fuel. Drawn for a 640 x 360 dp pane and scaled to whatever pane it gets.
 */
@Composable
internal fun CockpitCard(
    snap: ObdApp.Snapshot,
    modifier: Modifier = Modifier,
    style: CockpitStyle = CockpitStyle.VW,
    slots: List<String> = DefaultDials,
    onCycle: ((Int) -> Unit)? = null,
    tiles: List<String> = DefaultCardTiles,
) {
    val r = snap.readings ?: return
    CompositionLocalProvider(LocalObdScale provides 1f) {
        Column(modifier.fillMaxSize().background(cockpitBackdrop()), verticalArrangement = Arrangement.spacedBy(sdp(6f))) {
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(sdp(4f)), verticalAlignment = Alignment.CenterVertically) {
                DialRow(snap, slots, style, onCycle)
            }
            Row(Modifier.fillMaxWidth().weight(0.55f), horizontalArrangement = Arrangement.spacedBy(sdp(6f))) {
                val picked = pickCardTiles(snap, tiles)
                picked.forEach { id ->
                    val t = cardTile(id, snap) ?: return@forEach
                    CompactTile(
                        stringResource(t.label),
                        t.value,
                        if (t.unit != 0) stringResource(t.unit) else "",
                        style,
                        Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                repeat((ObdAppSettings.CARD_TILES - picked.size).coerceAtLeast(0)) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/** A flat panel: no card fill, just a hairline outline, so the numbers carry the tile. */
@Composable
private fun Modifier.minimalPanel(shape: androidx.compose.ui.graphics.Shape): Modifier =
    clip(shape)
        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.035f))
        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), shape)

/** Pure white on a dark theme, black on a light one: the numbers stay the brightest thing in the tile. */
@Composable
private fun valueColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color.White else Color.Black

/** A tile of the full page: the VW look ([Stat]) or the Porsche one (a small upper-case label over a bright number). */
@Composable
private fun ValueTile(label: String, value: String, unit: String, style: CockpitStyle, alert: Boolean, modifier: Modifier) {
    val weight = if (style == CockpitStyle.VW) FontWeight.SemiBold else FontWeight.Medium
    BoxWithConstraints(
        modifier.minimalPanel(MaterialTheme.shapes.extraSmall).padding(horizontal = sdp(10f), vertical = sdp(4f)),
        contentAlignment = Alignment.Center,
    ) {
        val hasUnit = unit.isNotEmpty() && value != DASH
        val small = minOf(maxHeight * 0.15f, sdp(15f)).coerceAtLeast(sdp(8f))
        val big = minOf(maxHeight * (if (hasUnit) 0.40f else 0.52f), maxWidth / (value.length * 0.58f + 0.6f), sdp(60f))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                label.uppercase(),
                fontSize = dpText(small * 0.9f),
                lineHeight = dpText(small * 0.9f),
                fontFamily = CockpitFont,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                value,
                fontSize = dpText(big),
                lineHeight = dpText(big),
                fontFamily = CockpitFont,
                fontWeight = weight,
                maxLines = 1,
                color = if (alert) ObdRed else valueColor(),
            )
            if (hasUnit) {
                Text(
                    unit,
                    fontSize = dpText(small * 0.9f),
                    lineHeight = dpText(small * 0.9f),
                    fontFamily = CockpitFont,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The values the compact card's five tiles can show, in the picker's order. */
internal val CardTileIds = listOf("speed", "rpm", "gear", "consumption", "fuel", "boost", "load", "coolant", "oil", "intake", "egt1", "egt2", "rail", "battery")

/** What the card shows until the driver picks a set in the settings. */
internal val DefaultCardTiles = listOf("speed", "rpm", "gear", "consumption", "fuel")

private class TileText(val label: Int, val value: String, val unit: Int = 0)

internal fun cardTileLabel(id: String): Int = when (id) {
    "speed" -> R.string.obd_speed
    "rpm" -> R.string.obd_rpm
    "gear" -> R.string.obd_gear
    "consumption" -> R.string.obd_fuel_use
    "fuel" -> R.string.obd_fuel
    "boost" -> R.string.obd_boost
    "load" -> R.string.obd_load
    "coolant" -> R.string.obd_coolant
    "oil" -> R.string.obd_oil
    "intake" -> R.string.obd_intake_air
    "egt1" -> R.string.obd_egt1
    "egt2" -> R.string.obd_egt2
    "rail" -> R.string.obd_rail
    "battery" -> R.string.obd_battery
    else -> R.string.obd_speed
}

private fun cardTile(id: String, snap: ObdApp.Snapshot): TileText? {
    val r = snap.readings ?: return null
    val kmpl = R.string.obd_unit_kmpl
    return when (id) {
        "speed" -> r.speed?.let { TileText(R.string.obd_speed, "%.0f".format(it), R.string.obd_kmh) }
        "rpm" -> r.rpm?.let { TileText(R.string.obd_rpm, "%.0f".format(it), R.string.obd_rpm_unit) }
        "gear" -> snap.gear?.let { TileText(R.string.obd_gear, "$it") }
        "consumption" -> snap.kmPerLiter?.let { TileText(R.string.obd_fuel_use, "%.1f".format(it), kmpl) }
        "fuel" -> r.fuel?.let { TileText(R.string.obd_fuel, "%.0f".format(it), R.string.obd_percent) }
        "boost" -> if (snap.showsBoost) (snap.gauge ?: r.boost)?.let { TileText(R.string.obd_boost, "%+.2f".format(it), R.string.obd_bar) } else null
        "load" -> if (!snap.showsBoost) (snap.gauge ?: r.load)?.let { TileText(R.string.obd_load, "%.0f".format(it), R.string.obd_percent) } else null
        "coolant" -> r.coolant?.let { TileText(R.string.obd_coolant, "%.0f".format(it), R.string.obd_deg_c) }
        "oil" -> r.oil?.let { TileText(R.string.obd_oil, "%.0f".format(it), R.string.obd_deg_c) }
        "intake" -> r.intakeAir?.let { TileText(R.string.obd_intake_air, "%.0f".format(it), R.string.obd_deg_c) }
        "egt1" -> r.egt1?.let { TileText(R.string.obd_egt1, "%.0f".format(it), R.string.obd_deg_c) }
        "egt2" -> r.egt2?.let { TileText(R.string.obd_egt2, "%.0f".format(it), R.string.obd_deg_c) }
        "rail" -> r.railKpa?.let { TileText(R.string.obd_rail, "%.0f".format(it / 100f), R.string.obd_bar) }
        "battery" -> r.battery?.let { TileText(R.string.obd_battery, "%.1f".format(it), R.string.obd_volt) }
        else -> null
    }
}

/** The card tiles this car can show right now. */
internal fun availableCardTiles(snap: ObdApp.Snapshot): List<String> = CardTileIds.filter { cardTile(it, snap) != null }

/** The five tiles to draw: the saved ones the car reports, then the first of the rest. */
internal fun pickCardTiles(snap: ObdApp.Snapshot, saved: List<String>, count: Int = ObdAppSettings.CARD_TILES): List<String> {
    val available = availableCardTiles(snap)
    val out = ArrayList<String>()
    for (id in saved) if (id in available && id !in out) out += id
    for (id in available) {
        if (out.size >= count) break
        if (id !in out) out += id
    }
    return out.take(count)
}

/** The three dials, each tapping through the values the car reports; empty slots keep the row even. */
@Composable
private fun RowScope.DialRow(snap: ObdApp.Snapshot, slots: List<String>, style: CockpitStyle, onCycle: ((Int) -> Unit)?) {
    val picks = pickDialIds(snap, slots)
    picks.forEachIndexed { i, id ->
        val d = dialFor(id, snap) ?: return@forEachIndexed
        val m = Modifier.weight(1f).fillMaxHeight()
        VwDial(d, style, if (onCycle == null) m else m.carClickable(MaterialTheme.shapes.large) { onCycle(i) })
    }
    repeat((3 - picks.size).coerceAtLeast(0)) { Box(Modifier.weight(1f)) }
}

/** A compact tile: the label, the number and the unit stacked, so even "20,0 км/л" fits a narrow pane. */
@Composable
private fun CompactTile(label: String, value: String, unit: String, style: CockpitStyle, modifier: Modifier) {
    val porsche = style != CockpitStyle.VW
    val thinNeedle = style == CockpitStyle.PORSCHE
    BoxWithConstraints(
        modifier.minimalPanel(MaterialTheme.shapes.extraSmall).padding(horizontal = sdp(6f), vertical = sdp(2f)),
        contentAlignment = Alignment.Center,
    ) {
        val labelSize = maxHeight * 0.17f
        val unitSize = maxHeight * 0.15f
        val valueSize = minOf(maxHeight * 0.44f, maxWidth / (value.length * 0.62f + 0.4f))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                label.uppercase(),
                fontSize = dpText(labelSize),
                lineHeight = dpText(labelSize),
                fontFamily = CockpitFont,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                value,
                fontSize = dpText(valueSize),
                lineHeight = dpText(valueSize),
                fontFamily = CockpitFont,
                fontWeight = FontWeight.SemiBold,
                color = valueColor(),
                maxLines = 1,
            )
            if (unit.isNotEmpty()) {
                Text(unit, fontSize = dpText(unitSize), lineHeight = dpText(unitSize), fontFamily = CockpitFont, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

/**
 * The Values page: three big dials over the tiles, the tiles split into groups (engine,
 * temperatures, fuel, gearbox) that fit the screen without scrolling. Everything the adapter
 * gives, standard PIDs and the UDS values whose identifiers are known.
 */
@Composable
internal fun ValuesTab(
    snap: ObdApp.Snapshot,
    wide: Boolean,
    initialGroup: Int = 0,
    style: CockpitStyle = CockpitStyle.VW,
    slots: List<String> = DefaultDials,
    onCycle: ((Int) -> Unit)? = null,
    tiles: List<String> = DefaultCardTiles,
) {
    val r = snap.readings ?: return
    var group by remember { mutableStateOf(Group.entries[initialGroup.coerceIn(0, Group.entries.lastIndex)]) }

    val groups = buildMap<Group, MutableList<@Composable (Modifier) -> Unit>> {
        Group.entries.forEach { put(it, ArrayList()) }
    }
    fun tile(g: Group, label: String, value: String, unit: String = "", alert: Boolean = false) {
        groups.getValue(g) += { m -> ValueTile(label, value, unit, style, alert, m) }
    }
    val rpmText = stringResource(R.string.obd_rpm)
    val speedText = stringResource(R.string.obd_speed)
    val gearText = stringResource(R.string.obd_gear)
    val loadText = stringResource(R.string.obd_load)
    val throttleText = stringResource(R.string.obd_throttle)
    val pedalText = stringResource(R.string.obd_pedal)
    val manifoldText = stringResource(R.string.obd_manifold)
    val baroText = stringResource(R.string.obd_baro)
    val mafText = stringResource(R.string.obd_maf)
    val railText = stringResource(R.string.obd_rail)
    val coolantText = stringResource(R.string.obd_coolant)
    val intakeText = stringResource(R.string.obd_intake_air)
    val outsideText = stringResource(R.string.obd_outside)
    val oilText = stringResource(R.string.obd_oil)
    val egt1Text = stringResource(R.string.obd_egt1)
    val egt2Text = stringResource(R.string.obd_egt2)
    val fuelRateText = stringResource(R.string.obd_fuel_rate)
    val fuelText = stringResource(R.string.obd_fuel)
    val batteryText = stringResource(R.string.obd_battery)
    val useText = stringResource(R.string.obd_fuel_use)
    val kmpl = stringResource(R.string.obd_unit_kmpl)

    r.speed?.let { tile(Group.ENGINE, speedText, "%.0f".format(it), "km/h") }
    r.rpm?.let { tile(Group.ENGINE, rpmText, "%.0f".format(it), "rpm") }
    snap.gear?.let { tile(Group.GEARBOX, gearText, "$it") }
    r.load?.let { tile(Group.ENGINE, loadText, "%.0f".format(it), "%") }
    r.throttle?.let { tile(Group.ENGINE, throttleText, "%.0f".format(it), "%") }
    r.pedal?.let { tile(Group.ENGINE, pedalText, "%.0f".format(it), "%") }
    r.manifoldKpa?.let { tile(Group.ENGINE, manifoldText, "%.0f".format(it), "kPa") }
    r.barometricKpa?.let { tile(Group.ENGINE, baroText, "%.0f".format(it), "kPa") }
    r.maf?.let { tile(Group.ENGINE, mafText, "%.1f".format(it), "g/s") }
    r.railKpa?.let { tile(Group.FUEL, railText, "%.0f".format(it / 100f), "bar") }

    r.coolant?.let { tile(Group.TEMPS, coolantText, "%.0f".format(it), "°C", it >= 110f) }
    r.intakeAir?.let { tile(Group.TEMPS, intakeText, "%.0f".format(it), "°C") }
    r.outside?.let { tile(Group.TEMPS, outsideText, "%.0f".format(it), "°C") }
    r.oil?.let { tile(Group.TEMPS, oilText, "%.0f".format(it), "°C", it >= 130f) }
    r.egt1?.let { tile(Group.TEMPS, egt1Text, "%.0f".format(it), "°C", it >= 750f) }
    r.egt2?.let { tile(Group.TEMPS, egt2Text, "%.0f".format(it), "°C", it >= 750f) }

    r.fuelRate?.let { tile(Group.FUEL, fuelRateText, "%.1f".format(it), "L/h") }
    r.fuel?.let { tile(Group.FUEL, fuelText, "%.0f".format(it), "%", it < 10f) }
    r.battery?.let { tile(Group.ENGINE, batteryText, "%.1f".format(it), "V", it < 11.5f) }
    snap.kmPerLiter?.let { tile(Group.FUEL, useText, "%.1f".format(it), kmpl) }
    // Values the standard PIDs don't carry, read over UDS once the car's identifiers are known.
    r.vag.forEach { (signal, v) ->
        val label = stringResource(signal.label)
        val unit = signal.unit
        val text = "%.${signal.decimals}f".format(v)
        tile(if (unit == "°C") Group.TEMPS else Group.FUEL, label, text, unit)
    }

    val shown = groups.getValue(group)
    val columns = if (wide) 5 else 3
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(sdp(8f))) {
        Row(
            Modifier.fillMaxWidth().weight(if (wide) 1.7f else 1.5f),
            horizontalArrangement = Arrangement.spacedBy(sdp(10f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DialRow(snap, slots, style, onCycle)
        }
        GroupTabs(group) { group = it }
        if (shown.isEmpty()) {
            Text(stringResource(R.string.obd_vag_none), fontSize = ssp(18f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val rows = (shown.size + columns - 1) / columns
            TileGrid(shown, columns, Modifier.fillMaxWidth().weight(rows.toFloat()))
        }
    }
}

/** The page's groups as the MIB bar draws tabs: thin text, hairline separators, an accent underline. */
@Composable
private fun GroupTabs(current: Group, onPick: (Group) -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val line = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(line))
        Row(Modifier.fillMaxWidth().height(sdp(34f))) {
            Group.entries.forEachIndexed { i, g ->
                if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight(0.5f).align(Alignment.CenterVertically).background(line))
                Box(
                    Modifier.weight(1f).fillMaxHeight().carClickable(androidx.compose.ui.graphics.RectangleShape) { onPick(g) },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(g.label),
                            fontSize = ssp(15f),
                            letterSpacing = 0.4.sp,
                            color = if (g == current) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                        Box(
                            Modifier.padding(top = sdp(2f)).width(sdp(24f)).height(2.dp)
                                .background(if (g == current) accent else Color.Transparent),
                        )
                    }
                }
            }
        }
    }
}

/**
 * A dial like the car's own: a rim, a ring of ticks with the red zone over it, a needle from the
 * middle and the number on a dark window in the centre. [CockpitStyle.PORSCHE] is the thinner,
 * flatter take of a Porsche cluster: a hairline rim, fine ticks, no wide arcs, a long thin needle.
 */
@Composable
private fun VwDial(d: Dial, style: CockpitStyle, modifier: Modifier) {
    val porsche = style != CockpitStyle.VW
    val thinNeedle = style == CockpitStyle.PORSCHE
    val rim = MaterialTheme.colorScheme.onSurface.copy(alpha = if (porsche) 0.30f else 0.55f)
    val tick = MaterialTheme.colorScheme.onSurfaceVariant
    val needle = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.onSurface
    val red = ObdRed
    val window = MaterialTheme.colorScheme.background
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val dia = minOf(maxWidth, (maxHeight - sdp(22f)).coerceAtLeast(sdp(36f)))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(dia).aspectRatio(1f), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f
                    val c = Offset(size.width / 2f, size.height / 2f)
                    // Open at the bottom, like the car's dials: 270° from the lower left.
                    val start = 135f
                    val sweep = 270f
                    fun angle(v: Float) = start + sweep * ((v - d.min) / (d.max - d.min)).coerceIn(0f, 1f)
                    fun at(len: Float, a: Float) = Offset(
                        c.x + len * cos(a * PI.toFloat() / 180f),
                        c.y + len * sin(a * PI.toFloat() / 180f),
                    )
                    val trackTop = Offset(c.x - radius * 0.95f, c.y - radius * 0.95f)
                    val trackSize = Size(radius * 1.90f, radius * 1.90f)
                    if (porsche) {
                        // A hairline rim, fine ticks, one thin arc and a long thin needle.
                        // The MIB rim: one bright outer ring, nothing inside it to fight the
                        // pictogram at the dial's bottom.
                        drawCircle(rim.copy(alpha = 0.9f), radius - radius * 0.014f, c, style = Stroke(radius * 0.016f))
                        for (i in 0..60) {
                            val major = i % 10 == 0
                            val a = start + sweep * i / 60f
                            drawLine(
                                tick.copy(alpha = if (major) 0.85f else 0.35f),
                                at(radius * (if (major) 0.80f else 0.855f), a),
                                at(radius * 0.925f, a),
                                strokeWidth = if (major) radius * 0.014f else radius * 0.007f,
                                cap = StrokeCap.Round,
                            )
                        }
                        d.red?.let { drawArc(red.copy(alpha = 0.9f), angle(it), start + sweep - angle(it), false, trackTop, trackSize, style = Stroke(radius * if (thinNeedle) 0.018f else 0.03f)) }
                        d.value?.let {
                            val a = angle(it)
                            drawArc(accent.copy(alpha = 0.85f), start, a - start, false, trackTop, trackSize, style = Stroke(radius * 0.018f, cap = StrokeCap.Round))
                            drawLine(
                                needle,
                                at(radius * if (thinNeedle) 0.12f else 0.16f, a + 180f),
                                at(radius * 0.86f, a),
                                strokeWidth = radius * if (thinNeedle) 0.011f else 0.028f,
                                cap = StrokeCap.Round,
                            )
                        }
                        drawCircle(window, radius * 0.42f, c)
                    } else {
                        // The VW power display: a bezel, a tick ring, wide arcs and a short needle.
                        drawCircle(rim.copy(alpha = 0.40f), radius - radius * 0.012f, c, style = Stroke(radius * 0.018f))
                        drawCircle(rim.copy(alpha = 0.22f), radius * 0.80f, c, style = Stroke(radius * 0.008f))
                        for (i in 0..50) {
                            val major = i % 5 == 0
                            val a = start + sweep * i / 50f
                            drawLine(
                                tick.copy(alpha = if (major) 0.9f else 0.45f),
                                at(radius * (if (major) 0.76f else 0.80f), a),
                                at(radius * 0.88f, a),
                                strokeWidth = if (major) radius * 0.024f else radius * 0.011f,
                                cap = StrokeCap.Round,
                            )
                        }
                        val rimTop = Offset(c.x - radius * 0.945f, c.y - radius * 0.945f)
                        val rimSize = Size(radius * 1.89f, radius * 1.89f)
                        d.red?.let { drawArc(red.copy(alpha = 0.85f), angle(it), start + sweep - angle(it), false, rimTop, rimSize, style = Stroke(radius * 0.05f)) }
                        d.value?.let {
                            val a = angle(it)
                            drawArc(accent, start, a - start, false, rimTop, rimSize, style = Stroke(radius * 0.05f, cap = StrokeCap.Round))
                            drawLine(needle, c, at(radius * 0.78f, a), strokeWidth = radius * 0.02f, cap = StrokeCap.Round)
                        }
                        drawCircle(window, radius * 0.40f, c)
                        drawCircle(rim.copy(alpha = 0.25f), radius * 0.40f, c, style = Stroke(radius * 0.008f))
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        fmt(d.value, if (d.decimals == 0) "%.0f" else "%.${d.decimals}f"),
                        fontSize = dpText(dia * if (porsche) 0.27f else 0.26f),
                        lineHeight = dpText(dia * if (porsche) 0.27f else 0.26f),
                        fontFamily = CockpitFont,
                        fontWeight = if (porsche) FontWeight.Light else FontWeight.Medium,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        if (porsche) d.unit.uppercase() else d.unit,
                        fontSize = dpText(dia * 0.072f),
                        lineHeight = dpText(dia * 0.072f),
                        fontFamily = CockpitFont,
                        letterSpacing = if (porsche) 0.6.sp else 0.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                // The dial's bottom is open (the scale starts and ends at the sides): the pictogram
                // sits there, under the unit, clear of the ticks and the needle.
                Icon(
                    d.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = dia * 0.085f).size(dia * 0.14f),
                )
            }
            Spacer(Modifier.height(sdp(6f)))
            Text(
                if (porsche) stringResource(d.label).uppercase() else stringResource(d.label),
                fontSize = ssp(if (porsche) 12f else 14f),
                lineHeight = ssp(if (porsche) 12f else 14f),
                fontFamily = CockpitFont,
                letterSpacing = if (porsche) 1.sp else 0.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (porsche) 0.8f else 1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
