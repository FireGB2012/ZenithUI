package com.gabriel.zenithui.data

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Zenith's lock helper: an accessibility service for one thing only, pressing the lock button for
 * you when a gesture asks. Android gives a home app no other way to turn the screen off and keep
 * the fingerprint working. It asks for no events and cannot see the screen: nothing on it is read,
 * kept or sent. It is off until switched on in Android's settings, and every other gesture works
 * without it.
 */
class LockService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = this
        _on.value = true
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        _on.value = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        _on.value = false
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        @Volatile private var instance: LockService? = null

        private val _on = MutableStateFlow(false)

        /** Whether the helper is running right now. */
        val on: StateFlow<Boolean> = _on.asStateFlow()

        /** Locks the phone. False when the helper is not on. */
        fun lock(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) == true

        /** The notification shade, for phones that will not let it be opened any other way. */
        fun notifications(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) == true

        fun quickSettings(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS) == true

        /** Whether it has been switched on in Android's settings, running or not. */
        fun enabled(context: Context): Boolean {
            val mine = ComponentName(context, LockService::class.java)
            val list = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
            return list.split(':').any { ComponentName.unflattenFromString(it) == mine }
        }

        /** Android's accessibility settings, where the helper is switched on. */
        fun openSettings(context: Context) {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}

/**
 * The notification shade and quick settings, pulled down for a gesture. Android has a call for it
 * that its own launchers use; where a phone refuses it, the lock helper's way is tried, if it is on.
 */
object SystemPanels {

    fun notifications(context: Context): Boolean = expand(context, "expandNotificationsPanel") || LockService.notifications()

    fun quickSettings(context: Context): Boolean = expand(context, "expandSettingsPanel") || LockService.quickSettings()

    private fun expand(context: Context, method: String): Boolean = runCatching {
        @Suppress("WrongConstant")
        val service = context.getSystemService("statusbar") ?: return false
        Class.forName("android.app.StatusBarManager").getMethod(method).invoke(service)
        true
    }.getOrDefault(false)
}
