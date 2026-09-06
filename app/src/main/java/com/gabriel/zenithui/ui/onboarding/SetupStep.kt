package com.gabriel.zenithui.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ZenithViewModel
import com.gabriel.zenithui.ui.common.GlassButton
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private const val SETUP_MILLIS = 20_000f

private val STAGES = listOf(
    "Reading your app library",
    "Sorting apps the way you will use them",
    "Placing your widgets",
    "Blending glass with your wallpaper",
    "Tuning animations for your display",
    "Final polish",
)

/** Twenty seconds of real work: the device is read, sorted and laid out while this fills. */
@Composable
fun SetupStep(
    viewModel: ZenithViewModel,
    name: String,
    paletteId: String,
    appearance: String,
    glass: Float,
    iconShape: String,
    widgets: List<String>,
    columns: Int,
    rows: Int,
    onDone: () -> Unit,
) {
    val palette = LocalPalette.current
    var progress by remember { mutableFloatStateOf(0f) }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val work = launch(Dispatchers.Default) {
            viewModel.buildHome(name, paletteId, appearance, widgets, columns, rows)
        }
        var startNanos = 0L
        while (true) {
            val fraction = withFrameNanos { now ->
                if (startNanos == 0L) startNanos = now
                ((now - startNanos) / 1_000_000f) / SETUP_MILLIS
            }
            progress = fraction.coerceIn(0f, 1f)
            if (fraction >= 1f) break
        }
        work.join()
        viewModel.updateSettings { it.copy(glass = glass, iconShape = iconShape) }
        done = true
    }

    val stageIndex = ((progress * STAGES.size).toInt()).coerceIn(0, STAGES.size - 1)

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.9f))

        ProgressRing(progress = progress, done = done)

        Spacer(Modifier.height(30.dp))

        Text(
            text = if (done) "All set${if (name.isBlank()) "" else ", ${name.trim()}"}." else "Setting things up",
            color = palette.onGlass,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (done) "Your home screen is ready." else "This takes about twenty seconds.",
            color = palette.onGlassMuted,
            fontSize = 14.sp,
        )

        Spacer(Modifier.height(28.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .glass(ZenithShapes.panel, strength = 0.9f)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            STAGES.forEachIndexed { index, label ->
                // Each line arrives as the work reaches it, rather than sitting there greyed out.
                AnimatedVisibility(
                    visible = done || index <= stageIndex,
                    enter = fadeIn(tween(460)) +
                        slideInVertically(spring(dampingRatio = 0.58f, stiffness = 170f)) { it / 2 } +
                        expandVertically(spring(dampingRatio = 0.85f, stiffness = 190f)),
                ) {
                    StageRow(
                        label = label,
                        state = when {
                            done || index < stageIndex -> StageState.Done
                            else -> StageState.Active
                        },
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        AnimatedVisibility(
            visible = done,
            enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 3 },
        ) {
            GlassButton(
                text = "Enter Zenith",
                onClick = onDone,
                filled = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(34.dp))
    }

    LaunchedEffect(done) {
        if (done) {
            delay(6_000)
            onDone()
        }
    }
}

private enum class StageState { Pending, Active, Done }

@Composable
private fun StageRow(label: String, state: StageState) {
    val palette = LocalPalette.current
    val transition = rememberInfiniteTransitionSafe()
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
        Box(
            Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(
                    when (state) {
                        StageState.Done -> palette.accent
                        StageState.Active -> palette.accent.copy(alpha = pulse)
                        StageState.Pending -> palette.onGlass.copy(alpha = 0.12f)
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = state == StageState.Done,
                enter = scaleIn(spring(dampingRatio = 0.34f, stiffness = 340f)) + fadeIn(tween(240)),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF0A0B12), modifier = Modifier.size(13.dp))
            }
        }
        Spacer(Modifier.size(14.dp))
        Text(
            text = label,
            color = when (state) {
                StageState.Pending -> palette.onGlass.copy(alpha = 0.35f)
                StageState.Active -> palette.onGlass
                StageState.Done -> palette.onGlassMuted
            },
            fontSize = 14.sp,
            fontWeight = if (state == StageState.Active) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

@Composable
private fun rememberInfiniteTransitionSafe() =
    androidx.compose.animation.core.rememberInfiniteTransition(label = "stage")

@Composable
private fun ProgressRing(progress: Float, done: Boolean) {
    val palette = LocalPalette.current
    val glow = rememberInfiniteTransitionSafe()
    val breathe by glow.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "breathe",
    )

    Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = breathe; scaleY = breathe; alpha = 0.35f }
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(palette.accent.copy(alpha = 0.5f), Color.Transparent),
                    center = center,
                    radius = size.minDimension / 1.6f,
                ),
                radius = size.minDimension / 1.6f,
            )
        }
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = palette.onGlass.copy(alpha = 0.12f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(palette.accent, palette.accentAlt, palette.accent),
                    center = center,
                ),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            // Comet head on the leading edge of the arc.
            val angle = Math.toRadians((-90f + 360f * progress).toDouble())
            val radius = (size.minDimension - stroke) / 2f
            val head = Offset(
                center.x + (radius * cos(angle)).toFloat(),
                center.y + (radius * sin(angle)).toFloat(),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(palette.accentAlt.copy(alpha = 0.9f), Color.Transparent),
                    center = head,
                    radius = stroke * 2.2f,
                ),
                radius = stroke * 2.2f,
                center = head,
            )
        }

        Box(
            Modifier
                .size(120.dp)
                .graphicsLayer { val s = if (done) 1.04f else 1f; scaleX = s; scaleY = s }
                .glass(ZenithShapes.icon, strength = 1.2f),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = if (done) 100 else (progress * 100).toInt(),
                transitionSpec = {
                    (slideInVertically(tween(260)) { it / 2 } + fadeIn(tween(180)))
                        .togetherWith(slideOutVertically(tween(260)) { -it / 2 } + fadeOut(tween(140)))
                },
                label = "percent",
            ) { value ->
                Text(
                    text = "$value%",
                    color = palette.onGlass,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Light,
                )
            }
        }
    }
}
