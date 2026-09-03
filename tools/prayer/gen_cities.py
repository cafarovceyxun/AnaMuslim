# -*- coding: utf-8 -*-
"""GeoNames → AnaMuslim şəhər kataloqları.

İKİ çıxış verir, eyni seçim/qatlama məntiqi ilə:

  1. `cities.tsv` — **paketdəki** siyahı (`cities15000`).
     Seçim: bütün Azərbaycan + bütün paytaxtlar + region (TR/RU/GE/IR/KZ/UZ/TM/TJ/KG/AM) >= 100k
     + dünya >= 200k.  Bu, GPS-siz/internetsiz hallar üçün son dayaqdır və APK/IPA-nın içindədir.

  2. `cities-v<N>.tsv.gz` + `cities.json` — **endirilən** genişləndirilmiş kataloq (`cities5000`).
     `inventory/prayer/`-ə commit olunur, tətbiq ilk açılışda `ghraw://` ilə çəkir
     (`CityCatalogStore`) — yəni istifadəçinin mirror seçimi (gh-proxy / raw / jsdelivr) işləyir.
     Kiçik yaşayış məntəqələri buradadır: Gədəbəy (≈14 500) `cities15000`-də YOXDUR,
     `cities5000`-dədir.

Sıralama hər ikisində əhaliyə görə azalan — `CityCatalog` prefiks axtarışı beləliklə böyük
şəhərləri əvvəl qaytarır, ayrıca əhali sütunu saxlamadan.

İşlətmək:
    python3 gen_cities.py                      # yalnız paketdəki siyahı
    python3 gen_cities.py --full --version 1   # hər ikisi
"""
import argparse, gzip, json, unicodedata, os
from az_names import AZ_NAMES

# GeoNames `alternateNamesV2` (194 MB) süzülüb: seçilmiş şəhərlərin az/tr/ru adları.
# Fayl repoda saxlanılır ki, təkrar generasiya böyük yükləmə tələb etməsin.
ALT = {}
if os.path.exists("alt_names.tsv"):
    for _line in open("alt_names.tsv", encoding="utf-8"):
        _gid, _lang, _name = _line.rstrip("\n").split("\t")
        ALT.setdefault(_gid, {})[_lang] = _name

RU_SPEAKING = {"RU","AZ","GE","AM","KZ","UZ","KG","TJ","TM","BY","UA","MD"}
REGION = {"TR","RU","GE","IR","KZ","UZ","TM","TJ","KG","AM"}
WORLD_MIN, REGION_MIN = 200000, 100000

# ASCII qatlama — Kotlin tərəfdəki CityIndex.fold() ilə EYNİ cədvəl olmalıdır.
EXTRA = {
    "ə":"e","Ə":"e","ı":"i","İ":"i","ğ":"g","Ğ":"g","ş":"s","Ş":"s","ç":"c","Ç":"c",
    "ö":"o","Ö":"o","ü":"u","Ü":"u","ø":"o","Ø":"o","æ":"ae","Æ":"ae","œ":"oe","Œ":"oe",
    "ß":"ss","đ":"d","Đ":"d","ð":"d","Ð":"d","ł":"l","Ł":"l","þ":"th","Þ":"th","å":"a","Å":"a",
}
CYR2LAT = {
    "а":"a","б":"b","в":"v","г":"g","д":"d","е":"e","ё":"e","ж":"zh","з":"z","и":"i","й":"y",
    "к":"k","л":"l","м":"m","н":"n","о":"o","п":"p","р":"r","с":"s","т":"t","у":"u","ф":"f",
    "х":"h","ц":"ts","ч":"ch","ш":"sh","щ":"sch","ъ":"","ы":"i","ь":"","э":"e","ю":"yu","я":"ya",
}

def fold(text):
    out = []
    for ch in text:
        low = ch.lower()
        if low in EXTRA:      out.append(EXTRA[low]); continue
        if low in CYR2LAT:    out.append(CYR2LAT[low]); continue
        d = unicodedata.normalize("NFKD", low)
        d = "".join(c for c in d if not unicodedata.combining(c))
        out.append(d if d.isascii() else "")
    s = "".join(out).lower()
    s = "".join(c if (c.isalnum() or c == " ") else " " for c in s)
    return " ".join(s.split())   # ⚠️ CityCatalog.fold() ilə eyni olmalıdır (o da boşluqları sıxır)

def is_latin(text):
    """Latın yazısıdırmı. ⚠️ `ord(c) < 0x0250` yoxlaması SINANDI və ATILDI: Azərbaycan «ə»
    (U+0259, IPA Extensions) həmin həddin üstündədir, ona görə «Gəncə» qatlanmır və «gence»
    axtarışı şəhəri tapmırdı."""
    for c in text:
        if not c.isalpha():          # durğu işarəsi/rəqəm/boşluq — qatlama onsuz da atır
            continue
        try:
            if not unicodedata.name(c).startswith("LATIN"):
                return False
        except ValueError:
            return False
    return True

RU_LETTERS = set("абвгдеёжзийклмнопрстуфхцчшщъыьэюяАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ")
SEPARATORS = set(" -'’")

def is_pure_russian(text):
    """Yalnız rus əlifbası. Qazax/serb/uyğur variantları translit cədvəlində yoxdur və
    qatlamada zibil verir («Guaңҗu»), ona görə kənarda qalır."""
    letters = [c for c in text if c not in SEPARATORS]
    return len(letters) >= 3 and all(c in RU_LETTERS for c in letters)

def is_pure_arabic(text):
    """Yalnız ərəb bloku. Qarışıq sətir («shngھayy») rədd edilir."""
    letters = [c for c in text if c not in SEPARATORS]
    return len(letters) >= 3 and all(0x0600 <= ord(c) <= 0x06FF or 0x0750 <= ord(c) <= 0x077F
                                     for c in letters)


def read_dump(path):
    """GeoNames `cities*.txt` sətirlərini sahələrə bölür; qısa sətirləri atır."""
    with open(path, encoding="utf-8") as fh:
        for line in fh:
            c = line.rstrip("\n").split("\t")
            if len(c) >= 18:
                yield c


def build_rows(source, keep):
    """[keep] süzgəcindən keçən şəhərləri `(pop, ad, qatlamalar, ölkə, en, uz, hünd)` kimi verir.

    [keep] imzası: `(cc, fcode, pop) -> bool`.  İki keçid var — birincisi hər ölkədə əsas adların
    qatlamasını yığır ki, ikincidə alias toqquşması tutulsun.
    """
    # 1-ci keçid: hər ölkədə əsas (göstərilən/beynəlxalq) adların qatlaması — alias toqquşmasını
    # tutmaq üçün. GeoNames-in `az` sahəsində Yevlax üçün «Gəncə» yazılıb; belə alias istifadəçini
    # «gence» axtaranda YANLIŞ şəhərə aparır.
    primary_by_country = {}
    for c in read_dump(source):
        for cand in (AZ_NAMES.get(c[2], c[1]) if c[8] == "AZ" else c[1], c[1], c[2]):
            f = fold(cand)
            if f:
                primary_by_country.setdefault(c[8], {}).setdefault(f, set()).add(c[0])

    rows = []
    for c in read_dump(source):
        gid, name, ascii_name, alts = c[0], c[1], c[2], c[3]
        lat, lng, fcode, cc = c[4], c[5], c[7], c[8]
        pop = int(c[14] or 0)

        # Hündürlük: əvvəlcə ölçülmüş `elevation` (15), yoxdursa relyef modeli `dem` (16).
        # `dem` çatışmayanda -9999 verir; mənfi/absurd dəyər 0-a sıxılır (Ölü dəniz sahilində
        # düzəliş onsuz da sıfıra yaxındır, kvadrat kök isə mənfi ədəd qəbul etmir).
        elev = 0
        for raw in (c[15] if len(c) > 15 else "", c[16] if len(c) > 16 else ""):
            v = int(raw) if raw.lstrip("-").isdigit() else None
            if v is not None and 0 <= v <= 9000:
                elev = v
                break

        if not keep(cc, fcode, pop):
            continue

        # ⚠️ Evristika («ilk azərbaycan hərfli variant») sınandı və ATILDI: Bakı üçün türkcə
        # «Bakü», Gəncə üçün «Ganja» seçirdi. Ana bazar 65 şəhərdir, ona görə əl ilə yoxlanmış
        # cədvəl var; qalan ölkələr üçün GeoNames-in beynəlxalq adı saxlanılır.
        # GÖSTƏRİLƏN ad: Azərbaycan şəhərləri üçün əl ilə yoxlanmış cədvəl, qalanları üçün
        # GeoNames-in beynəlxalq adı.
        #
        # ⚠️ GeoNames-in `az` adları GÖSTƏRMƏK üçün İŞLƏDİLMİR. 65 Azərbaycan şəhəri üzərində
        # tutuşdurdum: 57-si üst-üstə düşdü, 7-si fərqləndi və HƏR YEDDİSİNDƏ GeoNames pisdir —
        # «Şamxor» (sovet adı, 1991-dən Şəmkir), «Kürdemir»/«Ağcabedi»/«Xirdalan» (əskik ə/ı),
        # «Divichibazar» (2010-dan Şabran) və ən pisi: Yevlax üçün **«Gəncə»** yazılıb.
        # Bu nisbət (~1.6 %) 1175 az adına şamil olsaydı ~18 şəhər səhv adla görünərdi.
        # Ona görə az/tr/ru adları yalnız AXTARIŞ ALİASI kimi qatlamaya girir (aşağıda):
        # səhv alias ən pis halda artıq nəticə verir, səhv ad isə istifadəçini çaşdırır.
        display = AZ_NAMES.get(ascii_name, name) if cc == "AZ" else name

        localized = ALT.get(gid, {})
        primary = primary_by_country.get(cc, {})
        folds, seen = [], set()
        # Rus adı da qatlanır: sorğu «Москва» → "moskva", saxlanan da "moskva" → uyğun gəlir.
        # Ayrıca kiril sütunu saxlamağa ehtiyac qalmır.
        # ⚠️ «Neçənci namizəddir» ilə alias təyin etmək SINANDI və ATILDI: adı ilə asciiname-i
        # eyni olan şəhərlərdə (Yevlax/Yevlakh) sayğac sürüşür və qoruyucu işə düşmür.
        candidates = [(display, False), (name, False), (ascii_name, False)]
        candidates += [(localized.get(lang, ""), True) for lang in ("az", "tr", "ru")]

        for cand, is_alias in candidates:
            if not cand:
                continue
            if not (is_latin(cand) or is_pure_russian(cand)):
                continue                        # qeyri-latın/qeyri-rus mənbə qatlananda zibil verir
            f = fold(cand)
            if not f or len(f) < 2 or f in seen:
                continue
            # Alias BAŞQA şəhərin əsas adıdırsa atılır (GeoNames `az` sahəsindəki səhvlər:
            # Yevlax üçün «Gəncə» yazılıb və «gence» axtarışı yanlış şəhər qaytarırdı).
            if is_alias:
                owners = primary.get(f, set())
                if owners and gid not in owners:
                    continue
            seen.add(f); folds.append(f)

        if not folds:
            continue

        # GeoNames-in `alternatenames` sütunu sıralanmayıb və yoxlanılmayıb: Guangzhou üçün
        # ərəbcə «شینیانگ» (əslində Shenyang), Chongqing üçün rusca «Чунгкинг» (əslində Чунцин)
        # yazılıb. Ona görə:
        #   • ərəb sütunu TAMAMİLƏ atıldı — yoxlanmaz və yanlış uyğunluq yaradırdı;
        #   • kiril yalnız RUSDİLLİ məkanda saxlanılır, orada ekzonim yaxşı təsbit olunub.
        # Buraxılmış ad «tapılmadı» deməkdir (zərərsiz); yanlış ad isə səhv şəhər deməkdir.
        rows.append((pop, display, "|".join(folds), cc,
                     f"{float(lat):.3f}", f"{float(lng):.3f}", str(elev)))

    return rows


def validate(rows, min_rows):
    """Yazmazdan əvvəl yoxlama: pozuq fayl tətbiqdə səssizcə az şəhər deməkdir."""
    for pop, name, folds, cc, lat, lng, elev in rows:
        assert name and "\t" not in name, f"ad pozuq: {name!r}"
        assert folds and all(f for f in folds.split("|")), f"qatlama boşdur: {name!r}"
        assert "  " not in folds, f"qoşa boşluq — Kotlin tərəflə sürüşmə: {name!r} → {folds!r}"
        assert folds == folds.strip(), f"kənar boşluq: {name!r}"
        assert len(cc) == 2, f"ölkə kodu pozuq: {cc!r}"
        assert -90.0 <= float(lat) <= 90.0 and -180.0 <= float(lng) <= 180.0, f"koordinat: {name!r}"
        assert 0 <= int(elev) <= 9000, f"hündürlük: {name!r} → {elev}"
    assert len(rows) > min_rows, f"gözlənilməz az şəhər: {len(rows)}"


HEADER = (
    "# Mənbə: GeoNames (https://www.geonames.org) {dump} — CC BY 4.0.\n"
    "# Sütunlar: ad<TAB>qatlanmış adlar (|)<TAB>ölkə<TAB>enlik<TAB>uzunluq<TAB>hündürlük(m)\n"
    "# Sıra ƏHALİYƏ görə azalandır — axtarış böyük şəhərləri əvvəl qaytarsın deyə;\n"
    "# bu, ayrıca əhali sütunu saxlamağı lazımsız edir.\n"
)


def render(rows, dump):
    rows = sorted(rows, key=lambda r: -r[0])
    body = "".join(f"{n}\t{f}\t{cc}\t{lat}\t{lng}\t{e}\n" for _, n, f, cc, lat, lng, e in rows)
    return HEADER.format(dump=dump) + body


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--full", action="store_true",
                    help="endirilən genişləndirilmiş kataloqu da qur (cities5000.txt lazımdır)")
    ap.add_argument("--version", type=int, default=1,
                    help="genişləndirilmiş kataloqun manifest versiyası (hər yayımda artır)")
    args = ap.parse_args()

    # --- paketdəki siyahı ---
    bundled = build_rows(
        "cities15000.txt",
        lambda cc, fcode, pop: (cc == "AZ") or (fcode == "PPLC")
                               or (pop >= WORLD_MIN) or (cc in REGION and pop >= REGION_MIN),
    )
    validate(bundled, 3000)
    text = render(bundled, "cities15000")
    open("cities.tsv", "w", encoding="utf-8").write(text)
    print(f"cities.tsv: {len(bundled)} şəhər, {len(text.encode()) // 1024} KB")

    if not args.full:
        return

    # --- endirilən genişləndirilmiş kataloq ---
    # Süzgəc yoxdur: `cities5000` onsuz da 5 000 nəfər həddidir, yəni siyahının özü seçimdir.
    # Gədəbəy (≈14 500) məhz burada peyda olur.
    full = build_rows("cities5000.txt", lambda cc, fcode, pop: True)
    validate(full, len(bundled))
    text = render(full, "cities5000")

    name = f"cities-v{args.version}.tsv.gz"
    # mtime=0 → eyni giriş eyni baytları versin, yəni təkrar generasiya boş yükləmə yaratmasın.
    with gzip.GzipFile(name, "wb", compresslevel=9, mtime=0) as gz:
        gz.write(text.encode("utf-8"))

    # Tam `ghraw://` URL yazılır (WBW manifesti ilə eyni forma): fayl yeri dəyişsə tətbiq
    # yeniləməsi lazım gəlmir, mirror seçimi isə `resolveInventoryUrl`-da tətbiq olunur.
    manifest = {
        "version": args.version,
        "file": f"ghraw://cafarovceyxun/AnaMuslim/main/inventory/prayer/{name}",
        "rows": len(full),
    }
    open("cities.json", "w", encoding="utf-8").write(json.dumps(manifest, ensure_ascii=False) + "\n")

    raw_kb, gz_kb = len(text.encode()) // 1024, os.path.getsize(name) // 1024
    print(f"{name}: {len(full)} şəhər, {raw_kb} KB xam → {gz_kb} KB gzip")
    print(f"cities.json: {manifest}")


if __name__ == "__main__":
    main()
