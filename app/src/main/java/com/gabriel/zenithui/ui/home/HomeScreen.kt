package com.gabriel.zenithui.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.AddHome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ZenithViewModel
import com.gabriel.zenithui.data.AppCell
import com.gabriel.zenithui.data.FolderCell
import com.gabriel.zenithui.data.LayoutBuilder
import com.gabriel.zenithui.data.ROW_UNITS
import com.gabriel.zenithui.data.WidgetCell
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private sealed interface HomeSheet {
    data class Item(val id: String) : HomeSheet
    data class Folder(val id: String) : HomeSheet
    data class DrawerApp(val key: String) : HomeSheet
    data object Customize : HomeSheet
    data object WidgetPicker : HomeSheet
    data object Preferences : HomeSheet
    data object Help : HomeSheet
}

@Composable
fun HomeScreen(viewModel: ZenithViewModel) {
    val palette = LocalPalette.current
    val context = LocalContext.current

    val settings by viewModel.settings.collectAsState()
    val layout by viewModel.layout.collectAsState()
    val apps by viewModel.apps.collectAsState()
    val appsByKey = remember(apps) { apps.associateBy { it.key } }

    val drag = remember { HomeDragState() }
    val scope = rememberCoroutineScope()
    val drawer = remember { Animatable(0f) }
    // While a finger is down the progress is written straight from the drag, with no coroutine
    // in between, so the sheet sits exactly under the thumb.
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var drawerEverOpened by remember { mutableStateOf(false) }
    var focusSearch by remember { mutableStateOf(false) }
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    var resizingId by remember { mutableStateOf<String?>(null) }
    var ambient by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<HomeSheet?>(null) }
    var sheetVisible by remember { mutableStateOf(false) }

    var drawerHeightPx by remember { mutableFloatStateOf(0f) }
    val pageCount = ((layout.maxOfOrNull { it.page } ?: 0) + 1).coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val drawerProgress = if (dragging) dragProgress else drawer.value
    val drawerOpen = drawerProgress > 0.5f

    fun showSheet(next: HomeSheet) {
        sheet = next
        sheetVisible = true
    }

    fun hideSheet() {
        sheetVisible = false
    }

    fun openDrawer(search: Boolean = false) {
        focusSearch = search
        drawerEverOpened = true
        scope.launch {
            if (dragging) drawer.snapTo(dragProgress)
            dragging = false
            drawer.animateTo(1f, Motion.smooth())
        }
    }

    fun closeDrawer() {
        focusSearch = false
        scope.launch {
            if (dragging) drawer.snapTo(dragProgress)
            dragging = false
            drawer.animateTo(0f, Motion.smooth())
        }
    }

    /** Drag in pixels; positive is downward, which closes the drawer. */
    fun dragDrawer(delta: Float) {
        if (drawerHeightPx <= 0f) return
        if (!dragging) {
            dragProgress = drawer.value
            dragging = true
            drawerEverOpened = true
            scope.launch { drawer.stop() }
        }
        dragProgress = (dragProgress - delta / drawerHeightPx).coerceIn(0f, 1f)
    }

    /** On release a fling decides, otherwise the halfway point does. */
    fun settleDrawer(velocity: Float) {
        if (drawerHeightPx <= 0f) return
        val from = if (dragging) dragProgress else drawer.value
        val target = when {
            velocity > 900f -> 1f
            velocity < -900f -> 0f
            from > 0.4f -> 1f
            else -> 0f
        }
        scope.launch {
            drawer.snapTo(from)
            dragging = false
            drawer.animateTo(
                targetValue = target,
                animationSpec = Motion.smooth(),
                initialVelocity = velocity / drawerHeightPx,
            )
            if (target == 0f) focusSearch = false
        }
    }

    fun handleDrop() {
        val item = drag.item
        if (item != null) {
            val hover = drag.hoverItemId
            when {
                drag.removeArmed -> viewModel.removeItem(item.id)
                hover != null -> viewModel.combine(item.id, hover)
                else -> {
                    val rowStep = if (item is WidgetCell) 1 else LayoutBuilder.ICON_ROW_STEP
                    val cell = drag.cellUnderItem(
                        columns = settings.columns,
                        rows = settings.rows * ROW_UNITS,
                        rowStep = rowStep,
                    )
                    if (cell != null) {
                        // Land on the closest opening rather than refusing the drop outright.
                        val slot = viewModel.nearestFreeSlot(
                            page = pagerState.currentPage,
                            col = cell.first,
                            row = cell.second,
                            spanX = item.spanX,
                            spanY = item.spanY,
                            ignoreId = item.id,
                            rowStep = rowStep,
                            colWidthInRowUnits = if (drag.cellHeight > 0f) {
                                drag.cellWidth / drag.cellHeight
                            } else {
                                2f
                            },
                        )
                        if (slot != null) {
                            viewModel.moveItem(item.id, pagerState.currentPage, slot.first, slot.second)
                        }
                    }
                }
            }
        }
        drag.clear()
    }

    var hintDismissed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(sheetVisible) {
        if (!sheetVisible) {
            kotlinx.coroutines.delay(300)
            sheet = null
        }
    }

    val homeBlur by animateDpAsState(
        targetValue = if (drawerProgress > 0.55f) 20.dp else 0.dp,
        animationSpec = tween(420),
        label = "homeBlur",
    )
    val homePressed by viewModel.homePressed.collectAsState()
    val environmentTick by viewModel.environmentTick.collectAsState()
    val isDefaultLauncher = remember(homePressed, environmentTick) { isDefaultLauncher(context) }
    LaunchedEffect(homePressed) {
        if (homePressed > 0) {
            hideSheet()
            closeDrawer()
            if (pagerState.currentPage != 0) pagerState.animateScrollToPage(0)
        }
    }

    BackHandler(enabled = drawerProgress > 0.002f || sheetVisible || resizingId != null || ambient) {
        when {
            ambient -> ambient = false
            sheetVisible -> hideSheet()
            resizingId != null -> resizingId = null
            else -> closeDrawer()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { drawerHeightPx = it.height.toFloat() }
    ) {

        // ------------------------------------------------------------ home
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val scale = 1f - 0.06f * drawerProgress
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - 0.55f * drawerProgress
                }
                // The blur is its own short animation rather than a per-frame effect rebuild:
                // recreating a full screen RenderEffect on every drag frame is what made the
                // drawer feel like it was lagging behind the finger.
                .then(if (homeBlur > 0.5.dp) Modifier.blur(homeBlur) else Modifier)
        ) {
            Column(Modifier.fillMaxSize()) {
                Greeting(
                    name = settings.userName,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 26.dp, vertical = 8.dp),
                )

                Box(
                    Modifier
                        .weight(1f)
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta -> dragDrawer(delta) },
                            onDragStopped = { velocity -> settleDrawer(-velocity) },
                        )
                        .pointerInput(resizingId) {
                            detectTapGestures(
                                onTap = { if (resizingId != null) resizingId = null },
                                onLongPress = { if (resizingId == null) showSheet(HomeSheet.Customize) },
                            )
                        },
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1,
                    ) { page ->
                        val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        HomePage(
                            page = page,
                            active = page == pagerState.currentPage,
                            items = layout.filter { it.page == page },
                            settings = settings,
                            appsByKey = appsByKey,
                            drag = drag,
                            resizingId = resizingId,
                            onResize = { id, spanX, spanY -> viewModel.resizeWidget(id, spanX, spanY) },
                            onOpenApp = { viewModel.open(it) },
                            onOpenFolder = { showSheet(HomeSheet.Folder(it.id)) },
                            onItemMenu = { showSheet(HomeSheet.Item(it.id)) },
                            onSearch = { openDrawer(search = true) },
                            onNoteChange = { viewModel.setNote(it) },
                            onAmbient = { ambient = true },
                            onDrop = { handleDrop() },
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    val distance = abs(offset).coerceIn(0f, 1f)
                                    alpha = 1f - distance * 0.4f
                                    val scale = 0.94f + 0.06f * (1f - distance)
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }

                    if (layout.isEmpty()) {
                        Text(
                            text = "Swipe up to open your apps",
                            color = palette.onGlassMuted,
                            fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                if (!isDefaultLauncher && !hintDismissed) {
                    DefaultLauncherHint(
                        onSetDefault = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        },
                        onDismiss = { hintDismissed = true },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(horizontal = 18.dp),
                    )
                }

                PageDots(
                    count = pageCount,
                    current = pagerState.currentPage,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 10.dp),
                )

                DockBar(
                    keys = settings.dock,
                    appsByKey = appsByKey,
                    settings = settings,
                    onOpen = { viewModel.open(it) },
                    onLongPress = { showSheet(HomeSheet.DrawerApp(it)) },
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 10.dp),
                )
            }
        }

        // ------------------------------------------------------------ drawer
        if (drawerProgress > 0.002f || drawerEverOpened) {
            AppDrawer(
                apps = apps,
                settings = settings,
                focusSearch = focusSearch,
                progress = drawerProgress,
                onDrag = { delta -> dragDrawer(delta) },
                onDragStopped = { velocity -> settleDrawer(velocity) },
                onOpen = {
                    viewModel.open(it)
                    closeDrawer()
                },
                onLongPress = { showSheet(HomeSheet.DrawerApp(it.key)) },
                onDismiss = { closeDrawer() },
            )
        }

        if (resizingId != null) {
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp)
                    .appearIn(fromScale = 0.85f, riseFrom = -18f)
                    .glass(ZenithShapes.pill, strength = 1.2f, solid = 0.4f)
                    .clickable { resizingId = null }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Drag the handles to resize · tap to finish",
                    color = palette.onGlass,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // ------------------------------------------------------------ drag layer
        if (drag.isDragging) {
            // Rest on a neighbour for a moment before a folder is on the cards.
            LaunchedEffect(drag.hoverCandidateId) {
                drag.hoverItemId = null
                val candidate = drag.hoverCandidateId
                if (candidate != null) {
                    kotlinx.coroutines.delay(380)
                    drag.hoverItemId = candidate
                    haptics.performHapticFeedback(
                        androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                    )
                }
            }

            // Hold on the bin for a moment before it will actually take anything.
            LaunchedEffect(drag.overRemove) {
                drag.removeArmed = false
                if (drag.overRemove) {
                    kotlinx.coroutines.delay(450)
                    drag.removeArmed = true
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                }
            }
            RemoveTarget(
                armed = drag.removeArmed,
                hovering = drag.overRemove,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp)
                    .onGloballyPositioned { coords ->
                        val topLeft = coords.positionInRoot()
                        drag.removeZone = androidx.compose.ui.geometry.Rect(
                            offset = topLeft,
                            size = androidx.compose.ui.geometry.Size(
                                coords.size.width.toFloat(),
                                coords.size.height.toFloat(),
                            ),
                        )
                    },
            )
            DragGhost(drag = drag, settings = settings, appsByKey = appsByKey)
        }

        if (ambient) {
            AmbientScreen(userName = settings.userName, onDismiss = { ambient = false })
        }

        // ------------------------------------------------------------ sheets
        when (val current = sheet) {
            is HomeSheet.Item -> {
                val item = layout.firstOrNull { it.id == current.id }
                if (item == null) hideSheet() else ItemMenuSheet(
                    visible = sheetVisible,
                    item = item,
                    appsByKey = appsByKey,
                    inDock = (item as? AppCell)?.key in settings.dock,
                    onOpen = { viewModel.open(it) },
                    onAppInfo = { viewModel.openAppInfo(it) },
                    onToggleDock = { viewModel.toggleDock(it) },
                    onRename = { showSheet(HomeSheet.Folder(it)) },
                    onResize = {
                        resizingId = item.id
                        hideSheet()
                    },
                    onRemove = { viewModel.removeItem(item.id) },
                    onUninstall = { viewModel.uninstall(it) },
                    onDismiss = { hideSheet() },
                )
            }

            is HomeSheet.Folder -> {
                val folder = layout.firstOrNull { it.id == current.id } as? FolderCell
                // Taking the last app out dissolves the folder; close the sheet from an effect,
                // never from the composition itself.
                LaunchedEffect(folder == null) { if (folder == null) hideSheet() }
                if (folder != null) FolderSheet(
                    visible = sheetVisible,
                    folder = folder,
                    appsByKey = appsByKey,
                    settings = settings,
                    onOpen = { viewModel.open(it) },
                    onRemoveApp = { viewModel.removeFromFolder(folder.id, it) },
                    onRename = { viewModel.renameFolder(folder.id, it) },
                    onDismiss = { hideSheet() },
                )
            }

            is HomeSheet.DrawerApp -> {
                val app = appsByKey[current.key]
                DrawerAppSheet(
                    visible = sheetVisible,
                    label = app?.label ?: current.key,
                    pinned = current.key in settings.dock,
                    onOpen = {
                        viewModel.open(current.key)
                        closeDrawer()
                    },
                    onAddToHome = { viewModel.addAppToFirstFreeSlot(current.key) },
                    onToggleDock = { viewModel.toggleDock(current.key) },
                    onAppInfo = { viewModel.openAppInfo(current.key) },
                    onUninstall = { viewModel.uninstall(current.key) },
                    onDismiss = { hideSheet() },
                )
            }

            HomeSheet.Customize -> CustomizeSheet(
                visible = sheetVisible,
                onWallpaper = {
                    runCatching {
                        context.startActivity(
                            Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Wallpaper")
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                },
                onHelp = { showSheet(HomeSheet.Help) },
                onAmbient = {
                    hideSheet()
                    ambient = true
                },
                onWidgets = { showSheet(HomeSheet.WidgetPicker) },
                onSettings = { showSheet(HomeSheet.Preferences) },
                onDefaultLauncher = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                },
                onDismiss = { hideSheet() },
            )

            HomeSheet.WidgetPicker -> WidgetPickerSheet(
                visible = sheetVisible,
                onAdd = { viewModel.addWidget(it) },
                onDismiss = { hideSheet() },
            )

            HomeSheet.Help -> HelpSheet(
                visible = sheetVisible,
                onDismiss = { hideSheet() },
            )

            HomeSheet.Preferences -> SettingsSheet(
                visible = sheetVisible,
                settings = settings,
                onChange = { transform -> viewModel.updateSettings(transform) },
                onReset = { viewModel.resetEverything() },
                onDismiss = { hideSheet() },
            )

            null -> Unit
        }
    }
}

/** Zenith can only read the wallpaper and own the home button once it is the default launcher. */
private fun isDefaultLauncher(context: android.content.Context): Boolean = runCatching {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolved = context.packageManager.resolveActivity(
        intent,
        android.content.pm.PackageManager.MATCH_DEFAULT_ONLY,
    )
    resolved?.activityInfo?.packageName == context.packageName
}.getOrDefault(true)

@Composable
private fun DefaultLauncherHint(
    onSetDefault: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .appearIn(delayMillis = 260)
            .pressBounce(0.97f)
            .glass(ZenithShapes.pill, strength = 1.1f, solid = 0.35f)
            .clickable(onClick = onSetDefault)
            .padding(start = 18.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Home, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Make Zenith your home screen",
            color = palette.onGlass,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(percent = 50))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = palette.onGlassMuted, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun Greeting(name: String, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (name.isBlank()) "Zenith" else name,
            color = palette.onGlass.copy(alpha = 0.85f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { index ->
            val active = index == current
            val width by animateDpAsState(if (active) 22.dp else 6.dp, Motion.bouncy(), label = "dotWidth")
            val color by animateColorAsState(
                if (active) palette.onGlass.copy(alpha = 0.85f) else palette.onGlass.copy(alpha = 0.28f),
                tween(420),
                label = "dotColor",
            )
            Box(
                Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

@Composable
private fun RemoveTarget(armed: Boolean, hovering: Boolean, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val fill by animateColorAsState(
        targetValue = when {
            armed -> Color(0xFFFF6B6B).copy(alpha = 0.55f)
            hovering -> Color(0xFFFF6B6B).copy(alpha = 0.18f)
            else -> Color.Transparent
        },
        animationSpec = tween(450),
        label = "binFill",
    )
    val scale by animateFloatAsState(
        targetValue = if (armed) 1.12f else 1f,
        animationSpec = Motion.bouncy(),
        label = "binScale",
    )
    Row(
        modifier
            .appearIn(fromScale = 0.85f, riseFrom = -18f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .glass(ZenithShapes.pill, strength = 1.2f)
            .background(fill, ZenithShapes.pill)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Delete, contentDescription = null, tint = palette.onGlass, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (armed) "Release to remove" else "Hold to remove",
            color = palette.onGlass,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun DrawerAppSheet(
    visible: Boolean,
    label: String,
    pinned: Boolean,
    onOpen: () -> Unit,
    onAddToHome: () -> Unit,
    onToggleDock: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    ZenithSheet(visible = visible, onDismiss = onDismiss, title = label) {
        MenuRow(Icons.AutoMirrored.Rounded.OpenInNew, "Open") { onOpen(); onDismiss() }
        MenuRow(Icons.Rounded.AddHome, "Add to home screen") { onAddToHome(); onDismiss() }
        MenuRow(Icons.Rounded.PushPin, if (pinned) "Remove from dock" else "Pin to dock") { onToggleDock(); onDismiss() }
        MenuRow(Icons.Rounded.Info, "App info") { onAppInfo(); onDismiss() }
        MenuRow(Icons.Rounded.Delete, "Uninstall", danger = true) { onUninstall(); onDismiss() }
    }
}
