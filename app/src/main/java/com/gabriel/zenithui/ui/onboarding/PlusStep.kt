package com.gabriel.zenithui.ui.onboarding

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.Plus
import com.gabriel.zenithui.data.PlusStatus
import com.gabriel.zenithui.ui.common.GlassButton
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass

private val ICONS = listOf(
    Icons.Rounded.Palette,
    Icons.Rounded.VisibilityOff,
    Icons.Rounded.Save,
)

/**
 * The one moment Zenith asks for anything: straight after your home screen is built, and once.
 *
 * The offer is deliberately quiet — a decline is a plain "Not now" of equal weight, the free week
 * needs no card and no account, and nothing on this screen is required to use the launcher.
 */
@Composable
fun PlusStep(
    name: String,
    status: PlusStatus,
    onStartTrial: () -> Unit,
    onSkip: () -> Unit,
) {
    val palette = LocalPalette.current

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(40.dp))
        Text(
            text = if (name.isBlank()) "One last thing" else "One last thing, ${name.trim()}",
            color = palette.onGlass,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.6).sp,
            modifier = Modifier.appearIn(),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Zenith Plus is three extras, free for ${Plus.TRIAL_DAYS} days. " +
                "The launcher you just set up is yours either way.",
            color = palette.onGlassMuted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.appearIn(delayMillis = 60),
        )
        Spacer(Modifier.height(20.dp))

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Plus.FEATURES.forEachIndexed { index, (title, detail) ->
                PlusCard(
                    icon = ICONS.getOrElse(index) { Icons.Rounded.Palette },
                    title = title,
                    body = detail,
                    delayMillis = 120 + staggerDelay(index) * 2,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        GlassButton(
            text = "Start my ${Plus.TRIAL_DAYS} free days",
            onClick = onStartTrial,
            filled = true,
            modifier = Modifier
                .fillMaxWidth()
                .appearIn(delayMillis = 340),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            // Play's own sheet says this once the trial subscription exists; until then the week
            // is counted here, and the wording has to be true in both cases.
            text = "${Plus.TRIAL_DAYS} days free. After that it is ${status.monthlyPrice} a month, " +
                "or ${status.lifetimePrice} once and never again.",
            color = palette.onGlassMuted,
            fontSize = 11.5.sp,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Not now",
            color = palette.onGlassMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSkip() }
                .padding(vertical = 10.dp),
        )
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun PlusCard(icon: ImageVector, title: String, body: String, delayMillis: Int) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .appearIn(delayMillis = delayMillis, riseFrom = 26f)
            .glass(ZenithShapes.card, strength = 0.85f)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .glass(ZenithShapes.tile, strength = 1.1f),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(body, color = palette.onGlassMuted, fontSize = 12.5.sp, lineHeight = 17.sp)
        }
    }
}
