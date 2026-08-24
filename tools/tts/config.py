"""Ortaq konfiqurasiya — bütün skriptlər buradan oxuyur.

Sirr saxlanmır: Supabase anon açarı onsuz da tətbiq kodundadır (`SupabaseProvider.kt`),
Google açarı isə yalnız mühit dəyişənindən (`GOOGLE_API_KEY`) və ya `gcloud`-dan gəlir.
"""
import os
from pathlib import Path

# ==================== Yollar ====================

ROOT = Path(__file__).resolve().parents[2]          # repo kökü
TOOL_DIR = Path(__file__).resolve().parent
BUILD = ROOT / "build" / "tts"                       # generasiya çıxışı (git-ə düşmür)
CACHE = BUILD / "cache"                              # hash → wav keşi
WAV_DIR = BUILD / "wav"                              # birləşdirilmiş surə/hədis wav-ları
OUT_DIR = BUILD / "out"                              # yüklənəcək mp3-lər
SOURCE_JSON = BUILD / "source.json"                  # fetch.py çıxışı
INVENTORY = ROOT / "inventory" / "recitations"

# ==================== Supabase ====================

SUPABASE_URL = "https://molyqwcaynvsdmixtcbc.supabase.co/rest/v1/"
# Tətbiqdəki ilə eyni public anon açar (SupabaseProvider.kt), yalnız oxuma üçün.
SUPABASE_ANON_KEY = (
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
    "eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1vbHlxd2NheW52c2RtaXh0Y2JjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODA0MTYwOTcsImV4cCI6MjA5NTk5MjA5N30."
    "ceK_Sof_wKibBpNpfp3nEU6535MvewPm1HSGKrRVm9M"
)
TRANSLATION_SLUG = "az"

# ==================== Google TTS ====================

# İki backend dəstəklənir, hansı açar varsa o seçilir:
#
#  1. `GEMINI_API_KEY`  → Gemini API (generativelanguage.googleapis.com).
#     AI Studio-dakı ilə **eyni** yoldur — səsi orada bəyənmisənsə, bu.
#     Açar: https://aistudio.google.com/apikey (kredit kartı tələb etmir).
#  2. `GOOGLE_API_KEY` və ya `gcloud` → Cloud TTS (texttospeech.googleapis.com).
#     GCP layihəsi + billing tələb edir, əvəzində kvota daha yüksəkdir.
GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY", "")
GOOGLE_API_KEY = os.environ.get("GOOGLE_API_KEY", "")
GOOGLE_PROJECT = os.environ.get("GOOGLE_CLOUD_PROJECT", "")

GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta"
CLOUD_TTS_ENDPOINT = "https://texttospeech.googleapis.com/v1/text:synthesize"

# Seçilmiş səs və model (2026-08-24, İxlas surəsi üzərində 2.5 Flash / 2.5 Pro / 3.1 Flash
# müqayisəsindən sonra): **Gemini 3.1 Flash TTS + Iapetus**.
# Qiymət (Cloud TTS cədvəli): audio çıxışı $20 / 1M token, giriş $1 / 1M token
# → Quran ≈ $50, hədis ≈ $101. 2.5 Flash yarı qiymətə idi, fərq bilərəkdən qəbul edildi.
# Cloud TTS backend-ində eyni modelin adı fərqlidir (`gemini-3.1-flash-tts` — API açılanda
# `--list-models` ilə təsdiqlənməlidir); Gemini API-də ad `-preview` ilə bitir.
# ⚠️ Model adı API-də vaxtaşırı dəyişir (preview → GA) və iki backend-də fərqlidir.
# 2026-08-24-də Gemini API-nin verdiyi siyahı: gemini-3.1-flash-tts-preview,
# gemini-2.5-flash-preview-tts, gemini-2.5-pro-preview-tts. 404 gəlsə
# `python3 tools/tts/synthesize.py --list-models` işlət, sonra `export TTS_MODEL=<ad>`.
TTS_MODEL = os.environ.get("TTS_MODEL", "gemini-3.1-flash-tts-preview")
VOICE = os.environ.get("TTS_VOICE", "Iapetus")
LANGUAGE_CODE = os.environ.get("TTS_LANG", "az-AZ")

# Gemini TTS-də üslub SSML ilə yox, təbii dildə verilir və mətnin əvvəlinə qoşulur.
# Bütün çağırışlarda EYNİ qalmalıdır — yoxsa ayələr arasında tembr sürüşür
# (dəyişdirsən keş hash-i də dəyişir, yəni bütün korpus yenidən render olunur).
STYLE_PROMPT = os.environ.get(
    "TTS_PROMPT",
    # 2026-08-24-də İxlas surəsi üzərində üç variant dinlənildi (ləng / təbii / canlı);
    # seçilən budur. ⚠️ Bu mətn keş hash-inin içindədir — dəyişmək bütün korpusu
    # (≈3M simvol) yenidən render etmək deməkdir.
    "Read the following Azerbaijani text clearly and fluently, at a brisk but unhurried "
    "pace, like an experienced audiobook narrator. Keep the same tone and pace throughout. "
    "Read only the text, do not add anything:",
)

# 24 kHz mono 16-bit PCM. Cloud TTS bunu RIFF/WAV kimi qaytarır, Gemini API isə
# başlıqsız xam PCM — `synthesize.py` ikincisini WAV-a bükür ki, yığım eyni olsun.
AUDIO_ENCODING = "LINEAR16"
SAMPLE_RATE = 24000
SAMPLE_WIDTH = 2
CHANNELS = 1

# Bir sorğuya göndərilən maksimum mətn (bayt). Ayələr onsuz da altındadır;
# uzun hədislər cümlə sərhədində bölünür.
MAX_REQUEST_BYTES = 1500

# ==================== Audio yığımı ====================

VERSE_GAP_MS = 600        # ayələr arası sükut
HADITH_GAP_MS = 500       # hədis daxilində abzas fasiləsi
LEAD_IN_MS = 300          # faylın əvvəlindəki sükut
QURAN_BITRATE = "48k"
HADITH_BITRATE = "32k"

# ==================== Yayım ====================

GH_REPO = "cafarovceyxun/AnaMuslim"
QURAN_TAG = "tts-az-quran-v1"
HADITH_TAG = "tts-az-hadith-v1"
RECITER_ID = "tts_az_v1"
QURAN_URL_TEMPLATE = (
    f"https://github.com/{GH_REPO}/releases/download/{QURAN_TAG}/{{chapNo:%03d}}.mp3"
)
# Vaxt cədvəli tətbiqin paketindədir (bax: `RecitationModelManager.bundledTranslationReciters`).
# Şəbəkə asılılığı olmasın deyə — yeniləmək tətbiq buraxılışı tələb edir, əvəzində ayə sinxronu
# heç vaxt «fayl hələ push olunmayıb» səbəbindən sınmır.
TIMING_URL = "asset://recitation_timings/tts_az_v1.json.gz"

# `assemble.py` cədvəli avtomatik olaraq bura da kopyalayır ki, ikisi sürüşməsin.
BUNDLED_TIMING_PATH = ROOT / "shared/src/commonMain/composeResources/files/recitation_timings/tts_az_v1.json.gz"
TIMING_VERSION = 1


def ensure_dirs():
    for d in (BUILD, CACHE, WAV_DIR, OUT_DIR):
        d.mkdir(parents=True, exist_ok=True)
