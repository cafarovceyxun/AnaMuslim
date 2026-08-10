# iPad release checklist

iPad support is **not** in Build 1 / version 2026.08.08. It was deliberately held back so the first
release could clear review as an iPhone app; iPad ships as the next update. Decided 2026-08-09, see
`IOS_MIGRATION_PLAN.md` → *HAZIRDA HARDAYIQ*.

Everything below is prepared already except the flag itself.

## Prerequisite

The first version must be **live on the App Store**, not merely approved. While a version is in
review, submitting a new build means withdrawing that version first — which puts the app back at
the end of the queue for no gain.

## 1. Flip the flag — the only code change

`iosApp/iosApp.xcodeproj/project.pbxproj`, **two** occurrences (Debug and Release):

```
TARGETED_DEVICE_FAMILY = 1;      ->      TARGETED_DEVICE_FAMILY = "1,2";
```

Both, not one. Debug alone makes the simulator lie; Release alone ships an app whose local builds
do not match what users get.

## 2. Bump the version

`versionName` becomes the new release date (the scheme in use is `YYYY.MM.DD`), and the build
number increments. App Store Connect rejects a build whose version/build pair it has already seen.

## 3. Verify before archiving

- `/verify` — the four compile targets.
- Device architecture is **not** covered by those four (`iosSimulatorArm64` ≠ `iosArm64`). For a
  release archive run `:shared:linkDebugFrameworkIosArm64` too — a dependency-version drift surfaces
  at link time, not compile time.
- Open the app on an iPad simulator and on an iPhone simulator. The universal flag changes layout
  on both: `UIRequiresFullScreen` is not set, so iPad multitasking gives the app arbitrary window
  widths, and the phone path is what every existing user has.

## 4. Screenshots

`fastlane/screenshots/ios/ipad-13/` — six shots at 2064 × 2752, ready to upload. The 12.9-inch
derivative set is in `ipad-12.9/` if App Store Connect shows that slot instead.

⚠️ The iPad screenshot slot **only appears once the uploaded build is universal**. Upload the build
first, then the screenshots.

If the UI has changed since 2026-08-09, re-shoot rather than upload stale frames — Guideline 2.3.3
covers screenshots that do not reflect the app.

## 5. Store text — no change needed

`tr/`, `en-US/` and `ru/` descriptions name no device (checked 2026-08-09), so nothing claims
iPhone-only and nothing needs rewriting. If iPad is worth calling out in `promotional_text.txt`,
that field is editable without a new build.

## 6. Release notes

There is no `release_notes.txt` in this directory yet — App Store Connect asks for "What's New" on
every update after the first. Worth writing when the update is prepared: the two-pane reader on
iPad and the corrected bottom-bar spacing are the user-visible changes.
