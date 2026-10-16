#!/usr/bin/env python3
"""Azərbaycanca sözbəsöz (WBW) — cədvəl `quran_wbw_az` → paket `wbw_az.json.gz` → öz storage-ımız.

    python3 publish.py assemble <out-qovluğu> <az.json>  # agent çıxışı → data/glosses.json.gz (yoxlanmış)
    python3 publish.py stale <qovluq>                     # 'stale' ayələr → <qovluq>/in/stale.txt (GUIDE.md formatı)
    python3 publish.py merge <out-qovluğu>               # yenilənmiş ayələri data/glosses.json.gz-ə qatır
    python3 publish.py load                              # data/glosses.json.gz → quran_wbw_az (upsert)
    python3 publish.py pack                              # quran_wbw_az → build/wbw_az.json.gz (+ versiya)
    python3 publish.py upload                            # build/* → storage `wbw` bucket-i (public)

Bax README.md. Server: `ssh -i ~/.ssh/oracle_cafarovceyxun ubuntu@130.61.171.105`, baza `supabase-db`
konteynerində. Sirr (service_role açarı) yalnız serverdə oxunur və heç vaxt çap olunmur.
"""
import collections
import gzip
import hashlib
import json
import os
import re
import sqlite3
import subprocess
import sys
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
DATA = os.path.join(HERE, 'data')
BUILD = os.path.join(HERE, 'build')
DB = os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'db', 'quranapp.db')
SSH = ['ssh', '-i', os.path.expanduser('~/.ssh/oracle_cafarovceyxun'), 'ubuntu@130.61.171.105']
PSQL = 'sudo docker exec -i supabase-db psql -U postgres -d postgres -v ON_ERROR_STOP=1 -X -q'
EN_PACK = 'https://raw.githubusercontent.com/AlfaazPlus/QuranAppInventory/master/wbw_v2/wbw_en.json.gz'
PUBLIC = 'https://anamuslim.cafarovceyxun.com/storage/v1/object/public/wbw/'
PACK_ID = 'wbw_az'
MANIFEST = 'available_wbw_info_own.json'

BAD = re.compile(r"[؀-ۿ]|[âîûêôÂÎÛ]|\w'\w")


def arabic_words():
    con = sqlite3.connect(f'file:{DB}?mode=ro', uri=True)
    out = collections.defaultdict(dict)
    for aid, wi, t in con.execute('select ayah_id, word_index, text from ayah_words where script_id = 1'):
        out[aid][wi] = t
    return out


def psql(sql, capture=False):
    r = subprocess.run(SSH + [PSQL + (' -A -t' if capture else '')], input=sql, text=True,
                       capture_output=True)
    # psql xətası sorğu mətnini və ya parametrləri təkrar edə bilər — yalnız kodu və ilk sətri göstər
    if r.returncode != 0:
        sys.exit(f'psql xətası (kod {r.returncode}): {r.stderr.strip().splitlines()[0][:200] if r.stderr else ""}')
    return r.stdout


def sql_str(s):
    return 'null' if s is None else "'" + s.replace("'", "''") + "'"


def assemble(out_dir, dump):
    """Agent çıxışını yoxlayıb bir fayla yığır. Ayə/söz sayı ərəbcə ilə tam uyğun gəlməlidir."""
    ar = arabic_words()
    glosses = {}
    for f in sorted(os.listdir(out_dir)):
        if f.endswith('.json'):
            glosses.update(json.load(open(os.path.join(out_dir, f))))
    rows = json.load(open(dump))
    md5 = {f"{r['chapter_no']}:{r['verse_no']}": hashlib.md5(r['text'].encode()).hexdigest() for r in rows}
    errs = []
    for key, n in ((k, len(ar[int(k.split(':')[0]) * 1000 + int(k.split(':')[1])]) - 1) for k in md5):
        g = glosses.get(key)
        if g is None:
            errs.append(f'{key}: yoxdur')
        elif len(g) != n:
            errs.append(f'{key}: {len(g)} söz, gözlənilən {n}')
        else:
            errs += [f'{key}[{i}]: {s!r}' for i, s in enumerate(g)
                     if not isinstance(s, str) or not s.strip() or BAD.search(s) or s.count('(') != s.count(')')]
    if errs:
        sys.exit(f'{len(errs)} xəta:\n' + '\n'.join(errs[:40]))
    os.makedirs(DATA, exist_ok=True)
    payload = {k: {'gloss': [s.strip() for s in glosses[k]], 'source_md5': md5[k]}
               for k in sorted(md5, key=lambda k: tuple(map(int, k.split(':'))))}
    with gzip.open(os.path.join(DATA, 'glosses.json.gz'), 'wt', encoding='utf-8') as f:
        json.dump(payload, f, ensure_ascii=False, separators=(',', ':'))
    print(f'{len(payload)} ayə, {sum(len(v["gloss"]) for v in payload.values())} söz → data/glosses.json.gz')


def load():
    """Upsert. Yalnız mətni dəyişən sözlərin `status`-u 'ok'-a qayıdır — 'stale' işarəsi təsadüfən silinməsin."""
    data = json.load(gzip.open(os.path.join(DATA, 'glosses.json.gz'), 'rt', encoding='utf-8'))
    ar = arabic_words()
    en = json.load(gzip.open(urllib.request.urlopen(EN_PACK)))['verses']
    vals = []
    for key, v in data.items():
        c, a = map(int, key.split(':'))
        aid = c * 1000 + a
        for i, g in enumerate(v['gloss']):
            tl = (en.get(str(aid), {}).get(str(i)) or [None, None])[1] or None
            vals.append(f"({c},{a},{i},{sql_str(ar[aid][i])},{sql_str(g)},{sql_str(tl)},{sql_str(v['source_md5'])})")
    for start in range(0, len(vals), 5000):
        psql('insert into public.quran_wbw_az (chapter_no, verse_no, word_index, arabic, gloss, transliteration, source_md5)\nvalues '
             + ',\n'.join(vals[start:start + 5000]) +
             '\non conflict (chapter_no, verse_no, word_index) do update set '
             'arabic = excluded.arabic, gloss = excluded.gloss, transliteration = excluded.transliteration, '
             'source_md5 = excluded.source_md5, status = \'ok\', updated_at = now() '
             'where quran_wbw_az.gloss is distinct from excluded.gloss '
             '   or quran_wbw_az.source_md5 is distinct from excluded.source_md5;\n')
        print(f'  {min(start + 5000, len(vals))}/{len(vals)}')
    print(psql("select count(*), count(*) filter (where status = 'stale') from public.quran_wbw_az;", capture=True).strip())


def _inputs(keys, texts):
    """GUIDE.md giriş formatı: hər ayə üçün ərəbcə sözlər + türkcə/ingiliscə sözbəsöz + azərbaycanca tərcümə."""
    ar = arabic_words()
    tr = json.load(gzip.open(urllib.request.urlopen(EN_PACK.replace('wbw_en', 'wbw_tr'))))['verses']
    en = json.load(gzip.open(urllib.request.urlopen(EN_PACK)))['verses']
    prefix = re.compile(r'^\W*-?\s*\d+(?:\s*,\s*\d+)*\s*[:.]\s*')
    lines = []
    for c, a in keys:
        aid = c * 1000 + a
        n = len(ar[aid]) - 1
        lines.append(f'### {c}:{a} [n={n}]\naz: ' + prefix.sub('', texts[(c, a)].replace('\xa0', ' ')).strip())
        lines += [f'{i} {ar[aid][i]} | tr: {tr[str(aid)][str(i)][0]} | en: {en[str(aid)][str(i)][0]}' for i in range(n)]
        lines.append('')
    return '\n'.join(lines)


def stale(work):
    """Tərcüməsi dəyişmiş ayələri agent üçün hazırlayır (validate.py `stale` adı ilə yoxlayır)."""
    rows = json.loads(psql("select coalesce(json_agg(json_build_array(w.chapter_no, w.verse_no, q.text) "
                           "order by w.chapter_no, w.verse_no), '[]') from (select distinct chapter_no, verse_no "
                           "from public.quran_wbw_az where status = 'stale') w join public.quran_translations_data q "
                           "on q.slug = 'az' and q.chapter_no = w.chapter_no and q.verse_no = w.verse_no;", capture=True))
    if not rows:
        print('stale ayə yoxdur'); return
    os.makedirs(os.path.join(work, 'in'), exist_ok=True)
    texts = {(c, a): t for c, a, t in rows}
    open(os.path.join(work, 'in', 'stale.txt'), 'w').write(_inputs(sorted(texts), texts))
    md5 = {f'{c}:{a}': hashlib.md5(t.encode()).hexdigest() for c, a, t in rows}
    json.dump(md5, open(os.path.join(work, 'stale_md5.json'), 'w'))
    print(f'{len(rows)} ayə → {work}/in/stale.txt (GUIDE.md ilə agentə ver, çıxış out/stale_*.json)')


def merge(out_dir):
    """Yenilənmiş ayələri əsas fayla qatır; `source_md5` serverdəki indiki tərcümədən gəlir."""
    work = os.path.dirname(os.path.abspath(out_dir.rstrip('/')))
    md5 = json.load(open(os.path.join(work, 'stale_md5.json')))
    path = os.path.join(DATA, 'glosses.json.gz')
    data = json.load(gzip.open(path, 'rt', encoding='utf-8'))
    ar = arabic_words()
    new = {}
    for f in sorted(os.listdir(out_dir)):
        if f.startswith('stale_') and f.endswith('.json'):
            new.update(json.load(open(os.path.join(out_dir, f))))
    for k in md5:
        c, a = map(int, k.split(':'))
        g = new.get(k)
        if g is None or len(g) != len(ar[c * 1000 + a]) - 1 or any(BAD.search(s) or not s.strip() for s in g):
            sys.exit(f'{k}: çıxış yoxdur və ya yanlışdır')
        data[k] = {'gloss': [s.strip() for s in g], 'source_md5': md5[k]}
    with gzip.open(path, 'wt', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, separators=(',', ':'))
    print(f'{len(md5)} ayə yeniləndi → data/glosses.json.gz (sonra: load, pack, upload)')


def current_version():
    try:
        m = json.load(urllib.request.urlopen(PUBLIC + MANIFEST))
        return next((w['version'] for w in m['wbw'] if w['id'] == PACK_ID), 0)
    except Exception:
        return 0


def pack():
    rows = psql("select json_agg(json_build_array(chapter_no, verse_no, word_index, gloss, transliteration) "
                "order by chapter_no, verse_no, word_index) from public.quran_wbw_az;", capture=True)
    rows = json.loads(rows)
    verses = collections.defaultdict(dict)
    for c, a, i, g, tl in rows:
        verses[c * 1000 + a][i] = [g, tl or '']
    for aid, words in verses.items():  # ayə nömrəsi işarəsi — upstream paketləri kimi "(n)"
        words[len(words)] = [f'({aid % 1000})', '']
    version = current_version() + 1
    os.makedirs(BUILD, exist_ok=True)
    payload = {'version': version, 'verses': {str(k): {str(i): w for i, w in sorted(v.items())}
                                              for k, v in sorted(verses.items())}}
    with gzip.open(os.path.join(BUILD, f'{PACK_ID}.json.gz'), 'wt', encoding='utf-8') as f:
        json.dump(payload, f, ensure_ascii=False, separators=(',', ':'))
    manifest = {'version': 1, 'wbw': [{
        'id': PACK_ID, 'lang_code': 'az', 'lang_name': 'Azərbaycanca',
        'has_translation': True, 'has_transliteration': True,
        'url': PUBLIC + f'{PACK_ID}.json.gz', 'version': version,
    }]}
    json.dump(manifest, open(os.path.join(BUILD, MANIFEST), 'w'), ensure_ascii=False, indent=1)
    size = os.path.getsize(os.path.join(BUILD, f'{PACK_ID}.json.gz'))
    print(f'versiya {version}: {len(verses)} ayə, {len(rows)} söz, {size // 1024} KB')


def upload():
    """Faylı serverə ötürür və storage API ilə (service_role, yalnız serverdə oxunur) yazır."""
    for name, ctype in ((f'{PACK_ID}.json.gz', 'application/gzip'), (MANIFEST, 'application/json')):
        src = os.path.join(BUILD, name)
        subprocess.run(SSH + [f'cat > /tmp/{name}'], stdin=open(src, 'rb'), check=True)
        # Paket əvvəl, manifest sonra: manifest yeni versiyanı elan edəndə paket artıq yerində olsun.
        cmd = (f'set -e; KEY=$(sudo grep -E "^SERVICE_ROLE_KEY=" /opt/supabase/.env | cut -d= -f2-); '
               f'curl -s -o /dev/null -w "%{{http_code}}" -X POST '
               f'-H "Authorization: Bearer $KEY" -H "apikey: $KEY" -H "x-upsert: true" '
               f'-H "Cache-Control: max-age=300" -H "Content-Type: {ctype}" '
               f'--data-binary @/tmp/{name} https://anamuslim.cafarovceyxun.com/storage/v1/object/wbw/{name}; '
               f'rm -f /tmp/{name}')
        code = subprocess.run(SSH + [cmd], capture_output=True, text=True).stdout.strip()
        print(f'{name}: HTTP {code}')
        if code != '200':
            sys.exit('yükləmə alınmadı')
    local = hashlib.md5(open(os.path.join(BUILD, f'{PACK_ID}.json.gz'), 'rb').read()).hexdigest()
    remote = hashlib.md5(urllib.request.urlopen(PUBLIC + f'{PACK_ID}.json.gz').read()).hexdigest()
    print('ictimai URL md5', 'uyğundur' if local == remote else f'FƏRQLİDİR ({remote})')


if __name__ == '__main__':
    cmd = sys.argv[1]
    {'assemble': lambda: assemble(sys.argv[2], sys.argv[3]), 'load': load, 'pack': pack,
     'upload': upload, 'stale': lambda: stale(sys.argv[2]), 'merge': lambda: merge(sys.argv[2])}[cmd]()
