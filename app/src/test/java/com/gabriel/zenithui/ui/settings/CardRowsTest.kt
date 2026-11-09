package com.gabriel.zenithui.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class CardRowsTest {

    @Test
    fun `a hairline runs along the top of every row that shows but the first`() {
        assertEquals(listOf(52, 104), hairlinesAt(listOf(52, 52, 52)))
        // Rows of different heights: each line where the row under it begins.
        assertEquals(listOf(52, 96), hairlinesAt(listOf(52, 44, 60)))
    }

    @Test
    fun `a row folded away to nothing has no hairline of its own`() {
        // An app's shortcuts, not yet unfolded, at the top of the card: no line along its top edge.
        assertEquals(listOf(52), hairlinesAt(listOf(0, 52, 52)))
        // One folded away between two: a single line between them, not two on top of each other.
        assertEquals(listOf(52), hairlinesAt(listOf(52, 0, 52)))
        // And none under the last row that shows.
        assertEquals(emptyList<Int>(), hairlinesAt(listOf(52, 0)))
    }

    @Test
    fun `a card of one row or of none has no hairline`() {
        assertEquals(emptyList<Int>(), hairlinesAt(listOf(52)))
        assertEquals(emptyList<Int>(), hairlinesAt(emptyList()))
        assertEquals(emptyList<Int>(), hairlinesAt(listOf(0, 0)))
    }
}
