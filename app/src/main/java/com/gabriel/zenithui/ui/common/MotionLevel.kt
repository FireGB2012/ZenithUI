package com.gabriel.zenithui.ui.common

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * How much of the launcher moves. Reduced keeps what tells you something — a thing fading in where
 * it now is — and drops what is only for pleasure: nothing flies in, jiggles or tilts, pages slide
 * plainly, and folders and previews fade instead of growing.
 */
enum class MotionLevel {
    Full,
    Reduced;

    val reduced: Boolean get() = this == Reduced

    companion object {
        /**
         * Reduced when the Animations setting says so, or when the phone's own "Remove animations"
         * is on ([systemAnimations] false), whatever the setting. Anything else is Full: a word
         * this version does not know never takes movement away from somebody who did not ask.
         */
        fun of(setting: String, systemAnimations: Boolean): MotionLevel =
            if (setting == "reduced" || !systemAnimations) Reduced else Full
    }
}

val LocalMotionLevel = staticCompositionLocalOf { MotionLevel.Full }
