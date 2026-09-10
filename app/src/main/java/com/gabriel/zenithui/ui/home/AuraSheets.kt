package com.gabriel.zenithui.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.Aura
import com.gabriel.zenithui.data.AuraThemes
import com.gabriel.zenithui.data.CheckIn
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.GlassButton
import com.gabriel.zenithui.ui.common.SectionLabel
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.Palettes
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

/**
 * Where points are earned and spent.
 *
 * The whole economy is on this one sheet on purpose: how much you have, how you got it, what it
 * buys, and the code you can pass on. Nothing here is hidden behind a second screen.
 */
@Composable
fun AuraSheet(
    visible: Boolean,
    settings: ZenithSettings,
    onUnlock: (String) -> Boolean,
    onApply: (String) -> Unit,
    onRedeem: (String) -> String?,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    var message by remember(visible) { mutableStateOf<String?>(null) }
    var code by remember(visible) { mutableStateOf("") }

    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Aura") {
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // What you have, and what is keeping it coming.
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(ZenithShapes.card)
                    .background(palette.accent.copy(alpha = 0.14f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${settings.aura}",
                        color = palette.onGlass,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text("aura", color = palette.onGlassMuted, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (settings.streak > 0) "${settings.streak} day streak" else "No streak yet",
                            color = palette.onGlass,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Text(
                        text = "Tomorrow: +${Aura.dailyReward(settings.streak + 1)}",
                        color = palette.onGlassMuted,
                        fontSize = 11.sp,
                    )
                }
            }

            Text(
                text = "Open Zenith once a day to keep the streak. Each day in a row is worth more, " +
                    "up to ${Aura.MAX_DAILY}. Miss a day and it starts again — nothing else is lost.",
                color = palette.onGlassMuted,
                fontSize = 12.sp,
            )

            SectionLabel("Themes")
            AuraThemes.all.forEach { theme ->
                val owned = theme.id in settings.unlockedThemes
                val inUse = settings.paletteId == theme.id
                val affordable = settings.aura >= theme.price
                val look = Palettes.byId(theme.id)

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(ZenithShapes.card)
                        .background(palette.onGlass.copy(alpha = if (inUse) 0.10f else 0.05f))
                        .clickable {
                            message = when {
                                inUse -> null
                                owned -> {
                                    onApply(theme.id)
                                    null
                                }

                                onUnlock(theme.id) -> {
                                    onApply(theme.id)
                                    null
                                }

                                else -> "${theme.name} costs ${theme.price}. You have ${settings.aura}."
                            }
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Brush.linearGradient(listOf(look.accent, look.accentAlt))),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!owned) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = Color.Black.copy(alpha = 0.55f),
                                modifier = Modifier.size(15.dp),
                            )
                        } else if (inUse) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color.Black.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            theme.name,
                            color = palette.onGlass,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(theme.tagline, color = palette.onGlassMuted, fontSize = 12.sp)
                    }
                    Text(
                        text = when {
                            inUse -> "In use"
                            owned -> "Apply"
                            else -> "${theme.price}"
                        },
                        color = when {
                            owned -> palette.onGlassMuted
                            affordable -> palette.accent
                            else -> palette.onGlassMuted
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            SectionLabel("Your code")
            Text(
                text = settings.referralCode,
                color = palette.accent,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ZenithShapes.card)
                    .background(palette.onGlass.copy(alpha = 0.05f))
                    .padding(vertical = 14.dp),
            )
            Text(
                // Said plainly, because the alternative is people wondering why nothing arrived.
                text = "Give this to someone setting up Zenith and they get " +
                    "${Aura.REFERRAL_REWARD} aura. Zenith has no account and no server, so it " +
                    "cannot tell when your code is used — you are not paid for handing it out.",
                color = palette.onGlassMuted,
                fontSize = 11.5.sp,
            )

            if (settings.referredBy.isEmpty()) {
                SectionLabel("Someone gave you one?")
                AuraCodeField(value = code, onChange = { code = Aura.cleanCode(it) })
                GlassButton(
                    text = "Redeem for ${Aura.REFERRAL_REWARD} aura",
                    onClick = {
                        message = onRedeem(code)
                        if (message == null) code = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = palette.onGlassMuted,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Code ${settings.referredBy} redeemed.",
                        color = palette.onGlassMuted,
                        fontSize = 12.sp,
                    )
                }
            }

            message?.let {
                Text(it, color = palette.onGlassMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun AuraCodeField(value: String, onChange: (String) -> Unit) {
    val palette = LocalPalette.current
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = palette.onGlass,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
        ),
        cursorBrush = Brush.linearGradient(listOf(palette.accent, palette.accent)),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .glass(ZenithShapes.pill, strength = 0.8f)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (value.isEmpty()) {
                    Text("XXXXXX", color = palette.onGlassMuted, fontSize = 20.sp)
                }
                inner()
            }
        },
    )
}
