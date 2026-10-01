package com.gabriel.zenithui.data

/**
 * Brings a home screen saved before everything kept to whole rows up to that rule. It runs once
 * when Zenith starts on an older layout, and whenever an older backup is put back.
 */
object LayoutTidy {

    /** From this layout version on, every item sits on a whole row and the search bar is a setting. */
    const val VERSION = 2

    /**
     * Everything a stored layout may need on its way to today's rules, in order: rescaled into row
     * units if it was saved before there were any, the fourfold rescale undone if it went through
     * that twice, and then [upgrade]. A layout already at [VERSION] comes back untouched. The
     * repair for the double rescale in particular must never see one: now that every row is a
     * whole one, it could no longer tell a healthy layout from a damaged one.
     */
    fun bringUpToDate(settings: ZenithSettings, items: List<HomeItem>): Pair<ZenithSettings, List<HomeItem>> {
        if (settings.layoutVersion >= VERSION) return settings to items
        val inUnits = if (settings.layoutVersion < 1) LayoutBuilder.upgradeToRowUnits(items) else items
        return upgrade(settings, LayoutBuilder.undoDoubleUpgrade(inUnits, settings.rows))
    }

    /**
     * [settings] and [items] as they are under today's rules. The search bar leaves the page and
     * becomes the bar above the dock, and nothing tucks under a widget any more. Unless free
     * placement is on, every item also moves to the nearest whole row and every widget becomes
     * whole rows tall, with whatever then overlaps settled the usual way: the higher item keeps
     * its place. With free placement on, offsets and part rows stay; only what now lies on top of
     * something else moves, by as little as it can — an icon that was tucked onto the last unit
     * of a widget would otherwise count as overlapping it, and nothing on that page could step
     * aside for a drop. A layout already at [VERSION] comes back untouched.
     */
    fun upgrade(settings: ZenithSettings, items: List<HomeItem>): Pair<ZenithSettings, List<HomeItem>> {
        if (settings.layoutVersion >= VERSION) return settings to items
        val rest = items.filterNot { it is WidgetCell && it.type == Widgets.Search.type }
        val next = settings.copy(
            searchBar = settings.searchBar || rest.size != items.size,
            widgetTuck = 0,
            layoutVersion = VERSION,
        )
        return next to LayoutBuilder.settle(rest, settings.columns, settings.rows, free = settings.freeform)
    }

    /**
     * The layout once Plus has ended on a phone where free placement was left switched on. Free
     * placement is no longer in effect there, so everything goes onto whole rows, exactly as it
     * does when the switch is turned off. A layout that never used it comes back as it is.
     */
    fun afterPlusEnded(settings: ZenithSettings, items: List<HomeItem>): List<HomeItem> =
        if (settings.freeform) LayoutBuilder.settle(items, settings.columns, settings.rows, free = false) else items
}
