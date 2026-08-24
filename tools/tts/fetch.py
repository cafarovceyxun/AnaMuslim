#!/usr/bin/env python3
"""Supabase-dən səsləndiriləcək mətni çəkir → build/tts/source.json.

    python3 tools/tts/fetch.py            # hər ikisi
    python3 tools/tts/fetch.py --quran    # yalnız Quran tərcüməsi
    python3 tools/tts/fetch.py --hadith   # yalnız hədis

Çıxışda hər sətir üçün normalizasiyadan sonrakı mətnin `sha256`-sı var — sonrakı
mərhələlər yalnız hash-i dəyişənləri yenidən render edir.
"""
import argparse
import hashlib
import json
import sys
import urllib.request

from config import (
    BUILD, SOURCE_JSON, SUPABASE_ANON_KEY, SUPABASE_URL, TRANSLATION_SLUG, ensure_dirs
)
from normalize import normalize_hadith, normalize_verse

PAGE = 1000


def _get(path):
    req = urllib.request.Request(
        SUPABASE_URL + path,
        headers={"apikey": SUPABASE_ANON_KEY, "Authorization": f"Bearer {SUPABASE_ANON_KEY}"},
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.load(resp)


def _paged(path_fmt):
    rows, offset = [], 0
    while True:
        page = _get(path_fmt.format(limit=PAGE, offset=offset))
        if not page:
            break
        rows += page
        if len(page) < PAGE:
            break
        offset += PAGE
    return rows


def digest(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()[:16]


def fetch_verses():
    rows = _paged(
        f"translations?select=chapter_no,verse_no,text&slug=eq.{TRANSLATION_SLUG}"
        "&limit={limit}&offset={offset}&order=chapter_no.asc,verse_no.asc"
    )
    out = []
    for r in rows:
        spoken = normalize_verse(r.get("text") or "")
        out.append({
            "chapter": r["chapter_no"],
            "verse": r["verse_no"],
            "raw": r.get("text") or "",
            "spoken": spoken,
            "hash": digest(spoken),
        })
    return out


def fetch_hadith():
    rows = _paged(
        "hadith?select=id,hadith_no,chapter_slug,sub_chapter_slug,text_az"
        "&limit={limit}&offset={offset}&order=id.asc"
    )
    out = []
    for r in rows:
        spoken = normalize_hadith(r.get("text_az") or "")
        out.append({
            "id": r["id"],
            "hadith_no": r.get("hadith_no"),
            "chapter_slug": r.get("chapter_slug"),
            "sub_chapter_slug": r.get("sub_chapter_slug"),
            "raw": r.get("text_az") or "",
            "spoken": spoken,
            "hash": digest(spoken),
        })
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--quran", action="store_true")
    ap.add_argument("--hadith", action="store_true")
    args = ap.parse_args()
    both = not (args.quran or args.hadith)

    ensure_dirs()
    data = {}
    if SOURCE_JSON.exists():
        data = json.loads(SOURCE_JSON.read_text(encoding="utf-8"))

    if both or args.quran:
        data["verses"] = fetch_verses()
        chars = sum(len(v["spoken"]) for v in data["verses"])
        print(f"ayə: {len(data['verses'])} sətir, {chars} simvol (normalizasiyadan sonra)")
    if both or args.hadith:
        data["hadith"] = fetch_hadith()
        chars = sum(len(h["spoken"]) for h in data["hadith"])
        print(f"hədis: {len(data['hadith'])} sətir, {chars} simvol (normalizasiyadan sonra)")

    SOURCE_JSON.write_text(json.dumps(data, ensure_ascii=False, indent=1), encoding="utf-8")
    print("yazıldı:", SOURCE_JSON.relative_to(BUILD.parents[1]))


if __name__ == "__main__":
    sys.exit(main())
