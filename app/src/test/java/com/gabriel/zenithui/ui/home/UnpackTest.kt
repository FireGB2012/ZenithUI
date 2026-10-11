package com.gabriel.zenithui.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnpackTest {

    private val apps = 0 until 16

    @Test
    fun `nothing is out while the folder is still in its icon`() {
        for (index in apps) {
            assertEquals(0f, Unpack.shown(0f, index), 0f)
            assertEquals(0f, Unpack.shown(0.3f, index), 0f)
        }
    }

    @Test
    fun `everything is in its place once the folder is open, however many there are`() {
        for (index in apps) assertEquals("app $index", 1f, Unpack.shown(1f, index), 0f)
        // The pane's spring carries it a little past open: nothing goes past its place with it.
        for (index in apps) assertEquals(1f, Unpack.shown(1.06f, index), 0f)
    }

    @Test
    fun `the first out is always the furthest out`() {
        var open = 0f
        while (open <= 1f) {
            apps.zipWithNext { sooner, later ->
                assertTrue("at $open", Unpack.shown(open, sooner) >= Unpack.shown(open, later))
            }
            open += 0.02f
        }
        // And part of the way open, the first really is ahead of the ones behind it.
        assertTrue(Unpack.shown(0.6f, 0) > Unpack.shown(0.6f, 4))
    }

    @Test
    fun `an app only ever comes further out as the folder opens`() {
        for (index in apps) {
            var last = 0f
            var open = 0f
            while (open <= 1f) {
                val out = Unpack.shown(open, index)
                assertTrue("app $index at $open", out >= last)
                last = out
                open += 0.01f
            }
        }
    }

    @Test
    fun `a long folder does not keep the last of them waiting for ever`() {
        // The ninth and every one after it set off together.
        assertEquals(Unpack.shown(0.8f, 8), Unpack.shown(0.8f, 40), 0f)
    }
}
