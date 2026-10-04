# Namaz bələdçisi — məzmun generatorları

Mərhələ 1 (Təharət, 2026-10-04) üçün işlədilib. Mənbə: iCloud-dakı `anamuslim-mezmun-*.json` yedəyi.

1. `phase1_data.py` — Azərbaycanca çıxarışlar; hər biri hədisin `text_az`-ının hərfi parçası olmalıdır (yoxlanır).
2. `phase1_ar.py` — eyni çıxarışın ərəbcəsini **xam** `text_ar`-dan kəsir (hərəkə/hamzə qatlanaraq axtarılır).
3. `phase1_seed.py` — mövzu siyahısı (tək mənbə): `SalahTopic` enum-u və `salah_evidence` seed SQL-i.

⚠️ Skriptlərdə `S = ...scratchpad` yolu var; işlətməzdən əvvəl öz qovluğuna dəyiş və `db.json`-u
(yedəyin `tables` hissəsi) yanına qoy. Seed artıq tətbiq olunub — təkrar işlətmə (`on conflict do nothing`).
