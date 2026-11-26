package net.prototux.aare.ui.car.obd

import net.prototux.aare.obd.ObdReadings
import net.prototux.aare.obdapp.ObdApp
import org.junit.Assert.assertEquals
import org.junit.Test

class CockpitDialsTest {
    private fun snap(r: ObdReadings, boost: Boolean = false) = ObdApp.Snapshot(readings = r, showsBoost = boost, gauge = if (boost) r.boost else r.load)

    @Test
    fun theSavedDialsAreKeptWhenTheCarStillReportsThem() {
        val s = snap(ObdReadings(coolant = 86f, boost = 0.5f, oil = 91f), boost = true)
        assertEquals(listOf("coolant", "boost", "oil"), pickDialIds(s, DefaultDials))
    }

    @Test
    fun aMissingValueIsReplacedByWhatTheCarDoesReport() {
        val s = snap(ObdReadings(coolant = 86f, load = 30f, intakeAir = 30f))
        assertEquals(listOf("coolant", "intake", "load"), pickDialIds(s, DefaultDials))
    }

    @Test
    fun anAspiratedCarGetsLoadInsteadOfBoost() {
        val s = snap(ObdReadings(coolant = 86f, oil = 91f, load = 35f))
        assertEquals(listOf("coolant", "oil", "load"), pickDialIds(s, DefaultDials))
    }

    @Test
    fun tappingADialWalksPastTheOnesTheOthersShow() {
        val s = snap(ObdReadings(coolant = 86f, oil = 91f, intakeAir = 30f, fuel = 50f, battery = 14.6f))
        assertEquals(listOf("coolant", "oil", "fuel"), cycleDial(s, listOf("coolant", "oil", "intake"), 2))
        assertEquals(listOf("intake", "oil"), cycleDial(s, listOf("coolant", "oil"), 0))
    }

    @Test
    fun cyclingWithNothingAvailableChangesNothing() {
        val s = ObdApp.Snapshot(readings = ObdReadings())
        assertEquals(DefaultDials, cycleDial(s, DefaultDials, 0))
    }
}
