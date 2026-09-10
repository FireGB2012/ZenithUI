package com.gabriel.zenithui.data

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager

/** The activity behind a composable's context, or null if there somehow is not one. */
fun Context.findActivity(): Activity? {
    var context: Context? = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

/**
 * How fast the launcher draws: as fast as the screen can, for everybody.
 *
 * A launcher that stutters reads as a broken launcher, not as an advert for an upgrade, so this is
 * deliberately not something Plus unlocks. It is a request rather than a command — Android picks
 * the closest display mode it has, and a phone in battery saver or pinned to 60 Hz in system
 * settings will ignore it.
 */
object RefreshRate {

    /** The floor. Nothing here ever asks for less than this. */
    const val FLOOR_HZ = 60f

    /** The fastest mode this screen actually has, or [FLOOR_HZ] when it cannot be read. */
    fun maxHz(activity: Activity): Float = runCatching {
        val display = activity.display ?: return@runCatching FLOOR_HZ
        display.supportedModes.maxOfOrNull { it.refreshRate } ?: display.refreshRate
    }.getOrDefault(FLOOR_HZ).coerceAtLeast(FLOOR_HZ)

    /** Asks for the fastest mode the screen has. */
    fun applyFullSpeed(activity: Activity) {
        runCatching {
            val target = maxHz(activity)
            val window = activity.window ?: return@runCatching
            val params = window.attributes
            if (params.preferredRefreshRate == target) return@runCatching
            params.preferredRefreshRate = target
            // Older devices honour the mode id rather than the rate, so set both where we can.
            val mode = activity.display
                ?.supportedModes
                ?.filter { it.physicalWidth == activity.display?.mode?.physicalWidth }
                ?.minByOrNull { kotlin.math.abs(it.refreshRate - target) }
            if (mode != null) params.preferredDisplayModeId = mode.modeId
            window.attributes = params
        }
    }

    /** Removes the request entirely, letting the system do whatever it likes. */
    fun clear(activity: Activity) {
        runCatching {
            val window = activity.window ?: return@runCatching
            val params: WindowManager.LayoutParams = window.attributes
            params.preferredRefreshRate = 0f
            params.preferredDisplayModeId = 0
            window.attributes = params
        }
    }
}
