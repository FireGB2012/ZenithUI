package com.gabriel.zenithui.data

import androidx.compose.runtime.Immutable
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * What aura in hand buys, and how aura is earned: a day of Zenith Plus, or one of Plus's
 * wallpapers for good. Rules only; nothing here is saved or shown. See
 * `specs/2026-11-17-aura-levels-and-plus-design.md`.
 */
object AuraShop {

    /** A day of everything Plus has. Five days at the top of the streak. */
    const val PLUS_DAY = 250

    /** One of Plus's wallpapers, kept for good. About three weeks of turning up. */
    const val DESIGN = 1_000

    const val DAY_MS = 86_400_000L

    /** The wallpapers aura can buy: the designs made for Plus, in the order the picker has them. */
    val designs: List<WallStyle> = WallStyle.entries.filter { it.plus }

    /**
     * Earns [points]: they go into the aura in hand and into what was earned, and a level reached
     * past [AuraLevels.LAST_REWARD] adds a day of Plus, one for each such level, to the end of any
     * day already running. Says which levels were reached on the way.
     */
    fun earn(settings: ZenithSettings, points: Int, now: Long): Earned {
        if (points <= 0) return Earned(settings, emptyList())
        val before = Aura.earned(settings)
        val after = (before.toLong() + points).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val reached = ((AuraLevels.of(before) + 1)..AuraLevels.of(after)).toList()
        val days = reached.count { it > AuraLevels.LAST_REWARD }
        val pass = if (days == 0) settings.plusPassUntil else maxOf(settings.plusPassUntil, now) + days * DAY_MS
        return Earned(
            settings.copy(
                aura = (settings.aura.toLong() + points).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                auraEarned = after,
                plusPassUntil = pass,
            ),
            reached,
        )
    }

    /**
     * A day of Plus: everything it has, for a day from now, or a day more on the end of one still
     * running. Not sold to somebody who has Plus already, which it would be spent on nothing.
     */
    fun buyPlusDay(settings: ZenithSettings, plus: PlusStatus, now: Long): AuraSpend = when {
        plus.purchased -> AuraSpend.Refused("Plus is yours already.")
        plus.trialDaysLeft > 0 -> AuraSpend.Refused("Plus is on for your free week.")
        settings.aura < PLUS_DAY -> short(settings, PLUS_DAY)
        else -> AuraSpend.Done(
            settings.copy(
                aura = settings.aura - PLUS_DAY,
                plusPassUntil = maxOf(settings.plusPassUntil, now) + DAY_MS,
            )
        )
    }

    /**
     * One of Plus's wallpapers, for good. Not to somebody who has Plus for good, who has them all;
     * a subscriber may, since a subscription can end and a wallpaper bought does not.
     */
    fun buyDesign(settings: ZenithSettings, style: WallStyle, plus: PlusStatus): AuraSpend = when {
        !style.plus -> AuraSpend.Refused("That one is free.")
        style.name in settings.auraDesigns -> AuraSpend.Refused("It is yours already.")
        plus.lifetime -> AuraSpend.Refused("Plus is yours for good, and has it.")
        settings.aura < DESIGN -> short(settings, DESIGN)
        else -> AuraSpend.Done(
            settings.copy(
                aura = settings.aura - DESIGN,
                auraDesigns = settings.auraDesigns + style.name,
            )
        )
    }

    /** Whether [style] may be set: a free design, any design while Plus is on, or one bought with aura. */
    fun ownsDesign(settings: ZenithSettings, plusActive: Boolean, style: WallStyle): Boolean =
        !style.plus || plusActive || style.name in settings.auraDesigns

    /** When a day of Plus ends, if it is still to come; nought once it has run out, or if there is none. */
    fun passUntil(until: Long, now: Long): Long = if (until > now) until else 0L

    /**
     * Settings coming back from a backup ([restored]) onto [phone]: the day of Plus left is the
     * phone's, never the file's, as the free week and the purchase are. What is yours (the aura,
     * what was earned, the flame and the wallpapers bought) comes with the backup.
     */
    fun restoring(restored: ZenithSettings, phone: ZenithSettings): ZenithSettings =
        restored.copy(plusPassUntil = phone.plusPassUntil)

    private fun short(settings: ZenithSettings, price: Int): AuraSpend.Refused =
        AuraSpend.Refused("You need ${price - settings.aura.coerceAtLeast(0)} more aura.")
}

/** What earning did: the settings after, and the levels reached on the way, lowest first. */
@Immutable
data class Earned(val settings: ZenithSettings, val reached: List<Int>)

/** What spending did: the settings after, or why nothing was bought, in words for the person. */
@Immutable
sealed interface AuraSpend {
    data class Done(val settings: ZenithSettings) : AuraSpend
    data class Refused(val why: String) : AuraSpend
}

/** Aura's numbers and times, as the pages say them. */
object AuraWords {
    /** A count of aura with its thousands marked, as the phone's language marks them: "1,240". */
    fun count(points: Int): String = NumberFormat.getIntegerInstance().format(points)

    /**
     * When a day of Plus ends, as somebody would say it: the time alone today, "tomorrow" after the
     * time, or the day and the date further off.
     */
    fun until(at: Long, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): String {
        val end = Instant.ofEpochMilli(at).atZone(zone)
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val time = end.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        return when (end.toLocalDate()) {
            today -> time
            today.plusDays(1) -> "$time tomorrow"
            else -> end.format(DateTimeFormatter.ofPattern("EEE d MMM")) + ", " + time
        }
    }
}
