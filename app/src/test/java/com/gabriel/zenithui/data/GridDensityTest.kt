package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GridDensityTest {

    // The phone Zenith is tested on: 1344 by 2992 pixels at 480 dpi.
    private val wide = 448f
    private val tall = 997f

    private fun grid(id: String, columns: Int = 0, rows: Int = 0, width: Float = wide, height: Float = tall) =
        GridDensity.resolve(id, columns, rows, width, height)

    @Test
    fun `the presets give the grids they always have on a phone`() {
        assertEquals(5 to 6, grid("compact"))
        assertEquals(4 to 5, grid("balanced"))
        assertEquals(4 to 4, grid("roomy"))
        GridDensity.entries.forEach { assertEquals(it.gridFor(wide, tall), grid(it.id)) }
    }

    @Test
    fun `a grid picked by hand is that grid`() {
        assertEquals(5 to 5, grid(GridDensity.CUSTOM, 5, 5))
        assertEquals(6 to 8, grid(GridDensity.CUSTOM, 6, 8))
        assertEquals(3 to 3, grid(GridDensity.CUSTOM, 3, 3))
    }

    @Test
    fun `a phone offers three to six columns and three to eight rows`() {
        assertEquals(3..6, GridDensity.columnChoices(wide))
        assertEquals(3..8, GridDensity.rowChoices(tall))
        // Every preset's own grid is one that could have been picked by hand.
        GridDensity.entries.forEach { density ->
            val (columns, rows) = density.gridFor(wide, tall)
            assertTrue(density.name, columns in GridDensity.columnChoices(wide))
            assertTrue(density.name, rows in GridDensity.rowChoices(tall))
        }
    }

    @Test
    fun `a bigger screen offers more, and never more than a page can be`() {
        assertEquals(3..11, GridDensity.columnChoices(800f))
        assertEquals(3..10, GridDensity.rowChoices(1280f))
        assertEquals(GridDensity.MAX_COLUMNS, GridDensity.columnChoices(4000f).last)
        assertEquals(GridDensity.MAX_ROWS, GridDensity.rowChoices(4000f).last)
        // And the smallest screen still has a choice to make.
        assertEquals(3..4, GridDensity.columnChoices(200f))
        assertEquals(3..4, GridDensity.rowChoices(300f))
    }

    @Test
    fun `a grid picked on a tablet is kept within what a phone can hold`() {
        assertEquals(6 to 8, grid(GridDensity.CUSTOM, 12, 10))
        assertEquals(12 to 10, grid(GridDensity.CUSTOM, 12, 10, width = 1280f, height = 1280f))
    }

    @Test
    fun `custom with nothing picked yet, or an id nobody knows, is balanced`() {
        assertEquals(grid("balanced"), grid(GridDensity.CUSTOM))
        assertEquals(grid("balanced"), grid(GridDensity.CUSTOM, 5, 0))
        assertEquals(grid("balanced"), grid("origami", 6, 6))
        assertEquals(GridDensity.Balanced, GridDensity.byId(GridDensity.CUSTOM))
    }

    @Test
    fun `before the screen has been measured a picked grid is still a grid`() {
        assertEquals(5 to 6, grid(GridDensity.CUSTOM, 5, 6, width = 0f, height = 0f))
        assertEquals(GridDensity.MAX_COLUMNS to GridDensity.MAX_ROWS, grid(GridDensity.CUSTOM, 99, 99, width = 0f, height = 0f))
        assertEquals(GridDensity.DEFAULT_COLUMNS to GridDensity.DEFAULT_ROWS, grid("balanced", width = 0f, height = 0f))
    }
}
