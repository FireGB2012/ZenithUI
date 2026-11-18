package com.gabriel.zenithui.data

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloseAwayTest {

    private val mail = "app.mail/app.mail.Main"
    private val notes = "app.notes/app.notes.Main"

    private fun icon(key: String, page: Int, col: Int = 0) = AppCell(
        id = "$key@$page:$col",
        page = page,
        col = col,
        row = 0,
        packageName = key.substringBefore('/'),
        className = key.substringAfter('/'),
    )

    private fun folder(page: Int, vararg keys: String) =
        FolderCell(id = "folder@$page", page = page, col = 3, row = 0, title = "Folder", keys = keys.toList())

    private fun way(
        items: List<HomeItem>,
        shown: Int = 0,
        library: Boolean = false,
        dock: List<String> = emptyList(),
        key: String = mail,
        pkg: String = CloseRules.packageOf(key),
        work: Boolean = CloseRules.isWork(key),
    ) = CloseAway.way(key, pkg, work, items, dock, shown, library)

    @Test
    fun `an app that is only in the drawer goes down into it, the way the drawer comes up`() {
        assertEquals(CloseAway.Way.Down, way(listOf(icon(notes, 0))))
        assertEquals(CloseAway.Way.Down, way(emptyList()))
        assertFalse(CloseAway.Way.Down.towardsPage)
    }

    @Test
    fun `an app that is only in the App Library goes off to the side, where the library comes in from`() {
        assertEquals(CloseAway.Way.Library, way(listOf(icon(notes, 0)), library = true))
        assertFalse(CloseAway.Way.Library.towardsPage)
    }

    @Test
    fun `an app on another page goes off towards that page`() {
        assertEquals(CloseAway.Way.PageAfter, way(listOf(icon(mail, 2)), shown = 0))
        assertEquals(CloseAway.Way.PageBefore, way(listOf(icon(mail, 0)), shown = 2))
        // Its page is where it lives, before the library past the last page.
        assertEquals(CloseAway.Way.PageAfter, way(listOf(icon(mail, 1)), library = true))
        assertEquals(CloseAway.Way.PageBefore, way(listOf(icon(mail, 0)), shown = 1, library = true))
        assertTrue(CloseAway.Way.PageAfter.towardsPage)
        assertTrue(CloseAway.Way.PageBefore.towardsPage)
    }

    @Test
    fun `on two pages, towards the nearer, and the earlier of two as near`() {
        assertEquals(CloseAway.Way.PageAfter, way(listOf(icon(mail, 0), icon(mail, 3)), shown = 2))
        assertEquals(CloseAway.Way.PageBefore, way(listOf(icon(mail, 1), icon(mail, 3)), shown = 2))
    }

    @Test
    fun `a folder holding it counts, after an icon of its own`() {
        assertEquals(CloseAway.Way.PageAfter, way(listOf(folder(1, notes, mail)), shown = 0))
        assertEquals(CloseAway.Way.PageBefore, way(listOf(folder(1, notes, mail), icon(notes, 0)), shown = 2))
        // An icon of its own on page 3 wins over a folder holding it on page 1, as going into an icon does.
        assertEquals(CloseAway.Way.PageAfter, way(listOf(folder(1, mail), icon(mail, 3)), shown = 2))
    }

    @Test
    fun `another of the same app's screens counts as the app, a shortcut only by its own key`() {
        val settingsMain = "app.mail/app.mail.Settings"
        assertEquals(CloseAway.Way.PageAfter, way(listOf(icon(settingsMain, 1)), shown = 0))
        val shortcut = ShortcutKeys.of("app.mail", "compose")
        val shortcutCell = ShortcutCell(id = "s", page = 2, col = 0, row = 0, packageName = "app.mail", shortcutId = "compose", label = "Compose")
        // A shortcut on a page is not the app.
        assertEquals(CloseAway.Way.Down, way(listOf(shortcutCell), shown = 0))
        // The shortcut closing goes towards its own icon, and failing that the app's.
        assertEquals(CloseAway.Way.PageAfter, way(listOf(shortcutCell), shown = 0, key = shortcut))
        assertEquals(CloseAway.Way.PageBefore, way(listOf(icon(mail, 0)), shown = 1, key = shortcut))
    }

    @Test
    fun `a work app is not the personal one, nor the other way round`() {
        val work = WorkKeys.of(mail)
        assertEquals(CloseAway.Way.Down, way(listOf(icon(mail, 1)), key = work))
        assertEquals(CloseAway.Way.PageAfter, way(listOf(folder(1, work)), key = work))
        assertEquals(CloseAway.Way.Down, way(listOf(folder(1, work)), key = mail))
    }

    @Test
    fun `on the page on show with no place known for its icon, it goes into the middle as before`() {
        assertNull(way(listOf(icon(mail, 0)), shown = 0))
        assertNull(way(listOf(folder(2, mail)), shown = 2, library = true))
    }

    @Test
    fun `in a dock that is not on show, it goes down to where the dock would be`() {
        assertEquals(CloseAway.Way.Down, way(emptyList(), dock = listOf(notes, mail)))
        assertEquals(CloseAway.Way.Down, way(emptyList(), dock = listOf(mail), library = true))
        // A page of its own is nearer than that.
        assertEquals(CloseAway.Way.PageAfter, way(listOf(icon(mail, 1)), dock = listOf(mail), library = true))
    }

    private val width = 1080f
    private val height = 2340f
    private val side = 180f

    @Test
    fun `down, it goes off the foot of the screen, in the middle of it`() {
        val to = CloseAway.target(CloseAway.Way.Down, width, height, side)
        assertEquals(Rect(450f, height, 630f, height + side), to)
    }

    @Test
    fun `to the side, it goes off the edge half way down, right for the library and a later page, left for an earlier one`() {
        val middle = height / 2f
        val right = Rect(width, middle - side / 2f, width + side, middle + side / 2f)
        assertEquals(right, CloseAway.target(CloseAway.Way.Library, width, height, side))
        assertEquals(right, CloseAway.target(CloseAway.Way.PageAfter, width, height, side))
        assertEquals(Rect(-side, middle - side / 2f, 0f, middle + side / 2f), CloseAway.target(CloseAway.Way.PageBefore, width, height, side))
    }

    private fun frameAt(to: Rect, p: Float) = LaunchMotion.frame(
        to.left, to.top, to.right, to.bottom, width, height,
        x = p, y = p, size = p,
        arc = 0f, startRadius = 0f, endRadius = 0f,
    )

    @Test
    fun `all the way, the card keeps to the edge it goes through, and is gone as it gets there`() {
        val down = CloseAway.target(CloseAway.Way.Down, width, height, side)
        val right = CloseAway.target(CloseAway.Way.Library, width, height, side)
        val left = CloseAway.target(CloseAway.Way.PageBefore, width, height, side)
        for (step in 0..100) {
            val p = step / 100f
            // Down: its top comes down the screen as it shrinks, and its foot never comes up into it.
            val d = frameAt(down, p)
            assertEquals(height * (1f - p), d.top, 0.5f)
            assertTrue("foot at ${d.bottom} for $p", d.bottom >= height - 0.01f)
            assertEquals(width / 2f, d.centreX, 0.01f)
            // To the right: its left side goes across, its right one stays off the screen.
            val r = frameAt(right, p)
            assertEquals(width * (1f - p), r.left, 0.5f)
            assertTrue("right side at ${r.right} for $p", r.right >= width - 0.01f)
            assertEquals(height / 2f, r.centreY, 0.01f)
            // And to the left, the same the other way.
            val l = frameAt(left, p)
            assertEquals(width * p, l.right, 0.5f)
            assertTrue("left side at ${l.left} for $p", l.left <= 0.01f)
        }
        assertTrue(frameAt(down, 0f).top >= height)
        assertTrue(frameAt(right, 0f).left >= width)
        assertTrue(frameAt(left, 0f).right <= 0f)
    }

    @Test
    fun `coming home stays on the page on show when the app goes into an icon on it or off towards its page`() {
        assertTrue(CloseAway.staysOnPage(spot = true, way = null, route = CloseRoute.Card))
        assertTrue(CloseAway.staysOnPage(spot = true, way = null, route = CloseRoute.Handshake))
        assertTrue(CloseAway.staysOnPage(spot = false, way = CloseAway.Way.PageAfter, route = CloseRoute.Card))
        assertTrue(CloseAway.staysOnPage(spot = false, way = CloseAway.Way.PageBefore, route = CloseRoute.Picture))
        // Into the drawer or the library, or into the middle: back to the first page, as before.
        assertFalse(CloseAway.staysOnPage(spot = false, way = CloseAway.Way.Down, route = CloseRoute.Card))
        assertFalse(CloseAway.staysOnPage(spot = false, way = CloseAway.Way.Library, route = CloseRoute.Card))
        assertFalse(CloseAway.staysOnPage(spot = false, way = null, route = CloseRoute.Card))
        // Nothing of Zenith's to see: back to the first page, as before.
        assertFalse(CloseAway.staysOnPage(spot = true, way = null, route = CloseRoute.None))
        assertFalse(CloseAway.staysOnPage(spot = false, way = CloseAway.Way.PageAfter, route = CloseRoute.None))
    }
}
