"""Bölünmüş cümlələri Whisper ilə dinləyib mətnə uyğunluğunu yoxlayır.

Hər `wavs/az_XXXX.wav` lokal Whisper (MLX, large-v3-turbo) ilə yazıya çevrilir və
`script.tsv`-dəki öz mətni ilə, həm də qonşu cümlələrlə müqayisə olunur. Kəsik yanlış
yerə düşübsə (söz qonşu hissəyə keçib) və ya Gemini cümləni buraxıb/təkrarlayıbsa oxşarlıq
düşür. Nəticə: build/tts/myvoice/verify_report.tsv (ən pisdən yaxşıya).

İşlət: uv run --with mlx-whisper --with scipy python3 tools/tts/voice/verify_whisper.py [--only az_0181-0200]
"""
import argparse
import difflib
import re
from pathlib import Path

import mlx_whisper
import numpy as np
import wave
from scipy.signal import resample_poly

ROOT = Path(__file__).resolve().parents[3]
BASE = ROOT / "build/tts/myvoice"
MODEL = "mlx-community/whisper-large-v3-turbo"


def load16k(path):
    """ffmpeg-siz: 24 kHz wav → 16 kHz float32 (Whisper-in gözlədiyi)."""
    with wave.open(str(path)) as w:
        sr = w.getframerate()
        a = np.frombuffer(w.readframes(w.getnframes()), "<i2").astype("float32") / 32768
    g = np.gcd(16000, sr)
    return resample_poly(a, 16000 // g, sr // g).astype("float32")


def norm(s):
    s = s.replace("I", "ı").replace("İ", "i").lower().replace("i̇", "i")
    return re.sub(r"[^a-zəöüçşğıxq]", "", s)


def sim(a, b):
    return difflib.SequenceMatcher(None, norm(a), norm(b), autojunk=False).ratio()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", help="yalnız bu qrup (məs. az_0181-0200)")
    args = ap.parse_args()

    script = dict(l.split("\t", 1) for l in (BASE / "script.tsv").read_text("utf-8").splitlines())
    ids = sorted(p.stem for p in (BASE / "wavs").glob("az_*.wav"))
    if args.only:
        a, b = map(int, re.findall(r"\d+", args.only))
        ids = [i for i in ids if a <= int(i[3:]) <= b]

    rows = []
    for n, i in enumerate(ids, 1):
        hyp = mlx_whisper.transcribe(load16k(BASE / "wavs" / f"{i}.wav"), path_or_hf_repo=MODEL,
                                     language="az", condition_on_previous_text=False)["text"].strip()
        k = int(i[3:])
        own = sim(hyp, script[i])
        nb = max((sim(hyp, script.get(f"az_{k + d:04d}", "")) for d in (-1, 1)), default=0)
        flag = "⚠️" if own < 0.75 or nb > own else ""
        rows.append((own, i, nb, flag, hyp, script[i]))
        if n % 25 == 0:
            print(f"{n}/{len(ids)}", flush=True)

    rows.sort()
    out = BASE / ("verify_report.tsv" if not args.only else f"verify_{args.only}.tsv")
    with out.open("w", encoding="utf-8") as f:
        f.write("oxşarlıq\tid\tqonşu\tqeyd\twhisper\tmətn\n")
        for own, i, nb, flag, hyp, ref in rows:
            f.write(f"{own:.2f}\t{i}\t{nb:.2f}\t{flag}\t{hyp}\t{ref}\n")
    bad = [r for r in rows if r[3]]
    print(f"yoxlandı: {len(rows)}, şübhəli: {len(bad)}, median oxşarlıq: {rows[len(rows) // 2][0]:.2f}")
    print(out)


if __name__ == "__main__":
    main()
