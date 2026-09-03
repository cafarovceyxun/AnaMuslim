# Atlas generatoru

`shared/src/commonMain/composeResources/files/atlas/<skript>/6x.zip` faylını qurur.

```bash
python3 -m venv .venv && .venv/bin/pip install -r requirements.txt
.venv/bin/python gen_atlas.py --script uthmani
```

Referansla tutuşdurmaq üçün:

```bash
.venv/bin/python gen_atlas.py --script uthmani --out /tmp/out \
    --verify ../../shared/src/commonMain/composeResources/files/atlas/uthmani/6x.zip
```

## Atlas nədir

Tətbiqin mushaf rejimi qliflərini **əvvəlcədən hesablanmış** shaping nəticəsindən çəkir: HarfBuzz-un
hər söz forması üçün verdiyi qlif axını, üstəgəl həmin qliflərin bir teksturda toplanmış rasteri.
Beləliklə tətbiq nə shaping mühərriki daşıyır, nə də işləmə vaxtında rasterləşdirir.

Paket dörd fayldan ibarətdir; müqaviləni `QuranAtlasModel.kt` və `AtlasLayoutParser` təyin edir:

| Fayl | Nə |
|---|---|
| `meta.json` | font metrikaları, `kind`, `layout` və `sizes[].textures[]` |
| `atlas.json` | qlif ID → teksturadakı düzbucaqlı + bearing + advance |
| `layout.json` | `documents{}` — hər söz forması və onun qlif axını (font vahidində) |
| `atlas_0.png` | boz-tonlu qlif teksturu |

## Giriş məlumatı — hamısı repodadır

- **söz siyahısı**: `app/src/main/assets/db/quranapp.db` → `ayah_words` (uthmani üçün **21 497** forma)
- **font**: `shared/src/commonMain/composeResources/font/uthmanic_hafs.ttf` (KFGQPC)

Xarici yükləmə lazım deyil. Əvvəl bu paketlər `AlfaazPlus/QuranAppInventory`-dən gəlirdi.

## Yoxlanılmış nəticələr (2026-09-03)

Yuxarı axının paketi ilə tutuşdurma:

- **shaping: 21497/21497 dəqiq uyğun** — eyni HarfBuzz, eyni font, eyni nəticə
- qliflər: 747 ortaq, 291 bayt-dəqiq, **745/747 ±1 piksel daxilində**
- cihazda: müshəf səhifəsi düzgün render olundu, təcvid rəngləri işlədi

## Tələlər

⚠️ **`FT_LOAD_NO_HINTING` şərtdir.** Hinting qlif konturunu piksel şəbəkəsinə oturtmaq üçün əyir,
halbuki mövqeləmə font vahidində hesablanıb. Hinting açıq qalsa hərflər sözün içində sürüşür —
kompilyator da, test də bunu görmür, yalnız ekranda bilinir.

⚠️ **Bayt-eyni çıxış gözləmə.** Referans başqa rasterizator işlədib, qlif ölçüləri 1–2 piksel
fərqlənir. Əhəmiyyəti yoxdur: `atlas.json` hər qlifin düzbucaqlısını özü elan edir, renderer sabit
gözləmir. Vacib olan **daxili uyğunluqdur** — paketlənən şəkil ilə yazılan metrika eyni mənbədən.

⚠️ **QuranAppInventory-dəki bəzi paketlər KÖHNƏ sxemdədir** (`words.json`, tək `atlas.png`).
Tətbiq onları oxumur. Format çıxarmaq üçün paketdəki fayla bax, endirilənə yox.

⚠️ **`sizes[].atlas` fayl adıdır, ölçü deyil** — importer məhz həmin adla `atlas.json`-u açır.

## Nə hələ qurulmur

`dk_indopak` (22 526 söz forması bazadadır) — fontu repoda yoxdur, hələ
`AlfaazPlus/QuranAppInventory`-dən endirilir. Digital Khatt açıq qaynaqdır; fontu gətirib `FONTS`
cədvəlinə əlavə etmək kifayətdir.
