package net.prototux.aare.media

import android.content.ComponentName
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A media app found on the phone (media3 MediaLibraryService or legacy MediaBrowserService). */
data class MediaApp(
    val packageName: String,
    val service: ComponentName,
    val label: String,
    val icon: Bitmap?,
)

/** What is playing, from whichever media app is active. */
data class PlaybackInfo(
    val packageName: String,
    val title: String,
    val artist: String,
    val album: String,
    val art: Bitmap?,
    val playing: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    /** SystemClock.elapsedRealtime() when positionMs was sampled, to extrapolate progress. */
    val sampledAtMs: Long,
    /** The active source can be seeked; [positionMs] and [durationMs] are meaningful. */
    val canSeek: Boolean = false,
    val canPrevious: Boolean = false,
    val canNext: Boolean = false,
    /** Buttons the media session itself offers (media3 custom layout / legacy custom actions). */
    val actions: List<MediaActionInfo> = emptyList(),
    /** The app's label, for the source picker; null when the app is not installed. */
    val label: String? = null,
)

/** One transport button a media app exposes beyond play/pause and skip. */
data class MediaActionInfo(val id: String, val label: String)

/** An app with an active media session: what the universal bridge's source picker lists. */
data class MediaSessionApp(
    val packageName: String,
    val label: String,
    val playing: Boolean,
)

/**
 * A fallback for apps whose MediaBrowser library is closed to third-party hosts (Yandex Music on
 * some builds): the system media session still gives metadata, artwork, progress, transport and
 * custom actions, and a session-only app can be started from its launcher intent.
 */
data class UniversalMediaState(
    val packageName: String?,
    val label: String?,
    val title: String,
    val artist: String,
    val album: String,
    val art: Bitmap?,
    val playing: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val sampledAtMs: Long,
    val canSeek: Boolean,
    val canPrevious: Boolean,
    val canNext: Boolean,
    val actions: List<MediaActionInfo>,
    /** Other active media sessions, for the source picker. */
    val sessions: List<MediaSessionApp>,
)

/**
 * Media side of the car: discovery, one connection to the active media app, transport controls,
 * and the full-screen browse/now-playing UI.
 *
 * Implemented in this package. All methods are called on the main thread.
 */
interface MediaHub {
    /** Media apps installed on the phone; refreshed on package changes. */
    val apps: StateFlow<List<MediaApp>>

    /** The current playback, from the connected app or any active media session. */
    val playback: StateFlow<PlaybackInfo?>

    /** Connects to [app] (if not already) and makes it the active one. */
    fun select(app: MediaApp)

    /** Active media sessions the universal bridge can show, beyond the connected browser app. */
    val sessions: StateFlow<List<MediaSessionApp>> get() = MutableStateFlow(emptyList())

    /** Installed music players, including session-only apps with no active session yet. */
    val musicApps: StateFlow<List<MediaSessionApp>> get() = MutableStateFlow(emptyList())

    /** Follow a session-only app by package (null: automatic, the active player). */
    fun selectSessionPackage(packageName: String?) {}

    /** Seek the source the bridge currently shows. */
    fun seekTo(positionMs: Long) {}

    /** Run one of [PlaybackInfo.actions] of the current source. */
    fun runAction(actionId: String) {}

    /** Start an installed music app on the phone, for a session-only app that isn't running yet. */
    fun launchApp(packageName: String) {}

    /**
     * Waits until the connection to the selected app is ready, at most [timeoutMs]. Also true when the app is
     * connected but its library did not load in time; false if it couldn't connect, there is none, or it timed out.
     * The default, for hubs without a connection to wait for, is false.
     */
    suspend fun awaitConnection(timeoutMs: Long): Boolean = false

    fun playPause()
    fun next()
    fun previous()
    fun pause()

    /**
     * Full-screen UI for an app: browse tree (list/grid per content style), search, now playing.
     * [onShowVideo], when set, adds a "Show video" action to its top bar.
     */
    @Composable
    fun Screen(app: MediaApp, driving: Boolean, modifier: Modifier, onShowVideo: (() -> Unit)?)

    /** Disconnects everything. */
    fun release()
}
