package com.gabriel.zenithui.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.Plus
import com.gabriel.zenithui.data.PlusStatus
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.GlassButton
import com.gabriel.zenithui.ui.common.SectionLabel
import com.gabriel.zenithui.ui.common.AppIcon
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.popIn
import com.gabriel.zenithui.ui.common.popOut
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.common.shake
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.common.unfoldIn
import com.gabriel.zenithui.ui.common.unfoldOut
import com.gabriel.zenithui.ui.common.wordsChange
import com.gabriel.zenithui.ui.settings.Toggle
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.Palettes
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass

/**
 * The one paid thing in Zenith. It is shown as a description of what you get rather than as a
 * wall: everything that was free stays free, so this sheet never has to apologise for itself.
 */
@Composable
fun PlusSheet(
    visible: Boolean,
    status: PlusStatus,
    onStartTrial: () -> Unit,
    onBuyLifetime: () -> Unit,
    onSubscribe: () -> Unit,
    onRestore: () -> Unit,
    onRedeem: (String) -> Boolean,
    onDismiss: () -> Unit,
) {
    var redeeming by remember(visible) { mutableStateOf(false) }
    var code by remember(visible) { mutableStateOf("") }
    var message by remember(visible) { mutableStateOf<String?>(null) }
    /** Counts the codes that were not taken: the box shakes its head once for each. */
    var refusals by remember { mutableIntStateOf(0) }
    // What was last said, kept so that the line still has its words while it folds away.
    val said = remember { arrayOfNulls<String>(1) }
    if (message != null) said[0] = message
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced

    ZenithSheet(visible = visible, onDismiss = onDismiss) {
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // The sheet arrives a block at a time, from the top: what it is, what it holds, how to get it.
            PlusHero(status, Modifier.arrive(0))

            Column(
                Modifier
                    .fillMaxWidth()
                    .arrive(1)
                    .clip(ZenithShapes.card)
                    .background(palette.onGlass.copy(alpha = 0.05f))
                    .padding(vertical = 4.dp),
            ) {
                Plus.FEATURES.forEachIndexed { index, (title, detail) ->
                    FeatureRow(
                        icon = FEATURE_ICONS.getOrElse(index) { Icons.Rounded.AutoAwesome },
                        title = title,
                        detail = detail,
                        unlocked = status.active,
                    )
                }
            }

            if (!status.lifetime) {
                if (status.trialAvailable) {
                    GlassButton(
                        text = "Start my ${Plus.TRIAL_DAYS} free days",
                        onClick = onStartTrial,
                        filled = true,
                        modifier = Modifier.fillMaxWidth().arrive(2),
                    )
                }

                // Both plans side by side, the pass marked. Until Play has the products they are
                // shown but greyed, with the reason underneath, rather than a dead button.
                Row(Modifier.fillMaxWidth().arrive(3), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!status.subscribed) {
                        PlanCard(
                            title = "Monthly",
                            price = status.monthlyPrice,
                            per = "a month, cancel anytime",
                            badge = null,
                            featured = false,
                            enabled = status.purchasable,
                            onClick = onSubscribe,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    PlanCard(
                        title = "Lifetime",
                        price = status.lifetimePrice,
                        per = "once, yours for good",
                        badge = "Best value",
                        featured = true,
                        enabled = status.purchasable,
                        onClick = onBuyLifetime,
                        modifier = Modifier.weight(1f),
                    )
                }

                Text(
                    text = when {
                        !status.purchasable -> "Buying opens once Zenith is on Google Play. The free trial works right now."
                        status.subscribed -> "Buying the pass does not cancel the subscription — do that in Play, and you keep Plus either way."
                        else -> "Lifetime costs about ${Plus.LIFETIME_EQUIVALENT_MONTHS} months of the monthly. No ads, ever."
                    },
                    color = palette.onGlassMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallAction(Icons.Rounded.Restore, "Restore", Modifier.weight(1f)) { onRestore() }
                    SmallAction(Icons.Rounded.Redeem, "Have a code?", Modifier.weight(1f)) {
                        redeeming = !redeeming
                        message = null
                    }
                }

                // The box for a code opens out under the button that asked for it. A code that is
                // not one of ours makes it shake its head, and a line under it says why.
                AnimatedVisibility(visible = redeeming, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PinField(
                            value = code,
                            onChange = {
                                code = it.uppercase()
                                message = null
                            },
                            digitsOnly = false,
                            modifier = Modifier.shake(refusals),
                        )
                        GlassButton(
                            text = "Redeem",
                            onClick = {
                                val ok = onRedeem(code)
                                if (ok) {
                                    message = null
                                    code = ""
                                    redeeming = false
                                } else {
                                    message = "That code is not one of ours."
                                    refusals++
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        AnimatedVisibility(visible = message != null, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
                            Text(said[0].orEmpty(), color = palette.onGlassMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * A block of a sheet arriving, [turn] down from its top: up a little and in, after the ones above
 * it, and without a bounce, which on a block this size would be too much.
 */
@Composable
private fun Modifier.arrive(turn: Int): Modifier =
    appearIn(delayMillis = staggerDelay(turn) * 2, fromScale = 0.985f, riseFrom = 40f, gentle = true)

/** One icon per entry in Plus.FEATURES, in the same order. Setup shows them too. */
internal val FEATURE_ICONS: List<ImageVector> = listOf(
    Icons.Rounded.Palette,
    Icons.Rounded.VisibilityOff,
    Icons.Rounded.Save,
)

/** The header: what Plus is, and where you stand with it. */
@Composable
private fun PlusHero(status: PlusStatus, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    val statusLine = when {
        status.lifetime -> "Unlocked for good. Thank you — it follows you to every phone you sign in to."
        status.subscribed -> "Subscribed at ${status.monthlyPrice} a month."
        status.trialAvailable -> "${Plus.TRIAL_DAYS} free days first. Nothing is charged until they are up."
        status.trialDaysLeft > 1 -> "${status.trialDaysLeft} days left of your free trial. Everything is on."
        status.trialDaysLeft == 1 -> "Last day of your free trial. Everything is on."
        else -> "Your free week is over. Pick whichever suits you."
    }
    val inTrial = !status.purchased && !status.trialAvailable && status.trialDaysLeft > 0

    Column(
        modifier
            .fillMaxWidth()
            .clip(ZenithShapes.card)
            .background(
                Brush.linearGradient(
                    listOf(
                        palette.accent.copy(alpha = 0.42f),
                        palette.accentAlt.copy(alpha = 0.18f),
                        Color.Transparent,
                    )
                )
            )
            .border(1.dp, palette.accent.copy(alpha = 0.35f), ZenithShapes.card)
            .padding(18.dp),
    ) {
        Box(
            Modifier
                .size(46.dp)
                // The star lands a beat after the card it is on, with a bounce of its own.
                .appearIn(delayMillis = 180, fromScale = 0.3f, riseFrom = 0f)
                .clip(RoundedCornerShape(15.dp))
                .background(Brush.linearGradient(listOf(palette.accent, palette.accentAlt))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color(0xFF0A0B12), modifier = Modifier.size(25.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text("Zenith Plus", color = palette.onGlass, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(statusLine, color = palette.onGlass.copy(alpha = 0.82f), fontSize = 13.sp, lineHeight = 18.sp)

        if (inTrial) {
            Spacer(Modifier.height(14.dp))
            val left = status.trialDaysLeft.coerceIn(0, Plus.TRIAL_DAYS) / Plus.TRIAL_DAYS.toFloat()
            // What is left of the week runs out along its bar as the sheet arrives.
            val run = remember { Animatable(if (reduced) left else 0f) }
            LaunchedEffect(left, reduced) {
                if (reduced) run.snapTo(left) else run.animateTo(left, spring(dampingRatio = 0.85f, stiffness = 70f))
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(palette.onGlass.copy(alpha = 0.14f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(run.value.coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(palette.accent, palette.accentAlt)))
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "${status.trialDaysLeft} of ${Plus.TRIAL_DAYS} days left",
                color = palette.onGlassMuted,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, detail: String, unlocked: Boolean) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(palette.accent.copy(alpha = 0.32f), palette.accentAlt.copy(alpha = 0.16f))
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(detail, color = palette.onGlassMuted, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(8.dp))
        // Plus bought, or its week begun, with the sheet still open: each lock pops into a tick.
        val reduced = LocalMotionLevel.current.reduced
        AnimatedContent(
            targetState = unlocked,
            transitionSpec = { popIn(reduced, from = 0.3f, millis = 220).togetherWith(popOut(reduced, to = 0.3f)) },
            label = "featureLock",
        ) { open ->
            Icon(
                imageVector = if (open) Icons.Rounded.Check else Icons.Rounded.Lock,
                contentDescription = if (open) "Unlocked" else "Locked",
                tint = if (open) palette.accent else palette.onGlassMuted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** One of the two ways to pay. The featured one carries a badge and an accent rim. */
@Composable
private fun PlanCard(
    title: String,
    price: String,
    per: String,
    badge: String?,
    featured: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .pressBounce(0.97f, enabled = enabled)
                .alpha(if (enabled) 1f else 0.55f)
                .clip(ZenithShapes.card)
                .background(
                    if (featured) palette.accent.copy(alpha = 0.16f) else palette.onGlass.copy(alpha = 0.06f)
                )
                .border(
                    width = if (featured) 1.5.dp else 1.dp,
                    color = if (featured) palette.accent.copy(alpha = 0.8f) else palette.onGlass.copy(alpha = 0.10f),
                    shape = ZenithShapes.card,
                )
                .clickable(enabled = enabled) { onClick() }
                .padding(horizontal = 14.dp, vertical = 16.dp),
        ) {
            Text(title, color = palette.onGlassMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(
                text = price,
                color = if (featured) palette.accent else palette.onGlass,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(per, color = palette.onGlassMuted, fontSize = 11.sp, maxLines = 1)
        }
        if (badge != null) {
            Text(
                text = badge,
                color = Color(0xFF0A0B12),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(ZenithShapes.pill)
                    .background(Brush.horizontalGradient(listOf(palette.accent, palette.accentAlt)))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun SmallAction(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        modifier
            .pressBounce(0.96f)
            .clip(ZenithShapes.pill)
            .background(palette.onGlass.copy(alpha = 0.06f))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = palette.onGlass, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = palette.onGlass, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/** Locked features send you here rather than doing nothing. */
@Composable
fun PlusLockedRow(label: String, subtitle: String, onClick: () -> Unit) {
    MenuRow(Icons.Rounded.Lock, label, subtitle, onClick = onClick)
}

// ---------------------------------------------------------------------- theme maker

@Composable
fun ThemeMakerSheet(
    visible: Boolean,
    settings: ZenithSettings,
    onChange: ((ZenithSettings) -> ZenithSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    val preview = remember(settings) { Palettes.custom(settings) }

    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Theme maker") {
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // What you are building, drawn with the colours you have chosen so far.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(ZenithShapes.tile)
                    .background(Brush.linearGradient(preview.backdrop)),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    Modifier
                        .padding(horizontal = 18.dp)
                        .fillMaxWidth()
                        .glass(ZenithShapes.tile, strength = 1f, solid = 0.5f)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Brush.linearGradient(listOf(preview.accent, preview.accentAlt)))
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Yours",
                        color = if (settings.customDark) Color.White else Color(0xFF10131A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            ColourField(
                label = "Accent",
                value = settings.customAccent,
                onChange = { argb -> onChange { it.copy(customAccent = argb, paletteId = Palettes.CUSTOM) } },
            )
            ColourField(
                label = "Second accent",
                value = settings.customAccentAlt,
                onChange = { argb -> onChange { it.copy(customAccentAlt = argb, paletteId = Palettes.CUSTOM) } },
            )
            ColourField(
                label = "Background",
                value = settings.customBackdrop,
                onChange = { argb -> onChange { it.copy(customBackdrop = argb, paletteId = Palettes.CUSTOM) } },
            )

            SwitchRow(
                title = "Light text",
                detail = "On for dark backgrounds, off for pale ones.",
                checked = settings.customDark,
            ) { onChange { it.copy(customDark = !settings.customDark, paletteId = Palettes.CUSTOM) } }

            // Offered until the theme is in use, and folded away once it is.
            AnimatedVisibility(
                visible = settings.paletteId != Palettes.CUSTOM,
                enter = unfoldIn(LocalMotionLevel.current.reduced),
                exit = unfoldOut(LocalMotionLevel.current.reduced),
            ) {
                GlassButton(
                    text = "Use my theme",
                    onClick = { onChange { it.copy(paletteId = Palettes.CUSTOM) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** One colour, as a swatch and the three rails that make it. */
@Composable
private fun ColourField(label: String, value: Long, onChange: (Long) -> Unit) {
    val palette = LocalPalette.current
    val hsv = remember(value) { value.toHsv() }
    var hue by remember(value) { mutableStateOf(hsv[0]) }
    var saturation by remember(value) { mutableStateOf(hsv[1]) }
    var brightness by remember(value) { mutableStateOf(hsv[2]) }

    fun emit() = onChange(hsvToArgb(hue, saturation, brightness))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(value.toInt()))
                    .border(1.dp, palette.onGlass.copy(alpha = 0.20f), RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.width(12.dp))
            Text(label, color = palette.onGlass, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(
                text = "#%06X".format(value.toInt() and 0xFFFFFF),
                color = palette.onGlassMuted,
                fontSize = 11.sp,
            )
        }

        ColourRail(
            colours = (0..12).map { Color(hsvToArgb(it * 30f, 1f, 1f).toInt()) },
            fraction = hue / 360f,
        ) { fraction ->
            hue = fraction * 360f
            emit()
        }
        ColourRail(
            colours = listOf(Color(hsvToArgb(hue, 0f, brightness).toInt()), Color(hsvToArgb(hue, 1f, brightness).toInt())),
            fraction = saturation,
        ) { fraction ->
            saturation = fraction
            emit()
        }
        ColourRail(
            colours = listOf(Color.Black, Color(hsvToArgb(hue, saturation, 1f).toInt())),
            fraction = brightness,
        ) { fraction ->
            brightness = fraction
            emit()
        }
    }
}

/**
 * A gradient bar you drag a knob along. Nicer to the thumb than a stock slider, and on brand. A
 * tap glides the knob to where you touched, a drag is followed almost exactly, and for as long as
 * a finger is on the bar the knob stands a little larger.
 */
@Composable
private fun ColourRail(colours: List<Color>, fraction: Float, onPick: (Float) -> Unit) {
    var width by remember { mutableStateOf(1f) }
    var held by remember { mutableStateOf(false) }
    val at by animateFloatAsState(fraction.coerceIn(0f, 1f), spring(dampingRatio = 1f, stiffness = 1400f), label = "railKnob")
    val grip = animateFloatAsState(if (held) 1f else 0f, Motion.press(), label = "railGrip")
    Box(
        Modifier
            .fillMaxWidth()
            .height(26.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(Brush.horizontalGradient(colours))
            // Only watches, for as long as a finger is down: the drag and the tap are below.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    held = true
                    try {
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                    } finally {
                        held = false
                    }
                }
            }
            .pointerInput(Unit) {
                width = size.width.toFloat()
                detectHorizontalDragGestures(
                    onDragStart = { offset -> onPick((offset.x / size.width).coerceIn(0f, 1f)) },
                ) { change, _ ->
                    change.consume()
                    onPick((change.position.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                width = size.width.toFloat()
                detectTapGestures { offset -> onPick((offset.x / size.width).coerceIn(0f, 1f)) }
            },
    ) {
        Box(
            Modifier
                .fillMaxWidth(at)
                .height(26.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                Modifier
                    .padding(end = 2.dp)
                    .size(20.dp)
                    .graphicsLayer {
                        val grown = 1f + 0.2f * grip.value
                        scaleX = grown
                        scaleY = grown
                    }
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.White)
                    .border(2.dp, Color.Black.copy(alpha = 0.25f), RoundedCornerShape(percent = 50))
            )
        }
    }
}

private fun Long.toHsv(): FloatArray {
    val out = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toInt(), out)
    return out
}

private fun hsvToArgb(hue: Float, saturation: Float, brightness: Float): Long {
    val argb = android.graphics.Color.HSVToColor(
        floatArrayOf(hue.coerceIn(0f, 360f), saturation.coerceIn(0f, 1f), brightness.coerceIn(0f, 1f))
    )
    return argb.toLong() and 0xFFFFFFFFL
}

// ---------------------------------------------------------------------- hidden apps

/**
 * Hidden apps, and the PIN in front of them.
 *
 * This is a tidiness feature, not a security one, and the copy says so: the apps are kept out of
 * Zenith's drawer, they are not locked on the device.
 */
@Composable
fun HiddenAppsSheet(
    visible: Boolean,
    apps: List<AppInfo>,
    settings: ZenithSettings,
    onToggle: (String) -> Unit,
    onSetPin: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    var unlocked by remember(visible) { mutableStateOf(settings.hiddenPin.isEmpty()) }
    var entry by remember(visible) { mutableStateOf("") }
    var editingPin by remember(visible) { mutableStateOf(false) }
    /** Counts the wrong PINs: the box shakes its head once for each. */
    var refusals by remember { mutableIntStateOf(0) }

    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Hidden apps") {
        // The right PIN, and the lock gives way to the list where it stands, the sheet growing to
        // hold it. The wrong one empties the box and shakes it.
        AnimatedContent(
            targetState = unlocked,
            transitionSpec = { wordsChange(reduced) },
            label = "hiddenLock",
        ) { open ->
            if (!open) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter your PIN to see the list.", color = palette.onGlassMuted, fontSize = 13.sp)
                    PinField(value = entry, onChange = { entry = it }, modifier = Modifier.shake(refusals))
                    GlassButton(
                        text = "Unlock",
                        onClick = {
                            if (entry == settings.hiddenPin) {
                                unlocked = true
                            } else {
                                entry = ""
                                refusals++
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Hidden apps stay out of the drawer and out of search. They are not locked " +
                            "on your phone — anything else that lists your apps still shows them.",
                        color = palette.onGlassMuted,
                        fontSize = 12.sp,
                    )

                    // The row that offers a PIN turns into the box to type one in, and back.
                    AnimatedContent(
                        targetState = editingPin,
                        transitionSpec = { wordsChange(reduced) },
                        label = "hiddenPin",
                    ) { editing ->
                        if (editing) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                PinField(value = entry, onChange = { entry = it })
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    GlassButton(
                                        text = if (entry.isBlank()) "Remove PIN" else "Save PIN",
                                        onClick = {
                                            onSetPin(entry)
                                            entry = ""
                                            editingPin = false
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        } else {
                            MenuRow(
                                icon = Icons.Rounded.Lock,
                                label = if (settings.hiddenPin.isEmpty()) "Set a PIN" else "Change PIN",
                                subtitle = if (settings.hiddenPin.isEmpty()) "Optional" else "Asked for before this list opens",
                            ) { editingPin = true }
                        }
                    }

                    SectionLabel("Apps")
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(apps, key = { it.key }) { app ->
                            val hidden = app.key in settings.hiddenApps
                            val touch = remember { MutableInteractionSource() }
                            val pressed by touch.collectIsPressedAsState()
                            // Hidden, its icon fades back, so the list shows at a glance which
                            // apps are out of the drawer.
                            val shownAs = animateFloatAsState(if (hidden) 0.5f else 1f, tween(260), label = "hiddenIcon")
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(interactionSource = touch, indication = LocalIndication.current) { onToggle(app.key) }
                                    .padding(horizontal = 6.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AppIcon(
                                    key = app.key,
                                    label = app.label,
                                    size = 34.dp,
                                    shape = ZenithShapes.forIconShape(settings.iconShape),
                                    modifier = Modifier.graphicsLayer { alpha = shownAs.value },
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(app.label, color = palette.onGlass, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Toggle(hidden, pressed)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A setting on or off, in a sheet: what it is, a line about it, and the same switch Settings has,
 * which the whole row works.
 */
@Composable
private fun SwitchRow(title: String, detail: String, checked: Boolean, onFlip: () -> Unit) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    val touch = remember { MutableInteractionSource() }
    val pressed by touch.collectIsPressedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(interactionSource = touch, indication = null) {
                tick()
                onFlip()
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = palette.onGlass, fontSize = 15.sp)
            Text(detail, color = palette.onGlassMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.width(12.dp))
        Toggle(checked, pressed)
    }
}

@Composable
private fun PinField(value: String, onChange: (String) -> Unit, digitsOnly: Boolean = true, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    BasicTextField(
        value = value,
        onValueChange = { text ->
            onChange(if (digitsOnly) text.filter { c -> c.isDigit() }.take(6) else text.take(24))
        },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (digitsOnly) KeyboardType.NumberPassword else KeyboardType.Ascii,
        ),
        textStyle = TextStyle(color = palette.onGlass, fontSize = 20.sp, textAlign = TextAlign.Center),
        cursorBrush = Brush.linearGradient(listOf(palette.accent, palette.accent)),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .glass(ZenithShapes.pill, strength = 0.8f)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) { inner() }
        },
    )
}
