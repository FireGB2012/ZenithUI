package com.gabriel.zenithui.data

import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

/**
 * Where a dragged item lands, and what makes room for it — the way the launcher on a phone does it.
 * This follows how Android's own launcher reorders its grid, on Zenith's finer rows.
 *
 * The item goes where it is let go. Anything already there steps aside: pushed along first, taking
 * whatever it bumps into with it; failing that, moved as a group to the nearest opening; failing
 * that, one at a time. Letting go only partly over something, such as an icon a little high under a
 * widget, settles into the opening right there rather than shoving the widget away. When nothing
 * can make room the item takes an opening right beside the spot, or none at all: it is never sent
 * across the page on its own.
 */
object DropPlanner {

    /** A place on a page: a column, and a row counted in row units. */
    data class Spot(val col: Int, val row: Int)

    /** The page something is being let go on. */
    data class Page(
        val columns: Int,
        /** Its height in row units, [ROW_UNITS] to a row. */
        val unitRows: Int,
        /** How many units at the bottom of a widget an icon may tuck into. */
        val tuck: Int,
        /** Plus free placement: any row unit will do, for icons as well. */
        val free: Boolean,
        /** How wide a column is in row units, so that "nearest" means nearest on the glass. */
        val colWidthInRowUnits: Float,
    )

    /**
     * The page [settings] describe, with [freePlacement] when Plus has it on. [colWidthInRowUnits]
     * is measured from the grid on screen; before there is one, a typical phone's will do.
     */
    fun pageFor(settings: ZenithSettings, freePlacement: Boolean, colWidthInRowUnits: Float): Page = Page(
        columns = settings.columns,
        unitRows = settings.rows * ROW_UNITS,
        tuck = settings.widgetTuck,
        free = freePlacement,
        colWidthInRowUnits = colWidthInRowUnits.takeIf { it.isFinite() && it > 0f } ?: 2.9f,
    )

    /** What letting go right now would do. */
    data class Plan(
        /** Where the dragged item lands. */
        val target: Spot,
        /** What moves aside to make room for it, by id; empty when the spot was free already. */
        val moves: Map<String, Spot> = emptyMap(),
        /** It lands right where it was let go, rather than on an opening beside that. */
        val exact: Boolean = true,
    )

    /**
     * What letting [dragged] go on [items] with its top left corner at [exactCol], [exactRow] would
     * do, or null when it can go nowhere near there. The corner is in columns and row units and may
     * hang off the page. [dragged] may already be on the page or be arriving from somewhere else.
     */
    fun plan(items: List<HomeItem>, dragged: HomeItem, exactCol: Float, exactRow: Float, page: Page): Plan? {
        if (dragged.spanX > page.columns || dragged.spanY > page.unitRows) return null
        val others = items.filter { it.id != dragged.id }.map { Box.of(it, page.tuck) }
        val height = LayoutBuilder.blockedSpanY(dragged, page.tuck)
        val col = exactCol.roundToInt().coerceIn(0, page.columns - dragged.spanX)
        val rows = (0..(page.unitRows - dragged.spanY)).filter { canTake(dragged, col, it, others, page) }
        fun fits(row: Int) = others.none { it.overlaps(col, row, dragged.spanX, height) }
        fun cost(row: Int) = abs(row - exactRow) + misfit(dragged, col, row, others, page)

        val row = rows.minByOrNull(::cost) ?: return null
        val target = Spot(col, row)
        if (fits(row)) return Plan(target)

        // Let go only partly over something, less than half its height into it, with an opening in
        // the same column: that opening is what was meant.
        rows.filter { abs(it - exactRow) <= dragged.spanY / 2f && misfit(dragged, col, it, others, page) <= 0f && fits(it) }
            .minByOrNull(::cost)
            ?.let { return Plan(Spot(col, it), exact = false) }

        makeRoom(dragged, target, height, exactCol, exactRow, others, page)?.let { return it }

        // Nothing can make room. An opening right beside the spot will do; one across the page would
        // look like the item flying off on its own, so then there is nothing.
        return opening(dragged, exactCol, exactRow, others, page)
            ?.takeIf { abs(it.col - exactCol) <= NEAR_COLUMNS && abs(it.row - exactRow) <= NEAR_ROWS * ROW_UNITS }
            ?.let { Plan(it, exact = false) }
    }

    /**
     * The free spot nearest to where [dragged] was let go, anywhere on the page, or null when the
     * page has no room for it at all. For an item carried onto another page, which has to land on it.
     */
    fun opening(items: List<HomeItem>, dragged: HomeItem, exactCol: Float, exactRow: Float, page: Page): Spot? {
        if (dragged.spanX > page.columns || dragged.spanY > page.unitRows) return null
        val others = items.filter { it.id != dragged.id }.map { Box.of(it, page.tuck) }
        return opening(dragged, exactCol, exactRow, others, page)
    }

    private fun opening(dragged: HomeItem, exactCol: Float, exactRow: Float, others: List<Box>, page: Page): Spot? {
        val height = LayoutBuilder.blockedSpanY(dragged, page.tuck)
        var best: Spot? = null
        var bestCost = Float.MAX_VALUE
        for (r in 0..(page.unitRows - dragged.spanY)) {
            for (c in 0..(page.columns - dragged.spanX)) {
                if (!canTake(dragged, c, r, others, page)) continue
                if (others.any { it.overlaps(c, r, dragged.spanX, height) }) continue
                val cost = hypot((c - exactCol) * page.colWidthInRowUnits, r - exactRow) +
                    misfit(dragged, c, r, others, page).coerceAtLeast(0f)
                if (cost < bestCost) {
                    bestCost = cost
                    best = Spot(c, r)
                }
            }
        }
        return best
    }

    // ------------------------------------------------------------------ rows

    private fun isIcon(item: HomeItem) = item !is WidgetCell

    /**
     * Whether [item] may sit on [row] at [col]. Widgets may sit on any row unit. Icons keep to whole
     * rows, to a row something else on the page already sits on, or snug under whatever is directly
     * above them: the band under a widget, a slim search bar, an icon that is tucked in itself.
     */
    private fun canTake(item: HomeItem, col: Int, row: Int, others: List<Box>, page: Page): Boolean =
        page.free || !isIcon(item) || linesUp(item, col, row, others)

    private fun linesUp(item: HomeItem, col: Int, row: Int, others: List<Box>): Boolean =
        row % ROW_UNITS == 0 || others.any { it.row == row } ||
            others.any { it.bottom == row && it.col < col + item.spanX && col < it.right }

    /**
     * What sitting on [row] costs beyond its distance from where the item was let go. An icon out of
     * line with an icon right beside it costs a lot. A widget that lines up with the rows around it
     * gains a little, so that one let go close to a row settles onto it.
     */
    private fun misfit(item: HomeItem, col: Int, row: Int, others: List<Box>, page: Page): Float = when {
        page.free -> 0f
        isIcon(item) -> if (outOfLine(item, col, row, others)) OUT_OF_LINE else 0f
        linesUp(item, col, row, others) -> -WIDGET_PULL
        else -> 0f
    }

    /** An icon right beside the spot whose rows it would cut across instead of sharing. */
    private fun outOfLine(item: HomeItem, col: Int, row: Int, others: List<Box>): Boolean =
        others.any {
            isIcon(it.item) && (it.right == col || it.col == col + item.spanX) &&
                it.row < row + item.spanY && row < it.row + it.spanY &&
                (row - it.row) % ROW_UNITS != 0
        }

    // ------------------------------------------------------------------ making room

    private fun makeRoom(
        dragged: HomeItem,
        target: Spot,
        height: Int,
        exactCol: Float,
        exactRow: Float,
        others: List<Box>,
        page: Page,
    ): Plan? {
        val drop = Box(dragged.id, dragged, target.col, target.row, height)
        val inTheWay = others.filter { it.overlaps(drop) }.map { it.id }.toSet()
        if (inTheWay.isEmpty()) return Plan(target)
        val direction = direction(dragged, exactCol, exactRow, others.filter { it.id in inTheWay }, page)

        val ways: List<(List<Box>, List<Box>) -> Boolean> =
            pushOrder(direction).map { way -> { all: List<Box>, hits: List<Box> -> push(all, hits, drop, way) } } +
                listOf(
                    { all, hits -> moveTogether(all, hits, drop, direction, page) },
                    { all, hits -> moveApart(all, hits, drop, direction, page) },
                )
        for (way in ways) {
            val trial = others.map { it.copy() }
            val hits = trial.filter { it.id in inTheWay }.sortedWith(compareBy({ it.col }, { it.row }))
            if (way(trial, hits) && sound(trial, drop, others, page)) {
                val moves = trial.zip(others)
                    .filter { (now, was) -> now.col != was.col || now.row != was.row }
                    .associate { (now, _) -> now.id to Spot(now.col, now.row) }
                return Plan(target, moves)
            }
        }
        return null
    }

    /** One of eight directions on the grid. */
    private data class Way(val x: Int, val y: Int)

    /**
     * Which way what is in the spot gets pushed: away from the item, from its middle towards the
     * middle of what it covers. Measured on the glass, and scaled by the item's own size, as
     * Launcher3 does, so a wide item pushes sideways and a tall one up or down.
     */
    private fun direction(dragged: HomeItem, exactCol: Float, exactRow: Float, hits: List<Box>, page: Page): Way {
        val left = hits.minOf { it.col }
        val right = hits.maxOf { it.right }
        val top = hits.minOf { it.row }
        val bottom = hits.maxOf { it.row + it.spanY }
        var dx = ((left + right) / 2f - exactCol - dragged.spanX / 2f) * page.colWidthInRowUnits / dragged.spanX
        var dy = ((top + bottom) / 2f - exactRow - dragged.spanY / 2f) / (dragged.spanY.toFloat() / ROW_UNITS)
        if (right - left >= page.columns || dragged.spanX >= page.columns) dx = 0f
        if (bottom - top >= page.unitRows || dragged.spanY >= page.unitRows) dy = 0f
        return if (dx == 0f && dy == 0f) Way(1, 0) else toward(dx, dy)
    }

    private fun toward(dx: Float, dy: Float): Way {
        val angle = atan(dy / dx)
        return Way(
            if (abs(cos(angle)) > 0.5f) sign(dx).toInt() else 0,
            if (abs(sin(angle)) > 0.5f) sign(dy).toInt() else 0,
        )
    }

    /** The ways to try pushing, in Launcher3's order: the way it points, then back, then across. */
    private fun pushOrder(d: Way): List<Way> =
        if (d.x != 0 && d.y != 0) {
            listOf(Way(d.x, 0), Way(0, d.y), Way(-d.x, 0), Way(0, -d.y))
        } else {
            listOf(d, Way(-d.x, -d.y), Way(d.y, d.x), Way(-d.y, -d.x))
        }

    /**
     * Pushes what is in the way one unit at a time until the spot is clear, taking along everything
     * it runs into, the way a row of icons shuffles along on a phone. Whether that pushed anything
     * off the page is for [sound] to say.
     */
    private fun push(all: List<Box>, hits: List<Box>, drop: Box, way: Way): Boolean {
        val moving = hits.toMutableList()
        var distance = when {
            way.x < 0 -> moving.maxOf { it.right } - drop.col
            way.x > 0 -> drop.right - moving.minOf { it.col }
            way.y < 0 -> moving.maxOf { it.bottom } - drop.row
            else -> drop.bottom - moving.minOf { it.row }
        }
        if (distance <= 0) return false
        while (distance-- > 0) {
            do {
                val met = all.filter { box -> box !in moving && moving.any { it.meets(box, way) } }
                moving += met
            } while (met.isNotEmpty())
            moving.forEach {
                it.col += way.x
                it.row += way.y
            }
        }
        return true
    }

    /**
     * Moves what is in the way, all together and in the same arrangement, to the nearest opening
     * that takes it. Icons move by whole rows, so they stay in line with the icons they left.
     */
    private fun moveTogether(all: List<Box>, hits: List<Box>, drop: Box, direction: Way, page: Page): Boolean {
        val rest = all.filter { it !in hits }
        val left = hits.minOf { it.col }
        val top = hits.minOf { it.row }
        val right = hits.maxOf { it.right }
        val bottom = hits.maxOf { it.row + it.spanY }
        val step = if (page.free || hits.none { isIcon(it.item) }) 1 else ROW_UNITS
        var best: Way? = null
        var bestCost = Float.MAX_VALUE
        var bestLean = Int.MIN_VALUE
        for (dy in -top..(page.unitRows - bottom)) {
            if (dy % step != 0) continue
            for (dx in -left..(page.columns - right)) {
                if (dx == 0 && dy == 0) continue
                val clear = hits.all { h ->
                    val c = h.col + dx
                    val r = h.row + dy
                    !drop.overlaps(c, r, h.spanX, h.height) && rest.none { it.overlaps(c, r, h.spanX, h.height) }
                }
                if (!clear) continue
                val cost = hypot(dx * page.colWidthInRowUnits, dy.toFloat())
                val lean = lean(direction, dx * page.colWidthInRowUnits, dy.toFloat())
                if (cost < bestCost || (cost == bestCost && lean > bestLean)) {
                    best = Way(dx, dy)
                    bestCost = cost
                    bestLean = lean
                }
            }
        }
        val move = best ?: return false
        hits.forEach {
            it.col += move.x
            it.row += move.y
        }
        return true
    }

    /** Moves each thing in the way, the biggest first, to the opening nearest to it. */
    private fun moveApart(all: List<Box>, hits: List<Box>, drop: Box, direction: Way, page: Page): Boolean {
        for (h in hits.sortedByDescending { it.spanX * it.spanY }) {
            val around = all.filter { it !== h } + drop
            var best: Spot? = null
            var bestCost = Float.MAX_VALUE
            var bestLean = Int.MIN_VALUE
            for (r in 0..(page.unitRows - h.spanY)) {
                for (c in 0..(page.columns - h.spanX)) {
                    if (around.any { it.overlaps(c, r, h.spanX, h.height) }) continue
                    if (!canTake(h.item, c, r, around, page)) continue
                    val dx = (c - h.col) * page.colWidthInRowUnits
                    val dy = (r - h.row).toFloat()
                    val cost = hypot(dx, dy) + misfit(h.item, c, r, around, page).coerceAtLeast(0f)
                    val lean = lean(direction, dx, dy)
                    if (cost < bestCost || (cost == bestCost && lean > bestLean)) {
                        best = Spot(c, r)
                        bestCost = cost
                        bestLean = lean
                    }
                }
            }
            val spot = best ?: return false
            h.col = spot.col
            h.row = spot.row
        }
        return true
    }

    /** How well a move goes the way things are being pushed; only ever breaks a tie. */
    private fun lean(direction: Way, dx: Float, dy: Float): Int {
        if (dx == 0f && dy == 0f) return 0
        val way = toward(dx, dy)
        return direction.x * way.x + direction.y * way.y
    }

    /**
     * Whether a way of making room left a page that works: nothing on top of anything, nothing off
     * the page, and every icon that moved on a row an icon may take — or moved by whole rows, and so
     * still in line with wherever it was.
     */
    private fun sound(trial: List<Box>, drop: Box, before: List<Box>, page: Page): Boolean {
        if (trial.any { !it.onPage(page) || it.overlaps(drop) }) return false
        for (i in trial.indices) {
            for (j in i + 1 until trial.size) if (trial[i].overlaps(trial[j])) return false
        }
        return trial.zip(before).all { (now, was) ->
            (now.row - was.row) % ROW_UNITS == 0 ||
                canTake(now.item, now.col, now.row, trial.filter { it !== now } + drop, page)
        }
    }

    /** An item's place while a way of making room is tried out. [height] is how much of it blocks. */
    private class Box(val id: String, val item: HomeItem, var col: Int, var row: Int, val height: Int) {
        val spanX: Int get() = item.spanX
        val spanY: Int get() = item.spanY
        val right: Int get() = col + spanX
        val bottom: Int get() = row + height

        fun overlaps(c: Int, r: Int, w: Int, h: Int): Boolean = c < right && col < c + w && r < bottom && row < r + h

        fun overlaps(other: Box): Boolean = overlaps(other.col, other.row, other.spanX, other.height)

        fun onPage(page: Page): Boolean = col >= 0 && row >= 0 && right <= page.columns && row + spanY <= page.unitRows

        /** This box's leading edge, going [way], is up against [other]. */
        fun meets(other: Box, way: Way): Boolean = when {
            way.x > 0 -> right == other.col && row < other.bottom && other.row < bottom
            way.x < 0 -> col == other.right && row < other.bottom && other.row < bottom
            way.y > 0 -> bottom == other.row && col < other.right && other.col < right
            else -> row == other.bottom && col < other.right && other.col < right
        }

        fun copy(): Box = Box(id, item, col, row, height)

        companion object {
            fun of(item: HomeItem, tuck: Int): Box = Box(item.id, item, item.col, item.row, LayoutBuilder.blockedSpanY(item, tuck))
        }
    }

    /** What an icon out of line with the icon beside it costs, as row units of distance. */
    private const val OUT_OF_LINE = 3f

    /** How much further a widget reaches for a row that lines up with the page, in row units. */
    private const val WIDGET_PULL = 1f

    /** How close an opening has to be to stand in for the spot itself: in columns, and in rows. */
    private const val NEAR_COLUMNS = 1.25f
    private const val NEAR_ROWS = 1.25f
}
