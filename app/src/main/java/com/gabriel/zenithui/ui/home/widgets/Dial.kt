package com.gabriel.zenithui.ui.home.widgets

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.gabriel.zenithui.data.ClockWords
import com.gabriel.zenithui.ui.home.LocalBare
import com.gabriel.zenithui.ui.home.LocalWidgetFill
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import com.gabriel.zenithui.ui.theme.glassInk
import java.time.LocalDateTime

/*
 * The clock face every clock in the set is drawn with, at every size: the Clock widget's, and each
 * city's on the World Clock. It has three faces. On glass it is a disc of paper by day and of night by
 * night, as an iPhone's clock is, so the time reads at a glance whatever the wallpaper; on a colour
 * card it is the card itself, with a faint well; in a "No glass" widget there is no disc, and the
 * marks and hands are glass. The second hand is orange on every face, the one colour on it.
 */

/** Which of the dial's faces it wears. */
internal enum class DialFace { Day, Night, Clear }

/** The face for a clock showing [hour]: by day or by night on glass, and the card's or glass's anywhere else. */
@Composable
internal fun dialFaceAt(hour: Int): DialFace = when {
    LocalBare.current || LocalWidgetFill.current != null -> DialFace.Clear
    ClockWords.dayFace(hour) -> DialFace.Day
    else -> DialFace.Night
}

/** The paper of a day face and the night of a night one. */
private val PAPER = Color(0xFFF6F5F2)
private val NIGHT = Color(0xFF141416)

/** The ink of the marks and hands on a day face. */
private val PAPER_INK = Color(0xFF1C1C1E)

/**
 * A clock face showing [time]. [numerals] writes the hours round it, for a dial big enough to read
 * them; without them the hours are marks. [label] is a word or two under twelve, a city or the day,
 * in [labelHue] when it is given. [seconds] adds the orange second hand.
 */
@Composable
internal fun Dial(
    time: LocalDateTime,
    face: DialFace,
    modifier: Modifier = Modifier,
    numerals: Boolean = false,
    seconds: Boolean = true,
    label: String? = null,
    labelHue: Hue? = null,
) {
    val bare = LocalBare.current
    val ink = when (face) {
        DialFace.Day -> PAPER_INK
        DialFace.Night -> Color.White
        DialFace.Clear -> if (bare) Color.White else ink()
    }
    val second = if (bare) Color.White else when (face) {
        DialFace.Day -> Color(Hue.Orange.light)
        DialFace.Night -> Color(Hue.Orange.dark)
        DialFace.Clear -> hue(Hue.Orange)
    }
    val labelInk = when {
        labelHue == null || bare -> ink.copy(alpha = 0.6f)
        face == DialFace.Day -> Color(labelHue.light)
        face == DialFace.Night -> Color(labelHue.dark)
        else -> hue(labelHue)
    }
    val well = faint(0.08f)
    Box(modifier.then(if (bare) Modifier.glassInk(bevel = 1.6.dp) else Modifier)) {
        DialFaceLayer(face, ink, labelInk, if (bare) null else well, numerals, label, Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) {
            drawHands(time, face, ink, second, seconds)
        }
    }
}

/** Everything on the dial that stands still: the disc, its marks or numerals, and its label. */
@Composable
private fun DialFaceLayer(
    face: DialFace,
    ink: Color,
    labelInk: Color,
    /** The faint disc a Clear face sits on, or null for none. */
    well: Color?,
    numerals: Boolean,
    label: String?,
    modifier: Modifier,
) {
    val measurer = rememberTextMeasurer(cacheSize = 16)
    val family = LocalZenithFont.current
    Box(
        modifier.drawWithCache {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)
            // The hours, written round the dial where it is big enough, laid out once for its size.
            val written = if (numerals) {
                val style = TextStyle(fontFamily = family, fontSize = (r * 0.21f).toSp(this), fontWeight = FontWeight.Medium, color = ink)
                (1..12).map { hour -> hour to measurer.measure(hour.toString(), style) }
            } else {
                emptyList()
            }
            val labelled = label?.let {
                // No smaller than a widget's smallest words, however small the dial.
                val size = maxOf(r * 0.13f, Type.caps.toPx())
                measurer.measure(
                    it,
                    TextStyle(fontFamily = family, fontSize = size.toSp(this), fontWeight = FontWeight.Bold, color = labelInk, letterSpacing = (size * 0.09f).toSp(this)),
                )
            }
            onDrawBehind {
                when (face) {
                    DialFace.Day, DialFace.Night -> {
                        // A soft shadow straight down under the disc, as under everything that stands on glass.
                        drawIntoCanvas { canvas ->
                            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = (if (face == DialFace.Day) PAPER else NIGHT).toArgb()
                                setShadowLayer(r * 0.09f, 0f, r * 0.035f, Color.Black.copy(alpha = 0.28f).toArgb())
                            }
                            canvas.nativeCanvas.drawCircle(c.x, c.y, r, paint)
                        }
                        if (face == DialFace.Night) {
                            drawCircle(Color.White.copy(alpha = 0.16f), r - 0.5.dp.toPx(), c, style = Stroke(1.dp.toPx()))
                        }
                    }
                    DialFace.Clear -> if (well != null) drawCircle(well, r, c)
                }
                val markR = if (face == DialFace.Clear) r * 0.97f else r * 0.9f
                if (numerals) {
                    // A fine mark for every minute round the rim, the hours' a little stronger, and
                    // the hours written inside them.
                    for (minute in 0 until 60) {
                        val hour = minute % 5 == 0
                        val angle = dialAngle(minute / 60f)
                        val (x1, y1) = pointAt(c.x, c.y, markR, angle)
                        val (x2, y2) = pointAt(c.x, c.y, markR - r * (if (hour) 0.065f else 0.045f), angle)
                        drawLine(
                            ink.copy(alpha = if (hour) 0.8f else 0.3f),
                            Offset(x1, y1), Offset(x2, y2),
                            (r * (if (hour) 0.02f else 0.012f)).coerceAtLeast(0.6f),
                            StrokeCap.Round,
                        )
                    }
                    for ((hour, layout) in written) {
                        val (x, y) = pointAt(c.x, c.y, r * 0.68f, dialAngle(hour / 12f))
                        drawText(layout, topLeft = Offset(x - layout.size.width / 2f, y - layout.size.height / 2f))
                    }
                } else {
                    for (hour in 0 until 12) {
                        val angle = dialAngle(hour / 12f)
                        val quarter = hour % 3 == 0
                        val (x1, y1) = pointAt(c.x, c.y, markR, angle)
                        val (x2, y2) = pointAt(c.x, c.y, markR - r * (if (quarter) 0.16f else 0.1f), angle)
                        drawLine(ink.copy(alpha = if (quarter) 1f else 0.55f), Offset(x1, y1), Offset(x2, y2), r * (if (quarter) 0.05f else 0.035f), StrokeCap.Round)
                    }
                }
                if (labelled != null) {
                    drawText(labelled, topLeft = Offset(c.x - labelled.size.width / 2f, c.y - r * 0.4f - labelled.size.height / 2f))
                }
            }
        }
    )
}

/** Turns a size in pixels into the scaled points text is set in. */
private fun Float.toSp(density: Density) = with(density) { this@toSp.toSp() }

/**
 * The hands at [time]: hour and minute in the face's ink, each a thin stem from the middle and a
 * broad blade beyond it, and the orange second hand over them with its counterweight and its hub.
 * They stand on a soft shadow straight down, as the face does.
 */
private fun DrawScope.drawHands(time: LocalDateTime, face: DialFace, ink: Color, second: Color, seconds: Boolean) {
    val r = size.minDimension / 2f
    val c = center
    val minutes = time.minute + time.second / 60f
    val hours = (time.hour % 12) + minutes / 60f
    val shadow = face != DialFace.Clear
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            if (shadow) setShadowLayer(r * 0.05f, 0f, r * 0.025f, Color.Black.copy(alpha = if (face == DialFace.Day) 0.22f else 0.4f).toArgb())
        }
        fun hand(fraction: Float, length: Float, blade: Float) {
            val angle = dialAngle(fraction)
            val stem = r * 0.13f
            val (sx, sy) = pointAt(c.x, c.y, stem, angle)
            val (ex, ey) = pointAt(c.x, c.y, length, angle)
            paint.color = ink.toArgb()
            paint.strokeWidth = r * 0.035f
            native.drawLine(c.x, c.y, sx, sy, paint)
            paint.strokeWidth = blade
            native.drawLine(sx, sy, ex, ey, paint)
        }
        hand(hours / 12f, r * 0.5f, r * 0.085f)
        hand(minutes / 60f, r * 0.76f, r * 0.07f)
        paint.style = Paint.Style.FILL
        paint.color = ink.toArgb()
        native.drawCircle(c.x, c.y, r * 0.06f, paint)
        if (seconds) {
            val angle = dialAngle(time.second / 60f)
            val (tx, ty) = pointAt(c.x, c.y, -r * 0.2f, angle)
            val (ex, ey) = pointAt(c.x, c.y, r * 0.86f, angle)
            paint.style = Paint.Style.STROKE
            paint.color = second.toArgb()
            paint.strokeWidth = (r * 0.022f).coerceAtLeast(1f)
            native.drawLine(tx, ty, ex, ey, paint)
            paint.style = Paint.Style.FILL
            native.drawCircle(c.x, c.y, r * 0.045f, paint)
        }
        paint.clearShadowLayer()
        paint.style = Paint.Style.FILL
        // A pin through the middle, in the face's own colour.
        paint.color = when (face) {
            DialFace.Day -> PAPER.toArgb()
            DialFace.Night -> NIGHT.toArgb()
            DialFace.Clear -> Color.Transparent.toArgb()
        }
        if (face != DialFace.Clear) native.drawCircle(c.x, c.y, r * 0.018f, paint)
    }
}
