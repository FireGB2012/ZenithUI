package com.gabriel.zenithui.ui.connect

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.connect.ConnectFeed
import com.gabriel.zenithui.data.connect.ConnectSetup
import com.gabriel.zenithui.data.connect.FeedTab
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.theme.LocalLite
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.glassOnGlass
import kotlinx.coroutines.delay
import java.time.LocalDate

/**
 * Discover: the setup of the week on its big card, and every other setup in two columns that
 * fall like a masonry wall, re-sorting as you switch between This week, Trending, Top rated and New.
 */
@Composable
internal fun DiscoverTab(state: ConnectState, handle: String) {
    val list = ConnectFeed.list(state.setups, state.feedTab, state.query)
    val hero = ConnectFeed.heroShown(state.feedTab, state.query)
    val spotlight = state.setups.firstOrNull { it.id == ConnectFeed.SPOTLIGHT }
    val (heading, note) = ConnectFeed.heading(state.feedTab, state.query, list.size, LocalDate.now())
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        state = state.discoverGrid,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pageTop(), bottom = pageBottom()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp,
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "head", span = StaggeredGridItemSpan.FullLine) {
            DiscoverHead(state, handle)
        }
        item(key = "feeds", span = StaggeredGridItemSpan.FullLine) {
            PillPicker(
                options = FeedTab.entries,
                selected = state.feedTab,
                label = { it.label },
                modifier = Modifier.bleed(16.dp),
            ) { state.feedTab = it }
        }
        if (hero && spotlight != null) {
            item(key = "hero", span = StaggeredGridItemSpan.FullLine) {
                Hero(spotlight, Modifier.animateItem()) { from -> state.openSetup(spotlight, from) }
            }
        }
        item(key = "heading", span = StaggeredGridItemSpan.FullLine) {
            SectionHead(heading, Modifier.padding(top = 4.dp), inset = 0.dp) { HeadNote(note) }
        }
        items(list, key = { it.id }) { setup ->
            SetupCard(
                setup = setup,
                tab = state.feedTab,
                fresh = state.fresh == setup.id,
                modifier = Modifier.animateItem(
                    fadeInSpec = tween(260),
                    placementSpec = spring(dampingRatio = 0.86f, stiffness = 320f, visibilityThreshold = androidx.compose.ui.unit.IntOffset(1, 1)),
                    fadeOutSpec = tween(160),
                ),
            ) { from -> state.openSetup(setup, from) }
        }
        if (list.isEmpty()) {
            item(key = "empty", span = StaggeredGridItemSpan.FullLine) {
                Text(
                    "Nothing matches “${state.query.trim()}”. Try a creator or a vibe like minimal.",
                    color = LocalPalette.current.onGlassMuted,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
                )
            }
        }
    }
}

/** Lets what is laid out in a padded list reach [by] past it on either side, to the screen's edges. */
internal fun Modifier.bleed(by: Dp): Modifier = layout { measurable, constraints ->
    val extra = by.roundToPx() * 2
    val wide = constraints.copy(
        minWidth = (constraints.minWidth + extra).coerceAtLeast(0),
        maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + extra else constraints.maxWidth,
    )
    val placeable = measurable.measure(wide)
    layout((placeable.width - extra).coerceAtLeast(0), placeable.height) { placeable.place(-by.roundToPx(), 0) }
}

/** Connect's name and mark, the page's title, search and your picture, and the search field under them. */
@Composable
private fun DiscoverHead(state: ConnectState, handle: String) {
    val palette = LocalPalette.current
    val accent = LocalConnectAccent.current
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(state.searching) {
        if (state.searching) {
            delay(220)
            runCatching { focus.requestFocus() }
        }
    }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)) {
            ConnectMark(accent, Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text("zenith", color = palette.onGlass, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp)
            Spacer(Modifier.width(6.dp))
            Text(
                "CONNECT",
                color = palette.onGlassMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.8.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            PageTitle("Discover", Modifier.weight(1f))
            RoundGlassButton(
                icon = if (state.searching) Icons.Rounded.Close else Icons.Rounded.Search,
                description = if (state.searching) "Close search" else "Search setups",
                size = 44.dp,
                rotation = 0f,
            ) {
                if (state.searching) {
                    focusManager.clearFocus()
                    state.closeSearch()
                } else {
                    state.openSearch()
                }
            }
            Spacer(Modifier.width(8.dp))
            YouButton(handle) { state.goTab(ConnectTab.VAULT) }
        }
        AnimatedVisibility(
            visible = state.searching,
            enter = expandVertically(spring(dampingRatio = 0.86f, stiffness = 380f)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(240)) + fadeOut(tween(160)),
        ) {
            Row(
                Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .height(48.dp)
                    .inPageGlass(RoundedCornerShape(50))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.onGlassMuted, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (state.query.isEmpty()) {
                        Text("Search setups, creators, vibes", color = palette.onGlassMuted, fontSize = 16.sp, maxLines = 1)
                    }
                    BasicTextField(
                        value = state.query,
                        onValueChange = { state.query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = palette.onGlass, fontSize = 16.sp),
                        cursorBrush = SolidColor(accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                }
            }
        }
    }
}

/** Your picture: your first letter in the accent's ring, which opens the Vault. */
@Composable
private fun YouButton(handle: String, onClick: () -> Unit) {
    val accent = LocalConnectAccent.current
    val palette = LocalPalette.current
    val tick = rememberTick()
    Box(
        Modifier
            .size(44.dp)
            .pressBounce(0.88f)
            .clip(CircleShape)
            .background(if (palette.dark) Color(0xFF1C1B2E) else accent.copy(alpha = 0.14f))
            .border(2.dp, accent, CircleShape)
            .clickable(role = Role.Button, onClickLabel = "Open your Vault") {
                tick()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(com.gabriel.zenithui.data.connect.ConnectStudio.initial(handle), color = palette.onGlass, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** The setup of the week's big card: its wallpaper, its phone floating tilted on it, and a pane with what it is. */
@Composable
private fun Hero(setup: ConnectSetup, modifier: Modifier = Modifier, onOpen: (Rect?) -> Unit) {
    val accent = LocalConnectAccent.current
    val tick = rememberTick()
    val bounds = remember { arrayOfNulls<Rect>(1) }
    val still = LocalLite.current || LocalMotionLevel.current.reduced
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(214.dp)
            .onGloballyPositioned { bounds[0] = it.boundsInWindow() }
            .pressBounce(0.98f)
            .clip(shape)
            .wall(setup.wall)
            .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
            .clickable(role = Role.Button, onClickLabel = "Open ${setup.name}") {
                tick()
                onOpen(bounds[0])
            },
    ) {
        val float = if (still) {
            null
        } else {
            val transition = rememberInfiniteTransition(label = "heroFloat")
            transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "heroFloatValue",
            )
        }
        MiniPhone(
            setup,
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-22).dp, y = 26.dp)
                .size(width = 116.dp, height = 236.dp)
                .graphicsLayer {
                    val f = float?.value ?: 0f
                    cameraDistance = 10f * density
                    rotationY = -24f + 4f * f
                    rotationX = 10f - 2f * f
                    rotationZ = 3f
                    translationY = -8.dp.toPx() * f
                    shadowElevation = 18.dp.toPx()
                    this.shape = RoundedCornerShape(24.dp)
                },
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .width(204.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0C0B1C).copy(alpha = 0.58f))
                .glassOnGlass(RoundedCornerShape(20.dp), dark = true)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Overline("Setup of the week", colour = lighten(accent))
            Text(setup.name, color = Color(0xFFF6F6F9), fontSize = 19.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp, maxLines = 2)
            Text("by @${setup.by}", color = Color(0xBFF6F6F9), fontSize = 13.sp, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xBFF6F6F9), modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "${setup.rating?.let(ConnectFeed::rating) ?: "New"} · ${ConnectFeed.count(setup.applies)} applies",
                    color = Color(0xBFF6F6F9),
                    fontSize = 13.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

/** The accent made light enough to read on a dark wallpaper. */
internal fun lighten(colour: Color): Color = androidx.compose.ui.graphics.lerp(colour, Color.White, 0.25f)

/** A setup's card: its little home screen, what kind it is, its name, who made it, and its rating or age. */
@Composable
private fun SetupCard(setup: ConnectSetup, tab: FeedTab, fresh: Boolean, modifier: Modifier = Modifier, onOpen: (Rect?) -> Unit) {
    val palette = LocalPalette.current
    val accent = LocalConnectAccent.current
    val tick = rememberTick()
    val bounds = remember { arrayOfNulls<Rect>(1) }
    val ring by animateColorAsState(if (fresh) accent.copy(alpha = 0.75f) else Color.Transparent, tween(500), label = "freshRing")
    val glow by animateColorAsState(if (fresh) accent.copy(alpha = 0.2f) else Color.Transparent, tween(500), label = "freshGlow")
    val shape = CARD_SHAPE
    Column(
        modifier
            .fillMaxWidth()
            .onGloballyPositioned { bounds[0] = it.boundsInWindow() }
            .pressBounce(0.97f)
            .drawBehind {
                if (glow.alpha > 0f) {
                    val grow = 4.dp.toPx()
                    drawRoundRect(glow, topLeft = Offset(-grow, -grow), size = Size(size.width + grow * 2, size.height + grow * 2), cornerRadius = CornerRadius(28.dp.toPx()))
                }
            }
            .connectCard(shape, outline = ring.takeIf { it.alpha > 0.01f })
            .clickable(role = Role.Button, onClickLabel = "Open ${setup.name}") {
                tick()
                onOpen(bounds[0])
            }
            .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 12.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(ConnectFeed.previewHeight(setup.kind).dp)
                .clip(RoundedCornerShape(18.dp)),
        ) {
            SetupPreview(setup, Modifier.fillMaxSize())
            KindChip(setup.kind, Modifier.padding(8.dp))
        }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.padding(horizontal = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(setup.name, color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "@${setup.by}",
                    color = palette.onGlassMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(6.dp))
                Crossfade(ConnectFeed.stat(setup, tab) to ConnectFeed.starred(setup, tab), animationSpec = tween(200), label = "cardStat") { (stat, starred) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (starred) {
                            Icon(Icons.Rounded.Star, contentDescription = null, tint = palette.onGlass.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                        }
                        Text(stat, color = palette.onGlass.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}
