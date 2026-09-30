package com.gabriel.zenithui.ui.theme

import android.graphics.Bitmap
import android.graphics.RenderNode
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The glass falls back quietly when its shader cannot be built, so a typo in the AGSL or a
 * misnamed uniform would ship as "glass that never bends" with no crash to notice. This compiles
 * the program on a real device and sets every uniform for each kind of outline Zenith draws.
 */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 33)
@RequiresApi(33)
class LiquidGlassShaderTest {

    private val wallpaper = Bitmap.createBitmap(96, 216, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF3366CC.toInt()) }
    private val softened = Bitmap.createBitmap(24, 54, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF6688AA.toInt()) }

    @Test
    fun compilesAndTakesEveryUniformForEveryOutline() {
        val lens = LiquidGlassShader(wallpaper.asImageBitmap(), softened.asImageBitmap())
        val size = Size(240f, 96f)
        val outlines = listOf(
            Outline.Rounded(RoundRect(0f, 0f, size.width, size.height, CornerRadius(48f, 48f))),
            Outline.Generic(Path().apply { addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height)) }),
            Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height)),
        )
        for (material in GlassMaterial.entries) {
            for (dark in listOf(true, false)) {
                val look = glassLook(material, strength = 1f, solid = 0.3f, opacity = 1f, dark = dark, tint = Color(0xFF7C6CFF))
                for (outline in outlines) {
                    // Untouched, and with a finger's light in it.
                    for (touch in listOf(GlassTouch.None, GlassTouch(Offset(60f, 40f), 120f, 0.8f))) {
                        val node = RenderNode("glass").apply { setPosition(0, 0, size.width.toInt(), size.height.toInt()) }
                        val canvas = node.beginRecording()
                        try {
                            // Throws on a compile error or on any uniform the program does not declare.
                            lens.draw(
                                canvas, size, outline,
                                origin = Offset(40f, 900f),
                                screen = IntSize(1080, 2424),
                                wallpaperOffset = Offset(0.5f, 0.5f),
                                density = 2.625f,
                                look = look,
                                light = Offset(-0.45f, -1f),
                                dark = dark,
                                touch = touch,
                            )
                        } finally {
                            node.endRecording()
                        }
                    }
                }
            }
        }
    }
}
