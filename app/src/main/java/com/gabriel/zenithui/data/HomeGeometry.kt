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

    /**
     * From the bottom of a widget a whole number of rows tall to the bottom edge of its last row:
     * the strip its name is written in, and the gap under that.
     */
    val underWidget: Int = label + underInset

    /** Where an icon is drawn inside its own cell. */
    val iconFrame: PxRect = PxRect(sideInset, topInset, sideInset + icon, topInset + icon)

    /** The page's left line, the left edge of the first column's icons, from the grid's left edge. */
    val leftLine: Int get() = sideInset

    /** The page's right line, the right edge of the last column's icons, from the grid's left edge. */
    val rightLine: Int get() = gridWidth - sideInsetEnd

    /**
     * Where a widget [spanX] columns wide and [spanY] row units tall is drawn inside the cells it
     * covers: from the left edge of the icon in its first column to the right edge of the icon in
     * its last, and from the top of the icons in its first row to the bottom of the icons in its
     * last, as on an iPhone. Its name goes under it in the strip the icons beside it have theirs in
     * ([widgetNameStrip]). One shorter than a row, which only free placement allows, keeps its
     * whole height.
     */
    fun widgetFrame(spanX: Int, spanY: Int): PxRect {
        val bottom = (spanY * unitHeight).roundToInt()
        val right = spanX * cellWidth - sideInsetEnd
        return if (spanY >= ROW_UNITS) {
            PxRect(sideInset, topInset, right, bottom - underWidget)
        } else {
            PxRect(sideInset, 0, right, bottom)
        }
    }

    /**
     * Where a widget's name is written, inside the cells the widget covers: straight under its
     * [widgetFrame] and as wide as it, in the same strip as the names of the icons in its last
     * row, so the gap under it is the gap under theirs. It is empty when labels are off, and for a
     * widget shorter than a row, whose frame takes all of its height.
     */
    fun widgetNameStrip(spanX: Int, spanY: Int): PxRect {
        val frame = widgetFrame(spanX, spanY)
        val strip = if (spanY >= ROW_UNITS) label else 0
        return PxRect(frame.left, frame.bottom, frame.right, frame.bottom + strip)
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
     * The left edge of dock icon [index] of [count], from the grid's left edge. However many apps a
     * dock across the screen holds, as on an iPhone, they share out the grid's width equally, each
     * in the middle of its share, and its slab of glass stands a little wider round them. With one
     * app for each column a share is a column, so every icon is under the icons above it; with
     * fewer they spread across the dock, and with more they close up.
     */
    fun dockIconLeft(index: Int, count: Int): Int {
        // One for each column: on the page's own lines exactly, with no rounding of its own.
        if (count == columns) return sideInset + index * cellWidth
        val share = gridWidth.toFloat() / count.coerceAtLeast(1)
        return ((index + 0.5f) * share - dockIcon(count) / 2f).roundToInt()
    }

    /**
     * The left edge of dock icon [index] of [count] from the left edge of the screen, for a dock in
     * [style] on a screen [screenWidthPx] wide with this page centred above it, as the home screen
     * and its previews lay the page out. A dock across the screen has its apps where [dockIconLeft]
     * puts them under the page, so with one for each column each is under the icons above it. A
     * floating dock that closes up round fewer than four ([dockHugs]) has them side by side in the
     * middle of its slab, as far apart as a full slab's four and with the margin above and below
     * them at its ends too.
     */
    fun dockIconX(style: String, index: Int, count: Int, screenWidthPx: Int): Int {
        if (!dockHugs(style, count)) return (screenWidthPx - width) / 2 + originX + dockIconLeft(index, count)
        val icon = dockIcon(count)
        val slab = dockSlabWidth(style, count, screenWidthPx, icon, density)
        val margin = dockMargin(icon, density)
        return (screenWidthPx - slab) / 2 + margin + index * dockPitch(screenWidthPx, icon, density)
    }

    companion object {
        /** The height of the strip an 11sp label sits in, in dp. */
        const val LABEL_DP = 20f

        /**
         * The band the Search pill sits in, between the page and the dock, in dp. Like the dock it
         * is a fixed height rather than one taken from the page: the page's rows are worked out
         * from what the band and the dock leave, so a band sized from the page would change the
         * rows, which would change the band, and the two would never settle.
         */
        const val SEARCH_BAND_DP = 44f

        /** How tall the Search pill is, in the middle of its band, in dp. */
        const val SEARCH_PILL_HEIGHT_DP = 32f

        /**
         * How tall the floating glass dock is, in dp: the biggest icon a page can have, 68 dp, with
         * [DOCK_MARGIN_DP] above and below it. It does not follow the page, for the band's reason.
         */
        const val DOCK_HEIGHT_DP = 92f

        /** How far the floating dock stands in from the left and right edges of the screen, in dp. */
        const val DOCK_SIDE_INSET_DP = 10f

        /** The floating dock's corners, as round as an iPhone's, in dp. */
        const val DOCK_CORNER_DP = 36f

        /** The corners of every widget, whatever its size, in dp. */
        const val WIDGET_CORNER_DP = 22f

        /** A full dock: four apps, one under each column of the iPhone grid. */
        const val DOCK_APPS = 4

        /**
         * Whether a dock in [style] closes up round its [icons] rather than spanning the screen: a
         * floating one with fewer than a full dock's four. An empty one keeps its whole width, which
         * the words asking for an app need.
         */
        fun dockHugs(style: String, icons: Int): Boolean = style == "floating" && icons in 1 until DOCK_APPS

        /**
         * How wide the dock's slab of glass is, in pixels, on a screen [screenWidthPx] wide. Glass
         * spans the screen less [DOCK_SIDE_INSET_DP] each side however many apps it holds, as on an
         * iPhone. Floating is the same slab until it has fewer than four apps, when it closes up round
         * them ([dockHugs]): [iconPx] each, as far apart as a full slab's four, with the margin above
         * and below them at its ends too. Icons only ("none") draws no glass, and this is then the
         * row its icons stand in, the same as Glass. Nothing is ever wider than Glass.
         */
        fun dockSlabWidth(style: String, icons: Int, screenWidthPx: Int, iconPx: Int, density: Float): Int {
            val full = fullSlab(screenWidthPx, density)
            if (!dockHugs(style, icons)) return full
            val hug = 2 * dockMargin(iconPx, density) + iconPx + (icons - 1) * dockPitch(screenWidthPx, iconPx, density)
            return hug.coerceAtMost(full)
        }

        /** The slab across the screen, less its inset each side. */
        private fun fullSlab(screenWidthPx: Int, density: Float): Int =
            (screenWidthPx - 2 * (DOCK_SIDE_INSET_DP * density).roundToInt()).coerceAtLeast(0)

        /** Above and below the dock's icons inside its slab, and at the ends of a slab that hugs them. */
        private fun dockMargin(iconPx: Int, density: Float): Int =
            (((DOCK_HEIGHT_DP * density).roundToInt() - iconPx) / 2).coerceAtLeast(0)

        /**
         * From one dock icon's left edge to the next's in a slab that hugs them: the spacing a full
         * slab's four would have with the same margin at its ends as above and below them, so a
         * floating dock grows by the same step for each app it takes, up to the full slab.
         */
        private fun dockPitch(screenWidthPx: Int, iconPx: Int, density: Float): Int =
            ((fullSlab(screenWidthPx, density) - 2 * dockMargin(iconPx, density) - iconPx) / (DOCK_APPS - 1)).coerceAtLeast(0)

        /** How much of its share of an over-full dock an icon may take, leaving the rest as gaps. */
        private const val DOCK_FILL = 0.9f

        /** The space above and below the icons inside the dock's pane, in dp. */
        private const val DOCK_MARGIN_DP = 12f

        /**
         * How big an icon is, worked out from the cell it sits in rather than chosen: as on an
         * iPhone, a little under two thirds of the cell's width, kept between sizes that read well,
         * and never so tall that it pushes its label out or so wide that it meets its neighbours.
         */
        fun iconSizeDp(cellWidthDp: Float, rowHeightDp: Float, showLabels: Boolean): Float {
            val labelSpace = if (showLabels) 24f else 4f
            return minOf(
                (cellWidthDp * 0.64f).coerceIn(52f, 68f),
                (rowHeightDp - labelSpace - 8f).coerceAtLeast(28f),
                (cellWidthDp - 8f).coerceAtLeast(28f),
            )
        }
    }
}
