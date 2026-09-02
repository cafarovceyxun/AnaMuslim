#!/usr/bin/env bash
#
# Mac-dakı mətni qoşulu telefonun **panosuna** göndərir (KDE Connect), adb isə ehtiyat yoldur.
#
# Niyə: hədis toplu əlavə (`HadithBulkAddScreen`) və redaktor ekranları mətni panodan yapışdırmaqla
# doldurulur (`ClipboardFormParser` — `1§`, `2§` … bloklarını oxuyur). Mətn Mac-da hazırlanır, ona
# görə hər dəfə əl ilə köçürmək əvəzinə birbaşa telefonun panosuna atılır.
#
# ⚠️ İki tələ var, hər ikisi səssizcə mətni korlayır:
#
#   1. `pbcopy` girişi **cari lokalın** kodlaşdırması ilə oxuyur. Claude Code-un qabığında
#      `LC_CTYPE=C`-dir, ona görə UTF-8 baytlar Mac Roman kimi şərh olunur və telefona
#      «ÿØ ŸÑŸÑŸá ,Äî az…ôrbaycanca» kimi düşür (ölçdüm: 2026-09-02). `LC_CTYPE=UTF-8` məcburidir.
#      Nə pbcopy, nə kdeconnect-cli, nə də telefon xəta vermir — mətn sadəcə korlanır.
#   2. `adb shell` arqumentləri **cihazın qabığında yenidən parçalayır**. Yerli dırnaqlar itir,
#      ona görə `am start`-a gedən mətn ayrıca dırnaq qatı ilə göndərilir; əks halda boşluqdan
#      sonrakı söz paket adı sanılır («Error: Activity not started … pkg=Sınaq»).
#
# Yollar tam yazılıb: `adb` PATH-da yoxdur (~/.zshrc-də platform-tools sətri yoxdur), `kdeconnect-cli`
# isə tətbiq paketinin içindədir.
#
# İşlətmək:
#   tools/phone/send-clipboard.sh "mətn"
#   printf '1§ ...\n2§ ...' | tools/phone/send-clipboard.sh
#   tools/phone/send-clipboard.sh -f blok.txt
#   tools/phone/send-clipboard.sh --adb "mətn"    # panonu ötür, paylaşma vərəqini aç (bir toxunuş lazım)
#
set -euo pipefail

KDECONNECT="/Applications/KDE Connect.app/Contents/MacOS/kdeconnect-cli"
ADB="$HOME/Library/Android/sdk/platform-tools/adb"

USE_ADB=0
TEXT=""

while [ $# -gt 0 ]; do
    case "$1" in
        --adb)  USE_ADB=1; shift ;;
        -f)     TEXT="$(cat "$2")"; shift 2 ;;
        -h|--help)
            sed -n '2,25p' "$0" | sed 's/^# \{0,1\}//'
            exit 0 ;;
        *)      TEXT="$1"; shift ;;
    esac
done

# Arqument verilməyibsə stdin-dən oxu (boru xətti üçün).
if [ -z "$TEXT" ] && [ ! -t 0 ]; then
    TEXT="$(cat)"
fi

if [ -z "$TEXT" ]; then
    echo "send-clipboard: mətn yoxdur (arqument, -f fayl və ya stdin gözlənilir)" >&2
    exit 1
fi

CHARS=$(printf '%s' "$TEXT" | wc -m | tr -d ' ')

# ── adb yolu: paylaşma vərəqi ────────────────────────────────────────────────────────────────────
send_via_adb() {
    if [ ! -x "$ADB" ]; then
        echo "send-clipboard: adb tapılmadı ($ADB)" >&2
        return 1
    fi
    if ! "$ADB" get-state >/dev/null 2>&1; then
        echo "send-clipboard: USB ilə qoşulu cihaz yoxdur" >&2
        return 1
    fi
    # `am` arqumentləri binder üzərindən gedir, ona görə uzun mətn qabığın arqument həddinə dəyir.
    if [ "$CHARS" -gt 4000 ]; then
        echo "send-clipboard: mətn $CHARS simvoldur — adb paylaşması üçün çox uzun, KDE Connect işlət" >&2
        return 1
    fi
    local esc
    esc=$(printf '%s' "$TEXT" | sed "s/'/'\\\\''/g")
    "$ADB" shell am start -a android.intent.action.SEND -t text/plain \
        --es android.intent.extra.TEXT "'$esc'" >/dev/null
    echo "send-clipboard: paylaşma vərəqi açıldı ($CHARS simvol) — telefonda hədəfi seç"
}

if [ "$USE_ADB" -eq 1 ]; then
    send_via_adb
    exit $?
fi

# ── KDE Connect yolu: birbaşa telefonun panosuna ─────────────────────────────────────────────────
if [ ! -x "$KDECONNECT" ]; then
    echo "send-clipboard: KDE Connect quraşdırılmayıb, adb-yə keçirəm" >&2
    send_via_adb
    exit $?
fi

# LC_CTYPE olmadan Mac panosu korlanır — yuxarıdakı 1-ci tələ.
printf '%s' "$TEXT" | LC_CTYPE=UTF-8 pbcopy

# stderr-də DBus xəbərdarlıqları olur (macOS-da kdeconnectd .service faylı ilə qeydiyyatdan keçmir),
# əmr buna baxmayaraq işləyir — ona görə susdurulur.
DEVICE="$("$KDECONNECT" -a --id-only 2>/dev/null | head -1)"

if [ -z "$DEVICE" ]; then
    echo "send-clipboard: KDE Connect-də əlçatan cihaz yoxdur (eyni Wi-Fi?), adb-yə keçirəm" >&2
    send_via_adb
    exit $?
fi

"$KDECONNECT" -d "$DEVICE" --send-clipboard 2>/dev/null
NAME="$("$KDECONNECT" -a --id-name-only 2>/dev/null | grep "^$DEVICE " | cut -d' ' -f2-)"
echo "send-clipboard: ${NAME:-$DEVICE} panosuna göndərildi ($CHARS simvol) — ekranda yapışdır"
