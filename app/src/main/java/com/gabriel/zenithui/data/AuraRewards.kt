package com.gabriel.zenithui.data

import androidx.compose.runtime.Immutable
import kotlin.math.sqrt

/**
 * Levels: what the aura you have earned is worth, as against the aura you have in hand to spend.
 *
 * A level is reached by earning, and spending never touches what was earned, so a level is never
 * lost. Level `n` is reached at `25 n (n - 1)` aura earned, so each level asks fifty more than the
 * one before: one more day at the top of the streak. There is no top level. Each level up to
 * [LAST_REWARD] brings something by itself, a flame for the Aura widget and a colour set for the
 * wallpapers by turns; after that each brings a day of Zenith Plus. See
 * `specs/2026-11-17-aura-levels-and-plus-design.md`.
 */
object AuraLevels {

    /** The last level that brings a flame or a colour set; every level after it brings a day of Plus. */
    const val LAST_REWARD = 12

    /** Aura earned to reach [level]: nothing for one, fifty for two, then fifty more each level than the last. */
    fun threshold(level: Int): Int {
        if (level <= 1) return 0
        return (25L * level * (level - 1)).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    /** The level [earned] aura has reached: one for nothing at all. */
    fun of(earned: Int): Int {
        if (earned < 50) return 1
        // The largest n with 25 n (n - 1) <= earned, from the root and then made exact.
        var level = ((1.0 + sqrt(1.0 + 4.0 * earned / 25.0)) / 2.0).toInt().coerceAtLeast(1)
        while (level > 1 && exact(level) > earned) level--
        while (exact(level + 1) <= earned) level++
        return level
    }

    /** Where [settings] stand, from what they have earned. */
    fun of(settings: ZenithSettings): Int = of(Aura.earned(settings))

    private fun exact(level: Int): Long = if (level <= 1) 0L else 25L * level * (level - 1)

    /** How far [earned] is through its level, from nought at the level to just short of one at the next. */
    fun progress(earned: Int): Float {
        val level = of(earned.coerceAtLeast(0))
        val from = exact(level)
        val to = exact(level + 1)
        return ((earned.coerceAtLeast(0) - from).toFloat() / (to - from).toFloat()).coerceIn(0f, 1f)
    }

    /** How much more aura must be earned to reach the next level. */
    fun toNext(earned: Int): Int {
        val level = of(earned.coerceAtLeast(0))
        return (exact(level + 1) - earned.coerceAtLeast(0)).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    }

    /** What the levels up to [LAST_REWARD] bring, a flame and a colour set by turns. */
    val rewards: List<AuraReward> = listOf(
        AuraReward.Flame(AuraFlame.Blue),
        AuraReward.Colours(AuraThemes.all[0], 3),
        AuraReward.Flame(AuraFlame.Purple),
        AuraReward.Colours(AuraThemes.all[1], 5),
        AuraReward.Flame(AuraFlame.Green),
        AuraReward.Colours(AuraThemes.all[2], 7),
        AuraReward.Flame(AuraFlame.Gold),
        AuraReward.Colours(AuraThemes.all[3], 9),
        AuraReward.Flame(AuraFlame.Rose),
        AuraReward.Colours(AuraThemes.all[4], 11),
        AuraReward.Flame(AuraFlame.White),
    )

    /** What reaching [level] brings; null for level one, which everybody starts at. */
    fun reward(level: Int): AuraReward? = when {
        level <= 1 -> null
        level > LAST_REWARD -> AuraReward.PlusDay(level)
        else -> rewards.firstOrNull { it.level == level }
    }

    /** What the level after [level] will bring. */
    fun next(level: Int): AuraReward = reward(level.coerceAtLeast(1) + 1)!!

    /** The level a flame is reached at. */
    fun levelOf(flame: AuraFlame): Int = flame.level

    /** The level an Aura colour set is reached at; null for a set that is not one of them. */
    fun levelOfColours(id: String): Int? =
        rewards.firstOrNull { it is AuraReward.Colours && it.set.id == id }?.level

    /** Whether the Aura colour set [id] is yours: reached, or bought with aura while there was a shop. */
    fun hasColours(settings: ZenithSettings, id: String): Boolean {
        val at = levelOfColours(id) ?: return false
        return id in settings.unlockedThemes || of(settings) >= at
    }

    /** The Aura colour sets that are yours, in the order they are reached. */
    fun colourSets(settings: ZenithSettings): List<String> =
        AuraThemes.all.map { it.id }.filter { hasColours(settings, it) }
}

/** One thing a level brings. */
@Immutable
sealed interface AuraReward {
    val level: Int

    /** A colour for the Aura widget's flame. */
    data class Flame(val flame: AuraFlame) : AuraReward {
        override val level: Int get() = flame.level
    }

    /** One of the five Aura sets, as colours for Zenith's painted wallpapers. */
    data class Colours(val set: AuraTheme, override val level: Int) : AuraReward

    /** Past the last of those: a day of Zenith Plus, on the house. */
    data class PlusDay(override val level: Int) : AuraReward
}

/** What a reward is called, as the morning strip and the Aura page say it. */
val AuraReward.title: String
    get() = when (this) {
        is AuraReward.Flame -> "${flame.label} flame"
        is AuraReward.Colours -> "${set.name} colours"
        is AuraReward.PlusDay -> "A day of Plus"
    }

/**
 * The colours the Aura widget's flame can be, each reached at a level: its body ([outer], running
 * up to [tip]), its heart, and the colour the week's ticks and dots wear beside it, a lighter one
 * for dark glass and a deeper one for pale ([markDark], [markLight]), as the widgets' hues are; the
 * deeper one stands out on a white card, or the widget would wear its ink there instead. Orange is
 * everybody's from the start; it is the flame the widget always had, and keeps its colours.
 */
enum class AuraFlame(
    val id: String,
    val label: String,
    val level: Int,
    val outer: Long,
    val tip: Long,
    val heart: Long,
    val markDark: Long,
    val markLight: Long,
) {
    Orange("orange", "Orange", 1, 0xFFFF9F0A, 0xFFFF6A1A, 0xFFFFD60A, 0xFFFF9F0A, 0xFFFF9500),
    Blue("blue", "Blue", 2, 0xFF0A84FF, 0xFF5E5CE6, 0xFF64D2FF, 0xFF0A84FF, 0xFF007AFF),
    Purple("purple", "Purple", 4, 0xFFBF5AF2, 0xFF8E44D6, 0xFFFF9BE5, 0xFFBF5AF2, 0xFFAF52DE),
    Green("green", "Green", 6, 0xFF30D158, 0xFF0BA36B, 0xFFD8FF6E, 0xFF30D158, 0xFF28A745),
    Gold("gold", "Gold", 8, 0xFFFFCC33, 0xFFE89A00, 0xFFFFF7D6, 0xFFFFD60A, 0xFFC28E00),
    Rose("rose", "Rose", 10, 0xFFFF375F, 0xFFD3004C, 0xFFFFB3C8, 0xFFFF375F, 0xFFFF2D55),
    White("white", "White", 12, 0xFFEAF2FF, 0xFFB4CCF5, 0xFFFFFFFF, 0xFFF2F2F7, 0xFF8E8E93);

    companion object {
        fun byId(id: String?): AuraFlame = entries.firstOrNull { it.id == id } ?: Orange

        /** The flame [settings] have chosen, once its level is reached; the orange one until then. */
        fun worn(settings: ZenithSettings): AuraFlame {
            val chosen = byId(settings.auraFlame)
            return if (AuraLevels.of(settings) >= chosen.level) chosen else Orange
        }

        /** The flames reached by [level], in the order they come. */
        fun reached(level: Int): List<AuraFlame> = entries.filter { it.level <= level }
    }
}
