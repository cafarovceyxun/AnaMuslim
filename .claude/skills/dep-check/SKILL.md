---
name: dep-check
description: Şəbəkə/serializasiya asılılıqlarının iOS üçün HƏLL OLUNMUŞ versiyalarını yoxlayır (catalog-dakı yazını yox). Versiya sürüşməsi yalnız Kotlin/Native-də runtime-da partlayır — kompilyator və testlər tutmur. Ktor, kotlinx-datetime, supabase və ya serialization asılılığını dəyişəndən sonra mütləq işlət.
---

# /dep-check — iOS versiya sürüşməsi

## Tələ nədir

Gradle konflikti **«ən yüksək versiya qalib»** ilə həll edir. Yəni A kitabxanası B-ni transitiv
olaraq qaldıra bilər, halbuki A özü köhnə B-yə qarşı kompilyasiya olunub.

- **JVM/Android:** sinif adətən hələ də tapılır → **Android susur**.
- **Kotlin/Native:** simvolu run-time-da axtarır → `No class found for symbol ...`.

Kompilyator bunu tutmur, testlər də tutmur. Ona görə `libs.versions.toml`-dakı yazıya baxmaq
**kifayət deyil** — həll olunmuş versiyanı görmək lazımdır.

## İşlət

```bash
bash .claude/skills/dep-check/dep-check.sh
```

Kritik asılılıqları (`ktor`, `kotlinx-datetime`, `kotlinx-serialization`, `supabase`) iOS
konfiqurasiyasında yoxlayır. Tək bir asılılıq üçün:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :shared:dependencyInsight --configuration iosSimulatorArm64CompileKlibraries --dependency <ad>
```

## Nəticəni necə oxu

`X -> Y (conflict resolution)` görsən — catalog `X` deyir, real `Y` gəlir. Kimin qaldırdığına bax
(çıxışdakı ağac), sonra qərar ver: catalog-u qaldır, yoxsa qaldıranı endir.

## Bu layihədə məlum sərhədlər (2026-07-28-də həll olunub)

- **Ktor engine ↔ core:** `ktor-client-darwin` və `ktor-client-okhttp` **tək `ktor` version ref-ini**
  paylaşmalıdır. Engine-lər core-un `@InternalAPI` funksiyalarını çağırır — sürüşmə dərhal partladır.
  Ayrı `ktorClientOkhttp` açarı bilərəkdən silinib, geri əlavə etmə.
- **kotlinx-datetime qapanı:** Compose MP material3 1.9.0 → **0.7.1** tələb edir.
  - supabase-kt **3.1.1** 0.6.1-ə qarşı qurulub → `InstantIso8601Serializer` tapılmır (iOS-da).
  - supabase-kt **3.6.0** 0.7.1-ə qarşı qurulub → **işləyən variant budur** (Ktor 3.4.3 tələb edir).
  - ⚠️ supabase-kt **3.7.x-ə qaldırma** — 0.8.0 istəyir, material3 isə 0.7.1-də qalıb, eyni tələ
    tərsinə işləyəcək.

## Sonra

Versiya dəyişdinsə `/verify` (dörd hədəf) **və** `/ios-check` işlət. Yaşıl hədəflər bu tələyə
qarşı sübut deyil — xəta yalnız simulyatorda ekran açılanda çıxır.
