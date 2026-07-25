# Tajweed colour pipeline (Uthmani atlas)

Offline generator that produces per-glyph tajweed (recitation-rule) colours for the app's
Uthmani word-glyph atlas and writes them to:

```
shared/src/commonMain/composeResources/files/atlas/uthmani/tajweed.bin
```

It is a **build-time tool** (Phase 1 of `TAJWEED_PLAN.md`). The app ships the resulting
`tajweed.bin` and never runs this code. The reader pairs each colour class positionally
with the glyph placements it already loads from `6x.zip`.

## How it works

1. Loads cpfair's per-ayah rule annotations **and the exact Tanzil text those offsets index**
   — cpfair's pinned `quran-uthmani.txt` (ca. 2017), *not* a current Tanzil edition: newer
   encodings differ in the pause-mark/tanwin layer and would drift the offsets mid-ayah. It
   asserts the offsets land on the expected letters.
2. Reconstructs each ayah's text from `app/src/main/assets/db/quranapp.db`
   (`ayah_words`, `script_id = 1`), dropping the trailing ayah-number token.
3. Normalises the app's KFGQPC text and Tanzil's text and aligns them on the
   consonant skeleton (`difflib`) — the app encoding differs from Tanzil only in the
   diacritic layer (sukun variant, alef-maksura vs yeh, tanwin forms, …) so the letters
   align 1:1 (99.98% of ayahs within edit-distance ≤ 2).
4. Projects each cpfair rule onto app characters at **exact-char** granularity: letters align
   1:1 and each letter's marks map positionally, so a rule that targets a superscript-alef
   madd colours only that mark's glyph, not the base consonant.
5. Shapes every unique word with **uharfbuzz** (`features=None`, direction RTL, script
   `arab`, language `ar`, `cluster_level = 1`) and **hard-asserts** the glyph-id sequence
   equals `layout.json`. Each glyph inherits the highest-priority rule over its char span
   (needed because the atlas ligates multi-letter runs into single glyphs).
6. Encodes a per-unique-word **base** array plus per-occurrence **overrides** and gzips it.
   See `FORMAT.md` for the exact byte layout.
7. Renders QA artefacts (`qa/index.html`) and cross-checks a random sample against an
   independent tajweed markup.

## Run

```bash
python3 -m venv venv && source venv/bin/activate
pip install -r requirements.txt
python3 generate.py
```

`generate.py` is the single entrypoint (`qa_build.py` is imported by it). Source data is
downloaded once into `cache/` (git-ignored) and reused on subsequent runs; delete `cache/`
to force a refresh. The run prints coverage/alignment stats and writes `qa/stats.json`.

Requires (read-only) the repo font
`shared/src/commonMain/composeResources/font/uthmanic_hafs.ttf`, the atlas
`…/atlas/uthmani/6x.zip`, and `app/src/main/assets/db/quranapp.db`.

## Output

- `…/atlas/uthmani/tajweed.bin` — gzip, ~49 KB. 11-class granular palette (madd variants,
  ghunnah, qalqalah, ikhfa, idghaam, iqlab each distinct). Format: `FORMAT.md`.
- `qa/index.html` — the gold ayahs (Al-Fātiḥa, Al-Baqara 2:9/2:10/2:255, muqaṭṭaʿāt,
  Al-Ikhlāṣ) rendered from the real atlas bitmaps with per-glyph colours, for human review.
- `debug_ayah.py` — `python3 debug_ayah.py 2:9 2:10` traces the full chain (cpfair
  annotations → alignment → per-glyph classes) for any ayah.
- `qa/stats.json` — coverage and size stats.

## Data sources & licensing

| Data | Source | Licence | Shipped? |
|---|---|---|---|
| Tajweed rule annotations | [cpfair/quran-tajweed](https://github.com/cpfair/quran-tajweed) — `output/tajweed.hafs.uthmani-pause-sajdah.json` | **CC BY 4.0** | derived into `tajweed.bin` |
| Uthmani reference text (offset coordinate system) | cpfair's pinned [`quran-uthmani.txt`](https://github.com/cpfair/quran-tajweed/files/7281388/quran-uthmani.txt) (Tanzil, ca. 2017) | [Tanzil Text Licence](https://tanzil.net/docs/text_license) (CC BY 3.0) | **no** — alignment reference only |
| Independent tajweed markup (cross-check) | alquran.cloud edition `quran-tajweed` | licence unclear (Dar al-Maarifah origin) | **no** — offline QA comparison only |

The shipped `tajweed.bin` is a derivative of the cpfair annotations (CC BY 4.0); attribution
is recorded in the repo root `CREDITS.md`. The Tanzil text and the alquran.cloud tajweed
markup are used **only** at generation time and are **not** redistributed. See
`OPEN_SOURCE_CHECKLIST.md`.

CC BY 4.0 is one-way compatible with the app's GPLv3 code licence; the data asset keeps its
own licence and attribution (it is not itself GPLv3), consistent with the other bundled
assets described in `CREDITS.md`.
