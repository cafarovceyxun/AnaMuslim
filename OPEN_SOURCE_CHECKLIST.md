# Open-Source Release Checklist

Tracking what remains before AnaMuslim is fully published as an independent,
GPLv3 open-source project. Fork of
[AlfaazPlus/QuranApp](https://github.com/AlfaazPlus/QuranApp).

## ✅ Done

- [x] GPLv3 `LICENSE`
- [x] `NOTICE` with upstream attribution (AlfaazPlus/QuranApp, Faisal Khan)
- [x] `README.md`, `CONTRIBUTING.md`, `CONTRIBUTORS.md`
- [x] No dangerous secrets in repo or git history — no keystore, no
      `service_role` key, no `local.properties`, no `google-services.json`.
      (The Supabase **anon** key in `SupabaseProvider.kt` is a publishable
      client key, already shipped in the APK — safe to be public **if RLS is
      correct**; see item 2.)
- [x] `.gitignore` covers `local.properties`, keystores, `keys/`
- [x] `FUNDING.yml` does not route funding to upstream
- [x] CI workflow (`assembleDebug`) + issue templates
- [x] In-app repo / bug-report links point to `cafarovceyxun/AnaMuslim` (`ApiConfig.kt`).
      **Fixed 2026-07-27:** `AboutScreen` no longer prefers the upstream `urls.json`
      (feedback / help / **privacy policy** used to open `quran.alfaazplus.com`
      whenever that fetch succeeded), and `GITHUB_ISSUES_BUG_REPORT_URL` now asks for
      `?template=bug_report.md`, which is the file the repo actually ships. The dead
      `GITHUB_ISSUES_VERSE_REPORT_URL` was removed — verse reports go to Supabase.
- [x] Git remote set to `cafarovceyxun/AnaMuslim.git`
- [x] `CREDITS.md`, `PRIVACY.md`, `CODE_OF_CONDUCT.md`

## 🔜 Do soon (agreed)

### 1. Asset licensing audit — ⏳ mostly done
- [x] **KFGQPC fonts** (`inventory/fonts/kfqpc_v1/`): license permits free
      use/copy/distribution if **unmodified & not sold**. Added
      `inventory/fonts/kfqpc_v1/LICENSE.txt`; documented in [CREDITS.md](CREDITS.md).
      ✅ OK to bundle.
- [x] **Audited the remaining `inventory/` assets (2026-07-24)** — filled the
      per-item table in [CREDITS.md](CREDITS.md):
  - `quran_scripts` (KFQPC Arabic text) — Tanzil Text License / CC BY 3.0, OK
    unmodified with credit. ✅
  - `wbw` and `recitations` — only **manifests** are bundled; the bulk data/audio
    is fetched at runtime. No copyrighted bulk content in the repo. ✅
  - `translations` — mixed: 4 are public-domain/generated (Pickthall, Yusuf Ali,
    Elmalılı, transliteration); most others are copyrighted but OK for this free,
    non-commercial app under Tanzil terms (keep credit, unmodified).
- [x] **Removed `inventory/translations/en/en_abdul-haleem.json`** — © Oxford
      University Press does **not** permit bundled redistribution. Deleted from the
      repo and de-listed from `available_translations_info.json`. The app fetches
      inventory from the remote mirror at runtime, so this does not affect users.

### 1b. Slim the repo — ✅ DONE (2026-07-24, no LFS)
- [x] **Removed the 144 MB `inventory/fonts/kfqpc_v1/` (~600 `.TTF` + `.zip`) from
      the whole git history** with `git filter-repo` (kept `LICENSE.txt`), then
      force-pushed. `.git` dropped ~178 MB → ~21 MB. Chose removal over LFS because
      the app never used these files — it downloads fonts at runtime, now from this
      project's own GitHub Release (see item 2). LFS would have added GitHub quota
      limits and a contributor dependency for no benefit.
- [x] Re-pointed the `qpc` release tag to the slimmed commit so the fat history is
      not kept alive on the server by the tag.

### 2. Reduce runtime dependency on third-party / personal infrastructure
The app fetches data at runtime from services owned by others (and one personal
Supabase backend). Plan:
- [x] **Supabase RLS audit done (2026-07-23).** All tables have RLS enabled.
      Found and fixed two anon-write holes — see `docs/supabase/rls_hardening.sql`
      (run it in the SQL Editor before going public):
  - `hadith_volume/book/chapter/sub_chapter` had `"Allow all for anon"` → anon
    could edit/delete the whole hadith structure. **Fixed** (dropped; anon read-only).
  - `daily_content` had `"Authorized Write: ALL(public)"` (public includes anon)
    → anon could write daily content. **Fixed** (scoped to authenticated).
  - Verified safe: `app_logs` (anon insert-only, no read/delete), `quran_edits` &
    `hadith_edits` (no anon policy → editor emails protected), read-only content
    tables, `verse_reports`.
  - Also tightened over-broad anon table GRANTs (defense-in-depth) in the same file.
- [x] **RLS hardening applied** — `docs/supabase/rls_hardening.sql` has been run in
      the Supabase SQL Editor (2026-07-24). Anon-write holes closed; safe to go public.
- [ ] **Anon key note:** the key in `SupabaseProvider.kt` is a publishable **anon**
      key (not `service_role`), already shipped in every APK — safe to be public
      now that RLS is correct. Optional: move URL + anon key to build config
      (`local.properties`/CI) so forks use their own backend.
- [x] **`app_logs` admin viewer fixed** — added `authenticated` SELECT + DELETE
      policies (and grants) in `rls_hardening.sql`, so the in-app admin log viewer
      (`AppLogsScreen`) can read and clear logs once signed in. Anon stays
      insert-only.
- [ ] **AlfaazPlus / upstream data** — make base URLs overridable (they are
      already centralized in `ApiConfig.kt`), then optionally self-host a fork of
      the data:
  - [x] **Translations come only from Supabase (2026-07-24).** The app shows a
        single `az` book (hard-coded in `TranslationViewModel.mergeTranslations`) and
        downloads it from the Supabase Postgres `translations` table
        (`SharedTranslationDownloader` / `TranslationDownloadWorker`). Removed the
        vestigial `available_translations_info.json` manifest fetch entirely, so the
        translation list is built from local state and **never touches AlfaazPlus**.
  - `api.alfaazplus.com`, `gh-proxy.alfaazplus.com` — `ApiConfig.kt` (scripts, recitations, wbw)
  - [x] **`AboutScreen` support links no longer resolve to upstream (2026-07-27).**
        The screen used to fetch `inventory/other/urls.json` from the AlfaazPlus repo
        (`GithubApi.getAppUrls()`) and prefer it over the `ApiConfig` constants, so a
        successful fetch sent "Send feedback", "Help & support" and **"Privacy
        policy"** to `quran.alfaazplus.com`. It now always uses this project's own
        links, and the whole upstream-urls path was deleted with it:
        `GithubApi.getAppUrls()`, the `AppUrls` model, `InfoUtils.kt`,
        `UrlsManager.kt`, and the `urls` leg of `ResourceUpdateManager` (so the app
        no longer downloads or caches the upstream `urls.json` at all).
  - [x] `ScriptFontInstaller.kt` — KFQPC page-font archives now come from **this
        project's own GitHub Releases** via `ApiConfig.QPC_FONT_RELEASE_BASE_URL`
        (`…/cafarovceyxun/AnaMuslim/releases/download/qpc/`), no longer AlfaazPlus.
        **Done (2026-07-24):** created the `qpc` release and uploaded the three
        archives (`qpc_v1_by_page.tar.gz`, `qpc_v2_by_page.tar.gz`,
        `qpc_v4_tajweed_by_page.tar.gz`). Fonts now fully self-hosted.
  - `GH_PROXY_BASE_URL` / `JS_DELIVR_BASE_URL` / `GH_RAW_BASE_URL` → `AlfaazPlus/QuranApp@master`

### 4. Branding / icons — ✅ checked
- [x] App name **"Ənə Muslim"**, applicationId `com.cafarovceyxun.anamuslim`,
      funding empty — all own branding, no upstream trademark.
- [x] Launcher icons are custom (not upstream's).
- [x] **Icons unified (2026-07-24)** — regenerated a single `ic_launcher` set from
      `muslim.svg` via Asset Studio (adaptive + round + monochrome/themed + all
      density rasters, fixing the earlier missing API 24–25 fallbacks). Pointed
      `ShortcutUtils.kt` at `R.mipmap.ic_launcher`, and removed the old duplicate
      `icon_launcher` set (main + debug source sets), the orphaned
      `drawable/ic_launcher_background.xml` vector, and the stale
      `icon_launcher-playstore.png`. Verified with `:app:processDebugResources`.
      Remaining leftover: `shared/…/composeResources/drawable/icon_launcher_foreground.xml`
      (Compose/iOS side, no code usage found) — handle with the iOS icon later.
- [x] **`peacedesign/` confirmed OK to be public (2026-07-24)** — a `com.peacedesign`
      Android UI-utility library (dialogs, spans, view utils) © Faisal Khan
      (`github.com/faisalcodes`, per source headers), part of the upstream QuranApp
      project and covered by the existing [NOTICE](NOTICE) attribution. Actively used
      (`app/build.gradle.kts` → `implementation(project(":peacedesign"))`, imported in
      7 files), not dead code. `peacedesign/build/` is git-ignored; no third-party
      restrictive licenses found. No action needed.

## 📋 Recommended before / around release

- [x] **Release signing** — wired up in `app/build.gradle.kts`: reads
      `keystore.properties` (git-ignored) and signs release builds when present;
      falls back to unsigned for CI/forks. Template: `keystore.properties.example`.
      **Still needed to actually sign:** create the keystore, add `keystore.properties`
      locally (or the four values as CI secrets), keep both OUT of the repo.
- [x] Verify app **icons / branding** are original — icons are now generated from
      the project's own `muslim.svg` (see item 4); name/appId/funding are own branding.
- [x] Added repo **description, topics, and website** on GitHub (2026-07-24).
- [ ] Add CI for **iOS build** (currently only `assembleDebug`) once migration stabilizes.
- [x] Optional: `.vscode/` is git-ignored (`.gitignore` line 63) and not tracked — done.
- [x] `SECURITY.md` present (how to report vulnerabilities).
- [ ] Tag a first release (`v…`) with source + prebuilt debug/release APK.

## 📦 F-Droid / IzzyOnDroid distribution (2026-08-06)

### Signing — resolved, with a caveat
- [x] **Release signing verified end to end.** `keystore.properties` now points at the existing
      key (`key.jks`, alias **`key0`**); `:app:assembleRelease` succeeds and `apksigner verify`
      reports a valid v2 signature, `CN=Jeyhun Jafarov`, RSA 2048.
      ⚠️ The store/key password **ends with a trailing space** — editors that trim trailing
      whitespace on save silently break the build. Consider `keytool -storepasswd` /
      `-keypasswd` to remove it; that changes neither the key nor the resulting signature.
- [x] **Play App Signing is enabled — confirmed 2026-08-06.** Play's app signing certificate is
      `66:8E:79:FE:…:DC:C7:90`; `key.jks` is `28:04:8A:BC:…:87:AD:CB`, i.e. only the **upload
      key**. Google holds the distribution key, so **the Play signature cannot be reproduced
      outside Play**. Consequence: an APK published here cannot be installed over a Play install
      (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`), and F-Droid's own build would be a third signature.
      **Decision: keep the same `applicationId`** and document the switch (export → uninstall →
      install → import) rather than forking the package name.

### Release APK facts (3.1.6, versionCode 114111137)
- Size **26 MB** — fits IzzyOnDroid's ~30 MB budget, but that budget is *per app*, so in practice
  only one version will be retained there. `app/src/main/assets/db` is 25 MB of it; moving it to a
  runtime download (the mechanism already used for fonts) is the lever if space gets tight.
- No `debuggable` / `testOnly` flags. `minSdk = 24`, so the v2-only signature is sufficient
  (v1/JAR signing is only needed below API 24).

### Metadata
- [x] **`fastlane/metadata/android/` created** for `en-US`, `az`, `tr`, `ru` — title, short and
      full description, changelog `114111137.txt`, and a 512×512 `images/icon.png` copied from
      `app/src/main/ic_launcher-playstore.png`. All length limits verified. See
      [fastlane/README.md](fastlane/README.md).
- [ ] **Screenshots** — `images/phoneScreenshots/` is empty in all four locales. Both stores
      need at least two or the listing looks broken.

### Submission
- [ ] Commit fastlane metadata **before** tagging — both repos read metadata from the same tag
      they take the APK from.
- [ ] Tag `v3.1.6` and attach the signed release APK to a GitHub Release.
- [ ] Request IzzyOnDroid inclusion: <https://gitlab.com/IzzyOnDroid/repo> issue tracker.
- [ ] F-Droid main repo (later): RFP at <https://gitlab.com/fdroid/rfp>, or a merge request
      adding `metadata/com.cafarovceyxun.anamuslim.yml` to <https://gitlab.com/fdroid/fdroiddata>.
      ⚠️ Two risks specific to this project: F-Droid builds on a Debian buildserver with a 100%
      FLOSS toolchain (AGP 9.3.1 is very new, and `shared/` declares iOS targets), and the runtime
      dependence on the project's Supabase backend plus `api.alfaazplus.com` may earn a
      `NonFreeNet` anti-feature label.

## GPLv3 obligations reminder

Because AnaMuslim derives from GPLv3 software, any **distributed binary** (e.g. a
published APK) must:
- remain under GPLv3,
- make the **corresponding source** available to recipients,
- preserve the existing copyright and license notices.
