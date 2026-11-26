package net.prototux.aare.vpm

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VpmSnapshotTest {
    private val json = """
        [
          {"id":"engine.coolant_c","label":"ОЖ","value":"86","unit":"°C","raw":86,"stale":false,"warning":false,"category":"temperatures"},
          {"id":"engine.boost_kpa","label":"Наддув","value":"0,62","unit":"бар","raw":0.62,"stale":false,"warning":false,"category":"engine"},
          {"id":"engine.oil_c","label":"Масло","value":"95","unit":"°C","raw":95,"stale":false,"warning":false,"category":"temperatures"},
          {"id":"vehicle.speed","label":"Скорость","value":"96","unit":"км/ч","raw":96,"stale":false,"warning":false,"category":"engine"},
          {"id":"vehicle.gear","label":"Передача","value":"5","unit":"","raw":5,"stale":false,"warning":false,"category":"gearbox"},
          {"id":"fuel.l100_km","label":"Расход","value":"5,0","unit":"л/100 км","raw":5.0,"stale":false,"warning":false,"category":"fuel"},
          {"id":"fuel.level_pct","label":"Топливо","value":"58","unit":"%","raw":58,"stale":false,"warning":false,"category":"fuel"}
        ]
    """.trimIndent()

    private fun snapshot(
        version: Int = VPM_BRIDGE_VERSION,
        connection: String = "connected",
        signals: String = json,
    ) = parseVpmSnapshot(version, connection, "Wi-Fi ELM327", "Wi-Fi", 1_700_000_000_000, signals)

    @Test
    fun aContractSnapshotParsesEveryField() {
        val s = snapshot()!!
        assertEquals(VPM_BRIDGE_VERSION, s.version)
        assertEquals(VpmConnection.CONNECTED, s.connection)
        assertEquals("Wi-Fi ELM327", s.status)
        assertEquals("Wi-Fi", s.source)
        assertEquals(1_700_000_000_000, s.updatedAt)
        assertEquals(7, s.signals.size)
        val coolant = s.signal("engine.coolant_c")!!
        assertEquals("ОЖ", coolant.label)
        assertEquals("86", coolant.value)
        assertEquals("°C", coolant.unit)
        assertEquals(86.0, coolant.raw!!, 0.0)
        assertEquals("temperatures", coolant.category)
        assertEquals(0.62, s.signal("engine.boost_kpa")!!.raw!!, 0.0)
        assertEquals("5,0", s.signal("fuel.l100_km")!!.value)
    }

    @Test
    fun anotherContractVersionIsNotGuessed() {
        assertNull(snapshot(version = 2))
        assertNull(snapshot(version = 0))
    }

    @Test
    fun everyConnectionKindHasItsTruthfulness() {
        assertTrue(snapshot(connection = "demo")!!.live)
        assertTrue(snapshot(connection = "connected")!!.live)
        assertFalse(snapshot(connection = "connecting")!!.live)
        assertFalse(snapshot(connection = "disconnected")!!.live)
        assertFalse(snapshot(connection = "error")!!.live)
        assertFalse(snapshot(connection = "something-new")!!.live)
        assertEquals(VpmConnection.UNKNOWN, snapshot(connection = "something-new")!!.connection)
    }

    @Test
    fun aBrokenSignalsArrayIsEmptyNotACrash() {
        assertTrue(parseVpmSignals("not json").isEmpty())
        assertTrue(parseVpmSignals("").isEmpty())
        assertTrue(parseVpmSignals(null).isEmpty())
        assertTrue(parseVpmSignals("{}").isEmpty())
    }

    @Test
    fun oneBadEntryDoesNotLoseTheOthers() {
        val signals = parseVpmSignals(
            """[{"id":""},{"nope":1},{"id":"vehicle.speed","label":"Speed","raw":"42.5"},
               {"id":"vehicle.speed","label":"Duplicate","raw":1}]""",
        )
        assertEquals(1, signals.size)
        assertEquals("vehicle.speed", signals.single().id)
        assertEquals(42.5, signals.single().raw!!, 0.0)
    }

    @Test
    fun aSignalAppearsOncePerCategoryNotTwiceInOne() {
        val signals = parseVpmSignals(
            """[{"id":"engine.boost_kpa","raw":0.62,"category":"engine"},
                {"id":"engine.boost_kpa","raw":0.62,"category":"turbo"},
                {"id":"engine.boost_kpa","raw":0.62,"category":"engine"}]""",
        )
        assertEquals(2, signals.size)
        assertEquals(listOf("engine", "turbo"), signals.map { it.category })
        assertEquals("engine", signals.first { it.id == "engine.boost_kpa" }.category)
    }

    @Test
    fun missingValuesStayMissing() {
        val s = parseVpmSignals(
            """[{"id":"engine.oil_c","label":"Oil","value":null,"raw":null},
                {"id":"vehicle.gear","label":"Gear"}]""",
        )
        assertEquals(2, s.size)
        assertNull(s[0].value)
        assertNull(s[0].raw)
        assertNull(s[1].value)
        assertEquals("Gear", s[1].label)
    }

    @Test
    fun aLabelFallsBackToTheIdAndTheSignalCapHolds() {
        val array = JSONArray()
        repeat(VPM_MAX_SIGNALS + 20) { i ->
            array.put(JSONObject().put("id", "signal.$i").put("label", "").put("raw", i))
        }
        val parsed = parseVpmSignals(array.toString())
        assertEquals(VPM_MAX_SIGNALS, parsed.size)
        assertEquals("signal.0", parsed.first().label)
    }

    @Test
    fun aSnapshotWithoutSignalsIsStillUsable() {
        val s = snapshot(signals = "[]")!!
        assertTrue(s.signals.isEmpty())
        assertNull(s.signal("vehicle.speed"))
    }
}
