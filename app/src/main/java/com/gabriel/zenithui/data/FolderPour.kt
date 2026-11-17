package com.gabriel.zenithui.data

import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * An open folder's apps: nine to a page, three across and three down, in the folder's own order,
 * across and then down; the tenth starts the second page where the first started the first.
 */
object FolderGrid {
    const val COLUMNS = 3
    const val ROWS = 3
    const val PER_PAGE = COLUMNS * ROWS

    /** How many pages [count] apps take: always one, even for none. */
    fun pages(count: Int): Int = max(1, (count + PER_PAGE - 1) / PER_PAGE)

    fun pageOf(index: Int): Int = index / PER_PAGE

    fun column(index: Int): Int = (index % PER_PAGE) % COLUMNS

    fun row(index: Int): Int = (index % PER_PAGE) / COLUMNS

    /** What is on [page] of [items]. */
    fun <T> onPage(items: List<T>, page: Int): List<T> =
        items.drop(page * PER_PAGE).take(PER_PAGE)
}

/**
 * The open folder's pane, in dp, as iOS 26 has it: one size whatever is in it, a little over three
 * quarters of the screen wide, three columns of icons about the home screen's size with their names
 * under them, and room under the last row for the page dots. Its name is written above it, starting
 * where the first column's icon does.
 */
data class FolderSize(
    val width: Float,
    val height: Float,
    val corner: Float,
    /** From each side of the pane to the first and last column. */
    val side: Float,
    /** One column's width. */
    val column: Float,
    val icon: Float,
    /** One row's height: the icon, its name and the gap under it. */
    val row: Float,
    /** Above the first row and under the last. */
    val top: Float,
    val bottom: Float,
    /** The line a name is written on. */
    val label: Float,
) {
    /** Where the icon in [column] starts, from the pane's left edge. */
    fun iconLeft(column: Int): Float = side + column * this.column + (this.column - icon) / 2f

    /** Where the icons in [row] start, from the pane's top. */
    fun iconTop(row: Int): Float = top + row * this.row

    /** Where the folder's name starts, from the pane's left edge: in line with the first icon. */
    val titleStart: Float get() = iconLeft(0)

    /**
     * Where the pane's top is in a space [spaceHeight] tall: the pane and its name above it stand
     * in the middle of it together, never off its top.
     */
    fun paneTop(spaceHeight: Float): Float {
        val above = TITLE_LINE + TITLE_GAP
        return max(0f, (spaceHeight - above - height) / 2f) + above
    }

    companion object {
        /** How much of the screen's width the pane takes, and the least and most it can be. */
        const val SHARE = 0.78f
        const val MIN_WIDTH = 280f
        const val MAX_WIDTH = 360f

        /** Room kept either side of the pane on a screen too narrow for its least. */
        const val MARGIN = 12f

        const val CORNER = 36f
        const val SIDE = 11f

        /** An icon's share of its column, and the least and most it can be. */
        const val ICON_SHARE = 0.70f
        const val MIN_ICON = 52f
        const val MAX_ICON = 76f

        /** Between an icon and its name, and under the name. */
        const val NAME_GAP = 6f
        const val ROW_GAP = 14f
        const val TOP = 24f
        const val BOTTOM = 18f

        /** The name above the pane: its size, the line it stands on, and the gap down to the pane. */
        const val TITLE_SP = 32f
        const val TITLE_LINE = 40f
        const val TITLE_GAP = 18f

        /** The page dots: how big, how far apart, and how far up from the pane's foot. */
        const val DOT = 7f
        const val DOT_GAP = 8f
        const val DOT_FOOT = 13f

        /** The pane for a screen [screenWidth] dp wide, names written on lines [label] dp tall. */
        fun of(screenWidth: Float, label: Float = HomeGeometry.LABEL_DP): FolderSize {
            val width = floor((screenWidth * SHARE).coerceIn(MIN_WIDTH, MAX_WIDTH))
                .coerceAtMost(max(screenWidth - 2f * MARGIN, 1f))
            val column = (width - 2f * SIDE) / FolderGrid.COLUMNS
            val icon = (column * ICON_SHARE).coerceIn(MIN_ICON, MAX_ICON).coerceAtMost(column)
            val row = icon + NAME_GAP + label + ROW_GAP
            return FolderSize(
                width = width,
                height = TOP + FolderGrid.ROWS * row + BOTTOM,
                corner = CORNER,
                side = SIDE,
                column = column,
                icon = icon,
                row = row,
                top = TOP,
                bottom = BOTTOM,
                label = label,
            )
        }
    }
}

/** What a folder opens between: its tile on the home screen and the pane, in pixels of one space. */
data class FolderPourGeometry(
    val tileLeft: Float,
    val tileTop: Float,
    val tileWidth: Float,
    val tileHeight: Float,
    val tileRadius: Float,
    val paneLeft: Float,
    val paneTop: Float,
    val paneWidth: Float,
    val paneHeight: Float,
    val paneRadius: Float,
)

/**
 * A folder opening as liquid glass: its glass tile is the drop the pane spreads out of, wide first
 * and then tall, sloshing once and settling, with the first page zooming out of the tile's tiny
 * icons inside it; and drawn back into the tile again, which gulps it down. Worked out rather than
 * stepped, so any frame can be asked for, and closing can start from wherever opening got to.
 */
object FolderPour {

    /** How long opening's clock runs, in seconds: by then the drop is the pane to within half a pixel. */
    const val IN_S = 0.62f

    /** How long closing takes, and the end of it over which the drop fades onto the tile. */
    const val OUT_MS = 300
    const val FADE_MS = 70

    /** How far into closing the names, and the name above, are gone. */
    const val WORDS_OUT = 0.35f

    /** How long the name under the tile takes to fade as the drop leaves it, in seconds. */
    const val NAME_OUT_S = 0.12f

    /** The tile's gulp once the drop is back in it. */
    const val GULP_MS = 340

    /** How big the name above is as it starts to come in. */
    const val TITLE_FROM = 0.6f

    /** The ring of light that runs out from the finger as it opens: how long it runs, in ms, and how bright. */
    const val RING_MS = 600f
    const val RING_PEAK = 0.7f

    /**
     * The tile's tiny icons, the Grid style's three by three: each this share of the tile, this far
     * apart and this far in from its edges. The apps fly out of them into the pane and back.
     */
    const val MINI_ICON = 0.22f
    const val MINI_GAP = 0.05f
    const val MINI_EDGE = 0.12f

    /** The springs, as rad/s and damping (see [LiquidSpring]): width leads, height a beat behind. */
    private const val WIDTH_W = 21.5f
    private const val WIDTH_Z = 0.72f
    private const val HEIGHT_W = 20f
    private const val HEIGHT_Z = 0.7f
    private const val HEIGHT_LAG_S = 0.04f
    private const val MIDDLE_W = 22f
    private const val MIDDLE_Z = 0.86f
    private const val POP_W = 26f
    private const val POP_Z = 0.72f
    private const val TITLE_W = 22f
    private const val TITLE_Z = 0.8f
    private const val TITLE_LAG_S = 0.06f
    private const val TITLE_FADE_S = 0.18f

    /** When the first app nearest the finger starts out, how much later each after it, and how many get a turn of their own. */
    private const val POP_LEAD_S = 0.03f
    private const val POP_STEP_S = 0.016f
    private const val POP_TURNS = 8

    /** How far the apps have zoomed before their names start to show, and when they are all there. */
    private const val NAMES_FROM = 0.7f
    private const val NAMES_SPAN = 0.25f

    /** The gulp: how much wider and flatter, then narrower and taller, a third and two thirds through. */
    private const val GULP_WIDE = 1.07f
    private const val GULP_FLAT = 0.935f
    private const val GULP_NARROW = 0.975f
    private const val GULP_TALL = 1.025f

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    /** The drop [seconds] after it set off. */
    fun frameIn(seconds: Float, g: FolderPourGeometry): PourFrame {
        val across = LiquidSpring.step(seconds, WIDTH_W, WIDTH_Z)
        val down = LiquidSpring.step(seconds - HEIGHT_LAG_S, HEIGHT_W, HEIGHT_Z)
        val travel = LiquidSpring.step(seconds, MIDDLE_W, MIDDLE_Z)
        val width = lerp(g.tileWidth, g.paneWidth, across)
        val height = lerp(g.tileHeight, g.paneHeight, down)
        val middleX = lerp(g.tileLeft + g.tileWidth / 2f, g.paneLeft + g.paneWidth / 2f, travel)
        val middleY = lerp(g.tileTop + g.tileHeight / 2f, g.paneTop + g.paneHeight / 2f, travel)
        val radius = min(lerp(g.tileRadius, g.paneRadius, across.coerceIn(0f, 1f)), min(width, height) / 2f)
        return PourFrame(middleX - width / 2f, middleY - height / 2f, width, height, radius.coerceAtLeast(0f))
    }

    /**
     * The drop [fraction] of the way back from [from] into the tile: slow off, quicker, slow onto
     * it, and never past it; fading onto the tile over the last [FADE_MS].
     */
    fun frameOut(fraction: Float, from: PourFrame, g: FolderPourGeometry): PourFrame {
        val f = fraction.coerceIn(0f, 1f)
        val p = closing(f)
        val width = lerp(from.width, g.tileWidth, p)
        val height = lerp(from.height, g.tileHeight, p)
        val middleX = lerp(from.left + from.width / 2f, g.tileLeft + g.tileWidth / 2f, p)
        val middleY = lerp(from.top + from.height / 2f, g.tileTop + g.tileHeight / 2f, p)
        val radius = min(lerp(from.radius, g.tileRadius, p), min(width, height) / 2f)
        return PourFrame(middleX - width / 2f, middleY - height / 2f, width, height, radius.coerceAtLeast(0f), 1f - tileShown(f))
    }

    /**
     * How much of the folder's own tile shows on the home screen, [fraction] of the way through
     * closing: none while the folder is out of it, as the open pane is the tile poured out, and
     * coming back over the last [FADE_MS] as the drop fades onto it, so the two always make one.
     */
    fun tileShown(fraction: Float): Float {
        val fadeFrom = 1f - FADE_MS.toFloat() / OUT_MS
        val f = fraction.coerceIn(0f, 1f)
        return if (f <= fadeFrom) 0f else ((f - fadeFrom) / (1f - fadeFrom)).coerceIn(0f, 1f)
    }

    /** How far back into the tile the drop is, [fraction] of the way through closing: slow off, quicker, slow on. */
    fun closing(fraction: Float): Float {
        val f = fraction.coerceIn(0f, 1f)
        return if (f < 0.5f) 4f * f * f * f else 1f - (-2f * f + 2f).pow(3) / 2f
    }

    /**
     * Where an app is drawn as it comes out of the tile: [dx] and [dy] from its own place, in the
     * pane's pixels (all in the drop is drawn at the drop's zoom), and [scale] its size there.
     */
    data class Flight(val dx: Float, val dy: Float, val scale: Float)

    /**
     * App [slot] of the first page, [out] of the way from the tile's tiny icon into its own place
     * in [place] (0 the tiny icon, 1 its place, a touch past for a moment), with the drop at
     * [frame]. The tiny icons ride the drop, three by three across its width and down its height,
     * as big as its short side allows, so an app not yet out stays inside it as it spreads.
     * [unit] is pixels to the dp.
     */
    fun flight(out: Float, slot: Int, frame: PourFrame, g: FolderPourGeometry, place: FolderSize, unit: Float): Flight {
        val z = zoom(frame, g)
        if (z <= 0f || place.icon <= 0f || unit <= 0f) return Flight(0f, 0f, 1f)
        val column = FolderGrid.column(slot)
        val row = FolderGrid.row(slot)
        val step = MINI_ICON + MINI_GAP
        // The tiny icon's middle, from the drop's, and the app's own, from the pane's: both in the pane's pixels.
        val miniX = ((MINI_EDGE + column * step + MINI_ICON / 2f) * frame.width - frame.width / 2f) / z
        val miniY = ((MINI_EDGE + row * step + MINI_ICON / 2f) * frame.height - frame.height / 2f) / z
        val ownX = (place.iconLeft(column) + place.icon / 2f) * unit - g.paneWidth / 2f
        val ownY = (place.iconTop(row) + place.icon / 2f) * unit - g.paneHeight / 2f
        val miniScale = MINI_ICON * min(frame.width, frame.height) / (place.icon * unit * z)
        val left = 1f - out
        return Flight((miniX - ownX) * left, (miniY - ownY) * left, lerp(miniScale, 1f, out))
    }

    /** How big what is in the drop is drawn when it is [frame]: the drop's width over the pane's. */
    fun zoom(frame: PourFrame, g: FolderPourGeometry): Float =
        if (g.paneWidth <= 0f) 1f else frame.width / g.paneWidth

    /** How much of the apps' names shows with them zoomed to [zoom] of their size. */
    fun namesShown(zoom: Float): Float = ((zoom - NAMES_FROM) / NAMES_SPAN).coerceIn(0f, 1f)

    /**
     * The order the apps at [xs], [ys] come out in, nearest the finger at [tapX], [tapY] first; level
     * with each other, in their own order.
     */
    fun order(xs: FloatArray, ys: FloatArray, tapX: Float, tapY: Float): List<Int> =
        xs.indices.sortedWith(compareBy<Int> { hypot(xs[it] - tapX, ys[it] - tapY) }.thenBy { it })

    /**
     * How far out of the tile the app [rank] in [order] is, [seconds] after the drop set off: 0
     * still the tile's tiny icon, 1 in its place, a touch past for a moment (see [flight]).
     */
    fun pop(seconds: Float, rank: Int): Float =
        LiquidSpring.step(seconds - POP_LEAD_S - POP_STEP_S * rank.coerceIn(0, POP_TURNS), POP_W, POP_Z)

    /** The name above, [seconds] after the drop set off: how big, about its left edge, and how solid. */
    fun titleScale(seconds: Float): Float = lerp(TITLE_FROM, 1f, LiquidSpring.step(seconds - TITLE_LAG_S, TITLE_W, TITLE_Z))

    fun titleAlpha(seconds: Float): Float = ((seconds - TITLE_LAG_S) / TITLE_FADE_S).coerceIn(0f, 1f)

    /** How solid the words are [fraction] of the way through closing: gone by [WORDS_OUT]. */
    fun wordsLeaving(fraction: Float): Float = (1f - fraction / WORDS_OUT).coerceIn(0f, 1f)

    /**
     * How solid the name under the tile is, [seconds] after the drop set off: the tile is the drop,
     * so it goes at once, but its name fades over [NAME_OUT_S], where nothing behind is blurred.
     */
    fun nameLeaving(seconds: Float): Float = (1f - seconds / NAME_OUT_S).coerceIn(0f, 1f)

    /**
     * The tile gulping the drop down, [fraction] of the way through: as wide and as tall as it is,
     * across and down, about its foot. A little wide and flat, a little narrow and tall, still.
     */
    fun gulp(fraction: Float): Pair<Float, Float> {
        val f = fraction.coerceIn(0f, 1f)
        fun ease(t: Float) = t * t * (3f - 2f * t)
        return when {
            f >= 1f -> 1f to 1f
            f < 1f / 3f -> { val k = ease(f * 3f); lerp(1f, GULP_WIDE, k) to lerp(1f, GULP_FLAT, k) }
            f < 2f / 3f -> { val k = ease((f - 1f / 3f) * 3f); lerp(GULP_WIDE, GULP_NARROW, k) to lerp(GULP_FLAT, GULP_TALL, k) }
            else -> { val k = ease((f - 2f / 3f) * 3f); lerp(GULP_NARROW, 1f, k) to lerp(GULP_TALL, 1f, k) }
        }
    }
}
