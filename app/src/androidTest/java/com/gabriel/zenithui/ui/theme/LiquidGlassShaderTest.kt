package com.gabriel.zenithui.ui.theme

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Picture
import android.graphics.RenderNode
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.gabriel.zenithui.data.GlassLevels
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The glass falls back quietly when its shader cannot be built, so a typo in the AGSL or a
 * misnamed uniform would ship as "glass that never bends" with no crash to notice. This compiles
 * the program on a real device and sets every uniform for each kind of outline Zenith draws, and
 * then paints with it and looks at what came out: a program can compile and still put the
 * wallpaper in the wrong place.
 */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 33)
@RequiresApi(33)
class LiquidGlassShaderTest {

    private val wallpaper = Bitmap.createBitmap(96, 216, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF3366CC.toInt()) }

    @Test
    fun compilesAndTakesEveryUniformForEveryOutline() {
        val lens = LiquidGlassShader(GlassLevels.of(wallpaper))
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
                                lean = Offset(0.3f, -0.2f),
                            )
                        } finally {
                            node.endRecording()
                        }
                    }
                }
            }
        }
    }

    private val screen = IntSize(480, 1040)
    private val density = 2f

    private fun flat(colour: Int): Bitmap = Bitmap.createBitmap(480, 1040, Bitmap.Config.ARGB_8888).apply { eraseColor(colour) }

    /** One pane, painted on the GPU, which is the only place the shader runs, and read back. */
    private fun paint(wallpaper: Bitmap, look: GlassLook, size: Size, origin: Offset, corner: Float = 40f, dark: Boolean = true): Bitmap {
        val lens = LiquidGlassShader(GlassLevels.of(wallpaper))
        val picture = Picture()
        val canvas = picture.beginRecording(size.width.toInt(), size.height.toInt())
        lens.draw(
            canvas, size,
            Outline.Rounded(RoundRect(0f, 0f, size.width, size.height, CornerRadius(corner, corner))),
            origin = origin,
            screen = screen,
            wallpaperOffset = Offset(0.5f, 0.5f),
            density = density,
            look = look,
            light = Offset(-0.45f, -1f),
            dark = dark,
        )
        picture.endRecording()
        val hardware = Bitmap.createBitmap(picture)
        return hardware.copy(Bitmap.Config.ARGB_8888, false).also { hardware.recycle() }
    }

    private fun red(pixel: Int) = android.graphics.Color.red(pixel)
    private fun blue(pixel: Int) = android.graphics.Color.blue(pixel)
    private fun bright(pixel: Int) = red(pixel) + android.graphics.Color.green(pixel) + blue(pixel)

    @Test
    fun whatIsSeenThroughAPaneIsWhatIsBehindIt() {
        // Red on the left of the screen and blue on the right, meeting down the middle. Android
        // zooms a wallpaper about its middle, so that is where the join stays.
        val wallpaper = flat(0xFFC02020.toInt())
        Canvas(wallpaper).drawRect(240f, 0f, 480f, 1040f, Paint().apply { color = 0xFF2030C0.toInt() })
        // Every softness of glass reads a different copy of the wallpaper, and each has to be
        // lined up: a pane across the join, 100 px of it on either side.
        for (material in GlassMaterial.entries) for (solid in listOf(0f, 0.65f, 1.4f)) {
            val look = glassLook(material, strength = 1f, solid = solid, opacity = 1f, dark = true, tint = Color(0xFF7C6CFF))
            val pane = paint(wallpaper, look, Size(200f, 200f), origin = Offset(140f, 400f))
            val what = "$material, solid $solid, as soft as ${look.frost}"
            // The same distance either side of the join it is redder on the left and bluer on
            // the right, however thick a veil lies over both. Both places are on the pane's flat
            // face (its shoulders are 48 px wide), where nothing but the wallpaper colours it:
            // in a shoulder the light that shapes the slab would be measured too.
            val left = pane.getPixel(56, 100)
            val right = pane.getPixel(144, 100)
            assertTrue("left of the join is no redder than right of it: $what", red(left) > red(right) + 12)
            assertTrue("right of the join is no bluer than left of it: $what", blue(right) > blue(left) + 12)
            // And the join itself is in the middle: half way from one red to the other is found
            // there, not to one side, which is where a copy that was not lined up would put it.
            val half = (red(left) + red(right)) / 2
            var join = -1
            for (x in 56..144) {
                if (red(pane.getPixel(x, 100)) <= half) { join = x; break }
            }
            assertTrue("the join is seen at $join, not at 100: $what", join in 94..106)
        }
    }

    @Test
    fun theRimCatchesTheLightAndTheFaceDoesNot() {
        val look = glassLook(GlassMaterial.Regular, strength = 1f, solid = 0f, opacity = 1f, dark = true, tint = Color(0xFF7C6CFF))
        val pane = paint(flat(0xFF404858.toInt()), look, Size(240f, 240f), origin = Offset(120f, 400f))
        val face = bright(pane.getPixel(120, 120))
        val lit = bright(pane.getPixel(120, 1))
        val far = bright(pane.getPixel(120, 238))
        val side = bright(pane.getPixel(238, 120))
        assertTrue("the rim facing the light ($lit) is no brighter than the face ($face)", lit > face + 90)
        assertTrue("the rim opposite ($far) is no brighter than the face ($face)", far > face + 40)
        assertTrue("the rim is not lit all the way round: side $side, face $face", side > face + 20)
        assertTrue("the light is not strongest where the rim faces it: $lit, $far, $side", lit > far && far > side)
        // A flat wallpaper is the same through the whole face: nothing bends where nothing is.
        assertTrue(kotlin.math.abs(bright(pane.getPixel(90, 150)) - face) <= 3)
    }

    @Test
    fun aSlabIsShapedByTheLightAndASheetIsNot() {
        val look = glassLook(GlassMaterial.Regular, strength = 1f, solid = 0f, opacity = 1f, dark = true, tint = Color(0xFF7C6CFF))
        val wallpaper = flat(0xFF404858.toInt())
        val slab = paint(wallpaper, look, Size(240f, 240f), origin = Offset(120f, 400f))
        val sheet = paint(wallpaper, look.copy(relief = 0f), Size(240f, 240f), origin = Offset(120f, 400f))
        val face = bright(slab.getPixel(120, 120))
        // The pane's shoulders are 56 px wide. The one turned to the lamp is lit, and the ones
        // turned from it, at the foot and on the right, are in the slab's own shade.
        val lit = bright(slab.getPixel(120, 28))
        val foot = bright(slab.getPixel(120, 216))
        val side = bright(slab.getPixel(222, 120))
        assertTrue("the lit shoulder ($lit) is no brighter than the face ($face)", lit > face + 60)
        assertTrue("the far shoulder ($foot) is no darker than the face ($face)", foot < face - 30)
        assertTrue("the shoulder beside the light ($side) is no darker than the face ($face)", side < face - 20)
        // Its polished edge mirrors the lamp a few pixels in from the top.
        val glint = bright(slab.getPixel(120, 10))
        assertTrue("no glint: $glint on the slab, ${bright(sheet.getPixel(120, 10))} without", glint > bright(sheet.getPixel(120, 10)) + 150)
        // With no relief the same glass is a flat sheet of light: its shoulders are as its face.
        assertTrue(kotlin.math.abs(bright(sheet.getPixel(120, 216)) - face) <= 18)
        assertTrue(kotlin.math.abs(bright(sheet.getPixel(222, 120)) - face) <= 18)
        assertTrue(kotlin.math.abs(bright(sheet.getPixel(120, 120)) - face) <= 3)
    }

    @Test
    fun aPageThatFillsTheScreenHasNoRim() {
        val look = glassLook(GlassMaterial.Regular, strength = 1.2f, solid = 0.9f, opacity = 1f, dark = true, tint = Color(0xFF7C6CFF), border = false)
        val pane = paint(flat(0xFF404858.toInt()), look, Size(480f, 1040f), origin = Offset.Zero, corner = 0f)
        val middle = bright(pane.getPixel(240, 520))
        for ((x, y) in listOf(0 to 520, 479 to 520, 240 to 0, 240 to 1039, 2 to 2, 477 to 1037)) {
            val edge = bright(pane.getPixel(x, y))
            assertTrue("($x, $y) is $edge and the middle $middle", kotlin.math.abs(edge - middle) <= 3)
        }
    }
}
