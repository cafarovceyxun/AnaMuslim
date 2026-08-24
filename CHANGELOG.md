![English](https://img.shields.io/badge/English-1F6FEB?style=for-the-badge&logo=googletranslate&logoColor=white)
[![Azərbaycanca](https://img.shields.io/badge/Az%C9%99rbaycanca-6E7681?style=for-the-badge)](CHANGELOG.az.md)

# Changelog

User-visible changes, release by release. The short "What's new" texts submitted
to the stores live under `fastlane/metadata/`; the complete feature reference is
[FEATURES.md](FEATURES.md).

---

## Unreleased — 2026-08-24

Changes queued for the next store release.

### Added

- **Azerbaijani translation audio.** The Qur'an translation is now voiced in full
  (all 114 chapters) and downloadable from the app. The player gained an audio
  source control (Arabic only · translation only · both), the reciter sheet a
  "Translation voice" section, and the download screen its own section. Verse
  highlighting follows the translation too; on iOS playback runs as a
  verse-by-verse clip queue.
- **Arabic interface.** A fifth app language; selecting it turns the whole layout
  right-to-left, while Azerbaijani hadith and translation text keeps its own
  script direction.
- **Book mode for hadith.** Hadith flow as continuous book text instead of cards,
  toggled from the index.
- **Volume outline sheet.** The whole tree of books, chapters and sub-chapters,
  with expand-all, collapse-all and an introduction entry.
- **Pinch to resize text.** In the reader, two fingers scale the translation and
  three fingers the Arabic text.
- **A settings sheet of the reader's own**, plus a **default reading mode** —
  which layout the reader opens in from the index, bookmarks and history ("last
  used" included).
- **App text size.** One slider scales the interface text; the Qur'an and hadith
  text stay on their own settings.
- **Swipe between tabs** in the bottom navigation, with transition animations.
- **A setting to show or hide the verse-of-the-day card** on the home screen.

### Changed

- The **verse/hadith of the day card** was rebuilt: translation first, Arabic
  below, and it now honours the hadith reader's font and size settings.
- The **settings screen** was regrouped into "Reading", "Both readers" and "All
  settings".
- **Hadith editor** (maintainers): the `1§ 2§ 3§ 4§` clipboard format fills
  several hadith at once, which can be added or removed by hand and saved as one
  batch.

### Fixed

- **Arabic search**: text copied straight out of the mushaf — waqf signs and
  small letters included — returns results again (it used to come back empty).
- **Page-turn keys**: the keyboard's PAGE UP / PAGE DOWN and the S Pen button now
  turn pages reliably in the reader.
- **The verse-of-the-day card no longer disappears offline** — the last fetched
  content is shown, and stale content from a previous day is not.
- Azerbaijani text is no longer mirrored in the Arabic interface (numbers and
  punctuation swapping ends).

---

## Earlier releases

Notes for releases predating this file live on the store listings:
[Google Play](https://play.google.com/store/apps/details?id=com.cafarovceyxun.anamuslim)
and the [App Store](https://apps.apple.com/az/app/id6799231138) (first iOS
release: 2026-08-15). The "What's new" texts submitted to Play are kept under
`fastlane/metadata/android/<language>/changelogs/`.
