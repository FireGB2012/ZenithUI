# Releasing Zenith UI

## Identity

| | |
| --- | --- |
| Application ID | `com.gabriel.zenithui` |
| Debug builds install as | `com.gabriel.zenithui.debug` (so a debug and a release build can coexist) |
| minSdk / targetSdk | 31 (Android 12) / 37 |
| Version | `versionCode = 1`, `versionName = "1.0"` in `app/build.gradle.kts` |

The application ID is permanent once the first build is uploaded to Play. Change it **before**
uploading if you want a different one — edit `applicationId` and `namespace` in
`app/build.gradle.kts` and the `package` lines in the Kotlin sources.

## Signing

The upload key lives in `zenith-upload.jks` with its passwords in `keystore.properties`. Both are
git-ignored. `keystore.properties.example` shows the format.

**Back up `zenith-upload.jks` and the password somewhere safe (password manager, offline copy).**
With Play App Signing you can ask Google to reset a lost upload key, but losing it still costs you
days. Never commit either file.

Key details: RSA 4096, alias `zenith-upload`, valid ~27 years, `CN=Zenith UI`.

## Build

```bash
# Android App Bundle — this is what you upload to Play
./gradlew :app:bundleRelease
# app/build/outputs/bundle/release/app-release.aab

# Signed APK — for sideloading and testing the release build directly
./gradlew :app:assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

Both are signed automatically when `keystore.properties` is present, minified and shrunk with R8
(`optimization { enable = true }` plus `app/proguard-rules.pro`). Current sizes: **AAB 3.5 MB,
APK 2.5 MB** — the debug build is ~57 MB, so always test performance on the release build.

Verify a build before uploading:

```bash
"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --print-certs app/build/outputs/apk/release/app-release.apk
```

## Before each upload

1. Bump `versionCode` (must increase every upload) and `versionName`.
2. Install the release APK on a real device and walk the first-run flow — R8 shrinking is only
   exercised in release builds.
3. Update "What's new" in `play/listing.md`.
4. If the purchase path changed, test it with a licence tester on a build installed from a Play
   track. Play Billing does nothing in a sideloaded build, and nothing at all on an emulator with
   no Play Store — the buy button says so rather than failing quietly, which is the expected
   result there, not a bug.

## Play Console checklist

- [ ] Create the app (Personalisation category, free).
- [ ] Upload `app-release.aab` to Internal testing first; install from the test link before going to production.
- [ ] Store listing: copy from `play/listing.md`.
- [ ] Graphics: `play/icon-512.png`, `play/feature-graphic-1024x500.png`, `play/screenshots/`.
- [ ] Privacy policy: paste `https://firegb2012.github.io/zenith-privacy/`. It is served from `docs/index.html`
      by GitHub Pages (Settings -> Pages -> Source: `main`, folder `/docs`).
- [ ] Data safety: full answers in `play/listing.md`. "No data collected" only for a build with no
      server address; a build with one collects what is listed there.
- [ ] Content rating questionnaire: no user-generated content, no ads, no data sharing.
- [ ] Target audience: 13+ or Everyone; no ads declaration.
- [ ] App access: tick "All functionality is available without special access". An account is
      optional and anyone can make one in the app; see `play/listing.md`.
- [ ] Monetisation → In-app products: create `zenith_plus` (the $14.99 lifetime pass) exactly as
      specified in `play/listing.md` and **activate** it.
- [ ] Monetisation → Subscriptions: create `zenith_plus_monthly` at $1.99/month with a 7 day free
      trial offer, and **activate** it. Do both before the first release reaches testers, or the
      app will correctly report that the purchase is unavailable.
- [ ] Setup → License testing: add your own Gmail address so you can test the buy flow for free.
- [ ] Note: the moment `zenith_plus_monthly` has its free-trial offer, Play runs the seven days
      instead of the app. No new build needed — see `play/listing.md`.
- [ ] Declare in-app purchases on the store listing (the Ads answer stays "No").
- [ ] Keep Play App Signing enabled (default) so Google holds the release key.

## Turning the server on

The app has no server until `server.properties` says where it is. With both lines empty, nothing of
accounts is shown and the app sends nothing. To turn it on:

1. Send `server/PROMPT-FOR-SERVER-AI.md` to whoever runs the server. It installs
   `server/zenith_server.py` unchanged and answers with a ZENITH SERVER REPORT.
2. In Google Cloud (APIs & Services → Credentials) make one **Web application** OAuth client and an
   **Android** client for each key the app is ever signed with: this PC's debug key
   (`com.gabriel.zenith_Launcher.debug`), the upload key and — after the first upload — Play's
   **App signing** key, whose SHA-1 is in Play Console → Setup → App signing
   (`com.gabriel.zenith_Launcher` for both). Without the App signing one, Continue with Google
   works in a build installed by hand and fails in the one installed from Play.
3. Put the server's `https://` address and the Web client ID in `server.properties`, and give the
   same Web client ID to the server (`ZENITH_GOOGLE_CLIENT_ID`, as the prompt's "Later" says).
4. Build, and walk it on a real phone: create an account, sign out, sign in, Continue with Google,
   back up to the account and restore, use a code from a second account, delete the account.
5. Before that build reaches anybody: copy `docs/index.html` to the `zenith-privacy` repo (the
   policy now describes accounts), fill in Data safety and the Delete account URL from
   `play/listing.md`, and paste the new listing text.

To try it without any of that: `python server/zenith_server.py` on this PC, then
`./gradlew :app:assembleDebug -PzenithServerUrl=http://10.0.2.2:8787` and install on an emulator.
A debug build may talk plain HTTP to the PC it runs beside, and to nothing else.

## Notes for the reviewer

Zenith UI replaces the home screen, so on first run it asks to become the default home app via the
standard `RoleManager` dialog. It needs no sensitive permissions: app discovery uses `LauncherApps`
with a `<queries>` element rather than `QUERY_ALL_PACKAGES`.
