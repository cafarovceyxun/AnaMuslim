#!/usr/bin/env python3
"""Bazadakı mətni səsləndirmə mətninə çevirir.

Qaydalar uydurulmayıb — 2026-08-24-də bazadakı 6236 ayə və 1487 hədisin üzərində
sayılmış real formalardan çıxarılıb:

- Ayənin əvvəlindəki `-2: ` / `198: ` / `-3, 4: ` nömrə prefiksi (6198 ayədə var).
- Söz və dırnaqlara **bitişik haşiyə rəqəmləri** (352 hadisə: `görməyi1`, `”Rainə”1`,
  `təqvalı2`) — silinməsə TTS onları «bir», «iki» deyə oxuyur. Boşluqla ayrılan
  həqiqi rəqəmlər (`3 il ilə 9 il`) toxunulmaz qalır.
- Bitişik mötərizə şəkilçiləri (`mələk(lər)` → `mələklər`) və `həmd(-sənalar,
  təriflər)` kimi izahlar (~150 hadisə).
- Hədisdə: `221. ` nömrə prefiksi, `{...}` / `{{...}}` (Peyğəmbərin sözü — mətn
  qalır, mötərizə oxunmur, 845 hədisdə), sətir içi `(1)` haşiyə istinadları və
  `(Buxari-4981)` formalı mənbə istinadları.

`python3 tools/tts/normalize.py --report` — nümunə çıxarışı və statistika.
"""
import re
import unicodedata

# ==================== Ümumi təmizləmə ====================

_INVISIBLE = {
    " ": " ",   # NBSP — 2609 hadisə
    "​": "",    # zero-width space
    "­": "",    # soft hyphen
    "�": "",    # pozulmuş simvol
    "۩": "",    # ۩ səcdə işarəsi
    "–": "-",
    "—": "-",
    "“": '"',
    "”": '"',
    "’": "'",
    "‘": "'",
    "\t": " ",
}

# Rəqəm hərfə və ya bağlanan dırnağa **bitişikdirsə** haşiyə nişanıdır.
_FOOTNOTE_AFTER_WORD = re.compile(r'(?<=[^\W\d_])\d{1,2}(?![\d\w])', re.UNICODE)
_FOOTNOTE_AFTER_QUOTE = re.compile(r'(?<=["\')])\d{1,2}(?![\d\w])')
_FOOTNOTE_IN_PARENS = re.compile(r'\(\s*\d{1,2}\s*\)')

# `(Buxari-4981)`, `(Əhməd-7813)`, `(Məlik-790)` — mənbə istinadı, oxunmur.
_SOURCE_REF = re.compile(r'\(\s*[^()\d]{2,30}-\d{1,6}\s*\)')

_VERSE_PREFIX = re.compile(r'^\s*-?\s*\d{1,3}(?:\s*,\s*\d{1,3})*\s*:\s*')
_HADITH_PREFIX = re.compile(r'^\s*-?\s*\d{1,5}\s*[.)]\s*')

# `mələk(lər)` kimi bitişik şəkilçi: qısa, tamamı kiçik hərf.
_GLUED_SUFFIX = re.compile(r'(?<=\w)\((\w{1,6})\)')
# `həmd(-sənalar, təriflər)` — tire ilə başlayan izah.
_GLUED_APPOSITION = re.compile(r'(?<=\w)\(-\s*([^()]{1,80})\)')
_PARENS = re.compile(r'\(([^()]{1,200})\)')

_QUOTE_HYPHEN = re.compile(r'(["\'])-(?=\w)')
_PUNCT_HYPHEN = re.compile(r'(?<=[.!?,;])-(?=\w)')
_SPACE_BEFORE_PUNCT = re.compile(r'\s+([,.;:!?])')
_MULTI_SPACE = re.compile(r'[  - ]{2,}')
_MULTI_NEWLINE = re.compile(r'\n{2,}')
_EMPTY_PARENS = re.compile(r'\(\s*\)')


def _pre(text: str) -> str:
    text = unicodedata.normalize("NFC", text or "")
    for src, dst in _INVISIBLE.items():
        text = text.replace(src, dst)
    return text.replace("\r\n", "\n").replace("\r", "\n")


def _strip_footnotes(text: str) -> str:
    text = _FOOTNOTE_IN_PARENS.sub("", text)
    text = _FOOTNOTE_AFTER_WORD.sub("", text)
    return _FOOTNOTE_AFTER_QUOTE.sub("", text)


def _unwrap_parens(text: str) -> str:
    """Mötərizələri açır: şəkilçi bitişir, izah vergüllə ayrılır."""
    text = _GLUED_APPOSITION.sub(lambda m: ", " + m.group(1).strip(), text)
    text = _GLUED_SUFFIX.sub(lambda m: m.group(1), text)

    def _plain(m):
        # Mötərizə sadəcə açılır: süni vergül qoymaq TTS-də yersiz pauza yaradır
        # («…borclu kimsəyə, sədəqə etməyiniz» kimi), mətnin öz durğu işarələri kifayətdir.
        return m.group(1).strip(" ,;")

    return _PARENS.sub(_plain, text)


def _finish(text: str) -> str:
    text = _QUOTE_HYPHEN.sub(r"\1, ", text)
    text = _PUNCT_HYPHEN.sub(", ", text)
    text = _EMPTY_PARENS.sub("", text)
    text = _MULTI_SPACE.sub(" ", text)
    text = _SPACE_BEFORE_PUNCT.sub(r"\1", text)
    text = re.sub(r",\s*,", ",", text)
    text = re.sub(r"\s+\n", "\n", text)
    text = _MULTI_NEWLINE.sub("\n", text)
    text = "\n".join(line.strip(" -") for line in text.split("\n"))
    text = text.strip()
    if text and text[-1] not in ".!?\"'":
        text += "."
    return text


# ==================== Girişlər ====================

def normalize_verse(raw: str) -> str:
    """Ayə mətni → oxunacaq mətn. Boş ayə (birləşmiş ayələr) boş qayıdır."""
    text = _VERSE_PREFIX.sub("", _pre(raw))
    if not text.strip():
        return ""
    return _finish(_unwrap_parens(_strip_footnotes(text)))


def normalize_hadith(raw: str) -> str:
    """Hədis mətni → oxunacaq mətn (mənbə istinadları və `{}` işarələri atılır)."""
    text = _pre(raw)
    text = _SOURCE_REF.sub("", text)
    text = text.replace("{{", "").replace("}}", "").replace("{", "").replace("}", "")
    text = _HADITH_PREFIX.sub("", text)
    if not text.strip():
        return ""
    return _finish(_unwrap_parens(_strip_footnotes(text)))


# ==================== Sorğu üçün parçalama ====================

_SENTENCE_END = re.compile(r'(?<=[.!?])\s+')


def split_for_request(text: str, max_bytes: int):
    """Mətni sorğu limitindən kiçik, cümlə sərhədində parçalara bölür."""
    parts, buf = [], ""

    def flush():
        nonlocal buf
        if buf.strip():
            parts.append(buf.strip())
        buf = ""

    for block in text.split("\n"):
        for sentence in _SENTENCE_END.split(block):
            if not sentence.strip():
                continue
            candidate = (buf + " " + sentence).strip() if buf else sentence
            if len(candidate.encode("utf-8")) <= max_bytes:
                buf = candidate
                continue
            flush()
            if len(sentence.encode("utf-8")) <= max_bytes:
                buf = sentence
            else:
                # Cümlə tək başına limitdən böyükdür — vergüldən böl.
                chunk = ""
                for piece in re.split(r'(?<=,)\s+', sentence):
                    cand = (chunk + " " + piece).strip() if chunk else piece
                    if len(cand.encode("utf-8")) <= max_bytes:
                        chunk = cand
                    else:
                        if chunk:
                            parts.append(chunk)
                        chunk = piece
                if chunk:
                    parts.append(chunk)
        flush()
    flush()
    return parts or ([text] if text.strip() else [])


# ==================== Hesabat ====================

def _report():
    import json
    import fetch  # dövri idxal deyil: fetch normalize-i idxal edir, əksi yalnız burada

    rows = fetch._paged(
        f"translations?select=chapter_no,verse_no,text&slug=eq.{fetch.TRANSLATION_SLUG}"
        "&limit={limit}&offset={offset}&order=chapter_no.asc,verse_no.asc"
    )
    hadith = fetch._paged(
        "hadith?select=id,text_az&limit={limit}&offset={offset}&order=id.asc"
    )

    v_before = sum(len(r.get("text") or "") for r in rows)
    v_after = sum(len(normalize_verse(r.get("text") or "")) for r in rows)
    h_before = sum(len(r.get("text_az") or "") for r in hadith)
    h_after = sum(len(normalize_hadith(r.get("text_az") or "")) for r in hadith)
    empty = [(r["chapter_no"], r["verse_no"]) for r in rows if not normalize_verse(r.get("text") or "")]

    print(f"ayə   : {v_before} → {v_after} simvol ({len(rows)} sətir, {len(empty)} boş)")
    print(f"hədis : {h_before} → {h_after} simvol ({len(hadith)} sətir)")
    chunks = [len(split_for_request(normalize_hadith(r.get("text_az") or ""), 1500)) for r in hadith]
    print(f"hədis sorğu parçası: cəmi {sum(chunks)}, ən çox {max(chunks)} parça bir hədisdə")
    print(f"boş ayələr (birləşmiş ayələr): {empty}")

    print("\n--- nümunələr ---")
    for r in [rows[1], rows[286], rows[6000]]:
        print(f"\n[{r['chapter_no']}:{r['verse_no']}]")
        print("  əvvəl:", json.dumps((r.get('text') or '')[:200], ensure_ascii=False))
        print("  sonra:", json.dumps(normalize_verse(r.get('text') or '')[:200], ensure_ascii=False))
    for r in [hadith[1], hadith[404]]:
        print(f"\n[hədis {r['id']}]")
        print("  əvvəl:", json.dumps((r.get('text_az') or '')[:220], ensure_ascii=False))
        print("  sonra:", json.dumps(normalize_hadith(r.get('text_az') or '')[:220], ensure_ascii=False))


if __name__ == "__main__":
    import sys
    if "--report" in sys.argv:
        _report()
    else:
        print(__doc__)
