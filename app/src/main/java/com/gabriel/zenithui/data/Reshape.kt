package com.gabriel.zenithui.data

/**
 * Changing a thing's size or place from the resize bar, a tap at a time, rather than by pulling
 * the pointer or dragging. Both answer with the whole layout as it would be, or null when the
 * page has no room, so a refusal changes nothing and can be shown as one.
 */
object Reshape {

    /**
     * Item [id] at [spanX] columns by [spanY] row units, a size already fitted to what it may be.
     * It stays where it stands when there is room. When there is not it goes to the closest
     * opening on its own page, so asking for Wide with the widget in the last column slides it
     * across rather than refusing; it never changes page. [rowStep] keeps it on whole rows.
     */
    fun resized(
        items: List<HomeItem>,
        id: String,
        spanX: Int,
        spanY: Int,
        columns: Int,
        rows: Int,
        rowStep: Int = ROW_UNITS,
    ): List<HomeItem>? {
        val item = items.firstOrNull { it.id == id } ?: return null
        if (item.spanX == spanX && item.spanY == spanY) return items
        val grid = PageGrid(columns, rows * ROW_UNITS)
        items.filter { it.page == item.page && it.id != id }.forEach { grid.occupyItem(it) }
        val (col, row) = grid.nearestSlot(item.col, item.row, spanX, spanY, rowStep) ?: return null
        return items.map { other ->
            if (other.id != id) return@map other
            when (val moved = other.moved(other.page, col, row)) {
                is WidgetCell -> moved.copy(spanX = spanX, spanY = spanY)
                is AppCell -> moved.copy(spanX = spanX, spanY = spanY)
                // A folder is always one cell: there is no other size to give it.
                is FolderCell -> moved
            }
        }
    }

    /**
     * Item [id] moved [dCol] columns across and [dRow] row units down, either of which may be
     * negative. Null when that would take it off the page or onto something else: a nudge never
     * pushes anything aside.
     */
    fun nudged(items: List<HomeItem>, id: String, dCol: Int, dRow: Int, columns: Int, rows: Int): List<HomeItem>? {
        val item = items.firstOrNull { it.id == id } ?: return null
        if (dCol == 0 && dRow == 0) return items
        val grid = PageGrid(columns, rows * ROW_UNITS)
        items.filter { it.page == item.page && it.id != id }.forEach { grid.occupyItem(it) }
        val col = item.col + dCol
        val row = item.row + dRow
        if (!grid.isFree(col, row, item.spanX, item.spanY)) return null
        return items.map { if (it.id == id) it.moved(it.page, col, row) else it }
    }
}
