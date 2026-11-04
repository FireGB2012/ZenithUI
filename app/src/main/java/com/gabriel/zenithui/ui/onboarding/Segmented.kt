package com.gabriel.zenithui.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.theme.GlassMaterial
import com.gabriel.zenithui.ui.theme.GlassMotion
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.STRETCH_THIN
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.dragStretch
import com.gabriel.zenithui.ui.theme.glass
import com.gabriel.zenithui.ui.theme.pressSpring
import com.gabriel.zenithui.ui.theme.pressTarget
import com.gabriel.zenithui.ui.theme.snapSpring
import com.gabriel.zenithui.ui.theme.spec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor

/**
 * A row of options with one picked. The pill under it does not slide across as a block: the edge
 * facing the way it is going sets off at once and fast, and the edge behind follows on a softer
 * spring, so it stretches out towards the option you tapped and draws itself up when it gets
 * there, the way a drop of something thick moves. With movement reduced it is simply there.
 *
 * The pill can be picked up. Under a finger it turns into a lens of clear glass that swells; slide
 * it and it follows along the row, stretching with speed, with a tick as each option comes under
 * it; let go and it settles on the nearest, which is then the one picked.
 *
 * Given [icons], one for each option, the row is a little taller and each option's symbol stands
 * before a larger name: for a choice that changes how everything else on the page looks, which
 * should be seen at a glance.
 */
@Composable
fun Segmented(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    icons: List<ImageVector>? = null,
) {
    val palette = LocalPalette.current
    val large = icons != null
    val index = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val tick = rememberTick()
    val reduced = LocalMotionLevel.current.reduced
    val latestIndex by rememberUpdatedState(index)
    val latestOptions by rememberUpdatedState(options)
    val select by rememberUpdatedState(onSelect)
    val latestTick by rememberUpdatedState(tick)

    Box(
        Modifier
            .fillMaxWidth()
            .height(if (large) 52.dp else 46.dp)
            .glass(ZenithShapes.pill, strength = 0.8f, material = GlassMaterial.Clear)
            .padding(4.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val count = options.size.coerceAtLeast(1)
            val trackPx = constraints.maxWidth.toFloat()
            val itemPx = trackPx / count
            val itemWidth = maxWidth / count
            val scope = rememberCoroutineScope()
            val thumb = remember { SegmentThumb(scope, itemPx * index, itemPx) }
            LaunchedEffect(index, itemPx) { thumb.goTo(index, itemPx, reduced) }

            // Dark words on a light accent, white on a deep one: dark on violet was hard to read.
            val activeInk = if (palette.accent.luminance() > 0.3f) Color(0xFF0A0B12) else Color.White
            val fill = palette.accent.copy(alpha = 0.9f)

            Box(
                Modifier
                    .fillMaxSize()
                    // Read while drawing, so the pill moves without the row being composed again.
                    .drawBehind {
                        val shown = 1f - thumb.lens.value
                        if (shown <= 0.01f) return@drawBehind
                        val from = thumb.leftEdge(itemPx)
                        val to = thumb.rightEdge(itemPx)
                        drawRoundRect(
                            color = fill,
                            topLeft = Offset(minOf(from, to), 0f),
                            size = Size(abs(to - from), size.height),
                            cornerRadius = CornerRadius(size.height / 2f),
                            alpha = shown,
                        )
                    }
            )

            // The lens the pill becomes under a finger. Laid out over the first option and moved
            // by drawing alone; it sits under the names so they stay sharp.
            Box(
                Modifier
                    .width(itemWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        val from = thumb.leftEdge(itemPx)
                        val to = thumb.rightEdge(itemPx)
                        val swell = thumb.swell.value
                        val stretch = thumb.stretch.value
                        translationX = (from + to) / 2f - itemPx / 2f
                        scaleX = abs(to - from) / itemPx.coerceAtLeast(1f) * swell * (1f + stretch)
                        scaleY = swell * (1f - STRETCH_THIN * stretch)
                        alpha = thumb.lens.value
                    }
                    .glass(
                        ZenithShapes.pill,
                        material = GlassMaterial.Clear,
                        tracking = { thumb.watch() },
                    )
            )

            Row(
                Modifier
                    .fillMaxSize()
                    .pointerInput(count, itemPx, reduced) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val from = minOf(thumb.leftEdge(itemPx), thumb.rightEdge(itemPx))
                            val to = maxOf(thumb.leftEdge(itemPx), thumb.rightEdge(itemPx))
                            // Only the pill itself can be picked up; anywhere else is a tap.
                            if (down.position.x !in from..to || count < 2) return@awaitEachGesture
                            val grab = down.position.x - (from + to) / 2f
                            val velocity = VelocityTracker()
                            velocity.addPosition(down.uptimeMillis, down.position)
                            thumb.nearest = latestIndex
                            thumb.hold(reduced)
                            var dragging = false
                            var lifted = false
                            var nearest = latestIndex
                            try {
                                while (true) {
                                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) {
                                        lifted = true
                                        break
                                    }
                                    velocity.addPosition(change.uptimeMillis, change.position)
                                    if (!dragging) {
                                        val moved = change.position - down.position
                                        val slop = viewConfiguration.touchSlop
                                        if (abs(moved.x) > slop && abs(moved.x) > abs(moved.y)) {
                                            dragging = true
                                        } else if (abs(moved.y) > slop) {
                                            // Up or down: the page is being scrolled, not the pill.
                                            break
                                        }
                                    }
                                    if (dragging) {
                                        change.consume()
                                        val centre = (change.position.x - grab).coerceIn(itemPx / 2f, trackPx - itemPx / 2f)
                                        thumb.dragTo(centre)
                                        thumb.stretchTo(dragStretch(velocity.calculateVelocity().x / density, reduced))
                                        val now = nearestSegment(centre, trackPx, count)
                                        if (now != nearest) {
                                            nearest = now
                                            thumb.nearest = now
                                            latestTick()
                                        }
                                    }
                                }
                            } finally {
                                thumb.letGo(reduced)
                                if (dragging) {
                                    // Let go, it settles on the nearest and that is picked; taken
                                    // away some other way, it goes back to what was picked.
                                    val target = if (lifted) nearestSegment(thumb.centre(itemPx), trackPx, count) else latestIndex
                                    thumb.settle(target, itemPx, reduced) { latestIndex }
                                    if (lifted && target != latestIndex) {
                                        latestOptions.getOrNull(target)?.let { select(it.first) }
                                    }
                                }
                            }
                        }
                    },
            ) {
                // While the pill is a lens there is no accent under the words: the one it is over
                // takes the glass's own ink, as clear glass needs.
                val lensUp = thumb.held
                options.forEachIndexed { position, (id, label) ->
                    val active = position == index
                    val lit = if (lensUp) position == thumb.nearest else active
                    val textColour by animateColorAsState(
                        targetValue = when {
                            lit && lensUp -> palette.onGlass
                            lit -> activeInk
                            else -> palette.onGlassMuted
                        },
                        animationSpec = tween(260),
                        label = "segmentText",
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .pressBounce(0.94f)
                            .clip(RoundedCornerShape(percent = 50))
                            .clickable {
                                if (!active) tick()
                                onSelect(id)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            icons?.getOrNull(position)?.let { icon ->
                                Icon(icon, contentDescription = null, tint = textColour, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(7.dp))
                            }
                            Text(
                                text = label,
                                color = textColour,
                                fontSize = if (large) 15.sp else 13.sp,
                                fontWeight = if (lit) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Which of [count] options, side by side along a row [trackWidth] wide, a lens whose middle is at
 * [thumbCentreX] is nearest: the one whose middle is closest, and an end one past either end.
 */
internal fun nearestSegment(thumbCentreX: Float, trackWidth: Float, count: Int): Int {
    if (count <= 1 || trackWidth <= 0f) return 0
    return floor(thumbCentreX / (trackWidth / count)).toInt().coerceIn(0, count - 1)
}

/**
 * The pill of a [Segmented] row, and the lens it becomes: where its two edges are, or where a
 * finger has it; how swollen and stretched it is; and how far it has turned to glass. All of it
 * read while drawing.
 */
@Stable
private class SegmentThumb(private val scope: CoroutineScope, from: Float, item: Float) {
    val left = Animatable(from)
    val right = Animatable(from + item)
    val swell = Animatable(1f)
    val stretch = Animatable(0f)
    val lens = Animatable(0f)

    /** The middle of the lens while a finger has it, or NaN. */
    private var dragged by mutableFloatStateOf(Float.NaN)

    /** Whether a finger has the pill, so it is a lens. */
    var held by mutableStateOf(false)
        private set

    /** The option nearest the lens while it is held. */
    var nearest by mutableIntStateOf(0)

    /** The option a lens that was let go is settling on, so the choice catching up does not move it again. */
    private var settling = -1
    private var lastItem = item
    private var stretching: Job? = null

    fun leftEdge(item: Float): Float = if (dragged.isNaN()) left.value else dragged - item / 2f
    fun rightEdge(item: Float): Float = if (dragged.isNaN()) right.value else dragged + item / 2f
    fun centre(item: Float): Float = (leftEdge(item) + rightEdge(item)) / 2f

    /** Reads all that moves the lens, for its glass to redraw as it goes. */
    fun watch() {
        left.value
        right.value
        dragged
        swell.value
        stretch.value
    }

    /** The choice changed (or the row was measured again): off to it, a tap's way. */
    suspend fun goTo(index: Int, item: Float, reduced: Boolean) = coroutineScope {
        val resized = item != lastItem
        lastItem = item
        val toLeft = item * index
        val toRight = toLeft + item
        if (!resized && (settling == index || !dragged.isNaN())) return@coroutineScope
        if (left.targetValue == toLeft && right.targetValue == toRight) return@coroutineScope
        if (reduced || resized) {
            left.snapTo(toLeft)
            right.snapTo(toRight)
            return@coroutineScope
        }
        val rightwards = toLeft > left.value
        launch { left.animateTo(toLeft, if (rightwards) PILL_TRAILS else PILL_LEADS) }
        launch { right.animateTo(toRight, if (rightwards) PILL_LEADS else PILL_TRAILS) }
    }

    fun hold(reduced: Boolean) {
        held = true
        val target = pressTarget(GlassMotion.Drag, pressed = true, reduced)
        scope.launch { swell.animateTo(target.scale, pressSpring(GlassMotion.Drag, reduced).spec()) }
        scope.launch { lens.animateTo(1f, LENS_TURN) }
    }

    fun dragTo(centre: Float) {
        dragged = centre
    }

    /** Stretches as fast as it is going, and draws itself up again as soon as it slows. */
    fun stretchTo(amount: Float) {
        stretching?.cancel()
        stretching = scope.launch {
            stretch.animateTo(amount, STRETCH_FOLLOWS)
            delay(48)
            stretch.animateTo(0f, STRETCH_EASES)
        }
    }

    fun letGo(reduced: Boolean) {
        held = false
        val rest = pressTarget(GlassMotion.Drag, pressed = false, reduced)
        scope.launch { swell.animateTo(rest.scale, pressSpring(GlassMotion.Drag, reduced).spec()) }
        scope.launch { lens.animateTo(0f, LENS_TURN) }
        stretching?.cancel()
        stretching = scope.launch { stretch.animateTo(0f, STRETCH_EASES) }
    }

    /** From where the finger left it to [target], on the snap spring; then to [current] if that differs. */
    fun settle(target: Int, item: Float, reduced: Boolean, current: () -> Int) {
        settling = target
        scope.launch {
            try {
                // Edges first, from where it is drawn now, and only then let go of the finger's place.
                left.snapTo(leftEdge(item))
                right.snapTo(rightEdge(item))
                dragged = Float.NaN
                val spring = snapSpring(reduced).spec<Float>()
                coroutineScope {
                    launch { left.animateTo(item * target, spring) }
                    launch { right.animateTo(item * (target + 1), spring) }
                }
            } finally {
                settling = -1
                dragged = Float.NaN
            }
            // Whoever owns the choice may not have taken it (or took another): show theirs.
            val now = current()
            if (now != target) goTo(now, item, reduced)
        }
    }
}

/** The pill's leading edge: off at once, and there with next to nothing to spare. */
private val PILL_LEADS = spring<Float>(dampingRatio = 0.86f, stiffness = 640f)

/** Its trailing edge: later, slower, and a little past where it stops before it comes back. */
private val PILL_TRAILS = spring<Float>(dampingRatio = 0.7f, stiffness = 210f)

/** The pill turning to glass under a finger and back. */
private val LENS_TURN = spring<Float>(dampingRatio = 1f, stiffness = 500f)

private val STRETCH_FOLLOWS = spring<Float>(dampingRatio = 1f, stiffness = 1400f)
private val STRETCH_EASES = spring<Float>(dampingRatio = 1f, stiffness = 260f)
