# Namaz bələdçisi, Mərhələ 3 — maket generatoru

Maket: https://claude.ai/artifact/NDHyUdsDLzBQZJjPnnCvaP (2026-10-04)

```
python3 build.py <db.json> <çıxış.html>
```

`db.json` iCloud-dakı `anamuslim-mezmun-*.json` yedəyinin `tables` hissəsidir.

- `data.py` — çıxarışlar (`EX`, Muheymin 1-ci cild № 371–510) və zikrlər (`ZK`). `user: True` olan zikrin oxunuşu
  kitabda yoxdur, təklifdir və maketdə təsdiq qutusu ilə görünür.
- `build.py` — hər çıxarışı hədisin `text_az`-ında **hərfi** axtarır; zikrlərin ərəbcəsini xam `text_ar`-da (hərəkə
  qatlanaraq), oxunuşunu `text_az`-da, tərcüməsini kitabda yoxlayır. Biri tapılmasa dayanır.
- `template.html` + `../phase2/base.css`. Yeni poza şəkli yoxdur: səf sxemi SVG, qorxu namazı dəstə cədvəlidir.

## Seed (2026-10-04, tətbiq olunub)

```
python3 phase3_seed.py <db.json> <çıxış qovluğu>
```

- `phase3_ar.py` — hər çıxarışın ərəbcə lövbərləri (hərəkəsiz), `cut` Mərhələ 2-nin `phase2_ar.py`-sindən gəlir.
- `phase3_seed.py` — mövzu siyahısı (tək mənbə, 97 mövzu): `salah3_topics.kt.txt`, `salah3_seed.sql`, `salah3_verify.sql`.
  Dilim çox uzundursa (lövbər başqa rəvayətə sürüşüb) generator dayanır.
- Miqrasiya adı `salah_evidence_seed_phase3`, 153 sətir, md5 ilə 153/153 yoxlandı.
- «Amin» bazada deyil (kitabda tərcüməsi yoxdur) — `SalahGroupContent.amin`.
