package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetFormTest {

    /** One space of each shape, in dp, about as big as a phone's home page makes them. */
    private val spaces = mapOf(
        WidgetForm.Mini to (110f to 200f),
        WidgetForm.Strip to (170f to 90f),
        WidgetForm.Square to (170f to 190f),
        WidgetForm.Wide to (350f to 190f),
        WidgetForm.Tall to (170f to 330f),
        WidgetForm.Big to (350f to 330f),
    )

    @Test
    fun `each shape of space has a form of its own`() {
        for ((form, space) in spaces) assertEquals(form, WidgetForm.of(space.first, space.second))
    }

    @Test
    fun `four toggles in a two by two are round buttons with their names`() {
        val grid = ToggleGrid.of(170f, 190f, chosen = 4)
        assertEquals(4, grid.shown)
        assertEquals(2, grid.columns)
        assertEquals(2, grid.rows)
        assertEquals(ToggleGrid.Style.Named, grid.style)
    }

    @Test
    fun `wide cells become tiles, with a second line only when they are tall enough`() {
        val wide = ToggleGrid.of(350f, 190f, chosen = 4)
        assertEquals(ToggleGrid.Style.Tiles, wide.style)
        assertFalse(wide.detail)
        val large = ToggleGrid.of(350f, 330f, chosen = 4)
        assertEquals(ToggleGrid.Style.Tiles, large.style)
        assertTrue(large.detail)
    }

    @Test
    fun `one column of wide cells is a list`() {
        val tall = ToggleGrid.of(170f, 330f, chosen = 4)
        assertEquals(1, tall.columns)
        assertEquals(ToggleGrid.Style.List, tall.style)
    }

    @Test
    fun `a widget too small for everything chosen shows as many as can still be pressed`() {
        val strip = ToggleGrid.of(170f, 90f, chosen = 12)
        assertEquals(8, strip.shown)
        assertEquals(4, strip.columns)
        assertTrue(minOf(strip.cellWidth, strip.cellHeight) >= ToggleGrid.MIN_CELL)
        assertEquals(ToggleGrid.Style.Buttons, strip.style)
        // Never none at all, however small.
        assertEquals(1, ToggleGrid.of(40f, 40f, chosen = 4).shown)
    }

    @Test
    fun `every widget says what it shows in every shape, and no two shapes alike`() {
        for (kind in listOf("clock", "battery", "calendar", "notes", "media", "world", "countdown", "photo", "todo", "aura", "weather")) {
            val notes = spaces.values.map { (width, height) -> WidgetLayouts.note(kind, width, height) }
            notes.forEach { assertNotNull(kind, it) }
            assertEquals("$kind: $notes", notes.size, notes.toSet().size)
        }
    }

    @Test
    fun `the toggles say how they are set out, and when some are left out`() {
        assertEquals("Round buttons, each with its name under it.", WidgetLayouts.note("toggles", 170f, 190f, toggles = 4))
        assertEquals(
            "Round buttons, the icons alone. Room for 8 of your 12.",
            WidgetLayouts.note("toggles", 170f, 90f, toggles = 12),
        )
    }

    @Test
    fun `a kind with no layouts of its own has nothing to say`() {
        assertNull(WidgetLayouts.note("search", 350f, 90f))
    }
}
