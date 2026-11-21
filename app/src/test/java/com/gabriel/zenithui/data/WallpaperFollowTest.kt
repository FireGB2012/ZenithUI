package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperFollowTest {

    private val night = WallColors(accent = 1, accentAlt = 2, deep = 3, mid = 4, base = 5, dark = true)

    private fun key(style: String = "Waves", colors: WallColors = night, width: Int = 1080, height: Int = 2400) =
        WallpaperFollow.key(style, colors, width, height)

    @Test
    fun `the same picture has the same key`() {
        assertEquals(key(), key(colors = night.copy()))
    }

    @Test
    fun `the key changes with the design, any colour, light or dark, and the size, and with nothing else`() {
        val other = listOf(
            key(style = "Orbit"),
            key(colors = night.copy(accent = 9)),
            key(colors = night.copy(accentAlt = 9)),
            key(colors = night.copy(deep = 9)),
            key(colors = night.copy(mid = 9)),
            key(colors = night.copy(base = 9)),
            key(colors = night.copy(dark = false)),
            key(width = 720),
            key(height = 1600),
        )
        other.forEach { assertNotEquals(key(), it) }
        assertEquals(other.size, other.toSet().size)
    }

    private val following = ZenithSettings(wallpaperStyle = "Waves", wallpaperFollow = true)

    @Test
    fun `a wallpaper of Zenith's own is repainted when the picture wanted is not the one up`() {
        assertTrue(WallpaperFollow.shouldRepaint(following, desired = "new", applied = "old", ownWallpaperUp = true))
        assertTrue(WallpaperFollow.shouldRepaint(following, desired = "new", applied = null, ownWallpaperUp = true))
    }

    @Test
    fun `nothing is repainted when the picture wanted is already up`() {
        assertFalse(WallpaperFollow.shouldRepaint(following, desired = "same", applied = "same", ownWallpaperUp = true))
    }

    @Test
    fun `nothing is repainted when following is switched off`() {
        assertFalse(WallpaperFollow.shouldRepaint(following.copy(wallpaperFollow = false), "new", "old", ownWallpaperUp = true))
    }

    @Test
    fun `a photo or a wallpaper from another app is never replaced`() {
        // No design of Zenith's is in use.
        assertFalse(WallpaperFollow.shouldRepaint(following.copy(wallpaperStyle = ""), "new", "old", ownWallpaperUp = true))
        // One was, but something else is on screen now.
        assertFalse(WallpaperFollow.shouldRepaint(following, "new", "old", ownWallpaperUp = false))
    }

    // ---- Zenith's live wallpaper is Zenith's wallpaper, whatever design it shows ----
    // 2026-11-21 ("it keeps picking the bubble wallpaper"): putting a still design on while a
    // moving one was up, the live wallpaper's own new colours were taken for another app's
    // wallpaper. The design was forgotten half way up, the live wallpaper stayed on showing Hover's
    // marbles, and every setup after went on with its wallpaper left off.

    @Test
    fun `a change of wallpaper while Zenith's live wallpaper is up never forgets the design`() {
        // A still design on its way up over it: the live wallpaper announces its new colours first.
        assertFalse(WallpaperFollow.forgets("Iris", liveUp = true, copyUp = false))
        assertFalse(WallpaperFollow.forgets("Lotus", liveUp = true, copyUp = false))
    }

    @Test
    fun `a still design is Zenith's while its own picture is up`() {
        assertFalse(WallpaperFollow.forgets("Iris", liveUp = false, copyUp = true))
    }

    @Test
    fun `with neither up, something else has become the wallpaper and the design is forgotten`() {
        assertTrue(WallpaperFollow.forgets("Iris", liveUp = false, copyUp = false))
        assertTrue(WallpaperFollow.forgets("Lotus", liveUp = false, copyUp = false))
        // A design that moves is never a still picture, so a picture up is not it.
        assertTrue(WallpaperFollow.forgets("Lotus", liveUp = false, copyUp = true))
    }

    @Test
    fun `with no design named there is nothing to forget`() {
        assertFalse(WallpaperFollow.forgets("", liveUp = false, copyUp = false))
        assertFalse(WallpaperFollow.forgets("", liveUp = true, copyUp = true))
    }

    @Test
    fun `with no design named while Zenith's live wallpaper is up, the one it shows is named again`() {
        assertEquals("Hover", WallpaperFollow.adopt("", liveUp = true, liveShows = "Hover", copyUp = false, appliedKey = null))
        assertEquals("Iris", WallpaperFollow.adopt("", liveUp = true, liveShows = "Iris", copyUp = false, appliedKey = key(style = "Pond")))
    }

    @Test
    fun `with no design named while Zenith's picture of one is up, that one is named again`() {
        assertEquals("Iris", WallpaperFollow.adopt("", liveUp = false, liveShows = null, copyUp = true, appliedKey = key(style = "Iris")))
    }

    @Test
    fun `a photo, a wallpaper from elsewhere, or a design already named, names nothing`() {
        // A photo Zenith put up: its picture is up, and it is no design.
        assertNull(WallpaperFollow.adopt("", liveUp = false, liveShows = null, copyUp = true, appliedKey = null))
        // Something else is up.
        assertNull(WallpaperFollow.adopt("", liveUp = false, liveShows = null, copyUp = false, appliedKey = key(style = "Iris")))
        // Named already.
        assertNull(WallpaperFollow.adopt("Waves", liveUp = true, liveShows = "Hover", copyUp = true, appliedKey = key(style = "Iris")))
        // A design Zenith no longer has.
        assertNull(WallpaperFollow.adopt("", liveUp = false, liveShows = null, copyUp = true, appliedKey = key(style = "Neon")))
        assertNull(WallpaperFollow.adopt("", liveUp = true, liveShows = "Neon", copyUp = false, appliedKey = null))
    }
}
