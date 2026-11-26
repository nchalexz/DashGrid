package net.prototux.aare.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.drawable.toBitmap

/**
 * One Android App Widget the phone's editor can add: the provider's own identity, the labels and
 * preview a person chooses by, and the smallest cells its provider asks for.
 */
data class AndroidWidgetChoice(
    val component: ComponentName,
    val packageName: String,
    val appLabel: String,
    val widgetLabel: String,
    val appIcon: Bitmap?,
    val preview: Bitmap?,
    val minWidthDp: Int,
    val minHeightDp: Int,
    /** The provider reflows to the cells it is given; otherwise it keeps its minimum size. */
    val resizable: Boolean,
    /** The provider's own setup screen, if it has one (opened right after binding). */
    val hasConfiguration: Boolean,
) {
    /** The flattened component stored in [net.prototux.aare.workspace.WorkspaceItem.appWidgetProvider]. */
    val providerKey: String get() = component.flattenToString()
}

/**
 * Every standard App Widget installed on this phone, from every app that publishes one. The list is
 * the system's own ([AppWidgetManager.getInstalledProviders]); nothing is filtered by package, so a
 * widget that isn't in some hard-coded catalogue still appears.
 */
object AndroidWidgetCatalog {
    private const val TAG = "WidgetCatalog"

    fun choices(context: Context, iconSizePx: Int = 96): List<AndroidWidgetChoice> {
        val manager = AppWidgetManager.getInstance(context)
        val pm = context.packageManager
        val density = context.resources.displayMetrics.densityDpi
        val appCache = HashMap<String, Pair<String, Bitmap?>>()
        fun app(pkg: String): Pair<String, Bitmap?> = appCache.getOrPut(pkg) {
            val label = runCatching {
                val info = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(info).toString() to runCatching { pm.getApplicationIcon(info).toBitmap(iconSizePx, iconSizePx) }.getOrNull()
            }.getOrNull()
            label ?: (pkg to null)
        }
        return runCatching { manager.installedProviders }
            .onFailure { Log.w(TAG, "installedProviders: ${it.message}") }
            .getOrDefault(emptyList())
            .mapNotNull { info -> choice(context, info, density, ::app) }
            .distinctBy { it.providerKey }
            .sortedWith(compareBy<AndroidWidgetChoice> { it.appLabel.lowercase() }.thenBy { it.widgetLabel.lowercase() })
    }

    private fun choice(
        context: Context,
        info: AppWidgetProviderInfo,
        density: Int,
        app: (String) -> Pair<String, Bitmap?>,
    ): AndroidWidgetChoice? {
        val component = info.provider ?: return null
        val (appLabel, appIcon) = app(component.packageName)
        val widgetLabel = runCatching { info.label }.getOrNull()?.takeIf { it.isNotBlank() } ?: appLabel
        // The provider's preview is optional; a missing one must not hide the widget from the list.
        val preview = runCatching { info.loadPreviewImage(context, density)?.toBitmap(240, 240) }.getOrNull()
        return AndroidWidgetChoice(
            component = component,
            packageName = component.packageName,
            appLabel = appLabel,
            widgetLabel = widgetLabel,
            appIcon = appIcon,
            preview = preview,
            minWidthDp = info.minWidth.coerceAtLeast(1),
            minHeightDp = info.minHeight.coerceAtLeast(1),
            resizable = info.resizeMode != AppWidgetProviderInfo.RESIZE_NONE,
            hasConfiguration = info.configure != null,
        )
    }
}
