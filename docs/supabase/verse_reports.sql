-- verse_reports
--
-- Ayə ilə bağlı səhv bildirişləri. Adi (anonim) istifadəçi yaza bilir,
-- yalnız daxil olmuş (auth) istifadəçilər oxuya/idarə edə bilir.
--
-- Supabase SQL Editor-də bir dəfə işlədin.

create table if not exists public.verse_reports (
    id           bigint generated always as identity primary key,
    chapter_no   int    not null,
    verse_no     int    not null,
    verse_key    text,                       -- "Fatihə surəsi 1:1" (admin üçün oxunaqlı)
    message      text   not null,
    slugs        text,                       -- ayənin göstərildiyi tərcümə slug-ları, vergüllə
    app_version  text,
    status       text   not null default 'pending',   -- pending | reviewing | resolved | rejected
    admin_note   text,
    user_id      uuid   references auth.users (id) on delete set null,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now(),

    constraint verse_reports_message_len check (char_length(btrim(message)) between 3 and 2000),
    constraint verse_reports_status_valid check (status in ('pending', 'reviewing', 'resolved', 'rejected'))
);

create index if not exists verse_reports_created_at_idx on public.verse_reports (created_at desc);
create index if not exists verse_reports_status_idx     on public.verse_reports (status);

-- updated_at avtomatik yenilənsin
create or replace function public.set_verse_reports_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists verse_reports_set_updated_at on public.verse_reports;
create trigger verse_reports_set_updated_at
    before update on public.verse_reports
    for each row execute function public.set_verse_reports_updated_at();

-- RLS
alter table public.verse_reports enable row level security;

-- 1) Yazmaq: hamıya açıq (anon + authenticated).
--    status/admin_note klientdən gəlsə belə default-a düşsün deyə with check onları da yoxlayır.
drop policy if exists "verse_reports_insert_anyone" on public.verse_reports;
create policy "verse_reports_insert_anyone"
    on public.verse_reports
    for insert
    to anon, authenticated
    with check (
        status = 'pending'
        and admin_note is null
        and (user_id is null or user_id = auth.uid())
    );

-- 2) Oxumaq: yalnız daxil olmuş istifadəçilər.
drop policy if exists "verse_reports_select_authenticated" on public.verse_reports;
create policy "verse_reports_select_authenticated"
    on public.verse_reports
    for select
    to authenticated
    using (true);

-- 3) Status dəyişmək: yalnız daxil olmuş istifadəçilər.
drop policy if exists "verse_reports_update_authenticated" on public.verse_reports;
create policy "verse_reports_update_authenticated"
    on public.verse_reports
    for update
    to authenticated
    using (true)
    with check (true);

-- 4) Silmək: yalnız daxil olmuş istifadəçilər.
drop policy if exists "verse_reports_delete_authenticated" on public.verse_reports;
create policy "verse_reports_delete_authenticated"
    on public.verse_reports
    for delete
    to authenticated
    using (true);

-- anon rolu insert-dən başqa heç nə edə bilməsin (RLS onsuz da bağlayır,
-- bu isə qrant səviyyəsində ikinci qapı).
revoke all on public.verse_reports from anon;
grant insert on public.verse_reports to anon;
grant select, insert, update, delete on public.verse_reports to authenticated;
