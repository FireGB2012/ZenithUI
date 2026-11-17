package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class FolderPourTest {

    /** A Galaxy A55: 2.625 pixels to the dp, a folder in the second row, the pane in the middle. */
    private val unit = 2.625f
    private val size = FolderSize.of(411f)
    private val g = FolderPourGeometry(
        tileLeft = 130f * unit, tileTop = 190f * unit, tileWidth = 68f * unit, tileHeight = 68f * unit, tileRadius = 16f * unit,
        paneLeft = 45f * unit, paneTop = size.paneTop(891f) * unit, paneWidth = size.width * unit, paneHeight = size.height * unit,
        paneRadius = size.corner * unit,
    )

    private fun frames(until: Float = FolderPour.IN_S, step: Float = 0.004f) =
        generateSequence(0f) { it + step }.takeWhile { it <= until }.map { it to FolderPour.frameIn(it, g) }.toList()

    @Test
    fun `the drop starts as the tile, exactly where it is and as round`() {
        val first = FolderPour.frameIn(0f, g)
        assertEquals(g.tileLeft, first.left, 0.01f)
        assertEquals(g.tileTop, first.top, 0.01f)
        assertEquals(g.tileWidth, first.width, 0.01f)
        assertEquals(g.tileHeight, first.height, 0.01f)
        assertEquals(g.tileRadius, first.radius, 0.01f)
        assertEquals(1f, first.alpha, 0f)
    }

    @Test
    fun `once its clock has run it is the pane, to within half a pixel`() {
        val last = FolderPour.frameIn(FolderPour.IN_S, g)
        assertEquals(g.paneLeft, last.left, 0.5f)
        assertEquals(g.paneTop, last.top, 0.5f)
        assertEquals(g.paneWidth, last.width, 0.5f)
        assertEquals(g.paneHeight, last.height, 0.5f)
        assertEquals(g.paneRadius, last.radius, 0.5f)
    }

    @Test
    fun `it opens wide first and tall after`() {
        val early = FolderPour.frameIn(0.05f, g)
        val across = (early.width - g.tileWidth) / (g.paneWidth - g.tileWidth)
        val down = (early.height - g.tileHeight) / (g.paneHeight - g.tileHeight)
        assertTrue("across $across, down $down", across > down + 0.1f)
    }

    @Test
    fun `it sloshes once and settles, never a wobble`() {
        val all = frames()
        val widest = all.maxOf { it.second.width }
        val tallest = all.maxOf { it.second.height }
        // A little past, once: about 4%, and never more than 6%.
        assertTrue("width past by ${widest / g.paneWidth - 1f}", widest / g.paneWidth - 1f in 0.02f..0.06f)
        assertTrue("height past by ${tallest / g.paneHeight - 1f}", tallest / g.paneHeight - 1f in 0.02f..0.06f)
        // Any swing back after that is too small to see: under a pixel and a half on a 320 dp pane.
        val afterPeak = all.dropWhile { it.second.width < widest }
        val narrowest = afterPeak.minOf { it.second.width }
        assertTrue("swung back by ${g.paneWidth - narrowest} px", g.paneWidth - narrowest < 1.5f)
    }

    @Test
    fun `it never shrinks below the tile on the way, nor turns inside out`() {
        for ((t, frame) in frames()) {
            assertTrue("width at $t", frame.width >= g.tileWidth - 0.01f)
            assertTrue("height at $t", frame.height >= g.tileHeight - 0.01f)
            assertTrue("corners at $t", frame.radius <= minOf(frame.width, frame.height) / 2f + 0.01f)
            assertTrue("corners at $t", frame.radius >= 0f)
        }
    }

    @Test
    fun `its middle travels from the tile's to the pane's`() {
        val first = FolderPour.frameIn(0f, g)
        val last = FolderPour.frameIn(FolderPour.IN_S, g)
        assertEquals(g.tileLeft + g.tileWidth / 2f, first.left + first.width / 2f, 0.01f)
        assertEquals(g.paneLeft + g.paneWidth / 2f, last.left + last.width / 2f, 0.5f)
        assertEquals(g.paneTop + g.paneHeight / 2f, last.top + last.height / 2f, 0.5f)
    }

    @Test
    fun `what is in it zooms with it, from the tile's size to its own`() {
        assertEquals(g.tileWidth / g.paneWidth, FolderPour.zoom(FolderPour.frameIn(0f, g), g), 0.001f)
        assertEquals(1f, FolderPour.zoom(FolderPour.frameIn(FolderPour.IN_S, g), g), 0.002f)
    }

    @Test
    fun `names come in once the apps are most of their size`() {
        assertEquals(0f, FolderPour.namesShown(0.5f), 0f)
        assertEquals(0f, FolderPour.namesShown(0.7f), 0f)
        assertEquals(1f, FolderPour.namesShown(1f), 0f)
        var last = 0f
        var zoom = 0f
        while (zoom <= 1.05f) {
            val shown = FolderPour.namesShown(zoom)
            assertTrue(shown >= last)
            last = shown
            zoom += 0.01f
        }
    }

    @Test
    fun `closing goes from wherever it is back into the tile, with no overshoot`() {
        val from = FolderPour.frameIn(0.12f, g)
        val start = FolderPour.frameOut(0f, from, g)
        assertEquals(from.left, start.left, 0.01f)
        assertEquals(from.width, start.width, 0.01f)
        assertEquals(from.height, start.height, 0.01f)
        val end = FolderPour.frameOut(1f, from, g)
        assertEquals(g.tileLeft, end.left, 0.01f)
        assertEquals(g.tileTop, end.top, 0.01f)
        assertEquals(g.tileWidth, end.width, 0.01f)
        assertEquals(g.tileHeight, end.height, 0.01f)
        assertEquals(g.tileRadius, end.radius, 0.01f)
        val rest = FolderPour.frameIn(FolderPour.IN_S, g)
        var last = rest.width
        var fraction = 0f
        while (fraction <= 1f) {
            val frame = FolderPour.frameOut(fraction, rest, g)
            assertTrue("at $fraction", frame.width <= last + 0.01f)
            assertTrue("at $fraction", frame.width >= g.tileWidth - 0.01f)
            last = frame.width
            fraction += 0.01f
        }
    }

    @Test
    fun `it is slow off the pane and slow onto the tile`() {
        val rest = FolderPour.frameIn(FolderPour.IN_S, g)
        fun gone(fraction: Float) = (rest.width - FolderPour.frameOut(fraction, rest, g).width) / (rest.width - g.tileWidth)
        assertTrue(gone(0.1f) < 0.05f)
        assertTrue(1f - gone(0.9f) < 0.05f)
        assertEquals(0.5f, gone(0.5f), 0.01f)
    }

    @Test
    fun `the drop fades onto the tile only at the very end`() {
        val rest = FolderPour.frameIn(FolderPour.IN_S, g)
        val fadeFrom = 1f - FolderPour.FADE_MS.toFloat() / FolderPour.OUT_MS
        assertEquals(1f, FolderPour.frameOut(fadeFrom - 0.01f, rest, g).alpha, 0f)
        assertTrue(FolderPour.frameOut(fadeFrom + 0.1f, rest, g).alpha in 0.01f..0.99f)
        assertEquals(0f, FolderPour.frameOut(1f, rest, g).alpha, 0f)
    }

    @Test
    fun `the tile is away while the folder is out of it, and comes back only as the drop fades onto it`() {
        val fadeFrom = 1f - FolderPour.FADE_MS.toFloat() / FolderPour.OUT_MS
        assertEquals(0f, FolderPour.tileShown(0f), 0f)
        assertEquals(0f, FolderPour.tileShown(fadeFrom - 0.01f), 0f)
        assertEquals(1f, FolderPour.tileShown(1f), 0f)
        val rest = FolderPour.frameIn(FolderPour.IN_S, g)
        var fraction = 0f
        while (fraction <= 1f) {
            // The drop and the tile under it always make one between them: never both, never neither.
            assertEquals("at $fraction", 1f, FolderPour.tileShown(fraction) + FolderPour.frameOut(fraction, rest, g).alpha, 0.0001f)
            fraction += 0.01f
        }
    }

    @Test
    fun `the tile's name under it fades as the drop leaves, rather than going at once`() {
        assertEquals(1f, FolderPour.nameLeaving(0f), 0f)
        assertEquals(0f, FolderPour.nameLeaving(FolderPour.NAME_OUT_S), 0f)
        assertEquals(0f, FolderPour.nameLeaving(FolderPour.IN_S), 0f)
        // Gone well before the drop is still, and only ever going.
        assertTrue(FolderPour.NAME_OUT_S <= FolderPour.IN_S / 4f)
        var last = 1f
        var t = 0f
        while (t <= FolderPour.IN_S) {
            val now = FolderPour.nameLeaving(t)
            assertTrue("at $t", now <= last)
            last = now
            t += 0.004f
        }
    }

    @Test
    fun `the apps come out nearest the finger first`() {
        val xs = floatArrayOf(10f, 20f, 30f, 10f, 20f, 30f)
        val ys = floatArrayOf(10f, 10f, 10f, 20f, 20f, 20f)
        assertEquals(listOf(5, 2, 4, 1, 3, 0), FolderPour.order(xs, ys, 32f, 21f))
        // Level with each other, they keep their order.
        assertEquals(listOf(0, 1, 2, 3, 4, 5), FolderPour.order(floatArrayOf(5f, 5f, 5f, 5f, 5f, 5f), FloatArray(6), 5f, 0f))
    }

    @Test
    fun `each app comes out after the one before, and all are out by the time the pane is`() {
        for (rank in 0..12) {
            assertEquals(0f, FolderPour.pop(0f, rank), 0f)
            assertEquals("rank $rank", 1f, FolderPour.pop(FolderPour.IN_S, rank), 0.01f)
        }
        assertTrue(FolderPour.pop(0.08f, 0) > FolderPour.pop(0.08f, 3))
        // The ninth and any after it set off together.
        assertEquals(FolderPour.pop(0.2f, 8), FolderPour.pop(0.2f, 30), 0f)
        // A small bounce at most.
        val most = generateSequence(0f) { it + 0.004f }.takeWhile { it < FolderPour.IN_S }.maxOf { FolderPour.pop(it, 0) }
        assertTrue("pops past by ${most - 1f}", most - 1f <= 0.06f)
    }

    /** Where app [slot] is drawn on screen with the drop at [frame], [out] of the way into its place: its middle and its side. */
    private fun drawn(frame: PourFrame, slot: Int, out: Float): Triple<Float, Float, Float> {
        val z = FolderPour.zoom(frame, g)
        val flight = FolderPour.flight(out, slot, frame, g, size, unit)
        val placeX = (size.iconLeft(FolderGrid.column(slot)) + size.icon / 2f) * unit - g.paneWidth / 2f
        val placeY = (size.iconTop(FolderGrid.row(slot)) + size.icon / 2f) * unit - g.paneHeight / 2f
        return Triple(
            frame.left + frame.width / 2f + (placeX + flight.dx) * z,
            frame.top + frame.height / 2f + (placeY + flight.dy) * z,
            size.icon * unit * z * flight.scale,
        )
    }

    /** Where the tile's tiny icon [slot] is on screen: its middle and its side. */
    private fun mini(slot: Int): Triple<Float, Float, Float> {
        val step = FolderPour.MINI_ICON + FolderPour.MINI_GAP
        val side = FolderPour.MINI_ICON * g.tileWidth
        return Triple(
            g.tileLeft + (FolderPour.MINI_EDGE + FolderGrid.column(slot) * step) * g.tileWidth + side / 2f,
            g.tileTop + (FolderPour.MINI_EDGE + FolderGrid.row(slot) * step) * g.tileHeight + side / 2f,
            side,
        )
    }

    @Test
    fun `each app starts as the tile's tiny icon, exactly where it is and as big`() {
        val start = FolderPour.frameIn(0f, g)
        for (slot in 0 until FolderGrid.PER_PAGE) {
            val (x, y, side) = drawn(start, slot, 0f)
            val (miniX, miniY, miniSide) = mini(slot)
            assertEquals("slot $slot across", miniX, x, 0.01f)
            assertEquals("slot $slot down", miniY, y, 0.01f)
            assertEquals("slot $slot size", miniSide, side, 0.01f)
        }
    }

    @Test
    fun `once out, each app is in its own place at its own size`() {
        for (t in listOf(0.05f, 0.12f, FolderPour.IN_S)) {
            val frame = FolderPour.frameIn(t, g)
            for (slot in 0 until FolderGrid.PER_PAGE) {
                val flight = FolderPour.flight(1f, slot, frame, g, size, unit)
                assertEquals(0f, flight.dx, 0.001f)
                assertEquals(0f, flight.dy, 0.001f)
                assertEquals(1f, flight.scale, 0.001f)
            }
        }
    }

    @Test
    fun `an app still in the tile rides inside the drop as it spreads`() {
        for ((t, frame) in frames()) {
            for (slot in 0 until FolderGrid.PER_PAGE) {
                val (x, y, side) = drawn(frame, slot, 0f)
                assertTrue("slot $slot at $t", x - side / 2f >= frame.left - 0.5f && x + side / 2f <= frame.left + frame.width + 0.5f)
                assertTrue("slot $slot at $t", y - side / 2f >= frame.top - 0.5f && y + side / 2f <= frame.top + frame.height + 0.5f)
            }
        }
    }

    @Test
    fun `closing takes each app back into the tile's tiny icon as the drop lands`() {
        assertEquals(0f, FolderPour.closing(0f), 0f)
        assertEquals(1f, FolderPour.closing(1f), 0f)
        assertEquals(0.5f, FolderPour.closing(0.5f), 0.001f)
        var last = 0f
        var fraction = 0f
        while (fraction <= 1f) {
            assertTrue(FolderPour.closing(fraction) >= last)
            last = FolderPour.closing(fraction)
            fraction += 0.01f
        }
        // Landed, with nothing of the way out left, each app is the tile's tiny icon again.
        val landed = FolderPour.frameOut(1f, FolderPour.frameIn(FolderPour.IN_S, g), g)
        for (slot in 0 until FolderGrid.PER_PAGE) {
            val (x, y, side) = drawn(landed, slot, 1f - FolderPour.closing(1f))
            val (miniX, miniY, miniSide) = mini(slot)
            assertEquals(miniX, x, 0.01f)
            assertEquals(miniY, y, 0.01f)
            assertEquals(miniSide, side, 0.01f)
        }
    }

    @Test
    fun `the name above comes in from a little small and goes before the drop does`() {
        assertEquals(0f, FolderPour.titleAlpha(0f), 0f)
        assertEquals(1f, FolderPour.titleAlpha(0.3f), 0f)
        assertEquals(FolderPour.TITLE_FROM, FolderPour.titleScale(0f), 0f)
        assertEquals(1f, FolderPour.titleScale(FolderPour.IN_S), 0.01f)
        assertEquals(1f, FolderPour.wordsLeaving(0f), 0f)
        assertEquals(0f, FolderPour.wordsLeaving(FolderPour.WORDS_OUT), 0f)
        assertEquals(0f, FolderPour.wordsLeaving(1f), 0f)
    }

    @Test
    fun `the tile gulps it down, wide and flat, then a little tall, then still`() {
        assertEquals(1f to 1f, FolderPour.gulp(0f))
        assertEquals(1f to 1f, FolderPour.gulp(1f))
        val (wide, flat) = FolderPour.gulp(1f / 3f)
        assertTrue(wide > 1.05f && flat < 0.95f)
        val (narrow, tall) = FolderPour.gulp(2f / 3f)
        assertTrue(narrow < 1f && tall > 1f)
        var fraction = 0f
        while (fraction <= 1f) {
            val (x, y) = FolderPour.gulp(fraction)
            assertTrue(abs(x - 1f) <= 0.08f && abs(y - 1f) <= 0.08f)
            fraction += 0.01f
        }
    }
}
