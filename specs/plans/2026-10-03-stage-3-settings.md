# Stage 3: Settings as a full page — implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task by task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Settings opens as a full-height page with a title that shrinks, a search box, eight groups that each say how they are set, pages that push from the side, and a live mini phone on the two pages that change how the home screen looks.

**Architecture:** A pure `SettingsCatalog` lists every option once (id, page, title, keywords) and answers searches; it is what the search box and the "pulse this row" hand-off run on. The controls spread across `SettingsSheet.kt` move to one kit, `ui/settings/SettingsKit.kt`. `SettingsScreen` is the page itself; it is a layer of the home screen under the sheets, so a sheet opened from Settings (wallpapers, Plus, aura, theme maker, hidden apps, help) closes back onto Settings rather than onto the home screen.

**Tech stack:** Kotlin 2.2, Jetpack Compose (BOM 2024.12.01), JUnit 4. One test-only dependency: `org.json:json`, so the settings JSON can be tested on the JVM.

Spec: `specs/2026-10-01-zenith-overhaul-design.md`, "Stage 3: Settings".

## Global Constraints

- minSdk 29, targetSdk 37, no new app dependencies (the test-only `org.json` is not one).
- Every setting that exists today is still reachable, and behaves as it does today.
- A new settings field is written to and read from the JSON with its default, so an old backup restores.
- `motion` and `pageEffect` are stored and shown in this stage; what they change arrives in stage 4. `haptics` works at once for every control in the kit.
- Nothing new runs per frame while Settings is closed, or while it is open and still.
- Build and test: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`, Git Bash, repo root. Hands-on checks on `Android10_test`; the finished build goes on `Pixel_10a` last.

## Review Focus

1. Back: a sheet over Settings closes first, then a page pops, then search clears, then Settings closes. The system back, the arrow and a swipe from the left edge agree.
2. Search: choosing a result opens the right page with that row in view and pulsing; a query with no match says so; clearing it returns to the groups.
3. Plus: Free placement, theme maker, hidden apps, back up and restore open the Plus sheet when Plus is not active, and that sheet closes back onto Settings.
4. Run setup again still asks twice, and closes Settings.
5. The mini phone is a picture: it takes no touches and starts no work of its own while the page is still.

## File structure

| File | Responsibility |
| --- | --- |
| `data/Settings.kt` | `motion`, `pageEffect`, `haptics` |
| `ui/settings/SettingsCatalog.kt` (new) | Pages, entries, search. Pure Kotlin. |
| `ui/settings/SettingsKit.kt` (new) | Group, divider, caption, nav row, action row, toggle, slider, choice row, palette grid, shape choice, header, the tick, the pulse |
| `ui/settings/SettingsScreen.kt` (new) | The page: title, search, groups, push and pop, back |
| `ui/settings/SettingsPages.kt` (new) | What is on each of the six pages |
| `ui/home/HomeScreen.kt` | Settings as its own layer under the sheets |
| `ZenithViewModel.kt` | `tidyUp(): Int` |
| `ui/home/SettingsSheet.kt` | Removed |

---

### Task 1: three new settings, and JSON that is tested

**Files:** `gradle/libs.versions.toml`, `app/build.gradle.kts`, `data/Settings.kt`. Test: `data/SettingsJsonTest.kt`.

**Interfaces — Produces:** `ZenithSettings.motion: String = "full"` (`full` | `reduced`), `.pageEffect: String = "depth"` (`depth` | `slide` | `cube` | `stack`), `.haptics: Boolean = true`.

- [ ] **Step 1:** add `testImplementation` of `org.json:json:20240303`. **Step 2: failing tests:** a settings object with every field changed from its default survives `fromJson(toJson())`; JSON holding only `{"onboarded":true,"paletteId":"sunset"}` reads with `motion == "full"`, `pageEffect == "depth"`, `haptics == true`, `wallpaperFollow == true`, `searchBar == false`; unknown values are kept as written (the reader does not validate). **Step 3:** run, fail (`Unresolved reference: motion`). **Step 4:** add the fields and their JSON. **Step 5:** run, pass; commit "Settings for motion, page swipe and haptics; the settings JSON is tested".

### Task 2: the catalog

**Files:** create `ui/settings/SettingsCatalog.kt`. Test: `ui/settings/SettingsCatalogTest.kt`.

**Interfaces — Produces:**

```kotlin
enum class SettingsPage(val title: String, val opensSheet: Boolean = false) {
    Theme("Wallpaper & theme"), Home("Home screen"), Motion("Motion & feel"), Apps("Apps"),
    Plus("Zenith Plus", opensSheet = true), Aura("Aura", opensSheet = true),
    Backup("Backup & reset"), About("About & help"),
}
data class SettingsEntry(val id: String, val page: SettingsPage, val title: String, val keywords: String = "")
object SettingsCatalog {
    val entries: List<SettingsEntry>
    fun on(page: SettingsPage): List<SettingsEntry>
    fun search(query: String): List<SettingsEntry>
}
```

Search: the query is split into words; an entry matches when every word starts a word of its title, its keywords or its page's title, ignoring case. Entries whose title matches come before those matched only by keyword or page; within each, catalog order. A blank query finds nothing.

- [ ] **Step 1: failing tests:** ids are unique; every page has at least one entry; searching an entry's own title puts it first; `"dark"` finds `appearance`; `"color"` and `"colour"` both find `palette`; `"vibrate"` finds `haptics`; `"zzz"` and `""` find nothing; a two-word query needs both words. **Step 2:** run, fail. **Step 3:** implement, one entry per option in the spec's table. **Step 4:** pass; commit "SettingsCatalog: every option once, and a search over them".

### Task 3: one kit

**Files:** create `ui/settings/SettingsKit.kt`; modify `ui/home/SettingsSheet.kt` (parts leave it), `ui/home/WallpaperSheet.kt`, `ui/home/ZenithWidgets.kt` and any other user of the parts (imports), `MainActivity.kt` (provide the haptics switch).

**Interfaces — Produces:** in package `com.gabriel.zenithui.ui.settings`: `Group`, `GroupDivider`, `Caption`, `Note`, `NavRow`, `ActionRow`, `ToggleRow`, `Toggle`, `PillSlider`, `ChoiceRow`, `PaletteGrid`, `ShapeChoice`, `SettingsHeader`, `LocalHapticsOn: ProvidableCompositionLocal<Boolean>`, `rememberTick(): () -> Unit`, `Modifier.pulseWhen(active: Boolean)`.

- [ ] **Step 1:** move the parts, unchanged in looks, and make each control tick through `rememberTick()` (nothing when haptics are off). `PaletteGrid` wraps onto as many lines as it needs. **Step 2:** `ZenithApp` provides `LocalHapticsOn` from `settings.haptics`. **Step 3:** unit tests and `assembleDebug` pass; the old sheet still works. Commit "One settings kit; controls tick only when haptics are on".

### Task 4: the page

**Files:** create `ui/settings/SettingsScreen.kt`, `ui/settings/SettingsPages.kt`; modify `ui/home/HomeScreen.kt`, `ZenithViewModel.kt`; delete `ui/home/SettingsSheet.kt`.

**Interfaces — Produces:**
- `ZenithViewModel.tidyUp(): Int` — settles the layout, returns how many items moved.
- `SettingsScreen(visible, settings, plus, preview: SettingsPreview, actions: SettingsActions, onDismiss)`, with `SettingsPreview(items, dock, appsByKey, wallpaper, screenWidth, screenHeight)` and `SettingsActions(change, wallpaper, themeMaker, hiddenApps, freeform, backup, restore, plus, aura, help, reset, tidy)`.

Behaviour:
- Slides up to full height over a scrim; the status bar area is padded, not drawn under. The title is large at the top of the scroll and shrinks into the bar as the page scrolls.
- The first page: a search box, then the eight groups, each a `NavRow` with a coloured tile and a line saying how it is set. Typing replaces the groups with results (title, and the page it is on); choosing one opens that page, brings the row into view and pulses it twice.
- Pages push from the right and pop to the right, on a spring. Back, the arrow, or a drag from the left edge pops.
- Theme and Home pages start with the mini phone (`PhonePreview`, about 230 dp tall), showing page one of the real layout with the settings as they are being changed.
- "Tidy up" reports "Everything was already in place" or "Moved N items" in the row itself.

- [ ] **Step 1:** `tidyUp`. **Step 2:** the screen and its pages. **Step 3:** `HomeScreen`: `settingsOpen` state, the layer, back order, every caller of the old sheet. **Step 4:** delete `SettingsSheet.kt`. **Step 5:** unit tests and `assembleDebug` pass. Commit "Settings is a full page: search, eight groups, a live preview".

### Task 5: check it on the emulators, bring the words up to date

- [ ] On `Android10_test`: open Settings from Customise; every group opens and pops (arrow, back, edge swipe); search "dark" → Wallpaper & theme with the row pulsing; change palette and grid and watch the mini phone; a Plus row opens the Plus sheet and closing it lands on Settings; Tidy up reports; Run setup again asks twice; idle frames are 0 with Settings open and still, and after closing it; no crash in logcat.
- [ ] `README.md` (Customisation) and `play/listing.md` ("Settings you can read"), inside Play's limits; unit tests and `assembleDebug`; commit; install on `Pixel_10a`; push.
