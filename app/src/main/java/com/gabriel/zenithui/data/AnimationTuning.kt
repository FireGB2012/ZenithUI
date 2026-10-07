package com.gabriel.zenithui.data

import androidx.compose.runtime.Immutable
import org.json.JSONObject
import kotlin.math.roundToInt

/** The parts of opening and closing an app, in the order Settings shows them. */
enum class TuneGroup(val title: String, val note: String) {
    Open("Opening an app", "The card that grows out of the icon until it fills the screen."),
    Home("Home screen behind", "What the rest of the home screen does while the app opens, and undoes as you come back."),
    Close("Closing an app", "The home screen coming forward again, and the app's icon landing in its place."),
}

/** How a number is written out beside its slider. */
enum class TuneUnit { Plain, Ratio, Percent, Dp, Times }

/**
 * Every number the opening and closing of an app runs on. Each has a range it is kept inside, the
 * step one press of minus or plus moves it, and the value Zenith itself uses.
 *
 * Movement is on springs, as on an iPhone or a Galaxy: stiffness is how hard the spring pulls, so
 * how fast it goes; damping is how soon it comes to rest, where 1 stops dead and anything less
 * runs past and comes back. Sideways, up and down, and growth each have a spring of their own,
 * which is what lets the card take a curve instead of a straight line.
 */
enum class Tune(
    val id: String,
    val group: TuneGroup,
    val title: String,
    val min: Float,
    val max: Float,
    val step: Float,
    val unit: TuneUnit,
    /** Zenith's own value. */
    val own: Float,
    val note: String,
) {
    OpenXStiffness("openXStiffness", TuneGroup.Open, "Sideways stiffness", 40f, 1200f, 5f, TuneUnit.Plain, 330f, "How fast the card travels across."),
    OpenXDamping("openXDamping", TuneGroup.Open, "Sideways damping", 0.3f, 1.2f, 0.01f, TuneUnit.Ratio, 0.88f, "Under 1.00 it swings past the middle and back."),
    OpenYStiffness("openYStiffness", TuneGroup.Open, "Up and down stiffness", 40f, 1200f, 5f, TuneUnit.Plain, 260f, "How fast the card travels up or down."),
    OpenYDamping("openYDamping", TuneGroup.Open, "Up and down damping", 0.3f, 1.2f, 0.01f, TuneUnit.Ratio, 0.86f, "Under 1.00 it swings past the middle and back."),
    OpenSizeStiffness("openSizeStiffness", TuneGroup.Open, "Growth stiffness", 40f, 1200f, 5f, TuneUnit.Plain, 280f, "How fast the card grows to fill the screen."),
    OpenSizeDamping("openSizeDamping", TuneGroup.Open, "Growth damping", 0.3f, 1.2f, 0.01f, TuneUnit.Ratio, 0.9f, "Under 1.00 it swells a little past full size."),
    OpenArc("openArc", TuneGroup.Open, "Arc of the path", -80f, 80f, 1f, TuneUnit.Dp, 10f, "How far the card bows upward on its way. Below 0 it dips instead."),
    OpenLaunchAt("openLaunchAt", TuneGroup.Open, "App starts at", 0.1f, 0.95f, 0.01f, TuneUnit.Percent, 0.5f, "How far the card has grown when the app itself is asked to open."),
    OpenCorner("openCorner", TuneGroup.Open, "Corners at full screen", 0f, 60f, 1f, TuneUnit.Dp, 28f, "How round the card's corners are as it fills the screen."),
    OpenLogo("openLogo", TuneGroup.Open, "Logo at full size", 40f, 180f, 1f, TuneUnit.Dp, 96f, "How big the app's icon is in the middle of the full card."),
    OpenShadow("openShadow", TuneGroup.Open, "Shadow under the card", 0f, 1f, 0.01f, TuneUnit.Percent, 0.45f, "How dark the shadow is that the card casts on the home screen."),

    HomeScale("homeScale", TuneGroup.Home, "Home screen zoom", 0.7f, 1.4f, 0.005f, TuneUnit.Percent, 0.9f, "Under 100% it sinks back behind the app. Over 100% it rushes towards you."),
    HomePivot("homePivot", TuneGroup.Home, "Zoom towards the icon", 0f, 1f, 0.01f, TuneUnit.Percent, 0.4f, "0% zooms about the middle of the screen, 100% about the icon you tapped."),
    HomeDim("homeDim", TuneGroup.Home, "Dimming", 0f, 0.8f, 0.01f, TuneUnit.Percent, 0.25f, "How much the home screen fades behind the app."),
    HomeBlur("homeBlur", TuneGroup.Home, "Blur", 0f, 32f, 1f, TuneUnit.Dp, 8f, "How far out of focus the home screen goes."),
    IconPush("iconPush", TuneGroup.Home, "Icons pushed aside", 0f, 60f, 1f, TuneUnit.Dp, 10f, "How far the icons nearest the one you tapped move out of its way."),

    ReturnStiffness("returnStiffness", TuneGroup.Close, "Return stiffness", 40f, 1200f, 5f, TuneUnit.Plain, 300f, "How fast the home screen comes forward again."),
    ReturnDamping("returnDamping", TuneGroup.Close, "Return damping", 0.3f, 1.2f, 0.01f, TuneUnit.Ratio, 0.7f, "Under 1.00 it comes a little too far forward and settles."),
    CloseCard("closeCard", TuneGroup.Close, "Closing card", 0f, 1f, 0.01f, TuneUnit.Percent, 0f, "A card that shrinks back into the icon. Its size to start from; 0% is no card."),
    LandScale("landScale", TuneGroup.Close, "Icon lands from", 1f, 2f, 0.01f, TuneUnit.Times, 1.22f, "How large the app's icon is as it arrives back in its place."),
    LandStiffness("landStiffness", TuneGroup.Close, "Landing stiffness", 40f, 1200f, 5f, TuneUnit.Plain, 380f, "How fast the icon comes down to size."),
    LandDamping("landDamping", TuneGroup.Close, "Landing damping", 0.2f, 1.2f, 0.01f, TuneUnit.Ratio, 0.5f, "Under 1.00 the icon bounces as it lands."),
    LandFade("landFade", TuneGroup.Close, "Landing fade", 0f, 1f, 0.01f, TuneUnit.Percent, 0.65f, "How faint the icon is as it arrives."),
    Ripple("ripple", TuneGroup.Close, "Ripple round the icon", 0f, 40f, 1f, TuneUnit.Dp, 12f, "How far the icons round it are knocked back as it lands.");

    /** [value] on the nearest step, inside the range. */
    fun snap(value: Float): Float {
        val steps = ((value - min) / step).roundToInt()
        return (min + steps * step).coerceIn(min, max)
    }

    /** [value] as it is written beside the slider. */
    fun show(value: Float): String = when (unit) {
        TuneUnit.Plain -> "${value.roundToInt()}"
        TuneUnit.Ratio -> twoPlaces(value)
        TuneUnit.Percent -> when {
            this == CloseCard && value < 0.005f -> "Off"
            // The home screen's zoom moves half a percent at a time.
            step < 0.01f -> "${oneNoZero(value * 100f)}%"
            else -> "${(value * 100f).roundToInt()}%"
        }
        TuneUnit.Dp -> "${value.roundToInt()} dp"
        TuneUnit.Times -> "${twoPlaces(value)}×"
    }

    companion object {
        fun byId(id: String): Tune? = entries.firstOrNull { it.id == id }

        private fun twoPlaces(value: Float): String {
            val hundredths = (value * 100f).roundToInt()
            val sign = if (hundredths < 0) "-" else ""
            val whole = kotlin.math.abs(hundredths) / 100
            val part = kotlin.math.abs(hundredths) % 100
            return "$sign$whole.${part.toString().padStart(2, '0')}"
        }

        private fun oneNoZero(value: Float): String {
            val tenths = (value * 10f).roundToInt()
            return if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10}.${kotlin.math.abs(tenths % 10)}"
        }
    }
}

/**
 * A whole set of numbers under one name. Each says only where it differs from Zenith's own, so a
 * number added later is the same in every preset until one of them has a reason to change it.
 */
enum class AnimationPreset(
    val id: String,
    val label: String,
    val detail: String,
    /** Everything at once: 1 is as written, 2 twice as quick. */
    val speed: Float,
    private val changes: Map<Tune, Float>,
) {
    Zenith(
        "zenith", "Zenith",
        "Zenith's own: the card takes a slight curve, the home screen sinks back, and the icon lands with a bounce.",
        1f, emptyMap(),
    ),
    Fluid(
        "fluid", "Fluid",
        "Calm and unbroken. The home screen rushes towards you, into the icon you tapped, and nothing bounces.",
        1f,
        mapOf(
            Tune.OpenXStiffness to 220f, Tune.OpenXDamping to 0.94f,
            Tune.OpenYStiffness to 200f, Tune.OpenYDamping to 0.92f,
            Tune.OpenSizeStiffness to 210f, Tune.OpenSizeDamping to 0.95f,
            Tune.OpenArc to 0f, Tune.OpenLaunchAt to 0.45f, Tune.OpenShadow to 0.3f,
            Tune.HomeScale to 1.12f, Tune.HomePivot to 1f, Tune.HomeDim to 0.35f, Tune.HomeBlur to 6f,
            Tune.IconPush to 0f,
            Tune.ReturnStiffness to 220f, Tune.ReturnDamping to 0.88f,
            Tune.LandScale to 1.08f, Tune.LandStiffness to 300f, Tune.LandDamping to 0.75f, Tune.LandFade to 0.3f,
            Tune.Ripple to 0f,
        ),
    ),
    Bouncy(
        "bouncy", "Bouncy",
        "Springy and playful. The card swings a little past the middle, and the icon lands hard enough to knock its neighbours.",
        1f,
        mapOf(
            Tune.OpenXStiffness to 340f, Tune.OpenXDamping to 0.74f,
            Tune.OpenYStiffness to 300f, Tune.OpenYDamping to 0.72f,
            Tune.OpenSizeStiffness to 300f, Tune.OpenSizeDamping to 0.82f,
            Tune.OpenArc to 0f,
            Tune.HomeScale to 0.92f, Tune.HomePivot to 0f, Tune.HomeDim to 0.2f, Tune.HomeBlur to 10f,
            Tune.IconPush to 6f,
            Tune.ReturnStiffness to 260f, Tune.ReturnDamping to 0.6f,
            Tune.LandScale to 1.3f, Tune.LandStiffness to 420f, Tune.LandDamping to 0.42f, Tune.LandFade to 0.5f,
            Tune.Ripple to 18f,
        ),
    ),
    Dynamic(
        "dynamic", "Dynamic",
        "Quick and to the point: everything half as fast again, and it stops where it lands.",
        1.5f,
        mapOf(
            Tune.OpenXDamping to 0.96f, Tune.OpenYDamping to 0.96f, Tune.OpenSizeDamping to 0.98f,
            Tune.OpenArc to 0f, Tune.IconPush to 4f,
            Tune.ReturnDamping to 0.9f,
            Tune.LandScale to 1.1f, Tune.LandDamping to 0.8f, Tune.Ripple to 4f,
        ),
    ),
    Sweet(
        "sweet", "Sweet",
        "Slow and soft, for watching it happen: a long curve out, a deep blur, and a gentle landing.",
        0.7f,
        mapOf(
            Tune.OpenXDamping to 0.8f, Tune.OpenYDamping to 0.78f, Tune.OpenArc to 22f,
            Tune.HomeBlur to 14f, Tune.IconPush to 14f,
            Tune.ReturnDamping to 0.62f,
            Tune.LandDamping to 0.45f, Tune.Ripple to 16f,
        ),
    ),
    Classic(
        "classic", "Classic",
        "Opening and closing as they were before they could be tuned: one spring for everything, in a straight line.",
        1f,
        mapOf(
            Tune.OpenXStiffness to 300f, Tune.OpenXDamping to 0.8f,
            Tune.OpenYStiffness to 300f, Tune.OpenYDamping to 0.8f,
            Tune.OpenSizeStiffness to 300f, Tune.OpenSizeDamping to 0.8f,
            Tune.OpenArc to 0f, Tune.OpenLaunchAt to 0.55f, Tune.OpenShadow to 0f,
            Tune.HomePivot to 0f, Tune.IconPush to 0f,
            Tune.ReturnStiffness to 320f, Tune.ReturnDamping to 0.72f,
            Tune.Ripple to 0f,
        ),
    );

    fun value(tune: Tune): Float = changes[tune] ?: tune.own

    companion object {
        fun byId(id: String): AnimationPreset = entries.firstOrNull { it.id == id } ?: Zenith
    }
}

/**
 * How apps open and close on this phone: a preset, how fast it all runs, and whichever numbers
 * have been moved off the preset's by hand. Only what was changed is kept, so choosing a preset
 * again is one tap, and a preset that is improved later improves for everyone who left it alone.
 */
@Immutable
data class AnimationTuning(
    val preset: String = AnimationPreset.Zenith.id,
    /** Everything at once, or 0 to run at the preset's own speed. */
    val speed: Float = 0f,
    /** Numbers moved by hand, by [Tune.id]. */
    val changed: Map<String, Float> = emptyMap(),
) {
    private val base: AnimationPreset get() = AnimationPreset.byId(preset)

    /** The number in use: the one set by hand, or the preset's, kept inside its range. */
    operator fun get(tune: Tune): Float = (changed[tune.id] ?: base.value(tune)).coerceIn(tune.min, tune.max)

    /** How fast everything runs: 1 is as written. */
    val pace: Float get() = (if (speed > 0f) speed else base.speed).coerceIn(MIN_SPEED, MAX_SPEED)

    /**
     * A spring's stiffness at the pace in use. A spring four times as stiff moves twice as fast,
     * so the pace goes in squared.
     */
    fun stiffness(tune: Tune): Float = this[tune] * pace * pace

    /** Whether anything has been moved off the preset. */
    val custom: Boolean get() = changed.keys.any { Tune.byId(it) != null } || (speed > 0f && speed != base.speed)

    /** This with [tune] set to [value], on its nearest step. Set back to the preset's own, it is no longer kept. */
    fun with(tune: Tune, value: Float): AnimationTuning {
        val snapped = tune.snap(value)
        return if (kotlin.math.abs(snapped - tune.snap(base.value(tune))) < tune.step / 2f) {
            copy(changed = changed - tune.id)
        } else {
            copy(changed = changed + (tune.id to snapped))
        }
    }

    /** This with [tune] moved by [steps] of its own step: one press of minus or plus. */
    fun nudged(tune: Tune, steps: Int): AnimationTuning = with(tune, this[tune] + steps * tune.step)

    /** This at another pace. The preset's own pace is not kept. */
    fun paced(value: Float): AnimationTuning {
        val snapped = ((value / SPEED_STEP).roundToInt() * SPEED_STEP).coerceIn(MIN_SPEED, MAX_SPEED)
        return copy(speed = if (kotlin.math.abs(snapped - base.speed) < SPEED_STEP / 2f) 0f else snapped)
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("preset", preset)
        put("speed", speed.toDouble())
        put("changed", JSONObject().apply { changed.forEach { (id, value) -> put(id, value.toDouble()) } })
    }

    companion object {
        const val MIN_SPEED = 0.4f
        const val MAX_SPEED = 2.5f
        const val SPEED_STEP = 0.05f

        /** A preset with nothing moved. */
        fun of(preset: AnimationPreset): AnimationTuning = AnimationTuning(preset.id)

        /**
         * With movement reduced nothing swings, bounces, curves or zooms: the card grows straight
         * out and back, quickly, and the home screen stays where it is.
         */
        val Reduced: AnimationTuning = AnimationTuning(
            preset = AnimationPreset.Classic.id,
            changed = mapOf(
                Tune.OpenXStiffness.id to 520f, Tune.OpenXDamping.id to 1f,
                Tune.OpenYStiffness.id to 520f, Tune.OpenYDamping.id to 1f,
                Tune.OpenSizeStiffness.id to 520f, Tune.OpenSizeDamping.id to 1f,
                Tune.HomeScale.id to 1f, Tune.HomeBlur.id to 0f, Tune.HomeDim.id to 0.15f,
                Tune.ReturnStiffness.id to 520f, Tune.ReturnDamping.id to 1f,
                Tune.LandScale.id to 1f, Tune.LandDamping.id to 1f, Tune.LandFade.id to 0f,
            ),
        )

        /**
         * Read back leniently. A number outside its range is brought inside it. A preset or a
         * number this version has not heard of is kept as it was written, like every other
         * setting, so a backup from a newer Zenith passes through an older one whole; here it
         * simply has no effect. Anything that is not a tuning at all is Zenith's own.
         */
        fun fromJson(o: JSONObject?): AnimationTuning {
            if (o == null) return AnimationTuning()
            val changed = o.optJSONObject("changed")?.let { saved ->
                saved.keys().asSequence().mapNotNull { id ->
                    val value = saved.optDouble(id, Double.NaN).toFloat()
                    if (value.isNaN()) null else id to (Tune.byId(id)?.snap(value) ?: value)
                }.toMap()
            } ?: emptyMap()
            val speed = o.optDouble("speed", 0.0).toFloat().let { if (it.isNaN() || it <= 0f) 0f else it.coerceIn(MIN_SPEED, MAX_SPEED) }
            return AnimationTuning(o.optString("preset", AnimationPreset.Zenith.id), speed, changed)
        }
    }
}
