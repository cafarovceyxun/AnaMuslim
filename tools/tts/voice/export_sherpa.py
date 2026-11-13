#!/usr/bin/env python3
"""Piper modelini (az-v1.onnx) telefonda işləyəcək sherpa-onnx dəstinə çevirir.

Tətbiq modeli **cihazda** işlədir (sherpa-onnx 1.13.8 — Android AAR, iOS SherpaOnnxC.xcframework).
Bu skript GitHub Releases-ə yüklənəcək düz (alt qovluqsuz) fayl dəstini qurur və tətbiqdəki
`OfflineVoiceModel.FILES` siyahısı üçün ölçüləri çap edir.

Nə edir:
1. ONNX-ə sherpa-onnx-un tələb etdiyi metadata-nı yazır (`model_type=vits`, `comment=piper`,
   `voice=az`, `has_espeak=1` …) — model qrafikinə toxunmur.
2. `phoneme_id_map`-dən `tokens.txt` çıxarır. ⚠️ Çoxsimvollu açarlar (`aɪ`, `aʊ` … id 161–165)
   **atılır**: onlar yalnız config-də `vowel_clusters` olanda işlənir, bizim modeldə yoxdur, yəni bu
   id-lər öyrədilməyib — sherpa-onnx isə belə sətri oxuya bilmir və modeli ümumiyyətlə açmır.
3. sherpa-onnx-un öz `espeak-ng-data`-sından yalnız azərbaycanca (+ ingiliscəyə keçid) üçün lazım
   olan 8 faylı götürür (18 MB → ~860 KB). Piper-in öz espeak məlumatı yox, sherpa-nınkı: format sherpa-nın içindəki
   espeak-ng versiyasına bağlıdır. İkisinin eyni fonemləri verdiyi yoxlanılıb (2026-11-13).

Yoxlama (2026-11-13): noise 0 ilə Piper-in öz çıxışı və sherpa-onnx çıxışı korrelyasiya 0.99999 —
yəni telefon Mac-dakı ilə **eyni** səsi verir. Tək şərt: sherpa defolt olaraq cümlələr arasındakı
fasiləni `silence_scale=0.2` ilə sıxır; tətbiq onu 1.0 qoyur.

⚠️ sherpa-onnx 2.0 espeak-ng-ni kitabxanadan çıxarıb (fonemləri istifadəçi verməlidir) — tətbiq
ona görə 1.13.8-də sabitlənib. Yeniləmədən əvvəl bunu oxu.

Fayllar öz serverimizin Supabase Storage-ındadır (public `tts` bucket-i, `az-v1/` qovluğu). Serverdə
ümumi fayl limiti 50 MB-dır (`FILE_SIZE_LIMIT`, docker-compose.yml) — 63,5 MB-lıq model ona görə
**32 MiB-lıq hissələrə** bölünür (`az-v1.onnx.000`, `.001`), tətbiq endirəndən sonra birləşdirir.

İşlətmə (onnx paketi olan Python lazımdır):
    python3 -m venv /tmp/so && /tmp/so/bin/pip install onnx
    /tmp/so/bin/python tools/tts/voice/export_sherpa.py           # build/tts/sherpa/az-v1/ + ölçülər
    python3 tools/tts/voice/export_sherpa.py upload                # → storage `tts/az-v1/` (onnx lazım deyil)
"""
import hashlib
import json
import os
import shutil
import subprocess
import sys
import tarfile
import urllib.request

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
SRC_MODEL = os.path.join(ROOT, "build/tts/piper/onnx/az-v1.onnx")
SRC_CONFIG = SRC_MODEL + ".json"
WORK = os.path.join(ROOT, "build/tts/sherpa")
OUT = os.path.join(WORK, "az-v1")
PART_SIZE = 32 * 1024 * 1024  # serverin 50 MB limitindən aşağı
SSH = ["ssh", "-i", os.path.expanduser("~/.ssh/oracle_cafarovceyxun"), "ubuntu@130.61.171.105"]
PUBLIC = "https://anamuslim.cafarovceyxun.com/storage/v1/object/public/tts/az-v1/"
ESPEAK_URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/espeak-ng-data.tar.bz2"

# Release faylının adı → espeak-ng-data daxilindəki yol. Tətbiq faylları bu yollarla açır.
ESPEAK_FILES = {
    "espeak-phondata": "phondata",
    "espeak-phonindex": "phonindex",
    "espeak-phontab": "phontab",
    "espeak-intonations": "intonations",
    "espeak-az_dict": "az_dict",
    "espeak-lang-trk-az": "lang/trk/az",
    # espeak tanımadığı sözlərdə dili ingiliscəyə keçirir (`(en)` bayrağı) və en_dict tapmayanda
    # **prosesi bağlayır** — iOS-da səssiz çökmə kimi görünürdü (2026-11-13, hədis №1).
    "espeak-en_dict": "en_dict",
    "espeak-lang-gmw-en": "lang/gmw/en",
}


def espeak_dir() -> str:
    target = os.path.join(WORK, "espeak-ng-data")
    if os.path.isdir(target):
        return target
    os.makedirs(WORK, exist_ok=True)
    archive = os.path.join(WORK, "espeak-ng-data.tar.bz2")
    if not os.path.exists(archive):
        print("espeak-ng-data endirilir…")
        urllib.request.urlretrieve(ESPEAK_URL, archive)
    with tarfile.open(archive) as tar:
        for member in tar.getmembers():
            # Arxivdəki yol `espeak-ng-data/...` olmalıdır — kənara yazan üzv qəbul edilmir.
            if member.name.split("/")[0] != "espeak-ng-data" or ".." in member.name.split("/"):
                raise SystemExit(f"gözlənilməz arxiv üzvü: {member.name}")
        tar.extractall(WORK)
    return target


def main() -> None:
    import onnx

    cfg = json.load(open(SRC_CONFIG, encoding="utf-8"))
    if cfg.get("vowel_clusters"):
        raise SystemExit("config-də vowel_clusters var — çoxsimvollu tokenlər işlənir, skripti yenilə")

    shutil.rmtree(OUT, ignore_errors=True)
    os.makedirs(OUT)

    model = onnx.load(SRC_MODEL)
    meta = {
        "model_type": "vits",
        "comment": "piper",
        "language": "Azerbaijani",
        "voice": cfg["espeak"]["voice"],
        "has_espeak": 1,
        "n_speakers": cfg["num_speakers"],
        "sample_rate": cfg["audio"]["sample_rate"],
    }
    del model.metadata_props[:]
    for key, value in meta.items():
        prop = model.metadata_props.add()
        prop.key, prop.value = key, str(value)
    full = os.path.join(WORK, "az-v1.onnx")
    onnx.save(model, full)
    data = open(full, "rb").read()
    model_parts = []
    for i in range(0, len(data), PART_SIZE):
        name = f"az-v1.onnx.{i // PART_SIZE:03d}"
        open(os.path.join(OUT, name), "wb").write(data[i:i + PART_SIZE])
        model_parts.append(name)

    with open(os.path.join(OUT, "tokens.txt"), "w", encoding="utf-8") as f:
        for symbol, ids in cfg["phoneme_id_map"].items():
            if len(symbol) != 1:
                continue
            f.write(f"{symbol} {ids[0]}\n")

    src = espeak_dir()
    for asset, rel in ESPEAK_FILES.items():
        shutil.copyfile(os.path.join(src, rel), os.path.join(OUT, asset))

    print(f"\n{OUT}\n")
    print("// OfflineVoiceModel.FILES üçün:")
    print(f"model.onnx (birləşmiş) {len(data):>10d}")
    for name in [*model_parts, "tokens.txt", *ESPEAK_FILES]:
        path = os.path.join(OUT, name)
        size = os.path.getsize(path)
        sha = hashlib.sha256(open(path, "rb").read()).hexdigest()[:16]
        print(f"{name:22s} {size:>10d}  sha256:{sha}…")
    print("\ninference:", cfg["inference"])


def upload() -> None:
    """build/tts/sherpa/az-v1/* → storage `tts/az-v1/` (public bucket).

    `tools/wbw-az/publish.py` ilə eyni yol: fayl SSH ilə serverə ötürülür, storage API-yə service_role
    açarı ilə yazılır — açar yalnız serverdə oxunur və heç vaxt çap olunmur. Yol versiyalıdır, ona görə
    keş uzundur (fayl dəyişsə ID də dəyişir).
    """
    for name in sorted(os.listdir(OUT)):
        src = os.path.join(OUT, name)
        subprocess.run(SSH + [f"cat > /tmp/tts-{name}"], stdin=open(src, "rb"), check=True)
        cmd = (f'set -e; KEY=$(sudo grep -E "^SERVICE_ROLE_KEY=" /opt/supabase/.env | cut -d= -f2-); '
               f'curl -s -o /dev/null -w "%{{http_code}}" -X POST '
               f'-H "Authorization: Bearer $KEY" -H "apikey: $KEY" -H "x-upsert: true" '
               f'-H "Cache-Control: max-age=31536000" -H "Content-Type: application/octet-stream" '
               f'--data-binary @/tmp/tts-{name} '
               f'https://anamuslim.cafarovceyxun.com/storage/v1/object/tts/az-v1/{name}; '
               f'rm -f /tmp/tts-{name}')
        code = subprocess.run(SSH + [cmd], capture_output=True, text=True).stdout.strip()
        print(f"{name}: HTTP {code}")
        if code != "200":
            sys.exit("yükləmə alınmadı")
    # İctimai URL-dən geri oxuyub tutuşdur — yükləmə «200» deyib, fayl isə başqa ola bilər.
    for name in sorted(os.listdir(OUT)):
        local = hashlib.md5(open(os.path.join(OUT, name), "rb").read()).hexdigest()
        remote = hashlib.md5(urllib.request.urlopen(PUBLIC + name).read()).hexdigest()
        print(f"{name}: ictimai URL", "uyğundur" if local == remote else "FƏRQLİDİR")


if __name__ == "__main__":
    if sys.argv[1:] == ["upload"]:
        upload()
        sys.exit()
    if not os.path.exists(SRC_MODEL):
        sys.exit(f"model yoxdur: {SRC_MODEL}")
    main()
