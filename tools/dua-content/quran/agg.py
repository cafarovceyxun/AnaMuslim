"""Yazılmış sətirlərin tək-hash yoxlaması: python3 agg.py → hər alt başlıq üçün gözlənilən hash (server sorğusu ilə müqayisə)"""
import json, hashlib
R = json.load(open('review.json'))['rows']
md = lambda x: hashlib.md5((x or '').encode()).hexdigest()
out = {}
for r in sorted(R, key=lambda r: (r['sub'].encode(), r['sort'], r['part'])):
    out.setdefault(r['sub'], []).append(md(r['ar']) + md(r['az']) + md(r['note']) + md(r['source']) + f"{r['c']}:{r['v']}:{r['ve'] or ''}")
for s, l in out.items(): print(s, len(l), md(','.join(l)))
