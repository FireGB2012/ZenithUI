package com.gabriel.zenithui.ui.settings

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
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
import com.gabriel.zenithui.data.AuraTheme
import com.gabriel.zenithui.data.AuraThemes
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.popIn
import com.gabriel.zenithui.ui.common.popOut
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberHaptics
import com.gabriel.zenithui.ui.common.shake
import com.gabriel.zenithui.ui.common.unfoldIn
import com.gabriel.zenithui.ui.common.unfoldOut
import com.gabriel.zenithui.ui.onboarding.PrimaryButton
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.Palettes
import com.gabriel.zenithui.ui.theme.ZenithPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Aura, as a page of Settings: where points are earned and spent.
 *
 * The whole economy is on this one page on purpose — how much you have and what it is going
 * towards, the streak that keeps it coming, what it buys, and the code you can pass on. Nothing
 * here is hidden behind a second screen.
 */
@Composable
internal fun AuraOptions(settings: ZenithSettings, actions: SettingsActions) {
    val haptics = rememberHaptics()
    /** Goes up each time points arrive or are spent: the ring answers with a ripple. */
    var stirred by remember { mutableIntStateOf(0) }

    Spot("aura") {
        AuraRing(settings, stirred)
        Spacer(Modifier.height(12.dp))
        StreakLadder(settings.streak)
    }

    Spot("auraThemes") {
        Caption("Themes to unlock", Modifier.riseIn(2).padding(top = 6.dp, bottom = 10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AuraThemes.all.forEachIndexed { index, theme ->
                ThemeCard(
                    theme = theme,
                    settings = settings,
                    modifier = Modifier.riseIn(3 + index),
                    onUnlock = {
                        val bought = actions.unlockTheme(theme.id)
                        if (bought) {
                            haptics.confirm()
                            stirred++
                            actions.change { it.copy(paletteId = theme.id) }
                        }
                        bought
                    },
                    onApply = { actions.change { it.copy(paletteId = theme.id) } },
                )
            }
        }
    }

    Spot("auraCode") {
        YourCode(settings.referralCode, Modifier.riseIn(8))
    }

    Spot("auraRedeem") {
        Redeem(
            redeemed = settings.referredBy,
            modifier = Modifier.riseIn(9),
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

// ------------------------------------------------------------------------------- what you have

/**
 * What you have, counted up as the page arrives, inside a ring that shows how far it goes towards
 * the next theme. The ring draws itself round to that point and sends out a ripple; it does the
 * same whenever points arrive or are spent, and is still the rest of the time.
 */
@Composable
private fun AuraRing(settings: ZenithSettings, stirred: Int) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    val next = AuraThemes.next(settings.unlockedThemes)
    val towards = if (next == null) 1f else (settings.aura / next.price.toFloat()).coerceIn(0f, 1f)

    val counted = remember { Animatable(if (reduced) settings.aura.toFloat() else 0f) }
    val drawn = remember { Animatable(if (reduced) towards else 0f) }
    val ripple = remember { Animatable(1f) }
    LaunchedEffect(settings.aura, reduced) {
        if (reduced) counted.snapTo(settings.aura.toFloat()) else counted.animateTo(settings.aura.toFloat(), tween(900, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(towards, reduced) {
        if (reduced) drawn.snapTo(towards) else drawn.animateTo(towards, spring(dampingRatio = 0.7f, stiffness = 60f))
    }
    LaunchedEffect(stirred, reduced) {
        if (reduced) return@LaunchedEffect
        ripple.snapTo(0f)
        ripple.animateTo(1f, tween(1100, easing = LinearOutSlowInEasing))
    }

    val sweep = remember(palette.accent, palette.accentAlt) {
        Brush.sweepGradient(listOf(palette.accent, palette.accentAlt, palette.accent))
    }
    Column(Modifier.fillMaxWidth().riseIn(0), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(212.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(212.dp)) {
                val band = 13.dp.toPx()
                val radius = size.minDimension / 2f - 26.dp.toPx()
                // The light it gives off.
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(palette.accent.copy(alpha = 0.30f), palette.accentAlt.copy(alpha = 0.10f), Color.Transparent),
                        radius = size.minDimension / 2f,
                    ),
                    radius = size.minDimension / 2f,
                )
                // Two ripples, the second a little behind the first.
                val out = ripple.value
                if (out < 1f) {
                    for (lag in 0..1) {
                        val p = (out * 1.25f - 0.25f * lag).coerceIn(0f, 1f)
                        if (p > 0f && p < 1f) {
                            drawCircle(
                                color = palette.accent,
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
                // Turned so the colours start at the top, where the ring does.
                rotate(-90f) {
                    drawArc(
                        brush = sweep,
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
            text = when {
                next == null -> "Every theme is yours"
                settings.aura >= next.price -> "Enough for ${next.name}"
                else -> "${next.price - settings.aura} to go for ${next.name}"
            },
            color = palette.accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
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
            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
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
        // On the accent the flame is whichever of dark and white reads.
        lit -> if (palette.accent.luminance() > 0.3f) Color(0xFF0B0C10) else Color.White
        day.state == AuraDayState.Next -> palette.accent
        else -> palette.onGlass.copy(alpha = 0.3f)
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(if (day.state == AuraDayState.Today) 38.dp else 34.dp)
                .then(
                    when (day.state) {
                        AuraDayState.Done -> Modifier.clip(CircleShape).background(palette.accent.copy(alpha = 0.75f))
                        AuraDayState.Today -> Modifier
                            .border(2.dp, palette.onGlass.copy(alpha = 0.85f), CircleShape)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(palette.accent, palette.accentAlt)))
                        AuraDayState.Next -> Modifier.border(1.5.dp, palette.accent, CircleShape)
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
            color = if (day.state == AuraDayState.Next) palette.accent else palette.onGlassMuted,
            fontSize = 9.5.sp,
            maxLines = 1,
        )
    }
}

// ------------------------------------------------------------------------------- what it buys

/**
 * One theme, on a card painted in that theme's own colours, so it is seen before it is bought.
 * Not yet yours, it shows how far your points go towards it; within reach, tapping unlocks it and
 * puts it on, and a ring of light crosses the card; out of reach, the card shakes its head and
 * says how many more days it will take.
 */
@Composable
private fun ThemeCard(
    theme: AuraTheme,
    settings: ZenithSettings,
    modifier: Modifier,
    onUnlock: () -> Boolean,
    onApply: () -> Unit,
) {
    val look: ZenithPalette = remember(theme.id) { Palettes.byId(theme.id) }
    val reduced = LocalMotionLevel.current.reduced
    val owned = theme.id in settings.unlockedThemes
    val inUse = settings.paletteId == theme.id
    val within = settings.aura >= theme.price
    val ink = look.onGlass
    // The words on the accent pill: whichever of dark and white reads on this theme's accent.
    val onAccent = if (look.accent.luminance() > 0.3f) Color(0xFF0B0C10) else Color.White

    var refusals by remember { mutableIntStateOf(0) }
    var refused by remember { mutableStateOf(false) }
    val burst = remember { Animatable(1f) }
    var bought by remember { mutableIntStateOf(0) }
    LaunchedEffect(bought) {
        if (bought == 0 || reduced) return@LaunchedEffect
        burst.snapTo(0f)
        burst.animateTo(1f, tween(620, easing = LinearOutSlowInEasing))
    }
    // The refusal says its piece and goes.
    LaunchedEffect(refusals) {
        if (refusals == 0) return@LaunchedEffect
        refused = true
        delay(4_200)
        refused = false
    }
    val towards = remember { Animatable(0f) }
    val share = (settings.aura / theme.price.toFloat()).coerceIn(0f, 1f)
    LaunchedEffect(share, reduced) {
        if (reduced) towards.snapTo(share) else towards.animateTo(share, spring(dampingRatio = 0.85f, stiffness = 70f))
    }

    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .shake(refusals)
                .pressBounce(0.98f)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(look.backdrop))
                .drawBehind {
                    // The theme's two accents, as light falling in from either end of the card.
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(look.accent.copy(alpha = 0.50f), Color.Transparent),
                            center = Offset(size.width * 0.06f, size.height * 0.1f),
                            radius = size.height * 1.6f,
                        ),
                        radius = size.height * 1.6f,
                        center = Offset(size.width * 0.06f, size.height * 0.1f),
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(look.accentAlt.copy(alpha = 0.32f), Color.Transparent),
                            center = Offset(size.width, size.height),
                            radius = size.height * 1.3f,
                        ),
                        radius = size.height * 1.3f,
                        center = Offset(size.width, size.height),
                    )
                }
                .drawWithContent {
                    drawContent()
                    val out = burst.value
                    if (out < 1f) {
                        drawCircle(
                            color = Color.White,
                            radius = 30.dp.toPx() + size.width * out,
                            center = Offset(38.dp.toPx(), size.height / 2f),
                            alpha = 0.35f * (1f - out),
                        )
                    }
                }
                .border(1.dp, Color.White.copy(alpha = if (inUse) 0.5f else 0.10f), RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    when {
                        inUse -> Unit
                        owned -> onApply()
                        onUnlock() -> bought++
                        else -> refusals++
                    }
                }
                .padding(horizontal = 16.dp, vertical = 15.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(look.accent, look.accentAlt))),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = when {
                            inUse -> Icons.Rounded.Check
                            owned -> null
                            else -> Icons.Rounded.Lock
                        },
                        transitionSpec = { popIn(reduced, from = 0.4f, millis = 200).togetherWith(popOut(reduced, to = 0.4f, millis = 120)) },
                        label = "themeBadge",
                    ) { badge: ImageVector? ->
                        if (badge != null) {
                            Icon(badge, contentDescription = null, tint = onAccent.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                        } else {
                            Spacer(Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(theme.name, color = ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(theme.tagline, color = ink.copy(alpha = 0.66f), fontSize = 12.5.sp)
                    if (!owned) {
                        Spacer(Modifier.height(9.dp))
                        // How far what you have goes towards it.
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(ink.copy(alpha = 0.16f))
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .graphicsLayer {
                                        scaleX = towards.value.coerceIn(0f, 1f)
                                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                                    }
                                    .background(Brush.horizontalGradient(listOf(look.accent, look.accentAlt)))
                            )
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = if (within) "You have enough" else "${settings.aura.coerceAtMost(theme.price)} of ${theme.price}",
                            color = ink.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                AnimatedContent(
                    targetState = when {
                        inUse -> "In use"
                        owned -> "Apply"
                        within -> "Unlock"
                        else -> "${theme.price}"
                    },
                    transitionSpec = { fadeIn(tween(220, delayMillis = 60)).togetherWith(fadeOut(tween(120))) },
                    label = "themeState",
                ) { state ->
                    val solid = state == "Apply" || state == "Unlock"
                    Row(
                        Modifier
                            .clip(ZenithShapes.pill)
                            .background(if (solid) look.accent else ink.copy(alpha = 0.10f))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!solid && state != "In use") {
                            Icon(
                                Icons.Rounded.LocalFireDepartment,
                                contentDescription = null,
                                tint = ink.copy(alpha = 0.75f),
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            state,
                            color = if (solid) onAccent else ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = refused && !owned,
            enter = unfoldIn(reduced),
            exit = unfoldOut(reduced),
        ) {
            val days = Aura.daysToAfford(settings.aura, theme.price, settings.streak)
            Note(
                "${theme.name} costs ${theme.price} and you have ${settings.aura}: about $days more ${if (days == 1) "day" else "days"} of turning up.",
                Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
            )
        }
    }
}

// ------------------------------------------------------------------------------- the codes

@Composable
private fun YourCode(code: String, modifier: Modifier) {
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
                color = palette.accent,
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
                    val words = "I use Zenith UI as my home screen. Put my code $code in under Settings > Aura and you get ${Aura.REFERRAL_REWARD} aura to start with."
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
            // Said plainly, because the alternative is people wondering why nothing arrived.
            Note(
                "Give this to someone setting up Zenith and they get ${Aura.REFERRAL_REWARD} aura. Zenith has no account and " +
                    "no server, so it cannot tell when your code is used — you are not paid for handing it out.",
            )
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
private fun Redeem(redeemed: String, modifier: Modifier, onRedeem: (String) -> String?) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    var code by remember { mutableStateOf("") }
    var refusal by remember { mutableStateOf<String?>(null) }
    var refusals by remember { mutableIntStateOf(0) }

    Column(modifier) {
        Group(padding = 16.dp) {
            if (redeemed.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = palette.accent, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Code $redeemed redeemed: ${Aura.REFERRAL_REWARD} aura added.",
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
                    text = "Redeem for ${Aura.REFERRAL_REWARD} aura",
                    onClick = {
                        refusal = onRedeem(code)
                        if (refusal == null) code = "" else refusals++
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
