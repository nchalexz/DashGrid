package net.prototux.aare.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DockIconToneTest {

    @Test
    fun everyToneIsGrayscaleAndKeepsAlpha() {
        for (tone in DockIconTone.entries) {
            val m = tone.matrixArray()
            // The three colour rows are identical: nothing of the original hue is left.
            for (i in 0 until 3) {
                for (j in 0 until 5) assertEquals("$tone row $i column $j", m[j], m[(i * 5) + j], 0f)
            }
            // Alpha passes through untouched: the icon's shape and transparency survive.
            val alpha = m.copyOfRange(15, 20)
            assertEquals(listOf(0f, 0f, 0f, 1f, 0f), alpha.toList())
        }
    }

    @Test
    fun luminanceWeightsAreTheStandardOnes() {
        val m = DockIconTone.GREY.matrixArray()
        assertEquals(0.2126f, m[0], 1e-5f)
        assertEquals(0.7152f, m[1], 1e-5f)
        assertEquals(0.0722f, m[2], 1e-5f)
    }

    @Test
    fun theTonesGoFromLightToDarkInTheOrderTheSettingsListThem() {
        fun range(tone: DockIconTone): Pair<Float, Float> {
            val m = tone.matrixArray()
            val black = m[4] / 255f
            val white = black + m[0] + m[1] + m[2]
            return black to white
        }
        val black = range(DockIconTone.BLACK)
        val darkGrey = range(DockIconTone.DARK_GREY)
        val grey = range(DockIconTone.GREY)
        val white = range(DockIconTone.WHITE)
        assertTrue(black.second < grey.second)
        assertTrue(darkGrey.second < grey.second)
        assertTrue(grey.second < white.second)
        assertTrue(white.first > 0.3f) // a dark logo stays visible on a dark background
        assertTrue(black.first <= 0.02f) // the black tone is nearly black at its darkest
        assertTrue(black.second < 0.5f) // ...but never reaches mid grey: detail stays dark
    }

    @Test
    fun everyToneHasItsOwnMatrixAndAnIdThatRoundTrips() {
        val matrices = DockIconTone.entries.map { it.matrixArray().toList() }.toSet()
        assertEquals(DockIconTone.entries.size, matrices.size)
        for (tone in DockIconTone.entries) {
            assertEquals(tone, DockIconTone.byId(tone.id))
        }
        assertNotEquals(DockIconTone.WHITE, DockIconTone.BLACK)
        assertNull(DockIconTone.byId("no_such_tone"))
    }

    @Test
    fun theStoredIdsAreStable() {
        // These strings are in users' settings and in exported settings files: never change one.
        assertEquals(listOf("white", "grey", "dark_grey", "black"), DockIconTone.entries.map { it.id })
    }
}
