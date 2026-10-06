import json, html
from itertools import groupby
import os
P = os.path.dirname(os.path.abspath(__file__))
exec(open(f'{P}/cats.py').read())
R = json.load(open('review.json'))
E = lambda s: html.escape(s or '').replace('\n', '<br>')
order = {c[0]: i for i, c in enumerate(CATS)}
names = {c[0]: (c[1], c[2]) for c in CATS}
R.sort(key=lambda r: (order[r['cat']], r['sub'] or '', r['sort'], r['part']))
duas = [list(g) for _, g in groupby(R, key=lambda r: (r['cat'], r['sub'], r['sort'], r['hid']))]
def piece(r):
    rep = f'<span class="rep">{r["rep"]} dəfə</span>' if r['rep'] else ''
    out = [f'<div class="piece">{"<span class=rep-row>"+rep+"</span>" if rep else ""}',
           f'<p class="ar" dir="rtl" lang="ar">{E(r["ar"])}</p>']
    if r['tr']: out.append(f'<p class="tr">{E(r["tr"])}</p>')
    out.append(f'<p class="az">{E(r["az"])}</p>' if r['az'] else '<p class="az missing">Kitabda tərcüməsi yoxdur</p>')
    if r['note']: out.append(f'<p class="note">{E(r["note"])}</p>')
    out.append('</div>'); return ''.join(out)
body, toc = [], []
for cat, g in groupby(duas, key=lambda d: d[0]['cat']):
    g = list(g); nm, isnew = names[cat]
    toc.append(f'<li><a href="#{cat}">{E(nm)}</a><span class="n">{len(g)}</span>{"<span class=tag>yeni</span>" if isnew else ""}</li>')
    body.append(f'<section id="{cat}"><h2>{E(nm)}{" <span class=tag>yeni başlıq</span>" if isnew else ""}</h2>')
    for sub, sg in groupby(g, key=lambda d: d[0]['sub']):
        if sub:
            sn, snew = SUBS[sub]
            body.append(f'<h3>{E(sn)}{" <span class=tag>yeni alt başlıq</span>" if snew else ""}</h3>')
        for d in sg:
            h = d[0]
            body.append(f'<article><div class="meta"><span class="no">№ {h["no"]}</span></div>'
                        + ''.join(piece(p) for p in d) + f'<footer>{E(h["source"])}</footer></article>')
    body.append('</section>')
newc = sum(1 for c in CATS if c[2] and any(d[0]['cat'] == c[0] for d in duas))
page = open(f'{P}/tpl.html').read().replace('%%TOC%%', '\n'.join(toc)).replace('%%BODY%%', '\n'.join(body)) \
    .replace('%%N%%', str(len(duas))).replace('%%NC%%', str(newc))
open('dua-siyahisi.html', 'w').write(page)
print(len(duas), newc)
