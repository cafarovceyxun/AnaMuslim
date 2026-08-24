#!/usr/bin/env python3
"""Hazır faylları GitHub Releases-ə yükləyir və manifesti yeniləyir.

    export GITHUB_TOKEN=...          # fine-grained token, «Contents: Read and write»
    python3 tools/tts/upload.py --kind quran --dry-run
    python3 tools/tts/upload.py --kind quran
    python3 tools/tts/upload.py --manifest-only     # yalnız reciter yazısını yenilə

`gh` CLI **tələb olunmur** — release yaratmaq və aktiv yükləmək GitHub REST API ilə
edilir (bu maşında nə Homebrew, nə də gh var). Mövcud eyniadlı aktiv əvəzlənir.

Manifest və vaxt cədvəli repoya yazılır, amma commit-i **istifadəçi özü** edir.
"""
import argparse
import json
import os
import sys
import urllib.error
import urllib.request

import config as C

MANIFEST = C.INVENTORY / "available_recitation_translations_info_v2.json"


API = "https://api.github.com"
UPLOADS = "https://uploads.github.com"


def _token() -> str:
    token = os.environ.get("GITHUB_TOKEN", "")
    if not token:
        sys.exit(
            "GITHUB_TOKEN yoxdur. https://github.com/settings/tokens?type=beta ünvanında\n"
            "fine-grained token yarat: repository = AnaMuslim, «Contents: Read and write».\n"
            "Sonra: export GITHUB_TOKEN=..."
        )
    return token


def _api(method: str, url: str, payload=None, data: bytes = None, content_type: str = None):
    headers = {
        "Authorization": "Bearer " + _token(),
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
    }
    body = data
    if payload is not None:
        body = json.dumps(payload).encode("utf-8")
        headers["Content-Type"] = "application/json"
    if content_type:
        headers["Content-Type"] = content_type

    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=600) as resp:
        raw = resp.read()
        return json.loads(raw) if raw else {}


def ensure_release(tag: str, title: str, dry: bool) -> dict:
    """Release-i tapır, yoxdursa yaradır."""
    try:
        return _api("GET", f"{API}/repos/{C.GH_REPO}/releases/tags/{tag}")
    except urllib.error.HTTPError as e:
        if e.code != 404:
            sys.exit(f"release oxunmadı: HTTP {e.code} {e.read().decode()[:300]}")

    if dry:
        print(f"   (quru rejim) release yaradılacaqdı: {tag}")
        return {"id": 0, "assets": []}

    return _api("POST", f"{API}/repos/{C.GH_REPO}/releases", {
        "tag_name": tag,
        "name": title,
        "body": "Avtomatik yaradılmış səsləndirmə faylları (Google Gemini TTS, Iapetus).",
    })


def upload(kind: str, dry: bool):
    tag = C.QURAN_TAG if kind == "quran" else C.HADITH_TAG
    folder = C.OUT_DIR / kind
    files = sorted(folder.glob("*.mp3"))
    if not files:
        sys.exit(f"{folder} boşdur — əvvəlcə assemble.py işlət")

    total = sum(f.stat().st_size for f in files)
    print(f"{kind}: {len(files)} fayl, {total / 1e6:.1f} MB → {C.GH_REPO} @ {tag}")

    release = ensure_release(tag, f"TTS audio ({kind})", dry)
    existing = {a["name"]: a["id"] for a in release.get("assets", [])}

    for i, path in enumerate(files, 1):
        if dry:
            mark = "əvəzlənəcək" if path.name in existing else "yeni"
            print(f"   {i}/{len(files)} {path.name} ({path.stat().st_size / 1e6:.1f} MB) — {mark}")
            continue

        # Eyniadlı aktiv varsa əvvəlcə silinir: GitHub eyni adla ikinci aktivi qəbul etmir.
        if path.name in existing:
            _api("DELETE", f"{API}/repos/{C.GH_REPO}/releases/assets/{existing[path.name]}")

        url = f"{UPLOADS}/repos/{C.GH_REPO}/releases/{release['id']}/assets?name={path.name}"
        _api("POST", url, data=path.read_bytes(), content_type="audio/mpeg")
        print(f"   {i}/{len(files)} {path.name} ✓", flush=True)


def write_manifest(dry: bool = False):
    """`az` yazısını manifestə əlavə edir / yeniləyir, digər dilləri saxlayır."""
    doc = {"reciters": []}
    if MANIFEST.exists() and MANIFEST.stat().st_size:
        doc = json.loads(MANIFEST.read_text(encoding="utf-8"))

    entry = {
        "id": C.RECITER_ID,
        "reciter": "Süni səs (TTS)",
        "lang_code": "az",
        "lang_name": "Azərbaycan",
        "book": "AnaMuslim tərcüməsi",
        "is_default": True,
        "url_template": C.QURAN_URL_TEMPLATE,
        "timing_url": C.TIMING_URL,
        "timing_version": C.TIMING_VERSION,
        "translations": {
            "az": "Süni səs (TTS)",
            "tr": "Yapay ses (TTS)",
            "en": "Synthetic voice (TTS)",
            "ru": "Синтезированный голос (TTS)",
            "ar": "صوت اصطناعي",
        },
    }
    others = [r for r in doc.get("reciters", []) if r.get("id") != C.RECITER_ID]
    doc["reciters"] = [entry] + others

    if dry:
        # Quru rejim heç bir fayla toxunmamalıdır — yoxsa «sadəcə baxıram» deyib
        # repoda dəyişiklik qoyub gedirsən.
        print(f"   (quru rejim) manifestə yazılacaqdı: {entry['id']} → {entry['url_template']}")
        return
    MANIFEST.parent.mkdir(parents=True, exist_ok=True)
    MANIFEST.write_text(json.dumps(doc, ensure_ascii=False, indent=1), encoding="utf-8")
    print("manifest yeniləndi:", MANIFEST)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--kind", choices=["quran", "hadith"], default="quran")
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--manifest-only", action="store_true")
    args = ap.parse_args()

    if not args.manifest_only:
        upload(args.kind, args.dry_run)
    if args.kind == "quran":
        write_manifest(dry=args.dry_run)
    print("\n⚠️ inventory/ altındakı dəyişiklikləri commit etmək istifadəçinin işidir.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
