# Supabase layihə köçürməsi (Tokio → Frankfurt, 2026-10-07)

`anamuslim` (`molyqwcaynvsdmixtcbc`, ap-northeast-1) → `cafarovceyxun` (`vyacxuwhtqqbythsovzt`, eu-central-1).
Səbəb: region — Azərbaycandan eyni REST sorğusu Tokioda median 0.47 s, Frankfurtda 0.22 s.
Pulsuz planda «Restore to new project» yoxdur, ona görə köçürmə `pg_dump`/`pg_restore` ilədir.

## Alətlər

`pg_dump`/`psql` **17** lazımdır (server 17-dir; köhnə `pg_dump` yeni serveri dump etmir). Maşında
Homebrew yoxdur — Postgres.app-in rəsmi buraxılışı quraşdırmadan, mount edilərək işlədilir:

```bash
curl -L -o Postgres-17.dmg https://github.com/PostgresApp/PostgresApp/releases/download/v2.9.6/Postgres-2.9.6-17.dmg
hdiutil attach -nobrowse -readonly -mountpoint "$PWD/mnt" Postgres-17.dmg
export PGBIN="$PWD/mnt/Postgres.app/Contents/Versions/17/bin"
```

## Bağlantı

`~/.anamuslim-migrate.env` (chmod 600, repoda deyil) — **yalnız xam parollar**, dırnaqsız:

```
OLD_DB_PASSWORD=...
NEW_DB_PASSWORD=...
```

`build_urls.py` onlardan düzgün kodlanmış Session pooler URI-lərini qurur, hostu yoxlayaraq tapır
(`aws-1-…`/`aws-0-…`) və `~/.anamuslim-migrate.urls`-ə yazır. Səhv parolda dərhal dayanır —
pooler çoxlu uğursuz cəhddə IP-ni bloklayır.

⚠️ **psql-in xam xəta mətnini heç vaxt çap etmə.** URI pozuq olanda xəta mesajı parolun bir
hissəsini göstərir (2026-10-07-də belə sızdı). Skriptlər bütün çıxışı maskalayır.

## Addımlar

```bash
./migrate.sh dump      # köhnədən public (sxem+data) + auth.users/identities
./migrate.sh restore   # yalnız boş bazaya; auth artıq varsa keçir
./migrate.sh verify    # 439 sətirlik barmaq izi: ACL, RLS, trigger, indeks, view, hesab, hər cədvəlin md5-i
./migrate.sh resync    # ⛔ 2026-10-16-dan İŞLƏTMƏ — Oracle-dakı yeni işi silir; keçid günü delta.sh
```

İş qovluğu `$TMPDIR/anamuslim-migrate`-dir — dump-larda parol heşləri var, repoya düşməməlidir.

## Tələlər (hamısı canlıda rast gəlindi)

- **Supabase-in defolt icazələri** (`alter default privileges … grant all … to anon`) yeni cədvəl
  və funksiyaya anon-a tam hüquq verir, `pg_dump` isə ACL-i `acldefault`-a görə yazdığı üçün
  onları **geri almır**. Diqqətsiz bərpada `backup_table_json` (bütün bazanı oxuyan `SECURITY
  DEFINER`) anon-a açılardı. `restore` defoltları bərpa vaxtı söndürüb sonra qaytarır.
- `DEFAULT ACL` TOC sətirləri `supabase_admin`-indir — `postgres` dəyişə bilmir, bərpa dayanır.
  Yenidə onsuz da eynidir, TOC-dan çıxarılır. `public` sxemi və `rls_auto_enable` də yenidə
  hazır var (ensure_rls event trigger-i).
- Yeni layihədə Supabase-in özünün yaratdığı `rls_auto_enable` anon-a `EXECUTE` verir — köhnədə
  geri alınmışdı, yenidə də alındı (`rls_auto_enable_revoke_execute`).
- Data `session_replication_role = replica` ilə yüklənir: əks halda `trg_intercept_hadith`
  hədisləri `hadith_edits`-ə yönləndirərdi və FK-lar sıra xətası verərdi.
- Vault sirri dump-la köçmür (layihənin öz açarı ilə şifrələnir) — `~/.anamuslim-backup.env`-dəki
  `BACKUP_SECRET` ilə yenidən yaradılır. psql dəyişəni `-c` sorğusunda açılmır, stdin lazımdır.
- Edge Function sirri (`QIBLA_SAT_HD_KEY`) panel tərəfindən geri göstərilmir — MapTiler-dən götür.

## Self-hosted (Oracle) — 2026-10-07

Plan dəyişdi: Frankfurt Supabase layihəsi aralıq addım oldu, son ünvan **öz serverimizdir** —
`https://anamuslim.cafarovceyxun.com` (Oracle Always Free, Frankfurt, `/opt/supabase`, rəsmi
`self-hosted/v0.8.2`). Fərqlər:

- Bərpa superuser ilə gedir: `~/.anamuslim-migrate.urls`-də `NEW_ADMIN_URL` (`supabase_admin`) varsa
  `pg_restore --role=postgres` və `resync` onu işlədir (self-host-da `postgres` `session_replication_role`
  dəyişə bilmir). `NEW_DB_URL`/`NEW_ADMIN_URL` SSH tuneli ilə `127.0.0.1:54322`-yə baxır:
  `ssh -i ~/.ssh/oracle_cafarovceyxun -f -N -L 54322:127.0.0.1:54322 ubuntu@130.61.171.105`.
- `rls_auto_enable` + `ensure_rls` yenidə yox idi — funksiya TOC-dan çıxarılmadı, event trigger **postgres**
  ilə yaradıldı (supabase_admin ilə alınmır: superuser trigger superuser funksiyası tələb edir).
- `resync` məşq edildi: ~2 dəq 44 s, sonra `verify` 439/439 eyni. Hesab sessiyalarını sıfırlayır.

### Keçid günü (yeni versiya mağazada təsdiqlənəndə)

1. **Məzmun redaktəsini dayandır** — köhnə (mağazadakı) tətbiqdə də, test tətbiqində də.
2. ⛔ **`migrate.sh resync` İŞLƏTMƏ (2026-10-16-dan etibarsızdır).** 10-07-dən sonra əsas iş Oracle-da
   gedib (`quran_wbw_az`, `quran_edits`-də audit təklifləri, yeni dua, `story_announcement`,
   `like_count`/`reason` sütunları). `resync` Oracle-ın bütün public cədvəllərini truncate edib Tokio-dan
   doldurur — yalnız Oracle-da olan cədvəllər **səssizcə boş qalır**. Onun yerinə `delta.sh` (aşağıda).
3. Mağazalarda yayımla. Android-də **100% rollout** (mərhələli olsa məcburi yenilənən istifadəçi yeni
   versiyanı tapmaya bilər); App Store-da «Release».
4. **Yeni serverdə** `app_releases`: android `latest_version = 202610171`, ios — Xcode Cloud build nömrəsi,
   `latest_version_name = 2026.10.17` (keçid buraxılışı 2026.10.07-dən 2026.10.17-yə sürüşdü).
5. **Köhnə (Tokio) layihədə** `app_releases.min_version`-u həmin nömrələrə qaldır → köhnə build-lər
   `ForceUpdateGate`/`check4CriticalUpdate` ilə yeniləməyə məcbur olur və Tokio-ya yazmağı dayandırır.
   Bunu mağazada yeni versiya **görünəndən sonra** et.
6. **Delta** — Tokio-da keçiddən sonra yaranan datanı Oracle-a daşı:
   ```bash
   ./delta.sh plan    # heç nə yazmır: hesabat + media siyahısı
   ./delta.sh apply   # «bəli» soruşur → serverdə yedək → media → COMMIT
   ```
   Əvvəl `~/.anamuslim-migrate.env`-də `OLD_DB_PASSWORD`-u Tokio-nun **cari** parolu ilə yenilə (fayldakı
   10-07-dən əvvəlkidir — səhv parolla təkrar cəhd pooler-də IP-ni bloklayır, ona görə `plan` bir dəfə
   xəta versə parolu yoxla, ard-arda işlətmə). Mac-də psql lazım deyil. Nə daşınır (qaydalar
   `delta_merge.sql`-dədir; iki tərəfdə dəyişən sətrə **toxunulmur**, «münaqişə» kimi göstərilir):
   - **hilal elanları** — Tokio həlledicidir; şəkillər `lunar-media`-ya köçür, URL hostu dəyişir;
     Tokio-da silinmiş köhnə elan Oracle-dan da silinir (2026-10-16: 4 və 6 gəlir, 3 gedir);
   - **tərcümə mətni** — yalnız Oracle-da keçiddən sonra toxunulmamış ayələr (2026-10-16: 2:36, 2:196);
     sözbəsöz trigger-i onları `stale` edir; həmin ayədə gözləyən audit təklifi varsa xəbərdarlıq çıxır;
   - **düzəliş təklifləri** (`quran_edits`) — yeni id ilə (Tokio id-ləri audit təklifləri ilə toqquşur);
   - **istək lövhəsi** — səs/baxış sayı ikisinin böyüyü; yeni istək/müraciət/şikayət/log sətirləri;
   - qalan məzmun cədvəlləri, hesablar və `app_releases` — **yalnız hesabat** (Tokio-da dəyişən varsa
     «DİQQƏT» yazır, əl ilə həll et).
   Təkrar işlətmək təhlükəsizdir — ikinci `apply` heç nə dəyişmir. Sınaq (2026-10-16): Frankfurt
   nüsxəsi və təqlid olunmuş Tokio fərqləri ilə `plan` bütün yolları keçdi, Oracle toxunulmaz qaldı.
   Real Tokio ilə hələ işlədilməyib — ilk dəfə keçid günü `plan`-la.
7. Mac yedək düyməsi: `~/.anamuslim-backup.env`-də `SUPABASE_URL=https://anamuslim.cafarovceyxun.com`
   (`BACKUP_SECRET` eynidir — Vault-a eyni dəyər yazılıb). Telefon yedəyi admin sessiyası ilə avtomatik keçir.
8. 1–2 həftə sonra: yeniləməmiş istifadəçilərdən Tokio-ya düşənlər üçün `./delta.sh plan` → `apply`
   bir daha, sonra Tokio layihəsini bağla (Frankfurt ehtiyat kimi qalır — aşağıya bax).

## Ehtiyat (2026-10-07)

Frankfurt layihəsi bağlanmır — **ehtiyat** kimi qalır (cədvəllər + Edge Function-lar, gecəlik köçürmə). Ətraflı:
`docs/supabase/SCHEMA.md` → «Ehtiyat server». Keçid günündəki 8-ci addımda yalnız **Tokio** bağlanır.
