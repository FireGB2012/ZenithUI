package com.gabriel.zenithui.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceTest {

    @Test
    fun `light and dark are what was asked for whatever the phone is doing`() {
        assertFalse(ZenithSettings(appearance = "light").isDark(systemDark = true))
        assertTrue(ZenithSettings(appearance = "dark").isDark(systemDark = false))
    }

    @Test
    fun `auto, and anything not understood, follows the phone`() {
        assertTrue(ZenithSettings(appearance = "auto").isDark(systemDark = true))
        assertFalse(ZenithSettings(appearance = "auto").isDark(systemDark = false))
        assertTrue(ZenithSettings(appearance = "").isDark(systemDark = true))
    }
}
