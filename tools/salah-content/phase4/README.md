# Namaz bələdçisi, Mərhələ 4 — maket generatoru

Maket: https://claude.ai/artifact/T6cQTfAQdh4CaW74D6sKBN (2026-10-04)

```
python3 build.py <db.json> <çıxış.html>
```

`db.json` iCloud-dakı `anamuslim-mezmun-*.json` yedəyinin `tables` hissəsidir.

- `data.py` — çıxarışlar (`EX`, Muheymin 1-ci cild № 511–595; bazada № 559 yoxdur) və üç gecə duası (`ZK`).
- `build.py` — hər çıxarışı hədisin `text_az`-ında və ya qeydində **hərfi** axtarır. Dualar əl ilə yazılmır: ərəbcə xam
  `text_ar`-dakı `{…}` blokundan, oxunuş `text_az`-dakı `{…}` blokundan lövbərlə tapılır, tərcümə hədisin qeydindən
  `meanFrom … meanTo` arası kəsilir. Biri tapılmasa build dayanır.
- `template.html` + `../phase2/base.css`. Yeni şəkil yoxdur: sünnət cədvəli, gecə namazı formaları, səhv səcdəsi ardıcıllığı.
- Cənazə namazında təkbirlərdən sonra deyilənlər və salam bu bablarda yoxdur — 7-ci cildin hələ yüklənməmiş «Cənazələr»
  kitabını gözləyir.

## Seed (2026-10-04, tətbiq olunub)

```
python3 phase4_seed.py <db.json> <çıxış qovluğu>
```

- `phase4_ar.py` — hər çıxarışın və duanın ərəbcə lövbərləri (hərəkəsiz), `cut` Mərhələ 2-nin `phase2_ar.py`-sindən gəlir.
  `cut` start-ın **ilk** rastına baxır — çıxarış sonrakı rəvayətdəndirsə lövbər həmin rəvayətə xas sözlə seçilib.
- `phase4_seed.py` — mövzu siyahısı (tək mənbə, 78 mövzu): `salah4_topics.kt.txt`, `salah4_seed.sql`, `salah4_verify.sql`.
- Miqrasiya adı `salah_evidence_seed_phase4`, 114 sətir, md5 ilə 114/114 yoxlandı.
- № 532-nin müəllif qeydi (Duha) və № 551-in iki izahı ərəbcəsiz olduğu üçün bazada deyil, kodda
  (`SalahNaflContent.DUHA_NOTE`, `TAHAJJUD_NOTES`).
