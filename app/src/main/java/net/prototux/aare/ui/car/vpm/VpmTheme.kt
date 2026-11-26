package net.prototux.aare.ui.car.vpm

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import net.prototux.aare.R

/**
 * VPM's cockpit tokens, copied from its 0.4.1 `DashboardTheme`: the same graphite panels, thin
 * rings and white numbers by night, the same non-white daylight palette so the screen doesn't blind
 * the driver. The car screen's own Material theme is not used inside the cockpit.
 */
@Immutable
data class VpmColors(
    val background: Color,
    val panel: Color,
    val card: Color,
    val border: Color,
    val text: Color,
    val dim: Color,
    val tick: Color,
    val ring: Color,
    val accent: Color,
    val warning: Color,
    val critical: Color,
    val dialFace: Color,
    val needle: Color,
)

/** VPM's night cockpit: graphite, thin scales, white digits. */
val VpmNight = VpmColors(
    background = Color(0xFF10151B),
    panel = Color(0xFF141D24),
    card = Color(0xFF1A1F25),
    border = Color(0xFF2A3037),
    text = Color(0xFFF2F4F6),
    dim = Color(0xFF89939E),
    tick = Color(0xFF59636D),
    ring = Color(0xFFB8C0C8),
    accent = Color(0xFF54B6AD),
    warning = Color(0xFFE5AE54),
    critical = Color(0xFFD96060),
    dialFace = Color(0xFF0B1015),
    needle = Color(0xFFF2F4F6),
)

/** VPM's daylight cockpit: separate tokens, not an inverted image. */
val VpmDay = VpmColors(
    background = Color(0xFFE8ECEF),
    panel = Color(0xFFF6F8FA),
    card = Color(0xFFFFFFFF),
    border = Color(0xFFC3CBD3),
    text = Color(0xFF14191E),
    dim = Color(0xFF55606B),
    tick = Color(0xFF8A949E),
    ring = Color(0xFF4E5964),
    accent = Color(0xFF0F766E),
    warning = Color(0xFF9A5B00),
    critical = Color(0xFFB3261E),
    dialFace = Color(0xFFE1E7EC),
    needle = Color(0xFF1A2027),
)

val LocalVpmColors = staticCompositionLocalOf { VpmNight }

/** Titillium Web (OFL) is already in DashGrid's fonts; it gives the thin digits of the mockups. */
val VpmFont = FontFamily(
    Font(R.font.titillium_web_light, FontWeight.Light),
    Font(R.font.titillium_web_regular, FontWeight.Normal),
    Font(R.font.titillium_web_semibold, FontWeight.SemiBold),
)

/** Draws the cockpit in the night or day palette, following the car screen's current theme. */
@Composable
fun VpmTheme(dark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalVpmColors provides if (dark) VpmNight else VpmDay, content = content)
}
