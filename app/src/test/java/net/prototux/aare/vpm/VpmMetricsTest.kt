package net.prototux.aare.vpm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VpmMetricsTest {
    private fun snapshot(
        connection: String = "demo",
        signals: String = """
            [
              {"id":"engine.coolant_c","label":"ОЖ","value":"86","unit":"°C","raw":86,"category":"temperatures"},
              {"id":"engine.boost_kpa","label":"Наддув","value":"0,62","unit":"бар","raw":0.62,"category":"engine"},
              {"id":"engine.oil_c","label":"Масло","value":"95","unit":"°C","raw":95,"category":"temperatures"},
              {"id":"vehicle.speed","label":"Скорость","value":"96","unit":"км/ч","raw":96,"category":"engine"},
              {"id":"engine.rpm","label":"Обороты","value":"1840","unit":"об/мин","raw":1840,"category":"engine"},
              {"id":"vehicle.gear","label":"Передача","value":"5","unit":"","raw":5,"category":"gearbox"},
              {"id":"fuel.l100_km","label":"Расход","value":"5,0","unit":"л/100 км","raw":5.0,"category":"fuel"},
              {"id":"fuel.level_pct","label":"Топливо","value":"58","unit":"%","raw":58,"category":"fuel"}
            ]
        """.trimIndent(),
    ) = parseVpmSnapshot(VPM_BRIDGE_VERSION, connection, "ДЕМО", "", 1_700_000_000_000, signals)

    @Test
    fun theMainScreenIsThreeGaugesAndFiveTilesAsInTheMockup() {
        assertEquals(listOf("engine.coolant_c", "engine.boost_kpa", "engine.oil_c"), VpmMetrics.gauges.map { it.id })
        assertEquals(
            listOf("vehicle.speed", "engine.rpm", "vehicle.gear", "fuel.l100_km", "fuel.level_pct"),
            VpmMetrics.mainTiles.map { it.id },
        )
    }

    @Test
    fun demoAndLiveValuesAreDrawnAndADisconnectedCarIsNot() {
        assertEquals("86", readVpmMetric(VpmMetrics.coolant, snapshot()).text)
        assertEquals("86", readVpmMetric(VpmMetrics.coolant, snapshot(connection = "connected")).text)
        assertEquals(VpmReadingState.OFFLINE, readVpmMetric(VpmMetrics.coolant, snapshot(connection = "disconnected")).state)
        assertEquals(VpmReadingState.OFFLINE, readVpmMetric(VpmMetrics.coolant, null).state)
        assertEquals("—", readVpmMetric(VpmMetrics.coolant, null).text)
    }

    @Test
    fun aSilentCarShowsADashNotAZero() {
        val withoutOil = snapshot(signals = """[{"id":"engine.coolant_c","value":"86","raw":86}]""")
        val reading = readVpmMetric(VpmMetrics.oil, withoutOil)
        assertEquals(VpmReadingState.WAITING, reading.state)
        assertEquals("—", reading.text)
        assertNull(reading.display)
    }

    @Test
    fun aStaleValueIsADashAndTheNeedleIsHidden() {
        val stale = snapshot(signals = """[{"id":"engine.coolant_c","value":"86","raw":86,"stale":true}]""")
        val reading = readVpmMetric(VpmMetrics.coolant, stale)
        assertEquals(VpmReadingState.STALE, reading.state)
        assertEquals("—", reading.text)
        assertNull(reading.fraction)
    }

    @Test
    fun theBoostGaugeUsesVpmsBarScaleAndItsOwnThresholds() {
        val reading = readVpmMetric(VpmMetrics.boost, snapshot())
        assertEquals("0,62", reading.text)
        assertEquals(0.54f, reading.fraction!!, 0.001f)
        assertFalse(reading.warning)
        val hot = readVpmMetric(
            VpmMetrics.boost,
            snapshot(signals = """[{"id":"engine.boost_kpa","value":"1,80","raw":1.8}]"""),
        )
        assertTrue(hot.warning)
    }

    @Test
    fun warningAndCriticalFollowVpmsThresholds() {
        val warm = readVpmMetric(
            VpmMetrics.coolant,
            snapshot(signals = """[{"id":"engine.coolant_c","value":"110","raw":110}]"""),
        )
        assertTrue(warm.warning)
        assertFalse(warm.critical)
        val hot = readVpmMetric(
            VpmMetrics.coolant,
            snapshot(signals = """[{"id":"engine.coolant_c","value":"116","raw":116}]"""),
        )
        assertTrue(hot.warning)
        assertTrue(hot.critical)
    }

    @Test
    fun aVagOilSignalStandsInForTheSilentPid() {
        val vag = snapshot(
            signals = """[{"id":"vag.7e0.11be.oil_temp","label":"Масло (двиг.)","value":"91","raw":91,"category":"temperatures"}]""",
        )
        assertEquals("91", readVpmMetric(VpmMetrics.oil, vag).text)
    }

    @Test
    fun aGearSignalHasNoUnitAndKeepsItsNumber() {
        assertEquals(null, VpmMetrics.gear.unitRes)
        assertEquals("5", readVpmMetric(VpmMetrics.gear, snapshot()).text)
    }

    @Test
    fun categorySignalsKeepVpmsOwnOrderAndItsCategory() {
        val s = snapshot()!!
        assertEquals(
            listOf("engine.coolant_c", "engine.oil_c"),
            s.signalsIn(VpmCategory.TEMPERATURES).map { it.id },
        )
        assertEquals(listOf("engine.boost_kpa", "vehicle.speed", "engine.rpm"), s.signalsIn(VpmCategory.ENGINE).map { it.id })
        assertEquals(listOf("vehicle.gear"), s.signalsIn(VpmCategory.GEARBOX).map { it.id })
        assertTrue(s.signalsIn(VpmCategory.DIAGNOSTICS).isEmpty())
    }

    @Test
    fun aSignalWithoutACategoryFallsBackToItsKnownMetricThenToEngine() {
        val s = snapshot(
            signals = """
                [
                  {"id":"vehicle.speed","value":"96","raw":96},
                  {"id":"vag.7e0.1057.boost","value":"161","raw":161},
                  {"id":"engine.coolant_c","value":"86","raw":86,"category":"temperatures"}
                ]
            """.trimIndent(),
        )!!
        assertEquals(VpmCategory.ENGINE, vpmSignalCategory(s.signals[0]))
        assertEquals(VpmCategory.ENGINE, vpmSignalCategory(s.signals[1]))
        assertEquals(VpmCategory.TEMPERATURES, vpmSignalCategory(s.signals[2]))
    }

    @Test
    fun vpmsNumberStyleIsEuropean() {
        assertEquals("0,62", formatVpmNumber(0.615f.toDouble(), 2))
        assertEquals("-0,35", formatVpmNumber(-0.35, 2))
        assertEquals("96", formatVpmNumber(96.0, 0))
    }
}
