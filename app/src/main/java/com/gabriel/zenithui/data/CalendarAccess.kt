package com.gabriel.zenithui.data

/**
 * Where the bedside clock stands with the calendar, and so what it says about it. Kept apart from
 * the screen so the rule can be tested: Android itself only ever answers "allowed" or "not".
 */
enum class CalendarAccess {
    /** Allowed: today's events are listed. */
    Granted,

    /** Not allowed, and free to ask: say what it is for and what is read, and offer it. */
    Offer,

    /** "Not now" was chosen: one quiet line, and no more asking until it is tapped. */
    Declined,

    /**
     * Refused in a way that stops Android showing its question again. Only the phone's own
     * settings can allow it now, so that is where the button goes.
     */
    Blocked;

    companion object {
        /**
         * [granted] is what Android says now. [declined] is "Not now", remembered from an earlier
         * visit. [refused] is whether Android's question has been put on this visit and came back
         * refused, and [canAskAgain] whether it would still show the question after that.
         */
        fun of(granted: Boolean, declined: Boolean, refused: Boolean, canAskAgain: Boolean): CalendarAccess = when {
            granted -> Granted
            refused && !canAskAgain -> Blocked
            declined -> Declined
            else -> Offer
        }
    }
}
