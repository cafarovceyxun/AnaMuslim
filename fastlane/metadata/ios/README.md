# App Store metadata

Adapted from `fastlane/metadata/android/`. Three things differ from the Play Store copy, all forced
by App Store Connect rather than chosen freely.

## 1. Primary language is Turkish — decided 2026-08-08

App Store Connect supports 50 metadata languages as of March 2026 and **Azerbaijani is not one of
them**, so there is no `az/` here — only `tr/`, `en-US/`, `ru/` and `ar/`. This constrains the *store
listing only*; the app itself still ships Azerbaijani, Arabic, English, Turkish and Russian
(`shared/src/commonMain/composeResources/values*`).

Whatever the primary language is, it is what App Store shows to anyone whose device language has no
localization — which includes every Azerbaijani-speaking reader, the app's main audience. **Turkish
is set as primary** because Azerbaijani readers understand it far more readily than English. Set it
in App Store Connect when creating the app record (Primary Language); it cannot be changed casually
afterwards, so get it right on the first pass.

`en-US/`, `ru/` and `ar/` remain as additional localizations, shown to readers whose devices are set
to those languages.

## 1b. Arabic — added 2026-08-27

`ar/` exists because the app's Arabic interface is **complete** (751/751 strings in
`values-ar`, verified by diffing the `name="..."` keys against `values/`), and selecting it turns
the whole layout right-to-left. A listing in a language the app does not actually speak would be
the wrong trade; this one it speaks fully.

Two things differ from the other three locales:

- **`name.txt` is `أنا مسلم`, not `Ənə Muslim`.** This follows the app itself: `app_name` in
  `values-ar` is `أنا مسلم` while every other locale keeps `Ənə Muslim`, so Arabic is already the
  deliberate exception. Note the home-screen icon label still reads `Ənə Muslim` in every language —
  `CFBundleDisplayName` is not localized (there are no `.lproj` files, and `knownRegions` is
  `en, Base`). Localizing it would mean adding `ar.lproj/InfoPlist.strings` and wiring it into
  `project.pbxproj`.
- **The description says outright that the translation is Azerbaijani.** An Arabic reader needs the
  Qur'an and hadith *originals*, which the app has in Arabic — but the translation is not in their
  language, and the listing says so in the third paragraph rather than letting them find out after
  installing.

⚠️ **`CFBundleLocalizations` is deliberately NOT declared.** It would make the App Store product
page list all five languages, but the app sets its own language by overwriting the `AppleLanguages`
user default (`IosAppLocale.kt`) — which is also what iOS's per-app Language row in Settings writes.
Declaring bundle localizations creates that Settings row and hands the same key a second owner. Test
on a device before adding it; the store listing is not worth breaking the in-app language picker.

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
| `release_notes` | 4000 | The "What's New" text, required on every update after the first; taken from `CHANGELOG.md` with the Android-only items removed (Guideline 2.3.10 forbids naming other platforms and their hardware) |

Count characters, not bytes: `wc -m` under a non-UTF-8 locale overcounts `ə`, `ç`, `ı` and will make
a field look over-limit when it is not.

## Not covered here

- Screenshots — see `fastlane/screenshots/ios/README.md` (6.9" for iPhone, `ipad-13/` for iPad)
- The App Privacy questionnaire, which must agree with `iosApp/iosApp/PrivacyInfo.xcprivacy`
- Age rating
- The iPad update, held back from the first release — see `IPAD_RELEASE_CHECKLIST.md`
