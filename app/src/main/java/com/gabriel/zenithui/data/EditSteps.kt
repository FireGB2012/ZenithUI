package com.gabriel.zenithui.data

/**
 * The home screen as far as arranging it goes: what stands on the pages, the dock's apps, and how
 * many pages there are. What Undo puts back.
 */
data class Arrangement(val items: List<HomeItem>, val dock: List<String>, val pages: Int)

/** One change to the home screen's arrangement, named as Undo names it: "Mail moved". */
data class EditStep(val label: String, val before: Arrangement, val after: Arrangement)

/**
 * What Undo would take back now: its words, how many changes there are behind it ("+2"), and
 * whether its time is running out, as it is outside edit mode. [key] changes with the change.
 */
data class Undoable(val label: String, val more: Int, val timed: Boolean, val key: Any)

/**
 * Undo for every change made while the home screen is edited. A change is kept as the arrangement
 * just before it and just after it; taking it back puts back only what it touched — what it took
 * off, what it put on, what it moved or altered — so anything that has changed since without it
 * (an app taken off the phone, a stack turned to another of its widgets) stays as it is now.
 */
object EditSteps {

    /** The change [label] from [before] to [after], or null when nothing changed. */
    fun of(label: String, before: Arrangement, after: Arrangement): EditStep? =
        if (before == after) null else EditStep(label, before, after)

    /**
     * [now] with [step] taken back. Everything it touched is as it was before it, under its own id:
     * what it put on goes, what it took off comes back where it was. Something coming back to a
     * spot that has been taken since goes to the first opening instead, as a removal's Undo always
     * has. The dock and the number of pages go back only if the step changed them.
     */
    fun undo(step: EditStep, now: Arrangement, columns: Int, rows: Int): Arrangement {
        val was = step.before.items.associateBy { it.id }
        val became = step.after.items.associateBy { it.id }
        val touched = (was.keys + became.keys).filterTo(HashSet()) { was[it] != became[it] }
        // Each in its place in the list as it is now, and what comes back at the end, as an Undo
        // always added it.
        val here = now.items.mapTo(HashSet()) { it.id }
        var items = now.items.mapNotNull { if (it.id in touched) was[it.id] else it } +
            step.before.items.filter { it.id in touched && it.id !in here }
        val back = step.before.items.filter { it.id in touched }
        // Something else may have taken a spot since: what comes back moves on from there.
        for (item in back) {
            val others = items.filter { it.id != item.id }
            if (!overlapsAny(item, others, columns, rows)) continue
            val placed = LayoutBuilder.putBack(others, item, columns, rows) ?: continue
            items = items.map { if (it.id == item.id) placed else it }
        }
        val dock = if (step.before.dock != step.after.dock) step.before.dock else now.dock
        val pages = if (step.before.pages != step.after.pages) step.before.pages else now.pages
        return Arrangement(items, dock, maxOf(pages, (items.maxOfOrNull { it.page } ?: 0) + 1))
    }

    /** What [item] is called in Undo's words: the app's name, the folder's, the widget's. */
    fun nameOf(item: HomeItem, appsByKey: Map<String, AppInfo>): String = when (item) {
        is AppCell -> appsByKey[item.key]?.label ?: "App"
        is FolderCell -> item.title.ifBlank { "Folder" }
        is WidgetCell -> Widgets.byType(item.type)?.title ?: "Widget"
        is ShortcutCell -> item.label
        is HostedWidgetCell -> "Widget"
        is StackCell -> "Widget stack"
    }

    private fun overlapsAny(item: HomeItem, others: List<HomeItem>, columns: Int, rows: Int): Boolean {
        val grid = PageGrid(columns, rows * ROW_UNITS)
        others.filter { it.page == item.page }.forEach { grid.occupyItem(it) }
        return !grid.isFree(item.col, item.row, item.spanX, item.spanY)
    }
}

/** Every change made in one go at editing the home screen, newest last. */
data class EditHistory(val steps: List<EditStep> = emptyList()) {

    /** The change Undo takes back next, if there is one. */
    val newest: EditStep? get() = steps.lastOrNull()

    /** How many changes there are before [newest]: the capsule's "+2". */
    val more: Int get() = (steps.size - 1).coerceAtLeast(0)

    fun with(step: EditStep): EditHistory = EditHistory(steps + step)

    /** The newest change, and the history without it; null with nothing to take back. */
    fun takeNewest(): Pair<EditStep, EditHistory>? = newest?.let { it to EditHistory(steps.dropLast(1)) }

    /**
     * Everything a step here could put back or take off: what is held for them, another app's
     * widget's number or a shortcut's pin, must be kept while it can still be needed.
     */
    fun holds(): List<HomeItem> = steps.flatMap { it.before.items + it.after.items }
}
