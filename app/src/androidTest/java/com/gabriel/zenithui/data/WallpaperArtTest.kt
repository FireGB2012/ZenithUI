package com.gabriel.zenithui.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gabriel.zenithui.ui.theme.Palettes
import com.gabriel.zenithui.ui.theme.ZenithPalette
import com.gabriel.zenithui.ui.theme.toned
import com.gabriel.zenithui.ui.theme.wallColors
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The wallpapers are painted by code, so the only way to know a design works is to paint it: in
 * every theme, light and dark, and then to measure what a person would see. A light picture has
 * to be light where the dark labels sit and a dark one dark under the white ones. The contact
 * sheets it writes are for looking at, which no test can do.
 */
@RunWith(AndroidJUnit4::class)
class WallpaperArtTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val palettes = Palettes.all + Palettes.aura

    private fun paint(style: WallStyle, palette: ZenithPalette, dark: Boolean, width: Int = 270, height: Int = 586): Bitmap =
        WallpaperArt.render(style, palette.toned(dark).wallColors(), width, height)

    /** How bright the picture is between two heights, 0 for black to 1 for white, as the eye weighs colours. */
    private fun brightness(bitmap: Bitmap, from: Float = 0f, to: Float = 1f): Float {
        val top = (bitmap.height * from).toInt()
        val bottom = (bitmap.height * to).toInt().coerceAtMost(bitmap.height)
        val pixels = IntArray(bitmap.width * (bottom - top))
        bitmap.getPixels(pixels, 0, bitmap.width, 0, top, bitmap.width, bottom - top)
        var sum = 0.0
        for (p in pixels) sum += 0.2126 * Color.red(p) + 0.7152 * Color.green(p) + 0.0722 * Color.blue(p)
        return (sum / pixels.size / 255.0).toFloat()
    }

    @Test
    fun everyDesignPaintsInEveryPaletteLightAndDark() {
        for (style in WallStyle.entries) for (palette in palettes) for (dark in listOf(true, false)) {
            paint(style, palette, dark).recycle()
        }
    }

    @Test
    fun theLightPictureIsClearlyLighterThanTheDarkOne() {
        for (style in WallStyle.entries) for (palette in palettes) {
            val light = brightness(paint(style, palette, dark = false))
            val dark = brightness(paint(style, palette, dark = true))
            assertTrue("${style.name} in ${palette.name}: light $light, dark $dark", light >= dark + 0.2f)
        }
    }

    @Test
    fun labelsCanBeReadOverTheLowerPartOfEveryDesign() {
        for (style in WallStyle.entries) for (palette in palettes) {
            val light = brightness(paint(style, palette, dark = false), from = 0.55f)
            val dark = brightness(paint(style, palette, dark = true), from = 0.55f)
            assertTrue("${style.name} in ${palette.name}: light lower part is $light, too dark for dark labels", light >= 0.6f)
            assertTrue("${style.name} in ${palette.name}: dark lower part is $dark, too bright for white labels", dark <= 0.45f)
        }
    }

    @Test
    fun theStatusBarCanBeReadOverTheTopOfEveryDesign() {
        for (style in WallStyle.entries) for (palette in palettes) {
            val light = brightness(paint(style, palette, dark = false), to = 0.06f)
            val dark = brightness(paint(style, palette, dark = true), to = 0.06f)
            assertTrue("${style.name} in ${palette.name}: light top is $light", light >= 0.55f)
            assertTrue("${style.name} in ${palette.name}: dark top is $dark", dark <= 0.6f)
        }
    }

    @Test
    fun theSameDesignInTheSameColoursIsTheSamePicture() {
        for (style in WallStyle.entries) {
            val first = paint(style, Palettes.Midnight, dark = true)
            val second = paint(style, Palettes.Midnight, dark = true)
            assertTrue(style.name, first.sameAs(second))
        }
    }

    @Test
    fun aFullSizePaintIsQuick() {
        for (style in WallStyle.entries) for (dark in listOf(true, false)) {
            val start = SystemClock.elapsedRealtime()
            paint(style, Palettes.Midnight, dark, width = 1080, height = 2400).recycle()
            val took = SystemClock.elapsedRealtime() - start
            assertTrue("${style.name} took $took ms", took < 1500)
        }
    }

    /** Not a check: two pictures of every design side by side, to be pulled off the device and looked at. */
    @Test
    fun writesContactSheets() {
        for (dark in listOf(true, false)) for (palette in listOf(Palettes.Midnight, Palettes.Sunset)) {
            val across = 6
            val thumbWidth = 180
            val thumbHeight = 390
            val gap = 8
            val caption = 26
            val styles = WallStyle.entries
            val down = (styles.size + across - 1) / across
            val sheet = Bitmap.createBitmap(
                across * (thumbWidth + gap) + gap,
                down * (thumbHeight + caption + gap) + gap,
                Bitmap.Config.ARGB_8888,
            )
            val canvas = Canvas(sheet)
            canvas.drawColor(if (dark) Color.rgb(40, 40, 44) else Color.rgb(200, 200, 204))
            val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (dark) Color.WHITE else Color.BLACK
                textSize = 18f
            }
            styles.forEachIndexed { index, style ->
                val x = gap + (index % across) * (thumbWidth + gap)
                val y = gap + (index / across) * (thumbHeight + caption + gap)
                val thumb = paint(style, palette, dark, thumbWidth, thumbHeight)
                canvas.drawBitmap(thumb, x.toFloat(), y.toFloat(), null)
                thumb.recycle()
                canvas.drawText(style.label + if (style.plus) " +" else "", x.toFloat(), (y + thumbHeight + 20).toFloat(), text)
            }
            val name = "walls-${palette.id}-${if (dark) "dark" else "light"}.png"
            File(context.externalCacheDir, name).outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
            sheet.recycle()
        }
    }
}
