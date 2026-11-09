package com.gabriel.zenithui.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.AppEdit
import com.gabriel.zenithui.data.IconPack
import com.gabriel.zenithui.data.IconPackInfo
import com.gabriel.zenithui.data.IconPacks
import com.gabriel.zenithui.data.IconSource
import com.gabriel.zenithui.ui.common.AppIcon
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.common.wordsChange
import com.gabriel.zenithui.ui.settings.CardRows
import com.gabriel.zenithui.ui.settings.FieldBox
import com.gabriel.zenithui.ui.settings.FieldLook
import com.gabriel.zenithui.ui.settings.Note
import com.gabriel.zenithui.ui.settings.Section
import com.gabriel.zenithui.ui.settings.ToggleRow
import com.gabriel.zenithui.ui.theme.LocalApple
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.inkOnTint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column

/**
 * One app, as you would have it: its name, its icon and whether its name shows, set out on solid
 * cards as an iPhone sets out a contact. The icon at the top is the app as it stands now, and
 * springs up afresh each time it is given a new picture; a name typed in is on the home screen
 * behind the sheet as it is typed. Once anything has been changed, a card of its own at the foot
 * puts it all back.
 */
@Composable
fun EditAppSheet(
    visible: Boolean,
    appKey: String,
    /** The app's own name, shown in the field until you write one of your own. */
    original: String,
    edit: AppEdit,
    shape: androidx.compose.ui.graphics.Shape,
    onLabel: (String) -> Unit,
    onChooseIcon: () -> Unit,
    onHideLabel: (Boolean) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val reduced = LocalMotionLevel.current.reduced
    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Edit") {
        // On a short screen, or with large text, it scrolls rather than losing its last card.
        Column(Modifier.verticalScroll(rememberScrollState())) {
            // The app itself stands on the glass above its cards, in the middle, as a contact's photo does.
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .appearIn(delayMillis = staggerDelay(0), riseFrom = 22f)
                    .padding(top = 4.dp)
                    .size(68.dp),
                contentAlignment = Alignment.Center,
            ) {
                // A new picture is a new icon: it grows in where the old one was.
                key(edit.icon) {
                    Box(Modifier.appearIn(fromScale = 0.6f, riseFrom = 0f)) {
                        // Over the sheet's glass: a Glass icon is drawn on a pale plate, never as glass on glass.
                        AppIcon(key = appKey, label = edit.label.ifBlank { original }, size = 64.dp, shape = shape, onGlass = true)
                    }
                }
            }
            Section(header = "Name") {
                NameField(
                    value = edit.label,
                    placeholder = original,
                    onChange = onLabel,
                    modifier = Modifier.appearIn(delayMillis = staggerDelay(0), riseFrom = 22f),
                )
            }
            val source = IconSource.parse(edit.icon)
            Section {
                CardRows {
                    MenuRow(
                        Icons.Rounded.Image,
                        "Choose icon",
                        when (source) {
                            is IconSource.Pack -> "From an icon pack. Tap to choose another"
                            is IconSource.Photo -> "One of your photos. Tap to choose another"
                            null -> "From an icon pack, or one of your photos"
                        },
                        index = 1,
                    ) { onChooseIcon() }
                    Box(Modifier.appearIn(delayMillis = staggerDelay(2), riseFrom = 22f)) {
                        ToggleRow(
                            title = "Hide name",
                            subtitle = if (edit.hideLabel) "No name under this icon" else "Its name shows under the icon",
                            checked = edit.hideLabel,
                            onChange = onHideLabel,
                        )
                    }
                }
            }
            AnimatedContent(
                targetState = !edit.isEmpty,
                transitionSpec = { wordsChange(reduced) },
                label = "editReset",
            ) { changed ->
                if (changed) {
                    // On a card of its own after the others, as an iPhone keeps apart a row that undoes them all.
                    Section {
                        MenuRow(Icons.Rounded.RestartAlt, "Put it back as it was", "Its own name and icon, and its name showing", index = 3) { onReset() }
                    }
                } else {
                    Spacer(Modifier.height(1.dp))
                }
            }
        }
    }
}

/**
 * A name, typed in Apple's box as a row of the card it stands on: the app's own name in grey until
 * you write one of yours, never longer than [MAX_NAME], each word begun with a capital, and Done on
 * the keyboard.
 */
@Composable
private fun NameField(value: String, placeholder: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    FieldBox(
        value = value,
        onValueChange = { onChange(it.take(MAX_NAME)) },
        placeholder = placeholder,
        modifier = modifier,
        look = FieldLook.Row,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
    )
}

/** Long enough for any real name; a label wider than its icon is cut short anyway. */
private const val MAX_NAME = 40

/**
 * Where an app's icon comes from. Along the top, the ways to choose, as Apple's capsules on the
 * glass: the app's own icon, each icon pack on the phone, and a photo. A pack opens its icons below
 * on a card, the one it draws this app with first, under Apple's grey box to find one by name. A
 * tap on any of them is the choice.
 */
@Composable
fun IconPickerSheet(
    visible: Boolean,
    appKey: String,
    /** What the app has now, as [IconSource] writes it; empty for its own. */
    current: String,
    shape: androidx.compose.ui.graphics.Shape,
    onPick: (String) -> Unit,
    onPhoto: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val packs by produceState(initialValue = emptyList<IconPackInfo>()) {
        value = withContext(Dispatchers.IO) { IconPacks.installed(context) }
    }
    val chosenPack = (IconSource.parse(current) as? IconSource.Pack)?.packageName
    var open by remember { mutableStateOf<String?>(null) }
    // The pack in use opens first; otherwise the first there is.
    val shownPack = open ?: chosenPack ?: packs.firstOrNull()?.packageName

    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Choose icon", maxHeightFraction = 0.86f) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
            modifier = Modifier.appearIn(delayMillis = staggerDelay(0), riseFrom = 18f),
        ) {
            item(key = "own") {
                SourcePill("Its own", selected = current.isEmpty()) { onPick("") }
            }
            items(packs, key = { it.packageName }) { pack ->
                SourcePill(pack.label, selected = pack.packageName == shownPack) { open = pack.packageName }
            }
            item(key = "photo") {
                SourcePill("Photo", selected = IconSource.parse(current) is IconSource.Photo, icon = Icons.Rounded.Photo) { onPhoto() }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (shownPack == null) {
            // Grey words on the glass, in line with the footnotes under a card.
            Note(
                "Install any Nova or ADW compatible icon pack and its icons can be chosen here. Or use one of your photos.",
                Modifier.padding(horizontal = 12.dp),
            )
        } else {
            key(shownPack) {
                PackIcons(packageName = shownPack, appKey = appKey, current = current, shape = shape, onPick = onPick)
            }
        }
    }
}

/**
 * One way to choose, as Apple's capsule standing on the sheet's glass: grey, its words in the words'
 * colour, or, when it is the one chosen, filled with the accent and its words in the ink the accent
 * needs. It gives a little under a finger, and its colours fade over to the new ones when the
 * choice changes.
 */
@Composable
private fun SourcePill(label: String, selected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    val apple = LocalApple.current
    val accent = LocalPalette.current.accent
    val fill by animateColorAsState(if (selected) accent else apple.field, tween(220), label = "pillFill")
    val ink by animateColorAsState(if (selected) inkOnTint(accent) else apple.words, tween(220), label = "pillInk")
    Row(
        Modifier
            .pressBounce(0.95f)
            .clip(RoundedCornerShape(50))
            .background(fill)
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, color = ink, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/**
 * How tall the card of a pack's icons stands, whether they are still coming, have been found or are
 * not there, so the sheet never jumps between the three.
 */
private val ICONS_HEIGHT = 320.dp

/**
 * One pack's icons, the one it draws this app with first, under Apple's grey box to find one by
 * name. They stand on a solid card and scroll inside it; the one the app has now is ringed in the
 * accent.
 */
@Composable
private fun PackIcons(packageName: String, appKey: String, current: String, shape: androidx.compose.ui.graphics.Shape, onPick: (String) -> Unit) {
    val apple = LocalApple.current
    val accent = LocalPalette.current.accent
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val names by produceState<List<String>?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.IO) {
            val pack = IconPack(context, packageName).takeIf { it.load() } ?: return@withContext emptyList()
            val parts = appKey.split('/', limit = 2)
            val mine = if (parts.size == 2) pack.suggested(android.content.ComponentName(parts[0], parts[1])) else null
            listOfNotNull(mine) + (pack.allDrawables() - setOfNotNull(mine))
        }
    }
    val words = query.lowercase().filter { it.isLetterOrDigit() }
    val shown = names?.let { all -> if (words.isEmpty()) all else all.filter { words in it.lowercase().filter { c -> c.isLetterOrDigit() } } }

    FieldBox(
        value = query,
        onValueChange = { query = it },
        placeholder = "Find an icon",
        look = FieldLook.Search,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
    Section {
        when {
            shown == null -> Box(Modifier.fillMaxWidth().height(ICONS_HEIGHT))
            shown.isEmpty() -> Box(
                Modifier
                    .fillMaxWidth()
                    .height(ICONS_HEIGHT)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (words.isEmpty()) "This pack does not list its icons." else "No icon in this pack is called “${query.trim()}”.",
                    color = apple.secondary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                )
            }
            // A small margin inside the card and narrow gaps across, so each row holds as many
            // icons as will fit.
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(64.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().height(ICONS_HEIGHT),
            ) {
                items(shown, key = { it }) { name ->
                    val source = IconSource.pack(packageName, name)
                    val picked = source == current
                    val ring by animateColorAsState(if (picked) accent else Color.Transparent, tween(220), label = "iconRing")
                    // A cell is as wide as the card shares out; the icon's square stands in its middle,
                    // so its ring is square too.
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .size(64.dp)
                                .pressBounce(0.9f)
                                .clip(RoundedCornerShape(18.dp))
                                .border(2.dp, ring, RoundedCornerShape(18.dp))
                                .clickable(interactionSource = null, indication = null) { onPick(source) },
                            contentAlignment = Alignment.Center,
                        ) {
                            AppIcon(key = source, label = name, size = 50.dp, shape = shape)
                        }
                    }
                }
            }
        }
    }
}
