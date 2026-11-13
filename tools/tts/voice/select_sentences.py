"""Öz səsinlə TTS öyrətmək üçün oxunacaq cümlələri seçir.

Mənbə `build/tts/source.json`-dur (ayələr + hədislər) — model məhz bu üslubda mətn oxuyacaq,
ona görə yazı dili də elə budur. Mətn cümlələrə bölünür, oxunması 4–14 saniyəlik olanlar
saxlanılır, sonra acgöz (greedy) seçimlə hərf bigram/trigram-larını ən çox örtən cümlələr
ardıcıl götürülür — siyahının əvvəli ən çox «yeni səs birləşməsi» verir, yəni harada
dayansan da o ana qədər yazdıqların ən balanslı hissədir.

Çıxış: build/tts/myvoice/script.tsv  (id <TAB> mətn)
İşlət:  python3 tools/tts/voice/select_sentences.py [--count 1000]
"""
import argparse
import collections
import heapq
import json
import math
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
SOURCE = ROOT / "build/tts/source.json"
OUT_DIR = ROOT / "build/tts/myvoice"

MIN_CHARS, MAX_CHARS = 45, 170          # ~13 simvol/san → ≈4–13 san
BAD = re.compile(r"[0-9(){}\[\]«»/\\_*#@&%=+<>|~]")
SPLIT = re.compile(r"(?<=[.!?])\s+(?=[A-ZƏÖÜÇŞĞİI\"'—-])")


def lower_az(s: str) -> str:
    return s.replace("I", "ı").replace("İ", "i").lower().replace("i̇", "i")


TRANSLIT = re.compile(r"['′ʼ`´]")              # ərəbcə transliterasiya (dua mətnləri)
# azərbaycancada «və» yazılır — tək «va» və bu sözlər transliterasiya olunmuş dua deməkdir
TRANSLIT_WORDS = {"va", "vaş", "val", "vas", "vat", "vad", "ləkə", "ləkəl", "bikə", "əntə",
                  "allahummə", "ləbbeykə", "subhənəkə", "lilləhi", "ileykə", "minnə"}


def sentences(text: str):
    text = re.sub(r"\s+", " ", text.replace("\r", " ")).strip()
    text = re.sub(r"([,;:!?])(?=[^\s\"'.,;:!?])", r"\1 ", text)   # «dedi:Qureyş» → «dedi: Qureyş»
    text = re.sub(r"(?<!\.)\.\.(?!\.)", ".", text)                 # «edərdi..» → «edərdi.»
    for s in SPLIT.split(text):
        s = s.strip().strip("-— ").strip()
        if s.startswith("...") or s.endswith("..."):   # ayə parçası — yarımçıq intonasiya
            continue
        if s.count('"') % 2:                     # yarımçıq sitat — oxunuşu qəribə olur
            s = s.replace('"', "")
        if (MIN_CHARS <= len(s) <= MAX_CHARS and not BAD.search(s) and not TRANSLIT.search(s)
                and s[-1] in ".!?"):
            yield s


def words(s: str):
    return re.findall(r"[a-zəöüçşğıxq]+", lower_az(s))


def units(s: str):
    t = "#" + re.sub(r"[^a-zəöüçşğıxq ]", "", lower_az(s)).replace(" ", "#") + "#"
    return {t[i:i + n] for n in (2, 3) for i in range(len(t) - n + 1)}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--count", type=int, default=1000)
    args = ap.parse_args()

    src = json.loads(SOURCE.read_text("utf-8"))
    pool, seen = [], set()
    for v in src["verses"]:
        for s in sentences(v["spoken"]):
            if s not in seen:
                seen.add(s); pool.append(s)
    for h in src["hadith"]:
        for s in sentences(h.get("spoken") or h["raw"]):
            if s not in seen:
                seen.add(s); pool.append(s)

    # Korpusda 1–2 dəfə rast gələn söz ya ərəbcə dua transliterasiyası, ya da yapışmış söz
    # xətasıdır («ayaqyalınadamların»). Belə sözlər çox olan cümlə həm oxunmur, həm də
    # seçicini «nadir birləşmə» kimi aldadıb siyahının başına çıxır.
    wc = collections.Counter(w for s in pool for w in words(s))
    def clean(s):
        ws = words(s)
        rare = sum(wc[w] <= 2 for w in ws)
        if TRANSLIT_WORDS & set(ws):
            return False
        return ws and rare <= 1 and rare / len(ws) < 0.15
    pool = [s for s in pool if clean(s)]

    cand = [(s, units(s)) for s in pool]
    freq = collections.Counter(u for _, us in cand for u in us)
    weight = {u: 1.0 / math.log(2 + c) for u, c in freq.items()}   # nadir birləşmə daha qiymətli
    covered = collections.Counter()

    def score(i):
        s, us = cand[i]
        # artıq görünən birləşmənin dəyəri hər təkrarda yarıya düşür
        return sum(weight[u] * 0.5 ** covered[u] for u in us) / math.sqrt(len(s))

    # «lazy greedy»: bal yalnız azala bilər, ona görə köhnə bal yuxarı sərhəddir
    heap = [(-score(i), i) for i in range(len(cand))]
    heapq.heapify(heap)
    chosen = []
    while heap and len(chosen) < args.count:
        _, i = heapq.heappop(heap)
        fresh = score(i)
        if heap and fresh < -heap[0][0]:
            heapq.heappush(heap, (-fresh, i))
            continue
        chosen.append(cand[i][0])
        covered.update(cand[i][1])

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    out = OUT_DIR / "script.tsv"
    with out.open("w", encoding="utf-8") as f:
        for n, s in enumerate(chosen, 1):
            f.write(f"az_{n:04d}\t{s}\n")

    total_chars = sum(map(len, chosen))
    print(f"hovuz: {len(pool)} cümlə → seçildi {len(chosen)}")
    print(f"təxmini səs: {total_chars / 13 / 3600:.1f} saat (~13 simvol/san)")
    print(f"bigram/trigram örtüyü: {len(covered)}/{len(freq)}")
    for k in (100, 300, 500):
        if k <= len(chosen):
            cov = len({u for s in chosen[:k] for u in units(s)})
            print(f"  ilk {k}: {cov / len(freq):.0%} örtük")
    print(out)


if __name__ == "__main__":
    main()
