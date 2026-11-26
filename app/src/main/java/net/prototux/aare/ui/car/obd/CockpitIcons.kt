package net.prototux.aare.ui.car.obd

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.unit.dp

/**
 * The Cockpit's dial pictograms, drawn here instead of taken from a vendor set: the shapes follow
 * the common automotive symbols (the ISO 2575 family a VW cluster uses) — a thermometer standing in
 * the coolant, an oil can, a fuel pump, a battery, a flame for exhaust temperature, a turbine for
 * boost, a gauge for load, an injector for rail pressure. They are simple strokes on a 24 × 24 grid,
 * so they stay legible at a dial's size and get the theme's tint.
 */
internal object CockpitIcons {
    private fun icon(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(
                pathData = PathData(block),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            .build()

    /** A thermometer standing in the coolant. */
    val coolant: ImageVector by lazy {
        icon("coolant") {
            moveTo(12f, 3.5f); lineTo(12f, 12.5f)
            // the bulb
            moveTo(12f, 14.4f)
            arcToRelative(2.6f, 2.6f, 0f, true, true, 0f, -5.2f)
            arcToRelative(2.6f, 2.6f, 0f, true, true, 0f, 5.2f)
            // the liquid
            moveTo(4.5f, 21f)
            curveTo(6.1f, 19.6f, 7.7f, 22.4f, 9.3f, 21f)
            curveTo(10.9f, 19.6f, 12.5f, 22.4f, 14.1f, 21f)
            curveTo(15.7f, 19.6f, 17.3f, 22.4f, 18.9f, 21f)
        }
    }

    /** An oil can with a drop. */
    val oil: ImageVector by lazy {
        icon("oil") {
            moveTo(3.5f, 12f); lineTo(11f, 12f); lineTo(11f, 19.5f); lineTo(3.5f, 19.5f); close()
            moveTo(6f, 12f); curveTo(6f, 9.4f, 8.6f, 9.4f, 8.6f, 12f)
            moveTo(11f, 13.4f); lineTo(17f, 9.4f); lineTo(17f, 12.2f)
            moveTo(20.4f, 13.6f); lineTo(20.4f, 17.2f)
            moveTo(19.2f, 17.2f); lineTo(21.6f, 17.2f)
        }
    }

    /** A fuel pump. */
    val fuel: ImageVector by lazy {
        icon("fuel") {
            moveTo(5.5f, 9f); lineTo(12.5f, 9f); lineTo(12.5f, 20f); lineTo(5.5f, 20f); close()
            moveTo(7.5f, 11.4f); lineTo(10.5f, 11.4f); lineTo(10.5f, 14f); lineTo(7.5f, 14f); close()
            moveTo(12.5f, 12.2f); lineTo(15.5f, 10f); lineTo(15.5f, 15.4f); lineTo(14f, 15.4f)
            moveTo(3f, 20f); lineTo(21f, 20f)
        }
    }

    /** A battery with its terminals. */
    val battery: ImageVector by lazy {
        icon("battery") {
            moveTo(3.5f, 9f); lineTo(20.5f, 9f); lineTo(20.5f, 17.5f); lineTo(3.5f, 17.5f); close()
            moveTo(7f, 6.2f); lineTo(10f, 6.2f); lineTo(10f, 9f)
            moveTo(14f, 6.2f); lineTo(17f, 6.2f); lineTo(17f, 9f)
            moveTo(7.8f, 11.7f); lineTo(7.8f, 14.7f)
            moveTo(6.3f, 13.2f); lineTo(9.3f, 13.2f)
            moveTo(14.4f, 13.2f); lineTo(17.4f, 13.2f)
        }
    }

    /** A flame: exhaust gas temperature. */
    val flame: ImageVector by lazy {
        icon("flame") {
            moveTo(12f, 3f)
            curveTo(15.4f, 7f, 17.6f, 10.2f, 17.6f, 13.6f)
            curveTo(17.6f, 18f, 15.1f, 21f, 12f, 21f)
            curveTo(8.9f, 21f, 6.4f, 18f, 6.4f, 13.6f)
            curveTo(6.4f, 10.2f, 8.6f, 7f, 12f, 3f)
            close()
        }
    }

    /** A turbine wheel: boost. */
    val turbine: ImageVector by lazy {
        icon("turbine") {
            moveTo(20f, 12f)
            arcToRelative(8f, 8f, 0f, true, true, 0f, -0.01f)
            moveTo(12f, 12f); curveTo(13f, 9.4f, 16f, 8.4f, 18.2f, 9.4f)
            moveTo(12f, 12f); curveTo(14.6f, 13f, 15.6f, 16f, 14.6f, 18.2f)
            moveTo(12f, 12f); curveTo(11f, 14.6f, 8f, 15.6f, 5.8f, 14.6f)
            moveTo(12f, 12f); curveTo(9.4f, 11f, 8.4f, 8f, 9.4f, 5.8f)
        }
    }

    /** Air flowing: intake air. */
    val air: ImageVector by lazy {
        icon("air") {
            moveTo(3.5f, 8f)
            curveTo(5.5f, 6.2f, 7.5f, 9.8f, 9.5f, 8f)
            curveTo(11.5f, 6.2f, 13.5f, 9.8f, 15.5f, 8f)
            curveTo(17.5f, 6.2f, 19.5f, 9.8f, 21f, 8f)
            moveTo(3.5f, 12.5f)
            curveTo(5.5f, 10.7f, 7.5f, 14.3f, 9.5f, 12.5f)
            curveTo(11.5f, 10.7f, 13.5f, 14.3f, 15.5f, 12.5f)
            curveTo(17.5f, 10.7f, 19.5f, 14.3f, 21f, 12.5f)
            moveTo(3.5f, 17f)
            curveTo(5.5f, 15.2f, 7.5f, 18.8f, 9.5f, 17f)
            curveTo(11.5f, 15.2f, 13.5f, 18.8f, 15.5f, 17f)
            curveTo(17.5f, 15.2f, 19.5f, 18.8f, 21f, 17f)
        }
    }

    /** A gauge with its needle: engine load. */
    val gauge: ImageVector by lazy {
        icon("gauge") {
            moveTo(4f, 17f)
            arcTo(8f, 8f, 0f, true, true, 20f, 17f)
            moveTo(12f, 17f); lineTo(16.4f, 11.4f)
            moveTo(11.4f, 17f); lineTo(12.6f, 17f)
        }
    }

    /** An injector with its spray: rail pressure. */
    val injector: ImageVector by lazy {
        icon("injector") {
            moveTo(9.6f, 3f); lineTo(14.4f, 3f); lineTo(14.4f, 10f); lineTo(9.6f, 10f); close()
            moveTo(9.6f, 10f); lineTo(12f, 15.2f); lineTo(14.4f, 10f)
            moveTo(10.4f, 17.6f); lineTo(9.4f, 19.8f)
            moveTo(13.6f, 17.6f); lineTo(14.6f, 19.8f)
            moveTo(12f, 18.4f); lineTo(12f, 21f)
        }
    }
}
