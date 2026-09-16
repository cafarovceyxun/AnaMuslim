# Supabase sxemi — 2026-09-15 (son miqrasiyadan sonra)

`public` sxemindəki hər şey: cədvəllər, sütunlar, məhdudiyyətlər, indekslər, RLS, trigger-lər,
funksiyalar və icazələr.

**Mənbə:** 2026-07-27 tarixli xam dump + ondan sonra işlədilən miqrasiyalar. Nəticə iki dəst
yoxlama ilə təsdiqlənib: 22 struktur yoxlaması (RLS, trigger, funksiya, siyasət, indeks, grant) və
moderasiya axınının 9 davranış yoxlaması — hamısı **OK**. Sxem dəyişəndə bu faylı yeniləyin.

**Son dəyişiklik: 2026-09-15 (beşinci dalğa)** — **iki düzəliş**:

1. Dublikat indeksləri **mənbəni də** nəzərə alır. Əvvəl yalnız `(qrup, md5(text_ar))` idi və bu,
   Əsmaül Hüsnədə normal halı bloklayırdı: eyni ilahi ad onlarla ayədə keçir, yəni ikinci dəlilin
   ərəbcəsi birincisi ilə **eynidir** — fərq hansı ayədən götürülməsindədir. Ada ikinci dəlil əlavə
   etmək `23505` verirdi, tətbiq isə bunu «icazə və ya bağlantı problemi» kimi göstərirdi.
2. `text_az` **boş qala bilər** (CHECK `<= 6000`, `>= 1` deyil): tək bir ilahi adın tərcüməsi
   mənasızdır, məcburi sahə isə uydurmağa məcbur edərdi. `asma_name.is_visible` isə yalnız
   **Həşr surəsindəki 13 ad** üçün açıq qaldı (59:22-24 → siyahının 1–13-ü).

**Ondan əvvəl (dördüncü dalğa)** — `dua_subcategory` (alt başlıq, **məcburi deyil**)
və `dua.subcategory_slug`; həmçinin `dua.transliteration` / `asma_evidence.transliteration`.
Oxunuş ayrı sütundur, çünki mənbədə də ayrı yerdədir: hədisin `text_az`-ı rəvayətdir və dua orada
`{…}` içində oxunuş kimi verilir, **tərcüməsi isə `note` sahəsindədir**
(«Hədisdəki duanın tərcüməsi belədir: …»).

**Ondan əvvəl (üçüncü dalğa)** — `asma_evidence_count` **view**-u əlavə olundu
(ad → dəlil sayı). Səbəb: PostgREST bir cavabda **1000 sətir** verir və limitə dəyən sorğu xəta yox,
qısa cavab qaytarır. Bir ada çox dəlil düşəcəyi üçün «hamısını bir sorğuda çək» yanaşması sonrakı
adların dəlillərini **səssizcə** itirərdi — indi dəlillər ada görə ayrıca (və səhifələnərək) oxunur,
say isə view-dan gəlir (ən çox 99 sətir).

**Ondan əvvəl (ikinci dalğa)** — `dua.repeat_count` (zikr sayı) və
`asma_name.is_visible` (adın siyahıda görünüşü) əlavə olundu. Görünüş bayrağı **silmənin əvəzidir**:
`asma_evidence.name_no` CASCADE olduğu üçün adı silmək ona bağlanmış bütün dəlilləri də aparardı.

**Eyni gün, birinci dalğa** — **Dualar + Əsmaül Hüsnə** bölmələri üçün dörd cədvəl əlavə olundu
(`dua_category`, `dua`, `asma_name`, `asma_evidence`). Oxu hamıya, yazma giriş etmiş istifadəçiyə və
**yalnız öz sətrinə** (admin hamısına) — bu, sxemdəki ilk «sahibkarlıq» RLS-idir, ondan əvvəlkilər ya
tamamilə admin-only, ya da trigger ilə moderasiyadan keçirdi. `asma_name` istisnadır: 99 ad sabit
siyahıdır, yalnız admin dəyişir. Təfərrüat aşağıda.

**Ondan əvvəl: 2026-08-31** — `daily_content` uyğunluq view-u həqiqətən yalnız-oxunan edildi
(`daily_content_view_readonly`): view avto-yenilənən olduğu üçün `anon` onun üzərindən baza cədvəlinin
RLS-ini keçib yaza bilirdi. Ondan əvvəl (2026-08-30) `daily_content` növbəyə çevrilmişdi
(`daily_content_item` + gündə 5 yuva) və `reschedule_daily_content()` RPC-si əlavə olunmuşdu.
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
| `lunar_announcement` + `lunar_media_bucket_and_prune` (2026-09-15) | adminin «ayı gördük» elanı: ayın 1-i, uzunluğu (29/30), görünmə anı, media; `lunar-media` bucket-i və 12 aylıq `prune_lunar_announcements()` |
| `suggestions_publish_rejected` (2026-09-15) | `suggestions.status`-a `rejected` əlavə olundu; trigger rədd edilmiş təklifi **silmək əvəzinə** `rejected` statusu ilə yayımlayır |
| `asma_show_only_hashr_names_and_optional_translation` (2026-09-15) | `is_visible` yalnız 1–13 (Həşr 59:22-24) üçün açıq; `text_az` uzunluq CHECK-i sıfıra icazə verir |
| `dua_unique_per_source_not_per_text` (2026-09-15) | dublikat indeksləri `(hadith_id, chapter_no, verse_no)` ilə genişləndi — eyni parça **başqa mənbədən** qanunidir |
| `dua_subcategory_and_transliteration` (2026-09-15) | `dua_subcategory` cədvəli (+RLS, grant, trigger), `dua.subcategory_slug` (**`on delete set null`**), `dua.transliteration`, `asma_evidence.transliteration` |
| `asma_evidence_count_view` (2026-09-15) | ad → dəlil sayı view-u (`security_invoker`, yalnız SELECT); klient sayı sətirləri çəkərək yox, buradan alır |
| `dua_count_and_asma_visibility` (2026-09-15) | `dua.repeat_count` (1..100000 CHECK) və `asma_name.is_visible` + `(is_visible, no)` indeksi |
| `dua_and_asma_tables` (2026-09-15) | Dualar və Əsmaül Hüsnə: `dua_category`, `dua`, `asma_name`, `asma_evidence`; `set_dua_updated_at()` trigger funksiyası; `anon` yalnız SELECT |
| `asma_name_seed` (2026-09-15) | 99 adın ərəbcəsi, transliterasiyası və azərbaycanca mənası (`on conflict do update` — təkrar işlədilə bilər) |
| `quran_alt_translation` (2026-09-05) | ikinci Azərbaycanca tərcümə: `quran_translations_data.text_alt`/`note_alt`, `translations` view-una həmin sütunlar, kataloq cədvəli `quran_translation_books`, toplu yazma RPC-si `import_translation_text` |
| `quran_translation_books_grant_hardening` (2026-09-05) | yeni kataloq cədvəlində `anon`-un defolt INSERT/UPDATE/DELETE/TRUNCATE grant-ları geri alındı (`rls_hardening` qaydasının davamı) |
| `rls_hardening` | anon-un yazma deşikləri bağlandı, artıq table-level grant-lar geri alındı |
| `hadith_name_ar` | hədis struktur cədvəllərinə `name_ar` sütunları |
| `verse_reports` | ayə bildirişləri cədvəli, CHECK-lər və indekslər |
| `hadith_edits_approval` | hədis moderasiyası: `hadith_edits` üçün RLS siyasətləri, sınıq `process_hadith_approval()` götürüldü, təsdiqdə bütün sahələr köçür, yeni hədis təklifi üçün insert yolu, `status` NOT NULL |
| `edits_hardening` | Quran moderasiyası (təkrar trigger, `coalesce`, admin-only təsdiq), `quran_edits.verse_no`, `hadith` DELETE admin-only, `quran_translations_data` yazma admin-only + unikal `id` indeksi, `translations` view grant-ları, idarəetmə cədvəlləri, 17 ölü funksiya silindi |
| funksiya gigiyenası | `reject_hadith_from_edits` silindi, 6 canlı funksiyada `search_path` sabitləndi, trigger funksiyalarından `EXECUTE` geri alındı (Supabase linter tapıntıları) |
| `app_releases` (2026-07-31) | tətbiq buraxılış bildirişi cədvəli: platforma başına bir sətir, public read / admin write, `updated_at` trigger-i |
| `suggestions_story_targeting` (2026-08-31) | `suggestions.platform` (`all`/`ios`/`android`, CHECK) + `min_app_version` (buraxılış **adı**) — hekayə yalnız funksiyanı almış platformada və sürümdə görünür |
| `daily_content_view_readonly` (2026-08-31) | `daily_content` VIEW-undan `anon`/`authenticated` üçün INSERT/UPDATE/DELETE/TRUNCATE geri alındı və `security_invoker = true` qoyuldu — view avto-yenilənən idi və sahibin hüququ ilə işlədiyi üçün `daily_content_item`-in admin-only RLS-ini **keçirdi** |
| `suggestions_feature_image` (2026-08-30) | `suggestions.image_url` + public `suggestion-images` bucket (oxu hamıya, yazma admin) — «əlavə olunan funksiya buradadır» ekran görüntüsü |
| `suggestions_note_and_views` (2026-08-30) | `suggestions.note` (hekayədə görünən admin qeydi, ≤300) + `view_count` və `mark_suggestion_viewed()` RPC-si |
| `suggestions_media_list` (2026-08-30) | tək `image_url` → `media jsonb` massivi (`[{"url","type"}]`, `type ∈ image|video`), mövcud şəkillər köçürüldü; bucket 50 MB + `video/mp4`, `video/quicktime` |
| `suggestions` (2026-08-30) | istifadəçi təklifləri: `suggestion_submissions` (növbə) + `suggestions` (təsdiqlənmiş, ictimai), təsdiq trigger-i, 3 anonim RPC. **Kimlik saxlanmır** — aşağıya bax |
| `daily_content_queue_slots` (2026-08-30) | günün ayəsi **növbəsi**: `daily_content` → `daily_content_item`, gündə 5 yuva (`slot_index`), ayə aralığı (`verse_end`), hədis çıxarışı (`excerpt_ar`/`excerpt_az`), `(date, slot_index)` **DEFERRABLE** unikal; köhnə ad slot 0-ı göstərən **view** kimi qaldı |
| `daily_content_reschedule_rpc` (2026-08-30) | `reschedule_daily_content(jsonb)` — növbənin yerdəyişməsi bir ifadə ilə (upsert `GENERATED ALWAYS` id-yə görə yaramır) |
| `daily_content_view_count` (2026-08-30) | `daily_content_item.view_count` + `increment_daily_content_view(bigint)` RPC — hekayənin baxış sayı (kimlik saxlanmır) |
| `publish_rejection_reason` (2026-09-17) | rədd səbəbi ictimai olur: `admin_note` → `suggestions.note` (trigger `after update of status, **admin_note**`), mövcud iki sətir backfill edildi; `suggestions_note_len` 300 → **1000** və `suggestion_submissions.admin_note`-a ilk dəfə eyni hədd qoyuldu. Həmçinin `lunar_announcement.view_count` + `increment_lunar_announcement_view(bigint)` RPC |

---

## Cədvəllər

| Cədvəl | Sətir | Qeyd |
|---|---|---|
| `app_logs` | 0 | çökmə/loq qeydləri; `anon` yalnız INSERT, oxu/silmə admin |
| `asma_evidence` | 2 | bir Əsmaül Hüsnə adına dəlil olan ayə/hədis çıxarışı; **bir ada çox dəlil** |
| `asma_evidence_count` | — | **VIEW** — `asma_evidence`-in ad üzrə sayı (siyahıdakı nişan) |
| `asma_name` | 99 | **Əsmaül Hüsnə** — sabit siyahı, yalnız admin yazır |
| `app_releases` | 2 | ana ekrandakı yeniləmə banneri; platforma başına bir sətir, yazma admin, oxu hamıya |
| `dua` | 0 | **dualar** — bir başlığa bağlanmış hədis/ayə çıxarışı |
| `dua_category` | 0 | dua başlıqları («Səhər duaları» …) |
| `dua_subcategory` | 0 | alt başlıqlar; **məcburi deyil** — dua birbaşa başlığın altında da dura bilər |
| `daily_content_item` | 22 | **günün ayəsi/hədisi növbəsi**; gündə 5 yuva, yazma admin, oxu hamıya |
| `daily_content` | — | **VIEW** — `daily_content_item`-in `slot_index = 0` sətirləri (köhnə tətbiq buraxılışları üçün) |
| `lunar_announcement` | 0 | **qəməri ay elanı** — ay başına bir sətir, yazma admin, oxu hamıya |
| `hadith` | 289 | **əsas hədis cədvəli** (əvvəllər `hadith_data` — PK və sequence hələ o adı daşıyır) |
| `hadith_book` | 3 | kitab |
| `hadith_chapter` | 99 | bab |
| `hadith_edits` | 0 | redaktor təklifləri (moderasiya) |
| `hadith_sub_chapter` | 61 | alt-bab |
| `hadith_volume` | 2 | cild |
| `quran_edits` | 0 | tərcümə təklifləri — **2026-08-01-də boşdur** (07-27-dəki 34 gözləyən təklifi admin özü emal edib) |
| `quran_translations_data` | 6236 | **əsas tərcümə cədvəli** — bir sətirdə iki tərcümə (`text` və `text_alt`) |
| `quran_translation_books` | 2 | **tərcümə kataloqu** — hansı kitab var, hansı sütundadır, hamıya açıqdırmı; oxu hamıya, yazma admin |
| `resource_updates` | 1 | klient üçün versiya sayğacı (public read) |
| `resource_updates_admin` | 1 | admin yazır, trigger `resource_updates`-ə köçürür |
| `suggestion_submissions` | 0 | **istifadəçi təklifləri, moderasiya növbəsi** — yalnız admin oxuyur; yazma yalnız `submit_suggestion()` RPC-si ilə |
| `suggestions` | 0 | təsdiqlənmiş təkliflər, hamıya görünür; `vote_count` sayğacı |
| `verse_reports` | 0 | ayə səhv bildirişləri |
| `translations` | — | **VIEW** (`quran_translations_data` üzərində, aşağıda) |

RLS bütün 20 cədvəldə **aktivdir** və hamısının ən azı bir siyasəti var.

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
                        text_alt text · note_alt text
                        ℹ️ `text_alt`/`note_alt` **ikinci tərcümədir**, ayrı sətir deyil: sətirlər
                           eyni `slug = 'az'` sətirləridir, dəyişən sütundur. Ayə uyğunluğu birə-bir
                           olduğu üçün yeni tərcümə köhnəsinə toxunmadan doldurulur. Hansı sütunun
                           hansı kitab olduğunu `quran_translation_books.source_column` deyir.

quran_translation_books slug text NN (PK) · source_column text NN = 'text'
                        book_name text NN = '' · author_name text NN = ''
                        lang_code text NN = 'az' · lang_name text NN = 'Azərbaycan'
                        is_public bool NN = false · updated_at timestamptz NN = now()
                        ℹ️ `is_public = false` → kitab tərcümə siyahısında **yalnız giriş etmiş**
                           istifadəçiyə görünür (süzgəc klientdədir, `TranslationViewModel`).
                           Admin tərcüməni hazırlayarkən özü sınayır, hazır olanda bayrağı açır və
                           kitab hamıya çıxır — **yeni tətbiq buraxılışı lazım deyil**.
                           ⚠️ Bayrağı **geri bağlamaq** da işləyir, amma yalnız 2026-09-08-dən:
                           `TranslationVisibilitySync` bağlanmış kitabın cihazdakı nüsxəsini silir
                           və oxucu seçimindən çıxarır (açılışda + tərcümə ekranında). Ondan əvvəl
                           süzgəc yalnız siyahını qururdu, endirilmiş kitab isə «kataloqdan
                           çıxarılmış yerli kitab» qolundan geri qayıdırdı — yəni açar yalnız hələ
                           endirməmiş istifadəçiyə təsir edirdi.
                        ℹ️ CHECK: `source_column ∈ (text, text_alt)`

resource_updates        id int NN = 1 · version int = 0 · updated_at timestamptz = now()
resource_updates_admin  id int NN = 1 · version int = 0 · updated_at timestamptz = now()

suggestion_submissions  id bigint NN (identity) · ticket uuid NN = gen_random_uuid() (UNIQUE)
                        body text NN · category text NN = 'other' · app_version text · platform text
                        status text NN = 'pending' · admin_note text · suggestion_id bigint
                        created_at timestamptz NN = now() · updated_at timestamptz NN = now()
                        ℹ️ CHECK: `admin_note` ≤ **1000** (2026-09-17). Rədd ediləndə bu mətn
                           `suggestions.note`-a köçür, ona görə iki sütunun həddi **eyni olmalıdır** —
                           əks halda uzun cavabla «Rədd et» CHECK pozuntusu ilə dayanır (məhz belə
                           olmuşdu: hədsiz `admin_note` 323 simvola çatmışdı, `note` isə 300-lük idi).
                           Klientdəki qarşılığı `SuggestionsManagementScreen.ADMIN_NOTE_MAX`.
                        ⚠️ **Qəsdən `device_id`/`user_id` YOXDUR.** Göndərənin yeganə izi `ticket`-dir
                           və o yalnız cihazda saxlanılır (`SuggestionLocalStore`) — baza kimin nə
                           göndərdiyini bilmir. `id` ardıcıl olduğu üçün status sorğusu `ticket`
                           üzərindən gedir, `id` ilə növbəni açmaq mümkün deyil.

lunar_announcement      id bigint NN (identity) · hijri_year int NN · hijri_month int NN
                        start_date date NN · length_days int NN · sighted_at timestamptz
                        media jsonb NN = '[]' · note text · view_count int NN = 0
                        created_at timestamptz NN = now() · updated_at timestamptz NN = now()
                        ℹ️ `view_count` (2026-09-17) — hekayənin təxmini baxış sayı; klient **ilk
                           baxışda** `increment_lunar_announcement_view()` çağırır («görüldü»
                           vəziyyəti cihazdadır). Funksiya hekayələri (`suggestions.view_count`) və
                           günün ayəsi ilə eyni naxış. Sayğac `updated_at`-ı tərpədir, amma qəməri
                           yolda `updated_at` heç yerdə oxunmur.
                        ℹ️ `start_date` — ayın **1-inin miladi günü**. Klient platformanın Ümmül-Qüra
                           təqvimində həmin qəməri ayın 1-ini tapıb aradakı **gün fərqini** çıxarır və
                           onu `hijriDate(millis + gün)` girişinə verir (`LunarCalendar.offsetDaysFor`).
                           Yəni `expect/actual` çevirməyə toxunulmur, hər iki platforma eyni nəticəni verir.
                        ℹ️ `length_days ∈ (29, 30)` — paylaşılan aylıq təqvim şəklinin sətir sayı da budur.
                           Sürüşdürmə ayın **uzunluğunu** dəyişmir, ona görə uzunluq ayrıca üst-yazma
                           kimi tətbiq olunur (`LunarMonth.Override`).
                        ℹ️ `sighted_at` klient tərəfindən **həmişə `+00:00` ilə** yazılır və oxunanda
                           qurşağa çevrilmir: bu, ölçülmüş an deyil, **elan olunmuş** vaxtdır — yerli
                           saata çevirsək eyni elan hər ölkədə başqa saat göstərərdi.
                        ℹ️ `media` = `suggestions.media` ilə **eyni forma** (`[{"url","type"}]`),
                           klientdə də eyni model oxuyur. Linklər `lunar-media` bucket-indəndir.
                        ℹ️ UNIQUE `(hijri_year, hijri_month)`. Klient `upsert` yox, **update → insert**
                           edir ki, `id` sabit qalsın: cihazlar «yeni elan gəldi» qərarını id ilə verir
                           və hər redaktədə yeni id görsəydilər istifadəçilərin −2/+2 seçimi hər dəfə
                           sıfırlanardı.

suggestions             id bigint NN (identity) · body text NN · category text NN = 'other'
                        status text NN = 'open' · vote_count int NN = 0 · view_count int NN = 0
                        media jsonb NN = '[]' · note text · source_submission_id bigint
                        platform text NN = 'all' · min_app_version text
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
                        ℹ️ `platform` (`all`/`ios`/`android`) + `min_app_version` hekayənin **görünmə
                           şərtidir**: funksiya hansı platformada və hansı buraxılışda gəldi.
                           Süzgəc **klientdədir** (`Suggestion.isVisibleOn`) — sorğuda kimlik yoxdur,
                           ona görə server süzə bilmir. `min_app_version` **versionCode deyil**,
                           buraxılış **adıdır** (`2026.08.31`) və rəqəm qrupları ilə müqayisə olunur
                           (`AppVersionName`); `app_releases.min_version` isə tam ayrı say sahəsidir.
                           ⚠️ `suggestion_submissions.platform` bununla eyni şey deyil: o, təklifi
                           **göndərənin** cihazıdır. `publish_approved_suggestion()` açıq sütun
                           siyahısı ilə yazdığı üçün onu buraya köçürmür — hədəf platformanı admin
                           özü seçir (Ayarlar → Təkliflər → Görünmə).

dua_category            slug text NN (PK) · name text NN · name_ar text · description text
                        sort_no int NN = 0 · created_by uuid = auth.uid() · created_at timestamptz NN = now()
                        updated_at timestamptz NN = now()
                        ℹ️ `slug` klientdə addan qurulur (`duaCategorySlug`, az hərfləri ASCII-yə düşür);
                           toqquşanda `-2`, `-3` … əlavə olunur.
                        ℹ️ CHECK: slug `^[a-z0-9][a-z0-9_-]{0,79}$`, ad 1–120, ərəbcə ad ≤120, izah ≤1000

dua_subcategory         slug text NN (PK) · category_slug text NN · name text NN · name_ar text
                        sort_no int NN = 0 · created_by uuid = auth.uid()
                        created_at timestamptz NN = now() · updated_at timestamptz NN = now()
                        ℹ️ Slug bütün alt başlıqlar arasında unikaldır (PK), başlıq daxilində yox.

dua                     id bigint NN (identity, GENERATED ALWAYS) · category_slug text NN
                        subcategory_slug text (alt başlıq; null = birbaşa başlığın altında)
                        source_type text NN ∈ (hadith, quran) · hadith_id bigint
                        chapter_no int · verse_no int · verse_end int
                        text_ar text NN · text_az text NN · note text · source text
                        transliteration text · repeat_count int · sort_no int NN = 0
                        created_by uuid = auth.uid()
                        created_at timestamptz NN = now() · updated_at timestamptz NN = now()
                        ℹ️ `repeat_count` → zikrin təkrar sayı («33 dəfə»); null = say göstərilmir.
                           CHECK: null ya da 1..100000.
                        ℹ️ `transliteration` → duanın latın hərfləri ilə oxunuşu. Ayrı sütundur, çünki
                           mənbədə də ayrıdır: hədisin `text_az`-ı rəvayətdir, dua orada `{…}` içində
                           **oxunuş** kimi verilir, **tərcüməsi** isə `note` sahəsindədir. Seçim
                           ekranı buna görə üç mənbə blokunu (ərəbcə · rəvayət · qeyd) göstərir və
                           hansının hara düşdüyünü seçən adam deyir.
                        ℹ️ `text_az` **boş ola bilər** (CHECK yalnız yuxarı həddi yoxlayır): tək bir
                           ilahi adın və ya qısa zikrin tərcüməsi mənbədə olmaya bilər.
                        ℹ️ `text_*` **çıxarışdır**, mənbənin tam mətni deyil: mənbə redaktə olunsa dua
                           öz mətni ilə qalır. Vurğu mətni mənbədə axtarmaqla qurulur
                           (`excerptMatchRange`) — tapılmasa sadəcə vurğusuz göstərilir.
                        ⚠️ `id` **GENERATED ALWAYS** — açıq `id` ilə insert mümkün deyil (klient
                           `id = null` göndərir, supabase-kt `explicitNulls = false` ilə sütunu atır).
                        ℹ️ `created_by` **modeldə yoxdur**: baza `default auth.uid()` ilə doldurur və
                           RLS elə həmin dəyərə baxır — sahibliyi klient yaza bilmir.

asma_name               no int NN (PK, 1..99) · name_ar text NN · transliteration text NN
                        meaning text NN · description text · is_visible bool NN = true
                        updated_at timestamptz NN = now()
                        ℹ️ `description` uzun izah üçündür, hələ boşdur (UI onu şərti göstərir).
                        ⚠️ `is_visible = false` → ad **silinmir**, sadəcə oxucudan gizlənir. Silmək
                           olmaz: `asma_evidence.name_no` CASCADE-dir, ad gedəndə ona bağlanmış bütün
                           dəlillər də gedər. Süzgəc **klientdədir** (`AsmaScreen`): admin gizli adı
                           solğun və «Gizli» nişanı ilə görür, adi istifadəçi heç görmür — sətir
                           hamıya `SELECT`-ə açıq olduğu üçün bu, məxfilik deyil, **kurasiya** qapısıdır.

asma_evidence           id bigint NN (identity, GENERATED ALWAYS) · name_no int NN
                        source_type text NN ∈ (hadith, quran) · hadith_id bigint
                        chapter_no int · verse_no int · verse_end int
                        text_ar text NN · text_az text NN · note text · source text
                        sort_no int NN = 0 · created_by uuid = auth.uid()
                        created_at timestamptz NN = now() · updated_at timestamptz NN = now()
                        ℹ️ Forması `dua` ilə **eynidir** (eyni sütunlar, eyni CHECK-lər); fərq yalnız
                           hədəfdədir — başlıq yerinə ad nömrəsi.

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
- Dua/Əsma: `dua_category(slug)`, `dua_subcategory(slug)`, `dua(id)`, `asma_name(no)`,
  `asma_evidence(id)` PK.
  ⚠️ `dua.subcategory_slug → dua_subcategory.slug` **`on delete set null`**, CASCADE **yox**: alt
  başlıq silinəndə içindəki dualar itmir, başlığın birbaşa altına qalxır. `dua_subcategory.category_slug`
  isə CASCADE-dir (başlıq gedəndə alt başlıqları da gedir).
- `dua_subcategory` RLS-i `dua_category` ilə eynidir (oxu hamıya, yazma öz sətrinə + admin);
  `anon`-a yalnız `SELECT` verilib.
  FK: `dua.category_slug → dua_category.slug` (**CASCADE**, `on update cascade` də),
  `asma_evidence.name_no → asma_name.no` (CASCADE), hər iki cədvəldə `created_by → auth.users.id`
  (SET NULL). ⚠️ `hadith_id` **FK deyil** — hədis məzmunu Supabase-də olsa da dua onun **surətini**
  daşıyır; CASCADE silinən hədislə birlikdə duanı da aparardı, halbuki dua öz mətni ilə yaşaya bilir.
- Mənbə forması `case`-lə yazılmış CHECK-dir (`dua_source_shape`, `asma_evidence_source_shape`):
  `hadith` → `hadith_id` dolu, ayə sahələri **null**; `quran` → `chapter_no` 1..114, `verse_no ≥ 1`,
  `verse_end` null ya da `≥ verse_no`, `hadith_id` **null**; başqa növ → `false`.
  ⚠️ `case` qəsdəndir: adi `or` zəncirində null müqayisə **null** verir, NULL CHECK isə **keçir** —
  yəni `source_type = 'quran'` sətri heç bir ayə nömrəsi olmadan yazıla bilərdi.
- Dublikat qoruması **mənbə ilə birlikdə** unikaldır:
  `dua(category_slug, md5(text_ar), coalesce(hadith_id,-1), coalesce(chapter_no,-1), coalesce(verse_no,-1))`
  və `asma_evidence`-də eyni forma (`name_no` ilə).
  ⚠️ Mənbə açarları olmadan yazmaq **səhv idi**: eyni ilahi ad onlarla ayədə keçir, yəni ikinci
  dəlilin `text_ar`-ı birincisi ilə eyni olur — indeks normal halı bloklayırdı. İndi yalnız
  **eyni mənbədən eyni parça** təkrar sayılır.
- Şərti unikal indekslər (bir redaktora bir gözləyən təklif):
  `only_one_pending_per_editor` on `hadith_edits(hadith_id, editor_email) where status='pending'`
  `quran_only_one_pending_per_editor` on `quran_edits(translation_id, editor_email) where is_approved=false`
- Qəməri elan: `hijri_month ∈ 1..12`, `hijri_year ∈ 1300..1700`, `length_days ∈ (29, 30)`,
  `jsonb_typeof(media) = 'array'`, qeyd ≤300; UNIQUE `(hijri_year, hijri_month)` və
  `lunar_announcement_start_date_idx (start_date desc)`.
- Təkliflər: `suggestion_submissions.status ∈ (pending, approved, rejected)`,
  `suggestions.status ∈ (open, planned, done, rejected)`, hər iki cədvəldə `category ∈ (feature, bug, content, other)`
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
       updated_at,
       text_alt,       -- coalesce YOXDUR: bu kitab moderasiyadan keçmir
       note_alt
  from quran_translations_data qt;
```

Tətbiq tərcüməni bu view üzərindən yazır; `instead of` trigger düzəlişi `quran_edits`-ə salır.
View sahibin hüquqları ilə işləyir, ona görə icazələri dar saxlanılır (aşağıda).

⚠️ **View-a sütun əlavə edəndə `create or replace` işlət, `drop`+`create` yox.** Moderasiya divarı
məhz bu view-un üzərindəki `instead of` trigger-idir (`check_quran_before_update`); `drop` onu da
aparır və hər redaktor düzəlişi birbaşa əsas cədvələ düşür. Sütunlar yalnız **sonda** əlavə oluna
bilər. (2026-09-05-də `text_alt`/`note_alt` belə əlavə olundu; trigger yoxlandı, yerindədir.)

Klient tərəfi kitabın sütununu PostgREST ləqəbi ilə oxuyur (`text:text_alt`), ona görə DTO
(`SupabaseTranslation`) hər iki kitab üçün eynidir — bax `SharedTranslationDownloader`.

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

⚠️ **Yalnız oxunur — 2026-08-31-dən etibarən həqiqətən.** Bu bölmə əvvəl view-u «yalnız oxunan»
sayırdı, çünki köhnə admin buraxılışının `upsert ... on_conflict=date` çağırışı view üzərində işləmir.
**Bu, yazmanın qarşısını almırdı:** view avto-yenilənəndir (`is_updatable = YES`), `anon` roluna
INSERT/UPDATE/DELETE/TRUNCATE verilmişdi, `security_invoker` isə qoyulmamışdı — yəni yazma **sahibin
(postgres) hüququ ilə** icra olunub `daily_content_item`-in admin-only RLS-ini tamamilə keçirdi.
Anon açarı hər APK/IPA-nın içindədir, deməli növbəni istənilən adam dəyişə və ya silə bilərdi.
`daily_content_view_readonly` miqrasiyası yazma hüquqlarını geri aldı və `security_invoker = true`
qoydu; `SELECT` qaldığı üçün köhnə buraxılışlar oxumağa davam edir (24 sətir yoxlanıldı).

📌 **Qayda:** uyğunluq view-u yaradanda **hər ikisini** et — lazımsız hüquqları geri al və
`security_invoker = true` qoy. «Tətbiq bu view-a yazmır» arqumenti hüquq deyil; bunu yalnız
`GRANT`-lar həll edir. Yeganə qəsdən `security_definer` qalan view `translations`-dır (moderasiya
trigger-i məhz onun üzərindədir).

Yeni kod bu view-a **toxunmur**; hər şey `daily_content_item` üzərindəndir
(`DailyContentRepository`).

---

## `asma_evidence_count` VIEW

```sql
create view public.asma_evidence_count
with (security_invoker = true) as
  select name_no, count(*)::int as evidence_count
    from public.asma_evidence
   group by name_no;
```

**Niyə var:** siyahıda hər adın yanında dəlil sayı göstərilir. Sayı sətirləri çəkib saymaqla almaq
olmur — PostgREST bir cavabda **1000 sətir** verir (yoxlanıldı) və limitə dəyən sorğu xəta yox,
**qısa cavab** qaytarır. 99 ad × çox dəlil bu həddi keçəcək, yəni sonrakı adların sayı sıfır kimi
görünərdi. View ən çox 99 sətir verir.

`security_invoker = true` və yalnız `SELECT` grant-ı (`anon`, `authenticated`) — CLAUDE.md-dəki
«view yaradanda hər ikisini et» qaydası. Aqreqat view onsuz da avto-yenilənən deyil, amma qayda
grant-lara söykənir, strukturun təsadüfünə yox.

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
| `suggestion_submissions` | `on_suggestion_approved` | `publish_approved_suggestion()` | `approved` → sətri `suggestions`-a `open` kimi köçürür və `suggestion_id`-ni geri yazır. **`rejected` → eyni cür yayımlanır, amma `rejected` statusu ilə** (2026-09-15-dən; əvvəl yayımlanan sətir silinirdi) **və `admin_note` → `suggestions.note`** (2026-09-17: səbəbsiz «Rədd edilənlər» bölməsi işini görmürdü — eyni təklif yenə təkrar gəlirdi). `pending`-ə qaytarılanda sətir silinir — növbəyə qayıdan təklif ictimai deyil. Artıq yayımlanmış sətrin statusu id saxlanaraq dəyişdiyi üçün rədd → təsdiq geri dönüşü səsləri itirmir; təsdiqdə `note` təmizlənir, çünki orada o, «funksiya haradadır» izahıdır. ⚠️ Trigger `after update of status, **admin_note**`-dur: səbəb rəddən sonra yazılanda da ictimai sətir yenilənsin |
| `suggestions` / `suggestion_submissions` | `*_set_updated_at` | `set_suggestions_updated_at()` | `updated_at` (INVOKER, iki cədvəl bir funksiyanı bölüşür) |
| `lunar_announcement` | `lunar_announcement_set_updated_at` | `set_lunar_announcement_updated_at()` | `updated_at` |
| `app_releases` | `app_releases_set_updated_at` | `set_app_releases_updated_at()` | `updated_at` — klient sətri açıq `null` ilə göndərir, BEFORE trigger NOT NULL yoxlamasından əvvəl doldurur |
| `dua_category` / `dua` / `asma_name` / `asma_evidence` | `*_set_updated_at` | `set_dua_updated_at()` | `updated_at` (INVOKER, dörd cədvəl bir funksiyanı bölüşür) |
| `quran_translation_books` | `quran_translation_books_set_updated_at` | `set_quran_translation_books_updated_at()` | `updated_at` |

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
| `prune_lunar_announcements` | ✅ (RPC, `authenticated`) — 12 aydan köhnə elanları **və `lunar-media`-dakı fayllarını** silir; admin yoxlaması funksiyanın **içindədir** |
| `reschedule_daily_content` | ❌ `INVOKER` (RPC, `authenticated`) |
| `increment_daily_content_view` | ✅ (RPC, `anon`+`authenticated`) |
| `increment_lunar_announcement_view` | ✅ (RPC, `anon`+`authenticated`) — `lunar_announcement.view_count` +1 |
| `set_suggestions_updated_at` | ❌ `INVOKER` |
| `set_lunar_announcement_updated_at` | ❌ `INVOKER` (trigger; `EXECUTE` `public`/`anon`/`authenticated`-dən geri alınıb) |
| `set_verse_reports_updated_at` | ❌ `INVOKER` |
| `set_app_releases_updated_at` | ❌ `INVOKER` |
| `import_translation_text` | ❌ `INVOKER` (RPC, `authenticated`) |
| `set_quran_translation_books_updated_at` | ❌ `INVOKER` |
| `set_dua_updated_at` | ❌ `INVOKER` (dörd dua/əsma cədvəlinin trigger-i; `EXECUTE` geri alınıb) |

`reschedule_daily_content(items jsonb)` növbənin `(date, slot_index)` yerlərini **bir** `update`
ifadəsi ilə yazır və dəyişən sətir sayını qaytarır. `SECURITY INVOKER`-dir: RLS qüvvədədir, yəni
admin olmayan çağırış xəta yox, **0** alır — klient bu sayı yoxlayır. Bir ifadə olması vacibdir:
`(date, slot_index)` unikallığı təxirə salınıb, aralıq toqquşma yalnız eyni tranzaksiyada bağışlanır.

`increment_daily_content_view(p_id)` və `increment_lunar_announcement_view(p_id)`
`SECURITY DEFINER`-dir, çünki sayğacı artırmaq cədvələ yazmaq deməkdir, yazma isə admin-onlydır.
Funksiya bir çağırışda yalnız **+1** edir və başqa heç nəyə toxunmur; qalan risk `vote_suggestion`
ilə eyni sinifdəndir (spam/xərc, məlumat sızması yox). `get_advisors(type: security)` hər ikisini
«anon `SECURITY DEFINER` çağıra bilir» kimi WARN sayır — bu, **qəbul edilmiş** siyahıdır, aşağıdakı
təhlükəsizlik cədvəlindəki eyni sətir onları əhatə edir.

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
lunar_announcement      SELECT anon,authenticated: true
                        ALL authenticated: email = admin        ← yazma yalnız admin
suggestions             SELECT anon,authenticated: true
                        ALL authenticated: email = admin        ← yazma yalnız admin
dua_category            SELECT anon,authenticated: true
dua                     INSERT authenticated: created_by = auth.uid() OR email = admin
asma_evidence           UPDATE authenticated: created_by = auth.uid() OR email = admin
                        DELETE authenticated: created_by = auth.uid() OR email = admin
                        ⚠️ Sxemdəki yeganə «sahibkarlıq» modelidir: redaktor **öz** sətrini dəyişir və
                           silir, başqasınınkına toxuna bilmir; admin hamısına. `created_by` null olan
                           sətrə (admin SQL-i ilə salınmış) yalnız admin toxuna bilir.
                        ℹ️ INSERT-in `with check`-i sahibliyi **məcbur edir**: klient `created_by`
                           göndərmir, baza `default auth.uid()` ilə doldurur — yəni başqasının adına
                           sətir yazmaq mümkün deyil.
asma_name               SELECT anon,authenticated: true
                        ALL authenticated: email = admin     ← 99 ad sabitdir
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
  (`SuggestionMediaStorage`) — `storage-kt` plugin-i qəsdən quraşdırılmayıb, bax həmin fayl.
- Storage `lunar-media` bucket: `suggestion-images` ilə **eyni qayda** (public oxu, admin yazma,
  50 MB, şəkil + mp4/quicktime), amma **ayrı** bucket-dir: `prune_lunar_announcements()` 12 aydan
  köhnə elanların fayllarını silir və bir bucket-i bölüşsəydilər funksiya hekayələrinin şəkillərini
  də aparardı. Klient tərəfi `LunarMediaStorage` (`MediaStorage` sinfinin ikinci nüsxəsi).
- Qəməri elan: `anon` → `SELECT`; `authenticated` → `SELECT/INSERT/UPDATE/DELETE`, RLS isə yazmanı
  adminə bağlayır.
- Təkliflər: `anon`/`authenticated` → `suggestions` üzərində yalnız `SELECT`;
  `suggestion_submissions` üzərində **heç nə**. Üç RPC-yə (`submit_suggestion`,
  `get_suggestion_tickets`, `vote_suggestion`) `EXECUTE` verilib.
- `reschedule_daily_content`: `public`/`anon`-dan `EXECUTE` geri alınıb, yalnız `authenticated`-ə
  verilib (qapı funksiyanın içində yox, cədvəlin RLS-indədir).
- `increment_daily_content_view` və `increment_lunar_announcement_view`: `public`-dən geri alınıb,
  `anon` və `authenticated`-ə verilib — hekayəyə giriş etmədən baxılır.
- `translations` view: `anon` → `SELECT`, `authenticated` → `SELECT, UPDATE` (başqa heç nə).
- `quran_translation_books`: `anon` → yalnız `SELECT`; `authenticated` tam icazəlidir, qapı RLS-dədir
  (yazma admin-only). ⚠️ Yeni public cədvəl yaradılanda Supabase `anon`-a da INSERT/UPDATE/DELETE
  verir — `quran_translation_books_grant_hardening` onları geri aldı. Yeni cədvəldə bunu təkrarla.
- `import_translation_text`: `public`/`anon`-dan `EXECUTE` geri alınıb, yalnız `authenticated`-ə
  verilib. Funksiya `SECURITY INVOKER`-dir, ona görə admin yoxlaması funksiyanın içində **yoxdur** —
  qapı `quran_translations_data`-nın admin-only UPDATE siyasətidir; admin olmayan çağırış xəta yox,
  **0** alır və klient həmin sayı yoxlayır (`TranslationImportRepository`).
- Dua/Əsma: `anon` → yalnız `SELECT` (defolt gələn INSERT/UPDATE/DELETE/TRUNCATE geri alındı —
  `quran_translation_books_grant_hardening` qaydasının davamı); `authenticated` → dördündə də
  SELECT/INSERT/UPDATE/DELETE, qapı RLS-dədir. 2026-09-15-də canlı açarla yoxlanıldı: anon oxuyur,
  hər üç yazma cəhdi `42501` (HTTP 401) alır.
- `authenticated` və `service_role` qalan cədvəllərdə tam icazəlidir — məhdudlaşdırma RLS-dədir.

---

## Edge Functions

### `qibla-tiles` — qiblə xəritəsinin tayl proxy-si *(2026-09-16)*

Mənbə: `supabase/functions/qibla-tiles/` (repoda saxlanılır — bazadan fərqli olaraq funksiya
kodu versiya nəzarətindədir).

Yol: `GET /functions/v1/qibla-tiles/{layer}/{z}/{x}/{y}` — `layer` ∈ `street` | `sat` | `sat_hd`.

**Niyə tətbiq birbaşa xəritə provayderinə getmir:**

1. **Məxfilik** — istifadəçinin IP-si provayderə çatmır, sorğu Supabase-dən gedir.
2. **Açar** — HD peyk qatının API açarı serverdədir, GPLv3 repoda görünmür.
3. **Keçid** — provayder tətbiq yeniləməsi olmadan dəyişdirilə bilir.

**Env dəyişənləri:**

| Dəyişən | İzah |
|---|---|
| `QIBLA_SAT_HD_KEY` | HD qatının açarı. Yoxdursa funksiya `503` verir |
| `QIBLA_HD_ENABLED` | `"false"` → HD qatı söndürülür (xərc açarı) |
| `QIBLA_STREET_URL` / `QIBLA_SAT_URL` / `QIBLA_SAT_HD_URL` | Yuxarı axın şablonları |

⚠️ **`503`/uğursuz cavab tətbiqdə davranış dəyişdirir:** `QiblaTileStore.highResAvailable` düşür,
UI HD qatını **təklif etməyi dayandırır** və açıq peyk qatına qayıdır. Yəni kvota bitəndə xəritə
ağarmır. Bu, `AppStoreReviewProvider.isAvailable` naxışının serverdən idarə olunan variantıdır.

⚠️ **Zoom hədləri iki yerdə yazılıb** — funksiyadakı `LAYERS[...].maxZoom` və tətbiqdəki
`QiblaMapLayer.maxZoom`. **Eyni olmalıdırlar**: fərqlənsə tətbiq mövcud olmayan tayl istəyir,
funksiya `400` qaytarır və xəritə səssizcə boş qalır. Nə kompilyator, nə test bunu tutur.

**Loglama:** funksiya heç nə loglamır. Sürət limiti IP-nin qısaldılmış SHA-256 hash-ini bir
dəqiqəlik yaddaşda saxlayır, sonra atır — `PRIVACY.md` bunu açıq yazır.

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

-- Qeyd hədləri: `admin_note` və `note` EYNİ rəqəmdə olmalıdır (rədd cavabı birindən o birinə köçür)
select conname, pg_get_constraintdef(oid) from pg_constraint
 where conname in ('suggestions_note_len', 'suggestion_submissions_admin_note_len');

-- Rədd edilmiş sətirdə səbəb boş qalmamalıdır (admin cavab yazıbsa)
select s.id, s.note is null as note_missing from public.suggestions s
  join public.suggestion_submissions q on q.suggestion_id = s.id
 where s.status = 'rejected' and q.admin_note is not null;
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

### Qəməri təqvim elanı (2026-09-15)

| | |
|---|---|
| Admin girişi | İdarəetmə paneli → «Qəməri ay elanı» (`LunarAnnouncementManagementScreen`) |
| İstifadəçi girişi | Ana ekran hekayə zolağı → «Qəməri təqvim» (günün ayəsinin yanında, son 12 ay) |
| Şəbəkə | `LunarAnnouncementRepository`, media `LunarMediaStorage` |
| Tarix riyaziyyatı | `LunarCalendar` (gün fərqi) + `LunarMonth.Override` (29/30) |
| Cihaz vəziyyəti | `prayer.lunar_announced_offset`, `prayer.lunar_announcement_id`, `prayer.lunar_announcements`, `prayer.lunar_story_seen` — hamısı `DEVICE_LOCAL_KEYS`-dədir |

⚠️ **İki ayrı sürüşdürmə var və onlar toplanır:** serverin elanı (`announcedLunarOffsetDays`) və
istifadəçinin öz −2/+2 seçimi (`lunarOffsetDays`). Qəməri tarix göstərən hər yer **cəmi** oxumalıdır
(`PrayerSettings.effectiveLunarOffsetDays`); yalnız birini götürmək tətbiqin bir yerində bir tarix,
başqa yerində başqa tarix deməkdir və nə kompilyator, nə test bunu tutur.

⚠️ **Yeni elan istifadəçinin düzəlişini sıfırlayır** — qəsdən, «yeni ay göründüyü tarixdən etibarən
hamıda eyni tarix». Sıfırlama elanın **id-si dəyişəndə** olur, ona görə `upsert` yox, `update → insert`
işlədilir (bax sütun qeydləri).

### Dualar / Əsmaül Hüsnə (2026-09-15)

| | |
|---|---|
| Giriş | Namaz vaxtları ekranı → «Dua və zikr» kartları (`DuaEntryCards`) |
| Ekranlar | `compose/screens/dua/` — `DuaScreen`, `AsmaScreen`, `DuaSourceSheet`, `ExcerptPicker` |
| Şəbəkə | `DuaRepository`, `AsmaRepository` (+ `DuaPreferences` oflayn keşi) |
| ViewModel | `DuaViewModel`, `AsmaViewModel` |

Bilməli olduğun üç şey:

1. **Çıxarış seçimi `readOnly` mətn sahəsindən keçir.** `SelectionContainer` seçimi çağırana vermir,
   ona görə `ExcerptField` `FormTextField(value: TextFieldValue, readOnly = true)` işlədir və seçimi
   `TextFieldValue.selection`-dan oxuyur. Klaviatura açılmır, mətn dəyişmir. `TextRange` **tərs** də
   ola bilər (sağdan sola sürükləmə), ona görə `min`/`max` ilə kəsilir.

2. **Vurğu axtarışdakı ilə eyni deyil.** `withSearchHighlight` sorğunu söz-söz bölür; dua çıxarışı
   bütöv cümlədir və eyni məntiqlə hədisin yarısı sarıya düşərdi. `withExcerptHighlight` /
   `excerptMatchRange` **bitişik** bir aralıq tapır (yastılanmış mətn üzərində, yəni hərəkə fərqi
   pozmur) və tapmasa **heç nə** boyamır — mənbə redaktə olunandan sonra səpələnmiş sarı yanlış
   təəssürat yaradardı.

3. **Dəlillər Quran ardıcıllığı ilə düzülür** (`inQuranOrder`, `AsmaRepository.kt`): əvvəl ayələr
   (surə, sonra ayə nömrəsi), sonra hədislər. Sıralama **klientdədir**, çünki qarışıq mənbəli
   siyahını SQL-də düzmək üçün üç sütun üzrə null-aware `order by` lazım olardı və keşdən gələn
   siyahı onsuz da eyni sıraya salınmalıdır. `sort_no`/`id` yalnız bərabərlikdə həll edicidir —
   əl ilə sıralama hələ yoxdur.

4. **Dəlillər ada görə yüklənir, hamısı birdən yox.** `AsmaViewModel.ensureEvidence(nameNo)` —
   vərəqləyicinin hər səhifəsi açılanda çağırılır, artıq yüklənibsə heç nə etmir. Siyahıdakı say
   `asma_evidence_count` view-undan gəlir. Səbəb yuxarıdakı 1000 sətir həddidir; ikinci səbəb isə
   trafikdir — istifadəçi bir anda bir ada baxır. Dəlil siyahısı `LazyColumn`-dur (üfüqi pager-in
   içində şaquli lazy siyahı sərbəstdir), keş isə son **20** adı saxlayır.

5. **Yazma iki ayrı ViewModel instansiyasından gedir.** Duanı oxucudakı seçim ekranı yazır, siyahını
   isə dua ekranının öz instansiyası göstərir — iOS-da hər ikisi proses boyu yaşayır. Ona görə
   `DuaViewModel`/`AsmaViewModel`-in yanında **instansiyadan kənar** `revision` sayğacı var və ekranlar
   onu `LaunchedEffect` açarı kimi işlədir (`HadithViewModel.hadithContentRevision` ilə eyni tələ).

Mənbəyi açan «Hədisi aç» / «Oxucuda aç» düymələri `LocalDuaActions` seam-indəndir və seam **`null`
defolt** daşıyır: qoşulmamış hostda düymə görünmür (basılıb heç nə etmir yerinə). Android tərəfi
`ActivityPrayerTimes`, paylaşılan host `rememberNavDuaActions`.


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
