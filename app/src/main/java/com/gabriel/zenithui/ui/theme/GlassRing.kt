package com.gabriel.zenithui.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.channels.Channel
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max

// A drop on the glass, the home screen's fourth twist: tap an empty bit of wallpaper and a ring of
// light runs out from the finger across the screen, and every pane of glass lights up, rim and face,
// as it passes over it. There is one ring for the whole screen, read by every pane while drawing, as
// the tilt is. These are its rules, kept apart from drawing so they can be tested without a phone.

/**
 * A ring as it is at one moment: where it started, [centre]; how far it has run, [radius]; how wide
 * its band of light is, [width]; and how bright it is, [strength], from 1 under the finger down to
 * 0 as it runs out. All in pixels: on the screen as the ring keeps it, and in a pane's own pixels as
 * a pane draws it.
 */
@Immutable
data class RingNow(val centre: Offset, val radius: Float, val width: Float, val strength: Float)

/**
 * How far a ring has run [elapsedMs] after the tap, in pixels, to have run [reachPx] by [durationMs]:
 * fast at first and slowing as it spreads, as a ring on water does (a cubic ease-out).
 */
fun ringRadius(elapsedMs: Float, reachPx: Float, durationMs: Float = RING_MS): Float {
    val left = 1f - ringProgress(elapsedMs, durationMs)
    return reachPx * (1f - left * left * left)
}

/** How bright a ring is [elapsedMs] after the tap: all of it under the finger, and none once it has run out. */
fun ringStrength(elapsedMs: Float, durationMs: Float = RING_MS): Float = 1f - ringProgress(elapsedMs, durationMs)

/**
 * How much of [ring]'s light falls on something [distancePx] from where the ring started: all of its
 * strength where that is as far out as the ring has run, falling away either side across its band,
 * `strength * exp(-((distance - radius) / width)^2)`, to under a fiftieth two widths off.
 */
fun paneLight(distancePx: Float, ring: RingNow): Float {
    // A band with no width has nowhere for its light to fall.
    if (ring.width <= 0f) return 0f
    val off = (distancePx - ring.radius) / ring.width
    return ring.strength * exp(-off * off)
}

/** How far through its run a ring is [elapsedMs] after the tap: 0 at the tap, 1 once it has run out. */
private fun ringProgress(elapsedMs: Float, durationMs: Float): Float =
    if (durationMs <= 0f) 1f else (elapsedMs / durationMs).coerceIn(0f, 1f)

/**
 * How wide a ring's band of light is [elapsedMs] after the tap, in pixels at [density] pixels to the
 * dp: it widens a little as it spreads, as a ring on water does, from [RING_WIDTH_DP] by [RING_WIDEN_DP].
 */
internal fun ringWidth(elapsedMs: Float, density: Float, durationMs: Float = RING_MS): Float =
    (RING_WIDTH_DP + RING_WIDEN_DP * ringProgress(elapsedMs, durationMs)) * density

/**
 * How far a ring from [at] has to run to cross the whole of [screen]: to the corner farthest from
 * it, so that it passes over every pane on the screen on its way.
 */
internal fun ringReach(at: Offset, screen: Size): Float =
    hypot(max(abs(at.x), abs(screen.width - at.x)), max(abs(at.y), abs(screen.height - at.y)))

/**
 * The ring of light a tap on the wallpaper sends out, one for the whole screen: where and when it
 * started ([start]), and how it is at any moment ([now]). Every pane reads it while drawing
 * ([drawn]), as the tilt is read, so a running ring redraws what it passes and composes nothing,
 * and once it has run out nothing is drawn again for it. [play] moves it on with the frames.
 *
 * A ring runs across [screen], in pixels, until its band has passed the corner farthest from where
 * it started; [density] is how many pixels make a dp, for the width of its band. Whoever shows the
 * ring keeps both up to date: they are read as a ring starts.
 */
@Stable
class GlassRing(var screen: Size = Size.Zero, var density: Float = 1f) {
    // Where and when the ring started, and how far it is to run: plain values, written only as a
    // ring starts. What tells the panes to draw again is the frame below.
    private var centre = Offset.Zero
    private var startedAt = 0L
    private var reach = 0f
    private var started = false

    /** The frame being drawn while a ring runs, and [IDLE] while none does: the one thing a pane reads. */
    private val frame = mutableLongStateOf(IDLE)

    /** Wakes [play] for a ring that has just started. */
    private val wake = Channel<Unit>(Channel.CONFLATED)

    /**
     * Sends a ring out from [at], on the screen in pixels, at [nowNanos] on the frame clock. One that
     * is still running gives way to it.
     */
    fun start(at: Offset, nowNanos: Long) {
        centre = at
        startedAt = nowNanos
        // By the time it ends its band has passed the farthest corner, not just reached it.
        reach = ringReach(at, screen) + 2f * ringWidth(RING_MS, density)
        started = true
        wake.trySend(Unit)
    }

    /** The ring at [nowNanos] on the frame clock: null before any has started, and once it has run out. */
    fun now(nowNanos: Long): RingNow? {
        if (!started) return null
        // A frame drawn the moment a ring starts can be stamped a little before it.
        val elapsed = ((nowNanos - startedAt) / NANOS_PER_MS).coerceAtLeast(0f)
        if (elapsed >= RING_MS) return null
        return RingNow(centre, ringRadius(elapsed, reach), ringWidth(elapsed, density), ringStrength(elapsed))
    }

    /** The ring on the frame being drawn, or null while none is running: for reading while drawing. */
    fun drawn(): RingNow? {
        val at = frame.longValue
        return if (at == IDLE) null else now(at)
    }

    /**
     * Moves each ring on with the frames while it runs, and waits, writing nothing, while none does.
     * Runs until it is cancelled, in the composition that shows the ring.
     */
    suspend fun play() {
        try {
            while (true) {
                wake.receive()
                do {
                    val running = withFrameNanos { nanos -> step(nanos) }
                } while (running)
            }
        } finally {
            frame.longValue = IDLE
        }
    }

    /**
     * One frame, at [nanos] on the frame clock: true while the ring is still running. The frame it
     * runs out on says so at once, so the panes draw without it once rather than twice.
     */
    private fun step(nanos: Long): Boolean {
        val running = now(nanos) != null
        frame.longValue = if (running) nanos else IDLE
        return running
    }
}

/**
 * The ring of light from a tap on the wallpaper, for every pane on the home screen to read while it
 * draws ([GlassRing.drawn]). Null where no ring is sent out: everywhere but the home screen.
 */
val LocalGlassRing = staticCompositionLocalOf<GlassRing?> { null }

/** How long a ring takes to run out and fade, in milliseconds. */
internal const val RING_MS = 1200f

/** How wide a ring's band of light is as it leaves the finger, and how much wider it gets, in dp. */
private const val RING_WIDTH_DP = 20f
private const val RING_WIDEN_DP = 16f

/** The frame of a ring that is not running. No frame of the clock is ever stamped with it. */
private const val IDLE = Long.MIN_VALUE

private const val NANOS_PER_MS = 1_000_000f
