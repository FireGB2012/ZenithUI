package com.gabriel.zenithui.ui.connect

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.common.LocalLabelStyle
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.theme.LocalGlassLevels
import com.gabriel.zenithui.ui.theme.LocalOnWallpaper
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalScreenSize
import com.gabriel.zenithui.ui.theme.LocalTheme
import com.gabriel.zenithui.ui.theme.LocalWallpaperDim
import com.gabriel.zenithui.ui.theme.LocalWallpaperOffset
import com.gabriel.zenithui.ui.theme.drawSoftenedWallpaper
import com.gabriel.zenithui.ui.theme.isGlass
import com.gabriel.zenithui.ui.theme.palette
import com.gabriel.zenithui.ui.theme.themePage
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Zenith Connect, the app that lives inside Zenith: Discover, Share, the Vault and Updates under a
 * tab bar of glass, with a setup's page, the apply screen and the sheets over them. Zenith draws it
 * itself, opened from its icon and closed back into it like any app, so Android never sees it as one.
 *
 * It stands on your own wallpaper, a little dimmed, and everything on it is Zenith's own Liquid
 * Glass, the glass of the home screen's widgets and dock, lined up with the wallpaper behind it: in
 * Lite too, which keeps that glass. In a theme that is not glass (Clean), or with no copy of the
 * wallpaper to stand on, it is the theme's own page and panels, as Settings is.
 *
 * What it shows and does is real ([ConnectBridge]): Zenith's picks, the Looks featured on Zenith's
 * server and any Look by its code, and your own; previews of your own home screen wearing them; and
 * Apply, which puts one on Zenith, with Undo to take it off.
 */
@Composable
fun ConnectApp(state: ConnectState, bridge: ConnectBridge, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val latest by rememberUpdatedState(bridge)
    LaunchedEffect(state) { state.load(latest) }
    DisposableEffect(state) {
        state.visible = true
        onDispose { state.visible = false }
    }
    BackHandler {
        if (!state.back()) onClose()
    }

    val theme = LocalTheme.current
    val levels = LocalGlassLevels.current
    // On your wallpaper: a glass theme with Zenith's copy of the wallpaper to stand on. There, as on
    // the home screen, the words are white whatever Light or Dark, and the glass is the wallpaper's.
    val onWall = theme.isGlass && levels != null
    val wallPalette = remember(theme) { theme.palette(true) }
    val palette = if (onWall) wallPalette else LocalPalette.current
    val dim = remember { mutableFloatStateOf(GLASS_DIM) }
    val none = remember { mutableFloatStateOf(0f) }

    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalOnWallpaper provides onWall,
        LocalLabelStyle provides LocalLabelStyle.current.copy(onWallpaper = onWall),
        // The glass shows the wallpaper as dim as the page round it is.
        LocalWallpaperDim provides if (onWall) dim else none,
    ) {
        Box(
            modifier
                .fillMaxSize()
                // It is all of the screen: a touch anywhere on it is Connect's, never the home screen's under it.
                .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false) } },
        ) {
            Ground(onWall)
            val reduced = LocalMotionLevel.current.reduced
            // The tabs go out as a setup's page comes in over them, and come back as it goes.
            val under by animateFloatAsState(
                if (state.detail != null) 1f else 0f,
                if (reduced) tween(150) else tween(260),
                label = "tabsUnder",
            )
            if (under < 0.999f) {
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - under }) {
                    TabPages(state, bridge)
                    TopFade()
                    FootFade(Modifier.align(Alignment.BottomCenter))
                    TabBar(state, modifier = Modifier.align(Alignment.BottomCenter))
                }
            }
            SetupPage(state, bridge)
            ApplyScreen(state, bridge, onOpenZenith = {
                state.leaveApply()
                state.closeDetail()
                onClose()
            })
            ConnectSheets(state, bridge)
            // With a sheet up a word comes at the top, clear of the sheet.
            val sheetUp = state.sheet != null
            Toast(
                state,
                overTabs = state.detail == null,
                atTop = sheetUp,
                modifier = Modifier.align(if (sheetUp) Alignment.TopCenter else Alignment.BottomCenter),
            )
        }
    }
}

/** How much darker Connect lays the wallpaper under itself, and the glass shows it by as much. */
private const val GLASS_DIM = 0.4f

/**
 * What Connect stands on: Zenith's copy of your wallpaper, exactly where the real one is, and a
 * veil over it a little darker at the top and the foot, where the titles and the tab bar are. In a
 * theme that is not glass, the theme's own page.
 */
@Composable
private fun Ground(onWall: Boolean) {
    val levels = LocalGlassLevels.current
    val screen = LocalScreenSize.current
    val offset = LocalWallpaperOffset.current
    val palette = LocalPalette.current
    if (onWall && levels != null) {
        var origin by remember { mutableStateOf(Offset.Zero) }
        val veil = remember {
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.52f),
                0.3f to Color.Black.copy(alpha = GLASS_DIM),
                0.75f to Color.Black.copy(alpha = GLASS_DIM),
                1f to Color.Black.copy(alpha = 0.5f),
            )
        }
        Canvas(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInWindow() }) {
            val window = if (screen.width > 0 && screen.height > 0) screen else IntSize(size.width.roundToInt(), size.height.roundToInt())
            drawSoftenedWallpaper(levels.sharp, origin, window, offset.value, size, 1f)
            drawRect(veil)
        }
    } else {
        Box(Modifier.fillMaxSize().themePage(palette, RectangleShape))
    }
}

/** Where a tab's page starts below the status bar, and the room it leaves at its foot for the tab bar. */
@Composable
internal fun tabPadding(): PaddingValues {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return PaddingValues(top = top + 10.dp, bottom = bottom + TAB_BAR_HEIGHT + 34.dp)
}

/** The tabs' pages, the one chosen fading in over the last. */
@Composable
private fun TabPages(state: ConnectState, bridge: ConnectBridge) {
    val reduced = LocalMotionLevel.current.reduced
    AnimatedContent(
        targetState = state.tab,
        transitionSpec = {
            fadeIn(tween(if (reduced) 120 else 220, delayMillis = if (reduced) 0 else 40))
                .togetherWith(fadeOut(tween(if (reduced) 120 else 160)))
        },
        label = "connectTabs",
        modifier = Modifier.fillMaxSize(),
    ) { tab ->
        when (tab) {
            ConnectTab.DISCOVER -> DiscoverTab(state, bridge)
            ConnectTab.SHARE -> ShareTab(state, bridge)
            ConnectTab.VAULT -> VaultTab(state, bridge)
            ConnectTab.UPDATES -> UpdatesTab(state, bridge)
        }
    }
}

/** Under the status bar what scrolls up fades out, so the clock and the battery stay clear. */
@Composable
private fun TopFade() {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val ink = edgeColour(top = true)
    val strong = !LocalOnWallpaper.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(top + 22.dp)
            .background(
                Brush.verticalGradient(
                    0f to ink.copy(alpha = if (strong) 0.92f else 0.45f),
                    0.6f to ink.copy(alpha = if (strong) 0.6f else 0.22f),
                    1f to Color.Transparent,
                ),
            ),
    )
}

/**
 * Under the tab bar what scrolls down fades out, as it does under iOS 26's, so nothing shows
 * plainly round the foot of the floating bar.
 */
@Composable
private fun FootFade(modifier: Modifier = Modifier) {
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(
        modifier
            .fillMaxWidth()
            .height(bottom + TAB_BAR_HEIGHT + 34.dp)
            .background(footFade()),
    )
}

/** What a page fades into at its top or its foot: the dark over the wallpaper, the theme's own page in a theme that is not glass. */
@Composable
internal fun edgeColour(top: Boolean): Color {
    if (LocalOnWallpaper.current) return Color.Black
    val backdrop = LocalPalette.current.backdrop
    return (if (top) backdrop.firstOrNull() else backdrop.lastOrNull()) ?: Color.Black
}

/** The fade at the foot of a page, under whatever floats there. */
@Composable
internal fun footFade(): Brush {
    val onWall = LocalOnWallpaper.current
    val ink = edgeColour(top = false)
    return remember(ink, onWall) {
        if (onWall) {
            Brush.verticalGradient(0f to Color.Transparent, 0.4f to ink.copy(alpha = 0.3f), 1f to ink.copy(alpha = 0.62f))
        } else {
            Brush.verticalGradient(0f to Color.Transparent, 0.4f to ink.copy(alpha = 0.75f), 1f to ink.copy(alpha = 0.96f))
        }
    }
}

/** The title at the head of a tab, with whatever stands beside it. */
@Composable
internal fun TabTitle(title: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    val palette = LocalPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            color = palette.onGlass,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.8).sp,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

internal val TAB_BAR_HEIGHT: Dp = 62.dp

/**
 * The tab bar, as iOS 26 has it: a capsule of glass floating over the page with the four tabs on
 * it, the chosen one on a lighter capsule that slides across, stretching as it goes, and beside it
 * a round pane of its own for typing somebody's code.
 */
@Composable
private fun TabBar(state: ConnectState, modifier: Modifier = Modifier) {
    val reduced = LocalMotionLevel.current.reduced
    val palette = LocalPalette.current
    val tick = rememberTick()
    val target = state.tab.ordinal.toFloat()
    val place = remember { Animatable(target) }
    val from = remember { floatArrayOf(target) }
    LaunchedEffect(target) {
        from[0] = place.value
        if (reduced) place.snapTo(target) else place.animateTo(target, tween(480, easing = CubicBezierEasing(0.3f, 0.7f, 0.2f, 1f)))
    }
    Row(
        modifier
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
            .widthIn(max = 540.dp)
            .fillMaxWidth()
            .height(TAB_BAR_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .pane(PILL, interactive = true)
                .padding(5.dp),
        ) {
            val lit = palette.onGlass.copy(alpha = 0.15f)
            Canvas(Modifier.fillMaxSize()) {
                val span = place.targetValue - from[0]
                val travelled = if (span == 0f) 1f else ((place.value - from[0]) / span).coerceIn(0f, 1f)
                val stretch = 1f + 0.4f * sin(PI.toFloat() * travelled)
                val w = size.width / ConnectTab.entries.size
                val width = (w * stretch).coerceAtMost(size.width)
                val x = (place.value * w - (width - w) / 2f).coerceIn(0f, size.width - width)
                drawRoundRect(lit, topLeft = Offset(x, 0f), size = Size(width, size.height), cornerRadius = CornerRadius(size.height / 2f))
            }
            Row(Modifier.fillMaxSize()) {
                ConnectTab.entries.forEach { tab ->
                    val on = tab == state.tab
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
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
                        val ink = if (on) palette.accent else palette.onGlass
                        Icon(tab.icon(on), contentDescription = null, tint = ink, modifier = Modifier.size(23.dp))
                        Spacer(Modifier.height(2.dp))
                        Text(tab.label, color = ink, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        RoundButton(
            icon = Icons.Rounded.VpnKey,
            label = "Have a code",
            onClick = { state.openSheet(ConnectSheet.CODE) },
            size = TAB_BAR_HEIGHT,
        )
    }
}

private fun ConnectTab.icon(on: Boolean): ImageVector = when (this) {
    ConnectTab.DISCOVER -> if (on) Icons.Rounded.AutoAwesome else Icons.Outlined.AutoAwesome
    ConnectTab.SHARE -> if (on) Icons.Rounded.IosShare else Icons.Outlined.IosShare
    ConnectTab.VAULT -> if (on) Icons.Rounded.Inventory2 else Icons.Outlined.Inventory2
    ConnectTab.UPDATES -> if (on) Icons.Rounded.NewReleases else Icons.Outlined.NewReleases
}

/** The pill a word comes up in, over the tab bar, or near the foot with no tab bar on show. */
@Composable
private fun Toast(state: ConnectState, overTabs: Boolean, atTop: Boolean, modifier: Modifier = Modifier) {
    val reduced = LocalMotionLevel.current.reduced
    val palette = LocalPalette.current
    val lift by animateFloatAsState(if (overTabs && !atTop) 1f else 0f, tween(300), label = "toastLift")
    // It comes in from the edge it stands by.
    val from = if (atTop) -1 else 1
    Box(
        modifier
            .then(if (atTop) Modifier.statusBarsPadding().padding(top = 12.dp) else Modifier.navigationBarsPadding().padding(bottom = 22.dp))
            // Lifted over the tab bar by placement, never a layer, so its glass stays lined up with the wallpaper.
            .offset { IntOffset(0, -(lift * (TAB_BAR_HEIGHT + 4.dp).toPx()).roundToInt()) }
            .padding(horizontal = 24.dp),
        contentAlignment = if (atTop) Alignment.TopCenter else Alignment.BottomCenter,
    ) {
        AnimatedContent(
            targetState = state.toast?.let { it to state.toastTurn },
            transitionSpec = {
                if (reduced) {
                    fadeIn(tween(150)).togetherWith(fadeOut(tween(150)))
                } else {
                    (slideInVertically(spring(dampingRatio = 0.7f, stiffness = 420f)) { from * it / 2 } + fadeIn(tween(180)))
                        .togetherWith(slideOutVertically(tween(200)) { from * it / 3 } + fadeOut(tween(160)))
                }
            },
            contentAlignment = if (atTop) Alignment.TopCenter else Alignment.BottomCenter,
            label = "toast",
        ) { shown ->
            if (shown != null) {
                Text(
                    shown.first,
                    color = palette.onGlass,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    modifier = Modifier
                        .pane(PILL, shadow = true)
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                )
            } else {
                Spacer(Modifier.height(1.dp))
            }
        }
    }
}

/** A dot in a colour, for how something stands. */
@Composable
internal fun Dot(colour: Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(modifier.size(size).background(colour, CircleShape))
}
