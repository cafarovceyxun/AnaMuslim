"""Piper dataseti (22050 Hz wav + metadata.csv) → Qwen3-TTS fine-tune girişi (24 kHz wav + train_raw.jsonl).

Qwen-in ref_mel çıxarışı yalnız 24 kHz qəbul edir. Referans səs bütün nümunələr üçün eynidir (README:
spiker sabitliyi üçün tövsiyə) — ~10 s-lik, sürəti median olan bir klip avtomatik seçilir.

Serverdə: /workspace/qwenv/bin/python qwen_prepare.py /workspace/aztts/data /workspace/qwen/data
"""
import csv
import json
import os
import statistics
import sys
from concurrent.futures import ProcessPoolExecutor

import librosa
import soundfile as sf

SRC, DST = sys.argv[1], sys.argv[2]
os.makedirs(f"{DST}/wavs24", exist_ok=True)


def conv(name):
    out = f"{DST}/wavs24/{name}"
    if not os.path.exists(out):
        a, _ = librosa.load(f"{SRC}/wavs/{name}", sr=24000)
        sf.write(out, a, 24000, subtype="PCM_16")
    return name, sf.info(out).duration


rows = list(csv.reader(open(f"{SRC}/metadata.csv", encoding="utf-8"), delimiter="|"))
with ProcessPoolExecutor(12) as ex:
    dur = dict(ex.map(conv, [r[0] for r in rows], chunksize=64))

rate = {f: len(t) / dur[f] for f, t in rows}
med = statistics.median(rate.values())
ref = min((f for f, _ in rows if 9 <= dur[f] <= 12), key=lambda f: abs(rate[f] - med))
ref_path = f"{DST}/wavs24/{ref}"

with open(f"{DST}/train_raw.jsonl", "w", encoding="utf-8") as fo:
    for f, t in rows:
        fo.write(json.dumps({"audio": f"{DST}/wavs24/{f}", "text": t.replace('"', ""),
                             "ref_audio": ref_path}, ensure_ascii=False) + "\n")
print(f"{len(rows)} sətir, cəmi {sum(dur.values()) / 3600:.1f} saat; referans: {ref} ({dur[ref]:.1f} s)")
print("referans mətn:", dict(rows)[ref])
