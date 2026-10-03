package com.gabriel.zenithui.ui.settings

import android.app.role.RoleManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.IconShapes
import com.gabriel.zenithui.data.PlusStatus
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.Palettes
import com.gabriel.zenithui.ui.theme.glass

/** What the mini phone on the Theme and Home pages is a picture of: page one of the real home screen. */
class SettingsPreview(
    val items: List<HomeItem>,
    val appsByKey: Map<String, AppInfo>,
    val wallpaper: ImageBitmap?,
    val screenWidth: Dp,
    val screenHeight: Dp,
)

/** Everything Settings can ask the home screen to do. */
class SettingsActions(
    val change: ((ZenithSettings) -> ZenithSettings) -> Unit,
    val wallpaper: () -> Unit,
    val themeMaker: () -> Unit,
    val hiddenApps: () -> Unit,
    val freeform: (Boolean) -> Unit,
    val backup: () -> Unit,
    val restore: () -> Unit,
    val plus: () -> Unit,
    val aura: () -> Unit,
    val help: () -> Unit,
    val reset: () -> Unit,
    /** Puts the layout back in line; returns how many items had to move. */
    val tidy: () -> Int,
)

/**
 * Settings: a page of its own, the full height of the screen. The first page is a search box and
 * eight groups, each saying how it is set now, so most questions are answered without opening
 * anything. A group opens with a push from the side; back, the arrow or a swipe in from the left
 * edge pops it. Searching finds any option by its name or by other words for it, opens its page
 * and shows which row it is.
 *
 * It sits under the sheets, not among them: a sheet opened from here — wallpapers, Plus, aura —
 * closes back onto Settings. While one is open ([covered]) back belongs to the sheet.
 */
@Composable
fun SettingsScreen(
    visible: Boolean,
    covered: Boolean,
    settings: ZenithSettings,
    plus: PlusStatus,
    preview: SettingsPreview,
    actions: SettingsActions,
    onDismiss: () -> Unit,
) {
    var page by remember { mutableStateOf<SettingsPage?>(null) }
    var query by remember { mutableStateOf("") }
    var highlight by remember { mutableStateOf<String?>(null) }
    // Opening Settings again always starts at the top.
    LaunchedEffect(visible) {
        if (visible) {
            page = null
            query = ""
            highlight = null
        }
    }

    fun open(target: SettingsPage, entry: String?) {
        when (target) {
            SettingsPage.Plus -> actions.plus()
            SettingsPage.Aura -> actions.aura()
            else -> {
                highlight = entry
                page = target
            }
        }
    }

    fun pop() {
        page = null
        highlight = null
    }

    BackHandler(enabled = visible && !covered) {
        when {
            page != null -> pop()
            query.isNotEmpty() -> query = ""
            else -> onDismiss()
        }
    }

    val palette = LocalPalette.current
    val focus = LocalFocusManager.current
    val rootScroll = rememberScrollState()
    val pageScroll = rememberScrollState()
    LaunchedEffect(page) { if (page != null) pageScroll.scrollTo(0) }
    // Under a sheet the page empties to plain glass: where the glass is only frosted, a sheet is
    // see-through, and two layers of words on top of each other can be read by nobody.
    val uncovered = animateFloatAsState(if (covered) 0f else 1f, tween(200), label = "settingsUncovered")

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = visible, enter = fadeIn(tween(300)), exit = fadeOut(tween(260))) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
        }
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(Motion.smooth()) { it } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(320)) { it } + fadeOut(tween(260)),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .glass(RectangleShape, strength = 1.2f, solid = 0.9f, border = false, shadow = false)
                    // Nothing underneath takes a touch; a tap on empty space puts the keyboard away.
                    .pointerInput(Unit) { detectTapGestures { focus.clearFocus() } }
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                Column(Modifier.fillMaxSize().graphicsLayer { alpha = uncovered.value }) {
                TopBar(
                    title = page?.title ?: "Settings",
                    canGoBack = page != null,
                    // The small title comes in as the large one scrolls away under the bar.
                    titleShown = { ((if (page == null) rootScroll.value else pageScroll.value) / 110f).coerceIn(0f, 1f) },
                    onBack = { pop() },
                    onClose = onDismiss,
                )
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    AnimatedContent(
                        targetState = page,
                        transitionSpec = {
                            val push = targetState != null
                            val slide = spring(dampingRatio = 1f, stiffness = 420f, visibilityThreshold = IntOffset(1, 1))
                            // The page comes in over the groups from the right, and leaves the same
                            // way; the groups shift a little and dim underneath it.
                            ContentTransform(
                                targetContentEnter = slideInHorizontally(slide) { if (push) it else -it / 4 } + fadeIn(tween(220)),
                                initialContentExit = slideOutHorizontally(slide) { if (push) -it / 4 else it } + fadeOut(tween(220)),
                                targetContentZIndex = if (push) 1f else 0f,
                            )
                        },
                        label = "settingsPage",
                    ) { current ->
                        Column(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(if (current == null) rootScroll else pageScroll)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                current?.title ?: "Settings",
                                color = palette.onGlass,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.6).sp,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 4.dp),
                            )
                            if (current == null) {
                                Root(settings, plus, query, onQuery = { query = it }, onOpen = ::open)
                            } else {
                                CompositionLocalProvider(LocalSettingsHighlight provides highlight) {
                                    when (current) {
                                        SettingsPage.Theme -> ThemeOptions(settings, preview, actions)
                                        SettingsPage.Home -> HomeOptions(settings, plus, preview, actions)
                                        SettingsPage.Motion -> MotionOptions(settings, actions)
                                        SettingsPage.Apps -> AppsOptions(settings, plus, actions)
                                        SettingsPage.Backup -> BackupOptions(plus, actions, onClose = onDismiss)
                                        SettingsPage.About -> AboutOptions(actions)
                                        // These two open a sheet instead; see open().
                                        SettingsPage.Plus, SettingsPage.Aura -> Unit
                                    }
                                }
                            }
                            Spacer(Modifier.height(28.dp))
                        }
                    }
                    if (page != null) {
                        // A drag in from the left edge goes back, for phones with buttons instead
                        // of gestures. Where the system has its own back swipe, that gets there first.
                        Box(
                            Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight()
                                .width(20.dp)
                                .pointerInput(Unit) {
                                    var travelled = 0f
                                    detectHorizontalDragGestures(
                                        onDragStart = { travelled = 0f },
                                        onDragEnd = { if (travelled > 56.dp.toPx()) pop() },
                                    ) { change, amount ->
                                        change.consume()
                                        travelled += amount
                                    }
                                }
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun TopBar(title: String, canGoBack: Boolean, titleShown: () -> Float, onBack: () -> Unit, onClose: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            // Pulling the bar down closes Settings, the way pulling a sheet down closes a sheet.
            .pointerInput(Unit) { detectVerticalDragGestures { _, delta -> if (delta > 30f) onClose() } }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp)) {
            androidx.compose.animation.AnimatedVisibility(
                visible = canGoBack,
                enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.7f),
                exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.7f),
            ) {
                BackButton(onBack)
            }
        }
        Text(
            title,
            color = palette.onGlass,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .graphicsLayer { alpha = titleShown() },
        )
        Box(
            Modifier
                .size(36.dp)
                .pressBounce(0.88f)
                .clip(CircleShape)
                .background(palette.onGlass.copy(alpha = 0.08f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "Close settings", tint = palette.onGlass, modifier = Modifier.size(19.dp))
        }
    }
}

// ---------------------------------------------------------------------------------- first page

@Composable
private fun Root(
    settings: ZenithSettings,
    plus: PlusStatus,
    query: String,
    onQuery: (String) -> Unit,
    onOpen: (SettingsPage, String?) -> Unit,
) {
    SearchField(query, onQuery)
    if (query.isBlank()) {
        val context = LocalContext.current
        val isHome = remember { context.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_HOME) == true }
        // Looks and feel; what you own; the phone itself.
        listOf(
            listOf(SettingsPage.Theme, SettingsPage.Home, SettingsPage.Motion, SettingsPage.Apps),
            listOf(SettingsPage.Plus, SettingsPage.Aura),
            listOf(SettingsPage.Backup, SettingsPage.About),
        ).forEach { pages ->
            Group {
                pages.forEachIndexed { index, page ->
                    if (index > 0) GroupDivider()
                    NavRow(
                        icon = page.icon(),
                        tile = page.tile(),
                        title = page.title,
                        summary = summary(page, settings, plus, isHome),
                        onClick = { onOpen(page, null) },
                    )
                }
            }
        }
    } else {
        val found = remember(query) { SettingsCatalog.search(query) }
        if (found.isEmpty()) {
            Note("Nothing in Settings is called “${query.trim()}”.", Modifier.padding(top = 6.dp))
        } else {
            Group {
                found.forEachIndexed { index, entry ->
                    if (index > 0) GroupDivider(inset = 16.dp)
                    ResultRow(entry) { onOpen(entry.page, entry.id) }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(palette.onGlass.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.onGlassMuted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) Text("Search settings", color = palette.onGlassMuted, fontSize = 15.sp)
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(color = palette.onGlass, fontSize = 15.sp),
                cursorBrush = SolidColor(palette.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Clear search",
                tint = palette.onGlassMuted,
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .clickable { onQuery("") }
                    .padding(3.dp),
            )
        }
    }
}

/** One thing a search found: what it is called, and the page it is on. */
@Composable
private fun ResultRow(entry: SettingsEntry, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .pressBounce(0.98f)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(entry.title, color = palette.onGlass, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            // A group that is its own sheet needs no "in": the title already says where it goes.
            if (!entry.page.opensSheet) Text(entry.page.title, color = palette.onGlassMuted, fontSize = 13.sp)
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = palette.onGlass.copy(alpha = 0.35f),
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun SettingsPage.icon(): ImageVector = when (this) {
    SettingsPage.Theme -> Icons.Rounded.Palette
    SettingsPage.Home -> Icons.Rounded.GridView
    SettingsPage.Motion -> Icons.Rounded.Animation
    SettingsPage.Apps -> Icons.Rounded.Apps
    SettingsPage.Plus -> Icons.Rounded.AutoAwesome
    SettingsPage.Aura -> Icons.Rounded.LocalFireDepartment
    SettingsPage.Backup -> Icons.Rounded.SettingsBackupRestore
    SettingsPage.About -> Icons.Rounded.Info
}

private fun SettingsPage.tile(): List<Color> = when (this) {
    SettingsPage.Theme -> listOf(Color(0xFF8B7CFF), Color(0xFF5B8CFF))
    SettingsPage.Home -> listOf(Color(0xFF2FC9A6), Color(0xFF1C9C8B))
    SettingsPage.Motion -> listOf(Color(0xFFFF7AB6), Color(0xFFE2559A))
    SettingsPage.Apps -> listOf(Color(0xFF4FB3FF), Color(0xFF2F7FE6))
    SettingsPage.Plus -> listOf(Color(0xFFFFD36B), Color(0xFFF5A623))
    SettingsPage.Aura -> listOf(Color(0xFFFF9F5A), Color(0xFFFF6A4D))
    SettingsPage.Backup -> listOf(Color(0xFF7BC96F), Color(0xFF4CA65A))
    SettingsPage.About -> listOf(Color(0xFF9AA3B2), Color(0xFF6B7384))
}

/** How a group is set right now, in a line: what the first page is for. */
private fun summary(page: SettingsPage, settings: ZenithSettings, plus: PlusStatus, isHome: Boolean): String = when (page) {
    SettingsPage.Theme -> {
        val appearance = when (settings.appearance) {
            "light" -> "light"
            "dark" -> "dark"
            else -> "follows the phone"
        }
        "${Palettes.resolve(settings).name}, $appearance"
    }
    SettingsPage.Home -> "${settings.columns} × ${settings.rows}, ${IconShapes.label(settings.iconShape).lowercase()} icons"
    SettingsPage.Motion -> buildString {
        append(if (settings.motion == "reduced") "Reduced" else "Full")
        append(if (settings.haptics) ", haptics on" else ", haptics off")
    }
    SettingsPage.Apps -> buildString {
        append(if (settings.appsView == "library") "App Library" else "App drawer")
        if (settings.hiddenApps.isNotEmpty() && plus.active) append(", ${settings.hiddenApps.size} hidden")
    }
    SettingsPage.Plus -> when {
        plus.lifetime -> "Yours for good"
        plus.subscribed -> "Subscribed"
        plus.trialDaysLeft > 1 -> "${plus.trialDaysLeft} days of your trial left"
        plus.trialDaysLeft == 1 -> "Last day of your trial"
        else -> "Wallpapers, theme maker, hidden apps, backup"
    }
    SettingsPage.Aura -> if (settings.streak > 0) "${settings.aura} aura, ${settings.streak} day streak" else "${settings.aura} aura"
    SettingsPage.Backup -> "Save your home screen, or start again"
    SettingsPage.About -> if (isHome) "Zenith is your home screen" else "Zenith is not your home screen yet"
}
