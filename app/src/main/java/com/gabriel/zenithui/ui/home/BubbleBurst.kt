package com.gabriel.zenithui.ui.home

import android.os.SystemClock
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.dp
import com.gabriel.zenithui.data.PxRect
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * What is popping off the home screen and what is blowing back onto it. Something taken off it
 * pops like a soap bubble, where it stood: a quick swell, a ring and a few droplets. Undo brings it
 * back the other way, its droplets flying in and the thing forming again with a wobble. The home
 * screen says which ids are which as it takes them off or puts them back; the page plays them.
 */
@Stable
class Bubbles {
    /** A pop: when it was asked for, and how many droplets it throws. */
    data class Pop(val at: Long, val droplets: Int)

    // Read once, as what pops or comes back is first drawn going or coming: nothing watches them.
    private val popping = HashMap<String, Pop>()
    private val returning = HashMap<String, Long>()
    private var lastPop = -1L

    /** [ids] are about to be taken off the home screen: each pops rather than shrinks away. */
    fun pop(ids: Collection<String>) {
        val now = SystemClock.uptimeMillis()
        val drops = BeadMotion.droplets(if (lastPop < 0L) Long.MAX_VALUE else now - lastPop)
        lastPop = now
        forget(now)
        ids.forEach { popping[it] = Pop(now, drops) }
    }

    /** [ids] have just been brought back by Undo: each blows back together. */
    fun back(ids: Collection<String>) {
        val now = SystemClock.uptimeMillis()
        forget(now)
        ids.forEach { returning[it] = now }
    }

    /** The pop asked for [id], if it was asked for just now; it is played once, so it is taken. */
    fun takePop(id: String): Pop? = popping.remove(id)?.takeIf { SystemClock.uptimeMillis() - it.at < FRESH_MS }

    /** Whether [id] has just been brought back by Undo; it blows back once, so this is taken. */
    fun takeReturn(id: String): Boolean = returning.remove(id)?.let { SystemClock.uptimeMillis() - it < FRESH_MS } == true

    private fun forget(now: Long) {
        popping.entries.removeAll { now - it.value.at >= FRESH_MS }
        returning.entries.removeAll { now - it.value >= FRESH_MS }
    }

    private companion object {
        /** Long enough for a page turned to on Undo to show it; any later it is old news. */
        const val FRESH_MS = 1500L
    }
}

/** The bubble's ring spreads quickly and slows; on the way back in it gathers speed. */
private val RING_OUT = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)
private val DROPS_OUT = CubicBezierEasing(0.15f, 0.7f, 0.35f, 1f)
private val DROPS_IN = CubicBezierEasing(0.45f, 0f, 0.7f, 0.7f)
private val RING_IN = CubicBezierEasing(0.42f, 0f, 1f, 1f)

/**
 * A soap bubble bursting round [centre], [ms] after it burst: its ring spreading and fading, and
 * [droplets] flying off. With [inward] it plays the other way, closing in on [centre]. [span] is
 * how big what burst was, in pixels.
 */
internal fun DrawScope.drawBurst(centre: Offset, span: Float, ms: Float, droplets: Int, inward: Boolean) {
    val size = min(span * 0.9f, 170.dp.toPx())
    // The ring: a thin band of white with the colours a soap film has at its edge.
    val ringT = ms / (if (inward) BeadMotion.RING_IN_MS else BeadMotion.RING_MS)
    if (ringT in 0f..1f) {
        val (scale, alpha) = if (inward) {
            val e = RING_IN.transform(ringT)
            (1.5f - 0.8f * e) to 0.9f * e
        } else {
            val e = RING_OUT.transform(ringT)
            (0.7f + 0.85f * e) to 0.95f * (1f - e)
        }
        val radius = size * 1.25f / 2f * scale
        if (radius > 0f && alpha > 0f) {
            drawCircle(
                Brush.radialGradient(
                    0f to Color.Transparent,
                    0.55f to Color.Transparent,
                    0.62f to Color.White.copy(alpha = 0.9f),
                    0.66f to Color(0x99BEE1FF),
                    0.69f to Color(0x66FFCDF0),
                    0.73f to Color.Transparent,
                    1f to Color.Transparent,
                    center = centre,
                    radius = radius,
                ),
                radius = radius,
                center = centre,
                alpha = alpha.coerceIn(0f, 1f),
            )
        }
    }
    // The droplets: each its own way round, a little further than the bubble's edge.
    val dropT = ms / BeadMotion.DROPLETS_MS
    if (dropT !in 0f..1f) return
    val e = if (inward) DROPS_IN.transform(dropT) else DROPS_OUT.transform(dropT)
    val fall = 10.dp.toPx()
    for (i in 0 until droplets) {
        val d = BeadMotion.droplet(i, droplets)
        val reach = size * 0.5f + 16.dp.toPx() + 16.dp.toPx() * d.reach
        val dx = cos(d.angle) * reach
        val dy = sin(d.angle) * reach
        val dot = 4.dp.toPx() + 4.dp.toPx() * d.size
        // Out: from near the middle to their reach, then falling a little as they fade. In: the
        // same, the other way, gone again as they arrive.
        val (at, s, a) = if (!inward) {
            if (e < 0.6f) {
                val f = e / 0.6f
                Triple(Offset(dx * (0.2f + 0.8f * f), dy * (0.2f + 0.8f * f)), 1f - 0.1f * f, 1f - 0.1f * f)
            } else {
                val f = (e - 0.6f) / 0.4f
                Triple(Offset(dx, dy + fall * f), 0.9f - 0.4f * f, 0.9f * (1f - f))
            }
        } else {
            if (e < 0.8f) {
                val f = e / 0.8f
                Triple(Offset(dx * (1f - 0.8f * f), (dy + fall) * (1f - f) + dy * 0.2f * f), 0.5f + 0.5f * f, f)
            } else {
                val f = (e - 0.8f) / 0.2f
                Triple(Offset(dx * 0.2f * (1f - f), dy * 0.2f * (1f - f)), 1f - 0.4f * f, 1f - f)
            }
        }
        drawDroplet(centre + at, dot * s / 2f, a.coerceIn(0f, 1f))
    }
}

/** A droplet of soapy water: clear, lit from the top left, with a bright rim and a faint shadow. */
private fun DrawScope.drawDroplet(at: Offset, radius: Float, alpha: Float) {
    if (radius <= 0f || alpha <= 0f) return
    drawCircle(Color.Black.copy(alpha = 0.22f * alpha), radius = radius, center = at + Offset(0f, 1.dp.toPx()))
    drawCircle(
        Brush.radialGradient(
            0f to Color.White,
            0.45f to Color.White.copy(alpha = 0.45f),
            0.72f to Color.White.copy(alpha = 0.18f),
            1f to Color.White.copy(alpha = 0.18f),
            center = at + Offset(-radius * 0.32f, -radius * 0.4f),
            radius = radius * 1.4f,
        ),
        radius = radius,
        center = at,
        alpha = alpha,
    )
    drawCircle(Color.White.copy(alpha = 0.8f * alpha), radius = radius, center = at, style = Stroke(0.8.dp.toPx()))
}

/** The middle of what stands in [frame], for its bubble to burst round. */
internal fun burstCentre(frame: PxRect): Offset = Offset((frame.left + frame.right) / 2f, (frame.top + frame.bottom) / 2f)

/** How big what stands in [frame] is, for its bubble. */
internal fun burstSpan(frame: PxRect): Float = maxOf(frame.width, frame.height).toFloat()

/** How long a burst lasts, from the moment the thing starts to swell. */
internal const val BURST_MS = BeadMotion.RING_DELAY_MS + BeadMotion.DROPLETS_MS

/** What [Bubbles] calls the dock's [key], which could otherwise be taken for something on a page. */
internal fun dockBubble(key: String): String = "dock:$key"

/**
 * Draws what is in it [bright] times as bright, read while drawing: what pops catches the light
 * just as it bursts. At 1 it is drawn as it is, with no layer of its own.
 */
internal fun Modifier.brightened(bright: () -> Float): Modifier = drawWithCache {
    val paint = Paint()
    onDrawWithContent {
        val k = bright()
        if (k <= 1.001f) {
            drawContent()
        } else {
            paint.colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(k, k, k, 1f) })
            drawIntoCanvas { it.saveLayer(Rect(Offset.Zero, size), paint) }
            drawContent()
            drawIntoCanvas { it.restore() }
        }
    }
}
