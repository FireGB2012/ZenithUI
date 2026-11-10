package com.gabriel.zenithui.ui.home

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddHome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.AppShortcut
import com.gabriel.zenithui.data.FolderCell
import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.AppCell
import com.gabriel.zenithui.data.HostedWidgetCell
import com.gabriel.zenithui.data.ShortcutCell
import com.gabriel.zenithui.data.StackCell
import com.gabriel.zenithui.data.StackMember
import com.gabriel.zenithui.data.StackRules
import com.gabriel.zenithui.data.WidgetCell
import com.gabriel.zenithui.data.Widgets
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LayersClear
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Public
import com.gabriel.zenithui.ui.common.AppIcon
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.SheetPull
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.onboarding.TipList
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.glass
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** How far a sheet comes up when it is pulled up, at the very most. */
private val SHEET_GIVE = 28.dp

/**
 * How long a sheet takes to go once it is closed, and a little over. One sheet opened from another
 * keeps the old one for this long, so it can be seen going down as the new one comes up.
 */
const val SHEET_GONE_MS = 340

/**
 * Bottom-anchored glass sheet. It comes up from the edge on a spring, the home screen dimming
 * behind it, and goes back the way it came.
 *
 * Take hold of it and it follows your finger: down as far as you pull, up only a little and less
 * the further. Let go and it settles back, or, past a third of its own height or with a flick,
 * carries on down and is gone. Pulling down on what is inside it does the same, once that has
 * nowhere left to scroll. See [SheetPull] for the sums.
 */
@Composable
fun ZenithSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
    /** How much of the screen the sheet may take. Settings, with its pages, gets more. */
    maxHeightFraction: Float = 0.72f,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * maxHeightFraction
    val scope = rememberCoroutineScope()
    val dismiss by rememberUpdatedState(onDismiss)
    val give = with(LocalDensity.current) { SHEET_GIVE.toPx() }

    // Closed to begin with, even when it is first shown already open: a sheet is opened by being
    // put on screen, and without this it would simply be there instead of coming up from the edge.
    val shown = remember { MutableTransitionState(false) }
    shown.targetState = visible

    // While a finger has it the pull is written straight from the drag, with no coroutine in
    // between, so the sheet sits exactly under the thumb. Let go, and a spring takes it from there.
    var held by remember { mutableStateOf(false) }
    var pulled by remember { mutableFloatStateOf(0f) }
    val settling = remember { Animatable(0f) }
    /** The sheet's own height with the gap under it: how far down is out of sight. */
    var height by remember { mutableFloatStateOf(1f) }

    /** How far down from its place the sheet is right now. Read while placing and drawing. */
    fun down(): Float = if (held) SheetPull.shown(pulled, give) else settling.value

    fun take() {
        if (held) return
        pulled = SheetPull.raw(settling.value, give)
        held = true
        scope.launch { settling.stop() }
    }

    fun letGo(velocity: Float) {
        if (!held) return
        val from = down()
        scope.launch {
            // Handed from the finger to the spring between two frames, at the same spot.
            settling.snapTo(from)
            held = false
            if (SheetPull.dismisses(from, height, velocity)) {
                // Closed from the moment it is thrown, so nothing unseen is left in the way of
                // the next touch; it carries on down at the speed it was thrown at as it goes.
                dismiss()
                settling.animateTo(
                    targetValue = height,
                    animationSpec = if (reduced) tween(Motion.REDUCED_MS) else spring(dampingRatio = 1f, stiffness = 380f),
                    initialVelocity = if (reduced) 0f else velocity,
                )
            } else {
                settling.animateTo(
                    targetValue = 0f,
                    animationSpec = if (reduced) tween(Motion.REDUCED_MS) else Motion.sheet(),
                    initialVelocity = if (reduced) 0f else velocity,
                )
            }
        }
    }

    // Shown again after being thrown away, it starts from its own place, not from where it went.
    LaunchedEffect(visible) {
        if (visible) {
            held = false
            pulled = 0f
            settling.snapTo(0f)
        }
    }

    // What is inside scrolls first. Only what it cannot use moves the sheet.
    val pullThrough = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Part way down already: going back up puts the sheet back before anything scrolls.
                if (!held || pulled <= 0f || available.y >= 0f) return Offset.Zero
                val back = maxOf(available.y, -pulled)
                pulled += back
                return Offset(0f, back)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
                take()
                pulled += available.y
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!held) return Velocity.Zero
                letGo(available.y)
                return available
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visibleState = shown,
            enter = fadeIn(tween(if (reduced) Motion.REDUCED_MS else 300)),
            exit = fadeOut(tween(if (reduced) Motion.REDUCED_MS else 260)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    // It clears as the sheet is pulled away, so nothing is left dimmed behind a
                    // sheet that has already gone.
                    .drawBehind { drawRect(Color.Black, alpha = 0.35f * (1f - (down() / height).coerceIn(0f, 1f))) }
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() },
            )
        }

        AnimatedVisibility(
            visibleState = shown,
            // With movement reduced a sheet does not travel: it is there, and then it is not.
            enter = if (reduced) fadeIn(tween(Motion.REDUCED_MS)) else slideInVertically(Motion.sheet()) { it } + fadeIn(tween(220)),
            exit = if (reduced) fadeOut(tween(Motion.REDUCED_MS)) else slideOutVertically(tween(320)) { it } + fadeOut(tween(260)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .onSizeChanged { height = it.height.toFloat().coerceAtLeast(1f) }
                    // Moved while placing rather than on a layer, so its glass looks again at
                    // where it is all the way down.
                    .offset { IntOffset(0, down().roundToInt()) }
                    .padding(10.dp)
                    // The regular material provides a readable body without a second opaque fill.
                    .glass(RoundedCornerShape(34.dp), strength = 1.2f, solid = 0.65f)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }
                    .nestedScroll(pullThrough)
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            take()
                            pulled += delta
                        },
                        onDragStopped = { velocity -> letGo(velocity) },
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        // The handle draws out a little while the sheet is in your hand.
                        .graphicsLayer { scaleX = 1f + 0.5f * (abs(down()) / (height * SheetPull.AWAY)).coerceIn(0f, 1f) }
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(palette.onGlass.copy(alpha = 0.25f))
                )
                if (title != null) {
                    Spacer(Modifier.height(14.dp))
                    Text(title, color = palette.onGlass, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
                content()
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun MenuRow(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    danger: Boolean = false,
    index: Int = 0,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    val tint = if (danger) palette.destructive else palette.onGlass
    Row(
        Modifier
            .fillMaxWidth()
            .appearIn(delayMillis = staggerDelay(index), riseFrom = 22f)
            .pressBounce(0.97f)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, color = palette.onGlassMuted, fontSize = 12.sp)
            }
        }
    }
}

/**
 * An app's own shortcuts, at the top of its menu: a route home, one chat. Each runs at a tap, and
 * the button beside it puts it on the home screen as an icon of its own. Android hands them over
 * a moment after the menu is up, so they open out above the rest and rise in one after another.
 */
@Composable
fun ShortcutRows(
    shortcuts: List<AppShortcut>,
    onRun: (AppShortcut) -> Unit,
    onAdd: (AppShortcut) -> Unit,
) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    // What was last listed stays while it folds away, so it is seen to go rather than vanish.
    val listed = remember { arrayOf(shortcuts) }
    if (shortcuts.isNotEmpty()) listed[0] = shortcuts
    AnimatedVisibility(
        visible = shortcuts.isNotEmpty(),
        enter = if (reduced) fadeIn(tween(Motion.REDUCED_MS)) else expandVertically(Motion.sheet()) + fadeIn(tween(200)),
        exit = if (reduced) fadeOut(tween(Motion.REDUCED_MS)) else shrinkVertically(tween(220)) + fadeOut(tween(160)),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(palette.onGlass.copy(alpha = 0.06f))
                .padding(vertical = 4.dp),
        ) {
            listed[0].forEachIndexed { index, shortcut ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .appearIn(delayMillis = staggerDelay(index), riseFrom = 18f)
                        .pressBounce(0.97f)
                        .clickable { onRun(shortcut) }
                        .padding(start = 12.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(key = shortcut.key, label = shortcut.label, size = 30.dp)
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = shortcut.label,
                        color = palette.onGlass,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier
                            .size(38.dp)
                            .pressBounce()
                            .clip(RoundedCornerShape(50))
                            .clickable { onAdd(shortcut) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.AddHome,
                            contentDescription = "Put ${shortcut.label} on the home screen",
                            tint = palette.onGlassMuted,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }
        }
    }
}

/** What another app's widget is called, by its host number; "Widget" if Android will not say. */
internal fun hostedName(context: android.content.Context, appWidgetId: Int): String = runCatching {
    android.appwidget.AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId)?.loadLabel(context.packageManager)
}.getOrNull() ?: "Widget"

/** What a member of a stack is called. */
internal fun memberName(context: android.content.Context, member: StackMember): String =
    if (member.hosted) hostedName(context, member.appWidgetId) else Widgets.byType(member.type)?.title ?: "Widget"

@Composable
fun ItemMenuSheet(
    visible: Boolean,
    item: HomeItem,
    appsByKey: Map<String, AppInfo>,
    onOpen: (String) -> Unit,
    onAppInfo: (String) -> Unit,
    onRename: (String) -> Unit,
    onResize: () -> Unit,
    onRemove: () -> Unit,
    onUninstall: (String) -> Unit,
    onChooseToggles: () -> Unit = {},
    /** Opens Customise: on a full page, this is the easy way there. */
    onCustomize: () -> Unit = {},
    /** The app's own shortcuts, once Android has said what they are. */
    shortcuts: List<AppShortcut> = emptyList(),
    onShortcut: (AppShortcut) -> Unit = {},
    onAddShortcut: (AppShortcut) -> Unit = {},
    /** Changes the app's name or icon. */
    onEdit: (String) -> Unit = {},
    /** Opens the gallery to put another widget on this one, making or growing a stack. */
    onStack: () -> Unit = {},
    /** A pane of glass behind another app's widget, or none. */
    onGlassBehind: (Boolean) -> Unit = {},
    /** Takes the member of a stack at this place out onto a spot of its own. */
    onUnstack: (Int) -> Unit = {},
    /** Sets up one of Zenith's widgets that needs something chosen: its cities, its date, its photos. */
    onSetUp: (String) -> Unit = {},
    /** A colour of its own for one of Zenith's widgets, or a folder. */
    onColour: () -> Unit = {},
    /** The layout is locked: nothing in the menu moves, resizes or removes. */
    locked: Boolean = false,
    onUnlock: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val title = when (item) {
        is AppCell -> appsByKey[item.key]?.label ?: item.packageName
        is FolderCell -> item.title
        is WidgetCell -> Widgets.byType(item.type)?.title ?: "Widget"
        is ShortcutCell -> item.label
        is HostedWidgetCell -> remember(item.appWidgetId) { hostedName(context, item.appWidgetId) }
        is StackCell -> "Widget stack"
    }

    ZenithSheet(visible = visible, onDismiss = onDismiss, title = title) {
        // Each row a beat behind the one above it, so the menu is read from the top as it arrives.
        var row = 0
        if (locked) {
            MenuRow(Icons.Rounded.LockOpen, "The layout is locked", "Unlock it to move, resize or remove things", index = row++) { onUnlock() }
        }
        when (item) {
            is AppCell -> {
                ShortcutRows(shortcuts, onRun = { onShortcut(it); onDismiss() }, onAdd = { onAddShortcut(it); onDismiss() })
                if (!locked) MenuRow(Icons.Rounded.OpenInFull, "Resize and move", "Pull its corner to any size, or move it a cell at a time", index = row++) { onResize() }
                MenuRow(Icons.Rounded.Edit, "Edit", "Its name, its icon, or no name at all", index = row++) { onEdit(item.key) }
                MenuRow(Icons.AutoMirrored.Rounded.OpenInNew, "Open", index = row++) { onOpen(item.key); onDismiss() }
                MenuRow(Icons.Rounded.Info, "App info", "Storage, permissions and notifications, in the phone's settings", index = row++) { onAppInfo(item.key); onDismiss() }
                if (!locked) MenuRow(Icons.Rounded.Home, "Remove from home", "The app stays installed, and in the drawer", danger = true, index = row++) { onRemove(); onDismiss() }
                MenuRow(Icons.Rounded.Delete, "Uninstall", "Takes the app off the phone", danger = true, index = row++) { onUninstall(item.key); onDismiss() }
            }

            is ShortcutCell -> {
                MenuRow(Icons.AutoMirrored.Rounded.OpenInNew, "Open", index = row++) { onOpen(item.key); onDismiss() }
                if (!locked) MenuRow(Icons.Rounded.Home, "Remove from home", "The app keeps its shortcut", danger = true, index = row++) { onRemove(); onDismiss() }
            }

            is FolderCell -> {
                MenuRow(Icons.Rounded.Edit, "Rename folder", index = row++) { onRename(item.id); }
                MenuRow(Icons.Rounded.Palette, "Colour", if (item.colour == 0L) "Clear glass, or a colour of its own" else "Its own colour", index = row++) { onColour() }
                if (!locked) MenuRow(Icons.Rounded.Home, "Remove from home", "Its apps stay installed, and in the drawer", danger = true, index = row++) { onRemove(); onDismiss() }
            }

            is WidgetCell -> {
                when (Widgets.baseType(item.type)) {
                    "toggles" -> MenuRow(Icons.Rounded.Tune, "Choose buttons", "Pick which controls it shows", index = row++) { onChooseToggles() }
                    "world" -> MenuRow(Icons.Rounded.Public, "Choose cities", "Up to four, in the order shown", index = row++) { onSetUp("world") }
                    "countdown" -> MenuRow(Icons.Rounded.Event, "Set the date", "What it counts down to, and when", index = row++) { onSetUp("countdown") }
                    "photo" -> MenuRow(Icons.Rounded.PhotoLibrary, "Choose photos", "Up to twelve; a different one each time you come home", index = row++) {
                        onSetUp("photo")
                        onDismiss()
                    }
                    "weather" -> MenuRow(Icons.Rounded.LocationOn, "Choose a place", "Where the forecast is for", index = row++) { onSetUp("weather") }
                    "todo" -> MenuRow(Icons.Rounded.DoneAll, "Clear what is done", "Takes the ticked ones off the list", index = row++) {
                        onSetUp("todo")
                        onDismiss()
                    }
                    "media" -> if (!com.gabriel.zenithui.data.NotificationDots.allowed(context)) {
                        MenuRow(Icons.Rounded.MusicNote, "Let Zenith see what is playing", "Android's notification access, the one the dots use", index = row++) {
                            onSetUp("media")
                            onDismiss()
                        }
                    }
                }
                MenuRow(Icons.Rounded.Palette, "Colour", if (item.accent == 0L) "Plain, or a colour of its own" else "A colour of its own", index = row++) { onColour() }
                if (!locked) MenuRow(Icons.Rounded.OpenInFull, "Resize and move", "Pull its corner, pick a size, or move it a cell at a time", index = row++) { onResize() }
                if (!locked) MenuRow(Icons.Rounded.Layers, "Stack another widget here", "Swipe up and down between them, in this one spot", index = row++) { onStack() }
                if (!locked) MenuRow(Icons.Rounded.Home, "Remove widget", "It can be added again from Widgets", danger = true, index = row++) { onRemove(); onDismiss() }
            }

            is HostedWidgetCell -> {
                if (!locked) MenuRow(Icons.Rounded.OpenInFull, "Resize and move", "Pull its corner, or move it a cell at a time", index = row++) { onResize() }
                MenuRow(
                    if (item.glass) Icons.Rounded.LayersClear else Icons.Rounded.Layers,
                    if (item.glass) "Take the glass away" else "Put glass behind it",
                    if (item.glass) "The widget on its own, as its app draws it" else "A pane of glass, for a widget with no background of its own",
                    index = row++,
                ) { onGlassBehind(!item.glass); onDismiss() }
                if (!locked) MenuRow(Icons.Rounded.Layers, "Stack another widget here", "Swipe up and down between them, in this one spot", index = row++) { onStack() }
                if (!locked) MenuRow(Icons.Rounded.Home, "Remove widget", "It can be added again from Widgets", danger = true, index = row++) { onRemove(); onDismiss() }
            }

            is StackCell -> {
                if (!locked) MenuRow(Icons.Rounded.OpenInFull, "Resize and move", "Every widget in it takes the new size", index = row++) { onResize() }
                if (item.members.size < StackRules.MOST) {
                    if (!locked) MenuRow(Icons.Rounded.Layers, "Stack another widget", "${item.members.size} in it now", index = row++) { onStack() }
                }
                item.members.forEachIndexed { index, member ->
                    val name = remember(member) { memberName(context, member) }
                    MenuRow(Icons.AutoMirrored.Rounded.Logout, "Take $name out", "Onto a spot of its own on this page", index = row++) {
                        onUnstack(index)
                        onDismiss()
                    }
                }
                if (!locked) MenuRow(Icons.Rounded.Home, "Remove stack", "All ${item.members.size} widgets in it", danger = true, index = row++) { onRemove(); onDismiss() }
            }
        }
        MenuRow(Icons.Rounded.Tune, "Customise home screen", "Widgets, wallpaper and settings", index = row) { onCustomize() }
    }
}

@Composable
fun CustomizeSheet(
    visible: Boolean,
    /** The wallpaper as it is now, for its tile to show; null when Zenith has no copy of it. */
    wallpaper: ImageBitmap?,
    /** What the wallpaper is called, when it is one of Zenith's own designs. */
    wallpaperName: String?,
    onWallpaper: () -> Unit,
    onHelp: () -> Unit,
    onAmbient: () -> Unit,
    onWidgets: () -> Unit,
    onSettings: () -> Unit,
    onDefaultLauncher: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val isHome = remember(visible) {
        context.getSystemService(android.app.role.RoleManager::class.java)
            ?.isRoleHeld(android.app.role.RoleManager.ROLE_HOME) == true
    }
    val palette = LocalPalette.current
    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Customise") {
        // The four things people come here for, as big targets with room between them; the rest
        // underneath, in a group of their own with a line between each.
        Row(horizontalArrangement = Arrangement.spacedBy(TILE_GAP)) {
            QuickTile(Icons.Rounded.Dashboard, "Widgets", "Add a clock, calendar, note, battery or toggles", index = 0, modifier = Modifier.weight(1f)) { onWidgets() }
            QuickTile(
                icon = Icons.Rounded.Image,
                label = "Wallpaper",
                detail = when {
                    wallpaperName != null -> "$wallpaperName, one of Zenith's own"
                    wallpaper != null -> "A photo of yours"
                    else -> "Zenith's own designs, or a photo of yours"
                },
                picture = wallpaper,
                index = 1,
                modifier = Modifier.weight(1f),
            ) { onWallpaper() }
        }
        Spacer(Modifier.height(TILE_GAP))
        Row(horizontalArrangement = Arrangement.spacedBy(TILE_GAP)) {
            QuickTile(Icons.Rounded.Tune, "Settings", "Light or dark, grid, glass, motion and backup", index = 2, modifier = Modifier.weight(1f)) { onSettings() }
            QuickTile(Icons.Rounded.Bedtime, "Bedside clock", "A dim full-screen clock for the night", index = 3, modifier = Modifier.weight(1f)) { onAmbient() }
        }
        Spacer(Modifier.height(18.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(palette.onGlass.copy(alpha = 0.07f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            if (!isHome) {
                MenuRow(Icons.Rounded.Home, "Make Zenith your home screen", "So the home button always comes back here", index = 4) { onDefaultLauncher(); onDismiss() }
                // Under the words, not under the icon, as between the rows of Settings.
                Box(
                    Modifier
                        .padding(start = 58.dp, end = 8.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(palette.onGlass.copy(alpha = 0.08f))
                )
            }
            MenuRow(Icons.Rounded.HelpOutline, "How it works", "Every gesture in one page", index = 5) { onHelp() }
        }
    }
}

/** Between the tiles of Customise, either way. */
private val TILE_GAP = 12.dp

/**
 * A big square-ish button: the icon in the corner, the name underneath with a line saying what is
 * behind it, lit with the accent. Given a [picture] it shows that behind it, under a wash of the
 * page's own ground that thickens towards the foot so the words read on any picture: the
 * Wallpaper tile shows the wallpaper you have.
 */
@Composable
private fun QuickTile(
    icon: ImageVector,
    label: String,
    detail: String,
    index: Int,
    modifier: Modifier = Modifier,
    picture: ImageBitmap? = null,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    val ground = palette.backdrop.first()
    Box(
        modifier
            .height(128.dp)
            .appearIn(delayMillis = staggerDelay(index), riseFrom = 18f)
            .pressBounce(0.95f)
            .clip(RoundedCornerShape(22.dp))
            .background(palette.onGlass.copy(alpha = 0.07f))
            .clickable(onClick = onClick),
    ) {
        if (picture != null) {
            Image(
                bitmap = picture,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to ground.copy(alpha = 0.08f),
                            0.45f to ground.copy(alpha = 0.4f),
                            1f to ground.copy(alpha = 0.92f),
                        )
                    )
            )
        }
        Column(
            Modifier
                .matchParentSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    // On a picture the plate is the page's ground, so the icon stands off it.
                    .background(if (picture != null) ground.copy(alpha = 0.72f) else palette.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(label, color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(2.dp))
                Text(detail, color = palette.onGlassMuted, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun HelpSheet(visible: Boolean, onDismiss: () -> Unit) {
    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "How it works") {
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TipList()
        }
    }
}
