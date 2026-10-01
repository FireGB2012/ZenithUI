package com.gabriel.zenithui.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import com.gabriel.zenithui.data.AppCell
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.FolderCell
import com.gabriel.zenithui.data.HomeGeometry
import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.ROW_UNITS
import com.gabriel.zenithui.data.WidgetCell
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.AppIcon
import com.gabriel.zenithui.ui.home.HomeItemContent
import com.gabriel.zenithui.ui.home.STANDARD_ICON_SIZE
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.GlassMaterial
import com.gabriel.zenithui.ui.theme.glass
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
    // The page settles where everything sits; the name, the search bar and the dock read the same.
    var page by remember { mutableStateOf<HomeGeometry?>(null) }
    val density = LocalDensity.current

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
        // Laid out exactly like the home screen it becomes: name, page, dots, dock.
        Column(Modifier.fillMaxSize().padding(top = statusTop)) {
            Text(
                text = settings.userName.trim().ifBlank { "Zenith" },
                color = palette.onGlass.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 26.dp, vertical = 8.dp),
            )

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
                            )
                        }
                    }
                }
            }

            // The page dot and the add-a-page button beside it, where the real ones sit.
            Row(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .width(22.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(palette.onGlass.copy(alpha = 0.85f))
                )
                Box(
                    Modifier
                        .padding(start = 2.dp)
                        .size(18.dp)
                        .clip(RoundedCornerShape(50))
                        .background(palette.onGlass.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = palette.onGlass.copy(alpha = 0.55f),
                        modifier = Modifier.size(12.dp),
                    )
                }
            }

            if (settings.showDock && dock.isNotEmpty()) {
                // One app per column stands under its column, exactly as the real dock lines up.
                val aligned = dock.size == settings.columns
                Row(
                    Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = navBottom + 10.dp)
                        .fillMaxWidth()
                        .glass(ZenithShapes.panel, strength = 1.15f, material = GlassMaterial.Clear)
                        .padding(vertical = 12.dp, horizontal = if (aligned) 0.dp else 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    dock.forEachIndexed { index, appKey ->
                        key(appKey) {
                            val visible = items.size + index < shown
                            val appear by animateFloatAsState(
                                targetValue = if (visible) 1f else 0f,
                                animationSpec = if (visible) spring(dampingRatio = 0.62f, stiffness = 360f) else tween(160),
                                label = "dockAppear",
                            )
                            Box(
                                if (aligned) Modifier.weight(1f) else Modifier,
                                contentAlignment = Alignment.Center,
                            ) {
                                AppIcon(
                                    key = appKey,
                                    label = appsByKey[appKey]?.label ?: "",
                                    size = page?.iconDp?.dp ?: STANDARD_ICON_SIZE,
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
            } else {
                Spacer(Modifier.padding(bottom = navBottom).height(10.dp))
            }
        }
    }
}

/** Items are rebuilt with fresh ids whenever a choice changes; this is what they really are. */
private fun stableKey(item: HomeItem): String = when (item) {
    is WidgetCell -> "w:${item.type}"
    is AppCell -> "a:${item.packageName}/${item.className}"
    is FolderCell -> "f:${item.title}"
}
