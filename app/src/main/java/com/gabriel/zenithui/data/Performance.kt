package com.gabriel.zenithui.data

/**
 * How much drawing Zenith asks of the phone.
 *
 * Full is all of Liquid Glass: the light on the glass and the icons follows the phone as it tilts,
 * the icons and widgets float a hair over the wallpaper, the lens splits a whisper of colour at its
 * rim, and the home screen frosts over behind a folder, Search and an opening app. Every one of
 * those is drawn again on every frame something moves, and with the phone in your hand something
 * always is.
 *
 * Lite keeps the glass, its lens, its rim of light and its shadow, and lets the rest go: the light
 * stays where it sits, up and to the left, nothing floats, the rim keeps its edge without the
 * colour, and what was frosted is dimmed instead. A phone lying in your hand then draws nothing
 * until something on it changes.
 *
 * Auto, where everybody starts, is Lite on a phone whose graphics would struggle with all of it,
 * known by its chip ([byChip]), or for a chip that is not known, by how its frames keep up
 * ([byFrames]); and Full on the rest.
 */
object Performance {
    const val AUTO = "auto"
    const val FULL = "full"
    const val LITE = "lite"

    val CHOICES = listOf(AUTO, FULL, LITE)

    /** What Auto has found this phone to be: not known yet, able to draw it all, or not. */
    enum class Verdict { UNKNOWN, FULL, LITE }

    /** [id] if it is one of the choices; Auto for anything else. */
    fun of(id: String): String = if (id in CHOICES) id else AUTO

    /** Whether Lite is on: as chosen, or with Auto, as this phone was found to be. Full until that is known. */
    fun isLite(choice: String, verdict: Verdict): Boolean = when (of(choice)) {
        FULL -> false
        LITE -> true
        else -> verdict == Verdict.LITE
    }

    /**
     * What the phone's chip says, before a frame has been drawn: [model] is Android's name for it
     * (Build.SOC_MODEL), [manufacturer] its maker's (Build.SOC_MANUFACTURER). Lite for a mid-range
     * or budget chip's graphics, the Galaxy A series' Exynos among them, and for a phone short of
     * memory; Full for a flagship's; and for a chip it does not know, nothing yet, so that its
     * frames decide.
     */
    fun byChip(model: String, manufacturer: String = "", totalRamMb: Long = 0L, lowRam: Boolean = false): Verdict {
        if (lowRam || totalRamMb in 1 until SHORT_OF_MEMORY_MB) return Verdict.LITE
        val chip = model.trim().lowercase()
        val maker = manufacturer.trim().lowercase()
        return when {
            maker.contains("spreadtrum") || maker.contains("unisoc") -> Verdict.LITE
            chip.isEmpty() || chip == "unknown" -> Verdict.UNKNOWN
            // Samsung's own: the flagships' Exynos 2100 to 2500, and every other one, the A series'.
            chip.startsWith("s5e99") || chip.startsWith("exynos2") -> Verdict.FULL
            chip.startsWith("s5e") || chip.startsWith("exynos") -> Verdict.LITE
            // Snapdragon: the 8 series and the newest 7s are flagship graphics; the 4, 6 and older 7s are not.
            chip.startsWith("sm8") || STRONG_SNAPDRAGON.any { chip.startsWith(it) } -> Verdict.FULL
            chip.startsWith("sm") -> Verdict.LITE
            // MediaTek: Dimensity 1200, 8000 and 9000 are; the 6000s, 7000s and Helios are not.
            chip.startsWith("mt") -> if (STRONG_DIMENSITY.any { chip.startsWith(it) }) Verdict.FULL else Verdict.LITE
            // Google's Tensor.
            chip.startsWith("gs") || chip.startsWith("tensor") || chip.startsWith("zuma") -> Verdict.FULL
            else -> Verdict.UNKNOWN
        }
    }

    /**
     * What the frames drawn so far say: [late] of [drawn] were late (see [isLate]). Nothing until
     * there have been [FRAMES_TO_JUDGE]; then Lite if more than [LATE_SHARE] of them were late.
     */
    fun byFrames(late: Int, drawn: Int): Verdict = when {
        drawn < FRAMES_TO_JUDGE -> Verdict.UNKNOWN
        late > drawn * LATE_SHARE -> Verdict.LITE
        else -> Verdict.FULL
    }

    /**
     * Whether a frame that took [durationNanos] to draw was late on a screen that shows a new one
     * every [intervalNanos]: half as long again as it is given, so it missed its turn and the one
     * after it showed nothing new.
     */
    fun isLate(durationNanos: Long, intervalNanos: Long): Boolean = durationNanos * 2 > intervalNanos * 3

    /** Whether the light on the glass follows the phone as it tilts: on glass, with Tilt shine on, and not in Lite. */
    fun lightTilts(tiltShine: Boolean, glass: Boolean, lite: Boolean): Boolean = tiltShine && glass && !lite

    /** Whether the icons, widgets and dock float a hair over the wallpaper with the tilt: not with movement reduced, nor in Lite. */
    fun floats(reduced: Boolean, lite: Boolean): Boolean = !reduced && !lite

    /**
     * How far, in dp, the home screen is blurred behind a folder, Search, an opening app and the
     * drawer: as far as the theme blurs, and in Lite not at all, so it is dimmed instead, as the
     * themes that blur nothing dim it.
     */
    fun blurBehind(theme: Float, lite: Boolean): Float = if (lite) 0f else theme

    /**
     * Whether a page turning, or the home screen going back behind the drawer, fades by drawing each
     * thing on it a little lighter, rather than drawing all of it into a picture first and fading
     * that: in Lite, where that picture, the size of the screen, was drawn again on every frame of
     * the swipe. The two only differ where things overlap, and only while they fade.
     */
    fun fadesInPlace(lite: Boolean): Boolean = lite

    /** The glass as you tuned it; in Lite without the whisper of colour at its rim, which looks at the wallpaper three times where once will do. */
    fun tuning(tuning: GlassTuning, lite: Boolean): GlassTuning = if (lite) tuning.copy(rainbow = 0f) else tuning

    /** How many frames Auto watches before it decides, after the first few of each start. */
    const val FRAMES_TO_JUDGE = 240

    /** More than this share of them late, and the phone is struggling. */
    const val LATE_SHARE = 0.25f

    /** Less memory than this, in MB, and a phone is short of it. */
    private const val SHORT_OF_MEMORY_MB = 4_000L

    /** Snapdragon 7+ Gen 2, 7 Gen 3, 7s Gen 3 and 7+ Gen 3. */
    private val STRONG_SNAPDRAGON = listOf("sm7475", "sm7550", "sm7635", "sm7675")

    /** Dimensity 1200, 8100, 8200, 8300, 9000, 9200, 9300 and 9400. */
    private val STRONG_DIMENSITY = listOf("mt6893", "mt6895", "mt6896", "mt6897", "mt6983", "mt6985", "mt6989", "mt6991")
}
