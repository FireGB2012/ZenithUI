package com.gabriel.zenithui.ui.home.widgets

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.gabriel.zenithui.data.WidgetForm
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.home.LocalBare
import com.gabriel.zenithui.ui.home.LocalWidgetFill
import com.gabriel.zenithui.ui.home.contrast
import com.gabriel.zenithui.ui.home.quiet
import com.gabriel.zenithui.ui.home.rememberFilled
import com.gabriel.zenithui.ui.home.tight
import com.gabriel.zenithui.ui.theme.LocalLite
import com.gabriel.zenithui.ui.theme.LocalOnWallpaper
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.glassInk

/*
 * The design every one of Zenith's widgets is drawn to. One inset, one set of type sizes, one way
 * of setting numbers, a few colours that each mean one thing, and the shapes the widgets share:
 * rings, bars, wells and the little symbols. A widget picks from these rather than making up its
 * own, which is what makes the set read as one family.
 *
 * Every piece works in the three places a widget can be: on a pane of glass over the wallpaper
 * (white words with the names' soft shadow), on a colour card (the card's own inks), and with no
 * pane at all ([LocalBare]), where the big numbers and symbols are made of glass.
 */

/** The room every widget leaves round its edge, as an iPhone's widgets do. */
internal val INSET = 16.dp

/** The same for a widget one icon big, and for a strip one row tall. */
internal val INSET_MINI = 9.dp
internal val INSET_STRIP = 14.dp

/**
 * The corners of what sits inside a widget, as far in as [INSET]: the widget's own corner less the
 * inset would leave them almost square, so they are a little rounder than that, as an iPhone's are.
 */
internal val INNER_CORNER = 12.dp

/** The type sizes the widgets use, and nothing between them. */
internal object Type {
    /** A place, a song, an event: the thing the widget is about. */
    val headline = 15.sp

    /** What is said about it. */
    val body = 13.sp

    /** Times, counts and other small print. */
    val meta = 12.sp

    /** Upper-case labels over a number: a weekday, a unit. */
    val caps = 11.sp
}

/**
 * The colours a widget may use, each for one thing, as iOS's own widgets use them: red for the
 * calendar's day, orange for the second hand and the streak's flame, yellow for the sun and for
 * notes, green for a healthy battery, cyan for rain, blue for lists and actions, indigo for a
 * countdown. A lighter version for dark glass and dark cards, a deeper one for pale cards.
 */
internal enum class Hue(val dark: Long, val light: Long) {
    Red(0xFFFF453A, 0xFFFF3B30),
    Orange(0xFFFF9F0A, 0xFFFF9500),
    Yellow(0xFFFFD60A, 0xFFFFCC00),
    Green(0xFF30D158, 0xFF34C759),
    Cyan(0xFF64D2FF, 0xFF32ADE6),
    Blue(0xFF0A84FF, 0xFF007AFF),
    Indigo(0xFF7D7AFF, 0xFF5856D6),
    Purple(0xFFBF5AF2, 0xFFAF52DE),
}

/**
 * [hue] as this widget wears it: the version for its ink (light on dark glass, deeper on a pale
 * card), giving way to the widget's own colour as that eases in, and to the card's ink on a colour
 * card it would not stand out on.
 */
@Composable
internal fun hue(hue: Hue): Color {
    val palette = LocalPalette.current
    val base = Color(if (palette.dark) hue.dark else hue.light)
    val worn = if (palette.ownColour > 0f) lerp(base, palette.accent, palette.ownColour.coerceAtMost(1f)) else base
    val card = LocalWidgetFill.current ?: return worn
    return if (minOf(contrast(worn, card.top), contrast(worn, card.bottom)) < STANDS_OUT) palette.onGlass else worn
}

/**
 * A hue as small words wear it: a heading, a weekday, a count. On the wallpaper, through its glass or
 * straight on it in a "No glass" widget, the picture can be any colour, this one among them, and only
 * the words' ink reads in small letters whatever it is, so there they are in that ink, as an iPhone's
 * clear widgets have them, and the colour stays in the symbols and marks beside them. On a card, or a
 * widget shown in a sheet, [colour] itself.
 */
@Composable
internal fun wordHue(colour: Color): Color =
    if (LocalOnWallpaper.current && LocalWidgetFill.current == null) ink() else colour

/** How far a colour must stand out from a card to be used on it: what WCAG asks of a symbol. */
private const val STANDS_OUT = 2.6f

/** The words' ink: white on glass, the card's ink on a card. */
@Composable
internal fun ink(): Color = LocalPalette.current.onGlass

/** A second line's ink, as strong as it needs to be to read where it is. */
@Composable
internal fun soft(): Color = quiet()

/** Decoration that is not read: tracks, faint marks, a well's fill. */
@Composable
internal fun faint(alpha: Float = 0.16f): Color =
    LocalPalette.current.onGlass.copy(alpha = if (LocalBare.current) alpha * 1.25f else alpha)

/**
 * One line of a widget's words, in one of the [Type] sizes. It keeps the words' shadow from where
 * it is drawn ([LocalTextStyle]), sets its line no taller than its letters need, and cuts a line
 * that is too long short with an ellipsis rather than spilling.
 */
@Composable
internal fun Words(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Medium,
    color: Color = ink(),
    maxLines: Int = 1,
    align: TextAlign = TextAlign.Unspecified,
    tracking: TextUnit = TextUnit.Unspecified,
    tabular: Boolean = false,
    /** A line through it, for something done. */
    strike: Boolean = false,
) {
    Text(
        text = text,
        modifier = modifier,
        style = LocalTextStyle.current.merge(
            TextStyle(
                color = color,
                fontSize = size,
                fontWeight = weight,
                letterSpacing = tracking,
                textAlign = align,
                lineHeight = size * 1.24f,
                textDecoration = if (strike) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                fontFeatureSettings = if (tabular) "tnum" else null,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
            )
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        softWrap = maxLines > 1,
    )
}

/** An upper-case label: a weekday over a date, a unit under a number. */
@Composable
internal fun Label(text: String, modifier: Modifier = Modifier, color: Color = soft(), size: TextUnit = Type.caps) {
    Words(text.uppercase(), size, modifier, weight = FontWeight.Bold, color = color, tracking = 0.6.sp)
}

/**
 * The first of [choices] that fits its room on one line, so a line says as much as there is room for
 * and is never cut off; the last one, cut short, if none does. They are written fullest first.
 */
@Composable
internal fun FirstThatFits(
    choices: List<String>,
    size: TextUnit,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Medium,
    color: Color = ink(),
    tabular: Boolean = false,
) {
    Layout(
        content = { choices.forEach { Words(it, size, weight = weight, color = color, tabular = tabular) } },
        modifier = modifier,
    ) { lines, constraints ->
        val pick = lines.firstOrNull { it.maxIntrinsicWidth(Constraints.Infinity) <= constraints.maxWidth } ?: lines.last()
        val line = pick.measure(constraints.copy(minWidth = 0))
        layout(line.width.coerceAtLeast(constraints.minWidth), line.height) { line.place(0, 0) }
    }
}

/**
 * A widget's headline number: a time, a temperature, a count. Set in the numbers' font with its
 * figures all one width, so a number that changes keeps its place, and with nothing above or below
 * its figures, so it sits exactly where it is put. In a "No glass" widget it is glass itself, set
 * heavier to give the light some body to bend in.
 */
@Composable
internal fun Hero(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Light,
    color: Color = Color.Unspecified,
    rolling: Boolean = false,
) {
    val bare = LocalBare.current
    val style = if (bare) {
        tight(size * 0.94f, FontWeight.SemiBold, keepWeight = true).copy(color = Color.White, fontFeatureSettings = "tnum")
    } else {
        tight(size, weight).copy(
            color = if (color.isSpecified) color else ink(),
            shadow = LocalTextStyle.current.shadow,
            fontFeatureSettings = "tnum",
        )
    }
    val look = if (bare) Modifier.glassInk(bevel = with(LocalDensity.current) { size.toDp() } * 0.07f) else Modifier
    if (rolling) {
        Rolling(text, style, modifier.then(look))
    } else {
        Text(degreesTight(text), style = style, maxLines = 1, softWrap = false, modifier = modifier.then(look))
    }
}

/**
 * How far a degree sign is pulled in towards its number, as a share of the type's size: the font
 * sets it as a letter of its own, with room before it, where a temperature wants it up against the
 * figure, as iOS sets it.
 */
private const val DEGREE_PULL = 0.1f

/** [text] with the figure before a degree sign set closer to it. */
private fun degreesTight(text: String): androidx.compose.ui.text.AnnotatedString {
    val at = text.indexOf('°')
    if (at <= 0) return androidx.compose.ui.text.AnnotatedString(text)
    return androidx.compose.ui.text.buildAnnotatedString {
        append(text)
        addStyle(androidx.compose.ui.text.SpanStyle(letterSpacing = (-DEGREE_PULL * 1f).em), at - 1, at)
    }
}

/**
 * Words whose characters roll over one at a time as they change, the way a departures board's do:
 * only the figure that changes moves, up out of the way as the new one comes up from under it.
 * With movement reduced the new figure fades in where the old one was.
 */
@Composable
internal fun Rolling(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    val reduced = LocalMotionLevel.current.reduced
    Row(modifier) {
        text.forEachIndexed { index, char ->
            // Keyed by place from the end, so "9:59" to "10:00" keeps the minutes in their places.
            androidx.compose.runtime.key(text.length - index) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        if (reduced) {
                            fadeIn(tween(150)).togetherWith(fadeOut(tween(150)))
                        } else {
                            (slideInVertically(spring(dampingRatio = 0.8f, stiffness = 300f)) { it / 2 } + fadeIn(tween(260)))
                                .togetherWith(slideOutVertically(tween(260)) { -it / 2 } + fadeOut(tween(180)))
                        }
                    },
                    label = "rolling",
                ) { shown ->
                    val pull = if (shown == '°') with(LocalDensity.current) { (style.fontSize * DEGREE_PULL).toDp() } else 0.dp
                    Text(shown.toString(), style = style, maxLines = 1, softWrap = false, modifier = Modifier.layout { measurable, constraints ->
                        // The degree sign is drawn where it is, but takes that much less room before it.
                        val placeable = measurable.measure(constraints)
                        val back = pull.roundToPx()
                        layout((placeable.width - back).coerceAtLeast(0), placeable.height) { placeable.place(-back, 0) }
                    })
                }
            }
        }
    }
}

/**
 * A well: a patch a shade lighter than the widget, with rounder corners, that holds a group of
 * things together (a stat, a row of hours) without a line round it.
 */
@Composable
internal fun Modifier.well(corner: Dp = INNER_CORNER): Modifier {
    val alpha = when {
        LocalBare.current -> 0.12f
        LocalWidgetFill.current != null -> 0.07f
        else -> 0.10f
    }
    return this.background(LocalPalette.current.onGlass.copy(alpha = alpha), RoundedCornerShape(corner))
}

/** A hairline across the widget, between two parts of it. */
@Composable
internal fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(0.75.dp).background(faint(0.18f)))
}

/**
 * How full something is, as a bar with round ends: a faint track and [fraction] of it lit in
 * [colour], never less than a dot. It runs up to its value when it first appears and whenever it
 * changes. In a "No glass" widget the lit part is a rod of glass.
 */
@Composable
internal fun Meter(fraction: Float, colour: Color, modifier: Modifier, thickness: Dp = 6.dp) {
    val bare = LocalBare.current
    val filled = rememberFilled(fraction.coerceIn(0f, 1f))
    val track = faint()
    Box(modifier.height(thickness)) {
        Canvas(Modifier.fillMaxSize()) {
            val h = size.height
            drawRoundRect(track, cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f))
            if (!bare) {
                val lit = (size.width * filled.value).coerceIn(h, size.width)
                drawRoundRect(colour, size = Size(lit, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f))
            }
        }
        if (bare && fraction > 0f) {
            Canvas(Modifier.fillMaxSize().glassInk(bevel = thickness * 0.4f)) {
                val h = size.height
                val lit = (size.width * filled.value).coerceIn(h, size.width)
                drawRoundRect(Color.White, size = Size(lit, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f))
            }
        }
    }
}

/**
 * A ring that is [fraction] full, from the top round to the right, in [colour] over a faint track,
 * with [content] in the middle. While [glint] a soft light runs round the lit part, over and over:
 * the one thing that moves by itself, kept for a battery that is charging. In a "No glass" widget the
 * lit part is a band of glass.
 */
@Composable
internal fun Gauge(
    fraction: Float,
    colour: Color,
    stroke: Dp,
    modifier: Modifier,
    glint: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val bare = LocalBare.current
    val reduced = LocalMotionLevel.current.reduced
    val filled = rememberFilled(fraction.coerceIn(0f, 1f))
    val track = faint()
    // Lite draws nothing that moves by itself, so the ring charges without its glint.
    val running = if (glint && !reduced && !LocalLite.current) {
        rememberInfiniteTransition(label = "gaugeGlint").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
            label = "glint",
        )
    } else {
        null
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val width = stroke.toPx()
            val side = size.minDimension - width
            val topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f)
            val ring = Stroke(width = width, cap = StrokeCap.Round)
            drawArc(track, 0f, 360f, false, topLeft, Size(side, side), style = Stroke(width))
            val sweep = 360f * filled.value
            if (!bare && sweep > 0f) drawArc(colour, -90f, sweep.coerceAtLeast(0.5f), false, topLeft, Size(side, side), style = ring)
            val along = running?.value
            if (along != null && sweep > GLINT) {
                drawArc(
                    Color.White,
                    -90f + (sweep - GLINT) * along,
                    GLINT,
                    false,
                    topLeft,
                    Size(side, side),
                    alpha = 0.55f * kotlin.math.sin(along * Math.PI).toFloat(),
                    style = ring,
                )
            }
        }
        if (bare && fraction > 0f) {
            Canvas(Modifier.fillMaxSize().glassInk(bevel = stroke * 0.4f)) {
                val width = stroke.toPx()
                val side = size.minDimension - width
                drawArc(
                    Color.White, -90f, (360f * filled.value).coerceAtLeast(0.5f), false,
                    Offset((size.width - side) / 2f, (size.height - side) / 2f), Size(side, side),
                    style = Stroke(width = width, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/** How much of the ring the charging light covers, in degrees. */
private const val GLINT = 30f

/**
 * A widget with nothing to show yet: a symbol in a round well, what is missing, and what a tap does
 * about it, centred. [onClick] null leaves it as words only.
 */
@Composable
internal fun Invite(
    form: WidgetForm,
    title: String,
    detail: String?,
    onClick: (() -> Unit)?,
    symbol: @Composable (Dp) -> Unit,
) {
    val small = form == WidgetForm.Mini || form == WidgetForm.Strip
    val tap = if (onClick != null) {
        Modifier
            .pressBounce(0.96f)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
    } else {
        Modifier
    }
    if (form == WidgetForm.Strip) {
        Row(
            Modifier.fillMaxSize().then(tap).padding(horizontal = INSET_STRIP),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(36.dp).well(18.dp), contentAlignment = Alignment.Center) { symbol(20.dp) }
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Words(title, Type.body, weight = FontWeight.SemiBold)
                if (detail != null) Words(detail, Type.meta, color = soft())
            }
        }
        return
    }
    Column(
        Modifier.fillMaxSize().then(tap).padding(if (small) INSET_MINI else INSET),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val well = if (small) 34.dp else 46.dp
        Box(Modifier.size(well).well(well / 2), contentAlignment = Alignment.Center) { symbol(well * 0.52f) }
        if (!small) {
            Spacer(Modifier.height(10.dp))
            Words(title, Type.body, weight = FontWeight.SemiBold, align = TextAlign.Center, maxLines = 2)
            if (detail != null) {
                Spacer(Modifier.height(2.dp))
                Words(detail, Type.meta, color = soft(), align = TextAlign.Center, maxLines = 2)
            }
        }
    }
}

/**
 * The room each shape of widget is designed in, in dp across and down. A widget smaller than its
 * room (a small phone, a big font) is drawn a little smaller, everything in it together, so it
 * still fits as designed; a bigger one keeps its size and its parts take the room. Strips are
 * designed to their height alone, as they run any width.
 */
private fun designRoom(form: WidgetForm): Pair<Float, Float> = when (form) {
    WidgetForm.Mini -> 62f to 62f
    WidgetForm.Strip -> 0f to 58f
    WidgetForm.Square -> 158f to 158f
    WidgetForm.Wide -> 338f to 158f
    WidgetForm.Tall -> 158f to 354f
    WidgetForm.Big -> 338f to 354f
}

/** As far as a widget is drawn smaller to fit before it is left to cut its words short instead. */
private const val LEAST_FIT = 0.74f

/** As much as a widget lets the phone's font size grow its words: more and the design breaks. */
private const val MOST_FONT_SCALE = 1.15f

/**
 * [content] drawn in the room it was designed for ([designRoom]): told the width and height it has
 * once fitted, in dp.
 */
@Composable
internal fun Fitted(form: WidgetForm, width: Dp, height: Dp, content: @Composable (width: Dp, height: Dp) -> Unit) {
    val (across, down) = designRoom(form)
    val byWidth = if (across > 0f) width.value / across else 1f
    val byHeight = if (down > 0f) height.value / down else 1f
    val shrink = minOf(1f, byWidth, byHeight).coerceAtLeast(LEAST_FIT)
    val density = LocalDensity.current
    val fontScale = density.fontScale.coerceAtMost(MOST_FONT_SCALE)
    if (shrink >= 0.999f && fontScale == density.fontScale) {
        content(width, height)
    } else {
        CompositionLocalProvider(LocalDensity provides Density(density.density * shrink, fontScale)) {
            content(width / shrink, height / shrink)
        }
    }
}

/** What a screen reader hears for a widget that is only to be read: the whole of it, as one. */
internal fun Modifier.says(words: String): Modifier = semantics(mergeDescendants = true) { contentDescription = words }

/**
 * Whether the phone tells the time in 24 hours. Looked at again whenever the home screen comes back,
 * since it is changed in the phone's settings.
 */
@Composable
internal fun rememberH24(): Boolean {
    val context = LocalContext.current
    var back by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { back++ }
    return remember(back) { android.text.format.DateFormat.is24HourFormat(context) }
}

/**
 * When the phone's next alarm goes off, in milliseconds, or null for none. Android says when it
 * changes; it needs no permission to read.
 */
@Composable
internal fun rememberNextAlarm(): Long? {
    val context = LocalContext.current
    var changed by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { changed++ }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                changed++
            }
        }
        runCatching {
            ContextCompat.registerReceiver(
                context, receiver, IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return remember(changed) {
        runCatching { context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.triggerTime }.getOrNull()
    }
}

/** A round button's well, for a symbol in the middle of something: CircleShape at [size]. */
@Composable
internal fun Modifier.roundWell(size: Dp): Modifier = this.size(size).well(size / 2)

/** The shape of a round thing, for clips. */
internal val Round = CircleShape
