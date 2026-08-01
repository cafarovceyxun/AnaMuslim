---
name: ios-check
description: iOS dəyişikliyini simulyatorda həqiqətən açıb görmək üçün — panel aç, xcodebuild ilə qur, işə sal, ekran görüntüsü al. "Dörd hədəf də yaşıldır" iOS üçün kifayət deyil; UI/naviqasiya/asılılıq dəyişikliyindən sonra bunu işlət.
---

# /ios-check — ekranı simulyatorda gör

## Niyə

Kompilyasiya hədəfləri keçdiyi halda iOS-da partlayan ən azı üç sinif xəta var:

- `viewModel<T>()` (factory-siz) — Kotlin/Native-də refleksiya yoxdur, **render anında** çökür.
- Asılılıq versiya sürüşməsi — `No class found for symbol ...`, run-time-da.
- Qeydiyyatsız provider/seam — inert default yoxdursa bütün ekran yıxılır.

Üçünü də yalnız ekranı açmaqla görürsən.

## Ardıcıllıq

**1. ƏVVƏLCƏ paneli aç** (build-dən əvvəl — ucuzdur, istifadəçi prosesi izləsin):

`mcp__Claude_Code_iOS_Simulator__control` → `action: "attach"`

Simulyator boot olunmayıbsa səhv qaytarır; əvvəl boot et, sonra yenidən attach:

```bash
xcrun simctl list devices booted
```

**2. Qur.** `mcp__Claude_Code_iOS_Simulator__build` (`project_path: iosApp/iosApp.xcodeproj`,
`scheme: iosApp`, `configuration: Debug`) — və ya birbaşa:

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'platform=iOS Simulator,id=<UDID>' -derivedDataPath iosApp/build/dd build
```

ℹ️ `-derivedDataPath` artıq **məcburi deyil** (2026-08-01-də `gradlew`-in 23-cü sətrindəki sitat
səhvi düzəldildi — əvvəl Xcode-un DerivedData sandbox-ında `APP_HOME` səhv düşüb
`ClassNotFoundException: GradleWrapperMain` verirdi). Yenə də tövsiyə olunur: build çıxışı
repo daxilində qalır və təmizləmək asan olur.

**3. İşə sal.** `control` → `action: "launch"`, `app_path` build nəticəsindəki `.app` yolu.

**4. Gör.** `control` → `action: "screenshot"`. Dəyişdirdiyin ekrana `tap`/`swipe` ilə naviqasiya
et və **həmin ekranın** görüntüsünü al — başlanğıc ekranı sübut deyil.

## Layihə məlumatları

| | |
|---|---|
| Xcode layihəsi | `iosApp/iosApp.xcodeproj` (tək target) |
| Scheme | `iosApp` |
| Bundle ID | `com.cafarovceyxun.anamuslim` (`project.pbxproj`-dan) |

## Nəticəni necə hesabat ver

- Ekranı **görmüsənsə** — "simulyatorda yoxlandı" de və nəyi gördüyünü yaz.
- Build keçib, ekranı açmamısansa — bunu açıq de. "Hədəflər yaşıldır" ilə qarışdırma.
- Çökmə olsa `xcrun simctl spawn booted log stream --level debug` və ya launch çıxışındakı
  stack trace-i oxu; `No class found for symbol` görsən → `/dep-check`.
