# `cities.tsv` generatoru

`shared/src/commonMain/composeResources/files/prayer/cities.tsv` faylını yenidən qurur.

```bash
curl -O https://download.geonames.org/export/dump/cities15000.zip
unzip cities15000.zip
python3 gen_cities.py
mv cities.tsv ../../shared/src/commonMain/composeResources/files/prayer/
```

**Mənbə:** GeoNames `cities15000` — CC BY 4.0, atribusiya `CREDITS.md`-dədir.

**Seçim:** bütün Azərbaycan (65) + bütün paytaxtlar + region (TR/RU/GE/IR/KZ/UZ/TM/TJ/KG/AM ≥ 100k)
+ dünya ≥ 200k = **3521 şəhər**, 126 KB (sıxılmış ~63 KB).

⚠️ `fold()` funksiyası `CityCatalog.fold()` ilə **eyni olmalıdır**: fayldakı qatlanmış adlar burada,
istifadəçinin sorğusu isə Kotlin tərəfdə qatlanır. Cədvəl dəyişdirilirsə Kotlin tərəf də yenidən
generasiya olunmalıdır (`CityCatalogTest` sürüşməni tutur).

⚠️ `az_names.py` — Azərbaycan şəhərlərinin adları **əl ilə** yoxlanılıb. GeoNames-in `name` sahəsi
ingiliscədir («Baku», «Ganja»), `alternatenames`-dən avtomatik seçim isə türkcə variantı gətirirdi
(«Bakü»). Yeni AZ şəhəri əlavə olunsa həmin cədvələ də yazılmalıdır.
