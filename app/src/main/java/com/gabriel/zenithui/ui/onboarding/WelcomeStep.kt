package com.gabriel.zenithui.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.foundation.layout.Arrangement
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

/**
 * Milliseconds after the screen appears at which each piece arrives. Unhurried: each piece has
 * finished settling before the next one starts, so the screen fills in like a slow exhale.
 */
private val CUES = listOf(300, 1000, 1700, 2300)

/**
 * The opening: a hello, one line on what happens next, and your name. Everything sits in one
 * calm column, the pieces arriving in quick succession. Tap to skip ahead.
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
                .animateContentSize(tween(1100, easing = FastOutSlowInEasing)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            Arrival(visible = stage >= 1, rise = 24) {
                ZenithMark(size = 76.dp)
            }

            Arrival(visible = stage >= 2, rise = 20) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(26.dp))
                    Text(
                        text = "Hi there 👋",
                        color = palette.onGlass,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.8).sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Welcome to Zenith. Let's make your home screen feel like yours — it only takes a minute.",
                        color = palette.onGlass.copy(alpha = 0.75f),
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // The one question, in its own card, with the reassurance right where it is needed.
            Arrival(visible = stage >= 3, rise = 22) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .glass(ZenithShapes.card, strength = 1f)
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = if (name.isBlank()) "What should we call you?" else "Nice to meet you, ${name.trim()}!",
                        color = palette.onGlass,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                    NameField(
                        name = name,
                        onNameChange = onNameChange,
                        onDone = { if (name.isNotBlank()) onContinue() },
                    )
                    Text(
                        text = "Just a first name is fine. It stays on your phone.",
                        color = palette.onGlassMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Arrival(visible = stage >= 4, rise = 26) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(16.dp))
                    GlassButton(
                        text = if (name.isBlank()) "Let's go" else "Continue as ${name.trim()}",
                        onClick = onContinue,
                        enabled = name.isNotBlank(),
                        filled = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

/**
 * One piece of the opening arriving: a long fade with a short glide up and the faintest growth.
 * No springs, so nothing bounces or overshoots — it just eases into place.
 */
@Composable
private fun Arrival(visible: Boolean, rise: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(1000, easing = LinearOutSlowInEasing)) +
            slideInVertically(tween(1200, easing = FastOutSlowInEasing)) { rise * 2 } +
            scaleIn(tween(1200, easing = FastOutSlowInEasing), initialScale = 0.97f),
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
