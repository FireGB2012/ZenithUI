# Apple look, step 3: Settings, the gallery, controls and buttons — implementation plan

**Goal:** Settings and the widget gallery are pages as an iPhone draws them: Apple's page colour, a
large bold title, a grey search field, and the options in solid cards of rows, each group named small
and grey above its card and explained in a grey footnote under it. The controls on them are Apple's:
switches in the accent, a segmented control whose pill is solid but can still be picked up and slid,
thin sliders with a round white knob, and rows that darken under a finger instead of bouncing.
Buttons everywhere are Apple's three kinds: a solid pill of the accent, a grey pill, and words in the
accent.

**Architecture:** Every page of Settings is built from the parts in `SettingsKit.kt` and framed by
`FullPage`, so the look changes there once. `FullPage` is drawn in `LocalApple.page`, not glass, and
its Back is an accent chevron with the name of where it goes. A new `Section` (a name above, a
`solidCard` of rows, a footnote below) takes the place of `Group` with a `Caption` and a `Note` inside
it, and each page is rearranged into Sections, every `Spot` kept, so search still finds each option.
The rows share one iOS row: a 30 dp square in a colour of the group's own, the name, the second line
in grey, a chevron, a hairline from under the name, and a shade while pressed. The colours the new
parts need join `AppleSurfaces` (`pressed`, `pill`, `field`, `danger`), and the three buttons' colours
are one rule, `AppleColours.button`. `GlassButton` becomes `ZenithButton(kind = …)`, and setup's
`PrimaryButton` and `QuietButton` are drawn by it. The rules with numbers in them are plain functions
with tests: those colours, the groups' tiles, where Back goes (`SettingsPage.parent`) and the
slider's knob (`SliderGeometry`).

Spec: `specs/2026-11-04-apple-look-design.md`, "3. Settings, sheets, the drawer and buttons": its
first three parts. Sheets, the drawer and "Add to home screen" are step 4.

## Global constraints

- As steps 1 and 2: no new dependencies; old settings, backups and Looks read as before; nothing on
  Gabriel's phone or his own emulator. `./gradlew :app:testDebugUnitTest :app:assembleDebug
  :app:compileDebugAndroidTestKotlin` after each part, and then the part is committed and pushed to
  `apple-look` (Gabriel asked for a push after each part, 2026-11-08). Pictures in light and dark on
  a throwaway emulator at the end.
- Search finds every option where it did: each `Spot` id stays, and `SettingsCatalogTest` passes
  unchanged.
- Sheets keep their glass until step 4. The parts they share with Settings (switches, sliders,
  segmented controls, rows, buttons) take the new look now, which is what step 4 puts inside them.
- With movement reduced nothing slides or swells; a pressed row still darkens, which is not movement.
- The small live copy of the home screen at the top of Theme and Home stays as it is.
- The widget gallery is plain, like the iPhone's (Gabriel's choice, 2026-11-08): widgets stand on
  the page, with no card and no wallpaper behind them, in the gallery and in a widget's showcase.

## File structure

| File | Responsibility |
| --- | --- |
| `ui/theme/AppleColours.kt` | `pressed`, `pill`, `field`, `danger`; `ButtonKind` and `AppleColours.button` |
| `ui/common/FullPage.kt` | the page in Apple's page colour; Back as an accent chevron and where it goes; a round grey close |
| `ui/settings/SettingsKit.kt` | `Section`, the iOS row and its shade, `Toggle`, the slider (`SliderGeometry`), `StepperRow`, `ChoiceRow` with a check |
| `ui/onboarding/Segmented.kt` | Apple's segmented control: a grey track and a raised solid pill that can still be slid |
| `ui/settings/SettingsScreen.kt`, `SettingsCatalog.kt` | the grey search field, group names, tiles in Apple's colours, `SettingsPage.parent` |
| `ui/settings/SettingsPages.kt` and the other pages | each page in Sections, footnotes under their cards |
| `ui/settings/StudioKit.kt` | font and page-turn tiles and chips, solid |
| `ui/home/WidgetGallery.kt`, `ui/home/HostedWidgets.kt` | the plain gallery and showcase |
| `ui/common/Components.kt` | `ZenithButton` (Main, Grey, Plain) in place of `GlassButton` |
| `ui/onboarding/OnboardingKit.kt` | `PrimaryButton` is a main button, `QuietButton` words in the accent |
| callers of `GlassButton` | `ZenithButton` |
| `README.md` | Settings, the gallery and the buttons as they now are |

## Tasks

### Part 1: the rules, tests first

- [x] `AppleColoursTest`: words and second lines stand 4.5 to 1 on a pressed row, on the segmented
  pill, and in the search field over the page and over a card, light and dark; a main button's words
  stand 3 to 1 on every palette's accent (Apple's own white on blue is 4 to 1); a grey button's words
  stand 4.5 to 1 on it over a card and over the page; a plain button is the accent with nothing
  behind it; danger words stand 3 to 1 on a card and on the page.
- [x] `SettingsPageTest`: white stands 3 to 1 on every group's tile; Back from Advanced animation
  goes to Motion & feel, and from every other page to Settings.
- [x] `SliderGeometryTest`: the knob's middle stays a knob's radius inside each end; a value puts it
  its share of the way along; a touch picks the value under it, and the end value past either end;
  a track no wider than the knob, or a range of nothing, divides by nothing.
- [x] `AppleColours.kt`, `SettingsPage.parent` and the tiles, `SliderGeometry`.

### Part 2: the page

- [ ] `FullPage`: solid, in the page colour; Back an accent chevron and where it goes; the close a
  round grey button; the large and small titles in Apple's words.

### Part 3: the parts

- [ ] `Section`; the iOS row for `NavRow`, `ActionRow`, `ToggleRow`, `ChoiceRow` (a check in the
  accent), `StepperRow` (Apple's − + pair) and search results; the shade while pressed.
- [ ] `Toggle`: iOS's switch. The slider: a thin track, the accent up to the knob, a white knob.
- [ ] `Segmented`: grey track, solid pill; held, it swells, slides, ticks and settles (no lens).

### Part 4: the pages

- [ ] Settings' first page: the search field, group names, tiles.
- [ ] Theme, Home, Motion, Advanced animation, Gestures, Apps, Island, Looks, Aura, Account, Backup
  and About in Sections; `StudioKit` tiles and chips; nothing left of glass on a Settings page.
- [ ] The widget gallery and the showcase, plain.

### Part 5: the buttons

- [ ] `ZenithButton` (Main with the Jelly squish, Grey, Plain); every `GlassButton` moved over;
  `PrimaryButton` and `QuietButton` drawn by it.

### Part 6: check and ship

- [ ] README; build and every unit test; pictures in light and dark on a throwaway emulator
  (Settings' first page, Theme, Home, Motion, the gallery, a showcase, a sheet with switches and
  buttons); commit and push to `apple-look`.
