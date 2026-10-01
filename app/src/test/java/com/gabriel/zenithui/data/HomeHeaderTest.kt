package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeHeaderTest {

    @Test
    fun `the name is shown as it is, and Zenith when there is none`() {
        assertEquals("Gabriel", HomeHeader.text(HomeHeader.NAME, " Gabriel ", hour = 9))
        assertEquals("Zenith", HomeHeader.text(HomeHeader.NAME, "", hour = 9))
    }

    @Test
    fun `the greeting follows the time of day`() {
        assertEquals("Good morning, Gabriel", HomeHeader.text(HomeHeader.GREETING, "Gabriel", hour = 5))
        assertEquals("Good afternoon, Gabriel", HomeHeader.text(HomeHeader.GREETING, "Gabriel", hour = 12))
        assertEquals("Good evening, Gabriel", HomeHeader.text(HomeHeader.GREETING, "Gabriel", hour = 22))
        assertEquals("Good night, Gabriel", HomeHeader.text(HomeHeader.GREETING, "Gabriel", hour = 23))
        assertEquals("Good night, Gabriel", HomeHeader.text(HomeHeader.GREETING, "Gabriel", hour = 4))
    }

    @Test
    fun `a greeting with no name is just the greeting`() {
        assertEquals("Good morning", HomeHeader.text(HomeHeader.GREETING, "  ", hour = 8))
    }

    @Test
    fun `off shows nothing and an unknown style shows the name`() {
        assertNull(HomeHeader.text(HomeHeader.OFF, "Gabriel", hour = 8))
        assertEquals("Gabriel", HomeHeader.text("something else", "Gabriel", hour = 8))
    }
}
