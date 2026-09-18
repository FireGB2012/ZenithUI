# Play Store listing — Zenith UI

Paste these into Play Console → Grow → Store presence → Main store listing.

## App details

| Field | Value |
| --- | --- |
| App name (30 chars max) | `Zenith UI` |
| Package / application ID | `com.gabriel.zenithui` (final once uploaded — cannot be changed later) |
| Category | Personalisation |
| Tags | Launcher, Home screen, Widgets |
| Contact email | gabrieltinndahn06@gmail.com |
| Website | *(optional)* |
| Privacy policy URL | `https://firegb2012.github.io/zenith-privacy/` (served from `docs/index.html` via GitHub Pages) |
| Ads | No |
| In-app purchases | Yes — a $1.99/month subscription and a $14.99 lifetime pass |
| Content rating | Everyone (questionnaire: no user-generated content, no data sharing) |

## Short description (80 characters max)

```
Liquid glass launcher with resizable widgets, icon packs and thumb-first menus.
```

## Full description (4000 characters max)

```
Zenith UI is a home screen that pairs the calm of frosted glass with Android's freedom.

LIQUID GLASS
Panels are translucent, tinted and lit from the top edge, and once Zenith is your default home app they blur a copy of your own wallpaper — so the launcher looks like it is made of your background, not painted over it. Tilt the phone and the light travels round the edge of every panel, with a little weight to it, the way a real bevel catches a real light.

BUILT FOR ONE HAND
Every menu is a bottom sheet. The app drawer's search field sits at the bottom where your thumb already is, and the drawer follows your finger as you pull it up or push it down.

SET UP IN TWENTY SECONDS
Tell Zenith your name, pick a palette, choose your widgets and grid, and it builds the first page for you — your widgets, plus Google, Gmail and your browser. Everything else stays one swipe away instead of being dumped across five pages.

WIDGETS THAT FIT
Six built-in widgets: an oversized clock, a live battery ring, a month calendar, a quick note, quick toggles with a working torch, and a search bar that searches your apps or Google. Long press any widget, tap Resize and drag its handles — the grid has quarter-row steps, so a slim bar takes a slim slot instead of hogging a whole row.

ZENITH PLUS
Try the extras free for a week: a theme maker that builds a palette from any three colours you like, hidden apps that stay out of the drawer behind a PIN, and a backup you can save to a file and restore on any phone. After that it is $1.99 a month, or $14.99 once for the lifetime pass — about eight months of the subscription, and then never again. No ads either way, and nothing that was free before is ever taken away.

AS MANY PAGES AS YOU LIKE
Tap the + beside the page dots for another page, or hold an icon against the side of the screen to carry it across. Pages you leave empty tidy themselves away.

AURA
Open Zenith once a day and the streak pays you: ten points on day one, more for every day in a row, up to fifty. Points buy themes — five to start with, from Ember to Obsidian, and more to come. Miss a day and the streak starts again; nothing you have earned or bought is ever taken back. It is all counted on your phone, with no account and nothing sent anywhere.

MAKE IT YOURS
Six palettes, light/dark/auto, adjustable glass, Compact, Balanced or Roomy grids that size themselves to your screen, icon size, labels on or off, four icon shapes, and support for any Nova or ADW compatible icon pack.

BEDSIDE CLOCK
Tap the clock widget for a dimmed full screen clock with the date and today's calendar — a quiet bedside face that keeps the screen awake while it is open.

SMOOTH BY DESIGN, FOR EVERYONE
Nothing snaps. Icons glide to their new cell, sheets spring up from the edge, palettes cross-fade, and the whole launcher runs on one spring vocabulary tuned for high refresh rate screens — at your screen's full rate, up to 120Hz, whether or not you ever buy anything.

PRIVATE BY DEFAULT
No account, no analytics, no ads. Your name, layout, streak and settings never leave your phone — the only thing that goes online is Google Play checking whether you own Zenith Plus.

Beyond Google Play's own billing permissions, Zenith UI only asks to set your wallpaper and to vibrate briefly when you pick up an icon. Calendar access is optional and only asked for if you want the bedside clock to list today's events.
```

## What's new (500 characters max)

```
First release.
• Liquid glass home screen that blurs your wallpaper
• 20 second guided setup
• Six built-in widgets, resizable in quarter-row steps
• Drawer that tracks your thumb, with app and Google search
• Six palettes, icon packs, four icon shapes, adjustable grid
• Bedside clock with today's calendar
• Add home pages, and carry icons between them
• Zenith Plus: theme maker, hidden apps, backup and restore — free for a week
• Aura: daily streak points that buy five unlockable themes
```

## Graphics

| Asset | File | Requirement |
| --- | --- | --- |
| App icon | `play/icon-512.png` | 512×512 PNG, 32-bit |
| Feature graphic | `play/feature-graphic-1024x500.png` | 1024×500 PNG |
| Phone screenshots | `play/screenshots/*.png` | 1080×2424, at least 2, up to 8 |

Screenshots included (8, in upload order): home screen, wallpaper gallery, setup — pick a colour, setup — the finished page, app drawer, settings, settings — Look, Zenith Plus.

## In-app products

Two ways in, both real, both shown side by side. The pass is the one the app argues for, on the
honest ground that it costs about eight months of the subscription — there is no invented "was"
price anywhere, which Play rejects anyway.

**Lifetime pass** — Monetisation → In-app products:

| Field | Value |
| --- | --- |
| Product ID | `zenith_plus` (cannot be changed once created) |
| Type | One-time product, non-consumable, "multi-quantity" off |
| Name | Zenith Plus — Lifetime |
| Description | Theme maker, hidden apps, and backup & restore. Paid once, yours for good. |
| Price | $14.99 (Play converts for other countries) |

**Monthly** — Monetisation → Subscriptions:

| Field | Value |
| --- | --- |
| Subscription ID | `zenith_plus_monthly` |
| Base plan | Monthly, auto-renewing, $1.99 |
| Offer | Free trial, 7 days, new subscribers |
| Name | Zenith Plus — Monthly |

Create both and **activate** them. If only one exists the app shows only that one; if neither
does, the sheet says the Play Store is unavailable rather than pretending to sell anything.

The seven day trial is offered on the last screen of setup and counted by the app itself, from the
moment it is accepted. It needs nothing configured in Play. Create the one-time product and activate
it *before* the first release goes to testers, or the buy button will correctly say the purchase is
unavailable.

### Who counts the free week

Once `zenith_plus_monthly` carries its 7 day offer, "Start my free days" hands over to Play's own
sheet — the familiar "7 days free, then $1.99" one — and Google counts the week and takes over
billing at the end. Until that subscription exists, the app counts the seven days itself and asks
for nothing, which is what happens today. Same button either way; no new build needed to switch.

To test the purchase without paying: Play Console → Setup → License testing, add the tester's Gmail
address, then buy from a build signed with the upload key and installed from a Play track.

## Data safety form answers

- **Does your app collect or share any of the required user data types?** No. The Zenith Plus
  purchase is handled entirely by Google Play; the app never sees or stores a payment detail, and
  records only a single "purchased" flag on the device. The `INTERNET`, `ACCESS_NETWORK_STATE` and
  `BILLING` permissions in the manifest come from Google's Play Billing library and carry only
  Play's own purchase check; Zenith UI opens no network connections itself.
- **Is all of the user data collected by your app encrypted in transit?** Not applicable — no data leaves the device.
- **Do you provide a way for users to request that their data is deleted?** Not applicable; clearing app storage removes everything.
- Note for the reviewer, if asked: the launcher reads the list of installed applications through `LauncherApps` in order to display them, and — only with the user's consent — today's calendar entries for the bedside clock. Neither is transmitted, logged or stored off-device.

## Permissions declared

| Permission | Why |
| --- | --- |
| `SET_WALLPAPER` | The Customise sheet opens the system wallpaper picker |
| `VIBRATE` | A short tick when you pick up an icon or resize a widget |
| `READ_CALENDAR` | Optional. Requested at runtime the first time you ask the bedside clock to show today's events; the launcher works fully without it |
| `com.android.vending.BILLING` | Selling Zenith Plus through Google Play |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Merged in by the Play Billing library so it can reach Google Play; the app's own code makes no network calls |

`READ_CALENDAR` is in the "personal and sensitive" family, so the listing must explain it: the bedside clock shows today's events on a dimmed screen, the data is read on the device and never leaves it, and every other feature works if you decline.

No `QUERY_ALL_PACKAGES`: app discovery uses `LauncherApps` plus a `<queries>` element, so no sensitive-permission declaration form is required.
