package com.gabriel.zenithui.ui.home

import androidx.compose.ui.graphics.Color
import com.gabriel.zenithui.ui.theme.NeutralLook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The words on a widget you made in a colour of your own: [WidgetColours.made]. */
class MadeCardInkTest {
    private val white = NeutralLook.Dark.onGlass
    private val nearBlack = NeutralLook.Light.onGlass

    @Test
    fun `white words on iOS blue, red, purple, pink and indigo, as on an iPhone`() {
        for (colour in listOf(0xFF0A84FF, 0xFFFF453A, 0xFFBF5AF2, 0xFFFF375F, 0xFF5E5CE6)) {
            assertEquals("on ${colour.toString(16)}", white, WidgetColours.made(Color(colour), dark = false).ink)
        }
    }

    @Test
    fun `near-black words where white would not read, on yellow, mint, cyan, orange and green`() {
        for (colour in listOf(0xFFFFD60A, 0xFF63E6E2, 0xFF64D2FF, 0xFFFF9F0A, 0xFF30D158)) {
            assertEquals("on ${colour.toString(16)}", nearBlack, WidgetColours.made(Color(colour), dark = false).ink)
        }
    }

    @Test
    fun `the words are the same in Light and in Dark, since the colour is the one chosen`() {
        for (colour in listOf(0xFF0A84FF, 0xFFFF9F0A, 0xFF1C1C1E, 0xFFFFFFFF, 0xFFAC8E68)) {
            assertEquals(WidgetColours.made(Color(colour), dark = false), WidgetColours.made(Color(colour), dark = true))
        }
    }

    @Test
    fun `white words always stand at least three to one, what large words need`() {
        for (colour in listOf(0xFF0A84FF, 0xFFFF453A, 0xFFBF5AF2, 0xFFFF375F, 0xFF5E5CE6, 0xFF1C1C1E)) {
            val fill = WidgetColours.made(Color(colour), dark = false)
            assertTrue(contrast(fill.ink, fill.top) >= 3f && contrast(fill.ink, fill.bottom) >= 3f)
        }
    }

    @Test
    fun `with no colour of its own it is the plain card`() {
        assertEquals(WidgetColours.of("made", dark = true), WidgetColours.made(null, dark = true))
        assertEquals(WidgetColours.of("made", dark = false), WidgetColours.made(null, dark = false))
    }
}
