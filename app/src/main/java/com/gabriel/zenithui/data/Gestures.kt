package com.gabriel.zenithui.data

/**
 * What each gesture on the home screen does. Only what you have changed is kept, so a gesture
 * nobody has touched does whatever Zenith does by default — and if that default ever changes, it
 * changes for them too. Swipe up stays the apps and holding an empty spot stays Customise, so
 * Settings can always be reached, whatever is chosen here.
 */
object Gestures {
    const val SWIPE_DOWN = "swipeDown"
    const val TWO_DOWN = "twoDown"
    const val TWO_UP = "twoUp"
    const val DOUBLE_TAP = "doubleTap"
    const val PINCH_IN = "pinchIn"
    const val PINCH_OUT = "pinchOut"
    const val HOME = "home"

    val ids = listOf(SWIPE_DOWN, TWO_DOWN, TWO_UP, DOUBLE_TAP, PINCH_IN, PINCH_OUT, HOME)

    fun label(id: String): String = when (id) {
        SWIPE_DOWN -> "Swipe down"
        TWO_DOWN -> "Two-finger swipe down"
        TWO_UP -> "Two-finger swipe up"
        DOUBLE_TAP -> "Double tap"
        PINCH_IN -> "Pinch in"
        PINCH_OUT -> "Pinch out"
        HOME -> "Home button, when already home"
        else -> id
    }

    fun default(id: String): String = when (id) {
        SWIPE_DOWN -> GestureActions.NOTIFICATIONS
        TWO_DOWN -> GestureActions.QUICK_SETTINGS
        PINCH_IN -> GestureActions.CUSTOMISE
        PINCH_OUT -> GestureActions.OVERVIEW
        HOME -> GestureActions.FIRST_PAGE
        else -> GestureActions.NONE
    }

    /** What [id] does: what you chose for it, or what it does by default. */
    fun actionFor(id: String, chosen: Map<String, String>): String = chosen[id] ?: default(id)

    /** [chosen] with [action] for [id]. Choosing a gesture's default is the same as not choosing. */
    fun with(chosen: Map<String, String>, id: String, action: String): Map<String, String> =
        if (action == default(id)) chosen - id else chosen + (id to action)
}

/** Everything a gesture can do. An app is `app:<key>`, a shortcut its own key. */
object GestureActions {
    const val NONE = "none"
    const val NOTIFICATIONS = "notifications"
    const val QUICK_SETTINGS = "quickSettings"
    const val LOCK = "lock"
    const val DRAWER = "drawer"
    const val SEARCH = "search"
    const val TORCH = "torch"
    const val AMBIENT = "ambient"
    const val CUSTOMISE = "customise"
    const val SETTINGS = "settings"
    const val WIDGETS = "widgets"
    const val FIRST_PAGE = "firstPage"
    const val OVERVIEW = "overview"

    private const val APP = "app:"

    /** The ones that are not an app, in the order they are offered. */
    val fixed = listOf(
        NONE, NOTIFICATIONS, QUICK_SETTINGS, LOCK, DRAWER, SEARCH, TORCH,
        AMBIENT, CUSTOMISE, SETTINGS, WIDGETS, FIRST_PAGE, OVERVIEW,
    )

    fun app(key: String): String = "$APP$key"

    /** The app [action] opens, by its key, if it opens one. */
    fun appKey(action: String): String? = action.takeIf { it.startsWith(APP) }?.removePrefix(APP)?.takeIf { '/' in it }

    fun isShortcut(action: String): Boolean = ShortcutKeys.isShortcut(action)

    /** What an action is called, with [name] for whatever app or shortcut it opens. */
    fun label(action: String, name: (String) -> String?): String = when (action) {
        NONE -> "Nothing"
        NOTIFICATIONS -> "Notifications"
        QUICK_SETTINGS -> "Quick settings"
        LOCK -> "Lock the phone"
        DRAWER -> "All apps"
        SEARCH -> "Search"
        TORCH -> "Torch"
        AMBIENT -> "Bedside clock"
        CUSTOMISE -> "Customise"
        SETTINGS -> "Settings"
        WIDGETS -> "Widgets"
        FIRST_PAGE -> "First page"
        OVERVIEW -> "Page overview"
        else -> appKey(action)?.let { "Open ${name(it) ?: "an app"}" }
            ?: if (isShortcut(action)) name(action) ?: "A shortcut" else action
    }

    /** One line under an action, saying what it does or what it needs. */
    fun detail(action: String): String? = when (action) {
        NOTIFICATIONS -> "Pulls down the notification shade"
        QUICK_SETTINGS -> "Pulls the quick settings all the way down"
        LOCK -> "Turns the screen off and locks, as the side button does. Needs Zenith's lock helper"
        SEARCH -> "All apps, with the keyboard up"
        TORCH -> "Turns the flashlight on or off"
        AMBIENT -> "A dim clock for the night"
        FIRST_PAGE -> "Back to the first page, and everything closed"
        OVERVIEW -> "Every page small, to reorder, add or tidy away"
        else -> null
    }

    /** Whether [action] can only work once Zenith's lock helper is switched on in Android's settings. */
    fun needsHelper(action: String): Boolean = action == LOCK
}
