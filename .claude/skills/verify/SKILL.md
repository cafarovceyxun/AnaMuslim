---
name: verify
description: AnaMuslim üçün dörd məcburi kompilyasiya hədəfini (app, shared Android, shared iOS simulator, commonMain metadata) düzgün JDK ilə işlədir və sonda Gradle daemon-larını dayandırır. app/, shared/ və ya peacedesign/ altında hər Kotlin dəyişikliyindən sonra, işi "bitdi" hesab etməzdən əvvəl işlət.
---

# /verify — dörd yoxlama hədəfi

## Niyə dördü

Üç platforma hədəfi keçsə də, **common-only stdlib xətalarını yalnız metadata analizi tutur**.
`:shared:compileCommonMainKotlinMetadata` olmadan IDE-də görünən xətalar CLI-də görünmür.
Hədəflərdən birini "vaxta qənaət üçün" atma.

## Əmr

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugKotlin :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :shared:compileCommonMainKotlinMetadata --console=plain 2>&1
```

Sistemdə yalnız JDK 11 var — `JAVA_HOME` prefiksi olmadan build sınır. Prefiksi silmə.

## Bitəndən sonra MÜTLƏQ

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew --stop
```

Gradle/Kotlin/KSP daemon-ları bir neçə GB RAM tutur. Build uğurlu olsa da, olmasa da bunu işlət.

## Nəticəni necə oxu

- **Dördü də `BUILD SUCCESSFUL`** → yalnız o zaman "yoxlama keçdi" de.
- **Yalnız iOS hədəfi sınır, digərləri keçir** → çox güman asılılıq versiya sürüşməsidir, kod deyil.
  `/dep-check` işlət.
- **`NoWhenBranchMatchedException` / `NoSuchMethodError` runtime-da** → kompilyator susur, dublikat
  sinif deməkdir. `/dup-scan` işlət.
- **Hədəflər yaşıldır, amma iş iOS UI-ına toxunub** → kifayət deyil. `/ios-check` işlət və ekranı gör.

## Nə etmə

- Nəticəni "yəqin keçər" deyə təxmin etmə — hər dörd hədəfin çıxışını gör.
- Sınan hədəfi susdurmaq üçün `-q`, `--offline` və ya hədəfi siyahıdan çıxarma.
- Bu skill commit etmir. Commit-i həmişə istifadəçi özü edir.
