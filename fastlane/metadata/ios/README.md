# App Store metadata

Adapted from `fastlane/metadata/android/`. Three things differ from the Play Store copy, all forced
by App Store Connect rather than chosen freely.

## 1. Primary language is Turkish — decided 2026-08-08

App Store Connect supports 50 metadata languages as of March 2026 and **Azerbaijani is not one of
them**, so there is no `az/` here — only `tr/`, `en-US/` and `ru/`. This constrains the *store
listing only*; the app itself still ships Azerbaijani, English, Turkish and Russian
(`shared/src/commonMain/composeResources/values*`).

Whatever the primary language is, it is what App Store shows to anyone whose device language has no
localization — which includes every Azerbaijani-speaking reader, the app's main audience. **Turkish
is set as primary** because Azerbaijani readers understand it far more readily than English. Set it
in App Store Connect when creating the app record (Primary Language); it cannot be changed casually
afterwards, so get it right on the first pass.

`en-US/` and `ru/` remain as additional localizations, shown to readers whose devices are set to
those languages.

## 2. No HTML

Play renders `<b>` in descriptions; App Store shows the tags as literal text. The `description.txt`
files here are the Play copy with the markup stripped, nothing else changed.

## 3. Keywords carry Azerbaijani spellings

Because the Turkish listing is what Azerbaijani readers land on, `tr/keywords.txt` includes both
spellings where they diverge — Turkish *kuran* / Azerbaijani *quran*, *hadis* / *hedis*, plus
*tercume*. A reader searching in Azerbaijani would otherwise miss the app entirely.

## Field limits (all files verified)

| Field | Limit | Notes |
|---|---|---|
| `name` | 30 | Matches the on-device name (`CFBundleDisplayName`) |
| `subtitle` | 30 | No Play equivalent — written fresh; Play's `short_description` is 80 and does not fit |
| `keywords` | 100 | Comma-separated, no spaces after commas — spaces count against the limit |
| `promotional_text` | 170 | Editable without submitting a new build |
| `description` | 4000 | |

Count characters, not bytes: `wc -m` under a non-UTF-8 locale overcounts `ə`, `ç`, `ı` and will make
a field look over-limit when it is not.

## Not covered here

- Screenshots — see `fastlane/screenshots/ios/README.md` (the captured set is 6.3"; App Store needs 6.9")
- The App Privacy questionnaire, which must agree with `iosApp/iosApp/PrivacyInfo.xcprivacy`
- Age rating
