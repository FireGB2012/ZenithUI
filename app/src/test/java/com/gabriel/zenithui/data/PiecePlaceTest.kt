package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Where a piece of a made widget is drawn from its point, and how it is moved and pulled. */
class PiecePlaceTest {

    @Test
    fun `a piece starts at its point, sits round it, or ends at it`() {
        assertEquals(40f, PiecePlace.left(40f, 100f, PieceAlign.START))
        assertEquals(-10f, PiecePlace.left(40f, 100f, PieceAlign.CENTER))
        assertEquals(-60f, PiecePlace.left(40f, 100f, PieceAlign.END))
        // Down, always by its middle.
        assertEquals(30f, PiecePlace.top(50f, 40f))
    }

    @Test
    fun `lined up the other way, it stays where it is`() {
        // From 40 to 140: by its start its point is at 40, by its middle at 90, by its end at 140.
        assertEquals(90f, PiecePlace.repoint(40f, 100f, PieceAlign.START, PieceAlign.CENTER))
        assertEquals(140f, PiecePlace.repoint(40f, 100f, PieceAlign.START, PieceAlign.END))
        assertEquals(40f, PiecePlace.repoint(140f, 100f, PieceAlign.END, PieceAlign.START))
        assertEquals(40f, PiecePlace.repoint(40f, 100f, PieceAlign.START, PieceAlign.START))
    }

    @Test
    fun `moved, its point follows its box and stays on the widget`() {
        assertEquals(120f, PiecePlace.pointX(120f, 100f, PieceAlign.START, 400f))
        assertEquals(170f, PiecePlace.pointX(120f, 100f, PieceAlign.CENTER, 400f))
        assertEquals(220f, PiecePlace.pointX(120f, 100f, PieceAlign.END, 400f))
        // Pulled off the left, a piece that starts at its point stops at the edge.
        assertEquals(0f, PiecePlace.pointX(-80f, 100f, PieceAlign.START, 400f))
        // Pulled off the right, it stops with no more than half of it over the edge.
        assertEquals(350f, PiecePlace.pointX(390f, 100f, PieceAlign.START, 400f))
        assertEquals(400f, PiecePlace.pointX(380f, 100f, PieceAlign.CENTER, 400f))
        assertEquals(400f, PiecePlace.pointX(360f, 100f, PieceAlign.END, 400f))
        // Down, by its middle.
        assertEquals(70f, PiecePlace.pointY(50f, 40f, 200f))
        assertEquals(0f, PiecePlace.pointY(-60f, 40f, 200f))
        assertEquals(200f, PiecePlace.pointY(400f, 40f, 200f))
    }

    @Test
    fun `pulled by its corner, the corner stays under the finger`() {
        // Its box grows from its point: from the start, by the whole pull; round its middle, by twice it.
        assertEquals(10f to 16f, PiecePlace.stretch(PieceAlign.START, 10f, 8f))
        assertEquals(20f to 16f, PiecePlace.stretch(PieceAlign.CENTER, 10f, 8f))
        // One that ends at its point has its corner on the left, and grows the other way.
        assertEquals(10f to 16f, PiecePlace.stretch(PieceAlign.END, -10f, 8f))
        assertTrue(PiecePlace.cornerOnLeft(PieceAlign.END))
        assertFalse(PiecePlace.cornerOnLeft(PieceAlign.START))
        assertFalse(PiecePlace.cornerOnLeft(PieceAlign.CENTER))
    }

    @Test
    fun `words grow by as much as their box does`() {
        assertEquals(1.5f, PiecePlace.growth(100f, 20f, 50f, 10f), 0.0001f)
        assertEquals(1f, PiecePlace.growth(0f, 0f, 50f, 10f), 0.0001f)
    }

    @Test
    fun `words too wide for the widget are drawn smaller until they fit`() {
        assertEquals(1f, PiecePlace.fit(200f, 300f))
        assertEquals(0.5f, PiecePlace.fit(600f, 300f))
        // Not measured yet, or with no room at all: as they are.
        assertEquals(1f, PiecePlace.fit(0f, 300f))
        assertEquals(1f, PiecePlace.fit(600f, 0f))
    }
}
