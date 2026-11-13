"""Qwen3-TTS fine-tune checkpoint-indən nümunə səs yaradır (serverdə).

/workspace/qwenv/bin/python qwen_sample.py /workspace/qwen/out/checkpoint-epoch-0 /workspace/qwen/samples/e0
"""
import os
import sys

import soundfile as sf
import torch
from qwen_tts import Qwen3TTSModel

ckpt, prefix = sys.argv[1], sys.argv[2]
os.makedirs(os.path.dirname(prefix), exist_ok=True)
TEXTS = [
    "Rəhmli, mərhəmətli Allahın adı ilə. Həmd olsun aləmlərin Rəbbi Allaha.",
    "Şübhəsiz ki, Allah səbr edənlərlə bərabərdir.",
    "Rəsulullah sallallahu aleyhi və səlləm buyurdu: Əməllər niyyətlərə görədir.",
    "Ey iman gətirənlər! Səbr və namazla Allahdan kömək diləyin.",
    "Bizə Yəhya ibn Bükeyr danışdı, dedi: Bizə Leys xəbər verdi.",
    "Bu gün havanın necə olacağını bilmirəm, amma axşam evdə olacağam.",
]
tts = Qwen3TTSModel.from_pretrained(ckpt, device_map="cuda:0", dtype=torch.bfloat16, attn_implementation="sdpa")
for i, t in enumerate(TEXTS, 1):
    # 12 Hz kod → 30 s = 360; model «dayan» tokenini öyrənməyibsə sonsuz uzanmasın
    wavs, sr = tts.generate_custom_voice(text=t, speaker="iapetus_az", max_new_tokens=400)
    sf.write(f"{prefix}_{i}.wav", wavs[0], sr)
    print(f"{prefix}_{i}.wav {len(wavs[0]) / sr:.1f}s", flush=True)
