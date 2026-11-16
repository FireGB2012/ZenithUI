package com.gabriel.zenithui.data

/**
 * Changing a thing's size other than by pulling its corner: one of its named sizes from its menu,
 * in a tap, or a column or a row at a time, for a screen reader. Which things have a corner to pull
 * at all is here too.
 */
object Reshape {

    /** A column or a row more or less: what it is called, and the size it asks for. */
    data class Step(val name: String, val spanX: Int, val spanY: Int)

    /**
     * Whether [item] has a corner to pull while the home screen is edited: every widget does, and
     * an app once it is [chosen] to be resized from its menu. A folder or a shortcut never does:
     * it is always one cell.
     */
    fun hasCorner(item: HomeItem, chosen: Boolean): Boolean = when (item) {
        is WidgetCell, is HostedWidgetCell, is StackCell -> true
        is AppCell -> chosen
        is FolderCell, is ShortcutCell -> false
    }

    /**
     * What [item] can be asked to become without pulling its corner, a column or a whole row at a
     * time, as far as the page on its grid of [columns] by [rows] goes. Whether there is room, and
     * what a widget's design allows, is for the resize itself to say.
     */
    fun steps(item: HomeItem, columns: Int, rows: Int): List<Step> {
        if (item is FolderCell || item is ShortcutCell) return emptyList()
        val unitRows = rows * ROW_UNITS
        return buildList {
            if (item.col + item.spanX < columns) add(Step("Wider", item.spanX + 1, item.spanY))
            if (item.spanX > 1) add(Step("Narrower", item.spanX - 1, item.spanY))
            if (item.row + item.spanY + ROW_UNITS <= unitRows) add(Step("Taller", item.spanX, item.spanY + ROW_UNITS))
            if (item.spanY > ROW_UNITS) add(Step("Shorter", item.spanX, item.spanY - ROW_UNITS))
        }
    }

    /**
     * Gives a thing one of its named sizes. It answers with the whole layout as it would be, or null
     * when the page has no room, so a refusal changes nothing and can be shown as one.
     *
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
                is HostedWidgetCell -> moved.copy(spanX = spanX, spanY = spanY)
                is StackCell -> moved.copy(spanX = spanX, spanY = spanY)
                // A folder or a shortcut is always one cell: there is no other size to give it.
                is FolderCell -> moved
                is ShortcutCell -> moved
            }
        }
    }
}
