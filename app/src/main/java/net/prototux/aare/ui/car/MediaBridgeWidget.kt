package net.prototux.aare.ui.car

import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import net.prototux.aare.R
import net.prototux.aare.ui.theme.LocalCarDimens

/**
 * The Universal Media Bridge widget: the system media session, not a MediaBrowser library. It works
 * for an app whose library service is closed to third-party hosts (Yandex Music on some builds),
 * as long as it publishes a MediaSession and AARE holds notification access (the framework's rule
 * for MediaSessionManager.getActiveSessions).
 *
 * It shows artwork, title/artist, progress with seeking, play/pause, skip, the session's own
 * custom actions, a source picker (all active sessions) and a way to start the chosen app.
 */
@Composable
internal fun MediaBridgeWidget(
    state: CarUiState,
    actions: CarActions,
    modifier: Modifier,
    compact: Boolean = false,
) {
    val np = state.nowPlaying
    Card(modifier) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(if (compact) 12.dp else 18.dp)) {
            val narrow = maxWidth < 260.dp || maxHeight < 190.dp
            if (np == null) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(if (narrow) 28.dp else 40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.media_bridge_none), style = MaterialTheme.typography.titleSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    SourceRow(state, actions, compact = true)
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Artwork(np.art, if (narrow) 48 else 64)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(np.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(np.subtitle, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (!narrow && np.appLabel != null) {
                            Text(np.appLabel, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (np.durationMs > 0) SeekBar(np, actions)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ControlButton(Icons.Rounded.SkipPrevious, stringResource(R.string.media_action_previous), np.canPrevious) { actions.mediaPrevious() }
                        ControlButton(
                            if (np.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            stringResource(R.string.media_action_play_pause), true, filled = true,
                        ) { actions.mediaPlayPause() }
                        ControlButton(Icons.Rounded.SkipNext, stringResource(R.string.media_action_next), np.canNext) { actions.mediaNext() }
                    }
                    if (!narrow && np.actions.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            np.actions.take(3).forEach { action ->
                                Text(
                                    action.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .carClickable(RoundedCornerShape(50)) { actions.runMediaAction(action.id) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    SourceRow(state, actions, compact = narrow)
                }
            }
        }
    }
}

/** Artwork, or a neutral placeholder: never a blank hole in a widget. */
@Composable
private fun Artwork(bitmap: android.graphics.Bitmap?, sizeDp: Int) {
    val shape = MaterialTheme.shapes.small
    if (bitmap != null) {
        Image(bitmap.asImageBitmap(), null, Modifier.size(sizeDp.dp).clip(shape))
    } else {
        Box(Modifier.size(sizeDp.dp).clip(shape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size((sizeDp * 0.5f).dp))
        }
    }
}

/** Progress and seeking, extrapolated while playing like the full player. */
@Composable
private fun SeekBar(np: NowPlaying, actions: CarActions) {
    var now by remember(np.packageName, np.updatedAtMs) { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(np.playing, np.updatedAtMs) {
        while (np.playing) {
            delay(500)
            now = SystemClock.elapsedRealtime()
        }
    }
    var drag by remember(np.packageName, np.title, np.updatedAtMs) { mutableStateOf<Float?>(null) }
    val progress = mediaProgress(np.positionMs, np.durationMs, np.playing, np.updatedAtMs, now)
    if (np.canSeek) {
        Slider(
            value = drag ?: progress,
            onValueChange = { drag = it },
            onValueChangeFinished = {
                drag?.let { actions.seekMedia((it * np.durationMs).toLong()) }
                drag = null
            },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth().height(28.dp),
        )
    } else {
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)))
    }
}

@Composable
private fun ControlButton(icon: ImageVector, label: String, enabled: Boolean, filled: Boolean = false, onClick: () -> Unit) {
    val size = LocalCarDimens.current.touchTarget.coerceAtMost(64.dp)
    val bg = if (filled) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent
    val fg = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        Modifier.size(size).clip(CircleShape).background(bg)
            .carClickable(CircleShape, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = if (enabled) fg else fg.copy(alpha = 0.35f), modifier = Modifier.size(size * 0.44f))
    }
}

/**
 * Who is playing: the active source, or a pinned app. The menu lists every active session (not
 * only MediaBrowser apps), so a session-only Yandex Music can be chosen, and "Open app" starts it
 * when it hasn't published a session yet.
 */
@Composable
private fun SourceRow(state: CarUiState, actions: CarActions, compact: Boolean) {
    var menu by remember { mutableStateOf(false) }
    val selected = state.mediaBridgePackage
    val sessions = state.mediaSessions
    val label = sessions.firstOrNull { it.packageName == selected }?.label
        ?: state.nowPlaying?.appLabel
        ?: selected
        ?: stringResource(R.string.media_bridge_auto)
    val packageToOpen = selected ?: state.nowPlaying?.packageName
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (!compact) {
            Text(stringResource(R.string.media_bridge_source), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
        }
        Box {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clip(RoundedCornerShape(50)).carClickable(RoundedCornerShape(50)) { menu = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.media_bridge_auto)) }, onClick = {
                    menu = false; actions.selectMediaSource(null)
                })
                sessions.forEach { session ->
                    DropdownMenuItem(
                        text = { Text(session.label + if (session.playing) " ●" else "") },
                        onClick = { menu = false; actions.selectMediaSource(session.packageName) },
                    )
                }
                val activePackages = sessions.mapTo(HashSet()) { it.packageName }
                val startable = state.musicApps.filter { it.packageName !in activePackages }
                if (startable.isNotEmpty()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.media_bridge_installed)) },
                        onClick = {},
                        enabled = false,
                    )
                    startable.forEach { app ->
                        DropdownMenuItem(
                            text = { Text(app.label) },
                            onClick = {
                                menu = false
                                actions.selectMediaSource(app.packageName)
                                actions.openMediaApp(app.packageName)
                            },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(40.dp).clip(CircleShape).carClickable(CircleShape) {
                if (packageToOpen != null) actions.openMediaApp(packageToOpen) else actions.openLauncher()
            },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.OpenInNew, stringResource(R.string.media_bridge_open),
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}
