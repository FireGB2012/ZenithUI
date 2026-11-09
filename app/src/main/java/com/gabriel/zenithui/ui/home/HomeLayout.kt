package com.gabriel.zenithui.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gabriel.zenithui.data.HomeGeometry

/**
 * The home screen as it is seen, from the top: Android's status bar, the page, the band Search
 * sits in, and the dock. Nothing else is on it. This only says where each part goes. What is in
 * each place, and everything that happens on it (the drawer, an app opening and closing, the
 * sheets, every gesture), belongs to the home screen's content, which hands each part in as a slot
 * and keeps the state behind them.
 *
 * The page is given whatever room the band and the dock leave, so neither may take its size from
 * the page: the page's rows are worked out from that room, and a band sized from the rows would
 * change the room again.
 */
@Composable
fun HomeLayout(
    /** The page and what is on it, given the modifier that fills the room left for it. */
    pager: @Composable (Modifier) -> Unit,
    /** The band between the page and the dock that Search sits in, always the same height. */
    searchBand: @Composable (Modifier) -> Unit,
    /** The dock, or, with no dock, the room the gesture bar needs. */
    dock: @Composable (Modifier) -> Unit,
    /**
     * What floats over the page, in a box exactly as big as the page's room and drawn over it: put
     * at the foot of the box, a thing stands just above the band, clear of Search and the dock.
     */
    overlays: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        // The room Android's status bar takes, and a little air under it.
        Spacer(Modifier.statusBarsPadding().height(TOP_AIR))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            pager(Modifier.fillMaxSize())
            overlays()
        }
        searchBand(Modifier.fillMaxWidth().height(HomeGeometry.SEARCH_BAND_DP.dp))
        dock(Modifier)
    }
}

/** Between the status bar and the top of the page, as there always was with nothing written there. */
private val TOP_AIR = 8.dp
