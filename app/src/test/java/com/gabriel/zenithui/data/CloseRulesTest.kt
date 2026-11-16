package com.gabriel.zenithui.data

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloseRulesTest {

    private fun route(
        style: String = CloseRules.INTO_ICON,
        asked: Boolean = false,
        target: Boolean = true,
        picture: Boolean = false,
        recent: Boolean = true,
        cardFrom: Float = 0f,
    ) = CloseRules.route(style, asked, target, picture, recent, cardFrom)

    @Test
    fun `when Android asks where the icon is, the real window goes into it`() {
        assertEquals(CloseRoute.Handshake, route(asked = true))
        // Ahead of a picture, which would only play the close a second time.
        assertEquals(CloseRoute.Handshake, route(asked = true, picture = true))
        // Asked, with no icon to give: Android does its own, and nothing is added after it.
        assertEquals(CloseRoute.None, route(asked = true, target = false, picture = true))
    }

    @Test
    fun `without Android's question, a picture of the app shrinks into its icon, or the middle`() {
        assertEquals(CloseRoute.Picture, route(picture = true))
        assertEquals(CloseRoute.Picture, route(picture = true, target = false))
        assertEquals(CloseRoute.Picture, route(picture = true, cardFrom = 0.6f))
    }

    @Test
    fun `with neither, it closes as it always did`() {
        assertEquals(CloseRoute.Land, route())
        assertEquals(CloseRoute.Card, route(cardFrom = 0.4f))
        // The colour card's "Off".
        assertEquals(CloseRoute.Land, route(cardFrom = 0.004f))
        // Nothing to land on, or opened too long ago to be the app that has just closed.
        assertEquals(CloseRoute.None, route(target = false))
        assertEquals(CloseRoute.None, route(recent = false, cardFrom = 0.4f))
    }

    @Test
    fun `Plain closes as it did before, whatever Android or the helper offers`() {
        assertEquals(CloseRoute.Land, route(style = CloseRules.PLAIN, asked = true, picture = true))
        assertEquals(CloseRoute.Card, route(style = CloseRules.PLAIN, picture = true, cardFrom = 0.5f))
        assertEquals(CloseRoute.None, route(style = CloseRules.PLAIN, target = false, picture = true))
        // A style this version has not heard of is Zenith's own.
        assertEquals(CloseRoute.Handshake, route(style = "somethingNew", asked = true))
    }

    @Test
    fun `the picture comes in over the way out and goes over the same stretch on the way back`() {
        assertEquals(0f, CloseRules.pictureShare(0f), 0f)
        assertEquals(0f, CloseRules.pictureShare(CloseRules.PICTURE_FROM), 0f)
        assertEquals(1f, CloseRules.pictureShare(CloseRules.PICTURE_TO), 0f)
        assertEquals(1f, CloseRules.pictureShare(1.04f), 0f)
        assertEquals(0.5f, CloseRules.pictureShare((CloseRules.PICTURE_FROM + CloseRules.PICTURE_TO) / 2f), 0.001f)
        // Always on its way in as the card grows.
        var last = -1f
        for (i in 0..100) {
            val share = CloseRules.pictureShare(i / 100f)
            assertTrue(share >= last)
            last = share
        }
    }

    @Test
    fun `a closing card fades in over its first stretch, so it never pops up over the home screen`() {
        assertEquals(0f, CloseRules.closingAlpha(0.92f, from = 0.92f), 0f)
        assertEquals(0.5f, CloseRules.closingAlpha(0.92f - CloseRules.CLOSE_FADE_IN / 2f, from = 0.92f), 0.001f)
        assertEquals(1f, CloseRules.closingAlpha(0.92f - CloseRules.CLOSE_FADE_IN, from = 0.92f), 0f)
        assertEquals(1f, CloseRules.closingAlpha(0f, from = 0.92f), 0f)
        // A spring carrying it a little past the icon keeps it whole.
        assertEquals(1f, CloseRules.closingAlpha(-0.03f, from = 0.92f), 0f)
    }

    @Test
    fun `with no icon to go into, it fades out over its last stretch`() {
        assertEquals(1f, CloseRules.vanishAlpha(1f), 0f)
        assertEquals(1f, CloseRules.vanishAlpha(CloseRules.VANISH), 0f)
        assertEquals(0.5f, CloseRules.vanishAlpha(CloseRules.VANISH / 2f), 0.001f)
        assertEquals(0f, CloseRules.vanishAlpha(0f), 0f)
        assertEquals(0f, CloseRules.vanishAlpha(-0.05f), 0f)
    }

    @Test
    fun `a picture fills the card without being stretched, keeping its middle`() {
        // A 576 by 1280 picture into a square: all of its width, the middle of its height.
        assertEquals(CloseRules.Crop(0, 352, 576, 576), CloseRules.coverCrop(576, 1280, 168f, 168f))
        // Into a card the shape of the screen it was taken on: all of it.
        assertEquals(CloseRules.Crop(0, 0, 576, 1280), CloseRules.coverCrop(576, 1280, 1080f, 2400f))
        // Into a card wider than it: all of its height, the middle of its width.
        assertEquals(CloseRules.Crop(144, 0, 288, 1280), CloseRules.coverCrop(576, 1280, 300f, 1333.3334f))
        // Nothing to fill, or nothing to fill it with.
        assertEquals(CloseRules.Crop(0, 0, 576, 1280), CloseRules.coverCrop(576, 1280, 0f, 0f))
        assertEquals(CloseRules.Crop(0, 0, 0, 0), CloseRules.coverCrop(0, 0, 100f, 100f))
    }

    @Test
    fun `an app with no icon on show goes into the middle of the screen`() {
        assertEquals(Rect(510f, 1170f, 570f, 1230f), CloseRules.middle(1080f, 2400f, 60f))
    }

    @Test
    fun `which app a key opens`() {
        assertEquals("com.example.mail", CloseRules.packageOf("com.example.mail/com.example.mail.Main"))
        assertEquals("com.example.mail", CloseRules.packageOf("work:com.example.mail/com.example.mail.Main"))
        assertEquals("com.example.mail", CloseRules.packageOf("shortcut:com.example.mail/compose/new"))
        assertEquals("", CloseRules.packageOf("shortcut:broken"))
        assertTrue(CloseRules.isWork("work:com.example.mail/com.example.mail.Main"))
        assertTrue(!CloseRules.isWork("com.example.mail/com.example.mail.Main"))
        assertEquals("com.example.mail/.Main", CloseRules.keyOf("com.example.mail", ".Main", work = false))
        assertEquals("work:com.example.mail/.Main", CloseRules.keyOf("com.example.mail", ".Main", work = true))
    }
}
