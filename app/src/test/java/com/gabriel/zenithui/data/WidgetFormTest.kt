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
    fun `each size of the clock is a design with a short name of its own`() {
        // Testers could not tell the clock's sizes apart: each now says which design it is.
        val names = spaces.values.map { (width, height) -> WidgetLayouts.style("clock", width, height) }
        names.forEach { name ->
            assertNotNull(name)
            assertTrue(name!!, name.length <= 12)
        }
        assertEquals(names.size, names.toSet().size)
        assertEquals("Digital", WidgetLayouts.style("clock", 170f, 90f))
        assertEquals("Analogue", WidgetLayouts.style("clock", 170f, 190f))
        // A "No glass" clock is set out just as the glass one is.
        assertEquals(WidgetLayouts.style("clock", 350f, 330f), WidgetLayouts.style(Widgets.baseType("bare_clock"), 350f, 330f))
    }

    @Test
    fun `a widget whose designs have no names of their own says none`() {
        assertNull(WidgetLayouts.style("battery", 170f, 190f))
        assertNull(WidgetLayouts.style("hologram", 170f, 190f))
    }

    @Test
    fun `four toggles in a two by two are round buttons, two by two`() {
        val grid = ToggleGrid.of(170f, 190f, chosen = 4)
        assertEquals(4, grid.shown)
        assertEquals(2, grid.columns)
        assertEquals(2, grid.rows)
        assertTrue(grid.slots.all { it.kind == ToggleGrid.Kind.Button })
    }

    @Test
    fun `wide toggles are tiles with their names, and big cells are cards`() {
        val wide = ToggleGrid.of(350f, 190f, chosen = 4)
        assertTrue(wide.slots.all { it.kind == ToggleGrid.Kind.Tile })
        val large = ToggleGrid.of(350f, 330f, chosen = 4)
        assertTrue(large.slots.all { it.kind == ToggleGrid.Kind.Card })
    }

    @Test
    fun `one column of wide cells is a list of tiles`() {
        val tall = ToggleGrid.of(170f, 330f, chosen = 4)
        assertEquals(1, tall.columns)
        assertTrue(tall.slots.all { it.kind == ToggleGrid.Kind.Tile })
    }

    @Test
    fun `a widget too small for everything chosen shows as many as can still be pressed`() {
        val strip = ToggleGrid.of(170f, 90f, chosen = 12)
        assertEquals(3, strip.shown)
        assertEquals(9, strip.more)
        assertEquals(ToggleGrid.Kind.More, strip.slots.last().kind)
        assertTrue(strip.button >= ToggleGrid.MIN_STRIP_BUTTON)
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
        assertEquals("Round buttons, the symbols alone, as Control Center has them.", WidgetLayouts.note("toggles", 170f, 190f, toggles = 4))
        assertEquals("Tiles, each with its name and whether it is on.", WidgetLayouts.note("toggles", 350f, 190f, toggles = 4))
        assertEquals("The first two as tiles with their names, the rest as round buttons.", WidgetLayouts.note("toggles", 350f, 190f, toggles = 6))
        assertEquals(
            "Round buttons, the symbols alone, as Control Center has them. Room for 3 of your 12; the last button opens the rest.",
            WidgetLayouts.note("toggles", 170f, 90f, toggles = 12),
        )
    }

    @Test
    fun `a kind with no layouts of its own has nothing to say`() {
        assertNull(WidgetLayouts.note("search", 350f, 90f))
    }
}
