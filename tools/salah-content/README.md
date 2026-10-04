# Namaz bələdçisi — məzmun generatorları

Mərhələ 1 (Təharət, 2026-10-04) üçün işlədilib. Mənbə: iCloud-dakı `anamuslim-mezmun-*.json` yedəyi.

1. `phase1_data.py` — Azərbaycanca çıxarışlar; hər biri hədisin `text_az`-ının hərfi parçası olmalıdır (yoxlanır).
2. `phase1_ar.py` — eyni çıxarışın ərəbcəsini **xam** `text_ar`-dan kəsir (hərəkə/hamzə qatlanaraq axtarılır).
3. `phase1_seed.py` — mövzu siyahısı (tək mənbə): `SalahTopic` enum-u və `salah_evidence` seed SQL-i.

⚠️ Skriptlərdə `S = ...scratchpad` yolu var; işlətməzdən əvvəl öz qovluğuna dəyiş və `db.json`-u
(yedəyin `tables` hissəsi) yanına qoy. Seed artıq tətbiq olunub — təkrar işlətmə (`on conflict do nothing`).

## Dəstəmaz kadrları (`wudu/`, 2026-10-04)

`wudu/split_panels.py` istifadəçinin 15 kadrlıq ağ-qara vərəqindən (`destemaz-wudu-ag-qara.svg`, repoda
saxlanmır) yalnız addımlara uyğun 10 kadrı `composeResources/drawable/dr_salah_wudu_*.xml` kimi kəsir —
nömrələr və çərçivələr götürülmür. Qolu ovuşdurmaq (6, 8), boyun (11) və qulaqlar (12–13) addım olmadığı
üçün qəsdən çıxarılıb. Kadr→addım bağlantısı `SalahGuideContent`-dəki `WuduDrawing`-dədir.

## Təyəmmüm və qüsl kadrları (2026-10-04)

`tools/salah-art/vector/sheets.py` istifadəçinin boyama vərəqlərindən kəsilmiş kadrları
(`tools/salah-art/vector/src/{tayammum,ghusl}_*.png`) `dr_salah_{tayammum,ghusl}_*.xml`-ə çevirir.
Kitaba uyğunlaşdırma düzəlişləri (oxların, üz işarələrinin silinməsi, üfürmə xətləri) skriptin başında yazılıb.
Qüslün «Sağ, sol, orta» addımı üçün vərəqdə kadr yox idi: `make_sides()` onu «üç ovuc» kadrından qurur
(yuxarı əl güzgülənib başın sağına, aşağı əl soluna köçür, axın xətləri və 1–2–3 nömrələri çəkilir).
