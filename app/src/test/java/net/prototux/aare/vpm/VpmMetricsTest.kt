package net.prototux.aare.vpm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VpmMetricsTest {
    /**
     * VPM 0.6.2's snapshot shape: the Engine tab carries MAP in kPa, the Turbo tab the same id in
     * bar, and the main-screen gauges/tiles sit first in their own categories.
     */
    private fun snapshot(
        connection: String = "demo",
        signals: String = """
            [
              {"id":"electrical.voltage_v","label":"Напряжение","value":"14,4","unit":"В","raw":14.4,"category":"engine","status":"valid"},
              {"id":"engine.map_kpa","label":"Давл. впуска","value":"98","unit":"кПа","raw":98.0,"category":"engine","status":"valid"},
              {"id":"engine.coolant_c","label":"ОЖ","value":"86","unit":"°C","raw":86,"category":"temperatures","status":"valid"},
              {"id":"engine.oil_c","label":"Масло","value":"88","unit":"°C","raw":88,"category":"temperatures","status":"valid"},
              {"id":"engine.intake_air_c","label":"Воздух впуска","value":"42","unit":"°C","raw":42,"category":"temperatures","status":"valid"},
              {"id":"vag.7e0.10fb.egt1","label":"Выхлоп","value":"152","unit":"°C","raw":152,"category":"temperatures","status":"valid"},
              {"id":"fuel.rail_kpa","label":"Давл. рампы","value":"314","unit":"бар","raw":314.0,"category":"fuel","status":"valid"},
              {"id":"vag.7e1.2104.atf_temp","label":"Темп. DSG","value":"73","unit":"°C","raw":73,"category":"gearbox","status":"valid"},
              {"id":"engine.map_kpa","label":"Наддув абс.","value":"0,98","unit":"бар","raw":0.98,"category":"turbo","status":"valid"},
              {"id":"engine.boost_kpa","label":"Наддув изб.","value":"0,00","unit":"бар","raw":0.0,"category":"turbo","status":"valid"}
            ]
        """.trimIndent(),
    ) = parseVpmSnapshot(VPM_BRIDGE_VERSION, connection, "ДЕМО", "", 1_700_000_000_000, signals)

    @Test
    fun theMainScreenIsThreeGaugesAndFiveTilesAsInVpm062() {
        assertEquals(listOf("engine.coolant_c", "engine.map_kpa", "engine.oil_c"), VpmMetrics.gauges.map { it.id })
        assertEquals(
            listOf("vag.7e1.2104.atf_temp", "engine.intake_air_c", "vag.7e0.10fb.egt1", "electrical.voltage_v", "fuel.rail_kpa"),
            VpmMetrics.mainTiles.map { it.id },
        )
    }

    @Test
    fun speedRevsGearFuelUseAndLevelAreNoLongerOnTheMainScreen() {
        val main = (VpmMetrics.gauges + VpmMetrics.mainTiles).map { it.id }.toSet()
        assertFalse("vehicle.speed" in main)
        assertFalse("engine.rpm" in main)
        assertFalse("vehicle.gear" in main)
        assertFalse("fuel.l100_km" in main)
        assertFalse("fuel.level_pct" in main)
        // They stay known for the category grid and the bridge parser.
        assertEquals("vehicle.speed", VpmMetrics.of("vehicle.speed")?.id)
        assertEquals("vehicle.gear", VpmMetrics.of("vehicle.gear")?.id)
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
    fun theNewMainTilesReadVpm062ValuesAndUnits() {
        val s = snapshot()
        assertEquals("73", readVpmMetric(VpmMetrics.dsgTemp, s).text)
        assertEquals("42", readVpmMetric(VpmMetrics.intakeAir, s).text)
        assertEquals("152", readVpmMetric(VpmMetrics.egt, s).text)
        assertEquals("14,4", readVpmMetric(VpmMetrics.voltage, s).text)
        assertEquals("314", readVpmMetric(VpmMetrics.rail, s).text)
    }

    @Test
    fun theAbsoluteBoostGaugeReadsTheTurboOccurrenceInBarNotTheEngineOneInKpa() {
        val reading = readVpmMetric(VpmMetrics.boostAbs, snapshot())
        // Engine carries the same id as "98 кПа"; the gauge must take Turbo's "0,98 бар".
        assertEquals("0,98", reading.text)
        assertEquals(0.98, reading.display!!, 0.0001)
        assertEquals(0.392f, reading.fraction!!, 0.002f)
        assertEquals(VpmCategory.TURBO, VpmMetrics.boostAbs.category)
    }

    @Test
    fun vpmAlreadyConvertsKpaToBarSoNoSecondFactorIsApplied() {
        // Contract v1 sends raw in display units; 98 kPa arrives as 0.98 bar and 314 bar stays 314.
        assertEquals(0.98, readVpmMetric(VpmMetrics.boostAbs, snapshot()).display!!, 0.0001)
        assertEquals(314.0, readVpmMetric(VpmMetrics.rail, snapshot()).display!!, 0.0001)
        assertEquals(1.0, VpmMetrics.boostAbs.factor, 0.0)
        assertEquals(1.0, VpmMetrics.rail.factor, 0.0)
    }

    @Test
    fun aDroppedLinkKeepsTheLastStaleNumberOnScreen() {
        val dropped = snapshot(
            connection = "disconnected",
            signals = """[{"id":"engine.coolant_c","value":"86","raw":86,"stale":true,"status":"stale"}]""",
        )
        val reading = readVpmMetric(VpmMetrics.coolant, dropped)
        assertEquals(VpmReadingState.STALE, reading.state)
        assertEquals("86", reading.text)
    }

    @Test
    fun aDroppedLinkDoesNotPassAnOldValidNumberAsCurrent() {
        val dropped = snapshot(
            connection = "disconnected",
            signals = """[{"id":"engine.coolant_c","value":"86","raw":86,"stale":false,"status":"valid"}]""",
        )
        assertEquals(VpmReadingState.OFFLINE, readVpmMetric(VpmMetrics.coolant, dropped).state)
        assertEquals("—", readVpmMetric(VpmMetrics.coolant, dropped).text)
    }

    @Test
    fun aSilentCarShowsADashNotAZero() {
        val withoutOil = snapshot(signals = """[{"id":"engine.coolant_c","value":"86","raw":86,"status":"valid"}]""")
        val reading = readVpmMetric(VpmMetrics.oil, withoutOil)
        assertEquals(VpmReadingState.WAITING, reading.state)
        assertEquals("—", reading.text)
        assertNull(reading.display)
    }

    @Test
    fun aStaleValueIsANumberWithAStaleStateAndAFadedNeedle() {
        val stale = snapshot(signals = """[{"id":"engine.coolant_c","value":"86","raw":86,"stale":true,"status":"stale"}]""")
        val reading = readVpmMetric(VpmMetrics.coolant, stale)
        assertEquals(VpmReadingState.STALE, reading.state)
        assertEquals("86", reading.text)
        assertEquals(86.0, reading.display!!, 0.0)
        assertEquals(0.575f, reading.fraction!!, 0.001f)
    }

    @Test
    fun anOldVpmWithoutStatusStillKeepsItsStaleNumber() {
        val old = snapshot(signals = """[{"id":"engine.coolant_c","value":"86","raw":86,"stale":true}]""")
        val reading = readVpmMetric(VpmMetrics.coolant, old)
        assertEquals(VpmReadingState.STALE, reading.state)
        assertEquals("86", reading.text)
    }

    @Test
    fun neverReadIsTheOnlyStatusThatForcesADash() {
        val never = snapshot(signals = """[{"id":"engine.coolant_c","status":"never_read"}]""")
        val reading = readVpmMetric(VpmMetrics.coolant, never)
        assertEquals(VpmReadingState.WAITING, reading.state)
        assertEquals("—", reading.text)
        assertNull(reading.display)

        // A stale status without a number is also a dash: there is nothing to keep.
        val staleNoValue = snapshot(signals = """[{"id":"engine.coolant_c","status":"stale"}]""")
        assertEquals("—", readVpmMetric(VpmMetrics.coolant, staleNoValue).text)
    }

    @Test
    fun warningAndCriticalFollowVpmsThresholds() {
        val warm = readVpmMetric(
            VpmMetrics.coolant,
            snapshot(signals = """[{"id":"engine.coolant_c","value":"110","raw":110,"status":"valid"}]"""),
        )
        assertTrue(warm.warning)
        assertFalse(warm.critical)
        val hot = readVpmMetric(
            VpmMetrics.coolant,
            snapshot(signals = """[{"id":"engine.coolant_c","value":"116","raw":116,"status":"valid"}]"""),
        )
        assertTrue(hot.warning)
        assertTrue(hot.critical)
    }

    @Test
    fun dsgExhaustAndAbsoluteBoostUseVpm062Thresholds() {
        fun reading(metric: VpmMetric, id: String, raw: Int) = readVpmMetric(
            metric,
            snapshot(signals = """[{"id":"$id","value":"$raw","raw":$raw,"status":"valid"}]"""),
        )
        assertTrue(reading(VpmMetrics.dsgTemp, "vag.7e1.2104.atf_temp", 110).warning)
        assertTrue(reading(VpmMetrics.dsgTemp, "vag.7e1.2104.atf_temp", 125).critical)
        assertTrue(reading(VpmMetrics.egt, "vag.7e0.10fb.egt1", 700).warning)
        assertTrue(reading(VpmMetrics.egt, "vag.7e0.10fb.egt1", 850).critical)

        val boost = readVpmMetric(
            VpmMetrics.boostAbs,
            snapshot(signals = """[{"id":"engine.map_kpa","value":"2,05","raw":2.05,"category":"turbo","status":"valid"}]"""),
        )
        assertTrue(boost.warning)
    }

    @Test
    fun aVagOilSignalStandsInForTheSilentPid() {
        val vag = snapshot(
            signals = """[{"id":"vag.7e0.11be.oil_temp","label":"Масло (двиг.)","value":"91","raw":91,"category":"temperatures","status":"valid"}]""",
        )
        assertEquals("91", readVpmMetric(VpmMetrics.oil, vag).text)
    }

    @Test
    fun aNeverReadPrimaryDoesNotHideALiveVagOilSignal() {
        // Real snapshot shape on a diesel: the main gauge entry exists but PID 0x5C never answered,
        // while the VAG block already has the temperature. The first entry must not win by position.
        val vag = snapshot(
            signals = """
                [
                  {"id":"engine.oil_c","label":"Масло","value":null,"raw":null,"category":"temperatures","status":"never_read"},
                  {"id":"vag.7e0.11be.oil_temp","label":"Масло (двиг.)","value":"91","raw":91,"category":"temperatures","status":"valid"}
                ]
            """.trimIndent(),
        )
        val reading = readVpmMetric(VpmMetrics.oil, vag)
        assertEquals("91", reading.text)
        assertEquals(VpmReadingState.OK, reading.state)
    }

    @Test
    fun aFreshPrimaryWinsOverAFreshAlternativeAndAStalePrimaryLosesToAFreshAlternative() {
        val bothFresh = snapshot(
            signals = """
                [
                  {"id":"engine.oil_c","value":"95","raw":95,"category":"temperatures","status":"valid"},
                  {"id":"vag.7e0.11be.oil_temp","value":"91","raw":91,"category":"temperatures","status":"valid"}
                ]
            """.trimIndent(),
        )
        assertEquals("95", readVpmMetric(VpmMetrics.oil, bothFresh).text)

        val stalePrimary = snapshot(
            signals = """
                [
                  {"id":"engine.oil_c","value":"95","raw":95,"category":"temperatures","status":"stale"},
                  {"id":"vag.7e0.11be.oil_temp","value":"91","raw":91,"category":"temperatures","status":"valid"}
                ]
            """.trimIndent(),
        )
        assertEquals("91", readVpmMetric(VpmMetrics.oil, stalePrimary).text)
    }

    @Test
    fun categorySignalsKeepVpmsOwnOrderAndItsCategory() {
        val s = snapshot()!!
        assertEquals(
            listOf("engine.coolant_c", "engine.oil_c", "engine.intake_air_c", "vag.7e0.10fb.egt1"),
            s.signalsIn(VpmCategory.TEMPERATURES).map { it.id },
        )
        assertEquals(
            listOf("electrical.voltage_v", "engine.map_kpa"),
            s.signalsIn(VpmCategory.ENGINE).map { it.id },
        )
        assertEquals(listOf("fuel.rail_kpa"), s.signalsIn(VpmCategory.FUEL).map { it.id })
        assertEquals(listOf("vag.7e1.2104.atf_temp"), s.signalsIn(VpmCategory.GEARBOX).map { it.id })
        assertEquals(listOf("engine.map_kpa", "engine.boost_kpa"), s.signalsIn(VpmCategory.TURBO).map { it.id })
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
