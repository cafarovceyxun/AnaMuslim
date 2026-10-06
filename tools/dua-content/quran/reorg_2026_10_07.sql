-- dua_qurandan_movzu_basliqlari (2026-10-07): «Qurandan dualar» içində vəziyyət və mövzu alt başlıqları.
-- Qayda (istifadəçi): Allahın istədiyi / salehlər qrupundan olan dua yeni başlığa KÖÇÜR; peyğəmbər duası öz alt
-- başlığında QALIR, yeni başlığa nüsxəsi düşür və qeydində hansı peyğəmbərin duası olduğu yazılır.
-- Mətn ötürülmür: köçürmə update, nüsxə insert … select (server mövcud sətirdən götürür).

-- 1. Dublikat qaydası alt başlıq səviyyəsinə enir: eyni çıxarış eyni alt başlıqda təkrar olmasın, fərqli alt başlıqda olar
drop index public.dua_unique_excerpt;
create unique index dua_unique_excerpt on public.dua (category_slug, coalesce(subcategory_slug, ''), md5(text_ar),
  coalesce(hadith_id, -1), coalesce(chapter_no, -1), coalesce(verse_no, -1));

-- 2. Yeni alt başlıqlar (salehlər və mələklərdən sonra; cənnət əhli və «dua haqqında» sona sürüşür)
update public.dua_subcategory set sort_no = 28 where slug = 'quran-cennet-ehli';
update public.dua_subcategory set sort_no = 29 where slug = 'quran-dua-haqqinda';
insert into public.dua_subcategory (slug, category_slug, name, sort_no) values
('quran-minik',          'qurandan-dualar', 'Minik vasitəsinə minəndə', 16),
('quran-insaallah',      'qurandan-dualar', '«İnşəallah» deməyi unudanda', 17),
('quran-quran-oxuyanda', 'qurandan-dualar', 'Quran oxumağa başlayanda', 18),
('quran-musibet',        'qurandan-dualar', 'Müsibət üz verəndə', 19),
('quran-nemete-baxanda', 'qurandan-dualar', 'Nemətə, mala baxanda', 20),
('quran-dusmen',         'qurandan-dualar', 'Düşmənlə qarşılaşanda', 21),
('quran-qirx-yas',       'qurandan-dualar', 'Qırx yaşına çatanda', 22),
('quran-ovlad',          'qurandan-dualar', 'Övlad istəyəndə', 23),
('quran-valideyn',       'qurandan-dualar', 'Valideynlər üçün', 24),
('quran-elm',            'qurandan-dualar', 'Elm istəyəndə', 25),
('quran-tovbe',          'qurandan-dualar', 'Tövbə və bağışlanma diləyəndə', 26),
('quran-sixinti',        'qurandan-dualar', 'Sıxıntı və darlıqda', 27);

-- 3. Xəritə: (əməliyyat, haradan, surə, ayə, hara, sıra) — sıra yeni alt başlıqda Quran ardıcıllığıdır
create temp table m(op text, src text, c int, v int, dst text, srt int) on commit drop;
insert into m values
('move','quran-allahin-istediyi-dualar',43,13,'quran-minik',30),
('copy','quran-nuh',11,41,'quran-minik',10),
('copy','quran-nuh',23,28,'quran-minik',20),
('move','quran-allahin-istediyi-dualar',18,23,'quran-insaallah',10),
('move','quran-allahin-istediyi-dualar',16,98,'quran-quran-oxuyanda',10),
('move','quran-salehler',2,156,'quran-musibet',10),
('move','quran-salehler',18,39,'quran-nemete-baxanda',10),
('move','quran-salehler',2,250,'quran-dusmen',10),
('move','quran-salehler',3,147,'quran-dusmen',20),
('move','quran-salehler',3,173,'quran-dusmen',30),
('move','quran-salehler',46,15,'quran-qirx-yas',10),
('copy','quran-zekeriyya',3,38,'quran-ovlad',10),
('copy','quran-zekeriyya',19,4,'quran-ovlad',20),
('copy','quran-zekeriyya',21,89,'quran-ovlad',30),
('move','quran-salehler',25,74,'quran-ovlad',40),
('copy','quran-ibrahim',37,100,'quran-ovlad',50),
('move','quran-allahin-istediyi-dualar',17,24,'quran-valideyn',10),
('move','quran-allahin-istediyi-dualar',20,114,'quran-elm',10),
('copy','quran-adem',7,23,'quran-tovbe',10),
('copy','quran-musa',7,143,'quran-tovbe',20),
('copy','quran-musa',7,151,'quran-tovbe',30),
('move','quran-allahin-istediyi-dualar',23,118,'quran-tovbe',40),
('copy','quran-musa',28,16,'quran-tovbe',50),
('move','quran-allahin-istediyi-dualar',47,19,'quran-tovbe',60),
('move','quran-allahin-istediyi-dualar',110,3,'quran-tovbe',70),
('copy','quran-eyyub',21,83,'quran-sixinti',10),
('copy','quran-yunus',21,87,'quran-sixinti',20),
('copy','quran-musa',28,24,'quran-sixinti',30),
('copy','quran-eyyub',38,41,'quran-sixinti',40),
('copy','quran-nuh',54,10,'quran-sixinti',50);

-- Hər xəritə sətri dəqiq bir baş sətrə düşməlidir (yoxsa heç nə dəyişmədən dayan)
do $$
declare bad int;
begin
  select count(*) into bad from m where (select count(*) from public.dua d
    where d.category_slug = 'qurandan-dualar' and d.subcategory_slug = m.src and d.chapter_no = m.c
      and d.verse_no = m.v and d.part_of_id is null) <> 1;
  if bad > 0 then raise exception 'xəritə uyğunsuzluğu: % sətir', bad; end if;
end $$;

-- 4. Nüsxələr (peyğəmbər duaları): qeyd = «X əleyhissəlamın duası» + mövcud qeyd
insert into public.dua (category_slug, subcategory_slug, source_type, hadith_id, chapter_no, verse_no, verse_end,
                        text_ar, text_az, transliteration, note, source, repeat_count, sort_no)
select d.category_slug, m.dst, d.source_type, d.hadith_id, d.chapter_no, d.verse_no, d.verse_end,
       d.text_ar, d.text_az, d.transliteration,
       replace(sc.name, 'duaları', 'duası') || coalesce('. ' || d.note, ''),
       d.source, d.repeat_count, m.srt
from m
join public.dua d on d.category_slug = 'qurandan-dualar' and d.subcategory_slug = m.src and d.chapter_no = m.c
                 and d.verse_no = m.v and d.part_of_id is null
join public.dua_subcategory sc on sc.slug = m.src
where m.op = 'copy';

-- 5. Köçürmələr
update public.dua d set subcategory_slug = m.dst, sort_no = m.srt
from m
where m.op = 'move' and d.category_slug = 'qurandan-dualar' and d.subcategory_slug = m.src
  and d.chapter_no = m.c and d.verse_no = m.v and d.part_of_id is null;
