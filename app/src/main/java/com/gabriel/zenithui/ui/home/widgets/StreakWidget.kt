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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.AuraFlame
import com.gabriel.zenithui.data.StreakWeek
import com.gabriel.zenithui.data.WidgetForm
import com.gabriel.zenithui.ui.home.LocalBare
import com.gabriel.zenithui.ui.home.LocalWidgetWorld
import com.gabriel.zenithui.ui.home.rememberToday
import com.gabriel.zenithui.ui.theme.glassInk
import com.gabriel.zenithui.ui.theme.inkOnTint
import java.text.NumberFormat
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Streak: the days in a row you have come back, under a flame, this week as seven days with the ones
 * the streak covers ticked in the flame's colour (orange, until a level brings another) and today
 * ringed until it counts, and your aura.
 *
 * - Mini: the flame and the streak.
 * - Strip: the flame and the streak; wider, your aura and this week as dots too.
 * - Square: the flame, the streak large, and this week.
 * - Wide: the streak on the left; this week and your aura on the right.
 * - Tall: the flame, the streak, this week and your aura, down the widget.
 * - Big: the same, larger, with this week's days named.
 */
@Composable
internal fun StreakWidget(form: WidgetForm, width: Dp, height: Dp) {
    val settings = LocalWidgetWorld.current?.settings
    val streak = settings?.streak ?: 0
    val aura = remember(settings?.aura) { NumberFormat.getIntegerInstance().format(settings?.aura ?: 0) }
    val today = rememberToday()
    val firstDay = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }
    val week = remember(streak, today, firstDay) { StreakWeek.of(streak, today, firstDay) }
    val letters = remember(today, firstDay) {
        com.gabriel.zenithui.data.CalendarFacts.week(today, firstDay).map { it.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()) }
    }
    // The flame a level brought, if one is worn; the week's marks wear its colour too.
    val flame = remember(settings?.auraFlame, settings?.auraEarned, settings?.aura) {
        settings?.let { AuraFlame.worn(it) } ?: AuraFlame.Orange
    }
    val orange = hue(flame.markDark, flame.markLight)
    val days = if (streak == 1) "day in a row" else "days in a row"
    val spoken = "$streak $days, $aura aura"

    Fitted(form, width, height) { w, _ ->
        val root = Modifier.fillMaxSize().says(spoken)
        when (form) {
            WidgetForm.Mini -> Column(root.padding(INSET_MINI), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                FlameGlyph(18.dp, flame = flame)
                Hero("$streak", 20.sp, weight = FontWeight.Medium, rolling = true)
            }

            WidgetForm.Strip -> Row(root.padding(horizontal = INSET_STRIP), verticalAlignment = Alignment.CenterVertically) {
                FlameGlyph(28.dp, flame = flame)
                Spacer(Modifier.width(8.dp))
                Hero("$streak", 28.sp, rolling = true)
                Spacer(Modifier.width(10.dp))
                if (w >= 250.dp) {
                    Column(Modifier.weight(1f)) {
                        Words("day streak", Type.body, weight = FontWeight.SemiBold)
                        Words("$aura aura", Type.meta, color = soft())
                    }
                    WeekDots(week, orange, dot = 9.dp)
                } else {
                    Words(if (streak == 1) "day" else "days", Type.body, weight = FontWeight.SemiBold, color = soft(), modifier = Modifier.weight(1f))
                }
            }

            WidgetForm.Square -> Column(root.padding(INSET)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FlameGlyph(20.dp, flame = flame)
                    Spacer(Modifier.width(5.dp))
                    Label("Streak", color = wordHue(orange))
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Hero("$streak", 46.sp, weight = FontWeight.Normal, rolling = true)
                    Spacer(Modifier.width(6.dp))
                    Words("days", Type.body, weight = FontWeight.Medium, color = soft(), modifier = Modifier.padding(bottom = 5.dp))
                }
                Spacer(Modifier.weight(1f))
                WeekRow(week, letters, orange, Modifier.fillMaxWidth(), mark = 16.dp)
            }

            WidgetForm.Wide -> Row(root.padding(INSET), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(112.dp)) {
                    FlameGlyph(34.dp, flame = flame)
                    Hero("$streak", 48.sp, weight = FontWeight.Normal, rolling = true)
                    Words(days, Type.meta, weight = FontWeight.Medium, color = soft())
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    Label("This week")
                    WeekRow(week, letters, orange, Modifier.fillMaxWidth(), mark = 22.dp)
                    AuraLine(aura)
                }
            }

            WidgetForm.Tall -> Column(root.padding(INSET), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(6.dp))
                FlameGlyph(52.dp, flame = flame)
                Spacer(Modifier.height(6.dp))
                Hero("$streak", 56.sp, weight = FontWeight.Normal, rolling = true)
                Words(days, Type.body, weight = FontWeight.Medium, color = soft())
                Spacer(Modifier.weight(1f))
                WeekRow(week, letters, orange, Modifier.fillMaxWidth(), mark = 16.dp)
                Spacer(Modifier.height(14.dp))
                AuraLine(aura)
            }

            WidgetForm.Big -> Column(root.padding(INSET)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Label("Streak", color = wordHue(orange), modifier = Modifier.weight(1f))
                    Words("$aura aura", Type.meta, weight = FontWeight.SemiBold, color = soft())
                }
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FlameGlyph(84.dp, flame = flame)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Hero("$streak", 72.sp, weight = FontWeight.Normal, rolling = true)
                            Words(days, Type.headline, weight = FontWeight.Medium, color = soft())
                        }
                    }
                }
                Column(Modifier.fillMaxWidth().well().padding(horizontal = 12.dp, vertical = 12.dp)) {
                    Label("This week")
                    Spacer(Modifier.height(10.dp))
                    WeekRow(week, letters, orange, Modifier.fillMaxWidth(), mark = 28.dp)
                }
            }
        }
    }
}

/** Your aura in a well, beside a little sparkle of orange. */
@Composable
private fun AuraLine(aura: String) {
    Row(Modifier.fillMaxWidth().well().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Label("Aura", modifier = Modifier.weight(1f))
        Words(aura, Type.body, weight = FontWeight.SemiBold, tabular = true)
    }
}

/** This week as its days' letters over their marks, spread across the room. */
@Composable
private fun WeekRow(week: List<StreakWeek.Day>, letters: List<String>, colour: Color, modifier: Modifier, mark: Dp) {
    Row(modifier, horizontalArrangement = Arrangement.SpaceBetween) {
        week.forEachIndexed { i, day ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Label(letters.getOrElse(i) { "" }, color = if (day == StreakWeek.Day.Today || day == StreakWeek.Day.Open) ink() else soft())
                Spacer(Modifier.height(5.dp))
                DayMark(day, colour, mark)
            }
        }
    }
}

/** This week as seven dots in a row, no letters: for a strip. */
@Composable
private fun WeekDots(week: List<StreakWeek.Day>, colour: Color, dot: Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        week.forEach { day -> DayMark(day, colour, dot) }
    }
}

/**
 * One day of the week: a tick on an orange disc for a day the streak covers, today on its disc once it
 * counts and ringed until it does, a faint disc for a day missed and a faint ring for one to come.
 */
@Composable
private fun DayMark(day: StreakWeek.Day, colour: Color, size: Dp) {
    val bare = LocalBare.current
    val faint = faint(0.22f)
    val check = if (bare) Color.Black.copy(alpha = 0.55f) else inkOnTint(colour)
    Canvas(Modifier.size(size).then(if (bare) Modifier.glassInk(bevel = size * 0.12f) else Modifier)) {
        val r = this.size.minDimension / 2f
        val lit = if (bare) Color.White else colour
        when (day) {
            StreakWeek.Day.Kept, StreakWeek.Day.Today -> {
                drawCircle(lit, r)
                if (r >= 6.dp.toPx()) {
                    val a = Offset(this.size.width * 0.3f, this.size.height * 0.52f)
                    val b = Offset(this.size.width * 0.45f, this.size.height * 0.66f)
                    val c = Offset(this.size.width * 0.71f, this.size.height * 0.38f)
                    val stroke = (r * 0.2f).coerceAtLeast(1.4.dp.toPx())
                    drawLine(check, a, b, stroke, StrokeCap.Round)
                    drawLine(check, b, c, stroke, StrokeCap.Round)
                }
            }
            StreakWeek.Day.Open -> drawCircle(lit, r - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
            StreakWeek.Day.Missed -> drawCircle(if (bare) Color.White.copy(alpha = 0.3f) else faint, r)
            StreakWeek.Day.Ahead -> drawCircle(if (bare) Color.White.copy(alpha = 0.3f) else faint, r - 0.75.dp.toPx(), style = Stroke(1.5.dp.toPx()))
        }
    }
}
