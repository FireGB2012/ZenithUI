package com.gabriel.zenithui.ui.home

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import com.gabriel.zenithui.data.AnimationTuning
import com.gabriel.zenithui.data.Tune
import com.gabriel.zenithui.ui.theme.RingStyle
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The home screen coming home like liquid. A wave runs out across the screen from where the app went
 * back into its icon, or from the middle of the screen after an unlock, and everything on the home
 * screen rises as the wave reaches it: from a little small, faint, and a little nearer where the wave
 * started, up into its place on a spring that carries it a hair past and settles it. A ring of light
 * runs with the wave ([light], sent on the home screen's one `GlassRing`), so each pane of glass
 * lights up, rim and face, and each icon gleams, as it rises.
 *
 * One wave for the whole screen, worked out once as it sets off: each thing only asks it how far on
 * it is ([LiquidSpot]), on the home screen's one arrival clock, so forty icons are still one
 * animation, and the clock ticking composes nothing.
 */
@Immutable
class LiquidWave(
    /** Where it starts, on the screen in pixels. */
    val centre: Offset,
    /** How far it runs, in pixels: as far as its light does, past the farthest corner of the screen. */
    val reach: Float,
    /** The light running with it, which sets its pace: quick off the start, slowing as it spreads. */
    val light: RingStyle,
    /** The spring each thing rises on: how hard it pulls, and how soon it is still (1 stops dead). */
    val stiffness: Float,
    val damping: Float,
    /** How big each thing is as it starts to rise: 1 is its own size. */
    val from: Float,
    /** How much nearer [centre] the things farthest from it start, in pixels. */
    val pull: Float,
    /** How long before the wave's front reaches something it starts to rise, in milliseconds. */
    val leadMs: Float,
) {
    /** How long the front takes to run all the way, in milliseconds. */
    val runMs: Float get() = light.durationMs

    /** How long one thing takes from setting off to being still, to within a hair, in milliseconds. */
    val settleMs: Float = LiquidArrival.settleMs(stiffness, damping)

    /** When everything is still, the last of it in the farthest corner: the end of the arrival. */
    val totalMs: Float = (runMs - leadMs).coerceAtLeast(0f) + settleMs

    /** How far out the front is at [clockMs] on the arrival clock, in pixels. */
    fun front(clockMs: Float): Float = reach * light.spreadAt(if (runMs > 0f) clockMs / runMs else 1f)

    /** When something [distance] pixels from [centre] sets off, in milliseconds on the arrival clock. */
    fun delayMs(distance: Float): Float {
        val part = if (reach > 0f) distance / reach else 1f
        return (light.progressAt(part) * runMs - leadMs).coerceAtLeast(0f)
    }

    /**
     * How far from its place something that set off at [delayMs] still is at [clockMs]: 1 before it
     * sets off, 0 in its place, and a little under 0 for a moment as it rises past.
     */
    fun out(clockMs: Float, delayMs: Float): Float = 1f - LiquidArrival.rise((clockMs - delayMs) / 1000f, stiffness, damping)

    /** How much nearer [centre] something [distance] pixels from it starts: the whole of [pull] once it is far out. */
    fun pullAt(distance: Float): Float =
        if (reach <= 0f) 0f else pull * (distance / (reach * LiquidArrival.PULL_SPAN)).coerceIn(0f, 1f)

    /** How big something is [out] from its place: [from] as it sets off, its own size in its place. */
    fun scaleAt(out: Float): Float = 1f - (1f - from) * out
}

/**
 * One thing on the home screen as a [LiquidWave] meets it: when it sets off, and where from, worked
 * out the first time it is asked about each wave, from where its middle is then, and kept for as
 * long as the wave runs, so nothing that moves it on the way changes when it rises.
 */
class LiquidSpot {
    private var met: LiquidWave? = null

    /** When it sets off, in milliseconds on the arrival clock. */
    var delayMs = 0f
        private set

    /** Where it sets off from, in pixels from its place: nearer where the wave started. */
    var startX = 0f
        private set
    var startY = 0f
        private set

    /** Meets [wave], with its middle at [at] on the screen, unless it already has. Somewhere not yet known is where the wave starts. */
    fun meet(wave: LiquidWave, at: () -> Offset) {
        if (met === wave) return
        met = wave
        val middle = at()
        val dx = if (middle.isSpecified) middle.x - wave.centre.x else 0f
        val dy = if (middle.isSpecified) middle.y - wave.centre.y else 0f
        val distance = hypot(dx, dy)
        delayMs = wave.delayMs(distance)
        if (distance > 0.5f) {
            val pull = wave.pullAt(distance)
            startX = -dx / distance * pull
            startY = -dy / distance * pull
        } else {
            startX = 0f
            startY = 0f
        }
    }

    /** How far from its place it is at [clockMs] on the wave it last met: 0 with none met. */
    fun out(clockMs: Float): Float = met?.out(clockMs, delayMs) ?: 0f
}

object LiquidArrival {

    /** Coming home set to Liquid. */
    const val STYLE = "liquid"

    /** How long before the wave reaches something it starts to rise, at the pace as written. */
    const val LEAD_MS = 90f

    /** How far out, as a share of the wave's reach, something is drawn in the whole of the pull. */
    const val PULL_SPAN = 0.5f

    /** Still: nearer its place than this share of the way it came. Far under a pixel for anything on the home screen. */
    const val STILL = 0.002f

    /** However slack the spring, the arrival is over in this long. */
    const val LONGEST_SETTLE_MS = 4000f

    /** The light of the wave: across the screen in the time set, at the pace set, as bright as set. */
    fun light(tuning: AnimationTuning): RingStyle =
        RingStyle.arrive(tuning[Tune.ArriveWave] / tuning.pace, tuning[Tune.ArriveLight])

    /**
     * The wave from [centre], on the screen in pixels, as [tuning] has it, at [pxPerDp] pixels to the
     * dp. [reach] is how far its light runs from there (`GlassRing.reachFrom`), so wave and light
     * keep step the whole way.
     */
    fun wave(centre: Offset, tuning: AnimationTuning, pxPerDp: Float, reach: (RingStyle) -> Float): LiquidWave {
        val light = light(tuning)
        return LiquidWave(
            centre = centre,
            reach = reach(light),
            light = light,
            stiffness = tuning.stiffness(Tune.ArriveStiffness),
            damping = tuning[Tune.ArriveDamping],
            from = tuning[Tune.ArriveFrom],
            pull = tuning[Tune.ArrivePull] * pxPerDp,
            leadMs = LEAD_MS / tuning.pace,
        )
    }

    /** How solid something is [out] from its place: not there at all as it sets off, quickly most of the way. */
    fun alphaAt(out: Float): Float {
        val o = out.coerceIn(0f, 1f)
        return 1f - o * o
    }

    /**
     * How far a spring as stiff as [stiffness] and as damped as [damping] has risen [seconds] after
     * it was let go, from 0 at rest to 1 in its place, as Compose's own springs move (a mass of 1):
     * past 1 for a moment when [damping] is under 1, never past it otherwise.
     */
    fun rise(seconds: Float, stiffness: Float, damping: Float): Float {
        if (seconds <= 0f) return 0f
        val omega = sqrt(stiffness.coerceAtLeast(1f))
        val zeta = damping.coerceAtLeast(0.01f)
        val t = seconds
        val left = when {
            abs(zeta - 1f) < 1e-3f -> exp(-omega * t) * (1f + omega * t)
            zeta < 1f -> {
                val swing = omega * sqrt(1f - zeta * zeta)
                exp(-zeta * omega * t) * (cos(swing * t) + zeta * omega / swing * sin(swing * t))
            }
            else -> {
                val spread = omega * sqrt(zeta * zeta - 1f)
                val slow = -zeta * omega + spread
                val fast = -zeta * omega - spread
                (fast * exp(slow * t) - slow * exp(fast * t)) / (fast - slow)
            }
        }
        return 1f - left
    }

    /** How long, in milliseconds, a spring like that takes to come within [STILL] of its place and stay there. */
    fun settleMs(stiffness: Float, damping: Float): Float {
        val zeta = damping.coerceAtLeast(0.01f)
        val seconds = if (zeta < 1f - 1e-3f) {
            // Its swings die away inside exp(-ζωt) / √(1 − ζ²): once that is under a hair, so is it.
            val omega = sqrt(stiffness.coerceAtLeast(1f))
            ln(1f / (STILL * sqrt(1f - zeta * zeta))) / (zeta * omega)
        } else {
            // It comes in without passing its place, ever more slowly: the moment it is a hair away.
            fun away(at: Float) = 1f - rise(at, stiffness, damping)
            var early = 0f
            var late = 0.1f
            while (away(late) > STILL && late < LONGEST_SETTLE_MS / 1000f) late *= 2f
            repeat(32) {
                val mid = (early + late) / 2f
                if (away(mid) > STILL) early = mid else late = mid
            }
            late
        }
        return (seconds * 1000f).coerceIn(0f, LONGEST_SETTLE_MS)
    }
}
