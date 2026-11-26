package net.prototux.aare.ui.car.obd

import net.prototux.aare.obd.ObdReadings
import net.prototux.aare.obdapp.ObdApp
import org.junit.Assert.assertEquals
import org.junit.Test

class CockpitCardTilesTest {
    private fun snap(r: ObdReadings) = ObdApp.Snapshot(readings = r, showsBoost = false, gauge = r.load)

    @Test
    fun theSavedTilesAreKeptWhenTheCarReportsThem() {
        val s = snap(ObdReadings(speed = 90f, rpm = 1800f, fuel = 55f, coolant = 86f))
        assertEquals(listOf("speed", "rpm", "coolant", "fuel"), pickCardTiles(s, listOf("speed", "rpm", "coolant", "fuel")))
    }

    @Test
    fun aTileTheCarCannotFillIsReplaced() {
        val s = snap(ObdReadings(speed = 90f, rpm = 1800f, fuel = 55f))
        val picked = pickCardTiles(s, listOf("speed", "rpm", "gear", "consumption", "fuel"))
        assertEquals(listOf("speed", "rpm", "fuel"), picked)
    }

    @Test
    fun theCardNeverShowsMoreThanFive() {
        val s = snap(ObdReadings(speed = 90f, rpm = 1800f, fuel = 55f, coolant = 86f, oil = 90f, battery = 14.4f, egt1 = 300f))
        assertEquals(5, pickCardTiles(s, emptyList()).size)
        assertEquals(5, pickCardTiles(s, CardTileIds).size)
    }

    @Test
    fun boxerFreeCarsDontGetBoost() {
        val s = snap(ObdReadings(speed = 90f, load = 40f))
        assertEquals(listOf("speed", "load"), pickCardTiles(s, listOf("speed", "boost", "load")))
    }
}
