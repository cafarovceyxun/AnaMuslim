# Credits & Asset Licenses

AnaMuslim is a GPLv3 fork of
[QuranApp by AlfaazPlus](https://github.com/AlfaazPlus/QuranApp). The application
**source code** is licensed under the GNU GPL v3.0 (see [LICENSE](LICENSE)).

**Bundled data assets — fonts, Qur'an scripts, translations, recitations, and
word-by-word data — are NOT covered by the app's GPLv3 license.** Each asset
carries its own license and copyright, held by its original publisher. They are
included here for convenience and remain the property of their respective
owners.

> ⚠️ **Action required before public release / redistribution:** confirm that
> the redistribution terms of every asset below permit inclusion in a public,
> GPLv3-licensed repository. Where a license does not permit bundling, the asset
> must be removed from the repo and downloaded at runtime instead. See
> [OPEN_SOURCE_CHECKLIST.md](OPEN_SOURCE_CHECKLIST.md).

## Assets in this repository

| Location | Asset | Source / Publisher | License status |
|---|---|---|---|
| `inventory/fonts/kfqpc_v1/` | KFGQPC (King Fahd Complex) Qur'an fonts | King Fahd Glorious Qur'an Printing Complex | ✅ **OK to bundle** — KFGQPC license grants free use/copy/distribution; **must be unmodified, not sold, and keep its license** (see `inventory/fonts/kfqpc_v1/LICENSE.txt`). Fonts are **NOT** GPLv3. |
| `inventory/quran_scripts/` | Qur'an Arabic text (KFQPC v1 script) | KFGQPC / Tanzil | ✅ **OK to bundle unmodified** — the underlying Qur'an text is under the [Tanzil Text License](https://tanzil.net/docs/text_license) (CC BY 3.0): verbatim copies permitted, **no modification**, must credit source. Not GPLv3. |
| `inventory/translations/` | Per-translation full text | Individual translators / publishers | ⚠️ **Mixed** — see the per-translation table below. Some are public domain; most are copyrighted and permitted for **non-commercial** redistribution only; one (Abdul Haleem / OUP) should be **removed and fetched at runtime**. |
| `inventory/recitations/` | Recitation metadata only | quranicaudio.com / qurancdn.com | ✅ Metadata only; audio streamed/served by third parties at runtime. No bulk copyrighted media bundled. |
| `inventory/wbw/` | Word-by-word **manifest only** | Qur'an.com (QUL) | ✅ `available_wbw_info*.json` are manifests; the actual WBW data is fetched at runtime (`ghraw://…/wbw_*.json.gz`). No bulk WBW text bundled. |
| `inventory/other/`, `inventory/versions/` | Misc. data manifests | upstream | ✅ Config/version manifests only — no copyrighted content. |
| `inventory/prayer/cities-v*.tsv.gz` | Extended city catalogue fetched on first launch (69 691 rows) | [GeoNames](https://www.geonames.org) `cities5000`, `alternateNamesV2` | ✅ **OK to bundle** — same **CC BY 4.0** terms as the bundled `cities.tsv` above; served from this repo over `ghraw://` so the user's mirror choice applies. Generated offline by `tools/prayer/` (see its `README.md`). |
| `shared/src/commonMain/composeResources/files/prayer/cities.tsv` | City names and coordinates for prayer times (3 500 rows) | [GeoNames](https://www.geonames.org) `cities15000`, `alternateNamesV2` | ✅ **OK to bundle** — **CC BY 4.0**, attribution required and given (in this file and in the app's location picker). One-way compatible with GPLv3; the data keeps its own licence and is **not** GPLv3. Generated offline by `tools/prayer/` (see its `README.md`). |
| `shared/src/commonMain/composeResources/files/atlas/uthmani/tajweed.bin` | Uthmani per-glyph tajweed colours | Derived from [cpfair/quran-tajweed](https://github.com/cpfair/quran-tajweed) | ✅ **OK to bundle** — the rule data is **CC BY 4.0**; attribution below. One-way compatible with GPLv3 code; the asset keeps its own licence and is **not** GPLv3. Generated offline by `tools/tajweed/` (see its `README.md`). |

### Translations — per-item status

The full text of each translation is bundled under `inventory/translations/`. Their
terms follow the [Tanzil translation terms](https://tanzil.net/trans/): copyrighted
translations may be redistributed for **non-commercial** use, with attribution and
**without modification**. Because AnaMuslim is free of charge, this is within those
terms — but the assets are **NOT** GPLv3 and may not be sold or altered. Downstream
forks that intend commercial use must obtain their own permissions.

| Translation | Copyright / status | Bundle verdict |
|---|---|---|
| Pickthall (`en_pickthall`) | Public domain (d. 1936) | ✅ Free to bundle |
| Yusuf Ali (`en_yusuf-ali`) | Public domain (d. 1953; life+70) | ✅ Free to bundle |
| Elmalılı Hamdi Yazır (`tr_elmalili`) | Public domain (d. 1942) | ✅ Free to bundle |
| Transliteration (`en_transliteration`) | No single author; generated | ✅ Free to bundle |
| Sahih International (`en_sahih-international`) | © dar Abul-Qasim | ✅ Non-commercial, unmodified, with credit |
| Hilali & Khan (`en_hilali-khan`) | © King Fahd Complex | ✅ Non-commercial, unmodified, with credit |
| The Clear Qur'an (`en_the-clear-quran`) | © Dr. Mustafa Khattab | ✅ Non-commercial + credit, no derivatives ([terms](https://blog.clearquran.com/download)) |
| Taqi Usmani (`en_taqi-usmani`) | © Mufti Taqi Usmani | ⚠️ Copyrighted; non-commercial only — keep credit, consider runtime fetch |
| Mahmoud Ghali (`en_mahmoud-ghali`) | © Dr. Mahmoud Ghali | ⚠️ Copyrighted; non-commercial only |
| T.B. Irving (`en_irving`) | © estate (d. 2002) | ⚠️ Copyrighted; non-commercial only |
| Tafhim-ul-Qur'an / Maududi (`en_maududi`) | © Islamic Foundation | ⚠️ Copyrighted; non-commercial only |
| Abu Adel (`ru_abu-adel`) | © Abu Adel; freely distributed | ⚠️ Copyrighted; non-commercial only |
| Elmir Kuliev (`ru_elmir-kuliev`) | © Elmir Kuliev; freely distributed | ⚠️ Copyrighted; non-commercial only |
| Ministry of Awqaf, Egypt (`ru_ministry-of-awqaf`) | © Egyptian Ministry of Awqaf | ⚠️ Official text; non-commercial only |
| Diyanet (`tr_diyanet`) | © Diyanet İşleri (Turkey) | ⚠️ Official text; non-commercial only |
| **Abdul Haleem (`en_abdul-haleem`)** | **© Oxford University Press** | ❌ **OUP does not grant free redistribution — remove from repo and fetch at runtime** |

**Action:** the ✅ rows are safe to keep bundled. The ⚠️ rows are permissible for this
free app but carry non-commercial restrictions — keep them, or (safer for forks) move
them to runtime download. The ❌ Abdul Haleem / OUP text should be **removed from the
repository** and downloaded on demand like the other runtime data, since OUP's terms do
not allow bundled redistribution.

### KFGQPC font license summary

The King Fahd Glorious Qur'an Printing Complex license grants, free of cost, the
right to **use, copy, and distribute** the font — provided the font is **not
sold, modified, altered, translated, reverse-engineered, or reproduced without
KFGQPC's written approval**, and its license text is retained. Because AnaMuslim
ships the fonts **unmodified** and **free of charge**, bundling them is permitted.
The fonts remain the property of KFGQPC and are **not** covered by AnaMuslim's
GPLv3 (GPL applies to code; the fonts are an aggregated work under their own
license). See [Tanzil — Quranic Fonts](https://tanzil.net/docs/quranic_fonts)
and the [KFGQPC license](https://scancode-licensedb.aboutcode.org/kfgqpc-uthmanic-script-hafs.html).

### Tajweed colour data

The bundled `atlas/uthmani/tajweed.bin` gives each glyph of the Uthmani script its tajweed
(recitation-rule) colour. It is generated offline (`tools/tajweed/`) from two sources:

- **Rule annotations:** [cpfair/quran-tajweed](https://github.com/cpfair/quran-tajweed) —
  the file `output/tajweed.hafs.uthmani-pause-sajdah.json`, © Charles Pletcher and
  contributors, licensed **[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)**. The
  shipped `tajweed.bin` is a derivative work of this data and is redistributed under CC BY
  4.0 with this attribution. CC BY 4.0 permits inclusion in a public GPLv3 repository (the
  data keeps its own licence; only the app **code** is GPLv3).
- **Uthmani reference text** (used only to align the rules to the app's script at generation
  time; **not** bundled): cpfair's pinned copy of the Tanzil Uthmani text (ca. 2017, the exact
  text its offsets index), under the [Tanzil Text License](https://tanzil.net/docs/text_license)
  (CC BY 3.0). The app's own Qur'an text is credited above under `inventory/quran_scripts/`.

The alquran.cloud `quran-tajweed` edition (Dar al-Maarifah origin, licence unclear) is used
**only** for an offline QA cross-check in the generator and is **never** bundled or
redistributed.

## Runtime data providers (not bundled)

The app fetches content at runtime from third-party services:

- `api.quran.com` — Qur'an.com API
- `api.alfaazplus.com`, `gh-proxy.alfaazplus.com` — AlfaazPlus services
- `github.com/AlfaazPlus/QuranAppInventory/releases` — on-demand font packs
- `download.quranicaudio.com`, `audio.qurancdn.com` — recitation & WBW audio
- `github.com/cafarovceyxun/AnaMuslim/releases` (tag `qpc`) — this project's own
  KFQPC page-font archives

These are external dependencies operated by third parties. Availability is not
guaranteed by this project. See the checklist for the plan to self-host.

This project also runs its own Supabase instance
(`molyqwcaynvsdmixtcbc.supabase.co`), which serves the hadith library, the
Azerbaijani Qur'an translation, the daily verse/hadith, and the verse-report
inbox. That content is maintained by this project, not by a third party.

## Ported algorithms

| Where | What | Source | License |
|---|---|---|---|
| `shared/src/commonMain/.../utils/prayer/PrayerMath.kt` | Solar position and rise/set/transit solving (Meeus, *Astronomical Algorithms* pp. 88, 93, 102, 163–165) | [adhan-js](https://github.com/batoulapps/adhan-js) — Batoul Apps | ✅ **MIT** — one-way compatible with GPLv3. The port keeps the algorithm, not the code layout; the surrounding high-latitude rules, elevation option and scheduling are this project's own. |

Verified against the source library: for the same coordinates and angles the two
agree within **two seconds** on every prayer across solstices and equinox (the
one exception is Asr, 33 s, from a documented difference in which declination
the shadow angle is built from).

## Original code attribution

- Upstream project: **QuranApp** © AlfaazPlus and contributors (GPLv3)
- Portions © Faisal Khan (https://github.com/faisalcodes) — preserved in source headers

See [NOTICE](NOTICE) for the full attribution statement.
