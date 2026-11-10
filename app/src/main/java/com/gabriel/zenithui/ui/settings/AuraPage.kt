package com.gabriel.zenithui.ui.settings

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.Aura
import com.gabriel.zenithui.data.AuraDay
import com.gabriel.zenithui.data.AuraDayState
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.popIn
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberHaptics
import com.gabriel.zenithui.ui.common.shake
import com.gabriel.zenithui.ui.common.unfoldIn
import com.gabriel.zenithui.ui.common.unfoldOut
import com.gabriel.zenithui.ui.onboarding.PrimaryButton
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import com.gabriel.zenithui.ui.theme.inkOnTint
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import androidx.compose.runtime.rememberCoroutineScope
import com.gabriel.zenithui.data.server.Account
import kotlinx.coroutines.launch

/**
 * Aura, as a page of Settings: where points are earned.
 *
 * It is all on this one page on purpose — how much you have, the streak that keeps it coming, and
 * the code you can pass on. Nothing here is hidden behind a second screen. Points have nothing to
 * buy for now: the themes they bought went with Zenith's colour themes, and Aura is to be rebuilt.
 * What you have is kept, and a set already bought is still offered for Zenith's wallpapers.
 */
@Composable
internal fun AuraOptions(settings: ZenithSettings, actions: SettingsActions, onAccount: () -> Unit) {
    val haptics = rememberHaptics()
    /** Goes up each time points arrive: the ring answers with a ripple. */
    var stirred by remember { mutableIntStateOf(0) }

    Spot("aura") {
        AuraRing(settings, stirred)
        Spacer(Modifier.height(12.dp))
        StreakLadder(settings.streak)
    }

    // With a server, codes are real: it knows whose each one is and counts who has used it, and
    // that takes an account. Until somebody is signed in, one card stands where the two would
    // be, and a search for either lands on it.
    val account = actions.account
    if (account != null && account.signed == null) {
        Spot("auraCode") {
            Spot("auraRedeem") { CodesNeedAccount(Modifier.riseIn(2), onAccount) }
        }
        // A code used before there were accounts is still a code used.
        if (settings.referredBy.isNotEmpty()) Redeem(settings.referredBy, Modifier.riseIn(3), onRedeem = { null })
    } else {
        Spot("auraCode") {
            YourCode(settings.referralCode, account?.signed?.account, Modifier.riseIn(2))
        }

        Spot("auraRedeem") {
            Redeem(
                redeemed = settings.referredBy,
                modifier = Modifier.riseIn(3),
                onRedeem = { code ->
                    val refused = actions.redeem(code)
                    if (refused == null) {
                        haptics.confirm()
                        stirred++
                    }
                    refused
                },
            )
        }
    }
}

// ------------------------------------------------------------------------------- what you have

/**
 * What you have, counted up as the page arrives, inside a ring in the words' ink. With nothing to
 * save towards, the ring is always whole, but it still draws itself round as the page arrives, and
 * sends out a ripple then and again whenever points arrive; the rest of the time it is still.
 */
@Composable
private fun AuraRing(settings: ZenithSettings, stirred: Int) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced

    val counted = remember { Animatable(if (reduced) settings.aura.toFloat() else 0f) }
    val drawn = remember { Animatable(if (reduced) 1f else 0f) }
    val ripple = remember { Animatable(1f) }
    LaunchedEffect(settings.aura, reduced) {
        if (reduced) counted.snapTo(settings.aura.toFloat()) else counted.animateTo(settings.aura.toFloat(), tween(900, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(reduced) {
        if (reduced) drawn.snapTo(1f) else drawn.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 60f))
    }
    LaunchedEffect(stirred, reduced) {
        if (reduced) return@LaunchedEffect
        ripple.snapTo(0f)
        ripple.animateTo(1f, tween(1100, easing = LinearOutSlowInEasing))
    }

    Column(Modifier.fillMaxWidth().riseIn(0), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(212.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(212.dp)) {
                val band = 13.dp.toPx()
                val radius = size.minDimension / 2f - 26.dp.toPx()
                // Two ripples, the second a little behind the first.
                val out = ripple.value
                if (out < 1f) {
                    for (lag in 0..1) {
                        val p = (out * 1.25f - 0.25f * lag).coerceIn(0f, 1f)
                        if (p > 0f && p < 1f) {
                            drawCircle(
                                color = palette.onGlass,
                                radius = radius + band / 2f + (size.minDimension / 2f - radius - band / 2f) * p,
                                alpha = 0.45f * (1f - p),
                                style = Stroke(width = 2.dp.toPx()),
                            )
                        }
                    }
                }
                val topLeft = Offset(center.x - radius, center.y - radius)
                val box = Size(radius * 2f, radius * 2f)
                drawArc(palette.onGlass.copy(alpha = 0.10f), 0f, 360f, useCenter = false, topLeft = topLeft, size = box, style = Stroke(band))
                // Turned so it starts at the top as it draws itself round.
                rotate(-90f) {
                    drawArc(
                        color = palette.onGlass,
                        startAngle = 0f,
                        sweepAngle = 360f * drawn.value.coerceIn(0f, 1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = box,
                        style = Stroke(band, cap = StrokeCap.Round),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Counted(settings.aura) { counted.value }
                Text("aura", color = palette.onGlassMuted, fontSize = 13.sp)
            }
        }
        Text(
            text = "Earned for turning up, day after day",
            color = palette.onGlassMuted,
            fontSize = 13.sp,
        )
    }
}

/**
 * The number in the ring. On its own so that, while it counts, it is the only thing composed
 * again on each frame. A screen reader is told the [balance] itself, not whichever step of the
 * count happens to be showing.
 */
@Composable
private fun Counted(balance: Int, value: () -> Float) {
    Text(
        text = value().roundToInt().toString(),
        color = LocalPalette.current.onGlass,
        fontSize = 46.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1).sp,
        modifier = Modifier.clearAndSetSemantics { contentDescription = balance.toString() },
    )
}

/**
 * The streak as a ladder of seven days: the ones behind you lit, today ringed, tomorrow waiting,
 * each with what it pays. The flames come up one after another as the page arrives.
 */
@Composable
private fun StreakLadder(streak: Int) {
    val palette = LocalPalette.current
    val arrival = LocalSettingsRise.current
    Group(padding = 14.dp, rise = 1) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = palette.onGlass, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (streak > 0) "$streak day streak" else "No streak yet",
                color = palette.onGlass,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text("Tomorrow +${Aura.dailyReward(streak + 1)}", color = palette.onGlassMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth()) {
            Aura.ladder(streak).forEachIndexed { index, day ->
                LadderDay(
                    day = day,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (arrival != null) {
                                Modifier.appearIn(delayMillis = 220 + 60 * index, fromScale = 0.3f, riseFrom = 0f, replay = arrival)
                            } else {
                                Modifier
                            }
                        ),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Note(
            "Open Zenith once a day to keep the streak. Each day in a row is worth more, up to ${Aura.MAX_DAILY}. " +
                "Miss a day and it starts again — nothing else is lost.",
        )
    }
}

@Composable
private fun LadderDay(day: AuraDay, modifier: Modifier) {
    val palette = LocalPalette.current
    val lit = day.state == AuraDayState.Done || day.state == AuraDayState.Today
    val flame = when {
        // A day climbed is a disc in the words' ink, and its flame is whichever of dark and white
        // reads on that: dark on the white of Dark, white on the near-black of Light.
        lit -> inkOnTint(palette.onGlass)
        day.state == AuraDayState.Next -> palette.onGlass
        else -> palette.onGlass.copy(alpha = 0.3f)
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(if (day.state == AuraDayState.Today) 38.dp else 34.dp)
                .then(
                    when (day.state) {
                        AuraDayState.Done -> Modifier.clip(CircleShape).background(palette.onGlass.copy(alpha = 0.75f))
                        AuraDayState.Today -> Modifier
                            .border(2.dp, palette.onGlass.copy(alpha = 0.85f), CircleShape)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(palette.onGlass)
                        AuraDayState.Next -> Modifier.border(1.5.dp, palette.onGlass, CircleShape)
                        AuraDayState.Later -> Modifier.clip(CircleShape).background(palette.onGlass.copy(alpha = 0.07f))
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = flame, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "+${day.reward}",
            color = if (day.state == AuraDayState.Later) palette.onGlassMuted else palette.onGlass,
            fontSize = 12.sp,
            fontWeight = if (day.state == AuraDayState.Today) FontWeight.Bold else FontWeight.Medium,
        )
        Text(
            when (day.state) {
                AuraDayState.Today -> "Today"
                AuraDayState.Next -> "Next"
                else -> "Day ${day.day}"
            },
            color = if (day.state == AuraDayState.Next) palette.onGlass else palette.onGlassMuted,
            fontSize = 9.5.sp,
            maxLines = 1,
        )
    }
}

// ------------------------------------------------------------------------------- the codes

/**
 * In a build with a server, a code is only real once the server knows whose it is, and using one
 * is counted against an account. Until somebody is signed in, this says so and leads there.
 */
@Composable
private fun CodesNeedAccount(modifier: Modifier, onAccount: () -> Unit) {
    val palette = LocalPalette.current
    Column(modifier) {
        Group(padding = 16.dp) {
            Caption("Aura codes")
            Spacer(Modifier.height(10.dp))
            Text(
                "With a Zenith account you get a code to give to friends. Each one who uses it gets " +
                    "${Aura.REFERRAL_REWARD} aura, and so do you. An account is also how you use a code a friend gave you.",
                color = palette.onGlass,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(14.dp))
            PrimaryButton(text = "Sign in or create an account", onClick = onAccount)
        }
    }
}

/** [account] is the one signed in to, in a build with a server; null in a build that has none. */
@Composable
private fun YourCode(code: String, account: Account?, modifier: Modifier) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptics = rememberHaptics()
    var copies by remember { mutableIntStateOf(0) }
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copies) {
        if (copies == 0) return@LaunchedEffect
        copied = true
        delay(1_800)
        copied = false
    }
    fun copy() {
        clipboard.setText(AnnotatedString(code))
        haptics.tick()
        copies++
    }

    Column(modifier) {
        Group(padding = 16.dp) {
            Caption("Your code")
            Spacer(Modifier.height(10.dp))
            Text(
                text = code.ifEmpty { "······" },
                color = palette.onGlass,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 7.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .pressBounce(0.97f)
                    .clip(ZenithShapes.card)
                    .background(palette.onGlass.copy(alpha = 0.06f))
                    .clickable { copy() }
                    .padding(vertical = 16.dp),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CodeButton(
                    icon = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                    label = if (copied) "Copied" else "Copy",
                    modifier = Modifier.weight(1f),
                ) { copy() }
                CodeButton(Icons.Rounded.IosShare, "Share", Modifier.weight(1f)) {
                    val words = if (account == null) {
                        "I use Zenith UI as my home screen. Put my code $code in under Settings > Aura and you get ${Aura.REFERRAL_REWARD} aura to start with."
                    } else {
                        "I use Zenith UI as my home screen. Make an account under Settings > Account, then put my code $code in " +
                            "under Settings > Aura, and we both get ${Aura.REFERRAL_REWARD} aura."
                    }
                    runCatching {
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, words),
                                "Share your code",
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (account == null) {
                // Said plainly, because the alternative is people wondering why nothing arrived.
                Note(
                    "Give this to someone setting up Zenith and they get ${Aura.REFERRAL_REWARD} aura. Zenith has no account and " +
                        "no server, so it cannot tell when your code is used — you are not paid for handing it out.",
                )
            } else {
                val used = when {
                    account.referralCap > 0 && account.referrals >= account.referralCap ->
                        "All ${account.referralCap} have: it still gives a friend aura, but no more comes to you."
                    account.referrals == 0 -> "Nobody has used it yet."
                    account.referrals == 1 -> "One friend has used it."
                    else -> "${account.referrals} friends have used it."
                }
                Note(
                    "Give this to someone with a Zenith account. When they put it in they get ${Aura.REFERRAL_REWARD} aura, and so do you" +
                        (if (account.referralCap > 0) ", for up to ${account.referralCap} friends. " else ". ") + used,
                )
            }
        }
    }
}

@Composable
private fun CodeButton(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    Row(
        modifier
            .height(46.dp)
            .pressBounce(0.95f)
            .clip(ZenithShapes.pill)
            .background(palette.onGlass.copy(alpha = 0.09f))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = icon to label,
            transitionSpec = { popIn(reduced, from = 0.7f).togetherWith(fadeOut(tween(100))) },
            label = "codeButton",
        ) { (image, words) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(image, contentDescription = null, tint = palette.onGlass, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(words, color = palette.onGlass, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/**
 * Somebody else's code, once. A code that will not do makes the box shake its head and says why;
 * one that will is taken, and the ring at the top of the page counts the points in.
 */
@Composable
private fun Redeem(redeemed: String, modifier: Modifier, onRedeem: suspend (String) -> String?) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    val scope = rememberCoroutineScope()
    /** True while the server is being asked about a code. */
    var busy by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var refusal by remember { mutableStateOf<String?>(null) }
    var refusals by remember { mutableIntStateOf(0) }

    Column(modifier) {
        Group(padding = 16.dp) {
            if (redeemed.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = palette.onGlass, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (redeemed == Aura.USED_ON_ACCOUNT) {
                            "Your account has used a friend's code: ${Aura.REFERRAL_REWARD} aura added."
                        } else {
                            "Code $redeemed redeemed: ${Aura.REFERRAL_REWARD} aura added."
                        },
                        color = palette.onGlass,
                        fontSize = 14.sp,
                    )
                }
            } else {
                Caption("Someone gave you one?")
                Spacer(Modifier.height(10.dp))
                AuraCodeField(
                    value = code,
                    onChange = {
                        code = Aura.cleanCode(it)
                        refusal = null
                    },
                    modifier = Modifier.shake(refusals),
                )
                Spacer(Modifier.height(10.dp))
                PrimaryButton(
                    text = if (busy) "Checking\u2026" else "Redeem for ${Aura.REFERRAL_REWARD} aura",
                    onClick = {
                        if (!busy) {
                            busy = true
                            refusal = null
                            scope.launch {
                                val refused = try {
                                    onRedeem(code)
                                } finally {
                                    busy = false
                                }
                                refusal = refused
                                if (refused == null) code = "" else refusals++
                            }
                        }
                    },
                )
                AnimatedVisibility(
                    visible = refusal != null,
                    enter = unfoldIn(reduced),
                    exit = unfoldOut(reduced),
                ) {
                    Note(refusal.orEmpty(), Modifier.padding(top = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun AuraCodeField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(
            fontFamily = LocalZenithFont.current,
            color = palette.onGlass,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            letterSpacing = 4.sp,
        ),
        cursorBrush = Brush.linearGradient(listOf(palette.accent, palette.accent)),
        modifier = modifier,
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .glass(ZenithShapes.pill, strength = 0.8f)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (value.isEmpty()) {
                    Text("XXXXXX", color = palette.onGlassMuted, fontSize = 20.sp, letterSpacing = 4.sp)
                }
                inner()
            }
        },
    )
}
