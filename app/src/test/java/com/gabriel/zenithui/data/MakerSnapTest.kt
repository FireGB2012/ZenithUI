package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** What a piece being moved in the maker lines up with. The widget here is 400 by 200. */
class MakerSnapTest {

    private val width = 400f
    private val height = 200f
    private val margin = 16f
    private val within = 6f

    private fun snap(moving: Box, others: List<Box> = emptyList()) =
        MakerSnap.snap(moving, others, width, height, margin, within)

    @Test
    fun `near the middle, a piece is pulled onto it and the line shows`() {
        // Its middle is at 198, two short of the widget's.
        val snapped = snap(Box(150f, 70f, 96f, 40f))
        assertEquals(2f, snapped.dx, 0.001f)
        assertEquals(200f, snapped.lineX)
        // Down, nothing is near enough.
        assertEquals(0f, snapped.dy, 0.001f)
        assertNull(snapped.lineY)
        assertTrue(snapped.lined)
    }

    @Test
    fun `far from everything, it stays where the finger put it`() {
        val snapped = snap(Box(60f, 70f, 50f, 40f))
        assertEquals(0f, snapped.dx, 0.001f)
        assertEquals(0f, snapped.dy, 0.001f)
        assertNull(snapped.lineX)
        assertNull(snapped.lineY)
        assertFalse(snapped.lined)
    }

    @Test
    fun `its edges line up with the margin round the widget, across and down apart`() {
        // Its left edge is 3 past the margin, its bottom 2 short of the one at the foot.
        val snapped = snap(Box(19f, 142f, 60f, 40f))
        assertEquals(-3f, snapped.dx, 0.001f)
        assertEquals(16f, snapped.lineX)
        assertEquals(2f, snapped.dy, 0.001f)
        assertEquals(184f, snapped.lineY)
        // And the right edge with the margin on the right.
        val right = snap(Box(320f, 70f, 66f, 40f))
        assertEquals(-2f, right.dx, 0.001f)
        assertEquals(384f, right.lineX)
    }

    @Test
    fun `it lines up with the edges and middles of the other pieces`() {
        val other = Box(40f, 60f, 100f, 30f)
        // Its left edge is 3 past the other's.
        val snapped = snap(Box(43f, 120f, 80f, 30f), listOf(other))
        assertEquals(-3f, snapped.dx, 0.001f)
        assertEquals(40f, snapped.lineX)
        assertNull(snapped.lineY)
        // Its middle with the other's middle, down: 77 to 75.
        val level = snap(Box(250f, 57f, 60f, 40f), listOf(other))
        assertEquals(-2f, level.dy, 0.001f)
        assertEquals(75f, level.lineY)
    }

    @Test
    fun `the nearest line wins`() {
        // Its middle is 1 past the widget's and its right edge 2 short of the other piece's.
        val other = Box(203f, 150f, 50f, 20f)
        val snapped = snap(Box(151f, 70f, 100f, 40f), listOf(other))
        assertEquals(-1f, snapped.dx, 0.001f)
        assertEquals(200f, snapped.lineX)
    }

    @Test
    fun `a box knows its edges and middle`() {
        val box = Box(10f, 20f, 100f, 40f)
        assertEquals(110f, box.right)
        assertEquals(60f, box.bottom)
        assertEquals(60f, box.middleX)
        assertEquals(40f, box.middleY)
        assertEquals(Box(15f, 17f, 100f, 40f), box.moved(5f, -3f))
    }
}
