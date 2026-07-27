# AnaMuslim (Ənə Muslim)

[![Android CI](https://github.com/cafarovceyxun/AnaMuslim/actions/workflows/android.yml/badge.svg)](https://github.com/cafarovceyxun/AnaMuslim/actions/workflows/android.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

An ad-free, privacy-focused Qur'an **and hadith** application for Azerbaijani
readers. Read, listen to, search and study the Qur'an with an Azerbaijani
translation, word-by-word breakdown, tajweed colouring and recitations —
alongside an Arabic/Azerbaijani hadith library. Everything works offline once
the content is downloaded: no ads, no analytics SDKs, no account required.

Built with Kotlin Multiplatform and Compose Multiplatform — Android is the
shipping platform, iOS is under active migration.

## Download

<a href="https://play.google.com/store/apps/details?id=com.cafarovceyxun.anamuslim">
  <img alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" height="64">
</a>

**Google Play:** https://play.google.com/store/apps/details?id=com.cafarovceyxun.anamuslim

> **AnaMuslim** is an independent fork of
> [**QuranApp** by AlfaazPlus](https://github.com/AlfaazPlus/QuranApp),
> licensed under the GNU General Public License v3.0. See
> [NOTICE](NOTICE) for attribution and a summary of changes.

## Features

### Qur'an reader

- Four reading layouts: verse-by-verse, mushaf (page-by-page) reading,
  translation, and a vertical translation view
- Navigation by chapter, juz, hizb and page
- Arabic scripts: Uthmani, KFQPC v1, KFQPC v2, KFQPC v4 (tajweed) and IndoPak
  in 15-line and 16-line variants; KFQPC page fonts are downloaded on demand
- Tajweed colouring on the Uthmani script
- Word-by-word translation, transliteration and per-word audio (Turkish,
  English and Russian word packs)
- Arabic text can be switched off for translation-only reading; Arabic and
  translation text sizes are set independently
- Surah (chapter) info pages, "similar verses" lookup, and a verse-reference
  quick view
- Auto-scroll, page turning with the volume / page / S Pen keys, and landscape
  and tablet layouts

### Translation

- Azerbaijani Qur'an translation (Mürşüd Yusifoğlu) with per-verse notes,
  downloaded from the project's own backend for offline reading
- Toggle or highlight translator parentheses
- Report a mistake in a verse or a translation directly from the reader;
  maintainers review and correct it in-app, and the fix reaches every user

### Hadith

- Hadith library organised as volumes → books → chapters (bab) → sub-chapters
- Arabic text with Azerbaijani translation, source reference and notes
- Download the whole library for offline reading, with its own cache cleanup
- Dedicated display settings: Arabic font (Noto Naskh, Uthman Taha or Ayat
  Quraan), text sizes, and Arabic / translation / source toggles
- Bookmarks, read history, and in-collection navigation and search

### Audio & recitation

- Verse-by-verse recitation from a selection of reciters, with verse tracking in
  the reader
- Background playback with a media notification, headset controls and Android
  Auto media browsing
- Playback speed, verse and range repeat, and configurable end-of-audio
  behaviour
- Download recitations and word-by-word audio for offline listening

### Search

- Full-text search across Arabic verses, translations, surah names and hadith
- Filters, quick links and search history
- Voice search (Android)

### Library & personal data

- Bookmarks with notes, for both verses and hadith
- Separate read history for the Qur'an and for hadith
- Verse (or hadith) of the day, with an optional daily reminder notification
- Home screen widgets: Verse of the Day and the recitation player
- Export and import of settings and bookmarks
- Storage cleanup for downloaded translations, recitations and script fonts

### Sharing

- Share verses and hadith as text (with WhatsApp-friendly formatting) or as a
  rendered image through the built-in image editor

### Appearance & localisation

- Light / dark / system theme, seven accent palettes, and Material You dynamic
  colour on Android 12+
- App languages: Azerbaijani, English, Russian and Turkish (plus system
  default), with selectable Latin / Arabic numerals

## Tech stack

- **Language:** Kotlin `2.3.20` (Kotlin Multiplatform)
- **UI:** Compose Multiplatform `1.11.1`, Material 3
- **Data:** Room + SQLite (bundled), DataStore preferences, Paging 3
- **Network:** Ktor client, kotlinx.serialization
- **Backend:** Supabase (hadith, Azerbaijani translation, daily content, verse
  reports)
- **Audio (Android):** Media3 / ExoPlayer with a `MediaLibraryService`
- **Platforms:** Android (production) — iOS migration in progress
  (see [IOS_MIGRATION_PLAN.md](IOS_MIGRATION_PLAN.md))
- **Min SDK:** 24 · **Target/Compile SDK:** 36 · **JDK:** 17

## Project structure

| Path           | Contents                                                            |
| -------------- | ------------------------------------------------------------------- |
| `shared/`      | Multiplatform module — screens, view models, database, repositories  |
| `app/`         | Android application: activities, widgets, media service, downloads   |
| `iosApp/`      | iOS host project (Xcode) around the shared Compose UI                |
| `peacedesign/` | Android UI utility library inherited from the upstream project       |
| `inventory/`   | Manifests for recitations, scripts and word-by-word data             |
| `tools/`       | Offline tooling — tajweed colour-data generation and QA scripts      |
| `docs/`        | Backend SQL (Supabase policies)                                      |

## Building

Requires the Android SDK and JDK 17+.

```bash
git clone https://github.com/cafarovceyxun/AnaMuslim.git
cd AnaMuslim
```

Point Gradle at your Android SDK:

```bash
echo "sdk.dir=/path/to/Android/sdk" > local.properties
```

Build a debug APK:

```bash
./gradlew :app:assembleDebug
```

The resulting APK is written to `app/build/outputs/apk/debug/`. Debug builds
install alongside a Play Store install — they use the
`com.cafarovceyxun.anamuslim.test` application id.

> **Note:** `local.properties` is machine-specific and must never be committed.
> Release signing is configured through `keystore.properties` — see
> [keystore.properties.example](keystore.properties.example).

The iOS target is a work in progress; open `iosApp/iosApp.xcodeproj` in Xcode
after a Gradle sync to build it.

## Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md)
before opening a pull request, and follow our
[Code of Conduct](CODE_OF_CONDUCT.md). Translators and reviewers, see
[CONTRIBUTORS.md](CONTRIBUTORS.md).

## Privacy & credits

- [PRIVACY.md](PRIVACY.md) — what the app stores and which services it contacts
- [CREDITS.md](CREDITS.md) — asset sources and their separate licenses
- [SECURITY.md](SECURITY.md) — how to report a security issue
- [OPEN_SOURCE_CHECKLIST.md](OPEN_SOURCE_CHECKLIST.md) — release status & remaining tasks

## License

AnaMuslim is free software licensed under the **GNU General Public License
v3.0** — see [LICENSE](LICENSE). Because it is derived from GPLv3 software,
any distributed version (including binaries) must remain under GPLv3 and make
the corresponding source available.
