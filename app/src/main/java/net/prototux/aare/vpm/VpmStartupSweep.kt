package net.prototux.aare.vpm

import android.os.SystemClock

/**
 * The ignition needle test runs once per app process, not every time the Car page is shown.
 *
 * The first gauge that composes stamps the start; every gauge in that first cockpit joins the
 * same 1.8 s sweep. Later visits see an elapsed time past the sweep and keep the live needle.
 */
object VpmStartupSweep {
    const val DURATION_MS = 1800L

    private var startedAt = 0L

    @Synchronized
    fun start(): Long {
        if (startedAt == 0L) startedAt = SystemClock.elapsedRealtime()
        return startedAt
    }

    fun elapsedFraction(startedAt: Long): Float =
        ((SystemClock.elapsedRealtime() - startedAt) / DURATION_MS.toFloat()).coerceIn(0f, 1f)
}
