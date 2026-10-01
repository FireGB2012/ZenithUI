package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToInt

class HomeGeometryTest {

    /** Every grid a phone can end up with, at three densities, on pages that do not divide evenly. */
    private fun everyPage(check: (HomeGeometry) -> Unit) {
        for (density in listOf(2.0f, 2.625f, 3.5f)) {
            for (columns in 3..6) for (rows in 4..7) for (labels in listOf(true, false)) {
                for (widthDp in listOf(336f, 387.4f, 411f)) for (heightDp in listOf(560f, 657.3f, 720f)) {
                    check(HomeGeometry((widthDp * density).toInt(), (heightDp * density).toInt(), columns, rows, density, labels))
                }
            }
        }
    }

    /** The Pixel 10a's page: 1080 px less the page margins, at 2.625 px per dp, 4 by 5. */
    private val pixel = HomeGeometry(width = 1016, height = 1724, columns = 4, rows = 5, density = 2.625f, showLabels = true)

    @Test
    fun `every cell is the same size and the spare pixels are shared between the edges`() = everyPage { g ->
        assertEquals(g.width / g.columns, g.cellWidth)
        assertEquals(g.height / g.rows, g.rowHeight)
        assertEquals(g.cellWidth * g.columns, g.gridWidth)
        assertEquals(g.rowHeight * g.rows, g.gridHeight)
        assertTrue(g.originX >= 0 && g.width - g.gridWidth - g.originX * 2 in 0..1)
        assertTrue(g.originY >= 0 && g.height - g.gridHeight - g.originY * 2 in 0..1)
        assertEquals(g.rowHeight.toFloat(), g.unitHeight * ROW_UNITS, 0.001f)
    }

    @Test
    fun `an icon sits inside its cell with its label under it`() = everyPage { g ->
        val icon = g.iconFrame
        assertEquals(g.icon, icon.width)
        assertEquals(g.icon, icon.height)
        assertTrue(icon.left >= 0 && icon.right <= g.cellWidth)
        assertTrue(icon.top >= 0 && icon.bottom + g.label <= g.rowHeight)
        assertEquals(g.cellWidth, g.sideInset + g.icon + g.sideInsetEnd)
        assertEquals(g.rowHeight, g.topInset + g.icon + g.label + g.underInset)
    }

    @Test
    fun `a widget runs from icon edge to icon edge`() = everyPage { g ->
        for (spanX in 1..g.columns) {
            val widget = g.widgetFrame(spanX, 2 * ROW_UNITS)
            assertEquals(g.iconFrame.left, widget.left)
            assertEquals((spanX - 1) * g.cellWidth + g.iconFrame.right, widget.right)
        }
    }

    @Test
    fun `a widget runs from the top of its icons to the bottom of its last labels`() = everyPage { g ->
        for (rows in 1..g.rows) {
            val widget = g.widgetFrame(2, rows * ROW_UNITS)
            assertEquals(g.iconFrame.top, widget.top)
            assertEquals((rows - 1) * g.rowHeight + g.iconFrame.bottom + g.label, widget.bottom)
        }
    }

    @Test
    fun `the gap under a widget is the gap under a label`() = everyPage { g ->
        assertEquals(g.underInset, 2 * g.rowHeight - g.widgetFrame(2, 2 * ROW_UNITS).bottom)
    }

    @Test
    fun `with labels off a widget ends where its last icons end`() {
        val g = HomeGeometry(1016, 1724, 4, 5, 2.625f, showLabels = false)
        assertEquals(0, g.label)
        assertEquals(g.rowHeight + g.iconFrame.bottom, g.widgetFrame(2, 2 * ROW_UNITS).bottom)
    }

    @Test
    fun `a widget shorter than a row keeps its whole height`() {
        // Free placement only: a strip half a row tall.
        val strip = pixel.widgetFrame(4, 2)
        assertEquals(0, strip.top)
        assertEquals((2 * pixel.unitHeight).roundToInt(), strip.bottom)
        assertEquals(pixel.iconFrame.left, strip.left)
    }

    @Test
    fun `a super icon spans icon edge to icon edge and one row of it is an icon tall`() = everyPage { g ->
        val wide = g.superIconFrame(spanX = 2, rowsTall = 1)
        assertEquals(g.iconFrame.left, wide.left)
        assertEquals(g.cellWidth + g.iconFrame.right, wide.right)
        assertEquals(g.iconFrame.top, wide.top)
        assertEquals(g.iconFrame.bottom, wide.bottom)
        // Two by two sits between the top of its first icons and the bottom of its last.
        val square = g.superIconFrame(spanX = 2, rowsTall = 2)
        assertTrue(square.top >= g.iconFrame.top && square.bottom <= g.rowHeight + g.iconFrame.bottom)
        assertTrue(square.height <= square.width)
    }

    @Test
    fun `a dock with one app for each column stands under the columns`() = everyPage { g ->
        for (index in 0 until g.columns) {
            assertEquals(index * g.cellWidth + g.iconFrame.left, g.dockIconLeft(index, g.columns))
        }
        assertEquals(0 to g.gridWidth, g.dockPane(g.columns))
    }

    @Test
    fun `a smaller dock is centred on the same spacing`() = everyPage { g ->
        for (count in 1 until g.columns) {
            for (index in 1 until count) {
                assertEquals(g.cellWidth, g.dockIconLeft(index, count) - g.dockIconLeft(index - 1, count))
            }
            val (left, right) = g.dockPane(count)
            assertEquals(count * g.cellWidth, right - left)
            // As far from the left edge as from the right, give or take a pixel of rounding.
            assertTrue(abs(left - (g.gridWidth - right)) <= 1)
            assertEquals(left + g.sideInset, g.dockIconLeft(0, count))
        }
    }

    @Test
    fun `a dock with more apps than columns is spread evenly inside the grid`() = everyPage { g ->
        // Five was the limit before the dock followed the columns, and six leaves a margin.
        for (count in (g.columns + 1)..6) {
            val size = g.dockIcon(count)
            val lefts = (0 until count).map { g.dockIconLeft(it, count) }
            assertTrue("$count apps on ${g.columns} columns start at ${lefts.first()}", lefts.first() >= 0)
            assertTrue("$count apps on ${g.columns} columns end past the grid", lefts.last() + size <= g.gridWidth)
            assertTrue("$count apps on ${g.columns} columns touch", lefts.zipWithNext().all { (a, b) -> b - a > size })
            assertTrue(size in 1..g.icon)
            assertEquals(0 to g.gridWidth, g.dockPane(count))
        }
    }

    @Test
    fun `dock icons are the page's size whenever there is a column for each`() = everyPage { g ->
        for (count in 0..g.columns) assertEquals(g.icon, g.dockIcon(count))
    }

    @Test
    fun `a grid saved with no columns or rows does not divide by nothing`() {
        val broken = HomeGeometry(1016, 1724, columns = 0, rows = 0, density = 2.625f, showLabels = true)
        assertTrue(broken.cellWidth > 0 && broken.rowHeight > 0)
        assertTrue(broken.icon > 0)
    }

    @Test
    fun `the dock is as tall however cramped the rows are`() {
        // Six rows a pixel apart in height. The icons shrink to fit the shorter rows, and a dock
        // that shrank with them would hand the page its pixel back, the icons would grow again,
        // and the two would chase each other on every frame.
        val roomier = HomeGeometry(1016, 1338, columns = 5, rows = 6, density = 2.625f, showLabels = true)
        val tighter = HomeGeometry(1016, 1337, columns = 5, rows = 6, density = 2.625f, showLabels = true)
        assertEquals(139, roomier.icon)
        assertEquals(138, tighter.icon)
        assertEquals(roomier.dockHeight, tighter.dockHeight)
    }

    @Test
    fun `the dock has room for the page's icons and a margin above and below`() = everyPage { g ->
        assertTrue(g.dockHeight >= g.icon + 2 * (12f * g.density).roundToInt())
    }

    @Test
    fun `an empty dock is as wide as the grid`() {
        assertEquals(0 to pixel.gridWidth, pixel.dockPane(0))
    }

    @Test
    fun `the page's left and right lines are the outer icon edges`() = everyPage { g ->
        assertEquals(g.iconFrame.left, g.leftLine)
        assertEquals((g.columns - 1) * g.cellWidth + g.iconFrame.right, g.rightLine)
    }

    @Test
    fun `icons are sized as they always were`() {
        // Three quarters of the cell, held between 54 and 72 dp...
        assertEquals(72f, HomeGeometry.iconSizeDp(96.8f, 131f, showLabels = true), 0.001f)
        assertEquals(58.5f, HomeGeometry.iconSizeDp(78f, 131f, showLabels = true), 0.001f)
        // ...never so tall that the label is pushed out of the row...
        assertEquals(48f, HomeGeometry.iconSizeDp(96.8f, 80f, showLabels = true), 0.001f)
        // ...and with labels off the row gives the icon that room back.
        assertEquals(68f, HomeGeometry.iconSizeDp(96.8f, 80f, showLabels = false), 0.001f)
        assertEquals(189, pixel.icon)
    }
}
