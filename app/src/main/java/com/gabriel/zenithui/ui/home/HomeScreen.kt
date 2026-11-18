package com.gabriel.zenithui.ui.home

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.boundsInWindow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.gabriel.zenithui.ZenithViewModel
import com.gabriel.zenithui.data.AnimationTuning
import com.gabriel.zenithui.data.AppCell
import com.gabriel.zenithui.data.AppEdit
import com.gabriel.zenithui.data.AppShortcut
import com.gabriel.zenithui.data.Backup
import com.gabriel.zenithui.data.Badges
import com.gabriel.zenithui.data.CalendarAccess
import com.gabriel.zenithui.data.CalendarRepository
import com.gabriel.zenithui.data.NotificationDots
import com.gabriel.zenithui.data.ShortcutKeys
import com.gabriel.zenithui.ui.common.BadgeState
import com.gabriel.zenithui.ui.common.LocalBadges
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import com.gabriel.zenithui.data.GestureActions
import com.gabriel.zenithui.data.Gestures
import com.gabriel.zenithui.data.HostedWidgetCell
import com.gabriel.zenithui.data.StackCell
import com.gabriel.zenithui.data.LockService
import com.gabriel.zenithui.data.AndroidClose
import com.gabriel.zenithui.data.AppPictures
import com.gabriel.zenithui.data.CloseAway
import com.gabriel.zenithui.data.CloseRoute
import com.gabriel.zenithui.data.CloseRules
import com.gabriel.zenithui.data.GestureHandshake
import com.gabriel.zenithui.data.PictureRules
import com.gabriel.zenithui.data.PictureService
import com.gabriel.zenithui.ui.common.IconPlaces
import com.gabriel.zenithui.data.SystemPanels
import com.gabriel.zenithui.data.Torch
import com.gabriel.zenithui.ui.common.GlassButton
import com.gabriel.zenithui.ui.common.staggerDelay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest
import com.gabriel.zenithui.data.DropPlanner
import com.gabriel.zenithui.data.FolderCell
import com.gabriel.zenithui.data.FolderPour
import com.gabriel.zenithui.data.FolderSize
import com.gabriel.zenithui.data.HoldDo
import com.gabriel.zenithui.data.HoldEntry
import com.gabriel.zenithui.data.HoldGesture
import com.gabriel.zenithui.data.HoldMenuRules
import com.gabriel.zenithui.data.HoldSubject
import com.gabriel.zenithui.data.HoldTuning
import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.SizeChoice
import com.gabriel.zenithui.data.LaunchMotion
import com.gabriel.zenithui.data.LayoutBuilder
import com.gabriel.zenithui.data.NewLook
import com.gabriel.zenithui.data.ROW_UNITS
import com.gabriel.zenithui.data.Tune
import com.gabriel.zenithui.data.WallStyle
import com.gabriel.zenithui.data.WidgetCell
import com.gabriel.zenithui.data.findActivity
import com.gabriel.zenithui.ui.common.HomeCover
import com.gabriel.zenithui.ui.common.LaunchOrigin
import com.gabriel.zenithui.ui.common.LocalHomeCover
import com.gabriel.zenithui.ui.common.homeScaleUnderPage
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.wordsChange
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.rememberHaptics
import com.gabriel.zenithui.ui.settings.LookActions
import com.gabriel.zenithui.ui.settings.SettingsActions
import com.gabriel.zenithui.ui.settings.SettingsPreview
import com.gabriel.zenithui.ui.settings.SettingsScreen
import com.gabriel.zenithui.ui.theme.LocalWallpaperDim
import com.gabriel.zenithui.ui.theme.LocalWallpaperOffset
import com.gabriel.zenithui.ui.theme.Palettes
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import com.gabriel.zenithui.ui.theme.inkOnTint
import com.gabriel.zenithui.ui.theme.GlassRing
import com.gabriel.zenithui.ui.theme.LocalGlassRing
import com.gabriel.zenithui.ui.theme.ringOfLight
import androidx.compose.ui.layout.positionInWindow
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

/** How much of the theme's page frosts over the home screen behind the drawer, in a theme that blurs nothing. */
private const val DRAWER_VEIL = 0.45f

/** Coming home longer than this after opening an app does not shrink it back into its icon. */
private const val CLOSE_WINDOW_MS = 30 * 60 * 1000L

/** How long after Zenith comes back from an app the home button still counts as part of coming back. */
private const val COMING_BACK_MS = 2_000L

/** The app closing as Zenith comes back, and how it goes back into its icon (see [CloseRules.route]). */
private class Closing(
    /** The app, as Zenith writes it. */
    val key: String,
    val pkg: String,
    /** The icon it goes into, at rest; null when there is none on show. */
    val spot: IconPlaces.Spot?,
    /**
     * With no icon on show, which way it goes off the screen, towards where it lives (see
     * CloseAway): down into the drawer, or to the side into the library or towards its page.
     */
    val away: CloseAway.Way?,
    /** Its card as it was opened from Zenith, if it was. */
    val opened: AppExpansion?,
    val route: CloseRoute,
    /** A picture of it from this time it was open, from the app picture helper. */
    val picture: android.graphics.Bitmap?,
    /** What Zenith told Android, if Android asked. */
    val answer: GestureHandshake.Answer?,
    /** When it was worked out. */
    val at: Long,
) {
    /** The icon that lands: none for a folder, which only knocks the icons round it. */
    val landKey: String? get() = spot?.takeIf { !it.folder }?.key
}

/** How long it has to stay there, so carrying an item past an edge does not flip anything. */
private const val PAGE_FLIP_DELAY = 240L

/**
 * How long the carried item rests over a spot before whatever is there steps aside for it: long
 * enough that carrying it across the page shuffles nothing, short enough to feel like it answers.
 */
private const val REORDER_DELAY = 280L

/** The space between the edge of the screen and the page, the dock and everything else on it. */
private val PAGE_MARGIN = 12.dp

/** How dark the veil over the wallpaper is when Customise's "Dim the wallpaper" is on. */
private const val WALLPAPER_DIM = 0.3f

/** How long the home screen must be left alone before a scheduled Look goes on. */
private const val LOOK_DUE_WAIT_MS = 1500L

/**
 * How long the home screen must be at rest before the new look is offered: well past the icons'
 * arrival, so the question comes up over a page that has settled.
 */
private const val NEW_LOOK_WAIT_MS = 1200L

private sealed interface HomeSheet {
    data class DrawerApp(val key: String) : HomeSheet
    /** One app's name, icon and whether its name shows. */
    data class EditApp(val key: String) : HomeSheet
    /** Where that app's icon comes from. */
    data class PickIcon(val key: String) : HomeSheet
    /** What the lock helper is, before Android's page for it opens. */
    data object LockHelper : HomeSheet
    /** What the app picture helper is, before Android's page for it opens. */
    data object AppPictures : HomeSheet
    /** What one of the Notch's permissions is for, before Android's page for it opens. */
    data class NotchAccess(val which: com.gabriel.zenithui.data.NotchAccess) : HomeSheet
    /** What one gesture could do, from Settings → Gestures. */
    data class GestureAction(val gesture: String) : HomeSheet
    /** The World clock's cities. */
    data object WorldCities : HomeSheet
    /** What the Countdown counts down to. */
    data object CountdownDate : HomeSheet
    /** Where the Weather widget's forecast is for. */
    data object WeatherPlace : HomeSheet
    /** A drawer tab to make (-1) or change (its place along the top). */
    data class DrawerTabEdit(val index: Int) : HomeSheet
    /** A name for the home screen as a new Look (null), or a new name for the Look [renaming]. */
    data class LookName(val renaming: String?) : HomeSheet
    /** What can be done with one saved Look. */
    data class LookMenu(val id: String) : HomeSheet
    /** A time for a Look to come on by itself. */
    data object LookTime : HomeSheet
    /** A Look somebody shared, by its code; a Featured one comes with its code filled in. */
    data class LookCode(val code: String) : HomeSheet
    /** The glass made your own. */
    data object GlassStudio : HomeSheet
    /** One of Zenith's widgets in a colour of its own. */
    data class WidgetColour(val id: String) : HomeSheet
    /** The fonts Zenith carries, and their licence. */
    data object FontLicences : HomeSheet
    data object Help : HomeSheet
    data object Plus : HomeSheet
    data object HiddenApps : HomeSheet
    data object Toggles : HomeSheet
    data object Wallpapers : HomeSheet
}

@Composable
fun HomeScreen(viewModel: ZenithViewModel) {
    val wallpaperOffset = remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    // What every app has waiting. Only what draws a dot reads it, so a notification arriving
    // redraws dots and nothing else.
    val counts by NotificationDots.counts.collectAsState()
    val badgeStyle by remember { viewModel.settings.map { Badges.of(it.badges) } }.collectAsState(Badges.of(viewModel.settings.value.badges))
    val badges = remember(counts, badgeStyle) { BadgeState(counts, badgeStyle) }
    CompositionLocalProvider(
        LocalWallpaperOffset provides wallpaperOffset,
        LocalBadges provides badges,
        LocalWidgetHost provides viewModel.widgetHost,
    ) {
        HomeContent(viewModel, wallpaperOffset)
    }
}

@Composable
private fun HomeContent(viewModel: ZenithViewModel, wallpaperOffset: MutableState<Offset>) {
    val palette = LocalPalette.current
    val context = LocalContext.current

    val settings by viewModel.settings.collectAsState()
    val layout by viewModel.layout.collectAsState()
    // What is popping off the home screen and what Undo is blowing back onto it.
    val bubbles = remember { Bubbles() }
    val looks by viewModel.looks.collectAsState()
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

    // A photo chosen as an app's icon: Android's own picker, which needs no permission. Which app
    // it is for is held here while the picker is up.
    val photoFor = remember { arrayOfNulls<String>(1) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val key = photoFor[0] ?: return@rememberLauncherForActivityResult
        photoFor[0] = null
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            if (!viewModel.choosePhotoIcon(key, uri)) {
                Toast.makeText(context, "That picture could not be read", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val drawer = remember { Animatable(0f) }
    // While a finger is down the progress is written straight from the drag, with no coroutine
    // in between, so the sheet sits exactly under the thumb.
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var drawerEverOpened by remember { mutableStateOf(false) }
    var focusSearch by remember { mutableStateOf(false) }
    // Search out of the pill: whether it is open, and whether its glass is out of the pill at all
    // (opening, open, or shrinking back into it), for which time the pill itself is not drawn, the
    // field's glass standing in its place.
    var searchOpen by remember { mutableStateOf(false) }
    var searchShown by remember { mutableStateOf(false) }
    // The pill as it is laid out, and where on screen it was as the field last set off from it or
    // turned back towards it: what the field's glass stretches out of and shrinks back into. Taken
    // as it goes each way, not kept up as the pill moves, so the pill arriving or the page springing
    // back from a pull composes nothing.
    val pillPlaced = remember { arrayOfNulls<androidx.compose.ui.layout.LayoutCoordinates>(1) }
    var searchFrom by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }

    /** Where the Search pill is on screen now, or null while it is not there, as while Undo has its place. */
    fun pillNow(): androidx.compose.ui.geometry.Rect? = pillPlaced[0]?.takeIf { it.isAttached }?.boundsInWindow()

    /**
     * Stretches the pill into the search field, with the keyboard up. With no pill on the screen to
     * come out of, the field fades in where it sits.
     */
    fun openSearch() {
        searchFrom = pillNow() ?: androidx.compose.ui.geometry.Rect.Zero
        searchShown = true
        searchOpen = true
    }

    /** Shrinks the field back into the pill, wherever the pill is by now. */
    fun closeSearch() {
        if (!searchOpen) return
        pillNow()?.let { searchFrom = it }
        searchOpen = false
    }
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val motion = LocalMotionLevel.current
    // Asked as something happens, by listeners that outlive a change to Motion.
    val liveMotion by androidx.compose.runtime.rememberUpdatedState(motion)
    var resizingId by remember { mutableStateOf<String?>(null) }
    // The menu beside something held, and a question asked in a card of glass over the home screen.
    val holdMenu = remember { HoldMenuState() }
    var alert by remember { mutableStateOf<HomeAlert?>(null) }
    // The held app's own shortcuts, once Android has said what they are. What was last heard is
    // kept by package, so a second hold on an app shows them with the menu and not after it.
    var holdShortcuts by remember { mutableStateOf(emptyList<AppShortcut>()) }
    val shortcutsKnown = remember { HashMap<String, List<AppShortcut>>() }
    // Edit mode: everything on the home screen wiggles, wears a minus and can be moved. And a few
    // words said over the home screen for a moment, in glass.
    var editing by remember { mutableStateOf(false) }
    // Customise: a card of glass at the foot of the home screen, which changes above it as you choose.
    var customising by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<HomeToast?>(null) }
    LaunchedEffect(toast) {
        if (toast != null) {
            kotlinx.coroutines.delay(2800)
            toast = null
        }
    }
    var ambient by remember { mutableStateOf(false) }
    // Every page small on a tray, to go to, move, add or take away.
    var overview by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<HomeSheet?>(null) }
    var sheetVisible by remember { mutableStateOf(false) }
    // Settings is a page of its own, under the sheets: one opened from it closes back onto it.
    var settingsOpen by remember { mutableStateOf(false) }
    // So is the widget gallery.
    var widgetsOpen by remember { mutableStateOf(false) }
    // The widget the gallery is choosing another to stack onto, while it is.
    var stackOnto by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(widgetsOpen) { if (!widgetsOpen) stackOnto = null }
    // The home screen fades out and sinks back under either of them, exactly as far as it is covered:
    // as they come up, and as they are pulled away again by a finger.
    val homeCover = remember { HomeCover() }
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
    // Where the middle of each app in the dock stands, so a drop from the page goes between them.
    var dockCentres by remember { mutableStateOf(emptyList<Float>()) }
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
    // How far open the drawer is: read only where it is placed or drawn, never while the home
    // screen is composed, so a frame of the swipe composes nothing. The lines it crosses are
    // derived, so only crossing one of them composes anything.
    val drawerNow: () -> Float = remember { { if (dragging) dragProgress else drawer.value } }
    val drawerShown by remember { derivedStateOf { drawerNow() > 0.002f } }
    // Moving under a finger or on its spring: the light on its glass, and on the home screen's
    // behind it, stays where it was until it is still (see HeldTilt).
    val drawerMoving = remember { derivedStateOf { dragging || drawer.isRunning } }
    val drawerCovers = remember { derivedStateOf { drawerNow() > 0.002f } }
    // Open and still: what the drawer asks for only once it is there, so asking costs the swipe nothing.
    val drawerSettled by remember { derivedStateOf { !dragging && !drawer.isRunning && drawer.value > 0.999f } }
    // The home screen's blur behind the drawer: a copy of it blurred once and faded in over it, not a
    // blur grown frame by frame (see DrawerFrost), as far as the theme blurs what is behind. Android 12
    // and later; before that the home screen only dims behind the drawer, as it always did there. A
    // theme that blurs nothing (Clean, Retro) lays a veil of its page over it instead, on any phone,
    // and so does Lite, in any theme.
    val theme = com.gabriel.zenithui.ui.theme.LocalTheme.current
    val lite = com.gabriel.zenithui.ui.theme.LocalLite.current
    val blurBehind = com.gabriel.zenithui.data.Performance.blurBehind(theme.blurBehind, lite).dp
    val canFrost = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && blurBehind > 0.dp
    val homeLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()
    val frost = remember { derivedStateOf { DrawerFrost.amount(drawerNow()) } }
    val frosting by remember { derivedStateOf { frost.value > 0f } }
    val frostPx = with(LocalDensity.current) { blurBehind.coerceAtLeast(1.dp).toPx() }
    val frostEffect = remember(frostPx) {
        androidx.compose.ui.graphics.BlurEffect(frostPx, frostPx, androidx.compose.ui.graphics.TileMode.Clamp)
    }
    val liveTilt = com.gabriel.zenithui.ui.theme.LocalTilt.current
    val liveParallax = com.gabriel.zenithui.ui.theme.LocalParallax.current
    val homeTilt = remember(liveTilt) { com.gabriel.zenithui.ui.theme.HeldTilt(liveTilt, drawerCovers) }
    val homeParallax = remember(liveParallax) { com.gabriel.zenithui.ui.theme.HeldTilt(liveParallax, drawerCovers) }
    val drawerTilt = remember(liveTilt) { com.gabriel.zenithui.ui.theme.HeldTilt(liveTilt, drawerMoving) }

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

    // A folder poured out of its tile: which one, where its tile is and where it was tapped, and how
    // far it has got (FolderMotion), read while laying out and drawing.
    var folderUp by remember { mutableStateOf<FolderUp?>(null) }
    val folderMotion = remember { FolderMotion() }
    val folderJob = remember { arrayOfNulls<kotlinx.coroutines.Job>(1) }
    // The open folder is away from its tile, which only comes back as the drop lands in it: for the
    // tiles to read while drawing.
    val folderAway = remember {
        object : FolderAway {
            override fun tile(id: String) = if (folderUp?.id == id) folderMotion.tileShown() else 1f
            override fun name(id: String) = if (folderUp?.id == id) folderMotion.nameShown() else 1f
        }
    }

    // The one ring of light on the home screen, for every pane of glass and every icon to read while
    // drawing: a tap on the wallpaper sends it out, and so do edit mode's waves.
    val glassRing = remember { GlassRing() }
    LaunchedEffect(glassRing) { glassRing.play() }
    val ringDensity = LocalDensity.current.density

    // Edit mode starts and ends with a wave of light: a quick one from where it was started wakes
    // everything as it reaches it, and a calmer one from where it was ended settles it all again.
    // While the calm one crosses the screen, what it has not reached yet still wiggles.
    var calming by remember { mutableStateOf(false) }
    val calmJob = remember { arrayOfNulls<kotlinx.coroutines.Job>(1) }
    /** A wave of [style] from [at] on the screen, or from the middle of it; none with movement reduced. */
    fun sendWave(at: Offset?, style: com.gabriel.zenithui.ui.theme.RingStyle) {
        if (liveMotion.reduced) return
        glassRing.screen = androidx.compose.ui.geometry.Size(drawerWidthPx, drawerHeightPx)
        glassRing.density = ringDensity
        glassRing.startOnNextFrame(at ?: Offset(drawerWidthPx / 2f, drawerHeightPx / 2f), style)
    }

    /** Edit mode, begun: the wave that wakes everything runs out from [at]. */
    fun beginEditing(at: Offset?) {
        calmJob[0]?.cancel()
        calming = false
        sendWave(at, com.gabriel.zenithui.ui.theme.RingStyle.WAKE)
        editing = true
        viewModel.setEditing(true)
    }

    /**
     * Into edit mode, from a hold on the wallpaper, "Edit Home Screen" in a menu, or something being
     * picked up, at [at] on the screen. With the layout locked it says so instead, and offers to unlock.
     */
    fun startEdit(at: Offset? = null) {
        if (settings.lockLayout) {
            toast = HomeToast("The layout is locked", "Unlock") { viewModel.updateSettings { it.copy(lockLayout = false) } }
            return
        }
        holdMenu.close()
        resizingId = null
        if (!editing) beginEditing(at)
    }

    /**
     * Out of edit mode, from Done, a tap on the wallpaper, Back or Home: the calm wave runs out from
     * [at], or the middle of the screen. [quietly] settles everything at once, for leaving the home
     * screen for something that covers it.
     */
    fun endEdit(at: Offset? = null, quietly: Boolean = false) {
        if (editing) {
            if (!quietly) sendWave(at, com.gabriel.zenithui.ui.theme.RingStyle.CALM)
            if (!quietly && !liveMotion.reduced) {
                calming = true
                calmJob[0]?.cancel()
                calmJob[0] = scope.launch {
                    delay(com.gabriel.zenithui.ui.theme.RingStyle.CALM.durationMs.toLong() + BeadMotion.CALM_MS)
                    calming = false
                }
            }
        }
        editing = false
        customising = false
        resizingId = null
        // Undo keeps the last change made, for the usual few seconds, and lets go of the rest.
        viewModel.setEditing(false)
    }

    /**
     * A folder poured out of its tile as liquid glass, from [tile], where it was tapped at [tap] (both
     * on the screen): see FolderPour. A ring of light runs out from the finger as it spreads, as the
     * coming-home wave's does, but not in Lite. With movement reduced, or no tile to come out of (a
     * folder renamed from its menu has one), it only fades in.
     */
    fun openFolder(id: String, tile: androidx.compose.ui.geometry.Rect?, tap: Offset? = null) {
        hideSheet()
        folderUp = FolderUp(id, tile, tap)
        val grows = !liveMotion.reduced && tile != null
        val pace = viewModel.settings.value.animation.pace
        folderJob[0]?.cancel()
        folderJob[0] = scope.launch { folderMotion.open(grows, pace) }
        if (grows && !lite && tile != null) {
            sendWave(
                tap ?: tile.center,
                com.gabriel.zenithui.ui.theme.RingStyle.arrive(FolderPour.RING_MS / pace, FolderPour.RING_PEAK),
            )
        }
    }

    /**
     * Back into its tile, which gulps it down; only then is it gone, so the home screen is never
     * left dimmed behind nothing. The blur behind clears as it goes.
     */
    fun closeFolder() {
        val up = folderUp ?: return
        if (folderMotion.closing) return
        val pace = viewModel.settings.value.animation.pace
        folderJob[0]?.cancel()
        folderJob[0] = scope.launch {
            folderMotion.close(pace)
            if (folderUp == up) folderUp = null
            if (folderMotion.grows) FolderGulp.play(up.id, pace)
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
    /**
     * How squashed a closing card is as it lands in its icon: wider and flatter, springing back to
     * its shape (see [shrinkCard]). Nothing for an opening card.
     */
    val cardSquash = remember { Animatable(0f) }
    /** How solid a closing card still is as it gives way to the icon under it, once it is on it. */
    val cardLeaving = remember { Animatable(1f) }
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

    /** Puts all of it at [at] between two frames, with nothing seen of it. */
    suspend fun restCard(at: Float = 0f) {
        cardX.snapTo(at)
        cardY.snapTo(at)
        cardSquash.snapTo(0f)
        cardProgress.snapTo(at)
    }

    /**
     * A closing card going into its icon, as Void's does: over the Shrinking time, slowly at first,
     * the app back on the screen for a beat, and faster all the way in, its middle with it
     * (CloseRules.intoIcon), so it lands with a bump. Then, if it is to [squash], it gets wider and
     * flatter by Squash as it lands, and springs back to its shape on the landing's spring, coming
     * to rest within a pixel (CloseRules.SQUASH_REST). Waits for all of it.
     */
    suspend fun shrinkCard(squash: Boolean) = kotlinx.coroutines.coroutineScope {
        val t = liveTuning
        val into = tween<Float>(
            durationMillis = t.ms(Tune.CloseTime).roundToInt().coerceAtLeast(1),
            easing = androidx.compose.animation.core.Easing { CloseRules.intoIcon(it) },
        )
        launch { cardX.animateTo(0f, into) }
        launch { cardY.animateTo(0f, into) }
        cardProgress.animateTo(0f, into)
        val amount = t[Tune.CloseSquash]
        if (squash && amount > 0f) {
            val stiffness = t.stiffness(Tune.LandStiffness)
            val damping = t[Tune.LandDamping]
            // Sent off from its shape at the speed that takes it that far before it comes back: a
            // bump, not a jump.
            cardSquash.snapTo(0f)
            cardSquash.animateTo(
                targetValue = 0f,
                animationSpec = androidx.compose.animation.core.spring(damping, stiffness, CloseRules.SQUASH_REST),
                initialVelocity = LaunchMotion.knockVelocity(amount, stiffness, damping),
            )
        }
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
    /**
     * Back from an app after Android's own close, with a card going into the icon: the home screen
     * comes forward about the middle of its foot, over the dock, where Android's close left the
     * app (CloseRules.HOME_FOOT), as Void's does, and not about the icon.
     */
    var comeBackFromFoot by remember { mutableStateOf(false) }
    /** Set when an app covers Zenith, and let go of as Zenith comes forward again, so that happens once each time. */
    val away = remember { BooleanArray(1) }
    /** Android's question about the app being closed, as Zenith answered it (see [GestureHandshake]). */
    val handshake = remember { arrayOfNulls<GestureHandshake.Answer>(1) }
    /** The app closing this time Zenith comes back, worked out once, by whichever needs it first. */
    val closing = remember { arrayOfNulls<Closing>(1) }
    val closingWorkedOut = remember { BooleanArray(1) }
    /** The close [comeForward] has worked out, waiting for Zenith to be seen, and which time it is. */
    val forward = remember { arrayOfNulls<() -> Unit>(1) }
    val forwardTurn = remember { IntArray(1) }
    /**
     * Set while the home screen waits, empty, for Android's own close of the app to be over, so
     * that nothing of Zenith's plays hidden behind it (see AndroidClose): the arrival and the
     * close both hold until then.
     */
    val waitingForAndroid = remember { BooleanArray(1) }
    /**
     * Set while the home screen is kept empty for a card to bring it back with it (see
     * [CloseRules.bringsHome]): it has no arrival of its own this time, and shows whole, sunk back,
     * in the frame the card goes up in ([revealHome]).
     */
    val homeHidden = remember { BooleanArray(1) }
    val waitJob = remember { arrayOfNulls<kotlinx.coroutines.Job>(1) }
    /** When Zenith was last covered, and whether by an app with the screen on, which Android then closes. */
    val coveredAt = remember { LongArray(1) }
    val coveredByApp = remember { BooleanArray(1) }
    /** Where a closing card set off from, and whether it has no icon to go into and fades instead. */
    var closingFrom by remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    var closingVanishes by remember { mutableStateOf(false) }
    /**
     * Whether a closing card is on its way off the screen, towards where its app lives (see
     * CloseAway): straight there, with no arc, and not with the home screen as it comes forward.
     */
    var closingAway by remember { mutableStateOf(false) }

    // Arriving. One clock for the whole home screen: at 0 everything is out, and by the end of the
    // arrival (FlyIn.TOTAL_MS, or a liquid wave's own end) it is all in its place, where the clock is
    // left past the end of every style (FlyIn.AT_REST). It is wound back while nobody can see — at a
    // cold start, or when Zenith is stopped — and let run once the home screen is on show again.
    val flyOnStart = remember { !entrance && !motion.reduced }
    val arrivalClock = remember { Animatable(if (flyOnStart) 0f else FlyIn.AT_REST) }
    val arrivalDepth = remember { mutableFloatStateOf(FlyIn.FULL) }
    val arrivalPending = remember { BooleanArray(1).also { it[0] = flyOnStart } }
    /**
     * Coming home like liquid (Motion & feel → Coming home → Liquid): the wave everything rises on
     * this time, read by everything on the home screen while drawing. Null for any other style.
     */
    val arrivalWave = remember { mutableStateOf<LiquidWave?>(null) }
    /**
     * Where the next liquid wave starts, on the screen: the icon the app being closed goes back
     * into, worked out as Zenith comes forward. With none, as after an unlock, the middle.
     */
    val arrivalFrom = remember { arrayOfNulls<Offset>(1) }
    val power = remember(context) { context.getSystemService(android.os.PowerManager::class.java) }

    /**
     * The wave to come home on, when Coming home is Liquid, from [arrivalFrom] or the middle of the
     * screen, on the numbers in Advanced animation settings. Its light runs on the one ring of light,
     * so the screen is measured for it as the ring itself is.
     */
    fun liquidWave(): LiquidWave? {
        if (settings.arrival != LiquidArrival.STYLE) return null
        val width = drawerWidthPx.takeIf { it > 0f } ?: hostView.width.toFloat()
        val height = drawerHeightPx.takeIf { it > 0f } ?: hostView.height.toFloat()
        glassRing.screen = androidx.compose.ui.geometry.Size(width, height)
        glassRing.density = ringDensity
        val at = arrivalFrom[0] ?: Offset(width / 2f, height / 2f)
        return LiquidArrival.wave(at, liveTuning, pxPerDp) { glassRing.reachFrom(at, it) }
    }

    fun arrive() {
        if (!arrivalPending[0] || waitingForAndroid[0]) return
        arrivalPending[0] = false
        val wave = liquidWave()
        arrivalWave.value = wave
        // The light runs with the wave, lighting every pane of glass as it rises, unless it is off.
        if (wave != null && wave.light.peak > 0f) sendWave(wave.centre, wave.light)
        val total = wave?.totalMs ?: FlyIn.TOTAL_MS.toFloat()
        scope.launch {
            arrivalClock.snapTo(0f)
            arrivalClock.animateTo(total, tween(total.roundToInt(), easing = androidx.compose.animation.core.LinearEasing))
            // Arrived: past the end of every style, so a change of style finds nothing part way in.
            arrivalClock.snapTo(FlyIn.AT_REST)
        }
    }

    /**
     * The home screen kept empty for a closing card is shown whole, in its place, from this frame,
     * with no arrival: the card is going up over it, and it comes forward as the card shrinks. The
     * clock is set there and then, not on the next turn of the loop, so the frame the card goes up
     * in already has it.
     */
    fun revealHome() {
        if (!homeHidden[0]) return
        homeHidden[0] = false
        arrivalPending[0] = false
        arrivalWave.value = null
        scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { arrivalClock.snapTo(FlyIn.AT_REST) }
    }
    LaunchedEffect(Unit) {
        // A cold start: once the first frame is up.
        androidx.compose.runtime.withFrameNanos { }
        arrive()
    }
    // A Look put on: Settings and any sheet go, the first page comes round, and the home screen
    // arrives in its new colours the way it does after an unlock.
    LaunchedEffect(Unit) {
        viewModel.lookPut.collect {
            settingsOpen = false
            hideSheet()
            pagerState.scrollToPage(0)
            if (!liveMotion.reduced) {
                arrivalDepth.floatValue = FlyIn.FULL
                arrivalFrom[0] = null
                arrivalPending[0] = true
                arrive()
            }
        }
    }
    // A phone that had Zenith before the new look is asked once whether to take it (NewLook.due),
    // over the home screen at rest with nothing open on it, so the question is not lost among
    // icons still arriving. Once it is up it stays until it is answered: meanwhile nothing else on
    // the home screen answers a touch or Back, and the home button runs no gesture, only bringing
    // the first page round under it. Read through a state, since the two-finger gestures and the
    // home button read it long after it was composed.
    var newLookReady by remember { mutableStateOf(false) }
    val askingNewLook by androidx.compose.runtime.rememberUpdatedState(newLookReady && NewLook.due(settings))
    // A Look the schedule has due waits for a moment with nothing under the finger and nothing
    // open over the home screen, and then goes on as one put on by hand does.
    val lookDue by viewModel.lookDue.collectAsState()
    val idle = !dragging && drag.item == null && !sheetVisible && !settingsOpen && !widgetsOpen &&
        !drawerShown && folderUp == null && card == null && resizingId == null && !ambient && !overview &&
        !askingNewLook && !searchShown && holdMenu.open == null && alert == null && !editing && !customising
    LaunchedEffect(lookDue, idle) {
        if (lookDue != null && idle) {
            delay(LOOK_DUE_WAIT_MS)
            viewModel.applyDueLook()
        }
    }
    // Where every icon sits at rest, for an app closing back into one (see IconPlaces). Asked as
    // each icon moves, so no place is taken down while the home screen is sunk behind an app,
    // zoomed about a card, flying in, or knocked about by an icon landing.
    val restingNow by androidx.compose.runtime.rememberUpdatedState(idle)
    androidx.compose.runtime.DisposableEffect(Unit) {
        val atRest = {
            restingNow && card == null && !holdSunk && !homeSink.isRunning && abs(homeSink.value) < 0.002f &&
                arrivalClock.value >= (arrivalWave.value?.totalMs ?: FlyIn.TOTAL_MS.toFloat()) - 0.5f &&
                !AppLanding.busy && !HomeRipple.swinging &&
                !pagerState.isScrollInProgress && abs(pagerState.currentPageOffsetFraction) < 0.002f
        }
        IconPlaces.atRest = atRest
        onDispose { if (IconPlaces.atRest === atRest) IconPlaces.atRest = { false } }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { IconPlaces.atRest() }.collect { resting ->
            if (resting) {
                // A frame on, so every icon has been drawn where it now stays.
                androidx.compose.runtime.withFrameNanos { }
                IconPlaces.cameToRest()
            }
        }
    }
    // Answered either way, it is not due any more, and the next time it is (a backup from before
    // the new look, restored) it waits for the home screen to be at rest again.
    val newLookDue = NewLook.due(settings)
    LaunchedEffect(newLookDue, idle) {
        if (!newLookDue) {
            newLookReady = false
        } else if (idle) {
            delay(NEW_LOOK_WAIT_MS)
            newLookReady = true
        }
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

    fun steerCard(
        start: kotlinx.coroutines.CoroutineStart = kotlinx.coroutines.CoroutineStart.DEFAULT,
        block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit,
    ) {
        cardJob[0]?.cancel()
        cardJob[0] = scope.launch(start = start, block = block)
    }

    /** The app's icon arriving back in its place, and the icons round it knocked back by it. A folder only knocks. */
    suspend fun landIcon(key: String?, at: androidx.compose.ui.geometry.Rect?) = kotlinx.coroutines.coroutineScope {
        val t = liveTuning
        val ripple = t[Tune.Ripple] * pxPerDp
        if (at != null && ripple > 0f) {
            // The ripple swings on the landing's own spring, a little slacker, so it trails it.
            launch {
                HomeRipple.knock(at.center, ripple, t.stiffness(Tune.LandStiffness) * 0.6f, t[Tune.LandDamping])
            }
        }
        if (key != null) AppLanding.land(key, t[Tune.LandScale], t[Tune.LandFade], t.springOf(Tune.LandStiffness, Tune.LandDamping))
    }

    /**
     * Android has sprung the app's own window into its icon: the icon takes it with a small swell
     * and the icons round it are knocked back, both sent off at speed from where they stand.
     */
    suspend fun receiveWindow(key: String?, at: androidx.compose.ui.geometry.Rect) = kotlinx.coroutines.coroutineScope {
        val t = liveTuning
        val ripple = t[Tune.Ripple] * pxPerDp
        if (ripple > 0f) {
            launch { HomeRipple.knock(at.center, ripple, t.stiffness(Tune.LandStiffness) * 0.6f, t[Tune.LandDamping]) }
        }
        if (key != null) AppLanding.swell(key, (t[Tune.LandScale] - 1f) * 0.4f, t.stiffness(Tune.LandStiffness), t[Tune.LandDamping])
    }

    /** Where Zenith's window is on the screen, for telling Android where an icon is. */
    fun windowOnScreen(): IntArray = IntArray(2).also { hostView.getLocationOnScreen(it) }

    /**
     * Android asks where the closing app's icon is (see [GestureHandshake]): answered at once,
     * with the place the icon is in when the home screen is at rest, which is where it will be by
     * the time the window gets there. Closing Plain, or with movement reduced, it is not answered
     * and Android closes the app its own way.
     *
     * One UI asks too, and then closes the app into the middle of the screen whatever it is told,
     * never saying the window got anywhere (AndroidClose.followsHandshake). There it is told there
     * is no icon, so that it does its own, and the app goes into its icon Zenith's own way after.
     */
    fun answerHandshake(ask: GestureHandshake) {
        val now = android.os.SystemClock.uptimeMillis()
        handshake[0] = null
        if (viewModel.settings.value.closeAnimation == CloseRules.PLAIN || liveMotion.reduced) return
        if (!AndroidClose.followsHandshake(AndroidClose.makeHere())) {
            ask.answer(null) {}
            return
        }
        val opened = returningCard[0]?.takeIf {
            CloseRules.packageOf(it.key) == ask.packageName && CloseRules.isWork(it.key) == !ask.personal
        }
        val spot = IconPlaces.find(opened?.key ?: ask.key, ask.packageName, work = !ask.personal, near = opened?.from)
        // The app as its icon on the home screen has it, which may be another of its screens.
        val answer = GestureHandshake.Answer(
            key = spot?.takeIf { !it.folder }?.key ?: opened?.key ?: ask.key,
            at = now,
            target = spot?.bounds,
            folder = spot?.folder == true,
        )
        handshake[0] = answer
        val window = windowOnScreen()
        ask.answer(spot?.let { GestureHandshake.onScreen(it.bounds, window[0], window[1]) }) { answer.arrived.complete(Unit) }
    }

    /**
     * Which app is closing as Zenith comes back, which icon it goes into, and how (see
     * [CloseRules.route]). The app is the one Android asked about, failing that the one the
     * picture helper saw in front, failing that the one opened from Zenith. Worked out once each
     * time, by the first to need it: the home button deciding whether to stay on this page, or
     * the home screen coming forward.
     */
    fun workOutClosing(): Closing? {
        val now = android.os.SystemClock.uptimeMillis()
        // Once worked out, it holds while Zenith is coming back, and not for a home press after.
        if (closingWorkedOut[0]) return closing[0]?.takeIf { away[0] || now - it.at <= COMING_BACK_MS }
        if (!away[0]) return null
        closingWorkedOut[0] = true
        val current = viewModel.settings.value
        val style = if (liveMotion.reduced) CloseRules.PLAIN else current.closeAnimation
        val answer = handshake[0]?.takeIf { it.fresh(now) }
        handshake[0] = null
        val opened = returningCard[0]
        val seen = if (answer == null) AppPictures.closingApp(now) else null
        val key: String
        val pkg: String
        when {
            answer != null -> {
                key = answer.key
                pkg = CloseRules.packageOf(answer.key)
            }
            // Another app than the one opened from Zenith: one it opened, say, or one opened from a notification.
            seen != null && (opened == null || CloseRules.packageOf(opened.key) != seen) -> {
                pkg = seen
                key = apps.firstOrNull { it.packageName == seen && !it.work }?.key ?: CloseRules.keyOf(seen, "", work = false)
            }
            opened != null -> {
                key = opened.key
                pkg = CloseRules.packageOf(opened.key)
            }
            else -> return null
        }
        val work = CloseRules.isWork(key)
        val back = opened?.takeIf { CloseRules.packageOf(it.key) == pkg && CloseRules.isWork(it.key) == work }
        val spot = if (answer != null) {
            answer.target?.let { IconPlaces.Spot(listOf(answer.key), it, folder = answer.folder) }
        } else {
            IconPlaces.find(back?.key ?: key, pkg, work, near = back?.from)
        }
        // With no icon of it on show, it goes off the screen towards where it lives: its page, the
        // drawer below or the library to the side (CloseAway).
        val away = if (spot == null) {
            CloseAway.way(
                key, pkg, work,
                items = viewModel.layout.value,
                // On show, its icon there would have been found: this is a dock put away, below.
                dock = current.dock,
                shownPage = pagerState.currentPage,
                library = current.appsView == "library",
            )
        } else {
            null
        }
        // Opened from Zenith a long time ago and never seen closing: maybe not the app closing now.
        val recent = back == null || (launchedAt[0] != 0L && now - launchedAt[0] < CLOSE_WINDOW_MS)
        // A picture from this time it was open, so it shows what was on the screen as it closed,
        // the same way up as the screen is now.
        val since = AppPictures.visitStart(pkg) ?: Long.MAX_VALUE
        val picture = if (work || style == CloseRules.PLAIN) {
            null
        } else {
            AppPictures.forClosing(pkg, since, now)?.takeIf {
                PictureRules.fits(it.width, it.height, hostView.width.toFloat(), hostView.height.toFloat())
            }
        }
        val route = CloseRules.route(style, asked = answer != null, target = spot != null, picture = picture != null, recent = recent, cardFrom = liveTuning[Tune.CloseCard])
        return Closing(key, pkg, spot, away, back, route, picture, answer, at = now).also { closing[0] = it }
    }

    /**
     * A card shrinking back into the icon the app is closing into, or with none on show off the
     * screen towards where the app lives (CloseAway: down into the drawer, or to the side into the
     * library or towards its page), or failing that into the middle of the screen, fading as it
     * goes: holding a picture of the app if there is one, and the icon's own colour with the icon
     * blown up on it otherwise (the Closing card).
     *
     * As Void's does: it fills the screen again, shrinks onto the icon and lands a little
     * flattened, and is the icon by then, so it gives way to it in a few frames with no landing of
     * its own (see [shrinkCard]). The icon's name stays on show all the while (AppLanding).
     *
     * With [hold] it goes up in this very frame and waits, full size, until that is done: on a
     * phone that shows a still picture of Zenith before its own frames (CloseRules.cardFirst).
     */
    fun closeIntoIcon(closed: Closing, bringHome: (() -> Unit)? = null, hold: kotlinx.coroutines.Deferred<Unit>? = null) {
        val t = liveTuning
        val picture = closed.picture?.asImageBitmap()
        val from = if (closed.route == CloseRoute.Picture) t[Tune.ClosePicture] else CloseRules.cardStart(t[Tune.CloseCard])
        val spot = closed.spot
        val side = closed.opened?.from?.minDimension ?: (60f * pxPerDp)
        val screenWidth = hostView.width.toFloat()
        val screenHeight = hostView.height.toFloat()
        val away = closed.away.takeIf { spot == null }
        val target = spot?.bounds
            ?: away?.let { CloseAway.target(it, screenWidth, screenHeight, side) }
            ?: CloseRules.middle(screenWidth, screenHeight, side)
        val landKey = closed.landKey
        // The icon stays out of sight until the card is on it, and is shown under it as the card gives way.
        if (landKey != null) AppLanding.hold(landKey)
        val opened = closed.opened
        // A super icon's own stretched picture, if it is going back into that same super icon.
        val wide = opened?.wideIcon?.takeIf { spot != null && (spot.bounds.center - opened.from.center).getDistance() < 4f * pxPerDp }
        val iconKey = landKey ?: closed.key
        // Held up, it is set there and then, so the frame Zenith draws next already has it.
        steerCard(if (hold != null) kotlinx.coroutines.CoroutineStart.UNDISPATCHED else kotlinx.coroutines.CoroutineStart.DEFAULT) {
            // Whatever happens to the card, the home screen it was bringing back comes forward.
            var broughtHome = bringHome == null
            try {
                // Again, in case a card it took over from let go of it on its way out.
                if (landKey != null) AppLanding.hold(landKey)
                val icon = opened?.icon ?: viewModel.icons.peek(iconKey, 192)
                    ?: runCatching { viewModel.icons.load(iconKey, 192) }.getOrNull()
                card = AppExpansion(key = closed.key, from = target, icon = icon, wideIcon = wide, picture = picture)
                cardOpening = false
                closingCard = true
                closingFrom = from
                // Off the screen it is gone as it gets there; only into the middle does it fade.
                closingVanishes = spot == null && away == null
                closingAway = away != null
                cardLeaving.snapTo(1f)
                restCard(from)
                // Bringing the home screen back with it: shown whole behind it from this frame, and
                // on its way forward once the card is well on its way, so it is seen coming forward.
                revealHome()
                // Up at once, the app back on the screen, until Android's close is waited out.
                hold?.await()
                if (bringHome != null) {
                    launch {
                        snapshotFlow { cardProgress.value }.first { it <= CloseRules.HOME_GOES_AT }
                        broughtHome = true
                        bringHome()
                    }
                }
                // Off the screen, or into the middle of it, with no icon to go into, it is gone by the
                // time it gets there.
                shrinkCard(squash = spot != null)
                if (spot != null) {
                    // On the icon, and as good as it: the icon is shown under it and it gives way, so an
                    // icon drawn its own way (glass, a shape of its own) takes over without a jump.
                    AppLanding.hold(null)
                    cardLeaving.animateTo(0f, tween(CloseRules.HAND_OVER_MS, easing = androidx.compose.animation.core.LinearEasing))
                }
                card = null
                closingCard = false
                restCard()
            } finally {
                // Turned round or cut short: never leave an icon hidden, the home screen empty, or a
                // card that cannot be tapped.
                AppLanding.hold(null)
                closingCard = false
                closingVanishes = false
                closingAway = false
                revealHome()
                // Put away because Zenith was covered again, it is let go of at rest there (ON_STOP),
                // and not sent forward while nobody can see it.
                val seen = hostLifecycle?.currentState?.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED) != false
                if (!broughtHome && seen) bringHome?.invoke()
            }
        }
    }

    /** Sets off the close [comeForward] worked out, once, if it has not gone yet and nothing holds it. */
    fun setOff() {
        if (waitingForAndroid[0]) return
        val go = forward[0] ?: return
        forward[0] = null
        go()
    }

    /** Android's own close is over, or is not waited for any more: the home screen arrives, and the app goes into its icon. */
    fun endWait() {
        if (!waitingForAndroid[0]) return
        waitingForAndroid[0] = false
        arrive()
        setOff()
    }

    /**
     * Closing an app, once Zenith is on show again: the home screen comes forward, and the app goes
     * back into its icon whichever way this phone allows (see [workOutClosing]): Android's own
     * window sprung into it, a picture of the app shrinking into it, the card it opened on, or the
     * icon landing on its own.
     *
     * Worked out as Zenith resumes. Back from an app with nothing of Android's coming into the icon,
     * Android plays its own close of the app first, over Zenith, which is already in front under it:
     * the home screen waits for that, empty, and only then do the icons come in and the app go into
     * its icon (see AndroidClose). Otherwise it is set off once Zenith can be seen: at once with its
     * window in front, or as the window gets focus or [CloseRules.SIGHT_WAIT_MS] later, whichever
     * comes first. Until it goes the home screen stays sunk back and the icon out of sight, so the
     * first frame seen is the start of the close.
     *
     * After that wait, a card going into the icon brings the home screen back with it, as Void's
     * does (see [CloseRules.bringsHome]): shown whole as the card goes up, sunk back, and coming
     * forward about the middle of its foot as the card shrinks, with no arrival of its own.
     */
    fun comeForward() {
        if (!away[0]) return
        val closed = workOutClosing()
        away[0] = false
        val sunk = returningKey[0] != null
        returningKey[0] = null
        returningCard[0] = null
        launchedAt[0] = 0L
        // Everything zooms, and ripples, about the icon the app goes back into, and coming home
        // like liquid its wave starts there.
        val spot = closed?.spot
        val about = spot?.bounds?.center ?: closed?.opened?.from?.center
        if (about != null) HomeRipple.origin = about
        arrivalFrom[0] = about
        // A card or a picture on its way into the icon: the icon is out of sight from the first frame.
        closed?.takeIf { it.route == CloseRoute.Card || it.route == CloseRoute.Picture }?.landKey?.let(AppLanding::hold)
        // Android's own window going into the icon is Android's, and the icons are wanted in their
        // places for it to land in, so that is not waited for; nor is anything with movement reduced.
        val fromApp = coveredByApp[0]
        coveredByApp[0] = false
        val plan = if (fromApp && closed?.answer == null && !liveMotion.reduced) {
            AndroidClose.planHere(context, liveTuning[Tune.CloseWait])
        } else {
            null
        }
        val withCard = CloseRules.bringsHome(waited = plan != null, route = closed?.route)
        // On a phone that shows a still picture of Zenith before its own frames, the card is up
        // from the start and only let go once the wait is over (CloseRules.cardFirst).
        val held = if (closed != null && CloseRules.cardFirst(plan, closed.route)) kotlinx.coroutines.CompletableDeferred<Unit>() else null
        val comeBack: () -> Unit = {
            scope.launch {
                // Snap to exactly where the hold leaves off before letting go of it, so the hand-over
                // from holding to animating happens between two frames with nothing visible in it.
                homeSink.snapTo(1f)
                holdSunk = false
                homeSink.animateTo(0f, liveTuning.springOf(Tune.ReturnStiffness, Tune.ReturnDamping))
                comeBackFromFoot = false
            }
        }
        val turn = ++forwardTurn[0]
        forward[0] = go@{
            // Brought back by a card, it comes forward when the card says (see closeIntoIcon).
            if (sunk && !withCard) comeBack()
            closed ?: return@go
            when (closed.route) {
                CloseRoute.None -> Unit
                CloseRoute.Land -> scope.launch { landIcon(closed.landKey, spot?.bounds) }
                CloseRoute.Handshake -> scope.launch {
                    // The window is Android's to move: once it says it has got there, or after a moment
                    // in case it never does, the icon takes it.
                    kotlinx.coroutines.withTimeoutOrNull(CloseRules.HANDSHAKE_WAIT_MS) { closed.answer?.arrived?.await() }
                    if (spot != null) receiveWindow(closed.landKey, spot.bounds)
                }
                // Already up and waiting: let go of it.
                CloseRoute.Card, CloseRoute.Picture -> if (held != null) {
                    held.complete(Unit)
                } else {
                    closeIntoIcon(closed, bringHome = if (withCard) comeBack else null)
                }
            }
        }
        if (plan != null) {
            // Empty from the first frame: everything is wound right back, and comes in from there,
            // or with a card to bring it back is shown whole, sunk back, as the card goes up.
            waitingForAndroid[0] = true
            arrivalDepth.floatValue = FlyIn.FULL
            if (withCard) {
                homeHidden[0] = true
                arrivalPending[0] = false
                holdSunk = true
                comeBackFromFoot = true
            }
            // The card up now, full size, over the home screen sunk back behind it, so the still
            // picture the phone shows of Zenith before its own frames is the app back on the screen.
            if (held != null && closed != null) closeIntoIcon(closed, bringHome = comeBack, hold = held)
            val startedAt = android.os.SystemClock.uptimeMillis()
            val since = coveredAt[0]
            waitJob[0]?.cancel()
            waitJob[0] = scope.launch {
                AndroidClose.awaitSetOff(plan, startedAt, since)
                if (forwardTurn[0] == turn) endWait()
            }
        } else if (hostView.hasWindowFocus()) {
            setOff()
        } else {
            scope.launch {
                delay(CloseRules.SIGHT_WAIT_MS)
                if (forwardTurn[0] == turn) setOff()
            }
        }
    }

    // Android's question about where a closing app's icon is comes with the home intent, before the
    // home screen has seen that intent, and has to be answered at once (see GestureHandshake).
    androidx.compose.runtime.DisposableEffect(Unit) {
        val answer: (GestureHandshake) -> Unit = { answerHandshake(it) }
        GestureHandshake.answerer = answer
        onDispose { if (GestureHandshake.answerer === answer) GestureHandshake.answerer = null }
    }

    /** How far back the home screen is: 0 at rest, 1 behind an app. Read while drawing. */
    fun homeDepth(): Float = when {
        card != null && !closingCard -> cardProgress.value.coerceIn(0f, 1f)
        holdSunk -> 1f
        else -> homeSink.value.coerceIn(-0.12f, 1f)
    }

    /**
     * How far the home screen is zoomed for how far back it is: Home screen zoom behind an app, 1 at
     * rest, and a little past 1 as it comes forward too far. Read while drawing.
     */
    fun homeZoom(): Float = 1f + (liveTuning[Tune.HomeScale] - 1f) * homeDepth()

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
        comeBackFromFoot = false
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
        // A close still waiting for Zenith to be seen, or for Android's own close, goes now, so the
        // opening takes over from it.
        endWait()
        setOff()
        revealHome()
        val current = card
        if (current != null && current.key == key) {
            openCard(current)
            return
        }
        val from = LaunchOrigin.boundsFor(key)
        if (from == null) {
            if (!viewModel.open(key) && ShortcutKeys.isShortcut(key)) {
                Toast.makeText(context, "The app does not offer that shortcut any more", Toast.LENGTH_SHORT).show()
            }
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
            // With the app picture helper on, a picture of the app as it was left, so it is seen
            // coming out of its icon. Not a shortcut's, which opens somewhere else in it, nor a
            // work app's, which the helper cannot tell from the personal one.
            val picture = if (ShortcutKeys.isShortcut(key) || CloseRules.isWork(key)) {
                null
            } else {
                AppPictures.forOpening(CloseRules.packageOf(key))
                    ?.takeIf { PictureRules.fits(it.width, it.height, hostView.width.toFloat(), hostView.height.toFloat()) }
                    ?.asImageBitmap()
            }
            preparingLaunch = false
            // A different app starts from its own icon.
            if (card != null) {
                cardJob[0]?.cancel()
                restCard()
            }
            launchedAt[0] = 0L
            openCard(AppExpansion(key = key, from = from, icon = icon, wideIcon = wideIcon, picture = picture))
        }
    }

    androidx.compose.runtime.DisposableEffect(hostLifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    // Whatever covers Zenith now is what closes when it comes back: worked out afresh.
                    away[0] = true
                    closingWorkedOut[0] = false
                    closing[0] = null
                    // Covered with the screen on is an app coming over Zenith, which Android closes
                    // its own way when it goes; with the screen off the next thing is an unlock.
                    coveredAt[0] = android.os.SystemClock.uptimeMillis()
                    coveredByApp[0] = power?.isInteractive != false
                    // Covered again while waiting for the last close to be over: that wait is dropped
                    // with the close it held, below.
                    waitJob[0]?.cancel()
                    waitJob[0] = null
                    waitingForAndroid[0] = false
                    homeHidden[0] = false
                    comeBackFromFoot = false
                    // Covered again before the last close was seen: it is dropped, its icon shown and
                    // the home screen let go of, at rest, so nothing is left waiting for it.
                    if (forward[0] != null) {
                        forward[0] = null
                        // A card held up for it is put away with it (see CloseRules.cardFirst), even
                        // one still fetching its icon. A card opening an app is dealt with below.
                        if (!cardOpening) {
                            cardJob[0]?.cancel()
                            card = null
                            closingCard = false
                            scope.launch { restCard() }
                        }
                        AppLanding.hold(null)
                        holdSunk = false
                        scope.launch { homeSink.snapTo(0f) }
                    }
                    // Search folds back into its pill while nobody can see it, so coming home finds
                    // the home screen and not a field without its keyboard.
                    closeSearch()
                    // Wind the arrival back now, unseen. Stopped with the screen off, the next
                    // thing seen is an unlock: the full fly-in. Stopped behind an app: a settle.
                    if (!liveMotion.reduced) {
                        arrivalDepth.floatValue = if (power?.isInteractive == false) FlyIn.FULL else FlyIn.SETTLE
                        // From the middle, unless an app is closed on the way home (see comeForward).
                        arrivalFrom[0] = null
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

                // Resumed is when Zenith is on its way to the front again: the close is worked out
                // now, and set off once Zenith can be seen (see comeForward). Window focus, below,
                // is when that is on the phones that resume before the home screen is shown.
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
    // screen appeared, and resuming is part of the same step as starting. Window focus arrives as
    // the home screen is shown: that is when One UI's grid swells back and the app goes into its icon.
    androidx.compose.runtime.DisposableEffect(hostView) {
        val listener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { focused ->
            if (focused) {
                comeForward()
                setOff()
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

    // While the pages turn, and for a second after they come to rest, the Search pill's words give
    // way to the page dots (pillDots). Moved on here and read by the pill only while it draws, so
    // paging composes nothing for it.
    val pillDotsShown = remember { Animatable(0f) }
    LaunchedEffect(pagerState) {
        // Until the pages have moved there is no rest to count the second from.
        var moved = false
        snapshotFlow { pagerState.isScrollInProgress }.collectLatest { moving ->
            if (moving) moved = true
            val restedAt = android.os.SystemClock.uptimeMillis()
            fun sinceRest() = if (moving || !moved) -1L else android.os.SystemClock.uptimeMillis() - restedAt
            val spec: androidx.compose.animation.core.AnimationSpec<Float> =
                if (liveMotion.reduced) tween(Motion.REDUCED_MS) else Motion.page()
            while (pillDots(moving, sinceRest())) {
                pillDotsShown.animateTo(1f, spec)
                // While they move the dots stay; the next change of mind starts this again.
                if (moving) return@collectLatest
                delay((PILL_DOTS_LINGER_MS - sinceRest()).coerceAtLeast(1L))
            }
            pillDotsShown.animateTo(0f, spec)
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

    /** One of Android's settings that a search found by its name, or Zenith's own Settings. */
    fun openSetting(link: com.gabriel.zenithui.data.SettingsLinks.Link) {
        if (link.action == com.gabriel.zenithui.data.SettingsLinks.ZENITH) {
            settingsOpen = true
        } else {
            val opened = runCatching {
                context.startActivity(Intent(link.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.isSuccess
            if (!opened) runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }

    /** A sum's answer or a conversion that a search found, onto the clipboard. */
    fun copyAnswer(text: String) {
        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Zenith", text))
        // Android 13 and later say so themselves.
        if (android.os.Build.VERSION.SDK_INT < 33) Toast.makeText(context, "Copied $text", Toast.LENGTH_SHORT).show()
    }

    /**
     * Does what a gesture was set to do. Anything that needs something Zenith does not have yet —
     * the lock helper, a torch — says so rather than doing nothing.
     */
    fun runGesture(action: String) {
        when (action) {
            GestureActions.NONE -> Unit
            GestureActions.NOTIFICATIONS -> if (!SystemPanels.notifications(context)) {
                Toast.makeText(context, "This phone does not let Zenith pull the notifications down", Toast.LENGTH_SHORT).show()
            }
            GestureActions.QUICK_SETTINGS -> if (!SystemPanels.quickSettings(context)) {
                Toast.makeText(context, "This phone does not let Zenith pull the quick settings down", Toast.LENGTH_SHORT).show()
            }
            GestureActions.LOCK -> if (!LockService.lock()) showSheet(HomeSheet.LockHelper)
            GestureActions.DRAWER -> openDrawer()
            // The Search pill's search, its glass stretching out of the pill, with the keyboard up.
            GestureActions.SEARCH -> openSearch()
            GestureActions.TORCH -> if (!Torch.toggle(context)) {
                Toast.makeText(context, "There is no torch Zenith can use on this phone", Toast.LENGTH_SHORT).show()
            }
            GestureActions.AMBIENT -> ambient = true
            GestureActions.CUSTOMISE -> customising = true
            GestureActions.SETTINGS -> settingsOpen = true
            GestureActions.WIDGETS -> widgetsOpen = true
            GestureActions.FIRST_PAGE -> scope.launch { if (pagerState.currentPage != 0) pagerState.animateScrollToPage(0) }
            GestureActions.OVERVIEW -> overview = true
            else -> {
                val app = GestureActions.appKey(action)
                when {
                    app != null -> launchApp(app)
                    GestureActions.isShortcut(action) -> launchApp(action)
                }
            }
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

    // A drag on the page is told apart by how it starts. Up is the drawer, as it always was. Down,
    // with the drawer shut, is the swipe-down gesture: the page comes a little way after the finger,
    // on a band that gives less the further it is pulled, a hint at the top says what letting go
    // will do and lights up once it will, and the page springs home when it is let go.
    val swipeDownAction by androidx.compose.runtime.rememberUpdatedState(Gestures.actionFor(Gestures.SWIPE_DOWN, settings.gestures))
    var dragKind by remember { mutableIntStateOf(DRAG_UNDECIDED) }
    var pullDown by remember { mutableFloatStateOf(0f) }
    var pulling by remember { mutableStateOf(false) }
    val pullBack = remember { Animatable(0f) }
    /** Set by the watcher over the whole screen while a second finger is down: it is a two-finger gesture, not a drag. */
    var twoFingers by remember { mutableStateOf(false) }
    val pullFirePx = with(LocalDensity.current) { PULL_FIRE.toPx() }

    /** How far down the swipe-down gesture has the page: under the finger, or springing home. */
    fun pulled(): Float = if (pulling) pullDown else pullBack.value

    fun pageDrag(delta: Float) {
        if (twoFingers) return
        if (dragKind == DRAG_UNDECIDED) {
            dragKind = if (delta > 0f && drawer.value <= 0.001f && !dragging && swipeDownAction != GestureActions.NONE) DRAG_PULL else DRAG_DRAWER
        }
        if (dragKind == DRAG_PULL) {
            pulling = true
            pullDown = (pullDown + delta).coerceAtLeast(0f)
        } else if (!libraryMode) {
            dragDrawer(delta)
        }
    }

    fun pageDragStopped(velocity: Float) {
        val kind = dragKind
        dragKind = DRAG_UNDECIDED
        if (kind == DRAG_PULL) {
            val from = pullDown
            // Far enough, or flicked: it happens. A flick still has to mean it.
            val armed = from >= pullFirePx || (velocity > 1400f && from > pullFirePx * 0.35f)
            scope.launch {
                pullBack.snapTo(from)
                pulling = false
                pullDown = 0f
                pullBack.animateTo(0f, Motion.drop())
            }
            if (armed && !twoFingers) runGesture(swipeDownAction)
        } else if (!libraryMode) {
            settleDrawer(-velocity)
        }
    }

    // A tap on the wallpaper sends a ring of light out from under the finger, across the whole
    // screen: every pane of glass lights up as it passes, and the icons catch a gleam. It takes the
    // place of the small ring a double tap used to send, and a double tap sends it too. None with
    // movement reduced. (The ring itself, which edit mode's waves are too, is made further up.)
    /** A ring from [at], on the screen in pixels. */
    fun ringAt(at: Offset) {
        // Asked as the tap comes, not when the page's tap listener was set up, which outlives a
        // change to Motion: that listener holds on to this function as it was then.
        if (liveMotion.reduced) return
        // The screen as the home screen is measured now, which is the whole window.
        glassRing.screen = androidx.compose.ui.geometry.Size(drawerWidthPx, drawerHeightPx)
        glassRing.density = ringDensity
        // The frame clock counts in System.nanoTime's nanoseconds, as Android's frames do.
        glassRing.start(at, System.nanoTime())
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
    val jiggleStrength = animateFloatAsState(if ((drag.isDragging || ((editing || calming) && !customising)) && !motion.reduced) 1f else 0f, tween(200), label = "jiggle")
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
    // The page in view, as far as carrying something cares: only while something is carried, so a
    // swipe on its own does not compose the whole home screen again half way through.
    val carriedPage by remember { derivedStateOf { if (drag.isDragging) pagerState.currentPage else -1 } }
    LaunchedEffect(dropPlan?.target, drag.hoverCandidateId, overRemove, carriedPage) {
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

    // Another app's widget has gone onto the home screen, after whatever Android asked: turn to it.
    LaunchedEffect(Unit) {
        viewModel.widgetPlaced.collect { page -> revealPage(page) }
    }
    // How big a cell and its icons are, in dp, so another app's widget is given the cells its own
    // size needs.
    val pageGeometry = drag.geometry
    LaunchedEffect(pageGeometry) {
        val g = pageGeometry ?: return@LaunchedEffect
        viewModel.noteCellSize(g.cellWidth / density.density, g.unitHeight * ROW_UNITS / density.density, g.iconDp)
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
            val gathered = drag.gathered.map { it.first.id }
            when {
                droppedOnDock -> {
                    // Where it was let go among the dock's apps decides its place: after every app
                    // whose middle is left of the finger, as moving an app along the dock decides it.
                    // Sharing the slab out evenly put it a place off where the apps do not stand
                    // evenly across it, as with two or three on the glass dock or a floating one.
                    val index = dockCentres.count { it < drag.pointer.x }
                    if (viewModel.moveToDock(item.id, index)) {
                        drag.sent = Sent(item, copyAt, into = null)
                    } else {
                        Toast.makeText(context, "The dock is full — drag one out first", Toast.LENGTH_SHORT).show()
                    }
                }
                drag.removeArmed -> {
                    drag.sent = Sent(item, copyAt, into = drag.removeZone.center, faint = true)
                    // What was gathered goes with it, and comes back with it on Undo.
                    viewModel.removeItems(listOf(item.id) + gathered)
                }
                hover != null -> {
                    drag.sent = Sent(item, copyAt, into = layout.firstOrNull { it.id == hover }?.let { drag.iconCentre(it) })
                    viewModel.combine(item.id, hover)
                }
                else -> {
                    dropOnPage(item)
                    // Gathered icons land round it, side by side, from where you let go.
                    if (gathered.isNotEmpty()) viewModel.landBeside(item.id, gathered)
                }
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

    // A page you added but left empty is forgotten once you swipe away from it. Watched rather than
    // read while composing, so a page coming to rest composes nothing here.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage to drag.isDragging }.collect { (settled, dragging) ->
            if (!dragging) viewModel.trimPages(settled)
        }
    }

    var hintDismissed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(sheetVisible) {
        if (!sheetVisible) {
            kotlinx.coroutines.delay(300)
            sheet = null
        }
    }

    val designedBlur = animateDpAsState(
        // The liquid glass opening frosts the home screen behind the growing glass.
        targetValue = when {
            // (Behind the drawer the home screen frosts over by a blurred copy fading in instead:
            // see DrawerFrost.)
            // An open folder has the home screen out of focus behind it; it clears as the folder goes.
            folderUp != null && !folderMotion.closing -> 14.dp
            // So has Search, open out of its pill; below Android 12 the blur does nothing, and the
            // field's dim alone is behind it.
            searchOpen -> 16.dp
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
    // As far as the theme blurs what is behind: Liquid Glass's is the blur as it is drawn, and Clean
    // and Retro have none, only the dim. Nor has Lite.
    val blurShare = blurBehind.value / com.gabriel.zenithui.data.Themes.LiquidGlass.blurBehind
    val homeBlur = designedBlur.value * blurShare
    val homePressed by viewModel.homePressed.collectAsState()
    val environmentTick by viewModel.environmentTick.collectAsState()
    val wallpaperIncoming by viewModel.wallpaperIncoming.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.wallpaperRefused.collect {
            Toast.makeText(context, "Android would not set that wallpaper", Toast.LENGTH_SHORT).show()
        }
    }
    val isDefaultLauncher = remember(homePressed, environmentTick) { isDefaultLauncher(context) }
    val seenAgain = remember { intArrayOf(viewModel.homeAgain.value) }
    LaunchedEffect(homePressed) {
        if (homePressed > 0) {
            // Pressed with Zenith already in front, and nothing open on it: the home button
            // gesture, whatever that has been set to. Anything else folds everything back.
            val again = viewModel.homeAgain.value != seenAgain[0]
            seenAgain[0] = viewModel.homeAgain.value
            val busy = (card != null && cardOpening) || sheetVisible || settingsOpen || widgetsOpen ||
                folderUp != null || drawer.value > 0.02f || resizingId != null || ambient || askingNewLook || searchOpen ||
                holdMenu.open != null || alert != null || editing || customising
            val action = Gestures.actionFor(Gestures.HOME, viewModel.settings.value.gestures)
            // Home while an app is still opening sends it straight back into its icon.
            if (card != null && cardOpening) closeCard()
            hideSheet()
            holdMenu.close()
            alert = null
            endEdit()
            settingsOpen = false
            widgetsOpen = false
            closeFolder()
            closeDrawer()
            closeSearch()
            // Coming back from an app that goes back into an icon on show stays on that page, so the
            // app can be seen going into it, and so does one going off towards its page, so the way
            // it goes is the way to it (CloseAway.staysOnPage). Anything else goes back to the first page.
            val staysHere = !again && workOutClosing()?.let { CloseAway.staysOnPage(it.spot != null, it.away, it.route) } == true
            if (again && !busy && action != GestureActions.FIRST_PAGE) {
                runGesture(action)
            } else if (pagerState.currentPage != 0 && !staysHere) {
                pagerState.animateScrollToPage(0)
            }
        }
    }

    BackHandler(enabled = overview || (card != null && cardOpening) || drawerShown || sheetVisible || resizingId != null || ambient || folderUp != null || holdMenu.open != null || editing) {
        // A held thing's menu is the topmost of these: from its colours back to its rows, then away.
        if (holdMenu.open != null) {
            if (holdMenu.colours) holdMenu.colours = false else holdMenu.close()
            return@BackHandler
        }
        if (overview) {
            overview = false
            return@BackHandler
        }
        when {
            card != null && cardOpening -> closeCard()
            ambient -> ambient = false
            sheetVisible -> hideSheet()
            folderUp != null -> closeFolder()
            resizingId != null -> resizingId = null
            editing -> endEdit()
            else -> closeDrawer()
        }
    }

    // What a widget asks for that only the home screen can show: a picker, a sheet, Android's page.
    val pickFrame = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(12)) { uris ->
        if (uris.isNotEmpty()) scope.launch {
            if (!viewModel.keepFramePhotos(uris)) {
                Toast.makeText(context, "Those pictures could not be read", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // The Calendar widget's "Show your events" puts Android's question for the calendar when it is
    // tapped, never by itself. As the bedside clock does, this remembers whether the question has come
    // back refused on this visit and whether Android would still show it, since once Android has
    // stopped asking, a tap that put the question again would do nothing at all.
    var calendarRefused by remember { mutableStateOf(false) }
    var calendarCanAskAgain by remember { mutableStateOf(true) }
    val askCalendar = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        calendarRefused = !granted
        calendarCanAskAgain = context.findActivity()?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.READ_CALENDAR)
        } ?: true
    }
    fun setUpWidget(kind: String) {
        when (kind) {
            "world" -> showSheet(HomeSheet.WorldCities)
            "countdown" -> showSheet(HomeSheet.CountdownDate)
            "weather" -> showSheet(HomeSheet.WeatherPlace)
            "photo" -> pickFrame.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            "media" -> NotificationDots.ask(context)
            "todo" -> viewModel.updateSettings { it.copy(todos = com.gabriel.zenithui.data.Todos.clearDone(it.todos)) }
            // The widget has no "Not now" of its own, and the bedside clock's is about the clock, so
            // a tap here always asks; once Android has stopped asking, Zenith's page in the phone's
            // settings opens instead, where the calendar can still be allowed.
            "calendar" -> when (
                CalendarAccess.of(
                    granted = CalendarRepository.hasPermission(context),
                    declined = false,
                    refused = calendarRefused,
                    canAskAgain = calendarCanAskAgain,
                )
            ) {
                CalendarAccess.Granted -> Unit
                CalendarAccess.Blocked -> runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                else -> askCalendar.launch(Manifest.permission.READ_CALENDAR)
            }
        }
    }
    val forecast by viewModel.forecast.collectAsState()
    val visits by viewModel.visits.collectAsState()
    val widgetWorld = remember(settings, forecast, visits) {
        WidgetWorld(
            settings = settings,
            change = { transform -> viewModel.updateSettings(transform) },
            forecast = forecast,
            visits = visits,
            setUp = { kind -> setUpWidget(kind) },
            frameDir = viewModel.frameDir,
        )
    }

    // ------------------------------------------------------------ the hold menu
    // Holding something brings its menu up beside it while the finger is still down. What the menu
    // says is a rule (HoldMenuRules); what each row does is here.
    val pickUpSlop = with(LocalDensity.current) { HoldTuning.PICK_UP_DP.dp.toPx() }
    val holdOpen = holdMenu.open
    val heldItem = (holdOpen?.target as? HoldTarget.Item)?.let { held -> layout.firstOrNull { it.id == held.id } }
    val heldDockKey = (holdOpen?.target as? HoldTarget.Dock)?.key
    // What is held has gone from under its menu, taken off by one of its own rows or by anything
    // else: the menu goes with it.
    val heldGone = holdOpen != null && heldItem == null && (heldDockKey == null || heldDockKey !in settings.dock)
    LaunchedEffect(heldGone) { if (heldGone) holdMenu.close() }
    val heldSizes = remember(heldItem, layout, settings.columns, settings.rows) {
        val widget = heldItem as? WidgetCell
        val spec = widget?.let { com.gabriel.zenithui.data.Widgets.byType(it.type) }
        if (widget == null || spec == null) {
            emptyList()
        } else {
            com.gabriel.zenithui.data.WidgetSizes.options(spec, settings.columns, settings.rows).map { size ->
                val current = size.spanX == widget.spanX && size.spanY == widget.spanY
                SizeChoice(
                    name = size.name, spanX = size.spanX, spanY = size.spanY, current = current,
                    // Asked without changing anything: the same rule the size is then given by.
                    fits = current || com.gabriel.zenithui.data.Reshape.resized(
                        layout, widget.id, size.spanX, size.spanY, settings.columns, settings.rows,
                    ) != null,
                )
            }
        }
    }
    val holdSubject: HoldSubject? = when {
        heldItem is AppCell -> HoldSubject.App(holdShortcuts.map { it.label })
        heldItem is com.gabriel.zenithui.data.ShortcutCell -> HoldSubject.Pinned
        heldItem is FolderCell -> HoldSubject.Folder
        heldItem is WidgetCell -> HoldSubject.ZenithWidget(
            kind = com.gabriel.zenithui.data.Widgets.baseType(heldItem.type),
            sizes = heldSizes,
            mediaAllowed = NotificationDots.allowed(context),
        )
        heldItem is HostedWidgetCell -> HoldSubject.HostedWidget(heldItem.glass)
        heldItem is StackCell -> HoldSubject.Stack(
            members = heldItem.members.map { memberName(context, it) },
            full = heldItem.members.size >= com.gabriel.zenithui.data.StackRules.MOST,
        )
        heldDockKey != null -> HoldSubject.App(holdShortcuts.map { it.label }, inDock = true)
        else -> null
    }
    val holdEntries = remember(holdSubject, settings.lockLayout) {
        holdSubject?.let { HoldMenuRules.rows(it, settings.lockLayout) } ?: emptyList()
    }
    val heldColour = when (heldItem) {
        is FolderCell -> heldItem.colour
        is WidgetCell -> heldItem.accent
        else -> 0L
    }

    /** Takes [id] off the home screen, popping like a soap bubble where it stood. */
    fun popOff(id: String) {
        bubbles.pop(listOf(id))
        viewModel.removeItem(id)
    }

    /** Asks before something is taken off the home screen, the way a phone does. Undo still follows. */
    fun askRemove(target: HoldTarget) {
        val apps = if (libraryMode) "App Library" else "app drawer"
        when (target) {
            is HoldTarget.Dock -> {
                val name = appsByKey[target.key]?.label ?: "this app"
                alert = HomeAlert(
                    title = "Remove “$name”?",
                    message = "Removing from the Dock will keep the app in your $apps.",
                    buttons = listOf(
                        AlertButton("Remove from Dock") {
                            bubbles.pop(listOf(dockBubble(target.key)))
                            viewModel.toggleDock(target.key)
                        },
                        AlertButton("Uninstall App", danger = true) { viewModel.uninstall(target.key) },
                        AlertButton("Cancel") {},
                    ),
                )
            }

            is HoldTarget.Item -> when (val item = layout.firstOrNull { it.id == target.id }) {
                is AppCell -> alert = HomeAlert(
                    title = "Remove “${appsByKey[item.key]?.label ?: "this app"}”?",
                    message = "Removing from Home Screen will keep the app in your $apps.",
                    buttons = listOf(
                        AlertButton("Remove from Home Screen") { popOff(item.id) },
                        AlertButton("Uninstall App", danger = true) { viewModel.uninstall(item.key) },
                        AlertButton("Cancel") {},
                    ),
                )

                is com.gabriel.zenithui.data.ShortcutCell -> alert = HomeAlert(
                    title = "Remove “${item.label}”?",
                    message = "Its app keeps the shortcut.",
                    buttons = listOf(AlertButton("Remove", danger = true) { popOff(item.id) }, AlertButton("Cancel") {}),
                )

                is FolderCell -> alert = HomeAlert(
                    title = "Remove “${item.title}”?",
                    message = "Removing from Home Screen will keep all its apps in your $apps.",
                    buttons = listOf(
                        AlertButton("Remove from Home Screen", danger = true) { popOff(item.id) },
                        AlertButton("Cancel") {},
                    ),
                )

                is WidgetCell, is HostedWidgetCell -> {
                    val name = if (item is WidgetCell) com.gabriel.zenithui.data.Widgets.shortName(item.type)
                    else hostedName(context, (item as HostedWidgetCell).appWidgetId)
                    alert = HomeAlert(
                        title = "Remove “$name”?",
                        message = "You can add it again from Add Widget.",
                        buttons = listOf(AlertButton("Remove", danger = true) { popOff(item.id) }, AlertButton("Cancel") {}),
                    )
                }

                is StackCell -> alert = HomeAlert(
                    title = "Remove this stack?",
                    message = "All ${item.members.size} widgets in it go. You can add them again from Add Widget.",
                    buttons = listOf(AlertButton("Remove", danger = true) { popOff(item.id) }, AlertButton("Cancel") {}),
                )

                null -> Unit
            }
        }
    }

    /** What a row of the menu does. Most put the menu away first; unlocking and the colours keep it. */
    fun holdDo(row: HoldEntry.Row, open: HoldOpen) {
        val target = open.target
        val id = (target as? HoldTarget.Item)?.id
        val item = id?.let { held -> layout.firstOrNull { it.id == held } }
        val appKey = (item as? AppCell)?.key ?: (target as? HoldTarget.Dock)?.key
        when (row.what) {
            HoldDo.UNLOCK -> viewModel.updateSettings { it.copy(lockLayout = false) }
            HoldDo.COLOUR -> holdMenu.colours = true
            else -> {
                holdMenu.close()
                when (row.what) {
                    HoldDo.SHORTCUT -> holdShortcuts.getOrNull(row.index)?.let { launchApp(it.key) }
                    HoldDo.EDIT_HOME -> startEdit(open.frame.center)
                    HoldDo.EDIT_NAME -> appKey?.let { showSheet(HomeSheet.EditApp(it)) }
                    // Into edit mode, with this app's corner out as well as the widgets'.
                    HoldDo.RESIZE -> {
                        startEdit(open.frame.center)
                        if (editing) resizingId = id
                    }
                    HoldDo.ADD_TO_HOME -> appKey?.let { key ->
                        val page = viewModel.addAppToFirstFreeSlot(key)
                        if (page == null) Toast.makeText(context, "Every page is full", Toast.LENGTH_SHORT).show() else revealPage(page)
                    }
                    HoldDo.APP_INFO -> appKey?.let { viewModel.openAppInfo(it) }
                    HoldDo.REMOVE -> askRemove(target)
                    // It opens out of its own icon, as it does from a tap.
                    HoldDo.RENAME -> id?.let { openFolder(it, open.frame, null) }
                    HoldDo.SET_UP -> (item as? WidgetCell)?.let { widget ->
                        val kind = com.gabriel.zenithui.data.Widgets.baseType(widget.type)
                        if (kind == "toggles") showSheet(HomeSheet.Toggles) else setUpWidget(kind)
                    }
                    HoldDo.STACK -> id?.let {
                        stackOnto = it
                        widgetsOpen = true
                    }
                    HoldDo.GLASS -> (item as? HostedWidgetCell)?.let { viewModel.setHostedGlass(it.id, !it.glass) }
                    HoldDo.UNSTACK -> id?.let {
                        if (!viewModel.unstack(it, row.index)) {
                            Toast.makeText(context, "No room for it on this page", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    /** A row, a size, a colour or a shortcut's "+" was chosen: by a tap, or by the held finger lifting on it. */
    fun holdHit(hit: HoldHit) {
        val open = holdMenu.open ?: return
        val id = (open.target as? HoldTarget.Item)?.id
        when (hit) {
            is HoldHit.Row -> (holdEntries.getOrNull(hit.index) as? HoldEntry.Row)?.let { holdDo(it, open) }
            is HoldHit.Plus -> {
                val shortcut = holdShortcuts.getOrNull((holdEntries.getOrNull(hit.index) as? HoldEntry.Row)?.index ?: -1) ?: return
                holdMenu.close()
                val page = viewModel.addShortcut(shortcut)
                if (page == null) Toast.makeText(context, "Every page is full", Toast.LENGTH_SHORT).show() else revealPage(page)
            }
            is HoldHit.Size -> {
                val choice = holdEntries.filterIsInstance<HoldEntry.Sizes>().firstOrNull()?.choices?.getOrNull(hit.index) ?: return
                if (id == null || choice.current || !choice.fits) return
                // The widget changes shape under its menu, which would then stand in the wrong place.
                holdMenu.close()
                haptic.tick()
                viewModel.resizeWidgetTo(id, choice.spanX, choice.spanY)
            }
            // Each colour takes at once, and the menu stays for another.
            is HoldHit.Swatch -> HOLD_COLOURS.getOrNull(hit.index)?.let { (colour, _) ->
                if (id != null) {
                    haptic.tick()
                    viewModel.setItemColour(id, colour)
                }
            }
        }
    }

    // The gestures that report a hold are set up once and outlive recompositions, so what they call
    // is read afresh each time, never captured.
    val onHoldStart by androidx.compose.runtime.rememberUpdatedState<(HoldTarget, androidx.compose.ui.geometry.Rect, androidx.compose.ui.geometry.Rect, Float, Float, Offset) -> Unit> { target, frame, clear, lift, corner, pointer ->
        val key = when (target) {
            is HoldTarget.Item -> (layout.firstOrNull { it.id == target.id } as? AppCell)?.key
            is HoldTarget.Dock -> target.key
        }
        // A work app's shortcuts are not Zenith's to list.
        val packageName = key?.takeUnless { com.gabriel.zenithui.data.WorkKeys.isWork(it) }?.substringBefore('/')
        holdShortcuts = packageName?.let { shortcutsKnown[it] } ?: emptyList()
        holdMenu.begin(HoldOpen(target, frame, clear, lift, corner, pointer))
        if (packageName != null) {
            scope.launch {
                val found = viewModel.shortcuts(packageName)
                shortcutsKnown[packageName] = found
                if (holdMenu.open?.target == target) holdShortcuts = found
            }
        }
    }
    val onHoldMove by androidx.compose.runtime.rememberUpdatedState<(Offset) -> Boolean> { pointer ->
        val open = holdMenu.open
        val hit = if (open == null) null else holdMenu.hitAt(pointer)
        when {
            open == null -> false
            hit != null -> {
                // On the menu: from here the finger chooses, and letting go on a row takes it.
                holdMenu.onMenu = true
                if (holdMenu.hover != hit) {
                    holdMenu.hover = hit
                    haptic.tick()
                }
                false
            }
            else -> {
                holdMenu.hover = null
                val by = pointer - open.start
                val pickedUp = HoldGesture.picksUp(by.x, by.y, pickUpSlop, holdMenu.above, holdMenu.onMenu, settings.lockLayout)
                if (pickedUp) {
                    // Picked up, the home screen is being edited, and stays so when the thing is put
                    // down. The wave wakes everything from what was picked up.
                    holdMenu.close(pickedUp = true)
                    if (!editing) beginEditing(open.frame.center)
                }
                pickedUp
            }
        }
    }
    val onHoldRelease by androidx.compose.runtime.rememberUpdatedState {
        val hit = holdMenu.hover
        holdMenu.hover = null
        if (hit != null) holdHit(hit)
    }
    val holdCalls = remember {
        HoldCalls(
            start = { target, frame, clear, lift, corner, pointer -> onHoldStart(target, frame, clear, lift, corner, pointer) },
            move = { onHoldMove(it) },
            release = { onHoldRelease() },
        )
    }

    // The wallpaper a little darker, when Customise has been asked for that: a veil over it, under
    // everything that stands on it, and the same darkening on the copy of it the glass shows.
    val dimmed = animateFloatAsState(if (settings.dimWallpaper) WALLPAPER_DIM else 0f, tween(300), label = "dimWallpaper")
    CompositionLocalProvider(
        LocalWidgetWorld provides widgetWorld,
        LocalWallpaperDim provides dimmed,
        LocalHomeCover provides homeCover,
        LocalFolderAway provides folderAway,
    ) {
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged {
                drawerHeightPx = it.height.toFloat()
                drawerWidthPx = it.width.toFloat()
            }
            .onGloballyPositioned { homeOrigin = it.positionInRoot() }
            // Two fingers on the home screen: pinching in (Customise, as on Samsung and Pixel
            // phones), pinching out, or swiping up or down together. What each does is chosen
            // under Settings → Gestures. It only watches; the page still gets every touch.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    // Cleared as each new touch begins, not as the last one ends: the drag on the
                    // page hears it has stopped after this does, and must still know it was two.
                    twoFingers = false
                    var startSpan = 0f
                    var startCentre = Offset.Zero
                    var fired = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val fingers = event.changes.filter { it.pressed }
                        if (fingers.isEmpty()) break
                        if (fingers.size < 2) {
                            startSpan = 0f
                            continue
                        }
                        twoFingers = true
                        val a = fingers[0].position
                        val b = fingers[1].position
                        val span = (a - b).getDistance()
                        val centre = (a + b) / 2f
                        if (startSpan == 0f) {
                            startSpan = span
                            startCentre = centre
                            continue
                        }
                        // Only on the home screen itself. Through the drawer, a folder, a sheet or
                        // an app on its way open, what it opens would be under those, and Back
                        // would go to them first.
                        val idle = !fired && !drag.isDragging && !settingsOpen && !widgetsOpen &&
                            !sheetVisible && folderUp == null && card == null && !dragging && drawer.value < 0.02f &&
                            !askingNewLook && !searchOpen && holdMenu.open == null && alert == null && !editing && !customising
                        if (!idle) continue
                        val travel = centre - startCentre
                        val gesture = when {
                            span < startSpan * 0.72f -> Gestures.PINCH_IN
                            span > startSpan * 1.38f -> Gestures.PINCH_OUT
                            // Both fingers the same way, not spreading: a swipe, not a pinch.
                            abs(travel.y) > TWO_FINGER_SWIPE.toPx() && abs(travel.y) > abs(travel.x) * 1.5f &&
                                abs(span / startSpan - 1f) < 0.25f -> if (travel.y > 0f) Gestures.TWO_DOWN else Gestures.TWO_UP
                            else -> null
                        } ?: continue
                        val action = Gestures.actionFor(gesture, viewModel.settings.value.gestures)
                        if (action == GestureActions.NONE) continue
                        fired = true
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        runGesture(action)
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

        // The veil that dims the wallpaper; the glass darkens its own copy by as much.
        if (dimmed.value > 0.001f) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                drawRect(androidx.compose.ui.graphics.Color.Black.copy(alpha = dimmed.value))
            }
        }

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
                    val zoom = homeZoom()
                    // Under Settings or the widget gallery it sinks back a little, as it does under
                    // the drawer, and comes forward again as the page is pulled away.
                    val covered = homeCover.amount()
                    val sink = if (motion.reduced) 1f else homeScaleUnderPage(covered)
                    val drawerOpenBy = drawerNow()
                    val scale = (1f - 0.06f * drawerOpenBy) * zoom * sink
                    scaleX = scale
                    scaleY = scale
                    // It zooms about the icon the app came from, as far as asked, so the eye is
                    // carried into the app and out of it again at the same spot.
                    // Back from an app after Android's own close, with a card going into the icon, it
                    // comes forward about the middle of its foot instead, where Android's close left
                    // the app, as Void's does (see comeForward).
                    val icon = HomeRipple.origin
                    val towards = if (depth != 0f && icon.isSpecified) t[Tune.HomePivot] else 0f
                    transformOrigin = if (comeBackFromFoot && depth != 0f) {
                        val (across, down) = CloseRules.comeBackPivot()
                        androidx.compose.ui.graphics.TransformOrigin(across, down)
                    } else {
                        androidx.compose.ui.graphics.TransformOrigin(
                            LaunchMotion.pivot(icon.x, size.width, towards),
                            LaunchMotion.pivot(icon.y, size.height, towards),
                        )
                    }
                    val sunk = depth.coerceIn(0f, 1f)
                    // Settings is glass over the wallpaper; the home screen is not seen through it.
                    alpha = (1f - 0.55f * drawerOpenBy) * (1f - t[Tune.HomeDim] * sunk) * (1f - covered)
                    // Pulled down for the swipe-down gesture: after the finger, but less and less.
                    if (!motion.reduced) translationY = band(pulled(), PULL_REACH.toPx())
                }
                // The blur is its own short animation rather than a per-frame effect rebuild:
                // recreating a full screen RenderEffect on every drag frame is what made the
                // drawer feel like it was lagging behind the finger.
                .then(if (homeBlur > 0.5.dp) Modifier.blur(homeBlur) else Modifier)
                // The ring of light from a tap, on the wallpaper under everything on the home screen.
                .ringOfLight(glassRing)
        ) {
            // The home screen as it is, kept as a layer of its own: behind the drawer a blurred copy of
            // it fades in over it (see DrawerFrost), and that copy is drawn from this same layer. In a
            // theme that blurs nothing it goes out as its veil comes in, so the drawer stands over the
            // wallpaper rather than over the icons it has just covered.
            Box(
                Modifier
                    .fillMaxSize()
                    .then(
                        when {
                            canFrost -> Modifier
                                .graphicsLayer { alpha = DrawerFrost.sharpAlpha(frost.value) }
                                .drawWithContent {
                                    homeLayer.record { this@drawWithContent.drawContent() }
                                    drawLayer(homeLayer)
                                }
                            blurBehind <= 0.dp -> Modifier.graphicsLayer { alpha = DrawerFrost.sharpAlpha(frost.value) }
                            else -> Modifier
                        }
                    )
            ) {
            // Picking something up on the home screen answers a beat sooner than Android's usual
            // long press, so moving things feels immediate rather than like waiting for permission.
            val baseConfiguration = androidx.compose.ui.platform.LocalViewConfiguration.current
            val quickHold = remember(baseConfiguration) { QuickHold(baseConfiguration) }
            // Every pane of glass on the home screen, and every icon, reads the ring while drawing.
            // Behind the drawer the light on them stays where it was, so the blurred copy of the home
            // screen is worked out once, not again for every frame the phone moves in the hand.
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalViewConfiguration provides quickHold,
                LocalGlassRing provides glassRing,
                com.gabriel.zenithui.ui.theme.LocalTilt provides homeTilt,
                com.gabriel.zenithui.ui.theme.LocalParallax provides homeParallax,
            ) {
            HomeLayout(
                // The page and the gestures made on it. Nothing is written above it any more; while
                // Zenith is not the home app the hint to make it so stands there instead, above the
                // first row, so it covers nothing on the page and keeps clear of Search.
                pager = { room ->
                    Column(room) {
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
                                modifier = Modifier
                                    .padding(horizontal = 18.dp)
                                    .padding(bottom = 4.dp),
                            )
                        }

                        val doubleTap = Gestures.actionFor(Gestures.DOUBLE_TAP, settings.gestures)
                        val doubleTapNow by androidx.compose.runtime.rememberUpdatedState(doubleTap)
                        var pageOrigin by remember { mutableStateOf(Offset.Zero) }
                        Box(
                            Modifier
                                .weight(1f)
                                // On the screen as the glass has it, which is where a ring is sent from.
                                .onGloballyPositioned { pageOrigin = it.positionInWindow() }
                                .draggable(
                                    orientation = Orientation.Vertical,
                                    // With the library on the side, swiping up is left alone; swiping
                                    // down is still the swipe-down gesture.
                                    state = rememberDraggableState { delta -> pageDrag(delta) },
                                    onDragStarted = { dragKind = DRAG_UNDECIDED },
                                    onDragStopped = { velocity -> pageDragStopped(velocity) },
                                )
                                // A double tap waits for the second tap before a single one counts, so it
                                // is only listened for when it has been given something to do.
                                .pointerInput(resizingId, doubleTap != GestureActions.NONE, editing, settings.lockLayout) {
                                    detectTapGestures(
                                        // Only a tap on the wallpaper itself comes here: the icons and
                                        // widgets on the page take their own, and Search and the dock
                                        // are not on the page. It sends a ring of light out, or, while
                                        // something is being resized, puts that down instead.
                                        onTap = { at ->
                                            when {
                                                resizingId != null -> resizingId = null
                                                // A tap on the wallpaper is one of the ways out of edit mode.
                                                editing -> endEdit(pageOrigin + at)
                                                else -> ringAt(pageOrigin + at)
                                            }
                                        },
                                        onDoubleTap = if (doubleTap == GestureActions.NONE) null else { at ->
                                            if (resizingId == null && !editing) {
                                                ringAt(pageOrigin + at)
                                                runGesture(doubleTapNow)
                                            }
                                        },
                                        // Holding an empty spot starts edit mode, as on a phone;
                                        // Customise is the brush in its bar.
                                        onLongPress = { at -> if (resizingId == null && !editing) startEdit(pageOrigin + at) },
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
                                // How far the page is through a swipe is read only while it is drawn,
                                // in its layer below and the layers of what is on it: a swipe composes
                                // nothing, every page is drawn again as it moves rather than built
                                // again on every frame of it. What the page is composed with changes
                                // once a swipe, as it comes to rest.
                                val offsetNow = remember(pagerState, page) {
                                    { (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction }
                                }
                                // The page the swipe started on, whichever way it goes: the one Stack sinks.
                                val leaving = page == pagerState.settledPage
                                // With movement reduced a page simply slides.
                                val effect = if (motion.reduced) "slide" else settings.pageEffect
                                val pageItems = remember(layout, page) { layout.filter { it.page == page } }
                                // The page coming in is drawn over the one being left, going back as
                                // well as going on: the pager alone draws them left to right. Set out
                                // here, so the page itself is not composed again as the swipe ends.
                                Box(Modifier.fillMaxSize().zIndex(PageEffects.over(leaving))) {
                                HomePage(
                                    page = page,
                                    // The page that is in view at rest: the one things are dropped on.
                                    active = page == pagerState.settledPage,
                                    items = pageItems,
                                    settings = settings,
                                    appsByKey = appsByKey,
                                    drag = drag,
                                    resizingId = resizingId,
                                    onResize = { item, spanX, spanY ->
                                        when (item) {
                                            is WidgetCell -> viewModel.resizeWidget(item.id, spanX, spanY)
                                            is AppCell -> viewModel.resizeApp(item.id, spanX, rows = spanY / ROW_UNITS)
                                            is HostedWidgetCell, is StackCell -> viewModel.resizeHosted(item.id, spanX, spanY)
                                            else -> false
                                        }
                                    },
                                    onRestoreWidget = { item, info -> viewModel.addHostedWidget(info, replacing = item.id) },
                                    onResizeDone = { resizingId = null },
                                    onResizePull = { item, starting ->
                                        if (starting) {
                                            viewModel.beginChange()
                                        } else {
                                            viewModel.endChange({ "${viewModel.nameOf(item.id)} resized" })
                                        }
                                    },
                                    onGathered = { haptic.tick() },
                                    onOpenApp = { launchApp(it) },
                                    onOpenFolder = { folder, icon, tap -> openFolder(folder.id, icon, tap) },
                                    hold = holdCalls,
                                    heldId = (holdMenu.open?.target as? HoldTarget.Item)?.id,
                                    heldSquash = { holdMenu.squash() },
                                    editing = editing,
                                    onBead = { askRemove(HoldTarget.Item(it.id)) },
                                    beads = !customising,
                                    bubbles = bubbles,
                                    onSearch = { openDrawer(search = true) },
                                    onNoteChange = { viewModel.setNote(it) },
                                    onAmbient = { ambient = true },
                                    onDrop = { handleDrop() },
                                    dropPlan = dropPlan,
                                    freePlacement = settings.freeform && plus.active,
                                    animateEntrance = entrance,
                                    pageOffset = if (PageEffects.parallax(effect)) offsetNow else NO_PAGE_OFFSET,
                                    arrivalClock = { arrivalClock.value },
                                    arrivalDepth = { arrivalDepth.floatValue },
                                    arrivalWave = { arrivalWave.value },
                                    jiggleClock = { jiggleClock.floatValue },
                                    jiggleStrength = { jiggleStrength.value },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            // Which page the swipe started on is read here too, so
                                            // the layer changes with it and the page is not composed.
                                            val turn = PageEffects.transform(effect, offsetNow(), page == pagerState.settledPage)
                                            alpha = turn.alpha
                                            scaleX = turn.scale * turn.squash
                                            scaleY = turn.scale
                                            rotationY = turn.rotationY
                                            rotationZ = turn.rotationZ
                                            transformOrigin = TransformOrigin(turn.pivotX, turn.pivotY)
                                            translationX = turn.shiftX * size.width
                                            // Further back than the default, so a turning page keeps its
                                            // shape instead of flaring out towards the eye.
                                            cameraDistance = 14f * this.density
                                        }
                                        .padding(horizontal = PAGE_MARGIN, vertical = 4.dp),
                                )
                                }
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
                    }
                },
                // Search, a pill in the middle of the band between the page and the dock. There is no
                // full-width bar any more, and no row of page dots: while the pages turn, and for a
                // second after, the pill's words give way to the dots. Tapped, its glass stretches
                // into the search field, which stands in its place until it has shrunk back into it.
                //
                // For the five seconds something taken off the home screen can still be put back,
                // Undo stands in the band in Search's place, and Search comes back when it goes. Over
                // the foot of the page it covered the last row's names and icons; a little taller
                // than the band, here it reaches only into the page's padding and the top of the
                // dock's slab, and the band is drawn over the dock so the slab does not cut it off.
                // Part of the home screen, it sinks, fades and softens with it as the drawer, a
                // folder, an app or Settings comes over it.
                searchBand = { band ->
                    // What Undo would take back: outside edit mode the one change left over, for a
                    // few seconds; edited, every change made, newest first, for as long as it lasts.
                    val undo by viewModel.undoable.collectAsState()
                    val undoShown = undo?.takeIf { editing != it.timed && !customising }
                    // Edited, the pill stretches into the bar of controls; while something is
                    // carried the bar draws back into the pill, which shows the pages, so the
                    // dock is free to be dropped on.
                    val barShown = editing && !drag.isDragging && !customising
                    Box(band.zIndex(1f), contentAlignment = Alignment.Center) {
                        AnimatedVisibility(
                            visible = !barShown && (undoShown == null || editing),
                            enter = bandSwapIn(motion.reduced),
                            exit = bandSwapOut(motion.reduced),
                        ) {
                            SearchPill(
                                onOpen = { openSearch() },
                                dots = { if (editing) 1f else pillDotsShown.value },
                                pageCount = pageCount,
                                position = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
                                // It arrives as the search bar did, moved on a layer, so its glass reads
                                // the clock that moves it and keeps the wallpaper in place behind it.
                                tracking = { arrivalClock.value },
                                modifier = Modifier
                                    .arriving(
                                        { arrivalClock.value }, { arrivalDepth.floatValue }, distance = 0.85f, from = 40.dp,
                                        style = settings.arrival, wave = { arrivalWave.value },
                                    )
                                    // Where it is, for the field to stretch out of and shrink back into.
                                    .onGloballyPositioned { pillPlaced[0] = it }
                                    // Not drawn while the field's glass is out of it, standing in its
                                    // place; with movement reduced the field fades in over it instead.
                                    .drawWithContent { if (!searchShown || motion.reduced) drawContent() },
                            )
                        }
                        UndoPill(
                            what = undoShown?.label,
                            key = undoShown?.key,
                            millis = com.gabriel.zenithui.data.UNDO_MS,
                            more = undoShown?.more ?: 0,
                            timed = undoShown?.timed ?: true,
                            // It happened on another page: the screen turns there to show it undone.
                            // What comes back to the page, rather than moving back on it, blows
                            // back together.
                            onUndo = {
                                val had = layout.mapTo(HashSet()) { it.id }
                                val docked = settings.dock.toHashSet()
                                val back = viewModel.undo()
                                bubbles.back(
                                    back.filter { it.id !in had }.map { it.id } +
                                        viewModel.settings.value.dock.filter { it !in docked }.map(::dockBubble)
                                )
                                back.firstOrNull()?.let { revealPage(it.page) }
                            },
                            // Edited, Undo stands just above the bar rather than in its place.
                            modifier = Modifier.padding(horizontal = 16.dp).offset(y = if (editing) (-56).dp else 0.dp),
                        )
                        EditBar(
                            visible = barShown,
                            pageCount = pageCount,
                            position = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
                            onAdd = { widgetsOpen = true },
                            onCustomise = { customising = true },
                            onPages = { overview = true },
                            onDone = { at -> endEdit(at) },
                        )
                    }
                },
                dock = { room ->
                    // The dock's slab stands on the wallpaper, as the page does: the same clear glass
                    // in Light and in Dark.
                    if (settings.showDock) OnWallpaper {
                        // The dock's own icons, where a closing app can go back to (see IconPlaces).
                        androidx.compose.runtime.CompositionLocalProvider(com.gabriel.zenithui.ui.common.LocalHomePlaces provides true) {
                        DockBar(
                            keys = settings.dock,
                            appsByKey = appsByKey,
                            settings = settings,
                            highlight = overDock,
                            animateEntrance = entrance,
                            geometry = drag.geometry,
                            onOpen = { launchApp(it) },
                            hold = holdCalls,
                            heldKey = heldDockKey,
                            heldSquash = { holdMenu.squash() },
                            editing = editing,
                            onBead = { askRemove(HoldTarget.Dock(it)) },
                            beads = !customising,
                            bubbles = bubbles,
                            jiggleClock = { jiggleClock.floatValue },
                            jiggleStrength = { jiggleStrength.value },
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
                            onCentres = { dockCentres = it },
                            // It spans the screen and stands its slab in from the edges itself, its apps
                            // still under the page's columns.
                            modifier = room
                                .arriving(
                                    { arrivalClock.value }, { arrivalDepth.floatValue }, distance = 1f, from = 56.dp,
                                    style = settings.arrival, wave = { arrivalWave.value },
                                )
                                .navigationBarsPadding()
                                .padding(bottom = 10.dp),
                            // It arrives moved on a layer, so its glass reads the clock that moves it and
                            // keeps the wallpaper in place behind it, as Search's does.
                            tracking = { arrivalClock.value },
                        )
                        }
                    } else {
                        // Without the dock the pages reach down to the gesture bar instead.
                        Spacer(room.navigationBarsPadding().height(10.dp))
                    }
                },
                // Nothing floats over the page: Undo stands in the band, in Search's place.
                overlays = {},
            )
            }
            }
            // A theme with no blur: a veil of its page over the wallpaper, faded in as the copy would
            // be while the home screen goes out, so what is behind the drawer is quiet, not bare.
            if (blurBehind <= 0.dp && frosting) {
                val veil = palette.backdrop.first()
                Box(Modifier.matchParentSize().drawBehind { drawRect(veil, alpha = DRAWER_VEIL * frost.value) })
            }
            // The blurred copy: worked out once as it starts to come in, then only faded, which costs
            // the swipe nothing. Once the drawer is open it is the home screen's blur, and follows it.
            if (canFrost && frosting) {
                Box(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            renderEffect = frostEffect
                            alpha = frost.value
                            // As Modifier.blur does: the blur kept inside the screen's edges.
                            clip = true
                        }
                        .drawBehind { drawLayer(homeLayer) }
                )
            }
        }

        // ------------------------------------------------------------ drawer
        // ------------------------------------------------------------ page overview
        // The page in view is read inside, so a swipe composes the overview's corner of the home
        // screen and not all of it.
        CurrentPage(pagerState) { currentPage ->
        PageOverview(
            visible = overview,
            pageCount = pageCount,
            current = currentPage,
            layout = layout,
            appsByKey = appsByKey,
            settings = settings,
            wallpaper = glassLevels?.sharp,
            screenWidth = with(density) { drawerWidthPx.toDp() },
            screenHeight = with(density) { drawerHeightPx.toDp() },
            onGo = { page ->
                overview = false
                scope.launch { pagerState.animateScrollToPage(page) }
            },
            onMove = { from, to ->
                val here = com.gabriel.zenithui.data.PageOrder.newIndex(pagerState.currentPage, from, to)
                viewModel.movePage(from, to)
                scope.launch { pagerState.scrollToPage(here) }
            },
            onRemove = { page ->
                val here = pagerState.currentPage.let { if (it > page) it - 1 else it }.coerceAtMost(pageCount - 2).coerceAtLeast(0)
                viewModel.removePage(page)
                scope.launch { pagerState.scrollToPage(here) }
            },
            onAdd = { viewModel.addPage() },
            onDismiss = { overview = false },
        )
        }

        // Undo is part of the home screen now, in Search's place in the band: see HomeLayout's searchBand.

        if (drawerShown || drawerEverOpened) {
            // In the order chosen in Settings. By colour needs every icon's colour, worked out once.
            val colours by produceState(emptyMap<String, com.gabriel.zenithui.data.IconColour>(), drawerApps, settings.drawerSort) {
                if (settings.drawerSort == com.gabriel.zenithui.data.DrawerSort.COLOUR) value = viewModel.icons.colours(drawerApps.map { it.key })
            }
            val sortedApps = remember(drawerApps, settings.drawerSort, settings.launchCounts, colours) {
                com.gabriel.zenithui.data.DrawerSort.sort(drawerApps, settings.drawerSort, settings.launchCounts, viewModel.repository::installedAt, colours)
            }
            val workApps by viewModel.workApps.collectAsState()
            // Every app's shortcuts, asked for once each time the drawer is open, for its search: once
            // it has come to rest, so the answer never arrives in the middle of the swipe.
            val allShortcuts by produceState(emptyList<AppShortcut>(), drawerSettled) {
                if (drawerSettled) value = viewModel.allShortcuts()
            }
            // While it moves, the light on its glass and icons stays where it was.
            androidx.compose.runtime.CompositionLocalProvider(com.gabriel.zenithui.ui.theme.LocalTilt provides drawerTilt) {
            AppDrawer(
                apps = sortedApps,
                groups = libraryGroups,
                horizontal = libraryMode,
                settings = settings,
                focusSearch = focusSearch,
                progress = drawerNow,
                onDrag = { delta -> dragDrawer(delta) },
                onDragStopped = { velocity -> settleDrawer(velocity) },
                onOpen = {
                    launchApp(it)
                    closeDrawer()
                },
                onLongPress = { showSheet(HomeSheet.DrawerApp(it.key)) },
                onDismiss = { closeDrawer() },
                columns = settings.drawerColumns.takeIf { it > 0 } ?: settings.columns,
                alphabetical = com.gabriel.zenithui.data.DrawerSort.of(settings.drawerSort) == com.gabriel.zenithui.data.DrawerSort.AZ,
                tabs = settings.drawerTabs,
                workApps = workApps,
                onNewTab = { showSheet(HomeSheet.DrawerTabEdit(-1)) },
                onEditTab = { index -> showSheet(HomeSheet.DrawerTabEdit(index)) },
                shortcuts = allShortcuts,
                onShortcut = {
                    launchApp(it.key)
                    closeDrawer()
                },
                onSetting = { link ->
                    closeDrawer()
                    openSetting(link)
                },
                onCopy = { text -> copyAnswer(text) },
                onIconSize = { px -> viewModel.drawerIconsAre(px) },
            )
            }
        }

        // ------------------------------------------------------------ search
        // The pill stretched into a search field on the keyboard, over the home screen dimmed and
        // softened behind it, until it has shrunk back into the pill. It finds what the drawer's
        // search does, among the same apps: the hidden ones stay hidden, and work apps are found.
        if (searchShown) {
            val workApps by viewModel.workApps.collectAsState()
            val searchApps = remember(drawerApps, workApps) { drawerApps + workApps }
            // Every app's shortcuts, asked for once each time it opens, for its answers.
            val searchShortcuts by produceState(emptyList<AppShortcut>(), searchOpen) {
                if (searchOpen) value = viewModel.allShortcuts()
            }
            SearchField(
                open = searchOpen,
                from = searchFrom,
                apps = searchApps,
                shortcuts = searchShortcuts,
                // Out of the icon it found, as from the drawer, while the field goes back into the pill.
                onOpenApp = { key ->
                    launchApp(key)
                    closeSearch()
                },
                onClose = { closeSearch() },
                onSetting = { link ->
                    closeSearch()
                    openSetting(link)
                },
                onCopy = { text -> copyAnswer(text) },
                onShrunk = { searchShown = false },
                iconShape = ZenithShapes.forIconShape(settings.iconShape),
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

        }
        // There is no bin any more: something is taken off the home screen by the minus on its
        // corner while the screen is edited, or by the red row in its menu, and asked about first.
        if (drag.isDragging) DragGhost(drag = drag, settings = settings, appsByKey = appsByKey)
        // What was let go into the bin, the dock or a folder is seen going there.
        drag.sent?.let { sent ->
            SentGhost(sent = sent, drag = drag, settings = settings, appsByKey = appsByKey, onGone = { if (drag.sent === sent) drag.sent = null })
        }

        // ------------------------------------------------------------ folder
        // The folders' apps, drawn at the open folder's size and kept to hand, off the main thread:
        // a folder pours out of its tile with its apps in it from the first frame, never with blank
        // plates that fade in as they load, and never reading a file as it starts to move.
        val folderKeys = remember(layout) { layout.filterIsInstance<FolderCell>().flatMap { it.keys }.distinct() }
        val folderIconPx = if (drawerWidthPx <= 0f) 0 else with(LocalDensity.current) {
            FolderSize.of(drawerWidthPx / this.density).icon.dp.roundToPx().coerceIn(48, 288)
        }
        val iconVersion by viewModel.icons.version.collectAsState()
        LaunchedEffect(folderKeys, folderIconPx, settings.iconStyle, iconVersion) {
            if (folderIconPx <= 0) return@LaunchedEffect
            viewModel.icons.warm(
                purpose = "folders",
                keys = folderKeys,
                sizePx = folderIconPx,
                glyphs = com.gabriel.zenithui.data.IconStyles.of(settings.iconStyle) != com.gabriel.zenithui.data.IconStyles.ORIGINAL,
            )
        }
        folderUp?.let { up ->
            val folder = layout.firstOrNull { it.id == up.id } as? FolderCell
            // Taking the last app out dissolves the folder: then there is nothing left to show.
            LaunchedEffect(folder == null) {
                if (folder == null) {
                    folderJob[0]?.cancel()
                    folderUp = null
                }
            }
            // On the wallpaper, as the hold menu is: the same clear glass and white names in Light
            // and in Dark, lit by the home screen's one ring of light.
            if (folder != null) OnWallpaper {
                androidx.compose.runtime.CompositionLocalProvider(LocalGlassRing provides glassRing) {
                    FolderPanel(
                        folder = folder,
                        tile = up.tile,
                        tileShape = folderTileShape(settings.iconShape),
                        tap = up.tap,
                        motion = folderMotion,
                        blurred = canFrost,
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
            }
        }

        // ------------------------------------------------------------ the hold menu
        // Over the page, the dock and an open folder; under the sheets its rows lead to. Its glass
        // stands on the wallpaper, as the page's does: the same in Light and in Dark, and lit by the
        // same ring of light.
        OnWallpaper {
            androidx.compose.runtime.CompositionLocalProvider(LocalGlassRing provides glassRing) {
                HoldMenuLayer(
                    state = holdMenu,
                    entries = holdEntries,
                    shortcutKeys = holdShortcuts.map { it.key },
                    colour = heldColour,
                    columns = settings.columns,
                    rows = settings.rows,
                    onHit = { holdHit(it) },
                    onDismiss = { holdMenu.close() },
                )
                CustomiseCard(
                    visible = customising,
                    iconStyle = settings.iconStyle,
                    widgetLook = settings.widgetLook,
                    large = !settings.showLabels,
                    dim = settings.dimWallpaper,
                    sampleKey = (layout.firstOrNull { it is AppCell } as? AppCell)?.key ?: settings.dock.firstOrNull(),
                    iconShape = ZenithShapes.forIconShape(settings.iconShape),
                    showNewLook = !NewLook.isOn(settings),
                    showMakeHome = !isDefaultLauncher,
                    wallpaper = WallStyle.byName(settings.wallpaperStyle),
                    wallpaperColours = remember(settings) {
                        Palettes.setById(settings.wallpaperPalette.ifEmpty { settings.paletteId }, settings)
                    },
                    wallpaperFollows = settings.wallpaperFollow,
                    onIconStyle = { style -> viewModel.updateSettings { it.copy(iconStyle = style) } },
                    onWidgetLook = { look -> viewModel.updateSettings { it.copy(widgetLook = look) } },
                    // Larger icons are icons with no names under them: they take the room the names had.
                    onLarge = { large -> viewModel.updateSettings { it.copy(showLabels = !large) } },
                    onDim = { dim -> viewModel.updateSettings { it.copy(dimWallpaper = dim) } },
                    onWallpaper = {
                        customising = false
                        showSheet(HomeSheet.Wallpapers)
                    },
                    onClock = {
                        endEdit(quietly = true)
                        ambient = true
                    },
                    onHelp = {
                        customising = false
                        showSheet(HomeSheet.Help)
                    },
                    onSettings = {
                        endEdit(quietly = true)
                        settingsOpen = true
                    },
                    // The grid it sets is worked out for this screen and the home screen settled
                    // into it, as for any grid: nothing is lost, though some may move to another page.
                    onNewLook = { viewModel.updateSettings { NewLook.apply(it) } },
                    onMakeHome = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    },
                    onDismiss = { customising = false },
                )
                GlassAlert(alert = alert, onDismiss = { alert = null })
                GlassToast(toast = toast, onDone = { toast = null })
            }
        }

        StreakStrip(
            state = checkIn,
            onShown = { viewModel.clearCheckIn() },
            modifier = Modifier.align(Alignment.TopCenter),
            flame = com.gabriel.zenithui.data.AuraFlame.worn(settings),
        )

        // What letting go of a swipe down will do, coming down from the top as the page is pulled.
        PullHint(
            progress = { (pulled() / pullFirePx).coerceIn(0f, 1.4f) },
            armed = pulling && pullDown >= pullFirePx,
            label = GestureActions.label(swipeDownAction) { key -> appsByKey[key]?.label },
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
                // Off the screen it goes straight, keeping to the edge it goes through (CloseAway).
                look = remember(tuning, closingCard && closingAway) {
                    CardLook.of(tuning).let { if (closingCard && closingAway) it.copy(arc = 0.dp) else it }
                },
                glass = settings.openAnimation == "glass",
                catchTaps = cardOpening,
                onTap = { closeCard() },
                fade = {
                    if (!closingCard) {
                        1f
                    } else {
                        val grown = cardProgress.value
                        CloseRules.closingAlpha(grown, closingFrom) *
                            (if (closingVanishes) CloseRules.vanishAlpha(grown) else 1f) *
                            cardLeaving.value
                    }
                },
                squash = { cardSquash.value },
                ghost = closingCard,
                // Bringing the home screen back with it, it goes into the icon where the icon is on the
                // way. Off the screen it goes to the edge, which the home screen does not move.
                homeZoom = { if (closingCard && comeBackFromFoot && !closingAway) homeZoom() else 1f },
            )
        }

        // ------------------------------------------------------------ settings
        // Page one, small: on Settings' Theme and Home pages, and on a wallpaper's preview.
        val homePreview = SettingsPreview(
            items = remember(layout) { layout.filter { it.page == 0 } },
            appsByKey = appsByKey,
            wallpaper = glassLevels?.sharp,
            screenWidth = with(density) { drawerWidthPx.toDp() },
            screenHeight = with(density) { drawerHeightPx.toDp() },
        )
        SettingsScreen(
            visible = settingsOpen,
            covered = sheetVisible,
            settings = settings,
            plus = plus,
            preview = homePreview,
            actions = SettingsActions(
                change = { transform -> viewModel.updateSettings(transform) },
                wallpaper = { showSheet(HomeSheet.Wallpapers) },
                hiddenApps = { withPlus { showSheet(HomeSheet.HiddenApps) } },
                freeform = { on ->
                    // Turning it on is the Plus part; turning it off is always allowed.
                    if (on && !plus.active) showSheet(HomeSheet.Plus)
                    else viewModel.updateSettings { it.copy(freeform = on) }
                },
                backup = { withPlus { saveBackup.launch(Backup.fileName()) } },
                restore = { withPlus { openBackup.launch(arrayOf("*/*")) } },
                plus = { showSheet(HomeSheet.Plus) },
                redeem = { code -> viewModel.redeemAuraCode(code) },
                buyPlusDay = viewModel::buyPlusDay,
                buyDesign = viewModel::buyDesign,
                wearFlame = viewModel::wearFlame,
                help = { showSheet(HomeSheet.Help) },
                reset = { viewModel.resetEverything() },
                tidy = { viewModel.tidyUp() },
                gesture = { id -> showSheet(HomeSheet.GestureAction(id)) },
                lockHelper = {
                    if (LockService.enabled(context)) LockService.openSettings(context) else showSheet(HomeSheet.LockHelper)
                },
                appPictures = {
                    if (PictureService.enabled(context)) PictureService.openSettings(context) else showSheet(HomeSheet.AppPictures)
                },
                notchAccess = { which -> showSheet(HomeSheet.NotchAccess(which)) },
                // Saving, switching, scheduling and sharing are Plus; opening a code and
                // deleting a Look of your own never are.
                looks = LookActions(
                    server = ServerConfig.enabled,
                    save = { withPlus { showSheet(HomeSheet.LookName(null)) } },
                    put = { look -> withPlus { viewModel.applyLook(look) } },
                    menu = { look -> showSheet(HomeSheet.LookMenu(look.id)) },
                    addRule = { withPlus { showSheet(if (looks.isEmpty()) HomeSheet.LookName(null) else HomeSheet.LookTime) } },
                    removeRule = { index -> viewModel.setLookRules(settings.lookRules.filterIndexed { i, _ -> i != index }) },
                    openCode = { showSheet(HomeSheet.LookCode("")) },
                    featured = viewModel::featuredLooks,
                    openFeatured = { shared -> showSheet(HomeSheet.LookCode(shared.shown)) },
                ),
                glassStudio = { withPlus { showSheet(HomeSheet.GlassStudio) } },
                fontLicences = { showSheet(HomeSheet.FontLicences) },
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
            looks = looks,
        )

        // ------------------------------------------------------------ widgets
        WidgetGallery(
            visible = widgetsOpen,
            settings = settings,
            geometry = drag.geometry,
            onAdd = { type, spanX, spanY ->
                val onto = stackOnto
                if (onto != null) {
                    val stacked = viewModel.stackWidget(type, onto)
                    if (!stacked) Toast.makeText(context, "That stack is full", Toast.LENGTH_SHORT).show()
                    stacked
                } else {
                    val page = viewModel.addWidget(type, spanX, spanY)
                    revealPage(page)
                    page != null
                }
            },
            onAddHosted = { info -> viewModel.addHostedWidget(info, stackOnto = stackOnto) },
            stacking = stackOnto != null,
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
                is HomeSheet.DrawerApp -> {
                    val work = com.gabriel.zenithui.data.WorkKeys.isWork(current.key)
                    val workList by viewModel.workApps.collectAsState()
                    val app = appsByKey[current.key] ?: workList.firstOrNull { it.key == current.key }
                    val shortcuts by produceState(emptyList<AppShortcut>(), current.key) {
                        if (!work) value = viewModel.shortcuts(current.key.substringBefore('/'))
                    }
                    DrawerAppSheet(
                        visible = up,
                        work = work,
                        label = app?.label ?: current.key,
                        pinned = current.key in settings.dock,
                        shortcuts = shortcuts,
                        onShortcut = {
                            launchApp(it.key)
                            closeDrawer()
                        },
                        onAddShortcut = { shortcut ->
                            val page = viewModel.addShortcut(shortcut)
                            if (page == null) {
                                Toast.makeText(context, "Every page is full", Toast.LENGTH_SHORT).show()
                            } else {
                                revealPage(page)
                                closeDrawer()
                            }
                        },
                        onEdit = { showSheet(HomeSheet.EditApp(current.key)) },
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

                is HomeSheet.EditApp -> {
                    val edit = settings.appEdits[current.key] ?: AppEdit()
                    EditAppSheet(
                        visible = up,
                        appKey = current.key,
                        original = remember(current.key, apps) { viewModel.originalLabel(current.key) },
                        edit = edit,
                        shape = ZenithShapes.forIconShape(settings.iconShape),
                        onLabel = { name -> viewModel.editApp(current.key) { it.copy(label = name) } },
                        onChooseIcon = { showSheet(HomeSheet.PickIcon(current.key)) },
                        onHideLabel = { hide -> viewModel.editApp(current.key) { it.copy(hideLabel = hide) } },
                        onReset = { viewModel.editApp(current.key) { AppEdit() } },
                        onDismiss = { hideSheet() },
                    )
                }

                is HomeSheet.PickIcon -> IconPickerSheet(
                    visible = up,
                    appKey = current.key,
                    current = settings.appEdits[current.key]?.icon.orEmpty(),
                    shape = ZenithShapes.forIconShape(settings.iconShape),
                    onPick = { source ->
                        viewModel.editApp(current.key) { it.copy(icon = source) }
                        // Back to the app, to see it with its new face.
                        showSheet(HomeSheet.EditApp(current.key))
                    },
                    onPhoto = {
                        photoFor[0] = current.key
                        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        showSheet(HomeSheet.EditApp(current.key))
                    },
                    onDismiss = { hideSheet() },
                )

                HomeSheet.Help -> HelpSheet(
                    visible = up,
                    onDismiss = { hideSheet() },
                )

                HomeSheet.LockHelper -> LockHelperSheet(
                    visible = up,
                    onOpen = { LockService.openSettings(context) },
                    onDismiss = { hideSheet() },
                )

                HomeSheet.AppPictures -> AppPicturesSheet(
                    visible = up,
                    onOpen = { PictureService.openSettings(context) },
                    onDismiss = { hideSheet() },
                )

                is HomeSheet.NotchAccess -> NotchAccessSheet(
                    visible = up,
                    which = current.which,
                    onOpen = {
                        when (current.which) {
                            com.gabriel.zenithui.data.NotchAccess.OVER_APPS -> com.gabriel.zenithui.island.IslandService.ask(context)
                            com.gabriel.zenithui.data.NotchAccess.NOTIFICATIONS -> com.gabriel.zenithui.data.NotificationDots.ask(context)
                        }
                    },
                    onDismiss = { hideSheet() },
                )

                is HomeSheet.GestureAction -> {
                    val helperOn by LockService.on.collectAsState()
                    GestureActionSheet(
                        visible = up,
                        gesture = current.gesture,
                        current = Gestures.actionFor(current.gesture, settings.gestures),
                        apps = apps,
                        helperOn = helperOn,
                        shortcutsOf = { pkg -> viewModel.shortcuts(pkg) },
                        onPick = { action ->
                            viewModel.updateSettings { it.copy(gestures = Gestures.with(it.gestures, current.gesture, action)) }
                            // Locking needs the helper: say so now, rather than when the gesture is first used.
                            if (GestureActions.needsHelper(action) && !LockService.enabled(context)) {
                                showSheet(HomeSheet.LockHelper)
                            } else {
                                hideSheet()
                            }
                        },
                        onDismiss = { hideSheet() },
                    )
                }

                HomeSheet.Wallpapers -> WallpaperSheet(
                    visible = up,
                    settings = settings,
                    home = homePreview,
                    plusActive = plus.active,
                    onSet = { style, colours, lockToo -> viewModel.setWallpaper(style, colours, lockToo) },
                    onPhoto = {
                        viewModel.photoWallpaperSet()
                        // The glass is drawn from the wallpaper, so read the new one straight away.
                        viewModel.refreshWallpaper()
                    },
                    onFollow = { on -> viewModel.updateSettings { it.copy(wallpaperFollow = on) } },
                    onPlus = { showSheet(HomeSheet.Plus) },
                    onDismiss = { hideSheet() },
                    onBuyDesign = viewModel::buyDesign,
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
                        val started = activity != null && viewModel.subscribe(activity, it)
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

                HomeSheet.HiddenApps -> HiddenAppsSheet(
                    visible = up,
                    apps = apps,
                    settings = settings,
                    onToggle = { key -> viewModel.toggleHidden(key) },
                    onSetPin = { pin -> viewModel.setHiddenPin(pin) },
                    onDismiss = { hideSheet() },
                )

                is HomeSheet.DrawerTabEdit -> {
                    val index = current.index
                    val existing = settings.drawerTabs.getOrNull(index)
                    DrawerTabSheet(
                        visible = up,
                        tab = existing,
                        apps = drawerApps,
                        onSave = { tab ->
                            viewModel.updateSettings {
                                val tabs = it.drawerTabs.toMutableList()
                                if (existing != null && index in tabs.indices) tabs[index] = tab else tabs += tab
                                it.copy(drawerTabs = tabs.take(com.gabriel.zenithui.data.DrawerTab.MOST))
                            }
                            hideSheet()
                        },
                        onDelete = if (existing == null) null else ({
                            viewModel.updateSettings { it.copy(drawerTabs = it.drawerTabs.filterIndexed { i, _ -> i != index }) }
                            hideSheet()
                        }),
                        onDismiss = { hideSheet() },
                    )
                }

                HomeSheet.WorldCities -> WorldCitiesSheet(
                    visible = up,
                    chosen = settings.worldClocks,
                    onChange = { cities -> viewModel.updateSettings { it.copy(worldClocks = cities.distinct().take(com.gabriel.zenithui.data.WorldClocks.MOST)) } },
                    onDismiss = { hideSheet() },
                )

                HomeSheet.CountdownDate -> CountdownSheet(
                    visible = up,
                    title = settings.countdownTitle,
                    day = settings.countdownDay,
                    onSet = { title, day ->
                        viewModel.setCountdown(title, day)
                        hideSheet()
                    },
                    onClear = {
                        viewModel.setCountdown("", 0L)
                        hideSheet()
                    },
                    onDismiss = { hideSheet() },
                )

                HomeSheet.WeatherPlace -> WeatherPlaceSheet(
                    visible = up,
                    place = settings.weatherPlace,
                    units = settings.weatherUnits,
                    search = { query -> viewModel.searchPlaces(query) },
                    onPick = { place, units ->
                        viewModel.setWeather(place, units)
                        hideSheet()
                    },
                    onUnits = { units ->
                        viewModel.updateSettings { it.copy(weatherUnits = units) }
                        viewModel.refreshWeather(force = true)
                    },
                    onClear = {
                        viewModel.setWeather(null, settings.weatherUnits)
                        hideSheet()
                    },
                    onDismiss = { hideSheet() },
                )

                is HomeSheet.LookName -> {
                    val renaming = current.renaming?.let { id -> looks.firstOrNull { it.id == id } }
                    LookNameSheet(
                        visible = up,
                        title = if (renaming == null) "Save this look" else "Rename",
                        initial = renaming?.name.orEmpty(),
                        action = if (renaming == null) "Save" else "Rename",
                        onSave = { name ->
                            if (renaming == null) viewModel.saveLook(name) else viewModel.renameLook(renaming.id, name)
                            hideSheet()
                        },
                        onDismiss = { hideSheet() },
                    )
                }

                is HomeSheet.LookMenu -> {
                    val look = looks.firstOrNull { it.id == current.id }
                    if (look != null) {
                        LookMenuSheet(
                            visible = up,
                            look = look,
                            canShare = ServerConfig.enabled,
                            onPut = { withPlus { viewModel.applyLook(look) } },
                            onUpdate = { withPlus { viewModel.updateLook(look.id) } },
                            onRename = { showSheet(HomeSheet.LookName(look.id)) },
                            onShare = { again ->
                                if (plus.active) viewModel.shareLook(look.id, again) else {
                                    showSheet(HomeSheet.Plus)
                                    null to null
                                }
                            },
                            // Taking a code back is never Plus: what you shared is yours to unshare.
                            onStopSharing = { viewModel.stopSharingLook(look.id) },
                            onDelete = { viewModel.deleteLook(look.id) },
                            onDismiss = { hideSheet() },
                        )
                    }
                }

                HomeSheet.LookTime -> LookRuleSheet(
                    visible = up,
                    looks = looks,
                    onAdd = { rule ->
                        viewModel.setLookRules(settings.lookRules + rule)
                        hideSheet()
                    },
                    onDismiss = { hideSheet() },
                )

                is HomeSheet.LookCode -> LookCodeSheet(
                    visible = up,
                    initial = current.code,
                    open = viewModel::openLookCode,
                    onDismiss = { hideSheet() },
                )

                HomeSheet.GlassStudio -> GlassStudioSheet(
                    visible = up,
                    tuning = settings.glassTuning,
                    onChange = viewModel::setGlassTuning,
                    onDismiss = { hideSheet() },
                )

                is HomeSheet.WidgetColour -> {
                    val target = layout.firstOrNull { it.id == current.id }
                    val colour = when (target) {
                        is WidgetCell -> target.accent
                        is FolderCell -> target.colour
                        else -> null
                    }
                    if (target != null && colour != null) {
                        WidgetColourSheet(
                            visible = up,
                            current = colour,
                            onPick = { picked -> viewModel.setItemColour(target.id, picked) },
                            onDismiss = { hideSheet() },
                        )
                    }
                }

                HomeSheet.FontLicences -> FontLicenceSheet(visible = up, onDismiss = { hideSheet() })

                null -> Unit
            }
        }

        // ------------------------------------------------------------ the new look
        // Over everything else, since it has to be answered first. Either answer means it is not
        // due any more, which is what takes it away.
        NewLookSheet(
            visible = askingNewLook,
            settings = settings,
            // As Customise's "Use the new look": the grid is worked out for this screen and the
            // home screen settled into it, as for any grid.
            onSwitch = { viewModel.updateSettings { NewLook.apply(it) } },
            onKeep = { viewModel.updateSettings { it.copy(newLookAsked = true) } },
            home = homePreview,
            free = settings.freeform && plus.active,
        )
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
private fun RemoveTarget(armed: Boolean, hovering: Boolean, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val fill by animateColorAsState(
        targetValue = when {
            armed -> palette.destructive.copy(alpha = 0.55f)
            hovering -> palette.destructive.copy(alpha = 0.18f)
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
    /** A work app: it opens, and has its app info, but stays in the drawer's Work tab. */
    work: Boolean = false,
    label: String,
    pinned: Boolean,
    shortcuts: List<AppShortcut>,
    onShortcut: (AppShortcut) -> Unit,
    onAddShortcut: (AppShortcut) -> Unit,
    onEdit: () -> Unit,
    onOpen: () -> Unit,
    onAddToHome: () -> Unit,
    onToggleDock: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    ZenithSheet(visible = visible, onDismiss = onDismiss, title = label) {
        var row = 0
        ShortcutRows(shortcuts, onRun = { onShortcut(it); onDismiss() }, onAdd = { onAddShortcut(it); onDismiss() })
        MenuRow(Icons.AutoMirrored.Rounded.OpenInNew, "Open", if (work) "In your work profile" else null, index = row++) { onOpen(); onDismiss() }
        if (work) {
            MenuRow(Icons.Rounded.Info, "App info", index = row) { onAppInfo(); onDismiss() }
        } else {
            MenuRow(Icons.Rounded.AddHome, "Add to home screen", index = row++) { onAddToHome(); onDismiss() }
            MenuRow(Icons.Rounded.Edit, "Edit", "Its name, its icon, or no name at all", index = row++) { onEdit() }
            // Apps go into the dock by dragging them there; the menu only offers the way back out.
            if (pinned) MenuRow(Icons.Rounded.PushPin, "Remove from dock", index = row++) { onToggleDock(); onDismiss() }
            MenuRow(Icons.Rounded.Info, "App info", index = row++) { onAppInfo(); onDismiss() }
            MenuRow(Icons.Rounded.Delete, "Uninstall", danger = true, index = row) { onUninstall(); onDismiss() }
        }
    }
}

/**
 * What a swipe down will do, on a pill of glass that comes down from under the status bar as the
 * page is pulled, and goes back up as the page springs home. It fills with the accent, with a
 * tick, from the moment letting go would do it. Moved by being placed, so its glass keeps up.
 */
@Composable
private fun PullHint(progress: () -> Float, armed: Boolean, label: String, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val haptic = rememberHaptics()
    LaunchedEffect(armed) { if (armed) haptic.tick() }
    val shown by remember { derivedStateOf { progress() > 0.001f } }
    if (!shown) return
    val fill by animateColorAsState(if (armed) palette.accent.copy(alpha = 0.88f) else Color.Transparent, tween(160), label = "pullFill")
    val ink by animateColorAsState(if (armed) inkOnTint(palette.accent) else palette.onGlass, tween(160), label = "pullInk")
    Row(
        modifier
            .statusBarsPadding()
            .padding(top = 10.dp)
            .offset { IntOffset(0, (-28.dp.toPx() * (1f - progress().coerceIn(0f, 1f))).roundToInt()) }
            .graphicsLayer {
                val p = progress().coerceIn(0f, 1f)
                alpha = p
                val s = 0.86f + 0.14f * p + 0.04f * (progress() - 1f).coerceIn(0f, 0.4f)
                scaleX = s
                scaleY = s
            }
            .glass(ZenithShapes.pill, strength = 1.1f, solid = 0.35f)
            .background(fill, ZenithShapes.pill)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            tint = ink,
            modifier = Modifier
                .size(18.dp)
                // The arrow leans further down the nearer it is to happening.
                .graphicsLayer { translationY = 3.dp.toPx() * progress().coerceIn(0f, 1f) },
        )
        Spacer(Modifier.width(6.dp))
        Text(label, color = ink, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/**
 * Before Android's accessibility page opens: what the lock helper is, and that it is the only way
 * Android leaves a home app to lock the phone. It is asked for here, once, in words, because
 * Android's own page only says that an app wants control.
 */
@Composable
private fun LockHelperSheet(visible: Boolean, onOpen: () -> Unit, onDismiss: () -> Unit) {
    val palette = LocalPalette.current
    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Lock with a gesture") {
        Text(
            "Android only lets a home app lock the phone through an accessibility service. Zenith's lock helper " +
                "is one that does nothing else: it presses lock when you use the gesture, asks for no events and " +
                "cannot see the screen. Nothing on it is read, kept or sent.",
            color = palette.onGlass,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            modifier = Modifier.appearIn(delayMillis = staggerDelay(0), riseFrom = 18f),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "In the list that opens, choose Zenith lock helper and turn it on. Android asks you to confirm.",
            color = palette.onGlassMuted,
            fontSize = 13.sp,
            modifier = Modifier.appearIn(delayMillis = staggerDelay(1), riseFrom = 18f),
        )
        Spacer(Modifier.height(18.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.appearIn(delayMillis = staggerDelay(2), riseFrom = 18f),
        ) {
            GlassButton(text = "Not now", onClick = onDismiss, modifier = Modifier.weight(1f))
            GlassButton(
                text = "Open settings",
                filled = true,
                onClick = {
                    onOpen()
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * What the app picture helper is, before Android's page for it opens: also the disclosure Play
 * asks for, of what it takes and what is done with it.
 */
@Composable
private fun AppPicturesSheet(visible: Boolean, onOpen: () -> Unit, onDismiss: () -> Unit) {
    val palette = LocalPalette.current
    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "App pictures") {
        Text(
            "Android only gives a home app a picture of another app through an accessibility service. Zenith's " +
                "app picture helper is one that does nothing else: it takes a picture of the app in front every few " +
                "seconds, so that app can be seen going back into its icon when you close it, and coming out of it " +
                "when you open it again.",
            color = palette.onGlass,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            modifier = Modifier.appearIn(delayMillis = staggerDelay(0), riseFrom = 18f),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "It cannot read what is on the screen. The pictures are kept in memory only, for the last few apps and " +
                "five minutes at most, and all go when the screen goes off. Nothing is saved or sent anywhere. Apps " +
                "that hide themselves from screenshots, such as banks, are left out. It costs a little battery while " +
                "an app is open.",
            color = palette.onGlass,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            modifier = Modifier.appearIn(delayMillis = staggerDelay(1), riseFrom = 18f),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "In the list that opens, choose Zenith app pictures and turn it on; on some phones it is under Installed " +
                "apps. If Android says the setting is restricted, allow restricted settings from the menu in Zenith's " +
                "App info first.",
            color = palette.onGlassMuted,
            fontSize = 13.sp,
            modifier = Modifier.appearIn(delayMillis = staggerDelay(2), riseFrom = 18f),
        )
        Spacer(Modifier.height(18.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.appearIn(delayMillis = staggerDelay(3), riseFrom = 18f),
        ) {
            GlassButton(text = "Not now", onClick = onDismiss, modifier = Modifier.weight(1f))
            GlassButton(
                text = "Open settings",
                filled = true,
                onClick = {
                    onOpen()
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** How far the page shows a pull of [pulled] pixels: all of it at first, less and less after, never past [reach]. */
private fun band(pulled: Float, reach: Float): Float =
    if (pulled <= 0f || reach <= 0f) 0f else reach * (1f - 1f / (pulled / reach * 0.55f + 1f))

/** A drag on the page not yet told apart, the drawer's, or the swipe-down gesture's. */
private const val DRAG_UNDECIDED = 0
private const val DRAG_DRAWER = 1
private const val DRAG_PULL = 2

/** How far a swipe down has to be pulled before letting go does it. */
private val PULL_FIRE = 76.dp

/** The most the page comes down with it, however far it is pulled. */
private val PULL_REACH = 64.dp

/** How far two fingers have to travel together to be a swipe. */
private val TWO_FINGER_SWIPE = 70.dp

/** Android's view configuration with a shorter long press, for picking things up on the home screen. */
private class QuickHold(base: androidx.compose.ui.platform.ViewConfiguration) :
    androidx.compose.ui.platform.ViewConfiguration by base {
    override val longPressTimeoutMillis: Long get() = HoldTuning.HOLD_MS
}

/**
 * The page in view, read here rather than by the home screen itself: the home screen is a great
 * deal to compose, and reading the page where it is composed would compose all of it again half
 * way through every swipe, just for the one thing that wants to know.
 */
@Composable
private fun CurrentPage(pagerState: androidx.compose.foundation.pager.PagerState, content: @Composable (Int) -> Unit) {
    content(pagerState.currentPage)
}
