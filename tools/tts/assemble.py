#!/usr/bin/env python3
"""Keşdəki parçaları surə/hədis fayllarına yığır və vaxt cədvəlini çıxarır.

    python3 tools/tts/assemble.py --kind quran     # 114 mp3 + tts_az_timings.json
    python3 tools/tts/assemble.py --kind hadith    # hədis başına mp3 + manifest
    python3 tools/tts/assemble.py --only 1,2,112   # yalnız bu surələr

Vaxtlar **PCM sample sayından** hesablanır (mp3 kodlayıcı gecikməsi hesaba
qatılmır — 600 ms fasilə onu udur), ona görə `ChapterTimingMetadata` ilə tam üst-üstə düşür.

Birləşmiş ayələr (`-3, 4: …` — 11 hadisə) bazada boş sətir kimi qalır; onların
audiosu ortağı ilə eynidir, ona görə pəncərə bərabər hissələrə bölünür — üst-üstə
düşən pəncərə `getVerseAtPosition` ikili axtarışını pozardı.
"""
import argparse
import gzip
import io
import json
import shutil
import subprocess
import sys
import wave

import config as C
from synthesize import cache_path, item_chunks


def _pcm(text: str) -> bytes:
    path = cache_path(text)
    if not path.exists():
        raise SystemExit(f"keşdə yoxdur: {path.name} — əvvəlcə synthesize.py işlət")
    with wave.open(str(path), "rb") as w:
        if w.getframerate() != C.SAMPLE_RATE or w.getnchannels() != C.CHANNELS:
            raise SystemExit(f"gözlənilməz format: {path.name}")
        return w.readframes(w.getnframes())


def _silence(ms: int) -> bytes:
    return b"\x00" * (int(C.SAMPLE_RATE * ms / 1000) * C.SAMPLE_WIDTH * C.CHANNELS)


def _ms(nbytes: int) -> int:
    return round(nbytes / (C.SAMPLE_RATE * C.SAMPLE_WIDTH * C.CHANNELS) * 1000)


def _encode(pcm: bytes, out_path, bitrate: str):
    """PCM → mp3. `ffmpeg` varsa onunla, yoxsa `lameenc` (pip) ilə.

    macOS-un öz `afconvert`-i mp3 **kodlaya bilmir** (yalnız dekod), ona görə iki yoldan
    biri lazımdır:
        pip3 install lameenc        # kiçik, Homebrew tələb etmir
        brew install ffmpeg         # Homebrew varsa
    """
    out_path.parent.mkdir(parents=True, exist_ok=True)
    kbps = int(bitrate.rstrip("k"))

    if shutil.which("ffmpeg"):
        subprocess.run(
            ["ffmpeg", "-hide_banner", "-loglevel", "error",
             "-f", "s16le", "-ar", str(C.SAMPLE_RATE), "-ac", str(C.CHANNELS), "-i", "pipe:0",
             "-codec:a", "libmp3lame", "-b:a", bitrate, "-y", str(out_path)],
            input=pcm, check=True,
        )
        return

    try:
        import lameenc
    except ImportError:
        raise SystemExit(
            "mp3 kodlayıcı yoxdur. Biri kifayətdir:\n"
            "  pip3 install lameenc\n"
            "  brew install ffmpeg"
        ) from None

    encoder = lameenc.Encoder()
    encoder.set_bit_rate(kbps)
    encoder.set_in_sample_rate(C.SAMPLE_RATE)
    encoder.set_channels(C.CHANNELS)
    encoder.set_quality(2)          # 0 ən yaxşı, 9 ən sürətli
    encoder.silence()

    data = encoder.encode(pcm) + encoder.flush()
    tmp = out_path.with_suffix(".tmp")
    tmp.write_bytes(bytes(data))
    tmp.replace(out_path)           # yarımçıq fayl qalmasın


def _split_shared_window(start_ms: int, end_ms: int, count: int):
    """Bir audio pəncərəsini `count` bitişik, üst-üstə düşməyən hissəyə bölür."""
    step = max(1, (end_ms - start_ms) // count)
    bounds = []
    for i in range(count):
        s = start_ms + i * step
        e = end_ms if i == count - 1 else start_ms + (i + 1) * step
        bounds.append((s, max(s + 1, e)))
    return bounds


def assemble_chapter(verses):
    """[{verse, spoken}] → (pcm, [{verse,start_ms,end_ms}], duration_ms)."""
    buf = io.BytesIO()
    buf.write(_silence(C.LEAD_IN_MS))
    timings = []
    pending_empty = []                      # audiosu növbəti ayə ilə ortaq olanlar

    for row in verses:
        chunks = item_chunks(row["spoken"])
        if not chunks:
            pending_empty.append(row["verse"])
            continue

        start = _ms(buf.tell())
        for i, chunk in enumerate(chunks):
            if i:
                buf.write(_silence(150))    # cümlə parçaları arası nəfəs
            buf.write(_pcm(chunk))
        end = _ms(buf.tell())

        group = pending_empty + [row["verse"]]
        pending_empty = []
        for verse_no, (s, e) in zip(group, _split_shared_window(start, end, len(group))):
            timings.append({"verse": verse_no, "start_ms": s, "end_ms": e})

        buf.write(_silence(C.VERSE_GAP_MS))

    # Surənin sonunda boş ayə qalıbsa sonuncu pəncərəni onunla bölüşdür.
    if pending_empty and timings:
        last = timings.pop()
        group = [last["verse"]] + pending_empty
        for verse_no, (s, e) in zip(group, _split_shared_window(last["start_ms"], last["end_ms"], len(group))):
            timings.append({"verse": verse_no, "start_ms": s, "end_ms": e})

    pcm = buf.getvalue()
    return pcm, sorted(timings, key=lambda t: t["verse"]), _ms(len(pcm))


def build_quran(data, only):
    chapters = {}
    for v in data["verses"]:
        chapters.setdefault(v["chapter"], []).append(v)

    out = []
    for chapter in sorted(chapters):
        if only and chapter not in only:
            continue
        verses = sorted(chapters[chapter], key=lambda v: v["verse"])
        pcm, timings, duration = assemble_chapter(verses)
        _encode(pcm, C.OUT_DIR / "quran" / f"{chapter:03d}.mp3", C.QURAN_BITRATE)
        out.append({"chapter": chapter, "duration_ms": duration, "verses": timings})
        print(f"  surə {chapter:3d}: {len(verses):3d} ayə, {duration/1000:7.1f} s", flush=True)
    return out


def write_timings(chapters):
    """Mövcud cədvəli oxuyub yalnız yenidən yığılan surələri əvəz edir."""
    path = C.INVENTORY / "tts_az_timings.json"
    existing = {}
    if path.exists():
        old = json.loads(path.read_text(encoding="utf-8"))
        existing = {c["chapter"]: c for c in old.get("chapters", [])}
    for c in chapters:
        existing[c["chapter"]] = c

    doc = {
        "version": C.TIMING_VERSION,
        "reciter": C.RECITER_ID,
        "chapters": [existing[k] for k in sorted(existing)],
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps(doc, ensure_ascii=False, separators=(",", ":"))
    path.write_text(payload, encoding="utf-8")
    with gzip.open(str(path) + ".gz", "wb", compresslevel=9) as f:
        f.write(payload.encode("utf-8"))
    print(f"vaxt cədvəli: {len(doc['chapters'])} surə → {path.name}(.gz)")


def build_hadith(data, only):
    manifest = {}
    manifest_path = C.INVENTORY.parent / "hadith_audio" / "hadith_audio_v1.json"
    if manifest_path.exists():
        manifest = {int(k): v for k, v in
                    json.loads(manifest_path.read_text(encoding="utf-8")).get("items", {}).items()}

    built = 0
    for h in data["hadith"]:
        if only and h["id"] not in only:
            continue
        if manifest.get(h["id"], {}).get("hash") == h["hash"]:
            continue                                   # mətn dəyişməyib
        chunks = item_chunks(h["spoken"])
        if not chunks:
            continue
        buf = io.BytesIO()
        buf.write(_silence(C.LEAD_IN_MS))
        for i, chunk in enumerate(chunks):
            if i:
                buf.write(_silence(C.HADITH_GAP_MS))
            buf.write(_pcm(chunk))
        pcm = buf.getvalue()
        _encode(pcm, C.OUT_DIR / "hadith" / f"h{h['id']}.mp3", C.HADITH_BITRATE)
        manifest[h["id"]] = {"duration_ms": _ms(len(pcm)), "hash": h["hash"]}
        built += 1
        if built % 50 == 0:
            print(f"  {built} hədis", flush=True)

    manifest_path.parent.mkdir(parents=True, exist_ok=True)
    manifest_path.write_text(
        json.dumps({"version": C.TIMING_VERSION,
                    "url_base": f"https://github.com/{C.GH_REPO}/releases/download/{C.HADITH_TAG}/",
                    "items": {str(k): manifest[k] for k in sorted(manifest)}},
                   ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )
    print(f"hədis: {built} yeni fayl, manifestdə {len(manifest)} yazı")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--kind", choices=["quran", "hadith", "both"], default="quran")
    ap.add_argument("--only", default="", help="vergüllə: surə nömrələri / hədis id-ləri")
    ap.add_argument("--ready", action="store_true",
                    help="yalnız bütün parçaları keşdə olan surələri yığır (render davam edərkən)")
    args = ap.parse_args()

    C.ensure_dirs()
    only = {int(x) for x in args.only.split(",") if x.strip()}
    data = json.loads(C.SOURCE_JSON.read_text(encoding="utf-8"))

    if args.ready:
        # Render davam edərkən yarımçıq surəni yığmaq natamam fayl verərdi.
        from synthesize import cache_path
        chapters = {}
        for v in data["verses"]:
            chapters.setdefault(v["chapter"], []).append(v)
        only = {
            no for no, verses in chapters.items()
            if (chunks := [c for v in verses for c in item_chunks(v["spoken"])])
            and all(cache_path(c).exists() for c in chunks)
        }
        print(f"tam hazır surə: {len(only)} → {sorted(only)}")

    if args.kind in ("quran", "both"):
        write_timings(build_quran(data, only))
    if args.kind in ("hadith", "both"):
        build_hadith(data, only)
    return 0


if __name__ == "__main__":
    sys.exit(main())
