package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarryManyTest {

    private fun app(id: String, page: Int, col: Int, row: Int) = AppCell(id, page, col, row, "com.$id", ".Main")

    @Test
    fun `gathered icons land round the one let go, on its page`() {
        val layout = listOf(app("main", 1, 2, 2 * ROW_UNITS), app("x", 0, 0, 0), app("y", 0, 3, 4 * ROW_UNITS))
        val landed = LayoutBuilder.landBeside(layout, "main", listOf("x", "y"), columns = 4, rows = 5)
        val x = landed.single { it.id == "x" }
        val y = landed.single { it.id == "y" }
        assertEquals(1, x.page)
        assertEquals(1, y.page)
        // Side by side with it: the nearest spots, never on top of it or each other.
        val spots = landed.filter { it.page == 1 }.map { it.col to it.row }
        assertEquals(spots.size, spots.toSet().size)
        assertTrue(kotlin.math.abs(x.col - 2) <= 1 && kotlin.math.abs(x.row - 2 * ROW_UNITS) <= ROW_UNITS)
    }

    @Test
    fun `with the page full, a gathered icon stays where it was`() {
        val full = (0 until 4).flatMap { c -> (0 until 5).map { r -> app("f$c$r", 1, c, r * ROW_UNITS) } }
        val layout = full + app("x", 0, 0, 0)
        val landed = LayoutBuilder.landBeside(layout, "f00", listOf("x"), columns = 4, rows = 5)
        assertEquals(app("x", 0, 0, 0), landed.single { it.id == "x" })
    }

    @Test
    fun `nothing gathered, nothing moves`() {
        val layout = listOf(app("main", 0, 0, 0))
        assertEquals(layout, LayoutBuilder.landBeside(layout, "main", emptyList(), 4, 5))
        assertEquals(layout, LayoutBuilder.landBeside(layout, "gone", listOf("main"), 4, 5))
    }
}
