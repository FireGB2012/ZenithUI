package com.gabriel.zenithui.data

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloseMemoryTest {

    private val mailIcon = Rect(656f, 980f, 865f, 1189f)

    private fun launch(
        key: String = "com.example.mail/.Main",
        from: Rect? = mailIcon,
        spot: Rect? = mailIcon,
        dock: Boolean = false,
        page: Int = 1,
        at: Long = 123_456L,
        boot: Long = 1_763_600_000_000L,
    ) = CloseMemory.Launch(key, from, spot, dock, page, at, boot)

    @Test
    fun `what Zenith opened is kept on the phone as it was`() {
        val opened = launch()
        assertEquals(opened, CloseMemory.decode(CloseMemory.encode(opened)))
        // Opened with no icon to grow out of (Search, a gesture, a shortcut).
        val fromNowhere = launch(from = null, spot = null, page = 0)
        assertEquals(fromNowhere, CloseMemory.decode(CloseMemory.encode(fromNowhere)))
        // From the dock, in the work profile.
        val work = launch(key = "work:com.example.mail/.Main", dock = true, page = 3)
        assertEquals(work, CloseMemory.decode(CloseMemory.encode(work)))
    }

    @Test
    fun `nothing kept, or anything else, is nothing`() {
        assertNull(CloseMemory.decode(null))
        assertNull(CloseMemory.decode(""))
        assertNull(CloseMemory.decode("   "))
        assertNull(CloseMemory.decode("not json"))
        assertNull(CloseMemory.decode("{}"))
        assertNull(CloseMemory.decode("""{"key":"","at":1,"boot":2}"""))
        assertNull(CloseMemory.decode("""{"key":"com.example.mail/.Main"}"""))
    }

    @Test
    fun `a place that is not a place is dropped, and the app still kept`() {
        val kept = CloseMemory.decode("""{"key":"com.example.mail/.Main","from":"1,2,3","spot":"5,5,5,9","at":1,"boot":2}""")
        assertEquals("com.example.mail/.Main", kept?.key)
        assertNull(kept?.from)
        assertNull(kept?.spot)
        assertFalse(kept!!.dock)
        assertEquals(0, kept.page)
    }

    @Test
    fun `what was kept before the phone was started again is not used`() {
        val boot = 1_763_600_000_000L
        assertTrue(CloseMemory.sameBoot(boot, boot))
        // The two clocks it is worked out from drift a little apart.
        assertTrue(CloseMemory.sameBoot(boot, boot + 3_000L))
        assertTrue(CloseMemory.sameBoot(boot, boot - CloseMemory.BOOT_SLACK_MS))
        assertFalse(CloseMemory.sameBoot(boot, boot + 60_000L))
        assertEquals(launch(boot = boot), CloseMemory.usable(launch(boot = boot), boot + 2_000L))
        assertNull(CloseMemory.usable(launch(boot = boot), boot + 3_600_000L))
        assertNull(CloseMemory.usable(null, boot))
    }

    @Test
    fun `a Zenith started afresh closes the app into the icon it was opened from, if that is on show`() {
        // On the page it was opened from.
        assertEquals(mailIcon, launch(page = 1).restSpot(shownPage = 1))
        // Another page is on show now: not there.
        assertNull(launch(page = 1).restSpot(shownPage = 2))
        // The dock is on show whatever the page.
        assertEquals(mailIcon, launch(page = 1, dock = true).restSpot(shownPage = 4))
        // Opened from no icon on the home screen (the drawer, Search): none.
        assertNull(launch(spot = null).restSpot(shownPage = 1))
    }
}
