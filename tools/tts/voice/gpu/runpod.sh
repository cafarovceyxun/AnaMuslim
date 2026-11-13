#!/usr/bin/env bash
# Kirayə GPU-da (RunPod və ya hər hansı SSH-li Linux + NVIDIA) Piper öyrətməsini Mac-dan idarə edir.
#
#   runpod.sh target root@1.2.3.4 22077   # serveri yadda saxla (RunPod → Connect → «SSH over exposed TCP»)
#   runpod.sh push                        # skriptləri + dataseti göndər (DATA=quran defolt, DATA=myvoice)
#   runpod.sh setup                       # Piper-i serverdə qur (~5–10 dəq)
#   runpod.sh train [EPOCHS=2000 BATCH=32 RUN=az-v1 INIT=...]   # tmux-da başlat
#   runpod.sh status                      # epoxa, GPU yükü
#   runpod.sh log                         # son log sətirləri
#   runpod.sh sample                      # serverdə ONNX çıxar, Mac-a çək, nümunə səs yarat
#   runpod.sh ssh                         # serverə daxil ol
#   runpod.sh speed                       # şəbəkə: Mac→server yükləmə, server←HF/GitHub endirmə
#
# ⚠️ Öyrətmə bitəndən sonra RunPod panelində pod-u STOP/TERMINATE et — yoxsa saat pulu gedir.
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/../../../.." && pwd)
HERE="$ROOT/tools/tts/voice/gpu"
LOCAL="$ROOT/build/tts/piper"
MYVOICE="$ROOT/build/tts/myvoice"
TARGET_FILE="$LOCAL/runpod_target"
R=/workspace/aztts

cmd=${1:-}; shift || true

if [ "$cmd" = target ]; then
    [ $# -eq 2 ] || { echo "istifadə: runpod.sh target root@IP PORT"; exit 1; }
    mkdir -p "$LOCAL"; echo "$1 $2" > "$TARGET_FILE"; echo "yadda saxlandı: $1 -p $2"; exit 0
fi
[ -f "$TARGET_FILE" ] || { echo "əvvəl: runpod.sh target root@IP PORT"; exit 1; }
read -r HOST PORT < "$TARGET_FILE"
SSH=(ssh -p "$PORT" -o StrictHostKeyChecking=accept-new -o ServerAliveInterval=30 "$HOST")
RSH="ssh -p $PORT -o StrictHostKeyChecking=accept-new"

case "$cmd" in
push)
    DATA=${DATA:-quran}
    "${SSH[@]}" "command -v rsync >/dev/null && command -v flac >/dev/null || (apt-get update -qq && apt-get install -y -qq rsync flac >/dev/null); mkdir -p $R/scripts $R/data/wavs $R/data/flac"
    rsync -az -e "$RSH" "$HERE/setup.sh" "$HERE/train.sh" "$HERE/piper-local.patch" "$HOST:$R/scripts/"
    if [ "$DATA" = quran ]; then
        SRC="$ROOT/build/tts/quran_ds"            # tools/tts/voice/build_quran_dataset.py
        # bölmə hələ gedirsə tam metadata.csv yoxdur — qısa ayələrlə (metadata.short.csv) başla
        META="$SRC/metadata.csv"; [ -f "$META" ] || META="$SRC/metadata.short.csv"
        [ -f "$META" ] || { echo "$SRC-də metadata yoxdur"; exit 1; }
        cp "$META" "$LOCAL/metadata.upload.csv"
        # FLAC (itkisiz, ~40% kiçik) göndərilir, serverdə wav-a açılır — Bakıdan yükləmə dar boğazdır
        uv run -q --with soundfile python3 "$HERE/to_flac.py" "$SRC"
        rsync -a --delete -e "$RSH" "$SRC/flac/" "$HOST:$R/data/flac/"
        "${SSH[@]}" "cd $R/data/flac && ls *.flac | sed 's/\.flac$//' | sort > /tmp/f.lst && (cd ../wavs && ls 2>/dev/null | sed 's/\.wav$//' | sort) > /tmp/w.lst;
            comm -23 /tmp/f.lst /tmp/w.lst | sed 's/$/.flac/' | xargs -r -P 8 -n 200 flac -d -s -f --output-prefix=../wavs/;
            comm -13 /tmp/f.lst /tmp/w.lst | sed 's/$/.wav/' | (cd ../wavs && xargs -r rm -f);
            echo \"serverdə wav: \$(ls ../wavs | wc -l)\""
    else
        SRC="$MYVOICE"
        [ -f "$SRC/metadata.csv" ] || { echo "$SRC/metadata.csv yoxdur"; exit 1; }
        sed 's/^\(az_[0-9]*\)|/\1.wav|/' "$SRC/metadata.csv" > "$LOCAL/metadata.upload.csv"
        rsync -az --delete -e "$RSH" "$SRC/wavs/" "$HOST:$R/data/wavs/"
    fi
    # Piper metadata-nı csv.reader ilə oxuyur: `"` ilə başlayan mətn növbəti sətirləri udur və
    # bir neçə ayə bir «cümləyə» yığılır (3000+ fonem → GPU yaddaşı partlayır). Dırnaq oxunmur, silinir.
    sed -i '' 's/"//g' "$LOCAL/metadata.upload.csv"
    python3 -c "import csv,sys; r=list(csv.reader(open(sys.argv[1],encoding='utf-8'),delimiter='|')); n=sum(1 for _ in open(sys.argv[1],encoding='utf-8')); assert len(r)==n and all(len(x)==2 for x in r), (len(r),n)" "$LOCAL/metadata.upload.csv"
    rsync -az -e "$RSH" "$LOCAL/metadata.upload.csv" "$HOST:$R/data/metadata.csv"
    n=$(wc -l < "$LOCAL/metadata.upload.csv" | tr -d ' ')
    echo "göndərildi: $n parça"
    ;;
setup)
    "${SSH[@]}" "BASE_DFKI=${BASE_DFKI:-0} bash $R/scripts/setup.sh"
    ;;
train)
    envs=""
    for kv in "$@"; do envs+="$kv "; done
    "${SSH[@]}" "tmux has-session -t train 2>/dev/null && { echo 'öyrətmə artıq gedir (runpod.sh status)'; exit 1; }; tmux new -d -s train 'env $envs bash $R/scripts/train.sh'; sleep 2; echo başladı"
    ;;
status)
    "${SSH[@]}" "tmux has-session -t train 2>/dev/null && echo 'tmux: işləyir' || echo 'tmux: dayanıb';
        L=\$(ls -t $R/runs/*/train.log 2>/dev/null | head -1); [ -n \"\$L\" ] && tail -c 4000 \"\$L\" | tr '\r' '\n' | grep -E '^Epoch [0-9]+' | tail -1 | cut -c1-140;
        nvidia-smi --query-gpu=name,utilization.gpu,memory.used,memory.total --format=csv,noheader"
    ;;
log)
    "${SSH[@]}" "L=\$(ls -t $R/runs/*/train.log | head -1); tail -c 6000 \"\$L\" | tr '\r' '\n' | grep -v '^\s*$' | tail -25"
    ;;
sample)
    RUN=${RUN:-az-v1}
    stamp=$(date +%m%d-%H%M)
    "${SSH[@]}" "cd $R/piper1-gpl && source .venv/bin/activate && C=\$(ls -t $R/runs/$RUN/lightning_logs/*/checkpoints/last.ckpt | head -1) &&
        python -m piper.train.export_onnx --checkpoint \"\$C\" --output-file $R/onnx/$RUN.onnx 2>&1 | grep -E 'Exported|Error' ;
        cp $R/runs/$RUN/config.json $R/onnx/$RUN.onnx.json"
    mkdir -p "$LOCAL/onnx" "$LOCAL/samples"
    rsync -az -e "$RSH" "$HOST:$R/onnx/$RUN.onnx" "$HOST:$R/onnx/$RUN.onnx.json" "$LOCAL/onnx/"
    "$LOCAL/piper1-gpl/.venv/bin/python" - "$LOCAL/onnx/$RUN.onnx" "$LOCAL/samples/$RUN-$stamp" <<'EOF'
import sys, wave
from piper import PiperVoice
v = PiperVoice.load(sys.argv[1])
texts = [
    "Rəhmli, mərhəmətli Allahın adı ilə. Həmd olsun aləmlərin Rəbbi Allaha.",
    "Şübhəsiz ki, Allah səbr edənlərlə bərabərdir.",
    "Rəsulullah sallallahu aleyhi və səlləm buyurdu: Əməllər niyyətlərə görədir.",
    "Ey iman gətirənlər! Səbr və namazla Allahdan kömək diləyin.",
    "Bizə Yəhya ibn Bükeyr danışdı, dedi: Bizə Leys xəbər verdi.",
    "Bu gün havanın necə olacağını bilmirəm, amma axşam evdə olacağam.",
]
for i, t in enumerate(texts, 1):
    with wave.open(f"{sys.argv[2]}_{i}.wav", "wb") as w:
        v.synthesize_wav(t, w)
print("nümunələr:", f"{sys.argv[2]}_1..{len(texts)}.wav")
EOF
    ;;
speed)
    f=$(mktemp); dd if=/dev/urandom of="$f" bs=1m count=8 2>/dev/null
    t0=$(python3 -c 'import time;print(time.time())')
    rsync -a -e "$RSH" "$f" "$HOST:/tmp/speedtest.bin"
    t1=$(python3 -c 'import time;print(time.time())'); rm -f "$f"
    python3 -c "print('Mac → server: %.0f KB/s' % (8192/($t1-$t0)))"
    "${SSH[@]}" "rm -f /tmp/speedtest.bin; for u in https://huggingface.co/datasets/rhasspy/piper-checkpoints/resolve/main/en/en_US/ljspeech/medium/lj-med_1000.ckpt https://codeload.github.com/OHF-Voice/piper1-gpl/tar.gz/refs/heads/main; do
        curl -sL -o /dev/null -w \"%{speed_download}\" --max-time 10 \$u | awk -v u=\$u '{printf \"server ← %s: %.0f KB/s\\n\", substr(u,9,20), \$1/1024}'; done"
    ;;
ssh)
    exec "${SSH[@]}"
    ;;
*)
    sed -n '2,13p' "$0"; exit 1
    ;;
esac
