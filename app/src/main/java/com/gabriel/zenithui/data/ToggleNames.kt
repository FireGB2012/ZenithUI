package com.gabriel.zenithui.data

/**
 * How the names on a Quick Toggles widget's tiles are set: which of them take their short form
 * ("Focus" for Do Not Disturb, "Airplane" for Airplane mode), and at what share of their full size.
 * Every tile in a widget is set at the same size, so a row of them reads as one.
 */
data class ToggleNames(
    /** For each tile in turn, whether its name is the short one. */
    val short: List<Boolean>,
    val scale: Float,
) {
    /** A name's width at full size, in full and in its short form. */
    data class Widths(val full: Float, val short: Float)

    companion object {
        /** A name is never set smaller than this share of its full size: 12sp of 14sp. */
        const val SMALLEST = 12f / 14f

        /**
         * Each name in full where it fits [room], the space beside a tile's symbol, and in its short
         * form where it does not. If even that is too wide, every name shrinks together until it
         * fits, but never past [SMALLEST]; past that, the longest is cut short. [names] and [room]
         * are in the same unit.
         */
        fun fit(names: List<Widths>, room: Float): ToggleNames {
            if (room <= 0f) return ToggleNames(names.map { false }, 1f)
            val short = names.map { it.full > room }
            val widest = names.indices.maxOfOrNull { if (short[it]) names[it].short else names[it].full } ?: 0f
            val scale = if (widest <= room) 1f else (room / widest).coerceIn(SMALLEST, 1f)
            return ToggleNames(short, scale)
        }
    }
}
