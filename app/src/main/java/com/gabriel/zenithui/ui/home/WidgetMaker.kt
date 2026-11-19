package com.gabriel.zenithui.ui.home

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FlipToBack
import androidx.compose.material.icons.rounded.FlipToFront
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.FormatAlignLeft
import androidx.compose.material.icons.rounded.FormatAlignRight
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Interests
import androidx.compose.material.icons.rounded.Redo
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.gabriel.zenithui.data.Box as PieceBox
import com.gabriel.zenithui.data.HomeGeometry
import com.gabriel.zenithui.data.LayoutBuilder
import com.gabriel.zenithui.data.MadeBack
import com.gabriel.zenithui.data.MadeStarters
import com.gabriel.zenithui.data.MadeWidget
import com.gabriel.zenithui.data.MadeWidgets
import com.gabriel.zenithui.data.MakerHistory
import com.gabriel.zenithui.data.MakerSnap
import com.gabriel.zenithui.data.Piece
import com.gabriel.zenithui.data.PieceAlign
import com.gabriel.zenithui.data.PieceKinds
import com.gabriel.zenithui.data.PiecePlace
import com.gabriel.zenithui.data.PieceStyles
import com.gabriel.zenithui.data.Pieces
import com.gabriel.zenithui.data.ROW_UNITS
import com.gabriel.zenithui.data.Snapped
import com.gabriel.zenithui.data.Starter
import com.gabriel.zenithui.data.WidgetSize
import com.gabriel.zenithui.data.WidgetSizes
import com.gabriel.zenithui.data.Widgets
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.FullPage
import com.gabriel.zenithui.ui.common.GlassButton
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.home.widgets.INSET
import com.gabriel.zenithui.ui.home.widgets.LocalInMaker
import com.gabriel.zenithui.ui.home.widgets.MadeWidgetView
import com.gabriel.zenithui.ui.home.widgets.symbolIcon
import com.gabriel.zenithui.ui.onboarding.Segmented
import com.gabriel.zenithui.ui.settings.ChoiceChips
import com.gabriel.zenithui.ui.settings.PillSlider
import com.gabriel.zenithui.ui.settings.Toggle
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import com.gabriel.zenithui.ui.theme.ZenithFonts
import com.gabriel.zenithui.ui.theme.corners
import com.gabriel.zenithui.ui.theme.glass
import com.gabriel.zenithui.ui.theme.inkOnTint
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/*
 * The widget maker: your own widget, from scratch. A page of its own over the home screen, as
 * Settings and the gallery are, in four parts from the top: the stage, where the widget stands on
 * your wallpaper and its pieces are picked, moved and pulled bigger with a finger; undo, redo and
 * its size; the panel, which adds pieces and changes the one picked; and the button that puts it
 * on the home screen. See specs/2026-11-18-widget-maker-design.md.
 */

/** What the maker is opened on: a widget, new or kept, and where it was opened from. */
@Immutable
data class MakerOpen(
    val design: MadeWidget,
    /** Made just now: kept when the maker closes only if something was put on it. */
    val isNew: Boolean,
    /** Opened from the widget on the page, which is there already: it ends with Done. */
    val fromPage: Boolean,
    /** That widget on the page, when it was opened from one. */
    val itemId: String? = null,
    /** Tells one opening from the next, so each starts afresh. */
    val session: Long = System.nanoTime(),
) {
    companion object {
        /** A new widget, called what [designs] leave free, at the size a widget is ordinarily put on the page at. */
        fun fresh(designs: List<MadeWidget>, columns: Int, rows: Int): MakerOpen {
            val (spanX, spanY) = Widgets.defaultSpan(MadeWidgets.spec(""), columns, rows)
            val design = MadeWidget(
                id = MadeWidgets.newId(),
                name = MadeWidgets.nextName(designs),
                spanX = spanX,
                rows = (spanY / ROW_UNITS).coerceAtLeast(1),
            )
            return MakerOpen(design, isNew = true, fromPage = false)
        }
    }
}

/**
 * The maker, open while [open] is not null. Closing it in any way keeps what was made under Your
 * widgets ([onKeep]), apart from a new widget with nothing on it. [onAddToHome] keeps it and puts
 * it on the home screen, false when no page has room; [onDelete] deletes it and every copy of it.
 * [onClose] says whether it was put on the home screen.
 */
@Composable
fun WidgetMaker(
    open: MakerOpen?,
    settings: ZenithSettings,
    geometry: HomeGeometry?,
    onKeep: (MadeWidget) -> Unit,
    onAddToHome: (MadeWidget) -> Boolean,
    onDelete: (MadeWidget) -> Unit,
    onKeepPhoto: suspend (Uri) -> String?,
    onChoosePlace: () -> Unit,
    onClose: (added: Boolean) -> Unit,
) {
    // The last one opened stays while the page goes away, so it leaves showing what it showed.
    val last = remember { arrayOfNulls<MakerOpen>(1) }
    if (open != null) last[0] = open
    val opening = last[0] ?: return
    key(opening.session) {
        MakerPage(
            opening = opening,
            visible = open != null,
            settings = settings,
            geometry = geometry,
            onKeep = onKeep,
            onAddToHome = onAddToHome,
            onDelete = onDelete,
            onKeepPhoto = onKeepPhoto,
            onChoosePlace = onChoosePlace,
            onClose = onClose,
        )
    }
}

/** Stands in for the piece a photo is wanted for while the picker is up, when it is to be a new one. */
private const val NEW_PHOTO = "\u0000new"

@Composable
private fun MakerPage(
    opening: MakerOpen,
    visible: Boolean,
    settings: ZenithSettings,
    geometry: HomeGeometry?,
    onKeep: (MadeWidget) -> Unit,
    onAddToHome: (MadeWidget) -> Boolean,
    onDelete: (MadeWidget) -> Unit,
    onKeepPhoto: suspend (Uri) -> String?,
    onChoosePlace: () -> Unit,
    onClose: (added: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tick = rememberTick()
    var history by remember { mutableStateOf(MakerHistory(opening.design)) }
    val design = history.now
    var picked by remember { mutableStateOf<String?>(null) }
    val piece = design.pieces.firstOrNull { it.id == picked }
    var boxes by remember { mutableStateOf<Map<String, PieceBox>>(emptyMap()) }
    var widgetWidth by remember { mutableStateOf(0f) }
    var askingDelete by remember { mutableStateOf(false) }
    // Whether this widget is under Your widgets yet: kept before, or kept while it was being made.
    var kept by remember { mutableStateOf(!opening.isNew) }
    var finished by remember { mutableStateOf(false) }

    fun change(next: MadeWidget, key: String? = null) {
        history = history.changed(next, key)
    }

    fun changePiece(next: Piece, key: String? = null) = change(Pieces.changed(history.now, next), key)

    fun settle() {
        history = history.settled()
    }

    /** Keeps the widget as it is now, if there is anything to keep. */
    fun keep() {
        val now = history.now
        if (!MadeWidgets.worthKeeping(now) && !kept) return
        if (kept && now == opening.design && !opening.isNew) return
        onKeep(now)
        kept = true
    }

    fun finish(added: Boolean) {
        if (finished) return
        finished = true
        val now = history.now
        when {
            added -> Unit
            MadeWidgets.worthKeeping(now) || !opening.isNew -> keep()
            // A new one emptied again after it was kept on the way: not worth keeping after all.
            kept -> onDelete(now)
        }
        onClose(added)
    }

    // The work is kept whenever Zenith goes out of sight, so nothing made is lost to a phone call,
    // and when the page is put away from outside it, by the home button, as if closed by hand.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { if (visible && !finished) keep() }
    LaunchedEffect(visible) { if (!visible && !finished) finish(added = false) }
    BackHandler(enabled = visible) {
        when {
            picked != null -> picked = null
            askingDelete -> askingDelete = false
            else -> finish(added = false)
        }
    }

    var photoFor by remember { mutableStateOf<String?>(null) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val forId = photoFor
        photoFor = null
        if (uri == null || forId == null) return@rememberLauncherForActivityResult
        scope.launch {
            val name = onKeepPhoto(uri)
            if (name == null) {
                Toast.makeText(context, "That photo could not be read", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val now = history.now
            if (forId == NEW_PHOTO) {
                val added = Pieces.new(PieceKinds.PHOTO, LayoutBuilder.newId()).copy(text = name)
                change(Pieces.added(now, added))
                picked = added.id
            } else {
                now.pieces.firstOrNull { it.id == forId }?.let { change(Pieces.changed(now, it.copy(text = name))) }
            }
        }
    }

    fun choosePhoto(forId: String) {
        photoFor = forId
        runCatching { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            .onFailure { Toast.makeText(context, "There is no way to choose a photo on this phone", Toast.LENGTH_SHORT).show() }
    }

    fun add(kind: String) {
        if (!Pieces.canAdd(history.now)) {
            Toast.makeText(context, "A widget holds ${MadeWidgets.MOST_PIECES} pieces at most", Toast.LENGTH_SHORT).show()
            return
        }
        if (kind == PieceKinds.PHOTO) {
            choosePhoto(NEW_PHOTO)
            return
        }
        val added = Pieces.new(kind, LayoutBuilder.newId())
        change(Pieces.added(history.now, added))
        picked = added.id
        tick()
    }

    fun start(starter: Starter) {
        val made = starter.madeAs(design.id, design.name)
        change(
            made.copy(
                spanX = made.spanX.coerceAtMost(settings.columns.coerceAtLeast(1)),
                rows = made.rows.coerceAtMost(settings.rows.coerceAtLeast(1)),
            )
        )
        picked = null
        tick()
    }

    val real = realSize(geometry, WidgetSize("", design.spanX, design.rows), settings.columns)
    FullPage(
        visible = visible,
        covered = false,
        title = design.name.ifBlank { "Your widget" },
        canGoBack = false,
        titleShown = { 1f },
        onBack = {},
        onClose = { finish(added = false) },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val stageHeight = (maxHeight * 0.38f).coerceIn(200.dp, 330.dp)
            Column(Modifier.fillMaxSize()) {
                Stage(
                    design = design,
                    picked = picked,
                    real = real,
                    history = history,
                    onUndo = {
                        history = history.undone()
                        tick()
                    },
                    onRedo = {
                        history = history.redone()
                        tick()
                    },
                    onPick = { picked = it },
                    onChange = { next, key -> change(next, key) },
                    onSettle = ::settle,
                    onBoxes = { laid, width ->
                        boxes = laid
                        widgetWidth = width
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(stageHeight)
                        .padding(horizontal = 16.dp),
                )
                SizeRow(
                    design = design,
                    columns = settings.columns,
                    rows = settings.rows,
                    onSize = { spanX, rows -> change(design.copy(spanX = spanX, rows = rows)) },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp),
                )
                val scroll = rememberScrollState()
                LaunchedEffect(picked) { scroll.scrollTo(0) }
                val reduced = LocalMotionLevel.current.reduced
                AnimatedContent(
                    targetState = picked != null && piece != null,
                    transitionSpec = {
                        val ms = if (reduced) Motion.REDUCED_MS else 180
                        fadeIn(tween(ms)).togetherWith(fadeOut(tween(ms)))
                    },
                    modifier = Modifier.weight(1f),
                    label = "makerPanel",
                ) { onPiece ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scroll)
                            .padding(horizontal = 16.dp),
                    ) {
                        val shownPiece = design.pieces.firstOrNull { it.id == picked }
                        if (onPiece && shownPiece != null) {
                            PiecePanel(
                                piece = shownPiece,
                                design = design,
                                settings = settings,
                                onChange = { next, key -> changePiece(next, key) },
                                onSettle = ::settle,
                                onLineUp = { to ->
                                    val box = boxes[shownPiece.id]
                                    val x = if (box != null && widgetWidth > 0f) {
                                        (PiecePlace.repoint(shownPiece.x * widgetWidth, box.width, shownPiece.align, to) / widgetWidth).coerceIn(0f, 1f)
                                    } else {
                                        shownPiece.x
                                    }
                                    changePiece(shownPiece.copy(align = to, x = x))
                                },
                                onDuplicate = {
                                    val copy = LayoutBuilder.newId()
                                    change(Pieces.duplicated(design, shownPiece.id, copy))
                                    picked = copy
                                },
                                onForward = { change(Pieces.forward(design, shownPiece.id)) },
                                onBackward = { change(Pieces.backward(design, shownPiece.id)) },
                                onRemove = {
                                    change(Pieces.removed(design, shownPiece.id))
                                    picked = null
                                },
                                onDone = { picked = null },
                                onChoosePhoto = { choosePhoto(shownPiece.id) },
                                onChoosePlace = onChoosePlace,
                            )
                        } else {
                            WholePanel(
                                design = design,
                                settings = settings,
                                geometry = geometry,
                                canDelete = kept || MadeWidgets.worthKeeping(design),
                                askingDelete = askingDelete,
                                onAdd = ::add,
                                onStart = ::start,
                                onChange = { next, key -> change(next, key) },
                                onSettle = ::settle,
                                onPick = {
                                    picked = it
                                    tick()
                                },
                                onAskDelete = { askingDelete = it },
                                onDelete = {
                                    askingDelete = false
                                    if (kept) onDelete(history.now)
                                    finished = true
                                    onClose(false)
                                },
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
                val empty = design.pieces.isEmpty()
                GlassButton(
                    text = when {
                        opening.fromPage -> "Done"
                        empty -> "Add something to it first"
                        else -> "Add to Home Screen"
                    },
                    filled = true,
                    enabled = opening.fromPage || !empty,
                    onClick = {
                        if (opening.fromPage) {
                            finish(added = false)
                        } else if (onAddToHome(history.now)) {
                            kept = true
                            finished = true
                            onClose(true)
                        } else {
                            Toast.makeText(context, "No page has room for it", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

// ------------------------------------------------------------------------------- the stage

/** As much larger than its real size as a widget is drawn on the stage, so a small one's pieces can be reached. */
private const val MAX_ZOOM = 2f

/** How near a line a piece must come to line up on it. */
private val SNAP_WITHIN = 6.dp

/** The least a piece can be pressed at, however small it is drawn. */
private val TOUCH = 44.dp

/** How far from its corner the corner of a picked piece can be taken hold of. */
private val CORNER_REACH = 26.dp

/** How far out from a picked piece its outline is drawn. */
private val OUTLINE_OUT = 4.dp

/** The lines a piece lines up on: a yellow that shows on the wallpaper and on any card's colour. */
private val GUIDE = Color(0xFFFFD60A)

/**
 * The widget on a window onto your wallpaper, drawn by the home screen's own code, at its real size
 * or larger, up to [MAX_ZOOM]: larger by being drawn at a larger density, so everything on it, its
 * corners and the room round its edges too, is the same design, bigger, and its glass still finds
 * the wallpaper where it really is. A tap picks a piece; a drag moves it, lining it up as it goes;
 * its corner, or a pinch, makes it bigger or smaller; a tap on nothing puts it down.
 */
@Composable
private fun Stage(
    design: MadeWidget,
    picked: String?,
    real: DpSize,
    history: MakerHistory,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onPick: (String?) -> Unit,
    onChange: (MadeWidget, String) -> Unit,
    onSettle: () -> Unit,
    onBoxes: (Map<String, PieceBox>, Float) -> Unit,
    modifier: Modifier,
) {
    val density = LocalDensity.current
    val palette = LocalPalette.current
    val tick = rememberTick()
    var boxes by remember { mutableStateOf<Map<String, PieceBox>>(emptyMap()) }
    var guides by remember { mutableStateOf<Snapped?>(null) }
    val latestDesign = rememberUpdatedState(design)
    val latestPicked = rememberUpdatedState(picked)
    val latestBoxes = rememberUpdatedState(boxes)
    val pick by rememberUpdatedState(onPick)
    val changeTo by rememberUpdatedState(onChange)
    val settleNow by rememberUpdatedState(onSettle)
    BoxWithConstraints(modifier.wallpaperWell(corners(26.dp))) {
        // The foot of the stage holds Undo and Redo and says what size it is shown at, so nothing
        // ever stands over the widget, however wide it is.
        val caption = FOOT
        val pad = 18.dp
        val zoom = min(
            MAX_ZOOM,
            min((maxWidth - pad * 2) / real.width, (maxHeight - pad * 2 - caption) / real.height),
        ).coerceAtLeast(0.1f)
        val shown = DpSize(real.width * zoom, real.height * zoom)
        val left = (maxWidth - shown.width) / 2
        val top = (maxHeight - caption - shown.height) / 2
        val origin = with(density) { Offset(left.toPx(), top.toPx()) }
        val width = with(density) { shown.width.toPx() }
        val height = with(density) { shown.height.toPx() }
        val margin = with(density) { INSET.toPx() } * zoom
        val within = with(density) { SNAP_WITHIN.toPx() }

        Box(
            Modifier
                .offset(left, top)
                .size(shown)
                .semantics { contentDescription = "${design.name.ifBlank { "Your widget" }}, as it will look" },
        ) {
            CompositionLocalProvider(
                LocalDensity provides Density(density.density * zoom, density.fontScale),
                LocalInMaker provides true,
            ) {
                MadeWidgetView(
                    design,
                    Modifier.fillMaxSize(),
                    interactive = false,
                    onLaidOut = { laid ->
                        if (laid != boxes) {
                            boxes = laid
                            onBoxes(laid, width)
                        }
                    },
                )
            }
        }

        // What is picked, and the lines it lines up on: each drawn over a soft dark edge, so it
        // shows on any card, any wallpaper and any colour of words.
        Canvas(Modifier.fillMaxSize()) {
            val halo = Color.Black.copy(alpha = 0.35f)
            guides?.let { lines ->
                val stroke = 1.dp.toPx()
                val past = 8.dp.toPx()
                fun line(from: Offset, to: Offset) {
                    drawLine(halo, from, to, stroke * 3f)
                    drawLine(GUIDE, from, to, stroke)
                }
                lines.lineX?.let { x -> line(Offset(origin.x + x, origin.y - past), Offset(origin.x + x, origin.y + height + past)) }
                lines.lineY?.let { y -> line(Offset(origin.x - past, origin.y + y), Offset(origin.x + width + past, origin.y + y)) }
            }
            val chosen = design.pieces.firstOrNull { it.id == picked } ?: return@Canvas
            val box = boxes[chosen.id] ?: return@Canvas
            val out = OUTLINE_OUT.toPx()
            val topLeft = Offset(origin.x + box.left - out, origin.y + box.top - out)
            val size = Size(box.width + out * 2f, box.height + out * 2f)
            val round = CornerRadius(8.dp.toPx())
            drawRoundRect(halo, topLeft, size, round, style = Stroke(4.5f.dp.toPx()))
            drawRoundRect(Color.White, topLeft, size, round, style = Stroke(2.dp.toPx()))
            // The corner to pull: a white disc, ringed in the accent.
            val corner = origin + cornerOf(box, chosen.align, out)
            drawCircle(halo, 10.dp.toPx(), corner)
            drawCircle(Color.White, 8.dp.toPx(), corner)
            drawCircle(palette.accent, 8.dp.toPx(), corner, style = Stroke(2.dp.toPx()))
        }

        // Every touch on the stage, the widget's own and the room round it.
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(origin, width, height, margin) {
                    val touch = TOUCH.toPx()
                    val reach = CORNER_REACH.toPx()
                    val out = OUTLINE_OUT.toPx()
                    val slop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val at = down.position - origin
                        val now = latestDesign.value
                        val laid = latestBoxes.value
                        val chosen = now.pieces.firstOrNull { it.id == latestPicked.value }
                        val chosenBox = chosen?.let { laid[it.id] }
                        val onCorner = chosen != null && chosenBox != null &&
                            (at - cornerOf(chosenBox, chosen.align, out)).getDistance() <= reach
                        val hit = if (onCorner) chosen else now.pieces.asReversed().firstOrNull { candidate ->
                            laid[candidate.id]?.let { touchable(it, touch, at) } == true
                        }
                        if (hit == null) {
                            // A tap on nothing puts the piece down.
                            if (waitForUpOrCancellation() != null && latestPicked.value != null) pick(null)
                            return@awaitEachGesture
                        }
                        down.consume()
                        if (hit.id != latestPicked.value) {
                            pick(hit.id)
                            tick()
                        }
                        val startBox = laid[hit.id] ?: return@awaitEachGesture
                        val others = laid.filterKeys { it != hit.id }.values.toList()
                        var travel = Offset.Zero
                        var moving = false
                        var pinchFrom = 0f
                        var pinchPiece: Piece = hit
                        var lastLines: Pair<Float?, Float?>? = null
                        while (true) {
                            val event = awaitPointerEvent()
                            val fingers = event.changes.filter { it.pressed }
                            if (fingers.isEmpty()) break
                            if (fingers.size >= 2) {
                                // Two fingers: bigger or smaller, by how far apart they have gone.
                                val span = (fingers[0].position - fingers[1].position).getDistance()
                                if (pinchFrom <= 0f) {
                                    pinchFrom = span
                                    pinchPiece = latestDesign.value.pieces.firstOrNull { it.id == hit.id } ?: hit
                                    guides = null
                                } else if (span > 0f) {
                                    changeTo(Pieces.changed(latestDesign.value, Pieces.scaled(pinchPiece, span / pinchFrom)), "pinch ${hit.id}")
                                }
                                fingers.forEach { it.consume() }
                                moving = true
                                continue
                            }
                            // The finger left after a pinch does nothing until it is lifted.
                            if (pinchFrom > 0f) {
                                fingers.forEach { it.consume() }
                                continue
                            }
                            val finger = fingers.first()
                            travel += finger.positionChange()
                            if (!moving && travel.getDistance() < slop) continue
                            moving = true
                            finger.consume()
                            val current = latestDesign.value.pieces.firstOrNull { it.id == hit.id } ?: break
                            if (onCorner) {
                                val (dw, dh) = PiecePlace.stretch(hit.align, travel.x, travel.y)
                                val next = if (PieceKinds.isBox(hit.kind)) {
                                    current.copy(
                                        w = ((startBox.width + dw) / width).coerceIn(Pieces.THINNEST, 1f),
                                        h = ((startBox.height + dh) / height).coerceIn(Pieces.THINNEST, 1f),
                                    )
                                } else {
                                    current.copy(size = (hit.size * PiecePlace.growth(startBox.width, startBox.height, dw, dh)).coerceIn(Pieces.SMALLEST, Pieces.LARGEST))
                                }
                                changeTo(Pieces.changed(latestDesign.value, next), "corner ${hit.id}")
                            } else {
                                val raw = startBox.moved(travel.x, travel.y)
                                val snap = MakerSnap.snap(raw, others, width, height, margin, within)
                                val box = raw.moved(snap.dx, snap.dy)
                                val x = PiecePlace.pointX(box.left, box.width, current.align, width) / width
                                val y = PiecePlace.pointY(box.top, box.height, height) / height
                                changeTo(Pieces.changed(latestDesign.value, current.copy(x = x, y = y)), "move ${hit.id}")
                                guides = snap.takeIf { it.lined }
                                // A tick as it comes onto a line: it has lined up.
                                val lines = snap.lineX to snap.lineY
                                if (snap.lined && lines != lastLines) tick()
                                lastLines = lines
                            }
                        }
                        guides = null
                        settleNow()
                    }
                },
        )

        Row(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 10.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RoundButton(Icons.Rounded.Undo, "Undo", enabled = history.canUndo, onClick = onUndo)
            RoundButton(Icons.Rounded.Redo, "Redo", enabled = history.canRedo, onClick = onRedo)
        }
        Text(
            when {
                design.pieces.isEmpty() -> "Add something below"
                zoom > 1.05f -> "Shown ${"%.1f".format(java.util.Locale.ROOT, zoom).removeSuffix(".0")}× bigger"
                zoom < 0.95f -> "Shown smaller to fit"
                else -> "Its real size"
            },
            // On a pill of glass, as the buttons beside it are, so it reads on any wallpaper.
            color = palette.onGlass,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 12.dp)
                .glass(RoundedCornerShape(50), strength = 0.9f)
                .padding(horizontal = 11.dp, vertical = 6.dp),
        )
    }
}

/** How tall the foot of the stage is, under the widget: Undo and Redo, and what size it is shown at. */
private val FOOT = 46.dp

/** Where a picked piece's corner is, to be pulled: bottom right, or bottom left for one that ends at its point. */
private fun cornerOf(box: PieceBox, align: PieceAlign, out: Float): Offset =
    Offset(if (PiecePlace.cornerOnLeft(align)) box.left - out else box.right + out, box.bottom + out)

/** Whether [at] is on [box], made up to [touch] across and down where it is smaller than a finger. */
private fun touchable(box: PieceBox, touch: Float, at: Offset): Boolean {
    val growX = ((touch - box.width) / 2f).coerceAtLeast(0f)
    val growY = ((touch - box.height) / 2f).coerceAtLeast(0f)
    return at.x in (box.left - growX)..(box.right + growX) && at.y in (box.top - growY)..(box.bottom + growY)
}

/** A small round button of glass on the stage. */
@Composable
private fun RoundButton(icon: ImageVector, says: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(38.dp)
            .pressBounce(0.9f, enabled = enabled)
            .glass(CircleShape, strength = 0.9f, interactive = enabled)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = says) { onClick() }
            .semantics { contentDescription = says },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = LocalPalette.current.onGlass, modifier = Modifier.size(20.dp).alpha(if (enabled) 1f else 0.35f))
    }
}

// ------------------------------------------------------------------------------- its size

/** The gallery's sizes, each drawn as the room it takes on the page. The one the widget is at is lit. */
@Composable
private fun SizeRow(design: MadeWidget, columns: Int, rows: Int, onSize: (Int, Int) -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    val sizes = remember(columns, rows) { WidgetSizes.options(MadeWidgets.spec(""), columns, rows) }
    val tallest = sizes.maxOf { it.rows }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        sizes.forEach { size ->
            val on = size.spanX == design.spanX && size.rows == design.rows
            Column(
                Modifier
                    .weight(1f)
                    .pressBounce(0.92f)
                    .clip(corners(14.dp))
                    .background(if (on) palette.onGlass.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable(onClickLabel = "Make it this size") {
                        if (!on) {
                            tick()
                            onSize(size.spanX, size.rows)
                        }
                    }
                    .semantics {
                        contentDescription = "${size.name}, ${size.spanX} by ${size.rows}"
                        selected = on
                    }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SizeShape(
                    spanX = size.spanX,
                    rows = size.rows,
                    columns = columns,
                    maxRows = tallest,
                    modifier = Modifier.size(width = 40.dp, height = 26.dp),
                    lit = { if (on) 1f else 0f },
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    size.name,
                    color = palette.onGlass.copy(alpha = if (on) 1f else 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

// ------------------------------------------------------------------------------- the panel

/** The panel with nothing picked: what to add, where to start from, what it stands on, its pieces and its name. */
@Composable
private fun WholePanel(
    design: MadeWidget,
    settings: ZenithSettings,
    geometry: HomeGeometry?,
    canDelete: Boolean,
    askingDelete: Boolean,
    onAdd: (String) -> Unit,
    onStart: (Starter) -> Unit,
    onChange: (MadeWidget, String?) -> Unit,
    onSettle: () -> Unit,
    onPick: (String) -> Unit,
    onAskDelete: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalPalette.current
    Heading("Add")
    AddTiles(enabled = Pieces.canAdd(design), onAdd = onAdd)
    if (design.pieces.isEmpty()) {
        Heading("Or start from")
        Starters(geometry = geometry, columns = settings.columns, onStart = onStart)
    }
    Heading("Background")
    Segmented(
        options = listOf(MadeBack.GLASS.key to "Glass", MadeBack.COLOUR.key to "Colour", MadeBack.CLEAR.key to "No glass"),
        selected = design.back.key,
        onSelect = { onChange(design.copy(back = MadeBack.of(it)), null) },
    )
    if (design.back != MadeBack.CLEAR) {
        Spacer(Modifier.height(12.dp))
        Swatches(
            selected = design.colour,
            none = if (design.back == MadeBack.GLASS) "Clear" else "Plain",
            onPick = { onChange(design.copy(colour = it), null) },
        )
    }
    Note(
        when (design.back) {
            MadeBack.GLASS -> "The pane Zenith's widgets have, tinted with a colour if you like."
            MadeBack.COLOUR -> "A card in its colour, its words in whichever ink reads on it."
            MadeBack.CLEAR -> "Nothing but your wallpaper. Turn on Made of glass for a piece to make it glass."
        },
    )
    if (design.pieces.isNotEmpty()) {
        Heading("Pieces")
        PieceChips(design, onPick)
    }
    Heading("Name")
    MakerField(
        value = design.name,
        placeholder = MadeWidgets.FIRST_NAME,
        lines = 1,
        onChange = { onChange(design.copy(name = it.take(MadeWidgets.MOST_NAME)), "name") },
        onDone = onSettle,
    )
    Note("Written under it on the home screen, as an app's name is.")
    if (canDelete) {
        Spacer(Modifier.height(22.dp))
        if (askingDelete) {
            Text(
                "Delete ${design.name.ifBlank { "this widget" }}? Every copy of it comes off your home screen too.",
                color = palette.onGlass,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton(text = "Keep It", onClick = { onAskDelete(false) }, modifier = Modifier.weight(1f))
                DangerButton("Delete", onClick = onDelete, modifier = Modifier.weight(1f))
            }
        } else {
            DangerButton("Delete Widget", onClick = { onAskDelete(true) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** The panel for the piece picked: what can be done with it, and everything it has. */
@Composable
private fun PiecePanel(
    piece: Piece,
    design: MadeWidget,
    settings: ZenithSettings,
    onChange: (Piece, String?) -> Unit,
    onSettle: () -> Unit,
    onLineUp: (PieceAlign) -> Unit,
    onDuplicate: () -> Unit,
    onForward: () -> Unit,
    onBackward: () -> Unit,
    onRemove: () -> Unit,
    onDone: () -> Unit,
    onChoosePhoto: () -> Unit,
    onChoosePlace: () -> Unit,
) {
    val palette = LocalPalette.current
    val at = design.pieces.indexOfFirst { it.id == piece.id }
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            PieceKinds.name(piece.kind),
            color = palette.onGlass,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text(
            "Done",
            color = palette.accent,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(role = Role.Button, onClickLabel = "Put this piece down") { onDone() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PieceAction(Icons.Rounded.ContentCopy, "Duplicate", enabled = Pieces.canAdd(design), onClick = onDuplicate, modifier = Modifier.weight(1f))
        PieceAction(Icons.Rounded.FlipToFront, "Forward", enabled = at in 0 until design.pieces.lastIndex, onClick = onForward, modifier = Modifier.weight(1f))
        PieceAction(Icons.Rounded.FlipToBack, "Back", enabled = at > 0, onClick = onBackward, modifier = Modifier.weight(1f))
        PieceAction(Icons.Rounded.DeleteOutline, "Delete", enabled = true, danger = true, onClick = onRemove, modifier = Modifier.weight(1f))
    }

    val kind = piece.kind
    if (kind == PieceKinds.TEXT) {
        Heading("Words")
        MakerField(
            value = piece.text,
            placeholder = "Your words",
            lines = Pieces.MOST_LINES,
            onChange = { onChange(piece.copy(text = Pieces.words(it)), "words ${piece.id}") },
            onDone = onSettle,
        )
    }
    if (kind == PieceKinds.WEATHER) {
        Heading("Where")
        Note(
            if (settings.weatherPlace.isBlank()) "Choose where the weather is for. Only that place is ever sent, to Open-Meteo."
            else "For ${settings.weatherPlace}, as the Weather widget is.",
        )
        Spacer(Modifier.height(8.dp))
        GlassButton(
            text = if (settings.weatherPlace.isBlank()) "Choose a Place" else "Change Place",
            onClick = onChoosePlace,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (kind == PieceKinds.PHOTO) {
        Heading("Photo")
        GlassButton(
            text = if (piece.text.isEmpty()) "Choose a Photo" else "Choose Another Photo",
            onClick = onChoosePhoto,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    val styles = PieceStyles.of(kind)
    if (kind == PieceKinds.SYMBOL) {
        Heading("Symbol")
        SymbolGrid(piece.style.ifEmpty { styles.first() }) { onChange(piece.copy(style = it), null) }
    } else if (styles.isNotEmpty()) {
        Heading(if (kind == PieceKinds.SHAPE) "Shape" else "Shows")
        ChoiceChips(
            options = styles.map { it to PieceStyles.name(it, kind) },
            selected = piece.style.takeIf { it in styles } ?: styles.first(),
            onSelect = { onChange(piece.copy(style = it), null) },
        )
    }

    Heading("Size")
    if (PieceKinds.isBox(kind)) {
        PillSlider(
            label = "Width",
            value = piece.w,
            range = Pieces.THINNEST..1f,
            display = "${(piece.w * 100).roundToInt()}%",
            onSettled = onSettle,
        ) { onChange(piece.copy(w = it), "width ${piece.id}") }
        Spacer(Modifier.height(8.dp))
        PillSlider(
            label = "Height",
            value = piece.h,
            range = Pieces.THINNEST..1f,
            display = "${(piece.h * 100).roundToInt()}%",
            onSettled = onSettle,
        ) { onChange(piece.copy(h = it), "height ${piece.id}") }
        if (kind == PieceKinds.PHOTO || piece.style == PieceStyles.SHAPE_RECT || piece.style.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            PillSlider(
                label = "Corners",
                value = piece.corner,
                range = 0f..1f,
                display = "${(piece.corner * 100).roundToInt()}%",
                onSettled = onSettle,
            ) { onChange(piece.copy(corner = it), "corner slider ${piece.id}") }
        }
    } else {
        PillSlider(
            label = "Size",
            value = piece.size,
            range = Pieces.SMALLEST..Pieces.LARGEST,
            display = "${(piece.size * 100).roundToInt()}%",
            onSettled = onSettle,
        ) { onChange(piece.copy(size = it), "size slider ${piece.id}") }
    }
    Note("Or pull its corner on the widget, or pinch it.")

    if (written(piece)) {
        Heading("Weight")
        val weights = listOf(300 to "Light", 400 to "Regular", 500 to "Medium", 600 to "Semibold", 700 to "Bold", 900 to "Heavy")
        ChoiceChips(
            options = weights.map { (weight, name) -> weight.toString() to name },
            selected = weights.minBy { abs(it.first - piece.weight) }.first.toString(),
            onSelect = { onChange(piece.copy(weight = it.toInt()), null) },
        )
        Heading("Font")
        ChoiceChips(
            options = listOf("" to "Zenith's") + ZenithFonts.all.map { it.id to it.label },
            selected = piece.font,
            onSelect = { onChange(piece.copy(font = it), null) },
        )
    }

    Heading("Lines up by")
    Segmented(
        options = listOf(PieceAlign.START.key to "Start", PieceAlign.CENTER.key to "Middle", PieceAlign.END.key to "End"),
        selected = piece.align.key,
        onSelect = { onLineUp(PieceAlign.of(it)) },
        icons = listOf(Icons.Rounded.FormatAlignLeft, Icons.Rounded.FormatAlignCenter, Icons.Rounded.FormatAlignRight),
    )

    if (kind != PieceKinds.PHOTO) {
        Heading("Colour")
        Swatches(
            selected = piece.colour,
            none = if (kind == PieceKinds.BATTERY) "By charge" else "Auto",
            onPick = { onChange(piece.copy(colour = it), null) },
        )
    }
    Heading("How solid")
    PillSlider(
        label = "Opacity",
        value = piece.alpha,
        range = Pieces.FAINTEST..1f,
        display = "${(piece.alpha * 100).roundToInt()}%",
        onSettled = onSettle,
    ) { onChange(piece.copy(alpha = it), "alpha ${piece.id}") }
    if (kind != PieceKinds.PHOTO) {
        Spacer(Modifier.height(14.dp))
        GlassRow(
            on = piece.glass,
            title = "Made of glass",
            says = if (kind == PieceKinds.SHAPE) "A pane of glass in its shape." else "Clear glass that bends the wallpaper, as the No glass widgets' numbers are.",
            onToggle = { onChange(piece.copy(glass = it), null) },
        )
    }
}

/** Whether [piece] is written, and so has a weight and a font. */
private fun written(piece: Piece): Boolean = PieceKinds.isWords(piece.kind) ||
    (piece.kind == PieceKinds.WEATHER && piece.style != PieceStyles.WEATHER_SKY) ||
    (piece.kind == PieceKinds.BATTERY && piece.style != PieceStyles.BATTERY_BAR)

// ------------------------------------------------------------------------------- the panel's parts

@Composable
private fun Heading(text: String) {
    Text(
        text,
        color = LocalPalette.current.onGlassMuted,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 9.dp),
    )
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        color = LocalPalette.current.onGlassMuted,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
    )
}

/** The icon each kind of piece is added by. */
private fun kindIcon(kind: String): ImageVector = when (kind) {
    PieceKinds.TIME -> Icons.Rounded.AccessTime
    PieceKinds.DATE -> Icons.Rounded.CalendarToday
    PieceKinds.TEXT -> Icons.Rounded.TextFields
    PieceKinds.BATTERY -> Icons.Rounded.BatteryFull
    PieceKinds.DIAL -> Icons.Rounded.Watch
    PieceKinds.WEATHER -> Icons.Rounded.WbSunny
    PieceKinds.SYMBOL -> Icons.Rounded.Interests
    PieceKinds.SHAPE -> Icons.Rounded.Category
    PieceKinds.PHOTO -> Icons.Rounded.Image
    PieceKinds.GREETING -> Icons.Rounded.WavingHand
    else -> Icons.Rounded.Alarm
}

/** A tile for each kind of piece; a tap adds one in the middle, picked. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddTiles(enabled: Boolean, onAdd: (String) -> Unit) {
    val palette = LocalPalette.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = ADD_ROW,
        modifier = Modifier.alpha(if (enabled) 1f else 0.4f),
    ) {
        PieceKinds.all.forEach { kind ->
            Column(
                Modifier
                    .weight(1f)
                    .pressBounce(0.93f, enabled = enabled)
                    .clip(corners(16.dp))
                    .background(palette.onGlass.copy(alpha = 0.08f))
                    .clickable(enabled = enabled, role = Role.Button, onClickLabel = "Add ${PieceKinds.name(kind)}") { onAdd(kind) }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(kindIcon(kind), contentDescription = null, tint = palette.onGlass, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(6.dp))
                Text(PieceKinds.name(kind), color = palette.onGlass, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        // The last row as wide a tile as the rest.
        repeat((ADD_ROW - PieceKinds.all.size % ADD_ROW) % ADD_ROW) { Spacer(Modifier.weight(1f)) }
    }
}

/** How many tiles stand in a row under Add. */
private const val ADD_ROW = 4

/** The four starting points, each drawn live; a tap starts the widget from it. */
@Composable
private fun Starters(geometry: HomeGeometry?, columns: Int, onStart: (Starter) -> Unit) {
    val palette = LocalPalette.current
    val density = LocalDensity.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MadeStarters.all.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { starter ->
                    val real = realSize(geometry, WidgetSize(starter.name, starter.spanX.coerceAtMost(columns.coerceAtLeast(1)), starter.rows), columns)
                    val made = remember(starter) { starter.madeAs("starter", starter.name) }
                    Column(
                        Modifier
                            .weight(1f)
                            .pressBounce(0.96f)
                            .clip(corners(18.dp))
                            .background(palette.onGlass.copy(alpha = 0.06f))
                            .clickable(role = Role.Button, onClickLabel = "Start from ${starter.name}") { onStart(starter) }
                            .padding(8.dp),
                    ) {
                        BoxWithConstraints(
                            Modifier
                                .fillMaxWidth()
                                .height(92.dp)
                                .wallpaperWell(corners(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            val zoom = min(1f, min((maxWidth - 12.dp) / real.width, (maxHeight - 12.dp) / real.height))
                            Box(Modifier.size(real.width * zoom, real.height * zoom)) {
                                CompositionLocalProvider(LocalDensity provides Density(density.density * zoom, density.fontScale)) {
                                    MadeWidgetView(made, Modifier.fillMaxSize(), interactive = false)
                                }
                            }
                        }
                        Text(
                            starter.name,
                            color = palette.onGlass,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
                        )
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** A chip for each piece, the top one first, to pick one that is hard to reach on the stage. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PieceChips(design: MadeWidget, onPick: (String) -> Unit) {
    val palette = LocalPalette.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        design.pieces.asReversed().forEach { piece ->
            val words = when (piece.kind) {
                PieceKinds.TEXT -> piece.text.lineSequence().firstOrNull()?.takeIf { it.isNotBlank() }?.let { ": ${it.take(16)}" }.orEmpty()
                PieceKinds.SYMBOL -> ": ${piece.style}"
                else -> ""
            }
            Row(
                Modifier
                    .pressBounce(0.94f)
                    .clip(RoundedCornerShape(50))
                    .background(palette.onGlass.copy(alpha = 0.08f))
                    .clickable(role = Role.Button, onClickLabel = "Pick it") { onPick(piece.id) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(kindIcon(piece.kind), contentDescription = null, tint = palette.onGlass, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(PieceKinds.name(piece.kind) + words, color = palette.onGlass, fontSize = 14.sp, maxLines = 1)
            }
        }
    }
}

/** Duplicate, Forward, Back or Delete, under a picked piece's name. */
@Composable
private fun PieceAction(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, danger: Boolean = false) {
    val palette = LocalPalette.current
    val ink = if (danger) Color(0xFFFF453A) else palette.onGlass
    Column(
        modifier
            .alpha(if (enabled) 1f else 0.35f)
            .pressBounce(0.93f, enabled = enabled)
            .clip(corners(14.dp))
            .background(palette.onGlass.copy(alpha = 0.08f))
            .clickable(enabled = enabled, role = Role.Button) { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = ink, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** The symbols to choose from, each drawn as it will be. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SymbolGrid(selected: String, onPick: (String) -> Unit) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 5,
    ) {
        PieceStyles.symbols.forEach { name ->
            val on = name == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .pressBounce(0.9f)
                    .clip(corners(14.dp))
                    .background(if (on) palette.accent else palette.onGlass.copy(alpha = 0.08f))
                    .clickable(role = Role.Button, onClickLabel = name) {
                        if (!on) tick()
                        onPick(name)
                    }
                    .semantics {
                        contentDescription = name
                        this.selected = on
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(symbolIcon(name), contentDescription = null, tint = if (on) inkOnTint(palette.accent) else palette.onGlass, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/** The colours to choose from: Apple's, as Zenith's widgets use them, with white and black. */
private val SWATCHES = listOf(
    0xFFFFFFFF to "White", 0xFF1C1C1E to "Black", 0xFFFF453A to "Red", 0xFFFF9F0A to "Orange",
    0xFFFFD60A to "Yellow", 0xFF30D158 to "Green", 0xFF63E6E2 to "Mint", 0xFF64D2FF to "Cyan",
    0xFF0A84FF to "Blue", 0xFF5E5CE6 to "Indigo", 0xFFBF5AF2 to "Purple", 0xFFFF375F to "Pink",
    0xFFAC8E68 to "Brown",
)

/** A colour to choose, or none ([none] says what none means here). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Swatches(selected: Long, none: String, onPick: (Long) -> Unit) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val noneOn = selected == 0L
        Text(
            none,
            color = if (noneOn) inkOnTint(palette.accent) else palette.onGlass,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .heightIn(min = 34.dp)
                .clip(RoundedCornerShape(50))
                .background(if (noneOn) palette.accent else palette.onGlass.copy(alpha = 0.1f))
                .clickable(role = Role.Button) {
                    if (!noneOn) tick()
                    onPick(0L)
                }
                .semantics { this.selected = noneOn }
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
        SWATCHES.forEach { (colour, name) ->
            val on = colour == selected
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(colour))
                    .border(if (on) 3.dp else 1.dp, if (on) palette.accent else palette.onGlass.copy(alpha = 0.2f), CircleShape)
                    .clickable(role = Role.Button) {
                        if (!on) tick()
                        onPick(colour)
                    }
                    .semantics {
                        contentDescription = name
                        this.selected = on
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (on) Icon(Icons.Rounded.Check, contentDescription = null, tint = inkOnTint(Color(colour)), modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Made of glass, or not: a row with its switch. */
@Composable
private fun GlassRow(on: Boolean, title: String, says: String, onToggle: (Boolean) -> Unit) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(corners(16.dp))
            .background(palette.onGlass.copy(alpha = 0.08f))
            .clickable(role = Role.Switch) {
                tick()
                onToggle(!on)
            }
            .semantics { selected = on }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(says, color = palette.onGlassMuted, fontSize = 13.sp, lineHeight = 17.sp)
        }
        Spacer(Modifier.width(12.dp))
        Toggle(checked = on)
    }
}

/** A button that takes something away, in red. */
@Composable
private fun DangerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Color(0xFFFF453A),
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .pressBounce(0.96f)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFFF453A).copy(alpha = 0.14f))
            .clickable(role = Role.Button) { onClick() }
            .padding(vertical = 14.dp),
    )
}

/** A box to type in, ringed in the accent while it has the keyboard. [lines] above one lets it go onto more. */
@Composable
private fun MakerField(value: String, placeholder: String, lines: Int, onChange: (String) -> Unit, onDone: () -> Unit) {
    val palette = LocalPalette.current
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .clip(corners(14.dp))
            .background(palette.onGlass.copy(alpha = 0.08f))
            .border(1.5.dp, if (focused) palette.accent.copy(alpha = 0.75f) else Color.Transparent, corners(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) Text(placeholder, color = palette.onGlassMuted, fontSize = 15.sp)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = lines == 1,
            maxLines = lines,
            textStyle = TextStyle(fontFamily = LocalZenithFont.current, color = palette.onGlass, fontSize = 15.sp),
            cursorBrush = SolidColor(palette.accent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = if (lines == 1) ImeAction.Done else ImeAction.Default,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (focused && !it.isFocused) onDone()
                    focused = it.isFocused
                },
        )
    }
}
