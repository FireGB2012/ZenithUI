package com.gabriel.zenithui.ui.connect

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.connect.ServerState
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.popIn
import com.gabriel.zenithui.ui.common.popOut
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.theme.GlassMaterial
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import com.gabriel.zenithui.ui.theme.glass
import com.gabriel.zenithui.ui.theme.inkOnTint
import com.gabriel.zenithui.ui.theme.themed

// Connect's own small parts. Everything it is made of is Zenith's: its panes are the same Liquid
// Glass as the home screen's widgets and dock (Modifier.glass), standing on your own wallpaper, and
// in a theme that is not glass they are that theme's panels, as everywhere else in Zenith.

/** A card's corners, as Connect's cards have them. */
internal val CARD = RoundedCornerShape(28.dp)

/** A smaller pane's: a row, a field. */
internal val ROW = RoundedCornerShape(20.dp)

internal val PILL = RoundedCornerShape(percent = 50)

/**
 * A pane of Connect's: Zenith's Liquid Glass over the wallpaper behind it, lined up with it, with
 * its rim of light that moves as the phone tilts. [interactive] lights it under a finger, as a
 * widget is lit.
 */
@Composable
internal fun Modifier.pane(
    shape: Shape = CARD,
    interactive: Boolean = false,
    material: GlassMaterial = GlassMaterial.Regular,
    strength: Float = 1f,
    shadow: Boolean = true,
): Modifier = glass(themed(shape), strength = strength, material = material, interactive = interactive, shadow = shadow)

/** A button of glass: clear, or the accent's tinted glass for the one that does the thing ([tinted]). */
@Composable
internal fun ConnectButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tinted: Boolean = false,
    enabled: Boolean = true,
    height: Dp = 50.dp,
) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    val fill = palette.accent
    val ink = if (tinted) inkOnTint(fill) else palette.onGlass
    Row(
        modifier
            .height(height)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .glass(
                themed(PILL),
                tint = if (tinted) fill else Color.Unspecified,
                material = if (tinted) GlassMaterial.Clear else GlassMaterial.Regular,
                interactive = enabled,
                tinted = tinted,
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
            ) {
                tick()
                onClick()
            }
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A round glass button with a symbol on it: back, copy, send. */
@Composable
internal fun RoundButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 42.dp) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    Box(
        modifier
            .size(size)
            .pressBounce(0.88f)
            .glass(CircleShape, material = GlassMaterial.Clear, interactive = true)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = label,
            ) {
                tick()
                onClick()
            }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = palette.onGlass, modifier = Modifier.size(size * 0.48f))
    }
}

/**
 * A choice that is on or off, as a capsule: on, it is the accent's tinted glass with a tick; off,
 * clear glass. Used for Discover's filters and for what a setup puts on.
 */
@Composable
internal fun Chip(text: String, on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    val ink by animateColorAsState(if (on) inkOnTint(palette.accent) else palette.onGlass, tween(180), label = "chipInk")
    Row(
        modifier
            .height(38.dp)
            .pressBounce(0.94f)
            .glass(
                themed(PILL),
                tint = if (on) palette.accent else Color.Unspecified,
                material = GlassMaterial.Clear,
                tinted = on,
                interactive = true,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
            ) {
                tick()
                onClick()
            }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** The heading over a block of a page, with a small note beside it, standing on the heading's own line. */
@Composable
internal fun SectionHead(title: String, modifier: Modifier = Modifier, note: String? = null) {
    val palette = LocalPalette.current
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text(
            title,
            color = palette.onGlass,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp,
            modifier = Modifier.alignByBaseline(),
        )
        if (note != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                note,
                color = palette.onGlassMuted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.alignByBaseline(),
            )
        }
    }
}

/**
 * One line of words at [most] size, made a little smaller (down to [least]) where they would not
 * fit, and cut short with an ellipsis only past that: a setup's name on a card, whole where it can be.
 */
@Composable
internal fun FitText(
    text: String,
    color: Color,
    most: TextUnit,
    least: TextUnit,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val base = LocalTextStyle.current
    BoxWithConstraints(modifier) {
        val width = constraints.maxWidth
        val size = remember(text, width, base, most, least, fontWeight) {
            var fitted = most.value
            while (fitted > least.value) {
                val style = base.merge(TextStyle(fontSize = fitted.sp, fontWeight = fontWeight))
                if (measurer.measure(text, style, softWrap = false, maxLines = 1).size.width <= width) break
                fitted -= 0.5f
            }
            fitted.coerceAtLeast(least.value).sp
        }
        Text(text, color = color, fontSize = size, fontWeight = fontWeight, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Small words under something, in the muted ink. */
@Composable
internal fun Note(text: String, modifier: Modifier = Modifier, colour: Color = LocalPalette.current.onGlassMuted) {
    Text(text, color = colour, fontSize = 13.sp, lineHeight = 18.sp, modifier = modifier)
}

/** The dot's colour for how the server stands: green up, amber asking, red out of reach, grey with none. */
@Composable
internal fun serverColour(state: ServerState): Color = when (state) {
    ServerState.UP -> LocalPalette.current.switchOn
    ServerState.ASKING -> Color(0xFFFFC53D)
    ServerState.DOWN -> LocalPalette.current.destructive
    ServerState.NONE -> LocalPalette.current.onGlassMuted
}

/**
 * A box to type in, on a pane of its own, ringed in the accent while it has the keyboard. A
 * [secret] one is dots until the eye at its end is tapped.
 */
@Composable
internal fun ConnectField(
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    secret: Boolean = false,
    capitals: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    action: ImeAction = ImeAction.Done,
    onAction: () -> Unit = {},
    singleLine: Boolean = true,
    minHeight: Dp = 50.dp,
    /** To put the keyboard on it from outside: a sheet that is for typing does as it opens. */
    focus: FocusRequester? = null,
) {
    val palette = LocalPalette.current
    var focused by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(false) }
    val ring by animateColorAsState(if (focused) palette.accent.copy(alpha = 0.8f) else Color.Transparent, tween(220), label = "fieldRing")
    val shape = themed(RoundedCornerShape(18.dp))
    Row(
        modifier
            .fillMaxWidth()
            .height(if (singleLine) minHeight else minHeight * 2.4f)
            .clip(shape)
            .background(palette.onGlass.copy(alpha = 0.09f))
            .border(1.5.dp, ring, shape)
            .padding(start = 16.dp, end = if (secret) 6.dp else 16.dp, top = if (singleLine) 0.dp else 12.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
    ) {
        Box(Modifier.weight(1f), contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart) {
            if (value.isEmpty()) Text(hint, color = palette.onGlassMuted, fontSize = 15.sp, maxLines = if (singleLine) 1 else 4)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = singleLine,
                textStyle = TextStyle(fontFamily = LocalZenithFont.current, color = palette.onGlass, fontSize = 15.sp, lineHeight = 20.sp),
                cursorBrush = SolidColor(palette.accent),
                visualTransformation = if (secret && !shown) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(
                    capitalization = if (secret) KeyboardCapitalization.None else capitals,
                    autoCorrectEnabled = !secret && capitals == KeyboardCapitalization.Sentences,
                    keyboardType = if (secret) KeyboardType.Password else KeyboardType.Text,
                    imeAction = action,
                ),
                keyboardActions = KeyboardActions(onDone = { onAction() }, onGo = { onAction() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (focus != null) Modifier.focusRequester(focus) else Modifier)
                    .onFocusChanged { focused = it.isFocused }
                    .semantics { contentDescription = hint },
            )
        }
        if (secret) {
            val reduced = LocalMotionLevel.current.reduced
            Box(
                Modifier
                    .size(38.dp)
                    .pressBounce(0.86f)
                    .clip(CircleShape)
                    .clickable { shown = !shown },
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = shown,
                    transitionSpec = { popIn(reduced, from = 0.5f).togetherWith(popOut(reduced, to = 0.5f)) },
                    label = "passwordEye",
                ) { open ->
                    Icon(
                        if (open) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (open) "Hide password" else "Show password",
                        tint = palette.onGlassMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** Puts [text] on the clipboard. */
internal fun copyText(context: Context, label: String, text: String) {
    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(label, text))
}

/** Hands [text] to another app (a message, an email), through Android's own chooser. */
internal fun sendText(context: Context, text: String, title: String, subject: String? = null) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    if (subject != null) send.putExtra(Intent.EXTRA_SUBJECT, subject)
    runCatching { context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/**
 * A card that says how something stands, with a symbol in a round well beside it and, given
 * [action], a button for what can be done about it: the server out of reach, nothing saved yet.
 */
@Composable
internal fun InfoCard(
    icon: ImageVector,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    actionIcon: ImageVector? = null,
    tinted: Boolean = false,
    onAction: () -> Unit = {},
) {
    val palette = LocalPalette.current
    Row(
        modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .pane(CARD)
            .padding(18.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(palette.onGlass.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = palette.onGlass, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(14.dp))
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(title, color = palette.onGlass, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Note(text)
            if (action != null) {
                Spacer(Modifier.height(12.dp))
                ConnectButton(action, onAction, icon = actionIcon, tinted = tinted, height = 42.dp)
            }
        }
    }
}

/** Taps here do [onClick], with a tick and no ripple: the glass under it lights instead. */
@Composable
internal fun Modifier.tap(label: String, onClick: () -> Unit): Modifier {
    val tick = rememberTick()
    return clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        role = Role.Button,
        onClickLabel = label,
    ) {
        tick()
        onClick()
    }
}
