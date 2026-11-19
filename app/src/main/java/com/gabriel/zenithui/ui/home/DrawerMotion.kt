package com.gabriel.zenithui.ui.home

import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The drawer's apps rising into their places as it opens: a row at a time from the top, and along
 * each row from the left, every one on the spring Motion.rise is. Worked out from one clock for the
 * whole drawer rather than an animation of each icon's own, so opening it starts one thing and
 * composes nothing: each icon reads how far it has risen while it is drawn. Plain arithmetic, so
 * the timing can be tested without a phone.
 */
object DrawerRise {

    /** How long after the row above a row of apps sets off. */
    const val ROW_TURN_MS = 46

    /** How long after the app beside it an app sets off, along its row. */
    const val ICON_TURN_MS = 14

    /**
     * How long one app takes to come to rest from its turn. Motion.rise is within a thousandth of its
     * place by then, so putting it there exactly is a step too small to see.
     */
    const val SETTLE_MS = 640

    /** Motion.rise: damped at 0.8, at a stiffness of 220. */
    private const val DAMPING = 0.8
    private const val STIFFNESS = 220.0

    /** When the app at [index] sets off, with [columns] apps to a row. */
    fun turn(index: Int, columns: Int): Int {
        val across = columns.coerceAtLeast(1)
        return (index / across) * ROW_TURN_MS + (index % across) * ICON_TURN_MS
    }

    /**
     * How far into its place an app has risen [elapsedMs] after the drawer began to open, when its
     * turn comes [turnMs] after that: 0 until its turn, then up on the spring, a touch past its place
     * and back, and exactly 1 once it has settled.
     */
    fun progress(elapsedMs: Float, turnMs: Int): Float {
        val ms = elapsedMs - turnMs
        if (ms <= 0f) return 0f
        if (ms >= SETTLE_MS) return 1f
        val t = ms / 1000.0
        val swing = sqrt(STIFFNESS)
        val damped = swing * sqrt(1.0 - DAMPING * DAMPING)
        val decay = exp(-DAMPING * swing * t)
        return (1.0 - decay * (cos(damped * t) + DAMPING * swing / damped * sin(damped * t))).toFloat()
    }

    /** How long after the drawer began to open the last of [count] apps has settled: the clock stops then. */
    fun totalMs(count: Int, columns: Int): Int = if (count <= 0) 0 else turn(count - 1, columns) + SETTLE_MS
}

/**
 * The home screen going out of focus behind the drawer. A blur that grows as the drawer opens has to
 * be worked out again by Android on every frame of the swipe, the whole screen over; instead one
 * blurred copy of the home screen, worked out once, comes in over the sharp one as the drawer
 * opens, which costs no more than a fade. Once the drawer is open that copy is the home screen's
 * blur, live: it follows anything that changes under it.
 */
object DrawerFrost {

    /** How far open the drawer is when the blurred copy starts to come in, and when it is all in. */
    const val FROM = 0.2f
    const val TO = 0.6f

    /** How much of the blurred copy is seen with the drawer [progress] open, 0 to 1, eased in and out. */
    fun amount(progress: Float): Float {
        val x = ((progress - FROM) / (TO - FROM)).coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }

    /**
     * How much of the sharp home screen is still drawn under the blurred copy, [amount] of the way in.
     * Nearly all of it at first, so the two together hardly let the wallpaper through more than
     * either alone would; none once the copy is all in, so nothing sharp shows where the blur has
     * spread an icon thin.
     */
    fun sharpAlpha(amount: Float): Float {
        val x = amount.coerceIn(0f, 1f)
        return 1f - x * x * x
    }

    /**
     * Where nothing is blurred (a theme that blurs nothing, and Lite, which Auto picks on a phone
     * like the A55), a veil of the theme's page dims the wallpaper behind the drawer and the library
     * instead: this much of it with the drawer open, as dark as it used to look. It lies over the
     * whole screen. It was drawn inside the home screen, which sinks back to 94% behind the drawer,
     * so it shrank with it and stood in from every edge: a frame round the drawer and the library.
     */
    const val VEIL = 0.2f

    /** How much of the page's veil is over the wallpaper with the blurred copy [amount] of the way in. */
    fun veil(amount: Float): Float = VEIL * amount.coerceIn(0f, 1f)
}
