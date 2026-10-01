# Stage 2: wallpapers that follow the theme — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Changing palette or light/dark repaints the chosen Zenith wallpaper in the new colours and fades it in; there are 22 designs, each with a real light version.

**Architecture:** Every palette gains a light and a dark tone (`ZenithPalette.toned`), used by the interface and by the wallpaper colours alike. Settings remember which design is up. A `WallpaperDirector` owned by the view model watches the design, the palette in effect, light or dark and the screen size; when the picture that should be up differs from the one that is, it paints the new one, shows it as a layer behind the home screen while it fades in, and only then sets the system wallpaper.

**Tech Stack:** Kotlin 2.2, Jetpack Compose (BOM 2024.12.01), `android.graphics` Canvas for the art, JUnit 4 (JVM) and AndroidJUnit4 (on device, run with `am instrument`).

**Spec:** `specs/2026-10-01-zenith-overhaul-design.md`, section "Stage 2: wallpapers that follow the theme".

## Global Constraints

- minSdk 29, targetSdk 37, no new app dependencies.
- A wallpaper set in another app, or a photo, is never replaced: Zenith repaints only while its own kept copy is still the wallpaper on screen.
- The same design in the same colours is the same picture at any size and after any theme change: the seed comes from the design alone.
- A design already in use that needs Plus stays, and keeps following the theme, if Plus ends.
- Painting runs off the main thread. Nothing new runs per frame while the home screen is idle (check `dumpsys gfxinfo` after the stage).
- Build and test with `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` in Git Bash from the repo root. Never `connectedDebugAndroidTest`; on-device tests run with `adb shell am instrument`. Hands-on checks on `Android10_test`; the finished build goes on `Pixel_10a` last.
- Long scripted edits go in a file (commands over about 8,000 characters are cut off); sources mix CRLF and LF.

## Review Focus

1. Tapping quickly through several palettes: only the last one is painted and set; nothing queues up.
2. The user sets a wallpaper in another app, then changes theme: Zenith must not replace it, and `wallpaperStyle` is cleared.
3. Android refuses the wallpaper: the layer fades back out, nothing is recorded as applied, a toast says so.
4. The system switches to dark while Zenith is not showing: the wallpaper is set without a layer, and nothing is left on screen when the user comes home.
5. A light palette (Frost, or a custom light one) in dark, and a dark palette in light: both the interface and the wallpaper use the palette's own tone, not Midnight's or Frost's.

## File structure

| File | Responsibility |
| --- | --- |
| `ui/theme/Palette.kt` | `toned`, `wallColors`, `Palettes.forWallpaper` |
| `data/Settings.kt` | `isDark`, four wallpaper fields |
| `data/WallpaperFollow.kt` (new) | The key of the picture that should be up, and when to repaint. Pure. |
| `data/WallpaperArt.kt` | The engine: sizes, seed, shared brushes, grain |
| `data/WallDesigns.kt` (new) | The 22 designs |
| `data/GlassWallpaper.kt`, `data/Wallpaper.kt` | Which picture is applied; whether Zenith's copy is still up; showing a picture to the glass early |
| `WallpaperDirector.kt` (new, package root) | Follows the theme; the incoming layer's state |
| `ZenithViewModel.kt`, `MainActivity.kt` | Own and feed the director |
| `ui/home/HomeScreen.kt`, `ui/home/WallpaperSheet.kt`, `ui/home/SettingsSheet.kt` | The fading layer, the sheet, one switch |

Paths are under `app/src/main/java/com/gabriel/zenithui/`; JVM tests under `app/src/test/java/com/gabriel/zenithui/`; on-device tests under `app/src/androidTest/java/com/gabriel/zenithui/`.

---

### Task 1: a light and a dark tone for every palette

**Files:** Modify `ui/theme/Palette.kt`, `ui/theme/Theme.kt`, `data/Settings.kt`, `data/WallpaperArt.kt` (`WallColors`), `MainActivity.kt`, `ui/home/WallpaperSheet.kt` (drop its private `wallColors`). Test: `ui/theme/PaletteToneTest.kt`, `data/AppearanceTest.kt`.

**Interfaces — Produces:**
- `fun ZenithSettings.isDark(systemDark: Boolean): Boolean`
- `fun ZenithPalette.toned(dark: Boolean): ZenithPalette`
- `data class WallColors(accent: Int, accentAlt: Int, deep: Int, mid: Int, base: Int, dark: Boolean = true)`
- `fun ZenithPalette.wallColors(): WallColors`

- [ ] **Step 1: failing tests.** `PaletteToneTest`: toning a palette to its own appearance returns the same object; for every palette in `Palettes.all + Palettes.aura`, `toned(false).backdrop` all have `luminance() > 0.7f` and `toned(true).backdrop` all `< 0.12f`, accents unchanged, `dark` set; `Palettes.Sunset.toned(false).backdrop != Palettes.Aurora.toned(false).backdrop` and neither equals `Palettes.Frost.backdrop`; `wallColors()` carries accent, accentAlt, the three backdrop colours and `dark`. `AppearanceTest`: `isDark` is false for `light`, true for `dark`, and the system's for `auto` or anything else.
- [ ] **Step 2: run, see them fail** (`Unresolved reference: toned`, `isDark`).
- [ ] **Step 3: implement.**

```kotlin
// data/Settings.kt, inside ZenithSettings
    /** Whether the launcher is dark right now: what was asked for, or with "auto" what the phone is. */
    fun isDark(systemDark: Boolean): Boolean = when (appearance) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }
```

```kotlin
// ui/theme/Palette.kt
/**
 * This palette in light or in dark. Its accents stay. When the tone asked for is not the palette's
 * own, the backdrop is made from those accents — a near-white or near-black base with a little of
 * each mixed in — rather than borrowed from another palette, so the interface and a wallpaper
 * painted from it still belong to this one.
 */
fun ZenithPalette.toned(dark: Boolean): ZenithPalette {
    if (dark == this.dark) return this
    val backdrop = if (dark) {
        listOf(lerp(Color(0xFF07080D), accent, 0.07f), lerp(Color(0xFF0C0F1A), accentAlt, 0.16f), lerp(Color(0xFF06070B), accent, 0.05f))
    } else {
        listOf(lerp(Color.White, accent, 0.10f), lerp(Color.White, accentAlt, 0.18f), lerp(Color.White, accent, 0.05f))
    }
    return copy(dark = dark, backdrop = backdrop)
}

/** The five colours a wallpaper is painted from, and whether it is the light or the dark picture. */
fun ZenithPalette.wallColors(): WallColors = WallColors(
    accent = accent.toArgb(),
    accentAlt = accentAlt.toArgb(),
    deep = backdrop.first().toArgb(),
    mid = backdrop.getOrElse(1) { backdrop.first() }.toArgb(),
    base = backdrop.last().toArgb(),
    dark = dark,
)
```

`ZenithTheme`: `val dark = settings.isDark(isSystemInDarkTheme())` and `val target = base.toned(dark)`. `ZenithApp`: `val lightBars = !settings.isDark(isSystemInDarkTheme())`. `WallpaperSheet` uses the shared `wallColors()`.

- [ ] **Step 4: run all unit tests and `assembleDebug`, see them pass. Step 5: commit** "A light and a dark tone for every palette".

---

### Task 2: what Zenith remembers, and when it repaints

**Files:** Modify `data/Settings.kt`, `ui/theme/Palette.kt`. Create `data/WallpaperFollow.kt`. Test: `data/WallpaperFollowTest.kt`, more cases in `ui/theme/PaletteToneTest.kt`.

**Interfaces — Produces:**
- `ZenithSettings.wallpaperStyle: String = ""`, `.wallpaperPalette: String = ""`, `.wallpaperLock: Boolean = false`, `.wallpaperFollow: Boolean = true` (written and read in the settings JSON)
- `WallpaperFollow.key(style: String, colors: WallColors, width: Int, height: Int): String`, `WallpaperFollow.shouldRepaint(settings, desired: String, applied: String?, ownWallpaperUp: Boolean): Boolean`, `WallpaperFollow.ART_VERSION`
- `Palettes.forWallpaper(settings: ZenithSettings, dark: Boolean): ZenithPalette`

- [ ] **Step 1: failing tests.** The key is equal for equal inputs and differs when the style, any colour, light or dark, the width or the height differs. `shouldRepaint` is true only when following is on, a style is set, Zenith's own wallpaper is still up and the keys differ: one test per condition turned off. `forWallpaper`: with `wallpaperPalette` empty it is the theme's palette toned; pinned to `"aurora"` it is Aurora toned whatever the theme; pinned to `custom` it is the custom palette.
- [ ] **Step 2: run, see them fail.**
- [ ] **Step 3: implement.**

```kotlin
// data/WallpaperFollow.kt
package com.gabriel.zenithui.data

/** Deciding when one of Zenith's own wallpapers has to be painted again. */
object WallpaperFollow {

    /** Bumped when the designs themselves change, so wallpapers already up are painted again. */
    const val ART_VERSION = 1

    /** The picture that should be on the wall, as a key that changes exactly when the picture would. */
    fun key(style: String, colors: WallColors, width: Int, height: Int): String = listOf(
        style, colors.accent, colors.accentAlt, colors.deep, colors.mid, colors.base,
        if (colors.dark) "dark" else "light", width, height, ART_VERSION,
    ).joinToString("|")

    /**
     * Whether to repaint without being asked: only while following is on, one of Zenith's designs
     * is the wallpaper, Zenith's kept copy is still what is on screen (so a wallpaper chosen
     * anywhere else is never replaced), and the picture wanted is not the one already up.
     */
    fun shouldRepaint(settings: ZenithSettings, desired: String, applied: String?, ownWallpaperUp: Boolean): Boolean =
        settings.wallpaperFollow && settings.wallpaperStyle.isNotEmpty() && ownWallpaperUp && desired != applied
}
```

```kotlin
// ui/theme/Palette.kt, inside Palettes
    /** The palette a Zenith wallpaper is painted in: the one it was pinned to, or the theme's, in the tone in use. */
    fun forWallpaper(settings: ZenithSettings, dark: Boolean): ZenithPalette = when (settings.wallpaperPalette) {
        "" -> resolve(settings)
        CUSTOM -> custom(settings)
        else -> byId(settings.wallpaperPalette)
    }.toned(dark)
```

- [ ] **Step 4: run, pass. Step 5: commit** "Zenith remembers its wallpaper and knows when to repaint it".

---

### Task 3: the art engine, and light versions of the eight designs

**Files:** Modify `data/WallpaperArt.kt`; create `data/WallDesigns.kt`; modify `ui/home/WallpaperSheet.kt` (new `render` signature). Test: `androidTest/.../data/WallpaperArtTest.kt` (new, on device).

**Interfaces — Produces:**
- `enum class WallGroup(val label: String) { Soft, Lines, Shapes, Scenes }`
- `enum class WallStyle(val label: String, val group: WallGroup, val plus: Boolean = false)` with `WallStyle.byName(name: String): WallStyle?`
- `WallpaperArt.render(style: WallStyle, colors: WallColors, width: Int, height: Int): Bitmap` (the seed is `style.name.hashCode()`; the `seed` parameter and `WallpaperArt.apply` go away, the director sets wallpapers)

- [ ] **Step 1: the on-device test first.** For every style, every palette in `Palettes.all + Palettes.aura`, light and dark, paint 270 by 586 without error. For each style in Midnight: the mean luminance of the light picture is at least 0.2 above the dark one; the lower 45% of the light picture has mean luminance at least 0.55 (dark labels read on it) and of the dark picture at most 0.40 (white labels read on it); the same style and colours painted twice are the same pixels; a full-size paint (1080 by 2400) takes under 1500 ms. It also writes two contact sheets, `walls-dark.png` and `walls-light.png` (every style in Midnight, 5 across), to the app's external cache directory, to be pulled and looked at.
- [ ] **Step 2: run it on `Android10_test`, see it fail** (the light pictures of the existing designs are dark, or the luminance checks fail).

```bash
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5556 install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5556 shell am instrument -w -e class com.gabriel.zenithui.data.WallpaperArtTest com.gabriel.zenith_Launcher.debug.test/androidx.test.runner.AndroidJUnitRunner
```

- [ ] **Step 3: implement.** `WallpaperArt.kt` keeps `render`, `screenSize`, and the shared helpers (`backdrop`, `light`, `mix`, `alpha`, `grain`, and a new `field(width, height, cell) { x, y -> colour }` that paints a per-pixel picture small and scales it up smoothly). The eight designs move to `WallDesigns.kt` as functions taking `(Canvas, WallColors, w, h, unit, Random)`. Each gets its light version by these rules: the backdrop comes from the palette's light tones; pools of light are drawn at about two thirds strength; lines and shapes use the accents deepened by mixing a little black in (`ink`) so they hold against a pale ground; white stars and highlights become soft tinted marks; a wash that darkens the lower part for white labels becomes one that lightens it for dark labels.
- [ ] **Step 4: run on device, pass; pull the contact sheets and look at them** (`adb -s emulator-5556 pull /sdcard/Android/data/com.gabriel.zenith_Launcher.debug/cache/walls-light.png`). Fix what looks wrong. **Step 5: commit** "Light versions of the eight wallpapers; designs in their own file".

---

### Task 4: seven new designs anyone can use

**Files:** Modify `data/WallDesigns.kt`, `data/WallpaperArt.kt` (enum entries). Test: `WallpaperArtTest` (it walks every style).

Mesh (Soft): four or five soft pools of colour blended per pixel. Drift (Soft): diagonal streaks of blurred light. Contour (Lines): map contour lines round two centres over a soft gradient. Ripple (Lines): rings spreading from a point off centre, fading out. Grid (Lines): a grid in perspective running to a glowing horizon. Bubbles (Shapes): clear orbs of different sizes with bright rims. Peaks (Scenes): layered mountain ridges fading into haze.

- [ ] **Step 1:** add the seven entries to `WallStyle` with their groups; the build fails until each has a painter (`when` is exhaustive). **Step 2:** write each painter, light and dark. **Step 3:** run `WallpaperArtTest` on device, pass; pull and look at both contact sheets; adjust. **Step 4: commit** "Seven new wallpapers: Mesh, Drift, Contour, Ripple, Grid, Bubbles, Peaks".

---

### Task 5: seven designs for Zenith Plus

**Files:** as Task 4. Test: `data/WallStyleTest.kt` (JVM) and `WallpaperArtTest`.

Silk (Soft): smooth folded bands shaded like cloth. Halo (Soft): one large ring of light with a soft corona. Ribbons (Lines): wide see-through ribbons crossing in curves. Prism (Shapes): large angled shards with gradient faces. Bloom (Shapes): petals spreading from a point low on the screen. Horizon (Scenes): a sun in light, a moon in dark, over layered haze and water. Nebula (Scenes): soft clouds of colour, stars in dark and pale wisps in light.

- [ ] **Step 1: failing JVM test** `WallStyleTest`: 22 styles; groups hold 6, 6, 5 and 5; exactly Silk, Halo, Ribbons, Prism, Bloom, Horizon and Nebula need Plus; `byName` finds each by its name and returns null for `""` and for an unknown name. **Step 2:** run, fail. **Step 3:** add entries and painters. **Step 4:** unit tests and `WallpaperArtTest` pass; look at the contact sheets. **Step 5: commit** "Seven Plus wallpapers: Silk, Halo, Ribbons, Prism, Bloom, Horizon, Nebula".

---

### Task 6: the director

**Files:** Create `WallpaperDirector.kt`. Modify `data/GlassWallpaper.kt`, `data/Wallpaper.kt`, `ZenithViewModel.kt`, `MainActivity.kt`.

**Interfaces — Produces:**
- `GlassWallpaper.set(context, bitmap, which, key: String? = null): Boolean` (records the key on success, clears it when `key` is null), `GlassWallpaper.appliedKey(context): String?`, `GlassWallpaper.isCurrent(context): Boolean`
- `WallpaperSource.present(bitmap: Bitmap)` (hands a picture to the glass before it is the system wallpaper; does not recycle it)
- `class WallpaperDirector(app, scope, settings: StateFlow<ZenithSettings>, source: WallpaperSource)` with `val incoming: StateFlow<Incoming?>`, `val refused: SharedFlow<Unit>`, `var homeShowing: Boolean`, `fun start()`, `fun request()`; `data class Incoming(val image: ImageBitmap, val leaving: Boolean)`
- `ZenithViewModel.wallpaperIncoming`, `.wallpaperRefused`, `.setWallpaper(style: WallStyle, pinnedPalette: String, lockToo: Boolean, switchThemeTo: String?)`, `.photoWallpaperSet()`, `.homeShowing(showing: Boolean)`

Behaviour:
- The director combines the settings fields that decide the picture, the system's night mode (kept up to date through `ComponentCallbacks` on the application) and a request counter, waits 200 ms for them to settle, and handles only the latest.
- For each: work out the key. Unless this is an explicit request, stop if `WallpaperFollow.shouldRepaint` says no. Paint on `Dispatchers.Default`.
- Home showing: publish `Incoming(image, leaving = false)`; after 350 ms call `source.present(bitmap)`; after another 350 ms set the system wallpaper with the key; wait for the glass copy to refresh, at most 1500 ms; publish `leaving = true`; after 320 ms publish null. Home not showing: set the wallpaper straight away.
- Refused by Android: emit on `refused`, mark the layer leaving, refresh the glass from the old copy, record nothing.
- When Android reports new wallpaper colours and Zenith's copy is no longer current while `wallpaperStyle` is set, clear `wallpaperStyle`.
- `setWallpaper` writes the four fields (and the theme, if asked) and calls `request()`. `photoWallpaperSet` clears `wallpaperStyle`.

The pure rule is tested in Task 2; this task is checked on the emulator in Task 8.

- [ ] **Step 1:** `GlassWallpaper` and `WallpaperSource` additions. **Step 2:** the director. **Step 3:** view model and activity wiring. **Step 4:** unit tests and `assembleDebug` pass. **Step 5: commit** "WallpaperDirector: the wallpaper follows the palette and light or dark".

---

### Task 7: the fading layer, the sheet, the switch

**Files:** Modify `ui/home/HomeScreen.kt`, `ui/home/WallpaperSheet.kt`, `ui/home/SettingsSheet.kt`.

- The layer: first child of the home screen's root `Box`, drawing `incoming.image` with the same zoom and offset the glass uses (`SYSTEM_WALLPAPER_ZOOM`, `systemWallpaperOffset`), alpha animated to 1 over 700 ms and back to 0 over 300 ms when leaving. A toast "Android would not set that wallpaper" on `refused`.
- The sheet: group chips (All, Soft, Lines, Shapes, Scenes) above the palette chips; a "Follows your theme" switch; thumbnails in the appearance in use with a lock on designs that need Plus; the preview shows the light and the dark picture side by side; "Set wallpaper" calls `setWallpaper` and closes the sheet with no toast, or reads "Unlock with Plus" and opens the Plus sheet. Setting a design in another palette's colours without switching Zenith to it pins the wallpaper to that palette. While no Zenith design is set, one line says "Pick one and it will follow your theme." A photo calls `photoWallpaperSet`.
- Settings, Look page: a "Wallpaper follows theme" switch.

- [ ] **Step 1:** the layer and the toast. **Step 2:** the sheet. **Step 3:** the switch. **Step 4:** unit tests and `assembleDebug` pass. **Step 5: commit** "Wallpapers: groups, light and dark previews, Plus designs, and a fade when the theme changes".

---

### Task 8: check it on the emulators, bring the words up to date

- [ ] On `Android10_test`: set Waves; change palette in Settings, Look: the wallpaper repaints in the new palette and fades in; switch Light and Dark: it repaints; check `GlassWallpaper`'s prefs record the key; tap through four palettes quickly: one set at the end; set a photo, change theme: the photo stays and `wallpaperStyle` is empty; idle frames are 0 afterwards; no crash in logcat.
- [ ] On `Pixel_10a` (Android 12+ may restart the home screen after a wallpaper change): install, and read logcat for an activity relaunch after one theme change made by Gabriel; if it happens, the layer must already be at full strength when it does.
- [ ] `README.md` (wallpapers and "How the glass works") and `play/listing.md`; `./gradlew :app:testDebugUnitTest :app:assembleDebug`; commit; install on `Pixel_10a`.
