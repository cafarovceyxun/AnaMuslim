# Namaz bələdçisi, Mərhələ 2 — maket generatoru

Maket: https://claude.ai/artifact/T3hNn6zxUvwcG8y4pKnR31 (2026-10-04)

```
python3 build.py <db.json> <çıxış.html>
```

`db.json` iCloud-dakı `anamuslim-mezmun-*.json` yedəyinin `tables` hissəsidir.

- `data.py` — çıxarışlar (`EX`), zikrlər (`ZK`), təsbih sayğacı, əzan cədvəli (istifadəçinin verdiyi, hərfi).
- `build.py` — hər çıxarışı hədisin `text_az`-ında **hərfi** axtarır; zikrlərin ərəbcəsini xam `text_ar`-da
  (hərəkə qatlanaraq), oxunuşunu `text_az`-da, tərcüməsini kitabın `note`-unda yoxlayır. Biri tapılmasa dayanır.
- `template.html` + `base.css` (Mərhələ 1 maketinin CSS-i) + `ill-k.css`.
- PNG oxu/yaz köməkçisi (`png.py`) vektor izləyicisi ilə birlikdə `../../salah-art/vector/`-dədir.

Şəkillər `../../salah-art/vector/svg/`-dən inline SVG kimi gəlir (tətbiqdəki `dr_salah_pose_*.xml` ilə eyni yollar).
Vektorları yenidən qurmaq: `python3 ../../salah-art/vector/vec.py` → `out/*.xml` drawable-a, `out/*.svg` `svg/`-yə köçür.

## Seed (2026-10-04, tətbiq olunub)

```
python3 phase2_seed.py <db.json> <çıxış qovluğu>
```

- `phase2_ar.py` — hər çıxarışın ərəbcə lövbərləri (hərəkəsiz), `cut` xam `text_ar`-dan dilim qaytarır.
- `phase2_seed.py` — mövzu siyahısı (tək mənbə): `salah2_topics.kt.txt` (`SalahTopic` sətirləri), `salah2_seed.sql`
  və `salah2_verify.sql`. SQL ərəbcəni **ötürmür**: `substr(h.text_ar, başlanğıc, uzunluq)` ilə server kəsir;
  azərbaycanca da mümkün olduqda `substr(h.text_az / h.note, …)`. Yoxlama sorğusu hər sətrin md5-ini tutuşdurur.
- Miqrasiya adı `salah_evidence_seed_phase2`; `on conflict do nothing` — təkrar işlətmək zərərsizdir.
