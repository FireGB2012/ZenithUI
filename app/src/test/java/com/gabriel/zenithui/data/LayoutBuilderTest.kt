package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutBuilderTest {

    private fun app(id: String, col: Int, row: Int, dy: Float = 0f) =
        AppCell(id, 0, col, row, "pkg.$id", "Main", dy = dy)

    private fun widget(id: String, col: Int, row: Int, spanX: Int, spanY: Int, type: String = "clock") =
        WidgetCell(id, 0, col, row, type, spanX, spanY)

    @Test
    fun `heights round to the nearest whole row and never to none`() {
        assertEquals(listOf(4, 4, 4, 4, 4, 8, 8, 8, 8, 12), (1..10).map { LayoutBuilder.wholeRows(it) })
    }

    @Test
    fun `a row between two goes to the nearer and halfway goes up the page`() {
        assertEquals(0, LayoutBuilder.nearestRow(1f))
        assertEquals(0, LayoutBuilder.nearestRow(2f))
        assertEquals(4, LayoutBuilder.nearestRow(2.5f))
        assertEquals(4, LayoutBuilder.nearestRow(3f))
        assertEquals(8, LayoutBuilder.nearestRow(7f))
        assertEquals(8, LayoutBuilder.nearestRow(10f))
    }

    @Test
    fun `an icon that was tucked under a widget settles on the row under it`() {
        val clock = widget("clock", 0, 0, 2, 8)
        val tucked = app("tucked", 0, 7)
        val settled = LayoutBuilder.settle(listOf(clock, tucked), columns = 4, rows = 5, free = false)
        assertEquals(8, settled.first { it.id == "tucked" }.row)
        assertEquals(0, settled.first { it.id == "tucked" }.col)
        assertEquals(clock, settled.first { it.id == "clock" })
    }

    @Test
    fun `a widget a row and a half tall grows to two rows and what it then covers moves down`() {
        val note = widget("note", 0, 0, 2, 6, type = "notes")
        val under = app("under", 0, 6)
        val settled = LayoutBuilder.settle(listOf(note, under), 4, 5, free = false)
        val grown = settled.first { it.id == "note" }
        assertEquals(8, grown.spanY)
        assertEquals(0, grown.row)
        // Straight down in its own column, not two columns across.
        assertEquals(0 to 8, settled.first { it.id == "under" }.let { it.col to it.row })
    }

    @Test
    fun `a layout already on whole rows is left alone`() {
        val items = listOf(widget("clock", 0, 0, 2, 8), app("a", 2, 0), app("b", 0, 8), app("c", 3, 16))
        assertEquals(items, LayoutBuilder.settle(items, 4, 5, free = false))
    }

    @Test
    fun `free placement keeps quarter rows and offsets`() {
        val items = listOf(app("a", 1, 5, dy = 0.3f), widget("w", 2, 1, 2, 3, type = "battery"))
        assertEquals(items, LayoutBuilder.settle(items, 4, 5, free = true))
    }

    @Test
    fun `nothing is lost when taller widgets no longer fit the page`() {
        // Three full-width widgets a row and a half tall filled the page; as two-row widgets only two fit.
        val items = listOf(
            widget("one", 0, 0, 4, 6, type = "notes"),
            widget("two", 0, 6, 4, 6, type = "notes"),
            widget("three", 0, 12, 4, 6, type = "notes"),
        )
        val settled = LayoutBuilder.settle(items, 4, 5, free = false)
        assertEquals(listOf("one", "two", "three"), settled.map { it.id })
        assertTrue(settled.all { it.spanY == 8 && it.row % ROW_UNITS == 0 })
        assertEquals(listOf(0 to 0, 0 to 8, 1 to 0), settled.map { it.page to it.row })
    }

    @Test
    fun `setup fills the dock with four apps, or one for each column when there are fewer`() {
        assertEquals(listOf(3, 4, 4, 4), listOf(3, 4, 5, 8).map { LayoutBuilder.setupDockSize(it) })
    }

    @Test
    fun `the dock holds one app for each column and never fewer than four`() {
        assertEquals(listOf(4, 4, 5, 8), listOf(3, 4, 5, 8).map { LayoutBuilder.dockLimit(it) })
    }

    @Test
    fun `setup puts widgets and icons on whole rows and leaves the search bar out`() {
        val grid = PageGrid(4, 20)
        assertEquals(0 to 0, grid.place(2, 8, rowStep = ROW_UNITS))
        assertEquals(2 to 0, grid.place(1, ROW_UNITS, rowStep = ROW_UNITS))
        assertEquals(listOf(Widgets.Clock, Widgets.Battery, Widgets.Calendar, Widgets.Notes, Widgets.Toggles), Widgets.placeable)
        assertTrue(Widgets.placeable.all { it.minSpanY % ROW_UNITS == 0 })
    }
}
