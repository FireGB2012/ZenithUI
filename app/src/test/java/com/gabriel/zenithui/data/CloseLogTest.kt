package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloseLogTest {

    private fun entry(
        route: CloseRoute = CloseRoute.Card,
        where: CloseLog.Where = CloseLog.Where.Icon,
        source: CloseSource? = CloseSource.Opened,
        waited: Boolean = false,
        missed: Boolean = false,
        over: Boolean = false,
        label: String? = "Mail",
    ) = CloseLog.Entry(label, source, route, where, waited, missed, over)

    @Test
    fun `where an app went`() {
        assertEquals(CloseLog.Where.Icon, CloseLog.where(spot = true, folder = false, away = null))
        assertEquals(CloseLog.Where.Folder, CloseLog.where(spot = true, folder = true, away = null))
        // An icon to go into comes first.
        assertEquals(CloseLog.Where.Icon, CloseLog.where(spot = true, folder = false, away = CloseAway.Way.Down))
        assertEquals(CloseLog.Where.Drawer, CloseLog.where(spot = false, folder = false, away = CloseAway.Way.Down))
        assertEquals(CloseLog.Where.Library, CloseLog.where(spot = false, folder = false, away = CloseAway.Way.Library))
        assertEquals(CloseLog.Where.PageBefore, CloseLog.where(spot = false, folder = false, away = CloseAway.Way.PageBefore))
        assertEquals(CloseLog.Where.PageAfter, CloseLog.where(spot = false, folder = false, away = CloseAway.Way.PageAfter))
        assertEquals(CloseLog.Where.Middle, CloseLog.where(spot = false, folder = false, away = null))
    }

    @Test
    fun `before any app has closed there is nothing to say`() {
        assertEquals("", CloseLog.describe(null))
    }

    @Test
    fun `the last close said in a sentence`() {
        assertEquals(
            "The last close: Mail, its card went into its icon, after Android's own close. Android said which app it was.",
            CloseLog.describe(entry(source = CloseSource.Named, waited = true)),
        )
        assertEquals(
            "The last close: Mail, Android took its own window into its icon. Android said which app it was.",
            CloseLog.describe(entry(route = CloseRoute.Handshake, source = CloseSource.Answered)),
        )
        assertEquals(
            "The last close: Mail, a picture of it went down into the app drawer. The app picture helper saw it go.",
            CloseLog.describe(entry(route = CloseRoute.Picture, where = CloseLog.Where.Drawer, source = CloseSource.Seen)),
        )
        assertEquals(
            "The last close: the app, its card went into the middle of the screen. Zenith opened it.",
            CloseLog.describe(entry(where = CloseLog.Where.Middle, source = CloseSource.Remembered, label = null)),
        )
        assertEquals(
            "The last close: Mail, its card went back into its icon. It was opened from its icon.",
            CloseLog.describe(entry(over = true)),
        )
    }

    @Test
    fun `a window Android was told about and never moved is said to have missed`() {
        val missed = CloseLog.describe(entry(route = CloseRoute.Handshake, source = CloseSource.Answered, missed = true))
        assertTrue(missed, missed.contains("never said its window got there, so the icon landed"))
    }

    @Test
    fun `an app Zenith knew nothing of is said as that`() {
        val unknown = CloseLog.describe(entry(route = CloseRoute.None, where = CloseLog.Where.Nowhere, source = null, label = null))
        assertTrue(unknown, unknown.startsWith("The last app to close was not one Zenith opened"))
    }

    @Test
    fun `what is said is the latest close, marked missed if Android never moved its window`() {
        CloseLog.note(entry(route = CloseRoute.Handshake, source = CloseSource.Answered))
        CloseLog.missed()
        assertEquals(true, CloseLog.last.value?.missed)
        CloseLog.note(entry())
        assertEquals(false, CloseLog.last.value?.missed)
    }
}
