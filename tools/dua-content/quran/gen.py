"""Qurandan dualar: spec.py → review.json + seed.sql + verify.sql

python3 gen.py [--print]
Ərəbcə cihazdakı quranapp.db-dən (uthmani, script_id 1), tərcümə «az» kitabından (iCloud yedəyi; yazmazdan əvvəl
serverdəki mətnlə md5 ilə tutuşdurulur). Mətnlər SQL-ə base64 kimi gedir — alət çağırışında NFC/köçürmə pozuntusu olmasın.
"""
import json, os, re, sqlite3, sys, hashlib
P = os.path.dirname(os.path.abspath(__file__))
exec(open(f'{P}/verses.py').read())
exec(open(f'{P}/spec.py').read())
B = os.path.expanduser('~/Library/Mobile Documents/com~apple~CloudDocs/anamuslim-mezmun-2026-10-03.json')
T = {(r['chapter_no'], r['verse_no']): r['text'] for r in json.load(open(B))['tables']['quran_translations_data']}
db = sqlite3.connect(f'{P}/../../../app/src/main/assets/db/quranapp.db')
# Surə adları tətbiqin özündən (AzerbaijaniSurahNames.kt) — mənbə sətri DailyContentFactory-dəki kimi «Ad ayə[-son]»
NAME = {int(n): s for n, s in re.findall(r'(\d+) to "([^"]+)"', open(f'{P}/../../../shared/src/commonMain/kotlin/com/cafarovceyxun/anamuslim/utils/quran/AzerbaijaniSurahNames.kt').read().split('meanings')[0])}
assert len(NAME) == 114
CAT = ('qurandan-dualar', 'Qurandan dualar')
NUM = re.compile(r'^[٠-٩]+$')


def words(c, v):
    w = [x for (x,) in db.execute("select text from ayah_words w join ayahs a using(ayah_id) where script_id=1 "
                                   "and surah_no=? and ayah_no=? order by word_index", (c, v))]
    w = [x.replace('۞', '').strip() for x in w]
    assert NUM.match(w[-1]), (c, v, w[-1])
    return w[:-1], w[-1]


def clean_tr(t):
    t = re.sub(r'^\s*-?\s*[\d, ]+:\s*', '', t)
    t = re.sub(r'(?<=[^\W\d_])\d+(?=[\s.,!?;:)”"]|$)', '', t)   # dipnot nömrəsi (sözə yapışıq)
    t = re.sub(r'(?<=[)!?.])\d+(?=[\s.,!?;:)”"]|$)', '', t)
    return t.strip()


def strip_edges(t):
    t = t.strip()
    t = re.sub(r'^(\.\.\.|…)+\s*', '', t); t = re.sub(r'\s*(\.\.\.|…)+$', '', t)
    t = t.strip(' ”“"')
    return t.strip()


def cut(t, a, b, where):
    i = t.find(a)
    if i < 0: raise KeyError(f'{where}: başlanğıc yox: {a!r} in {t!r}')
    j = t.find(b, i + (len(a) if a == b else 0))
    if a == b: j = i
    if j < 0: raise KeyError(f'{where}: son yox: {b!r}')
    return t[i:j + len(b)]


def build(segs):
    ar, tr = [], []
    for k, (c, v, w0, w1, trs) in enumerate(segs):
        w, num = words(c, v)
        piece = ' '.join(w[w0:(w1 + 1 if w1 is not None else None)])
        last = k == len(segs) - 1
        ar.append(piece + ('' if last or w1 is not None else ' ' + num))
        full = clean_tr(T[(c, v)])
        if trs == '': continue
        x = full if trs is None else cut(full, trs[0], trs[1], f'{c}:{v}')
        x = strip_edges(x)
        if x: tr.append(x)
    t = ' '.join(tr)
    if t.count('”') + t.count('“') == 1:   # aralığın içində qalan tək dırnaq (başqa yerdə açılıb/bağlanıb)
        t = re.sub(r'\s*[”“]\s*', ' ', t).replace(' (', ' (').strip()
    return ' '.join(ar), t


def source(segs):
    c = segs[0][0]; vs = [s[1] for s in segs]
    a, b = min(vs), max(vs)
    return c, a, (b if b > a else None), f'{NAME[c]} {a}' + (f'-{b}' if b > a else '')


rows, errors = [], []
sortno = {}
for d in D:
    sortno[d['sub']] = sortno.get(d['sub'], 0) + 10
    pieces = [d] + d.get('parts', [])
    for n, p in enumerate(pieces, 1):
        try:
            ar, tr = build(p['segs'])
            nt = None
            if n == 1 and d.get('note'):
                c, v, a, b = d['note']
                nt = strip_edges(cut(clean_tr(T[(c, v)]), a, b, f'note {c}:{v}'))
                if nt.count('”') + nt.count('“') == 1: nt = re.sub(r'\s*[”“]\s*', ' ', nt).strip()
        except KeyError as ex:
            errors.append(str(ex)); continue
        c, v, ve, src = source(p['segs'])
        rows.append(dict(sub=d['sub'], sort=sortno[d['sub']], part=n, c=c, v=v, ve=ve, ar=ar, az=tr, note=nt, source=src))
if errors:
    print('\n'.join(errors)); sys.exit(1)

if '--print' in sys.argv:
    for r in rows:
        print(f"\n[{r['sub']} {r['sort']}.{r['part']}] {r['source']}\nAR: {r['ar']}\nAZ: {r['az']}\nNOTE: {r['note']}")
json.dump(dict(cat=CAT, subs=SUBS, rows=rows), open(f'{P}/review.json', 'w'), ensure_ascii=False, indent=1)

import base64
hx = lambda s: 'null' if s is None else f"convert_from(decode('{base64.b64encode(s.encode()).decode()}','base64'),'UTF8')"
lit = lambda s: 'null' if s is None else "'" + str(s).replace("'", "''") + "'"
used = [s for s in SUBS if any(r['sub'] == s[0] for r in rows)]
sql = [f"insert into public.dua_category (slug, name, sort_no) values ({lit(CAT[0])}, {hx(CAT[1])}, 0);",
       "insert into public.dua_subcategory (slug, category_slug, name, sort_no) values\n" + ',\n'.join(
           f"({lit(s)}, {lit(CAT[0])}, {hx(n)}, {i})" for i, (s, n) in enumerate(used, 1)) + ';']
def dua_insert(rs):
    # ərəbcə NFC-də dəyişir (hərəkə sırası) → base64; azərbaycanca NFC-də sabitdir → adi literal
    V = ',\n'.join(f"({lit(r['sub'])}, {r['c']}, {r['v']}, {r['ve'] or 'null'}, {hx(r['ar'])}, {lit(r['az'])}, {lit(r['note'])}, "
                   f"{lit(r['source'])}, {r['sort']}, {r['part']})" for r in rs)
    return f"""with v(sub, c, vn, ve, ar, az, nt, src, sort, part_no) as (values
{V}
), heads as (
  insert into public.dua (category_slug, subcategory_slug, source_type, chapter_no, verse_no, verse_end, text_ar, text_az,
                          note, source, sort_no)
  select {lit(CAT[0])}, sub, 'quran', c, vn, ve, ar, az, nt, src, sort from v where part_no = 1
  returning id, subcategory_slug, sort_no
)
insert into public.dua (category_slug, subcategory_slug, source_type, chapter_no, verse_no, verse_end, text_ar, text_az,
                        note, source, sort_no, part_of_id, part_no)
select {lit(CAT[0])}, v.sub, 'quran', c, vn, ve, ar, az, nt, src, sort, h.id, v.part_no
from v join heads h on h.subcategory_slug = v.sub and h.sort_no = v.sort where v.part_no > 1;"""


# Bir alət çağırışına sığsın deyə dua sətirləri alt başlıq sərhədində ~22 KB-lıq hissələrə bölünür (seed_1.sql …);
# hissə sətirləri (part_no > 1) öz baş sətri ilə həmişə eyni alt başlıqdadır, ona görə bölgü onları ayırmır.
chunks, cur = [], []
for sub, _ in SUBS:
    rs = [r for r in rows if r['sub'] == sub]
    for r in rs:   # alt başlıq da bölünə bilər, amma hissə sətri öz baş sətrindən ayrılmır
        if cur and r['part'] == 1 and len(dua_insert(cur + [r])) > 22000:
            chunks.append(cur); cur = []
        cur.append(r)
if cur: chunks.append(cur)
for i, ch in enumerate(chunks, 1):
    open(f'{P}/seed_{i}.sql', 'w').write(('\n\n'.join(sql) + '\n\n' if i == 1 else '') + dua_insert(ch) + '\n')
print('seed hissələri:', [len(c) for c in chunks])
md = lambda x: hashlib.md5((x or '').encode()).hexdigest()
chk = ','.join(f"({lit(r['sub'])},{r['sort']},{r['part']},'{md(r['ar'])}','{md(r['az'])}','{md(r['note'])}','{md(r['source'])}')" for r in rows)
open(f'{P}/verify.sql', 'w').write(
    "select count(*) expected, count(d.id) found, count(*) filter (where md5(d.text_ar)=v.ar) ar_ok,"
    " count(*) filter (where md5(d.text_az)=v.az) az_ok, count(*) filter (where md5(coalesce(d.note,''))=v.nt) note_ok,"
    " count(*) filter (where md5(d.source)=v.src) src_ok from (values " + chk +
    ") v(sub,sort,pn,ar,az,nt,src) left join public.dua d on d.category_slug='qurandan-dualar' and d.subcategory_slug=v.sub"
    " and d.sort_no=v.sort and d.part_no=v.pn")
# Tərcümə mənbəyi: serverdəki mətn yedəkdəki ilə eynidirmi
vs = sorted({(s[0], s[1]) for d in D for p in [d] + d.get('parts', []) for s in p['segs']} |
            {(d['note'][0], d['note'][1]) for d in D if d.get('note')})
open(f'{P}/source_check.sql', 'w').write(
    "select count(*) expected, count(t.id) found, count(*) filter (where md5(t.text)=v.m) same from (values " +
    ','.join(f"({c},{v},'{md(T[(c, v)])}')" for c, v in vs) +
    ") v(c,vn,m) left join public.quran_translations_data t on t.slug='az' and t.chapter_no=v.c and t.verse_no=v.vn")
print(f"{len(rows)} sətir ({sum(r['part'] == 1 for r in rows)} dua), {len(used)} alt başlıq, {len(vs)} mənbə ayə")
