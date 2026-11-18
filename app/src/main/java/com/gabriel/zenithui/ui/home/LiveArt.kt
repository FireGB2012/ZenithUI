package com.gabriel.zenithui.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.gabriel.zenithui.data.LiveMotion
import com.gabriel.zenithui.data.LiveScene
import com.gabriel.zenithui.data.WallColors
import com.gabriel.zenithui.data.WallStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A design that moves, painted small for the wallpaper sheet's previews and posed on every frame as
 * the live wallpaper itself is ([LiveScene]): an idle design drifting, an unlock design opening over
 * and over from its lock pose. Null until its layers are painted; with movement reduced ([moving]
 * false) it holds its home pose. What it gives back draws it over a whole DrawScope, cropped to fill.
 */
@Composable
internal fun rememberLiveArt(style: WallStyle, colors: WallColors, widthPx: Int, heightPx: Int, moving: Boolean): (DrawScope.() -> Unit)? {
    var scene by remember(style, colors, widthPx, heightPx) { mutableStateOf<LiveScene?>(null) }
    LaunchedEffect(style, colors, widthPx, heightPx) {
        val made = withContext(Dispatchers.Default) {
            runCatching { LiveScene.of(style, colors, widthPx.toFloat(), heightPx.toFloat()).also { it.prepare() } }.getOrNull()
        } ?: return@LaunchedEffect
        scene = made
        try {
            awaitCancellation()
        } finally {
            // Nothing draws it once it is gone from here; the frames already drawn keep their own hold on it.
            scene = null
            made.release()
        }
    }
    val elapsed = remember(style) { mutableLongStateOf(0L) }
    val current = scene
    LaunchedEffect(current, moving) {
        if (current == null || !moving) return@LaunchedEffect
        val start = withFrameNanos { it } - elapsed.longValue * 1_000_000L
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                // 30 frames a second is plenty for a picture this size, and spares the sheet's own.
                if (now - last >= FRAME_NANOS) {
                    elapsed.longValue = (now - start) / 1_000_000L
                    last = now
                }
            }
        }
    }
    if (current == null) return null
    return remember(current, moving) {
        {
            val ms = if (moving) elapsed.longValue else 0L
            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                val scale = max(size.width / widthPx, size.height / heightPx)
                native.save()
                native.translate((size.width - widthPx * scale) / 2f, (size.height - heightPx * scale) / 2f)
                native.scale(scale, scale)
                if (style.live == LiveMotion.Unlock && moving) {
                    drawUnlock(native, current, ms)
                } else {
                    current.draw(native, ms / 1000.0, 1f)
                }
                native.restore()
            }
        }
    }
}

/**
 * An unlock design's loop: its home pose held, a quick fade back to the lock pose, the lock pose
 * held a moment, and the opening. It starts on the home pose, which is what the still of it shows.
 */
private fun drawUnlock(canvas: android.graphics.Canvas, scene: LiveScene, elapsedMs: Long) {
    val cycle = HOME_MS + RESET_MS + LOCK_MS + scene.openMs
    val at = elapsedMs % cycle
    when {
        at < HOME_MS -> scene.draw(canvas, 0.0, 1f)
        at < HOME_MS + RESET_MS -> {
            scene.draw(canvas, 0.0, 0f)
            val left = 1f - (at - HOME_MS) / RESET_MS.toFloat()
            canvas.saveLayerAlpha(null, (left * 255f).roundToInt().coerceIn(0, 255))
            scene.draw(canvas, 0.0, 1f)
            canvas.restore()
        }
        at < HOME_MS + RESET_MS + LOCK_MS -> scene.draw(canvas, 0.0, 0f)
        else -> scene.draw(canvas, 0.0, (at - HOME_MS - RESET_MS - LOCK_MS) / scene.openMs.toFloat())
    }
}

private const val FRAME_NANOS = 32_000_000L
private const val HOME_MS = 2_200L
private const val RESET_MS = 350L
private const val LOCK_MS = 700L
