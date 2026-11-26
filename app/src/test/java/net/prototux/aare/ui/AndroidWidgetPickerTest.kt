package net.prototux.aare.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidWidgetPickerTest {

    @Test
    fun aWidgetGetsCellsFromItsProvidersMinimumSize() {
        // A 1×1 clock (about 110×40 dp) still needs one row; a 4×2 weather widget gets 3 columns.
        assertEquals(2 to 1, defaultWidgetRect(110, 40))
        assertEquals(3 to 2, defaultWidgetRect(250, 110))
        // A small widget still gets one whole cell, never a zero-sized one.
        assertEquals(1 to 1, defaultWidgetRect(30, 20))
        // Huge minimums are clamped to the 10×6 grid.
        assertEquals(10 to 6, defaultWidgetRect(2000, 2000))
    }
}
