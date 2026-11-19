package com.gabriel.zenithui.ui.connect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.connect.ConnectSetup
import com.gabriel.zenithui.data.connect.SetupSource
import com.gabriel.zenithui.ui.theme.LocalPalette

/**
 * The Vault: Undo for what Connect put on last, saving the home screen as a Look, your Looks with
 * a picture of each, and your home screen backed up to your Zenith account.
 */
@Composable
internal fun VaultTab(state: ConnectState, bridge: ConnectBridge) {
    val mine = remember(bridge.looks) { bridge.looks.map { ConnectSetup.of(it, SetupSource.MINE) } }
    val aspect = phoneAspect(bridge)
    val plus = if (bridge.plus) "" else " · Plus"

    LazyColumn(state = state.vaultList, contentPadding = tabPadding(), modifier = Modifier.fillMaxSize()) {
        item(key = "title") { TabTitle("Vault") }
        item(key = "intro") {
            Note(
                "Your Looks, kept on this phone, and your home screen backed up to your account.",
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
            )
        }
        val worn = bridge.worn
        if (worn != null) {
            item(key = "undo") {
                InfoCard(
                    icon = Icons.AutoMirrored.Rounded.Undo,
                    title = "${quoted(worn)} is on",
                    text = "Put on from Connect. Undo puts your home screen back exactly as it was before.",
                    action = "Undo",
                    actionIcon = Icons.AutoMirrored.Rounded.Undo,
                    onAction = { state.undo(bridge) },
                    modifier = Modifier.padding(bottom = 14.dp),
                )
            }
        }
        item(key = "save") {
            InfoCard(
                icon = Icons.Rounded.BookmarkAdd,
                title = "Save this home screen",
                text = "Keep it as a Look: its wallpaper, icons, widgets and pages, to come back to or share.",
                action = "Save as a Look$plus",
                tinted = true,
                onAction = { state.saveHome(bridge) },
            )
        }
        item(key = "looks-head") {
            SectionHead(
                "Your Looks",
                note = if (mine.isEmpty()) null else if (mine.size == 1) "1 saved" else "${mine.size} saved",
                modifier = Modifier.padding(top = 26.dp, bottom = 12.dp),
            )
        }
        if (mine.isEmpty()) {
            item(key = "no-looks") {
                InfoCard(
                    icon = Icons.Rounded.Inventory2,
                    title = "No Looks yet",
                    text = "Save your home screen as a Look and it's kept here. Looks you save in Settings show up here too.",
                )
            }
        } else {
            items(mine.chunked(2), key = { row -> "v-" + row.first().id }) { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { setup -> SetupCard(setup, state, bridge, aspect, Modifier.weight(1f)) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        item(key = "backup-head") { SectionHead("Backup", modifier = Modifier.padding(top = 26.dp, bottom = 12.dp)) }
        item(key = "backup") { BackupCard(state, bridge) }
    }
}

/** Your account's backup: when it was made, Back up now and Restore; or what it needs first. */
@Composable
private fun BackupCard(state: ConnectState, bridge: ConnectBridge) {
    val palette = LocalPalette.current
    val plus = if (bridge.plus) "" else " · Plus"
    when {
        !bridge.server -> InfoCard(
            icon = Icons.Rounded.CloudOff,
            title = "No server in this build",
            text = "Backups go to a Zenith account on Zenith's server, which this build doesn't have.",
        )
        bridge.account == null -> InfoCard(
            icon = Icons.Rounded.CloudUpload,
            title = "Back up to an account",
            text = "Sign in and your home screen is kept on Zenith's server, ready for a new phone or a fresh start.",
            action = "Sign in or make one",
            onAction = { state.openSheet(ConnectSheet.SIGN_IN) },
        )
        else -> Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .pane(CARD)
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(palette.onGlass.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (bridge.backupAt != null) Icons.Rounded.CloudDone else Icons.Rounded.CloudUpload,
                        contentDescription = null,
                        tint = palette.onGlass,
                        modifier = Modifier.size(21.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (bridge.backupAt != null) "Backed up ${whenSaved(bridge.backupAt)}" else "Not backed up yet",
                        color = palette.onGlass,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Note("To ${bridge.account}'s account on Zenith's server")
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ConnectButton(
                    if (state.busy && state.sheet == null) "Backing up…" else "Back up now$plus",
                    onClick = { state.backUp(bridge) },
                    icon = Icons.Rounded.CloudUpload,
                    tinted = true,
                    height = 44.dp,
                    modifier = Modifier.weight(1f),
                )
                ConnectButton(
                    "Restore",
                    onClick = { state.askRestore(bridge) },
                    icon = Icons.Rounded.CloudDownload,
                    enabled = bridge.backupAt != null,
                    height = 44.dp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
