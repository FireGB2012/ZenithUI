package com.gabriel.zenithui.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.AppShortcut
import com.gabriel.zenithui.data.GestureActions
import com.gabriel.zenithui.data.Gestures
import com.gabriel.zenithui.ui.common.AppIcon
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.popIn
import com.gabriel.zenithui.ui.common.popOut
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.common.unfoldIn
import com.gabriel.zenithui.ui.common.unfoldOut
import com.gabriel.zenithui.ui.settings.CardRows
import com.gabriel.zenithui.ui.settings.FieldBox
import com.gabriel.zenithui.ui.settings.FieldLook
import com.gabriel.zenithui.ui.settings.GroupDivider
import com.gabriel.zenithui.ui.settings.GroupName
import com.gabriel.zenithui.ui.settings.ROW_INSET
import com.gabriel.zenithui.ui.settings.ROW_TEXT_INSET
import com.gabriel.zenithui.ui.settings.Section
import com.gabriel.zenithui.ui.settings.rowPress
import com.gabriel.zenithui.ui.theme.LocalApple
import com.gabriel.zenithui.ui.theme.LocalOnGlass
import com.gabriel.zenithui.ui.theme.LocalPalette
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

/**
 * What one gesture could do, as an iPhone lists choices: Zenith's own actions on a card, each
 * saying in grey what it does, then, under "Or open an app", Apple's grey search and every app on a
 * card of its own; under an app, its shortcuts, opened with the arrow beside it. The one chosen now
 * carries a check in the accent, which pops onto whichever is tapped as the sheet goes.
 */
@Composable
fun GestureActionSheet(
    visible: Boolean,
    gesture: String,
    current: String,
    apps: List<AppInfo>,
    /** Whether the lock helper is on, so Lock the phone can say it is still to be set up. */
    helperOn: Boolean,
    shortcutsOf: suspend (String) -> List<AppShortcut>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val words = query.trim().lowercase()
    val shown = remember(apps, words) { if (words.isEmpty()) apps else apps.filter { words in it.label.lowercase() } }
    var opened by remember { mutableStateOf<String?>(null) }

    ZenithSheet(visible = visible, onDismiss = onDismiss, title = Gestures.label(gesture), maxHeightFraction = 0.86f) {
        LazyColumn(Modifier.weight(1f, fill = false)) {
            // Zenith's own actions, one card of them, each coming up a beat behind the one above.
            item(key = "actions") {
                Section {
                    CardRows(inset = ROW_INSET) {
                        GestureActions.fixed.forEachIndexed { index, action ->
                            key(action) {
                                ChoiceLine(
                                    title = GestureActions.label(action) { null },
                                    detail = GestureActions.detail(action),
                                    note = if (GestureActions.needsHelper(action) && !helperOn) "Set up" else null,
                                    selected = current == action,
                                    modifier = Modifier.appearIn(delayMillis = staggerDelay(index), riseFrom = 18f),
                                ) { onPick(action) }
                            }
                        }
                    }
                }
            }
            // The apps' group: its name on the glass, the search that finds one, then their card.
            item(key = "apps") {
                Column(Modifier.padding(top = 12.dp)) {
                    GroupName("Or open an app")
                    FieldBox(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = "Find an app",
                        look = FieldLook.Search,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
            itemsIndexed(shown, key = { _, app -> "app:" + app.key }) { index, app ->
                AppChoice(
                    app = app,
                    current = current,
                    open = opened == app.key,
                    first = index == 0,
                    last = index == shown.lastIndex,
                    onToggle = { opened = if (opened == app.key) null else app.key },
                    shortcutsOf = shortcutsOf,
                    onPick = onPick,
                )
            }
        }
    }
}

/** The corners of the apps' card: those of every card of rows, CardKind.Grouped's. */
private val APPS_CARD_CORNER = 12.dp

/** An app's icon in its row: as wide as a Settings row's square, so its name and hairline line up with theirs. */
private val APP_ICON = 30.dp

/** A shortcut's icon, a little smaller than its app's. */
private val SHORTCUT_ICON = 26.dp

/**
 * Where a shortcut's name starts, and so the hairline over it: its row is set in under its app's
 * name, then its icon and the same gap as an app's row.
 */
private val SHORTCUT_WORDS_INSET = ROW_TEXT_INSET + SHORTCUT_ICON + 14.dp

/**
 * One thing to choose, as a row of a card: its symbol where it has one ([leading]), what it is, in
 * grey what it does, a [note] in the accent where it still needs something, and a check in the
 * accent when it is the one, which pops in and out. Under a finger the row shades rather than
 * bouncing. It starts [start] in from the card's edge, further in for a shortcut under its app;
 * [trailing] is a button of its own at its end.
 */
@Composable
private fun ChoiceLine(
    title: String,
    detail: String?,
    note: String? = null,
    selected: Boolean,
    modifier: Modifier = Modifier,
    start: Dp = ROW_INSET,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val apple = LocalApple.current
    val accent = LocalPalette.current.accent
    val reduced = LocalMotionLevel.current.reduced
    val touch = remember { MutableInteractionSource() }
    // A button at its end brings its own room with it.
    val button = trailing != null
    Row(
        modifier
            .fillMaxWidth()
            .rowPress(touch)
            .clickable(interactionSource = touch, indication = null, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(start = start, end = if (button) 4.dp else 16.dp)
            .padding(vertical = if (button) 4.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = apple.words, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (detail != null) Text(detail, color = apple.secondary, fontSize = 14.sp)
        }
        if (note != null) {
            Text(note, color = accent, fontSize = 15.sp, modifier = Modifier.padding(start = 8.dp))
        }
        AnimatedVisibility(visible = selected, enter = popIn(reduced), exit = popOut(reduced)) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = "Chosen",
                tint = accent,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(22.dp),
            )
        }
        trailing?.invoke()
    }
}

/**
 * An app to open, as one piece of the apps' card ([AppCardPiece]): its icon, its name and a check
 * when it is the one, and, under the arrow beside it, the shortcuts it offers, unfolding as rows of
 * the same card set in under its name. [first] and [last] say where on the card it stands.
 */
@Composable
private fun AppChoice(
    app: AppInfo,
    current: String,
    open: Boolean,
    first: Boolean,
    last: Boolean,
    onToggle: () -> Unit,
    shortcutsOf: suspend (String) -> List<AppShortcut>,
    onPick: (String) -> Unit,
) {
    val apple = LocalApple.current
    val reduced = LocalMotionLevel.current.reduced
    val turn by animateFloatAsState(if (open) 180f else 0f, if (reduced) tween(Motion.REDUCED_MS) else Motion.snappy(), label = "expand")
    AppCardPiece(first = first, last = last) {
        if (!first) GroupDivider()
        ChoiceLine(
            title = app.label,
            detail = null,
            selected = current == GestureActions.app(app.key),
            // On a card in a sheet: a glass icon gets AppIcon's plate, not a pane of glass of its own.
            leading = { AppIcon(key = app.key, label = app.label, size = APP_ICON, onGlass = true) },
            trailing = {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onToggle)
                        .semantics { stateDescription = if (open) "Open" else "Closed" },
                    contentAlignment = Alignment.Center,
                ) {
                    // The only way to an app's shortcuts, so it is a control's grey, not a chevron's.
                    Icon(
                        Icons.Rounded.ExpandMore,
                        contentDescription = "${app.label}'s shortcuts",
                        tint = apple.secondary,
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer { rotationZ = turn },
                    )
                }
            },
        ) { onPick(GestureActions.app(app.key)) }
        AnimatedVisibility(visible = open, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
            val shortcuts by produceState<List<AppShortcut>?>(initialValue = null, app.key) {
                value = shortcutsOf(app.packageName)
            }
            Column {
                val list = shortcuts
                when {
                    // A moment's room while Android hands them over.
                    list == null -> Spacer(Modifier.height(8.dp))
                    list.isEmpty() -> {
                        GroupDivider()
                        Text(
                            "${app.label} offers no shortcuts",
                            color = apple.secondary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = ROW_TEXT_INSET, end = 16.dp, top = 12.dp, bottom = 12.dp),
                        )
                    }
                    else -> list.forEachIndexed { index, shortcut ->
                        GroupDivider(inset = SHORTCUT_WORDS_INSET)
                        ChoiceLine(
                            title = shortcut.label,
                            detail = null,
                            selected = current == shortcut.key,
                            modifier = Modifier.appearIn(delayMillis = staggerDelay(index), riseFrom = 14f),
                            start = ROW_TEXT_INSET,
                            leading = { AppIcon(key = shortcut.key, label = shortcut.label, size = SHORTCUT_ICON, onGlass = true) },
                        ) { onPick(shortcut.key) }
                    }
                }
            }
        }
    }
}

/**
 * One piece of a card that runs on across the items of a list, so that however many apps there
 * are, only the rows on screen are composed: the card's corners along the top of the [first] piece
 * and the foot of the [last], square between, so the pieces stand together as one solid card. On
 * it, words are a card's, even inside a sheet.
 */
@Composable
private fun AppCardPiece(first: Boolean, last: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val top = if (first) APPS_CARD_CORNER else 0.dp
    val foot = if (last) APPS_CARD_CORNER else 0.dp
    CompositionLocalProvider(LocalOnGlass provides false) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = top, topEnd = top, bottomEnd = foot, bottomStart = foot))
                .background(LocalApple.current.card),
            content = content,
        )
    }
}
