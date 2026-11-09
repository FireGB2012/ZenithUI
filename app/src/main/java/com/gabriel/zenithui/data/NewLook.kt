package com.gabriel.zenithui.data

/**
 * The new look, the iPhone home screen, as one rule: what switching to it changes, when a phone
 * that already had Zenith is asked about it, and whether a home screen is on it already.
 *
 * A new install starts on it. A phone that had Zenith before is asked once, and either answer is
 * final; either way every one of these settings can be changed again in Customise and Settings.
 */
object NewLook {

    /** The iPhone grid: four across, with rows spaced like the columns. See [GridDensity.Four]. */
    private val GRID = GridDensity.Four.id

    /** The iPhone's rounded square: see [IconShapes]. */
    private const val ICON_SHAPE = "iphone"

    /** Widgets solid and colourful, like iOS's own. */
    private const val WIDGETS = WidgetLooks.COLOUR

    /** The floating slab of glass. */
    private const val DOCK = "glass"

    /**
     * [s] on the new look: the iPhone grid, the iPhone's icon shape, colour widgets and the glass
     * dock, shown; and the question counts as put. Nothing else changes. The grid's columns and
     * rows are worked out from the screen as for any grid, and the home screen is then settled
     * into them, so apps and widgets keep their order though some may move to another page.
     */
    fun apply(s: ZenithSettings): ZenithSettings = s.copy(
        gridDensity = GRID,
        iconShape = ICON_SHAPE,
        widgetLook = WIDGETS,
        dockStyle = DOCK,
        showDock = true,
        newLookAsked = true,
    )

    /**
     * Whether to ask about the new look now: once, on a phone that was set up before there was
     * one. A new install is set up on it and never asked, and a backup from before it, restored,
     * has not been asked.
     */
    fun due(s: ZenithSettings): Boolean = s.onboarded && !s.newLookAsked

    /**
     * Whether the home screen is on the new look: its grid, icon shape, widgets and dock as the new
     * look has them. Whether the dock is shown is a switch of its own and not part of the look, so
     * hiding it does not count as leaving.
     */
    fun isOn(s: ZenithSettings): Boolean =
        s.gridDensity == GRID && s.iconShape == ICON_SHAPE && s.widgetLook == WIDGETS && s.dockStyle == DOCK
}
