package com.gabriel.zenithui.ui.common

/**
 * A sheet under a finger. Pulled down, it goes exactly as far as it is pulled. Pulled up, where it
 * has nowhere to go, it gives a little, and less the further, like something on elastic, and never
 * as much as it has to give. Sums only, so they can be checked without a screen.
 */
object SheetPull {

    /** A flick faster than this, in pixels a second, decides by itself. */
    const val FLING = 900f

    /** Let go further down than this share of its own height, a sheet goes away. */
    const val AWAY = 0.3f

    /**
     * Where a sheet pulled by [raw] pixels is drawn, in pixels down from where it rests. [give] is
     * the most it will ever come up.
     */
    fun shown(raw: Float, give: Float): Float {
        if (raw >= 0f || give <= 0f) return raw.coerceAtLeast(0f)
        return -give * (1f - 1f / (1f + -raw / (give * 2f)))
    }

    /**
     * The pull that draws a sheet at [shown]: the way back from [SheetPull.shown], for taking hold
     * of a sheet that is still on its way to rest without it jumping under the finger.
     */
    fun raw(shown: Float, give: Float): Float {
        if (shown >= 0f || give <= 0f) return shown.coerceAtLeast(0f)
        val share = (-shown / give).coerceAtMost(0.999f)
        return -give * 2f * share / (1f - share)
    }

    /**
     * Whether a sheet let go [shown] pixels down, moving at [velocity] pixels a second (down is
     * positive), goes away. A flick decides, either way; without one, how far down it is does.
     */
    fun dismisses(shown: Float, height: Float, velocity: Float): Boolean = when {
        velocity > FLING -> true
        velocity < -FLING -> false
        else -> shown > height * AWAY
    }
}
