# Şəhər kataloqu generatoru

Bir skript, **iki** çıxış — ikisi də eyni seçim/qatlama məntiqindən keçir:

| Fayl | Hara gedir | Mənbə | Nə üçün |
|---|---|---|---|
| `cities.tsv` | `shared/src/commonMain/composeResources/files/prayer/` (APK/IPA-nın içi) | `cities15000` | GPS və internet olmayanda son dayaq |
| `cities-v<N>.tsv.gz` + `cities.json` | `inventory/prayer/` (repoda, `ghraw://` ilə verilir) | `cities5000` | Kiçik məntəqələr; tətbiq ilk açılışda çəkir |

```bash
curl -O https://download.geonames.org/export/dump/cities15000.zip
curl -O https://download.geonames.org/export/dump/cities5000.zip
unzip -o cities15000.zip && unzip -o cities5000.zip

# aliaslar yalnız mənbə dəyişəndə lazımdır (194 MB endirmə):
# curl -O https://download.geonames.org/export/dump/alternateNamesV2.zip
# unzip -o alternateNamesV2.zip && python3 gen_alt_names.py cities5000.txt

python3 gen_cities.py                       # yalnız paketdəki siyahı
python3 gen_cities.py --full --version 1    # hər ikisi

mv cities.tsv ../../shared/src/commonMain/composeResources/files/prayer/
mv cities-v1.tsv.gz cities.json ../../inventory/prayer/
```

**Mənbə:** GeoNames — CC BY 4.0, atribusiya `CREDITS.md`-dədir.

## Paketdəki siyahı

**Seçim:** bütün Azərbaycan (65) + bütün paytaxtlar + region (TR/RU/GE/IR/KZ/UZ/TM/TJ/KG/AM ≥ 100k)
+ dünya ≥ 200k = **3500 şəhər**, 157 KB (gzip ~78 KB).

Bu siyahı **silinmir və kiçildilmir**. Oflaynlıq `CityCatalog` sənədində dizayn qərarı kimi yazılıb:
şəhər seçimi məhz internetin olmadığı anlarda lazım olur (təyyarə rejimi, SIM-siz telefon, rədd
edilmiş icazə). Genişləndirilmiş kataloq onun **üstünə** gəlir, yerinə yox.

## Endirilən kataloq

`cities5000` süzgəcsiz götürülür — siyahının özü onsuz da 5 000 nəfər həddidir: **69 691 şəhər**,
3 073 KB xam → **1 533 KB gzip**. Gədəbəy (≈14 500) `cities15000`-də **yoxdur**, `cities5000`-dədir;
bu funksiyanın bütün səbəbi budur. Azərbaycan məntəqələri 65-dən **134**-ə çıxır.

Yayım addımları:

1. `python3 gen_cities.py --full --version <N>` — `<N>` hər dəfə **artır**, əks halda telefonlardakı
   nüsxə yenilənmir (`CityCatalogStore` versiyanı müqayisə edir).
2. Hər iki faylı `inventory/prayer/`-ə qoy və **bir commit-də** göndər — manifest və data eyni anda
   canlı olsun.
3. Köhnə `cities-v<N-1>.tsv.gz` bir müddət qalsın: jsDelivr branch ref-lərini bir həftəyə qədər
   keşləyir, yəni bəzi telefonlar hələ köhnə manifesti görür. Fayl adının versiya damğalı olmasının
   səbəbi budur — köhnə manifest gecikmə yaradır, qırıq sorğu yox.
4. Repo hər versiyada ~1,5 MB böyüyür və git onu həmişəlik saxlayır. Kataloq ildə bir-iki dəfə
   yenilənməlidir; aylıq yeniləmə üçün bu yol uyğun deyil.

Tətbiq tərəfi: `CityCatalogStore` (endirmə, gzip açma, yoxlama) və `PrayerLocationViewModel`
(endirilmiş → paketdəki sırası).

## Tələlər

⚠️ `fold()` funksiyası `CityCatalog.fold()` ilə **eyni olmalıdır**: fayldakı qatlanmış adlar burada,
istifadəçinin sorğusu isə Kotlin tərəfdə qatlanır. İki tərəf sürüşsə axtarış **səssizcə boş
qayıdır** — nə kompilyator, nə də ekran xəbər verir. (`CityCatalogTest` sürüşməni tutur.)

⚠️ `az_names.py` — Azərbaycan şəhərlərinin adları **əl ilə** yoxlanılıb. GeoNames-in `name` sahəsi
ingiliscədir («Baku», «Ganja»), `alternatenames`-dən avtomatik seçim isə türkcə variantı gətirirdi
(«Bakü»). Cədvəldə olmayan AZ məntəqəsi GeoNames-in beynəlxalq adı ilə görünür («Gadabay»);
`cities5000`-ə keçəndə siyahıya düşən yeni AZ məntəqələri həmin cədvələ yazılmalıdır.

⚠️ `alt_names.tsv` **generasiya olunur**, əl ilə yazılmır — `gen_alt_names.py`, mənbə
`alternateNamesV2.zip` (194 MB). Nəticə repoda saxlanılır ki, adi generasiya həmin dump-ı tələb
etməsin. Bütün `cities5000` gid-lərini əhatə edir (37 447 sətir), yəni hər iki kataloqa yetər.

⚠️ **Axtarış böyük siyahıda ucuz deyil.** `CityCatalog.search` hər pillədə bütün siyahını gəzir:
JVM-də 3 500 şəhərdə 0,2–1,2 ms, 69 691 şəhərdə **3,3–8,2 ms**. Kotlin/Native daha yavaşdır, ona görə
`PrayerLocationViewModel.search` fon telində işləyir və hər hərfdə əvvəlkini ləğv edir. Sinxron
qaytarsan iOS-da klaviatura ilişir — nə kompilyator, nə test bunu tutur.
