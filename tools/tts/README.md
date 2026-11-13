# tools/tts — azərbaycanca səsləndirmə boru xətti

Quran tərcüməsini (6236 ayə) və hədisləri (1487) **Google Cloud TTS / Gemini `Iapetus`**
səsi ilə bir dəfə render edir, surə başına mp3 + ayə vaxt cədvəli çıxarır, GitHub
Releases-ə yükləyir. Tətbiq onları adi «tərcümə qarisi» kimi oxuyur — pleyer tərəfində
yeni format yoxdur.

## Tələblər

**1. Açar** — iki backend-dən biri (hansı dəyişən varsa o seçilir):

```bash
export GEMINI_API_KEY=...    # AI Studio: https://aistudio.google.com/apikey  ← ən qısa yol
export GOOGLE_API_KEY=...    # alternativ: Cloud TTS (GCP layihəsi + billing)
```

**2. mp3 kodlayıcı** — macOS-un `afconvert`-i mp3 kodlaya **bilmir**, ona görə biri lazımdır:

```bash
pip3 install lameenc         # kiçik wheel, Homebrew tələb etmir
brew install ffmpeg          # Homebrew varsa
```

Python: qalan hər şey standart kitabxanadır. Supabase anon açarı `config.py`-dədir (tətbiqdəki ilə
eynidir, yalnız oxuma).

⚠️ **Model adını əvvəlcə yoxla** — `gemini-3.1-flash-tts` default-dur, amma API-də adlar dəyişir:

```bash
python3 tools/tts/synthesize.py --list-models
export TTS_MODEL=<siyahıdakı ad>
```

## Axın

```bash
python3 tools/tts/fetch.py                       # Supabase → build/tts/source.json
python3 tools/tts/normalize.py --report          # qaydaların nə etdiyini göstərir
python3 tools/tts/sample.py --chapter 112        # Faza 0: dinlə, səsi təsdiqlə
python3 tools/tts/synthesize.py --list-models    # cari model adları
python3 tools/tts/synthesize.py --dry-run        # neçə sorğu/simvol qalıb
python3 tools/tts/synthesize.py --kind quran     # render (keş sayəsində təkrar ucuzdur)
python3 tools/tts/assemble.py --kind quran       # 114 mp3 + tts_az_timings.json(.gz)
python3 tools/tts/upload.py --kind quran         # GitHub Releases + manifest
```

Hədis üçün eyni ardıcıllıq, `--kind hadith`.

## Bilinməsi vacib olanlar

- **Keş mətnə bağlıdır.** `build/tts/cache/<hash>.wav` — hash mətn + model + səs +
  üslub promptundan çıxır. Prompt və ya səsi dəyişmək **bütün korpusu** yenidən
  ödəmək deməkdir; ona görə Faza 0 nümunəsi olmadan tam render başlatma.
- **Vaxt cədvəli PCM sample sayından çıxır**, mp3-dən yox. LAME-in kodlayıcı gecikməsi
  (~26 ms) 600 ms-lik ayə fasiləsinin içində itir.
- **Birləşmiş ayələr** (`-3, 4: …`) bazada boş sətir kimi görünür (11 hadisə). Onların
  audiosu ortağı ilə eynidir; `assemble.py` pəncərəni bərabər bölür, çünki üst-üstə düşən
  pəncərə tətbiqdəki `getVerseAtPosition` ikili axtarışını pozur.
- **73:1 bazada tamamilə boşdur** — tərcümə mətni yoxdur, səs də olmayacaq.
- **Normalizasiya qaydaları** real mətn üzərində sayılıb (haşiyə rəqəmləri, mötərizə
  formaları, hədis mənbə istinadları) — təfərrüat `normalize.py` başlığındadır.
  Bazadakı mətnin forması dəyişsə, əvvəlcə `--report` işlət.
- **İki backend fərqli səs verir.** Keş hash-inə backend adı da girir, ona görə Gemini API-dən
  Cloud TTS-ə keçmək bütün korpusu yenidən render etmək deməkdir. Birini seç və qal.
- **Gemini API xam PCM qaytarır** (`audio/L16`, başlıqsız) — `synthesize.py` onu WAV-a bükür,
  Cloud TTS isə onsuz da WAV verir. Yığım hər iki halda eynidir.
- **Commit istifadəçinindir.** Skriptlər `inventory/` altındakı manifest və vaxt cədvəlini
  yazır, amma heç nə commit etmir.

## Cihazda səsləndirmə — öz Piper modelimiz (2026-11-13)

Hədislər Iapetus ilə əvvəlcədən render olunmur: tətbiq öz Piper modelimizi (`az-v1`, Quran
Iapetus səsi üzərində öyrədilib — `voice/build_quran_dataset.py`) **telefonda** işlədir,
sherpa-onnx 1.13.8 ilə (Android AAR, iOS `SherpaOnnxC.xcframework`). Model tətbiqə qablaşdırılmır,
ilk «Dinlə»də bir dəfə **öz serverimizin storage-ından** endirilir (≈64 MB; serverin 50 MB
limitinə görə model iki hissədədir, telefon birləşdirir).

```bash
python3 -m venv /tmp/so && /tmp/so/bin/pip install onnx
/tmp/so/bin/python tools/tts/voice/export_sherpa.py      # → build/tts/sherpa/az-v1/ + ölçülər
python3 tools/tts/voice/export_sherpa.py upload          # → öz storage: public `tts` bucket-i, az-v1/
```

Modeli yeniləyəndə: `OfflineVoiceModel.FILES`-dəki ölçüləri skriptin çıxışından köçür, `ID`-ni və
storage qovluğunu (`az-v1/`) dəyiş (köhnə qovluğu tətbiq özü silir).

- **sherpa-onnx 2.0-a keçmə** — orada espeak-ng kitabxanadan çıxarılıb, model isə fonemləri ondan alır.
- **Yoxlanılıb:** noise 0 ilə Piper-in öz çıxışı və sherpa-onnx çıxışı eynidir (korrelyasiya 0.99999).
  Tək şərt `silence_scale = 1` (sherpa defoltu 0.2 fasilələri sıxır) — hər iki platformada qoyulub.
- **Mətn qaydaları** `normalize.py`-dən `shared/.../utils/speech/SpeechText.kt`-ə köçürülüb; birini
  dəyişəndə o birini də dəyiş (`SpeechTextTest` Python çıxışı ilə tutuşdurur).
