package net.prototux.aare.vpm

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * What DashGrid knows about VAG Performance Monitor (VPM), the app that owns the ELM327 adapter.
 *
 * DashGrid draws the cockpit itself and reads VPM's read-only snapshot ([VpmSnapshot]); it never
 * opens an OBD connection, never binds VPM's telemetry service and never launches VPM to get data.
 * When VPM (or the bridge in it) is missing, [read] says which one so the cockpit can explain it.
 */
object Vpm {
    const val PACKAGE = "io.github.nchalexz.aaperfmon"

    /** VPM's read-only snapshot provider (contract v1, see [VpmSnapshot]). */
    const val AUTHORITY = "$PACKAGE.telemetry"

    /** The normal permission the provider is read with; VPM defines it. */
    const val READ_PERMISSION = "$PACKAGE.permission.READ_TELEMETRY"

    /** Where a phone without VPM installs it. */
    const val RELEASES_URL = "https://github.com/nchalexz/VAG-PerformanceMonitor/releases/latest"

    private const val TAG = "AareVpm"

    /** The last bridge problem logged, so a 500 ms poll doesn't write one line five times a second. */
    @Volatile
    private var lastProblem: String? = null

    private fun noteProblem(key: String, message: String) {
        if (lastProblem == key) return
        lastProblem = key
        Log.i(TAG, message)
    }

    private fun noteOk() {
        lastProblem = null
    }

    fun isInstalled(context: Context): Boolean = applicationInfo(context) != null

    /** The installed app's package info, or null when it isn't installed (or can't be read). */
    fun applicationInfo(context: Context): ApplicationInfo? = runCatching {
        val pm = context.packageManager
        if (Build.VERSION.SDK_INT >= 33) {
            pm.getApplicationInfo(PACKAGE, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(PACKAGE, 0)
        }
    }.getOrNull()

    fun label(context: Context): String? = applicationInfo(context)?.let { info ->
        runCatching { context.packageManager.getApplicationLabel(info).toString() }.getOrNull()
    }

    fun icon(context: Context, sizePx: Int = 128): Bitmap? = applicationInfo(context)?.let { info ->
        runCatching { context.packageManager.getApplicationIcon(info).toBitmap(sizePx, sizePx) }.getOrNull()
    }

    /** Opens VPM's main screen on the phone; false when it can't. */
    fun openOnPhone(context: Context): Boolean {
        val intent = runCatching { context.packageManager.getLaunchIntentForPackage(PACKAGE) }
            .getOrNull()?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ?: return false
        return runCatching { context.startActivity(intent) }
            .onFailure { Log.w(TAG, "open VPM: ${it.message}") }
            .isSuccess
    }

    /** Opens VPM's releases page in the phone's browser, where the user can install or update it. */
    fun openReleases(context: Context): Boolean = runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.onFailure { Log.w(TAG, "open the VPM releases page: ${it.message}") }.isSuccess

    /**
     * Reads VPM's latest snapshot. IO thread: a ContentProvider call is a Binder round trip.
     *
     * The result distinguishes a missing app from a missing bridge: VPM before the bridge answers
     * nothing at all, and the cockpit must say "update VPM" rather than pretend the car is silent.
     */
    suspend fun read(context: Context): VpmRead = withContext(Dispatchers.IO) {
        if (applicationInfo(context) == null) {
            noteProblem("not-installed", "VPM is not installed")
            return@withContext VpmRead.NotInstalled
        }
        val uri = Uri.parse("content://$AUTHORITY")
        val bundle = try {
            context.contentResolver.call(uri, VPM_METHOD_SNAPSHOT, null, null)
        } catch (e: IllegalArgumentException) {
            // No provider with this authority (VPM before the bridge, or it was never installed).
            noteProblem("no-provider", "VPM has no telemetry provider: ${e.message}")
            return@withContext VpmRead.BridgeMissing
        } catch (e: SecurityException) {
            noteProblem("denied", "VPM telemetry permission denied: ${e.message}")
            return@withContext VpmRead.Failed("READ_TELEMETRY")
        } catch (e: Exception) {
            noteProblem("failed", "VPM telemetry call failed: ${e.message}")
            return@withContext VpmRead.Failed(e.message.orEmpty())
        } ?: return@withContext VpmRead.NoSnapshot

        parseVpmSnapshot(
            version = bundle.getInt("version", 0),
            connection = bundle.getString("connection"),
            status = bundle.getString("status"),
            source = bundle.getString("source"),
            updatedAt = bundle.getLong("updatedAt", 0L),
            signalsJson = bundle.getString("signals"),
        )?.let {
            noteOk()
            VpmRead.Snapshot(it)
        } ?: VpmRead.BridgeMissing
    }
}

/** The outcome of one snapshot read: the four states the cockpit distinguishes. */
sealed interface VpmRead {
    /** VPM answered with contract v1. */
    data class Snapshot(val value: VpmSnapshot) : VpmRead

    /** VPM's provider answered, but without a snapshot (the service hasn't run yet). */
    data object NoSnapshot : VpmRead

    /** VPM isn't installed. */
    data object NotInstalled : VpmRead

    /** VPM is installed, but this version exports no contract-v1 bridge. */
    data object BridgeMissing : VpmRead

    /** The provider exists but refused (permission or an internal error). */
    data class Failed(val message: String) : VpmRead
}
