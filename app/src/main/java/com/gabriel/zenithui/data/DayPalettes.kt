package com.gabriel.zenithui.data

import java.time.LocalDateTime

/**
 * A palette for each part of the day (Zenith Plus): dawn, day, dusk and night, each from the
 * hour it begins. As a part begins its palette comes on, with the theme's own fade; one chosen by
 * hand in the middle of a part stays until the next part begins.
 */
object DayParts {

    /** A part of the day and the minute of the day it begins. */
    data class Part(val id: String, val label: String, val start: Int)

    val all = listOf(
        Part("dawn", "Dawn", 5 * 60),
        Part("day", "Day", 8 * 60),
        Part("dusk", "Dusk", 17 * 60),
        Part("night", "Night", 20 * 60),
    )

    /** The part of the day [minute] is in. Before dawn it is still night. */
    fun at(minute: Int): Part = all.lastOrNull { minute >= it.start } ?: all.last()

    /** Until when a part lasts, as a clock says it: Dawn "until 08:00". */
    fun until(part: Part): Int = all[(all.indexOf(part) + 1) % all.size].start

    /**
     * The palette [chosen] for the part of the day [now] is in, and what to remember the change by;
     * null when that part has no palette, or its change has been made already ([last]).
     */
    fun due(chosen: Map<String, String>, now: LocalDateTime, last: String): Pair<String, String>? {
        val minute = now.hour * 60 + now.minute
        val part = at(minute)
        val palette = chosen[part.id]?.takeIf { it.isNotEmpty() } ?: return null
        // A night that began yesterday evening is yesterday's, all the way to dawn.
        val began = if (minute < part.start) now.toLocalDate().minusDays(1) else now.toLocalDate()
        val occasion = "${part.id}@${began.toEpochDay()}"
        return if (occasion == last) null else palette to occasion
    }

    fun toJson(chosen: Map<String, String>): org.json.JSONObject =
        org.json.JSONObject().apply { chosen.forEach { (part, palette) -> if (all.any { it.id == part }) put(part, palette) } }

    fun fromJson(o: org.json.JSONObject?): Map<String, String> {
        if (o == null) return emptyMap()
        return all.mapNotNull { part -> o.optString(part.id).takeIf { it.isNotEmpty() }?.let { part.id to it } }.toMap()
    }
}
