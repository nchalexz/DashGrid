package net.prototux.aare.ui.car

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DockButtonsTest {

    @Test
    fun withBothSettingsOffTheOldSingleButtonStays() {
        assertTrue(dockShowsCombinedButton(onWorkspace = true, alwaysShowHome = false, alwaysShowAppDash = false))
        assertTrue(dockShowsCombinedButton(onWorkspace = false, alwaysShowHome = false, alwaysShowAppDash = false))
    }

    @Test
    fun aFixedButtonReplacesTheSwitchingOneOnlyWhenItWouldRepeatIt() {
        // On a workspace the switching button is Apps: pinning Home leaves it (Home is a different
        // action), pinning Apps drops it as a duplicate.
        assertTrue(dockShowsCombinedButton(onWorkspace = true, alwaysShowHome = true, alwaysShowAppDash = false))
        assertFalse(dockShowsCombinedButton(onWorkspace = true, alwaysShowHome = false, alwaysShowAppDash = true))
        // In an app or the grid the switching button is Home: the other way round.
        assertFalse(dockShowsCombinedButton(onWorkspace = false, alwaysShowHome = true, alwaysShowAppDash = false))
        assertTrue(dockShowsCombinedButton(onWorkspace = false, alwaysShowHome = false, alwaysShowAppDash = true))
        // With both fixed there is nothing left for the switching button to do.
        assertFalse(dockShowsCombinedButton(onWorkspace = true, alwaysShowHome = true, alwaysShowAppDash = true))
        assertFalse(dockShowsCombinedButton(onWorkspace = false, alwaysShowHome = true, alwaysShowAppDash = true))
    }
}
