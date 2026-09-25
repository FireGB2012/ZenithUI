package com.gabriel.zenithui.ui.home

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.layout.layout
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
import com.gabriel.zenithui.ui.common.AppIcon
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.launchOrigin
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.theme.GlassMaterial
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import kotlin.math.roundToInt

/**
 * The app drawer. The list scrolls at the top, the single-line search bar sits at the bottom
 * where your thumb already is, and anything the apps do not answer goes to Google.
 */
@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    /** Set when swiping up should show the App Library instead of one long list. */
    groups: List<LibraryGroup>?,
    /** Slides in from the side, the way the iOS App Library does, instead of from below. */
    horizontal: Boolean = false,
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

    // In the library, the group whose small stack you tapped. Searching always spans every app.
    var openGroup by remember { mutableStateOf<String?>(null) }
    val shownGroup = if (query.isBlank()) groups?.firstOrNull { it.name == openGroup } else null
    val shelves = groups != null && query.isBlank() && shownGroup == null

    val filtered = remember(query, apps, shownGroup) {
        when {
            shownGroup != null -> shownGroup.apps
            query.isBlank() -> apps
            else -> apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
        }
    }

    // Composed after the launcher's own handler, so back steps out of a group before closing.
    BackHandler(enabled = shownGroup != null) { openGroup = null }
    LaunchedEffect(openGroup) { gridState.scrollToItem(0) }

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
            openGroup = null
            runCatching { focusManager.clearFocus(force = true) }
            gridState.scrollToItem(0)
        }
    }
    LaunchedEffect(query) { if (query.isNotEmpty()) gridState.scrollToItem(0) }
    // Scrolling the results means you are looking, not typing: give the screen back.
    LaunchedEffect(gridState.isScrollInProgress) {
        if (gridState.isScrollInProgress) runCatching { focusManager.clearFocus() }
    }

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
            // Moved while placing rather than on a graphics layer: glass inside the drawer lines
            // its wallpaper up with where it really is, and a layer move would not tell it.
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    val hidden = 1f - progress.coerceIn(0f, 1f)
                    if (horizontal) {
                        placeable.place((hidden * placeable.width).roundToInt(), 0)
                    } else {
                        placeable.place(0, (hidden * placeable.height).roundToInt())
                    }
                }
            }
            .draggable(
                orientation = if (horizontal) Orientation.Horizontal else Orientation.Vertical,
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
            // A tap anywhere that is not an app puts the keyboard away, as it does on iOS.
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            // The grab handle only makes sense for a sheet that is pulled down to close.
            if (horizontal) Spacer(Modifier.height(13.dp)) else Box(
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
                        orientation = if (horizontal) Orientation.Horizontal else Orientation.Vertical,
                        state = rememberDraggableState { delta -> onDrag(delta) },
                        onDragStopped = { velocity -> onDragStopped(-velocity) },
                    )
                    .padding(horizontal = 26.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = when {
                            query.isNotBlank() -> "Results"
                            shownGroup != null -> shownGroup.name
                            groups != null -> "App Library"
                            else -> "All apps"
                        },
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
                        .glass(RoundedCornerShape(percent = 50), strength = 0.9f, material = GlassMaterial.Clear)
                        .clickable { if (shownGroup != null) openGroup = null else onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (shownGroup != null) Icons.AutoMirrored.Rounded.ArrowBack else Icons.Rounded.Close,
                        contentDescription = if (shownGroup != null) "Back to App Library" else "Close",
                        tint = palette.onGlass,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            BoxWithConstraints(Modifier.weight(1f)) {
                val viewportPx = constraints.maxHeight.toFloat()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (shelves) 2 else settings.columns),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        // Pulling the list past its top closes a sheet from below; one from the
                        // side closes with a sideways swipe instead.
                        .then(if (horizontal) Modifier else Modifier.nestedScroll(nestedScroll)),
                    contentPadding = PaddingValues(horizontal = if (shelves) 22.dp else 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (shelves) 18.dp else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    if (shelves && groups != null) {
                        itemsIndexed(groups, key = { _, group -> "group:${group.name}" }) { index, group ->
                            LibraryCard(
                                group = group,
                                settings = settings,
                                onOpen = onOpen,
                                onLongPress = {
                                    focusManager.clearFocus()
                                    onLongPress(it)
                                },
                                onExpand = { openGroup = group.name },
                                modifier = Modifier
                                    .animateItem()
                                    .then(
                                        if (index < 6 && gridState.firstVisibleItemIndex == 0) {
                                            Modifier.appearIn(delayMillis = staggerDelay(index), riseFrom = 26f)
                                        } else {
                                            Modifier
                                        }
                                    ),
                            )
                        }
                    } else itemsIndexed(filtered, key = { _, app -> app.key }) { index, app ->
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
                                .launchOrigin(app.key)
                                .pointerInput(app.key) {
                                    detectTapGestures(
                                        onTap = { onOpen(app.key) },
                                        onLongPress = {
                                            focusManager.clearFocus()
                                            onLongPress(app)
                                        },
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            AppTile(
                                key = app.key,
                                label = app.label,
                                iconSize = STANDARD_ICON_SIZE,
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
                                .glass(ZenithShapes.pill, strength = 1f, material = GlassMaterial.Clear)
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

/** One folder in the App Library: a kind of app, and every app of that kind. */
@Immutable
data class LibraryGroup(val name: String, val apps: List<AppInfo>)

/**
 * A library folder, drawn the iOS way: the first three apps at full size so they open in one tap,
 * and the rest as a small stack in the fourth corner that opens the whole group.
 */
@Composable
private fun LibraryCard(
    group: LibraryGroup,
    settings: ZenithSettings,
    onOpen: (String) -> Unit,
    onLongPress: (AppInfo) -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val shape = ZenithShapes.forIconShape(settings.iconShape)
    // Exactly four fit without a stack; any more and the fourth corner becomes the way in.
    val lead = if (group.apps.size <= 4) group.apps else group.apps.take(3)
    val rest = if (group.apps.size <= 4) emptyList() else group.apps.drop(3)

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .glass(RoundedCornerShape(26.dp), strength = 0.9f, material = GlassMaterial.Clear)
                .padding(12.dp),
        ) {
            val gap = 10.dp
            val cell = (maxWidth - gap) / 2
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                for (r in 0 until 2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        for (c in 0 until 2) {
                            val i = r * 2 + c
                            Box(Modifier.size(cell), contentAlignment = Alignment.Center) {
                                if (i < lead.size) {
                                    val app = lead[i]
                                    AppIcon(
                                        key = app.key,
                                        label = app.label,
                                        size = cell * 0.9f,
                                        shape = shape,
                                        modifier = Modifier
                                            .pressBounce()
                                            .launchOrigin(app.key)
                                            .pointerInput(app.key) {
                                                detectTapGestures(
                                                    onTap = { onOpen(app.key) },
                                                    onLongPress = { onLongPress(app) },
                                                )
                                            },
                                    )
                                } else if (i == 3 && rest.isNotEmpty()) {
                                    MiniStack(apps = rest, size = cell, shape = shape, onClick = onExpand)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = group.name,
            color = palette.onGlass,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

/** Four tiny icons standing in for the rest of a group. Tapping it opens the group. */
@Composable
private fun MiniStack(
    apps: List<AppInfo>,
    size: androidx.compose.ui.unit.Dp,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    val gap = 4.dp
    val inset = 5.dp
    val small = (size - inset * 2 - gap) / 2
    Box(
        Modifier
            .size(size)
            .pressBounce(0.92f)
            .clip(RoundedCornerShape(16.dp))
            .background(palette.onGlass.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(inset),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            for (r in 0 until 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (c in 0 until 2) {
                        val app = apps.getOrNull(r * 2 + c)
                        if (app != null) {
                            AppIcon(key = app.key, label = app.label, size = small, shape = shape)
                        } else {
                            Spacer(Modifier.size(small))
                        }
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
