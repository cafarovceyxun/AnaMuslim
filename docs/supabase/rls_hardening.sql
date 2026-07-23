-- RLS hardening
--
-- Repo public olmazdan əvvəl işlədin. Anon açar (SupabaseProvider.kt)
-- publikdir və hər APK-da var; ona görə bazanın təhlükəsizliyi tamamilə
-- RLS + GRANT-lardan asılıdır.
--
-- Aşkarlanan problemlər (2026-07-23):
--   1) hadith_volume/book/chapter/sub_chapter — "Allow all for anon" (ALL to anon)
--      => anon bütün hədis strukturunu insert/update/delete edə bilirdi.
--   2) daily_content — "Authorized Write" policy-si `public` roluna ALL verirdi
--      => anon günün məzmununu yaza/silə bilirdi.
--   3) Bütün cədvəllərdə anon-a lazımsız table-level GRANT-lar (UPDATE/DELETE/…).
--
-- Prinsip: anon YALNIZ oxuya bilər (məzmun cədvəlləri) və YALNIZ insert edə bilər
-- (app_logs, verse_reports). Bütün yazma/idarəetmə `authenticated` (admin) olsun.
--
-- Supabase SQL Editor-də bir dəfə işlədin. Idempotentdir.

begin;

-- =========================================================================
-- 1) Hədis struktur cədvəlləri: anon-un yazma deşiyini bağla
-- =========================================================================
drop policy if exists "Allow all for anon" on public.hadith_volume;
drop policy if exists "Allow all for anon" on public.hadith_book;
drop policy if exists "Allow all for anon" on public.hadith_chapter;
drop policy if exists "Allow all for anon" on public.hadith_sub_chapter;
-- (Qalan policy-lər saxlanır: "Allow read for everyone" SELECT anon/auth,
--  "Owner can insert ... directly" INSERT auth, "Allow update for admins only" UPDATE auth.)

-- =========================================================================
-- 2) daily_content: yazmanı yalnız authenticated-ə bağla
-- =========================================================================
drop policy if exists "Authorized Write" on public.daily_content;
create policy "Authorized Write"
    on public.daily_content
    for all
    to authenticated
    using (true)
    with check (true);
-- "Public Read" (SELECT public) saxlanır.

-- =========================================================================
-- 2b) app_logs: admin (authenticated) logları oxusun və silsin; anon YOX.
--     (AppLogsScreen fetchLogs = SELECT, clearRemoteLogs = DELETE.)
-- =========================================================================
drop policy if exists "app_logs_select_authenticated" on public.app_logs;
create policy "app_logs_select_authenticated"
    on public.app_logs
    for select
    to authenticated
    using (true);

drop policy if exists "app_logs_delete_authenticated" on public.app_logs;
create policy "app_logs_delete_authenticated"
    on public.app_logs
    for delete
    to authenticated
    using (true);
-- "Allow anonymous insert" (INSERT public) saxlanır ki, tətbiq log göndərə bilsin.
grant select, delete on public.app_logs to authenticated;

-- =========================================================================
-- 3) Table-level GRANT-ları minimuma endir (defense-in-depth)
--    anon: məzmun cədvəllərində yalnız SELECT; log/report cədvəllərində yalnız INSERT.
-- =========================================================================

-- Yalnız-oxu cədvəlləri
revoke all on public.quran_translations_data from anon;
grant  select on public.quran_translations_data to anon;

revoke all on public.hadith            from anon;
grant  select on public.hadith         to anon;
revoke all on public.hadith_volume     from anon;
grant  select on public.hadith_volume  to anon;
revoke all on public.hadith_book       from anon;
grant  select on public.hadith_book    to anon;
revoke all on public.hadith_chapter    from anon;
grant  select on public.hadith_chapter to anon;
revoke all on public.hadith_sub_chapter from anon;
grant  select on public.hadith_sub_chapter to anon;

revoke all on public.daily_content     from anon;
grant  select on public.daily_content  to anon;

revoke all on public.resource_updates    from anon;
grant  select on public.resource_updates to anon;

-- Yalnız-insert cədvəlləri (anon oxuya bilməməlidir)
revoke all on public.app_logs   from anon;
grant  insert on public.app_logs to anon;

revoke all on public.verse_reports   from anon;
grant  insert on public.verse_reports to anon;

-- anon-un tamamilə əli olmamalı cədvəllər (PII / admin)
revoke all on public.quran_edits            from anon;
revoke all on public.hadith_edits           from anon;
revoke all on public.resource_updates_admin from anon;

commit;

-- =========================================================================
-- Yoxlama: bunu işlədib nəticəyə bax — anon artıq yalnız gözlənilən icazələrə
-- malik olmalı, ALL(anon)/ALL(public) yazma policy-si qalmamalıdır.
-- =========================================================================
-- select c.relname as table_name,
--        c.relrowsecurity as rls_on,
--        (select string_agg(privilege_type, ',' order by privilege_type)
--         from information_schema.role_table_grants g
--         where g.table_schema='public' and g.table_name=c.relname and g.grantee='anon') as anon_grants,
--        (select string_agg(policyname||':'||cmd||'('||array_to_string(roles,'/')||')', '  |  ')
--         from pg_policies p where p.schemaname='public' and p.tablename=c.relname) as policies
-- from pg_class c
-- where c.relnamespace='public'::regnamespace and c.relkind='r'
-- order by c.relname;
