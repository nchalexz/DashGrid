package net.prototux.aare.vpm

import org.json.JSONArray

/**
 * The read-only telemetry contract between VAG Performance Monitor (VPM) and DashGrid, version 1.
 *
 * VPM owns the ELM327 adapter and the VAG UDS session; DashGrid never opens an OBD connection.
 * VPM publishes the last state it already holds through a ContentProvider:
 * `content://io.github.nchalexz.aaperfmon.telemetry`, method [VPM_METHOD_SNAPSHOT]. The provider
 * must not start its service or poll the adapter: reading a snapshot is a read, not a drive.
 *
 * The bundle carries `version`, `connection`, `status`, `source`, `updatedAt` and `signals` (a JSON
 * array, [VPM_MAX_SIGNALS] at most). Every signal carries the value **as VPM's own cockpit shows
 * it** (formatted text, display unit and numeric display value), so DashGrid draws the same numbers
 * and never re-derives VPM's conversion factors.
 *
 * The array is grouped by VPM's display category and keeps VPM's own order inside each category, so
 * a category tab draws exactly the tiles its mockup has. A signal VPM shows in two categories (boost
 * in Engine and in Turbo, for one) appears once per category with the matching `category`; the first
 * occurrence with an id is the one the main screen reads.
 */
const val VPM_METHOD_SNAPSHOT = "snapshot"

/** The contract version this client understands. A provider answering another one is unusable. */
const val VPM_BRIDGE_VERSION = 1

/** Enough for VPM's whole catalog, small enough for one Binder transaction. */
const val VPM_MAX_SIGNALS = 128

/** How VPM's telemetry engine describes its link to the adapter. */
enum class VpmConnection(val wire: String) {
    DEMO("demo"),
    CONNECTING("connecting"),
    CONNECTED("connected"),
    DISCONNECTED("disconnected"),
    ERROR("error"),

    /** A word this client doesn't know: drawn as "no link", never as live data. */
    UNKNOWN("unknown");

    /** Demo values are VPM's own sample data; connected values are live. Both can be drawn. */
    val canShowData: Boolean get() = this == DEMO || this == CONNECTED

    companion object {
        fun byId(id: String?): VpmConnection =
            entries.firstOrNull { it.wire == id?.trim()?.lowercase() } ?: UNKNOWN
    }
}

/**
 * How fresh a signal is, from VPM's `status` field (added in VPM 0.6.0).
 *
 * [VALID] is a current value, [STALE] a value VPM still holds but has not refreshed, and
 * [NEVER_READ] the absence of any successful read in the session. The numbers stay numbers when
 * [STALE]: only [NEVER_READ] or a missing value is a dash on the cockpit.
 */
enum class VpmSignalStatus(val wire: String) {
    VALID("valid"),
    STALE("stale"),
    NEVER_READ("never_read"),
}

/**
 * One metric of VPM's snapshot.
 *
 * [value] is the text VPM's cockpit shows (comma decimals, "—" for no value is not sent: null is).
 * [unit] is VPM's display unit. [raw] is the numeric value in that same display unit, used to place
 * a needle and to reach VPM's warning thresholds. [category] is one of VPM's display categories
 * (`engine`, `temperatures`, `fuel`, `gearbox`, `turbo`, `diagnostics`).
 *
 * [status] comes from VPM 0.6.0+; for an older provider it is derived from [stale] and the presence
 * of a value, so an old VPM never looks like a new one and never crashes the parser.
 */
data class VpmSignal(
    val id: String,
    val label: String,
    val value: String?,
    val unit: String,
    val raw: Double?,
    val stale: Boolean,
    val warning: Boolean,
    val category: String?,
    val status: VpmSignalStatus,
) {
    /** A number worth drawing: VPM sent one, whether it is fresh or stale. */
    val hasValue: Boolean get() = value != null || raw != null

    val isStale: Boolean get() = status == VpmSignalStatus.STALE

    val isNeverRead: Boolean get() = status == VpmSignalStatus.NEVER_READ
}

/** One VPM snapshot: the link's state and every value VPM had at [updatedAt]. */
data class VpmSnapshot(
    val version: Int,
    val connection: VpmConnection,
    /** VPM's human status line ("ДЕМО", "Wi-Fi ELM327", "Нет адаптера"…). */
    val status: String,
    /** The transport or adapter VPM is using, when it says so. */
    val source: String,
    /** When VPM last updated its values, not when this snapshot was read. 0 means never. */
    val updatedAt: Long,
    val signals: List<VpmSignal>,
) {
    fun signal(id: String): VpmSignal? = signals.firstOrNull { it.id == id }

    /** True when VPM is in its demo mode: the values are VPM's samples, not a car. */
    val demo: Boolean get() = connection == VpmConnection.DEMO

    /** True when the cockpit may draw a number at all. */
    val live: Boolean get() = connection.canShowData
}

/**
 * Parses a snapshot from the provider's bundle fields: the Android-free half of the bridge, so a
 * unit test can feed it strings from the contract.
 *
 * Returns null when the provider doesn't speak [VPM_BRIDGE_VERSION]; the caller then reports a
 * missing bridge rather than drawing whatever it got.
 */
fun parseVpmSnapshot(
    version: Int,
    connection: String?,
    status: String?,
    source: String?,
    updatedAt: Long,
    signalsJson: String?,
): VpmSnapshot? {
    if (version != VPM_BRIDGE_VERSION) return null
    return VpmSnapshot(
        version = version,
        connection = VpmConnection.byId(connection),
        status = status.orEmpty().trim(),
        source = source.orEmpty().trim(),
        updatedAt = updatedAt.coerceAtLeast(0L),
        signals = parseVpmSignals(signalsJson),
    )
}

/**
 * Parses the `signals` JSON array. A broken array is an empty list, not a crash: a missing value is
 * an honest "—", and one bad entry must not lose the others.
 */
fun parseVpmSignals(json: String?): List<VpmSignal> {
    if (json.isNullOrBlank()) return emptyList()
    val array = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
    val out = ArrayList<VpmSignal>(minOf(array.length(), VPM_MAX_SIGNALS))
    val seen = HashSet<String>()
    for (i in 0 until minOf(array.length(), VPM_MAX_SIGNALS)) {
        val o = array.optJSONObject(i) ?: continue
        val id = o.optString("id").trim()
        if (id.isEmpty()) continue
        val category = o.optString("category").trim().takeIf { it.isNotEmpty() }
        // One entry per (id, category): VPM may list a signal under two tabs, but not twice in one.
        if (!seen.add(id + "\u0000" + category.orEmpty())) continue
        val value = when {
            !o.has("value") || o.isNull("value") -> null
            else -> o.opt("value")?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        }
        val raw = when (val raw = if (o.has("raw") && !o.isNull("raw")) o.opt("raw") else null) {
            is Number -> raw.toDouble().takeIf { it.isFinite() }
            is String -> raw.toDoubleOrNull()?.takeIf { it.isFinite() }
            else -> null
        }
        val stale = o.optBoolean("stale", false)
        val status = parseVpmSignalStatus(o.optString("status"), stale, value != null || raw != null)
        out += VpmSignal(
            id = id,
            label = o.optString("label").trim().ifEmpty { id },
            value = value,
            unit = o.optString("unit").trim(),
            raw = raw,
            stale = stale,
            warning = o.optBoolean("warning", false),
            category = category,
            status = status,
        )
    }
    return out
}

/**
 * Reads the contract's `status` with the compatibility rule of VPM 0.6.0:
 *
 * - a known `valid` / `stale` / `never_read` wins, even if `stale` disagrees with it;
 * - a provider older than 0.6.0 has no `status`, so `stale = true` means [VpmSignalStatus.STALE],
 *   a missing value means [VpmSignalStatus.NEVER_READ], and anything else is
 *   [VpmSignalStatus.VALID];
 * - an unknown word is not an error: `stale` and the presence of a value decide, so a future VPM
 *   still draws its numbers instead of a black screen.
 */
fun parseVpmSignalStatus(status: String?, stale: Boolean, hasValue: Boolean): VpmSignalStatus {
    val wire = status?.trim()?.lowercase()
    VpmSignalStatus.entries.firstOrNull { it.wire == wire }?.let { return it }
    return when {
        stale -> VpmSignalStatus.STALE
        !hasValue -> VpmSignalStatus.NEVER_READ
        else -> VpmSignalStatus.VALID
    }
}
