package com.gabriel.zenithui.ui.settings

/** The groups on the first page of Settings. Zenith Plus opens a sheet of its own, not a page. */
enum class SettingsPage(val title: String, val opensSheet: Boolean = false) {
    // Theme in the code, as it always was, though there are no themes on the page now.
    Theme("Wallpaper & appearance"),
    Home("Home screen"),
    Motion("Motion & feel"),
    /** A page of Motion & feel rather than a group of its own: it is reached from there, and goes back there. */
    AdvancedMotion("Advanced animation settings"),
    Gestures("Gestures"),
    Apps("Apps"),
    Plus("Zenith Plus", opensSheet = true),
    Island("Island"),
    Looks("Looks"),
    Aura("Aura"),
    /** Only in a build that has a server: see `server.properties`. */
    Account("Account"),
    Backup("Backup & reset"),
    About("About & help"),
}

/**
 * One option: where it lives, what it is called on the page, and the other words somebody might
 * type when looking for it.
 */
data class SettingsEntry(
    val id: String,
    val page: SettingsPage,
    val title: String,
    val keywords: String = "",
    /** Only there in a build that has a server to talk to. */
    val server: Boolean = false,
)

/**
 * Every option in Settings, once. The search box reads it, and so does the page a search result
 * opens, to know which row to bring into view. Kept free of anything on screen so it can be tested
 * on its own.
 */
object SettingsCatalog {

    val entries: List<SettingsEntry> = listOf(
        SettingsEntry("appearance", SettingsPage.Theme, "Light or dark", "auto night day mode appearance theme"),
        SettingsEntry("font", SettingsPage.Theme, "Font", "typeface text letters inter serif mono condensed hand casual"),
        SettingsEntry("numberFont", SettingsPage.Theme, "Numbers", "clock digits font thin typeface time"),
        SettingsEntry("wallpaper", SettingsPage.Theme, "Wallpaper", "background picture photo design"),
        SettingsEntry("wallpaperFollow", SettingsPage.Theme, "Wallpaper follows light and dark", "repaint match light dark night day"),
        SettingsEntry("glass", SettingsPage.Theme, "Glass", "blur frost transparency strength liquid"),
        SettingsEntry("glassBody", SettingsPage.Theme, "Glass panels", "clear frosted solid contrast readable see through transparent blend wallpaper stand out"),
        SettingsEntry("tiltShine", SettingsPage.Theme, "Tilt shine", "highlight light reflection gyro"),
        SettingsEntry("glassStudio", SettingsPage.Theme, "Glass studio", "glass bend refraction frost rim light rainbow direction tune plus"),

        SettingsEntry("grid", SettingsPage.Home, "Grid", "density compact balanced roomy custom columns rows size icons layout cells iphone four"),
        SettingsEntry("iconShape", SettingsPage.Home, "Icon shape", "squircle circle rounded square pebble teardrop hexagon clover iphone"),
        SettingsEntry("iconStyle", SettingsPage.Home, "Icon style", "themed tinted monochrome glass clear original colour color symbol"),
        SettingsEntry("widgetLook", SettingsPage.Home, "Widget look", "colour color glass solid widgets"),
        SettingsEntry("iconPack", SettingsPage.Home, "Icon pack", "icons nova adw"),
        SettingsEntry("labels", SettingsPage.Home, "Labels", "names text app names"),
        SettingsEntry("labelSize", SettingsPage.Home, "Name size", "label text font bigger smaller large small"),
        SettingsEntry("labelShadow", SettingsPage.Home, "Name shadow", "label text readable contrast outline"),
        SettingsEntry("badges", SettingsPage.Home, "Notification dots", "badge badges count unread number alerts"),
        SettingsEntry("dock", SettingsPage.Home, "Dock", "bottom favourites favorites"),
        SettingsEntry("dockStyle", SettingsPage.Home, "Dock style", "bar floating pill icons only glass"),
        // The top line, the page dots and the full-width search bar are not drawn on the home
        // screen any more: their settings are kept for old backups and Looks, but not offered.
        SettingsEntry("folderStyle", SettingsPage.Home, "Folders", "folder grid stack cover colour color"),
        SettingsEntry("lockLayout", SettingsPage.Home, "Lock layout", "lock freeze prevent move accidental"),
        SettingsEntry("hideStatusBar", SettingsPage.Home, "Hide the status bar", "status bar fullscreen immersive clock notifications"),
        SettingsEntry("freeform", SettingsPage.Home, "Free placement", "plus anywhere freeform resize"),
        SettingsEntry("tidy", SettingsPage.Home, "Tidy up", "arrange sort clean align fix gaps"),

        SettingsEntry("animations", SettingsPage.Motion, "Animations", "reduce reduced full movement"),
        SettingsEntry("openAnimation", SettingsPage.Motion, "Opening apps", "launch colour color glass card"),
        SettingsEntry("pageEffect", SettingsPage.Motion, "Page swipe", "depth slide cube stack carousel wheel flip fold transition effect turn"),
        SettingsEntry("arrival", SettingsPage.Motion, "Coming home", "arrive fly in unlock cascade below middle"),
        SettingsEntry("drawerArrival", SettingsPage.Motion, "Opening the drawer", "app drawer rise grow glass swipe up"),
        SettingsEntry("press", SettingsPage.Motion, "Pressing", "press tap touch bounce squish ripple feedback"),
        SettingsEntry("haptics", SettingsPage.Motion, "Haptics", "vibrate vibration tick feedback buzz"),
        SettingsEntry("advancedMotion", SettingsPage.Motion, "Advanced animation settings", "tuning tune spring stiffness damping bounce fine open close custom"),

        SettingsEntry("motionStyle", SettingsPage.AdvancedMotion, "Animation style", "preset zenith fluid bouncy dynamic sweet classic"),
        SettingsEntry("motionSpeed", SettingsPage.AdvancedMotion, "Animation speed", "fast slow quicker pace"),
        SettingsEntry("motionOpen", SettingsPage.AdvancedMotion, "Opening an app", "launch card arc curve stiffness damping corner logo shadow spring"),
        SettingsEntry("motionHome", SettingsPage.AdvancedMotion, "Home screen behind", "zoom dim dimming blur push pushed sink"),
        SettingsEntry("motionClose", SettingsPage.AdvancedMotion, "Closing an app", "return landing lands bounce ripple card spring stiffness damping"),

        SettingsEntry("gesture_swipeDown", SettingsPage.Gestures, "Swipe down", "notifications shade pull down gesture"),
        SettingsEntry("gesture_twoDown", SettingsPage.Gestures, "Two-finger swipe down", "quick settings fingers gesture"),
        SettingsEntry("gesture_twoUp", SettingsPage.Gestures, "Two-finger swipe up", "fingers gesture"),
        SettingsEntry("gesture_doubleTap", SettingsPage.Gestures, "Double tap", "lock screen off sleep tap twice gesture"),
        SettingsEntry("gesture_pinchIn", SettingsPage.Gestures, "Pinch in", "zoom customise gesture"),
        SettingsEntry("gesture_pinchOut", SettingsPage.Gestures, "Pinch out", "zoom overview pages gesture spread"),
        SettingsEntry("gesture_home", SettingsPage.Gestures, "Home button, when already home", "press first page gesture"),
        SettingsEntry("lockHelper", SettingsPage.Gestures, "Lock helper", "accessibility lock screen off double tap"),

        SettingsEntry("appsView", SettingsPage.Apps, "Swipe up for", "drawer library list"),
        SettingsEntry("drawerSort", SettingsPage.Apps, "Order", "sort drawer most used newest colour color alphabetical a to z"),
        SettingsEntry("drawerColumns", SettingsPage.Apps, "Columns", "drawer grid across wide apps per row"),
        SettingsEntry("drawerTabs", SettingsPage.Apps, "Tabs", "drawer groups categories work profile"),
        SettingsEntry("hiddenApps", SettingsPage.Apps, "Hidden apps", "hide private pin plus lock"),

        SettingsEntry("plus", SettingsPage.Plus, "Zenith Plus", "subscribe subscription buy trial lifetime purchase"),
        SettingsEntry("island", SettingsPage.Island, "Island", "dynamic island pill camera notch over apps overlay plus"),
        SettingsEntry("islandShows", SettingsPage.Island, "What it shows", "island music calls timers charger notifications ringer silent"),
        SettingsEntry("islandAlways", SettingsPage.Island, "Always show it", "island pill camera rest idle always"),
        SettingsEntry("islandWhen", SettingsPage.Island, "Out of the way", "island full screen video game sideways landscape hide"),
        SettingsEntry("lookSave", SettingsPage.Looks, "Save this look", "look looks setup profile preset theme save switch"),
        SettingsEntry("lookSchedule", SettingsPage.Looks, "Schedule", "look time night work automatic switch"),
        SettingsEntry("lookCode", SettingsPage.Looks, "Open a code", "look friend code import featured received", server = true),
        SettingsEntry("aura", SettingsPage.Aura, "Aura", "streak points daily reward balance flame"),
        SettingsEntry("auraCode", SettingsPage.Aura, "Your code", "referral share invite friend copy"),
        SettingsEntry("auraRedeem", SettingsPage.Aura, "Redeem a code", "referral gift friend enter"),

        SettingsEntry("account", SettingsPage.Account, "Account", "sign in log login register username password google create", server = true),
        SettingsEntry("accountDelete", SettingsPage.Account, "Delete account", "remove erase close", server = true),

        SettingsEntry("backup", SettingsPage.Backup, "Back up home screen", "save export file plus"),
        SettingsEntry("restore", SettingsPage.Backup, "Restore from a backup", "import load file plus"),
        SettingsEntry("cloudBackup", SettingsPage.Backup, "Back up to your account", "cloud online save plus", server = true),
        SettingsEntry("cloudRestore", SettingsPage.Backup, "Restore from your account", "cloud online load plus", server = true),
        SettingsEntry("reset", SettingsPage.Backup, "Run setup again", "start over erase clear welcome"),

        SettingsEntry("defaultHome", SettingsPage.About, "Default home app", "launcher set"),
        SettingsEntry("keepLoaded", SettingsPage.About, "Keep Zenith loaded", "battery optimisation optimization sleep background unrestricted"),
        SettingsEntry("help", SettingsPage.About, "How it works", "gestures tips guide"),
        SettingsEntry("fonts", SettingsPage.About, "Fonts", "licence license open font inter jetbrains"),
        SettingsEntry("version", SettingsPage.About, "Version", "build update"),
        SettingsEntry("privacy", SettingsPage.About, "Privacy policy", "data"),
    )

    /** What is on [page]. In a build with no [server], what needs one is not there. */
    fun on(page: SettingsPage, server: Boolean = true): List<SettingsEntry> = entries.filter { it.page == page && (server || !it.server) }

    /**
     * What a search for [query] finds. Every word typed has to start a word of the option's title,
     * of its other words, or of its page's name. Options matched by their title alone come first;
     * otherwise they keep the order of the pages. In a build with no [server], what needs one is
     * never found.
     */
    fun search(query: String, server: Boolean = true): List<SettingsEntry> {
        val typed = words(query)
        if (typed.isEmpty()) return emptyList()
        val entries = if (server) entries else entries.filter { !it.server }
        val byTitle = entries.filter { entry ->
            val title = words(entry.title)
            typed.all { word -> title.any { it.startsWith(word) } }
        }
        val byAnything = entries.filter { entry ->
            val all = words(entry.title) + words(entry.keywords) + words(entry.page.title)
            typed.all { word -> all.any { it.startsWith(word) } }
        }
        return byTitle + (byAnything - byTitle.toSet())
    }

    private val gaps = Regex("[^\\p{L}\\p{N}]+")

    private fun words(text: String): List<String> = text.lowercase().split(gaps).filter { it.isNotEmpty() }
}
