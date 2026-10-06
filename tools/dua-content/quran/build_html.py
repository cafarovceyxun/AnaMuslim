"""review.json → qurandan-dualar.html (bazaya yazmazdan əvvəl istifadəçiyə göstərilən baxış səhifəsi)"""
import json, html, os, re
from itertools import groupby
P = os.path.dirname(os.path.abspath(__file__))
R = json.load(open(f'{P}/review.json'))
E = lambda s: html.escape(s or '')
style = re.search(r'<style>.*?</style>', open(f'{P}/../tpl.html').read(), re.S)[0]
style = style.replace('</style>', '.qid { font-family: var(--f-ui); font-size: 12px; font-weight: 600; color: var(--muted); border: 1px solid var(--line); border-radius: 4px; padding: 0 6px; font-variant-numeric: tabular-nums; }\n.lab { font-family: var(--f-ui); font-size: 12px; color: var(--muted); }\n</style>')
names = dict(R['subs']); order = {s: i for i, (s, _) in enumerate(R['subs'])}
rows = sorted(R['rows'], key=lambda r: (order[r['sub']], r['sort'], r['part']))
duas = [list(g) for _, g in groupby(rows, key=lambda r: (r['sub'], r['sort']))]
NEW_GROUPS = {'quran-cennet-ehli', 'quran-dua-haqqinda'}
body, toc, q = [], [], 0
for sub, g in groupby(duas, key=lambda d: d[0]['sub']):
    g = list(g)
    tag = ' <span class="tag">təklif</span>' if sub in NEW_GROUPS else ''
    toc.append(f'<li><a href="#{sub}">{E(names[sub])}</a><span class="n">{len(g)}</span>{tag}</li>')
    body.append(f'<section id="{sub}"><h2>{E(names[sub])}{tag}</h2>')
    for d in g:
        q += 1
        parts = []
        for r in d:
            p = [f'<p class="ar" dir="rtl" lang="ar">{E(r["ar"])}</p>', f'<p class="az">{E(r["az"])}</p>']
            if r['note']: p.insert(0, f'<p class="note">{E(r["note"])}</p>')
            if r['part'] > 1: p.insert(0, f'<span class="lab">Davamı · {E(r["source"])}</span>')
            parts.append('<div class="piece">' + ''.join(p) + '</div>')
        body.append(f'<article><div class="meta"><span class="qid">Q{q}</span></div>{"".join(parts)}'
                    f'<footer>{E(d[0]["source"])}</footer></article>')
    body.append('</section>')
page = f'''<title>Qurandan dualar</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Amiri:wght@400;700&family=Literata:ital,opsz,wght@0,7..72,400;0,7..72,600;1,7..72,400&family=Source+Sans+3:wght@400;600&display=swap">
{style}
<div class="wrap">
  <header class="intro">
    <div class="eyebrow">Dua bölməsi · bazaya yazılmadan əvvəl baxış</div>
    <h1>Qurandan dualar</h1>
    <p class="lede">Yeni başlıq «Qurandan dualar», altında 17 alt başlıq: əvvəlcə Allahın etməyimizi istədiyi dualar, sonra nəbilərin duaları (hər nəbi ayrıca alt başlıq), sonra salehlər, mələklər və digərləri. Ərəbcə tətbiqin öz Quran bazasından (Osmani yazısı), tərcümə tətbiqdəki Mürşüd Yusifoğlu tərcüməsindən kəsilib, əl ilə heç nə yazılmayıb. Boz qeyd sətri duanı kimin etdiyini göstərir və o da ayənin öz tərcüməsidir.</p>
    <div class="stats"><span><b>{len(duas)}</b> dua</span><span><b>{len(toc)}</b> alt başlıq</span><span>Hər duanın nömrəsi (Q1, Q2…) ilə «Q37-ni çıxar» kimi yaza bilərsən</span></div>
  </header>
  <aside class="qs" aria-labelledby="qs-h">
    <h2 id="qs-h">Qərar gözləyən suallar</h2>
    <ol>
      <li><b>Yeri.</b> «Qurandan dualar» başlığını siyahının ən başına qoydum (Namazdan da əvvəl). Belə qalsın?</li>
      <li><b>Bəqərə 285–286 və Ali-İmran 191–194.</b> Ayədə «onlar dedilər» deyilir, yəni möminlərin duasıdır. Ona görə «Salehlər» altına qoydum. Birinci qrupa («Allahın istədiyi») keçirim?</li>
      <li><b>Sözü olmayan əmrlər.</b> Birinci qrupda Nəhl 98 (şeytandan sığın), Əhzab 56 (salavat), Muhəmməd 19 və Nəsr 3 (bağışlanma dilə) var. Bunlarda hazır dua sözü yoxdur, əmrin özüdür. Saxlayım?</li>
      <li><b>Qarğış duaları.</b> Nuh 26–27 (kafirlərə qarşı) və Yunus 88 (Musanın Firona qarşı duası) Quranda nəbinin duası kimi gəlir, ona görə saxladım. Çıxarım?</li>
      <li><b>Təklif qrupları.</b> «Cənnət əhlinin həmdi» (4) və «Dua və zikr haqqında ayələr» (11) sənin «başqa bab» sözünə görə əlavə etdim. Lazım deyilsə ikisini də çıxarım.</li>
      <li><b>Oxunuş (latın hərfləri).</b> Tətbiqin bazasında Quranın latın oxunuşu yoxdur. Kitabda olmayanı yazmadığım üçün bu duaların oxunuş sətri boş qalır.</li>
    </ol>
  </aside>
  <nav aria-label="Alt başlıqlar"><h2>Alt başlıqlar</h2><ol>
{chr(10).join(toc)}
  </ol></nav>
  <main>
{chr(10).join(body)}
  </main>
</div>
'''
open(f'{P}/qurandan-dualar.html', 'w').write(page)
print(len(duas), len(toc))
