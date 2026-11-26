package net.prototux.aare.obdapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CarProfilesTest {
    @Test fun theGolfProfileIsADieselWithATdiRevCounter() {
        val p = CarProfiles.GOLF7_CUNA_DQ381
        assertEquals(ObdAppSettings.Fuel.DIESEL, p.fuel)
        assertEquals(ObdAppSettings.Engine.TURBO, p.engine)
        assertEquals(1968, p.displacementCc)
        assertEquals(50, p.tankLiters)
        assertEquals(5000, p.rpmMax)
        assertTrue("red line before the cut", p.redline in 4000 until p.rpmMax)
    }

    @Test fun theGenericProfileKeepsTheOldDefaults() {
        val g = CarProfiles.GENERIC
        assertEquals(1600, g.displacementCc)
        assertEquals(7000, g.rpmMax)
        assertEquals(6500, g.redline)
    }

    @Test fun anUnknownIdFallsBackToGeneric() {
        assertSame(CarProfiles.GENERIC, CarProfiles.byId("no-such-car"))
        assertSame(CarProfiles.GENERIC, CarProfiles.byId(null))
        assertSame(CarProfiles.GENERIC, CarProfiles.byId(""))
        assertSame(CarProfiles.GOLF7_CUNA_DQ381, CarProfiles.byId("golf7-cuna-dq381"))
    }

    @Test fun profileIdsAreUnique() {
        val ids = CarProfiles.ALL.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }
}
