# IzzyOnDroid — inclusion request

Paste as a new issue at <https://gitlab.com/IzzyOnDroid/repo/-/issues/new>.

⚠️ **Precondition that only the maintainer can satisfy:** IzzyOnDroid pulls the APK from a
GitHub Release. Before opening this issue there must be a release on the `v2026.09.19` tag with
the **signed** release APK attached. That APK is built with `keystore.properties` + `key.jks`,
neither of which is in the repository — so it has to be produced on the developer machine.

---

**Title:** `[Inclusion] AnaMuslim — Qur'an and hadith in Azerbaijani`

---

* **Package ID:** `com.cafarovceyxun.anamuslim`
* **Name:** AnaMuslim (Azerbaijani: Ənə Muslim)
* **Summary:** Qur'an and hadith in Azerbaijani. Ad-free, offline, fixable by its readers.
* **License:** GPL-3.0-or-later
* **Source code:** https://github.com/cafarovceyxun/AnaMuslim
* **Releases (APK source):** https://github.com/cafarovceyxun/AnaMuslim/releases
* **Current version:** 2026.09.19 (versionCode 202609191)

### About

Qur'an reader with an Azerbaijani translation, word-by-word breakdown, tajweed colouring and 16
reciters, plus an Arabic/Azerbaijani hadith library, prayer times with notifications and home
screen widgets. Everything except fetching new content works offline. An independent GPLv3 fork
of [QuranApp by AlfaazPlus](https://github.com/AlfaazPlus/QuranApp).

### Scanner-relevant facts

* **No ads, no analytics SDK, no trackers.** No Google Play Services, no Firebase, no billing
  library. Dependencies are AndroidX, media3/ExoPlayer, Ktor, okio, kotlinx and JetBrains Compose
  Multiplatform.
* **No account required to read.** Signing in is only used to submit a content correction.
* **Network use:** content (translations, hadith, audio, mushaf page fonts) is downloaded from the
  project's own Supabase backend and from `api.alfaazplus.com`. This is why the F-Droid recipe
  declares the `NonFreeNet` anti-feature.
* **Size:** around 26 MB, about 25 MB of it a bundled SQLite database. This is close to the repo's
  per-app budget; if it is a problem, the database can be moved to a runtime download using the
  mechanism already in place for the page fonts — say the word and it will be done.
* `minSdk = 24`, `targetSdk = 36`, v2 signature, no `debuggable` or `testOnly` flag.

### Metadata

Fastlane metadata is in the repository at `fastlane/metadata/android/` — title, short and full
descriptions, per-version changelogs and a 512×512 icon for `en-US`, `az`, `tr`, `ru` and `ar`,
plus ten phone screenshots under `en-US`.
