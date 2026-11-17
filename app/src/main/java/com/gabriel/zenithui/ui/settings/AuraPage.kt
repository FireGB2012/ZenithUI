package com.gabriel.zenithui.ui.settings

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.Aura
import com.gabriel.zenithui.data.AuraDay
import com.gabriel.zenithui.data.AuraDayState
import com.gabriel.zenithui.data.AuraFlame
import com.gabriel.zenithui.data.AuraLevels
import com.gabriel.zenithui.data.AuraReward
import com.gabriel.zenithui.data.AuraShop
import com.gabriel.zenithui.data.AuraWords
import com.gabriel.zenithui.data.PlusStatus
import com.gabriel.zenithui.data.WallStyle
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.data.server.Account
import com.gabriel.zenithui.data.title
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.popIn
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberHaptics
import com.gabriel.zenithui.ui.common.shake
import com.gabriel.zenithui.ui.common.unfoldIn
import com.gabriel.zenithui.ui.common.unfoldOut
import com.gabriel.zenithui.ui.home.WallpaperImage
import com.gabriel.zenithui.ui.home.widgets.FlameGlyph
import com.gabriel.zenithui.ui.onboarding.PrimaryButton
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import com.gabriel.zenithui.ui.theme.Palettes
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import com.gabriel.zenithui.ui.theme.inkOnTint
import com.gabriel.zenithui.ui.theme.toned
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Aura, as a page of Settings: what you have, what it has earned you, and what it buys.
 *
 * It is all on this one page on purpose: the aura in hand inside a ring that runs round to your
 * next level, the streak that keeps it coming, what each level brings and the flame you wear, what
 * aura buys, and the code you can pass on. Nothing here is hidden behind a second screen. Spending
 * never costs a level: the level counts what was earned. See
 * `specs/2026-11-17-aura-levels-and-plus-design.md`.
 */
@Composable
internal fun AuraOptions(settings: ZenithSettings, plus: PlusStatus, actions: SettingsActions, onAccount: () -> Unit) {
    val haptics = rememberHaptics()
    /** Goes up each time points arrive or are spent: the ring answers with a ripple. */
    var stirred by remember { mutableIntStateOf(0) }
    val earned = Aura.earned(settings)
    val level = AuraLevels.of(earned)

    Spot("aura") {
        AuraRing(aura = settings.aura, earned = earned, stirred = stirred)
        Spacer(Modifier.height(12.dp))
        StreakLadder(settings.streak)
    }

    Spot("auraLevels") {
        LevelRoad(
            settings = settings,
            level = level,
            earned = earned,
            onWear = { flame ->
                haptics.tick()
                actions.wearFlame(flame)
            },
            onColours = actions.wallpaper,
        )
    }

    Spot("auraFlame") {
        FlamePicker(
            worn = AuraFlame.worn(settings),
            level = level,
            onWear = { flame ->
                haptics.tick()
                actions.wearFlame(flame)
            },
        )
    }

    Spot("auraShop") {
        AuraShopGroup(
            settings = settings,
            plus = plus,
            onPlusDay = {
                val refused = actions.buyPlusDay()
                if (refused == null) {
                    haptics.confirm()
                    stirred++
                }
                refused
            },
            onDesign = { style ->
                val refused = actions.buyDesign(style)
                if (refused == null) {
                    haptics.confirm()
                    stirred++
                }
                refused
            },
            onWallpapers = actions.wallpaper,
        )
    }

    // With a server, codes are real: it knows whose each one is and counts who has used it, and
    // that takes an account. Until somebody is signed in, one card stands where the two would
    // be, and a search for either lands on it.
    val account = actions.account
    if (account != null && account.signed == null) {
        Spot("auraCode") {
            Spot("auraRedeem") { CodesNeedAccount(Modifier.riseIn(6), onAccount) }
        }
        // A code used before there were accounts is still a code used.
        if (settings.referredBy.isNotEmpty()) Redeem(settings.referredBy, Modifier.riseIn(7), onRedeem = { null })
    } else {
        Spot("auraCode") {
            YourCode(settings.referralCode, account?.signed?.account, Modifier.riseIn(6))
        }

        Spot("auraRedeem") {
            Redeem(
                redeemed = settings.referredBy,
                modifier = Modifier.riseIn(7),
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
 * The aura in hand, counted up as the page arrives, inside a ring in the words' ink that runs round
 * to the next level. It draws itself to where you stand as the page arrives; when a level is reached
 * while you look, it closes, ripples, and starts round again from the top. Points arriving or being
 * spent send a ripple too; the rest of the time it is still.
 */
@Composable
private fun AuraRing(aura: Int, earned: Int, stirred: Int) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    val level = AuraLevels.of(earned)
    val progress = AuraLevels.progress(earned)

    val counted = remember { Animatable(if (reduced) aura.toFloat() else 0f) }
    val drawn = remember { Animatable(if (reduced) progress else 0f) }
    val ripple = remember { Animatable(1f) }
    /** The level the ring is showing: it catches up with [level] once the ring has closed. */
    var shown by remember { mutableIntStateOf(level) }
    var closed by remember { mutableIntStateOf(0) }

    LaunchedEffect(aura, reduced) {
        if (reduced) counted.snapTo(aura.toFloat()) else counted.animateTo(aura.toFloat(), tween(900, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(level, progress, reduced) {
        if (reduced) {
            drawn.snapTo(progress)
            shown = level
            return@LaunchedEffect
        }
        if (level > shown) {
            drawn.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
            shown = level
            closed++
            drawn.snapTo(0f)
        }
        drawn.animateTo(progress, spring(dampingRatio = 0.8f, stiffness = 60f))
    }
    LaunchedEffect(stirred, closed, reduced) {
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
                // Turned so it starts at the top. A sliver is always shown, so the start of the way is seen.
                val sweep = (360f * drawn.value.coerceIn(0f, 1f)).coerceAtLeast(1.5f)
                rotate(-90f) {
                    drawArc(
                        color = palette.onGlass,
                        startAngle = 0f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = box,
                        style = Stroke(band, cap = StrokeCap.Round),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedContent(
                    targetState = shown,
                    transitionSpec = { popIn(reduced, from = 0.6f).togetherWith(fadeOut(tween(120))) },
                    label = "auraLevel",
                ) { at ->
                    Text(
                        "LEVEL $at",
                        color = palette.onGlassMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                    )
                }
                Counted(aura) { counted.value }
                Text("aura", color = palette.onGlassMuted, fontSize = 13.sp)
            }
        }
        val next = AuraLevels.next(level)
        Text(
            text = "${AuraWords.count(AuraLevels.toNext(earned))} more earned for level ${level + 1}: ${next.title.lowercaseFirst()}",
            color = palette.onGlassMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/** "Blue flame" as it reads after a colon: "blue flame". "A day of Plus" keeps its capital P. */
private fun String.lowercaseFirst(): String = replaceFirstChar { it.lowercase() }

/**
 * The number in the ring. On its own so that, while it counts, it is the only thing composed
 * again on each frame. A screen reader is told the [balance] itself, not whichever step of the
 * count happens to be showing.
 */
@Composable
private fun Counted(balance: Int, value: () -> Float) {
    Text(
        text = AuraWords.count(value().roundToInt()),
        color = LocalPalette.current.onGlass,
        fontSize = 46.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1).sp,
        modifier = Modifier.clearAndSetSemantics { contentDescription = "${AuraWords.count(balance)} aura" },
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

// ------------------------------------------------------------------------------- what levels bring

/**
 * What each level brings, as a shelf of tiles to swipe along, from level two to the first day of
 * Plus: the ones reached marked as yours, the next with what is left to earn, the rest with the
 * level they come at. It starts with the last one reached in view. A flame reached is put on with
 * a tap; a colour set reached opens the wallpapers, where it now is.
 */
@Composable
private fun LevelRoad(
    settings: ZenithSettings,
    level: Int,
    earned: Int,
    onWear: (AuraFlame) -> Unit,
    onColours: () -> Unit,
) {
    val palette = LocalPalette.current
    val worn = AuraFlame.worn(settings)
    val road = remember { AuraLevels.rewards + AuraLevels.reward(AuraLevels.LAST_REWARD + 1)!! }
    // The last tile reached, and the next beside it.
    val start = (road.indexOfLast { it.level <= level }).coerceAtLeast(0)
    val shelf = rememberLazyListState(initialFirstVisibleItemIndex = (start - 1).coerceAtLeast(0))
    Group(rise = 2) {
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("What levels bring", color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("Level $level", color = palette.onGlassMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(
            state = shelf,
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(road, key = { _, reward -> reward.level }) { _, reward ->
                RewardTile(
                    reward = reward,
                    reached = reward.level <= level || (reward is AuraReward.Colours && AuraLevels.hasColours(settings, reward.set.id)),
                    next = reward.level == level + 1,
                    toGo = AuraLevels.threshold(reward.level) - earned,
                    worn = reward is AuraReward.Flame && reward.flame == worn,
                    onClick = {
                        when (reward) {
                            is AuraReward.Flame -> if (reward.level <= level) onWear(reward.flame)
                            is AuraReward.Colours -> if (AuraLevels.hasColours(settings, reward.set.id)) onColours()
                            is AuraReward.PlusDay -> Unit
                        }
                    },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Note(
            "Levels count all the aura you earn, so spending never costs you one. " +
                "After level ${AuraLevels.LAST_REWARD}, every level brings a day of Plus.",
            Modifier.padding(start = 12.dp, end = 12.dp, bottom = 14.dp),
        )
    }
}

@Composable
private fun RewardTile(reward: AuraReward, reached: Boolean, next: Boolean, toGo: Int, worn: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val ring by animateColorAsState(
        when {
            worn -> palette.accent
            next -> palette.onGlass.copy(alpha = 0.55f)
            else -> palette.onGlass.copy(alpha = 0.10f)
        },
        tween(220),
        label = "rewardRing",
    )
    Column(
        Modifier
            .width(98.dp)
            .pressBounce(0.95f)
            .clip(RoundedCornerShape(18.dp))
            .background(palette.onGlass.copy(alpha = if (reached) 0.07f else 0.035f))
            .border(if (worn || next) 1.5.dp else 1.dp, ring, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.graphicsLayer { alpha = if (reached || next) 1f else 0.45f }, contentAlignment = Alignment.Center) {
                RewardPicture(reward)
            }
            if (!reached) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            reward.label,
            color = palette.onGlass,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            when {
                worn -> "Wearing"
                reached && reward is AuraReward.Colours -> "In wallpapers"
                reached -> "Yours"
                next -> "${AuraWords.count(toGo.coerceAtLeast(0))} to go"
                reward is AuraReward.PlusDay -> "Level ${reward.level} on"
                else -> "Level ${reward.level}"
            },
            color = if (next || worn) palette.onGlass else palette.onGlassMuted,
            fontSize = 11.sp,
            fontWeight = if (next || worn) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

/**
 * A reward's name as a tile has room for it: a colour set by its name alone, since its picture is the
 * wallpaper in those colours and "Obsidian colours" is wider than a tile.
 */
private val AuraReward.label: String
    get() = when (this) {
        is AuraReward.Colours -> set.name
        else -> title
    }

/** What a reward looks like: its flame, its colours as one of Zenith's wallpapers, or Plus's star. */
@Composable
private fun RewardPicture(reward: AuraReward) {
    val palette = LocalPalette.current
    when (reward) {
        is AuraReward.Flame -> Box(
            Modifier.size(52.dp).clip(CircleShape).background(palette.onGlass.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            FlameGlyph(34.dp, flame = reward.flame)
        }
        is AuraReward.Colours -> {
            val dark = isSystemInDarkTheme()
            val set = remember(reward.set.id, dark) { Palettes.byId(reward.set.id).toned(palette.dark) }
            WallpaperImage(
                style = WallStyle.Iris,
                palette = set,
                widthPx = 120,
                modifier = Modifier.size(width = 38.dp, height = 56.dp).clip(RoundedCornerShape(9.dp)),
            )
        }
        is AuraReward.PlusDay -> Box(
            Modifier.size(52.dp).clip(CircleShape).background(palette.onGlass.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Star, contentDescription = null, tint = palette.onGlass, modifier = Modifier.size(26.dp))
        }
    }
}

/**
 * The flame you wear on the Aura widget and the morning strip: each colour reached is a disc to
 * tap, the one worn ringed; the rest wait, faint, with the level they come at.
 */
@Composable
private fun FlamePicker(worn: AuraFlame, level: Int, onWear: (AuraFlame) -> Unit) {
    val palette = LocalPalette.current
    Group(padding = 14.dp, rise = 3) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Your flame", color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(worn.label, color = palette.onGlassMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AuraFlame.entries.forEach { flame ->
                val reached = flame.level <= level
                val on = flame == worn
                val ring by animateColorAsState(if (on) palette.accent else Color.Transparent, tween(200), label = "flameRing")
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .border(2.dp, ring, CircleShape)
                            .padding(3.dp)
                            .pressBounce(0.9f)
                            .clip(CircleShape)
                            .background(palette.onGlass.copy(alpha = if (reached) 0.09f else 0.04f))
                            .clickable(enabled = reached) { onWear(flame) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.graphicsLayer { alpha = if (reached) 1f else 0.3f }) {
                            FlameGlyph(24.dp, flame = flame)
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        if (reached) flame.label else "Lv ${flame.level}",
                        color = if (on) palette.onGlass else palette.onGlassMuted,
                        fontSize = 10.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Note("It is the flame on your Aura widget, and on the line that slides down each morning. The week's ticks wear it too.")
    }
}

// ------------------------------------------------------------------------------- what aura buys

/**
 * What aura in hand buys: a day of Plus, and Plus's wallpapers one at a time, for good. A price is
 * asked twice, as the App Store asks: the first tap says what it will spend, the second spends it.
 * Why something cannot be bought is said under it, in words.
 */
@Composable
private fun AuraShopGroup(
    settings: ZenithSettings,
    plus: PlusStatus,
    onPlusDay: () -> String?,
    onDesign: (WallStyle) -> String?,
    onWallpapers: () -> Unit,
) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    var refusal by remember { mutableStateOf<String?>(null) }
    Group(rise = 4) {
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Spend your aura", color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("${AuraWords.count(settings.aura)} to spend", color = palette.onGlassMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(6.dp))

        // A day of Plus.
        val dayLine = when {
            plus.purchased -> "Plus is yours already"
            plus.inTrial -> "Plus is on for your free week"
            plus.onAura -> "On until ${AuraWords.until(plus.passUntil)}. Another goes on the end"
            else -> "Hidden apps, backup, the Notch and every wallpaper, for 24 hours"
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(palette.onGlass.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Star, contentDescription = null, tint = palette.onGlass, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("A day of Plus", color = palette.onGlass, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                // A note stands in from the edge of a group; here it lines up under its title.
                Note(dayLine, Modifier.offset(x = (-4).dp))
            }
            Spacer(Modifier.width(10.dp))
            PricePill(
                price = AuraShop.PLUS_DAY,
                enabled = !plus.purchased && !plus.inTrial,
                affordable = settings.aura >= AuraShop.PLUS_DAY,
                onBuy = { refusal = onPlusDay() },
            )
        }
        GroupDivider(inset = 14.dp)

        // Plus's wallpapers.
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Plus wallpapers", color = palette.onGlass, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(
                if (plus.lifetime) "All yours with Plus" else "Yours for good",
                color = palette.onGlassMuted,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(10.dp))
        val dark = palette.dark
        val colours = remember(settings.wallpaperPalette, settings.paletteId, dark) {
            Palettes.setById(settings.wallpaperPalette.ifEmpty { settings.paletteId }, settings).toned(dark)
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(AuraShop.designs, key = { _, style -> style.name }) { _, style ->
                val owned = style.name in settings.auraDesigns || plus.lifetime
                DesignTile(
                    style = style,
                    colours = colours,
                    owned = owned,
                    affordable = settings.aura >= AuraShop.DESIGN,
                    onBuy = { refusal = onDesign(style) },
                    onOpen = onWallpapers,
                )
            }
        }
        AnimatedVisibility(visible = refusal != null, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
            Note(refusal.orEmpty(), Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp))
        }
        Spacer(Modifier.height(12.dp))
        Note(
            "A day of Plus is ${AuraShop.PLUS_DAY} aura and a wallpaper ${AuraWords.count(AuraShop.DESIGN)}. " +
                "When a day runs out, Zenith is as it is after the free week; a wallpaper bought stays yours.",
            Modifier.padding(start = 12.dp, end = 12.dp, bottom = 14.dp),
        )
    }
    LaunchedEffect(refusal) {
        if (refusal != null) {
            delay(5_000)
            refusal = null
        }
    }
}

@Composable
private fun DesignTile(
    style: WallStyle,
    colours: com.gabriel.zenithui.ui.theme.ZenithPalette,
    owned: Boolean,
    affordable: Boolean,
    onBuy: () -> Unit,
    onOpen: () -> Unit,
) {
    val palette = LocalPalette.current
    Column(Modifier.width(86.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.56f)
                .pressBounce(0.95f)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, palette.onGlass.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                .clickable(onClick = onOpen),
        ) {
            WallpaperImage(style = style, palette = colours, widthPx = 180, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(6.dp))
        Text(style.label, color = palette.onGlass, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        if (owned) {
            Row(
                Modifier.height(30.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = palette.onGlass, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Yours", color = palette.onGlass, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            PricePill(price = AuraShop.DESIGN, enabled = true, affordable = affordable, onBuy = onBuy, compact = true)
        }
    }
}

/**
 * A price to tap, the App Store's way: the price in a pill; tapped, it becomes "Buy" in the blue for
 * a few seconds; tapped again, it buys. Short of aura, a tap goes straight through, to be told why
 * not. Off ([enabled] false), it is faint and does nothing.
 */
@Composable
private fun PricePill(price: Int, enabled: Boolean, affordable: Boolean, onBuy: () -> Unit, compact: Boolean = false) {
    val palette = LocalPalette.current
    val haptics = rememberHaptics()
    val reduced = LocalMotionLevel.current.reduced
    var asking by remember { mutableStateOf(false) }
    LaunchedEffect(asking) {
        if (asking) {
            delay(4_000)
            asking = false
        }
    }
    val fill by animateColorAsState(
        when {
            asking -> palette.accent
            else -> palette.onGlass.copy(alpha = if (enabled) 0.10f else 0.05f)
        },
        tween(180),
        label = "priceFill",
    )
    val ink = when {
        asking -> inkOnTint(palette.accent)
        !enabled || !affordable -> palette.onGlassMuted
        else -> palette.onGlass
    }
    Box(
        Modifier
            .height(30.dp)
            .pressBounce(0.92f)
            .clip(ZenithShapes.pill)
            .background(fill)
            .clickable(enabled = enabled) {
                when {
                    !affordable -> onBuy()
                    !asking -> {
                        haptics.tick()
                        asking = true
                    }
                    else -> {
                        asking = false
                        onBuy()
                    }
                }
            }
            .padding(horizontal = if (compact) 12.dp else 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = asking,
            transitionSpec = { popIn(reduced, from = 0.8f).togetherWith(fadeOut(tween(90))) },
            label = "priceWords",
        ) { buying ->
            Text(
                if (buying) "Buy" else AuraWords.count(price),
                color = ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
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
