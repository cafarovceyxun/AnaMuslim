#!/usr/bin/env bash
# Bir cədvəli yedəkdən geri yazır. Default **dry-run** — heç nə dəyişmir.
#
#   ./tools/supabase/restore-table.sh 2026-09-17 hadith            # nə olacağını göstərir
#   SUPABASE_SERVICE_KEY='<secret>' \
#     ./tools/supabase/restore-table.sh 2026-09-17 hadith --apply  # həqiqətən yazır
#
# Yedəklər iCloud Drive-dadır: ~/Library/Mobile Documents/com~apple~CloudDocs/AnaMuslim-Yedekler/
#
# ⚠️ Yazma **upsert**-dir: yedəkdəki sətirlər PK üzrə üstünə yazılır. Yedəkdən SONRA əlavə olunmuş
# sətirlər silinmir — yəni bu, «o tarixə tam qaytarma» deyil, «yedəkdəki sətirləri bərpa etmə»dir.
# Tam qaytarma lazımdırsa əvvəlcə cədvəli boşaltmaq lazımdır (əl ilə, SQL editorunda).
#
# ⚠️ Açar burada **qəsdən** saxlanmır: ~/.anamuslim-backup.env yalnız yedəyi oxumaq üçündür.
# service_role / sb_secret açarını yalnız bərpa anında mühit dəyişəni kimi ver.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="${ANAMUSLIM_BACKUP_ENV:-$HOME/.anamuslim-backup.env}"
CHUNK=500

# shellcheck source=/dev/null
[ -f "$ENV_FILE" ] && . "$ENV_FILE"

FOLDER="${1:-}"
TABLE="${2:-}"
MODE="${3:-}"
[ -n "$FOLDER" ] && [ -n "$TABLE" ] || {
  echo "İstifadə: $0 <tarix|qovluq> <cədvəl> [--apply]" >&2
  exit 1
}

ICLOUD_DIR="$HOME/Library/Mobile Documents/com~apple~CloudDocs/AnaMuslim-Yedekler"

FILE="$FOLDER/$TABLE.json"
[ -f "$FILE" ] || FILE="$ICLOUD_DIR/$FOLDER/$TABLE.json"
[ -f "$FILE" ] || FILE="$ROOT/backups/supabase/$FOLDER/$TABLE.json"
[ -f "$FILE" ] || {
  echo "Fayl yoxdur: $FILE" >&2
  echo "Mövcud yedəklər: $(ls -1 "$ICLOUD_DIR" 2>/dev/null | tr '\n' ' ')" >&2
  exit 1
}

: "${SUPABASE_URL:?SUPABASE_URL yoxdur — $ENV_FILE faylına yaz}"

ROWS="$(jq 'length' "$FILE")"
echo "Yedək:  $FILE — $ROWS sətir"

if [ "$MODE" != "--apply" ]; then
  echo
  echo "DRY-RUN — heç nə yazılmadı."
  echo "Həqiqətən bərpa etmək üçün:"
  echo "  SUPABASE_SERVICE_KEY='<secret>' $0 $FOLDER $TABLE --apply"
  exit 0
fi

: "${SUPABASE_SERVICE_KEY:?SUPABASE_SERVICE_KEY yoxdur — bərpa üçün məcburidir}"

auth_headers=(-H "apikey: $SUPABASE_SERVICE_KEY")
case "$SUPABASE_SERVICE_KEY" in
  eyJ*) auth_headers+=(-H "Authorization: Bearer $SUPABASE_SERVICE_KEY") ;;
esac

CURRENT="$(curl -fsS -I -X HEAD "${auth_headers[@]}" \
  -H 'Range: 0-0' -H 'Prefer: count=exact' \
  "$SUPABASE_URL/rest/v1/$TABLE?select=*" | tr -d '\r' |
  awk -F'/' 'tolower($0) ~ /^content-range:/ {print $2}')"

echo "Serverdə indi: ${CURRENT:-?} sətir"
echo
printf 'Təsdiq üçün cədvəlin adını yaz (%s): ' "$TABLE"
read -r CONFIRM
[ "$CONFIRM" = "$TABLE" ] || { echo "Ləğv edildi."; exit 1; }

offset=0
while [ "$offset" -lt "$ROWS" ]; do
  jq -c --argjson s "$offset" --argjson n "$CHUNK" '.[$s:$s+$n]' "$FILE" |
    curl -fsS -X POST "$SUPABASE_URL/rest/v1/$TABLE" \
      "${auth_headers[@]}" \
      -H 'Content-Type: application/json' \
      -H 'Prefer: resolution=merge-duplicates,return=minimal' \
      --data-binary @- > /dev/null
  offset=$((offset + CHUNK))
  printf '\r%s/%s sətir' "$((offset < ROWS ? offset : ROWS))" "$ROWS"
done
echo
echo "Bərpa bitdi: $TABLE"
