# Apple look, step 4: sheets, menus, the drawer and "Add to home screen" — implementation plan

**Goal:** Every sheet is an iPhone's: frosted glass from the foot of the screen with Apple's corners
at the top, a grabber, its name in the middle and a round grey X, and everything in it on solid
cards, as Settings' rows now are. The long-press menus are sheets of that kind, their rows in cards
the way an iPhone groups them, what cannot be undone in red. The app drawer is a frosted blur of the
wallpaper with nothing of the theme washed over it, a grey search field at the foot, grey tabs with
the chosen one in the accent. "Add to home screen" from another app comes up on the same sheet.

**Architecture:** `ZenithSheet` draws every sheet, so the frame changes there once: it sits on the
foot of the screen with `CardKind.Sheet`'s 38 dp corners, and its header (grabber, centred title,
`CloseButton`, a back chevron for a sheet with pages) is its own. What goes inside a sheet is built
from the parts Settings already uses (`Section`, the rows, `Toggle`, `Segmented`, `PillSlider`,
`ZenithButton`), plus three that sheets need: `CardRows`, a card's rows with a hairline between each
two that show (so a menu whose rows come and go needs no dividers of its own); `MenuRow` as an iOS
row on such a card; and `FieldBox`, Apple's box to type in. Words that stand on a sheet's glass
rather than on a card (group names, footnotes) take the glass's words (`LocalOnGlass`), which hold
up over any wallpaper. Glass is never put on other glass: what was glass inside a sheet or the
drawer becomes a card, a field or a grey button. The rules with numbers in them are plain functions
with tests: a covering pane's frosted body when the wallpaper cannot be read (`fallbackBody`), and
where a card's hairlines go (`hairlinesAt`).

Spec: `specs/2026-11-04-apple-look-design.md`, "3. Settings, sheets, the drawer and buttons": sheets,
the app drawer and "Add to home screen"; and "The rule for glass".

## Global constraints

- As steps 1 to 3: no new dependencies; old settings, backups and Looks read as before; nothing on
  Gabriel's phone or his own emulator. `./gradlew :app:testDebugUnitTest :app:assembleDebug
  :app:compileDebugAndroidTestKotlin` after each part, and then the part is committed and pushed to
  `apple-look` (Gabriel asked for a push after each part, 2026-11-08). Pictures in light and dark on
  a throwaway emulator at the end.
- Every sheet keeps what it does: every choice, callback, condition, limit and word of help stays;
  only how it is drawn changes. A sheet is still pulled down, flicked away, or closed by a tap above
  it, and now by its X as well.
- Glass only where the wallpaper is right behind it. Inside a sheet and on the drawer, nothing is
  glass, with one exception: the Glass studio's own samples, which are what it tunes.
- Behind a sheet is the home screen, not the wallpaper. When the wallpaper cannot be read (a live
  one), a sheet's frosted body is not see-through, so its words never sit over the icons behind it.
- With movement reduced nothing slides, rises or rolls over; a pressed row still darkens.
- Tinted glass has nothing left to colour and is removed (`glass(tinted = …)`).

## File structure

| File | Responsibility |
| --- | --- |
| `ui/theme/Glass.kt`, `LiquidGlassShader.kt` | `fallbackBody` and `GlassLook.covers`; tinted glass removed |
| `ui/settings/SettingsKit.kt` | `CardRows` and `hairlinesAt`; `FieldBox`; `LocalOnGlass` for words on a sheet's glass |
| `ui/common/FullPage.kt` | `CloseButton` shared with sheets |
| `ui/common/Components.kt` | `SectionLabel` as Apple's group name |
| `ui/home/Sheets.kt` | `ZenithSheet`'s frame and header; `MenuRow`, `ShortcutRows`, the long-press menu, Customise, How it works |
| `ui/onboarding/HelpStep.kt` | the tips on solid cards |
| `ui/home/EditSheets.kt`, `GestureSheets.kt`, `LookSheets.kt`, `PlusSheets.kt`, `StudioSheets.kt`, `WallpaperSheet.kt`, `WidgetSheets.kt`, the sheets in `HomeScreen.kt` and `ZenithWidgets.kt` | each sheet's insides on solid cards |
| `PinItemActivity.kt` | "Add to home screen" on `ZenithSheet` |
| `ui/home/AppDrawer.kt`, `DrawerExtras.kt` | the frosted drawer, its grey search and tabs, solid groups, answers and letter bubble |
| `README.md` | sheets, menus and the drawer as they now are |

## Tasks

### Part 1: the rules, tests first

- [ ] `GlassLookTest`: when the wallpaper cannot be read, a sheet's body and an open folder's are not
  see-through, in the light and the dark; a floating control's still lets a live wallpaper show
  through, even with the person's Solid glass.
- [ ] `CardRowsTest`: a hairline at the top of every row that shows except the first; none for a row
  folded away to nothing; none for a card of one row or of none.
- [ ] `GlassPressTest`: the words on a pill of the accent (the test that spoke of tinted glass).
- [ ] `fallbackBody`, `GlassLook.covers`, `hairlinesAt`; tinted glass removed.

### Part 2: the sheet

- [ ] `ZenithSheet`: on the foot of the screen, 38 dp corners, frosted; the grabber; the title in the
  middle, rolling over when it changes; the round X; a back chevron for a sheet with pages.
- [ ] `CardRows`, `MenuRow` as an iOS row, `FieldBox`, `LocalOnGlass`, `SectionLabel`.

### Part 3: every sheet's insides

- [ ] Edit and Choose icon; a gesture's sheet and Lock with a gesture; the Looks' four; Plus, Theme
  maker and Hidden apps; Glass studio, By time of day, Colour and Fonts; Wallpaper (its pages through
  the sheet's back chevron); World clock, Countdown, Weather and Quick Toggles; Customise and How it
  works. Glass, glassy fills and rings inside them give way to cards, fields and grey buttons.

### Part 4: the long-press menus and "Add to home screen"

- [ ] The long-press menu in Apple's groups: the app's shortcuts, what can be done, what cannot be
  undone in red, and Customise. Each row an iOS row.
- [ ] `PinItemActivity` on `ZenithSheet`: the picture and whose it is on a card, Not now and Add.

### Part 5: the app drawer

- [ ] A frosted blur of the wallpaper with no wash of the theme; its header's button grey; the tabs
  grey capsules, the chosen one in the accent; the search Apple's grey at the foot; the library's
  groups, the search's answers and the letter bubble solid; the tab sheet as every sheet.

### Part 6: check and ship

- [ ] README; build and every unit test; pictures in light and dark on a throwaway emulator (a menu,
  Customise, a sheet with switches and fields, Wallpaper's pages, the drawer and its search);
  commit and push to `apple-look`.
