package com.gabriel.zenithui.data

import android.appwidget.AppWidgetHost
import android.content.Context
import kotlin.math.ceil

/**
 * The one widget host Zenith keeps other apps' widgets in. Android tells it whenever one of them
 * changes, but only while it is listening, which is while the home screen is on show.
 */
object ZenithWidgetHost {
    /** Zenith's own number for its host: any number will do, as long as it never changes. */
    const val HOST_ID = 0x5A31

    @Volatile private var host: AppWidgetHost? = null

    fun get(context: Context): AppWidgetHost =
        host ?: synchronized(this) { host ?: AppWidgetHost(context.applicationContext, HOST_ID).also { host = it } }
}

/**
 * How big another app's widget is on Zenith's grid. A widget says how big it is in dp, or — from
 * Android 12 — in cells of a grid like Zenith's own; what it says is turned into cells and whole
 * rows of the page in use, never more than the page.
 */
object HostedSizes {

    /**
     * The cells across and row units down a widget wants. [cellDp] and [rowDp] are one cell's width
     * and one whole row's height on the page, and [iconDp] the size of its icons; [targetX] and
     * [targetY] the widget's own cells, or 0.
     */
    fun spanFor(
        widthDp: Int,
        heightDp: Int,
        targetX: Int,
        targetY: Int,
        cellDp: Float,
        rowDp: Float,
        iconDp: Float,
        columns: Int,
        rows: Int,
    ): Pair<Int, Int> {
        val across = if (targetX > 0) targetX else cells(widthDp, cellDp, iconDp)
        val down = if (targetY > 0) targetY else cells(heightDp, rowDp, iconDp)
        return across.coerceIn(1, columns) to down.coerceIn(1, rows) * ROW_UNITS
    }

    /** The smallest it may be made, in cells and row units: what it says it can shrink to. */
    fun minSpan(minWidthDp: Int, minHeightDp: Int, cellDp: Float, rowDp: Float, iconDp: Float, columns: Int, rows: Int): Pair<Int, Int> =
        cells(minWidthDp, cellDp, iconDp).coerceIn(1, columns) to cells(minHeightDp, rowDp, iconDp).coerceIn(1, rows) * ROW_UNITS

    /**
     * How many cells [pitch] apart it takes to hold [dp]. A widget is drawn from the icons in its
     * first cell to the icons in its last ([HomeGeometry.widgetFrame]), so n cells give it the room
     * of n - 1 of them and one icon of [iconDp], across and down alike.
     */
    private fun cells(dp: Int, pitch: Float, iconDp: Float): Int {
        if (dp <= 0 || pitch <= 0f) return 1
        // A widget a hair bigger than that still fits in it: Android's own launchers forgive
        // about a tenth.
        val past = (dp * 0.9f - iconDp).coerceAtLeast(0f)
        return (1f + ceil(past / pitch)).toInt()
    }
}

/**
 * Making and taking apart stacks. A stack is made of widgets of the same size in the same spot; it
 * takes the place and size of the widget it was made on, and when only one widget is left in it,
 * that widget stands on its own again, where the stack was.
 */
object StackRules {
    /** As many as a stack holds. Past this, swiping through it is a chore, not a pleasure. */
    const val MOST = 8

    /** [item] as a member of a stack, if it is a widget that can be one. */
    fun memberOf(item: HomeItem): StackMember? = when (item) {
        is WidgetCell -> StackMember(item.id, type = item.type)
        is HostedWidgetCell -> StackMember(item.id, appWidgetId = item.appWidgetId, provider = item.provider)
        else -> null
    }

    /** [target] with [member] stacked on it, or null when it cannot take any more or is not a widget. */
    fun stack(target: HomeItem, member: StackMember, newId: String): StackCell? = when (target) {
        is StackCell -> if (target.members.size >= MOST) null else target.copy(members = target.members + member)
        else -> memberOf(target)?.let { first ->
            StackCell(
                id = newId, page = target.page, col = target.col, row = target.row,
                spanX = target.spanX, spanY = target.spanY,
                members = listOf(first, member),
                dx = target.dx, dy = target.dy,
            )
        }
    }

    /**
     * [stack] without the member at [index]: a smaller stack, or the one widget left standing on its
     * own in the stack's place.
     */
    fun without(stack: StackCell, index: Int): HomeItem? {
        val left = stack.members.filterIndexed { i, _ -> i != index }
        return when {
            left.size >= 2 -> stack.copy(members = left)
            left.size == 1 -> alone(stack, left.first())
            else -> null
        }
    }

    /** One member standing on its own, in [stack]'s place and at its size. */
    fun alone(stack: StackCell, member: StackMember): HomeItem =
        if (member.hosted) {
            HostedWidgetCell(member.id, stack.page, stack.col, stack.row, stack.spanX, stack.spanY, member.appWidgetId, member.provider, dx = stack.dx, dy = stack.dy)
        } else {
            WidgetCell(member.id, stack.page, stack.col, stack.row, member.type, stack.spanX, stack.spanY, dx = stack.dx, dy = stack.dy)
        }

    /** [stack] turned so the member at [index] is on top, the others after it in their order. */
    fun bringToTop(stack: StackCell, index: Int): StackCell {
        if (index !in stack.members.indices || index == 0) return stack
        return stack.copy(members = stack.members.drop(index) + stack.members.take(index))
    }
}
