package com.gabriel.zenithui.data.connect

import org.json.JSONObject

/**
 * Zenith's own setups in Connect: styles made from what Zenith has, each a theme in Light or Dark,
 * one of its wallpaper designs in one of its colour sets, a way of drawing the icons and a look for
 * the widgets. Worn over your own home screen, so your apps and widgets stay where you put them.
 *
 * Every one is free: the designs and colour sets are ones anybody has, and none of them moves (a
 * design that moves is set through Android's own live wallpaper screen). ConnectRulesTest holds
 * them to that.
 */
object ConnectPicks {

    val all: List<ConnectSetup> = listOf(
        pick(
            "liquid-night", "Liquid Night",
            "Liquid Glass after dark: an iris of violet light, and every icon in clear glass.",
            theme = "glass", appearance = "dark", wallpaper = "Iris", colours = "midnight", icons = "glass", widgets = "glass",
        ),
        pick(
            "northern-lights", "Northern Lights",
            "Aurora's coloured glass over the northern lights, greens running into teal.",
            theme = "aurora", appearance = "dark", wallpaper = "Aurora", colours = "aurora", icons = "original", widgets = "glass",
        ),
        pick(
            "morning-frost", "Morning Frost",
            "Frost in daylight: pale satin, ice-blue glass and widgets in their own colours.",
            theme = "frost", appearance = "light", wallpaper = "Satin", colours = "frost", icons = "original", widgets = "colour",
        ),
        pick(
            "deep-ocean", "Deep Ocean",
            "Blue all the way down: ripples on a dark pond, glass icons floating over it.",
            theme = "glass", appearance = "dark", wallpaper = "Pond", colours = "ocean", icons = "glass", widgets = "glass",
        ),
        pick(
            "golden-hour", "Golden Hour",
            "Dunes at sunset, amber into rose, with widgets as bright as the sky.",
            theme = "glass", appearance = "dark", wallpaper = "Dunes", colours = "sunset", icons = "original", widgets = "colour",
        ),
        pick(
            "paper", "Paper",
            "Clean and quiet: a soft lavender page, plain tiles, nothing that shines.",
            theme = "clean", appearance = "light", wallpaper = "Calm", colours = "lavender", icons = "tinted", widgets = "colour",
        ),
        pick(
            "graphite", "Graphite",
            "Pure monochrome on fluted glass. Nothing on the screen but what you need.",
            theme = "glass", appearance = "dark", wallpaper = "Fluted", colours = "graphite", icons = "tinted", widgets = "glass",
        ),
        pick(
            "berry-pop", "Berry Pop",
            "Glossy marbles in berry and plum, and widgets in every colour they have.",
            theme = "glass", appearance = "dark", wallpaper = "Marbles", colours = "berry", icons = "original", widgets = "colour",
        ),
        pick(
            "sakura", "Sakura",
            "Petals out of focus in pink and plum, under clear glass icons.",
            theme = "glass", appearance = "dark", wallpaper = "Bokeh", colours = "sakura", icons = "glass", widgets = "glass",
        ),
        pick(
            "forest-walk", "Forest Walk",
            "Pines in fresh green, Frost's glass and the widgets in colour.",
            theme = "frost", appearance = "dark", wallpaper = "Forest", colours = "lime", icons = "original", widgets = "colour",
        ),
    )

    private fun pick(
        id: String,
        name: String,
        about: String,
        theme: String,
        appearance: String,
        wallpaper: String,
        colours: String,
        icons: String,
        widgets: String,
    ): ConnectSetup {
        val settings = JSONObject()
            .put("theme", theme)
            .put("appearance", appearance)
            .put("wallpaperStyle", wallpaper)
            .put("wallpaperPalette", colours)
            .put("iconStyle", icons)
            .put("widgetLook", widgets)
            .toString()
        return ConnectSetup(
            id = "pick-$id",
            name = name,
            by = "Zenith",
            about = about,
            source = SetupSource.PICK,
            settings = settings,
            tags = ConnectApply.tagsOf(settings),
        )
    }
}

/** What came in one version of Zenith, for Connect's Updates. */
data class News(val version: String, val date: String, val title: String, val lines: List<String>)

/**
 * What is new, newest first. The first is the version this is (ConnectRulesTest checks it against
 * the build), so a new version of Zenith gets its lines here when its number goes up.
 */
object ConnectNews {
    val all: List<News> = listOf(
        News(
            "4.0.1", "21 Nov 2026", "Your wallpaper, every time",
            listOf(
                "Putting a setup on over a wallpaper that moves no longer leaves Hover's marbles behind: its own wallpaper goes on, and so does every setup's after it.",
                "Zenith's live wallpaper shows the design you chose wherever Android keeps it up, the lock screen too, moving or still.",
                "A setup's page shows that setup's own wallpaper again, not the one you already have on.",
            ),
        ),
        News(
            "4.0", "21 Nov 2026", "Faster, safer, and a cleaner Connect",
            listOf(
                "Zenith is made ready for your phone as it installs, so it runs at full speed from the first minute, not after days of use. It's 0.6 MB smaller too.",
                "A Look from a code, or a backup, is checked before any of it goes on: nothing in one can crash Zenith, rename your apps, or give anybody Plus.",
                "The PIN on your hidden apps is kept only as a hash, so not even a backup file shows it.",
                "Connect is tidied up: what a setup puts on sits clear of Apply, headings line up, and the foot of every tab fades cleanly under the tab bar.",
                "A Look that couldn't be shared no longer pushes your oldest saved Look out when you try again.",
            ),
        ),
        News(
            "3.9.5", "20 Nov 2026", "Every app goes back into its icon",
            listOf(
                "Closing an app plays every time now: apps opened from Search or a notification, apps left open for hours, and after Android closed Zenith in the background.",
                "An app that never covered the whole screen sends its card back into its icon as it goes.",
                "Settings, Motion, Advanced animation settings: under Wait for Android's close, Zenith says what the last close did.",
            ),
        ),
        News(
            "3.9.4", "20 Nov 2026", "Ten more wallpapers that move",
            listOf(
                "Lotus and Daybreak open as you unlock on a Galaxy now. One UI's word that the phone was unlocked never reached them before.",
                "Six more that drift on their own: Borealis, Petals, Kaleido, Flow, Jelly and Clouds.",
                "Four more that open as you unlock: Planet, Aperture, Gyro and Ink.",
            ),
        ),
        News(
            "3.9.3", "20 Nov 2026", "Ready for the next one",
            listOf("Put a setup on and go home to see it: next time Connect opens on the tab you were on, ready for the next one, not on the one you just put on."),
        ),
        News(
            "3.9.2", "19 Nov 2026", "Zenith Connect, for real",
            listOf(
                "Connect is Liquid Glass over your own wallpaper, the same glass as your home screen, in Lite too.",
                "Every preview is your real home screen: your apps and widgets, in the setup's theme and wallpaper.",
                "Apply puts a setup on Zenith, as much of it as you choose, and Undo takes it off again.",
                "Looks come from Zenith's server: the featured ones, any Look by its code, and yours to share.",
                "The Vault keeps your Looks and backs your home screen up to your account.",
            ),
        ),
        News(
            "3.9.1", "19 Nov 2026", "The drawer's dim, edge to edge",
            listOf("The dim behind the drawer and the App Library covers the whole screen again, in Lite too."),
        ),
        News(
            "3.9", "19 Nov 2026", "Make your own widget",
            listOf(
                "The widget maker: build a widget from scratch and put it on your home screen.",
                "Aurora and Frost, two glass themes, take over from Neon and Retro.",
                "Zenith Connect arrives, an app that lives inside Zenith.",
            ),
        ),
        News(
            "3.8", "18 Nov 2026", "Glass, folders and wallpapers that move",
            listOf(
                "Wallpapers that move, and eight more Depth designs.",
                "Folders like iOS 26's, poured out of their tile as liquid glass.",
                "Apps close into their icons, and Lite keeps slower phones smooth.",
                "The Zenith Notch, four themes and a new setup.",
            ),
        ),
    )
}
