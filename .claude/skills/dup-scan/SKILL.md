---
name: dup-scan
description: app/ və shared/ arasında eyni FQN-li Kotlin siniflərini axtarır — commonMain-ə köçürmədən sonra app-da qalan kopyalar. Bu tələni kompilyator tutmur, xəta yalnız APK runtime-ında NoWhenBranchMatchedException / NoSuchMethodError kimi çıxır. Hər commonMain köçürməsindən sonra işlət.
---

# /dup-scan — dublikat sinif tələsi

## Niyə kompilyator bunu tutmur

`app` və `shared` ayrı modullardır. Eyni FQN hər ikisində olsa, hər modul öz kopyasına qarşı
uğurla kompilyasiya olunur — **kompilyator susur, testlər keçir**. APK-da isə ART iki dex-dən
birini seçir, adətən gözlədiyini yox. Nəticə: `NoWhenBranchMatchedException` və ya
`NoSuchMethodError` — köçürmədən günlər sonra, tamam başqa ekranda.

## İşlət

```bash
python3 .claude/skills/dup-scan/dup-scan.py
```

Dublikat tapılsa çıxış kodu `1` olur.

## İki yoxlama

- **A) eyni (paket, fayl adı)** — fayl `shared`-ə kopyalanıb, `app`-dakı silinməyib.
- **B) eyni FQN-li top-level tip** — tip başqa adlı fayla köçüb, köhnəsi qalıb. A bunu tutmur.

## Yanlış-müsbətlər

Layihədə **qəsdən** eyni paketi iki modul arasında bölən yerlər var — məsələn
`db.entities.hadith`: entity-lər `shared`-də, `toEntity()/toModel()` mapper-ləri `app`-da
(`HadithEntityMappers.kt`), ki çağırıçıların importları pozulmasın. Belə hallarda fayl adları
fərqlidir və B siyahısı boş qalır. Nəticəni buna görə oxu: **B-dəki hər sətir realdır**, A-dakılar
əl ilə baxılmalıdır.

## Tapılandan sonra

1. `app`-dakı kopyanı sil (`shared` versiyası saxlanılır).
2. Qalıq importları yoxla.
3. `/verify` işlət — dörd hədəf də.
