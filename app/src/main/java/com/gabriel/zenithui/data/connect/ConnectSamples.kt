package com.gabriel.zenithui.data.connect

/**
 * A glow of colour in a wallpaper: an ellipse [rx] wide and [ry] tall (shares of the wallpaper's
 * width and height) about ([x], [y]), at full strength in the middle and gone by [reach] of the
 * way out, as a CSS radial gradient draws one.
 */
data class WallGlow(val colour: Long, val x: Float, val y: Float, val rx: Float, val ry: Float, val reach: Float)

/** A setup's wallpaper: a colour, or two from top to bottom, with glows over it, the first on top. */
data class ConnectWall(val id: String, val base: List<Long>, val glows: List<WallGlow>)

object ConnectWalls {

    val all: List<ConnectWall> = listOf(
        ConnectWall("nightfall", listOf(0xFF100D2A), listOf(WallGlow(0xFF7B5CFF, 0.88f, 0f, 1.2f, 0.7f, 0.62f), WallGlow(0xFF149DB3, 0f, 1f, 1.1f, 0.7f, 0.58f))),
        ConnectWall("kyoto", listOf(0xFF1A1030), listOf(WallGlow(0xFFFF7FA6, 0.12f, 0f, 0.9f, 0.6f, 0.62f), WallGlow(0xFF4130D9, 1f, 1f, 0.9f, 0.7f, 0.6f))),
        ConnectWall("solar", listOf(0xFF1F0C0E), listOf(WallGlow(0xFFFFB24D, 0.22f, 0.12f, 0.9f, 0.7f, 0.58f), WallGlow(0xFFD9346E, 1f, 1f, 1f, 0.8f, 0.62f))),
        ConnectWall("arcade", listOf(0xFF08061A), listOf(WallGlow(0xFF00D9FF, 0f, 0f, 0.7f, 0.5f, 0.6f), WallGlow(0xFFFF2BD1, 1f, 1f, 0.8f, 0.6f, 0.6f))),
        ConnectWall("glyph", listOf(0xFF0E1224), listOf(WallGlow(0xFF3C6CFF, 1f, 0f, 1f, 0.8f, 0.62f), WallGlow(0xFF2A1E5C, 0f, 1f, 0.9f, 0.7f, 0.6f))),
        ConnectWall("moss", listOf(0xFF0D1712), listOf(WallGlow(0xFF2F9A62, 0.5f, 1.15f, 1.1f, 0.8f, 0.62f), WallGlow(0xFFBFEA9C, 0f, 0f, 0.8f, 0.6f, 0.55f))),
        ConnectWall("paper", listOf(0xFFEFECE6, 0xFFD8D2C6), listOf(WallGlow(0xFFFFFFFF, 0.8f, 0.08f, 0.9f, 0.6f, 0.6f))),
        ConnectWall("monolith", listOf(0xFFECEAE5, 0xFFCFCBC3), emptyList()),
        ConnectWall("afterglow", listOf(0xFF1A0F24), listOf(WallGlow(0xFFFF8A5B, 0.5f, 0f, 0.9f, 0.6f, 0.6f), WallGlow(0xFF5B3BD9, 0.5f, 1f, 1f, 0.7f, 0.6f))),
    )

    fun byId(id: String): ConnectWall? = all.firstOrNull { it.id == id }
}

/** Someone who posts in Community. [role] is Dev, Mod, or empty. */
data class ConnectPerson(val id: String, val name: String, val initial: String, val bg: Long, val fg: Long, val role: String)

/** Community's channels, as the Zenith Discord has them. */
enum class Channel(val title: String) {
    ANNOUNCEMENTS("announcements"),
    UPDATE_LOG("update-log"),
    SHOWCASE("showcase"),
    FEEDBACK("feedback"),
}

/** A post in a channel. Only what it carries is set: words, a picture of a setup, a build, a changelog, or a status to vote on. */
data class ConnectMessage(
    val id: String,
    val channel: Channel,
    val who: String,
    val time: String,
    val text: String = "",
    /** A setup's wallpaper shown as a phone, and the time on its clock. */
    val image: String? = null,
    val clock: String = "",
    /** A link to the beta's latest build. */
    val build: Boolean = false,
    val version: String = "",
    /** New or Fix, and what. */
    val changes: List<Pair<String, String>> = emptyList(),
    /** The setup it shows, to open in Discover. */
    val link: String? = null,
    /** For feedback: Planned, In progress or Exploring, and how many voted for it. */
    val status: String = "",
    val votes: Int = 0,
    val likes: Int = 0,
    val replies: Int = 0,
)

data class RoadmapItem(val id: String, val title: String, val status: String, val votes: Int, val mine: Boolean)

data class VaultLayout(val id: String, val name: String, val meta: String, val wall: String, val dark: Boolean)

/** A colour Connect can wear for its buttons and highlights; null for Zenith's own blue. */
data class ConnectAccent(val name: String, val colour: Long?)

/**
 * What Connect shows until it has a server to ask: the setups, people, posts, build and layouts
 * from Gabriel's Zenith Connect prototype, as they were there.
 */
object ConnectSamples {

    val setups: List<ConnectSetup> = listOf(
        ConnectSetup("nightfall", "Nightfall Glass", "lumen", 4.9f, 18_200, 99, 9, SetupKind.SETUP, "21:08", 0.26f, true,
            listOf(0xFF7B5CFF, 0xFFBDAFFF, 0xFF149DB3, 0xFF100D2A, 0xFFF6F6F9), 3, 6, "Glassline", "glass dark purple amoled", "nightfall"),
        ConnectSetup("kyoto", "Kyoto Rain", "mika.set", 4.8f, 9_600, 95, 4, SetupKind.SETUP, "06:40", 0.5f, true,
            listOf(0xFFFF7FA6, 0xFF4130D9, 0xFF1A1030, 0xFFF3C6D6, 0xFFF6F6F9), 2, 4, "Round Drop", "pink rain anime cozy", "kyoto"),
        ConnectSetup("solar", "Solar Drift", "sunny.px", 4.5f, 5_100, 92, 2, SetupKind.WIDGETS, "18:55", 0.26f, true,
            listOf(0xFFFFB24D, 0xFFD9346E, 0xFF1F0C0E, 0xFFFFD9A8, 0xFFF6F6F9), 1, 3, "", "warm sunset orange", "solar"),
        ConnectSetup("arcade", "Arcade Neon", "byte.club", 4.7f, 7_300, 90, 7, SetupKind.SETUP, "23:59", 0.13f, true,
            listOf(0xFF00D9FF, 0xFFFF2BD1, 0xFF08061A, 0xFF7A5CFF, 0xFFF6F6F9), 2, 5, "Pixel Arc", "neon gaming cyber amoled", "arcade"),
        ConnectSetup("glyph", "Glyphline", "glyphsmith", 4.9f, 21_400, 86, 20, SetupKind.ICONS, "", 0.3f, true,
            listOf(0xFF3C6CFF, 0xFF2A1E5C, 0xFF0E1224, 0xFFA9BCFF, 0xFFF6F6F9), 1, 0, "1,240 icons", "blue line minimal", "glyph"),
        ConnectSetup("moss", "Moss & Mono", "fernwork", 4.7f, 3_900, 80, 12, SetupKind.WIDGETS, "09:12", 0.26f, true,
            listOf(0xFF2F9A62, 0xFFBFEA9C, 0xFF0D1712, 0xFF6FC48F, 0xFFF6F6F9), 1, 4, "", "green nature calm", "moss"),
        ConnectSetup("paper", "Paper Minimal", "quietdesk", 4.6f, 2_200, 72, 1, SetupKind.SETUP, "10:24", 0.2f, false,
            listOf(0xFFEFECE6, 0xFFD8D2C6, 0xFF16161E, 0xFF8C8579, 0xFFFFFFFF), 1, 3, "Ink Line", "light minimal clean", "paper"),
        ConnectSetup("monolith", "Monolith", "stoneui", 4.8f, 6_800, 66, 3, SetupKind.ICONS, "", 0.5f, false,
            listOf(0xFFECEAE5, 0xFFCFCBC3, 0xFF1B1B22, 0xFFEDEBE6, 0xFF6D6A64), 1, 0, "860 icons", "minimal round mono", "monolith"),
    )

    val people: Map<String, ConnectPerson> = listOf(
        ConnectPerson("team", "Zenith Team", "Z", 0xFFF6F6F9, 0xFF0B0C12, "Dev"),
        ConnectPerson("nova", "nova", "N", 0xFF3A2F6B, 0xFFF6F6F9, "Mod"),
        ConnectPerson("mika", "mika.set", "M", 0xFF6B2F4E, 0xFFF6F6F9, ""),
        ConnectPerson("byte", "byte.club", "B", 0xFF1F4E6B, 0xFFF6F6F9, ""),
        ConnectPerson("fern", "fernwork", "F", 0xFF2F5B3E, 0xFFF6F6F9, ""),
        ConnectPerson("quiet", "quietdesk", "Q", 0xFF5B4A2F, 0xFFF6F6F9, ""),
    ).associateBy { it.id }

    val messages: List<ConnectMessage> = listOf(
        ConnectMessage("a1", Channel.ANNOUNCEMENTS, "team", "2h",
            text = "Zenith Connect is rolling out to beta testers today. Anything you apply here lands in Zenith in one tap.",
            image = "nightfall", clock = "21:08", likes = 214, replies = 36),
        ConnectMessage("a2", Channel.ANNOUNCEMENTS, "team", "1d",
            text = "Build 0.9.4 is up in the Beta Hub. Widget stacks are in, go break them.", build = true, likes = 158, replies = 41),
        ConnectMessage("a3", Channel.ANNOUNCEMENTS, "nova", "2d",
            text = "Post your setups in #showcase. Next week’s spotlight gets picked from there.", likes = 62, replies = 9),
        ConnectMessage("u1", Channel.UPDATE_LOG, "team", "1d", version = "0.9.4-beta.2",
            changes = listOf("New" to "Widget stacks you can swipe through", "New" to "Deep links from Zenith Connect", "Fix" to "Dock icons jumping after rotation"),
            likes = 88, replies = 14),
        ConnectMessage("u2", Channel.UPDATE_LOG, "team", "6d", version = "0.9.3-beta.1",
            changes = listOf("New" to "Icon pack picker with live preview", "New" to "Faster app drawer search", "Fix" to "Clock widget freezing overnight"),
            likes = 73, replies = 11),
        ConnectMessage("s1", Channel.SHOWCASE, "mika", "5h",
            text = "Kyoto Rain v2 is up. The rain widget now follows the real weather.", image = "kyoto", clock = "06:40", link = "kyoto", likes = 97, replies = 12),
        ConnectMessage("s2", Channel.SHOWCASE, "byte", "9h",
            text = "Arcade Neon, but make it AMOLED.", image = "arcade", clock = "23:59", link = "arcade", likes = 64, replies = 7),
        ConnectMessage("f1", Channel.FEEDBACK, "fern", "3h",
            text = "Could we get per-app icon shapes? Some packs look off on round icons.", status = "Planned", votes = 412, replies = 23),
        ConnectMessage("f2", Channel.FEEDBACK, "quiet", "1d",
            text = "A soft haptic tick when a widget snaps into place.", status = "Exploring", votes = 188, replies = 8),
    )

    /** The post that comes in live a few seconds after Community is first opened, as the Discord would bring one. */
    val welcome = ConnectMessage("live", Channel.ANNOUNCEMENTS, "nova", "now",
        text = "Welcome to everyone joining from today’s beta wave! Show off your setups in #showcase.", likes = 3, replies = 0)

    /** How long after Community is first opened that post comes in, in milliseconds. */
    const val WELCOME_AFTER_MS = 5_200L

    val roadmap: List<RoadmapItem> = listOf(
        RoadmapItem("shapes", "Per-app icon shapes", "Planned", 412, mine = true),
        RoadmapItem("gestures", "Gesture shortcuts", "In progress", 268, mine = false),
        RoadmapItem("lock", "Lock-screen widgets", "Exploring", 190, mine = false),
    )

    val layouts: List<VaultLayout> = listOf(
        VaultLayout("daily", "Daily driver", "4 pages · 18 apps", "nightfall", dark = true),
        VaultLayout("night", "Night mode", "2 pages · 11 apps", "arcade", dark = true),
        VaultLayout("focus", "Focus", "1 page · 6 apps", "paper", dark = false),
    )

    /** The beta's latest build, as the Beta Hub shows it. */
    const val BUILD_VERSION = "0.9.4"
    const val BUILD_TAG = "beta.2"
    const val BUILD_FILE = "zenith-0.9.4-beta.2.apk"
    const val BUILD_META = "APK · 38.6 MB · Android 10+"
    val BUILD_NOTES = listOf("Widget stacks you can swipe through", "Deep links from Zenith Connect")

    /** Connect's colours: Zenith's own blue first, then the prototype's four. */
    val accents: List<ConnectAccent> = listOf(
        ConnectAccent("Zenith", null),
        ConnectAccent("Lilac", 0xFFBDAFFF),
        ConnectAccent("Mint", 0xFF6BE1BB),
        ConnectAccent("Ice", 0xFF80C7F8),
        ConnectAccent("Coral", 0xFFFFA189),
    )
}
