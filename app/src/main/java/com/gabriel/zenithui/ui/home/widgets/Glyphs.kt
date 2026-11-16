package com.gabriel.zenithui.ui.home.widgets

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.gabriel.zenithui.data.WeatherCodes
import com.gabriel.zenithui.ui.common.symbolShadowColour
import com.gabriel.zenithui.ui.home.LocalBare
import com.gabriel.zenithui.ui.home.LocalWidgetFill
import com.gabriel.zenithui.ui.home.contrast
import com.gabriel.zenithui.ui.home.wordShadow
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.glassInk
import kotlin.math.cos
import kotlin.math.sin

/*
 * The widgets' own symbols, drawn rather than taken from a font, so each can wear more than one
 * colour as iOS's weather symbols do: a yellow sun behind a white cloud, cyan rain under it. Each is
 * drawn in a square of [size], the shapes worked out as fractions of it, and cut from one another
 * where one stands in front, so there is a sliver of nothing between them rather than one colour
 * painted over the other. On the wallpaper's glass they stand on the same soft shadow as the words;
 * in a "No glass" widget they are glass, all in one piece.
 */

/** What a weather symbol shows: the sky's [WeatherCodes.Kind], by day or by night. */
@Composable
internal fun WeatherGlyph(code: Int, day: Boolean, size: Dp, modifier: Modifier = Modifier) {
    val kind = WeatherCodes.kind(code)
    val inks = glyphInks()
    val shadow = glyphShadow()
    val bare = LocalBare.current
    Spacer(
        modifier
            .size(size)
            .then(if (bare) Modifier.glassInk(bevel = size * 0.06f) else Modifier)
            .drawWithCache {
                val parts = weatherParts(kind, day, this.size.minDimension)
                onDrawBehind { drawParts(parts, inks, shadow) }
            }
    )
}

/** The streak's flame: orange, with a yellow heart. */
@Composable
internal fun FlameGlyph(size: Dp, modifier: Modifier = Modifier) {
    val inks = glyphInks()
    val shadow = glyphShadow()
    val bare = LocalBare.current
    Spacer(
        modifier
            .size(size)
            .then(if (bare) Modifier.glassInk(bevel = size * 0.07f) else Modifier)
            .drawWithCache {
                val parts = flameParts(this.size.minDimension)
                onDrawBehind { drawParts(parts, inks, shadow) }
            }
    )
}

/**
 * A symbol from Material's rounded set, in [tint] or the words' ink, standing on the words' shadow on
 * the wallpaper's glass, and glass itself in a "No glass" widget.
 */
@Composable
internal fun Glyph(icon: ImageVector, size: Dp, modifier: Modifier = Modifier, tint: Color = Color.Unspecified, says: String? = null) {
    val bare = LocalBare.current
    val ink = when {
        bare -> Color.White
        tint.isSpecified -> tint
        else -> ink()
    }
    val shadow = if (bare) null else wordShadow()
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (shadow != null) {
            val fall = with(LocalDensity.current) { shadow.offset.y.toDp() }
            Icon(icon, contentDescription = null, tint = symbolShadowColour(shadow), modifier = Modifier.size(size).offset(y = fall))
        }
        Icon(
            icon,
            contentDescription = says,
            tint = ink,
            modifier = Modifier
                .size(size)
                .then(if (bare) Modifier.glassInk(bevel = size * 0.09f) else Modifier),
        )
    }
}

// ---------------------------------------------------------------------- colours

/** The colours a symbol's parts are painted in. */
internal data class GlyphInks(
    val sun: Color,
    val moon: Color,
    val cloud: Color,
    val farCloud: Color,
    val rain: Color,
    val snow: Color,
    val bolt: Color,
    val mist: Color,
    val flame: Color,
    val flameTip: Color,
    val flameHeart: Color,
)

/** What each part of a symbol is painted in: as iOS paints them, or, in a "No glass" widget, all white for the glass. */
@Composable
internal fun glyphInks(): GlyphInks {
    val palette = LocalPalette.current
    val card = LocalWidgetFill.current
    if (LocalBare.current) {
        val white = Color.White
        return GlyphInks(white, white, white, white.copy(alpha = 0.7f), white, white, white, white.copy(alpha = 0.8f), white, white, white.copy(alpha = 0.7f))
    }
    // White parts are the words' ink: white on glass and on a dark card, the card's dark ink on a pale one.
    val ink = palette.onGlass
    // On a pale card iOS's yellow is lost, so the sun is its orange, and the rain its deeper blue.
    val pale = card != null && contrast(Color(Hue.Yellow.dark), card.top) < 1.6f
    return GlyphInks(
        sun = if (pale) Color(Hue.Orange.light) else Color(Hue.Yellow.dark),
        moon = if (pale) ink.copy(alpha = 0.8f) else Color(0xFFF4F1E6),
        cloud = ink,
        farCloud = ink.copy(alpha = 0.55f),
        rain = if (pale) Color(Hue.Blue.light) else Color(Hue.Cyan.dark),
        snow = ink,
        bolt = if (pale) Color(Hue.Orange.light) else Color(Hue.Yellow.dark),
        mist = ink.copy(alpha = 0.75f),
        flame = Color(Hue.Orange.dark),
        flameTip = Color(0xFFFF6A1A),
        flameHeart = Color(Hue.Yellow.dark),
    )
}

/** The words' shadow, for a symbol standing on the wallpaper's glass: null anywhere else. */
@Composable
private fun glyphShadow(): Shadow? = if (LocalBare.current) null else wordShadow()

// ---------------------------------------------------------------------- parts

/** One piece of a symbol: its outline, and which of [GlyphInks] it is painted in. */
/**
 * One piece of a symbol: its outline, and which of [GlyphInks] it is painted in; with [tip], it runs
 * from that at its top to [ink] at its foot.
 */
private class Part(val path: Path, val ink: (GlyphInks) -> Color, val tip: ((GlyphInks) -> Color)? = null)

private fun DrawScope.drawParts(parts: List<Part>, inks: GlyphInks, shadow: Shadow?) {
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        if (shadow != null) {
            // One shadow for the whole symbol, as words have one, drawn first under every part.
            val all = Path().apply { parts.forEach { addPath(it.path) } }
            paint.color = android.graphics.Color.TRANSPARENT
            paint.setShadowLayer(shadow.blurRadius.coerceAtLeast(0.5f), shadow.offset.x, shadow.offset.y, shadow.color.copy(alpha = shadow.color.alpha * 0.8f).toArgb())
            // A shadow layer is cast only by what is painted, so the shape is painted in a colour that
            // is all but nothing; the parts go over it.
            paint.color = android.graphics.Color.argb(2, 0, 0, 0)
            native.drawPath(all, paint)
            paint.clearShadowLayer()
        }
        for (part in parts) {
            val foot = part.ink(inks)
            val tip = part.tip?.invoke(inks)
            if (tip != null) {
                val bounds = RectF().also { part.path.computeBounds(it, true) }
                // A shader is painted at the paint's own alpha, which the shadow left all but clear.
                paint.color = android.graphics.Color.BLACK
                paint.shader = android.graphics.LinearGradient(
                    0f, bounds.top, 0f, bounds.bottom, tip.toArgb(), foot.toArgb(), android.graphics.Shader.TileMode.CLAMP,
                )
            } else {
                paint.shader = null
                paint.color = foot.toArgb()
            }
            native.drawPath(part.path, paint)
        }
        paint.shader = null
    }
}

/** A cloud in the box from [left], [top], [width] wide, as tall as two thirds of that. Fluffier on the left. */
private fun cloud(left: Float, top: Float, width: Float, grow: Float = 0f): Path {
    val h = width / 1.5f
    val path = Path()
    val base = Path().apply {
        addRoundRect(RectF(left - grow, top + h * 0.45f - grow, left + width + grow, top + h + grow), h * 0.275f + grow, h * 0.275f + grow, Path.Direction.CW)
    }
    val big = Path().apply { addCircle(left + width * 0.58f, top + h * 0.42f, h * 0.42f + grow, Path.Direction.CW) }
    val small = Path().apply { addCircle(left + width * 0.30f, top + h * 0.60f, h * 0.30f + grow, Path.Direction.CW) }
    path.op(base, Path.Op.UNION)
    path.op(big, Path.Op.UNION)
    path.op(small, Path.Op.UNION)
    return path
}

/** A sun round ([cx], [cy]): a disc of [r] and eight short rays round it. */
private fun sun(cx: Float, cy: Float, r: Float): Path {
    val path = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
    val ray = r * 0.36f
    val from = r * 1.45f
    val to = r * 2.12f
    for (i in 0 until 8) {
        val angle = Math.toRadians(i * 45.0)
        val one = Path().apply {
            addRoundRect(RectF(from, -ray / 2f, to, ray / 2f), ray / 2f, ray / 2f, Path.Direction.CW)
            transform(Matrix().apply {
                postRotate(Math.toDegrees(angle).toFloat())
                postTranslate(cx, cy)
            })
        }
        path.op(one, Path.Op.UNION)
    }
    return path
}

/** A crescent moon in the circle round ([cx], [cy]) of [r], its hollow to the top right. */
private fun moon(cx: Float, cy: Float, r: Float): Path {
    val path = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
    val bite = Path().apply { addCircle(cx + r * 0.58f, cy - r * 0.52f, r * 0.86f, Path.Direction.CW) }
    path.op(bite, Path.Op.DIFFERENCE)
    return path
}

/** A short rounded stroke from ([x1], [y1]) to ([x2], [y2]), [width] thick. */
private fun stroke(x1: Float, y1: Float, x2: Float, y2: Float, width: Float): Path {
    val length = kotlin.math.hypot(x2 - x1, y2 - y1)
    val angle = Math.toDegrees(kotlin.math.atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())).toFloat()
    return Path().apply {
        addRoundRect(RectF(0f, -width / 2f, length, width / 2f), width / 2f, width / 2f, Path.Direction.CW)
        transform(Matrix().apply {
            postRotate(angle)
            postTranslate(x1, y1)
        })
    }
}

private fun weatherParts(kind: WeatherCodes.Kind, day: Boolean, s: Float): List<Part> {
    val gap = s * 0.045f
    fun withGapFrom(behind: Path, front: Path): Path = Path(behind).apply { op(front, Path.Op.DIFFERENCE) }

    // The cloud most kinds hang their weather from, across the top of the square.
    val upper = { cloud(s * 0.10f, s * 0.12f, s * 0.80f) }
    val upperGap = { cloud(s * 0.10f, s * 0.12f, s * 0.80f, grow = gap) }
    val upperFoot = s * 0.12f + s * 0.80f / 1.5f

    return when (kind) {
        WeatherCodes.Kind.Clear -> if (day) {
            listOf(Part(sun(s * 0.5f, s * 0.5f, s * 0.2f), { it.sun }))
        } else {
            listOf(Part(moon(s * 0.48f, s * 0.52f, s * 0.32f), { it.moon }))
        }

        WeatherCodes.Kind.PartlyCloudy -> {
            val front = cloud(s * 0.24f, s * 0.40f, s * 0.72f)
            val clearance = cloud(s * 0.24f, s * 0.40f, s * 0.72f, grow = gap)
            val behind = if (day) sun(s * 0.38f, s * 0.36f, s * 0.15f) else moon(s * 0.36f, s * 0.36f, s * 0.24f)
            listOf(
                Part(withGapFrom(behind, clearance), if (day) ({ it.sun }) else ({ it.moon })),
                Part(front, { it.cloud }),
            )
        }

        WeatherCodes.Kind.Cloudy -> {
            val back = cloud(s * 0.30f, s * 0.16f, s * 0.62f)
            val front = cloud(s * 0.06f, s * 0.36f, s * 0.78f)
            val clearance = cloud(s * 0.06f, s * 0.36f, s * 0.78f, grow = gap)
            listOf(Part(withGapFrom(back, clearance), { it.farCloud }), Part(front, { it.cloud }))
        }

        WeatherCodes.Kind.Fog -> {
            val top = cloud(s * 0.12f, s * 0.08f, s * 0.76f)
            val lines = Path().apply {
                op(stroke(s * 0.14f, s * 0.70f, s * 0.86f, s * 0.70f, s * 0.07f), Path.Op.UNION)
                op(stroke(s * 0.26f, s * 0.85f, s * 0.74f, s * 0.85f, s * 0.07f), Path.Op.UNION)
            }
            listOf(Part(top, { it.cloud }), Part(lines, { it.mist }))
        }

        WeatherCodes.Kind.Drizzle -> {
            val drops = Path()
            for ((x, y) in listOf(0.32f to 0.76f, 0.52f to 0.76f, 0.72f to 0.76f, 0.42f to 0.90f, 0.62f to 0.90f)) {
                drops.op(stroke(s * x, s * y - s * 0.03f, s * x - s * 0.02f, s * y + s * 0.02f, s * 0.06f), Path.Op.UNION)
            }
            listOf(Part(upper(), { it.cloud }), Part(withGapFrom(drops, upperGap()), { it.rain }))
        }

        WeatherCodes.Kind.Rain -> {
            val drops = Path()
            for (x in listOf(0.34f, 0.52f, 0.70f)) {
                drops.op(stroke(s * x, upperFoot + s * 0.07f, s * x - s * 0.07f, upperFoot + s * 0.25f, s * 0.07f), Path.Op.UNION)
            }
            listOf(Part(upper(), { it.cloud }), Part(withGapFrom(drops, upperGap()), { it.rain }))
        }

        WeatherCodes.Kind.Snow -> {
            val flakes = Path()
            for ((x, y) in listOf(0.32f to 0.75f, 0.52f to 0.75f, 0.72f to 0.75f, 0.42f to 0.89f, 0.62f to 0.89f)) {
                flakes.addCircle(s * x, s * y, s * 0.045f, Path.Direction.CW)
            }
            listOf(Part(upper(), { it.cloud }), Part(withGapFrom(flakes, upperGap()), { it.snow }))
        }

        WeatherCodes.Kind.Storm -> {
            val bolt = Path().apply {
                moveTo(s * 0.55f, s * 0.50f)
                lineTo(s * 0.36f, s * 0.76f)
                lineTo(s * 0.49f, s * 0.76f)
                lineTo(s * 0.42f, s * 0.97f)
                lineTo(s * 0.66f, s * 0.68f)
                lineTo(s * 0.53f, s * 0.68f)
                lineTo(s * 0.62f, s * 0.50f)
                close()
            }
            val boltGap = Path(bolt).apply {
                // The bolt is cut out of the cloud with a little room round it, so it reads as in front.
                transform(Matrix().apply { setScale(1.18f, 1.10f, s * 0.52f, s * 0.72f) })
            }
            val cloudCut = withGapFrom(upper(), boltGap)
            listOf(Part(cloudCut, { it.cloud }), Part(bolt, { it.bolt }))
        }
    }
}

private fun flameParts(s: Float): List<Part> {
    val outer = Path().apply {
        moveTo(s * 0.50f, s * 0.97f)
        cubicTo(s * 0.26f, s * 0.97f, s * 0.13f, s * 0.80f, s * 0.15f, s * 0.60f)
        cubicTo(s * 0.17f, s * 0.44f, s * 0.27f, s * 0.36f, s * 0.31f, s * 0.24f)
        cubicTo(s * 0.37f, s * 0.31f, s * 0.38f, s * 0.38f, s * 0.38f, s * 0.43f)
        cubicTo(s * 0.44f, s * 0.30f, s * 0.48f, s * 0.15f, s * 0.47f, s * 0.03f)
        cubicTo(s * 0.66f, s * 0.15f, s * 0.86f, s * 0.38f, s * 0.86f, s * 0.62f)
        cubicTo(s * 0.86f, s * 0.82f, s * 0.71f, s * 0.97f, s * 0.50f, s * 0.97f)
        close()
    }
    val heart = Path().apply {
        moveTo(s * 0.50f, s * 0.95f)
        cubicTo(s * 0.38f, s * 0.95f, s * 0.31f, s * 0.86f, s * 0.33f, s * 0.75f)
        cubicTo(s * 0.35f, s * 0.64f, s * 0.45f, s * 0.58f, s * 0.50f, s * 0.46f)
        cubicTo(s * 0.59f, s * 0.56f, s * 0.69f, s * 0.67f, s * 0.67f, s * 0.79f)
        cubicTo(s * 0.65f, s * 0.89f, s * 0.58f, s * 0.95f, s * 0.50f, s * 0.95f)
        close()
    }
    return listOf(
        Part(outer, { it.flame }, tip = { it.flameTip }),
        Part(heart, { it.flameHeart }),
    )
}

/** The angle of a point [fraction] of the way round a dial from twelve, in radians, for the dial's sums. */
internal fun dialAngle(fraction: Float): Double = Math.toRadians(fraction * 360.0 - 90.0)

/** The point [radius] from ([cx], [cy]) at [angle] radians. */
internal fun pointAt(cx: Float, cy: Float, radius: Float, angle: Double): Pair<Float, Float> =
    (cx + (cos(angle) * radius).toFloat()) to (cy + (sin(angle) * radius).toFloat())
