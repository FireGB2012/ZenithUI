package com.gabriel.zenithui

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.gabriel.zenithui.data.HostedSizes
import com.gabriel.zenithui.data.HostedWidgetCell
import com.gabriel.zenithui.data.LayoutBuilder
import com.gabriel.zenithui.data.ROW_UNITS
import com.gabriel.zenithui.data.SettingsStore
import com.gabriel.zenithui.data.ShortcutCell
import com.gabriel.zenithui.data.ZenithWidgetHost
import com.gabriel.zenithui.ui.common.ZenithButton
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.MotionLevel
import com.gabriel.zenithui.ui.common.rememberAnimationsEnabled
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.ZenithTheme
import androidx.compose.runtime.setValue
import com.gabriel.zenithui.ui.home.ZenithSheet
import com.gabriel.zenithui.ui.home.SHEET_GONE_MS
import com.gabriel.zenithui.ui.settings.Section
import com.gabriel.zenithui.ui.theme.LocalApple
import com.gabriel.zenithui.ui.theme.ButtonKind
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.fillMaxSize

/**
 * Where another app's "Add to home screen" lands: Chrome's shortcut to a page, a messaging app's
 * shortcut to one chat, an app's own "add this widget". Android hands it to the home app, and
 * Zenith shows it on one of its own sheets over the app that asked. Say yes and it goes on the first
 * free spot of the home screen, and you stay where you were; the next time you go home it is there.
 */
class PinItemActivity : ComponentActivity() {

    private var request: LauncherApps.PinItemRequest? = null

    /** A widget waiting for Android's word that Zenith may show it: its number, and what it is. */
    private var binding: Pair<Int, AppWidgetProviderInfo>? = null
    private val closing = mutableStateOf(false)

    private val bindWidget = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val (id, info) = binding ?: return@registerForActivityResult
        binding = null
        if (result.resultCode == RESULT_OK) placeWidget(id, info) else runCatching { ZenithWidgetHost.get(this).deleteAppWidgetId(id) }
        closing.value = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val launcherApps = getSystemService(LauncherApps::class.java)
        val asked = runCatching { launcherApps.getPinItemRequest(intent) }.getOrNull()?.takeIf { it.isValid }
        if (asked == null) {
            finish()
            return
        }
        request = asked
        val density = resources.displayMetrics.densityDpi

        val card: Card = when (asked.requestType) {
            LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT -> {
                val info = asked.shortcutInfo ?: return finish()
                val label = (info.shortLabel ?: info.longLabel ?: "").toString()
                Card(
                    label = label,
                    from = appLabel(info.`package`),
                    picture = runCatching { launcherApps.getShortcutIconDrawable(info, density)?.toBitmap(192, 192)?.asImageBitmap() }.getOrNull(),
                    wide = false,
                    add = { addShortcut(info.`package`, info.id, label) },
                )
            }
            LauncherApps.PinItemRequest.REQUEST_TYPE_APPWIDGET -> {
                val info = asked.getAppWidgetProviderInfo(this) ?: return finish()
                Card(
                    label = runCatching { info.loadLabel(packageManager) }.getOrDefault("Widget"),
                    from = appLabel(info.provider.packageName),
                    picture = runCatching {
                        (info.loadPreviewImage(this, density) ?: info.loadIcon(this, density))?.let { d ->
                            val w = d.intrinsicWidth.takeIf { it > 0 } ?: 256
                            val h = d.intrinsicHeight.takeIf { it > 0 } ?: 256
                            val scale = minOf(1f, 480f / maxOf(w, h))
                            d.toBitmap((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1)).asImageBitmap()
                        }
                    }.getOrNull(),
                    wide = true,
                    add = { addWidget(info) },
                )
            }
            else -> return finish()
        }

        val store = SettingsStore.get(this)
        setContent {
            val settings by store.settings.collectAsState()
            ZenithTheme(settings = settings) {
                CompositionLocalProvider(LocalMotionLevel provides MotionLevel.of(settings.motion, rememberAnimationsEnabled())) {
                    PinCard(
                        card = card,
                        shape = ZenithShapes.forIconShape(settings.iconShape),
                        closing = closing.value,
                        onGone = { finish() },
                    )
                }
            }
        }
    }

    private fun appLabel(packageName: String): String = runCatching {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)

    /** The first free spot for something this size, or null and a word that there is none. */
    private fun freeSlot(spanX: Int, spanY: Int): Triple<Int, Int, Int>? {
        val store = SettingsStore.get(this)
        val now = store.settings.value
        val slot = LayoutBuilder.firstFreeSlot(store.layout.value, now.columns, now.rows, spanX, spanY)
        if (slot == null) Toast.makeText(this, "Every page is full", Toast.LENGTH_SHORT).show()
        return slot
    }

    /** True when it was put on the home screen, so the card can go. */
    private fun addShortcut(packageName: String, id: String, label: String): Boolean {
        val slot = freeSlot(1, ROW_UNITS) ?: return false
        // Accepting is what pins it: only then is it Zenith's to show.
        if (runCatching { request?.accept() }.getOrNull() != true) return false
        SettingsStore.get(this).mutateLayout { items ->
            items + ShortcutCell(LayoutBuilder.newId(), slot.first, slot.second, slot.third, packageName, id, label)
        }
        return true
    }

    /**
     * A widget needs a number from Zenith's host and Android's word that Zenith may show it, which
     * is asked for only the first time. False while that question is up; the card goes when it is
     * answered.
     */
    private fun addWidget(info: AppWidgetProviderInfo): Boolean {
        val host = ZenithWidgetHost.get(this)
        val id = runCatching { host.allocateAppWidgetId() }.getOrNull() ?: return false
        val bound = runCatching {
            AppWidgetManager.getInstance(this).bindAppWidgetIdIfAllowed(id, info.profile, info.provider, null)
        }.getOrDefault(false)
        if (bound) return placeWidget(id, info)
        binding = id to info
        val ask = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)
        if (runCatching { bindWidget.launch(ask) }.isFailure) {
            binding = null
            runCatching { host.deleteAppWidgetId(id) }
            return true
        }
        return false
    }

    private fun placeWidget(id: Int, info: AppWidgetProviderInfo): Boolean {
        val store = SettingsStore.get(this)
        val now = store.settings.value
        // Near enough the page's own cells: the screen shared out among its columns and rows.
        val metrics = resources.displayMetrics
        val cellDp = metrics.widthPixels / metrics.density / now.columns.coerceAtLeast(1)
        val rowDp = (metrics.heightPixels / metrics.density - 220f) / now.rows.coerceAtLeast(1)
        val targetX = if (Build.VERSION.SDK_INT >= 31) info.targetCellWidth else 0
        val targetY = if (Build.VERSION.SDK_INT >= 31) info.targetCellHeight else 0
        val (spanX, spanY) = HostedSizes.spanFor(info.minWidth, info.minHeight, targetX, targetY, cellDp, rowDp, now.columns, now.rows)
        val slot = freeSlot(spanX, spanY)
        if (slot == null) {
            runCatching { ZenithWidgetHost.get(this).deleteAppWidgetId(id) }
            return true
        }
        val accepted = runCatching {
            request?.accept(Bundle().apply { putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, id) })
        }.getOrNull() == true
        if (!accepted) {
            runCatching { ZenithWidgetHost.get(this).deleteAppWidgetId(id) }
            return true
        }
        store.mutateLayout { items ->
            items + HostedWidgetCell(LayoutBuilder.newId(), slot.first, slot.second, slot.third, spanX, spanY, id, info.provider.flattenToString())
        }
        return true
    }

    override fun finish() {
        super.finish()
        // The card has already gone; the window goes with nothing more to see.
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}

/** What the card shows, and what saying yes to it does: true when it is done and the card can go. */
private class Card(
    val label: String,
    val from: String,
    val picture: ImageBitmap?,
    /** A widget's picture is shown at its own shape; a shortcut's is cut to the icon shape. */
    val wide: Boolean,
    val add: () -> Boolean,
)

/**
 * The card: what is to be added and whose it is, and two answers, on one of Zenith's own sheets.
 * It comes up from the foot of the screen as they all do, and goes back down whichever answer is
 * given, or when it is pulled down, its X or the space above it is tapped, or back is pressed.
 * Once it has gone, so has the screen.
 */
@Composable
private fun PinCard(card: Card, shape: Shape, closing: Boolean, onGone: () -> Unit) {
    val apple = LocalApple.current
    var open by remember { mutableStateOf(true) }
    // Android's answer about a widget closes it from outside.
    LaunchedEffect(closing) { if (closing) open = false }
    // Once it has been put away, and has had time to go, the screen closes.
    LaunchedEffect(open) {
        if (!open) {
            delay(SHEET_GONE_MS.toLong())
            onGone()
        }
    }
    BackHandler(enabled = open) { open = false }

    ZenithSheet(visible = open, onDismiss = { open = false }, title = "Add to home screen") {
        Section {
            if (card.wide) {
                // A widget's picture, at its own shape, in a well of the card-on-a-card grey.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 12.dp)
                        .height(150.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(apple.raised),
                    contentAlignment = Alignment.Center,
                ) {
                    card.picture?.let { Image(it, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(10.dp)) }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!card.wide) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(shape)
                            .background(apple.raised),
                        contentAlignment = Alignment.Center,
                    ) {
                        card.picture?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
                    }
                    Spacer(Modifier.width(14.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(card.label, color = apple.words, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("From ${card.from}", color = apple.secondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ZenithButton(text = "Not now", onClick = { open = false }, modifier = Modifier.weight(1f))
            ZenithButton(
                text = "Add",
                kind = ButtonKind.Main,
                onClick = { if (card.add()) open = false },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
