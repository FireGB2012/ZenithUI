package com.gabriel.zenithui.data

import androidx.compose.runtime.Immutable
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** One thing on the To-do widget's list. */
@Immutable
data class TodoItem(val text: String, val done: Boolean = false) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("text", text)
        if (done) put("done", true)
    }

    companion object {
        fun fromJson(o: JSONObject): TodoItem? =
            o.optString("text").takeIf { it.isNotBlank() }?.let { TodoItem(it, o.optBoolean("done", false)) }
    }
}

/**
 * The To-do widget's rules. What is still to do comes first, in the order it was written down;
 * what is done drops below it, the most recently ticked at the top of that, so the list reads as
 * what is left, and a tick can be taken back from right where it went.
 */
object Todos {
    /** As many as the widget keeps. A list longer than this is a different kind of app. */
    const val MOST = 20

    fun add(list: List<TodoItem>, text: String): List<TodoItem> {
        val clean = text.trim()
        if (clean.isEmpty() || list.size >= MOST) return list
        val (open, done) = list.partition { !it.done }
        return open + TodoItem(clean) + done
    }

    /** The one at [index] ticked, or unticked, and moved to where that puts it. */
    fun toggle(list: List<TodoItem>, index: Int): List<TodoItem> {
        val item = list.getOrNull(index) ?: return list
        val rest = list.filterIndexed { i, _ -> i != index }
        val (open, done) = rest.partition { !it.done }
        return if (item.done) open + item.copy(done = false) + done else open + item.copy(done = true) + done
    }

    fun clearDone(list: List<TodoItem>): List<TodoItem> = list.filterNot { it.done }

    fun left(list: List<TodoItem>): Int = list.count { !it.done }
}

/** The World clock's cities: time zones, and how each is named and told apart from here. */
object WorldClocks {
    val DEFAULT = listOf("Europe/London", "America/New_York", "Asia/Tokyo")

    /** As many as it shows at its largest. */
    const val MOST = 4

    /** A zone's city as people say it: "America/New_York" is "New York". */
    fun city(zone: String): String = zone.substringAfterLast('/').replace('_', ' ')

    /** Its region, for telling two cities of one name apart in a list: "America". */
    fun region(zone: String): String = zone.substringBefore('/', "").replace('_', ' ')

    /**
     * How far [zone] is from [here] right now, as a person would say it: "Same time", "+5 h",
     * "−3 h 30", and a day word when the date there is not today's.
     */
    fun offset(zone: String, here: ZoneId, now: Instant): String {
        val there = runCatching { ZoneId.of(zone) }.getOrNull() ?: return ""
        val hereTime = ZonedDateTime.ofInstant(now, here)
        val thereTime = ZonedDateTime.ofInstant(now, there)
        val minutes = (thereTime.offset.totalSeconds - hereTime.offset.totalSeconds) / 60
        val days = ChronoUnit.DAYS.between(hereTime.toLocalDate(), thereTime.toLocalDate())
        val gap = when {
            minutes == 0 -> "Same time"
            minutes % 60 == 0 -> "${if (minutes > 0) "+" else "−"}${kotlin.math.abs(minutes) / 60} h"
            else -> "${if (minutes > 0) "+" else "−"}${kotlin.math.abs(minutes) / 60} h ${kotlin.math.abs(minutes) % 60}"
        }
        return when {
            days > 0 -> "$gap, tomorrow"
            days < 0 -> "$gap, yesterday"
            else -> gap
        }
    }

    /** Whether it is day in [zone] at [now]: between seven in the morning and seven at night. */
    fun daytime(zone: String, now: Instant): Boolean {
        val there = runCatching { ZoneId.of(zone) }.getOrNull() ?: return true
        return ZonedDateTime.ofInstant(now, there).hour in 7..18
    }

    /** Every zone there is, as a city, A to Z, for choosing from. */
    fun all(): List<String> = ZoneId.getAvailableZoneIds()
        .filter { '/' in it && !it.startsWith("Etc/") && !it.startsWith("SystemV/") && it.first().isUpperCase() }
        .sortedBy { city(it) }
}

/** The Countdown widget's sums, in whole days. */
object Countdown {
    /** What the widget says the count is at: days to go, today, or days since. */
    sealed interface Count {
        data class ToGo(val days: Long) : Count
        data object Today : Count
        data class Since(val days: Long) : Count
        data object NotSet : Count
    }

    fun count(day: Long, today: LocalDate): Count {
        if (day <= 0L) return Count.NotSet
        val left = day - today.toEpochDay()
        return when {
            left > 0 -> Count.ToGo(left)
            left == 0L -> Count.Today
            else -> Count.Since(-left)
        }
    }

    /**
     * How much of the wait is behind you, from 0 the day it was set to 1 on the day: what the
     * widget's ring is drawn round to. 1 once the day has come.
     */
    fun progress(from: Long, day: Long, today: LocalDate): Float {
        if (day <= 0L) return 0f
        // The day has come, however recently it was set.
        if (today.toEpochDay() >= day) return 1f
        val total = (day - from).coerceAtLeast(1)
        val gone = today.toEpochDay() - from
        return (gone.toFloat() / total).coerceIn(0f, 1f)
    }

    /** The count in words, for under its number. */
    fun words(count: Count, title: String): String {
        val what = title.ifBlank { "the day" }
        return when (count) {
            is Count.ToGo -> if (count.days == 1L) "day to $what" else "days to $what"
            Count.Today -> "$what is today"
            is Count.Since -> if (count.days == 1L) "day since $what" else "days since $what"
            Count.NotSet -> "Set a date from its menu"
        }
    }
}
