package com.gabriel.zenithui.data

import kotlin.math.roundToInt

/**
 * How large a cell — and so an icon or a widget — wants to be.
 *
 * Zenith deliberately does not store a column count. A phone and a tablet asked for the same
 * "Balanced" grid should get cells of the same size, not the same *number* of them, so the counts
 * are worked out from the screen each time it changes. On a phone these reproduce the grids Zenith
 * has always used; on a tablet they stop four columns being stretched across the whole slab.
 */
enum class GridDensity(
    val id: String,
    val label: String,
    /** Target cell size, in dp. */
    private val cellWidth: Float,
    private val cellHeight: Float,
) {
    Compact("compact", "Compact", 82f, 154f),
    Balanced("balanced", "Balanced", 103f, 185f),
    Roomy("roomy", "Roomy", 110f, 231f);

    /** The columns and rows that fit a [widthDp] x [heightDp] screen at this density. */
    fun gridFor(widthDp: Float, heightDp: Float): Pair<Int, Int> {
        if (widthDp <= 0f || heightDp <= 0f) return DEFAULT_COLUMNS to DEFAULT_ROWS
        return (widthDp / cellWidth).roundToInt().coerceIn(MIN_COLUMNS, MAX_COLUMNS) to
            (heightDp / cellHeight).roundToInt().coerceIn(MIN_ROWS, MAX_ROWS)
    }

    companion object {
        const val MIN_COLUMNS = 3
        const val MAX_COLUMNS = 16
        const val MIN_ROWS = 3
        const val MAX_ROWS = 10

        const val DEFAULT_COLUMNS = 4
        const val DEFAULT_ROWS = 5

        /** The reference grid every widget's declared span is drawn against. */
        const val BASE_COLUMNS = 4

        /**
         * Not a density at all: the columns and rows are the ones the person picked. Stored where
         * a density's id is, so a Zenith from before there was a choice reads it as Balanced.
         */
        const val CUSTOM = "custom"

        /** The narrowest a cell may be made by hand and still hold an icon and most of its name. */
        private const val CUSTOM_MIN_CELL_WIDTH = 72f

        /** The shortest a row may be made by hand, measured against the whole screen as the presets are. */
        private const val CUSTOM_MIN_ROW_HEIGHT = 118f

        /** How many columns can be asked for on a screen [widthDp] wide: three to six on a phone. */
        fun columnChoices(widthDp: Float): IntRange =
            MIN_COLUMNS..(widthDp / CUSTOM_MIN_CELL_WIDTH).toInt().coerceIn(MIN_COLUMNS + 1, MAX_COLUMNS)

        /** How many rows can be asked for on a screen [heightDp] tall: three to eight on a phone. */
        fun rowChoices(heightDp: Float): IntRange =
            MIN_ROWS..(heightDp / CUSTOM_MIN_ROW_HEIGHT).toInt().coerceIn(MIN_ROWS + 1, MAX_ROWS)

        /**
         * The columns and rows for what is saved, on this screen. A preset is worked out from the
         * screen. A grid chosen by hand is that grid, kept within what this screen can hold, so
         * one picked on an unfolded tablet does not arrive on a phone as twelve columns; with
         * nothing picked yet it is Balanced.
         */
        fun resolve(id: String, customColumns: Int, customRows: Int, widthDp: Float, heightDp: Float): Pair<Int, Int> {
            if (id != CUSTOM || customColumns <= 0 || customRows <= 0) return byId(id).gridFor(widthDp, heightDp)
            if (widthDp <= 0f || heightDp <= 0f) {
                return customColumns.coerceIn(MIN_COLUMNS, MAX_COLUMNS) to customRows.coerceIn(MIN_ROWS, MAX_ROWS)
            }
            val columns = columnChoices(widthDp)
            val rows = rowChoices(heightDp)
            return customColumns.coerceIn(columns.first, columns.last) to customRows.coerceIn(rows.first, rows.last)
        }

        fun byId(id: String): GridDensity = entries.firstOrNull { it.id == id } ?: Balanced

        /** The nearest preset to a grid saved before densities existed. */
        fun fromGrid(columns: Int, rows: Int): GridDensity = when {
            columns >= 5 -> Compact
            rows <= 4 -> Roomy
            else -> Balanced
        }
    }
}
