package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutTidyTest {

    /** A phone that was last run before whole rows: version 1, icons tucking one unit. */
    private val old = ZenithSettings(columns = 4, rows = 5, layoutVersion = 1, widgetTuck = 1)

    private fun app(id: String, col: Int, row: Int) = AppCell(id, 0, col, row, "pkg.$id", "Main")

    private fun widget(id: String, col: Int, row: Int, spanX: Int, spanY: Int, type: String = "clock") =
        WidgetCell(id, 0, col, row, type, spanX, spanY)

    @Test
    fun `a search bar on the page becomes the bar above the dock`() {
        val bar = widget("bar", 0, 0, 4, 2, type = "search")
        val (settings, items) = LayoutTidy.upgrade(old, listOf(bar, app("a", 0, 2), app("b", 1, 2)))
        assertTrue(settings.searchBar)
        assertEquals(LayoutTidy.VERSION, settings.layoutVersion)
        assertEquals(0, settings.widgetTuck)
        // The icons that sat under it close the gap it left.
        assertEquals(listOf("a" to 0, "b" to 0), items.map { it.id to it.row })
    }

    @Test
    fun `a tucked icon comes down to the row under its widget`() {
        val clock = widget("clock", 0, 0, 2, 8)
        val beside = app("beside", 2, 8)
        val (settings, items) = LayoutTidy.upgrade(old, listOf(clock, app("tucked", 0, 7), beside))
        assertEquals(false, settings.searchBar)
        assertEquals(8, items.first { it.id == "tucked" }.row)
        assertEquals(beside, items.first { it.id == "beside" })
        assertEquals(clock, items.first { it.id == "clock" })
    }

    @Test
    fun `a widget on a quarter step moves to the nearest whole row`() {
        val (_, items) = LayoutTidy.upgrade(old, listOf(widget("clock", 0, 5, 2, 8)))
        assertEquals(4, items.single().row)
    }

    @Test
    fun `free placement keeps its layout and still loses the search widget`() {
        val bar = widget("bar", 0, 3, 4, 2, type = "search")
        val icon = AppCell("a", 0, 1, 5, "pkg.a", "Main", dx = 0.2f, dy = -0.3f)
        val small = widget("w", 2, 9, 2, 3, type = "battery")
        val (settings, items) = LayoutTidy.upgrade(old.copy(freeform = true), listOf(bar, icon, small))
        assertTrue(settings.searchBar)
        assertEquals(listOf<HomeItem>(icon, small), items)
    }

    @Test
    fun `free placement keeps its layout but an icon tucked under a widget no longer overlaps it`() {
        // Tucking is gone, so the icon on the clock's last unit would now count as on top of it,
        // and nothing on that page could step aside for a drop.
        val clock = widget("clock", 0, 0, 2, 8)
        val far = AppCell("far", 0, 3, 13, "pkg.far", "Main", dx = 0.2f, dy = -0.3f)
        val (_, items) = LayoutTidy.upgrade(old.copy(freeform = true), listOf(clock, app("tucked", 0, 7), far))
        assertEquals(8, items.first { it.id == "tucked" }.row)
        assertEquals(0, items.first { it.id == "tucked" }.col)
        assertEquals(clock, items.first { it.id == "clock" })
        assertEquals(far, items.first { it.id == "far" })
    }

    @Test
    fun `when Plus has ended a layout left in free placement goes onto whole rows`() {
        val lapsed = old.copy(freeform = true, layoutVersion = LayoutTidy.VERSION, widgetTuck = 0)
        val icon = AppCell("a", 0, 1, 5, "pkg.a", "Main", dx = 0.2f, dy = 0.3f)
        val small = widget("w", 2, 9, 2, 3, type = "battery")
        val tidied = LayoutTidy.afterPlusEnded(lapsed, listOf(icon, small))
        assertTrue(tidied.all { it.row % ROW_UNITS == 0 && it.spanY % ROW_UNITS == 0 && it.dx == 0f && it.dy == 0f })
        assertEquals(listOf("a", "w"), tidied.map { it.id })
    }

    @Test
    fun `when Plus has ended a layout that never used free placement is left alone`() {
        val now = old.copy(layoutVersion = LayoutTidy.VERSION, widgetTuck = 0)
        val items = listOf<HomeItem>(widget("clock", 0, 0, 2, 8), app("a", 2, 4))
        assertEquals(items, LayoutTidy.afterPlusEnded(now, items))
    }

    @Test
    fun `a layout at today's version is never rescaled`() {
        // Whole rows everywhere and something past the bottom of the page is what the old
        // double-upgrade repair looks for; on an up-to-date layout it must not fire.
        val now = old.copy(layoutVersion = LayoutTidy.VERSION, widgetTuck = 0)
        val items = listOf<HomeItem>(widget("clock", 0, 0, 2, 8), app("a", 0, 8), app("stranded", 0, 20))
        assertEquals(now to items, LayoutTidy.bringUpToDate(now, items))
    }

    @Test
    fun `a layout from before row units is rescaled and tidied in one go`() {
        val ancient = old.copy(layoutVersion = 0, widgetTuck = 0)
        val (settings, items) = LayoutTidy.bringUpToDate(ancient, listOf(widget("clock", 0, 0, 2, 2), app("a", 0, 2)))
        assertEquals(LayoutTidy.VERSION, settings.layoutVersion)
        assertEquals(0 to 8, items.first { it.id == "clock" }.let { it.row to it.spanY })
        assertEquals(8, items.first { it.id == "a" }.row)
    }

    @Test
    fun `an older layout that was rescaled twice is put right before the tidy`() {
        val (_, items) = LayoutTidy.bringUpToDate(old, listOf(widget("clock", 0, 0, 2, 32), app("a", 0, 32)))
        assertEquals(0 to 8, items.first { it.id == "clock" }.let { it.row to it.spanY })
        assertEquals(8, items.first { it.id == "a" }.row)
    }

    @Test
    fun `a layout already brought up to date comes back as it is`() {
        val now = old.copy(layoutVersion = LayoutTidy.VERSION, widgetTuck = 0)
        val items = listOf<HomeItem>(widget("clock", 0, 5, 2, 6))
        assertEquals(now to items, LayoutTidy.upgrade(now, items))
    }

    @Test
    fun `nothing is lost when the page has no room for the taller widgets`() {
        val items = listOf(
            widget("one", 0, 0, 4, 6, type = "notes"),
            widget("two", 0, 6, 4, 6, type = "notes"),
            widget("three", 0, 12, 4, 6, type = "notes"),
        )
        val (_, tidied) = LayoutTidy.upgrade(old, items)
        assertEquals(listOf("one", "two", "three"), tidied.map { it.id })
        assertEquals(listOf(0, 0, 1), tidied.map { it.page })
    }
}
