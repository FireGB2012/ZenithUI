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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.gabriel.zenithui.data.AnimationTuning
import com.gabriel.zenithui.data.CloseRules
import com.gabriel.zenithui.data.LaunchMotion
import com.gabriel.zenithui.data.Tune
import com.gabriel.zenithui.ui.theme.GlassMaterial
import com.gabriel.zenithui.ui.theme.LocalGlassTuning
import com.gabriel.zenithui.ui.theme.drawLiquidGlass
import com.gabriel.zenithui.ui.theme.glassLook
import com.gabriel.zenithui.ui.theme.sharedLens
import com.gabriel.zenithui.ui.theme.LocalBackdrop
import com.gabriel.zenithui.ui.theme.LocalGlassLevels
import com.gabriel.zenithui.ui.theme.LocalGlassStrength
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalScreenSize
import com.gabriel.zenithui.ui.theme.LocalTilt
import com.gabriel.zenithui.ui.theme.LocalWallpaperDim
import com.gabriel.zenithui.ui.theme.LocalWallpaperOffset
import com.gabriel.zenithui.ui.theme.WALLPAPER_VEIL
import com.gabriel.zenithui.ui.theme.tuned
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
    /**
     * A picture of the app itself, from the app picture helper ([com.gabriel.zenithui.data.AppPictures]):
     * it takes over from the icon's colour and logo as the card grows, and gives way to them again
     * as it shrinks, so the app is seen coming out of its icon and going back into it.
     */
    val picture: ImageBitmap? = null,
)

/**
 * One of the springs an opening or a closing runs on, from the numbers in use (see
 * [AnimationTuning]). Springs, not timers, are what let the card be turned round mid-flight: each
 * carries on from wherever it is at whatever speed it has, so opening and closing an app over and
 * over stays one continuous, fluid motion — the way iOS and One UI do.
 */
fun AnimationTuning.springOf(pull: Tune, rest: Tune): AnimationSpec<Float> =
    spring(dampingRatio = this[rest], stiffness = stiffness(pull))

/** What the card is drawn with, apart from where it is: taken from the tuning in use. */
@Immutable
data class CardLook(
    /** How far the path bows upward at its half way point. */
    val arc: Dp = 0.dp,
    /** The rounding of the card's corners as it fills the screen. */
    val corner: Dp = 28.dp,
    /** The size of the app's logo in the middle of the full card. */
    val logo: Dp = 96.dp,
    /** How dark the shadow under the card is at its darkest, 0 for none. */
    val shadow: Float = 0f,
) {
    companion object {
        fun of(tuning: AnimationTuning) = CardLook(
            arc = tuning[Tune.OpenArc].dp,
            corner = tuning[Tune.OpenCorner].dp,
            logo = tuning[Tune.OpenLogo].dp,
            shadow = tuning[Tune.OpenShadow],
        )
    }
}

/**
 * Opening and closing an app the way iOS and One UI do. Android does not let a launcher that is not
 * built into the phone move another app's window, so Zenith draws the motion itself: a card in the
 * icon's own colour, carrying the icon, between the icon's place ([progress] 0) and the whole
 * screen ([progress] 1). Its middle travels across and down on springs of its own ([x] and [y]),
 * which is what lets it take a curve. The card only draws; whoever owns the springs decides where
 * it goes.
 */
@Composable
fun AppLaunchCard(
    card: AppExpansion,
    /** How far it has grown: 0 is the icon, 1 is the whole screen. Read while drawing, so every frame is smooth. */
    progress: () -> Float,
    /** How far its middle has travelled across and down, 0 to 1 the same way. */
    x: () -> Float = progress,
    y: () -> Float = progress,
    look: CardLook = CardLook(),
    /** A refracting wallpaper lens instead of a card in the icon's own colour. */
    glass: Boolean,
    /** While an app is opening the card catches taps, so tapping it sends the app back. */
    catchTaps: Boolean,
    onTap: () -> Unit,
    /** How solid the whole card is, read while drawing: a closing card comes in, and one with nowhere to go fades out. */
    fade: () -> Float = { 1f },
    /**
     * How squashed it is, read while drawing: a closing card landing in its icon is that much wider
     * and flatter for a moment, and springs back to its shape (see LaunchMotion.frame).
     */
    squash: () -> Float = { 0f },
    /**
     * Closing, as Void's does: the icon is drawn as wide as the card, cut to it, a ghost of itself
     * while the card fills the screen and solid by the time it is small (see CloseRules.ghostAlpha),
     * instead of the logo an app's own opening screen shows.
     */
    ghost: Boolean = false,
    /**
     * How far the home screen under it is zoomed about the middle of its foot, read while drawing:
     * coming back with a closing card, the card goes into its icon where the icon is drawn on the
     * way, and moves with it (see CloseRules.fromFoot). 1, at rest, for every other card.
     */
    homeZoom: () -> Float = { 1f },
) {
    val palette = LocalPalette.current
    val icon = card.icon
    val fill = remember(icon) { icon?.let(::backgroundColourOf) } ?: palette.backdrop.last()
    val tapped by rememberUpdatedState(onTap)
    val backdrop = LocalBackdrop.current
    val levels = LocalGlassLevels.current
    val screen = LocalScreenSize.current
    val wallpaperOffset = LocalWallpaperOffset.current
    val wallpaperDim = LocalWallpaperDim.current
    val glassStrength = LocalGlassStrength.current.coerceIn(0f, 1.4f)
    val tilt = LocalTilt.current
    val tuning = LocalGlassTuning.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    val clip = remember { Path() }
    // The lens every pane shares: an app opening does not have to compile one of its own first.
    val lens = sharedLens(if (glass) levels else null)
    val outline = remember { android.graphics.Path() }
    val shade = remember { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG) }
    val layer = remember { android.graphics.Paint() }

    Canvas(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInWindow() }
            .then(
                if (catchTaps) Modifier.pointerInput(card.key) { detectTapGestures { tapped() } } else Modifier
            ),
    ) {
        val p = progress().coerceIn(0f, 1.06f)
        // At the icon a card is nothing to draw, except a closing one, which is the icon by then:
        // squashed as it lands, and giving way to the icon under it.
        if (p <= 0.002f && !ghost) return@Canvas
        val solid = fade().coerceIn(0f, 1f)
        if (solid <= 0.003f) return@Canvas
        // Faded as one, so the colour under the picture never shows through it on the way.
        val faded = solid < 0.997f
        if (faded) {
            layer.alpha = (solid * 255f).roundToInt()
            drawContext.canvas.nativeCanvas.saveLayer(0f, 0f, size.width, size.height, layer)
        }
        try {
            drawFrame@ run {
                val from = CloseRules.fromFoot(card.from, homeZoom(), size.width, size.height)
                val wide = card.wideIcon
                // An icon's own rounding at the start, the phone's screen corners at the end. A super icon
                // starts from its own flatter rounding, so the card leaves it without a jump.
                val startRadius = from.minDimension * if (wide != null) 0.26f else 0.24f
                // Where it is: the same sums the preview in Settings and the tests use. The springs may
                // carry its middle a little past either end; no further than looks like a bounce.
                val frame = LaunchMotion.frame(
                    from.left, from.top, from.right, from.bottom,
                    size.width, size.height,
                    x().coerceIn(-0.2f, 1.2f), y().coerceIn(-0.2f, 1.2f), p,
                    look.arc.toPx(),
                    startRadius, look.corner.toPx(),
                    squash = squash(),
                )
                val left = frame.left
                val top = frame.top
                val right = frame.right
                val bottom = frame.bottom
                val radius = frame.radius
                val cardTopLeft = Offset(left, top)
                val cardSize = Size(right - left, bottom - top)
                val corners = CornerRadius(radius, radius)
                // The card floats over the home screen on its way: a soft shadow, coming in as it leaves
                // the icon and gone again by the time it fills the screen, where there is nothing left
                // to cast it on.
                val shadow = look.shadow * (p * 6f).coerceAtMost(1f) * (1f - p * p).coerceIn(0f, 1f)
                if (shadow > 0.01f) {
                    outline.rewind()
                    outline.addRoundRect(left, top, right, bottom, radius, radius, android.graphics.Path.Direction.CW)
                    shade.color = android.graphics.Color.BLACK
                    shade.setShadowLayer(
                        (14.dp.toPx() + 26.dp.toPx() * p),
                        0f,
                        (6.dp.toPx() + 10.dp.toPx() * p),
                        Color.Black.copy(alpha = 0.55f * shadow).toArgb(),
                    )
                    drawIntoCanvas {
                        val canvas = it.nativeCanvas
                        canvas.save()
                        // Only the shadow is wanted: the card itself is drawn next.
                        canvas.clipOutPath(outline)
                        canvas.drawPath(outline, shade)
                        canvas.restore()
                    }
                }
                if (glass) {
                    val amount = p.coerceIn(0f, 1f)
                    val roundRect = RoundRect(0f, 0f, cardSize.width, cardSize.height, corners)
                    clip.reset()
                    clip.addRoundRect(roundRect)
                    // The same glass as every pane on the home screen, gaining body as it grows so the
                    // app arriving on top of it reads cleanly. It always stands on the wallpaper, so it is
                    // the same clear glass in Light and in Dark.
                    val look = glassLook(
                        material = GlassMaterial.Regular,
                        strength = glassStrength,
                        solid = 0.15f + 0.35f * amount,
                        opacity = 1f,
                        dark = true,
                        tint = WALLPAPER_VEIL,
                        onWallpaper = true,
                    ).tuned(tuning)
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
                                dark = true,
                                tuning = tuning,
                                dim = wallpaperDim.value,
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

                val bitmap = icon
                if (bitmap != null && ghost) {
                    // Closing: the icon blown up to the card's width and cut to it, a ghost of itself while
                    // the card fills the screen, solid as it gets small, and the card is its icon again
                    // when it gets there. A flattened card shows the middle of it. Over a super icon's own
                    // picture it gives way to it near the end, as the logo does.
                    val wideAlpha = if (wide != null) ((p - 0.15f) / 0.35f).coerceIn(0f, 1f) else 1f
                    val ghostAlpha = CloseRules.ghostAlpha(p) * wideAlpha
                    if (ghostAlpha > 0.003f) {
                        val side = CloseRules.ghostSide(right - left, p)
                        val centreX = (left + right) / 2f
                        val centreY = (top + bottom) / 2f
                        clip.reset()
                        clip.addRoundRect(RoundRect(left, top, right, bottom, corners))
                        clipPath(clip) {
                            drawImage(
                                image = bitmap,
                                srcOffset = IntOffset.Zero,
                                srcSize = IntSize(bitmap.width, bitmap.height),
                                dstOffset = IntOffset((centreX - side / 2f).roundToInt(), (centreY - side / 2f).roundToInt()),
                                dstSize = IntSize(side.roundToInt().coerceAtLeast(1), side.roundToInt().coerceAtLeast(1)),
                                alpha = ghostAlpha,
                            )
                        }
                    }
                } else if (bitmap != null) {
                    // The logo grows more slowly than the card, easing towards the size an app's own opening
                    // screen shows it at, so the hand-over to the app is seamless.
                    val startSide = if (wide != null) from.minDimension * 0.62f else from.minDimension
                    val side = mix(startSide, look.logo.toPx(), p.coerceIn(0f, 1f))
                    val logoAlpha = if (wide != null) ((p - 0.15f) / 0.35f).coerceIn(0f, 1f) else 1f
                    if (logoAlpha > 0f) {
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

                // The app itself, over its colour and logo: none of it near the icon, all of it by the time
                // the card nearly fills the screen, filling the card the way it filled the screen.
                val picture = card.picture
                val share = if (picture != null) CloseRules.pictureShare(p) else 0f
                if (picture != null && share > 0.002f) {
                    val crop = CloseRules.coverCrop(picture.width, picture.height, cardSize.width, cardSize.height)
                    clip.reset()
                    clip.addRoundRect(RoundRect(left, top, right, bottom, corners))
                    clipPath(clip) {
                        drawImage(
                            image = picture,
                            srcOffset = IntOffset(crop.left, crop.top),
                            srcSize = IntSize(crop.width, crop.height),
                            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                            dstSize = IntSize(cardSize.width.roundToInt().coerceAtLeast(1), cardSize.height.roundToInt().coerceAtLeast(1)),
                            alpha = share,
                        )
                    }
                }
            }
        } finally {
            if (faded) drawContext.canvas.nativeCanvas.restore()
        }
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

    /** An icon kept out of sight because a closing card is on its way to it. */
    var held by mutableStateOf<String?>(null)
        private set

    private val progress = Animatable(1f)
    private var fromScale = 1.22f
    private var fade = 0.65f

    /** An icon swelling a little as an app's real window arrives in it (Android's gesture handshake). */
    private var swelling by mutableStateOf<String?>(null)
    private val swell = Animatable(0f)

    /** Whether an icon is landing or swelling now: the home screen is not at rest while one is. */
    val busy: Boolean get() = key != null || swelling != null || held != null

    /** Hides [key]'s icon until it lands, or shows whichever was hidden again with null. */
    fun hold(key: String?) {
        held = key
    }

    /**
     * Lands [key]'s icon: from [fromScale] times its size and [fade] of the way to invisible, on
     * [spec]. Must run on a scope with a frame clock, such as a composition's.
     */
    suspend fun land(
        key: String,
        fromScale: Float = 1.22f,
        fade: Float = 0.65f,
        spec: AnimationSpec<Float> = spring(dampingRatio = 0.5f, stiffness = 380f),
    ) {
        this.fromScale = fromScale
        this.fade = fade.coerceIn(0f, 1f)
        this.key = key
        if (held == key) held = null
        progress.snapTo(0f)
        progress.animateTo(1f, spec)
        if (this.key == key) this.key = null
    }

    /**
     * Gives [key]'s icon a small swell, as far as [by] of its size, and lets it settle on a spring
     * of [stiffness] and [damping]: an app's own window has just arrived in it. It is sent off at
     * speed from its own size, never made bigger and let go, so there is no jump.
     */
    suspend fun swell(key: String, by: Float, stiffness: Float, damping: Float) {
        if (by <= 0f) return
        swelling = key
        try {
            swell.snapTo(0f)
            swell.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = damping, stiffness = stiffness),
                initialVelocity = LaunchMotion.knockVelocity(by, stiffness, damping),
            )
        } finally {
            if (swelling == key) swelling = null
        }
    }

    /** How large [itemKey]'s icon should be drawn right now. Read while drawing. */
    fun scaleFor(itemKey: String): Float {
        val landing = if (itemKey != key) 1f else fromScale - (fromScale - 1f) * progress.value
        return if (itemKey == swelling) landing * (1f + swell.value) else landing
    }

    /**
     * How solid: back to full a little before it has finished settling. An item that shows its
     * name under its icon ([iconOnly]) keeps it while the icon is held: only the icon waits for
     * the card ([iconAlphaFor]), as Void's does.
     */
    fun alphaFor(itemKey: String, iconOnly: Boolean = false): Float = when {
        itemKey == held -> if (iconOnly) 1f else 0f
        itemKey != key -> 1f
        else -> ((1f - fade) + fade * 1.6f * progress.value).coerceIn(0f, 1f)
    }

    /** How solid the icon itself is, apart from its name: out of sight while a closing card is on its way to it. */
    fun iconAlphaFor(itemKey: String): Float = if (itemKey == held) 0f else 1f
}

/**
 * Icons making way. While an app opens, the ones nearest its icon are pushed aside as the card
 * grows out of it; when it closes they are knocked back as its icon lands, and swing into place
 * again. Kept outside composition, like [AppLanding], because every item on the page reads it
 * while drawing, and reading it there is what moves them without recomposing anything.
 */
object HomeRipple {
    /** Where it spreads from, in the home screen's own coordinates. */
    var origin by mutableStateOf(Offset.Unspecified)

    /** How far away the push has fallen to a little over half, in pixels. */
    var reach by mutableFloatStateOf(1f)

    /** How far the nearest are pushed while a card grows, in pixels. Read while drawing. */
    var push: (() -> Float)? by mutableStateOf(null)

    private val wave = Animatable(0f)

    /** Whether icons are swinging back into place now: the home screen is not at rest while they are. */
    val swinging: Boolean get() = wave.isRunning

    /**
     * Knocks the icons round [from] back as far as [px] and lets them swing home, on a spring of
     * [stiffness] and [damping]. They are sent off at speed from where they stand rather than
     * moved and let go, so there is no jump at the start of it.
     */
    suspend fun knock(from: Offset, px: Float, stiffness: Float, damping: Float) {
        origin = from
        wave.snapTo(0f)
        wave.animateTo(
            targetValue = 0f,
            animationSpec = spring(dampingRatio = damping, stiffness = stiffness),
            initialVelocity = LaunchMotion.knockVelocity(px, stiffness, damping),
        )
    }

    /**
     * How far the item whose middle is at [centre] is out of its place right now. Read while
     * drawing. Where its middle is, is only asked for while something is pushing.
     */
    fun offsetFor(centre: () -> Offset): Offset {
        val from = origin
        if (!from.isSpecified) return Offset.Zero
        val amount = (push?.invoke() ?: 0f) + wave.value
        if (amount == 0f) return Offset.Zero
        val at = centre()
        val (dx, dy) = LaunchMotion.push(at.x - from.x, at.y - from.y, amount, reach)
        return Offset(dx, dy)
    }
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
