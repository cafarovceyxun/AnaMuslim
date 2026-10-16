-- delta.sh → Tokio (köhnə layihə). Yalnız OXUYUR: hər paket bir CSV sətridir (ad, jsonb).
-- `cutoff`, `skip` dəyişənlərini delta.sh verir.
copy (
              select 'lunar',    coalesce((select jsonb_agg(to_jsonb(x) order by x.id) from public.lunar_announcement x), '[]')
    union all select 'qtd',      coalesce((select jsonb_agg(to_jsonb(x) order by x.id) from public.quran_translations_data x
                                            where x.updated_at >= :'cutoff'), '[]')
    union all select 'qe',       coalesce((select jsonb_agg(to_jsonb(x) order by x.id) from public.quran_edits x), '[]')
    union all select 'sug',      coalesce((select jsonb_agg(to_jsonb(x) order by x.id) from public.suggestions x), '[]')
    union all select 'sub',      coalesce((select jsonb_agg(to_jsonb(x) order by x.id) from public.suggestion_submissions x), '[]')
    union all select 'vr',       coalesce((select jsonb_agg(to_jsonb(x) order by x.id) from public.verse_reports x), '[]')
    union all select 'logs',     coalesce((select jsonb_agg(to_jsonb(x) order by x.id) from public.app_logs x
                                            where x.created_at >= :'cutoff'), '[]')
    union all select 'releases', coalesce((select jsonb_agg(to_jsonb(x)) from public.app_releases x), '[]')
    union all select 'content',  coalesce((select jsonb_agg(jsonb_build_array(t, k, h, ts))
                                             from pg_temp.delta_rowhashes(:'skip'::text[], null)), '[]')
    -- hesablar: parol heşinin heşi + e-poçt + ban + rollar (heç nə açıq göndərilmir)
    union all select 'auth',     to_jsonb((select md5(string_agg(id::text || md5(coalesce(encrypted_password, '')) || coalesce(email, '')
                                                                 || coalesce(banned_until::text, '') || coalesce(raw_app_meta_data::text, ''),
                                                                 ',' order by id)) from auth.users))
) to stdout with (format csv);
