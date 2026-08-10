# Ənə Muslim — Osmanlı Skriptinə Təcvid Rəngləri Planı

> **Bu fayl nə üçündür:** Osmanlı (Uthmani) atlas skriptinə hərf-səviyyəli təcvid
> rənglərinin əlavə edilməsinin yol xəritəsi və gedişat izləyicisidir. Yeni prompt
> açanda **bu faylı oxut**, `## 🔖 HAZIRDA HARDAYIQ` bölməsinə bax. Hər iş bitəndə
> müvafiq `- [ ]` qutusunu `- [x]` et və status bölməsini yenilə.
>
> ⚠️ **COMMIT QAYDASI:** `git commit`-i **HƏMİŞƏ istifadəçi özü edir.** Assistant heç vaxt
> commit etməsin.
>
> 👥 **İŞ BÖLGÜSÜ:** Asan/mexaniki işlər → paralel Sonnet; qarışıq/dizayn-həssas işlər →
> Opus / əsas model.

---

## 🔖 HAZIRDA HARDAYIQ

- **Faza 1-5 əsasən bitib. Data `tajweed.bin` v8-dədir** (2026-08-10). İstifadəçi cihazda (iOS)
  təsdiqlədi ("belə yaxşıdı", 2026-07-25). Qalan iş Faza 5 QA-sındadır (aşağıda).
- ⚠️ **v7 qərarı (2026-07-25) + v8 (2026-08-10):** Osmanlı rejimində YALNIZ samit qaydaları
  rənglənir — ghunnah, qalqalah, ikhfa, idghaam (ğunnə ilə), iqlab **və v8-dən etibarən
  `idghaam_no_ghunnah`** (kəhrəba/sarı, sinif 2 — v7-də normal məddin boşaltdığı slot).
  Bütün məddlər + silent letter RƏNGSİZDİR (istifadəçi istəyi). Bu, `الرَّحۡمَٰن` liqatura
  daşqını problemini də təbii həll etdi (mədd ümumiyyətlə rənglənmir).
  Əhatə: 618 277 qlifin **10.25%-i** rənglidir (v7-də 9.54%).
- ℹ️ **Fatihə rəngsizdir və bu DÜZGÜNDÜR** (2026-08-10-da təkzib olunan «hizalama sürüşməsi»
  hipotezi — Faza 5 blokuna bax). Mətndə rənglənən qaydalardan heç biri yoxdur. Təcvidi
  yoxlayanda **1-ci səhifəyə baxıb qərar vermə** — Bəqərəyə keç.
- 🐛 **AÇIQ QÜSUR (2026-08-10): ilk açılışda rəng gəlmir** — təmiz quraşdırmadan sonra reader
  atlas hazır olmadan qurulur, font fallback-ə düşür, import bitəndə isə heç nə siyahını yenidən
  qurmur. Rejim dəyişikliyi/restart düzəldir. **Növbəti iş budur** — detallar Faza 5 blokunda.
- Faza 2 (data import + DB) və Faza 3 (render) tamamlandı: `tajweed.bin` dekoderi,
  `ExternalQuranDatabase` v4→v5 migration + `TajweedDao`/entity-lər, `TajweedImporter`
  (layout sənəd sırası ilə baza cədvəlini qurur), `TajweedColorSource` (birləşmiş
  qlif-sinifləri keşi), və `QuranAtlasText` per-qlif rənglənməsi.
- **Cihaz iterasiyaları (2026-07-25):** (a) offset sürüşməsi düzəldildi (cpfair-in
  pinned mətninə keçid), (b) samitə artıq rəng getdi (dəqiq-simvol proyeksiya),
  (c) ğunnə ailəsi qranulyar siniflərə bölündü, (d) AMOLED palitra + rəng mənbəyi
  `TajweedPalette.kt`-yə köçürüldü, (e) `الرَّحۡمَٰن` liqatura suppression (v5),
  (f) dekoder versiya-tolerant edildi. Detallar aşağıda "Rəng palitrası" bölməsində.
- Bütün riskli fərziyyələr praktikada yoxlanılıb (aşağıda "Təsdiqlənmiş faktlar").
  Atlas yenidən generasiya **LAZIM DEYİL**.

---

## Təsdiqlənmiş faktlar (2026-07-24 validasiyası)

Bunlar fərziyyə deyil — skriptlə tam korpus üzərində yoxlanılıb:

1. **Shaping 100% üst-üstə düşür.** Repo-dakı `shared/src/commonMain/composeResources/font/uthmanic_hafs.ttf`
   (KFGQPC HAFS Uthmanic Script v2.2, upem 2048) atlası yaradan fontun eynisidir
   (atlas.json advance-ləri hmtx ilə tutuşur). uharfbuzz ilə
   **`features=None` (default ərəb shaping), direction=RTL, script=arab, language=ar**
   konfiqurasiyasında `layout.json`-dakı **21,497/21,497 sənədin gid ardıcıllığı
   100.000% eynidir** (məntiqi/forward sırada). ⚠️ Feature-ları əl ilə ON etmək
   (`init,medi,fina,...=True`) nəticəni sındırır (1.2%) — mütləq `None` ötürülməlidir.
2. **Cluster xəritəsi işləyir:** `buf.cluster_level = 1` (MONOTONE_CHARACTERS) ilə hər
   qlif ayrıca mənbə-simvol indeksi daşıyır — hərəkələr bazadan ayrıla bilir.
   (Default level 0-da hərəkələr bazanın cluster-inə qarışır — istifadə etmə.)
3. **Atlas↔DB bağlantısı:** `app/src/main/assets/db/quranapp.db` → `ayah_words`
   (script_id=1 = uthmani) cədvəlindəki **77,429 söz occurrence-inin 100%-i** atlas
   layout-unda dəqiq mətn uyğunluğu ilə tapılır. Hər ayənin sonunda ayə-nömrəsi
   tokeni (məs. `١`) var — pipeline-da atılmalıdır.
4. **cpfair offsetləri birbaşa işləmir:** cpfair datası Tanzil Uthmani mətninə
   indekslənir (surə-açan ayələrdə bəsmələ prefiksli). Bizim KFGQPC mətnlə xam
   müqayisədə ayələrin yalnız ~17%-i üst-üstə düşür — fərqlər: sukun kodlaması
   (U+06E1↔U+0652), U+06DF, əlif-məqsurə (U+0649↔U+064A), tənvin variantları,
   iqlab mimi (U+06E2↔U+0656), həmzə oturacaqları, söz seqmentasiyası, vəqf işarələri.
5. **Skelet-lövbərli alignment problemi həll edir:** normallaşdırma + samit skeleti
   üzərində `difflib.SequenceMatcher` ilə ayələrin **94.9%-i dəqiq, 100%-i
   edit-distance ≤2** daxilində uyğunlaşır. Yəni Tanzil offset → app offset xəritəsi
   etibarlı qurulur.
6. **Dedup məhdudiyyəti:** atlas layout-u söz mətninə görə deduplicated-dir
   (21,497 unikal ≠ 77,429 occurrence), təcvid isə söz sərhədində kontekstdən
   asılıdır (idğam/ixfa/iqlab növbəti sözün ilk hərfindən asılıdır) →
   rəng datası **per-occurrence** açarlanmalıdır (və ya unikal-söz bazası +
   sərhəd override-ları).
7. **Həcm:** 618,277 qlif instansı; tövsiyə olunan kodlaşdırma ilə (unikal sənəd
   başına baza massiv + occurrence başına sərhəd override) hədəf **gzip <100 KB**.

## Data mənbəyi və lisenziya

- **Mənbə: [cpfair/quran-tajweed](https://github.com/cpfair/quran-tajweed)** —
  `output/tajweed.hafs.uthmani-pause-sajdah.json` (6236 ayə, simvol-aralığı → qayda).
  **Lisenziya: CC BY 4.0** → GPLv3-ə bir-istiqamətli uyğundur; attribution
  `CREDITS.md`-yə əlavə olunmalıdır. Classifier kodu da açıqdır (lazım olsa öz
  mətnimizə qarşı yenidən generasiya mümkündür).
- **18 qayda:** ghunnah, idghaam_ghunnah, idghaam_no_ghunnah, idghaam_mutajaanisain,
  idghaam_mutaqaaribain, idghaam_shafawi, ikhfa, ikhfa_shafawi, iqlab, madd_2,
  madd_246, madd_muttasil, madd_munfasil, madd_6, qalqalah, hamzat_wasl,
  lam_shamsiyyah, silent.
- ⚠️ **Tafkhim cpfair-də YOXDUR** — mövcud legenddə isə var. Qərar: ya legenddən
  atlas rejimində gizlət, ya da öz deterministik ra/lam qaydamızı əlavə et
  (ayrıca iş, Faza 5-ə baxılır).
- **İstifadə OLUNMAYAN mənbələr** (qeyd üçün): alquran.cloud `quran-tajweed`
  nəşri və onun parser-i [vipafattal/TajweedParser](https://github.com/vipafattal/TajweedParser) —
  lisenziya qeyri-müəyyən/Dar al-Maarifah mənşəli; yalnız **offline cross-check**
  üçün istifadə oluna bilər (data ship olunmur). GreentechApps repoları yoxlanıldı —
  təcvid datası açıq deyil.

## Rəng palitrası (qranulyar Türk üslubu — AMOLED palitra)

İstifadəçi qərarı (2026-07-24): burunlu qaydaları tək yaşıla yığmaq Al Quran/GreentechApps
tipli istifadəçilər üçün yanlış oxunur (orada yaşıl = qəlqələ). Ona görə hər burun/idğam
qaydası ayrıca sinif alır.

⚠️ **Rəngin qəti mənbəyi artıq `TajweedPalette.kt`-dir** (Kotlin), `tajweed.bin`-in içindəki
palitra DEYİL. Render və legend hər ikisi oradan oxuyur → rəng dəyişmək bir sətirlik iş,
data regen tələb etmir. `tajweed.bin` sinif *nömrələrini* verir, rəngi yox. Aşağıdakı hex-lər
AMOLED üçün seçilmiş cari dəyərlərdir (istifadəçi tələbi: tünd, bir-birinə yaxın olmayan).

**v7-də yalnız sinif 6-10 rənglənir** (aşağıda `[AKTİV]`). Sinif 1-5 (məddlər + silent)
cpfair-də qaydaları var, amma `UNCOLOURED_RULES`-a köçürülüb → data-da heç vaxt görünmür,
legenddə də yoxdur. Palitrada 1-5 rəngləri saxlanılır ki sinif nömrələri və nibble
paketləmə dəyişməsin.

| Sinif | Hex (`TajweedPalette.kt`) | Rəng | cpfair qaydaları |
|---|---|---|---|
| 0 | mətn rəngi | — | lam_shamsiyyah + (v7) bütün məddlər, silent, hamzat_wasl, idghaam_no_ghunnah |
| 1 | `0xFF90A4AE` | boz-mavi | *(v7: rəngsiz)* |
| 2 | `0xFFF9A825` | tünd sarı | *(v7: rəngsiz)* |
| 3 | `0xFFFB8C00` | narıncı | *(v7: rəngsiz)* |
| 4 | `0xFFEC407A` | çəhrayı | *(v7: rəngsiz)* |
| 5 | `0xFFD81B60` | tünd çəhrayı | *(v7: rəngsiz)* |
| 6 | `0xFFB71C1C` | tünd qırmızı | ghunnah `[AKTİV]` |
| 7 | `0xFF43A047` | yaşıl | qalqalah `[AKTİV]` |
| 8 | `0xFFEF5350` | açıq qırmızı | ikhfa, ikhfa_shafawi `[AKTİV]` |
| 9 | `0xFF8E24AA` | bənövşəyi | idghaam_ghunnah, idghaam_shafawi, idghaam_mutajanisayn, idghaam_mutaqaribayn `[AKTİV]` |
| 10 | `0xFF1E88E5` | mavi | iqlab `[AKTİV]` |

Üst-üstə düşmədə prioritet: `5 > 4 > 3 > 2 > 7 > 10 > 9 > 8 > 6 > 1`.
Tafkhim SKIP edilir (cpfair-də qayda yoxdur — atlas rejimində legenddən gizlədilir).

**Liqatura suppression (v5, 2026-07-25):** atlas çoxhərfli qrupu tək qlifə birləşdirir
(məs. `الرَّحۡمَٰن`-ın `حمٰن` quyruğu = gid610). Qayda yalnız liqaturanın daxili işarəsinə
düşəndə (məs. mədd superscript-alef-də) bütün qlif boyanmasın deyə: qlif ≥2 baza hərfi
əhatə edirsə, ancaq qayda baza hərfinin üstündə olanda rənglənir. Tək-hərfli və tək-işarəli
(standalone superscript-alef mədd) qliflər normal boyanır. Korpus təsiri: ~34 qlif.

**Versiya kövrəkliyi (2026-07-25):** `TajweedBinDecoder` artıq `version == SCHEMA_VERSION`
tələb ETMİR — `version >= MIN_ENCODING_VERSION (3)` qəbul edir (v3/v4/v5 kodlaşması eynidir).
`SCHEMA_VERSION` (cari **5**) yalnız importer-in re-import qərarı üçündür; data hər regen-də
bump olunur. Beləcə versiya sürüşməsi bir daha rəngləməni səssizcə öldürmür.

---

## Faza 1 — Offline data pipeline (Python, `tools/tajweed/`) — Opus

Validasiya skriptlərinin işlək prototipləri scratchpad-də mövcuddur
(`shape_validate.py`, `full_check.py`, `cluster_map.py`, `align.py`, `fullalign.py`,
`skeltest.py`, `sizeest.py`) — scratchpad müvəqqətidir, kod repo-ya köçürülməlidir.

- [x] `tools/tajweed/` qovluğu: `requirements.txt` (uharfbuzz, fonttools), README
      (necə işə salınır, mənbə datanın URL-i və lisenziyası).
- [x] Addım 1: cpfair JSON + Tanzil `quran-uthmani` mətnini endir/keşlə
      (bəsmələ-prefiks qaydası ilə), offset-lərin Tanzil mətninə oturduğunu assert et.
- [x] Addım 2: `quranapp.db` → `ayah_words(script_id=1)`-dən ayə mətnini yığ
      (word_index sırası, ayə-nömrə tokenini at), söz başına char-span saxla.
- [x] Addım 3: normallaşdırma cədvəli (06E1/06DF→0652, 0649→064A, həmzə-oturacaqları→ء,
      tənvin variantları, 06E2↔0656, əlif-vəslə→əlif, tatweel/vəqf işarələrinin
      atılması) + skelet-lövbərli `SequenceMatcher` alignment → Tanzil-offset →
      app-offset xəritəsi. Surə-açan ayələrdə bəsmələ prefiksini ayır.
- [x] Addım 4: cpfair aralıqlarını app offset-lərinə → (söz, söz-daxili-char)
      koordinatlarına proyeksiya et. Sərhəd-aşan qaydaları (mədd munfasil, sözlərarası
      idğam) iki sözün run-larına böl.
- [x] Addım 5: hər unikal sözü uharfbuzz ilə shape et (**features=None, RTL/arab/ar,
      cluster_level=1**), gid ardıcıllığının `layout.json` ilə eyniliyini **build-time
      guard** kimi assert et; qlif → cluster → qayda → rəng-sinfi.
- [x] Addım 6: kodlaşdırma və çıxış faylı: unikal-sənəd baza massivləri
      (qlif başına 1 bayt rəng-sinfi) + per-occurrence sərhəd override-ları
      (`ayah_id`,`word_index`,son-qlif sinifləri) + qayda→rəng cədvəli + schema
      versiyası. Hədəf: gzip <100 KB.
- [x] Addım 7 (QA): qızıl ayələr üzərində vizual yoxlama üçün HTML/PNG render
      (Fatihə, Bəqərə 255, müqəttəat, İxlas, səcdə ayəsi) + alquran.cloud markup ilə
      offline cross-check hesabatı (yalnız müqayisə, data ship olunmur).
- [x] Çıxışı `shared/src/commonMain/composeResources/files/atlas/uthmani/tajweed.bin`
      (gzip) kimi yerləşdir. (Gələcəkdə `AlfaazPlus/QuranAppInventory` yükləmə yolu ilə
      də paylana bilər — indi bundled kifayətdir, fayl kiçikdir.)

## Faza 2 — Data import + DB (commonMain) — Opus

- [x] Yeni entity/DAO: təcvid rəng datası. İki cədvəl + meta:
      `tajweed_word_colors(bundle_key, word, classes BLOB)` (unikal söz bazası,
      `atlas_word_shapes` şablonu) + `tajweed_overrides(ayah_id, word_index, diffs BLOB)`
      + `tajweed_meta(bundle_key, version, palette BLOB)`. `ExternalQuranDatabase` v4→v5
      migration (`ExternalQuranDatabaseMigrations.MIGRATION_4_5`) + `TajweedDao`.
- [x] Importer: `TajweedBinDecoder` (`FORMAT.md`-dən yazılıb, gzip inflate okio `GzipSource`
      ilə) + `TajweedImporter` — `tajweed.bin`-i oxu, `layout.json`-u sənəd sırası ilə
      stream edərək baza rekordlarını söz mətninə bağla, cədvəllərə yaz; versiya yoxlaması
      (`tajweed_meta.version` schema versiyası ilə tutuşmayanda yenidən import).
- [x] `TajweedColorSource` səviyyəsində lookup: `getForWords` `ayahId*4096+wordIndex`
      açarı ilə birləşmiş qlif-sinfi massivi qaytarır (baza massiv + override
      birləşdirilmiş halda, keşlənir), palitra `paletteState`-də.

## Faza 3 — Render (commonMain) — Opus

- [x] `QuranWordText` → `QuranAtlasText`-ə opsional `glyphClasses: ByteArray?` +
      `tajweedPalette: List<Color>?` parametrlərini keçir (`Mushaf.kt` + `QuranTextWbw.kt`
      çağırış yerlərindən; palitra `LocalTajweedPalette` compositionLocal ilə).
- [x] `QuranAtlasText` draw loop-unda tək `colorFilter` əvəzinə qlif başına seçim:
      sinif 0 → mövcud mətn rəngi, əks halda palitradan. `Map<Int, ColorFilter>`
      keşi (remember-lənib) — hər draw-da allokasiya olmasın. Qlif-sinifləri
      **orijinal placement indeksi** ilə oxunur (skip olunan gid-lər sırasını pozmasın).
- [x] Massiv uzunluğu placements ilə düz gəlməyəndə (defensive) tam rəngsiz fallback —
      heç vaxt səhv qlifə rəng düşməsin (`glyphClasses.size == placements.size` yoxlaması).
- [x] Dark mode qərarı: ilk versiyada legenddəki sabit hex-lər hər iki temada
      (mövcud legend davranışı ilə eyni); kontrast problemi çıxsa dark-variant palitra
      sonra əlavə olunur (`isDark` onsuz da `ReaderItemsBuilder`-dən axır).

## Faza 4 — Ayarlar + legend UI — Sonnet (asan, şablon var)

- [x] `ReaderPreferences`: `KEY_TAJWEED_COLORS_ENABLED = PrefKey(booleanPreferencesKey("reader.tajweed_colors_enabled"), false)`
      + get/set/observe trio (`KEY_ARABIC_TEXT_ENABLED` şablonu, ReaderPreferences.kt:33).
- [x] `SettingsMainScreen.kt` Quran qrupunda `SwitchItem` (mövcud `SwitchItem` şablonu,
      ~sətir 315) — yalnız `observeQuranScript() == SCRIPT_UTHMANI` olanda görünür.
- [x] String resursları 4 dildə (en/az/tr/ru): başlıq + alt yazı
      (values/, values-az/, values-tr/, values-ru/ strings.xml + compose resources).
- [x] `ReaderScreen.kt`-də `tajweedSupported` gating-ini genişləndir:
      `SCRIPT_KFQPC_V4 || (SCRIPT_UTHMANI && tajweedColorsEnabled)` → legend bar
      Osmanlı rejimində də çıxsın. Tafkhim sətri atlas rejimində gizlədilir
      (cpfair-də yoxdur — Faza 5-ə qədər).
- [ ] `ScriptsScreen.kt`: Osmanlı kartında kiçik "təcvid dəstəklənir" nişanı (opsional).

## Faza 5 — QA + son işlər

> 🚨 **2026-08-09 (Opus) — FAZA 5 QA-SI İLK DƏFƏ EDİLDİ: xüsusiyyət iOS-da İŞLƏYİR, amma
> **FATİHƏ TAMAMİLƏ RƏNGSİZDİR** — yəni tətbiqin ən çox baxılan səhifəsi.
>
> **Ölçmə (təxmin deyil, piksel analizi — `max(r,g,b)-min(r,g,b) > 30` sayğacı):**
> - Fatihə, 1-ci səhifə, mushaf rejimi → **18 354 mətn pikselindən 0-ı rəngli**
> - Bəqərə səhifəsi → **23 420 pikseldən 3 886-sı rəngli (16.6%)**, palitra düzgün (yaşıl/qırmızı/bənövşəyi)
>
> ⚠️ **Metodoloji xəbərdarlıq (mən buna düşdüm):** ilk ölçmə yalnız Fatihədə aparıldı və «iOS-da
> ümumiyyətlə render olunmur» nəticəsi çıxarıldı — **səhv idi**. Fatihə korpusun yeganə rəngsiz
> surəsidir, yəni ən pis mümkün nümunə. Təcvidi yoxlayanda **birinci səhifəyə baxıb qərar vermə**.
>
> **Kod zənciri tam sağlamdır** (hər iki ucda eyni açarla loglanaraq təsdiqləndi):
> - Builder qapısı: `atlas=true script=uthmani pref=true enabled=true prepared=true palette=11`
> - Builder → render sətir-sətir üst-üstə düşür: `TJB p=2 L=5 classes=4` → `TJR L=5 mapSize=4`
> - Palitra render səthinə çatır, `QuranAtlasText` rəngləri tətbiq edir
>
> **ƏSL QÜSUR — MƏLUMATDADIR, KODDA DEYİL.** `tajweed_word_colors`-da Fatihənin **36 sözünün
> hamısı var**, amma **hamısının sinif massivi tam sıfırdır** → `getForWords` boş xəritə qaytarır →
> rəng yoxdur. Korpus üzrə sayıldı: **114 surədən düz 1-i tamamilə rəngsizdir və o, Fatihədir.**
> Ümumi əhatə: 21 497 sözün 8 063-ü (37.5%) qeyri-sıfır sinif daşıyır.
>
> ❌ **HİPOTEZ TƏKZİB OLUNDU (2026-08-10, Opus) — bu QÜSUR DEYİL, düzəldiləcək bir şey yoxdur.**
> 08-09-da yazılan «bəsmələ ayə 1 sayılır → ofsetlər sürüşür → hizalama boşa çıxır» fərziyyəsi
> yanlışdır. Yoxlama **hizalamadan tamamilə asılı olmayan** yerdə aparıldı — mənbə annotasiyalarının
> özündə (`tools/tajweed/cache/tajweed.hafs.uthmani-pause-sajdah.json`, cpfair-in orijinal datası,
> generatordan keçmədən). Fatihənin **bütün** annotasiyaları:
>
> ```
> hamzat_wasl 11 · lam_shamsiyyah 7 · madd_2 6 · madd_246 7 · madd_6 1
> ```
>
> Beşi də v7 qərarı ilə **bilərəkdən rəngsizdir**. Fatihədə tək bir ğunnə/qalqalə/ixfa/idğam/iqlab
> **yoxdur** — mətnin özündə yoxdur (nun/mim şəddəli yoxdur; `نَعۡبُدُ`-də sakin olan ع-dir, ب yox;
> `أَنۡعَمۡتَ` izhardır; `ٱلۡمَغۡضُوبِ`-in ب-si yalnız vəqfdə qalqalə verir, cpfair onu işarələmir).
> v8-in əlavə etdiyi `idghaam_no_ghunnah` da sayıma daxil edildi — nəticə dəyişmir. Yəni
> «114 surədən 1-i rəngsizdir» **düzgün nəticədir**, sürüşmə əlaməti deyil.
>
> Qalan yeganə şey qərardır, kod işi yox: **Fatihə üçün istisna olaraq mədləri açaq?** (Əks halda
> tətbiqin ən çox baxılan səhifəsi həmişə rəngsiz qalacaq — bu, gözlənilən davranışdır, amma
> istifadəçi «xüsusiyyət işləmir» kimi oxuya bilər.)
>
> ✅ **Beləliklə iOS render yolu bu bənd üçün TƏSDİQLƏNDİ** — «iOS-da yoxlama» bəndi bağlanır.
>
> 🔬 **Metod (təkrarla):** `println` sayğaclarını `ReaderItemsBuilder`-in mushaf qapısına və
> `QuranAtlasText`-ə qoy, `xcrun simctl launch --console-pty` ilə tut — Kotlin/Native `println`
> **unified log-a düşmür**, `log stream` boş qayıdır.

> ✅ **2026-08-10 (Opus) — v8 SİMULYATORDA TƏSDİQLƏNDİ (iPhone 17 Pro, təmiz quraşdırma).**
> Bəqərə səh. 2, müshəf rejimi, Osmanlı xətti. Palitraya ən yaxın rəngə görə təsnif edilmiş
> piksel sayımı (mətn sahəsi, 78 626 mətn pikseli, **9.6%-i rəngli**):
>
> | sinif | piksel | səhifədə görünən yer |
> |---|---|---|
> | ixfa (qırmızı) | 2170 | `أُنزِلَ`, `مِن قَبْلِكَ` |
> | **idghaam bila ghunnah (kəhrəba) — v8** | **1979** | `هُدًى لِّلْمُتَّقِينَ`, `مِّن رَّبِّهِمْ` |
> | idghaam maal ghunnah (bənövşəyi) | 1311 | `هُدًى مِّن` |
> | qalqalah (yaşıl) | 1235 | `رَزَقْنَٰهُمْ` |
> | ghunnah (tünd qırmızı) | 830 | `مِمَّا` |
> | iqlab (mavi) | 0 | bu səhifədə iqlab yoxdur — gözlənilən |
>
> Legend altı sətri də göstərir, «İdğam (ğunnəsiz)» birinci sırada və kəhrəba rəngdədir.
> ⚠️ **Rənglər YALNIZ müshəf rejimindədir** — ayə-ayə rejimində mətn ağdır (atlas render yolu
> işə düşmür), legend çipi isə orada da görünür. Bu, gözlənilən davranışdırmı, qərar verilməyib.
>
> ℹ️ Onboardinq-də «Təcvid rəngləri» açarı **defolt AÇIQDIR** və Osmanlı xətti defolt seçilidir,
> yəni yeni istifadəçi xüsusiyyəti heç nə etmədən görür — **ilk açılış istisna olmaqla, aşağı bax.**

> 🐛 **2026-08-10 (Opus) — İLK AÇILIŞ QÜSURU: təmiz quraşdırmada reader atlas-sız render olunur.**
> **Təkrarlanma (2/2):** `simctl uninstall` → yenidən quraşdır → onboardinq → Quran → Bəqərə.
> Ayə-ayə rejimi (defolt) açılır, «Quran xətti hazırlanır» spinneri görünür, sonra mətn **ağ**
> gəlir. Çipi «Təcvid rəngləri» yerindədir, yəni istifadəçi üçün xüsusiyyət **sınıq görünür**.
> Müshəf rejiminə keçib qayıtmaq — və ya tətbiqi yenidən başlatmaq — **birdəfəlik** düzəldir;
> ondan sonra həmişə rəngli qalır.
>
> **Rəng deyil, ATLAS itir.** Ayrıca əlamət: ilk açılışda ayə nömrələri adi rəqəmlə (`٢`) və sətir
> font metrikaları ilə çıxır; atlas işləyəndə isə naxışlı ayə nişanı və atlas qlif yerləşməsi olur.
> Yəni `ReaderItemsBuilder`-də `atlasBundle == null` → `tajweedEnabled = false` (üç qapının hamısı
> `atlasBundle != null` şərtinə bağlıdır: sətir 308, 477, 683).
>
> **Vaxt möhürü sübutu:** ilk sessiyada siyahı 14:33-də render olundu, atlas teksturu isə
> `Documents/atlas/uthmani_tex0.png` **14:34-də** yazıldı — import siyahı qurulduqdan **sonra**
> bitir və heç nə siyahını yenidən qurmur. `QuranAtlasLoader.isImporting` yalnız spinneri idarə
> edir, invalidasiya siqnalı deyil.
>
> ⚠️ **Mexanizm tam dəqiqləşdirilməyib** — `getBundle` importu gözləməli olduğu halda niyə `null`
> qaytardığı instrumentasiya edilməyib. Baxılacaq iki namizəd:
> 1. `SharedAtlasImporter.importFromBytes` **sabit adlı** müvəqqəti fayl işlədir
>    (`AtlasFiles.tempFile("${bundleKey}_import.zip")`) və onu **mutex-dən əvvəl** yazıb `finally`-də
>    silir. İki paralel çağırışda biri digərinin oxuduğu zip-i üzərinə yaza və ya silə bilər →
>    ikinci çağırış `null` alır.
> 2. Import bitəndə heç bir invalidasiya yoxdur — düzəliş `isImporting` `false`-a düşəndə reader
>    elementlərini yenidən qurmaq ola bilər.
>
> **Təsir:** hər yeni istifadəçi. Bloklayıcıdır ki, əvvəl bu düzəlsin, sonra «Fatihə üçün mədlər»
> kimi dizayn qərarlarına keçilsin.

- [x] ~~**ƏVVƏLCƏ BUNU:** Fatihənin sıfır sinifləri — `tools/tajweed/` generatorunda araşdır.~~
      ✅ **BAĞLANDI (2026-08-10, Opus): qüsur deyil** — mənbə datasında Fatihənin rəngli qaydası
      ümumiyyətlə yoxdur, hizalama sağlamdır. Detallar yuxarıdakı blokda. ⚠️ Generatorda vaxt
      itirmə. Açıq qalan **qərar**: Fatihə üçün mədlər istisna olaraq açılsınmı?
- [ ] 🐛 **İLK AÇILIŞDA RƏNG YOXDUR (2026-08-10-da tapıldı, TƏKRARLANIR)** — aşağıdakı bloka bax.
      Təmiz quraşdırmadan sonra reader ilk dəfə açılanda atlas hazır olmadığı üçün **font fallback**
      ilə render olunur: rəng yoxdur, amma «Təcvid rəngləri» çipi görünür. Rejimi dəyişib qayıtmaq
      və ya tətbiqi yenidən başlatmaq **birdəfəlik** düzəldir. Yəni **hər yeni istifadəçi**
      xüsusiyyəti ilk baxışda sınıq görür.
- [~] Cihazda vizual yoxlama: ✅ **Bəqərə səh. 2 (2026-08-10, v8 datası ilə)** — aşağıdakı bloka bax.
      ⏭️ Qalan nümunələr: Bəqərə 255, müqəttəat (طسٓمٓ kimi), İxlas, alignment-i edit-distance 1-2 ilə
      düzəlmiş ayələrdən nümunələr. (Səh. 2-dəki `الٓمٓ` müqəttəatı rəngsiz render olunur — düzgündür.)
- [x] Toggle off → köhnə davranışla piksel-eyni render ✅ **(2026-08-10, Opus)** — eyni səhifə
      açıq/bağlı çəkilib müqayisə olundu: bağlı halda **78 649 mətn pikselindən 0-ı rəngli**,
      legend çipi də yox olur. Qlif yerləşməsi eynidir: mürəkkəb maskası 78 681 pikselin
      **99.889%-i üst-üstə düşür** (fərq yalnız rəng/ağ sərhədindəki 87 antialiasing pikselidir,
      sürüşmə deyil). Metod: `xcrun simctl io <udid> screenshot` + `tools/tajweed/venv` Pillow.
- [ ] Performans: səhifə skrollunda jank yoxdur (per-qlif ColorFilter keşi işləyir).
- [x] `CREDITS.md`: cpfair/quran-tajweed (CC BY 4.0) + Tanzil attribution ✅ — **artıq yazılıb**
      (2026-08-09-da yoxlandı): `CREDITS.md`-də həm asset cədvəlində (`tajweed.bin` sətri), həm də
      «Tajweed colour data» bölməsində tam attribution var — qayda annotasiyaları CC BY 4.0,
      hizalama üçün işlədilən Tanzil Uthmani mətni (CC BY 3.0, bundle-a girmir), və QA üçün
      işlədilib **paylanmayan** alquran.cloud nəşri. Bənd sadəcə işarələnməmiş qalmışdı.
- [ ] Qərar: Android home-screen widget (`AtlasAyahRasterizer.kt`) təcvid alsınmı?
      (Ayrıca render yoludur — ilk buraxılışda **yox**, reader-only.)
- [ ] Qərar: tafkhim/ra-lam qaydaları öz classifier-imizlə əlavə olunsunmu? (ayrıca iş)
- [x] iOS-da yoxlama ✅ **(2026-08-09)** — iPhone 17 Pro simulyatorunda render yolu təsdiqləndi
      (Bəqərə səhifəsində 16.6% rəngli piksel). Yol boyu Fatihə boşluğu tapıldı, bax yuxarı.

---

## Açıq suallar

1. Tafkhim: ilk buraxılışda buraxılır, yoxsa deterministik qayda yazılır?
2. Widget əhatəsi: reader-only qərarı qətidirmi?
3. Rəng-qayda qruplaşdırmasının KFQPC V4 fontu ilə vizual tutuşdurulması
   (Faza 1 Addım 7-də şəkillərlə təsdiqlənəcək).
