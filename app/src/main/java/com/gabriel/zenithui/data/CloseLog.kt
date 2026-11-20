package com.gabriel.zenithui.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What the last close of an app was: which app, how Zenith knew it was that one, and how it went
 * back. Said in Settings, under Advanced animation settings' "Wait for Android's close", so a close
 * that looked wrong on a phone can be told apart from one that went the way it was meant to.
 */
object CloseLog {

    /** Where the app went. */
    enum class Where { Icon, Folder, Drawer, Library, PageBefore, PageAfter, Middle, Nowhere }

    data class Entry(
        /** The app's name, if Zenith has it. */
        val label: String?,
        /** How Zenith knew which app it was; null when it did not know. */
        val source: CloseSource?,
        val route: CloseRoute,
        val where: Where,
        /** Whether Android's own close was waited out first. */
        val waited: Boolean,
        /** Android was told where its icon is and never said its window got there: the icon landed instead. */
        val missed: Boolean = false,
        /** Its card came back from an app that came over Zenith without covering it. */
        val over: Boolean = false,
    )

    private val latest = MutableStateFlow<Entry?>(null)

    /** The last close. */
    val last: StateFlow<Entry?> = latest.asStateFlow()

    fun note(entry: Entry) {
        latest.value = entry
    }

    /** The last close, now with Android having missed the icon it was told. */
    fun missed() {
        latest.value = latest.value?.copy(missed = true)
    }

    /** Where an app went: into its [spot] (a folder's if [folder]), or with none [away], else into the middle. */
    fun where(spot: Boolean, folder: Boolean, away: CloseAway.Way?): Where = when {
        spot -> if (folder) Where.Folder else Where.Icon
        away == CloseAway.Way.Down -> Where.Drawer
        away == CloseAway.Way.Library -> Where.Library
        away == CloseAway.Way.PageBefore -> Where.PageBefore
        away == CloseAway.Way.PageAfter -> Where.PageAfter
        else -> Where.Middle
    }

    /** [entry] said in a sentence, for Settings: nothing before any app has closed. */
    fun describe(entry: Entry?): String {
        if (entry == null) return ""
        val app = entry.label?.takeIf { it.isNotBlank() } ?: "the app"
        if (entry.source == null) {
            return "The last app to close was not one Zenith opened, and Android did not say which it was, so the home screen came in on its own."
        }
        val into = when (entry.where) {
            Where.Icon -> "into its icon"
            Where.Folder -> "into its folder"
            Where.Drawer -> "down into the app drawer"
            Where.Library -> "off the side into the App Library"
            Where.PageBefore, Where.PageAfter -> "off the side towards its page"
            Where.Middle, Where.Nowhere -> "into the middle of the screen"
        }
        val how = when (entry.route) {
            CloseRoute.Handshake -> if (entry.missed) {
                "Android was told where its icon is but never said its window got there, so the icon landed"
            } else {
                "Android took its own window $into"
            }
            CloseRoute.Picture -> "a picture of it went $into"
            CloseRoute.Card -> if (entry.over) "its card went back $into" else "its card went $into"
            CloseRoute.Land -> "its icon landed"
            CloseRoute.None -> "nothing of Zenith's played (Closing apps is Plain)"
        }
        val known = when (entry.source) {
            CloseSource.Answered, CloseSource.Named -> "Android said which app it was"
            CloseSource.Seen -> "the app picture helper saw it go"
            CloseSource.Opened -> "it was opened from its icon"
            CloseSource.Remembered -> "Zenith opened it"
        }
        val wait = if (entry.waited) ", after Android's own close" else ""
        return "The last close: $app, $how$wait. ${known.replaceFirstChar { it.uppercase() }}."
    }
}
