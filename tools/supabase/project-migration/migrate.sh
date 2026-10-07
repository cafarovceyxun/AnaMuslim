#!/bin/bash
# AnaMuslim: anamuslim (Tokio) → cafarovceyxun (Frankfurt) baza köçürməsi.
#
#   migrate.sh dump      — köhnədən public (sxem+data) və auth.users/identities dump-ı
#   migrate.sh restore   — yeniyə bərpa (yalnız boş bazaya)
#   migrate.sh verify    — iki bazanın struktur/icazə/data barmaq izlərini tutuşdurur
#   migrate.sh resync    — son keçiddən əvvəl: yenidəki public datanı + hesabları köhnədən yenidən
#                          yükləyir (bir tranzaksiya; sxem və icazələrə toxunmur)
#
# Bağlantılar ~/.anamuslim-migrate.urls-dən (build_urls.py yazır); bütün çıxış maskalanır.
set -euo pipefail

# Dump-larda hesabların parol heşləri var — repoya DÜŞMƏMƏLİDİR, ona görə iş qovluğu kənardadır.
WORK="${MIGRATE_WORK:-${TMPDIR:-/tmp}/anamuslim-migrate}"
mkdir -p "$WORK" && chmod 700 "$WORK"
: "${PGBIN:?PGBIN verilməyib — pg_dump/psql 17 qovluğu, bax README.md}"
export PATH="$PGBIN:$PATH"
# shellcheck disable=SC1090
source "$HOME/.anamuslim-migrate.urls"   # build_urls.py yazır (parollar kodlanmış)
: "${OLD_DB_URL:?}" "${NEW_DB_URL:?}"

# psql/pg_dump xətası URI-ni (və parolu) mətnə qata bilər — bütün çıxışı maskalayırıq.
mask() {
  python3 -u -c '
import os, re, sys
secrets = []
for k in ("OLD_DB_URL", "NEW_DB_URL", "NEW_ADMIN_URL"):
    u = os.environ.get(k, "")
    if u:
        secrets.append(u)
        m = re.match(r"[^:]+://[^:]+:(.*)@[^@]*$", u)
        if m and len(m.group(1)) >= 4:
            secrets.append(m.group(1))
for line in sys.stdin:
    for s in sorted(secrets, key=len, reverse=True):
        line = line.replace(s, "***")
    sys.stdout.write(line)
'
}
export OLD_DB_URL NEW_DB_URL NEW_ADMIN_URL
exec > >(mask) 2> >(mask >&2)

psql_old() { psql "$OLD_DB_URL" -X -v ON_ERROR_STOP=1 -q "$@"; }
psql_new() { psql "$NEW_DB_URL" -X -v ON_ERROR_STOP=1 -q "$@"; }

# Öz serverimizdə (self-hosted) `postgres` superuser deyil və `session_replication_role`-u dəyişə bilmir
# (Supabase-in öz serverlərində supautils buna icazə verir). Onda superuser ilə qoşulub `--role=postgres`
# işlədirik — obyektlərin sahibi yenə `postgres` olur.
RESTORE_URL="${NEW_ADMIN_URL:-$NEW_DB_URL}"
RESTORE_ROLE=()
[ -n "${NEW_ADMIN_URL:-}" ] && RESTORE_ROLE=(--role=postgres)

# Supabase-in defolt icazələri yeni cədvəl/funksiyaya anon-a TAM hüquq verir, pg_dump isə onları
# geri almır (ACL-i acldefault-a görə yazır) — məsələn backup_table_json anon-a açılardı.
# Bərpa vaxtı defoltları söndürürük ki, icazələr köhnədəki ilə BİR-BİR eyni olsun, sonra
# defoltları köhnə vəziyyətə qaytarırıq (verify bunu da tutuşdurur).
DEFAULTS_OFF="
alter default privileges for role postgres in schema public revoke all on tables    from anon, authenticated, service_role;
alter default privileges for role postgres in schema public revoke all on sequences from anon, authenticated, service_role;
alter default privileges for role postgres in schema public revoke all on functions from anon, authenticated, service_role;"
DEFAULTS_ON="
alter default privileges for role postgres in schema public grant all on tables    to anon, authenticated, service_role;
alter default privileges for role postgres in schema public grant all on sequences to anon, authenticated, service_role;
alter default privileges for role postgres in schema public grant all on functions to anon, authenticated, service_role;"

cmd_dump() {
  pg_dump "$OLD_DB_URL" -Fc --schema=public --no-publications --no-subscriptions -f "$WORK/public.dump"
  pg_dump "$OLD_DB_URL" -Fc --data-only --table=auth.users --table=auth.identities -f "$WORK/auth.dump"
  # public sxeminin özü, rls_auto_enable (ensure_rls event trigger-i ona bağlıdır) və defolt
  # icazələr (DEFAULT ACL — supabase_admin-inkini postgres dəyişə bilmir) yenidə artıq eynidir —
  # onların TOC sətirlərini çıxarırıq.
  pg_restore -l "$WORK/public.dump" | grep -v -E 'SCHEMA (- )?public|rls_auto_enable|DEFAULT ACL' > "$WORK/public.toc"
  echo "TOC: $(grep -c -v '^;' "$WORK/public.toc") element"
  ls -la "$WORK"/*.dump
}

cmd_restore() {
  local n
  n=$(psql_new -At -c "select count(*) from pg_class where relnamespace='public'::regnamespace and relkind='r'")
  [ "$n" = "0" ] || { echo "Yeni bazada artıq $n cədvəl var — restore yalnız boş bazaya"; exit 1; }

  if [ "$(psql_new -At -c 'select count(*) from auth.users')" = "0" ]; then
    echo "→ auth (hesablar)"
    PGOPTIONS='-c session_replication_role=replica' \
      pg_restore -d "$RESTORE_URL" "${RESTORE_ROLE[@]}" --data-only --no-owner --single-transaction --exit-on-error "$WORK/auth.dump"
  else
    echo "→ auth artıq köçürülüb, keçirəm"
  fi

  echo "→ public (sxem + data); defolt icazələr müvəqqəti söndürülür"
  psql_new -c "$DEFAULTS_OFF"
  trap 'psql_new -c "$DEFAULTS_ON"' EXIT
  pg_restore -d "$RESTORE_URL" "${RESTORE_ROLE[@]}" --no-owner --single-transaction --exit-on-error -L "$WORK/public.toc" "$WORK/public.dump"
  psql_new -c "$DEFAULTS_ON"
  trap - EXIT

  echo "→ storage bucket-ləri və siyasətləri (db-backups köçürülmür)"
  psql_old -At -c "
    select format('insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types) values (%L, %L, %L, %s, %L);',
                  id, name, public, coalesce(file_size_limit::text, 'null'), allowed_mime_types)
      from storage.buckets where id <> 'db-backups'
    union all
    select format('create policy %I on storage.objects as %s for %s to %s%s%s;',
                  policyname, permissive, cmd, array_to_string(roles, ', '),
                  coalesce(' using (' || qual || ')', ''), coalesce(' with check (' || with_check || ')', ''))
      from pg_policies where schemaname = 'storage' and tablename = 'objects';" \
    > "$WORK/storage.sql"
  psql_new -1 -f "$WORK/storage.sql"

  echo "→ Vault: backup_trigger_secret"
  local secret
  secret=$(grep -E '^BACKUP_SECRET=' "$HOME/.anamuslim-backup.env" | cut -d= -f2- | tr -d "\"'")
  [ -n "$secret" ] || { echo "BACKUP_SECRET tapılmadı"; exit 1; }
  # psql dəyişəni yalnız skriptdə açılır, -c sorğusunda yox — ona görə stdin.
  psql_new -v s="$secret" >/dev/null <<'SQL'
select vault.create_secret(:'s', 'backup_trigger_secret', 'db-backup Edge Function sirri');
SQL

  psql_new -c "notify pgrst, 'reload schema';"
  echo "restore bitdi"
}

cat > "$WORK/fingerprint.sql" <<'SQL'
set timezone = 'UTC';
set datestyle = 'ISO, YMD';
set extra_float_digits = 1;
\echo == relations (acl, rls, options)
select relname, relkind, relacl::text, relrowsecurity, relforcerowsecurity, reloptions::text
  from pg_class where relnamespace = 'public'::regnamespace and relkind in ('r','v','m','S','p') order by 1;
\echo == default acl (public)
select defaclobjtype, defaclacl::text from pg_default_acl
 where defaclnamespace = 'public'::regnamespace and defaclrole = 'postgres'::regrole order by 1;
\echo == functions
select proname, pg_get_function_identity_arguments(oid), proacl::text, prosecdef, proconfig::text, md5(prosrc)
  from pg_proc where pronamespace = 'public'::regnamespace order by 1, 2;
\echo == policies
select schemaname, tablename, policyname, permissive, roles::text, cmd, qual, with_check
  from pg_policies where schemaname in ('public','storage') order by 1, 2, 3;
\echo == triggers
select pg_get_triggerdef(oid) from pg_trigger
 where tgrelid in (select oid from pg_class where relnamespace = 'public'::regnamespace) and not tgisinternal order by 1;
\echo == constraints
select conrelid::regclass::text, conname, pg_get_constraintdef(oid) from pg_constraint
 where connamespace = 'public'::regnamespace order by 1, 2;
\echo == indexes
select indexdef from pg_indexes where schemaname = 'public' order by 1;
\echo == views
select viewname, md5(definition) from pg_views where schemaname = 'public' order by 1;
\echo == buckets
select id, public, file_size_limit, allowed_mime_types::text from storage.buckets where id <> 'db-backups' order by 1;
\echo == vault
select name from vault.secrets order by 1;
\echo == auth
select count(*), md5(string_agg(id::text || coalesce(email,'') || coalesce(encrypted_password,''), ',' order by id)) from auth.users;
select count(*), md5(string_agg(id::text || user_id::text || provider, ',' order by id)) from auth.identities;
\echo == data (sətir sayı + md5)
select format('select %L as t, count(*) as n, md5(coalesce(string_agg(x::text, %L order by x::text), %L)) from public.%I x;', relname, '|', '', relname)
  from pg_class where relnamespace = 'public'::regnamespace and relkind = 'r' order by relname \gexec
\echo == sequences
select format('select %L, last_value, is_called from public.%I;', relname, relname)
  from pg_class where relnamespace = 'public'::regnamespace and relkind = 'S' order by relname \gexec
SQL

cmd_verify() {
  psql_old -At -F ' ¦ ' -f "$WORK/fingerprint.sql" > "$WORK/fp_old.txt"
  psql_new -At -F ' ¦ ' -f "$WORK/fingerprint.sql" > "$WORK/fp_new.txt"
  wc -l "$WORK/fp_old.txt" "$WORK/fp_new.txt"
  if diff "$WORK/fp_old.txt" "$WORK/fp_new.txt" > "$WORK/fp.diff"; then
    echo "EYNİDİR: struktur, icazələr, siyasətlər, hesablar və bütün cədvəllərin datası"
  else
    echo "FƏRQ VAR ($(grep -c '^[<>]' "$WORK/fp.diff") sətir):"; cat "$WORK/fp.diff"
  fi
}

cmd_resync() {
  echo "→ köhnədən təzə data dump-ı"
  pg_dump "$OLD_DB_URL" -Fc --data-only --schema=public -f "$WORK/public_data.dump"
  pg_dump "$OLD_DB_URL" -Fc --data-only --table=auth.users --table=auth.identities -f "$WORK/auth.dump"
  pg_restore --data-only --no-owner -f "$WORK/auth_data.sql" "$WORK/auth.dump"
  pg_restore --data-only --no-owner -f "$WORK/public_data.sql" "$WORK/public_data.dump"
  local tables
  tables=$(psql_new -At -c "select string_agg(format('public.%I', relname), ', ') from pg_class where relnamespace='public'::regnamespace and relkind='r'")
  cat > "$WORK/resync.sql" <<EOF
set session_replication_role = replica;
truncate $tables, auth.identities, auth.users cascade;
\\i $WORK/auth_data.sql
\\i $WORK/public_data.sql
EOF
  echo "→ yenidə data dəyişdirilir (bir tranzaksiya — xəta olsa heç nə dəyişmir)"
  # Self-host-da `postgres` session_replication_role-u dəyişə bilmir — superuser ilə (RESTORE_URL).
  psql "$RESTORE_URL" -X -v ON_ERROR_STOP=1 -q -1 -f "$WORK/resync.sql" > /dev/null
  echo "resync bitdi — indi verify işlət"
}

case "${1:-}" in
  dump) cmd_dump ;;
  restore) cmd_restore ;;
  verify) cmd_verify ;;
  resync) cmd_resync ;;
  *) echo "istifadə: $0 dump|restore|verify|resync"; exit 2 ;;
esac
