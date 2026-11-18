package com.gabriel.zenithui.ui.home.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.ClockWords
import com.gabriel.zenithui.data.Greeting
import com.gabriel.zenithui.data.WidgetForm
import com.gabriel.zenithui.ui.home.rememberNow
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The Clock. A clock face where there is room for one, the way a watch tells the time at a glance,
 * and the time in figures where the widget is a line; the bigger ones have both, with the date, a
 * greeting and the next alarm. Its face is paper by day and night by night ([dialFaceAt]).
 *
 * - Mini: the face alone.
 * - Strip: the time, the day (shortened where the whole of it does not fit) and the date, and the
 *   next alarm where it fits.
 * - Square: the face with its hours written round it and the day under twelve.
 * - Wide: a greeting, the time and the date, beside the face.
 * - Tall: the face, the time and the date under it, and the next alarm at the foot.
 * - Big: a greeting, the date and the time across the top, the face large, and the next alarm under it.
 */
@Composable
internal fun ClockWidget(form: WidgetForm, width: Dp, height: Dp, userName: String) {
    val now = rememberNow()
    val h24 = rememberH24()
    val alarmAt = rememberNextAlarm()
    val time = ClockWords.time(now, h24)
    val half = ClockWords.half(now, h24)
    val weekday = remember(now.toLocalDate()) { now.format(DateTimeFormatter.ofPattern("EEEE")) }
    val shortWeekday = remember(now.toLocalDate()) { now.format(DateTimeFormatter.ofPattern("EEE")) }
    val date = remember(now.toLocalDate()) { now.format(DateTimeFormatter.ofPattern("d MMMM")) }
    val shortDate = remember(now.toLocalDate()) { now.format(DateTimeFormatter.ofPattern("d MMM")) }
    val short = remember(now.toLocalDate()) { now.format(DateTimeFormatter.ofPattern("EEE d")).uppercase() }
    // The alarm is read against the minute, so it moves on from "Tomorrow" when the day does.
    val alarm = remember(alarmAt, now.hour, now.minute, h24) {
        alarmAt?.let { ClockWords.alarm(it, System.currentTimeMillis(), ZoneId.systemDefault(), h24) }
    }
    val greeting = remember(now.hour, userName) { Greeting.of(now.hour, userName) }
    val face = dialFaceAt(now.hour)
    val spoken = "$time${half?.let { " $it" } ?: ""}, $weekday $date" + (alarm?.let { ", alarm $it" } ?: "")

    Fitted(form, width, height) { w, _ ->
        val root = Modifier.fillMaxSize().says(spoken)
        when (form) {
            WidgetForm.Mini -> Box(root.padding(5.dp)) {
                Dial(now, face, Modifier.fillMaxSize())
            }

            WidgetForm.Strip -> Row(
                root.padding(horizontal = INSET_STRIP),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TimeLine(time, half, 30.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    // "Wed" where "Wednesday" would be cut short: at its smallest it read "Wedne...".
                    FirstThatFits(listOf(weekday, shortWeekday), Type.body, weight = FontWeight.SemiBold)
                    Words(if (w >= 250.dp) date else shortDate, Type.meta, color = soft())
                }
                if (alarm != null && w >= 250.dp) {
                    Spacer(Modifier.width(8.dp))
                    AlarmChip(alarm)
                }
            }

            WidgetForm.Square -> Box(root.padding(10.dp), contentAlignment = Alignment.Center) {
                Dial(now, face, Modifier.fillMaxSize(), numerals = true, label = short, labelHue = Hue.Red)
            }

            WidgetForm.Wide -> Row(root.padding(INSET), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    Label(greeting)
                    Column {
                        TimeLine(time, half, 50.sp)
                        Spacer(Modifier.height(6.dp))
                        Words("$weekday, $date", Type.body, weight = FontWeight.Medium, color = soft())
                    }
                    if (alarm != null) AlarmLine(alarm) else Spacer(Modifier.height(1.dp))
                }
                Spacer(Modifier.width(14.dp))
                Dial(now, face, Modifier.fillMaxHeight().aspectRatio(1f), numerals = true)
            }

            WidgetForm.Tall -> Column(root.padding(INSET), verticalArrangement = Arrangement.SpaceBetween) {
                Dial(now, face, Modifier.fillMaxWidth().aspectRatio(1f), numerals = true)
                Column {
                    TimeLine(time, half, 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Words(weekday, Type.headline, weight = FontWeight.SemiBold)
                    Words(date, Type.body, color = soft())
                }
                if (alarm != null) AlarmWell(alarm) else GreetingWell(greeting)
            }

            WidgetForm.Big -> Column(root.padding(INSET)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Label(greeting)
                        Spacer(Modifier.height(3.dp))
                        Words("$weekday, $date", Type.headline, weight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(10.dp))
                    TimeLine(time, half, 30.sp)
                }
                Box(Modifier.weight(1f).fillMaxWidth().padding(top = 14.dp), contentAlignment = Alignment.Center) {
                    Dial(now, face, Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true), numerals = true)
                }
                if (alarm != null) {
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { AlarmChip(alarm) }
                }
            }
        }
    }
}

/** The time in figures, as large as [size], with AM or PM beside it, smaller, where the phone uses them. */
@Composable
internal fun TimeLine(time: String, half: String?, size: TextUnit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Hero(time, size, rolling = true, weight = FontWeight.Normal)
        if (half != null) {
            Spacer(Modifier.width((size.value * 0.08f).dp))
            Words(half, (size.value * 0.34f).coerceAtLeast(11f).sp, weight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = (size.value * 0.06f).dp))
        }
    }
}

/** The next alarm in a capsule: an orange alarm clock and when it goes off. */
@Composable
private fun AlarmChip(alarm: String) {
    Row(
        Modifier.well(50.dp).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Glyph(Icons.Rounded.Alarm, 14.dp, tint = hue(Hue.Orange))
        Spacer(Modifier.width(5.dp))
        Words(alarm, Type.meta, weight = FontWeight.SemiBold)
    }
}

/** The next alarm as a line: the alarm clock and when it goes off. */
@Composable
private fun AlarmLine(alarm: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Glyph(Icons.Rounded.Alarm, 14.dp, tint = hue(Hue.Orange))
        Spacer(Modifier.width(5.dp))
        Words(alarm, Type.meta, weight = FontWeight.SemiBold, color = soft())
    }
}

/** The greeting in a well across the foot of the widget, where the next alarm would be. */
@Composable
private fun GreetingWell(greeting: String) {
    Box(Modifier.fillMaxWidth().well().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Words(greeting, Type.meta, weight = FontWeight.Medium, color = soft(), maxLines = 2)
    }
}

/** The next alarm in a well across the widget: "Alarm" and when, over each other. */
@Composable
private fun AlarmWell(alarm: String) {
    Row(
        Modifier.fillMaxWidth().well().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Glyph(Icons.Rounded.Alarm, 18.dp, tint = hue(Hue.Orange))
        Spacer(Modifier.width(8.dp))
        Column {
            Label("Alarm")
            Words(alarm, Type.body, weight = FontWeight.SemiBold)
        }
    }
}
