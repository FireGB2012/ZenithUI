package com.gabriel.zenithui.ui.home

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.Look
import com.gabriel.zenithui.data.LookRule
import com.gabriel.zenithui.data.LookSchedule
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.ZenithButton
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rollOver
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.common.unfoldIn
import com.gabriel.zenithui.ui.common.unfoldOut
import com.gabriel.zenithui.ui.settings.CardRows
import com.gabriel.zenithui.ui.settings.ControlRow
import com.gabriel.zenithui.ui.settings.FieldBox
import com.gabriel.zenithui.ui.settings.FieldLook
import com.gabriel.zenithui.ui.settings.Section
import com.gabriel.zenithui.ui.settings.rowPress
import com.gabriel.zenithui.ui.theme.LocalApple
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.inkOnTint
import com.gabriel.zenithui.ui.theme.solidCard
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity

/**
 * One line to type in, as a row of the card it stands on: Apple's box ([FieldBox]), the words in the
 * words' colour and the cursor in the accent. With [words] the keyboard starts each word with a
 * capital, as a name has; without, it types capitals only, as a code is written.
 */
@Composable
private fun LineField(value: String, placeholder: String, onChange: (String) -> Unit, words: Boolean = true) {
    FieldBox(
        value = value,
        onValueChange = onChange,
        placeholder = placeholder,
        look = FieldLook.Row,
        keyboardOptions = KeyboardOptions(
            capitalization = if (words) KeyboardCapitalization.Words else KeyboardCapitalization.Characters,
            imeAction = ImeAction.Done,
        ),
    )
}

/** A name for a Look: a new one, or a new name for one saved already. It is typed on a card, with the button that saves it under. */
@Composable
fun LookNameSheet(visible: Boolean, title: String, initial: String, action: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember(initial, visible) { mutableStateOf(initial) }
    ZenithSheet(visible = visible, onDismiss = onDismiss, title = title) {
        Section {
            LineField(name, "Its name: Night, Work, Summer", { name = it.take(30) })
        }
        Spacer(Modifier.height(16.dp))
        ZenithButton(text = action, filled = true, enabled = name.isNotBlank(), onClick = { onSave(name) }, modifier = Modifier.fillMaxWidth())
    }
}

/**
 * What can be done with one saved Look, on cards as an iPhone groups a menu: put it on, save the
 * home screen over it, or rename it; share it (with an account), a shared Look showing its code on
 * a card of its own, to copy or send, with the way to stop sharing it under; and, on a card of its
 * own since it cannot be undone, delete it. Why sharing did not work is said in red under its card.
 */
@Composable
fun LookMenuSheet(
    visible: Boolean,
    look: Look,
    canShare: Boolean,
    onPut: () -> Unit,
    onUpdate: () -> Unit,
    onRename: () -> Unit,
    /** Shares it, or with `again` shares it as it is now under a new code: the code, or why not. */
    onShare: suspend (again: Boolean) -> Pair<String?, String?>,
    /** Takes its code back: why not, or null. */
    onStopSharing: suspend () -> String?,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val reduced = LocalMotionLevel.current.reduced
    val scope = rememberCoroutineScope()
    var busy by remember(look.id) { mutableStateOf(false) }
    var problem by remember(look.id) { mutableStateOf<String?>(null) }
    var armed by remember(look.id) { mutableStateOf(false) }
    // Kept while the code folds away after sharing stops, so it does not empty out as it goes.
    var shownCode by remember(look.id) { mutableStateOf(look.shared) }
    LaunchedEffect(look.shared) { if (look.shared.isNotEmpty()) shownCode = look.shared }

    fun ask(work: suspend () -> String?) {
        if (busy) return
        busy = true
        problem = null
        scope.launch {
            problem = work()
            busy = false
        }
    }

    ZenithSheet(visible = visible, onDismiss = onDismiss, title = look.name) {
        // On a short screen the cards scroll, so the last of them is never cut off.
        Column(Modifier.verticalScroll(rememberScrollState())) {
            // Each row a beat behind the one above it, so the menu is read from the top as it arrives.
            var row = 0
            Section {
                CardRows {
                    MenuRow(Icons.Rounded.CheckCircle, "Put it on", "The home screen, its colours and its wallpaper become this look", index = row++) { onPut() }
                    MenuRow(Icons.Rounded.Save, "Save the home screen over it", "Keeps its name; what is on the home screen now becomes the look", index = row++) { onUpdate(); onDismiss() }
                    MenuRow(Icons.Rounded.Edit, "Rename", index = row++) { onRename() }
                }
            }
            if (canShare) {
                // The code, once there is one, on a card of its own between what else can be done
                // and the ways to share it.
                AnimatedVisibility(visible = look.shared.isNotEmpty(), enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
                    SharedCode(code = shownCode, old = look.sharedIsOld)
                }
                Section {
                    CardRows {
                        if (look.shared.isEmpty()) {
                            MenuRow(Icons.Rounded.Share, if (busy) "Sharing…" else "Share", "Gets a code for it, without your name, notes, lists or photos", index = row++) {
                                ask { onShare(false).second }
                            }
                        } else {
                            if (look.sharedIsOld) {
                                MenuRow(Icons.Rounded.Share, if (busy) "Sharing…" else "Share how it is now", "A new code; the old one stops working", index = row++) {
                                    ask { onShare(true).second }
                                }
                            }
                            MenuRow(Icons.Rounded.LinkOff, if (busy) "Stopping…" else "Stop sharing", "The code opens nothing any more", index = row++) {
                                ask { onStopSharing() }
                            }
                        }
                    }
                }
                LookProblem(problem)
            }
            // What cannot be undone, on a card of its own, as an iPhone sets it apart.
            Section {
                MenuRow(
                    Icons.Rounded.Delete,
                    if (armed) "Tap again to delete it" else "Delete",
                    if (look.shared.isNotEmpty()) "The look, and its code: nothing on the home screen changes" else "The look only: nothing on the home screen changes",
                    danger = true,
                    index = row,
                ) {
                    if (armed) {
                        onDelete()
                        onDismiss()
                    } else {
                        armed = true
                    }
                }
            }
        }
    }
}

/**
 * A Look's code on a card of its own, big enough to read out, with a button in the accent to copy it
 * and one to send it. What the code gives is said in grey under the card.
 */
@Composable
private fun SharedCode(code: String, old: Boolean) {
    val apple = LocalApple.current
    val context = LocalContext.current
    val shown = "LOOK-$code"
    Section(footer = if (old) "Gives the look as it was when you shared it." else "Anybody with the code can put this look on.") {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(shown, color = apple.words, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            CodeAction(Icons.Rounded.ContentCopy, "Copy the code") {
                val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Zenith look", shown))
            }
            CodeAction(Icons.Rounded.Share, "Send the code") {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, "My home screen in Zenith UI: open Settings, Looks, Open a code, and type $shown")
                runCatching { context.startActivity(Intent.createChooser(send, "Share the look").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
    }
}

/** A small action on a card, as an iPhone draws one: a symbol in the accent that dims while it is held. */
@Composable
private fun CodeAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    val accent = LocalPalette.current.accent
    val touch = remember { MutableInteractionSource() }
    val pressed by touch.collectIsPressedAsState()
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(interactionSource = touch, indication = null, role = Role.Button, onClick = onClick)
            .graphicsLayer { alpha = if (pressed) 0.35f else 1f },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = accent, modifier = Modifier.size(20.dp))
    }
}

/**
 * Why what was asked did not happen, in Apple's red, under the card it was asked from, where that
 * card's footnote would stand. It unfolds as it comes and folds away as it goes, still saying what
 * it said.
 */
@Composable
private fun LookProblem(text: String?) {
    val reduced = LocalMotionLevel.current.reduced
    // The last words it had, to fold away with rather than an empty line. Not state: it is only
    // read as the words are composed.
    val said = remember { arrayOf(text) }
    if (text != null) said[0] = text
    AnimatedVisibility(visible = text != null, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
        Text(
            said[0].orEmpty(),
            color = LocalApple.current.danger,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 7.dp),
        )
    }
}

/**
 * A time for a Look to come on by itself, on cards: which Look, from when to when, and on which
 * days, with what that comes to in grey under the days. The Looks and the days are Apple's grey
 * chips, the chosen ones filled with the accent. Hours and minutes are stepped a quarter of an
 * hour at a time, the numbers rolling as they go.
 */
@Composable
fun LookRuleSheet(visible: Boolean, looks: List<Look>, onAdd: (LookRule) -> Unit, onDismiss: () -> Unit) {
    val palette = LocalPalette.current
    val apple = LocalApple.current
    var chosen by remember(visible) { mutableStateOf(looks.firstOrNull()?.id) }
    var start by remember(visible) { mutableIntStateOf(22 * 60) }
    var end by remember(visible) { mutableIntStateOf(7 * 60) }
    var days by remember(visible) { mutableStateOf((1..7).toSet()) }

    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Add a time", maxHeightFraction = 0.86f) {
        // On a short screen the cards scroll, and Add stays in sight under them.
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            Section(header = "Look", modifier = Modifier.appearIn(delayMillis = staggerDelay(0), riseFrom = 14f)) {
                // The Looks run to the card's edges as they scroll, the first starting where a row's words do.
                ControlRow(edgeToEdge = true) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                    ) {
                        items(looks, key = { it.id }) { look ->
                            val on = look.id == chosen
                            val fill by animateColorAsState(if (on) palette.accent else apple.raised, tween(220), label = "ruleLook")
                            val ink by animateColorAsState(if (on) inkOnTint(palette.accent) else apple.words, tween(220), label = "ruleLookInk")
                            Text(
                                look.name,
                                color = ink,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .pressBounce(0.94f)
                                    .clip(RoundedCornerShape(50))
                                    .background(fill)
                                    .clickable(interactionSource = null, indication = null) { chosen = look.id }
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                            )
                        }
                    }
                }
            }
            Section(modifier = Modifier.appearIn(delayMillis = staggerDelay(1), riseFrom = 14f)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TimeStepper("From", start, { start = it }, Modifier.weight(1f))
                    TimeStepper("Until", end, { end = it }, Modifier.weight(1f))
                }
            }
            Section(
                header = "Days",
                footer = "${LookSchedule.clock(start)} to ${LookSchedule.clock(end)}, ${LookSchedule.days(days).lowercase()}" +
                    if (end <= start) ", running past midnight." else ".",
                modifier = Modifier.appearIn(delayMillis = staggerDelay(2), riseFrom = 14f),
            ) {
                ControlRow {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { i, letter ->
                            val day = i + 1
                            val on = day in days
                            val fill by animateColorAsState(if (on) palette.accent else apple.raised, tween(200), label = "ruleDay")
                            val ink by animateColorAsState(if (on) inkOnTint(palette.accent) else apple.words, tween(200), label = "ruleDayInk")
                            Box(
                                Modifier
                                    // 38 across, and smaller only where seven of them would not fit side by side.
                                    .weight(1f, fill = false)
                                    .widthIn(max = 38.dp)
                                    .aspectRatio(1f)
                                    .pressBounce(0.9f)
                                    .clip(CircleShape)
                                    .background(fill)
                                    .clickable(interactionSource = null, indication = null) {
                                        days = if (on && days.size > 1) days - day else days + day
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(letter, color = ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        ZenithButton(
            text = "Add",
            filled = true,
            enabled = chosen != null && start != end,
            onClick = { chosen?.let { onAdd(LookRule(it, start, end, days)) } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * A time of day on Apple's grey, the grey of a card on a card: what it is, and the clock between a
 * minus and a plus that step it a quarter of an hour at a time, the clock rolling up or down as it
 * changes. On the narrowest screens the clock gives up room before the steps do.
 */
@Composable
private fun TimeStepper(label: String, minute: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val apple = LocalApple.current
    val reduced = LocalMotionLevel.current.reduced
    Column(
        modifier
            .solidCard(colour = apple.raised)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = apple.secondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            QuarterStep(Icons.Rounded.Remove, "$label, earlier") { onChange((minute - 15 + 1440) % 1440) }
            AnimatedContent(
                targetState = minute,
                transitionSpec = { rollOver(reduced, down = targetState < initialState) },
                modifier = Modifier.weight(1f, fill = false),
                label = "ruleTime",
            ) { m ->
                // As large as the room between the steps allows, up to 22 sp: with large text the
                // time grows smaller rather than running over the minus and the plus.
                BoxWithConstraints(contentAlignment = Alignment.Center) {
                    val fit = with(LocalDensity.current) { minOf(22.sp.toPx(), maxWidth.toPx() / 3f).toSp() }
                    Text(
                        LookSchedule.clock(m),
                        color = apple.words,
                        fontSize = fit,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            QuarterStep(Icons.Rounded.Add, "$label, later") { onChange((minute + 15) % 1440) }
        }
    }
}

/** One step of a [TimeStepper]: a round button holding a minus or a plus, that shades while it is held, as an iPhone's stepper does. */
@Composable
private fun QuarterStep(icon: ImageVector, description: String, onClick: () -> Unit) {
    val touch = remember { MutableInteractionSource() }
    // A finger's room round a smaller circle, which is what shades.
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(interactionSource = touch, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .rowPress(touch),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = description, tint = LocalApple.current.words, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * A code somebody shared: typed in, opened, and put on. Needs no account and no Plus. A Featured
 * Look comes here with its code [initial]ly filled in, so nothing replaces the home screen
 * without a second tap. The code is typed on a card, what opening it does is said in grey under
 * the card, and why it would not open in red.
 */
@Composable
fun LookCodeSheet(visible: Boolean, initial: String, open: suspend (String) -> String?, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var code by remember(visible, initial) { mutableStateOf(initial) }
    var opening by remember(visible) { mutableStateOf(false) }
    var problem by remember(visible) { mutableStateOf<String?>(null) }
    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Open a code") {
        Section(footer = "The Look comes on in place of yours. Save yours first, under Looks, to come back to it.") {
            LineField(code, "LOOK-ABC234", { code = it.take(16) }, words = false)
        }
        LookProblem(problem)
        Spacer(Modifier.height(16.dp))
        ZenithButton(
            text = if (opening) "Opening…" else "Open",
            filled = true,
            enabled = !opening && code.count { it.isLetterOrDigit() } >= 6,
            onClick = {
                opening = true
                problem = null
                scope.launch {
                    val why = open(code)
                    opening = false
                    if (why == null) onDismiss() else problem = why
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
