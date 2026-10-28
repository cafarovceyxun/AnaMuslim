#!/usr/bin/env python3
"""duzelis.cafarovceyxun.com — gözləyən tərcümə təkliflərini birbaşa `quran_edits`-dən qurur.

    python3 site_from_db.py build              # → site/out/index.html (serverdən oxuyur, heç nə yazmır)
    python3 site_from_db.py deploy             # build + serverdəki səhifəni yedəkləyib əvəzləyir
    python3 site_from_db.py install            # serverə qoyur: /opt/duzelis + systemd taymeri (hər 5 dəq)
    python3 site_from_db.py refresh            # (serverdə, taymer işlədir) cədvəl dəyişibsə səhifəni yenidən qurur

Mənbə yalnız cədvəldir: `editor_email = 'translation-audit'` və `not is_approved` olan hər sətir bir kartdır —
solda serverdəki indiki mətn (`quran_translations_data`), sağda təklif (`new_text`), altında `reason`.
Təklif təsdiqlənəndə, rədd ediləndə və ya «Redaktə» ilə dəyişəndə səhifəni yenidən qurmaq kifayətdir —
köhnə generator (sessiya scratchpad-ında idi, itdi) auditin öz mətnindən qururdu və cədvəldən ayrılırdı
(məs. haşiyə nömrələri 10-18-dən qalır, saytda isə silinmiş görünürdü). Şablon `site/template.html`-dir.

Avtomatik yenilənmə serverdədir: brauzer cədvəli birbaşa oxuya bilməz (`quran_edits` yalnız `authenticated`-ə
açıqdır, sayt isə anonimdir), ona görə `duzelis-refresh.timer` hər 5 dəqiqədən bir `refresh` işlədir — təkliflərin
heşi dəyişməyibsə heç nə yazmır. Serverdə `quranapp.db` yoxdur: ərəbcə və surə adları `install` zamanı
`arabic.json`-a çıxarılıb yanına qoyulur.
"""
import collections
import datetime
import difflib
import html
import json
import os
import re
import sqlite3
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
DB = os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'db', 'quranapp.db')
TEMPLATE = os.path.join(HERE, 'site', 'template.html')
OUT = os.path.join(HERE, 'site', 'out', 'index.html')
SSH = ['ssh', '-i', os.path.expanduser('~/.ssh/oracle_cafarovceyxun'), 'ubuntu@130.61.171.105']
PSQL = 'sudo docker exec -i supabase-db psql -U postgres -d postgres -v ON_ERROR_STOP=1 -X -q -A -t'
LIVE = '/opt/supabase/volumes/proxy/caddy/meal/index.html'  # konteynerdə /etc/caddy/meal — qovluq adı köhnədir
REMOTE = '/opt/duzelis'
ON_SERVER = os.path.exists(os.path.join(HERE, 'arabic.json'))  # `install` yalnız serverdəki nüsxənin yanına qoyur
if ON_SERVER:
    TEMPLATE = os.path.join(HERE, 'template.html')

CATS = {'mena': 'Məna və struktur', 'moterize': 'Mötərizə qaydası', 'dirnaq': 'Dırnaq və mötərizə',
        'qram': 'Qrammatika', 'yazi': 'Yazı (orfoqrafiya)', 'durgu': 'Durğu və boşluq', 'uslub': 'Böyük/kiçik hərf'}


def fetch():
    sql = ("select coalesce(json_agg(json_build_object('c', e.chapter_no, 'v', e.verse_no, 'orig', q.text, "
           "'new', e.new_text, 'onote', q.note, 'nnote', e.note, 'reason', e.reason) "
           "order by e.chapter_no, e.verse_no), '[]') from public.quran_edits e "
           "join public.quran_translations_data q on q.id = e.translation_id "
           "where e.editor_email = 'translation-audit' and not e.is_approved;")
    cmd = ['sh', '-c', PSQL.replace('sudo ', '')] if ON_SERVER else SSH + [PSQL]
    r = subprocess.run(cmd, input=sql, text=True, capture_output=True)
    if r.returncode != 0:
        sys.exit(f'psql xətası (kod {r.returncode})')  # xəta mətni çap olunmur — sirr gigiyenası
    return json.loads(r.stdout)


def arabic_words():
    if ON_SERVER:
        return {int(k): v for k, v in json.load(open(os.path.join(HERE, 'arabic.json')))['ayahs'].items()}
    con = sqlite3.connect(f'file:{DB}?mode=ro', uri=True)
    out = collections.defaultdict(list)
    for aid, t in con.execute('select ayah_id, text from ayah_words where script_id = 1 order by ayah_id, word_index'):
        out[aid].append(t)
    return {k: ' '.join(v[:-1]) for k, v in out.items()}  # sonuncu söz ayə nömrəsi işarəsidir


def surah_names():
    if ON_SERVER:
        return {int(k): v for k, v in json.load(open(os.path.join(HERE, 'arabic.json')))['names'].items()}
    con = sqlite3.connect(f'file:{DB}?mode=ro', uri=True)
    return {r[0]: r[1] for r in con.execute("select surah_no, name from surah_localizations where lang_code='en'")}


# ---------------------------------------------------------------- render (köhnə generatorun eyni üslubu)

e = html.escape
TOK = re.compile(r'\s+|\w+|[^\w\s]')


def esc(s):
    return e(s).replace('​', '<span class="zw">görünməz simvol</span>').replace('\n', '<span class="zw">⏎</span>')


def vis(s, kind):
    if s.strip() == '':
        return f'<{kind} class="ws" title="boşluq">{"⏎" if chr(10) in s else "␣" * len(s)}</{kind}>'
    return f'<{kind}>{esc(s)}</{kind}>'


def diff(a, b):
    ta, tb = TOK.findall(a), TOK.findall(b)
    A, B = [], []
    for op, i1, i2, j1, j2 in difflib.SequenceMatcher(None, ta, tb, autojunk=False).get_opcodes():
        x, y = ''.join(ta[i1:i2]), ''.join(tb[j1:j2])
        if op == 'equal':
            A.append(esc(x)); B.append(esc(y))
        else:
            if x: A.append(vis(x, 'del'))
            if y: B.append(vis(y, 'ins'))
    return ''.join(A), ''.join(B)


def inline(t):
    t = e(t)
    t = re.sub(r'&quot;([^&]+?)&quot;', r'<q>\1</q>', t)
    return re.sub(r'([؀-ۿ][؀-ۿً-ٰ\s]*[؀-ۿً-ٰ])',
                  r'<span class="ar-in" lang="ar" dir="rtl">\1</span>', t)


def classify(reason):
    """Kateqoriya səbəb sətirlərindən: əvvəl audit qeydləri, `mexaniki:` sətirləri yalnız başqa qeyd yoxdursa."""
    lines = [l.lstrip('• ').strip() for l in (reason or '').split('\n') if l.strip()]
    main = [l for l in lines if not l.startswith('mexaniki:')]
    r = '\n'.join(main)
    if any(k in r.upper() for k in ('MƏNA', 'STRUKTUR', 'BURAXILMA', 'AYDINLIQ', 'MƏTN (')):
        return 'mena'
    if any(l.startswith('mötərizə:') for l in main):
        return 'moterize'
    low = r.lower()
    for cat, keys in (('dirnaq', ('dırnaq', 'mötərizə', 'simvol')), ('qram', ('qrammatika', 'zaman', 'təkrar')),
                      ('uslub', ('üslub', 'böyük hərf', 'kiçik hərf')), ('durgu', ('durğu', 'boşluq', 'prefiks')),
                      ('yazi', ('orfoqrafiya', 'hərf', 'ad', 'söz seçimi'))):
        if any(k in low for k in keys):
            return cat
    return 'durgu' if not main else 'yazi'


def card(x, ar):
    ref = f"{x['c']}:{x['v']}"
    cat = classify(x['reason'])
    pri = cat == 'mena' and bool(re.search(r'ciddi|hökm|BURAXILMA|STRUKTUR|\(orta', x['reason'] or ''))
    out = [f'<article class="card c-{cat}{" pri" if pri else ""}" data-cat="{cat}" data-pri="{int(pri)}" '
           f'data-new2="0" data-refs="{ref}" id="a{x["c"]}_{x["v"]}">',
           f'<header class="ch"><span class="ref">{ref}</span><span class="tag">{e(CATS[cat])}</span>'
           + ('<span class="tag crit">vacib</span>' if pri else '') + '</header>']
    pairs = [(ref, x['orig'], x['new'])]
    if (x['nnote'] or None) != (x['onote'] or None) and x['nnote'] is not None:
        pairs.append((ref + ' · qeyd (dipnot)', x['onote'] or '', x['nnote']))
    for lab, a, b in pairs:
        ca, cb = diff(a.strip(), b.strip())
        out.append(f'<div class="pair"><div class="col cur"><div class="vl">{e(lab)} · hazırkı</div><p class="tx">{ca}</p></div>'
                   f'<div class="col new"><div class="vl">{e(lab)} · təklif</div><p class="tx">{cb}</p></div></div>')
    a = ar.get(x['c'] * 1000 + x['v'])
    if a and cat in ('mena', 'moterize'):
        out.append(f'<div class="arabic"><div><span class="vl">{ref} · ərəbcə</span><p lang="ar" dir="rtl">{e(a)}</p></div></div>')
    lines = [l.lstrip('• ').strip() for l in (x['reason'] or '').split('\n') if l.strip()]
    out.append('<div class="why"><div class="wl">İzah</div>' + ''.join(f'<p>{inline(l)}</p>' for l in lines) + '</div></article>')
    return cat, pri, ''.join(out)


def build(out=OUT, rows=None):
    rows = fetch() if rows is None else rows
    ar, names = arabic_words(), surah_names()
    cards = [(x,) + card(x, ar) for x in rows]
    pri = [c for c in cards if c[2]]
    cnt = collections.Counter(c[1] for c in cards)
    body = [f'<section id="vacib"><h2>Ən vacib — məna və hökm</h2><p class="lede">Ərəbcəyə açıq zidd olan, hökmü '
            f'dəyişən və ya ayənin quruluşunu pozan {len(pri)} yer. Hər birində ərəbcə mətn də verilib.</p>'
            f'<div class="list">' + ''.join(c[3] for c in pri) + '</div></section>']
    groups = collections.OrderedDict()
    for x, cat, p, h in cards:
        if not p:
            groups.setdefault(x['c'], []).append(h)
    body.append('<section id="hamisi"><h2>Bütün təkliflər, surə-surə</h2><div class="groups">' + ''.join(
        f'<div class="sura" data-sura="{s}"><h3><span>{s}. {e(names.get(s, ""))}</span><small>{len(hs)} ayə</small></h3>'
        f'<div class="list">' + ''.join(hs) + '</div></div>' for s, hs in groups.items()) + '</div></section>')
    chips = ''.join(f'<button type="button" class="chip on" data-cat="{k}" id="chip-{k}" aria-pressed="true">'
                    f'<i class="dot c-{k}"></i>{e(v)} <b>{cnt.get(k, 0)}</b></button>' for k, v in CATS.items() if cnt.get(k))
    now = datetime.datetime.now().strftime('%d.%m.%Y %H:%M')
    intro = (f'<p class="intro">Bu səhifə <b>birbaşa düzəliş cədvəlindən</b> (<code>quran_edits</code>, təsdiq gözləyən '
             f'təkliflər) qurulur — tətbiqdəki «Düzəlişləri İdarə Et» panelində nə varsa, burada da odur. Hər kartda solda '
             f'bazadakı hazırkı mətn, sağda təklif var: silinən hissə qırmızı xətlə, gələn hissə yaşıl rənglə. '
             f'Cəmi <b>{len(cards)}</b> ayə, onlardan {cnt.get("mena", 0)}-i məna və quruluşa aiddir. '
             f'Təsdiqlənən və rədd edilən təkliflər səhifə yeniləndikdə buradan çıxır.</p>\n'
             f'    <p class="intro">Ayələrdəki haşiyə nömrələri (məs. «tutmaz1») gələcək qeydlərin yeridir və saxlanılır.</p>')
    tpl = open(TEMPLATE).read()
    page = (tpl.replace('%%EYEBROW%%', f'Mürşüd Yusifoğlu tərcüməsi · gözləyən təkliflər · yenilənib {now}')
            .replace('%%INTRO%%', intro).replace('%%CHIPS%%', chips).replace('%%BODY%%', ''.join(body)))
    os.makedirs(os.path.dirname(out), exist_ok=True)
    tmp = out + '.tmp'
    open(tmp, 'w').write(page)
    os.replace(tmp, out)  # Caddy yarımçıq faylı verməsin
    print(f'{len(cards)} ayə ({dict(cnt)}), vacib {len(pri)} → {out} ({len(page) // 1024} KB)')


def deploy():
    build()
    stamp = datetime.datetime.now().strftime('%Y-%m-%d-%H%M')
    subprocess.run(SSH + ['cat > /tmp/duzelis.html'], stdin=open(OUT, 'rb'), check=True)
    subprocess.run(SSH + [f'set -e; D=/opt/supabase/volumes/proxy/caddy; sudo cp -p {LIVE} $D/index.html.bak-{stamp}; '
                          f'sudo cp /tmp/duzelis.html {LIVE}; rm /tmp/duzelis.html'], check=True)
    print(f'yerləşdirildi, köhnəsi: index.html.bak-{stamp}')


def refresh():
    """Serverdə taymer işlədir: yalnız təkliflər (və ya şablon/skript) dəyişəndə yazır."""
    import hashlib
    rows = fetch()
    h = hashlib.md5((json.dumps(rows, sort_keys=True, ensure_ascii=False) + open(TEMPLATE).read()
                     + open(__file__).read()).encode()).hexdigest()
    state = os.path.join(HERE, 'last.hash')
    if os.path.exists(state) and open(state).read().strip() == h and os.path.exists(LIVE):
        return
    build(LIVE, rows)
    open(state, 'w').write(h)


UNIT = """[Unit]
Description=duzelis saytını quran_edits-dən yenidən qur
After=docker.service

[Service]
Type=oneshot
ExecStart=/usr/bin/python3 {remote}/site_from_db.py refresh
"""
TIMER = """[Unit]
Description=duzelis saytı — hər 5 dəqiqə

[Timer]
OnBootSec=2min
OnUnitActiveSec=5min

[Install]
WantedBy=timers.target
"""


def install():
    """Skripti, şablonu və ərəbcəni serverə qoyur, systemd taymerini qoşur və bir dəfə işlədir."""
    data = {'ayahs': arabic_words(), 'names': surah_names()}
    tmpdir = os.path.join(HERE, 'site', 'out')
    os.makedirs(tmpdir, exist_ok=True)
    json.dump(data, open(os.path.join(tmpdir, 'arabic.json'), 'w'), ensure_ascii=False)
    for src, name in ((__file__, 'site_from_db.py'), (TEMPLATE, 'template.html'),
                      (os.path.join(tmpdir, 'arabic.json'), 'arabic.json')):
        subprocess.run(SSH + [f'cat > /tmp/{name}'], stdin=open(src, 'rb'), check=True)
    unit = UNIT.format(remote=REMOTE).replace('\n', '\\n')
    subprocess.run(SSH + [f"""set -e
sudo mkdir -p {REMOTE}
sudo mv /tmp/site_from_db.py /tmp/template.html /tmp/arabic.json {REMOTE}/
sudo rm -f {REMOTE}/last.hash
printf '%b' '{unit}' | sudo tee /etc/systemd/system/duzelis-refresh.service >/dev/null
printf '%b' '{TIMER.replace(chr(10), chr(92) + 'n')}' | sudo tee /etc/systemd/system/duzelis-refresh.timer >/dev/null
sudo systemctl daemon-reload
sudo systemctl enable --now duzelis-refresh.timer >/dev/null
sudo systemctl start duzelis-refresh.service
systemctl is-active duzelis-refresh.timer
sudo journalctl -u duzelis-refresh.service -n 3 --no-pager -o cat"""], check=True)


if __name__ == '__main__':
    {'build': build, 'deploy': deploy, 'refresh': refresh, 'install': install}[sys.argv[1]]()
