package com.gabriel.zenithui.ui.connect

import android.util.LruCache
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gabriel.zenithui.data.GridDensity
import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.LayoutBuilder
import com.gabriel.zenithui.data.Look
import com.gabriel.zenithui.data.Looks
import com.gabriel.zenithui.data.Themes
import com.gabriel.zenithui.data.WallStyle
import com.gabriel.zenithui.data.WallpaperArt
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.data.connect.ConnectApply
import com.gabriel.zenithui.data.connect.ConnectLayers
import com.gabriel.zenithui.data.connect.ConnectSetup
import com.gabriel.zenithui.data.connect.Layer
import com.gabriel.zenithui.ui.onboarding.PhonePreview
import com.gabriel.zenithui.ui.theme.Palettes
import com.gabriel.zenithui.ui.theme.ThemedPicture
import com.gabriel.zenithui.ui.theme.ZenithPalette
import com.gabriel.zenithui.ui.theme.wallColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/** A setup worn over your home screen: the settings it would leave, and page one and the dock as they would be. */
@Immutable
internal class WornHome(val settings: ZenithSettings, val items: List<HomeItem>, val dock: List<String>)

/**
 * [setup] worn with [on] of it, over what you have now, worked out the way Apply works it out
 * (ConnectApply.wear, then Looks.apply as the view model does): your page one and dock, unless
 * its own pages are taken, which are then fitted to this phone and to its grid on this screen.
 */
internal fun wornHome(setup: ConnectSetup, on: Set<Layer>, bridge: ConnectBridge): WornHome {
    val wearing = ConnectApply.wear(bridge.currentSettings, "", setup, on, bridge.owns)
    val pages = Layer.LAYOUT in on && ConnectLayers.hasLayout(setup)
    val look = Look("", setup.name, wearing.settings, if (pages) wearing.layout else "[]", 0L)
    var (settings, items) = Looks.apply(bridge.settings, look)
    if (!pages) return WornHome(settings, bridge.home.items, settings.dock)
    if (wearing.fromElsewhere) {
        val fitted = Looks.keepInstalled(settings, items, bridge.home.appsByKey.keys)
        settings = fitted.first
        items = fitted.second
    }
    val width = bridge.home.screenWidth.value
    val height = bridge.home.screenHeight.value
    if (width > 0f && height > 0f) {
        val (columns, rows) = GridDensity.resolve(settings.gridDensity, settings.gridColumns, settings.gridRows, width, height)
        settings = settings.copy(columns = columns, rows = rows)
        items = LayoutBuilder.settle(items, columns, rows, settings.freeform)
    }
    return WornHome(settings, items.filter { it.page == 0 }, settings.dock)
}

@Composable
internal fun rememberWorn(setup: ConnectSetup, on: Set<Layer>, bridge: ConnectBridge): WornHome =
    remember(setup, on, bridge.currentSettings, bridge.settings, bridge.home.items, bridge.home.appsByKey, bridge.home.screenWidth, bridge.home.screenHeight) {
        wornHome(setup, on, bridge)
    }

/**
 * A setup worn over your home screen, as a phone: your own apps and widgets, drawn by the very code
 * that draws the home screen, in the setup's theme, its icons, its widgets' look, and its wallpaper
 * design painted in its colours (or Zenith's copy of your own wallpaper where it keeps yours). What
 * Apply would leave, exactly.
 */
@Composable
internal fun SetupPhone(worn: WornHome, bridge: ConnectBridge, modifier: Modifier = Modifier, artWidth: Int = 300) {
    val configuration = LocalConfiguration.current
    val screenWidth = bridge.home.screenWidth.takeIf { it > 0.dp } ?: configuration.screenWidthDp.dp
    val screenHeight = bridge.home.screenHeight.takeIf { it > 0.dp } ?: configuration.screenHeightDp.dp
    val wallpaper = rememberWornWallpaper(worn.settings, bridge, artWidth)
    ThemedPicture(worn.settings) {
        PhonePreview(
            items = worn.items,
            dock = worn.dock,
            appsByKey = bridge.home.appsByKey,
            settings = worn.settings,
            wallpaper = wallpaper,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            shown = Int.MAX_VALUE,
            modifier = modifier,
        )
    }
}

/** The screen's own shape, width over height, for a box a [SetupPhone] fills. */
@Composable
internal fun phoneAspect(bridge: ConnectBridge): Float {
    val configuration = LocalConfiguration.current
    val width = bridge.home.screenWidth.takeIf { it > 0.dp } ?: configuration.screenWidthDp.dp
    val height = bridge.home.screenHeight.takeIf { it > 0.dp } ?: configuration.screenHeightDp.dp
    return (width / height).coerceIn(0.3f, 1f)
}

/**
 * The wallpaper a worn setup shows: its design, painted in its colours for its Light or Dark; or,
 * where it is the one you have now (or a photo of yours, which no setup replaces), Zenith's copy of
 * your own.
 */
@Composable
private fun rememberWornWallpaper(settings: ZenithSettings, bridge: ConnectBridge, widthPx: Int): ImageBitmap? {
    val system = isSystemInDarkTheme()
    val style = WallStyle.byName(settings.wallpaperStyle)
    val dark = Themes.isDark(Themes.byId(settings.theme), settings.isDark(system))
    val mine = bridge.settings
    val mineDark = Themes.isDark(Themes.byId(mine.theme), mine.isDark(system))
    val asNow = settings.wallpaperStyle == mine.wallpaperStyle &&
        settings.wallpaperPalette.ifEmpty { settings.paletteId } == mine.wallpaperPalette.ifEmpty { mine.paletteId } &&
        dark == mineDark
    if (style == null || (asNow && bridge.home.wallpaper != null)) return bridge.home.wallpaper
    val palette = remember(settings.wallpaperPalette, settings.paletteId, dark) { Palettes.forWallpaper(settings, dark) }
    return rememberDesignArt(style, palette, widthPx)
}

/**
 * One of Zenith's designs painted small, off the main thread, and kept, so scrolling back past it
 * is instant: null until it is ready.
 */
@Composable
private fun rememberDesignArt(style: WallStyle, palette: ZenithPalette, widthPx: Int): ImageBitmap? {
    val colours = remember(palette) { palette.wallColors() }
    val key = "${style.name}|$colours|$widthPx"
    val image by produceState(initialValue = artCache.get(key), key) {
        value = artCache.get(key) ?: withContext(Dispatchers.Default) {
            runCatching { WallpaperArt.render(style, colours, widthPx, (widthPx / 0.46f).toInt()).asImageBitmap() }.getOrNull()
        }?.also { artCache.put(key, it) }
    }
    return image
}

/** Painted designs, kept by the memory they take: a few dozen small ones at most. */
private val artCache = object : LruCache<String, ImageBitmap>(16 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
}

/**
 * A phone you can turn with a finger: it follows the finger round its upright middle, as far as
 * [MAX_TURN] either way, and let go it swings back to where it rests, a little turned, on a soft
 * spring. Nothing moves on its own, so it asks for no frames while it rests.
 */
@Stable
internal class Spin(private val scope: CoroutineScope, private val rest: Float = REST_TURN) {
    val angle = Animatable(rest)

    fun drag(by: Float) {
        scope.launch { angle.snapTo((angle.value + by).coerceIn(-MAX_TURN, MAX_TURN)) }
    }

    fun letGo(velocity: Float) {
        scope.launch { angle.animateTo(rest, spring(dampingRatio = 0.5f, stiffness = 90f), initialVelocity = velocity) }
    }
}

/** How far the phone rests turned, and as far as it turns, in degrees. */
internal const val REST_TURN = -9f
internal const val MAX_TURN = 38f

/** Degrees of turn for each dp the finger moves. */
private const val TURN_PER_DP = 0.32f

/**
 * Turned by [spin] in 3D, and turnable by a finger across it. A sheen crosses its glass as it
 * turns, as light would; with movement reduced it stands square and still.
 */
internal fun Modifier.turned(spin: Spin, reduced: Boolean): Modifier {
    if (reduced) return this
    return this
        .pointerInput(spin) {
            val tracker = VelocityTracker()
            detectHorizontalDragGestures(
                onDragStart = { tracker.resetTracking() },
                onDragEnd = { spin.letGo(tracker.calculateVelocity().x / density * TURN_PER_DP) },
                onDragCancel = { spin.letGo(0f) },
            ) { change, dx ->
                tracker.addPosition(change.uptimeMillis, change.position)
                change.consume()
                spin.drag(dx / density * TURN_PER_DP)
            }
        }
        .graphicsLayer {
            rotationY = spin.angle.value
            cameraDistance = 16f * density
        }
        .drawWithContent {
            drawContent()
            // Over the screen only: the box is the phone's own shape, its corners the screen's.
            val turn = spin.angle.value / MAX_TURN
            val strength = (abs(turn) * 0.2f + 0.03f).coerceAtMost(0.24f)
            val across = size.width * (0.5f - turn * 0.9f)
            val corner = size.width * PHONE_CORNER
            drawRoundRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = strength), Color.Transparent),
                    start = Offset(across - size.width * 0.45f, 0f),
                    end = Offset(across + size.width * 0.45f, size.height),
                ),
                cornerRadius = CornerRadius(corner, corner),
            )
        }
}

/** A phone's screen corner as a share of its width: PhonePreview's 46 dp on a screen about 412 dp wide. */
private const val PHONE_CORNER = 0.112f

/** As tall as [width] makes a phone of this screen's shape. */
internal fun phoneHeight(width: Dp, aspect: Float): Dp = width / aspect
