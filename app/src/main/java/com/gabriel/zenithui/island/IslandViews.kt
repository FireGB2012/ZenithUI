package com.gabriel.zenithui.island

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.IslandKind
import com.gabriel.zenithui.data.IslandMoment
import com.gabriel.zenithui.data.NotificationDots
import com.gabriel.zenithui.data.Playing
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.pressBounce
import kotlinx.coroutines.delay

/**
 * Where this phone's camera is, and everything the island is sized from: in dp, from the top left
 * of the screen. The island is the camera stretched, so all of it follows from the camera.
 */
@Immutable
data class IslandGeometry(
    /** The camera's width; 0 on a phone with no camera in the screen. */
    val hole: Dp,
    val holeX: Dp,
    val holeY: Dp,
    val statusBar: Dp,
    val width: Dp,
) {
    /** The island's height: the camera and a hair round it, so the two read as one thing. */
    val height: Dp get() = if (hole > 0.dp) (hole + 3.dp).coerceIn(22.dp, 34.dp) else 26.dp

    /** Its top: centred on the camera, or in the middle of the status bar on a phone with none. */
    val top: Dp get() = if (hole > 0.dp) holeY - height / 2 else ((statusBar - height) / 2).coerceAtLeast(2.dp)

    val centreX: Dp get() = if (hole > 0.dp) holeX else width / 2

    /** Kept clear in the middle, for the camera: it, and a little either side of it. */
    val gap: Dp get() = if (hole > 0.dp) hole + 10.dp else 8.dp

    /** An icon or a picture round the camera: the island's height, less a hair at top and bottom. */
    val item: Dp get() = height - 8.dp

    /** From each end to what is in it: what puts an icon in the middle of the island's rounded end. */
    val end: Dp get() = (height - item) / 2

    /** At rest it is the camera and nothing more. */
    val rest: Dp get() = hole

    /** The short pill round the camera that an open card hangs from. */
    val root: Dp get() = gap + height

    /** Where an open card hangs: under the status bar, so none of the phone's own is under it. */
    val cardTop: Dp get() = max(statusBar, top + height) + 7.dp

    val cardWidth: Dp get() = min(width - 20.dp, 420.dp)

    /**
     * The widest the island grows round the camera: a little under half the screen, which leaves
     * the clock on one side and the status icons on the other clear of it, as they sit above it.
     */
    val widest: Dp get() = max(width * 0.46f, root + height * 2)

    /** The most room words have in one of the island's ends; a longer name is cut short. */
    val words: Dp get() = (widest - gap) / 2 - end - 7.dp

    companion object {
        /** A phone like most: a camera of 24 dp in the middle of a status bar of 32 dp. */
        fun typical(width: Dp = 392.dp) = IslandGeometry(hole = 24.dp, holeX = width / 2, holeY = 20.dp, statusBar = 32.dp, width = width)
    }
}

/** This phone's camera and status bar, as the app's own window reports them. */
@Composable
fun rememberIslandGeometry(): IslandGeometry {
    val view = LocalView.current
    val density = LocalDensity.current
    val width = LocalConfiguration.current.screenWidthDp.dp
    var geometry by remember { mutableStateOf(IslandGeometry.typical(width)) }
    LaunchedEffect(view, width) {
        val insets = view.rootWindowInsets ?: return@LaunchedEffect
        val hole = insets.displayCutout?.boundingRectTop?.takeIf { !it.isEmpty }
        val bar = if (Build.VERSION.SDK_INT >= 30) insets.getInsets(android.view.WindowInsets.Type.statusBars()).top else @Suppress("DEPRECATION") insets.systemWindowInsetTop
        geometry = with(density) {
            IslandGeometry(
                hole = (hole?.let { minOf(it.width(), it.height()) } ?: 0).toDp(),
                holeX = hole?.centerX()?.toDp() ?: (width / 2),
                holeY = hole?.exactCenterY()?.toDp() ?: 0.dp,
                statusBar = bar.toDp(),
                width = width,
            )
        }
    }
    return geometry
}

/** Everything the island draws from, at one moment. [kind] null: at rest, the camera alone. */
@Immutable
data class IslandState(
    val kind: IslandKind?,
    val moment: IslandMoment? = null,
    val music: Playing? = null,
    /** A colour from the cover, for the bars and the progress: the music's own. */
    val musicColour: Color? = null,
    val live: NotificationDots.Live? = null,
    /** The running clock, as it reads now. */
    val liveText: String = "",
    /** How full the battery is, in percent. */
    val level: Int = 0,
    /** The icon and name of the app a notification or a running clock is from. */
    val appIcon: ImageBitmap? = null,
    val appName: String = "",
    /** What goes on beside it, in a bubble split off from it. */
    val secondary: IslandKind? = null,
    /** The icon of what is in the bubble, when it is a running clock. */
    val secondaryIcon: ImageBitmap? = null,
)

/** What the island's parts do when they are touched. */
class IslandActions(
    val close: () -> Unit = {},
    val openNotification: () -> Unit = {},
    val openLive: () -> Unit = {},
    val openMusic: () -> Unit = {},
    val playPause: () -> Unit = {},
    val next: () -> Unit = {},
    val previous: () -> Unit = {},
)

private val INK = Color.White
private val QUIET = Color.White.copy(alpha = 0.6f)
private val ISLAND = Color.Black
val ISLAND_GREEN = Color(0xFF30D158)
val ISLAND_ORANGE = Color(0xFFFF9F0A)
val ISLAND_RED = Color(0xFFFF453A)

/** The music's colour when a song gives none: white, since the island is black in Light and in Dark. */
private val MUSIC_PLAIN = Color.White

/**
 * The island's type: the phone's own sans, as the status bar beside it is set in, whatever font
 * the launcher is in — the island belongs to the phone more than to the home screen.
 */
private val ISLAND_FONT = FontFamily.SansSerif

/** What all words on the island start from: that sans, set close, at its natural line height. */
private val ISLAND_TEXT = TextStyle(fontFamily = ISLAND_FONT, letterSpacing = 0.sp)

/** A message read on its card; measured in the same style, so the card is as tall as it needs. */
private val READ_TEXT = TextStyle(fontFamily = ISLAND_FONT, fontSize = 14.sp, lineHeight = 19.sp, letterSpacing = 0.sp)

/** The card's room beside a message: its edges, the app's icon, and the space after it. */
private val READ_SIDES = 86.dp

/** Words round the camera: small and firm, with figures all one width, so a clock does not shuffle. */
@Composable
private fun sideText(): TextStyle = TextStyle(
    fontFamily = ISLAND_FONT,
    fontSize = 12.5.sp,
    fontWeight = FontWeight.SemiBold,
    fontFeatureSettings = "tnum",
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

/** The island's spring: quick, and just enough past its mark to feel alive. */
private fun <T> islandSpring(stiff: Float = 230f, damping: Float = 0.78f) = spring<T>(dampingRatio = damping, stiffness = stiff)

/** The colour of a running clock: a call's green, anything else's orange. */
private fun liveColour(live: NotificationDots.Live?): Color = if (live?.call == true) ISLAND_GREEN else ISLAND_ORANGE

/** The ringer's words and colour. */
private fun ringerLook(mode: String): Pair<String, Color> = when (mode) {
    "silent" -> "Silent" to ISLAND_RED
    "vibrate" -> "Vibrate" to INK
    else -> "Ring" to INK
}

// ---------------------------------------------------------------------- round the camera

/**
 * The island round the camera. At rest it is exactly the camera, black on black, so it is not
 * there; for what is going on it grows out of it — wider, then a hair taller — with what it shows
 * in its two rounded ends, either side of the camera, and nothing over it. [expanded] keeps only a
 * short pill round the camera, for an open card to hang from. [skirt] is room below it that still
 * takes a tap, for a phone whose status bar keeps the touches on top of it.
 */
@Composable
fun IslandCompact(
    state: IslandState,
    geometry: IslandGeometry,
    expanded: Boolean,
    onTap: () -> Unit,
    onHold: () -> Unit,
    modifier: Modifier = Modifier,
    skirt: Dp = 0.dp,
    visible: Boolean = true,
) {
    val reduced = LocalMotionLevel.current.reduced
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val style = sideText()
    fun wide(text: String): Dp = with(density) { measurer.measure(text, style, maxLines = 1).size.width.toDp() }
    val kind = state.kind
    val item = geometry.item
    // How wide each end has to be for what is in it; the island is as wide as both, either side
    // of the camera, so it stays centred on it.
    val (lead, trail) = when (kind) {
        IslandKind.MUSIC -> item to item
        IslandKind.LIVE -> item to wide(state.liveText)
        IslandKind.NOTIFICATION -> item to min(wide(sender(state)), geometry.words)
        IslandKind.RINGER -> item to wide(ringerLook(state.moment?.text.orEmpty()).first)
        IslandKind.PLUGGED -> wide("Charging") to (BATTERY_WIDE + 5.dp + wide("${state.moment?.level ?: state.level}%"))
        else -> 0.dp to 0.dp
    }
    val side = min(geometry.end + max(lead, trail) + 7.dp, (geometry.widest - geometry.gap) / 2)
    val targetW = when {
        expanded -> geometry.root
        kind == null -> geometry.rest
        kind == IslandKind.IDLE -> max(geometry.root + geometry.height * 0.6f, geometry.height * 2.6f)
        else -> geometry.gap + side * 2
    }
    val targetH = if (kind == null && !expanded) geometry.rest else geometry.height
    val width by animateDpAsState(targetW, if (reduced) tween(Motion.REDUCED_MS) else islandSpring(230f, 0.76f), label = "islandWidth")
    val height by animateDpAsState(targetH, if (reduced) tween(Motion.REDUCED_MS) else islandSpring(300f, 0.86f), label = "islandHeight")
    var pressed by remember { mutableStateOf(false) }
    val squeeze by animateFloatAsState(if (pressed) 0.94f else 1f, spring(dampingRatio = 0.5f, stiffness = 700f), label = "islandPress")
    val bubble by animateFloatAsState(
        if (state.secondary != null && !expanded) 1f else 0f,
        if (reduced) tween(Motion.REDUCED_MS) else islandSpring(260f, 0.66f),
        label = "islandBubble",
    )
    val bubbleSide = (geometry.height + 6.dp) * bubble
    val tappable = kind?.opens == true && !expanded
    val tap by rememberUpdatedState(onTap)
    val hold by rememberUpdatedState(onHold)

    AnimatedVisibility(visible = visible, enter = fadeIn(tween(220)), exit = fadeOut(tween(180)), modifier = modifier) {
      CompositionLocalProvider(LocalTextStyle provides ISLAND_TEXT) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.pointerInput(tappable) {
                if (!tappable) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { tap() },
                    onLongPress = { hold() },
                )
            },
        ) {
            Row(verticalAlignment = Alignment.Top) {
                // As wide as the bubble on the right, so the island itself stays on the camera.
                Spacer(Modifier.width(bubbleSide))
                Box(
                    Modifier
                        .padding(top = ((geometry.height - height) / 2).coerceAtLeast(0.dp))
                        .size(width, height)
                        .graphicsLayer {
                            scaleX = squeeze
                            scaleY = squeeze
                        }
                        .clip(RoundedCornerShape(50))
                        .background(ISLAND),
                ) {
                    if (!expanded) {
                        AnimatedContent(
                            targetState = kind,
                            transitionSpec = {
                                if (reduced) {
                                    fadeIn(tween(Motion.REDUCED_MS)) togetherWith fadeOut(tween(Motion.REDUCED_MS))
                                } else {
                                    // In once the island has made room for it; out before it shrinks.
                                    (fadeIn(tween(200, delayMillis = 110)) + scaleIn(tween(320, delayMillis = 80), initialScale = 0.82f)) togetherWith fadeOut(tween(100))
                                }
                            },
                            label = "islandSides",
                        ) { what ->
                            val blur by transition.animateDp(label = "islandSidesBlur") { if (it == EnterExitState.Visible || reduced) 0.dp else 6.dp }
                            Box(Modifier.fillMaxSize().blur(blur)) {
                                Sides(what, state, geometry)
                            }
                        }
                    }
                }
                Box(Modifier.width(bubbleSide).height(geometry.height), contentAlignment = Alignment.CenterEnd) {
                    if (bubble > 0.01f) {
                        Box(
                            Modifier
                                .graphicsLayer {
                                    scaleX = bubble
                                    scaleY = bubble
                                    // Out of the island's side, as if it had come away from it.
                                    translationX = -(1f - bubble) * geometry.height.toPx()
                                    alpha = bubble.coerceIn(0f, 1f)
                                }
                                .size(geometry.height)
                                .clip(CircleShape)
                                .background(ISLAND),
                            contentAlignment = Alignment.Center,
                        ) {
                            when (state.secondary) {
                                IslandKind.MUSIC -> Bars(state.musicColour ?: MUSIC_PLAIN, state.music?.playing == true, geometry.item * 0.62f)
                                IslandKind.LIVE -> LiveMark(state.live, state.secondaryIcon, geometry.item * 0.82f)
                                else -> Unit
                            }
                        }
                    }
                }
            }
            if (skirt > 0.dp) Spacer(Modifier.height(skirt))
        }
      }
    }
}

/** Who a notification is from, as short as it goes. */
private fun sender(state: IslandState): String = state.moment?.title.orEmpty().ifBlank { state.appName }

/** What is in the island's two ends, for [kind]. */
@Composable
private fun Sides(kind: IslandKind?, state: IslandState, geometry: IslandGeometry) {
    val item = geometry.item
    val style = sideText()
    Box(Modifier.fillMaxSize().padding(horizontal = geometry.end)) {
        val start = Modifier.align(Alignment.CenterStart)
        val end = Modifier.align(Alignment.CenterEnd)
        when (kind) {
            IslandKind.MUSIC -> {
                Box(start) { Cover(state.music?.art, item, item * 0.3f, state.musicColour ?: MUSIC_PLAIN) }
                Box(end.padding(end = 2.dp)) { Bars(state.musicColour ?: MUSIC_PLAIN, state.music?.playing == true, item * 0.86f) }
            }
            IslandKind.LIVE -> {
                Box(start) { LiveMark(state.live, state.appIcon, item) }
                Text(state.liveText, style = style, color = liveColour(state.live), maxLines = 1, modifier = end.padding(end = 3.dp))
            }
            IslandKind.NOTIFICATION -> {
                Box(start) { AppMark(state.appIcon, item) }
                Text(
                    sender(state),
                    style = style,
                    color = INK,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = end.padding(end = 3.dp).widthIn(max = geometry.words),
                )
            }
            IslandKind.RINGER -> {
                val (word, colour) = ringerLook(state.moment?.text.orEmpty())
                Box(start) { Bell(state.moment?.text.orEmpty(), colour, item * 0.82f) }
                Text(word, style = style, color = colour, maxLines = 1, modifier = end.padding(end = 3.dp))
            }
            IslandKind.PLUGGED -> {
                val level = state.moment?.level ?: state.level
                Text("Charging", style = style, color = INK, maxLines = 1, modifier = start.padding(start = 3.dp))
                Row(end.padding(end = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("$level%", style = style, color = ISLAND_GREEN, maxLines = 1)
                    Spacer(Modifier.width(5.dp))
                    Battery(level)
                }
            }
            else -> Unit
        }
    }
}

// ---------------------------------------------------------------------- the card it opens into

/**
 * A card opened from the island: it grows out of the island round the camera and drops below the
 * status bar, still joined to it by a neck — one shape of black, as if it had run out of the
 * camera — with what is in it coming into focus once there is room. A tap on the card opens what
 * it is about; anywhere else closes it. [open] false folds it back up into the camera, and then
 * [onClosed]. [from] is how wide the island was when it opened.
 */
@Composable
fun IslandCard(
    state: IslandState,
    geometry: IslandGeometry,
    open: Boolean,
    from: Dp,
    actions: IslandActions,
    onClosed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduced = LocalMotionLevel.current.reduced
    val progress = remember { Animatable(0f) }
    val closed by rememberUpdatedState(onClosed)
    LaunchedEffect(open) {
        if (reduced) {
            progress.animateTo(if (open) 1f else 0f, tween(Motion.REDUCED_MS))
        } else {
            progress.animateTo(if (open) 1f else 0f, if (open) islandSpring(220f, 0.8f) else spring(dampingRatio = 1f, stiffness = 420f))
        }
        if (!open) closed()
    }
    val kind = state.kind
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val message = if (kind == IslandKind.NOTIFICATION) state.moment?.text.orEmpty() else ""
    val lines = remember(message, geometry.cardWidth, density) {
        if (message.isBlank()) {
            0
        } else {
            val room = with(density) { (geometry.cardWidth - READ_SIDES).roundToPx() }.coerceAtLeast(1)
            measurer.measure(message, READ_TEXT, maxLines = 3, constraints = Constraints(maxWidth = room)).lineCount
        }
    }
    val height = when (kind) {
        IslandKind.MUSIC -> 158.dp
        IslandKind.LIVE -> 100.dp
        IslandKind.NOTIFICATION -> max(72.dp, 68.dp + 19.dp * lines)
        else -> 132.dp
    }
    val cardW = geometry.cardWidth
    val left = (geometry.centreX - cardW / 2).coerceIn(10.dp, (geometry.width - 10.dp - cardW).coerceAtLeast(10.dp))
    val target = DpRect(left, geometry.cardTop, left + cardW, geometry.cardTop + height)
    val start = DpRect(geometry.centreX - from / 2, geometry.top, geometry.centreX + from / 2, geometry.top + geometry.height)
    val p = progress.value
    val now = DpRect(
        lerp(start.left, target.left, p),
        lerp(start.top, target.top, p),
        lerp(start.right, target.right, p),
        lerp(start.bottom, target.bottom, p),
    )
    val corner = lerp(geometry.height / 2, 30.dp, p.coerceIn(0f, 1f))
    val shown = ((p - 0.5f) / 0.5f).coerceIn(0f, 1f)

    Box(
        modifier
            .fillMaxSize()
            .pointerInput(target) {
                // Anywhere but the card closes it, the island included.
                detectTapGestures { at ->
                    val inside = with(density) { at.x in target.left.toPx()..target.right.toPx() && at.y in target.top.toPx()..target.bottom.toPx() }
                    if (!inside) actions.close()
                }
            },
    ) {
        // One shape of black: the pill round the camera, the card, and the neck between them. A soft
        // shadow under the card lifts it off a pale app; a hairline at its edge marks it on a dark one.
        Canvas(Modifier.fillMaxSize()) {
            val card = roundRect(now, corner, this)
            for (step in 1..4) {
                val spread = step * 3.dp.toPx()
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.07f * p.coerceIn(0f, 1f) / step),
                    topLeft = Offset(card.left - spread, card.top - spread + step * 2.dp.toPx()),
                    size = Size(card.width + spread * 2, card.height + spread * 2),
                    cornerRadius = CornerRadius(card.topLeftCornerRadius.x + spread),
                )
            }
            val shape = silhouette(geometry, now, corner, this)
            drawPath(shape, ISLAND)
            drawPath(shape, Color.White.copy(alpha = 0.09f * p.coerceIn(0f, 1f)), style = Stroke(1.dp.toPx()))
        }
        // What is in the card, at its full size, seen only inside the card as it grows.
        CompositionLocalProvider(LocalTextStyle provides ISLAND_TEXT) {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    val card = Path().apply { addRoundRect(roundRect(now, corner, this@drawWithContent)) }
                    clipPath(card) { this@drawWithContent.drawContent() }
                },
        ) {
            Box(
                Modifier
                    .offset(target.left, target.top)
                    .size(target.right - target.left, height)
                    .graphicsLayer {
                        alpha = shown
                        val scale = 0.95f + 0.05f * shown
                        scaleX = scale
                        scaleY = scale
                    }
                    .blur(10.dp * (1f - shown)),
            ) {
                when (kind) {
                    IslandKind.MUSIC -> MusicCard(state, actions)
                    IslandKind.LIVE -> LiveCard(state, actions)
                    IslandKind.NOTIFICATION -> ReadCard(state, actions)
                    else -> Unit
                }
            }
        }
        }
    }
}

/** A rectangle in dp. */
@Immutable
private data class DpRect(val left: Dp, val top: Dp, val right: Dp, val bottom: Dp)

private fun roundRect(r: DpRect, corner: Dp, density: androidx.compose.ui.unit.Density): RoundRect = with(density) {
    val radius = corner.toPx().coerceAtMost(((r.bottom - r.top).toPx() / 2f).coerceAtLeast(0f))
    RoundRect(r.left.toPx(), r.top.toPx(), r.right.toPx(), r.bottom.toPx(), CornerRadius(radius))
}

/**
 * The open island as one shape: the short pill round the camera, the card below it, and, once the
 * card has dropped clear, a neck joining them that widens into each with a curve, the way a drop
 * of water hangs from a tap.
 */
private fun silhouette(geometry: IslandGeometry, card: DpRect, corner: Dp, density: androidx.compose.ui.unit.Density): Path = with(density) {
    val cx = geometry.centreX.toPx()
    val rootHalf = geometry.root.toPx() / 2f
    val rootTop = geometry.top.toPx()
    val rootBottom = rootTop + geometry.height.toPx()
    val root = Path().apply { addRoundRect(RoundRect(cx - rootHalf, rootTop, cx + rootHalf, rootBottom, CornerRadius(geometry.height.toPx() / 2f))) }
    val body = Path().apply { addRoundRect(roundRect(card, corner, this@with)) }
    var shape = Path().apply { op(root, body, PathOperation.Union) }
    val cardTop = card.top.toPx()
    val gap = cardTop - rootBottom
    if (gap > 0.5f) {
        val half = (geometry.hole.toPx().coerceAtLeast(geometry.height.toPx() * 0.6f)) / 2f
        val r = (gap / 2f).coerceAtMost(6.dp.toPx())
        val neck = Path().apply {
            addRect(Rect(cx - half, rootBottom - 1f, cx + half, cardTop + 1f))
            // The curves where the neck meets the card, and where it meets the pill.
            for ((x, y, sx, sy) in listOf(
                Quad(cx - half, cardTop, -1f, -1f), Quad(cx + half, cardTop, 1f, -1f),
                Quad(cx - half, rootBottom, -1f, 1f), Quad(cx + half, rootBottom, 1f, 1f),
            )) {
                val box = Path().apply {
                    addRect(Rect(minOf(x, x + sx * r), minOf(y, y + sy * r), maxOf(x, x + sx * r), maxOf(y, y + sy * r)))
                }
                val round = Path().apply { addOval(Rect(Offset(x + sx * r, y + sy * r), r)) }
                val fillet = Path().apply { op(box, round, PathOperation.Difference) }
                op(this, fillet, PathOperation.Union)
            }
        }
        shape = Path().apply { op(shape, neck, PathOperation.Union) }
    }
    shape
}

private data class Quad(val a: Float, val b: Float, val c: Float, val d: Float)

/** Music, opened: the cover, the song, how far through, and the three buttons. */
@Composable
private fun MusicCard(state: IslandState, actions: IslandActions) {
    val music = state.music
    val colour = state.musicColour ?: MUSIC_PLAIN
    Column(Modifier.fillMaxSize().padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { actions.openMusic() }) {
                Cover(music?.art, 52.dp, 12.dp, colour)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(music?.title.orEmpty().ifEmpty { "Nothing playing" }, color = INK, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(1.dp))
                Text(music?.artist.orEmpty(), color = QUIET, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(10.dp))
            Bars(colour, music?.playing == true, 20.dp)
        }
        Spacer(Modifier.height(14.dp))
        Progress(music, colour)
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Control(Icons.Rounded.SkipPrevious, "Back", music?.canGoBack != false, actions.previous, 30.dp)
            Spacer(Modifier.width(34.dp))
            Control(if (music?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (music?.playing == true) "Pause" else "Play", true, actions.playPause, 40.dp)
            Spacer(Modifier.width(34.dp))
            Control(Icons.Rounded.SkipNext, "Next", music?.canSkip != false, actions.next, 30.dp)
        }
    }
}

/** How far through the song: a thin line in the music's colour, and the time gone and the time left. */
@Composable
private fun Progress(music: Playing?, colour: Color) {
    val along by produceState(0L, music) {
        while (true) {
            value = music?.positionNow() ?: 0L
            if (music?.playing != true) break
            delay(500)
        }
    }
    val length = music?.duration ?: 0L
    val share = if (length > 0L) (along.toFloat() / length).coerceIn(0f, 1f) else 0f
    Canvas(Modifier.fillMaxWidth().height(4.dp)) {
        drawRoundRect(Color.White.copy(alpha = 0.18f), size = size, cornerRadius = CornerRadius(size.height / 2f))
        drawRoundRect(colour, size = Size(size.width * share, size.height), cornerRadius = CornerRadius(size.height / 2f))
    }
    Spacer(Modifier.height(5.dp))
    Row(Modifier.fillMaxWidth()) {
        val style = TextStyle(fontSize = 11.sp, fontFeatureSettings = "tnum", color = QUIET, fontFamily = ISLAND_FONT)
        Text(minutes(along), style = style)
        Spacer(Modifier.weight(1f))
        Text(if (length > 0L) "−" + minutes(length - along) else "", style = style)
    }
}

private fun minutes(ms: Long): String {
    val seconds = (ms.coerceAtLeast(0L)) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

@Composable
private fun Control(icon: androidx.compose.ui.graphics.vector.ImageVector, said: String, enabled: Boolean, onClick: () -> Unit, size: Dp) {
    Box(
        Modifier
            .size(size + 14.dp)
            .pressBounce(enabled = enabled)
            .clip(CircleShape)
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = said, tint = if (enabled) INK else INK.copy(alpha = 0.3f), modifier = Modifier.size(size))
    }
}

/** A running clock, opened: whose it is, and the time, large. A tap opens the app. */
@Composable
private fun LiveCard(state: IslandState, actions: IslandActions) {
    val live = state.live
    Row(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { actions.openLive() }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiveMark(live, state.appIcon, 44.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                live?.title.orEmpty().ifBlank { state.appName },
                color = QUIET,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                state.liveText,
                color = liveColour(live),
                fontSize = 34.sp,
                fontWeight = FontWeight.Light,
                fontFamily = ISLAND_FONT,
                style = TextStyle(fontFeatureSettings = "tnum"),
                maxLines = 1,
            )
        }
    }
}

/** A notification, opened to read: the app, who it is from, and what it says. A tap opens it. */
@Composable
private fun ReadCard(state: IslandState, actions: IslandActions) {
    val moment = state.moment
    Row(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { actions.openNotification() }
            .padding(start = 16.dp, end = 18.dp, top = 16.dp, bottom = 14.dp),
    ) {
        AppMark(state.appIcon, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(state.appName, color = QUIET, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("now", color = QUIET, fontSize = 12.sp)
            }
            Spacer(Modifier.height(2.dp))
            Text(moment?.title.orEmpty(), color = INK, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!moment?.text.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(moment?.text.orEmpty(), style = READ_TEXT, color = INK.copy(alpha = 0.86f), maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ---------------------------------------------------------------------- small pieces

@Composable
private fun Cover(art: ImageBitmap?, size: Dp, corner: Dp, colour: Color) {
    if (art != null) {
        Image(art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size).clip(RoundedCornerShape(corner)))
    } else {
        Box(Modifier.size(size).clip(RoundedCornerShape(corner)).background(colour.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = colour, modifier = Modifier.size(size * 0.62f))
        }
    }
}

/**
 * Four bars in the music's colour, rising and falling out of step while it plays and resting low
 * when it stops. The one thing on the island that moves by itself: it is the music moving it.
 */
@Composable
private fun Bars(colour: Color, playing: Boolean, size: Dp) {
    val reduced = LocalMotionLevel.current.reduced
    val beat = rememberInfiniteTransition(label = "bars")
    val levels = (0 until 4).map { index ->
        if (!playing || reduced) {
            listOf(0.35f, 0.6f, 0.45f, 0.3f)[index]
        } else {
            beat.animateFloat(
                initialValue = 0.22f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(380 + 90 * index, delayMillis = listOf(0, 160, 300, 80)[index], easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "bar$index",
            ).value
        }
    }
    val settle by animateFloatAsState(if (playing) 1f else 0.55f, tween(300), label = "barsRest")
    Canvas(Modifier.size(width = size, height = size * 0.78f)) {
        val bar = size.toPx() / 7f
        val step = (this.size.width - bar) / 3f
        levels.forEachIndexed { index, level ->
            val tall = (this.size.height * level * settle).coerceAtLeast(bar)
            drawRoundRect(
                color = colour,
                topLeft = Offset(index * step, (this.size.height - tall) / 2f),
                size = Size(bar, tall),
                cornerRadius = CornerRadius(bar / 2f),
            )
        }
    }
}

/** The app a notification is from: its icon, rounded like an icon on the home screen. */
@Composable
private fun AppMark(icon: ImageBitmap?, size: Dp) {
    if (icon != null) {
        Image(icon, contentDescription = null, modifier = Modifier.size(size).clip(RoundedCornerShape(size * 0.28f)))
    } else {
        Box(Modifier.size(size).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Notifications, contentDescription = null, tint = INK, modifier = Modifier.size(size * 0.6f))
        }
    }
}

/** Whose running clock it is: a green phone for a call, else the app's icon. */
@Composable
private fun LiveMark(live: NotificationDots.Live?, icon: ImageBitmap?, size: Dp) {
    if (live?.call == true || icon == null) {
        Box(Modifier.size(size).clip(CircleShape).background(liveColour(live)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.58f))
        }
    } else {
        AppMark(icon, size)
    }
}

/** The ringer's bell, giving a little shake as it changes, slashed for silent. */
@Composable
private fun Bell(mode: String, colour: Color, size: Dp) {
    val reduced = LocalMotionLevel.current.reduced
    val shake = remember { Animatable(0f) }
    LaunchedEffect(mode) {
        if (!reduced) {
            shake.animateTo(0f, keyframes {
                durationMillis = 560
                -18f at 70
                15f at 170
                -10f at 270
                6f at 370
                0f at 560
            })
        }
    }
    val icon = when (mode) {
        "silent" -> Icons.Rounded.NotificationsOff
        "vibrate" -> Icons.Rounded.Vibration
        else -> Icons.Rounded.Notifications
    }
    Icon(icon, contentDescription = null, tint = colour, modifier = Modifier.size(size).graphicsLayer { rotationZ = shake.value })
}

private val BATTERY_WIDE = 23.dp

/** A battery in outline, filled in green as far as it is charged. */
@Composable
private fun Battery(level: Int) {
    val fill = remember { Animatable(0f) }
    val reduced = LocalMotionLevel.current.reduced
    LaunchedEffect(level) { fill.animateTo(level / 100f, if (reduced) tween(Motion.REDUCED_MS) else tween(700, easing = FastOutSlowInEasing)) }
    Canvas(Modifier.size(width = BATTERY_WIDE, height = 11.dp)) {
        val nub = 2.dp.toPx()
        val body = Size(size.width - nub - 1.dp.toPx(), size.height)
        val stroke = 1.2.dp.toPx()
        drawRoundRect(Color.White.copy(alpha = 0.45f), topLeft = Offset(stroke / 2, stroke / 2), size = Size(body.width - stroke, body.height - stroke), cornerRadius = CornerRadius(3.dp.toPx()), style = Stroke(stroke))
        drawRoundRect(Color.White.copy(alpha = 0.45f), topLeft = Offset(body.width + 1.dp.toPx(), size.height * 0.33f), size = Size(nub, size.height * 0.34f), cornerRadius = CornerRadius(1.dp.toPx()))
        val inset = 2.2.dp.toPx()
        drawRoundRect(
            ISLAND_GREEN,
            topLeft = Offset(inset, inset),
            size = Size((body.width - inset * 2) * fill.value, body.height - inset * 2),
            cornerRadius = CornerRadius(1.6.dp.toPx()),
        )
    }
}
