package com.gabriel.zenithui.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.gabriel.zenithui.data.AppCell
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.FolderCell
import com.gabriel.zenithui.data.HomeGeometry
import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.ROW_UNITS
import com.gabriel.zenithui.data.HostedWidgetCell
import com.gabriel.zenithui.data.ShortcutCell
import com.gabriel.zenithui.data.StackCell
import com.gabriel.zenithui.data.WidgetCell
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.AppIcon
import com.gabriel.zenithui.ui.home.DockSlab
import com.gabriel.zenithui.ui.home.HomeItemContent
import com.gabriel.zenithui.ui.home.OnWallpaper
import com.gabriel.zenithui.ui.home.STANDARD_ICON_SIZE
import com.gabriel.zenithui.ui.home.SearchPill
import com.gabriel.zenithui.ui.home.SlotArrangement
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalBackdrop
import com.gabriel.zenithui.ui.theme.LocalGlassLevels
import com.gabriel.zenithui.ui.theme.ZenithShapes
import kotlin.math.min

/** The phone's position on screen, kept outside state: the zoom reads it every frame and must not recompose. */
private class PhoneSpot {
    var coordinates: LayoutCoordinates? = null
}

/**
 * Your actual home screen, shrunk to fit: the same widgets, your own apps and icons, laid out by the
 * same code that will build the real thing. It is drawn at full phone size and scaled down, so what
 * you see here is exactly what you get, down to the curve of your phone's own screen corners.
 *
 * [shown] lets the page assemble itself: grid items first, then the dock, each counted in order.
 * [halo] is the strength of the coloured light behind it, [sweep] runs a band of light across it
 * once (0 to 1), and [zoom] grows it until it fills the whole screen (0 to 1), for the hand-off to
 * the real home screen. All three are read while drawing only.
 */
@Composable
fun PhonePreview(
    items: List<HomeItem>,
    dock: List<String>,
    appsByKey: Map<String, AppInfo>,
    settings: ZenithSettings,
    wallpaper: ImageBitmap?,
    screenWidth: Dp,
    screenHeight: Dp,
    shown: Int,
    modifier: Modifier = Modifier,
    deviceCorner: Dp = 46.dp,
    halo: () -> Float = { 0f },
    sweep: () -> Float = { 0f },
    zoom: () -> Float = { 0f },
) {
    val palette = LocalPalette.current
    val edge = palette.onGlass.copy(alpha = 0.16f)
    val accent = palette.accent
    // A black shadow under a pale phone reads as a smudge; a deep blue one reads as depth.
    val shadow = if (palette.dark) Color.Black else Color(0xFF1C2B4A).copy(alpha = 0.28f)
    val sweepAlpha = if (palette.dark) 0.22f else 0.30f
    val spot = remember { PhoneSpot() }

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val scale = min(maxWidth / screenWidth, maxHeight / screenHeight).coerceAtLeast(0.05f)
        val phoneWidth = screenWidth * scale
        val phoneHeight = screenHeight * scale
        // Scaled with everything else, so when the phone grows to fill the screen the corner
        // lands exactly on the real screen's corner with no reshaping on the way.
        val corner = deviceCorner * scale
        val shape = RoundedCornerShape(corner)

        // The light behind the phone. Drawn past its own bounds on purpose, so it has no edge.
        Spacer(
            Modifier
                .matchParentSize()
                .drawWithCache {
                    val radius = phoneWidth.toPx() * 0.9f
                    val glow = Brush.radialGradient(
                        listOf(accent, accent.copy(alpha = 0f)),
                        center = size.center,
                        radius = radius,
                    )
                    onDrawBehind {
                        val strength = halo()
                        if (strength > 0.001f) {
                            drawCircle(glow, radius = radius, center = center, alpha = strength.coerceIn(0f, 1f))
                        }
                    }
                }
        )

        // Measured here, outside the layer that moves, so the zoom always knows where it started.
        Box(
            Modifier
                .size(phoneWidth, phoneHeight)
                .onPlaced { spot.coordinates = it },
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val p = zoom()
                        val coordinates = spot.coordinates
                        if (p > 0f && coordinates != null && coordinates.isAttached) {
                            val grow = 1f + (1f / scale - 1f) * p
                            scaleX = grow
                            scaleY = grow
                            val root = coordinates.findRootCoordinates().size
                            val origin = coordinates.positionInRoot()
                            translationX = (root.width / 2f - (origin.x + size.width / 2f)) * p
                            translationY = (root.height / 2f - (origin.y + size.height / 2f)) * p
                        }
                        shadowElevation = 28.dp.toPx() * scale * (1f - p)
                        ambientShadowColor = shadow
                        spotShadowColor = shadow
                        this.shape = shape
                        clip = true
                    }
                    .drawWithCache {
                        val band = size.width * 0.35f
                        val bandBrush = Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = sweepAlpha), Color.Transparent),
                            startX = 0f,
                            endX = band,
                        )
                        val stroke = 1.dp.toPx()
                        val radius = (corner.toPx() - stroke / 2f).coerceAtLeast(0f)
                        onDrawWithContent {
                            drawContent()
                            val s = sweep()
                            if (s > 0f && s < 1f) {
                                val left = (-0.5f + 2f * s) * size.width - band / 2f
                                translate(left = left) { drawRect(bandBrush, size = Size(band, size.height)) }
                            }
                            val rim = 1f - zoom()
                            if (rim > 0f) {
                                drawRoundRect(
                                    color = edge,
                                    topLeft = Offset(stroke / 2f, stroke / 2f),
                                    size = Size(size.width - stroke, size.height - stroke),
                                    cornerRadius = CornerRadius(radius, radius),
                                    style = Stroke(stroke),
                                    alpha = rim,
                                )
                            }
                        }
                    },
            ) {
                Box(
                    Modifier
                        .wrapContentSize(Alignment.TopStart, unbounded = true)
                        .requiredSize(screenWidth, screenHeight)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0f)
                        },
                ) {
                    MiniHome(
                        items = items,
                        dock = dock,
                        appsByKey = appsByKey,
                        settings = settings,
                        wallpaper = wallpaper,
                        shown = shown,
                        // Over the last stretch of the zoom the preview's own background dissolves,
                        // so your real wallpaper is already there when the home screen takes over.
                        backgroundAlpha = { ((1f - zoom()) / 0.4f).coerceIn(0f, 1f) },
                    )
                }
                // It is a picture of your phone, not a second phone: nothing inside takes a touch.
                Box(Modifier.matchParentSize().pointerInput(Unit) { detectTapGestures { } })
            }
        }
    }
}

@Composable
private fun MiniHome(
    items: List<HomeItem>,
    dock: List<String>,
    appsByKey: Map<String, AppInfo>,
    settings: ZenithSettings,
    wallpaper: ImageBitmap?,
    shown: Int,
    backgroundAlpha: () -> Float,
) {
    val palette = LocalPalette.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val backdrop = Brush.verticalGradient(
        listOf(palette.accent.copy(alpha = 0.55f), palette.backdrop.getOrElse(1) { palette.backdrop.first() }, palette.backdrop.last())
    )
    // The page settles where everything sits; the dock stands on the same lines.
    var page by remember { mutableStateOf<HomeGeometry?>(null) }
    val density = LocalDensity.current

    // A picture of the phone, drawn small. Neither the lens nor the frosted copy of the wallpaper
    // would line up with the wallpaper in the picture, so its glass is frosted, a body of its own,
    // as in the wallpaper sheet's previews.
    CompositionLocalProvider(LocalGlassLevels provides null, LocalBackdrop provides null) {
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind { drawRect(backdrop, alpha = backgroundAlpha()) }
        ) {
            if (wallpaper != null) {
                Image(
                    wallpaper,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = backgroundAlpha() },
                )
            }
            // Laid out exactly like the home screen it becomes: the page, Search in its band and the
            // dock, with nothing written above the page and no page dots.
            Column(Modifier.fillMaxSize().padding(top = statusTop)) {
                // The air under the status bar, as on the home screen.
                Spacer(Modifier.height(8.dp))

                BoxWithConstraints(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    val geometry = remember(constraints.maxWidth, constraints.maxHeight, settings.columns, settings.rows, density.density, settings.showLabels) {
                        HomeGeometry(constraints.maxWidth, constraints.maxHeight, settings.columns, settings.rows, density.density, settings.showLabels)
                    }
                    val cellWidth = with(density) { geometry.cellWidth.toDp() }
                    val cellHeight = with(density) { geometry.unitHeight.toDp() }
                    val originX = with(density) { geometry.originX.toDp() }
                    val originY = with(density) { geometry.originY.toDp() }
                    SideEffect { page = geometry }

                    // On the wallpaper, as a home page's are, so the names are white.
                    OnWallpaper {
                        items.forEachIndexed { index, item ->
                            key(stableKey(item)) {
                                val visible = index < shown
                                val appear by animateFloatAsState(
                                    targetValue = if (visible) 1f else 0f,
                                    animationSpec = if (visible) spring(dampingRatio = 0.62f, stiffness = 360f) else tween(160),
                                    label = "appear",
                                )
                                // Adding or removing a widget moves the others; they glide rather than jump.
                                val x by animateDpAsState(originX + cellWidth * item.col, spring(dampingRatio = 0.9f, stiffness = 320f), label = "x")
                                val y by animateDpAsState(originY + cellHeight * item.row, spring(dampingRatio = 0.9f, stiffness = 320f), label = "y")
                                Box(
                                    Modifier
                                        .offset(x, y)
                                        .size(cellWidth * item.spanX, cellHeight * item.spanY)
                                        .graphicsLayer {
                                            alpha = appear.coerceIn(0f, 1f)
                                            val s = 0.82f + 0.18f * appear
                                            scaleX = s
                                            scaleY = s
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    HomeItemContent(
                                        item = item,
                                        settings = settings,
                                        appsByKey = appsByKey,
                                        geometry = geometry,
                                        onSearch = {},
                                        onNoteChange = {},
                                        // The real one is on the home screen: a widget can only be drawn once.
                                        live = false,
                                    )
                                }
                            }
                        }
                    }
                }

                // Search, the pill in the middle of its band, as on the home screen.
                Box(Modifier.fillMaxWidth().height(HomeGeometry.SEARCH_BAND_DP.dp), contentAlignment = Alignment.Center) {
                    SearchPill(onOpen = {}, dots = { 0f }, pageCount = 1, position = { 0f })
                }

                if (settings.showDock && dock.isNotEmpty()) {
                    MiniDock(
                        dock = dock,
                        appsByKey = appsByKey,
                        settings = settings,
                        page = page,
                        // Its apps come after the page's while the home screen is put together.
                        shownFrom = items.size,
                        shown = shown,
                        modifier = Modifier.padding(bottom = navBottom + 10.dp),
                    )
                } else {
                    Spacer(Modifier.padding(bottom = navBottom).height(10.dp))
                }
            }
        }
    }
}

/**
 * The dock as the home screen draws it: the same slab of glass, with its apps in the same places
 * ([HomeGeometry.dockIconX]) and no names, standing still. Its apps arrive in turn, after the page's
 * [shownFrom] things, as [shown] counts up.
 */
@Composable
private fun MiniDock(
    dock: List<String>,
    appsByKey: Map<String, AppInfo>,
    settings: ZenithSettings,
    page: HomeGeometry?,
    shownFrom: Int,
    shown: Int,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val style = settings.dockStyle
    BoxWithConstraints(modifier.fillMaxWidth().height(HomeGeometry.DOCK_HEIGHT_DP.dp)) {
        val wide = constraints.maxWidth
        val iconPx = page?.dockIcon(dock.size) ?: with(density) { STANDARD_ICON_SIZE.roundToPx() }
        // Icons only has no glass.
        if (style != "none") {
            val slab = HomeGeometry.dockSlabWidth(style, dock.size, wide, iconPx, density.density)
            DockSlab(width = { slab })
        }
        val arrangement = remember(page, style, dock.size, wide) {
            page?.let { g -> SlotArrangement(List(dock.size) { g.dockIconX(style, it, dock.size, wide) }) }
        }
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = arrangement ?: Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dock.forEachIndexed { index, appKey ->
                key(appKey) {
                    val visible = shownFrom + index < shown
                    val appear by animateFloatAsState(
                        targetValue = if (visible) 1f else 0f,
                        animationSpec = if (visible) spring(dampingRatio = 0.62f, stiffness = 360f) else tween(160),
                        label = "dockAppear",
                    )
                    Box(contentAlignment = Alignment.Center) {
                        AppIcon(
                            key = appKey,
                            label = appsByKey[appKey]?.label ?: "",
                            size = with(density) { iconPx.toDp() },
                            shape = ZenithShapes.forIconShape(settings.iconShape),
                            modifier = Modifier.graphicsLayer {
                                alpha = appear.coerceIn(0f, 1f)
                                val s = 0.7f + 0.3f * appear
                                scaleX = s
                                scaleY = s
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Items are rebuilt with fresh ids whenever a choice changes; this is what they really are. */
private fun stableKey(item: HomeItem): String = when (item) {
    is WidgetCell -> "w:${item.type}"
    is AppCell -> "a:${item.packageName}/${item.className}"
    is FolderCell -> "f:${item.title}"
    is ShortcutCell -> "s:${item.key}"
    is HostedWidgetCell -> "h:${item.provider}"
    is StackCell -> "k:${item.members.joinToString(",") { it.type.ifEmpty { it.provider } }}"
}
