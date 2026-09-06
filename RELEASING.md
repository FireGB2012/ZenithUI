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

## Play Console checklist

- [ ] Create the app (Personalisation category, free).
- [ ] Upload `app-release.aab` to Internal testing first; install from the test link before going to production.
- [ ] Store listing: copy from `play/listing.md`.
- [ ] Graphics: `play/icon-512.png`, `play/feature-graphic-1024x500.png`, `play/screenshots/`.
- [ ] Privacy policy: paste `https://firegb2012.github.io/ZenithUI/`. It is served from `docs/index.html`
      by GitHub Pages (Settings -> Pages -> Source: `main`, folder `/docs`).
- [ ] Data safety: "No data collected" — full answers in `play/listing.md`.
- [ ] Content rating questionnaire: no user-generated content, no ads, no data sharing.
- [ ] Target audience: 13+ or Everyone; no ads declaration.
- [ ] App access: no login required — tick "All functionality is available without special access".
- [ ] Keep Play App Signing enabled (default) so Google holds the release key.

## Notes for the reviewer

Zenith UI replaces the home screen, so on first run it asks to become the default home app via the
standard `RoleManager` dialog. It needs no sensitive permissions: app discovery uses `LauncherApps`
with a `<queries>` element rather than `QUERY_ALL_PACKAGES`.
