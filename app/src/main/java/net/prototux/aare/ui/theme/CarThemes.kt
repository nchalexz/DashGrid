package net.prototux.aare.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** The built-in themes. A new theme is a new entry here (and in the settings' choices). */
object CarThemes {
    val all: List<CarThemeSpec> get() = listOf(DashGridTheme, AareTheme, AndroidAutoTheme, AndroidAuto2019Theme, CarPlayTheme, MaterialYouTheme, HighContrastTheme, NightTheme, RetroTheme, FrostedGlassTheme, DarkGlassTheme)

    /** The themes this phone can show: Material You needs Android 12. */
    val available: List<CarThemeSpec>
        get() = all.filter { !it.phoneColors || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }

    /** The theme stored under [id], or AARE's for an unknown or removed one. */
    fun byId(id: String?): CarThemeSpec = all.firstOrNull { it.id == id } ?: AareTheme
}

/**
 * DashGrid's own look: graphite surfaces (#101418, #1B2229, #252E36) and one turquoise accent
 * (#36D6C3), with the text colours of the brand. The palette is repeated here as the car screen's
 * design tokens; [DashGridColors] names them for composables that shouldn't pick raw hex values.
 */
object DashGridColors {
    val Background = Color(0xFF101418)
    val Surface = Color(0xFF1B2229)
    val SurfaceHigh = Color(0xFF252E36)
    val Accent = Color(0xFF36D6C3)
    val Text = Color(0xFFF1F5F9)
    val TextMuted = Color(0xFF94A3B8)
}

val DashGridTheme: CarThemeSpec = run {
    val accent = DashGridColors.Accent
    CarThemeSpec(
        id = "dashgrid",
        name = "DashGrid",
        night = darkColorScheme(
            primary = accent,
            onPrimary = Color(0xFF00201C),
            primaryContainer = Color(0xFF0E3B36),
            onPrimaryContainer = Color(0xFFB9F5EA),
            secondary = Color(0xFF9FB4BD),
            onSecondary = Color(0xFF15242B),
            secondaryContainer = DashGridColors.SurfaceHigh,
            onSecondaryContainer = Color(0xFFDCE6EA),
            tertiary = Color(0xFFA7C4D4),
            onTertiary = Color(0xFF0B1A22),
            background = DashGridColors.Background,
            onBackground = DashGridColors.Text,
            surface = DashGridColors.Background,
            onSurface = DashGridColors.Text,
            surfaceVariant = DashGridColors.Surface,
            onSurfaceVariant = DashGridColors.TextMuted,
            surfaceContainerLowest = Color(0xFF0B0E12),
            surfaceContainerLow = Color(0xFF151A20),
            surfaceContainer = DashGridColors.Surface,
            surfaceContainerHigh = DashGridColors.SurfaceHigh,
            surfaceContainerHighest = Color(0xFF2E3942),
            outline = Color(0xFF55636E),
            outlineVariant = Color(0xFF2A343D),
            error = Color(0xFFFF8A80),
        ),
        day = lightColorScheme(
            primary = Color(0xFF0E7C70),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFB9F5EA),
            onPrimaryContainer = Color(0xFF00201C),
            secondary = Color(0xFF4A5C66),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFD7E3E8),
            onSecondaryContainer = Color(0xFF0B1A22),
            tertiary = Color(0xFF3F5C6B),
            onTertiary = Color.White,
            background = Color(0xFFF4F7F9),
            onBackground = Color(0xFF101418),
            surface = Color(0xFFF4F7F9),
            onSurface = Color(0xFF101418),
            surfaceVariant = Color(0xFFDCE4E9),
            onSurfaceVariant = Color(0xFF47545E),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFEDF2F5),
            surfaceContainer = Color(0xFFE6ECF0),
            surfaceContainerHigh = Color(0xFFDEE7EC),
            surfaceContainerHighest = Color(0xFFD5E0E6),
            outline = Color(0xFF6C7A84),
            outlineVariant = Color(0xFFC3D0D7),
            error = Color(0xFFB3261E),
        ),
        nightColors = CarColors(
            accept = Color(0xFF2E7D32),
            decline = Color(0xFFD32F2F),
            onCall = Color.White,
            red = Color(0xFFFF7A70),
            green = Color(0xFF6EDB8F),
            blue = Color(0xFF82B4FF),
            yellow = Color(0xFFFFD45C),
            inkOnLight = Color(0xFF101418),
        ),
        dayColors = CarColors(
            accept = Color(0xFF2E7D32),
            decline = Color(0xFFD32F2F),
            onCall = Color.White,
            red = Color(0xFFC62828),
            green = Color(0xFF1B7F3B),
            blue = Color(0xFF1A5FBF),
            yellow = Color(0xFF8A6A00),
            inkOnLight = Color(0xFF101418),
        ),
        typography = CarTypography,
        shapes = CarShapes,
        appIcon = RoundedCornerShape(24.dp),
    )
}

/** AARE's own look: calm dark surfaces, one river-teal accent (the Aare is famously turquoise). */
val AareTheme: CarThemeSpec = run {
    val teal = Color(0xFF3DDBC6)
    val tealDeep = Color(0xFF00695C)
    CarThemeSpec(
        id = "aare",
        name = "AARE",
        night = darkColorScheme(
            primary = teal,
            onPrimary = Color(0xFF00201C),
            primaryContainer = Color(0xFF0E3B36),
            onPrimaryContainer = Color(0xFFB9F5EA),
            secondary = Color(0xFFB4CAC5),
            onSecondary = Color(0xFF1F3531),
            secondaryContainer = Color(0xFF263330),
            onSecondaryContainer = Color(0xFFD0E6E1),
            tertiary = Color(0xFFFFB86B),
            onTertiary = Color(0xFF2E1500),
            background = Color(0xFF0B0F0F),
            onBackground = Color(0xFFE3E9E7),
            surface = Color(0xFF0B0F0F),
            onSurface = Color(0xFFE3E9E7),
            surfaceVariant = Color(0xFF1A2120),
            onSurfaceVariant = Color(0xFFAFBAB7),
            surfaceContainerLowest = Color(0xFF060909),
            surfaceContainerLow = Color(0xFF121818),
            surfaceContainer = Color(0xFF161D1C),
            surfaceContainerHigh = Color(0xFF1D2524),
            surfaceContainerHighest = Color(0xFF26302E),
            outline = Color(0xFF5E6B68),
            outlineVariant = Color(0xFF2E3836),
            error = Color(0xFFFF8A80),
        ),
        day = lightColorScheme(
            primary = tealDeep,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFB9F5EA),
            onPrimaryContainer = Color(0xFF00201C),
            secondary = Color(0xFF4A635F),
            secondaryContainer = Color(0xFFCCE8E2),
            onSecondaryContainer = Color(0xFF06201C),
            tertiary = Color(0xFF8B5000),
            background = Color(0xFFF3F7F6),
            onBackground = Color(0xFF161D1C),
            surface = Color(0xFFF3F7F6),
            onSurface = Color(0xFF161D1C),
            surfaceVariant = Color(0xFFDAE5E2),
            onSurfaceVariant = Color(0xFF3F4947),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFECF2F0),
            surfaceContainer = Color(0xFFE6ECEA),
            surfaceContainerHigh = Color(0xFFE0E7E5),
            surfaceContainerHighest = Color(0xFFD9E1DF),
            outline = Color(0xFF6F7977),
            outlineVariant = Color(0xFFBEC9C6),
        ),
        nightColors = CarColors(
            accept = Color(0xFF2E7D32),
            decline = Color(0xFFD32F2F),
            onCall = Color.White,
            red = Color(0xFFFF7A70),
            green = Color(0xFF6EDB8F),
            blue = Color(0xFF82B4FF),
            yellow = Color(0xFFFFD45C),
            inkOnLight = Color(0xFF101414),
        ),
        dayColors = CarColors(
            accept = Color(0xFF2E7D32),
            decline = Color(0xFFD32F2F),
            onCall = Color.White,
            red = Color(0xFFC62828),
            green = Color(0xFF1B7F3B),
            blue = Color(0xFF1A5FBF),
            yellow = Color(0xFF8A6A00),
            inkOnLight = Color(0xFF101414),
        ),
        typography = CarTypography,
        shapes = CarShapes,
    )
}
