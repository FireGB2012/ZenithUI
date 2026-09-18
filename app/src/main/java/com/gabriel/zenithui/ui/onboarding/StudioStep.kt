package com.gabriel.zenithui.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.GridDensity
import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.Palettes

/** The widgets on offer, in the order they are worth trying, with short everyday names. */
private val WIDGET_CHOICES = listOf(
    "clock" to "Clock",
    "toggles" to "Toggles",
    "battery" to "Battery",
    "calendar" to "Calendar",
    "notes" to "Note",
    "search" to "Search",
)

/**
 * Picking a look, choosing what goes on the page, and watching it get built — one screen with
 * your phone in the middle of it. Only the words above and the controls below change; the phone
 * stays put and updates as you go, and when the controls fold away it grows into the space.
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
    onPalette: (String) -> Unit,
    onAppearance: (String) -> Unit,
    onToggleWidget: (String) -> Unit,
    onDensity: (GridDensity) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        // The words change in place; the phone below them does not move.
        val titleKey = if (step >= 3 && built) 4 else step
        AnimatedContent(
            targetState = titleKey,
            transitionSpec = {
                (fadeIn(tween(260, delayMillis = 80)) + slideInVertically(spring(dampingRatio = 1f, stiffness = 300f)) { it / 6 })
                    .togetherWith(fadeOut(tween(140)))
                    .using(SizeTransform(clip = false))
            },
            label = "studioTitle",
            modifier = Modifier.padding(horizontal = 28.dp),
        ) { key ->
            val first = name.trim()
            when (key) {
                1 -> ScreenTitle(
                    eyebrow = if (first.isEmpty()) "Your look" else "Nice to meet you, $first",
                    headline = "Pick a colour.",
                    lede = null,
                    play = playIntro,
                )

                2 -> ScreenTitle(
                    eyebrow = "Your home screen",
                    headline = "What goes on it?",
                    lede = null,
                    play = true,
                )

                3 -> ScreenTitle(
                    eyebrow = "One moment",
                    headline = "Putting it together.",
                    lede = null,
                    play = true,
                )

                else -> ScreenTitle(
                    eyebrow = if (first.isEmpty()) "Done" else "Done, $first",
                    headline = "This is yours now.",
                    lede = "Hold anything to move it. Nothing here is fixed.",
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
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 20.dp),
        )

        AnimatedContent(
            targetState = step.coerceAtMost(3),
            transitionSpec = {
                (fadeIn(tween(240, delayMillis = 90)) + slideInVertically(spring(dampingRatio = 1f, stiffness = 320f)) { it / 5 })
                    .togetherWith(fadeOut(tween(120)) + slideOutVertically(tween(160)) { it / 8 })
                    // The controls' height eases, so the phone grows or shrinks smoothly with it.
                    .using(SizeTransform(clip = false) { _, _ -> spring(dampingRatio = 1f, stiffness = 260f, visibilityThreshold = IntSize(1, 1)) })
            },
            label = "studioControls",
        ) { current ->
            when (current) {
                1 -> LookControls(
                    paletteId = settings.paletteId,
                    appearance = settings.appearance,
                    onPalette = onPalette,
                    onAppearance = onAppearance,
                )

                2 -> HomeControls(
                    selected = selectedWidgets,
                    density = density,
                    columns = settings.columns,
                    rows = settings.rows,
                    onToggle = onToggleWidget,
                    onDensity = onDensity,
                )

                else -> Spacer(Modifier.height(0.dp))
            }
        }
    }
}

@Composable
private fun LookControls(
    paletteId: String,
    appearance: String,
    onPalette: (String) -> Unit,
    onAppearance: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(Palettes.all, key = { it.id }) { option ->
                Swatch(
                    name = option.name,
                    colors = listOf(option.accent, option.accentAlt),
                    selected = option.id == paletteId,
                    onClick = { onPalette(option.id) },
                )
            }
        }
        Box(Modifier.padding(horizontal = 24.dp)) {
            Segmented(
                options = listOf("auto" to "Auto", "light" to "Light", "dark" to "Dark"),
                selected = appearance,
                onSelect = onAppearance,
            )
        }
    }
}

@Composable
private fun Swatch(name: String, colors: List<Color>, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val ring by animateColorAsState(
        if (selected) palette.onGlass else Color.Transparent,
        tween(220),
        label = "swatchRing",
    )
    val inner by animateDpAsState(if (selected) 38.dp else 44.dp, spring(dampingRatio = 0.7f, stiffness = 420f), label = "swatchSize")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        // No ripple: the press squeeze is the feedback, and a ripple would be cut to a box that
        // does not match the round swatch. Clipping the whole column cut the names off instead.
        modifier = Modifier
            .pressBounce(0.92f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(
            Modifier
                .size(50.dp)
                .border(2.dp, ring, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(inner)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(colors))
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = name,
            color = if (selected) palette.onGlass else palette.onGlassMuted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
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
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
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
        Spacer(Modifier.height(4.dp))
        Segmented(
            options = GridDensity.entries.map { it.id to it.label },
            selected = density.id,
            onSelect = { onDensity(GridDensity.byId(it)) },
        )
        Text(
            text = "$columns across, $rows down. You can change this later.",
            color = palette.onGlassMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
