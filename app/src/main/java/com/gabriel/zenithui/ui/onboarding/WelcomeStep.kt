package com.gabriel.zenithui.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.common.GlassButton
import com.gabriel.zenithui.ui.common.shimmer
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import kotlinx.coroutines.delay
import kotlin.math.sin

/** Milliseconds after the screen appears at which each piece arrives. */
private val CUES = listOf(260, 1050, 1750, 2500, 2950, 3350)

/**
 * The opening. One thing arrives at a time — the mark, a hello, the name, the field — each on a
 * spring, with the column re-centring itself as the content grows. Tap to skip ahead.
 */
@Composable
fun WelcomeStep(
    name: String,
    onNameChange: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val palette = LocalPalette.current
    var stage by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        var elapsed = 0
        CUES.forEachIndexed { index, cue ->
            delay((cue - elapsed).toLong())
            elapsed = cue
            if (stage < index + 1) stage = index + 1
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { if (stage < CUES.size) stage = CUES.size }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 30.dp)
                .animateContentSize(spring(dampingRatio = 0.72f, stiffness = 150f)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.9f))

            Arrival(visible = stage >= 1, rise = 40) {
                ZenithMark()
            }

            Arrival(visible = stage >= 2, rise = 30) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(30.dp))
                    Text(
                        text = "Hey.",
                        color = palette.onGlass,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-2).sp,
                    )
                }
            }

            Arrival(visible = stage >= 3, rise = 24) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "This is",
                        color = palette.onGlassMuted,
                        fontSize = 15.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Zenith UI",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-1.4).sp,
                        color = palette.onGlass,
                        modifier = Modifier.shimmer(),
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Arrival(visible = stage >= 4, rise = 26) {
                Text(
                    text = if (name.isBlank()) "What should we call you?" else "Nice to meet you, ${name.trim()}.",
                    color = palette.onGlass,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            Arrival(visible = stage >= 5, rise = 30) {
                NameField(
                    name = name,
                    onNameChange = onNameChange,
                    onDone = { if (name.isNotBlank()) onContinue() },
                )
            }

            Arrival(visible = stage >= 6, rise = 34) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(18.dp))
                    GlassButton(
                        text = if (name.isBlank()) "Enter your name" else "Continue",
                        onClick = onContinue,
                        enabled = name.isNotBlank(),
                        filled = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(34.dp))
        }
    }
}

/** One piece of the opening arriving: fade, rise and a touch of scale, all on the same spring. */
@Composable
private fun Arrival(visible: Boolean, rise: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(560)) +
            slideInVertically(spring(dampingRatio = 0.58f, stiffness = 170f)) { rise * 4 } +
            scaleIn(spring(dampingRatio = 0.52f, stiffness = 160f), initialScale = 0.90f),
    ) {
        content()
    }
}

@Composable
private fun NameField(name: String, onNameChange: (String) -> Unit, onDone: () -> Unit) {
    val palette = LocalPalette.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .glass(ZenithShapes.pill, strength = 1f),
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = name,
            onValueChange = { if (it.length <= 24) onNameChange(it) },
            singleLine = true,
            textStyle = TextStyle(
                color = palette.onGlass,
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
            cursorBrush = SolidColor(palette.accent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.Center) {
                    if (name.isEmpty()) {
                        Text(
                            text = "Your name",
                            color = palette.onGlass.copy(alpha = 0.35f),
                            fontSize = 19.sp,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

/** The mark: a glass tile with a slow conic sweep and a gentle float. */
@Composable
fun ZenithMark(size: Dp = 104.dp) {
    val palette = LocalPalette.current
    val transition = rememberInfiniteTransition(label = "mark")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
        label = "bob",
    )

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { translationY = sin(bob) * 8f }
            .glass(ZenithShapes.icon, strength = 1.2f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = sweep; alpha = 0.55f }
                .background(
                    Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            palette.accent.copy(alpha = 0.85f),
                            Color.Transparent,
                            palette.accentAlt.copy(alpha = 0.7f),
                            Color.Transparent,
                        )
                    )
                )
        )
        Text(
            text = "Z",
            fontSize = (size.value * 0.48f).sp,
            fontWeight = FontWeight.Light,
            color = palette.onGlass,
        )
    }
}
