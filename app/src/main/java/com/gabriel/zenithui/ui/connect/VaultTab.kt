package com.gabriel.zenithui.ui.connect

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.connect.ConnectSetup
import com.gabriel.zenithui.data.connect.ConnectStudio
import com.gabriel.zenithui.data.connect.ConnectVault
import com.gabriel.zenithui.data.connect.VaultLayout
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.settings.Toggle
import com.gabriel.zenithui.ui.theme.LocalPalette
import java.time.LocalDateTime

// The Vault: who you are on Zenith Connect, your layouts kept in the cloud and the setups you
// saved. Your name is the one you gave Zenith; the layouts and the sync are the prototype's
// samples for now, as Connect has no server yet.

@Composable
internal fun VaultTab(state: ConnectState, userName: String, handle: String) {
    val saved = state.saved.mapNotNull { id -> state.setups.firstOrNull { it.id == id } }
    LazyColumn(
        state = state.vaultList,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pageTop(), bottom = pageBottom()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "head") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PageTitle("Vault", Modifier.weight(1f))
                RoundGlassButton(Icons.Rounded.Tune, "Accent color", size = 44.dp) { state.accentOpen = true }
            }
        }
        item(key = "profile") { Profile(userName, handle) }
        item(key = "sync") { SyncCard(state) }
        if (saved.isNotEmpty()) {
            item(key = "saved-head") {
                SectionHead("Saved setups", Modifier.padding(top = 8.dp).animateItem(), inset = 0.dp) { HeadNote("${saved.size}") }
            }
            item(key = "saved") {
                Row(
                    Modifier
                        .animateItem()
                        .bleed(16.dp)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    saved.forEach { setup -> SavedSetup(setup) { from -> state.openSetup(setup, from) } }
                }
            }
        }
        item(key = "layouts-head") {
            SectionHead("Your layouts", Modifier.padding(top = 8.dp).animateItem(), inset = 0.dp) {
                val accent = LocalConnectAccent.current
                val tick = rememberTick()
                Row(
                    Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(role = Role.Button) {
                            tick()
                            state.backUp(LocalDateTime.now())
                        }
                        .padding(start = 10.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Back up current", color = accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        items(state.layouts, key = { it.id }) { layout ->
            LayoutRow(
                layout,
                current = layout.id == state.inUse,
                modifier = Modifier.animateItem(
                    fadeInSpec = tween(460),
                    placementSpec = spring(dampingRatio = 0.84f, stiffness = 300f, visibilityThreshold = IntOffset(1, 1)),
                    fadeOutSpec = tween(160),
                ),
            ) { state.restore(layout) }
        }
    }
}

/** You: your letter in the accent's ring, your name and handle, your badges, and the one Zenith ID they follow. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Profile(userName: String, handle: String) {
    val palette = LocalPalette.current
    val accent = LocalConnectAccent.current
    val green = liveGreen()
    Column(
        Modifier
            .fillMaxWidth()
            .connectCard()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val ring by animateColorAsState(accent, tween(400), label = "avatarRing")
            Box(
                Modifier
                    .size(60.dp)
                    .drawBehind { drawCircle(ring.copy(alpha = 0.16f), radius = size.minDimension / 2f + 5.dp.toPx()) }
                    .clip(CircleShape)
                    .background(Color(0xFF1C1B2E))
                    .border(2.dp, ring, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(ConnectStudio.initial(handle), color = Color(0xFFF6F6F9), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(userName.trim().ifEmpty { "You" }, color = palette.onGlass, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("@$handle · ", color = palette.onGlassMuted, fontSize = 14.sp, maxLines = 1)
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = palette.onGlassMuted, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Zenith ID", color = palette.onGlassMuted, fontSize = 14.sp, maxLines = 1)
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Badge(Icons.Rounded.Science, "Beta Tester", green)
            Badge(Icons.Rounded.AutoAwesome, "Creator", accent)
            Badge(Icons.Rounded.Star, "Setup of the Week", null)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(quietEdge()))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinkedTiles()
            Spacer(Modifier.width(12.dp))
            Text(
                "One Zenith ID. Badges, downloads and beta access follow you across your apps.",
                color = palette.onGlass.copy(alpha = 0.78f),
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

/** A badge: its little round icon in its colour, and its name. */
@Composable
private fun Badge(icon: ImageVector, name: String, colour: Color?) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(50))
            .background(quietFill())
            .border(1.dp, quietEdge(), RoundedCornerShape(50))
            .padding(start = 6.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(colour?.copy(alpha = 0.16f) ?: palette.onGlass.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colour ?: palette.onGlass, modifier = Modifier.size(12.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text(name, color = palette.onGlass, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Zenith's tile and Connect's, one over the other: the apps the ID joins. */
@Composable
private fun LinkedTiles() {
    val palette = LocalPalette.current
    val ring = if (palette.dark) Color(0xFF17171F) else Color.White
    Row {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (palette.dark) Color(0xFFF6F6F9) else Color(0xFF16161E))
                .border(2.dp, ring, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("Z", color = if (palette.dark) Color(0xFF0B0C12) else Color(0xFFF6F6F9), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            Modifier
                .offset(x = (-8).dp)
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF2A2170), Color(0xFF0B0A24))))
                .border(2.dp, ring, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            ConnectMark(Color.White, Modifier.size(18.dp))
        }
    }
}

/** Cloud sync: the cloud to sync by hand, what has been kept, and Auto-sync's switch. */
@Composable
private fun SyncCard(state: ConnectState) {
    val palette = LocalPalette.current
    val green = liveGreen()
    val reduced = LocalMotionLevel.current.reduced
    val tick = rememberTick()
    val (title, sub) = ConnectVault.syncCopy(state.autoSync, state.syncedByHand, state.lastSync, state.layouts.size)
    val on = state.autoSync || state.syncedByHand || state.syncing
    val face by animateColorAsState(if (on) green.copy(alpha = 0.14f) else quietFill(), tween(300), label = "syncFace")
    val ink by animateColorAsState(if (on) green else palette.onGlassMuted, tween(300), label = "syncInk")
    Row(
        Modifier
            .fillMaxWidth()
            .connectCard()
            .padding(start = 14.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .pressBounce(0.9f)
                .popOn(state.syncRun)
                .clip(CircleShape)
                .background(face)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClickLabel = "Sync now",
                ) {
                    tick()
                    state.syncNow(reduced)
                },
            contentAlignment = Alignment.Center,
        ) {
            if (state.syncing) {
                val turn = rememberInfiniteTransition(label = "sync")
                val angle by turn.animateFloat(0f, -360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "syncTurn")
                Icon(Icons.Rounded.Sync, contentDescription = null, tint = ink, modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = if (reduced) 0f else angle })
            } else {
                Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = ink, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AnimatedContent(
                targetState = if (state.syncing) "Syncing…" else title,
                transitionSpec = { com.gabriel.zenithui.ui.common.wordsChange(reduced) },
                label = "syncTitle",
            ) { words ->
                Text(words, color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            AnimatedContent(targetState = sub, transitionSpec = { com.gabriel.zenithui.ui.common.wordsChange(reduced) }, label = "syncSub") { words ->
                Text(words, color = palette.onGlassMuted, fontSize = 13.sp, lineHeight = 17.sp)
            }
        }
        Spacer(Modifier.width(8.dp))
        val touch = remember { MutableInteractionSource() }
        val pressed by touch.collectIsPressedAsState()
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .clickable(interactionSource = touch, indication = null, role = Role.Switch, onClickLabel = "Auto-sync") {
                    tick()
                    state.toggleAutoSync()
                }
                .padding(horizontal = 6.dp, vertical = 9.dp),
        ) {
            Toggle(checked = state.autoSync, pressed = pressed)
        }
    }
}

/** A setup you saved, small: tapped, its page opens. */
@Composable
private fun SavedSetup(setup: ConnectSetup, onOpen: (Rect?) -> Unit) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    val bounds = remember { arrayOfNulls<Rect>(1) }
    Column(
        Modifier
            .width(96.dp)
            .pressBounce(0.95f)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                tick()
                onOpen(bounds[0])
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MiniPhone(
            setup,
            Modifier
                .size(width = 84.dp, height = 168.dp)
                .onGloballyPositioned { bounds[0] = it.boundsInWindow() },
            corner = 18.dp,
        )
        Spacer(Modifier.height(8.dp))
        Text(setup.name, color = palette.onGlass, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("@${setup.by}", color = palette.onGlassMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A layout in the Vault: a little picture of it, its name and size, and In use or Restore. */
@Composable
private fun LayoutRow(layout: VaultLayout, current: Boolean, modifier: Modifier = Modifier, onRestore: () -> Unit) {
    val palette = LocalPalette.current
    val green = liveGreen()
    val reduced = LocalMotionLevel.current.reduced
    val tone = Tone.of(layout.dark)
    Row(
        modifier
            .fillMaxWidth()
            .connectCard(RoundedCornerShape(22.dp), outline = if (current) green.copy(alpha = 0.4f) else null)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 40.dp, height = 76.dp)
                .clip(RoundedCornerShape(10.dp))
                .wall(layout.wall)
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(10.dp)),
        ) {
            Box(Modifier.padding(start = 6.dp, top = 10.dp).size(width = 16.dp, height = 5.dp).clip(RoundedCornerShape(3.dp)).background(tone.ink))
            Box(Modifier.padding(start = 5.dp, end = 5.dp, top = 21.dp).fillMaxWidth().height(12.dp).clip(RoundedCornerShape(4.dp)).background(tone.glass))
            Box(Modifier.align(Alignment.BottomCenter).padding(start = 5.dp, end = 5.dp, bottom = 5.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(tone.glass))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(layout.name, color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(layout.meta, color = palette.onGlassMuted, fontSize = 13.sp, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                if (reduced) {
                    fadeIn(tween(150)).togetherWith(fadeOut(tween(150)))
                } else {
                    (scaleIn(spring(dampingRatio = 0.55f, stiffness = 520f), initialScale = 0.6f) + fadeIn(tween(200))).togetherWith(fadeOut(tween(100)))
                }
            },
            contentAlignment = Alignment.CenterEnd,
            label = "inUse",
        ) { inUse ->
            if (inUse) {
                Row(
                    Modifier
                        .padding(end = 6.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(50))
                        .background(green.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(green))
                    Spacer(Modifier.width(6.dp))
                    Text("IN USE", color = green, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, letterSpacing = 0.9.sp)
                }
            } else {
                GhostButton("Restore", height = 36.dp) { onRestore() }
            }
        }
    }
}
