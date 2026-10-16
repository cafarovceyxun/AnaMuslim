-- delta.sh → Oracle (yeni server). `delta_in` cədvəli artıq Tokio paketləri ilə doludur (delta.sh
-- tranzaksiyanı açıb COPY edir). Hər addım `R|bölmə|hökm|izah` sətri çap edir; sonda `apply`
-- true-dursa COMMIT, yoxsa ROLLBACK — yəni `plan` eyni dəyişiklikləri edib geri alır.
--
-- Qayda: keçiddən (`cutoff`) sonra Tokio-da yaranan/dəyişən istifadəçi və admin datası Oracle-a
-- daşınır; Oracle-da keçiddən sonra edilmiş iş (wbw, audit təklifləri, yeni dua və s.) heç vaxt
-- üzərinə yazılmır — iki tərəfdə də dəyişən sətir «münaqişə» kimi göstərilir və toxunulmur.

-- ── 0. Qalan məzmun cədvəlləri — yalnız hesabat; dəyişikliklərdən ƏVVƏL tutuşdurulur ─────────
create temp table tok_c on commit drop as
select e ->> 0 as t, e ->> 1 as k, e ->> 2 as h, e ->> 3 as ts
  from delta_in, jsonb_array_elements(payload) e where name = 'content';

create temp table cmp_c on commit drop as
select coalesce(tk.t, o.t) as t, coalesce(tk.k, o.k) as k,
       case
           when o.k is null then 'tokio_only'
           when tk.k is null then 'oracle_only'
           when tk.h = o.h then 'same'
           when tk.ts >= :'cutoff' then 'changed_tokio'
           else 'changed_oracle'
       end as kind
  from tok_c tk
  full join pg_temp.delta_rowhashes(:'skip'::text[], (select array_agg(distinct t) from tok_c)) o
    on o.t = tk.t and o.k = tk.k;

select format('R|məzmun|%s|%s', t,
              case when count(*) filter (where kind in ('tokio_only', 'changed_tokio')) = 0
                   then format('eyni (Oracle-da %s yeni, %s dəyişmiş sətir — normaldır)',
                               count(*) filter (where kind = 'oracle_only'), count(*) filter (where kind = 'changed_oracle'))
                   else format('DİQQƏT: Tokio-da %s dəyişmiş, %s yalnız Tokio-da olan sətir (məs. %s) — avtomatik köçürülmür',
                               count(*) filter (where kind = 'changed_tokio'), count(*) filter (where kind = 'tokio_only'),
                               (select string_agg(k, ', ') from (select c2.k from cmp_c c2
                                 where c2.t = cmp_c.t and c2.kind in ('tokio_only', 'changed_tokio') order by c2.k limit 8) s))
              end)
  from cmp_c group by t order by t;

-- ── 1. Hilal elanları — Tokio həlledicidir (admin elanı mağazadakı tətbiqdən verir) ─────────────
create temp table t_lunar on commit drop as
select (jsonb_populate_record(null::public.lunar_announcement, '{"like_count": 0}'::jsonb || e)).*
  from delta_in, jsonb_array_elements(payload) e
 where name = 'lunar';
update t_lunar set media = replace(media::text, :'old_storage', :'new_storage')::jsonb where media is not null;

with d as (
    delete from public.lunar_announcement o
     where not exists (select 1 from t_lunar t where t.id = o.id)
       and (o.created_at < :'cutoff'
            or exists (select 1 from t_lunar t where t.hijri_year = o.hijri_year and t.hijri_month = o.hijri_month))
    returning o.id, o.hijri_year, o.hijri_month)
select format('R|hilal|sil|id=%s (%s-%s) — Tokio-da artıq yoxdur', id, hijri_year, hijri_month) from d;

select format('R|hilal|saxla|id=%s (%s-%s) yalnız Oracle-dadır, keçiddən sonra yaradılıb', id, hijri_year, hijri_month)
  from public.lunar_announcement o
 where not exists (select 1 from t_lunar t where t.id = o.id);

with u as (
    update public.lunar_announcement o
       set hijri_year = t.hijri_year, hijri_month = t.hijri_month, start_date = t.start_date,
           length_days = t.length_days, sighted_at = t.sighted_at, note = t.note, media = t.media,
           created_at = t.created_at, view_count = greatest(o.view_count, t.view_count)
      from t_lunar t
     where t.id = o.id
       and (o.hijri_year, o.hijri_month, o.start_date, o.length_days, o.sighted_at, o.note, o.media, o.created_at, o.view_count)
           is distinct from
           (t.hijri_year, t.hijri_month, t.start_date, t.length_days, t.sighted_at, t.note, t.media, t.created_at,
            greatest(o.view_count, t.view_count))
    returning o.id, o.hijri_year, o.hijri_month)
select format('R|hilal|yenilə|id=%s (%s-%s)', id, hijri_year, hijri_month) from u;

with i as (
    insert into public.lunar_announcement
           (id, hijri_year, hijri_month, start_date, length_days, sighted_at, note, media, view_count, like_count, created_at, updated_at)
    overriding system value
    select id, hijri_year, hijri_month, start_date, length_days, sighted_at, note, media, view_count, 0, created_at, updated_at
      from t_lunar t
     where not exists (select 1 from public.lunar_announcement o where o.id = t.id)
    returning id, hijri_year, hijri_month, start_date, jsonb_array_length(coalesce(media, '[]'::jsonb)) as n)
select format('R|hilal|əlavə|id=%s (%s-%s, başlanğıc %s, %s media)', id, hijri_year, hijri_month, start_date, n) from i;

select setval(pg_get_serial_sequence('public.lunar_announcement', 'id'),
              greatest((select max(id) from public.lunar_announcement), 1)) \g /dev/null

-- ── 2. Tərcümə mətni — yalnız Oracle-da keçiddən sonra toxunulmamış ayələr ───────────────────────
create temp table c_qtd on commit drop as
select t.*, o.updated_at as o_updated,
       case
           when o.id is null then 'yoxdur'
           when (o.text, o.note, o.text_alt, o.note_alt) is not distinct from (t.text, t.note, t.text_alt, t.note_alt) then 'eyni'
           when o.updated_at < :'cutoff' then 'tətbiq'
           else 'münaqişə'
       end as verdict
  from (select (jsonb_populate_record(null::public.quran_translations_data, e)).*
          from delta_in, jsonb_array_elements(payload) e where name = 'qtd') t
  left join public.quran_translations_data o on o.id = t.id;

select format('R|tərcümə|%s|id=%s %s %s:%s%s', verdict, id, slug, chapter_no, verse_no,
              case verdict
                  when 'münaqişə' then format(' — Oracle-da da dəyişib (%s); TOXUNULMADI, iki mətni əl ilə tutuşdur', o_updated)
                  when 'yoxdur' then ' — Oracle-da belə sətir yoxdur; TOXUNULMADI'
                  else '' end)
  from c_qtd order by verdict, id;

select format('R|tərcümə|diqqət|%s:%s üçün Oracle-da %s gözləyən təklif var — köhnə mətnə əsaslanır, paneldə yoxla',
              c.chapter_no, c.verse_no, count(*))
  from c_qtd c
  join public.quran_edits e on e.translation_id = c.id and not e.is_approved
 where c.verdict = 'tətbiq'
 group by c.chapter_no, c.verse_no;

-- `on_quran_translation_changed_wbw` trigger-i həmin ayələrin sözbəsözünü `stale` edir.
update public.quran_translations_data o
   set text = c.text, note = c.note, text_alt = c.text_alt, note_alt = c.note_alt, updated_at = c.updated_at
  from c_qtd c
 where c.id = o.id and c.verdict = 'tətbiq';

select format('R|tərcümə|wbw|%s:%s — %s söz «stale» oldu (yenidən baxılacaq)', w.chapter_no, w.verse_no, count(*))
  from public.quran_wbw_az w
  join c_qtd c on c.verdict = 'tətbiq' and c.slug = 'az' and w.chapter_no = c.chapter_no and w.verse_no = c.verse_no
 where w.status = 'stale'
 group by w.chapter_no, w.verse_no;

-- ── 3. Tərcümə düzəliş təklifləri — id-lər toqquşur, ona görə yeni id ilə ─────────────────────────
create temp table c_qe on commit drop as
select t.*,
       case
           when exists (select 1 from public.quran_edits o
                         where (o.translation_id, o.editor_email, o.new_text, o.created_at, o.is_approved)
                               is not distinct from (t.translation_id, t.editor_email, t.new_text, t.created_at, t.is_approved))
               then 'var'
           when t.created_at < :'cutoff' then 'köhnə'
           when not t.is_approved and exists (select 1 from public.quran_edits o
                                               where o.translation_id = t.translation_id and o.editor_email = t.editor_email
                                                 and not o.is_approved)
               then 'münaqişə'
           else 'köçür'
       end as verdict
  from (select (jsonb_populate_record(null::public.quran_edits, e)).*
          from delta_in, jsonb_array_elements(payload) e where name = 'qe') t;

select format('R|təklif|%s|Tokio id=%s %s:%s %s%s', verdict, id, chapter_no, verse_no,
              case when is_approved then 'təsdiqlənib' else 'gözləyir' end,
              case verdict
                  when 'köhnə' then ' — keçiddən əvvəlki sətir Oracle-da yoxdur; TOXUNULMADI'
                  when 'münaqişə' then ' — eyni redaktorun bu ayə üçün Oracle-da gözləyən təklifi var; TOXUNULMADI'
                  else '' end)
  from c_qe where verdict <> 'var' order by id;

with i as (
    insert into public.quran_edits (translation_id, chapter_no, verse_no, new_text, note, editor_email, user_id, is_approved, created_at)
    select translation_id, chapter_no, verse_no, new_text, note, editor_email, user_id, is_approved, created_at
      from c_qe where verdict = 'köçür' order by id
    returning id)
select format('R|təklif|yeni id|%s sətir əlavə olundu (id %s–%s)', count(*), min(id), max(id)) from i having count(*) > 0;

-- Tokio-da emal olunub (təsdiq/silinmə) amma Oracle-da hələ gözləyən köhnə təkliflər — yalnız hesabat.
select format('R|təklif|yoxla|Oracle id=%s %s:%s gözləyir, Tokio-da artıq yoxdur (orada emal olunub?)', o.id, o.chapter_no, o.verse_no)
  from public.quran_edits o
 where not o.is_approved and o.created_at < :'cutoff'
   and o.editor_email is distinct from 'translation-audit'
   and not exists (select 1 from c_qe t where t.id = o.id);

-- ── 4. İstək lövhəsi — səs/baxış sayı ikisinin böyüyü; məzmun fərqi yalnız hesabat ────────────────
create temp table t_sug on commit drop as
select (jsonb_populate_record(null::public.suggestions, '{"like_count": 0}'::jsonb || e)).*
  from delta_in, jsonb_array_elements(payload) e where name = 'sug';
update t_sug set media = replace(media::text, :'old_storage', :'new_storage')::jsonb where media is not null;

with u as (
    update public.suggestions o
       set vote_count = greatest(o.vote_count, t.vote_count), view_count = greatest(o.view_count, t.view_count)
      from t_sug t
     where t.id = o.id
       and (o.vote_count, o.view_count) is distinct from (greatest(o.vote_count, t.vote_count), greatest(o.view_count, t.view_count))
    returning o.id, o.vote_count, o.view_count)
select format('R|istək|say|%s sətirdə səs/baxış Tokio-dakı böyük dəyərə qaldırıldı (id: %s)', count(*), string_agg(id::text, ',' order by id))
  from u having count(*) > 0;

select format('R|istək|fərq|id=%s fərqli sahələr: %s — Oracle-dakı saxlanıldı, əl ilə bax', o.id,
              concat_ws(',',
                  case when o.status   is distinct from t.status   then 'status'   end,
                  case when o.body     is distinct from t.body     then 'body'     end,
                  case when o.category is distinct from t.category then 'category' end,
                  case when o.note     is distinct from t.note     then 'note'     end,
                  case when o.media    is distinct from t.media    then 'media'    end,
                  case when o.platform is distinct from t.platform then 'platform' end,
                  case when o.min_app_version is distinct from t.min_app_version then 'min_app_version' end))
  from t_sug t join public.suggestions o on o.id = t.id
 where (o.status, o.body, o.category, o.note, o.media, o.platform, o.min_app_version)
       is distinct from (t.status, t.body, t.category, t.note, t.media, t.platform, t.min_app_version);

-- Yeni istəklər; `source_submission_id` ↔ `suggestion_id` dövri FK olduğu üçün əvvəl null, sonra bağlanır.
with i as (
    insert into public.suggestions (id, category, body, note, media, platform, min_app_version, status,
                                    vote_count, view_count, like_count, source_submission_id, created_at, updated_at)
    overriding system value
    select id, category, body, note, media, platform, min_app_version, status,
           vote_count, view_count, 0, null, created_at, updated_at
      from t_sug t
     where t.created_at >= :'cutoff' and not exists (select 1 from public.suggestions o where o.id = t.id)
    returning id)
select format('R|istək|əlavə|id=%s', id) from i;

select format('R|istək|yoxla|id=%s Tokio-da var, Oracle-da yox (keçiddən əvvəl yaradılıb — Oracle-da silinib?)', t.id)
  from t_sug t
 where t.created_at < :'cutoff' and not exists (select 1 from public.suggestions o where o.id = t.id);

-- ── 5. İstək müraciətləri, ayə şikayətləri, loglar — yalnız yeni sətirlər ────────────────────────
create temp table t_sub on commit drop as
select (jsonb_populate_record(null::public.suggestion_submissions, e)).*
  from delta_in, jsonb_array_elements(payload) e where name = 'sub';

with i as (
    insert into public.suggestion_submissions
    overriding system value
    select * from t_sub t
     where t.created_at >= :'cutoff' and not exists (select 1 from public.suggestion_submissions o where o.id = t.id)
    returning id)
select format('R|müraciət|əlavə|id=%s', id) from i;

update public.suggestions o set source_submission_id = t.source_submission_id
  from t_sug t
 where t.id = o.id and o.source_submission_id is null and t.source_submission_id is not null
   and exists (select 1 from public.suggestion_submissions s where s.id = t.source_submission_id);

-- Mövcud müraciətin statusu Tokio-da dəyişibsə avtomatik köçürmürük: `on_suggestion_approved`
-- trigger-i Oracle-da ikinci dəfə istək yaradardı.
select format('R|müraciət|fərq|id=%s Tokio-da dəyişib (%s → %s) — əl ilə bax', o.id, o.status, t.status)
  from t_sub t join public.suggestion_submissions o on o.id = t.id
 where t.updated_at >= :'cutoff'
   and (o.status, o.admin_note, o.suggestion_id) is distinct from (t.status, t.admin_note, t.suggestion_id);

with i as (
    insert into public.verse_reports
    overriding system value
    select (jsonb_populate_record(null::public.verse_reports, e)).*
      from delta_in, jsonb_array_elements(payload) e
     where name = 'vr' and (e ->> 'created_at')::timestamptz >= :'cutoff'
       and not exists (select 1 from public.verse_reports o where o.id = (e ->> 'id')::bigint)
    returning id, verse_key)
select format('R|şikayət|əlavə|id=%s %s', id, verse_key) from i;

select format('R|şikayət|fərq|id=%s Tokio-da dəyişib — əl ilə bax', (e ->> 'id'))
  from delta_in, jsonb_array_elements(payload) e
  join public.verse_reports o on o.id = (e ->> 'id')::bigint
 where name = 'vr' and (e ->> 'updated_at')::timestamptz >= :'cutoff'
   and (o.status, o.admin_note) is distinct from (e ->> 'status', e ->> 'admin_note');

with i as (
    insert into public.app_logs (type, place, stack_trace, device_info, app_version, created_at)
    select l.type, l.place, l.stack_trace, l.device_info, l.app_version, l.created_at
      from delta_in, jsonb_array_elements(payload) e,
           lateral jsonb_populate_record(null::public.app_logs, e) l
     where name = 'logs'
       and not exists (select 1 from public.app_logs o
                        where (o.created_at, o.place, o.type) is not distinct from (l.created_at, l.place, l.type))
    returning id)
select format('R|log|əlavə|%s sətir', count(*)) from i having count(*) > 0;

-- ── 6. Hesablar və buraxılış cədvəli — yalnız hesabat ────────────────────────────────────────────
select format('R|hesab|%s|',
              case when (select payload #>> '{}' from delta_in where name = 'auth') =
                        (select md5(string_agg(id::text || md5(coalesce(encrypted_password, '')) || coalesce(email, '')
                                               || coalesce(banned_until::text, '') || coalesce(raw_app_meta_data::text, ''),
                                               ',' order by id)) from auth.users)
                   then 'eyni (parol/e-poçt/rol dəyişməyib)'
                   else 'FƏRQLİ — Tokio-da hesab əlavə olunub və ya parol dəyişib; avtomatik köçürülmür' end);

select format('R|buraxılış|%s|Tokio: latest=%s min=%s (%s); Oracle: latest=%s min=%s (%s)', t.platform,
              t.latest_version, t.min_version, t.latest_version_name, o.latest_version, o.min_version, o.latest_version_name)
  from jsonb_populate_recordset(null::public.app_releases, (select payload from delta_in where name = 'releases')) t
  left join public.app_releases o on o.platform = t.platform
 order by 1;

\if :apply
commit;
select 'R|nəticə|COMMIT|dəyişikliklər Oracle-a yazıldı';
\else
rollback;
select 'R|nəticə|ROLLBACK|bu yalnız plandır — heç nə yazılmadı';
\endif
