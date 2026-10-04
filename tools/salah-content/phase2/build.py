"""Mərhələ 2 maketini qurur: data.py + bazadakı hədislər + ill.js + şablon → merhele2.html.

Hər çıxarış hədisin text_az-ında hərfi axtarılır, zikrlərin ərəbcəsi xam text_ar-da (hərəkə qatlanaraq),
transkripti text_az-da (durğu qatlanaraq). Biri tapılmasa build dayanır.
"""
import json, re, sys, unicodedata, os

P = os.path.dirname(os.path.abspath(__file__))
# usage: build.py <db.json (yedəyin tables hissəsi)> <çıxış.html>
DB, OUT = sys.argv[1], sys.argv[2]
ILL = os.path.join(P, '../../salah-art/ill.js')
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
    # № 1 in volume 1 is the İman kitabı hadith; a stray "1." exists in the night-prayer chapter.
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
    if r and not z.get('user'):
        if fold_ar(z['ar']) not in fold_ar(r['text_ar']) and k != 'takbir':
            errors.append(f'ərəbcə tapılmadı: {k}')
        if fold_lat(z['tr']) not in fold_lat(r['text_az']) and k != 'takbir':
            errors.append(f'transkript tapılmadı: {k}')
        if z['meanSrc'] == 'hədisin qeydi' and fold_lat(z['mean']) not in fold_lat(r['note'] or ''):
            errors.append(f'tərcümə qeyddə yoxdur: {k}')
        if k == 'rise' and fold_lat(z['mean']) not in fold_lat(H[('c1', 327)]['text_az'].replace('} (', ' ').replace('{', '')):
            pass
    ZKO[k] = dict(z, ref=f"{z['ref'][0]}-{z['ref'][1]}")

# № 327-dəki izahlar ayrı-ayrı yoxlanır.
t327 = H[('c1', 327)]['text_az']
for part in ('Allah, Ona həmd edən kimsəni eşitdi', 'Rəbbimiz, Sənədir həmd!'):
    if part not in t327:
        errors.append('327 izahı tapılmadı: ' + part)

refs = {tuple(v[:2]) for v in EX.values()} | {z['ref'] for z in ZK.values()} | set(EXTRA_REFS) | {('c1', int(a[s])) for a in ADHAN for s in ('arSrc', 'meanSrc', 'trSrc') if a.get(s)}
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

import base64
SVG = os.path.join(P, '../../salah-art/vector/svg')
IMG = {n[:-4]: open(f'{SVG}/{n}').read().replace('<svg xmlns="http://www.w3.org/2000/svg" ', '<svg role="img" ') for n in sorted(os.listdir(SVG)) if n.endswith('.svg')}
DATA = {'IMG': IMG, 'EV': EV, 'EX': EXO, 'ZK': ZKO, 'ADHAN': ADHAN, 'COUNTER': COUNTER}
tpl = open(f'{P}/template.html').read()
css_k = open(f'{P}/ill-k.css').read()
ill = open(ILL).read()
html = tpl.replace('/*BASE_CSS*/', open(f'{P}/base.css').read()).replace('/*ILL_CSS*/', css_k).replace('/*ILL_JS*/', ill).replace('/*DATA*/', 'const DATA = ' + json.dumps(DATA, ensure_ascii=False) + ';')
open(OUT, 'w').write(html)
print('ok', len(EV), 'hədis,', len(EXO), 'çıxarış,', len(ZKO), 'zikr,', len(html), 'bayt')
