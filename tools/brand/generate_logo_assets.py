"""Generate every AnaMuslim logo asset from the 2000x2000 master image.

usage: python3 -I tools/brand/generate_logo_assets.py docs/brand/anamuslim-logo-master.jpg <out root> <preview dir>
       (<out root> = . writes straight into the repo; needs pillow, numpy, scipy)

The master is a lit, textured render (brushed metal, soft drop shadows, paper with a light falloff
from the top-right), so colour icons are CUT FROM THE MASTER rather than rebuilt from an extracted
alpha — that keeps the texture, light and shadows. A smooth background model is fitted only to
build the one-colour silhouettes and the transparent PNG.

⚠️ The one-colour icons in the app (Android themed icon `ic_launcher_monochrome`, notification icon
`ic_notification`) are deliberately taken from the earlier FLAT logo
(docs/brand/anamuslim-logo-flat.jpg) — its clean edges read better in one colour than the brushed
metal does. This script only previews its own 3D silhouettes; set WRITE_SILHOUETTES = True to
overwrite the app's files with them.
"""
WRITE_SILHOUETTES = False
import os, sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from scipy import ndimage

SRC, OUT, PREV = sys.argv[1], sys.argv[2], sys.argv[3]
os.makedirs(PREV, exist_ok=True)

master = Image.open(SRC).convert('RGB')
assert master.size == (2000, 2000), master.size
m = np.asarray(master).astype(np.float32)
H = W = 2000
yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)


# ---- background model: quadratic surface fitted to the paper frame around the logo --------------
def _terms(x, y):
    x, y = x / 1000 - 1, y / 1000 - 1
    return np.stack([np.ones_like(x), x, y, x * x, x * y, y * y], axis=-1)


d0 = np.abs(m - np.median(m[:60].reshape(-1, 3), axis=0)).max(axis=2)
ys0, xs0 = np.where(d0 > 60)
frame = np.ones((H, W), bool)
frame[max(0, ys0.min() - 120):ys0.max() + 120, max(0, xs0.min() - 120):xs0.max() + 120] = False
fy, fx = np.where(frame)
fy, fx = fy[::13], fx[::13]
coef, *_ = np.linalg.lstsq(_terms(fx.astype(np.float32), fy.astype(np.float32)), m[fy, fx], rcond=None)
bg_full = _terms(xx, yy) @ coef                                     # (H, W, 3)
resid = np.abs(m[frame] - bg_full[frame]).max(axis=1)
print('bg fit residual p99 =', round(float(np.percentile(resid, 99)), 1))

# ---- ink mask -----------------------------------------------------------------------------------
# Ink = saturated OR dark. Paper stays grey-cream even deep in the emboss shadows (chroma ≈ 17–31,
# brightest channel ≥ ~190); gold and teal metal are chroma ≥ 55, the dark teal lettering is chroma
# ≈ 40 but its brightest channel is < 110. Brightness alone would count the shadows as ink, so the
# darkness test only runs below the cube (y > 1250), where the lettering is — inside the cube the
# deep emboss shadows in the square openings are just as dark.
chroma = m.max(axis=2) - m.min(axis=2)
alpha = np.maximum(np.clip((chroma - 36) / (56 - 36), 0, 1),
                   np.clip((175 - m.max(axis=2)) / (175 - 130), 0, 1) * (yy > 1250))
solid = alpha > 0.5
solid = ndimage.binary_opening(solid, iterations=2)
lab, n = ndimage.label(solid)
sizes = ndimage.sum(solid, lab, range(1, n + 1))
keep = np.zeros(n + 1, bool)
keep[1:] = sizes > 150                                 # paper specks out
solid = keep[lab]
alpha = alpha * ndimage.binary_dilation(solid, iterations=6)
iy, ix = np.where(solid)
content_box = (int(ix.min()), int(iy.min()), int(ix.max()) + 1, int(iy.max()) + 1)
box_cx = (content_box[0] + content_box[2]) / 2
box_cy = (content_box[1] + content_box[3]) / 2
ink_h = content_box[3] - content_box[1]
print('content box', content_box)

# Round masks: keep the logo centred horizontally, slide it vertically halfway towards the height
# where the farthest ink is closest (the «م» tail sticks out bottom-left, the top-right is empty).
sx, sy = ix[::7].astype(np.float32), iy[::7].astype(np.float32)
ys_try = np.arange(box_cy - 160, box_cy + 161, 1.0)
radii = [float(np.sqrt((sx - box_cx) ** 2 + (sy - y) ** 2).max()) for y in ys_try]
round_cy = (float(ys_try[int(np.argmin(radii))]) + box_cy) / 2
ink_r = float(np.sqrt((sx - box_cx) ** 2 + (sy - round_cy) ** 2).max())
print('round centre y', round_cy, 'ink radius', round(ink_r, 1))

# ---- clean one-colour silhouette ----------------------------------------------------------------
# The brushed-metal streaks and specular glints break a raw threshold into scratches, so the
# silhouette is rebuilt from cleaned masks: holes filled, colour classes smoothed by majority vote.
sil_solid = ndimage.binary_closing(solid, iterations=1)
holes = ndimage.binary_fill_holes(sil_solid) & ~sil_solid
hl, hn = ndimage.label(holes)
hsz = ndimage.sum(holes, hl, range(1, hn + 1))
small = np.zeros(hn + 1, bool)
small[1:] = hsz < 600                                   # texture pinholes; real negative spaces are far larger
sil_solid |= small[hl]
cls = np.where(m[..., 2] > m[..., 0] + 12, 1.0, 0.0)    # 1 = teal, 0 = gold
cls = ndimage.uniform_filter(cls * sil_solid, 31) / np.maximum(ndimage.uniform_filter(sil_solid.astype(np.float32), 31), 1e-3)
teal = sil_solid & (cls > 0.5)
gold = sil_solid & (cls <= 0.5)
teal = ndimage.binary_opening(teal, iterations=5)
gold = ndimage.binary_opening(gold, iterations=5)
seam = ndimage.binary_dilation(teal, iterations=7) & ndimage.binary_dilation(gold, iterations=7)
sil_alpha = ndimage.gaussian_filter((sil_solid & ~seam).astype(np.float32), 1.5)

# cube = components whose centre lies above the lettering
lab, n = ndimage.label(solid)
cents = ndimage.center_of_mass(solid, lab, range(1, n + 1))
cube_ids = [i + 1 for i, c in enumerate(cents) if c[0] < 1120]
cube = np.isin(lab, cube_ids)
cube_alpha = sil_alpha * ndimage.binary_dilation(cube, iterations=6)
cy_, cx_ = np.where(cube)
cube_box = (int(cx_.min()), int(cy_.min()), int(cx_.max()) + 1, int(cy_.max()) + 1)
print('cube box', cube_box)

# ---- extended master: the paper continues past the image edge (only the invisible parallax ------
# margin of the Android adaptive icon ever reaches out there)
PAD = 1200
EW = W + 2 * PAD
eyy, exx = np.mgrid[0:EW, 0:EW].astype(np.float32)
ext = _terms(exx - PAD, eyy - PAD) @ coef
edge = np.minimum.reduce([xx, yy, W - 1 - xx, H - 1 - yy])
wgt = np.clip(edge / 80, 0, 1)[..., None]
ext[PAD:PAD + H, PAD:PAD + W] = m * wgt + ext[PAD:PAD + H, PAD:PAD + W] * (1 - wgt)
ext_img = Image.fromarray(np.clip(ext, 0, 255).astype(np.uint8))
sil_img = Image.fromarray((sil_alpha * 255).astype(np.uint8))
CREAM = tuple(int(v) for v in (_terms(np.float32(box_cx), np.float32(box_cy)) @ coef).round())
GOLD = (196, 152, 70)


def window(cx, cy, side, px, src=ext_img, pad=PAD):
    """Square crop of the (extended) master centred on (cx, cy) master px, resized to px."""
    box = (cx - side / 2 + pad, cy - side / 2 + pad, cx + side / 2 + pad, cy + side / 2 + pad)
    return src.resize((px, px), Image.LANCZOS, box=box)


def sil_window(alpha_img, cx, cy, side, px):
    """Same crop of a silhouette (transparent outside the master) as white RGBA."""
    a = Image.new('L', (W + 2 * PAD, H + 2 * PAD), 0)
    a.paste(alpha_img, (PAD, PAD))
    a = window(cx, cy, side, px, src=a)
    out = Image.new('RGBA', (px, px), (255, 255, 255, 0))
    out.putalpha(a)
    return out


def save(img, path):
    full = os.path.join(OUT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    if path.endswith('.webp'):
        img.save(full, 'WEBP', lossless=True, method=6)
    else:
        img.save(full, 'PNG', optimize=True)


def masked(img, shape, inset_frac=0.0, radius_frac=0.0, ss=4):
    px = img.size[0]
    S = px * ss
    mk = Image.new('L', (S, S), 0)
    d = ImageDraw.Draw(mk)
    i = S * inset_frac
    b = [i, i, S - i - 1, S - i - 1]
    (d.ellipse if shape == 'circle' else lambda bb, fill: d.rounded_rectangle(bb, radius=S * radius_frac, fill=fill))(b, fill=255)
    out = img.convert('RGBA')
    out.putalpha(mk.resize((px, px), Image.LANCZOS))
    return out


ANDROID = {'mdpi': 1, 'hdpi': 1.5, 'xhdpi': 2, 'xxhdpi': 3, 'xxxhdpi': 4}

# ---- 1. Android adaptive icon: opaque, full-bleed foreground cut from the master ----------------
# Circle masks show a 72dp disc on the 108dp canvas (radius = 144/432). Farthest ink at 141/432.
side_adaptive = ink_r * 432 / 141
for dens, k in ANDROID.items():
    px = round(108 * k)
    save(window(box_cx, round_cy, side_adaptive, px), f'app/src/main/res/mipmap-{dens}/ic_launcher_foreground.webp')
    if WRITE_SILHOUETTES:
        save(sil_window(sil_img, box_cx, round_cy, side_adaptive, px), f'app/src/main/res/mipmap-{dens}/ic_launcher_monochrome.webp')
window(box_cx, round_cy, side_adaptive, 432).save(os.path.join(PREV, 'adaptive_fg.png'))
sil_window(sil_img, box_cx, round_cy, side_adaptive, 432).save(os.path.join(PREV, 'monochrome.png'))

# legacy launcher icons (API 24–25)
for dens, k in ANDROID.items():
    px = round(48 * k)
    inner = 44 / 48
    sq = window(box_cx, box_cy, ink_h / 0.80 / inner, px)
    save(masked(sq, 'rounded', inset_frac=2 / 48, radius_frac=0.12), f'app/src/main/res/mipmap-{dens}/ic_launcher.webp')
    rd = window(box_cx, round_cy, 2 * ink_r / 0.90 / inner, px)
    save(masked(rd, 'circle', inset_frac=2 / 48), f'app/src/main/res/mipmap-{dens}/ic_launcher_round.webp')


# ---- 2. Circular badge (splash, About, share cards, widgets): same proportions as before --------
def badge(px, circle_frac=0.62):
    ss = 4
    S = px * ss
    d_px = S * circle_frac
    disc = window(box_cx, round_cy, 2 * ink_r / 0.88, round(d_px))
    canvas = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    o = round((S - d_px) / 2)
    canvas.paste(masked(disc, 'circle', ss=1), (o, o))
    ring = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(ring).ellipse([o, o, o + round(d_px) - 1, o + round(d_px) - 1], outline=GOLD + (255,),
                                 width=max(1, round(d_px * 0.018)))
    canvas.alpha_composite(ring)
    return canvas.resize((px, px), Image.LANCZOS)


save(badge(432), 'app/src/main/res/drawable-xxxhdpi/ic_launcher_foreground.webp')
save(badge(512), 'shared/src/commonMain/composeResources/drawable/ic_launcher_foreground.webp')
badge(432).save(os.path.join(PREV, 'badge.png'))
for suf, px in {'': 256, '@2x': 512, '@3x': 768}.items():
    save(badge(px, circle_frac=0.86), f'iosApp/iosApp/Assets.xcassets/LaunchLogo.imageset/LaunchLogo{suf}.png')

# ---- 3. Notification small icon: white cube silhouette, 24dp with 1dp padding --------------------
cube_img = Image.fromarray((cube_alpha * 255).astype(np.uint8))
cside = max(cube_box[2] - cube_box[0], cube_box[3] - cube_box[1]) * 24 / 22
ccx, ccy = (cube_box[0] + cube_box[2]) / 2, (cube_box[1] + cube_box[3]) / 2
for dens, k in ANDROID.items():
    px = round(24 * k)
    if WRITE_SILHOUETTES:
        save(sil_window(cube_img, ccx, ccy, cside, px), f'app/src/main/res/drawable-{dens}/ic_notification.webp')
sil_window(cube_img, ccx, ccy, cside, 96).save(os.path.join(PREV, 'notification.png'))

# ---- 4. Square store / iOS icons: ink height 80 % of the icon, opaque ---------------------------
side_sq = ink_h / 0.80
save(window(box_cx, box_cy, side_sq, 1024).convert('RGB'), 'iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png')
store = window(box_cx, box_cy, side_sq, 512).convert('RGB')
save(store, 'app/src/main/ic_launcher-playstore.png')
for lang in ('ar', 'az', 'en-US', 'ru', 'tr'):
    save(store, f'fastlane/metadata/android/{lang}/images/icon.png')

# ---- 5. transparent master (no shadows) for docs ------------------------------------------------
save(Image.fromarray(np.dstack([m, alpha * 255]).astype(np.uint8)), 'docs/brand/anamuslim-logo-transparent.png')

print('cream', '#%02X%02X%02X' % CREAM)
