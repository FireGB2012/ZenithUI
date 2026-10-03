package com.gabriel.zenithui.ui.home

import kotlin.math.abs

/**
 * How a page is drawn part way through a swipe. [shiftX] is in page widths, on top of where the
 * pager has already moved it; [pivotX] is where across the page it turns about, 0 for its left
 * edge to 1 for its right; [parallax] lets the icons on it drift apart as it goes.
 */
data class PageTransform(
    val scale: Float = 1f,
    val alpha: Float = 1f,
    val rotationY: Float = 0f,
    val pivotX: Float = 0.5f,
    val shiftX: Float = 0f,
    val parallax: Boolean = false,
)

/**
 * The ways one home page gives way to the next. Each is a plain function of how far the page is
 * from the middle of the screen — in pages, positive when it lies to the left — so they can be
 * checked without a screen, and every one of them is exactly the plain page at rest.
 */
object PageEffects {

    val styles: List<String> = listOf("depth", "slide", "cube", "stack")

    fun transform(style: String, offset: Float): PageTransform {
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
            // A deck: the page on the left stays where it is and sinks, the next one comes over it.
            "stack" -> if (at > 0f) {
                PageTransform(scale = 1f - 0.12f * at, alpha = 1f - at, shiftX = at)
            } else {
                PageTransform()
            }
            // Depth, and anything unknown: the page tips back and fades a little as it passes.
            else -> PageTransform(
                scale = 0.94f + 0.06f * (1f - distance),
                alpha = 1f - 0.4f * distance,
                parallax = true,
            )
        }
    }

    /**
     * How much of the active pill dot [index] has while the pager is at [position] (a page number,
     * with a fraction while turning): 1 for the page in view, shared between two dots in between.
     */
    fun dotWidth(position: Float, index: Int): Float = (1f - abs(position - index)).coerceIn(0f, 1f)
}
