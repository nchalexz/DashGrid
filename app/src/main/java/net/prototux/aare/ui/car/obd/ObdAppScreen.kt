package net.prototux.aare.ui.car.obd

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import net.prototux.aare.R
import net.prototux.aare.ui.car.carClickable
import net.prototux.aare.obd.ObdReader
import net.prototux.aare.obd.Vag
import net.prototux.aare.obdapp.CardValue
import net.prototux.aare.obdapp.ObdApp
import net.prototux.aare.obdapp.CarProfiles
import net.prototux.aare.obdapp.ObdAppSettings
import net.prototux.aare.obdapp.ObdHistory
import java.text.DateFormat
import java.util.Date

private enum class ObdTab(val label: Int) {
    DASH(R.string.obd_tab_dash), VALUES(R.string.obd_tab_values), TRIP(R.string.obd_tab_trip),
    HISTORY(R.string.obd_tab_history), SETTINGS(R.string.obd_tab_settings),
}

/** The reference app area the sizes are drawn for, in dp. */
private const val REF_WIDTH = 1000f
private const val REF_HEIGHT = 600f

/**
 * The OBD2 app on the car screen: everything the adapter gives, in tabs. Only listed with the
 * OBD2 setting on (see CarRuntime), so a car without an adapter never sees it.
 * Every page fits the screen at once, whatever its size or shape: nothing scrolls.
 */
@Composable
fun ObdAppScreen(modifier: Modifier) {
    val ctx = LocalContext.current
    val settings = remember { ObdApp.settings(ctx) }
    var tab by rememberSaveable { mutableStateOf(ObdTab.DASH) }
    val snap by ObdApp.snapshot.collectAsState()
    var dialSlots by remember { mutableStateOf(settings.dialSlots) }
    val cardTiles = settings.cardTiles.ifEmpty { DefaultCardTiles }
    val cycle: (Int) -> Unit = { i ->
        dialSlots = cycleDial(snap, dialSlots, i)
        settings.dialSlots = dialSlots
    }
    val status by ObdReader.status.collectAsState()
    BoxWithConstraints(modifier.fillMaxSize().background(cockpitBackdrop())) {
        val scale = minOf(maxWidth.value / REF_WIDTH, maxHeight.value / REF_HEIGHT).coerceIn(0.5f, 1.6f)
        val wide = maxWidth > maxHeight * 1.2f
        val compact = cockpitIsCompact(maxWidth, maxHeight)
        // Line height follows each text's own size (the theme's is fixed, which clips the big numbers).
        CompositionLocalProvider(LocalObdScale provides scale, LocalTextStyle provides LocalTextStyle.current.copy(lineHeight = TextUnit.Unspecified)) {
            if (compact) {
                if (snap.readings == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(statusText(status), fontSize = ssp(18f), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                } else {
                    CockpitCard(snap, Modifier.fillMaxSize(), CockpitLook, dialSlots, cycle, cardTiles)
                }
                return@CompositionLocalProvider
            }
            Column(Modifier.fillMaxSize().padding(horizontal = sdp(16f), vertical = sdp(10f)), verticalArrangement = Arrangement.spacedBy(sdp(10f))) {
                if (snap.simulated && snap.readings != null) Note(stringResource(R.string.obd_simulated_badge), maxLines = 1, color = ObdRed)
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    if (snap.readings == null && tab != ObdTab.HISTORY && tab != ObdTab.SETTINGS) {
                        Text(statusText(status), Modifier.align(Alignment.Center), fontSize = ssp(20f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else when (tab) {
                        ObdTab.DASH -> CockpitCard(snap, Modifier.fillMaxSize(), CockpitLook, dialSlots, cycle, cardTiles)
                        ObdTab.VALUES -> ValuesTab(snap, wide, style = CockpitLook, slots = dialSlots, onCycle = cycle)
                        ObdTab.TRIP -> TripTab(snap, wide)
                        ObdTab.HISTORY -> HistoryTab(wide)
                        ObdTab.SETTINGS -> SettingsTab(settings, wide)
                    }
                }
                // The pages sit in a thin bar at the bottom, MIB-style: no chips, hairline
                // separators and an accent underline under the page that's open.
                val rows = if (wide) listOf(ObdTab.entries) else ObdTab.entries.chunked(4)
                MibTabs(rows, tab) { tab = it }
            }
        }
    }
}

/**
 * The Cockpit's backdrop: the theme's colour with a breath of the MIB screens' dark blue at the
 * top on a dark theme (a light theme keeps its plain colour, where a gradient would just look dirty).
 */
@Composable
internal fun cockpitBackdrop(): Brush {
    val bg = MaterialTheme.colorScheme.background
    return if (bg.luminance() < 0.5f) {
        Brush.verticalGradient(listOf(lerp(bg, Color(0xFF16222E), 0.55f), bg))
    } else {
        SolidColor(bg)
    }
}

/** A row of pages as the MIB head unit draws its bar: thin text, hairline separators, an accent underline. */
@Composable
private fun MibTabs(rows: List<List<ObdTab>>, current: ObdTab, onPick: (ObdTab) -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val line = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(line))
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth().height(sdp(34f))) {
                row.forEachIndexed { i, t ->
                    if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight(0.55f).align(Alignment.CenterVertically).background(line))
                    Box(
                        Modifier.weight(1f).fillMaxHeight().carClickable(RectangleShape) { onPick(t) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(t.label),
                                fontSize = ssp(14f),
                                letterSpacing = 0.4.sp,
                                color = if (t == current) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                            Box(
                                Modifier.padding(top = sdp(3f)).width(sdp(26f)).height(2.dp)
                                    .background(if (t == current) accent else Color.Transparent),
                            )
                        }
                    }
                }
                if (row.size < 4) repeat(4 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun statusText(s: ObdReader.Status) = when (s.state) {
    ObdReader.State.OFF, ObdReader.State.CONNECTED -> stringResource(R.string.obd_waiting)
    ObdReader.State.SEARCHING -> stringResource(R.string.obd_status_searching, s.detail)
    ObdReader.State.NO_CAR -> stringResource(R.string.obd_status_no_car, s.adapter)
    ObdReader.State.GAVE_UP -> s.detail.ifBlank { stringResource(R.string.obd_status_gave_up) }
}

// --- Trip ---

@Composable
private fun TripTab(snap: ObdApp.Snapshot, wide: Boolean) {
    val km = stringResource(R.string.obd_unit_kmpl)
    val tiles = listOf<@Composable (Modifier) -> Unit>(
        { m -> Stat(stringResource(R.string.obd_trip_distance), "%.1f".format(snap.tripKm), m, "km") },
        { m -> Stat(stringResource(R.string.obd_trip_time), minutes(snap.tripMinutes), m) },
        { m -> Stat(stringResource(R.string.obd_trip_range), fmt(snap.rangeKm, "%.0f"), m, "km") },
        { m -> Stat(stringResource(R.string.obd_trip_average), fmt(snap.tripAverage, "%.1f"), m, km) },
        { m -> Stat(stringResource(R.string.obd_trip_now), fmt(snap.kmPerLiter, "%.1f"), m, km) },
        { m -> Stat(stringResource(R.string.obd_fuel), fmt(snap.readings?.fuel, "%.0f"), m, "%") },
    )
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(sdp(10f))) {
        TileGrid(tiles, if (wide) 3 else 2, Modifier.fillMaxWidth().weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(sdp(14f))) {
            ObdButton(stringResource(R.string.obd_trip_reset)) { ObdApp.resetTrip() }
            Note(stringResource(R.string.obd_trip_note), Modifier.weight(1f), maxLines = 3)
        }
    }
}

/** h:mm:ss, or m:ss under an hour. */
private fun minutes(m: Float): String {
    val t = (m * 60).toInt()
    return if (t >= 3600) "%d:%02d:%02d".format(t / 3600, t / 60 % 60, t % 60) else "%d:%02d".format(t / 60, t % 60)
}

// --- History ---

@Composable
private fun HistoryTab(wide: Boolean) {
    val version by ObdApp.historyVersion.collectAsState()
    val entries = remember(version) { ObdApp.history?.read().orEmpty() }
    var confirm by remember { mutableStateOf(false) }
    if (entries.isEmpty()) {
        Text(stringResource(R.string.obd_history_empty), fontSize = ssp(18f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val records = remember(entries) { ObdHistory.records(entries) }
    val dates = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(sdp(8f))) {
        if (records.isNotEmpty()) {
            Text(stringResource(R.string.obd_history_records), fontSize = ssp(18f), fontWeight = FontWeight.SemiBold)
            val cols = if (wide) 4 else 2
            val tiles = records.values.map { e -> @Composable { m: Modifier -> Stat(kindName(e.kind), valueText(e), m, footer = dates.format(Date(e.timeMs))) } }
            TileGrid(tiles, cols, Modifier.fillMaxWidth().weight((tiles.size + cols - 1) / cols * 1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.obd_history_recent), fontSize = ssp(18f), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Chip(stringResource(if (confirm) R.string.obd_history_clear_confirm else R.string.obd_history_clear)) {
                if (confirm) { ObdApp.clearHistory(); confirm = false } else confirm = true
            }
        }
        // As many recent lines as the space holds, newest first.
        BoxWithConstraints(Modifier.fillMaxWidth().weight(2f)) {
            val lineHeight = sdp(30f)
            val n = (maxHeight / lineHeight).toInt().coerceAtLeast(1)
            Column {
                entries.asReversed().take(n).forEach { e ->
                    Row(Modifier.fillMaxWidth().height(lineHeight), verticalAlignment = Alignment.CenterVertically) {
                        Text(dates.format(Date(e.timeMs)), fontSize = ssp(16f), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, modifier = Modifier.weight(1f))
                        Text(kindName(e.kind), fontSize = ssp(16f), maxLines = 1, modifier = Modifier.weight(1f))
                        Text(valueText(e), fontSize = ssp(16f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1.2f))
                    }
                }
            }
        }
    }
}

@Composable
internal fun kindName(k: ObdHistory.Kind) = stringResource(when (k) {
    ObdHistory.Kind.TRIP -> R.string.obd_history_trip
    ObdHistory.Kind.ZERO_100 -> R.string.obd_test_0_100
    ObdHistory.Kind.KMH_80_120 -> R.string.obd_test_80_120
    ObdHistory.Kind.QUARTER_MILE -> R.string.obd_test_quarter
    ObdHistory.Kind.BRAKE_100_0 -> R.string.obd_test_brake
    ObdHistory.Kind.LAP -> R.string.obd_history_lap
    ObdHistory.Kind.DYNO -> R.string.obd_history_dyno
})

@Composable
private fun valueText(e: ObdHistory.Entry) = when (e.kind) {
    ObdHistory.Kind.TRIP -> "%.1f km".format(e.value) + if (e.second > 0f) " · %.1f %s".format(e.second, stringResource(R.string.obd_unit_kmpl)) else ""
    ObdHistory.Kind.BRAKE_100_0 -> "%.1f m".format(e.value)
    ObdHistory.Kind.QUARTER_MILE -> "%.2f s".format(e.value) + if (e.second > 0f) " · %.0f km/h".format(e.second) else ""
    ObdHistory.Kind.LAP -> time(e.value)
    ObdHistory.Kind.DYNO -> "%.0f hp".format(e.value)
    else -> "%.2f s".format(e.value)
}

// --- Settings: two columns side by side on a wide screen, all at once ---

@Composable
private fun SettingsTab(settings: ObdAppSettings, wide: Boolean) {
    var profile by remember { mutableStateOf(settings.profile) }
    var engine by remember { mutableStateOf(settings.engine) }
    var rpmMax by remember { mutableIntStateOf(settings.rpmMax) }
    var redline by remember { mutableIntStateOf(settings.redline) }
    var cc by remember { mutableIntStateOf(settings.displacementCc) }
    var tank by remember { mutableIntStateOf(settings.tankLiters) }
    var fuel by remember { mutableStateOf(settings.fuel) }
    var ethanol by remember { mutableIntStateOf(settings.ethanolPercent) }
    var mass by remember { mutableIntStateOf(settings.massKg) }
    var gears by remember { mutableIntStateOf(ObdApp.learnedGears) }
    var picking by remember { mutableStateOf(false) }
    var pickingTiles by remember { mutableStateOf(false) }
    val probe by ObdReader.probe.collectAsState()
    var vag by remember { mutableStateOf(false) }
    if (picking) { CardPicker(settings, wide) { picking = false }; return }
    if (pickingTiles) { CardTilePicker(settings, wide) { pickingTiles = false }; return }
    if (vag) { VagList(probe.answers) { vag = false }; return }
    val items = listOf<@Composable (Modifier) -> Unit>(
        { m ->
            Choices(stringResource(R.string.obd_set_profile), CarProfiles.ALL.map { it to it.label }, CarProfiles.byId(profile), m) { p ->
                settings.applyProfile(p)
                profile = p.id
                engine = settings.engine
                fuel = settings.fuel
                rpmMax = settings.rpmMax
                redline = settings.redline
                cc = settings.displacementCc
                tank = settings.tankLiters
                mass = settings.massKg
            }
        },
        { m ->
            Choices(stringResource(R.string.obd_set_engine), listOf(ObdAppSettings.Engine.AUTO to R.string.obd_set_engine_auto,
                ObdAppSettings.Engine.TURBO to R.string.obd_set_engine_turbo, ObdAppSettings.Engine.ASPIRATED to R.string.obd_set_engine_na), engine, m) { engine = it; settings.engine = it }
        },
        { m ->
            Choices(stringResource(R.string.obd_set_fuel), listOf(ObdAppSettings.Fuel.PETROL to R.string.obd_set_fuel_petrol,
                ObdAppSettings.Fuel.DIESEL to R.string.obd_set_fuel_diesel), fuel, m) { fuel = it; settings.fuel = it }
        },
        { m -> Stepper(stringResource(R.string.obd_set_rpm_max), rpmMax, "rpm", 500, 4000..10000, modifier = m) { rpmMax = it; settings.rpmMax = it; if (redline > it) { redline = it; settings.redline = it } } },
        { m -> Stepper(stringResource(R.string.obd_set_redline), redline, "rpm", 100, 2000..rpmMax, bigStep = 500, modifier = m) { redline = it; settings.redline = it } },
        { m -> Stepper(stringResource(R.string.obd_set_displacement), cc, "cm³", 100, 600..8000, bigStep = 1000, modifier = m) { cc = it; settings.displacementCc = it } },
        { m -> Stepper(stringResource(R.string.obd_set_ethanol), ethanol, "%", 1, 0..100, bigStep = 10, modifier = m) { ethanol = it; settings.ethanolPercent = it } },
        { m -> Stepper(stringResource(R.string.obd_set_tank), tank, "L", 1, 20..150, bigStep = 10, modifier = m) { tank = it; settings.tankLiters = it } },
        { m -> Stepper(stringResource(R.string.obd_set_mass), mass, "kg", 10, 500..4000, bigStep = 100, modifier = m) { mass = it; settings.massKg = it } },
        { m ->
            Row(m, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.obd_set_card), fontSize = ssp(18f), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Chip(stringResource(R.string.obd_set_card_pick)) { picking = true }
            }
        },
        { m ->
            Row(m, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.obd_set_gears, gears), fontSize = ssp(18f), maxLines = 2, modifier = Modifier.weight(1f))
                Chip(stringResource(R.string.obd_set_gears_forget)) { ObdApp.forgetGears(); gears = 0 }
            }
        },
        { m ->
            Row(m, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(sdp(6f))) {
                Text(stringResource(R.string.obd_set_vag), fontSize = ssp(18f), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (probe.running) {
                    Text(stringResource(R.string.obd_vag_running), fontSize = ssp(16f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Chip(stringResource(if (probe.answers.isEmpty()) R.string.obd_vag_probe else R.string.obd_vag_again)) { ObdReader.requestProbe() }
                    Chip(stringResource(R.string.obd_vag_scan)) { ObdReader.requestScan() }
                    if (probe.answers.isNotEmpty()) Chip(stringResource(R.string.obd_vag_show)) { vag = true }
                }
            }
        },
        { m ->
            Row(m, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.obd_set_card_tiles), fontSize = ssp(18f), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Chip(stringResource(R.string.obd_set_card_pick)) { pickingTiles = true }
            }
        },
    )
    // Two pages, so every setting keeps a full-size touch target and a page still fits a short screen.
    var page by rememberSaveable { mutableIntStateOf(0) }
    val pages = listOf(
        R.string.obd_set_page_car to listOf(items[0], items[2], items[6], items[5], items[7], items[8]),
        R.string.obd_set_page_gauges to listOf(items[1], items[3], items[4], items[9], items[10], items[11], items[12]),
    )
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(sdp(10f))) {
        Row(horizontalArrangement = Arrangement.spacedBy(sdp(8f)), verticalAlignment = Alignment.CenterVertically) {
            pages.forEachIndexed { i, (label, _) -> Chip(stringResource(label), i == page) { page = i } }
            Note(stringResource(if (page == 0) R.string.obd_set_note_car else R.string.obd_set_note_gauges), Modifier.weight(1f).padding(start = sdp(8f)), maxLines = 3)
        }
        val cols = if (wide) 2 else 1
        Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(sdp(8f))) {
            pages[page].second.chunked(cols).forEach { row ->
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(sdp(28f)), verticalAlignment = Alignment.CenterVertically) {
                    row.forEach { it(Modifier.weight(1f)) }
                    repeat(cols - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/**
 * What the VAG probe got: one line per question, with the module, the identifier and the bytes the
 * car answered (a dash when it refused or stayed silent). The identifiers are tried, not promised:
 * a car's software decides which ones it knows.
 */
@Composable
private fun VagList(answers: List<ObdReader.VagAnswer>, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(sdp(8f))) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(sdp(14f))) {
            Note(stringResource(R.string.obd_vag_hint), Modifier.weight(1f), maxLines = 2)
            ObdButton(stringResource(R.string.obd_done), primary = true, onClick = onDone)
        }
        if (answers.isEmpty()) {
            Text(stringResource(R.string.obd_vag_none), fontSize = ssp(18f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(sdp(6f)),
        ) {
            answers.forEach { a ->
                Row(
                    Modifier.fillMaxWidth().height(sdp(44f))
                        .themed(MaterialTheme.shapes.small, MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = sdp(12f)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(a.query.name, fontSize = ssp(18f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text("${a.query.header} ${"%04X".format(a.query.did)}", fontSize = ssp(15f), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    Text("  " + (a.bytes?.let { Vag.hex(it) } ?: DASH), fontSize = ssp(15f), maxLines = 1)
                }
            }
        }
    }
}

/**
 * Which values the dashboard's Car card shows: tapped in order, up to [CardValue.MAX]; each picked
 * one shows its place. All of them on one screen, like the rest of the app.
 */
@Composable
private fun CardPicker(settings: ObdAppSettings, wide: Boolean, onDone: () -> Unit) {
    var picked by remember { mutableStateOf(settings.cardValues) }
    val tiles = CardValue.entries.map { v ->
        @Composable { m: Modifier ->
            val place = picked.indexOf(v)
            val selected = place >= 0
            Row(
                m.themed(MaterialTheme.shapes.small, if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .carClickable(MaterialTheme.shapes.small) {
                        picked = if (selected) picked - v else if (picked.size < CardValue.MAX) picked + v else picked
                        settings.cardValues = picked
                    }.padding(horizontal = sdp(12f)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                Icon(v.icon, null, tint = fg, modifier = Modifier.size(sdp(26f)))
                Text(stringResource(v.label), fontSize = ssp(17f), color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = sdp(8f)))
                if (selected) Text("${place + 1}", fontSize = ssp(20f), fontWeight = FontWeight.Bold, color = fg)
            }
        }
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(sdp(10f))) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(sdp(14f))) {
            Note(stringResource(R.string.obd_set_card_hint, CardValue.MAX), Modifier.weight(1f), maxLines = 2)
            ObdButton(stringResource(R.string.obd_done), primary = true, onClick = onDone)
        }
        TileGrid(tiles, if (wide) 4 else 2, Modifier.fillMaxWidth().weight(1f))
    }
}

/**
 * Which five values the compact card in an app pane shows: tapped in order, each picked one shows
 * its place. A value the car stops reporting is replaced by one it does report.
 */
@Composable
private fun CardTilePicker(settings: ObdAppSettings, wide: Boolean, onDone: () -> Unit) {
    var picked by remember { mutableStateOf(settings.cardTiles.ifEmpty { DefaultCardTiles }) }
    val tiles = CardTileIds.map { id ->
        val place = picked.indexOf(id)
        val selected = place >= 0
        @Composable { m: Modifier ->
            Row(
                m.themed(MaterialTheme.shapes.small, if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .carClickable(MaterialTheme.shapes.small) {
                        picked = if (selected) picked - id else if (picked.size < ObdAppSettings.CARD_TILES) picked + id else picked
                        settings.cardTiles = picked
                    }
                    .padding(horizontal = sdp(12f)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                Text(
                    stringResource(cardTileLabel(id)),
                    fontSize = ssp(17f),
                    color = fg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = sdp(8f)),
                )
                if (selected) Text("${place + 1}", fontSize = ssp(20f), fontWeight = FontWeight.Bold, color = fg)
            }
        }
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(sdp(10f))) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(sdp(14f))) {
            Note(stringResource(R.string.obd_set_card_tiles_hint), Modifier.weight(1f), maxLines = 2)
            ObdButton(stringResource(R.string.obd_done), primary = true, onClick = onDone)
        }
        TileGrid(tiles, if (wide) 4 else 2, Modifier.fillMaxWidth().weight(1f))
    }
}

@Composable
private fun <T> Choices(title: String, options: List<Pair<T, Int>>, selected: T, modifier: Modifier, onSelect: (T) -> Unit) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(sdp(6f))) {
        Text(title, fontSize = ssp(18f), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        options.forEach { (v, label) -> Chip(stringResource(label), v == selected) { onSelect(v) } }
    }
}
