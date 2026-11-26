package net.prototux.aare.obd

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VagTest {
    @Test fun singleFrame() {
        assertArrayEquals(intArrayOf(0x5A), Vag.parse("6211005A", 0x1100))
    }

    @Test fun spacesAndFramePrefixesAreDropped() {
        assertArrayEquals(intArrayOf(0x00, 0x64), Vag.parse("0: 62 11 4F 00 64", 0x114F))
    }

    @Test fun multiFrameAnswersAreJoined() {
        assertArrayEquals(intArrayOf(0x00, 0x64, 0x1A, 0x2B), Vag.parse("0:62114F0064\r1:1A2B", 0x114F))
    }

    @Test fun refusalsAndSilence() {
        assertNull(Vag.parse("7F 22 31", 0x1100))
        assertNull(Vag.parse("NO DATA", 0x1100))
        assertNull(Vag.parse("[no answer]", 0x1100))
    }

    @Test fun anotherIdentifierDoesNotCount() {
        assertNull(Vag.parse("62110100", 0x1100))
    }

    @Test fun aSignalIsScaledAndChecked() {
        val coolant = Vag.Signal("7E0", 0x1100, 0, "°C", offset = -40f, min = 20f, max = 130f)
        assertEquals(90f, Vag.decode(coolant, intArrayOf(130))!!, 0.01f)
        assertNull("an answer below the range is a wrong identifier", Vag.decode(coolant, intArrayOf(0)))
        val boost = Vag.Signal("7E0", 0x11AA, 0, "bar", scale = 0.01f, offset = -1f, decimals = 2)
        assertEquals(0.5f, Vag.decode(boost, intArrayOf(0x00, 0x96))!!, 0.01f)
    }

    @Test fun aSignalCanStartLaterInABlock() {
        val second = Vag.Signal("7E0", 0x11C0, 0, "°C", index = 2, scale = 0.1f, offset = -40f)
        assertEquals(320f, Vag.decode(second, intArrayOf(0x00, 0x00, 0x0E, 0x10))!!, 0.01f)
        assertNull("a block too short for the index", Vag.decode(second, intArrayOf(0x00)))
    }

    @Test fun hexFormatting() {
        assertEquals("62 11 00 5A", Vag.hex(intArrayOf(0x62, 0x11, 0x00, 0x5A)))
    }
}
