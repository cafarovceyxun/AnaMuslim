"""Converts the ill.js line-art set into Kotlin data (TaharahArtData.kt).

Each SVG element becomes Shape(role, pathData, matrix?, strokeWidth?) or Label(...). Group transforms
are flattened into one affine matrix per shape. Circles become arc paths.
"""
import json, math, re, subprocess, sys
import xml.etree.ElementTree as ET

import os
S = os.path.dirname(os.path.abspath(__file__))
OUT = sys.argv[1]
NAMES = ['face', 'mouth', 'hands', 'arm', 'head', 'foot', 'pour', 'sides', 'body', 'strike', 'blow', 'faceWipe',
         'wipeHands', 'khuff', 'qibla', 'ruku']

js = open(f'{S}/ill.js').read()
dump = js + "\nvar o={};[" + ",".join(f"'{n}'" for n in NAMES) + "].forEach(function(n){o[n]=ILLX[n]();});JSON.stringify(o);"
import tempfile
with tempfile.NamedTemporaryFile('w', suffix='.js', delete=False) as fh:
    fh.write(dump)
raw = subprocess.check_output(['osascript', '-l', 'JavaScript', fh.name]).decode()
os.unlink(fh.name)
arts = json.loads(raw)


def mul(a, b):
    # affine as (a,b,c,d,e,f): x' = a x + c y + e ; y' = b x + d y + f
    return (a[0] * b[0] + a[2] * b[1], a[1] * b[0] + a[3] * b[1],
            a[0] * b[2] + a[2] * b[3], a[1] * b[2] + a[3] * b[3],
            a[0] * b[4] + a[2] * b[5] + a[4], a[1] * b[4] + a[3] * b[5] + a[5])


ID = (1, 0, 0, 1, 0, 0)


def parse_transform(t):
    m = ID
    for name, args in re.findall(r'(\w+)\(([^)]*)\)', t or ''):
        v = [float(x) for x in re.split(r'[ ,]+', args.strip()) if x]
        if name == 'translate':
            m = mul(m, (1, 0, 0, 1, v[0], v[1] if len(v) > 1 else 0))
        elif name == 'rotate':
            r = math.radians(v[0])
            m = mul(m, (math.cos(r), math.sin(r), -math.sin(r), math.cos(r), 0, 0))
        elif name == 'scale':
            sx = v[0]
            sy = v[1] if len(v) > 1 else v[0]
            m = mul(m, (sx, 0, 0, sy, 0, 0))
    return m


def circle_d(cx, cy, r):
    return f'M{cx - r} {cy} A{r} {r} 0 1 0 {cx + r} {cy} A{r} {r} 0 1 0 {cx - r} {cy} Z'


def fmt(v):
    s = f'{v:.4f}'.rstrip('0').rstrip('.')
    return s if s not in ('-0', '') else '0'


def walk(el, m, out):
    tag = el.tag.split('}')[-1]
    if tag == 'g':
        m2 = mul(m, parse_transform(el.get('transform')))
        for c in el:
            walk(c, m2, out)
        return
    cls = el.get('class', '')
    if tag == 'rect' and 'bgc' in cls:
        return
    if tag == 'text':
        out.append(('L', cls or 'label', float(el.get('x')), float(el.get('y')), el.text or '',
                    el.get('text-anchor') or 'start', m))
        return
    m2 = mul(m, parse_transform(el.get('transform')))
    if tag == 'path':
        d = el.get('d')
    elif tag == 'circle':
        d = circle_d(float(el.get('cx')), float(el.get('cy')), float(el.get('r')))
    elif tag == 'ellipse':
        cx, cy, rx, ry = (float(el.get(k)) for k in ('cx', 'cy', 'rx', 'ry'))
        d = f'M{cx - rx} {cy} A{rx} {ry} 0 1 0 {cx + rx} {cy} A{rx} {ry} 0 1 0 {cx - rx} {cy} Z'
    else:
        raise ValueError(tag)
    sw = el.get('stroke-width')
    out.append(('S', cls, d, m2, float(sw) if sw else None))


ROLE = {
    'sk': 'SKIN', 'cl': 'CLOTH', 'hr': 'HAIR', 'dt': 'DETAIL', 'cdt': 'CLOTH_DETAIL', 'soft': 'SOFT', 'stitch': 'STITCH',
    'wa': 'WATER', 'wl': 'WATER_LINE', 'wb': 'WATER_BAND', 'wh': 'WATER_HIGHLIGHT', 'ea': 'EARTH', 'eat': 'EARTH_DETAIL',
    'du': 'DUST', 'mt': 'METAL', 'mtd': 'METAL_HANDLE', 'mtl': 'METAL_DETAIL', 'lt': 'LEATHER', 'ltd': 'LEATHER_DETAIL',
    'si': 'SILHOUETTE', 'shl': 'SHADOW_LINE', 'mat': 'MAT', 'hi': 'HIGHLIGHT', 'mv': 'MOTION', 'air': 'AIR', 'ring': 'RING',
    'bad': 'BAD', 'ok': 'OK', 'kaaba': 'KAABA', 'kiswa': 'KISWA', 'you': 'YOU', 'num': 'NUMBER', 'num2': 'NUMBER_ALT',
}
LABEL = {'p': 'ACCENT', 'w': 'ON_ACCENT', 'x': 'BAD', 'label': 'MUTED'}
ANCHOR = {'start': 'START', 'middle': 'MIDDLE', 'end': 'END'}

lines = []
for name in NAMES:
    s = arts[name]
    s = s.replace('<svg ', '<svg xmlns="http://www.w3.org/2000/svg" ', 1)
    root = ET.fromstring(s)
    vb = [float(x) for x in root.get('viewBox').split()]
    items = []
    for c in root:
        walk(c, ID, items)
    body = []
    for it in items:
        if it[0] == 'S':
            _, cls, d, m, sw = it
            role = ROLE[cls]
            args = [f'ArtRole.{role}', json.dumps(re.sub(r'\s+', ' ', d.strip()))]
            if m != ID:
                args.append('m(' + ', '.join(fmt(v) + 'f' for v in m) + ')')
            if sw is not None:
                args.append(f'w = {fmt(sw)}f')
            body.append('            Shape(' + ', '.join(args) + '),')
        else:
            _, cls, x, y, text, anchor, m = it
            if m != ID:
                x, y = m[0] * x + m[2] * y + m[4], m[1] * x + m[3] * y + m[5]
            role = LABEL[cls.split()[0]] if cls else 'MUTED'
            body.append(f'            Label({json.dumps(text, ensure_ascii=False)}, {fmt(x)}f, {fmt(y)}f, LabelRole.{role}, LabelAnchor.{ANCHOR[anchor]}),')
    const = re.sub(r'(?<!^)([A-Z])', r'_\1', name).upper()
    lines.append(f'    val {const} = Art(\n        width = {fmt(vb[2])}f,\n        height = {fmt(vb[3])}f,\n        items = listOf(\n' + '\n'.join(body) + '\n        ),\n    )\n')

kt = '''package com.cafarovceyxun.anamuslim.compose.screens.salah.art

// ⚠️ GENERATED by tools/salah-art/ill2kt.py from tools/salah-art/ill.js — do not edit by hand.
// Change the drawing in ill.js and run the script again.

@Suppress("LargeClass")
internal object TaharahArtData {
''' + '\n'.join(lines) + '}\n'
open(OUT, 'w').write(kt)
print('wrote', OUT, len(kt), 'bytes;', sum(len(a) for a in arts.values()), 'svg chars')
