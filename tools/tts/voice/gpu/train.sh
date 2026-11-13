#!/usr/bin/env bash
# Serverdə Piper öyrətməsi. `runpod.sh train` bunu tmux içində başladır (bağlantı kəsilsə də işləyir).
# Yarıda dayanıbsa eyni əmr son `last.ckpt`-dən davam edir.
#
# Dəyişənlər (runpod.sh ötürür):
#   RUN     qaçışın adı                (defolt az-v1)
#   EPOCHS  epoxa sayı                 (defolt 300 — 20 saatlıq Quran datası üçün; ~1 saatlıq
#                                       data olsaydı 2000)
#   BATCH   batch ölçüsü               (defolt 32; 4090-un 24 GB-ı üçün)
#   INIT    başlanğıc checkpoint       (defolt LJSpeech medium — public domain)
set -euo pipefail

W=/workspace/aztts
RUN=${RUN:-az-v1}
EPOCHS=${EPOCHS:-300}
BATCH=${BATCH:-32}
INIT=${INIT:-$W/base/lj_medium.ckpt}
OUT=$W/runs/$RUN
mkdir -p "$OUT"

cd "$W/piper1-gpl"
source .venv/bin/activate

LAST=$(ls -t "$OUT"/lightning_logs/*/checkpoints/last.ckpt 2>/dev/null | head -1 || true)
if [ -n "$LAST" ]; then
    echo "davam edir: $LAST"
    START=(--ckpt_path "$LAST")
else
    echo "başlayır: $INIT"
    START=(--model.warmstart_ckpt "$INIT")
fi

python -m piper.train fit \
    --data.voice_name "$RUN" \
    --data.csv_path "$W/data/metadata.csv" \
    --data.audio_dir "$W/data/wavs" \
    --model.sample_rate 22050 \
    --data.espeak_voice az \
    --data.cache_dir "$W/cache" \
    --data.config_path "$OUT/config.json" \
    --data.batch_size "$BATCH" \
    --data.num_workers 8 \
    --trainer.accelerator gpu \
    --trainer.devices 1 \
    --trainer.max_epochs "$EPOCHS" \
    --trainer.check_val_every_n_epoch 10 \
    --trainer.default_root_dir "$OUT" \
    "${START[@]}" 2>&1 | tee -a "$OUT/train.log"

echo "BİTDİ $(date)" | tee -a "$OUT/train.log"
