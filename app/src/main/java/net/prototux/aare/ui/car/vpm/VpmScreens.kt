package net.prototux.aare.ui.car.vpm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.prototux.aare.R
import net.prototux.aare.ui.car.carClickable
import net.prototux.aare.vpm.VpmCategory
import net.prototux.aare.vpm.VpmMetric
import net.prototux.aare.vpm.VpmMetrics
import net.prototux.aare.vpm.VpmReadingState
import net.prototux.aare.vpm.VpmSignal
import net.prototux.aare.vpm.VpmSnapshot
import net.prototux.aare.vpm.formatVpmNumber
import net.prototux.aare.vpm.readVpmMetric
import net.prototux.aare.vpm.signalsIn

/**
 * VPM's main screen: three gauges over five tiles, no category bar. The composition is VPM 0.4.1's
 * (`DashboardScreen`), drawn from a snapshot instead of from VPM's own telemetry state.
 */
@Composable
fun VpmMainScreen(
    snapshot: VpmSnapshot?,
    onGauge: (VpmMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val portrait = maxHeight > maxWidth
        val columns = when {
            maxWidth >= 700.dp -> 5
            maxWidth >= 500.dp -> 3
            else -> 2
        }
        val portraitPanelHeight = maxWidth * 0.44f
        val tileRowHeight = ((maxWidth - 6.dp * (columns - 1)) / columns) * 0.48f
        Column(Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 6.dp)) {
            VpmGaugePanel(
                snapshot = snapshot,
                onGauge = onGauge,
                modifier = if (portrait) {
                    Modifier.fillMaxWidth().height(portraitPanelHeight)
                } else {
                    Modifier.fillMaxWidth().weight(1f)
                },
            )
            Spacer(Modifier.height(8.dp))
            VpmTileRows(
                metrics = VpmMetrics.mainTiles,
                snapshot = snapshot,
                columns = columns,
                rowHeight = if (portrait) portraitPanelHeight * 0.82f else tileRowHeight,
                modifier = Modifier.fillMaxWidth(),
            )
            if (portrait) Spacer(Modifier.weight(1f))
        }
    }
}

/**
 * VPM's extended screen: the same three gauges smaller, six category tabs and its adaptive grid.
 */
@Composable
fun VpmCategoriesScreen(
    snapshot: VpmSnapshot?,
    selected: VpmCategory,
    onCategory: (VpmCategory) -> Unit,
    onGauge: (VpmMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val portrait = maxHeight > maxWidth
        val portraitPanelHeight = maxWidth * 0.44f
        Column(Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 6.dp)) {
            VpmGaugePanel(
                snapshot = snapshot,
                onGauge = onGauge,
                modifier = if (portrait) {
                    Modifier.fillMaxWidth().height(portraitPanelHeight)
                } else {
                    Modifier.fillMaxWidth().weight(0.50f)
                },
            )
            Spacer(Modifier.height(4.dp))
            VpmCategoryTabBar(selected, onCategory)
            Spacer(Modifier.height(6.dp))
            VpmMetricGrid(
                snapshot = snapshot,
                category = selected,
                modifier = if (portrait) {
                    Modifier.fillMaxWidth().weight(1f)
                } else {
                    Modifier.fillMaxWidth().weight(0.50f)
                },
            )
        }
    }
}

@Composable
private fun VpmGaugePanel(
    snapshot: VpmSnapshot?,
    onGauge: (VpmMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVpmColors.current
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .clip(shape)
            .background(colors.panel)
            .border(1.dp, colors.border.copy(alpha = 0.7f), shape)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        VpmMetrics.gauges.forEach { metric ->
            VpmGauge(
                metric = metric,
                reading = readVpmMetric(metric, snapshot),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onGauge(metric) },
            )
        }
    }
}

@Composable
private fun VpmTileRows(
    metrics: List<VpmMetric>,
    snapshot: VpmSnapshot?,
    columns: Int,
    rowHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        metrics.chunked(columns).forEach { row ->
            Row(
                Modifier.height(rowHeight).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { metric ->
                    val reading = readVpmMetric(metric, snapshot)
                    VpmTile(
                        label = stringResource(metric.labelRes),
                        value = reading.text,
                        unit = metric.unitRes?.let { stringResource(it) }.orEmpty(),
                        warning = reading.warning,
                        critical = reading.critical,
                        stale = reading.state != VpmReadingState.OK,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                repeat(columns - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** VPM's category strip: the active tab underlined in the accent, thin dividers between tabs. */
@Composable
private fun VpmCategoryTabBar(
    selected: VpmCategory,
    onCategory: (VpmCategory) -> Unit,
) {
    val colors = LocalVpmColors.current
    Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        VpmCategory.entries.forEachIndexed { index, category ->
            if (index > 0) {
                Box(Modifier.width(1.dp).fillMaxHeight(0.55f).background(colors.border.copy(alpha = 0.8f)))
            }
            val active = category == selected
            Column(
                Modifier.weight(1f).fillMaxHeight()
                    .carClickable(RectangleShape, onClick = { onCategory(category) }),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    stringResource(category.labelRes),
                    color = if (active) colors.text else colors.dim,
                    fontFamily = VpmFont,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(5.dp))
                Box(Modifier.height(2.dp).width(30.dp).background(if (active) colors.accent else Color.Transparent))
            }
        }
    }
}

/** VPM's adaptive metric grid: 2 to 5 columns, two full rows visible as on the mockup. */
@Composable
private fun VpmMetricGrid(
    snapshot: VpmSnapshot?,
    category: VpmCategory,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVpmColors.current
    val signals = snapshot?.signalsIn(category).orEmpty()
    BoxWithConstraints(modifier) {
        val columns = (maxWidth.value / 236f).toInt().coerceIn(2, 5)
        val tileHeight = ((maxHeight - 8.dp) / 2f).coerceIn(88.dp, 220.dp)
        if (signals.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.vpm_category_empty),
                    color = colors.dim,
                    fontFamily = VpmFont,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(signals, key = { it.id }) { signal ->
                    VpmSignalTile(signal, Modifier.fillMaxWidth().height(tileHeight))
                }
            }
        }
    }
}

/** One tile of the extended grid: VPM's label, value and unit, localized for the signals DashGrid knows. */
@Composable
private fun VpmSignalTile(signal: VpmSignal, modifier: Modifier = Modifier) {
    val metric = VpmMetrics.of(signal.id)
    val knownLabel = metric?.let { stringResource(it.labelRes) }
    val knownUnit = metric?.unitRes?.let { stringResource(it) }
    val value = when {
        signal.stale -> "—"
        signal.value != null -> signal.value
        signal.raw != null -> formatVpmNumber(signal.raw, metric?.decimals ?: 0)
        else -> "—"
    }
    VpmTile(
        label = knownLabel ?: signal.label,
        value = value,
        unit = knownUnit ?: signal.unit,
        warning = !signal.stale && (signal.warning || metric?.warning(signal.raw) == true),
        critical = !signal.stale && metric?.critical(signal.raw) == true,
        stale = signal.stale,
        modifier = modifier,
    )
}

/**
 * VPM's tile: a small upper-case label over the value, the unit under it, a hairline border around
 * a card. A missing or stale value is a dash, never a zero.
 */
@Composable
private fun VpmTile(
    label: String,
    value: String,
    unit: String,
    warning: Boolean,
    critical: Boolean,
    stale: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVpmColors.current
    BoxWithConstraints(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.card)
            .border(1.dp, colors.border, RoundedCornerShape(10.dp)),
    ) {
        val w = maxWidth.value
        val h = maxHeight.value
        val labelSize = (w * 0.036f).coerceIn(10f, 14f).sp
        val unitSize = (w * 0.030f).coerceIn(10f, 13f).sp
        val valueSize = minOf(w * 0.16f, h * 0.36f).coerceIn(18f, 58f).sp
        Column(
            Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                label.uppercase(),
                color = colors.dim,
                fontFamily = VpmFont,
                fontSize = labelSize,
                letterSpacing = 1.1.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        value,
                        color = when {
                            critical -> colors.critical
                            warning -> colors.warning
                            stale -> colors.dim
                            else -> colors.text
                        },
                        fontFamily = VpmFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = valueSize,
                        lineHeight = valueSize,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center,
                    )
                    if (unit.isNotEmpty()) {
                        Text(
                            unit,
                            color = colors.dim,
                            fontFamily = VpmFont,
                            fontSize = unitSize,
                            lineHeight = unitSize,
                            letterSpacing = 0.4.sp,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
        if (stale) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(top = 7.dp, end = 7.dp)
                    .size(5.dp).clip(CircleShape).background(colors.warning),
            )
        }
    }
}

/** VPM's source pill: DEMO, LIVE or no link, with VPM's own status as the channel. */
@Composable
fun VpmSourcePill(snapshot: VpmSnapshot?, modifier: Modifier = Modifier) {
    val colors = LocalVpmColors.current
    val demo = snapshot?.connection == net.prototux.aare.vpm.VpmConnection.DEMO
    val live = snapshot?.connection == net.prototux.aare.vpm.VpmConnection.CONNECTED
    val failed = snapshot?.connection == net.prototux.aare.vpm.VpmConnection.ERROR
    val label = when {
        snapshot == null -> stringResource(R.string.vpm_status_offline)
        demo -> stringResource(R.string.vpm_status_demo)
        live -> stringResource(R.string.vpm_status_live)
        snapshot.connection == net.prototux.aare.vpm.VpmConnection.CONNECTING ->
            snapshot.status.ifBlank { stringResource(R.string.vpm_status_connecting) }
        failed -> snapshot.status.ifBlank { stringResource(R.string.vpm_status_offline) }
        else -> stringResource(R.string.vpm_status_offline)
    }
    val color = when {
        demo -> colors.warning
        live || snapshot?.connection == net.prototux.aare.vpm.VpmConnection.CONNECTING -> colors.accent
        failed -> colors.critical
        else -> colors.dim
    }
    // VPM's own status is a channel only on a live link; the DEMO pill stays one word, as in VPM.
    val channel = if (snapshot?.connection == net.prototux.aare.vpm.VpmConnection.CONNECTED) {
        snapshot.status
    } else {
        ""
    }
    Row(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, color = color, fontFamily = VpmFont, fontSize = 10.sp, letterSpacing = 1.1.sp, maxLines = 1)
        if (channel.isNotBlank()) {
            Text(
                " · $channel",
                color = colors.dim,
                fontFamily = VpmFont,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Two dots: the cockpit has two screens, and either can be picked without a swipe. */
@Composable
fun VpmPageDots(
    current: Int,
    count: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVpmColors.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(count) { index ->
            val name = stringResource(if (index == 0) R.string.vpm_page_main else R.string.vpm_page_categories)
            Box(
                Modifier
                    .size(28.dp)
                    .carClickable(CircleShape, onClick = { onSelect(index) })
                    .semantics { contentDescription = name },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(if (index == current) 6.dp else 5.dp)
                        .clip(CircleShape)
                        .background(if (index == current) colors.accent else colors.dim.copy(alpha = 0.45f)),
                )
            }
        }
    }
}
