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
with tests: a covering pane's body when the wallpaper cannot be read, Apple's page (`fallbackFill`), and
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
| `ui/theme/Glass.kt`, `LiquidGlassShader.kt` | `fallbackFill` and `GlassLook.covers`; tinted glass removed |
| `ui/settings/SettingsKit.kt` | `CardRows` and `hairlinesAt`; `FieldBox`; `LocalOnGlass` for words on a sheet's glass |
| `ui/common/FullPage.kt` | `CloseButton` shared with sheets |
| `ui/common/Components.kt` | `SectionLabel` as Apple's group name; `LocalIconsOnPlate` |
| `ui/onboarding/PhonePreview.kt` | the live copy of the home screen draws its icons as the home screen does |
| `ui/home/Sheets.kt` | `ZenithSheet`'s frame and header; `MenuRow`, `ShortcutRows`, the long-press menu, Customise, How it works |
| `ui/onboarding/HelpStep.kt` | the tips on solid cards |
| `ui/home/EditSheets.kt`, `GestureSheets.kt`, `LookSheets.kt`, `PlusSheets.kt`, `StudioSheets.kt`, `WallpaperSheet.kt`, `WidgetSheets.kt`, the sheets in `HomeScreen.kt` and `ZenithWidgets.kt` | each sheet's insides on solid cards |
| `PinItemActivity.kt` | "Add to home screen" on `ZenithSheet` |
| `ui/home/AppDrawer.kt`, `DrawerExtras.kt` | the frosted drawer, its grey search and tabs, solid groups, answers and letter bubble |
| `README.md` | sheets, menus and the drawer as they now are |

## Tasks

### Part 1: the rules, tests first

- [x] `GlassLookTest`: when the wallpaper cannot be read, a sheet's body and an open folder's are not
  see-through, in the light and the dark; a floating control's still lets a live wallpaper show
  through, even with the person's Solid glass.
- [x] `CardRowsTest`: a hairline at the top of every row that shows except the first; none for a row
  folded away to nothing; none for a card of one row or of none.
- [x] `GlassPressTest`: the words on a pill of the accent (the test that spoke of tinted glass).
- [x] `fallbackFill`, `GlassLook.covers`, `hairlinesAt`; tinted glass removed. (Seen on the
  emulator: a sheet's own frosted body was nearly the cards' colour, so with no wallpaper to read it
  is Apple's page, which the cards stand off as in Settings; the test says so.)

### Part 2: the sheet

- [x] `ZenithSheet`: on the foot of the screen, 38 dp corners, frosted; the grabber; the title in the
  middle, rolling over when it changes; the round X; a back chevron for a sheet with pages.
- [x] `CardRows`, `MenuRow` as an iOS row, `FieldBox`, `LocalOnGlass`, `SectionLabel`.

### Part 3: the long-press menus and "Add to home screen"

- [x] The long-press menus (on the home screen and in the drawer) in Apple's groups: the app's
  shortcuts, what can be done, what cannot be undone in red, and Customise. Each row an iOS row.
- [x] Customise (solid tiles), How it works (the tips on one card) and Lock with a gesture.
- [x] `PinItemActivity` on `ZenithSheet`: the picture and whose it is on a card, Not now and Add.

### Part 4: every other sheet's insides

- [x] Edit and Choose icon; a gesture's sheet; the Looks' four; Plus, Theme maker and Hidden apps;
  Glass studio, By time of day, Colour and Fonts; Wallpaper (its pages through the sheet's back
  chevron); World clock, Countdown, Weather and Quick Toggles. Glass, glassy fills and rings inside
  them give way to cards, fields and grey buttons. (Done by seven agents at once, one file each,
  from one brief of the rules; then built and checked here.)

### Part 5: the app drawer

- [x] A frosted blur of the wallpaper with no wash of the theme; its header's button grey; the tabs
  grey capsules, the chosen one in the accent; the search Apple's grey at the foot; the search's
  answers and the letter bubble solid; the library's plates a lift of white in the dark and Apple's
  white card in the light, never glass; the tab sheet as every sheet.

### Review

Five reviewers read every change of this step, each from one angle (the rule for glass, what the
lead's files used to do, what the sheets used to do, Compose, accessibility), and two more tried to
refute each finding against the code: 17 held. Fixed, with the two found on the emulator:

- [x] Over Settings or the gallery a sheet's glass was a hole through a solid page to the wallpaper:
  there a sheet is solid now, a step lighter than the page in the dark (`AppleSurfaces.elevated`,
  tested; `LocalSheetsOverPage`).
- [x] With no wallpaper to read, a covering pane's top sheen and shading lifted its page to the
  cards' grey just where the first cards sit: it is flat now (`fallbackSheen`, tested). The sheet's
  rim ran down the screen's own edges: a sheet's glass has no rim.
- [x] The drawer's full-screen glass was drawn again on every scroll and every tilt: it is a cached
  layer of its own, and a pane with no rim no longer follows the tilt (`GlassLook.followsTilt`,
  tested). Its contents now know they stand on glass (`LocalOnGlass`, `LocalIconsOnPlate`).
- [x] A Glass-style icon on a card or a page was a pane of glass, a hole through it: anywhere in a
  sheet or on a page it takes a plate (`LocalIconsOnPlate`), except in the live copies of the home
  screen, which draw it as the home screen does.
- [x] Wallpaper's light and dark previews drew widget words in the wrong tone; a disabled Plus plan
  card was see-through; "Add to home screen" could lose its buttons on a short window and could
  close before its sheet had gone (`ZenithSheet(onGone)`); the Edit sheet, the long-press menus and
  Customise could not scroll when taller than the sheet; the search's "No apps match" sat over its
  answers; `FieldBox` took taps only on its line of text and had no name for TalkBack; the gesture
  sheet's shortcuts arrow, the drawer's tabs and the tab sheet's ticks were faint or unnamed to
  TalkBack; the Looks clock ran over its buttons at large text sizes.
- Left as they were (older than this step, and not asked for by the design): the day chips' and
  swatches' TalkBack names, Hidden apps' wrong-PIN shake with movement reduced, the Customise tiles
  and plan cards at the largest text sizes, and an icon's name read twice beside its row's name.

### Part 6: check and ship

- [x] README; build and every unit test; pictures in light and dark on a throwaway emulator (menus,
  Customise, How it works, Edit, a gesture's sheet over Settings, Wallpapers and a design's page,
  Plus, the drawer, the App Library and a search); commit and push to `apple-look`.
