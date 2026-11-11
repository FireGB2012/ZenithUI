package com.gabriel.zenithui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gabriel.zenithui.data.AppCell
import com.gabriel.zenithui.data.AppEdit
import com.gabriel.zenithui.data.AppEdits
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.AppRepository
import com.gabriel.zenithui.data.AppShortcut
import com.gabriel.zenithui.data.IconSource
import com.gabriel.zenithui.data.HostedWidgetCell
import com.gabriel.zenithui.data.StackCell
import com.gabriel.zenithui.data.StackMember
import com.gabriel.zenithui.data.StackRules
import com.gabriel.zenithui.data.ShortcutCell
import kotlinx.coroutines.flow.receiveAsFlow
import com.gabriel.zenithui.data.ShortcutKeys
import com.gabriel.zenithui.data.Aura
import com.gabriel.zenithui.data.CheckIn
import com.gabriel.zenithui.data.Backup
import com.gabriel.zenithui.data.BillingRepository
import com.gabriel.zenithui.data.DropPlanner
import com.gabriel.zenithui.data.FolderCell
import com.gabriel.zenithui.data.GridDensity
import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.withId
import com.gabriel.zenithui.data.IconLoader
import com.gabriel.zenithui.data.LayoutBuilder
import com.gabriel.zenithui.data.LayoutTidy
import com.gabriel.zenithui.data.NewLook
import com.gabriel.zenithui.data.PageGrid
import com.gabriel.zenithui.data.occupyItem
import com.gabriel.zenithui.data.Plus
import com.gabriel.zenithui.data.PlusStatus
import com.gabriel.zenithui.data.PriceAmounts
import com.gabriel.zenithui.data.SubscriptionPlan
import com.gabriel.zenithui.data.ROW_UNITS
import com.gabriel.zenithui.data.Reshape
import com.gabriel.zenithui.data.SettingsStore
import com.gabriel.zenithui.data.WidgetCell
import com.gabriel.zenithui.data.Widgets
import com.gabriel.zenithui.data.WallStyle
import com.gabriel.zenithui.data.WallpaperSource
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.data.trialDaysLeft
import com.gabriel.zenithui.data.server.Account
import com.gabriel.zenithui.data.server.AccountRules
import com.gabriel.zenithui.data.server.AccountStore
import com.gabriel.zenithui.data.server.Reply
import com.gabriel.zenithui.data.server.ServerConfig
import com.gabriel.zenithui.data.server.Signed
import com.gabriel.zenithui.data.server.SignedIn
import com.gabriel.zenithui.data.server.ZenithServer
import com.gabriel.zenithui.data.server.problem
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ZenithViewModel(application: Application) : AndroidViewModel(application) {

    val store = SettingsStore.get(application)
    val repository = AppRepository(application)
    val icons = IconLoader(application, repository)
    private val wallpaperSource = WallpaperSource(application)
    val billing = BillingRepository(application)

    /** Where other apps' widgets are drawn from. Here, before init, which tidies it. */
    val widgetHost = com.gabriel.zenithui.data.ZenithWidgetHost.get(application)

    // Declared before init, which tidies widget numbers and must know what Undo still holds.
    private val _removed = MutableStateFlow<List<HomeItem>>(emptyList())

    /** What was just taken off the home screen, while it can still be put back: see [undoRemove]. */
    val removed: StateFlow<List<HomeItem>> = _removed.asStateFlow()

    /** Lets go, once Undo's time is up, of what the last removal held. */
    private var letGo: kotlinx.coroutines.Job? = null
    private val widgetManager = android.appwidget.AppWidgetManager.getInstance(application)

    /** Zenith's server, if this build has one; a test build may reach one on the test PC. */
    private val server = ZenithServer(ServerConfig.url, allowPlainHttp = BuildConfig.DEBUG)
    private val accounts = AccountStore(application)

    /** This phone's sign-in to a Zenith account, or null. Always null in a build with no server. */
    val account: StateFlow<Signed?> = accounts.signed

    val settings: StateFlow<ZenithSettings> = store.settings
    val layout: StateFlow<List<HomeItem>> = store.layout

    /** Every app, with the names you gave any of them, in order by those names. */
    val apps: StateFlow<List<AppInfo>> = combine(
        repository.apps,
        store.settings.map { it.appEdits }.distinctUntilChanged(),
    ) { list, edits -> AppEdits.relabel(list, edits) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppEdits.relabel(repository.apps.value, store.settings.value.appEdits))
    /** The wallpaper as the glass shows it, or null when Zenith has no copy of it. */
    val glassLevels = wallpaperSource.levels

    private val director = WallpaperDirector(application, viewModelScope, store.settings, wallpaperSource) {
        store.update { it.copy(wallpaperStyle = "") }
    }

    /** A wallpaper fading in over the old one, for the home screen to draw; null when there is none. */
    val wallpaperIncoming: StateFlow<Incoming?> = director.incoming

    /** Android would not take a wallpaper. */
    val wallpaperRefused = director.refused

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
        billing.quarterlyPrice,
        billing.monthlyOffered,
        billing.quarterlyOffered,
        billing.amounts,
    ) { values ->
        val settings = values[0] as ZenithSettings
        val purchased = values[1] as Boolean
        val subscribed = values[2] as Boolean
        val available = values[3] as Boolean
        val trialOffer = values[4] as Boolean
        val monthly = values[5] as String
        val lifetimeCost = values[6] as String
        val quarterly = values[7] as String
        val monthlyOffered = values[8] as Boolean
        val quarterlyOffered = values[9] as Boolean
        val amounts = values[10] as PriceAmounts
        PlusStatus(
            purchased = purchased || settings.plusPurchased,
            lifetime = settings.plusPurchased,
            subscribed = subscribed,
            trialDaysLeft = trialDaysLeft(settings.plusTrialStart),
            trialAvailable = settings.plusTrialStart == 0L,
            monthlyPrice = monthly,
            quarterlyPrice = quarterly,
            lifetimePrice = lifetimeCost,
            monthlyOffered = monthlyOffered,
            quarterlyOffered = quarterlyOffered,
            amounts = amounts,
            purchasable = available,
            playTrialOffer = trialOffer,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PlusStatus())

    private val _homePressed = MutableStateFlow(0)

    /** Incremented every time the home button is pressed, from an app or from Zenith itself. */
    val homePressed: StateFlow<Int> = _homePressed.asStateFlow()

    private val _homeAgain = MutableStateFlow(0)

    /** Incremented, with [homePressed], only when home is pressed while Zenith was already in front. */
    val homeAgain: StateFlow<Int> = _homeAgain.asStateFlow()

    fun onHomePressed(alreadyHome: Boolean) {
        if (alreadyHome) _homeAgain.value += 1
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
        icons.setChosen(AppEdits.icons(settings.value.appEdits))
        viewModelScope.launch {
            settings.map { AppEdits.icons(it.appEdits) }.distinctUntilChanged().collect { icons.setChosen(it) }
        }
        viewModelScope.launch { repository.refresh() }
        viewModelScope.launch { repository.refreshWork() }
        viewModelScope.launch { wallpaperSource.refresh() }
        wallpaperSource.watch {
            viewModelScope.launch { wallpaperSource.refresh() }
            director.wallpaperChanged()
            // With no copy of it to read, Android's reading of the new wallpaper's colours.
            if (wallpaperSource.levels.value == null) readWallpaperPalette(null)
        }
        viewModelScope.launch { wallpaperSource.levels.collect { readWallpaperPalette(it) } }
        // A design that followed the theme keeps the colours it was painted in: settings from
        // before the themes went have no colour set for it, so it is given paletteId's, once and
        // before anything is painted. Those are the colours it already has, so nothing repaints.
        if (settings.value.wallpaperPalette.isEmpty()) store.update { it.copy(wallpaperPalette = it.paletteId) }
        director.start()
        viewModelScope.launch {
            val current = settings.value
            icons.configure(current.iconPack, current.iconShape)
        }
        // The app list arrives asynchronously; once it does, every icon asks again.
        viewModelScope.launch {
            repository.apps.collect { icons.invalidate() }
        }
        repository.observePackages {
            viewModelScope.launch { repository.refresh() }
            viewModelScope.launch { repository.refreshWork() }
        }
        tidyWidgetIds()
        // Photos left over from a home screen another Look was put on over, kept by nothing now.
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { icons.tidyPhotos(photosInUse()) }
        // A purchase is written down the first time Play mentions it, and never unwritten.
        viewModelScope.launch {
            billing.permanent.collect { bought ->
                if (bought && !settings.value.plusPurchased) store.update { it.copy(plusPurchased = true) }
            }
        }
        // Free placement left switched on by someone whose Plus has since ended is not in effect,
        // so their layout goes back onto whole rows. Only once Play itself has said nothing is
        // bought: a subscriber who is merely offline must never have an arrangement undone.
        viewModelScope.launch {
            billing.answered.collect { answered ->
                if (!answered) return@collect
                val current = settings.value
                val active = billing.purchased.value || current.plusPurchased || trialDaysLeft(current.plusTrialStart) > 0
                if (active) return@collect
                val tidied = LayoutTidy.afterPlusEnded(current, layout.value)
                if (tidied != layout.value) store.setLayout(tidied)
            }
        }
        billing.refresh()
    }

    /**
     * Brings whatever layout is stored up to today's rules (see [LayoutTidy.bringUpToDate]) and
     * settles it so nothing overlaps. Safe to run on a layout that already follows them, which is
     * why a restored backup goes through it too.
     */
    private fun upgradeLayout() {
        // Rescaled into row units, onto whole rows, the search bar a setting rather than a widget:
        // all of it happens once, and a layout that is already up to date is not touched.
        if (settings.value.layoutVersion < LayoutTidy.VERSION) {
            val (next, items) = LayoutTidy.bringUpToDate(settings.value, layout.value)
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

    /** The subscription, on either of its plans: a month at a time, or three months at a time. */
    fun subscribe(activity: android.app.Activity, plan: SubscriptionPlan): Boolean = billing.subscribe(activity, plan)

    /**
     * Begins the trial the best way this phone can. When Play has the free-trial subscription it
     * shows its own sheet - the "7 days free, then the monthly price" one - and the week is
     * Google's to count. Until that product exists, the week is counted here instead, which is
     * what happens today. Returns true if Play took over.
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

    fun backupText(): String = Backup.encode(settings.value, layout.value, store.looks.value)

    /**
     * Puts a backup back. The trial and the purchase are deliberately taken from the phone you
     * are restoring onto, never from the file, so a backup can neither hand out Plus nor take it
     * away from someone who paid. A backup [fromAccount] was sent without the hidden-apps PIN, so
     * the one on this phone stays.
     */
    fun restoreBackup(text: String, fromAccount: Boolean = false): Boolean {
        val (restored, items) = Backup.decode(text) ?: return false
        finishRemoval()
        val current = settings.value
        // The backup's Looks join the ones saved here; one in both is the backup's.
        val incoming = Backup.looks(text)
        if (incoming.isNotEmpty()) {
            val ids = incoming.map { it.id }.toSet()
            store.setLooks((incoming + store.looks.value.filterNot { it.id in ids }).take(com.gabriel.zenithui.data.Looks.MOST))
        }
        store.restore(
            settings = restored.copy(
                plusTrialStart = current.plusTrialStart,
                plusPurchased = current.plusPurchased,
                hiddenPin = if (fromAccount) current.hiddenPin else restored.hiddenPin,
                // A photo chosen as an icon on another phone is not on this one: that app has its own again.
                appEdits = restored.appEdits
                    .mapValues { (_, edit) ->
                        val lost = IconSource.parse(edit.icon) is IconSource.Photo && !icons.hasPhoto(edit.icon)
                        if (lost) edit.copy(icon = "") else edit
                    }
                    .filterValues { !it.isEmpty },
                onboarded = true,
            ),
            items = items,
        )
        upgradeLayout()
        tidyWidgetIds()
        viewModelScope.launch { icons.configure(restored.iconPack, restored.iconShape) }
        return true
    }

    // ---------------------------------------------------------------- the account
    //
    // Each of these asks the server for something and answers with why it did not happen, in
    // words for the person, or with null when it did. None of them is ever called in a build
    // with no server, and none of them sends anything until somebody makes an account or signs in.
    //
    // They are seen through even if the page that asked is closed half way: an account the
    // server has made, or a code it has counted, must not be lost on its way back to the phone.
    // And they take turns, so that what the server says is taken in the order it was said.

    private val accountTurn = Mutex()

    private suspend fun <T> seenThrough(work: suspend () -> T): T =
        viewModelScope.async { accountTurn.withLock { work() } }.await()

    suspend fun createAccount(username: String, password: String): String? = seenThrough {
        if (accounts.signed.value != null) return@seenThrough null
        ensureReferralCode()
        entered(server.createAccount(username.trim().lowercase(), password, settings.value.referralCode), email = null)
    }

    suspend fun signIn(username: String, password: String): String? = seenThrough {
        if (accounts.signed.value != null) return@seenThrough null
        entered(server.signIn(username.trim().lowercase(), password), email = null)
    }

    /** [idToken] is what Google handed the phone; [email] is only for showing who is signed in. */
    suspend fun signInWithGoogle(idToken: String, email: String?): String? = seenThrough {
        if (accounts.signed.value != null) return@seenThrough null
        ensureReferralCode()
        entered(server.signInWithGoogle(idToken, settings.value.referralCode), email)
    }

    private fun entered(reply: Reply<SignedIn>, email: String?): String? {
        if (reply !is Reply.Ok) return reply.problem()
        accounts.signIn(reply.value.token, reply.value.account, email)
        settle(reply.value.account)
        accountLooked = SystemClock.elapsedRealtime()
        return null
    }

    /** Brings this phone into line with the account: see [AccountRules.settle]. */
    private fun settle(account: Account) = store.update { AccountRules.settle(it, account) }

    private var accountLooked = 0L
    private var accountLooking = false

    /**
     * Asks the server how the account stands, if it has not been asked in the last few minutes
     * (or [now] is true). Called when the home screen comes back: it is how a friend using your
     * code reaches your phone.
     */
    fun refreshAccount(now: Boolean = false) {
        if (accounts.signed.value == null || accountLooking) return
        val moment = SystemClock.elapsedRealtime()
        if (!now && accountLooked != 0L && moment - accountLooked < ACCOUNT_LOOK_EVERY_MS) return
        accountLooked = moment
        accountLooking = true
        viewModelScope.launch {
            try {
                accountTurn.withLock {
                    // Whoever is signed in by the time it is this one's turn.
                    val signed = accounts.signed.value ?: return@withLock
                    when (val reply = server.me(signed.token)) {
                        is Reply.Ok -> {
                            accounts.update(reply.value)
                            settle(reply.value)
                        }
                        // The account was deleted somewhere else, or this sign-in was left unused too long.
                        is Reply.Refused -> if (reply.signedOut) accounts.signOut()
                        // Out of reach: look again in a minute, not in ten.
                        is Reply.Unreachable -> accountLooked = moment - ACCOUNT_LOOK_EVERY_MS + ACCOUNT_LOOK_AGAIN_MS
                    }
                }
            } finally {
                accountLooking = false
            }
        }
    }

    /** Signs this phone out. What is on the phone stays; so does the account. */
    suspend fun signOut(): Unit = seenThrough {
        val signed = accounts.signed.value ?: return@seenThrough
        accounts.signOut()
        // Told so that the key stops working there too; if it cannot be reached, it is forgotten here all the same.
        server.signOut(signed.token)
    }

    /**
     * Deletes the account from the server, with its code and its backup. Nothing on the phone is
     * touched. Null when it is gone.
     */
    suspend fun deleteAccount(password: String?): String? = seenThrough {
        val signed = accounts.signed.value ?: return@seenThrough null
        when (val reply = server.deleteAccount(signed.token, password)) {
            is Reply.Ok -> {
                accounts.signOut()
                null
            }
            is Reply.Refused -> if (reply.signedOut) {
                // The server no longer knows this sign-in, so it deleted nothing: say so, rather
                // than leave somebody believing an account is gone that is still there.
                accounts.signOut()
                "You had been signed out, so nothing was deleted. Sign in again to delete the account."
            } else {
                reply.problem()
            }
            is Reply.Unreachable -> reply.problem()
        }
    }

    /**
     * Sends the home screen to the account, in place of whatever backup was there. The hidden-apps
     * PIN and the purchase stay on the phone: see [AccountRules.forAccount].
     */
    suspend fun backUpToAccount(): String? = seenThrough {
        val signed = accounts.signed.value ?: return@seenThrough SIGN_IN_FIRST
        when (val reply = server.saveBackup(signed.token, Backup.encode(AccountRules.forAccount(settings.value), layout.value, store.looks.value))) {
            is Reply.Ok -> {
                accounts.update(signed.account.copy(backupAt = reply.value))
                null
            }
            else -> lost(reply)
        }
    }

    /** Fetches the account's backup and puts it back, as [restoreBackup] does a file. */
    suspend fun restoreFromAccount(): String? = seenThrough {
        val signed = accounts.signed.value ?: return@seenThrough SIGN_IN_FIRST
        when (val reply = server.loadBackup(signed.token)) {
            is Reply.Ok -> if (restoreBackup(reply.value.text, fromAccount = true)) {
                // The backup carries the code and the counts it was made with; the account's are the ones that hold.
                settle(signed.account)
                null
            } else {
                "The backup in your account could not be read."
            }
            else -> lost(reply)
        }
    }

    /**
     * Uses a friend's code. With a server it is the server that says whether the code is real and
     * counts it for the friend; without one it is the old rule, where any well-made code will do.
     */
    suspend fun redeemAuraCode(code: String): String? {
        if (!server.isSetUp) return redeemReferral(code)
        return seenThrough {
            val cleaned = Aura.cleanCode(code)
            val signed = accounts.signed.value
            when {
                settings.value.referredBy.isNotEmpty() -> "You have already used a code."
                !Aura.isWellFormedCode(cleaned) -> "That is not a Zenith code."
                signed == null -> "Sign in to your Zenith account to use a code."
                else -> when (val reply = server.redeem(signed.token, cleaned)) {
                    is Reply.Ok -> {
                        accounts.update(reply.value)
                        store.update { it.copy(referredBy = cleaned, aura = it.aura + Aura.REFERRAL_REWARD) }
                        null
                    }
                    else -> lost(reply)
                }
            }
        }
    }

    /** A reply that is not a yes, in words; and if it says this phone is no longer signed in, it is signed out. */
    private fun lost(reply: Reply<*>): String? {
        if (reply is Reply.Refused && reply.signedOut) accounts.signOut()
        return reply.problem()
    }

    // ---------------------------------------------------------------- Looks

    /** The Looks saved on this phone, newest first. */
    val looks: StateFlow<List<com.gabriel.zenithui.data.Look>> = store.looks

    private val _lookPut = kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.CONFLATED)

    /** A Look has just been put on: the home screen arrives again, as after an unlock. */
    val lookPut = _lookPut.receiveAsFlow()

    private val _lookDue = MutableStateFlow<com.gabriel.zenithui.data.Look?>(null)

    /** A Look the schedule wants on, waiting for a moment when nothing is under your finger. */
    val lookDue: StateFlow<com.gabriel.zenithui.data.Look?> = _lookDue.asStateFlow()

    /** Saves the home screen as it is now as a Look called [name], first in the list. */
    fun saveLook(name: String): com.gabriel.zenithui.data.Look {
        val look = com.gabriel.zenithui.data.Looks.capture(settings.value, layout.value, name, LayoutBuilder.newId(), System.currentTimeMillis())
        store.setLooks((listOf(look) + looks.value).take(com.gabriel.zenithui.data.Looks.MOST))
        store.update { it.copy(activeLook = look.id) }
        return look
    }

    /** Saves the home screen as it is now over the Look [id], keeping its name, its place and its code. */
    fun updateLook(id: String) {
        store.setLooks(looks.value.map { old ->
            if (old.id != id) old
            else com.gabriel.zenithui.data.Looks.capture(settings.value, layout.value, old.name, old.id, System.currentTimeMillis())
                .copy(shared = old.shared, sharedAt = old.sharedAt)
        })
        store.update { it.copy(activeLook = id) }
    }

    fun renameLook(id: String, name: String) =
        store.setLooks(looks.value.map { if (it.id == id && name.isNotBlank()) it.copy(name = name.trim().take(30)) else it })

    fun deleteLook(id: String) {
        // A shared one stops being shared as well. Offline, its code works on until the account goes.
        val gone = looks.value.firstOrNull { it.id == id }
        val signed = accounts.signed.value
        if (gone != null && gone.shared.isNotEmpty() && signed != null) viewModelScope.launch { server.deleteLook(signed.token, gone.shared) }
        store.setLooks(looks.value.filterNot { it.id == id })
        store.update { it.copy(lookRules = it.lookRules.filterNot { rule -> rule.lookId == id }, activeLook = if (it.activeLook == id) "" else it.activeLook) }
        // What only that Look held goes with it: other apps' widgets, and photos chosen as icons.
        tidyWidgetIds()
        icons.tidyPhotos(photosInUse())
    }

    /** The last size the grid was worked out for, so a Look made on another screen is fitted to this one. */
    private var screenDp: Pair<Float, Float>? = null

    /**
     * Puts [look] on: what it keeps put into the settings, its layout on the page, its wallpaper
     * design painted again in its colours, and the home screen arriving afresh. A Look somebody
     * else made ([fromElsewhere]) is fitted to this phone first: apps not installed here are left
     * out, and a Plus wallpaper without Plus keeps the wallpaper there is.
     */
    fun applyLook(look: com.gabriel.zenithui.data.Look, fromElsewhere: Boolean = false) {
        finishRemoval()
        val current = settings.value
        var (next, items) = com.gabriel.zenithui.data.Looks.apply(current, look)
        if (fromElsewhere) {
            val installed = repository.apps.value.map { it.key }.toSet()
            val fitted = com.gabriel.zenithui.data.Looks.keepInstalled(next, items, installed)
            next = fitted.first
            items = fitted.second
        }
        val design = WallStyle.byName(next.wallpaperStyle)
        if (design != null && design.plus && !plus.value.active) {
            next = next.copy(wallpaperStyle = current.wallpaperStyle, wallpaperPalette = current.wallpaperPalette)
        }
        next = next.copy(
            activeLook = if (fromElsewhere) "" else look.id,
            onboarded = true,
            paletteOccasion = "",
            // A photo chosen as an icon that is not on this phone: that app has its own again.
            appEdits = next.appEdits
                .mapValues { (_, edit) -> if (IconSource.parse(edit.icon) is IconSource.Photo && !icons.hasPhoto(edit.icon)) edit.copy(icon = "") else edit }
                .filterValues { !it.isEmpty },
        )
        _lookDue.value = null
        store.restore(next, items)
        upgradeLayout()
        screenDp?.let { (w, h) -> resolveGrid(w, h) }
        tidyWidgetIds()
        viewModelScope.launch { icons.configure(next.iconPack, next.iconShape) }
        if (next.wallpaperStyle.isNotEmpty()) director.request()
        _lookPut.trySend(Unit)
    }

    /** The schedule's time for a Look has come: it goes on now if it is not on already. */
    private fun checkSchedule(now: Boolean) {
        if (!plus.value.active) return
        val clock = java.time.LocalDateTime.now()
        checkLooks(clock, now)
    }

    private fun checkLooks(clock: java.time.LocalDateTime, now: Boolean) {
        val s = settings.value
        if (s.lookRules.isEmpty()) return
        val (rule, occasion) = com.gabriel.zenithui.data.LookSchedule.due(s.lookRules, clock, s.lookOccasion) ?: return
        val look = looks.value.firstOrNull { it.id == rule.lookId } ?: return
        store.update { it.copy(lookOccasion = occasion) }
        if (s.activeLook == look.id) return
        if (now) applyLook(look) else _lookDue.value = look
    }

    private var scheduleJob: kotlinx.coroutines.Job? = null

    private fun followSchedule(showing: Boolean) {
        scheduleJob?.cancel()
        if (!showing) return
        // Coming home is a moment with nothing under the finger yet: what is due goes on at once.
        checkSchedule(now = true)
        scheduleJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(30_000)
                checkSchedule(now = false)
            }
        }
    }

    /** The home screen is idle: whatever the schedule was waiting to put on goes on. */
    fun applyDueLook() {
        _lookDue.value?.let { applyLook(it) }
    }

    fun setLookRules(rules: List<com.gabriel.zenithui.data.LookRule>) = store.update { it.copy(lookRules = rules, lookOccasion = "") }

    // ---------------------------------------------------------------- the glass studio

    fun setGlassTuning(tuning: com.gabriel.zenithui.data.GlassTuning) = store.update { it.copy(glassTuning = tuning) }

    /** One of Zenith's widgets, or a folder, in a colour of its own, as ARGB, or as it was again with 0. */
    fun setItemColour(id: String, colour: Long) = store.setLayout(
        layout.value.map {
            when {
                it.id != id -> it
                it is WidgetCell -> it.copy(accent = colour)
                it is FolderCell -> it.copy(colour = colour)
                else -> it
            }
        }
    )

    /**
     * Works out the colours in the wallpaper, which are the "wallpaper" colour set a design can be
     * painted in: from Zenith's copy of it, or, without one, from the colours Android reads in it.
     * Never for one of Zenith's own designs: those are painted from a colour set, and a set taken
     * from them would only chase its own tail.
     */
    private fun readWallpaperPalette(levels: com.gabriel.zenithui.data.GlassLevels?) {
        if (settings.value.wallpaperStyle.isNotEmpty()) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val fromCopy = levels?.let { copy ->
                runCatching {
                    val image = copy.frost
                    val pixels = IntArray(image.width * image.height)
                    image.readPixels(pixels)
                    com.gabriel.zenithui.data.WallpaperPalette.fromPixels(pixels)
                }.getOrNull()
            }
            val picked = fromCopy ?: runCatching {
                val colours = android.app.WallpaperManager.getInstance(getApplication<Application>())
                    .getWallpaperColors(android.app.WallpaperManager.FLAG_SYSTEM) ?: return@runCatching null
                com.gabriel.zenithui.data.WallpaperPalette.pick(
                    listOfNotNull(
                        colours.primaryColor.toArgb() to 0.6f,
                        colours.secondaryColor?.let { it.toArgb() to 0.25f },
                        colours.tertiaryColor?.let { it.toArgb() to 0.15f },
                    )
                )
            }.getOrNull() ?: return@launch
            fun long(argb: Int) = argb.toLong() and 0xFFFFFFFFL
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                val s = settings.value
                if (s.wallpaperStyle.isNotEmpty()) return@withContext
                if (s.wallAccent == long(picked.accent) && s.wallAccentAlt == long(picked.accentAlt) &&
                    s.wallBackdrop == long(picked.backdrop) && s.wallDark == picked.dark
                ) return@withContext
                store.update {
                    it.copy(wallAccent = long(picked.accent), wallAccentAlt = long(picked.accentAlt), wallBackdrop = long(picked.backdrop), wallDark = picked.dark)
                }
            }
        }
    }

    /**
     * Shares the Look [id] from the account, without what is personal in it. Answers with its
     * code, or why not. One shared already answers with the code it has; [again] takes that code
     * back and shares the Look as it is now under a new one.
     */
    suspend fun shareLook(id: String, again: Boolean = false): Pair<String?, String?> = seenThrough {
        val look = looks.value.firstOrNull { it.id == id } ?: return@seenThrough null to "That Look is not here any more."
        if (look.shared.isNotEmpty() && !again) return@seenThrough look.shared to null
        val signed = accounts.signed.value ?: return@seenThrough null to SIGN_IN_FIRST
        val sending = com.gabriel.zenithui.data.Looks.forSharing(look).toJson().toString()
        when (val reply = server.shareLook(signed.token, sending)) {
            is Reply.Ok -> {
                // The new code is out before the old one is taken back: a share never ends with none.
                if (look.shared.isNotEmpty()) server.deleteLook(signed.token, look.shared)
                store.setLooks(store.looks.value.map { if (it.id == id) it.copy(shared = reply.value, sharedAt = System.currentTimeMillis()) else it })
                reply.value to null
            }
            else -> null to lost(reply)
        }
    }

    /** Takes back the code the Look [id] was shared under: it opens nothing any more. Answers with why not, or null. */
    suspend fun stopSharingLook(id: String): String? = seenThrough {
        val look = looks.value.firstOrNull { it.id == id } ?: return@seenThrough null
        if (look.shared.isEmpty()) return@seenThrough null
        val signed = accounts.signed.value ?: return@seenThrough SIGN_IN_FIRST
        val reply = server.deleteLook(signed.token, look.shared)
        // Gone from the server already is as good as taken back.
        if (reply is Reply.Ok || (reply is Reply.Refused && reply.error == "no_look")) {
            store.setLooks(store.looks.value.map { if (it.id == id) it.copy(shared = "", sharedAt = 0L) else it })
            null
        } else {
            lost(reply)
        }
    }

    /** Opens a Look somebody shared, by its code, and puts it on. Needs no account. Answers with why not, or null. */
    suspend fun openLookCode(code: String): String? = seenThrough {
        val cleaned = code.uppercase().filter { it.isLetterOrDigit() }.removePrefix("LOOK")
        if (cleaned.length != 6) return@seenThrough "A Look's code is LOOK- and six letters and numbers."
        when (val reply = server.openLook(cleaned)) {
            is Reply.Ok -> {
                val look = runCatching { com.gabriel.zenithui.data.Look.fromJson(org.json.JSONObject(reply.value)) }.getOrNull()
                    ?: return@seenThrough "That code is for something that is not a Look."
                applyLook(look, fromElsewhere = true)
                null
            }
            else -> reply.problem()
        }
    }

    /** The Looks the server's keeper chose to show, or none when there is no server or no answer. */
    suspend fun featuredLooks(): List<com.gabriel.zenithui.data.server.SharedLook> =
        (server.featuredLooks() as? Reply.Ok)?.value.orEmpty()

    private companion object {
        const val ACCOUNT_LOOK_EVERY_MS = 10 * 60 * 1000L
        const val ACCOUNT_LOOK_AGAIN_MS = 60 * 1000L
        const val SIGN_IN_FIRST = "Sign in to your Zenith account first."

        /** As many photos as the frame holds, and the longest side each is kept at. */
        const val FRAME_MOST = 12
        const val FRAME_SIDE = 1440
    }

    override fun onCleared() {
        finishRemoval()
        wallpaperSource.stopWatching()
        director.stop()
        repository.stopObserving()
        super.onCleared()
    }

    fun refreshWallpaper() = viewModelScope.launch { wallpaperSource.refresh() }

    /**
     * Makes one of Zenith's designs the wallpaper, painted in the colour set [colours] (its id) and
     * from now on changing between light and dark with Zenith; on the lock screen too if [lockToo].
     * The set is always written, so the wallpaper never depends on paletteId, which nothing sets
     * any more.
     */
    fun setWallpaper(style: WallStyle, colours: String, lockToo: Boolean) {
        store.update {
            it.copy(
                wallpaperStyle = style.name,
                wallpaperPalette = colours,
                wallpaperLock = lockToo,
            )
        }
        director.request()
    }

    /** A photo is the wallpaper now: no design of Zenith's is in use, and nothing follows light and dark. */
    fun photoWallpaperSet() = store.update { it.copy(wallpaperStyle = "") }

    /** Whether the home screen is on show, which is when a new wallpaper can be faded in. */
    fun homeShowing(showing: Boolean) {
        director.homeShowing = showing
        if (showing) {
            _visits.value += 1
            refreshWeather()
        }
        followSchedule(showing)
    }

    // ---------------------------------------------------------------- widgets' own things

    private val _visits = MutableStateFlow(0)

    /** How many times the home screen has been come back to: the photo frame moves on with it. */
    val visits: StateFlow<Int> = _visits.asStateFlow()

    private val weather = com.gabriel.zenithui.data.WeatherRepository(application)

    /** The Weather widget's forecast, for the place chosen, once there is one. */
    val forecast = weather.forecast

    /** Fetches the forecast if a place has been chosen and the last one is old. Sends nothing otherwise. */
    fun refreshWeather(force: Boolean = false) {
        val s = settings.value
        if (s.weatherPlace.isBlank()) return
        viewModelScope.launch { weather.refresh(s.weatherPlace, s.weatherLat, s.weatherLon, s.weatherUnits, force) }
    }

    suspend fun searchPlaces(query: String): List<com.gabriel.zenithui.data.Place> = weather.search(query)

    /** The Weather widget's place, and its units; a place of null stops the widget asking for any. */
    fun setWeather(place: com.gabriel.zenithui.data.Place?, units: String) {
        store.update {
            it.copy(
                weatherPlace = place?.name.orEmpty(),
                weatherLat = place?.lat ?: 0.0,
                weatherLon = place?.lon ?: 0.0,
                weatherUnits = units,
            )
        }
        refreshWeather(force = true)
    }

    /** Where the Photo frame's pictures are kept. */
    val frameDir = java.io.File(application.filesDir, "frame").apply { mkdirs() }

    /**
     * Keeps the photos at [uris] for the Photo frame, each no bigger than a screen, in place of
     * those it had. False when none of them could be read.
     */
    suspend fun keepFramePhotos(uris: List<android.net.Uri>): Boolean {
        val kept = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            uris.take(FRAME_MOST).mapIndexedNotNull { index, uri ->
                runCatching {
                    val source = android.graphics.ImageDecoder.createSource(getApplication<Application>().contentResolver, uri)
                    val picture = android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                        val longest = maxOf(info.size.width, info.size.height)
                        if (longest > FRAME_SIDE) decoder.setTargetSampleSize(longest / FRAME_SIDE)
                    }
                    val name = "frame-${System.currentTimeMillis()}-$index.jpg"
                    java.io.File(frameDir, name).outputStream().use { picture.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }
                    name
                }.getOrNull()
            }
        }
        if (kept.isEmpty()) return false
        val before = settings.value.framePhotos
        store.update { it.copy(framePhotos = kept) }
        before.filterNot { it in kept }.forEach { runCatching { java.io.File(frameDir, it).delete() } }
        return true
    }

    /** What the Countdown counts down to; a [day] of 0 clears it. */
    fun setCountdown(title: String, day: Long) = store.update {
        it.copy(countdownTitle = title.trim(), countdownDay = day, countdownFrom = java.time.LocalDate.now().toEpochDay())
    }

    fun refreshApps() = viewModelScope.launch {
        repository.refresh()
        repository.refreshWork()
    }

    /**
     * Puts the layout back in line: anything overlapping, off the grid or between rows is moved
     * to the nearest place it fits. Returns how many items had to move.
     */
    fun tidyUp(): Int {
        val current = settings.value
        val before = layout.value
        val after = LayoutBuilder.settle(before, current.columns, current.rows, free = current.freeform && plus.value.active)
        val was = before.associateBy { it.id }
        val moved = after.count { was[it.id] != it }
        if (moved > 0) store.setLayout(after)
        return moved
    }

    // ---------------------------------------------------------------- onboarding

    /** The real work behind the setup screen: read the device, then compose a home screen. */
    suspend fun buildHome(
        name: String,
        appearance: String,
        widgets: List<String>,
        density: GridDensity,
        columns: Int,
        rows: Int,
    ) {
        val installed = repository.refresh()
        val dock = repository.suggestedDock(LayoutBuilder.setupDockSize(columns))
        val items = LayoutBuilder.build(repository, installed, widgets, columns, rows, dock)
        store.update {
            // A home screen built in setup is on the new look from the first, and is never asked
            // about it: the iPhone's icon shape, colour widgets and the glass dock, shown. Its grid
            // is the one picked in setup, which starts on the iPhone's.
            NewLook.apply(it).copy(
                userName = name.trim(),
                appearance = appearance,
                gridDensity = density.id,
                columns = columns,
                rows = rows,
                dock = dock,
                // Still written, for Looks and backups, though setup no longer offers Search and the
                // home screen draws no bar for it: Search is its pill, always there.
                searchBar = Widgets.Search.type in widgets,
                // Built just now, so already under today's rules. Without this a rerun of setup
                // left the version behind, and the next launch upgraded the new layout as if it
                // were an old one — every widget four times too tall, every app below the page.
                layoutVersion = LayoutTidy.VERSION,
            )
        }
        store.setLayout(items)
        // Saved here rather than through updateSettings, so the icons are told their shape here
        // too. Setup has drawn them in it already (previewNewLook), and then this changes nothing.
        settings.value.let { saved -> icons.configure(saved.iconPack, saved.iconShape) }
        wallpaperSource.refresh()
    }

    /**
     * Setup shows the home screen it is about to build, which is on the new look, so its icons are
     * drawn in the new look's shape from the first, rather than changing shape as the home screen
     * is put together. Nothing is saved: building the home screen saves the look.
     */
    fun previewNewLook() {
        val look = NewLook.apply(settings.value)
        viewModelScope.launch { icons.configure(look.iconPack, look.iconShape) }
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
        // Nothing taken off can come back onto a home screen that is starting again.
        letGo?.cancel()
        letGo = null
        _removed.value = emptyList()
        store.reset()
        // Every other app's widget goes with the layout it was on.
        runCatching { widgetHost.deleteHost() }
        viewModelScope.launch { icons.configure("", "squircle") }
    }

    /**
     * Gives back the number of every other app's widget that nothing on the page holds any more —
     * left by a restored backup, or by an add that never finished — so Android stops keeping it up
     * to date. Never while a widget is on its way in.
     */
    private fun tidyWidgetIds() {
        if (pending != null) return
        val held = heldWidgetIds(includeLooks = true)
        runCatching { widgetHost.appWidgetIds }.getOrNull()?.forEach { id ->
            if (id !in held) runCatching { widgetHost.deleteAppWidgetId(id) }
        }
    }

    /**
     * The numbers of other apps' widgets on the page and, with [includeLooks], in the Looks saved
     * on this phone: a widget a Look holds must still be there when that Look is put on again.
     */
    private fun heldWidgetIds(includeLooks: Boolean): Set<Int> {
        fun of(items: List<HomeItem>) = items.flatMap { item ->
            when (item) {
                is HostedWidgetCell -> listOf(item.appWidgetId)
                is StackCell -> item.members.filter { it.hosted }.map { it.appWidgetId }
                else -> emptyList()
            }
        }
        // Read from the store, which is there from the first line: this runs while init does.
        val inLooks = if (!includeLooks) emptyList() else store.looks.value.flatMap { look ->
            of(com.gabriel.zenithui.data.Looks.apply(ZenithSettings(), look).second)
        }
        // What Undo can still put back holds its widgets too.
        return (of(layout.value + _removed.value) + inLooks).toSet()
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
            val settled = LayoutBuilder.settle(layout.value, after.columns, after.rows, free = after.freeform && plus.value.active)
            if (settled != layout.value) store.setLayout(settled)
        }
    }

    /**
     * Re-derives the grid for the screen we are actually on. Called whenever the window changes
     * size — a fold opening, a rotation, a tablet — so cells keep the size the user asked for
     * instead of stretching to fill whatever glass they are on.
     */
    fun resolveGrid(widthDp: Float, heightDp: Float) {
        screenDp = widthDp to heightDp
        val current = settings.value
        val (columns, rows) = GridDensity.resolve(current.gridDensity, current.gridColumns, current.gridRows, widthDp, heightDp)
        if (columns == current.columns && rows == current.rows) return
        updateSettings { it.copy(columns = columns, rows = rows) }
    }

    fun setNote(text: String) = store.update { it.copy(note = text) }

    // ---------------------------------------------------------------- launching

    /**
     * Opens an app, or runs a shortcut when [key] is one. [sourceBounds] and [options] let its
     * window grow out of the icon that was touched. False when it could not be opened.
     */
    fun open(key: String, sourceBounds: android.graphics.Rect? = null, options: android.os.Bundle? = null): Boolean {
        if (ShortcutKeys.isShortcut(key)) return repository.startShortcut(key, sourceBounds, options)
        if (com.gabriel.zenithui.data.WorkKeys.isWork(key)) return repository.launchWork(key, sourceBounds, options)
        if (!repository.launch(key, sourceBounds, options)) {
            refreshApps()
            return false
        }
        // Remembered for the library's Suggestions, kept short: only the latest dozen matter. And
        // counted, for the drawer's Most used.
        store.update {
            it.copy(
                recentLaunches = (listOf(key) + (it.recentLaunches - key)).take(12),
                launchCounts = com.gabriel.zenithui.data.DrawerSort.counted(it.launchCounts, key),
            )
        }
        return true
    }

    // ---------------------------------------------------------------- one app's own name and icon

    /** Changes what is shown for one app: its name, its picture, whether its name shows. */
    fun editApp(key: String, transform: (AppEdit) -> AppEdit) {
        val before = settings.value.appEdits[key] ?: AppEdit()
        val after = transform(before)
        store.update { it.copy(appEdits = AppEdits.with(it.appEdits, key, after)) }
        // A photo nobody uses any more is not kept; a saved Look that still shows it is somebody.
        if (before.icon != after.icon && before.icon !in photosInUse()) icons.forgetPhoto(before.icon)
    }

    /** Every photo chosen as an icon that the home screen, or a Look saved on this phone, shows. */
    private fun photosInUse(): Set<String> =
        (listOf(settings.value.appEdits) + store.looks.value.map { com.gabriel.zenithui.data.Looks.apply(ZenithSettings(), it).first.appEdits })
            .flatMap { AppEdits.icons(it).values }
            .filter { IconSource.parse(it) is IconSource.Photo }
            .toSet()

    /** Takes the photo at [uri] as [key]'s icon. False when it could not be read. */
    suspend fun choosePhotoIcon(key: String, uri: android.net.Uri): Boolean {
        val kept = icons.keepPhoto(uri) ?: return false
        editApp(key) { it.copy(icon = IconSource.photo(kept)) }
        return true
    }

    /** An app's own name, as it came, whatever you have called it. */
    fun originalLabel(key: String): String = repository.byKey(key)?.label ?: key.substringBefore('/').substringAfterLast('.')

    // ---------------------------------------------------------------- other apps' widgets

    /** What Android has to be asked, by the activity, while a widget is being added. */
    sealed interface WidgetAsk {
        /** Its permission to put this widget on the home screen. */
        data class Bind(val id: Int, val info: android.appwidget.AppWidgetProviderInfo) : WidgetAsk
        /** The widget's own set-up screen. */
        data class Configure(val id: Int) : WidgetAsk
    }

    private val _widgetAsks = kotlinx.coroutines.channels.Channel<WidgetAsk>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val widgetAsks = _widgetAsks.receiveAsFlow()

    private val _widgetPlaced = kotlinx.coroutines.channels.Channel<Int>(kotlinx.coroutines.channels.Channel.CONFLATED)

    /** The page a widget has just gone onto, for the home screen to turn to. */
    val widgetPlaced = _widgetPlaced.receiveAsFlow()

    /** One widget on its way in: its number, what it is, its size, and where it is going. */
    private data class PendingWidget(
        val id: Int,
        val info: android.appwidget.AppWidgetProviderInfo,
        val spanX: Int,
        val spanY: Int,
        /** A widget or stack it is to be stacked on, rather than a spot of its own. */
        val stackOnto: String? = null,
        /** A widget whose place it takes, after a restore on another phone. */
        val replacing: String? = null,
    )

    private var pending: PendingWidget? = null

    /** One cell's width and one whole row's height on the page, in dp, as the grid was last drawn. */
    private var cellDp = 80f
    private var rowDp = 100f

    /** And its icons' size, in dp: a widget is drawn from the icons in its first cell to those in its last. */
    private var iconDp = com.gabriel.zenithui.data.HomeGeometry.iconSizeDp(cellDp, rowDp, showLabels = true)

    fun noteCellSize(cellWidthDp: Float, rowHeightDp: Float, iconSizeDp: Float) {
        if (cellWidthDp > 0f) cellDp = cellWidthDp
        if (rowHeightDp > 0f) rowDp = rowHeightDp
        if (iconSizeDp > 0f) iconDp = iconSizeDp
    }

    /**
     * Starts putting another app's widget on the home screen: on a spot of its own, onto a widget
     * or stack ([stackOnto]), or in place of one that did not come through a restore ([replacing]).
     * Android may want to be asked first, and the widget may have a set-up screen; each of those
     * comes to the activity through [widgetAsks], and its answer back through [widgetBound] and
     * [widgetConfigured].
     */
    fun addHostedWidget(info: android.appwidget.AppWidgetProviderInfo, stackOnto: String? = null, replacing: String? = null) {
        cancelPendingWidget()
        val s = settings.value
        val target = layout.value.firstOrNull { it.id == (stackOnto ?: replacing) }
        val (spanX, spanY) = if (target != null) {
            target.spanX to target.spanY
        } else {
            val targetX = if (android.os.Build.VERSION.SDK_INT >= 31) info.targetCellWidth else 0
            val targetY = if (android.os.Build.VERSION.SDK_INT >= 31) info.targetCellHeight else 0
            com.gabriel.zenithui.data.HostedSizes.spanFor(info.minWidth, info.minHeight, targetX, targetY, cellDp, rowDp, iconDp, s.columns, s.rows)
        }
        val id = runCatching { widgetHost.allocateAppWidgetId() }.getOrNull() ?: return
        pending = PendingWidget(id, info, spanX, spanY, stackOnto, replacing)
        val bound = runCatching { widgetManager.bindAppWidgetIdIfAllowed(id, info.profile, info.provider, null) }.getOrDefault(false)
        if (bound) afterBind() else _widgetAsks.trySend(WidgetAsk.Bind(id, info))
    }

    /** Android's answer to whether the widget may be put on the home screen. */
    fun widgetBound(ok: Boolean) {
        if (ok) afterBind() else cancelPendingWidget()
    }

    /** The widget's set-up screen is done: [ok] when it was finished rather than backed out of. */
    fun widgetConfigured(ok: Boolean) {
        if (ok) placePendingWidget() else cancelPendingWidget()
    }

    private fun afterBind() {
        val waiting = pending ?: return
        // From Android 12 a widget can say its set-up is only a choice, to be made later.
        val optional = android.os.Build.VERSION.SDK_INT >= 31 &&
            waiting.info.widgetFeatures and android.appwidget.AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL != 0
        if (waiting.info.configure != null && !optional) _widgetAsks.trySend(WidgetAsk.Configure(waiting.id)) else placePendingWidget()
    }

    private fun placePendingWidget() {
        val waiting = pending ?: return
        pending = null
        val provider = waiting.info.provider.flattenToString()
        val member = StackMember(LayoutBuilder.newId(), appWidgetId = waiting.id, provider = provider)
        val items = layout.value
        val onto = waiting.stackOnto?.let { id -> items.firstOrNull { it.id == id } }
        val replaced = waiting.replacing?.let { id -> items.firstOrNull { it.id == id } }
        when {
            onto != null -> {
                val stacked = StackRules.stack(onto, member, LayoutBuilder.newId())
                if (stacked == null) {
                    runCatching { widgetHost.deleteAppWidgetId(waiting.id) }
                    return
                }
                store.mutateLayout { all -> all.map { if (it.id == onto.id) stacked else it } }
                _widgetPlaced.trySend(onto.page)
            }
            replaced is HostedWidgetCell -> {
                store.mutateLayout { all -> all.map { if (it.id == replaced.id) replaced.copy(appWidgetId = waiting.id, provider = provider) else it } }
                _widgetPlaced.trySend(replaced.page)
            }
            replaced is StackCell -> {
                // A stack keeps its place; the one member that did not come through is put back.
                val lost = replaced.members.indexOfFirst { it.hosted && it.provider == provider && widgetManager.getAppWidgetInfo(it.appWidgetId) == null }
                val members = replaced.members.mapIndexed { i, m -> if (i == lost) m.copy(appWidgetId = waiting.id) else m }
                store.mutateLayout { all -> all.map { if (it.id == replaced.id) replaced.copy(members = members) else it } }
                _widgetPlaced.trySend(replaced.page)
            }
            else -> {
                val slot = firstFreeSlot(waiting.spanX, waiting.spanY, rowStep())
                if (slot == null) {
                    runCatching { widgetHost.deleteAppWidgetId(waiting.id) }
                    return
                }
                store.mutateLayout { all ->
                    all + HostedWidgetCell(member.id, slot.first, slot.second, slot.third, waiting.spanX, waiting.spanY, waiting.id, provider)
                }
                _widgetPlaced.trySend(slot.first)
            }
        }
    }

    private fun cancelPendingWidget() {
        val waiting = pending ?: return
        pending = null
        runCatching { widgetHost.deleteAppWidgetId(waiting.id) }
    }

    /** Puts one of Zenith's own widgets onto the widget or stack [ontoId]. False when it cannot take more. */
    fun stackWidget(type: String, ontoId: String): Boolean {
        val onto = layout.value.firstOrNull { it.id == ontoId } ?: return false
        val stacked = StackRules.stack(onto, StackMember(LayoutBuilder.newId(), type = type), LayoutBuilder.newId()) ?: return false
        store.mutateLayout { all -> all.map { if (it.id == ontoId) stacked else it } }
        return true
    }

    /**
     * Takes the member at [index] out of stack [stackId] onto a spot of its own on the same page,
     * the nearest there is. False when the page has no room for it.
     */
    fun unstack(stackId: String, index: Int): Boolean {
        val stack = layout.value.firstOrNull { it.id == stackId } as? StackCell ?: return false
        val member = stack.members.getOrNull(index) ?: return false
        val rest = StackRules.without(stack, index) ?: return false
        val s = settings.value
        val grid = PageGrid(s.columns, s.rows * ROW_UNITS)
        (layout.value.filter { it.page == stack.page && it.id != stackId } + rest).forEach { grid.occupyItem(it) }
        val slot = grid.findSlot(stack.spanX, stack.spanY, rowStep()) ?: return false
        val alone = StackRules.alone(stack, member).moved(stack.page, slot.first, slot.second)
        store.mutateLayout { all -> all.map { if (it.id == stackId) rest else it } + alone }
        return true
    }

    /** Turns stack [stackId] so the member at [index] is the one shown first. */
    fun bringToTop(stackId: String, index: Int) = store.mutateLayout { all ->
        all.map { if (it.id == stackId && it is StackCell) StackRules.bringToTop(it, index) else it }
    }

    /** A pane of glass behind another app's widget, or none. */
    fun setHostedGlass(id: String, glass: Boolean) = store.mutateLayout { all ->
        all.map { if (it.id == id && it is HostedWidgetCell) it.copy(glass = glass) else it }
    }

    /**
     * Resizes another app's widget, or a stack, where it stands: never smaller than its widget
     * says it can be, and only into room that is free.
     */
    fun resizeHosted(id: String, spanX: Int, spanY: Int): Boolean {
        val item = layout.value.firstOrNull { it.id == id } ?: return false
        val s = settings.value
        val (minX, minY) = when (item) {
            is HostedWidgetCell -> widgetManager.getAppWidgetInfo(item.appWidgetId)?.let { info ->
                com.gabriel.zenithui.data.HostedSizes.minSpan(
                    info.minResizeWidth.takeIf { it > 0 } ?: info.minWidth,
                    info.minResizeHeight.takeIf { it > 0 } ?: info.minHeight,
                    cellDp, rowDp, iconDp, s.columns, s.rows,
                )
            } ?: (1 to ROW_UNITS)
            else -> 1 to ROW_UNITS
        }
        val free = s.freeform && plus.value.active
        val targetX = spanX.coerceIn(minX, s.columns)
        val targetY = (if (free) spanY else LayoutBuilder.wholeRows(spanY)).coerceIn(minY, s.rows * ROW_UNITS)
        if (targetX == item.spanX && targetY == item.spanY) return false
        if (!isSlotFree(item.page, item.col, item.row, targetX, targetY, id)) return false
        store.mutateLayout { all ->
            all.map {
                when {
                    it.id != id -> it
                    it is HostedWidgetCell -> it.copy(spanX = targetX, spanY = targetY)
                    it is StackCell -> it.copy(spanX = targetX, spanY = targetY)
                    else -> it
                }
            }
        }
        return true
    }

    // ---------------------------------------------------------------- shortcuts

    suspend fun shortcuts(packageName: String): List<AppShortcut> = repository.shortcuts(packageName)

    /** Puts one of an app's shortcuts on the first free spot and keeps it pinned. Returns the page. */
    fun addShortcut(shortcut: AppShortcut): Int? {
        val slot = firstFreeSlot(1, ROW_UNITS, rowStep()) ?: return null
        repository.pinShortcut(shortcut.key, pinned = true)
        store.mutateLayout { items ->
            items + ShortcutCell(LayoutBuilder.newId(), slot.first, slot.second, slot.third, shortcut.packageName, shortcut.id, shortcut.label)
        }
        return slot.first
    }

    fun openAppInfo(key: String) =
        if (com.gabriel.zenithui.data.WorkKeys.isWork(key)) repository.openWorkAppInfo(key) else repository.openAppInfo(key)

    /** The work profile's apps, for the drawer's Work tab. */
    val workApps = repository.workApps

    /** Every app's shortcuts, for the drawer's search to look through. Asked once each time it opens. */
    suspend fun allShortcuts(): List<AppShortcut> = repository.allShortcuts()

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

    /**
     * Takes [id] off the home screen. For [UNDO_MS] it can be put back with [undoRemove], so what
     * it holds — another app's widget's number, a shortcut's pin — is kept until then.
     */
    fun removeItem(id: String) = removeItems(listOf(id))

    /** Takes [ids] off the home screen together: one Undo puts them all back. */
    fun removeItems(ids: List<String>) {
        val gone = layout.value.filter { it.id in ids }
        if (gone.isEmpty()) return
        // What was taken off before is past putting back now.
        finishRemoval()
        store.mutateLayout { items -> items.filterNot { it.id in ids } }
        _removed.value = gone
        letGo = viewModelScope.launch {
            kotlinx.coroutines.delay(com.gabriel.zenithui.data.UNDO_MS)
            finishRemoval()
        }
    }

    /**
     * Puts back what was just taken off: where it was, if that spot is still free, or else in the
     * first opening. It comes back under a new id, so the page sees it arrive and springs it out
     * of its spot. Answers with where it went, or null when there was nothing to put back.
     */
    fun undoRemove(): List<HomeItem> {
        val items = _removed.value
        if (items.isEmpty()) return emptyList()
        letGo?.cancel()
        letGo = null
        _removed.value = emptyList()
        val s = settings.value
        val back = ArrayList<HomeItem>()
        for (item in items) {
            // Each put back counts as taken for the next, so two never land on one spot.
            val placed = LayoutBuilder.putBack(layout.value + back, item, s.columns, s.rows, rowStep()) ?: continue
            back += placed.withId(LayoutBuilder.newId())
        }
        store.mutateLayout { it + back }
        return back
    }

    // ---------------------------------------------------------------- the page overview

    /** Page [from] moved to [to], everything on it going with it. */
    fun movePage(from: Int, to: Int) {
        finishRemoval()
        store.setLayout(com.gabriel.zenithui.data.PageOrder.move(layout.value, from, to))
    }

    /** Page [page] taken away, and all that is on it; the pages after it close up. */
    fun removePage(page: Int) {
        finishRemoval()
        val s = settings.value
        val count = maxOf((layout.value.maxOfOrNull { it.page } ?: 0) + 1, s.pages)
        if (count <= 1) return
        val (kept, gone) = com.gabriel.zenithui.data.PageOrder.remove(layout.value, page)
        store.setLayout(kept)
        store.update { it.copy(pages = (count - 1).coerceAtLeast(1)) }
        // What only that page held is let go: shortcuts' pins, and other apps' widgets.
        gone.filterIsInstance<ShortcutCell>().filter { g -> kept.none { it is ShortcutCell && it.key == g.key } }
            .forEach { repository.pinShortcut(it.key, pinned = false) }
        tidyWidgetIds()
    }

    /** What the last removal held is given back now: it is past putting back. */
    private fun finishRemoval() {
        letGo?.cancel()
        letGo = null
        val goneAll = _removed.value
        if (goneAll.isEmpty()) return
        _removed.value = emptyList()
        // Another app's widget gives its number back, so Android stops keeping it up to date —
        // unless a saved Look still holds it.
        val stillHeld = heldWidgetIds(includeLooks = true)
        for (gone in goneAll) {
            // A shortcut no page holds any more is let go, so the app may tidy it away.
            if (gone is ShortcutCell && layout.value.none { it is ShortcutCell && it.key == gone.key }) {
                repository.pinShortcut(gone.key, pinned = false)
            }
            when (gone) {
                is HostedWidgetCell -> if (gone.appWidgetId !in stillHeld) runCatching { widgetHost.deleteAppWidgetId(gone.appWidgetId) }
                is StackCell -> gone.members.filter { it.hosted && it.appWidgetId !in stillHeld }.forEach { runCatching { widgetHost.deleteAppWidgetId(it.appWidgetId) } }
                else -> Unit
            }
        }
    }

    /**
     * The icons gathered while [mainId] was carried, put down round where it landed: each in the
     * free spot nearest it on its page, in the order they were gathered. One with no room left on
     * that page stays where it was.
     */
    fun landBeside(mainId: String, gathered: List<String>) {
        val s = settings.value
        store.mutateLayout { items -> LayoutBuilder.landBeside(items, mainId, gathered, s.columns, s.rows, rowStep()) }
    }

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

    /**
     * Puts a widget on the first page with room for it and returns that page, or null when no
     * page has room. With no size given it is the widget's ordinary one; any size asked for is
     * fitted to what the widget and the grid allow. [wantY] is in row units.
     */
    fun addWidget(type: String, wantX: Int? = null, wantY: Int? = null): Int? {
        val spec = Widgets.byType(type) ?: return null
        val s = settings.value
        val (spanX, spanY) = if (wantX != null && wantY != null) {
            Widgets.clampSpan(spec, wantX, wantY, s.columns, s.rows)
        } else {
            Widgets.defaultSpan(spec, s.columns, s.rows)
        }
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
     * Gives a widget one of the gallery's named sizes while it is on the page: where it stands if
     * there is room, otherwise the closest opening on its page. False when the page has no room.
     */
    fun resizeWidgetTo(id: String, spanX: Int, spanY: Int): Boolean {
        val widget = layout.value.firstOrNull { it.id == id } as? WidgetCell ?: return false
        val spec = Widgets.byType(widget.type) ?: return false
        val s = settings.value
        val (targetX, targetY) = Widgets.clampSpan(spec, spanX, LayoutBuilder.wholeRows(spanY), s.columns, s.rows)
        val next = Reshape.resized(layout.value, id, targetX, targetY, s.columns, s.rows, rowStep()) ?: return false
        if (next != layout.value) store.setLayout(next)
        return true
    }

    /**
     * Moves an item one cell across or one row up or down from the resize bar. A row is a whole
     * row, or a quarter of one with free placement on. False when the page ends there or
     * something is in the way.
     */
    fun nudgeItem(id: String, dCol: Int, dRows: Int): Boolean {
        val s = settings.value
        val next = Reshape.nudged(layout.value, id, dCol, dRows * rowStep(), s.columns, s.rows) ?: return false
        if (next != layout.value) store.setLayout(next)
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
        for (page in LayoutBuilder.pagesToTry(items.maxOfOrNull { it.page } ?: 0, from = startPage)) {
            val grid = PageGrid(s.columns, s.rows * ROW_UNITS)
            items.filter { it.page == page }.forEach { grid.occupyItem(it) }
            val slot = grid.findSlot(spanX, spanY, rowStep) ?: continue
            return Triple(page, slot.first, slot.second)
        }
        return null
    }

    /**
     * Returns page/col/row of the first opening big enough for the given span, in row units, on
     * a new page if the ones in use are full. Null when every page there can be is full.
     */
    fun firstFreeSlot(spanX: Int, spanY: Int, rowStep: Int = 1): Triple<Int, Int, Int>? {
        val s = settings.value
        return LayoutBuilder.firstFreeSlot(layout.value, s.columns, s.rows, spanX, spanY, rowStep)
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

    private fun dockLimit(): Int = LayoutBuilder.dockLimit(settings.value.columns)

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
