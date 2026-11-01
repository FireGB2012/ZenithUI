package com.gabriel.zenithui.ui.theme

import android.graphics.BitmapShader
import android.graphics.LinearGradient
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.gabriel.zenithui.data.GlassLevels
import kotlin.math.hypot
import kotlin.math.max

/**
 * Makes whatever this draws out of glass, the way iOS makes the Lock Screen clock: no pane behind
 * it, the numerals and symbols themselves are the glass. Its own outline becomes a rounded rim, so
 * the wallpaper behind it bends and magnifies towards its edges, a crisp highlight runs along the
 * edges facing the light and moves as the phone tilts, and a soft shade round it keeps it clear of
 * the wallpaper. Each stroke is a rounded bar of glass and is lit as one: the side of it turned
 * from the light is in its own shade, and where its round mirrors the lamp it glints. What is
 * drawn white is clear glass; what is drawn in a colour is glass of that colour, toned by how
 * bright the wallpaper behind it is.
 *
 * [bevel] is how far in from its edges the glass stays curved: about a third of the stroke width
 * looks right, so a big numeral gets a wider rim than a small icon.
 *
 * Android 13+ draws it with a shader over Zenith's copy of the wallpaper, or with frosted glass
 * when there is no copy to show. Older Android draws it as it is, a little translucent.
 */
@Composable
fun Modifier.glassInk(bevel: Dp = 4.dp, halo: Boolean = true): Modifier {
    if (Build.VERSION.SDK_INT < 33) return this.graphicsLayer { alpha = 0.92f }

    val levels = LocalGlassLevels.current
    val screen = LocalScreenSize.current
    val wallpaperOffset = LocalWallpaperOffset.current
    val tilt = LocalTilt.current
    val tuning = LocalGlassTuning.current
    val strength = LocalGlassStrength.current.coerceIn(0f, 1.4f)
    val lift = LocalGlassBody.current.lift
    val ink = remember(levels) {
        // A GPU that cannot compile it still shows the shape, plainly.
        runCatching { GlassInkShader(levels) }.getOrNull()
    }
    var origin by remember { mutableStateOf(Offset.Zero) }

    return this
        // The rim's highlight and the shade round the shape need a little room past its edges.
        .bleed(INK_BLEED)
        .onGloballyPositioned { origin = it.positionInWindow() }
        .graphicsLayer {
            // Read here, not in composition: moving, paging and tilting only rebuild the effect.
            val lean = if (tuning.lightTilt) tilt.value else Offset.Zero
            renderEffect = ink?.effect(
                size = size,
                origin = origin,
                screen = screen,
                wallpaperOffset = systemWallpaperOffset(wallpaperOffset.value),
                bevel = bevel.toPx(),
                light = lightFrom(lean, tuning),
                strength = strength,
                halo = halo,
                lift = lift,
                lean = lean,
            )?.asComposeRenderEffect()
        }
        .padding(INK_BLEED)
}

private val INK_BLEED = 8.dp

/** How much more light lifted glass gathers over a dark picture, on top of what it always does. */
private const val INK_GATHER = 0.27f

/**
 * Lets the next modifiers draw [margin] past this element's edges on every side without taking
 * any more room in the layout: measured bigger, reported at the original size, placed back by the
 * margin so the content lands exactly where it would have been.
 */
private fun Modifier.bleed(margin: Dp): Modifier = layout { measurable, constraints ->
    val extra = margin.roundToPx()
    val placeable = measurable.measure(constraints.offset(extra * 2, extra * 2))
    layout((placeable.width - extra * 2).coerceAtLeast(0), (placeable.height - extra * 2).coerceAtLeast(0)) {
        placeable.place(-extra, -extra)
    }
}

/** The shader behind [glassInk]: one per element, rebuilt only when the wallpaper changes. */
@RequiresApi(33)
internal class GlassInkShader(levels: GlassLevels?) {
    private val sharpSize = levels?.sharp?.let(::sizeOf)
    private val mistSize = levels?.mist?.let(::sizeOf)
    private val frostSize = levels?.frost?.let(::sizeOf)
    private val hazeSize = levels?.haze?.let(::sizeOf)
    private val shader = RuntimeShader(PROGRAM).apply {
        if (levels != null) {
            setInputShader("wallpaper", bitmapShader(levels.sharp))
            setInputShader("misted", bitmapShader(levels.mist))
            setInputShader("frosted", bitmapShader(levels.frost))
            setInputShader("hazed", bitmapShader(levels.haze))
        } else {
            // Nothing to see through: the program takes its frosted branch and never samples these.
            for (name in listOf("wallpaper", "misted", "frosted", "hazed")) setInputShader(name, flat())
        }
    }

    fun effect(
        size: Size,
        /** Top-left of the element's layer on screen, in pixels. */
        origin: Offset,
        screen: IntSize,
        wallpaperOffset: Offset,
        bevel: Float,
        light: Offset,
        strength: Float,
        halo: Boolean,
        /** How far the glass is lifted off a picture of its own tone: see GlassBody. */
        lift: Float = 0f,
        /** Which way the phone leans: what the edges bend and reflect slides with it. */
        lean: Offset = Offset.Zero,
    ): RenderEffect? =
        // The uniforms are copied into the effect as it is made, so every change needs a new one.
        program(size, origin, screen, wallpaperOffset, bevel, light, strength, halo, lift, lean)
            ?.let { RenderEffect.createRuntimeShaderEffect(it, "content") }

    /** The program with its numbers set for one drawing. `content`, the shape to be made of glass, is still to be given. */
    internal fun program(
        size: Size,
        origin: Offset,
        screen: IntSize,
        wallpaperOffset: Offset,
        bevel: Float,
        light: Offset,
        strength: Float,
        halo: Boolean,
        lift: Float = 0f,
        lean: Offset = Offset.Zero,
    ): RuntimeShader? {
        if (size.width <= 0f || size.height <= 0f) return null
        val visible = sharpSize != null && mistSize != null && frostSize != null && hazeSize != null &&
            screen.width > 0 && screen.height > 0
        if (visible) {
            // The wallpaper as Android shows it, exactly as the panes line it up.
            val scale = max(screen.width / sharpSize!!.width, screen.height / sharpSize.height) * SYSTEM_WALLPAPER_ZOOM
            val cropX = (sharpSize.width * scale - screen.width) * wallpaperOffset.x.coerceIn(0f, 1f)
            val cropY = (sharpSize.height * scale - screen.height) * wallpaperOffset.y.coerceIn(0f, 1f)
            shader.setFloatUniform("origin", origin.x + cropX, origin.y + cropY)
            shader.setFloatUniform("imageScale", 1f / scale)
            shader.setFloatUniform("mistRatio", mistSize!!.width / sharpSize.width, mistSize.height / sharpSize.height)
            shader.setFloatUniform("frostRatio", frostSize!!.width / sharpSize.width, frostSize.height / sharpSize.height)
            shader.setFloatUniform("hazeRatio", hazeSize!!.width / sharpSize.width, hazeSize.height / sharpSize.height)
        } else {
            shader.setFloatUniform("origin", 0f, 0f)
            shader.setFloatUniform("imageScale", 1f)
            shader.setFloatUniform("mistRatio", 1f, 1f)
            shader.setFloatUniform("frostRatio", 1f, 1f)
            shader.setFloatUniform("hazeRatio", 1f, 1f)
        }
        shader.setFloatUniform("seeThrough", if (visible) 1f else 0f)
        shader.setFloatUniform("bevel", bevel.coerceAtLeast(1f))
        shader.setFloatUniform("refraction", 0.55f + 0.45f * strength.coerceAtMost(1.2f))
        val length = hypot(light.x, light.y).coerceAtLeast(0.001f)
        shader.setFloatUniform("light", light.x / length, light.y / length)
        shader.setFloatUniform("lean", lean.x.coerceIn(-1f, 1f), lean.y.coerceIn(-1f, 1f))
        val lifted = lift.coerceIn(0f, 1f)
        shader.setFloatUniform("halo", if (halo) 0.26f + 0.10f * lifted else 0f)
        shader.setFloatUniform("gather", INK_GATHER * lifted)
        shader.setFloatUniform("mirror", MIRROR_CLEAR * strength.coerceAtMost(1f))
        shader.setFloatUniform("glow", GLOW)
        shader.setFloatUniform("relief", strength.coerceIn(0f, 1f))
        return shader
    }

    private fun sizeOf(image: ImageBitmap) = Size(image.width.toFloat(), image.height.toFloat())

    private fun bitmapShader(image: ImageBitmap) =
        BitmapShader(image.asAndroidBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            filterMode = BitmapShader.FILTER_MODE_LINEAR
        }

    private fun flat() = LinearGradient(0f, 0f, 1f, 1f, 0xFF808080.toInt(), 0xFF808080.toInt(), Shader.TileMode.CLAMP)

    companion object {
        /**
         * The element's own drawing arrives as `content`; only its coverage shapes the glass (plus
         * its colour, as a tint). Coverage averaged over a small disc is a height map: 1 deep in a
         * stroke, a half on its edge, 0 outside. The same taps give the slope, so one pass yields
         * both how curved the glass is here and which way it faces.
         */
        internal const val PROGRAM = """
            uniform shader content;
            uniform shader wallpaper;
            uniform shader misted;
            uniform shader frosted;
            uniform shader hazed;
            uniform float2 origin;
            uniform float imageScale;
            uniform float2 mistRatio;
            uniform float2 frostRatio;
            uniform float2 hazeRatio;
            uniform float seeThrough;
            uniform float bevel;
            uniform float refraction;
            uniform float2 light;
            uniform float2 lean;
            uniform float halo;
            uniform float gather;
            uniform float mirror;
            uniform float glow;
            uniform float relief;

            // What is behind a point, as soft as asked: 0 the picture as it is, 3 little but its
            // colours, by blending the prepared pictures either side of that softness.
            float3 behind(float2 point, float soft) {
                if (seeThrough < 0.5) {
                    // No picture of the wallpaper: frosted glass, which still takes the light.
                    return float3(0.60, 0.62, 0.68);
                }
                float2 uv = (point + origin) * imageScale;
                if (soft < 1.0) {
                    return mix(wallpaper.eval(uv).rgb, misted.eval(uv * mistRatio).rgb, max(soft, 0.0));
                }
                if (soft < 2.0) {
                    return mix(misted.eval(uv * mistRatio).rgb, frosted.eval(uv * frostRatio).rgb, soft - 1.0);
                }
                return mix(frosted.eval(uv * frostRatio).rgb, hazed.eval(uv * hazeRatio).rgb, min(soft - 2.0, 1.0));
            }

            half4 main(float2 p) {
                half4 src = content.eval(p);
                float a = src.a;

                float h = a;
                float2 slope = float2(0.0);
                for (int ring = 1; ring <= 3; ring++) {
                    float r = bevel * float(ring) / 3.0;
                    for (int k = 0; k < 12; k++) {
                        // Each ring turned a little against the last, so the taps do not line up.
                        float angle = (float(k) + 0.33 * float(ring)) * 0.52359878;
                        float2 dir = float2(cos(angle), sin(angle));
                        float c = content.eval(p + dir * r).a;
                        h += c;
                        slope += c * dir;
                    }
                }
                h /= 37.0;

                // Right on the edge, a line of even width whatever the bevel: how much of a tiny
                // ring round this point falls outside the shape.
                float edgeRing = 0.0;
                float edgeWidth = min(1.4 * bevel, 4.0);
                for (int k = 0; k < 8; k++) {
                    float angle = float(k) * 0.78539816;
                    edgeRing += content.eval(p + float2(cos(angle), sin(angle)) * edgeWidth).a;
                }
                float hairline = clamp((a - edgeRing / 8.0) * 2.2, 0.0, 1.0);

                // Round the shape, a soft shade that lifts it off the wallpaper.
                float shade = halo * smoothstep(0.0, 0.7, h);
                if (a <= 0.002) {
                    return half4(0.0, 0.0, 0.0, half(shade));
                }

                // How curved the glass is here: 1 on the edge, 0 where it has gone flat.
                float t = clamp((1.0 - h) * 2.0, 0.0, 1.0);
                float curve = 1.0 - sqrt(max(1.0 - t * t, 0.0));
                float steep = length(slope);
                float2 inward = steep > 0.0001 ? slope / steep : float2(0.0);
                float2 normal = -inward;

                // Through the glass: what is behind is pulled in from further inside, magnified
                // as through the rounded edge of a thick lens, with a whisper of colour split.
                float2 bend = (inward * 1.7 * refraction + lean * 0.5) * curve * bevel;
                // Lightly misted through its face, softer where the edge squeezes the picture.
                float soft = max(0.45 * (1.0 - 0.5 * t), curve);
                float3 color = float3(
                    behind(p + bend * 0.94, soft).r,
                    behind(p + bend, soft).g,
                    behind(p + bend * 1.06, soft).b
                );
                float luma = dot(color, float3(0.2126, 0.7152, 0.0722));
                color = mix(float3(luma), color, 1.25);

                // Coloured ink makes coloured glass: its tone follows the brightness behind it,
                // as tinted glass does. White ink stays clear.
                float3 ink = clamp(float3(src.rgb) / max(a, 0.001), 0.0, 1.0);
                float chroma = max(ink.r, max(ink.g, ink.b)) - min(ink.r, min(ink.g, ink.b));
                color = mix(color, ink * (0.35 + 0.9 * luma), clamp(chroma * 1.4, 0.0, 0.85));
                // Glass gathers light: it reads brighter than the picture it sits on, and most
                // over a dark one, where clear glass would be lost. Over a pale one it is the other
                // way about, and the glass holds a little shade of its own.
                float lowlight = 1.0 - smoothstep(0.08, 0.62, luma);
                color = mix(color, float3(1.0), 0.12 + gather * lowlight);
                color *= 1.0 - 1.5 * gather * smoothstep(0.62, 0.95, luma);

                // Reflection: the steep edge of the glass holds the picture just outside the
                // shape, turned over; and the light it catches is the colour of what is round it.
                float3 lamp = float3(1.0);
                if (seeThrough > 0.5) {
                    float2 beside = p + normal * ((1.0 - t) * bevel * 2.6 + 0.5 * bevel + 2.0) + lean * bevel * 1.5;
                    float3 thrown = frosted.eval((beside + origin) * imageScale * frostRatio).rgb;
                    float bright = dot(thrown, float3(0.2126, 0.7152, 0.0722));
                    thrown = clamp(mix(float3(bright), thrown, 1.3), 0.0, 1.0);
                    color = mix(color, thrown, mirror * t * t * t);
                    float3 air = hazed.eval((p + normal * bevel * 2.0 + origin) * imageScale * hazeRatio).rgb;
                    float peak = max(air.r, max(air.g, air.b));
                    float3 tone = mix(float3(1.0), air / max(peak, 0.001), smoothstep(0.04, 0.3, peak));
                    lamp = mix(float3(1.0), tone, glow);
                }

                // Form. The glass is a rounded bar, and it is light that shows its shape: the
                // lamp hangs up and to the left, so the side of the bar turned from it is in its
                // own shade, and where the round of it mirrors the lamp there is a glint.
                float facing = dot(normal, light);
                float lit = max(facing, 0.0);
                float away = max(-facing, 0.0);
                float3 above = normalize(float3(light * 0.77, 0.64));
                float3 round3 = float3(normal * t, sqrt(max(1.0 - t * t, 0.0)));
                float form = dot(round3, above) - above.z;
                color *= 1.0 - 0.45 * relief * clamp(-form, 0.0, 1.0);
                float3 toLight = normalize(above + float3(0.0, 0.0, 1.0));
                float3 toFar = normalize(float3(-above.xy, above.z + 1.0));
                float level = pow(toLight.z, 12.0);
                float glint = max(pow(max(dot(round3, toLight), 0.0), 12.0) - level, 0.0)
                    + 0.35 * max(pow(max(dot(round3, toFar), 0.0), 12.0) - level, 0.0);
                // Deep in a stroke the glass has no one way it faces, and a glint there would be a speck.
                glint *= smoothstep(2.0, 6.0, steep);

                // Light: a crisp line along the edges facing it, a weaker one where it leaves,
                // a soft bloom on the lit curve, and the rim brighter at its grazing angle.
                float rim = hairline * (0.12 + 0.3 * gather + 0.58 * pow(lit, 1.4) + 0.28 * pow(away, 1.8));
                float bloom = t * t * t * (0.18 * pow(lit, 2.0) + 0.06 * pow(away, 2.4));
                color += lamp * (rim + bloom + relief * (0.6 * glint + 0.45 * max(form, 0.0))) + pow(t, 4.0) * 0.04;

                float alpha = a + shade * (1.0 - a);
                return half4(half3(clamp(color, 0.0, 1.0) * a), half(alpha));
            }
        """
    }
}
