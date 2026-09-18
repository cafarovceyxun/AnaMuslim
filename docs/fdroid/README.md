# F-Droid / IzzyOnDroid — hazırlanmış materiallar

Bu qovluqdakı üç fayl **göndərilməyə hazırdır**, amma heç biri özü göndərilmir — hər iki mağaza
GitLab-dadır və müraciət sənin hesabınla açılmalıdır.

| Fayl | Nədir | Hara gedir |
|---|---|---|
| `com.cafarovceyxun.anamuslim.yml` | F-Droid build resepti | <https://gitlab.com/fdroid/fdroiddata> → `metadata/` qovluğuna MR |
| `rfp-issue.md` | RFP mətni (MR açmaq istəmirsənsə) | <https://gitlab.com/fdroid/rfp> → yeni issue |
| `izzyondroid-issue.md` | IzzyOnDroid müraciəti | <https://gitlab.com/IzzyOnDroid/repo> → yeni issue |

⚠️ Resept **bu repoda saxlanılır, burada işləmir**. F-Droid onu öz `fdroiddata` reposunda gözləyir;
buradakı nüsxə sadəcə mənbədir ki, MR açanda yenidən yazmayasan.

## Sənin əlinlə görüləcək addımlar

1. **Tag.** `git tag -a v2026.09.19 -m "2026.09.19" && git push origin v2026.09.19`
   Reseptdəki `commit: v2026.09.19` məhz buna baxır. Tag metadata commit olunandan **sonra**
   atılmalıdır — hər iki mağaza APK-nı və metadata-nı eyni tag-dan oxuyur.
2. **İmzalanmış APK + GitHub Release.** IzzyOnDroid APK-nı GitHub Release-dən götürür.
   `keystore.properties` və `key.jks` repoda yoxdur (düzgün qərar), ona görə bu addım yalnız
   sənin maşınında mümkündür.
3. **İki müraciəti aç** — yuxarıdakı cədvəldəki linklərlə.

## Reseptdə diqqət ediləcək iki yer

- `gradle: [yes]` — YAML-da `yes` sözü açar kimi görünür, amma `fdroiddata`-nın öz konvensiyası
  belədir (dırnaqsız). `fdroidserver` onu sətir kimi oxuyur, dəyişdirmə.
- `UpdateCheckMode: Tags ^v[0-9.]+$` — repodakı `tts-az-quran-v1` tag-ını kənarda saxlayır,
  `v2026.09.19`-u tutur. Yoxlanılıb.

## Gözlənilən iki problem

1. **`NonFreeNet` etiketi.** Məzmun layihənin öz Supabase backend-indən və `api.alfaazplus.com`-dan
   yüklənir, server tərəfi yayımlanmır. Resept bunu **özü elan edir** — gizlətməyə çalışmaq
   müraciəti uzadar.
2. **Buildserver.** Ön şərt 2026-08-07-də AGP 9.3.1 ilə yoxlanmışdı; indi AGP 9.4.0 / Gradle 9.6.0
   və `shared/` modulu iOS hədəfləri elan edir. Debian buildserver-də bunun qurulduğu **yenidən
   yoxlanmayıb** — müraciətdən əvvəl `keystore.properties`-i kənara çəkib
   `:app:assembleRelease` işlətmək bu riski əvvəlcədən bağlayır.
