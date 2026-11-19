package com.gabriel.zenithui.ui.connect

import android.os.Build
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.BuildConfig
import com.gabriel.zenithui.data.connect.ConnectNews
import com.gabriel.zenithui.data.connect.ConnectReport
import com.gabriel.zenithui.data.connect.ConnectServer
import com.gabriel.zenithui.data.connect.News
import com.gabriel.zenithui.data.connect.ServerState
import com.gabriel.zenithui.ui.theme.LocalLite
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalTheme
import com.gabriel.zenithui.ui.theme.inkOnTint

/**
 * Updates: the Zenith on this phone and what each version brought, a problem report that goes out
 * in a message of your own (nothing is sent behind your back), and how Zenith's server stands.
 */
@Composable
internal fun UpdatesTab(state: ConnectState, bridge: ConnectBridge) {
    LazyColumn(state = state.updatesList, contentPadding = tabPadding(), modifier = Modifier.fillMaxSize()) {
        item(key = "title") { TabTitle("Updates") }
        item(key = "build") { BuildCard() }
        item(key = "news-head") { SectionHead("What's new", modifier = Modifier.padding(top = 26.dp, bottom = 12.dp)) }
        items(ConnectNews.all, key = { "n-" + it.version }) { news -> NewsCard(news, latest = news === ConnectNews.all.first()) }
        item(key = "report-head") { SectionHead("Something wrong?", modifier = Modifier.padding(top = 26.dp, bottom = 12.dp)) }
        item(key = "report") { ReportCard(state) }
        item(key = "server-head") { SectionHead("Zenith's server", modifier = Modifier.padding(top = 26.dp, bottom = 12.dp)) }
        item(key = "server") { ServerCard(state, bridge) }
    }
}

/** The Zenith on this phone, with its mark. */
@Composable
private fun BuildCard() {
    val palette = LocalPalette.current
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 6.dp)
            .fillMaxWidth()
            .pane(CARD)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Brush.linearGradient(listOf(palette.accent, palette.accentAlt))),
            contentAlignment = Alignment.Center,
        ) {
            Text("Z", color = inkOnTint(palette.accent), fontSize = 26.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Zenith ${BuildConfig.VERSION_NAME.removeSuffix("-debug")}", color = palette.onGlass, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Note("Build ${BuildConfig.VERSION_CODE} · on this phone now")
        }
    }
}

@Composable
private fun NewsCard(news: News, latest: Boolean) {
    val palette = LocalPalette.current
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
            .fillMaxWidth()
            .pane(CARD)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                news.version,
                color = if (latest) inkOnTint(palette.accent) else palette.onGlass,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (latest) palette.accent else palette.onGlass.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            )
            Spacer(Modifier.width(10.dp))
            Note(news.date)
            if (latest) {
                Spacer(Modifier.weight(1f))
                Text("NEW", color = palette.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(news.title, color = palette.onGlass, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 23.sp)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            news.lines.forEach { line ->
                Row {
                    Text("•", color = palette.onGlassMuted, fontSize = 14.sp, modifier = Modifier.width(14.dp))
                    Text(line, color = palette.onGlass.copy(alpha = 0.88f), fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        }
    }
}

/** What went wrong, in your words, with this phone's details under them, sent through an app of yours. */
@Composable
private fun ReportCard(state: ConnectState) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val lite = LocalLite.current
    val theme = LocalTheme.current
    val lines = ConnectReport.lines(
        version = BuildConfig.VERSION_NAME,
        code = BuildConfig.VERSION_CODE,
        model = Build.MODEL.orEmpty(),
        maker = Build.MANUFACTURER.orEmpty(),
        release = Build.VERSION.RELEASE.orEmpty(),
        lite = lite,
        theme = theme.id,
    )
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .pane(CARD)
            .padding(16.dp),
    ) {
        Note("Say what happened. It goes out in a message you send yourself, with these details under it, so nothing leaves your phone without you.")
        Spacer(Modifier.height(12.dp))
        ConnectField(
            value = state.report,
            onChange = { state.report = it },
            hint = "What happened, and what did you expect?",
            singleLine = false,
            action = ImeAction.Default,
        )
        Spacer(Modifier.height(10.dp))
        Text(lines.joinToString("\n"), color = palette.onGlassMuted, fontSize = 12.sp, lineHeight = 17.sp)
        Spacer(Modifier.height(12.dp))
        ConnectButton(
            "Send report",
            onClick = {
                val why = ConnectReport.problem(state.report)
                if (why != null) {
                    state.say(why)
                } else {
                    sendText(context, ConnectReport.text(state.report, lines), "Send a problem report", subject = "Zenith problem report")
                }
            },
            icon = Icons.AutoMirrored.Rounded.Send,
            tinted = true,
            height = 44.dp,
        )
    }
}

@Composable
private fun ServerCard(state: ConnectState, bridge: ConnectBridge) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .pane(CARD)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Dot(serverColour(state.server), size = 10.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(ConnectServer.pill(state.server), color = palette.onGlass, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Note(
                when (state.server) {
                    ServerState.NONE -> "This build has no server: Connect works on this phone only."
                    ServerState.UP -> "Featured Looks, codes, sharing and backups all go through it."
                    ServerState.ASKING -> "Asking it how it is…"
                    ServerState.DOWN -> "Codes, sharing and backups wait until it answers. Picks and your own Looks still work."
                },
            )
        }
        if (bridge.server) {
            Spacer(Modifier.width(10.dp))
            ConnectButton("Try again", onClick = { state.load(bridge, force = true) }, icon = Icons.Rounded.Refresh, height = 40.dp)
        }
    }
}
