package com.gabriel.zenithui.ui.common

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.core.animateFloatAsState
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
import com.gabriel.zenithui.ui.theme.LocalOnWallpaper
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.GlassMaterial
import com.gabriel.zenithui.ui.theme.GlassMotion
import com.gabriel.zenithui.ui.theme.glass
import com.gabriel.zenithui.ui.theme.inkOnTint

val LocalIconLoader = staticCompositionLocalOf<IconLoader> { error("IconLoader not provided") }

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

/**
 * The names under icons: how big, whether Name shadow is on, and whether they are written on the
 * wallpaper itself, as a home page's are, rather than on a surface of their own.
 */
@Immutable
data class LabelStyle(val size: TextUnit = 11.sp, val shadow: Boolean = false, val onWallpaper: Boolean = false)

val LocalLabelStyle = staticCompositionLocalOf { LabelStyle() }

/** What each app has waiting, and how a dot shows it. Changes often, so only what reads it redraws. */
@Immutable
data class BadgeState(val counts: Map<String, Int> = emptyMap(), val style: String = Badges.OFF) {
    fun forKey(key: String): Int = if (style == Badges.OFF || ShortcutKeys.isShortcut(key)) 0 else Badges.forKey(key, counts)
    fun forKeys(keys: List<String>): Int = if (style == Badges.OFF) 0 else Badges.total(keys, counts)
}

val LocalBadges = compositionLocalOf { BadgeState() }

/**
 * An app's name under its icon, at the size asked for in Settings: on the wallpaper white with a
 * soft shadow, as on an iPhone, and anywhere else in the palette's ink with the shadow Settings
 * asks for.
 */
@Composable
fun IconLabel(text: String, modifier: Modifier = Modifier, textAlign: TextAlign? = null) {
    val palette = LocalPalette.current
    val style = LocalLabelStyle.current
    val density = LocalDensity.current.density
    Text(
        text = text,
        color = labelInk(style.onWallpaper, palette.onGlass),
        fontSize = style.size,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        fontWeight = FontWeight.Medium,
        style = TextStyle(fontFamily = LocalZenithFont.current, shadow = labelShadow(style.onWallpaper, style.shadow, density)),
        modifier = modifier,
    )
}

/**
 * The ink a name is written in. On the wallpaper it is white whatever the palette: a light
 * palette's dark ink could not be read over a dark wallpaper. Anywhere else, the drawer and the
 * folder panel, it is the palette's own ink, [onGlass], which their surfaces are made for.
 */
internal fun labelInk(onWallpaper: Boolean, onGlass: Color): Color = if (onWallpaper) Color.White else onGlass

/**
 * The shadow under a name. On the wallpaper it is always a soft one, a dp below the words and
 * blurred across three, which keeps white words readable on a pale wallpaper without dulling them
 * on a dark one. Anywhere else it is the one Name shadow ([legacyShadow]) has always given, or none.
 */
internal fun labelShadow(onWallpaper: Boolean, legacyShadow: Boolean, density: Float): Shadow? = when {
    onWallpaper -> Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 1f * density), 3f * density)
    legacyShadow -> NAME_SHADOW
    else -> null
}

/** What Name shadow puts under the names, in pixels, as it always has. */
private val NAME_SHADOW = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 1.5f), 7f)

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
 * The app's symbol on a tile: in the words' ink on a plain tile, iOS's dark grey in Dark and white
 * in Light, or on a tile of clear glass in the ink of the page, and white on the wallpaper, where
 * the glass is the same in Light and in Dark. Its symbol fades in once it is drawn, over the tile,
 * which is there at once.
 */
@Composable
private fun StyledIcon(key: String, label: String, px: Int, shape: Shape, style: String, onGlass: Boolean, version: Int) {
    val loader = LocalIconLoader.current
    val palette = LocalPalette.current
    val onWallpaper = LocalOnWallpaper.current
    val ready = remember(key, px, version) { loader.peekGlyph(key, px) }
    val glyph by produceState(initialValue = ready, key, px, version) {
        value = loader.loadGlyph(key, px) ?: value
    }
    val appear by animateFloatAsState(if (glyph != null) 1f else 0f, tween(340), label = "glyphFade")
    val glass = style == IconStyles.GLASS
    // No theme's colour in it: the symbol is in the ink, whatever the tile. A glass tile on the
    // wallpaper is the wallpaper's clear glass in either mode, so its symbol is white, as the names
    // under it are; a plain tile is a surface of its own, in the mode's ink.
    val glassTileInk = if (onWallpaper) Color.White else palette.onGlass
    val ink = if (glass) glassTileInk else palette.onGlass
    val tile = when {
        // Glass inside glass would show the wallpaper through a hole in it: a pale plate instead.
        glass && onGlass -> Modifier.clip(shape).background(glassTileInk.copy(alpha = 0.12f))
        glass -> Modifier.glass(shape, strength = 1.2f, material = GlassMaterial.Clear, interactive = true)
        palette.dark -> Modifier.clip(shape).background(Color(0xFF1C1C1E))
        else -> Modifier.clip(shape).background(Color.White)
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
 * A notification dot: a small bead of glass in iOS's red at the icon's top corner, with the count
 * in white in it if counts are asked for, as an iPhone's badges are. It springs up with the first
 * notification and shrinks away with the last, still saying what it said as it goes.
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
    val red = palette.destructive
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
                    brush = Brush.verticalGradient(listOf(lerp(red, Color.White, 0.35f), red, lerp(red, Color.Black, 0.25f))),
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
                color = Color.White,
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
            // While the icon loads, a plain plate with the app's initial, as a super icon's is.
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(palette.onGlass.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label.take(1).uppercase(),
                    color = palette.onGlass,
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
    /** The button that does the thing: tinted glass. Kept for older callers; [style] says the same. */
    filled: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    style: GlassButtonStyle = if (filled) GlassButtonStyle.Tinted else GlassButtonStyle.Regular,
    motion: GlassMotion = if (style == GlassButtonStyle.Tinted) GlassMotion.Jelly else GlassMotion.Flex,
) {
    val palette = LocalPalette.current
    val contentAlpha = if (enabled) 1f else 0.4f
    val tinted = style == GlassButtonStyle.Tinted
    Box(
        modifier = modifier
            .height(56.dp)
            .graphicsLayer { alpha = contentAlpha }
            .glass(
                ZenithShapes.pill,
                tint = if (tinted) palette.accent else Color.Unspecified,
                strength = if (tinted) 1f else 0.9f,
                material = if (style == GlassButtonStyle.Regular) GlassMaterial.Regular else GlassMaterial.Clear,
                interactive = enabled,
                tinted = tinted,
                motion = motion,
                pressEnabled = enabled,
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val contentColor = if (tinted) inkOnTint(palette.accent) else palette.onGlass
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

/**
 * The three glasses a [GlassButton] is made of. Regular frosts what is behind it, for buttons in
 * sheets; Clear only mists it, for a button over the wallpaper or the dark; Tinted is the accent's
 * glass, for the one that does the thing.
 */
enum class GlassButtonStyle { Regular, Clear, Tinted }

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
