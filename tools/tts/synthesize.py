#!/usr/bin/env python3
"""Google TTS (Gemini `Iapetus`) ilə ayə/hədis parçalarını render edir.

    export GEMINI_API_KEY=...            # AI Studio açarı — https://aistudio.google.com/apikey
    python3 tools/tts/synthesize.py --list-models      # cari TTS model adları
    python3 tools/tts/synthesize.py --dry-run          # nə qədər simvol/sorğu qalıb
    python3 tools/tts/synthesize.py --kind quran       # render (keşdə olanı atlayır)

Alternativ backend: `GOOGLE_API_KEY` və ya `gcloud` varsa Cloud TTS işlədilir.

Hər parça `build/tts/cache/<hash>.wav` kimi saxlanır; hash mətn + model + səs +
üslub promptundan çıxır, ona görə **yalnız dəyişən mətn** yenidən pul yandırır.
"""
from __future__ import annotations

import argparse
import base64
import io
import shutil
import hashlib
import tempfile
import urllib.parse
import json
import os
import re
import subprocess
import sys
import threading
import time
import urllib.error
import wave
import urllib.request
from concurrent.futures import ThreadPoolExecutor

from pathlib import Path

import config as C
from normalize import split_for_request

_token_lock = threading.Lock()
_cached_token = {"value": None, "at": 0.0}


def _access_token():
    """`gcloud`-dan OAuth token (API açarı verilməyibsə). 50 dəqiqə keşlənir."""
    with _token_lock:
        if _cached_token["value"] and time.time() - _cached_token["at"] < 3000:
            return _cached_token["value"]
        if _service_account_file():
            token = _sa_token()
        elif not _gcloud_available():
            sys.exit(
                "Cloud TTS üçün OAuth yoxdur: nə GOOGLE_APPLICATION_CREDENTIALS təyin olunub, "
                "nə də `gcloud` quraşdırılıb."
            )
        else:
            token = subprocess.run(
                ["gcloud", "auth", "print-access-token"],
                capture_output=True, text=True, check=True,
            ).stdout.strip()
        _cached_token.update(value=token, at=time.time())
        return token


def backend() -> str:
    """Hansı API işlədiləcək.

    `TTS_BACKEND=gemini|cloudtts` hər şeyi həll edir. Verilməyibsə Gemini açarı
    üstündür — amma hər iki açar mühitdə qalıbsa bu səssizcə yanlış API-yə göndərə
    bilər, ona görə seçim `--list-models` və render başlığında çap olunur.
    """
    forced = os.environ.get("TTS_BACKEND", "").strip().lower()
    if forced in ("gemini", "cloudtts"):
        return forced
    if C.GEMINI_API_KEY:
        return "gemini"
    if C.GOOGLE_API_KEY or _gcloud_available() or _service_account_file():
        return "cloudtts"

    # Açarsız işlədilən addımlar (assemble, hesabatlar) da keşi tapa bilməlidir: backend
    # adı hash-in içindədir, ona görə render zamanı yazılmış dəyər burada oxunur.
    return _remembered_backend()


_BACKEND_FILE = "backend.txt"


def _remembered_backend() -> str:
    path = C.BUILD / _BACKEND_FILE
    return path.read_text(encoding="utf-8").strip() if path.exists() else ""


def _remember_backend(name: str):
    path = C.BUILD / _BACKEND_FILE
    if name and _remembered_backend() != name:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(name, encoding="utf-8")


def backend_description() -> str:
    """Seçilmiş backend + açarın haradan gəldiyi (dəyər çap olunmur, yalnız mənbə)."""
    which = backend()
    if which == "gemini":
        source = "GEMINI_API_KEY" if C.GEMINI_API_KEY else "(açar yoxdur!)"
    elif which == "cloudtts":
        source = ("xidmət hesabı: " + Path(_service_account_file()).name if _service_account_file()
                  else "gcloud auth print-access-token" if _gcloud_available()
                  else "(OAuth yoxdur — Cloud TTS API açarı qəbul etmir!)")
    else:
        return "backend yoxdur"

    other = []
    if which == "gemini" and (C.GOOGLE_API_KEY or _gcloud_available()):
        other.append("GOOGLE_API_KEY də təyin olunub — Cloud TTS üçün: TTS_BACKEND=cloudtts")
    if which == "cloudtts" and C.GEMINI_API_KEY:
        other.append("GEMINI_API_KEY də təyin olunub — Gemini üçün: TTS_BACKEND=gemini")

    note = ("\n  ⚠️ " + "; ".join(other)) if other else ""

    return f"{which} (açar: {source}, model: {C.TTS_MODEL}){note}"


def _gcloud_available() -> bool:
    return shutil.which("gcloud") is not None


def _service_account_file() -> str:
    return os.environ.get("GOOGLE_APPLICATION_CREDENTIALS", "")


def _sa_token() -> str:
    """Xidmət hesabı JSON-undan OAuth2 tokeni çıxarır.

    Cloud TTS **API açarı qəbul etmir** («API keys are not supported by this API»),
    `gcloud` isə bu maşında yoxdur. Ona görə token birbaşa burada alınır: JWT qurulur,
    xidmət hesabının açarı ilə imzalanır və token endpoint-ində dəyişdirilir.

    İmzalama üçün `google-auth` varsa o işlədilir; yoxdursa macOS-da onsuz da mövcud olan
    `openssl` ilə eyni iş görülür — yəni əlavə quraşdırma tələb olunmur.
    """
    path = _service_account_file()
    info = json.loads(Path(path).read_text(encoding="utf-8"))

    try:
        from google.oauth2 import service_account          # type: ignore
        from google.auth.transport.requests import Request  # type: ignore

        creds = service_account.Credentials.from_service_account_info(
            info, scopes=["https://www.googleapis.com/auth/cloud-platform"],
        )
        creds.refresh(Request())
        return creds.token
    except ImportError:
        return _sa_token_via_openssl(info)


def _sa_token_via_openssl(info: dict) -> str:
    now = int(time.time())
    header = {"alg": "RS256", "typ": "JWT"}
    claims = {
        "iss": info["client_email"],
        "scope": "https://www.googleapis.com/auth/cloud-platform",
        "aud": info.get("token_uri", "https://oauth2.googleapis.com/token"),
        "iat": now,
        "exp": now + 3600,
    }

    def b64(raw: bytes) -> bytes:
        return base64.urlsafe_b64encode(raw).rstrip(b"=")

    signing_input = b64(json.dumps(header).encode()) + b"." + b64(json.dumps(claims).encode())

    with tempfile.NamedTemporaryFile("w", suffix=".pem", delete=False) as key_file:
        os.chmod(key_file.name, 0o600)          # xüsusi açar başqasına görünməsin
        key_file.write(info["private_key"])
        key_path = key_file.name

    try:
        signature = subprocess.run(
            ["openssl", "dgst", "-sha256", "-sign", key_path],
            input=signing_input, capture_output=True, check=True,
        ).stdout
    finally:
        os.unlink(key_path)

    assertion = signing_input + b"." + b64(signature)
    body = urllib.parse.urlencode({
        "grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer",
        "assertion": assertion.decode(),
    }).encode()

    req = urllib.request.Request(
        claims["aud"], data=body,
        headers={"Content-Type": "application/x-www-form-urlencoded"},
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.load(resp)["access_token"]


def require_credentials():
    """Seçilmiş backend-in **həqiqətən** işlək kimlik məlumatı olduğunu yoxlayır.

    Sadəcə «backend seçildi» kifayət deyil: `TTS_BACKEND=cloudtts` verilib OAuth
    qurulmayanda çağırış dərinlikdə `gcloud` tapılmadı deyə çökürdü.
    """
    which = backend()

    if which == "gemini" and C.GEMINI_API_KEY:
        return
    if which == "cloudtts" and (_service_account_file() or _gcloud_available()):
        return

    if which == "cloudtts":
        sys.exit(
            "Cloud TTS OAuth tələb edir — API açarı qəbul etmir. Xidmət hesabı yarat və:\n"
            "  export GOOGLE_APPLICATION_CREDENTIALS=/yol/xidmet-hesabi.json\n"
            "(alternativ: `gcloud auth login`, amma gcloud bu maşında quraşdırılmayıb)"
        )

    sys.exit(
        "Açar tapılmadı. Ya:\n"
        "  export GEMINI_API_KEY=...                      # https://aistudio.google.com/apikey\n"
        "ya da Cloud TTS üçün:\n"
        "  export GOOGLE_APPLICATION_CREDENTIALS=...json  # xidmət hesabı"
    )


def _request(url: str, payload, method: str = "POST", api_key: str = ""):
    headers = {"Content-Type": "application/json; charset=utf-8"}

    if api_key:
        url += ("&" if "?" in url else "?") + "key=" + api_key
    else:
        headers["Authorization"] = "Bearer " + _access_token()
        if C.GOOGLE_PROJECT:
            headers["x-goog-user-project"] = C.GOOGLE_PROJECT

    data = json.dumps(payload).encode("utf-8") if payload is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=180) as resp:
        return json.load(resp)


def _request_or_explain(*args, **kwargs):
    """`_request`, amma HTTP xətasını traceback yerinə cavab mətni ilə göstərir."""
    try:
        return _request(*args, **kwargs)
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", "replace")[:600]
        hint = ""
        if e.code == 401:
            hint = ("\n  → Cloud TTS **API açarı qəbul etmir**, OAuth lazımdır:\n"
                    "     export GOOGLE_APPLICATION_CREDENTIALS=/yol/xidmet-hesabi.json\n"
                    "     (Gemini API isə əksinə — yalnız GEMINI_API_KEY ilə işləyir.)")
        elif e.code == 403:
            hint = "\n  → API layihədə aktiv deyil, ya da açarın məhdudiyyəti var."
        sys.exit(f"HTTP {e.code}: {detail}{hint}")
    except urllib.error.URLError as e:
        sys.exit(f"Şəbəkə xətası: {e.reason}")


def _wav(pcm: bytes, sample_rate: int) -> bytes:
    """Xam PCM-i WAV-a bükür — Gemini API başlıqsız `audio/L16` qaytarır."""
    buf = io.BytesIO()
    with wave.open(buf, "wb") as w:
        w.setnchannels(C.CHANNELS)
        w.setsampwidth(C.SAMPLE_WIDTH)
        w.setframerate(sample_rate)
        w.writeframes(pcm)
    return buf.getvalue()


def _rate_from_mime(mime: str) -> int:
    """`audio/L16;codec=pcm;rate=24000` → 24000."""
    for part in mime.split(";"):
        part = part.strip()
        if part.startswith("rate="):
            try:
                return int(part[5:])
            except ValueError:
                break
    return C.SAMPLE_RATE


def _call_gemini(text: str) -> bytes:
    url = f"{C.GEMINI_ENDPOINT}/models/{C.TTS_MODEL}:generateContent"
    payload = {
        "contents": [{"parts": [{"text": f"{C.STYLE_PROMPT}\n\n{text}"}]}],
        "generationConfig": {
            "responseModalities": ["AUDIO"],
            "speechConfig": {
                "voiceConfig": {"prebuiltVoiceConfig": {"voiceName": C.VOICE}}
            },
        },
    }

    body = _request(url, payload, api_key=C.GEMINI_API_KEY)
    parts = body["candidates"][0]["content"]["parts"]
    inline = next(p["inlineData"] for p in parts if "inlineData" in p)

    _record_usage(body.get("usageMetadata") or {}, len(text))

    return _wav(base64.b64decode(inline["data"]), _rate_from_mime(inline.get("mimeType", "")))


def _call_cloud_tts(text: str) -> bytes:
    payload = {
        "input": {"text": text, "prompt": C.STYLE_PROMPT},
        "voice": {
            "languageCode": C.LANGUAGE_CODE,
            "name": C.VOICE,
            "model_name": C.TTS_MODEL,
        },
        "audioConfig": {
            "audioEncoding": C.AUDIO_ENCODING,
            "sampleRateHertz": C.SAMPLE_RATE,
        },
    }

    # Cloud TTS LINEAR16-nı onsuz da RIFF/WAV başlığı ilə qaytarır.
    # Cloud TTS API açarını **qəbul etmir**, ona görə açar ötürülmür: `_request`
    # avtomatik OAuth tokeninə (xidmət hesabı və ya gcloud) keçir.
    body = _request(C.CLOUD_TTS_ENDPOINT, payload)
    return base64.b64decode(body["audioContent"])


def chunk_hash(text: str) -> str:
    key = "|".join([backend(), C.TTS_MODEL, C.VOICE, C.LANGUAGE_CODE, C.STYLE_PROMPT, text])
    return hashlib.sha256(key.encode("utf-8")).hexdigest()[:24]


def cache_path(text: str):
    return C.CACHE / f"{chunk_hash(text)}.wav"


def synthesize_chunk(
    text: str,
    retries: int = 6,
    allow_split: bool = True,
    cache: bool = True,
) -> bytes:
    """Bir parçanı render edir (keşdə varsa oxuyur). LINEAR16 → RIFF/WAV baytları."""
    path = cache_path(text)
    if cache and path.exists() and path.stat().st_size > 44:
        return path.read_bytes()

    active = backend()
    call = _call_gemini if active == "gemini" else _call_cloud_tts

    delay = 2.0
    for attempt in range(retries):
        try:
            PACER.wait()
            audio = call(text)
            PACER.reward()
            _remember_backend(active)
            tmp = path.with_suffix(".tmp")
            tmp.write_bytes(audio)
            tmp.replace(path)          # yarımçıq fayl keşdə qalmasın
            return audio
        except urllib.error.HTTPError as e:
            detail = e.read().decode("utf-8", "replace")[:1200]

            # 429 iki fərqli şeydir və mesajları oxşardır — ayırd etmək vacibdir:
            #   • «prepayment credits are depleted» → balans bitib, gözləmək kömək etmir.
            #   • «You exceeded your current quota … check your plan and billing details»
            #     → sürət/kvota limiti, gözləyəndə keçir.
            # ⚠️ İkincisində də «billing» sözü var, ona görə marker kimi yalnız balansa
            # məxsus ifadələr işlənir; «billing»-i marker saymaq bütün işi ilk 429-da
            # dayandırırdı (2026-08-24-də Bəqərənin 212 parçası məhz buna görə düşdü).
            fatal_429 = e.code == 429 and any(
                marker in detail for marker in ("credits are depleted", "prepayment credits")
            )

            # Yeni aktivləşdirilmiş API bir müddət 403 `SERVICE_DISABLED` qaytarır
            # («wait a few minutes for the action to propagate») — bu, keçicidir.
            propagating = e.code == 403 and "SERVICE_DISABLED" in detail

            if e.code == 429 and not fatal_429:
                PACER.penalise()
                _note_quota(detail)

            # Qalan 429/5xx keçicidir, 4xx-in qalanı sorğu səhvidir — dərhal dayan.
            retryable = e.code in (429, 500, 502, 503, 504) or propagating

            # Vertex-in məzmun süzgəci bəzi ayələri sabit rədd edir (məs. 93:9 «yetimi xor
            # görmə»). Mətni dəyişmək olmaz — tərcümədir — ona görə **sorğu sərhədi** dəyişilir:
            # cümlə iki hissəyə bölünüb ayrı render olunur və PCM səviyyəsində birləşdirilir.
            # Sözlər toxunulmaz qalır, yalnız araya kiçik nəfəs düşür.
            if e.code == 400 and _blocked_by_filter(detail) and allow_split:
                halves = _split_in_two(text)
                if halves:
                    print(f"  ⓘ süzgəc rədd etdi, iki hissəyə bölünür: «{text[:40]}…»", flush=True)
                    audio = _concat_wav([
                        synthesize_chunk(half, retries=3, allow_split=False, cache=False)
                        for half in halves
                    ])
                    if cache:
                        tmp = path.with_suffix(".tmp")
                        tmp.write_bytes(audio)
                        tmp.replace(path)
                    return audio

            if fatal_429 or not retryable or attempt == retries - 1:
                raise RuntimeError(f"TTS {e.code}: {detail}") from None

            if propagating and attempt == 0:
                print("  ⓘ API yeni açılıb, yayılma gözlənilir…", flush=True)

            # Google `RetryInfo`-da nə qədər gözləmək lazım olduğunu özü deyir; varsa ona qulaq as.
            time.sleep(_retry_delay(detail) or delay)
            delay = min(delay * 2, 60.0)
        except (urllib.error.URLError, TimeoutError):
            if attempt == retries - 1:
                raise
            time.sleep(delay)
            delay *= 2
    raise RuntimeError("çatılmaz")


def _retry_delay(detail: str):
    """Xəta gövdəsindəki `RetryInfo.retryDelay` («31s») → saniyə."""
    match = re.search(r'"retryDelay"\s*:\s*"(\d+(?:\.\d+)?)s"', detail)
    return min(float(match.group(1)) + 1.0, 120.0) if match else None


_quota_seen = set()


def _note_quota(detail: str):
    """Limitin adını bir dəfə çap edir — RPM, TPM və gündəlik limiti ayırd etmək üçün."""
    for quota_id in set(re.findall(r'"quotaId"\s*:\s*"([^"]+)"', detail)):
        if quota_id not in _quota_seen:
            _quota_seen.add(quota_id)
            print(f"  ⓘ limit: {quota_id}", flush=True)


class RatePacer:
    """Sorğu tempini özü tənzimləyir.

    Gemini API dəqiqəlik limiti (RPM) elan etmir, 429 mesajında da rəqəm yoxdur — ona görə
    limiti təxmin etmək əvəzinə ölçürük: hər 429-da aralıq uzanır, uzun uğurlu seriyadan
    sonra yavaş-yavaş daralır. 287 parçalıq surə bir dəfə divara dəyib dayanmasın deyə.
    """

    def __init__(self, rpm: float, floor_rpm: float = 60.0):
        self._lock = threading.Lock()
        self._interval = 60.0 / max(rpm, 0.1)
        self._floor = 60.0 / floor_rpm
        self._next_at = 0.0
        self._ok_streak = 0

    def wait(self):
        with self._lock:
            now = time.monotonic()
            start = max(now, self._next_at)
            self._next_at = start + self._interval
        delay = start - time.monotonic()
        if delay > 0:
            time.sleep(delay)

    def penalise(self):
        with self._lock:
            self._interval = min(self._interval * 1.5, 30.0)
            self._ok_streak = 0

    def reward(self):
        with self._lock:
            self._ok_streak += 1
            if self._ok_streak >= 20:
                self._ok_streak = 0
                self._interval = max(self._interval * 0.9, self._floor)

    @property
    def rpm(self) -> float:
        return 60.0 / self._interval


PACER = RatePacer(rpm=10.0)

_usage_lock = threading.Lock()


def _record_usage(meta: dict, chars: int):
    """API-nin bildirdiyi token sayını `build/tts/usage.json`-da toplayır.

    Xərci təxmin etmək üçün yeganə dürüst yol budur: Gemini TTS **simvola görə yox,
    tokenə görə** hesablanır və audio çıxış tokenləri mətn tokenlərindən baha olur.
    """
    if not meta:
        return

    path = C.BUILD / "usage.json"

    with _usage_lock:
        total = {"calls": 0, "chars": 0, "prompt_tokens": 0, "output_tokens": 0, "total_tokens": 0}
        if path.exists():
            try:
                total.update(json.loads(path.read_text(encoding="utf-8")))
            except ValueError:
                pass

        total["calls"] += 1
        total["chars"] += chars
        total["prompt_tokens"] += meta.get("promptTokenCount", 0)
        total["output_tokens"] += meta.get("candidatesTokenCount", 0)
        total["total_tokens"] += meta.get("totalTokenCount", 0)

        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(total, indent=1), encoding="utf-8")


def usage_summary() -> str:
    # Cloud TTS cavabında `usageMetadata` yoxdur (o, Gemini API-yə xasdır), ona görə
    # bu sayğac yalnız Gemini backend-i üçün mənalıdır — əks halda köhnə rəqəm göstərib
    # yanıldardı. Cloud TTS xərci Cloud Billing hesabatından izlənilir.
    if backend() == "cloudtts":
        return "Cloud TTS token uçotu bildirmir — xərc Cloud Billing hesabatındadır"

    path = C.BUILD / "usage.json"
    if not path.exists():
        return "uçot yoxdur"

    u = json.loads(path.read_text(encoding="utf-8"))
    chars = max(u.get("chars", 0), 1)
    per_1k = u.get("total_tokens", 0) / chars * 1000

    return (f"{u.get('calls', 0)} çağırış, {u.get('chars', 0)} simvol → "
            f"{u.get('output_tokens', 0)} audio + {u.get('prompt_tokens', 0)} mətn tokeni "
            f"({per_1k:.0f} token / 1000 simvol)")


def _concat_wav(parts: list) -> bytes:
    """Bir neçə WAV-ı tək WAV-a birləşdirir (eyni format olduğu üçün sadə PCM yığımı)."""
    pcm = b""
    for raw in parts:
        with wave.open(io.BytesIO(raw), "rb") as w:
            pcm += w.readframes(w.getnframes())

    out = io.BytesIO()
    with wave.open(out, "wb") as w:
        w.setnchannels(C.CHANNELS)
        w.setsampwidth(C.SAMPLE_WIDTH)
        w.setframerate(C.SAMPLE_RATE)
        w.writeframes(pcm)
    return out.getvalue()


def _split_in_two(text: str):
    """Mətni vergüldən, olmasa söz sərhədindən iki bərabər hissəyə bölür."""
    commas = [i for i, ch in enumerate(text) if ch == ","]
    if commas:
        pivot = min(commas, key=lambda i: abs(i - len(text) // 2)) + 1
        return text[:pivot].strip(), text[pivot:].strip()

    words = text.split()
    if len(words) < 2:
        return None
    half = len(words) // 2
    return " ".join(words[:half]), " ".join(words[half:])


def _blocked_by_filter(detail: str) -> bool:
    return "usage guidelines" in detail or "violates" in detail


def item_chunks(spoken: str):
    return split_for_request(spoken, C.MAX_REQUEST_BYTES) if spoken.strip() else []


def load_source():
    if not C.SOURCE_JSON.exists():
        sys.exit("build/tts/source.json yoxdur — əvvəlcə: python3 tools/tts/fetch.py")
    return json.loads(C.SOURCE_JSON.read_text(encoding="utf-8"))


def plan(data, kind, chapters=None):
    """(etiket, parça mətni) siyahısı — keşdə olmayanlar sonda ayrıca sayılır."""
    items = []
    if kind in ("quran", "both"):
        for v in data.get("verses", []):
            if chapters and v["chapter"] not in chapters:
                continue
            for i, ch in enumerate(item_chunks(v["spoken"])):
                items.append((f"{v['chapter']}:{v['verse']}#{i}", ch))
    if kind in ("hadith", "both"):
        for h in data.get("hadith", []):
            for i, ch in enumerate(item_chunks(h["spoken"])):
                items.append((f"h{h['id']}#{i}", ch))
    return items


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--kind", choices=["quran", "hadith", "both"], default="both")
    ap.add_argument("--limit", type=int, default=0, help="yalnız ilk N parça (sınaq üçün)")
    ap.add_argument("--chapters", default="", help="vergüllə surə nömrələri (yalnız --kind quran)")
    ap.add_argument("--workers", type=int, default=4)
    ap.add_argument("--rpm", type=float, default=10.0,
                    help="başlanğıc temp (sorğu/dəqiqə); 429-larda özü azalır, uğurda artır")
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--enable-api", default="",
                    help="verilən API-ni layihədə aktivləşdirir (məs. aiplatform.googleapis.com)")
    ap.add_argument("--diagnose", action="store_true",
                    help="layihənin billing və API vəziyyətini oxuyur (heç nə ödəmir)")
    ap.add_argument("--probe", action="store_true",
                    help="Cloud TTS üçün işlək (languageCode, model) kombinasiyasını tapır")
    ap.add_argument("--list-models", action="store_true",
                    help="backend-in tanıdığı TTS model/səs adları")
    args = ap.parse_args()

    C.ensure_dirs()
    globals()["PACER"] = RatePacer(rpm=args.rpm)

    if args.enable_api:
        require_credentials()
        sa_path = _service_account_file()
        if not sa_path:
            sys.exit("Bu əməliyyat xidmət hesabı tələb edir (GOOGLE_APPLICATION_CREDENTIALS).")

        project = json.loads(Path(sa_path).read_text(encoding="utf-8"))["project_id"]
        service = args.enable_api
        print(f"{service} → {project} layihəsində aktivləşdirilir…")

        try:
            body = _request(
                f"https://serviceusage.googleapis.com/v1/projects/{project}/services/{service}:enable",
                {}, method="POST",
            )
            done = body.get("done")
            print("cavab:", "tamamlandı" if done else "əməliyyat başladı (bir neçə dəqiqə çəkə bilər)")
            if body.get("error"):
                print("xəta:", json.dumps(body["error"], ensure_ascii=False)[:400])
        except urllib.error.HTTPError as e:
            detail = e.read().decode("utf-8", "replace")
            try:
                err = json.loads(detail)["error"]
                print(f"HTTP {e.code}: {err.get('message', '')[:300]}")
                # Billing tələbi məhz burada açıq şəkildə görünür.
                for d in err.get("details", []):
                    if d.get("reason"):
                        print("  reason:", d["reason"])
            except ValueError:
                print(f"HTTP {e.code}: {detail[:300]}")
        return 0

    if args.diagnose:
        require_credentials()
        sa_path = _service_account_file()
        if not sa_path:
            sys.exit("Diaqnostika xidmət hesabı tələb edir (GOOGLE_APPLICATION_CREDENTIALS).")

        project = json.loads(Path(sa_path).read_text(encoding="utf-8"))["project_id"]
        print(f"layihə: {project}\n")

        # Billing: Vertex/Agent Platform aktiv billing olmadan açılmır — əsas sual budur.
        try:
            info = _request(
                f"https://cloudbilling.googleapis.com/v1/projects/{project}/billingInfo",
                None, method="GET",
            )
            enabled = info.get("billingEnabled")
            account = info.get("billingAccountName") or "(bağlanmayıb)"
            print(f"billing: {'✅ aktiv' if enabled else '❌ deaktiv'}  |  hesab: {account}")
        except urllib.error.HTTPError as e:
            print(f"billing: oxunmadı ({e.code}) — xidmət hesabında billing.viewer rolu yoxdur")

        for service in ("texttospeech.googleapis.com", "aiplatform.googleapis.com",
                        "generativelanguage.googleapis.com"):
            try:
                body = _request(
                    f"https://serviceusage.googleapis.com/v1/projects/{project}/services/{service}",
                    None, method="GET",
                )
                state = body.get("state", "?")
                mark = "✅" if state == "ENABLED" else "❌"
                print(f"{mark} {service:<38} {state}")
            except urllib.error.HTTPError as e:
                detail = e.read().decode("utf-8", "replace")
                try:
                    msg = json.loads(detail)["error"]["message"][:90]
                except ValueError:
                    msg = detail[:90]
                print(f"?  {service:<38} oxunmadı ({e.code}): {msg}")
        return 0

    if args.probe:
        require_credentials()
        print(f"backend: {backend_description()}\n")
        text = "Bu bir sınaq cümləsidir."
        saved_lang, saved_model = C.LANGUAGE_CODE, C.TTS_MODEL
        works = []

        for lang in ("az-AZ", "en-US"):
            for model in ("gemini-3.1-flash-tts", "gemini-3.1-flash-tts-preview",
                          "gemini-2.5-flash-tts"):
                C.LANGUAGE_CODE, C.TTS_MODEL = lang, model
                label = f"{lang:<6} + {model:<30}"
                try:
                    audio = _call_cloud_tts(text)
                    print(f"  ✅ {label} → {len(audio) // 1024} KB")
                    works.append((lang, model))
                except urllib.error.HTTPError as e:
                    body = e.read().decode("utf-8", "replace")
                    try:
                        msg = json.loads(body)["error"]["message"][:110]
                    except ValueError:
                        msg = body[:110]
                    print(f"  ❌ {label} → {e.code}: {msg}")
                except Exception as exc:                     # noqa: BLE001
                    print(f"  ❌ {label} → {str(exc)[:110]}")

        C.LANGUAGE_CODE, C.TTS_MODEL = saved_lang, saved_model

        if works:
            lang, model = works[0]
            print(f"\nİşlək kombinasiya: export TTS_LANG={lang} TTS_MODEL={model}")
        else:
            print("\nHeç bir kombinasiya işləmədi — mesajları yuxarıda oxu.")
        return 0

    if args.list_models:
        require_credentials()
        print(f"backend: {backend_description()}")

        if backend() == "gemini":
            body = _request_or_explain(f"{C.GEMINI_ENDPOINT}/models", None, method="GET",
                                       api_key=C.GEMINI_API_KEY)
            for m in body.get("models", []):
                name = m.get("name", "")
                if "tts" in name.lower() or "AUDIO" in (m.get("supportedGenerationMethods") or []):
                    print(" ", name.removeprefix("models/"), "—", m.get("displayName"))
        else:
            # Dil filtri ilə sorğu Gemini səslərini göstərmir (onlar model əsaslıdır),
            # ona görə bütün siyahı çəkilir və üç kəsimdə göstərilir.
            body = _request_or_explain(
                "https://texttospeech.googleapis.com/v1/voices", None, method="GET",
            )
            voices = body.get("voices", [])
            print(f"  cəmi {len(voices)} səs")

            named = [v for v in voices if C.VOICE.lower() in (v.get("name") or "").lower()]
            print(f"\n  «{C.VOICE}» adını daşıyanlar ({len(named)}):")
            for v in named:
                print("   ", v.get("name"), v.get("languageCodes"))

            local = [v for v in voices if C.LANGUAGE_CODE in (v.get("languageCodes") or [])]
            print(f"\n  {C.LANGUAGE_CODE} səsləri ({len(local)}):")
            for v in local:
                print("   ", v.get("name"))

            families = sorted({(v.get("name") or "").split("-")[2] for v in voices
                               if len((v.get("name") or "").split("-")) > 2})
            print("\n  səs ailələri:", ", ".join(families))
        return 0

    if not args.dry_run:
        require_credentials()

    chapters = {int(x) for x in args.chapters.split(",") if x.strip()}
    items = plan(load_source(), args.kind, chapters)
    if args.limit:
        items = items[: args.limit]

    todo = [(k, t) for k, t in items if not cache_path(t).exists()]
    chars = sum(len(t) for _, t in todo)
    print(f"backend: {backend_description()}")
    print(f"{len(items)} parça, keşdə {len(items) - len(todo)}, render ediləcək {len(todo)} "
          f"({chars} simvol)")
    if args.dry_run or not todo:
        return 0

    done = {"n": 0}
    lock = threading.Lock()
    failures = []

    def work(pair):
        key, text = pair
        try:
            synthesize_chunk(text)
        except Exception as e:                      # noqa: BLE001 — hesabat üçün toplanır
            with lock:
                failures.append((key, str(e)[:200]))
        with lock:
            done["n"] += 1
            if done["n"] % 25 == 0 or done["n"] == len(todo):
                rate = PACER.rpm
                left = (len(todo) - done["n"]) / max(rate, 0.1)
                print(f"  {done['n']}/{len(todo)}  ({rate:.1f} sorğu/dəq, "
                      f"qalıq ~{left:.0f} dəq, {len(failures)} xəta)", flush=True)

    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        list(pool.map(work, todo))

    print("token uçotu:", usage_summary())

    if failures:
        print(f"\n{len(failures)} parça alınmadı:")
        for key, err in failures[:10]:
            print("  ", key, err)
        return 1
    print("hamısı keşdədir")
    return 0


if __name__ == "__main__":
    sys.exit(main())
