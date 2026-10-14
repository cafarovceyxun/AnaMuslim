#!/usr/bin/env python3
"""Fərq göstəricisi: `python3 diff.py base [səbəb-süzgəci] [say]` və ya `python3 diff.py edits [ref…]`."""
import difflib
import json
import os
import sys

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'out')


def show(ref, a, b):
    sm = difflib.SequenceMatcher(None, a, b, autojunk=False)
    parts = []
    for op, i1, i2, j1, j2 in sm.get_opcodes():
        if op == 'equal':
            continue
        parts.append(f'…{a[max(0, i1 - 18):i1]}[{a[i1:i2]}→{b[j1:j2]}]{a[i2:i2 + 12]}…')
    print(ref, ' | '.join(parts))


if sys.argv[1] == 'base':
    base = json.load(open(os.path.join(OUT, 'base.json')))
    flt = sys.argv[2] if len(sys.argv) > 2 else ''
    n = int(sys.argv[3]) if len(sys.argv) > 3 else 10
    for k, e in base.items():
        if any(flt in r for r in e['mechanical']):
            show(k, e['original'], e['text'])
            n -= 1
            if n == 0:
                break
else:
    edits = {e['ref']: e for e in json.load(open(os.path.join(OUT, 'edits.json')))}
    for k in sys.argv[2:] or list(edits)[:20]:
        show(k, edits[k]['original'], edits[k]['new_text'])
