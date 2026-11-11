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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.Plus
import com.gabriel.zenithui.data.PlusStatus
import com.gabriel.zenithui.data.SubscriptionPlan
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.GlassButton
import com.gabriel.zenithui.ui.common.SectionLabel
import com.gabriel.zenithui.ui.common.AppIcon
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.popIn
import com.gabriel.zenithui.ui.common.popOut
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.shake
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.common.unfoldIn
import com.gabriel.zenithui.ui.common.unfoldOut
import com.gabriel.zenithui.ui.common.wordsChange
import com.gabriel.zenithui.ui.settings.Toggle
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import com.gabriel.zenithui.ui.theme.inkOnTint

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
    onSubscribe: (SubscriptionPlan) -> Unit,
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

                // The plans one above the other, shortest first and the pass marked: three side by
                // side would leave no room for a price in a longer currency. Until Play has the
                // products they are shown but greyed, with the reason underneath, rather than a
                // dead button; a plan Play's subscription does not carry is not shown at all.
                Column(Modifier.fillMaxWidth().arrive(3), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Both plans are the one subscription, so a subscriber is offered neither again.
                    if (!status.subscribed) {
                        if (status.monthlyOffered) {
                            PlanCard(
                                title = "1 month",
                                price = status.monthlyPrice,
                                per = "every month, cancel any time",
                                badge = null,
                                featured = false,
                                enabled = status.purchasable,
                                onClick = { onSubscribe(SubscriptionPlan.MONTHLY) },
                            )
                        }
                        if (status.quarterlyOffered) {
                            PlanCard(
                                title = "3 months",
                                price = status.quarterlyPrice,
                                per = "every 3 months, cancel any time",
                                badge = null,
                                featured = false,
                                enabled = status.purchasable,
                                onClick = { onSubscribe(SubscriptionPlan.QUARTERLY) },
                            )
                        }
                    }
                    PlanCard(
                        title = "Forever",
                        price = status.lifetimePrice,
                        per = "once, yours for good",
                        badge = "Best value",
                        featured = true,
                        enabled = status.purchasable,
                        onClick = onBuyLifetime,
                    )
                }

                Text(
                    text = when {
                        !status.purchasable -> "Buying opens once Zenith is on Google Play. The free trial works right now."
                        status.subscribed -> "Buying the pass does not cancel the subscription — do that in Play, and you keep Plus either way."
                        // The pass is set against the monthly plan only while there is one to compare.
                        else -> listOfNotNull(
                            Plus.payOnce(status.amounts)?.takeIf { status.monthlyOffered },
                            "No ads, ever.",
                        ).joinToString(" ")
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

/**
 * One icon per entry in Plus.FEATURES, in the same order, so an entry taken out of one is taken out
 * of the other. Setup shows them too. An entry with none here (the island) has the star.
 */
internal val FEATURE_ICONS: List<ImageVector> = listOf(
    Icons.Rounded.VisibilityOff,
    Icons.Rounded.Save,
)

/**
 * The free week's terms in one line, from the prices as they stand: what follows it, and every
 * plan on offer. Play's own sheet says this once the trial subscription exists; until then the
 * week is counted here, and the wording has to be true in both cases. Setup quotes the same line,
 * so the two can never disagree about a price.
 */
internal fun trialTerms(status: PlusStatus): String {
    val days = Plus.TRIAL_DAYS
    val others = listOfNotNull(
        "${status.quarterlyPrice} every 3 months".takeIf { status.quarterlyOffered },
        "${status.lifetimePrice} once",
    )
    return if (status.playTrialOffer) {
        // Play's trial is the monthly plan's, so it is the monthly price that follows it.
        "Free for $days days through Google Play, then ${status.monthlyPrice} a month. " +
            "Cancel any time in Play. Or pay ${choices(others)}."
    } else {
        val all = listOfNotNull("${status.monthlyPrice} a month".takeIf { status.monthlyOffered }) + others
        "Free for $days days, no card needed. After that it's ${choices(all)}."
    }
}

/** The plans as anyone would say them: "a, b, or c", the last after an "or". */
private fun choices(plans: List<String>): String =
    if (plans.size < 2) plans.joinToString() else plans.dropLast(1).joinToString(", ") + ", or " + plans.last()

/** The header: what Plus is, and where you stand with it. */
@Composable
private fun PlusHero(status: PlusStatus, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    val statusLine = when {
        status.lifetime -> "Unlocked for good. Thank you — it follows you to every phone you sign in to."
        // Play does not tell the app which of the two plans it is, so no price is quoted here.
        status.subscribed -> "Subscribed through Google Play. Everything is on."
        status.trialAvailable -> trialTerms(status)
        status.trialDaysLeft > 1 -> "${status.trialDaysLeft} days left of your free trial. Everything is on."
        status.trialDaysLeft == 1 -> "Last day of your free trial. Everything is on."
        else -> "Your free week is over. Pick whichever suits you."
    }
    val inTrial = !status.purchased && !status.trialAvailable && status.trialDaysLeft > 0

    // A plain plate in the words' ink, as the rest of the sheet is: the blue is kept for what can be
    // tapped, and there is no theme's colour left to dress the card in.
    Column(
        modifier
            .fillMaxWidth()
            .clip(ZenithShapes.card)
            .background(palette.onGlass.copy(alpha = 0.06f))
            .border(1.dp, palette.onGlass.copy(alpha = 0.10f), ZenithShapes.card)
            .padding(18.dp),
    ) {
        Box(
            Modifier
                .size(46.dp)
                // The star lands a beat after the card it is on, with a bounce of its own.
                .appearIn(delayMillis = 180, fromScale = 0.3f, riseFrom = 0f)
                .clip(RoundedCornerShape(15.dp))
                .background(palette.onGlass.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = palette.onGlass, modifier = Modifier.size(25.dp))
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
                        .background(palette.onGlass)
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
                .background(palette.onGlass.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = palette.onGlass, modifier = Modifier.size(20.dp))
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

/**
 * One of the ways to pay, a row across the sheet: what it is on the left, and the price on the
 * right, where a price in any currency has the room it needs. The featured one carries a badge
 * beside its name and an accent rim.
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
    val palette = LocalPalette.current
    Row(
        modifier
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                if (badge != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = badge,
                        color = inkOnTint(palette.accent),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(ZenithShapes.pill)
                            .background(palette.accent)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(per, color = palette.onGlassMuted, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = price,
            color = palette.onGlass,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
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
        textStyle = TextStyle(fontFamily = LocalZenithFont.current, color = palette.onGlass, fontSize = 20.sp, textAlign = TextAlign.Center),
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
