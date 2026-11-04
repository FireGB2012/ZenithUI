# Zenith, the iPhone look: liquid glass × a normal-looking phone

- Date: 2026-11-04
- Branch: `apple-look`, made from `liquid-glass-buttons` (which is not merged yet: the press motions
  built there are used here). Merging into `main` is Gabriel's call.
- Status: designed with Gabriel, section by section, from four pictures of the launcher he likes
  (an iPhone-style Lawnchair build). His words: "fully remove the neon lights stuff and all that
  liquid glass everywhere, let's make it look like Apple so the widgets are normal… liquid glass ×
  normal looking phone". Every decision below was put to him and agreed. Nothing is built until he
  has read this.

## What the pictures showed

- **Content is solid.** The Clock widget is a dark grey tile with a white face; Weather is one flat
  sky colour. Settings is an iOS grouped list: a light grey page, white rounded cards of rows, each
  row a small coloured square icon, a name, a chevron; blue switches; actions as blue words.
- **Glass is only for controls that float over the wallpaper**: the dock, a small "search" pill
  with a round button beside it, the pill round the page dots, Cancel and the eye button in edit
  mode, and four round buttons along the bottom (Wallpaper, Widgets, Settings…). A settings sheet
  over the home screen is frosted, with solid cards inside it.
- **Nothing glows.** That launcher has a "Glow effect" switch, and it is off.

Zenith today does the opposite: glass on everything (widgets, Settings, every sheet, every button),
light that moves behind setup, a glow of the accent inside every widget, and rims that take the
wallpaper's colour. This design turns that round.

## Decisions (each agreed with Gabriel)

1. **Like an iPhone**, not "Zenith but calmer": Apple's colours everywhere, the palettes only as
   the accent.
2. **Glass stays on** the dock and search, open folders, sheets and the app drawer, and on the small
   controls floating over the home screen. Everything else is solid.
3. **Widgets are solid tiles only.** No glass widgets and no no-glass widgets.
4. **All the neon goes:** setup's moving lights, the coloured glow on glass (rim colour and the
   light under a finger), the glowing wallpapers, and Aura's glow.
5. **Wallpapers:** the glowing designs leave the picker and a new shelf of calm iPhone-style designs
   comes in.
6. **Approach A**: replace the look in place, in phases on one branch. No second "style" to keep up.
7. **Jiggle mode** is added: the iPhone's edit mode, as the last step.

## The rule for glass

Zenith's glass shows the **wallpaper**, not what is drawn behind it in the app. So glass may only
sit where the wallpaper is directly behind it (glass on glass shows a hole through to the
wallpaper, as README says of icons in folders):

- the dock, the Search pill and its Google button, the page dots' pill;
- folders, on the page and open;
- sheets and the long-press menus (which are sheets);
- the app drawer;
- pills and buttons floating over the home screen (Undo, the Aura streak, jiggle mode's bar and
  buttons).

Never on a solid page (Settings, the widget gallery, setup), where it would look like a hole through
to the wallpaper. Inside a sheet, what sits on it is solid: cards, fields, buttons.

**What the glass looks like**: Apple's. A thin clean white rim; no colour taken from the wallpaper
into the rim or the glow just inside it (`GlassLook.glow` is 0); no light blooming under a finger
(`PressLight` goes, and with it the extra bend while pressed); no theme colour in the veil, which
is neutral. It still bends the wallpaper at its edge and casts its soft shadow, and the press
motions stay where there is glass: Flex and Lean (`GlassPress`). The Glass studio (Plus) still
tunes what glass is left; its defaults are this glass. Tinted glass (`glass(tinted = true)`) has
nothing left to colour and is removed.

## 1. Colours, text and the solid card

- **Apple's colours, light and dark** (`ui/theme/AppleColours.kt`, new), replacing the palette's
  backdrop wherever the interface is drawn:

  | | Dark | Light |
  |---|---|---|
  | Page | `#000000` | `#F2F2F7` |
  | Card, widget tile | `#1C1C1E` | `#FFFFFF` |
  | Card on a card, control track | `#2C2C2E` | `#E5E5EA` |
  | Words | `#FFFFFF` | `#000000` |
  | Second line | words at 60% | words at 60% |
  | Separator | `#38383A` | `#C6C6C8` |
  | Grey button | `#787880` at 36% | `#787880` at 20% |
  | Badge | `#FF453A` | `#FF3B30` |

  Second lines keep Zenith's rule that they are readable: `AppleColoursTest` holds every pair of
  words and the card or page under them to 4.5 to 1 or better, in the light and in the dark.
- **The palette is the accent.** All eleven stay, the six anyone has and the five earned with Aura,
  and keep painting Zenith's wallpapers. In the interface a palette only colours switches, the
  chosen option, the main buttons, accent words, the calendar's today and a widget given a colour
  of its own. A new install starts on **Frost**, whose accent becomes Apple's blue (`#0A84FF`
  dark, `#007AFF` light); anyone already on another palette keeps it.
- **Every accent has a light version and a dark one**, as Apple's own colours do
  (`AppleColours.accent(palette, dark)`). Over a white card a pale accent would vanish (Graphite's
  near-white, Solstice's yellow), so in the light it is taken deeper, keeping its hue, until it
  stands 3 to 1 against white and `#F2F2F7`; in the dark, until it stands 3 to 1 against
  `#1C1C1E` and black. `AppleColoursTest` holds all eleven to that. Words on a pill of the accent
  take the ink it needs (`inkOnTint`): white on blue, near-black on yellow.
- **Font:** Inter becomes the default for a new install, as the nearest open font to Apple's SF Pro
  (which Apple's licence keeps to its own devices). Anyone who chose a font keeps it.
- **One solid card** (`ui/theme/Card.kt`, new): Apple's corners (widgets 22 dp, grouped cards
  12 dp, sheets 38 dp at the top), the card colour, no rim, no glow. Widgets, the grouped rows of
  Settings and the cards inside sheets are all drawn with it.

## 2. The home screen

- **Widgets are solid tiles** (`ZenithWidgets.kt`, `MoreWidgets.kt`): every widget keeps its sizes
  and its designs; the pane of glass and the accent glow inside it give way to the tile, words in
  the words' colour, the accent where something is picked out. Some take Apple's own colour:
  - **Weather**: the sky as it is there, a deep blue by day, navy by night, grey under cloud, slate
    in rain (`WeatherSky`, a plain function with a test);
  - **Now playing**: the main colour of the album's picture (as `IconColour` finds an icon's);
  - **Photo frame**: the photo fills the tile;
  - a widget given a colour of its own (its menu → Colour) fills its tile with it, words in the ink
    that colour needs (`inkOnTint`).
- **No-glass widgets go.** A `bare_<type>` widget on a page, in a stack, in a backup or in a Look
  becomes its solid `<type>` the first time it is read (`WidgetMigration`, with a test); nothing is
  dropped or moved. The gallery loses its Glass / No glass switch; `GlassInk.kt` is deleted.
- **Other apps' widgets** are drawn as their app draws them. The pane of glass that could be put
  behind one becomes a tile that can be put behind one.
- **The dock** stays a bar of glass, with the clean rim.
- **Search** becomes the iPhone's: a small pill of glass above the dock saying "Search", and beside
  it a round glass button for Google (where the full-width bar's button was). The setting that
  switches the bar on switches these on.
- **Folders** stay glass, on the page and open.
- **Notification dots** are Apple's: a red badge, white number (`99+` past 99; `BadgeText`, with a
  test), or a red dot where Dot is chosen.
- **Floating pills** (Undo, Aura's streak) are plain glass with white words, no glow.
- **Stays as it is**: the grid, gestures, icon shapes and packs, icon styles (Glass icons are like
  iOS 26's clear icons and stay a choice, Original by default), opening and closing apps, the Island.

## 3. Settings, sheets, the drawer and buttons

- **Settings and the widget gallery** (`FullPage.kt`, `SettingsScreen.kt`, `SettingsKit.kt`,
  `SettingsPages.kt`, `StudioKit.kt`, `WidgetGallery.kt`): an iOS grouped list on the page colour —
  a large bold title, the grey rounded search field, groups as solid cards with rows (a small
  square icon in a colour of the group's own, the name, the value in grey, a chevron), separators
  that start under the name, group names small and grey above the cards, and the line under each
  choice as a grey footnote below its card. Back is Apple's: an accent chevron and the page's name.
  The small live copy of the home screen at the top of Theme and Home stays.
- **Controls on solid pages**:
  - switches are iOS's, the accent when on (`Toggle`);
  - a row of options is Apple's segmented control: a grey track and a raised pill, white in the
    light and `#636366` in the dark (`Segmented`). The pill can still be picked up and slid along
    (the Drag motion built in `liquid-glass-buttons`): it swells, ticks at each option and settles
    on the nearest, but it is solid, not a lens;
  - sliders: a thin track in the accent and a round white knob with a shadow;
  - a row pressed darkens for a moment, as iOS's do, rather than bouncing.
- **Buttons** (`OnboardingKit.kt`, `Components.kt`):
  - the main one (Continue, Set wallpaper, Done) is a solid pill of the accent, its words in the
    ink the accent needs, and keeps the Jelly squish;
  - second choices (Not now, Keep it, Clear) are Apple's grey filled pills;
  - small actions are words in the accent, as "Regenerate all icons" is in the pictures.
  `GlassButton` becomes `ZenithButton` with those three kinds; `filled = true` is the main kind.
- **Sheets** (`Sheets.kt` and every sheet built on `ZenithSheet`): frosted glass, as in the
  pictures; a grabber, a title in the middle, a round X; everything in it on solid cards.
- **The app drawer** (`AppDrawer.kt`, `DrawerExtras.kt`): a frosted blur of the wallpaper, like the
  iPhone's App Library, without the coloured wash over it; its search field stays at the foot by
  the thumb, in Apple's grey; the tabs are grey capsules, the chosen one in the accent.
- **"Add to home screen"** from other apps (`PinItemActivity`) uses the sheet.

## 4. Setup, wallpapers and Aura

- **Setup** (`ui/onboarding/`): a plain page, black or white, the bold titles as they are.
  `OnboardingBackdrop` (the moving lights) and the unused `AuroraBackdrop` are deleted; the phone
  in `PhonePreview` loses its glow; fields are Apple's grey; the main buttons solid.
- **Wallpapers** (`WallpaperArt.kt`, `WallDesigns.kt`, `WallpaperSheet.kt`):
  - leaving the picker, as glowing designs: **Glow, Drift, Dawn, Bokeh, Halo, Rays, Grid, Aurora,
    Nebula**. They are marked retired, not deleted, so a phone, a backup or a Look that has one
    still paints it and still follows the theme; it is only not offered (`WallStyle.offered`, with
    a test). Twenty-four designs stay.
  - a new shelf, **Flow**: broad smooth sweeps of colour like the wallpaper in the pictures, each
    with a light picture and a dark one, painted from the palette's colours like every other
    design: **Sweep, Arc, Fold, Layers** for everybody and **Current, Swell** for Plus. Each is
    looked at on the emulator in every palette, light and dark, before it is kept.
- **Aura** (`AuraPage.kt`, the Aura widget, `StreakStrip.kt`): streaks, aura and the colours it
  earns stay. The ring is drawn flat in the accent, the flame is a flat symbol, and the radial
  glows behind them go.

## 5. Jiggle mode

The iPhone's way of rearranging, as in the third picture (`HomeScreen.kt`, `HomeGrid.kt`, and a new
`EditSession`):

- **In**: hold an empty spot (where Customise opened), or **Edit home screen** at the top of any
  app's or widget's menu.
- **While in it**: every icon and widget rocks gently, each out of step (the rocking a carried item
  already sets off, kept on); each has a grey minus at its corner that takes it off (with the Undo
  pill, as now); anything can be dragged straight away, without holding, to any spot, the dock,
  a folder or another page.
- **Along the top**, glass pills: **Cancel** on the left and **Done**, solid in the accent, on the
  right. **Along the foot**, round glass buttons with their names under them: **Wallpaper**,
  **Widgets**, **Settings**, and **Customise** for the rest of what that sheet held (Bedside clock,
  making Zenith the home screen, How it works).
- **Out**: Done keeps everything; the home button, back, or a tap on an empty spot does the same.
  Cancel puts the pages back exactly as they were when it began (`EditSession` keeps that layout,
  and has a test: begin, change, cancel gives the first layout back; done keeps the last).
- Carrying an item from a long press without jiggle mode works as it does today.
- With movement reduced nothing rocks; the minus badges and the bars still come.

## 6. How it is built and checked

In eight steps, one after another on `apple-look`; after each, `./gradlew :app:testDebugUnitTest
:app:assembleDebug`, pictures in light and in dark on an emulator made for the purpose and deleted
afterwards, set beside Gabriel's reference pictures; then a commit and a push.

1. Colours, the card, the font, the clean glass (no glow, no finger light, neutral veil).
2. The home screen: solid widgets and their migration, dock, Search pill, badges, folders, pills.
3. Settings and the gallery as grouped lists; switches, segmented control, sliders; the buttons.
4. Sheets, menus, the drawer, "Add to home screen".
5. Setup.
6. Wallpapers: the nine retired, the Flow shelf added.
7. Aura flat; README, `play/listing.md` and this spec brought up to date.
8. Jiggle mode.

**Rules with tests first**: `AppleColoursTest` (every word on every card and page, and all eleven
accents, in the light and in the dark), `WidgetMigrationTest` (`bare_<type>` → `<type>` on pages,
in stacks, backups and Looks), `WeatherSkyTest`, `BadgeTextTest`, the wallpapers offered in
`WallStyleTest`, `EditSessionTest`. Tests that pin the old look change with it: `GlassLookTest`
(glow 0, neutral veil), `GlassLiftTest`, `PaletteToneTest` (now against Apple's page and card),
`GlassTuningTest` (new defaults). `GlassPressTest` stays as it is: the motions do not change.

**On Gabriel's phone** only when he asks: the signed release over his Zenith, which keeps his home
screen (its certificate matched on 2026-11-04). A release build cannot be read with `run-as`, so the
backup CLAUDE.md asks for is his Backup & restore file, made before.

## Not in this plan

iOS 26's Clear and Tinted widgets; the Lock Screen; a Control Centre; changes to gestures, the grid,
the server, Plus or its price. Old settings and backups keep reading: nothing is taken out of the
saved form, only no longer offered.
