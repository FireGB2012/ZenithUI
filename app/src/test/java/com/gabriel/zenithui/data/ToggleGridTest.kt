package com.gabriel.zenithui.data

import com.gabriel.zenithui.data.ToggleGrid.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToggleGridTest {

    // The frames of the gallery's sizes on the iPhone grid of a phone 412 dp across, in dp.
    private val small = 164f to 61f
    private val medium = 164f to 164.5f
    private val wide = 354f to 164.5f
    private val tall = 164f to 268f
    private val large = 354f to 268f

    private fun grid(size: Pair<Float, Float>, chosen: Int) = ToggleGrid.of(size.first, size.second, chosen)

    private fun kinds(grid: ToggleGrid) = grid.slots.map { it.kind }

    @Test
    fun `four in a two by two are four big round buttons, as Control Center has them`() {
        val grid = grid(medium, 4)
        assertEquals(2, grid.columns)
        assertEquals(2, grid.rows)
        assertEquals(List(4) { Kind.Button }, kinds(grid))
        assertTrue("buttons are ${grid.button}", grid.button >= 60f)
        assertEquals(0, grid.more)
    }

    @Test
    fun `with room to spare the first controls get tiles with their names`() {
        // Three in a two by two: the first across the top, the other two as buttons under it.
        val three = grid(medium, 3)
        assertEquals(listOf(Kind.Tile, Kind.Button, Kind.Button), kinds(three))
        assertEquals(2, three.slots[0].span)
        // Two: a tile each, the width of the widget.
        assertEquals(listOf(Kind.Tile, Kind.Tile), kinds(grid(medium, 2)))
        // One alone has the whole widget: a card.
        assertEquals(listOf(Kind.Card), kinds(grid(medium, 1)))
    }

    @Test
    fun `a wide widget sets four out as tiles, and more as tiles over a row of buttons`() {
        assertEquals(List(4) { Kind.Tile }, kinds(grid(wide, 4)))
        assertEquals(listOf(Kind.Tile, Kind.Tile, Kind.Tile, Kind.Button, Kind.Button), kinds(grid(wide, 5)))
        val six = grid(wide, 6)
        assertEquals(listOf(Kind.Tile, Kind.Tile) + List(4) { Kind.Button }, kinds(six))
        assertTrue(six.slots.take(2).all { it.row == 0 } && six.slots.drop(2).all { it.row == 1 })
        assertEquals(List(8) { Kind.Button }, kinds(grid(wide, 8)))
        // Two have half the widget each, tall enough to be cards.
        assertEquals(listOf(Kind.Card, Kind.Card), kinds(grid(wide, 2)))
    }

    @Test
    fun `twelve in a large widget are four tiles over eight buttons`() {
        val grid = grid(large, 12)
        assertEquals(12, grid.shown)
        assertEquals(4, grid.named)
        assertEquals(0, grid.more)
        assertTrue("buttons are ${grid.button}", grid.button >= ToggleGrid.COMFORTABLE)
    }

    @Test
    fun `four in a tall widget are a list of tiles`() {
        val grid = grid(tall, 4)
        assertEquals(1, grid.columns)
        assertEquals(List(4) { Kind.Tile }, kinds(grid))
    }

    @Test
    fun `a widget without room for everything shows the first few and a button for the rest`() {
        val grid = grid(medium, 8)
        assertEquals(3, grid.shown)
        assertEquals(5, grid.more)
        assertEquals(Kind.More, grid.slots.last().kind)
        assertEquals(listOf(0, 1, 2), grid.slots.filter { it.kind != Kind.More }.map { it.control })
        // A strip: four buttons in a row hold the four it starts with, and twelve need More.
        assertEquals(List(4) { Kind.Button }, kinds(grid(small, 4)))
        val strip = grid(small, 12)
        assertEquals(3, strip.shown)
        assertEquals(9, strip.more)
    }

    @Test
    fun `a two by one strip holds four on a phone about 390 dp across or wider`() {
        // 158 dp is a two-by-one widget's width on a phone about 390 dp across, 164 on 412.
        for (width in listOf(158f, 164f, 171f)) {
            for (height in listOf(58f, 61f, 63f)) {
                val grid = ToggleGrid.of(width, height, 4)
                assertEquals("$width x $height", 4, grid.shown)
                assertEquals("$width x $height", 0, grid.more)
            }
        }
    }

    @Test
    fun `the controls keep their order, tiles then buttons then More, in reading order`() {
        for (size in listOf(small, medium, wide, tall, large)) {
            for (chosen in 1..12) {
                val grid = grid(size, chosen)
                val shownOrder = grid.slots.filter { it.kind != Kind.More }.map { it.control }
                assertEquals("$size $chosen", (0 until grid.shown).toList(), shownOrder)
                val places = grid.slots.map { it.row * 100 + it.column }
                assertEquals("$size $chosen reads in order", places.sorted(), places)
                val firstButton = grid.slots.indexOfFirst { it.kind == Kind.Button || it.kind == Kind.More }
                if (firstButton >= 0) assertFalse(grid.slots.drop(firstButton).any { it.kind == Kind.Tile || it.kind == Kind.Card })
            }
        }
    }

    @Test
    fun `nothing overlaps, nothing leaves the grid, and every chosen control is shown or counted`() {
        for (width in listOf(150f, 171f, 200f, 260f, 353f, 400f)) {
            for (height in listOf(56f, 63f, 90f, 120f, 171f, 230f, 280f, 388f)) {
                for (chosen in 1..12) {
                    val grid = ToggleGrid.of(width, height, chosen)
                    val taken = HashSet<Pair<Int, Int>>()
                    for (slot in grid.slots) {
                        assertTrue(slot.row in 0 until grid.rows)
                        assertTrue(slot.column >= 0 && slot.column + slot.span <= grid.columns)
                        for (c in slot.column until slot.column + slot.span) assertTrue("overlap", taken.add(slot.row to c))
                    }
                    assertEquals("$width x $height, $chosen", chosen, grid.shown + grid.more)
                    assertTrue(grid.shown >= 1)
                }
            }
        }
    }

    @Test
    fun `a button is never smaller than a finger, nor a strip's smaller than its least`() {
        for (width in listOf(171f, 260f, 353f)) {
            for (height in listOf(63f, 90f, 171f, 280f, 388f)) {
                for (chosen in 1..12) {
                    val grid = ToggleGrid.of(width, height, chosen)
                    val least = if (height < ToggleGrid.STRIP_BELOW) ToggleGrid.MIN_STRIP_BUTTON else ToggleGrid.MIN_BUTTON
                    if (grid.slots.any { it.kind == Kind.Button || it.kind == Kind.More }) {
                        assertTrue("$width x $height, $chosen: ${grid.button}", grid.button >= least - 0.01f)
                    }
                    // A tile always has room for its symbol and a name beside it.
                    for (slot in grid.slots.filter { it.kind == Kind.Tile }) {
                        assertTrue(grid.width(slot) >= ToggleGrid.TILE_WIDTH - 0.01f)
                    }
                }
            }
        }
    }

    @Test
    fun `the margin and the gaps are the same whatever the widget holds`() {
        for (chosen in 1..12) {
            val grid = grid(large, chosen)
            assertEquals(ToggleGrid.PAD, grid.pad, 0f)
            assertEquals(ToggleGrid.GAP, grid.gap, 0f)
        }
        assertEquals(ToggleGrid.STRIP_PAD, grid(small, 4).pad, 0f)
    }

    @Test
    fun `the cells fill the room inside the margin exactly`() {
        for (size in listOf(small, medium, wide, tall, large)) {
            for (chosen in 1..12) {
                val grid = grid(size, chosen)
                val across = grid.columns * grid.cellWidth + (grid.columns - 1) * grid.gap
                val down = grid.rows * grid.cellHeight + (grid.rows - 1) * grid.gap
                assertEquals(size.first - grid.pad * 2, across, 0.01f)
                assertEquals(size.second - grid.pad * 2, down, 0.01f)
            }
        }
    }

    @Test
    fun `never none at all, however small`() {
        val tiny = ToggleGrid.of(40f, 40f, 4)
        assertEquals(1, tiny.shown)
        // No room for a button for the rest either: they are only counted.
        assertEquals(3, tiny.more)
        assertFalse(tiny.slots.any { it.kind == Kind.More })
    }
}
