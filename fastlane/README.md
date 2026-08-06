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

## Still missing

- [ ] **Screenshots.** `images/phoneScreenshots/` is empty in every locale. Both stores want at
      least two; the listing looks broken without them. Name them `1.png`, `2.png`, … — they are
      shown in filename order. Suggested set: reader (mushaf), reader (translation), hadith,
      recitation player, search, settings/theming.
- [ ] Optional `images/featureGraphic.png` (1024x500).

Screenshots are per-locale. If you only produce one set, put it in `az/` and copy it to the
others — a missing locale falls back, but an empty directory does not.

## Limits are enforced

`title.txt`, `short_description.txt` and `full_description.txt` are truncated or rejected if they
exceed the sizes above. Current sizes were checked and all four locales pass.
