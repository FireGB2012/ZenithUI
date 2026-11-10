package com.gabriel.zenithui.ui.home

import androidx.compose.runtime.CompositionLocalProvider
import com.gabriel.zenithui.ui.common.WideAppIcon
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.zIndex
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.material3.LocalTextStyle
import com.gabriel.zenithui.data.AppCell
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.DropPlanner
import com.gabriel.zenithui.data.FolderCell
import com.gabriel.zenithui.data.HomeGeometry
import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.PxRect
import com.gabriel.zenithui.data.ROW_UNITS
import com.gabriel.zenithui.data.HostedWidgetCell
import com.gabriel.zenithui.data.ShortcutCell
import com.gabriel.zenithui.data.StackCell
import com.gabriel.zenithui.data.WidgetCell
import com.gabriel.zenithui.data.Widgets
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.AppIcon
import com.gabriel.zenithui.ui.common.IconBadge
import com.gabriel.zenithui.ui.common.IconLabel
import com.gabriel.zenithui.ui.common.LocalBadges
import com.gabriel.zenithui.ui.common.LocalLabelStyle
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.launchOrigin
import com.gabriel.zenithui.ui.common.labelShadow
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.theme.GlassMaterial
import com.gabriel.zenithui.ui.theme.IconOutlineShape
import com.gabriel.zenithui.ui.theme.LocalOnWallpaper
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.PressLight
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import androidx.compose.ui.unit.round
import com.gabriel.zenithui.ui.theme.withAccent
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

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
    /** The bin's own rectangle. The pointer has to be over it or straight above it, not merely
     *  above its bottom edge — widgets live at the top of the page, so a whole top strip cannot be
     *  a trapdoor. See [overRemove]. */
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

    /** The grey copy as it was let go into the bin, the dock or a folder, so it is seen going there. */
    var sent by mutableStateOf<Sent?>(null)

    val isDragging: Boolean get() = item != null

    /** Where the middle of [item]'s icon is on screen, on the page as it was last measured. */
    fun iconCentre(item: HomeItem): Offset? {
        val icon = geometry?.iconFrame ?: return null
        return gridOrigin + Offset(
            (item.col + item.dx) * cellWidth + (icon.left + icon.right) / 2f,
            (item.row + item.dy) * cellHeight + (icon.top + icon.bottom) / 2f,
        )
    }

    /**
     * Your finger is on the bin right now: across it, and above the bottom of both the bin and the
     * first row's icons. With nothing written above it the page starts just under the status bar,
     * so the bin's lower half lies over the first row's middle icons; carrying something to those
     * cells, or onto one of those icons to make a folder, must not arm it. The strip above the bin
     * counts, so it stays as easy to hit as it was.
     */
    val overRemove: Boolean
        get() = item != null && !removeZone.isEmpty &&
            pointer.x in removeZone.left..removeZone.right &&
            pointer.y < minOf(removeZone.bottom, gridOrigin.y + (geometry?.topInset ?: 0))

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

    /**
     * Icons gathered while one is carried: tapped with another finger, each flies into a stack
     * under the carried one, and they are put down round it together. Only plain app icons, and
     * only while a plain app icon is carried. Each comes with where it was gathered from, on screen.
     */
    val gathered = androidx.compose.runtime.mutableStateListOf<Pair<HomeItem, Offset>>()

    /** [other] tapped while carrying: into the stack, if it can be carried with what is carried. */
    fun gather(other: HomeItem, from: Offset): Boolean {
        val carrying = item as? AppCell ?: return false
        if (!carrying.isPlainIcon || other !is AppCell || !other.isPlainIcon || other.id == carrying.id) return false
        if (gathered.any { it.first.id == other.id } || gathered.size >= MOST_GATHERED) return false
        gathered += other to from
        return true
    }

    fun clear() {
        item = null
        gathered.clear()
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

/**
 * The parts of the home screen that are not on the grid — the line at the top, the dots, the
 * search bar, the dock — arriving with it: up or down from [from] and fading in, as late as
 * [distance] says (0 with the middle of the page, 1 with its corners).
 */
internal fun Modifier.arriving(clock: () -> Float, depth: () -> Float, distance: Float, from: Dp, style: String = "middle"): Modifier =
    graphicsLayer {
        val at = clock()
        if (at < FlyIn.TOTAL_MS) {
            val out = (1f - FlyIn.progress(at, FlyIn.delayMs(distance))) * depth()
            // From below everything rises, and in a cascade everything drops in; from the middle
            // each comes in from its own side.
            val reach = when (style) {
                "below" -> abs(from.toPx())
                "cascade" -> -abs(from.toPx())
                else -> from.toPx()
            }
            translationY = reach * out
            alpha = (1f - out * out).coerceIn(0f, 1f)
        }
    }

/** Things stepping aside on [page] for the carried item, as [plan] has them. */
@Immutable
data class DropPreview(val page: Int, val plan: DropPlanner.Plan)

/** Item [id] was just let go with its grey copy's corner at [ghostOnGrid], in the page's pixels. */
@Immutable
data class Landing(val id: String, val ghostOnGrid: Offset)

/**
 * The grey copy of [item] as it was let go, its corner at [topLeft] on screen, when what it was let
 * go into was not a spot on the page. [into] is the point it goes into, on screen: the bin, or the
 * icon it makes a folder with. With none, it sinks away where it was let go. Held over the bin it
 * was already [faint], and goes from there.
 */
@Immutable
data class Sent(val item: HomeItem, val topLeft: Offset, val into: Offset?, val faint: Boolean = false)

/** How solid the grey copy under a finger is, and how solid while it is held over the bin. */
private const val COPY_ALPHA = 0.62f
private const val COPY_ALPHA_OVER_BIN = 0.32f

/** How much bigger than itself something is while it is carried: an icon a good deal, anything larger only a little. */
private fun carriedScale(item: HomeItem): Float =
    if (item.isWidgetLike || (item is AppCell && !item.isPlainIcon)) 1.02f else 1.14f

/**
 * What has just left a page: thrown away, put in the dock, made into a folder, taken off from its
 * own menu. Each is kept just long enough to be seen going, where it stood. Without this an item
 * taken off the page is there in one frame and not in the next.
 */
@Stable
private class Departures(first: List<HomeItem>) {
    private var before = first
    private val going = ArrayList<HomeItem>()

    /** Goes up when one has finished going, so the page is composed again without it. */
    private var finished by mutableIntStateOf(0)

    /** Told what is on the page [now], says what is on its way off it. */
    fun after(now: List<HomeItem>): List<HomeItem> {
        finished
        if (now != before) {
            val here = now.mapTo(HashSet()) { it.id }
            for (old in before) if (old.id !in here && going.none { it.id == old.id }) going += old
            // Back before it had finished going: it is simply there again.
            going.removeAll { it.id in here }
            before = now
        }
        return if (going.isEmpty()) emptyList() else going.toList()
    }

    fun gone(item: HomeItem) {
        if (going.remove(item)) finished++
    }
}

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
    /** Another icon has just joined the one being carried. */
    onGathered: () -> Unit = {},
    onOpenApp: (String) -> Unit,
    /** A folder was tapped: which one, and where its icon is on screen, for it to open out of. */
    onOpenFolder: (FolderCell, Rect) -> Unit,
    onItemMenu: (HomeItem) -> Unit,
    onSearch: () -> Unit,
    onNoteChange: (String) -> Unit,
    onAmbient: () -> Unit,
    onDrop: () -> Unit,
    /** Another app's widget that did not come through a restore, to be put back: the item, and what it is. */
    onRestoreWidget: (HomeItem, android.appwidget.AppWidgetProviderInfo) -> Unit = { _, _ -> },
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
    /** The arrival clock, in milliseconds; at [FlyIn.TOTAL_MS] or beyond everything is in its place. */
    arrivalClock: () -> Float = { FlyIn.TOTAL_MS.toFloat() },
    /** How far out the arrival starts: [FlyIn.FULL] for the fly-in, [FlyIn.SETTLE] for coming back from an app. */
    arrivalDepth: () -> Float = { 0f },
    /** Seconds on the clock every item on the page jiggles to, while something is being carried. */
    jiggleClock: () -> Float = { 0f },
    /** How hard: 0 at rest, 1 while something is in the air. It eases in and out. */
    jiggleStrength: () -> Float = { 0f },
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val density = LocalDensity.current
    val reduced = LocalMotionLevel.current.reduced
    // Items already on the page when it first appears never animate in; ones added later do.
    val firstIds = remember { items.map { it.id }.toSet() }
    // And ones taken off it are seen to go.
    val departures = remember { Departures(items) }
    val haptics = LocalHapticFeedback.current

    // Everything on the page stands on the wallpaper, so its names are white, in Light as in Dark.
    OnWallpaper {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val geometry = remember(constraints.maxWidth, constraints.maxHeight, settings.columns, settings.rows, density.density, settings.showLabels, settings.gridDensity) {
            HomeGeometry(
                constraints.maxWidth, constraints.maxHeight, settings.columns, settings.rows, density.density, settings.showLabels,
                rowRatio = HomeGeometry.rowRatioFor(settings.gridDensity, settings.columns),
            )
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
                    } else if (dragged.isWidgetLike) {
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
            // How far this item's middle is from the middle of the page: it decides both where the
            // item flies in from and when it sets off, the middle of the page first.
            val fromCentreX = (item.col + item.dx + item.spanX / 2f) * cellWidthPx - geometry.gridWidth / 2f
            val fromCentreY = (item.row + item.dy + item.spanY / 2f) * cellHeightPx - geometry.gridHeight / 2f
            val flyIn = FlyIn.start(settings.arrival, fromCentreX, fromCentreY, geometry.gridWidth.toFloat(), geometry.gridHeight.toFloat())
            // While something is carried everything else on the page rocks a little, each item out
            // of step with its neighbours and at its own pace. A wide widget rocks less: the same
            // angle would swing its corners much further than an icon's.
            val jigglePhase = remember(item.id) { (item.id.hashCode() and 0xFF) / 255f }
            val jiggleAngle = if (item.spanX > 1 || item.spanY > ROW_UNITS) 0.45f else 1.2f
            val carried = item.id == dragged?.id
            val gatheredHere = drag.gathered.any { it.first.id == item.id }
            val gatheredAlpha by animateFloatAsState(if (gatheredHere) 0.3f else 1f, Motion.smooth(), label = "gathered")
            // Put down, it comes back to its own size from the size it was carried at, with one
            // small bounce: the copy under your finger and the thing itself are the same thing.
            val lifted = carriedScale(item)
            val settle = remember { Animatable(if (landing != null && !reduced) lifted else 1f) }
            LaunchedEffect(landing) {
                if (landing != null && !reduced) {
                    settle.snapTo(lifted)
                    settle.animateTo(1f, Motion.drop())
                }
            }
            // And it settles with a ring that spreads from it and fades: white, as it is drawn on
            // the wallpaper.
            val ring = remember { Animatable(1f) }
            LaunchedEffect(landing) {
                if (landing != null) {
                    ring.snapTo(0f)
                    ring.animateTo(1f, tween(460, easing = LinearOutSlowInEasing))
                } else {
                    // Another item landed before this ring had finished: it must not be left half drawn.
                    ring.snapTo(1f)
                }
            }
            val ringFrame = when {
                item.isWidgetLike -> geometry.widgetFrame(item.spanX, item.spanY)
                item is AppCell && !item.isPlainIcon -> geometry.superIconFrame(item.spanX, (item.spanY / ROW_UNITS).coerceAtLeast(1))
                else -> geometry.iconFrame
            }
            val ringRound = item is FolderCell || item is ShortcutCell || (item is AppCell && item.isPlainIcon)
            val opens = item.launchKey
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
                        val landing = if (opens != null) AppLanding.scaleFor(opens) else 1f
                        scaleX = scale * landing * settle.value
                        scaleY = scale * landing * settle.value
                        alpha = wayAlpha * gatheredAlpha * (if (opens != null) AppLanding.alphaFor(opens) else 1f)
                        // Parallax: while pages slide, items drift apart from the middle of the page
                        // and settle back together as it lands, so the page has depth.
                        translationX = pageOffset * cellWidthPx * PAGE_PARALLAX *
                            (item.col + item.spanX / 2f - settings.columns / 2f)
                        // Arriving: out from the middle and a little small, then in to its place
                        // and a touch past it. Read here, while drawing, so the clock ticking
                        // recomposes nothing.
                        val clock = arrivalClock()
                        if (clock < FlyIn.TOTAL_MS) {
                            val out = (1f - FlyIn.progress(clock, flyIn.delayMs)) * arrivalDepth()
                            translationX += flyIn.x * out
                            translationY = flyIn.y * out
                            val shrink = 1f - 0.3f * out
                            scaleX *= shrink
                            scaleY *= shrink
                            alpha *= (1f - out * out).coerceIn(0f, 1f)
                        }
                        // Making way: pushed aside while an app opens beside it, knocked back as one
                        // lands, and home again. Worked out from where its middle is on the screen.
                        val aside = HomeRipple.offsetFor(
                            Offset(cellRoot.x + cellWidthPx * item.spanX / 2f, cellRoot.y + cellHeightPx * item.spanY / 2f)
                        )
                        translationX += aside.x
                        translationY += aside.y
                        val shake = jiggleStrength()
                        if (shake > 0f && !carried) {
                            rotationZ = jiggleAngle * shake *
                                sin((jiggleClock() * (2f + 0.7f * jigglePhase) + jigglePhase) * TWO_PI)
                        }
                    }
                    .drawBehind {
                        // Held over, a plate spreads out from under it: let go there, and that
                        // is the folder the two of them become.
                        val plate = ((scale - 1f) / 0.18f).coerceIn(0f, 1f)
                        if (plate > 0.01f) {
                            val icon = geometry.iconFrame
                            val out = 7.dp.toPx() * plate
                            val side = icon.width + 2f * out
                            drawRoundRect(
                                color = palette.onGlass,
                                topLeft = Offset(icon.left - out, icon.top - out),
                                size = Size(side, icon.height + 2f * out),
                                cornerRadius = CornerRadius(side * 0.3f, side * 0.3f),
                                alpha = 0.22f * plate,
                            )
                        }
                        val spread = ring.value
                        if (spread < 1f) {
                            val grow = 16.dp.toPx() * spread
                            val stroke = Stroke(3.dp.toPx() * (1f - spread) + 0.5.dp.toPx())
                            val fade = 0.6f * (1f - spread)
                            if (ringRound) {
                                drawCircle(
                                    color = Color.White,
                                    radius = ringFrame.width / 2f + grow,
                                    center = Offset((ringFrame.left + ringFrame.right) / 2f, (ringFrame.top + ringFrame.bottom) / 2f),
                                    alpha = fade,
                                    style = stroke,
                                )
                            } else {
                                val corner = 26.dp.toPx() + grow
                                drawRoundRect(
                                    color = Color.White,
                                    topLeft = Offset(ringFrame.left - grow, ringFrame.top - grow),
                                    size = Size(ringFrame.width + 2f * grow, ringFrame.height + 2f * grow),
                                    cornerRadius = CornerRadius(corner, corner),
                                    alpha = fade,
                                    style = stroke,
                                )
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { contentRoot = it.positionInRoot() }
                        .then(if (opens != null) Modifier.launchOrigin(opens) else Modifier)
                        .pointerInput(item.id, cellWidthPx, cellHeightPx, settings.columns, resizing, settings.lockLayout) {
                            // While its size is being pulled, the item stays put: a finger resting
                            // on the pointer must never turn into a long press that carries it off.
                            if (resizing) return@pointerInput
                            // Another app's widget and a stack keep their own taps and swipes; only
                            // a hold that stays still is taken for a pick-up.
                            if (liveItem.hasOwnTouches) {
                                holdToCarry(
                                    onStart = { local ->
                                        longPressHandled = true
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        // Locked, a hold only ever opens the menu.
                                        if (!settings.lockLayout) drag.begin(liveItem, contentRoot + local, cellRoot)
                                    },
                                    onDrag = { delta ->
                                        if (!settings.lockLayout) {
                                            drag.pointer += delta
                                            drag.hoverCandidateId = hoverTarget(drag, liveItems, liveItem)
                                        }
                                    },
                                    onEnd = { moved ->
                                        if (settings.lockLayout || moved < 24f) {
                                            drag.clear()
                                            onItemMenu(liveItem)
                                        } else {
                                            onDrop()
                                        }
                                    },
                                    onCancel = { drag.clear() },
                                )
                                return@pointerInput
                            }
                            detectDragGesturesAfterLongPress(
                                onDragStart = { local ->
                                    travelled = 0f
                                    longPressHandled = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    // Locked, a hold only ever opens the menu.
                                    if (!settings.lockLayout) drag.begin(liveItem, contentRoot + local, cellRoot)
                                },
                                onDrag = { change, delta ->
                                    change.consume()
                                    if (!settings.lockLayout) {
                                        travelled += delta.getDistance()
                                        drag.pointer += delta
                                        drag.hoverCandidateId = hoverTarget(drag, liveItems, liveItem)
                                    }
                                },
                                onDragEnd = {
                                    if (settings.lockLayout || travelled < 24f) {
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
                            // Its taps are its own: the widget inside answers them.
                            if (liveItem.hasOwnTouches) return@pointerInput
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
                                    // Another icon tapped while one is carried joins it.
                                    if (drag.isDragging && drag.gather(item, cellRoot)) {
                                        onGathered()
                                        return@detectTapGestures
                                    }
                                    when {
                                        resizing -> onResizeDone()
                                        opens != null -> onOpenApp(opens)
                                        item is FolderCell -> {
                                            val icon = geometry.iconFrame
                                            onOpenFolder(
                                                item,
                                                Rect(
                                                    cellRoot.x + icon.left, cellRoot.y + icon.top,
                                                    cellRoot.x + icon.right, cellRoot.y + icon.bottom,
                                                ),
                                            )
                                        }
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
                        onRestoreWidget = onRestoreWidget,
                    )
                }

                // As a sibling on top, so a finger on the pointer is never also a press on the item.
                // It stays a moment after resizing is done, to be seen letting go of the item.
                val framed = resizing && (item.isWidgetLike || item is AppCell)
                var lingering by remember { mutableStateOf(false) }
                LaunchedEffect(framed) { if (framed) lingering = true }
                if (framed || lingering) {
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
                        leaving = !framed,
                        onGone = { lingering = false },
                    )
                }
            }
            }
        }

        // What has just been taken off the page shrinks away where it stood.
        departures.after(items).forEach { item ->
            key("gone", item.id) {
                val out = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    out.animateTo(1f, if (reduced) tween(Motion.REDUCED_MS) else tween(220, easing = FastOutLinearInEasing))
                    departures.gone(item)
                }
                Box(
                    Modifier
                        .offset {
                            IntOffset(
                                ((item.col + item.dx) * cellWidthPx).roundToInt(),
                                ((item.row + item.dy) * cellHeightPx).roundToInt(),
                            )
                        }
                        .requiredSize(cellWidthDp * item.spanX, cellHeightDp * item.spanY)
                        .graphicsLayer {
                            val gone = out.value
                            alpha = 1f - gone
                            if (!reduced) {
                                val shrunk = 1f - 0.35f * gone
                                scaleX = shrunk
                                scaleY = shrunk
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
                        live = false,
                    )
                    // A picture of what was there, not the thing: nothing in it takes a touch.
                    Box(Modifier.matchParentSize().pointerInput(Unit) { detectTapGestures { } })
                }
            }
        }
        }
    }
    }
}

/**
 * [content] written on the wallpaper itself, as everything on a home page is, and on the small home
 * screens that show one: whatever Light or Dark, its names are white with a soft shadow, as on an
 * iPhone, and its glass is the wallpaper's ([LocalOnWallpaper]), the same clear glass in both. The
 * drawer and the folder panel have surfaces of their own under their names, and keep the ink and the
 * glass of the mode.
 */
@Composable
internal fun OnWallpaper(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalLabelStyle provides LocalLabelStyle.current.copy(onWallpaper = true),
        LocalOnWallpaper provides true,
        content = content,
    )
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

/** Drawn in a widget's frame: one of Zenith's, another app's, or a stack of them. */
internal val HomeItem.isWidgetLike: Boolean
    get() = this is WidgetCell || this is HostedWidgetCell || this is StackCell

/**
 * Has touches of its own inside it, which must keep working: another app's widget, or a stack
 * that pages. Picked up with [holdToCarry], which watches without taking.
 */
private val HomeItem.hasOwnTouches: Boolean
    get() = this is HostedWidgetCell || this is StackCell

/** What tapping this item opens, by the key it opens through: an app or a shortcut. Null for the rest. */
private val HomeItem.launchKey: String?
    get() = when (this) {
        is AppCell -> key
        is ShortcutCell -> key
        else -> null
    }

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
    /**
     * False for a copy — the grey one under a finger, the small home screen in Settings — which
     * shows another app's widget by its picture: Android lets only one view draw a widget at once.
     */
    live: Boolean = true,
    onRestoreWidget: (HomeItem, android.appwidget.AppWidgetProviderInfo) -> Unit = { _, _ -> },
) {
    val iconSize = geometry.iconDp.dp
    when (item) {
        is HostedWidgetCell -> {
            HostedWidget(
                cell = item,
                live = live,
                onRestore = { onRestoreWidget(item, it) },
                modifier = Modifier
                    .fillMaxSize()
                    .pixelFrame(geometry.widgetFrame(item.spanX, item.spanY)),
            )
            // Another app's widget is named after the app it comes from.
            WidgetName(hostedName(item.provider, appsByKey), geometry.widgetNameStrip(item.spanX, item.spanY))
        }

        is StackCell -> {
            // A stack is named after whichever of its widgets is showing, as it settles on each.
            var showing by remember { mutableIntStateOf(0) }
            WidgetStack(
                stack = item,
                onShown = { showing = it },
                modifier = Modifier
                    .fillMaxSize()
                    .pixelFrame(geometry.widgetFrame(item.spanX, item.spanY)),
            ) { member ->
                if (member.hosted) {
                    HostedWidget(
                        cell = HostedWidgetCell(member.id, item.page, item.col, item.row, item.spanX, item.spanY, member.appWidgetId, member.provider),
                        live = live,
                        onRestore = { onRestoreWidget(item, it) },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    ZenithWidget(
                        type = member.type,
                        userName = settings.userName,
                        note = settings.note,
                        toggles = settings.toggles,
                        onNoteChange = onNoteChange,
                        onSearch = onSearch,
                        onAmbient = onAmbient,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            val member = item.members.getOrNull(showing) ?: item.members.firstOrNull()
            val name = when {
                member == null -> Widgets.UNNAMED
                member.hosted -> hostedName(member.provider, appsByKey)
                else -> Widgets.shortName(member.type)
            }
            WidgetName(name, geometry.widgetNameStrip(item.spanX, item.spanY))
        }

        is AppCell -> {
            val app = appsByKey[item.key]
            val label = app?.label ?: item.packageName.substringAfterLast('.')
            // An app whose name you hid shows none, whatever Labels says.
            val showLabel = settings.showLabels && settings.appEdits[item.key]?.hideLabel != true
            when {
                !item.isPlainIcon ->
                    SuperAppTile(
                        key = item.key,
                        label = label,
                        frame = geometry.superIconFrame(item.spanX, (item.spanY / ROW_UNITS).coerceAtLeast(1)),
                        showLabel = showLabel,
                    )
                else -> AppTile(
                    key = item.key,
                    label = label,
                    iconSize = iconSize,
                    showLabel = showLabel,
                    shape = ZenithShapes.forIconShape(settings.iconShape),
                    frame = geometry.iconFrame,
                    badge = true,
                )
            }
        }

        is ShortcutCell -> ShortcutTile(
            item = item,
            appsByKey = appsByKey,
            iconSize = iconSize,
            showLabel = settings.showLabels,
            shape = ZenithShapes.forIconShape(settings.iconShape),
            frame = geometry.iconFrame,
        )

        is FolderCell -> FolderTile(
            folder = item,
            appsByKey = appsByKey,
            iconSize = iconSize,
            showLabel = settings.showLabels,
            frame = geometry.iconFrame,
            style = settings.folderStyle,
            // Cut like the app icons beside it. The free-form outlines (pebble, teardrop, hexagon,
            // clover) keep the squircle: the lens draws every free-form outline as a superellipse, so
            // glass clipped to one of them would show a squircle's rim cut off by the clip.
            shape = ZenithShapes.forIconShape(settings.iconShape).takeUnless { it is IconOutlineShape } ?: ZenithShapes.icon,
        )

        is WidgetCell -> {
            InOwnColour(item.accent) {
                ZenithWidget(
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
            WidgetName(Widgets.shortName(item.type), geometry.widgetNameStrip(item.spanX, item.spanY))
        }
    }
}

/**
 * A widget's name, written under it as an icon's is under the icon: in [strip], the strip the names
 * of the icons in its last row are in ([HomeGeometry.widgetNameStrip]), so they read as one line.
 * With labels off the strip is empty, and nothing is written.
 */
@Composable
private fun WidgetName(name: String, strip: PxRect) {
    if (strip.width <= 0 || strip.height <= 0) return
    Box(Modifier.fillMaxSize().pixelFrame(strip), contentAlignment = Alignment.BottomCenter) {
        IconLabel(name, textAlign = TextAlign.Center)
    }
}

/**
 * The name under another app's widget, [provider] being its flattened component name: the app it
 * comes from as the drawer has it, or as Android has it for an app the drawer does not have.
 *
 * Until the drawer's list has come in, which at every start is a moment after the home screen is
 * first drawn, the name is left empty rather than asked of Android: that would be a call to the
 * system for every such widget while the page is first composed, and would put Android's name up
 * where the drawer may have one of its own.
 */
@Composable
private fun hostedName(provider: String, appsByKey: Map<String, AppInfo>): String {
    val context = LocalContext.current
    return remember(provider, appsByKey) {
        Widgets.hostedName(provider, appsByKey.values)
            ?: if (appsByKey.isEmpty()) {
                ""
            } else {
                runCatching {
                    val packages = context.packageManager
                    packages.getApplicationLabel(packages.getApplicationInfo(provider.substringBefore('/'), 0)).toString()
                }.getOrNull() ?: Widgets.UNNAMED
            }
    }
}

/** What a folder shows of what is in it, in its [style]. */
@Composable
private fun FolderInside(folder: FolderCell, appsByKey: Map<String, AppInfo>, iconSize: androidx.compose.ui.unit.Dp, style: String) {
    when (style) {
        // The first three fanned like cards in a hand, the first in front.
        "stack" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val fan = listOf(Triple(-0.14f, -0.06f, -12f), Triple(0.14f, -0.04f, 11f), Triple(0f, 0.05f, 0f))
            val keys = folder.keys.take(3)
            keys.indices.reversed().forEach { from ->
                val (x, y, turn) = fan[2 - from]
                val key = keys[from]
                AppIcon(
                    key = key,
                    label = appsByKey[key]?.label ?: "",
                    size = iconSize * 0.46f,
                    onGlass = true,
                    modifier = Modifier.graphicsLayer {
                        translationX = x * iconSize.toPx()
                        translationY = y * iconSize.toPx()
                        rotationZ = turn
                    },
                )
            }
        }
        // The first app large, the next two small at its corner.
        "cover" -> Box(Modifier.fillMaxSize()) {
            folder.keys.firstOrNull()?.let { key ->
                AppIcon(key = key, label = appsByKey[key]?.label ?: "", size = iconSize * 0.54f, onGlass = true, modifier = Modifier.align(Alignment.TopStart))
            }
            Row(Modifier.align(Alignment.BottomEnd), horizontalArrangement = Arrangement.spacedBy(iconSize * 0.03f)) {
                folder.keys.drop(1).take(2).forEach { key ->
                    AppIcon(key = key, label = appsByKey[key]?.label ?: "", size = iconSize * 0.2f, onGlass = true)
                }
            }
        }
        else -> Column(
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
                            onGlass = true,
                        )
                    }
                    if (rowKeys.size == 1) Spacer(Modifier.size(iconSize * 0.33f))
                }
            }
        }
    }
}

/**
 * [content] in a widget's own colour, [accent], or in the plain look when it is 0. A change of
 * colour is eased in, and back out to the plain look, rather than switched. Its decoration follows
 * through [com.gabriel.zenithui.ui.theme.ZenithPalette.decor]: in its own colour as far as that has
 * eased in, and in the words' ink with none.
 */
@Composable
private fun InOwnColour(accent: Long, content: @Composable () -> Unit) {
    val palette = LocalPalette.current
    val ease = if (LocalMotionLevel.current.reduced) tween<Float>(Motion.REDUCED_MS) else tween(420)
    // The colour it last had of its own: going back to the plain look fades out of it, not out of nothing.
    val lastOwn = remember { arrayOf(palette.accent) }
    if (accent != 0L) lastOwn[0] = Color(accent.toInt())
    val own by androidx.compose.animation.animateColorAsState(
        lastOwn[0],
        if (LocalMotionLevel.current.reduced) tween(Motion.REDUCED_MS) else tween(420),
        label = "widgetAccent",
    )
    val mix by androidx.compose.animation.core.animateFloatAsState(if (accent != 0L) 1f else 0f, ease, label = "widgetOwn")
    val wearing = if (mix <= 0f) {
        palette
    } else {
        val its = palette.withAccent(own)
        palette.copy(
            accent = androidx.compose.ui.graphics.lerp(palette.accent, its.accent, mix),
            accentAlt = androidx.compose.ui.graphics.lerp(palette.accentAlt, its.accentAlt, mix),
            glassTint = androidx.compose.ui.graphics.lerp(palette.glassTint, its.glassTint, mix),
            ownColour = mix,
        )
    }
    CompositionLocalProvider(LocalPalette provides wearing, content = content)
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
                IconLabel(label)
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
    /** Shows the app's notification dot. */
    badge: Boolean = false,
    /** Over glass or the drawer already: see [AppIcon]. */
    onGlass: Boolean = false,
) {
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
            badge = badge,
            onGlass = onGlass,
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
                IconLabel(label, textAlign = TextAlign.Center)
            }
        }
    }
}

/**
 * One of an app's shortcuts on the page: the shortcut's own picture with the app's icon small in
 * its corner, as Android's own launcher shows them, so it is plain whose shortcut it is.
 */
@Composable
fun ShortcutTile(
    item: ShortcutCell,
    appsByKey: Map<String, AppInfo>,
    iconSize: androidx.compose.ui.unit.Dp,
    showLabel: Boolean,
    shape: androidx.compose.ui.graphics.Shape = ZenithShapes.icon,
    modifier: Modifier = Modifier,
    frame: PxRect? = null,
) {
    val density = LocalDensity.current
    val owner = remember(appsByKey, item.packageName) { appsByKey.values.firstOrNull { it.packageName == item.packageName }?.key }
    Column(
        modifier = if (frame == null) modifier.padding(horizontal = 2.dp) else modifier.fillMaxSize(),
        horizontalAlignment = if (frame == null) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = if (frame == null) Arrangement.Center else Arrangement.Top,
    ) {
        if (frame != null) Spacer(Modifier.height(with(density) { frame.top.toDp() }))
        Box(
            Modifier
                .then(if (frame != null) Modifier.padding(start = with(density) { frame.left.toDp() }) else Modifier)
                .size(iconSize)
                .launchOrigin(item.key)
                .pressBounce(),
        ) {
            AppIcon(key = item.key, label = item.label, size = iconSize, shape = shape)
            if (owner != null) {
                AppIcon(
                    key = owner,
                    label = "",
                    size = iconSize * 0.42f,
                    shape = shape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = iconSize * 0.06f, y = iconSize * 0.06f),
                )
            }
        }
        if (showLabel) {
            Box(
                (if (frame != null) Modifier.fillMaxWidth().padding(horizontal = 2.dp) else Modifier).height(LABEL_LINE_HEIGHT),
                contentAlignment = Alignment.BottomCenter,
            ) {
                IconLabel(item.label, textAlign = TextAlign.Center)
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
    /** "grid": four small, two by two; "stack": the first three fanned; "cover": the first large. */
    style: String = "grid",
    /** The outline of its glass and its colour, so that it is cut like the app icons beside it. */
    shape: androidx.compose.ui.graphics.Shape = ZenithShapes.icon,
) {
    val density = LocalDensity.current
    val own = if (folder.colour == 0L) null else Color(folder.colour.toInt())
    val shownColour by animateColorAsState(own ?: Color.Transparent, tween(420), label = "folderColour")
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
                .pressBounce(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .glass(shape, strength = 1.2f, material = GlassMaterial.Clear, interactive = true, tint = own ?: Color.Unspecified)
                    // A colour of its own, laid in the glass, easing in and out as it is chosen.
                    .background(shownColour.copy(alpha = shownColour.alpha * 0.3f), shape)
                    .padding(iconSize * 0.12f),
                contentAlignment = Alignment.Center,
            ) {
                FolderInside(folder, appsByKey, iconSize, style)
            }
            // One dot for the whole folder: all of its apps' put together.
            IconBadge(count = LocalBadges.current.forKeys(folder.keys), iconSize = iconSize)
        }
        if (showLabel) {
            Box(
                (if (frame != null) Modifier.fillMaxWidth() else Modifier).height(LABEL_LINE_HEIGHT),
                contentAlignment = Alignment.BottomCenter,
            ) {
                IconLabel(folder.title)
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
    /** Resizing is over: the outline fades and the pointer draws back into its corner, and then [onGone]. */
    leaving: Boolean = false,
    onGone: () -> Unit = {},
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val finish by rememberUpdatedState(onDone)
    val gone by rememberUpdatedState(onGone)
    // The spans are read live: the gesture below outlives every size it passes through.
    val live by rememberUpdatedState(item)
    val isApp = item is AppCell
    // A widget on the iPhone grid stands a little wider than the icons above it
    // (HomeGeometry.widgetReach), and its outline with it.
    val reach = if (isApp) 0f else geometry.widgetReach.toFloat()
    val side = geometry.sideInset.toFloat() - reach
    val sideEnd = geometry.sideInsetEnd.toFloat() - reach
    val topInset = geometry.topInset.toFloat()
    // A widget ends where its last row's icons end, with its name in the strip under it.
    val underWidget = geometry.underWidget.toFloat()
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
        spanY >= ROW_UNITS -> spanY * cellHeightPx - underWidget
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
            // Any number of row units: whichever height's outline ends nearest the finger, as for
            // whole rows. Adding the name's strip to the finger instead grew a widget shorter than
            // a row the moment it was pulled, since nothing is left under one of those for a name.
            free -> spanX to ((1..(unitRows - live.row)).minByOrNull { abs(restingBottom(it) - b) } ?: 1)
            else -> spanX to (((1..room).minByOrNull { abs(restingBottom(it * ROW_UNITS) - b) } ?: 1) * ROW_UNITS)
        }
    }

    // The top edge only moves when an app changes how many rows it spans; it glides there too.
    val frameTop by animateFloatAsState(top(item.spanY), settle, label = "frameTop")
    val appear = remember { Animatable(0f) }
    LaunchedEffect(leaving) {
        if (leaving) {
            // Back the way it came, quicker, and without the bounce it arrived with.
            appear.animateTo(0f, spring(dampingRatio = 1f, stiffness = 520f))
            gone()
        } else {
            appear.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f))
        }
    }
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
            // typing in a note or flipping a toggle. On its way out it is only a picture.
            .then(if (leaving) Modifier else Modifier.pointerInput(Unit) { detectTapGestures(onTap = { finish() }) }),
    ) {
        // The outline, with a soft white glow round it: it is drawn on the wallpaper.
        Canvas(Modifier.matchParentSize()) {
            val out = grow.toPx()
            val frame = Rect(side - out, frameTop - out, right.value + out, bottom.value + out)
            val radius = CornerRadius(radius() + out)
            val fade = appear.value.coerceIn(0f, 1f)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.28f * fade),
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
        ) { drawPointer() }

        // What your finger takes hold of: a generous square over the pointer and the corner.
        val touch = 56.dp
        if (!leaving) Box(
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
 * and a soft shadow so it reads on any wallpaper.
 */
private fun DrawScope.drawPointer() {
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
    LaunchedEffect(item.id) { lift.animateTo(carriedScale(item), Motion.lift()) }
    val shadowFrame = when {
        item.isWidgetLike -> geometry.widgetFrame(item.spanX, item.spanY)
        item is AppCell && !item.isPlainIcon -> geometry.superIconFrame(item.spanX, (item.spanY / ROW_UNITS).coerceAtLeast(1))
        else -> geometry.iconFrame
    }
    val ghostAlpha by animateFloatAsState(if (drag.overRemove) COPY_ALPHA_OVER_BIN else COPY_ALPHA, Motion.smooth(), label = "ghost")
    val reduced = LocalMotionLevel.current.reduced

    // The gathered ones, behind it: each flies from its place into the stack, fanned a little.
    drag.gathered.forEachIndexed { index, (other, from) ->
        key(other.id) {
            val flight = remember { Animatable(if (reduced) 1f else 0f) }
            LaunchedEffect(Unit) { flight.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 300f)) }
            val depth = (drag.gathered.size - index).coerceAtMost(3)
            val step = with(density) { 5.dp.toPx() }
            GreyCopy(
                item = other,
                geometry = geometry,
                settings = settings,
                appsByKey = appsByKey,
                modifier = Modifier
                    .offset {
                        val stack = topLeft + Offset(step * depth, -step * depth)
                        val at = from + (stack - from) * flight.value
                        IntOffset(at.x.roundToInt(), at.y.roundToInt())
                    }
                    .requiredSize(with(density) { drag.cellWidth.toDp() }, with(density) { (drag.cellHeight * other.spanY).toDp() })
                    .graphicsLayer {
                        val size = 1f + (carriedScale(other) - 0.06f * depth - 1f) * flight.value
                        scaleX = size
                        scaleY = size
                        rotationZ = (if (index % 2 == 0) 5f else -5f) * depth * flight.value
                        alpha = COPY_ALPHA * (0.6f + 0.4f * flight.value)
                    },
            )
        }
    }

    GreyCopy(
        item = item,
        geometry = geometry,
        settings = settings,
        appsByKey = appsByKey,
        modifier = Modifier
            .offset { IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()) }
            .requiredSize(width, height)
            // Off the page, it casts a shadow: a soft pool under it, a little lower than it is.
            .drawWithCache {
                val centre = Offset(
                    (shadowFrame.left + shadowFrame.right) / 2f,
                    (shadowFrame.top + shadowFrame.bottom) / 2f + shadowFrame.height * 0.16f,
                )
                val reach = shadowFrame.width * 0.72f
                val squash = (shadowFrame.height * 0.72f / reach).coerceIn(0.3f, 1f)
                val pool = Brush.radialGradient(
                    listOf(Color.Black.copy(alpha = 0.34f), Color.Transparent),
                    center = centre,
                    radius = reach,
                )
                onDrawBehind {
                    val up = ((lift.value - 1f) / 0.02f).coerceIn(0f, 1f)
                    if (up > 0f) {
                        scale(scaleX = 1f, scaleY = squash, pivot = centre) {
                            drawCircle(pool, radius = reach, center = centre, alpha = up)
                        }
                    }
                }
            }
            .graphicsLayer {
                scaleX = lift.value
                scaleY = lift.value
                alpha = ghostAlpha
            },
    )

    // How many are carried, once there is more than one, on the corner of the icon.
    val count = drag.gathered.size + 1
    androidx.compose.animation.AnimatedVisibility(
        visible = count > 1,
        enter = com.gabriel.zenithui.ui.common.popIn(reduced),
        exit = com.gabriel.zenithui.ui.common.popOut(reduced),
        modifier = Modifier.offset {
            val icon = geometry.iconFrame
            IntOffset((topLeft.x + icon.right - 10.dp.toPx()).roundToInt(), (topLeft.y + icon.top - 10.dp.toPx()).roundToInt())
        },
    ) {
        val palette = LocalPalette.current
        androidx.compose.animation.AnimatedContent(
            targetState = count,
            transitionSpec = { com.gabriel.zenithui.ui.common.rollOver(reduced) },
            label = "carriedCount",
        ) { shown ->
            Box(
                Modifier
                    .size(24.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    // A count on a corner is a badge, and badges are iOS's red with white in them.
                    .background(palette.destructive),
                contentAlignment = Alignment.Center,
            ) {
                Text("$shown", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** As many as can be gathered onto one carried icon. */
private const val MOST_GATHERED = 15

/**
 * The grey copy once it has been let go into the bin, the dock or a folder: it shrinks away into
 * where it went, quicker the nearer it gets, and is gone. With movement reduced it only fades.
 */
@Composable
fun SentGhost(
    sent: Sent,
    drag: HomeDragState,
    settings: ZenithSettings,
    appsByKey: Map<String, AppInfo>,
    onGone: () -> Unit,
) {
    val geometry = drag.geometry
    val reduced = LocalMotionLevel.current.reduced
    val gone by rememberUpdatedState(onGone)
    val away = remember(sent) { Animatable(0f) }
    LaunchedEffect(sent) {
        if (geometry != null) {
            away.animateTo(1f, if (reduced) tween(Motion.REDUCED_MS) else tween(240, easing = FastOutLinearInEasing))
        }
        gone()
    }
    if (geometry == null) return
    val item = sent.item
    val density = LocalDensity.current
    val solid = if (sent.faint) COPY_ALPHA_OVER_BIN else COPY_ALPHA

    GreyCopy(
        item = item,
        geometry = geometry,
        settings = settings,
        appsByKey = appsByKey,
        modifier = Modifier
            .offset { IntOffset(sent.topLeft.x.roundToInt(), sent.topLeft.y.roundToInt()) }
            .requiredSize(
                with(density) { (drag.cellWidth * item.spanX).toDp() },
                with(density) { (drag.cellHeight * item.spanY).toDp() },
            )
            .graphicsLayer {
                val out = away.value
                alpha = solid * (1f - out * out)
                if (!reduced) {
                    val small = carriedScale(item) * (1f - 0.8f * out)
                    scaleX = small
                    scaleY = small
                    val into = sent.into
                    if (into != null) {
                        translationX = (into.x - (sent.topLeft.x + size.width / 2f)) * out
                        translationY = (into.y - (sent.topLeft.y + size.height / 2f)) * out
                    }
                }
            },
    )
}

/**
 * [item] as it stands on the page with the colour drained out of it, so it reads as a stand-in for
 * the real one: what is under your finger while that waits where it was.
 */
@Composable
private fun GreyCopy(
    item: HomeItem,
    geometry: HomeGeometry,
    settings: ZenithSettings,
    appsByKey: Map<String, AppInfo>,
    modifier: Modifier,
) {
    val greyPaint = remember {
        Paint().apply {
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        }
    }
    Box(
        modifier.drawWithContent {
            drawIntoCanvas { canvas ->
                canvas.saveLayer(Rect(Offset.Zero, size), greyPaint)
                drawContent()
                canvas.restore()
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        // Carried over the page, it is still on the wallpaper, and its name is written as on the page.
        OnWallpaper {
            HomeItemContent(
                item = item,
                settings = settings,
                appsByKey = appsByKey,
                geometry = geometry,
                onSearch = {},
                onNoteChange = {},
                live = false,
            )
        }
    }
}

/** Puts a row's children at fixed places from its left edge, mirrored when the layout is. */
internal class SlotArrangement(private val lefts: List<Int>) : Arrangement.Horizontal {
    override fun Density.arrange(totalSize: Int, sizes: IntArray, layoutDirection: LayoutDirection, outPositions: IntArray) {
        for (i in sizes.indices) {
            val left = lefts.getOrElse(i) { 0 }
            outPositions[i] = if (layoutDirection == LayoutDirection.Ltr) left else totalSize - left - sizes[i]
        }
    }
}

/**
 * The dock: a slab of clear glass floating just in from the edges of the screen, its corners as
 * round as an iPhone's, with apps on it and no names under them. Apps arrive by being dragged onto
 * it from the home screen, and once there they are rearranged the same way: hold one, slide it
 * along and the others make room; lift it up off the dock and it goes back to the home screen.
 * Holding without moving still opens the app's menu. Something carried over it from the page makes
 * it swell a little, to say that letting go there puts it in.
 *
 * Glass and Floating are both the slab. Across the screen, its apps share out the page's width, so
 * with one for each column each stands under the icons above it; Floating closes up round fewer
 * than four. Icons only puts the apps on the wallpaper with no glass behind them. See
 * [HomeGeometry.dockSlabWidth] and [HomeGeometry.dockIconX].
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
    /**
     * Where something carried from the page is let go into the dock, on screen: the slab across the
     * screen whatever the style, so a floating dock closed up round its apps takes a drop anywhere
     * along it, as it always has.
     */
    onBounds: (Rect) -> Unit,
    /**
     * Where the middle of each app in the dock stands across the screen, in order: something carried
     * from the page and let go on the dock takes its place among them by these, as an app moved
     * along the dock does.
     */
    onCentres: (List<Float>) -> Unit = {},
    animateEntrance: Boolean = false,
    /** The page's geometry: the dock stands on the same columns and its icons are the same size. */
    geometry: HomeGeometry? = null,
    modifier: Modifier = Modifier,
    /**
     * Reads whatever moves the dock with a layer, such as its arrival, so the wallpaper in its glass
     * stays where it is on screen while the dock moves.
     */
    tracking: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val reduced = LocalMotionLevel.current.reduced
    // The apps that have had their entrance. One put in the dock later rises into it once; one
    // that was taken out and comes back rises in again.
    val known = remember { if (animateEntrance) HashSet<String>() else keys.toHashSet() }
    known.retainAll(keys.toSet())
    // Where each app was last seen: how far along the dock, how far off its line, and how big.
    // An app whose place changes — the others closing up round a gap, one more coming in, the one
    // in your hand let go — starts from here and settles to where it now belongs, so nothing in
    // the dock is ever in one place and then in another.
    val seenAt = remember { HashMap<String, FloatArray>() }
    seenAt.keys.retainAll(keys.toSet())
    /** How many times each app has been let go, so that being put down is always a fresh start. */
    val drops = remember { HashMap<String, Int>() }
    val density = LocalDensity.current
    val style = settings.dockStyle
    // The page's icon size, or a little smaller when more apps than columns have to share the dock.
    val iconPx = geometry?.dockIcon(keys.size) ?: with(density) { STANDARD_ICON_SIZE.roundToPx() }
    val iconSize = with(density) { iconPx.toDp() }
    val iconSizePx = iconPx.toFloat()
    // Closed up round fewer than four, the apps stand somewhere other than across the screen, and
    // going from one to the other is a move like any other, seen from where they were.
    val hugs = HomeGeometry.dockHugs(style, keys.size)
    // Icons only has no glass.
    val paned = style != "none"

    // Where the dock's place is on screen, the whole width and as tall as the slab, and, for when
    // no page has been measured yet and there are no lines to go by, where each icon was found.
    var placeAt by remember { mutableStateOf(Rect.Zero) }
    val measured = remember { androidx.compose.runtime.mutableStateMapOf<Int, Rect>() }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var grabRoot by remember { mutableStateOf(Offset.Zero) }
    var suppressTap by remember { mutableStateOf(false) }

    // The gesture below is set up once and outlives recompositions, so everything it asks at the
    // moment of release is read from live state through these, never from values captured then.
    val currentKeys by rememberUpdatedState(keys)
    val currentIconPx by rememberUpdatedState(iconPx)
    val currentOnReorder by rememberUpdatedState(onReorder)
    val currentOnDragOut by rememberUpdatedState(onDragOut)
    val currentOnLongPress by rememberUpdatedState(onLongPress)

    // Something carried over it from the page: it swells a little, read only while drawing. It
    // used to wear an accent rim as well; the swell alone says it.
    val swell = animateFloatAsState(if (highlight) 1f else 0f, tween(220), label = "dockSwell")
    // The finger's light in the slab. The apps stand over the slab rather than in it, so one carried
    // up off the dock is not cut off at its edge; the light is driven from the whole dock instead of
    // by a touch of the slab's own, so it still comes up under an app as it is pressed.
    val lightScope = rememberCoroutineScope()
    val light = remember { PressLight(lightScope) }

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(HomeGeometry.DOCK_HEIGHT_DP.dp)
            // Found before the dock is given its layer, so that swelling for something carried over
            // it does not move its own lines, nor the place that takes a drop.
            .onGloballyPositioned { coords ->
                val at = coords.boundsInRoot()
                placeAt = at
                // The slab across the screen, its inset in from each side scaled as the home
                // screen is when it is found.
                val width = coords.size.width
                val slab = HomeGeometry.dockSlabWidth("glass", 0, width, 0, density.density)
                val inset = if (width > 0) (width - slab) / 2f * at.width / width else 0f
                onBounds(Rect(at.left + inset, at.top, at.right - inset, at.bottom))
            },
    ) {
        val wide = constraints.maxWidth
        val currentWide by rememberUpdatedState(wide)
        // Where each app stands, from the screen's left edge. Before a page has been measured there
        // are no lines to go by, and the apps are simply spread out.
        val lefts = remember(geometry, style, keys.size, wide) {
            geometry?.let { g -> List(keys.size) { g.dockIconX(style, it, keys.size, wide) } }
        }
        val currentLefts by rememberUpdatedState(lefts)
        val arrangement = remember(lefts) { lefts?.let { SlotArrangement(it) } }
        // The slab grows and shrinks round its apps as they come and go, or as its style changes.
        // Read while laying out, so that it lays the slab out again and recomposes nothing.
        val slab = animateIntAsState(
            targetValue = HomeGeometry.dockSlabWidth(style, keys.size, wide, iconPx, density.density),
            animationSpec = if (reduced) snap() else spring(dampingRatio = 0.8f, stiffness = 380f),
            label = "dockSlab",
        )

        /**
         * Where icon [index] belongs on screen. Worked out from the dock's place and the page's
         * lines, not measured from the icon: an icon is often somewhere else, on its way to its
         * place, and where it happens to be drawn must never feed back into where it belongs.
         */
        fun slotOf(index: Int): Rect? {
            val place = placeAt
            val left = currentLefts?.getOrNull(index)
            if (left == null || place.isEmpty) return measured[index]
            val side = currentIconPx.toFloat()
            val top = place.top + (place.height - side) / 2f
            return Rect(place.left + left, top, place.left + left + side, top + side)
        }

        // The apps' middles, told again whenever where they belong changes: the dock found, the page
        // measured, an app more or less, another style.
        val currentOnCentres by rememberUpdatedState(onCentres)
        LaunchedEffect(Unit) {
            androidx.compose.runtime.snapshotFlow { List(currentKeys.size) { slotOf(it)?.center?.x ?: 0f } }
                .collect { currentOnCentres(it) }
        }

        /** Carried far enough up to leave the dock. */
        fun liftedNow(): Boolean = draggingIndex != null && dragOffset.y < -iconSizePx * 1.2f

        /** The index the carried icon would take if let go now. */
        fun targetNow(): Int? {
            val carriedFrom = draggingIndex ?: return null
            if (liftedNow()) return carriedFrom
            val carried = (slotOf(carriedFrom)?.center?.x ?: 0f) + dragOffset.x
            return currentKeys.indices.count { it != carriedFrom && (slotOf(it)?.center?.x ?: 0f) < carried }
        }

        val from = draggingIndex
        val spacing = if (keys.size > 1) {
            val a = slotOf(0)
            val b = slotOf(1)
            if (a != null && b != null) b.center.x - a.center.x else iconSizePx
        } else {
            iconSizePx
        }
        val lifted = liftedNow()
        val target = targetNow()

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val grow = 1f + 0.03f * swell.value
                    scaleX = grow
                    scaleY = grow
                }
                .then(
                    if (paned) {
                        Modifier.lightsPane(light) { at ->
                            // From the dock's place onto the slab, which stands in the middle of it.
                            val width = slab.value
                            val x = at.x - (currentWide - width) / 2f
                            if (x in 0f..width.toFloat()) Offset(x, at.y) else null
                        }
                    } else {
                        Modifier
                    }
                ),
        ) {
            if (paned) DockSlab(width = { slab.value }, light = light, tracking = tracking)
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = when {
                    keys.isEmpty() -> Arrangement.Center
                    arrangement != null -> arrangement
                    else -> Arrangement.SpaceEvenly
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (keys.isEmpty()) {
                    // White, with the soft shadow the names on the wallpaper have, as Search's word
                    // is: it stands on clear glass that only mists the wallpaper, or on the wallpaper.
                    Text(
                        text = if (highlight) "Let go to add it" else "Drag an app here to add it to the dock",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        style = LocalTextStyle.current.merge(
                            TextStyle(shadow = labelShadow(onWallpaper = true, legacyShadow = false, density = density.density))
                        ),
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                }
                keys.forEachIndexed { index, key ->
                    // What it remembers belongs to this app, in this place, among this many, closed up
                    // or not, since it was last put down. When any of those changes it starts again
                    // from where it was last seen: see seenAt.
                    androidx.compose.runtime.key(key, index, keys.size, drops[key] ?: 0, hugs) {
                    val isCarried = index == from
                    // Neighbours step aside to show where the carried icon will land.
                    val shift = when {
                        from == null || target == null || isCarried -> 0f
                        from < target && index in (from + 1)..target -> -spacing
                        from > target && index in target until from -> spacing
                        else -> 0f
                    }
                    val shiftAnimated by animateFloatAsState(shift, spring(dampingRatio = 0.75f, stiffness = 400f), label = "dockShift")
                    // Picked up, it comes off the dock and rocks once, like anything else that is lifted.
                    val lift by animateFloatAsState(
                        targetValue = if (isCarried) DOCK_LIFT else 1f,
                        animationSpec = if (reduced) snap() else Motion.lift(),
                        label = "dockLift",
                    )
                    val fresh = remember { known.add(key) }
                    val left = lefts?.getOrNull(index)
                    // Out of its place by this much as it starts, and bigger by this much.
                    val astray = remember {
                        val was = seenAt[key]
                        if (was == null || left == null || reduced) floatArrayOf(0f, 0f, 1f) else floatArrayOf(was[0] - left, was[1], was[2])
                    }
                    val cameDown = astray[2] > 1.001f
                    val settle = remember { Animatable(if (astray[0] == 0f && astray[1] == 0f && !cameDown) 0f else 1f) }
                    LaunchedEffect(Unit) {
                        if (settle.value != 0f) settle.animateTo(0f, Motion.drop())
                    }

                    Box(
                        Modifier
                            .then(
                                if (fresh) {
                                    Modifier.appearIn(delayMillis = 90 + staggerDelay(index), riseFrom = 26f)
                                } else {
                                    Modifier
                                }
                            )
                            // The one just put down settles over its neighbours, not under them.
                            .zIndex(if (isCarried || cameDown) 1f else 0f)
                            .onGloballyPositioned { coords ->
                                if (lefts == null && draggingIndex == null) measured[index] = coords.boundsInRoot()
                            }
                            .graphicsLayer {
                                val back = settle.value
                                val x = (if (isCarried) dragOffset.x else shiftAnimated) + astray[0] * back
                                val y = (if (isCarried) dragOffset.y else 0f) + astray[1] * back
                                val grown = lift * (1f + (astray[2] - 1f) * back)
                                translationX = x
                                translationY = y
                                scaleX = grown
                                scaleY = grown
                                if (left != null) {
                                    val seen = seenAt.getOrPut(key) { FloatArray(3) }
                                    seen[0] = left + x
                                    seen[1] = y
                                    seen[2] = grown
                                }
                            }
                            .launchOrigin(key)
                            .pointerInput(key) {
                                detectTapGestures(
                                    onPress = { suppressTap = false },
                                    onTap = { if (!suppressTap) onOpen(key) },
                                )
                            }
                            .pointerInput(key, index, settings.lockLayout) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { local ->
                                        suppressTap = true
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        grabRoot = (slotOf(index)?.topLeft ?: Offset.Zero) + local
                                        dragOffset = Offset.Zero
                                        // Locked, a hold only ever opens the menu.
                                        if (!settings.lockLayout) draggingIndex = index
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        if (!settings.lockLayout) dragOffset += amount
                                    },
                                    onDragEnd = {
                                        val moved = dragOffset.getDistance()
                                        val start = draggingIndex
                                        val end = targetNow()
                                        when {
                                            // Held still: that is a long press, so show the menu.
                                            moved < iconSizePx * 0.25f -> currentOnLongPress(key)
                                            liftedNow() -> currentOnDragOut(key, (slotOf(index)?.center ?: grabRoot) + dragOffset)
                                            start != null && end != null && end != start -> currentOnReorder(start, end)
                                        }
                                        // Put down: from where it was let go it settles into its place.
                                        // How far it was carried is left as it is until the next
                                        // pick-up clears it. Clearing it here would draw the icon back
                                        // in its old place for a moment, and that, not where it was
                                        // let go, is what it would then be seen to start from.
                                        drops[key] = (drops[key] ?: 0) + 1
                                        draggingIndex = null
                                    },
                                    onDragCancel = {
                                        // Also what a gesture hears as its icon is taken out from
                                        // under it, long after it was put down: that is not a drop.
                                        if (draggingIndex == index) {
                                            drops[key] = (drops[key] ?: 0) + 1
                                            draggingIndex = null
                                        }
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
                            badge = true,
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
    }
}

/**
 * The dock's slab of clear glass, [width] pixels wide and as tall as the box it stands in the middle
 * of: the real dock's, and the small home screens' in setup and Settings, so the two are one pane.
 * [width] is read while laying out, so a slab growing round its apps lays itself out again and
 * recomposes nothing. [light] is the finger's light in it, driven from outside, for a dock that can
 * be pressed; [tracking] reads whatever moves it with a layer.
 */
@Composable
internal fun BoxScope.DockSlab(width: () -> Int, light: PressLight? = null, tracking: (() -> Unit)? = null) {
    Box(
        Modifier
            .align(Alignment.Center)
            .layout { measurable, constraints ->
                val wide = width().coerceIn(0, constraints.maxWidth)
                val placeable = measurable.measure(Constraints.fixed(wide, constraints.maxHeight))
                layout(wide, constraints.maxHeight) { placeable.place(0, 0) }
            }
            .glass(
                RoundedCornerShape(HomeGeometry.DOCK_CORNER_DP.dp),
                strength = 1.15f,
                material = GlassMaterial.Clear,
                interactive = light != null,
                light = light,
                tracking = tracking,
            ),
    )
}

/**
 * Lights [light]'s pane under a finger anywhere in this, as an interactive pane lights itself, for
 * a pane with things standing over it that take the finger first: the dock's slab, under its apps.
 * [onPane] turns a point here into one on the pane, or null where the pane is not. It watches the
 * finger without taking it, so taps, holds and drags still belong to whatever is pressed.
 */
private fun Modifier.lightsPane(light: PressLight, onPane: (Offset) -> Offset?): Modifier = pointerInput(light) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val at = onPane(down.position) ?: return@awaitEachGesture
        light.press(at)
        try {
            while (true) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                onPane(change.position)?.let { light.at = it }
            }
        } finally {
            light.release()
        }
    }
}

/** How much bigger than itself a dock icon is while it is in your hand. */
private const val DOCK_LIFT = 1.14f

/** How far items spread apart, in cells per cell from the middle, as a page slides fully away. */
private const val PAGE_PARALLAX = 0.35f

private const val TWO_PI = 6.2831855f

/** Height of a single 11sp app label line, plus its spacer — the same strip [HomeGeometry] counts. */
private val LABEL_LINE_HEIGHT = HomeGeometry.LABEL_DP.dp

val TransparentColor: Color = Color.Transparent
