package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UndoTest {

    private val removed = AppCell("a", 0, 1, 2 * ROW_UNITS, "com.mail", ".Main")

    @Test
    fun `what was taken off goes back where it was, if that spot is free`() {
        val others = listOf(AppCell("b", 0, 0, 0, "com.phone", ".Main"))
        assertEquals(removed, LayoutBuilder.putBack(others, removed, columns = 4, rows = 5))
    }

    @Test
    fun `with its spot taken meanwhile, it goes to the first opening`() {
        val taker = AppCell("t", 0, 1, 2 * ROW_UNITS, "com.camera", ".Main")
        val back = LayoutBuilder.putBack(listOf(taker), removed, columns = 4, rows = 5)!!
        assertEquals(Triple(0, 0, 0), Triple(back.page, back.col, back.row))
        assertEquals("a", back.id)
    }

    @Test
    fun `with no room anywhere there is nowhere to put it`() {
        // One widget over the whole page, on every page there can be.
        val full = (0 until LayoutBuilder.MAX_PAGES).map { page -> WidgetCell("w$page", page, 0, 0, "clock", 4, 5 * ROW_UNITS) }
        assertNull(LayoutBuilder.putBack(full, removed, columns = 4, rows = 5))
    }

    @Test
    fun `a new id changes nothing else`() {
        val items: List<HomeItem> = listOf(
            removed,
            FolderCell("f", 1, 0, 0, "Fun", listOf("a/b", "c/d")),
            WidgetCell("w", 0, 0, 0, "clock", 4, 8, accent = 0xFFFF0000),
            ShortcutCell("s", 0, 2, 0, "com.maps", "home", "Home"),
            HostedWidgetCell("h", 2, 0, 0, 4, 8, appWidgetId = 3, provider = "x/.W"),
            StackCell("st", 3, 0, 0, 4, 8, listOf(StackMember("m", type = "clock"))),
        )
        for (item in items) {
            val renamed = item.withId("new")
            assertEquals("new", renamed.id)
            assertEquals(item.toJson().put("id", "new").toString(), renamed.toJson().toString())
        }
    }
}
