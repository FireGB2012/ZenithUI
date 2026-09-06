package com.gabriel.zenithui.ui.home

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass

/**
 * The app drawer. The list scrolls at the top, the single-line search bar sits at the bottom
 * where your thumb already is, and anything the apps do not answer goes to Google.
 */
@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    settings: ZenithSettings,
    focusSearch: Boolean,
    /** 0 = fully closed, 1 = fully open. The drawer follows this while you drag. */
    progress: Float,
    onDrag: (Float) -> Unit,
    onDragStopped: (Float) -> Unit,
    onOpen: (String) -> Unit,
    onLongPress: (AppInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val gridState = rememberLazyGridState()

    val filtered = remember(query, apps) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }

    fun searchWeb() {
        if (query.isBlank()) return
        webSearch(context, query.trim())
        onDismiss()
    }

    LaunchedEffect(focusSearch) {
        if (focusSearch) runCatching { focusRequester.requestFocus() }
    }

    // The drawer stays composed between openings, so tidy it up once it is fully closed.
    val closed = progress < 0.01f
    LaunchedEffect(closed) {
        if (closed) {
            query = ""
            runCatching { focusManager.clearFocus(force = true) }
            gridState.scrollToItem(0)
        }
    }
    LaunchedEffect(query) { if (query.isNotEmpty()) gridState.scrollToItem(0) }

    val nestedScroll = remember(progress) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Already part way closed: keep following the finger rather than scrolling.
                if (available.y > 0f && progress < 1f) {
                    onDrag(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // The list is at the top and you are still pulling down: start closing.
                if (available.y > 0f) {
                    onDrag(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (progress < 1f) {
                    onDragStopped(-available.y)
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { translationY = (1f - progress.coerceIn(0f, 1f)) * size.height }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta -> onDrag(delta) },
                onDragStopped = { velocity -> onDragStopped(-velocity) },
            )
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.backdrop.first().copy(alpha = 0.30f),
                        palette.backdrop.last().copy(alpha = 0.80f),
                    )
                )
            )
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(palette.onGlass.copy(alpha = 0.30f))
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta -> onDrag(delta) },
                        onDragStopped = { velocity -> onDragStopped(-velocity) },
                    )
                    .padding(horizontal = 26.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (query.isBlank()) "All apps" else "Results",
                        color = palette.onGlass,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    AnimatedContent(
                        targetState = filtered.size,
                        transitionSpec = {
                            (slideInVertically(Motion.snappy()) { it / 2 } + fadeIn(tween(220)))
                                .togetherWith(slideOutVertically(tween(240)) { -it / 2 } + fadeOut(tween(160)))
                        },
                        label = "count",
                    ) { count ->
                        Text(
                            text = "$count apps",
                            color = palette.onGlassMuted,
                            fontSize = 12.sp,
                        )
                    }
                }
                Box(
                    Modifier
                        .size(38.dp)
                        .glass(RoundedCornerShape(percent = 50), strength = 0.9f)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = palette.onGlass, modifier = Modifier.size(18.dp))
                }
            }

            BoxWithConstraints(Modifier.weight(1f)) {
                val viewportPx = constraints.maxHeight.toFloat()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(settings.columns),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScroll),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    itemsIndexed(filtered, key = { _, app -> app.key }) { index, app ->
                        // Only the first screenful is staggered in. Anything composed while you
                        // are scrolling must land at once, or the list appears to lag behind.
                        val stagger = index < settings.columns * 4 && gridState.firstVisibleItemIndex == 0
                        // Scroll-linked motion: an icon dissolves and shrinks as it slides past
                        // the top or bottom edge, and is left alone once it is fully in view.
                        var itemTop by remember { mutableFloatStateOf(0f) }
                        var itemHeight by remember { mutableFloatStateOf(1f) }
                        Box(
                            Modifier
                                .animateItem()
                                .then(
                                    if (stagger) {
                                        Modifier.appearIn(delayMillis = staggerDelay(index), riseFrom = 26f)
                                    } else {
                                        Modifier
                                    }
                                )
                                .onPlaced { coords ->
                                    itemTop = coords.positionInParent().y
                                    itemHeight = coords.size.height.toFloat().coerceAtLeast(1f)
                                }
                                .graphicsLayer {
                                    val clippedTop = (-itemTop).coerceAtLeast(0f)
                                    val clippedBottom =
                                        (itemTop + itemHeight - viewportPx).coerceAtLeast(0f)
                                    val hidden = (maxOf(clippedTop, clippedBottom) / itemHeight)
                                        .coerceIn(0f, 1f)
                                    val visible = 1f - hidden
                                    alpha = visible
                                    val edgeScale = 0.78f + 0.22f * visible
                                    scaleX = edgeScale
                                    scaleY = edgeScale
                                    transformOrigin = TransformOrigin(0.5f, if (itemTop < 0f) 1f else 0f)
                                }
                                .pointerInput(app.key) {
                                    detectTapGestures(
                                        onTap = { onOpen(app.key) },
                                        onLongPress = { onLongPress(app) },
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            AppTile(
                                key = app.key,
                                label = app.label,
                                iconSize = settings.iconSize.dp,
                                showLabel = true,
                                shape = ZenithShapes.forIconShape(settings.iconShape),
                            )
                        }
                    }
                }

                if (filtered.isEmpty() && query.isNotBlank()) {
                    Column(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "No apps match “${query.trim()}”",
                            color = palette.onGlassMuted,
                            fontSize = 14.sp,
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(
                            Modifier
                                .appearIn(fromScale = 0.9f)
                                .pressBounce(0.95f)
                                .glass(ZenithShapes.pill, strength = 1f, solid = 0.3f)
                                .clickable { searchWeb() }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.TravelExplore,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Search Google for “${query.trim()}”",
                                color = palette.onGlass,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .height(46.dp)
                    .glass(ZenithShapes.pill, strength = 1.1f)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    tint = palette.onGlassMuted,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it.replace("\n", "") },
                    singleLine = true,
                    maxLines = 1,
                    textStyle = TextStyle(color = palette.onGlass, fontSize = 15.sp),
                    cursorBrush = SolidColor(palette.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            val first = filtered.firstOrNull()
                            if (first != null) onOpen(first.key) else searchWeb()
                        },
                    ),
                    interactionSource = remember { MutableInteractionSource() },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) {
                                Text(
                                    "Search apps or the web",
                                    color = palette.onGlass.copy(alpha = 0.35f),
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                )
                            }
                            inner()
                        }
                    },
                )
                if (query.isNotEmpty()) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(palette.onGlass.copy(alpha = 0.12f))
                            .clickable { query = "" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = palette.onGlass, modifier = Modifier.size(13.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .size(38.dp)
                            .appearIn(fromScale = 0.6f, riseFrom = 0f)
                            .pressBounce(0.88f)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(palette.accent)
                            .clickable { searchWeb() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.TravelExplore,
                            contentDescription = "Search the web",
                            tint = androidx.compose.ui.graphics.Color(0xFF0A0B12),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Hands the query to the device's search app, falling back to Google in a browser. */
fun webSearch(context: Context, query: String) {
    val web = Intent(Intent.ACTION_WEB_SEARCH)
        .putExtra(SearchManager.QUERY, query)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val browser = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    runCatching { context.startActivity(web) }
        .onFailure { runCatching { context.startActivity(browser) } }
}
