package net.prototux.aare.ui.car.obd

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CockpitLayoutTest {
    @Test
    fun aLivePaneGetsTheCompactCard() {
        assertTrue(cockpitIsCompact(720.dp, 680.dp))
        assertTrue(cockpitIsCompact(1264.dp, 340.dp))
        assertTrue(cockpitIsCompact(640.dp, 360.dp))
    }

    @Test
    fun aHeadUnitsOwnScreenKeepsTheTabs() {
        assertFalse(cockpitIsCompact(1280.dp, 720.dp))
        assertFalse(cockpitIsCompact(1024.dp, 600.dp))
        assertFalse(cockpitIsCompact(800.dp, 480.dp))
    }
}
