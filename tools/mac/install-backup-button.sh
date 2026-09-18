#!/usr/bin/env bash
# Mac-də «Yedək al» düyməsini qurur: iCloud qovluğuna ikiqat kliklə işə düşən `.command` faylı.
#
#   ./tools/mac/install-backup-button.sh            # qur / yenilə
#   ./tools/mac/install-backup-button.sh --uninstall
#
# Cədvəl yoxdur, launchd yoxdur: yedək **yalnız sən basanda** alınır (telefonda tətbiqin ana
# ekranındakı xatırlatma, Mac-də bu fayl). Nəticə həmişə eyni yerə düşür:
#   ~/Library/Mobile Documents/com~apple~CloudDocs/AnaMuslim-Yedekler/<tarix>/
#
# ⚠️ Fayl birbaşa repodakı skripti çağırır. Bu, launchd-dən fərqlidir: Terminal-dan işə düşdüyü
# üçün macOS TCC qorumasına ilişmir (launchd agenti `~/Desktop`-dakı faylı icra edə bilmirdi —
# `Operation not permitted`). İlk dəfə macOS «Terminal iCloud Drive-a müraciət edir» deyə soruşa
# bilər — icazə ver.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DEST="$HOME/Library/Mobile Documents/com~apple~CloudDocs/AnaMuslim-Yedekler"
BUTTON="$DEST/Yedək al.command"

if [ "${1:-}" = "--uninstall" ]; then
  rm -f "$BUTTON"
  echo "silindi: $BUTTON (yedəklərin özünə toxunulmadı)"
  exit 0
fi

[ -f "$HOME/.anamuslim-backup.env" ] || {
  echo "~/.anamuslim-backup.env yoxdur — SUPABASE_URL və BACKUP_SECRET orada olmalıdır" >&2
  exit 1
}

mkdir -p "$DEST"

cat > "$BUTTON" <<EOF
#!/bin/bash
# AnaMuslim — məzmun yedəyi. İkiqat klik: bütün cədvəllər bu qovluğa tarixli alt-qovluqda yazılır.
"$ROOT/tools/supabase/backup.sh" --force
status=\$?
echo
if [ \$status -eq 0 ]; then
  echo "Bitdi."
else
  echo "XƏTA (kod \$status)"
fi
read -n 1 -s -r -p "Bağlamaq üçün istənilən düyməyə bas..."
echo
EOF

chmod +x "$BUTTON"

echo "quraşdırıldı: $BUTTON"
echo "Finder-də ikiqat klik → yedək alınır."
