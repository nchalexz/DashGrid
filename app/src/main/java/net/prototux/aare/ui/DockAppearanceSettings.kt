package net.prototux.aare.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.ui.theme.DockIconPreview
import net.prototux.aare.ui.theme.DockIconTone
import net.prototux.aare.workspace.WorkspaceCatalog

/**
 * The "Dock icons" and "Navigation buttons" groups of the car-screen settings: the monochrome
 * switch, the day and night tones, a preview of real icons in every tone, and the two independent
 * fixed buttons. Everything writes to [Settings] at once, so the car screen (a running session
 * included) picks it up through its settings listener, with no restart and no reconnect.
 */
@Composable
internal fun DockAppearanceSettings(settings: Settings) {
    var monochrome by remember { mutableStateOf(settings.dockMonochrome) }
    var light by remember { mutableStateOf(DockIconTone.byId(settings.dockToneLight) ?: DockIconTone.DARK_GREY) }
    var dark by remember { mutableStateOf(DockIconTone.byId(settings.dockToneDark) ?: DockIconTone.WHITE) }
    var alwaysHome by remember { mutableStateOf(settings.alwaysShowHomeButton) }
    var alwaysApps by remember { mutableStateOf(settings.alwaysShowAppDashButton) }
    val ctx = LocalContext.current
    // The user's own icons: the car apps and media apps on this phone, then ordinary launcher
    // apps, so the preview is the real artwork and not a drawn sample.
    val icons by produceState(initialValue = emptyList<Bitmap>(), ctx) {
        value = withContext(Dispatchers.IO) { previewIcons(ctx) }
    }

    Section(stringResource(R.string.setup_section_dock)) {
        Toggle(
            stringResource(R.string.dock_monochrome_title),
            stringResource(R.string.dock_monochrome_subtitle),
            monochrome,
        ) { on ->
            monochrome = on
            settings.dockMonochrome = on
        }
        Spacer(Modifier.height(12.dp))
        ToneChoice(stringResource(R.string.dock_tone_light_title), light, icons) { tone ->
            light = tone
            settings.dockToneLight = tone.id
        }
        Spacer(Modifier.height(12.dp))
        ToneChoice(stringResource(R.string.dock_tone_dark_title), dark, icons) { tone ->
            dark = tone
            settings.dockToneDark = tone.id
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.dock_tone_preview_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        TonePreviewRow(stringResource(R.string.dock_tone_preview_light), icons, light)
        Spacer(Modifier.height(8.dp))
        TonePreviewRow(stringResource(R.string.dock_tone_preview_dark), icons, dark)
    }
    Spacer(Modifier.height(16.dp))
    Section(stringResource(R.string.setup_section_navigation)) {
        Toggle(
            stringResource(R.string.dock_always_home_title),
            stringResource(R.string.dock_always_home_subtitle),
            alwaysHome,
        ) { on ->
            alwaysHome = on
            settings.alwaysShowHomeButton = on
        }
        Toggle(
            stringResource(R.string.dock_always_app_dash_title),
            stringResource(R.string.dock_always_app_dash_subtitle),
            alwaysApps,
        ) { on ->
            alwaysApps = on
            settings.alwaysShowAppDashButton = on
        }
    }
}

/** One tone setting: four cells, each drawing the phone's real icons in that tone, the chosen one marked. */
@Composable
private fun ToneChoice(title: String, selected: DockIconTone, icons: List<Bitmap>, onSelect: (DockIconTone) -> Unit) {
    Text(title, style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DockIconTone.entries.forEach { tone ->
            val isSelected = tone == selected
            Column(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                    .clickable { onSelect(tone) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (icons.isEmpty()) {
                        Box(Modifier.size(24.dp))
                    } else {
                        icons.take(2).forEach { icon ->
                            DockIconPreview(icon, tone, null, Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)))
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(tone.labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** A strip of the phone's real icons drawn in [tone], under the name of the theme it belongs to. */
@Composable
private fun TonePreviewRow(title: String, icons: List<Bitmap>, tone: DockIconTone) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(width = 92.dp, height = 24.dp))
        if (icons.isEmpty()) {
            Text(stringResource(R.string.dock_tone_no_icons), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                icons.take(6).forEach { icon ->
                    DockIconPreview(icon, tone, null, Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)))
                }
            }
        }
    }
}

/**
 * Up to six real icons from this phone: the car apps and media apps the editor already knows,
 * then ordinary launchable apps (2GIS, a navigator, a music player…) — whatever is installed.
 * Bitmaps are small (96 px), so the whole preview costs a few hundred kilobytes.
 */
private fun previewIcons(context: android.content.Context): List<Bitmap> {
    val found = ArrayList<Bitmap>()
    runCatching { WorkspaceCatalog.apps(context, 96) }.getOrDefault(emptyList())
        .mapNotNull { it.icon }
        .forEach { icon -> if (found.size < 6 && found.none { it.sameAs(icon) }) found.add(icon) }
    if (found.size >= 6) return found
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolved = runCatching {
        if (Build.VERSION.SDK_INT >= 33) pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        else @Suppress("DEPRECATION") pm.queryIntentActivities(intent, 0)
    }.getOrDefault(emptyList())
    resolved.mapNotNull { ri ->
        val ai = ri.activityInfo?.applicationInfo ?: return@mapNotNull null
        runCatching { pm.getApplicationIcon(ai).toBitmap(96, 96) }.getOrNull()
    }.forEach { icon ->
        if (found.size < 6) found.add(icon)
    }
    // The launcher list may repeat the same app under several activities: keep the order, drop
    // the exact duplicates by pixel equality (cheap at 96 px and only for the preview).
    val unique = ArrayList<Bitmap>()
    for (icon in found) if (unique.none { existing -> existing.sameAs(icon) }) unique.add(icon)
    return unique.take(6).ifEmpty { found.take(6) }
}
