package com.gabriel.zenithui.ui.home

import com.gabriel.zenithui.ui.common.WideAppIcon
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.zIndex
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.gabriel.zenithui.data.AppCell
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.DropPlanner
import com.gabriel.zenithui.data.FolderCell
import com.gabriel.zenithui.data.HomeGeometry
import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.PxRect
import com.gabriel.zenithui.data.ROW_UNITS
import com.gabriel.zenithui.data.WidgetCell
import com.gabriel.zenithui.data.Widgets
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
import androidx.compose.ui.unit.round
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Everything the drag-and-drop interaction needs to know, hoisted above the pager. */
@Stable
class HomeDragState {
    var item by mutableStateOf<HomeItem?>(null)
        private set
    var pointer by mutableStateOf(Offset.Zero)
    var grab by mutableStateOf(Offset.Zero)
    /** The icon the finger happens to be over. */
    var hoverCandidateId by mutableStateOf<String?>(null)

    /**
     * The icon a folder would actually be made with. You have to rest on a neighbour before it
     * counts, so carrying an icon across a full page never swallows it into a folder by accident.
     */
    var hoverItemId by mutableStateOf<String?>(null)
    /** The bin's own rectangle. The pointer has to be inside it, not merely above its bottom
     *  edge — widgets live at the top of the page, so a whole top strip cannot be a trapdoor. */
    var removeZone by mutableStateOf(Rect.Zero)
    var gridOrigin by mutableStateOf(Offset.Zero)
    var cellWidth by mutableStateOf(0f)
    var cellHeight by mutableStateOf(0f)
    /** The icon size the live page settled on, so the ghost is drawn at exactly that size. */
    var iconSize by mutableStateOf(48.dp)

    /** The same in pixels, for telling how close to an icon's middle the carried one is. */
    var iconSizePx by mutableStateOf(0f)

    /** Where everything sits on the live page, so the ghost and the dock are cut to the same lines. */
    var geometry by mutableStateOf<HomeGeometry?>(null)

    /**
     * What has stepped aside for the carried item while it rests over a spot, as a phone's launcher
     * shows it: the page, and the plan it is for. Letting go there does exactly this.
     */
    var preview by mutableStateOf<DropPreview?>(null)

    /** The item last let go and where its grey copy was, so it lands from under your finger. */
    var landing by mutableStateOf<Landing?>(null)

    val isDragging: Boolean get() = item != null

    /** Your finger is on the bin right now. */
    val overRemove: Boolean
        get() = item != null && !removeZone.isEmpty && removeZone.contains(pointer)

    /**
     * Only an armed bin deletes anything. The bin lives over the top row, where widgets are, so
     * passing through it while you carry an icon upwards must never throw the icon away: you have
     * to hold still on it, and it turns red first.
     */
    var removeArmed by mutableStateOf(false)

    fun begin(target: HomeItem, pointerRoot: Offset, itemRoot: Offset) {
        item = target
        pointer = pointerRoot
        grab = pointerRoot - itemRoot
    }

    fun clear() {
        item = null
        hoverItemId = null
        hoverCandidateId = null
        removeArmed = false
        preview = null
    }

    /** The grey copy's top left corner on the page's grid, in pixels. */
    val ghostOnGrid: Offset get() = pointer - grab - gridOrigin

    /**
     * Where the top left corner of the carried item is on the grid, in columns (x) and row units
     * (y), or null when it has been carried right off the grid.
     *
     * An item that only hangs over an edge — a wide widget let go near the side, an icon dragged
     * a little too low — still counts, and is pulled back onto the grid rather than refused.
     * Refusing it made the item jump back to where it started, which reads as the launcher
     * ignoring you.
     */
    fun exactTopLeft(columns: Int, unitRows: Int): Offset? {
        val current = item ?: return null
        if (cellWidth <= 0f || cellHeight <= 0f) return null
        val col = ghostOnGrid.x / cellWidth
        val row = ghostOnGrid.y / cellHeight
        // Only when the middle of what you are carrying is off the grid is it truly off it.
        val centreX = col + current.spanX / 2f
        val centreY = row + current.spanY / 2f
        if (centreX < -0.5f || centreX > columns + 0.5f) return null
        if (centreY < -ROW_UNITS / 2f || centreY > unitRows + ROW_UNITS / 2f) return null
        return Offset(col, row)
    }
}

/** Things stepping aside on [page] for the carried item, as [plan] has them. */
@Immutable
data class DropPreview(val page: Int, val plan: DropPlanner.Plan)

/** Item [id] was just let go with its grey copy's corner at [ghostOnGrid], in the page's pixels. */
@Immutable
data class Landing(val id: String, val ghostOnGrid: Offset)

@Composable
fun HomePage(
    page: Int,
    active: Boolean,
    items: List<HomeItem>,
    settings: ZenithSettings,
    appsByKey: Map<String, AppInfo>,
    drag: HomeDragState,
    resizingId: String?,
    /** Asks for a new size, in cells across and row units down; false when there is no room. */
    onResize: (HomeItem, Int, Int) -> Boolean,
    /** Tapping the item being resized puts it down, just like tapping anywhere else. */
    onResizeDone: () -> Unit = {},
    onOpenApp: (String) -> Unit,
    onOpenFolder: (FolderCell) -> Unit,
    onItemMenu: (HomeItem) -> Unit,
    onSearch: () -> Unit,
    onNoteChange: (String) -> Unit,
    onAmbient: () -> Unit,
    onDrop: () -> Unit,
    /**
     * Where the carried item would land if let go now, and what would step aside for it. Only
     * ever for the page in view; the hint and the drop both come from it, so they always agree.
     */
    dropPlan: DropPlanner.Plan? = null,
    /** Plus free placement is on: the drop hint follows the exact spot, not the cell. */
    freePlacement: Boolean = false,
    /** Right after setup the page flies in; any other time it is simply there. */
    animateEntrance: Boolean = false,
    /** How far this page is from the middle of the screen while swiping, -1 to 1. */
    pageOffset: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val density = LocalDensity.current
    // Items already on the page when it first appears never animate in; ones added later do.
    val firstIds = remember { items.map { it.id }.toSet() }
    val haptics = LocalHapticFeedback.current

    BoxWithConstraints(modifier.fillMaxSize()) {
        val geometry = remember(constraints.maxWidth, constraints.maxHeight, settings.columns, settings.rows, density.density, settings.showLabels) {
            HomeGeometry(constraints.maxWidth, constraints.maxHeight, settings.columns, settings.rows, density.density, settings.showLabels)
        }
        val cellWidthPx = geometry.cellWidth.toFloat()
        // One cell is a row unit tall; an icon spans ROW_UNITS of them.
        val cellHeightPx = geometry.unitHeight
        val iconSize = geometry.iconDp.dp
        if (active) {
            drag.cellWidth = cellWidthPx
            drag.cellHeight = cellHeightPx
            drag.iconSize = iconSize
            drag.iconSizePx = geometry.icon.toFloat()
            drag.geometry = geometry
        }
        val cellWidthDp = with(density) { cellWidthPx.toDp() }
        val cellHeightDp = with(density) { cellHeightPx.toDp() }

        // The grid itself: every cell the same whole number of pixels, with any left over shared
        // between the two edges. Everything below is placed from this box's corner.
        Box(
            Modifier
                .padding(start = with(density) { geometry.originX.toDp() }, top = with(density) { geometry.originY.toDp() })
                .size(with(density) { geometry.gridWidth.toDp() }, with(density) { geometry.gridHeight.toDp() })
                .onGloballyPositioned { coords ->
                    // Only the page in view owns the geometry drops are resolved against.
                    if (active) drag.gridOrigin = coords.positionInRoot()
                },
        ) {

        // Drop hint for the spot the dragged item would land in. When something is already there
        // the hint still shows the spot, because that is where the item goes: what is there steps
        // aside. Where nothing can make room there is no plan, and so no hint.
        val dragged = drag.item
        val plan = if (active) dropPlan else null
        if (dragged != null && plan != null) {
            val target = plan.target
            if (drag.hoverItemId == null) {
                // With free placement the item keeps the exact spot it is let go at, so the hint
                // shows that spot — worked out the same way the drop does — and glides after the
                // finger. It only jumps to a cell when the item lands beside where it is let go,
                // which is exactly when the drop would jump there too.
                val exact = freePlacement && plan.exact
                val hintTarget = if (exact) {
                    val topLeft = drag.ghostOnGrid
                    val offX = (topLeft.x / cellWidthPx - target.col).coerceIn(-0.49f, 0.49f)
                    val offY = (topLeft.y / cellHeightPx - target.row).coerceIn(-0.49f, 0.49f)
                    IntOffset(
                        ((target.col + offX) * cellWidthPx).roundToInt(),
                        ((target.row + offY) * cellHeightPx).roundToInt(),
                    )
                } else {
                    IntOffset(
                        (target.col * cellWidthPx).roundToInt(),
                        (target.row * cellHeightPx).roundToInt(),
                    )
                }
                val hintSpec: AnimationSpec<IntOffset> = if (freePlacement) {
                    spring(dampingRatio = 1f, stiffness = 420f)
                } else {
                    Motion.cell
                }
                val hintOffset by animateIntOffsetAsState(
                    targetValue = hintTarget,
                    animationSpec = hintSpec,
                    label = "dropHint",
                )

                Box(
                    Modifier
                        .offset { hintOffset }
                        .requiredSize(cellWidthDp * dragged.spanX, cellHeightDp * dragged.spanY),
                    contentAlignment = Alignment.Center,
                ) {
                    if (dragged is AppCell && !dragged.isPlainIcon) {
                        // A super icon's hint is the icon itself, not the cells around it.
                        val frame = geometry.superIconFrame(dragged.spanX, dragged.spanY / ROW_UNITS)
                        val round = with(density) { minOf(frame.width, frame.height).toDp() } * 0.26f
                        Box(
                            Modifier
                                .fillMaxSize()
                                .pixelFrame(frame)
                                .appearIn(fromScale = 0.8f, riseFrom = 0f)
                                .clip(RoundedCornerShape(round))
                                .background(palette.onGlass.copy(alpha = 0.12f))
                        )
                    } else if (dragged is WidgetCell) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .pixelFrame(geometry.widgetFrame(dragged.spanX, dragged.spanY))
                                .appearIn(fromScale = 0.8f, riseFrom = 0f)
                                .clip(ZenithShapes.tile)
                                .background(palette.onGlass.copy(alpha = 0.12f))
                        )
                    } else {
                        // Round the icon itself, a little bigger, so the hint sits where the icon will.
                        val pad = with(density) { 7.dp.roundToPx() }
                        val icon = geometry.iconFrame
                        Box(
                            Modifier
                                .fillMaxSize()
                                .pixelFrame(PxRect(icon.left - pad, icon.top - pad, icon.right + pad, icon.bottom + pad))
                                .appearIn(fromScale = 0.8f, riseFrom = 0f)
                                .clip(ZenithShapes.tile)
                                .background(palette.onGlass.copy(alpha = 0.12f))
                        )
                    }
                }
            }
        }

        // Only the page in view shows things stepping aside, and only for the plan made for it.
        val stepping = drag.preview?.takeIf { active && it.page == page }?.plan
        // Something stepping into the spot the carried item came from: the item's own copy there
        // gets out of its way. It is in your hand anyway.
        val carriedCovered = dragged != null && stepping != null && dragged.page == page &&
            stepping.moves.any { (id, spot) -> items.firstOrNull { it.id == id }?.let { covers(it, spot, dragged) } == true }

        items.forEachIndexed { index, item ->
            key(item.id) {
            var cellRoot by remember { mutableStateOf(Offset.Zero) }
            var contentRoot by remember { mutableStateOf(Offset.Zero) }
            var travelled by remember { mutableStateOf(0f) }
            // The gesture block below is keyed on the item's id and the cell geometry, none of
            // which change when a widget is resized — so without this it would keep dragging the
            // item at whatever span it had when the block was first launched.
            val liveItem by rememberUpdatedState(item)
            val liveItems by rememberUpdatedState(items)
            // A long press also ends in a pointer-up, which the tap detector would otherwise
            // report as a tap — opening an app or the clock behind the menu that just appeared.
            var longPressHandled by remember { mutableStateOf(false) }
            val hovered = drag.hoverItemId == item.id
            val resizing = item.id == resizingId
            val scale by animateFloatAsState(if (hovered) 1.18f else 1f, Motion.bouncy(), label = "hover")
            // The cell plus any free-placement offset, so a freely placed item sits exactly where
            // it was let go rather than on the grid line — or, while the carried item rests over
            // it, the spot it has stepped aside to.
            val aside = stepping?.moves?.get(item.id)
            val target = if (aside != null) {
                IntOffset((aside.col * cellWidthPx).roundToInt(), (aside.row * cellHeightPx).roundToInt())
            } else {
                IntOffset(
                    ((item.col + item.dx) * cellWidthPx).roundToInt(),
                    ((item.row + item.dy) * cellHeightPx).roundToInt(),
                )
            }
            // Just let go, it comes down from under your finger, the way a phone puts an icon
            // down, rather than travelling over from where it started.
            val landing = drag.landing?.takeIf { it.id == item.id }
            val arrivedByDrop = remember { landing != null }
            val cellOffset = remember { Animatable(landing?.ghostOnGrid?.round() ?: target, IntOffset.VectorConverter) }
            // Each landing plays once; anything that moves the item after that is a plain glide.
            val landed = remember { arrayOfNulls<Landing>(1) }
            var lastGeometry by remember { mutableStateOf(cellWidthPx to cellHeightPx) }
            // Glide when the item changes cell, but snap when the grid itself is re-measured.
            LaunchedEffect(target, cellWidthPx, cellHeightPx, landing) {
                val geometry = cellWidthPx to cellHeightPx
                val remeasured = geometry != lastGeometry
                lastGeometry = geometry
                when {
                    remeasured -> cellOffset.snapTo(target)
                    landing != null && landed[0] !== landing -> {
                        landed[0] = landing
                        cellOffset.snapTo(landing.ghostOnGrid.round())
                        cellOffset.animateTo(target, Motion.cell)
                    }
                    else -> cellOffset.animateTo(target, Motion.cell)
                }
            }
            val makingWay = carriedCovered && item.id == dragged?.id
            val wayAlpha by animateFloatAsState(if (makingWay) 0f else 1f, tween(140), label = "makingWay")

            Box(
                Modifier
                    // The item being resized draws, and takes touches, above its neighbours: its
                    // outline and pointer reach past its own edges.
                    .zIndex(if (resizing) 1f else 0f)
                    .offset { cellOffset.value }
                    .requiredSize(cellWidthDp * item.spanX, cellHeightDp * item.spanY)
                    .onGloballyPositioned { cellRoot = it.positionInRoot() }
                    .then(
                        // One that arrives by being let go here already has its landing.
                        if ((animateEntrance || item.id !in firstIds) && !arrivedByDrop) {
                            Modifier.appearIn(delayMillis = staggerDelay(index))
                        } else {
                            Modifier
                        }
                    )
                    .graphicsLayer {
                        // The real item stays put, in full colour, until you let go — it is the
                        // grey copy under your finger that moves. An app that has just been closed
                        // lands back into its place, super icons included.
                        val landing = if (item is AppCell) AppLanding.scaleFor(item.key) else 1f
                        scaleX = scale * landing
                        scaleY = scale * landing
                        alpha = wayAlpha * (if (item is AppCell) AppLanding.alphaFor(item.key) else 1f)
                        // Parallax: while pages slide, items drift apart from the middle of the page
                        // and settle back together as it lands, so the page has depth.
                        translationX = pageOffset * cellWidthPx * PAGE_PARALLAX *
                            (item.col + item.spanX / 2f - settings.columns / 2f)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { contentRoot = it.positionInRoot() }
                        .then(if (item is AppCell) Modifier.launchOrigin(item.key) else Modifier)
                        .pointerInput(item.id, cellWidthPx, cellHeightPx, settings.columns, resizing) {
                            // While its size is being pulled, the item stays put: a finger resting
                            // on the pointer must never turn into a long press that carries it off.
                            if (resizing) return@pointerInput
                            detectDragGesturesAfterLongPress(
                                onDragStart = { local ->
                                    travelled = 0f
                                    longPressHandled = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    drag.begin(liveItem, contentRoot + local, cellRoot)
                                },
                                onDrag = { change, delta ->
                                    change.consume()
                                    travelled += delta.getDistance()
                                    drag.pointer += delta
                                    drag.hoverCandidateId = hoverTarget(drag, liveItems, liveItem)
                                },
                                onDragEnd = {
                                    if (travelled < 24f) {
                                        drag.clear()
                                        onItemMenu(liveItem)
                                    } else {
                                        onDrop()
                                    }
                                },
                                onDragCancel = { drag.clear() },
                            )
                        }
                        .pointerInput(item.id, resizing) {
                            detectTapGestures(
                                onPress = {
                                    longPressHandled = false
                                    tryAwaitRelease()
                                },
                                onTap = {
                                    if (longPressHandled) {
                                        longPressHandled = false
                                        return@detectTapGestures
                                    }
                                    when {
                                        resizing -> onResizeDone()
                                        item is AppCell -> onOpenApp(item.key)
                                        item is FolderCell -> onOpenFolder(item)
                                        // Tapping the clock opens the bedside screen; long press
                                        // still belongs to the drag and menu detector.
                                        item is WidgetCell && Widgets.baseType(item.type) == "clock" -> onAmbient()
                                        else -> Unit
                                    }
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    HomeItemContent(
                        item = item,
                        settings = settings,
                        appsByKey = appsByKey,
                        geometry = geometry,
                        onSearch = onSearch,
                        onNoteChange = onNoteChange,
                        onAmbient = onAmbient,
                    )
                }

                // As a sibling on top, so a finger on the pointer is never also a press on the item.
                if (resizing && (item is WidgetCell || item is AppCell)) {
                    ResizeFrame(
                        item = item,
                        cellWidthPx = cellWidthPx,
                        cellHeightPx = cellHeightPx,
                        geometry = geometry,
                        free = freePlacement,
                        columns = settings.columns,
                        unitRows = settings.rows * ROW_UNITS,
                        onResize = onResize,
                        onDone = onResizeDone,
                    )
                }
            }
            }
        }
        }
    }
}

/**
 * The icon or folder the carried icon would make a folder with: the one whose middle it is held
 * over, within about half an icon. Held anywhere else over an icon, it takes that icon's place and
 * the icon steps aside, as on a phone.
 */
private fun hoverTarget(drag: HomeDragState, items: List<HomeItem>, dragged: HomeItem): String? {
    if (dragged !is AppCell || !dragged.isPlainIcon) return null
    if (drag.cellWidth <= 0f || drag.cellHeight <= 0f) return null
    val centreX = drag.ghostOnGrid.x + dragged.spanX * drag.cellWidth / 2f
    val centreY = drag.ghostOnGrid.y + dragged.spanY * drag.cellHeight / 2f
    val reach = drag.iconSizePx * FOLDER_REACH
    return items.firstOrNull {
        it.id != dragged.id && ((it is AppCell && it.isPlainIcon) || it is FolderCell) &&
            abs((it.col + it.dx + it.spanX / 2f) * drag.cellWidth - centreX) < reach &&
            abs((it.row + it.dy + it.spanY / 2f) * drag.cellHeight - centreY) < reach
    }?.id
}

/** Whether [other], moved to [spot], would cover any of the cells [item] stands on. */
private fun covers(other: HomeItem, spot: DropPlanner.Spot, item: HomeItem): Boolean =
    spot.col < item.col + item.spanX && item.col < spot.col + other.spanX &&
        spot.row < item.row + item.spanY && item.row < spot.row + other.spanY

/** How near the middle of an icon, in icon widths, the carried icon has to be to make a folder. */
private const val FOLDER_REACH = 0.45f

@Composable
fun HomeItemContent(
    item: HomeItem,
    settings: ZenithSettings,
    appsByKey: Map<String, AppInfo>,
    /** The page it is on: icons, super icons and widgets are all cut to its lines. */
    geometry: HomeGeometry,
    onSearch: () -> Unit,
    onNoteChange: (String) -> Unit,
    onAmbient: () -> Unit = {},
) {
    val iconSize = geometry.iconDp.dp
    when (item) {
        is AppCell -> {
            val app = appsByKey[item.key]
            val label = app?.label ?: item.packageName.substringAfterLast('.')
            when {
                !item.isPlainIcon ->
                    SuperAppTile(
                        key = item.key,
                        label = label,
                        frame = geometry.superIconFrame(item.spanX, (item.spanY / ROW_UNITS).coerceAtLeast(1)),
                        showLabel = settings.showLabels,
                    )
                else -> AppTile(
                    key = item.key,
                    label = label,
                    iconSize = iconSize,
                    showLabel = settings.showLabels,
                    shape = ZenithShapes.forIconShape(settings.iconShape),
                    frame = geometry.iconFrame,
                )
            }
        }

        is FolderCell -> FolderTile(
            folder = item,
            appsByKey = appsByKey,
            iconSize = iconSize,
            showLabel = settings.showLabels,
            frame = geometry.iconFrame,
        )

        is WidgetCell -> ZenithWidget(
            type = item.type,
            userName = settings.userName,
            note = settings.note,
            toggles = settings.toggles,
            onNoteChange = onNoteChange,
            onSearch = onSearch,
            onAmbient = onAmbient,
            modifier = Modifier
                .fillMaxSize()
                .pixelFrame(geometry.widgetFrame(item.spanX, item.spanY)),
        )
    }
}

/**
 * A super icon, as on HyperOS: the app's icon stretched over several columns and rows, its
 * background filling the whole shape and its logo in the middle, with the name underneath like any
 * other icon. [frame] is where the picture goes inside its cells (see [HomeGeometry.superIconFrame]),
 * which puts it on the same lines as every icon around it.
 */
@Composable
private fun SuperAppTile(key: String, label: String, frame: PxRect, showLabel: Boolean) {
    val palette = LocalPalette.current
    val density = LocalDensity.current
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(with(density) { frame.top.toDp() }))
        WideAppIcon(
            key = key,
            label = label,
            modifier = Modifier
                .padding(start = with(density) { frame.left.toDp() })
                .size(with(density) { frame.width.toDp() }, with(density) { frame.height.toDp() })
                .launchOrigin(key)
                .pressBounce(0.9f),
        )
        if (showLabel) {
            Box(Modifier.fillMaxWidth().height(LABEL_LINE_HEIGHT), contentAlignment = Alignment.BottomCenter) {
                Text(
                    text = label,
                    color = palette.onGlass,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/** The size icons are drawn at away from the grid: the drawer, folders, the dock's fallback. */
val STANDARD_ICON_SIZE = 60.dp

/**
 * Draws content at exactly [frame], in whole pixels, inside the space this is given. The space
 * itself is taken whole, so the parent and anything beside it see no difference.
 */
fun Modifier.pixelFrame(frame: PxRect): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(Constraints.fixed(frame.width.coerceAtLeast(0), frame.height.coerceAtLeast(0)))
    layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(frame.left, frame.top) }
}

@Composable
fun AppTile(
    key: String,
    label: String,
    iconSize: androidx.compose.ui.unit.Dp,
    showLabel: Boolean,
    shape: androidx.compose.ui.graphics.Shape = ZenithShapes.icon,
    modifier: Modifier = Modifier,
    /** On a home page: exactly where the icon goes inside its cell. Elsewhere it is centred. */
    frame: PxRect? = null,
) {
    val palette = LocalPalette.current
    val density = LocalDensity.current
    Column(
        modifier = if (frame == null) modifier.padding(horizontal = 2.dp) else modifier.fillMaxSize(),
        horizontalAlignment = if (frame == null) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = if (frame == null) Arrangement.Center else Arrangement.Top,
    ) {
        if (frame != null) Spacer(Modifier.height(with(density) { frame.top.toDp() }))
        AppIcon(
            key = key,
            label = label,
            size = iconSize,
            shape = shape,
            modifier = Modifier
                .then(if (frame != null) Modifier.padding(start = with(density) { frame.left.toDp() }) else Modifier)
                .launchOrigin(key)
                .pressBounce(),
        )
        if (showLabel) {
            // Always exactly LABEL_LINE_HEIGHT tall, so every icon's top edge sits on one line and
            // widgets beside it can be cut to the bottom of this strip.
            Box(
                (if (frame != null) Modifier.fillMaxWidth().padding(horizontal = 2.dp) else Modifier).height(LABEL_LINE_HEIGHT),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Text(
                    text = label,
                    color = palette.onGlass,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.graphicsLayer { shadowElevation = 0f },
                )
            }
        }
    }
}

@Composable
fun FolderTile(
    folder: FolderCell,
    appsByKey: Map<String, AppInfo>,
    iconSize: androidx.compose.ui.unit.Dp,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
    /** On a home page: exactly where the folder goes inside its cell. Elsewhere it is centred. */
    frame: PxRect? = null,
) {
    val palette = LocalPalette.current
    val density = LocalDensity.current
    Column(
        modifier = if (frame == null) modifier else modifier.fillMaxSize(),
        horizontalAlignment = if (frame == null) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = if (frame == null) Arrangement.Center else Arrangement.Top,
    ) {
        if (frame != null) Spacer(Modifier.height(with(density) { frame.top.toDp() }))
        Box(
            Modifier
                .then(if (frame != null) Modifier.padding(start = with(density) { frame.left.toDp() }) else Modifier)
                .size(iconSize)
                .pressBounce()
                .glass(ZenithShapes.icon, strength = 1.2f, material = GlassMaterial.Clear, interactive = true)
                .padding(iconSize * 0.12f),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(iconSize * 0.06f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                folder.keys.take(4).chunked(2).forEach { rowKeys ->
                    Row(horizontalArrangement = Arrangement.spacedBy(iconSize * 0.06f)) {
                        rowKeys.forEach { key ->
                            AppIcon(
                                key = key,
                                label = appsByKey[key]?.label ?: "",
                                size = iconSize * 0.33f,
                            )
                        }
                        if (rowKeys.size == 1) Spacer(Modifier.size(iconSize * 0.33f))
                    }
                }
            }
        }
        if (showLabel) {
            Box(
                (if (frame != null) Modifier.fillMaxWidth() else Modifier).height(LABEL_LINE_HEIGHT),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Text(
                    text = folder.title,
                    color = palette.onGlass,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/**
 * Resizing, as simple as pulling a corner. A thin outline hugs the item and a pointer sits on its
 * bottom-right corner; pull the pointer and the outline follows your finger exactly, while the item
 * underneath snaps to whole cells as you pass them, with a light tick, and a badge by the pointer
 * says the size. Let go and the outline settles onto the size the item took. A size with no room
 * turns the outline red rather than pushing anything out of the way.
 *
 * Apps and widgets can both be pulled out to any size, up to the whole page; a widget only stops at
 * the smallest its design allows. Apps grow in whole rows.
 */
@Composable
private fun BoxScope.ResizeFrame(
    item: HomeItem,
    cellWidthPx: Float,
    cellHeightPx: Float,
    geometry: HomeGeometry,
    /** Plus free placement: a widget may be any number of row units tall. */
    free: Boolean,
    columns: Int,
    unitRows: Int,
    onResize: (HomeItem, Int, Int) -> Boolean,
    /** A tap on the pointer, rather than a pull, finishes resizing like a tap anywhere else. */
    onDone: () -> Unit,
) {
    val palette = LocalPalette.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val finish by rememberUpdatedState(onDone)
    // The spans are read live: the gesture below outlives every size it passes through.
    val live by rememberUpdatedState(item)
    val isApp = item is AppCell
    val side = geometry.sideInset.toFloat()
    val sideEnd = geometry.sideInsetEnd.toFloat()
    val topInset = geometry.topInset.toFloat()
    val underInset = geometry.underInset.toFloat()
    val pageRows = unitRows / ROW_UNITS

    // Where an app's picture starts and ends for each number of rows it could span, from the same
    // frame the icon is drawn in (see HomeGeometry.superIconFrame); index 0 is unused.
    val appTop = FloatArray(pageRows + 1)
    val appBottom = FloatArray(pageRows + 1)
    for (rows in 1..pageRows) {
        val frame = geometry.superIconFrame(1, rows)
        appTop[rows] = frame.top.toFloat()
        appBottom[rows] = frame.bottom.toFloat()
    }
    fun rowsOf(spanY: Int) = (spanY / ROW_UNITS).coerceIn(1, pageRows.coerceAtLeast(1))

    // The outline runs along the lines the item is drawn on, in the item's own pixels. Its right
    // and bottom edges are the ones you pull.
    fun top(spanY: Int) = when {
        isApp -> appTop[rowsOf(spanY)]
        spanY >= ROW_UNITS -> topInset
        else -> 0f
    }
    fun restingRight(spanX: Int) = spanX * cellWidthPx - sideEnd
    fun restingBottom(spanY: Int) = when {
        isApp -> appBottom[rowsOf(spanY)]
        spanY >= ROW_UNITS -> spanY * cellHeightPx - underInset
        else -> spanY * cellHeightPx
    }

    val right = remember { Animatable(restingRight(item.spanX)) }
    val bottom = remember { Animatable(restingBottom(item.spanY)) }
    /** A finger is on the pointer. */
    var held by remember { mutableStateOf(false) }
    /** ...and has moved far enough to be pulling it, not tapping it. */
    var pulling by remember { mutableStateOf(false) }
    var blocked by remember { mutableStateOf(false) }
    val settle = spring<Float>(dampingRatio = 0.72f, stiffness = 420f)
    // Whenever nobody is pulling — after you let go, or when the size changes some other way —
    // the outline glides onto the item.
    LaunchedEffect(item.spanX, item.spanY, pulling, cellWidthPx, cellHeightPx) {
        if (!pulling) {
            blocked = false
            launch { right.animateTo(restingRight(item.spanX), settle) }
            launch { bottom.animateTo(restingBottom(item.spanY), settle) }
        }
    }

    /** The size the outline is asking for, in cells across and row units down. */
    fun sizeFor(r: Float, b: Float): Pair<Int, Int> {
        val spanX = ((r + sideEnd) / cellWidthPx).roundToInt().coerceIn(1, columns - live.col)
        val room = ((unitRows - live.row) / ROW_UNITS).coerceIn(1, pageRows.coerceAtLeast(1))
        return when {
            // Whole rows: whichever height's picture ends nearest the finger.
            isApp -> spanX to (((1..room).minByOrNull { abs(appBottom[it] - b) } ?: 1) * ROW_UNITS)
            free -> spanX to ((b + underInset) / cellHeightPx).roundToInt().coerceIn(1, unitRows - live.row)
            else -> spanX to (((1..room).minByOrNull { abs(restingBottom(it * ROW_UNITS) - b) } ?: 1) * ROW_UNITS)
        }
    }

    // The top edge only moves when an app changes how many rows it spans; it glides there too.
    val frameTop by animateFloatAsState(top(item.spanY), settle, label = "frameTop")
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f)) }
    val lift by animateFloatAsState(if (held) 1.15f else 1f, spring(dampingRatio = 0.5f, stiffness = 500f), label = "pointerLift")
    val outline by animateColorAsState(if (blocked) Color(0xFFFF5A5F) else Color.White, tween(160), label = "outline")
    val grow = 4.dp

    /**
     * Follows the item's own rounding: a widget's card, or for an app a quarter of its short side,
     * as its stretched icon is drawn — so it rounds more the bigger the icon is pulled.
     */
    fun Density.radius(): Float =
        if (isApp) 0.26f * minOf(right.value - side, bottom.value - frameTop).coerceAtLeast(0f) else 24.dp.toPx()

    /** Where the rounded corner of the outline comes closest to the corner of its box. */
    fun Density.tip(): Offset {
        val round = radius() + grow.toPx()
        val inset = round * (1f - 0.7071f) - grow.toPx()
        return Offset(right.value - inset, bottom.value - inset)
    }

    Box(
        Modifier
            .matchParentSize()
            // While it is being resized the item is not itself: a tap on it finishes, rather than
            // typing in a note or flipping a toggle.
            .pointerInput(Unit) { detectTapGestures(onTap = { finish() }) },
    ) {
        // The outline, with a soft glow of the accent round it.
        Canvas(Modifier.matchParentSize()) {
            val out = grow.toPx()
            val frame = Rect(side - out, frameTop - out, right.value + out, bottom.value + out)
            val radius = CornerRadius(radius() + out)
            val fade = appear.value.coerceIn(0f, 1f)
            drawRoundRect(
                color = palette.accent.copy(alpha = 0.28f * fade),
                topLeft = frame.topLeft, size = frame.size, cornerRadius = radius,
                style = Stroke(width = 7.dp.toPx()),
            )
            drawRoundRect(
                color = outline.copy(alpha = 0.95f * fade),
                topLeft = frame.topLeft, size = frame.size, cornerRadius = radius,
                style = Stroke(width = 2.dp.toPx()),
            )
        }

        // The pointer: its tip rests on the outline's corner.
        val pointerSize = 30.dp
        Canvas(
            Modifier
                .offset {
                    val tip = tip()
                    val reach = pointerSize.toPx() * POINTER_TIP
                    IntOffset((tip.x - reach).roundToInt(), (tip.y - reach).roundToInt())
                }
                .size(pointerSize)
                .graphicsLayer {
                    val scale = appear.value * lift
                    scaleX = scale
                    scaleY = scale
                    // Grows from its tip, so it never slides off the corner.
                    transformOrigin = TransformOrigin(POINTER_TIP, POINTER_TIP)
                },
        ) { drawPointer(palette.accent) }

        // What your finger takes hold of: a generous square over the pointer and the corner.
        val touch = 56.dp
        Box(
            Modifier
                .offset {
                    val tip = tip()
                    val half = touch.toPx() / 2f
                    // Centred a little inside the corner, over the body of the pointer.
                    val centre = tip - Offset(6.dp.toPx(), 6.dp.toPx())
                    IntOffset((centre.x - half).roundToInt(), (centre.y - half).roundToInt())
                }
                .size(touch)
                .pointerInput(item.id, cellWidthPx, cellHeightPx, columns, unitRows) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // Taken from the start, so the press is neither a tap on the item nor the
                        // beginning of a drag of the whole page.
                        down.consume()
                        held = true
                        val startRight = right.value
                        val startBottom = bottom.value
                        var pull = Offset.Zero
                        var lifted = false
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                lifted = true
                                break
                            }
                            pull += change.positionChange()
                            change.consume()
                            // A finger that has not really moved yet is still a tap.
                            if (!pulling) {
                                if (pull.getDistance() < viewConfiguration.touchSlop) continue
                                pulling = true
                            }
                            // No further than the page's own right and bottom edges.
                            val maxSpanX = (columns - live.col).coerceAtLeast(1)
                            val maxSpanY = (unitRows - live.row).coerceAtLeast(1)
                            val r = (startRight + pull.x).coerceIn(restingRight(1), restingRight(maxSpanX))
                            val lowest = restingBottom(if (isApp) ROW_UNITS else minOf(ROW_UNITS, live.spanY))
                            val b = (startBottom + pull.y).coerceIn(lowest, restingBottom(maxSpanY).coerceAtLeast(lowest))
                            scope.launch {
                                right.snapTo(r)
                                bottom.snapTo(b)
                            }
                            val (spanX, spanY) = sizeFor(r, b)
                            blocked = if (spanX == live.spanX && spanY == live.spanY) {
                                false
                            } else if (onResize(live, spanX, spanY)) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                false
                            } else {
                                true
                            }
                        }
                        held = false
                        if (pulling) pulling = false else if (lifted) finish()
                    }
                },
        )

        // The size, just above your finger while you pull.
        if (pulling) {
            Text(
                text = sizeLabel(live),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .offset {
                        // High enough to clear your fingertip and the top of an app icon.
                        IntOffset(
                            (right.value - 64.dp.toPx()).roundToInt(),
                            (bottom.value - 112.dp.toPx()).roundToInt(),
                        )
                    }
                    .appearIn(fromScale = 0.8f, riseFrom = 10f)
                    .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(percent = 50))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/** "3 × 1", "4 × 1½": columns by rows, quarter rows shown as fractions. */
private fun sizeLabel(item: HomeItem): String {
    val whole = item.spanY / ROW_UNITS
    val part = when (item.spanY % ROW_UNITS) {
        1 -> "¼"
        2 -> "½"
        3 -> "¾"
        else -> ""
    }
    val rows = if (whole == 0 && part.isNotEmpty()) part else "$whole$part"
    return "${item.spanX} × $rows"
}

/**
 * Where the pointer's tip lands in its square, across and down, once it is turned to aim at the
 * corner: drawn pointing up with its tip 0.46 of the way from the middle, then turned 135°.
 */
private const val POINTER_TIP = 0.5f + 0.46f * 0.7071f

/**
 * The resize pointer: a rounded arrowhead aimed at the bottom-right corner, white with a dark rim
 * and a soft shadow so it reads on any wallpaper, and a touch of the accent at its heart.
 */
private fun DrawScope.drawPointer(accent: Color) {
    val w = size.width
    // Drawn pointing straight up in a unit square, then turned to aim down and to the right.
    val shape = Path().apply {
        moveTo(0.5f * w, 0.04f * w)
        lineTo(0.86f * w, 0.9f * w)
        quadraticTo(0.5f * w, 0.7f * w, 0.14f * w, 0.9f * w)
        close()
    }
    val pivot = Offset(w / 2f, w / 2f)
    // Its shadow falls straight down, whichever way the pointer faces.
    translate(top = 1.5.dp.toPx()) {
        rotate(135f, pivot) { drawPath(shape, Color.Black.copy(alpha = 0.28f)) }
    }
    rotate(135f, pivot) {
        drawPath(shape, Color.White)
        drawPath(
            shape,
            Brush.verticalGradient(listOf(accent.copy(alpha = 0f), accent.copy(alpha = 0.35f)), startY = 0.3f * w, endY = 0.9f * w),
        )
        drawPath(shape, Color.Black.copy(alpha = 0.45f), style = Stroke(width = 1.2.dp.toPx(), join = StrokeJoin.Round))
    }
}

/** The ghost that follows your finger while dragging. */
@Composable
fun DragGhost(
    drag: HomeDragState,
    settings: ZenithSettings,
    appsByKey: Map<String, AppInfo>,
) {
    val item = drag.item ?: return
    val geometry = drag.geometry ?: return
    val density = LocalDensity.current
    val width = with(density) { (drag.cellWidth * item.spanX).toDp() }
    val height = with(density) { (drag.cellHeight * item.spanY).toDp() }
    val topLeft = drag.pointer - drag.grab
    val lift = remember { Animatable(1f) }
    LaunchedEffect(item.id) {
        val target = if (item is WidgetCell || (item is AppCell && !item.isPlainIcon)) 1.02f else 1.14f
        lift.animateTo(target, spring(dampingRatio = 0.38f, stiffness = 260f))
    }
    val ghostAlpha by animateFloatAsState(if (drag.overRemove) 0.32f else 0.62f, Motion.smooth(), label = "ghost")
    // Colour is drained out of the copy under your finger so it reads as a placeholder.
    val greyPaint = remember {
        Paint().apply {
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        }
    }

    Box(
        Modifier
            .offset { IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()) }
            .requiredSize(width, height)
            .graphicsLayer {
                scaleX = lift.value
                scaleY = lift.value
                alpha = ghostAlpha
            }
            .drawWithContent {
                drawIntoCanvas { canvas ->
                    canvas.saveLayer(Rect(Offset.Zero, size), greyPaint)
                    drawContent()
                    canvas.restore()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        HomeItemContent(
            item = item,
            settings = settings,
            appsByKey = appsByKey,
            geometry = geometry,
            onSearch = {},
            onNoteChange = {},
        )
    }
}

/** Breathing room either side of a dock icon before the bar counts as over-wide. */
private val DOCK_ICON_GAP = 36.dp

/**
 * The dock. Apps arrive by being dragged onto it from the home screen, and once there they are
 * rearranged the same way: hold one, slide it along and the others make room; lift it up off the
 * bar and it goes back to the home screen. Holding without moving still opens the app's menu.
 */
@Composable
fun DockBar(
    keys: List<String>,
    appsByKey: Map<String, AppInfo>,
    settings: ZenithSettings,
    /** Something from the home screen is being carried over the dock right now. */
    highlight: Boolean,
    onOpen: (String) -> Unit,
    onLongPress: (String) -> Unit,
    onReorder: (from: Int, to: Int) -> Unit,
    /** An app was lifted off the dock and let go, the middle of its icon at this point on screen. */
    onDragOut: (key: String, iconCentre: Offset) -> Unit,
    onBounds: (Rect) -> Unit,
    animateEntrance: Boolean = false,
    /** The page grid's column count; with as many dock apps, each stands under its column. */
    columns: Int = 0,
    /** The icon size the page settled on, so dock icons match the icons above them. */
    pageIconSize: Dp? = null,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val haptics = LocalHapticFeedback.current
    val firstKeys = remember { keys.toSet() }
    val density = LocalDensity.current
    val iconSize = pageIconSize ?: STANDARD_ICON_SIZE
    val iconSizePx = with(density) { iconSize.toPx() }
    // One dock app per column: then each is centred on its column, exactly under the page's icons.
    val aligned = columns > 0 && keys.size == columns
    // A dock is a row of icons, not a rail. Past the width its icons actually need, SpaceEvenly
    // stops being spacing and starts being distance, so on a tablet the bar centres itself instead
    // of flinging four icons into the corners.
    val widest = if (keys.isEmpty() || aligned) Dp.Infinity else (iconSize + DOCK_ICON_GAP) * keys.size + 20.dp

    // Where each icon sits at rest, measured only while nothing is moving so a dragged icon's own
    // offset never feeds back into the maths.
    val slots = remember { androidx.compose.runtime.mutableStateMapOf<Int, Rect>() }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var grabRoot by remember { mutableStateOf(Offset.Zero) }
    var suppressTap by remember { mutableStateOf(false) }

    // The gesture below is set up once and outlives recompositions, so everything it asks at the
    // moment of release is read from live state through these, never from values captured then.
    val currentKeys by rememberUpdatedState(keys)
    val currentOnReorder by rememberUpdatedState(onReorder)
    val currentOnDragOut by rememberUpdatedState(onDragOut)
    val currentOnLongPress by rememberUpdatedState(onLongPress)

    /** Carried far enough up to leave the dock. */
    fun liftedNow(): Boolean = draggingIndex != null && dragOffset.y < -iconSizePx * 1.2f

    /** The index the carried icon would take if let go now. */
    fun targetNow(): Int? {
        val carriedFrom = draggingIndex ?: return null
        if (liftedNow()) return carriedFrom
        val carried = (slots[carriedFrom]?.center?.x ?: 0f) + dragOffset.x
        return currentKeys.indices.count { it != carriedFrom && (slots[it]?.center?.x ?: 0f) < carried }
    }

    val from = draggingIndex
    val spacing = if (keys.size > 1) {
        val a = slots[0]
        val b = slots[1]
        if (a != null && b != null) b.center.x - a.center.x else iconSizePx
    } else {
        iconSizePx
    }
    val lifted = liftedNow()
    val target = targetNow()

    val rim by animateFloatAsState(if (highlight) 1f else 0f, tween(220), label = "dockRim")

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .widthIn(max = widest)
                .fillMaxWidth()
                .onGloballyPositioned { onBounds(it.boundsInRoot()) }
                .graphicsLayer {
                    val grow = 1f + 0.03f * rim
                    scaleX = grow
                    scaleY = grow
                }
                .glass(ZenithShapes.panel, strength = 1.15f, material = GlassMaterial.Clear, interactive = true)
                .then(
                    if (rim > 0.01f) {
                        Modifier.border(2.dp, palette.accent.copy(alpha = 0.85f * rim), ZenithShapes.panel)
                    } else {
                        Modifier
                    }
                )
                .padding(vertical = 12.dp, horizontal = if (aligned) 0.dp else 10.dp),
            horizontalArrangement = if (keys.isEmpty()) Arrangement.Center else Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (keys.isEmpty()) {
                Text(
                    text = if (highlight) "Let go to add it" else "Drag an app here to add it to the dock",
                    color = palette.onGlassMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 10.dp),
                )
            }
            keys.forEachIndexed { index, key ->
                val isCarried = index == from
                // Neighbours step aside to show where the carried icon will land.
                val shift = when {
                    from == null || target == null || isCarried -> 0f
                    from < target && index in (from + 1)..target -> -spacing
                    from > target && index in target until from -> spacing
                    else -> 0f
                }
                val shiftAnimated by animateFloatAsState(shift, spring(dampingRatio = 0.75f, stiffness = 400f), label = "dockShift")

                Box(
                    Modifier
                        .then(if (aligned) Modifier.weight(1f) else Modifier)
                        .then(
                            if (animateEntrance || key !in firstKeys) {
                                Modifier.appearIn(delayMillis = 90 + staggerDelay(index), riseFrom = 26f)
                            } else {
                                Modifier
                            }
                        )
                        .zIndex(if (isCarried) 1f else 0f)
                        .onGloballyPositioned { coords ->
                            if (draggingIndex == null) slots[index] = coords.boundsInRoot()
                        }
                        .graphicsLayer {
                            if (isCarried) {
                                translationX = dragOffset.x
                                translationY = dragOffset.y
                                scaleX = 1.14f
                                scaleY = 1.14f
                            } else {
                                translationX = shiftAnimated
                            }
                        }
                        .launchOrigin(key)
                        .pointerInput(key) {
                            detectTapGestures(
                                onPress = { suppressTap = false },
                                onTap = { if (!suppressTap) onOpen(key) },
                            )
                        }
                        .pointerInput(key, index) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { local ->
                                    suppressTap = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    grabRoot = (slots[index]?.topLeft ?: Offset.Zero) + local
                                    dragOffset = Offset.Zero
                                    draggingIndex = index
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount
                                },
                                onDragEnd = {
                                    val moved = dragOffset.getDistance()
                                    val start = draggingIndex
                                    val end = targetNow()
                                    when {
                                        // Held still: that is a long press, so show the menu.
                                        moved < iconSizePx * 0.25f -> currentOnLongPress(key)
                                        liftedNow() -> currentOnDragOut(key, (slots[index]?.center ?: grabRoot) + dragOffset)
                                        start != null && end != null && end != start -> currentOnReorder(start, end)
                                    }
                                    draggingIndex = null
                                    dragOffset = Offset.Zero
                                },
                                onDragCancel = {
                                    draggingIndex = null
                                    dragOffset = Offset.Zero
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(
                        key = key,
                        label = appsByKey[key]?.label ?: "",
                        size = iconSize,
                        shape = ZenithShapes.forIconShape(settings.iconShape),
                        modifier = Modifier
                            .launchOrigin(key)
                            .pressBounce()
                            .graphicsLayer {
                                // A dock app that has just been closed lands back into its place.
                                val landing = AppLanding.scaleFor(key)
                                scaleX = landing
                                scaleY = landing
                                alpha = (if (isCarried && lifted) 0.75f else 1f) * AppLanding.alphaFor(key)
                            },
                    )
                }
            }
        }
    }
}

/** How far items spread apart, in cells per cell from the middle, as a page slides fully away. */
private const val PAGE_PARALLAX = 0.35f

/** Height of a single 11sp app label line, plus its spacer — the same strip [HomeGeometry] counts. */
private val LABEL_LINE_HEIGHT = HomeGeometry.LABEL_DP.dp

val TransparentColor: Color = Color.Transparent
