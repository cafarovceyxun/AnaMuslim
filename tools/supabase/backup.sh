#!/usr/bin/env bash
# Supabase məzmununun yedəyini **birbaşa iCloud Drive-a** alır.
#
#   ./tools/supabase/backup.sh           # 3 gün keçibsə yedək alır
#   ./tools/supabase/backup.sh --force   # indi al
#   ./tools/supabase/backup.sh --check   # yalnız vəziyyət
#
# ⚠️ Yedək Supabase-də **saxlanmır**: nə bucket, nə cron: server yalnız oxu qapısıdır
# (`db-backup` Edge Function → `backup_table_list` / `backup_table_json`). Nəticə budur ki,
# **yedəyi bu Mac alır** — Mac üç gündən çox bağlı qalarsa yedək o qədər gecikir.
#
# launchd agenti (`tools/mac/install-backup-sync.sh`) bunu saatda bir işlədir; Mac yuxudan
# oyananda buraxılmış işləmə bir dəfə icra olunur.
#
# ⚠️ Skript repodan işlədilmir, ~/Library/Application Support-dakı **surətdən** işləyir: repo
# `~/Desktop`-dadır, o isə macOS TCC qorumasındadır (launchd `Operation not permitted` verir).
# Dəyişiklikdən sonra quraşdırıcını yenidən işlət.
#
# Sirr: ~/.anamuslim-backup.env (chmod 600, repoda deyil):
#   SUPABASE_URL=https://<ref>.supabase.co
#   BACKUP_SECRET=<Vault-dakı backup_trigger_secret>

set -euo pipefail

ENV_FILE="${ANAMUSLIM_BACKUP_ENV:-$HOME/.anamuslim-backup.env}"
FOLDER_NAME="AnaMuslim-Yedekler"
KEEP=30
MIN_DAYS=3
# Bu qədər gündən sonra yedək yoxdursa bildiriş verilir (Mac uzun müddət bağlı qalıb).
STALE_DAYS=5
LOG_FILE="$HOME/Library/Logs/anamuslim-backup-sync.log"
LOG_MAX_BYTES=1048576

log() { printf '%s  %s\n' "$(date '+%Y-%m-%d %H:%M')" "$*"; }

notify() {
  /usr/bin/osascript -e "display notification \"$1\" with title \"AnaMuslim yedəyi\"" \
    >/dev/null 2>&1 || true
}

fail() {
  log "XƏTA: $1"
  notify "$1"
  exit 1
}

# launchd logu əlavə rejimdə tutur; böyüyəndə kəsirik (fayl deskriptoru qırılmır).
if [ -f "$LOG_FILE" ] && [ "$(stat -f%z "$LOG_FILE")" -gt "$LOG_MAX_BYTES" ]; then
  : > "$LOG_FILE"
fi

# shellcheck source=/dev/null
[ -f "$ENV_FILE" ] && . "$ENV_FILE"
: "${SUPABASE_URL:?SUPABASE_URL yoxdur — $ENV_FILE faylına yaz}"
: "${BACKUP_SECRET:?BACKUP_SECRET yoxdur — $ENV_FILE faylına yaz}"

ICLOUD="$HOME/Library/Mobile Documents/com~apple~CloudDocs"
[ -d "$ICLOUD" ] || fail "iCloud Drive qovluğu tapılmadı"
DEST="${BACKUP_DEST:-$ICLOUD/$FOLDER_NAME}"
mkdir -p "$DEST"

FN="$SUPABASE_URL/functions/v1/db-backup"
call() {
  curl -fsS --max-time 180 -X POST "$FN" \
    -H "x-backup-secret: $BACKUP_SECRET" \
    -H 'Content-Type: application/json' \
    -d "$1"
}

days_since() {
  local when="$1" then
  then=$(date -j -f '%Y-%m-%d' "$when" '+%s' 2>/dev/null) || { echo 9999; return; }
  echo $(( ( $(date +%s) - then ) / 86400 ))
}

NEWEST="$(ls -1 "$DEST" 2>/dev/null | grep -E '^[0-9]{4}-[0-9]{2}-[0-9]{2}$' | sort | tail -1 || true)"
AGE="$( [ -n "$NEWEST" ] && days_since "$NEWEST" || echo 9999 )"

if [ "${1:-}" = "--check" ]; then
  if [ -n "$NEWEST" ]; then
    log "son yedək: $NEWEST ($AGE gün əvvəl), cəmi $(ls -1 "$DEST" | grep -cE '^[0-9]{4}' || echo 0) qovluq"
  else
    log "hələ yedək yoxdur — $DEST"
  fi
  exit 0
fi

if [ "$AGE" -lt "$MIN_DAYS" ] && [ "${1:-}" != "--force" ]; then
  log "yedək lazım deyil (son: $NEWEST, $AGE gün)"
  exit 0
fi

[ "$AGE" -gt "$STALE_DAYS" ] && [ -n "$NEWEST" ] && \
  log "XƏBƏRDARLIQ: son yedək $AGE gün əvvəldir — Mac uzun müddət bağlı qalıb"

# ── Yedək ────────────────────────────────────────────────────────────────────
FOLDER="$(date '+%Y-%m-%d')"
STAGE="$(mktemp -d)"
trap 'rm -rf "$STAGE"' EXIT

TABLES_JSON="$STAGE/tables.json"
call '{"mode":"tables"}' > "$TABLES_JSON" || fail "cədvəl siyahısı alınmadı"
PAGE="$(jq -r '.page_limit' "$TABLES_JSON")"

mkdir -p "$STAGE/$FOLDER"
PROBLEMS=""
TOTAL_ROWS=0

while IFS=$'\t' read -r table rows; do
  rm -f "$STAGE"/page.*.json
  offset=0
  page=0
  while [ "$offset" -lt "$rows" ]; do
    call "{\"mode\":\"table\",\"table\":\"$table\",\"offset\":$offset,\"limit\":$PAGE}" \
      > "$(printf '%s/page.%04d.json' "$STAGE" "$page")" \
      || fail "$table: səhifə $page alınmadı"
    offset=$((offset + PAGE))
    page=$((page + 1))
  done

  if [ "$page" -eq 0 ]; then
    echo '[]' > "$STAGE/$FOLDER/$table.json"
  else
    # Səhifələr bir massivə yığılır; `jq -s add` həm birləşdirir, həm də JSON-u yoxlayır.
    jq -s 'add' "$STAGE"/page.*.json > "$STAGE/$FOLDER/$table.json" \
      || fail "$table: JSON birləşdirilmədi"
  fi

  got="$(jq 'length' "$STAGE/$FOLDER/$table.json")"
  TOTAL_ROWS=$((TOTAL_ROWS + got))
  # Natamam yedək səssizcə keçməsin — manifestə də düşür.
  [ "$got" = "$rows" ] || PROBLEMS="$PROBLEMS $table(serverdə $rows, alınan $got)"
done < <(jq -r '.tables[] | "\(.table)\t\(.rows)"' "$TABLES_JSON")

TABLE_COUNT="$(jq '.tables | length' "$TABLES_JSON")"

jq -n \
  --arg folder "$FOLDER" \
  --arg generated "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" \
  --arg problems "$PROBLEMS" \
  --argjson tables "$(jq '.tables' "$TABLES_JSON")" \
  '{folder: $folder, generated_at: $generated, tables: $tables,
    problems: ($problems | ltrimstr(" ") | select(length > 0) // null)}' \
  > "$STAGE/$FOLDER/manifest.json"

# Yarımçıq qovluq iCloud-da görünməsin: əvvəlcə gizli ada, sonra yerinə.
rm -rf "${DEST:?}/.$FOLDER.partial" "${DEST:?}/$FOLDER"
cp -R "$STAGE/$FOLDER" "$DEST/.$FOLDER.partial"
mv "$DEST/.$FOLDER.partial" "$DEST/$FOLDER"

# ── Saxlama: son KEEP qovluq ─────────────────────────────────────────────────
list="$(ls -1 "$DEST" | grep -E '^[0-9]{4}-[0-9]{2}-[0-9]{2}$' | sort || true)"
total=$(printf '%s' "$list" | grep -c . || true)
if [ "$total" -gt "$KEEP" ]; then
  printf '%s\n' "$list" | sed -n "1,$((total - KEEP))p" | while read -r old; do
    rm -rf "${DEST:?}/$old"
    log "silindi (saxlama $KEEP): $old"
  done
fi

SIZE="$(du -sh "$DEST/$FOLDER" | cut -f1 | tr -d ' ')"
if [ -n "$PROBLEMS" ]; then
  log "yedək NATAMAM: $FOLDER — $TABLE_COUNT cədvəl, $TOTAL_ROWS sətir, $SIZE;$PROBLEMS"
  notify "Yedək natamam: $FOLDER"
else
  log "yedək: $FOLDER — $TABLE_COUNT cədvəl, $TOTAL_ROWS sətir, $SIZE"
fi
