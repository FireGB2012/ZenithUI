package com.gabriel.zenithui.data.connect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ConnectRulesTest {

    private val all = ConnectSamples.setups

    private fun ids(list: List<ConnectSetup>) = list.map { it.id }

    @Test
    fun `this week leaves out the setup of the week, which has the big card, and leads with what is trending`() {
        val week = ConnectFeed.list(all, FeedTab.WEEK, "")
        assertFalse(ConnectFeed.SPOTLIGHT in ids(week))
        assertEquals(listOf("kyoto", "solar", "arcade", "glyph", "moss", "paper", "monolith"), ids(week))
    }

    @Test
    fun `trending has everything by how fast it is going`() {
        assertEquals(
            listOf("nightfall", "kyoto", "solar", "arcade", "glyph", "moss", "paper", "monolith"),
            ids(ConnectFeed.list(all, FeedTab.TRENDING, "")),
        )
    }

    @Test
    fun `top rated goes by rating, and by trend between two rated the same`() {
        // Nightfall and Glyphline are both 4.9: Nightfall is trending harder.
        assertEquals(
            listOf("nightfall", "glyph", "kyoto", "monolith", "arcade", "moss", "paper", "solar"),
            ids(ConnectFeed.list(all, FeedTab.TOP, "")),
        )
        // One with no rating yet comes after every rated one.
        val fresh = all.first().copy(id = "fresh", rating = null, trend = 100)
        assertEquals("fresh", ConnectFeed.list(all + fresh, FeedTab.TOP, "").last().id)
    }

    @Test
    fun `new releases go newest first`() {
        assertEquals(
            listOf("paper", "solar", "monolith", "kyoto", "arcade", "nightfall", "moss", "glyph"),
            ids(ConnectFeed.list(all, FeedTab.NEW, "")),
        )
    }

    @Test
    fun `a search looks at the name, the creator, the kind and the tags, in any case`() {
        assertEquals(listOf("kyoto"), ids(ConnectFeed.list(all, FeedTab.WEEK, "  RAIN ")))
        assertEquals(listOf("arcade"), ids(ConnectFeed.list(all, FeedTab.WEEK, "byte")))
        assertEquals(listOf("glyph", "monolith"), ids(ConnectFeed.list(all, FeedTab.WEEK, "icon pack")))
        // The setup of the week is found like any other once you search.
        assertEquals(listOf("nightfall", "arcade"), ids(ConnectFeed.list(all, FeedTab.WEEK, "amoled")))
        // The order is still the tab's.
        assertEquals(listOf("paper", "monolith", "glyph"), ids(ConnectFeed.list(all, FeedTab.NEW, "minimal")))
        assertTrue(ConnectFeed.list(all, FeedTab.WEEK, "zzz").isEmpty())
    }

    @Test
    fun `the big card shows only on this week, with nothing searched`() {
        assertTrue(ConnectFeed.heroShown(FeedTab.WEEK, ""))
        assertTrue(ConnectFeed.heroShown(FeedTab.WEEK, "   "))
        assertFalse(ConnectFeed.heroShown(FeedTab.WEEK, "neon"))
        assertFalse(ConnectFeed.heroShown(FeedTab.TOP, ""))
    }

    @Test
    fun `each list says what it is`() {
        val day = LocalDate.of(2026, 11, 19)
        assertEquals("More this week" to "wk 47", ConnectFeed.heading(FeedTab.WEEK, "", 7, day))
        assertEquals("Trending now" to "live", ConnectFeed.heading(FeedTab.TRENDING, "", 8, day))
        assertEquals("Top rated" to "all time", ConnectFeed.heading(FeedTab.TOP, "", 8, day))
        assertEquals("New releases" to "last 7 days", ConnectFeed.heading(FeedTab.NEW, "", 8, day))
        assertEquals("Results" to "3 found", ConnectFeed.heading(FeedTab.TOP, "minimal", 3, day))
        assertEquals("Results" to "1 found", ConnectFeed.heading(FeedTab.TOP, "rain", 1, day))
        // The week of the year is ISO's: 1 January 2027 is still in 2026's week 53.
        assertEquals("wk 53", ConnectFeed.heading(FeedTab.WEEK, "", 7, LocalDate.of(2027, 1, 1)).second)
    }

    @Test
    fun `a card says its rating, or on new releases how new it is`() {
        val kyoto = all.first { it.id == "kyoto" }
        assertEquals("4.8", ConnectFeed.stat(kyoto, FeedTab.WEEK))
        assertEquals("4d ago", ConnectFeed.stat(kyoto, FeedTab.NEW))
        assertEquals("just now", ConnectFeed.stat(kyoto.copy(ageDays = 0), FeedTab.NEW))
        assertEquals("New", ConnectFeed.stat(kyoto.copy(rating = null), FeedTab.TOP))
        assertTrue(ConnectFeed.starred(kyoto, FeedTab.TOP))
        assertFalse(ConnectFeed.starred(kyoto, FeedTab.NEW))
        assertFalse(ConnectFeed.starred(kyoto.copy(rating = null), FeedTab.TOP))
    }

    @Test
    fun `big numbers are shortened the way the cards show them`() {
        assertEquals("18.2k", ConnectFeed.count(18_200))
        assertEquals("2.2k", ConnectFeed.count(2_240))
        assertEquals("10k", ConnectFeed.count(10_000))
        assertEquals("980", ConnectFeed.count(980))
        assertEquals("0", ConnectFeed.count(0))
        assertEquals("1.2M", ConnectFeed.count(1_200_000))
    }

    @Test
    fun `each kind's preview is as tall as the prototype's`() {
        assertEquals(244, ConnectFeed.previewHeight(SetupKind.SETUP))
        assertEquals(150, ConnectFeed.previewHeight(SetupKind.WIDGETS))
        assertEquals(160, ConnectFeed.previewHeight(SetupKind.ICONS))
    }

    @Test
    fun `what a setup can put on depends on what it is`() {
        val setup = all.first { it.kind == SetupKind.SETUP }
        val widgets = all.first { it.kind == SetupKind.WIDGETS }
        val icons = all.first { it.kind == SetupKind.ICONS }
        assertEquals(setOf(Layer.WALLPAPER, Layer.WIDGETS, Layer.ICONS), ConnectApply.available(setup))
        assertEquals(setOf(Layer.WIDGETS), ConnectApply.available(widgets))
        assertEquals(setOf(Layer.ICONS), ConnectApply.available(icons))
        assertEquals("Wallpaper", ConnectApply.label(Layer.WALLPAPER, setup))
        assertEquals("6 widgets", ConnectApply.label(Layer.WIDGETS, setup))
        assertEquals("Icons", ConnectApply.label(Layer.ICONS, setup))
        assertEquals("Icon pack", ConnectApply.label(Layer.ICONS, icons))
    }

    @Test
    fun `apply needs at least one layer it can put on`() {
        val icons = all.first { it.kind == SetupKind.ICONS }
        assertTrue(ConnectApply.canApply(icons, setOf(Layer.ICONS)))
        // Wallpaper is on, but an icon pack has none to give.
        assertFalse(ConnectApply.canApply(icons, setOf(Layer.WALLPAPER)))
        assertFalse(ConnectApply.canApply(icons, emptySet()))
    }

    @Test
    fun `the apply screen lists what is coming, in order`() {
        val nightfall = all.first { it.id == "nightfall" }
        val glyph = all.first { it.id == "glyph" }
        assertEquals("Wallpaper · 6 widgets · Glassline icons", ConnectApply.assets(nightfall, Layer.entries.toSet()))
        assertEquals("Wallpaper · Glassline icons", ConnectApply.assets(nightfall, setOf(Layer.ICONS, Layer.WALLPAPER)))
        assertEquals("1,240 icons", ConnectApply.assets(glyph, Layer.entries.toSet()))
    }

    @Test
    fun `each kind is put on in its own three steps`() {
        assertEquals(
            listOf("Pulling assets" to "Assets pulled", "Applying colors" to "Colors applied", "Arranging layout" to "Layout arranged"),
            ConnectApply.steps(SetupKind.SETUP),
        )
        assertEquals("Placing widgets" to "Widgets placed", ConnectApply.steps(SetupKind.WIDGETS)[2])
        assertEquals("Swapping icons" to "Icons swapped", ConnectApply.steps(SetupKind.ICONS)[2])
        ConnectApply.steps(SetupKind.ICONS).let { assertEquals(3, it.size) }
    }

    @Test
    fun `the progress line counts through what is being built`() {
        val nightfall = all.first { it.id == "nightfall" }
        assertEquals(listOf("Page 1 of 3", "Page 2 of 3", "Page 3 of 3", "Dock and app drawer"), ConnectApply.progress(nightfall))
        val moss = all.first { it.id == "moss" }
        assertEquals(listOf("Widget 1 of 4", "Widget 2 of 4", "Widget 3 of 4", "Widget 4 of 4"), ConnectApply.progress(moss))
        assertEquals(listOf("Home screen", "Dock", "App drawer"), ConnectApply.progress(all.first { it.id == "glyph" }))
    }

    @Test
    fun `a setup's first line says what kind it is and how much of it there is`() {
        assertEquals("Full setup · 3 pages", ConnectApply.kindLine(all.first { it.id == "nightfall" }))
        assertEquals("Full setup · 1 page", ConnectApply.kindLine(all.first { it.id == "paper" }))
        assertEquals("Widget preset · 4 widgets", ConnectApply.kindLine(all.first { it.id == "moss" }))
        assertEquals("Icon pack · 1,240 icons", ConnectApply.kindLine(all.first { it.id == "glyph" }))
    }

    @Test
    fun `a setup's link is its name, lowercased and joined with dashes`() {
        assertEquals("zenith://setup/nightfall-glass", ConnectApply.link("Nightfall Glass"))
        // An ampersand is read out, as the prototype's links have it.
        assertEquals("zenith://setup/moss-and-mono", ConnectApply.link("Moss & Mono"))
        assertEquals("zenith://setup/setup", ConnectApply.link("  !!  "))
    }

    @Test
    fun `publishing needs a name, and up to five tags`() {
        assertEquals("Give your setup a name first.", ConnectStudio.nameProblem("   "))
        assertNull(ConnectStudio.nameProblem("Afterglow"))
        val three = listOf("Minimal", "Dark", "Glass")
        assertEquals(three + "Cozy", ConnectStudio.toggleTag(three, "Cozy"))
        assertEquals(listOf("Minimal", "Glass"), ConnectStudio.toggleTag(three, "Dark"))
        val five = three + "Cozy" + "Anime"
        // The sixth is refused: the list stays as it was.
        assertNull(ConnectStudio.toggleTag(five, "Gaming"))
        // Taking one off a full list is always allowed.
        assertEquals(five - "Cozy", ConnectStudio.toggleTag(five, "Cozy"))
    }

    @Test
    fun `a published setup leads New releases, as yours`() {
        val mine = ConnectStudio.published("Afterglow", by = "gabriel", clock = "21:30", tags = listOf("Minimal", "Dark"), id = "mine-1")
        assertEquals("gabriel", mine.by)
        assertEquals(0, mine.ageDays)
        assertNull(mine.rating)
        assertEquals(SetupKind.SETUP, mine.kind)
        assertEquals("afterglow", mine.wall)
        assertEquals("mine-1", ConnectFeed.list(listOf(mine) + all, FeedTab.NEW, "").first().id)
        // Found by its name, like any other.
        assertEquals(listOf("mine-1"), ids(ConnectFeed.list(listOf(mine) + all, FeedTab.NEW, "afterglow")))
    }

    @Test
    fun `a handle is the name you gave Zenith, made into one word`() {
        assertEquals("gabriel", ConnectStudio.handle("Gabriel"))
        assertEquals("mary.jane", ConnectStudio.handle("  Mary Jane "))
        assertEquals("you", ConnectStudio.handle(""))
        assertEquals("G", ConnectStudio.initial("gabriel"))
        assertEquals("Y", ConnectStudio.initial(""))
    }

    @Test
    fun `every sample setup has what its card and its apply screen need`() {
        all.forEach { s ->
            assertTrue(s.id, s.palette.size == 5)
            assertTrue(s.id, ConnectWalls.byId(s.wall) != null)
            assertTrue(s.id, s.kind != SetupKind.SETUP || s.pages > 0)
        }
        assertEquals(1, all.count { it.id == ConnectFeed.SPOTLIGHT })
    }

    @Test
    fun `the apply screen runs its three steps on the prototype's clock`() {
        val nightfall = all.first { it.id == "nightfall" }
        val w = StepState.WAITING
        val r = StepState.RUNNING
        val d = StepState.DONE
        ConnectApply.stage(0, nightfall).let {
            assertEquals(listOf(w, w, w), it.steps)
            assertEquals(0, it.swatches)
            assertEquals(0f, it.bar, 0f)
            assertEquals("Waiting", it.line)
            assertFalse(it.done)
        }
        assertEquals(listOf(r, w, w), ConnectApply.stage(450, nightfall).steps)
        assertEquals(listOf(d, r, w), ConnectApply.stage(1_400, nightfall).steps)
        // The five colours come in one by one, 110 ms apart, once the colours step starts.
        assertEquals(0, ConnectApply.stage(1_509, nightfall).swatches)
        assertEquals(1, ConnectApply.stage(1_510, nightfall).swatches)
        assertEquals(5, ConnectApply.stage(1_950, nightfall).swatches)
        ConnectApply.stage(2_350, nightfall).let {
            assertEquals(listOf(d, d, r), it.steps)
            assertEquals("Page 1 of 3", it.line)
        }
        // Four lines over the bar's 1.5 s: 375 ms each.
        assertEquals("Page 2 of 3", ConnectApply.stage(2_725, nightfall).line)
        assertEquals("Dock and app drawer", ConnectApply.stage(3_849, nightfall).line)
        assertEquals(0.5f, ConnectApply.stage(3_100, nightfall).bar, 0.001f)
        assertEquals(1f, ConnectApply.stage(3_900, nightfall).bar, 0f)
        ConnectApply.stage(3_950, nightfall).let {
            assertEquals(listOf(d, d, d), it.steps)
            assertTrue(it.done)
            assertEquals("Done", it.line)
            assertEquals(5, it.swatches)
        }
        // Long after, as when Connect was left and opened again: still done.
        assertTrue(ConnectApply.stage(60_000, nightfall).done)
    }

    @Test
    fun `the sync card says how the vault is kept`() {
        assertEquals(
            "Everything is backed up" to "Synced 2 min ago · 3 layouts, 12 themes",
            ConnectVault.syncCopy(auto = true, byHand = false, lastSync = "2 min ago", layouts = 3),
        )
        assertEquals(
            "Synced just now" to "Auto-sync is off. Tap the cloud to sync again.",
            ConnectVault.syncCopy(auto = false, byHand = true, lastSync = "just now", layouts = 3),
        )
        assertEquals(
            "Auto-sync is off" to "Changes stay on this phone until you sync.",
            ConnectVault.syncCopy(auto = false, byHand = false, lastSync = "2 min ago", layouts = 4),
        )
    }

    @Test
    fun `a backup is named for when it was made and keeps the pages of the layout in use`() {
        assertEquals("Backup 19 Nov, 21:08", ConnectVault.backupName(LocalDateTime.of(2026, 11, 19, 21, 8)))
        assertEquals("Backup 5 Jan, 09:03", ConnectVault.backupName(LocalDateTime.of(2027, 1, 5, 9, 3)))
        assertEquals("4 pages · just now", ConnectVault.backupMeta("4 pages · 18 apps"))
        assertEquals("1 page · just now", ConnectVault.backupMeta("1 page"))
    }

    @Test
    fun `the download button says how far it has got`() {
        assertEquals("Download APK", ConnectBeta.downloadLabel(DownloadState.IDLE, 0f))
        assertEquals("Downloading… 42%", ConnectBeta.downloadLabel(DownloadState.BUSY, 0.424f))
        assertEquals("Downloading… 100%", ConnectBeta.downloadLabel(DownloadState.BUSY, 1.2f))
        assertEquals("Ready to install", ConnectBeta.downloadLabel(DownloadState.DONE, 1f))
        assertEquals("SM-A556B · Android 15", ConnectBeta.deviceLine("SM-A556B", "15"))
        assertEquals("Phone · Android 15", ConnectBeta.deviceLine("  ", "15"))
    }

    @Test
    fun `the publish button says what it is doing, for a public setup or a link`() {
        assertEquals("Publish to Discover", ConnectStudio.cta(public = true, phase = DownloadState.IDLE))
        assertEquals("Create share link", ConnectStudio.cta(public = false, phase = DownloadState.IDLE))
        assertEquals("Publishing…", ConnectStudio.cta(public = true, phase = DownloadState.BUSY))
        assertEquals("Creating link…", ConnectStudio.cta(public = false, phase = DownloadState.BUSY))
        assertEquals("Published", ConnectStudio.cta(public = true, phase = DownloadState.DONE))
        assertEquals("Link ready", ConnectStudio.cta(public = false, phase = DownloadState.DONE))
    }

    @Test
    fun `a like or a vote goes on and off, one at a time`() {
        assertEquals(true to 215, ConnectCommunity.flip(on = false, count = 214))
        assertEquals(false to 214, ConnectCommunity.flip(on = true, count = 215))
        assertEquals(false to 0, ConnectCommunity.flip(on = true, count = 0))
        assertEquals("1 reply", ConnectCommunity.replies(1))
        assertEquals("0 replies", ConnectCommunity.replies(0))
        assertEquals("36 replies", ConnectCommunity.replies(36))
    }

    @Test
    fun `the live post lands where you are looking, or leaves a dot where you are not`() {
        // Reading announcements: it comes into the list, with no dot anywhere.
        assertEquals(false to false, ConnectCommunity.dotsForLivePost(onCommunity = true, channel = Channel.ANNOUNCEMENTS))
        // In another channel: a dot on announcements.
        assertEquals(true to false, ConnectCommunity.dotsForLivePost(onCommunity = true, channel = Channel.SHOWCASE))
        // Somewhere else in Connect, or with Connect closed: a dot on the tab too.
        assertEquals(true to true, ConnectCommunity.dotsForLivePost(onCommunity = false, channel = Channel.ANNOUNCEMENTS))
    }
}
