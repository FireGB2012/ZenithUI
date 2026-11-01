package com.gabriel.zenithui.data

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject

/** The orders the drawer can list apps in. */
object DrawerSort {
    const val AZ = "az"
    const val USED = "used"
    const val NEW = "new"
    const val COLOUR = "colour"

    val ids = listOf(AZ, USED, NEW, COLOUR)

    fun of(id: String): String = if (id in ids) id else AZ

    fun label(id: String): String = when (of(id)) {
        USED -> "Most used"
        NEW -> "Newest"
        COLOUR -> "By colour"
        else -> "A to Z"
    }

    /** The name on a button of the choice in Settings, where four have to fit side by side. */
    fun short(id: String): String = when (of(id)) {
        USED -> "Most used"
        NEW -> "Newest"
        COLOUR -> "Colour"
        else -> "A–Z"
    }

    fun detail(id: String): String = when (of(id)) {
        USED -> "The apps you open most at the top, the rest A to Z."
        NEW -> "The apps you added last at the top; those that came with the phone after them."
        COLOUR -> "Round the colour wheel by each icon's main colour, the greys at the end."
        else -> "Every app by its name."
    }

    /**
     * [apps], already A to Z, in the order [mode] asks for. Ties keep the A to Z order, so the list
     * never jumps about between two apps that are alike.
     */
    fun sort(
        apps: List<AppInfo>,
        mode: String,
        counts: Map<String, Int> = emptyMap(),
        installedAt: (String) -> Long = { 0L },
        colours: Map<String, IconColour> = emptyMap(),
    ): List<AppInfo> = when (of(mode)) {
        USED -> apps.sortedByDescending { counts[it.key] ?: 0 }
        NEW -> apps.sortedWith(compareByDescending<AppInfo> { if (it.system) 0L else installedAt(it.key) })
        COLOUR -> apps.sortedWith(compareBy({ colours[it.key]?.band() ?: Int.MAX_VALUE }, { colours[it.key]?.lightness ?: 0f }))
        else -> apps
    }

    /** [counts] with one more opening of [key], kept to the apps that matter so it never grows for ever. */
    fun counted(counts: Map<String, Int>, key: String): Map<String, Int> {
        val next = counts + (key to (counts[key] ?: 0) + 1)
        return if (next.size <= MOST_COUNTED) next else next.entries.sortedByDescending { it.value }.take(MOST_COUNTED).associate { it.key to it.value }
    }

    private const val MOST_COUNTED = 120
}

/** An icon's main colour, as the drawer sorts by it: its hue, how coloured it is, how light. */
@Immutable
data class IconColour(val hue: Float, val saturation: Float, val lightness: Float) {
    /**
     * Which of twelve bands of the colour wheel it falls in, from red round to violet; any icon
     * with little colour in it — white, grey, black — comes after them all.
     */
    fun band(): Int = if (saturation < 0.18f || lightness < 0.12f || lightness > 0.94f) 12 else ((hue % 360f) / 30f).toInt().coerceIn(0, 11)

    companion object {
        /** From red, green and blue, each 0 to 255. */
        fun of(r: Int, g: Int, b: Int): IconColour {
            val rf = r / 255f
            val gf = g / 255f
            val bf = b / 255f
            val max = maxOf(rf, gf, bf)
            val min = minOf(rf, gf, bf)
            val light = (max + min) / 2f
            val chroma = max - min
            if (chroma < 0.0001f) return IconColour(0f, 0f, light)
            val sat = chroma / (1f - kotlin.math.abs(2f * light - 1f)).coerceAtLeast(0.0001f)
            val hue = when (max) {
                rf -> 60f * (((gf - bf) / chroma) % 6f)
                gf -> 60f * ((bf - rf) / chroma + 2f)
                else -> 60f * ((rf - gf) / chroma + 4f)
            }.let { if (it < 0f) it + 360f else it }
            return IconColour(hue, sat.coerceIn(0f, 1f), light)
        }
    }
}

/** A tab of the drawer you made yourself: a name and the apps in it, in the drawer's order. */
@Immutable
data class DrawerTab(val name: String, val keys: List<String>) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("keys", JSONArray(keys))
    }

    companion object {
        fun fromJson(o: JSONObject): DrawerTab? {
            val name = o.optString("name").trim()
            if (name.isEmpty()) return null
            return DrawerTab(name, o.optJSONArray("keys").toStringList())
        }

        /** As many as fit along the drawer's top without it becoming a list of its own. */
        const val MOST = 8
    }
}

/**
 * The work profile's apps are kept apart from the rest: their keys start `work:` and they are
 * only ever in the drawer's Work tab, opened in the work profile.
 */
object WorkKeys {
    const val PREFIX = "work:"
    fun of(appKey: String): String = "$PREFIX$appKey"
    fun isWork(key: String): Boolean = key.startsWith(PREFIX)
    fun appKey(key: String): String = key.removePrefix(PREFIX)
}
