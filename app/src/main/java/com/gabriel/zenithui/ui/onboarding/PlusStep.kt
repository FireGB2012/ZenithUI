package com.gabriel.zenithui.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.Plus
import com.gabriel.zenithui.data.PlusStatus
import com.gabriel.zenithui.ui.theme.LocalPalette

/**
 * The only time Zenith asks for anything, and it asks once. Three plain lines on what Plus adds,
 * the price in full before you tap, and a "Not now" that is as easy to hit as the yes.
 */
@Composable
fun PlusStep(status: PlusStatus) {
    val palette = LocalPalette.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
    ) {
        Spacer(Modifier.height(36.dp))
        ScreenTitle(
            eyebrow = "Zenith Plus",
            headline = "A few extras,\nfree for ${Plus.TRIAL_DAYS} days.",
            lede = "Your home screen works exactly the same without it.",
            play = true,
        )
        Spacer(Modifier.height(24.dp))

        Plus.FEATURES.forEachIndexed { index, (title, body) ->
            Column(Modifier.rise(3 + index)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(palette.onGlass.copy(alpha = 0.1f))
                )
                Spacer(Modifier.height(16.dp))
                Text(title, color = palette.onGlass, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(body, color = palette.onGlassMuted, fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(Modifier.height(16.dp))
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            // Play's own sheet says this once the trial subscription exists; until then the week is
            // counted here, and the wording has to be true in both cases.
            text = if (status.playTrialOffer) {
                "Free for ${Plus.TRIAL_DAYS} days through Google Play, then ${status.monthlyPrice} a month. " +
                    "Cancel any time in Play. Or pay ${status.lifetimePrice} once."
            } else {
                "Free for ${Plus.TRIAL_DAYS} days, no card needed. After that it is " +
                    "${status.monthlyPrice} a month, or ${status.lifetimePrice} once."
            },
            color = palette.onGlassMuted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.rise(6),
        )
        Spacer(Modifier.height(12.dp))
    }
}
