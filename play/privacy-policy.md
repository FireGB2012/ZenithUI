# Privacy Policy — Zenith UI

**Last updated: 31 October 2026**

> The copy Play actually links to is `docs/index.html`, published from the public repo
> github.com/FireGB2012/zenith-privacy at <https://firegb2012.github.io/zenith-privacy/>. Edit all
> three together, and copy `docs/index.html` into that repo, or the hosted policy drifts from this one.
>
> This version describes Zenith accounts. It has to be the hosted one no later than the first build
> with an address in `server.properties` reaches anybody; publishing it earlier does no harm.

Zenith UI is a home screen replacement (launcher) for Android. This policy explains what the app
does with your information. The short version: Zenith works without an account, and without one
nothing you do in it leaves your phone — with two exceptions you choose: the Weather widget, which
sends the place you pick to a weather service, and opening a Look somebody shared, which asks
Zenith's server for it. If you choose to make a Zenith account, the app sends Zenith's server only
what is listed under "A Zenith account" below.

## What Zenith UI collects

**Without an account: nothing.** Zenith UI has no analytics, no advertising and no crash reporting,
and no personal data is sold or shared. Until you make a Zenith account or sign in to one, the only
network traffic the app produces is Google Play Billing checking with Google Play whether you own
Zenith Plus — see Payments below — and, only if you ask for them, the requests described under
"The Weather widget" and "Opening a Look".

## The Weather widget

The Weather widget is off until you choose a place for it. When you search for one, what you type is
sent to Open-Meteo's place search (geocoding-api.open-meteo.com). Once you choose a place, its
latitude and longitude — and nothing else: no name, no account, no identifier — are sent to
Open-Meteo's forecast service (api.open-meteo.com), at most every half hour while the home screen is
showing, and the forecast that comes back is kept on your phone. Zenith does not use your phone's
location for this; it only ever sends the place you picked. Like any server, Open-Meteo sees the
internet address the request comes from; its own terms and privacy policy, at open-meteo.com, cover
what it does with that. Choosing "Stop getting the weather" in the widget's sheet stops the requests.

## The island

The island (Zenith Plus) is black round your phone's camera, shown over other apps. It needs
Android's "Display over other apps" permission, which you give in Android's settings and can take
back there at any time. It shows the music playing, a call or timer that is running, the ringer when
you change it, the battery as you plug in, and, for a moment, a notification as it arrives: whose it
is, and its message if you tap the island to read it. It reads these through the notification access
described under Permissions, if you have given it. Nothing it shows is kept, logged or sent
anywhere.

## Opening a Look

A Look is a home screen design: its colours, wallpaper design, icon choices and layout. Under
Settings > Looks you can type a code somebody gave you, or tap Featured looks. Either one asks
Zenith's server for the Look, with or without an account: the request holds the code, or asks for
the list, and nothing about you. Like any server, Zenith's sees the internet address the request
comes from, as described under "A Zenith account". The Look that comes back is put on your home
screen and kept on your phone only if you save it. Nothing is sent until you tap.

## A Zenith account

An account is optional. It makes your Aura code count, for the friend who uses it and for you, and
with Zenith Plus it can keep a backup of your home screen and share Looks. You make one in the app under Settings >
Account, with a username and a password or with Continue with Google. From then on the app talks to
Zenith's server, which is run by the developer, over an encrypted (HTTPS) connection, and the server
keeps:

- **How you sign in.** Either your username and a salted hash of your password — never the password
  itself — or, with Continue with Google, the number Google uses to identify your Google account to
  this app. The sign-in token Google hands the app also contains your email address and name; the
  server reads the token once to check that it is genuine and keeps only that number. Zenith asks
  for no email address, name or age of its own.
- **Your Aura code**, and a record of each time a code is used: whose code it was, who used it and
  when. That is how the server counts. The person whose code you use sees their count go up; they
  are not told who you are.
- **One backup of your home screen**, only if you have Zenith Plus and tap "Back up to your
  account". It holds what a backup file holds: your layout (which apps, folders and widgets you
  placed and where), your preferences, the name you typed during setup, the text of the Quick Note
  widget, your Aura points and streak, and the list of apps you chose to hide. It never holds the
  PIN for hidden apps or anything about your purchase. Each new backup replaces the one before. It
  is kept as it was sent, not locked with a password of its own, so whoever runs the server could
  read it.
- **The Looks you share**, only if you have Zenith Plus and tap Share on a Look. A shared Look
  holds how your home screen looks — its colours, wallpaper design, icon shape, pack and style, the
  grid, and which apps, folders and Zenith widgets are where — and the name you gave the Look. It
  never holds your name, your notes or lists, the apps you hid, photos, other apps' widgets, the
  names you gave apps, or anything about Aura or your purchase. Anybody who has its code can open
  it; it is shown to nobody else unless the developer chooses it for the Featured list. An account
  keeps its twenty newest. Stop sharing, or deleting the Look in the app, removes it from the
  server.
- **A sign-in key** for each phone you are signed in on, stored as a hash.

Like any server, it also sees the internet (IP) address a request comes from. It uses that to limit
how often one address can try to sign in, and the address appears in the server's access log beside
the time and the kind of request. Passwords, sign-in keys and the contents of requests are never
written to a log, and log entries are deleted as the log rotates.

While you are signed in, the app asks the server how your account stands when you open the Account
or Backup page and, at most every few minutes, when you return to the home screen; that is how a
friend using your code reaches your phone. None of this is used for advertising, sold, or shared
with anyone. Google takes part only if you choose Continue with Google, and its own privacy policy
covers its side.

## Deleting your account

You can delete your account at any time: in the app under Settings > Account > Delete account, or
without the app at <https://zenithlabz.duckdns.org/delete-account>. Deleting removes the account, its sign-in keys, its Aura code, its backup and the Looks it shared from the server at once. The
records of codes used stay, with nothing left in them that says whose they were. Signing out removes
the sign-in key for that phone. Uninstalling the app does not delete an account. For a copy of what
is kept about your account, write to the address at the end.

## What is stored on your device

The app saves the following in its own private storage, readable only by Zenith UI:

- The name you type during setup, used to greet you on your home screen.
- Your home screen layout: which apps, folders and widgets you placed and where.
- Your preferences: palette, light/dark mode, glass strength, grid size, icon size and shape, and
  the icon pack you selected.
- The text of the Quick Note widget, the To-do widget's list, the Countdown's name and date, the
  World clock's cities and the Weather widget's place, if you use them.
- Names you gave apps, icons you chose for them, and photos you picked for an app's icon or the
  Photo frame widget, copied small into the app's own storage.
- Whether you own Zenith Plus, and the date your free trial began.
- The palette you built, the apps you chose to hide, and the PIN you set for them, if you use those
  features. The PIN keeps a list tidy; it is not a device security measure and is stored as typed.
- Your Aura points, your daily streak and the day you last checked in, the themes you unlocked with
  points, your Aura code, and whether you have already used someone else's. Without a Zenith account
  none of this is sent anywhere.
- If you are signed in to a Zenith account: the sign-in key for this phone, what the server last
  said about the account and, with Google sign-in, the email address of that Google account, so the
  Account page can show who is signed in. These are left out of Android's own backups and
  phone-to-phone transfers.

All of this stays on the device, except for what you send to a Zenith account as described above.
Uninstalling the app, or clearing its storage in Android Settings, deletes it from the device
permanently.

## Payments

Zenith Plus is optional and sold through Google Play in two forms: a monthly subscription and a
one-time lifetime pass. Google handles the payment and tells the app one thing: whether this Google
account currently owns Plus. Zenith UI never sees your card, your name or your address, and stores
nothing about the payment beyond that yes or no. Subscriptions are managed and cancelled in the
Google Play app. Google's own privacy policy covers what Play does with the transaction.

The seven-day free trial is counted on your phone from the moment you accept it, unless Google Play
offers it as part of the subscription, in which case Google counts it.

A backup file you export contains your layout and settings, and is written only where you choose to
put it. Restoring a backup never changes your purchase or your trial.

Zenith's server is told nothing about your purchase. Backing up to your account is a Zenith Plus
feature, but whether you own Plus is checked on your phone.

## What Zenith UI reads from your device

To work as a launcher, the app reads:

- **The list of installed applications**, through Android's `LauncherApps` service, so it can show
  your apps and their icons. The list as a whole is never sent anywhere. A backup you send to a
  Zenith account names the apps you placed on your home screen and the ones you hid, as described
  above.
- **Your wallpaper**, once you have made Zenith UI your default home app, so glass panels can show a
  blurred copy of it. The image is used only for drawing and is never stored or transmitted.
- **Battery level and charging state**, for the Power widget, via a standard system broadcast.
- **Today's calendar entries**, but only if you open the bedside clock and grant calendar access.
  The titles and times are read each time the screen is shown, displayed on your device, and never
  stored by Zenith UI or sent anywhere. Declining leaves every other feature working.
- **The motion sensor (accelerometer)**, only while the home screen is showing, so the light on the
  glass panels moves as you tilt the phone. Readings are used for drawing and are never stored or
  sent anywhere.
- **Notifications, only if you give Zenith notification access** in Android's settings, for
  notification dots and the Now playing widget. Zenith counts how many notifications each app has
  showing, and reads the title, artist and album picture of whatever is playing; it does not read,
  keep or send what your notifications say.
- **Apps' shortcuts and widgets**, through Android's launcher services, so they can be shown and
  started from the home screen. They are drawn on the phone and never sent anywhere.
- **Nothing through the lock helper.** If you choose a gesture that locks the phone, Android asks you
  to switch on Zenith's lock helper, an accessibility service. It asks Android for no events and
  cannot see the screen; its only use is pressing lock when you make the gesture.

## Permissions

| Permission | Why it is needed |
| --- | --- |
| `SET_WALLPAPER` | Sets a wallpaper you pick from Zenith's own gallery, which the app paints on the phone. Choosing your own photo opens the system wallpaper picker instead |
| `VIBRATE` | Provides a short haptic tick when you pick up an icon or resize a widget |
| `READ_CALENDAR` | Optional. Asked for only when you tap "Allow calendar" on the bedside clock, so it can list today's events |
| `com.android.vending.BILLING` | Lets Google Play sell Zenith Plus inside the app |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Lets the app reach Zenith's server once you make or sign in to a Zenith account, lets Google's Play Billing library reach Google Play, and lets the Weather widget reach Open-Meteo once you choose a place for it. Otherwise Zenith UI itself opens no network connections |
| `EXPAND_STATUS_BAR` | Lets a swipe down on the home screen pull down the notification shade, as a phone's own launcher does |
| Notification access (optional) | Given in Android's settings, for notification dots and the Now playing widget, as described above |
| Accessibility: the lock helper (optional) | Switched on in Android's settings, only to lock the phone with a gesture you chose |

Zenith UI does not request location, contacts, storage, camera or microphone permissions. Calendar
access is optional, requested only in the bedside clock, and can be revoked at any time in Android
Settings without affecting the rest of the launcher. The torch tile uses the system camera flash API,
which needs no permission.

## Leaving the app

Searching the web from the search bar hands your query to the search app or browser you already
have installed. What happens after that is covered by that app's own privacy policy, not this one.
The same applies to any app you launch from Zenith UI.

## Children

Zenith UI is suitable for all ages and is not aimed at children. Without an account it collects no
data from anyone, including children. An account is optional and asks for no name, age or email
address, and it can be deleted at any time as described above.

## Changes

If this policy changes, the new version will be published at this address with an updated date.

## Contact

Questions about this policy: gabrieltinndahn06@gmail.com
