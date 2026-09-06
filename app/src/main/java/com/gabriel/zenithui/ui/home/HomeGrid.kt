package com.gabriel.zenithui.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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

    /** Cell under the centre of the dragged item, or null when it is off the grid. */
    fun cellUnderItem(columns: Int, rows: Int, rowStep: Int = 1): Pair<Int, Int>? {
        val current = item ?: return null
        if (cellWidth <= 0f || cellHeight <= 0f) return null
        // Where the item is actually being drawn, rounded to the closest cell.
        val topLeft = pointer - grab
        val col = ((topLeft.x - gridOrigin.x) / cellWidth).roundToInt()
        val rawRow = ((topLeft.y - gridOrigin.y) / cellHeight).roundToInt()
        // Icons stay on tidy boundaries; widgets may sit on any unit.
        val row = if (rowStep > 1) (rawRow.toFloat() / rowStep).roundToInt() * rowStep else rawRow
        if (col < 0 || row < 0 || col + current.spanX > columns || row + current.spanY > rows) return null
        return col to row
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
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val density = LocalDensity.current
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
        // An icon never grows so large that it pushes its label out of the cell.
        val labelSpace = if (settings.showLabels) 24.dp else 4.dp
        val rowHeightDp = cellHeightDp * ROW_UNITS
        val iconSize = minOf(
            settings.iconSize.dp,
            (rowHeightDp - labelSpace - 8.dp).coerceAtLeast(28.dp),
            (cellWidthDp - 8.dp).coerceAtLeast(28.dp),
        )
        if (active) drag.iconSize = iconSize

        // Drop hint for the cell the dragged item would land in.
        val dragged = drag.item
        if (dragged != null && active) {
            val cell = drag.cellUnderItem(
                columns = settings.columns,
                rows = settings.rows * ROW_UNITS,
                rowStep = if (dragged is WidgetCell) 1 else LayoutBuilder.ICON_ROW_STEP,
            )
            if (cell != null && drag.hoverItemId == null) {
                val hintOffset by animateIntOffsetAsState(
                    targetValue = IntOffset(
                        (cell.first * cellWidthPx).roundToInt(),
                        (cell.second * cellHeightPx).roundToInt(),
                    ),
                    animationSpec = Motion.cell,
                    label = "dropHint",
                )

                Box(
                    Modifier
                        .offset { hintOffset }
                        .requiredSize(cellWidthDp * dragged.spanX, cellHeightDp * dragged.spanY),
                    contentAlignment = Alignment.Center,
                ) {
                    if (dragged is WidgetCell) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(6.dp)
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
            // A long press also ends in a pointer-up, which the tap detector would otherwise
            // report as a tap — opening an app or the clock behind the menu that just appeared.
            var longPressHandled by remember { mutableStateOf(false) }
            val hovered = drag.hoverItemId == item.id
            val scale by animateFloatAsState(if (hovered) 1.18f else 1f, Motion.bouncy(), label = "hover")
            // Glide when the item changes cell, but snap when the grid itself is re-measured.
            val target = IntOffset(
                (item.col * cellWidthPx).roundToInt(),
                (item.row * cellHeightPx).roundToInt(),
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
                    .appearIn(delayMillis = staggerDelay(index))
                    .graphicsLayer {
                        // The real item stays put, in full colour, until you let go — it is the
                        // grey copy under your finger that moves.
                        scaleX = scale
                        scaleY = scale
                    },
                contentAlignment = Alignment.Center,
            ) {
                // A slim widget only listens inside its own bar, so the empty part of the cell
                // still belongs to the home screen.
                val slim = item is WidgetCell && item.type == "search"
                Box(
                    modifier = (if (slim) Modifier.fillMaxWidth().height(SEARCH_BAR_HEIGHT) else Modifier.fillMaxSize())
                        .onGloballyPositioned { contentRoot = it.positionInRoot() }
                        .pointerInput(item.id, cellWidthPx, cellHeightPx, settings.columns) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { local ->
                                    travelled = 0f
                                    longPressHandled = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    drag.begin(item, contentRoot + local, cellRoot)
                                },
                                onDrag = { change, delta ->
                                    change.consume()
                                    travelled += delta.getDistance()
                                    drag.pointer += delta
                                    drag.hoverCandidateId =
                                        hoverTarget(drag, items, item, settings.columns, settings.rows * ROW_UNITS)
                                },
                                onDragEnd = {
                                    if (travelled < 24f) {
                                        drag.clear()
                                        onItemMenu(item)
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
                        onSearch = onSearch,
                        onNoteChange = onNoteChange,
                        onAmbient = onAmbient,
                    )

                    if (item.id == resizingId && item is WidgetCell) {
                        ResizeFrame(
                            item = item,
                            cellWidthPx = cellWidthPx,
                            cellHeightPx = cellHeightPx,
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
    if (dragged !is AppCell) return null
    val cell = drag.cellUnderPointer(columns, rows) ?: return null
    val target = items.firstOrNull {
        it.id != dragged.id &&
            cell.first >= it.col && cell.first < it.col + it.spanX &&
            cell.second >= it.row && cell.second < it.row + it.spanY
    } ?: return null
    return if (target is AppCell || target is FolderCell) target.id else null
}

@Composable
fun HomeItemContent(
    item: HomeItem,
    settings: ZenithSettings,
    appsByKey: Map<String, AppInfo>,
    iconSize: androidx.compose.ui.unit.Dp = settings.iconSize.dp,
    onSearch: () -> Unit,
    onNoteChange: (String) -> Unit,
    onAmbient: () -> Unit = {},
) {
    when (item) {
        is AppCell -> {
            val app = appsByKey[item.key]
            AppTile(
                key = item.key,
                label = app?.label ?: item.packageName.substringAfterLast('.'),
                iconSize = iconSize,
                showLabel = settings.showLabels,
                shape = ZenithShapes.forIconShape(settings.iconShape),
            )
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
            onNoteChange = onNoteChange,
            onSearch = onSearch,
            onAmbient = onAmbient,
            modifier = Modifier
                .fillMaxSize()
                // The slim search bar is already exactly its own height: insetting it vertically
                // would squeeze the bar and leave its round button no room inside the curve.
                .padding(
                    horizontal = 6.dp,
                    vertical = if (item.type == "search") 0.dp else 6.dp,
                ),
        )
    }
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
        AppIcon(key = key, label = label, size = iconSize, shape = shape, modifier = Modifier.pressBounce())
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
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
            Spacer(Modifier.height(6.dp))
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

/**
 * The handles you drag to make a widget bigger or smaller. Everything snaps to whole grid
 * cells, and the view model refuses any size that would overlap a neighbour.
 */
@Composable
private fun BoxScope.ResizeFrame(
    item: WidgetCell,
    cellWidthPx: Float,
    cellHeightPx: Float,
    onResize: (String, Int, Int) -> Boolean,
) {
    val palette = LocalPalette.current
    val haptics = LocalHapticFeedback.current

    Box(
        Modifier
            .matchParentSize()
            .padding(4.dp)
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
    LaunchedEffect(item.id) { lift.animateTo(1.14f, spring(dampingRatio = 0.38f, stiffness = 260f)) }
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
            onSearch = {},
            onNoteChange = {},
        )
    }
}

@Composable
fun DockBar(
    keys: List<String>,
    appsByKey: Map<String, AppInfo>,
    settings: ZenithSettings,
    onOpen: (String) -> Unit,
    onLongPress: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glass(ZenithShapes.panel, strength = 1.15f)
            .padding(vertical = 12.dp, horizontal = 10.dp),
        horizontalArrangement = if (keys.isEmpty()) Arrangement.Center else Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (keys.isEmpty()) {
            Text(
                text = "Long press an app to pin it here",
                color = palette.onGlassMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 10.dp),
            )
        }
        keys.forEachIndexed { index, key ->
            Box(
                Modifier
                    .appearIn(delayMillis = 90 + staggerDelay(index), riseFrom = 26f)
                    .pointerInput(key) {
                    detectTapGestures(
                        onTap = { onOpen(key) },
                        onLongPress = { onLongPress(key) },
                    )
                },
            ) {
                AppIcon(
                    key = key,
                    label = appsByKey[key]?.label ?: "",
                    size = minOf(settings.iconSize.dp, 64.dp),
                    shape = ZenithShapes.forIconShape(settings.iconShape),
                    modifier = Modifier.pressBounce(),
                )
            }
        }
    }
}

/** Height of a single 11sp app label line, plus its spacer — the hint lines up with it. */
private val LABEL_LINE_HEIGHT = 20.dp

val TransparentColor: Color = Color.Transparent
