package com.gabriel.zenithui.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.AppShortcut
import com.gabriel.zenithui.data.Calculator
import com.gabriel.zenithui.data.DrawerTab
import com.gabriel.zenithui.data.SettingsLinks
import com.gabriel.zenithui.data.Units
import com.gabriel.zenithui.ui.common.AppIcon
import com.gabriel.zenithui.ui.common.ZenithButton
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.SectionLabel
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.popIn
import com.gabriel.zenithui.ui.common.popOut
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberHaptics
import com.gabriel.zenithui.ui.common.rollOver
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.settings.CardRows
import com.gabriel.zenithui.ui.settings.FieldBox
import com.gabriel.zenithui.ui.settings.GroupDivider
import com.gabriel.zenithui.ui.settings.Section
import com.gabriel.zenithui.ui.settings.rowPress
import com.gabriel.zenithui.ui.theme.ButtonKind
import com.gabriel.zenithui.ui.theme.LocalApple
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.inkOnTint
import com.gabriel.zenithui.ui.theme.solidCard
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick

/** A drawer tab, as the row of pills shows it: All, one you made, or Work. */
sealed interface TabChoice {
    data object All : TabChoice
    data class Made(val index: Int) : TabChoice
    data object Work : TabChoice
}

/**
 * The tabs along the top of the drawer: All, the ones you made, Work on a phone with a work
 * profile, and a + to make another. They are Apple's grey capsules, and the one chosen fills with
 * the accent; a tab you made is changed with a long press.
 */
@Composable
fun DrawerTabs(
    tabs: List<DrawerTab>,
    hasWork: Boolean,
    chosen: TabChoice,
    onChoose: (TabChoice) -> Unit,
    onNew: () -> Unit,
    onEdit: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val apple = LocalApple.current
    LazyRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 22.dp),
    ) {
        item(key = "all") { TabPill("All", chosen == TabChoice.All) { onChoose(TabChoice.All) } }
        items(tabs.size, key = { "tab:" + tabs[it].name }) { index ->
            TabPill(tabs[index].name, chosen == TabChoice.Made(index), onLongPress = { onEdit(index) }) { onChoose(TabChoice.Made(index)) }
        }
        if (hasWork) item(key = "work") { TabPill("Work", chosen == TabChoice.Work, icon = Icons.Rounded.Work) { onChoose(TabChoice.Work) } }
        if (tabs.size < DrawerTab.MOST) item(key = "new") {
            // A finger's room round the grey circle.
            Box(
                Modifier
                    .size(44.dp)
                    .pressBounce(0.9f)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onNew),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(apple.field),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "New tab", tint = apple.words, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun TabPill(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val apple = LocalApple.current
    val accent = LocalPalette.current.accent
    val fill by animateColorAsState(if (selected) accent else apple.field, tween(220), label = "tabFill")
    val ink by animateColorAsState(if (selected) inkOnTint(accent) else apple.words, tween(220), label = "tabInk")
    // A finger's room above and below the capsule, and a tab to TalkBack: chosen or not, opened at a
    // tap, changed with a long press.
    Box(
        Modifier
            .heightIn(min = 44.dp)
            .pointerInput(onLongPress) {
                val held = onLongPress
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = if (held == null) null else { _: androidx.compose.ui.geometry.Offset -> held() },
                )
            }
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                this.selected = selected
                this.onClick(label = null) { onClick(); true }
                val held = onLongPress
                if (held != null) onLongClick(label = "Change tab") { held(); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .height(34.dp)
                .pressBounce(0.94f)
                .clip(RoundedCornerShape(50))
                .background(fill)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(label, color = ink, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
        }
    }
}

/**
 * The drawer's index down its right edge, for an A to Z list long enough to need one: drag along
 * it, and a round solid bubble beside the finger says which letter it is on while the list jumps
 * there, with a tick at each letter. The letters near the finger swell out towards it, as the dock
 * of a Mac does, so the one under it can be seen round the thumb.
 */
@Composable
fun FastScroller(
    /** Each letter with the place in the list its first app is. */
    sections: List<Pair<Char, Int>>,
    onJump: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (sections.size < 2) return
    val palette = LocalPalette.current
    val apple = LocalApple.current
    val reduced = LocalMotionLevel.current.reduced
    val haptics = rememberHaptics()
    var held by remember { mutableStateOf<Int?>(null) }
    var fingerY by remember { mutableFloatStateOf(0f) }
    var height by remember { mutableFloatStateOf(1f) }
    // Where the finger is along the letters, as a fraction of one, for them to swell round.
    val near = remember { mutableFloatStateOf(-10f) }
    val swell by animateFloatAsState(if (held != null && !reduced) 1f else 0f, tween(180), label = "scrollerSwell")

    fun pick(y: Float) {
        fingerY = y.coerceIn(0f, height)
        val at = (fingerY / height * sections.size).coerceIn(0f, sections.size - 0.001f)
        near.floatValue = at
        val index = at.toInt()
        if (index != held) {
            held = index
            haptics.tick()
            onJump(sections[index].second)
        }
    }

    Box(modifier.width(30.dp).fillMaxHeight()) {
        Column(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .onSizeChanged { height = it.height.toFloat().coerceAtLeast(1f) }
                .pointerInput(sections) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        pick(down.position.y)
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            pick(change.position.y)
                        }
                        held = null
                    }
                },
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            sections.forEachIndexed { index, (letter, _) ->
                val chosen = index == held
                Text(
                    letter.toString(),
                    color = if (chosen) palette.accent else palette.onGlassMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.graphicsLayer {
                        val distance = abs(index + 0.5f - near.floatValue)
                        val lift = (1f - distance / 3f).coerceIn(0f, 1f) * swell
                        val s = 1f + 0.7f * lift
                        scaleX = s
                        scaleY = s
                        translationX = -14.dp.toPx() * lift
                    },
                )
            }
        }
        AnimatedVisibility(
            visible = held != null,
            enter = popIn(reduced, from = 0.4f, millis = 160),
            exit = popOut(reduced, to = 0.4f),
            modifier = Modifier.offset { IntOffset((-76).dp.roundToPx(), (fingerY - 30.dp.toPx()).roundToInt()) },
        ) {
            // Solid, as anything standing on the drawer's glass is, and lifted off it a little.
            Box(
                Modifier
                    .size(60.dp)
                    .shadow(10.dp, CircleShape)
                    .clip(CircleShape)
                    .background(apple.card),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = held?.let { sections.getOrNull(it)?.first } ?: ' ',
                    transitionSpec = { rollOver(reduced, down = targetState < initialState) },
                    label = "lensLetter",
                ) { letter ->
                    Text(letter.toString(), color = apple.words, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** The letter an app is found under: its first letter, or # for anything that is not a letter. */
fun indexLetter(label: String): Char = label.trim().firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() } ?: '#'

/**
 * What a search finds besides apps, at the top of the results, each on a card of its own: the
 * answer to a sum or a conversion, Android's settings by name, and apps' shortcuts. Each rises in
 * as it appears; a sum's answer and a conversion turn over as the typing changes them.
 */
@Composable
fun SearchAnswers(
    query: String,
    shortcuts: List<AppShortcut>,
    onCopy: (String) -> Unit,
    onSetting: (SettingsLinks.Link) -> Unit,
    onShortcut: (AppShortcut) -> Unit,
) {
    val palette = LocalPalette.current
    val apple = LocalApple.current
    val reduced = LocalMotionLevel.current.reduced
    val sum = remember(query) { Calculator.evaluate(query)?.let(Calculator::show) }
    val conversion = remember(query) { Units.convert(query) }
    val settingsFound = remember(query) { SettingsLinks.find(query) }
    val words = query.trim()
    val shortcutsFound = remember(words, shortcuts) {
        if (words.length < 2) emptyList() else shortcuts.filter { it.label.contains(words, ignoreCase = true) }.take(4)
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
        val answer = sum?.let { "= $it" to query.trim() } ?: conversion?.let { it.to to it.from }
        AnimatedVisibility(visible = answer != null, enter = popIn(reduced, from = 0.9f), exit = popOut(reduced, to = 0.9f)) {
            val shown = remember { arrayOf("" to "") }
            if (answer != null) shown[0] = answer
            val touch = remember { MutableInteractionSource() }
            Row(
                Modifier
                    .fillMaxWidth()
                    .solidCard()
                    .rowPress(touch)
                    .clickable(interactionSource = touch, indication = null, onClickLabel = "Copy") { onCopy(shown[0].first.removePrefix("= ")) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (sum != null) Icons.Rounded.Calculate else Icons.Rounded.SwapHoriz, contentDescription = null, tint = palette.accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    AnimatedContent(targetState = shown[0].first, transitionSpec = { rollOver(reduced) }, label = "answer") { value ->
                        Text(value, color = apple.words, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("${shown[0].second} · tap to copy", color = apple.secondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (settingsFound.isNotEmpty()) {
            Section(header = "Settings") {
                CardRows {
                    settingsFound.forEachIndexed { index, link ->
                        AnswerRow(index = index, onClick = { onSetting(link) }) {
                            Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.SettingsSuggest, contentDescription = null, tint = palette.accent, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Text(link.title, color = apple.words, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
        if (shortcutsFound.isNotEmpty()) {
            Section(header = "Shortcuts") {
                CardRows {
                    shortcutsFound.forEachIndexed { index, shortcut ->
                        AnswerRow(index = index, onClick = { onShortcut(shortcut) }) {
                            AppIcon(key = shortcut.key, label = shortcut.label, size = 30.dp)
                            Spacer(Modifier.width(14.dp))
                            Text(shortcut.label, color = apple.words, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        if (answer != null || settingsFound.isNotEmpty() || shortcutsFound.isNotEmpty()) {
            SectionLabel("Apps", Modifier.padding(start = 16.dp, top = 16.dp))
        }
    }
}

/** One thing a search found, on its card: rising in a beat behind the one above, and shading under a finger. */
@Composable
private fun AnswerRow(index: Int, onClick: () -> Unit, content: @Composable () -> Unit) {
    val touch = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .appearIn(delayMillis = staggerDelay(index), riseFrom = 14f, gentle = true)
            .rowPress(touch)
            .clickable(interactionSource = touch, indication = null, role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

/**
 * Making a tab, or changing one: its name, and every app to tick into it or out of it, on cards. A
 * tab that is changed can also be taken away, which takes nothing off the phone.
 */
@Composable
fun DrawerTabSheet(
    visible: Boolean,
    tab: DrawerTab?,
    apps: List<AppInfo>,
    onSave: (DrawerTab) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val reduced = LocalMotionLevel.current.reduced
    var name by remember(tab) { mutableStateOf(tab?.name.orEmpty()) }
    var picked by remember(tab) { mutableStateOf(tab?.keys?.toSet().orEmpty()) }

    ZenithSheet(visible = visible, onDismiss = onDismiss, title = if (tab == null) "New tab" else "Change tab", maxHeightFraction = 0.9f) {
        Spacer(Modifier.height(4.dp))
        FieldBox(
            value = name,
            onValueChange = { name = it.take(20) },
            placeholder = "Its name: Games, Work, Friends",
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        )
        // The list takes what room the sheet has left, and scrolls within it.
        Section(
            header = if (picked.isEmpty()) "Tick the apps that go in it" else "${picked.size} apps in it",
            modifier = Modifier.weight(1f, fill = false),
        ) {
            LazyColumn(Modifier.fillMaxWidth()) {
                itemsIndexed(apps, key = { _, app -> app.key }) { index, app ->
                    if (index > 0) GroupDivider()
                    TabAppRow(app = app, on = app.key in picked, reduced = reduced) {
                        picked = if (app.key in picked) picked - app.key else picked + app.key
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (onDelete != null) ZenithButton(text = "Remove tab", onClick = onDelete, modifier = Modifier.weight(1f))
            ZenithButton(
                text = if (tab == null) "Make tab" else "Save",
                kind = ButtonKind.Main,
                enabled = name.isNotBlank(),
                onClick = { onSave(DrawerTab(name.trim(), apps.map { it.key }.filter { it in picked })) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** An app in a tab's list: its icon and name, and a round tick at the end, filled with the accent when it is in. */
@Composable
private fun TabAppRow(app: AppInfo, on: Boolean, reduced: Boolean, onFlip: () -> Unit) {
    val apple = LocalApple.current
    val accent = LocalPalette.current.accent
    val touch = remember { MutableInteractionSource() }
    val fill by animateFloatAsState(if (on) 1f else 0f, if (reduced) tween(Motion.REDUCED_MS) else Motion.snappy(), label = "tabTick")
    Row(
        Modifier
            .fillMaxWidth()
            .rowPress(touch)
            // A box to tick, to TalkBack too: it says whether the app is in.
            .toggleable(value = on, interactionSource = touch, indication = null, role = Role.Checkbox) { onFlip() }
            .padding(start = 16.dp, end = 16.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(key = app.key, label = app.label, size = 30.dp)
        Spacer(Modifier.width(14.dp))
        Text(app.label, color = apple.words, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = fill))
                // Empty, a ring in grey that can be seen: it is the only sign the row can be ticked.
                .border(1.5.dp, apple.secondary.copy(alpha = apple.secondary.alpha * (1f - fill)), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = inkOnTint(accent),
                modifier = Modifier.size(16.dp).graphicsLayer {
                    alpha = fill
                    scaleX = 0.5f + 0.5f * fill
                    scaleY = 0.5f + 0.5f * fill
                },
            )
        }
    }
}
