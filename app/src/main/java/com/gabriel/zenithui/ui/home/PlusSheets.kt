package com.gabriel.zenithui.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.VisibilityOff
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.Plus
import com.gabriel.zenithui.data.PlusStatus
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.AppIcon
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.ZenithButton
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.popIn
import com.gabriel.zenithui.ui.common.popOut
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.common.shake
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.common.unfoldIn
import com.gabriel.zenithui.ui.common.unfoldOut
import com.gabriel.zenithui.ui.common.wordsChange
import com.gabriel.zenithui.ui.settings.CardRows
import com.gabriel.zenithui.ui.settings.FieldBox
import com.gabriel.zenithui.ui.settings.FieldLook
import com.gabriel.zenithui.ui.settings.GroupDivider
import com.gabriel.zenithui.ui.settings.Note
import com.gabriel.zenithui.ui.settings.Section
import com.gabriel.zenithui.ui.settings.Toggle
import com.gabriel.zenithui.ui.settings.ToggleRow
import com.gabriel.zenithui.ui.settings.rowPress
import com.gabriel.zenithui.ui.theme.AppleColours
import com.gabriel.zenithui.ui.theme.ButtonKind
import com.gabriel.zenithui.ui.theme.CardKind
import com.gabriel.zenithui.ui.theme.LocalApple
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.Palettes
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.inkOnTint
import com.gabriel.zenithui.ui.theme.solidCard

// The sheets Plus brings, set out as an iPhone sets out a sheet: everything on solid cards (Section
// and the rows Settings is built from), grey words on the sheet's glass only where a card's small
// print would stand, and nothing of glass inside them.

/**
 * The one paid thing in Zenith. It is shown as a description of what you get rather than as a
 * wall: everything that was free stays free, so this sheet never has to apologise for itself.
 *
 * Plus's name, said large, and where you stand with it are on a card at the top, so the sheet's
 * header carries no title of its own. Under it, what Plus adds as the rows of a second card; then
 * the free week as the main button, the two plans side by side, each a card of its own, the small
 * print under them, and Restore and the box for a code as small actions in the accent.
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
    val reduced = LocalMotionLevel.current.reduced

    ZenithSheet(visible = visible, onDismiss = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            // The sheet arrives a block at a time, from the top: what it is, what it holds, how to get it.
            PlusHero(status, Modifier.arrive(0))

            Section(modifier = Modifier.arrive(1)) {
                CardRows {
                    Plus.FEATURES.forEachIndexed { index, (title, detail) ->
                        FeatureRow(
                            icon = FEATURE_ICONS.getOrElse(index) { Icons.Rounded.AutoAwesome },
                            title = title,
                            detail = detail,
                            unlocked = status.active,
                        )
                    }
                }
            }

            if (!status.lifetime) {
                if (status.trialAvailable) {
                    Spacer(Modifier.height(16.dp))
                    ZenithButton(
                        text = "Start my ${Plus.TRIAL_DAYS} free days",
                        onClick = onStartTrial,
                        kind = ButtonKind.Main,
                        modifier = Modifier.fillMaxWidth().arrive(2),
                    )
                }

                // Both plans side by side, the pass marked. Until Play has the products they are
                // shown but greyed, with the reason underneath, rather than a dead button.
                Spacer(Modifier.height(12.dp))
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

                // The small print, in grey under the plans as an iPhone writes it under a card.
                Note(
                    text = when {
                        !status.purchasable -> "Buying opens once Zenith is on Google Play. The free trial works right now."
                        status.subscribed -> "Buying the pass does not cancel the subscription — do that in Play, and you keep Plus either way."
                        else -> "Lifetime costs about ${Plus.LIFETIME_EQUIVALENT_MONTHS} months of the monthly. No ads, ever."
                    },
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 7.dp),
                )

                Spacer(Modifier.height(4.dp))
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
                    Column {
                        Spacer(Modifier.height(8.dp))
                        PinField(
                            value = code,
                            onChange = {
                                code = it.uppercase()
                                message = null
                            },
                            digitsOnly = false,
                            modifier = Modifier.shake(refusals),
                        )
                        Spacer(Modifier.height(12.dp))
                        ZenithButton(
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
                            kind = ButtonKind.Grey,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        AnimatedVisibility(visible = message != null, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
                            Note(said[0].orEmpty(), Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp))
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

/**
 * The head of the sheet, on a card, as an iPhone heads a page of its own: Plus's star on a square of
 * the accent, its name large, and in grey where you stand with it, all in the middle. During the
 * free week a thin bar in the accent under that shows how much of the week is left.
 */
@Composable
private fun PlusHero(status: PlusStatus, modifier: Modifier = Modifier) {
    val apple = LocalApple.current
    val accent = LocalPalette.current.accent
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

    Section(modifier = modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    // The star lands a beat after the card it is on, with a bounce of its own.
                    .appearIn(delayMillis = 180, fromScale = 0.3f, riseFrom = 0f)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                // In the ink the accent needs: white on blue, near-black on yellow.
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = inkOnTint(accent), modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Zenith Plus",
                color = apple.words,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                statusLine,
                color = apple.secondary,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
            )

            if (inTrial) {
                Spacer(Modifier.height(18.dp))
                val left = status.trialDaysLeft.coerceIn(0, Plus.TRIAL_DAYS) / Plus.TRIAL_DAYS.toFloat()
                // What is left of the week runs out along its bar as the sheet arrives.
                val run = remember { Animatable(if (reduced) left else 0f) }
                LaunchedEffect(left, reduced) {
                    if (reduced) run.snapTo(left) else run.animateTo(left, spring(dampingRatio = 0.85f, stiffness = 70f))
                }
                // A thin grey track, filled with the accent as far as the days left, as an iPhone's
                // progress bar is drawn.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(apple.grey)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(run.value.coerceIn(0f, 1f))
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(accent)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "${status.trialDaysLeft} of ${Plus.TRIAL_DAYS} days left",
                    color = apple.secondary,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

/**
 * One thing Plus adds, as a row of its card: its symbol in the accent, its name, a line about it in
 * grey, and at the end a grey lock, or a tick in the accent once it is yours. It is only read, not
 * pressed, so it neither shades nor bounces.
 */
@Composable
private fun FeatureRow(icon: ImageVector, title: String, detail: String, unlocked: Boolean) {
    val apple = LocalApple.current
    val accent = LocalPalette.current.accent
    val reduced = LocalMotionLevel.current.reduced
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // As wide as a Settings row's square, so the words, and the hairline under them, line up.
        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = apple.words, fontSize = 16.sp)
            Text(detail, color = apple.secondary, fontSize = 14.sp, lineHeight = 18.sp)
        }
        Spacer(Modifier.width(10.dp))
        // Plus bought, or its week begun, with the sheet still open: each lock pops into a tick.
        AnimatedContent(
            targetState = unlocked,
            transitionSpec = { popIn(reduced, from = 0.3f, millis = 220).togetherWith(popOut(reduced, to = 0.3f)) },
            label = "featureLock",
        ) { open ->
            Icon(
                imageVector = if (open) Icons.Rounded.Check else Icons.Rounded.Lock,
                contentDescription = if (open) "Unlocked" else "Locked",
                tint = if (open) accent else apple.secondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * One of the two ways to pay, as a solid card of its own: what it is and what that buys in grey,
 * its price large between them. The featured one is ringed in the accent and carries a badge of
 * it. Under a finger the card shades, as an iPhone's rows do; while Play cannot sell it is dimmed
 * and does nothing.
 */
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
    val apple = LocalApple.current
    val accent = LocalPalette.current.accent
    val touch = remember { MutableInteractionSource() }
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                // The card stays solid; only what is on it dims while a plan cannot be bought.
                .solidCard(CardKind.Grouped)
                .alpha(if (enabled) 1f else 0.55f)
                .then(if (featured) Modifier.border(2.dp, accent, CardKind.Grouped.shape) else Modifier)
                .rowPress(touch, enabled = enabled)
                .clickable(interactionSource = touch, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 16.dp),
        ) {
            Text(title, color = apple.secondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(
                text = price,
                color = apple.words,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(per, color = apple.secondary, fontSize = 11.sp, maxLines = 1)
        }
        if (badge != null) {
            // A pill of the accent, its words in the ink the accent needs.
            Text(
                text = badge,
                color = inkOnTint(accent),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(ZenithShapes.pill)
                    .background(accent)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

/**
 * A small action under the plans, as an iPhone writes one: its symbol and its words in the accent,
 * with nothing behind them, dimming while they are held.
 */
@Composable
private fun SmallAction(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ZenithButton(
        text = label,
        onClick = onClick,
        modifier = modifier,
        kind = ButtonKind.Plain,
        // Tinted by the button itself, in the colour of its words.
        leading = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
    )
}

/**
 * A feature that needs Plus, as a row of a card: a lock in the accent, what the feature is, and in
 * grey what it would do. Locked features send you here rather than doing nothing. Like every
 * [MenuRow], it goes in a [CardRows] on a card ([Section]).
 */
@Composable
fun PlusLockedRow(label: String, subtitle: String, onClick: () -> Unit) {
    MenuRow(Icons.Rounded.Lock, label, subtitle, onClick = onClick)
}

// ---------------------------------------------------------------------- theme maker

/**
 * The theme maker: a palette of your own, from three colours and whether its words are light. At
 * the top, what you are building; under it each colour on a card of its own, with the rails it is
 * made from; then the switch for light words, and the button that puts the theme in use, offered
 * until it is.
 */
@Composable
fun ThemeMakerSheet(
    visible: Boolean,
    settings: ZenithSettings,
    onChange: ((ZenithSettings) -> ZenithSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val preview = remember(settings) { Palettes.custom(settings) }
    // Apple's card and words in the dark when Light text is on, and in the light when it is off.
    val tile = AppleColours.of(settings.customDark)

    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Theme maker") {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            // What you are building, drawn with the colours you have chosen so far: a wallpaper
            // painted from them, with a solid tile standing on it as the home screen's widgets do,
            // your two accents on the tile.
            Box(
                Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(CardKind.Grouped.shape)
                    .background(Brush.linearGradient(preview.backdrop)),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    Modifier
                        .padding(horizontal = 18.dp)
                        .fillMaxWidth()
                        .solidCard(CardKind.Grouped, tile.card)
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
                        color = tile.words,
                        fontSize = 16.sp,
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

            Section {
                ToggleRow(
                    title = "Light text",
                    subtitle = "On for dark backgrounds, off for pale ones.",
                    checked = settings.customDark,
                ) { on -> onChange { it.copy(customDark = on, paletteId = Palettes.CUSTOM) } }
            }

            // Offered until the theme is in use, and folded away once it is.
            AnimatedVisibility(
                visible = settings.paletteId != Palettes.CUSTOM,
                enter = unfoldIn(LocalMotionLevel.current.reduced),
                exit = unfoldOut(LocalMotionLevel.current.reduced),
            ) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    ZenithButton(
                        text = "Use my theme",
                        onClick = { onChange { it.copy(paletteId = Palettes.CUSTOM) } },
                        kind = ButtonKind.Main,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** The corners of a colour's swatch: those of the square at the start of a Settings row. */
private val SWATCH = RoundedCornerShape(7.dp)

/**
 * One colour, on a card of its own: a row with a swatch of it, its name, and its code in grey, as a
 * row of Settings is set out, and under that the three rails it is made from: its hue, how strong it
 * is, and how bright.
 */
@Composable
private fun ColourField(label: String, value: Long, onChange: (Long) -> Unit) {
    val apple = LocalApple.current
    val hsv = remember(value) { value.toHsv() }
    var hue by remember(value) { mutableStateOf(hsv[0]) }
    var saturation by remember(value) { mutableStateOf(hsv[1]) }
    var brightness by remember(value) { mutableStateOf(hsv[2]) }

    fun emit() = onChange(hsvToArgb(hue, saturation, brightness))

    Section {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Apple's hairline round it, so a colour as pale as the card, or as dark, still shows its edge.
            Box(
                Modifier
                    .size(30.dp)
                    .clip(SWATCH)
                    .background(Color(value.toInt()))
                    .border(1.dp, apple.separator, SWATCH)
            )
            Spacer(Modifier.width(14.dp))
            Text(label, color = apple.words, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text(
                text = "#%06X".format(value.toInt() and 0xFFFFFF),
                color = apple.secondary,
                fontSize = 15.sp,
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
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
}

/**
 * A bar of the colours one rail runs through, with a white knob to drag along it, as an iPhone's
 * colour picker has. A tap glides the knob to where you touched, a drag is followed almost exactly,
 * and for as long as a finger is on the bar the knob stands a little larger.
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
 *
 * Locked, it asks for the PIN in Apple's box on a card of its own. Open, it says in grey what hiding
 * does, offers a PIN as a row of a card, and lists every app on one card, each row a switch, with a
 * hairline under the names between each two.
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
    val apple = LocalApple.current
    val reduced = LocalMotionLevel.current.reduced
    val tick = rememberTick()
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
                Column {
                    Note("Enter your PIN to see the list.", Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp))
                    Spacer(Modifier.height(12.dp))
                    PinField(value = entry, onChange = { entry = it }, modifier = Modifier.shake(refusals))
                    Spacer(Modifier.height(16.dp))
                    ZenithButton(
                        text = "Unlock",
                        onClick = {
                            if (entry == settings.hiddenPin) {
                                unlocked = true
                            } else {
                                entry = ""
                                refusals++
                            }
                        },
                        kind = ButtonKind.Main,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Column {
                    // What hiding does, and what it does not, in grey over the cards.
                    Note(
                        "Hidden apps stay out of the drawer and out of search. They are not locked " +
                            "on your phone — anything else that lists your apps still shows them.",
                        Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp),
                    )

                    // The row that offers a PIN turns into the box to type one in, and back.
                    AnimatedContent(
                        targetState = editingPin,
                        transitionSpec = { wordsChange(reduced) },
                        label = "hiddenPin",
                    ) { editing ->
                        if (editing) {
                            Column {
                                Spacer(Modifier.height(12.dp))
                                PinField(value = entry, onChange = { entry = it }, placeholder = "New PIN")
                                Spacer(Modifier.height(12.dp))
                                ZenithButton(
                                    text = if (entry.isBlank()) "Remove PIN" else "Save PIN",
                                    onClick = {
                                        onSetPin(entry)
                                        entry = ""
                                        editingPin = false
                                    },
                                    // Saving a PIN is the thing to do here; an empty box takes the
                                    // PIN away, a second choice, and the button turns grey to say so.
                                    kind = if (entry.isBlank()) ButtonKind.Grey else ButtonKind.Main,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        } else {
                            Section {
                                CardRows {
                                    MenuRow(
                                        icon = Icons.Rounded.Lock,
                                        label = if (settings.hiddenPin.isEmpty()) "Set a PIN" else "Change PIN",
                                        subtitle = if (settings.hiddenPin.isEmpty()) "Optional" else "Asked for before this list opens",
                                    ) { editingPin = true }
                                }
                            }
                        }
                    }

                    Section(header = "Apps") {
                        LazyColumn(Modifier.heightIn(max = 320.dp)) {
                            itemsIndexed(apps, key = { _, app -> app.key }) { index, app ->
                                val hidden = app.key in settings.hiddenApps
                                val touch = remember { MutableInteractionSource() }
                                val pressed by touch.collectIsPressedAsState()
                                // Hidden, its icon fades back, so the list shows at a glance which
                                // apps are out of the drawer.
                                val shownAs = animateFloatAsState(if (hidden) 0.5f else 1f, tween(260), label = "hiddenIcon")
                                Column {
                                    // Between each two rows, Apple's hairline, starting under the names.
                                    if (index > 0) GroupDivider()
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            // The whole row is the switch, as on Settings' rows: a
                                            // finger anywhere on it is felt by the knob.
                                            .toggleable(value = hidden, interactionSource = touch, indication = null, role = Role.Switch) {
                                                tick()
                                                onToggle(app.key)
                                            }
                                            .heightIn(min = 52.dp)
                                            .padding(horizontal = 16.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        AppIcon(
                                            key = app.key,
                                            label = app.label,
                                            size = 30.dp,
                                            shape = ZenithShapes.forIconShape(settings.iconShape),
                                            modifier = Modifier.graphicsLayer { alpha = shownAs.value },
                                        )
                                        Spacer(Modifier.width(14.dp))
                                        Text(
                                            app.label,
                                            color = apple.words,
                                            fontSize = 16.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Toggle(hidden, pressed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The box a PIN or a code is typed into: Apple's, on a card of its own, the digits large and in the
 * middle, with [placeholder] in grey until something is typed. A PIN takes digits only, six at most,
 * from the number pad; a code takes up to twenty-four letters and numbers.
 */
@Composable
private fun PinField(
    value: String,
    onChange: (String) -> Unit,
    digitsOnly: Boolean = true,
    placeholder: String = if (digitsOnly) "PIN" else "Code",
    modifier: Modifier = Modifier,
) {
    FieldBox(
        value = value,
        onValueChange = { text ->
            onChange(if (digitsOnly) text.filter { c -> c.isDigit() }.take(6) else text.take(24))
        },
        placeholder = placeholder,
        modifier = modifier,
        look = FieldLook.Card,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (digitsOnly) KeyboardType.NumberPassword else KeyboardType.Ascii,
        ),
        fontSize = 20.sp,
        textAlign = TextAlign.Center,
    )
}
