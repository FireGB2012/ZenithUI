package com.gabriel.zenithui.ui.home

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * How a page is drawn part way through a swipe. [shiftX] is in page widths, on top of where the
 * pager has already moved it; [pivotX] and [pivotY] are where on the page it turns about, 0 for
 * its left or top edge to 1 for its right or foot, and further for a point beyond it; [squash]
 * narrows it on top of [scale]; [parallax] lets the icons on it drift apart as it goes.
 */
data class PageTransform(
    val scale: Float = 1f,
    val alpha: Float = 1f,
    val rotationY: Float = 0f,
    val pivotX: Float = 0.5f,
    val shiftX: Float = 0f,
    val parallax: Boolean = false,
    /** Turned in its own plane, clockwise, in degrees. */
    val rotationZ: Float = 0f,
    val pivotY: Float = 0.5f,
    val squash: Float = 1f,
)

/**
 * The ways one home page gives way to the next. Each is a plain function of how far the page is
 * from the middle of the screen — in pages, positive when it lies to the left — and of whether it
 * is the page being left, so they can be checked without a screen, and every one of them is
 * exactly the plain page at rest.
 */
object PageEffects {

    val styles: List<String> = listOf("depth", "slide", "cube", "stack", "carousel", "wheel", "flip", "fold")

    /**
     * Page [offset] pages from the middle. [leaving] is true for the page the swipe started on:
     * which page that is, and not which side a page is on, decides which one Stack sinks, so
     * going back sinks the page you leave just as going on does. Left out, it is the page on the
     * left, which is the page being left on the way forward.
     */
    fun transform(style: String, offset: Float, leaving: Boolean = offset > 0f): PageTransform {
        val at = offset.coerceIn(-1f, 1f)
        val distance = abs(at)
        return when (style) {
            // Nothing but the slide itself.
            "slide" -> PageTransform()
            // The two pages are two faces of a box: each turns about the edge they share.
            "cube" -> PageTransform(
                // Plus nought, so that a page at rest is turned by 0 and not by minus 0.
                rotationY = -90f * at + 0f,
                pivotX = if (at > 0f) 1f else 0f,
                alpha = 1f - 0.3f * distance,
            )
            // A deck: the page being left stays where it is and sinks, the next one comes over it,
            // whichever way the swipe goes. The page coming over is drawn over it: see [over].
            "stack" -> if (leaving) {
                PageTransform(scale = 1f - 0.12f * distance, alpha = 1f - distance, shiftX = at + 0f)
            } else {
                PageTransform()
            }
            // Pages on a turntable seen from in front: each turns to face the middle as it goes
            // round, smaller and closer in, the next already turning into view beside it.
            "carousel" -> PageTransform(
                rotationY = -38f * at + 0f,
                scale = 1f - 0.16f * distance,
                shiftX = 0.3f * at + 0f,
                alpha = 1f - 0.3f * distance,
            )
            // Pages on the rim of a great wheel under the screen: they swing round it, tipping
            // away from upright as they go.
            "wheel" -> PageTransform(
                rotationZ = -11f * at + 0f,
                pivotY = 3.2f,
                alpha = 1f - 0.25f * distance,
            )
            // A card turned over where it stands: the page goes edge on half way, and the next is
            // on its back, coming round.
            "flip" -> PageTransform(
                rotationY = -180f * at + 0f,
                shiftX = at + 0f,
                scale = 1f - 0.12f * sin(PI.toFloat() * distance),
                alpha = if (distance < 0.5f) 1f else 0f,
            )
            // A fold-out: each page folds in towards the crease it shares with the next, narrowing
            // and turning a little, the way a leaflet closes.
            "fold" -> PageTransform(
                squash = 1f - distance,
                rotationY = -24f * at + 0f,
                pivotX = if (at > 0f) 1f else 0f,
                alpha = 1f - 0.35f * distance,
            )
            // Depth, and anything unknown: the page tips back and fades a little as it passes.
            else -> PageTransform(
                scale = 0.94f + 0.06f * (1f - distance),
                alpha = 1f - 0.4f * distance,
                parallax = true,
            )
        }
    }

    /** Whether a page is drawn over the page being left: every page but that one is. */
    fun over(leaving: Boolean): Float = if (leaving) 0f else 1f

    /**
     * How much of the active pill dot [index] has while the pager is at [position] (a page number,
     * with a fraction while turning): 1 for the page in view, shared between two dots in between.
     */
    fun dotWidth(position: Float, index: Int): Float = (1f - abs(position - index)).coerceIn(0f, 1f)

    /**
     * How much of dot [index] is there while the row is growing or shrinking to [dots] of them (a
     * count, with a fraction while a page is being added or taken away): 1 for a dot that is fully
     * in the row, less for the last one on its way in or out, 0 for one that is not there.
     */
    fun dotPresence(dots: Float, index: Int): Float = (dots - index).coerceIn(0f, 1f)

    /**
     * Where the pill under a row of [count] options of equal width lies, from its left edge to its
     * right, in option widths, while what it follows is at [position] (an option's number, with a
     * fraction while between two). It does not slide across as a block: the edge going first
     * reaches the next option by half way, and the edge behind catches up over the second half,
     * so for a moment it lies under both, and then draws itself up.
     */
    fun pillSpan(position: Float, count: Int): Pair<Float, Float> {
        val last = (count - 1).coerceAtLeast(0)
        val at = position.coerceIn(0f, last.toFloat())
        val from = at.toInt().coerceAtMost(last)
        val along = at - from
        return (from + (2f * along - 1f).coerceIn(0f, 1f)) to (from + 1f + (2f * along).coerceIn(0f, 1f))
    }
}
