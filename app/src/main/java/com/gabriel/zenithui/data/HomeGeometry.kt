package com.gabriel.zenithui.data

import kotlin.math.roundToInt

/** A rectangle in whole pixels, measured from the top left corner of whatever it sits in. */
data class PxRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/**
 * Where everything on a home page sits, in whole pixels. The page, the drop hint, the resize
 * outline, the dock, the name at the top and the search bar all ask the same object, so two edges
 * that should meet are one number rather than two roundings of it.
 *
 * Every cell is the same whole number of pixels wide and every row the same number tall. Pixels
 * left over when the page does not divide evenly are shared between the outer edges ([originX],
 * [originY]) rather than making some cells a pixel bigger than their neighbours.
 */
class HomeGeometry(
    /** The page area in pixels: the screen less the page's own margins. */
    val width: Int,
    val height: Int,
    val columns: Int,
    val rows: Int,
    /** Pixels per dp. */
    val density: Float,
    val showLabels: Boolean,
) {
    // A page always has at least one column and one row: a saved value of nothing, from a damaged
    // backup, must not stop the home screen from being drawn at all.
    val cellWidth: Int = width / columns.coerceAtLeast(1)
    val rowHeight: Int = height / rows.coerceAtLeast(1)

    /** A row unit, a quarter of a row. Only free placement puts anything between rows. */
    val unitHeight: Float = rowHeight / ROW_UNITS.toFloat()

    val gridWidth: Int = cellWidth * columns
    val gridHeight: Int = rowHeight * rows

    /** The grid's top left corner inside the page area. */
    val originX: Int = (width - gridWidth) / 2
    val originY: Int = (height - gridHeight) / 2

    /** The icon size in dp, as it is handed to an icon, and the pixels that comes to. */
    val iconDp: Float = iconSizeDp(cellWidth / density, rowHeight / density, showLabels)
    val icon: Int = (iconDp * density).roundToInt()

    /** The strip a label is written in under an icon; nothing when labels are off. */
    val label: Int = if (showLabels) (LABEL_DP * density).roundToInt() else 0

    /** From a cell's left edge to its icon, and from the icon to the cell's right edge. */
    val sideInset: Int = ((cellWidth - icon) / 2).coerceAtLeast(0)
    val sideInsetEnd: Int = (cellWidth - icon - sideInset).coerceAtLeast(0)

    /** From a row's top edge to its icons. */
    val topInset: Int = ((rowHeight - icon - label) / 2).coerceAtLeast(0)

    /** From the bottom of a row's labels to the bottom edge of the row. */
    val underInset: Int = (rowHeight - topInset - icon - label).coerceAtLeast(0)

    /** Where an icon is drawn inside its own cell. */
    val iconFrame: PxRect = PxRect(sideInset, topInset, sideInset + icon, topInset + icon)

    /** The page's left line, the left edge of the first column's icons, from the grid's left edge. */
    val leftLine: Int get() = sideInset

    /** The page's right line, the right edge of the last column's icons, from the grid's left edge. */
    val rightLine: Int get() = gridWidth - sideInsetEnd

    /**
     * Where a widget [spanX] columns wide and [spanY] row units tall is drawn inside the cells it
     * covers: from the left edge of the icon in its first column to the right edge of the icon in
     * its last, and from the top of the icons in its first row to the bottom of the labels in its
     * last. That leaves the same gap under a widget as under any icon's label. One shorter than a
     * row, which only free placement allows, keeps its whole height.
     */
    fun widgetFrame(spanX: Int, spanY: Int): PxRect {
        val bottom = (spanY * unitHeight).roundToInt()
        val right = spanX * cellWidth - sideInsetEnd
        return if (spanY >= ROW_UNITS) {
            PxRect(sideInset, topInset, right, bottom - underInset)
        } else {
            PxRect(sideInset, 0, right, bottom)
        }
    }

    /**
     * Where a super icon [spanX] columns wide and [rowsTall] rows tall is drawn inside the cells
     * it covers. It runs from the left edge of the icon in its first column to the right edge of
     * the icon in its last. Going down, each row past the first adds a column's width rather than
     * a whole row, so it keeps a wide one's proportions: a 2 by 2 is square. It sits centred
     * between the top of the icons in its first row and the bottom of the icons in its last.
     */
    fun superIconFrame(spanX: Int, rowsTall: Int): PxRect {
        val span = rowHeight * (rowsTall - 1) + icon
        val height = minOf(cellWidth * (rowsTall - 1) + icon, span)
        val top = topInset + (span - height) / 2
        return PxRect(sideInset, top, sideInset + cellWidth * (spanX - 1) + icon, top + height)
    }

    /**
     * How tall the dock's pane is: an icon as big as the columns alone would make it, with a margin
     * above and below. It is deliberately not taken from the page's own icons. On a cramped page
     * the icons shrink to fit their rows; a dock that shrank with them would hand the page more
     * room, the icons would grow again, and the two would chase each other on every frame.
     */
    val dockHeight: Int =
        (iconSizeDp(cellWidth / density, Float.MAX_VALUE, showLabels) * density).roundToInt() +
            2 * (DOCK_MARGIN_DP * density).roundToInt()

    /**
     * How big a dock icon is with [count] apps: the page's icon size, or smaller when there are
     * more apps than columns, so that however many there are they stand side by side inside the
     * grid without touching.
     */
    fun dockIcon(count: Int): Int =
        if (count > columns && count > 0) minOf(icon, (gridWidth / count * DOCK_FILL).toInt()) else icon

    /**
     * The left edge of dock icon [index] of [count], from the grid's left edge. The dock's bar is
     * as wide as the grid however many apps it holds, as on an iPhone, and the apps share it out
     * equally, each in the middle of its share. With one app for each column a share is a column,
     * so every icon is under the icons above it; with fewer they spread across the bar, and with
     * more they close up.
     */
    fun dockIconLeft(index: Int, count: Int): Int {
        // One for each column: on the page's own lines exactly, with no rounding of its own.
        if (count == columns) return sideInset + index * cellWidth
        val share = gridWidth.toFloat() / count.coerceAtLeast(1)
        return ((index + 0.5f) * share - dockIcon(count) / 2f).roundToInt()
    }

    companion object {
        /** The height of the strip an 11sp label sits in, in dp. */
        const val LABEL_DP = 20f

        /** How much of its share of an over-full dock an icon may take, leaving the rest as gaps. */
        private const val DOCK_FILL = 0.9f

        /** The space above and below the icons inside the dock's pane, in dp. */
        private const val DOCK_MARGIN_DP = 12f

        /**
         * How big an icon is, worked out from the cell it sits in rather than chosen: about three
         * quarters of the cell's width, never so tall that it pushes its label out, and kept
         * between sizes that read well.
         */
        fun iconSizeDp(cellWidthDp: Float, rowHeightDp: Float, showLabels: Boolean): Float {
            val labelSpace = if (showLabels) 24f else 4f
            return minOf(
                (cellWidthDp * 0.75f).coerceIn(54f, 72f),
                (rowHeightDp - labelSpace - 8f).coerceAtLeast(28f),
                (cellWidthDp - 8f).coerceAtLeast(28f),
            )
        }
    }
}
