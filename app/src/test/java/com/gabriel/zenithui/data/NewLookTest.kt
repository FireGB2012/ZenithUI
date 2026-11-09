package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewLookTest {

    /** A phone that had Zenith before the new look, with every one of its settings set otherwise. */
    private val before = ZenithSettings(
        onboarded = true,
        userName = "Gabriel",
        paletteId = "sunset",
        gridDensity = GridDensity.CUSTOM,
        gridColumns = 5,
        gridRows = 5,
        columns = 5,
        rows = 5,
        iconShape = "clover",
        widgetLook = WidgetLooks.GLASS,
        dockStyle = "floating",
        showDock = false,
        newLookAsked = false,
    )

    @Test
    fun `the new look sets the grid, the icons, the widgets and the dock, and nothing else`() {
        assertEquals(
            before.copy(
                gridDensity = "four",
                iconShape = "iphone",
                widgetLook = "colour",
                dockStyle = "glass",
                showDock = true,
                newLookAsked = true,
            ),
            NewLook.apply(before),
        )
    }

    @Test
    fun `it is due once, on a phone that was set up before it`() {
        assertTrue(NewLook.due(ZenithSettings(onboarded = true)))
        assertFalse(NewLook.due(ZenithSettings(onboarded = true, newLookAsked = true)))
        // A new install is set up on the new look and never asked.
        assertFalse(NewLook.due(ZenithSettings(onboarded = false)))
        // Either answer puts the question for good: switching, or keeping the home screen as it is.
        assertFalse(NewLook.due(NewLook.apply(before)))
        assertFalse(NewLook.due(before.copy(newLookAsked = true)))
    }

    @Test
    fun `the new look is on once its grid, icons, widgets and dock are`() {
        assertFalse(NewLook.isOn(ZenithSettings()))
        assertFalse(NewLook.isOn(before))
        val switched = NewLook.apply(before)
        assertTrue(NewLook.isOn(switched))
        // Any one of the four set otherwise is a look of your own.
        assertFalse(NewLook.isOn(switched.copy(gridDensity = "balanced")))
        assertFalse(NewLook.isOn(switched.copy(iconShape = "squircle")))
        assertFalse(NewLook.isOn(switched.copy(widgetLook = "glass")))
        assertFalse(NewLook.isOn(switched.copy(dockStyle = "none")))
        // Hiding the dock is a switch of its own, not another look; nor is having been asked.
        assertTrue(NewLook.isOn(switched.copy(showDock = false)))
        assertTrue(NewLook.isOn(switched.copy(newLookAsked = false)))
    }

    @Test
    fun `switching a 5x5 home to the iPhone grid keeps every item`() {
        val home = fiveByFive()
        // It is a real 5x5 home: everything on it already has a place of its own.
        assertEquals(home, LayoutBuilder.settle(home, 5, 5, free = false))

        // The grid the new look gives a Pixel 10a.
        val switched = NewLook.apply(before)
        val (columns, rows) = GridDensity.resolve(switched.gridDensity, switched.gridColumns, switched.gridRows, 411f, 923f)
        assertEquals(4 to 6, columns to rows)

        val settled = LayoutBuilder.settle(home, columns, rows, free = false)
        assertEquals(home.map { it.id }, settled.map { it.id })
        // And each of them has a place on the new grid, on a whole row, with nothing on top of it.
        for (page in settled.map { it.page }.distinct()) {
            assertTrue(page in 0 until LayoutBuilder.MAX_PAGES)
            val grid = PageGrid(columns, rows * ROW_UNITS)
            for (item in settled.filter { it.page == page }) {
                val where = "${item.id} at ${item.col}, ${item.row} on page $page"
                assertTrue("$where is not on a whole row", item.row % ROW_UNITS == 0)
                assertTrue("$where is off the grid or under something", grid.isFree(item.col, item.row, item.spanX, item.spanY))
                grid.occupyItem(item)
            }
        }
        // The first page held more than four columns can, so some of it went on to the next.
        assertTrue(settled.count { it.page == 0 } < home.count { it.page == 0 })
    }

    /**
     * A 5x5 home of 25 apps, two widgets and a folder: a clock and the weather across the top of
     * the first page, fourteen apps and the folder filling the rows under them, and the other eleven
     * apps on the second page.
     */
    private fun fiveByFive(): List<HomeItem> {
        val home = mutableListOf<HomeItem>(
            WidgetCell("clock", 0, 0, 0, "clock", spanX = 3, spanY = 2 * ROW_UNITS),
            WidgetCell("weather", 0, 3, 0, "weather", spanX = 2, spanY = 2 * ROW_UNITS),
            FolderCell("folder", 0, 4, 4 * ROW_UNITS, "Social", listOf("pkg.chat/Main", "pkg.photos/Main")),
        )
        val apps = (0 until 25).map { "app$it" }
        val underWidgets = (2 until 5).flatMap { row -> (0 until 5).map { col -> col to row } } - (4 to 4)
        underWidgets.forEachIndexed { i, (col, row) ->
            home += AppCell(apps[i], 0, col, row * ROW_UNITS, "pkg.${apps[i]}", "Main")
        }
        apps.drop(underWidgets.size).forEachIndexed { i, id ->
            home += AppCell(id, 1, i % 5, (i / 5) * ROW_UNITS, "pkg.$id", "Main")
        }
        return home
    }
}
