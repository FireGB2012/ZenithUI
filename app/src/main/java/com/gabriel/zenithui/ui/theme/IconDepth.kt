package com.gabriel.zenithui.ui.theme

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.hypot

/** One shadow an icon casts on the wallpaper: how soft it is and how far down it falls, in dp, and how dark. */
@Immutable
data class IconShadow(val blurDp: Float, val dropDp: Float, val alpha: Float)

/**
 * The depth an icon is drawn with ([iconDepth]): the light on its rim, the light and shade on its
 * face, and the shadow it casts.
 */
@Immutable
data class IconDepthLook(
    /** The hairline of light hugging the rim: how wide, in dp, and how bright on the corner facing the light. */
    val rimDp: Float,
    val rim: Float,
    /** The softer light just inside it, where the edge of a thick tile catches the light: how wide, in dp, and how bright. */
    val sheenDp: Float,
    val sheen: Float,
    /** Light on the top of the face and shade at its foot, as white and black over the icon. */
    val topLight: Float,
    val footShade: Float,
    /** What it casts on the wallpaper, the wide soft one first; none for an icon that casts nothing. */
    val shadows: List<IconShadow>,
)

/**
 * The depth an iPhone's icons have, given to the icons Zenith shows as their apps draw them: a tile
 * with a little thickness to it, lit from up and to the left as Zenith's glass is. A hairline of
 * light hugs the rim, brightest on the corner facing the light, bright again on the corner opposite
 * and faint between, with a softer light just inside it, and it moves as the phone tips; the top of
 * the face catches a little light and its foot a whisper of shade; and on the wallpaper it stands a
 * little off the picture, on a soft shadow falling straight down.
 *
 * It is all drawn over the icon, inside its outline: the app's own picture is not changed. Rules
 * only, so they can be checked without a screen.
 */
object IconDepth {
    /** Smaller than this, in dp, an icon is drawn flat: a folder's tiny previews. */
    const val SMALLEST_DP = 18f

    /** From this size up, in dp, an icon standing on the wallpaper casts a shadow on it. */
    const val SHADOW_FROM_DP = 36f

    /**
     * How an icon [sideDp] across is lit: standing [onWallpaper] (a home page, the dock) or on a
     * surface of its own, in [dark] or not; null for one too small to show it. On the wallpaper it
     * is lit the same in Light and in Dark, as the glass there is, since the picture is the same in
     * both. [shadow] off for an icon inside glass, such as a folder's, which casts none.
     */
    fun look(sideDp: Float, onWallpaper: Boolean, dark: Boolean, shadow: Boolean = true): IconDepthLook? {
        if (sideDp < SMALLEST_DP) return null
        val lit = onWallpaper || dark
        // A small icon, a menu's or a list's, catches a little less of it.
        val reach = 0.75f + 0.25f * ((sideDp - SMALLEST_DP) / (FULL_FROM_DP - SMALLEST_DP)).coerceIn(0f, 1f)
        // In Light, off the wallpaper, the page is pale already, and the same light would whiten the edge.
        val light = if (lit) 1f else LIGHT_SHARE
        return IconDepthLook(
            rimDp = (sideDp * RIM_SHARE).coerceIn(0.6f, 1.4f),
            rim = RIM * light * reach,
            sheenDp = (sideDp * SHEEN_SHARE).coerceIn(1.6f, 7f),
            sheen = SHEEN * light * reach,
            topLight = TOP_LIGHT,
            footShade = FOOT_SHADE,
            shadows = if (shadow && onWallpaper && sideDp >= SHADOW_FROM_DP) {
                listOf(
                    // A bigger icon stands higher off the picture: a wider shadow, falling further.
                    IconShadow(blurDp = (sideDp * 0.09f).coerceIn(3.5f, 10f), dropDp = (sideDp * 0.04f).coerceIn(1.5f, 4f), alpha = WIDE_SHADOW),
                    // Close under its edge, only enough to draw the outline on a pale picture.
                    IconShadow(blurDp = 1.2f, dropDp = 0.6f, alpha = CLOSE_SHADOW),
                )
            } else {
                emptyList()
            },
        )
    }

    /**
     * Where the light on the rim is brightest and where it is brightest again, for light falling
     * from [light] on an icon of [size]: a line from the lit corner to the far one, stretched to the
     * icon's own shape, so a super icon twice as wide as it is tall is lit on its corner as a square
     * one is, as the glass's rim is lit.
     */
    fun rimEnds(light: Offset, size: Size): Pair<Offset, Offset> {
        val centre = Offset(size.width / 2f, size.height / 2f)
        val length = hypot(light.x, light.y).coerceAtLeast(0.001f)
        val towards = Offset(light.x / length, light.y / length)
        val across = Offset(towards.x / (size.width / 2f).coerceAtLeast(0.5f), towards.y / (size.height / 2f).coerceAtLeast(0.5f))
        val span = (abs(towards.x) + abs(towards.y)) / (across.x * across.x + across.y * across.y).coerceAtLeast(1e-9f)
        return (centre + across * span) to (centre - across * span)
    }

    /** How bright the rim is [t] of the way from the lit corner to the far one, as a share of its brightest. */
    fun rimAt(t: Float): Float {
        val at = t.coerceIn(0f, 1f)
        for (i in 1 until RIM_PROFILE.size) {
            val (to, toShare) = RIM_PROFILE[i]
            if (at <= to) {
                val (from, fromShare) = RIM_PROFILE[i - 1]
                return fromShare + (toShare - fromShare) * ((at - from) / (to - from))
            }
        }
        return RIM_PROFILE.last().second
    }

    /** The light along the rim, [brightest] on the corner at [lamp] and falling away towards [far]. */
    internal fun rimBrush(brightest: Float, lamp: Offset, far: Offset): Brush = Brush.linearGradient(
        *Array(RIM_PROFILE.size) { i ->
            val (at, share) = RIM_PROFILE[i]
            at to Color.White.copy(alpha = (brightest * share).coerceIn(0f, 1f))
        },
        start = lamp,
        end = far,
    )

    /** From this size up, in dp, an icon catches all of the light. */
    private const val FULL_FROM_DP = 40f

    /** How wide the rim's hairline is, and the light inside it, as a share of the icon's side. */
    private const val RIM_SHARE = 0.022f
    private const val SHEEN_SHARE = 0.085f

    /** How bright the rim and the light inside it are on the corner facing the light, as white. */
    private const val RIM = 0.62f
    private const val SHEEN = 0.12f

    /** How much of the light an icon off the wallpaper catches in Light. */
    private const val LIGHT_SHARE = 0.72f

    /** The light on the top of the face and the shade at its foot, as white and black. */
    private const val TOP_LIGHT = 0.12f
    private const val FOOT_SHADE = 0.07f

    /** How dark the wide shadow and the close one are at their darkest. */
    private const val WIDE_SHADOW = 0.18f
    private const val CLOSE_SHADOW = 0.1f

    /**
     * The rim's light from the lit corner (0) to the far one (1), as a share of its brightest: as
     * the glass's is, brightest facing the light, bright again opposite, faint along the sides.
     */
    private val RIM_PROFILE = listOf(0f to 1f, 0.32f to 0.14f, 0.68f to 0.14f, 1f to 0.62f)
}

/**
 * Lights an icon drawn in [shape], [side] across, as an iPhone's icons are lit ([IconDepth]): the
 * rim's hairline of light and the light just inside it, which move as the phone tips, the light on
 * the top of its face and the shade at its foot, and, standing on the wallpaper, a soft shadow
 * under it. [shadow] off for an icon inside glass. Goes on the icon's picture itself, inside
 * anything that fades or scales it, so all of it fades and scales with the picture.
 *
 * The tilt is read while drawing, so the light follows the phone without composing anything; the
 * shadow is the same for every icon of one outline and size, so it is drawn once and shared.
 */
@Composable
fun Modifier.iconDepth(shape: Shape, side: Dp, shadow: Boolean = true): Modifier {
    val onWallpaper = LocalOnWallpaper.current
    val dark = LocalPalette.current.dark
    val look = remember(side, onWallpaper, dark, shadow) { IconDepth.look(side.value, onWallpaper, dark, shadow) } ?: return this
    val tilt = LocalTilt.current
    val tuning = LocalGlassTuning.current
    return drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val face = Path().apply { addOutline(outline) }
        val rimWidth = look.rimDp.dp.toPx()
        // The light inside the rim, in three bands each starting at the edge, so it fades inward.
        val sheenWidths = floatArrayOf(1f, 0.66f, 0.33f).map { it * look.sheenDp.dp.toPx() }
        // Each stroke is drawn just inside the outline, so its light lies on the icon's own edge.
        val rim = face.insetBy(rimWidth / 2f, size)
        val sheens = sheenWidths.map { width -> face.insetBy(width / 2f, size) to width }
        val faceLight = Brush.verticalGradient(
            0f to Color.White.copy(alpha = look.topLight),
            0.5f to Color.Transparent,
            0.62f to Color.Transparent,
            1f to Color.Black.copy(alpha = look.footShade),
        )
        val cast = if (look.shadows.isEmpty()) null else CastShadows.of(shape, face, size, density, look.shadows)
        onDrawWithContent {
            if (cast != null) drawImage(cast.image, topLeft = Offset(-cast.margin, -cast.margin))
            drawContent()
            drawPath(face, faceLight)
            // Where the light is, read here so it moves with the phone and composes nothing.
            val (lamp, far) = IconDepth.rimEnds(lightFrom(tilt.value, tuning), size)
            val sheen = IconDepth.rimBrush(look.sheen / sheens.size, lamp, far)
            for ((path, width) in sheens) drawPath(path, sheen, style = Stroke(width))
            drawPath(rim, IconDepth.rimBrush(look.rim, lamp, far), style = Stroke(rimWidth))
        }
    }
}

/** This outline drawn [by] pixels further in all round, in an icon of [size]. */
private fun Path.insetBy(by: Float, size: Size): Path {
    if (size.width <= 2f * by || size.height <= 2f * by) return this
    val inner = android.graphics.Path(asAndroidPath())
    inner.transform(
        android.graphics.Matrix().apply {
            setScale((size.width - 2f * by) / size.width, (size.height - 2f * by) / size.height, size.width / 2f, size.height / 2f)
        },
    )
    return inner.asComposePath()
}

/** A shadow drawn once, to go under every icon of one outline and size: [margin] pixels round the icon. */
private class CastShadow(val image: ImageBitmap, val margin: Float)

/**
 * The shadows icons cast, one for each outline and size in use, which is a handful: every app's
 * icon on a page is the same shape and size, so they all share one picture of a shadow rather than
 * each blurring its own on every frame.
 */
private object CastShadows {
    private data class Kind(val shape: Shape, val width: Int, val height: Int, val density: Float, val shadows: List<IconShadow>)

    private val made = object : LinkedHashMap<Kind, CastShadow>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Kind, CastShadow>?): Boolean = size > KEPT
    }

    fun of(shape: Shape, face: Path, size: Size, density: Float, shadows: List<IconShadow>): CastShadow {
        val kind = Kind(shape, ceil(size.width).toInt(), ceil(size.height).toInt(), density, shadows)
        synchronized(made) { made[kind]?.let { return it } }
        val drawn = draw(face, kind)
        synchronized(made) { made[kind] = drawn }
        return drawn
    }

    private fun draw(face: Path, kind: Kind): CastShadow {
        // Room for the softest of them to fade out to nothing, and for how far it falls.
        val margin = ceil(kind.shadows.maxOf { it.blurDp * 2f + it.dropDp } * kind.density + 2f).toInt()
        val bitmap = Bitmap.createBitmap(kind.width + 2 * margin, kind.height + 2 * margin, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val outline = face.asAndroidPath()
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        for (shadow in kind.shadows) {
            paint.color = android.graphics.Color.argb((shadow.alpha * 255f).toInt().coerceIn(0, 255), 0, 0, 0)
            paint.maskFilter = BlurMaskFilter((shadow.blurDp * kind.density).coerceAtLeast(0.5f), BlurMaskFilter.Blur.NORMAL)
            canvas.save()
            canvas.translate(margin.toFloat(), margin + shadow.dropDp * kind.density)
            canvas.drawPath(outline, paint)
            canvas.restore()
        }
        // Nothing under the icon itself: one fading in shows the wallpaper through it, not its own shadow.
        paint.maskFilter = null
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        canvas.save()
        canvas.translate(margin.toFloat(), margin.toFloat())
        canvas.drawPath(outline, paint)
        canvas.restore()
        bitmap.prepareToDraw()
        return CastShadow(bitmap.asImageBitmap(), margin.toFloat())
    }

    /** As many as there could be outlines and sizes on screen at once, and a few over. */
    private const val KEPT = 12
}
