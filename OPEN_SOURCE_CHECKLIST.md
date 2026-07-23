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
- [x] In-app repo / bug-report / verse-report links point to `cafarovceyxun/AnaMuslim` (`ApiConfig.kt`)
- [x] Git remote set to `cafarovceyxun/AnaMuslim.git`
- [x] `CREDITS.md`, `PRIVACY.md`, `CODE_OF_CONDUCT.md`

## 🔜 Do soon (agreed)

### 1. Asset licensing audit — ⏳ mostly done
- [x] **KFGQPC fonts** (`inventory/fonts/kfqpc_v1/`): license permits free
      use/copy/distribution if **unmodified & not sold**. Added
      `inventory/fonts/kfqpc_v1/LICENSE.txt`; documented in [CREDITS.md](CREDITS.md).
      ✅ OK to bundle.
- [ ] Confirm the remaining `inventory/` assets — **quran_scripts, translations,
      wbw** — are redistributable (fill the "Verify" rows in CREDITS.md). Tanzil
      texts are generally fine unmodified for non-commercial use; verify each
      translation's terms.

### 1b. Slim the repo — Git LFS (`.git` ≈ 178 MB, mostly fonts)
- [x] Added LFS patterns to `.gitattributes` (`*.ttf`, `*.zip`, `*.mp3`, `*.db`, …).
      These apply to **future** commits only.
- [ ] Migrate existing history to LFS (rewrites history — do on a clean clone,
      coordinate before pushing):
  ```bash
  git lfs install
  # rewrite the whole history, moving matching blobs into LFS
  git lfs migrate import --everything \
    --include="*.ttf,*.otf,*.woff,*.woff2,*.zip,*.mp3,*.db"
  git push --force-with-lease origin master   # only after review
  ```
  Alternative (lighter repo, no LFS): remove the fonts from the repo and
  download them at runtime (the app already fetches font packs from
  `QuranAppInventory/releases`).

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
  - `api.alfaazplus.com`, `gh-proxy.alfaazplus.com` — `ApiConfig.kt`
  - `github.com/AlfaazPlus/QuranAppInventory/releases/download/qpc/` — `ScriptFontInstaller.kt`
  - `GH_PROXY_BASE_URL` / `JS_DELIVR_BASE_URL` / `GH_RAW_BASE_URL` → `AlfaazPlus/QuranApp@master`

### 4. Branding / icons — ✅ checked
- [x] App name **"Ənə Muslim"**, applicationId `com.cafarovceyxun.anamuslim`,
      funding empty — all own branding, no upstream trademark.
- [x] Launcher icons are custom (not upstream's). **Note:** two different icon
      sets exist — `ic_launcher` (red/amber calligraphy, used by the manifest)
      and `icon_launcher` (green #1D5333, used by app shortcuts in
      `ShortcutUtils.kt`). Not a blocker; **unify to one icon** for consistency.
- [ ] Confirm `peacedesign/` module is intended to be public (license, origin).

## 📋 Recommended before / around release

- [ ] **Release signing** — keep keystore OUT of the repo; store as CI secrets.
      Re-enable `signingConfig` in `app/build.gradle.kts` (currently commented at ~line 66).
- [ ] Verify app **icons / branding** are original (not upstream's trademarks).
- [ ] Add repo **description, topics, and website** on GitHub.
- [ ] Add CI for **iOS build** (currently only `assembleDebug`) once migration stabilizes.
- [ ] Optional: untrack `.vscode/settings.json`.
- [ ] Optional: `SECURITY.md` (how to report vulnerabilities).
- [ ] Tag a first release (`v…`) with source + prebuilt debug/release APK.

## GPLv3 obligations reminder

Because AnaMuslim derives from GPLv3 software, any **distributed binary** (e.g. a
published APK) must:
- remain under GPLv3,
- make the **corresponding source** available to recipients,
- preserve the existing copyright and license notices.
