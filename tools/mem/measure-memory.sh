#!/usr/bin/env bash
#
# Reader yaddaş izinin faza-faza ölçülməsi (dumpsys meminfo).
#
# Niyə: Play-in yeni keyfiyyət şərtləri dinamik yaddaş və bitmap istifadəsinə eşik qoyur, ölçüsü
# isə tətbiqin *fondakı* rezident izidir. Bu skript oxucunu doldurub fona atır və trim-dən sonra
# nə qədərinin geri qaytarıldığını göstərir — yəni `QuranApp.onTrimMemory` bağlantısının işlədiyini.
#
# İşlətmək: tools/mem/measure-memory.sh [--label <ad>] [--pages <N>] [--complete]
#
set -euo pipefail

PKG="com.cafarovceyxun.anamuslim.test"     # DEBUG paketi — suffikssiz ad istifadəçinin Play buildidir
ACTIVITY="com.cafarovceyxun.anamuslim.activities.MainActivity"
OPEN_READER="com.cafarovceyxun.anamuslim.action.OPEN_READER"

LABEL="$(date +%H%M%S)"
PAGES=12
SCROLL=h            # h = üfüqi səhifə çevirmə (mushaf/atlas rejimi), v = şaquli sürüşmə (tərcümə rejimi)
RUN_COMPLETE=0
OUT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CSV="$OUT_DIR/results.csv"

while [ $# -gt 0 ]; do
  case "$1" in
    --label)    LABEL="$2"; shift 2 ;;
    --pages)    PAGES="$2"; shift 2 ;;
    --scroll)   SCROLL="$2"; shift 2 ;;
    --complete) RUN_COMPLETE=1; shift ;;
    -h|--help)  sed -n '2,12p' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) echo "naməlum arqument: $1" >&2; exit 2 ;;
  esac
done

if [ "$PKG" = "com.cafarovceyxun.anamuslim" ]; then
  echo "DAYAN: bu, Play produksiya paketidir — ölçmə yalnız .test debug buildində." >&2
  exit 1
fi

command -v adb >/dev/null || export PATH="$PATH:$HOME/Library/Android/sdk/platform-tools"
command -v adb >/dev/null || { echo "adb tapılmadı (PATH-a ~/Library/Android/sdk/platform-tools əlavə et)" >&2; exit 1; }

if [ -z "$(adb devices | sed -n '2p')" ]; then
  echo "Cihaz qoşulu deyil. Telefonu USB ilə qoş, USB debugging-i aç, sonra yenidən işlət." >&2
  exit 1
fi

adb shell pm list packages 2>/dev/null | grep -q "^package:$PKG$" || {
  echo "$PKG quraşdırılmayıb. Əvvəl:" >&2
  echo '  JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:installDebug' >&2
  exit 1
}

DEVICE="$(adb shell getprop ro.product.model | tr -d '\r')"
SDK="$(adb shell getprop ro.build.version.sdk | tr -d '\r')"
DENSITY="$(adb shell wm density | tr -d '\r' | grep -oE '[0-9]+' | tail -1)"

# Play-in Android vitals metriki PSS deyil, **anonymous RSS + swap**-dır (developer.android.com/
# topic/performance/vitals/memory-usage). Onu /proc/<pid>/status verir; dumpsys meminfo-nun
# bölgüsü (Java / Native / Graphics) isə hansı hissənin dəyişdiyini göstərmək üçün saxlanılır.
# TOTAL PSS bu cihazda çox səs-küylüdür (paylaşılan GPU buferləri), qərar üçün ona baxma.
anon_swap_kb() {
  local pid anon swap
  pid="$(adb shell pidof "$PKG" | tr -d '\r' | awk '{print $1}')"
  [ -n "$pid" ] || { echo "0 0"; return; }
  local st; st="$(adb shell cat "/proc/$pid/status" 2>/dev/null | tr -d '\r')"
  anon="$(echo "$st" | awk '/^RssAnon:/ {print $2; exit}')"
  swap="$(echo "$st" | awk '/^VmSwap:/  {print $2; exit}')"
  echo "${anon:-0} ${swap:-0}"
}

sample() {
  local phase="$1"
  # Ölçmədən əvvəl bir qədər gözlə: `recycle()` yolu qəsdən götürülüb, piksellər növbəti GC-də
  # azad olunur, ona görə trim-dən dərhal sonrakı oxu düşməni göstərmir.
  sleep 6

  local raw
  raw="$(adb shell dumpsys meminfo "$PKG" 2>/dev/null | tr -d '\r')"

  local java native graphics total
  java="$(echo "$raw"     | awk '/Java Heap:/   {print $3; exit}')"
  native="$(echo "$raw"   | awk '/Native Heap:/ {print $3; exit}')"
  graphics="$(echo "$raw" | awk '/Graphics:/    {print $2; exit}')"
  total="$(echo "$raw"    | awk '/TOTAL PSS:/   {print $3; exit}')"

  : "${java:=0}" "${native:=0}" "${graphics:=0}" "${total:=0}"

  local anon swap sum
  read -r anon swap <<< "$(anon_swap_kb)"
  sum=$(( anon + swap ))

  printf "%-38s %11s %9s %9s %9s %9s\n" \
    "$phase" \
    "$(mb "$sum")" "$(mb "$java")" "$(mb "$native")" "$(mb "$graphics")" "$(mb "$total")"

  echo "$LABEL,$DEVICE,$SDK,$phase,$anon,$swap,$sum,$java,$native,$graphics,$total" >> "$CSV"
}

mb() { awk -v k="$1" 'BEGIN { printf "%.1f", k/1024 }'; }

settle() { sleep "${1:-3}"; }

# `am send-trim-memory <proses> <səviyyə>`. Səviyyə adları platforma sabitlərindən fərqlidir
# (TRIM_MEMORY_UI_HIDDEN → "HIDDEN"); köhnə cihazlarda əmr ümumiyyətlə olmaya bilər.
trim() {
  local level="$1"
  if ! adb shell am send-trim-memory "$PKG" "$level" 2>&1 | grep -qiE "error|unknown|exception"; then
    return 0
  fi
  echo "  (xəbərdarlıq: 'am send-trim-memory $PKG $level' keçmədi — bu cihazda faza etibarsızdır)" >&2
}

echo "Cihaz: $DEVICE (API $SDK, density $DENSITY) — paket: $PKG — etiket: $LABEL"
echo
printf "%-38s %11s %9s %9s %9s %9s\n" "Faza" "ANON+SWAP" "Java" "Native" "Qrafika" "PSS"
printf "%-38s %11s %9s %9s %9s %9s\n" "--------------------------------------" "-----------" "---------" "---------" "---------" "---------"

[ -f "$CSV" ] || echo "label,device,sdk,phase,rss_anon_kb,vm_swap_kb,anon_plus_swap_kb,java_kb,native_kb,graphics_kb,total_pss_kb" > "$CSV"

# --- 1. Soyuq başlanğıc ------------------------------------------------------
adb shell am force-stop "$PKG"
sleep 2
adb shell am start -W -n "$PKG/$ACTIVITY" >/dev/null
settle 5
sample "1. soyuq başlanğıc (ana ekran)"

# --- 2. Oxucu açıq -----------------------------------------------------------
# Boş extra-larla OPEN_READER son oxunan yeri açır; cüz 1 təkrarlana bilən başlanğıc verir.
adb shell am start -a "$OPEN_READER" -n "$PKG/$ACTIVITY" \
  --es reader.read_type juz --ei reader.juz_no 1 >/dev/null
settle 6
sample "2. oxucu açıq (cüz 1)"

# --- 3. Səhifə çevirmələri (atlas keşlərini doldurur) ------------------------
# `monkey` qəsdən işlədilmir — cihaz qaydası. `input swipe` real jest göndərir.
W="$(adb shell wm size | tr -d '\r' | grep -oE '[0-9]+x[0-9]+' | tail -1 | cut -dx -f1)"
H="$(adb shell wm size | tr -d '\r' | grep -oE '[0-9]+x[0-9]+' | tail -1 | cut -dx -f2)"
X1=$(( W * 80 / 100 )); X2=$(( W * 20 / 100 )); Y=$(( H / 2 ))

# Atlas keşləri yalnız mushaf/atlas rejimində dolur, orada da səhifə üfüqi çevrilir — default budur.
# Oxucu tərcümə rejimindədirsə `--scroll v` ver, yoxsa jest heç nə etmir və faza 3 mənasız çıxır.
for i in $(seq 1 "$PAGES"); do
  if [ "$SCROLL" = "v" ]; then
    adb shell input swipe "$(( W / 2 ))" "$(( H * 75 / 100 ))" "$(( W / 2 ))" "$(( H * 25 / 100 ))" 220
  else
    adb shell input swipe "$X1" "$Y" "$X2" "$Y" 220
  fi
  sleep 1.2
done
settle 4
sample "3. $PAGES səhifə oxunduqdan sonra"

# --- 4. Fon, trim-siz --------------------------------------------------------
adb shell input keyevent KEYCODE_HOME
settle 4
sample "4. fonda (trim gəlməmiş)"

# --- 5. UI_HIDDEN trim -------------------------------------------------------
# Düzəlişin əsas sübutu: bu faza ilə 4-cü faza arasındakı fərq atlas teksturaları + placement
# keşi + tajweed keşidir. Düzəlişdən əvvəl bu fərq sıfır idi (clearCache() çağırılmırdı).
trim HIDDEN   # Am.java-da UI_HIDDEN-in tokeni məhz "HIDDEN"-dir
sample "5. UI_HIDDEN trim-dən sonra"

# --- 6. Ön plan təzyiqi ------------------------------------------------------
# Trim-dən ƏVVƏL və SONRA ayrıca ölçülür: ön plana qayıdış oxucunu yenidən qurur, ona görə tək
# ölçüm 5-ci faza ilə müqayisə oluna bilməz — mənalı olan yalnız 6a → 6b fərqidir.
adb shell am start -n "$PKG/$ACTIVITY" >/dev/null
settle 6
sample "6a. ön plana qayıdış (yenidən quruldu)"
trim RUNNING_CRITICAL
sample "6b. RUNNING_CRITICAL sonrası"

if [ "$RUN_COMPLETE" = "1" ]; then
  adb shell input keyevent KEYCODE_HOME
  settle 3
  trim COMPLETE
  sample "7. COMPLETE trim-dən sonra"
fi

echo
echo "CSV: $CSV"
echo
echo "Necə oxumalı:"
echo "  • 3 → 4: fona keçəndə nə qədəri rezident qalır."
echo "  • 4 → 5: onTrimMemory-nin geri qaytardığı yaddaş. Düzəlişdən əvvəl bu ~0 idi."
echo "  • 6a → 6b: ön plandakı təzyiqdə yalnız teksturaların qaytardığı hissə."
echo "  • ANON+SWAP sütunu Play-in vitals metrikidir — qərarı ona görə ver, PSS-ə görə yox."
