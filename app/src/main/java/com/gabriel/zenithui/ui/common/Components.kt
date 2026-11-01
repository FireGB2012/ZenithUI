package com.gabriel.zenithui.ui.common

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.Badges
import com.gabriel.zenithui.data.IconLoader
import com.gabriel.zenithui.data.IconSource
import com.gabriel.zenithui.data.IconStyles
import com.gabriel.zenithui.data.ShortcutKeys
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.GlassMaterial
import com.gabriel.zenithui.ui.theme.glass
import kotlin.math.cos
import kotlin.math.sin

val LocalIconLoader = staticCompositionLocalOf<IconLoader> { error("IconLoader not provided") }

/** Slow drifting light, used behind onboarding and the app drawer. */
@Composable
fun AuroraBackdrop(modifier: Modifier = Modifier, intensity: Float = 1f) {
    val palette = LocalPalette.current
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(26_000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val accent = palette.accent
    val accentAlt = palette.accentAlt
    val tint = palette.glassTint

    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(palette.backdrop))
            .drawBehind {
                val orbs = listOf(
                    Triple(accent, Offset(0.22f + 0.10f * cos(phase), 0.16f + 0.06f * sin(phase * 1.3f)), 0.80f),
                    Triple(accentAlt, Offset(0.86f + 0.08f * sin(phase * 0.8f), 0.36f + 0.09f * cos(phase)), 0.66f),
                    Triple(tint, Offset(0.44f + 0.12f * cos(phase * 0.6f), 0.88f + 0.05f * sin(phase)), 0.95f),
                )
                orbs.forEach { (color, unit, radiusFraction) ->
                    val center = Offset(unit.x * size.width, unit.y * size.height)
                    val radius = radiusFraction * size.width
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = 0.32f * intensity), Color.Transparent),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        center = center,
                    )
                }
            }
    )
}

/** A super icon: the app's icon stretched to fill [modifier]'s width, as HyperOS draws them. */
@Composable
fun WideAppIcon(key: String, label: String, modifier: Modifier = Modifier) {
    val loader = LocalIconLoader.current
    val version by loader.version.collectAsState()
    val palette = LocalPalette.current
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.roundToPx() }
        val heightPx = with(density) { maxHeight.roundToPx() }
        val ready = remember(key, widthPx, heightPx, version) { loader.peekWide(key, widthPx, heightPx) }
        val bitmap by produceState(initialValue = ready, key, widthPx, heightPx, version) {
            value = loader.loadWide(key, widthPx, heightPx) ?: value
        }
        val image = bitmap
        val tall = maxHeight
        if (image != null) {
            Image(bitmap = image, contentDescription = label, modifier = Modifier.fillMaxSize())
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(tall * 0.26f))
                    .background(palette.onGlass.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(key = key, label = label, size = tall * 0.8f)
            }
        }
    }
}

/** How icons are drawn everywhere: one of [IconStyles]. */
val LocalIconStyle = staticCompositionLocalOf { IconStyles.ORIGINAL }

/** The names under icons: how big, and whether they have a shadow. */
@Immutable
data class LabelStyle(val size: TextUnit = 11.sp, val shadow: Boolean = false)

val LocalLabelStyle = staticCompositionLocalOf { LabelStyle() }

/** What each app has waiting, and how a dot shows it. Changes often, so only what reads it redraws. */
@Immutable
data class BadgeState(val counts: Map<String, Int> = emptyMap(), val style: String = Badges.OFF) {
    fun forKey(key: String): Int = if (style == Badges.OFF || ShortcutKeys.isShortcut(key)) 0 else Badges.forKey(key, counts)
    fun forKeys(keys: List<String>): Int = if (style == Badges.OFF) 0 else Badges.total(keys, counts)
}

val LocalBadges = compositionLocalOf { BadgeState() }

/** An app's name under its icon, at the size and with the shadow asked for in Settings. */
@Composable
fun IconLabel(text: String, modifier: Modifier = Modifier, textAlign: TextAlign? = null) {
    val palette = LocalPalette.current
    val style = LocalLabelStyle.current
    Text(
        text = text,
        color = palette.onGlass,
        fontSize = style.size,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        fontWeight = FontWeight.Medium,
        style = if (style.shadow) LABEL_SHADOW.copy(fontFamily = LocalZenithFont.current) else TextStyle(fontFamily = LocalZenithFont.current),
        modifier = modifier,
    )
}

private val LABEL_SHADOW = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 1.5f), 7f))

@Composable
fun AppIcon(
    key: String,
    label: String,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = ZenithShapes.icon,
    /** Shows the app's notification dot, where dots are on. */
    badge: Boolean = false,
    /** Already over glass or the drawer, as in a folder: a glass icon gets a pale plate, not a pane of its own. */
    onGlass: Boolean = false,
) {
    val loader = LocalIconLoader.current
    val version by loader.version.collectAsState()
    val px = with(LocalDensity.current) { size.roundToPx() }.coerceIn(48, 288)
    val style = LocalIconStyle.current
    // An icon somebody chose is shown as they chose it, whatever the style.
    val styled = style != IconStyles.ORIGINAL && !IconSource.isSource(key) && !loader.hasChosen(key)

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (styled) {
            StyledIcon(key = key, label = label, px = px, shape = shape, style = style, onGlass = onGlass, version = version)
        } else {
            PlainIcon(key = key, label = label, size = size, px = px, shape = shape, version = version)
        }
        if (badge) IconBadge(count = LocalBadges.current.forKey(key), iconSize = size)
    }
}

/**
 * The app's symbol on a tile: in the accent on the theme's tint, or in the ink of the page on a
 * tile of clear glass. Its symbol fades in once it is drawn, over the tile, which is there at once.
 */
@Composable
private fun StyledIcon(key: String, label: String, px: Int, shape: Shape, style: String, onGlass: Boolean, version: Int) {
    val loader = LocalIconLoader.current
    val palette = LocalPalette.current
    val ready = remember(key, px, version) { loader.peekGlyph(key, px) }
    val glyph by produceState(initialValue = ready, key, px, version) {
        value = loader.loadGlyph(key, px) ?: value
    }
    val appear by animateFloatAsState(if (glyph != null) 1f else 0f, tween(340), label = "glyphFade")
    val glass = style == IconStyles.GLASS
    val ink = when {
        glass -> palette.onGlass
        palette.dark -> lerp(palette.accent, Color.White, 0.42f)
        else -> lerp(palette.accent, Color.Black, 0.38f)
    }
    val tile = when {
        // Glass inside glass would show the wallpaper through a hole in it: a pale plate instead.
        glass && onGlass -> Modifier.clip(shape).background(palette.onGlass.copy(alpha = 0.12f))
        glass -> Modifier.glass(shape, strength = 1.2f, material = GlassMaterial.Clear, interactive = true)
        palette.dark -> Modifier.clip(shape).background(lerp(Color(0xFF14151B), palette.accent, 0.22f))
        else -> Modifier.clip(shape).background(lerp(Color.White, palette.accent, 0.18f))
    }
    Box(Modifier.fillMaxSize().then(tile), contentAlignment = Alignment.Center) {
        val image = glyph
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = label,
                colorFilter = ColorFilter.tint(ink),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = appear
                        val s = 0.92f + 0.08f * appear
                        scaleX = s
                        scaleY = s
                    },
            )
        }
    }
}

/**
 * A notification dot: a small bead of glass in the accent at the icon's top corner, with the count
 * in it if counts are asked for. It springs up with the first notification and shrinks away with
 * the last, still saying what it said as it goes.
 */
@Composable
fun BoxScope.IconBadge(count: Int, iconSize: Dp) {
    val badges = LocalBadges.current
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    val shown = count > 0 && badges.style != Badges.OFF
    val said = remember { intArrayOf(0) }
    if (count > 0) said[0] = count
    val grow by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = if (reduced) tween(Motion.REDUCED_MS) else Motion.bouncy(),
        label = "badge",
    )
    if (grow <= 0.001f && !shown) return
    val counting = badges.style == Badges.COUNT
    val side = iconSize * (if (counting) 0.34f else 0.24f)
    val words = Badges.text(said[0])
    val width = if (counting && words.length > 1) side * (0.55f + 0.32f * words.length) else side
    val accent = palette.accent
    Box(
        Modifier
            .align(Alignment.TopEnd)
            .offset(x = side * 0.2f, y = -side * 0.2f)
            .graphicsLayer {
                scaleX = grow
                scaleY = grow
                alpha = grow.coerceIn(0f, 1f)
            }
            .size(width, side)
            .drawBehind {
                // A bead: deeper at its foot, a rim of light round it and a spot of light near its top.
                val round = CornerRadius(size.height / 2f)
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(lerp(accent, Color.White, 0.35f), accent, lerp(accent, Color.Black, 0.25f))),
                    cornerRadius = round,
                )
                drawRoundRect(color = Color.White.copy(alpha = 0.55f), cornerRadius = round, style = Stroke(width = size.height * 0.07f))
                drawOval(
                    color = Color.White.copy(alpha = 0.5f),
                    topLeft = Offset(size.width * 0.24f, size.height * 0.12f),
                    size = Size(size.width * 0.52f, size.height * 0.28f),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (counting) {
            Text(
                text = words,
                color = Color(0xFF0A0B12),
                fontSize = (side.value * 0.56f).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PlainIcon(key: String, label: String, size: Dp, px: Int, shape: Shape, version: Int) {
    val loader = LocalIconLoader.current
    // Starts from the icon already to hand, so returning home or scrolling never flashes a blank.
    val ready = remember(key, px, version) { loader.peek(key, px) }
    val bitmap by produceState(initialValue = ready, key, px, version) {
        value = loader.load(key, px) ?: value
    }
    val palette = LocalPalette.current

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val bmp = bitmap
        val appear by animateFloatAsState(
            targetValue = if (bmp != null) 1f else 0f,
            animationSpec = tween(340),
            label = "iconFade",
        )
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = label,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = appear
                        val s = 0.92f + 0.08f * appear
                        scaleX = s
                        scaleY = s
                    },
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(
                        Brush.linearGradient(
                            listOf(palette.accent.copy(alpha = 0.55f), palette.accentAlt.copy(alpha = 0.35f))
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (size.value * 0.36f).sp,
                )
            }
        }
    }
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    val palette = LocalPalette.current
    val contentAlpha = if (enabled) 1f else 0.4f
    Box(
        modifier = modifier
            .height(56.dp)
            .pressBounce(0.96f, enabled = enabled)
            .graphicsLayer { alpha = contentAlpha }
            .then(
                if (filled) Modifier
                    .clip(ZenithShapes.pill)
                    .background(Brush.horizontalGradient(listOf(palette.accent, palette.accentAlt)))
                else Modifier.glass(ZenithShapes.pill, strength = 0.9f, material = GlassMaterial.Clear)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val contentColor = if (filled) Color(0xFF0A0B12) else palette.onGlass
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 28.dp),
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                leading?.invoke()
                Text(
                    text = text,
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** The heading over a block of a page: small capitals, spaced out, in the muted ink. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Text(
        text = text.uppercase(),
        color = palette.onGlassMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.4.sp,
        modifier = modifier,
    )
}

val PillShape: Shape = RoundedCornerShape(percent = 50)
