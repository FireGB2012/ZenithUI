package com.gabriel.zenithui.data

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The flashlight, for the Quick Toggles widget and a gesture alike. Whether it is on is Android's
 * word, not Zenith's guess, so the widget is right after the torch was turned on from the quick
 * settings, or by a gesture, or off again by another app.
 */
object Torch {
    private val _on = MutableStateFlow(false)
    val on: StateFlow<Boolean> = _on.asStateFlow()

    @Volatile private var camera: String? = null
    @Volatile private var watching = false

    private fun manager(context: Context): CameraManager? = context.applicationContext.getSystemService(CameraManager::class.java)

    /** The camera with a flash, if the phone has one. */
    private fun flashCamera(manager: CameraManager): String? = camera ?: runCatching {
        manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }.getOrNull()?.also { camera = it }

    /** Starts listening for the torch being turned on or off, by anyone. Safe to call often. */
    fun watch(context: Context) {
        if (watching) return
        val manager = manager(context) ?: return
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (cameraId == flashCamera(manager)) _on.value = enabled
            }
        }
        watching = runCatching { manager.registerTorchCallback(callback, Handler(Looper.getMainLooper())) }.isSuccess
    }

    /** Turns the torch the other way. False when the phone has none, or the camera is in use. */
    fun toggle(context: Context): Boolean {
        val manager = manager(context) ?: return false
        watch(context)
        val id = flashCamera(manager) ?: return false
        val next = !_on.value
        return runCatching { manager.setTorchMode(id, next) }.onSuccess { _on.value = next }.isSuccess
    }
}
