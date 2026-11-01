package com.gabriel.zenithui.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LayersClear
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.HomeGeometry
import com.gabriel.zenithui.data.ROW_UNITS
import com.gabriel.zenithui.data.WidgetLayouts
import com.gabriel.zenithui.data.WidgetSize
import com.gabriel.zenithui.data.WidgetSizes
import com.gabriel.zenithui.data.WidgetSpec
import com.gabriel.zenithui.data.Widgets
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.FullPage
import com.gabriel.zenithui.ui.common.LargeTitle
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.PAGE_GONE_MS
import com.gabriel.zenithui.ui.common.TITLE_GONE_PX
import com.gabriel.zenithui.ui.common.placedAgainWith
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberHaptics
import com.gabriel.zenithui.ui.common.shake
import com.gabriel.zenithui.ui.common.unfoldIn
import com.gabriel.zenithui.ui.common.unfoldOut
import com.gabriel.zenithui.ui.common.wordsChange
import com.gabriel.zenithui.ui.onboarding.PrimaryButton
import com.gabriel.zenithui.ui.onboarding.Segmented
import com.gabriel.zenithui.ui.theme.GlassMaterial
import com.gabriel.zenithui.ui.theme.LocalBackdrop
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.glass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

/**
 * The widgets, as a page of their own: every one of them drawn for real, in your theme, on your
 * wallpaper, with the clock telling the time and the battery showing its charge — what you see
 * here is what lands on the home screen. Tapping one grows it into a showcase where its sizes
 * stand in a row, each a different design; glass or no glass is one switch; and one button puts
 * it on the page.
 *
 * [geometry] is the home page's own lines, so every preview is cut to exactly the size it will be.
 */
@Composable
fun WidgetGallery(
    visible: Boolean,
    settings: ZenithSettings,
    geometry: HomeGeometry?,
    /** Puts the widget on the home screen at that size (in columns and row units); false when no page has room. */
    onAdd: (type: String, spanX: Int, spanY: Int) -> Boolean,
    onDismiss: () -> Unit,
    /** Puts another app's widget on the home screen; Android may be asked first. */
    onAddHosted: (android.appwidget.AppWidgetProviderInfo) -> Unit = {},
    /** Opened from a widget's "Stack another widget here": what is chosen goes onto that widget. */
    stacking: Boolean = false,
) {
    var bare by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<Picked?>(null) }
    // Where the card that was tapped sits within the page, for its showcase to grow out of.
    var pagesAt by remember { mutableStateOf(Rect.Zero) }
    var growFrom by remember { mutableStateOf(TransformOrigin.Center) }
    val reduced = LocalMotionLevel.current.reduced
    val scroll = rememberScrollState()
    // True once a showcase has been opened: coming back from it, the gallery is simply there,
    // rather than arriving card by card a second time.
    val returned = remember { booleanArrayOf(false) }
    // The next time it opens it starts at the top of the gallery. That is put right once the
    // page has gone, not as it comes back, so it never arrives showing the last showcase.
    LaunchedEffect(visible) {
        if (!visible) {
            delay(PAGE_GONE_MS)
            picked = null
            returned[0] = false
            scroll.scrollTo(0)
        }
    }
    BackHandler(enabled = visible) {
        if (picked != null) picked = null else onDismiss()
    }

    val inShowcase = animateFloatAsState(if (picked != null) 1f else 0f, tween(220), label = "showcaseTitle")
    FullPage(
        visible = visible,
        covered = false,
        title = picked?.let { (if (bare) bareOf(it.spec) else it.spec).title } ?: if (stacking) "Stack a widget" else "Widgets",
        canGoBack = picked != null,
        titleShown = { maxOf(inShowcase.value, (scroll.value / TITLE_GONE_PX).coerceIn(0f, 1f)) },
        onBack = { picked = null },
        onClose = onDismiss,
    ) {
        AnimatedContent(
            targetState = picked,
            // Only fades here: the showcase does its own growing, out of the card that was tapped
            // and back into it, because it has glass inside that must be told where it is.
            transitionSpec = {
                when {
                    reduced -> fadeIn(tween(Motion.REDUCED_MS)).togetherWith(fadeOut(tween(Motion.REDUCED_MS)))
                    targetState != null -> fadeIn(tween(200)).togetherWith(fadeOut(tween(160)))
                    else -> fadeIn(tween(240, delayMillis = 80)).togetherWith(fadeOut(tween(200)))
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { pagesAt = it.boundsInRoot() },
            label = "widgetPage",
        ) { chosen ->
            if (chosen == null) {
                Gallery(
                    settings = settings,
                    geometry = geometry,
                    bare = bare,
                    onBare = { bare = it },
                    scrolled = scroll,
                    stacking = stacking,
                    onHosted = { info ->
                        onAddHosted(info)
                        onDismiss()
                    },
                    arrive = remember { !returned[0] },
                    onPick = { spec, size, centre ->
                        returned[0] = true
                        if (!pagesAt.isEmpty) {
                            growFrom = TransformOrigin(
                                ((centre.x - pagesAt.left) / pagesAt.width).coerceIn(0f, 1f),
                                ((centre.y - pagesAt.top) / pagesAt.height).coerceIn(0f, 1f),
                            )
                        }
                        picked = Picked(spec, size)
                    },
                )
            } else {
                Showcase(
                    spec = chosen.spec,
                    opensOn = chosen.size,
                    settings = settings,
                    geometry = geometry,
                    bare = bare,
                    onBare = { bare = it },
                    growFrom = growFrom,
                    leaving = transition.targetState == EnterExitState.PostExit,
                    stacking = stacking,
                    onAdd = onAdd,
                    onAdded = onDismiss,
                )
            }
        }
    }
}

/** What was tapped in the gallery: the widget, and the size its card showed it at. */
private data class Picked(val spec: WidgetSpec, val size: WidgetSize)

// ------------------------------------------------------------------------------- the gallery

@Composable
private fun Gallery(
    settings: ZenithSettings,
    geometry: HomeGeometry?,
    bare: Boolean,
    onBare: (Boolean) -> Unit,
    scrolled: ScrollState,
    stacking: Boolean,
    onHosted: (android.appwidget.AppWidgetProviderInfo) -> Unit,
    /** Whether the page is opening, and the cards should come in one after another. */
    arrive: Boolean,
    onPick: (WidgetSpec, WidgetSize, Offset) -> Unit,
) {
    val palette = LocalPalette.current
    val density = LocalDensity.current
    // Four of them at their ordinary size, two to a row; the toggles at their widest across the
    // foot. Every card shows a size its showcase offers, and the showcase opens on that size.
    val square = Widgets.placeable.filter { it != Widgets.Toggles }
    val toggleSizes = remember(settings.columns, settings.rows) {
        WidgetSizes.options(Widgets.Toggles, settings.columns, settings.rows)
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrolled)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LargeTitle(if (stacking) "Stack a widget" else "Widgets", scrolled = { scrolled.value })
        Text(
            if (stacking) {
                "The one you choose goes onto the widget you held. Swipe up or down on the stack to go between them."
            } else {
                "Each one is live. Tap it to see its sizes and add it."
            },
            color = palette.onGlassMuted,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .arriving(arrive, 0),
        )
        // What the switch does is said under it, where the eye is when it is used.
        Column(Modifier.arriving(arrive, 1)) {
            StyleSwitch(bare, onBare)
            Spacer(Modifier.height(8.dp))
            StyleNote(bare, Modifier.padding(horizontal = 4.dp))
        }
        square.chunked(2).forEachIndexed { row, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEachIndexed { column, spec ->
                    val sizes = remember(spec, settings.columns, settings.rows) {
                        WidgetSizes.options(spec, settings.columns, settings.rows)
                    }
                    WidgetCard(
                        spec = spec,
                        size = sizes[WidgetSizes.startIndex(sizes)],
                        sizes = sizes.size,
                        wellHeight = 196.dp,
                        settings = settings,
                        geometry = geometry,
                        bare = bare,
                        modifier = Modifier
                            .weight(1f)
                            .arriving(arrive, 2 + row * 2 + column, card = true),
                        onPick = onPick,
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        WidgetCard(
            spec = Widgets.Toggles,
            size = toggleSizes.firstOrNull { it.name == WidgetSizes.WIDE } ?: toggleSizes[WidgetSizes.startIndex(toggleSizes)],
            sizes = toggleSizes.size,
            wellHeight = 196.dp,
            settings = settings,
            geometry = geometry,
            bare = bare,
            modifier = Modifier
                .fillMaxWidth()
                .arriving(arrive, 2 + square.size, card = true),
            onPick = onPick,
        )
        // Other apps' widgets, below Zenith's own.
        val cellDp = geometry?.let { with(density) { it.cellWidth.toDp().value } } ?: 80f
        val rowDp = geometry?.let { with(density) { (it.unitHeight * ROW_UNITS).toDp().value } } ?: 100f
        HostedWidgetPicker(
            cellDp = cellDp,
            rowDp = rowDp,
            columns = settings.columns,
            rows = settings.rows,
            modifier = Modifier
                .padding(top = 14.dp)
                .arriving(arrive, 3 + square.size, card = true),
            onPick = onHosted,
        )
        Spacer(Modifier.height(28.dp))
    }
}

/**
 * Glass or no glass: the one choice that changes every widget on the page at once, so each side
 * of it says so in a symbol as well as a word, a pane and a pane struck through.
 */
@Composable
private fun StyleSwitch(bare: Boolean, onBare: (Boolean) -> Unit) {
    Segmented(
        options = listOf("glass" to "Glass", "bare" to "No glass"),
        selected = if (bare) "bare" else "glass",
        onSelect = { onBare(it == "bare") },
        icons = listOf(Icons.Rounded.Layers, Icons.Rounded.LayersClear),
    )
}

/** What the style chosen means, changing where it stands when the other is chosen. */
@Composable
private fun StyleNote(bare: Boolean, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    AnimatedContent(
        targetState = bare,
        transitionSpec = { wordsChange(reduced) },
        modifier = modifier,
        label = "styleNote",
    ) { noGlass ->
        Text(
            text = if (noGlass) {
                "No pane round them: the numbers and icons are the glass, like the clock on an iPhone's Lock Screen."
            } else {
                "On a pane of glass that bends your wallpaper."
            },
            color = palette.onGlassMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

/**
 * How the gallery arrives when the page opens: the words first, then the cards one after another,
 * each from a little smaller and a little lower. Coming back from a showcase nothing moves.
 *
 * It is placed again on every frame of the way in. Glass finds the wallpaper behind it from where
 * it is placed, and a layer moving round it places nothing; without this a card would come to
 * rest showing the wallpaper from where it set off.
 */
@Composable
private fun Modifier.arriving(arrive: Boolean, turn: Int, card: Boolean = false): Modifier {
    if (!arrive) return this
    val reduced = LocalMotionLevel.current.reduced
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (reduced) {
            progress.animateTo(1f, tween(Motion.REDUCED_MS))
        } else {
            delay((if (card) 55L else 40L) * turn)
            progress.animateTo(1f, spring(dampingRatio = 0.86f, stiffness = 240f))
        }
    }
    return this
        .graphicsLayer {
            val value = progress.value
            alpha = value.coerceIn(0f, 1f)
            if (!reduced) {
                val from = if (card) 0.9f else 1f
                val scale = from + (1f - from) * value
                scaleX = scale
                scaleY = scale
                translationY = (1f - value) * (if (card) 70f else 40f)
            }
        }
        .placedAgainWith { progress.value }
}

/** One widget in the gallery: itself, live, on the wallpaper, with its name underneath. */
@Composable
private fun WidgetCard(
    spec: WidgetSpec,
    size: WidgetSize,
    /** How many sizes its showcase offers. */
    sizes: Int,
    wellHeight: Dp,
    settings: ZenithSettings,
    geometry: HomeGeometry?,
    bare: Boolean,
    modifier: Modifier,
    onPick: (WidgetSpec, WidgetSize, Offset) -> Unit,
) {
    val palette = LocalPalette.current
    val centre = remember { arrayOf(Offset.Zero) }
    val real = realSize(geometry, size, settings.columns)
    Column(
        modifier
            .pressBounce(0.97f)
            .onGloballyPositioned { centre[0] = it.boundsInRoot().center }
            .clip(RoundedCornerShape(26.dp))
            .background(palette.onGlass.copy(alpha = 0.06f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onPick(spec, size, centre[0]) }
            .padding(10.dp),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(wellHeight)
                .wallpaperWell(RoundedCornerShape(16.dp))
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Its real size, or as near as the card has room for.
            val fit = min(1f, min(maxWidth / real.width, maxHeight / real.height))
            LiveWidget(spec, real, fit, bare, settings)
        }
        Spacer(Modifier.height(10.dp))
        AnimatedContent(
            targetState = (if (bare) bareOf(spec) else spec).title,
            transitionSpec = { fadeIn(tween(220, delayMillis = 60)).togetherWith(fadeOut(tween(120))) },
            modifier = Modifier.padding(horizontal = 4.dp),
            label = "widgetCardName",
        ) { name ->
            Text(name, color = palette.onGlass, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            if (sizes == 1) "1 size" else "$sizes sizes",
            color = palette.onGlassMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 1.dp, bottom = 2.dp),
        )
    }
}

// ------------------------------------------------------------------------------- the showcase

/**
 * One widget, large. Its sizes stand in a row on the wallpaper, all to one scale so they can be
 * told apart, the one in the middle bright and the ones beside it standing back; a flick or a tap
 * brings another to the middle. Under them: the sizes by name, each over the room it takes on the
 * page; what the size in the middle is called and what it shows; the switch between glass and no
 * glass; and the button that puts it on the home screen.
 *
 * It grows out of the card that was tapped ([growFrom], as a share of the page) and, when it is
 * [leaving], goes back into it.
 */
@Composable
private fun Showcase(
    spec: WidgetSpec,
    /** The size its card in the gallery showed: the one to open on. */
    opensOn: WidgetSize,
    settings: ZenithSettings,
    geometry: HomeGeometry?,
    bare: Boolean,
    onBare: (Boolean) -> Unit,
    growFrom: TransformOrigin,
    leaving: Boolean,
    /** Going onto a widget already on the page, at its size, rather than a spot of its own. */
    stacking: Boolean = false,
    onAdd: (type: String, spanX: Int, spanY: Int) -> Boolean,
    onAdded: () -> Unit,
) {
    val palette = LocalPalette.current
    val haptics = rememberHaptics()
    val reduced = LocalMotionLevel.current.reduced
    val density = LocalDensity.current
    val sizes = remember(spec, settings.columns, settings.rows) { WidgetSizes.options(spec, settings.columns, settings.rows) }
    val start = sizes.indexOf(opensOn).takeIf { it >= 0 } ?: WidgetSizes.startIndex(sizes)
    val real = sizes.map { realSize(geometry, it, settings.columns) }

    val grow = remember { Animatable(1f) }
    LaunchedEffect(leaving, reduced) {
        when {
            reduced -> grow.snapTo(1f)
            leaving -> grow.animateTo(GROWS_FROM, Motion.close())
            else -> {
                grow.snapTo(GROWS_FROM)
                grow.animateTo(1f, Motion.open())
            }
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val size = grow.value
                scaleX = size
                scaleY = size
                transformOrigin = growFrom
            }
            // The glass on the stage has to follow it out of the card.
            .placedAgainWith { grow.value },
    ) {
        // Every size is drawn to one scale, so they can be told apart: the largest that lets
        // the widest of them fit with a neighbour showing at each side.
        val stageWidth = maxWidth - STAGE_INSET * 2
        val scale = min(
            1f,
            min((stageWidth - STAGE_PEEK * 2) / real.maxOf { it.width }, (STAGE_TALLEST - STAGE_MARGIN * 2) / real.maxOf { it.height }),
        )
        val widths = real.map { it.width * scale }
        // How far past the start of the row a size has to be for it to stand in the middle. The
        // row is padded so that the first does at nought.
        val toMiddle = { index: Int -> with(density) { ((widths[index] - widths[0]) / 2).roundToPx() } }
        // It opens on the size that was tapped.
        val strip = rememberLazyListState(start, toMiddle(start))
        // The size in the middle of the row: the one the words describe and the button adds.
        val current by remember(strip, sizes) {
            derivedStateOf { (stripPosition(strip, start) + 0.5f).toInt().coerceIn(0, sizes.lastIndex) }
        }
        var refusals by remember { mutableIntStateOf(0) }
        var full by remember { mutableStateOf(false) }
        val seen = remember { intArrayOf(-1) }
        LaunchedEffect(current) {
            // Another size might fit where this one did not.
            full = false
            // A tick as each size comes to the middle, though not for the one it opens on.
            if (seen[0] != -1) haptics.tick()
            seen[0] = current
        }
        // A size tapped, on the stage or in the row of names under it, comes to the middle.
        val scope = rememberCoroutineScope()
        val goTo: (Int) -> Unit = { index ->
            scope.launch {
                if (reduced) strip.scrollToItem(index, toMiddle(index)) else strip.animateScrollToItem(index, toMiddle(index))
            }
        }
        // What each size shows: every one is a design of its own, and says which.
        val kind = Widgets.baseType(spec.type)
        val notes = remember(real, kind, settings.toggles.size) {
            real.map { WidgetLayouts.note(kind, it.width.value, it.height.value, settings.toggles.size) }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            Stage(
                spec = spec,
                real = real,
                scale = scale,
                width = stageWidth,
                strip = strip,
                names = sizes.map { "${it.name}, ${it.spanX} by ${it.rows}" },
                bare = bare,
                settings = settings,
                onPick = goTo,
            )
            if (sizes.size > 1) {
                Spacer(Modifier.height(12.dp))
                SizePicker(
                    sizes = sizes,
                    columns = settings.columns,
                    current = current,
                    position = { stripPosition(strip, start) },
                    onPick = goTo,
                    modifier = Modifier.padding(horizontal = STAGE_INSET),
                )
            }
            Spacer(Modifier.height(14.dp))
            AnimatedContent(
                targetState = current,
                transitionSpec = { sizeChange(reduced, forwards = targetState > initialState) },
                label = "sizeName",
            ) { index ->
                val size = sizes[index]
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = STAGE_INSET + 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "${size.name}  \u00B7  ${size.spanX} \u00D7 ${size.rows}",
                        color = palette.onGlass,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    notes[index]?.let { note ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            note,
                            color = palette.onGlassMuted,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Column(Modifier.padding(horizontal = STAGE_INSET), horizontalAlignment = Alignment.CenterHorizontally) {
                StyleSwitch(bare, onBare)
                Spacer(Modifier.height(14.dp))
                AnimatedContent(
                    targetState = (if (bare) bareOf(spec) else spec).description,
                    transitionSpec = { wordsChange(reduced) },
                    label = "widgetAbout",
                ) { words ->
                    Text(
                        words,
                        color = palette.onGlassMuted,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(18.dp))
                PrimaryButton(
                    text = if (stacking) "Stack it here" else "Add to home screen",
                    modifier = Modifier.shake(refusals),
                    onClick = {
                        val size = sizes[current]
                        if (onAdd((if (bare) bareOf(spec) else spec).type, size.spanX, size.spanY)) {
                            haptics.confirm()
                            onAdded()
                        } else {
                            full = true
                            refusals++
                        }
                    },
                )
                AnimatedVisibility(
                    visible = full,
                    enter = unfoldIn(reduced),
                    exit = unfoldOut(reduced),
                ) {
                    Text(
                        "No page has room for it at this size. Try a smaller one, or free up some space.",
                        color = palette.onGlass,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Once it is on the page you can pull it to any size you like.",
                    color = palette.onGlassMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** The row of sizes, on a stretch of the wallpaper [width] across. */
@Composable
private fun Stage(
    spec: WidgetSpec,
    /** Each size as it is on the home page, and the one scale they are all drawn to. */
    real: List<DpSize>,
    scale: Float,
    width: Dp,
    strip: LazyListState,
    /** What each size is called, for a screen reader. */
    names: List<String>,
    bare: Boolean,
    settings: ZenithSettings,
    /** A size was tapped: bring it to the middle. */
    onPick: (Int) -> Unit,
) {
    val reduced = LocalMotionLevel.current.reduced
    LazyRow(
        state = strip,
        modifier = Modifier
            .padding(horizontal = STAGE_INSET)
            .fillMaxWidth()
            .height(real.maxOf { it.height } * scale + STAGE_MARGIN * 2)
            .wallpaperWell(RoundedCornerShape(30.dp)),
        contentPadding = PaddingValues(start = (width - real.first().width * scale) / 2, end = (width - real.last().width * scale) / 2),
        horizontalArrangement = Arrangement.spacedBy(STAGE_GAP),
        verticalAlignment = Alignment.CenterVertically,
        flingBehavior = rememberSnapFlingBehavior(strip, MiddleOfStage),
    ) {
        itemsIndexed(real) { index, size ->
            LiveWidget(
                spec = spec,
                real = size,
                scale = scale,
                bare = bare,
                settings = settings,
                label = names[index],
                modifier = Modifier
                    .graphicsLayer {
                        if (reduced) return@graphicsLayer
                        // The one in the middle is itself; the ones beside it stand back and dim.
                        val info = strip.layoutInfo
                        val me = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return@graphicsLayer
                        val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                        val away = (abs(me.offset + me.size / 2f - middle) / STAGE_REACH.toPx()).coerceIn(0f, 1f)
                        val back = 1f - 0.12f * away
                        scaleX = back
                        scaleY = back
                        alpha = 1f - 0.5f * away
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onPick(index) },
            )
        }
    }
}

/**
 * The sizes by name, each over the room it takes on the page ([SizeShape]), so they can be told
 * apart before any of them is looked at. A pill lies under the size in the middle of the stage and
 * goes with the stage as it is pulled, stretching towards the next size on the way and drawing
 * itself up there, as the pill of a row of options does; that size's shape lights up in the
 * accent. A tap on a name brings its size to the middle. All of it is drawn from [position], so
 * following a finger composes nothing again.
 */
@Composable
private fun SizePicker(
    sizes: List<WidgetSize>,
    columns: Int,
    /** The size in the middle, for a screen reader. */
    current: Int,
    /** Where the row of sizes stands: see [stripPosition]. */
    position: () -> Float,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val pill = palette.onGlass.copy(alpha = 0.1f)
    val tallest = sizes.maxOf { it.rows }
    Row(
        modifier
            .fillMaxWidth()
            .drawBehind {
                val each = size.width / sizes.size
                val (from, to) = PageEffects.pillSpan(position(), sizes.size)
                drawRoundRect(
                    color = pill,
                    topLeft = Offset(from * each, 0f),
                    size = Size((to - from) * each, size.height),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                )
            },
    ) {
        sizes.forEachIndexed { index, size ->
            val near = { (1f - abs(position() - index)).coerceIn(0f, 1f) }
            Column(
                Modifier
                    .weight(1f)
                    .pressBounce(0.92f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClickLabel = "Show this size") { onPick(index) }
                    .semantics {
                        contentDescription = "${size.name}, ${size.spanX} by ${size.rows}"
                        selected = index == current
                    }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SizeShape(
                    spanX = size.spanX,
                    rows = size.rows,
                    columns = columns,
                    maxRows = tallest,
                    modifier = Modifier.size(width = 42.dp, height = 28.dp),
                    lit = near,
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    size.name,
                    color = palette.onGlass,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier
                        .graphicsLayer { alpha = 0.66f + 0.34f * near() }
                        // Said already, with its cells, by the size it names.
                        .clearAndSetSemantics { },
                )
            }
        }
    }
}

/**
 * The name of a size giving way to the next one's as the row of sizes turns: it slides a little
 * the way the row went. With movement reduced one fades into the other where it stands.
 */
private fun sizeChange(reduced: Boolean, forwards: Boolean): ContentTransform {
    val way = if (forwards) 1 else -1
    return ContentTransform(
        targetContentEnter = if (reduced) {
            fadeIn(tween(Motion.REDUCED_MS))
        } else {
            slideInHorizontally(SIZE_NAME_SLIDE) { way * it / 8 } + fadeIn(tween(200, delayMillis = 40))
        },
        initialContentExit = if (reduced) {
            fadeOut(tween(Motion.REDUCED_MS))
        } else {
            slideOutHorizontally(tween(160)) { -way * it / 8 } + fadeOut(tween(110))
        },
        // A size whose line runs onto a second takes the room smoothly, and nothing is cut off.
        sizeTransform = SizeTransform(clip = false),
    )
}

private val SIZE_NAME_SLIDE = spring(dampingRatio = 0.8f, stiffness = 380f, visibilityThreshold = IntOffset(1, 1))

/**
 * Where a size comes to rest: the middle of the stage. The library's own Center is the middle of
 * what is left of the row once its padding is taken off each end, and this row is padded more at
 * one end than the other (each end by what lets its own size reach the middle), so that would
 * rest every size off to one side, and not where the dots, the words and the button look.
 */
private object MiddleOfStage : SnapPosition {
    override fun position(
        layoutSize: Int,
        itemSize: Int,
        beforeContentPadding: Int,
        afterContentPadding: Int,
        itemIndex: Int,
        itemCount: Int,
    ): Int = layoutSize / 2 - beforeContentPadding - itemSize / 2
}

/**
 * Where the row of sizes stands: the number of the size in the middle, with a fraction while it
 * is between two; [opensOn] until the row has been laid out. Read while drawing, so the dots
 * follow a finger without anything recomposing.
 */
private fun stripPosition(strip: LazyListState, opensOn: Int): Float {
    val info = strip.layoutInfo
    val items = info.visibleItemsInfo
    if (items.isEmpty()) return opensOn.toFloat()
    val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2f
    var before = items.first()
    if (middle <= before.offset + before.size / 2f) return before.index.toFloat()
    for (item in items) {
        val centre = item.offset + item.size / 2f
        if (centre >= middle) {
            val from = before.offset + before.size / 2f
            return before.index + ((middle - from) / (centre - from).coerceAtLeast(1f)).coerceIn(0f, 1f)
        }
        before = item
    }
    return before.index.toFloat()
}

/** How small the showcase is when it starts out of a card, as a share of itself. */
private const val GROWS_FROM = 0.35f

/** The gap between the stage and the sides of the page. */
private val STAGE_INSET = 16.dp

/** How much of the stage is left at each side of the widest size, for its neighbours to show in. */
private val STAGE_PEEK = 26.dp

/** The room above and below the tallest size. */
private val STAGE_MARGIN = 22.dp

/** The tallest the stage gets; sizes are scaled down to fit before it gets taller. */
private val STAGE_TALLEST = 380.dp

private val STAGE_GAP = 14.dp

/** How far from the middle a size is when it has stood all the way back. */
private val STAGE_REACH = 230.dp

/** The "No glass" version of [spec]. */
private fun bareOf(spec: WidgetSpec): WidgetSpec =
    Widgets.noGlass.firstOrNull { Widgets.baseType(it.type) == spec.type } ?: spec

// ------------------------------------------------------------------------------- a live widget

/**
 * What a widget is shown on: the wallpaper itself, seen through the page as through a window, so
 * a preview sits on what it will sit on at home and its glass bends the same picture. Where
 * Zenith has no copy of the wallpaper to show, a dark plate, which anything reads on.
 */
@Composable
private fun Modifier.wallpaperWell(shape: Shape): Modifier =
    if (LocalBackdrop.current != null) {
        glass(shape, strength = 0.6f, material = GlassMaterial.Clear, shadow = false)
    } else {
        clip(shape).background(Color.Black.copy(alpha = 0.3f))
    }

/**
 * The size a widget of [size] is on the home page. A widget lays itself out differently for each
 * shape of space, so only its real size shows the real thing.
 */
@Composable
private fun realSize(geometry: HomeGeometry?, size: WidgetSize, columns: Int): DpSize {
    val density = LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp
    return remember(geometry, size, columns, screenWidth, density) {
        if (geometry != null) {
            val frame = geometry.widgetFrame(size.spanX, size.spanY)
            with(density) { DpSize(frame.width.toDp(), frame.height.toDp()) }
        } else {
            // Before the home page has been measured: near enough, from the width of the screen.
            val cell = (screenWidth.dp - 24.dp) / columns.coerceAtLeast(1)
            DpSize(cell * size.spanX - 14.dp, 104.dp * size.rows - 14.dp)
        }
    }
}

/**
 * The real widget, drawn by the code that draws it on the home screen at the size it is there
 * ([real]), then scaled by [scale]. It takes up exactly the room it is drawn in.
 *
 * It is a picture of the widget, not the widget: nothing inside it takes a touch, though a touch
 * on it still reaches whatever it sits on. A screen reader is told the same: the buttons and the
 * note inside it are not there to be pressed, so all it hears of the picture is [label], if any.
 */
@Composable
private fun LiveWidget(
    spec: WidgetSpec,
    real: DpSize,
    scale: Float,
    bare: Boolean,
    settings: ZenithSettings,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val shown = DpSize(real.width * scale, real.height * scale)
    Box(modifier.size(shown)) {
        AnimatedContent(
            targetState = bare,
            transitionSpec = { fadeIn(tween(240)).togetherWith(fadeOut(tween(160))) },
            modifier = Modifier.clearAndSetSemantics { if (label != null) contentDescription = label },
            label = "widgetStyle",
        ) { noGlass ->
            Box(Modifier.size(shown)) {
                Box(
                    Modifier
                        .wrapContentSize(Alignment.TopStart, unbounded = true)
                        .requiredSize(real)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0f)
                        },
                ) {
                    ZenithWidget(
                        type = (if (noGlass) bareOf(spec) else spec).type,
                        userName = settings.userName,
                        note = settings.note,
                        toggles = settings.toggles,
                        onNoteChange = {},
                        onSearch = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        // Over the top of it: takes every touch meant for the widget and does nothing with it,
        // without using it up, so whatever the widget sits on still hears the tap.
        Box(
            Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) awaitPointerEvent()
                    }
                }
        )
    }
}
