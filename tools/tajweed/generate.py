#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
generate.py — offline tajweed-colour data pipeline for the AnaMuslim Uthmani atlas.

Single entrypoint. Produces:

    shared/src/commonMain/composeResources/files/atlas/uthmani/tajweed.bin   (gzip)

and QA artefacts under tools/tajweed/qa/.

The output format is documented byte-for-byte in FORMAT.md; the Kotlin importer is
written from that file alone. See README.md for how to run and data-source licensing.

Pipeline (mirrors TAJWEED_PLAN.md "Faza 1", steps 1-7):

  1. Download/cache cpfair rule annotations + Tanzil Uthmani reference text; assert the
     cpfair character offsets land on the expected letters in the Tanzil text.
  2. Reconstruct each app ayah from quranapp.db `ayah_words` (script_id=1), in word order,
     dropping the trailing ayah-number token; remember per-word char spans.
  3. Normalise both texts and align them on the consonant+mark skeleton
     (difflib.SequenceMatcher) -> a Tanzil-offset -> app-offset map. Strip the basmala
     prefix that Tanzil carries on surah-opening ayahs.
  4. Project each cpfair rule range onto app char offsets -> per-(word, char) colour class.
  5. Shape every unique word once with uharfbuzz (features=None, RTL/arab/ar,
     cluster_level=1) and HARD-ASSERT the gid sequence equals layout.json. Map each
     glyph -> its source char -> rule -> colour class.
  6. Encode: per-unique-word base class arrays + per-occurrence boundary overrides +
     a rule-class/palette header. Gzip.
  7. QA: render the gold ayahs from the real atlas bitmaps with per-glyph colours (HTML),
     and cross-check ~100 random ayahs against alquran.cloud's independent tajweed markup.
"""

import gzip
import io
import json
import os
import random
import sqlite3
import struct
import sys
import unicodedata
import urllib.request
import zipfile
from collections import Counter, defaultdict

import uharfbuzz as hb

# --------------------------------------------------------------------------------------
# Paths
# --------------------------------------------------------------------------------------
HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", ".."))
CACHE = os.path.join(HERE, "cache")
QA_DIR = os.path.join(HERE, "qa")

FONT_PATH = os.path.join(REPO, "shared/src/commonMain/composeResources/font/uthmanic_hafs.ttf")
ATLAS_ZIP = os.path.join(REPO, "shared/src/commonMain/composeResources/files/atlas/uthmani/6x.zip")
DB_PATH = os.path.join(REPO, "app/src/main/assets/db/quranapp.db")
OUT_BIN = os.path.join(REPO, "shared/src/commonMain/composeResources/files/atlas/uthmani/tajweed.bin")

CPFAIR_URL = ("https://raw.githubusercontent.com/cpfair/quran-tajweed/master/"
              "output/tajweed.hafs.uthmani-pause-sajdah.json")
CPFAIR_CACHE = os.path.join(CACHE, "tajweed.hafs.uthmani-pause-sajdah.json")
# cpfair's annotation offsets index THIS EXACT Tanzil Uthmani text (downloaded ca. 2017-04),
# pinned in the cpfair README. The current tanzil.net / alquran.cloud encodings differ in the
# pause-mark and tanwin layer, which drifts later offsets within an ayah — so this pinned copy
# is the ONLY correct offset reference. Format: one line per ayah, "surah|ayah|text".
CPFAIR_TEXT_URL = "https://github.com/cpfair/quran-tajweed/files/7281388/quran-uthmani.txt"
CPFAIR_TEXT_CACHE = os.path.join(CACHE, "cpfair-quran-uthmani.txt")
# alquran.cloud's `quran-uthmani` edition (Tanzil Text License, CC BY 3.0). Used ONLY for the
# offline agreement cross-check in step 7; NOT the offset reference and NOT shipped.
TANZIL_URL = "https://api.alquran.cloud/v1/quran/quran-uthmani"
TANZIL_CACHE = os.path.join(CACHE, "tanzil-uthmani.json")
# alquran.cloud's independent `quran-tajweed` edition — used ONLY for an offline
# agreement cross-check in step 7; never shipped, never parsed into the output.
ALQ_TAJWEED_URL = "https://api.alquran.cloud/v1/quran/quran-tajweed"
ALQ_TAJWEED_CACHE = os.path.join(CACHE, "alquran-tajweed.json")

# --------------------------------------------------------------------------------------
# Colour classes  (TAJWEED_PLAN.md "Rəng palitrası") — granular Turkish-style convention
# --------------------------------------------------------------------------------------
# class id -> (ARGB 0xAARRGGBB, [cpfair rule names])
CLASS_TABLE = {
    0: (0x00000000, []),  # sentinel: use the reader's default text colour
    1: (0xFF999999, ["silent", "hamzat_wasl", "idghaam_no_ghunnah"]),   # gray
    2: (0xFFFFC1E0, ["madd_2"]),                                        # pink
    3: (0xFFFF8E3B, ["madd_munfasil", "madd_246"]),                     # orange (separated madd)
    4: (0xFFFF5E8E, ["madd_muttasil"]),                                 # rose (connected madd)
    5: (0xFFE30000, ["madd_6"]),                                        # red (necessary madd)
    6: (0xFFB5651D, ["ghunnah"]),                                       # brown
    7: (0xFF26B55D, ["qalqalah"]),                                      # green
    8: (0xFFC62828, ["ikhfa", "ikhfa_shafawi"]),                        # dark red
    9: (0xFF9C27B0, ["idghaam_ghunnah", "idghaam_shafawi",              # purple (idghaam w/ ghunnah)
                     "idghaam_mutajanisayn", "idghaam_mutaqaribayn"]),
    10: (0xFF1976D2, ["iqlab"]),                                        # blue
}
# rules explicitly left uncoloured (class 0)
UNCOLOURED_RULES = ["lam_shamsiyyah"]

RULE_TO_CLASS = {}
for cid, (_argb, names) in CLASS_TABLE.items():
    for nm in names:
        RULE_TO_CLASS[nm] = cid
for nm in UNCOLOURED_RULES:
    RULE_TO_CLASS[nm] = 0

# When two rules cover the same glyph, the one earlier in this list wins (madd is most specific).
CLASS_PRIORITY = [5, 4, 3, 2, 7, 10, 9, 8, 6, 1, 0]
PRIORITY_RANK = {c: i for i, c in enumerate(CLASS_PRIORITY)}

NUM_CLASSES = len(CLASS_TABLE)  # 11

MAGIC = b"TJWD"
# v2: offset reference switched to cpfair's pinned Tanzil text (fixed mid-ayah offset drift);
#     projection switched to exact-char mapping (madd no longer bleeds onto the base consonant).
# v3: granular Turkish-style palette (11 classes: ghunnah/ikhfa/idghaam/iqlab now distinct).
#     Classes no longer fit 3 bits, so override diffs are now (uvarint glyph_index, u8 class).
# v4: fixed ṣila-madd drop (small waw/yeh signs) + tatweel mark-alignment; byte layout is
#     identical to v3 — the bump only forces the app importer to re-import.
VERSION = 5

# --------------------------------------------------------------------------------------
# Small utilities
# --------------------------------------------------------------------------------------
def log(*a):
    print(*a, flush=True)


def download_cached(url, path):
    if os.path.exists(path) and os.path.getsize(path) > 0:
        return
    log(f"  downloading {url}")
    req = urllib.request.Request(url, headers={"User-Agent": "anamuslim-tajweed-gen/1.0"})
    with urllib.request.urlopen(req, timeout=120) as r:
        data = r.read()
    with open(path, "wb") as f:
        f.write(data)


def is_number_token(w):
    return w != "" and all("٠" <= ch <= "٩" for ch in w)


# --------------------------------------------------------------------------------------
# Step 1 — load cpfair + Tanzil, assert offsets
# --------------------------------------------------------------------------------------
def load_cpfair():
    download_cached(CPFAIR_URL, CPFAIR_CACHE)
    data = json.load(open(CPFAIR_CACHE, encoding="utf-8"))
    out = {}
    for x in data:
        out[(x["surah"], x["ayah"])] = x["annotations"]
    return out


def load_cpfair_text():
    """The pinned Tanzil text the cpfair offsets index. Returns {(surah, ayah): text}."""
    download_cached(CPFAIR_TEXT_URL, CPFAIR_TEXT_CACHE)
    out = {}
    for line in open(CPFAIR_TEXT_CACHE, encoding="utf-8").read().splitlines():
        p = line.split("|")
        if len(p) >= 3 and p[0].isdigit() and p[1].isdigit():
            out[(int(p[0]), int(p[1]))] = p[2]
    return out


def load_tanzil():
    download_cached(TANZIL_URL, TANZIL_CACHE)
    data = json.load(open(TANZIL_CACHE, encoding="utf-8"))
    out = {}
    for s in data["data"]["surahs"]:
        for a in s["ayahs"]:
            out[(s["number"], a["numberInSurah"])] = a["text"].lstrip("﻿")
    return out


def assert_cpfair_offsets(cpfair, ref_text):
    """Every rule must land on the expected letters in cpfair's own pinned text."""
    checks = [
        ((1, 1), 7, 8, "ٱ"),      # hamzat_wasl on alef wasla
        ((1, 2), 29, 30, "ٰ"),     # madd_2 on superscript alef
        ((2, 10), 100, 101, "و"),  # madd_246 on the waw of يكذبون (drift regression guard)
    ]
    for key, s, e, expect in checks:
        seg = ref_text[key][s:e]
        assert seg == expect, f"offset check failed at {key}[{s}:{e}] = {seg!r} != {expect!r}"
    bad = 0
    for key, anns in cpfair.items():
        if key not in ref_text:
            bad += 1
            continue
        n = len(ref_text[key])
        for a in anns:
            if a["end"] > n or a["start"] < 0:
                bad += 1
                break
    assert bad == 0, f"{bad} ayahs have out-of-range cpfair offsets"
    log("  cpfair offset assertions: OK (pinned reference text)")


# --------------------------------------------------------------------------------------
# Step 2 — app ayah reconstruction
# --------------------------------------------------------------------------------------
def load_app_ayahs():
    con = sqlite3.connect(DB_PATH)
    cur = con.cursor()
    cur.execute(
        """SELECT a.surah_no, a.ayah_no, aw.word_index, aw.text
           FROM ayah_words aw JOIN ayahs a ON a.ayah_id = aw.ayah_id
           WHERE aw.script_id = 1
           ORDER BY a.surah_no, a.ayah_no, aw.word_index"""
    )
    ayahs = defaultdict(list)  # (s,a) -> [(word_index, text), ...]  (number token dropped)
    for s, ay, wi, t in cur.fetchall():
        if is_number_token(t):
            continue
        ayahs[(s, ay)].append((wi, t))
    con.close()
    return ayahs


# --------------------------------------------------------------------------------------
# Step 3 — normalisation + skeleton-anchored alignment
# --------------------------------------------------------------------------------------
HAMZA_SEATS = {"أ", "ؤ", "إ", "ئ", "ء", "ٔ", "ٕ"}
SUPERSCRIPT_ALEF = "ٰ"
# ṣila-madd signs: cpfair's text spells these as standalone Lm "letters" (small waw/yeh),
# while the app writes them as marks (tatweel + small-high-yeh). Treat them as marks on both
# sides so the letter skeletons stay aligned and the madd on them is not dropped.
SILAH_SIGNS = {"ۥ", "ۦ"}  # ARABIC SMALL WAW, ARABIC SMALL YEH


def canon_tokens(s):
    """
    Return (tokens, orig_idx) for alignment.

    Keeps letters and combining marks (as position anchors), drops spaces, tatweel, and
    standalone symbols (waqf / sajdah / rub / end-of-ayah). Each kept char yields one
    comparison token plus a back-reference to its original index in `s`, so a token match
    resolves to a real character offset.

    Canonicalisation (so the app's KFGQPC encoding and Tanzil compare equal):
      * combining marks -> generic 'M'  (except superscript-alef U+0670, a rule target that
        is identical in both encodings and kept distinct)
      * alef maksura U+0649 -> yeh U+064A
      * alef wasla  U+0671 -> alef U+0627
      * every hamza seat -> bare hamza U+0621
    """
    tokens = []
    idx = []
    for i, c in enumerate(s):
        cat = unicodedata.category(c)
        if c == " " or c == "ـ":  # space, tatweel
            continue
        if cat in ("So", "Cf", "Sk"):  # symbols / format (waqf, sajdah, rub, ...)
            continue
        if cat.startswith("M"):
            tokens.append(SUPERSCRIPT_ALEF if c == SUPERSCRIPT_ALEF else "M")
            idx.append(i)
            continue
        if "٠" <= c <= "٩":  # arabic-indic digit (defensive)
            continue
        if c == "ى":
            c = "ي"
        elif c == "ٱ":
            c = "ا"
        elif c in HAMZA_SEATS:
            c = "ء"
        tokens.append(c)
        idx.append(i)
    return tokens, idx


import difflib


def canon_letter(c):
    if c == "ى":
        return "ي"
    if c == "ٱ":
        return "ا"
    if c in HAMZA_SEATS:
        return "ء"
    return c


def letter_clusters(s):
    """Split `s` into grapheme clusters anchored on base letters.

    Returns (letters, clusters):
      letters[k]  : canonicalised base-letter token (for alignment)
      clusters[k] : list of char indices belonging to that cluster — the base letter plus
                    its trailing combining marks (harakat, tanwin, superscript alef, ...)

    Spaces and standalone symbols (waqf / sajdah / rub / end-of-ayah) break clusters and
    belong to none. Tajweed is applied at cluster granularity: a rule that lands on a
    letter or any of its marks colours the whole cluster (letter-level colouring).
    """
    letters, clusters = [], []
    cur = None
    for i, c in enumerate(s):
        cat = unicodedata.category(c)
        if c == " ":
            cur = None
            continue
        if c == "ـ":  # tatweel: a spacing connector with no rule — ignore entirely so it
            continue  # does not become a spurious "mark" that offsets positional mark mapping
        if c in SILAH_SIGNS:  # small waw/yeh (ṣila madd signs): behave like marks on the base
            if cur is not None:
                clusters[cur].append(i)
            continue
        if cat in ("So", "Cf", "Sk"):  # standalone symbol -> breaks the cluster
            cur = None
            continue
        if cat.startswith("M"):  # combining mark -> attaches to current base letter
            if cur is not None:
                clusters[cur].append(i)
            continue
        if "٠" <= c <= "٩":
            cur = None
            continue
        letters.append(canon_letter(c))
        clusters.append([i])
        cur = len(letters) - 1
    return letters, clusters


def basmala_prefix_len(tz):
    """Length (chars) of the 'basmala + trailing space' prefix Tanzil prepends to a
    surah-opening ayah. The basmala is the first four space-separated words."""
    parts = tz.split(" ")
    if len(parts) <= 4:
        return 0
    return len(" ".join(parts[:4])) + 1  # + the space that follows


# --------------------------------------------------------------------------------------
# Steps 3-4 — per-ayah app char colour classes (exact-char mapping)
# --------------------------------------------------------------------------------------
def build_char_map(ref_body, app_text):
    """Map each reference-text char index -> the corresponding app-text char index.

    Letters are aligned 1:1 (difflib on the consonant skeleton); within each matched letter
    the trailing marks are mapped positionally (both texts are fully-voweled Uthmani, so a
    letter carries the same number of marks in each). This is exact — a rule on a
    superscript-alef madd maps to the app superscript-alef only, NOT the base consonant, so
    madd/qalqalah no longer bleed onto neighbouring letters. Returns (cmap, quality)."""
    aL, aC = letter_clusters(app_text)   # aC[k] = [base_idx, mark_idx, ...]
    tL, tC = letter_clusters(ref_body)
    sm = difflib.SequenceMatcher(None, tL, aL, autojunk=False)
    cmap = {}
    edits = 0
    for tag, i1, i2, j1, j2 in sm.get_opcodes():
        if tag == "equal":
            for k in range(i2 - i1):
                tcl = tC[i1 + k]
                acl = aC[j1 + k]
                cmap[tcl[0]] = acl[0]  # base letter -> base letter
                tmarks, amarks = tcl[1:], acl[1:]
                for m, tm in enumerate(tmarks):
                    cmap[tm] = amarks[m] if m < len(amarks) else acl[0]
        else:
            edits += max(i2 - i1, j2 - j1)
    quality = 0 if tL == aL else (1 if edits <= 2 else 2)
    return cmap, quality


def ayah_char_classes(surah, ayah, app_words, ref_text, cpfair):
    """Return (classes, span_starts, quality):
       classes      : list[int] over the joined app-ayah text (one class per char)
       span_starts  : list[int] start offset of each app word within the joined text
       quality      : 0 (letters identical), 1 (edit<=2), 2 (worse)

    `ref_text` is cpfair's pinned Tanzil text (the coordinate system its offsets index)."""
    texts = [t for _, t in app_words]
    app_text = " ".join(texts)
    span_starts = []
    pos = 0
    for t in texts:
        span_starts.append(pos)
        pos += len(t) + 1  # + separating space

    classes = [0] * len(app_text)

    key = (surah, ayah)
    ref = ref_text.get(key)
    anns = cpfair.get(key, [])
    if ref is None:
        return classes, span_starts, 0

    # Strip the basmala prefix the text carries on surah-opening ayahs (not surah 1 / 9).
    pref = basmala_prefix_len(ref) if (ayah == 1 and surah not in (1, 9)) else 0
    ref_body = ref[pref:]

    cmap, quality = build_char_map(ref_body, app_text)

    for a in anns:
        cid = RULE_TO_CLASS.get(a["rule"])
        if cid is None:
            raise SystemExit(f"unknown cpfair rule {a['rule']!r}")
        if cid == 0:
            continue
        s = a["start"] - pref
        e = a["end"] - pref
        if e <= 0:
            continue  # entirely inside the stripped basmala
        if s < 0:
            s = 0
        for i in range(s, e):
            ap = cmap.get(i)
            if ap is None:
                continue
            if classes[ap] == 0 or PRIORITY_RANK[cid] < PRIORITY_RANK[classes[ap]]:
                classes[ap] = cid
    return classes, span_starts, quality


# --------------------------------------------------------------------------------------
# Step 5 — shaping + layout guard
# --------------------------------------------------------------------------------------
class Shaper:
    def __init__(self, font_path):
        self.font = hb.Font(hb.Face(open(font_path, "rb").read()))
        self._cache = {}

    def shape(self, text):
        """Return list of (gid, cluster) for `text`. cluster = source char index in `text`."""
        cached = self._cache.get(text)
        if cached is not None:
            return cached
        buf = hb.Buffer()
        buf.add_str(text)
        buf.direction = "rtl"
        buf.script = "Arab"
        buf.language = "ar"
        buf.cluster_level = 1  # MONOTONE_CHARACTERS — keeps marks separable
        hb.shape(self.font, buf, None)  # features=None: default Arabic shaping
        out = [(g.codepoint, g.cluster) for g in buf.glyph_infos]
        self._cache[text] = out
        return out


def load_atlas():
    """Return (doc_order, text2glyphs) from the shipped 6x.zip.
       doc_order : list of word texts in layout.json document order (base index == position)
       text2glyphs : text -> list[gid] (stored layout order)"""
    with zipfile.ZipFile(ATLAS_ZIP) as z:
        layout = json.loads(z.read("layout.json"))
    docs = layout["documents"]
    doc_order = []
    text2glyphs = {}
    for k in docs:  # insertion order == "0".."N-1" == Kotlin stream order
        d = docs[k]
        doc_order.append(d["text"])
        text2glyphs[d["text"]] = [g["g"] for g in d["glyphs"]]
    return doc_order, text2glyphs


def glyph_char_ranges(shaped, text_len):
    """For each glyph, the range of source chars it covers.

    HarfBuzz clusters partition the word into segments at distinct cluster values; a glyph
    covers [its cluster, next-higher cluster) — i.e. the whole char span it was shaped from.
    This matters because the Uthmani atlas ligates multi-letter runs into single wide glyphs
    (e.g. the tail of الرحمٰن is one glyph spanning ح…ن, superscript-alef madd included), so a
    glyph must inherit a rule that lands on ANY char in its span, not just its cluster index.
    """
    clusters = sorted({c for _g, c in shaped})
    nxt = {c: (clusters[j + 1] if j + 1 < len(clusters) else text_len)
           for j, c in enumerate(clusters)}
    return [(cl, nxt[cl]) for _g, cl in shaped]


def _is_base_letter(ch):
    """A real base letter (consonant/long vowel) that the atlas draws as a body glyph — NOT a
    combining mark and NOT tatweel. Note: superscript alef U+0670 and the small-high signs are
    Unicode category Mn (marks), so they correctly count as marks, not base letters."""
    if ch == "ـ":  # tatweel: a spacing connector, carries no letter of its own
        return False
    return unicodedata.category(ch).startswith("L")


def word_glyph_classes(shaper, text, char_classes, span_start, stored_gids):
    """Shape `text`, HARD-ASSERT gids == stored layout, and return per-glyph class bytes.

    A glyph normally inherits the highest-priority non-zero class over its whole char span.

    Ligature suppression: the Uthmani atlas ligates multi-letter runs into one wide glyph (e.g.
    الرحمٰن's tail حْمَٰنِ is a single glyph spanning ح+م+ن). Colouring the whole glyph because a
    madd sits on an internal superscript-alef mark floods letters that carry no rule. So when a
    glyph spans TWO OR MORE base letters, it is only coloured if the winning rule lands on an
    actual base letter inside it; a rule that only touches internal MARKS is dropped for that
    glyph. Glyphs over ≤1 base letter (ordinary letter+harakat, or a standalone mark glyph such
    as a lone superscript-alef madd) keep the normal whole-span behaviour."""
    shaped = shaper.shape(text)
    gids = [g for g, _ in shaped]
    if gids != stored_gids:
        raise AssertionError(
            f"gid mismatch for {text!r}: shaped {gids} != layout {stored_gids}"
        )
    out = bytearray(len(shaped))
    for i, (lo, hi) in enumerate(glyph_char_ranges(shaped, len(text))):
        span = range(span_start + lo, span_start + hi)
        base_letters = sum(
            1 for ci in span
            if 0 <= ci < len(char_classes) and _is_base_letter(text[ci - span_start])
        )
        ligature = base_letters >= 2
        best = 0
        for ci in span:
            cls = char_classes[ci] if 0 <= ci < len(char_classes) else 0
            if cls == 0:
                continue
            # In a true multi-letter ligature, only a rule anchored on a base letter may colour
            # the whole glyph; rules living on interior marks (e.g. a madd's superscript alef)
            # are suppressed so ح+م+ن aren't tinted for a madd that only touches the mark.
            if ligature and not _is_base_letter(text[ci - span_start]):
                continue
            if best == 0 or PRIORITY_RANK[cls] < PRIORITY_RANK[best]:
                best = cls
        out[i] = best
    return bytes(out)


# --------------------------------------------------------------------------------------
# Step 6 — encoding  (see FORMAT.md for the exact byte layout)
# --------------------------------------------------------------------------------------
def _uvarint(n):
    out = bytearray()
    while True:
        b = n & 0x7F
        n >>= 7
        out.append(b | 0x80 if n else b)
        if not n:
            break
    return bytes(out)


def _nibble_pack(arr):
    """Pack a class array (each value 0..15) two-per-byte: low nibble = even glyph."""
    out = bytearray((len(arr) + 1) // 2)
    for i, v in enumerate(arr):
        if i & 1:
            out[i >> 1] |= v << 4
        else:
            out[i >> 1] |= v
    return bytes(out)


def encode(doc_order, text2glyphs, base_by_text, override_groups):
    """Serialise the body (pre-gzip). `override_groups`: {ayah_id: [(word_index, diffs)]}
       where diffs is a list of (glyph_index, class)."""
    buf = io.BytesIO()
    buf.write(MAGIC)
    buf.write(struct.pack("<BBBB", VERSION, 0, NUM_CLASSES, 0))

    # palette: one ARGB u32 per class
    for cid in range(NUM_CLASSES):
        buf.write(struct.pack("<I", CLASS_TABLE[cid][0]))

    # rule-name table (self-describing; importer may skip it via the u16 length prefix)
    rn = io.BytesIO()
    for cid in range(NUM_CLASSES):
        names = CLASS_TABLE[cid][1] if cid != 0 else UNCOLOURED_RULES
        rn.write(struct.pack("<B", len(names)))
        for nm in names:
            nb = nm.encode("utf-8")
            rn.write(struct.pack("<B", len(nb)))
            rn.write(nb)
    rn_bytes = rn.getvalue()
    buf.write(struct.pack("<H", len(rn_bytes)))
    buf.write(rn_bytes)

    # base section — one nibble-packed record per layout document, in document order
    buf.write(struct.pack("<I", len(doc_order)))
    for text in doc_order:
        arr = base_by_text.get(text)
        if arr is None:
            arr = bytes(len(text2glyphs[text]))  # unused doc -> all class 0
        assert len(arr) == len(text2glyphs[text])
        buf.write(struct.pack("<B", len(arr)))
        buf.write(_nibble_pack(arr))

    # override section — grouped by ayah, ayah_id delta-varint encoded
    buf.write(struct.pack("<I", len(override_groups)))
    prev = 0
    for ayah_id in sorted(override_groups):
        buf.write(_uvarint(ayah_id - prev))
        prev = ayah_id
        words = sorted(override_groups[ayah_id])
        buf.write(struct.pack("<B", len(words)))
        for wi, diffs in words:
            buf.write(struct.pack("<BB", wi, len(diffs)))
            for gi, cl in diffs:
                # v3: classes exceed 3 bits (corpus max override glyph index is 18), so a diff
                # is a uvarint glyph index followed by a single class byte.
                assert cl < NUM_CLASSES
                buf.write(_uvarint(gi))
                buf.write(struct.pack("<B", cl))

    return buf.getvalue()


# --------------------------------------------------------------------------------------
# Driver
# --------------------------------------------------------------------------------------
def main():
    log("== Tajweed pipeline ==")
    log("[1] cpfair annotations + pinned reference text")
    cpfair = load_cpfair()
    ref_text = load_cpfair_text()          # offset reference (shipped-data derivation)
    tanzil = load_tanzil()                  # alquran.cloud — cross-check only
    assert_cpfair_offsets(cpfair, ref_text)

    log("[2] app ayahs from quranapp.db")
    app_ayahs = load_app_ayahs()
    log(f"    {len(app_ayahs)} ayahs, {sum(len(v) for v in app_ayahs.values())} word occurrences")

    log("[3-5] align, project, shape (gid guard active)")
    shaper = Shaper(FONT_PATH)
    doc_order, text2glyphs = load_atlas()
    log(f"    atlas: {len(doc_order)} unique word documents")

    # occurrence colour arrays, grouped by word text (for base) and keyed for overrides
    by_text = defaultdict(list)          # text -> list of class-byte arrays
    occ_records = []                     # (ayah_id, word_index, text, arr)
    q_exact = q_edit = q_worse = 0
    max_wi = 0

    for (s, a) in sorted(app_ayahs.keys()):
        words = app_ayahs[(s, a)]
        char_classes, span_starts, quality = ayah_char_classes(s, a, words, ref_text, cpfair)
        if quality == 0:
            q_exact += 1
        elif quality == 1:
            q_edit += 1
        else:
            q_worse += 1
        ayah_id = s * 1000 + a
        for (wi, text), span in zip(words, span_starts):
            max_wi = max(max_wi, wi)
            stored = text2glyphs.get(text)
            if stored is None:
                raise AssertionError(f"word not in atlas: {text!r} ({s}:{a})")
            arr = word_glyph_classes(shaper, text, char_classes, span, stored)
            by_text[text].append(arr)
            occ_records.append((ayah_id, wi, text, arr))

    assert max_wi < 256, f"word_index {max_wi} exceeds u8 range"
    log(f"    gid-sequence guard passed for all {len(occ_records)} occurrences")
    log(f"    alignment: exact={q_exact} edit<=2={q_edit} worse={q_worse} "
        f"({100*(q_exact+q_edit)/len(app_ayahs):.2f}% within edit<=2)")

    # base = modal array per unique word text
    base_by_text = {}
    for text, arrs in by_text.items():
        base_by_text[text] = Counter(arrs).most_common(1)[0][0]

    # overrides = occurrences differing from base, grouped by ayah, stored as glyph diffs
    override_groups = defaultdict(list)  # ayah_id -> [(word_index, [(glyph_index, class)])]
    n_overrides = 0
    for ayah_id, wi, text, arr in occ_records:
        base = base_by_text[text]
        if arr != base:
            diffs = [(i, arr[i]) for i in range(len(arr)) if arr[i] != base[i]]
            override_groups[ayah_id].append((wi, diffs))
            n_overrides += 1

    # coverage stats
    total_glyphs = sum(len(a) for _, _, _, a in occ_records)
    coloured_glyphs = sum(sum(1 for b in a if b != 0) for _, _, _, a in occ_records)
    log(f"    glyph instances: {total_glyphs}, coloured: {coloured_glyphs} "
        f"({100*coloured_glyphs/total_glyphs:.2f}%)")
    log(f"    overrides: {n_overrides} occurrences in {len(override_groups)} ayahs "
        f"({100*n_overrides/len(occ_records):.2f}% of occurrences)")

    log("[6] encode + gzip")
    body = encode(doc_order, text2glyphs, base_by_text, override_groups)
    gz = gzip.compress(body, compresslevel=9, mtime=0)  # mtime=0 -> reproducible bytes
    os.makedirs(os.path.dirname(OUT_BIN), exist_ok=True)
    with open(OUT_BIN, "wb") as f:
        f.write(gz)
    log(f"    body {len(body)} B -> gzip {len(gz)} B ({len(gz)/1024:.1f} KB) -> {OUT_BIN}")

    stats = {
        "ayahs": len(app_ayahs),
        "occurrences": len(occ_records),
        "unique_words": len(doc_order),
        "total_glyphs": total_glyphs,
        "coloured_glyphs": coloured_glyphs,
        "coloured_pct": round(100 * coloured_glyphs / total_glyphs, 2),
        "overrides": n_overrides,
        "override_ayahs": len(override_groups),
        "align_exact": q_exact,
        "align_edit2": q_edit,
        "align_worse": q_worse,
        "body_bytes": len(body),
        "gzip_bytes": len(gz),
    }
    with open(os.path.join(QA_DIR, "stats.json"), "w", encoding="utf-8") as f:
        json.dump(stats, f, ensure_ascii=False, indent=2)

    # QA + cross-check live in a sibling module to keep this file focused.
    override_full = {}  # (ayah_id, word_index) -> full class array, for QA rendering
    for ayah_id, wi, text, arr in occ_records:
        if arr != base_by_text[text]:
            override_full[(ayah_id, wi)] = arr
    try:
        import qa_build
        qa_build.build(shaper, doc_order, text2glyphs, base_by_text, override_full,
                       app_ayahs, tanzil, cpfair, CLASS_TABLE, stats)
    except Exception as e:  # QA must never block data generation
        log(f"    QA step skipped/failed: {e}")

    log("== done ==")
    return stats


if __name__ == "__main__":
    sys.path.insert(0, HERE)
    main()
