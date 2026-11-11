package com.gabriel.zenithui.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.HomeGeometry
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.theme.GlassMaterial
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import kotlin.math.roundToInt

/** iOS's blue for the one main action, as the rest of Zenith has it on glass over the wallpaper. */
private val ACTION_BLUE = Color(0xFF0A84FF)

/** The bar's own size: as tall as this, and at least this wide, wider with more pages to show. */
private val BAR_HEIGHT = 48.dp
private val BAR_BUTTON = 36.dp

/** How wide the bar is for [pages] pages, in dp, before the screen has its say. */
internal fun editBarWidthDp(pages: Int): Float = maxOf(300f, 220f + pages * 14f)

/**
 * Edit mode's controls, at your thumb: one bar of glass just above the dock, stretched out of the
 * Search pill as the search field is. "+" adds a widget, the brush opens Customise, the dots are the
 * pages (tap for all of them), and the blue Done ends it. The status bar is left alone.
 *
 * It grows out of the pill's own size and shrinks back to it, on a spring, with its buttons arriving
 * one after another; with movement reduced it fades. It stands on the wallpaper, so its glass is the
 * same in Light and in Dark.
 */
@Composable
internal fun EditBar(
    visible: Boolean,
    pageCount: Int,
    /** The page in view, and how far towards the next: read while drawing. */
    position: () -> Float,
    onAdd: () -> Unit,
    onCustomise: () -> Unit,
    onPages: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduced = LocalMotionLevel.current.reduced
    val grown = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (reduced) grown.animateTo(if (visible) 1f else 0f, tween(Motion.REDUCED_MS))
        else grown.animateTo(if (visible) 1f else 0f, spring(dampingRatio = if (visible) 0.72f else 1f, stiffness = 360f))
    }
    if (!visible && grown.value <= 0.01f) return
    val pill = HomeGeometry.SEARCH_PILL_HEIGHT_DP.dp
    Box(
        modifier
            .layout { measurable, constraints ->
                // From the pill's size to the bar's: laid out again as it grows, composing nothing.
                val t = if (reduced) 1f else grown.value
                val full = editBarWidthDp(pageCount).dp.toPx().coerceAtMost(constraints.maxWidth - 32.dp.toPx())
                val width = (108.dp.toPx() + (full - 108.dp.toPx()) * t).roundToInt().coerceAtLeast(1)
                val height = (pill.toPx() + (BAR_HEIGHT.toPx() - pill.toPx()) * t).roundToInt().coerceAtLeast(1)
                val placeable = measurable.measure(Constraints.fixed(width, height))
                layout(width, height) { placeable.place(0, 0) }
            }
            .graphicsLayer { alpha = if (reduced) grown.value.coerceIn(0f, 1f) else (grown.value * 4f).coerceIn(0f, 1f) }
            .glass(
                ZenithShapes.pill,
                material = GlassMaterial.Clear,
                wallpaperTint = true,
                tracking = { grown.value },
            )
            .semantics { contentDescription = "Edit controls" },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp)
                .graphicsLayer {
                    // The buttons come once there is room for them.
                    alpha = ((grown.value - 0.45f) / 0.4f).coerceIn(0f, 1f)
                    val s = 0.7f + 0.3f * grown.value.coerceIn(0f, 1.1f)
                    scaleX = s
                    scaleY = s
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarButton(Icons.Rounded.Add, "Add Widget", onAdd)
            BarButton(Icons.Rounded.Brush, "Customise", onCustomise)
            BarDots(pageCount, position, onPages)
            DoneButton(onDone)
        }
    }
}

@Composable
private fun BarButton(icon: ImageVector, name: String, onClick: () -> Unit) {
    val tick = rememberTick()
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        Modifier
            .size(BAR_BUTTON)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (pressed) 0.28f else 0.16f))
            .clickable(interactionSource = source, indication = null, role = Role.Button) {
                tick()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = name, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

/** The pages as dots, the one in view long: tap for all the pages. */
@Composable
private fun BarDots(pageCount: Int, position: () -> Float, onClick: () -> Unit) {
    val tick = rememberTick()
    val dot = 7.dp
    val gap = 7.dp
    val long = 20.dp
    Box(
        Modifier
            .height(BAR_BUTTON)
            .width(dot * (pageCount - 1).coerceAtLeast(0) + long + gap * (pageCount - 1).coerceAtLeast(0) + 24.dp)
            .clip(CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                tick()
                onClick()
            }
            .semantics { contentDescription = "Pages" }
            .drawBehind {
                val at = position().coerceIn(0f, (pageCount - 1).coerceAtLeast(0).toFloat())
                var x = 12.dp.toPx()
                val y = size.height / 2f
                for (page in 0 until pageCount) {
                    // How much this dot is the one in view: it stretches as the page comes.
                    val near = (1f - kotlin.math.abs(at - page)).coerceIn(0f, 1f)
                    val width = dot.toPx() + (long.toPx() - dot.toPx()) * near
                    drawRoundRect(
                        Color.White.copy(alpha = 0.45f + 0.55f * near),
                        topLeft = Offset(x, y - dot.toPx() / 2f),
                        size = Size(width, dot.toPx()),
                        cornerRadius = CornerRadius(dot.toPx() / 2f),
                    )
                    x += width + gap.toPx()
                }
            },
    )
}

@Composable
private fun DoneButton(onClick: () -> Unit) {
    val tick = rememberTick()
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.94f else 1f, spring(dampingRatio = 0.6f, stiffness = 600f), label = "donePress")
    Box(
        Modifier
            .height(BAR_BUTTON)
            .widthIn(min = 72.dp)
            .graphicsLayer {
                scaleX = press
                scaleY = press
            }
            .clip(CircleShape)
            .background(ACTION_BLUE)
            .clickable(interactionSource = source, indication = null, role = Role.Button) {
                tick()
                onClick()
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("Done", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** How big the bead is, and the room round it that still counts as touching it. */
internal val BEAD = 25.dp
internal val BEAD_TOUCH = 41.dp

/**
 * The minus on the corner of everything that can be taken off the home screen, while it is being
 * edited: a small bead that wells up on its corner, and asks before it removes anything. It is
 * given a box [BEAD_TOUCH] across, centred on the corner, so it is easy to hit; the bead is drawn in
 * the middle of that.
 */
@Composable
internal fun MinusBead(shown: Boolean, name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val reduced = LocalMotionLevel.current.reduced
    val up by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = if (reduced) tween(Motion.REDUCED_MS) else spring(dampingRatio = if (shown) 0.55f else 1f, stiffness = 420f),
        label = "bead",
    )
    if (!shown && up <= 0.01f) return
    val tick = rememberTick()
    Box(
        modifier
            .size(BEAD_TOUCH)
            .then(
                if (shown) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                    ) {
                        tick()
                        onClick()
                    }
                } else {
                    Modifier
                }
            )
            .semantics { contentDescription = "Remove $name" }
            .graphicsLayer {
                alpha = up.coerceIn(0f, 1f)
                val s = if (reduced) 1f else 0.2f + 0.8f * up
                scaleX = s
                scaleY = s
            }
            .drawBehind {
                val r = BEAD.toPx() / 2f
                val c = Offset(size.width / 2f, size.height / 2f)
                // A soft shadow, the pale bead, a bright rim on its upper edge, and the minus.
                drawCircle(Color.Black.copy(alpha = 0.22f), radius = r + 1.dp.toPx(), center = c + Offset(0f, 1.5.dp.toPx()))
                drawCircle(Color(0xFFEEF0F6), radius = r, center = c)
                drawCircle(Color.White, radius = r - 0.5.dp.toPx(), center = c, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()), alpha = 0.9f)
                val half = 5.5.dp.toPx()
                val thick = 2.5.dp.toPx()
                drawRoundRect(
                    Color(0xFF1C1C1E),
                    topLeft = Offset(c.x - half, c.y - thick / 2f),
                    size = Size(half * 2f, thick),
                    cornerRadius = CornerRadius(thick / 2f),
                )
            },
    )
}

/** A few words said over the home screen for a moment, with one thing to do about them, or none. */
data class HomeToast(val words: String, val action: String? = null, val onAction: () -> Unit = {})

/**
 * [toast] in a capsule of glass under the status bar: what an Android toast would say, in Zenith's
 * own glass, and able to carry a button ("The layout is locked", "Unlock"). The home screen takes it
 * away again after a moment.
 */
@Composable
internal fun GlassToast(toast: HomeToast?, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val reduced = LocalMotionLevel.current.reduced
    val last = remember { arrayOfNulls<HomeToast>(1) }
    if (toast != null) last[0] = toast
    val up by animateFloatAsState(
        targetValue = if (toast != null) 1f else 0f,
        animationSpec = if (reduced) tween(Motion.REDUCED_MS) else spring(dampingRatio = 0.7f, stiffness = 380f),
        label = "toast",
    )
    val said = last[0]
    if (said == null || (toast == null && up <= 0.01f)) return
    Box(modifier.fillMaxWidth().statusBarsPadding().padding(top = 12.dp), contentAlignment = Alignment.TopCenter) {
        Row(
            Modifier
                .padding(horizontal = 24.dp)
                .graphicsLayer {
                    alpha = up.coerceIn(0f, 1f)
                    if (!reduced) {
                        translationY = -14.dp.toPx() * (1f - up)
                        val s = 0.92f + 0.08f * up
                        scaleX = s
                        scaleY = s
                    }
                }
                .bodyWithoutWallpaper(22.dp)
                .glass(RoundedCornerShape(22.dp), strength = 1.2f, solid = 0.5f, tracking = { up })
                .padding(start = 16.dp, end = if (said.action != null) 6.dp else 16.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                said.words,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false).padding(vertical = 6.dp),
            )
            if (said.action != null) {
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(ACTION_BLUE)
                        .clickable(enabled = toast != null, role = Role.Button) {
                            said.onAction()
                            onDone()
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(said.action, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}

/** How far a wiggling thing rocks, in degrees: an icon, and anything bigger, whose corners swing further. */
internal const val WIGGLE_ICON = 2f
internal const val WIGGLE_WIDE = 0.7f

/** How fast: about four swings a second, each thing a little off its neighbours' pace. */
internal const val WIGGLE_HZ = 3.7f
internal const val WIGGLE_HZ_SPREAD = 0.5f

/** And it bobs a hair as it rocks, on another beat. */
internal val WIGGLE_BOB: Dp = 0.6.dp
internal const val BOB_HZ = 3.2f
