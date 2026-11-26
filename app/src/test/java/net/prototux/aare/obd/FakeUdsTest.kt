package net.prototux.aare.obd

import org.junit.Assert.assertEquals
import org.junit.Test

/** The pretend adapter answers the Cockpit's demo UDS identifiers, so the whole path is exercised. */
class FakeUdsTest {
    private val fake = FakeObdLink(delayMs = 0)

    @Test
    fun coolantComesBackFromTheDemoEngine() {
        val bytes = Vag.parse(fake.send("221100", 2_000), 0x1100)!!
        assertEquals(92f, Vag.decode(Vag.demoSignals[0], bytes)!!, 0.01f)
    }

    @Test
    fun theGearboxOilIsOnTheGearboxModule() {
        val bytes = Vag.parse(fake.send("222104", 2_000), 0x2104)!!
        assertEquals(84f, Vag.decode(Vag.demoSignals[1], bytes)!!, 0.01f)
    }

    @Test
    fun anInjectorCorrectionKeepsItsSign() {
        val bytes = Vag.parse(fake.send("2211C4", 2_000), 0x11C4)!!
        assertEquals(-0.4f, Vag.decode(Vag.demoSignals[2], bytes)!!, 0.001f)
    }

    @Test
    fun anIdentifierTheCarDoesNotKnowIsNoAnswer() {
        assertEquals("NODATA", fake.send("229999", 2_000))
    }
}
