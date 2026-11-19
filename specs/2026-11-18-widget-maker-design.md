# Widget maker: your own widget, from scratch

- Date: 2026-11-18
- Branch: `claude/project-thread-pbl8g3`, off `claude/project-thread-yjufje` (PR #19, 108 for Play)
- Asked: "Now make some kind of way to make ur own widget from scratch"

Every widget Zenith has was designed by us. This is the way to make one yourself: choose a size,
choose what it stands on, put pieces on it (the time, the date, your own words, the battery, a
clock face, the weather, a symbol, a shape, a photo, a greeting, the next alarm), move them with a
finger, and put it on the home screen. There it is a widget like the others: it moves, resizes,
stacks and comes off the same way. It is kept under **Your widgets** in the gallery, to put on the
page again or to change; a change shows on every copy at once.

## Where it is

- **The gallery** (Customise, Widgets) starts with **Make your own**: a card that opens the maker on
  a new widget, then your widgets, each drawn live. A tap on one opens it in the maker.
- **A made widget's hold menu** has **Edit Widget**, which opens the maker on it. It has no Colour
  row: the colours are part of the design.
- While stacking, your widgets are offered too, and a tap stacks one.

## The maker

A page of its own over the home screen (`FullPage`, like Settings and the gallery), in four parts
from the top:

1. **The stage.** A window onto your wallpaper with the widget on it, drawn by the same code as on
   the home screen. It is drawn as large as the stage allows, up to twice its size, so a small
   widget's pieces can be reached (*my call*). Its foot holds Undo and Redo and says what size it
   is shown at, on glass, so nothing ever stands over the widget, however wide it is.
   - Tap a piece to pick it: a white line goes round it, over a soft dark edge so it shows on any
     card, with a round corner to pull.
   - Drag it to move it. Its edges and middle snap to the middle of the widget, to the margin
     round its edges (16 dp, the widgets' own inset) and to the edges and middles of the other
     pieces, within 6 dp. A yellow line shows what it has lined up with, and a tick says it has.
   - Pull the corner, or pinch, to make it bigger or smaller.
   - A tap on nothing puts the piece down.
2. **Undo, redo and the size.** Undo and Redo take back and bring back every change, a drag or a
   slider's whole pull being one (`MakerHistory`). The sizes are the gallery's own: Small, Medium,
   Wide, Tall and Large, each drawn as the room it takes on the page (`SizeShape`).
3. **The panel**, which scrolls.
   - With nothing picked: **Add** (a tile for each kind of piece), **Start from** (four starting
     points, only while the widget is empty), **Background** (Glass, Colour or No glass, and its
     colour), **Pieces** (one chip for each, to pick one that is hard to reach on the stage) and
     the **Name** written under it on the home screen.
   - With a piece picked: its name, and Duplicate, Forward, Back and Delete; then what it has:
     size, colour, and for words their weight, font and which way they line up; the time's
     version (12:45, with seconds, the hour alone, the minutes alone, with AM/PM); the date's
     (Wed 18 Nov, Wednesday, 18 November, Wednesday 18 November, 18/11/2026, the day's number, the
     month); the battery as a ring, a number or a bar; a clock face with marks, with a second
     hand, or with numbers; the weather as the temperature with the sky, the temperature, the sky,
     in words, or today's high and low, and the place it is for; the symbols to choose from; a
     shape's form (a rectangle, a circle or a ring), width, height and corners; a photo, its width,
     height and corners; and for every piece how see-through it is and whether it is made of glass.
4. **Add to home screen**, or **Done** when the maker was opened from the widget on the page.

Closing the maker in any way (the cross, back, pulling it away) keeps what was made under Your
widgets (*my call*: nothing made is ever lost to a slip; a new widget with nothing on it is not
kept). **Delete widget**, at the foot of the panel, asks first, right there in the panel (an alert
would open under the page), and takes every copy of the widget off the home screen and out of
stacks as well. Undo on the home screen cannot bring those back, as there is nothing left to draw
them from, so its history is let go of when it held one.

Opened from the widget on the page, a new size chosen in the maker is given to that widget too when
the maker closes, where it stands or as near as its page has room (`resizeWidgetTo`).

## How a made widget is drawn

`MadeWidgetView` in `ui/home/widgets/MadeWidget.kt`.

- **What it stands on.** Glass is the pane Zenith's widgets have, tinted with its colour when it
  has one. Colour is a card in its colour, a touch lighter at the top and deeper at the foot
  (`WidgetColours`' own-colour card), the same in Light and in Dark. Its words are white wherever
  white stands at least 3 to 1, what large words need, as on an iPhone's blue, red, purple, pink
  and indigo, and near-black only where white would not read: yellow, mint, cyan, orange, green
  (`WidgetColours.made`; *my call*, since the plain rule of whichever ink reads better put black
  words on iOS blue). No glass is nothing
  but the wallpaper, its words with the soft shadow the names under the icons have.
- **Where the pieces are.** Each piece has a point, as a share of the widget's width and height,
  and lines up on it by its start, middle or end across and by its middle down. Its size is a share
  of the widget's shorter side (shapes and photos: of its width and its height). So a made widget
  reads the same on any phone, and pulled to another size on the page its pieces keep their places
  in proportion (*my call*: the same thing a design in percentages does).
- **Words never spill.** A line too wide for the widget is drawn smaller until it fits.
- **Made of glass.** Words, numbers, symbols and rings can be glass themselves (`glassInk`), as the
  No glass widgets' numbers are; a shape can be a pane of glass.
- **Nothing moves by itself** but the clock (a tick a minute, a second only for a time with
  seconds or a face with a second hand), so Lite has nothing to stop. The pane is the widgets' own,
  which Lite already draws for the A55.

## Kept

- In the settings as `madeWidgets` (`data/MadeWidgets.kt`), so a backup carries them. A Look does
  not keep them, as it keeps no notes or lists; a shared Look leaves made widgets out, as it leaves
  out other apps' widgets, since the other phone does not have them.
- On the page, a `WidgetCell` of type `made:<id>`; in a stack, a member of that type.
- Photos in Zenith's files (`made/`), each no bigger than a screen, as the Photo frame keeps its
  own, and not in a backup; one that can be seen through is kept as a PNG, so it stays so. A photo
  no widget uses any more is deleted when the maker closes or a widget is deleted.
- At most 24 widgets, 20 pieces on each, 60 letters of words in a piece, and 24 in a name.

## Rules, with tests

`MadeWidgetsTest`: reading and writing, and reading a damaged one; the type and its id; names;
taking a design off the page and out of stacks; photos in use. `MakerSnapTest`: what lines up
with what. `PiecePlaceTest`: where a piece is drawn from its point, and staying put when it lines
up the other way. `MakerHistoryTest`: undo and redo, one step per drag. `MadeWordsTest`: what the
time, date, battery and weather pieces say. `HoldMenuTest`: Edit Widget and no Colour row.
`LooksTest`: a shared Look leaves made widgets out. `MadeCardInkTest`: white words on a colour
card wherever they read.
