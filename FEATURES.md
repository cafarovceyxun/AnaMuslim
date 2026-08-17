![English](https://img.shields.io/badge/English-1F6FEB?style=for-the-badge&logo=googletranslate&logoColor=white)
[![Azərbaycanca](https://img.shields.io/badge/Az%C9%99rbaycanca-6E7681?style=for-the-badge)](FEATURES.az.md)

# AnaMuslim — Features

The complete feature list of the app, area by area. The
[README](README.md#-features-at-a-glance) carries a short summary; this document
is the full reference.

**Platform legend** — both platforms ship: Android on Google Play and iOS on the
App Store (released 2026-08-15; remaining gaps are tracked in
[IOS_MIGRATION_PLAN.md](IOS_MIGRATION_PLAN.md)). Unless a line is marked
otherwise, the feature is shared code and runs on both.

| Mark | Meaning |
| ---- | ------- |
| 🤖   | Android only (platform integration, no iOS counterpart yet) |
| 🌐   | Needs a network connection the first time (content download) |

---

## 1. Qur'an reader

### Reading modes

Four layouts, switchable from the reader's app bar without losing your place:

- **Verse by verse** — one verse per card: Arabic, translation, footnotes and a
  per-verse action row
- **Reading (mushaf)** — page-by-page continuous Arabic, laid out like a printed
  mushaf, with the page's chapter headers and Bismillah
- **Translation** — translation-focused paging
- **Translation (vertical)** — a single continuous scroll through the
  translation, for long uninterrupted reading

### Navigation

- Open the reader by **chapter (surah), juz, hizb or page**
- Reader index screen listing all 114 chapters, 30 juz, hizb quarters and 604
  mushaf pages, each with a text filter field
- In-reader navigator sheet: jump to any chapter → verse without leaving the page
- Footer navigator for the previous / next unit, and a pull-down gesture that
  reveals navigation from the top of the page
- Deep-link straight to a verse from other screens (search results, bookmarks,
  history, verse of the day, widgets)
- **Auto-scroll** with an adjustable speed slider and a gesture overlay for
  pausing and resuming
- Page turning with the **volume keys**, and support for external page-turner /
  S Pen keys 🤖
- Landscape and tablet layouts; on expanded windows the reader uses a wider
  two-pane-friendly arrangement

### Verse actions

Long-press or tap a verse marker for:

- Copy, share (text or image)
- Add or edit a **bookmark note**
- **Similar verses** — find other verses with matching or near-matching text
- **Verse information** — the verse's page, juz, hizb and revelation data
- Set the verse as the **Verse of the Day**
- **Report a mistake** in the Arabic text or the translation

### Chapter info

A dedicated page for each surah — revelation place, verse count, the meaning of
the name and background — rendered from bundled HTML, available in all four app
languages.

---

## 2. Arabic text and scripts

Five Qur'an scripts, selectable in **Settings → Scripts** with a live preview
tile for each:

| Script | Notes |
| ------ | ----- |
| **Uthmani (Hafs)** | Bundled — works offline from first launch |
| **King Fahd Complex V1** | Page-accurate KFQPC font, downloaded on demand 🌐 |
| **King Fahd Complex V2** | Page-accurate KFQPC font, downloaded on demand 🌐 |
| **King Fahd Complex V4 (Tajweed)** | Tajweed-coloured KFQPC pages 🌐 |
| **IndoPak** | 15-line and 16-line variants |

Additional text controls:

- **Tajweed colouring on the Uthmani script** — pronunciation rules coloured from
  a bundled colour atlas, toggleable, with palettes that adapt to light and dark
  themes
- **Arabic text can be switched off** entirely for translation-only reading
- **Independent text sizes** for Arabic and translation, each with its own
  multiplier
- KFQPC page fonts are cached per page and installed from a compressed archive,
  so only the pages you read are kept warm

---

## 3. Translation and word-by-word

- **Azerbaijani Qur'an translation (Mürşüd Yusifoğlu)** with per-verse notes,
  served from the project's own backend and stored for offline reading 🌐
- Additional translation sets in English, Russian and Turkish from the shared
  inventory 🌐
- **Translator parentheses** — hide them, or highlight them in a distinct colour,
  so added words are visually separable from the translated text
- **Word-by-word** mode: per-word meaning shown under the Arabic, with a
  tap-through detail sheet for each word. Word packs in **English** (translation
  + transliteration), **Russian** and **Turkish** (translation) 🌐
- **Per-word audio** — the Arabic recitation of each individual word,
  downloadable chapter by chapter or in one bulk download 🌐
- Footnotes rendered inline with a tap-to-expand presenter
- Translation downloads are managed per translation, with size and update state
  shown before you commit to the download

---

## 4. Hadith

- Hadith library organised as **volumes → books → chapters (bab) →
  sub-chapters**, browsable down to the individual hadith
- Every entry carries **Arabic text, Azerbaijani translation, source reference
  and notes**
- Download the whole library for offline reading 🌐, with its own storage entry
  and cleanup
- **Display settings sheet** dedicated to the hadith reader:
  - Arabic font: **Noto Naskh**, **Uthman Taha Naskh** or **Ayat** (default:
    Uthman Taha)
  - Independent Arabic and translation text sizes
  - Toggles for Arabic text, translation and source reference
- **Navigator sheet** for moving between books and chapters without going back
  to the index
- **Bookmarks** and a **separate read history** for hadith
- **Search** across the hadith collection, both from the global search screen and
  in-collection
- **Qur'an reference picker** — attach or open the Qur'an verse a hadith cites
- **Share** a hadith as text or as a rendered image through the image editor
- **In-app editor** for correcting a hadith's Arabic, translation, source or
  notes — submissions go to moderation (see §10)

---

## 5. Audio and recitation

- **Verse-by-verse recitation** from **16 reciters** — among them Yasser
  ad-Dussary, Mishari Rashid al-Afasy, Saad Al-Ghamdi, Al-Husary, Al-Minshawi,
  Saud Al-Shuraim, Abu Bakr al-Shatri and Al-Ajmi 🌐
- **Recitation with translation audio** from a separate set of reciters
- **Verse tracking** — the reader highlights and follows the verse being recited
- **Mini player and expanded player**: reciter artwork, seek bar, chapter and
  verse position, and a spotlight animation on the active verse
- **Playback speed** control
- **Repeat options** — repeat a single verse or a verse range, with a repeat
  count
- **End-of-audio behaviour** — stop, continue to the next chapter, or repeat the
  chapter
- **Audio source options** — Arabic only, translation only, or both
- **Background playback** with a media notification, lock-screen controls and
  headset / Bluetooth buttons 🤖
- **Android Auto** media browsing 🤖
- **Download recitations** per reciter (whole Qur'an or selected chapters) for
  offline listening 🌐, with per-reciter deletion

---

## 6. Search

- **Full-text search** across Arabic verse text, translations, surah names and
  the hadith collection
- **Global search** entry point in the bottom navigation, plus in-context search
  inside the hadith reader
- **Filters** — restrict a search to the Qur'an or hadith, and select which
  translations to search within
- **Quick links** — one-tap common searches and jumps
- **Search history**, with individual and bulk clearing
- **Voice search** through the system speech recognizer 🤖
- Arabic search tips shown inline (diacritic-insensitive matching guidance)

---

## 7. Library and personal data

- **Bookmarks** for both verses and hadith, each with an optional free-text
  **note**; a bookmark viewer sheet opens straight from the reader
- **Read history**, tracked separately for the Qur'an and for hadith, with the
  exact verse / hadith you left off at
- **Verse of the Day** on the home screen, refreshed daily from the backend, with
  its own "read now" action
- **Daily reminder notification** at a time you pick 🤖
- **Home screen widgets** 🤖:
  - *Verse of the Day* widget
  - *Recitation player* widget with transport controls
- **Home screen sections**: continue reading, featured reading entries,
  bookmarks, Qur'an read history and hadith read history
- **Greeting splash** on launch

---

## 8. Sharing

- **Share a verse or hadith as text**, with formatting tuned for messengers
  (WhatsApp-friendly line breaks and reference block)
- **Share as an image** through the built-in image editor:
  - Toggle Arabic and Azerbaijani text independently
  - Adjust text sizes and horizontal / vertical edge padding
  - Live preview before sharing
- Separate image pipelines for Qur'an verses and hadith entries, each rendering
  the card at export resolution 🤖

---

## 9. Appearance and localisation

- **Theme mode**: light, dark or follow-system
- **Seven accent palettes**: Default, Blue, Purple, Violet, Red, Yellow and
  Mono
- **Material You dynamic colour** on Android 12+ 🤖
- **App languages**: Azerbaijani, English, Russian, Turkish, plus system default
  — applied without restarting the app
- **Numeral system**: Latin (1, 2, 3) or Arabic-Indic (١, ٢, ٣) digits
- Every screen honours landscape and tablet widths; app bars collapse to 48dp in
  landscape but stay visible

---

## 10. Content quality — reports and community edits

This is what AnaMuslim adds on top of a normal Qur'an reader: the text can be
fixed by its readers, in place, without shipping an app update.

- **Report a mistake** in a verse's Arabic or its translation from the reader,
  with a free-text message; the verse reference is attached automatically
- **In-app editors** for hadith text and for Qur'an translations
- **Moderation pipeline** — an edit from a non-maintainer is routed to a pending
  queue instead of the live table; nothing goes public unreviewed
- **Edits management** panel (maintainers): pending / approved / rejected filters,
  side-by-side original vs. proposed text, approve or reject per entry
- **Reports management** panel (maintainers): triage incoming verse reports
- Approved corrections reach every user on their next content sync — no store
  release needed

---

## 11. Downloads and storage

- **Per-item downloads** for translations, recitations, word-by-word packs, the
  hadith library and KFQPC page fonts — nothing large is fetched without you
  asking
- **Resource download source** setting — choose which mirror content is fetched
  from
- **Storage cleanup** screen with per-category panes:
  - Downloaded translations
  - Downloaded recitations (per reciter)
  - Downloaded scripts / page fonts
  - Hadith library cache
  Each shows its on-disk size and can be deleted individually
- **Export and import**: settings only, bookmarks only, or everything — a single
  portable file covering language, theme, download source, reader mode,
  auto-scroll speed, Arabic-text toggle, text sizes, script and variant, current
  translation, reciter, playback speed, audio option and end-of-audio behaviour

---

## 12. Privacy and offline behaviour

- **No ads and no analytics SDKs**
- **No account required** for any reading feature — sign-in exists only for
  maintainers moderating content
- **Everything works offline** once the content is downloaded: reading,
  translation, hadith, bookmarks, history, search and downloaded audio
- Network access is limited to content downloads, the daily verse, report /
  edit submission and the update check
- Details: [PRIVACY.md](PRIVACY.md)

---

## 13. Platform integrations 🤖

- **App shortcuts and deep links** — `OPEN_READER`, `OPEN_REFERENCE`,
  `OPEN_CHAPTER_INFO` intents plus `https` deep links into the reader
- **Popup Qur'an window** — a floating verse reference triggered by
  `SHOW_POPUP`, so other apps can surface a verse without leaving their context
- **System search integration** (searchable configuration) and voice search
- **Media session** with notification, lock screen and Android Auto
- **Home screen widgets** (Verse of the Day, recitation player)
- **Volume-key and page-key navigation** in the reader
- **Crash receiver + in-app log viewer** for diagnosing issues without a
  desktop toolchain

---

## 14. Onboarding and updates

- **First-run onboarding**: app language → theme → translation and resource
  selection, so a fresh install is usable before it leaves the wizard
- **In-app update banner** on the home screen with the version number, a
  "what's new" list and a store link
- **Required-update mode** — maintainers can mark a minimum version, below which
  the banner becomes blocking
- **Release announcement panel** (maintainers): publish version code, version
  name, minimum version, store link and per-language release notes (Azerbaijani,
  English, Turkish, Russian) for Android and iOS separately

---

## 15. Maintainer tools

Visible only to a signed-in maintainer account:

- Edits management (Qur'an and hadith queues)
- Reports management
- App release announcement
- **App logs** — the local log viewer
- **Remote logs** — server-side log inspection
- Resource administration (content and version manifests)

---

## Platform support at a glance

| Area | Android | iOS |
| ---- | ------- | --- |
| Qur'an reader, all four modes | ✅ | 🚧 shared code, migration in progress |
| Scripts, tajweed, word-by-word | ✅ | 🚧 |
| Hadith library | ✅ | 🚧 |
| Recitation player | ✅ | 🚧 |
| Search | ✅ | 🚧 |
| Bookmarks, history, export/import | ✅ | 🚧 |
| Background audio, media notification | ✅ | ⛔ not yet |
| Android Auto | ✅ | — |
| Home screen widgets | ✅ | ⛔ not yet |
| Voice search | ✅ | ⛔ not yet |
| Image share editor | ✅ | 🚧 |
| Reports and moderation | ✅ | ✅ |

The shared Compose Multiplatform code already covers the reader, player, hadith,
settings and onboarding clusters; what remains is platform glue. Current status
lives in [IOS_MIGRATION_PLAN.md](IOS_MIGRATION_PLAN.md).

---

## Not in the app

Stated so nobody has to go looking:

- No prayer times, qibla compass or Islamic calendar — AnaMuslim is a Qur'an and
  hadith reader, not an all-in-one companion app
- No ads, no in-app purchases, no tracking
- No social feed, comments or user profiles

---

## License

AnaMuslim is free software under the **GNU General Public License v3.0** — see
[LICENSE](LICENSE). It is a fork of
[QuranApp by AlfaazPlus](https://github.com/AlfaazPlus/QuranApp); attribution and
a summary of changes are in [NOTICE](NOTICE), and asset sources with their own
licenses are listed in [CREDITS.md](CREDITS.md).
