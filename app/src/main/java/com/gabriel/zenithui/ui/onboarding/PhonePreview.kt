package com.gabriel.zenithui.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.AppCell
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.FolderCell
import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.ROW_UNITS
import com.gabriel.zenithui.data.WidgetCell
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.AppIcon
import com.gabriel.zenithui.ui.home.HomeItemContent
import com.gabriel.zenithui.ui.home.STANDARD_ICON_SIZE
import com.gabriel.zenithui.ui.home.homeIconSize
import com.gabriel.zenithui.ui.home.iconLines
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import kotlin.math.min

/**
 * Your actual home screen, shrunk to fit: the same widgets, your own apps and icons, laid out by the
 * same code that will build the real thing. It is drawn at full phone size and scaled down, so what
 * you see here is exactly what you get.
 *
 * [shown] lets the page assemble itself: grid items first, then the dock, each counted in order.
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
) {
    val palette = LocalPalette.current
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val scale = min(maxWidth / screenWidth, maxHeight / screenHeight).coerceAtLeast(0.05f)
        val corner = 46.dp * scale
        Box(
            Modifier
                .size(screenWidth * scale, screenHeight * scale)
                .shadow(28.dp * scale, RoundedCornerShape(corner), ambientColor = Color.Black, spotColor = Color.Black)
                .clip(RoundedCornerShape(corner))
                .border(1.dp, palette.onGlass.copy(alpha = 0.16f), RoundedCornerShape(corner)),
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
                MiniHome(items, dock, appsByKey, settings, wallpaper, shown)
            }
            // It is a picture of your phone, not a second phone: nothing inside takes a touch.
            Box(Modifier.matchParentSize().pointerInput(Unit) { detectTapGestures { } })
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
) {
    val palette = LocalPalette.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(palette.accent.copy(alpha = 0.55f), palette.backdrop.getOrElse(1) { palette.backdrop.first() }, palette.backdrop.last())
                )
            )
    ) {
        if (wallpaper != null) {
            Image(wallpaper, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Column(Modifier.fillMaxSize().padding(top = statusTop, bottom = navBottom)) {
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
                val cellWidth = maxWidth / settings.columns
                val cellHeight = maxHeight / (settings.rows * ROW_UNITS)
                val rowHeight = cellHeight * ROW_UNITS
                val iconSize = homeIconSize(cellWidth, rowHeight, settings.showLabels)
                val lines = iconLines(cellWidth, rowHeight, iconSize, settings.showLabels)

                items.forEachIndexed { index, item ->
                    key(stableKey(item)) {
                        val visible = index < shown
                        val appear by animateFloatAsState(
                            targetValue = if (visible) 1f else 0f,
                            animationSpec = if (visible) spring(dampingRatio = 0.62f, stiffness = 360f) else tween(160),
                            label = "appear",
                        )
                        // Adding or removing a widget moves the others; they glide rather than jump.
                        val x by animateDpAsState(cellWidth * item.col, spring(dampingRatio = 0.9f, stiffness = 320f), label = "x")
                        val y by animateDpAsState(cellHeight * item.row, spring(dampingRatio = 0.9f, stiffness = 320f), label = "y")
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
                                iconSize = iconSize,
                                lines = lines,
                                onSearch = {},
                                onNoteChange = {},
                            )
                        }
                    }
                }
            }

            // The page dot, where the real one sits.
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 10.dp)
                    .width(22.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(palette.onGlass.copy(alpha = 0.85f))
            )

            if (settings.showDock && dock.isNotEmpty()) {
                val dockIcon = STANDARD_ICON_SIZE
                Row(
                    Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 10.dp)
                        .fillMaxWidth()
                        .glass(ZenithShapes.panel, strength = 1.15f)
                        .padding(vertical = 12.dp, horizontal = 10.dp),
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
                            AppIcon(
                                key = appKey,
                                label = appsByKey[appKey]?.label ?: "",
                                size = dockIcon,
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
}

/** Items are rebuilt with fresh ids whenever a choice changes; this is what they really are. */
private fun stableKey(item: HomeItem): String = when (item) {
    is WidgetCell -> "w:${item.type}"
    is AppCell -> "a:${item.packageName}/${item.className}"
    is FolderCell -> "f:${item.title}"
}
