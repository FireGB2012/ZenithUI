package com.gabriel.zenithui.data

import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.PowerManager
import android.provider.Settings

/**
 * What a Quick Toggle's setting is now: [on] where it is a switch Android lets a launcher read,
 * null where it is not one or cannot be read; and [note], a word in place of On or Off where a
 * setting has more than two (how the phone rings).
 */
data class ToggleState(val on: Boolean?, val note: String? = null) {
    /** What a tile says under its name: its note, On or Off, or nothing when nothing is known. */
    val words: String? get() = note ?: on?.let { if (it) "On" else "Off" }

    companion object {
        val Unknown = ToggleState(null)

        fun of(on: Boolean?) = ToggleState(on)
    }
}

/**
 * Whether the settings behind the Quick Toggles are on, read as Android allows a launcher to read
 * them without asking for anything: the switches it keeps in its settings, and what its own
 * services will say to anyone. A launcher may not flip most of them, but it can say which are on,
 * so the widget lights them as Control Center does. Anything that cannot be read is left unknown,
 * never guessed.
 */
object ToggleStates {

    /** Wi-Fi as Android keeps it ("wifi_on"): on, on through airplane mode, off, or off for airplane mode. */
    fun wifi(setting: Int?): Boolean? = when (setting) {
        0, 3 -> false
        1, 2 -> true
        else -> null
    }

    /** Bluetooth as Android keeps it ("bluetooth_on"): off, on, or on through airplane mode. */
    fun bluetooth(setting: Int?): Boolean? = when (setting) {
        0 -> false
        1, 2 -> true
        else -> null
    }

    /** A setting kept as 0 for off and anything else for on. */
    fun flag(setting: Int?): Boolean? = setting?.let { it != 0 }

    /**
     * Do Not Disturb, from the interruption filter: everything let through is off; priority only,
     * alarms only or nothing at all is on. Unknown says nothing.
     */
    fun focus(filter: Int): Boolean? = when (filter) {
        NotificationManager.INTERRUPTION_FILTER_ALL -> false
        NotificationManager.INTERRUPTION_FILTER_PRIORITY,
        NotificationManager.INTERRUPTION_FILTER_NONE,
        NotificationManager.INTERRUPTION_FILTER_ALARMS -> true
        else -> null
    }

    /** How the phone rings, in a word. */
    fun ringer(mode: Int): String? = when (mode) {
        AudioManager.RINGER_MODE_NORMAL -> "Ring"
        AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
        AudioManager.RINGER_MODE_SILENT -> "Silent"
        else -> null
    }

    /** The control [id]'s setting now. The torch is told, since Zenith already watches it ([Torch]). */
    fun read(context: Context, id: String, torch: Boolean): ToggleState = runCatching {
        val resolver = context.contentResolver
        when (id) {
            "torch" -> ToggleState.of(torch)
            "wifi" -> ToggleState.of(wifi(global(resolver, Settings.Global.WIFI_ON)))
            "bluetooth" -> ToggleState.of(bluetoothAdapter(context) ?: bluetooth(global(resolver, Settings.Global.BLUETOOTH_ON)))
            "data" -> ToggleState.of(flag(global(resolver, MOBILE_DATA)))
            "airplane" -> ToggleState.of(flag(global(resolver, Settings.Global.AIRPLANE_MODE_ON)))
            "focus" -> ToggleState.of(context.getSystemService(NotificationManager::class.java)?.let { focus(it.currentInterruptionFilter) })
            "location" -> ToggleState.of(context.getSystemService(LocationManager::class.java)?.isLocationEnabled)
            "saver" -> ToggleState.of(context.getSystemService(PowerManager::class.java)?.isPowerSaveMode)
            "nfc" -> ToggleState.of(runCatching { NfcAdapter.getDefaultAdapter(context)?.isEnabled }.getOrNull())
            "volume" -> ToggleState(null, context.getSystemService(AudioManager::class.java)?.let { ringer(it.ringerMode) })
            "display" -> ToggleState(null, if (system(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) "Auto" else null)
            else -> ToggleState.Unknown
        }
    }.getOrDefault(ToggleState.Unknown)

    /** Android's broadcasts that say one of [ids] has changed. */
    fun broadcasts(ids: Collection<String>): List<String> = ids.flatMap { id ->
        when (id) {
            "wifi" -> listOf(WifiManager.WIFI_STATE_CHANGED_ACTION)
            "bluetooth" -> listOf(BluetoothAdapter.ACTION_STATE_CHANGED)
            "airplane" -> listOf(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            "focus" -> listOf(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            "location" -> listOf(LocationManager.MODE_CHANGED_ACTION)
            "saver" -> listOf(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            "nfc" -> listOf(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
            "volume" -> listOf(AudioManager.RINGER_MODE_CHANGED_ACTION)
            else -> emptyList()
        }
    }.distinct()

    /** The settings to watch for [ids], for the switches that are read from where Android keeps them. */
    fun watched(ids: Collection<String>): List<Uri> = ids.mapNotNull { id ->
        when (id) {
            "wifi" -> Settings.Global.getUriFor(Settings.Global.WIFI_ON)
            "bluetooth" -> Settings.Global.getUriFor(Settings.Global.BLUETOOTH_ON)
            "data" -> Settings.Global.getUriFor(MOBILE_DATA)
            "display" -> Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE)
            else -> null
        }
    }

    /** Mobile data's switch, which Android keeps but does not name for apps. */
    private const val MOBILE_DATA = "mobile_data"

    private fun global(resolver: ContentResolver, name: String): Int? =
        runCatching { Settings.Global.getInt(resolver, name) }.getOrNull()

    private fun system(resolver: ContentResolver, name: String): Int? =
        runCatching { Settings.System.getInt(resolver, name) }.getOrNull()

    /** Bluetooth's own word, which Android 12 and later give without asking; older ones keep it to apps that ask. */
    private fun bluetoothAdapter(context: Context): Boolean? =
        runCatching { context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled }.getOrNull()
}
