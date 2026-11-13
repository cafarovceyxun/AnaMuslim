"""Öz səsinlə TTS dataseti yazmaq üçün sadə terminal yazıcısı.

`select_sentences.py`-ın verdiyi `build/tts/myvoice/script.tsv`-dəki cümlələri bir-bir göstərir,
səsi `build/tts/myvoice/wavs/<id>.wav`-a yazır (48 kHz, mono, 16-bit). Dayanıb sonra yenidən
açanda yazılmamış ilk cümlədən davam edir.

İşlət:
    uv run --with sounddevice --with numpy python3 tools/tts/voice/record.py
    ... record.py --list            # mikrofonları göstər
    ... record.py --device 2        # konkret mikrofon

Hər cümlə üçün: Enter → danış → Enter (saxlanılır, növbəti çıxır).
Növbəti cümlənin sorğusunda: r = əvvəlkini yenidən yaz, p = əvvəlkini dinlə,
s = bu cümləni keç (oxunmur/səhv mətn), q = çıx.
"""
import argparse
import sys
import time
import wave
from pathlib import Path

import numpy as np
import sounddevice as sd

ROOT = Path(__file__).resolve().parents[3]
BASE = ROOT / "build/tts/myvoice"
SCRIPT = BASE / "script.tsv"
WAVS = BASE / "wavs"
SKIPPED = BASE / "skipped.txt"
SR = 48000

BOLD, DIM, YEL, GRN, RST = "\033[1m", "\033[2m", "\033[33m", "\033[32m", "\033[0m"


def load_script():
    rows = []
    for line in SCRIPT.read_text("utf-8").splitlines():
        if "\t" in line:
            i, t = line.split("\t", 1)
            rows.append((i, t))
    return rows


def skipped():
    return set(SKIPPED.read_text().split()) if SKIPPED.exists() else set()


def record(device):
    chunks = []

    def cb(indata, frames, t, status):
        chunks.append(indata.copy())

    with sd.InputStream(samplerate=SR, channels=1, dtype="float32", device=device, callback=cb):
        input(f"  {GRN}● yazılır…{RST} bitəndə Enter ")
        time.sleep(0.25)                       # son hecanın quyruğu kəsilməsin
    return np.concatenate(chunks)[:, 0] if chunks else np.zeros(0, dtype="float32")


def save(path, audio):
    pcm = (np.clip(audio, -1, 1) * 32767).astype("<i2")
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes(pcm.tobytes())


def read(path):
    with wave.open(str(path)) as w:
        return np.frombuffer(w.readframes(w.getnframes()), "<i2").astype("float32") / 32767


def report(audio):
    dur = len(audio) / SR
    peak = float(np.abs(audio).max()) if len(audio) else 0.0
    msg = f"  {DIM}{dur:.1f} san, pik {peak:.2f}{RST}"
    if peak >= 0.98:
        msg += f"  {YEL}⚠️ çox yüksəkdir (kəsilib) — mikrofondan bir az uzaqlaş, r ilə təkrarla{RST}"
    elif peak < 0.08:
        msg += f"  {YEL}⚠️ çox zəifdir — yaxınlaş və ya giriş səsini artır{RST}"
    if dur < 1.0:
        msg += f"  {YEL}⚠️ çox qısadır{RST}"
    print(msg)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--list", action="store_true")
    ap.add_argument("--device", type=int, default=None)
    args = ap.parse_args()

    if args.list:
        print(sd.query_devices())
        return
    if not SCRIPT.exists():
        sys.exit(f"{SCRIPT} yoxdur — əvvəl select_sentences.py işlət")

    WAVS.mkdir(parents=True, exist_ok=True)
    rows = load_script()
    dev = sd.query_devices(args.device, "input")
    print(f"Mikrofon: {BOLD}{dev['name']}{RST}")

    prev = None
    while True:
        skip = skipped()
        todo = [(i, t) for i, t in rows if not (WAVS / f"{i}.wav").exists() and i not in skip]
        if not todo:
            print("Hamısı yazılıb. 🎉")
            return
        done = len(rows) - len(todo) - len(skip & {i for i, _ in rows})
        secs = sum(len(read(p)) for p in WAVS.glob("*.wav")) / SR
        i, text = todo[0]
        print(f"\n{DIM}[{done}/{len(rows)} · {secs / 60:.0f} dəq yazılıb]  {i}{RST}")
        print(f"{BOLD}{text}{RST}")
        cmd = input("  Enter = yaz   r/p = əvvəlkini təkrarla/dinlə   s = keç   q = çıx  › ").strip().lower()

        if cmd == "q":
            return
        if cmd == "s":
            with SKIPPED.open("a") as f:
                f.write(i + "\n")
            continue
        if cmd in ("r", "p") and prev is None:
            print("  əvvəlki yazı yoxdur")
            continue
        if cmd == "p":
            sd.play(read(WAVS / f"{prev}.wav"), SR); sd.wait()
            continue
        target = prev if cmd == "r" else i
        if cmd == "r":
            print(f"  {DIM}yenidən: {dict(rows)[prev]}{RST}")
        audio = record(args.device)
        save(WAVS / f"{target}.wav", audio)
        report(audio)
        prev = target


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print()
