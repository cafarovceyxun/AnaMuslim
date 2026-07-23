# AnaMuslim

An ad-free, privacy-focused Qur'an application for reading, listening to, and
exploring the Holy Qur'an — with translations, word-by-word breakdown,
recitations, tafsir, and full-text search.

> **AnaMuslim** is an independent fork of
> [**QuranApp** by AlfaazPlus](https://github.com/AlfaazPlus/QuranApp),
> licensed under the GNU General Public License v3.0. See
> [NOTICE](NOTICE) for attribution and a summary of changes.

## Features

- 📖 Read the full Qur'an with multiple scripts and fonts
- 🌍 Multiple translations (including an Azerbaijani translation)
- 🔤 Word-by-word translation and transliteration
- 🔊 Audio recitations with a background media player
- 📝 Tafsir (commentary) support
- 🔎 Full-text search across verses and translations
- 🗺️ Reader with atlas / navigation support
- 🎨 Modern UI built with Compose Multiplatform

## Tech stack

- **Language:** Kotlin `2.2.21` (Kotlin Multiplatform)
- **UI:** Compose Multiplatform `1.8.2`
- **Platforms:** Android (primary) — iOS migration in progress
  (see [IOS_MIGRATION_PLAN.md](IOS_MIGRATION_PLAN.md))
- **Min SDK:** 24 · **Target/Compile SDK:** 35

## Building

Requires the Android SDK and JDK 17+.

```bash
# Clone
git clone https://github.com/cafarovceyxun/AnaMuslim.git
cd AnaMuslim

# Point Gradle at your Android SDK
echo "sdk.dir=/path/to/Android/sdk" > local.properties

# Build a debug APK
./gradlew :app:assembleDebug
```

The resulting APK is written to `app/build/outputs/apk/debug/`.

> **Note:** the `local.properties` file is machine-specific and must never be
> committed.

## Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md)
before opening a pull request, and follow our
[Code of Conduct](CODE_OF_CONDUCT.md). Translators and reviewers, see
[CONTRIBUTORS.md](CONTRIBUTORS.md).

## Privacy & credits

- [PRIVACY.md](PRIVACY.md) — what the app stores and which services it contacts
- [CREDITS.md](CREDITS.md) — asset sources and their separate licenses
- [OPEN_SOURCE_CHECKLIST.md](OPEN_SOURCE_CHECKLIST.md) — release status & remaining tasks

## License

AnaMuslim is free software licensed under the **GNU General Public License
v3.0** — see [LICENSE](LICENSE). Because it is derived from GPLv3 software,
any distributed version (including binaries) must remain under GPLv3 and make
the corresponding source available.
