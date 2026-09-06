# Privacy Policy — Zenith UI

**Last updated: 6 September 2026**

> The copy Play actually links to is `docs/index.html`, published at
> <https://firegb2012.github.io/ZenithUI/>. Edit both together, or the hosted policy drifts from
> this one.

Zenith UI is a home screen replacement (launcher) for Android. This policy explains what the app
does with your information. The short version: nothing leaves your phone.

## What Zenith UI collects

**Nothing.** Zenith UI has no accounts, no analytics, no advertising, no crash reporting and no
network permission of its own. No personal data is collected, transmitted or sold.

## What is stored on your device

The app saves the following in its own private storage, readable only by Zenith UI:

- The name you type during setup, used to greet you on your home screen.
- Your home screen layout: which apps, folders and widgets you placed and where.
- Your preferences: palette, light/dark mode, glass strength, grid size, icon size and shape, and
  the icon pack you selected.
- The text of the Quick Note widget, if you use it.

All of this stays on the device. Uninstalling the app, or clearing its storage in Android Settings,
deletes it permanently.

## What Zenith UI reads from your device

To work as a launcher, the app reads:

- **The list of installed applications**, through Android's `LauncherApps` service, so it can show
  your apps and their icons. This list is never sent anywhere.
- **Your wallpaper**, once you have made Zenith UI your default home app, so glass panels can show a
  blurred copy of it. The image is used only for drawing and is never stored or transmitted.
- **Battery level and charging state**, for the Power widget, via a standard system broadcast.
- **Today's calendar entries**, but only if you open the bedside clock and grant calendar access.
  The titles and times are read each time the screen is shown, displayed on your device, and never
  stored by Zenith UI or sent anywhere. Declining leaves every other feature working.

## Permissions

| Permission | Why it is needed |
| --- | --- |
| `SET_WALLPAPER` | Opens the system wallpaper picker when you choose "Wallpaper" in the Customise sheet |
| `VIBRATE` | Provides a short haptic tick when you pick up an icon or resize a widget |
| `READ_CALENDAR` | Optional. Asked for only when you tap "Allow calendar" on the bedside clock, so it can list today's events |

Zenith UI does not request internet, location, contacts, storage, camera or microphone permissions.
Calendar access is optional, requested only in the bedside clock, and can be revoked at any time in
Android Settings without affecting the rest of the launcher.
The torch tile uses the system camera flash API, which needs no permission.

## Leaving the app

Searching the web from the search bar hands your query to the search app or browser you already
have installed. What happens after that is covered by that app's own privacy policy, not this one.
The same applies to any app you launch from Zenith UI.

## Children

Zenith UI is suitable for all ages and collects no data from anyone, including children.

## Changes

If this policy changes, the new version will be published at this address with an updated date.

## Contact

Questions about this policy: gabrieltinndahn06@gmail.com
