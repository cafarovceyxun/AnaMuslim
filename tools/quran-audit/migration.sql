-- translation_review_2026_10_14
-- Tərcümə yoxlanışının təklifləri `quran_edits`-də (editor_email = 'translation-audit') gözləyən
-- düzəliş kimi saxlanılır və giriş etmiş istifadəçilərə ikinci tərcümə kimi göstərilir
-- (`az-review` kitabı → `translations.text_review` / `note_review`).
--
-- ⚠️ Bütün fayl bir tranzaksiyadır. `create or replace view` köhnə sütunlar eyni ad/tip/sırada
-- qalmasa xəta verir — yəni view-un indiki tərifi SCHEMA.md-dəkindən fərqlidirsə heç nə dəyişmir.
-- Tətbiqdən ƏVVƏL `select pg_get_viewdef('public.translations', true);` ilə tutuşdur.

begin;

-- 1. Təklifin səbəbi. Təsdiq trigger-i (`apply_quran_approved_edit`) bu sütunu köçürmür — yalnız
--    paneldə və ikinci tərcümənin qeydində göstərilir.
alter table public.quran_edits add column if not exists reason text;

-- 2. Yoxlanış təkliflərini giriş etmiş hər kəs oxuya bilər (adi redaktor yalnız özününkünü görür).
--    Yazma/təsdiq/silmə siyasətləri dəyişmir — admin-only qalır.
drop policy if exists quran_edits_select_review on public.quran_edits;
create policy quran_edits_select_review on public.quran_edits
  for select to authenticated
  using (editor_email = 'translation-audit');

-- 3. Kataloqda yeni sütun növü.
do $$
declare c text;
begin
  for c in select conname from pg_constraint
            where conrelid = 'public.quran_translation_books'::regclass and contype = 'c'
              and pg_get_constraintdef(oid) like '%source_column%'
  loop
    execute format('alter table public.quran_translation_books drop constraint %I', c);
  end loop;
end $$;
alter table public.quran_translation_books
  add constraint quran_translation_books_source_column_check
  check (source_column in ('text', 'text_alt', 'text_review'));

-- 4. View-a iki sütun SONDA əlavə olunur (`drop` yox — instead-of trigger view-un üzərindədir).
create or replace view public.translations with (security_invoker = true) as
select qt.id,
       coalesce((select qe.new_text from public.quran_edits qe
                  where qe.translation_id = qt.id
                    and qe.editor_email = (auth.jwt() ->> 'email')
                    and qe.is_approved = false
                  order by qe.created_at desc limit 1), qt.text) as text,
       qt.chapter_no, qt.verse_no, qt.slug,
       coalesce((select qe.note from public.quran_edits qe
                  where qe.translation_id = qt.id
                    and qe.editor_email = (auth.jwt() ->> 'email')
                    and qe.is_approved = false
                  order by qe.created_at desc limit 1), qt.note) as note,
       qt.updated_at,
       qt.text_alt,
       qt.note_alt,
       (select qe.new_text from public.quran_edits qe
         where qe.translation_id = qt.id
           and qe.editor_email = 'translation-audit'
           and qe.is_approved = false
         order by qe.created_at desc limit 1) as text_review,
       (select qe.reason from public.quran_edits qe
         where qe.translation_id = qt.id
           and qe.editor_email = 'translation-audit'
           and qe.is_approved = false
         order by qe.created_at desc limit 1) as note_review
  from public.quran_translations_data qt;

-- Moderasiya divarı yerindədirmi?
do $$
begin
  if not exists (select 1 from pg_trigger
                  where tgrelid = 'public.translations'::regclass
                    and tgname = 'check_quran_before_update') then
    raise exception 'check_quran_before_update trigger-i yoxdur — dayandırıldı';
  end if;
end $$;

-- 5. Kitab — yalnız giriş etmiş istifadəçiyə görünür (is_public = false).
insert into public.quran_translation_books
       (slug, source_column, book_name, author_name, lang_code, lang_name, is_public)
values ('az-review', 'text_review', 'Yoxlanış təklifləri',
        'Tərcümə yoxlanışı, 2026-10-14 — təsdiqlənməmiş', 'az', 'Azərbaycan', false)
on conflict (slug) do nothing;

commit;
