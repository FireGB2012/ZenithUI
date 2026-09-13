package com.gabriel.zenithui.ui.home

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.gabriel.zenithui.ui.theme.LocalPalette
import kotlin.math.roundToInt

/**
 * An app on its way open or closed: which one, where its icon sits, and the icon itself — loaded
 * before the card appears, so its very first frame already has the right colour and logo.
 */
@Immutable
data class AppExpansion(val key: String, val from: Rect, val icon: ImageBitmap?)

/**
 * The spring every opening and closing runs on. A spring, not a timer, is what lets the card be
 * turned round mid-flight: it carries on from wherever it is at whatever speed it has, so opening
 * and closing an app over and over stays one continuous, fluid motion — the way iOS and HyperOS do.
 */
fun launchSpring(): AnimationSpec<Float> = spring(dampingRatio = 0.86f, stiffness = 280f)

/** How far the card has to grow before the app itself is asked to open, so it is ready as the card fills. */
const val LAUNCH_AT = 0.55f

/**
 * Opening and closing an app the way iOS and One UI do. Android does not let a launcher that is not
 * built into the phone move another app's window, so Zenith draws the motion itself: a card in the
 * icon's own colour, carrying the icon, between the icon's place ([progress] 0) and the whole
 * screen ([progress] 1). The card only draws; whoever owns [progress] decides where it goes.
 */
@Composable
fun AppLaunchCard(
    card: AppExpansion,
    /** 0 is the icon, 1 is the whole screen. Read while drawing, so every frame is smooth. */
    progress: () -> Float,
    /** Grey liquid glass instead of a card in the icon's own colour. */
    glass: Boolean,
    /** While an app is opening the card catches taps, so tapping it sends the app back. */
    catchTaps: Boolean,
    onTap: () -> Unit,
) {
    val palette = LocalPalette.current
    val icon = card.icon
    val fill = remember(icon) { icon?.let(::backgroundColourOf) } ?: palette.backdrop.last()
    val tapped by rememberUpdatedState(onTap)

    Canvas(
        Modifier
            .fillMaxSize()
            .then(
                if (catchTaps) Modifier.pointerInput(card.key) { detectTapGestures { tapped() } } else Modifier
            ),
    ) {
        val p = progress().coerceIn(0f, 1.04f)
        if (p <= 0.002f) return@Canvas
        val from = card.from
        val left = mix(from.left, 0f, p)
        val top = mix(from.top, 0f, p)
        val right = mix(from.right, size.width, p)
        val bottom = mix(from.bottom, size.height, p)
        // An icon's own rounding at the start, the phone's screen corners at the end.
        val radius = mix(from.minDimension * 0.24f, 28.dp.toPx(), p.coerceIn(0f, 1f))
        val cardTopLeft = Offset(left, top)
        val cardSize = Size(right - left, bottom - top)
        val corners = CornerRadius(radius, radius)
        if (glass) {
            // Grey liquid glass: a see-through tint over the home screen, which blurs behind it, a
            // soft sheen falling from the top and a thin bright rim.
            drawRoundRect(
                color = GLASS_TINT.copy(alpha = 0.42f + 0.22f * p.coerceIn(0f, 1f)),
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
        // The logo grows more slowly than the card, easing towards the size an app's own opening
        // screen shows it at, so the hand-over to the app is seamless.
        val side = mix(from.minDimension, 96.dp.toPx(), p.coerceIn(0f, 1f))
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
