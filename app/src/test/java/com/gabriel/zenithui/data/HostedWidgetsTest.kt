package com.gabriel.zenithui.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HostedWidgetsTest {

    private fun roundTrip(item: HomeItem): HomeItem? = homeItemFromJson(JSONObject(item.toJson().toString()))

    @Test
    fun `another app's widget survives being written and read back`() {
        val cell = HostedWidgetCell("h1", page = 1, col = 0, row = 4, spanX = 4, spanY = 8, appWidgetId = 37, provider = "com.spotify/.Widget", glass = true)
        assertEquals(cell, roundTrip(cell))
        // Without glass, the default, nothing is written for it.
        val plain = cell.copy(glass = false)
        assertEquals(plain, roundTrip(plain))
        // One with nothing to say what it is cannot be put back, and is not read.
        assertNull(homeItemFromJson(JSONObject("""{"t":"hosted","id":"h","p":0,"c":0,"r":0,"wid":3}""")))
    }

    @Test
    fun `a stack survives being written and read back`() {
        val stack = StackCell(
            "s1", page = 0, col = 0, row = 0, spanX = 2, spanY = 8,
            members = listOf(StackMember("a", type = "clock"), StackMember("b", appWidgetId = 12, provider = "com.x/.W")),
        )
        assertEquals(stack, roundTrip(stack))
    }

    @Test
    fun `a widget's own colour is kept, and one without is written as before`() {
        val coloured = WidgetCell("w", 0, 0, 0, "clock", 4, 8, accent = 0xFFFF6B8B)
        assertEquals(coloured, roundTrip(coloured))
        assertEquals(0xFFFF6B8B, (roundTrip(coloured.moved(1, 0, 0)) as WidgetCell).accent)
        assertFalse(WidgetCell("p", 0, 0, 0, "clock", 4, 8).toJson().has("acc"))
    }

    @Test
    fun `a folder's own colour is kept, and one without is written as before`() {
        val coloured = FolderCell("f", 0, 1, 0, "Fun", listOf("a/b", "c/d"), colour = 0xFF4FB4FF)
        assertEquals(coloured, roundTrip(coloured))
        assertFalse(FolderCell("g", 0, 1, 0, "Fun", listOf("a/b")).toJson().has("col"))
    }

    @Test
    fun `a stack read back with one widget left is that widget, where the stack was`() {
        val json = """{"t":"stack","id":"s","p":2,"c":1,"r":4,"sx":2,"sy":8,"members":[{"id":"w","type":"battery"}]}"""
        assertEquals(WidgetCell("w", 2, 1, 4, "battery", 2, 8), homeItemFromJson(JSONObject(json)))
        assertNull(homeItemFromJson(JSONObject("""{"t":"stack","id":"s","p":0,"c":0,"r":0,"members":[]}""")))
    }

    @Test
    fun `a widget stacked on a widget makes a stack in its place and at its size`() {
        val clock = WidgetCell("c", page = 1, col = 2, row = 4, type = "clock", spanX = 2, spanY = 8)
        val stack = StackRules.stack(clock, StackMember("n", type = "notes"), newId = "s")!!
        assertEquals("s", stack.id)
        assertEquals(1 to 2, stack.page to stack.col)
        assertEquals(4, stack.row)
        assertEquals(2 to 8, stack.spanX to stack.spanY)
        // The widget it was made on keeps its id inside, so what was set for it stays with it.
        assertEquals(listOf("c", "n"), stack.members.map { it.id })
        // Another one goes on the end.
        val more = StackRules.stack(stack, StackMember("t", type = "toggles"), newId = "unused")!!
        assertEquals(listOf("c", "n", "t"), more.members.map { it.id })
        assertEquals("s", more.id)
    }

    @Test
    fun `only widgets stack, and a stack holds so many`() {
        val app = AppCell("a", 0, 0, 0, "p", "c")
        assertNull(StackRules.stack(app, StackMember("n", type = "notes"), "s"))
        var stack: HomeItem = WidgetCell("w0", 0, 0, 0, "clock", 2, 8)
        for (i in 1 until StackRules.MOST) stack = StackRules.stack(stack, StackMember("w$i", type = "notes"), "s")!!
        assertEquals(StackRules.MOST, (stack as StackCell).members.size)
        assertNull(StackRules.stack(stack, StackMember("one-too-many", type = "notes"), "s"))
    }

    @Test
    fun `taking widgets out of a stack leaves a stack, then a widget`() {
        val stack = StackCell(
            "s", 0, 1, 4, 2, 8,
            members = listOf(StackMember("a", type = "clock"), StackMember("b", type = "notes"), StackMember("c", appWidgetId = 5, provider = "x/.W")),
        )
        val two = StackRules.without(stack, 0) as StackCell
        assertEquals(listOf("b", "c"), two.members.map { it.id })
        // With one left it stands on its own, in the stack's place and at its size.
        val alone = StackRules.without(two, 0)
        assertEquals(HostedWidgetCell("c", 0, 1, 4, 2, 8, appWidgetId = 5, provider = "x/.W"), alone)
    }

    @Test
    fun `a stack turned to one member keeps the rest in order behind it`() {
        val stack = StackCell("s", 0, 0, 0, 2, 8, listOf(StackMember("a", type = "1"), StackMember("b", type = "2"), StackMember("c", type = "3")))
        assertEquals(listOf("c", "a", "b"), StackRules.bringToTop(stack, 2).members.map { it.id })
        assertTrue(StackRules.bringToTop(stack, 0) === stack)
    }

    @Test
    fun `a widget asks for the cells its own size needs, never more than the page`() {
        // Android 12's own cells win where the widget gives them.
        assertEquals(4 to 2 * ROW_UNITS, HostedSizes.spanFor(250, 110, targetX = 4, targetY = 2, cellDp = 80f, rowDp = 100f, iconDp = 52f, columns = 4, rows = 6))
        // Otherwise its size in dp, against the room n cells give it: n - 1 cells and one icon (52 dp
        // in cells 80 dp wide), across and down alike. 84 by 104 is more than an icon either way.
        assertEquals(2 to 2 * ROW_UNITS, HostedSizes.spanFor(84, 104, 0, 0, cellDp = 80f, rowDp = 100f, iconDp = 52f, columns = 4, rows = 6))
        assertEquals(3 to 3 * ROW_UNITS, HostedSizes.spanFor(220, 180, 0, 0, cellDp = 80f, rowDp = 100f, iconDp = 52f, columns = 4, rows = 6))
        // Bigger than the page is the whole page.
        assertEquals(4 to 6 * ROW_UNITS, HostedSizes.spanFor(900, 2000, 0, 0, cellDp = 80f, rowDp = 100f, iconDp = 52f, columns = 4, rows = 6))
        assertEquals(3 to ROW_UNITS, HostedSizes.minSpan(150, 40, cellDp = 80f, rowDp = 100f, iconDp = 52f, columns = 4, rows = 6))
    }
}
