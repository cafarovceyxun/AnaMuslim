"""Müəllifin ÖZ mötərizələrini mötərizə qaydası ilə yoxlayır (build.py check-parens yalnız təklifləri görür).

    python3 author_parens.py <az.json> [--list] [--sentences]

Qayda (README → «Mötərizə qaydası»): mətn həm mötərizə ilə (işarələr atılır), həm də mötərizəsiz
(məzmunla birlikdə atılır) düzgün cümlə olmalıdır. Skript yalnız formal izləri sayır — məna qırılması
gözlə yoxlanır (`--sentences` mötərizəsi olan hər cümləni iki oxunuşda çap edir). Kiçik hərflə başlama
(`(Bilin ki,) kim…`) müəllif üslubudur: ayrıca sayılır, xəta siyahısına düşmür.
Ərəbcə cihazdakı `quranapp.db`-dən (Osmani, script_id = 1) oxunur.
"""
import collections
import json
import os
import re
import sqlite3
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
DB = os.path.join(HERE, '..', '..', 'app', 'src', 'main', 'assets', 'db', 'quranapp.db')

INNER = re.compile(r'\([^()]*\)')
PREFIX = re.compile(r'^\W*\d+(?:\s*,\s*\d+)*\s*[:.]\s*')
LOWER = 'a-zəöüğıişç'
HARAKAT = re.compile('[ً-ٰٟۖ-ۭ]')


def bare(t):
    """Mötərizəsiz oxunuş: mötərizə məzmunu ilə birlikdə atılır (iç-içə olanlar içdən çölə)."""
    prev = None
    while prev != t:
        prev, t = t, re.sub(r'\s*\([^()]*\)', '', t)
    return re.sub(r'\s{2,}', ' ', t)


def full(t):
    """Mötərizəli oxunuş: yalnız işarələr atılır."""
    return re.sub(r'\s{2,}', ' ', re.sub(r'[()]', '', t))


def arabic():
    con = sqlite3.connect(DB)
    rows = con.execute(
        'select a.surah_no, a.ayah_no, w.text from ayahs a join ayah_words w on w.ayah_id = a.ayah_id '
        'where w.script_id = 1 order by a.ayah_id, w.word_index')
    out = collections.defaultdict(list)
    for s, a, w in rows:
        out[f'{s}:{a}'].append(w)
    return {k: ' '.join(v) for k, v in out.items()}


def plain(ar):
    return HARAKAT.sub('', ar).replace('ٱ', 'ا')


# Formal izlər — hər biri yalnız mötərizəsiz oxunuşda ARTANDA sayılır (mətnin özündəki durğu sayılmır).
TRACES = {
    'qalıq durğu': r'[,;:]\s*[,.;!?]|\s[,.;!?]|^\s*[,;]|[.!?]\s*,',
    'asılı «ki»': r',\s*ki\s*[,.;:!?”]|\bki\s*[.;!?]|^\s*ki\b',
    'boş cümlə': r'[.!?:]\s*[.!?]|”\s*”',
}
STYLE_LOWER = re.compile(rf'(?:^|[.!?]\s+|[:.!?]\s*”\s*)[{LOWER}]')
# Danışma feili mötərizədədir, iki nöqtə isə kənarda: «bilmişdilər. (Biz onlara belə dedik) : ”…”» →
# mötərizəsiz «bilmişdilər. : ”…”». Düzəlişi: iki nöqtəni mötərizənin içinə al — «(Biz onlara belə dedik:) ”…”».
ORPHAN_COLON = re.compile(r'(?:^|[.!?…,;”])\s*\([^()]*\)\s*:')
# Ahəng: mötərizəyə yapışıq «da/də» mötərizədəki son sözə uyğunlaşıb, mötərizəsiz oxunuşda əvvəlki sözə uymur
# («evli olanlar(la evlənmək) də» → «olanlar də»).
BACK, FRONT = set('aıouAIOU'), set('eəiöüEƏİÖÜ')


def harmony_breaks(s):
    n = 0
    for w, p in re.findall(r'([^\W\d_]+)\s+(da|də)(?![’\w])', s):
        vowels = [c for c in w if c in BACK or c in FRONT]
        if vowels and ((vowels[-1] in BACK) != (p == 'da')):
            n += 1
    return n
PRONOUN = re.compile(r'^(?:siz|onlar|biz|sən|mən|o|O|Siz|Onlar|Biz|Sən|Mən)$')
FA = re.compile(r'^(?:[Ee]lə isə|[Oo] zaman|[Bb]una görə də|[Ss]onra|[Dd]ərhal)$')
RELATIVE = re.compile(r'\bki,?$')


def main():
    src = sys.argv[1]
    data = json.load(open(src))
    ar = arabic()
    counts = collections.Counter()
    groups_total = 0
    hits = collections.defaultdict(list)
    sentences = []
    for v in data:
        ref = f"{v['chapter_no']}:{v['verse_no']}"
        t = PREFIX.sub('', v['text'].replace('\xa0', ' ').replace('​', '')).strip()
        groups = INNER.findall(t)
        if not groups:
            continue
        groups_total += len(re.findall(r'\(', t))
        counts['ayə (mötərizəli)'] += 1
        if t.count('(') != t.count(')'):
            hits['balanssız (§3-də var)'].append((ref, t))
            continue
        b, f = bare(t), full(t)
        for name, pat in TRACES.items():
            if len(re.findall(pat, b)) > len(re.findall(pat, f)):
                hits[name].append((ref, b))
        if len(STYLE_LOWER.findall(b)) > len(STYLE_LOWER.findall(f)):
            counts['üslub: kiçik hərflə başlama'] += 1
        if ORPHAN_COLON.search(t):
            hits['yetim iki nöqtə'].append((ref, b))
        if harmony_breaks(b) > harmony_breaks(f):
            hits['ahəng (da/də)'].append((ref, b))
        for m in re.finditer(r'\(([^()]*)\)', t):
            g = m.group(1).strip()
            before, after = t[m.start() - 1:m.start()], t[m.end():m.end() + 1]
            if before.isalpha() or after.isalpha():
                hits['sözə yapışıq'].append((ref, m.group(0)))
            if PRONOUN.match(g):
                counts['ərəbcədə: feildəki əvəzlik'] += 1
            elif FA.match(g) and re.search(r'(?:^|\s)فَ', ar.get(ref, '')):
                counts['ərəbcədə: fa (فَ) bağlayıcısı'] += 1
            elif RELATIVE.search(g) and re.search(r'ٱلَّذِي|ٱلَّذِينَ|ٱلَّتِي|ٱلَّٰتِي|مَن\b', ar.get(ref, '')):
                counts['ərəbcədə: الذي/من → «o … ki»'] += 1
            if re.search(r'\b(?:deyil|yox)\b', g):
                hits['mötərizədə inkar'].append((ref, m.group(0)))
        sentences.append((ref, f, b))

    print(f'{counts["ayə (mötərizəli)"]} ayədə {groups_total} mötərizə\n')
    for k in sorted(counts):
        if k != 'ayə (mötərizəli)':
            print(f'{counts[k]:5}  {k}')
    for k, v in hits.items():
        print(f'{len(v):5}  {k} ({len({r for r, _ in v})} ayə)')
    if '--list' in sys.argv:
        for k, v in hits.items():
            print(f'\n## {k}')
            for ref, s in v:
                print(f'{ref}\t{s}')
    if '--sentences' in sys.argv:
        for ref, f, b in sentences:
            print(f'{ref}\n  + {f}\n  - {b}')


if __name__ == '__main__':
    main()
