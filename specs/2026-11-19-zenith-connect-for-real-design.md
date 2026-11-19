# Zenith Connect, for real

- Date: 2026-11-19
- Branch: `claude/project-thread-23neth`, off `claude/project-thread-7dwv3z` (110 / 3.9.1)
- Version: 111 / 3.9.2
- Status: built, unit-tested and shot on the JVM rig against a local copy of Zenith's server. Not
  tried on the A55 yet.

Gabriel said:

> zennith connect is ass just saying no glas look the preweiew is still old zenith and it dosent
> connect to rela zennith servers and u cant rlly play anything

Four things, and each one was true of PR #22's Connect:

1. **No glass.** It drew its own blurred page and frosted cards. In Lite (his A55) that came out
   as flat frost, nothing like the home screen's Liquid Glass.
2. **The previews were old Zenith.** Every setup was a hand-drawn picture of a phone from the
   prototype, not anything Zenith draws today.
3. **No server.** Everything was the prototype's sample data, even though Zenith already has a
   server (`server.properties`, the program in `server/`) that keeps shared Looks.
4. **Nothing to play with.** Apply showed its steps and changed nothing. The Community and Beta
   tabs were sample posts.

Connect was rewritten from scratch (`ui/connect/`, `data/connect/`). The built-in app itself (its
key, icon, opening and closing out of its icon) is PR #22's and did not change.

## What a setup is (`ConnectRules.kt`)

A setup is a **Look**, the same thing Settings → Looks keeps: the settings it puts on and, for one
somebody saved, their home screen. There are four sources (`SetupSource`):

- **Zenith Picks** (`ConnectPicks`): ten styles made from what Zenith has. Each is a theme in Light
  or Dark, a wallpaper design in a colour set, an icon style and a widget look. Worn over your own
  home screen, so your apps stay where they are. All free, none that move (the test holds them to it).
- **Featured**: the Looks the server's keeper featured (`python3 zenith_server.py --feature CODE`),
  listed by `GET /v1/looks/featured` and each fetched by `GET /v1/look?code=`.
- **By code**: any shared Look, typed or pasted (`ConnectCodes.clean` takes `LOOK-ABC234`,
  `abc 234`, `zenith://look/ABC234` or a whole message with the code in it). It is opened, never
  put on, until Apply is tapped.
- **Yours**: the Looks saved on this phone.

### Layers

A setup goes on in parts (`Layer`): Wallpaper, Theme & glass, Icons, Widgets, Home screen, Motion &
feel. Every setting a Look keeps (`Looks.KEYS`) is in exactly one layer (tested), so a setup with
every layer on is the Look itself. A layer left off stays as you have it.

What starts on (`ConnectLayers.startingOn`):

- Everything a pick or a Look of yours has.
- For somebody else's Look, everything except **Home screen** and **Motion & feel**. Those are
  their apps in their places and their gestures, which you choose to take.
- **Wallpaper** starts off while yours is a photo (`wallpaperStyle` empty). A design put on goes in
  place of the photo, and the photo is kept nowhere, so Undo cannot bring it back. The page says so,
  and warns when it is turned on.
- A Look saved over a photo has no wallpaper to give: photos don't travel.

`ConnectApply.wear` works out what goes on: your settings with the setup's for the layers that are
on, and the setup's home screen only with Home screen on. Somebody else's pages are fitted to this
phone as they go on (`Looks.keepInstalled`, the grid for this screen). A Plus wallpaper design on a
phone without Plus leaves your wallpaper as it is.

## Apply and Undo, for real

- **Apply** calls `ZenithViewModel.wearFromConnect`, which keeps the home screen as it was (a
  Look, in memory) and puts the setup on with `applyLook`, the same way a Look goes on from Settings.
  The apply screen ticks through its steps while the home screen settles under Connect.
- **Undo** (on the apply screen, the setup's page and the Vault) calls `takeOffConnect`: the home
  screen goes back exactly as it was, other apps' widgets and all. Their ids are held while Undo
  can still use them (`heldWidgetIds`), and so are photos chosen as icons (`photosInUse`).
- Undo lasts until Zenith starts again, like any undo. Applying again replaces what Undo puts back.
- Switching to a Look of your own is Plus, as it is under Settings → Looks. Picks, Featured and
  codes are free.

## Previews of the Zenith you have now (`SetupPhone.kt`)

Every card and page shows **your own page one wearing the setup**, drawn by `PhonePreview`, the
code that draws Settings' previews and the setup screen's phone, with the home screen's widgets,
icons and dock. `ThemedPicture` gives it the setup's theme, palette, icon style and widget look.
The wallpaper is the setup's design painted in its colours (`WallpaperArt`, off the main thread,
kept in a 16 MB cache), or Zenith's copy of your own where the setup keeps yours. The picture
follows the layer chips, so it is what Apply would leave.

The setup's page has the phone large. It turns round its upright middle under a finger, up to 38°
either way, and springs back to rest a little turned (`Spin`). A sheen crosses its glass as it
turns. With movement reduced it stands square and still.

## Glass (`ConnectApp.kt`, `ConnectKit.kt`)

Connect stands on Zenith's copy of your wallpaper (`drawSoftenedWallpaper`, lined up with the real
one), under a veil 40% dark, a little darker at the top and the foot. Every pane, button, chip,
the tab bar and the sheets are the launcher's own `Modifier.glass`, with `LocalWallpaperDim` set
to the veil, so the glass shows the wallpaper exactly as dim as the page round it. That glass is
also Lite's glass, so his A55 sees the same. Panes are moved by placement, never a layer, so their
glass stays lined up. In Clean, or with no copy of the wallpaper, it is the theme's page and panels.

The tab bar is iOS 26's: a capsule of glass floating over the page, the chosen tab on a lighter
capsule that slides and stretches across, and a round pane beside it for typing a code.

## The tabs

- **Discover**: how the server stands (and Try again), search (a code typed here opens its Look),
  filters, the setup of the week, Zenith Picks, Featured (loading, empty and offline each say so),
  Opened by code, Your Looks, and Got a code?
- **Share**: the account (sign in or make one, on Zenith's server), Share this home screen (saved
  as a Look and shared, needs an account and Plus), your shared Looks with their codes to copy,
  send or stop (Stop asks twice), and the ones not shared yet.
- **Vault**: Undo, Save this home screen as a Look, your Looks as cards, and the account backup
  (Back up now, Restore, with when it was last made).
- **Updates**: this build, what each version brought (`ConnectNews`, newest first and always this
  version, tested), a problem report that goes out in a message of your own with the phone's
  details under it (nothing is sent without you, and no address is built in), and the server.

Community and the Beta Hub are gone. Their posts were made up, and the server has no feed to
fill them. A real Community feed needs new server routes, which means a new server message and a
reinstall, so it was left for its own piece of work.

## The server

Nothing on the server changed: `server/zenith_server.py` already serves everything Connect uses.
The app's server is `https://zenithlabz.duckdns.org` (`server.properties`). It answers only while
the PC it runs on is on. On the home Wi-Fi the name may not answer at all (the router doesn't loop
back), so it is tried on mobile data. Connect says when it can't reach it, and everything that
doesn't need it still works.

## Checked

- `ConnectRulesTest`: every Looks key in one layer, what starts on, wear (style over your home,
  pages taken and fitted, a Plus design kept, a photo not replaced unasked), codes in every form,
  search and filters, the week's pick, picks free and valid, news matching this version, the
  report, and the server's words.
- `ConnectShotsTest` (the rig in `/mnt/project-files/tools/connect-rig/`, never committed), run
  against a local copy of the server seeded with three featured Looks: Discover, a pick's page,
  Apply and the home screen wearing it, Undo, Share with a new account and a real code back, the
  Vault, Updates, a Look opened by its code with its pages, Light with Clean put on, and Lite.

Not tried on the A55: the cost of many small phones in Discover (each is a full preview drawn
small), the turn of the phone under a finger, and the real server from mobile data.
