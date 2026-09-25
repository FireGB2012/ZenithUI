package com.gabriel.zenithui.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.Plus
import com.gabriel.zenithui.data.PlusStatus
import com.gabriel.zenithui.ui.home.FEATURE_ICONS
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass

/**
 * The only time Zenith asks for anything, and it asks once, in a sheet over your finished phone.
 * What Plus adds, the price in full before you tap, and a "Not now" as easy to hit as the yes.
 */
@Composable
fun PlusSheet(status: PlusStatus, onTry: () -> Unit, onNotNow: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    // Only the story scrolls. The price and both answers stay pinned at the bottom, so on a small
    // screen or with big text "Not now" is never hidden below the fold.
    Column(
        modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(palette.onGlass.copy(alpha = 0.25f))
            )
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier
                    .size(56.dp)
                    .rise(0)
                    .glass(CircleShape, strength = 1.1f),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = palette.accent, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(14.dp))
            Eyebrow("Zenith Plus", Modifier.rise(1))
            Spacer(Modifier.height(6.dp))
            Text(
                text = "A few extras,\nfree for ${Plus.TRIAL_DAYS} days.",
                color = palette.onGlass,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.6).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.rise(2),
            )
            Spacer(Modifier.height(10.dp))
            Lede("Your home screen works exactly the same without it.", Modifier.rise(3))
            Spacer(Modifier.height(20.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .rise(4)
                    .glass(ZenithShapes.card, strength = 0.9f)
                    .padding(16.dp),
            ) {
                Plus.FEATURES.forEachIndexed { index, (title, body) ->
                    if (index > 0) {
                        Box(
                            Modifier
                                .padding(vertical = 12.dp)
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(palette.onGlass.copy(alpha = 0.08f))
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .glass(ZenithShapes.tile, strength = 1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                FEATURE_ICONS.getOrElse(index) { Icons.Rounded.AutoAwesome },
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(title, color = palette.onGlass, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(2.dp))
                            Text(body, color = palette.onGlassMuted, fontSize = 14.sp, lineHeight = 20.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
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
            textAlign = TextAlign.Center,
            modifier = Modifier.rise(5),
        )
        Spacer(Modifier.height(16.dp))
        PrimaryButton(text = "Try it free", onClick = onTry)
        QuietButton(text = "Not now", onClick = onNotNow)
        Spacer(Modifier.height(8.dp))
    }
}
