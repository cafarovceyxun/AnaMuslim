"""Dua bölməsinin dolğusu: Nəbinin (s.a.s.) dua və zikrləri, Muheymin hədislərindən.

python3 gen.py <yedək.json> [--extra] [--print]      (iCloud: anamuslim-mezmun-*.json)
Çıxış (cari qovluğa): dua_seed.sql, dua_verify.sql, review.json

Üsul Həcc bələdçisi ilə eynidir: ərəbcə, oxunuş və tərcümə əl ilə yazılmır — hədisin öz mətnindən başlanğıc/son
ifadə ilə kəsilir, SQL-də yalnız mövqe gedir, server substr ilə kəsir, sonra md5 ilə yoxlanır.
"""
import json, re, sys, hashlib, unicodedata

PRINT = '--print' in sys.argv
import os
P = os.path.dirname(os.path.abspath(__file__))
_src = json.load(open(next(x for x in sys.argv[1:] if not x.startswith('--'))))
H = {r['id']: r for r in _src.get('tables', _src)['hadith']}
exec(open(f'{P}/data_zikr.py').read())
exec(open(f'{P}/data_rest.py').read())
exec(open(f'{P}/data_extra.py').read())
# --extra: ikinci dalğa (data_extra.py); onsuz birinci dalğa — ikisi ayrı-ayrı tətbiq olunub
ROWS = EXTRA if '--extra' in sys.argv else ZIKR + REST

HARAKA = re.compile('[ً-ْٰـ­]')


def fold(ch):
    c = unicodedata.normalize('NFKC', ch)
    c = HARAKA.sub('', c)
    c = re.sub('[أإآٱ]', 'ا', c).replace('ى', 'ي').replace('ة', 'ه')
    c = re.sub(r'[\s،؛,.:!?"«»()\-ـ]+', ' ', c)
    return c


def folded(raw):
    out, idx = [], []
    for i, ch in enumerate(raw):
        for c in fold(ch):
            if c == ' ' and out and out[-1] == ' ':
                continue
            out.append(c); idx.append(i)
    return ''.join(out), idx


STRIP = set(' \n\t{}"«»“”')


def trim(raw, a, b):
    while a < b and raw[a] in STRIP: a += 1
    while b > a and raw[b - 1] in STRIP: b -= 1
    return a, b


def cut_ar(raw, s, e=None, sa=None, ea=None):
    f, idx = folded(raw)
    F = lambda x: ''.join(fold(c) for c in x)
    base = 0
    if sa:
        k = f.find(F(sa))
        if k < 0: raise KeyError(f'ar sa yox: {sa}')
        base = k + len(F(sa))
    i = f.find(F(s), base)
    if i < 0: raise KeyError(f'ar start yox: {s}')
    if e is None:
        j_end = i + len(F(s))
    else:
        eb = i + len(F(s))
        if ea:
            k = f.find(F(ea), i)
            if k < 0: raise KeyError(f'ar ea yox: {ea}')
            eb = k + len(F(ea))
        j = f.find(F(e), eb if F(e) != F(s) else i + len(F(s)))
        if j < 0:
            j = f.find(F(e), i)  # son başlanğıcın içindədirsə
        if j < 0: raise KeyError(f'ar end yox: {e}')
        j_end = j + len(F(e))
    a, b = idx[i], idx[j_end - 1] + 1
    while b < len(raw) and HARAKA.match(raw[b]): b += 1
    return trim(raw, a, b)


def cut_az(text, s, e=None, sa=None):
    base = 0
    if sa:
        k = text.find(sa)
        if k < 0: raise KeyError(f'az sa yox: {sa}')
        base = k
    i = text.find(s, base)
    if i < 0: raise KeyError(f'az start yox: {s!r}')
    if e is None:
        b = i + len(s)
    else:
        j = text.find(e, i + 1 if e == s else i)
        if e == s:
            j = text.find(e, i + len(s))
        if j < 0: raise KeyError(f'az end yox: {e!r}')
        b = j + len(e)
    return trim(text, i, b)


def spec_ar(h, sp):
    if sp[0] == 'LIT': return ('lit', sp[1])
    s, e, *rest = sp + (None, None)
    a, b = cut_ar(h['text_ar'], s, e, rest[0] if rest else None, rest[1] if len(rest) > 1 else None)
    return ('pos', a, b)


def spec_tr(h, sp):
    if sp is None: return None
    if sp[0] == 'LIT': return ('lit', sp[1])
    s, e, sa = (list(sp) + [None, None])[:3]
    a, b = cut_az(h['text_az'], s, e, sa)
    return ('pos', a, b)


def spec_az(h, sp):
    if sp[0] == 'LIT': return ('lit', sp[1])
    field, s, e, sa, hid = (list(sp) + [None, None, None])[:5]
    src = H[hid] if hid else h
    txt = src[field] or ''
    a, b = cut_az(txt, s, e, sa)
    return ('pos', a, b, field, hid or h['id'])


def lit(x):
    return 'null' if x is None else "'" + str(x).replace("'", "''") + "'"


errors, out_rows, review = [], [], []


def build(r, head):
    h = H[r['hid'] if head else head_hid]
    ar = spec_ar(h, r['ar']); tr = spec_tr(h, r.get('tr')); az = spec_az(h, r['az'])
    nt = spec_az(h, r['note']) if r.get('note') else None
    return h, ar, tr, az, nt


def text_of(h, sp, field='text_ar'):
    if sp is None: return None
    if sp[0] == 'lit': return sp[1]
    if len(sp) == 3: return h[field][sp[1]:sp[2]]
    src = H[sp[4]]
    return src[sp[3]][sp[1]:sp[2]]


for r in ROWS:
    head_hid = r['hid']
    pieces = [(r, 1)] + [(p, n + 2) for n, p in enumerate(r.get('parts', []))]
    for p, part_no in pieces:
        try:
            h, ar, tr, az, nt = build(p, True) if part_no == 1 else build(p, False)
        except (KeyError, IndexError) as ex:
            errors.append(f"{r['cat']}/{r['sort']} id{r['hid']} p{part_no}: {ex}"); continue
        T_ar = text_of(h, ar); T_tr = text_of(h, tr, 'text_az'); T_az = text_of(h, az); T_nt = text_of(h, nt)
        for lab, t in (('ar', T_ar), ('tr', T_tr), ('az', T_az), ('note', T_nt)):
            if t and ('{{' in t or '}}' in t):
                errors.append(f"{r['cat']}/{r['sort']} id{r['hid']}: {lab} mötərizə: {t[:60]}")
        if T_ar and len(T_ar) > 2500:
            errors.append(f"{r['cat']}/{r['sort']} id{r['hid']}: ar çox uzun {len(T_ar)}")
        no = re.match(r'[\s_-]*(\d+)', H[r['hid']]['text_az']); no = no[1] if no else '?'
        review.append(dict(cat=r['cat'], sub=r.get('sub'), sort=r['sort'], part=part_no, hid=r['hid'], no=no,
                           ar=T_ar, tr=T_tr, az=T_az, note=T_nt, rep=p.get('rep'), source=H[r['hid']]['source']))
        out_rows.append((r, part_no, ar, tr, az, nt, p.get('rep'), dict(ar=T_ar, tr=T_tr, az=T_az, note=T_nt)))
        if PRINT:
            print(f"\n### {r['cat']} / {r.get('sub')} / {r['sort']}.{part_no} — id {r['hid']} (№ {no})"
                  f"\nAR: {T_ar}\nTR: {T_tr}\nAZ: {T_az}\nNOTE: {T_nt}\nREP: {p.get('rep')}")

if errors:
    print('\n'.join(errors)); sys.exit(1)

json.dump(review, open('review.json', 'w'), ensure_ascii=False, indent=1)


def vals(r, part_no, ar, tr, az, nt, rep):
    def pos(sp):
        if sp is None: return ('null', 'null', 'null')
        if sp[0] == 'lit': return ('null', 'null', lit(sp[1]))
        return (str(sp[1] + 1), str(sp[2] - sp[1]), 'null')
    a = pos(ar); t = pos(tr)
    if az[0] == 'lit': z = ('null', 'null', 'null', 'null', lit(az[1]))
    else: z = (lit(az[3]), str(az[4]), str(az[1] + 1), str(az[2] - az[1]), 'null')
    if nt is None: n = ('null', 'null', 'null', 'null')
    else: n = (lit(nt[3]), str(nt[4]), str(nt[1] + 1), str(nt[2] - nt[1]))
    return [lit(r['cat']), lit(r.get('sub')), str(r['hid']), *a, *t, *z, *n, str(rep) if rep else 'null',
            str(r['sort']), str(part_no)]


V = ',\n'.join('(' + ', '.join(vals(r, pn, ar, tr, az, nt, rep)) + ')' for r, pn, ar, tr, az, nt, rep, _ in out_rows)
COLS = ('cat, sub, hid, ar_s, ar_n, ar_lit, tr_s, tr_n, tr_lit, az_f, az_h, az_s, az_n, az_lit, '
        'nt_f, nt_h, nt_s, nt_n, rep, sort, part_no')
SEL = """select v.cat, v.sub, 'hadith', h.id,
       coalesce(v.ar_lit, substr(h.text_ar, v.ar_s, v.ar_n)),
       coalesce(v.az_lit, case v.az_f when 'note' then substr(za.note, v.az_s, v.az_n)
                                       when 'text_az' then substr(za.text_az, v.az_s, v.az_n) end, ''),
       coalesce(v.tr_lit, substr(h.text_az, v.tr_s, v.tr_n)),
       case v.nt_f when 'note' then substr(zn.note, v.nt_s, v.nt_n) when 'text_az' then substr(zn.text_az, v.nt_s, v.nt_n) end,
       case when char_length(h.source) <= 300 then h.source
            else btrim(ltrim(btrim(split_part(h.source, E'\\n', 2)), '-')) end, v.rep::int, v.sort"""
sql = f"""with v({COLS}) as (values
{V}
), heads as (
  insert into public.dua (category_slug, subcategory_slug, source_type, hadith_id, text_ar, text_az, transliteration,
                          note, source, repeat_count, sort_no)
  {SEL}
  from v join public.hadith h on h.id = v.hid
  left join public.hadith za on za.id = v.az_h left join public.hadith zn on zn.id = v.nt_h
  where v.part_no = 1
  returning id, category_slug, hadith_id, sort_no
)
insert into public.dua (category_slug, subcategory_slug, source_type, hadith_id, text_ar, text_az, transliteration,
                        note, source, repeat_count, sort_no, part_of_id, part_no)
{SEL}, hd.id, v.part_no
from v join public.hadith h on h.id = v.hid
left join public.hadith za on za.id = v.az_h left join public.hadith zn on zn.id = v.nt_h
join heads hd on hd.category_slug = v.cat and hd.hadith_id = v.hid and hd.sort_no = v.sort
where v.part_no > 1;
"""
open('dua_seed.sql', 'w').write(sql)
md = lambda x: hashlib.md5((x or '').encode()).hexdigest()
chk = ','.join(f"({lit(r['cat'])},{r['hid']},{r['sort']},{pn},'{md(T['ar'])}','{md(T['az'])}','{md(T['tr'])}','{md(T['note'])}')"
               for r, pn, *_, T in out_rows)
open('dua_verify.sql', 'w').write(
    "select count(*) expected, count(d.id) found, count(*) filter (where md5(d.text_ar)=v.ar) ar_ok,"
    " count(*) filter (where md5(d.text_az)=v.az) az_ok, count(*) filter (where md5(coalesce(d.transliteration,''))=v.tr) tr_ok,"
    " count(*) filter (where md5(coalesce(d.note,''))=v.nt) note_ok from (values " + chk +
    ") v(cat,hid,sort,pn,ar,az,tr,nt) left join public.dua d on d.category_slug=v.cat and d.hadith_id=v.hid"
    " and d.sort_no=v.sort and d.part_no=v.pn")
print(f'{len(out_rows)} sətir ({sum(1 for x in out_rows if x[1]==1)} dua)')
