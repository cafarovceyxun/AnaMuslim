"""Quran tərcüməsinin hazır Iapetus səslərindən (build/tts/cache) Piper dataseti qurur.

Hər ayə `synthesize.item_chunks` ilə necə parçalanıb render olunubsa, eyni parçalar burada da
çıxır və `cache_path(parça)` həmin parçanın wav faylını verir — yəni mətn ↔ səs cütü dəqiqdir,
heç bir tutuşdurma lazım deyil.

Çıxış: build/tts/quran_ds/wavs/q_SSS_VVV[_k].wav (22050 Hz, kənar sükut 0,2 s-ə qədər kəsilib)
       build/tts/quran_ds/metadata.csv  (`q_001_001.wav|mətn`)
       build/tts/quran_ds/skipped.tsv   (çıxarılanlar və səbəbi)

Uzun parçalar (>20 s, ~7 saat) atılmır: mətn durğu işarələrində ≤15 s-lik hissələrə bölünür,
kəsik yeri `split_batches.split_points_whisper` ilə (Whisper söz vaxtı + ən uzun sükut) tapılır.
Gözlənilən müddətdən çox fərqlənən hissə yenə də atılır.

İşlət: uv run --with mlx-whisper --with scipy python3 tools/tts/voice/build_quran_dataset.py
"""
import json
import math
import os
import sys
import wave
from pathlib import Path

import numpy as np
from scipy.signal import resample_poly

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "tools/tts"))
# Quran korpusu 2026-08-24-də Cloud TTS backend-i ilə render olunub; backend adı hash-in içindədir.
os.environ["TTS_BACKEND"] = "cloudtts"
from synthesize import cache_path, item_chunks  # noqa: E402

sys.path.insert(0, str(Path(__file__).resolve().parent))
import re  # noqa: E402
from split_batches import silences, split_points_whisper, weight  # noqa: E402

TARGET_SEC = 15.0
# SKIP_LONG=1 — uzunları bölmədən yalnız qısaları yaz (Whisper-siz, ~1 dəq): öyrətməni tez başlatmaq
# üçün; tam metadata.csv bölmə ilə işləyən əsas qaçışdan gəlir.
SKIP_LONG = os.environ.get("SKIP_LONG") == "1"


def text_pieces(text, dur):
    """Mətni cümlə/bənd sərhədlərində elə qruplaşdırır ki, hər hissə ~TARGET_SEC-dən uzun olmasın."""
    sents = [x.strip() for x in re.split(r"(?<=[.!?;:])\s+", text) if x.strip()]
    units = []
    per = dur / max(1, weight(text))
    for x in sents:                       # tək cümlə özü uzundursa vergüldən böl
        if weight(x) * per > MAX_SEC:
            units += [y.strip() for y in re.split(r"(?<=,)\s+", x) if y.strip()]
        else:
            units.append(x)
    out, cur = [], ""
    for u in units:
        cand = (cur + " " + u).strip()
        if cur and weight(cand) * per > TARGET_SEC:
            out.append(cur); cur = u
        else:
            cur = cand
    if cur:
        out.append(cur)
    return out

OUT = ROOT / "build/tts/quran_ds"
SR_OUT = 22050
MAX_SEC = 20.0      # uzun parçalar GPU yaddaşını aşır və batch-i ləngidir
MIN_SEC = 0.8
KEEP_SIL = 0.2
SIL_DB = -50.0


def read(p):
    with wave.open(str(p)) as w:
        sr = w.getframerate()
        a = np.frombuffer(w.readframes(w.getnframes()), "<i2").astype("float32") / 32768
    return a, sr


def trim(a, sr):
    hop = int(0.01 * sr)
    n = len(a) // hop
    if n == 0:
        return a
    db = 20 * np.log10(np.sqrt((a[: n * hop].reshape(n, hop) ** 2).mean(axis=1)) + 1e-9)
    loud = np.where(db > SIL_DB)[0]
    if len(loud) == 0:
        return a[:0]
    s = max(0, loud[0] * hop - int(KEEP_SIL * sr))
    e = min(len(a), (loud[-1] + 1) * hop + int(KEEP_SIL * sr))
    return a[s:e]


def split_long(sid, text, a, sr, skipped):
    texts = text_pieces(text, len(a) / sr)
    if len(texts) < 2:
        skipped.append(f"{sid}\tbölünmür\t{len(a) / sr:.1f}\t{text[:80]}"); return []
    runs, total = silences(a, sr)
    bounds = split_points_whisper(a, sr, runs, total, texts)
    if bounds is None:
        skipped.append(f"{sid}\twhisper tutuşmadı\t{len(a) / sr:.1f}\t{text[:80]}"); return []
    span = bounds[-1][1] - bounds[0][0]
    W = sum(weight(t) for t in texts)
    out = []
    for n, ((s, e), t) in enumerate(zip(bounds, texts)):
        pid = f"{sid}_p{n + 1}"
        exp = span * weight(t) / W
        prev_e = bounds[n - 1][1] if n else 0.0
        next_s = bounds[n + 1][0] if n + 1 < len(bounds) else len(a) / sr
        s0 = max(0, s - KEEP_SIL, (prev_e + s) / 2)
        e0 = min(len(a) / sr, e + KEEP_SIL, (e + next_s) / 2)
        d = e0 - s0
        if not 0.65 <= (e - s) / exp <= 1.5 or not MIN_SEC <= d <= MAX_SEC:
            skipped.append(f"{pid}\thissə uyğunsuz\t{d:.1f}\t{t[:80]}"); continue
        out.append((pid, t, a[int(s0 * sr):int(e0 * sr)]))
    return out


def main():
    (OUT / "wavs").mkdir(parents=True, exist_ok=True)
    verses = json.loads((ROOT / "build/tts/source.json").read_text("utf-8"))["verses"]
    meta, skipped, total = [], ["id\tsəbəb\tmüddət\tmətn"], 0.0
    n_split = 0
    for v in verses:
        chunks = item_chunks(v["spoken"])
        for k, text in enumerate(chunks):
            sid = f"q_{v['chapter']:03d}_{v['verse']:03d}" + (f"_{k + 1}" if len(chunks) > 1 else "")
            p = cache_path(text)
            # `"` Piper-in csv.reader-ini pozur (sətirlər birləşir) və oxunmur — silinir
            text = " ".join(text.replace('"', "").split())
            if not p.exists():
                skipped.append(f"{sid}\tkeşdə yoxdur\t\t{text[:80]}"); continue
            a, sr = read(p)
            a = trim(a, sr)
            dur = len(a) / sr
            if dur < MIN_SEC:
                skipped.append(f"{sid}\tqısa\t{dur:.1f}\t{text[:80]}"); continue
            if dur <= MAX_SEC:
                parts = [(sid, text, a)]
            elif SKIP_LONG:
                continue
            else:
                parts = split_long(sid, text, a, sr, skipped)
                n_split += 1
            for pid, ptext, pa in parts:
                gg = math.gcd(SR_OUT, sr)
                b = resample_poly(pa, SR_OUT // gg, sr // gg)
                with wave.open(str(OUT / "wavs" / f"{pid}.wav"), "wb") as w:
                    w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR_OUT)
                    w.writeframes((np.clip(b, -1, 1) * 32767).astype("<i2").tobytes())
                meta.append(f"{pid}.wav|{ptext.replace('|', ' ')}")
                total += len(pa) / sr
            if n_split and n_split % 50 == 0 and dur > MAX_SEC:
                print(f"bölünən uzun ayə: {n_split}", flush=True)
    suffix = ".short" if SKIP_LONG else ""
    (OUT / f"metadata{suffix}.csv").write_text("\n".join(meta) + "\n", "utf-8")
    if not SKIP_LONG:
        (OUT / "skipped.tsv").write_text("\n".join(skipped) + "\n", "utf-8")
    print(f"dataset: {len(meta)} parça, {total / 3600:.1f} saat → {OUT}")
    print(f"çıxarıldı: {len(skipped) - 1} (səbəblər: {OUT / 'skipped.tsv'})")


if __name__ == "__main__":
    main()
