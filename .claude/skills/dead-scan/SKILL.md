---
name: dead-scan
description: Yazılıb heç vaxt işə düşməyən kodu tapır — heç yerdən çağırılmayan funksiyalar (obyekt üzvləri `Obyekt.ad` ilə dəqiq yoxlanılır) və yazılıb oxunmayan / heç istifadə olunmayan DataStore açarları. Kompilyator, testlər və ekran bunu tutmur: funksiya düzgündür, sadəcə heç kim onu çağırmır (dua «Açılış rejimi» ayarı belə ölü qalmışdı). Ayar, seam, provider və ya Preferences funksiyası əlavə edəndən / çağırış yerini silib dəyişəndən sonra işlət.
---

# /dead-scan — yazılan, amma işə düşməyən kod

## Niyə lazımdır

2026-09-18-də dua bölməsinin «Açılış rejimi» ayarı ölü çıxdı: ayarlar vərəqi
`dua.default_v_mode`-u yazırdı, onu tətbiq edən `DuaPreferences.applyDefaultViewMode()` isə
**heç yerdən çağırılmırdı**. Seçim edəndə rejim dərhal dəyişdiyi üçün ayar işləyən kimi
görünürdü, növbəti açılışda isə təsiri yox idi. Kompilyator public funksiyanın istifadəsiz
qalmasına susur, testlər funksiyanın özünü yoxlayır, çağırılıb-çağırılmadığını yox.

Adla axtarış bu halı **tutmur**: `HadithPreferences.applyDefaultViewMode` beş yerdən çağırılırdı,
`grep applyDefaultViewMode` dolu qayıdırdı. Skript obyekt üzvlərini `Obyekt.ad` forması ilə
axtarır. Köhnə commit-də (`f16a5bf`) sınanıb, səhvi tutur.

## İşlət

```bash
python3 .claude/skills/dead-scan/dead-scan.py              # baseline-dan TƏZƏ tapıntılar
python3 .claude/skills/dead-scan/dead-scan.py --changed    # yalnız main-dən dəyişən fayllar
python3 .claude/skills/dead-scan/dead-scan.py --all        # hamısı, baseline daxil (+ C3 məlumatı)
python3 .claude/skills/dead-scan/dead-scan.py <yol> ...    # yol süzgəci (fayl və ya qovluq)
```

~8 saniyə, Gradle lazım deyil (mətn analizidir). Təzə tapıntı varsa çıxış kodu `1` olur.

⚠️ `--changed` yalnız **dəyişən fayllardakı elanları** göstərir. Başqa fayldakı funksiyanın son
çağırışını sildinsə, ölü qalan funksiya dəyişməyən fayldadır: onu süzgəcsiz işə salmaq tutur.

## Yoxlamalar

| Kod | Nə | Necə |
|---|---|---|
| **A** | obyekt üzvü funksiya heç yerdən çağırılmır | başqa faylda `Obj.ad` / `Obj::ad`, Swift `Obj.shared.ad`, Java `Obj.INSTANCE.ad`; öz faylında, `import …Obj.ad` və ya `with(Obj)` olan faylda adın özü |
| **B** | qalan funksiya (sinif üzvü, top-level, extension), adı başqa heç yerdə keçmir | adla; `private` və lokal funksiyalar yalnız öz faylında |
| **C1** | DataStore açarı yazılır, heç vaxt oxunmur | `write/remove/[k] =` ↔ `read/readFirst/observe/flow/[k]` |
| **C2** | DataStore açarı heç istifadə olunmur | |
| **T** | yalnız testlərdən çağırılır | məlumat, çıxış koduna təsir etmir |
| C3 | açar oxunur, kodda yazılmır | yalnız `--all`-da; çox vaxt normaldır |

İki incəlik:

- **Ölü koddakı çağırış sayılmır** (sabit nöqtəyə qədər təkrarlanır). Yalnız ölü Composable-dan
  çağırılan köməkçi də ölüdür; açarın oxunduğu yeganə funksiya ölüdürsə açar da oxunmur. Hesabat
  bunu `↳ yalnız ölü koddan: …` / `↳ oxuyan funksiyalar ölüdür: …` kimi göstərir.
- **Açarın kimliyi sətir adıdır**, Kotlin `val`-ı deyil: `ReaderIndexFavouritesMigration.KEY`
  yazır, `ReaderIndexViewModel.KEY` oxuyur, ikisi də `"favourite_chapters"`-dir. Ad literal,
  eyni fayldakı `const val` və ya `Obyekt.SABİT` vasitəsilə həll olunur.

Şərhlər və sətir mətni axtarışdan əvvəl silinir (KDoc-dakı `[ad]` istifadə sayılmır),
`"${çağırış()}"` şablonları isə saxlanılır.

## Tapıntını necə oxumaq

Hər sətir üçün bir sual ver: **«bu işə düşməli idi?»**

- **Bəli** → çağırış yeri əskikdir. Dua açılış rejimi bu növ idi. Funksiyanı silmə, onu bağla:
  çağırış yerini tap (adətən eyni funksiyanın hədis/Quran qarşılığı hardan çağırılırsa, oradan).
- **Yox** → miras qalıb. Sil, sonra `/verify` işlət.

Xüsusi diqqət: `set…`/`observe…`/`apply…` cütündən **biri** ölüdürsə (yazan var, oxuyan yox və
ya əksinə), bu, demək olar həmişə yarımçıq qoşulmuş ayardır.

## Yanlış-müsbətlər (skript bunları buraxır)

- `override`, `operator`, `actual`, `external` funksiyalar: çərçivə, sintaksis və ya expect tərəfi
  çağırır. `expect` elanı isə yoxlanılır.
- `@Preview`, `@TypeConverter`, `@Test`, `@JavascriptInterface`, `@ObjCAction`, `@Keep`.
- `NSSelectorFromString("ad:")` ilə verilən ObjC selector adları.
- Sintaksis adları (`invoke`, `getValue`, `componentN`, `plus` …).

Skriptin **görmədiyi** hallar (tapılsa, əl ilə yoxla):

- refleksiya və ya sətir adı ilə çağırış (məs. Room/Glance-ın sinif adı ilə yüklədiyi şeylər);
- Swift-də `@ObjCName` ilə dəyişdirilmiş ad (layihədə hazırda yoxdur);
- `Obj.run { ad() }` xaricindəki scope funksiyaları (`with`, `run`, `apply`, `also` tanınır).

B adla işlədiyi üçün **az tutur**: `getLayout` kimi ümumi adlı funksiya başqa yerdə eyni adla
keçirsə, ölü olsa da görünməz. A belə deyil, obyekt üzvləri üçün dəqiqdir.

## Baseline

`baseline.txt` ilk işlətmədəki **bilinən** tapıntılardır (2026-10-01: 79 A, 119 B, 4 C2, 32 T).
Default rejim yalnız onlardan **təzə** olanları göstərir. Tapıntını düzəltdinsə və ya sildinsə:

```bash
python3 .claude/skills/dead-scan/dead-scan.py --update-baseline
```

Baseline-a əl ilə sətir **əlavə etmə**. Yanlış-müsbət tapsan skripti düzəlt. Bilinən tapıntını
yenidən görmək üçün sətrini sil.
