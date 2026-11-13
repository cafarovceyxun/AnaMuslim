#!/usr/bin/env bash
# Kirayə GPU serverində (RunPod «PyTorch» şablonu, Ubuntu + CUDA) Piper öyrətmə mühitini qurur.
# Mac-dan `runpod.sh setup` çağırır — əl ilə işlətməyə ehtiyac yoxdur. Təkrar işlətmək təhlükəsizdir.
set -euo pipefail

W=/workspace/aztts
PIPER_COMMIT=0e36946d4c54d95bda0c2e766f3ca32a76fc7693   # Mac-da sınanmış versiya
CKPTS="https://huggingface.co/datasets/rhasspy/piper-checkpoints/resolve/main"

export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get install -y -qq build-essential cmake ninja-build git rsync tmux curl flac python3-venv >/dev/null

mkdir -p "$W"/{base,data,runs,onnx}
cd "$W"

if [ ! -d piper1-gpl ]; then
    git clone -q https://github.com/OHF-Voice/piper1-gpl.git
fi
cd piper1-gpl
git fetch -q origin "$PIPER_COMMIT" 2>/dev/null || true
git checkout -q -f "$PIPER_COMMIT"
git apply "$W/scripts/piper-local.patch"

if [ ! -d .venv ]; then
    python3 -m venv .venv
fi
source .venv/bin/activate
pip install -q --upgrade pip
pip install -q cmake ninja scikit-build setuptools wheel cython
pip install -q -e '.[train]' onnxscript
./build_monotonic_align.sh >/dev/null
python setup.py build_ext --inplace >/dev/null    # Linux-da espeak-ng mənbədən normal qurulur

# azərbaycanca fonemləşdirmə işləyirmi
python - <<'EOF'
from piper.phonemize_espeak import EspeakPhonemizer
print("az fonemlər:", "".join(EspeakPhonemizer().phonemize("az", "Rəhmli, mərhəmətli Allahın adı ilə.")[0]))
EOF
python -c "import torch; print('CUDA:', torch.cuda.is_available(), torch.cuda.get_device_name(0) if torch.cuda.is_available() else '')"

cd "$W/base"
# Baza: LJSpeech medium — datası public domain, yəni model lisenziya şərti daşımır.
[ -s lj_medium.ckpt ] || curl -sfL -o lj_medium.ckpt "$CKPTS/en/en_US/ljspeech/medium/lj-med_1000.ckpt"
# Türkcə DFKI (CC BY-NC-SA) yalnız istənəndə: BASE_DFKI=1 runpod.sh setup
if [ "${BASE_DFKI:-0}" = 1 ]; then
    [ -s tr_dfki.ckpt ] || curl -sfL -o tr_dfki.ckpt "$CKPTS/tr/tr_TR/dfki/medium/epoch%3D5679-step%3D1489110.ckpt"
fi
ls -la "$W/base"
echo "QURULDU"
