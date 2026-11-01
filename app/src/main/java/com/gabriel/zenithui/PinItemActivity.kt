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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.gabriel.zenithui.ui.common.GlassButton
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.MotionLevel
import com.gabriel.zenithui.ui.common.rememberAnimationsEnabled
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.ZenithTheme
import com.gabriel.zenithui.ui.theme.glass
import kotlinx.coroutines.flow.first

/**
 * Where another app's "Add to home screen" lands: Chrome's shortcut to a page, a messaging app's
 * shortcut to one chat, an app's own "add this widget". Android hands it to the home app, and
 * Zenith shows it on a card of glass over the app that asked. Say yes and it goes on the first
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
 * The card: what is to be added and whose it is, and two answers. It comes up from the foot of
 * the screen like any of Zenith's sheets, and goes back down whichever answer is given.
 */
@Composable
private fun PinCard(card: Card, shape: Shape, closing: Boolean, onGone: () -> Unit) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    val shown = remember { MutableTransitionState(false).apply { targetState = true } }
    if (closing) shown.targetState = false
    // Once it has been put away, and has finished going, the screen closes.
    LaunchedEffect(shown) {
        snapshotFlow { !shown.targetState && shown.isIdle }.first { it }
        onGone()
    }
    BackHandler { shown.targetState = false }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visibleState = shown, enter = fadeIn(tween(240)), exit = fadeOut(tween(220))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { shown.targetState = false },
            )
        }
        AnimatedVisibility(
            visibleState = shown,
            enter = if (reduced) fadeIn(tween(Motion.REDUCED_MS)) else slideInVertically(Motion.sheet()) { it } + fadeIn(tween(220)),
            exit = if (reduced) fadeOut(tween(Motion.REDUCED_MS)) else slideOutVertically(tween(300)) { it } + fadeOut(tween(240)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .glass(RoundedCornerShape(34.dp), strength = 1.2f, solid = 0.65f)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Text("Add to home screen", color = palette.onGlass, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))
                if (card.wide) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(palette.onGlass.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        card.picture?.let { Image(it, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(10.dp)) }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!card.wide) {
                        Box(
                            Modifier
                                .size(56.dp)
                                .clip(shape)
                                .background(palette.onGlass.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            card.picture?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
                        }
                        Spacer(Modifier.width(14.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(card.label, color = palette.onGlass, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("From ${card.from}", color = palette.onGlassMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton(text = "Not now", onClick = { shown.targetState = false }, modifier = Modifier.weight(1f))
                    GlassButton(
                        text = "Add",
                        filled = true,
                        onClick = { if (card.add()) shown.targetState = false },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
