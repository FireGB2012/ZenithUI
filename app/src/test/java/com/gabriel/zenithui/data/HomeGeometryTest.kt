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

    /**
     * The Pixel 10a on the iPhone grid, 4 by 6, at 2.625 px per dp: a page as wide as its 1080 px
     * (411 dp) screen, and as tall as its 2424 px (923 dp) less the 230 dp, 604 px, that the
     * status bar, the Search band and the dock take.
     */
    private val four = HomeGeometry(width = 1080, height = 1820, columns = 4, rows = 6, density = 2.625f, showLabels = true)

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
    fun `a widget runs from the top of its first icons to the bottom of its last icons`() = everyPage { g ->
        for (rows in 1..g.rows) {
            val widget = g.widgetFrame(2, rows * ROW_UNITS)
            assertEquals(g.iconFrame.top, widget.top)
            assertEquals((rows - 1) * g.rowHeight + g.iconFrame.bottom, widget.bottom)
        }
    }

    @Test
    fun `a widget's name sits in the strip under its last icons, as wide as the widget`() = everyPage { g ->
        for (spanX in 1..g.columns) for (rows in 1..g.rows) {
            val widget = g.widgetFrame(spanX, rows * ROW_UNITS)
            val name = g.widgetNameStrip(spanX, rows * ROW_UNITS)
            // Straight under the widget and as wide as it...
            assertEquals(widget.left, name.left)
            assertEquals(widget.right, name.right)
            assertEquals(widget.bottom, name.top)
            // ...in the strip the names of the icons beside it are written in, with the same gap
            // under it as under theirs.
            assertEquals((rows - 1) * g.rowHeight + g.iconFrame.bottom, name.top)
            assertEquals(g.label, name.height)
            assertEquals(g.underInset, rows * g.rowHeight - name.bottom)
            assertEquals(g.underWidget, rows * g.rowHeight - widget.bottom)
        }
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
        // That leaves no room under it for a name.
        assertEquals(0, pixel.widgetNameStrip(4, 2).height)
    }

    @Test
    fun `a 2x2 widget on the four grid is within a tenth of square`() {
        // A row on the Pixel 10a's iPhone grid is about 1.12 times a column, as an iPhone's is...
        assertEquals(1.12f, four.rowHeight.toFloat() / four.cellWidth, 0.01f)
        // ...so a widget two icons across and two down comes out nearly square.
        val small = four.widgetFrame(2, 2 * ROW_UNITS)
        assertEquals(1f, small.height.toFloat() / small.width, 0.1f)
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
    }

    @Test
    fun `a smaller dock shares the whole bar out equally between its apps`() = everyPage { g ->
        for (count in 1 until g.columns) {
            val lefts = (0 until count).map { g.dockIconLeft(it, count) }
            // The bar is the grid's width whatever it holds: each app stands in the middle of
            // its share of that, give or take a pixel of rounding.
            lefts.forEachIndexed { index, left ->
                assertEquals((index + 0.5f) * g.gridWidth / count, left + g.icon / 2f, 1f)
            }
            // So they are as far from the left edge as from the right...
            assertTrue(abs(lefts.first() - (g.gridWidth - lefts.last() - g.icon)) <= 1)
            // ...and further apart than the columns above them, not huddled on the same spacing.
            lefts.zipWithNext().forEach { (a, b) -> assertTrue("$count apps on ${g.columns} columns", b - a > g.cellWidth) }
        }
    }

    @Test
    fun `a single app stands in the middle of the bar`() = everyPage { g ->
        assertTrue(abs(g.dockIconLeft(0, 1) + g.icon / 2f - g.gridWidth / 2f) <= 1f)
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
    fun `the floating dock has room for the biggest icon, and the Search band for its pill`() {
        // Neither follows the page, so they are sized for the biggest icon a page can have. A dock
        // that shrank with icons shrinking to fit cramped rows would hand the page more room, the
        // icons would grow again, and the two would chase each other on every frame.
        val biggest = HomeGeometry.iconSizeDp(Float.MAX_VALUE, Float.MAX_VALUE, showLabels = true)
        assertEquals(68f, biggest, 0f)
        assertTrue(HomeGeometry.DOCK_HEIGHT_DP >= biggest + 2 * 12f)
        assertTrue(HomeGeometry.DOCK_CORNER_DP <= HomeGeometry.DOCK_HEIGHT_DP / 2)
        assertTrue(HomeGeometry.SEARCH_PILL_HEIGHT_DP < HomeGeometry.SEARCH_BAND_DP)
    }

    /** Three phones: the Pixel 10a, a small one and a large one, each screen's width and density. */
    private val screens = listOf(1080 to 2.625f, 720 to 2f, 1440 to 3.5f)

    @Test
    fun `the glass dock spans the screen less its insets`() {
        for ((screen, density) in screens) {
            val icon = (65.8f * density).roundToInt()
            val inset = (10f * density).roundToInt()
            // However many apps it holds, as on an iPhone.
            for (icons in 0..6) {
                assertEquals(screen - 2 * inset, HomeGeometry.dockSlabWidth("glass", icons, screen, icon, density))
            }
        }
    }

    @Test
    fun `a floating dock hugs fewer than four icons`() {
        for ((screen, density) in screens) {
            val icon = (65.8f * density).roundToInt()
            val glass = HomeGeometry.dockSlabWidth("glass", 4, screen, icon, density)
            fun floating(icons: Int) = HomeGeometry.dockSlabWidth("floating", icons, screen, icon, density)
            // With fewer than four it closes up round them, and grows with each one more...
            assertTrue(floating(2) < floating(4))
            assertTrue(floating(1) < floating(2) && floating(2) < floating(3) && floating(3) < floating(4))
            // ...and it is never wider than the glass dock. A full one is the glass dock, and so is an
            // empty one, which needs the room for the words asking for an app.
            for (icons in 0..6) assertTrue(floating(icons) <= glass)
            assertEquals(glass, floating(4))
            assertEquals(glass, floating(0))
            // Icons only draws no pane: this is the row its icons stand in, the glass dock's.
            for (icons in 0..6) assertEquals(glass, HomeGeometry.dockSlabWidth("none", icons, screen, icon, density))
        }
    }

    @Test
    fun `a dock across the screen stands its apps under the page's columns`() = everyPage { g ->
        // The home screen and its previews centre the page on the screen, 12 dp in from each side.
        val screen = g.width + 2 * (12f * g.density).roundToInt()
        val pageLeft = (screen - g.width) / 2 + g.originX
        // Floating closes up round fewer than four, so it only spans the screen with four or more.
        val styles = if (g.columns >= HomeGeometry.DOCK_APPS) listOf("glass", "none", "floating") else listOf("glass", "none")
        for (style in styles) {
            // One for each column: on screen, each is under the icons above it...
            for (index in 0 until g.columns) {
                assertEquals(pageLeft + index * g.cellWidth + g.iconFrame.left, g.dockIconX(style, index, g.columns, screen))
            }
        }
        // ...and with fewer, the glass dock and Icons only share the page's width out between them as before.
        for (count in 1 until g.columns) for (index in 0 until count) {
            assertEquals(pageLeft + g.dockIconLeft(index, count), g.dockIconX("glass", index, count, screen))
            assertEquals(pageLeft + g.dockIconLeft(index, count), g.dockIconX("none", index, count, screen))
        }
    }

    @Test
    fun `a floating dock closes up round fewer than four apps, with an even margin all round`() {
        // The Pixel 10a's iPhone grid as the home screen lays it out, 32 px in from each side.
        val g = HomeGeometry(width = 1080 - 2 * 32, height = 1820, columns = 4, rows = 6, density = 2.625f, showLabels = true)
        val margin = ((HomeGeometry.DOCK_HEIGHT_DP * g.density).roundToInt() - g.icon) / 2
        val gaps = HashSet<Int>()
        for (count in 1..3) {
            val slab = HomeGeometry.dockSlabWidth("floating", count, 1080, g.dockIcon(count), g.density)
            val slabLeft = (1080 - slab) / 2
            val lefts = (0 until count).map { g.dockIconX("floating", it, count, 1080) }
            // Its ends are as far from its apps as its top and bottom are, give or take a pixel...
            assertEquals(margin.toFloat(), (lefts.first() - slabLeft).toFloat(), 1f)
            assertEquals(margin.toFloat(), (slabLeft + slab - lefts.last() - g.icon).toFloat(), 1f)
            // ...and its apps stand side by side, never touching, as far apart however many there are.
            lefts.zipWithNext().forEach { (a, b) ->
                assertTrue(b - a > g.icon)
                gaps += b - a
            }
        }
        assertEquals(1, gaps.size)
        // A full floating dock is the glass dock, its apps under the columns.
        for (index in 0 until 4) assertEquals(g.dockIconX("glass", index, 4, 1080), g.dockIconX("floating", index, 4, 1080))
    }

    @Test
    fun `the page's left and right lines are the outer icon edges`() = everyPage { g ->
        assertEquals(g.iconFrame.left, g.leftLine)
        assertEquals((g.columns - 1) * g.cellWidth + g.iconFrame.right, g.rightLine)
    }

    @Test
    fun `icons are about two thirds of a cell, between 52 and 68 dp`() {
        // As on an iPhone, 0.64 of a column: 65.8 dp of the Pixel 10a's 102.9 dp columns...
        assertEquals(0.64f * four.cellWidth / four.density, four.iconDp, 0.001f)
        assertEquals(173, four.icon)
        // ...and never less than 52 dp or more than 68...
        assertEquals(52f, HomeGeometry.iconSizeDp(70f, 131f, showLabels = true), 0.001f)
        assertEquals(68f, HomeGeometry.iconSizeDp(120f, 180f, showLabels = true), 0.001f)
        // ...never so tall that the label is pushed out of the row, where with labels off the row
        // gives the icon that room back...
        assertEquals(48f, HomeGeometry.iconSizeDp(96.8f, 80f, showLabels = true), 0.001f)
        assertEquals(0.64f * 96.8f, HomeGeometry.iconSizeDp(96.8f, 80f, showLabels = false), 0.001f)
        // ...and never wider than its cell less a margin.
        assertEquals(48f, HomeGeometry.iconSizeDp(56f, 131f, showLabels = true), 0.001f)
    }
}
