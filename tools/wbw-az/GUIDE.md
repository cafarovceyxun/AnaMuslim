# Azərbaycanca sözbəsöz (WBW) mənalar — qaydalar

Məqsəd: Quranın hər ərəbcə sözünə (Osmani yazı, `quranapp.db`) qısa azərbaycanca məna. Tətbiqdə hər ərəbcə
sözün altında göstərilir. Bu, Mürşüd Yusifoğlunun azərbaycanca tərcüməsinə (hər ayədə `az:` sətri) uyğun olmalıdır.

## Giriş formatı (`in/NN.txt`)
```
### 2:5 [n=8]          ← ayə, n = söz sayı (ayə nömrəsi işarəsi daxil deyil)
az: <ayənin azərbaycanca tərcüməsi>
0 <ərəbcə söz> | tr: <türkcə sözbəsöz> | en: <ingiliscə sözbəsöz>
...
```

## Qaydalar
1. **Ərəbcə əsasdır.** Məna ərəbcə sözün bu ayədəki mənasıdır. Türkcə/ingiliscə sözbəsöz kömək üçündür
   (seqmentləşmə onlarla eynidir — hər sətir bütöv ərəbcə sözdür, ön/son şəkilçilər daxil). Azərbaycanca tərcümə
   **terminologiya** üçündür. Tərcümə ərəbcəyə zidd görünürsə (fail, əvəzlik, inkar, zaman) — ərəbcəyə uy.
2. **Terminologiya tərcümədən:** sözü tərcümədə necə verilibsə elə yaz — Allah, Rəbb (Rəbbin, Rəbbiniz…),
   ər-Rahmən, ər-Rahim, Kitab, ayə(t), rəsul, nəbi, iman edənlər/möminlər, kafirlər, müşriklər, münafiqlər,
   müttəqilər, namaz, zəkat, Cənnət, Cəhənnəm, Od, axirət, dünya, salehlər, zalımlar, fasiqlər, hidayət, zikr…
   Xüsusi adlar tərcümədəki kimi: Musa, İbrahim, Nuh, Lut, Firon, İsrail oğulları, Məryəm, İsa, Cəbrayıl/Cibril,
   Bədr, Məkkə, Tur (dağı)… Allaha aid əvəzliklər **böyük** hərflə (O, Onun, Ona, Biz, Bizim, Mən, Sən — Allaha xitab).
3. **Şəkilçili sözlər** bir ərəbcə sözdür, mənası da bir yerdə: وَ → "və …", فَ → "sonra/elə isə …",
   بِ → "… ilə", لِ → "… üçün", كَ → "… kimi", sonluq əvəzlikləri → yiyəlik/təsirlik (كِتَٰبَهُۥ "onun kitabını").
   Hal şəkilçisi azərbaycanca cümlədəki rolu göstərsin ("Rəbbinin", "Allaha", "yerdə").
4. **Qısa:** adətən 1–4 söz. Təfsir, izah, alternativ siyahısı **yazma**. Feil zamanı/şəxsi ərəbcəyə uyğun
   (قَالُوا "dedilər", يَقُولُونَ "deyirlər", سَيَقُولُ "deyəcək", لَا تَقُولُوا "deməyin").
5. **Mötərizə** yalnız ərəbcədə yazılmayan, amma azərbaycancada lazım olan kiçik söz üçün (ingiliscə "(of)", "(is)"
   kimi) və yalnız zərurətdə. Qayda: məna həm mötərizə ilə, həm də mötərizəsiz düz oxunmalıdır.
   Ərəbcədə olan söz mötərizəyə **girmir**.
6. Hərfi-müqəttəat (الٓمٓ, حمٓ, يسٓ…) → hərflərin oxunuşu: "Əlif, Ləm, Mim", "Ha, Mim", "Ya, Sin".
7. Yalnız azərbaycan latın əlifbası (ə, ö, ü, ğ, ı, ş, ç). Türkcə formaları ("ve", "için", "değil", "ile",
   "onlar için" yaxşıdır, amma "Rabb'in", "Allah'ın" kimi apostrof yox) işlətmə: "Allahın", "Rəbbinin".

## Çıxış
Hər hissə üçün JSON obyekti: açar "surə:ayə", dəyər — **düz n elementli** sətir massivi (0..n-1 sırası ilə).
```json
{"1:1": ["adı ilə", "Allahın", "Rahmən", "Rahim olan"], "1:2": ["Həmd", "Allaha məxsusdur", "Rəbbi", "aləmlərin"]}
```
Böyük faylı hissə-hissə yaz: `out/NN_a.json`, `out/NN_b.json`, … (hər birində ~40–80 ayə). Hər hissəni yazandan
sonra yoxla: `python3 validate.py NN` — bütün xətalar (əskik ayə, say uyğunsuzluğu, ərəbcə/türkcə simvol) çap olunur.
Hissə tam və xətasız olana qədər davam et.

## Davam etmə (kəsintidən sonra)
Başlamazdan əvvəl `python3 validate.py NN --partial` işlət və `out/NN_*.json` fayllarına bax: artıq yazılmış ayələri
**təkrar yazma**, qalan ilk ayədən başla və **yeni hərfli** fayl adı ilə yaz (mövcud ən son hərfdən sonrakı).
Hər ~40 ayədən bir fayl yaz ki, iş yarıda kəsiləndə itməsin.
