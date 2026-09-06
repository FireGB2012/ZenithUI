package com.gabriel.zenithui.data

import java.util.UUID

/** Occupancy map for one home page. */
class PageGrid(private val columns: Int, private val rows: Int) {

    private val taken = Array(rows) { BooleanArray(columns) }

    /** [rowStep] keeps icons on tidy row boundaries while widgets may sit on any unit. */
    fun findSlot(spanX: Int, spanY: Int, rowStep: Int = 1): Pair<Int, Int>? {
        if (spanX > columns || spanY > rows) return null
        var row = 0
        while (row <= rows - spanY) {
            for (col in 0..columns - spanX) {
                if (isFree(col, row, spanX, spanY)) return col to row
            }
            row += rowStep
        }
        return null
    }

    fun isFree(col: Int, row: Int, spanX: Int, spanY: Int): Boolean {
        if (col < 0 || row < 0 || col + spanX > columns || row + spanY > rows) return false
        for (r in row until row + spanY) {
            for (c in col until col + spanX) if (taken[r][c]) return false
        }
        return true
    }

    fun occupy(col: Int, row: Int, spanX: Int, spanY: Int) {
        for (r in row until (row + spanY).coerceAtMost(rows)) {
            for (c in col until (col + spanX).coerceAtMost(columns)) taken[r][c] = true
        }
    }

    fun place(spanX: Int, spanY: Int, rowStep: Int = 1): Pair<Int, Int>? =
        findSlot(spanX, spanY, rowStep)?.also { (c, r) -> occupy(c, r, spanX, spanY) }
}

/**
 * Turns "these are your apps, these are the widgets you picked" into a finished home screen.
 * This is the work the setup screen is actually doing while its progress ring fills.
 */
object LayoutBuilder {

    fun build(
        repository: AppRepository,
        apps: List<AppInfo>,
        widgetTypes: List<String>,
        columns: Int,
        rows: Int,
        dock: List<String>,
    ): List<HomeItem> {
        val items = mutableListOf<HomeItem>()
        val page = PageGrid(columns, rows * ROW_UNITS)

        // The widgets you asked for, sized for the grid you chose.
        widgetTypes.mapNotNull { Widgets.byType(it) }.forEach { spec ->
            val (spanX, spanY) = Widgets.defaultSpan(spec, columns, rows)
            val slot = page.place(spanX, spanY, rowStep = ICON_ROW_STEP) ?: return@forEach
            items += WidgetCell(newId(), 0, slot.first, slot.second, spec.type, spanX, spanY)
        }

        // Then only the essentials. Everything else stays one swipe away in the drawer.
        val essentials = repository.essentialApps(exclude = dock)
        for (key in essentials) {
            val app = apps.firstOrNull { it.key == key } ?: continue
            val slot = page.place(1, ROW_UNITS, rowStep = ICON_ROW_STEP) ?: break
            items += AppCell(newId(), 0, slot.first, slot.second, app.packageName, app.className)
        }

        return items
    }

    /** Icons land on half-row boundaries, so rows stay tidy without wasting a slim widget's space. */
    const val ICON_ROW_STEP = ROW_UNITS / 2

    /** Re-flows an existing layout when the grid size changes so nothing falls off the page. */
    fun reflow(items: List<HomeItem>, columns: Int, rows: Int): List<HomeItem> {
        val grids = HashMap<Int, PageGrid>()
        val result = mutableListOf<HomeItem>()
        val overflow = mutableListOf<HomeItem>()

        fun grid(page: Int) = grids.getOrPut(page) { PageGrid(columns, rows * ROW_UNITS) }

        for (item in items.sortedWith(compareBy({ it.page }, { it.row }, { it.col }))) {
            val spanX = item.spanX.coerceAtMost(columns)
            val spanY = item.spanY.coerceAtMost(rows * ROW_UNITS)
            val g = grid(item.page)
            if (g.isFree(item.col, item.row, spanX, spanY)) {
                g.occupy(item.col, item.row, spanX, spanY)
                result += item
            } else {
                val slot = g.findSlot(spanX, spanY, ICON_ROW_STEP)
                if (slot != null) {
                    g.occupy(slot.first, slot.second, spanX, spanY)
                    result += item.moved(item.page, slot.first, slot.second)
                } else {
                    overflow += item
                }
            }
        }

        var page = (result.maxOfOrNull { it.page } ?: 0)
        for (item in overflow) {
            val spanX = item.spanX.coerceAtMost(columns)
            val spanY = item.spanY.coerceAtMost(rows * ROW_UNITS)
            var slot = grid(page).findSlot(spanX, spanY, ICON_ROW_STEP)
            while (slot == null && page < 8) {
                page += 1
                slot = grid(page).findSlot(spanX, spanY, ICON_ROW_STEP)
            }
            if (slot == null) continue
            grid(page).occupy(slot.first, slot.second, spanX, spanY)
            result += item.moved(page, slot.first, slot.second)
        }
        return result
    }

    fun newId(): String = UUID.randomUUID().toString().take(8)

    /** Layouts saved before the grid gained sub-rows measured everything in whole rows. */
    fun upgradeToRowUnits(items: List<HomeItem>): List<HomeItem> = items.map { item ->
        when (item) {
            is WidgetCell -> item.copy(row = item.row * ROW_UNITS, spanY = item.spanY * ROW_UNITS)
            is AppCell -> item.copy(row = item.row * ROW_UNITS)
            is FolderCell -> item.copy(row = item.row * ROW_UNITS)
        }
    }
}
