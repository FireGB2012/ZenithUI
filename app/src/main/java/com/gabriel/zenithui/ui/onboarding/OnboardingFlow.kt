package com.gabriel.zenithui.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.gabriel.zenithui.ZenithViewModel
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.AuroraBackdrop
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithTheme

/** The first-run experience: name, look, widgets, then a home screen built for you. */
@Composable
fun OnboardingFlow(
    viewModel: ZenithViewModel,
    backdrop: ImageBitmap?,
    screenSize: IntSize,
    onFinished: () -> Unit,
) {
    var step by rememberSaveable { mutableStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var paletteId by rememberSaveable { mutableStateOf("midnight") }
    var appearance by rememberSaveable { mutableStateOf("dark") }
    var glass by rememberSaveable { mutableStateOf(0.85f) }
    var columns by rememberSaveable { mutableStateOf(4) }
    var rows by rememberSaveable { mutableStateOf(5) }
    var iconShape by rememberSaveable { mutableStateOf("squircle") }
    val widgets = remember { androidx.compose.runtime.mutableStateListOf("clock", "toggles") }

    val preview = ZenithSettings(
        userName = name,
        paletteId = paletteId,
        appearance = appearance,
        glass = glass,
        columns = columns,
        rows = rows,
        iconShape = iconShape,
    )

    // Onboarding paints its own aurora, so glass panels stay pure translucency here.
    ZenithTheme(settings = preview, backdrop = null, screenSize = screenSize) {
        Box(Modifier.fillMaxSize()) {
            AuroraBackdrop()

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState > initialState
                    val direction = if (forward) 1 else -1
                    val slide = spring<IntOffset>(dampingRatio = 0.7f, stiffness = 150f)
                    (
                        slideInHorizontally(slide) { (it * 0.22f * direction).toInt() } +
                            fadeIn(tween(420)) +
                            scaleIn(spring(dampingRatio = 0.62f, stiffness = 160f), initialScale = 0.92f)
                        ).togetherWith(
                        slideOutHorizontally(slide) { (-it * 0.22f * direction).toInt() } +
                            fadeOut(tween(300)) +
                            scaleOut(tween(380), targetScale = 0.95f)
                    )
                },
                label = "onboarding",
            ) { current ->
                when (current) {
                    0 -> WelcomeStep(
                        name = name,
                        onNameChange = { name = it },
                        onContinue = { step = 1 },
                    )

                    1 -> ThemeStep(
                        name = name,
                        paletteId = paletteId,
                        appearance = appearance,
                        glass = glass,
                        onPalette = { paletteId = it },
                        onAppearance = { appearance = it },
                        onGlass = { glass = it },
                        onBack = { step = 0 },
                        onContinue = { step = 2 },
                    )

                    2 -> WidgetStep(
                        selected = widgets,
                        columns = columns,
                        rows = rows,
                        iconShape = iconShape,
                        onToggle = { type -> if (type in widgets) widgets.remove(type) else widgets.add(type) },
                        onDensity = { c, r -> columns = c; rows = r },
                        onIconShape = { iconShape = it },
                        onBack = { step = 1 },
                        onContinue = { step = 3 },
                    )

                    3 -> SetupStep(
                        viewModel = viewModel,
                        name = name,
                        paletteId = paletteId,
                        appearance = appearance,
                        glass = glass,
                        iconShape = iconShape,
                        widgets = widgets.toList(),
                        columns = columns,
                        rows = rows,
                        onDone = { step = 4 },
                    )

                    else -> HelpStep(
                        name = name,
                        onDone = {
                            viewModel.markOnboarded()
                            onFinished()
                        },
                    )
                }
            }

            if (step < 3) {
                StepIndicator(
                    step = step,
                    total = 3,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 18.dp),
                )
            }
        }
    }
}

@Composable
private fun StepIndicator(step: Int, total: Int, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(total) { index ->
            val active = index == step
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .height(4.dp)
                    .width(if (active) 26.dp else 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (active) palette.accent else palette.onGlass.copy(alpha = 0.22f)
                    )
            )
        }
        Box(Modifier.size(0.dp))
    }
}
