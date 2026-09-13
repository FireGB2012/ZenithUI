package com.gabriel.zenithui.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.gabriel.zenithui.ui.theme.LocalPalette
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * An app on its way open: which one, where its icon was when it was tapped, and the icon itself —
 * loaded before the card appears, so its very first frame already has the right colour and logo.
 */
@Immutable
data class AppExpansion(val key: String, val from: Rect, val icon: ImageBitmap?)

/** How long the icon takes to grow until it fills the screen. */
private const val GROW_MS = 320

/**
 * How long after the app is asked to open the card is taken away. By then the app's own opening
 * screen covers it, and Zenith is still drawing, so the home screen underneath is ready before you
 * come back to it. It also clears the card if the app never opened at all.
 */
private const val GIVE_UP_MS = 700L

/**
 * Opening an app the way iOS and One UI do. Android does not let a launcher that is not built
 * into the phone animate another app's window, so Zenith draws the growth itself: a card in the
 * icon's own colour, carrying the icon, grows from where the icon was until it fills the screen.
 * [onFilled] then opens the app, which fades in over the card.
 */
@Composable
fun AppExpansionOverlay(
    expansion: AppExpansion,
    /** Grey liquid glass instead of a card in the icon's own colour. */
    glass: Boolean = false,
    onFilled: () -> Unit,
    onDone: () -> Unit,
) {
    val palette = LocalPalette.current
    val progress = remember(expansion) { Animatable(0f) }
    val icon = expansion.icon
    val fill = remember(icon) { icon?.let(::backgroundColourOf) } ?: palette.backdrop.last()
    val filled by rememberUpdatedState(onFilled)
    val done by rememberUpdatedState(onDone)

    LaunchedEffect(expansion) {
        progress.animateTo(1f, tween(GROW_MS, easing = FastOutSlowInEasing))
        filled()
        delay(GIVE_UP_MS)
        done()
    }

    Canvas(
        Modifier
            .fillMaxSize()
            // Nothing behind the card can be tapped while an app is opening.
            .pointerInput(expansion) { detectTapGestures { } },
    ) {
        val p = progress.value
        val from = expansion.from
        val left = mix(from.left, 0f, p)
        val top = mix(from.top, 0f, p)
        val right = mix(from.right, size.width, p)
        val bottom = mix(from.bottom, size.height, p)
        // An icon's own rounding at the start, the phone's screen corners at the end.
        val radius = mix(from.minDimension * 0.24f, 28.dp.toPx(), p)
        val cardTopLeft = Offset(left, top)
        val cardSize = Size(right - left, bottom - top)
        val corners = CornerRadius(radius, radius)
        if (glass) {
            // Grey liquid glass: a see-through tint over the home screen, which blurs behind it, a
            // soft sheen falling from the top and a thin bright rim.
            drawRoundRect(
                color = GLASS_TINT.copy(alpha = 0.42f + 0.22f * p),
                topLeft = cardTopLeft,
                size = cardSize,
                cornerRadius = corners,
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.26f), Color.White.copy(alpha = 0.03f)),
                    startY = top,
                    endY = bottom,
                ),
                topLeft = cardTopLeft,
                size = cardSize,
                cornerRadius = corners,
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.40f),
                topLeft = cardTopLeft,
                size = cardSize,
                cornerRadius = corners,
                style = Stroke(width = 1.dp.toPx()),
            )
        } else {
            drawRoundRect(color = fill, topLeft = cardTopLeft, size = cardSize, cornerRadius = corners)
        }

        val bitmap = icon ?: return@Canvas
        val side = mix(from.minDimension, 96.dp.toPx(), p)
        val centreX = (left + right) / 2f
        val centreY = (top + bottom) / 2f
        drawImage(
            image = bitmap,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(bitmap.width, bitmap.height),
            dstOffset = IntOffset((centreX - side / 2f).roundToInt(), (centreY - side / 2f).roundToInt()),
            dstSize = IntSize(side.roundToInt(), side.roundToInt()),
        )
    }
}

private fun mix(from: Float, to: Float, amount: Float) = from + (to - from) * amount

/** The grey of the liquid glass opening. */
private val GLASS_TINT = Color(0xFF7C818C)

/**
 * The colour an icon is mostly made of, which for almost every app is its background. Pixels are
 * sorted into coarse colour buckets and the fullest bucket wins; an average would mix a colourful
 * logo into mud.
 */
private fun backgroundColourOf(image: ImageBitmap): Color? = runCatching {
    val bitmap = image.asAndroidBitmap()
    val counts = HashMap<Int, IntArray>()
    val step = (minOf(bitmap.width, bitmap.height) / 32).coerceAtLeast(1)
    for (y in 0 until bitmap.height step step) {
        for (x in 0 until bitmap.width step step) {
            val pixel = bitmap.getPixel(x, y)
            if (android.graphics.Color.alpha(pixel) < 220) continue
            val red = android.graphics.Color.red(pixel)
            val green = android.graphics.Color.green(pixel)
            val blue = android.graphics.Color.blue(pixel)
            val bucket = (red shr 4 shl 8) or (green shr 4 shl 4) or (blue shr 4)
            val sum = counts.getOrPut(bucket) { IntArray(4) }
            sum[0] += red
            sum[1] += green
            sum[2] += blue
            sum[3] += 1
        }
    }
    val top = counts.values.maxByOrNull { it[3] } ?: return@runCatching null
    Color(top[0] / top[3], top[1] / top[3], top[2] / top[3])
}.getOrNull()
