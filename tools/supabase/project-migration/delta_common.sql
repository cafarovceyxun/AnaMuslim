-- delta.sh: hər iki tərəfdə (Tokio və Oracle) eyni işləyən sətir barmaq izi.
-- Hər sətir üçün: cədvəl, əsas açar, məzmun heşi, dəyişmə vaxtı. Heşə sayğaclar (`view_count`,
-- `like_count`, `vote_count`), `updated_at` və yalnız Oracle-da olan `reason` daxil deyil — onlar
-- məzmun fərqi deyil. `only_tables` null-dursa `skip`-dən başqa bütün public cədvəllər.
create function pg_temp.delta_rowhashes(skip text[], only_tables text[])
returns table (t text, k text, h text, ts text)
language plpgsql as $fn$
declare
    r  record;
    pk text;
begin
    for r in
        select c.relname::text as rel
          from pg_class c
         where c.relnamespace = 'public'::regnamespace and c.relkind = 'r'
           and not (c.relname::text = any (skip))
           and (only_tables is null or c.relname::text = any (only_tables))
         order by 1
    loop
        select string_agg(format('coalesce(x.%I::text, '''')', a.attname), ' || ''/'' || ' order by a.attnum)
          into pk
          from pg_index i
          join pg_attribute a on a.attrelid = i.indrelid and a.attnum = any (i.indkey)
         where i.indrelid = format('public.%I', r.rel)::regclass and i.indisprimary;
        return query execute format(
            $q$select %L::text, %s,
                      md5((to_jsonb(x) - 'updated_at' - 'view_count' - 'like_count' - 'vote_count' - 'reason')::text),
                      coalesce(to_jsonb(x) ->> 'updated_at', to_jsonb(x) ->> 'created_at', '')
                 from public.%I x$q$,
            r.rel, coalesce(pk, 'md5(to_jsonb(x)::text)'), r.rel);
    end loop;
end
$fn$;
