package net.prototux.aare.media

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Creates the media hub for a car session. Call on the main thread; [scope] bounds its lifetime
 * (cancelling it stops background work), but call [MediaHub.release] to disconnect cleanly.
 */
fun MediaHub(context: Context, scope: CoroutineScope): MediaHub = DefaultMediaHub(context, scope)

/**
 * The [MediaHub]: [MediaDiscovery] for the app list, one [MediaConnection] to the selected app,
 * a [SessionTracker] for every other session, and [pickSource] to decide what "now playing" and
 * the transport buttons follow.
 */
class DefaultMediaHub(context: Context, scope: CoroutineScope) : MediaHub {
    private val context = context.applicationContext
    private val scope = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]) + Dispatchers.Main.immediate)

    internal val art = ArtLoader(this.context)
    private val discovery = MediaDiscovery(this.context, this.scope)
    private val tracker = SessionTracker(this.context, this.scope, art)

    private val _connection = MutableStateFlow<MediaConnection?>(null)
    internal val connection: StateFlow<MediaConnection?> = _connection.asStateFlow()

    /** Browse/now-playing UI state of the current connection, kept while the screen is away. */
    private var ui: MediaUi? = null

    private val _current = MutableStateFlow<PlaybackSource?>(null)
    private val _playback = MutableStateFlow<PlaybackInfo?>(null)
    private val _sessions = MutableStateFlow<List<MediaSessionApp>>(emptyList())
    private val _preferred = MutableStateFlow<String?>(null)

    /** Active sessions, for the universal bridge's source picker (also includes our connection). */
    override val sessions: StateFlow<List<MediaSessionApp>> = _sessions.asStateFlow()

    private val _musicApps = MutableStateFlow<List<MediaSessionApp>>(emptyList())
    override val musicApps: StateFlow<List<MediaSessionApp>> = _musicApps.asStateFlow()

    override val apps: StateFlow<List<MediaApp>> = discovery.apps

    /** Apps declaring `<uses name="media"/>` for Android Auto; [apps] also has the others. */
    val carMediaApps: StateFlow<List<MediaApp>> = discovery.carMediaApps

    /** False while we lack notification access, i.e. only our own connection is tracked. */
    val sessionAccess: StateFlow<Boolean> = tracker.available

    override val playback: StateFlow<PlaybackInfo?> = _playback.asStateFlow()

    init {
        discovery.start()
        tracker.start()
        scope.launch { followSources() }
        scope.launch {
            _musicApps.value = withContext(Dispatchers.IO) { installedMusicApps() }
        }
    }

    /**
     * Installed players the bridge can offer to start, even before they publish a session. Media
     * Browser services are added by [MediaDiscovery]; the launcher category catches session-only
     * apps (Yandex Music on builds with no exported library service).
     */
    private fun installedMusicApps(): List<MediaSessionApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC)
        val flags = PackageManager.MATCH_DEFAULT_ONLY
        val found = runCatching {
            if (Build.VERSION.SDK_INT >= 33) pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            else @Suppress("DEPRECATION") pm.queryIntentActivities(intent, 0)
        }.getOrDefault(emptyList())
        val packages = LinkedHashSet<String>()
        found.forEach { ri -> ri.activityInfo?.packageName?.let(packages::add) }
        // Players that are media3/legacy browser apps but don't declare the music launcher category.
        packages.addAll(discovery.apps.value.map { it.packageName })
        // Yandex Music's own package, even when its launcher category is missing on a build.
        packages.add(YANDEX_MUSIC)
        return packages.filter { it != context.packageName }
            .mapNotNull { pkg ->
                val info = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull() ?: return@mapNotNull null
                MediaSessionApp(pkg, pm.getApplicationLabel(info).toString(), false)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun followSources() {
        val ours = _connection.flatMapLatest { c -> c?.state?.map { listOf<Pair<PlaybackSource, SourceState>>(c to it) } ?: flowOf(emptyList()) }
        val others = tracker.sources.flatMapLatest { list ->
            if (list.isEmpty()) flowOf(emptyList())
            else combine(list.map { s -> s.state.map { s as PlaybackSource to it } }) { it.toList() }
        }
        combine(ours, others, _preferred) { a, b, p -> Triple(a, b, p) }.collect { (a, b, preferredPackage) ->
            val all = a + b
            _sessions.value = all.map { (source, st) ->
                MediaSessionApp(source.packageName, source.label, st.info?.playing == true)
            }.distinctBy { it.packageName }
            val candidates = all.map { (s, st) -> st.candidate(s) }
            val preferred = preferredPackage
            val preferredKey = preferred?.let { pkg ->
                all.firstOrNull { (s, st) -> s.packageName == pkg && st.info != null }?.first?.key
            }
            val key = preferredKey ?: pickSource(candidates, _current.value?.key)
            val pick = all.firstOrNull { it.first.key == key }
            _current.value = pick?.first
            _playback.value = pick?.second?.info
        }
    }

    override fun select(app: MediaApp) {
        _preferred.value = app.packageName
        val c = _connection.value
        if (c != null && c.app.service == app.service) {
            c.connect()
            return
        }
        // one connection at a time; the old app keeps playing and shows up through the tracker
        c?.release()
        ui = null
        _connection.value = MediaConnection(context, scope, app, art).also { it.connect() }
    }

    override suspend fun awaitConnection(timeoutMs: Long): Boolean {
        val c = _connection.value ?: return false
        return awaitSettled(c.status, timeoutMs) || c.connected
    }

    internal fun uiFor(c: MediaConnection): MediaUi = ui?.takeIf { it.connection === c } ?: MediaUi(c, scope).also { ui = it }

    /** Transport goes to what is shown as playing, else to our connection. */
    private fun target(): PlaybackSource? = _current.value ?: _connection.value

    override fun playPause() {
        target()?.playPause()
    }

    override fun next() {
        target()?.next()
    }

    override fun previous() {
        target()?.previous()
    }

    override fun pause() {
        target()?.pause()
    }

    override fun selectSessionPackage(packageName: String?) {
        _preferred.value = packageName
    }

    override fun seekTo(positionMs: Long) {
        target()?.seekTo(positionMs)
    }

    override fun runAction(actionId: String) {
        target()?.state?.value?.actions?.firstOrNull { it.id == actionId }?.run()
    }

    override fun launchApp(packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { Log.w(TAG, "can't launch $packageName: $it") }
    }

    @Composable
    override fun Screen(app: MediaApp, driving: Boolean, modifier: Modifier, onShowVideo: (() -> Unit)?) =
        MediaAppScreen(this, app, driving, modifier, onShowVideo)

    override fun release() {
        discovery.release()
        tracker.release()
        _connection.value?.release()
        _connection.value = null
        ui = null
        _current.value = null
        _playback.value = null
        _sessions.value = emptyList()
        scope.cancel()
    }

    private companion object {
        const val TAG = "AareMediaBridge"
        const val YANDEX_MUSIC = "ru.yandex.music"
    }
}

/** True once [status] is [MediaConnection.Status.Ready]; false if it fails first or stays connecting for [timeoutMs]. */
internal suspend fun awaitSettled(status: Flow<MediaConnection.Status>, timeoutMs: Long): Boolean =
    withTimeoutOrNull(timeoutMs) { status.firstOrNull { it !is MediaConnection.Status.Connecting } } is MediaConnection.Status.Ready
