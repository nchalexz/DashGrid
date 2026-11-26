package net.prototux.aare.ui.car.vpm

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cyclone
import androidx.compose.material.icons.rounded.OilBarrel
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import net.prototux.aare.ui.car.carClickable
import net.prototux.aare.vpm.VpmGaugeIcon
import net.prototux.aare.vpm.VpmMetric
import net.prototux.aare.vpm.VpmReading
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The round analogue-digital gauge of VPM's cockpit, with its 0.4.1 geometry:
 *
 * | Element | Position / size |
 * |---|---|
 * | centre, value | 0, 0 |
 * | outer radius | R |
 * | dark disc | 0.44R |
 * | value width | 0.78R |
 * | unit centre | 0, 0.54R |
 * | pictogram centre | 0, 0.73R |
 * | pictogram size | 0.18R |
 * | gauge name | 0, 1.15R |
 *
 * The value is centred on the glyphs' real bounds, not on the baseline, and shrinks until it fits
 * the disc. The bright progress arc between the rings stays (the customer asked for it), and only
 * the red zone is filled; there is no amber zone in the reference.
 */
@Composable
fun VpmGauge(
    metric: VpmMetric,
    reading: VpmReading,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalVpmColors.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val label = stringResource(metric.labelRes)
    val unit = metric.unitRes?.let { stringResource(it) }.orEmpty().uppercase()
    val typeface = remember {
        runCatching { ResourcesCompat.getFont(context, net.prototux.aare.R.font.titillium_web_light) }
            .getOrNull() ?: Typeface.DEFAULT
    }
    val paint = remember(typeface) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            this.typeface = typeface
        }
    }

    Column(
        modifier.then(if (onClick != null) Modifier.carClickable(RoundedCornerShape(16.dp), onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val side = minOf(maxWidth, maxHeight)
            val radius = side / 2f
            var lastFraction by remember(metric.id) { mutableStateOf(0f) }
            LaunchedEffect(reading.fraction) {
                reading.fraction?.let { lastFraction = it }
            }
            val target = if (reading.isOk) reading.fraction ?: lastFraction else lastFraction
            val needle by animateFloatAsState(
                targetValue = target,
                animationSpec = tween(durationMillis = 150, easing = LinearEasing),
                label = "vpm-needle",
            )
            val needleAlpha by animateFloatAsState(
                targetValue = if (reading.isOk) 1f else 0f,
                animationSpec = tween(durationMillis = 200),
                label = "vpm-needle-alpha",
            )

            Box(
                Modifier.size(side).semantics {
                    contentDescription = if (reading.isOk) {
                        "$label ${reading.text} $unit".trim()
                    } else {
                        "$label —".trim()
                    }
                },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawGauge(metric, needle, needleAlpha, colors)
                    drawValue(paint, reading, colors)
                }

                if (unit.isNotEmpty()) {
                    Text(
                        unit,
                        color = colors.dim,
                        fontFamily = VpmFont,
                        fontSize = with(density) { (radius.toPx() * 0.080f).toSp() },
                        letterSpacing = 1.2.sp,
                        maxLines = 1,
                        modifier = Modifier.align(Alignment.Center).offset(y = radius * 0.54f),
                    )
                }
                gaugeIcon(metric.icon)?.let { icon ->
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = colors.text.copy(alpha = 0.55f),
                        modifier = Modifier.align(Alignment.Center)
                            .offset(y = radius * 0.73f)
                            .size(radius * 0.18f),
                    )
                }
            }
        }
        Text(
            label.uppercase(),
            color = colors.dim,
            fontFamily = VpmFont,
            fontSize = 12.sp,
            letterSpacing = 1.2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

private fun gaugeIcon(icon: VpmGaugeIcon): ImageVector? = when (icon) {
    VpmGaugeIcon.COOLANT -> Icons.Rounded.Thermostat
    VpmGaugeIcon.BOOST -> Icons.Rounded.Cyclone
    VpmGaugeIcon.OIL -> Icons.Rounded.OilBarrel
    VpmGaugeIcon.NONE -> null
}

private fun DrawScope.drawGauge(
    metric: VpmMetric,
    needle: Float,
    needleAlpha: Float,
    colors: VpmColors,
) {
    val r = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val start = 135f
    val sweep = 270f
    fun angle(degrees: Float): Float = degrees * PI.toFloat() / 180f
    fun point(radius: Float, degrees: Float) = Offset(
        center.x + radius * cos(angle(degrees)),
        center.y + radius * sin(angle(degrees)),
    )
    fun fractionToAngle(fraction: Float): Float = start + sweep * fraction.coerceIn(0f, 1f)

    // The thin uniform outer ring and the inner ring, as in the reference.
    drawCircle(
        colors.ring.copy(alpha = 0.55f),
        radius = r * 0.975f,
        center = center,
        style = Stroke(width = 1.6.dp.toPx()),
    )
    drawCircle(
        colors.ring.copy(alpha = 0.22f),
        radius = r * 0.945f,
        center = center,
        style = Stroke(width = 1.dp.toPx()),
    )

    // The progress arc: a segment between the two rings from the scale's start to the value.
    if (needleAlpha > 0.01f) {
        drawArc(
            color = colors.text.copy(alpha = 0.90f * needleAlpha),
            startAngle = start,
            sweepAngle = sweep * needle.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(center.x - r * 0.958f, center.y - r * 0.958f),
            size = Size(r * 1.916f, r * 1.916f),
            style = Stroke(width = 4.2.dp.toPx(), cap = StrokeCap.Round),
        )
    }

    // The red zone, the scale's only fill.
    val warn = metric.warnFrom?.let {
        ((it - metric.scaleMin) / (metric.scaleMax - metric.scaleMin)).toFloat().coerceIn(0f, 1f)
    }
    if (warn != null && warn < 1f) {
        drawArc(
            color = colors.critical.copy(alpha = 0.78f),
            startAngle = fractionToAngle(warn),
            sweepAngle = sweep * (1f - warn),
            useCenter = false,
            topLeft = Offset(center.x - r * 0.905f, center.y - r * 0.905f),
            size = Size(r * 1.81f, r * 1.81f),
            style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round),
        )
    }

    // 60 divisions: long every 10, middle every 5.
    for (i in 0..60) {
        val major = i % 10 == 0
        val middle = i % 5 == 0
        val degrees = start + sweep * i / 60f
        val inner = when {
            major -> r * 0.765f
            middle -> r * 0.820f
            else -> r * 0.850f
        }
        drawLine(
            color = colors.ring.copy(alpha = if (major) 0.9f else if (middle) 0.55f else 0.30f),
            start = point(inner, degrees),
            end = point(r * 0.925f, degrees),
            strokeWidth = when {
                major -> 2.2.dp.toPx()
                middle -> 1.5.dp.toPx()
                else -> 1.dp.toPx()
            },
            cap = StrokeCap.Butt,
        )
    }

    if (needleAlpha > 0.01f) {
        val degrees = fractionToAngle(needle)
        drawLine(
            color = colors.needle.copy(alpha = needleAlpha),
            start = point(r * 0.45f, degrees),
            end = point(r * 0.90f, degrees),
            strokeWidth = (r * 0.022f).coerceAtLeast(2.8.dp.toPx()),
            cap = StrokeCap.Round,
        )
    }

    // The 0.44R disc: the value and the unit never cross its edge.
    drawCircle(colors.dialFace, radius = r * 0.44f, center = center)
}

/** The value, centred on the real glyph bounds and shrunk until it fits the disc. */
private fun DrawScope.drawValue(
    paint: Paint,
    reading: VpmReading,
    colors: VpmColors,
) {
    val r = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val text = reading.text
    if (text.isBlank()) return

    val color = when {
        reading.critical -> colors.critical
        reading.warning -> colors.warning
        !reading.isOk -> colors.text.copy(alpha = 0.55f)
        else -> colors.text
    }
    paint.color = color.toArgb()
    var textSize = r * 0.62f
    val bounds = Rect()
    while (textSize > r * 0.12f) {
        paint.textSize = textSize
        paint.getTextBounds(text, 0, text.length, bounds)
        if (bounds.width() <= r * 0.78f && bounds.height() <= r * 0.62f) break
        textSize *= 0.94f
    }
    paint.textSize = textSize
    paint.getTextBounds(text, 0, text.length, bounds)
    val baseline = center.y - (bounds.top + bounds.bottom) / 2f
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText(text, center.x, baseline, paint)
    }
}
