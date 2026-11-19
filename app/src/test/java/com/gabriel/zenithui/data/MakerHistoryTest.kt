package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Undo and redo in the widget maker. */
class MakerHistoryTest {

    private val first = MadeWidget(id = "a", name = "One")
    private val second = first.copy(name = "Two")
    private val third = first.copy(name = "Three")

    @Test
    fun `undo takes a change back and redo brings it back`() {
        val start = MakerHistory(first)
        assertFalse(start.canUndo)
        assertFalse(start.canRedo)
        val changed = start.changed(second)
        assertTrue(changed.canUndo)
        val undone = changed.undone()
        assertEquals(first, undone.now)
        assertTrue(undone.canRedo)
        assertEquals(second, undone.redone().now)
    }

    @Test
    fun `a whole drag is one step, and so is a slider's whole pull`() {
        val dragged = MakerHistory(first)
            .changed(second, key = "move p1")
            .changed(third, key = "move p1")
        assertEquals(third, dragged.now)
        assertEquals(first, dragged.undone().now)
        // Once it is let go, the next drag is a step of its own, even of the same piece.
        val again = dragged.settled().changed(second, key = "move p1")
        assertEquals(third, again.undone().now)
        // A change of something else is a step of its own too.
        val other = dragged.changed(second, key = "size p1")
        assertEquals(third, other.undone().now)
    }

    @Test
    fun `a change after an undo forgets what could have been redone`() {
        val history = MakerHistory(first).changed(second).undone().changed(third)
        assertFalse(history.canRedo)
        assertEquals(first, history.undone().now)
    }

    @Test
    fun `nothing to undo or redo leaves it as it is, and so does a change to the same thing`() {
        val start = MakerHistory(first)
        assertSame(start, start.undone())
        assertSame(start, start.redone())
        assertSame(start, start.changed(first))
    }

    @Test
    fun `only so many steps are kept`() {
        var history = MakerHistory(first)
        repeat(MakerHistory.MOST + 10) { history = history.changed(first.copy(name = "$it")) }
        var steps = 0
        while (history.canUndo) { history = history.undone(); steps++ }
        assertEquals(MakerHistory.MOST, steps)
    }
}
