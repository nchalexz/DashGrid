package net.prototux.aare.ui.car.vpm

import net.prototux.aare.vpm.VpmRead
import net.prototux.aare.vpm.VPM_BRIDGE_VERSION
import net.prototux.aare.vpm.parseVpmSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VpmUiStateTest {
    private val snapshot = parseVpmSnapshot(VPM_BRIDGE_VERSION, "demo", "ДЕМО", "", 1L, "[]")!!

    @Test
    fun aMissingAppIsACardNotAnException() {
        assertEquals(VpmUiState.NotInstalled, vpmUiState(VpmRead.NotInstalled))
    }

    @Test
    fun anInstalledAppWithoutTheBridgeAsksForAnUpdate() {
        assertEquals(VpmUiState.BridgeMissing, vpmUiState(VpmRead.BridgeMissing))
    }

    @Test
    fun aProviderWithoutASnapshotStillDrawsTheCockpitWithDashes() {
        val state = vpmUiState(VpmRead.NoSnapshot)
        assertTrue(state is VpmUiState.Cockpit)
        assertEquals(null, (state as VpmUiState.Cockpit).snapshot)
    }

    @Test
    fun aSnapshotIsDrawnAsTheCockpit() {
        val state = vpmUiState(VpmRead.Snapshot(snapshot))
        assertEquals(VpmUiState.Cockpit(snapshot), state)
    }

    @Test
    fun aRefusedReadIsAnExplanation() {
        val state = vpmUiState(VpmRead.Failed("READ_TELEMETRY"))
        assertEquals(VpmUiState.Failed("READ_TELEMETRY"), state)
    }

    @Test
    fun staleValuesKeepTheCockpitOnScreenInsteadOfBecomingACard() {
        val stale = parseVpmSnapshot(
            VPM_BRIDGE_VERSION,
            "connected",
            "OBDII",
            "OBDII",
            1L,
            """[{"id":"engine.coolant_c","value":"86","raw":86,"stale":true,"status":"stale"}]""",
        )!!
        val state = vpmUiState(VpmRead.Snapshot(stale))
        assertTrue(state is VpmUiState.Cockpit)
        assertEquals("86", (state as VpmUiState.Cockpit).snapshot!!.signal("engine.coolant_c")!!.value)
    }
}
