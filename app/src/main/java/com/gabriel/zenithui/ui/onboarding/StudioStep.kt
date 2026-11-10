package com.gabriel.zenithui.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.GridDensity
import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.theme.LocalPalette

/** The widgets on offer, in the order they are worth trying, with short everyday names. */
private val WIDGET_CHOICES = listOf(
    "clock" to "Clock",
    "toggles" to "Toggles",
    "battery" to "Battery",
    "calendar" to "Calendar",
    "notes" to "Note",
    "search" to "Search",
)

/** The grids on offer: the iPhone's first, which setup starts on, then the others as they come. */
private val GRID_CHOICES = listOf(GridDensity.Four) + GridDensity.entries.filter { it != GridDensity.Four }

/** A point on screen, remembered without being state: taps read it, nothing redraws on it. */
private class Spot {
    var centre = Offset.Zero
}

/**
 * Everything from choosing light or dark to the last tips happens on one stage with your phone in
 * the middle of it. Only the words above and the controls below change; the phone stays put,
 * updates as you go, and grows into any space the controls give up.
 *
 * [chrome] fades the words and controls away for the hand-off, leaving the phone alone on screen.
 */
@Composable
fun StudioStep(
    step: Int,
    built: Boolean,
    name: String,
    settings: ZenithSettings,
    items: List<HomeItem>,
    dock: List<String>,
    appsByKey: Map<String, AppInfo>,
    wallpaper: ImageBitmap?,
    screenWidth: Dp,
    screenHeight: Dp,
    shown: Int,
    selectedWidgets: List<String>,
    density: GridDensity,
    playIntro: Boolean,
    deviceCorner: Dp,
    onAppearance: (String, Offset) -> Unit,
    onToggleWidget: (String) -> Unit,
    onDensity: (GridDensity) -> Unit,
    halo: () -> Float,
    sweep: () -> Float,
    zoom: () -> Float,
    chrome: () -> Float,
) {
    Column(Modifier.fillMaxSize()) {
        // The words change in place; the phone below them does not move.
        val titleKey = when {
            step == Steps.TIPS -> Steps.TIPS
            step >= Steps.BUILD && built -> DONE
            else -> step
        }
        AnimatedContent(
            targetState = titleKey,
            transitionSpec = {
                (fadeIn(tween(260, delayMillis = 80)) + slideInVertically(spring(dampingRatio = 1f, stiffness = 300f)) { it / 6 })
                    .togetherWith(fadeOut(tween(140)))
                    .using(SizeTransform(clip = false))
            },
            label = "studioTitle",
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .padding(top = 8.dp)
                .graphicsLayer { alpha = chrome() },
        ) { key ->
            val first = name.trim()
            when (key) {
                Steps.LOOK -> ScreenTitle(
                    eyebrow = if (first.isEmpty()) "Your look" else "Nice to meet you, $first",
                    headline = "Light or dark?",
                    lede = null,
                    play = playIntro,
                )

                Steps.HOME -> ScreenTitle(
                    eyebrow = "Your home screen",
                    headline = "What goes on it?",
                    lede = null,
                    play = true,
                )

                Steps.BUILD -> ScreenTitle(
                    eyebrow = "One moment",
                    headline = "Putting it together.",
                    lede = null,
                    play = true,
                )

                Steps.TIPS -> ScreenTitle(
                    eyebrow = "Before you go",
                    headline = "Three quick moves.",
                    lede = null,
                    play = true,
                )

                else -> ScreenTitle(
                    eyebrow = if (first.isEmpty()) "Done" else "Done, $first",
                    headline = "This is yours now.",
                    lede = "Hold anything to move it.\nNothing here is fixed.",
                    play = true,
                )
            }
        }

        PhonePreview(
            items = items,
            dock = dock,
            appsByKey = appsByKey,
            settings = settings,
            wallpaper = wallpaper,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            shown = shown,
            deviceCorner = deviceCorner,
            halo = halo,
            sweep = sweep,
            zoom = zoom,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 16.dp),
        )

        val controlsKey = when (step) {
            Steps.LOOK, Steps.HOME, Steps.TIPS -> step
            else -> NO_CONTROLS
        }
        AnimatedContent(
            targetState = controlsKey,
            transitionSpec = {
                (fadeIn(tween(240, delayMillis = 90)) + slideInVertically(spring(dampingRatio = 1f, stiffness = 320f)) { it / 5 })
                    .togetherWith(fadeOut(tween(120)) + slideOutVertically(tween(160)) { it / 8 })
                    // The controls' height eases, so the phone grows or shrinks smoothly with it.
                    .using(SizeTransform(clip = false) { _, _ -> spring(dampingRatio = 1f, stiffness = 260f, visibilityThreshold = IntSize(1, 1)) })
            },
            label = "studioControls",
            modifier = Modifier.graphicsLayer { alpha = chrome() },
        ) { current ->
            when (current) {
                Steps.LOOK -> LookControls(
                    appearance = settings.appearance,
                    onAppearance = onAppearance,
                )

                Steps.HOME -> HomeControls(
                    selected = selectedWidgets,
                    density = density,
                    columns = settings.columns,
                    rows = settings.rows,
                    onToggle = onToggleWidget,
                    onDensity = onDensity,
                )

                Steps.TIPS -> QuickTips(play = true)

                else -> Spacer(Modifier.height(0.dp))
            }
        }
    }
}

/** Title key for the finished phone, shown on BUILD once built and behind the Plus sheet. */
private const val DONE = 100
private const val NO_CONTROLS = -1

/**
 * Light or dark, the step's one choice: Zenith has one look, so there is no colour to pick. The
 * tap tells the backdrop where to start its ring of light.
 */
@Composable
private fun LookControls(
    appearance: String,
    onAppearance: (String, Offset) -> Unit,
) {
    val segment = remember { Spot() }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .padding(horizontal = 24.dp)
                .onGloballyPositioned { segment.centre = it.boundsInRoot().center },
        ) {
            Segmented(
                options = listOf("auto" to "Auto", "light" to "Light", "dark" to "Dark"),
                selected = appearance,
                onSelect = { onAppearance(it, segment.centre) },
            )
        }
    }
}

@Composable
private fun HomeControls(
    selected: List<String>,
    density: GridDensity,
    columns: Int,
    rows: Int,
    onToggle: (String) -> Unit,
    onDensity: (GridDensity) -> Unit,
) {
    val palette = LocalPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            WIDGET_CHOICES.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { (type, label) ->
                        ChoiceChip(
                            label = label,
                            selected = type in selected,
                            onClick = { onToggle(type) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Segmented(
            options = GRID_CHOICES.map { it.id to it.label },
            selected = density.id,
            onSelect = { onDensity(GridDensity.byId(it)) },
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "$columns across, $rows down. You can change this later.",
            color = palette.onGlassMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
    }
}
