package com.gabriel.zenithui.ui.home

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import com.gabriel.zenithui.data.HostedSizes
import com.gabriel.zenithui.data.HostedWidgetCell
import com.gabriel.zenithui.data.StackCell
import com.gabriel.zenithui.data.StackMember
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.SectionLabel
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.common.unfoldIn
import com.gabriel.zenithui.ui.common.unfoldOut
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** The host other apps' widgets are drawn through, for whatever on the home screen draws one. */
val LocalWidgetHost = staticCompositionLocalOf<AppWidgetHost?> { null }

/**
 * Another app's widget, in its frame on the page: drawn by that app, through Android's widget
 * host, at exactly the size of its cells, with a pane of glass behind it if asked for.
 *
 * Only one view may draw a given widget at a time, so a copy of it — the grey one under a finger,
 * the small home screen in Settings — is [live] false and shows the widget's own preview picture
 * instead. One that did not come through a restore onto this phone offers to put itself back.
 */
@Composable
fun HostedWidget(
    cell: HostedWidgetCell,
    live: Boolean,
    onRestore: (AppWidgetProviderInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val manager = remember(context) { AppWidgetManager.getInstance(context) }
    // Asked again whenever the widget's number changes: a restored one gets a new number once it is put back.
    val info = remember(cell.appWidgetId, cell.provider) {
        manager.getAppWidgetInfo(cell.appWidgetId)?.takeIf { it.provider.flattenToString() == cell.provider }
    }
    Box(
        modifier
            .then(if (cell.glass) Modifier.glass(ZenithShapes.tile, strength = 1.1f, interactive = true) else Modifier)
            .clip(ZenithShapes.tile),
    ) {
        when {
            info == null -> MissingWidget(provider = cell.provider, onRestore = onRestore)
            !live -> WidgetPicture(info)
            else -> LiveHostedView(cell.appWidgetId, info)
        }
    }
}

/** The widget itself, as its app draws it, told its size in dp whenever that changes. */
@Composable
private fun LiveHostedView(appWidgetId: Int, info: AppWidgetProviderInfo) {
    val host = LocalWidgetHost.current
    if (host == null) {
        WidgetPicture(info)
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = maxWidth.value
        val height = maxHeight.value
        // A new view for a new number: the host keeps one view for each widget it draws.
        key(appWidgetId) {
            AndroidView(
                factory = { ctx -> host.createView(ctx, appWidgetId, info) },
                update = { view -> view.tellSize(width, height) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Tells the widget the room it has, so it can lay itself out for it, and only when that changes. */
private fun AppWidgetHostView.tellSize(widthDp: Float, heightDp: Float) {
    val size = widthDp to heightDp
    if (tag == size || widthDp <= 0f || heightDp <= 0f) return
    tag = size
    runCatching {
        if (Build.VERSION.SDK_INT >= 31) {
            updateAppWidgetSize(Bundle(), listOf(SizeF(widthDp, heightDp)))
        } else {
            @Suppress("DEPRECATION")
            updateAppWidgetSize(null, widthDp.toInt(), heightDp.toInt(), widthDp.toInt(), heightDp.toInt())
        }
    }
}

/** What a widget looks like, from the picture its app gives of it, or its icon when there is none. */
@Composable
private fun WidgetPicture(info: AppWidgetProviderInfo) {
    val context = LocalContext.current
    val picture by produceState<ImageBitmap?>(initialValue = null, info.provider) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val density = context.resources.displayMetrics.densityDpi
                (info.loadPreviewImage(context, density) ?: info.loadIcon(context, density))
                    ?.let { drawable ->
                        val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 256
                        val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 256
                        val scale = minOf(1f, 640f / maxOf(w, h))
                        drawable.toBitmap((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1)).asImageBitmap()
                    }
            }.getOrNull()
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        picture?.let { Image(it, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(6.dp)) }
    }
}

/**
 * A widget that is not on this phone in the way it was: brought by a backup from another, or one
 * whose app has gone. If its app is here it can be put back with a tap, in the same place.
 */
@Composable
private fun MissingWidget(provider: String, onRestore: (AppWidgetProviderInfo) -> Unit) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val found = remember(provider) {
        runCatching {
            AppWidgetManager.getInstance(context).installedProviders.firstOrNull { it.provider.flattenToString() == provider }
        }.getOrNull()
    }
    val app = remember(found) {
        found?.let { runCatching { it.loadLabel(context.packageManager) }.getOrNull() }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(palette.onGlass.copy(alpha = 0.08f))
            .then(if (found != null) Modifier.clickable { onRestore(found) } else Modifier)
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.Widgets, contentDescription = null, tint = palette.onGlassMuted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            if (found != null) "Tap to put ${app ?: "this widget"} back" else "This widget's app is not on this phone",
            color = palette.onGlassMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 3,
        )
    }
}

/**
 * A stack of widgets: one at a time, a swipe up or down for the next, the way the Smart Stacks on
 * an iPhone go. Small dots at its edge say how many there are and which is showing; they come up
 * while it is being swiped and fade a moment after it settles. [content] draws each member.
 */
@Composable
fun WidgetStack(
    stack: StackCell,
    onShown: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable (StackMember) -> Unit,
) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    val pager = rememberPagerState(pageCount = { stack.members.size })
    var dotsUp by remember { mutableStateOf(false) }
    LaunchedEffect(pager) {
        snapshotFlow { pager.isScrollInProgress }.collect { moving ->
            if (moving) dotsUp = true else {
                onShown(pager.currentPage)
                delay(1200)
                dotsUp = false
            }
        }
    }
    val dots by animateFloatAsState(if (dotsUp) 1f else 0f, if (reduced) tween(Motion.REDUCED_MS) else tween(260), label = "stackDots")
    Box(modifier) {
        VerticalPager(state = pager, modifier = Modifier.fillMaxSize(), key = { stack.members[it].id }) { page ->
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // The one leaving shrinks back a little and dims, so the next reads as coming over it.
                        if (!reduced) {
                            val off = (pager.currentPage - page + pager.currentPageOffsetFraction).coerceIn(-1f, 1f)
                            val back = kotlin.math.abs(off)
                            val s = 1f - 0.06f * back
                            scaleX = s
                            scaleY = s
                            alpha = 1f - 0.35f * back
                        }
                    },
            ) {
                content(stack.members[page])
            }
        }
        if (stack.members.size > 1) {
            Column(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .graphicsLayer { alpha = 0.3f + 0.7f * dots },
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(stack.members.size) { index ->
                    val near = 1f - kotlin.math.abs(pager.currentPage + pager.currentPageOffsetFraction - index).coerceIn(0f, 1f)
                    Box(
                        Modifier
                            .width(4.dp)
                            .height(4.dp + 6.dp * near)
                            .clip(RoundedCornerShape(50))
                            .background(palette.onGlass.copy(alpha = 0.35f + 0.55f * near)),
                    )
                }
            }
        }
    }
}

/**
 * Picking up something that has touches of its own inside it — another app's widget, a stack
 * that pages. It is watched from above without taking anything, so the widget inside still gets
 * its taps and scrolls; a finger that stays still long enough is a pick-up, and from then on the
 * touch is the carry's and what is inside is told it was cancelled.
 */
internal suspend fun PointerInputScope.holdToCarry(
    onStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    /** Let go, with how far it was carried in all. */
    onEnd: (Float) -> Unit,
    onCancel: () -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val slop = viewConfiguration.touchSlop
        var at = down.position
        // Still held when the time runs out: a lift, or a move past the slop, before then is not.
        var held = true
        withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            while (true) {
                val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed || (change.position - down.position).getDistance() > slop) break
                at = change.position
            }
            held = false
        }
        if (!held) return@awaitEachGesture
        onStart(at)
        var travelled = 0f
        while (true) {
            val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
            if (change == null) {
                onCancel()
                return@awaitEachGesture
            }
            change.consume()
            if (!change.pressed) {
                onEnd(travelled)
                return@awaitEachGesture
            }
            val delta = change.position - change.previousPosition
            travelled += delta.getDistance()
            onDrag(delta)
        }
    }
}

// ------------------------------------------------------------------------------- the gallery

/** One app's widgets, for the gallery. */
private data class ProviderGroup(val packageName: String, val app: String, val providers: List<AppWidgetProviderInfo>)

/**
 * The gallery's "From your apps": every app on the phone that has widgets, A to Z, each opening
 * out to its widgets in a row — each one's picture, its name and the room it takes on this grid.
 * A tap on one puts it on the home screen.
 */
@Composable
fun HostedWidgetPicker(
    cellDp: Float,
    rowDp: Float,
    columns: Int,
    rows: Int,
    modifier: Modifier = Modifier,
    onPick: (AppWidgetProviderInfo) -> Unit,
) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val reduced = LocalMotionLevel.current.reduced
    val groups by produceState(initialValue = emptyList<ProviderGroup>()) {
        value = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            runCatching { AppWidgetManager.getInstance(context).installedProviders }.getOrNull().orEmpty()
                .filter { it.provider.packageName != context.packageName }
                .groupBy { it.provider.packageName }
                .map { (pkg, providers) ->
                    val app = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
                    ProviderGroup(pkg, app, providers.sortedBy { runCatching { it.loadLabel(pm) }.getOrDefault("") })
                }
                .sortedBy { it.app.lowercase() }
        }
    }
    var open by remember { mutableStateOf<String?>(null) }

    Column(modifier) {
        SectionLabel("From your apps", Modifier.padding(start = 4.dp, bottom = 6.dp))
        Text(
            "Widgets from the apps on your phone, drawn by those apps, on Zenith's grid.",
            color = palette.onGlassMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp),
        )
        if (groups.isEmpty()) {
            Text("No app on this phone offers a widget.", color = palette.onGlassMuted, fontSize = 13.sp, modifier = Modifier.padding(4.dp))
        }
        Column(
            Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(palette.onGlass.copy(alpha = 0.06f)),
        ) {
            groups.forEachIndexed { index, group ->
                val expanded = open == group.packageName
                val turn by animateFloatAsState(if (expanded) 180f else 0f, if (reduced) tween(Motion.REDUCED_MS) else Motion.snappy(), label = "providerTurn")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .appearIn(delayMillis = staggerDelay(index), riseFrom = 16f, gentle = true)
                        .clickable { open = if (expanded) null else group.packageName }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val key = remember(group.packageName) {
                        context.packageManager.getLaunchIntentForPackage(group.packageName)?.component?.let { "${it.packageName}/${it.className}" }
                    }
                    if (key != null) com.gabriel.zenithui.ui.common.AppIcon(key = key, label = group.app, size = 34.dp)
                    else Spacer(Modifier.size(34.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(group.app, color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (group.providers.size == 1) "1 widget" else "${group.providers.size} widgets",
                            color = palette.onGlassMuted,
                            fontSize = 12.sp,
                        )
                    }
                    Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = palette.onGlassMuted, modifier = Modifier.graphicsLayer { rotationZ = turn })
                }
                androidx.compose.animation.AnimatedVisibility(visible = expanded, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    ) {
                        items(group.providers, key = { it.provider.flattenToString() }) { info ->
                            ProviderCard(info, cellDp, rowDp, columns, rows) { onPick(info) }
                        }
                    }
                }
            }
        }
    }
}

/** One widget to choose: its picture on a little well, its name, and how many cells it takes here. */
@Composable
private fun ProviderCard(info: AppWidgetProviderInfo, cellDp: Float, rowDp: Float, columns: Int, rows: Int, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val name = remember(info) { runCatching { info.loadLabel(context.packageManager) }.getOrDefault("Widget") }
    val (spanX, spanY) = remember(info, cellDp, rowDp, columns, rows) {
        val targetX = if (Build.VERSION.SDK_INT >= 31) info.targetCellWidth else 0
        val targetY = if (Build.VERSION.SDK_INT >= 31) info.targetCellHeight else 0
        HostedSizes.spanFor(info.minWidth, info.minHeight, targetX, targetY, cellDp, rowDp, columns, rows)
    }
    Column(
        Modifier
            .width(150.dp)
            .appearIn(fromScale = 0.9f, riseFrom = 14f, gentle = true)
            .pressBounce(0.96f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(palette.onGlass.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            WidgetPicture(info)
        }
        Spacer(Modifier.height(6.dp))
        Text(name, color = palette.onGlass, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("$spanX × ${spanY / com.gabriel.zenithui.data.ROW_UNITS}", color = palette.onGlassMuted, fontSize = 12.sp)
    }
}
