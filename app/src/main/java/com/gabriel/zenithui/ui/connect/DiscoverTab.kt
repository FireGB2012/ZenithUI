package com.gabriel.zenithui.ui.connect

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.connect.ConnectCodes
import com.gabriel.zenithui.data.connect.ConnectFeed
import com.gabriel.zenithui.data.connect.ConnectLayers
import com.gabriel.zenithui.data.connect.ConnectPicks
import com.gabriel.zenithui.data.connect.ConnectServer
import com.gabriel.zenithui.data.connect.ConnectSetup
import com.gabriel.zenithui.data.connect.ConnectStudio
import com.gabriel.zenithui.data.connect.Filter
import com.gabriel.zenithui.data.connect.ServerState
import com.gabriel.zenithui.data.connect.SetupSource
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.shimmer
import com.gabriel.zenithui.ui.theme.GlassMaterial
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.glass
import java.time.LocalDate

/**
 * Discover: the setup of the week, Zenith's picks, what is featured on Zenith's server, the Looks
 * opened by their codes and your own, every card a picture of your own home screen wearing it.
 * Searching (or a filter) shows everything that matches in a grid instead, and a code typed into
 * the search opens the Look it belongs to.
 */
@Composable
internal fun DiscoverTab(state: ConnectState, bridge: ConnectBridge) {
    val focus = LocalFocusManager.current
    val mine = remember(bridge.looks) { bridge.looks.map { ConnectSetup.of(it, SetupSource.MINE) } }
    val everything = remember(state.featured, state.opened, mine) {
        (ConnectPicks.all + state.featured + state.opened + mine).distinctBy { it.id }
    }
    val searching = state.query.isNotBlank() || state.filter != Filter.ALL
    val found = remember(everything, state.query, state.filter) { ConnectFeed.list(everything, state.query, state.filter) }
    val typedCode = remember(state.query) { ConnectCodes.clean(state.query) }
    val week = remember { ConnectFeed.ofTheWeek(ConnectPicks.all, LocalDate.now()) }
    val aspect = phoneAspect(bridge)

    LazyColumn(state = state.discoverList, contentPadding = tabPadding(), modifier = Modifier.fillMaxSize()) {
        item(key = "title") {
            TabTitle("Discover") { AccountBubble(state, bridge) }
        }
        item(key = "server") { ServerLine(state, bridge) }
        item(key = "search") {
            ConnectField(
                value = state.query,
                onChange = { state.query = it },
                hint = "Search setups, or paste a code",
                capitals = KeyboardCapitalization.None,
                onAction = {
                    if (typedCode != null && found.none { it.code == typedCode }) state.lookUp(bridge, state.query)
                    focus.clearFocus()
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item(key = "filters") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Filter.entries.forEach { filter ->
                    Chip(
                        filter.label,
                        on = state.filter == filter,
                        onClick = { state.filter = if (state.filter == filter) Filter.ALL else filter },
                    )
                }
            }
        }

        if (searching) {
            val (title, note) = ConnectFeed.heading(state.query, state.filter, found.size)
            item(key = "found-head") {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp), verticalAlignment = Alignment.Bottom) {
                    SectionHead(title, note = note, modifier = Modifier.weight(1f))
                    Text(
                        "Clear",
                        color = LocalPalette.current.accent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(end = 20.dp)
                            .tap("Clear the search") {
                                state.query = ""
                                state.filter = Filter.ALL
                                focus.clearFocus()
                            },
                    )
                }
            }
            if (typedCode != null && found.none { it.code == typedCode }) {
                item(key = "code-hit") {
                    InfoCard(
                        icon = Icons.Rounded.VpnKey,
                        title = "LOOK-$typedCode",
                        text = state.problem ?: if (state.busy) "Asking Zenith's server for it…" else "See it on your own home screen before you put anything on.",
                        action = if (state.busy) "Looking…" else "Open this Look",
                        tinted = true,
                        onAction = { state.lookUp(bridge, state.query) },
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }
            if (found.isEmpty() && typedCode == null) {
                item(key = "nothing") {
                    InfoCard(
                        icon = Icons.Rounded.SearchOff,
                        title = "Nothing like that yet",
                        text = "Try a colour, a theme or a wallpaper: ocean, frost, sunset, glass icons.",
                    )
                }
            } else {
                items(found.chunked(2), key = { row -> "row-" + row.first().id }) { row ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { setup -> SetupCard(setup, state, bridge, aspect, Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        } else {
            if (week != null) item(key = "week") { WeekCard(week, state, bridge, aspect) }
            item(key = "picks-head") { Head("Zenith Picks", "free, made for this Zenith") }
            item(key = "picks") { SetupRow(ConnectPicks.all, state, bridge, aspect) }
            item(key = "featured-head") {
                Head("Featured", if (state.server == ServerState.UP) "from Zenith's server" else null)
            }
            item(key = "featured") { FeaturedRow(state, bridge, aspect) }
            if (state.opened.isNotEmpty()) {
                item(key = "opened-head") { Head("Opened by code", null) }
                item(key = "opened") { SetupRow(state.opened, state, bridge, aspect) }
            }
            if (mine.isNotEmpty()) {
                item(key = "mine-head") { Head("Your Looks", if (mine.size == 1) "1 saved" else "${mine.size} saved") }
                item(key = "mine") { SetupRow(mine, state, bridge, aspect) }
            }
            item(key = "code") {
                InfoCard(
                    icon = Icons.Rounded.VpnKey,
                    title = "Got a code?",
                    text = "Somebody shared their setup? Type its code and see it on your own home screen before you put it on.",
                    action = "Type a code",
                    onAction = { state.openSheet(ConnectSheet.CODE) },
                    modifier = Modifier.padding(top = 26.dp),
                )
            }
        }
    }
}

/** A section's heading, with room above it. */
@Composable
private fun Head(title: String, note: String?) {
    SectionHead(title, note = note, modifier = Modifier.padding(top = 26.dp, bottom = 12.dp))
}

/** Your account's letter in a round pane, or a figure with none: to the Share tab, where the account is. */
@Composable
private fun AccountBubble(state: ConnectState, bridge: ConnectBridge) {
    val palette = LocalPalette.current
    val account = bridge.account
    Box(
        Modifier
            .size(40.dp)
            .pressBounce(0.88f)
            .glass(CircleShape, material = GlassMaterial.Clear, interactive = true)
            .tap(if (account == null) "Sign in" else "Your account") { state.goTab(ConnectTab.SHARE) },
        contentAlignment = Alignment.Center,
    ) {
        if (account == null) {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = palette.onGlass, modifier = Modifier.size(22.dp))
        } else {
            Text(ConnectStudio.initial(account), color = palette.onGlass, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** How Zenith's server stands, under the title: a dot and a few words, and Try again when it is out of reach. */
@Composable
private fun ServerLine(state: ConnectState, bridge: ConnectBridge) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    val asking = state.server == ServerState.ASKING
    val pulse = if (asking && !reduced) {
        val beat = rememberInfiniteTransition(label = "serverPulse")
        beat.animateFloat(1f, 0.3f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "serverDot").value
    } else {
        1f
    }
    val down = state.server == ServerState.DOWN
    Row(
        Modifier
            .padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
            .then(if (down) Modifier.tap("Try again") { state.load(bridge, force = true) } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Dot(serverColour(state.server), Modifier.graphicsLayer { alpha = pulse })
        Spacer(Modifier.width(8.dp))
        Text(ConnectServer.pill(state.server), color = palette.onGlassMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        if (down) {
            Text(" · Try again", color = palette.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** A row of setups that scrolls sideways. */
@Composable
private fun SetupRow(setups: List<ConnectSetup>, state: ConnectState, bridge: ConnectBridge, aspect: Float) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(setups, key = { it.id }) { setup -> SetupCard(setup, state, bridge, aspect, Modifier.width(CARD_WIDTH)) }
    }
}

/** What the Featured row shows: the Looks, while they come, or why there are none. */
@Composable
private fun FeaturedRow(state: ConnectState, bridge: ConnectBridge, aspect: Float) {
    when {
        state.featured.isNotEmpty() -> SetupRow(state.featured, state, bridge, aspect)
        state.server == ServerState.NONE -> InfoCard(
            icon = Icons.Rounded.CloudOff,
            title = "No server in this build",
            text = "Zenith's picks and your own Looks are all here. Shared ones need a build of Zenith with its server.",
        )
        state.server == ServerState.DOWN -> InfoCard(
            icon = Icons.Rounded.CloudOff,
            title = "Can't reach Zenith's server",
            text = "Check you're online, then try again. Zenith's picks and your own Looks work without it.",
            action = "Try again",
            actionIcon = Icons.Rounded.Refresh,
            onAction = { state.load(bridge, force = true) },
        )
        state.featuredLoading || state.server == ServerState.ASKING -> LoadingRow(aspect)
        else -> InfoCard(
            icon = Icons.Rounded.Star,
            title = "Nothing featured yet",
            text = "Looks people share turn up here once they're featured. Got a code? Tap the key at the bottom.",
        )
    }
}

/** Three cards still coming, shimmering where their phones will be. */
@Composable
private fun LoadingRow(aspect: Float) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState(), enabled = false)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(3) {
            Column(Modifier.width(CARD_WIDTH).pane(CARD_SHAPE).padding(7.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspect)
                        .clip(RoundedCornerShape(15.dp))
                        .background(palette.onGlass.copy(alpha = 0.08f))
                        .shimmer(),
                )
                Spacer(Modifier.height(10.dp))
                Box(Modifier.padding(start = 6.dp).width(84.dp).height(12.dp).clip(CircleShape).background(palette.onGlass.copy(alpha = 0.12f)))
                Spacer(Modifier.height(7.dp))
                Box(Modifier.padding(start = 6.dp, bottom = 8.dp).width(56.dp).height(10.dp).clip(CircleShape).background(palette.onGlass.copy(alpha = 0.08f)))
            }
        }
    }
}

/**
 * A setup as a card: your home screen wearing it (as much of it as its page starts with on), its
 * name, and who it is by. A tap opens its page.
 */
@Composable
internal fun SetupCard(setup: ConnectSetup, state: ConnectState, bridge: ConnectBridge, aspect: Float, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val on = remember(setup) { ConnectLayers.startingOn(setup) }
    val worn = rememberWorn(setup, on, bridge)
    val open = { state.open(setup, bridge) }
    Column(
        modifier
            .pressBounce(0.96f)
            .pane(CARD_SHAPE, interactive = true)
            .tap("Open ${setup.name}", open)
            .padding(7.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(aspect)) {
            SetupPhone(worn, bridge, Modifier.fillMaxSize(), artWidth = 240)
            // The picture takes no touch of its own: the card has it.
            Box(Modifier.matchParentSize().tap("Open ${setup.name}", open))
        }
        Spacer(Modifier.height(9.dp))
        Text(
            setup.name,
            color = palette.onGlass,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        Text(
            byLine(setup),
            color = palette.onGlassMuted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 1.dp, bottom = 6.dp),
        )
    }
}

/** Who a setup is by and what it is, in a few words: "Zenith · Aurora", "Shared · LOOK-BCD234". */
internal fun byLine(setup: ConnectSetup): String = when (setup.source) {
    SetupSource.CODE -> "Shared · ${setup.shownCode}"
    else -> listOfNotNull(setup.by, setup.tags.firstOrNull()).joinToString(" · ")
}

/** The setup of the week, large: the phone beside its name and what it is, and Try it on. */
@Composable
private fun WeekCard(setup: ConnectSetup, state: ConnectState, bridge: ConnectBridge, aspect: Float) {
    val palette = LocalPalette.current
    val on = remember(setup) { ConnectLayers.startingOn(setup) }
    val worn = rememberWorn(setup, on, bridge)
    val open = { state.open(setup, bridge) }
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp)
            .fillMaxWidth()
            .pressBounce(0.98f)
            .pane(CARD, interactive = true)
            .tap("Open ${setup.name}", open)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(WEEK_PHONE).aspectRatio(aspect)) {
            SetupPhone(worn, bridge, Modifier.fillMaxSize(), artWidth = 280)
            Box(Modifier.matchParentSize().tap("Open ${setup.name}", open))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("SETUP OF THE WEEK", color = palette.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(5.dp))
            Text(setup.name, color = palette.onGlass, fontSize = 25.sp, fontWeight = FontWeight.Bold, lineHeight = 29.sp, letterSpacing = (-0.4).sp)
            Spacer(Modifier.height(6.dp))
            Text(setup.about, color = palette.onGlassMuted, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(14.dp))
            ConnectButton("Try it on", onClick = open, tinted = true, height = 40.dp)
        }
    }
}

/** A card in a row, and its corners: the phone's own corners and the room round it, so the two curves run together. */
internal val CARD_WIDTH: Dp = 136.dp
internal val CARD_SHAPE = RoundedCornerShape(22.dp)
private val WEEK_PHONE: Dp = 120.dp
