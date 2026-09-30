package com.gabriel.zenithui.ui.theme

import android.graphics.Bitmap
import android.graphics.RenderNode
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The "No glass" widgets are drawn by this shader; if it failed to build they would quietly show
 * plain white shapes. This compiles it on a device, sets every uniform with and without a copy of
 * the wallpaper, and draws with the effect so the GPU has to accept it.
 */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 33)
@RequiresApi(33)
class GlassInkShaderTest {

    private val wallpaper = Bitmap.createBitmap(96, 216, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF3366CC.toInt()) }
    private val softened = Bitmap.createBitmap(24, 54, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF6688AA.toInt()) }

    @Test
    fun compilesAndTakesEveryUniformWithAndWithoutAWallpaper() {
        val shaders = listOf(
            GlassInkShader(wallpaper.asImageBitmap(), softened.asImageBitmap()),
            GlassInkShader(null, null),
        )
        for (ink in shaders) {
            for (halo in listOf(true, false)) {
                // Throws on a compile error or on any uniform the program does not declare.
                val effect = ink.effect(
                    size = Size(320f, 180f),
                    origin = Offset(40f, 900f),
                    screen = IntSize(1080, 2424),
                    wallpaperOffset = Offset(0.5f, 0.5f),
                    bevel = 11f,
                    light = Offset(-0.45f, -1f),
                    strength = 0.8f,
                    halo = halo,
                )
                assertNotNull(effect)
                val node = RenderNode("ink").apply {
                    setPosition(0, 0, 320, 180)
                    setRenderEffect(effect)
                }
                val canvas = node.beginRecording()
                try {
                    canvas.drawColor(0xFFFFFFFF.toInt())
                } finally {
                    node.endRecording()
                }
            }
        }
    }
}
