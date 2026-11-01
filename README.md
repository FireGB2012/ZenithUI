# Zenith UI

A launcher for Android that puts iOS-style liquid glass on top of Android's customisability —
built entirely in Jetpack Compose.

## First run

1. **Welcome** — animated glass mark, the four pillars, and a name field. What you type here is
   used across the launcher ("Good afternoon, Gabriel").
2. **Palette** — six palettes (Midnight, Aurora, Sunset, Frost, Sakura, Graphite), light/dark/auto,
   and a Glass slider. A live phone mock updates as you choose.
3. **Widgets & grid** — pick the widgets for page one, the grid density (Compact 5×6, Balanced 4×5,
   Roomy 4×4) and the icon shape.
4. **Setup** — a 20-second progress ring. It is not a fake wait: while it fills, the launcher reads
   every launchable activity, picks a dock from your dialer/messenger/browser/camera, groups apps
   into folders by their declared category, places the widgets you chose and reads the wallpaper.
   The ring lands on 100%, greets you by name and drops you on the finished home screen.

## Home screen

- **Free placement** — every page is a `columns × rows` grid; long-press any icon, folder or widget
  and drag it to any free cell. A drop hint shows where it will land.
- **Everything lines up** — icons and widgets keep to whole rows. A widget runs from the left edge
  of the icon in its first column to the right edge of the icon in its last, and from the top of
  its first row's icons to the bottom of its last row's labels, so the space under a widget is the
  space under any icon. A full dock stands on the same columns as the page, and the name at the top
  starts on the same line as the first column. One class, `HomeGeometry`, works out every position
  in whole pixels. (Zenith Plus free placement still puts things anywhere.)
- **Folders** — drop one app onto another to create a folder; drop onto a folder to add to it.
  Tap a folder for a bottom sheet with an editable name; long-press an app inside to take it out.
- **Remove** — drag to the "Remove" target at the top.
- **Resize and move** — long-press any app or widget → **Resize and move**, then pull the pointer
  on its bottom-right corner. The outline follows your finger and the item snaps to whole cells and whole rows as you
  pass them, with a size badge; it can be any size up to the whole page (a widget stops at the
  smallest its design allows). An app bigger than 1×1 becomes a super icon, its icon stretched to fill the space. A
  size with no room turns the outline red. Tap anywhere to finish.
  While it is up, a bar at the foot of the screen does the same in taps (`ui/home/ResizeBar.kt`):
  a widget's named sizes — Small, Medium, Wide, Tall, Large — one tap each, and four arrows that
  move the item a cell at a time. A named size is taken where the widget stands, or at the closest
  opening on its page when there is no room there; an arrow never pushes anything aside, and a tap
  with no room shakes the bar instead. The rules are `Reshape.resized` and `Reshape.nudged`.
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
  apps too.
- **Every menu is a bottom sheet** — item menus, Customise, wallpapers. Nothing needs a second
  hand. Settings and the widget gallery are pages of their own, the full height of the screen, on
  one shell (`ui/common/FullPage.kt`): a bar with a way back and a way out, pulled down to close.
- **Dock** — a bar as wide as the page however many apps it holds, as on an iPhone. It takes one
  app for each column of the page, and never fewer than four, and they share the bar out equally:
  a full dock has each app under the icons above it, and a smaller one spreads across the bar
  (`HomeGeometry.dockIconLeft`). Pin from any app's long-press menu.
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
- **Notification dots** — a bead of glass in the accent at an icon's top corner while its app has
  something to read, on the page, in the dock and in the drawer; a folder's is the sum of its apps'.
  Off, Dot or Count, under Settings → Home screen, which also asks Android for notification access
  the first time. `NotificationDots` only counts: nothing ongoing (music, a download), nothing an
  app asked not to badge, and nothing of what they say is kept or sent. A dot springs in with the
  first notification and shrinks away with the last.
- **Icon style** — **Original**, **Tinted** (each app's one-colour symbol in the accent on a tile
  of the theme's tint, as Android 13 does) or **Glass** (the symbol on a tile of clear glass, the
  way iOS draws clear icons). The symbol is the adaptive icon's monochrome layer where the app has
  one, its foreground where that is a logo on its own, and otherwise what stands out of the icon
  from its own tile (`IconLoader.loadGlyph`). An icon you chose yourself stays as you chose it.
- **Shapes and names** — eight icon shapes (Squircle, Circle, Rounded, Square, Pebble, Teardrop,
  Hexagon, Clover), one path each in `IconShapes` for the pictures and the tiles alike; names in
  three sizes inside the same strip, so nothing on the page moves, and an optional shadow under
  them for a busy wallpaper.

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
lights up, with a tick, once it will; a double tap sends a ring out from under the finger. The
home button counts as the gesture only when Zenith was already in front with nothing open on it,
as Android's own launcher tells the two apart; on the way back from an app it folds everything
back to page one, as before.

Locking needs the **lock helper** (`LockService`), an accessibility service that does nothing else:
it asks for no events and cannot see the screen. A sheet says so before Android's page opens. The
shade and quick settings are pulled down the way Android's own launchers do it
(`SystemPanels`), with the helper as the way round a phone that refuses. The torch is one `Torch`
for the gesture and the Quick Toggles widget alike, which now knows when the torch was turned on
anywhere else.

## Built-in widgets

| Widget | Size | What it does |
| --- | --- | --- |
| Zenith Clock | 2×2 | Small tile: time, short date and a greeting that uses your name |
| Power | 2×2 | Battery ring from `ACTION_BATTERY_CHANGED`, with charge state |
| Calendar | 2×2 | Real month grid, today highlighted, days left in the month |
| Quick Note | 2×2 | Scratchpad persisted with your settings |
| Quick Toggles | 2×2 | Small tile, 2×2 buttons: working torch (Camera2) plus Wi-Fi / Bluetooth / DND |
| Now playing | 4×2 | The album's picture, the song, play / pause / skip and how far in, from Android's media sessions (`NowPlaying`, through the dots' notification access); the picture crosses over as the song changes |
| World clock | 2×2 | Up to four cities, chosen from every time zone, each with its time, a sun or a moon and how far it is from yours ("+9 h, tomorrow"), clock faces in the bigger sizes |
| Countdown | 2×2 | The days to a date you name, as rolling digits in a ring that fills as the day comes; "today" on the day, and the days since after |
| Photo frame | 2×2 | Up to twelve of your photos, picked with Android's own picker and kept small in Zenith's files; a different one each time you come home, crossing over, and the next at a tap |
| To-do | 2×2 | A short list: what is left first, a tick that fills and draws itself, the ticked ones sliding below the rest, a line to add one |
| Aura | 2×2 | Your streak in rolling digits under a flame, the week behind it as dots that fill one after another, your aura, and in the large size how near the next theme is |
| Weather | 2×2 | Now, today's high and low, and the next five hours with their skies, for a place you choose (`WeatherRepository`, Open-Meteo) |

Each of them has a No glass version and a design for every shape of space, like the first five,
and says in the gallery what each size shows. What they need is set from their own menu: cities,
a date, photos, a place. **Weather is the one thing in Zenith that sends anything without an
account:** nothing until a place is searched for; then what was typed, to Open-Meteo's place
search, and from then on the chosen place's latitude and longitude, to its forecast, at most every
half hour while the home screen is shown. Nothing else goes with it, and the sheet says so before
anything is sent.

The **search bar** is not a widget: switch it on under Zenith settings → Home screen and it sits
above the dock, as wide as the icons above it. Tap to search apps, or hit the button for Google.

**The gallery** — long-press an empty spot → **Customise** → **Widgets**
(`ui/home/WidgetGallery.kt`). Every widget is drawn by the code that draws it on the home screen,
at the size it is there, on a window onto your wallpaper, so what you see is what lands on the
page. Tap one and it grows into a showcase: its sizes in a row, all to one scale — Small, Medium,
Wide, Tall and Large, each fitted to the widget and to your grid (`WidgetSizes`), and each a
different design — with a switch between Glass and No glass and one button that adds the size in
the middle. Under the row the sizes are named, each over the room it takes on the page: a little
grid as wide as your page, its cells filled in (`SizeShape`), so Wide is plainly the width of the
page and Tall a row more. A tap on a name brings that size to the middle, and under it a line says
what that size shows ("Hours over minutes, like a Lock Screen"). The lines come from
`WidgetLayouts`, which reads the same rules the widgets lay themselves out by (`WidgetForm`, and
`ToggleGrid` for the toggles), so they cannot say a size shows what it does not. The bar that
comes up for **Resize and move** shows the same little grid on each of its sizes.

The switch between Glass and No glass carries a symbol on each side, a pane and a pane struck
through, and what the side chosen means is said right under it.

**No glass** — the other side of that switch: the same widgets with no pane round
them, like the clock on an iPhone's Lock Screen: the big numbers, rings and icons are made of glass
themselves and sit straight on the wallpaper, with small glass icons added (a sun or moon on the
clock, a battery on Power, a calendar, a pencil). Their words are white with a soft shadow. They are
stored as `bare_<type>` (`bare_clock`, …) and otherwise behave exactly like the widget they are a
version of.

**Words that can be read.** A widget is read at a glance, over a wallpaper, so its second line (the
date, "On battery", "25 days left") is set stronger than the muted grey a page uses, nothing in a
widget is smaller than 11sp at its ordinary size, and the accent is taken part of the way towards
the pane's own ink when it is used for letters. The names under the Quick Toggles are the size of
the label under an app; one too long for its cell ("Do Not Disturb" in a two by two) goes onto a
second line, and every name takes the same number of lines so the buttons stay level
(`ToggleNames.fit`). Their symbols fill a little over half of their button, as Android's own quick
settings do. Everywhere else, second lines and small print (the subtitles in Settings, the words
under a choice) are 74% of the ink, which stands six to one or better against every
theme's backdrop, light and dark (`PaletteToneTest`).

## Other apps' widgets

The gallery's **From your apps** lists every app on the phone that has widgets, A to Z; an app
opens out to its widgets in a row, each its picture, its name and the cells it takes on this grid
(`HostedWidgetPicker`). A tap puts it on the first page with room, at the size it asks for —
Android 12's own cells where it gives them, otherwise its size in dp turned into cells and whole
rows (`HostedSizes`). Android asks once whether Zenith may show widgets, and a widget with a set-up
screen opens it; backing out of either puts nothing on the page. An app's own "add this widget"
comes up as a card of glass over that app, the same one shortcuts use.

On the page it is the app's own view in Zenith's frame (`HostedWidgetCell`, through one
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
now — Wallpaper & theme, Home screen, Motion & feel, Gestures, Apps, Zenith Plus, Looks, Aura,
Backup & reset, About & help — and an eleventh, Account, in a build that has a server. They stand in three blocks, each
headed with what it holds (Look and feel; Plus and Aura, or Plus, Aura and account; Backup and
help) and set further from
the next than its rows are from each other. A group pushes in from the side; back, the arrow or a swipe from the left edge pops it.
Search finds any option by its name or by other words for it ("dark", "vibrate", "battery"), opens
its page and pulses the row. The Theme and Home pages start with a small live copy of your own home
screen, drawn by the same code as the real one, so a change is seen as it is made.

**Aura** is one of those pages (`ui/settings/AuraPage.kt`): the balance, counted up inside a ring
drawn round to how far it goes towards the next theme; the streak as a ladder of seven days, each
with what it pays; every theme on a card in its own colours, with a bar towards its price and,
when it is out of reach, how many more days it will take; your code to copy or share, and the box
for somebody else's. `Aura.ladder`, `Aura.daysToAfford` and `AuraThemes.next` are plain functions
with tests.

Every option is listed once in `SettingsCatalog`, which is what the search reads. Any Nova- or
ADW-compatible icon pack installed on the device is detected and its `appfilter.xml` is parsed.
The grid is Compact, Balanced or Roomy — a cell size, from which the columns and rows are worked out
for the screen — or **Custom**: columns and rows of your own, three to six across and three to
eight down on a phone and more on a bigger screen (`GridDensity.resolve`). Changing the grid
re-flows the existing layout instead of dropping items, and **Tidy up** puts anything out of line
back in its place.

Every choice says what it does: the four tiles of Customise each carry a line, and under each
control in Settings a line of small print describes the option that is selected. The Wallpaper
tile shows the wallpaper you have, and says which of Zenith's designs it is; the rows under the
tiles sit in a group of their own, a line between each.

## Wallpapers

Zenith paints its own wallpapers on the phone (`WallpaperArt`, `WallDesigns`): thirty-three designs
on four shelves — Soft, Lines, Shapes and Scenes — seven of them for Zenith Plus. Every design is
drawn from a theme's five colours and has a light picture and a dark one, each made so labels and
the status bar stay readable over it. Tap one and both pictures are shown side by side, each with
your own home screen on it, drawn by the code that draws the real one in the ink it takes in that
light. Its glass is frosted there, as over any wallpaper Zenith cannot see: the real glass will
bend the new wallpaper, which is not behind it yet.

A design, once set, **follows the theme**. Change the palette, or go from light to dark (by hand
or with the phone, on Auto), and `WallpaperDirector` paints the same design again in the new
colours, fades it in over the old wallpaper, moves the glass onto it half way, and then hands it to
Android — which cannot fade between wallpapers itself. Only where a run of changes stops is
painted. A design set in another theme's colours without switching Zenith to that theme keeps those
colours and still changes between light and dark. The switch is under Wallpapers and under
Settings → Look.

Zenith only ever repaints a wallpaper it set and that is still on screen. A photo, or anything
chosen in another app, is never replaced, and choosing one ends the following.

## Looks

A **Look** is the whole home screen saved under a name (`data/Looks.kt`, Settings → Looks): the
palette and light or dark, the glass, the wallpaper design, the icon shape, pack and style, names
and badges, the grid, the dock, the layout, the motion and the gestures. Nothing of yours goes in
one: not your name, your note or lists, the countdown, your hidden apps, Aura or Plus. A Look is
kept as the very JSON the settings and layout are saved as, so one made today still puts on when
settings have grown; one from a newer Zenith puts on all this one knows. Putting one on cannot
change anything a Look does not keep (`Looks.KEYS`), so a Look written by hand cannot give anybody
Plus or Aura.

Tap a Look and Settings goes, the first page comes round, and the home screen flies in wearing it,
the way it arrives after an unlock; its wallpaper design is painted again in its colours. A Look
row shows its colours as three dots and which one is on now; its menu puts it on, saves the home
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

## Fonts, palettes and the Glass studio

**Font** (Wallpaper & theme): System, Inter, Condensed, Serif, Mono, Typewriter, Casual, Hand or
Small caps, for everything; **Numbers** sets the clocks, the battery, the countdown and the weather
in the same font, in Thin, or in any other (`ui/theme/Fonts.kt`). Inter and JetBrains Mono are
carried in the app, unchanged, under the SIL Open Font Licence (About & help → Fonts shows it in
full); the rest are the phone's own fonts asked for by name, and a phone without one shows its
usual font. Every style Zenith sets text in takes the font — the theme's, and the ones built by
hand for names, fields and widgets — so nothing is left in a font that was not chosen.

**Wallpaper** is a palette like the others, taken from the wallpaper (`WallpaperPalette`): its
liveliest colour that is more than a speck, for the glow; a second of another hue, for gradients;
its main colour, kept deep or pale, behind the glass; and light or dark from the picture's own
lightness. It is read from Zenith's copy of the wallpaper, or from Android's reading of it where
Zenith has no copy, and changes when the wallpaper does — but never from one of Zenith's own
designs, which are painted in the palette and would only chase their own tail.

**By time of day** (Plus) gives dawn, day, dusk and night each a palette (`DayParts`): as a part
begins its palette fades in; one chosen by hand mid-part stays until the next.

**Glass studio** (Plus, `GlassTuning`): bend, frost, rim light, rainbow edge, how much of the theme
is in the veil, and where the light comes from — Zenith's own, up and to the left, or anywhere on a
dial — and whether it swings as the phone tips. Every change shows at once on the sheet's three
panes, on the sheet and on the home screen behind it; Reset puts Zenith's glass back. The defaults
change nothing (`GlassTuningTest`), and without Plus the glass is Zenith's own again, the tuning
kept.

**Each Zenith widget** can have a colour of its own (its menu → Colour): twelve, or the theme's.
The widget eases into it and back out to the theme's; with none it changes with the theme to the
frame.

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
- **Closing an app.** Android does not let a launcher that is not built into the phone move another
  app's window, so Zenith draws the half it may: the home screen comes forward again on a spring
  that carries it a little too far, the app's icon lands in its place with a bounce, and the icons
  round it are knocked back and swing home. A card that shrinks back into the icon can be switched
  on as well.
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
- **Pages.** Four ways to turn one — Depth, Slide, Cube, Stack — and dots whose pill stretches
  towards the next as the page turns. A page added grows its dot out of the end of the row, and
  one forgotten draws it back in.
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
  itself in, toggles that change shape.
- **The drawer and sheets.** The drawer is put together just after the home screen arrives, out of
  sight, so the first swipe up is as smooth as the tenth. Rows rise in each time it opens, its
  title and count turn over as you search, and a wallpaper's preview grows out of the thumbnail
  that was tapped. A sheet comes up from the edge on a spring and follows a finger: down as far as
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
  finger that falls only on what was pressed. All of it gives way to plain fades with movement
  reduced.
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

- **Dock**: a glass bar, **Floating** (a pill that hugs its icons and grows or shrinks round them),
  or icons only.
- **Which page you are on**: dots, a line with a bar gliding along it, the page's number rolling
  over, or nothing.
- **Top of the page**: your name, a greeting, a large **Clock** in the numbers' font, the **Date**,
  or **At a glance** — the date and the weather where the Weather widget is set — or nothing.
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
sheets, which frosts it so text on top stays readable. On Android 13+ an AGSL shader
(`LiquidGlassShader`) draws each pane, and does three things to the wallpaper round it:

- **It bends it.** The flat middle shows the wallpaper exactly where it is; the rounded rim bends
  and magnifies it inward like the edge of a thick lens, with a faint colour split on clear glass.
  A bigger pane is a thicker slab and bends more.
- **It softens it**, with a true blur. `GlassLevels` keeps the wallpaper four times over — as it
  is, misted, frosted, and with little left but its colours, each made from the one before at half
  its size when the wallpaper changes — and a pane shows a blend of the two either side of how soft
  it is (`GlassLook.frost`), so any softness costs two lookups and nothing is blurred while
  drawing. What is dark behind the glass comes through a little brighter, in its own colour.
- **It reflects it.** The steep outer part of the rim is a mirror as much as a window and holds the
  wallpaper just outside the pane, turned over at the edge (`GlassLook.mirror`). The light the
  glass catches is the colour of the wallpaper round it (`GlassLook.glow`): a crisp line all the
  way round the rim, brightest where it faces the light and bright again opposite, with a soft glow
  just inside it. Light, bend and reflection all move as the phone tilts.
- **It is a thing, not a picture of one** (`GlassLook.relief`). A pane is a slab with a rounded
  shoulder and a polished edge, and the same light shows its shape: the shoulder turned to the lamp
  is lit and the one turned from it is in the slab's own shade, light falls away gently across the
  face, and where the polished edge mirrors the lamp there is a glint, with a fainter one opposite.
  A bigger pane is a thicker slab. Turning **Glass** down in Settings flattens it.

A readable veil thickens only over parts of the wallpaper that would fight the text. That is
**Glass panels: Clear** in Settings → Wallpaper & theme, the glass with nothing added and what
everybody has unless they ask for more. **Frosted** is for a wallpaper as dark as the pane itself,
or in the light as pale, where clear glass is mostly its rim: a little frost in the theme's colour
lifts the pane a step away, there and nowhere else (`glassLift`). **Solid** adds a fuller veil on
top of that (`GlassBody`). Every pane casts a shadow in two parts, away from the light: a wide
faint one that says how far off the wallpaper it floats and a close dark one under its edge that
says it is solid, both deeper for a thicker slab (`shadowPaints`). Panes you press
(`interactive = true`: the dock, folders, widgets, search) light up from within, the glow starting
under your finger.

A material can only be judged by looking at it, so `GlassSheetTest` paints a home screen's worth of
panes over several wallpapers with the launcher's own drawing code and leaves the pictures in the
app's external cache (`glass-*.png`), for Clear, Frosted and Solid and for both fallbacks.

`Modifier.glassInk()` makes whatever a composable draws out of glass, for the "No glass" widgets:
its own outline becomes the rim, so the wallpaper bends at the edges of each numeral and icon,
which throw back what is beside them, and the highlight runs along them in the wallpaper's colour.
Each stroke is a rounded bar of glass, shaded and glinting by the same light as the panes.
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
rim of the same kind and the slab's form drawn in plain shading, rather than showing a picture that
is not really behind them. Android 12
uses the frosted copy, and "No glass" widgets draw their numbers and icons plain white below
Android 13. Nothing is captured or blurred per frame.

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

- Reading the wallpaper for the glass backdrop requires Zenith to be the default home app. Entering
  the launcher raises the system "Set Zenith UI as your default home app?" dialog (`RoleManager`,
  `ROLE_HOME`, falling back to the home settings screen); it is asked at most three times, and a
  one-tap hint stays above the dock until it is granted. App shortcuts need the same: Android only
  hands them to the home app.
