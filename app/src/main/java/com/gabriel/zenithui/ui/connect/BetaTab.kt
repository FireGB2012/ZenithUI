package com.gabriel.zenithui.ui.connect

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.BuildConfig
import com.gabriel.zenithui.data.connect.ConnectBeta
import com.gabriel.zenithui.data.connect.ConnectSamples
import com.gabriel.zenithui.data.connect.DownloadState
import com.gabriel.zenithui.data.connect.RoadmapItem
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.common.shake
import com.gabriel.zenithui.ui.theme.LocalLite
import com.gabriel.zenithui.ui.theme.LocalPalette

// The Beta Hub: the latest test build to download, a bug report that says what phone it is from,
// and the roadmap to vote on. The build and the roadmap are the prototype's samples; the phone
// and the Zenith a report is about are this one's.

@Composable
internal fun BetaTab(state: ConnectState) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(state.betaScroll)
            .padding(start = 16.dp, end = 16.dp, top = pageTop(), bottom = pageBottom()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PageTitle("Beta Hub", Modifier.weight(1f))
            TesterChip()
        }
        BuildCard(state)
        BugCard(state)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
            SectionHead("Vote on what's next", inset = 0.dp) { Mono("roadmap") }
            state.roadmap.forEach { item -> RoadRow(item) { state.voteRoad(item) } }
        }
    }
}

/** Small words in the typewriter face, as the prototype puts its numbers and files. */
@Composable
internal fun Mono(text: String, modifier: Modifier = Modifier, colour: Color = LocalPalette.current.onGlassMuted, size: Int = 12) {
    Text(text, color = colour, fontSize = size.sp, fontFamily = FontFamily.Monospace, maxLines = 1, modifier = modifier)
}

/** You are in the beta: the green chip by the title. */
@Composable
private fun TesterChip() {
    val green = liveGreen()
    Row(
        Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(50))
            .background(green.copy(alpha = 0.12f))
            .border(1.dp, green.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(start = 10.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Science, contentDescription = null, tint = green, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text("TESTER", color = green, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, letterSpacing = 1.1.sp)
    }
}

/** The latest test build: what it is, what is new in it, and its download, which fills as it comes. */
@Composable
private fun BuildCard(state: ConnectState) {
    val palette = LocalPalette.current
    val accent = LocalConnectAccent.current
    val still = LocalLite.current || LocalMotionLevel.current.reduced
    val glowing = rememberInfiniteTransition(label = "buildGlow")
    val glow by glowing.animateFloat(0f, 1f, infiniteRepeatable(tween(5000), RepeatMode.Reverse), label = "glow")
    val busy = state.download == DownloadState.BUSY
    val progress by produceState(state.downloadProgress(), busy, state.downloadStartedAt) {
        value = state.downloadProgress()
        while (busy && value < 1f) {
            withFrameMillis { }
            value = state.downloadProgress()
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .connectCard(),
    ) {
        // The accent's glow in the far corner, breathing slowly.
        Canvas(Modifier.matchParentSize().clip(CARD_SHAPE)) {
            val g = if (still) 0.6f else glow
            val centre = Offset(size.width - 40.dp.toPx(), 10.dp.toPx())
            val reach = 120.dp.toPx()
            scale(0.95f + 0.15f * g, pivot = centre) {
                drawCircle(
                    Brush.radialGradient(listOf(accent.copy(alpha = 0.26f * (0.6f + 0.4f * g)), accent.copy(alpha = 0f)), center = centre, radius = reach),
                    radius = reach,
                    center = centre,
                )
            }
        }
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Overline("Latest test build", Modifier.weight(1f))
                Mono("2h ago")
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Zenith ${ConnectSamples.BUILD_VERSION}", color = palette.onGlass, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp)
                Spacer(Modifier.width(10.dp))
                Tag(ConnectSamples.BUILD_TAG, accent)
            }
            Spacer(Modifier.height(6.dp))
            Mono(ConnectSamples.BUILD_META)
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ConnectSamples.BUILD_NOTES.forEach { note ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
                        Spacer(Modifier.width(10.dp))
                        Text(note, color = palette.onGlass.copy(alpha = 0.9f), fontSize = 14.sp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AccentButton(
                    ConnectBeta.downloadLabel(state.download, progress),
                    icon = if (state.download == DownloadState.DONE) Icons.Rounded.Check else Icons.Rounded.Download,
                    progress = if (busy) progress else null,
                    done = state.download == DownloadState.DONE,
                    height = 52.dp,
                    modifier = Modifier.weight(1f),
                ) { state.startDownload() }
                GhostButton("Changelog", height = 52.dp) { state.openChangelog() }
            }
        }
    }
}

/** Found a bug: what comes with a report, and a box to say what happened in. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BugCard(state: ConnectState) {
    val palette = LocalPalette.current
    val bug = coral()
    Column(
        Modifier
            .fillMaxWidth()
            .connectCard()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(bug.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.BugReport, contentDescription = null, tint = bug, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Found a bug?", color = palette.onGlass, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("Logs attach themselves. Just say what happened.", color = palette.onGlassMuted, fontSize = 13.sp, lineHeight = 17.sp)
            }
            Spacer(Modifier.width(8.dp))
            GhostButton(if (state.reporting) "Cancel" else "Report") { state.toggleReport() }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Attachment(Icons.Outlined.PhoneAndroid, ConnectBeta.deviceLine(Build.MODEL ?: "", Build.VERSION.RELEASE ?: ""))
            Attachment(Icons.Outlined.Terminal, "Logcat · last 5 min")
            Attachment(Icons.Outlined.Science, "Zenith ${BuildConfig.VERSION_NAME}")
            Attachment(Icons.Outlined.Image, "Screenshot")
        }
        AnimatedVisibility(visible = state.reporting) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                InputBox(
                    value = state.report,
                    onChange = {
                        state.report = it.take(1000)
                        state.reportError = false
                    },
                    placeholder = "What happened? For example: dock icons jump after I rotate the phone",
                    lines = 3,
                    error = state.reportError,
                    modifier = Modifier.shake(state.reportShake),
                )
                AnimatedVisibility(visible = state.reportError) {
                    Text("Add a line about what happened first.", color = palette.destructive, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
                }
                AccentButton("Send report", height = 44.dp, modifier = Modifier.fillMaxWidth()) { state.sendReport() }
            }
        }
    }
}

/** What goes with a report, in a little label. */
@Composable
private fun Attachment(icon: ImageVector, text: String) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(quietFill().copy(alpha = quietFill().alpha * 0.7f))
            .border(1.dp, quietEdge().copy(alpha = quietEdge().alpha * 0.7f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = palette.onGlassMuted, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(6.dp))
        Mono(text, colour = palette.onGlass.copy(alpha = 0.75f), size = 11)
    }
}

/** Something on the roadmap: what it is, where it has got to, and your vote for it. */
@Composable
private fun RoadRow(item: RoadmapItem, onVote: () -> Unit) {
    val palette = LocalPalette.current
    val accent = LocalConnectAccent.current
    Row(
        Modifier
            .fillMaxWidth()
            .connectCard(RoundedCornerShape(20.dp))
            .padding(start = 16.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.title, color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            StatusTag(item.status)
        }
        Spacer(Modifier.width(12.dp))
        VoteBox(item.votes, item.mine, accent, item.title, onVote)
    }
}

/** The square vote button: an arrow over the count, in the accent once yours. */
@Composable
private fun VoteBox(votes: Int, mine: Boolean, accent: Color, title: String, onVote: () -> Unit) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    val shape = RoundedCornerShape(18.dp)
    val face by animateColorAsState(if (mine) accent.copy(alpha = 0.16f) else quietFill(), tween(250), label = "voteFace")
    val edge by animateColorAsState(if (mine) accent.copy(alpha = 0.55f) else quietEdge(), tween(250), label = "voteEdge")
    val ink by animateColorAsState(if (mine) accent else palette.onGlass.copy(alpha = 0.72f), tween(250), label = "voteInk")
    Column(
        Modifier
            .size(56.dp)
            .pressBounce(0.92f)
            .clip(shape)
            .background(face)
            .border(1.dp, edge, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = if (mine) "Remove your vote for $title, $votes votes" else "Vote for $title, $votes votes",
            ) {
                tick()
                onVote()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.ArrowUpward, contentDescription = null, tint = ink, modifier = Modifier.size(17.dp).reactMotion(mine, hop = true))
        Spacer(Modifier.height(2.dp))
        RollingNumber(votes, ink, size = 12)
    }
}
