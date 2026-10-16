-- Azərbaycanca sözbəsöz (WBW) mənalar — 2026-10-15. Bir tranzaksiya; README.md-yə bax.
begin;

create table if not exists public.quran_wbw_az (
    chapter_no  int  not null,
    verse_no    int  not null,
    word_index  int  not null,          -- quranapp.db `ayah_words.word_index` (0-dan; ayə nömrəsi işarəsi daxil deyil)
    arabic      text not null,          -- Osmani yazı (script_id = 1), tutuşdurmaq üçün
    gloss       text not null,          -- azərbaycanca məna
    transliteration text,
    -- 'ok' — məna ayənin indiki tərcüməsinə görə yazılıb; 'stale' — tərcümə dəyişib, yenidən baxılmalıdır
    status      text not null default 'ok' check (status in ('ok', 'stale')),
    source_md5  text not null,          -- mənanın uyğunlaşdırıldığı `quran_translations_data.text` (slug az) md5-i
    updated_at  timestamptz not null default now(),
    primary key (chapter_no, verse_no, word_index)
);

alter table public.quran_wbw_az enable row level security;

drop policy if exists quran_wbw_az_select on public.quran_wbw_az;
create policy quran_wbw_az_select on public.quran_wbw_az
    for select to anon, authenticated using (true);

drop policy if exists quran_wbw_az_admin_update on public.quran_wbw_az;
create policy quran_wbw_az_admin_update on public.quran_wbw_az
    for update to authenticated
    using ((auth.jwt() ->> 'email') = 'cafarovceyxun@gmail.com')
    with check ((auth.jwt() ->> 'email') = 'cafarovceyxun@gmail.com');

revoke insert, delete, truncate on public.quran_wbw_az from anon, authenticated;
revoke update on public.quran_wbw_az from anon;

-- Tərcümə dəyişəndə (təsdiqlənmiş düzəliş və ya admin redaktəsi) həmin ayənin sözləri yenidən baxılmalıdır.
-- Yol fərq etmir: trigger əsas cədvəldədir, `quran_edits` təsdiqi də ora yazır.
create or replace function public.mark_wbw_az_stale()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
    if new.slug = 'az' and new.text is distinct from old.text then
        update public.quran_wbw_az
           set status = 'stale', updated_at = now()
         where chapter_no = new.chapter_no and verse_no = new.verse_no;
    end if;
    return new;
end;
$$;

revoke execute on function public.mark_wbw_az_stale() from public, anon, authenticated;

drop trigger if exists on_quran_translation_changed_wbw on public.quran_translations_data;
create trigger on_quran_translation_changed_wbw
    after update of text on public.quran_translations_data
    for each row execute function public.mark_wbw_az_stale();

commit;
