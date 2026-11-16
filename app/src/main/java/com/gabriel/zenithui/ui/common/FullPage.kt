package com.gabriel.zenithui.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalTheme
import com.gabriel.zenithui.ui.theme.glass
import com.gabriel.zenithui.ui.theme.isGlass
import com.gabriel.zenithui.ui.theme.shape
import com.gabriel.zenithui.ui.theme.themePage
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A page the full height of the screen, laid over the home screen: glass over the wallpaper, a
 * bar with a way back and a way out, and whatever the page holds underneath it. Settings is one;
 * the widget gallery is another. It rises from the foot of the screen and goes back there, the home
 * screen fading and sinking back a little under it as it comes, as it does under the drawer.
 *
 * It can be pulled away, as a sheet can: by its bar, or by what it holds once that is scrolled to
 * the top. It goes exactly as far as the finger takes it, its corners rounding as it leaves the
 * edges of the screen and the home screen coming back into view under it ([LocalHomeCover]); let
 * go, it carries on down at the speed it was thrown at, or springs back up ([SheetPull] decides).
 *
 * It sits under the sheets, not among them. While a sheet is open over it ([covered]) the page
 * empties to plain glass: where the glass is only frosted a sheet is see-through, and two layers
 * of words on top of each other can be read by nobody.
 */
@Composable
fun FullPage(
    visible: Boolean,
    covered: Boolean,
    title: String,
    canGoBack: Boolean,
    /** How much of the bar's own small title shows, 0 to 1: it comes in as the large one scrolls away. */
    titleShown: () -> Float,
    onBack: () -> Unit,
    onClose: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val focus = LocalFocusManager.current
    val reduced = LocalMotionLevel.current.reduced
    val palette = LocalPalette.current
    val theme = LocalTheme.current
    val uncovered = animateFloatAsState(if (covered) 0f else 1f, tween(200), label = "pageUncovered")
    val scope = rememberCoroutineScope()
    // Read when a finger lets go, which can be long after this was composed.
    val close by rememberUpdatedState(onClose)
    val stillReduced by rememberUpdatedState(reduced)

    // Closed to begin with, even when it is first shown already open, so it comes up from the foot.
    val shown = remember { MutableTransitionState(false) }
    shown.targetState = visible

    // While a finger has it the pull is written straight from the drag, so the page sits exactly
    // under the thumb. Let go, and a spring takes it from there.
    var held by remember { mutableStateOf(false) }
    var pulled by remember { mutableFloatStateOf(0f) }
    val settling = remember { Animatable(0f) }
    /** The page's own height: how far down is out of sight. */
    var height by remember { mutableFloatStateOf(1f) }

    /** How far down from its place the page is right now. Read while placing and drawing. */
    fun down(): Float = if (held) SheetPull.shown(pulled, give = 0f) else settling.value

    fun take() {
        if (held) return
        pulled = settling.value
        held = true
        scope.launch { settling.stop() }
    }

    fun letGo(velocity: Float) {
        if (!held) return
        val from = down()
        scope.launch {
            // Handed from the finger to the spring between two frames, at the same spot.
            settling.snapTo(from)
            held = false
            val quick = stillReduced
            if (SheetPull.dismisses(from, height, velocity)) {
                // Closed from the moment it is thrown, and it carries on down at the speed it was
                // thrown at as it goes, on the spring a page goes away on.
                close()
                settling.animateTo(
                    targetValue = height,
                    animationSpec = if (quick) tween(Motion.REDUCED_MS) else Motion.close(),
                    initialVelocity = if (quick) 0f else velocity,
                )
            } else {
                settling.animateTo(
                    targetValue = 0f,
                    animationSpec = if (quick) tween(Motion.REDUCED_MS) else Motion.sheet(),
                    initialVelocity = if (quick) 0f else velocity,
                )
            }
        }
    }

    // Shown again after being thrown away, it starts from its own place, not from where it went.
    LaunchedEffect(visible) {
        if (visible) {
            held = false
            pulled = 0f
            settling.snapTo(0f)
        }
    }

    // How much of the home screen under it the page covers, for the home screen to fade and sink by.
    val homeCover = LocalHomeCover.current

    // What it holds scrolls first. Only what that cannot use moves the page.
    val pullThrough = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Part way down already: going back up puts the page back before anything scrolls.
                if (!held || pulled <= 0f || available.y >= 0f) return Offset.Zero
                val back = maxOf(available.y, -pulled)
                pulled += back
                return Offset(0f, back)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
                take()
                pulled += available.y
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!held) return Velocity.Zero
                letGo(available.y)
                return available
            }
        }
    }

    // Its coming and going is an animation of its own rather than a slide in and out, so that a page
    // let go part way down and closed goes on down from where the finger left it, at the speed it was
    // thrown at, and the home screen under it can be told exactly how much of it is still covered.
    AnimatedVisibility(visibleState = shown, enter = EnterTransition.None, exit = ExitTransition.None) {
        /** How far up it has come, 0 to 1. */
        val up = transition.animateFloat(
            transitionSpec = {
                when {
                    stillReduced -> tween(Motion.REDUCED_MS)
                    targetState == EnterExitState.Visible -> PAGE_UP
                    else -> PAGE_AWAY
                }
            },
            label = "pageUp",
        ) { if (it == EnterExitState.Visible) 1f else 0f }

        /** Where it is drawn, in pixels down from its place. With movement reduced it does not travel: it fades. */
        fun place(): Float = if (stillReduced) down().coerceAtLeast(0f) else pageDown(down(), up.value, height)

        /** How much of what is under it it covers, 0 to 1. */
        fun covers(): Float = (if (stillReduced) up.value.coerceIn(0f, 1f) else 1f) * pageCover(place(), height)

        DisposableEffect(homeCover) {
            val covering = { covers() }
            homeCover?.add(covering)
            onDispose { homeCover?.remove(covering) }
        }

        // Square to the screen's edges while it fills it; its top corners round as soon as it moves.
        val filling by remember { derivedStateOf { shown.isIdle && place() < 0.5f } }

        // The dim behind it clears as it goes, so nothing is left dimmed behind a page pulled away.
        val scrim = palette.scrim
        Box(Modifier.fillMaxSize().drawBehind { drawRect(scrim, alpha = scrim.alpha * covers()) })
        Column(
            Modifier
                .fillMaxSize()
                .onSizeChanged { height = it.height.toFloat().coerceAtLeast(1f) }
                // Moved while placing rather than on a layer, so its glass looks again at where it is
                // all the way down.
                .offset { IntOffset(0, place().roundToInt()) }
                .then(if (reduced) Modifier.graphicsLayer { alpha = up.value.coerceIn(0f, 1f) } else Modifier)
                .then(
                    // Liquid Glass's page is glass; another theme's is the theme's own page.
                    if (theme.isGlass) {
                        Modifier.glass(if (filling) RectangleShape else PAGE_LIFTED, strength = 1.2f, solid = 0.9f, border = false, shadow = false)
                    } else {
                        Modifier.themePage(palette, if (filling) RectangleShape else theme.shape(PAGE_LIFTED))
                    }
                )
                // Nothing underneath takes a touch; a tap on empty space puts the keyboard away.
                .pointerInput(Unit) { detectTapGestures { focus.clearFocus() } }
                .nestedScroll(pullThrough)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Column(Modifier.fillMaxSize().graphicsLayer { alpha = uncovered.value }) {
                PageBar(
                    title = title,
                    canGoBack = canGoBack,
                    titleShown = titleShown,
                    onBack = onBack,
                    onClose = onClose,
                    onPull = { delta ->
                        take()
                        // Never further up than its place, so a finger that went up first and then
                        // down has the page under it straight away.
                        pulled = (pulled + delta).coerceAtLeast(0f)
                    },
                    onLetGo = { velocity -> letGo(velocity) },
                )
                Box(Modifier.weight(1f).fillMaxWidth(), content = content)
            }
        }
    }
}

/** A page coming up, on a sheet's spring, followed to within a pixel of its place. */
private val PAGE_UP: SpringSpec<Float> = Motion.sheet<Float>().let { spring(it.dampingRatio, it.stiffness, PAGE_STILL) }

/** A page going, closed without a finger: down, with no bounce, on the spring a thrown one goes on. */
private val PAGE_AWAY: SpringSpec<Float> = Motion.close<Float>().let { spring(it.dampingRatio, it.stiffness, PAGE_STILL) }

/** Close enough to rest, as a share of the page's height: under a pixel on any phone. */
private const val PAGE_STILL = 0.0004f

/** A page off the screen's edges, being pulled or coming and going: its top corners rounded, as a sheet's are. */
private val PAGE_LIFTED = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)

/**
 * How much of the home screen the full pages over it cover, 0 to 1, for it to fade out and sink
 * back by. Every page on the screen says how much it covers; the one covering most counts. Read
 * while drawing, so the home screen follows a page pulled by a finger and composes nothing.
 */
@Stable
class HomeCover {
    private val pages = mutableStateListOf<() -> Float>()

    fun amount(): Float = pages.maxOfOrNull { it() } ?: 0f

    internal fun add(page: () -> Float) {
        pages += page
    }

    internal fun remove(page: () -> Float) {
        pages -= page
    }
}

/** The [HomeCover] of the home screen the pages stand over, or null where there is none. */
val LocalHomeCover = staticCompositionLocalOf<HomeCover?> { null }

/** How much less than its own size the home screen is drawn with a page all the way over it. */
const val PAGE_SINK = 0.04f

/**
 * Where a page is drawn, in pixels down from its place: as far as a finger has [pulled] it, or as
 * far as it still has to come up ([up], 0 to 1, of its [height]), whichever is further. So a page
 * let go part way down and closed goes on from there, and is never drawn above its place.
 */
fun pageDown(pulled: Float, up: Float, height: Float): Float =
    maxOf(pulled.coerceAtLeast(0f), (1f - up.coerceIn(0f, 1f)) * height)

/** How much of the home screen a page [down] pixels from its place covers, out of its [height]: all of it in its place, none off the screen. */
fun pageCover(down: Float, height: Float): Float =
    if (height <= 0f) 0f else (1f - down / height).coerceIn(0f, 1f)

/** The home screen's size under a page covering [cover] of it: sunk back a little, as under the drawer. */
fun homeScaleUnderPage(cover: Float): Float = 1f - PAGE_SINK * cover.coerceIn(0f, 1f)

@Composable
private fun PageBar(
    title: String,
    canGoBack: Boolean,
    titleShown: () -> Float,
    onBack: () -> Unit,
    onClose: () -> Unit,
    /** The bar pulled down, or back up, by this many pixels; and let go at this speed, down positive. */
    onPull: (Float) -> Unit,
    onLetGo: (Float) -> Unit,
) {
    val palette = LocalPalette.current
    val line = palette.onGlass
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            // A hairline under the bar once something has scrolled beneath it.
            .drawBehind {
                val shown = titleShown()
                if (shown > 0f) {
                    drawLine(
                        color = line,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx(),
                        alpha = 0.08f * shown,
                    )
                }
            }
            // Pulling the bar down takes the page down with it, the way a sheet is pulled away.
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta -> onPull(delta) },
                onDragStopped = { velocity -> onLetGo(velocity) },
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp)) {
            androidx.compose.animation.AnimatedVisibility(
                visible = canGoBack,
                enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.7f),
                exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.7f),
            ) {
                BackButton(onBack)
            }
        }
        Text(
            title,
            color = palette.onGlass,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    val shown = titleShown()
                    alpha = shown
                    // It comes up into the bar as it appears, as if the large title had shrunk into it.
                    translationY = (1f - shown) * 8.dp.toPx()
                },
        )
        Box(
            Modifier
                .size(36.dp)
                .pressBounce(0.88f)
                .clip(CircleShape)
                .background(palette.onGlass.copy(alpha = 0.08f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = palette.onGlass, modifier = Modifier.size(19.dp))
        }
    }
}

/** The round back arrow of a page's bar, and of a sheet that has pages. */
@Composable
fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Box(
        modifier
            .size(36.dp)
            .pressBounce(0.88f)
            .clip(CircleShape)
            .background(palette.onGlass.copy(alpha = 0.08f))
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = palette.onGlass, modifier = Modifier.size(19.dp))
    }
}

/**
 * How long a page takes to go once it is closed, and a little over. What a page was showing is
 * put away after this, out of sight, so that the next time it opens it starts at the top without
 * first arriving on whatever it was closed on.
 */
const val PAGE_GONE_MS = 420L

/** How far a page has to scroll before its large title has gone and the bar's small one is fully in. */
const val TITLE_GONE_PX = 110f

/**
 * The large title at the head of a page. It is part of what scrolls, and as it goes it shrinks
 * towards the bar, fades, and hangs back a little, so it seems to slide under the bar and turn
 * into the small title there. [scrolled] is how far the page has scrolled, read while drawing.
 */
@Composable
fun LargeTitle(text: String, scrolled: () -> Int, modifier: Modifier = Modifier) {
    Text(
        text,
        color = LocalPalette.current.onGlass,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.6).sp,
        modifier = modifier
            .padding(start = 4.dp, top = 2.dp, bottom = 4.dp)
            .graphicsLayer {
                val by = scrolled().coerceAtLeast(0)
                val gone = (by / TITLE_GONE_PX).coerceIn(0f, 1f)
                transformOrigin = TransformOrigin(0f, 0.5f)
                val size = 1f - 0.22f * gone
                scaleX = size
                scaleY = size
                alpha = 1f - gone
                translationY = by.coerceAtMost(TITLE_GONE_PX.toInt()) * 0.35f
            },
    )
}
