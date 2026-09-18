package com.gabriel.zenithui.data

import java.util.UUID
import kotlin.math.roundToInt

/** Marks [item] as taken, less any empty band at the bottom it can give up (see [LayoutBuilder.blockedSpanY]). */
fun PageGrid.occupyItem(item: HomeItem, tuck: Int) =
    occupy(item.col, item.row, item.spanX, LayoutBuilder.blockedSpanY(item, tuck))

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

    /**
     * The opening closest to [col], [row], or null when nothing that size fits. A column counts as
     * two row units of distance — roughly the shape of a cell — so "closest" matches what you see.
     */
    fun nearestSlot(col: Int, row: Int, spanX: Int, spanY: Int, rowStep: Int = 1): Pair<Int, Int>? {
        if (isFree(col, row, spanX, spanY)) return col to row
        var best: Pair<Int, Int>? = null
        var bestDistance = Int.MAX_VALUE
        var r = 0
        while (r <= rows - spanY) {
            for (c in 0..columns - spanX) {
                if (!isFree(c, r, spanX, spanY)) continue
                val dx = (c - col) * 2
                val dy = r - row
                val distance = dx * dx + dy * dy
                if (distance < bestDistance) {
                    bestDistance = distance
                    best = c to r
                }
            }
            r += rowStep
        }
        return best
    }
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

    /**
     * Whether [item] is drawn cut to the icon lines with an empty band under it — the strip where
     * the labels of the icons beside it sit. The slim search bar is not cut that way.
     */
    fun leavesBand(item: HomeItem): Boolean =
        item is WidgetCell && item.type != "search" && item.spanY >= ROW_UNITS

    /**
     * How many row units [item] really blocks: all of them, except that a widget gives up the
     * bottom [tuck] units of that empty band, so an icon can sit snug under it instead of a whole
     * label's height away. Items collide by what you can see, not the box around it.
     */
    fun blockedSpanY(item: HomeItem, tuck: Int): Int =
        if (leavesBand(item)) (item.spanY - tuck).coerceAtLeast(ICON_ROW_STEP) else item.spanY

    /** The same for a widget of [spanY] units that is being moved or resized. */
    fun blockedSpanY(widgetType: String, spanY: Int, tuck: Int): Int =
        if (widgetType != "search" && spanY >= ROW_UNITS) (spanY - tuck).coerceAtLeast(ICON_ROW_STEP) else spanY

    /**
     * The rows directly under a widget's blocked part: where an icon tucked in under it sits.
     * They are allowed for icons even when they are not on the usual half-row steps.
     */
    fun tuckRows(items: List<HomeItem>, tuck: Int): Set<Int> =
        if (tuck <= 0) emptySet() else items.filter(::leavesBand).map { it.row + blockedSpanY(it, tuck) }.toSet()

    /** As many home pages as anyone sensibly needs, and few enough that the dots still fit. */
    const val MAX_PAGES = 8

    /** Icons land on half-row boundaries, so rows stay tidy without wasting a slim widget's space. */
    const val ICON_ROW_STEP = ROW_UNITS / 2

    /**
     * Puts every item somewhere it may be on a [columns] by [rows] grid, moving as little as it
     * can. Items are taken from the top of each page down, so whatever sits higher keeps its place
     * and anything overlapping it moves to the closest opening — not the first gap on the page,
     * which is what used to shuffle icons around.
     *
     * With [free] false it also undoes free placement: an offset folds into the nearest cell, icons
     * go back to half-row steps and widgets back to the sizes they are designed for. A layout that
     * already follows the rules comes back unchanged, so it is always safe to run again.
     */
    fun settle(items: List<HomeItem>, columns: Int, rows: Int, free: Boolean, tuck: Int = 0): List<HomeItem> {
        val unitRows = rows * ROW_UNITS
        val grids = HashMap<Int, PageGrid>()
        fun grid(page: Int) = grids.getOrPut(page) { PageGrid(columns, unitRows) }
        fun stepFor(item: HomeItem) = if (free || item is WidgetCell) 1 else ICON_ROW_STEP

        val placed = HashMap<String, HomeItem>()
        val overflow = mutableListOf<HomeItem>()
        val topDown = items.map { fit(it, columns, rows, free) }
            .sortedWith(compareBy({ it.page }, { it.row }, { it.col }))
        for (item in topDown) {
            val g = grid(item.page)
            val slot = g.nearestSlot(item.col, item.row, item.spanX, item.spanY, stepFor(item))
            if (slot == null) {
                overflow += item
                continue
            }
            g.occupy(slot.first, slot.second, item.spanX, blockedSpanY(item, tuck))
            placed[item.id] = if (slot.first == item.col && slot.second == item.row) {
                item
            } else {
                item.moved(item.page, slot.first, slot.second)
            }
        }

        // Anything with no room left on its own page goes to the next page that has some.
        var page = items.maxOfOrNull { it.page } ?: 0
        for (item in overflow) {
            var slot = grid(page).findSlot(item.spanX, item.spanY, stepFor(item))
            while (slot == null && page < MAX_PAGES - 1) {
                page += 1
                slot = grid(page).findSlot(item.spanX, item.spanY, stepFor(item))
            }
            if (slot == null) continue
            grid(page).occupy(slot.first, slot.second, item.spanX, blockedSpanY(item, tuck))
            placed[item.id] = item.moved(page, slot.first, slot.second)
        }

        // Kept in the original order, so nothing is drawn in a different stacking; and nothing is
        // ever dropped — an item with no room anywhere simply stays as it was.
        return items.map { placed[it.id] ?: it }
    }

    /** One item made legal on its own: sized for the grid, on the page, and snapped unless [free]. */
    private fun fit(item: HomeItem, columns: Int, rows: Int, free: Boolean): HomeItem {
        val unitRows = rows * ROW_UNITS
        val sized: HomeItem = if (item is WidgetCell) {
            if (free) {
                item.copy(spanX = item.spanX.coerceIn(1, columns), spanY = item.spanY.coerceIn(1, unitRows))
            } else {
                val spec = Widgets.byType(item.type)
                val (spanX, spanY) = if (spec != null) {
                    Widgets.clampSpan(spec, item.spanX, item.spanY, columns, rows)
                } else {
                    item.spanX.coerceIn(1, columns) to item.spanY.coerceIn(1, unitRows)
                }
                item.copy(spanX = spanX, spanY = spanY)
            }
        } else if (item is AppCell) {
            // App icons come in three shapes: one cell, a super icon two cells wide, or one two
            // rows tall. Anything else is made the nearest of those.
            val tall = item.spanX == 1 && item.spanY > ROW_UNITS && unitRows >= 2 * ROW_UNITS
            val spanX = if (tall) 1 else item.spanX.coerceIn(1, minOf(2, columns))
            val spanY = if (tall) 2 * ROW_UNITS else ROW_UNITS
            if (spanX == item.spanX && spanY == item.spanY) item else item.copy(spanX = spanX, spanY = spanY)
        } else {
            item
        }
        val maxCol = (columns - sized.spanX).coerceAtLeast(0)
        val maxRow = (unitRows - sized.spanY).coerceAtLeast(0)

        if (free) {
            val col = sized.col.coerceIn(0, maxCol)
            val row = sized.row.coerceIn(0, maxRow)
            return if (col == sized.col && row == sized.row) {
                sized
            } else {
                sized.moved(sized.page, col, row).nudged(sized.dx, sized.dy)
            }
        }

        // Where the item is actually drawn, rounded to the closest spot normal placement allows.
        // An icon with no offset keeps the row it is on — it may be tucked under a widget, on a
        // row that is not one of the usual half-row steps, and that is where it was put.
        val step = if (sized is WidgetCell || (sized.dx == 0f && sized.dy == 0f)) 1 else ICON_ROW_STEP
        val col = (sized.col + sized.dx).roundToInt().coerceIn(0, maxCol)
        val row = (((sized.row + sized.dy) / step).roundToInt() * step).coerceIn(0, maxRow)
        return if (col == sized.col && row == sized.row && sized.dx == 0f && sized.dy == 0f) {
            sized
        } else {
            sized.moved(sized.page, col, row)
        }
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

    /**
     * Reverses [upgradeToRowUnits] on a layout that was already in row units when it ran, which
     * every home screen rebuilt by rerunning setup went through on its next launch. The tell cannot
     * come from a real layout: things lie past the bottom of the page, yet every row and every
     * height is a whole multiple of [ROW_UNITS], and dividing back down makes all of it fit.
     * Anything else is returned untouched.
     */
    fun undoDoubleUpgrade(items: List<HomeItem>, rows: Int): List<HomeItem> {
        val unitRows = rows * ROW_UNITS
        if (items.none { it.row + it.spanY > unitRows }) return items
        if (items.any { it.row % ROW_UNITS != 0 || it.spanY % ROW_UNITS != 0 }) return items
        val shrunk = items.map { item ->
            when (item) {
                is WidgetCell -> item.copy(row = item.row / ROW_UNITS, spanY = item.spanY / ROW_UNITS)
                is AppCell -> item.copy(row = item.row / ROW_UNITS)
                is FolderCell -> item.copy(row = item.row / ROW_UNITS)
            }
        }
        return if (shrunk.all { it.row + it.spanY <= unitRows }) shrunk else items
    }
}
