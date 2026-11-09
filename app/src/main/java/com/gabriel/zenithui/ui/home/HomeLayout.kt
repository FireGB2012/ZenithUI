package com.gabriel.zenithui.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The home screen as it is seen, from the top: the room Android's status bar takes, the page, what
 * stands between the page and the dock, and the dock. This only says where each part goes. What is
 * in each place, and everything that happens on it (the drawer, an app opening and closing, the
 * sheets, every gesture), belongs to the home screen's content, which hands each part in as a slot
 * and keeps the state behind them.
 *
 * The page is given whatever room the parts under it leave, so none of them may take its size from
 * the page: the page's rows are worked out from that room, and a part sized from the rows would
 * change the room again.
 */
@Composable
fun HomeLayout(
    /** The page and what is on it, given the modifier that fills the room left for it. */
    pager: @Composable (Modifier) -> Unit,
    /** What stands between the page and the dock. */
    searchBand: @Composable (Modifier) -> Unit,
    /** The dock, or, with no dock, the room the gesture bar needs. */
    dock: @Composable (Modifier) -> Unit,
    /**
     * What floats over the page, in a box exactly as big as the page's room and drawn over it: put
     * at the foot of the box, a thing stands just above whatever is under the page.
     */
    overlays: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        // The room Android's status bar takes.
        Spacer(Modifier.statusBarsPadding())
        Box(Modifier.weight(1f).fillMaxWidth()) {
            pager(Modifier.fillMaxSize())
            overlays()
        }
        searchBand(Modifier.fillMaxWidth())
        dock(Modifier)
    }
}
