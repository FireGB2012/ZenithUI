package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderGridTest {

    @Test
    fun `nine to a page, across and then down`() {
        assertEquals(0, FolderGrid.column(0))
        assertEquals(0, FolderGrid.row(0))
        assertEquals(2, FolderGrid.column(2))
        assertEquals(0, FolderGrid.row(2))
        assertEquals(0, FolderGrid.column(3))
        assertEquals(1, FolderGrid.row(3))
        assertEquals(2, FolderGrid.column(8))
        assertEquals(2, FolderGrid.row(8))
        assertEquals(0, FolderGrid.pageOf(8))
        // The tenth is the first on the second page, in the same place the first was.
        assertEquals(1, FolderGrid.pageOf(9))
        assertEquals(0, FolderGrid.column(9))
        assertEquals(0, FolderGrid.row(9))
    }

    @Test
    fun `there is always a page, and only as many as it takes`() {
        assertEquals(1, FolderGrid.pages(0))
        assertEquals(1, FolderGrid.pages(1))
        assertEquals(1, FolderGrid.pages(9))
        assertEquals(2, FolderGrid.pages(10))
        assertEquals(2, FolderGrid.pages(18))
        assertEquals(3, FolderGrid.pages(19))
    }

    @Test
    fun `a page holds its own nine and no more`() {
        val apps = (1..11).map { "app$it" }
        assertEquals(apps.take(9), FolderGrid.onPage(apps, 0))
        assertEquals(listOf("app10", "app11"), FolderGrid.onPage(apps, 1))
        assertEquals(emptyList<String>(), FolderGrid.onPage(apps, 2))
    }

    @Test
    fun `the pane is one size on a phone, about three quarters of it`() {
        val size = FolderSize.of(411f)
        assertEquals(320f, size.width, 0.5f)
        assertEquals(36f, size.corner, 0f)
        // About the home screen's own icons on the same phone (0.72 of a column).
        assertTrue("icon ${size.icon}", size.icon in 66f..70f)
    }

    @Test
    fun `the pane keeps within its bounds on small and large screens`() {
        assertEquals(FolderSize.MIN_WIDTH, FolderSize.of(320f).width, 0f)
        assertEquals(FolderSize.MAX_WIDTH, FolderSize.of(840f).width, 0f)
        for (screen in listOf(300f, 360f, 393f, 411f, 480f, 600f, 840f)) {
            val size = FolderSize.of(screen)
            assertTrue("$screen: icon ${size.icon}", size.icon in FolderSize.MIN_ICON..FolderSize.MAX_ICON)
            assertTrue("$screen: the pane fits", size.width <= screen)
        }
    }

    @Test
    fun `three columns fill the pane and the rows make its height`() {
        for (screen in listOf(360f, 411f, 600f)) {
            val size = FolderSize.of(screen)
            assertEquals(size.width, 2 * size.side + FolderGrid.COLUMNS * size.column, 0.01f)
            assertTrue(size.icon <= size.column)
            assertEquals(size.icon + FolderSize.NAME_GAP + size.label + FolderSize.ROW_GAP, size.row, 0.01f)
            assertEquals(size.top + FolderGrid.ROWS * size.row + size.bottom, size.height, 0.01f)
        }
    }

    @Test
    fun `each icon stands in the middle of its column, and the name above starts where the first does`() {
        val size = FolderSize.of(411f)
        for (column in 0 until FolderGrid.COLUMNS) {
            val middle = size.iconLeft(column) + size.icon / 2f
            assertEquals(size.side + (column + 0.5f) * size.column, middle, 0.01f)
        }
        assertEquals(size.iconLeft(0), size.titleStart, 0f)
        assertEquals(size.top, size.iconTop(0), 0f)
        assertEquals(size.top + 2 * size.row, size.iconTop(2), 0f)
    }

    @Test
    fun `the pane and the name over it stand in the middle together`() {
        val size = FolderSize.of(411f)
        val screen = 891f
        val top = size.paneTop(screen)
        val above = FolderSize.TITLE_LINE + FolderSize.TITLE_GAP
        assertEquals(screen - (top + size.height), top - above, 0.01f)
        // Never off the top of a short space (the keyboard up).
        assertTrue(size.paneTop(300f) >= above)
    }
}
