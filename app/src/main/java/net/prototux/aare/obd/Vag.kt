package net.prototux.aare.obd

import net.prototux.aare.R

/**
 * A few readings the standard OBD2 PIDs don't carry, asked of a VAG control unit over UDS
 * (service 0x22, "read data by identifier"). Only ever reads: no coding, no routines, no clears.
 *
 * The identifiers are the ones VAG tools use for these values on Bosch EDC17 diesel engines
 * (the Golf 7 2.0 TDI's family) and the DQ381's control unit, from public OBD/VCDS reports. They
 * differ between software versions, so [candidates] is a list to try on the car, not a promise:
 * the probe keeps what answers, and [SCANS] are walked when the value's identifier isn't known
 * (exhaust gas temperatures, injector corrections, gearbox temperatures).
 */
object Vag {
    /** One question: the module's 11-bit CAN header and the data identifier asked of it. */
    data class Query(val header: String, val did: Int, val name: String)

    /**
     * The candidates tried by the probe. The engine module is 7E0, the gearbox 7E1 (both 11-bit,
     * 500 kbit/s). Coolant and DPF values are EDC17 identifiers; 2104 on 7E1 is the DSG oil
     * temperature reported for MQB cars.
     */
    val candidates = listOf(
        Query("7E0", 0x1100, "Coolant"),
        Query("7E1", 0x2104, "DSG oil"),
    )

    /**
     * The identifier ranges worth walking when the value's identifier isn't known: the engine's
     * measured values (exhaust gas temperatures, injector corrections) and the gearbox's.
     */
    val SCANS = listOf(
        Scan("7E0", 0x1100, 0x11FF),
        Scan("7E1", 0x2100, 0x21FF),
    )

    data class Scan(val header: String, val from: Int, val to: Int)

    /**
     * The bytes of the positive answer to [did] (`62 <did> …`), or null when the module refused
     * or didn't answer. ELM327 with headers off prints a multi-frame answer as "0: …", "1: …"
     * lines; the prefixes and spaces are dropped before the search.
     */
    fun parse(answer: String, did: Int): IntArray? {
        val hex = answer.split('\r', '\n')
            .map { it.trim().replace(" ", "") }
            .map { it.replace(Regex("^\\d+:"), "") }
            .filter { it.isNotEmpty() && it.all { c -> c.isDigit() || c in 'A'..'F' || c in 'a'..'f' } }
            .joinToString("")
            .uppercase()
        val tag = "62%04X".format(did)
        val at = hex.indexOf(tag)
        if (at < 0) return null
        val rest = hex.substring(at + tag.length)
        val even = rest.length - rest.length % 2
        if (even <= 0) return IntArray(0)
        return IntArray(even / 2) { rest.substring(it * 2, it * 2 + 2).toInt(16) }
    }

    /** "62 11 00 5A" style, for the log and the probe's list. */
    fun hex(bytes: IntArray): String = bytes.joinToString(" ") { "%02X".format(it) }

    /**
     * A value worth reading every few seconds: which module, which identifier, where in its answer
     * the number starts (a block can hold several), and how to turn the bytes into a number.
     * [min] and [max] are a sanity check: an answer outside them is a wrong identifier, not a value,
     * and is dropped.
     */
    data class Signal(
        val header: String,
        val did: Int,
        /** The tile's label. */
        val label: Int,
        val unit: String,
        val index: Int = 0,
        val scale: Float = 1f,
        val offset: Float = 0f,
        val decimals: Int = 0,
        val min: Float = -Float.MAX_VALUE,
        val max: Float = Float.MAX_VALUE,
    )

    /** Two big-endian bytes from [index] of the answer (one when it's shorter), scaled and checked. */
    fun decode(signal: Signal, bytes: IntArray): Float? {
        val a = bytes.getOrNull(signal.index) ?: return null
        val b = bytes.getOrNull(signal.index + 1)
        val raw = if (b != null) (a shl 8) or (b and 0xFF) else a
        val value = raw * signal.scale + signal.offset
        return if (value in signal.min..signal.max) value else null
    }

    /**
     * The values read over UDS, on top of the standard PIDs. Empty until the car's identifiers are
     * known: they differ between control unit software versions, so they're filled in from the
     * probe/scan on the car, not guessed. Each entry is read every few seconds, in its module.
     */
    val signals: List<Signal> = emptyList()

    /**
     * What the pretend adapter answers, so the whole UDS path (question, parse, scale, tile) can be
     * tried without a car. Used only while [ObdReader] runs its simulator; the identifiers are the
     * public ones, the values are made up.
     */
    val demoSignals = listOf(
        Signal("7E0", 0x1100, R.string.obd_coolant, "°C", offset = -40f, min = 20f, max = 130f),
        Signal("7E1", 0x2104, R.string.obd_vag_gearbox_oil, "°C", offset = -40f, min = 0f, max = 150f),
        Signal("7E0", 0x11C4, R.string.obd_vag_injector1, "mg", scale = 0.01f, offset = -10f, decimals = 2, min = -10f, max = 10f),
    )
}
