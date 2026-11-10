package com.gabriel.zenithui.ui.home

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.Look
import com.gabriel.zenithui.data.LookRule
import com.gabriel.zenithui.data.LookSchedule
import com.gabriel.zenithui.ui.common.GlassButton
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.SectionLabel
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rollOver
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.common.unfoldIn
import com.gabriel.zenithui.ui.common.unfoldOut
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import com.gabriel.zenithui.ui.theme.inkOnTint
import kotlinx.coroutines.launch

/** One line to type in, ringed in the accent while it has the keyboard. */
@Composable
private fun LineField(value: String, placeholder: String, onChange: (String) -> Unit, words: Boolean = true) {
    val palette = LocalPalette.current
    var focused by remember { mutableStateOf(false) }
    val ring by animateColorAsState(if (focused) palette.accent.copy(alpha = 0.75f) else Color.Transparent, tween(220), label = "lineRing")
    Box(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.onGlass.copy(alpha = 0.08f))
            .border(1.5.dp, ring, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) Text(placeholder, color = palette.onGlassMuted, fontSize = 15.sp)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(fontFamily = LocalZenithFont.current, color = palette.onGlass, fontSize = 15.sp),
            cursorBrush = SolidColor(palette.accent),
            keyboardOptions = KeyboardOptions(
                capitalization = if (words) KeyboardCapitalization.Words else KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
        )
    }
}

/** A name for a Look: a new one, or a new name for one saved already. */
@Composable
fun LookNameSheet(visible: Boolean, title: String, initial: String, action: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember(initial, visible) { mutableStateOf(initial) }
    ZenithSheet(visible = visible, onDismiss = onDismiss, title = title) {
        LineField(name, "Its name: Night, Work, Summer", { name = it.take(30) })
        Spacer(Modifier.height(16.dp))
        GlassButton(text = action, filled = true, enabled = name.isNotBlank(), onClick = { onSave(name) }, modifier = Modifier.fillMaxWidth())
    }
}

/**
 * What can be done with one saved Look: put it on, save the home screen over it, rename it,
 * share it (with an account), or delete it. A shared Look shows its code here, to copy or send,
 * and the way to stop sharing it.
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
        var row = 0
        MenuRow(Icons.Rounded.CheckCircle, "Put it on", "The home screen, its colours and its wallpaper become this look", index = row++) { onPut() }
        MenuRow(Icons.Rounded.Save, "Save the home screen over it", "Keeps its name; what is on the home screen now becomes the look", index = row++) { onUpdate(); onDismiss() }
        MenuRow(Icons.Rounded.Edit, "Rename", index = row++) { onRename() }
        if (canShare) {
            // The code, once there is one, between sharing and what else can be done with it.
            AnimatedVisibility(visible = look.shared.isNotEmpty(), enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
                SharedCode(code = shownCode, old = look.sharedIsOld)
            }
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
            AnimatedVisibility(visible = problem != null, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
                Text(problem.orEmpty(), color = Color(0xFFFF6B6B), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
            }
        }
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

/** A Look's code, big enough to read out, with buttons to copy it and to send it. */
@Composable
private fun SharedCode(code: String, old: Boolean) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val shown = "LOOK-$code"
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(palette.accent.copy(alpha = 0.12f))
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(shown, color = palette.onGlass, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Box(
                Modifier.size(40.dp).pressBounce().clip(CircleShape).clickable {
                    val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Zenith look", shown))
                },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy the code", tint = palette.onGlass, modifier = Modifier.size(20.dp)) }
            Box(
                Modifier.size(40.dp).pressBounce().clip(CircleShape).clickable {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, "My home screen in Zenith UI: open Settings, Looks, Open a code, and type $shown")
                    runCatching { context.startActivity(Intent.createChooser(send, "Share the look").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Share, contentDescription = "Send the code", tint = palette.onGlass, modifier = Modifier.size(20.dp)) }
        }
        Text(
            if (old) "Gives the look as it was when you shared it." else "Anybody with the code can put this look on.",
            color = palette.onGlassMuted,
            fontSize = 12.sp,
        )
    }
}

/**
 * A time for a Look to come on by itself: which Look, from when to when, and on which days.
 * Hours and minutes are stepped a quarter of an hour at a time, the numbers rolling as they go.
 */
@Composable
fun LookRuleSheet(visible: Boolean, looks: List<Look>, onAdd: (LookRule) -> Unit, onDismiss: () -> Unit) {
    val palette = LocalPalette.current
    var chosen by remember(visible) { mutableStateOf(looks.firstOrNull()?.id) }
    var start by remember(visible) { mutableIntStateOf(22 * 60) }
    var end by remember(visible) { mutableIntStateOf(7 * 60) }
    var days by remember(visible) { mutableStateOf((1..7).toSet()) }

    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Add a time", maxHeightFraction = 0.86f) {
        SectionLabel("Look", Modifier.padding(start = 6.dp, bottom = 8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.appearIn(delayMillis = staggerDelay(0), riseFrom = 14f)) {
            items(looks, key = { it.id }) { look ->
                val on = look.id == chosen
                val fill by animateColorAsState(if (on) palette.accent else palette.onGlass.copy(alpha = 0.08f), tween(220), label = "ruleLook")
                Text(
                    look.name,
                    color = if (on) inkOnTint(palette.accent) else palette.onGlass,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .pressBounce(0.94f)
                        .clip(RoundedCornerShape(50))
                        .background(fill)
                        .clickable { chosen = look.id }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.appearIn(delayMillis = staggerDelay(1), riseFrom = 14f)) {
            TimeStepper("From", start, { start = it }, Modifier.weight(1f))
            TimeStepper("Until", end, { end = it }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        SectionLabel("Days", Modifier.padding(start = 6.dp, bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().appearIn(delayMillis = staggerDelay(2), riseFrom = 14f)) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { i, letter ->
                val day = i + 1
                val on = day in days
                val fill by animateColorAsState(if (on) palette.accent else palette.onGlass.copy(alpha = 0.08f), tween(200), label = "ruleDay")
                Box(
                    Modifier
                        .size(38.dp)
                        .pressBounce(0.9f)
                        .clip(CircleShape)
                        .background(fill)
                        .clickable { days = if (on && days.size > 1) days - day else days + day },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(letter, color = if (on) inkOnTint(palette.accent) else palette.onGlass, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "${LookSchedule.clock(start)} to ${LookSchedule.clock(end)}, ${LookSchedule.days(days).lowercase()}" +
                if (end <= start) ", running past midnight." else ".",
            color = palette.onGlassMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        Spacer(Modifier.height(16.dp))
        GlassButton(
            text = "Add",
            filled = true,
            enabled = chosen != null && start != end,
            onClick = { chosen?.let { onAdd(LookRule(it, start, end, days)) } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** A time of day, a quarter of an hour at a time, the clock rolling up or down as it changes. */
@Composable
private fun TimeStepper(label: String, minute: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(palette.onGlass.copy(alpha = 0.06f))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = palette.onGlassMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).pressBounce().clip(CircleShape).clickable { onChange((minute - 15 + 1440) % 1440) }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Remove, contentDescription = "Earlier", tint = palette.onGlass, modifier = Modifier.size(18.dp))
            }
            androidx.compose.animation.AnimatedContent(targetState = minute, transitionSpec = { rollOver(reduced, down = targetState < initialState) }, label = "ruleTime") { m ->
                Text(LookSchedule.clock(m), color = palette.onGlass, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(72.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            Box(Modifier.size(34.dp).pressBounce().clip(CircleShape).clickable { onChange((minute + 15) % 1440) }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, contentDescription = "Later", tint = palette.onGlass, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * A code somebody shared: typed in, opened, and put on. Needs no account and no Plus. A Featured
 * Look comes here with its code [initial]ly filled in, so nothing replaces the home screen
 * without a second tap.
 */
@Composable
fun LookCodeSheet(visible: Boolean, initial: String, open: suspend (String) -> String?, onDismiss: () -> Unit) {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    var code by remember(visible, initial) { mutableStateOf(initial) }
    var opening by remember(visible) { mutableStateOf(false) }
    var problem by remember(visible) { mutableStateOf<String?>(null) }
    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Open a code") {
        Text(
            "The Look comes on in place of yours. Save yours first, under Looks, to come back to it.",
            color = palette.onGlassMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        )
        Spacer(Modifier.height(12.dp))
        LineField(code, "LOOK-ABC234", { code = it.take(16) }, words = false)
        AnimatedVisibility(visible = problem != null) {
            Text(problem.orEmpty(), color = Color(0xFFFF6B6B), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp))
        }
        Spacer(Modifier.height(16.dp))
        GlassButton(
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
