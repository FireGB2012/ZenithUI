package com.gabriel.zenithui.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class ChoiceChipTest {

    // A 412 dp phone's row of 364 dp at three pixels a dp, chips 10 dp apart, each keeping 8 dp
    // either side of its name.
    private val row = 1092f
    private val gap = 30f
    private val inset = 24f

    @Test
    fun `three to a row while the widest name fits a third of it`() {
        assertEquals(3, chipsPerRow(rowWidth = row, gap = gap, inset = inset, widest = 200f))
        // A third is 344, less the room either side: 296 exactly still fits.
        assertEquals(3, chipsPerRow(rowWidth = row, gap = gap, inset = inset, widest = 296f))
    }

    @Test
    fun `two to a row when the widest fits only half, and one when it fits not even that`() {
        assertEquals(2, chipsPerRow(rowWidth = row, gap = gap, inset = inset, widest = 297f))
        assertEquals(2, chipsPerRow(rowWidth = row, gap = gap, inset = inset, widest = 483f))
        assertEquals(1, chipsPerRow(rowWidth = row, gap = gap, inset = inset, widest = 484f))
    }

    @Test
    fun `a row or a name not measured yet keeps the three`() {
        assertEquals(3, chipsPerRow(rowWidth = 0f, gap = gap, inset = inset, widest = 200f))
        assertEquals(3, chipsPerRow(rowWidth = row, gap = gap, inset = inset, widest = 0f))
    }
}
