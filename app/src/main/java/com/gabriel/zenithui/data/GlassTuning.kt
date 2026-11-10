package com.gabriel.zenithui.data

import androidx.compose.runtime.Immutable
import org.json.JSONObject

/**
 * The glass made your own (Zenith Plus, Wallpaper & theme → Glass studio): how far it bends, how
 * frosted it is, how bright its rim, how much light splits into colour at its edge, and where the
 * light comes from. The defaults are the glass as Zenith makes it and change nothing; only what is
 * changed is saved. [tint] is the old "Theme in the glass", kept so old settings, Looks and backups
 * read, and ignored.
 */
@Immutable
data class GlassTuning(
    /** How far the glass bends what is behind it: 1 as made, 0 flat, 2 twice as far. */
    val bend: Float = 1f,
    /** Added to how soft the picture is through the face: −1 clearer, +1 more frosted. */
    val frost: Float = 0f,
    /** How brightly the rim catches the light and throws back what is round it: 1 as made. */
    val rim: Float = 1f,
    /** How far light splits into colour at the rim of clear glass: 1 as made. */
    val rainbow: Float = 1f,
    /**
     * The old "Theme in the glass": how much more of the theme's colour was in the veil, 0 to 0.4.
     * There are no themes now, so nothing reads it into the glass; it is still read and saved, so
     * old settings, Looks and backups come back as they went, and its key is never given another
     * meaning.
     */
    val tint: Float = 0f,
    /** Where the light comes from, in degrees clockwise from the right; null is Zenith's, up and to the left. */
    val lightAngle: Float? = null,
    /** Whether the light swings the other way as the phone tips, with Tilt shine on. */
    val lightTilt: Boolean = true,
) {
    val isDefault: Boolean get() = this == GlassTuning()

    fun toJson(): JSONObject = JSONObject().apply {
        val d = GlassTuning()
        if (bend != d.bend) put("bend", bend.toDouble())
        if (frost != d.frost) put("frost", frost.toDouble())
        if (rim != d.rim) put("rim", rim.toDouble())
        if (rainbow != d.rainbow) put("rainbow", rainbow.toDouble())
        if (tint != d.tint) put("tint", tint.toDouble())
        lightAngle?.let { put("light", it.toDouble()) }
        if (lightTilt != d.lightTilt) put("tilt", lightTilt)
    }

    companion object {
        val BEND = 0f..2f
        val FROST = -1f..1f
        val RIM = 0f..2f
        val RAINBOW = 0f..3f
        val TINT = 0f..0.4f

        /** Zenith's own light, as an angle: up and to the left, where [lightAngle] null puts it. */
        const val ZENITH_LIGHT = 245.77f

        fun fromJson(o: JSONObject?): GlassTuning {
            if (o == null) return GlassTuning()
            val d = GlassTuning()
            fun read(key: String, fallback: Float, range: ClosedFloatingPointRange<Float>) =
                o.optDouble(key, fallback.toDouble()).toFloat().takeIf { !it.isNaN() }?.coerceIn(range) ?: fallback
            return GlassTuning(
                bend = read("bend", d.bend, BEND),
                frost = read("frost", d.frost, FROST),
                rim = read("rim", d.rim, RIM),
                rainbow = read("rainbow", d.rainbow, RAINBOW),
                tint = read("tint", d.tint, TINT),
                lightAngle = if (o.has("light")) o.optDouble("light").toFloat().takeIf { !it.isNaN() }?.let { ((it % 360f) + 360f) % 360f } else null,
                lightTilt = o.optBoolean("tilt", d.lightTilt),
            )
        }
    }
}
