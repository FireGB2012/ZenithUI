package com.gabriel.zenithui.ui.settings

/** The groups on the first page of Settings. Zenith Plus opens a sheet of its own, not a page. */
enum class SettingsPage(val title: String, val opensSheet: Boolean = false) {
    Theme("Wallpaper & theme"),
    Home("Home screen"),
    Motion("Motion & feel"),
    Apps("Apps"),
    Plus("Zenith Plus", opensSheet = true),
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
        SettingsEntry("palette", SettingsPage.Theme, "Colour", "color palette theme accent tint make your own theme maker"),
        SettingsEntry("appearance", SettingsPage.Theme, "Light or dark", "auto night day mode appearance"),
        SettingsEntry("wallpaper", SettingsPage.Theme, "Wallpaper", "background picture photo design"),
        SettingsEntry("wallpaperFollow", SettingsPage.Theme, "Wallpaper follows theme", "repaint match colour color"),
        SettingsEntry("glass", SettingsPage.Theme, "Glass", "blur frost transparency strength liquid"),
        SettingsEntry("glassBody", SettingsPage.Theme, "Glass panels", "clear frosted solid contrast readable see through transparent blend wallpaper stand out"),
        SettingsEntry("tiltShine", SettingsPage.Theme, "Tilt shine", "highlight light reflection gyro"),

        SettingsEntry("grid", SettingsPage.Home, "Grid", "density compact balanced roomy custom columns rows size icons layout cells"),
        SettingsEntry("iconShape", SettingsPage.Home, "Icon shape", "squircle circle rounded square"),
        SettingsEntry("iconPack", SettingsPage.Home, "Icon pack", "icons nova adw"),
        SettingsEntry("labels", SettingsPage.Home, "Labels", "names text app names"),
        SettingsEntry("dock", SettingsPage.Home, "Dock", "bottom favourites favorites"),
        SettingsEntry("searchBar", SettingsPage.Home, "Search bar", "find google web"),
        SettingsEntry("header", SettingsPage.Home, "Top of the page", "name greeting header title hello"),
        SettingsEntry("freeform", SettingsPage.Home, "Free placement", "plus anywhere freeform resize"),
        SettingsEntry("tidy", SettingsPage.Home, "Tidy up", "arrange sort clean align fix gaps"),

        SettingsEntry("animations", SettingsPage.Motion, "Animations", "reduce reduced full movement"),
        SettingsEntry("openAnimation", SettingsPage.Motion, "Opening apps", "launch colour color glass card"),
        SettingsEntry("pageEffect", SettingsPage.Motion, "Page swipe", "depth slide cube stack transition effect"),
        SettingsEntry("haptics", SettingsPage.Motion, "Haptics", "vibrate vibration tick feedback buzz"),

        SettingsEntry("appsView", SettingsPage.Apps, "Swipe up for", "drawer library list"),
        SettingsEntry("hiddenApps", SettingsPage.Apps, "Hidden apps", "hide private pin plus lock"),

        SettingsEntry("plus", SettingsPage.Plus, "Zenith Plus", "subscribe subscription buy trial lifetime purchase"),
        SettingsEntry("aura", SettingsPage.Aura, "Aura", "streak points daily reward balance flame"),
        SettingsEntry("auraThemes", SettingsPage.Aura, "Themes to unlock", "ember tide orchid solstice obsidian buy spend"),
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
