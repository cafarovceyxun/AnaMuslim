# App Store metadata

Adapted from `fastlane/metadata/android/`. Two differences from the Play Store copy, both forced by
App Store Connect rather than chosen:

**1. There is no Azerbaijani localization.** App Store Connect supports 50 metadata languages as of
March 2026 and Azerbaijani is not one of them — so `az/` has no counterpart here, only `en-US/`,
`tr/` and `ru/`. This constrains the *store listing only*; the app itself still ships Azerbaijani,
English, Turkish and Russian (`shared/src/commonMain/composeResources/values*`).

The practical consequence: a reader in the Azerbaijan storefront whose device is set to Azerbaijani
falls back to the primary localization. Which language that primary listing is written in is a
product decision, not a technical one — see "Open decision" below.

**2. No HTML.** Play renders `<b>` in descriptions; App Store shows the tags as literal text. The
`description.txt` files here are the Play copy with the markup stripped, nothing else changed.

## Field limits (all current files verified against them)

| Field | Limit | Notes |
|---|---|---|
| `name` | 30 | Matches the on-device name (`CFBundleDisplayName`) |
| `subtitle` | 30 | No Play equivalent — written fresh; Play's `short_description` is 80 and does not fit |
| `keywords` | 100 | Comma-separated, no spaces after commas (spaces count against the limit) |
| `promotional_text` | 170 | Editable without submitting a new build — useful for announcements |
| `description` | 4000 | |

## Open decision

The primary localization is currently **English (U.S.), written in English**. The alternative is to
keep the slot as English (U.S.) but write the copy in Azerbaijani, since that is who actually reads
it. Apps in markets whose language Apple does not support commonly do this; the risk is that a
reviewer objects to metadata not matching its declared localization. Swapping is a one-file change.

## Not covered here

- Screenshots (6.9" iPhone required; the app is iPhone-only — `TARGETED_DEVICE_FAMILY = 1`)
- The App Privacy questionnaire, which must agree with `iosApp/iosApp/PrivacyInfo.xcprivacy`
- Age rating
