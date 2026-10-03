package com.gabriel.zenithui.ui.common

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalView

/** Whether Zenith may tick under your finger: the Haptics switch in Settings. */
val LocalHapticsOn = staticCompositionLocalOf { true }

/**
 * What the launcher feels like under a finger, in one place. Three things only: a tick when
 * something changes a step, a firmer press when something is picked up, and a confirmation when
 * something is put down or made. Nothing for page turns or sheets. Silent when the setting is off.
 */
class Haptics(private val view: View?, private val on: Boolean) {

    /** A switch or choice changed, a slider moved a step, the landing spot moved. */
    fun tick() = perform(HapticFeedbackConstants.CLOCK_TICK)

    /** Something was picked up. */
    fun pickUp() = perform(HapticFeedbackConstants.LONG_PRESS)

    /** Something was put down, or a folder was made. */
    fun confirm() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
    )

    private fun perform(kind: Int) {
        if (on) view?.performHapticFeedback(kind)
    }

    companion object {
        val Silent = Haptics(null, false)
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    val on = LocalHapticsOn.current
    return remember(view, on) { Haptics(view, on) }
}

/** The tick on its own, for controls that only ever do that. */
@Composable
fun rememberTick(): () -> Unit {
    val haptics = rememberHaptics()
    return remember(haptics) { { haptics.tick() } }
}

/**
 * Stands in for Compose's own haptics while the setting is off, so that every older call that
 * goes through `LocalHapticFeedback` falls silent too without each one having to ask.
 */
object NoHapticFeedback : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}
