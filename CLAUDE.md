# CLAUDE.md — AnaMuslim

Bu fayl hər Claude Code sessiyasına avtomatik yüklənir. Burada layihənin **koddan və git
tarixçəsindən görünməyən** qaydaları var: iş axını, təkrarlanan tələlər, hansı sənədin harada olduğu.

## Layihə

Quran + Hədis tətbiqi (AlfaazPlus/QuranApp fork-u, GPLv3). Android canlıdır, iOS keçidi davam edir.

- `app/` — Android (Kotlin + Jetpack Compose), `shared/` — KMP + Compose Multiplatform,
  `iosApp/` — iOS host, `peacedesign/` — dizayn komponentləri.
- Backend: Supabase (tərcümə/hədis məzmunu, moderasiya, loglar).

## Sessiyaya başlayanda oxu

| Mövzu | Fayl |
|---|---|
| iOS keçidi — hardayıq, növbəti addım | `IOS_MIGRATION_PLAN.md` → **`## 🔖 HAZIRDA HARDAYIQ`** bölməsi |
| Təcvid rəngləri | `TAJWEED_PLAN.md` |
| Açıq qaynaq yayını | `OPEN_SOURCE_CHECKLIST.md` |
| Supabase sxemi | `docs/supabase/SCHEMA.md` (miqrasiyalar tətbiq olunub, skript faylları saxlanmır) |

İş bitəndə plandakı `- [ ]` qutularını yenilə və status bölməsinə tarixli qeyd əlavə et.

## İş qaydaları

1. **Commit-i həmişə istifadəçi edir.** Assistant heç vaxt commit etməsin və "commit edim?" deyə
   soruşmasın — sadəcə dəyişiklikləri işlək ağacda hazır saxla. (Səbəb: commit mesajları və vaxtı
   istifadəçinin öz formatındadır.)
2. **Build JDK:** sistemdə yalnız JDK 11 var, ona görə
   `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew ...`
3. **Build bitəndən sonra `./gradlew --stop`** — Gradle/Kotlin/KSP daemon-ları bir neçə GB RAM tutur.
4. **Yoxlama hədəfləri (dördü də):** `:app:compileDebugKotlin`, `:shared:compileDebugKotlinAndroid`,
   `:shared:compileKotlinIosSimulatorArm64`, **`:shared:compileCommonMainKotlinMetadata`**.
   Sonuncusu vacibdir: üç platforma hədəfi keçsə də, common-only stdlib xətalarını yalnız metadata
   analizi tutur — IDE-də görünüb CLI-də görünməyən xətaların səbəbi budur.
5. **Debug paket `com.cafarovceyxun.anamuslim.test`-dir** (`applicationIdSuffix = ".test"`).
   Suffikssiz `com.cafarovceyxun.anamuslim` istifadəçinin **Play Store produksiya** tətbiqidir —
   ona toxunma, onu açıb "düzəlmədi" nəticəsi çıxarma.
6. **İş bölgüsü:** mexaniki/təcrid olunmuş tapşırıqlar → paralel Sonnet subagent; dizayn-həssas,
   coupling-ağır işlər → əsas model (Opus) özü. Sonnet-lər `build.gradle`/version-catalog-a toxunmur,
   merge qapısı Opus-dadır. Paralel subagent worktree ilə işlədiyi üçün əvvəl istifadəçi commit etməlidir.

## Kod konvensiyaları

- **commonMain-də həmişə `viewModel { T() }`** — `viewModel<T>()` (factory-siz) Kotlin/Native-də
  refleksiya olmadığı üçün iOS-da runtime çökməsi verir. Kod kompilyasiya olunur, testlər keçir,
  xəta yalnız ekran render olunanda çıxır.
- **Provider/DI seam qaydası:** hər platformada tətbiqi olan seam → `?: error(...)` (unset = wiring
  səhvi); hansısa platformada hələ tətbiq olunmayan seam → **inert default** (əks halda bir qeydiyyatsız
  provider bütün ekranı yıxır).
- **iOS üçün "hədəflər yaşıldır" kifayət deyil** — ekranı simulyatorda aç və gör.
- **Dublikat sinif tələsi:** commonMain-ə tip köçürəndə app-dakı kopyanı **mütləq sil**. Eyni FQN həm
  app-da, həm shared-də qalsa kompilyator susur, amma APK-da ART başqa dex-dəki sinfi yükləyir →
  `NoWhenBranchMatchedException` / `NoSuchMethodError`.
- **AppBar-lar:** geri ikonu həmişə `dr_icon_chevron_left`; mətn başlıqları sola; landscape-də bar
  48dp-ə daralır amma **həmişə görünür**. Yeni bar yazanda
  `compose/components/common/AppBarDefaults.kt` və `CollapsingAppBar.kt`-dən istifadə et, yeni magic
  number əlavə etmə.
- Detallı KMP köçürmə pattern-ləri (expect/actual, resurslar, font, player seam, arxa fon) →
  `IOS_MIGRATION_PLAN.md`.

## Supabase

- Admin e-poçtu bütün RLS siyasətlərində hardcoded: `cafarovceyxun@gmail.com`.
- **Moderasiya axını:** tətbiq `hadith` / `translations` (view) üzərinə yazır → trigger admin olmayanı
  `hadith_edits` / `quran_edits`-ə yönləndirir → təsdiq trigger-i əsas cədvələ köçürür.
  İdarəetmə paneli: Ayarlar → Düzəlişləri İdarə Et.
- **RLS bir əməliyyatı bloklayanda PostgREST xəta yox, boş nəticə qaytarır** — yəni yazma "uğurlu"
  görünür, amma heç nə dəyişmir. Klientdə yazma sorğularını `select()` ilə göndər və təsirlənən sətir
  sayını yoxla (`EditsViewModel` nümunəsi).
- **Yeni funksiya yazanda:** `set search_path = public, pg_temp` ver; trigger funksiyasıdırsa
  `revoke execute ... from public, anon, authenticated` et (əks halda PostgREST onu RPC kimi açır).
- Sxem dəyişəndə `docs/supabase/SCHEMA.md`-ni yenilə — miqrasiya SQL faylları saxlanmır, bu sənəd
  bazanın yeganə qeydidir. Yoxlama sorğuları həmin faylın "Yoxlama" bölməsindədir.
