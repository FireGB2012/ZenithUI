package com.gabriel.zenithui.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcutCellTest {

    @Test
    fun `a shortcut on the page survives being written and read back`() {
        val cell = ShortcutCell("s1", page = 1, col = 2, row = 8, packageName = "com.google.android.apps.maps", shortcutId = "nav/home", label = "Home")
        assertEquals(cell, homeItemFromJson(JSONObject(cell.toJson().toString())))
        // Placed freely, its offset comes back too.
        val nudged = cell.nudged(0.2f, -0.3f)
        assertEquals(nudged, homeItemFromJson(JSONObject(nudged.toJson().toString())))
    }

    @Test
    fun `a shortcut with nothing to say which it is is not read`() {
        assertNull(homeItemFromJson(JSONObject("""{"t":"shortcut","id":"s","p":0,"c":0,"r":0,"pkg":"","sid":"x"}""")))
        assertNull(homeItemFromJson(JSONObject("""{"t":"shortcut","id":"s","p":0,"c":0,"r":0,"pkg":"com.a"}""")))
    }

    @Test
    fun `a shortcut is always one cell`() {
        val cell = ShortcutCell("s1", 0, 0, 0, "com.a", "x", "X")
        assertEquals(1, cell.spanX)
        assertEquals(ROW_UNITS, cell.spanY)
        // Moved, it drops any free-placement offset, as every other item does.
        val moved = cell.nudged(0.4f, 0.4f).moved(2, 3, 4)
        assertEquals(0f, moved.dx)
        assertEquals(0f, moved.dy)
        assertEquals(2, moved.page)
    }

    @Test
    fun `a shortcut's key is never an app's`() {
        val key = ShortcutKeys.of("com.whatsapp", "chat/42")
        assertTrue(ShortcutKeys.isShortcut(key))
        assertFalse(ShortcutKeys.isShortcut("com.whatsapp/com.whatsapp.Main"))
        // An id may hold a slash; a package name never does.
        assertEquals("com.whatsapp" to "chat/42", ShortcutKeys.parse(key))
        assertNull(ShortcutKeys.parse("shortcut:com.whatsapp"))
        assertNull(ShortcutKeys.parse("com.whatsapp/com.whatsapp.Main"))
        assertEquals(key, AppShortcut("com.whatsapp", "chat/42", "Mum").key)
    }
}
