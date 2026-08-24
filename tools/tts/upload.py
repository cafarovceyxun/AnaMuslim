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
import subprocess
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

    try:
        with urllib.request.urlopen(req, timeout=600) as resp:
            raw = resp.read()
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        # 404 çağıran tərəfdə mənalıdır (release var/yox) — onu olduğu kimi ötür.
        if e.code == 404:
            raise

        detail = e.read().decode("utf-8", "replace")
        try:
            message = json.loads(detail).get("message", detail)[:300]
        except ValueError:
            message = detail[:300]

        # GitHub 403-də hansı icazənin lazım olduğunu başlıqda deyir — ən dəqiq göstərici budur.
        accepted = e.headers.get("x-accepted-github-permissions") if e.headers else None
        if accepted:
            message += f"\n  gözlənilən icazə: {accepted}"

        hint = ""
        if e.code == 403:
            hint = ("\n  → Token bu repoda yazmağa icazə vermir. Yoxla:\n"
                    "     • fine-grained tokendə repository seçimi «AnaMuslim»-i əhatə edirmi\n"
                    "     • «Repository permissions → Contents» **Read and write**-dırmı\n"
                    "     • token müddəti bitməyibmi\n"
                    "     `python3 tools/tts/upload.py --check` ilə yoxlaya bilərsən")
        elif e.code == 401:
            hint = "\n  → GITHUB_TOKEN səhvdir və ya ləğv olunub."

        sys.exit(f"GitHub HTTP {e.code}: {message}{hint}")


def _upload_asset(release_id: int, path, name: str):
    """Aktivi `curl` ilə yükləyir.

    `urllib` 35 MB-lıq gövdəni tək parçada göndərir və GitHub bağlantını qırır
    («Broken pipe», 2026-08-24-də 8-ci fayl). `curl` axınla göndərir və özü təkrar cəhd edir.
    Token arqument kimi verilmir — `ps` çıxışında görünməsin deyə konfiqurasiya stdin-dən oxunur.
    """
    url = f"{UPLOADS}/repos/{C.GH_REPO}/releases/{release_id}/assets?name={name}"
    config = (
        f'header = "Authorization: Bearer {_token()}"\n'
        f'header = "Content-Type: audio/mpeg"\n'
        f'header = "X-GitHub-Api-Version: 2022-11-28"\n'
        f'url = "{url}"\n'
    )

    result = subprocess.run(
        ["curl", "-sS", "-K", "-", "-X", "POST", "--data-binary", f"@{path}",
         "--retry", "5", "--retry-delay", "3", "--retry-all-errors",
         "--connect-timeout", "30", "--max-time", "1800",
         "-o", "/dev/null", "-w", "%{http_code}"],
        input=config, capture_output=True, text=True,
    )

    code = (result.stdout or "").strip()

    if result.returncode != 0 or code not in ("200", "201"):
        raise RuntimeError(f"{name}: curl rc={result.returncode} http={code} {result.stderr[:200]}")


def check_access():
    """Tokenin bu repoya nə edə bildiyini göstərir — 403-ün səbəbini tapmaq üçün."""
    repo = _api("GET", f"{API}/repos/{C.GH_REPO}")
    print(f"repo: {repo.get('full_name')}  (private={repo.get('private')})")

    # ⚠️ `permissions.push` **hesabın** hüququdur, tokenin deyil — fine-grained token oxuma
    # ilə məhdud olsa belə orada `true` görünür. Yeganə dürüst yoxlama release siyahısını
    # oxumaq və 403-ün başlığına baxmaqdır.
    try:
        releases = _api("GET", f"{API}/repos/{C.GH_REPO}/releases?per_page=1")
        print(f"release oxunuşu: ✅ ({len(releases)} yazı göründü)")
        print("\n⚠️ Yazma icazəsini yalnız real yükləmə göstərir — GitHub tokenin icazələrini "
              "oxumaq üçün API vermir. `--kind quran` işlət, 403 gəlsə mesajda gözlənilən icazə yazılacaq.")
    except SystemExit:
        raise


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

        _upload_asset(release["id"], path, path.name)
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
    ap.add_argument("--check", action="store_true", help="token icazələrini yoxlayır")
    args = ap.parse_args()

    if args.check:
        _token()
        check_access()
        return 0

    if not args.manifest_only:
        upload(args.kind, args.dry_run)
    if args.kind == "quran":
        write_manifest(dry=args.dry_run)
    print("\n⚠️ inventory/ altındakı dəyişiklikləri commit etmək istifadəçinin işidir.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
