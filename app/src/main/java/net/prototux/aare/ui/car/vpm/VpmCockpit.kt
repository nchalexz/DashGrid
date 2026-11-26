package net.prototux.aare.ui.car.vpm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import net.prototux.aare.vpm.VpmRead
import net.prototux.aare.vpm.VpmSnapshot

/** What the cockpit knows at one moment. The four non-cockpit states each get their own calm card. */
sealed interface VpmUiState {
    data object NotInstalled : VpmUiState
    data object BridgeMissing : VpmUiState
    data class Failed(val message: String) : VpmUiState
    data class Cockpit(val snapshot: VpmSnapshot?) : VpmUiState
}

/** How often the cockpit asks VPM for its snapshot while it is the page on screen. */
private const val VPM_POLL_MS = 500L

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
        Box(modifier.fillMaxSize().background(colors.background)) {
            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxSize(),
                // The page's own two screens still swipe inside the workspace; DashGrid's pager keeps
                // its gesture on the empty margin and the bottom dots.
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
            VpmSourcePill(snapshot, Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 8.dp))
            VpmPageDots(
                current = pager.currentPage,
                count = 2,
                onSelect = { target -> scope.launch { pager.animateScrollToPage(target) } },
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 4.dp, top = 2.dp),
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
