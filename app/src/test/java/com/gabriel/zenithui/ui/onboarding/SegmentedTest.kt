package com.gabriel.zenithui.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class SegmentedTest {

    @Test
    fun `a lens over the middle of an option is nearest to that option`() {
        assertEquals(0, nearestSegment(thumbCentreX = 50f, trackWidth = 300f, count = 3))
        assertEquals(1, nearestSegment(thumbCentreX = 150f, trackWidth = 300f, count = 3))
        assertEquals(2, nearestSegment(thumbCentreX = 250f, trackWidth = 300f, count = 3))
    }

    @Test
    fun `either side of the line between two options it goes to the nearer`() {
        assertEquals(0, nearestSegment(thumbCentreX = 99f, trackWidth = 300f, count = 3))
        assertEquals(1, nearestSegment(thumbCentreX = 101f, trackWidth = 300f, count = 3))
        assertEquals(1, nearestSegment(thumbCentreX = 199f, trackWidth = 300f, count = 3))
        assertEquals(2, nearestSegment(thumbCentreX = 201f, trackWidth = 300f, count = 3))
    }

    @Test
    fun `past either end it is the end option`() {
        assertEquals(0, nearestSegment(thumbCentreX = -40f, trackWidth = 300f, count = 3))
        assertEquals(2, nearestSegment(thumbCentreX = 380f, trackWidth = 300f, count = 3))
        assertEquals(3, nearestSegment(thumbCentreX = 400f, trackWidth = 400f, count = 4))
    }

    @Test
    fun `a row of one, or one not measured yet, has only the first`() {
        assertEquals(0, nearestSegment(thumbCentreX = 120f, trackWidth = 300f, count = 1))
        assertEquals(0, nearestSegment(thumbCentreX = 120f, trackWidth = 0f, count = 3))
        assertEquals(0, nearestSegment(thumbCentreX = 120f, trackWidth = 300f, count = 0))
    }
}
