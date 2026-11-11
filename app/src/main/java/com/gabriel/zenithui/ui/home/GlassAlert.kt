package com.gabriel.zenithui.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.theme.glass

/** One answer to an alert. The red one takes something away. */
data class AlertButton(val label: String, val danger: Boolean = false, val onClick: () -> Unit)

/** A question asked over the home screen: what, a line saying what it means, and the answers, top to bottom. */
data class HomeAlert(val title: String, val message: String, val buttons: List<AlertButton>)

/**
 * A question in a card of glass in the middle of the home screen: a bold line, a quieter one, and
 * the answers as capsules one under another, the way a phone asks before it removes something.
 * It stands on the wallpaper, so it is the same clear glass in Light and in Dark, with white words.
 * Drawn inside the home screen's own layout. Every answer puts it away; so does Back, which is the
 * same as its last answer never being pressed.
 */
@Composable
internal fun GlassAlert(alert: HomeAlert?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val reduced = LocalMotionLevel.current.reduced
    // What it last asked stays while it goes, so it is not seen to empty out.
    val last = remember { arrayOfNulls<HomeAlert>(1) }
    if (alert != null) last[0] = alert
    val shown = remember { Animatable(0f) }
    val grown = remember { Animatable(1.12f) }
    var up by remember { mutableStateOf(false) }
    LaunchedEffect(alert != null) {
        if (alert != null) {
            up = true
            if (reduced) {
                grown.snapTo(1f)
            } else {
                // It settles from a little large, as the fade below brings it in.
                grown.snapTo(1.12f)
                grown.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 380f))
            }
        }
    }
    LaunchedEffect(alert != null) {
        if (alert != null) {
            shown.animateTo(1f, tween(if (reduced) Motion.REDUCED_MS else 200))
        } else {
            shown.animateTo(0f, tween(if (reduced) Motion.REDUCED_MS else 180))
            up = false
        }
    }
    val asked = last[0]
    if (!up || asked == null) return

    BackHandler(enabled = alert != null) { onDismiss() }
    Box(
        modifier
            .fillMaxSize()
            // The screen behind dims a little, and takes no touches while it is asked.
            .drawBehind { drawRect(Color.Black.copy(alpha = 0.24f * shown.value)) }
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false).consume() } }
            .systemBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 300.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = shown.value
                    scaleX = grown.value
                    scaleY = grown.value
                }
                .bodyWithoutWallpaper(34.dp)
                .glass(RoundedCornerShape(34.dp), strength = 1.2f, solid = 0.68f, tracking = { grown.value })
                .semantics { paneTitle = asked.title }
                .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 14.dp),
        ) {
            Text(
                asked.title,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                asked.message,
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 14.5.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            Spacer(Modifier.height(6.dp))
            asked.buttons.forEach { button ->
                val source = remember(button.label) { MutableInteractionSource() }
                val pressed by source.collectIsPressedAsState()
                Box(
                    Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(Color.White.copy(alpha = if (pressed) 0.2f else 0.11f))
                        .clickable(interactionSource = source, indication = null, role = Role.Button, enabled = alert != null) {
                            onDismiss()
                            button.onClick()
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        button.label,
                        color = if (button.danger) Color(0xFFFF453A) else Color.White,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            shadow = if (button.danger) null else Shadow(Color.Black.copy(alpha = 0.3f), Offset(0f, 1f), 2f),
                        ),
                    )
                }
            }
        }
    }
}
