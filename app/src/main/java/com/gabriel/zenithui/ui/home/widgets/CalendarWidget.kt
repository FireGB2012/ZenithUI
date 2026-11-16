package com.gabriel.zenithui.ui.home.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.gabriel.zenithui.data.CalendarEvent
import com.gabriel.zenithui.data.CalendarFacts
import com.gabriel.zenithui.data.CalendarRepository
import com.gabriel.zenithui.data.EventWords
import com.gabriel.zenithui.data.WidgetForm
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.home.AsManyAsFit
import com.gabriel.zenithui.ui.home.LocalBare
import com.gabriel.zenithui.ui.home.LocalWidgetWorld
import com.gabriel.zenithui.ui.home.rememberToday
import com.gabriel.zenithui.ui.theme.glassInk
import com.gabriel.zenithui.ui.theme.inkOnTint
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * The Calendar. The day in capitals over the date, as a calendar page has it, red on a card (on the
 * wallpaper's glass the red is today's disc), and the day's events from the next one on, each a card
 * with a bar in its calendar's colour, its name, when it is and how long until it starts. The wider
 * ones set the date beside the week; the tall ones over the month. Until Zenith may read the
 * calendar, "Show your events" stands where the events would be.
 *
 * - Mini: the day over the date.
 * - Strip: the date, and the next event beside it.
 * - Square: the day, the date, and the next event in a card.
 * - Wide: the date on the left; the week and the next events on the right.
 * - Tall: the date over the month, and the next events under it.
 * - Big: the date across the top; the month and the day's events side by side.
 */
@Composable
internal fun CalendarWidget(form: WidgetForm, width: Dp, height: Dp) {
    val today = rememberToday()
    val day = rememberDay(today)
    val weekday = remember(today) { today.format(DateTimeFormatter.ofPattern("EEEE")) }
    val shortDay = remember(today) { today.format(DateTimeFormatter.ofPattern("EEE")) }
    val month = remember(today) { today.format(DateTimeFormatter.ofPattern("MMMM")) }
    val monthYear = remember(today) { today.format(DateTimeFormatter.ofPattern("MMMM yyyy")) }
    val date = "${today.dayOfMonth}"
    val firstDay = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }
    val red = wordHue(hue(Hue.Red))
    val spoken = "$weekday ${today.dayOfMonth} $month" + (day.next?.firstOrNull()?.let { ", next ${it.title}" } ?: "")

    Fitted(form, width, height) { w, _ ->
        val root = Modifier.fillMaxSize().says(spoken)
        when (form) {
            WidgetForm.Mini -> Column(
                root.padding(INSET_MINI),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Label(shortDay, color = red)
                Hero(date, 28.sp, weight = FontWeight.Normal)
            }

            WidgetForm.Strip -> Row(root.padding(horizontal = INSET_STRIP), verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Label(shortDay, color = red)
                    Hero(date, 22.sp, weight = FontWeight.Normal)
                }
                Spacer(Modifier.width(12.dp))
                Box(Modifier.width(1.dp).height(30.dp).background(faint(0.22f)))
                Spacer(Modifier.width(12.dp))
                Events(day, most = 1, card = false, modifier = Modifier.weight(1f), more = false)
            }

            WidgetForm.Square -> Column(root.padding(INSET)) {
                Label(weekday, color = red)
                Hero(date, 40.sp, weight = FontWeight.Normal)
                Spacer(Modifier.weight(1f))
                Events(day, most = 1, card = true, modifier = Modifier.fillMaxWidth(), more = false, titleLines = 1)
            }

            WidgetForm.Wide -> Row(root.padding(INSET)) {
                Column(Modifier.width(92.dp).fillMaxHeight()) {
                    Label(weekday, color = red)
                    Hero(date, 46.sp, weight = FontWeight.Normal)
                    Spacer(Modifier.weight(1f))
                    Words(month, Type.body, weight = FontWeight.SemiBold, color = soft())
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    WeekStrip(today, firstDay)
                    Spacer(Modifier.height(10.dp))
                    Events(day, most = 2, card = false, modifier = Modifier.weight(1f).fillMaxWidth(), more = false)
                }
            }

            WidgetForm.Tall -> Column(root.padding(INSET)) {
                Label(weekday, color = red)
                Row(verticalAlignment = Alignment.Bottom) {
                    Hero(date, 40.sp, weight = FontWeight.Normal)
                    Spacer(Modifier.width(8.dp))
                    Words(month, Type.body, weight = FontWeight.SemiBold, color = soft(), modifier = Modifier.padding(bottom = 4.dp))
                }
                Spacer(Modifier.height(12.dp))
                MonthGrid(today, firstDay, Modifier.fillMaxWidth(), number = 11.sp, cell = 19.dp)
                Spacer(Modifier.height(12.dp))
                Hairline()
                Spacer(Modifier.height(10.dp))
                Events(day, most = 3, card = false, modifier = Modifier.weight(1f).fillMaxWidth())
            }

            WidgetForm.Big -> Column(root.padding(INSET)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Label(weekday, color = red)
                        Hero(date, 44.sp, weight = FontWeight.Normal)
                    }
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 4.dp)) {
                        Words(monthYear, Type.headline, weight = FontWeight.SemiBold)
                        val count = day.events?.size
                        if (day.allowed && count != null) {
                            Words(if (count == 1) "1 event today" else if (count == 0) "Nothing today" else "$count events today", Type.meta, color = soft())
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.weight(1f)) {
                    MonthGrid(today, firstDay, Modifier.width(if (w >= 330.dp) 160.dp else 150.dp).fillMaxHeight(), number = 12.sp, cell = 22.dp, roomy = true)
                    Spacer(Modifier.width(16.dp))
                    Events(day, most = Int.MAX_VALUE, card = true, modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

/**
 * What the calendar has to say about today: whether Zenith may read it, today's events once they are
 * read (null until then, so it never says there are none before it has looked), what is still to come
 * of them, and the clock they were last looked at by.
 */
private class Day(val allowed: Boolean, val events: List<CalendarEvent>?, val next: List<CalendarEvent>?, val now: Long, val ask: (() -> Unit)?)

@Composable
private fun rememberDay(today: LocalDate): Day {
    val context = LocalContext.current
    val world = LocalWidgetWorld.current
    // Looked at again whenever the home screen comes back: the answer to Android's question, or the
    // calendar itself, may have changed.
    var back by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { back++ }
    val allowed = remember(back) { CalendarRepository.hasPermission(context) }
    val events by produceState<List<CalendarEvent>?>(null, allowed, today, back) {
        value = if (allowed) CalendarRepository.today(context) else emptyList()
    }
    // What is listed changes as an event starts or ends, and "In 5 min" every minute, so the clock is
    // read again each minute while there is something still to come.
    val now by produceState(System.currentTimeMillis(), events, back) {
        while (true) {
            val at = System.currentTimeMillis()
            value = at
            if (events.orEmpty().none { !it.allDay && it.endMillis > at }) break
            delay(60_000L - at % 60_000L + 50L)
        }
    }
    val next = remember(events, now) { events?.let { CalendarRepository.upcoming(it, now, most = it.size) } }
    return Day(allowed, events, next, now, world?.let { { it.setUp("calendar") } })
}

/**
 * The events still to come, as many as there is room for and no more than [most]: as cards, or as
 * lines with a bar, and with [more], a line saying how many more there are. In their place, why there
 * are none, or the way to let Zenith read them.
 */
@Composable
private fun Events(day: Day, most: Int, card: Boolean, modifier: Modifier, more: Boolean = true, titleLines: Int = if (card) 2 else 1) {
    val h24 = rememberH24()
    val next = day.next
    when {
        !day.allowed -> ShowEvents(day.ask, modifier)
        next == null -> Box(modifier)
        next.isEmpty() -> Box(modifier, contentAlignment = Alignment.CenterStart) {
            Words(if (day.events.isNullOrEmpty()) "No events today" else "No more events today", Type.body, weight = FontWeight.Medium, color = soft())
        }
        else -> AsManyAsFit(
            next,
            modifier,
            gap = if (card) 6.dp else 9.dp,
            most = most,
            more = if (!more) null else { left ->
                Words(if (left == 1) "1 more event" else "$left more events", Type.meta, weight = FontWeight.Medium, color = soft())
            },
        ) { event -> EventRow(event, day.now, h24, card, titleLines) }
    }
}

/**
 * One event: a bar in its calendar's colour, its name on up to [titleLines], and when it is with how
 * long until it starts, as much of that as fits; stronger while it is under way, and red where red
 * can be read. As a [card] it sits in a well of its own.
 */
@Composable
private fun EventRow(event: CalendarEvent, now: Long, h24: Boolean, card: Boolean, titleLines: Int) {
    val zone = remember { ZoneId.systemDefault() }
    val lines = remember(event, h24, now) { EventWords.lines(event.startMillis, event.endMillis, event.allDay, now, zone, h24) }
    val under = !event.allDay && EventWords.until(event.startMillis, event.endMillis, now) == "Now"
    val bar = if (event.colour != 0) Color(event.colour).copy(alpha = 1f) else hue(Hue.Red)
    val bare = LocalBare.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .then(if (card) Modifier.well().padding(horizontal = 9.dp, vertical = 8.dp) else Modifier),
    ) {
        Box(
            Modifier
                .width(3.5.dp)
                .fillMaxHeight()
                .then(if (bare) Modifier.glassInk(bevel = 1.5.dp) else Modifier)
                .background(if (bare) Color.White else bar, RoundedCornerShape(50)),
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Words(event.title, Type.body, weight = FontWeight.SemiBold, maxLines = titleLines)
            FirstThatFits(
                lines,
                Type.meta,
                weight = if (under) FontWeight.SemiBold else FontWeight.Medium,
                color = if (under) wordHue(hue(Hue.Red)) else soft(),
                tabular = true,
            )
        }
    }
}

/** Where the events would be, until Zenith may read the calendar: a tap asks for it. */
@Composable
private fun ShowEvents(ask: (() -> Unit)?, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.CenterStart) {
        Row(
            Modifier
                .then(
                    if (ask != null) {
                        Modifier
                            .pressBounce(0.94f)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = ask)
                    } else {
                        Modifier
                    }
                )
                .well(50.dp)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Glyph(Icons.Rounded.CalendarMonth, 15.dp, tint = hue(Hue.Red))
            Spacer(Modifier.width(6.dp))
            Words("Show your events", Type.meta, weight = FontWeight.SemiBold)
        }
    }
}

/** This week, a day to a column: its letter over its date, today's date on a red disc. */
@Composable
private fun WeekStrip(today: LocalDate, firstDay: DayOfWeek) {
    val week = remember(today, firstDay) { CalendarFacts.week(today, firstDay) }
    Row(Modifier.fillMaxWidth()) {
        week.forEach { day ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Label(day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()))
                Spacer(Modifier.height(4.dp))
                DayNumber(day.dayOfMonth, today = day == today, past = day.isBefore(today), size = 24.dp, number = 12.sp)
            }
        }
    }
}

/**
 * The month in whole weeks, from the phone's first day of the week: the days' letters over it, the
 * days gone by fainter, and today on a red disc.
 */
@Composable
private fun MonthGrid(today: LocalDate, firstDay: DayOfWeek, modifier: Modifier, number: TextUnit, cell: Dp, roomy: Boolean = false) {
    val weeks = remember(today, firstDay) { CalendarFacts.month(YearMonth.from(today), firstDay) }
    val letters = remember(today, firstDay) { CalendarFacts.week(today, firstDay).map { it.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()) } }
    BoxWithConstraints(modifier) {
        // Given the room, the weeks stand a little apart, as on a page of a calendar, and fill more
        // of the height beside the events; never so far apart that they stop reading as a month.
        val gap = if (roomy && constraints.hasBoundedHeight) {
            ((maxHeight - LETTERS - cell * weeks.size) / weeks.size).coerceIn(1.dp, 14.dp)
        } else {
            1.dp
        }
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().height(LETTERS)) {
                letters.forEach { letter ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) { Label(letter) }
                }
            }
            weeks.forEachIndexed { i, week ->
                if (i > 0) Spacer(Modifier.height(gap))
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (day != null) DayNumber(day.dayOfMonth, today = day == today, past = day.isBefore(today), size = cell, number = number)
                            else Spacer(Modifier.size(cell))
                        }
                    }
                }
            }
        }
    }
}

/** The height of the row of the days' letters over a month, with the room under it. */
private val LETTERS = 18.dp

/** A day's number in a circle of [size]: on a red disc, in white, for today; fainter for a day gone by. */
@Composable
private fun DayNumber(dayOfMonth: Int, today: Boolean, past: Boolean, size: Dp, number: TextUnit) {
    val bare = LocalBare.current
    val red = hue(Hue.Red)
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        if (today) {
            Box(
                Modifier
                    .size(size)
                    .then(if (bare) Modifier.glassInk(bevel = size * 0.18f) else Modifier)
                    .background(if (bare) Color.White else red, RoundedCornerShape(50)),
            )
        }
        Words(
            "$dayOfMonth",
            number,
            weight = if (today) FontWeight.Bold else FontWeight.Medium,
            color = when {
                today && bare -> Color.White
                today -> inkOnTint(red)
                past -> ink().copy(alpha = 0.45f)
                else -> ink()
            },
            align = TextAlign.Center,
            tabular = true,
        )
    }
}
