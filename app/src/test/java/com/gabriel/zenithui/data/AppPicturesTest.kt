package com.gabriel.zenithui.data

import com.gabriel.zenithui.data.PictureRules.Front
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPicturesTest {

    private val dropped = mutableListOf<String>()
    private val shelf = PictureShelf<String> { dropped += it }

    private fun shot(pkg: String, name: String, at: Long) =
        shelf.put(PictureShelf.Picture(pkg, name, at, 576, 1280))

    @Test
    fun `a picture is only kept of the app in front`() {
        shelf.cameToFront("mail", at = 0L)
        assertTrue(shot("mail", "m1", 1_000L))
        assertFalse(shot("maps", "x", 1_000L))
        assertEquals(listOf("x"), dropped)
        shelf.cameToFront(null, at = 5_000L)
        assertFalse(shot("mail", "late", 6_000L))
    }

    @Test
    fun `a picture counts once the app has stayed in front for a while after it`() {
        shelf.cameToFront("mail", at = 0L)
        shot("mail", "m1", 1_000L)
        assertNull(shelf.forClosing("mail", since = 0L, now = 2_500L))
        assertEquals("m1", shelf.forClosing("mail", since = 0L, now = 3_000L)?.image)
        // The newest is not yet sure, so the one before it is used.
        shot("mail", "m2", 4_000L)
        assertEquals("m1", shelf.forClosing("mail", since = 0L, now = 5_000L)?.image)
        assertEquals("m2", shelf.forClosing("mail", since = 0L, now = 6_000L)?.image)
    }

    @Test
    fun `a picture taken just before the app went is dropped, since it may show it going`() {
        shelf.cameToFront("mail", at = 0L)
        shot("mail", "m1", 1_000L)
        shot("mail", "m2", 4_000L)
        // Home, 1.5 s after m2: a swipe home may already have been moving the app when it was taken.
        shelf.cameToFront(null, at = 5_500L)
        assertEquals(listOf("m2"), dropped)
        assertEquals("m1", shelf.forClosing("mail", since = 0L, now = 5_600L)?.image)
        // Long enough after: it stays.
        shelf.cameToFront("maps", at = 6_000L)
        shot("maps", "p1", 7_000L)
        shelf.cameToFront(null, at = 9_500L)
        assertEquals("p1", shelf.forClosing("maps", since = 0L, now = 9_600L)?.image)
    }

    @Test
    fun `closing only uses a picture from this visit`() {
        shelf.cameToFront("mail", at = 0L)
        shot("mail", "m1", 1_000L)
        shelf.cameToFront(null, at = 4_000L)
        // Opened again at 10 s, and closed before a new picture could be sure.
        shelf.cameToFront("mail", at = 10_000L)
        shot("mail", "m2", 10_700L)
        assertNull(shelf.forClosing("mail", since = 10_000L, now = 11_000L))
        // Opening, though, could have used the last one.
        assertEquals("m1", shelf.forOpening("mail", now = 11_000L)?.image)
    }

    @Test
    fun `opening only uses a picture from the last five minutes`() {
        shelf.cameToFront("mail", at = 0L)
        shot("mail", "m1", 1_000L)
        shelf.cameToFront(null, at = 4_000L)
        assertEquals("m1", shelf.forOpening("mail", now = 1_000L + PictureRules.OPEN_FRESH_MS)?.image)
        assertNull(shelf.forOpening("mail", now = 1_001L + PictureRules.OPEN_FRESH_MS))
        assertNull(shelf.forOpening("maps", now = 2_000L))
    }

    @Test
    fun `at most two pictures of the app in front, one of any other, and four apps`() {
        shelf.cameToFront("a", at = 0L)
        shot("a", "a1", 1_000L)
        shot("a", "a2", 4_000L)
        shot("a", "a3", 7_000L)
        assertEquals(listOf("a1"), dropped)
        shelf.cameToFront("b", at = 10_000L)
        assertEquals(listOf("a1", "a2"), dropped)
        assertEquals("a3", shelf.forOpening("a", now = 10_000L)?.image)
        shot("b", "b1", 11_000L)
        shelf.cameToFront("c", at = 14_000L)
        shot("c", "c1", 15_000L)
        shelf.cameToFront("d", at = 18_000L)
        shot("d", "d1", 19_000L)
        shelf.cameToFront("e", at = 22_000L)
        shot("e", "e1", 23_000L)
        // Five apps: the one seen longest ago goes.
        assertTrue("a3" in dropped)
        assertNull(shelf.forOpening("a", now = 24_000L))
        listOf("b", "c", "d").forEach { assertEquals("${it}1", shelf.forOpening(it, now = 24_000L)?.image) }
    }

    @Test
    fun `old pictures of apps in the back are let go`() {
        shelf.cameToFront("a", at = 0L)
        shot("a", "a1", 1_000L)
        shelf.cameToFront("b", at = 4_000L)
        shot("b", "b1", 2_000L + PictureRules.OPEN_FRESH_MS)
        assertTrue("a1" in dropped)
    }

    @Test
    fun `something of the system's over the app hides it for a moment, without it having gone`() {
        shelf.cameToFront("mail", at = 0L)
        shot("mail", "m1", 1_000L)
        shot("mail", "m2", 4_000L)
        // The shade comes down at 5 s: m2 may show it on its way.
        shelf.covered(at = 5_000L)
        assertEquals(listOf("m2"), dropped)
        assertEquals("mail", shelf.front)
        assertEquals("mail", shelf.closingApp(now = 9_000L))
        assertEquals("m1", shelf.forClosing("mail", since = 0L, now = 9_000L)?.image)
        // Nothing in front: nothing to hide.
        shelf.cameToFront(null, at = 10_000L)
        shelf.covered(at = 11_000L)
        assertEquals(listOf("m2"), dropped)
    }

    @Test
    fun `the app being closed is the one in front, or the one that has only just gone`() {
        assertNull(shelf.closingApp(now = 0L))
        shelf.cameToFront("mail", at = 0L)
        assertEquals("mail", shelf.closingApp(now = 500L))
        shelf.cameToFront(null, at = 1_000L)
        assertEquals("mail", shelf.closingApp(now = 1_000L + PictureRules.LEFT_FRESH_MS))
        assertNull(shelf.closingApp(now = 1_001L + PictureRules.LEFT_FRESH_MS))
    }

    @Test
    fun `clearing lets go of every picture`() {
        shelf.cameToFront("a", at = 0L)
        shot("a", "a1", 1_000L)
        shot("a", "a2", 4_000L)
        shelf.clear()
        assertEquals(listOf("a1", "a2"), dropped.sorted())
        assertNull(shelf.forOpening("a", now = 9_000L))
        assertNull(shelf.closingApp(now = 9_000L))
    }

    @Test
    fun `a visit starts when the app comes to the front, and is kept for closing it once it has gone`() {
        assertNull(shelf.visitStart("mail"))
        shelf.cameToFront("mail", at = 1_000L)
        assertEquals(1_000L, shelf.visitStart("mail"))
        // Another screen of the same app is the same visit, and so is the shade over it.
        shelf.cameToFront("mail", at = 3_000L)
        shelf.covered(at = 4_000L)
        assertEquals(1_000L, shelf.visitStart("mail"))
        // Gone home: still the visit that has just ended, which is the one being closed.
        shelf.cameToFront(null, at = 6_000L)
        assertEquals(1_000L, shelf.visitStart("mail"))
        assertNull(shelf.visitStart("maps"))
        // Opened again: a new visit.
        shelf.cameToFront("mail", at = 9_000L)
        assertEquals(9_000L, shelf.visitStart("mail"))
        // Another app over it: mail's is the visit just ended, maps' the one under way.
        shelf.cameToFront("maps", at = 12_000L)
        assertEquals(9_000L, shelf.visitStart("mail"))
        assertEquals(12_000L, shelf.visitStart("maps"))
        shelf.clear()
        assertNull(shelf.visitStart("mail"))
        assertNull(shelf.visitStart("maps"))
    }

    @Test
    fun `with the screen off the pictures go, but which app is in front is kept`() {
        shelf.cameToFront("mail", at = 0L)
        shot("mail", "m1", 1_000L)
        shelf.dropPictures()
        assertEquals(listOf("m1"), dropped)
        assertEquals("mail", shelf.front)
        assertEquals(0L, shelf.visitStart("mail"))
        assertNull(shelf.forOpening("mail", now = 4_000L))
        // Unlocked, the next picture of it is kept as before.
        assertTrue(shot("mail", "m2", 30_000L))
        assertEquals("m2", shelf.forClosing("mail", since = 0L, now = 32_000L)?.image)
    }

    @Test
    fun `what came to the front`() {
        fun kind(pkg: String?) = PictureRules.kind(
            pkg,
            own = "zenith",
            homes = setOf("zenith", "launcher.recents"),
            keyboards = setOf("keyboard"),
            launchable = { it == "mail" || it == "keyboard" || it == "zenith" },
        )
        assertEquals(Front.App, kind("mail"))
        // Zenith says for itself when the home screen is on show: its other windows change nothing.
        assertEquals(Front.Ignore, kind("zenith"))
        assertEquals(Front.Home, kind("launcher.recents"))
        // A keyboard comes and goes over the app, which is still the one in front.
        assertEquals(Front.Ignore, kind("keyboard"))
        assertEquals(Front.Other, kind("com.android.systemui"))
        assertEquals(Front.Ignore, kind(null))
        assertEquals(Front.Ignore, kind(""))
    }

    @Test
    fun `a picture is shrunk to at most 1280 pixels along its longer side`() {
        assertEquals(576 to 1280, PictureRules.shrunk(1080, 2400))
        assertEquals(1280 to 576, PictureRules.shrunk(2400, 1080))
        assertEquals(720 to 1280, PictureRules.shrunk(1440, 2560))
        assertEquals(800 to 1200, PictureRules.shrunk(800, 1200))
    }

    @Test
    fun `a picture taken sideways is not used upright`() {
        assertTrue(PictureRules.fits(576, 1280, 1080f, 2400f))
        assertFalse(PictureRules.fits(1280, 576, 1080f, 2400f))
        // A different shape altogether: a picture from the other screen of a fold.
        assertFalse(PictureRules.fits(1000, 1100, 1080f, 2400f))
        assertFalse(PictureRules.fits(0, 0, 1080f, 2400f))
    }

    @Test
    fun `an app hidden from screenshots comes out black, and its picture is not kept`() {
        val width = 100
        val height = 200
        val black = IntArray(width * height) { 0xFF000000.toInt() }
        assertTrue(PictureRules.blank(black, width, height))
        // The status bar's clock and icons over the black change nothing.
        val withBars = black.copyOf().also { pixels ->
            for (y in 0 until 10) for (x in 0 until width) pixels[y * width + x] = 0xFFFFFFFF.toInt()
            for (y in 190 until 200) for (x in 0 until width) pixels[y * width + x] = 0xFFFFFFFF.toInt()
        }
        assertTrue(PictureRules.blank(withBars, width, height))
        // A dark app with something on it is kept.
        val darkApp = black.copyOf().also { pixels ->
            for (y in 40 until 60) for (x in 0 until width) pixels[y * width + x] = 0xFFEEEEEE.toInt()
        }
        assertFalse(PictureRules.blank(darkApp, width, height))
        assertFalse(PictureRules.blank(IntArray(width * height) { 0xFF2A2A2A.toInt() }, width, height))
        assertFalse(PictureRules.blank(IntArray(0), 0, 0))
    }

    @Test
    fun `the helper is offered only where this copy of Zenith has it`() {
        assertTrue(PictureRules.offered(sdk = 30, declared = true))
        assertTrue(PictureRules.offered(sdk = 36, declared = true))
        // The copy that installs by tapping it goes without: Play Protect refuses it otherwise.
        assertFalse(PictureRules.offered(sdk = 36, declared = false))
        // Android 10 has no screenshots for accessibility services.
        assertFalse(PictureRules.offered(sdk = 29, declared = true))
    }
}
