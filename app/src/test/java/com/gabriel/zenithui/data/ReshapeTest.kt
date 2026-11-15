package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
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
    fun `resizing drops a free-placement offset, as dragging does`() {
        val items = listOf(widget("w", 1, 1).nudged(0.3f, -0.2f))
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

    @Test
    fun `in edit mode every widget has a corner to pull, and an app only once it is chosen to be resized`() {
        assertTrue(Reshape.hasCorner(widget("w", 0, 0), chosen = false))
        assertTrue(Reshape.hasCorner(HostedWidgetCell("h", 0, 0, 0, 4, 2 * ROW_UNITS, appWidgetId = 17, provider = "com.weather/.Widget"), chosen = false))
        assertTrue(Reshape.hasCorner(StackCell("st", 0, 0, 0, 2, 2 * ROW_UNITS, listOf(StackMember("m1", type = "clock"))), chosen = false))
        assertFalse(Reshape.hasCorner(app("a", 0, 0), chosen = false))
        assertTrue(Reshape.hasCorner(app("a", 0, 0), chosen = true))
        // A folder and a shortcut are always one cell: there is nothing to pull.
        assertFalse(Reshape.hasCorner(FolderCell("f", 0, 0, 0, "Folder", listOf("x/y")), chosen = true))
        assertFalse(Reshape.hasCorner(ShortcutCell("s", 0, 0, 0, "com.maps", "home", "Home"), chosen = true))
    }

    @Test
    fun `without pulling, it grows or shrinks a column or a row at a time, as far as the page goes`() {
        fun steps(item: HomeItem) = Reshape.steps(item, columns, rows).map { it.name to (it.spanX to it.spanY / ROW_UNITS) }
        // In the middle of the page it can go every way.
        assertEquals(
            listOf("Wider" to (3 to 2), "Narrower" to (1 to 2), "Taller" to (2 to 3), "Shorter" to (2 to 1)),
            steps(widget("w", 1, 1)),
        )
        // Against the page's right edge it grows no wider, and at its foot no taller.
        assertEquals(listOf("Narrower", "Taller", "Shorter"), steps(widget("w", 2, 1)).map { it.first })
        assertEquals(listOf("Wider", "Narrower", "Shorter"), steps(widget("w", 1, 3)).map { it.first })
        // An icon in the last cell of the page has nowhere to go, and a folder never has.
        assertEquals(emptyList<String>(), steps(app("a", 3, 4)).map { it.first })
        assertEquals(listOf("Wider", "Taller"), steps(app("a", 0, 0)).map { it.first })
        assertEquals(emptyList<String>(), steps(FolderCell("f", 0, 0, 0, "Folder", listOf("x/y"))).map { it.first })
    }
}
