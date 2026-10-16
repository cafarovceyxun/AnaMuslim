#!/bin/bash
# AnaMuslim keçid günü: Tokio (köhnə, mağazadakı tətbiq) → Oracle (yeni server) DELTA köçürməsi.
#
#   delta.sh plan    — Tokio-nu oxuyur, Oracle-da bütün dəyişiklikləri edir və GERİ ALIR; hesabat çap edir
#   delta.sh apply   — əvvəl serverdə təzə yedək, sonra media faylları, sonra eyni dəyişikliklər COMMIT ilə
#
# `migrate.sh resync`-in əvəzidir: resync Oracle-ı Tokio-nun nüsxəsi ilə əvəz edirdi, keçiddən sonra isə
# Oracle-da öz iş var (wbw, audit təklifləri, yeni dua...). Burada yalnız Tokio-da keçiddən sonra
# yaranan/dəyişən sətirlər daşınır, iki tərəfdə dəyişənlərə toxunulmur — qaydalar delta_merge.sql-dədir.
# Təkrar işlətmək təhlükəsizdir: ikinci `apply` heç nə dəyişmir.
#
# Tələb: ~/.anamuslim-migrate.env-də Tokio-nun CARİ parolu (OLD_DB_PASSWORD=..., dırnaqsız) və
# Oracle-a SSH açarı. Mac-də psql lazım deyil — hər iki sorğu serverdəki supabase-db konteynerinin
# psql 17-si ilə gedir. Heç bir parol/açar çap olunmur.
set -euo pipefail

MODE="${1:-}"
case "$MODE" in plan) APPLY=false ;; apply) APPLY=true ;; *) echo "istifadə: $0 plan|apply"; exit 2 ;; esac

HERE="$(cd "$(dirname "$0")" && pwd)"
WORK="${MIGRATE_WORK:-${TMPDIR:-/tmp}/anamuslim-migrate}/delta"
mkdir -p "$WORK/media" && chmod 700 "$WORK" "$(dirname "$WORK")"

CUTOFF='2026-10-07 00:00:00+00'   # Tokio → Oracle köçürməsinin günü
OLD_STORAGE='https://molyqwcaynvsdmixtcbc.supabase.co/storage/v1/object/public/'
NEW_BASE='https://anamuslim.cafarovceyxun.com'
NEW_STORAGE="$NEW_BASE/storage/v1/object/public/"
# Ayrıca qaydası olan, ya da heç vaxt tutuşdurulmayan cədvəllər (məzmun hesabatından kənar).
SKIP='{lunar_announcement,quran_translations_data,quran_edits,suggestions,suggestion_submissions,verse_reports,app_logs,app_releases,backend_switch,backup_sync_status}'
SSH=(ssh -o BatchMode=yes -o ConnectTimeout=15 -i "$HOME/.ssh/oracle_cafarovceyxun" ubuntu@130.61.171.105)
ORACLE_ENV="$HOME/.anamuslim-oracle-supabase.env"

# Tokio URL-i ~/.anamuslim-migrate.env-dəki XAM paroldan qurulur (build_urls.py/psql lazım deyil).
OLD_DB_URL=$(python3 - "$HOME/.anamuslim-migrate.env" <<'PY'
import sys, urllib.parse
pw = None
for line in open(sys.argv[1], encoding="utf-8"):
    k, _, v = line.rstrip("\r\n").partition("=")
    if k.strip() == "OLD_DB_PASSWORD":
        pw = v
if not pw:
    sys.exit("~/.anamuslim-migrate.env-də OLD_DB_PASSWORD yoxdur")
print("postgresql://postgres.molyqwcaynvsdmixtcbc:" + urllib.parse.quote(pw, safe="")
      + "@aws-1-ap-northeast-1.pooler.supabase.com:5432/postgres?sslmode=require")
PY
)

# Tokio xətasını yalnız kateqoriya kimi göstər: libpq mətni URI-ni/parolu qata bilər.
fetch_error() {
  local err="$1"
  if grep -q 'password authentication failed' "$err"; then
    echo "Tokio parolu səhvdir — ~/.anamuslim-migrate.env-ə CARİ parolu yaz və build_urls.py işlət"
  elif grep -q 'Tenant or user not found' "$err"; then
    echo "Tokio pooler hostu/istifadəçi tapılmadı — build_urls.py-ni yenidən işlət"
  elif grep -qE 'translate host name|timed out|timeout|Connection refused' "$err"; then
    echo "Tokio-ya qoşulmaq olmadı (şəbəkə) — layihə pauzadadır? Supabase panelində bax"
  elif grep -q 'ERROR:' "$err"; then
    echo "Tokio sorğu xətası:"; grep -oE 'ERROR: [^"]{0,160}' "$err" | head -3
  else
    echo "Tokio-dan oxumaq alınmadı (naməlum xəta — mətn gizlədilib)"
  fi
}

# ── 1. Tokio-dan paketlər ────────────────────────────────────────────────────────────────────────
fetch() {
  echo "→ Tokio oxunur"
  # İlk sətir URL-dir (konteynerdə `read` götürür), qalanı psql-in skriptidir. URL heç yerdə çap olunmur.
  if ! { printf '%s\n' "$OLD_DB_URL"
         printf '\\set ON_ERROR_STOP 1\n\\set cutoff %s\n\\set skip %s\nset timezone = %s;\n' \
                "'$CUTOFF'" "'$SKIP'" "'UTC'"
         cat "$HERE/delta_common.sql" "$HERE/delta_fetch.sql"; } \
       | "${SSH[@]}" 'sudo docker exec -i supabase-db sh -c '\''read -r U; exec psql "$U" -X -At -q'\''' \
         > "$WORK/tokio.csv" 2> "$WORK/fetch.err"; then
    fetch_error "$WORK/fetch.err"; exit 1
  fi
  local n; n=$(python3 -c 'import csv,sys; csv.field_size_limit(1<<30); print(sum(1 for _ in csv.reader(open(sys.argv[1]))))' "$WORK/tokio.csv")
  [ "$n" = 10 ] || { echo "Tokio cavabı natamamdır ($n/10 paket)"; exit 1; }
  echo "  10/10 paket, $(du -h "$WORK/tokio.csv" | cut -f1)"
}

# ── 2. Media: Tokio storage-dakı, hilal/istək elanlarının istinad etdiyi fayllar ────────────────────
# 200 cavabında Content-Length, yoxsa boş.
remote_size() {
  curl -sI "$1" | tr -d '\r' | awk -F': ' 'NR==1 && $0 !~ / 200/ {exit} tolower($1)=="content-length" {print $2}'
}

media() {
  echo "→ media faylları"
  python3 - "$WORK/tokio.csv" "$OLD_STORAGE" > "$WORK/media.txt" <<'PY'
import csv, json, sys
csv.field_size_limit(1 << 30)
src, prefix = sys.argv[1], sys.argv[2]
seen = set()
for name, payload in csv.reader(open(src)):
    if name not in ("lunar", "sug"):
        continue
    for row in json.loads(payload):
        for m in row.get("media") or []:
            url = (m or {}).get("url") or ""
            if url.startswith(prefix) and url not in seen:
                seen.add(url)
                print(url[len(prefix):])
PY
  if [ ! -s "$WORK/media.txt" ]; then echo "  köçürüləcək fayl yoxdur"; return; fi

  local key
  key=$(grep -m1 '^SERVICE_ROLE_KEY=' "$ORACLE_ENV" | cut -d= -f2- | tr -d '"'"'"'')
  [ -n "$key" ] || { echo "  $ORACLE_ENV-də SERVICE_ROLE_KEY yoxdur"; exit 1; }
  # Açar curl-ə stdin konfiqurasiyası ilə verilir — nə əmr sətrində (ps), nə çıxışda görünür.
  local auth; auth=$(printf 'header = "Authorization: Bearer %s"\nheader = "apikey: %s"\n' "$key" "$key")

  # Açarın işlədiyini yazmadan yoxla (bucket siyahısı yalnız service rolu ilə açılır).
  local code
  code=$(printf '%s\n' "$auth" | curl -s -o /dev/null -w '%{http_code}' -K - "$NEW_BASE/storage/v1/bucket")
  [ "$code" = 200 ] || { echo "  Oracle storage service açarını qəbul etmədi (HTTP $code)"; exit 1; }

  local path size_old size_new file ctype failed=0
  while IFS= read -r path; do
    size_old=$(remote_size "$OLD_STORAGE$path")
    size_new=$(remote_size "$NEW_STORAGE$path")
    if [ -z "$size_old" ]; then echo "  TOKİO-DA YOXDUR  $path"; failed=1; continue; fi
    if [ "$size_new" = "$size_old" ]; then echo "  var          $path ($size_old bayt)"; continue; fi
    if [ "$APPLY" != true ]; then echo "  köçürüləcək  $path ($size_old bayt)"; continue; fi
    file="$WORK/media/$(basename "$path")"
    ctype=$(curl -sf -D - -o "$file" "$OLD_STORAGE$path" | tr -d '\r' | awk -F': ' 'tolower($1)=="content-type"{print $2}')
    code=$(printf '%s\n' "$auth" | curl -s -o /dev/null -w '%{http_code}' -X POST -K - \
                -H "x-upsert: true" -H "Content-Type: ${ctype:-application/octet-stream}" \
                --data-binary "@$file" "$NEW_BASE/storage/v1/object/$path")
    size_new=$(remote_size "$NEW_STORAGE$path")
    if [ "$code" = 200 ] && [ "$size_new" = "$size_old" ]; then
      echo "  köçürüldü $path ($size_old bayt)"
    else
      echo "  XƏTA      $path (HTTP $code, ölçü ${size_new:-?}/$size_old)"; failed=1
    fi
  done < "$WORK/media.txt"
  [ "$failed" = 0 ] || { echo "Media köçürülmədi — baza dəyişdirilmir"; exit 1; }
}

# ── 3. Birləşdirmə (Oracle) ──────────────────────────────────────────────────────────────────────
merge() {
  echo "→ Oracle: $([ "$APPLY" = true ] && echo 'tətbiq (COMMIT)' || echo 'plan (ROLLBACK)')"
  if ! { printf '\\set ON_ERROR_STOP 1\n\\set cutoff %s\n\\set skip %s\n\\set old_storage %s\n\\set new_storage %s\n\\set apply %s\n' \
                "'$CUTOFF'" "'$SKIP'" "'$OLD_STORAGE'" "'$NEW_STORAGE'" "$APPLY"
         printf 'set timezone = %s;\nbegin;\n' "'UTC'"
         cat "$HERE/delta_common.sql"
         printf 'create temp table delta_in (name text primary key, payload jsonb) on commit drop;\n'
         printf 'copy delta_in from stdin with (format csv);\n'
         cat "$WORK/tokio.csv"
         printf '\\.\n'
         cat "$HERE/delta_merge.sql"; } \
       | "${SSH[@]}" 'sudo docker exec -i supabase-db psql -U supabase_admin -d postgres -X -At -q' \
         > "$WORK/merge.out" 2> "$WORK/merge.err"; then
    echo "Oracle xətası (tranzaksiya geri alındı, heç nə dəyişmədi):"
    grep -E 'ERROR|LINE|psql:' "$WORK/merge.err" | head -8
    exit 1
  fi
  echo
  grep '^R|' "$WORK/merge.out" | python3 -c '
import sys
for line in sys.stdin:
    a, b, c = (line.rstrip("\n")[2:].split("|", 2) + ["", ""])[:3]
    print(f"  {a:<10} {b:<12} {c}")'
}

if [ "$APPLY" = true ]; then
  cat <<'TXT'
Bu, Oracle-dakı canlı bazanı dəyişəcək. Əvvəl:
  • məzmun redaktəsi hər iki tətbiqdə dayandırılıb?
  • yeni versiya mağazalarda görünür?
TXT
  read -r -p "Davam etmək üçün «bəli» yaz: " ok
  [ "$ok" = "bəli" ] || { echo "dayandırıldı"; exit 1; }
  echo "→ serverdə təzə yedək (anamuslim-backup.service)"
  "${SSH[@]}" -n 'sudo systemctl start anamuslim-backup.service && systemctl show -p Result --value anamuslim-backup.service' \
    | grep -qx success || { echo "Yedək alınmadı — dayandırıldı"; exit 1; }
  echo "  yedək hazırdır"
fi

# DELTA_REUSE_FETCH=1 — yalnız sınaq üçün: Tokio-nu yenidən oxumadan mövcud tokio.csv ilə işlə.
if [ "${DELTA_REUSE_FETCH:-}" = 1 ] && [ "$APPLY" != true ]; then
  echo "→ Tokio oxunmur, köhnə paket işlədilir ($WORK/tokio.csv)"
else
  fetch
fi
media
merge

if [ "$APPLY" = true ]; then
  cat <<'TXT'

Növbəti addımlar (README → «Keçid günü»): Oracle-da app_releases-i yeni versiyaya yaz, sonra Tokio-da
min_version-u qaldır. Ehtiyat (Frankfurt) gecə özü yenilənir; dərhal lazımdırsa:
  ssh … 'sudo systemctl start anamuslim-backup-sync.service'
TXT
fi
