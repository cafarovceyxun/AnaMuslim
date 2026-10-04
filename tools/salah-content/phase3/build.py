"""Mərhələ 3 maketini qurur: data.py + bazadakı hədislər + şablon → merhele3.html.

Hər çıxarış hədisin text_az-ında hərfi axtarılır, zikrlərin ərəbcəsi xam text_ar-da (hərəkə qatlanaraq),
oxunuşu text_az-da (durğu qatlanaraq; «user» olanlar təklifdir və yoxlanmır). Biri tapılmasa build dayanır.
"""
import json, re, sys, unicodedata, os

P = os.path.dirname(os.path.abspath(__file__))
# usage: build.py <db.json (yedəyin tables hissəsi)> <çıxış.html>
DB, OUT = sys.argv[1], sys.argv[2]
exec(open(f'{P}/data.py').read())

t = json.load(open(DB))
sub = {s['slug']: s for s in t['hadith_sub_chapter']}
chap = {c['slug']: c for c in t['hadith_chapter']}
book = {b['slug']: b for b in t['hadith_book']}
H = {}
for r in t['hadith']:
    m = re.match(r'[\s_]*(\d+)\.', r['text_az'] or '')
    if not m:
        continue
    key = (r['chapter_slug'][:2], int(m[1]))
    if key == ('c1', 1) and not r['chapter_slug'].startswith('c1im'):
        continue
    H.setdefault(key, r)

def ws(s):
    return re.sub(r'\s+', ' ', s).strip()

HAR = re.compile('[ً-ْٰـۡ]')
def fold_ar(s):
    s = unicodedata.normalize('NFKC', s)
    s = HAR.sub('', s)
    s = s.translate(str.maketrans('أإآٱىة', 'اااايه'))
    return re.sub(r'[^ء-ي]', '', s)

def fold_lat(s):
    return re.sub(r'[^a-zəığüöşçA-ZƏIĞÜÖŞÇ]', '', s).lower()

errors = []
def need(key):
    if key not in H:
        errors.append(f'hədis yoxdur: {key}')
        return None
    return H[key]

EXO = {}
for k, (vol, no, text) in EX.items():
    r = need((vol, no))
    if r and ws(text) not in ws(r['text_az']):
        errors.append(f'çıxarış hərfi deyil: {k} № {no}: {text[:60]}')
    EXO[k] = {'ref': f'{vol}-{no}', 'text': text}

ZKO = {}
for k, z in ZK.items():
    r = need(z['ref'])
    if r:
        if fold_ar(z['ar']) not in fold_ar(r['text_ar']):
            errors.append(f'ərəbcə tapılmadı: {k}')
        if not z.get('user') and fold_lat(z['tr']) not in fold_lat(r['text_az']):
            errors.append(f'oxunuş tapılmadı: {k}')
        src = {'hədisin qeydi': r['note'] or '', 'hədisin mətni': r['text_az'], '№ 327-nin izahı': H[('c1', 327)]['text_az']}.get(z['meanSrc'])
        if src is not None and z['mean'] and fold_lat(z['mean'].replace('Allahummə, ', '')) not in fold_lat(src):
            errors.append(f'tərcümə mənbədə yoxdur: {k}')
    ZKO[k] = dict(z, ref=f"{z['ref'][0]}-{z['ref'][1]}")

refs = {tuple(v[:2]) for v in EX.values()} | {z['ref'] for z in ZK.values()} | set(EXTRA_REFS)
EV = {}
for key in sorted(refs, key=lambda k: (k[0], k[1])):
    r = need(key)
    if not r:
        continue
    az = r['text_az']
    main = re.split(r'Digər bir rəvayətdə', az)[0]
    body = main.split('\n', 1)[1] if '\n' in main else main
    srcm = re.findall(r'\(([^()]*-\s*[\d,-]+[^()]*)\)\.?\s*$', body.strip())
    body = re.sub(r'\n\s*\([^()]*\d[^()]*\)\.?\s*$', '', body.strip())
    s = r['sub_chapter_slug']
    bab = (sub.get(s) or {}).get('name') or (chap.get(r['chapter_slug']) or {}).get('name') or ''
    bk = (book.get((chap.get(r['chapter_slug']) or {}).get('book_slug')) or {}).get('name', '')
    EV[f'{key[0]}-{key[1]}'] = {'id': r['id'], 'no': key[1], 'vol': key[0], 'book': bk, 'bab': bab.strip(),
                               'q': ws(body)[:1400], 'src': srcm[0] if srcm else ''}

if errors:
    print('\n'.join(errors))
    sys.exit(1)

DATA = {'EV': EV, 'EX': EXO, 'ZK': ZKO}
tpl = open(f'{P}/template.html').read()
html = tpl.replace('/*BASE_CSS*/', open(f'{P}/../phase2/base.css').read()).replace('/*ILL_CSS*/', '') \
          .replace('/*DATA*/', 'const DATA = ' + json.dumps(DATA, ensure_ascii=False) + ';')
open(OUT, 'w').write(html)
print('ok', len(EV), 'hədis,', len(EXO), 'çıxarış,', len(ZKO), 'zikr,', len(html), 'bayt')
