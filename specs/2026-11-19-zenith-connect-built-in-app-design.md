# Zenith Connect, an app that lives inside Zenith

- Date: 2026-11-19
- Branch: `claude/project-thread-moemer`, off `claude/project-thread-yjufje` (108 / 3.8)
- Status: built. Not tried on the A55 yet.

Gabriel asked:

> look at the artifact zenith connect make it like the app looks now ios looks with real glass and
> write it in a real app language so I can put it inside zennith launcher as an app more like a.
> Fake app like it shows up like a real app and u can open it like a real app but it wont show on
> ur phone

Zenith Connect was a clickable prototype in a Claude artifact (HTML, sample data): Discover,
Community, the Beta Hub and the Vault, a setup's page, a one-tap apply screen and a Creator Studio.
It is now Kotlin and Jetpack Compose inside the Zenith APK, in Zenith's look (Liquid Glass, iOS 26
shapes and motion), and it is an app to everything in Zenith while Android never sees it.

## An app, to Zenith only (`BuiltInApps`)

- Its key is written the way every app's is, `zenith.builtin/connect`, so a home cell, the dock,
  a folder, a Look and a backup keep it with no change. `zenith.builtin` is no package Android
  lists.
- `AppRepository` adds it to the app list (sorted in by name, counted as the phone's own), so the
  drawer, the App Library, Search, Suggestions and Most used have it like any app.
- Its icon is Zenith's own (`res/drawable/ic_connect_app*`: adaptive, with a monochrome layer). An
  icon pack knows nothing of it; one chosen for it by hand still wins.
- It goes onto the home screen once, on the first page with room, as a newly installed app does
  (`ZenithViewModel.placeBuiltIns`, remembered in the `zenith_builtins` prefs). Taken off, it stays
  in the drawer and does not come back. A Zenith still being set up gets it the next time it starts.
- Android never sees it: no activity, no manifest entry. It is not in the phone's own app list or
  its Settings, and no other launcher has it.
- Nothing offers to uninstall it: not the remove alert (Dock or home), not the drawer's sheet.
  App info opens Zenith's own, since it is part of Zenith. It has no shortcuts to ask Android for.

## Opening and closing like an app (`HomeScreen`)

- Opening is any app's: the same card out of its icon on the same springs. As the card all but
  fills the screen (`BUILT_IN_SHOWS_AT`, 0.95) Connect comes in over it in 200 ms, then the card
  goes. The home screen stays sunk back behind it, and is not drawn while Connect covers it all
  (`HomeCover`), which saves the A55 drawing a screen nobody can see.
- Connect is recorded as it is drawn. Closing, that picture is on the card from the first frame,
  all of the screen, and shrinks into the icon (the Picture route with `startAt = 1`) while the home
  screen comes forward with it, as any app closes. The same picture is what it opens out of next
  time, unless the theme or the colours changed since. Should no picture be had, it closes on its
  icon's own card, as an app with no picture does, fading off the card as it sets off. Back pressed
  again while it goes does nothing more.
- Home, or Back with nothing left to close in Connect, or Open Zenith on the apply screen, closes
  it. With its icon not on show it goes off to where it lives (`CloseAway`); opened from the drawer
  or a folder and closed with Back, it goes into the icon it came out of there. With movement
  reduced it fades.
- Another app coming over Zenith puts it away unseen; opened again, it is where it was left. The
  screen going off leaves it open, and it is there after the unlock.
- Home while it is still opening turns the card round, as for any app. Touches on it never reach
  the home screen under it, and the two-finger gestures and Home's own gesture stay out of it.

## What is in it (`ui/connect`)

| Prototype | In Zenith |
| --- | --- |
| Discover | Search, the setup of the week, This week / Trending / Top rated / New releases, a grid of cards with live previews (`DiscoverTab`, `SetupPreview`) |
| A setup's page | Its phone, in 3D, to turn with a finger; what applying puts on, layer by layer; its colours; Save to Vault; Apply to Zenith (`SetupPage`) |
| One-tap apply | The prototype's own clock: the three steps, the bar, the hand-off from Connect to Zenith with its burst; Undo; Open Zenith (`ApplyScreen`) |
| Community | Channels, the live dot, posts with likes, votes and replies, a post that lands while you watch (`CommunityTab`) |
| Beta Hub | The build card and its download, the bug report with this phone's details, the roadmap's votes (`BetaTab`) |
| Vault | Your Zenith ID with the name you gave Zenith, Sync and Auto-sync, saved setups, your layouts with Restore (`VaultTab`) |
| Creator Studio and accent | Publish a setup (a name, up to five tags; it leads New releases as yours); Connect's own accent (`ConnectSheets`) |

The glass is Zenith's: the tab bar, the search field, the buttons and the sheets are glass over the
page itself, which is recorded as it is drawn and shown blurred through them (`connectGlass`,
Android 12 and up). In Lite, on older Android, or in a theme without glass they are frosted, with
no blur, and the glows, floats and bursts stay still; with movement reduced too.

## What is real and what is sample

Like the prototype, its setups, people, posts, builds, downloads, bug reports, sync and layouts are
sample data on the phone. Nothing is sent or downloaded, and Apply shows its steps without changing
the home screen. Real: the name you gave Zenith, the phone's model, Android and Zenith versions in
the bug report, the accent you pick and the setups you save (kept on the phone, `zenith_connect`).
Apply wired to Zenith's Looks, and the Vault to its backups, is the obvious next step.

## Code

- `data/BuiltInApps.kt`; `data/connect/ConnectSamples.kt` (the prototype's data) and
  `ConnectRules.kt` (its rules: lists, search, apply steps, studio, vault, beta, community).
- `ui/connect/`: `ConnectApp` (tabs, tab bar, toast), `ConnectState`, `ConnectKit` (glass, buttons,
  motion), `DiscoverTab`, `SetupPreview`, `SetupPage`, `ApplyScreen`, `CommunityTab`, `BetaTab`,
  `VaultTab`, `ConnectSheets`.
- `ui/home/HomeScreen.kt`: `openBuiltIn`, `closeBuiltIn`, `putAwayBuiltIn`, `closeIntoIcon`'s
  `startAt`; no Uninstall for a built-in app.
- `AppRepository` (the app list, app info, uninstall, shortcuts), `IconLoader`, `ZenithViewModel`.

## Checked

Rules first: `BuiltInAppsTest` (4) and `ConnectRulesTest` (28), then the whole suite (872 tests)
and the debug build. Every screen shot on the JVM rig in Dark, Light and Lite, with the frames of
opening out of the icon and closing back into it (Home and Back). The rig draws pictures in
software, which cannot take one of Connect, so there it closes on its icon's card: the close on
its own picture is only seen on a phone.

The rig caught the 3D phone's idle sway asking for frames for ever, which a test waits on: it now
asks for them as an endless animation (`withInfiniteAnimationFrameMillis`), as Compose's own do.

Not checked on a phone: how it runs on the A55 (the page blur, the 3D phone), the open and close on
a real GPU, the finger on the 3D phone, and TalkBack.
