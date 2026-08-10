#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
qa_build.py — QA artefacts for the tajweed pipeline (imported by generate.py).

  * qa/index.html : the gold ayahs rendered from the REAL atlas glyph bitmaps with the
    per-glyph colours the pipeline assigns — the exact glyphs/positions the app draws, so a
    human can eyeball correctness.
  * agreement cross-check against alquran.cloud's independent `quran-tajweed` edition over a
    random sample of ayahs (offline only; that data is never shipped).
"""

import base64
import io
import json
import os
import random
import re
import unicodedata
import urllib.request
import zipfile

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", ".."))
CACHE = os.path.join(HERE, "cache")
QA_DIR = os.path.join(HERE, "qa")
ATLAS_ZIP = os.path.join(REPO, "shared/src/commonMain/composeResources/files/atlas/uthmani/6x.zip")

UPEM = 2048
PPEM = 192
FONT_SIZE_PX = float(PPEM)          # render 1:1 with the 6x atlas -> glyphScale == 1
FONT_SCALE = FONT_SIZE_PX / UPEM
ASCENDER_FU = 2400
DESCENDER_FU = -1200
BASELINE_Y = ASCENDER_FU * FONT_SCALE
CANVAS_H = int(round((ASCENDER_FU - DESCENDER_FU) * FONT_SCALE))
DISPLAY_SCALE = 0.34                # downscale embedded PNGs for the HTML
DEFAULT_TEXT_RGB = (26, 26, 26)

GOLD = [
    ("Al-Fātiḥa 1:1", 1, 1), ("Al-Fātiḥa 1:2", 1, 2), ("Al-Fātiḥa 1:3", 1, 3),
    ("Al-Fātiḥa 1:4", 1, 4), ("Al-Fātiḥa 1:5", 1, 5), ("Al-Fātiḥa 1:6", 1, 6),
    ("Al-Fātiḥa 1:7", 1, 7),
    ("Al-Fātiḥa 1:3 الرَّحْمَٰنِ (ligature madd)", 1, 3),
    ("Al-Baqara 2:8 (idghaam-ghunnah + ikhfa-shafawi)", 2, 8),
    ("Al-Baqara 2:9 (device-report ayah)", 2, 9),
    ("Al-Baqara 2:10 (device-report ayah)", 2, 10),
    ("Al-Baqara 2:255 (Āyat al-Kursī)", 2, 255),
    ("Muqaṭṭaʿāt — Al-Baqara 2:1 (الٓمٓ)", 2, 1),
    ("Muqaṭṭaʿāt — Yā-Sīn 36:1 (يسٓ)", 36, 1),
    ("Al-Ikhlāṣ 112:1", 112, 1), ("Al-Ikhlāṣ 112:2", 112, 2),
    ("Al-Ikhlāṣ 112:3", 112, 3), ("Al-Ikhlāṣ 112:4", 112, 4),
]


def argb_to_rgb(argb):
    return ((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF)


def load_atlas_bitmaps():
    with zipfile.ZipFile(ATLAS_ZIP) as z:
        atlas = json.loads(z.read("atlas.json"))
        png = Image.open(io.BytesIO(z.read("atlas_0.png"))).convert("L")
    return atlas["glyphs"], png


def render_word(text, stored_placements, classes, glyphs_meta, atlas_png, palette_rgb):
    """Compose one word bitmap from atlas glyphs, tinting glyph i with `classes[i]`."""
    total_w = sum(p["xa"] for p in stored_placements) * FONT_SCALE
    w = max(1, int(round(total_w)))
    canvas = Image.new("RGBA", (w, CANVAS_H), (0, 0, 0, 0))
    cur_x = 0.0
    for i, p in enumerate(stored_placements):
        g = glyphs_meta.get(str(p["g"]))
        if g and g["w"] > 0 and g["h"] > 0:
            x = cur_x + p["xo"] * FONT_SCALE + g["bearing_x"]      # glyphScale == 1
            y = BASELINE_Y - p["yo"] * FONT_SCALE - g["bearing_y"]
            mask = atlas_png.crop((g["x"], g["y"], g["x"] + g["w"], g["y"] + g["h"]))
            cid = classes[i] if i < len(classes) else 0
            rgb = palette_rgb.get(cid, DEFAULT_TEXT_RGB) if cid != 0 else DEFAULT_TEXT_RGB
            tint = Image.new("RGBA", mask.size, rgb + (0,))
            tint.putalpha(mask)
            canvas.alpha_composite(tint, (int(round(x)), int(round(y))))
        cur_x += p["xa"] * FONT_SCALE
    return canvas


def resolve_classes(text, ayah_id, wi, base_by_text, overrides):
    return overrides.get((ayah_id, wi), base_by_text[text])


def png_data_uri(img):
    if DISPLAY_SCALE != 1.0:
        img = img.resize((max(1, int(img.width * DISPLAY_SCALE)),
                          max(1, int(img.height * DISPLAY_SCALE))), Image.LANCZOS)
    bg = Image.new("RGBA", img.size, (255, 255, 255, 255))
    bg.alpha_composite(img)
    buf = io.BytesIO()
    bg.convert("RGB").save(buf, format="PNG")
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()


def build(shaper, doc_order, text2glyphs, base_by_text, overrides,
          app_ayahs, tanzil, cpfair, class_table, stats):
    os.makedirs(QA_DIR, exist_ok=True)
    palette_rgb = {cid: argb_to_rgb(argb) for cid, (argb, _n) in class_table.items() if cid != 0}
    # `names` is empty for the classes whose rules were dropped from the coloured set (the madds,
    # since v7). Guard it: `names[0]` on an empty list raised IndexError, and because the caller
    # swallows QA exceptions to keep data generation running, the whole QA step had been silently
    # skipping ever since — "QA step skipped/failed: list index out of range".
    def _class_label(cid, names):
        if cid == 0:
            base = "default"
        elif not names:
            return "(unused)"
        else:
            base = names[0]
        return base + (f" (+{len(names) - 1})" if len(names) > 1 else "")

    class_names = {cid: _class_label(cid, names) for cid, (_a, names) in class_table.items()}

    glyphs_meta, atlas_png = load_atlas_bitmaps()

    # rebuild the raw placements per word text from the layout (need xa/xo/yo, not just gids)
    with zipfile.ZipFile(ATLAS_ZIP) as z:
        layout = json.loads(z.read("layout.json"))
    text2placements = {d["text"]: d["glyphs"] for d in layout["documents"].values()}

    rows_html = []
    for label, s, a in GOLD:
        if (s, a) not in app_ayahs:
            continue
        words = app_ayahs[(s, a)]
        word_imgs = []
        for wi, text in words:
            classes = resolve_classes(text, s * 1000 + a, wi, base_by_text, overrides)
            img = render_word(text, text2placements[text], classes, glyphs_meta, atlas_png, palette_rgb)
            word_imgs.append(png_data_uri(img))
        # RTL: first word rightmost
        imgs = "".join(f'<img src="{u}" alt="">' for u in word_imgs)
        rows_html.append(
            f'<div class="ayah"><div class="lbl">{label}</div>'
            f'<div class="line" dir="rtl">{imgs}</div></div>'
        )

    legend = "".join(
        f'<span class="chip"><i style="background:#%02x%02x%02x"></i>' % argb_to_rgb(class_table[c][0])
        + f'{c} · {class_names[c]}</span>'
        for c in class_table if c != 0
    )

    agree = cross_check(app_ayahs, tanzil, base_by_text, overrides, text2glyphs, shaper)

    st = stats
    html = f"""<!doctype html><meta charset="utf-8">
<title>Tajweed QA — AnaMuslim Uthmani</title>
<style>
 body{{font:14px/1.5 -apple-system,Segoe UI,Roboto,sans-serif;margin:24px;background:#fafafa;color:#222}}
 h1{{font-size:20px}} .sub{{color:#666}}
 .legend{{margin:14px 0;display:flex;flex-wrap:wrap;gap:10px}}
 .chip{{display:inline-flex;align-items:center;gap:6px;background:#fff;border:1px solid #e2e2e2;border-radius:14px;padding:3px 10px}}
 .chip i{{width:14px;height:14px;border-radius:3px;display:inline-block}}
 .ayah{{background:#fff;border:1px solid #eaeaea;border-radius:10px;padding:12px 16px;margin:12px 0}}
 .lbl{{color:#555;font-size:12px;margin-bottom:6px}}
 .line{{display:flex;flex-wrap:wrap;align-items:flex-end;gap:10px}}
 .line img{{height:auto}}
 table{{border-collapse:collapse;margin-top:8px}} td,th{{border:1px solid #e5e5e5;padding:4px 8px;text-align:left}}
 code{{background:#f2f2f2;padding:1px 4px;border-radius:3px}}
</style>
<h1>Tajweed colour QA — Uthmani atlas</h1>
<div class="sub">Glyphs are cropped from the shipped atlas (<code>6x.zip</code>) and positioned with the
app's own render math, then tinted by the pipeline's per-glyph class. This is what the reader draws.</div>
<div class="legend">{legend}</div>
<h2>Coverage</h2>
<table>
<tr><th>ayahs</th><th>word occurrences</th><th>unique words</th><th>glyph instances</th>
<th>coloured glyphs</th><th>overrides</th><th>align exact</th><th>align edit≤2</th><th>align worse</th>
<th>output (gzip)</th></tr>
<tr><td>{st['ayahs']}</td><td>{st['occurrences']}</td><td>{st['unique_words']}</td>
<td>{st['total_glyphs']}</td><td>{st['coloured_glyphs']} ({st['coloured_pct']}%)</td>
<td>{st['overrides']}</td><td>{st['align_exact']}</td><td>{st['align_edit2']}</td>
<td>{st['align_worse']}</td><td>{st['gzip_bytes']/1024:.1f} KB</td></tr>
</table>
<h2>Cross-check vs alquran.cloud tajweed markup</h2>
<div class="sub">Random {agree['n']} ayahs. Mean per-ayah agreement on which letters get coloured,
restricted to the categories both taxonomies colour (excludes lām-shamsiyya &amp; idghām-no-ghunnah,
which we deliberately leave uncoloured): <b>{agree['mean']:.1f}%</b>
(median {agree['median']:.1f}%).</div>
<h2>Gold ayahs</h2>
{''.join(rows_html)}
"""
    with open(os.path.join(QA_DIR, "index.html"), "w", encoding="utf-8") as f:
        f.write(html)
    print(f"    QA: wrote qa/index.html ; cross-check mean agreement {agree['mean']:.1f}% over {agree['n']} ayahs")


# --------------------------------------------------------------------------------------
# Cross-check against alquran.cloud tajweed edition (offline only)
# --------------------------------------------------------------------------------------
ALQ_TAJWEED_CACHE = os.path.join(CACHE, "alquran-tajweed.json")
ALQ_TAJWEED_URL = "https://api.alquran.cloud/v1/quran/quran-tajweed"

# alquran.cloud tag code -> whether it belongs to a category we also colour.
# l = lam-shamsiyya (we leave uncoloured) -> excluded from the shared metric.
ALQ_SHARED = {"h": True, "s": True, "n": True, "p": True, "m": True, "q": True,
              "g": True, "i": True, "f": True, "b": True, "c": True, "w": True, "a": True}
ALQ_EXCLUDE = {"l"}
TAG_RE = re.compile(r"\[([a-z])(?::\d+)?\[")


def _alq_colored_letters(marked):
    """Parse '[x[..]' markup -> (plain_text, colored_flags) where a flag is True when the
    char sits inside a shared-category tag (lām excluded)."""
    plain = []
    flags = []
    stack = []
    i = 0
    n = len(marked)
    while i < n:
        m = TAG_RE.match(marked, i)
        if m:
            stack.append(m.group(1))
            i = m.end()
            continue
        c = marked[i]
        if c == "]":
            if stack:
                stack.pop()
            i += 1
            continue
        active = any((t in ALQ_SHARED and t not in ALQ_EXCLUDE) for t in stack)
        plain.append(c)
        flags.append(active)
        i += 1
    return "".join(plain), flags


def cross_check(app_ayahs, tanzil, base_by_text, overrides, text2glyphs, shaper, n=100, seed=7):
    import difflib
    from generate import canon_tokens  # reuse the exact tokeniser

    try:
        if not (os.path.exists(ALQ_TAJWEED_CACHE) and os.path.getsize(ALQ_TAJWEED_CACHE) > 0):
            req = urllib.request.Request(ALQ_TAJWEED_URL, headers={"User-Agent": "anamuslim-tajweed-gen/1.0"})
            with urllib.request.urlopen(req, timeout=120) as r:
                open(ALQ_TAJWEED_CACHE, "wb").write(r.read())
        alq = json.load(open(ALQ_TAJWEED_CACHE, encoding="utf-8"))
    except Exception as e:
        return {"n": 0, "mean": 0.0, "median": 0.0, "note": f"unavailable: {e}"}

    alq_map = {}
    for s in alq["data"]["surahs"]:
        for a in s["ayahs"]:
            alq_map[(s["number"], a["numberInSurah"])] = a["text"].lstrip("﻿")

    keys = [k for k in app_ayahs.keys() if k in alq_map]
    random.Random(seed).shuffle(keys)
    keys = keys[:n]

    def letters_only(seq_tokens, seq_idx, flags_by_orig):
        # keep only base-letter tokens (marks are 'M' or superscript alef); compare colouring at letters
        out = []
        for t, oi in zip(seq_tokens, seq_idx):
            if t == "M":
                continue
            out.append((t, flags_by_orig.get(oi, False)))
        return out

    scores = []
    for (s, a) in keys:
        words = app_ayahs[(s, a)]
        our_text = " ".join(t for _, t in words)
        span_starts = []
        pos = 0
        for _wi, t in words:
            span_starts.append(pos)
            pos += len(t) + 1
        # our coloured char flags: rebuild from the shipped base/override glyph classes,
        # marking every char in a coloured glyph's cluster range (matches the ligature logic)
        from generate import glyph_char_ranges
        our_flags = [False] * len(our_text)
        for (wi, text), span in zip(words, span_starts):
            classes = overrides.get((s * 1000 + a, wi), base_by_text[text])
            shaped = shaper.shape(text)
            for gi, (lo, hi) in enumerate(glyph_char_ranges(shaped, len(text))):
                if gi < len(classes) and classes[gi] != 0:
                    for ci in range(span + lo, span + hi):
                        if 0 <= ci < len(our_flags):
                            our_flags[ci] = True

        alq_plain, alq_flags = _alq_colored_letters(alq_map[(s, a)])

        at, ai = canon_tokens(our_text)
        bt, bi = canon_tokens(alq_plain)
        our_letters = letters_only(at, ai, {i: our_flags[i] for i in range(len(our_flags))})
        alq_letters = letters_only(bt, bi, {i: alq_flags[i] for i in range(len(alq_flags))})

        # align letter sequences, compare colour flags on matched letters
        seqA = [t for t, _ in our_letters]
        seqB = [t for t, _ in alq_letters]
        sm = difflib.SequenceMatcher(None, seqA, seqB, autojunk=False)
        matched = agree = 0
        for tag, i1, i2, j1, j2 in sm.get_opcodes():
            if tag != "equal":
                continue
            for k in range(i2 - i1):
                matched += 1
                if our_letters[i1 + k][1] == alq_letters[j1 + k][1]:
                    agree += 1
        if matched:
            scores.append(100.0 * agree / matched)

    scores.sort()
    if not scores:
        return {"n": 0, "mean": 0.0, "median": 0.0}
    mean = sum(scores) / len(scores)
    median = scores[len(scores) // 2]
    return {"n": len(scores), "mean": mean, "median": median}
