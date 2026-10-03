# Stage 4: motion — implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task by task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The launcher moves like something made with care: pages turn in the style you chose, the home screen flies in when you arrive, icons jiggle while one is carried and land with a ring, folders open out of their own icon, widgets animate their own changes, and all of it turns down to quick fades when asked.

**Architecture:** What can be worked out without a screen is a pure function with a test: how much to move (`MotionLevel`), where a page is drawn for a given swipe (`PageEffects`), when each item arrives (`FlyIn`). Everything on screen reads those in `graphicsLayer` or while drawing, never in layout, and anything that runs for a while runs on one shared clock per page rather than one animation per item.

**Tech stack:** Kotlin 2.2, Jetpack Compose (BOM 2024.12.01), JUnit 4.

Spec: `specs/2026-10-01-zenith-overhaul-design.md`, "Stage 4: motion".

## Global Constraints

- minSdk 29, targetSdk 37, no new dependencies.
- Movement goes through `graphicsLayer` or draw-time state, never through layout. No new per-frame allocation, and no blur or shader rebuilt per frame.
- Nothing runs while the home screen is idle: 0 frames in 3 s by `dumpsys gfxinfo`, checked after every task that adds something that repeats.
- `MotionLevel.Reduced` (the Animations setting, or the phone's Remove animations): no fly-in, no jiggle, no parallax, page swipe is Slide, folders and previews fade instead of growing.
- Haptics go through `Haptics`; nothing ticks when the Haptics setting is off.
- Build and test as in the earlier stages. Hands-on checks on `Android10_test`; the finished build goes on `Pixel_10a` last.

## Review Focus

1. Idle frames are 0 after: a fly-in, a drag and drop, opening and closing a folder, a page turn in each style.
2. A drag that ends anywhere (dropped, cancelled, onto the bin, into a folder, off to another page) stops the jiggle and leaves no item tilted.
3. A folder opened and closed quickly, or closed by back, home, or opening an app from it, never leaves the home screen dimmed or blurred.
4. Reduced really is reduced: none of the listed movements play, and nothing is left half-way.
5. Page styles: every style is exactly the plain page at rest (no tilt, shift or fade left over), on the first and the last page too.

## File structure

| File | Responsibility |
| --- | --- |
| `ui/common/MotionLevel.kt` (new) | `MotionLevel`, `LocalMotionLevel`. Pure rule. |
| `ui/common/Motion.kt` | Named specs |
| `ui/common/Haptics.kt` | `Haptics`: tick, pick up, confirm; silent when off |
| `ui/home/PageEffects.kt` (new) | Page swipe styles as pure functions |
| `ui/home/FlyIn.kt` (new) | When and from where each item arrives. Pure. |
| `ui/home/HomeScreen.kt`, `ui/home/HomeGrid.kt` | Pager transforms, page dots, fly-in, jiggle, ring, haptics |
| `ui/home/FolderPanel.kt` (new) | A folder opening out of its icon |
| `ui/home/ZenithWidgets.kt` | Clock, battery, toggles, calendar |
| `ui/home/AppDrawer.kt`, `ui/home/WallpaperSheet.kt` | Rows rising in; a thumbnail growing into its preview |

---

### Task 1: how much to move, and one place for haptics

**Interfaces — Produces:**
- `enum class MotionLevel { Full, Reduced }` with `MotionLevel.of(setting: String, systemAnimations: Boolean): MotionLevel`; `LocalMotionLevel`.
- `Motion.press`, `.lift`, `.drop`, `.open`, `.close`, `.sheet`, `.page`, `.theme` (existing names stay).
- `class Haptics { fun tick(); fun pickUp(); fun confirm() }`, `LocalHaptics`, and Compose's own `LocalHapticFeedback` silenced when the setting is off.

- [ ] **Step 1: failing test** `MotionLevelTest`: Reduced for `"reduced"`, or with system animations off whatever the setting; anything else, an unknown word included, is Full. **Step 2:** run, fail. **Step 3:** implement; provide the locals in `ZenithApp`. **Step 4:** pass; commit "MotionLevel, named motions and one Haptics".

### Task 2: page swipe styles, and dots that stretch

**Interfaces — Produces:**

```kotlin
data class PageTransform(val scale: Float = 1f, val alpha: Float = 1f, val rotationY: Float = 0f,
                         val pivotX: Float = 0.5f, val shiftX: Float = 0f, val parallax: Boolean = false)
object PageEffects {
    val styles: List<String>                       // depth, slide, cube, stack
    fun transform(style: String, offset: Float): PageTransform
    fun dotWidth(position: Float, index: Int): Float   // 0 for a resting dot to 1 for the active one
}
```

`offset` is how far the page is from the middle, in pages: positive when it lies to the left. `shiftX` is in page widths.

- [ ] **Step 1: failing test** `PageEffectTest`: every style is `PageTransform()` at 0 apart from `parallax`; depth at ±1 is scale 0.94, alpha 0.6; slide is identity everywhere; cube at ±0.5 turns ±45° about the edge it shares (pivot 1 for a page on the left, 0 on the right) and never past 90°; stack pins a page on the left in place (`shiftX == offset`), smaller and fainter, and leaves a page on the right alone; unknown style is depth; offsets beyond ±1 are clamped; dot widths at a position between two dots sum to 1. **Step 2:** run, fail. **Step 3:** implement; use it in the pager (Reduced forces slide); `PageDots` draws from the pager's position. **Step 4:** pass, build; commit "Four ways to turn a page, and dots that stretch".

### Task 3: the home screen flies in

**Interfaces — Produces:** `object FlyIn { const val TOTAL_MS; fun delayMs(distance: Float): Int; fun progress(clockMs: Float, delayMs: Int): Float; fun shift(fromCentre: Float): Float }`; `HomePage(arrival: () -> Float?)`.

One clock per arrival, run by the home screen: on a cold start and on unlock, the full fly-in; on coming back from an app, a short settle (smaller distance, same order). Items nearest the middle land first; everything is down inside 600 ms.

- [ ] **Step 1: failing test** `FlyInTest`: nothing has moved before its delay; everything is at 1 by `TOTAL_MS`; a nearer item never starts after a farther one; progress may overshoot but ends on exactly 1. **Step 2:** run, fail. **Step 3:** implement and wire. **Step 4:** pass, build, idle frames; commit "The home screen flies in when you arrive".

### Task 4: pick up, carry, drop

- The copy under the finger lifts on `Motion.lift` and gets a soft shadow; every other item on the page tilts back and forth about 1.2°, each out of step, from one clock that runs only while something is carried.
- A tick as the landing spot changes; a confirm when it is put down, and when a folder is made.
- The item put down settles with a ring that spreads and fades.

- [ ] **Step 1:** implement. **Step 2:** build, unit tests, idle frames after a drag on the emulator. Commit "Carrying an icon: lift, jiggle, tick, and a ring where it lands".

### Task 5: folders open in place

`FolderPanel`: from the folder icon's own rectangle to a glass panel in the middle of the screen, on one spring; the home screen dims and blurs behind it. Back, a tap outside, home, or opening an app closes it back into the icon. Renaming and taking an app out work as they did in the sheet, which goes.

- [ ] **Step 1:** implement; remove `FolderSheet` and `HomeSheet.Folder`. **Step 2:** build, unit tests, check on the emulator. Commit "Folders open out of their own icon".

### Task 6: widgets that move

Clock digits roll when they change; the battery ring springs to its value and shimmers while charging; a toggle changes colour and shape as it switches; today's ring on the calendar draws itself in.

- [ ] **Step 1:** implement, each on its own change only (nothing that repeats, except the charging shimmer while charging and visible). **Step 2:** build, idle frames. Commit "Widgets animate their own changes".

### Task 7: the drawer, sheets and wallpapers

Drawer rows rise and fade in one after another as it opens; a wallpaper thumbnail grows into its preview from where it was tapped.

- [ ] **Step 1:** implement. **Step 2:** build. Commit "Drawer rows rise in; a wallpaper grows into its preview".

### Task 8: check it, in Full and in Reduced

- [ ] On `Android10_test`, each row of the spec's table, in Full and in Reduced, with screenshots; the five review-focus points; no crash in logcat.
- [ ] README and `play/listing.md`; unit tests and `assembleDebug`; commit; install on `Pixel_10a`; push.
