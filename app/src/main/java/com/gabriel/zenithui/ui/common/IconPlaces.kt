package com.gabriel.zenithui.ui.common

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.toSize
import com.gabriel.zenithui.data.CloseRules
import com.gabriel.zenithui.data.ShortcutKeys

/**
 * Where each icon on the home screen sits while the home screen is at rest: the page on show, and
 * the dock. An app being closed goes back into one of them, so this has to be known at once when
 * Android asks ([com.gabriel.zenithui.data.GestureHandshake]), while the home screen is still
 * sunk back behind the app and every icon is drawn somewhere else.
 *
 * So a place is only taken down while the home screen is at rest ([atRest], which the home screen
 * sets), and the moment it comes to rest every place is taken down again ([cameToRest]). Kept
 * outside composition, like [LaunchOrigin], and used from the main thread only.
 */
object IconPlaces {

    /** One icon: what it opens, where it is at rest, whether it is in the dock or a folder holding them. */
    data class Spot(
        val keys: List<String>,
        val bounds: Rect,
        val dock: Boolean = false,
        val folder: Boolean = false,
    ) {
        /** The key of the app this one is, or the first of the folder's. */
        val key: String get() = keys.firstOrNull().orEmpty()
    }

    private class Seen(var keys: List<String>, var dock: Boolean, var folder: Boolean) {
        var coordinates: LayoutCoordinates? = null
        var rest: Rect = Rect.Zero
    }

    private val seen = LinkedHashMap<Any, Seen>()
    private var screen = Size.Zero

    /**
     * Whether the home screen is at rest right now, so a place taken now is where an icon stays.
     * Asked each time an icon moves, so that it is never a frame late. Set by the home screen.
     */
    var atRest: () -> Boolean = { false }

    /** The home screen has come to rest: every place is taken down again. */
    fun cameToRest() {
        if (atRest()) seen.values.forEach(::takeDown)
    }

    internal fun saw(token: Any, keys: List<String>, dock: Boolean, folder: Boolean, coordinates: LayoutCoordinates) {
        val place = seen.getOrPut(token) { Seen(keys, dock, folder) }
        place.keys = keys
        place.dock = dock
        place.folder = folder
        place.coordinates = coordinates
        if (atRest()) takeDown(place)
    }

    internal fun forget(token: Any) {
        seen.remove(token)
    }

    private fun takeDown(place: Seen) {
        val coordinates = place.coordinates?.takeIf { it.isAttached } ?: return
        place.rest = coordinates.boundsInRoot()
        screen = coordinates.findRootCoordinates().size.toSize()
    }

    /** Every icon on show, as it was last at rest. */
    fun spots(): List<Spot> = seen.values.map { Spot(it.keys, it.rest, it.dock, it.folder) }

    /** The icon the app [pkg] goes back into (see [choose]). */
    fun find(key: String?, pkg: String, work: Boolean, near: Rect?): Spot? = choose(spots(), key, pkg, work, near, screen)

    /**
     * Which of [spots] the app [pkg] goes back into: an icon of [key], the one opened from, the
     * one nearest [near] (where it was touched) if there are two; failing that an icon of the same
     * app, on the page before the dock, as Android's own launcher does; failing that a folder
     * holding it. Only an icon wholly on show counts. [work] says which profile the app is in.
     */
    fun choose(spots: List<Spot>, key: String?, pkg: String, work: Boolean, near: Rect?, screen: Size): Spot? {
        val shown = spots.filter { it.bounds.width > 0f && it.bounds.height > 0f && it.onScreen(screen) }
        fun sameApp(k: String) = !ShortcutKeys.isShortcut(k) && CloseRules.isWork(k) == work && CloseRules.packageOf(k) == pkg
        val icons = shown.filter { !it.folder }
        if (key != null) {
            val exact = icons.filter { key in it.keys }
            if (exact.isNotEmpty()) {
                return if (near != null) exact.minByOrNull { (it.bounds.center - near.center).getDistanceSquared() } else exact.pageFirst()
            }
        }
        icons.filter { spot -> spot.keys.any(::sameApp) }.pageFirst()?.let { return it }
        return shown.firstOrNull { spot -> spot.folder && spot.keys.any { it == key || sameApp(it) } }
    }

    private fun List<Spot>.pageFirst(): Spot? = firstOrNull { !it.dock } ?: firstOrNull()

    private fun Spot.onScreen(screen: Size): Boolean {
        if (screen.width <= 0f || screen.height <= 0f) return true
        return bounds.left >= -1f && bounds.top >= -1f && bounds.right <= screen.width + 1f && bounds.bottom <= screen.height + 1f
    }
}

/**
 * True only round the home screen's own pages and dock. The same icons are drawn in the small home
 * screens of Settings and the page overview, and in the copy under a finger, and an app must never
 * be sent into one of those.
 */
val LocalHomePlaces = staticCompositionLocalOf { false }

/** Takes down this app icon's place on the home screen, for an app closing back into it (see [IconPlaces]). */
fun Modifier.homePlace(key: String, dock: Boolean = false): Modifier = homePlace(listOf(key), dock = dock, folder = false)

/** Takes down this icon's place: one app's, or with [folder] a folder holding [keys]. */
fun Modifier.homePlace(keys: List<String>, dock: Boolean = false, folder: Boolean): Modifier = composed {
    val token = remember { Any() }
    val current = rememberUpdatedState(keys)
    val counts = LocalHomePlaces.current
    DisposableEffect(token) { onDispose { IconPlaces.forget(token) } }
    if (counts) onGloballyPositioned { IconPlaces.saw(token, current.value, dock, folder, it) } else Modifier
}
