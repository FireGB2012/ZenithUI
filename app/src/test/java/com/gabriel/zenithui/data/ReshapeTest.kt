package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ReshapeTest {

    private val columns = 4
    private val rows = 5

    private fun widget(id: String, col: Int, row: Int, spanX: Int = 2, rowsTall: Int = 2, page: Int = 0) =
        WidgetCell(id, page, col, row * ROW_UNITS, "clock", spanX, rowsTall * ROW_UNITS)

    private fun app(id: String, col: Int, row: Int, page: Int = 0) = AppCell(id, page, col, row * ROW_UNITS, "pkg.$id", "Main")

    private fun List<HomeItem>.at(id: String) = first { it.id == id }.let { Triple(it.col, it.row / ROW_UNITS, it.spanX to it.spanY / ROW_UNITS) }

    @Test
    fun `a new size is taken where the widget stands when there is room`() {
        val items = listOf(widget("w", 0, 0), app("a", 0, 3))
        val wide = Reshape.resized(items, "w", 4, 2 * ROW_UNITS, columns, rows)!!
        assertEquals(Triple(0, 0, 4 to 2), wide.at("w"))
        assertEquals(items.first { it.id == "a" }, wide.first { it.id == "a" })
    }

    @Test
    fun `asked to be wide in the last columns, it slides across rather than refusing`() {
        val items = listOf(widget("w", 2, 1))
        assertEquals(Triple(0, 1, 4 to 2), Reshape.resized(items, "w", 4, 2 * ROW_UNITS, columns, rows)!!.at("w"))
        // And made taller at the foot of the page, it moves up.
        val low = listOf(widget("w", 0, 3))
        assertEquals(Triple(0, 2, 2 to 3), Reshape.resized(low, "w", 2, 3 * ROW_UNITS, columns, rows)!!.at("w"))
    }

    @Test
    fun `it goes to the closest opening when something is in the way`() {
        // An app sits right of the widget: growing wide there would cover it.
        val items = listOf(widget("w", 0, 0), app("a", 2, 0))
        val wide = Reshape.resized(items, "w", 4, 2 * ROW_UNITS, columns, rows)!!
        assertEquals(Triple(0, 1, 4 to 2), wide.at("w"))
        assertEquals(Triple(2, 0, 1 to 1), wide.at("a"))
    }

    @Test
    fun `a size the page has no room for anywhere is refused`() {
        val items = listOf(widget("w", 0, 0)) + (0 until 4).map { app("a$it", it, 2) } + (0 until 4).map { app("b$it", it, 4) }
        assertNull(Reshape.resized(items, "w", 4, 3 * ROW_UNITS, columns, rows))
        // Bigger than the page is never possible.
        assertNull(Reshape.resized(listOf(widget("w", 0, 0)), "w", 5, 2 * ROW_UNITS, columns, rows))
    }

    @Test
    fun `the size it already has changes nothing, and nor does a widget that is not there`() {
        val items = listOf(widget("w", 1, 1))
        assertSame(items, Reshape.resized(items, "w", 2, 2 * ROW_UNITS, columns, rows))
        assertNull(Reshape.resized(items, "gone", 2, 2 * ROW_UNITS, columns, rows))
    }

    @Test
    fun `other pages are left alone, and do not count as in the way`() {
        val items = listOf(widget("w", 0, 0), widget("far", 0, 0, spanX = 4, rowsTall = 5, page = 1))
        val large = Reshape.resized(items, "w", 4, 3 * ROW_UNITS, columns, rows)!!
        assertEquals(Triple(0, 0, 4 to 3), large.at("w"))
        assertEquals(1, large.first { it.id == "far" }.page)
    }

    @Test
    fun `a nudge moves one cell, and stops at the edge of the page`() {
        val items = listOf(widget("w", 1, 1))
        assertEquals(Triple(2, 1, 2 to 2), Reshape.nudged(items, "w", 1, 0, columns, rows)!!.at("w"))
        assertEquals(Triple(0, 1, 2 to 2), Reshape.nudged(items, "w", -1, 0, columns, rows)!!.at("w"))
        assertEquals(Triple(1, 0, 2 to 2), Reshape.nudged(items, "w", 0, -ROW_UNITS, columns, rows)!!.at("w"))
        assertEquals(Triple(1, 2, 2 to 2), Reshape.nudged(items, "w", 0, ROW_UNITS, columns, rows)!!.at("w"))
        // Two columns wide in the last two columns: no further right.
        assertNull(Reshape.nudged(listOf(widget("w", 2, 0)), "w", 1, 0, columns, rows))
        assertNull(Reshape.nudged(listOf(widget("w", 0, 0)), "w", -1, 0, columns, rows))
        assertNull(Reshape.nudged(listOf(widget("w", 0, 0)), "w", 0, -ROW_UNITS, columns, rows))
        assertNull(Reshape.nudged(listOf(widget("w", 0, 3)), "w", 0, ROW_UNITS, columns, rows))
    }

    @Test
    fun `a nudge never pushes anything aside`() {
        val items = listOf(widget("w", 0, 0), app("a", 2, 1))
        assertNull(Reshape.nudged(items, "w", 1, 0, columns, rows))
        // Down is clear.
        assertEquals(Triple(0, 1, 2 to 2), Reshape.nudged(items, "w", 0, ROW_UNITS, columns, rows)!!.at("w"))
        assertSame(items, Reshape.nudged(items, "w", 0, 0, columns, rows))
        assertNull(Reshape.nudged(items, "gone", 1, 0, columns, rows))
    }

    @Test
    fun `moving or resizing drops a free-placement offset, as dragging does`() {
        val items = listOf(widget("w", 1, 1).nudged(0.3f, -0.2f))
        val moved = Reshape.nudged(items, "w", 1, 0, columns, rows)!!.first()
        assertEquals(0f, moved.dx, 0f)
        assertEquals(0f, moved.dy, 0f)
        val resized = Reshape.resized(items, "w", 3, 2 * ROW_UNITS, columns, rows)!!.first()
        assertEquals(0f, resized.dx, 0f)
        assertEquals(3, resized.spanX)
    }

    @Test
    fun `an app takes a new size the same way, and a folder keeps the one it has`() {
        val apps = listOf(app("a", 3, 0))
        assertEquals(Triple(2, 0, 2 to 2), Reshape.resized(apps, "a", 2, 2 * ROW_UNITS, columns, rows)!!.at("a"))
        val folder = listOf(FolderCell("f", 0, 0, 0, "Folder", listOf("x/y")))
        assertSame(folder, Reshape.resized(folder, "f", 1, ROW_UNITS, columns, rows))
    }
}
