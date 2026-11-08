package com.gabriel.zenithui.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.LayoutBuilder
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.ZenithButton
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.common.unfoldIn
import com.gabriel.zenithui.ui.common.unfoldOut
import com.gabriel.zenithui.ui.onboarding.PhonePreview
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.glass
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val TILE_HEIGHT = 230.dp
private val TILE_GAP = 14.dp

/**
 * Every home page, small, on a tray of glass (pinch out, or the gesture of your choice). Tap one
 * to go to it. Hold one and slide it to move it: the others step aside to show where it will
 * land. Flick one up to take it away — an empty one goes at once, one with things on it asks
 * first. The last tile adds a page.
 */
@Composable
fun PageOverview(
    visible: Boolean,
    pageCount: Int,
    current: Int,
    layout: List<HomeItem>,
    appsByKey: Map<String, AppInfo>,
    settings: ZenithSettings,
    wallpaper: ImageBitmap?,
    screenWidth: Dp,
    screenHeight: Dp,
    onGo: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    val reduced = LocalMotionLevel.current.reduced
    val palette = LocalPalette.current
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = visible, enter = fadeIn(tween(240)), exit = fadeOut(tween(200))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.42f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
            )
        }
        AnimatedVisibility(
            visible = visible,
            // The pages coming down onto the tray from the size they were on the screen.
            enter = if (reduced) fadeIn(tween(Motion.REDUCED_MS)) else fadeIn(tween(200)) + scaleIn(spring(dampingRatio = 0.8f, stiffness = 320f), initialScale = 1.35f),
            exit = if (reduced) fadeOut(tween(Motion.REDUCED_MS)) else fadeOut(tween(180)) + scaleOut(tween(220), targetScale = 1.25f),
            modifier = Modifier.align(Alignment.Center),
        ) {
            var asking by remember { mutableStateOf<Int?>(null) }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Pages", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("Hold one to move it · flick it up to take it away", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                Tray(
                    pageCount = pageCount,
                    current = current,
                    layout = layout,
                    appsByKey = appsByKey,
                    settings = settings,
                    wallpaper = wallpaper,
                    tileWidth = TILE_HEIGHT * (screenWidth / screenHeight),
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    onGo = onGo,
                    onMove = onMove,
                    onFlicked = { page ->
                        if (layout.none { it.page == page }) onRemove(page) else asking = page
                    },
                    onAdd = onAdd,
                )
                AnimatedVisibility(visible = asking != null, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
                    val page = asking ?: 0
                    val things = layout.count { it.page == page }
                    Column(
                        Modifier
                            .padding(top = 16.dp, start = 24.dp, end = 24.dp)
                            .glass(RoundedCornerShape(26.dp), solid = 0.6f)
                            .padding(16.dp),
                    ) {
                        Text(
                            "Page ${page + 1} has ${if (things == 1) "one thing" else "$things things"} on it. Take the page away, and ${if (things == 1) "it" else "them"}?",
                            color = palette.onGlass,
                            fontSize = 14.sp,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ZenithButton(text = "Keep it", onClick = { asking = null }, modifier = Modifier.weight(1f))
                            ZenithButton(text = "Take it away", filled = true, onClick = {
                                onRemove(page)
                                asking = null
                            }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Tray(
    pageCount: Int,
    current: Int,
    layout: List<HomeItem>,
    appsByKey: Map<String, AppInfo>,
    settings: ZenithSettings,
    wallpaper: ImageBitmap?,
    tileWidth: Dp,
    screenWidth: Dp,
    screenHeight: Dp,
    onGo: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onFlicked: (Int) -> Unit,
    onAdd: () -> Unit,
) {
    val palette = LocalPalette.current
    val density = LocalDensity.current
    val tick = rememberTick()
    val step = with(density) { (tileWidth + TILE_GAP).toPx() }
    // The page being carried, how far it has been slid, and so where it would land.
    var carried by remember { mutableStateOf<Int?>(null) }
    var slid by remember { mutableFloatStateOf(0f) }
    val landing = carried?.let { from -> (from + (slid / step).roundToInt()).coerceIn(0, pageCount - 1) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(TILE_GAP),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 30.dp),
    ) {
        for (page in 0 until pageCount) {
            // The others step aside to show where the carried page will go.
            val from = carried
            val aside = when {
                from == null || landing == null || page == from -> 0f
                from < landing && page in (from + 1)..landing -> -step
                from > landing && page in landing until from -> step
                else -> 0f
            }
            val shifted by animateFloatAsState(aside, spring(dampingRatio = 0.75f, stiffness = 420f), label = "pageAside")
            val lifted by animateFloatAsState(if (page == carried) 1.06f else 1f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "pageLift")
            val flick = remember(page, pageCount) { Animatable(0f) }
            val scope = rememberCoroutineScope()
            val ring by animateColorAsState(if (page == current) Color.White else Color.Transparent, tween(220), label = "pageRing")
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .zIndex(if (page == carried) 1f else 0f)
                    .appearIn(delayMillis = staggerDelay(page), riseFrom = 24f)
                    .graphicsLayer {
                        translationX = if (page == carried) slid else shifted
                        translationY = flick.value
                        alpha = (1f + flick.value / (size.height * 0.8f)).coerceIn(0f, 1f)
                        scaleX = lifted
                        scaleY = lifted
                    }
                    .pointerInput(page, pageCount) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                tick()
                                carried = page
                                slid = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                slid += amount.x
                            },
                            onDragEnd = {
                                val to = landing
                                carried = null
                                slid = 0f
                                if (to != null && to != page) {
                                    tick()
                                    onMove(page, to)
                                }
                            },
                            onDragCancel = {
                                carried = null
                                slid = 0f
                            },
                        )
                    }
                    .pointerInput(page, pageCount) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, amount ->
                                change.consume()
                                scope.launch { flick.snapTo((flick.value + amount).coerceAtMost(0f)) }
                            },
                            onDragEnd = {
                                scope.launch {
                                    val far = flick.value < -size.height * 0.3f
                                    if (far && pageCount > 1) {
                                        tick()
                                        onFlicked(page)
                                    }
                                    flick.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 380f))
                                }
                            },
                        )
                    },
            ) {
                Box(
                    Modifier
                        .size(width = tileWidth + 8.dp, height = TILE_HEIGHT + 8.dp)
                        .border(2.dp, ring, RoundedCornerShape(26.dp))
                        .pressBounce(0.95f)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onGo(page) },
                    contentAlignment = Alignment.Center,
                ) {
                    PhonePreview(
                        items = layout.filter { it.page == page },
                        dock = emptyList(),
                        appsByKey = appsByKey,
                        settings = settings,
                        wallpaper = wallpaper,
                        screenWidth = screenWidth,
                        screenHeight = screenHeight,
                        shown = Int.MAX_VALUE,
                        deviceCorner = 30.dp,
                        modifier = Modifier.size(width = tileWidth, height = TILE_HEIGHT),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "${page + 1}",
                    color = Color.White.copy(alpha = if (page == current) 1f else 0.7f),
                    fontSize = 13.sp,
                    fontWeight = if (page == current) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
        if (pageCount < LayoutBuilder.MAX_PAGES) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.appearIn(delayMillis = staggerDelay(pageCount), riseFrom = 24f)) {
                Box(
                    Modifier
                        .size(width = tileWidth * 0.7f, height = TILE_HEIGHT * 0.7f)
                        .pressBounce(0.92f)
                        .clip(RoundedCornerShape(22.dp))
                        .glass(RoundedCornerShape(22.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            tick()
                            onAdd()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add a page", tint = palette.onGlass, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text("New", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            }
        }
    }
}
