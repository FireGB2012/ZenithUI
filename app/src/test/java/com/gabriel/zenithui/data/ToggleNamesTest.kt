package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToggleNamesTest {

    private fun name(full: Float, short: Float = full) = ToggleNames.Widths(full, short)

    @Test
    fun `names that fit their tiles are set in full, at full size`() {
        val names = ToggleNames.fit(listOf(name(40f), name(60f, 30f)), room = 82f)
        assertEquals(listOf(false, false), names.short)
        assertEquals(1f, names.scale, 0f)
    }

    @Test
    fun `a name too long for its tile takes its short form, and only that one`() {
        // "Do Not Disturb" in a wide tile becomes "Focus"; "Wi-Fi" beside it stays as it is.
        val names = ToggleNames.fit(listOf(name(36f), name(101f, 40f)), room = 82f)
        assertEquals(listOf(false, true), names.short)
        assertEquals(1f, names.scale, 0f)
    }

    @Test
    fun `every tile shares one size, which the tightest name sets`() {
        val names = ToggleNames.fit(listOf(name(30f), name(70f, 70f)), room = 66.5f)
        assertEquals(66.5f / 70f, names.scale, 0.0001f)
    }

    @Test
    fun `names are never set smaller than twelve of fourteen`() {
        val names = ToggleNames.fit(listOf(name(200f, 150f)), room = 60f)
        assertEquals(ToggleNames.SMALLEST, names.scale, 0.0001f)
        assertTrue(ToggleNames.SMALLEST * 14f >= 12f - 0.0001f)
    }

    @Test
    fun `no tiles, or no room, leaves the names alone`() {
        assertEquals(ToggleNames(emptyList(), 1f), ToggleNames.fit(emptyList(), room = 80f))
        assertEquals(ToggleNames(listOf(false), 1f), ToggleNames.fit(listOf(name(90f, 40f)), room = 0f))
    }
}
