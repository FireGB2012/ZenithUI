package com.gabriel.zenithui.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.GridDensity
import com.gabriel.zenithui.data.GlassLevels
import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.OptimizeJob
import com.gabriel.zenithui.data.Optimizing
import com.gabriel.zenithui.data.Theme
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.theme.GraphicsWarmUp
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalTheme
import com.gabriel.zenithui.ui.theme.corners
import com.gabriel.zenithui.ui.theme.glass
import com.gabriel.zenithui.ui.theme.isGlass
import kotlinx.coroutines.flow.StateFlow

/**
 * The widgets on offer, in the order they are worth trying, with short everyday names. Search is
 * not one of them: it is the pill under the page, on every home screen, so a chip for it would
 * change nothing.
 */
private val WIDGET_CHOICES = listOf(
    "clock" to "Clock",
    "toggles" to "Toggles",
    "battery" to "Battery",
    "calendar" to "Calendar",
    "notes" to "Note",
)

/** The grids on offer: the iPhone's first, which setup starts on, then the others as they come. */
private val GRID_CHOICES = GridDensity.offered

/** A point on screen, remembered without being state: taps read it, nothing redraws on it. */
internal class Spot {
    var centre = Offset.Zero
}

/**
 * Everything from choosing a theme to the last tips happens on one stage with your phone in the
 * middle of it. Only the words above and the card of controls below change; the phone stays put,
 * updates as you go, and grows into any space the card gives up. The card is one pane of glass
 * that takes the shape of each step's controls in turn, rather than a new card for each.
 *
 * [chrome] fades the words and controls away for the hand-off, leaving the phone alone on screen.
 */
@Composable
fun StudioStep(
    step: Int,
    built: Boolean,
    optimized: Boolean,
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
    optimizing: StateFlow<Optimizing?>,
    glassLevels: GlassLevels?,
    appearanceDark: Boolean,
    reduced: Boolean,
    onTheme: (Theme, Offset) -> Unit,
    onAppearance: (String, Offset) -> Unit,
    onToggleWidget: (String) -> Unit,
    onDensity: (GridDensity) -> Unit,
    onGraphicsWarm: () -> Unit,
    halo: () -> Float,
    sweep: () -> Float,
    zoom: () -> Float,
    chrome: () -> Float,
) {
    Column(Modifier.fillMaxSize()) {
        // The words change in place; the phone below them does not move.
        val titleKey = when (step) {
            // Built, it moves straight on to optimizing: the words go there with it.
            Steps.BUILD -> if (built) Steps.OPTIMIZE else Steps.BUILD
            Steps.OPTIMIZE -> if (optimized) DONE else Steps.OPTIMIZE
            // The finished phone, behind the Plus sheet.
            Steps.PLUS -> DONE
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
                    eyebrow = if (first.isEmpty()) "Your look" else "Make it yours, $first",
                    headline = "Pick a theme.",
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

                Steps.OPTIMIZE -> ScreenTitle(
                    eyebrow = "Almost there",
                    headline = "Optimizing Zenith.",
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
            floats = true,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 16.dp),
        )

        val controlsKey = when (step) {
            Steps.LOOK, Steps.HOME, Steps.TIPS -> step
            // The card comes back for the work as soon as the page is built, and goes once it is done.
            Steps.BUILD -> if (built) Steps.OPTIMIZE else NO_CONTROLS
            Steps.OPTIMIZE -> if (optimized) NO_CONTROLS else Steps.OPTIMIZE
            else -> NO_CONTROLS
        }
        val onCard = controlsKey != NO_CONTROLS
        val card = animateFloatAsState(if (onCard) 1f else 0f, tween(if (onCard) 260 else 160), label = "controlsCard")
        val glassy = LocalTheme.current.isGlass
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp)
                .graphicsLayer { alpha = chrome() },
        ) {
            // The card: one pane of glass the size of whatever is on it, so as the controls change
            // from step to step it changes shape with them, on the same spring, rather than being
            // swapped for another. It fades out where a step has nothing to put on it.
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = card.value }
                    .glass(corners(CARD_RADIUS), strength = 0.95f, solid = 0.3f),
            )
            AnimatedContent(
                targetState = controlsKey,
                transitionSpec = {
                    (fadeIn(tween(240, delayMillis = 90)) + slideInVertically(spring(dampingRatio = 1f, stiffness = 320f)) { it / 5 })
                        .togetherWith(fadeOut(tween(120)) + slideOutVertically(tween(160)) { it / 8 })
                        // The card's height eases, so the phone grows or shrinks smoothly with it.
                        .using(SizeTransform(clip = false) { _, _ -> spring(dampingRatio = 1f, stiffness = 260f, visibilityThreshold = IntSize(1, 1)) })
                },
                label = "studioControls",
                modifier = Modifier.fillMaxWidth(),
            ) { current ->
                when (current) {
                    Steps.LOOK -> ThemeControls(
                        themeId = settings.theme,
                        appearance = settings.appearance,
                        font = settings.font,
                        appearanceDark = appearanceDark,
                        reduced = reduced,
                        play = playIntro,
                        onTheme = onTheme,
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

                    Steps.OPTIMIZE -> OptimizeCard(
                        progress = optimizing,
                        glassy = glassy,
                        reduced = reduced,
                        warmUp = { job ->
                            // Once the wallpaper's copies are made: the glass is drawn with them.
                            if (job == OptimizeJob.GRAPHICS) {
                                GraphicsWarmUp(levels = glassLevels, onWarm = onGraphicsWarm)
                            }
                        },
                    )

                    Steps.TIPS -> QuickTips(play = true)

                    else -> Spacer(Modifier.height(0.dp))
                }
            }
        }
    }
}

/** Title key for the finished phone: once it is optimized, and behind the Plus sheet. */
private const val DONE = 100
private const val NO_CONTROLS = -1

/** The corners of the card setup's controls sit on. */
private val CARD_RADIUS = 28.dp

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
            .padding(horizontal = 14.dp, vertical = 14.dp),
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
