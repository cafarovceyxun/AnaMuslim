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

- **Faza 1-4 bitib.** Qalan: **Faza 5 — cihazda QA** (vizual yoxlama, toggle-off
  regressiya, performans, iOS simulyatorda baxış).
- Faza 2 (data import + DB) və Faza 3 (render) tamamlandı: `tajweed.bin` dekoderi,
  `ExternalQuranDatabase` v4→v5 migration + `TajweedDao`/entity-lər, `TajweedImporter`
  (layout sənəd sırası ilə baza cədvəlini qurur), `TajweedColorSource` (birləşmiş
  qlif-sinifləri keşi + palitra), və `QuranAtlasText` per-qlif rənglənməsi.
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

## Rəng palitrası (qranulyar Türk üslubu — bin şəması v3)

İstifadəçi qərarı (2026-07-24): burunlu qaydaları tək yaşıla yığmaq Al Quran/GreentechApps
tipli istifadəçilər üçün yanlış oxunur (orada yaşıl = qəlqələ). Ona görə hər burun/idğam
qaydası ayrıca sinif alır. Bu cədvəl `tajweed.bin` v3 palitrasının **qəti** mənbəyidir
(rənglər faylın içindəki palitradan oxunur, kodda sabitlənmir).

| Sinif | Hex | Rəng | cpfair qaydaları |
|---|---|---|---|
| 0 | mətn rəngi | — | lam_shamsiyyah |
| 1 | `0xFF999999` | boz | silent, hamzat_wasl, idghaam_no_ghunnah |
| 2 | `0xFFFFC1E0` | çəhrayı | madd_2 |
| 3 | `0xFFFF8E3B` | narıncı | madd_munfasil, madd_246 (ayrı mədd) |
| 4 | `0xFFFF5E8E` | qızılgül | madd_muttasil (bitişik mədd) |
| 5 | `0xFFE30000` | qırmızı | madd_6 (lazımi mədd) |
| 6 | `0xFFB5651D` | qəhvəyi | ghunnah |
| 7 | `0xFF26B55D` | yaşıl | qalqalah |
| 8 | `0xFFC62828` | tünd qırmızı | ikhfa, ikhfa_shafawi |
| 9 | `0xFF9C27B0` | bənövşəyi | idghaam_ghunnah, idghaam_shafawi, idghaam_mutajanisayn, idghaam_mutaqaribayn |
| 10 | `0xFF1976D2` | mavi | iqlab |

Üst-üstə düşmədə prioritet: `5 > 4 > 3 > 2 > 7 > 10 > 9 > 8 > 6 > 1`.
Tafkhim SKIP edilir (cpfair-də qayda yoxdur — atlas rejimində legenddən gizlədilir).

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

- [ ] Cihazda vizual yoxlama: Fatihə, Bəqərə 255, müqəttəat (طسٓمٓ kimi), İxlas,
      alignment-i edit-distance 1-2 ilə düzəlmiş ayələrdən nümunələr.
- [ ] Toggle off → köhnə davranışla piksel-eyni render (regressiya yoxlaması).
- [ ] Performans: səhifə skrollunda jank yoxdur (per-qlif ColorFilter keşi işləyir).
- [ ] `CREDITS.md`: cpfair/quran-tajweed (CC BY 4.0) + Tanzil attribution.
- [ ] Qərar: Android home-screen widget (`AtlasAyahRasterizer.kt`) təcvid alsınmı?
      (Ayrıca render yoludur — ilk buraxılışda **yox**, reader-only.)
- [ ] Qərar: tafkhim/ra-lam qaydaları öz classifier-imizlə əlavə olunsunmu? (ayrıca iş)
- [ ] iOS-da yoxlama (render yolu commonMain-dədir, işləməlidir — amma simulyatorda baxılsın).

---

## Açıq suallar

1. Tafkhim: ilk buraxılışda buraxılır, yoxsa deterministik qayda yazılır?
2. Widget əhatəsi: reader-only qərarı qətidirmi?
3. Rəng-qayda qruplaşdırmasının KFQPC V4 fontu ilə vizual tutuşdurulması
   (Faza 1 Addım 7-də şəkillərlə təsdiqlənəcək).
