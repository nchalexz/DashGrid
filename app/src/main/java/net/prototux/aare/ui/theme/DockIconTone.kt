package net.prototux.aare.ui.theme

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.os.Build
import android.util.LruCache
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import net.prototux.aare.R

/**
 * How the dock's monochrome icons are toned, after their colour is removed. A tone is a
 * *tonality*, not a fill: the icon keeps its shape, its logo, its internal detail and its alpha;
 * only the grayscale is mapped into a lighter or darker range. The mapping is the same for every
 * app, so two icons look like they belong to one set.
 *
 * The values are a linear luminance remap (out = contrast × luminance + offset) folded into one
 * ColorMatrix, which the GPU applies. The offsets keep a dark logo readable on a light background
 * (and a light one on a dark background) instead of crushing it to black or white.
 *
 * The ids are stored in the settings: never change one once released.
 */
enum class DockIconTone(val id: String, @StringRes val labelRes: Int, val contrast: Float, val offset: Float) {
    /** A light grayscale for dark backgrounds: detail survives in the top half of the range. */
    WHITE("white", R.string.dock_tone_white, contrast = 0.62f, offset = 0.40f),

    /** Plain grayscale, as a monochrome screen would show it. */
    GREY("grey", R.string.dock_tone_grey, contrast = 1f, offset = 0f),

    /** A darkened grayscale for light backgrounds: darker than mid grey, detail still separated. */
    DARK_GREY("dark_grey", R.string.dock_tone_dark_grey, contrast = 0.58f, offset = 0.07f),

    /**
     * The darkest grayscale that still keeps lines and highlights apart: for a very light
     * wallpaper, where a mid-grey icon would wash out.
     */
    BLACK("black", R.string.dock_tone_black, contrast = 0.30f, offset = 0.015f);

    companion object {
        fun byId(id: String?): DockIconTone? = entries.firstOrNull { it.id == id }
    }
}

/** The dock's icon treatment, provided by the layout that draws the dock. */
@Immutable
data class DockIconStyle(
    val monochrome: Boolean = false,
    val toneLight: DockIconTone = DockIconTone.DARK_GREY,
    val toneDark: DockIconTone = DockIconTone.WHITE,
) {
    /** The tone for the colours now applied (the dock can be forced dark by the "dark bar" setting). */
    fun tone(night: Boolean): DockIconTone = if (night) toneDark else toneLight
}

val LocalDockIconStyle = compositionLocalOf { DockIconStyle() }

/**
 * Processed dock icons, so dragging the workspace pager or opening the app grid doesn't run the
 * same ColorMatrix over the same bitmap on every frame. Keyed by the bitmap itself (identity and
 * generation) and the tone; an entry is dropped when the cache grows past [MAX_ENTRIES]. The
 * originals are never modified.
 */
object DockIconCache {
    private const val MAX_ENTRIES = 64

    private val cache = object : LruCache<String, Bitmap>(MAX_ENTRIES) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    /** [source] as grayscale in [tone]; the same bitmap for the same icon and tone while cached. */
    fun process(source: Bitmap, tone: DockIconTone): Bitmap {
        if (source.isRecycled) return source
        val key = "${System.identityHashCode(source)}:${source.generation()}:${source.width}x${source.height}:${tone.id}"
        cache.get(key)?.takeIf { !it.isRecycled }?.let { return it }
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(source, 0f, 0f, Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            colorFilter = ColorMatrixColorFilter(tone.colorMatrix())
        })
        cache.put(key, out)
        return out
    }

    /** Clears the cache (tests, and a theme change that would otherwise mix tones). */
    fun clear() = cache.evictAll()
}

/** A bitmap's generation, for cache keys; 0 where the API doesn't expose one (Android 7 and older). */
internal fun Bitmap.generation(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) generationId else 0

/**
 * The matrix [tone] needs, as plain floats: saturation 0 (the same luminance in all three colour
 * channels) and then the tone's luminance range. Kept separate from [ColorMatrix] so the maths is
 * testable on the JVM, where the android.graphics classes are stubs.
 */
internal fun DockIconTone.matrixArray(): FloatArray {
    // Luminance weights of sRGB, as used by grayscale(); each output channel gets the same value.
    val r = 0.2126f * contrast
    val g = 0.7152f * contrast
    val b = 0.0722f * contrast
    val shift = offset * 255f
    return floatArrayOf(
        r, g, b, 0f, shift,
        r, g, b, 0f, shift,
        r, g, b, 0f, shift,
        0f, 0f, 0f, 1f, 0f,
    )
}

/** The one ColorMatrix a [DockIconTone] needs, for the GPU to apply while the icon is drawn. */
internal fun DockIconTone.colorMatrix(): ColorMatrix = ColorMatrix(matrixArray())

/**
 * An app icon as the dock draws it: monochrome in the current theme's tone when the setting is on,
 * else its own colours (and, on a single-colour theme, that theme's tint — the behaviour before
 * this setting existed). Only dock icons use this; the app grid and the workspaces keep the
 * original artwork.
 */
@Composable
fun DockAppIcon(icon: Bitmap, label: String, modifier: Modifier) {
    val style = LocalDockIconStyle.current
    if (style.monochrome) {
        val tone = style.tone(LocalCarNight.current)
        val processed = remember(icon, icon.generation(), tone) { DockIconCache.process(icon, tone) }
        Image(processed.asImageBitmap(), label, modifier)
    } else {
        Image(icon.asImageBitmap(), label, modifier, colorFilter = appIconFilter())
    }
}

/** One real icon processed in [tone], for the settings preview; never cached across tone changes. */
@Composable
fun DockIconPreview(icon: Bitmap, tone: DockIconTone, label: String?, modifier: Modifier) {
    val processed = remember(icon, icon.generation(), tone) { DockIconCache.process(icon, tone) }
    Image(processed.asImageBitmap(), label, modifier)
}
