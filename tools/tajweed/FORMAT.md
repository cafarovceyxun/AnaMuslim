# `tajweed.bin` — binary format (schema version 7)

> The `version` byte in the header is the source of truth; the app importer re-imports when
> it changes. **v3** introduced the granular Turkish-style palette (11 classes) and, because
> classes no longer fit 3 bits, a new **override diff encoding** (see the Override section).
> **v4 and v5 are byte-for-byte the same format as v3** — only the data changed: v4 fixed the
> ṣila-madd projection, and **v5 suppresses colouring on multi-letter ligature glyphs** (a rule
> that only touches an internal mark, e.g. the madd on the superscript-alef inside الرحمٰن's
> حمٰن ligature, no longer floods the whole glyph). Each version bump exists solely to force the
> app importer to re-import. The header, palette, rule-name table and nibble-packed base section
> are unchanged from v1/v2. **A v1/v2 importer will misparse v3+ overrides** — read the `version`
> byte and use this spec for `version >= 3`.
>
> The embedded palette is kept accurate but is **no longer the render authority**: the app reads
> its colours from `TajweedPalette.kt` (Kotlin), so a colour tweak needs no data regen.

Per-glyph tajweed colour classes for the Uthmani word-glyph atlas. The Kotlin importer is
written from **this document alone**; the generator (`generate.py`) is the reference
encoder and `FORMAT.md` is the contract.

- **File** = a single **gzip** stream. Decompress it, then parse the **body** below.
- All integers are **little-endian**, **unsigned**.
- `u8`, `u16`, `u32` = fixed-width integers. `uvarint` = unsigned LEB128 (7 bits/byte,
  low bits first, high bit `0x80` = "more bytes follow").
- A **class** is one byte-value `0..num_classes-1` (v3: `0..10`). Class `0` means *no tajweed
  colour* — the reader draws the glyph in its normal text colour. Classes `1..N-1` index the
  palette.

The atlas layout (`6x.zip` → `layout.json`) is deduplicated by word text (21,497 unique
word documents). Tajweed at a word boundary depends on the neighbouring word, so the same
word can be coloured differently in different ayahs. The file therefore stores a
**context-free base** per unique word plus **per-occurrence overrides** keyed by
`(ayah_id, word_index)`.

---

## Body layout

```
┌── Header ───────────────────────────────────────────────────────────────┐
│ magic        4 bytes   ASCII "TJWD" (0x54 0x4A 0x57 0x44)                 │
│ version      u8        = 7  (current; v3..v7 share this exact layout)     │
│ flags        u8        = 0 (reserved)                                     │
│ num_classes  u8        = 11  (classes 0..10)                              │
│ reserved     u8        = 0                                                │
├── Palette ──────────────────────────────────────────────────────────────┤
│ colours      num_classes × u32   ARGB 0xAARRGGBB, one per class id 0..N-1 │
│                                  class 0 = 0x00000000 (sentinel: unused)  │
├── Rule-name table (self-describing; may be skipped via its length) ──────┤
│ rn_len       u16       byte length of the block that follows             │
│ rn_block     rn_len bytes:                                               │
│              for class 0..num_classes-1, in order:                        │
│                 name_count  u8                                            │
│                 name_count × ( len u8, utf8[len] )   cpfair rule names    │
├── Base section ─────────────────────────────────────────────────────────┤
│ base_count   u32       = number of atlas layout documents (21,497)        │
│ records      base_count records, in layout.json DOCUMENT ORDER:           │
│                 glyph_count  u8                                           │
│                 packed       ceil(glyph_count/2) bytes  (nibble-packed)   │
│              Each class is a 4-bit nibble. Byte k holds glyph 2k in its    │
│              low nibble and glyph 2k+1 in its high nibble. A trailing odd  │
│              glyph leaves the high nibble 0.                               │
├── Override section (v3 diff encoding) ──────────────────────────────────┤
│ ayah_count   u32       number of ayahs that carry at least one override    │
│ groups       ayah_count groups, sorted by ayah_id ascending:              │
│                 ayah_id_delta  uvarint  (ayah_id minus previous group's    │
│                                          ayah_id; first delta = absolute)  │
│                 word_count     u8                                         │
│                 word_count entries, sorted by word_index:                  │
│                    word_index  u8                                         │
│                    diff_count  u8                                         │
│                    diff_count diffs, each:                                 │
│                       glyph_index  uvarint   (placement index, 0-based)    │
│                       class        u8        (0..num_classes-1)            │
└─────────────────────────────────────────────────────────────────────────┘
```

**Why a uvarint glyph index (v3).** The v1/v2 packing `(glyph_index<<3)|class` needed
`class < 8` and `glyph_index < 32`. v3 has 11 classes, so class no longer fits 3 bits. The
corpus-wide **maximum glyph index carrying an override diff is 18** (≥ 16), so a symmetric
`(glyph_index<<4)|class` nibble byte would also overflow. The diff is therefore two fields:
a `uvarint` glyph index (always a single byte here — no word has ≥ 128 glyphs) followed by a
one-byte class. Base arrays are unaffected (11 classes < 16, so they stay nibble-packed).

There is no trailing data; the override section ends the body exactly.

### Keys and identifiers

- **Base record index → word.** Base records are in the **same order** as the `documents`
  object in `layout.json` (its keys are `"0".."N-1"` in order, which is also the order the
  app's `AtlasLayoutParser` streams them). Record *i* is the tajweed base for the *i*-th
  layout document, i.e. for that document's `text`. Because every layout word text is
  **unique**, the importer may key the base by word text: while streaming `layout.json` for
  `atlas_word_shapes`, take the *i*-th document's `text` and pair it with base record *i*.
- **`ayah_id`** = `surah_no * 1000 + ayah_no` (e.g. Al-Fātiḥa 1:1 → `1001`,
  Al-Baqara 2:255 → `2255`, An-Nās 114:6 → `114006`). This matches `ayahs.ayah_id` in
  `app/src/main/assets/db/quranapp.db`.
- **`word_index`** = the 0-based `ayah_words.word_index` (script_id=1). The trailing
  ayah-number token that `ayah_words` stores as the last "word" of each ayah is **not**
  present in this file (it carries no tajweed); simply leave it in its default colour.

### Reconstructing a word's per-glyph classes

For a rendered word at `(ayah_id, word_index)` with atlas text `T` and `G` glyph placements:

1. `classes = unpack(base_record_for(T))` → a `G`-length array of class bytes
   (nibble-unpack; low nibble first).
2. If the override section has a group for `ayah_id` containing `word_index`, apply each
   diff `(glyph_index, class)`: `classes[glyph_index] = class`.
3. Pair `classes[i]` with the atlas glyph placement `i` (they are positionally aligned and
   the generator hard-asserts the glyph sequences match). If `classes[i] == 0`, draw glyph
   `i` in the default text colour; otherwise tint it with `palette[classes[i]]`.

Overrides store only the glyphs that differ from the base, so applying them requires the
word's base (looked up by `T`). The structure itself is fully parseable without the atlas
or DB; only the *semantic* base-of-a-word lookup needs `T`, which the render path already
has.

> **Note on ligatures.** The Uthmani atlas ligates multi-letter runs into single wide
> glyphs (e.g. the tail of الرَّحْمَٰن is one glyph spanning ح…ن). A glyph inherits the
> highest-priority rule that lands on **any** letter in its span, so such a glyph may be a
> single colour covering several letters — this is inherent to a word-ligature atlas.

---

## Palette (schema version 3 — granular Turkish-style convention)

| class | ARGB         | colour | meaning (cpfair rules) |
|------:|--------------|--------|------------------------|
| 0 | `0x00000000` | (text) | default text colour: lam_shamsiyyah |
| 1 | `0xFF999999` | gray | silent, hamzat_wasl, idghaam_no_ghunnah |
| 2 | `0xFFFFC1E0` | pink | madd_2 |
| 3 | `0xFFFF8E3B` | orange | madd_munfasil, madd_246 (separated madd) |
| 4 | `0xFFFF5E8E` | rose | madd_muttasil (connected madd) |
| 5 | `0xFFE30000` | red | madd_6 (necessary madd) |
| 6 | `0xFFB5651D` | brown | ghunnah |
| 7 | `0xFF26B55D` | green | qalqalah |
| 8 | `0xFFC62828` | dark red | ikhfa, ikhfa_shafawi |
| 9 | `0xFF9C27B0` | purple | idghaam_ghunnah, idghaam_shafawi, idghaam_mutajanisayn, idghaam_mutaqaribayn |
| 10 | `0xFF1976D2` | blue | iqlab |

The palette and rule-name table are embedded in every file; a reader should use the
embedded palette rather than hard-coding these values, so a future re-grouping only touches
the generator. When two rules cover one glyph the priority is
`5 > 4 > 3 > 2 > 7 > 10 > 9 > 8 > 6 > 1`.

---

## Worked example (bytes from the shipped file)

**Header + palette** (first 52 bytes, hex — actual shipped bytes):

```
54 4a 57 44 04 00 0b 00   TJWD, v4, flags0, 11 classes, reserved0
00 00 00 00               class0  = 0x00000000
99 99 99 ff               class1  = 0xFF999999 (gray)      LE byte order
e0 c1 ff ff               class2  = 0xFFFFC1E0 (pink)
3b 8e ff ff               class3  = 0xFFFF8E3B (orange)
8e 5e ff ff               class4  = 0xFFFF5E8E (rose)
00 00 e3 ff               class5  = 0xFFE30000 (red)
1d 65 b5 ff               class6  = 0xFFB5651D (brown)
5d b5 26 ff               class7  = 0xFF26B55D (green)
28 28 c6 ff               class8  = 0xFFC62828 (dark red)
b0 27 9c ff               class9  = 0xFF9C27B0 (purple)
d2 76 19 ff               class10 = 0xFF1976D2 (blue)
```

Then `rn_len = 0x00E8 = 232`, followed by the 232-byte rule-name block, then
`base_count = 0x000053F9 = 21497`.

**First base record** = layout document 0 (`ءَأَتَّخِذُ`, 11 glyphs, all class 0):

```
0b              glyph_count = 11
00 00 00 00 00 00   6 nibble bytes  (11 glyphs -> ceil(11/2)=6; all nibbles 0)
```

**Base record for `ٱلرَّحۡمَٰنِ`** (6 glyphs; classes `[2,0,0,0,0,1]` in glyph order):

```
06              glyph_count = 6
02 00 10        nibble bytes:
                  byte0 = (glyph1<<4)|glyph0 = (0<<4)|2 = 0x02
                  byte1 = (glyph3<<4)|glyph2 = (0<<4)|0 = 0x00
                  byte2 = (glyph5<<4)|glyph4 = (1<<4)|0 = 0x10
```

Unpacking `02 00 10` low-nibble-first gives `[2,0,0,0,0,1]`: glyph 0 (the wide حۡمَٰنِ
ligature) is class 2 (`madd_2`, from the superscript-alef madd inside it) and glyph 5 (the
ٱ) is class 1 (`hamzat_wasl`).

**An override group** for ayah `ayah_id = 2004` where, say, word 3's glyph 17 becomes class 10
(`iqlab`) and glyph 18 becomes class 9 (`idghaam`):

```
<uvarint ayah_id_delta>   e.g. if the previous group was ayah 2002 -> delta 2 -> 0x02
01                        word_count = 1
03                        word_index = 3
02                        diff_count = 2
11 0a                     diff 0: glyph_index = uvarint 0x11 = 17, class = 0x0a = 10
12 09                     diff 1: glyph_index = uvarint 0x12 = 18, class = 0x09 = 9
```
