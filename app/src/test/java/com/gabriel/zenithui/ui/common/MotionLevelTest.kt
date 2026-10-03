package com.gabriel.zenithui.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class MotionLevelTest {

    @Test
    fun `everything moves only when it was asked for and the phone allows it`() {
        assertEquals(MotionLevel.Full, MotionLevel.of("full", systemAnimations = true))
    }

    @Test
    fun `the setting turns it down`() {
        assertEquals(MotionLevel.Reduced, MotionLevel.of("reduced", systemAnimations = true))
    }

    @Test
    fun `the phone's own Remove animations turns it down whatever the setting`() {
        assertEquals(MotionLevel.Reduced, MotionLevel.of("full", systemAnimations = false))
        assertEquals(MotionLevel.Reduced, MotionLevel.of("reduced", systemAnimations = false))
    }

    @Test
    fun `a value from a newer Zenith is read as full, the default`() {
        // Somebody who never asked for less should not get less because of a word we do not know.
        assertEquals(MotionLevel.Full, MotionLevel.of("extra", systemAnimations = true))
        assertEquals(MotionLevel.Full, MotionLevel.of("", systemAnimations = true))
    }
}
