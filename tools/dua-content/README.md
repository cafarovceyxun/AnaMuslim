# Dua bölməsi — Nəbinin (s.a.s.) dua və zikrləri

Mənbə: Muheymin 1–7-ci cildlər; əsas hissə 7-ci cildin «Zikr və Dua kitabı»dır (№ 2807–2876, `hadith.id` 3200–3269),
qalanı Namaz, Həcc, Tibb, Cihad, Məğazi və s. kitablardakı hədislərdəndir. 2026-10-06-da `dua_nebi_dualari_seed`
miqrasiyası ilə yazılıb: 15 başlıq, 4 alt başlıq (Namaz), 91 sətir = 89 dua.

- `data_zikr.py`, `data_rest.py` — hər dua üçün hədis id-si və lövbərlər: ərəbcə (hərəkəsiz, durğu boşluq sayılır),
  oxunuş (`text_az`-dakı `{{…}}`), tərcümə (`note` və ya `text_az`), qeyd (hədisin öz sözü — fəzilət, nə vaxt deyilir).
- `data_extra.py` — ikinci dalğa (`dua_nebi_dualari_seed_2`, 21 dua): geri qaytarılan bəndlər və ikinci rəvayətlər;
  `gen.py … --extra` ilə qurulur.
- `data_c7.py` — üçüncü dalğa (2026-10-09, 6 dua, Oracle-da birbaşa psql ilə yazılıb, 6/6 md5): 7-ci cildin sonradan
  yüklənən kitabları — Cənnət (sonu), Cəhənnəm, Tövbə, Cənazə (`hadith.id` 3602–4056). Qurulma:
  `C7_OPT=istigfar_70,istigfar_100,zikr_qudsi,telqin python3 gen.py <yedək.json> --c7` (`C7_OPT` — istifadəçinin
  seçdiyi, dua forması olmayan bəndlər). Yedək kimi serverdən təzə `hadith` dump-ı işlədilib. ⚠️ Cənazə kitabında
  cənazə namazının (təkbirlər arası) duaları **yoxdur**.
- Yatarkən zikri (2026-10-09, əl ilə SQL): dua 90 (№ 2835) altında İxlas/Fələq/Nas, dua 91 (№ 2828) altında Bəqərə
  285-286 tam — `source_type = 'quran'` hissələri (part 2–4). Mətn `quran/gen.py`-nin `build()`-i ilə qurulub, base64.
- `gen.py <yedək.json> [--extra | --c7] [--print]` — mövqeləri hesablayır, `dua_seed.sql` (server `substr` ilə kəsir) və
  `dua_verify.sql` (md5 yoxlaması) yazır. `--print` hər cütü çap edir — ərəbcə ilə tərcümə eyni rəvayətdən olmalıdır.
- `cats.py` + `build_html.py` + `tpl.html` — yazmazdan əvvəl istifadəçiyə göstərilən baxış səhifəsi (`review.json`-dan).

Qaydalar (Həcc/Namaz bələdçisi ilə eyni):
- Seçim bab adına görə yox, **hədisin öz mətninə** görədir. Nəbinin sözü olmayan dualar (səhabə, mələk, əvvəlki nəbilər,
  imam Əbu Ubeydənin № 208-dəki həmdi) daxil edilmir.
- Kitabda tərcüməsi olmayan dua üçün tərcümə yazılmır — `text_az` boş qalır.
- Yazmazdan əvvəl mənbə hədislərin md5-i yedəklə tutuşdurulmalıdır (yedək köhnə ola bilər, mövqelər sürüşər).
- `dua.source` ≤ 300 simvol: uzun mənbədə yalnız istifadə olunan rəvayətin zəncir sətri yazılır.

⚠️ Seed artıq tətbiq olunub — təkrar işlətmə: `dua` üzərindəki unikal indeks eyni çıxarışı bloklayar, başlıq insert-i isə
PK səhvi verər.

## Qurandan dualar (`quran/`, 2026-10-07)

`dua_qurandan_dualar_seed_1..4`: başlıq `qurandan-dualar` (sort 0), 17 alt başlıq, 105 sətir = 104 dua.
- `verses.py` — alt başlıqlar və ayə siyahısı; `spec.py` — hər dua üçün söz indeksləri (ərəbcə, `quranapp.db` script 1)
  və tərcümə lövbərləri (`az`), qeyd = duanı kimin etdiyi (ayənin öz tərcüməsindən).
- `gen.py [--print]` → `review.json`, `seed_N.sql` (~22 KB-lıq hissələr), `verify.sql`, `source_check.sql` (serverdəki
  tərcümə yedəklə eynidirmi); `agg.py` — alt başlıq üzrə gözlənilən hash (yazandan sonra serverlə tutuşdur);
  `build_html.py` → baxış səhifəsi.
- Ərəbcə base64 literaldır: serverdə Quran ərəbcəsi yoxdur (`substr` mümkün deyil), Osmani mətni isə NFC-də dəyişir.
⚠️ Seed tətbiq olunub — təkrar işlətmə (başlıq PK səhvi verər).

Sonra (eyni gün) `reorg_2026_10_07.sql` (`dua_qurandan_movzu_basliqlari`): 12 vəziyyət/mövzu alt başlığı. Allahın istədiyi /
salehlər qrupundan olanlar köçdü, peyğəmbər duaları nüsxələndi (qeyd: «X əleyhissəlamın duası»). ⚠️ `spec.py`/`gen.py`
köçürmədən **əvvəlki** yerləşməni təsvir edir — bazanın cari halı üçün bu SQL-ə də bax.
