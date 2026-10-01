package com.gabriel.zenithui.data

/** The line written at the top of the home screen. */
object HomeHeader {
    const val NAME = "name"
    const val GREETING = "greeting"
    const val OFF = "off"

    /** The line for [style], or null when there is none. [hour] is the hour of the day, 0 to 23. */
    fun text(style: String, name: String, hour: Int): String? {
        val who = name.trim()
        return when (style) {
            OFF -> null
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
