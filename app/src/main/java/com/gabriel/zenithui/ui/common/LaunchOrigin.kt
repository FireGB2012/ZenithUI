package com.gabriel.zenithui.ui.common

import android.os.SystemClock
import android.view.View
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.roundToInt

/**
 * Where on screen an app was last touched, so opening it can start from that icon. Icons record
 * themselves the moment a finger lands on them, and opening the app straight afterwards picks the
 * spot up. Only the latest touch is kept, and only briefly, so an app opened some other way — a
 * search result, say — simply opens.
 */
object LaunchOrigin {
    private const val FRESH_MS = 2_000L

    private var key: String? = null
    private var bounds: Rect = Rect.Zero
    private var touchedAt = 0L

    fun touched(key: String, bounds: Rect) {
        this.key = key
        this.bounds = bounds
        touchedAt = SystemClock.uptimeMillis()
    }

    /** Where [key]'s icon sits in Zenith's own coordinates, if it is the app that was just touched. */
    fun boundsFor(key: String): Rect? {
        val area = bounds
        if (key != this.key || area.isEmpty || SystemClock.uptimeMillis() - touchedAt > FRESH_MS) return null
        return area
    }

    /** [bounds] moved onto the whole screen, which is how Android wants to be told where a launch began. */
    fun toScreen(view: View, bounds: Rect): android.graphics.Rect {
        val onScreen = IntArray(2).also { view.getLocationOnScreen(it) }
        return android.graphics.Rect(
            bounds.left.roundToInt() + onScreen[0],
            bounds.top.roundToInt() + onScreen[1],
            bounds.right.roundToInt() + onScreen[0],
            bounds.bottom.roundToInt() + onScreen[1],
        )
    }
}

/** Records this app icon's place on screen whenever a finger touches it. Consumes nothing. */
fun Modifier.launchOrigin(key: String): Modifier = composed {
    val bounds = remember { arrayOf(Rect.Zero) }
    onGloballyPositioned { bounds[0] = it.boundsInRoot() }
        .pointerInput(key) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                LaunchOrigin.touched(key, bounds[0])
            }
        }
}
