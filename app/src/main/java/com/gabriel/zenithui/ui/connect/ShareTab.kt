package com.gabriel.zenithui.ui.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.Look
import com.gabriel.zenithui.data.connect.ConnectCodes
import com.gabriel.zenithui.data.connect.ConnectLayers
import com.gabriel.zenithui.data.connect.ConnectSetup
import com.gabriel.zenithui.data.connect.ConnectStudio
import com.gabriel.zenithui.data.connect.SetupSource
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.theme.LocalPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Share: your Zenith account, sharing the home screen as it is now as a code anybody with Zenith
 * can open, and your Looks, the shared ones with their codes to copy, send or stop.
 */
@Composable
internal fun ShareTab(state: ConnectState, bridge: ConnectBridge) {
    val shared = remember(bridge.looks) { bridge.looks.filter { it.shared.isNotEmpty() } }
    val unshared = remember(bridge.looks) { bridge.looks.filter { it.shared.isEmpty() } }
    // A Look whose Stop has been pressed once: a second press stops it.
    var stopping by remember { mutableStateOf<String?>(null) }

    LazyColumn(state = state.shareList, contentPadding = tabPadding(), modifier = Modifier.fillMaxSize()) {
        item(key = "title") { TabTitle("Share") }
        item(key = "intro") {
            Note(
                "Share your setup as a code. Anyone with Zenith opens it in Connect and sees it on their own home screen first.",
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
            )
        }
        item(key = "account") { AccountCard(state, bridge) }
        item(key = "home") { ShareHomeCard(state, bridge) }
        if (shared.isNotEmpty()) {
            item(key = "shared-head") { SectionHead("Shared", note = "their codes work now", modifier = Modifier.padding(top = 26.dp, bottom = 12.dp)) }
            items(shared, key = { "s-" + it.id }) { look ->
                SharedRow(look, state, bridge, stopping = stopping == look.id, onStop = {
                    if (stopping == look.id) {
                        stopping = null
                        state.stopSharing(bridge, look)
                    } else {
                        stopping = look.id
                    }
                })
            }
        }
        if (unshared.isNotEmpty()) {
            item(key = "unshared-head") { SectionHead("Not shared yet", modifier = Modifier.padding(top = 26.dp, bottom = 12.dp)) }
            items(unshared, key = { "u-" + it.id }) { look -> UnsharedRow(look, state, bridge) }
        }
        item(key = "code") {
            InfoCard(
                icon = Icons.Rounded.VpnKey,
                title = "Got somebody's code?",
                text = "Open it here and see it on your home screen before anything goes on.",
                action = "Type a code",
                onAction = { state.openSheet(ConnectSheet.CODE) },
                modifier = Modifier.padding(top = 26.dp),
            )
        }
    }
}

/** Who is signed in, or why to sign in: sharing needs a Zenith account. */
@Composable
private fun AccountCard(state: ConnectState, bridge: ConnectBridge) {
    val palette = LocalPalette.current
    val account = bridge.account
    when {
        !bridge.server -> InfoCard(
            icon = Icons.Rounded.CloudOff,
            title = "No server in this build",
            text = "Sharing needs Zenith's server. Everything else in Connect works on this phone.",
        )
        account == null -> InfoCard(
            icon = Icons.Rounded.Person,
            title = "Sign in to share",
            text = "A Zenith account keeps your codes yours, so you can stop sharing any of them, and backs up your home screen.",
            action = "Sign in or make one",
            tinted = true,
            onAction = { state.openSheet(ConnectSheet.SIGN_IN) },
        )
        else -> Row(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .pane(CARD)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(palette.accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(ConnectStudio.initial(account), color = com.gabriel.zenithui.ui.theme.inkOnTint(palette.accent), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(account, color = palette.onGlass, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Note(if (bridge.plus) "Signed in · Zenith Plus" else "Signed in")
            }
            ConnectButton("Sign out", onClick = { state.signOut(bridge) }, height = 40.dp)
        }
    }
}

/** Your home screen as it is now, small, and Share it. */
@Composable
private fun ShareHomeCard(state: ConnectState, bridge: ConnectBridge) {
    val palette = LocalPalette.current
    val worn = remember(bridge.settings, bridge.home.items) { WornHome(bridge.settings, bridge.home.items, bridge.settings.dock) }
    val aspect = phoneAspect(bridge)
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 14.dp)
            .fillMaxWidth()
            .pane(CARD)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(92.dp).aspectRatio(aspect)) {
            SetupPhone(worn, bridge, Modifier.fillMaxSize(), artWidth = 220)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("Your home screen", color = palette.onGlass, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Note("As it is now: its look, wallpaper design and pages. Other apps' widgets, shortcuts and the names you gave apps stay on this phone.")
            Spacer(Modifier.height(12.dp))
            ConnectButton(
                if (bridge.plus || !bridge.server) "Share it" else "Share it · Plus",
                onClick = { state.shareHome(bridge) },
                icon = Icons.Rounded.IosShare,
                tinted = true,
                height = 42.dp,
            )
        }
    }
}

/** A small picture of a Look of yours, as it is: tap the row to open its page. */
@Composable
private fun LookThumb(look: Look, bridge: ConnectBridge) {
    val setup = remember(look) { ConnectSetup.of(look, SetupSource.MINE) }
    val on = remember(setup) { ConnectLayers.available(setup) }
    val worn = rememberWorn(setup, on, bridge)
    Box(Modifier.width(50.dp).aspectRatio(phoneAspect(bridge))) {
        SetupPhone(worn, bridge, Modifier.fillMaxSize(), artWidth = 160)
    }
}

@Composable
private fun SharedRow(look: Look, state: ConnectState, bridge: ConnectBridge, stopping: Boolean, onStop: () -> Unit) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val code = "LOOK-${look.shared}"
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp)
            .fillMaxWidth()
            .pressBounce(0.98f)
            .pane(ROW, interactive = true)
            .tap("Open ${look.name}") { state.open(ConnectSetup.of(look, SetupSource.MINE), bridge) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LookThumb(look, bridge)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(look.name, color = palette.onGlass, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(code, color = palette.onGlass, fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp)
            Note(
                when {
                    stopping -> "Tap stop again: its code will open nothing."
                    look.sharedIsOld -> "Saved over since: the code gives it as it was"
                    else -> "Shared " + whenSaved(look.sharedAt)
                },
                colour = if (stopping) palette.onGlass else palette.onGlassMuted,
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RoundButton(Icons.Rounded.ContentCopy, "Copy the code", size = 38.dp, onClick = {
                copyText(context, "Zenith Look", code)
                state.say("Copied $code")
            })
            RoundButton(Icons.AutoMirrored.Rounded.Send, "Send the code", size = 38.dp, onClick = {
                sendText(context, ConnectCodes.message(look.name, look.shared), "Send ${look.name}")
            })
            RoundButton(Icons.Rounded.LinkOff, if (stopping) "Stop sharing now" else "Stop sharing", size = 38.dp, onClick = onStop)
        }
    }
}

@Composable
private fun UnsharedRow(look: Look, state: ConnectState, bridge: ConnectBridge) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp)
            .fillMaxWidth()
            .pressBounce(0.98f)
            .pane(ROW, interactive = true)
            .tap("Open ${look.name}") { state.open(ConnectSetup.of(look, SetupSource.MINE), bridge) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LookThumb(look, bridge)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(look.name, color = palette.onGlass, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Note("Saved " + whenSaved(look.savedAt))
        }
        Spacer(Modifier.width(8.dp))
        ConnectButton("Share", onClick = { state.shareSaved(bridge, look) }, icon = Icons.Rounded.IosShare, height = 38.dp)
    }
}

/** "today", or the day: "on 19 Nov". */
internal fun whenSaved(at: Long, now: Long = System.currentTimeMillis()): String {
    if (at <= 0L) return "a while ago"
    val day = SimpleDateFormat("yyyyMMdd", Locale.US)
    return if (day.format(Date(at)) == day.format(Date(now))) "today" else "on " + SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(at))
}
