package com.gabriel.zenithui.data

import androidx.compose.runtime.Immutable
import kotlin.math.abs
import kotlin.math.min

/*
 * The widget maker's rules for moving pieces with a finger: what they line up with, where they are
 * drawn from their points, and undo. Plain arithmetic in whatever unit the stage is measured in,
 * so it is tried in tests.
 */

/** A piece's box on the stage: its left and top edges, and its size. */
@Immutable
data class Box(val left: Float, val top: Float, val width: Float, val height: Float) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
    val middleX: Float get() = left + width / 2f
    val middleY: Float get() = top + height / 2f

    fun moved(dx: Float, dy: Float): Box = copy(left = left + dx, top = top + dy)
}

/** Where a moved piece comes to rest: how far it is pulled to line up, and the lines it lines up on, if any. */
@Immutable
data class Snapped(val dx: Float, val dy: Float, val lineX: Float? = null, val lineY: Float? = null) {
    val lined: Boolean get() = lineX != null || lineY != null
}

object MakerSnap {
    /**
     * Where [moving] is pulled to, within [within] of a line, on a widget [width] by [height]. Its
     * edges and middle line up with the widget's middle; its edges with the [margin] round the
     * widget's edges; and its edges and middle with the edges and middles of the [others]. Across
     * and down are lined up apart, and the nearest line wins.
     */
    fun snap(moving: Box, others: List<Box>, width: Float, height: Float, margin: Float, within: Float): Snapped {
        val (dx, lineX) = nearest(
            lines = Triple(moving.left, moving.middleX, moving.right),
            middle = width / 2f,
            start = margin,
            end = width - margin,
            others = others.map { Triple(it.left, it.middleX, it.right) },
            within = within,
        )
        val (dy, lineY) = nearest(
            lines = Triple(moving.top, moving.middleY, moving.bottom),
            middle = height / 2f,
            start = margin,
            end = height - margin,
            others = others.map { Triple(it.top, it.middleY, it.bottom) },
            within = within,
        )
        return Snapped(dx, dy, lineX, lineY)
    }

    /**
     * Along one way: how far the nearest line is, and where it is. [lines] are the moving box's
     * start, middle and end; [start] and [end] the margins, which only the box's own start and end
     * line up with.
     */
    private fun nearest(
        lines: Triple<Float, Float, Float>,
        middle: Float,
        start: Float,
        end: Float,
        others: List<Triple<Float, Float, Float>>,
        within: Float,
    ): Pair<Float, Float?> {
        var best = Float.MAX_VALUE
        var at: Float? = null
        fun consider(line: Float, target: Float) {
            val d = target - line
            if (abs(d) <= within && abs(d) < abs(best)) {
                best = d
                at = target
            }
        }
        val (first, mid, last) = lines
        // The widget's middle first, so that among lines as near, it is the one shown.
        consider(mid, middle)
        consider(first, middle)
        consider(last, middle)
        consider(first, start)
        consider(last, end)
        for ((a, b, c) in others) {
            for (line in listOf(mid, first, last)) {
                consider(line, b)
                consider(line, a)
                consider(line, c)
            }
        }
        return if (at == null) 0f to null else best to at
    }
}

/** Where a piece is drawn from its point, and how its point follows it when it is moved or pulled. */
object PiecePlace {
    /** The left edge of a piece [width] wide whose point is at [pointX]. */
    fun left(pointX: Float, width: Float, align: PieceAlign): Float = when (align) {
        PieceAlign.START -> pointX
        PieceAlign.CENTER -> pointX - width / 2f
        PieceAlign.END -> pointX - width
    }

    /** The top edge of a piece [height] tall whose point is at [pointY]: it is always by its middle. */
    fun top(pointY: Float, height: Float): Float = pointY - height / 2f

    /** The point of a piece whose box starts at [left], lined up by [align]. */
    private fun pointOf(left: Float, width: Float, align: PieceAlign): Float = when (align) {
        PieceAlign.START -> left
        PieceAlign.CENTER -> left + width / 2f
        PieceAlign.END -> left + width
    }

    /** The point that keeps a piece where it is when it is lined up by [to] in place of [from]. */
    fun repoint(pointX: Float, width: Float, from: PieceAlign, to: PieceAlign): Float =
        pointOf(left(pointX, width, from), width, to)

    /**
     * The point of a piece moved so its box starts at [left], on a widget [room] wide: its middle
     * stays on the widget, and so does its point, so no piece is ever lost off an edge.
     */
    fun pointX(left: Float, width: Float, align: PieceAlign, room: Float): Float {
        val middle = (left + width / 2f).coerceIn(0f, room)
        return pointOf(middle - width / 2f, width, align).coerceIn(0f, room)
    }

    /** The point of a piece moved so its box starts at [top], on a widget [room] tall. */
    fun pointY(top: Float, height: Float, room: Float): Float = (top + height / 2f).coerceIn(0f, room)

    /**
     * How much a piece's box grows, across and down, when its corner is pulled by [dx] and [dy]:
     * as much as keeps the corner under the finger. It grows from its point, so round its middle
     * by twice the pull, and down always round its middle.
     */
    fun stretch(align: PieceAlign, dx: Float, dy: Float): Pair<Float, Float> = when (align) {
        PieceAlign.START -> dx to 2f * dy
        PieceAlign.CENTER -> 2f * dx to 2f * dy
        PieceAlign.END -> -dx to 2f * dy
    }

    /** Whether the corner to pull is on the left: for a piece that ends at its point, which grows to the left. */
    fun cornerOnLeft(align: PieceAlign): Boolean = align == PieceAlign.END

    /** How many times bigger words become when their box, [width] by [height], grows by [dw] and [dh]. */
    fun growth(width: Float, height: Float, dw: Float, dh: Float): Float {
        val was = width + height
        if (was <= 0f) return 1f
        return ((width + dw) + (height + dh)) / was
    }

    /** How much words [width] wide are drawn smaller to fit [room]: not at all when they fit, or before they are measured. */
    fun fit(width: Float, room: Float): Float = if (width <= 0f || room <= 0f) 1f else min(1f, room / width)
}

/**
 * Undo and redo in the maker. Each step keeps the whole widget as it was before it; a change made
 * under the same key as the one before it, the rest of one drag or of one slider's pull, joins that
 * step rather than making one of its own, until [settled].
 */
@Immutable
data class MakerHistory(
    val now: MadeWidget,
    val back: List<MadeWidget> = emptyList(),
    val ahead: List<MadeWidget> = emptyList(),
    val lastKey: String? = null,
) {
    val canUndo: Boolean get() = back.isNotEmpty()
    val canRedo: Boolean get() = ahead.isNotEmpty()

    fun changed(next: MadeWidget, key: String? = null): MakerHistory = when {
        next == now -> this
        key != null && key == lastKey -> copy(now = next, ahead = emptyList())
        else -> copy(now = next, back = (back + now).takeLast(MOST), ahead = emptyList(), lastKey = key)
    }

    /** The step under way is over: the next change is a step of its own, whatever its key. */
    fun settled(): MakerHistory = if (lastKey == null) this else copy(lastKey = null)

    fun undone(): MakerHistory =
        if (back.isEmpty()) this else copy(now = back.last(), back = back.dropLast(1), ahead = listOf(now) + ahead, lastKey = null)

    fun redone(): MakerHistory =
        if (ahead.isEmpty()) this else copy(now = ahead.first(), back = back + now, ahead = ahead.drop(1), lastKey = null)

    companion object {
        /** As many steps as can be taken back. */
        const val MOST = 60
    }
}
