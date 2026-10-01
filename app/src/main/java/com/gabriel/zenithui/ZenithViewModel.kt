package com.gabriel.zenithui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gabriel.zenithui.data.AppCell
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.AppRepository
import com.gabriel.zenithui.data.Aura
import com.gabriel.zenithui.data.AuraThemes
import com.gabriel.zenithui.data.CheckIn
import com.gabriel.zenithui.data.Backup
import com.gabriel.zenithui.data.BillingRepository
import com.gabriel.zenithui.data.DropPlanner
import com.gabriel.zenithui.data.FolderCell
import com.gabriel.zenithui.data.GridDensity
import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.IconLoader
import com.gabriel.zenithui.data.LayoutBuilder
import com.gabriel.zenithui.data.LayoutTidy
import com.gabriel.zenithui.data.PageGrid
import com.gabriel.zenithui.data.occupyItem
import com.gabriel.zenithui.data.Plus
import com.gabriel.zenithui.data.PlusStatus
import com.gabriel.zenithui.data.ROW_UNITS
import com.gabriel.zenithui.data.SettingsStore
import com.gabriel.zenithui.data.WidgetCell
import com.gabriel.zenithui.data.Widgets
import com.gabriel.zenithui.data.WallpaperSource
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.data.trialDaysLeft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ZenithViewModel(application: Application) : AndroidViewModel(application) {

    val store = SettingsStore(application)
    val repository = AppRepository(application)
    val icons = IconLoader(application, repository)
    private val wallpaperSource = WallpaperSource(application)
    val billing = BillingRepository(application)

    val settings: StateFlow<ZenithSettings> = store.settings
    val layout: StateFlow<List<HomeItem>> = store.layout
    val apps: StateFlow<List<AppInfo>> = repository.apps
    val backdrop = wallpaperSource.backdrop
    val sharpBackdrop = wallpaperSource.sharpBackdrop

    /**
     * What Zenith Plus is worth to this user right now. Play is the authority on the purchase,
     * but the saved copy is what keeps Plus working on a plane or a dead network.
     */
    val plus: StateFlow<PlusStatus> = combine(
        store.settings,
        billing.purchased,
        billing.subscribed,
        billing.available,
        billing.trialOffer,
        billing.monthlyPrice,
        billing.lifetimePrice,
    ) { values ->
        val settings = values[0] as ZenithSettings
        val purchased = values[1] as Boolean
        val subscribed = values[2] as Boolean
        val available = values[3] as Boolean
        val trialOffer = values[4] as Boolean
        val monthly = values[5] as String
        val lifetimeCost = values[6] as String
        PlusStatus(
            purchased = purchased || settings.plusPurchased,
            lifetime = settings.plusPurchased,
            subscribed = subscribed,
            trialDaysLeft = trialDaysLeft(settings.plusTrialStart),
            trialAvailable = settings.plusTrialStart == 0L,
            monthlyPrice = monthly,
            lifetimePrice = lifetimeCost,
            purchasable = available,
            playTrialOffer = trialOffer,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PlusStatus())

    private val _homePressed = MutableStateFlow(0)

    /** Incremented every time the home button is pressed while Zenith is already showing. */
    val homePressed: StateFlow<Int> = _homePressed.asStateFlow()

    fun onHomePressed() {
        _homePressed.value += 1
    }

    private val _environmentTick = MutableStateFlow(0)

    /** Bumped when something outside the app changed, such as which launcher is the default. */
    val environmentTick: StateFlow<Int> = _environmentTick.asStateFlow()

    fun onEnvironmentChanged() {
        _environmentTick.value += 1
        refreshWallpaper()
    }

    fun noteHomePrompt() = store.update { it.copy(homePrompts = it.homePrompts + 1) }

    init {
        upgradeLayout()
        icons.prime(settings.value.iconPack, settings.value.iconShape)
        viewModelScope.launch { repository.refresh() }
        viewModelScope.launch { wallpaperSource.refresh() }
        wallpaperSource.watch { viewModelScope.launch { wallpaperSource.refresh() } }
        viewModelScope.launch {
            val current = settings.value
            icons.configure(current.iconPack, current.iconShape)
        }
        // The app list arrives asynchronously; once it does, every icon asks again.
        viewModelScope.launch {
            repository.apps.collect { icons.invalidate() }
        }
        repository.observePackages { viewModelScope.launch { repository.refresh() } }
        // A purchase is written down the first time Play mentions it, and never unwritten.
        viewModelScope.launch {
            billing.permanent.collect { bought ->
                if (bought && !settings.value.plusPurchased) store.update { it.copy(plusPurchased = true) }
            }
        }
        billing.refresh()
    }

    /**
     * Brings whatever layout is stored up to today's rules: rescaled into row units, onto whole
     * rows, and settled so nothing overlaps. Safe to run on a layout that already follows them,
     * which is why a restored backup goes through it too.
     */
    private fun upgradeLayout() {
        // Layouts saved before the grid gained sub-rows are rescaled once.
        if (settings.value.layoutVersion < 1) {
            if (layout.value.isNotEmpty()) store.setLayout(LayoutBuilder.upgradeToRowUnits(layout.value))
            store.update { it.copy(layoutVersion = 1) }
        }
        // Undo that fourfold rescale on a home screen that already went through it.
        LayoutBuilder.undoDoubleUpgrade(layout.value, settings.value.rows).let { fixed ->
            if (fixed != layout.value) store.setLayout(fixed)
        }
        // Whole rows for everything, and the search bar as a setting rather than a widget.
        if (settings.value.layoutVersion < LayoutTidy.VERSION) {
            val (next, items) = LayoutTidy.upgrade(settings.value, layout.value)
            store.restore(next, items)
        }
        // A layout left in a state normal placement does not allow — offsets from free placement,
        // widgets grown over their neighbours — is put right once, as soon as it is loaded.
        if (!settings.value.freeform) {
            val current = settings.value
            val settled = LayoutBuilder.settle(layout.value, current.columns, current.rows, free = false)
            if (settled != layout.value) store.setLayout(settled)
        }
    }

    // ---------------------------------------------------------------- Aura

    private val _checkIn = MutableStateFlow<CheckIn?>(null)

    /** The last thing the daily check-in had to say, for the slide-down to show. */
    val checkIn: StateFlow<CheckIn?> = _checkIn.asStateFlow()

    /**
     * Counts today, once.
     *
     * Called every time the launcher comes back to the foreground, which is many times a day, so
     * everything past the first call is a no-op. A missed day resets the streak and pays nothing:
     * today is spent, and tomorrow starts again at day one.
     */
    fun checkInForToday() {
        val current = settings.value
        if (!current.onboarded) return
        val today = Aura.today()
        val last = current.lastCheckIn

        if (last == today) return

        if (last == today - 1 || last == 0L) {
            val streak = current.streak + 1
            val award = Aura.dailyReward(streak)
            store.update {
                it.copy(
                    streak = streak,
                    lastCheckIn = today,
                    aura = it.aura + award,
                    referralCode = it.referralCode.ifEmpty { Aura.newReferralCode() },
                )
            }
            _checkIn.value = CheckIn.Continued(streak = streak, awarded = award)
        } else {
            val lost = current.streak
            store.update {
                it.copy(
                    streak = 0,
                    lastCheckIn = today,
                    referralCode = it.referralCode.ifEmpty { Aura.newReferralCode() },
                )
            }
            _checkIn.value = CheckIn.Broken(previousStreak = lost)
        }
    }

    /** The slide-down has been seen; do not show it again this session. */
    fun clearCheckIn() {
        _checkIn.value = null
    }

    /** Makes sure this install has a code to share, without waiting for a check-in. */
    fun ensureReferralCode() {
        if (settings.value.referralCode.isEmpty()) {
            store.update { it.copy(referralCode = Aura.newReferralCode()) }
        }
    }

    /** Buys a theme with points. False when it costs more than you have, or is already yours. */
    fun unlockTheme(id: String): Boolean {
        val theme = AuraThemes.byId(id) ?: return false
        val current = settings.value
        if (id in current.unlockedThemes) return false
        if (current.aura < theme.price) return false
        store.update {
            it.copy(aura = it.aura - theme.price, unlockedThemes = it.unlockedThemes + id)
        }
        return true
    }

    /**
     * Takes someone else's referral code, once ever.
     *
     * Only the person typing it can be paid: with no server there is nobody to tell that a code
     * was used, so the friend who handed it out gets nothing. The wording in the app says so.
     */
    fun redeemReferral(code: String): String? {
        val cleaned = Aura.cleanCode(code)
        val current = settings.value
        return when {
            current.referredBy.isNotEmpty() -> "You have already used a code."
            !Aura.isWellFormedCode(cleaned) -> "That is not a Zenith code."
            cleaned == current.referralCode -> "That is your own code."
            else -> {
                store.update {
                    it.copy(referredBy = cleaned, aura = it.aura + Aura.REFERRAL_REWARD)
                }
                null
            }
        }
    }

    // ---------------------------------------------------------------- Zenith Plus

    /**
     * Takes the free week. Offered at the end of setup and from the Plus sheet, never taken on
     * someone's behalf: a trial that starts by itself is one you never knew you had.
     */
    fun startTrial() {
        if (settings.value.plusTrialStart == 0L) {
            store.update { it.copy(plusTrialStart = System.currentTimeMillis()) }
        }
    }

    /**
     * Unlocks Plus with a code. It writes the same flag a purchase does, so a redeemed unlock and
     * a bought one behave identically for ever after — including offline and after a restore.
     */
    fun redeemCode(code: String): Boolean {
        if (!Plus.isValidCode(code)) return false
        store.update { it.copy(plusPurchased = true) }
        return true
    }

    fun refreshPurchases() = billing.refresh()

    /** The pass: one payment, kept for ever. */
    fun buyLifetime(activity: android.app.Activity): Boolean = billing.buyLifetime(activity)

    /** The monthly subscription. */
    fun subscribe(activity: android.app.Activity): Boolean = billing.subscribe(activity)

    /**
     * Begins the trial the best way this phone can. When Play has the free-trial subscription it
     * shows its own sheet - the "7 days free, then five pounds" one - and the week is Google's to
     * count. Until that product exists, the week is counted here instead, which is what happens
     * today. Returns true if Play took over.
     */
    fun startTrialViaPlayOrLocally(activity: android.app.Activity?): Boolean {
        if (activity != null && billing.startPlayTrial(activity)) return true
        startTrial()
        return false
    }

    /** Hides an app from the drawer, or brings it back. */
    fun toggleHidden(key: String) = store.update { current ->
        val hidden = current.hiddenApps
        current.copy(hiddenApps = if (key in hidden) hidden - key else hidden + key)
    }

    fun setHiddenPin(pin: String) = store.update { it.copy(hiddenPin = pin.filter { c -> c.isDigit() }.take(6)) }

    fun backupText(): String = Backup.encode(settings.value, layout.value)

    /**
     * Puts a backup back. The trial and the purchase are deliberately taken from the phone you
     * are restoring onto, never from the file, so a backup can neither hand out Plus nor take it
     * away from someone who paid.
     */
    fun restoreBackup(text: String): Boolean {
        val (restored, items) = Backup.decode(text) ?: return false
        val current = settings.value
        store.restore(
            settings = restored.copy(
                plusTrialStart = current.plusTrialStart,
                plusPurchased = current.plusPurchased,
                onboarded = true,
            ),
            items = items,
        )
        upgradeLayout()
        viewModelScope.launch { icons.configure(restored.iconPack, restored.iconShape) }
        return true
    }

    override fun onCleared() {
        wallpaperSource.stopWatching()
        repository.stopObserving()
        super.onCleared()
    }

    fun refreshWallpaper() = viewModelScope.launch { wallpaperSource.refresh() }

    fun refreshApps() = viewModelScope.launch { repository.refresh() }

    // ---------------------------------------------------------------- onboarding

    /** The real work behind the setup screen: read the device, then compose a home screen. */
    suspend fun buildHome(
        name: String,
        paletteId: String,
        appearance: String,
        widgets: List<String>,
        density: GridDensity,
        columns: Int,
        rows: Int,
    ) {
        val installed = repository.refresh()
        val dock = repository.suggestedDock(minOf(4, columns))
        val items = LayoutBuilder.build(repository, installed, widgets, columns, rows, dock)
        store.update {
            it.copy(
                userName = name.trim(),
                paletteId = paletteId,
                appearance = appearance,
                gridDensity = density.id,
                columns = columns,
                rows = rows,
                dock = dock,
                // Choosing the search bar in setup switches on the bar above the dock.
                searchBar = Widgets.Search.type in widgets,
                // Built just now, so already under today's rules. Without this a rerun of setup
                // left the version behind, and the next launch upgraded the new layout as if it
                // were an old one — every widget four times too tall, every app below the page.
                layoutVersion = LayoutTidy.VERSION,
            )
        }
        store.setLayout(items)
        wallpaperSource.refresh()
    }

    /** Set when setup has just finished, so the home screen's first appearance gets its entrance. */
    private var homeEntrancePending = false

    fun consumeHomeEntrance(): Boolean = homeEntrancePending.also { homeEntrancePending = false }

    /**
     * Set when setup grew its preview into the home screen: for that first moment the page has
     * to be laid out exactly like the preview, so anything extra waits until the hand-off is over.
     */
    private var setupHandoffPending = false

    fun consumeSetupHandoff(): Boolean = setupHandoffPending.also { setupHandoffPending = false }

    /**
     * Setup is done. [entrance] plays the home screen's own arrival; setup passes false when it
     * has already grown its preview into the home screen, so the two do not play on top of each other.
     */
    fun markOnboarded(entrance: Boolean = true) {
        homeEntrancePending = entrance
        setupHandoffPending = !entrance
        store.update { it.copy(onboarded = true) }
    }

    fun resetEverything() {
        store.reset()
        viewModelScope.launch { icons.configure("", "squircle") }
    }

    // ---------------------------------------------------------------- settings

    fun updateSettings(transform: (ZenithSettings) -> ZenithSettings) {
        val before = settings.value
        store.update(transform)
        val after = settings.value
        if (after.iconPack != before.iconPack || after.iconShape != before.iconShape) {
            viewModelScope.launch { icons.configure(after.iconPack, after.iconShape) }
        }
        val gridChanged = after.columns != before.columns || after.rows != before.rows
        // Leaving free placement puts everything back on the grid in one go, instead of leaving
        // half-cell offsets and odd sizes behind for the next drag to trip over.
        val leftFreePlacement = before.freeform && !after.freeform
        if (gridChanged || leftFreePlacement) {
            val settled = LayoutBuilder.settle(layout.value, after.columns, after.rows, free = after.freeform)
            if (settled != layout.value) store.setLayout(settled)
        }
    }

    /**
     * Re-derives the grid for the screen we are actually on. Called whenever the window changes
     * size — a fold opening, a rotation, a tablet — so cells keep the size the user asked for
     * instead of stretching to fill whatever glass they are on.
     */
    fun resolveGrid(density: GridDensity, widthDp: Float, heightDp: Float) {
        val (columns, rows) = density.gridFor(widthDp, heightDp)
        val current = settings.value
        if (columns == current.columns && rows == current.rows) return
        updateSettings { it.copy(columns = columns, rows = rows) }
    }

    fun setNote(text: String) = store.update { it.copy(note = text) }

    // ---------------------------------------------------------------- launching

    /** Opens an app. [sourceBounds] and [options] let its window grow out of the icon that was touched. */
    fun open(key: String, sourceBounds: android.graphics.Rect? = null, options: android.os.Bundle? = null) {
        if (!repository.launch(key, sourceBounds, options)) {
            refreshApps()
            return
        }
        // Remembered for the library's Suggestions. Kept short: only the latest dozen matter.
        store.update { it.copy(recentLaunches = (listOf(key) + (it.recentLaunches - key)).take(12)) }
    }

    fun openAppInfo(key: String) = repository.openAppInfo(key)

    fun uninstall(key: String) = repository.uninstall(key)

    // ---------------------------------------------------------------- home editing

    /** Whole rows, unless Plus free placement is in effect. */
    private fun rowStep(): Int = if (settings.value.freeform && plus.value.active) 1 else ROW_UNITS

    /** The page as the drop planner sees it; [colWidthInRowUnits] is measured from the grid on screen. */
    fun dropPage(colWidthInRowUnits: Float): DropPlanner.Page =
        DropPlanner.pageFor(settings.value, settings.value.freeform && plus.value.active, colWidthInRowUnits)

    /**
     * Lets item [id] go on [page] as [plan] says: it takes the plan's spot, and everything the plan
     * moves aside moves with it, all in one change, so nothing is ever seen half moved. [dx] and
     * [dy] are a free-placement offset to keep within the spot.
     */
    fun applyDrop(id: String, page: Int, plan: DropPlanner.Plan, dx: Float = 0f, dy: Float = 0f) =
        store.mutateLayout { items ->
            items.map { item ->
                val aside = plan.moves[item.id]
                when {
                    item.id == id -> item.moved(page, plan.target.col, plan.target.row).let {
                        if (dx != 0f || dy != 0f) it.nudged(dx, dy) else it
                    }
                    aside != null && item.page == page -> item.moved(page, aside.col, aside.row)
                    else -> item
                }
            }
        }

    fun moveItem(id: String, page: Int, col: Int, row: Int) = store.mutateLayout { items ->
        items.map { if (it.id == id) it.moved(page, col, row) else it }
    }

    fun removeItem(id: String) = store.mutateLayout { items -> items.filterNot { it.id == id } }

    /** Puts an app in the first opening anywhere, and says which page that turned out to be. */
    fun addAppToFirstFreeSlot(key: String): Int? {
        val parts = key.split('/', limit = 2)
        if (parts.size != 2) return null
        val slot = firstFreeSlot(1, ROW_UNITS, rowStep()) ?: return null
        store.mutateLayout { items ->
            items + AppCell(LayoutBuilder.newId(), slot.first, slot.second, slot.third, parts[0], parts[1])
        }
        return slot.first
    }

    /** Adds a widget, returning the page it landed on, or null when nothing would fit. */
    fun addWidget(type: String): Int? {
        val spec = Widgets.byType(type) ?: return null
        val s = settings.value
        val (spanX, spanY) = Widgets.defaultSpan(spec, s.columns, s.rows)
        val slot = firstFreeSlot(spanX, spanY, rowStep()) ?: return null
        store.mutateLayout { items ->
            items + WidgetCell(LayoutBuilder.newId(), slot.first, slot.second, slot.third, spec.type, spanX, spanY)
        }
        return slot.first
    }

    /** Resizes a widget where it stands, in whole rows unless free placement is on, refusing sizes that would overlap or overflow. */
    fun resizeWidget(id: String, spanX: Int, spanY: Int): Boolean {
        val widget = layout.value.firstOrNull { it.id == id } as? WidgetCell ?: return false
        val spec = Widgets.byType(widget.type) ?: return false
        val s = settings.value
        // Free placement drops each widget's designed range: anything from one unit up to the
        // whole page. Staying clear of other items still applies.
        val (targetX, targetY) = if (s.freeform && plus.value.active) {
            spanX.coerceIn(1, s.columns) to spanY.coerceIn(1, s.rows * ROW_UNITS)
        } else {
            Widgets.clampSpan(spec, spanX, LayoutBuilder.wholeRows(spanY), s.columns, s.rows)
        }
        if (targetX == widget.spanX && targetY == widget.spanY) return false
        if (!isSlotFree(widget.page, widget.col, widget.row, targetX, targetY, id)) return false
        store.mutateLayout { items ->
            items.map { if (it.id == id) widget.copy(spanX = targetX, spanY = targetY) else it }
        }
        return true
    }

    /**
     * Resizes an app icon where it stands, for the resize pointer: [spanX] columns wide and [rows]
     * whole rows tall, anything up to the whole page. 1 by 1 is an ordinary icon; anything bigger is
     * a super icon, stretched to fill it. It never moves the icon — it is under your finger — so a
     * size with no room is refused, and false comes back.
     */
    fun resizeApp(id: String, spanX: Int, rows: Int): Boolean {
        val app = layout.value.firstOrNull { it.id == id } as? AppCell ?: return false
        val s = settings.value
        val sx = spanX.coerceIn(1, s.columns)
        val sy = rows.coerceIn(1, s.rows) * ROW_UNITS
        if (sx == app.spanX && sy == app.spanY) return false
        if (!isSlotFree(app.page, app.col, app.row, sx, sy, id)) return false
        store.mutateLayout { items -> items.map { if (it.id == id) app.copy(spanX = sx, spanY = sy) else it } }
        return true
    }

    // ---------------------------------------------------------------- pages

    /** How many pages the pager should show: the ones in use, or more if you asked for them. */
    fun pageCount(): Int {
        val used = (layout.value.maxOfOrNull { it.page } ?: 0) + 1
        return maxOf(used, settings.value.pages).coerceIn(1, LayoutBuilder.MAX_PAGES)
    }

    /**
     * Adds an empty page at the end and returns its index, or null when there is no room — which
     * includes the case where the last page is already empty, so nobody ends up with a run of
     * blank pages by leaning on the edge of the screen.
     */
    fun addPage(): Int? {
        val next = pageCount()
        if (next >= LayoutBuilder.MAX_PAGES) return null
        if (next > 1 && layout.value.none { it.page == next - 1 }) return null
        store.update { it.copy(pages = next + 1) }
        return next
    }

    /** The first opening on one particular page, for an item that has been carried onto it. */
    fun freeSlotOnPage(page: Int, spanX: Int, spanY: Int, rowStep: Int, ignoreId: String?): Pair<Int, Int>? {
        val s = settings.value
        val grid = PageGrid(s.columns, s.rows * ROW_UNITS)
        layout.value.filter { it.page == page && it.id != ignoreId }
            .forEach { grid.occupyItem(it) }
        return grid.findSlot(spanX, spanY, rowStep)
    }

    /**
     * Forgets trailing pages nobody put anything on, so an empty page you swiped away from does
     * not linger for ever. [keep] is the page you are looking at, which is never taken away
     * underneath you.
     */
    fun trimPages(keep: Int) {
        val used = (layout.value.maxOfOrNull { it.page } ?: 0) + 1
        val target = maxOf(1, used, keep + 1)
        if (target < settings.value.pages) store.update { it.copy(pages = target) }
    }

    /**
     * The first opening on [startPage] or any page after it — a brand new page if every one is
     * full. For a drop onto a full page: the item goes on to the next page with room rather than
     * quietly returning to where it came from.
     */
    fun firstFreeSlotFrom(startPage: Int, spanX: Int, spanY: Int, rowStep: Int, ignoreId: String?): Triple<Int, Int, Int>? {
        val s = settings.value
        val items = layout.value.filter { it.id != ignoreId }
        val lastPage = maxOf(items.maxOfOrNull { it.page } ?: 0, startPage)
        for (page in startPage..minOf(lastPage + 1, LayoutBuilder.MAX_PAGES - 1)) {
            val grid = PageGrid(s.columns, s.rows * ROW_UNITS)
            items.filter { it.page == page }.forEach { grid.occupyItem(it) }
            val slot = grid.findSlot(spanX, spanY, rowStep) ?: continue
            return Triple(page, slot.first, slot.second)
        }
        return null
    }

    /** Returns page/col/row of the first opening big enough for the given span, in row units. */
    fun firstFreeSlot(spanX: Int, spanY: Int, rowStep: Int = 1): Triple<Int, Int, Int>? {
        val s = settings.value
        val items = layout.value
        val lastPage = (items.maxOfOrNull { it.page } ?: 0)
        for (page in 0..(lastPage + 1)) {
            val grid = PageGrid(s.columns, s.rows * ROW_UNITS)
            items.filter { it.page == page }.forEach { grid.occupyItem(it) }
            val slot = grid.findSlot(spanX, spanY, rowStep)
            if (slot != null) return Triple(page, slot.first, slot.second)
        }
        return null
    }

    fun isSlotFree(page: Int, col: Int, row: Int, spanX: Int, spanY: Int, ignoreId: String?): Boolean {
        val s = settings.value
        val rows = s.rows * ROW_UNITS
        if (col < 0 || row < 0 || col + spanX > s.columns || row + spanY > rows) return false
        val grid = PageGrid(s.columns, rows)
        layout.value.filter { it.page == page && it.id != ignoreId }
            .forEach { grid.occupyItem(it) }
        return grid.isFree(col, row, spanX, spanY)
    }

    /** Drop one app onto another to make a folder; drop onto a folder to add to it. */
    fun combine(draggedId: String, targetId: String) = store.mutateLayout { items ->
        val dragged = items.firstOrNull { it.id == draggedId } ?: return@mutateLayout items
        val target = items.firstOrNull { it.id == targetId } ?: return@mutateLayout items
        when {
            target is FolderCell && dragged is AppCell ->
                items.filterNot { it.id == draggedId }
                    .map { if (it.id == targetId) target.copy(keys = (target.keys + dragged.key).distinct()) else it }

            target is AppCell && dragged is AppCell ->
                items.filterNot { it.id == draggedId || it.id == targetId } + FolderCell(
                    id = LayoutBuilder.newId(),
                    page = target.page, col = target.col, row = target.row,
                    title = "Folder",
                    keys = listOf(target.key, dragged.key),
                )

            else -> items
        }
    }

    fun renameFolder(id: String, title: String) = store.mutateLayout { items ->
        items.map { if (it.id == id && it is FolderCell) it.copy(title = title) else it }
    }

    /**
     * Takes one app out of a folder and puts it back on the page, rather than throwing it away.
     * A folder left holding one app becomes that app again; an empty one disappears.
     */
    fun removeFromFolder(folderId: String, key: String) = store.mutateLayout { items ->
        val folder = items.firstOrNull { it.id == folderId } as? FolderCell ?: return@mutateLayout items
        val keys = folder.keys.filterNot { it == key }
        val s = settings.value

        // Give the app that just came out a cell of its own on the same page.
        fun rehome(base: List<HomeItem>): List<HomeItem> {
            val parts = key.split('/', limit = 2)
            if (parts.size != 2) return base
            val grid = PageGrid(s.columns, s.rows * ROW_UNITS)
            base.filter { it.page == folder.page }
                .forEach { grid.occupyItem(it) }
            val slot = grid.findSlot(1, ROW_UNITS, rowStep()) ?: return base
            return base + AppCell(
                LayoutBuilder.newId(), folder.page, slot.first, slot.second, parts[0], parts[1],
            )
        }

        when {
            keys.isEmpty() -> rehome(items.filterNot { it.id == folderId })

            keys.size == 1 -> {
                val parts = keys.first().split('/', limit = 2)
                if (parts.size != 2) items else rehome(
                    items.map {
                        if (it.id == folderId) {
                            AppCell(folder.id, folder.page, folder.col, folder.row, parts[0], parts[1])
                        } else {
                            it
                        }
                    }
                )
            }

            else -> rehome(items.map { if (it.id == folderId) folder.copy(keys = keys) else it })
        }
    }

    // ---------------------------------------------------------------- dock

    /** How many apps the dock takes: one for each column of the page, so each can stand under one. */
    private fun dockLimit(): Int = settings.value.columns

    /** Adds [key] to the dock or takes it off. False when it was to be added and the dock is full. */
    fun toggleDock(key: String): Boolean {
        val dock = settings.value.dock
        if (key !in dock && dock.size >= dockLimit()) return false
        store.update { it.copy(dock = if (key in it.dock) it.dock - key else it.dock + key) }
        return true
    }

    fun setDock(keys: List<String>) = store.update { it.copy(dock = keys.take(dockLimit())) }

    /**
     * Takes an app off the home screen and slots it into the dock at [index]. False when the dock
     * is already full, in which case the app stays exactly where it was.
     */
    fun moveToDock(itemId: String, index: Int): Boolean {
        val cell = layout.value.firstOrNull { it.id == itemId } as? AppCell ?: return false
        val dock = settings.value.dock
        if (cell.key !in dock) {
            if (dock.size >= dockLimit()) return false
            store.update { current ->
                current.copy(dock = current.dock.toMutableList().apply { add(index.coerceIn(0, size), cell.key) })
            }
        }
        removeItem(itemId)
        return true
    }

    /** Moves a dock icon from one position to another. */
    fun moveInDock(from: Int, to: Int) = store.update { current ->
        if (from !in current.dock.indices) return@update current
        val dock = current.dock.toMutableList()
        val key = dock.removeAt(from)
        dock.add(to.coerceIn(0, dock.size), key)
        current.copy(dock = dock)
    }

    /**
     * Lifts an app out of the dock onto [page], where it was let go — its cell's corner at
     * [exactCol], [exactRow] — with whatever is there stepping aside, just as on the home screen.
     * Where nothing can make room it takes the nearest opening on the page. On a page with no room
     * at all it stays in the dock, as it would on a phone, and null comes back; otherwise the id of
     * the icon now on the page.
     */
    fun moveDockToHome(key: String, page: Int, exactCol: Float, exactRow: Float, colWidthInRowUnits: Float): String? {
        val parts = key.split('/', limit = 2)
        if (parts.size != 2) return null
        val arriving = AppCell(LayoutBuilder.newId(), page, 0, 0, parts[0], parts[1])
        val onPage = layout.value.filter { it.page == page }
        val grid = dropPage(colWidthInRowUnits)
        val plan = DropPlanner.plan(onPage, arriving, exactCol, exactRow, grid)
            ?: DropPlanner.opening(onPage, arriving, exactCol, exactRow, grid)?.let { DropPlanner.Plan(it, exact = false) }
            ?: return null
        store.update { it.copy(dock = it.dock - key) }
        store.mutateLayout { items ->
            items.map { item ->
                val aside = plan.moves[item.id]
                if (aside != null && item.page == page) item.moved(page, aside.col, aside.row) else item
            } + arriving.moved(page, plan.target.col, plan.target.row)
        }
        return arriving.id
    }
}
