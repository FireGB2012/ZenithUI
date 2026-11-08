package com.gabriel.zenithui.ui.settings

import android.app.role.RoleManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.SmartButton
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.AnimationPreset
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.IconShapes
import com.gabriel.zenithui.data.PlusStatus
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.FullPage
import com.gabriel.zenithui.ui.common.LargeTitle
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.PAGE_GONE_MS
import com.gabriel.zenithui.ui.common.SectionLabel
import com.gabriel.zenithui.ui.common.TITLE_GONE_PX
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.popIn
import com.gabriel.zenithui.ui.common.popOut
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import com.gabriel.zenithui.ui.theme.Palettes
import kotlinx.coroutines.delay
import androidx.compose.material.icons.rounded.AccountCircle
import com.gabriel.zenithui.data.server.Signed

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
    /** Spends aura on a theme; false when there is not enough, or it is already owned. */
    val unlockTheme: (String) -> Boolean,
    /** Takes somebody else's code; returns why not, or null when it was taken. With a server it is the server that is asked. */
    val redeem: suspend (String) -> String?,
    val help: () -> Unit,
    val reset: () -> Unit,
    /** Puts the layout back in line; returns how many items had to move. */
    val tidy: () -> Int,
    /** The Zenith account, in a build that has a server to keep one on; null in a build that has none. */
    val account: AccountActions? = null,
    /** Opens the list of what one gesture could do. */
    val gesture: (String) -> Unit = {},
    /** Says what the lock helper is, and opens Android's page for it. */
    val lockHelper: () -> Unit = {},
    /** Everything the Looks page does. */
    val looks: LookActions? = null,
    /** The Glass studio's sheet (Plus). */
    val glassStudio: () -> Unit = {},
    /** The sheet of a palette for each part of the day (Plus). */
    val dayPalettes: () -> Unit = {},
    /** The fonts Zenith carries, and their licence. */
    val fontLicences: () -> Unit = {},
)

/**
 * Settings: a page of its own, the full height of the screen. The first page is a search box and
 * eight groups, each saying how it is set now, so most questions are answered without opening
 * anything; a build with a server has a ninth, the account. A group opens with a push from the
 * side; back, the arrow or a swipe in from the left edge pops it. Searching finds any option by
 * its name or by other words for it, opens its page and shows which row it is.
 *
 * It arrives in order: the search box, then each group, rising in one after another, and the same
 * for the blocks of a page as it is pushed. Coming back to the groups from a page they are simply
 * there; they have been seen.
 *
 * A sheet opened from here — wallpapers, Plus — closes back onto Settings. While one is open
 * ([covered]) back belongs to the sheet.
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
    /** The Looks saved on this phone. */
    looks: List<com.gabriel.zenithui.data.Look> = emptyList(),
) {
    var page by remember { mutableStateOf<SettingsPage?>(null) }
    var query by remember { mutableStateOf("") }
    var highlight by remember { mutableStateOf<String?>(null) }
    // Set when the groups come back on show because a page was popped. Not state: nothing has to
    // be redrawn when it changes, it is only read as the groups are composed again.
    val returned = remember { booleanArrayOf(false) }
    val reduced = LocalMotionLevel.current.reduced
    val rootScroll = rememberScrollState()
    val pageScroll = rememberScrollState()
    // The next time Settings opens it starts at the top. That is put right once the page has
    // gone, not as it comes back, so it never arrives showing the page it was closed on.
    LaunchedEffect(visible) {
        if (!visible) {
            delay(PAGE_GONE_MS)
            page = null
            query = ""
            highlight = null
            rootScroll.scrollTo(0)
        }
    }

    fun open(target: SettingsPage, entry: String?) {
        if (target.opensSheet) {
            actions.plus()
        } else {
            highlight = entry
            page = target
        }
    }

    fun pop() {
        // The advanced page goes back to the page it is part of, not all the way out.
        if (page == SettingsPage.AdvancedMotion) {
            page = SettingsPage.Motion
            highlight = "advancedMotion"
            return
        }
        returned[0] = true
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

    LaunchedEffect(page) { if (page != null) pageScroll.scrollTo(0) }

    FullPage(
        visible = visible,
        covered = covered,
        title = page?.title ?: "Settings",
        canGoBack = page != null,
        titleShown = { ((if (page == null) rootScroll.value else pageScroll.value) / TITLE_GONE_PX).coerceIn(0f, 1f) },
        onBack = { pop() },
        onClose = onDismiss,
    ) {
        // Closed and gone: the next time Settings opens, its groups rise in again.
        DisposableEffect(Unit) { onDispose { returned[0] = false } }

        AnimatedContent(
            targetState = page,
            transitionSpec = {
                // Leaving the advanced page is going back, wherever it goes back to.
                val push = targetState != null && initialState != SettingsPage.AdvancedMotion
                val slide = spring(dampingRatio = 1f, stiffness = 420f, visibilityThreshold = IntOffset(1, 1))
                if (reduced) {
                    // No travelling: one gives way to the other where it stands.
                    fadeIn(tween(Motion.REDUCED_MS)).togetherWith(fadeOut(tween(Motion.REDUCED_MS)))
                } else {
                    // The page comes in over the groups from the right, and leaves the same way;
                    // the groups shift a little and dim underneath it.
                    ContentTransform(
                        targetContentEnter = slideInHorizontally(slide) { if (push) it else -it / 4 } + fadeIn(tween(220)),
                        initialContentExit = slideOutHorizontally(slide) { if (push) -it / 4 else it } + fadeOut(tween(220)),
                        targetContentZIndex = if (push) 1f else 0f,
                    )
                }
            },
            label = "settingsPage",
        ) { current ->
            val scroll = if (current == null) rootScroll else pageScroll
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LargeTitle(current?.title ?: "Settings", scrolled = { scroll.value })
                CompositionLocalProvider(
                    // What rises in: the groups when Settings opens, a page's blocks when it is pushed.
                    LocalSettingsRise provides when {
                        current != null -> current
                        returned[0] -> null
                        else -> "groups"
                    },
                    LocalSettingsHighlight provides if (current == null) null else highlight,
                ) {
                    when (current) {
                        null -> Root(settings, plus, actions.account, query, onQuery = { query = it }, onOpen = ::open)
                        SettingsPage.Theme -> ThemeOptions(settings, plus, preview, actions)
                        SettingsPage.Home -> HomeOptions(settings, plus, preview, actions)
                        SettingsPage.Motion -> MotionOptions(settings, actions, onAdvanced = { open(SettingsPage.AdvancedMotion, null) })
                        SettingsPage.AdvancedMotion -> AdvancedMotionOptions(settings, actions)
                        SettingsPage.Gestures -> GestureOptions(settings, preview, actions)
                        SettingsPage.Apps -> AppsOptions(settings, plus, actions)
                        SettingsPage.Backup -> BackupOptions(plus, actions, onClose = onDismiss, onAccount = { open(SettingsPage.Account, null) })
                        SettingsPage.About -> AboutOptions(actions)
                        SettingsPage.Aura -> AuraOptions(settings, actions, onAccount = { open(SettingsPage.Account, null) })
                        SettingsPage.Looks -> actions.looks?.let { LooksOptions(settings, plus, looks, it) }
                        SettingsPage.Island -> IslandOptions(settings, plus, actions)
                        SettingsPage.Account -> AccountOptions(actions)
                        // Opens a sheet instead; see open().
                        SettingsPage.Plus -> Unit
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
        if (page != null) {
            // A drag in from the left edge goes back, for phones with buttons instead of gestures.
            // Where the system has its own back swipe, that gets there first.
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

// ---------------------------------------------------------------------------------- first page

@Composable
private fun Root(
    settings: ZenithSettings,
    plus: PlusStatus,
    account: AccountActions?,
    query: String,
    onQuery: (String) -> Unit,
    onOpen: (SettingsPage, String?) -> Unit,
) {
    SearchField(query, onQuery, Modifier.riseIn(0))
    // The groups give way to what a search finds, and come back when the box is cleared.
    AnimatedContent(
        targetState = query.isBlank(),
        transitionSpec = {
            (fadeIn(tween(200, delayMillis = 60)) + slideInVertically(tween(260)) { it / 24 })
                .togetherWith(fadeOut(tween(110)))
                .using(SizeTransform(clip = false))
        },
        label = "settingsFind",
    ) { browsing ->
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                browsing -> Groups(settings, plus, account, onOpen)
                // On its way out after the box was cleared there is nothing left to say.
                query.isNotBlank() -> Results(query, server = account != null, onOpen = onOpen)
            }
        }
    }
}

@Composable
private fun Groups(settings: ZenithSettings, plus: PlusStatus, account: AccountActions?, onOpen: (SettingsPage, String?) -> Unit) {
    val context = LocalContext.current
    val isHome = remember { context.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_HOME) == true }
    // Looks and feel; what you own; the phone itself. Each block is headed with what it holds and
    // stands further from the next than its rows do from each other, so the page reads as three
    // and the eye can go straight to one. A heading rises in with its block.
    listOf(
        "Look and feel" to listOf(SettingsPage.Theme, SettingsPage.Home, SettingsPage.Motion, SettingsPage.Gestures, SettingsPage.Apps),
        // The account is only there in a build with a server to keep it on.
        (if (account != null) "Plus, Aura and account" else "Plus and Aura") to
            listOfNotNull(SettingsPage.Plus, SettingsPage.Island, SettingsPage.Looks, SettingsPage.Aura, SettingsPage.Account.takeIf { account != null }),
        "Backup and help" to listOf(SettingsPage.Backup, SettingsPage.About),
    ).forEachIndexed { block, (heading, pages) ->
        Column(
            Modifier
                .riseIn(block + 1)
                .padding(top = if (block == 0) 2.dp else 10.dp),
        ) {
            // Over the tiles of its rows, not over the edge of the group.
            SectionLabel(heading, Modifier.padding(start = 14.dp, bottom = 8.dp))
            Group {
                pages.forEachIndexed { index, page ->
                    if (index > 0) GroupDivider()
                    NavRow(
                        icon = page.icon(),
                        tile = page.tile(),
                        title = page.title,
                        summary = summary(page, settings, plus, isHome, account?.signed),
                        onClick = { onOpen(page, null) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Results(query: String, server: Boolean, onOpen: (SettingsPage, String?) -> Unit) {
    val found = remember(query, server) { SettingsCatalog.search(query, server) }
    if (found.isEmpty()) {
        Note("Nothing in Settings is called “${query.trim()}”.", Modifier.padding(top = 6.dp))
        return
    }
    Group {
        found.forEachIndexed { index, entry ->
            // A result that was already there stays put as you type on; a new one rises in.
            key(entry.id) {
                Column(Modifier.appearIn(fromScale = 1f, riseFrom = 20f, gentle = true)) {
                    if (index > 0) GroupDivider(inset = 16.dp)
                    ResultRow(entry) { onOpen(entry.page, entry.id) }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    var focused by remember { mutableStateOf(false) }
    // In use, the box is ringed in the accent and its glass lights up.
    val ring by animateColorAsState(if (focused) palette.accent.copy(alpha = 0.75f) else Color.Transparent, tween(220), label = "searchRing")
    val lens by animateColorAsState(if (focused) palette.accent else palette.onGlassMuted, tween(220), label = "searchLens")
    Row(
        modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(palette.onGlass.copy(alpha = 0.08f))
            .border(1.5.dp, ring, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = lens, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) Text("Search settings", color = palette.onGlassMuted, fontSize = 15.sp)
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(fontFamily = LocalZenithFont.current, color = palette.onGlass, fontSize = 15.sp),
                cursorBrush = SolidColor(palette.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused },
            )
        }
        AnimatedVisibility(
            visible = query.isNotEmpty(),
            enter = popIn(reduced, millis = 140),
            exit = popOut(reduced, millis = 100),
        ) {
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
            if (!entry.page.opensSheet && entry.title != entry.page.title) {
                Text(entry.page.title, color = palette.onGlassMuted, fontSize = 13.sp)
            }
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
    SettingsPage.AdvancedMotion -> Icons.Rounded.Animation
    SettingsPage.Gestures -> Icons.Rounded.Gesture
    SettingsPage.Apps -> Icons.Rounded.Apps
    SettingsPage.Plus -> Icons.Rounded.AutoAwesome
    SettingsPage.Island -> Icons.Rounded.SmartButton
    SettingsPage.Looks -> Icons.Rounded.Style
    SettingsPage.Aura -> Icons.Rounded.LocalFireDepartment
    SettingsPage.Account -> Icons.Rounded.AccountCircle
    SettingsPage.Backup -> Icons.Rounded.SettingsBackupRestore
    SettingsPage.About -> Icons.Rounded.Info
}

/** How a group is set right now, in a line: what the first page is for. */
private fun summary(page: SettingsPage, settings: ZenithSettings, plus: PlusStatus, isHome: Boolean, signed: Signed?): String = when (page) {
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
    SettingsPage.AdvancedMotion -> AnimationPreset.byId(settings.animation.preset).label
    SettingsPage.Gestures -> when (val changed = settings.gestures.size) {
        0 -> "Swipe down for notifications, pinch for Customise"
        1 -> "1 gesture set by you"
        else -> "$changed gestures set by you"
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
    SettingsPage.Island -> when {
        !plus.active -> "Plus: your camera, stretched, over every app"
        settings.island.on -> "On: " + listOfNotNull(
            "music".takeIf { settings.island.music },
            "calls".takeIf { settings.island.live },
            "notifications".takeIf { settings.island.notifications },
            "charger".takeIf { settings.island.charging },
        ).joinToString(", ").ifEmpty { "nothing chosen" }
        else -> "Off"
    }
    SettingsPage.Looks -> when {
        !plus.active -> "Plus: save looks, switch, schedule and share them"
        settings.lookRules.isNotEmpty() -> "${settings.lookRules.size} on a schedule"
        else -> "Save your home screen and switch in a tap"
    }
    SettingsPage.Aura -> if (settings.streak > 0) "${settings.aura} aura, ${settings.streak} day streak" else "${settings.aura} aura"
    SettingsPage.Account -> when {
        signed == null -> "Sign in, or create one"
        signed.account.username != null -> "@${signed.account.username}"
        else -> signed.email ?: "Signed in with Google"
    }
    SettingsPage.Backup -> "Save your home screen, or start again"
    SettingsPage.About -> if (isHome) "Zenith is your home screen" else "Zenith is not your home screen yet"
}
