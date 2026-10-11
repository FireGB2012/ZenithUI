package com.gabriel.zenithui.ui.home

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ZenithViewModel
import com.gabriel.zenithui.data.AnimationTuning
import com.gabriel.zenithui.data.AppCell
import com.gabriel.zenithui.data.Backup
import com.gabriel.zenithui.data.DropPlanner
import com.gabriel.zenithui.data.FolderCell
import com.gabriel.zenithui.data.HomeHeader
import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.LaunchMotion
import com.gabriel.zenithui.data.LayoutBuilder
import com.gabriel.zenithui.data.ROW_UNITS
import com.gabriel.zenithui.data.Tune
import com.gabriel.zenithui.data.WidgetCell
import com.gabriel.zenithui.data.findActivity
import com.gabriel.zenithui.ui.common.LaunchOrigin
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.edgeIn
import com.gabriel.zenithui.ui.common.edgeOut
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.wordsChange
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.rememberHaptics
import com.gabriel.zenithui.ui.settings.SettingsActions
import com.gabriel.zenithui.ui.settings.SettingsPreview
import com.gabriel.zenithui.ui.settings.SettingsScreen
import com.gabriel.zenithui.ui.theme.LocalWallpaperOffset
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import com.gabriel.zenithui.data.server.ServerConfig
import com.gabriel.zenithui.ui.settings.AccountActions

/**
 * How close to the side of the glass the finger itself has to be to turn the page, for items too
 * wide to carry half off it. Anything narrower turns the page once its middle passes the edge of
 * the grid, which is easy to do with a thumb that cannot see past what it is holding.
 */
private val PAGE_FLIP_EDGE = 16.dp

/** How far the home screen sinks back while an app opens out of it. */
/** Coming home longer than this after opening an app does not shrink it back into its icon. */
private const val CLOSE_WINDOW_MS = 30 * 60 * 1000L

/** How long it has to stay there, so carrying an item past an edge does not flip anything. */
private const val PAGE_FLIP_DELAY = 240L

/**
 * How long the carried item rests over a spot before whatever is there steps aside for it: long
 * enough that carrying it across the page shuffles nothing, short enough to feel like it answers.
 */
private const val REORDER_DELAY = 280L

/** The space between the edge of the screen and the page, the dock and everything else on it. */
private val PAGE_MARGIN = 12.dp

/**
 * The room left round a bar of glass that fades as it comes and goes. While it fades it is drawn
 * through a layer of its own, which cuts off whatever reaches past its edges: the shadow the glass
 * casts, and the bin itself when it swells.
 */
private val BAR_ROOM = 28.dp

private sealed interface HomeSheet {
    data class Item(val id: String) : HomeSheet
    data class DrawerApp(val key: String) : HomeSheet
    data object Customize : HomeSheet
    data object Help : HomeSheet
    data object Plus : HomeSheet
    data object ThemeMaker : HomeSheet
    data object HiddenApps : HomeSheet
    data object Toggles : HomeSheet
    data object Wallpapers : HomeSheet
}

@Composable
fun HomeScreen(viewModel: ZenithViewModel) {
    val wallpaperOffset = remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    CompositionLocalProvider(LocalWallpaperOffset provides wallpaperOffset) {
        HomeContent(viewModel, wallpaperOffset)
    }
}

@Composable
private fun HomeContent(viewModel: ZenithViewModel, wallpaperOffset: MutableState<Offset>) {
    val palette = LocalPalette.current
    val context = LocalContext.current

    val settings by viewModel.settings.collectAsState()
    val layout by viewModel.layout.collectAsState()
    val apps by viewModel.apps.collectAsState()
    val appsByKey = remember(apps) { apps.associateBy { it.key } }
    val plus by viewModel.plus.collectAsState()
    val signed by viewModel.account.collectAsState()
    val checkIn by viewModel.checkIn.collectAsState()
    // Only true the first time the home screen appears after setup.
    val entrance = remember { viewModel.consumeHomeEntrance() }
    // Straight after setup's hand-off the page must match the preview it replaced, so the "make
    // Zenith your home app" hint holds back a moment and then eases in, moving the grid gently.
    val handedOff = remember { viewModel.consumeSetupHandoff() }
    var hintAllowed by remember { mutableStateOf(!handedOff) }
    LaunchedEffect(Unit) {
        if (!hintAllowed) {
            kotlinx.coroutines.delay(900)
            hintAllowed = true
        }
    }

    // Counted once a day, on the way back to the launcher — which is every time you unlock the
    // phone, so the strip lands on the first look of the morning and never again that day.
    LaunchedEffect(settings.onboarded, viewModel.homePressed.collectAsState().value) {
        viewModel.checkInForToday()
        viewModel.ensureReferralCode()
        // How a friend using your code reaches this phone. Nothing is sent unless somebody is signed in.
        viewModel.refreshAccount()
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
    val motion = LocalMotionLevel.current
    var resizingId by remember { mutableStateOf<String?>(null) }
    var ambient by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<HomeSheet?>(null) }
    var sheetVisible by remember { mutableStateOf(false) }
    // Settings is a page of its own, under the sheets: one opened from it closes back onto it.
    var settingsOpen by remember { mutableStateOf(false) }
    // So is the widget gallery.
    var widgetsOpen by remember { mutableStateOf(false) }
    // The home screen fades out from under either of them.
    val pageShown = animateFloatAsState(if (settingsOpen || widgetsOpen) 1f else 0f, tween(280), label = "pageShown")
    val glassLevels by viewModel.glassLevels.collectAsState()

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
    // Only a single app goes in the dock; a folder or widget carried over it is ignored. Derived,
    // so the whole screen is not recomposed for every pixel the finger moves.
    val overDock by remember {
        derivedStateOf { drag.item is AppCell && !dockBounds.isEmpty && dockBounds.contains(drag.pointer) }
    }
    val overRemove by remember { derivedStateOf { drag.overRemove } }
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

    // A folder open out of its icon: which one and where its icon is, and how far open it is.
    var folderUp by remember { mutableStateOf<Pair<String, androidx.compose.ui.geometry.Rect?>?>(null) }
    val folderProgress = remember { Animatable(0f) }
    val folderJob = remember { arrayOfNulls<kotlinx.coroutines.Job>(1) }

    fun openFolder(id: String, from: androidx.compose.ui.geometry.Rect?) {
        hideSheet()
        folderUp = id to from
        folderJob[0]?.cancel()
        folderJob[0] = scope.launch {
            if (motion.reduced) folderProgress.animateTo(1f, tween(Motion.REDUCED_MS)) else folderProgress.animateTo(1f, Motion.open())
        }
    }

    /** Back into its icon; only then is it gone, so the home screen is never left dimmed behind nothing. */
    fun closeFolder() {
        if (folderUp == null) return
        folderJob[0]?.cancel()
        folderJob[0] = scope.launch {
            if (motion.reduced) folderProgress.animateTo(0f, tween(Motion.REDUCED_MS)) else folderProgress.animateTo(0f, Motion.close())
            folderUp = null
        }
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
    /**
     * How far the card's middle has travelled across and down, while [cardProgress] is how far it
     * has grown. Each is on a spring of its own, which is what lets the card take a curve.
     */
    val cardX = remember { Animatable(0f) }
    val cardY = remember { Animatable(0f) }
    /** Pixels to a dp, for the distances in the tuning. */
    val pxPerDp = LocalDensity.current.density
    /** The numbers all of it runs on: the ones chosen in Settings, or calmer ones with movement reduced. */
    val tuning = if (motion.reduced) AnimationTuning.Reduced else settings.animation
    val liveTuning by androidx.compose.runtime.rememberUpdatedState(tuning)
    /** A card on show that is shrinking back into its icon after an app has closed, not one you can turn round. */
    var closingCard by remember { mutableStateOf(false) }

    /** Sends the card's three springs towards the icon (0) or the screen (1), and waits for them to settle. */
    suspend fun flyCard(to: Float) = kotlinx.coroutines.coroutineScope {
        val t = liveTuning
        launch { cardX.animateTo(to, t.springOf(Tune.OpenXStiffness, Tune.OpenXDamping)) }
        launch { cardY.animateTo(to, t.springOf(Tune.OpenYStiffness, Tune.OpenYDamping)) }
        cardProgress.animateTo(to, t.springOf(Tune.OpenSizeStiffness, Tune.OpenSizeDamping))
    }

    /** Puts all three at [at] between two frames, with nothing seen of it. */
    suspend fun restCard(at: Float = 0f) {
        cardX.snapTo(at)
        cardY.snapTo(at)
        cardProgress.snapTo(at)
    }
    /** When the app was asked to open, or 0 while it has not been. */
    val launchedAt = remember { LongArray(1) }
    /**
     * How far the home screen is sunk back while no card is on show: 1 while an app covers Zenith,
     * springing to 0 as you come home. The card's own progress drives the sink while it is moving.
     */
    val homeSink = remember { Animatable(0f) }
    /** The app that covered Zenith when it was stopped, so its icon can land when we come back. */
    val returningKey = remember { arrayOfNulls<String>(1) }
    /** Its card as it was opened, for the icon's place on the page and for a closing card. */
    val returningCard = remember { arrayOfNulls<AppExpansion>(1) }
    /**
     * Holds the home screen fully sunk from the moment an app covers Zenith until it starts coming
     * forward. A plain state write, not an animation, so it is already true for the very first frame
     * Android shows on the way home — no flash of the full-size screen before it sinks.
     */
    var holdSunk by remember { mutableStateOf(false) }

    // Arriving. One clock for the whole home screen: at 0 everything is out from the middle, at
    // FlyIn.TOTAL_MS it is all in its place. It is wound back while nobody can see — at a cold
    // start, or when Zenith is stopped — and let run once the home screen is on show again.
    val liveMotion by androidx.compose.runtime.rememberUpdatedState(motion)
    val flyOnStart = remember { !entrance && !motion.reduced }
    val arrivalClock = remember { Animatable(if (flyOnStart) 0f else FlyIn.TOTAL_MS.toFloat()) }
    val arrivalDepth = remember { mutableFloatStateOf(FlyIn.FULL) }
    val arrivalPending = remember { BooleanArray(1).also { it[0] = flyOnStart } }
    val power = remember(context) { context.getSystemService(android.os.PowerManager::class.java) }

    fun arrive() {
        if (!arrivalPending[0]) return
        arrivalPending[0] = false
        scope.launch {
            arrivalClock.snapTo(0f)
            arrivalClock.animateTo(
                FlyIn.TOTAL_MS.toFloat(),
                tween(FlyIn.TOTAL_MS, easing = androidx.compose.animation.core.LinearEasing),
            )
        }
    }
    LaunchedEffect(Unit) {
        // A cold start: once the first frame is up.
        androidx.compose.runtime.withFrameNanos { }
        arrive()
    }
    // The drawer is put together a moment after the home screen has arrived, out of sight, rather
    // than under the first finger that asks for it. Built then, it cost the first swipe up its
    // first frames, and on a slow phone the swipe itself: the drawer never came.
    LaunchedEffect(Unit) {
        delay(FlyIn.TOTAL_MS + 700L)
        drawerEverOpened = true
    }

    /** Heading open (true) or back into the icon (false). */
    var cardOpening by remember { mutableStateOf(false) }
    val cardJob = remember { arrayOfNulls<kotlinx.coroutines.Job>(1) }
    var preparingLaunch by remember { mutableStateOf(false) }

    fun steerCard(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {
        cardJob[0]?.cancel()
        cardJob[0] = scope.launch(block = block)
    }

    /** The app's icon arriving back in its place, and the icons round it knocked back by it. */
    suspend fun landIcon(key: String, at: androidx.compose.ui.geometry.Rect?) = kotlinx.coroutines.coroutineScope {
        val t = liveTuning
        val ripple = t[Tune.Ripple] * pxPerDp
        if (at != null && ripple > 0f) {
            // The ripple swings on the landing's own spring, a little slacker, so it trails it.
            launch {
                HomeRipple.knock(at.center, ripple, t.stiffness(Tune.LandStiffness) * 0.6f, t[Tune.LandDamping])
            }
        }
        AppLanding.land(key, t[Tune.LandScale], t[Tune.LandFade], t.springOf(Tune.LandStiffness, Tune.LandDamping))
    }

    /**
     * The half of closing an app that a launcher is allowed to draw: the home screen comes forward
     * again and the app's icon lands in its place. With a closing card
     * asked for, a card the app's colour shrinks back into the icon first.
     */
    fun comeForward() {
        val key = returningKey[0] ?: return
        returningKey[0] = null
        val back = returningCard[0]
        returningCard[0] = null
        val recent = launchedAt[0] != 0L &&
            android.os.SystemClock.uptimeMillis() - launchedAt[0] < CLOSE_WINDOW_MS
        launchedAt[0] = 0L
        val t = liveTuning
        // Everything zooms, and ripples, about the icon the app was opened from.
        if (back != null) HomeRipple.origin = back.from.center
        scope.launch {
            // Snap to exactly where the hold leaves off before letting go of it, so the hand-over
            // from holding to animating happens between two frames with nothing visible in it.
            homeSink.snapTo(1f)
            holdSunk = false
            homeSink.animateTo(0f, t.springOf(Tune.ReturnStiffness, Tune.ReturnDamping))
        }
        if (!recent) return
        val from = t[Tune.CloseCard]
        if (from <= 0.005f || back == null) {
            scope.launch { landIcon(key, back?.from) }
            return
        }
        // The icon stays out of sight until the card has shrunk onto it, then lands.
        AppLanding.hold(key)
        card = back
        cardOpening = false
        closingCard = true
        steerCard {
            try {
                restCard(from)
                val flight = launch { flyCard(0f) }
                snapshotFlow { cardProgress.value }.first { it <= 0.03f }
                flight.cancel()
                card = null
                closingCard = false
                restCard()
                landIcon(key, back.from)
            } finally {
                // Turned round or cut short: never leave an icon hidden, or a card that cannot be tapped.
                AppLanding.hold(null)
                closingCard = false
            }
        }
    }

    /** How far back the home screen is: 0 at rest, 1 behind an app. Read while drawing. */
    fun homeDepth(): Float = when {
        card != null && !closingCard -> cardProgress.value.coerceIn(0f, 1f)
        holdSunk -> 1f
        else -> homeSink.value.coerceIn(-0.12f, 1f)
    }

    /** Sends the card back into its icon from wherever it is, and stops the app opening if it has not yet. */
    fun closeCard() {
        if (card == null) return
        cardOpening = false
        steerCard {
            flyCard(0f)
            card = null
            launchedAt[0] = 0L
        }
    }

    /** Sends the card towards the whole screen, asking the app to open on the way. */
    fun openCard(target: AppExpansion) {
        card = target
        cardOpening = true
        closingCard = false
        // The icons nearest it make way, and the home screen zooms about it.
        HomeRipple.origin = target.from.center
        steerCard {
            // The app is asked to open partway up, so it is already on its way as the card fills.
            launch {
                val launchAt = liveTuning[Tune.OpenLaunchAt]
                snapshotFlow { cardProgress.value }.first { it >= launchAt }
                launchedAt[0] = android.os.SystemClock.uptimeMillis()
                viewModel.open(
                    target.key,
                    LaunchOrigin.toScreen(hostView, target.from),
                    android.app.ActivityOptions
                        .makeCustomAnimation(context, com.gabriel.zenithui.R.anim.zenith_app_enter, 0)
                        .toBundle(),
                )
            }
            flyCard(1f)
            // The app should now cover Zenith. If Zenith is still in front, it never opened: come back.
            delay(900)
            if (hostLifecycle?.currentState?.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) == true) {
                cardOpening = false
                flyCard(0f)
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
            // A super icon, wide or tall, starts its card from its own stretched picture.
            val wideIcon = if (from.width > from.height * 1.4f || from.height > from.width * 1.4f) {
                runCatching {
                    viewModel.icons.loadWide(key, from.width.roundToInt(), from.height.roundToInt())
                }.getOrNull()
            } else {
                null
            }
            preparingLaunch = false
            // A different app starts from its own icon.
            if (card != null) {
                cardJob[0]?.cancel()
                restCard()
            }
            launchedAt[0] = 0L
            openCard(AppExpansion(key = key, from = from, icon = icon, wideIcon = wideIcon))
        }
    }

    androidx.compose.runtime.DisposableEffect(hostLifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    // Wind the arrival back now, unseen. Stopped with the screen off, the next
                    // thing seen is an unlock: the full fly-in. Stopped behind an app: a settle.
                    if (!liveMotion.reduced) {
                        arrivalDepth.floatValue = if (power?.isInteractive == false) FlyIn.FULL else FlyIn.SETTLE
                        arrivalPending[0] = true
                        scope.launch { arrivalClock.snapTo(0f) }
                    }
                    // The app now covers Zenith. Put the card away while nobody can see it and
                    // leave the home screen sunk back, so the first frame on the way home is the
                    // home screen waiting to come forward. Keeping the full-screen card here is what
                    // made closing an app shrink twice: once by Android, then again by Zenith.
                    val moving = card
                    if (moving != null && launchedAt[0] != 0L) {
                        returningKey[0] = moving.key
                        returningCard[0] = moving
                        cardJob[0]?.cancel()
                        holdSunk = true
                        card = null
                        cardOpening = false
                        scope.launch { restCard() }
                    }
                }

                androidx.lifecycle.Lifecycle.Event.ON_START -> {
                    // A stopped window keeps its last picture; redraw straight away on the way back.
                    hostView.invalidate()
                    if (returningKey[0] == null && card != null && !cardOpening) {
                        cardJob[0]?.cancel()
                        scope.launch { restCard() }
                        card = null
                    }
                }

                // Resumed is when Zenith is on its way to the front again. Window focus, below, is
                // the backstop for the phones that resume before the home screen is shown.
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    comeForward()
                    // With the window already in front, arrive now; otherwise when it gets there,
                    // and in any case before long, so the home screen is never left wound back.
                    if (hostView.hasWindowFocus()) arrive() else scope.launch {
                        delay(450)
                        arrive()
                    }
                }

                else -> Unit
            }
        }
        hostLifecycle?.addObserver(observer)
        onDispose { hostLifecycle?.removeObserver(observer) }
    }

    // The home screen only comes forward once it can actually be seen. Android animates the closing
    // app's window first and shows a launcher that is not built into the phone only when that is
    // done, so anything started on ON_START had already finished, unseen, by the time the home
    // screen appeared. Window focus arrives as the home screen is shown: that is when One UI's grid
    // swells back and the app's icon lands.
    androidx.compose.runtime.DisposableEffect(hostView) {
        val listener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { focused ->
            if (focused) {
                comeForward()
                arrive()
            }
        }
        hostView.viewTreeObserver.addOnWindowFocusChangeListener(listener)
        onDispose { hostView.viewTreeObserver.removeOnWindowFocusChangeListener(listener) }
    }

    // Parallax for the real wallpaper: tell Android how far across the pages we are, so a
    // wallpaper wider than the screen slides slowly behind them, as on any stock launcher.
    val wallpaperManager = remember(context) { android.app.WallpaperManager.getInstance(context) }

    // The icons nearest an opening app make way for its card: as far as asked when it is full size.
    androidx.compose.runtime.DisposableEffect(Unit) {
        HomeRipple.reach = 150f * pxPerDp
        HomeRipple.push = {
            if (card != null && !closingCard) {
                liveTuning[Tune.IconPush] * pxPerDp * cardProgress.value.coerceIn(0f, 1f)
            } else {
                0f
            }
        }
        onDispose { HomeRipple.push = null }
    }
    LaunchedEffect(pagerState, pageCount) {
        snapshotFlow { pagerState.currentPage + pagerState.currentPageOffsetFraction }.collect { position ->
            val token = hostView.windowToken ?: return@collect
            val last = (pageCount - 1).coerceAtLeast(1)
            val horizontal = if (pageCount > 1) (position / last).coerceIn(0f, 1f) else 0.5f
            runCatching {
                wallpaperManager.setWallpaperOffsetSteps(1f / last, 1f)
                wallpaperManager.setWallpaperOffsets(token, horizontal, 0.5f)
                wallpaperOffset.value = Offset(horizontal, 0.5f)
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

    val density = LocalDensity.current

    // The hour, for a greeting at the top of the page; looked at again every minute.
    val hour by produceState(java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
        while (true) {
            delay(60_000)
            value = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        }
    }

    /** The page in view as the drop planner sees it, measured from the grid actually drawn. */
    fun dropPage(): DropPlanner.Page =
        DropPlanner.pageFor(settings, settings.freeform && plus.active, drag.cellWidth / drag.cellHeight)

    // Where the carried item would land if let go right now, and what would step aside for it.
    // Worked out afresh as it moves; the hint, whatever steps aside and the drop itself all come
    // from here, so letting go always does what is on show.
    val dropPlan by remember {
        derivedStateOf {
            val item = drag.item ?: return@derivedStateOf null
            // About to be thrown away, put in the dock or made into a folder: not a spot on the page.
            // Merely passing over the bin is not: it sits over the top row, which you can still use.
            if (drag.removeArmed || drag.hoverItemId != null) return@derivedStateOf null
            if (item is AppCell && !dockBounds.isEmpty && dockBounds.contains(drag.pointer)) return@derivedStateOf null
            // Mid-turn the page under your finger is halfway across the screen.
            if (abs(pagerState.currentPageOffsetFraction) >= 0.01f) return@derivedStateOf null
            val corner = drag.exactTopLeft(settings.columns, settings.rows * ROW_UNITS) ?: return@derivedStateOf null
            val page = pagerState.currentPage
            DropPlanner.plan(layout.filter { it.page == page }, item, corner.x, corner.y, dropPage())
        }
    }

    // A tick each time the spot it would land in changes, so the hand knows without looking. Not
    // for the first spot of a carry: picking it up has only just been felt.
    val haptic = rememberHaptics()
    val lastSpot = remember { arrayOfNulls<DropPlanner.Spot>(1) }
    LaunchedEffect(dropPlan?.target, drag.isDragging) {
        val spot = if (drag.isDragging) dropPlan?.target else null
        if (spot != null && lastSpot[0] != null && spot != lastSpot[0]) haptic.tick()
        if (spot != null || !drag.isDragging) lastSpot[0] = spot
    }

    // Everything else on the page rocks while something is carried. One clock for all of it, and
    // it runs only from the moment something is picked up until the rocking has died away.
    val jiggleStrength = animateFloatAsState(if (drag.isDragging && !motion.reduced) 1f else 0f, tween(200), label = "jiggle")
    val jiggleClock = remember { mutableFloatStateOf(0f) }
    val jiggling by remember { derivedStateOf { jiggleStrength.value > 0.001f } }
    LaunchedEffect(jiggling) {
        if (!jiggling) return@LaunchedEffect
        val start = androidx.compose.runtime.withFrameNanos { it }
        while (true) androidx.compose.runtime.withFrameNanos { jiggleClock.floatValue = (it - start) / 1_000_000_000f }
    }

    // Rest over a spot for a moment and whatever is there steps aside, the way it does on a phone;
    // keep moving and nothing shuffles. Resting on an icon's middle is for making a folder, and
    // resting on the bin for throwing away, so nothing steps aside then.
    LaunchedEffect(dropPlan?.target, drag.hoverCandidateId, overRemove, pagerState.currentPage) {
        drag.preview = null
        val plan = dropPlan ?: return@LaunchedEffect
        if (plan.moves.isEmpty() || drag.hoverCandidateId != null || overRemove) return@LaunchedEffect
        delay(REORDER_DELAY)
        val latest = dropPlan ?: return@LaunchedEffect
        if (latest.target == plan.target && latest.moves.isNotEmpty()) {
            drag.preview = DropPreview(pagerState.currentPage, latest)
        }
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
     * Puts the carried item down on the page in view where it was let go, with whatever is there
     * stepping aside: exactly what was on show at the moment you let go.
     */
    fun dropOnPage(item: HomeItem) {
        val page = pagerState.currentPage
        val ghost = drag.ghostOnGrid
        val corner = drag.exactTopLeft(settings.columns, settings.rows * ROW_UNITS)
        // If things had already stepped aside for this very spot, that is what happens; otherwise
        // the plan for where it was let go.
        val plan = drag.preview?.takeIf { it.page == page && it.plan.target == dropPlan?.target }?.plan ?: dropPlan
        if (plan != null) {
            // Free placement keeps the exact spot you let go of, not just the cell it rounds to —
            // unless it landed beside that spot, where an offset from the finger would mean nothing.
            val offset = corner?.takeIf { settings.freeform && plus.active && plan.exact }
            drag.landing = Landing(item.id, ghost)
            viewModel.applyDrop(
                id = item.id,
                page = page,
                plan = plan,
                dx = offset?.let { (it.x - plan.target.col).coerceIn(-0.49f, 0.49f) } ?: 0f,
                dy = offset?.let { (it.y - plan.target.row).coerceIn(-0.49f, 0.49f) } ?: 0f,
            )
            return
        }
        val settled = abs(pagerState.currentPageOffsetFraction) < 0.01f
        if (item.page == page) {
            // Nowhere near there has room, even with everything stepping aside, or it was let go
            // off the grid. It goes back to its own spot from under your finger, as on a phone,
            // rather than off to some far corner of the page.
            if (settled) drag.landing = Landing(item.id, ghost)
            return
        }
        // Carried onto another page, it has to land on that page: the opening nearest to where it
        // was let go, however far — or mid-turn, when that means nothing, the first one there is.
        val rowStep = if (settings.freeform && plus.active) 1 else ROW_UNITS
        val opening = if (settled && corner != null) {
            DropPlanner.opening(layout.filter { it.page == page }, item, corner.x, corner.y, dropPage())
        } else {
            viewModel.freeSlotOnPage(page, item.spanX, item.spanY, rowStep, item.id)?.let { DropPlanner.Spot(it.first, it.second) }
        }
        if (opening != null) {
            if (settled && corner != null) drag.landing = Landing(item.id, ghost)
            viewModel.applyDrop(item.id, page, DropPlanner.Plan(opening, exact = false))
            return
        }
        // No room on this page at all: on to the next page that has some, and the screen follows it
        // there. Nothing is ever dropped into thin air.
        val onward = viewModel.firstFreeSlotFrom(page, item.spanX, item.spanY, rowStep, item.id)
        if (onward != null) {
            viewModel.moveItem(item.id, onward.first, onward.second, onward.third)
            revealPage(onward.first)
        } else {
            Toast.makeText(context, "Every page is full", Toast.LENGTH_SHORT).show()
        }
    }

    // A landing plays the moment the item is let go; after that it would only replay by mistake.
    LaunchedEffect(drag.landing) {
        if (drag.landing != null) {
            delay(1000)
            drag.landing = null
        }
    }

    fun handleDrop() {
        val item = drag.item
        if (item != null) {
            val hover = drag.hoverItemId
            // Worked out here from live state, not from [overDock]: the page's drag gesture holds
            // on to the drop callback it started with, so a value captured then would be stale.
            val droppedOnDock = item is AppCell && !dockBounds.isEmpty && dockBounds.contains(drag.pointer)
            // Where the grey copy is as it is let go. Anywhere but a spot on the page, it is the
            // copy that is seen going: into the bin, into the folder, or into the dock under it.
            val copyAt = drag.pointer - drag.grab
            when {
                droppedOnDock -> {
                    // Where along the bar it was let go decides its place among the others.
                    val slots = settings.dock.size + 1
                    val index = ((drag.pointer.x - dockBounds.left) / dockBounds.width * slots).toInt()
                    if (viewModel.moveToDock(item.id, index)) {
                        drag.sent = Sent(item, copyAt, into = null)
                    } else {
                        Toast.makeText(context, "The dock is full — drag one out first", Toast.LENGTH_SHORT).show()
                    }
                }
                drag.removeArmed -> {
                    drag.sent = Sent(item, copyAt, into = drag.removeZone.center, faint = true)
                    viewModel.removeItem(item.id)
                }
                hover != null -> {
                    drag.sent = Sent(item, copyAt, into = layout.firstOrNull { it.id == hover }?.let { drag.iconCentre(it) })
                    viewModel.combine(item.id, hover)
                }
                else -> dropOnPage(item)
            }
            // Put down, thrown away or made into a folder: it is done, and the hand is told so.
            haptic.confirm()
        }
        drag.clear()
    }

    /**
     * Which way a page turn is being asked for: carrying an item off the side of the page flips to
     * the page beside it, and pushing past the last page makes a new one. Without this an item
     * could never leave the page it started on.
     *
     * Off the side means the middle of what you carry is past the edge of the grid, or your finger
     * is at the very edge of the glass. Holding an icon over the outer column to put it there never
     * turns the page: on most phones the middle of that column is nearer the edge than a thumb is
     * wide, and resting there is exactly what putting something in it looks like.
     */
    val flipDirection by remember(edgeFlipPx) {
        derivedStateOf {
            val item = drag.item
            if (item == null || pagerBounds.isEmpty || drag.cellWidth <= 0f) return@derivedStateOf 0
            val middle = drag.pointer.x - drag.grab.x + item.spanX * drag.cellWidth / 2f
            val gridLeft = drag.gridOrigin.x
            val gridRight = gridLeft + settings.columns * drag.cellWidth
            when {
                middle < gridLeft || drag.pointer.x < pagerBounds.left + edgeFlipPx -> -1
                middle > gridRight || drag.pointer.x > pagerBounds.right - edgeFlipPx -> 1
                else -> 0
            }
        }
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
        targetValue = when {
            drawerProgress > 0.55f -> 20.dp
            // An open folder has the home screen out of focus behind it.
            folderUp != null -> 14.dp
            // The liquid glass opening wants more of it than the card does.
            card != null && !closingCard && settings.openAnimation == "glass" -> (tuning[Tune.HomeBlur] + 12f).dp
            // One UI softens the home screen behind an opening app; a lighter frost for the card.
            // It is still out of focus while an app covers it, and clears as you come home.
            (card != null && !closingCard) || holdSunk -> tuning[Tune.HomeBlur].dp
            else -> 0.dp
        },
        // A quick frost in and out, so opening and closing apps never feels held up by it.
        animationSpec = tween(110),
        label = "homeBlur",
    )
    val homePressed by viewModel.homePressed.collectAsState()
    val environmentTick by viewModel.environmentTick.collectAsState()
    val wallpaperIncoming by viewModel.wallpaperIncoming.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.wallpaperRefused.collect {
            Toast.makeText(context, "Android would not set that wallpaper", Toast.LENGTH_SHORT).show()
        }
    }
    val isDefaultLauncher = remember(homePressed, environmentTick) { isDefaultLauncher(context) }
    LaunchedEffect(homePressed) {
        if (homePressed > 0) {
            // Home while an app is still opening sends it straight back into its icon.
            if (card != null && cardOpening) closeCard()
            hideSheet()
            settingsOpen = false
            widgetsOpen = false
            closeFolder()
            closeDrawer()
            if (pagerState.currentPage != 0) pagerState.animateScrollToPage(0)
        }
    }

    BackHandler(enabled = (card != null && cardOpening) || drawerProgress > 0.002f || sheetVisible || resizingId != null || ambient || folderUp != null) {
        when {
            card != null && cardOpening -> closeCard()
            ambient -> ambient = false
            sheetVisible -> hideSheet()
            folderUp != null -> closeFolder()
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
            // Pinching in anywhere on the home screen opens Customise, the way it does on Samsung
            // and Pixel phones. It only watches; the page still gets every touch.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var startSpan = 0f
                    var fired = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val fingers = event.changes.filter { it.pressed }
                        if (fingers.isEmpty()) break
                        if (fingers.size < 2) {
                            startSpan = 0f
                            continue
                        }
                        val span = (fingers[0].position - fingers[1].position).getDistance()
                        if (startSpan == 0f) {
                            startSpan = span
                        } else if (
                            !fired && !drag.isDragging && !settingsOpen && !widgetsOpen &&
                            // Only on the home screen itself. Through the drawer, a folder, a sheet
                            // or an app on its way open, Customise would open with those still up
                            // underneath it, and Back would go to them first.
                            !sheetVisible && folderUp == null && card == null && !dragging && drawer.value < 0.02f &&
                            span < startSpan * 0.72f
                        ) {
                            fired = true
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            showSheet(HomeSheet.Customize)
                        }
                    }
                }
            }
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

        // A wallpaper changing with the theme fades in here, behind everything.
        WallpaperFade(wallpaperIncoming, wallpaperOffset)

        // ------------------------------------------------------------ home
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // The home screen sinks back as the card grows, on the very same spring, and
                    // swells forward again on its own spring when you come home from the app. The
                    // return may overshoot a little past full size, which is the point.
                    val depth = homeDepth()
                    val t = liveTuning
                    // Under 1 it sinks back behind the app; over 1 it rushes towards you instead.
                    val zoom = 1f + (t[Tune.HomeScale] - 1f) * depth
                    val scale = (1f - 0.06f * drawerProgress) * zoom
                    scaleX = scale
                    scaleY = scale
                    // It zooms about the icon the app came from, as far as asked, so the eye is
                    // carried into the app and out of it again at the same spot.
                    val icon = HomeRipple.origin
                    val towards = if (depth != 0f && icon.isSpecified) t[Tune.HomePivot] else 0f
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                        LaunchMotion.pivot(icon.x, size.width, towards),
                        LaunchMotion.pivot(icon.y, size.height, towards),
                    )
                    val sunk = depth.coerceIn(0f, 1f)
                    // Settings is glass over the wallpaper; the home screen is not seen through it.
                    alpha = (1f - 0.55f * drawerProgress) * (1f - t[Tune.HomeDim] * sunk) * (1f - pageShown.value)
                }
                // The blur is its own short animation rather than a per-frame effect rebuild:
                // recreating a full screen RenderEffect on every drag frame is what made the
                // drawer feel like it was lagging behind the finger.
                .then(if (homeBlur > 0.5.dp) Modifier.blur(homeBlur) else Modifier)
        ) {
            // Picking something up on the home screen answers a beat sooner than Android's usual
            // long press, so moving things feels immediate rather than like waiting for permission.
            val baseConfiguration = androidx.compose.ui.platform.LocalViewConfiguration.current
            val quickHold = remember(baseConfiguration) { QuickHold(baseConfiguration) }
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalViewConfiguration provides quickHold) {
            Column(Modifier.fillMaxSize()) {
                // The page's left and right lines: the outer edges of the first and last columns'
                // icons. What is written at the top starts on the left one, and the search bar
                // runs from one to the other. They are measured from the page's own margin, which
                // is put on separately so that it rounds to the same pixel as the page's does.
                val geometry = drag.geometry
                val leftLine = if (geometry != null) {
                    with(density) { (geometry.originX + geometry.leftLine).toDp() }
                } else {
                    14.dp
                }
                val rightLine = if (geometry != null) {
                    with(density) { (geometry.width - geometry.originX - geometry.rightLine).toDp() }
                } else {
                    14.dp
                }
                val header = HomeHeader.text(settings.header, settings.userName, hour)
                if (header != null) {
                    Greeting(
                        text = header,
                        modifier = Modifier
                            .statusBarsPadding()
                            .arriving({ arrivalClock.value }, { arrivalDepth.floatValue }, distance = 1f, from = (-28).dp)
                            // Holding it opens Customise too — always reachable, however full
                            // the page underneath is.
                            .pointerInput(Unit) {
                                detectTapGestures(onLongPress = {
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    showSheet(HomeSheet.Customize)
                                })
                            }
                            .padding(horizontal = PAGE_MARGIN)
                            .padding(start = leftLine, end = rightLine, top = 8.dp, bottom = 8.dp),
                    )
                } else {
                    Spacer(Modifier.statusBarsPadding().height(8.dp))
                }

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
                        // With movement reduced a page simply slides.
                        val turn = PageEffects.transform(if (motion.reduced) "slide" else settings.pageEffect, offset)
                        HomePage(
                            page = page,
                            active = page == pagerState.currentPage,
                            items = layout.filter { it.page == page },
                            settings = settings,
                            appsByKey = appsByKey,
                            drag = drag,
                            resizingId = resizingId,
                            onResize = { item, spanX, spanY ->
                                when (item) {
                                    is WidgetCell -> viewModel.resizeWidget(item.id, spanX, spanY)
                                    is AppCell -> viewModel.resizeApp(item.id, spanX, rows = spanY / ROW_UNITS)
                                    else -> false
                                }
                            },
                            onResizeDone = { resizingId = null },
                            onOpenApp = { launchApp(it) },
                            onOpenFolder = { folder, icon -> openFolder(folder.id, icon) },
                            onItemMenu = { showSheet(HomeSheet.Item(it.id)) },
                            onSearch = { openDrawer(search = true) },
                            onNoteChange = { viewModel.setNote(it) },
                            onAmbient = { ambient = true },
                            onDrop = { handleDrop() },
                            dropPlan = dropPlan,
                            freePlacement = settings.freeform && plus.active,
                            animateEntrance = entrance,
                            pageOffset = if (turn.parallax) offset else 0f,
                            arrivalClock = { arrivalClock.value },
                            arrivalDepth = { arrivalDepth.floatValue },
                            jiggleClock = { jiggleClock.floatValue },
                            jiggleStrength = { jiggleStrength.value },
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = turn.alpha
                                    scaleX = turn.scale
                                    scaleY = turn.scale
                                    rotationY = turn.rotationY
                                    transformOrigin = TransformOrigin(turn.pivotX, 0.5f)
                                    translationX = turn.shiftX * size.width
                                    // Further back than the default, so a turning page keeps its
                                    // shape instead of flaring out towards the eye.
                                    cameraDistance = 14f * this.density
                                }
                                .padding(horizontal = PAGE_MARGIN, vertical = 4.dp),
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

                AnimatedVisibility(
                    visible = !isDefaultLauncher && !hintDismissed && hintAllowed,
                    enter = fadeIn(tween(260)) + androidx.compose.animation.expandVertically(
                        androidx.compose.animation.core.spring(dampingRatio = 1f, stiffness = 300f)
                    ),
                    exit = fadeOut(tween(160)) + androidx.compose.animation.shrinkVertically(
                        androidx.compose.animation.core.spring(dampingRatio = 1f, stiffness = 300f)
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    DefaultLauncherHint(
                        onSetDefault = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        },
                        onDismiss = { hintDismissed = true },
                        modifier = Modifier.padding(horizontal = 18.dp),
                    )
                }

                PageDots(
                    count = pageCount,
                    position = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
                    canAdd = pageCount < LayoutBuilder.MAX_PAGES,
                    onAddPage = { revealPage(viewModel.addPage()) },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .arriving({ arrivalClock.value }, { arrivalDepth.floatValue }, distance = 0.7f, from = 30.dp)
                        .padding(vertical = 10.dp),
                )

                // From the first column's icons to the last column's, like everything on the page.
                if (settings.searchBar) {
                    HomeSearchBar(
                        onSearch = { openDrawer(search = true) },
                        modifier = Modifier
                            .arriving({ arrivalClock.value }, { arrivalDepth.floatValue }, distance = 0.85f, from = 40.dp)
                            .padding(horizontal = PAGE_MARGIN)
                            .padding(start = leftLine, end = rightLine, bottom = 10.dp),
                    )
                }

                if (settings.showDock) DockBar(
                    keys = settings.dock,
                    appsByKey = appsByKey,
                    settings = settings,
                    highlight = overDock,
                    animateEntrance = entrance,
                    geometry = drag.geometry,
                    onOpen = { launchApp(it) },
                    onLongPress = { showSheet(HomeSheet.DrawerApp(it)) },
                    onReorder = { from, to -> viewModel.moveInDock(from, to) },
                    onDragOut = { key, iconCentre ->
                        if (drag.cellWidth > 0f && drag.cellHeight > 0f) {
                            // The cell that puts a home screen icon exactly where the dock icon was
                            // let go; from there it is put down like any icon on the page.
                            val iconMiddle = (drag.geometry?.topInset ?: 0) + drag.iconSizePx / 2f
                            val ghost = Offset(
                                iconCentre.x - drag.gridOrigin.x - drag.cellWidth / 2f,
                                iconCentre.y - drag.gridOrigin.y - iconMiddle,
                            )
                            val placed = viewModel.moveDockToHome(
                                key = key,
                                page = pagerState.currentPage,
                                exactCol = ghost.x / drag.cellWidth,
                                exactRow = ghost.y / drag.cellHeight,
                                colWidthInRowUnits = drag.cellWidth / drag.cellHeight,
                            )
                            if (placed != null) {
                                drag.landing = Landing(placed, ghost)
                            } else {
                                Toast.makeText(context, "No room on this page", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onBounds = { dockBounds = it },
                    modifier = Modifier
                        .arriving({ arrivalClock.value }, { arrivalDepth.floatValue }, distance = 1f, from = 56.dp)
                        .padding(horizontal = PAGE_MARGIN)
                        .navigationBarsPadding()
                        .padding(bottom = 10.dp),
                ) else {
                    // Without the dock the pages reach down to the gesture bar instead.
                    Spacer(Modifier.navigationBarsPadding().height(10.dp))
                }
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

        // Down from the top edge while something is being resized, and back up when it is done.
        AnimatedVisibility(
            visible = resizingId != null,
            enter = edgeIn(motion.reduced),
            exit = edgeOut(motion.reduced),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                Modifier
                    .statusBarsPadding()
                    .padding(start = BAR_ROOM, top = 12.dp, end = BAR_ROOM, bottom = BAR_ROOM)
                    .pressBounce(0.96f)
                    .glass(ZenithShapes.pill, strength = 1.2f, solid = 0.4f)
                    .clickable { resizingId = null }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Pull the pointer to resize · tap to finish",
                    color = palette.onGlass,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // The same thing in taps: a widget's named sizes, and arrows that move it a cell at a time.
        // It comes up from the foot of the screen, and on its way back down it is still the bar
        // for whatever was last being resized.
        val reshaping = resizingId?.let { id -> layout.firstOrNull { it.id == id } }
        val reshaped = remember { arrayOfNulls<HomeItem>(1) }
        if (reshaping != null) reshaped[0] = reshaping
        AnimatedVisibility(
            visible = reshaping != null,
            enter = edgeIn(motion.reduced, fromFoot = true),
            exit = edgeOut(motion.reduced, toFoot = true),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            val item = reshaped[0]
            if (item != null) ResizeBar(
                item = item,
                columns = settings.columns,
                rows = settings.rows,
                onSize = { size -> viewModel.resizeWidgetTo(item.id, size.spanX, size.spanY) },
                onNudge = { dCol, dRows -> viewModel.nudgeItem(item.id, dCol, dRows) },
                onDone = { resizingId = null },
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
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
            LaunchedEffect(overRemove) {
                drag.removeArmed = false
                if (overRemove) {
                    kotlinx.coroutines.delay(450)
                    drag.removeArmed = true
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                }
            }
        }
        // The bin comes down from the top edge when something is picked up, and goes back up when
        // it is put down: still red on its way, if what was put down went into it.
        val binTook = remember { booleanArrayOf(false) }
        if (drag.isDragging) binTook[0] = drag.removeArmed
        AnimatedVisibility(
            visible = drag.isDragging,
            enter = edgeIn(motion.reduced),
            exit = edgeOut(motion.reduced),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            RemoveTarget(
                armed = if (drag.isDragging) drag.removeArmed else binTook[0],
                hovering = overRemove,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = BAR_ROOM, top = 12.dp, end = BAR_ROOM, bottom = BAR_ROOM)
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
        }
        if (drag.isDragging) DragGhost(drag = drag, settings = settings, appsByKey = appsByKey)
        // What was let go into the bin, the dock or a folder is seen going there.
        drag.sent?.let { sent ->
            SentGhost(sent = sent, drag = drag, settings = settings, appsByKey = appsByKey, onGone = { if (drag.sent === sent) drag.sent = null })
        }

        // ------------------------------------------------------------ folder
        folderUp?.let { (id, icon) ->
            val folder = layout.firstOrNull { it.id == id } as? FolderCell
            // Taking the last app out dissolves the folder: then there is nothing left to show.
            LaunchedEffect(folder == null) {
                if (folder == null) {
                    folderJob[0]?.cancel()
                    folderProgress.snapTo(0f)
                    folderUp = null
                }
            }
            if (folder != null) FolderPanel(
                folder = folder,
                origin = icon,
                progress = { folderProgress.value },
                grows = !motion.reduced,
                appsByKey = appsByKey,
                settings = settings,
                onOpen = {
                    launchApp(it)
                    closeFolder()
                },
                onRemoveApp = { viewModel.removeFromFolder(folder.id, it) },
                onRename = { viewModel.renameFolder(folder.id, it) },
                onDismiss = { closeFolder() },
            )
        }

        StreakStrip(
            state = checkIn,
            onShown = { viewModel.clearCheckIn() },
            modifier = Modifier.align(Alignment.TopCenter),
        )

        // The bedside clock is a room going dark, not a page turning: it fades in slowly, and out
        // a little quicker.
        AnimatedVisibility(
            visible = ambient,
            enter = fadeIn(tween(if (motion.reduced) Motion.REDUCED_MS else 520)),
            exit = fadeOut(tween(if (motion.reduced) Motion.REDUCED_MS else 300)),
        ) {
            AmbientScreen(
                userName = settings.userName,
                calendarDeclined = settings.calendarDeclined,
                onCalendarDeclined = { declined -> viewModel.updateSettings { it.copy(calendarDeclined = declined) } },
                onDismiss = { ambient = false },
            )
        }

        card?.let { moving ->
            AppLaunchCard(
                card = moving,
                progress = { cardProgress.value },
                x = { cardX.value },
                y = { cardY.value },
                look = remember(tuning) { CardLook.of(tuning) },
                glass = settings.openAnimation == "glass",
                catchTaps = cardOpening,
                onTap = { closeCard() },
            )
        }

        // ------------------------------------------------------------ settings
        SettingsScreen(
            visible = settingsOpen,
            covered = sheetVisible,
            settings = settings,
            plus = plus,
            preview = SettingsPreview(
                items = remember(layout) { layout.filter { it.page == 0 } },
                appsByKey = appsByKey,
                wallpaper = glassLevels?.sharp,
                screenWidth = with(density) { drawerWidthPx.toDp() },
                screenHeight = with(density) { drawerHeightPx.toDp() },
            ),
            actions = SettingsActions(
                change = { transform -> viewModel.updateSettings(transform) },
                wallpaper = { showSheet(HomeSheet.Wallpapers) },
                themeMaker = { withPlus { showSheet(HomeSheet.ThemeMaker) } },
                hiddenApps = { withPlus { showSheet(HomeSheet.HiddenApps) } },
                freeform = { on ->
                    // Turning it on is the Plus part; turning it off is always allowed.
                    if (on && !plus.active) showSheet(HomeSheet.Plus)
                    else viewModel.updateSettings { it.copy(freeform = on) }
                },
                backup = { withPlus { saveBackup.launch(Backup.fileName()) } },
                restore = { withPlus { openBackup.launch(arrayOf("*/*")) } },
                plus = { showSheet(HomeSheet.Plus) },
                unlockTheme = { id -> viewModel.unlockTheme(id) },
                redeem = { code -> viewModel.redeemAuraCode(code) },
                help = { showSheet(HomeSheet.Help) },
                reset = { viewModel.resetEverything() },
                tidy = { viewModel.tidyUp() },
                // A build with no server has no account, and nothing of one is shown.
                account = if (!ServerConfig.enabled) null else AccountActions(
                    signed = signed,
                    googleClientId = ServerConfig.googleClientId,
                    create = viewModel::createAccount,
                    signIn = viewModel::signIn,
                    signInWithGoogle = viewModel::signInWithGoogle,
                    signOut = viewModel::signOut,
                    delete = viewModel::deleteAccount,
                    backUp = viewModel::backUpToAccount,
                    restore = viewModel::restoreFromAccount,
                    refresh = { viewModel.refreshAccount(now = true) },
                ),
            ),
            onDismiss = { settingsOpen = false },
        )

        // ------------------------------------------------------------ widgets
        WidgetGallery(
            visible = widgetsOpen,
            settings = settings,
            geometry = drag.geometry,
            onAdd = { type, spanX, spanY ->
                val page = viewModel.addWidget(type, spanX, spanY)
                revealPage(page)
                page != null
            },
            onDismiss = { widgetsOpen = false },
        )

        // ------------------------------------------------------------ sheets
        // One sheet opened from another: the old one is kept a moment, so it is seen going down
        // as the new one comes up, rather than one being swapped for the other where it stands.
        AnimatedContent(
            targetState = sheet,
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = EnterTransition.None,
                    // Nothing of its own: only time for the sheet inside to put itself away.
                    initialContentExit = fadeOut(snap(delayMillis = if (motion.reduced) Motion.REDUCED_MS else SHEET_GONE_MS)),
                    sizeTransform = null,
                )
            },
            modifier = Modifier.fillMaxSize(),
            label = "sheet",
        ) { current ->
            // The sheet that is being left is closed, whatever the one taking its place is.
            val up = sheetVisible && transition.targetState != EnterExitState.PostExit
            when (current) {
                is HomeSheet.Item -> {
                    // Taking the item off the page is one of the things its own menu does. The
                    // menu keeps what it last showed, so it is seen to close and not simply gone.
                    val found = layout.firstOrNull { it.id == current.id }
                    val last = remember { arrayOfNulls<HomeItem>(1) }
                    if (found != null) last[0] = found
                    val item = found ?: last[0]
                    if (found == null) hideSheet()
                    if (item != null) ItemMenuSheet(
                        visible = up,
                        item = item,
                        appsByKey = appsByKey,
                        onCustomize = { showSheet(HomeSheet.Customize) },
                        onOpen = { launchApp(it) },
                        onAppInfo = { viewModel.openAppInfo(it) },
                        onChooseToggles = { showSheet(HomeSheet.Toggles) },
                        onRename = { openFolder(it, null) },
                        onResize = {
                            resizingId = item.id
                            hideSheet()
                        },
                        onRemove = { viewModel.removeItem(item.id) },
                        onUninstall = { viewModel.uninstall(it) },
                        onDismiss = { hideSheet() },
                    )
                }

                is HomeSheet.DrawerApp -> {
                    val app = appsByKey[current.key]
                    DrawerAppSheet(
                        visible = up,
                        label = app?.label ?: current.key,
                        pinned = current.key in settings.dock,
                        onOpen = {
                            launchApp(current.key)
                            closeDrawer()
                        },
                        onAddToHome = {
                            val page = viewModel.addAppToFirstFreeSlot(current.key)
                            if (page == null) {
                                Toast.makeText(context, "Every page is full", Toast.LENGTH_SHORT).show()
                            } else {
                                // The app may well have gone onto a page you are not looking at, so
                                // put the drawer away and take you to it.
                                revealPage(page)
                                closeDrawer()
                            }
                        },
                        onToggleDock = {
                        if (!viewModel.toggleDock(current.key)) {
                            Toast.makeText(context, "The dock is full — drag one out first", Toast.LENGTH_SHORT).show()
                        }
                    },
                        onAppInfo = { viewModel.openAppInfo(current.key) },
                        onUninstall = { viewModel.uninstall(current.key) },
                        onDismiss = { hideSheet() },
                    )
                }

                HomeSheet.Customize -> CustomizeSheet(
                    visible = up,
                    onWallpaper = { showSheet(HomeSheet.Wallpapers) },
                    onHelp = { showSheet(HomeSheet.Help) },
                    onAmbient = {
                        hideSheet()
                        ambient = true
                    },
                    onWidgets = {
                        hideSheet()
                        widgetsOpen = true
                    },
                    onSettings = {
                        hideSheet()
                        settingsOpen = true
                    },
                    onDefaultLauncher = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    },
                    onDismiss = { hideSheet() },
                )

                HomeSheet.Help -> HelpSheet(
                    visible = up,
                    onDismiss = { hideSheet() },
                )

                HomeSheet.Wallpapers -> WallpaperSheet(
                    visible = up,
                    settings = settings,
                    plusActive = plus.active,
                    onSet = { style, pinnedPalette, lockToo, switchThemeTo ->
                        viewModel.setWallpaper(style, pinnedPalette, lockToo, switchThemeTo)
                    },
                    onPhoto = {
                        viewModel.photoWallpaperSet()
                        // The glass is drawn from the wallpaper, so read the new one straight away.
                        viewModel.refreshWallpaper()
                    },
                    onFollow = { on -> viewModel.updateSettings { it.copy(wallpaperFollow = on) } },
                    onPlus = { showSheet(HomeSheet.Plus) },
                    onDismiss = { hideSheet() },
                )

                HomeSheet.Plus -> PlusSheet(
                    visible = up,
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
                    visible = up,
                    selected = settings.toggles,
                    onChange = { chosen -> viewModel.updateSettings { it.copy(toggles = chosen) } },
                    onDismiss = { hideSheet() },
                )

                HomeSheet.ThemeMaker -> ThemeMakerSheet(
                    visible = up,
                    settings = settings,
                    onChange = { transform -> viewModel.updateSettings(transform) },
                    onDismiss = { hideSheet() },
                )

                HomeSheet.HiddenApps -> HiddenAppsSheet(
                    visible = up,
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
private fun Greeting(text: String, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            color = palette.onGlass.copy(alpha = 0.85f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PageDots(
    count: Int,
    /** Where the pager is: a page number, with a fraction while a page is turning. */
    position: () -> Float,
    canAdd: Boolean,
    onAddPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        PagerDots(count, position)

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
    val reduced = LocalMotionLevel.current.reduced
    val scale by animateFloatAsState(
        targetValue = if (armed) 1.12f else 1f,
        animationSpec = Motion.bouncy(),
        label = "binScale",
    )
    // Ready to take what is held over it, the bin tips towards it and swells; read only where it
    // is drawn, so it rocks into place without anything being composed again.
    val tip = animateFloatAsState(if (armed && !reduced) 1f else 0f, Motion.bouncy(), label = "binTip")
    Row(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .glass(ZenithShapes.pill, strength = 1.2f)
            .background(fill, ZenithShapes.pill)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Delete,
            contentDescription = null,
            tint = palette.onGlass,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer {
                    rotationZ = -16f * tip.value
                    val grown = 1f + 0.18f * tip.value
                    scaleX = grown
                    scaleY = grown
                },
        )
        Spacer(Modifier.width(8.dp))
        AnimatedContent(
            targetState = armed,
            transitionSpec = { wordsChange(reduced) },
            label = "binWords",
        ) { ready ->
            Text(
                text = if (ready) "Release to remove" else "Hold to remove",
                color = palette.onGlass,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
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
        var row = 0
        MenuRow(Icons.AutoMirrored.Rounded.OpenInNew, "Open", index = row++) { onOpen(); onDismiss() }
        MenuRow(Icons.Rounded.AddHome, "Add to home screen", index = row++) { onAddToHome(); onDismiss() }
        // Apps go into the dock by dragging them there; the menu only offers the way back out.
        if (pinned) MenuRow(Icons.Rounded.PushPin, "Remove from dock", index = row++) { onToggleDock(); onDismiss() }
        MenuRow(Icons.Rounded.Info, "App info", index = row++) { onAppInfo(); onDismiss() }
        MenuRow(Icons.Rounded.Delete, "Uninstall", danger = true, index = row) { onUninstall(); onDismiss() }
    }
}

/** Android's view configuration with a shorter long press, for picking things up on the home screen. */
private class QuickHold(base: androidx.compose.ui.platform.ViewConfiguration) :
    androidx.compose.ui.platform.ViewConfiguration by base {
    override val longPressTimeoutMillis: Long get() = 320L
}
