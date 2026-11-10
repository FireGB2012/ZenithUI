package com.gabriel.zenithui.ui.theme

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

/** How wide a ring's band of light is as it leaves the finger, and how much wider it gets, in dp. */
private const val RING_WIDTH_DP = 20f
private const val RING_WIDEN_DP = 16f

/** The frame of a ring that is not running. No frame of the clock is ever stamped with it. */
private const val IDLE = Long.MIN_VALUE

private const val NANOS_PER_MS = 1_000_000f
