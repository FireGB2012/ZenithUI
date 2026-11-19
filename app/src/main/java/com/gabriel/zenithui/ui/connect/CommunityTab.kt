package com.gabriel.zenithui.ui.connect

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.connect.Channel
import com.gabriel.zenithui.data.connect.ConnectCommunity
import com.gabriel.zenithui.data.connect.ConnectMessage
import com.gabriel.zenithui.data.connect.ConnectSamples
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.theme.LocalLite
import com.gabriel.zenithui.ui.theme.LocalPalette

// Community: the Zenith Discord's channels, as Connect shows them, with likes, votes, the beta's
// build and setups to open in Discover. The posts are the prototype's samples for now.

/** How long a post just in stays lit, as ConnectState keeps it. */
private const val LANDED_MS = 2_600

/** The prototype's ease, which everything that settles here settles by. */
private val EASE = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

@Composable
internal fun CommunityTab(state: ConnectState) {
    val feed = state.messages.filter { it.channel == state.channel }
    // Posts rise in once when a channel opens, never again as they scroll back into view.
    val seen = remember(state.channel) { mutableSetOf<String>() }
    LazyColumn(
        state = state.communityList,
        contentPadding = PaddingValues(top = pageTop(), bottom = pageBottom()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "head") { CommunityHead(state) }
        item(key = "channels") {
            PillPicker(
                options = Channel.entries,
                selected = state.channel,
                label = { it.title },
                leading = Icons.Rounded.Tag,
                dots = state.unread.toSet(),
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            ) { state.pickChannel(it) }
        }
        items(feed, key = { it.id }) { message ->
            val index = feed.indexOf(message)
            val first = remember { seen.add(message.id) }
            val live = message.id == state.landed
            Box(
                Modifier
                    .animateItem(
                        fadeInSpec = null,
                        placementSpec = spring(dampingRatio = 0.86f, stiffness = 300f, visibilityThreshold = IntOffset(1, 1)),
                        fadeOutSpec = null,
                    )
                    .padding(horizontal = 16.dp)
                    .then(
                        if (first) {
                            Modifier.appearIn(
                                delayMillis = if (live) 0 else 55 * index.coerceAtMost(6),
                                fromScale = 0.97f,
                                riseFrom = with(LocalDensity.current) { (if (live) -12 else 14).dp.toPx() },
                                replay = state.channel,
                                gentle = true,
                            )
                        } else {
                            Modifier
                        },
                    ),
            ) {
                MessageCard(message, state, live)
            }
        }
    }
}

@Composable
private fun CommunityHead(state: ConnectState) {
    val palette = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            PageTitle("Community")
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveDot()
                Spacer(Modifier.width(8.dp))
                Text("Live from the Zenith Discord", color = palette.onGlassMuted, fontSize = 13.sp)
            }
        }
        RoundGlassButton(Icons.AutoMirrored.Rounded.OpenInNew, "Open the server in Discord", size = 44.dp) {
            state.say("Your Discord invite link goes here")
        }
    }
}

/** A green dot with a ring going out from it now and then, as something live has. Still in Lite. */
@Composable
internal fun LiveDot(modifier: Modifier = Modifier) {
    val green = liveGreen()
    val still = LocalLite.current || LocalMotionLevel.current.reduced
    val beat = rememberInfiniteTransition(label = "live")
    val ring by beat.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f))), label = "liveRing")
    Canvas(modifier.size(20.dp)) {
        val r = 4.dp.toPx()
        if (!still) {
            val p = if (ring < 0.7f) ring / 0.7f else 1f
            drawCircle(green.copy(alpha = 0.6f * (1f - p)), radius = r + 8.dp.toPx() * p)
        }
        drawCircle(green, radius = r)
    }
}

/**
 * A post: who wrote it and when, what it says, and what it carries (a changelog, a setup's
 * picture, the beta's build), then its likes or votes and its replies. Just in, it is outlined in
 * green for a moment.
 */
@Composable
private fun MessageCard(message: ConnectMessage, state: ConnectState, live: Boolean) {
    val palette = LocalPalette.current
    val green = liveGreen()
    val person = ConnectSamples.people[message.who]
    val flash = remember(message.id) { Animatable(0f) }
    LaunchedEffect(live) {
        if (!live) return@LaunchedEffect
        flash.snapTo(1f)
        flash.animateTo(0f, tween(LANDED_MS, easing = EASE))
    }
    val lit = flash.value
    val edge = if (palette.dark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.05f)
    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                // A soft ring just outside the card, fading with the outline.
                if (lit <= 0.01f) return@drawBehind
                val ring = 4.dp.toPx()
                drawRoundRect(
                    green.copy(alpha = 0.2f * lit),
                    topLeft = Offset(-ring / 2f, -ring / 2f),
                    size = Size(size.width + ring, size.height + ring),
                    cornerRadius = CornerRadius(22.dp.toPx() + ring / 2f),
                    style = Stroke(ring),
                )
            }
            .connectCard(RoundedCornerShape(22.dp), outline = if (lit > 0.01f) lerp(edge, green.copy(alpha = 0.85f), lit) else null)
            .padding(14.dp),
    ) {
        Avatar(
            person?.initial ?: "?",
            bg = Color(person?.bg ?: 0xFF2A2150),
            fg = Color(person?.fg ?: 0xFFF6F6F9),
            size = 36.dp,
            modifier = if (palette.dark) Modifier else Modifier.border(1.dp, quietEdge(), CircleShape),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(person?.name ?: message.who, color = palette.onGlass, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                if (!person?.role.isNullOrEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    RoleTag(person!!.role)
                }
                Spacer(Modifier.weight(1f))
                Text(message.time, color = palette.onGlassMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
            if (message.text.isNotEmpty()) {
                Text(message.text, color = palette.onGlass.copy(alpha = 0.9f), fontSize = 14.sp, lineHeight = 20.sp)
            }
            if (message.version.isNotEmpty()) Changes(message)
            if (message.image != null) Picture(message.image, message.clock)
            if (message.build) BuildLink { state.goTab(ConnectTab.BETA) }
            Actions(message, state)
        }
    }
}

/** Dev or Mod, in small capitals by a name. */
@Composable
private fun RoleTag(role: String) {
    val colour = if (role == "Dev") LocalConnectAccent.current else liveGreen()
    Box(
        Modifier
            .height(20.dp)
            .clip(RoundedCornerShape(50))
            .background(colour.copy(alpha = 0.16f))
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(role.uppercase(), color = colour, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, letterSpacing = 0.8.sp)
    }
}

/** Planned, In progress or Exploring, as a feedback post or the roadmap has it. */
@Composable
internal fun StatusTag(status: String, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val (face, ink) = when (status) {
        "Planned" -> LocalConnectAccent.current.let { it.copy(alpha = 0.16f) to it }
        "In progress" -> liveGreen().let { it.copy(alpha = 0.14f) to it }
        else -> quietFill() to palette.onGlass.copy(alpha = 0.72f)
    }
    Box(
        modifier
            .height(22.dp)
            .clip(RoundedCornerShape(50))
            .background(face)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(status.uppercase(), color = ink, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, letterSpacing = 0.8.sp, maxLines = 1)
    }
}

/** A changelog: the version, and each change marked New or Fix. */
@Composable
private fun Changes(message: ConnectMessage) {
    val palette = LocalPalette.current
    val accent = LocalConnectAccent.current
    val green = liveGreen()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(quietFill().copy(alpha = quietFill().alpha * 0.7f))
            .border(1.dp, quietEdge().copy(alpha = quietEdge().alpha * 0.7f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(message.version, color = palette.onGlass, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
        message.changes.forEach { (kind, what) ->
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    kind.uppercase(),
                    color = if (kind == "Fix") green else accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.width(34.dp).padding(top = 2.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(what, color = palette.onGlass.copy(alpha = 0.78f), fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

/** A setup's picture in a post: its wallpaper, with a little phone tipped on it showing the time. */
@Composable
private fun Picture(wall: String, clock: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .wall(wall)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
    ) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = 22.dp)
                .size(width = 84.dp, height = 170.dp)
                .graphicsLayer { rotationZ = -6f }
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF07070D))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                .padding(3.dp),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(13.dp))
                    .wall(wall)
                    .padding(start = 7.dp, end = 7.dp, top = 12.dp, bottom = 7.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(clock, color = Color(0xFFF6F6F9), fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 13.sp)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(7.dp)),
                )
                MiniGrid(Color.White.copy(alpha = 0.22f))
            }
        }
    }
}

/** The beta's build, in a post: opens the Beta Hub. */
@Composable
private fun BuildLink(onClick: () -> Unit) {
    val palette = LocalPalette.current
    val accent = LocalConnectAccent.current
    val tick = rememberTick()
    Row(
        Modifier
            .fillMaxWidth()
            .pressBounce(0.98f)
            .clip(RoundedCornerShape(16.dp))
            .background(quietFill())
            .border(1.dp, quietEdge(), RoundedCornerShape(16.dp))
            .clickable(role = Role.Button) {
                tick()
                onClick()
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Download, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(ConnectSamples.BUILD_FILE, color = palette.onGlass, fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("38.6 MB · open in Beta Hub", color = palette.onGlassMuted, fontSize = 12.sp)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = palette.onGlassMuted, modifier = Modifier.size(20.dp))
    }
}

/** A post's foot: a heart and its count, or for feedback its status and a vote, then its replies. */
@Composable
private fun Actions(message: ConnectMessage, state: ConnectState) {
    val palette = LocalPalette.current
    val accent = LocalConnectAccent.current
    val muted = palette.onGlassMuted
    Row(
        Modifier
            .fillMaxWidth()
            .offset(x = (-10).dp)
            .padding(top = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (message.status.isNotEmpty()) {
            Spacer(Modifier.width(10.dp))
            StatusTag(message.status)
            val on = message.id in state.voted
            React(
                icon = Icons.Rounded.ArrowUpward,
                count = message.votes,
                on = on,
                onColour = accent,
                hop = true,
                description = if (on) "Remove your vote, ${message.votes} votes" else "Upvote, ${message.votes} votes",
            ) { state.vote(message) }
        } else {
            val on = message.id in state.liked
            React(
                icon = if (on) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                count = message.likes,
                on = on,
                onColour = heartPink(),
                hop = false,
                description = if (on) "Unlike, ${message.likes} likes" else "Like, ${message.likes} likes",
            ) { state.like(message) }
        }
        Row(Modifier.heightIn(min = 44.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Rounded.Reply, contentDescription = null, tint = muted, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(ConnectCommunity.replies(message.replies), color = muted, fontSize = 13.sp)
        }
        val link = message.link
        if (link != null && message.status.isEmpty()) {
            Spacer(Modifier.weight(1f))
            val tick = rememberTick()
            Row(
                Modifier
                    .offset(x = 18.dp)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(role = Role.Button) {
                        tick()
                        state.openFromPost(link)
                    }
                    .padding(start = 10.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Open in Discover", color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * A heart or a vote and its count: in its colour once yours, the count rolling to its new value,
 * the heart popping or the arrow hopping the way the vote went (see [reactMotion]).
 */
@Composable
internal fun React(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    on: Boolean,
    onColour: Color,
    hop: Boolean,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    val ink by androidx.compose.animation.animateColorAsState(if (on) onColour else palette.onGlassMuted, tween(200), label = "reactInk")
    Row(
        modifier
            .heightIn(min = 44.dp)
            .widthIn(min = 44.dp)
            .pressBounce(0.9f)
            .clip(RoundedCornerShape(50))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = description,
            ) {
                tick()
                onClick()
            }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(16.dp).reactMotion(on, hop))
        Spacer(Modifier.width(6.dp))
        RollingNumber(count, ink)
    }
}

/**
 * What a heart or a vote's arrow does as it is pressed: the heart pops, the arrow hops up for a
 * vote and down for one taken back. Nothing when it first shows, nor with movement reduced.
 */
@Composable
internal fun Modifier.reactMotion(on: Boolean, hop: Boolean): Modifier {
    val reduced = LocalMotionLevel.current.reduced
    val lift = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(on) {
        if (first[0]) {
            first[0] = false
            return@LaunchedEffect
        }
        if (reduced) return@LaunchedEffect
        if (hop) {
            lift.animateTo(0f, keyframes {
                durationMillis = 320
                (if (on) -5f else 5f) at 120
                0f at 320
            })
        } else {
            pop.snapTo(0.7f)
            pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 520f))
        }
    }
    return graphicsLayer {
        translationY = lift.value * density
        scaleX = pop.value
        scaleY = pop.value
    }
}
