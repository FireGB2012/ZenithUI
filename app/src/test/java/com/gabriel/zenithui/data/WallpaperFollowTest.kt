package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
}
