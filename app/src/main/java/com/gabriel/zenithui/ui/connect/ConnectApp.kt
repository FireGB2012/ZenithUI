package com.gabriel.zenithui.ui.connect

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.connect.ConnectStudio
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.TITLE_GONE_PX
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalTheme
import com.gabriel.zenithui.ui.theme.glassOnGlass
import com.gabriel.zenithui.ui.theme.isGlass
import kotlin.math.PI
import kotlin.math.sin

/**
 * Zenith Connect, the app that lives inside Zenith: Discover, Community, the Beta Hub and the
 * Vault under a glass tab bar, with a setup's page, the apply screen and the Creator Studio over
 * them. It is drawn by Zenith itself, opened from its icon and closed back into it like any app,
 * so Android never sees it as one: it is not in the phone's app list, and no other launcher has it.
 *
 * [userName] is the name you gave Zenith, which Connect knows you by. [onClose] puts it away,
 * back into its icon: from back with nothing left to close, and from the apply screen's Open Zenith.
 */
@Composable
fun ConnectApp(state: ConnectState, userName: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LaunchedEffect(state) { state.load(context) }
    DisposableEffect(state) {
        state.visible = true
        onDispose { state.visible = false }
    }
    val palette = LocalPalette.current
    val accent = state.accent.colour?.let { Color(it) } ?: palette.accent
    val onAccent = if (state.accent.colour == null) palette.onAccent else inkOn(accent)
    val handle = remember(userName) { ConnectStudio.handle(userName) }
    val reduced = LocalMotionLevel.current.reduced

    BackHandler {
        if (!state.back()) onClose()
    }

    CompositionLocalProvider(LocalConnectAccent provides accent, LocalConnectOnAccent provides onAccent) {
        val ground = pageBrush()
        Box(
            modifier
                .fillMaxSize()
                .background(ground)
                // It is all of the screen: a touch anywhere on it is Connect's, never the home screen's under it.
                .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false) } },
        ) {
            // The tabs sink back a little and dim as a setup's page comes up over them.
            val under by animateFloatAsState(
                if (state.detail != null) 1f else 0f,
                if (reduced) tween(150) else spring(dampingRatio = 0.9f, stiffness = 260f),
                label = "tabsUnder",
            )
            BackdropHost(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val scale = 1f - 0.07f * under
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - 0.7f * under
                    },
                page = {
                    Aura(accent)
                    TabPages(state, handle, userName)
                },
                chrome = {
                    ScrollEdge(state)
                    TabBar(state, hidden = under, modifier = Modifier.align(Alignment.BottomCenter))
                },
            )
            SetupPage(state)
            ApplyScreen(state, onOpenZenith = {
                state.leaveApply()
                onClose()
            })
            StudioSheet(state, handle)
            AccentSheet(state)
            Toast(state, overTabs = state.detail == null, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

/**
 * A page and what floats over it: [page] is recorded as it is drawn, so [chrome] can show it
 * through its glass, blurred (see [connectGlass]).
 */
@Composable
internal fun BackdropHost(
    modifier: Modifier = Modifier,
    page: @Composable BoxScope.() -> Unit,
    chrome: @Composable BoxScope.() -> Unit,
) {
    val layer = rememberGraphicsLayer()
    val backdrop = remember { ConnectBackdrop() }
    backdrop.layer = layer
    Box(modifier.onGloballyPositioned { backdrop.host = it }) {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                },
            content = page,
        )
        CompositionLocalProvider(LocalConnectBackdrop provides backdrop) { chrome() }
    }
}

/** The soft glow of the accent in the top corner, still, as the prototype's was. */
@Composable
private fun Aura(accent: Color) {
    val dark = LocalPalette.current.dark
    Canvas(Modifier.fillMaxSize()) {
        val centre = Offset(110.dp.toPx(), 10.dp.toPx())
        val rx = 260.dp.toPx()
        val ry = 220.dp.toPx()
        val brush = Brush.radialGradient(
            listOf(accent.copy(alpha = if (dark) 0.22f else 0.14f), accent.copy(alpha = 0f)),
            center = centre,
            radius = ry,
        )
        withTransform({ scale(rx / ry, 1f, centre) }) {
            drawRect(brush, topLeft = Offset(centre.x - ry, centre.y - ry), size = Size(ry * 2f, ry * 2f))
        }
    }
}

/** How far down from the top of the screen a tab's page starts, and how much room it leaves at its foot for the tab bar. */
@Composable
internal fun pageTop(): Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp

@Composable
internal fun pageBottom(): Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + TAB_BAR_HEIGHT + 34.dp

/** The tabs' pages, the one chosen sliding in from the side it is on. */
@Composable
private fun TabPages(state: ConnectState, handle: String, userName: String) {
    val reduced = LocalMotionLevel.current.reduced
    AnimatedContent(
        targetState = state.tab,
        transitionSpec = { tabChange(reduced) },
        label = "connectTabs",
        modifier = Modifier.fillMaxSize(),
    ) { tab ->
        when (tab) {
            ConnectTab.DISCOVER -> DiscoverTab(state, handle)
            ConnectTab.COMMUNITY -> CommunityTab(state)
            ConnectTab.BETA -> BetaTab(state)
            ConnectTab.VAULT -> VaultTab(state, userName, handle)
        }
    }
}

private val EASE = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

private fun AnimatedContentTransitionScope<ConnectTab>.tabChange(reduced: Boolean): ContentTransform {
    if (reduced) return fadeIn(tween(150)).togetherWith(fadeOut(tween(150)))
    val way = if (targetState.ordinal > initialState.ordinal) 1 else -1
    return (slideInHorizontally(tween(460, easing = EASE)) { (it * 0.08f).toInt() * way } + fadeIn(tween(460, easing = EASE)))
        .togetherWith(slideOutHorizontally(tween(280, easing = EASE)) { -(it * 0.08f).toInt() * way } + fadeOut(tween(280, easing = EASE)))
}

/** The title each tab has at its head, which the small one at the top of the screen repeats once it has scrolled away. */
private fun ConnectTab.title(): String = when (this) {
    ConnectTab.DISCOVER -> "Discover"
    ConnectTab.COMMUNITY -> "Community"
    ConnectTab.BETA -> "Beta Hub"
    ConnectTab.VAULT -> "Vault"
}

/** How far the tab on show has scrolled, in pixels, as far as its title is concerned. Read while drawing. */
private fun ConnectState.scrolled(): Int = when (tab) {
    ConnectTab.DISCOVER -> if (discoverGrid.firstVisibleItemIndex > 0) Int.MAX_VALUE / 2 else discoverGrid.firstVisibleItemScrollOffset
    ConnectTab.COMMUNITY -> if (communityList.firstVisibleItemIndex > 0) Int.MAX_VALUE / 2 else communityList.firstVisibleItemScrollOffset
    ConnectTab.BETA -> betaScroll.value
    ConnectTab.VAULT -> if (vaultList.firstVisibleItemIndex > 0) Int.MAX_VALUE / 2 else vaultList.firstVisibleItemScrollOffset
}

/**
 * The top of the screen as iOS keeps it over a page that has scrolled: the page fades out under
 * the status bar, and the tab's title comes in small once its large one has gone.
 */
@Composable
private fun ScrollEdge(state: ConnectState) {
    val page = pageColour()
    val palette = LocalPalette.current
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        Modifier
            .fillMaxWidth()
            .height(top + 54.dp)
            .graphicsLayer { alpha = (state.scrolled() / 40f).coerceIn(0f, 1f) }
            // All but solid behind the small title, so nothing scrolling under it reads through it.
            .background(Brush.verticalGradient(0f to page, 0.68f to page.copy(alpha = 0.94f), 1f to page.copy(alpha = 0f))),
    ) {
        Text(
            state.tab.title(),
            color = palette.onGlass,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = top + 8.dp)
                .graphicsLayer { alpha = ((state.scrolled() - TITLE_GONE_PX * 0.6f) / (TITLE_GONE_PX * 0.5f)).coerceIn(0f, 1f) },
        )
    }
}

internal val TAB_BAR_HEIGHT = 64.dp

/**
 * The tab bar, as iOS 26 has it: a capsule of glass floating over the page with the four tabs on
 * it, the chosen one on a lighter capsule that slides across, stretching as it goes, and the
 * Studio's + beside it in a glass circle of its own. It drops away under a setup's page ([hidden]).
 */
@Composable
private fun TabBar(state: ConnectState, hidden: Float, modifier: Modifier = Modifier) {
    val reduced = LocalMotionLevel.current.reduced
    val palette = LocalPalette.current
    val accent = LocalConnectAccent.current
    val tick = rememberTick()
    val target = state.tab.ordinal.toFloat()
    val place = remember { Animatable(target) }
    val from = remember { floatArrayOf(target) }
    LaunchedEffect(target) {
        from[0] = place.value
        if (reduced) place.snapTo(target) else place.animateTo(target, tween(540, easing = CubicBezierEasing(0.3f, 0.7f, 0.2f, 1f)))
    }
    Row(
        modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .height(TAB_BAR_HEIGHT)
            .graphicsLayer {
                translationY = hidden * size.height * 1.6f
                alpha = 1f - hidden
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .connectGlass(RoundedCornerShape(50))
                .padding(5.dp),
        ) {
            val lit = if (palette.dark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f)
            Canvas(Modifier.fillMaxSize()) {
                val span = place.targetValue - from[0]
                val travelled = if (span == 0f) 1f else ((place.value - from[0]) / span).coerceIn(0f, 1f)
                val stretch = 1f + 0.45f * sin(PI.toFloat() * travelled)
                val w = size.width / ConnectTab.entries.size
                val width = w * stretch
                val x = place.value * w - (width - w) / 2f
                drawRoundRect(
                    lit,
                    topLeft = Offset(x.coerceIn(0f, size.width - width.coerceAtMost(size.width)), 0f),
                    size = Size(width.coerceAtMost(size.width), size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
                )
            }
            Row(Modifier.fillMaxSize()) {
                ConnectTab.entries.forEach { tab ->
                    val on = tab == state.tab
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .pressBounce(0.9f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab,
                                onClickLabel = tab.label,
                            ) {
                                tick()
                                state.goTab(tab)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        val ink = if (on) accent else palette.onGlass
                        Box {
                            Icon(tab.icon(on), contentDescription = null, tint = ink, modifier = Modifier.size(24.dp))
                            if (tab == ConnectTab.COMMUNITY && state.communityDot) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 0.dp)
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(liveGreen()),
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(tab.label, color = ink, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        CreateButton(open = state.studioOpen, onClick = state::openStudio)
    }
}

private fun ConnectTab.icon(on: Boolean): ImageVector = when (this) {
    ConnectTab.DISCOVER -> if (on) Icons.Rounded.AutoAwesome else Icons.Outlined.AutoAwesome
    ConnectTab.COMMUNITY -> if (on) Icons.Rounded.ChatBubble else Icons.Outlined.ChatBubbleOutline
    ConnectTab.BETA -> if (on) Icons.Rounded.Science else Icons.Outlined.Science
    ConnectTab.VAULT -> if (on) Icons.Rounded.Cloud else Icons.Outlined.Cloud
}

/** The Studio's +, in the accent's glass, turning into an × while the Studio is open. */
@Composable
private fun CreateButton(open: Boolean, onClick: () -> Unit) {
    val accent = LocalConnectAccent.current
    val onAccent = LocalConnectOnAccent.current
    val dark = LocalPalette.current.dark
    val glass = LocalTheme.current.isGlass
    val reduced = LocalMotionLevel.current.reduced
    val tick = rememberTick()
    val turn by animateFloatAsState(if (open) 45f else 0f, if (reduced) tween(150) else spring(dampingRatio = 0.55f, stiffness = 420f), label = "createTurn")
    Box(
        Modifier
            .size(TAB_BAR_HEIGHT)
            .pressBounce(0.88f)
            .clip(CircleShape)
            .background(accent)
            .then(if (glass) Modifier.glassOnGlass(CircleShape, dark, interactive = true) else Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = "Publish a setup",
            ) {
                tick()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Add,
            contentDescription = "Create: publish a setup",
            tint = onAccent,
            modifier = Modifier.size(30.dp).graphicsLayer { rotationZ = turn },
        )
    }
}

/** The pill a word comes up in, over the tab bar, or near the foot with no tab bar on show. */
@Composable
private fun Toast(state: ConnectState, overTabs: Boolean, modifier: Modifier = Modifier) {
    val reduced = LocalMotionLevel.current.reduced
    val palette = LocalPalette.current
    val lift by animateFloatAsState(if (overTabs) 1f else 0f, tween(300), label = "toastLift")
    val words = state.toast
    Box(
        modifier
            .navigationBarsPadding()
            .padding(bottom = 22.dp)
            .graphicsLayer { translationY = -lift * (TAB_BAR_HEIGHT + 4.dp).toPx() }
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedContent(
            targetState = words?.let { it to state.toastTurn },
            transitionSpec = {
                if (reduced) {
                    fadeIn(tween(150)).togetherWith(fadeOut(tween(150)))
                } else {
                    (slideInVertically(spring(dampingRatio = 0.7f, stiffness = 420f)) { it / 2 } + fadeIn(tween(180)))
                        .togetherWith(slideOutVertically(tween(200)) { it / 3 } + fadeOut(tween(160)))
                }
            },
            contentAlignment = Alignment.BottomCenter,
            label = "toast",
        ) { shown ->
            if (shown != null) {
                Text(
                    shown.first,
                    color = palette.onGlass,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier
                        .connectGlass(RoundedCornerShape(50))
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                )
            } else {
                Spacer(Modifier.height(1.dp))
            }
        }
    }
}
