package com.gabriel.zenithui.data

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * How the Quick Toggles widget sets out the controls chosen for it at one size, the way Control
 * Center sets out its own: a grid of equal cells, with the same margin all round and the same gap
 * between every two, however many it holds. A control in one cell is a round button, its symbol
 * alone. Where a cell, or two side by side, is wide enough for a name, the first controls are
 * tiles instead, with their names and whether they are on; a tile with room to stand tall is a
 * card. Nothing is made smaller than a finger can press ([MIN_BUTTON]): a widget without room for
 * every control shows the first ones, and in its last cell a button that opens the rest. Sizes are
 * in dp.
 *
 * It was a grid of round buttons with names under them, as many as would go at 30 dp each, which
 * at twelve controls ran the names into one another ("Airplane mode Do Not Disturb") and packed the
 * widget full.
 */
data class ToggleGrid(
    val columns: Int,
    val rows: Int,
    val cellWidth: Float,
    val cellHeight: Float,
    /** The margin round the grid, inside the widget's edge. */
    val pad: Float,
    /** Between every two cells, across and down. */
    val gap: Float,
    /** What stands where, in reading order: the tiles, then the round buttons, then More. */
    val slots: List<Slot>,
    /** How many of the controls chosen there was no room for. The More button stands for them, where there is room for it. */
    val more: Int,
) {
    /** One place on the grid: [control] counts into the controls chosen, and is -1 for More. */
    data class Slot(val control: Int, val row: Int, val column: Int, val span: Int, val kind: Kind)

    enum class Kind {
        /** Round, its symbol alone. */
        Button,

        /** Its symbol, with its name and whether it is on beside it. */
        Tile,

        /** A tile tall enough to have its symbol above its name. */
        Card,

        /** Round, and opens the controls there was no room for. */
        More,
    }

    /** How many of the controls chosen are shown. */
    val shown: Int get() = slots.count { it.kind != Kind.More }

    /** How many of them are shown with their names. */
    val named: Int get() = slots.count { it.kind == Kind.Tile || it.kind == Kind.Card }

    /** How big a round button is drawn: as big as its cell, up to [MAX_BUTTON]. */
    val button: Float get() = min(min(cellWidth, cellHeight), MAX_BUTTON)

    /** How tall a tile is drawn: as tall as its cell, up to [MAX_TILE]. A card fills its cell. */
    val tile: Float get() = min(cellHeight, MAX_TILE)

    /** How wide [slot] is, gaps it spans included. */
    fun width(slot: Slot): Float = slot.span * cellWidth + (slot.span - 1) * gap

    /** Where [slot]'s cells start, from the top left corner inside the margin. */
    fun x(slot: Slot): Float = slot.column * (cellWidth + gap)
    fun y(slot: Slot): Float = slot.row * (cellHeight + gap)

    /**
     * What this layout is, in a line, for the widget gallery: see [WidgetLayouts]. [chosen] is how
     * many controls were chosen.
     */
    fun note(chosen: Int): String {
        val buttons = slots.count { it.kind == Kind.Button }
        val cards = slots.count { it.kind == Kind.Card }
        val how = when {
            named == 0 -> "Round buttons, the symbols alone, as Control Center has them."
            buttons == 0 && cards == named -> "Big tiles, each with its name and whether it is on."
            buttons == 0 -> "Tiles, each with its name and whether it is on."
            named == 1 -> "The first as a tile with its name, the rest as round buttons."
            else -> "The first ${words(named)} as tiles with their names, the rest as round buttons."
        }
        if (more == 0) return how
        val rest = if (slots.any { it.kind == Kind.More }) "; the last button opens the rest." else "."
        return "$how Room for $shown of your $chosen$rest"
    }

    companion object {
        /** No round button is smaller than this: about the least a finger presses without missing. */
        const val MIN_BUTTON = 44f

        /**
         * In a strip, one row of buttons with no more height to give, the least is this: enough for
         * a two-by-one widget to hold four on most phones, as it did before.
         */
        const val MIN_STRIP_BUTTON = 30f

        /** Buttons at least this big are comfortable: past it, room goes to names before size. */
        const val COMFORTABLE = 52f

        /** The biggest a round button gets, about Control Center's. */
        const val MAX_BUTTON = 64f

        /** The tallest a tile gets before it is a card. */
        const val MAX_TILE = 80f

        /** A tile needs this much across: its symbol, and a name beside it. */
        const val TILE_WIDTH = 128f

        /** A tile at least this wide and a cell at least [CARD_HEIGHT] tall make a card. */
        const val CARD_WIDTH = 120f
        const val CARD_HEIGHT = 100f

        /** The margin and gaps of a widget, and of a strip. */
        const val PAD = 14f
        const val GAP = 10f
        const val STRIP_PAD = 9f
        const val STRIP_GAP = 6f

        /** A widget shorter than this is a strip: one row of controls. */
        const val STRIP_BELOW = 100f

        /**
         * [chosen] controls in a widget [width] by [height]. Of every grid that fits, the one that
         * shows the most of them wins; then the one whose buttons are comfortable; then the one
         * that names the most; then bigger buttons; then fewer empty cells.
         */
        fun of(width: Float, height: Float, chosen: Int): ToggleGrid {
            val count = chosen.coerceAtLeast(1)
            val strip = height < STRIP_BELOW
            val pad = if (strip) STRIP_PAD else PAD
            val gap = if (strip) STRIP_GAP else GAP
            val least = if (strip) MIN_STRIP_BUTTON else MIN_BUTTON
            val across = (width - pad * 2).coerceAtLeast(1f)
            val down = (height - pad * 2).coerceAtLeast(1f)
            // A tile takes two cells at most, so more columns than that could use are never tried.
            val mostColumns = floor((across + gap) / (least + gap)).toInt().coerceIn(1, count * 2)
            val mostRows = if (strip) 1 else floor((down + gap) / (least + gap)).toInt().coerceIn(1, count)
            var best: Plan? = null
            for (columns in 1..mostColumns) {
                for (rows in 1..mostRows) {
                    val cellWidth = (across - gap * (columns - 1)) / columns
                    val cellHeight = (down - gap * (rows - 1)) / rows
                    val plan = plan(columns, rows, cellWidth, cellHeight, gap, count) ?: continue
                    if (best == null || plan.beats(best)) best = plan
                }
            }
            // Room for nothing at all at a size a finger can press: one cell, as big as there is.
            val chosenPlan = best ?: plan(1, 1, across, down, gap, count)!!
            return ToggleGrid(
                columns = chosenPlan.columns,
                rows = chosenPlan.rows,
                cellWidth = chosenPlan.cellWidth,
                cellHeight = chosenPlan.cellHeight,
                pad = pad,
                gap = gap,
                slots = chosenPlan.slots,
                more = count - chosenPlan.slots.count { it.kind != Kind.More },
            )
        }

        private class Plan(val columns: Int, val rows: Int, val cellWidth: Float, val cellHeight: Float, val slots: List<Slot>) {
            val shown = slots.count { it.kind != Kind.More }
            val named = slots.count { it.kind == Kind.Tile || it.kind == Kind.Card }

            /** How big its controls are: its buttons' size, or for nothing but tiles, their height. */
            val size = if (slots.any { it.kind == Kind.Button || it.kind == Kind.More }) min(cellWidth, cellHeight) else cellHeight

            fun beats(other: Plan): Boolean {
                val mine = floatArrayOf(shown.toFloat(), min(size, COMFORTABLE), named.toFloat(), min(size, MAX_BUTTON), -(columns * rows).toFloat(), min(cellWidth, cellHeight))
                val theirs = floatArrayOf(other.shown.toFloat(), min(other.size, COMFORTABLE), other.named.toFloat(), min(other.size, MAX_BUTTON), -(other.columns * other.rows).toFloat(), min(other.cellWidth, other.cellHeight))
                for (i in mine.indices) {
                    if (mine[i] > theirs[i] + EVEN) return true
                    if (mine[i] < theirs[i] - EVEN) return false
                }
                return false
            }
        }

        /** Two sizes this close are the same size: a hundredth of a dp is rounding. */
        private const val EVEN = 0.01f

        /**
         * The controls on one grid: as many as there are cells for, less one for More when not all
         * of them fit; and as many of the first as can have tiles while everything still fits.
         * Null when the grid has room for nothing.
         */
        private fun plan(columns: Int, rows: Int, cellWidth: Float, cellHeight: Float, gap: Float, count: Int): Plan? {
            val cells = columns * rows
            if (cells < 1) return null
            val more = count > cells && cells > 1
            val shown = if (more) cells - 1 else min(count, cells)
            // A tile is one cell where that is wide enough for a name, otherwise two side by side.
            val span = when {
                cellWidth >= TILE_WIDTH -> 1
                columns >= 2 && cellWidth * 2 + gap >= TILE_WIDTH -> 2
                else -> 0
            }
            val card = span > 0 && span * cellWidth + (span - 1) * gap >= CARD_WIDTH && cellHeight >= CARD_HEIGHT
            for (named in (if (span == 0) 0 else shown) downTo 0) {
                val slots = place(columns, rows, span, named, shown, more, card) ?: continue
                return Plan(columns, rows, cellWidth, cellHeight, slots)
            }
            return null
        }

        /** Sets the controls out in reading order, a tile never split across two rows. Null if they do not fit. */
        private fun place(columns: Int, rows: Int, span: Int, named: Int, shown: Int, more: Boolean, card: Boolean): List<Slot>? {
            val slots = ArrayList<Slot>(shown + 1)
            var row = 0
            var column = 0
            fun put(control: Int, width: Int, kind: Kind): Boolean {
                if (column + width > columns) {
                    row++
                    column = 0
                }
                if (row >= rows || width > columns) return false
                slots += Slot(control, row, column, width, kind)
                column += width
                return true
            }
            for (control in 0 until shown) {
                val placed = if (control < named) {
                    put(control, span, if (card) Kind.Card else Kind.Tile)
                } else {
                    put(control, 1, Kind.Button)
                }
                if (!placed) return null
            }
            if (more && !put(-1, 1, Kind.More)) return null
            return slots
        }

        private fun words(n: Int): String = when (n) {
            2 -> "two"
            3 -> "three"
            4 -> "four"
            5 -> "five"
            6 -> "six"
            7 -> "seven"
            8 -> "eight"
            9 -> "nine"
            10 -> "ten"
            11 -> "eleven"
            12 -> "twelve"
            else -> n.toString()
        }
    }
}
