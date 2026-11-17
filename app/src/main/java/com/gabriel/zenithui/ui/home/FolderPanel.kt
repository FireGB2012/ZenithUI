package com.gabriel.zenithui.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.FolderCell
import com.gabriel.zenithui.data.FolderGrid
import com.gabriel.zenithui.data.FolderPour
import com.gabriel.zenithui.data.FolderPourGeometry
import com.gabriel.zenithui.data.FolderSize
import com.gabriel.zenithui.data.PourFrame
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.AppIcon
import com.gabriel.zenithui.ui.common.IconLabel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.launchOrigin
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.theme.LocalBackdrop
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.dim
import com.gabriel.zenithui.ui.theme.glass
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** A folder that is open, or on its way in or out: which, where its tile is and where it was tapped, in root pixels. */
internal data class FolderUp(val id: String, val tile: Rect?, val tap: Offset?)

/**
 * How far an open folder has got, for the pane and the home screen behind it to read while laying
 * out and drawing: opening on a clock of its own ([clock], in seconds, run at the Speed of Motion &
 * feel), at rest, or closing ([out], 0 to 1). With movement reduced, or with no tile to come out
 * of, it only fades ([fade]) where it stands.
 */
@Stable
internal class FolderMotion {
    enum class Phase { Gone, In, Rest, Out }

    var phase by mutableStateOf(Phase.Gone)
        private set

    /** Whether it pours out of its tile and back, or only fades. */
    var grows by mutableStateOf(true)
        private set

    val clock = Animatable(0f)
    val out = Animatable(0f)
    val fade = Animatable(0f)

    /** Where the drop was as closing began: closing goes back from there. */
    var from: PourFrame? = null
        private set

    /** How far opening's clock had run as closing began: each app goes back from as far out as it had come. */
    var fromClock: Float = FolderPour.IN_S
        private set

    /** The drop as it was last laid out. A plain value, written while laying out, never read to draw. */
    var last: PourFrame? = null

    /** On its way out: the home screen behind clears as it goes. */
    val closing: Boolean get() = phase == Phase.Out

    suspend fun open(grows: Boolean, pace: Float) {
        this.grows = grows
        from = null
        out.snapTo(0f)
        if (!grows) {
            phase = Phase.In
            fade.animateTo(1f, tween(Motion.REDUCED_MS))
        } else {
            fade.snapTo(1f)
            clock.snapTo(0f)
            phase = Phase.In
            clock.animateTo(FolderPour.IN_S, tween((FolderPour.IN_S * 1000f / pace).roundToInt(), easing = LinearEasing))
        }
        phase = Phase.Rest
    }

    suspend fun close(pace: Float) {
        from = last
        fromClock = if (phase == Phase.In) clock.value else FolderPour.IN_S
        out.snapTo(0f)
        phase = Phase.Out
        if (!grows || from == null) {
            fade.animateTo(0f, tween(Motion.REDUCED_MS))
        } else {
            out.animateTo(1f, tween((FolderPour.OUT_MS / pace).roundToInt(), easing = LinearEasing))
        }
        phase = Phase.Gone
    }

    /** How much of the veil behind is up: read while drawing. */
    fun shown(): Float = when (phase) {
        Phase.Gone -> 0f
        Phase.Rest -> 1f
        Phase.In -> if (grows) (clock.value / VEIL_IN_S).coerceIn(0f, 1f) else fade.value
        Phase.Out -> if (grows && from != null) 1f - out.value else fade.value
    }

    private companion object {
        /** The veil is up this far into opening. */
        const val VEIL_IN_S = 0.15f
    }
}

/**
 * A folder's tile gulping the open pane down as it lands back in it: the tile is squashed, read
 * while drawing ([squash]), and nothing is composed for it.
 */
internal object FolderGulp {
    private var id by mutableStateOf<String?>(null)
    private val clock = Animatable(1f)

    /** How [folderId]'s tile is squashed now, across and down, or null while it is not gulping. */
    fun squash(folderId: String): Pair<Float, Float>? =
        if (id != folderId) null else FolderPour.gulp(clock.value)

    suspend fun play(folderId: String, pace: Float) {
        id = folderId
        try {
            clock.snapTo(0f)
            clock.animateTo(1f, tween((FolderPour.GULP_MS / pace).roundToInt(), easing = LinearEasing))
        } finally {
            if (id == folderId) id = null
        }
    }
}

/**
 * A drop of glass with round corners, as round as [radius] says now (read as it is drawn, so the
 * drop's corners change with it) and never rounder than half its short side.
 */
private class DropShape(private val radius: () -> Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = min(radius(), min(size.width, size.height) / 2f).coerceAtLeast(0f)
        return Outline.Rounded(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r)))
    }
}

/**
 * What of the pane shows inside the drop, in the pane's own pixels: all of its width, the drop's
 * height at the pane's scale, about its middle, as round as the drop. The pane is drawn scaled to
 * the drop's width, so this is the drop's outline drawn back at the pane's size.
 */
private data class SeenOfPane(val height: Float, val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val h = min(height, size.height)
        val top = (size.height - h) / 2f
        val r = min(radius, min(size.width, h) / 2f).coerceAtLeast(0f)
        return Outline.Rounded(RoundRect(0f, top, size.width, top + h, CornerRadius(r)))
    }
}

/** How round a tile in [shape] [side] pixels across is at its corners: a continuous corner counts as about a quarter of its side. */
internal fun cornerOf(shape: Shape, side: Float, density: Density): Float =
    when (val outline = shape.createOutline(Size(side, side), LayoutDirection.Ltr, density)) {
        is Outline.Rounded -> outline.roundRect.topLeftCornerRadius.x
        is Outline.Rectangle -> 0f
        is Outline.Generic -> side * CONTINUOUS_CORNER
    }

/** A continuous corner, as an iPhone's icons have, matched by a round one: about this share of the side. */
private const val CONTINUOUS_CORNER = 0.242f

/**
 * A folder, open, as iOS 26 has it: a pane of the wallpaper's clear glass in the middle of the
 * screen, one size whatever is in it, its name big and bold above it, its apps three by three with
 * their names and badges, nine to a page, with page dots when there is more than one page.
 *
 * It pours out of its tile as liquid glass ([FolderPour]): the tile is the drop the pane spreads
 * out of, wide first and then tall, sloshing once and settling, and the tile's tiny icons zoom with
 * it into the apps, nearest the finger first. Closing, it is drawn back into the tile, which gulps
 * it down ([FolderGulp]). All of it is read while laying out and drawing ([motion]), so it moves
 * without composing anything. With movement reduced it only fades where it stands.
 *
 * [tile] and [tap] are in the home screen's own pixels; [blurred] is whether the home screen behind
 * is out of focus, which leaves it all but undimmed.
 */
@Composable
internal fun FolderPanel(
    folder: FolderCell,
    tile: Rect?,
    tileShape: Shape,
    tap: Offset?,
    motion: FolderMotion,
    blurred: Boolean,
    appsByKey: Map<String, AppInfo>,
    settings: ZenithSettings,
    onOpen: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    val density = LocalDensity.current
    var title by remember(folder.id) { mutableStateOf(folder.title) }
    // Saving on every keystroke rewrites the whole layout as you type; wait for a pause.
    LaunchedEffect(title) {
        delay(400)
        if (title.isNotBlank() && title != folder.title) onRename(title)
    }
    // Where this layer is on the home screen, to turn the tile's place into one of its own.
    var layerOrigin by remember { mutableStateOf(Offset.Zero) }
    val veil = if (blurred) Color.Black.copy(alpha = BLURRED_VEIL) else palette.dim(SHARP_VEIL)

    Box(
        Modifier
            .fillMaxSize()
            .imePadding()
            .onGloballyPositioned { layerOrigin = it.positionInRoot() },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = motion.shown() }
                .background(veil)
                // Anywhere outside the pane puts the folder away.
                .pointerInput(Unit) { detectTapGestures { onDismiss() } }
        )
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val pane = remember(maxWidth) { FolderSize.of(maxWidth.value) }
            val unit = density.density
            val paneWidth = pane.width * unit
            val paneHeight = pane.height * unit
            val paneLeft = (constraints.maxWidth - paneWidth) / 2f
            val paneTop = pane.paneTop(maxHeight.value) * unit
            val rest = PourFrame(paneLeft, paneTop, paneWidth, paneHeight, pane.corner * unit)
            val tileRadius = remember(tile, tileShape) { tile?.let { cornerOf(tileShape, it.width, density) } ?: 0f }
            val geometry = tile?.let {
                FolderPourGeometry(
                    tileLeft = it.left - layerOrigin.x, tileTop = it.top - layerOrigin.y,
                    tileWidth = it.width, tileHeight = it.height, tileRadius = tileRadius,
                    paneLeft = paneLeft, paneTop = paneTop, paneWidth = paneWidth, paneHeight = paneHeight,
                    paneRadius = rest.radius,
                )
            }

            /** The drop now. Read only while laying out and drawing. */
            fun frame(): PourFrame {
                val g = geometry
                val now = when (motion.phase) {
                    FolderMotion.Phase.Gone -> rest.copy(alpha = 0f)
                    FolderMotion.Phase.Rest -> rest
                    FolderMotion.Phase.In ->
                        if (!motion.grows || g == null) rest.copy(alpha = motion.fade.value) else FolderPour.frameIn(motion.clock.value, g)
                    FolderMotion.Phase.Out -> {
                        val from = motion.from
                        if (!motion.grows || g == null || from == null) rest.copy(alpha = motion.fade.value)
                        else FolderPour.frameOut(motion.out.value, from, g)
                    }
                }
                if (motion.phase != FolderMotion.Phase.Out) motion.last = now
                return now
            }

            /** How big what is in the pane is drawn now, as a share of its own size. */
            fun zoom(): Float = if (motion.grows && geometry != null) frame().width / paneWidth else 1f

            /** How solid the words are now: the apps' names, the page dots and the name above. */
            fun words(): Float = when (motion.phase) {
                FolderMotion.Phase.Out -> if (motion.grows) FolderPour.wordsLeaving(motion.out.value) else motion.fade.value
                FolderMotion.Phase.Gone -> 0f
                else -> 1f
            }

            /**
             * How far out of the tile app [rank] (nearest the finger first) of the first page is: 0
             * still the tile's tiny icon, 1 in its place. Closing, each goes back from as far out as
             * it had come, landing in the tile as the drop does.
             */
            fun outOf(rank: Int): Float {
                if (!motion.grows || geometry == null) return 1f
                return when (motion.phase) {
                    FolderMotion.Phase.In -> FolderPour.pop(motion.clock.value, rank)
                    FolderMotion.Phase.Out ->
                        if (motion.from == null) 1f
                        else FolderPour.pop(motion.fromClock, rank) * (1f - FolderPour.closing(motion.out.value))
                    else -> 1f
                }
            }

            /** Where app [rank] in [slot] of the first page is drawn now, flying out of the tile or back. */
            fun flightOf(rank: Int, slot: Int): FolderPour.Flight {
                val g = geometry
                if (!motion.grows || g == null || motion.phase == FolderMotion.Phase.Rest || motion.phase == FolderMotion.Phase.Gone) return STILL
                return FolderPour.flight(outOf(rank), slot, frame(), g, pane, unit)
            }

            // The order the first page's apps come out in, nearest the finger first.
            val ranks = remember(folder.keys, tap, tile, pane, layerOrigin, paneLeft, paneTop) {
                val first = FolderGrid.onPage(folder.keys, 0)
                val at: Offset? = tap ?: tile?.center
                if (at == null) {
                    first.indices.associate { first[it] to it }
                } else {
                    val xs = FloatArray(first.size) { paneLeft + (pane.iconLeft(FolderGrid.column(it)) + pane.icon / 2f) * unit }
                    val ys = FloatArray(first.size) { paneTop + (pane.iconTop(FolderGrid.row(it)) + pane.icon / 2f) * unit }
                    FolderPour.order(xs, ys, at.x - layerOrigin.x, at.y - layerOrigin.y)
                        .withIndex().associate { (rank, index) -> first[index] to rank }
                }
            }

            // ---------------------------------------------------------------- the drop
            val noCopy = LocalBackdrop.current == null
            Box(
                Modifier
                    .layout { measurable, constraints ->
                        val now = frame()
                        val placeable = measurable.measure(
                            Constraints.fixed(now.width.roundToInt().coerceAtLeast(1), now.height.roundToInt().coerceAtLeast(1))
                        )
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeable.place(now.left.roundToInt(), now.top.roundToInt())
                        }
                    }
                    .graphicsLayer { alpha = frame().alpha }
                    // Where Zenith has no copy of the wallpaper the glass is only a veil: a body under it.
                    .then(
                        if (noCopy) {
                            Modifier.drawBehind {
                                val r = min(frame().radius, min(size.width, size.height) / 2f)
                                drawRoundRect(Color(0xFF2A2A2E).copy(alpha = 0.92f), cornerRadius = CornerRadius(r))
                            }
                        } else {
                            Modifier
                        }
                    )
                    .glass(
                        DropShape { frame().radius },
                        strength = 1.2f,
                        solid = PANE_BODY,
                        // It is moved and resized as it pours: the wallpaper behind is lined up again each frame.
                        tracking = {
                            motion.clock.value
                            motion.out.value
                        },
                    ),
            )

            // ---------------------------------------------------------------- the apps
            val pages = FolderGrid.pages(folder.keys.size)
            val pager = rememberPagerState(pageCount = { FolderGrid.pages(folder.keys.size) })
            Box(
                Modifier
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(
                            Constraints.fixed(paneWidth.roundToInt().coerceAtLeast(1), paneHeight.roundToInt().coerceAtLeast(1))
                        )
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeable.place(paneLeft.roundToInt(), paneTop.roundToInt())
                        }
                    }
                    .graphicsLayer {
                        val now = frame()
                        val z = if (motion.grows && geometry != null) now.width / paneWidth else 1f
                        scaleX = z
                        scaleY = z
                        translationX = now.left + now.width / 2f - (paneLeft + paneWidth / 2f)
                        translationY = now.top + now.height / 2f - (paneTop + paneHeight / 2f)
                        alpha = now.alpha
                        // Only what the drop covers shows: the pane is revealed as the glass spreads over it.
                        if (z > 0f) {
                            shape = SeenOfPane(now.height / z, now.radius / z)
                            clip = true
                        }
                    }
                    // A touch on the pane itself is not a touch outside it.
                    .pointerInput(Unit) { detectTapGestures { } }
                    .semantics { paneTitle = folder.title },
            ) {
                HorizontalPager(
                    state = pager,
                    userScrollEnabled = pages > 1,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    Box(Modifier.fillMaxSize()) {
                        FolderGrid.onPage(folder.keys, page).forEachIndexed { slot, app ->
                            key(app) {
                                // Only the first page is on the tile, so only its apps fly out of it.
                                val rank = ranks[app] ?: slot
                                FolderApp(
                                    key = app,
                                    label = appsByKey[app]?.label ?: "",
                                    pane = pane,
                                    slot = slot,
                                    shape = ZenithShapes.forIconShape(settings.iconShape),
                                    flight = { if (page == 0) flightOf(rank, slot) else STILL },
                                    names = {
                                        val out = if (page == 0) outOf(rank) else 1f
                                        FolderPour.namesShown(min(zoom(), out)) * words()
                                    },
                                    onOpen = { onOpen(app) },
                                    onTakeOut = { onRemoveApp(app) },
                                )
                            }
                        }
                    }
                }
                if (pages > 1) {
                    PageDots(
                        count = pages,
                        current = { pager.currentPage },
                        shown = { FolderPour.namesShown(zoom()) * words() },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = FolderSize.DOT_FOOT.dp),
                    )
                }
            }

            // ---------------------------------------------------------------- its name
            FolderName(
                title = title,
                onTitle = { title = it.replace("\n", "") },
                left = with(density) { paneLeft.toDp() },
                width = with(density) { paneWidth.toDp() },
                textStart = pane.titleStart.dp,
                top = with(density) { paneTop.toDp() } - (FolderSize.TITLE_LINE + FolderSize.TITLE_GAP).dp,
                scale = {
                    if (motion.phase == FolderMotion.Phase.In && motion.grows && geometry != null) FolderPour.titleScale(motion.clock.value) else 1f
                },
                alpha = {
                    when (motion.phase) {
                        FolderMotion.Phase.In ->
                            if (motion.grows && geometry != null) FolderPour.titleAlpha(motion.clock.value) else motion.fade.value
                        else -> words()
                    }
                },
            )
        }
    }
}

/**
 * One app in the open folder, in its [slot] on the page: its icon, with its badge, and its name
 * under it in white. It is drawn where [flight] says, flying out of the tile's tiny icon and back,
 * and its name shows as far as [names] says, both read while drawing. A tap opens it, from its
 * icon here; a hold takes it out onto the home screen.
 */
@Composable
private fun FolderApp(
    key: String,
    label: String,
    pane: FolderSize,
    slot: Int,
    shape: Shape,
    flight: () -> FolderPour.Flight,
    names: () -> Float,
    onOpen: () -> Unit,
    onTakeOut: () -> Unit,
) {
    val density = LocalDensity.current
    // One taken out, and the rest glide up to close the gap.
    val place by animateIntOffsetAsState(
        with(density) {
            IntOffset(
                (pane.side + FolderGrid.column(slot) * pane.column).dp.roundToPx(),
                pane.iconTop(FolderGrid.row(slot)).dp.roundToPx(),
            )
        },
        Motion.cell,
        label = "folderApp",
    )
    val slotHeight = pane.icon + FolderSize.NAME_GAP + pane.label
    Column(
        Modifier
            .offset { place }
            .width(pane.column.dp)
            .graphicsLayer {
                val now = flight()
                translationX = now.dx
                translationY = now.dy
                scaleX = now.scale
                scaleY = now.scale
                transformOrigin = TransformOrigin(0.5f, (pane.icon / 2f) / slotHeight)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(
            key = key,
            label = label,
            size = pane.icon.dp,
            shape = shape,
            badge = true,
            onGlass = true,
            modifier = Modifier
                // So an app opened from here grows out of its icon here.
                .launchOrigin(key)
                .pressBounce()
                .pointerInput(key) {
                    detectTapGestures(
                        onTap = { onOpen() },
                        onLongPress = { onTakeOut() },
                    )
                },
        )
        Spacer(Modifier.height(FolderSize.NAME_GAP.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(pane.label.dp)
                .padding(horizontal = 2.dp)
                .graphicsLayer { alpha = names() },
            contentAlignment = Alignment.Center,
        ) {
            IconLabel(label, textAlign = TextAlign.Center)
        }
    }
}

/** The folder's page dots: white for the page in view, faint for the rest. */
@Composable
private fun PageDots(count: Int, current: () -> Int, shown: () -> Float, modifier: Modifier = Modifier) {
    val now = current()
    Row(
        modifier
            .graphicsLayer { alpha = shown() }
            .semantics { contentDescription = "Page ${now + 1} of $count" },
        horizontalArrangement = Arrangement.spacedBy(FolderSize.DOT_GAP.dp),
    ) {
        repeat(count) { index ->
            val lit by animateFloatAsState(if (index == now) 1f else 0f, tween(200), label = "folderDot")
            Box(
                Modifier
                    .size(FolderSize.DOT.dp)
                    .drawBehind { drawCircle(Color.White.copy(alpha = 0.4f + 0.6f * lit)) }
            )
        }
    }
}

/**
 * The folder's name over the pane: big, bold and white, starting where the first icon does. Tap it
 * to rename the folder; while it has the keyboard it stands in a capsule of glass the width of the
 * pane, with a button that clears it, as iOS's does. It grows in from a little small about its
 * left edge as the folder opens ([scale], [alpha], read while drawing).
 */
@Composable
private fun FolderName(
    title: String,
    onTitle: (String) -> Unit,
    left: androidx.compose.ui.unit.Dp,
    width: androidx.compose.ui.unit.Dp,
    textStart: androidx.compose.ui.unit.Dp,
    top: androidx.compose.ui.unit.Dp,
    scale: () -> Float,
    alpha: () -> Float,
) {
    val focus = LocalFocusManager.current
    var naming by remember { mutableStateOf(false) }
    val capsule by animateFloatAsState(if (naming) 1f else 0f, tween(200), label = "folderNaming")
    val density = LocalDensity.current
    Box(
        Modifier
            .offset(x = left, y = top)
            .width(width)
            .height(FolderSize.TITLE_LINE.dp)
            .graphicsLayer {
                val s = scale()
                scaleX = s
                scaleY = s
                this.alpha = alpha()
                transformOrigin = TransformOrigin(textStart.toPx() / width.toPx().coerceAtLeast(1f), 0.5f)
            },
    ) {
        if (capsule > 0.01f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.alpha = capsule
                        val s = 0.92f + 0.08f * capsule
                        scaleX = s
                        scaleY = s
                    }
                    .glass(RoundedCornerShape(50), strength = 1.2f, solid = PANE_BODY)
            )
        }
        BasicTextField(
            value = title,
            onValueChange = onTitle,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = LocalZenithFont.current,
                color = Color.White,
                fontSize = FolderSize.TITLE_SP.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.6).sp,
                shadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, with(density) { 1.dp.toPx() }), with(density) { 6.dp.toPx() }),
            ),
            cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = textStart, end = if (naming) 44.dp else 8.dp)
                .fillMaxWidth()
                .onFocusChanged { naming = it.isFocused }
                .semantics { contentDescription = "Folder name" },
        )
        if (naming) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
                    .size(26.dp)
                    .graphicsLayer { this.alpha = capsule }
                    .background(Color.White.copy(alpha = 0.32f), CircleShape)
                    .pointerInput(Unit) { detectTapGestures { onTitle("") } }
                    .semantics { contentDescription = "Clear the name" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.Black.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** An app in its place, not flying. */
private val STILL = FolderPour.Flight(0f, 0f, 1f)

/** How much body the pane's glass has under its white words: the hold menu's, the same in Light and in Dark. */
private const val PANE_BODY = 0.55f

/** How dim the home screen is behind an open folder: hardly at all when it is out of focus, more when it cannot be. */
private const val BLURRED_VEIL = 0.12f
private const val SHARP_VEIL = 0.38f
