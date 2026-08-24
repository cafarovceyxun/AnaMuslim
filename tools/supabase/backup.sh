#!/usr/bin/env bash
# Supabase məzmun yedəyi — PostgREST üzərindən, quraşdırma tələb etmir.
#
#   ./tools/supabase/backup.sh [çıxış_qovluğu]
#
# Default çıxış: backups/supabase/<UTC-tarix-saat>/
# Hər cədvəl bir JSON massivi kimi yazılır (NULL/tip qorunur, CSV-dən fərqli olaraq).
#
# Açar: default olaraq SupabaseProvider.kt-dəki *anon* açarı işlədilir — yəni
# yalnız RLS-in ictimai oxumağa icazə verdiyi sətirlər düşür. Tam yedək üçün
# service_role açarını ötür:
#   SUPABASE_KEY='<service_role>' ./tools/supabase/backup.sh
# (açarı repoya YAZMA, yalnız mühit dəyişəni kimi ver.)

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PROVIDER="$ROOT/shared/src/commonMain/kotlin/com/cafarovceyxun/anamuslim/utils/supabase/SupabaseProvider.kt"

SUPABASE_URL="${SUPABASE_URL:-$(grep -o 'https://[a-z0-9]*\.supabase\.co' "$PROVIDER" | head -1)}"
SUPABASE_KEY="${SUPABASE_KEY:-$(grep -o 'eyJ[A-Za-z0-9._-]*' "$PROVIDER" | head -1)}"
[ -n "$SUPABASE_URL" ] && [ -n "$SUPABASE_KEY" ] || { echo "URL/açar tapılmadı"; exit 1; }

OUT="${1:-$ROOT/backups/supabase/$(date -u +%Y-%m-%dT%H%M%SZ)}"
PAGE=1000

# cədvəl:sıralama_sütunu
TABLES=(
  quran_translations_data:id
  quran_edits:id
  hadith:id
  hadith_edits:id
  hadith_volume:slug
  hadith_book:slug
  hadith_chapter:slug
  hadith_sub_chapter:slug
  daily_content:id
  verse_reports:id
  resource_updates:id
  resource_updates_admin:id
  app_releases:platform
  app_logs:id
)

key_role() {
  local b64 pad
  b64="$(printf '%s' "$SUPABASE_KEY" | cut -d. -f2 | tr '_-' '/+')"
  pad=$(( (4 - ${#b64} % 4) % 4 ))
  while [ "$pad" -gt 0 ]; do b64="$b64="; pad=$((pad - 1)); done
  printf '%s' "$b64" | base64 -d 2>/dev/null | jq -r '.role // "?"' 2>/dev/null || echo '?'
}

mkdir -p "$OUT"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

MANIFEST="$OUT/manifest.txt"
{
  echo "# AnaMuslim Supabase yedəyi"
  echo "tarix_utc: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "url:       $SUPABASE_URL"
  echo "rol:       $(key_role)"
  echo
  printf '%-26s %8s %8s %10s\n' cədvəl serverdə yüklənən bayt
} > "$MANIFEST"

fail=0; skipped=0
for entry in "${TABLES[@]}"; do
  table="${entry%%:*}"; order="${entry#*:}"
  base="$SUPABASE_URL/rest/v1/$table?select=*&order=$order.asc"

  hdr="$(curl -sS -D - -o /dev/null -w '%{http_code}' \
    -H "apikey: $SUPABASE_KEY" -H "Authorization: Bearer $SUPABASE_KEY" \
    -H "Range: 0-0" -H "Prefer: count=exact" "$base" | tr -d '\r')"
  code="$(printf '%s' "$hdr" | tail -1)"
  if [ "$code" != "200" ] && [ "$code" != "206" ]; then
    printf '%-26s %8s %8s %10s  %s\n' "$table" "?" "-" "-" "HTTP $code — açar bu cədvəli oxuya bilmir (RLS)" >> "$MANIFEST"
    skipped=$((skipped + 1))
    continue
  fi
  total=$(printf '%s' "$hdr" | awk -F'/' 'tolower($0) ~ /^content-range:/ {print $2}')
  total="${total:-0}"

  rm -f "$TMP"/page.*.json
  offset=0; page=0
  while [ "$offset" -lt "$total" ]; do
    curl -fsS -H "apikey: $SUPABASE_KEY" -H "Authorization: Bearer $SUPABASE_KEY" \
      -H "Range: $offset-$((offset + PAGE - 1))" \
      -o "$(printf '%s/page.%04d.json' "$TMP" "$page")" "$base"
    offset=$((offset + PAGE)); page=$((page + 1))
  done

  if [ "$page" -eq 0 ]; then
    echo '[]' > "$OUT/$table.json"
  else
    jq -s 'add' "$TMP"/page.*.json > "$OUT/$table.json"
  fi

  got=$(jq 'length' "$OUT/$table.json")
  bytes=$(wc -c < "$OUT/$table.json" | tr -d ' ')
  printf '%-26s %8s %8s %10s\n' "$table" "$total" "$got" "$bytes" >> "$MANIFEST"
  if [ "$got" != "$total" ]; then
    echo "XƏBƏRDARLIQ: $table — serverdə $total, yüklənən $got" >&2
    fail=1
  fi
done

cp "$ROOT/docs/supabase/SCHEMA.md" "$OUT/SCHEMA.md" 2>/dev/null || true

cat "$MANIFEST"
echo
echo "Yedək: $OUT"
[ "$skipped" -eq 0 ] || echo "$skipped cədvəl buraxıldı — anon açar onları oxumur; tam yedək üçün SUPABASE_KEY='<service_role>' ilə işlət."
[ "$fail" -eq 0 ] || { echo "Bəzi cədvəllər natamam yükləndi." >&2; exit 2; }
