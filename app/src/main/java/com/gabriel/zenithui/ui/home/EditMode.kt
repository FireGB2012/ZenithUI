package com.gabriel.zenithui.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.gabriel.zenithui.ui.theme.LocalGlassRing
import com.gabriel.zenithui.ui.theme.corners
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalTheme
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import com.gabriel.zenithui.ui.theme.isGlass
import com.gabriel.zenithui.ui.theme.wallPaneInk
import kotlin.math.roundToInt

/**
 * The one main action on a pane on the wallpaper. In a glass theme it is the theme's accent as Dark
 * has it, since glass on the wallpaper is made as Dark makes it, in Light too: iOS's blue in Liquid
 * Glass, Aurora's green, Frost's ice blue. Elsewhere it is the theme's accent.
 */
@Composable
@ReadOnlyComposable
private fun actionColour(): Color {
    val theme = LocalTheme.current
    return if (theme.isGlass) Color(theme.dark.accent) else LocalPalette.current.accent
}

/** The words on [actionColour]. */
@Composable
@ReadOnlyComposable
private fun onActionColour(): Color {
    val theme = LocalTheme.current
    return if (theme.isGlass) Color(theme.dark.onAccent) else LocalPalette.current.onAccent
}

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
    /** Done was tapped, its middle here on the screen. */
    onDone: (Offset) -> Unit,
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
    val ink = wallPaneInk()
    Box(
        Modifier
            .size(BAR_BUTTON)
            .clip(CircleShape)
            .background(ink.copy(alpha = if (pressed) 0.28f else 0.16f))
            .clickable(interactionSource = source, indication = null, role = Role.Button) {
                tick()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = name, tint = ink, modifier = Modifier.size(20.dp))
    }
}

/** The pages as dots, the one in view long: tap for all the pages. */
@Composable
private fun BarDots(pageCount: Int, position: () -> Float, onClick: () -> Unit) {
    val tick = rememberTick()
    val dot = 7.dp
    val gap = 7.dp
    val long = 20.dp
    val ink = wallPaneInk()
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
                        ink.copy(alpha = 0.45f + 0.55f * near),
                        topLeft = Offset(x, y - dot.toPx() / 2f),
                        size = Size(width, dot.toPx()),
                        cornerRadius = CornerRadius(dot.toPx() / 2f),
                    )
                    x += width + gap.toPx()
                }
            },
    )
}

/** Done, which says where its middle is on the screen as it is tapped: the calm wave starts there. */
@Composable
private fun DoneButton(onClick: (Offset) -> Unit) {
    val tick = rememberTick()
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.94f else 1f, spring(dampingRatio = 0.6f, stiffness = 600f), label = "donePress")
    val middle = remember { arrayOf(Offset.Zero) }
    Box(
        Modifier
            .height(BAR_BUTTON)
            .widthIn(min = 72.dp)
            .onGloballyPositioned { middle[0] = it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f) }
            .graphicsLayer {
                scaleX = press
                scaleY = press
            }
            .clip(CircleShape)
            .background(actionColour())
            .clickable(interactionSource = source, indication = null, role = Role.Button) {
                tick()
                onClick(middle[0])
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("Done", color = onActionColour(), fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** How big the bead is, and the room round it that still counts as touching it. */
internal val BEAD = 25.dp
internal val BEAD_TOUCH = 41.dp

/**
 * How awake one thing on the home screen is to its being edited: whether it wears its bead
 * ([awake]), how much of its wiggle it has ([level], 0 to 1), and how far through the jolt it gave on
 * waking it is ([jolt], 1 at rest). The last two are read while drawing.
 */
@Stable
internal class EditWake(awake: Boolean) {
    var awake by mutableStateOf(awake)
    val level = Animatable(if (awake) 1f else 0f)
    val jolt = Animatable(1f)
}

/**
 * Wakes this thing as editing starts, and settles it as editing ends, when the wave of light
 * ([LocalGlassRing]) reaches its [middle] on the screen: it starts to wiggle with a small jolt and its
 * bead wells up, or its wiggle dies away like water going still and its bead sinks. At once with no
 * wave, with movement reduced, or off the screen. One first seen while editing is simply awake.
 */
@Composable
internal fun rememberEditWake(editing: Boolean, middle: () -> Offset?): EditWake {
    val ring = LocalGlassRing.current
    val reduced = LocalMotionLevel.current.reduced
    val wake = remember { EditWake(editing) }
    LaunchedEffect(editing) {
        if (editing == wake.awake) return@LaunchedEffect
        val at = middle()
        val wait = if (reduced || ring == null || at == null) 0f else ring.reachesIn(at, withFrameNanos { it })
        if (wait >= 1f) delay(wait.toLong())
        wake.awake = editing
        if (editing) {
            launch { wake.level.animateTo(1f, tween(if (reduced) 0 else BeadMotion.WAKE_MS)) }
            if (!reduced) {
                wake.jolt.snapTo(0f)
                wake.jolt.animateTo(1f, tween(BeadMotion.JOLT_MS, easing = LinearEasing))
            }
        } else {
            wake.jolt.snapTo(1f)
            wake.level.animateTo(0f, tween(if (reduced) 0 else BeadMotion.CALM_MS, easing = LinearOutSlowInEasing))
        }
    }
    return wake
}

/**
 * The minus on the corner of everything that can be taken off the home screen, while it is being
 * edited: a bead of glass that wells up out of the corner like a drop, magnifying the corner under
 * it, and asks before it removes anything. It is given a box [BEAD_TOUCH] across, centred on the
 * corner, so it is easy to hit; the bead is drawn in the middle of that.
 *
 * [lens] is what the bead sits on, drawn into a layer, and [corner] where in that layer the bead's
 * middle is: the bead shows that corner a quarter bigger, as a drop of glass would. Without one the
 * bead is clear glass over the wallpaper.
 */
@Composable
internal fun MinusBead(
    shown: Boolean,
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    lens: GraphicsLayer? = null,
    corner: Offset = Offset.Zero,
) {
    val reduced = LocalMotionLevel.current.reduced
    // Welling up, 0 to 1, and sinking back, 0 to 1; one already up when it is first seen, on a page
    // turned to in edit mode, is simply there.
    val up = remember { Animatable(if (shown) 1f else 0f) }
    val down = remember { Animatable(0f) }
    LaunchedEffect(shown) {
        if (shown) {
            if (down.value > 0f) up.snapTo(0f)
            down.snapTo(0f)
            up.animateTo(1f, tween(if (reduced) Motion.REDUCED_MS else BeadMotion.WELL_UP_MS, easing = LinearEasing))
        } else if (up.value > 0f) {
            down.animateTo(1f, tween(if (reduced) Motion.REDUCED_MS else BeadMotion.SINK_MS, easing = LinearEasing))
            up.snapTo(0f)
            down.snapTo(0f)
        }
    }
    if (!shown && up.value <= 0f) return
    val tick = rememberTick()
    val shade = remember { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG) }
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
                val pose = when {
                    reduced -> BeadMotion.Pose(up.value * (1f - down.value), 0f, 0f, 1f, 1f)
                    down.value > 0f -> BeadMotion.sink(down.value)
                    else -> BeadMotion.wellUp(up.value)
                }
                alpha = pose.alpha
                translationX = pose.dx.dp.toPx()
                translationY = pose.dy.dp.toPx()
                scaleX = pose.scaleX
                scaleY = pose.scaleY
            }
            .drawBehind { drawBead(lens, corner, shade) },
    )
}

/** A bead of glass with a minus in it: see [MinusBead]. */
private fun DrawScope.drawBead(lens: GraphicsLayer?, corner: Offset, shade: android.graphics.Paint) {
    val r = BEAD.toPx() / 2f
    val c = Offset(size.width / 2f, size.height / 2f)
    // It floats a little off what it sits on, as Apple's glass does: one soft shadow, straight down.
    drawIntoCanvas {
        shade.color = android.graphics.Color.TRANSPARENT
        shade.setShadowLayer(3.dp.toPx(), 0f, 1.dp.toPx(), android.graphics.Color.argb(46, 0, 0, 0))
        it.nativeCanvas.drawCircle(c.x, c.y, r - 0.5f, shade)
    }
    val round = Path().apply { addOval(Rect(c, r)) }
    clipPath(round) {
        // Pale glass, and through it the corner it sits on, a quarter bigger.
        drawRect(BEAD_GLASS)
        if (lens != null) {
            withTransform({
                translate(c.x, c.y)
                scale(LENS_POWER, LENS_POWER, pivot = Offset.Zero)
                translate(-corner.x, -corner.y)
            }) {
                drawLayer(lens)
            }
            // The glass keeps its paleness over what it shows, so it is the same grey bead over
            // any corner and the minus reads on a dark one as on a light one.
            drawRect(BEAD_GLASS.copy(alpha = BEAD_OVER_LENS))
        }
        // Where the glass turns away at its edge it bends the light: a narrow band there, a touch
        // brighter than its flat face. No shading, no highlight on the face: it is a sheet.
        drawCircle(
            Brush.radialGradient(
                0.7f to Color.White.copy(alpha = 0f),
                1f to Color.White.copy(alpha = 0.22f),
                center = c,
                radius = r,
            ),
            radius = r,
            center = c,
        )
    }
    // Its rim: a hairline of light, brightest on the edge facing the light (up and to the left) and
    // again on the edge opposite, and faint between, as on all of Zenith's glass.
    drawCircle(
        Brush.linearGradient(
            0f to Color.White.copy(alpha = 0.95f),
            0.35f to Color.White.copy(alpha = 0.28f),
            0.65f to Color.White.copy(alpha = 0.28f),
            1f to Color.White.copy(alpha = 0.7f),
            start = c - Offset(r, r) * 0.71f,
            end = c + Offset(r, r) * 0.71f,
        ),
        radius = r - 0.5.dp.toPx(),
        center = c,
        style = Stroke(1.dp.toPx()),
    )
    val half = 5.5.dp.toPx()
    val thick = 2.5.dp.toPx()
    drawRoundRect(
        MINUS_INK,
        topLeft = Offset(c.x - half, c.y - thick / 2f),
        size = Size(half * 2f, thick),
        cornerRadius = CornerRadius(thick / 2f),
    )
}

/** The bead's own glass, pale and a little see-through, as iOS's minus is. */
private val BEAD_GLASS = Color(0x9EEEF0F6)
private val MINUS_INK = Color(0xFF1C1C1E)

/** How much of the bead's paleness lies over the corner it magnifies. */
private const val BEAD_OVER_LENS = 0.45f

/** How much bigger a bead shows the corner under it. */
private const val LENS_POWER = 1.25f

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
                .glass(corners(22.dp), strength = 1.2f, solid = 0.5f, tracking = { up })
                .padding(start = 16.dp, end = if (said.action != null) 6.dp else 16.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                said.words,
                color = wallPaneInk(),
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
                        .background(actionColour())
                        .clickable(enabled = toast != null, role = Role.Button) {
                            said.onAction()
                            onDone()
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(said.action, color = onActionColour(), fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
