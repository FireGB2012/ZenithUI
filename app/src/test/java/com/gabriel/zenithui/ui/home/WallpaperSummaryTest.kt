package com.gabriel.zenithui.ui.home

import com.gabriel.zenithui.data.WallStyle
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.theme.Palettes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperSummaryTest {

    @Test
    fun `a design of Zenith's is named with the colours it is painted in`() {
        assertEquals("Aurora · Midnight colours", WallpaperSummary.name(WallStyle.Aurora, Palettes.byId("midnight"), ownPicture = false))
        assertEquals("Aurora · Your colours", WallpaperSummary.name(WallStyle.Aurora, Palettes.custom(ZenithSettings()), ownPicture = false))
        assertEquals("Aurora", WallpaperSummary.name(WallStyle.Aurora, null, ownPicture = false))
    }

    @Test
    fun `a picture of your own, or one set in another app, is said to be`() {
        assertEquals("Your own picture", WallpaperSummary.name(null, null, ownPicture = true))
        assertEquals("From another app", WallpaperSummary.name(null, null, ownPicture = false))
        assertEquals("The glass bends it too", WallpaperSummary.how(null, follows = true, ownPicture = true))
        assertEquals("The glass stays frosted", WallpaperSummary.how(null, follows = true, ownPicture = false))
    }

    @Test
    fun `what it does is said under its name`() {
        assertEquals("Live: moves gently on its own", WallpaperSummary.how(WallStyle.Hover, follows = true, ownPicture = false))
        assertEquals("Live: opens as you unlock", WallpaperSummary.how(WallStyle.Lotus, follows = true, ownPicture = false))
        assertEquals("Changes with light and dark", WallpaperSummary.how(WallStyle.Aurora, follows = true, ownPicture = false))
        assertEquals("Stays as it is in light and dark", WallpaperSummary.how(WallStyle.Aurora, follows = false, ownPicture = false))
    }

    @Test
    fun `every design in every colour set is named in a line the card has room for`() {
        WallStyle.entries.forEach { style ->
            Palettes.all.forEach { set ->
                val name = WallpaperSummary.name(style, set, ownPicture = false)
                assertTrue(name, name.length <= 30)
            }
        }
    }
}
