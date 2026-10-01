package com.gabriel.zenithui.data

/**
 * Brings a home screen saved before everything kept to whole rows up to that rule. It runs once
 * when Zenith starts on an older layout, and whenever an older backup is put back.
 */
object LayoutTidy {

    /** From this layout version on, every item sits on a whole row and the search bar is a setting. */
    const val VERSION = 2

    /**
     * [settings] and [items] as they are under today's rules. The search bar leaves the page and
     * becomes the bar above the dock, and nothing tucks under a widget any more. Unless free
     * placement is on, every item also moves to the nearest whole row and every widget becomes
     * whole rows tall, with whatever then overlaps settled the usual way: the higher item keeps
     * its place. A layout already at [VERSION] comes back untouched.
     */
    fun upgrade(settings: ZenithSettings, items: List<HomeItem>): Pair<ZenithSettings, List<HomeItem>> {
        if (settings.layoutVersion >= VERSION) return settings to items
        val rest = items.filterNot { it is WidgetCell && it.type == Widgets.Search.type }
        val next = settings.copy(
            searchBar = settings.searchBar || rest.size != items.size,
            widgetTuck = 0,
            layoutVersion = VERSION,
        )
        val placed = if (settings.freeform) {
            rest
        } else {
            LayoutBuilder.settle(rest, settings.columns, settings.rows, free = false)
        }
        return next to placed
    }
}
