package com.gabriel.zenithui.data

/**
 * How the names under a row of round Quick Toggles are set: at what share of their full size, and
 * on how many lines. Every name in the widget is set the same way, so the buttons above them stay
 * in a line.
 */
data class ToggleNames(val scale: Float, val lines: Int) {

    companion object {
        /** A name is never set smaller than this share of its full size: 10sp of 12sp. */
        const val SMALLEST = 10f / 12f

        /**
         * Names are set as large as the label under an app, which is larger than they used to be,
         * so a long one ("Do Not Disturb" in a two by two) no longer fits its cell on one line.
         * When the cell is tall enough to keep a button you can press above two lines
         * ([roomForTwo]) it goes onto a second; a single word wider than the cell shrinks the lot
         * until it fits. With no room for two lines the names shrink to stay on one, and past
         * [SMALLEST] the longest is cut short, as it always was.
         *
         * [widestName] and [widestWord] are widths at full size and [cell] the room across, all in
         * the same unit.
         */
        fun fit(widestName: Float, widestWord: Float, cell: Float, roomForTwo: Boolean): ToggleNames {
            if (cell <= 0f || widestName <= cell) return ToggleNames(1f, 1)
            if (roomForTwo) return ToggleNames((cell / widestWord.coerceAtLeast(1f)).coerceIn(SMALLEST, 1f), 2)
            return ToggleNames((cell / widestName).coerceIn(SMALLEST, 1f), 1)
        }
    }
}
