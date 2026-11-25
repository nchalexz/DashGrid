package net.prototux.aare.workspace

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import net.prototux.aare.R
import net.prototux.aare.host.CarAppCatalog
import net.prototux.aare.host.CarRuntime

/**
 * An app the phone's workspace editor can add as a shortcut (or as a map/music widget's app).
 * [id] is what gets stored in [WorkspaceItem.appId]: a package name for a Car App Library app
 * (stable across devices, and visible to the phone without a car session), or a built-in entry id
 * (`CarRuntime.PHONE_ID` and friends), which the runtime resolves in `CarUiState.workspaceApp`.
 */
data class WorkspaceAppChoice(
    val id: String,
    val label: String,
    val icon: Bitmap?,
    /** The app qualifies for the car's navigation role (so it can be a map widget's app). */
    val navigation: Boolean,
    val builtin: Boolean,
)

/**
 * What the phone's editor offers: DashGrid's built-in car apps and the Car App Library apps
 * installed on this phone. Media apps that only exist as a media session aren't listed here; a
 * music widget follows what is playing unless it is given one of these apps.
 */
object WorkspaceCatalog {

    fun apps(context: Context, iconSizePx: Int = 96): List<WorkspaceAppChoice> {
        val builtins = listOf(
            builtin(context, CarRuntime.PHONE_ID, R.string.setup_app_entry_phone, R.drawable.ic_car_phone, iconSizePx),
            builtin(context, CarRuntime.NOTIFICATIONS_ID, R.string.setup_app_entry_notifications, R.drawable.ic_car_notifications, iconSizePx),
            builtin(context, CarRuntime.SETTINGS_ID, R.string.setup_app_entry_settings, R.drawable.ic_car_settings, iconSizePx),
            builtin(context, CarRuntime.BROWSER_ID, R.string.browser_app_label, R.drawable.ic_car_browser, iconSizePx),
            builtin(context, CarRuntime.CAR_DATA_ID, R.string.car_data_app_label, R.drawable.ic_car_data, iconSizePx),
            builtin(context, CarRuntime.HOME_ID, R.string.home_app_label, R.drawable.ic_car_home, iconSizePx),
            builtin(context, CarRuntime.OBD_ID, R.string.obd_app_label, R.drawable.ic_car_obd, iconSizePx),
            builtin(context, CarRuntime.PHONE_SCREEN_ID, R.string.phone_section_title, R.drawable.ic_phone_screen, iconSizePx),
        )
        val known = builtins.mapTo(HashSet()) { it.id }
        val carApps = CarAppCatalog.query(context, iconSizePx)
            .map { info ->
                WorkspaceAppChoice(
                    id = info.packageName,
                    label = info.label,
                    icon = info.icon,
                    navigation = info.isNavigation,
                    builtin = false,
                )
            }
            .filter { it.id !in known }
            .distinctBy { it.id }
            .sortedBy { it.label.lowercase() }
        val carPackages = carApps.mapTo(HashSet()) { it.id }
        val mediaApps = mediaApps(context, iconSizePx)
            .filter { it.packageName !in carPackages }
            .sortedBy { it.choice.label.lowercase() }
            .map { it.choice }
        return builtins + carApps + mediaApps
    }

    /** A media app found through its MediaBrowserService, with the entry id the runtime gives it. */
    private class MediaFound(val packageName: String, val choice: WorkspaceAppChoice)

    /**
     * Apps that publish a media browser service: the music apps AARE's media screens can drive even
     * when they have no car app of their own. Their stored id is `media:<package>`, the id
     * CarRuntime gives that app's entry, so a shortcut opens the same session the music card does.
     */
    private fun mediaApps(context: Context, iconSizePx: Int): List<MediaFound> {
        val pm = context.packageManager
        val intent = Intent("android.media.browse.MediaBrowserService")
        val flags = PackageManager.GET_META_DATA or PackageManager.GET_RESOLVED_FILTER
        val found = runCatching {
            if (Build.VERSION.SDK_INT >= 33) pm.queryIntentServices(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
            else @Suppress("DEPRECATION") pm.queryIntentServices(intent, flags)
        }.getOrDefault(emptyList())
        return found.mapNotNull { ri ->
            val si = ri.serviceInfo ?: return@mapNotNull null
            val app = si.applicationInfo ?: return@mapNotNull null
            val pkg = si.packageName
            if (pkg == context.packageName) return@mapNotNull null
            val label = runCatching { pm.getApplicationLabel(app).toString() }.getOrNull() ?: pkg
            val icon = runCatching { pm.getApplicationIcon(app).toBitmap(iconSizePx, iconSizePx) }.getOrNull()
            MediaFound(
                packageName = pkg,
                choice = WorkspaceAppChoice(
                    id = "media:$pkg",
                    label = label,
                    icon = icon,
                    navigation = false,
                    builtin = false,
                ),
            )
        }.distinctBy { it.packageName }
    }

    private fun builtin(context: Context, id: String, labelRes: Int, iconRes: Int, sizePx: Int) = WorkspaceAppChoice(
        id = id,
        label = context.getString(labelRes),
        icon = runCatching { ContextCompat.getDrawable(context, iconRes)?.toBitmap(sizePx, sizePx) }.getOrNull(),
        navigation = false,
        builtin = true,
    )
}
