# Supabase sxemi — 2026-08-30 (son miqrasiyadan sonra)

`public` sxemindəki hər şey: cədvəllər, sütunlar, məhdudiyyətlər, indekslər, RLS, trigger-lər,
funksiyalar və icazələr.

**Mənbə:** 2026-07-27 tarixli xam dump + ondan sonra işlədilən miqrasiyalar. Nəticə iki dəst
yoxlama ilə təsdiqlənib: 22 struktur yoxlaması (RLS, trigger, funksiya, siyasət, indeks, grant) və
moderasiya axınının 9 davranış yoxlaması — hamısı **OK**. Sxem dəyişəndə bu faylı yeniləyin.

**Son dəyişiklik: 2026-08-30** — `daily_content` növbəyə çevrildi (`daily_content_item` + gündə 5
yuva); köhnə ad uyğunluq view-u kimi qaldı, `reschedule_daily_content()` RPC-si əlavə olundu.
Təfərrüat aşağıda, «Miqrasiyalar» və «`daily_content` VIEW» bölmələrində.

**Son tam sinxronlaşdırma: 2026-08-01** — canlı baza ilə tutuşdurulub (Supabase MCP, read-only).
Struktur tam uyğun çıxdı: 14 cədvəl + `translations` view, 14 PK, 6 FK, 8 CHECK, 22 indeks,
7 trigger, 40 RLS siyasəti, 8 funksiyanın hamısında `search_path` sabitlənib. RLS-i açıq olub
siyasətsiz cədvəl yoxdur, RLS-i bağlı cədvəl yoxdur, `anon`-un SELECT/INSERT-dən artıq icazəsi
yoxdur, köhnə `hadith_data` sxem qalığı yoxdur, `translations` view-da `security_invoker`
qoşulmayıb (aşağıdakı qeyd düzdür). Yenilənən: sətir sayları və funksiya siyahısı (aşağıda).

Aşağıda `admin` yazılan hər yerdə siyasətin gövdəsində konkret e-poçt ünvanı durur. **Ünvanın özü bu
repoda saxlanmır** (2026-08-11-də çıxarıldı) — yeni siyasət və ya trigger yazarkən lazım olsa bazadan
oxu:

```sql
select polname, pg_get_expr(polqual, polrelid) from pg_policy where polname like '%admin%';
```

## Miqrasiyalar

Hamısı 2026-07-27-də tətbiq olunub; skript faylları sonra silinib — bu sənəd yekun vəziyyətin
yeganə qeydidir.

| Miqrasiya | Nə etdi |
|---|---|
| `rls_hardening` | anon-un yazma deşikləri bağlandı, artıq table-level grant-lar geri alındı |
| `hadith_name_ar` | hədis struktur cədvəllərinə `name_ar` sütunları |
| `verse_reports` | ayə bildirişləri cədvəli, CHECK-lər və indekslər |
| `hadith_edits_approval` | hədis moderasiyası: `hadith_edits` üçün RLS siyasətləri, sınıq `process_hadith_approval()` götürüldü, təsdiqdə bütün sahələr köçür, yeni hədis təklifi üçün insert yolu, `status` NOT NULL |
| `edits_hardening` | Quran moderasiyası (təkrar trigger, `coalesce`, admin-only təsdiq), `quran_edits.verse_no`, `hadith` DELETE admin-only, `quran_translations_data` yazma admin-only + unikal `id` indeksi, `translations` view grant-ları, idarəetmə cədvəlləri, 17 ölü funksiya silindi |
| funksiya gigiyenası | `reject_hadith_from_edits` silindi, 6 canlı funksiyada `search_path` sabitləndi, trigger funksiyalarından `EXECUTE` geri alındı (Supabase linter tapıntıları) |
| `app_releases` (2026-07-31) | tətbiq buraxılış bildirişi cədvəli: platforma başına bir sətir, public read / admin write, `updated_at` trigger-i |
| `suggestions_feature_image` (2026-08-30) | `suggestions.image_url` + public `suggestion-images` bucket (oxu hamıya, yazma admin) — «əlavə olunan funksiya buradadır» ekran görüntüsü |
| `suggestions_note_and_views` (2026-08-30) | `suggestions.note` (hekayədə görünən admin qeydi, ≤300) + `view_count` və `mark_suggestion_viewed()` RPC-si |
| `suggestions_media_list` (2026-08-30) | tək `image_url` → `media jsonb` massivi (`[{"url","type"}]`, `type ∈ image|video`), mövcud şəkillər köçürüldü; bucket 50 MB + `video/mp4`, `video/quicktime` |
| `suggestions` (2026-08-30) | istifadəçi təklifləri: `suggestion_submissions` (növbə) + `suggestions` (təsdiqlənmiş, ictimai), təsdiq trigger-i, 3 anonim RPC. **Kimlik saxlanmır** — aşağıya bax |
| `daily_content_queue_slots` (2026-08-30) | günün ayəsi **növbəsi**: `daily_content` → `daily_content_item`, gündə 5 yuva (`slot_index`), ayə aralığı (`verse_end`), hədis çıxarışı (`excerpt_ar`/`excerpt_az`), `(date, slot_index)` **DEFERRABLE** unikal; köhnə ad slot 0-ı göstərən **view** kimi qaldı |
| `daily_content_reschedule_rpc` (2026-08-30) | `reschedule_daily_content(jsonb)` — növbənin yerdəyişməsi bir ifadə ilə (upsert `GENERATED ALWAYS` id-yə görə yaramır) |
| `daily_content_view_count` (2026-08-30) | `daily_content_item.view_count` + `increment_daily_content_view(bigint)` RPC — hekayənin baxış sayı (kimlik saxlanmır) |

---

## Cədvəllər

| Cədvəl | Sətir | Qeyd |
|---|---|---|
| `app_logs` | 0 | çökmə/loq qeydləri; `anon` yalnız INSERT, oxu/silmə admin |
| `app_releases` | 2 | ana ekrandakı yeniləmə banneri; platforma başına bir sətir, yazma admin, oxu hamıya |
| `daily_content_item` | 22 | **günün ayəsi/hədisi növbəsi**; gündə 5 yuva, yazma admin, oxu hamıya |
| `daily_content` | — | **VIEW** — `daily_content_item`-in `slot_index = 0` sətirləri (köhnə tətbiq buraxılışları üçün) |
| `hadith` | 289 | **əsas hədis cədvəli** (əvvəllər `hadith_data` — PK və sequence hələ o adı daşıyır) |
| `hadith_book` | 3 | kitab |
| `hadith_chapter` | 99 | bab |
| `hadith_edits` | 0 | redaktor təklifləri (moderasiya) |
| `hadith_sub_chapter` | 61 | alt-bab |
| `hadith_volume` | 2 | cild |
| `quran_edits` | 0 | tərcümə təklifləri — **2026-08-01-də boşdur** (07-27-dəki 34 gözləyən təklifi admin özü emal edib) |
| `quran_translations_data` | 6236 | **əsas tərcümə cədvəli** |
| `resource_updates` | 1 | klient üçün versiya sayğacı (public read) |
| `resource_updates_admin` | 1 | admin yazır, trigger `resource_updates`-ə köçürür |
| `suggestion_submissions` | 0 | **istifadəçi təklifləri, moderasiya növbəsi** — yalnız admin oxuyur; yazma yalnız `submit_suggestion()` RPC-si ilə |
| `suggestions` | 0 | təsdiqlənmiş təkliflər, hamıya görünür; `vote_count` sayğacı |
| `verse_reports` | 0 | ayə səhv bildirişləri |
| `translations` | — | **VIEW** (`quran_translations_data` üzərində, aşağıda) |

RLS bütün 16 cədvəldə **aktivdir** və hamısının ən azı bir siyasəti var.

### Sütunlar

```
app_logs                id bigint NN · type text NN · stack_trace text NN · device_info text NN
                        app_version text NN · place text · created_at timestamptz NN = now()

app_releases            platform text NN (PK) ∈ (android, ios) · latest_version bigint NN = 0
                        latest_version_name text · min_version bigint NN = 0 · action_url text
                        release_notes jsonb NN = '{}' · updated_at timestamptz NN = now()
                        release_notes formatı: {"az": ["sətir", …], "en": [...]} — dil kodu → sətirlər

daily_content_item      id bigint NN (identity, GENERATED ALWAYS) · content_type text NN
                        chapter_no int · verse_no int · verse_end int · hadith_id bigint
                        text_ar text NN · text_az text NN · excerpt_ar text · excerpt_az text
                        source text · date date NN = CURRENT_DATE · slot_index int NN = 0
                        view_count int NN = 0 · created_at timestamptz = now() · created_by uuid
                        ℹ️ `view_count` → hekayəyə baxış sayı; yalnız `increment_daily_content_view()`
                           artırır. **Səs cədvəli kimi baxış cədvəli yoxdur** — «bu cihaz saydımı»
                           sualının cavabı cihazdadır (`VersePreferences`), baza kimin baxdığını bilmir.
                        ℹ️ `verse_no`..`verse_end` → **çoxayəli** element (aralıq bir gündə bir yuva tutur)
                        ℹ️ `excerpt_*` → hədisin yalnız göstəriləcək hissəsi; null olanda tam mətn
                        ℹ️ `slot_index` 0..4 → günün beş bildiriş yuvası, saatlar tətbiqdədir
                           (`DailyContentSchedule.SLOT_TIMES`: 08:00 / 12:00 / 15:00 / 18:00 / 21:00)
                        ⚠️ `id` **GENERATED ALWAYS**-dır: açıq id ilə upsert mümkün deyil, ona görə
                           yerdəyişmə `reschedule_daily_content()` RPC-sindən keçir

hadith                  id bigint NN = nextval('hadith_data_id_seq') · chapter_slug text
                        sub_chapter_slug text · hadith_no int · text_ar text · text_az text
                        source text · note text · updated_at timestamptz = now() · chapter_no int

hadith_volume           slug text NN (PK) · name text NN · author text · updated_at timestamptz = now()
                        name_ar text · description text
hadith_book             slug text NN (PK) · volume_slug text · book_no int NN · name text NN
                        updated_at timestamptz = now() · name_ar text
hadith_chapter          slug text NN (PK) · book_slug text · chapter_no int NN · name text NN
                        updated_at timestamptz = now() · name_ar text
hadith_sub_chapter      slug text NN (PK) · chapter_slug text · sub_chapter_no int NN · name text NN
                        updated_at timestamptz = now() · name_ar text

hadith_edits            id bigint NN = nextval('hadith_edits_id_seq') · hadith_id bigint
                        chapter_slug text · sub_chapter_slug text · hadith_no int
                        text_ar text · text_az text · source text · note text
                        editor_email text NN · status text NN = 'pending' · created_at timestamptz = now()
                        chapter_no int · user_id uuid · is_delete bool NN = false
                        ℹ️ `updated_at` yoxdur (tətbiq də gözləmir)
                        ℹ️ `is_delete = true` → sətir düzəliş yox, **silmə tələbidir**; təsdiqdə
                           `hadith` sətri silinir, mətn sahələri yalnız paneldə göstərmək üçündür

quran_edits             id bigint NN · translation_id bigint · new_text text NN · editor_email text NN
                        is_approved bool = false · created_at timestamptz = now()
                        user_id uuid = auth.uid() · chapter_no bigint · note text · verse_no bigint

quran_translations_data id bigint NN · chapter_no bigint NN · verse_no bigint NN · slug text NN
                        text text NN · updated_at text NN = now() · note text

resource_updates        id int NN = 1 · version int = 0 · updated_at timestamptz = now()
resource_updates_admin  id int NN = 1 · version int = 0 · updated_at timestamptz = now()

suggestion_submissions  id bigint NN (identity) · ticket uuid NN = gen_random_uuid() (UNIQUE)
                        body text NN · category text NN = 'other' · app_version text · platform text
                        status text NN = 'pending' · admin_note text · suggestion_id bigint
                        created_at timestamptz NN = now() · updated_at timestamptz NN = now()
                        ⚠️ **Qəsdən `device_id`/`user_id` YOXDUR.** Göndərənin yeganə izi `ticket`-dir
                           və o yalnız cihazda saxlanılır (`SuggestionLocalStore`) — baza kimin nə
                           göndərdiyini bilmir. `id` ardıcıl olduğu üçün status sorğusu `ticket`
                           üzərindən gedir, `id` ilə növbəni açmaq mümkün deyil.

suggestions             id bigint NN (identity) · body text NN · category text NN = 'other'
                        status text NN = 'open' · vote_count int NN = 0 · view_count int NN = 0
                        media jsonb NN = '[]' · note text · source_submission_id bigint
                        created_at timestamptz NN = now() · updated_at timestamptz NN = now()
                        ℹ️ `media` = `[{"url": "...", "type": "image"|"video"}]`, sıra hekayədəki sıradır.
                           Linklər `suggestion-images` bucket-indəndir. CHECK: massiv olmalıdır.
                           Yalnız admin yazır; istifadəçi təklifinə media qoşma yolu qəsdən yoxdur.
                        ℹ️ `note` hekayədə mətnin üstündə görünən **ictimai** admin qeydidir —
                           `suggestion_submissions.admin_note` isə göndərənə cavabdır, ictimai deyil.
                        ℹ️ `view_count` təxminidir: klient hekayəni **ilk dəfə** açanda
                           `mark_suggestion_viewed()` çağırır, baxılma vəziyyəti isə cihazdadır.
                        ℹ️ `vote_count`-u yalnız `vote_suggestion()` RPC-si dəyişir; **səs cədvəli
                           yoxdur** — «bu cihaz səs veribmi» sualının cavabı cihazdadır.

verse_reports           id bigint NN · chapter_no int NN · verse_no int NN · verse_key text
                        message text NN · slugs text · app_version text · status text NN = 'pending'
                        admin_note text · user_id uuid · created_at timestamptz NN · updated_at timestamptz NN
```

### Məhdudiyyətlər

- PK: `app_logs(id)`, `app_releases(platform)`, `daily_content_item(id)`, `hadith(id)` (`hadith_data_pkey`), `hadith_volume(slug)`,
  `hadith_book(slug)`, `hadith_chapter(slug)`, `hadith_sub_chapter(slug)`, `hadith_edits(id)`,
  `quran_edits(id)`, `verse_reports(id)`, `resource_updates(id)`, `resource_updates_admin(id)`,
  `quran_translations_data(id, chapter_no, verse_no, slug, text, updated_at)` ← qəribə geniş PK;
  `id`-nin unikallığını **ayrıca** `quran_translations_data_id_key` indeksi təmin edir
- FK: `hadith_book.volume_slug → hadith_volume.slug` (CASCADE), `hadith_chapter.book_slug → hadith_book.slug`
  (CASCADE), `hadith_sub_chapter.chapter_slug → hadith_chapter.slug` (CASCADE),
  `daily_content_item.created_by → auth.users.id`, `quran_edits.user_id → auth.users.id`,
  `verse_reports.user_id → auth.users.id` (SET NULL)
  ⚠️ **`hadith.chapter_slug` / `hadith.sub_chapter_slug` xarici açar DEYİL.** Struktur silinəndə baza
  heç nə etmir: hədislər mövcud olmayan slug-a işləyən yetim sətirlər kimi qalır — UI-də görünmür,
  cədvəldə və axtarışda qalır. Ona görə tətbiq struktur silməzdən əvvəl içindəkiləri sayır və boş
  deyilsə imtina edir (`HadithViewModel.deleteStructure` → `DeleteOutcome.NotEmpty`). Bunu bazada
  CASCADE ilə həll etməmişik: yanlış slug bir dəfə yazılsa CASCADE səssizcə məzmun uçurardı.
- CHECK: `hadith_edits.status ∈ (pending, approved, rejected)` **və NOT NULL**,
  `verse_reports.status ∈ (pending, reviewing, resolved, rejected)`,
  `verse_reports` mesaj uzunluğu 3–2000, `daily_content_item.content_type ∈ (verse, hadith)`,
  `daily_content_item.slot_index` 0..4, `daily_content_item.verse_end` null ya da `>= verse_no`,
  `daily_content_item.view_count >= 0`,
  `app_releases.platform ∈ (android, ios)`, `app_releases.latest_version >= 0`,
  `app_releases.min_version >= 0`, `app_releases.release_notes` **jsonb obyekt** olmalıdır
- Şərti unikal indekslər (bir redaktora bir gözləyən təklif):
  `only_one_pending_per_editor` on `hadith_edits(hadith_id, editor_email) where status='pending'`
  `quran_only_one_pending_per_editor` on `quran_edits(translation_id, editor_email) where is_approved=false`
- Təkliflər: `suggestion_submissions.status ∈ (pending, approved, rejected)`,
  `suggestions.status ∈ (open, planned, done)`, hər iki cədvəldə `category ∈ (feature, bug, content, other)`
  və gövdə uzunluğu 5–1000, `suggestion_submissions.platform ∈ (android, ios)` və ya null,
  `suggestions.vote_count >= 0`. FK-lar **qarşılıqlıdır** və hər ikisi `on delete set null`:
  `suggestion_submissions.suggestion_id → suggestions.id`, `suggestions.source_submission_id →
  suggestion_submissions.id` — yəni birini silmək o birini uçurmur, sadəcə bağı qırır.
- Təkliflərin indeksləri: `suggestion_submissions(ticket)` **unikal** (status sorğusunun yeganə açarı),
  `suggestion_submissions(status)`, `suggestion_submissions(created_at desc)`,
  `suggestions(vote_count desc, created_at desc)`
- Digər indekslər: `quran_translations_data(id)` **unikal**, `hadith_edits(status)`,
  `hadith_edits(created_at desc)`, `verse_reports(created_at desc)`, `verse_reports(status)`
- ⚠️ `daily_content_item_date_slot_key` — `(date, slot_index)` **UNIQUE DEFERRABLE INITIALLY
  DEFERRED**. Təxirə salınmadan növbədə iki elementin yerini dəyişmək mümkün olmazdı (aralıq
  vəziyyətdə hər iki sətir eyni yuvada olur). Əvəzi: **təxirə salınmış məhdudiyyət `on conflict`
  arbitri ola bilmir** — buna görə də yerdəyişmə upsert ilə yox, `reschedule_daily_content()` ilə
  edilir. Köhnə `idx_daily_content_date` (gündə bir sətir) silinib.

---

## `translations` VIEW

`quran_translations_data` üzərində view — redaktora **öz təsdiqlənməmiş** düzəlişini göstərir:

```sql
select id,
       coalesce((select qe.new_text from quran_edits qe
                  where qe.translation_id = qt.id
                    and qe.editor_email = auth.jwt() ->> 'email'
                    and qe.is_approved = false
                  order by qe.created_at desc limit 1), text) as text,
       chapter_no, verse_no, slug,
       coalesce((... eyni məntiqlə qe.note ...), note) as note,
       updated_at
  from quran_translations_data qt;
```

Tətbiq tərcüməni bu view üzərindən yazır; `instead of` trigger düzəlişi `quran_edits`-ə salır.
View sahibin hüquqları ilə işləyir, ona görə icazələri dar saxlanılır (aşağıda).

---

## `daily_content` VIEW (uyğunluq)

```sql
select id, content_type, chapter_no, verse_no, hadith_id,
       coalesce(excerpt_ar, text_ar) as text_ar,
       coalesce(excerpt_az, text_az) as text_az,
       source, date, created_at, created_by
  from public.daily_content_item
 where slot_index = 0;
```

**Niyə var:** mağazadakı **yenilənməmiş** Android/iOS buraxılışları `daily_content`-i
`decodeSingleOrNull` ilə oxuyur — gündə bir sətir gözləyir. Növbə gündə beş yuvaya keçəndə cədvəlin
özü bu vədi poza bilməzdi, ona görə cədvəl `daily_content_item` adına keçdi və köhnə ad slot 0-ı
göstərən view kimi qaldı. `coalesce` sayəsində köhnə build hədisin **çıxarışını** da alır.

⚠️ **Yalnız oxunur.** Köhnə admin buraxılışı buraya `upsert ... on_conflict=date` göndərirdi; view
üzərində `on conflict` işləmir, ona görə köhnə build-dən günün ayəsini təyin etmək **artıq
mümkün deyil** — yeni build panel vasitəsilə edir. Bu qəsdəndir: admin bir nəfərdir və yenilənir,
istifadəçilər isə yalnız oxuyur.

Yeni kod bu view-a **toxunmur**; hər şey `daily_content_item` üzərindəndir
(`DailyContentRepository`).

---

## Trigger-lər (canlı — hamısı yoxlanılıb)

| Cədvəl | Trigger | Funksiya | Rolu |
|---|---|---|---|
| `hadith` | `trg_intercept_hadith` | `intercept_hadith_before_upsert()` | admin → birbaşa yazır; redaktor → `hadith_edits`-ə yönəldilir (`source`, `hadith_no`, slug-lar daxil) |
| `hadith` | `trg_intercept_hadith_delete` | `intercept_hadith_before_delete()` | **silmənin eyni məntiqi:** admin → sətir silinir; redaktor → `hadith_edits`-ə `is_delete = true` düşür, `return null` ilə silmə ləğv olunur |
| `hadith_edits` | `on_hadith_edit_approved` | `apply_hadith_approved_edit()` | `status` → `approved` olanda bütün sahələri `hadith`-ə köçürür; `hadith_id is null`-dursa yeni hədis yaradıb təklifi ona bağlayır |
| `hadith_edits` | `zz_on_hadith_delete_approved` | `apply_hadith_approved_delete()` | `when (status = 'approved' and is_delete)` → `hadith` sətrini silir. Adı `zz_` ilə başlayır ki, AFTER trigger-lərin əlifba sırasında yuxarıdakı köçürmədən **sonra** işləsin |
| `quran_edits` | `on_quran_edit_approved` | `apply_quran_approved_edit()` | `is_approved` → true olanda mətn/qeyd/`chapter_no`-nu (coalesce ilə) əsas cədvələ köçürür |
| `translations` (view) | `check_quran_before_update` | `intercept_quran_update()` | düzəlişi `quran_edits`-ə salır (`verse_no` daxil), giriş yoxdursa aydın xəta verir |
| `resource_updates_admin` | `trigger_sync_resource_updates` | `sync_resource_updates_func()` | admin versiyasını public sayğaca köçürür |
| `verse_reports` | `verse_reports_set_updated_at` | `set_verse_reports_updated_at()` | `updated_at` |
| `suggestion_submissions` | `on_suggestion_approved` | `publish_approved_suggestion()` | `status` → `approved` olanda sətri `suggestions`-a köçürür və `suggestion_id`-ni geri yazır. Təsdiq geri alınanda (`approved` → başqa status) yayımlanan sətir **silinir** — qəsdən: rədd edilmiş təklif ictimai siyahıda qalmamalıdır |
| `suggestions` / `suggestion_submissions` | `*_set_updated_at` | `set_suggestions_updated_at()` | `updated_at` (INVOKER, iki cədvəl bir funksiyanı bölüşür) |
| `app_releases` | `app_releases_set_updated_at` | `set_app_releases_updated_at()` | `updated_at` — klient sətri açıq `null` ilə göndərir, BEFORE trigger NOT NULL yoxlamasından əvvəl doldurur |

Trigger-lər `status` / `is_approved` sütunlarına bağlanıb (`after update of ...`), ona görə təsdiq
daxilindəki köməkçi yeniləmələr onları yenidən işə salmır — rekursiya riski yoxdur.

### Moderasiya axını

1. Redaktor tətbiqdə hədisi/tərcüməni yadda saxlayır → `hadith` cədvəlinə, tərcümə isə `translations`
   view-una yazılır.
2. Trigger işə düşür — **iki yol fərqlidir:**
   - **Hədis** (`intercept_hadith_before_upsert()`): e-poçta baxır. **admin** → birbaşa `hadith`
     cədvəlinə yazır; **redaktor** → sətir `hadith_edits`-ə `pending` kimi düşür.
   - **Quran** (`intercept_quran_update()`): **admin qolu YOXDUR.** Girişi olan hər kəsin düzəlişi —
     admin-inki də daxil — `quran_edits`-ə `is_approved=false` kimi düşür və funksiya `return old`
     etdiyi üçün `quran_translations_data`-ya **heç vaxt birbaşa yazılmır** (girişsizə aydın xəta).
     Yəni admin hesabı ilə tərcümə redaktəsi produksiya məzmununu dəyişmir — hədis redaktəsi dəyişir
     (2026-08-01, 64-cü dalğada runtime-da təsdiqləndi).
3. Admin paneldə (Ayarlar → Düzəlişləri İdarə Et) `status`-u `approved` / `is_approved`-i true edir.
4. Təsdiq trigger-i mətni əsas cədvələ köçürür (yeni hədis təklifi olarsa sətri yaradır).

---

## Funksiyalar

**Canlı (7 funksiya, hamısı yuxarıdakı trigger-lərə bağlıdır):**

| Funksiya | `SECURITY DEFINER`? |
|---|---|
| `apply_hadith_approved_edit` | ✅ |
| `apply_hadith_approved_delete` | ✅ |
| `intercept_hadith_before_delete` | ✅ |
| `apply_quran_approved_edit` | ✅ |
| `intercept_hadith_before_upsert` | ✅ |
| `intercept_quran_update` | ✅ |
| `sync_resource_updates_func` | ✅ |
| `publish_approved_suggestion` | ✅ |
| `submit_suggestion` | ✅ (RPC) |
| `get_suggestion_tickets` | ✅ (RPC) |
| `vote_suggestion` | ✅ (RPC) |
| `mark_suggestion_viewed` | ✅ (RPC) |
| `reschedule_daily_content` | ❌ `INVOKER` (RPC, `authenticated`) |
| `increment_daily_content_view` | ✅ (RPC, `anon`+`authenticated`) |
| `set_suggestions_updated_at` | ❌ `INVOKER` |
| `set_verse_reports_updated_at` | ❌ `INVOKER` |
| `set_app_releases_updated_at` | ❌ `INVOKER` |

`reschedule_daily_content(items jsonb)` növbənin `(date, slot_index)` yerlərini **bir** `update`
ifadəsi ilə yazır və dəyişən sətir sayını qaytarır. `SECURITY INVOKER`-dir: RLS qüvvədədir, yəni
admin olmayan çağırış xəta yox, **0** alır — klient bu sayı yoxlayır. Bir ifadə olması vacibdir:
`(date, slot_index)` unikallığı təxirə salınıb, aralıq toqquşma yalnız eyni tranzaksiyada bağışlanır.

`increment_daily_content_view(p_id)` `SECURITY DEFINER`-dir, çünki sayğacı artırmaq cədvələ yazmaq
deməkdir, yazma isə admin-onlydır. Funksiya bir çağırışda yalnız **+1** edir və başqa heç nəyə
toxunmur; qalan risk `vote_suggestion` ilə eyni sinifdəndir (spam/xərc, məlumat sızması yox).

`SECURITY DEFINER` olanlar RLS-i keçib əsas cədvələ yaza bilsin deyə belədir. İki `updated_at`
trigger-i **qəsdən `INVOKER`-dir** — onlar yalnız yazılmaqda olan sətrin öz sütununu doldurur,
RLS-i keçməyə ehtiyacları yoxdur. Yeni `updated_at` trigger-i yazanda bu nümunəni təkrarla:
lazımsız `SECURITY DEFINER` vermə.

**`rls_auto_enable`** — Supabase-in **event trigger**-i (yeni cədvəldə RLS-i avtomatik açır). Ona toxunmayın.

### Funksiya gigiyenası (yeni funksiya yazanda təkrarla)

- **`search_path` sabitlənir:** `alter function public.f() set search_path = public, pg_temp;`
  (`pg_temp` sonda). `SECURITY DEFINER` funksiyada bunsuz obyekt-kölgələmə riski qalır.
- **Trigger funksiyalarında `EXECUTE` geri alınır:**
  `revoke execute on function public.f() from public, anon, authenticated;`
  Əks halda PostgREST onları `/rest/v1/rpc/f` kimi açır. Trigger-in işləməsinə təsir etmir —
  `EXECUTE` icazəsi yalnız `create trigger` anında yoxlanılır, trigger atəşlənəndə yox.

**Silinib** (köhnə `hadith_data` / `new_text_ar` / `editor_id` / shadow-id sxeminə istinad edirdilər):
`approve_hadith_edit`, `approve_hadith_from_edits`, **`reject_hadith_from_edits`**,
`check_editor_shadow_exists`, `handle_hadith_after_insert_to_edits`, `handle_hadith_upsert_to_edits`,
`handle_hadith_view_upsert`, `handle_quran_translations_upsert`, `intercept_hadith_update`,
`intercept_hadith_upsert`, `process_hadith_real_upsert`, `secure_hadith_real_table_upsert`,
`secure_hadith_upsert_process`, `is_admin`, `log_activity` (×2), `process_hadith_approval`,
`process_quran_approval`, `process_quran_real_upsert`.

> `reject_hadith_from_edits` digərlərindən sonra tapıldı (Supabase linter-i ilə): `SECURITY DEFINER`
> idi, `anon`-a RPC kimi açıq idi və gövdəsində `delete from hadith` var idi. `hadith.status` sütunu
> olmadığı üçün çağırılanda çökürdü, yəni istismar olunmurdu — amma o sütun gələcəkdə əlavə edilsəydi
> anonim silmə yoluna çevrilirdi.

---

## RLS siyasətləri

```
hadith                  SELECT anon,authenticated: true
                        INSERT authenticated: true          ← trigger admini ayırd edir
                        UPDATE authenticated: true          ← eyni trigger
                        DELETE authenticated: true          ← `hadith_delete_via_trigger`, eyni məntiq
                        ⚠️ köhnə admin-only DELETE siyasəti də qalıb (siyasətlər OR-lanır, ona görə
                           təsirsizdir) — adını tapıb silmək olar
hadith_volume/book/     SELECT anon,authenticated: true
chapter/sub_chapter     INSERT/UPDATE authenticated: email = admin
                        DELETE authenticated: email = admin  ← `hadith_*_delete_admin`, növbə yoxdur
hadith_edits            SELECT authenticated: admin OR editor_email = jwt email
                        INSERT authenticated: admin OR editor_email = jwt email
                        UPDATE authenticated: email = admin          ← təsdiq/rədd
                        DELETE authenticated: email = admin
quran_edits             SELECT authenticated: admin OR user_id = auth.uid()
                        INSERT authenticated: admin OR (editor_email = jwt email AND user_id = auth.uid())
                        UPDATE authenticated: email = admin          ← təsdiq
                        DELETE authenticated: admin OR (user_id = auth.uid() AND is_approved = false)
quran_translations_data SELECT anon,authenticated: true
                        INSERT/UPDATE authenticated: email = admin   (DELETE siyasəti yoxdur)
daily_content           SELECT public: true · ALL authenticated: email = admin
app_logs                INSERT public: true · SELECT/DELETE authenticated: email = admin
app_releases            SELECT public: true · ALL authenticated: email = admin
verse_reports           INSERT anon,authenticated: status='pending' and admin_note is null
                        SELECT/UPDATE authenticated: true            ← redaktorlar da baxa/statusu dəyişə bilər
                        DELETE authenticated: email = admin
resource_updates        SELECT public: true
resource_updates_admin  ALL authenticated: email = admin
suggestions             SELECT anon,authenticated: true
                        ALL authenticated: email = admin        ← yazma yalnız admin
suggestion_submissions  SELECT/UPDATE/DELETE authenticated: email = admin
                        ⚠️ INSERT siyasəti **yoxdur** və olmamalıdır: anon-un cədvəl üzərində heç bir
                           icazəsi yoxdur, yazmanın yeganə yolu `submit_suggestion()` RPC-sidir
```

## İcazələr (`grant`)

- `anon`: məzmun cədvəllərində (`hadith`, `hadith_*`, `quran_translations_data`, `daily_content_item`,
  `daily_content` view-u,
  `resource_updates`, `app_releases`) yalnız `SELECT`; `app_logs`, `verse_reports` üzərində yalnız
  `INSERT`.
- `anon`-un `quran_edits` / `hadith_edits` / `resource_updates_admin`-ə heç bir icazəsi yoxdur.
- Storage `suggestion-images` bucket: `public = true` (oxu hamıya), `storage.objects` üzərində
  INSERT/UPDATE/DELETE **yalnız admin**. Limit 50 MB; `image/png|jpeg|webp` + `video/mp4|quicktime`.
  ⚠️ Ad artıq dəqiq deyil (video da saxlayır), amma **dəyişdirilmir**: içindəki faylların public
  linkləri sətirlərdə yazılıdır, bucket adı dəyişsə o linklər qırılar. Tətbiq faylı Storage REST API-si ilə göndərir
  (`SuggestionImageStorage`) — `storage-kt` plugin-i qəsdən quraşdırılmayıb, bax həmin fayl.
- Təkliflər: `anon`/`authenticated` → `suggestions` üzərində yalnız `SELECT`;
  `suggestion_submissions` üzərində **heç nə**. Üç RPC-yə (`submit_suggestion`,
  `get_suggestion_tickets`, `vote_suggestion`) `EXECUTE` verilib.
- `reschedule_daily_content`: `public`/`anon`-dan `EXECUTE` geri alınıb, yalnız `authenticated`-ə
  verilib (qapı funksiyanın içində yox, cədvəlin RLS-indədir).
- `increment_daily_content_view`: `public`-dən geri alınıb, `anon` və `authenticated`-ə verilib —
  hekayəyə giriş etmədən baxılır.
- `translations` view: `anon` → `SELECT`, `authenticated` → `SELECT, UPDATE` (başqa heç nə).
- `authenticated` və `service_role` qalan cədvəllərdə tam icazəlidir — məhdudlaşdırma RLS-dədir.

---

## Yoxlama

Sxemə toxunan dəyişiklikdən sonra ən azı bunlara baxın (sorğuları Supabase SQL Editor-də işlədin):

```sql
-- Trigger-lər: yuxarıdakı 7 sətir olmalıdır, artığı yox
select c.relname, t.tgname from pg_trigger t join pg_class c on c.oid = t.tgrelid
 where not t.tgisinternal and c.relnamespace = 'public'::regnamespace order by 1, 2;

-- Siyasətlər: RLS açıq olub siyasəti olmayan cədvəl QALMAMALIDIR
select c.relname from pg_class c
 where c.relnamespace = 'public'::regnamespace and c.relkind = 'r' and c.relrowsecurity
   and not exists (select 1 from pg_policy p where p.polrelid = c.oid);
select tablename, policyname, cmd, roles, qual, with_check from pg_policies
 where schemaname = 'public' order by tablename, policyname;

-- anon icazələri: yalnız SELECT/INSERT çıxmalıdır
select table_name, privilege_type from information_schema.role_table_grants
 where table_schema = 'public' and grantee = 'anon' and privilege_type not in ('SELECT','INSERT');

-- Köhnə sxem qalığı: nəticə boş olmalıdır
select proname from pg_proc where pronamespace = 'public'::regnamespace
   and replace(prosrc, 'hadith_data_id_seq', '') like '%hadith_data%';
```

Moderasiya axınını canlı sınamaq lazım gəlsə: `hadith_edits`-ə süni `pending` sətir salıb `status`-u
`approved` edin və `hadith`-də sətrin yeniləndiyini yoxlayın — bunu `begin; … rollback;` içində edin
ki, bazada iz qalmasın.

## Supabase linter — bilərəkdən qalan xəbərdarlıqlar

Database Linter bunları `WARN` kimi göstərir; hamısı qərardır, nasazlıq deyil:

| Xəbərdarlıq | Niyə belədir |
|---|---|
| `app_logs · Allow anonymous insert` (INSERT `with check true`) | Tətbiq çökmə loglarını giriş etmədən göndərməlidir. Oxu/silmə admin-only olduğu üçün məlumat sızması yoxdur; qalan risk **spam/xərc**dir — kənar skript cədvəli şişirdə bilər. |
| `hadith · insert/update (true)` | Bu cədvəldə qapı RLS deyil, **`trg_intercept_hadith`** trigger-idir: admin olmayanın yazısını `hadith_edits`-ə yönləndirib `return null` ilə ləğv edir. Linter trigger-i görmür. ⚠️ Trigger silinsə divar da yox olur — ona toxunanda bunu nəzərə al. |
| `hadith · delete (true)` | Eyni quruluş, silmə üçün: **`trg_intercept_hadith_delete`**. Admin sətri silir, redaktorun silməsi `hadith_edits`-ə `is_delete = true` kimi düşür və `return null` ilə ləğv olunur. Eyni xəbərdarlıq: trigger gedərsə divar da gedir. |
| `verse_reports · update (true)` | Redaktorlar bildirişlərin statusunu dəyişə bilir (triaj), amma silə bilmir — DELETE admin-only. |
| `submit_suggestion`, `get_suggestion_tickets`, `vote_suggestion` · anon `SECURITY DEFINER` RPC | Təkliflər axını **qəsdən girişsizdir** — tətbiqdə self-service qeydiyyat yoxdur, ona görə təklif göndərən hər kəs `anon`-dur. Cədvəllərin özü bağlıdır, bu üç funksiya yeganə qapıdır və hər biri dar işlə məhdudlaşır: göndəriş (saatlıq tavan 100 sətir), qəbz üzrə status oxuma, `±1` səs. Qalan risk **spam/xərc**dir (`app_logs`-un anonim insert-i ilə eyni sinif), məlumat sızması yox. |

⚠️ **Bu sətirlərin hamısı bir fərziyyəyə söykənir:** `authenticated` = etibarlı redaktor, çünki
self-service qeydiyyat yoxdur (hesabları yalnız layihə sahibi yaradır). Supabase panelində
Authentication → Sign In / Providers → «Allow new users to sign up» **bağlı qalmalıdır** — açılsa
yuxarıdakı `true`-ların hamısı «internetdəki hər kəs» mənasına gəlir.

Auth tərəfdə **Leaked Password Protection** açıq olmalıdır (Authentication → parol siyasəti) —
2026-08-11 tarixində linter hələ də bağlı olduğunu göstərirdi.

## Bilərəkdən saxlanılan qəribəliklər

- `hadith` cədvəlinin PK-sı və sequence-i hələ də `hadith_data_*` adlanır (cədvəl adı dəyişəndə
  qalıb). Funksional problem deyil, adlandırma borcu.
- `quran_translations_data`-nın geniş PK-sı (`id, chapter_no, verse_no, slug, text, updated_at`)
  saxlanılıb; `id`-nin unikallığı ayrıca indekslə təmin olunur.
- Admin e-poçtu siyasətlərdə hardcoded-dur (rol cədvəli yoxdur). İkinci admin lazım olsa siyasətlər
  yenidən yazılmalıdır.
- `translations` view sahibin hüquqları ilə işləyir (`security_invoker` qoşulmayıb) — RLS-i keçir,
  ona görə icazələri yuxarıdakı kimi dar saxlanılır.
- Admin öz Quran düzəlişini də `quran_edits`-dən keçirir (hədisdə isə birbaşa yazır). Bu qəsdəndir:
  tərcümə dəyişikliyi həmişə paneldə iz qoyur.

## Tətbiq tərəfi

Təkliflər: istifadəçi ekranı `SuggestionsScreen.kt`, göndərmə vərəqi `SuggestionSubmitSheet.kt`,
panel `SuggestionsManagementScreen.kt`, şəbəkə `SuggestionRepository.kt`. **Kimlik saxlanmadığı
üçün** iki şey yalnız cihazdadır (`SuggestionLocalStore`): göndəriş qəbzləri və hansı təkliflərə səs
verildiyi. Nəticələr: (1) tətbiq silinəndə «mənim təkliflərim» və səs vəziyyəti itir — sətirlər
bazada qalır; (2) eyni təklifə başqa cihazdan yenidən səs vermək mümkündür, yəni sayğac dəqiq
«unikal insan» sayı deyil. Bu, qeydiyyatsız və izsiz axının qəbul edilmiş qiymətidir — dəyişmək
istəsən əvvəlcə hansı kimliyin toplanacağına qərar vermək lazımdır.

İdarəetmə paneli: [`EditsManagementScreen.kt`](../../shared/src/commonMain/kotlin/com/cafarovceyxun/anamuslim/compose/screens/settings/EditsManagementScreen.kt),
[`EditsViewModel.kt`](../../shared/src/commonMain/kotlin/com/cafarovceyxun/anamuslim/viewModels/EditsViewModel.kt).
Modellər (`QuranEdit`, `HadithEdit`) sxemlə sütun-sütun uyğundur.

Panelin bilməli olduğu davranış: **RLS bir əməliyyatı bloklayanda PostgREST xəta yox, boş nəticə
qaytarır.** Ona görə təsdiq/rədd/silmə sorğuları `select()` ilə gedir və təsirlənən sətir sayı
sıfırdırsa istifadəçiyə bildiriş göstərilir (`strMsgEditActionBlocked`) — əks halda əməliyyat
"uğurlu" görünür, amma heç nə dəyişmir.
