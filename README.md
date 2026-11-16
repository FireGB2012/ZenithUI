# Zenith UI

A launcher for Android that looks like an iPhone on iOS 26, Liquid Glass and all, with twists of
its own, on top of Android's customisability — built entirely in Jetpack Compose.

Every screen is being rebuilt that way, one at a time
([`specs/2026-11-09-ios26-home-screen-design.md`](specs/2026-11-09-ios26-home-screen-design.md)).
The home screen is done. Folders, the menus you get when you hold something and edit mode come
next, then the app drawer, search's results, the widget gallery, Settings, setup, and last Zenith
Plus, Aura and the island; until its turn each looks as it did on 4 November 2026. Zenith has one
look, light or dark, with iOS blue for actions: the six colour themes are gone
([`specs/2026-11-10-no-colour-themes-design.md`](specs/2026-11-10-no-colour-themes-design.md)).

## First run

1. **Hello**, and then **your name**, which is used across the launcher. It can be skipped.
2. **Light or dark?** — Auto, Light or Dark. Zenith has one look, so there is no colour to pick.
3. **What goes on it?** — the widgets for page one (Clock, Toggles, Battery, Calendar, Note) and the
   grid: iPhone, which it starts on, Compact, Balanced or Roomy. A live phone shows each choice.
4. **Putting it together** — not a fake wait: while it runs, the launcher reads every launchable
   activity, picks a dock from your dialer/messenger/browser/camera, groups apps into folders by
   their declared category and places the widgets you chose, and the phone on screen is put back
   together a piece at a time, with a tick for each. The home screen it builds is on the new look:
   the grid you picked, the iPhone's icon shape, colour widgets and the glass dock.
5. **Zenith Plus**, where it is on offer: what it adds, the prices in full, and a "Not now" as easy
   to hit as the yes.
6. **Three quick moves** — swipe up, hold an icon, hold an empty spot — and the phone grows into your
   home screen.

## Home screen

From the top: Android's status bar; the page, with its widgets and icons; a small Search pill; the
dock. Nothing else is drawn: no greeting, no page dots, no search bar across the page. (Those
settings are still kept, for old backups and Looks.)

- **The iPhone grid** (Settings → Home screen → Grid → **iPhone**, `GridDensity.Four`) — four across,
  with rows spaced like an iPhone's: a row is never more than 1.09 times as tall as a column is wide
  (`HomeGeometry.IPHONE_ROW_RATIO`). On a tall phone the grid stands at the top, and what it does not
  need is left under its last row, above Search, as an iPhone leaves it. From 600 dp across, a tablet
  or an unfolded phone, it is Balanced. Compact, Balanced, Roomy and Custom are still there, with the
  numbers they always had.
- **Icons** are cut to an iPhone's rounded square, its corners continuous (the **iPhone** shape),
  72% of a cell's width (56 to 78 dp), a little bigger than an iPhone's, and 80% with **Larger
  icons, with no names** (Customise), which take the room their names had. Each app's logo stands
  off its tile as on iOS 26 (`LogoLift`): a soft shadow straight down under it, its face lit from
  above, and a hairline of light on its edge. Their names are white with a soft shadow on the home
  screen, in light and in dark, as on an iPhone; the drawer and folders keep their own ink. See
  `specs/2026-11-16-3d-logos-and-bigger-icons-design.md`.
- **Widgets** are square: Small (2×2), Medium (4×2) and Large (4×4), with one corner radius, each with
  its name under it like an icon's (another app's widget shows its app's name). On the iPhone grid a
  widget reaches a few dp past its icons at the sides (`HomeGeometry.widgetReach`), so a 2×2 is
  exactly square. Any other size still works, from the smallest a widget allows to the whole page.
- **Search** is a small pill of Liquid Glass, a magnifier and "Search", centred between the page and
  the dock (`SearchPill`).
- **The dock** is a floating slab of Liquid Glass, inset from the screen's edges, with an app for each
  column (four on the iPhone grid) and no names, its corners as round as an iPhone's. **Glass** and **Floating** are both that slab
  (Floating closes up round fewer than four apps); **Icons only** is the icons on the wallpaper.
- **Widget look** (Customise, and Settings → Home screen): **Colour**, solid like iOS's own widgets,
  each kind in colours of its own — the Weather a sky, blue by day, deep blue at night and grey under
  cloud, the Calendar a white page with a red weekday — and darker in dark mode; or **Glass**,
  Zenith's Liquid Glass tinted by the wallpaper, with white words in light and dark (`WidgetLooks`,
  `WidgetColours`). Other apps' widgets draw themselves either way.
- **A phone that already had Zenith** is asked once, the first time this version opens: "Zenith has
  a new look", with **Switch to the new look** (the iPhone grid, the iPhone icon shape, colour
  widgets and the glass dock; apps and widgets keep their order, though some may move to another
  page) or **Keep my home screen**, which changes nothing (`NewLook`, `NewLookSheet`). It needs one
  of the two answers and is not shown again; a backup from before it asks again. Until a home screen
  is on the new look, Customise offers **Use the new look**. A new install starts on it.

### The five twists

Zenith's own, on top of the iPhone's layout:

1. **Light that moves with you.** Tilt the phone and the glass catches the light on its rim and face,
   and icons, widgets and the dock float a hair above the wallpaper, moving a few dp against the tilt
   (the dock half as far), so the wallpaper reads as deeper. The way you hold the phone is the rest
   pose, not upright: it slowly follows how the phone is held, and a still phone stops redrawing
   (`rememberTilt`, `TiltRest`). **Tilt shine** turns the light and the floating off together.
2. **Glass made from your wallpaper.** The dock, Search and the glass widgets take on a tint of the
   wallpaper's own colour where each one sits, with a soft glow of that colour under it
   (`GlassTint`). Change the wallpaper and the glass follows.
3. **Liquid Search.** Tap the pill and its glass stretches into a wide search field that rides up on
   the keyboard, the page dimming and softening behind it (`SearchField`). Typing shows the apps that
   match, found the same way the drawer finds them (`AppSearch`), and the drawer's answers: a sum, a
   conversion, Android's settings, "Search the web for …". Tap outside or press back and the field
   shrinks back into the pill with a small wobble. While you swipe between pages the pill's word
   gives way to page dots, the current one long, and comes back about a second after the page
   settles.
4. **A drop on the glass.** Tap an empty bit of wallpaper, or double tap, and a ring of light runs out
   from the finger across the screen: every pane of glass lights up, rim and face, as the ring passes
   over it, and the icons and colour widgets catch a faint gleam (`GlassRing`).
5. **Jelly icons.** Press an app, a folder or a dock icon and it squishes and lights up from under the
   finger, then springs back with one small bounce (`JellyIcon`). On the home screen icons always do this;
   the Pressing setting applies everywhere else.

With movement reduced nothing floats, there is no ring, the search field fades in rather than
stretching, and a press is a small one with no wobble.

### Arranging it, and the rest

- **Free placement** — every page is a `columns × rows` grid; long-press any icon, folder or widget
  and drag it to any free cell. A drop hint shows where it will land.
- **Everything lines up** — icons and widgets keep to whole rows. A widget's frame runs from the top
  of its first row's icons to the bottom of its last row's icons, with its name under it in the strip
  the icons' names share, so the space under a widget is the space under any icon; across, it runs
  from the left edge of the icon in its first column to the right edge of the icon in its last, and
  on the iPhone grid a few dp past them. A full dock stands on the same columns as the page. One
  class, `HomeGeometry`, works out every position in whole pixels. (Zenith Plus free placement still
  puts things anywhere.)
- **Folders** — drop one app onto another to create a folder; drop onto a folder to add to it.
  Tap a folder for a bottom sheet with an editable name; long-press an app inside to take it out.
- **Remove** — drag to the "Remove" target at the top.
- **Resize** — in edit mode every widget has a corner to pull: a short arc of white round its
  bottom-right corner. Pull it and it follows your finger, while the widget snaps to whole cells
  and rows as you pass them, a thin outline shows the size asked for and a capsule of glass says it
  ("4 × 2"); it can be any size up to the whole page (a widget stops at the smallest its design
  allows). A size with no room turns the outline red rather than pushing anything aside. A whole
  pull is one change for Undo. An app gets the same corner from **Resize** in its menu, which starts
  edit mode; bigger than 1×1 it becomes a super icon, its icon stretched to fill the space. One of
  Zenith's widgets also has its named sizes in its menu (Small, Medium, Wide, Tall, Large), each
  taken where it stands or at the closest opening on its page (`Reshape.resized`).
- **App drawer** — a sheet that tracks your finger: drag up anywhere on the page and it follows,
  release and a fling (or the halfway point) decides whether it opens or falls back. Close it by
  dragging down anywhere — the handle, the header, the search row, or the app grid once it is
  scrolled to the top (nested scroll hands the gesture over) — or with back, or the X. The home
  screen blurs and scales behind it in step with the drag. The single-line search field sits at the
  *bottom*, next to your thumb; it searches installed apps and falls through to Google: enter opens
  the first match or, when nothing matches, the web search.
- **The drawer, your way** (Settings → Apps): A to Z, Most used (counted on the phone as apps are
  opened), Newest, or By colour (each icon's main colour, round the wheel, greys last:
  `DrawerSort`, `IconColour`); as many columns as the home screen or three to six. Along the top,
  tabs you make — **+** for a new one, a name and the apps ticked into it, a hold to change it —
  and **Work** on a phone with a work profile, whose apps open there. A to Z, a long list gets an
  index down its right edge: drag along it and a lens of glass beside the thumb shows the letter,
  the letters near it swell out like a Mac's dock, the list jumps with a tick at each.
- **Search that answers** — above the apps it finds: a sum (`2+2*3`, `15% of 80`, `80 + 15%`:
  `Calculator`), a conversion (`5 km in miles`, `30 c to f`, `2 cups in ml`: `Units`), each a pane
  that turns over as you type and copies at a tap; Android's settings by name ("blue" finds
  Bluetooth: `SettingsLinks`); and any app's shortcuts by their names. A search spans the work
  apps too. The Search pill's field gives the same answers.
- **Every menu is a bottom sheet** — item menus, Customise, wallpapers. Nothing needs a second
  hand. Settings and the widget gallery are pages of their own, the full height of the screen, on
  one shell (`ui/common/FullPage.kt`): a bar with a way back and a way out, pulled down to close.
- **Dock** — the slab of glass described above. It takes one app for each column of the page, and
  never fewer than four, and they share the slab out equally: a full dock has each app under the
  icons above it, and a smaller one spreads across the slab (`HomeGeometry.dockIconLeft`). Pin from
  any app's long-press menu.
- **Edit an app** — long-press it on a page, in the dock or in the drawer → **Edit**: a name of
  your own, an icon from any installed icon pack (a grid of the pack's pictures, the one it draws
  this app with first, with a box to find one by name) or one of your photos (Android's own
  picker, no permission), and **Hide name** for that one icon. It is kept per app (`AppEdit`), so
  the drawer, the dock, folders and search all agree, and a renamed app is found under its new
  name. A photo stays in Zenith's files; a backup taken to another phone gives that app its own
  icon back.
- **App shortcuts** — the top of an app's long-press menu lists what it offers from its icon (a
  route home, one chat), as Android's own launcher does. A tap runs one; the button beside it puts
  it on the page as an icon of its own (`ShortcutCell`), the shortcut's picture with the app's
  small in its corner, pinned for as long as it is there. Another app's "Add to home screen" —
  Chrome's, say — comes up as a card of glass over that app (`PinItemActivity`), and lands on the
  first free spot.
- **Notification dots** — a red dot, as on an iPhone, at an icon's top corner while its app has
  something to read, on the page, in the dock and in the drawer; a folder's is the sum of its apps'.
  Off, Dot or Count, under Settings → Home screen, which also asks Android for notification access
  the first time. `NotificationDots` only counts: nothing ongoing (music, a download), nothing an
  app asked not to badge, and nothing of what they say is kept or sent. A dot springs in with the
  first notification and shrinks away with the last.
- **Icon style** — **Original**, **Tinted** (each app's one-colour symbol in the words' colour on a
  plain dark or white tile, as Android 13 does) or **Glass** (the symbol on a tile of clear glass, the
  way iOS draws clear icons). In the drawer, an open folder and a folder's tiny icons, where it
  already lies in glass, a Glass tile is glass lying in glass (`glassOnGlass`): a light frost, the
  same hairline of light on its edge as every pane, moving with the tilt, a soft shadow straight
  down, and in Light a soft grey band round its rim. The symbol is the adaptive icon's monochrome
  layer where the app has one, its foreground where that is a logo on its own, and otherwise what
  stands out of the icon from its own tile (`IconLoader.loadGlyph`). An icon you chose yourself
  stays as you chose it.
- **Shapes and names** — nine icon shapes (iPhone, Squircle, Circle, Rounded, Square, Pebble,
  Teardrop, Hexagon, Clover), one path each in `IconShapes` for the pictures and the tiles alike;
  names in three sizes inside the same strip, so nothing on the page moves, and **Name shadow**, a
  darker shadow under the names and Search for a pale or busy wallpaper.

## Gestures

Settings → **Gestures** has a row for each gesture on the home screen and what it does now; a
row opens the list of what it could do instead (`GestureActionSheet`): nothing, the notifications,
the quick settings, lock the phone, all apps, search, the torch, the bedside clock, Customise,
Settings, Widgets, the first page, the page overview, or any app — and under an app, its shortcuts.

| Gesture | Unless changed |
| --- | --- |
| Swipe down | Notifications |
| Two-finger swipe down | Quick settings |
| Two-finger swipe up | Nothing |
| Double tap | Nothing |
| Pinch in | Customise |
| Pinch out | Page overview |
| Home button, when already home | First page |

Only what is changed is kept (`Gestures.with`), so a gesture nobody touched follows Zenith's own
choice. Swipe up is always the apps and holding an empty spot always Customise, so Settings can
always be reached. A swipe down pulls the page after the finger on a band that gives less the
further it goes, while a pill comes down from the status bar saying what letting go will do and
lights up, with a tick, once it will; a tap on the wallpaper, or a double tap, sends a ring of light
across the screen (the fourth twist). The
home button counts as the gesture only when Zenith was already in front with nothing open on it,
as Android's own launcher tells the two apart; on the way back from an app it folds everything
back, and goes back to page one unless the app is closing into an icon on the page on show, which
it stays on so the app can be seen going into it.

Locking needs the **lock helper** (`LockService`), an accessibility service that does nothing else:
it asks for no events and cannot see the screen. A sheet says so before Android's page opens. The
shade and quick settings are pulled down the way Android's own launchers do it
(`SystemPanels`), with the helper as the way round a phone that refuses. The torch is one `Torch`
for the gesture and the Quick Toggles widget alike, which now knows when the torch was turned on
anywhere else.

## Built-in widgets

| Widget | Size | What it does |
| --- | --- | --- |
| Zenith Clock | 2×2 | A real clock face, paper by day and dark by night, with an orange second hand; the bigger sizes add the time in figures, the date, a greeting that uses your name and your next alarm |
| Power | 2×2 | The charge as a ring from `ACTION_BATTERY_CHANGED`: green, yellow in battery saver, red when low, a light running round it while it charges; the bigger sizes add how long to full, where the power comes from, the temperature, the health and the voltage |
| Calendar | 2×2 | The iPhone's: the weekday in red over the date, large, and today's events from the next one on, each a card with a bar in its calendar's colour and when it is ("5:30 PM · In 18 min", red while it is under way); Wide adds the week, Tall and Large the month. Until Zenith may read the calendar, "Show your events" stands where they would be and asks. The date turns over at midnight |
| Quick Note | 2×2 | A note written on the widget itself, its first line set as its title, persisted with your settings |
| Quick Toggles | 2×2 | Set out as Control Center is: big round buttons, tiles with names and On / Off where there is room, and a +N button for the rest. Each lights up in its own colour while it is on, read live from Android (`ToggleStates`); the torch works (Camera2), the rest open their panel |
| Now playing | 4×2 | The album's picture on a soft shadow of its own colour, the song, three bars moving while it plays, how far in and how long is left, and back / play / next, from Android's media sessions (`NowPlaying`, through the dots' notification access); the picture crosses over as the song changes |
| World clock | 2×2 | Up to four cities, chosen from every time zone, each a clock face, paper where it is day there and dark where it is night, with its time and how far it is from yours ("+9 h, tomorrow") |
| Countdown | 2×2 | The days to a date you name, in rolling digits, and the whole wait as a field of dots, those gone lit: a dot a day, a week or a month, as many as the room holds; "today" on the day, and the days since after |
| Photo frame | 2×2 | Up to twelve of your photos, picked with Android's own picker and kept small in Zenith's files, edge to edge; a different one each time you come home, crossing over, and the next at a tap |
| To-do | 2×2 | A short list in blue: what is left first and how many, a tick that fills and draws itself, the ticked ones sliding below the rest, a line to add one |
| Aura | 2×2 | Your streak under a drawn flame, this week ticked off day by day, and your aura |
| Weather | 2×2 | The iPhone's: the place, a big temperature, a drawn sky and its words, and today's low to high on a bar coloured by the temperature; Wide and Large chart the next hours, Tall lists them, and Large adds the days ahead. For a place you choose (`WeatherRepository`, Open-Meteo) |

They are one family, drawn from scratch to one design (`ui/home/widgets/WidgetKit.kt`: one grid,
one type scale, one hue each, symbols drawn rather than borrowed; the why is in
`specs/2026-11-16-widgets-redesign-design.md`). Each of them comes in both widget looks, Colour and
Glass, has a No glass version and a design for every shape of space, fitted down to a smaller cell
rather than cut, and says in the gallery what each size shows. What they need is set from their own menu: cities,
a date, photos, a place. **Weather is the one thing in Zenith that sends anything without an
account:** nothing until a place is searched for; then what was typed, to Open-Meteo's place
search, and from then on the chosen place's latitude and longitude, to its forecast, at most every
half hour while the home screen is shown (the next days come in the same answer). Nothing else goes
with it, and the sheet says so before anything is sent. **The Calendar widget** reads today's events
from the phone's own calendar, once you allow it from its "Show your events"; they are drawn on the
widget, and nothing of them is kept or sent (`CalendarRepository`).

**Search** is not a widget: it is the pill above the dock, on every home screen (the third twist).
Setup no longer offers a Search widget, and the setting for the old bar across the page is kept but
not drawn.

**The gallery** — long-press an empty spot → **Customise** → **Widgets**
(`ui/home/WidgetGallery.kt`). Every widget is drawn by the code that draws it on the home screen,
at the size it is there, on a window onto your wallpaper, so what you see is what lands on the
page. Tap one and it grows into a showcase: its sizes in a row, all to one scale — Small, Medium,
Wide, Tall and Large, each fitted to the widget and to your grid (`WidgetSizes`), and each a
different design — with a switch between Glass and No glass and one button that adds the size in
the middle. Under the row the sizes are named, each over the room it takes on the page: a little
grid as wide as your page, its cells filled in (`SizeShape`), so Wide is plainly the width of the
page and Tall a row more. A tap on a name brings that size to the middle, and under it a line says
what that size shows ("A clock face with its hours round it, and the day on it"). The lines come from
`WidgetLayouts`, which reads the same rules the widgets lay themselves out by (`WidgetForm`, and
`ToggleGrid` for the toggles), so they cannot say a size shows what it does not. The row of sizes
in a widget's menu shows the same little grid on each of them.

The switch between Glass and No glass carries a symbol on each side, a pane and a pane struck
through, and what the side chosen means is said right under it. It is about the No glass versions
below; Colour or Glass for every widget at once is the widget look. The gallery itself is still the
4 November one, until the widget picker's turn in the rebuild.

**No glass** — the other side of that switch: the same widgets with no pane round
them, like the clock on an iPhone's Lock Screen: the same layouts, with the big numbers, rings,
dials and symbols made of glass themselves, straight on the wallpaper. Their words are white with a
soft shadow. They are
stored as `bare_<type>` (`bare_clock`, …) and otherwise behave exactly like the widget they are a
version of.

**Words that can be read.** A widget is read at a glance, over a wallpaper, so its second line (the
date, "On battery", "25 days left") is set stronger than the muted grey a page uses, nothing in a
widget is smaller than 11sp at its ordinary size, and on the wallpaper (through glass or in "No
glass") small coloured words are in the ink, as an iPhone's clear widgets have them, the colour
staying in the marks beside them (`wordHue`): the picture behind can be any colour, and only the ink
reads on all of them. The Quick Toggles carry names only on their tiles,
beside the symbol, as Control Center does; a name too long for its tile takes its short form
("Focus" for Do Not Disturb), and if even that will not fit, every name in the widget shrinks
together, never under 12sp (`ToggleNames.fit`). A round button carries its symbol alone. Everywhere
else, second lines and small print (the subtitles in Settings, the words
under a choice) are 74% of the ink, which stands six to one or better against the page
backdrops, light and dark (`PaletteToneTest`).

## Other apps' widgets

The gallery's **From your apps** lists every app on the phone that has widgets, A to Z; an app
opens out to its widgets in a row, each its picture, its name and the cells it takes on this grid
(`HostedWidgetPicker`). A tap puts it on the first page with room, at the size it asks for —
Android 12's own cells where it gives them, otherwise its size in dp turned into cells and whole
rows (`HostedSizes`). Android asks once whether Zenith may show widgets, and a widget with a set-up
screen opens it; backing out of either puts nothing on the page. An app's own "add this widget"
comes up as a card of glass over that app, the same one shortcuts use.

On the page it is the app's own view in Zenith's frame, with its app's name under it (`HostedWidgetCell`, through one
`AppWidgetHost`, kept up to date while the home screen is on show), told its size in dp, with an
optional pane of glass behind it from its menu, off unless asked for since most widgets bring a
background of their own. It keeps its taps and scrolls: a hold that does not move is watched from
above without taking anything (`holdToCarry`), and only then is it picked up, carried, resized
with the same corner and bar, and taken away like any widget, giving its number back to Android.
Only one view may draw a widget at a time, so the grey copy under a finger and the small home
screen in Settings show its picture. A backup brings widgets onto another phone as a pane saying
"Tap to put Spotify back", which does, in the same place and size.

**Stacks** — any widget's menu has **Stack another widget here**, which opens the gallery to choose
one, Zenith's or another app's, to go on top at the same size (`StackCell`, `StackRules`). A swipe
up or down on a stack brings the next, the one leaving shrinking back under it; small dots at its
edge come up while it is swiped and fade after. Its menu takes any widget out onto a spot of its
own, and one left alone stands as a widget again. Eight at most.

## Customisation

Long-press an empty spot → **Customise** → **Settings**. Settings is a page of its own, the full
height of the screen (`ui/settings/`): a search box and ten groups, each saying how it is set
now — Wallpaper & appearance, Home screen, Motion & feel, Gestures, Apps, Zenith Plus, Looks, Aura,
Backup & reset, About & help — and an eleventh, Account, in a build that has a server. They stand in three blocks, each
headed with what it holds (Look and feel; Plus and Aura, or Plus, Aura and account; Backup and
help) and set further from
the next than its rows are from each other. A group pushes in from the side; back, the arrow or a swipe from the left edge pops it.
Search finds any option by its name or by other words for it ("dark", "vibrate", "battery"), opens
its page and pulses the row. The Wallpaper & appearance and Home screen pages start with a small
live copy of your own home screen, drawn by the same code as the real one, so a change is seen as it
is made. Wallpaper & appearance holds light or dark (Auto, Light or Dark: pages and words change, and
the home screen's glass stays the same), the font, the wallpaper, the glass and Tilt shine; there is
no colour to choose.

**Aura** is one of those pages (`ui/settings/AuraPage.kt`): the balance; the streak as a ladder of
seven days, each with what it pays; your code to copy or share, and the box for somebody else's.
`Aura.ladder` is a plain function with tests. With the colour themes gone aura has nothing to buy
until Aura is rebuilt, the last step of the rebuild; themes bought with it are kept, and stay among
a wallpaper's colour sets.

Every option is listed once in `SettingsCatalog`, which is what the search reads. Any Nova- or
ADW-compatible icon pack installed on the device is detected and its `appfilter.xml` is parsed.
The grid is **iPhone** (four across, as above), Compact, Balanced or Roomy — the last three a cell
size, from which the columns and rows are worked out for the screen — or **Custom**: columns and rows of your own, three to six across and three to
eight down on a phone and more on a bigger screen (`GridDensity.resolve`). Changing the grid
re-flows the existing layout instead of dropping items, and **Tidy up** puts anything out of line
back in its place.

Every choice says what it does: the four tiles of Customise each carry a line, and under each
control in Settings a line of small print describes the option that is selected. The Wallpaper
tile shows the wallpaper you have, and says which of Zenith's designs it is. Under the tiles are the
**Widget look** switch and, while the home screen is not on the new look, **Use the new look**; the
rows under them sit in a group of their own, a line between each.

## Wallpapers

Zenith paints its own wallpapers on the phone (`WallpaperArt`, `WallDesigns`, `WallDepth`):
forty-one designs on five shelves — Depth, Soft, Lines, Shapes and Scenes — nine of them for Zenith
Plus. Every design comes in a choice of colour sets (thirteen free ones round the colour wheel, six
of them once Zenith's themes, any bought with aura, colours of your own made before the themes went,
and colours taken from your wallpaper) and has a light picture and a dark one, each made so labels
and the status bar stay readable over it.

**Depth**, the first shelf, is eight designs with a body to them, as the wallpapers that come with
an iPhone or a Galaxy have: Iris, Layers, Satin, Marbles, Liquid and Twist, and for Plus, Loop and
Eclipse. They are lit as things in the world are, light added as light, their colours run between
the set's two accents round the colour wheel the short way in OKLab (`DepthKit`), so a gradient stays
vivid where the same two colours mixed as RGB go grey. They are worked out a pixel at a time on all
the phone's cores; the ribbon in Twist is drawn as a mesh. See
`specs/2026-11-16-depth-wallpapers-design.md`. Tap one and both pictures are shown side by side, each with
your own home screen on it, drawn by the code that draws the real one in the ink it takes in that
light. Its glass is frosted there, as over any wallpaper Zenith cannot see: the real glass will
bend the new wallpaper, which is not behind it yet.

A design, once set, **follows light and dark** (the switch is on the wallpaper sheet and under
Settings → Wallpaper & appearance). Go from light to dark, by hand or with the phone on Auto, and
`WallpaperDirector` paints the same design again in its colour set's other light, fades it in over
the old wallpaper, moves the glass onto it half way, and then hands it to Android — which cannot
fade between wallpapers itself. Only where a run of changes stops is painted. A design keeps the
colour set it was set in (`wallpaperPalette`). One that followed the theme before the themes went was
pinned, the first time this version ran, to the colours the phone had then, so no wallpaper changed
colour or was painted again on the update.

Zenith only ever repaints a wallpaper it set and that is still on screen. A photo, or anything
chosen in another app, is never replaced, and choosing one ends the following.

## Looks

A **Look** is the whole home screen saved under a name (`data/Looks.kt`, Settings → Looks): light or
dark, the glass, the wallpaper design and its colours, the icon shape, pack and style, names and
badges, the grid, the dock, the widget look, the layout, the motion and the gestures. It no longer
changes Zenith's own colours, since there is one look: a Look saved with a theme puts on everything
else, and its theme is kept, unused. Nothing of yours goes in
one: not your name, your note or lists, the countdown, your hidden apps, Aura or Plus. A Look is
kept as the very JSON the settings and layout are saved as, so one made today still puts on when
settings have grown; one from a newer Zenith puts on all this one knows. Putting one on cannot
change anything a Look does not keep (`Looks.KEYS`), so a Look written by hand cannot give anybody
Plus or Aura.

Tap a Look and Settings goes, the first page comes round, and the home screen flies in wearing it,
the way it arrives after an unlock; its wallpaper design is painted again in its colours. A Look
row shows whether it is light or dark as three dots, and which one is on now; its menu puts it on, saves the home
screen over it, renames, shares or deletes it. Twelve are kept, and they go in backups (an older
Zenith reads past them); restoring adds the backup's Looks to the phone's.

**The schedule** puts a Look on by itself: a Look, from when until when, on which days. A slot that
ends before it starts runs past midnight and belongs to the day it began. A Look comes on as its
slot begins — waiting, if you are using the home screen, until nothing has been touched for a
moment — or as you come home during it, and only once a slot: one you choose by hand in the middle
of a slot is left alone until the next (`LookSchedule`).

**Sharing** needs an account: a Look goes to the server without anything that only works on this
phone — other apps' widgets, shortcuts, photos chosen as icons — and without the names you gave
apps, and comes back as a code, `LOOK-` and six letters, to copy or send. Anybody can open a code,
with no account and no Plus; a Look from another phone leaves out the apps this one has not got.
A shared Look keeps its code, which gives it as it was when shared; **Stop sharing** takes the
code back, and deleting the Look or the account does too. The few Looks under **Featured** are
the ones the server's keeper chose (`zenith_server.py --feature CODE`): nothing anybody shares is
shown to anybody else unless somebody chose it. The list, like a code, is asked for only when you
tap: with no account, nothing goes to the server unless you ask.

Saving, switching, scheduling and sharing are Zenith Plus; opening a code, renaming, stopping a
share and deleting are not. Photos chosen as icons are kept while the home screen or any saved
Look shows them, and let go of when nothing does.

## Fonts and the Glass studio

**Font** (Wallpaper & appearance): System, Inter, Condensed, Serif, Mono, Typewriter, Casual, Hand or
Small caps, for everything; **Numbers** sets the clocks, the battery, the countdown and the weather
in the same font, in Thin, or in any other (`ui/theme/Fonts.kt`). Inter and JetBrains Mono are
carried in the app, unchanged, under the SIL Open Font Licence (About & help → Fonts shows it in
full); the rest are the phone's own fonts asked for by name, and a phone without one shows its
usual font. Every style Zenith sets text in takes the font — the app's own type styles, and the
ones built by hand for names, fields and widgets — so nothing is left in a font that was not chosen.

**Glass studio** (Plus, `GlassTuning`): bend, frost, rim light, rainbow edge, and where the light
comes from — Zenith's own, up and to the left, or anywhere on a dial — and whether it swings as the
phone tips. Every change shows at once on the sheet's three
panes, on the sheet and on the home screen behind it; Reset puts Zenith's glass back. The defaults
change nothing (`GlassTuningTest`), and without Plus the glass is Zenith's own again, the tuning
kept.

**Each Zenith widget** can have a colour of its own (its menu → Colour): twelve, or None. In the
Colour look it is the card's fill, its words in whichever ink stands on it; in the Glass look it
tints the glass in place of the wallpaper's colour. With None, a colour widget keeps its kind's own
colours and a glass one takes the wallpaper's. The widget eases into a colour and back out of it.

## Motion

Everything that moves is a spring, and anything that can be worked out without a screen is a plain
function with a test: `MotionLevel` (how much moves), `PageEffects` (where a page is drawn part way
through a swipe), `FlyIn` (when each item arrives), `LaunchMotion` (where an opening app's card is),
`SheetPull` (where a sheet sits under a finger, and whether letting go puts it away), `Unpack` (how
far out of an opening folder each app is) and `AnimationTuning` (the numbers it all runs on).

- **Opening an app.** The icon grows into a card that fills the screen, and the app appears over
  it. The card's middle travels across and down on springs of their own while a third grows it, so
  it takes a slight curve rather than a straight line, with a soft shadow under it on the way.
  Behind it the home screen sinks back about the icon that was tapped, dims and goes out of focus,
  and the icons nearest make way. Tap the card, or press home, and it turns round where it is.
- **Closing an app.** The app goes back into its icon whichever way the phone allows
  (`CloseRules.route`; the design is `specs/2026-11-16-app-open-close-animations-design.md`, and
  `specs/2026-11-16-closing-on-every-phone-design.md` for phones like a Galaxy). With gesture
  navigation on a Pixel-type phone, Android asks the home app where the icon is
  (`GestureHandshake`, Launcher3's `GestureNavContract`) and springs the app's own window into it;
  the icon swells a little as it arrives. Elsewhere, with **App pictures** on, a card holding a
  picture of the app shrinks into the icon once Android's own close is done, the picture giving way
  to the icon's colour and logo as it gets small. With neither (One UI, buttons, or the copy of
  Zenith that installs by tapping it), the card the app opened on, in its colour and with its logo,
  shrinks back into the icon from 72% of the screen. Without the handshake Android plays its own
  close of the app first, over Zenith, so the home screen waits for it, empty, until Android says
  it is over (`AndroidClose`, `specs/2026-11-16-waiting-for-androids-close-design.md`; before
  Android 14 the usual length for that make of phone), and then the icons fly in as the app goes
  into its icon. Advanced animation settings → *Wait for Android's close* sets it by hand. With the
  handshake it starts once Zenith can be seen (its window's focus, or 450 ms after it resumed). The
  home screen comes forward again on a spring that carries it a little too far, the app's icon
  lands in its place with a bounce, and the icons round it are knocked back and swing home. The
  icon is the one it was opened from, or that app's icon on the page on show or in the dock, or the
  folder holding it (`IconPlaces`, which keeps where each sits at rest); with none, the card shrinks
  into the middle of the screen and fades. Settings → Motion & feel → **Closing apps** has *Into
  its icon* and *Plain*: Android's own close, then the landing, which is how it was.
- **App pictures** (`PictureService`, Android 11 and later, off until switched on in Accessibility;
  only in a copy of Zenith that has it, so not the one that installs by tapping it, since Play
  Protect refuses a sideloaded app with an accessibility service). An accessibility service that
  does nothing else: 0.7 s after an app's screen comes to the front,
  and every 3 s after while it stays there, it takes a screenshot, shrinks it to at most 1280 px
  and keeps it in memory (`AppPictures`) for the last four apps, five minutes at most. A picture
  only counts once the app has stayed in front 2 s after it, since a swipe home moves the app before
  Android says so. It asks for one kind of event and cannot read the screen; never Zenith, System
  UI, the lock screen or a black picture from an app that hides itself; all are dropped when the
  screen goes off. Opening an app with a picture from the last five minutes, the picture fades in
  over the card as it grows, so the app comes out of its icon as itself.
- **Advanced animation settings** (Settings → Motion & feel). Every one of those numbers can be
  changed: stiffness and damping for each spring, the arc of the path, how far the card has grown
  when the app starts, corners, logo and shadow, the home screen's zoom, dimming, blur and how far
  icons are pushed, and the return, the landing and the ripple. Each has a slider and a minus and a
  plus that move it one step, a dp or a hundredth at a time. Six styles to start from — Zenith,
  Fluid, Bouncy, Dynamic, Sweet and Classic, the last being how it was before any of this — one
  slider for the speed of everything, and a small screen at the top of the page that plays whatever
  is set. Only what is changed is saved (`AnimationTuning`), and with animations reduced none of it
  swings, bounces, curves or zooms.

- **Arriving.** On a cold start and after an unlock the home screen flies in: every item starts
  out from the middle and comes in to its place, the middle of the page first, inside 600 ms.
  Coming back from an app it is the same movement over a fifth of the distance.
- **Pages.** Four ways to turn one — Depth, Slide, Cube, Stack — and, while one turns, page dots in
  the Search pill, the current one long.
- **Carrying.** The copy under the finger lifts and casts a shadow; everything else on the page
  rocks, each item out of step; a tick as the landing spot changes; a ring where it lands. Put
  down, the thing itself comes back to its own size from the size it was carried at. Held over
  another icon, a plate spreads out under that one: the folder the two will make. Let go into the
  bin, a folder or the dock, the copy shrinks away into it, and anything taken off the page,
  however it was taken, is seen to go. The bin comes down from the top edge, tips and reddens when
  it is ready, and goes back up.
- **The dock.** Its icons never jump: the others close up round a gap or make room, and the one in
  your hand settles from where it was let go, each starting from wherever it was last seen.
- **Folders** grow out of their own icon into a pane of glass and go back into it, and their apps
  come out one after another as it opens.
- **Widgets** animate their own changes: rolling digits, a ring that springs to its value and
  glints while charging, every other gauge filling the same way as it appears, today's dot drawing
  itself in, toggles that fill with their own colour from the middle as they come on.
- **The drawer and sheets.** The drawer is put together just after the home screen arrives, out of
  sight, so the first swipe up is as smooth as the tenth. Every app's icon is drawn at the drawer's
  size once, on a background thread, as soon as the app list is known, and kept in memory
  (`IconLoader.warm`); only an app installed, updated or removed is drawn again, when Android says
  so. A frame of the swipe composes nothing: the drawer's progress is read only where it is placed
  and drawn, the rows rise in each time it opens on one clock (`DrawerRise`), and behind it the home
  screen frosts over by a copy of it blurred once and faded in (`DrawerFrost`), its light held still
  while the drawer moves. Its title and count turn over as you search, and a wallpaper's preview
  grows out of the thumbnail that was tapped. A sheet comes up from the edge on a spring and follows a finger: down as far as
  it is pulled, up only a little. Let go and it settles, or, past a third of its height or with a
  flick, goes. One sheet opened from another is seen going down as the new one comes up.
- **Settings and the gallery.** Settings arrives in order — its title, the search box, then each
  group in turn — and a row answers a press. The pill of a row of options stretches towards the
  one that was tapped and draws itself up there; a switch's knob spreads under a finger; a number
  rolls up or down a step; the small print under a control changes where it stands. The gallery's
  cards come in one after another, and a widget's showcase grows out of the card that was tapped
  and goes back into it. In the showcase the pill under the names of the sizes goes with the row
  of sizes as it is pulled, stretching towards the next on the way and drawing itself up there
  (`PageEffects.pillSpan`); that size's little grid lights up in the accent, and its name below
  slides in the way the row went.

Movement goes through `graphicsLayer` or draw-time state rather than layout, except where glass
has to be told where it is: a sheet, and the bars that come in from an edge, are moved by being
placed. Anything that runs for a while runs on one clock per page, so nothing is left running once
it has settled (the one exception is the charging glint, while there is a charger).
**Animations: Reduced** in Settings, or the phone's own *Remove animations*, turns all of it down
to quick fades: no fly-in, no rocking, no tilt; sheets and full-screen pages fade in where they
stand, a sheet still follows a finger but settles without a spring, and home pages simply slide.
Haptics go through one `Haptics` helper — a tick, a pick-up, a confirmation — and are silent when
the switch is off.

## More motion

- **Page swipe** has eight turns (`PageEffects`): Depth, Slide, Cube and Stack, and Carousel (pages
  on a turntable), Wheel (round a great wheel under the screen), Flip (a card turned over where it
  stands) and Fold (folding in at the crease). Each is a tile in Motion & feel with two little
  pages caught part way through it, drawn by the same transform, and plays through once when tapped.
- **Coming home**: from the middle (as before), from below, or in a cascade a column at a time
  (`FlyIn.start`). **Opening the drawer**: Rise, Grow, or Glass, clearing where it is.
  **Pressing**: Bounce, Squish (soft, and it wobbles back), or Ripple, a ring of light from the
  finger that falls only on what was pressed; on the home screen icons are jelly whatever it is set
  to. All of it gives way to plain fades with movement reduced.
- **Glass under a finger** (`GlassPress`). Glass buttons move like glass, each its own way, and
  not by the Pressing setting: glass buttons, plain and in the accent's tinted glass, swell and
  light up from the fingertip, as iOS's do (Flex); clear glass over the wallpaper or the dark (the
  round search button, the bedside clock's button) and the Quick Toggles swell and lean towards the
  finger (Lean). Every press springs back as Apple's glass does, one small bounce past its rest and
  still, never a wobble. The pill of a
  row of options can be picked up: it turns into a lens of clear glass that swells, follows the
  finger along the row, stretching the faster it goes, ticks as each option comes under it, and
  settles on the nearest, which is then the one picked. Only how they are drawn changes, and the
  wallpaper behind stays where it is on screen. With movement reduced a press is only a small
  change of size, and a lens does not stretch.
- **Page overview** (pinch out, or any gesture set to it): every page small on a tray. Tap one to
  go there; hold and slide one to move it, the others stepping aside; flick one up to take it away
  (an empty one goes at once, one with things on it asks); the last tile adds a page (`PageOrder`).
- **Carry many**: holding one icon, tap others with another finger; each flies into a stack under
  your finger with a count on it, and they are put down round where you let go, side by side
  (`LayoutBuilder.landBeside`). Into the bin, they all go.
- **Undo**: anything taken off the home screen leaves a pill of glass for five seconds, a ring
  running down round Undo. Undo puts it back where it was, or in the first opening, springing out
  of its spot; until then other apps' widgets keep their numbers and shortcuts their pins
  (`LayoutBuilder.putBack`).

## Home screen details

- **Dock**: the glass slab, **Floating** (the same slab, closing up round fewer than four apps), or
  **Icons only**; and a switch that hides it, keeping its apps for when it is back.
- **Folders**: Grid, Stack (the first three fanned) or Cover (the first app large), and a colour
  of its own from a folder's menu.
- **Lock layout**: a hold only opens an item's menu, the dock's icons stay put, and every menu
  offers to unlock. **Hide the status bar**: it comes in for a moment from the very top.

## The island

**Island** (Zenith Plus, Settings → Island) is the camera, stretched: black round the camera hole,
drawn over every app (`island/`). At rest it is exactly the camera and nothing more. When
something happens it grows out of the camera on a spring, with what it shows in its two rounded
ends, either side of the lens, and nothing over it:

- **Music**: the cover, and bars in the cover's own colour. Tap for the song, how far through it
  is, and back, play and next.
- **Calls and timers**: any app's ongoing notification with a running clock, a call's time in
  green and a timer's or stopwatch's in orange. Tap to open the app.
- **Notifications**: the app's icon and who it is from, for five seconds. Tap to read the message
  in a card; tap the card to open it. Only what Android itself would alert you to: not silent or
  minimised notifications, not what Do Not Disturb is holding back, not "running" notices,
  downloads or updates that do not alert again. For a chat, the newest message and its sender.
- **Charger** and **ring or silent**: for a moment, as they change.

A moment outranks a running clock, which outranks music (`IslandRules`); what else is going on
splits off into a small bubble beside it. An opened card drops below the status bar, joined to the
island by a short neck so the two read as one shape of black, and folds back into the camera at a
tap anywhere else.

All of it is measured from the phone: the camera's size and place from the display cutout, and the
status bar's height from the window insets (`IslandGeometry`). Its height is the camera's and a
hair, the icons sit in the middle of its rounded ends, and it grows to at most 46% of the screen's
width, which keeps it clear of the clock and the status icons. Its words are in the phone's own
sans, like the status bar beside it, whatever font the home screen is in.

Its page has a live preview, the top of this very phone at its own size with the same island on
it, and buttons to try music, a call, a notification, silent and the charger; then what it shows,
**Always show it** (a small pill even with nothing going on), and keeping out of the way in full
screen and sideways. It needs Android's "Display over other apps", and notification access for
music, calls and notifications; the page asks for each.

`IslandService` keeps one small window round the camera while the island is on, Plus is active and
Android allows it, which also keeps it running, and a second one while a card is open. Three
things Android decides: a window over other apps that takes no touches is drawn at most 80% solid,
so the island's window takes touches whenever it is in sight; the status bar sits above every
app's window and keeps the touches inside it (on the Galaxy A55 a tap on the island itself went to
the status bar), so the island also takes taps on a strip just under it; and Android's own
Settings hides every app's window over it. It reads only what the phone's notifications and music
already show, keeps nothing and sends nothing.

## How the glass works

`Modifier.glass()` is Zenith's Liquid Glass, modelled on Apple's own description of it and on the
pictures Apple published of it, in two finishes: **Clear** for the dock, folders and small
controls, which only mists the wallpaper and bends it most, and **Regular** for widgets, search and
sheets, which frosts it so text on top stays readable. A third, **Tinted** (`glass(tinted = true)`),
is the accent's own glass, for the one button that moves you on: clear glass's bend under about
three fifths of the accent, and with nothing readable behind it a body of the accent rather than a
white one. A pressed pane bends the light a little more as the finger's light comes up. On Android
13+ an AGSL shader (`LiquidGlassShader`), one lens shared by every pane over the same copy of the
wallpaper, draws each pane, and does these things to the wallpaper round it:

- **It bends it.** The flat middle shows the wallpaper exactly where it is; a band round the rim
  bends and magnifies it inward like the edge of a thick convex lens, gently where the band starts
  and hardest at the outline, where it turns over, with a faint colour split on clear glass. A
  bigger pane is thicker glass, with a wider band, and bends more.
- **It softens it**, with a true blur. `GlassLevels` keeps the wallpaper four times over — as it
  is, misted, frosted, and with little left but its colours, each made from the one before at half
  its size when the wallpaper changes — and a pane shows a blend of the two either side of how soft
  it is (`GlassLook.frost`), so any softness costs two lookups and nothing is blurred while
  drawing. What is dark behind the glass comes through a little brighter, in its own colour.
- **It catches the light on its edge, and nowhere else.** A hairline of light hugs the outline,
  brightest on the corner facing the light (up and to the left, as on iOS) and again on the corner
  opposite, faint between, with a little sheen just inside it; the bent band is a touch brighter
  than the face, as glass seen edge-on is. The face itself is flat: no shading, no glints, no glow
  round the edge. The light is a little of the wallpaper's colour round the pane
  (`GlassLook.glow`), and it moves as the phone tilts.
- **It makes colour richer, not greyer** (`GlassLook.vibrance`): what is seen through the glass is a
  little more saturated, as Apple's vibrancy makes it.
- **It takes the wallpaper's colour.** On the home screen each pane's veil is tinted with the
  wallpaper's own colour where the pane sits, read from the copy the lens already samples, with a
  soft glow of that colour just outside it (`GlassTint`, the second twist). The fallbacks take one
  colour from the pane's middle, or the wallpaper's saved colours.
- **A ring runs through it.** The ring of light from a tap on the wallpaper (`GlassRing`) is one
  state for the whole screen, where it started and when, read by every pane as it draws: a band of
  light in the lens, a brighter rim in the fallbacks.

A pane standing on the wallpaper — everything on the home page, the dock, the Search pill, the card
an app opens from — is the same clear glass in light and in dark, with white words and the soft
shadow the names have (`LocalOnWallpaper`). A pane on a page or a sheet keeps a body that follows
light and dark, as iOS's sheets do: the glass cannot show a page under itself, so its body stands in
for the page.

A readable veil thickens only over parts of the wallpaper that would fight the text. That is
**Glass panels: Clear** in Settings → Wallpaper & appearance, the glass with nothing added and what
everybody has unless they ask for more. **Frosted** is for a wallpaper as dark as the pane itself,
or in the light as pale, where clear glass is mostly its rim: a little frost lifts the pane a step
away, there and nowhere else (`glassLift`). **Solid** adds a fuller veil on
top of that (`GlassBody`). Every pane casts a soft shadow straight down, as Apple's glass does: a
wide faint one that says how far off the wallpaper it floats and a hairline one under its edge,
deeper for thicker glass (`shadowPaints`). Panes you press
(`interactive = true`: the dock, folders, widgets, search) light up from within, the glow starting
under your finger.

A material can only be judged by looking at it, so `GlassSheetTest` paints a home screen's worth of
panes over several wallpapers with the launcher's own drawing code and leaves the pictures in the
app's external cache (`glass-*.png`), for Clear, Frosted and Solid and for both fallbacks.

`Modifier.glassInk()` makes whatever a composable draws out of glass, for the "No glass" widgets:
its own outline becomes the rim, so the wallpaper bends at the edges of each numeral and icon,
which throw back what is beside them, and the highlight runs along them in the wallpaper's colour.
Each stroke is a flat bar of glass with a hairline of light on its edge, lit by the same light as
the panes.
What is drawn white is clear glass; a colour becomes tinted glass.
With Glass panels on Frosted or Solid the glass also gathers more light over a dark wallpaper, so
a glass clock stands out from it.

Android 13+ does not let a launcher read the wallpaper, so `GlassWallpaper` keeps its own copy of
any wallpaper Zenith sets (its own designs, or a photo picked under Wallpapers → Your photos) and
checks it is still current by the number Android gives every wallpaper (`WallpaperManager.getWallpaperId`,
noted when the copy is made; see `WallpaperTrust`). The wallpaper's colours are only the fallback:
Samsung phones never announce them to the app that set the wallpaper. Android 14+ shows a
still wallpaper zoomed by a tenth to leave room for parallax, and the shader matches that. With no
usable copy (a live wallpaper, or one set in another app) panes fall back to frosted glass with a
rim of the same kind, rather than showing a picture that is not really behind them. Android 12
uses the frosted copy, and "No glass" widgets draw their numbers and icons plain white below
Android 13. Nothing is captured or blurred per frame.

## Zenith Plus

Zenith works in full without it. Plus (`data/Plus.kt`, through Google Play Billing) adds hidden apps
behind a PIN, backup and restore, the island, free placement, the Glass studio, saving, switching,
scheduling and sharing Looks, seven of the wallpapers and backups to an account. It is free for
seven days, then one of three plans, the shortest first on its sheet:

| Plan | Price | In Play |
| --- | --- | --- |
| 1 month | $4.00 a month | the subscription `zenith_plus_monthly`'s monthly base plan (P1M), which carries the free trial |
| 3 months | $8.00 every 3 months | its base plan `three-months` (P3M) |
| Forever | $10.00 once, marked best value | the one-time product `zenith_plus` |

The two subscription plans are told apart by how often they bill, never by their place in Play's
list (`SubscriptionPlan`). The prices shown are Play's, in the phone's currency; the three above are
only stand-ins until Play answers, and a plan Play does not carry yet is not shown. The pass says
plainly how it compares, worked out from those prices: "Pay once: less than three months of the
monthly plan". What to set up in Play Console, and when, is in [RELEASING.md](RELEASING.md).

## Accounts and the server

Zenith works with no account, and then nothing leaves the phone. `server.properties` says where
Zenith's server is; while its two lines are empty the app has no server, shows nothing of accounts
and sends nothing. With an address:

- **Settings → Account** (`ui/settings/AccountPage.kt`): create an account or sign in with a
  username and a password, or Continue with Google (Credential Manager, `GoogleSignIn.kt`) when a
  client ID is set; then who you are, your code, how many friends used it, when the backup was
  saved, sign out, and delete — which asks twice and, for a password account, for the password.
- **Backup & reset** gains "Back up to your account" and "Restore from your account" for Zenith
  Plus. The copy sent leaves out the hidden-apps PIN and the purchase; restoring keeps the phone's.
- **Aura**: a code is checked by the server. One nobody has, or your own, is refused; a real one
  pays you 150 and its owner 150, for the owner's first ten friends. The owner's phone is paid the
  next time it asks how the account stands: on the Account or Backup page, and at most every ten
  minutes on the way back to the home screen.
- **Looks** (see [Looks](#looks)): a Look is shared from the account under a code, which anybody
  can open; an account keeps its twenty newest, and they go when it does.

`data/server/` is the app's side: `ZenithServer` (every route, over `HttpURLConnection` and
`org.json`; it answers yes, a no the app can act on, or "out of reach", and never throws),
`AccountStore` (the sign-in, in a preferences file that Android's backups leave out) and
`AccountRules` (what an account changes on the phone, as plain functions with tests). The view
model sees each request through even if the page that asked is closed, one at a time.

`server/` is the other side: `zenith_server.py`, one file of standard-library Python with SQLite,
and `PROMPT-FOR-SERVER-AI.md`, the message that has it installed. `ZenithServerTest` starts that
program and runs the app's own client against it, so the two cannot drift apart (it needs Python on
the PC, and is skipped without it). What the two say to each other, and what was decided, is in
[`specs/2026-10-04-zenith-server-design.md`](specs/2026-10-04-zenith-server-design.md); turning it
on is in [RELEASING.md](RELEASING.md).

## Building

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Compose BOM 2024.12.01, minSdk 29, application ID `com.gabriel.zenith_Launcher`, no third-party libraries:
only Google's own (Play Billing, and Credential Manager with `googleid` for Continue with Google).
Settings and layout are stored as JSON in `SharedPreferences` (`SettingsStore`).

For signed release builds and the Play Store checklist see [RELEASING.md](RELEASING.md); store
copy, graphics and the privacy policy live in [`play/`](play/).

## Working on another PC

Everything the app needs is in the repository, including where its server is (`server.properties`),
so a fresh copy builds the same app, talking to the same server:

```bash
git clone https://github.com/FireGB2012/ZenithUI.git
cd ZenithUI
./gradlew :app:assembleDebug
```

Opening the folder in Android Studio writes `local.properties` (where the Android SDK is); from a
bare terminal set `ANDROID_HOME` instead. The unit tests that start the server program need Python
and are skipped without it.

Three things are left out of the repository on purpose, and travel by hand when they are needed:

- **The debug key**, `.android/debug.keystore` in your user folder. Every PC makes its own, and
  Android will not install a build signed with one key over a build signed with another. To install
  from a second PC onto a phone that already has Zenith from the first, copy that one file across to
  the same place. Do not uninstall instead: that clears the home screen. Continue with Google is
  tied to the same key.
- **The upload key**, `zenith-upload.jks` with `keystore.properties`, for builds that go to Play:
  see RELEASING.md.
- **`.superpowers/`**: working notes, and the copies of the phone's settings taken before each
  install.

Finish a piece of work by committing and pushing it. Work left on one PC has been lost to the other
before.

## Known limits

- Opening and closing apps into their icons has not been tried on a phone yet. The gesture
  handshake only happens with gesture navigation on phones whose recents are AOSP's (not One UI,
  not with buttons); App pictures needs Android 11 and a Zenith from Play or put on with a
  computer. Without the handshake Android's own close still plays before Zenith's, with the home
  screen empty until it is over: how long is Android's word from Android 14 on, and a guess for
  the make of phone before that. An app that had to start again shows its splash screen after the
  picture for a moment. Without App pictures
  the card is the app's colour and logo, not the app, and Zenith only knows which app is closing
  from having opened it.

- Reading the wallpaper for the glass backdrop requires Zenith to be the default home app. Entering
  the launcher raises the system "Set Zenith UI as your default home app?" dialog (`RoleManager`,
  `ROLE_HOME`, falling back to the home settings screen); it is asked at most three times, and a
  one-tap hint stays above the dock until it is granted. App shortcuts need the same: Android only
  hands them to the home app.
