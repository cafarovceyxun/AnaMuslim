# -*- coding: utf-8 -*-
"""Qur'an skript atlası — öz fontumuzdan, öz məlumat bazamızdan.

Atlas paketi tətbiqin mushaf rendering-i üçün **əvvəlcədən hesablanmış shaping keşidir**: HarfBuzz-un
hər söz forması üçün verdiyi qlif axını, üstəgəl həmin qliflərin rasterləşdirilmiş teksturu. Tətbiq
beləliklə nə shaping mühərriki daşıyır, nə də işləmə vaxtında rasterləşdirir.

Əvvəl bu paketlər `AlfaazPlus/QuranAppInventory`-dən endirilirdi. Bura köçürülməsinin səbəbi
müstəqillikdir, amma **giriş məlumatının hamısı onsuz da bizdə idi**:

  • söz siyahısı → `app/src/main/assets/db/quranapp.db`, `ayah_words` cədvəli
  • font        → `shared/src/commonMain/composeResources/font/uthmanic_hafs.ttf` (KFGQPC)

⚠️ Sxem `schema_version: 1`, `kind: "word_glyph_atlas"`-dır: `meta.json` + `atlas.json` +
`layout.json` + `atlas_<i>.png`. Müqaviləni `QuranAtlasModel.kt` və `AtlasLayoutParser` təyin edir.
QuranAppInventory-dəki paketlərin bir hissəsi **köhnə sxemdədir** (`words.json`, tək `atlas.png`) —
onlara baxıb format çıxarma, tətbiq onları oxumur.

⚠️ Bayt-eyni çıxış HƏDƏF DEYİL və gözlənilməməlidir. Referans başqa rasterizator işlədib: qlif
ölçüləri bəzən 1–2 piksel fərqlənir. Bunun əhəmiyyəti yoxdur, çünki `atlas.json` hər qlifin
düzbucaqlısını və bearing-ini **özü elan edir** — renderer həmin dəyərləri oxuyur, sabit gözləmir.
Vacib olan daxili uyğunluqdur: paketlənən şəkil ilə yazılan metrika eyni mənbədən gəlməlidir.

✅ Shaping isə bire-bir eynidir — 200 sözdə yoxlanıldı, hamısı dəqiq uyğun gəldi. Səbəb aydındır:
HarfBuzz + eyni font = eyni nəticə. Deməli `words.json` yuxarı axınla tam əvəzlənə bilər.

İşlətmək:
    python3 gen_atlas.py --script uthmani
    python3 gen_atlas.py --script uthmani --verify köhnə/6x.zip

Asılılıqlar: `pip install -r requirements.txt`
"""
import argparse, io, json, os, sqlite3, zipfile

import freetype
import uharfbuzz as hb
from PIL import Image

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
DB = os.path.join(REPO, "app/src/main/assets/db/quranapp.db")
FONT_DIR = os.path.join(REPO, "shared/src/commonMain/composeResources/font")

# Skript → font. Yalnız burada sadalananlar qurula bilər: qalan skriptlərin fontu repoda yoxdur.
FONTS = {
    "uthmani": "uthmanic_hafs.ttf",
}

# Yuxarı axının paketləri ilə eyni: 32 ppem baza, 6x variant → 192 ppem.
BASE_PPEM = 32
PADDING = 2
MAX_WIDTH = 4096


def word_forms(script_code):
    """Skriptin unikal söz formaları — atlasın giriş dəsti."""
    con = sqlite3.connect(DB)
    row = con.execute("select script_id from scripts where code = ?", (script_code,)).fetchone()
    if row is None:
        raise SystemExit(f"bazada belə skript yoxdur: {script_code}")
    words = [r[0] for r in con.execute(
        "select distinct text from ayah_words where script_id = ? order by text", (row[0],))]
    con.close()
    if not words:
        raise SystemExit(f"{script_code} üçün söz tapılmadı")
    return words


def shape_all(font_path, words):
    """Hər sözü qlif axınına çevirir. Mövqelər **font vahidindədir** (piksel deyil) — `words.json`
    ölçüdən asılı olmasın deyə, çünki eyni fayl bütün ppem variantlarına xidmət edir."""
    blob = hb.Blob.from_file_path(font_path)
    face = hb.Face(blob)
    font = hb.Font(face)
    font.scale = (face.upem, face.upem)

    shapes, used = {}, set()
    for word in words:
        buf = hb.Buffer()
        buf.add_str(word)
        # `guess_segment_properties` yazıdan istiqaməti özü çıxarır (ərəbcə → RTL). Əl ilə
        # təyin etmək lazım deyil və səhv təyin etmək shaping-i sakitcə pozar.
        buf.guess_segment_properties()
        hb.shape(font, buf)

        run = []
        for info, pos in zip(buf.glyph_infos, buf.glyph_positions):
            run.append({"g": info.codepoint, "xa": pos.x_advance, "ya": pos.y_advance,
                        "xo": pos.x_offset, "yo": pos.y_offset})
            used.add(info.codepoint)
        shapes[word] = run

    return shapes, sorted(used)


def raster(font_path, glyph_ids, ppem):
    """Hər qlifi [ppem]-də boz-tonlu rasterləşdirir; `(gid → (metrika, şəkil))` qaytarır.

    ⚠️ `FT_LOAD_NO_HINTING` şərtdir: hinting qlifi piksel şəbəkəsinə oturtmaq üçün konturu
    **əyir**, halbuki mövqeləmə font vahidində hesablanıb. Hinting açıq qalsa hərflər sözün
    içində sürüşür — kompilyator da, test də bunu görmür.
    """
    face = freetype.Face(font_path)
    face.set_pixel_sizes(0, ppem)
    flags = freetype.FT_LOAD_RENDER | freetype.FT_LOAD_NO_HINTING

    out = {}
    for gid in glyph_ids:
        face.load_glyph(gid, flags)
        slot = face.glyph
        bmp = slot.bitmap
        w, h = bmp.width, bmp.rows

        img = None
        if w and h:
            # `bmp.buffer` sətir-sətir `pitch` addımı ilə gəlir; pitch > width ola bilər.
            data = bytes(bytearray(bmp.buffer))
            rows = [data[y * bmp.pitch:y * bmp.pitch + w] for y in range(h)]
            img = Image.frombytes("L", (w, h), b"".join(rows))

        out[gid] = ({
            "w": w, "h": h,
            "bearing_x": slot.bitmap_left,
            "bearing_y": slot.bitmap_top,
            "advance": slot.advance.x / 64.0,
        }, img)

    return out


def pack(rasters):
    """Sadə rəf (shelf) paketləməsi: hündürlüyə görə azalan sıra, sabit en, sətir-sətir düzülüş.

    Optimal deyil, amma atlas bir dəfə qurulur və nəticə `atlas.json`-da açıq yazılır — yerləşdirmə
    alqoritmi tətbiq tərəfi üçün görünməzdir. Sadəliyi doğruluğundan vacibdir.
    """
    boxes = [(gid, m["w"], m["h"]) for gid, (m, img) in rasters.items() if m["w"] and m["h"]]
    boxes.sort(key=lambda b: (-b[2], -b[1]))

    placed, x, y, shelf_h = {}, 0, 0, 0
    for gid, w, h in boxes:
        if x + w + PADDING > MAX_WIDTH:
            x, y, shelf_h = 0, y + shelf_h + PADDING, 0
        placed[gid] = (x, y)
        x += w + PADDING
        shelf_h = max(shelf_h, h)

    width = max((px + rasters[g][0]["w"] for g, (px, py) in placed.items()), default=1)
    height = y + shelf_h
    return placed, width, height


def build(script, out_dir, scale):
    font_name = FONTS.get(script)
    if font_name is None:
        raise SystemExit(f"{script} üçün font təyin edilməyib (FONTS cədvəlinə bax)")
    font_path = os.path.join(FONT_DIR, font_name)
    ppem = BASE_PPEM * scale

    words = word_forms(script)
    shapes, used = shape_all(font_path, words)
    rasters = raster(font_path, used, ppem)
    placed, width, height = pack(rasters)

    sheet = Image.new("L", (max(width, 1), max(height, 1)), 0)
    for gid, (px, py) in placed.items():
        img = rasters[gid][1]
        if img is not None:
            sheet.paste(img, (px, py))

    glyphs = {}
    for gid, (m, _img) in rasters.items():
        px, py = placed.get(gid, (0, 0))
        glyphs[str(gid)] = {"atlas": 0, "x": px, "y": py, "w": m["w"], "h": m["h"],
                            "bearing_x": m["bearing_x"], "bearing_y": m["bearing_y"],
                            "advance": m["advance"]}

    ft = freetype.Face(font_path)
    upem, asc, desc = ft.units_per_EM, ft.ascender, ft.descender
    texture = {"index": 0, "width": sheet.width, "height": sheet.height, "padding": PADDING,
               "channels": "L", "format": "png", "image": "atlas_0.png"}

    meta = {
        "schema_version": 1,
        "kind": "word_glyph_atlas",
        "font": {"units_per_em": upem, "ascender_fu": asc, "descender_fu": desc,
                 "height_fu": ft.height, "line_gap_fu": max(ft.height - (asc - desc), 0)},
        "base_ppem": BASE_PPEM,
        "layout": {"kind": "words", "file": "layout.json"},
        # ⚠️ `sizes[].atlas` FAYL ADIDIR, ölçü deyil: importer məhz bu adla `atlas.json`-u açır.
        "sizes": [{"label": f"{scale}x", "scale": scale, "ppem": ppem,
                   "atlas": "atlas.json", "textures": [texture]}],
        "bundle": {"scale": f"{scale}x", "ppem": ppem},
    }
    layer = {"schema_version": 1, "ppem": ppem, "textures": [texture], "glyphs": glyphs}

    # `documents` açarı istifadə olunmur (parser onu atır), amma JSON obyekt olmalıdır.
    layout = {"schema_version": 1,
              "documents": {str(i): {"text": w, "glyphs": shapes[w]}
                            for i, w in enumerate(sorted(shapes))}}

    os.makedirs(out_dir, exist_ok=True)
    png = io.BytesIO()
    sheet.save(png, format="PNG", optimize=True)

    zip_path = os.path.join(out_dir, f"{scale}x.zip")
    # Sabit tarix: eyni giriş eyni faylı versin, təkrar qurma boş fərq yaratmasın.
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for name, payload in (
            ("layout.json", json.dumps(layout, ensure_ascii=False, separators=(",", ":")).encode()),
            ("meta.json", json.dumps(meta, indent=2).encode()),
            ("atlas.json", json.dumps(layer, separators=(",", ":")).encode()),
            ("atlas_0.png", png.getvalue()),
        ):
            info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(info, payload)

    print(f"{zip_path}: {len(words)} söz, {len(used)} qlif, atlas {sheet.width}×{sheet.height}, "
          f"{os.path.getsize(zip_path) // 1024} KB")
    return shapes, glyphs


def verify(shapes, glyphs, reference_zip):
    """Köhnə paketlə tutuşdurma — shaping eyni olmalıdır, rasterləşdirmə isə yaxın."""
    with zipfile.ZipFile(reference_zip) as z:
        names = z.namelist()
        if "layout.json" in names:                       # cari sxem
            docs = json.loads(z.read("layout.json"))["documents"]
            ref_words = {d["text"]: d["glyphs"] for d in docs.values()}
        else:                                            # köhnə sxem (QuranAppInventory)
            ref_words = json.loads(z.read("words.json"))
        ref_glyphs = json.loads(z.read("atlas.json"))["glyphs"]

    # Referansın qlif axınında əlavə `v` sahəsi ola bilər; müqayisə yalnız modeldəki sahələr üzrə.
    keys = ("g", "xa", "ya", "xo", "yo")
    ref_words = {w: [{k: p[k] for k in keys} for p in run] for w, run in ref_words.items()}

    same = sum(1 for w, v in shapes.items() if ref_words.get(w) == v)
    print(f"  söz dəsti : bizdə {len(shapes)}, referansda {len(ref_words)}, "
          f"kəsişmə {len(set(shapes) & set(ref_words))}")
    print(f"  shaping   : {same}/{len(shapes)} dəqiq uyğun")

    common = set(glyphs) & set(ref_glyphs)
    exact = sum(1 for g in common
                if all(glyphs[g][k] == ref_glyphs[g][k] for k in ("w", "h", "bearing_x", "bearing_y")))
    close = sum(1 for g in common
                if all(abs(glyphs[g][k] - ref_glyphs[g][k]) <= 1
                       for k in ("w", "h", "bearing_x", "bearing_y")))
    print(f"  qlif      : {len(common)} ortaq, {exact} dəqiq, {close} ±1 piksel daxilində")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--script", default="uthmani", choices=sorted(FONTS))
    ap.add_argument("--scale", type=int, default=6)
    ap.add_argument("--out", default=None,
                    help="default: composeResources/files/atlas/<script> (paketdəki atlas)")
    ap.add_argument("--verify", default=None, help="köhnə paketlə tutuşdur (zip)")
    args = ap.parse_args()

    # uthmani atlası paketin İÇİNDƏDİR (`isPrebuiltAtlas`), endirilmir — ona görə default
    # çıxış composeResources-dur, `inventory/` deyil.
    out = args.out or os.path.join(
        REPO, "shared/src/commonMain/composeResources/files/atlas", args.script)
    shapes, glyphs = build(args.script, out, args.scale)
    if args.verify:
        verify(shapes, glyphs, args.verify)


if __name__ == "__main__":
    main()
