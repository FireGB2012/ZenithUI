package com.gabriel.zenithui.ui.home

import kotlin.math.roundToInt

/**
 * The home screen arriving. Everything starts a little further from the middle of the screen than
 * it belongs, and comes in to its place — what is nearest the middle first, the edges last — on
 * one clock for the whole page, so a page of forty icons costs one animation and not forty.
 *
 * Coming home from the lock screen or a cold start it is the full fly-in; coming back from an app
 * it is the same movement over a fraction of the distance: a settle.
 */
object FlyIn {

    /** From the first item moving to the last one down, in milliseconds. */
    const val TOTAL_MS = 600

    /** How long any one item takes over its own journey. */
    private const val TRAVEL_MS = 380

    /** How far out an item starts, as a share of its own distance from the middle. */
    const val SPREAD = 0.5f

    /** The depth of a full fly-in, and of the settle on the way back from an app. */
    const val FULL = 1f
    const val SETTLE = 0.2f

    /** How far past its place an item goes before it comes back. */
    private const val OVERSHOOT = 1.1f

    /**
     * When an item sets off. [distance] is how far its middle is from the middle of the page: 0 at
     * the middle, 1 at the far corner.
     */
    fun delayMs(distance: Float): Int = ((TOTAL_MS - TRAVEL_MS) * distance.coerceIn(0f, 1f)).roundToInt()

    /**
     * How far along an item is at [clockMs] on the page's clock: 0 where it starts, 1 in its place,
     * a little over 1 for a moment on the way.
     */
    fun progress(clockMs: Float, delayMs: Int): Float {
        val t = (clockMs - delayMs) / TRAVEL_MS
        // The two ends exactly, not to within a rounding: an item at rest must not be a hair out.
        if (t <= 0f) return 0f
        if (t >= 1f) return 1f
        val back = t - 1f
        return 1f + (OVERSHOOT + 1f) * back * back * back + OVERSHOOT * back * back
    }
}
