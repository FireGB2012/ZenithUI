package com.gabriel.zenithui.ui.connect

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.theme.LocalLite
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalTheme
import com.gabriel.zenithui.ui.theme.glassOnGlass
import com.gabriel.zenithui.ui.theme.isGlass
import com.gabriel.zenithui.ui.theme.themeSurface
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

// The parts Connect is built from, in Zenith's look: the theme's colours and type, glass where
// Zenith has glass, iOS blue (or the colour chosen in the Vault) for what you press to go on.

/** Connect's colour for actions and highlights, and the colour of words on it. */
internal val LocalConnectAccent = staticCompositionLocalOf { Color(0xFF0A84FF) }
internal val LocalConnectOnAccent = staticCompositionLocalOf { Color.White }

/** Words on [colour]: white on a deep one, Connect's ink on a pale one, as the prototype's lilac had. */
internal fun inkOn(colour: Color): Color = if (colour.luminance() > 0.45f) Color(0xFF0B0C12) else Color.White

/** The heart's pink, iOS's, a shade deeper on a pale page. */
@Composable
@ReadOnlyComposable
internal fun heartPink(): Color = if (LocalPalette.current.dark) Color(0xFFFF375F) else Color(0xFFFF2D55)

/** Live, online and Tester: iOS's green, as Zenith's switches are. */
@Composable
@ReadOnlyComposable
internal fun liveGreen(): Color = LocalPalette.current.switchOn

/**
 * What lies under Connect's floating chrome: its page as it was drawn this frame, recorded into a
 * layer as it is drawn, and where that page is in the window. The tab bar, the buttons that float
 * over the page and the toast blur it, as iOS's glass does what scrolls under it (see [connectGlass]).
 */
internal class ConnectBackdrop {
    var layer: GraphicsLayer? = null

    /** Where the page is laid out, which the chrome over it is measured from. */
    var host: androidx.compose.ui.layout.LayoutCoordinates? = null
}

internal val LocalConnectBackdrop = staticCompositionLocalOf<ConnectBackdrop?> { null }

/**
 * Glass for what floats over Connect's own page. Zenith's glass bends the wallpaper, and over a
 * page it would show the wallpaper through a hole in it, so this one is made from the page: what
 * is under it, blurred and lifted a little, in the hairline of light and soft shadow Zenith's glass
 * on glass has ([glassOnGlass]), moving as the phone tilts.
 *
 * The blur is the page under the pane only, a little past its edges, so it costs no more than the
 * pane's own size. In Lite, and before Android 12, which has no blur to draw it with, the pane is
 * the page's own colour frosted instead: solid enough that the words under it never fight its own.
 */
@Composable
internal fun Modifier.connectGlass(shape: Shape, interactive: Boolean = false): Modifier {
    val backdrop = LocalConnectBackdrop.current
    val palette = LocalPalette.current
    val dark = palette.dark
    val glass = LocalTheme.current.isGlass
    val blurs = glass && backdrop != null && !LocalLite.current && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val under = if (blurs) rememberGraphicsLayer() else null
    val density = LocalDensity.current
    val radius = with(density) { BLUR.toPx() }
    // Where the pane is in the window, for lining the page up under it. Read while drawing, so a
    // pane that moves (a sheet coming up, the tab bar going) looks again at what is under it.
    var at by remember { mutableStateOf(Offset.Zero) }
    val page = pageColour()
    val base = if (glass) (if (dark) Color(0xFF16161C) else Color(0xFFF2F2F7)) else page
    val veil = if (dark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.38f)
    val frost = base.copy(alpha = if (glass) (if (dark) 0.9f else 0.92f) else 0.96f)
    if (under != null) {
        remember(under, radius) { under.renderEffect = BlurEffect(radius, radius, TileMode.Clamp); Unit }
    }
    return this
        .onGloballyPositioned { pane ->
            val host = backdrop?.host?.takeIf { it.isAttached }
            at = if (host != null && pane.isAttached) host.localPositionOf(pane, Offset.Zero) else pane.positionInWindow()
        }
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val path = Path().apply { addOutline(outline) }
            onDrawBehind {
                val layer = backdrop?.layer
                val where = at
                if (under != null && layer != null) {
                    val pad = radius * 2f
                    under.record(IntSize((size.width + pad * 2f).toInt(), (size.height + pad * 2f).toInt())) {
                        // Opaque under the page, so the blur pulls in no see-through edges.
                        drawRect(base)
                        translate(left = pad - where.x, top = pad - where.y) {
                            drawLayer(layer)
                        }
                    }
                    under.topLeft = IntOffset(-pad.toInt(), -pad.toInt())
                    clipPath(path) { drawLayer(under) }
                    drawOutline(outline, veil)
                } else {
                    drawOutline(outline, frost)
                }
            }
        }
        .glassOnGlass(shape, dark, interactive)
}

/**
 * Glass for what lies on Connect's own page, not over it: a button in a page's head, the field
 * search types into. It cannot see the page it is part of, so it is a faint wash of the page's ink
 * in Zenith's hairline of light, moving as the phone tilts, as glass lying on glass is.
 */
@Composable
internal fun Modifier.inPageGlass(shape: Shape, interactive: Boolean = false): Modifier {
    val palette = LocalPalette.current
    val wash = if (palette.dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.72f)
    return this
        .clip(shape)
        .background(wash)
        .glassOnGlass(shape, palette.dark, interactive)
}

/** The colour of Connect's page, as an app's own background: iOS's in Liquid Glass, the theme's own page otherwise. */
@Composable
@ReadOnlyComposable
internal fun pageColour(): Color {
    val palette = LocalPalette.current
    return if (LocalTheme.current.isGlass) pageBase() else palette.backdrop.firstOrNull() ?: pageBase()
}

/** What Connect's page is painted with: [pageColour], or the theme's page from top to bottom. */
@Composable
@ReadOnlyComposable
internal fun pageBrush(): Brush {
    val palette = LocalPalette.current
    if (LocalTheme.current.isGlass || palette.backdrop.size < 2) return androidx.compose.ui.graphics.SolidColor(pageColour())
    return Brush.verticalGradient(palette.backdrop)
}

/** How soft what is under the chrome is made. */
private val BLUR = 22.dp

/**
 * A card on Connect's page, as iOS lays its cards on an app's page: a faint lift of white with a
 * hairline round it on a dark page, white on a pale one, and the theme's own panel in a theme that
 * is not glass. [outline] rings it in a colour, as a setup just published is.
 */
@Composable
internal fun Modifier.connectCard(shape: Shape = CARD_SHAPE, outline: Color? = null): Modifier {
    val palette = LocalPalette.current
    val theme = LocalTheme.current
    if (!theme.isGlass) return themeSurface(shape).then(if (outline != null) Modifier.border(1.5.dp, outline, shape) else Modifier)
    val face = if (palette.dark) {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.075f), Color.White.copy(alpha = 0.035f)))
    } else {
        androidx.compose.ui.graphics.SolidColor(Color.White)
    }
    val edge = outline ?: if (palette.dark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.05f)
    return this
        .clip(shape)
        .background(face)
        .border(if (outline != null) 1.5.dp else 1.dp, edge, shape)
}

/** The faint fill of a quiet control on the page: a ghost button, a chip, a field. */
@Composable
@ReadOnlyComposable
internal fun quietFill(): Color = LocalPalette.current.let { if (it.dark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.045f) }

/** The hairline round a quiet control. */
@Composable
@ReadOnlyComposable
internal fun quietEdge(): Color = LocalPalette.current.let { if (it.dark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f) }

/** The prototype's coral, for bugs: deeper on a pale page so it can be read there. */
@Composable
@ReadOnlyComposable
internal fun coral(): Color = if (LocalPalette.current.dark) Color(0xFFFFA189) else Color(0xFFE8553A)

/** A button with no fill of its own: the page's ink on a faint wash, in a hairline. */
@Composable
internal fun GhostButton(text: String, modifier: Modifier = Modifier, height: Dp = 44.dp, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    Box(
        modifier
            .height(height)
            .pressBounce(0.95f)
            .clip(RoundedCornerShape(50))
            .background(quietFill())
            .border(1.dp, quietEdge(), RoundedCornerShape(50))
            .clickable(role = Role.Button) {
                tick()
                onClick()
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(targetState = text, transitionSpec = { fadeIn(tween(160)).togetherWith(fadeOut(tween(100))) }, label = "ghostText") { words ->
            Text(words, color = palette.onGlass, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

/** A word in small capitals, spaced out, over a heading: "SETUP OF THE WEEK". */
@Composable
internal fun Overline(text: String, modifier: Modifier = Modifier, colour: Color = LocalPalette.current.onGlassMuted) {
    Text(
        text.uppercase(),
        color = colour,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.4.sp,
        maxLines = 1,
        modifier = modifier,
    )
}

/** A large page title, as iOS heads an app's page: "Community". */
@Composable
internal fun PageTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, color = LocalPalette.current.onGlass, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp, maxLines = 1, modifier = modifier)
}

internal val CARD_SHAPE = RoundedCornerShape(24.dp)

/**
 * A round glass button: floating over the page ([floating]: back, share), or lying on it (search,
 * the accent sheet), each in the glass that fits (see [connectGlass] and [inPageGlass]).
 */
@Composable
internal fun RoundGlassButton(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color = LocalPalette.current.onGlass,
    rotation: Float = 0f,
    floating: Boolean = false,
    onClick: () -> Unit,
) {
    val tick = rememberTick()
    Box(
        modifier
            .size(size)
            .pressBounce(0.88f)
            .then(if (floating) Modifier.connectGlass(CircleShape, interactive = true) else Modifier.inPageGlass(CircleShape, interactive = true))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = description,
            ) {
                tick()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(size * 0.48f).graphicsLayer { rotationZ = rotation })
    }
}

/**
 * The button that moves you on, in the accent: a capsule with a fill that runs across it as
 * something it started gets done ([progress], 0 to 1, or null with nothing running).
 */
@Composable
internal fun AccentButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    progress: Float? = null,
    done: Boolean = false,
    height: Dp = 52.dp,
    onClick: () -> Unit,
) {
    val accent = LocalConnectAccent.current
    val onAccent = LocalConnectOnAccent.current
    val palette = LocalPalette.current
    val tick = rememberTick()
    val green = liveGreen()
    // Done, it turns green, as iOS's do once something has gone through.
    val face by animateColorAsState(
        when {
            done -> green
            enabled -> accent
            else -> palette.onGlass.copy(alpha = 0.12f)
        },
        tween(300),
        label = "accentFace",
    )
    val ink by animateColorAsState(
        when {
            done -> inkOn(green)
            enabled -> onAccent
            else -> palette.onGlass.copy(alpha = 0.4f)
        },
        tween(300),
        label = "accentInk",
    )
    Box(
        modifier
            .height(height)
            .pressBounce(0.96f, enabled = enabled)
            .clip(RoundedCornerShape(50))
            .background(face)
            .drawWithCache {
                val fill = Color(0xFF0B0C12).copy(alpha = 0.2f)
                onDrawBehind {
                    val p = progress ?: return@onDrawBehind
                    drawRect(fill, size = size.copy(width = size.width * p.coerceIn(0f, 1f)))
                }
            }
            .clickable(enabled = enabled, role = Role.Button) {
                tick()
                onClick()
            }
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
            }
            AnimatedContent(
                targetState = text,
                transitionSpec = { fadeIn(tween(160)).togetherWith(fadeOut(tween(100))) },
                label = "accentText",
            ) { words ->
                Text(words, color = ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

/** A quieter button: the accent's words on a faint wash of it. */
@Composable
internal fun SoftButton(text: String, modifier: Modifier = Modifier, height: Dp = 36.dp, onClick: () -> Unit) {
    val accent = LocalConnectAccent.current
    val tick = rememberTick()
    Box(
        modifier
            .height(height)
            .pressBounce(0.94f)
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.14f))
            .clickable(role = Role.Button) {
                tick()
                onClick()
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(targetState = text, transitionSpec = { fadeIn(tween(160)).togetherWith(fadeOut(tween(100))) }, label = "softText") { words ->
            Text(words, color = accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

/** A small label in a capsule: a kind of setup, a role, a status. */
@Composable
internal fun Tag(text: String, colour: Color, modifier: Modifier = Modifier, filled: Boolean = false) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) colour else colour.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, color = if (filled) inkOn(colour) else colour, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/**
 * A chip that is on or off, as iOS's do: on in the accent with a tick, off as a faint outline
 * with a plus, popping a little as it changes.
 */
@Composable
internal fun ToggleChip(
    text: String,
    on: Boolean,
    modifier: Modifier = Modifier,
    showMark: Boolean = true,
    height: Dp = 38.dp,
    onClick: () -> Unit,
) {
    val accent = LocalConnectAccent.current
    val palette = LocalPalette.current
    val tick = rememberTick()
    val face by animateColorAsState(if (on) accent.copy(alpha = 0.18f) else Color.Transparent, tween(180), label = "chipFace")
    val edge by animateColorAsState(if (on) accent.copy(alpha = 0.55f) else palette.onGlass.copy(alpha = 0.18f), tween(180), label = "chipEdge")
    val ink by animateColorAsState(if (on) accent else palette.onGlass.copy(alpha = 0.8f), tween(180), label = "chipInk")
    Row(
        modifier
            .height(height)
            .pressBounce(0.92f)
            .popOn(on)
            .clip(RoundedCornerShape(50))
            .background(face)
            .border(1.dp, edge, RoundedCornerShape(50))
            .clickable(role = Role.Checkbox) {
                tick()
                onClick()
            }
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (showMark) {
            Icon(
                if (on) Icons.Rounded.Check else Icons.Rounded.Add,
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(text, color = ink, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/**
 * A row of pills you pick one of, scrolling sideways when they do not fit: the chosen one sits on
 * a capsule of the accent that slides across to the next as it is chosen, and the row scrolls to
 * keep it in view. [dots] are the ones with something new in them.
 */
@Composable
internal fun <T> PillPicker(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    dots: Set<T> = emptySet(),
    onPick: (T) -> Unit,
) {
    val accent = LocalConnectAccent.current
    val onAccent = LocalConnectOnAccent.current
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    val tick = rememberTick()
    val scroll = rememberScrollState()
    // Each pill's place and width in the row, as laid out.
    val places = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    val chosen = options.indexOf(selected).coerceAtLeast(0)
    val x = remember { Animatable(0f) }
    val w = remember { Animatable(0f) }
    val placed = places[chosen]
    LaunchedEffect(placed) {
        val (toX, toW) = placed ?: return@LaunchedEffect
        if (w.value == 0f || reduced) {
            x.snapTo(toX)
            w.snapTo(toW)
        } else {
            coroutineScope {
                launch { x.animateTo(toX, spring(dampingRatio = 0.78f, stiffness = 380f)) }
                launch { w.animateTo(toW, spring(dampingRatio = 0.78f, stiffness = 380f)) }
            }
        }
        // Keep it in view, near the middle where the row allows.
        val target = (toX + toW / 2f - scroll.viewportSize / 2f).coerceIn(0f, scroll.maxValue.toFloat())
        if (reduced) scroll.scrollTo(target.toInt()) else scroll.animateScrollTo(target.toInt())
    }
    Box(modifier.horizontalScroll(scroll)) {
        Box(Modifier.padding(horizontal = 16.dp)) {
            // The capsule under the chosen pill, stretched a little while it travels.
            Canvas(Modifier.matchParentSize()) {
                if (w.value <= 0f) return@Canvas
                val travel = ((placed?.first ?: x.value) - x.value).let { kotlin.math.abs(it) }
                val stretch = (travel / (w.value * 2f)).coerceIn(0f, 0.25f)
                val width = w.value * (1f + stretch)
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(x.value - (width - w.value) / 2f, 0f),
                    size = androidx.compose.ui.geometry.Size(width, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEachIndexed { index, option ->
                    val on = index == chosen
                    val ink by animateColorAsState(if (on) onAccent else palette.onGlass.copy(alpha = 0.82f), tween(160), label = "pillInk")
                    Row(
                        Modifier
                            .height(36.dp)
                            .onGloballyPositioned { places[index] = it.positionInParent().x to it.size.width.toFloat() }
                            .pressBounce(0.94f)
                            .clip(RoundedCornerShape(50))
                            .background(if (on) Color.Transparent else palette.onGlass.copy(alpha = 0.07f))
                            .clickable(role = Role.Tab) {
                                if (!on) {
                                    tick()
                                    onPick(option)
                                }
                            }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (leading != null) {
                            Icon(leading, contentDescription = null, tint = ink.copy(alpha = ink.alpha * 0.8f), modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(label(option), color = ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        if (option in dots) {
                            Spacer(Modifier.width(6.dp))
                            Box(Modifier.size(7.dp).clip(CircleShape).background(liveGreen()))
                        }
                    }
                }
            }
        }
    }
}

/** Connect's mark: a dot in two rings, fading outwards, in [colour]. */
@Composable
internal fun ConnectMark(colour: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val unit = size.minDimension / 28f
        val middle = center
        drawCircle(colour, radius = 4f * unit, center = middle)
        drawCircle(colour.copy(alpha = colour.alpha * 0.55f), radius = 8.5f * unit, center = middle, style = Stroke(1.6f * unit))
        drawCircle(colour.copy(alpha = colour.alpha * 0.25f), radius = 12.5f * unit, center = middle, style = Stroke(1.6f * unit))
    }
}

/** A round picture of someone: their first letter in their colours, or with [ring], in the accent's ring. */
@Composable
internal fun Avatar(initial: String, bg: Color, fg: Color, size: Dp, modifier: Modifier = Modifier, ring: Color? = null) {
    Box(
        modifier
            .size(size)
            .then(if (ring != null) Modifier.border(2.dp, ring, CircleShape).padding(4.dp) else Modifier)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(initial, color = fg, fontSize = (size.value * 0.4f).sp, fontWeight = FontWeight.SemiBold)
    }
}

/** A heading over a group, with a small note or a button at its far end. */
@Composable
internal fun SectionHead(title: String, modifier: Modifier = Modifier, inset: Dp = 16.dp, trailing: @Composable (() -> Unit)? = null) {
    val palette = LocalPalette.current
    Row(modifier.padding(horizontal = inset), verticalAlignment = Alignment.CenterVertically) {
        AnimatedContent(
            targetState = title,
            transitionSpec = { (fadeIn(tween(220)) + androidx.compose.animation.slideInVertically(tween(220)) { it / 3 }).togetherWith(fadeOut(tween(120))) },
            label = "sectionTitle",
            modifier = Modifier.weight(1f),
        ) { words ->
            Text(words, color = palette.onGlass, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
        }
        trailing?.invoke()
    }
}

/** The small grey note at the end of a heading. */
@Composable
internal fun HeadNote(text: String) {
    Text(text, color = LocalPalette.current.onGlassMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
}

/** A thin bar that fills with the accent, 0 to 1, as something runs. */
@Composable
internal fun ProgressLine(progress: () -> Float, modifier: Modifier = Modifier, colour: Color = LocalConnectAccent.current) {
    val track = LocalPalette.current.onGlass.copy(alpha = 0.1f)
    Canvas(modifier.height(6.dp)) {
        val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
        drawRoundRect(track, cornerRadius = r)
        val p = progress().coerceIn(0f, 1f)
        if (p > 0f) drawRoundRect(colour, size = size.copy(width = size.width * p), cornerRadius = r)
    }
}

/** How a pressed thing grows back out, and how long a number takes to roll over. */
internal val POP = spring<Float>(dampingRatio = 0.55f, stiffness = 520f)

/** A number that rolls up or down to its new value as it changes. */
@Composable
internal fun RollingNumber(value: Int, colour: Color, modifier: Modifier = Modifier, size: Int = 13) {
    val reduced = LocalMotionLevel.current.reduced
    AnimatedContent(
        targetState = value,
        transitionSpec = { com.gabriel.zenithui.ui.common.rollOver(reduced, down = targetState < initialState) },
        label = "rolling",
        modifier = modifier,
    ) { n ->
        Text(com.gabriel.zenithui.data.connect.ConnectFeed.count(n), color = colour, fontSize = size.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** The page's own colour, under everything, so a picture of it taken to close it has no holes. */
@Composable
@ReadOnlyComposable
internal fun pageBase(): Color = if (LocalPalette.current.dark) Color(0xFF0B0B10) else Color(0xFFF2F2F7)

/** How long a toast stays up. */
internal const val TOAST_MS = 2_900L

/** A step of the motion Connect is built on, or none with movement reduced. */
@Composable
internal fun <T> connectSpring(reducedSpec: androidx.compose.animation.core.AnimationSpec<T> = tween(Motion.REDUCED_MS)): androidx.compose.animation.core.AnimationSpec<T> =
    if (LocalMotionLevel.current.reduced) reducedSpec else spring(dampingRatio = 0.82f, stiffness = 300f)

/** A slight pop as [trigger] changes: a chip turned on, a heart liked. */
@Composable
internal fun Modifier.popOn(trigger: Any?): Modifier {
    val reduced = LocalMotionLevel.current.reduced
    val scale = remember { Animatable(1f) }
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(trigger) {
        if (first[0]) {
            first[0] = false
            return@LaunchedEffect
        }
        if (reduced) return@LaunchedEffect
        scale.snapTo(0.82f)
        scale.animateTo(1f, POP)
    }
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
