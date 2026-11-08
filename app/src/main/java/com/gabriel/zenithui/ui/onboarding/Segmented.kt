package com.gabriel.zenithui.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.theme.GlassMotion
import com.gabriel.zenithui.ui.theme.LocalApple
import com.gabriel.zenithui.ui.theme.STRETCH_THIN
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.dragStretch
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
 * A row of options with one picked, as an iPhone's segmented control: a grey track, and a raised
 * pill, white in the light and grey in the dark, under the one picked. The pill does not slide
 * across as a block: the edge facing the way it is going sets off at once and fast, and the edge
 * behind follows on a softer spring, so it stretches out towards the option you tapped and draws
 * itself up when it gets there. With movement reduced it is simply there.
 *
 * The pill can be picked up. Under a finger it swells; slide it and it follows along the row,
 * stretching with speed, with a tick as each option comes under it; let go and it settles on the
 * nearest, which is then the one picked. It stays solid the whole way: there is no glass on a
 * solid page.
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
    val apple = LocalApple.current
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
            .height(if (large) 48.dp else 40.dp)
            .clip(ZenithShapes.pill)
            .background(apple.field)
            .padding(3.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val count = options.size.coerceAtLeast(1)
            val trackPx = constraints.maxWidth.toFloat()
            val itemPx = trackPx / count
            val itemWidth = maxWidth / count
            val scope = rememberCoroutineScope()
            val thumb = remember { SegmentThumb(scope, itemPx * index, itemPx) }
            LaunchedEffect(index, itemPx) { thumb.goTo(index, itemPx, reduced) }

            // The pill. Laid out over the first option and moved by drawing alone, so it travels
            // without the row being composed again; it sits under the names.
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
                        // Apple's raised pill casts a small soft shadow onto its track.
                        shadowElevation = 3.dp.toPx()
                        shape = ZenithShapes.pill
                        clip = false
                    }
                    .background(apple.pill, ZenithShapes.pill)
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
                // While the pill is held, the option under it is the one written heavier.
                val heldNow = thumb.held
                options.forEachIndexed { position, (id, label) ->
                    val active = position == index
                    val lit = if (heldNow) position == thumb.nearest else active
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(ZenithShapes.pill)
                            .clickable(
                                interactionSource = null,
                                indication = null,
                                role = Role.Tab,
                            ) {
                                if (!active) tick()
                                onSelect(id)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            icons?.getOrNull(position)?.let { icon ->
                                Icon(icon, contentDescription = null, tint = apple.words, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(7.dp))
                            }
                            Text(
                                text = label,
                                color = apple.words,
                                fontSize = if (large) 15.sp else 13.sp,
                                fontWeight = if (lit) FontWeight.SemiBold else FontWeight.Normal,
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
 * Which of [count] options, side by side along a row [trackWidth] wide, a pill whose middle is at
 * [thumbCentreX] is nearest: the one whose middle is closest, and an end one past either end.
 */
internal fun nearestSegment(thumbCentreX: Float, trackWidth: Float, count: Int): Int {
    if (count <= 1 || trackWidth <= 0f) return 0
    return floor(thumbCentreX / (trackWidth / count)).toInt().coerceIn(0, count - 1)
}

/**
 * The pill of a [Segmented] row: where its two edges are, or where a finger has it, and how
 * swollen and stretched it is. All of it read while drawing.
 */
@Stable
private class SegmentThumb(private val scope: CoroutineScope, from: Float, item: Float) {
    val left = Animatable(from)
    val right = Animatable(from + item)
    val swell = Animatable(1f)
    val stretch = Animatable(0f)

    /** The middle of the pill while a finger has it, or NaN. */
    private var dragged by mutableFloatStateOf(Float.NaN)

    /** Whether a finger has the pill. */
    var held by mutableStateOf(false)
        private set

    /** The option nearest the pill while it is held. */
    var nearest by mutableIntStateOf(0)

    /** The option a pill that was let go is settling on, so the choice catching up does not move it again. */
    private var settling = -1
    private var lastItem = item
    private var stretching: Job? = null

    fun leftEdge(item: Float): Float = if (dragged.isNaN()) left.value else dragged - item / 2f
    fun rightEdge(item: Float): Float = if (dragged.isNaN()) right.value else dragged + item / 2f
    fun centre(item: Float): Float = (leftEdge(item) + rightEdge(item)) / 2f

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

private val STRETCH_FOLLOWS = spring<Float>(dampingRatio = 1f, stiffness = 1400f)
private val STRETCH_EASES = spring<Float>(dampingRatio = 1f, stiffness = 260f)
