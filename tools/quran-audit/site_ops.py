#!/usr/bin/env python3
"""`duzelis` saytının (2026-10-14) əl ilə yoxlanmış redaktələrini ayə üzrə birləşdirir.

    python3 site_ops.py <canvas-qovluğu> <az.json>   # → out/site.json

Sayt hər audit bəndini ayrıca kart kimi göstərir və hər kartın redaktəsini orijinal mətnə **ayrıca**
tətbiq edir. Burada eyni ayəyə düşən bütün kartların əməliyyatları ardıcıl tətbiq olunur. Əməliyyat
tapılmasa (iki kart eyni yeri dəyişib) ayə `conflicts`-ə düşür və build.py həmin ayə üçün agent
nəticəsinə keçir. Generatorun özü (`gen.py`, `manual.py`, `manual2.py`, `items.json`) həmin
sessiyanın qovluğundadır — məntiq oradan köçürülüb.
"""
import json
import os
import re
import sys

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'out')
CANVAS, DUMP = sys.argv[1], sys.argv[2]
sys.path.insert(0, CANVAS)
from manual import MANUAL, BLOCK  # noqa: E402
from manual2 import MANUAL2  # noqa: E402

MANUAL.update(MANUAL2)


def _sh(d):  # 24:26 (indeks 237) auditdən silinib — gen.py ilə eyni sürüşmə
    return {(k - 1 if k > 237 else k): v for k, v in d.items() if k != 237}


MANUAL = _sh(MANUAL)
BLOCK = _sh(BLOCK)

rows = json.load(open(DUMP))
M = {f"{r['chapter_no']}:{r['verse_no']}": r['text'] for r in rows}
M.update({f"{r['chapter_no']}:{r['verse_no']}#note": (r['note'] or '') for r in rows})
items = json.load(open(os.path.join(CANVAS, 'items.json')))
TOK = re.compile(r'\s+|\w+|[^\w\s]')


def bfind(o):
    pat = re.escape(o)
    if re.match(r'\w', o):
        pat = r'(?<!\w)' + pat
    if re.search(r'\w$', o):
        pat = pat + r'(?!\w)'
    return pat


def apply(text, old, new):
    if old == '':
        t = text.rstrip()
        return t + new + text[len(t):]
    if old == '@NL':
        assert '\n' in text
        return re.sub(r'\s*\n\s*', new, text, count=1)
    if old.startswith('*'):
        o = old[1:]
        assert o in text, o
        return text.replace(o, new)
    if old.startswith('$'):
        o = old[1:]
        i = text.rfind(o)
        assert i >= 0, o
        return text[:i] + new + text[i + len(o):]
    if old.startswith('~'):
        new_text = re.sub(bfind(old[1:]), lambda m: new, text)
        assert new_text != text, old
        return new_text
    i = text.find(old)
    assert i >= 0, old
    return text[:i] + new + text[i + len(old):]


def narrow(o, n):
    import difflib
    to, tn = TOK.findall(o), TOK.findall(n)
    k = len(tn)
    if k >= len(to):
        return o, n
    best = (0, 0)
    for st in range(len(to) - k + 1):
        w = ''.join(to[st:st + k])
        r = difflib.SequenceMatcher(None, w.lower(), n.lower()).ratio()
        if r > best[0]:
            best = (r, st)
    if best[0] < 0.5:
        return o, n
    st = best[1]
    return o, ''.join(to[:st]) + n + ''.join(to[st + k:])


ops = {}  # ref → [(kart, old, new)]
for i, x in enumerate(items):
    if i in BLOCK:
        continue  # ASCII dırnaq blokları — build.py-nin mexaniki keçidi hamısını edir
    if i in MANUAL:
        edits = [e for e in MANUAL[i] if e[1] is not None]
    elif x['auto'] and not x['amb']:
        edits = []
        for _, o, n in x['auto']:
            o, n = narrow(o, n)
            for r in x['refs']:
                if r in M and re.search(bfind(o), M[r]):
                    edits.append((r, '~' + o, n))
    else:
        continue
    for r, o, n in edits:
        ops.setdefault(r, []).append((i, o, n))

result, conflicts = {}, {}
for r, lst in ops.items():
    text = M[r]
    try:
        for i, o, n in lst:
            text = apply(text, o, n)
    except AssertionError as e:
        conflicts[r] = f'kart #{i}: {str(e)[:80]}'
        continue
    if text != M[r]:
        result[r] = {'text': text,
                     'reasons': list(dict.fromkeys(f"{items[i]['type']}: {items[i]['problem']}" for i, _, _ in lst))}

json.dump({'verses': result, 'conflicts': conflicts}, open(os.path.join(OUT, 'site.json'), 'w'),
          ensure_ascii=False, indent=1)
print(f"sayt: {len([r for r in result if '#' not in r])} ayə mətni, "
      f"{len([r for r in result if '#' in r])} qeyd, {len(conflicts)} toqquşma: {list(conflicts.items())[:8]}")
