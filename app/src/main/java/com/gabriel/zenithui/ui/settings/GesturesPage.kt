package com.gabriel.zenithui.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowDown
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pinch
import androidx.compose.material.icons.rounded.SwipeDown
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.GestureActions
import com.gabriel.zenithui.data.Gestures
import com.gabriel.zenithui.data.LockService
import com.gabriel.zenithui.data.ShortcutKeys
import com.gabriel.zenithui.data.ZenithSettings

/**
 * Settings → Gestures: every gesture on the home screen and what it does now. A row opens the
 * list of what it could do instead. A gesture set to lock the phone says when the lock helper it
 * needs is still to be switched on.
 */
@Composable
internal fun GestureOptions(settings: ZenithSettings, preview: SettingsPreview, actions: SettingsActions) {
    val helperOn by LockService.on.collectAsState()
    Group(rise = 0) {
        Gestures.ids.forEachIndexed { index, id ->
            if (index > 0) GroupDivider()
            Spot("gesture_$id") {
                val action = Gestures.actionFor(id, settings.gestures)
                ActionRow(
                    icon = gestureIcon(id),
                    title = Gestures.label(id),
                    subtitle = GestureActions.label(action) { key -> nameOf(key, preview.appsByKey) },
                    status = if (GestureActions.needsHelper(action) && !helperOn) "Set up" else null,
                    onClick = { actions.gesture(id) },
                )
            }
        }
    }
    Group(rise = 1) {
        Spot("lockHelper") {
            ActionRow(
                icon = if (helperOn) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                title = "Lock helper",
                subtitle = if (helperOn) "On. A gesture can lock the phone" else "Off. Only needed to lock the phone with a gesture",
                status = if (helperOn) "On" else null,
                onClick = actions.lockHelper,
            )
        }
    }
    Note(
        "Swipe up always opens your apps, and holding an empty spot always opens Customise, whatever is chosen here.",
        Modifier.riseIn(2).padding(horizontal = 14.dp),
    )
}

/** What an app or shortcut a gesture opens is called. */
internal fun nameOf(key: String, apps: Map<String, AppInfo>): String? {
    ShortcutKeys.parse(key)?.let { (pkg, _) ->
        val app = apps.values.firstOrNull { it.packageName == pkg }?.label ?: return "A shortcut"
        return "A shortcut of $app"
    }
    return apps[key]?.label
}

private fun gestureIcon(id: String): ImageVector = when (id) {
    Gestures.SWIPE_DOWN -> Icons.Rounded.SwipeDown
    Gestures.TWO_DOWN -> Icons.Rounded.KeyboardDoubleArrowDown
    Gestures.TWO_UP -> Icons.Rounded.KeyboardDoubleArrowUp
    Gestures.DOUBLE_TAP -> Icons.Rounded.TouchApp
    Gestures.PINCH_IN -> Icons.Rounded.Pinch
    Gestures.PINCH_OUT -> Icons.Rounded.ZoomOutMap
    else -> Icons.Rounded.Home
}
