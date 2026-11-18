# Closing an app off to where it lives

- Date: 2026-11-18
- Branch: `claude/project-thread-ch6u5s`, off `claude/project-thread-o3yi5o` (PR #16, folders)
- Follows: `2026-11-17-closing-like-void-design.md`
- Status: built. Gabriel: "Do so the the app close anim if the app isn't on the home screen it like
  has a smothh anil dow to the drake or to the side to the library": an app whose icon is not on
  the home screen should go smoothly down to the drawer, or to the side to the library, as it closes.

"Drake" is read as the drawer: Zenith's apps are either an app drawer, which comes up from below,
or an App Library, which comes in from the side past the last page (Settings → Apps → Swipe up
for), and those are the two places he names. An app in the dock already goes into its icon there.

## What it did

With no icon of the app on show (an app opened from the drawer, the library or Search, that is on
no page, or is on another page than the one on show), the card filled the screen, shrank into a
60 dp square in the middle of it and faded over its last 30%, as iOS's does. On a Galaxy that is
every app opened from the drawer: it looked like it went nowhere.

## What changes

1. **Off the screen, towards where it lives** (`CloseAway.way`):
   - On another page: towards that page, off the left for a page before the one on show and off
     the right for one after. Its own icon first, the nearest of them (the earlier of two as near),
     then a folder holding it, the way a card picks the icon it goes into (`IconPlaces.choose`):
     another of the app's screens counts, a shortcut only by its own key, and a work app is not the
     personal one.
   - On no page: down into the drawer, or off the right into the App Library when the apps are a
     library.
   - In a dock that is put away (Settings → Home → Dock off): down, where the dock would be.
   - On the page on show, with no place known for its icon (it has not been at rest yet): into the
     middle, as before.
2. **How it goes** (`CloseAway.target`): the card goes the way it goes into an icon, as Void's
   does (`CloseRules.intoIcon` over *Shrinking time*: a beat with the app back on the screen, then
   faster and faster), towards a square as big as the icon it opened from, just past the edge: in
   the middle of the foot going down, half way down going to the side. So it keeps to the edge it
   goes through all the way: going down its top comes down the screen as it narrows about the
   middle and its foot never comes up off the bottom, and going to the side its far edge never
   comes in. It is gone as it gets there: no fade, no squash, no hand-over.
   - It goes straight: no arc (*Arc of the path*), which would lift its foot off the edge half way.
   - It does not move with the home screen as that comes forward about its foot
     (`CloseRules.fromFoot`): the edge of the screen does not move.
3. **The home screen** comes back with it as it does with a card going into an icon
   (`CloseRules.bringsHome`): whole behind it from its first frame, sunk back, and forward about
   the middle of its foot once the card is down to 70%. On One UI with gestures the card still goes
   up at once and waits there for One UI's close (`AndroidClose.Plan.cardFirst`).
4. **Which page.** Coming home with the home button from an app going off towards its page stays
   on the page on show, as it does for an app going into an icon on it, so the way it goes is the
   way to its page (`CloseAway.staysOnPage`). Into the drawer, the library or the middle it goes
   back to the first page, as before.

## Not changed

- An app with an icon on show, the dock's included, goes into it as before.
- Closing *Plain*, and with movement reduced: no card with no icon to go into.
- The Pixel handshake: Android is told there is no icon, and does its own close, into the foot of
  the screen. Nothing of Zenith's plays after it.
- Opening an app.

## Limits

- Not seen on a phone yet, only in the JVM rig modelled on Gabriel's A55.
- An app on the first page opened from another page goes off towards the first page and the home
  screen stays where it is. Going to the first page and into its icon there would need its place
  on a page that is not on show, which nothing measures yet.
- The App Library is past the last page, so an app only in the library goes off the right from any
  page, the same way as one on a later page.
- With *Closing card* set below the whole screen the card starts part way off the edge it goes to,
  fading in as it does.

## Checked

- `CloseAwayTest`: which way for the drawer, the library, a page before or after (the nearer of
  two, an icon before a folder), the same app's other screens, shortcuts, work apps, a dock put
  away and the page on show; where it goes; that the card keeps to its edge all the way and is
  gone as it gets there (`LaunchMotion.frame`, every 1%); and which page coming home stays on.
- Frames from the JVM rig, as Gabriel's A55 with gestures (One UI's question with the home intent,
  its still picture, Zenith's own frames from 560 ms), closing Photos opened from the drawer, from
  the App Library, and from the drawer with Photos on the second page: before (into the middle) and
  after.
