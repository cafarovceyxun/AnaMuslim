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
- **Cavab sıxılması Caddy-dədir (2026-11-11):** Supabase buludunda gzip-i Cloudflare verirdi, self-host-da isə nə
  Envoy, nə PostgREST sıxır — 10-07-dən 11-11-ə qədər bütün JSON xam gedirdi (1000 hədis 4.3 MB; indi ~1 MB).
  `/opt/supabase/volumes/proxy/caddy/Caddyfile`-da əsas domen blokunda `encode gzip` var. Yalnız gzip: OkHttp
  özü yalnız onu istəyir, ehtiyat (Frankfurt) da onu verir. Şəkil/video/`.gz` Caddy-nin defolt tip siyahısında
  yoxdur. Yoxlama: `curl -sI -H 'Accept-Encoding: gzip' …/rest/v1/hadith?select=*&limit=200` → `content-encoding: gzip`.
  Caddyfile-ı sıfırdan quranda (`Caddyfile.upstream`) bu sətir itir — yenidən əlavə et.
- **Qari səsi keçidi Caddy-dədir (2026-11-12):** quranicaudio-nun CDN-i (Bunny) Azərbaycanda bəzi IP diapazonlarında
  (169.150.247.x, 138.199.37.x) ~800 bayt/s-ə qədər boğulur — `ping` normaldır, TLS 5–10 s, 64 KB 40 s-yə gəlmir;
  185.111.111.159-da isə eyni fayl 0.5 s-dir. DNS hansını versə, qari ona görə açılır və ya açılmır (pleyer xəta
  vermir, sadəcə başlamır). Əsas domen blokunda iki yol var: `/qari/download/<yol>.mp3` → `download.quranicaudio.com`,
  `/qari/mirrors/<yol>.mp3` → `mirrors.quranicaudio.com`. Yalnız GET/HEAD, yalnız `[A-Za-z0-9_/-]+\.mp3`,
  `X-Forwarded-*` yuxarı axına ötürülmür. Tətbiq tərəfi `RecitationAudioRelay.kt`-dir (`prepareAudioUrl`-da çağırılır)
  — oradakı `RELAYABLE_PATH` Caddy-dəki `path_regexp` ilə **eyni** qalmalıdır. Ölçü (Azərbaycandan): 64 KB ~0.6 s,
  13.8 MB-lıq surə 3.5 s, sha256 orijinalla eynidir. Keş yoxdur (Caddy-də modul yoxdur) — hər bayt Oracle-dan
  keçir, ayda 10 TB pulsuz trafik var. ⚠️ Server çöksə bu qarilər də susur (ehtiyat Frankfurt-da keçid yoxdur);
  uptime.yml hər iki yolu yoxlayır. Nüsxə: `Caddyfile.bak-2026-11-12-qari`.
- **Envoy-un PostgREST sağlamlıq yolu dəyişdirilib (2026-11-11):** upstream `cds.yaml`-da `rest` klasteri `/`-ə baxır,
  bu isə PostgREST-də hər dəfə OpenAPI sxemini qurur (~100 ms-lik kataloq sorğusu, 5 saniyədən bir — 35 gündə 600 min
  çağırış, `pg_stat_statements`-in birinci yeri). İndi `path: /app_releases?select=platform&limit=1` (anon oxuya
  bilir, bazaya qədər gedir). Admin portu (`:3001/ready`) işlədilə bilmir — `PGRST_ADMIN_SERVER_HOST=localhost`.
  ⚠️ `cds.yaml` konteynerə **tək fayl** kimi mount olunub: `sed -i` inode-u dəyişir və konteyner köhnə faylı görür —
  yerində yaz (`cat yeni > cds.yaml`), sonra `docker restart supabase-envoy` (2 saniyəlik fasilə; Envoy fayl
  dəyişikliyini özü götürmür). Yoxlama Envoy-un admin səhifəsindəndir (konteynerdə curl yoxdur):
  `sudo nsenter -t $(sudo docker inspect -f '{{.State.Pid}}' supabase-envoy) -n curl -s 127.0.0.1:9901/clusters | grep health_flags`.
  Nüsxə: `cds.yaml.bak-2026-11-11`.
- **Parol bərpası e-poçtla işləmir — serverdə alət var (2026-11-11):** Auth-un SMTP-si qurulmayıb (`SMTP_HOST=supabase-mail`,
  o konteyner isə yoxdur) və tətbiqdə parol bərpası ekranı da yoxdur. Parolunu unudan hesab üçün:
  `sudo anamuslim-set-password.sh <e-poçt>` — parolu iki dəfə soruşur (ekranda görünmür, əmr sətrinə/loglara düşmür),
  bcrypt heşini yazır və hesabın bütün sessiyalarını bağlayır. `DRYRUN=1` ilə heç nə yazmır. Yalnız root, yalnız
  interaktiv terminal. Əsl e-poçt lazım olsa `.env`-də `SMTP_*` dəyərləri real provayderlə doldurulmalıdır.
- **Yarımçıq yedək `synced_at`-ı dondurur (2026-11-11):** serverdə xəbərdarlıq kanalı yoxdur (SMTP qurulmayıb), uptime.yml
  isə yalnız ehtiyatdakı `backup_sync_status.synced_at`-a baxır. Əvvəl serverdən kənar yükləmə (Object Storage PAR-ı
  bitsə və s.) uğursuz olanda sinxron yenə keçirdi və heç kim xəbər tutmurdu. İndi `anamuslim-backup.sh` hər addım
  keçəndən sonra yedək qovluğuna `OK` faylı qoyur; `anamuslim-backup-sync.sh` işarə yoxdursa datanı yenə köçürür, amma
  `synced_at`-ı **yeniləmir** və xəta ilə bitir → uptime 26 saat sonra e-poçt göndərir. Hər iki yol əl ilə sınandı.
  Köhnə skriptlər: `/usr/local/bin/anamuslim-backup*.sh.bak-2026-11-11`.

### Keçid günü (yeni versiya mağazada təsdiqlənəndə)

1. **Məzmun redaktəsini dayandır** — köhnə (mağazadakı) tətbiqdə də, test tətbiqində də.
2. ⛔ **`migrate.sh resync` İŞLƏTMƏ (2026-10-16-dan etibarsızdır).** 10-07-dən sonra əsas iş Oracle-da
   gedib (`quran_wbw_az`, `quran_edits`-də audit təklifləri, yeni dua, `story_announcement`,
   `like_count`/`reason` sütunları). `resync` Oracle-ın bütün public cədvəllərini truncate edib Tokio-dan
   doldurur — yalnız Oracle-da olan cədvəllər **səssizcə boş qalır**. Onun yerinə `delta.sh` (aşağıda).
3. Mağazalarda yayımla. Android-də **100% rollout** (mərhələli olsa məcburi yenilənən istifadəçi yeni
   versiyanı tapmaya bilər); App Store-da «Release».
4. ✅ **2026-11-11-də edildi** — Oracle-da və Frankfurt ehtiyatında android `202610171`, ios `74` (5-ci addımdakı
   qeydə bax), `latest_version_name = 2026.10.17`; `min_version` də eyni. Növbəti buraxılışda Ayarlar → «Buraxılış
   Bildirişi» ilə yenilə (yalnız Oracle; ehtiyata gecəlik sinxron aparır).
   **Yeni serverdə** `app_releases`: android `latest_version = 202610171`, ios — Xcode Cloud build nömrəsi,
   `latest_version_name = 2026.10.17` (keçid buraxılışı 2026.10.07-dən 2026.10.17-yə sürüşdü).
5. ✅ **2026-11-11-də edildi** (android `min_version = latest_version = 202610171`; ios **74** — 2026.10.17-nin dəqiq
   Xcode Cloud nömrəsi repoda yoxdur, 73 isə Tokio-ya baxan son mağaza build-i idi (2026.10.05), ona görə 74 bütün
   köhnə iOS build-lərini tutur). O gün Tokio-ya 78 sorğu/gün gəlirdi (yalnız oxu), Oracle-a ~4000.
   **Köhnə (Tokio) layihədə** `app_releases.min_version`-u həmin nömrələrə qaldır → köhnə build-lər
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
