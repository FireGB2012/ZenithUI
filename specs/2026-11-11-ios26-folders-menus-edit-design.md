# Folders, hold menus and edit mode: design

Step 2 of rebuilding Zenith's screens in the iOS 26 style with twists of its own. Step 1 was the home
screen (`2026-11-09-ios26-home-screen-design.md`). This step is everything that happens when you
hold something on it: the menu that comes up, arranging the screen, and folders.

Gabriel tapped through a clickable mockup of it (`mockups/2026-11-11-folders-menus-edit.html`), with
eight twists he could switch on and off, and approved it as shown with all eight on: "Yep now make it
inside the real app". The twists are simply how Zenith works; there are no switches for them.

It is built in parts, each committed and pushed on its own. This document is kept up to date as the
parts land; "Built" under a heading says what is in the app now.

## The look

Everything here stands over the home screen, so it is Zenith's clear glass, the same in Light and in
Dark (Gabriel: "the liquid glass should just be liquid glass, not more white or dark"): menus, the
alerts, an open folder, the edit bar, the Customise card, the pages. White words; red (#FF453A) for
a row that takes something away; blue only for a main action. Words are plain British English, and
menu rows are in Title Case ("Edit Home Screen"). Nothing a user reads says "iPhone" or "iOS".

All of it is drawn inside the home screen's own layout, never in a window of its own (no Dialog, no
Popup): the finger that made a hold is still down and must be able to slide onto a row, and the glass
must be Zenith's own, made from the wallpaper. Glass shows a softened copy of the wallpaper, so a
pane over icons hides them. Where Zenith has no copy of the wallpaper (a live wallpaper, one it may
not read) a pane is only a thin veil, and these panes put a dark grey body under it
(`bodyWithoutWallpaper`) so the icons behind do not read through the words.

## Part 1: the menu beside what you hold (built)

Before: holding lifted a grey copy and brought the bin down; letting go without moving opened a
bottom sheet. Now, as on a phone running iOS 26:

- Rest a finger on an icon, a folder, a widget or a dock icon for 450 ms. It grows a little (an icon
  1.15 times, anything bigger 1.04 times), everything else is washed very slightly paler, and its
  menu comes up beside it while the finger is still down.
- The menu is a pane of glass 250 dp wide with 28 dp corners and 44 dp rows (taller with large
  text), each row's symbol on the left. It stands 10 dp under the thing and its name when there is
  room, otherwise above it, and then its rows are read from the bottom up, so the nearest to the
  finger stay nearest and the red row ends up at the top. A thing in the left half of the screen
  lines the menu's left edge up with its own; one in the right half, the right edges.
- Lift the finger and the menu stays: tap a row, or anywhere else to put it away. Or keep the finger
  down, slide onto a row (it lights up, with a tick) and let go on it.
- Move the finger more than 10 dp, other than towards the menu, and the thing is picked up instead:
  the menu is drawn back in and the carry goes on exactly as before. Never with the layout locked.
- Twist 2, "menus poured from the icon": the menu pours out of the thing as a drop of glass on a
  thin neck, the neck thins and snaps at 150 ms, the thing gives once like a tap letting go of a
  drop, and the menu settles a touch wide, then a touch tall, by 300 ms. Its rows can be tapped from
  the first frame. Put away, it is drawn back along the neck. With movement reduced it fades. The
  light that gathers under the finger while it rests is step 1's jelly press.

The rows, as they read under the thing (`HoldMenuRules`, pinned by `HoldMenuTest`):

| Held | Rows |
| --- | --- |
| An app on a page | its shortcuts, each with a "+" that puts it on the home screen · Edit Home Screen · Edit Name and Icon · Resize · App Info · Remove App |
| An app in the dock | its shortcuts · Edit Home Screen · Edit Name and Icon · Add to Home Screen · App Info · Remove App |
| A pinned shortcut | Edit Home Screen · Remove Shortcut |
| A folder | Rename · Colour · Edit Home Screen · Remove Folder |
| One of Zenith's widgets | its sizes, side by side · its set-up ("Edit Widget", or what it does: "Choose Photos", "Clear What Is Done", "Let Zenith See What Is Playing") · Colour · Stack Another Widget · Edit Home Screen · Remove Widget |
| Another app's widget | Put Glass Behind It, or Take the Glass Away · Stack Another Widget · Edit Home Screen · Remove Widget |
| A stack | Stack Another Widget · Take {name} Out, for each widget in it · Edit Home Screen · Remove Stack |

With the layout locked every menu leads with "Unlock Layout" and offers nothing that moves, resizes
or removes. The red row is always the last and there is never more than one.

- A widget's sizes are the named sizes it already had (Small, Medium, Wide, Tall, Large), drawn as
  the cells they cover; the one it is now is lit, one with no room on the page is greyed.
- "Colour" turns the menu into the twelve colours and "None"; each tap takes at once.
- Every remove row asks first, in a card of glass in the middle of the screen (`GlassAlert`):
  "Remove “Maps”?", "Removing from Home Screen will keep the app in your App Library." (or "app
  drawer"), with "Remove from Home Screen", "Uninstall App" in red, and "Cancel". A widget and a
  shortcut get "Remove" and "Cancel"; a folder "Remove from Home Screen" and "Cancel"; a dock app
  "Remove from Dock", "Uninstall App" and "Cancel". Undo still follows a removal, as before.

Where today's rows went: "Open" is gone (tap the icon). "Edit" is "Edit Name and Icon". "Resize and
move" is "Resize" for an app, the size row for one of Zenith's widgets, and edit mode's corner for
the rest. "Uninstall" is in the Remove App alert. "Rename folder", "Colour", the set-up rows, the
glass row, the stack rows and the shortcuts keep their place. "Customise home screen" gives way to
"Edit Home Screen". The dock no longer opens the drawer's sheet; the drawer keeps it until step 3.

Until part 2 is built, "Edit Home Screen" opens what stands nearest to it: today's resize and move
mode for a widget, and Customise for anything else. (Part 2 has since replaced that: see below.)

The lead's calls in part 1: the hold is 450 ms, the mockup's time (it was 320 ms); an app's
"Uninstall" cannot be reached with the layout locked (unlock first); a finger that never trembles
ends its hold "cancelled" in Compose, and that counts as a plain lift, so the menu stays.

## Part 2: edit mode

Built so far: holding an empty spot, "Edit Home Screen" in a menu, or picking something up starts edit
mode. Everything wiggles (2 degrees for an icon, 0.7 for anything bigger, about four swings a second
with a hair of a bob) and wears a minus on its corner, the dock's apps too and a folder not; the
minus asks before it removes. The Search pill stretches into the bar ("+" opens the widget picker,
the brush Customise, the dots the pages, Done ends it) and draws back into the page dots while
something is carried. Edited, a thing is picked up the moment the finger moves on it, with no hold,
on the page and in the dock; held still, its menu still comes. A tap opens nothing but a folder. Done,
a tap on the wallpaper, Back and the home button end it. With the layout locked, a hold on the
wallpaper says "The layout is locked" in a capsule of glass with "Unlock". The bin is gone. Undo
stands just above the bar. Still to come in this part: the Customise card, the corner to pull, the
wave, the glass beads and their pop, and Undo for every change.

The plan for the whole part:


Hold an empty spot, or choose "Edit Home Screen", or pick something up: every icon and widget
wiggles and wears a bead of glass with a minus on its corner (twist 6: the beads well up, magnify
the corner under them, and a removed app pops like a bubble). The controls are one bar of glass
stretched out of the Search pill, at your thumb (twist 8): "+" for Add Widget, a brush for Customise,
the page dots, and a blue "Done". A wave of light from the finger wakes the icons as it reaches them
(twist 1). Widgets show a corner to pull. Undo, while editing, takes back each change in turn. The
bin and the resize bar go. Customise becomes a card at the foot of the screen: icon style, widget
look, larger icons, dim the wallpaper, and the ways on to Wallpaper, the bedside clock, How It Works
and All Settings.

## Part 3: folders

A folder's tile shows who is waiting (twist 5). Open, it is a clear pane in the middle of a blurred,
undimmed screen with its name above it, three by three with pages; it spreads from where it was
tapped (twist 4). Hold an app inside for its menu; drag it past the edge to take it out. Resting one
app on another grows a bridge of glass between them (twist 3), and the new folder is named after
what its apps are.

## Part 4: pages

The pages view as cards of glass, and a fan of page cards over the dots while something is carried
(twist 7).

## Part 5: the words

How It Works, setup's tips, the notes in Settings, the README and the Play listing say what is true
again, and what the new things replaced is deleted.

## Not in this step

The App Library drawer, search, the widget picker, Settings, setup, Plus, Aura and the island keep
the look they have until their own steps. A sheet opened from a new menu (an app's name and icon, a
widget's set-up, the wallpapers) is still the old sheet.

## What would be a failure even if it works

Glass that differs between Light and Dark. A menu in a window of its own. Something a user could do
before that they can no longer do, and that is not named above as gone. A home screen that keeps
redrawing while nothing moves.
