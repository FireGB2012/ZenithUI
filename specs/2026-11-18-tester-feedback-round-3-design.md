# Tester feedback, round 3

- Date: 2026-11-18
- Branch: `claude/project-thread-yjufje`, off `claude/project-thread-t91t0v` (PR #18, live wallpapers)
- Status: built. Version 106 / 3.8, for Play.

The same eight points testers made on 2026-10-25 (answered then in 219f3dc, shipped as 105) came
back. Since then the widget gallery, Quick Toggles, the Customise card and the themes were all
designed again, and some of the first answers went with the old designs. Points 9 to 12 were
praise and change nothing.

| # | Testers said | What changes |
| --- | --- | --- |
| 1 | The widget selection screen could use slightly larger descriptions | Every gallery card says what its widget is for (`WidgetBlurbs`, 14 sp, two lines kept so rows line up). The gallery's words go up a step: intro 15 → 16 sp, the style note 13 → 15 sp; in a widget's showcase the size note 14 → 15 sp, the description 15 → 16 sp, the small print 13 → 14 sp. |
| 2 | Settings subtitles could have stronger contrast against the dark background | Second lines are 84% of the ink in Dark, up from 74% (`ThemeColours.muted`); Light keeps 74%. `ThemesTest` now holds them to 7:1 on every dark page and panel, `PaletteToneTest` likewise. |
| 3 | The customization menu could have clearer spacing between its options | The Customise card's parts stand 16 dp apart with a fine line between them; its buttons are larger (14 sp, 14 × 10 dp inside) with 10 dp between them; the Colour / Glass choice is larger too. |
| 4 | Glass and No Glass could have more noticeable labels | The segmented switch becomes two cards under a "Widget style" heading: each name at 17 sp, what it means under it, a symbol on a disc; the one chosen has an accent ring and a lit disc. They are radio buttons to a screen reader. |
| 5 | Quick toggle icons could be slightly larger | About a tenth larger everywhere (`ToggleSymbols`): half a round button (was 0.44, now 19 to 32 dp), 58% of a tile's disc (was 50%), 26 dp on a card (was 23), 0.76 of a "No glass" button (was 0.7). Each still leaves a ring of its disc showing, which is how a control says it is on. |
| 6 | The wallpaper option could have a more descriptive preview | In Customise, "Wallpaper" was a word in a capsule. It is now a row: a picture of the wallpaper in use (Zenith's own copy, or the design painted afresh), its name and colours ("Aurora · Midnight colours") and what it does ("Changes with light and dark", "Live: moves gently on its own"), from `WallpaperSummary`. |
| 7 | The clock widget could offer a clearer distinction between its sizes | The size picker's shapes are larger (50 × 34 dp), the names 14 sp, with columns × rows under each. The clock's sizes each name their design in a capsule (`WidgetLayouts.style`: Digital, Analogue, Side by side, Stacked, Big face; Face only for the smallest). Its Small size said "Wedne…": it now says "Wed" when the whole day does not fit. |
| 8 | Settings categories could use more visual separation | On the first page of Settings each category is 20 dp from the one before (was 10), under a heading in a stronger ink (`CategoryHeading`, marked as a heading). Every group in Liquid Glass gets a hairline edge, and groups on every page are 16 dp apart (was 12). |

## Checked

Unit tests for each rule (`WidgetBlurbsTest`, `WidgetFormTest`, `ToggleSymbolsTest`,
`WallpaperSummaryTest`, `ThemesTest`, `PaletteToneTest`), then the whole suite. Before and after
screenshots of each screen from the JVM rig, on Aurora in Dark and Glow in Light.

Not checked on a phone: how the larger toggle symbols and the Customise card's new height feel
on the A55.
