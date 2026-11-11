package com.gabriel.zenithui.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddHome
import androidx.compose.material.icons.rounded.AppRegistration
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LayersClear
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.HoldDo
import com.gabriel.zenithui.data.HoldEntry
import com.gabriel.zenithui.data.HoldMenuPlace
import com.gabriel.zenithui.data.HoldMenuRules
import com.gabriel.zenithui.data.HoldTuning
import com.gabriel.zenithui.data.Pour
import com.gabriel.zenithui.data.PourFrame
import com.gabriel.zenithui.data.PourGeometry
import com.gabriel.zenithui.data.PourNeck
import com.gabriel.zenithui.data.ROW_UNITS
import com.gabriel.zenithui.data.SizeChoice
import com.gabriel.zenithui.ui.common.AppIcon
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.theme.LocalBackdrop
import com.gabriel.zenithui.ui.theme.glass
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** What is being held: something on a page, by its id, or an app in the dock, by its key. */
sealed interface HoldTarget {
    data class Item(val id: String) : HoldTarget
    data class Dock(val key: String) : HoldTarget
}

/** A place on the menu a finger can be on. Rows are numbered as the menu reads from the top when it stands under the thing. */
sealed interface HoldHit {
    data class Row(val index: Int) : HoldHit
    data class Plus(val index: Int) : HoldHit
    data class Size(val index: Int) : HoldHit

    /** A colour: 0 is "None", then the colours in order. */
    data class Swatch(val index: Int) : HoldHit
}

/**
 * A menu that is open: what for, where the thing itself stands on screen at rest ([frame], in root
 * pixels) and the room it takes with its name under it ([clear], which the menu stands clear of), how
 * much it has grown and how round its corners are, and where the finger was at the hold.
 */
data class HoldOpen(val target: HoldTarget, val frame: Rect, val clear: Rect, val lift: Float, val corner: Float, val start: Offset)

/**
 * What the things on the home screen tell it about a hold, from the moment a finger has rested long
 * enough to the moment it lifts. The home screen answers [move] with whether the thing has been
 * picked up, and from then on the gesture is a carry and the menu hears no more of it.
 */
@Stable
class HoldCalls(
    val start: (target: HoldTarget, frame: Rect, clear: Rect, lift: Float, corner: Float, pointer: Offset) -> Unit,
    /** The finger, still down, is now at [pointer]. True: pick the thing up now. */
    val move: (pointer: Offset) -> Boolean,
    /** The finger lifted without picking the thing up: on a row, which takes it, or not, and the menu stays. */
    val release: () -> Unit,
)

/** The menu beside a held thing: whether it is open, and what the finger is on. */
@Stable
class HoldMenuState {
    var open by mutableStateOf<HoldOpen?>(null)
        private set

    /** What the finger from the hold is on, while it is still down. */
    var hover by mutableStateOf<HoldHit?>(null)

    /** The colours stand in the rows' place. */
    var colours by mutableStateOf(false)

    /** The finger has reached the menu since the hold: from then on it chooses, and never picks up. */
    var onMenu = false

    /** The thing was picked up from under its menu, which is then drawn back in sooner. */
    var pickedUp = false
        private set

    /** Whether the menu stands above the thing; the layer says so once it has placed it. */
    var above = false

    /** Each row and button's rectangle on screen, so a finger that never lifted can be told which it is on. */
    internal val hits = HashMap<HoldHit, Rect>()

    /** A short squash of the held thing as the neck of glass snaps: 0 to 1 through it, 1 at rest. */
    internal val recoil = Animatable(1f)

    fun begin(next: HoldOpen) {
        hits.clear()
        hover = null
        colours = false
        onMenu = false
        pickedUp = false
        open = next
    }

    fun close(pickedUp: Boolean = false) {
        if (open == null) return
        this.pickedUp = pickedUp
        hover = null
        open = null
    }

    /** What is under [point], the "+" of a row before the row itself. */
    fun hitAt(point: Offset): HoldHit? =
        hits.entries.firstOrNull { it.key is HoldHit.Plus && it.value.contains(point) }?.key
            ?: hits.entries.firstOrNull { it.key !is HoldHit.Plus && it.value.contains(point) }?.key

    /**
     * How the held thing is squashed right now, across and down, as factors of its size: a little
     * wide and short as the drop lets go of it, a little tall, and back. Read while drawing.
     */
    fun squash(): Offset {
        val t = recoil.value
        if (t >= 1f) return Offset(1f, 1f)
        return when {
            t < 1f / 3f -> { val k = t * 3f; Offset(1f + 0.035f * k, 1f - 0.045f * k) }
            t < 2f / 3f -> { val k = (t - 1f / 3f) * 3f; Offset(1.035f - 0.045f * k, 0.955f + 0.06f * k) }
            else -> { val k = (t - 2f / 3f) * 3f; Offset(0.99f + 0.01f * k, 1.015f - 0.015f * k) }
        }
    }
}

/** The colours a folder or one of Zenith's widgets can take from its menu: none, then the twelve. */
internal val HOLD_COLOURS: List<Pair<Long, String>> = listOf(0L to "None") + WIDGET_COLOURS

private val INK = Color.White
private val INK_SOFT = Color.White.copy(alpha = 0.72f)
private val RED = Color(0xFFFF453A)
private val HIGHLIGHT = Color.White.copy(alpha = 0.17f)
private val FILL = Color.White.copy(alpha = 0.13f)
private val FILL_STRONG = Color.White.copy(alpha = 0.20f)

/** How much body the menu's glass has under its white words: the same in Light and in Dark. */
private const val MENU_BODY = 0.55f

/**
 * A body under glass that has no copy of the wallpaper to show (a live wallpaper, one that cannot be
 * read, an old phone). Such a pane is only a thin veil, and over the home screen's icons they would
 * read through its words; glass that does show the wallpaper hides them already. The same dark grey
 * in Light and in Dark, as the veil over it is.
 */
@Composable
internal fun Modifier.bodyWithoutWallpaper(corner: Dp): Modifier =
    if (LocalBackdrop.current != null) this else drawBehind {
        val round = min(corner.toPx(), min(size.width, size.height) / 2f)
        drawRoundRect(Color(0xFF2A2A2E).copy(alpha = 0.92f), cornerRadius = CornerRadius(round))
    }

/** A row grows with the text size, so large words are not cut off. */
private fun rowHeightDp(fontScale: Float): Float = max(HoldTuning.ROW_DP, 17f * fontScale * 1.25f + 16f)

/** The colours' pane: three rows of five, and the chosen one's name under them. */
private const val SWATCH_DP = 34f
private const val SWATCHES_HEIGHT_DP = HoldTuning.PAD_DP * 2f + 10f + 3f * SWATCH_DP + 2f * 12f + 12f + 16f + 12f

private enum class Phase { Gone, In, Rest, Out }

/**
 * The menu that stands beside a thing held on the home screen, the wash over everything else, and
 * the neck of glass it pours out on. It is drawn inside the home screen's own layout, not in a
 * window of its own: the finger that made the hold is still down on the thing underneath, so the
 * rows are found by where they are ([HoldMenuState.hits]) rather than by being touched, and the
 * glass is Zenith's own, standing on the wallpaper.
 *
 * [entries] are the rows for what is held, in reading order; [shortcutKeys] the icons of an app's
 * shortcuts, by row index; [colour] the held thing's own colour. [onHit] is a row or button chosen,
 * by a tap or by the finger lifting on it; [onDismiss] a touch anywhere else.
 */
@Composable
internal fun HoldMenuLayer(
    state: HoldMenuState,
    entries: List<HoldEntry>,
    shortcutKeys: List<String>,
    colour: Long,
    columns: Int,
    rows: Int,
    onHit: (HoldHit) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val reduced = LocalMotionLevel.current.reduced
    val unit = density.density
    val open = state.open
    // What was last open is kept while it is drawn back in, rows and all.
    val shown = remember { arrayOfNulls<HoldOpen>(1) }
    val shownEntries = remember { arrayOf(entries) }
    if (open != null) {
        shown[0] = open
        shownEntries[0] = entries
    }
    var phase by remember { mutableStateOf(Phase.Gone) }
    val showing = if (phase == Phase.Gone && open == null) null else shown[0]
    val listed = shownEntries[0]

    var layerSize by remember { mutableStateOf(IntSize.Zero) }
    var layerOrigin by remember { mutableStateOf(Offset.Zero) }
    val statusTop = WindowInsets.statusBars.getTop(density)
    val navBottom = WindowInsets.navigationBars.getBottom(density)

    // Where the menu belongs, in root pixels: for the rows, or for the colours in their place.
    val rowPx = rowHeightDp(density.fontScale) * unit
    val menuWidth = min(HoldTuning.MENU_WIDTH_DP * unit, layerSize.width - 2f * HoldTuning.MARGIN_DP * unit).coerceAtLeast(1f)
    val menuHeight = if (state.colours) {
        SWATCHES_HEIGHT_DP * unit
    } else {
        HoldMenuRules.height(listed, rowPx, HoldTuning.SIZE_ROW_DP * unit, HoldTuning.GAP_DP * unit, HoldTuning.PAD_DP * unit)
    }
    val frame = showing?.frame ?: Rect.Zero
    val clear = showing?.clear ?: Rect.Zero
    val rowsHeight = HoldMenuRules.height(listed, rowPx, HoldTuning.SIZE_ROW_DP * unit, HoldTuning.GAP_DP * unit, HoldTuning.PAD_DP * unit)
    // Which side it stands on is decided by the rows alone, so that the colours, which are shorter,
    // open where the rows were and do not jump across the thing.
    val side = HoldMenuPlace.place(
        clear.left, clear.top, clear.right, clear.bottom, menuWidth, rowsHeight, layerSize.width.toFloat(),
        roomTop = statusTop + 6f * unit, roomBottom = layerSize.height - navBottom - 12f * unit,
        gap = HoldTuning.STAND_OFF_DP * unit, margin = HoldTuning.MARGIN_DP * unit,
    )
    val target = if (state.colours) {
        // The colours keep the edge that faces the thing.
        val top = if (side.above) side.top + rowsHeight - menuHeight else side.top
        Rect(side.left, top, side.left + menuWidth, top + menuHeight)
    } else {
        Rect(side.left, side.top, side.left + menuWidth, side.top + menuHeight)
    }
    state.above = side.above

    // The pour's clock, in seconds; the drawing back, 0 to 1; a plain fade for movement reduced.
    val clock = remember { Animatable(0f) }
    val out = remember { Animatable(0f) }
    val fade = remember { Animatable(0f) }
    val rest = remember { Animatable(target, Rect.VectorConverter) }
    val targetNow = remember { arrayOf(target) }
    targetNow[0] = target

    LaunchedEffect(open) {
        if (open != null) {
            out.snapTo(0f)
            if (reduced) {
                rest.snapTo(targetNow[0])
                phase = Phase.Rest
                fade.animateTo(1f, tween(Motion.REDUCED_MS))
            } else {
                fade.snapTo(1f)
                clock.snapTo(0f)
                phase = Phase.In
                clock.animateTo(Pour.MS / 1000f, tween(Pour.MS, easing = LinearEasing))
                rest.snapTo(targetNow[0])
                phase = Phase.Rest
            }
        } else if (phase != Phase.Gone) {
            if (reduced) {
                fade.animateTo(0f, tween(Motion.REDUCED_MS))
            } else {
                if (phase == Phase.In) rest.snapTo(targetNow[0])
                phase = Phase.Out
                out.snapTo(0f)
                out.animateTo(1f, tween(if (state.pickedUp) Pour.OUT_PICKED_MS else Pour.OUT_MS, easing = LinearEasing))
            }
            phase = Phase.Gone
        }
    }
    // The icon recoils once as the neck snaps, like a tap letting go of a drop.
    LaunchedEffect(open) {
        if (open != null && !reduced) {
            state.recoil.snapTo(1f)
            kotlinx.coroutines.delay((Pour.SNAP_S * 1000).toLong())
            state.recoil.snapTo(0f)
            state.recoil.animateTo(1f, tween(300, easing = LinearEasing))
        } else {
            state.recoil.snapTo(1f)
        }
    }
    // Once it stands, a change of size (the colours, a shortcut that came late) is a short glide.
    LaunchedEffect(target, phase) {
        if (phase == Phase.Rest) {
            if (reduced) rest.snapTo(target) else rest.animateTo(target, spring(dampingRatio = 0.86f, stiffness = 420f))
        }
    }

    val wash by animateFloatAsState(if (open != null) 1f else 0f, tween(250), label = "holdWash")

    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { layerSize = it }
            .onGloballyPositioned { layerOrigin = it.positionInRoot() },
    ) {
        if (showing == null) return@Box

        val geometry: () -> PourGeometry = {
            val at = if (phase == Phase.In) targetNow[0] else rest.value
            PourGeometry(
                above = side.above,
                iconCentreX = frame.center.x, iconCentreY = frame.center.y, iconHeight = frame.height, lift = showing.lift,
                menuLeft = at.left, menuTop = at.top, menuWidth = at.width, menuHeight = at.height, unit = unit,
            )
        }
        // The menu's glass this frame, in root pixels. Read only while laying out and drawing.
        val drop: () -> PourFrame = {
            when (phase) {
                Phase.In -> Pour.frameIn(clock.value, geometry())
                Phase.Out -> Pour.frameOut(out.value, geometry())
                else -> rest.value.let { PourFrame(it.left, it.top, it.width, it.height, HoldTuning.CORNER_DP * unit) }
            }
        }

        // Everything but the held thing is washed a little paler; it alone stays as it was.
        Canvas(Modifier.fillMaxSize()) {
            if (wash <= 0.001f) return@Canvas
            val grown = Rect(
                frame.center.x - frame.width * showing.lift / 2f - layerOrigin.x,
                frame.center.y - frame.height * showing.lift / 2f - layerOrigin.y,
                frame.center.x + frame.width * showing.lift / 2f - layerOrigin.x,
                frame.center.y + frame.height * showing.lift / 2f - layerOrigin.y,
            )
            val hole = Path().apply { addRoundRect(RoundRect(grown, CornerRadius(showing.corner * showing.lift))) }
            clipPath(hole, ClipOp.Difference) { drawRect(Color.White.copy(alpha = 0.07f * wash)) }
        }

        // A touch anywhere else puts the menu away, and goes no further.
        if (open != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false).consume()
                            onDismiss()
                        }
                    },
            )
        }

        // The neck of glass between the icon and the drop, for the moment it lasts.
        Canvas(Modifier.fillMaxSize()) {
            val neck = drop().neck ?: return@Canvas
            val path = neckPath(neck, layerOrigin)
            drawPath(
                path,
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.16f)),
                    startY = min(neck.y0, neck.y1) - layerOrigin.y,
                    endY = max(neck.y0, neck.y1) - layerOrigin.y,
                ),
            )
            drawPath(path, Color.White.copy(alpha = 0.4f), style = Stroke(0.75.dp.toPx()))
        }

        // The pane itself: glass standing on the wallpaper, the size and place the pour gives it.
        Box(
            Modifier
                .layout { measurable, constraints ->
                    val now = drop()
                    val placeable = measurable.measure(
                        Constraints.fixed(now.width.roundToInt().coerceAtLeast(1), now.height.roundToInt().coerceAtLeast(1))
                    )
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place((now.left - layerOrigin.x).roundToInt(), (now.top - layerOrigin.y).roundToInt())
                    }
                }
                .graphicsLayer { alpha = drop().alpha * fade.value }
                .bodyWithoutWallpaper(HoldTuning.CORNER_DP.dp)
                .glass(
                    RoundedCornerShape(HoldTuning.CORNER_DP.dp),
                    strength = 1.2f,
                    solid = MENU_BODY,
                    // It is moved and resized as it pours: the wallpaper behind is lined up again each frame.
                    tracking = {
                        clock.value
                        out.value
                        rest.value
                    },
                ),
        )

        // The rows, where the menu will stand; seen only through the pane as it grows over them.
        Box(
            Modifier
                .layout { measurable, constraints ->
                    val at = if (phase == Phase.Rest || phase == Phase.Out) rest.value else targetNow[0]
                    val placeable = measurable.measure(
                        Constraints.fixed(at.width.roundToInt().coerceAtLeast(1), at.height.roundToInt().coerceAtLeast(1))
                    )
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place((at.left - layerOrigin.x).roundToInt(), (at.top - layerOrigin.y).roundToInt())
                    }
                }
                .graphicsLayer {
                    alpha = fade.value * when (phase) {
                        Phase.In -> ((clock.value - 0.11f) / 0.14f).coerceIn(0f, 1f)
                        Phase.Out -> (1f - out.value / 0.45f).coerceIn(0f, 1f)
                        else -> 1f
                    }
                }
                .drawWithContent {
                    val now = drop()
                    val at = if (phase == Phase.Rest || phase == Phase.Out) rest.value else targetNow[0]
                    clipRect(now.left - at.left, now.top - at.top, now.left - at.left + now.width, now.top - at.top + now.height) {
                        this@drawWithContent.drawContent()
                    }
                }
                .clip(RoundedCornerShape(HoldTuning.CORNER_DP.dp))
                // A touch on the menu between its rows is the menu's: it does not put it away.
                .pointerInput(Unit) { awaitEachGesture { awaitFirstDown().consume() } }
                .semantics { paneTitle = "Menu" },
        ) {
            if (state.colours) {
                Swatches(state = state, colour = colour, onHit = onHit)
            } else {
                Column(Modifier.fillMaxSize().padding(vertical = HoldTuning.PAD_DP.dp)) {
                    // Above the thing the rows are read from the bottom up, so the nearest to the
                    // finger stay nearest: the red row ends up at the top.
                    val order = if (side.above) listed.indices.reversed() else listed.indices.toList()
                    for (index in order) {
                        when (val entry = listed[index]) {
                            is HoldEntry.Sizes -> key("sizes") {
                                if (side.above) Spacer(Modifier.height(HoldTuning.GAP_DP.dp))
                                SizeRow(state = state, choices = entry.choices, columns = columns, rows = rows, onHit = onHit)
                                if (!side.above) Spacer(Modifier.height(HoldTuning.GAP_DP.dp))
                            }

                            is HoldEntry.Row -> key(entry.what, entry.label, index) {
                                MenuLine(
                                    state = state,
                                    index = index,
                                    row = entry,
                                    height = rowHeightDp(density.fontScale),
                                    shortcutKey = if (entry.what == HoldDo.SHORTCUT) shortcutKeys.getOrNull(entry.index) else null,
                                    onHit = onHit,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The neck as an outline: wide at the icon, pinched in the middle, wide again where it meets the drop. */
private fun neckPath(n: PourNeck, origin: Offset): Path {
    val x0 = n.x0 - origin.x
    val y0 = n.y0 - origin.y
    val x1 = n.x1 - origin.x
    val y1 = n.y1 - origin.y
    val xm = (x0 + x1) / 2f
    val ym = (y0 + y1) / 2f
    val k = (y1 - y0) * 0.38f
    return Path().apply {
        moveTo(x0 - n.w0 / 2f, y0)
        cubicTo(x0 - n.w0 / 2f, y0 + k, xm - n.wm / 2f, ym - k * 0.55f, xm - n.wm / 2f, ym)
        cubicTo(xm - n.wm / 2f, ym + k * 0.55f, x1 - n.w1 / 2f, y1 - k, x1 - n.w1 / 2f, y1)
        lineTo(x1 + n.w1 / 2f, y1)
        cubicTo(x1 + n.w1 / 2f, y1 - k, xm + n.wm / 2f, ym + k * 0.55f, xm + n.wm / 2f, ym)
        cubicTo(xm + n.wm / 2f, ym - k * 0.55f, x0 + n.w0 / 2f, y0 + k, x0 + n.w0 / 2f, y0)
        close()
    }
}

/** Remembers where [hit] is on screen for as long as it is there. */
@Composable
private fun Modifier.hit(state: HoldMenuState, hit: HoldHit): Modifier {
    DisposableEffect(hit) { onDispose { state.hits.remove(hit) } }
    return onGloballyPositioned { state.hits[hit] = it.boundsInRoot() }
}

private fun glyphFor(row: HoldEntry.Row): ImageVector = when (row.what) {
    HoldDo.UNLOCK -> Icons.Rounded.LockOpen
    HoldDo.SHORTCUT -> Icons.Rounded.Add
    HoldDo.EDIT_HOME -> Icons.Rounded.AppRegistration
    HoldDo.EDIT_NAME, HoldDo.RENAME -> Icons.Rounded.Edit
    HoldDo.RESIZE -> Icons.Rounded.OpenInFull
    HoldDo.ADD_TO_HOME -> Icons.Rounded.AddHome
    HoldDo.APP_INFO -> Icons.Outlined.Info
    HoldDo.REMOVE -> Icons.Rounded.RemoveCircleOutline
    HoldDo.COLOUR -> Icons.Outlined.Palette
    HoldDo.SET_UP -> when {
        row.label.contains("Photos") -> Icons.Rounded.PhotoLibrary
        row.label.startsWith("Clear") -> Icons.Rounded.DoneAll
        row.label.startsWith("Let ") -> Icons.Rounded.MusicNote
        else -> Icons.Outlined.Info
    }
    HoldDo.STACK -> Icons.Rounded.Layers
    HoldDo.GLASS -> if (row.label.startsWith("Take")) Icons.Rounded.LayersClear else Icons.Rounded.Layers
    HoldDo.UNSTACK -> Icons.AutoMirrored.Rounded.Logout
}

/** One row: its symbol on the left, its words, and for a shortcut the "+" that puts it on the home screen. */
@Composable
private fun MenuLine(
    state: HoldMenuState,
    index: Int,
    row: HoldEntry.Row,
    height: Float,
    shortcutKey: String?,
    onHit: (HoldHit) -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val lit = pressed || state.hover == HoldHit.Row(index)
    val glow by animateFloatAsState(if (lit) 1f else 0f, tween(120), label = "rowLit")
    val ink = if (row.danger) RED else INK
    Box(
        Modifier
            .fillMaxWidth()
            .height(height.dp)
            .hit(state, HoldHit.Row(index))
            .drawBehind {
                // A capsule under the row the finger is on, in from the menu's sides.
                if (glow > 0.01f) {
                    val inX = 10.dp.toPx()
                    val inY = 2.dp.toPx()
                    drawRoundRect(
                        HIGHLIGHT.copy(alpha = HIGHLIGHT.alpha * glow),
                        topLeft = Offset(inX, inY),
                        size = Size(size.width - 2f * inX, size.height - 2f * inY),
                        cornerRadius = CornerRadius(20.dp.toPx()),
                    )
                }
            }
            .clickable(interactionSource = source, indication = null, role = Role.Button) { onHit(HoldHit.Row(index)) },
        contentAlignment = Alignment.CenterStart,
    ) {
        // The symbol's middle is 38 from the menu's edge, the words start at 62: iOS 26's lines.
        Box(Modifier.padding(start = 26.dp).size(24.dp), contentAlignment = Alignment.Center) {
            if (shortcutKey != null) {
                AppIcon(key = shortcutKey, label = "", size = 24.dp, shape = RoundedCornerShape(7.dp))
            } else {
                Icon(glyphFor(row), contentDescription = null, tint = ink, modifier = Modifier.size(22.dp))
            }
        }
        Text(
            text = row.label,
            color = ink,
            style = TextStyle(
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = (-0.3).sp,
                // Red words on glass over a bright wallpaper need the little shadow the mockup gives them.
                shadow = if (row.danger) Shadow(Color.Black.copy(alpha = 0.4f), Offset(0f, 1f), 2f) else null,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 62.dp, end = if (row.plus) 46.dp else 14.dp),
        )
        if (row.plus) {
            val plusLit = state.hover == HoldHit.Plus(index)
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .size(40.dp)
                    .hit(state, HoldHit.Plus(index))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                        onHit(HoldHit.Plus(index))
                    }
                    .semantics { contentDescription = "Put ${row.label} on the Home Screen" },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(27.dp).clip(CircleShape).background(if (plusLit) FILL_STRONG else FILL),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = INK, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/** A widget's sizes side by side, each drawn as the cells it covers: the one it is now lit, one with no room greyed. */
@Composable
private fun SizeRow(state: HoldMenuState, choices: List<SizeChoice>, columns: Int, rows: Int, onHit: (HoldHit) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(HoldTuning.SIZE_ROW_DP.dp).padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val most = choices.maxOf { it.spanY / ROW_UNITS }.coerceAtLeast(2)
        choices.forEachIndexed { index, choice ->
            val lit = state.hover == HoldHit.Size(index) && choice.fits
            Box(
                Modifier
                    .weight(1f, fill = false)
                    .size(width = 60.dp, height = 40.dp)
                    .hit(state, HoldHit.Size(index))
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (choice.current) FILL_STRONG else if (lit) HIGHLIGHT else Color.Transparent)
                    .graphicsLayer { alpha = if (choice.fits || choice.current) 1f else 0.3f }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = choice.fits && !choice.current,
                        role = Role.Button,
                    ) { onHit(HoldHit.Size(index)) }
                    .semantics {
                        contentDescription = "${choice.name}, ${choice.spanX} by ${choice.spanY / ROW_UNITS}" +
                            if (choice.current) ", the size it is now" else if (!choice.fits) ", no room for it here" else ""
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                SizeShape(
                    spanX = choice.spanX,
                    rows = choice.spanY / ROW_UNITS,
                    columns = columns,
                    maxRows = min(most, rows).coerceAtLeast(1),
                    modifier = Modifier.fillMaxSize(),
                    ink = INK,
                )
            }
        }
    }
}

/** The colours, in the rows' place: none, then the twelve, and the chosen one's name. Each tap takes at once. */
@Composable
private fun Swatches(state: HoldMenuState, colour: Long, onHit: (HoldHit) -> Unit) {
    val chosen = HOLD_COLOURS.indexOfFirst { it.first == colour }.coerceAtLeast(0)
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(220)) }
    Column(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = appear.value }
            .padding(top = (HoldTuning.PAD_DP + 10f).dp, bottom = HoldTuning.PAD_DP.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HOLD_COLOURS.chunked(5).forEachIndexed { line, colours ->
            if (line > 0) Spacer(Modifier.height(12.dp))
            // Five across, the block of them in the middle of the pane: (250 - 5 x 34 - 4 x 13) / 2 at each side.
            Row(Modifier.fillMaxWidth().padding(start = 14.dp), horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                colours.forEachIndexed { at, (value, name) ->
                    val index = line * 5 + at
                    val lit = state.hover == HoldHit.Swatch(index)
                    Box(
                        Modifier
                            .size(SWATCH_DP.dp)
                            .hit(state, HoldHit.Swatch(index))
                            .graphicsLayer {
                                val s = if (lit) 1.12f else 1f
                                scaleX = s
                                scaleY = s
                            }
                            .then(
                                if (index == chosen) Modifier.border(2.dp, INK, CircleShape).padding(4.dp) else Modifier
                            )
                            .clip(CircleShape)
                            .then(
                                if (value == 0L) Modifier.border(1.5.dp, INK_SOFT, CircleShape)
                                else Modifier.background(Color(value.toInt()))
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Button,
                            ) { onHit(HoldHit.Swatch(index)) }
                            .semantics { contentDescription = name + if (index == chosen) ", chosen" else "" },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(HOLD_COLOURS[chosen].second, color = INK_SOFT, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}
