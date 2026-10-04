package com.gabriel.zenithui.data

/** One size a widget can be added at: what it is called, and the columns and whole rows it takes. */
data class WidgetSize(val name: String, val spanX: Int, val rows: Int) {
    /** Its height in the row units a layout is stored in. */
    val spanY: Int get() = rows * ROW_UNITS
}

/**
 * The sizes the widget gallery offers. Any widget can be pulled to any size once it is on the
 * page; these are the handful worth showing, because each brings out a different design: every
 * widget is laid out afresh for a strip, a square, a wide, a tall and a large space.
 */
object WidgetSizes {
    const val SMALL = "Small"
    const val MEDIUM = "Medium"
    const val WIDE = "Wide"
    const val TALL = "Tall"
    const val LARGE = "Large"

    /**
     * Small (the least the widget can be), Medium (what it is placed at when nothing is chosen),
     * Wide (the width of the page), Tall (a row more) and Large (both), on a grid of [columns] by
     * [rows]. Each is fitted to what the widget and the grid allow; two that come out the same are
     * offered once, and Medium is always among them.
     */
    fun options(spec: WidgetSpec, columns: Int, rows: Int): List<WidgetSize> {
        val (mediumX, mediumY) = Widgets.defaultSpan(spec, columns, rows)
        val mediumRows = rowsOf(mediumY)
        fun fitted(name: String, spanX: Int, wantRows: Int): WidgetSize {
            val (x, y) = Widgets.clampSpan(spec, spanX, wantRows * ROW_UNITS, columns, rows)
            return WidgetSize(name, x, rowsOf(y))
        }
        val candidates = listOf(
            fitted(SMALL, spec.minSpanX, rowsOf(spec.minSpanY)),
            WidgetSize(MEDIUM, mediumX, mediumRows),
            fitted(WIDE, columns, mediumRows),
            fitted(TALL, mediumX, mediumRows + 1),
            fitted(LARGE, columns, mediumRows + 1),
        )
        // Medium keeps its name whatever else comes out the same size.
        val taken = hashSetOf(mediumX to mediumRows)
        return candidates.filter { it.name == MEDIUM || taken.add(it.spanX to it.rows) }
    }

    /** The one to show first: the size the widget is ordinarily placed at. */
    fun startIndex(options: List<WidgetSize>): Int = options.indexOfFirst { it.name == MEDIUM }.coerceAtLeast(0)

    private fun rowsOf(units: Int): Int = ((units + ROW_UNITS - 1) / ROW_UNITS).coerceAtLeast(1)
}
