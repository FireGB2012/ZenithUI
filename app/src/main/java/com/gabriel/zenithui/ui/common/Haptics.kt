package com.gabriel.zenithui.ui.common

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

/** Whether Zenith may tick under your finger: the Haptics switch in Settings. */
val LocalHapticsOn = staticCompositionLocalOf { true }

/**
 * The small tick a switch, a choice or a step of a slider gives as it changes. Does nothing when
 * haptics are switched off, so a control never has to ask.
 */
@Composable
fun rememberTick(): () -> Unit {
    val view = LocalView.current
    val on = LocalHapticsOn.current
    return remember(view, on) {
        { if (on) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
    }
}
