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

        fun byId(id: String): GridDensity = entries.firstOrNull { it.id == id } ?: Balanced

        /** The nearest preset to a grid saved before densities existed. */
        fun fromGrid(columns: Int, rows: Int): GridDensity = when {
            columns >= 5 -> Compact
            rows <= 4 -> Roomy
            else -> Balanced
        }
    }
}
