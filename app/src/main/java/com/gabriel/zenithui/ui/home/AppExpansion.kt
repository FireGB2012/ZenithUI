package com.gabriel.zenithui.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.gabriel.zenithui.ui.theme.GlassMaterial
import com.gabriel.zenithui.ui.theme.drawLiquidGlass
import com.gabriel.zenithui.ui.theme.glassLook
import com.gabriel.zenithui.ui.theme.rememberLens
import com.gabriel.zenithui.ui.theme.LocalBackdrop
import com.gabriel.zenithui.ui.theme.LocalGlassBackdrop
import com.gabriel.zenithui.ui.theme.LocalGlassStrength
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalScreenSize
import com.gabriel.zenithui.ui.theme.LocalTilt
import com.gabriel.zenithui.ui.theme.LocalWallpaperOffset
import kotlin.math.roundToInt

/**
 * An app on its way open or closed: which one, where its icon sits, and the icon itself — loaded
 * before the card appears, so its very first frame already has the right colour and logo.
 */
@Immutable
data class AppExpansion(
    val key: String,
    val from: Rect,
    val icon: ImageBitmap?,
    /** A super icon's own stretched picture, so the card leaves it looking exactly like it. */
    val wideIcon: ImageBitmap? = null,
)

/**
 * The spring every opening and closing runs on. A spring, not a timer, is what lets the card be
 * turned round mid-flight: it carries on from wherever it is at whatever speed it has, so opening
 * and closing an app over and over stays one continuous, fluid motion — the way iOS and HyperOS do.
 */
fun launchSpring(): AnimationSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 300f)

/**
 * The home screen coming forward again after an app closes. A touch under-damped, so it swells a
 * hair past full size and settles, the way One UI's grid expands back when you go home.
 */
fun homeReturnSpring(): AnimationSpec<Float> = spring(dampingRatio = 0.72f, stiffness = 320f)

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
    /** A refracting wallpaper lens instead of a card in the icon's own colour. */
    glass: Boolean,
    /** While an app is opening the card catches taps, so tapping it sends the app back. */
    catchTaps: Boolean,
    onTap: () -> Unit,
) {
    val palette = LocalPalette.current
    val icon = card.icon
    val fill = remember(icon) { icon?.let(::backgroundColourOf) } ?: palette.backdrop.last()
    val tapped by rememberUpdatedState(onTap)
    val backdrop = LocalBackdrop.current
    val sharp = LocalGlassBackdrop.current ?: backdrop
    val screen = LocalScreenSize.current
    val wallpaperOffset = LocalWallpaperOffset.current
    val glassStrength = LocalGlassStrength.current.coerceIn(0f, 1.4f)
    val tilt = LocalTilt.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    val clip = remember { Path() }
    val lens = rememberLens(if (glass) sharp else null, backdrop)

    Canvas(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInWindow() }
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
        val wide = card.wideIcon
        // An icon's own rounding at the start, the phone's screen corners at the end. A super icon
        // starts from its own flatter rounding, so the card leaves it without a jump.
        val startRadius = from.minDimension * if (wide != null) 0.26f else 0.24f
        val radius = mix(startRadius, 28.dp.toPx(), p.coerceIn(0f, 1f))
        val cardTopLeft = Offset(left, top)
        val cardSize = Size(right - left, bottom - top)
        val corners = CornerRadius(radius, radius)
        if (glass) {
            val amount = p.coerceIn(0f, 1f)
            val roundRect = RoundRect(0f, 0f, cardSize.width, cardSize.height, corners)
            clip.reset()
            clip.addRoundRect(roundRect)
            // The same glass as every pane on the home screen, gaining body as it grows so the
            // app arriving on top of it reads cleanly.
            val look = glassLook(
                material = GlassMaterial.Regular,
                strength = glassStrength,
                solid = 0.15f + 0.35f * amount,
                opacity = 1f,
                dark = palette.dark,
                tint = palette.glassTint,
            )
            translate(left, top) {
                clipPath(clip) {
                    drawLiquidGlass(
                        paneSize = cardSize,
                        outline = Outline.Rounded(roundRect),
                        lens = lens,
                        softened = backdrop,
                        origin = origin + cardTopLeft,
                        screen = screen,
                        wallpaperOffset = wallpaperOffset.value,
                        look = look,
                        lean = tilt.value,
                        dark = palette.dark,
                    )
                }
            }
        } else {
            drawRoundRect(color = fill, topLeft = cardTopLeft, size = cardSize, cornerRadius = corners)
        }

        if (wide != null) {
            // A super icon begins as itself: its own stretched picture fills the card at first and
            // fades as the card grows, handing over to the logo the app opens on.
            val wideAlpha = (1f - p / 0.45f).coerceIn(0f, 1f)
            if (wideAlpha > 0f) {
                drawImage(
                    image = wide,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(wide.width, wide.height),
                    dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                    dstSize = IntSize((right - left).roundToInt(), (bottom - top).roundToInt()),
                    alpha = wideAlpha,
                )
            }
        }

        val bitmap = icon ?: return@Canvas
        // The logo grows more slowly than the card, easing towards the size an app's own opening
        // screen shows it at, so the hand-over to the app is seamless.
        val startSide = if (wide != null) from.minDimension * 0.62f else from.minDimension
        val side = mix(startSide, 96.dp.toPx(), p.coerceIn(0f, 1f))
        val logoAlpha = if (wide != null) ((p - 0.15f) / 0.35f).coerceIn(0f, 1f) else 1f
        if (logoAlpha <= 0f) return@Canvas
        val centreX = (left + right) / 2f
        val centreY = (top + bottom) / 2f
        drawImage(
            image = bitmap,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(bitmap.width, bitmap.height),
            dstOffset = IntOffset((centreX - side / 2f).roundToInt(), (centreY - side / 2f).roundToInt()),
            dstSize = IntSize(side.roundToInt(), side.roundToInt()),
            alpha = logoAlpha,
        )
    }
}

private fun mix(from: Float, to: Float, amount: Float) = from + (to - from) * amount

/**
 * The app that has just been closed, so its icon can land back in its place the way One UI's does:
 * it arrives a little large and faint and settles with a small bounce while the home screen comes
 * forward around it. Android only lets the phone's own launcher shrink an app's real window into
 * its icon, so this is the half of that motion a launcher like Zenith is allowed to draw.
 *
 * Kept outside composition, like [com.gabriel.zenithui.ui.common.LaunchOrigin], because the grid,
 * super icons and the dock all read it while drawing.
 */
object AppLanding {
    var key by mutableStateOf<String?>(null)
        private set

    private val progress = Animatable(1f)

    /** Lands [key]'s icon. Must run on a scope with a frame clock, such as a composition's. */
    suspend fun land(key: String) {
        this.key = key
        progress.snapTo(0f)
        progress.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 380f))
        if (this.key == key) this.key = null
    }

    /** How large [itemKey]'s icon should be drawn right now. Read while drawing. */
    fun scaleFor(itemKey: String): Float = if (itemKey != key) 1f else 1.22f - 0.22f * progress.value

    fun alphaFor(itemKey: String): Float =
        if (itemKey != key) 1f else (0.35f + 1.04f * progress.value).coerceIn(0f, 1f)
}

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
