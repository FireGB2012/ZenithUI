package com.gabriel.zenithui.data

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import kotlin.random.Random

/**
 * Aura: the points you earn for turning up, and the themes you spend them on.
 *
 * It is entirely local. There is no account, no server and nothing sent anywhere — which is the
 * only way it can honestly sit inside a launcher that promises nothing leaves your phone. That
 * also sets the limits: a streak lives and dies with the install, and a referral can only reward
 * the person typing the code, never the person who gave it out.
 */
object Aura {

    /** What one day of showing up is worth, before the streak bonus. */
    const val BASE_DAILY = 10

    /** Each further day in a row adds this, up to [MAX_DAILY]. */
    const val STREAK_STEP = 5
    const val MAX_DAILY = 50

    /** A one-off, for using someone else's code. */
    const val REFERRAL_REWARD = 150

    /** Points for a given day of a streak. Day one is [BASE_DAILY]. */
    fun dailyReward(streak: Int): Int =
        (BASE_DAILY + (streak - 1).coerceAtLeast(0) * STREAK_STEP).coerceAtMost(MAX_DAILY)

    /** Codes are six characters, no vowels, so nothing accidentally spells anything. */
    private const val ALPHABET = "BCDFGHJKLMNPQRSTVWXZ23456789"

    fun newReferralCode(): String =
        (1..6).map { ALPHABET[Random.nextInt(ALPHABET.length)] }.joinToString("")

    fun isWellFormedCode(code: String): Boolean {
        val cleaned = code.uppercase().filter { it.isLetterOrDigit() }
        return cleaned.length == 6 && cleaned.all { it in ALPHABET }
    }

    fun cleanCode(code: String): String = code.uppercase().filter { it.isLetterOrDigit() }.take(6)

    /** Today, as a day number. Local time, because a streak is about your days, not UTC's. */
    fun today(): Long = LocalDate.now().toEpochDay()
}

/** What happened when the launcher checked in for the day. */
@Immutable
sealed interface CheckIn {
    /** Already counted today. Nothing to say. */
    data object AlreadyDone : CheckIn

    /** Another day in a row. */
    data class Continued(val streak: Int, val awarded: Int) : CheckIn

    /**
     * A day was missed. Deliberately worth nothing: the streak resets, today earns nothing, and
     * tomorrow starts again at day one. Saying so plainly beats pretending it did not happen.
     */
    data class Broken(val previousStreak: Int) : CheckIn
}

/**
 * The five themes aura buys, in the order they should be offered.
 *
 * They are priced so the cheapest is about a week of turning up and the dearest is a month or so.
 * More can be added here later without touching anything else.
 */
@Immutable
data class AuraTheme(
    val id: String,
    val name: String,
    val tagline: String,
    val price: Int,
)

object AuraThemes {
    val all = listOf(
        AuraTheme("ember", "Ember", "Banked coals under dark iron", 150),
        AuraTheme("tide", "Tide", "Deep water with a green shore", 250),
        AuraTheme("orchid", "Orchid", "Violet bloom on near-black", 350),
        AuraTheme("solstice", "Solstice", "Low sun, long gold shadows", 500),
        AuraTheme("obsidian", "Obsidian", "Black glass with a gold rim", 750),
    )

    fun byId(id: String): AuraTheme? = all.firstOrNull { it.id == id }

    fun isAuraTheme(paletteId: String): Boolean = all.any { it.id == paletteId }
}
