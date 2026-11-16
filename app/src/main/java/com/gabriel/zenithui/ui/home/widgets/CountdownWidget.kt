package com.gabriel.zenithui.ui.home.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Event
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.Countdown
import com.gabriel.zenithui.data.CountdownDots
import com.gabriel.zenithui.data.WidgetForm
import com.gabriel.zenithui.ui.home.LocalBare
import com.gabriel.zenithui.ui.home.LocalWidgetWorld
import com.gabriel.zenithui.ui.home.rememberFilled
import com.gabriel.zenithui.ui.home.rememberToday
import com.gabriel.zenithui.ui.theme.glassInk
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor

/**
 * The Countdown: the days left to a day, large, and the whole wait as a field of dots, the ones behind
 * you lit in indigo and the one you are on ringed: a dot a day, a week or a month, as the wait is long
 * and as many as the field has room for. On the day itself it celebrates; after it, it counts the
 * days since.
 *
 * - Mini: the days left.
 * - Strip: the days left, what to and when, and how far along.
 * - Square: what to, the days left, and the wait in dots.
 * - Wide: what to, the days left and the date on the left; the dots on the right.
 * - Tall: what to, the days left and the date, and the dots under them.
 * - Big: what to and when, the days left large, and the dots, with what each one is.
 */
@Composable
internal fun CountdownWidget(form: WidgetForm, width: Dp, height: Dp) {
    val world = LocalWidgetWorld.current
    val settings = world?.settings
    val today = rememberToday()
    val count = Countdown.count(settings?.countdownDay ?: 0L, today)
    if (settings == null || count == Countdown.Count.NotSet) {
        Fitted(form, width, height) { _, _ ->
            Invite(form, "Count down to a day", "Tap to choose the date", world?.let { { it.setUp("countdown") } }) { size ->
                Glyph(Icons.Rounded.Event, size, tint = hue(Hue.Indigo))
            }
        }
        return
    }
    val days = when (count) {
        is Countdown.Count.ToGo -> count.days
        is Countdown.Count.Since -> count.days
        else -> 0L
    }
    val title = settings.countdownTitle.ifBlank { "Countdown" }
    val words = Countdown.words(count, settings.countdownTitle)
    val unit = when (count) {
        is Countdown.Count.ToGo -> if (days == 1L) "day to go" else "days to go"
        is Countdown.Count.Since -> if (days == 1L) "day since" else "days since"
        else -> "today"
    }
    val date = remember(settings.countdownDay) { LocalDate.ofEpochDay(settings.countdownDay).format(DateTimeFormatter.ofPattern("EEE d MMMM")) }
    val from = settings.countdownFrom.takeIf { it in 1 until settings.countdownDay } ?: (settings.countdownDay - days.coerceAtLeast(1L))
    // The bigger the field, the more dots it holds: a wait of weeks shown a dot a day on a Big one.
    val most = when (form) {
        WidgetForm.Square -> 30
        WidgetForm.Tall -> 100
        WidgetForm.Big -> 120
        else -> CountdownDots.MOST
    }
    val plan = remember(from, settings.countdownDay, today, most) {
        CountdownDots.of((settings.countdownDay - from).toInt(), (today.toEpochDay() - from).toInt(), most)
    }
    val progress = Countdown.progress(from, settings.countdownDay, today)
    val indigo = hue(Hue.Indigo)
    val heading = wordHue(indigo)
    val spoken = if (count == Countdown.Count.Today) "$title is today" else "$days $words, $date"
    val each = when (plan.unit) {
        CountdownDots.Unit.Day -> "A dot a day"
        CountdownDots.Unit.Week -> "A dot a week"
        CountdownDots.Unit.Month -> "A dot a month"
    }

    @Composable
    fun Number(size: TextUnit) {
        if (count == Countdown.Count.Today) {
            Glyph(Icons.Rounded.Celebration, (size.value * 1.05f).dp, tint = indigo)
        } else {
            Hero("$days", size, weight = FontWeight.Normal, rolling = true)
        }
    }

    Fitted(form, width, height) { w, _ ->
        val root = Modifier.fillMaxSize().says(spoken)
        when (form) {
            WidgetForm.Mini -> Column(root.padding(INSET_MINI), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Number(24.sp)
                Label(if (count == Countdown.Count.Today) "today" else "days", color = heading)
            }

            WidgetForm.Strip -> Row(root.padding(horizontal = INSET_STRIP), verticalAlignment = Alignment.CenterVertically) {
                Number(28.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Words(title, Type.body, weight = FontWeight.SemiBold)
                    Words(if (w >= 250.dp) "$unit · $date" else unit, Type.meta, color = soft())
                }
                if (w >= 250.dp) {
                    Spacer(Modifier.width(10.dp))
                    Meter(progress, indigo, Modifier.width(64.dp), thickness = 5.dp)
                }
            }

            WidgetForm.Square -> Column(root.padding(INSET)) {
                Label(title, color = heading)
                Row(verticalAlignment = Alignment.Bottom) {
                    Number(44.sp)
                    Spacer(Modifier.width(6.dp))
                    Words(unit, Type.body, weight = FontWeight.Medium, color = soft(), modifier = Modifier.padding(bottom = 5.dp))
                }
                Spacer(Modifier.height(10.dp))
                DotField(plan, indigo, Modifier.weight(1f).fillMaxWidth())
            }

            WidgetForm.Wide -> Row(root.padding(INSET)) {
                Column(Modifier.width(130.dp).fillMaxHeight()) {
                    Label(title, color = heading)
                    Number(50.sp)
                    Words(unit, Type.body, weight = FontWeight.Medium, color = soft())
                    Spacer(Modifier.weight(1f))
                    Words(date, Type.meta, weight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    DotField(plan, indigo, Modifier.weight(1f).fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Words(each, Type.caps, weight = FontWeight.SemiBold, color = soft())
                }
            }

            WidgetForm.Tall -> Column(root.padding(INSET)) {
                Label(title, color = heading)
                Number(52.sp)
                Words(unit, Type.body, weight = FontWeight.Medium, color = soft())
                Spacer(Modifier.height(2.dp))
                Words(date, Type.meta, weight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))
                DotField(plan, indigo, Modifier.weight(1f).fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Words(each, Type.caps, weight = FontWeight.SemiBold, color = soft())
            }

            WidgetForm.Big -> Column(root.padding(INSET)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Label(title, color = heading, modifier = Modifier.weight(1f))
                    Words(date, Type.meta, weight = FontWeight.SemiBold, color = soft())
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Number(64.sp)
                    Spacer(Modifier.width(8.dp))
                    Words(words, Type.headline, weight = FontWeight.Medium, color = soft(), modifier = Modifier.padding(bottom = 8.dp))
                }
                Spacer(Modifier.height(14.dp))
                DotField(plan, indigo, Modifier.weight(1f).fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Words(each, Type.caps, weight = FontWeight.SemiBold, color = soft(), modifier = Modifier.weight(1f))
                    Words("${(progress * 100).toInt()}% of the way", Type.caps, weight = FontWeight.SemiBold, color = soft())
                }
            }
        }
    }
}

/**
 * The wait as a field of dots, as big as fit the room and set in the middle of it: the ones behind lit
 * in [colour], the one now ringed in it, the ones ahead faint. They light one after another as it
 * first appears.
 */
@Composable
private fun DotField(plan: CountdownDots.Plan, colour: Color, modifier: Modifier) {
    val bare = LocalBare.current
    val faint = faint(0.2f)
    val lit = rememberFilled(if (plan.total == 0) 0f else plan.gone / plan.total.toFloat())
    Box(modifier) {
        Canvas(Modifier.fillMaxSize().then(if (bare) Modifier.glassInk(bevel = 1.5.dp) else Modifier)) {
            val n = plan.total.coerceAtLeast(1)
            // The largest dot, with half a dot between, for which every dot fits.
            var dot = 26.dp.toPx()
            var columns = 1
            var rows = 1
            while (dot > 3.dp.toPx()) {
                val gap = dot * 0.55f
                columns = floor((size.width + gap) / (dot + gap)).toInt().coerceAtLeast(1)
                rows = ceil(n / columns.toFloat()).toInt()
                if (rows * (dot + gap) - gap <= size.height) break
                dot -= 0.5.dp.toPx()
            }
            val gap = dot * 0.55f
            val used = columns.coerceAtMost(n)
            // Spread across the room, so a short wait still fills its row from edge to edge, and set
            // in the middle of the room's height.
            val pitch = (if (used > 1) (size.width - dot) / (used - 1) else 0f).coerceAtMost(dot + gap * 3f)
            val top = ((size.height - (rows * (dot + gap) - gap)) / 2f).coerceAtLeast(0f)
            val shown = lit.value * plan.total
            for (i in 0 until n) {
                val col = i % columns
                val row = i / columns
                val center = Offset(dot / 2f + col * pitch, top + dot / 2f + row * (dot + gap))
                when {
                    i < shown.toInt() -> drawCircle(if (bare) Color.White else colour, dot / 2f, center)
                    i == plan.gone && plan.gone < plan.total -> {
                        drawCircle(faint, dot / 2f, center)
                        drawCircle(if (bare) Color.White else colour, dot / 2f - 0.75.dp.toPx(), center, style = Stroke(1.5.dp.toPx()))
                    }
                    else -> if (!bare) drawCircle(faint, dot / 2f, center) else drawCircle(Color.White.copy(alpha = 0.35f), dot / 2f, center)
                }
            }
        }
    }
}
