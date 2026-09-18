package com.gabriel.zenithui.ui.home

import com.gabriel.zenithui.ui.common.WideAppIcon
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.zIndex
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.gabriel.zenithui.data.AppCell
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.FolderCell
import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.LayoutBuilder
import com.gabriel.zenithui.data.ROW_UNITS
import com.gabriel.zenithui.data.WidgetCell
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.AppIcon
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.launchOrigin
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
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

    /** The lines icons sit on in the live page, so the ghost is cut to them too. */
    var lines by mutableStateOf<IconLines?>(null)

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
    }

    /**
     * The cell the dragged item would take, or null when it has been carried right off the grid.
     *
     * An item that only hangs over an edge — a wide widget let go near the side, an icon dragged
     * a little too low — is pulled back onto the grid rather than refused. Refusing it made the
     * item jump back to where it started, which reads as the launcher ignoring you.
     */
    fun cellUnderItem(columns: Int, rows: Int, rowStep: Int = 1): Pair<Int, Int>? {
        val current = item ?: return null
        if (cellWidth <= 0f || cellHeight <= 0f) return null
        val topLeft = pointer - grab
        // Only when the middle of what you are carrying is off the grid is it truly off it.
        val centreX = topLeft.x - gridOrigin.x + current.spanX * cellWidth / 2f
        val centreY = topLeft.y - gridOrigin.y + current.spanY * cellHeight / 2f
        val slackX = cellWidth / 2f
        val slackY = cellHeight * ROW_UNITS / 2f
        if (centreX < -slackX || centreX > columns * cellWidth + slackX) return null
        if (centreY < -slackY || centreY > rows * cellHeight + slackY) return null
        // Where the item is actually being drawn, rounded to the closest cell and kept on the grid.
        val col = ((topLeft.x - gridOrigin.x) / cellWidth).roundToInt()
            .coerceIn(0, (columns - current.spanX).coerceAtLeast(0))
        val rawRow = ((topLeft.y - gridOrigin.y) / cellHeight).roundToInt()
            .coerceIn(0, (rows - current.spanY).coerceAtLeast(0))
        // Icons stay on tidy boundaries; widgets may sit on any unit.
        val row = if (rowStep > 1) {
            ((rawRow.toFloat() / rowStep).roundToInt() * rowStep).coerceAtMost(rows - current.spanY).let { it - it % rowStep }
        } else {
            rawRow
        }
        return col to row.coerceAtLeast(0)
    }

    fun cellUnderPointer(columns: Int, rows: Int): Pair<Int, Int>? {
        if (cellWidth <= 0f || cellHeight <= 0f) return null
        val col = ((pointer.x - gridOrigin.x) / cellWidth).toInt()
        val row = ((pointer.y - gridOrigin.y) / cellHeight).toInt()
        if (col !in 0 until columns || row !in 0 until rows) return null
        return col to row
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
    onResize: (String, Int, Int) -> Boolean,
    onOpenApp: (String) -> Unit,
    onOpenFolder: (FolderCell) -> Unit,
    onItemMenu: (HomeItem) -> Unit,
    onSearch: () -> Unit,
    onNoteChange: (String) -> Unit,
    onAmbient: () -> Unit,
    onDrop: () -> Unit,
    resolveDrop: (HomeItem, Int, Int) -> Pair<Int, Int>?,
    /** Row units between the positions an icon may take; smaller with free placement. */
    iconRowStep: Int = LayoutBuilder.ICON_ROW_STEP,
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                // Only the page in view owns the geometry drops are resolved against.
                if (active) drag.gridOrigin = coords.positionInRoot()
            },
    ) {
        val cellWidthPx = constraints.maxWidth.toFloat() / settings.columns
        // One cell is a row unit tall; an icon spans ROW_UNITS of them.
        val cellHeightPx = constraints.maxHeight.toFloat() / (settings.rows * ROW_UNITS)
        if (active) {
            drag.cellWidth = cellWidthPx
            drag.cellHeight = cellHeightPx
        }
        val cellWidthDp = with(density) { cellWidthPx.toDp() }
        val cellHeightDp = with(density) { cellHeightPx.toDp() }
        val rowHeightDp = cellHeightDp * ROW_UNITS
        val iconSize = homeIconSize(cellWidthDp, rowHeightDp, settings.showLabels)
        if (active) drag.iconSize = iconSize
        val lines = iconLines(cellWidthDp, rowHeightDp, iconSize, settings.showLabels)
        if (active) drag.lines = lines

        // Drop hint for the cell the dragged item would land in.
        val dragged = drag.item
        if (dragged != null && active) {
            // The raw row under the item; which rows it may actually take is resolveDrop's call,
            // so a row tucked under a widget is not rounded away before it is even considered.
            val cell = drag.cellUnderItem(
                columns = settings.columns,
                rows = settings.rows * ROW_UNITS,
                rowStep = 1,
            )
            // The cell under your finger may be taken, in which case the drop lands beside it.
            // Point at that opening instead: a hint showing somewhere the item will not go is
            // worse than no hint at all.
            val target = cell?.let { resolveDrop(dragged, it.first, it.second) }
            if (target != null && drag.hoverItemId == null) {
                // With free placement the item keeps the exact spot it is let go at, so the hint
                // shows that spot — worked out the same way the drop does — and glides after the
                // finger. It only jumps to a cell when the one under the finger is taken, which
                // is exactly when the drop would jump there too.
                val exact = freePlacement && target == cell
                val hintTarget = if (exact) {
                    val topLeft = drag.pointer - drag.grab - drag.gridOrigin
                    val offX = (topLeft.x / cellWidthPx - target.first).coerceIn(-0.49f, 0.49f)
                    val offY = (topLeft.y / cellHeightPx - target.second).coerceIn(-0.49f, 0.49f)
                    IntOffset(
                        ((target.first + offX) * cellWidthPx).roundToInt(),
                        ((target.second + offY) * cellHeightPx).roundToInt(),
                    )
                } else {
                    IntOffset(
                        (target.first * cellWidthPx).roundToInt(),
                        (target.second * cellHeightPx).roundToInt(),
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
                    if (dragged is AppCell && dragged.spanY > ROW_UNITS) {
                        // A tall icon's hint is the icon itself, not the two rows around it.
                        val frame = tallIconFrame(cellWidthDp, rowHeightDp, iconSize)
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            Box(
                                Modifier
                                    .padding(top = lines.top + frame.drop)
                                    .width(cellWidthDp - lines.side * 2)
                                    .height(frame.height)
                                    .appearIn(fromScale = 0.8f, riseFrom = 0f)
                                    .clip(RoundedCornerShape((cellWidthDp - lines.side * 2) * 0.26f))
                                    .background(palette.onGlass.copy(alpha = 0.12f))
                            )
                        }
                    } else if (dragged is WidgetCell || (dragged is AppCell && !dragged.isPlainIcon)) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .toIconLines(lines, dragged.spanY)
                                .appearIn(fromScale = 0.8f, riseFrom = 0f)
                                .clip(ZenithShapes.tile)
                                .background(palette.onGlass.copy(alpha = 0.12f))
                        )
                    } else {
                        // An icon is drawn above its label, so the hint has to sit above the
                        // middle of the cell by exactly the same amount or it looks offset.
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                Modifier
                                    .size(iconSize + 14.dp)
                                    .appearIn(fromScale = 0.8f, riseFrom = 0f)
                                    .clip(ZenithShapes.tile)
                                    .background(palette.onGlass.copy(alpha = 0.12f))
                            )
                            if (settings.showLabels) Spacer(Modifier.height(LABEL_LINE_HEIGHT))
                        }
                    }
                }
            }
        }

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
            val scale by animateFloatAsState(if (hovered) 1.18f else 1f, Motion.bouncy(), label = "hover")
            // Glide when the item changes cell, but snap when the grid itself is re-measured.
            // The cell plus any free-placement offset, so a freely placed item sits exactly where
            // it was let go rather than on the grid line.
            val target = IntOffset(
                ((item.col + item.dx) * cellWidthPx).roundToInt(),
                ((item.row + item.dy) * cellHeightPx).roundToInt(),
            )
            val cellOffset = remember { Animatable(target, IntOffset.VectorConverter) }
            var lastGeometry by remember { mutableStateOf(cellWidthPx to cellHeightPx) }
            LaunchedEffect(target, cellWidthPx, cellHeightPx) {
                val geometry = cellWidthPx to cellHeightPx
                val remeasured = geometry != lastGeometry
                lastGeometry = geometry
                if (remeasured) cellOffset.snapTo(target) else cellOffset.animateTo(target, Motion.cell)
            }

            Box(
                Modifier
                    .offset { cellOffset.value }
                    .requiredSize(cellWidthDp * item.spanX, cellHeightDp * item.spanY)
                    .onGloballyPositioned { cellRoot = it.positionInRoot() }
                    .then(
                        if (animateEntrance || item.id !in firstIds) {
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
                        if (item is AppCell) alpha = AppLanding.alphaFor(item.key)
                        // Parallax: while pages slide, items drift apart from the middle of the page
                        // and settle back together as it lands, so the page has depth.
                        translationX = pageOffset * cellWidthPx * PAGE_PARALLAX *
                            (item.col + item.spanX / 2f - settings.columns / 2f)
                    },
                contentAlignment = Alignment.Center,
            ) {
                // A slim widget only listens inside its own bar, so the empty part of the cell
                // still belongs to the home screen.
                val slim = item is WidgetCell && item.type == "search"
                Box(
                    modifier = (if (slim) Modifier.fillMaxWidth().height(SEARCH_BAR_HEIGHT) else Modifier.fillMaxSize())
                        .onGloballyPositioned { contentRoot = it.positionInRoot() }
                        .then(if (item is AppCell) Modifier.launchOrigin(item.key) else Modifier)
                        .pointerInput(item.id, cellWidthPx, cellHeightPx, settings.columns) {
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
                                    drag.hoverCandidateId =
                                        hoverTarget(drag, liveItems, liveItem, settings.columns, settings.rows * ROW_UNITS)
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
                        .pointerInput(item.id) {
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
                                        item is AppCell -> onOpenApp(item.key)
                                        item is FolderCell -> onOpenFolder(item)
                                        // Tapping the clock opens the bedside screen; long press
                                        // still belongs to the drag and menu detector.
                                        item is WidgetCell && item.type == "clock" -> onAmbient()
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
                        iconSize = iconSize,
                        lines = lines,
                        onSearch = onSearch,
                        onNoteChange = onNoteChange,
                        onAmbient = onAmbient,
                    )

                    if (item.id == resizingId && item is WidgetCell) {
                        ResizeFrame(
                            item = item,
                            cellWidthPx = cellWidthPx,
                            cellHeightPx = cellHeightPx,
                            lines = lines,
                            onResize = onResize,
                        )
                    }
                }
            }
            }
        }
    }
}

private fun hoverTarget(
    drag: HomeDragState,
    items: List<HomeItem>,
    dragged: HomeItem,
    columns: Int,
    rows: Int,
): String? {
    if (dragged !is AppCell || !dragged.isPlainIcon) return null
    val cell = drag.cellUnderPointer(columns, rows) ?: return null
    val target = items.firstOrNull {
        it.id != dragged.id &&
            cell.first >= it.col && cell.first < it.col + it.spanX &&
            cell.second >= it.row && cell.second < it.row + it.spanY
    } ?: return null
    return if ((target is AppCell && target.isPlainIcon) || target is FolderCell) target.id else null
}

@Composable
fun HomeItemContent(
    item: HomeItem,
    settings: ZenithSettings,
    appsByKey: Map<String, AppInfo>,
    iconSize: androidx.compose.ui.unit.Dp = STANDARD_ICON_SIZE,
    /** Where icons sit in their cells; widgets and super icons are cut to the same lines. */
    lines: IconLines? = null,
    onSearch: () -> Unit,
    onNoteChange: (String) -> Unit,
    onAmbient: () -> Unit = {},
) {
    when (item) {
        is AppCell -> {
            val app = appsByKey[item.key]
            val label = app?.label ?: item.packageName.substringAfterLast('.')
            val shape = ZenithShapes.forIconShape(settings.iconShape)
            when {
                !item.isPlainIcon ->
                    SuperAppTile(
                        key = item.key,
                        label = label,
                        iconSize = iconSize,
                        lines = lines,
                        showLabel = settings.showLabels,
                        tall = item.spanY > ROW_UNITS,
                    )
                else -> AppTile(
                    key = item.key,
                    label = label,
                    iconSize = iconSize,
                    showLabel = settings.showLabels,
                    shape = shape,
                )
            }
        }

        is FolderCell -> FolderTile(
            folder = item,
            appsByKey = appsByKey,
            iconSize = iconSize,
            showLabel = settings.showLabels,
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
                .then(
                    when {
                        lines == null -> Modifier.padding(
                            horizontal = 6.dp,
                            vertical = if (item.type == "search") 0.dp else 6.dp,
                        )
                        // The slim search bar is exactly its own height, so only its sides are cut
                        // to the icon lines; insetting it vertically would squeeze the bar.
                        item.type == "search" -> Modifier.padding(horizontal = lines.side)
                        else -> Modifier.toIconLines(lines, item.spanY)
                    }
                ),
        )
    }
}

/**
 * A super icon, as on HyperOS: the app's icon stretched two columns wide or two rows tall, its
 * background filling the whole shape and its logo in the middle, with the name underneath like any
 * other icon. A wide one runs from the left edge of the icon in its first column to the right edge
 * of the icon in its second; a tall one from the top of the icon in its first row to the bottom of
 * the icon in its second. Either way it sits on the same lines as every icon around it.
 */
@Composable
private fun SuperAppTile(
    key: String,
    label: String,
    iconSize: Dp,
    lines: IconLines?,
    showLabel: Boolean,
    tall: Boolean = false,
) {
    val palette = LocalPalette.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val frame = tallIconFrame(cellWidth = maxWidth, rowHeight = maxHeight / 2, iconSize = iconSize)
        val iconHeight = if (tall) frame.height else iconSize
        val drop = if (tall) frame.drop else 0.dp
        Column(
            Modifier
                .fillMaxSize()
                .then(
                    if (lines != null) {
                        Modifier.padding(start = lines.side, end = lines.side, top = lines.top + drop)
                    } else {
                        Modifier.padding(start = 6.dp, end = 6.dp, top = 4.dp + drop, bottom = 4.dp)
                    }
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WideAppIcon(
                key = key,
                label = label,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(iconHeight)
                    .launchOrigin(key)
                    .pressBounce(0.9f),
            )
            if (showLabel) {
                Box(Modifier.height(LABEL_LINE_HEIGHT), contentAlignment = Alignment.BottomCenter) {
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
}

/**
 * How big an icon is, worked out from the cell it sits in rather than chosen: about three quarters
 * of the cell's width, never so tall that it pushes its label out, and kept between sizes that read
 * well. Because it follows the grid, icons, widgets and the gaps between them always stay in
 * proportion — pick a roomier grid and the icons grow with it.
 */
fun homeIconSize(cellWidth: Dp, rowHeight: Dp, showLabels: Boolean): Dp {
    val labelSpace = if (showLabels) 24.dp else 4.dp
    return minOf(
        (cellWidth * 0.75f).coerceIn(54.dp, 72.dp),
        (rowHeight - labelSpace - 8.dp).coerceAtLeast(28.dp),
        (cellWidth - 8.dp).coerceAtLeast(28.dp),
    )
}

/** The size icons are drawn at away from the grid: the drawer, folders, the dock's fallback. */
val STANDARD_ICON_SIZE = 60.dp

/** How tall a tall super icon is, and how far below its row's icon line it starts. */
@Immutable
data class TallIconFrame(val height: Dp, val drop: Dp)

/**
 * A tall super icon is the wide one turned on its side: as tall as a wide one is long (one cell
 * plus an icon), so it keeps the same proportions instead of stretching down two whole rows. It
 * sits centred in the space between the top of its first row's icon and the bottom of its
 * second's. The icon and its drop hint both come from here, so they are always the same size.
 */
fun tallIconFrame(cellWidth: Dp, rowHeight: Dp, iconSize: Dp): TallIconFrame {
    val span = rowHeight + iconSize
    val height = minOf(cellWidth + iconSize, span)
    return TallIconFrame(height = height, drop = (span - height) / 2)
}

/**
 * Where an icon sits inside its cell. Everything else on the page is cut to the same lines: a
 * widget starts at the left edge of the icon in its first column and ends at the right edge of the
 * icon in its last column, and runs from the top of the icons in its first row to the bottom of
 * the icons in its last row. That is what makes apps, super icons and widgets line up.
 */
@Immutable
data class IconLines(val side: Dp, val top: Dp, val bottom: Dp)

fun iconLines(cellWidth: Dp, rowHeight: Dp, iconSize: Dp, showLabels: Boolean): IconLines {
    val label = if (showLabels) LABEL_LINE_HEIGHT else 0.dp
    val top = ((rowHeight - iconSize - label) / 2).coerceAtLeast(0.dp)
    return IconLines(
        side = ((cellWidth - iconSize) / 2).coerceAtLeast(0.dp),
        top = top,
        bottom = (rowHeight - top - iconSize).coerceAtLeast(0.dp),
    )
}

/** Insets something spanning [spanY] row units to the icon lines of the cells it covers. */
fun Modifier.toIconLines(lines: IconLines, spanY: Int): Modifier =
    if (spanY >= ROW_UNITS) {
        padding(start = lines.side, top = lines.top, end = lines.side, bottom = lines.bottom)
    } else {
        // Shorter than a row, like the search bar: only the sides can follow the icons.
        padding(horizontal = lines.side)
    }

@Composable
fun AppTile(
    key: String,
    label: String,
    iconSize: androidx.compose.ui.unit.Dp,
    showLabel: Boolean,
    shape: androidx.compose.ui.graphics.Shape = ZenithShapes.icon,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppIcon(key = key, label = label, size = iconSize, shape = shape, modifier = Modifier.launchOrigin(key).pressBounce())
        if (showLabel) {
            // Always exactly LABEL_LINE_HEIGHT tall, so every icon's top edge sits on one line and
            // widgets beside it can be cut to that line.
            Box(Modifier.height(LABEL_LINE_HEIGHT), contentAlignment = Alignment.BottomCenter) {
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
) {
    val palette = LocalPalette.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(iconSize)
                .pressBounce()
                .glass(ZenithShapes.icon, strength = 1.2f)
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
            Box(Modifier.height(LABEL_LINE_HEIGHT), contentAlignment = Alignment.BottomCenter) {
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
 * The handles you drag to make a widget bigger or smaller. Everything snaps to whole grid
 * cells, and the view model refuses any size that would overlap a neighbour.
 */
@Composable
private fun BoxScope.ResizeFrame(
    item: WidgetCell,
    cellWidthPx: Float,
    cellHeightPx: Float,
    lines: IconLines,
    onResize: (String, Int, Int) -> Boolean,
) {
    val palette = LocalPalette.current
    val haptics = LocalHapticFeedback.current

    Box(
        Modifier
            .matchParentSize()
            .toIconLines(lines, item.spanY)
            .appearIn(fromScale = 0.94f, riseFrom = 0f)
            .border(2.dp, palette.accent, ZenithShapes.card)
    )

    // The spans are read live, and the target size is always measured from where the drag
    // started, so the widget follows the handle instead of drifting a step at a time.
    val live by rememberUpdatedState(item)

    @Composable
    fun handle(alignment: Alignment, horizontal: Boolean, vertical: Boolean) {
        Box(
            Modifier
                .align(alignment)
                .padding(2.dp)
                .size(38.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(palette.accent)
                .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(percent = 50))
                .pointerInput(item.id, cellWidthPx, cellHeightPx) {
                    var startSpanX = live.spanX
                    var startSpanY = live.spanY
                    var travelX = 0f
                    var travelY = 0f
                    detectDragGestures(
                        onDragStart = {
                            startSpanX = live.spanX
                            startSpanY = live.spanY
                            travelX = 0f
                            travelY = 0f
                        },
                        onDrag = { change, delta ->
                            change.consume()
                            if (horizontal) travelX += delta.x
                            if (vertical) travelY += delta.y
                            val targetX = startSpanX + (travelX / cellWidthPx).roundToInt()
                            val targetY = startSpanY + (travelY / cellHeightPx).roundToInt()
                            if (targetX != live.spanX || targetY != live.spanY) {
                                if (onResize(live.id, targetX, targetY)) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        },
                    )
                }
        )
    }

    handle(Alignment.CenterEnd, horizontal = true, vertical = false)
    handle(Alignment.BottomCenter, horizontal = false, vertical = true)
    handle(Alignment.BottomEnd, horizontal = true, vertical = true)
}

/** The ghost that follows your finger while dragging. */
@Composable
fun DragGhost(
    drag: HomeDragState,
    settings: ZenithSettings,
    appsByKey: Map<String, AppInfo>,
) {
    val item = drag.item ?: return
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
            iconSize = drag.iconSize,
            lines = drag.lines,
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
    /** An app was lifted off the dock and let go at this point on screen. */
    onDragOut: (key: String, pointerRoot: Offset) -> Unit,
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
                .glass(ZenithShapes.panel, strength = 1.15f)
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
                                        liftedNow() -> currentOnDragOut(key, grabRoot + dragOffset)
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

/** Height of a single 11sp app label line, plus its spacer — the hint lines up with it. */
private val LABEL_LINE_HEIGHT = 20.dp

val TransparentColor: Color = Color.Transparent
