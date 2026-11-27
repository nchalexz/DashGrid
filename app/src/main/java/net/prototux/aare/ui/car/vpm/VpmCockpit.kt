package net.prototux.aare.ui.car.vpm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.prototux.aare.R
import net.prototux.aare.ui.car.carClickable
import net.prototux.aare.vpm.Vpm
import net.prototux.aare.vpm.VpmCategory
import net.prototux.aare.vpm.VpmMetric
import net.prototux.aare.vpm.VpmMetrics
import net.prototux.aare.vpm.readVpmMetric
import net.prototux.aare.vpm.VpmRead
import net.prototux.aare.vpm.VpmSnapshot

/** What the cockpit knows at one moment. The four non-cockpit states each get their own calm card. */
sealed interface VpmUiState {
    data object NotInstalled : VpmUiState
    data object BridgeMissing : VpmUiState
    data class Failed(val message: String) : VpmUiState
    data class Cockpit(val snapshot: VpmSnapshot?) : VpmUiState
}

/**
 * How often the cockpit asks VPM for its snapshot while it is the page on screen.
 *
 * The bridge is cheap; 50 ms keeps the displayed sample within half of VPM's MAP frame
 * instead of waiting for the next one.
 */
private const val VPM_POLL_MS = 50L

/**
 * The bridge's four answers to the four cards the cockpit can show. Kept pure so a unit test can
 * prove that a missing app is a card and never an exception.
 */
fun vpmUiState(read: VpmRead): VpmUiState = when (read) {
    is VpmRead.Snapshot -> VpmUiState.Cockpit(read.value)
    VpmRead.NoSnapshot -> VpmUiState.Cockpit(null)
    VpmRead.NotInstalled -> VpmUiState.NotInstalled
    VpmRead.BridgeMissing -> VpmUiState.BridgeMissing
    is VpmRead.Failed -> VpmUiState.Failed(read.message)
}

/**
 * The preinstalled "Car" widget: DashGrid's own drawing of VPM's cockpit, fed by VPM's read-only
 * snapshot. Nothing here opens an OBD connection or starts VPM's service.
 *
 * Only the page on screen polls: [active] is DashGrid's "this page is the visible one", so the
 * neighbouring page and the editor preview cost nothing.
 */
@Composable
fun VpmCockpit(active: Boolean, modifier: Modifier = Modifier) {
    if (!active) {
        VpmInactivePanel(modifier)
        return
    }
    val context = LocalContext.current
    var state by remember { mutableStateOf<VpmUiState>(VpmUiState.Cockpit(null)) }
    LaunchedEffect(Unit) {
        while (isActive) {
            state = vpmUiState(Vpm.read(context))
            delay(VPM_POLL_MS)
        }
    }
    when (val current = state) {
        is VpmUiState.Cockpit -> VpmCockpitView(current.snapshot, modifier)
        is VpmUiState.NotInstalled -> VpmTheme {
            VpmMessageCard(
                title = stringResource(R.string.vpm_name),
                body = stringResource(R.string.vpm_not_installed),
                primary = stringResource(R.string.vpm_install) to { Vpm.openReleases(context) },
                secondary = null,
                modifier = modifier,
            )
        }
        is VpmUiState.BridgeMissing -> VpmTheme {
            VpmMessageCard(
                title = stringResource(R.string.vpm_name),
                body = stringResource(R.string.vpm_bridge_missing),
                primary = stringResource(R.string.vpm_update) to { Vpm.openReleases(context) },
                secondary = stringResource(R.string.vpm_open_app) to { Vpm.openOnPhone(context) },
                modifier = modifier,
            )
        }
        is VpmUiState.Failed -> VpmTheme {
            VpmMessageCard(
                title = stringResource(R.string.vpm_name),
                body = stringResource(R.string.vpm_failed),
                primary = stringResource(R.string.vpm_open_app) to { Vpm.openOnPhone(context) },
                secondary = stringResource(R.string.vpm_update) to { Vpm.openReleases(context) },
                modifier = modifier,
            )
        }
    }
}

/**
 * The cockpit's two screens and its state overlays, drawn from [snapshot] alone: no Android
 * lookups, no polling. Also what the Paparazzi mockups render.
 */
@Composable
fun VpmCockpitView(
    snapshot: VpmSnapshot?,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    initialCategory: VpmCategory = VpmCategory.ENGINE,
) {
    VpmTheme {
        val colors = LocalVpmColors.current
        val pager = rememberPagerState(initialPage = initialPage, pageCount = { 2 })
        val scope = rememberCoroutineScope()
        var categoryId by rememberSaveable { mutableStateOf(initialCategory.id) }
        val showCategories = {
            scope.launch { pager.animateScrollToPage(1) }
        }
        BoxWithConstraints(modifier.fillMaxSize().background(colors.background)) {
            // A cockpit in a small workspace cell gets the compact four-value panel; the full
            // gauges/tabs composition needs roughly half a screen or more.
            if (maxWidth < 360.dp || maxHeight < 220.dp) {
                VpmCompactPanel(snapshot, Modifier.fillMaxSize())
            } else {
                Column(Modifier.fillMaxSize()) {
                    // The source pill used to float over the gauge panel; a live VPM status can be
                    // long ("Bluetooth OBDII · ELM327 v1.5 · 14.3V") and covered the coolant dial.
                    // A thin header keeps the pill and the page dots out of the instruments' way.
                    Row(
                        Modifier.fillMaxWidth().height(28.dp).padding(start = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            VpmSourcePill(snapshot)
                        }
                        VpmPageDots(
                            current = pager.currentPage,
                            count = 2,
                            onSelect = { target -> scope.launch { pager.animateScrollToPage(target) } },
                        )
                    }
                    HorizontalPager(
                        state = pager,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        // The cockpit's own two screens are switched by the header dots or by
                        // tapping a gauge, never by a horizontal swipe: a swipe belongs to
                        // DashGrid's workspace pager, so "swipe anywhere" keeps working even when
                        // the cockpit fills the page.
                        userScrollEnabled = false,
                        key = { it },
                    ) { page ->
                        when (page) {
                            0 -> VpmMainScreen(
                                snapshot = snapshot,
                                onGauge = { metric -> categoryId = metric.category.id; showCategories() },
                                modifier = Modifier.fillMaxSize(),
                            )
                            else -> VpmCategoriesScreen(
                                snapshot = snapshot,
                                selected = VpmCategory.byId(categoryId) ?: VpmCategory.ENGINE,
                                onCategory = { categoryId = it.id },
                                onGauge = { metric: VpmMetric -> categoryId = metric.category.id },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The compact cockpit for a workspace cell: boost, coolant, oil and DSG as plain values.
 *
 * A small VPM item used to clip the full cockpit down to one bottom-left tile. This panel never
 * steals horizontal swipes and works in a 1 × 1 cell (one value), a strip (four in a row) or a
 * block (2 × 2).
 */
@Composable
private fun VpmCompactPanel(snapshot: VpmSnapshot?, modifier: Modifier = Modifier) {
    val colors = LocalVpmColors.current
    val metrics = listOf(VpmMetrics.boostAbs, VpmMetrics.coolant, VpmMetrics.oil, VpmMetrics.dsgTemp)
    BoxWithConstraints(modifier.fillMaxSize().background(colors.background).padding(6.dp)) {
        val wideStrip = maxHeight < 150.dp && maxWidth >= 360.dp
        val four = wideStrip || (maxWidth >= 360.dp && maxHeight >= 140.dp)
        val two = !four && maxWidth >= 220.dp && maxHeight >= 110.dp
        when {
            wideStrip -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                metrics.forEach { metric ->
                    VpmCompactCell(metric, snapshot, Modifier.weight(1f).fillMaxHeight(), compact = true)
                }
            }
            four || two -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val shown = if (four) metrics else metrics.take(2)
                shown.chunked(2).forEach { row ->
                    Row(
                        Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        row.forEach { metric ->
                            VpmCompactCell(metric, snapshot, Modifier.weight(1f).fillMaxHeight(), compact = true)
                        }
                        repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            else -> VpmCompactCell(metrics.first(), snapshot, Modifier.fillMaxSize(), compact = false)
        }
    }
}

@Composable
private fun VpmCompactCell(
    metric: VpmMetric,
    snapshot: VpmSnapshot?,
    modifier: Modifier,
    compact: Boolean,
) {
    val colors = LocalVpmColors.current
    val reading = readVpmMetric(metric, snapshot)
    val unit = metric.unitRes?.let { stringResource(it) }.orEmpty()
    Column(
        modifier.padding(2.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(metric.labelRes).uppercase(),
            color = colors.dim,
            fontFamily = VpmFont,
            fontSize = if (compact) 8.sp else 9.sp,
            letterSpacing = 0.6.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            reading.text,
            color = when {
                reading.isStale -> colors.text.copy(alpha = 0.55f)
                reading.critical -> colors.critical
                reading.warning -> colors.warning
                reading.isOk -> colors.text
                else -> colors.dim
            },
            fontFamily = VpmFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (compact) 18.sp else 26.sp,
            maxLines = 1,
            softWrap = false,
        )
        if (unit.isNotEmpty()) {
            Text(
                unit,
                color = colors.dim,
                fontFamily = VpmFont,
                fontSize = 8.sp,
                maxLines = 1,
            )
        }
    }
}

/** A calm card for a cockpit that isn't the visible page: neither empty nor polling. */
@Composable
private fun VpmInactivePanel(modifier: Modifier = Modifier) {
    VpmTheme {
        val colors = LocalVpmColors.current
        Box(modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.Speed, null, tint = colors.dim, modifier = Modifier.size(28.dp))
                Text(
                    stringResource(R.string.vpm_inactive),
                    color = colors.dim,
                    fontFamily = VpmFont,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

/**
 * What the cockpit says when it can't draw anything: VPM missing, VPM without the bridge, or a
 * provider that refused. No black square, no crash, and no invented numbers.
 */
@Composable
private fun VpmMessageCard(
    title: String,
    body: String,
    primary: Pair<String, () -> Unit>,
    secondary: Pair<String, () -> Unit>?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVpmColors.current
    BoxWithConstraints(modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
        val compact = maxHeight < 220.dp
        Column(
            Modifier.padding(if (compact) 12.dp else 24.dp).widthIn(max = 560.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Rounded.Speed,
                null,
                tint = colors.accent,
                modifier = Modifier.size(if (compact) 26.dp else 44.dp),
            )
            Spacer(Modifier.size(if (compact) 6.dp else 12.dp))
            Text(
                title,
                color = colors.text,
                fontFamily = VpmFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (compact) 15.sp else 20.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(if (compact) 2.dp else 6.dp))
            Text(
                body,
                color = colors.dim,
                fontFamily = VpmFont,
                fontSize = if (compact) 11.sp else 13.sp,
                textAlign = TextAlign.Center,
                maxLines = if (compact) 2 else 4,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.size(if (compact) 8.dp else 16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VpmAction(primary.first, primary = true, onClick = primary.second)
                secondary?.let { VpmAction(it.first, primary = false, onClick = it.second) }
            }
        }
    }
}

/** A knob-friendly pill action for the placeholder cards. */
@Composable
private fun VpmAction(label: String, primary: Boolean, onClick: () -> Unit) {
    val colors = LocalVpmColors.current
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .background(if (primary) colors.accent else colors.card)
            .border(1.dp, if (primary) colors.accent else colors.border, shape)
            .carClickable(shape, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            label,
            color = if (primary) colors.background else colors.text,
            fontFamily = VpmFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1,
        )
    }
}
