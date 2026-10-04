"""Raster line art → vector (Android VectorDrawable XML + SVG), pure Python (no PIL/numpy/potrace).

Pipeline per image: gray → threshold → remove frame lines (long straight runs) and stray pieces at the edges
→ crop to the figure → bilinear upscale → threshold → trace pixel-crack contours → Douglas–Peucker →
quadratic smoothing through midpoints → one even-odd filled path (the ink), transparent background.
"""
import sys, os, math, json
sys.setrecursionlimit(200000)
from collections import deque
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import png

def gray(path):
    w, h, bpp, rows = png.read(path)
    g = [[255] * w for _ in range(h)]
    for y in range(h):
        r = rows[y]
        for x in range(w):
            if bpp >= 3:
                v = (r[x*bpp] * 299 + r[x*bpp+1] * 587 + r[x*bpp+2] * 114) // 1000
            else:
                v = r[x*bpp]
            if bpp in (2, 4) and r[x*bpp + bpp - 1] < 128:
                v = 255
            g[y][x] = v
    return w, h, g

def components(mask, w, h):
    seen = [[False] * w for _ in range(h)]; comps = []
    for y in range(h):
        for x in range(w):
            if mask[y][x] and not seen[y][x]:
                q = deque([(x, y)]); seen[y][x] = True; pts = []
                while q:
                    cx, cy = q.popleft(); pts.append((cx, cy))
                    for dx in (-1, 0, 1):
                        for dy in (-1, 0, 1):
                            nx, ny = cx + dx, cy + dy
                            if 0 <= nx < w and 0 <= ny < h and mask[ny][nx] and not seen[ny][nx]:
                                seen[ny][nx] = True; q.append((nx, ny))
                comps.append(pts)
    return comps

def clean(w, h, g, thr=150, protect=None, line_frac=0.42, speck=6):
    """Whiten frame lines and debris. protect(x, y) -> True keeps a pixel (e.g. a head outline crossing a frame line)."""
    dark = [[g[y][x] < thr for x in range(w)] for y in range(h)]
    kill = [[False] * w for _ in range(h)]
    # Long straight horizontal / vertical runs = frame lines.
    for y in range(h):
        x = 0
        while x < w:
            if dark[y][x]:
                s = x
                while x < w and dark[y][x]: x += 1
                if x - s > line_frac * w:
                    for k in range(s, x): kill[y][k] = True
            else:
                x += 1
    for x in range(w):
        y = 0
        while y < h:
            if dark[y][x]:
                s = y
                while y < h and dark[y][x]: y += 1
                if y - s > line_frac * h:
                    for k in range(s, y): kill[k][x] = True
            else:
                y += 1
    # Grow the kill mask by 1 px across the line so its antialiased edges go too.
    grown = [row[:] for row in kill]
    for y in range(h):
        for x in range(w):
            if kill[y][x]:
                for dx in (-1, 0, 1):
                    for dy in (-1, 0, 1):
                        nx, ny = x + dx, y + dy
                        if 0 <= nx < w and 0 <= ny < h and g[ny][nx] < 230: grown[ny][nx] = True
    for y in range(h):
        for x in range(w):
            if grown[y][x] and not (protect and protect(x, y)):
                g[y][x] = 255; dark[y][x] = False
    # Debris: specks, and small pieces touching the image border (bits of neighbouring panels).
    for pts in components(dark, w, h):
        xs = [p[0] for p in pts]; ys = [p[1] for p in pts]
        touches = min(xs) == 0 or min(ys) == 0 or max(xs) == w - 1 or max(ys) == h - 1
        big = len(pts) > 0.004 * w * h
        corner = len(pts) < 0.02 * w * h and (max(xs) < 0.15 * w or min(xs) > 0.85 * w) and (max(ys) < 0.15 * h or min(ys) > 0.85 * h)
        if len(pts) < speck or (touches and not big) or corner:
            for x, y in pts: g[y][x] = 255
    return g

def bbox(w, h, g, thr=150, margin=6):
    xs = [x for y in range(h) for x in range(w) if g[y][x] < thr]
    ys = [y for y in range(h) for x in range(w) if g[y][x] < thr]
    x0, x1, y0, y1 = max(0, min(xs) - margin), min(w, max(xs) + 1 + margin), max(0, min(ys) - margin), min(h, max(ys) + 1 + margin)
    return x0, y0, x1, y1

def upscale_mask(g, x0, y0, x1, y1, s, thr=150):
    W, H = (x1 - x0) * s, (y1 - y0) * s
    m = [[False] * W for _ in range(H)]
    for Y in range(H):
        fy = (Y + .5) / s - .5 + y0; iy = int(math.floor(fy)); ty = fy - iy
        ya, yb = min(max(iy, y0), y1 - 1), min(max(iy + 1, y0), y1 - 1)
        for X in range(W):
            fx = (X + .5) / s - .5 + x0; ix = int(math.floor(fx)); tx = fx - ix
            xa, xb = min(max(ix, x0), x1 - 1), min(max(ix + 1, x0), x1 - 1)
            v = (g[ya][xa] * (1 - tx) + g[ya][xb] * tx) * (1 - ty) + (g[yb][xa] * (1 - tx) + g[yb][xb] * tx) * ty
            m[Y][X] = v < thr
    return W, H, m

def trace(W, H, m):
    """Crack-following contours; each pixel boundary edge oriented clockwise around ink."""
    out = {}
    def add(a, b): out.setdefault(a, []).append(b)
    for y in range(H):
        row = m[y]
        for x in range(W):
            if not row[x]: continue
            if y == 0 or not m[y-1][x]: add((x, y), (x+1, y))
            if x == W-1 or not row[x+1]: add((x+1, y), (x+1, y+1))
            if y == H-1 or not m[y+1][x]: add((x+1, y+1), (x, y+1))
            if x == 0 or not row[x-1]: add((x, y+1), (x, y))
    loops = []
    while out:
        start = next(iter(out)); a = start; prev = None; loop = [a]
        while True:
            nxt = out[a]
            if len(nxt) == 1: b = nxt.pop()
            else:
                # Ambiguous diagonal vertex: take the right turn relative to the incoming direction.
                d = (a[0] - prev[0], a[1] - prev[1]) if prev else (1, 0)
                right = (-d[1], d[0])
                b = next((c for c in nxt if (c[0] - a[0], c[1] - a[1]) == right), nxt[0]); nxt.remove(b)
            if not out[a]: del out[a]
            prev, a = a, b
            if a == start: break
            loop.append(a)
        loops.append(loop)
    return loops

def dp(pts, eps):
    if len(pts) < 3: return pts
    a, b = pts[0], pts[-1]; dx, dy = b[0] - a[0], b[1] - a[1]; L = math.hypot(dx, dy) or 1e-9
    i_max, d_max = 0, -1
    for i in range(1, len(pts) - 1):
        d = abs(dy * (pts[i][0] - a[0]) - dx * (pts[i][1] - a[1])) / L
        if d > d_max: i_max, d_max = i, d
    if d_max > eps:
        return dp(pts[:i_max + 1], eps)[:-1] + dp(pts[i_max:], eps)
    return [a, b]

def simplify_closed(loop, eps):
    if len(loop) < 8: return loop
    # Split at the farthest point from loop[0] so DP works on two open chains.
    far = max(range(len(loop)), key=lambda i: (loop[i][0] - loop[0][0]) ** 2 + (loop[i][1] - loop[0][1]) ** 2)
    c1 = loop[:far + 1]; c2 = loop[far:] + [loop[0]]
    return dp(c1, eps)[:-1] + dp(c2, eps)[:-1]

def path_d(loops, s, min_area):
    parts = []
    def f(v): return ('%.1f' % (v / s)).rstrip('0').rstrip('.')
    for lp in loops:
        area = 0
        for i in range(len(lp)):
            x1, y1 = lp[i]; x2, y2 = lp[(i + 1) % len(lp)]; area += x1 * y2 - x2 * y1
        if abs(area) / 2 < min_area: continue
        p = simplify_closed(lp, eps=0.9)
        if len(p) < 3: continue
        n = len(p); mid = lambda i: ((p[i][0] + p[(i + 1) % n][0]) / 2, (p[i][1] + p[(i + 1) % n][1]) / 2)
        m0 = mid(0); d = 'M%s %s' % (f(m0[0]), f(m0[1]))
        for i in range(1, n + 1):
            c = p[i % n]; mm = mid(i % n)
            d += 'Q%s %s %s %s' % (f(c[0]), f(c[1]), f(mm[0]), f(mm[1]))
        parts.append(d + 'Z')
    return ''.join(parts)

def complete_head(w, h, g, thr=150):
    """The head top hides under a frame line in the user's drawings: fit the skull circle from its sides just
    below the line, wipe everything above, and redraw the missing arc with the outline's own thickness."""
    line = [y for y in range(min(40, h)) if sum(1 for x in range(w) if g[y][x] < thr) > 0.5 * w]
    if not line: return
    lb = max(line); pts = []
    for y in range(lb + 2, lb + 16):
        xs = [x for x in range(int(w * .15), int(w * .85)) if g[y][x] < thr]
        if xs: pts.append((y, min(xs), max(xs)))
    (y1, a1, b1), (y2, a2, b2) = pts[0], pts[-1]
    cx = (a1 + b1 + a2 + b2) / 4; h1, h2 = (b1 - a1) / 2, (b2 - a2) / 2
    cy = ((h2 ** 2 - h1 ** 2) + (y2 ** 2 - y1 ** 2)) / (2 * (y2 - y1)); r = math.sqrt(h1 ** 2 + (y1 - cy) ** 2)
    t = 1.0  # half the outline width at source resolution (~2 px strokes)
    for y in range(0, lb + 3):
        for x in range(w):
            d = abs(math.hypot(x - cx, y - cy) - r)
            g[y][x] = 255 if d > t + 1 else min(255, int(30 + max(0.0, d - t) * 200))
    return cx, cy, r

def vectorize(src, name, outdir, scale=4, protect=None, line_frac=0.42, head=False, thr=150):
    w, h, g = gray(src)
    if head: print('  baş:', complete_head(w, h, g, thr))
    g = clean(w, h, g, thr=thr, protect=protect, line_frac=line_frac)
    x0, y0, x1, y1 = bbox(w, h, g, thr)
    W, H, m = upscale_mask(g, x0, y0, x1, y1, scale, thr)
    loops = trace(W, H, m)
    d = path_d(loops, scale, min_area=4 * scale * scale)
    vw, vh = x1 - x0, y1 - y0
    svg = f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {vw} {vh}"><path fill="currentColor" fill-rule="evenodd" d="{d}"/></svg>'
    xml = (f'<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    android:width="{vw}dp"\n    android:height="{vh}dp"\n'
           f'    android:viewportWidth="{vw}"\n    android:viewportHeight="{vh}">\n'
           f'    <path\n        android:fillColor="#FF1B1B1F"\n        android:fillType="evenOdd"\n        android:pathData="{d}" />\n</vector>\n')
    open(f'{outdir}/{name}.svg', 'w').write(svg)
    open(f'{outdir}/{name}.xml', 'w').write(xml)
    return vw, vh, len(d)

if __name__ == '__main__':
    V = os.path.dirname(os.path.abspath(__file__)); OUT = f'{V}/out'; os.makedirs(OUT, exist_ok=True)
    # line_frac 1.01 = do not remove straight lines (ruku: the inset frame belongs to the picture;
    # sitfront/frontgaze: the frame line was already removed and the head completed by hand).
    LF = {'ruku': 1.01, 'sitfront': 1.01, 'frontgaze': 1.01}
    HEAD = {'stand', 'qiyam'}
    # Small sources (~270 px tall): a softer threshold keeps the thin grey lines (hands, folds) connected.
    THR = {'stand': 185, 'qiyam': 185, 'takbir': 185, 'ruku': 180, 'sajda': 175}
    names = sys.argv[1:] or ['stand', 'takbir', 'qiyam', 'ruku', 'sajda', 'salam', 'sitfront', 'sitgaze', 'frontgaze', 'hand', 'feet', 'feetc']
    for n in names:
        print(n, vectorize(f'{V}/src/{n}.png', n, OUT, line_frac=LF.get(n, 0.42), head=n in HEAD, thr=THR.get(n, 150)), flush=True)
