package com.gabriel.zenithui.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.CheckIn
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import kotlinx.coroutines.delay

/**
 * The daily streak, as a strip that leans in from under the status bar and leaves on its own.
 *
 * Deliberately not a dialog. Nothing is dismissed, nothing is tapped, nothing waits for you: you
 * unlock your phone, a line slides down to say where you are, and it goes again. A home screen
 * that demands a tap before you can use it is a home screen people uninstall.
 */
@Composable
fun StreakStrip(state: CheckIn?, onShown: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    var visible by remember(state) { mutableStateOf(state != null) }

    LaunchedEffect(state) {
        if (state == null) return@LaunchedEffect
        visible = true
        delay(3600)
        visible = false
        // Let it finish sliding out before the state is cleared underneath it.
        delay(400)
        onShown()
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(420)) { -it } + fadeIn(tween(300)),
        exit = slideOutVertically(tween(360)) { -it } + fadeOut(tween(260)),
        modifier = modifier,
    ) {
        val broken = state is CheckIn.Broken
        Row(
            Modifier
                .statusBarsPadding()
                .padding(top = 8.dp)
                .glass(ZenithShapes.pill, strength = 1.2f, solid = 0.35f)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                tint = if (broken) palette.onGlassMuted else palette.accent,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = when (state) {
                    is CheckIn.Continued -> "Streak ${state.streak} · +${state.awarded} aura"
                    is CheckIn.Broken -> "Streak lost — back to day one tomorrow"
                    else -> ""
                },
                color = palette.onGlass,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
