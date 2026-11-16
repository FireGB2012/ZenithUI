package com.gabriel.zenithui.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.Channel
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max

// A drop on the glass, the home screen's fourth twist: tap an empty bit of wallpaper and a ring of
// light runs out from the finger across the screen, and every pane of glass lights up, rim and face,
// as it passes over it. There is one ring for the whole screen, read by every pane while drawing, as
// the tilt is. Its rules are kept apart from drawing so they can be tested without a phone; how it is
// drawn on the wallpaper, and the gleam it puts on what is not glass, are at the end.
//
// Edit mode sends the same ring as a wave: a quicker one that wakes everything on the home screen as
// it reaches it, and a slower, fainter one that settles it all again ([RingStyle]).

/**
 * A ring as it is at one moment: where it started, [centre]; how far it has run, [radius]; how wide
 * its band of light is, [width]; and how bright it is, [strength], from 1 under the finger down to
 * 0 as it runs out. All in pixels: on the screen as the ring keeps it, and in a pane's own pixels as
 * a pane draws it.
 */
@Immutable
data class RingNow(val centre: Offset, val radius: Float, val width: Float, val strength: Float)

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
 * How a ring runs: how long it takes ([durationMs]); how far out it is at each moment ([spreadAt],
 * from how far through its run it is to how much of its reach it has crossed, both 0 to 1); and how
 * bright it is then ([strengthAt]), at most [peak]. [TAP] is a tap on the wallpaper's ring; [WAKE]
 * the quicker wave that wakes the home screen as editing starts; [CALM] the slower, fainter one that
 * settles it as editing ends.
 */
@Immutable
class RingStyle(
    val durationMs: Float,
    private val spread: (Float) -> Float,
    private val fade: (Float) -> Float,
    val peak: Float = 1f,
) {
    fun spreadAt(progress: Float): Float = spread(progress.coerceIn(0f, 1f))

    fun strengthAt(progress: Float): Float = peak * fade(progress.coerceIn(0f, 1f))

    /** How far through its run, 0 to 1, a ring of this kind is as it has crossed [part] of its reach. */
    fun progressAt(part: Float): Float {
        if (part <= 0f) return 0f
        if (part >= 1f) return 1f
        // Every kind spreads outwards the whole time, so the moment can be closed in on.
        var early = 0f
        var late = 1f
        repeat(32) {
            val mid = (early + late) / 2f
            if (spreadAt(mid) < part) early = mid else late = mid
        }
        return (early + late) / 2f
    }

    companion object {
        /**
         * Fast at first and slowing as it spreads, as a ring on water does (a cubic ease-out), and
         * all its light under the finger, fading evenly to none as it runs out.
         */
        val TAP = RingStyle(RING_MS, spread = { 1f - (1f - it) * (1f - it) * (1f - it) }, fade = { 1f - it })
        val WAKE = RingStyle(WAKE_RING_MS, spread = WAVE_CURVE::transform, fade = ::waveFade)
        val CALM = RingStyle(CALM_RING_MS, spread = WAVE_CURVE::transform, fade = ::waveFade, peak = CALM_PEAK)
    }
}

/** A wave's light: most of it until a little past halfway, then fading to nothing as it runs out. */
private fun waveFade(progress: Float): Float =
    if (progress < WAVE_HOLD) 1f - (1f - WAVE_HELD) * progress / WAVE_HOLD
    else WAVE_HELD * (1f - (progress - WAVE_HOLD) / (1f - WAVE_HOLD))

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
    // Where and when the ring started, how far it is to run and how: plain values, written only as
    // a ring starts. What tells the panes to draw again is the frame below.
    private var centre = Offset.Zero
    private var startedAt = 0L
    private var reach = 0f
    private var started = false
    private var style = RingStyle.TAP

    /** Started, but to be timed from the next frame drawn, which has not come yet. */
    private var pending = false

    /** The frame being drawn while a ring runs, and [IDLE] while none does: the one thing a pane reads. */
    private val frame = mutableLongStateOf(IDLE)

    /** Wakes [play] for a ring that has just started. */
    private val wake = Channel<Unit>(Channel.CONFLATED)

    /**
     * Sends a ring of [style] out from [at], on the screen in pixels, at [nowNanos] on the frame
     * clock. One that is still running gives way to it.
     */
    fun start(at: Offset, nowNanos: Long, style: RingStyle = RingStyle.TAP) {
        centre = at
        startedAt = nowNanos
        pending = false
        this.style = style
        // By the time it ends its band has passed the farthest corner, not just reached it.
        reach = ringReach(at, screen) + 2f * ringWidth(style.durationMs, density, style.durationMs)
        started = true
        wake.trySend(Unit)
    }

    /**
     * Sends a ring of [style] out from [at], timed from the next frame drawn: for a ring sent where
     * there is no frame time to hand, so that it starts exactly as its first frame is drawn.
     */
    fun startOnNextFrame(at: Offset, style: RingStyle) {
        start(at, 0L, style)
        pending = true
    }

    /** The ring at [nowNanos] on the frame clock: null before any has started, and once it has run out. */
    fun now(nowNanos: Long): RingNow? {
        if (!started || pending) return null
        // A frame drawn the moment a ring starts can be stamped a little before it.
        val elapsed = ((nowNanos - startedAt) / NANOS_PER_MS).coerceAtLeast(0f)
        val run = style.durationMs
        if (elapsed >= run) return null
        val progress = elapsed / run
        return RingNow(centre, reach * style.spreadAt(progress), ringWidth(elapsed, density, run), style.strengthAt(progress))
    }

    /**
     * How long from [nowNanos] on the frame clock, in milliseconds, until the ring running now
     * reaches [at] on the screen: 0 once it has passed there, for somewhere off the screen, and with
     * no ring running. A ring still waiting for its first frame is counted from that frame.
     */
    fun reachesIn(at: Offset, nowNanos: Long): Float {
        if (!started) return 0f
        if (at.x < 0f || at.y < 0f || at.x > screen.width || at.y > screen.height) return 0f
        val elapsed = if (pending) 0f else ((nowNanos - startedAt) / NANOS_PER_MS).coerceAtLeast(0f)
        val run = style.durationMs
        if (elapsed >= run || reach <= 0f) return 0f
        val arrives = style.progressAt((at - centre).getDistance() / reach) * run
        return (arrives - elapsed).coerceAtLeast(0f)
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
        if (pending) {
            startedAt = nanos
            pending = false
        }
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

/**
 * Draws [ring] behind what this holds, on the wallpaper itself: a soft band of white with a fine line
 * of light along it, fading as it runs out, under everything standing on the wallpaper. Glass shows
 * the wallpaper and not what Zenith draws on it, so the ring is not seen through a pane: each pane
 * lights up of itself as the ring passes, and icons and colour widgets gleam ([ringGleam]).
 */
@Composable
fun Modifier.ringOfLight(ring: GlassRing): Modifier {
    // Where this is on screen, kept from its layout, to bring the ring's centre into its own pixels.
    val origin = remember { arrayOf(Offset.Zero) }
    return this
        .onGloballyPositioned { origin[0] = it.positionInWindow() }
        .drawBehind {
            val now = ring.drawn() ?: return@drawBehind
            drawRingOfLight(now, now.centre - origin[0])
        }
}

/** [ring] as the wallpaper shows it, round [centre] in this scope's pixels. */
private fun DrawScope.drawRingOfLight(ring: RingNow, centre: Offset) {
    val reach = ring.radius + 3f * ring.width
    if (ring.strength <= 0f || reach <= 0f) return
    // The band falls away either side of the ring as the light it puts on a pane does (paneLight),
    // taken at whole widths for the gradient to blend between. While the ring has hardly left the
    // finger, what would lie nearer than the middle is the middle.
    val stops = ArrayList<Pair<Float, Color>>(8)
    stops += 0f to Color.White.copy(alpha = RING_BAND * paneLight(0f, ring))
    for (step in -3..3) {
        val out = ring.radius + step * ring.width
        if (out > 0f) stops += (out / reach) to Color.White.copy(alpha = RING_BAND * ring.strength * exp(-(step * step).toFloat()))
    }
    drawCircle(Brush.radialGradient(*stops.toTypedArray(), center = centre, radius = reach), radius = reach, center = centre)
    if (ring.radius > 1f) {
        drawCircle(
            color = Color.White.copy(alpha = RING_LINE * ring.strength),
            radius = ring.radius,
            center = centre,
            style = Stroke(RING_LINE_DP.dp.toPx()),
        )
    }
}

/**
 * The faint gleam a ring of light from a tap on the wallpaper puts on what is not glass as it passes
 * over it, an icon or a colour widget's card: white, inside [shape], as much as [GLEAM] of it where
 * the ring is brightest. It is as bright as the ring is where the middle of this is. Glass lights up
 * of itself and takes none of this; away from the home screen ([LocalGlassRing]) nothing is drawn.
 */
@Composable
fun Modifier.ringGleam(shape: Shape): Modifier {
    val ring = LocalGlassRing.current ?: return this
    // The middle of this on screen, kept from its layout and read while drawing.
    val middle = remember { arrayOf(Offset.Unspecified) }
    return this
        .onGloballyPositioned { middle[0] = it.positionInWindow() + Offset(it.size.width / 2f, it.size.height / 2f) }
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            onDrawWithContent {
                drawContent()
                // Read here, while drawing, so a ring passing redraws this and composes nothing.
                val now = ring.drawn() ?: return@onDrawWithContent
                val at = middle[0]
                if (!at.isSpecified) return@onDrawWithContent
                val lit = paneLight((at - now.centre).getDistance(), now)
                if (lit > 0.004f) drawOutline(outline, Color.White.copy(alpha = GLEAM * lit))
            }
        }
}

/** How much white the ring's band is on the wallpaper at its brightest, and the line along it. */
private const val RING_BAND = 0.26f
private const val RING_LINE = 0.6f
private const val RING_LINE_DP = 1.5f

/** The most white a passing ring puts on an icon or a colour widget. */
private const val GLEAM = 0.18f

/** How long a ring takes to run out and fade, in milliseconds. */
internal const val RING_MS = 1200f

/** How long the wave that wakes the home screen takes to cross it, and the one that settles it. */
private const val WAKE_RING_MS = 640f
private const val CALM_RING_MS = 950f

/** A wave quick off the finger and slowing as it spreads. */
private val WAVE_CURVE = CubicBezierEasing(0.2f, 0.55f, 0.45f, 1f)

/** How much of its light a wave keeps until how far through its run, before it fades. */
private const val WAVE_HELD = 0.85f
private const val WAVE_HOLD = 0.55f

/** How bright the settling wave is beside the waking one. */
private const val CALM_PEAK = 0.6f

/** How wide a ring's band of light is as it leaves the finger, and how much wider it gets, in dp. */
private const val RING_WIDTH_DP = 20f
private const val RING_WIDEN_DP = 16f

/** The frame of a ring that is not running. No frame of the clock is ever stamped with it. */
private const val IDLE = Long.MIN_VALUE

private const val NANOS_PER_MS = 1_000_000f
