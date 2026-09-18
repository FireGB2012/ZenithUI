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
- [ ] Data safety: "No data collected" — full answers in `play/listing.md`.
- [ ] Content rating questionnaire: no user-generated content, no ads, no data sharing.
- [ ] Target audience: 13+ or Everyone; no ads declaration.
- [ ] App access: no login required — tick "All functionality is available without special access".
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

## Notes for the reviewer

Zenith UI replaces the home screen, so on first run it asks to become the default home app via the
standard `RoleManager` dialog. It needs no sensitive permissions: app discovery uses `LauncherApps`
with a `<queries>` element rather than `QUERY_ALL_PACKAGES`.
