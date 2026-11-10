package com.gabriel.zenithui.ui.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.Look
import com.gabriel.zenithui.data.LookRule
import com.gabriel.zenithui.data.LookSchedule
import com.gabriel.zenithui.data.Looks
import com.gabriel.zenithui.data.PlusStatus
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.data.server.SharedLook
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.NeutralLook
import kotlinx.coroutines.launch

/** What the Looks page can ask the home screen to do: each opens a sheet, or puts a Look on. */
class LookActions(
    /** Whether this build has a server: with none, nothing can be shared or opened by a code. */
    val server: Boolean,
    val save: () -> Unit,
    val put: (Look) -> Unit,
    val menu: (Look) -> Unit,
    val addRule: () -> Unit,
    val removeRule: (Int) -> Unit,
    val openCode: () -> Unit,
    /** The Looks the server's keeper chose, or none in a build with no server. */
    val featured: suspend () -> List<SharedLook>,
    val openFeatured: (SharedLook) -> Unit,
)

/**
 * Settings → Looks: the Looks saved on this phone, each a tap from being put on; when they come
 * on by themselves; and a way to open one somebody shared. Saving, switching, scheduling and
 * sharing are Zenith Plus; opening a Look somebody sent you is not.
 */
@Composable
internal fun LooksOptions(settings: ZenithSettings, plus: PlusStatus, looks: List<Look>, actions: LookActions) {
    // Asked for only when tapped: with no account, nothing goes to the server unless you ask.
    var featured by remember { mutableStateOf<List<SharedLook>?>(null) }
    var asking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val reduced = LocalMotionLevel.current.reduced
    // What is here as the page opens rises with the page; only what is added after unfolds in.
    val firstLooks = remember { looks.map { it.id }.toSet() }
    val firstRules = remember { settings.lookRules.toSet() }
    val growing = if (reduced) Modifier else Modifier.animateContentSize(spring(dampingRatio = 0.86f, stiffness = 240f))
    Group(rise = 0) {
        Spot("lookSave") {
            ActionRow(
                icon = if (plus.active) Icons.Rounded.AddCircleOutline else Icons.Rounded.Lock,
                title = "Save this look",
                subtitle = if (plus.active) "Light or dark, glass, wallpaper, icons, grid and layout, under a name" else "Plus: keep looks, and switch between them in a tap",
                onClick = actions.save,
            )
        }
    }
    if (looks.isNotEmpty()) {
        Group(rise = 1) {
            Column(growing) {
                looks.forEachIndexed { index, look ->
                    key(look.id) {
                        Column(if (look.id in firstLooks) Modifier else Modifier.appearIn(riseFrom = 14f, gentle = true)) {
                            if (index > 0) GroupDivider()
                            LookRow(look, inUse = settings.activeLook == look.id, onPut = { actions.put(look) }, onMenu = { actions.menu(look) })
                        }
                    }
                }
            }
        }
    }
    Group(rise = 2) {
        Spot("lookSchedule") {
            Column(growing) {
                Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp)) {
                    Caption("Schedule")
                    Spacer(Modifier.padding(top = 4.dp))
                    Note(
                        when {
                            settings.lookRules.isEmpty() -> "A Look can come on by itself: a Work look on weekdays, a Night look after ten."
                            !plus.active -> "Kept, but resting: the schedule switches Looks while you have Plus."
                            else -> "Each comes on as its time begins, and when you come home during it."
                        },
                    )
                }
                settings.lookRules.forEachIndexed { index, rule ->
                    key(rule) {
                        ScheduleRow(
                            name = looks.firstOrNull { it.id == rule.lookId }?.name ?: "A Look that has gone",
                            rule = rule,
                            arriving = rule !in firstRules,
                            onRemove = { actions.removeRule(index) },
                        )
                    }
                }
                ActionRow(
                    icon = if (plus.active) Icons.Rounded.Schedule else Icons.Rounded.Lock,
                    title = "Add a time",
                    subtitle = if (looks.isEmpty()) "Save a look first" else "A Look, the hours it is on, and the days",
                    onClick = actions.addRule,
                )
            }
        }
    }
    if (actions.server) {
        Group(rise = 3) {
            Column(growing) {
                Spot("lookCode") {
                    ActionRow(
                        icon = Icons.Rounded.QrCode2,
                        title = "Open a code",
                        subtitle = "A Look somebody shared with you: LOOK- and six letters",
                        onClick = actions.openCode,
                    )
                }
                GroupDivider()
                ActionRow(
                    icon = Icons.Rounded.Star,
                    title = "Featured looks",
                    subtitle = when {
                        asking -> "Asking Zenith's server…"
                        featured == null -> "A few chosen by Zenith's maker"
                        featured.isNullOrEmpty() -> "None just now. Tap to ask again"
                        else -> "Tap one to see it, then Open to put it on"
                    },
                    onClick = {
                        if (!asking) {
                            asking = true
                            scope.launch {
                                featured = actions.featured()
                                asking = false
                            }
                        }
                    },
                )
                // Each unfolds in as they arrive, one after another.
                featured.orEmpty().forEachIndexed { index, shared ->
                    key(shared.code) {
                        Column(Modifier.appearIn(delayMillis = staggerDelay(index), riseFrom = 14f, gentle = true)) {
                            GroupDivider()
                            ActionRow(icon = Icons.Rounded.Star, title = shared.name, subtitle = "Featured · ${shared.shown}", onClick = { actions.openFeatured(shared) })
                        }
                    }
                }
            }
        }
    }
    Note(
        "A Look keeps everything you see, and nothing of yours: not your name, your notes or lists, your hidden apps, Aura or Plus.",
        Modifier.riseIn(4).padding(horizontal = 14.dp),
    )
}

/** One time on the schedule: the Look, its hours and days, and a way to take it away. */
@Composable
private fun ScheduleRow(name: String, rule: LookRule, arriving: Boolean, onRemove: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .then(if (arriving) Modifier.appearIn(riseFrom = 14f, gentle = true) else Modifier)
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = palette.onGlassMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                "${LookSchedule.clock(rule.start)}–${LookSchedule.clock(rule.end)} · ${LookSchedule.days(rule.days)}",
                color = palette.onGlassMuted,
                fontSize = 13.sp,
            )
        }
        Box(
            Modifier
                .size(34.dp)
                .pressBounce()
                .clip(CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "Take this time away", tint = palette.onGlassMuted, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * A saved Look: whether it is light or dark, as three dots of Zenith's look in that mode, its name,
 * and whether it is the one on now.
 */
@Composable
private fun LookRow(look: Look, inUse: Boolean, onPut: () -> Unit, onMenu: () -> Unit) {
    val palette = LocalPalette.current
    // Light or dark, read from what the Look keeps; one that goes with the phone shows as the phone
    // is now. Zenith has one look, so that is what the dots can tell Looks apart by.
    val systemDark = isSystemInDarkTheme()
    val its = remember(look, systemDark) { NeutralLook.of(Looks.apply(ZenithSettings(), look).first.isDark(systemDark)) }
    Row(
        Modifier
            .fillMaxWidth()
            .pressBounce(0.98f)
            .clickable(onClick = onPut)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 44.dp, height = 28.dp)) {
            // Each is rimmed, so Dark's black on a dark page, or Light's white on a pale one, is
            // still a dot.
            val dots = listOf(its.backdrop.first(), its.backdrop.last(), its.accent)
            dots.forEachIndexed { i, colour ->
                Box(
                    Modifier
                        .offset(x = (i * 10).dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(colour)
                        .border(1.dp, palette.onGlass.copy(alpha = 0.16f), CircleShape),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(look.name, color = palette.onGlass, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (inUse) "On now" else "Tap to put it on",
                color = if (inUse) palette.onGlass else palette.onGlassMuted,
                fontSize = 13.sp,
                fontWeight = if (inUse) FontWeight.SemiBold else null,
            )
        }
        Box(
            Modifier
                .size(38.dp)
                .pressBounce()
                .clip(CircleShape)
                .clickable(onClick = onMenu),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.MoreHoriz, contentDescription = "More for ${look.name}", tint = palette.onGlassMuted, modifier = Modifier.size(20.dp))
        }
    }
}
