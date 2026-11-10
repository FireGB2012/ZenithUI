package com.gabriel.zenithui.ui.home

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp

// The Search pill's rules, kept apart from drawing it so they can be checked without a screen: when
// its words give way to the page dots, and where its glass is while it stretches into the search
// field and shrinks back into the pill.

/**
 * Whether the Search pill shows the page dots rather than its words: while the pages are moving,
 * and until [PILL_DOTS_LINGER_MS] after they last came to rest, [msSinceSettled] ago, so the dots
 * do not blink out between two swipes in a row and the words come back once you have stopped. A
 * time below nought cannot be, and is taken as long ago, so the words are never stuck behind the
 * dots.
 */
fun pillDots(pagerMoving: Boolean, msSinceSettled: Long): Boolean =
    pagerMoving || msSinceSettled in 0L until PILL_DOTS_LINGER_MS

/** How long the page dots stay in the pill once the pages have come to rest: about a second. */
const val PILL_DOTS_LINGER_MS = 1000L

/**
 * Where the pill's glass is, [progress] of the way from the [pill] to the search [field]: 0 is the
 * pill and 1 the field, and each edge moves straight from one to the other, so half way is half way
 * on every edge. The spring it rides on carries it a little past either end before it settles, and
 * the glass goes with it, past the field or the pill in proportion: that is the wobble. The glass is
 * painted at this rectangle, rather than the pill being stretched by a layer, because a stretched
 * layer would thicken the rim, bend the corners and magnify the wallpaper.
 */
fun pillToField(progress: Float, pill: Rect, field: Rect): Rect = lerp(pill, field, progress)
