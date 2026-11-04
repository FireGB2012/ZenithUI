# Apple look, step 2: the home screen — implementation plan

**Goal:** Zenith's widgets are solid tiles, as an iPhone's are: Apple's dark grey or white, a colour
of their own when given one, the weather in its sky and the music in its album's colour. The
"No glass" widgets and the glass-ink they were drawn with are gone, and any already placed become
their solid selves. Search is the iPhone's small glass pill above the dock, with a round glass
button for Google, and notification dots are Apple's red badges.

**Architecture:** One frame for every Zenith widget, `ZenithWidget`, now a `solidCard`. What fills it
comes from `LocalWidgetFill`: nothing (Apple's card), the widget's own colour (`InOwnColour`, which
also gives the ink that colour needs), or what a widget paints for itself (Weather's sky, Now
playing's album colour), each giving its words white ink. The type of a widget is cleaned where
every layout is read, `homeItemFromJson`, so pages, stacks, backups and Looks all come through it.
Pure rules with tests: `Widgets.solid`, `WeatherSky`, `AppleColours.underWhite`.

Spec: `specs/2026-11-04-apple-look-design.md`, "2. The home screen".

## Global constraints

- As step 1: no new dependencies; old layouts, backups and Looks read; nothing on Gabriel's phone
  unless he asks; `./gradlew :app:testDebugUnitTest :app:assembleDebug` after each task; pictures on
  a throwaway emulator at the end.
- Every widget keeps its sizes and its designs; only what it is made of changes.
- The dock and folders stay glass, as they are after step 1.

## File structure

| File | Responsibility |
| --- | --- |
| `data/Models.kt` | `Widgets.solid`, applied in `homeItemFromJson` and `StackMember.fromJson`; the "No glass" list and `isBare` go |
| `ui/home/WeatherSky.kt` (new) | the sky a Weather tile is painted in, by kind of weather and day or night |
| `ui/theme/AppleColours.kt` | `underWhite`: a colour taken deeper until white words stand on it |
| `ui/home/ZenithWidgets.kt` | the solid frame and `LocalWidgetFill`; no `LocalBare`; figures, symbols and rings in plain ink and accent; no charging glint; the Search pill |
| `ui/home/MoreWidgets.kt` | no `LocalBare`; Weather in its sky; Now playing in its album's colour; the photo fills its tile |
| `ui/home/HomeGrid.kt` | `InOwnColour` fills the tile and gives its ink |
| `ui/home/WidgetGallery.kt` | the Glass / No glass switch and its note go |
| `ui/home/HostedWidgets.kt`, `ui/home/Sheets.kt` | another app's widget can have a tile behind it, not glass |
| `ui/common/Components.kt` | `IconBadge` is Apple's red badge |
| `ui/theme/GlassInk.kt` | deleted |
| `README.md` | the widgets, the gallery, search and dots as they now are |

## Tasks

- [x] `WidgetMigrationTest`: a `bare_` widget on a page and in a stack is read as its solid self,
  keeping its id, place, size and colour; `Widgets.byType` finds the solid spec for either.
- [x] `WeatherSkyTest`: white words stand 4.5 to one on every sky, by day and by night; a clear day
  is blue; night is darker than day; cloud and fog are grey; rain is darker than cloud.
- [x] `AppleColoursTest`: `underWhite` leaves a deep colour alone and takes a pale one only as deep
  as white words need.
- [x] `Models.kt`, `WeatherSky.kt`, `underWhite`.
- [x] The frame, `LocalWidgetFill`, `InOwnColour`; `LocalBare` and `glassInk` out of both widget
  files; the gallery switch out; `GlassInk.kt` deleted.
- [x] Weather, Now playing, Photo frame fills; rings solid, no glint.
- [x] Search pill and Google button; hosted widget's tile; red badges.
- [x] README; build and tests; pictures in light and dark on a throwaway emulator; commit and push.
