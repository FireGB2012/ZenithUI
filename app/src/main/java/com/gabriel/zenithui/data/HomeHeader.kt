package com.gabriel.zenithui.data

/** The line written at the top of the home screen. */
object HomeHeader {
    const val NAME = "name"
    const val GREETING = "greeting"
    const val OFF = "off"

    /** A large clock, with the date under it. */
    const val CLOCK = "clock"

    /** The day and the date, large. */
    const val DATE = "date"

    /** The date, and the weather when the Weather widget has a place. */
    const val GLANCE = "glance"

    /** Whether [style] is one that changes by itself, with the time: drawn live, not as a line of words. */
    fun isLive(style: String): Boolean = style == CLOCK || style == DATE || style == GLANCE

    /** The day and the date as people say them: "Saturday 31 October". */
    fun dateLine(date: java.time.LocalDate): String {
        val day = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
        val month = date.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
        return "$day ${date.dayOfMonth} $month"
    }

    /** The weather in a few words, for At a glance: "12° and cloudy". Null with no forecast. */
    fun weatherLine(temperature: Double?, words: String?): String? {
        if (temperature == null) return null
        val degrees = "${Math.round(temperature)}°"
        return if (words.isNullOrBlank()) degrees else "$degrees and ${words.replaceFirstChar { it.lowercase() }}"
    }

    /** The line for [style], or null when there is none. [hour] is the hour of the day, 0 to 23. */
    fun text(style: String, name: String, hour: Int): String? {
        val who = name.trim()
        return when (style) {
            OFF, CLOCK, DATE, GLANCE -> null
            GREETING -> {
                val hello = when (hour) {
                    in 5..11 -> "Good morning"
                    in 12..17 -> "Good afternoon"
                    in 18..22 -> "Good evening"
                    else -> "Good night"
                }
                if (who.isEmpty()) hello else "$hello, $who"
            }
            else -> who.ifEmpty { "Zenith" }
        }
    }
}
