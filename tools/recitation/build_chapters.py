#!/usr/bin/env python3
"""EveryAyah-ın ayə-ayə fayllarından surə faylı + ayə vaxt cədvəli qurur.

    python3 tools/recitation/build_chapters.py Maher_AlMuaiqly_64kbps --id maher
    python3 tools/recitation/build_chapters.py Maher_AlMuaiqly_64kbps --id maher --only 1,36,112

Niyə: pleyerimiz surəni bir fayl kimi çalır və ayəni vaxt cədvəli ilə tapır
(`ChapterTimingMetadata`). EveryAyah-dakı qarilərin çoxunun surə səviyyəsində cədvəli
yoxdur — ayə fayllarını birləşdirəndə isə cədvəl **quruluşdan** çıxır.

Addımlar (yalnız macOS: `afconvert` Apple-ın AAC kodlayıcısıdır, ffmpeg lazım deyil):
  1. ayə fayllarını yüklə (keşlənir; `build/recitation/src/<folder>/`);
  2. hər birini ortaq PCM formatına aç — ⚠️ eyni qarinin faylları eyni formatda DEYİL
     (Maher-in 001001-i 44.1 kHz stereo 202 kbps, qalanı 22 kHz mono/stereo 64 kbps);
  3. PCM-i yapışdır (tikişlərdə 5 ms keçid, bax `fade_edges`), hər ayənin başlanğıcını
     **nümunə sayından** yaz;
  4. bütöv surəni bir dəfə HE-AAC-yə kodla (`.m4a`). MP3-ləri birbaşa yapışdırmaq hər
     tikişdə kodlayıcı gecikməsi/doldurma qoyur — həm çıqqıltı, həm də cədvəl sürüşməsi.

Bəsmələ: 1 və 9-dan başqa surələrin əvvəlinə həmin qarinin 001001-i qoyulur, 1-ci ayə
ondan sonra başlayır (quranicaudio surə faylları da belədir).

Çıxış: `build/recitation/out/<id>/NNN.m4a` + `build/recitation/out/<id>.json`
(`composeResources/files/recitation_timings/basfar.json` ilə eyni format).
"""
import argparse
import array
import concurrent.futures as cf
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import time
import urllib.request
import wave

ROOT = Path(__file__).resolve().parents[2]
BUILD = ROOT / "build" / "recitation"

VERSE_COUNTS = [
    7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
    112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53,
    89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12,
    12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19, 36, 25, 22, 17, 19, 26,
    30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6,
]
assert len(VERSE_COUNTS) == 114 and sum(VERSE_COUNTS) == 6236

SAMPLE_RATE = 44100   # HE-AAC SBR üçün; 22 kHz-də əsas zolaq 11 kHz-ə düşür
SAMPLE_WIDTH = 2


def ayah_name(surah: int, ayah: int) -> str:
    return f"{surah:03d}{ayah:03d}.mp3"


def download(folder: str, name: str) -> Path:
    path = BUILD / "src" / folder / name
    if path.exists() and path.stat().st_size > 0:
        return path
    path.parent.mkdir(parents=True, exist_ok=True)
    url = f"https://everyayah.com/data/{folder}/{name}"
    for attempt in range(4):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "curl/8.7.1"})
            with urllib.request.urlopen(req, timeout=60) as r:
                data = r.read()
            if not data:
                raise IOError("boş cavab")
            tmp = path.with_suffix(".part")
            tmp.write_bytes(data)
            tmp.rename(path)
            return path
        except Exception as e:  # noqa: BLE001 — şəbəkə xətası, təkrar sına
            if attempt == 3:
                raise SystemExit(f"yüklənmədi: {url} ({e})")
            time.sleep(2 * (attempt + 1))
    raise AssertionError


def decode(mp3: Path, channels: int, tmpdir: Path) -> bytes:
    wav = tmpdir / (mp3.stem + ".wav")
    subprocess.run(
        ["afconvert", "-f", "WAVE", "-d", f"LEI16@{SAMPLE_RATE}", "-c", str(channels),
         str(mp3), str(wav)],
        check=True, capture_output=True,
    )
    with wave.open(str(wav), "rb") as w:
        assert w.getframerate() == SAMPLE_RATE and w.getnchannels() == channels
        frames = w.readframes(w.getnframes())
    wav.unlink()
    return fade_edges(frames, channels)


FADE_MS = 5


def fade_edges(pcm: bytes, channels: int) -> bytes:
    """Hər ayə faylının kənarlarına 5 ms-lik xətti keçid qoyur — uzunluğu dəyişmir.

    EveryAyah-da bəzi ayələr sükutsuz, sıx kəsilib (Maher, əl-Bəqərə: 285 tikişdən 8-i —
    2:33, 2:142 …): sükutsuz iki dalğa bir-birinə yapışanda tikişdə çıqqıltı olur.
    """
    samples = array.array("h", pcm)
    n_frames = len(samples) // channels
    n = min(int(SAMPLE_RATE * FADE_MS / 1000), n_frames // 2)
    for i in range(n):
        g = i / n
        for c in range(channels):
            head = i * channels + c
            tail = (n_frames - 1 - i) * channels + c
            samples[head] = int(samples[head] * g)
            samples[tail] = int(samples[tail] * g)
    return samples.tobytes()


def build_chapter(folder: str, rid: str, surah: int, channels: int, bitrate: int,
                  codec: str) -> dict:
    items = []  # (ayah no or 0 for basmala, mp3 path)
    if surah not in (1, 9):
        items.append((0, download(folder, ayah_name(1, 1))))
    for ayah in range(1, VERSE_COUNTS[surah - 1] + 1):
        items.append((ayah, download(folder, ayah_name(surah, ayah))))

    frame_bytes = SAMPLE_WIDTH * channels
    out_dir = BUILD / "out" / rid
    out_dir.mkdir(parents=True, exist_ok=True)
    starts = {}
    with tempfile.TemporaryDirectory() as td:
        td = Path(td)
        wav_path = td / f"{surah:03d}.wav"
        total = 0
        with wave.open(str(wav_path), "wb") as out:
            out.setnchannels(channels)
            out.setsampwidth(SAMPLE_WIDTH)
            out.setframerate(SAMPLE_RATE)
            for ayah, mp3 in items:
                pcm = decode(mp3, channels, td)
                if ayah:
                    starts[ayah] = total
                out.writeframes(pcm)
                total += len(pcm) // frame_bytes
        m4a = out_dir / f"{surah:03d}.m4a"
        subprocess.run(
            ["afconvert", "-f", "m4af", "-d", codec, "-b", str(bitrate), "-s", "0",
             str(wav_path), str(m4a)],
            check=True, capture_output=True,
        )
        # Yoxlama üçün PCM-in özü də saxlanıla bilər (`--keep-wav`).
        if ARGS.keep_wav:
            (out_dir / "wav").mkdir(exist_ok=True)
            wav_path.rename(out_dir / "wav" / wav_path.name)

    to_ms = lambda frames: round(frames * 1000 / SAMPLE_RATE)  # noqa: E731
    count = VERSE_COUNTS[surah - 1]
    verses = []
    for ayah in range(1, count + 1):
        end = starts[ayah + 1] if ayah < count else total
        verses.append({"verse": ayah, "start_ms": to_ms(starts[ayah]), "end_ms": to_ms(end)})
    return {"chapter": surah, "duration_ms": to_ms(total), "verses": verses}


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("folder", help="EveryAyah qovluğu, məs. Maher_AlMuaiqly_64kbps")
    p.add_argument("--id", required=True, help="tətbiqdəki reciter id")
    p.add_argument("--only", help="vergüllə surə nömrələri (defolt: 1..114)")
    p.add_argument("--channels", type=int, default=2)
    p.add_argument("--bitrate", type=int, default=48000)
    p.add_argument("--codec", default="aach", help="aach = HE-AAC, aac = AAC-LC")
    p.add_argument("--jobs", type=int, default=4)
    p.add_argument("--keep-wav", action="store_true")
    global ARGS
    ARGS = p.parse_args()

    surahs = [int(s) for s in ARGS.only.split(",")] if ARGS.only else list(range(1, 115))
    with cf.ThreadPoolExecutor(ARGS.jobs) as ex:
        futures = {
            s: ex.submit(build_chapter, ARGS.folder, ARGS.id, s, ARGS.channels, ARGS.bitrate,
                         ARGS.codec)
            for s in surahs
        }
        chapters = []
        for s in surahs:
            chapters.append(futures[s].result())
            print(f"surə {s:3d}: {chapters[-1]['duration_ms'] / 1000:8.1f} s", flush=True)

    out = BUILD / "out" / f"{ARGS.id}.json"
    out.write_text(json.dumps({"version": 1, "chapters": chapters}, separators=(", ", ": ")))
    print(f"→ {out}", file=sys.stderr)


ARGS = None

if __name__ == "__main__":
    main()
