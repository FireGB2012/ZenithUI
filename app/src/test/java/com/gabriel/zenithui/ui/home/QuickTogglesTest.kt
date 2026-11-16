package com.gabriel.zenithui.ui.home

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class QuickTogglesTest {

    @Test
    fun `a lit control's symbol is white, as Control Center has it`() {
        for (tone in listOf(Tone.Blue, Tone.Green, Tone.Orange, Tone.Indigo)) {
            for (dark in listOf(true, false)) {
                assertEquals("$tone, dark $dark", Color.White, litInk(tone.lit(dark)))
            }
        }
    }

    @Test
    fun `but dark where its colour is too light for white`() {
        for (dark in listOf(true, false)) {
            assertNotEquals("Yellow, dark $dark", Color.White, litInk(Tone.Yellow.lit(dark)))
        }
        // The torch is white in Dark, so its symbol is dark; in Light it is near-black, a white
        // torch being lost on a white card, and its symbol white again.
        assertNotEquals(Color.White, litInk(Tone.Light.lit(dark = true)))
        assertEquals(Color.White, litInk(Tone.Light.lit(dark = false)))
    }

    @Test
    fun `a no glass torch lights up in a colour, since its symbol is white while it is off`() {
        assertNotEquals(Color.White, Tone.Light.lit(dark = true, bare = true))
    }
}
