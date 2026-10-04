"""Təyəmmüm və qüsl kadrları (2026-10-04) — istifadəçinin boyama vərəqlərindən (uşaq fiquru, namaz duruşları ilə
eyni üslub). `src/tayammum_*.png`, `src/ghusl_*.png` həmin vərəqlərdən kəsilmiş kadrlardır (nömrə, çərçivə,
palitra, qələmlər artıq kənardadır). Kitaba uyğunlaşdırmaq üçün düzəlişlər:

Təyəmmüm (Ammar, № 177–182):
- **face** — üzün yanındakı iki dairəvi ox silinir: məsh üzdən bir dəfə keçməkdir, oxlar isə ovuşdurmağı
  (fırlatmağı) göstərirdi.
- **blow** — kadrda əllər sadəcə birləşib, üfürmək görünmürdü: ağızla ovuclar arasına üç qısa hava xətti çəkilir.

Qüsl (№ 134–158):
- **hands / wudu / three** — üzdəki burun və ağız işarələri silinir: qalan bütün kadrlar üzsüzdür, bunlar isə
  qaşqabaqlı görünürdü.
- Vərəqin 4 və 5-ci kadrları eynidir — biri (**body**) götürülüb. Mavi su rəngi eşikdə özü ağarır.

İşlətmək: python3 sheets.py → `composeResources/drawable/dr_salah_{tayammum,ghusl}_*.xml`.
"""
import math, os, shutil, sys
import vec

V = os.path.dirname(os.path.abspath(__file__))
OUT = f'{V}/out'
DRAWABLE = os.path.normpath(f'{V}/../../../shared/src/commonMain/composeResources/drawable')


def remove_in(*rects, thr=150):
    """Bütünlüklə [rects]-in birinin içində qalan mürəkkəb parçalarını silir (fiqurun xəttinə toxunmur)."""
    def prep(w, h, g):
        mask = [[g[y][x] < thr for x in range(w)] for y in range(h)]
        for pts in vec.components(mask, w, h):
            xs = [p[0] for p in pts]; ys = [p[1] for p in pts]
            if any(x0 <= min(xs) and max(xs) <= x1 and y0 <= min(ys) and max(ys) <= y1 for x0, y0, x1, y1 in rects):
                for x, y in pts: g[y][x] = 255
    return prep


def strokes(lines, width=1.9):
    """Kvadratik əyrilər (başlanğıc, nəzarət, son) — mənbənin xətt qalınlığında, kənarı yumşaldılmış."""
    def prep(w, h, g):
        r = width / 2
        for (ax, ay), (cx, cy), (bx, by) in lines:
            pts = [((1-t)**2*ax + 2*(1-t)*t*cx + t*t*bx, (1-t)**2*ay + 2*(1-t)*t*cy + t*t*by) for t in [i / 40 for i in range(41)]]
            for y in range(max(0, int(min(p[1] for p in pts) - 3)), min(h, int(max(p[1] for p in pts) + 4))):
                for x in range(max(0, int(min(p[0] for p in pts) - 3)), min(w, int(max(p[0] for p in pts) + 4))):
                    d = min(math.hypot(x - px, y - py) for px, py in pts)
                    if d < r + 1:
                        g[y][x] = min(g[y][x], int(max(0.0, d - r) * 255))
    return prep


PREP = {
    'tayammum_face': remove_in((140, 74, 162, 112), (13, 108, 43, 142)),
    # Çənənin altından barmaq uclarına qədər, hər biri iki qırıq parça — «hava» kimi oxunur, yağış kimi yox.
    'tayammum_blow': strokes([
        ((156, 133), (156, 139), (157, 145)), ((157, 151), (158, 157), (159, 163)),
        ((163, 131), (164, 137), (165, 143)), ((166, 149), (167, 155), (169, 161)),
        ((170, 129), (172, 135), (173, 141)), ((175, 147), (176, 152), (178, 158)),
    ]),
    'ghusl_hands': remove_in((98, 89, 105, 99), (88, 103, 103, 109)),
    'ghusl_wudu': remove_in((84, 90, 90, 99), (91, 102, 104, 107)),
    'ghusl_three': remove_in((23, 234, 29, 243), (28, 248, 41, 253)),
}


# ---------------------------------------------------------------------------------------------
# «Sağ, sol, orta» (№ 141) — vərəqdə kadr yoxdur, «üç ovuc» kadrından qurulur: orta əl və axın yerində
# qalır, yuxarı əl güzgülənib başın sağına (uşaq bizə baxır → bizim solumuz), aşağı əl soluna keçir,
# hər birinə qısa su axını və sıra nömrəsi (1 sağ, 2 sol, 3 orta) çəkilir.
# ---------------------------------------------------------------------------------------------

SIDES_PAD = 60  # kətan hər tərəfdən genişlənir ki, yan əllər sığsın


def make_sides():
    w, h, g = vec.gray(f'{V}/src/ghusl_three.png')
    PREP['ghusl_three'](w, h, g)
    mask = [[g[y][x] < 150 for x in range(w)] for y in range(h)]
    hands = {}
    for pts in vec.components(mask, w, h):
        xs = [p[0] for p in pts]; ys = [p[1] for p in pts]
        box = (min(xs), min(ys), max(xs), max(ys))
        if min(xs) >= 140:  # vərəqin bəzək çərçivəsindən düşən nöqtələr
            for x, y in pts: g[y][x] = 255
        elif box[1] < 75 and box[3] < 75: hands['top'] = pts
        elif box[1] > 100 and box[3] < 152 and box[0] > 60: hands['bottom'] = pts

    def grab(pts):
        # Antialias kənarı da götürülür: parçanın 2 px ətrafındakı boz piksellər.
        sel = set()
        for x, y in pts:
            for dx in range(-2, 3):
                for dy in range(-2, 3):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h and g[ny][nx] < 245: sel.add((nx, ny))
        cut = {(x, y): g[y][x] for x, y in sel}
        for x, y in sel: g[y][x] = 255
        return cut

    top, bottom = grab(hands['top']), grab(hands['bottom'])
    W = w + 2 * SIDES_PAD
    G = [[255] * W for _ in range(h)]
    for y in range(h):
        for x in range(w):
            G[y][x + SIDES_PAD] = g[y][x]

    def paste(cut, dx, dy, mirror=False):
        xs = [x for x, _ in cut]; x0, x1 = min(xs), max(xs)
        for (x, y), v in cut.items():
            nx = (x1 - (x - x0) if mirror else x) + dx
            ny = y + dy
            if 0 <= nx < W and 0 <= ny < h: G[ny][nx] = min(G[ny][nx], v)

    # Yuxarı əl (ovuc solda, qol sağda) güzgülənir: ovuc sağa baxır, başın sol kənarının üstünə düşür.
    paste(top, dx=-57 + 4, dy=78, mirror=True)
    # Aşağı əl olduğu kimi başın sağ kənarının üstünə.
    paste(bottom, dx=SIDES_PAD + 32, dy=-6)
    rows = [bytes(v for v in row) for row in G]
    import png
    png.write(f'{V}/src/ghusl_sides.png', W, h, 1, rows)


def digit(ch, x, y, k=1.0):
    """Sıra nömrəsi kvadratik əyrilərlə — 8×12 qutu, ([x], [y]) yuxarı sol künc."""
    P = lambda a, b: (x + a * k, y + b * k)
    L = lambda a, b: (a, ((a[0] + b[0]) / 2, (a[1] + b[1]) / 2), b)
    shapes = {
        '1': [L(P(1, 3), P(4.5, 0)), L(P(4.5, 0), P(4.5, 12))],
        '2': [(P(0.5, 3.5), P(1, 0), P(4, 0)), (P(4, 0), P(7.5, 0), P(7, 4)), L(P(7, 4), P(0.5, 12)), L(P(0.5, 12), P(7.5, 12))],
        '3': [(P(0.5, 1.5), P(4, -1.5), P(6.5, 1.5)), (P(6.5, 1.5), P(7.5, 5.5), P(3, 5.8)),
              (P(3, 5.8), P(8, 6), P(7, 9.5)), (P(7, 9.5), P(5, 13.5), P(0.5, 10.8))],
    }
    return shapes[ch]


PREP['ghusl_sides'] = strokes([
    # Sağ (bizim solumuz): ovucdan başın sol kənarına iki xətli axın.
    ((66, 142), (63, 158), (70, 174)), ((73, 141), (71, 156), (77, 171)),
    # Sol (bizim sağımız).
    ((161, 146), (165, 161), (157, 178)), ((168, 145), (172, 159), (164, 175)),
] + digit('1', 30, 92) + digit('2', 192, 84) + digit('3', 130, 50))

NAMES = [f'tayammum_{n}' for n in ['strike', 'blow', 'face', 'hands']] + \
        [f'ghusl_{n}' for n in ['hands', 'wudu', 'three', 'sides', 'body', 'hair']]

if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    make_sides()
    for name in sys.argv[1:] or NAMES:
        print(name, vec.vectorize(f'{V}/src/{name}.png', name, OUT, prep=PREP.get(name)), flush=True)
        shutil.copy(f'{OUT}/{name}.xml', f'{DRAWABLE}/dr_salah_{name}.xml')
