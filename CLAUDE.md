# CLAUDE.md — AnaMuslim

Bu fayl hər Claude Code sessiyasına avtomatik yüklənir. Burada layihənin **koddan və git
tarixçəsindən görünməyən** qaydaları var: iş axını, təkrarlanan tələlər, hansı sənədin harada olduğu.

## Layihə

Quran + Hədis tətbiqi (AlfaazPlus/QuranApp fork-u, GPLv3). Android canlıdır, iOS keçidi davam edir.

- `app/` — Android (Kotlin + Jetpack Compose), `shared/` — KMP + Compose Multiplatform,
  `iosApp/` — iOS host. (`peacedesign/` modulu 2026-08-08-də silindi — miras qalan Android View
  kitabxanası idi, tətbiq tam Compose olduğu üçün qalan 6 istifadəsi əvəzləndi.)
- Backend: Supabase (tərcümə/hədis məzmunu, moderasiya, loglar).

## Sessiyaya başlayanda oxu

| Mövzu | Fayl |
|---|---|
| iOS keçidi — hardayıq, növbəti addım | `IOS_MIGRATION_PLAN.md` → **`## 🔖 HAZIRDA HARDAYIQ`** bölməsi |
| Təcvid rəngləri | `TAJWEED_PLAN.md` |
| Açıq qaynaq yayını | `OPEN_SOURCE_CHECKLIST.md` |
| Supabase sxemi | `docs/supabase/SCHEMA.md` (miqrasiyalar tətbiq olunub, skript faylları saxlanmır) |

İş bitəndə plandakı `- [ ]` qutularını yenilə və status bölməsinə tarixli qeyd əlavə et.

⚠️ `IOS_MIGRATION_PLAN.md` **433 KB-dır** — tam oxuma, kontekstin böyük hissəsini yeyir.
`/plan-status` işlət (cari vəziyyət + son dalğalar + açıq bəndlər, ~19 KB).

## Layihə skilləri

Aşağıdakı qaydaların icra edilən qarşılığı `.claude/skills/`-dədir:

| Skill | Nə edir | Hansı qaydanı icra edir |
|---|---|---|
| `/verify` | dörd hədəf + düzgün JAVA_HOME + `--stop` | İş qaydaları 2, 3, 4 |
| `/plan-status` | plandan yalnız cari vəziyyəti çıxarır | «Sessiyaya başlayanda oxu» |
| `/ios-check` | simulyatorda aç və gör (attach → build → launch → screenshot) | «iOS üçün hədəflər yaşıldır kifayət deyil» |
| `/dep-check` | iOS üçün **həll olunmuş** asılılıq versiyaları | «Asılılıq versiya sürüşməsi tələsi» |
| `/dup-scan` | `app/` ↔ `shared/` eyni FQN axtarışı | «Dublikat sinif tələsi» |

### Google-un Android skilləri (Apache-2.0, kənardan gətirilib)

[github.com/android/skills](https://github.com/android/skills) reposundan **seçilmiş üçü** eyni
qovluqdadır. Layihəyə xas deyil, ona görə ayrıca sadalanır — yeniləmək üçün repodan yenidən kopyala:

| Skill | Nə üçün seçilib |
|---|---|
| `r8-analyzer` | release-də `isMinifyEnabled = true`, `app/proguard-rules.pro` əl ilə yazılıb |
| `perfetto-trace-analysis` | reader sürüşməsi / audio jank araşdırması |
| `android-intent-security` | vidcet PendingIntent-ləri, paylaşma və deep link-lər |
| `android-cli` | aşağıdakı `android` alətinin istifadəsini öyrədir |

Qalan 16-sı quraşdırılmayıb — səbəb: AGP artıq 9.3.1-dədir, layihə onsuz da Compose-dur, camera/wear/
xr/play-billing istifadə olunmur, `navigation-3` isə KMP-də JetBrains naviqasiya publikasiyası ilə
ziddiyyət yaradır.

**Android CLI** quraşdırılıb: `~/.local/bin/android` (PATH `~/.zshrc`-dədir). Faydalı hissəsi
`android docs search <sorğu>` / `android docs fetch <kb-url>` — 4921 sənədlik **oflayn** Android
Knowledge Base, `~/Library/Android/sdk` avtomatik tapılır. ⚠️ `android emulator start` işlətmə —
«emulyatoru soruşmadan başlatma» qaydası buna da şamildir; qurma/işə salma üçün `/verify` və
`/ios-check` qalır.

Sessiya bitəndə `./gradlew --stop` **SessionEnd hook-u ilə avtomatik** işləyir
(`.claude/settings.json`) — əl ilə çağırmağa ehtiyac yoxdur.

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
   ⚠️ **Bu dörd hədəf test mənbələrini QURMUR.** `/verify` yaşıl ola-ola test dəsti aylarla sınıq
   qala bilər — 2026-08-09-da məlum oldu ki, `commonTest`/`iosTest` **2026-07-27-dən bəri**
   kompilyasiya olunmur (bir DAO imzası dəyişib test yenilənməyib; bir test isə okio `Closeable`
   üzərində stdlib `use`-u çağırırdı — okio-nun `Closeable`-ı `kotlin.AutoCloseable` deyil, ona görə
   yalnız JVM-də həll olunur). Kotlin dəyişikliyindən sonra bunu da işlət:
   `:shared:testDebugUnitTest :shared:iosSimulatorArm64Test` (hazırda **iOS 198 / JVM 133**).
   Test faylı yalnız JVM-də keçirsə, bu, iOS-da olmayan API deməkdir.
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
- **Tab kökünü heç vaxt başqa ekranın üstünə push etmə (iOS naviqasiyası):** `AppDestination`-da
  bottom-bar tab kökləri (`Home`/`ReaderIndex`/`HadithIndex`/`Search`/`Settings`) **yalnız bar
  vasitəsilə** açılmalıdır. Bir ekranın üstünə açmaq lazımdırsa detal route-unu işlət
  (`SettingsDetail`/`SearchDetail`). Səbəb: `BottomTabBar` tab dəyişəndə `popUpTo(start)
  { saveState = true }` ilə cari yığını saxlayır — push olunmuş tab kökü həmin yığına düşür və
  `restoreState` sonra **yanlış tab-ı yanlış ekranla** bərpa edir (Quran tab-ına basanda Ayarlar
  açılırdı). Kompilyator da, testlər də tutmur; Android bu tələyə düşmür, çünki orada eyni düymələr
  ayrıca Activity açır.
- **Provider/DI seam qaydası:** hər platformada tətbiqi olan seam → `?: error(...)` (unset = wiring
  səhvi); hansısa platformada hələ tətbiq olunmayan seam → **inert default** (əks halda bir qeydiyyatsız
  provider bütün ekranı yıxır). ⚠️ **İnert default UI-ni azad etmir:** o, çökməni əvəz edir, davranışı
  yox. Seam-in qeydiyyatdan keçib-keçmədiyini bildirən `isAvailable` bayrağı ver və **UI həmin
  əməliyyatı ancaq o zaman təklif etsin** (`WbwAudioDownloadProvider` nümunədir) — yoxsa düymə basılır,
  vərəq açılır və heç nə olmur.
- **Paylaşılan ekrana default-lu davranış callback-i vermə:** `onExport: (…) -> Unit = {}` kimi
  opsional parametr Android host-dan verilir, `AppNavHost`-dan verilmir → iOS-da düymə **səssizcə
  heç nə etmir** (Eksport/İmport aylarla belə qaldı). Davranış platformadan asılıdırsa öz seam-inə
  çıxar (məs. `TextDocumentSaver`/`TextDocumentOpener`), ekran isə öz-özünə yetərli olsun. Parametr
  həqiqətən lazımdırsa **default vermə** — kompilyator onda hər çağırış yerini göstərir.
- **`rememberCoroutineScope()` çox addımlı iş üçün etibarlı deyil:** `applyAppLanguage`
  (→ `AppCompatDelegate`) və Android-də Activity-ni yenidən yaradan hər şey kompozisiyanı dispose
  edir və həmin scope-u **ləğv edir** — qalan addımlar səssizcə düşür (import zamanı dil tətbiq
  olunurdu, ondan sonrakı mövzu/ayarlar isə yox). Belə axını proses-ömürlü scope-da işlət və
  platformanı yenidən yaradan addımı **sonuncu** qoy. Toast/etiket mətnlərini də `getString`
  (suspend) ilə sonradan yox, `stringResource` ilə əvvəlcədən oxu.
- **Tam ekran səth `Dialog` olmalıdır, inline yox:** `ReaderProvider`-in altındakı vərəqlərdən açılan
  tam-ekran ekranları (məs. `QuranImageEditorScreen`) birbaşa emit etmə — `Dialog(properties =
  DialogProperties(usePlatformDefaultWidth = false))` içinə sal. Səbəb: provider həm tam ekranlı
  reader-dən, həm də `QuickReference` kimi **modal vərəqdən** çağırılır; inline emit ikinci halda
  vərəqin popup pəncərəsinin altında qalır və düymə **səssizcə heç nə etmir**. Kompilyator tutmur,
  Android-də də eyni struktur var.
- **Compose Resources-da `%%` yazma:** `Res.string` formatlaması Android `getString`-dən fərqli olaraq
  `%%`-i escape kimi açmır — ekranda hərfi `%%` görünür (`similarVerseRowMeta` buna düşmüşdü). Faiz
  işarəsi lazımdırsa `composeResources/values*/strings.xml`-də tək `%` yaz. Bu fayllar Android
  `R.string` tərəfindən oxunmur, ona görə təhlükəsizdir.
- **Əskik lokalizə sətri səssizcə ingiliscəyə düşür:** Compose Resources açar `values-xx`-də yoxdursa
  `values`-a fallback edir — nə kompilyator, nə test, nə də ekran görüntüsü bunu xəbər verir, sadəcə
  həmin sətir ingiliscə görünür. 2026-08-08-də `values-ru` **169**, `values-tr` **10** sətir geridə
  qalmışdı (plan «üç sətir» sanırdı). Yeni sətir əlavə edəndə dördünü də doldur, sonra yoxla:
  `grep -o 'name="[^"]*"' values/strings.xml | sort -u` çıxışlarını `comm -23` ilə müqayisə et.
  ⚠️ Türkcə App Store-un **əsas lokallaşdırma dilidir** (azərbaycanca dəstəklənmir) — tr boşluğu
  mağaza səviyyəsində görünür.
- **iOS üçün "hədəflər yaşıldır" kifayət deyil** — ekranı simulyatorda aç və gör.
- **Dublikat sinif tələsi:** commonMain-ə tip köçürəndə app-dakı kopyanı **mütləq sil**. Eyni FQN həm
  app-da, həm shared-də qalsa kompilyator susur, amma APK-da ART başqa dex-dəki sinfi yükləyir →
  `NoWhenBranchMatchedException` / `NoSuchMethodError`.
- **Asılılıq versiya sürüşməsi tələsi (yalnız iOS-da partlayır):** Gradle konflikti "ən yüksək versiya
  qalib" ilə həll edir, ona görə A kitabxanası B-ni transitiv olaraq qaldıra bilər — A isə köhnə B-yə
  qarşı kompilyasiya olunub. JVM-də sinif adətən hələ də tapılır (Android susur), Kotlin/Native isə
  simvolu run-time-da axtarır → `No class found for symbol ...`. Kompilyator və testlər bunu **tutmur**.
  Şəbəkə/serializasiya asılılığını dəyişəndə `./gradlew :shared:dependencyInsight --configuration
  iosSimulatorArm64CompileKlibraries --dependency <ad>` ilə **həll olunmuş** versiyanı yoxla, catalog-dakı
  yazını yox. Eyni səbəbdən Ktor engine (`darwin`/`okhttp`) və `ktor-client-core` **bir version ref**
  paylaşmalıdır — engine-lər core-un `@InternalAPI` funksiyalarını çağırır.
- **Glance vidcetlərində lazy siyahı kliki tələsi:** `LazyColumn`/`LazyVerticalGrid` elementlərinə
  `clickable` qoyma. Lazy konteyner RemoteViews kolleksiyasıdır, kolleksiyanın klik şablonu isə
  Android-də **yalnız Activity PendingIntent** ola bilər — ona görə Glance hər sətir toxunuşunu
  `InvisibleActionTrampolineActivity`-dən keçirir. Proses soyuq olanda One UI onu kəsir
  (`Skip pre-destroyed transaction item: LaunchActivityItem{dat=glance-action:...}`) və seçim
  **səssizcə düşür** — siyahı sürüşür, log təmizdir, kompilyator susur. Ölçdüm: 8 toxunuşdan 2-si
  çatdı. Səs çalınarkən proses canlı olduğu üçün test **keçir**, sonra istifadəçidə sınır.
  Əvəzinə adi `Column`/`Row` + offset-lə səhifələmə işlət — lazy konteynerdən kənarda eyni
  `actionRunCallback` broadcast-a çevrilir və düşmür. Səhifələməni ağrısız etmək üçün siyahını cari
  elementin üstündən aç (`RecitationPlayerWidgetUi.kt` → `PickerPager`). Yan qeyd: `GridCells.Fixed`
  yalnız **1–5 sütun** dəstəkləyir, artığı `IllegalArgumentException` verir və launcher-də boş vidcet
  kimi görünür.
- **Vidcet mətnləri `localizedAppContext()` ilə oxunmalıdır:** `wrapContextWithAppLocale` API 33+-da
  bilərəkdən no-op-dur (platforma dili Activity-lərə özü tətbiq edir), amma Glance vidcet
  kompozisiyanı fon worker-ində qurur. `LocaleManager.applicationLocales` ilə SPAppConfigs
  ayrılanda vidcet **sistem dilində** çıxır — surə adları (paylaşılan `AppLocale`) azərbaycanca,
  ətrafındakı bütün etiketlər türkcə idi.
- **AppBar-lar:** geri ikonu həmişə `dr_icon_chevron_left`; mətn başlıqları sola; landscape-də bar
  48dp-ə daralır amma **həmişə görünür**. Yeni bar yazanda
  `compose/components/common/AppBarDefaults.kt` və `CollapsingAppBar.kt`-dən istifadə et, yeni magic
  number əlavə etmə.
- Detallı KMP köçürmə pattern-ləri (expect/actual, resurslar, font, player seam, arxa fon) →
  `IOS_MIGRATION_PLAN.md`.

## Supabase

- Admin e-poçtu bütün RLS siyasətlərində hardcoded: `cafarovceyxun@gmail.com`.
- **Moderasiya axını:** tətbiq `hadith` / `translations` (view) üzərinə yazır → trigger düzəlişi
  `hadith_edits` / `quran_edits`-ə yönləndirir → təsdiq trigger-i əsas cədvələ köçürür.
  İdarəetmə paneli: Ayarlar → Düzəlişləri İdarə Et.
  ⚠️ **İki yol simmetrik deyil:** `intercept_quran_update()`-in **admin qolu yoxdur** — admin daxil
  olmaqla hər tərcümə düzəlişi növbəyə düşür, əsas cədvələ birbaşa yazılmır. `hadith`-də isə admin
  **birbaşa produksiya məzmununu dəyişir**. Ona görə tərcümə moderasiyasını admin hesabı ilə sınamaq
  təhlükəsizdir, hədis moderasiyasını **yox**.
- **RLS bir əməliyyatı bloklayanda PostgREST xəta yox, boş nəticə qaytarır** — yəni yazma "uğurlu"
  görünür, amma heç nə dəyişmir. Klientdə yazma sorğularını `select()` ilə göndər və təsirlənən sətir
  sayını yoxla (`EditsViewModel` nümunəsi).
- **Yeni funksiya yazanda:** `set search_path = public, pg_temp` ver; trigger funksiyasıdırsa
  `revoke execute ... from public, anon, authenticated` et (əks halda PostgREST onu RPC kimi açır).
- **Supabase MCP (`.mcp.json`):** server `zsh -c` ilə işə salınır ki, `~/.zshenv`-dəki
  `SUPABASE_ACCESS_TOKEN` oxunsun — Claude Code Desktop tətbiqi MCP alt-prosesinə dotfile env-i
  ötürmür, birbaşa `npx` çağırışı `Unauthorized` verir. `.mcp.json`-a `env` bloku **əlavə etmə**:
  dəyişən Claude Code-un env-ində olmadığı üçün boş sətrə açılıb shell-dən gələn dəyəri əzir.
  Token repoda saxlanmır, yalnız `~/.zshenv`-dədir.
- Sxem dəyişəndə `docs/supabase/SCHEMA.md`-ni yenilə — miqrasiya SQL faylları saxlanmır, bu sənəd
  bazanın yeganə qeydidir. Yoxlama sorğuları həmin faylın "Yoxlama" bölməsindədir.
