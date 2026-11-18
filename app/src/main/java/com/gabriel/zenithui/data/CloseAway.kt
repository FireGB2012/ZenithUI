package com.gabriel.zenithui.data

import androidx.compose.ui.geometry.Rect
import kotlin.math.abs

/**
 * Where an app goes as it closes when there is no icon of it on show to go into: off the screen,
 * towards where it lives (see `specs/2026-11-18-closing-off-to-where-it-lives-design.md`). Down into
 * the app drawer, which comes up from below; off to the side into the App Library, which comes in
 * from past the last page; or off to the side towards the page its icon is on. Before, it shrank
 * into the middle of the screen and faded, as iOS's does, which looked like it was going nowhere.
 *
 * The card goes there the way it goes into an icon (CloseRules.intoIcon, Shrinking time), towards
 * an icon-sized square just past the edge, so the edge it goes to is the one it keeps to all the
 * way: its foot never comes up off the bottom of the screen going down, and its far side never
 * comes in off the edge going to the side. It is gone as it gets there, with nothing to fade.
 */
object CloseAway {

    enum class Way {
        /** Into the app drawer, which comes up from below, or a dock that is not on show: down off the foot of the screen. */
        Down,

        /** Into the App Library, past the last page: off the right of the screen. */
        Library,

        /** To a page before the one on show, where its icon is: off the left of the screen. */
        PageBefore,

        /** To a page after the one on show, where its icon is: off the right of the screen. */
        PageAfter;

        /**
         * Towards one of the home screen's own pages. Coming home stays on the page on show for it
         * (see [staysOnPage]), as it does for an app going into an icon on show, since the way it
         * goes says where its page is from this one.
         */
        val towardsPage: Boolean get() = this == PageBefore || this == PageAfter
    }

    /**
     * Which way the app [key] ([pkg], in the work profile if [work]) goes, with no icon of it on
     * show to go into. [items] is every page of the home screen, [dock] the dock's apps, [shownPage]
     * the page on show, and [library] whether the apps are an App Library rather than a drawer.
     *
     * Its icon on another page first, the nearest of them (the earlier of two as near), then a
     * folder holding it, the way an app goes into its icon (IconPlaces.choose): towards that page.
     * Failing that, a dock that is not on show, below. Failing that it is only among the apps:
     * into the drawer, below, or the library, to the side. Null when it lives on the page on show,
     * where its icon has no place known to go into: into the middle, as before.
     */
    fun way(
        key: String,
        pkg: String,
        work: Boolean,
        items: List<HomeItem>,
        dock: List<String>,
        shownPage: Int,
        library: Boolean,
    ): Way? {
        fun sameApp(k: String) = !ShortcutKeys.isShortcut(k) && CloseRules.isWork(k) == work && CloseRules.packageOf(k) == pkg
        fun ownIcon(item: HomeItem): Boolean = when (item) {
            is AppCell -> item.key == key || sameApp(item.key)
            is ShortcutCell -> item.key == key
            else -> false
        }
        fun inFolder(item: HomeItem): Boolean = item is FolderCell && item.keys.any { it == key || sameApp(it) }
        val nearest = compareBy<HomeItem>({ abs(it.page - shownPage) }, { it.page })
        val home = items.filter(::ownIcon).minWithOrNull(nearest) ?: items.filter(::inFolder).minWithOrNull(nearest)
        return when {
            home == null && dock.any { it == key || sameApp(it) } -> Way.Down
            home == null -> if (library) Way.Library else Way.Down
            home.page < shownPage -> Way.PageBefore
            home.page > shownPage -> Way.PageAfter
            else -> null
        }
    }

    /**
     * Where a card on its way [way] goes on a [width] by [height] screen: a square [side] across
     * just past the edge, in the middle of the foot going down, and half way down going to the side.
     */
    fun target(way: Way, width: Float, height: Float, side: Float): Rect = when (way) {
        Way.Down -> Rect(width / 2f - side / 2f, height, width / 2f + side / 2f, height + side)
        Way.Library, Way.PageAfter -> Rect(width, height / 2f - side / 2f, width + side, height / 2f + side / 2f)
        Way.PageBefore -> Rect(-side, height / 2f - side / 2f, 0f, height / 2f + side / 2f)
    }

    /**
     * Whether coming home from an app stays on the page on show, rather than going back to the
     * first page: when the app goes into an icon on it ([spot]), or off towards the page its icon is
     * on, so the way it goes is the way to that page. Not when nothing of Zenith's plays ([route]).
     */
    fun staysOnPage(spot: Boolean, way: Way?, route: CloseRoute): Boolean =
        route != CloseRoute.None && (spot || way?.towardsPage == true)
}
