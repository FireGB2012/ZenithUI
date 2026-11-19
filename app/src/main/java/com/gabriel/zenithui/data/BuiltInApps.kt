package com.gabriel.zenithui.data

/**
 * Apps that come inside Zenith instead of being installed on the phone. Zenith Connect is the
 * first. To the home screen, the dock, the drawer, the library and Search each is an app like any
 * other: a key, a name and an icon, opened out of its icon and closed back into it. But it is a
 * page of Zenith's own, not an Android activity, so nothing outside Zenith ever sees it: not the
 * phone's own list of apps, not its Settings, not another launcher.
 *
 * Its key is written the way every app's is, "package/class", so everything that keeps apps (a
 * cell on a page, the dock, a Look, a backup) keeps it with no change. The package, [PACKAGE], is
 * Zenith's own name for its apps, not one Android lists.
 */
object BuiltInApps {

    /** The package every built-in app is under. */
    const val PACKAGE = "zenith.builtin"

    /** Zenith Connect: setups made by others, the community, the beta, and your Zenith ID. */
    const val CONNECT = "$PACKAGE/connect"

    /** Every built-in app, as the app list holds it. It came with Zenith, so it counts as the phone's own. */
    val all: List<AppInfo> = listOf(
        AppInfo(packageName = PACKAGE, className = "connect", label = "Connect", system = true),
    )

    fun isBuiltIn(key: String?): Boolean = key != null && key.startsWith("$PACKAGE/")

    fun app(key: String): AppInfo? = all.firstOrNull { it.key == key }

    /** [installed], the phone's apps, with Zenith's own among them, in order of name as the phone's are. */
    fun joinedWith(installed: List<AppInfo>): List<AppInfo> =
        (installed.filterNot { isBuiltIn(it.key) } + all).sortedBy { it.label.lowercase() }

    /**
     * Whether a built-in app goes onto the home screen now: once, the first time Zenith has it, and
     * never again once it has been, even if it was taken off since. One already on the home screen
     * (put there by a Look, a backup or by hand) counts as put there.
     */
    fun needsPlacing(key: String, placed: Set<String>, onHome: Boolean): Boolean = key !in placed && !onHome
}
