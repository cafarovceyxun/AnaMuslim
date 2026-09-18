# F-Droid — Request For Packaging

Paste as a new issue at <https://gitlab.com/fdroid/rfp/-/issues/new> (template: "Request For Packaging").
Alternatively, skip the RFP and open a merge request against <https://gitlab.com/fdroid/fdroiddata>
adding `metadata/com.cafarovceyxun.anamuslim.yml` — the recipe in this folder is ready for that.

---

**Title:** `AnaMuslim — Qur'an and hadith in Azerbaijani`

---

* **Package ID:** `com.cafarovceyxun.anamuslim`
* **Name:** AnaMuslim (Azerbaijani: Ənə Muslim)
* **Summary:** Qur'an and hadith in Azerbaijani. Ad-free, offline, fixable by its readers.
* **License:** GPL-3.0-or-later
* **Source code:** https://github.com/cafarovceyxun/AnaMuslim
* **Issue tracker:** https://github.com/cafarovceyxun/AnaMuslim/issues
* **Categories:** Reading

### Description

Read, listen to, search and study the Qur'an with an Azerbaijani translation, word-by-word
breakdown, tajweed colouring and 16 reciters — alongside an Arabic/Azerbaijani hadith library.
Once content is downloaded, everything keeps working with the network off. There are no ads and
no analytics SDK, and no account is needed to read. Readers can report a mistake in a verse or
translation from inside the app; an approved correction reaches every user without a store
release.

It is an independent fork of [QuranApp by AlfaazPlus](https://github.com/AlfaazPlus/QuranApp),
also GPLv3. The repository's `NOTICE` documents attribution and the changes made.

### Why it should qualify

* Every dependency is FOSS: AndroidX, media3/ExoPlayer, Ktor, okio, kotlinx, JetBrains Compose
  Multiplatform. **No Google Play Services, no Firebase, no analytics SDK, no ads, no billing.**
* No prebuilt `.jar` / `.aar` / `.so` / `.dex` committed to the repository.
* No product flavors, no NDK, no ABI splits — the recipe is `subdir: app`, `gradle: [yes]`.
* `:app:assembleRelease` has been verified with `keystore.properties` absent, i.e. exactly the
  state the buildserver is in: BUILD SUCCESSFUL, producing an unsigned release APK.

### Anti-feature to declare

**`NonFreeNet`.** Content — translations, hadith text, recitation audio and mushaf page fonts —
is fetched from the project's own Supabase backend and from `api.alfaazplus.com`. The server side
is not published. The app is fully usable offline once content is downloaded, and reading needs
no account; signing in is only required to submit a content correction.

### Notes for the packager

* **Fastlane metadata is already in the repository** (`fastlane/metadata/android/`): title, short
  and full descriptions, per-version changelogs and a 512×512 icon for `en-US`, `az`, `tr`, `ru`
  and `ar`, plus ten phone screenshots under `en-US`.
* **Tags** are `vYYYY.MM.DD`. The repository also carries one non-version tag
  (`tts-az-quran-v1`), which `UpdateCheckMode: Tags ^v[0-9.]+$` excludes.
* **Toolchain is current** — AGP 9.4.0, Gradle 9.6.0, Kotlin 2.3.20. The `shared/` module is
  Kotlin Multiplatform and declares iOS targets alongside Android; the iOS targets are not
  buildable on Linux and are skipped there, while the Android target builds normally. Flagging it
  in case the buildserver needs a specific Gradle/JDK combination.
* **Size:** the release APK is roughly 26 MB, about 25 MB of which is a bundled SQLite database at
  `app/src/main/assets/db`.
* **Signing:** the Google Play build uses Play App Signing, so an F-Droid build will necessarily
  carry a different signature and cannot be installed over a Play install. The store description
  already documents the switch (export → uninstall → install → import).
