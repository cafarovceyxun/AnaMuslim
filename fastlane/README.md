# Fastlane metadata

Store listing text and graphics for **F-Droid** and **IzzyOnDroid**. Both repos read this
directory from the *same tag* they take the APK from — so any change here must be committed
**before** the release tag is created, or the listing will lag one version behind.

## Layout

```
fastlane/metadata/android/<locale>/
    title.txt                    app name          (<= 30 chars)
    short_description.txt        one-liner         (<= 80 chars)
    full_description.txt         listing body      (<= 4000 chars)
    changelogs/<versionCode>.txt release notes     (<= 500 chars)
    images/icon.png              512x512
    images/phoneScreenshots/     1.png, 2.png, ...
```

Locales present: `en-US`, `az`, `tr`, `ru` — matching the four in-app languages.

The changelog filename is the **versionCode**, not the versionName. For 3.1.6 that is
`114111137.txt`. Every release needs a new file named after its own versionCode.

`images/icon.png` is a copy of `app/src/main/ic_launcher-playstore.png`. If the launcher icon is
regenerated, re-copy it into all four locales.

## Screenshots

Ten shots live in `en-US/images/phoneScreenshots/` only, and every other locale falls back to
them. That is deliberate: the UI in the images is Azerbaijani regardless of listing language, so
four copies would put ~17 MB of duplicate JPEGs into a repo that was explicitly slimmed down.

Filenames are **zero-padded** (`01.jpg` … `10.jpg`) because both stores sort by name — with
`1.jpg`, `10.jpg` would sort between `1` and `2`.

| # | Screen |
|---|---|
| 01 | Home — verse of the day, reading history |
| 02 | Mushaf mode, page 597, tajweed colouring |
| 03 | Verse-by-verse — al-Fatiha with Azerbaijani translation |
| 04 | Translation mode |
| 05 | Verse-by-verse — al-'Alaq with tajweed |
| 06 | Recitation player — reciter, repeat, speed |
| 07 | Recitation player — verse highlight view |
| 08 | Hadith — Arabic |
| 09 | Hadith — Azerbaijani translation |
| 10 | Global search |

Captured from the debug build, so the *launcher* label reads "(test)" — it does not appear
anywhere in the UI, and no status bar or personal data is visible in the frames. Verified before
committing.

## Still missing

- [ ] Optional `images/featureGraphic.png` (1024x500).
- [ ] A settings / theming shot would round the set out (7 accent palettes, light+dark).

## Limits are enforced

`title.txt`, `short_description.txt` and `full_description.txt` are truncated or rejected if they
exceed the sizes above. Current sizes were checked and all four locales pass.
