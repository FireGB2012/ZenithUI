package com.gabriel.zenithui.ui.home

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToggleSymbolsTest {

    @Test
    fun `a round button's symbol is half the button, never smaller than 19 dp or larger than 32`() {
        assertEquals(24f, ToggleSymbols.button(48.dp).value, 0.001f)
        assertEquals(19f, ToggleSymbols.button(30.dp).value, 0.001f)
        assertEquals(32f, ToggleSymbols.button(80.dp).value, 0.001f)
    }

    @Test
    fun `symbols are larger than they were, so each control is known at a glance`() {
        // Testers asked for the Quick Toggles' symbols to be a little larger. They were 0.44 of a
        // button between 18 and 28 dp, half a tile's disc, 23 dp on a card, and 0.7 with no glass.
        assertTrue(ToggleSymbols.BUTTON > 0.44f)
        assertTrue(ToggleSymbols.SMALLEST > 18.dp && ToggleSymbols.LARGEST > 28.dp)
        assertTrue(ToggleSymbols.TILE > 0.5f)
        assertTrue(ToggleSymbols.CARD > 23.dp)
        assertTrue(ToggleSymbols.BARE > 0.7f)
    }

    @Test
    fun `a symbol still leaves a ring of its disc showing round it`() {
        // The disc's colour round the symbol is how a control says it is on.
        for (disc in listOf(30.dp, 36.dp, 40.dp)) assertTrue(ToggleSymbols.tile(disc) <= disc * 0.6f)
        for (button in listOf(40.dp, 52.dp, 64.dp, 80.dp)) assertTrue(ToggleSymbols.button(button) <= button * 0.6f)
        assertTrue(ToggleSymbols.CARD <= ToggleSymbols.CARD_DISC * 0.6f)
    }
}
