package com.gabriel.zenithui.ui.connect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.connect.ConnectCodes
import com.gabriel.zenithui.ui.common.shake
import com.gabriel.zenithui.ui.home.ZenithSheet
import com.gabriel.zenithui.ui.onboarding.Segmented
import com.gabriel.zenithui.ui.theme.LocalPalette
import kotlinx.coroutines.delay

/**
 * Connect's sheets, Zenith's own glass sheets: a code to open, signing in, sharing, saving a Look,
 * and putting the account's backup back. They rise with the keyboard, so what is typed stays in sight.
 */
@Composable
internal fun ConnectSheets(state: ConnectState, bridge: ConnectBridge) {
    // Only what the keyboard takes beyond the navigation bar: the sheet keeps clear of that itself.
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars))) {
        CodeSheet(state, bridge)
        SignInSheet(state, bridge)
        ShareSheet(state, bridge)
        SaveSheet(state, bridge)
        RestoreSheet(state, bridge)
    }
}

/** Puts the keyboard on [focus] once the sheet it is in has come up. */
@Composable
private fun TypeOnOpen(open: Boolean, focus: FocusRequester) {
    LaunchedEffect(open) {
        if (open) {
            delay(280)
            runCatching { focus.requestFocus() }
        }
    }
}

/** What went wrong, under what was asked, with a shake of the head each time it is said again. */
@Composable
private fun ColumnScope.Problem(state: ConnectState) {
    val problem = state.problem ?: return
    var turn by remember { mutableStateOf(0) }
    LaunchedEffect(problem) { turn++ }
    Spacer(Modifier.height(10.dp))
    Note(problem, modifier = Modifier.fillMaxWidth().shake(turn), colour = LocalPalette.current.onGlass)
}

@Composable
private fun CodeSheet(state: ConnectState, bridge: ConnectBridge) {
    val open = state.sheet == ConnectSheet.CODE
    var text by remember(open) { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val clipboard = LocalClipboardManager.current
    TypeOnOpen(open, focus)
    ZenithSheet(visible = open, onDismiss = { state.closeSheet() }, title = "Have a code?") {
        Note("Type or paste the code somebody sent you. You'll see it on your own home screen before anything goes on.")
        Spacer(Modifier.height(14.dp))
        ConnectField(
            value = text,
            onChange = {
                text = it
                state.problem = null
            },
            hint = "LOOK-XXXXXX",
            capitals = KeyboardCapitalization.Characters,
            action = ImeAction.Go,
            onAction = { state.lookUp(bridge, text) },
            focus = focus,
        )
        Problem(state)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ConnectButton("Paste", onClick = {
                clipboard.getText()?.text?.let { pasted ->
                    text = ConnectCodes.clean(pasted)?.let { "LOOK-$it" } ?: pasted.trim().take(40)
                    state.problem = null
                }
            }, icon = Icons.Rounded.ContentPaste)
            ConnectButton(
                if (state.busy) "Looking…" else "Open",
                onClick = { state.lookUp(bridge, text) },
                tinted = true,
                enabled = text.isNotBlank() && !state.busy,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SignInSheet(state: ConnectState, bridge: ConnectBridge) {
    val open = state.sheet == ConnectSheet.SIGN_IN
    var making by remember(open) { mutableStateOf(false) }
    var name by remember(open) { mutableStateOf("") }
    var password by remember(open) { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    TypeOnOpen(open, focus)
    val go = { state.signIn(bridge, name, password, making) }
    ZenithSheet(visible = open, onDismiss = { state.closeSheet() }, title = if (making) "Make a Zenith account" else "Sign in to Zenith") {
        Segmented(
            options = listOf("in" to "Sign in", "new" to "New account"),
            selected = if (making) "new" else "in",
            onSelect = {
                making = it == "new"
                state.problem = null
            },
        )
        Spacer(Modifier.height(14.dp))
        ConnectField(
            value = name,
            onChange = {
                name = it
                state.problem = null
            },
            hint = "Username",
            capitals = KeyboardCapitalization.None,
            action = ImeAction.Next,
            focus = focus,
        )
        Spacer(Modifier.height(10.dp))
        ConnectField(
            value = password,
            onChange = {
                password = it
                state.problem = null
            },
            hint = "Password",
            secret = true,
            action = ImeAction.Go,
            onAction = go,
        )
        Problem(state)
        Spacer(Modifier.height(14.dp))
        ConnectButton(
            when {
                state.busy -> if (making) "Making it…" else "Signing in…"
                making -> "Make my account"
                else -> "Sign in"
            },
            onClick = go,
            tinted = true,
            enabled = !state.busy,
            height = 52.dp,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Note(
            "Your account is on Zenith's own server. It keeps your shared codes yours and your backup safe.",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ShareSheet(state: ConnectState, bridge: ConnectBridge) {
    val open = state.sheet == ConnectSheet.SHARE
    val context = LocalContext.current
    val palette = LocalPalette.current
    var name by remember(open) { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val made = state.madeCode
    val look = state.sheetLook?.let { id -> bridge.looks.firstOrNull { it.id == id } }
    TypeOnOpen(open && state.sheetLook == null && made == null, focus)
    ZenithSheet(
        visible = open,
        onDismiss = { state.closeSheet() },
        title = if (made != null) "It's shared" else "Share ${look?.name?.let(::quoted) ?: "your home screen"}",
    ) {
        when {
            made != null -> {
                val (madeName, code) = made
                Text(
                    "LOOK-$code",
                    color = palette.onGlass,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                )
                Note(
                    "Anyone with Zenith can open ${quoted(madeName)} with this code: Connect, then Have a code. You can stop sharing it in Share.",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ConnectButton("Copy", onClick = {
                        copyText(context, "Zenith Look", "LOOK-$code")
                        state.say("Copied LOOK-$code")
                    }, icon = Icons.Rounded.ContentCopy, modifier = Modifier.weight(1f))
                    ConnectButton("Send", onClick = {
                        sendText(context, ConnectCodes.message(madeName, code), "Send $madeName")
                    }, icon = Icons.AutoMirrored.Rounded.Send, tinted = true, modifier = Modifier.weight(1f))
                }
            }
            state.busy -> Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = palette.onGlass, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("Sharing…", color = palette.onGlass, fontSize = 16.sp)
            }
            look != null -> {
                // A Look that couldn't be shared just now: why, and again.
                Problem(state)
                Spacer(Modifier.height(14.dp))
                ConnectButton("Try again", onClick = { state.shareSaved(bridge, look) }, tinted = true, modifier = Modifier.fillMaxWidth())
            }
            else -> {
                Note("Your home screen as it is now is saved as a Look and shared under a code of its own.")
                Spacer(Modifier.height(14.dp))
                ConnectField(
                    value = name,
                    onChange = {
                        name = it
                        state.problem = null
                    },
                    hint = "Name it, like My dark glass",
                    action = ImeAction.Go,
                    onAction = { state.shareNamed(bridge, name) },
                    focus = focus,
                )
                Problem(state)
                Spacer(Modifier.height(14.dp))
                ConnectButton("Share it", onClick = { state.shareNamed(bridge, name) }, tinted = true, height = 52.dp, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SaveSheet(state: ConnectState, bridge: ConnectBridge) {
    val open = state.sheet == ConnectSheet.SAVE
    var name by remember(open) { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    TypeOnOpen(open, focus)
    ZenithSheet(visible = open, onDismiss = { state.closeSheet() }, title = "Save as a Look") {
        Note("Its wallpaper, theme, icons, widgets and pages, kept on this phone to come back to or share.")
        Spacer(Modifier.height(14.dp))
        ConnectField(
            value = name,
            onChange = {
                name = it
                state.problem = null
            },
            hint = "Name it",
            action = ImeAction.Done,
            onAction = { state.saveNamed(bridge, name) },
            focus = focus,
        )
        Problem(state)
        Spacer(Modifier.height(14.dp))
        ConnectButton("Save", onClick = { state.saveNamed(bridge, name) }, tinted = true, height = 52.dp, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun RestoreSheet(state: ConnectState, bridge: ConnectBridge) {
    val open = state.sheet == ConnectSheet.RESTORE
    ZenithSheet(visible = open, onDismiss = { state.closeSheet() }, title = "Put your backup back?") {
        Note(
            "Your home screen as it was backed up " + (bridge.backupAt?.let(::whenSaved) ?: "") +
                " goes on in place of the one you have now. Your Looks on this phone stay.",
        )
        Problem(state)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ConnectButton("Cancel", onClick = { state.closeSheet() }, modifier = Modifier.weight(1f))
            ConnectButton(
                if (state.busy) "Restoring…" else "Put it back",
                onClick = { state.restore(bridge) },
                tinted = true,
                enabled = !state.busy,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
