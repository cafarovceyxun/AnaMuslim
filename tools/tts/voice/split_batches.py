"""AI Studio-dan gələn 20 cümləlik qrup fayllarını tək-tək cümlələrə bölür.

Giriş:  build/tts/myvoice/batches/az_0001-0020.wav (+ eyni adlı .txt — sıra ilə 20 cümlə)
Çıxış:  build/tts/myvoice/wavs/az_0001.wav … , build/tts/myvoice/split_report.tsv

Üsul (əsas): Whisper (MLX, large-v3-turbo) qrupu söz vaxtları ilə yazıya çevirir, sözlər
mətnlə tutuşdurulur və hər cümlə sərhədi üçün zaman pəncərəsi çıxır — kəsik o pəncərədəki
ən uzun sükuta düşür. Whisper tutuşmasa aşağıdakı DP ehtiyat yoldur.

Ehtiyat üsul: sükut bölgələri (-60 dBFS-dən aşağı, ≥120 ms) kəsik namizədləridir. Cümlələr arası
fasilə vergül fasiləsindən həmişə uzun olmur, ona görə sadəcə «ən uzun 19 sükut» götürülmür —
dinamik proqramlaşdırma elə 19 kəsik seçir ki, hər hissənin müddəti cümlənin mətn
uzunluğundan gözlənilən müddətə yaxın olsun və kəsiklər uzun sükutlara düşsün.
Gözləniləndən çox fərqlənən hissə hesabatda ⚠️ ilə işarələnir (Gemini cümləni buraxıb və ya
təkrarlayıbsa bu belə görünür) — həmin qrupu yenidən səsləndirmək lazımdır.

İşlət: uv run --with mlx-whisper --with scipy python3 tools/tts/voice/split_batches.py
"""
import difflib
import math
import re
import wave
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parents[3]
BASE = ROOT / "build/tts/myvoice"
BATCHES = BASE / "batches"
WAVS = BASE / "wavs"
REPORT = BASE / "split_report.tsv"

HOP = 0.01            # san
WIN = 0.025
SIL_DB = -60.0
MIN_SIL = 0.12
PAD = 0.15            # hissənin hər iki tərəfində saxlanılan sükut


def read(path):
    with wave.open(str(path)) as w:
        sr = w.getframerate()
        a = np.frombuffer(w.readframes(w.getnframes()), "<i2").astype("float32") / 32768
    return a, sr


def write(path, a, sr):
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(sr)
        w.writeframes((np.clip(a, -1, 1) * 32767).astype("<i2").tobytes())


def silences(a, sr):
    hop, win = int(HOP * sr), int(WIN * sr)
    n = max(0, (len(a) - win) // hop)
    frames = np.lib.stride_tricks.sliding_window_view(a, win)[::hop][:n]
    db = 20 * np.log10(np.sqrt((frames ** 2).mean(axis=1)) + 1e-9)
    quiet = db < SIL_DB
    runs, i = [], 0
    while i < n:
        if quiet[i]:
            j = i
            while j < n and quiet[j]:
                j += 1
            runs.append((i * HOP, j * HOP + WIN))
            i = j
        else:
            i += 1
    return runs, n * HOP


def weight(text):
    """Mətnin «oxunma ağırlığı»: hərflər + durğu fasilələri."""
    letters = len(re.findall(r"\w", text))
    return letters + 6 * len(re.findall(r"[,;:]", text)) + 4 * len(re.findall(r"[.!?\"]", text))


def split_points(runs, total, texts):
    """19 daxili kəsik seçir. Qaytarır: [(başlanğıc, son)] hər cümlə üçün."""
    k = len(texts)
    # baş və son sükutu kənara at
    speech_start = runs[0][1] if runs and runs[0][0] <= 0.05 else 0.0
    speech_end = runs[-1][0] if runs and runs[-1][1] >= total - 0.05 else total
    inner = [r for r in runs if r[0] > speech_start + 0.3 and r[1] < speech_end - 0.3
             and r[1] - r[0] >= MIN_SIL]
    w = [weight(t) for t in texts]
    span = speech_end - speech_start
    exp = [span * x / sum(w) for x in w]

    cand = [(s + e) / 2 for s, e in inner]
    gap = [e - s for s, e in inner]
    m = len(cand)
    if m < k - 1:
        return None, exp

    def seg_cost(a, b, e):
        d = b - a
        return 12.0 * ((d - e) / e) ** 2

    INF = float("inf")
    # dp[c][j]: c-ci kəsik j namizədindədir
    dp = [[INF] * m for _ in range(k)]
    back = [[-1] * m for _ in range(k)]
    for j in range(m):
        dp[1][j] = seg_cost(speech_start, cand[j], exp[0]) - math.log(gap[j])
    for c in range(2, k):
        for j in range(c - 1, m):
            best, arg = INF, -1
            for p in range(c - 2, j):
                if dp[c - 1][p] == INF:
                    continue
                v = dp[c - 1][p] + seg_cost(cand[p], cand[j], exp[c - 1])
                if v < best:
                    best, arg = v, p
            if arg >= 0:
                dp[c][j] = best - math.log(gap[j])
                back[c][j] = arg
    best, last = INF, -1
    for j in range(k - 2, m):
        v = dp[k - 1][j] + seg_cost(cand[j], speech_end, exp[k - 1])
        if v < best:
            best, last = v, j
    idx = [last]
    for c in range(k - 1, 1, -1):
        idx.append(back[c][idx[-1]])
    idx.reverse()

    bounds, prev_end = [], speech_start
    for j in idx:
        s, e = inner[j]
        bounds.append((prev_end, s))
        prev_end = e
    bounds.append((prev_end, speech_end))
    return bounds, exp


def norm_word(w):
    w = w.replace("I", "ı").replace("İ", "i").lower().replace("i̇", "i")
    return re.sub(r"[^a-zəöüçşğıxq]", "", w)


def whisper_words(a, sr):
    import mlx_whisper
    from scipy.signal import resample_poly
    g = math.gcd(16000, sr)
    a16 = resample_poly(a, 16000 // g, sr // g).astype("float32")
    res = mlx_whisper.transcribe(a16, path_or_hf_repo="mlx-community/whisper-large-v3-turbo",
                                 language="az", word_timestamps=True,
                                 condition_on_previous_text=False)
    out = []
    for seg in res["segments"]:
        for w in seg.get("words", []):
            n = norm_word(w["word"])
            if n:
                out.append((n, w["start"], w["end"]))
    return out


def split_points_whisper(a, sr, runs, total, texts):
    """Whisper söz vaxtları ilə hər cümlə sərhədinin pəncərəsini tapır, kəsiyi o pəncərədəki
    ən uzun sükuta qoyur. Uzun/qısa fasilə səhvlərini (DP-nin zəif yeri) aradan qaldırır."""
    hyp = whisper_words(a, sr)
    ref, owner = [], []
    for si, t in enumerate(texts):
        for w in t.split():
            n = norm_word(w)
            if n:
                ref.append(n); owner.append(si)
    sm = difflib.SequenceMatcher(None, ref, [h[0] for h in hyp], autojunk=False)
    pairs = [(b.a + k, b.b + k) for b in sm.get_matching_blocks() for k in range(b.size)]
    if len(pairs) < 0.4 * len(ref):
        return None
    speech_start = runs[0][1] if runs and runs[0][0] <= 0.05 else 0.0
    speech_end = runs[-1][0] if runs and runs[-1][1] >= total - 0.05 else total
    cuts = []
    for si in range(len(texts) - 1):
        before = [hyp[h][2] for r, h in pairs if owner[r] <= si]
        after = [hyp[h][1] for r, h in pairs if owner[r] >= si + 1]
        if not before or not after:
            return None
        t1, t2 = max(before), min(after)
        lo, hi = min(t1, t2) - 0.3, max(t1, t2) + 0.3
        prev = cuts[-1][1] if cuts else speech_start
        opts = [r for r in runs if r[1] > lo and r[0] < hi and r[0] > prev + 0.3]
        if opts:
            cuts.append(max(opts, key=lambda r: r[1] - r[0]))
        else:
            # Gemini bəzən cümlələr arasında heç fasilə vermir — pəncərənin ən sakit kadrı
            i0, i1 = int(max(lo, prev + 0.3) * sr), int(hi * sr)
            win = int(WIN * sr)
            if i1 - i0 <= win:
                return None
            seg = a[i0:i1]
            frames = np.lib.stride_tricks.sliding_window_view(seg, win)[::int(HOP * sr)]
            t = (i0 + int(np.argmin((frames ** 2).mean(axis=1))) * int(HOP * sr) + win // 2) / sr
            cuts.append((t, t))
    bounds, prev_end = [], speech_start
    for s, e in cuts:
        bounds.append((prev_end, s)); prev_end = e
    bounds.append((prev_end, speech_end))
    return bounds


def main():
    WAVS.mkdir(parents=True, exist_ok=True)
    rows = ["id\tqrup\tmüddət\tgözlənilən\tnisbət\tqeyd\tüsul"]
    flagged = set()
    for wav in sorted(BATCHES.glob("az_*.wav")):
        txt = wav.with_suffix(".txt")
        if not txt.exists():
            print(f"{wav.name}: mətn faylı yoxdur, keçildi"); continue
        texts = [l for l in txt.read_text("utf-8").splitlines() if l.strip()]
        first = int(re.match(r"az_(\d+)-", wav.name).group(1))
        a, sr = read(wav)
        runs, total = silences(a, sr)
        dp_bounds, exp = split_points(runs, total, texts)
        bounds = split_points_whisper(a, sr, runs, total, texts)
        method = "whisper"
        if bounds is None:
            bounds, method = dp_bounds, "dp"
            print(f"{wav.name}: Whisper uyğunlaşmadı, DP ilə bölündü")
        if bounds is None:
            print(f"{wav.name}: ⚠️ sükut azdır, bölünmədi"); flagged.add(wav.stem); continue
        for n, ((s, e), x) in enumerate(zip(bounds, exp)):
            sid = f"az_{first + n:04d}"
            # pad qonşu cümlənin səsinə keçməsin: yalnız sükutun öz içində genişlən
            prev_e = bounds[n - 1][1] if n else 0.0
            next_s = bounds[n + 1][0] if n + 1 < len(bounds) else len(a) / sr
            s0 = max(0, s - PAD, (prev_e + s) / 2)
            e0 = min(len(a) / sr, e + PAD, (e + next_s) / 2)
            write(WAVS / f"{sid}.wav", a[int(s0 * sr):int(e0 * sr)], sr)
            ratio = (e - s) / x
            note = "⚠️" if not 0.65 <= ratio <= 1.5 else ""
            if note:
                flagged.add(wav.stem)
            rows.append(f"{sid}\t{wav.stem}\t{e - s:.2f}\t{x:.2f}\t{ratio:.2f}\t{note}\t{method}")
    REPORT.write_text("\n".join(rows) + "\n", "utf-8")
    n = len(rows) - 1
    print(f"{n} cümlə yazıldı → {WAVS}")
    print(f"hesabat: {REPORT}")
    print("şübhəli qruplar:", ", ".join(sorted(flagged)) or "yoxdur")


if __name__ == "__main__":
    main()
