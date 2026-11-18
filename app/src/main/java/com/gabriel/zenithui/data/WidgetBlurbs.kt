package com.gabriel.zenithui.data

/**
 * What each widget is for, in a few words, said under its name on its card in the widget gallery,
 * so the gallery can be read before anything in it is tapped. Testers asked for the gallery's words
 * to be easier to read; a card had only its name and how many sizes it comes in. The showcase a
 * card opens says more ([WidgetSpec.description]), and what each size shows ([WidgetLayouts]).
 */
object WidgetBlurbs {

    /** The longest a blurb may be, in letters: two lines on a card half the page wide. */
    const val LONGEST = 30

    /** [type] may be a "No glass" one, which is for the same thing. Null for one without a card. */
    fun of(type: String): String? = when (Widgets.baseType(type)) {
        "clock" -> "Time, date and a greeting"
        "battery" -> "Your battery's charge, live"
        "calendar" -> "Today and your next events"
        "notes" -> "A note that stays at hand"
        "toggles" -> "Torch, Wi-Fi and more"
        "media" -> "What's playing, with controls"
        "world" -> "The time in other cities"
        "countdown" -> "The days to a date you pick"
        "photo" -> "Your photos, one at a time"
        "todo" -> "A short list to tick off"
        "aura" -> "Your streak and your aura"
        "weather" -> "Now and the hours ahead"
        else -> null
    }
}
