package com.gabriel.zenithui.data

/**
 * Deciding whether the copy Zenith kept of a wallpaper it set is still the wallpaper on screen.
 *
 * Android numbers every wallpaper it is given, hands the number back to whoever set it, and will
 * tell anyone the number of the one on screen. That is the whole test: the number noted when the
 * copy was made, against the number now. It needs nothing announced and nothing timed, so it
 * holds on phones that never announce a new wallpaper's colours to the app that set it (Samsung's
 * do not), and across any number of restarts.
 *
 * The colours are the older test, kept for copies made before numbers were noted and for a phone
 * that will not say the number.
 */
internal object WallpaperTrust {

    /**
     * Whether the kept copy is the wallpaper on screen. [justSet] is true for the moments in
     * which Android is still taking a picture Zenith has just given it; [storedId] and
     * [storedColours] are what was noted when the copy was made (0 and null when nothing was);
     * [currentId] is the wallpaper's number now, or not above 0 when Android will not say.
     * [currentColours] is only asked for when the numbers cannot decide.
     */
    fun isCurrent(
        justSet: Boolean,
        storedId: Int,
        currentId: Int,
        storedColours: String?,
        currentColours: () -> String?,
    ): Boolean = when {
        justSet -> true
        storedId > 0 && currentId > 0 -> storedId == currentId
        else -> storedColours != null && storedColours == currentColours()
    }

    /**
     * The number of the wallpaper a call to set one has made, or 0 if it made none. [returned] is
     * what the call handed back, and [before] and [after] the number of the wallpaper on screen
     * either side of it. Android can take the call and leave the wallpaper as it was: then
     * nothing is handed back and the number has not moved.
     */
    fun idAfterSet(before: Int, returned: Int, after: Int): Int = when {
        returned > 0 -> returned
        after > 0 && after != before -> after
        else -> 0
    }
}
