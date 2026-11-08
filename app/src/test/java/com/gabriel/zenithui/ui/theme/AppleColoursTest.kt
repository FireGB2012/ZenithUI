package com.gabriel.zenithui.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleColoursTest {

    private val every = Palettes.all + Palettes.aura
    private val both = listOf(AppleColours.Light, AppleColours.Dark)

    @Test
    fun `words and second lines stand four and a half to one on the page and on every card`() {
        for (s in both) {
            for (ground in listOf(s.page, s.card, s.raised)) {
                assertTrue("words, dark=${s.dark}", contrast(s.words, ground) >= 4.5f)
                assertTrue("second line, dark=${s.dark}", contrast(s.secondary, ground) >= 4.5f)
            }
        }
    }

    @Test
    fun `every palette's accent stands three to one on a card and on the page, light and dark`() {
        for (palette in every) {
            for (dark in listOf(true, false)) {
                val s = AppleColours.of(dark)
                val accent = AppleColours.accent(palette, dark)
                assertTrue("${palette.id} dark=$dark on the card", contrast(accent, s.card) >= 3f)
                assertTrue("${palette.id} dark=$dark on the page", contrast(accent, s.page) >= 3f)
            }
        }
    }

    @Test
    fun `frost is apple's own blue`() {
        assertEquals(Color(0xFF0A84FF), AppleColours.accent(Palettes.Frost, dark = true))
        assertEquals(Color(0xFF007AFF), AppleColours.accent(Palettes.Frost, dark = false))
    }

    @Test
    fun `an accent that already stands is left as it is`() {
        assertEquals(Palettes.Sunset.accent, AppleColours.accent(Palettes.Sunset, dark = true))
        assertEquals(Palettes.Midnight.accent, AppleColours.accent(Palettes.Midnight, dark = true))
        assertEquals(Palettes.Midnight.accent, AppleColours.accent(Palettes.Midnight, dark = false))
    }

    @Test
    fun `a pale accent is taken deeper in the light, and a deep one lighter in the dark`() {
        val pale = Color(0xFFF2F2F4)
        val deeper = AppleColours.readable(pale, dark = false)
        assertTrue(contrast(deeper, AppleColours.Light.page) >= 3f)
        assertTrue(deeper.luminance() < pale.luminance())

        val deep = Color(0xFF15161A)
        val lighter = AppleColours.readable(deep, dark = true)
        assertTrue(contrast(lighter, AppleColours.Dark.card) >= 3f)
        assertTrue(lighter.luminance() > deep.luminance())
    }

    @Test
    fun `it is moved no further than it needs to be`() {
        for (palette in listOf(Palettes.Graphite, Palettes.Solstice)) {
            val accent = AppleColours.accent(palette, dark = false)
            val s = AppleColours.Light
            val least = minOf(contrast(accent, s.card), contrast(accent, s.page))
            assertTrue("${palette.id}: $least", least in 3f..3.15f)
        }
    }

    @Test
    fun `a tile for white words is left alone when deep, and taken only as deep as it must be when pale`() {
        val deep = Color(0xFF1B2C55)
        assertEquals(deep, AppleColours.underWhite(deep))
        val pale = Color(0xFFFFE08A)
        val under = AppleColours.underWhite(pale)
        val stands = contrast(Color.White, under)
        assertTrue("$stands", stands in 4.5f..4.65f)
        assertTrue(under.luminance() < pale.luminance())
    }

    @Test
    fun `words and second lines stand four and a half to one on a pressed row and in the search field`() {
        for (s in both) {
            val grounds = listOf(
                "a pressed row" to s.pressed,
                "the field on the page" to s.field.compositeOver(s.page),
                "the field on a card" to s.field.compositeOver(s.card),
            )
            for ((name, ground) in grounds) {
                assertTrue("words on $name, dark=${s.dark}", contrast(s.words, ground) >= 4.5f)
                assertTrue("second line on $name, dark=${s.dark}", contrast(s.secondary, ground) >= 4.5f)
            }
        }
    }

    @Test
    fun `words stand four and a half to one on the segmented control's pill`() {
        for (s in both) {
            assertTrue("dark=${s.dark}", contrast(s.words, s.pill) >= 4.5f)
        }
    }

    @Test
    fun `a main button is the accent, its words standing three to one on it, for every palette`() {
        // Three, not four and a half: Apple's own white on its blue is four to one, and these
        // words are large and heavy.
        for (palette in every) {
            for (dark in listOf(true, false)) {
                val s = AppleColours.of(dark)
                val accent = AppleColours.accent(palette, dark)
                val main = AppleColours.button(ButtonKind.Main, accent, s)
                assertEquals(accent, main.fill)
                assertTrue("${palette.id} dark=$dark", contrast(main.words, main.fill) >= 3f)
            }
        }
    }

    @Test
    fun `a grey button's words stand four and a half to one on it, over a card and over the page`() {
        for (s in both) {
            val grey = AppleColours.button(ButtonKind.Grey, AppleColours.accent(Palettes.Frost, s.dark), s)
            for (ground in listOf(s.card, s.page)) {
                assertTrue("dark=${s.dark}", contrast(grey.words, grey.fill.compositeOver(ground)) >= 4.5f)
            }
        }
    }

    @Test
    fun `a plain button is the accent's words with nothing behind them`() {
        for (s in both) {
            val accent = AppleColours.accent(Palettes.Frost, s.dark)
            val plain = AppleColours.button(ButtonKind.Plain, accent, s)
            assertEquals(accent, plain.words)
            assertEquals(0f, plain.fill.alpha, 0f)
        }
    }

    @Test
    fun `words that undo something stand three to one on a card and on the page`() {
        for (s in both) {
            assertTrue("card, dark=${s.dark}", contrast(s.danger, s.card) >= 3f)
            assertTrue("page, dark=${s.dark}", contrast(s.danger, s.page) >= 3f)
        }
    }

    @Test
    fun `contrast is the same either way round, and words see through to what is under them`() {
        assertEquals(contrast(Color.Black, Color.White), contrast(Color.White, Color.Black), 0.0001f)
        assertEquals(21f, contrast(Color.Black, Color.White), 0.01f)
        assertEquals(1f, contrast(Color.White.copy(alpha = 0f), Color.White), 0.0001f)
    }
}
