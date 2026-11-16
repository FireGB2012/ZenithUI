package com.gabriel.zenithui.data

import android.os.BatteryManager
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The plain rules behind Zenith's widgets: what they say and how they work things out, kept apart
 * from how they are drawn so each can be tried without a screen.
 */

/** The clock's greeting: the part of the day, and the name you gave Zenith when there is one. */
object Greeting {
    fun of(hour: Int, name: String): String {
        val part = when (hour) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            in 18..21 -> "Good evening"
            else -> "Good night"
        }
        val who = name.trim()
        return if (who.isEmpty()) part else "$part, $who"
    }
}

/** Times as the phone writes them: in 24 hours or in 12, as it is set. */
object ClockWords {
    private val H24 = DateTimeFormatter.ofPattern("HH:mm")
    private val H12 = DateTimeFormatter.ofPattern("h:mm")

    /** The time, with no AM or PM: that is [half], set apart and smaller, as an iPhone sets it. */
    fun time(at: LocalDateTime, h24: Boolean): String = at.format(if (h24) H24 else H12)

    /** AM or PM in 12 hours; nothing in 24. */
    fun half(at: LocalDateTime, h24: Boolean): String? = if (h24) null else if (at.hour < 12) "AM" else "PM"

    /**
     * One of the forecast's hours, given as two figures from 00 to 23, labelled for the row of hours:
     * "17" in 24 hours, "5PM" in 12. Anything else is shown as it came.
     */
    fun hour(twoFigures: String, h24: Boolean): String {
        if (h24) return twoFigures
        val hour = twoFigures.toIntOrNull()?.takeIf { it in 0..23 } ?: return twoFigures
        val twelve = if (hour % 12 == 0) 12 else hour % 12
        return "$twelve${if (hour < 12) "AM" else "PM"}"
    }

    /**
     * When the next alarm goes off, for the clock: its time alone today, "Tomorrow" before it
     * tomorrow, and its weekday within the week. Null for one already past or more than six days off,
     * which is not worth a place on a clock.
     */
    fun alarm(
        alarmMillis: Long,
        nowMillis: Long,
        zone: ZoneId,
        h24: Boolean,
        locale: Locale = Locale.getDefault(),
    ): String? {
        if (alarmMillis <= nowMillis) return null
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val at = Instant.ofEpochMilli(alarmMillis).atZone(zone).toLocalDateTime()
        val days = ChronoUnit.DAYS.between(now, at.toLocalDate())
        if (days > 6) return null
        val clock = time(at, h24) + (half(at, h24)?.let { " $it" } ?: "")
        return when (days) {
            0L -> clock
            1L -> "Tomorrow $clock"
            else -> "${at.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)} $clock"
        }
    }

    /** Whether a clock showing [hour] wears its day face, pale, or its night face, dark. */
    fun dayFace(hour: Int): Boolean = hour in 6..17
}

/**
 * Temperatures as colours, for the bars under the forecast's days: blue in the frost, through
 * teal, green and yellow, to orange and red in the heat, as a weather map has them.
 */
object TemperatureScale {
    /** Where each colour stands, in degrees Celsius. */
    private val STOPS = listOf(
        -10.0 to 0xFF5E9BFF.toInt(),
        0.0 to 0xFF64D2FF.toInt(),
        10.0 to 0xFF5AD8B5.toInt(),
        18.0 to 0xFFA5E05A.toInt(),
        24.0 to 0xFFFFD60A.toInt(),
        30.0 to 0xFFFF9F0A.toInt(),
        38.0 to 0xFFFF453A.toInt(),
    )

    fun argbAt(celsius: Double): Int {
        val first = STOPS.first()
        val last = STOPS.last()
        if (celsius <= first.first) return first.second
        if (celsius >= last.first) return last.second
        val upper = STOPS.indexOfFirst { it.first >= celsius }
        val (fromAt, from) = STOPS[upper - 1]
        val (toAt, to) = STOPS[upper]
        return mix(from, to, ((celsius - fromAt) / (toAt - fromAt)).toFloat())
    }

    /** A temperature in the forecast's [units] ("c" or "f") in Celsius. */
    fun celsius(value: Double, units: String): Double = if (units == "f") (value - 32.0) * 5.0 / 9.0 else value

    /**
     * How far along a bar running from [coldest] to [warmest] a day from [low] to [high] is lit, as
     * fractions from its left end. A run of days all at one temperature lights the whole bar.
     */
    fun span(low: Double, high: Double, coldest: Double, warmest: Double): Pair<Float, Float> {
        val range = warmest - coldest
        if (range <= 0.0) return 0f to 1f
        val from = ((low - coldest) / range).toFloat().coerceIn(0f, 1f)
        val to = ((high - coldest) / range).toFloat().coerceIn(from, 1f)
        return from to to
    }

    private fun mix(a: Int, b: Int, t: Float): Int {
        fun channel(shift: Int) = (((a shr shift) and 0xFF) + (((b shr shift) and 0xFF) - ((a shr shift) and 0xFF)) * t).roundToInt()
        return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
}

/** The colours the battery wears, as an iPhone's does. */
enum class BatteryTone {
    /** Green: all is well. */
    Good,

    /** Yellow: battery saver is on. */
    Saver,

    /** Red: low, and not charging. */
    Low,
}

/** What the Power widget says about the battery. */
object BatteryFacts {
    /** At or under this, and not charging, the battery is low. */
    const val LOW = 20

    fun tone(level: Int, charging: Boolean, saver: Boolean): BatteryTone = when {
        saver -> BatteryTone.Saver
        !charging && level in 0..LOW -> BatteryTone.Low
        else -> BatteryTone.Good
    }

    /** [millis] as hours and minutes, "1 h 5 min"; never less than a minute, and nothing for none. */
    fun duration(millis: Long): String? {
        if (millis <= 0L) return null
        val minutes = ((millis + 59_999L) / 60_000L).coerceAtLeast(1L)
        val hours = minutes / 60
        val rest = minutes % 60
        return when {
            hours == 0L -> "$rest min"
            rest == 0L -> "$hours h"
            else -> "$hours h $rest min"
        }
    }

    /**
     * What the battery is doing, in one line. [toFull] is how long Android says it has to go before
     * it is charged, in milliseconds, and below 0 when it does not know. Some phones say 0 long before
     * they are full, so only the level says it is charged.
     */
    fun status(level: Int, charging: Boolean, saver: Boolean, toFull: Long): String = when {
        charging && level >= 100 -> "Charged"
        charging -> duration(toFull)?.let { "Full in $it" } ?: "Charging"
        saver -> "Battery saver on"
        level in 0..LOW -> "Low battery"
        else -> "On battery"
    }

    /** Where the power comes from, from [BatteryManager.EXTRA_PLUGGED]. */
    fun source(plugged: Int): String = when {
        plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS != 0 -> "Wireless"
        plugged and BatteryManager.BATTERY_PLUGGED_AC != 0 -> "Charger"
        plugged and BatteryManager.BATTERY_PLUGGED_USB != 0 -> "USB"
        // BATTERY_PLUGGED_DOCK, which Android only named in 13.
        plugged and 8 != 0 -> "Dock"
        else -> "Battery"
    }

    /** How the battery is, from [BatteryManager.EXTRA_HEALTH]. */
    fun health(health: Int): String = when (health) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Too hot"
        BatteryManager.BATTERY_HEALTH_COLD -> "Too cold"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Worn out"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
        else -> "Unknown"
    }

    /**
     * The battery's temperature, given in tenths of a degree Celsius, in the units the weather is
     * in. Null when Android did not say.
     */
    fun temperature(tenths: Int, units: String): String? {
        if (tenths == Int.MIN_VALUE) return null
        val celsius = tenths / 10.0
        return if (units == "f") "${(celsius * 9.0 / 5.0 + 32.0).roundToInt()}°F" else "${celsius.roundToInt()}°C"
    }

    /** The battery's voltage, given in millivolts, in volts to two places. Null when Android did not say. */
    fun voltage(millivolts: Int): String? {
        if (millivolts <= 0) return null
        val hundredths = (millivolts + 5) / 10
        return "${hundredths / 100}.${(hundredths % 100).toString().padStart(2, '0')} V"
    }
}

/** What the Calendar widget says about one of the day's events. */
object EventWords {
    private val H24 = DateTimeFormatter.ofPattern("HH:mm")
    private val H12 = DateTimeFormatter.ofPattern("h:mm")

    /**
     * When an event is, "All day" or from and to. In 12 hours AM or PM is said once when both ends
     * are in the same half of the day, and at each end when they are not.
     */
    fun span(startMillis: Long, endMillis: Long, allDay: Boolean, zone: ZoneId, h24: Boolean): String {
        if (allDay) return "All day"
        val from = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDateTime()
        val to = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDateTime()
        if (h24) return "${from.format(H24)}–${to.format(H24)}"
        val fromHalf = if (from.hour < 12) "AM" else "PM"
        val toHalf = if (to.hour < 12) "AM" else "PM"
        return if (fromHalf == toHalf) {
            "${from.format(H12)}–${to.format(H12)} $toHalf"
        } else {
            "${from.format(H12)} $fromHalf–${to.format(H12)} $toHalf"
        }
    }

    /**
     * How long until an event starts: "Now" once it has (or within the minute), "In 25 min",
     * "In 1 h 15 min" under three hours, and the hours alone beyond that. Null for one that is over.
     */
    fun until(startMillis: Long, endMillis: Long, nowMillis: Long): String? {
        if (endMillis <= nowMillis) return null
        val wait = startMillis - nowMillis
        if (wait < 60_000L) return "Now"
        val minutes = wait / 60_000L
        return if (minutes < 180L) "In ${BatteryFacts.duration(minutes * 60_000L)}" else "In ${minutes / 60} h"
    }

    /**
     * The ways to say when an event is, the fullest first, for a widget to use the first that fits its
     * room: when it is and how long until it starts; under way, till when; hours off, the time alone
     * says more than the wait, and soon, the wait says more than the time.
     */
    fun lines(startMillis: Long, endMillis: Long, allDay: Boolean, nowMillis: Long, zone: ZoneId, h24: Boolean): List<String> {
        if (allDay) return listOf("All day")
        val span = span(startMillis, endMillis, allDay = false, zone = zone, h24 = h24)
        val until = until(startMillis, endMillis, nowMillis) ?: return listOf(span)
        if (until == "Now") return listOf("Now · till ${at(endMillis, zone, h24)}", "Now")
        val start = at(startMillis, zone, h24)
        return if (startMillis - nowMillis < 3 * 3_600_000L) {
            listOf("$span · $until", "$start · $until", until)
        } else {
            listOf("$span · $until", span, start)
        }
    }

    /** A moment the way the phone tells the time: "17:30", or "5:30 PM". */
    fun at(millis: Long, zone: ZoneId, h24: Boolean): String {
        val time = Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime()
        return if (h24) time.format(H24) else "${time.format(H12)} ${if (time.hour < 12) "AM" else "PM"}"
    }
}

/** A city's name shortened for a dial or a narrow column: "LON", "TOK", and "NY" for New York. */
object CityCode {
    fun of(city: String): String {
        val words = city.trim().split(' ', '-').filter { it.isNotBlank() }
        return when {
            words.isEmpty() -> ""
            words.size == 1 -> words.first().take(3).uppercase()
            else -> words.take(3).joinToString("") { it.first().uppercase() }
        }
    }
}

/**
 * Where each of the next hours stands on the Weather widget's chart, from 0 at the foot for the
 * coldest to 1 at the top for the warmest; level through the middle when they are all one.
 */
object HourChart {
    fun levels(temperatures: List<Double>): List<Float> {
        if (temperatures.isEmpty()) return emptyList()
        val coldest = temperatures.min()
        val range = temperatures.max() - coldest
        if (range <= 0.0) return temperatures.map { 0.5f }
        return temperatures.map { ((it - coldest) / range).toFloat() }
    }
}

/** How far into a song, or how long it is: "1:35", and "1:02:03" for an hour or more. */
object TrackTime {
    fun of(millis: Long): String {
        val seconds = (millis.coerceAtLeast(0L) / 1_000L)
        val hours = seconds / 3_600L
        val minutes = (seconds % 3_600L) / 60L
        val rest = (seconds % 60L).toString().padStart(2, '0')
        return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:$rest" else "$minutes:$rest"
    }
}

/** The calendar's weeks and months, set out from the day the phone's week starts on. */
object CalendarFacts {
    /** The seven days of the week [day] is in. */
    fun week(day: LocalDate, firstDay: DayOfWeek): List<LocalDate> {
        val start = day.with(TemporalAdjusters.previousOrSame(firstDay))
        return List(7) { start.plusDays(it.toLong()) }
    }

    /** [month] in whole weeks of seven, a blank before its first day and after its last. */
    fun month(month: YearMonth, firstDay: DayOfWeek): List<List<LocalDate?>> {
        val first = month.atDay(1)
        val lead = ((first.dayOfWeek.value - firstDay.value) + 7) % 7
        val cells = List(lead) { null } + List(month.lengthOfMonth()) { month.atDay(it + 1) }
        val padded = cells + List((7 - cells.size % 7) % 7) { null }
        return padded.chunked(7)
    }
}

/**
 * The countdown's dots: the whole wait as a field of dots, the ones behind you filled. A dot a day
 * while that is few enough to count, a dot a week after that, and a dot a month for a wait of years.
 */
object CountdownDots {
    enum class Unit(val days: Int) { Day(1), Week(7), Month(30) }

    /** As many dots as the field is made of at most, unless a bigger one asks for more. */
    const val MOST = 60

    data class Plan(val unit: Unit, val total: Int, val gone: Int)

    /** The wait as dots: a dot a day, a week or a month, the shortest for which [most] are enough. */
    fun of(totalDays: Int, goneDays: Int, most: Int = MOST): Plan {
        val total = totalDays.coerceAtLeast(1)
        val unit = Unit.entries.firstOrNull { (total + it.days - 1) / it.days <= most } ?: Unit.Month
        val dots = ((total + unit.days - 1) / unit.days).coerceIn(1, most)
        val gone = (goneDays.coerceIn(0, total) / unit.days).coerceAtMost(dots)
        return Plan(unit, dots, gone)
    }
}

/** This week as the streak sees it: the days kept, today, and what is still ahead. */
object StreakWeek {
    enum class Day {
        /** Before today and not part of the streak. */
        Missed,

        /** Before today, and part of the streak. */
        Kept,

        /** Today, already counted. */
        Today,

        /** Today, not counted yet. */
        Open,

        /** Still to come. */
        Ahead,
    }

    fun of(streak: Int, today: LocalDate, firstDay: DayOfWeek): List<Day> =
        CalendarFacts.week(today, firstDay).map { day ->
            val behind = ChronoUnit.DAYS.between(day, today)
            when {
                behind < 0 -> Day.Ahead
                behind == 0L -> if (streak > 0) Day.Today else Day.Open
                behind < streak -> Day.Kept
                else -> Day.Missed
            }
        }
}
