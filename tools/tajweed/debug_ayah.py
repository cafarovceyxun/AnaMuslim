#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
debug_ayah.py — trace the full tajweed pipeline for one ayah.

    python3 debug_ayah.py 2:9 [2:10 ...]

Prints, stage by stage, so a wrong colour is attributable to a specific step:
  1. app words (script_id=1) and their char spans in the joined ayah text
  2. cpfair source annotations: rule + Tanzil [start:end) + the actual letters covered
  3. the letter-skeleton alignment quality
  4. per app-char colour class after projection
  5. per-word, per-glyph final class (base merged with any override) + the char span each
     glyph covers and the cpfair letters inside that span
"""

import sys
import unicodedata

import generate as G

G.log = lambda *a: None


def uname(c):
    try:
        return unicodedata.name(c)
    except ValueError:
        return "?"


def short(c):
    # compact label for a char
    n = uname(c)
    return n.replace("ARABIC LETTER ", "").replace("ARABIC ", "")


def dump(surah, ayah, cpfair, ref_text, app, shaper, doc_order, text2glyphs,
         base_by_text, override_full):
    key = (surah, ayah)
    words = app[key]
    texts = [t for _, t in words]
    app_text = " ".join(texts)
    spans = []
    pos = 0
    for t in texts:
        spans.append(pos)
        pos += len(t) + 1

    cc, span_starts, quality = G.ayah_char_classes(surah, ayah, words, ref_text, cpfair)

    print("=" * 90)
    print(f"{surah}:{ayah}   alignment quality = {quality}  (0=exact letters, 1=edit<=2, 2=worse)")
    print(f"app text : {app_text}")
    tz = ref_text.get(key, "")
    pref = G.basmala_prefix_len(tz) if (ayah == 1 and surah not in (1, 9)) else 0
    print(f"cpfair tx: {tz}")
    if pref:
        print(f"           (basmala prefix stripped: {pref} chars)")

    print("\n-- app words & spans --")
    for (wi, t), sp in zip(words, spans):
        print(f"  w{wi:<2} span[{sp:>3}..{sp+len(t)}]  {t}")

    print("\n-- cpfair source annotations (Tanzil coords) --")
    for a in cpfair.get(key, []):
        seg = tz[a["start"]:a["end"]]
        cls = G.RULE_TO_CLASS.get(a["rule"], "?")
        letters = "".join(short(c) + "|" for c in seg)
        print(f"  {a['rule']:<20} class {cls}  [{a['start']}:{a['end']}] = {seg!r}  ({letters})")

    print("\n-- projected app-char classes (non-zero only) --")
    for i, c in enumerate(app_text):
        if cc[i] != 0:
            print(f"  app[{i:>3}] class {cc[i]}  {c!r}  {short(c)}")

    print("\n-- per-word per-glyph final classes (base+override) --")
    for (wi, t), sp in zip(words, spans):
        stored = text2glyphs[t]
        shaped = shaper.shape(t)
        ranges = G.glyph_char_ranges(shaped, len(t))
        base = base_by_text[t]
        ov = override_full.get((surah * 1000 + ayah, wi))
        final = list(ov) if ov is not None else list(base)
        tag = "OVERRIDE" if ov is not None else "base"
        print(f"  w{wi} {t!r}  ({tag})")
        for gi, ((gid, cl), (lo, hi)) in enumerate(zip(shaped, ranges)):
            covered = "".join(short(t[k]) + "|" for k in range(lo, hi))
            fc = final[gi]
            mark = "  <<<" if fc != 0 else ""
            print(f"     g{gi:<2} gid{gid:<5} cluster{cl:<2} covers chars[{lo}:{hi}]={covered:<28} class {fc}{mark}")


def main():
    args = sys.argv[1:] or ["2:9", "2:10"]
    cpfair = G.load_cpfair()
    ref_text = G.load_cpfair_text()
    app = G.load_app_ayahs()
    shaper = G.Shaper(G.FONT_PATH)
    doc_order, text2glyphs = G.load_atlas()

    # rebuild base + overrides exactly as generate.main would
    from collections import Counter, defaultdict
    by_text = defaultdict(list)
    occ = []
    for (s, a) in sorted(app.keys()):
        words = app[(s, a)]
        char_classes, span_starts, _q = G.ayah_char_classes(s, a, words, ref_text, cpfair)
        for (wi, text), sp in zip(words, span_starts):
            arr = G.word_glyph_classes(shaper, text, char_classes, sp, text2glyphs[text])
            by_text[text].append(arr)
            occ.append((s * 1000 + a, wi, text, arr))
    base_by_text = {t: Counter(a).most_common(1)[0][0] for t, a in by_text.items()}
    override_full = {}
    for aid, wi, text, arr in occ:
        if arr != base_by_text[text]:
            override_full[(aid, wi)] = arr

    for spec in args:
        s, a = spec.split(":")
        dump(int(s), int(a), cpfair, ref_text, app, shaper, doc_order, text2glyphs,
             base_by_text, override_full)


if __name__ == "__main__":
    import os
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    main()
