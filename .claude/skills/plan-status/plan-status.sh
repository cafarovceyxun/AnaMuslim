#!/usr/bin/env bash
# IOS_MIGRATION_PLAN.md-dən yalnız cari vəziyyəti çıxarır (tam fayl 433 KB-dır).
#
# Faylın quruluşu:
#   - Bölmənin başı  → sabitlənmiş cari vəziyyət bloku (ən son xülasə, cari faza)
#   - Tarixli jurnal → "- 2026-XX-XX — N-ci dalğa: ..." sətirləri, ƏN YENİDƏN KÖHNƏYƏ
#   - Bölmənin sonu  → ən KÖHNƏ qeydlər (adətən lazım deyil)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
PLAN="$ROOT/IOS_MIGRATION_PLAN.md"

[ -f "$PLAN" ] || { echo "TAPILMADI: $PLAN" >&2; exit 1; }

START=$(grep -n '^## 🔖 HAZIRDA HARDAYIQ' "$PLAN" | head -1 | cut -d: -f1)
[ -n "$START" ] || { echo "'## 🔖 HAZIRDA HARDAYIQ' başlığı tapılmadı" >&2; exit 1; }

END=$(awk -v s="$START" 'NR>s && /^## /{print NR-1; exit}' "$PLAN")
[ -n "$END" ] || END=$(wc -l < "$PLAN")

echo "═══ IOS_MIGRATION_PLAN.md — $(wc -c < "$PLAN" | tr -d ' ') bayt, $(wc -l < "$PLAN" | tr -d ' ') sətir"
echo "═══ HAZIRDA HARDAYIQ: $START–$END · tarixli jurnal: $(grep -c '^- 202[0-9]-' "$PLAN") qeyd"
echo
echo "───────── 1. CARİ VƏZİYYƏT (bölmənin başı) ─────────"
sed -n "$((START + 1)),$((START + 40))p" "$PLAN"
echo
echo "───────── 2. SON 5 DALĞA (tam mətn) ─────────"
grep '^- 202[0-9]-' "$PLAN" | head -5
echo
echo "───────── 3. DALĞA İNDEKSİ (növbəti 25, kəsilmiş) ─────────"
echo "     Konkret dalğa lazımdırsa: Read IOS_MIGRATION_PLAN.md offset=<sətir> limit=1"
grep -n '^- 202[0-9]-' "$PLAN" | sed -n '6,30p' | cut -c1-150
echo
echo "───────── 4. AÇIQ BƏNDLƏR (- [ ]) ─────────"
grep -n '^\s*- \[ \]' "$PLAN" | cut -c1-200
echo
echo "───────── 5. QİSMƏN BİTMİŞ (- [~]) ─────────"
grep -n '^\s*- \[~\]' "$PLAN" | cut -c1-200 || echo "(yoxdur)"
