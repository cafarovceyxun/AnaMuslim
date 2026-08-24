#!/usr/bin/env python3
"""Faza 0 — dinləmə nümunəsi: bir surəni (və ya bir neçə hədisi) tam boru xətti ilə render edir.

    python3 tools/tts/sample.py --chapter 112 --voices Iapetus,Charon
    python3 tools/tts/sample.py --hadith 2,4

Nəticə: `build/tts/samples/…mp3`. Səs, sürət və prompt seçimi burada bağlanır —
tam render başlayandan sonra dəyişiklik bütün faylları yenidən çəkir.
"""
import argparse
import json
import wave
import sys

import config as C
import synthesize
from assemble import _encode, assemble_chapter, _ms, _pcm, _silence
from synthesize import item_chunks


def render(rows, name, is_hadith=False):
    for row in rows:
        for chunk in item_chunks(row["spoken"]):
            synthesize.synthesize_chunk(chunk)

    if is_hadith:
        import io
        buf = io.BytesIO()
        buf.write(_silence(C.LEAD_IN_MS))
        for row in rows:
            for i, chunk in enumerate(item_chunks(row["spoken"])):
                if i:
                    buf.write(_silence(C.HADITH_GAP_MS))
                buf.write(_pcm(chunk))
            buf.write(_silence(C.VERSE_GAP_MS))
        pcm = buf.getvalue()
        duration = _ms(len(pcm))
    else:
        pcm, _timings, duration = assemble_chapter(rows)

    out = C.BUILD / "samples" / f"{name}.mp3"

    try:
        _encode(pcm, out, C.QURAN_BITRATE)
    except SystemExit:
        # Dinləmə nümunəsi üçün mp3 şərt deyil — kodlayıcı yoxdursa WAV yaz.
        # (Surə faylları üçün yox: 24 saatlıq WAV bir neçə GB olardı.)
        out = out.with_suffix(".wav")
        out.parent.mkdir(parents=True, exist_ok=True)
        with wave.open(str(out), "wb") as w:
            w.setnchannels(C.CHANNELS)
            w.setsampwidth(C.SAMPLE_WIDTH)
            w.setframerate(C.SAMPLE_RATE)
            w.writeframes(pcm)
        print("  (mp3 kodlayıcı yoxdur → WAV yazıldı)")

    print(f"  {out}  ({duration / 1000:.1f} s)")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--chapter", type=int, default=112)
    ap.add_argument("--hadith", default="", help="vergüllə hədis id-ləri")
    ap.add_argument("--voices", default=C.VOICE, help="vergüllə səs adları")
    ap.add_argument("--tag", default="", help="fayl adına əlavə (üslub variantlarını ayırmaq üçün)")
    args = ap.parse_args()

    C.ensure_dirs()
    synthesize.require_credentials()
    data = json.loads(C.SOURCE_JSON.read_text(encoding="utf-8"))

    suffix = f"_{args.tag}" if args.tag else ""

    for voice in [v.strip() for v in args.voices.split(",") if v.strip()]:
        C.VOICE = voice                       # hash-a düşür, yəni səslər keşdə qarışmır
        print(f"səs: {voice}")
        if args.hadith:
            ids = {int(x) for x in args.hadith.split(",")}
            rows = [h for h in data["hadith"] if h["id"] in ids]
            render(rows, f"hadith_{'_'.join(map(str, sorted(ids)))}_{voice}{suffix}", is_hadith=True)
        else:
            rows = sorted([v for v in data["verses"] if v["chapter"] == args.chapter],
                          key=lambda v: v["verse"])
            render(rows, f"{args.chapter:03d}_{voice}{suffix}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
