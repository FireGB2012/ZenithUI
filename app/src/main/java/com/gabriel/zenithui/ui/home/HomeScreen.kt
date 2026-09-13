package com.gabriel.zenithui.ui.home

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ZenithViewModel
import com.gabriel.zenithui.data.AppCell
import com.gabriel.zenithui.data.Backup
import com.gabriel.zenithui.data.FolderCell
import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.LayoutBuilder
import com.gabriel.zenithui.data.ROW_UNITS
import com.gabriel.zenithui.data.WidgetCell
import com.gabriel.zenithui.data.findActivity
import com.gabriel.zenithui.ui.common.LaunchOrigin
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * How close to the side of the screen a dragged item has to be to turn the page. Wide enough to
 * hit with a thumb that is already holding something, since you cannot see your finger under it.
 */
private val PAGE_FLIP_EDGE = 60.dp

/** How far the home screen sinks back while an app opens out of it. */
private const val HOME_SUNK = 0.9f

/** Coming home longer than this after opening an app does not shrink it back into its icon. */
private const val CLOSE_WINDOW_MS = 30 * 60 * 1000L

/** How long it has to stay there, so carrying an item past an edge does not flip anything. */
private const val PAGE_FLIP_DELAY = 240L

private sealed interface HomeSheet {
    data class Item(val id: String) : HomeSheet
    data class Folder(val id: String) : HomeSheet
    data class DrawerApp(val key: String) : HomeSheet
    data object Customize : HomeSheet
    data object WidgetPicker : HomeSheet
    data object Preferences : HomeSheet
    data object Help : HomeSheet
    data object Plus : HomeSheet
    data object Aura : HomeSheet
    data object ThemeMaker : HomeSheet
    data object HiddenApps : HomeSheet
    data object Toggles : HomeSheet
}

@Composable
fun HomeScreen(viewModel: ZenithViewModel) {
    val palette = LocalPalette.current
    val context = LocalContext.current

    val settings by viewModel.settings.collectAsState()
    val layout by viewModel.layout.collectAsState()
    val apps by viewModel.apps.collectAsState()
    val appsByKey = remember(apps) { apps.associateBy { it.key } }
    val plus by viewModel.plus.collectAsState()
    val checkIn by viewModel.checkIn.collectAsState()
    // Only true the first time the home screen appears after setup.
    val entrance = remember { viewModel.consumeHomeEntrance() }

    // Counted once a day, on the way back to the launcher — which is every time you unlock the
    // phone, so the strip lands on the first look of the morning and never again that day.
    LaunchedEffect(settings.onboarded, viewModel.homePressed.collectAsState().value) {
        viewModel.checkInForToday()
        viewModel.ensureReferralCode()
    }

    // Hiding an app is a Plus feature, so when Plus lapses the apps come back rather than staying
    // locked away — nobody loses sight of an app because a trial ran out.
    val drawerApps = remember(apps, settings.hiddenApps, plus.active) {
        if (!plus.active || settings.hiddenApps.isEmpty()) apps
        else apps.filterNot { it.key in settings.hiddenApps }
    }

    // Biggest groups first; the catch-alls go last so the top of the library has actual groups.
    // A group of one is mostly empty glass, so lone apps are folded into Other instead.
    val libraryGroups: List<LibraryGroup>? = remember(drawerApps, settings.appsView, settings.recentLaunches) {
        if (settings.appsView != "library") return@remember null
        val byKind = drawerApps.groupBy { viewModel.repository.libraryCategory(it.key) }
        val loners = byKind.filterValues { it.size < 2 }.values.flatten()
        val kinds = byKind.filterValues { it.size >= 2 }
            .let { kept -> if (loners.isEmpty()) kept else kept + ("Other" to (kept["Other"].orEmpty() + loners).sortedBy { it.label.lowercase() }) }
            .map { (name, members) -> LibraryGroup(name, members) }
            .sortedWith(compareBy({ it.name == "Utilities" || it.name == "Other" }, { -it.apps.size }))

        // The two iOS shelves on top. Both are extra: an app in them still has its own folder.
        val byKey = drawerApps.associateBy { it.key }
        val suggestions = settings.recentLaunches.mapNotNull { byKey[it] }.take(8)
        // Apps that came with the phone were all "installed" the day it was made, so they are
        // left out; otherwise this would just be the factory apps in a random order.
        val recentlyAdded = drawerApps
            .filterNot { it.system }
            .map { it to viewModel.repository.installedAt(it.key) }
            .filter { it.second > 0L }
            .sortedByDescending { it.second }
            .take(8)
            .map { it.first }

        buildList<LibraryGroup> {
            if (suggestions.isNotEmpty()) add(LibraryGroup("Suggestions", suggestions))
            if (recentlyAdded.isNotEmpty()) add(LibraryGroup("Recently Added", recentlyAdded))
            addAll(kinds)
        }
    }

    val saveBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(Backup.MIME)
    ) { uri ->
        if (uri != null) {
            val ok = Backup.write(context, uri, viewModel.backupText())
            Toast.makeText(context, if (ok) "Home screen backed up" else "Could not write that file", Toast.LENGTH_SHORT).show()
        }
    }
    // Some file pickers report a .json backup as a generic binary, so the filter is deliberately
    // wide: a backup you cannot find is worse than a picker that lists too much.
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = Backup.read(context, uri)
            val ok = text != null && viewModel.restoreBackup(text)
            Toast.makeText(context, if (ok) "Home screen restored" else "That is not a Zenith backup", Toast.LENGTH_SHORT).show()
        }
    }

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
    var homeOrigin by remember { mutableStateOf(Offset.Zero) }
    val homeFocus = androidx.compose.ui.platform.LocalFocusManager.current
    var drawerWidthPx by remember { mutableFloatStateOf(0f) }
    // The App Library lives past the last page, like on iOS; the drawer lives below.
    val libraryMode = settings.appsView == "library"
    // A page exists either because something is on it or because you asked for it.
    val pageCount = maxOf((layout.maxOfOrNull { it.page } ?: 0) + 1, settings.pages)
        .coerceIn(1, LayoutBuilder.MAX_PAGES)
    val pagerState = rememberPagerState(pageCount = { pageCount })
    var pagerBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var dockBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    // A hidden dock must not keep catching drops at the place it used to be.
    LaunchedEffect(settings.showDock) {
        if (!settings.showDock) dockBounds = androidx.compose.ui.geometry.Rect.Zero
    }
    // Only a single app goes in the dock; a folder or widget carried over it is ignored.
    val overDock = drag.item is AppCell && !dockBounds.isEmpty && dockBounds.contains(drag.pointer)
    val edgeFlipPx = with(LocalDensity.current) { PAGE_FLIP_EDGE.toPx() }
    val drawerProgress = if (dragging) dragProgress else drawer.value
    val drawerOpen = drawerProgress > 0.5f

    fun showSheet(next: HomeSheet) {
        sheet = next
        sheetVisible = true
    }

    /** Runs a Plus feature, or explains itself first. */
    fun withPlus(action: () -> Unit) {
        if (plus.active) action() else showSheet(HomeSheet.Plus)
    }

    fun hideSheet() {
        sheetVisible = false
    }

    val hostView = androidx.compose.ui.platform.LocalView.current
    val hostLifecycle = remember(context) { (context.findActivity() as? androidx.lifecycle.LifecycleOwner)?.lifecycle }

    /**
     * The one card an app opens and closes through. It is never replaced mid-flight: opening sends
     * [cardProgress] towards 1 and closing towards 0, and a spring carries it from wherever it is
     * with the speed it already has. That is what makes opening and closing an app over and over
     * one smooth, unbroken motion instead of a string of restarts.
     */
    var card by remember { mutableStateOf<AppExpansion?>(null) }
    val cardProgress = remember { Animatable(0f) }
    /** Heading open (true) or back into the icon (false). */
    var cardOpening by remember { mutableStateOf(false) }
    /** When the app was asked to open, or 0 while it has not been. */
    val launchedAt = remember { LongArray(1) }
    val cardJob = remember { arrayOfNulls<kotlinx.coroutines.Job>(1) }
    var preparingLaunch by remember { mutableStateOf(false) }

    fun steerCard(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {
        cardJob[0]?.cancel()
        cardJob[0] = scope.launch(block = block)
    }

    /** Sends the card back into its icon from wherever it is, and stops the app opening if it has not yet. */
    fun closeCard() {
        if (card == null) return
        cardOpening = false
        steerCard {
            cardProgress.animateTo(0f, launchSpring())
            card = null
            launchedAt[0] = 0L
        }
    }

    /** Sends the card towards the whole screen, asking the app to open on the way. */
    fun openCard(target: AppExpansion) {
        card = target
        cardOpening = true
        steerCard {
            // The app is asked to open partway up, so it is already on its way as the card fills.
            launch {
                snapshotFlow { cardProgress.value }.first { it >= LAUNCH_AT }
                launchedAt[0] = android.os.SystemClock.uptimeMillis()
                viewModel.open(
                    target.key,
                    LaunchOrigin.toScreen(hostView, target.from),
                    android.app.ActivityOptions
                        .makeCustomAnimation(context, com.gabriel.zenithui.R.anim.zenith_app_enter, 0)
                        .toBundle(),
                )
            }
            cardProgress.animateTo(1f, launchSpring())
            // The app should now cover Zenith. If Zenith is still in front, it never opened: come back.
            delay(900)
            if (hostLifecycle?.currentState?.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) == true) {
                cardOpening = false
                cardProgress.animateTo(0f, launchSpring())
                card = null
                launchedAt[0] = 0L
            }
        }
    }

    /**
     * Opens an app the way iOS and One UI do: the icon grows until it fills the screen and the app
     * appears over it. Tapping the same icon while its card is still moving turns the card round
     * rather than starting again. An app opened any other way simply opens.
     */
    fun launchApp(key: String) {
        val current = card
        if (current != null && current.key == key) {
            openCard(current)
            return
        }
        val from = LaunchOrigin.boundsFor(key)
        if (from == null) {
            viewModel.open(key)
            return
        }
        if (preparingLaunch) return
        // The icon is fetched first (it is almost always cached already), so the card never
        // starts out as a blank dark shape while its picture loads.
        preparingLaunch = true
        scope.launch {
            val icon = runCatching { viewModel.icons.load(key, 192) }.getOrNull()
            preparingLaunch = false
            // A different app starts from its own icon.
            if (card != null) {
                cardJob[0]?.cancel()
                cardProgress.snapTo(0f)
            }
            launchedAt[0] = 0L
            openCard(AppExpansion(key = key, from = from, icon = icon))
        }
    }

    androidx.compose.runtime.DisposableEffect(hostLifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event != androidx.lifecycle.Lifecycle.Event.ON_START) return@LifecycleEventObserver
            // A stopped window keeps its last picture; redraw straight away on the way back.
            hostView.invalidate()
            // Coming back from an app opened from its icon: the card is still over the screen, so it
            // shrinks back into that icon while the home screen comes forward, the way iOS closes
            // an app. It picks up from wherever it was, even if the app had only just opened.
            val recent = launchedAt[0] != 0L &&
                android.os.SystemClock.uptimeMillis() - launchedAt[0] < CLOSE_WINDOW_MS
            if (card != null && recent) {
                closeCard()
            } else if (card != null && !cardOpening) {
                cardJob[0]?.cancel()
                scope.launch { cardProgress.snapTo(0f) }
                card = null
            }
        }
        hostLifecycle?.addObserver(observer)
        onDispose { hostLifecycle?.removeObserver(observer) }
    }

    // Parallax for the real wallpaper: tell Android how far across the pages we are, so a
    // wallpaper wider than the screen slides slowly behind them, as on any stock launcher.
    val wallpaperManager = remember(context) { android.app.WallpaperManager.getInstance(context) }
    LaunchedEffect(pagerState, pageCount) {
        snapshotFlow { pagerState.currentPage + pagerState.currentPageOffsetFraction }.collect { position ->
            val token = hostView.windowToken ?: return@collect
            val last = (pageCount - 1).coerceAtLeast(1)
            runCatching {
                wallpaperManager.setWallpaperOffsetSteps(1f / last, 1f)
                wallpaperManager.setWallpaperOffsets(token, if (pageCount > 1) (position / last).coerceIn(0f, 1f) else 0.5f, 0.5f)
            }
        }
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
        val span = if (libraryMode) drawerWidthPx else drawerHeightPx
        if (span <= 0f) return
        if (!dragging) {
            dragProgress = drawer.value
            dragging = true
            drawerEverOpened = true
            scope.launch { drawer.stop() }
        }
        dragProgress = (dragProgress - delta / span).coerceIn(0f, 1f)
    }

    /** On release a fling decides, otherwise the halfway point does. */
    fun settleDrawer(velocity: Float) {
        val span = if (libraryMode) drawerWidthPx else drawerHeightPx
        if (span <= 0f) return
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
                initialVelocity = velocity / span,
            )
            if (target == 0f) focusSearch = false
        }
    }

    // In library mode the pager hands over whatever it cannot use: a swipe left from the last page
    // has nowhere to go, so it pulls the library in, and it follows the finger both ways from there.
    val libraryPull = remember(libraryMode) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!libraryMode || !dragging || available.x <= 0f) return Offset.Zero
                dragDrawer(available.x)
                return Offset(available.x, 0f)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (!libraryMode || source != NestedScrollSource.UserInput || available.x >= 0f) return Offset.Zero
                dragDrawer(available.x)
                return Offset(available.x, 0f)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!libraryMode || !dragging) return Velocity.Zero
                settleDrawer(-available.x)
                return available
            }
        }
    }

    /**
     * How finely an icon may be placed: every half row normally, every quarter row with Plus free
     * placement on. Read live, because the drag gesture keeps the callbacks it started with.
     */
    fun iconRowStep(): Int =
        if (settings.freeform && plus.active) 1 else LayoutBuilder.ICON_ROW_STEP

    /**
     * Where an item dragged over [col], [row] would actually come to rest — the closest opening,
     * since a drop onto a taken cell lands beside it rather than being refused. The hint and the
     * drop itself both resolve through here, so the hint can never point at a cell the item will
     * not take.
     */
    fun dropSlotFor(item: HomeItem, col: Int, row: Int): Pair<Int, Int>? =
        viewModel.nearestFreeSlot(
            page = pagerState.currentPage,
            col = col,
            row = row,
            spanX = item.spanX,
            spanY = item.spanY,
            ignoreId = item.id,
            rowStep = if (item is WidgetCell) 1 else iconRowStep(),
            colWidthInRowUnits = if (drag.cellHeight > 0f) drag.cellWidth / drag.cellHeight else 2f,
        )

    fun handleDrop() {
        val item = drag.item
        if (item != null) {
            val hover = drag.hoverItemId
            // Worked out here from live state, not from [overDock]: the page's drag gesture holds
            // on to the drop callback it started with, so a value captured then would be stale.
            val droppedOnDock = item is AppCell && !dockBounds.isEmpty && dockBounds.contains(drag.pointer)
            when {
                droppedOnDock -> {
                    // Where along the bar it was let go decides its place among the others.
                    val slots = settings.dock.size + 1
                    val index = ((drag.pointer.x - dockBounds.left) / dockBounds.width * slots).toInt()
                    if (!viewModel.moveToDock(item.id, index)) {
                        Toast.makeText(context, "The dock is full — drag one out first", Toast.LENGTH_SHORT).show()
                    }
                }
                drag.removeArmed -> viewModel.removeItem(item.id)
                hover != null -> viewModel.combine(item.id, hover)
                else -> {
                    val rowStep = if (item is WidgetCell) 1 else iconRowStep()
                    // Mid-turn the page under your finger is halfway across the screen, so the
                    // cell it reports is meaningless. Fall through to the first free slot instead.
                    val settled = abs(pagerState.currentPageOffsetFraction) < 0.01f
                    val cell = if (!settled) null else drag.cellUnderItem(
                        columns = settings.columns,
                        rows = settings.rows * ROW_UNITS,
                        rowStep = rowStep,
                    )
                    // Letting go off the grid leaves an item where it was — unless it has been
                    // carried to another page, where dropping it back on the old one would look
                    // like the launcher had swallowed it.
                    val slot = cell?.let { dropSlotFor(item, it.first, it.second) }
                        ?: if (!settled || item.page != pagerState.currentPage) {
                            viewModel.freeSlotOnPage(
                                page = pagerState.currentPage,
                                spanX = item.spanX,
                                spanY = item.spanY,
                                rowStep = rowStep,
                                ignoreId = item.id,
                            )
                        } else {
                            null
                        }
                    if (slot != null) {
                        val free = settings.freeform && plus.active
                        if (free && cell != null && slot == cell && drag.cellWidth > 0f && drag.cellHeight > 0f) {
                            // Free placement keeps the exact spot you let go of, not just the cell
                            // it rounds to. Only when it landed in that cell — if it had to move
                            // aside for a neighbour, an offset from the finger would mean nothing.
                            val topLeft = drag.pointer - drag.grab
                            val exactCol = (topLeft.x - drag.gridOrigin.x) / drag.cellWidth
                            val exactRow = (topLeft.y - drag.gridOrigin.y) / drag.cellHeight
                            viewModel.placeItemFreely(
                                id = item.id,
                                page = pagerState.currentPage,
                                col = slot.first,
                                row = slot.second,
                                dx = (exactCol - slot.first).coerceIn(-0.49f, 0.49f),
                                dy = (exactRow - slot.second).coerceIn(-0.49f, 0.49f),
                            )
                        } else {
                            viewModel.moveItem(item.id, pagerState.currentPage, slot.first, slot.second)
                        }
                    }
                }
            }
        }
        drag.clear()
    }

    /**
     * Turns to the page something has just been put on, so you never have to go looking for it.
     * A page added this moment is not in the pager yet, hence the wait.
     */
    fun revealPage(page: Int?) {
        if (page == null) return
        scope.launch {
            snapshotFlow { pagerState.pageCount }.first { it > page }
            pagerState.animateScrollToPage(page)
        }
    }

    /**
     * Which way a page turn is being asked for: carrying an item to the very edge of the screen
     * flips to the page beside it, and pushing past the last page makes a new one. Without this
     * an item could never leave the page it started on.
     */
    val flipDirection = when {
        !drag.isDragging || pagerBounds.isEmpty -> 0
        drag.pointer.x < pagerBounds.left + edgeFlipPx -> -1
        drag.pointer.x > pagerBounds.right - edgeFlipPx -> 1
        else -> 0
    }
    /**
     * A column is wider than the edge strip, so an icon in the first or last column is already
     * inside it the moment you pick it up — and holding still would turn the page under you, and
     * then keep turning. The edge only counts once you have carried the item away from it, which
     * is exactly what taking something to the side of the screen means.
     */
    var flipArmed by remember { mutableStateOf(false) }

    // One coroutine for the whole time you hold at the edge, and the only place the arming is
    // decided — a second effect would not re-run when this one's keys are unchanged, which is how
    // the edge went dead after the first drag. It must also not be keyed on the current page: a
    // page turn changes that halfway through its own animation, which would cancel the scroll and
    // leave the pager parked between two pages.
    LaunchedEffect(flipDirection, drag.isDragging) {
        if (!drag.isDragging) {
            // Every pick-up starts disarmed, so an icon in the outer column sits still.
            flipArmed = false
            return@LaunchedEffect
        }
        if (flipDirection == 0) {
            flipArmed = true
            return@LaunchedEffect
        }
        if (!flipArmed) return@LaunchedEffect
        while (true) {
            // Long enough that merely sweeping past the edge does not turn the page.
            delay(PAGE_FLIP_DELAY)
            val next = pagerState.currentPage + flipDirection
            if (next < 0) break
            if (next >= pagerState.pageCount) {
                // Past the last page: make one, and wait for the pager to hear about it.
                viewModel.addPage() ?: break
                snapshotFlow { pagerState.pageCount }.first { it > next }
            }
            pagerState.animateScrollToPage(next)
        }
    }

    // Letting go always leaves a whole page on screen, never two halves.
    LaunchedEffect(drag.isDragging) {
        if (!drag.isDragging && pagerState.currentPageOffsetFraction != 0f) {
            pagerState.animateScrollToPage(pagerState.currentPage)
        }
    }

    // A page you added but left empty is forgotten once you swipe away from it.
    LaunchedEffect(pagerState.settledPage, drag.isDragging) {
        if (!drag.isDragging) viewModel.trimPages(pagerState.settledPage)
    }

    var hintDismissed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(sheetVisible) {
        if (!sheetVisible) {
            kotlinx.coroutines.delay(300)
            sheet = null
        }
    }

    val homeBlur by animateDpAsState(
        // The liquid glass opening frosts the home screen behind the growing glass.
        targetValue = if (drawerProgress > 0.55f || (card != null && settings.openAnimation == "glass")) 20.dp else 0.dp,
        // A quick frost in and out, so opening and closing apps never feels held up by it.
        animationSpec = tween(110),
        label = "homeBlur",
    )
    val homePressed by viewModel.homePressed.collectAsState()
    val environmentTick by viewModel.environmentTick.collectAsState()
    val isDefaultLauncher = remember(homePressed, environmentTick) { isDefaultLauncher(context) }
    LaunchedEffect(homePressed) {
        if (homePressed > 0) {
            // Home while an app is still opening sends it straight back into its icon.
            if (card != null && cardOpening) closeCard()
            hideSheet()
            closeDrawer()
            if (pagerState.currentPage != 0) pagerState.animateScrollToPage(0)
        }
    }

    BackHandler(enabled = (card != null && cardOpening) || drawerProgress > 0.002f || sheetVisible || resizingId != null || ambient) {
        when {
            card != null && cardOpening -> closeCard()
            ambient -> ambient = false
            sheetVisible -> hideSheet()
            resizingId != null -> resizingId = null
            else -> closeDrawer()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged {
                drawerHeightPx = it.height.toFloat()
                drawerWidthPx = it.width.toFloat()
            }
            .onGloballyPositioned { homeOrigin = it.positionInRoot() }
            // Watches every touch before anything else handles it, and never consumes one. While
            // a note is being typed in, a touch anywhere outside it ends the typing and puts the
            // keyboard away — the way a text field on any other screen behaves.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val typing = HomeTypingArea.bounds
                    if (typing != null && !typing.contains(down.position + homeOrigin)) {
                        HomeTypingArea.bounds = null
                        homeFocus.clearFocus(force = true)
                    }
                }
            }
    ) {

        // ------------------------------------------------------------ home
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // The home screen sinks back as the card grows and comes forward as it shrinks,
                    // on the very same spring, so both always move together.
                    val zoom = 1f - (1f - HOME_SUNK) * cardProgress.value.coerceIn(0f, 1f)
                    val scale = (1f - 0.06f * drawerProgress) * zoom
                    scaleX = scale
                    scaleY = scale
                    val sunk = ((1f - zoom) / (1f - HOME_SUNK)).coerceIn(0f, 1f)
                    alpha = (1f - 0.55f * drawerProgress) * (1f - 0.45f * sunk)
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
                            // With the library on the side, swiping up is left alone.
                            enabled = !libraryMode,
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
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(libraryPull)
                            .onGloballyPositioned { coords ->
                                val topLeft = coords.positionInRoot()
                                pagerBounds = androidx.compose.ui.geometry.Rect(
                                    offset = topLeft,
                                    size = androidx.compose.ui.geometry.Size(
                                        coords.size.width.toFloat(),
                                        coords.size.height.toFloat(),
                                    ),
                                )
                            },
                        // While something is in the air every page stays composed, so the gesture
                        // carrying it survives being scrolled off screen.
                        beyondViewportPageCount = if (drag.isDragging) LayoutBuilder.MAX_PAGES else 1,
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
                            onOpenApp = { launchApp(it) },
                            onOpenFolder = { showSheet(HomeSheet.Folder(it.id)) },
                            onItemMenu = { showSheet(HomeSheet.Item(it.id)) },
                            onSearch = { openDrawer(search = true) },
                            onNoteChange = { viewModel.setNote(it) },
                            onAmbient = { ambient = true },
                            onDrop = { handleDrop() },
                            resolveDrop = { item, col, row -> dropSlotFor(item, col, row) },
                            iconRowStep = iconRowStep(),
                            freePlacement = settings.freeform && plus.active,
                            animateEntrance = entrance,
                            pageOffset = offset,
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
                            text = if (libraryMode) "Swipe left to open your apps" else "Swipe up to open your apps",
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
                    canAdd = pageCount < LayoutBuilder.MAX_PAGES,
                    onAddPage = { revealPage(viewModel.addPage()) },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 10.dp),
                )

                if (settings.showDock) DockBar(
                    keys = settings.dock,
                    appsByKey = appsByKey,
                    settings = settings,
                    highlight = overDock,
                    animateEntrance = entrance,
                    columns = settings.columns,
                    pageIconSize = drag.iconSize,
                    onOpen = { launchApp(it) },
                    onLongPress = { showSheet(HomeSheet.DrawerApp(it)) },
                    onReorder = { from, to -> viewModel.moveInDock(from, to) },
                    onDragOut = { key, point ->
                        // The finger's cell on the page in view, in the same units the grid uses.
                        val col = if (drag.cellWidth > 0f) ((point.x - drag.gridOrigin.x) / drag.cellWidth).toInt() else 0
                        val row = if (drag.cellHeight > 0f) ((point.y - drag.gridOrigin.y) / drag.cellHeight).toInt() else 0
                        revealPage(
                            viewModel.moveDockToHome(
                                key = key,
                                page = pagerState.currentPage,
                                col = col,
                                row = row,
                                colWidthInRowUnits = if (drag.cellHeight > 0f) drag.cellWidth / drag.cellHeight else 2f,
                            )
                        )
                    },
                    onBounds = { dockBounds = it },
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 10.dp),
                ) else {
                    // Without the dock the pages reach down to the gesture bar instead.
                    Spacer(Modifier.navigationBarsPadding().height(10.dp))
                }
            }
        }

        // ------------------------------------------------------------ drawer
        if (drawerProgress > 0.002f || drawerEverOpened) {
            AppDrawer(
                apps = drawerApps,
                groups = libraryGroups,
                horizontal = libraryMode,
                settings = settings,
                focusSearch = focusSearch,
                progress = drawerProgress,
                onDrag = { delta -> dragDrawer(delta) },
                onDragStopped = { velocity -> settleDrawer(velocity) },
                onOpen = {
                    launchApp(it)
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

        StreakStrip(
            state = checkIn,
            onShown = { viewModel.clearCheckIn() },
            modifier = Modifier.align(Alignment.TopCenter),
        )

        if (ambient) {
            AmbientScreen(userName = settings.userName, onDismiss = { ambient = false })
        }

        card?.let { moving ->
            AppLaunchCard(
                card = moving,
                progress = { cardProgress.value },
                glass = settings.openAnimation == "glass",
                catchTaps = cardOpening,
                onTap = { closeCard() },
            )
        }

        // ------------------------------------------------------------ sheets
        when (val current = sheet) {
            is HomeSheet.Item -> {
                val item = layout.firstOrNull { it.id == current.id }
                if (item == null) hideSheet() else ItemMenuSheet(
                    visible = sheetVisible,
                    item = item,
                    appsByKey = appsByKey,
                    onOpen = { launchApp(it) },
                    onAppInfo = { viewModel.openAppInfo(it) },
                    onIconSize = { spanX ->
                        if (!viewModel.resizeApp(item.id, spanX)) {
                            Toast.makeText(context, "No room for that size on this page", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onChooseToggles = { showSheet(HomeSheet.Toggles) },
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
                    onOpen = { launchApp(it) },
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
                        launchApp(current.key)
                        closeDrawer()
                    },
                    onAddToHome = {
                        // The app may well have gone onto a page you are not looking at, so put
                        // the drawer away and take you to it.
                        revealPage(viewModel.addAppToFirstFreeSlot(current.key))
                        closeDrawer()
                    },
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
                onAdd = { type ->
                    val page = viewModel.addWidget(type)
                    revealPage(page)
                    page != null
                },
                onDismiss = { hideSheet() },
            )

            HomeSheet.Help -> HelpSheet(
                visible = sheetVisible,
                onDismiss = { hideSheet() },
            )

            HomeSheet.Preferences -> SettingsSheet(
                visible = sheetVisible,
                settings = settings,
                plus = plus,
                onChange = { transform -> viewModel.updateSettings(transform) },
                onAura = { showSheet(HomeSheet.Aura) },
                onFreeform = { on ->
                    // Turning it on is the Plus part; turning it off is always allowed.
                    if (on && !plus.active) showSheet(HomeSheet.Plus)
                    else viewModel.updateSettings { it.copy(freeform = on) }
                },
                onThemeMaker = { withPlus { showSheet(HomeSheet.ThemeMaker) } },
                onHiddenApps = { withPlus { showSheet(HomeSheet.HiddenApps) } },
                onBackup = { withPlus { saveBackup.launch(Backup.fileName()) } },
                onRestore = { withPlus { openBackup.launch(arrayOf("*/*")) } },
                onPlus = { showSheet(HomeSheet.Plus) },
                onReset = { viewModel.resetEverything() },
                onDismiss = { hideSheet() },
            )

            HomeSheet.Plus -> PlusSheet(
                visible = sheetVisible,
                status = plus,
                onStartTrial = {
                    viewModel.startTrialViaPlayOrLocally(context as? android.app.Activity)
                },
                onBuyLifetime = {
                    val activity = context as? android.app.Activity
                    val started = activity != null && viewModel.buyLifetime(activity)
                    if (!started) {
                        Toast.makeText(context, "Play could not open the purchase just now", Toast.LENGTH_SHORT).show()
                    }
                },
                onSubscribe = {
                    val activity = context as? android.app.Activity
                    val started = activity != null && viewModel.subscribe(activity)
                    if (!started) {
                        Toast.makeText(context, "Play could not open the subscription just now", Toast.LENGTH_SHORT).show()
                    }
                },
                onRestore = {
                    viewModel.refreshPurchases()
                    Toast.makeText(context, "Checking your Play purchases…", Toast.LENGTH_SHORT).show()
                },
                onRedeem = { code ->
                    val ok = viewModel.redeemCode(code)
                    if (ok) Toast.makeText(context, "Zenith Plus unlocked", Toast.LENGTH_SHORT).show()
                    ok
                },
                onDismiss = { hideSheet() },
            )

            HomeSheet.Toggles -> TogglesSheet(
                visible = sheetVisible,
                selected = settings.toggles,
                onChange = { chosen -> viewModel.updateSettings { it.copy(toggles = chosen) } },
                onDismiss = { hideSheet() },
            )

            HomeSheet.Aura -> AuraSheet(
                visible = sheetVisible,
                settings = settings,
                onUnlock = { id -> viewModel.unlockTheme(id) },
                onApply = { id -> viewModel.updateSettings { it.copy(paletteId = id) } },
                onRedeem = { code -> viewModel.redeemReferral(code) },
                onDismiss = { hideSheet() },
            )

            HomeSheet.ThemeMaker -> ThemeMakerSheet(
                visible = sheetVisible,
                settings = settings,
                onChange = { transform -> viewModel.updateSettings(transform) },
                onDismiss = { hideSheet() },
            )

            HomeSheet.HiddenApps -> HiddenAppsSheet(
                visible = sheetVisible,
                apps = apps,
                settings = settings,
                onToggle = { key -> viewModel.toggleHidden(key) },
                onSetPin = { pin -> viewModel.setHiddenPin(pin) },
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
private fun PageDots(
    count: Int,
    current: Int,
    canAdd: Boolean,
    onAddPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
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

        // The one control that makes another page. It sits where the next dot would appear, so
        // it reads as "one more of these" rather than as a button bolted on.
        if (canAdd) {
            Box(
                Modifier
                    .padding(start = 2.dp)
                    .size(18.dp)
                    .clip(RoundedCornerShape(50))
                    .background(palette.onGlass.copy(alpha = 0.12f))
                    .pressBounce()
                    .clickable { onAddPage() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add page",
                    tint = palette.onGlass.copy(alpha = 0.55f),
                    modifier = Modifier.size(12.dp),
                )
            }
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
        // Apps go into the dock by dragging them there; the menu only offers the way back out.
        if (pinned) MenuRow(Icons.Rounded.PushPin, "Remove from dock") { onToggleDock(); onDismiss() }
        MenuRow(Icons.Rounded.Info, "App info") { onAppInfo(); onDismiss() }
        MenuRow(Icons.Rounded.Delete, "Uninstall", danger = true) { onUninstall(); onDismiss() }
    }
}
