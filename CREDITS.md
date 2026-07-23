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
| `inventory/quran_scripts/` | Qur'an text scripts | Tanzil / QuranComplex / upstream | **Verify** per script (Tanzil texts are generally free for non-commercial redistribution unmodified) |
| `inventory/translations/` | Translations (incl. Azerbaijani) | Individual translators — see [CONTRIBUTORS.md](CONTRIBUTORS.md) | **Verify** per translation |
| `inventory/recitations/` | Recitation metadata | quranicaudio.com / qurancdn.com | Metadata only; audio streamed/served by third parties |
| `inventory/wbw/` | Word-by-word data | Qur'an.com (QUL) | **Verify** |
| `inventory/other/`, `inventory/versions/` | Misc. data manifests | upstream | Inherited from upstream |

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

## Runtime data providers (not bundled)

The app fetches content at runtime from third-party services:

- `api.quran.com` — Qur'an.com API
- `api.alfaazplus.com`, `gh-proxy.alfaazplus.com` — AlfaazPlus services
- `github.com/AlfaazPlus/QuranAppInventory/releases` — on-demand font packs
- `download.quranicaudio.com`, `audio.qurancdn.com` — recitation & WBW audio

These are external dependencies operated by third parties. Availability is not
guaranteed by this project. See the checklist for the plan to self-host.

## Original code attribution

- Upstream project: **QuranApp** © AlfaazPlus and contributors (GPLv3)
- Portions © Faisal Khan (https://github.com/faisalcodes) — preserved in source headers

See [NOTICE](NOTICE) for the full attribution statement.
