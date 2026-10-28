"""Haşiyə nömrələri üçün ortaq kod — make_sheet.py və import_notes.py işlədir.

Nömrə qaydası tətbiqdəki `TextAnnotator.kt` (`normalizeFootnoteMarkers` + `findFootnoteMarkers`)
ilə eynidir: sözə yapışıq 1–2 rəqəm (`parçalanma1`) və ya mötərizəli nömrə (`möminlər (1)`).
Bazada əl ilə yazılmış Unicode üst-indeks (`muttəqilər¹`) da nömrə sayılır.
"""
import json
import re
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PROVIDER = ROOT / "shared/src/commonMain/kotlin/com/cafarovceyxun/anamuslim/utils/supabase/SupabaseProvider.kt"
SURAH_NAMES = ROOT / "shared/src/commonMain/kotlin/com/cafarovceyxun/anamuslim/utils/quran/AzerbaijaniSurahNames.kt"
QURAN_DB = ROOT / "app/src/main/assets/db/quranapp.db"

_PAREN = re.compile(r"\s?\((\d{1,2})\)")
_TAG = re.compile(r"<[^>]+>")
_ANCHORS = ")!?”’\"»"
SUP = "⁰¹²³⁴⁵⁶⁷⁸⁹"


def rest_config():
    src = PROVIDER.read_text()
    url = re.search(r'SUPABASE_URL = "([^"]+)"', src).group(1)
    key = re.search(r'SUPABASE_KEY = "([^"]+)"', src).group(1)
    return url, key


def fetch_translations():
    """`translations` view-dan bütün `az` sətirləri (PostgREST 1000 sətir limiti → səhifələmə)."""
    url, key = rest_config()
    rows, offset = [], 0
    while True:
        req = urllib.request.Request(
            f"{url}/rest/v1/translations?select=id,chapter_no,verse_no,text,note&slug=eq.az&order=id.asc",
            headers={"apikey": key, "Authorization": f"Bearer {key}", "Range": f"{offset}-{offset + 999}"},
        )
        page = json.load(urllib.request.urlopen(req, timeout=60))
        rows += page
        if len(page) < 1000:
            break
        offset += 1000
    if len(rows) != 6236:
        raise SystemExit(f"Gözlənilən 6236 sətir əvəzinə {len(rows)} gəldi — dayandırıldı.")
    return rows


def surah_names():
    src = SURAH_NAMES.read_text()
    block = src[src.index("private val names"):src.index("private val meanings")]
    return {int(a): b for a, b in re.findall(r'(\d+) to "([^"]+)"', block)}


def markers(text):
    """[(nömrə, söz)] — mətn sırası ilə, hər nömrə bir dəfə."""
    t = _PAREN.sub(r"\1", _TAG.sub("", text or ""))
    found, i, n = [], 0, len(t)
    while i < n:
        c = t[i]
        if c in SUP:
            j = i
            while j < n and t[j] in SUP:
                j += 1
            num = "".join(str(SUP.index(x)) for x in t[i:j])
        elif "0" <= c <= "9":
            j = i
            while j < n and "0" <= t[j] <= "9":
                j += 1
            prev = t[i - 1] if i > 0 else " "
            nxt = t[j] if j < n else " "
            if not (j - i <= 2 and (prev.isalpha() or prev in _ANCHORS) and nxt != ":" and not nxt.isalpha()):
                i = j
                continue
            num = t[i:j]
        else:
            i += 1
            continue
        k = i
        while k > 0 and not t[k - 1].isspace():
            k -= 1
        word = t[k:i].strip("“”\"’«»,.;:()!?…")
        found.append((int(num), word))
        i = j
    seen, out = set(), []
    for num, word in found:
        if num not in seen:
            seen.add(num)
            out.append((num, word))
    return out


_NOTE_SPLIT = re.compile(r"(?m)^\s*\((\d{1,2})\)\s*[-–—]?\s*")


def parse_note(note):
    """'(1) - a\\n(2) - b' → {1: 'a', 2: 'b'}. Nömrəsiz qeyd {0: mətn} qaytarır."""
    s = (note or "").replace("\r\n", "\n").replace("\r", "\n").strip()
    if not s:
        return {}
    parts = _NOTE_SPLIT.split(s)
    out = {}
    if parts[0].strip():
        out[0] = parts[0].strip()
    for num, body in zip(parts[1::2], parts[2::2]):
        body = re.sub(r"\s*\n\s*", " ", body).strip()
        out[int(num)] = (out.get(int(num), "") + " " + body).strip()
    return out


def compose_note(pieces):
    """{1: 'a', 2: 'b'} → '(1) - a\\n(2) - b' (boş parçalar atılır)."""
    lines = []
    if pieces.get(0):
        lines.append(pieces[0])
    for num in sorted(k for k in pieces if k):
        if pieces[num].strip():
            lines.append(f"({num}) - {pieces[num].strip()}")
    return "\n".join(lines)
