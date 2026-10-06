package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToggleNamesTest {

    @Test
    fun `names that fit their cell are set full size on one line`() {
        assertEquals(ToggleNames(1f, 1), ToggleNames.fit(widestName = 57f, widestWord = 57f, cell = 74f, roomForTwo = true))
        assertEquals(ToggleNames(1f, 1), ToggleNames.fit(widestName = 74f, widestWord = 40f, cell = 74f, roomForTwo = false))
    }

    @Test
    fun `a name too long for its cell goes onto a second line when there is room`() {
        // "Do Not Disturb" in a two by two: wider than the cell, but no word of it is.
        assertEquals(ToggleNames(1f, 2), ToggleNames.fit(widestName = 88f, widestWord = 45f, cell = 74f, roomForTwo = true))
    }

    @Test
    fun `a single word wider than the cell shrinks every name until it fits`() {
        val names = ToggleNames.fit(widestName = 90f, widestWord = 80f, cell = 72f, roomForTwo = true)
        assertEquals(2, names.lines)
        assertEquals(0.9f, names.scale, 0.0001f)
    }

    @Test
    fun `with no room for a second line the names shrink to stay on one`() {
        val names = ToggleNames.fit(widestName = 80f, widestWord = 45f, cell = 72f, roomForTwo = false)
        assertEquals(1, names.lines)
        assertEquals(0.9f, names.scale, 0.0001f)
    }

    @Test
    fun `names are never set smaller than ten of twelve`() {
        val oneLine = ToggleNames.fit(widestName = 200f, widestWord = 60f, cell = 60f, roomForTwo = false)
        assertEquals(ToggleNames.SMALLEST, oneLine.scale, 0.0001f)
        val twoLines = ToggleNames.fit(widestName = 200f, widestWord = 150f, cell = 60f, roomForTwo = true)
        assertEquals(ToggleNames.SMALLEST, twoLines.scale, 0.0001f)
        assertTrue(ToggleNames.SMALLEST * 12f >= 10f - 0.0001f)
    }

    @Test
    fun `a cell with no width leaves the names alone`() {
        assertEquals(ToggleNames(1f, 1), ToggleNames.fit(widestName = 80f, widestWord = 40f, cell = 0f, roomForTwo = true))
    }
}
