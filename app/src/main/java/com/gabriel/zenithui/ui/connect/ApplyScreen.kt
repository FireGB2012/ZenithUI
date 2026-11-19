package com.gabriel.zenithui.ui.connect

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.rememberHaptics
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.inkOnTint
import androidx.compose.runtime.LaunchedEffect

/**
 * Over everything while a setup goes on: the dimmed page, and a card of glass with your home
 * screen wearing it, ticking off what goes on as it does (for real, under it). Then it is on: back
 * to the home screen to see it, Undo, or Done back to the setup's page.
 */
@Composable
internal fun ApplyScreen(state: ConnectState, bridge: ConnectBridge, onOpenZenith: () -> Unit) {
    val reduced = LocalMotionLevel.current.reduced
    val applying = state.applying
    // Kept while it fades away, so it goes with what it said.
    val kept = remember { arrayOfNulls<Applying>(1) }
    if (applying != null) kept[0] = applying
    AnimatedVisibility(
        visible = applying != null,
        enter = fadeIn(tween(if (reduced) 120 else 220)),
        exit = fadeOut(tween(if (reduced) 120 else 260)),
    ) {
        val shown = applying ?: kept[0] ?: return@AnimatedVisibility
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                // Nothing under it answers a touch while it is up.
                .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false).consume() } },
            contentAlignment = Alignment.Center,
        ) {
            ApplyCard(
                shown,
                state,
                bridge,
                onOpenZenith,
                Modifier.animateEnterExit(
                    enter = if (reduced) fadeIn(tween(120)) else scaleIn(spring(dampingRatio = 0.72f, stiffness = 420f), initialScale = 0.86f),
                    exit = if (reduced) fadeOut(tween(120)) else scaleOut(tween(220), targetScale = 0.94f),
                ),
            )
        }
    }
}

@Composable
private fun ApplyCard(applying: Applying, state: ConnectState, bridge: ConnectBridge, onOpenZenith: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    val haptics = rememberHaptics()
    val done = state.applied && state.applying === applying
    LaunchedEffect(done) { if (done) haptics.confirm() }
    val worn = rememberWorn(applying.setup, applying.on, bridge)
    val aspect = phoneAspect(bridge)
    val steps = applying.steps
    val progress by animateFloatAsState(
        if (done) 1f else (state.ticked.toFloat() / steps.size.coerceAtLeast(1)).coerceIn(0f, 1f),
        if (reduced) tween(120) else tween(ConnectApplyTiming, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "applyProgress",
    )
    Column(
        modifier
            .padding(horizontal = 24.dp)
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .pane(CARD, strength = 1.15f)
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.width(112.dp).aspectRatio(aspect)) {
            SetupPhone(worn, bridge, Modifier.fillMaxSize(), artWidth = 260)
        }
        Spacer(Modifier.height(18.dp))
        AnimatedContent(
            targetState = done,
            transitionSpec = { fadeIn(tween(200)).togetherWith(fadeOut(tween(140))) },
            label = "applyTitle",
        ) { on ->
            Text(
                if (on) "${quoted(applying.setup.name)} is on" else "Applying ${quoted(applying.setup.name)}…",
                color = palette.onGlass,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.3).sp,
            )
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            steps.forEachIndexed { i, step ->
                StepLine(step, ticked = i < state.ticked || done, working = i == state.ticked && !done)
            }
        }
        Spacer(Modifier.height(16.dp))
        // How far it has got.
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(palette.onGlass.copy(alpha = 0.12f)),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(CircleShape)
                    .background(palette.accent),
            )
        }
        if (applying.photoGone) {
            Spacer(Modifier.height(12.dp))
            Note("The design took your photo's place.", modifier = Modifier.fillMaxWidth())
        }
        if (applying.keptWallpaper) {
            Spacer(Modifier.height(12.dp))
            Note("Its wallpaper is a Zenith Plus design, so yours stayed.", modifier = Modifier.fillMaxWidth())
        }
        if (done) {
            Spacer(Modifier.height(20.dp))
            ConnectButton(
                "See it on your home screen",
                onClick = onOpenZenith,
                icon = Icons.Rounded.Home,
                tinted = true,
                height = 52.dp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ConnectButton("Undo", onClick = { state.undo(bridge) }, icon = Icons.AutoMirrored.Rounded.Undo, modifier = Modifier.weight(1f))
                ConnectButton("Done", onClick = { state.leaveApply() }, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** How long the bar takes to catch up with each tick. */
private const val ConnectApplyTiming = 300

/** One line of what goes on: a ring while it is going, a tick once it is on. */
@Composable
private fun StepLine(text: String, ticked: Boolean, working: Boolean) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = ticked,
                transitionSpec = {
                    if (reduced) fadeIn(tween(100)).togetherWith(fadeOut(tween(100)))
                    else scaleIn(spring(dampingRatio = 0.55f, stiffness = 600f), initialScale = 0.4f).togetherWith(fadeOut(tween(90)))
                },
                label = "stepTick",
            ) { on ->
                when {
                    on -> Box(
                        Modifier.size(22.dp).clip(CircleShape).background(palette.accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = inkOnTint(palette.accent), modifier = Modifier.size(15.dp))
                    }
                    working -> CircularProgressIndicator(
                        color = palette.onGlass,
                        trackColor = palette.onGlass.copy(alpha = 0.15f),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                    else -> Box(Modifier.size(18.dp).border(1.5.dp, palette.onGlass.copy(alpha = 0.3f), CircleShape))
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            color = if (ticked || working) palette.onGlass else palette.onGlassMuted,
            fontSize = 15.sp,
            fontWeight = if (ticked) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
