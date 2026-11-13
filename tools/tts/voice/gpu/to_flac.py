"""Dataset wav-larını yükləmə üçün FLAC-a sıxır (itkisiz, ~40% kiçik). Təkrar işlədiləndə yalnız
yeni/dəyişmiş faylları çevirir. Serverdə `flac -d` geri açır (runpod.sh push).

İşlət: uv run --with soundfile python3 tools/tts/voice/gpu/to_flac.py build/tts/quran_ds
"""
import sys
from pathlib import Path

import soundfile as sf

src = Path(sys.argv[1])
wavs, flacs = src / "wavs", src / "flac"
flacs.mkdir(exist_ok=True)
n = 0
for w in sorted(wavs.glob("*.wav")):
    f = flacs / (w.stem + ".flac")
    if f.exists() and f.stat().st_mtime >= w.stat().st_mtime:
        continue
    a, sr = sf.read(w, dtype="int16")
    sf.write(f, a, sr, format="FLAC", subtype="PCM_16")
    n += 1
keep = {w.stem for w in wavs.glob("*.wav")}
gone = [f for f in flacs.glob("*.flac") if f.stem not in keep]
for f in gone:
    f.unlink()
print(f"çevrildi: {n}, silindi: {len(gone)}, cəmi flac: {len(keep)}")
