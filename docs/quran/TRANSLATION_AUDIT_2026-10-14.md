# Quran tərcüməsinin yoxlanışı — 2026-10-14

**Mənbə:** `quran_translations_data` (slug `az`, Mürşüd Yusifoğlu), 6236 ayə, öz serverdən (`anamuslim.cafarovceyxun.com`) 2026-10-14-də endirildi. `text_alt` boşdur — ikinci tərcümə yoxdur.

**Üsul:**
1. **Mexaniki yoxlama — bütün mətn, skriptlə:** mötərizə balansı, dırnaq simvolları, boşluqlar, ayə nömrəsi prefiksi, yetim dipnot rəqəmləri, təkrar sözlər.
2. **Məna və yazı yoxlaması — bütün 6236 ayə, 60 hissədə (hər hizb ayrıca) oxundu.** Şübhəli yerlər cihazdakı `quranapp.db`-dəki Osmani ərəbcə mətnlə tutuşduruldu.
3. **İkinci keçid (eyni gün, §4):** bütün 6236 ayə ərəbcə ilə **yan-yana** yenidən oxundu + avtomatik yoxlamalar (eyni ərəbcəli ayələrin tərcümədə fərqi, Allahın adlarının ərəbcə köklə tutuşdurulması, uzunluq nisbəti). Birinci keçidin buraxdığı ~100 qeyd tapıldı, o cümlədən bir hökm tərsinə çevrilməsi (2:196) və ayə hissələrinin düşdüyü 10-dan çox yer. Yeni qeydlər §1 və §2-də 🆕 ilə işarələnib.
4. Müəllifin üslub seçimləri (çoxlu izah mötərizəsi, "(, …)" yazılışı, `...` ilə ayələrarası keçid, `mə’ruf`-tipli apostrof) **xəta sayılmadı**. Təfsirə əsaslanan izahlar da sayılmadı — yalnız ərəbcəyə açıq zidd olanlar qeyd olundu.
5. **Mötərizə qaydası (2026-10-14, istifadəçinin göstərişi):** mötərizə təfsirdir, ona görə mətn **iki oxunuşda da** düzgün cümlə olmalıdır — mötərizə ilə (işarələr atılır, məzmun oxunur) və mötərizəsiz (məzmunla birlikdə atılır). Nəticələr: ərəbcədə olan söz (fa → "elə isə/o zaman", الَّذِي → "o … ki", feilin şəxs şəkilçisindəki "siz", ikilik formasındakı mübtəda, لَا أَبْرَحُ kimi əsas feil) mötərizəyə **girmir**; cümləni bağlayan "ki", vergül və mübtəda mötərizədən kənarda qalır; mötərizədən sonra gələn söz cümlənin başıdırsa, böyük hərf mötərizədən kənardakı sözə keçir (`Nə (Allah yolunda) infaq etsəniz`, `(Allah yolunda) nə…` yox); vergüllü əlavə müəllifin `(, …)` yazılışı ilə verilir. Təklif olunan mətnlər `python3 tools/quran-audit/build.py check-parens` ilə yoxlanır (formal izlər; `--all` hər yeni mötərizəni iki oxunuşda çap edir), 12 təklif bu qaydaya görə düzəldildi (`tools/quran-audit/paren_fixes.json`). Müəllifin **özünün** mövcud mötərizələri (`(Bilin ki,) kim…` — mötərizəsiz kiçik hərflə başlama) bu keçiddə toxunulmadı; məna və ya qrammatika qırılanlar (7:163, 29:35 kimi) ayrıca qeyd olunur — bütün mətn üzrə yoxlama §5-dədir (`tools/quran-audit/author_parens.py`).

> ⚠️ Bu, dini mətndir: «MƏNA» qeydlərindəki təklif olunan ifadələr düzəlişin istiqamətini göstərir, son variantı bilən birinin (müəllifin və ya alimin) təsdiqi ilə yazmaq lazımdır.

## 1. Ən vacib — məna və struktur xətaları

| Ayə | Problem | Təklif |
|---|---|---|
| 2:36 | **MƏNA** — "O, onları ondan yayındıraraq…" — ərəbcədə fail Şeytandır (فَأَزَلَّهُمَا الشَّيْطَانُ), böyük "O" isə Allahı bildirir | "Şeytan onları oradan sürüşdürərək (yayındıraraq)…" |
| 2:96 | **MƏNA (orta)** — "onların və müşriklərin … ən həris kimsələr olduğunu" — ərəbcədə وَمِنَ الَّذِينَ أَشْرَكُوا = "hətta müşriklərdən də (daha həris)"; tərcümə iki qrupu bərabərləşdirir | "…onları insanların, hətta müşriklərdən də, (dünya) həyat(ın)a ən həris kimsələri olaraq görəcəksən" |
| 🆕 2:196 | **MƏNA (ciddi — hökm)** — "qoy Həcdə yeddi gün, (əhlinin yanına) qayıtdıqda isə üç gün oruc tutsun" — ərəbcədə tərsinədir: فَصِيَامُ ثَلَاثَةِ أَيَّامٍ فِي الْحَجِّ وَسَبْعَةٍ إِذَا رَجَعْتُمْ | "qoy Həccdə üç gün, (əhlinin yanına) qayıtdıqda isə yeddi gün oruc tutsun" |
| 2:214 | **MƏNA (buraxılma)** — "hətta (sonunda) rəsul (belə) demişdi" — ərəbcədə الرَّسُولُ وَالَّذِينَ آمَنُوا مَعَهُ: "rəsul və onunla birlikdə iman gətirənlər" | "…hətta rəsul və onunla birlikdə iman gətirənlər (belə) demişdilər: …" |
| 🆕 2:282 | **MƏNA (orta — fail)** — "(katib də) ondan heç bir şeyi əskiltməsin" — وَلْيَتَّقِ اللَّهَ رَبَّهُ وَلَا يَبْخَسْ مِنْهُ شَيْئًا imla edən borcluya aiddir, katibə yox | "…Rəbbi olan Allahdan qorxsun və (borcdan) heç bir şeyi əskiltməsin" |
| 2:285 | **MƏNA (orta)** — "Biz Onun rəsullarından heç birinin arasında fərq qoymuruq" — bu möminlərin sözüdür (ərəbcədə gizli "deyirlər"), böyük "Biz" isə Allahın danışdığını göstərir | "(Onlar deyirlər:) “Biz Onun rəsullarından… fərq qoymuruq.”" — "biz" kiçik hərflə |
| 🆕 3:3–3:4 | **MƏNA (orta — struktur)** — مِن قَبْلُ هُدًى لِّلنَّاسِ Tövrat və İncilə aiddir, tərcümə onu Furqana bağlayır | 3:3 "…Tövratı və İncili də O nazil etdi," 3:4 "…bundan öncə, insanlara hidayət olaraq. Furqanı da O nazil etdi." |
| 🆕 3:92 | **BURAXILMA** — ayənin ikinci yarısı yoxdur: وَمَا تُنفِقُوا مِن شَيْءٍ فَإِنَّ اللَّهَ بِهِ عَلِيمٌ | "…nail olmayacaqsınız! Nə (Allah yolunda) infaq etsəniz, şübhəsiz ki, Allah onu Biləndir." |
| 🆕 3:143 | **MƏNA (orta — əvəzlik)** — "onlarla qarşılaşmadan … onları gördünüz" — مِن قَبْلِ أَن تَلْقَوْهُ فَقَدْ رَأَيْتُمُوهُ: tək əvəzlik ölümə aiddir, düşmənə yox | "…ölümlə qarşılaşmadan əvvəl onu təmənna edirdiniz. Artıq onu gördünüz, (ona) baxırsınız" |
| 4:128 | **MÖTƏRİZƏ (ciddi)** — "nəfslər simicliyə2, xəsisliyə) bağlıdır (,meyillidir. Odur ki, ey kişilər!)" — mötərizələr sürüşüb, mətn pozulub; "rəftardan1" yetim rəqəm | "(Bilin ki,) nəfslər simicliyə (xəsisliyə) bağlıdır (meyillidir). (Odur ki, ey kişilər!) Əgər…" |
| 🆕 5:33 | **BURAXILMA** — ayənin sonu yoxdur: ذَٰلِكَ لَهُمْ خِزْيٌ فِي الدُّنْيَا ۖ وَلَهُمْ فِي الْآخِرَةِ عَذَابٌ عَظِيمٌ | "…Bu, onlar üçün dünyada bir rüsvayçılıqdır, axirətdə isə onlar üçün əzəmətli bir əzab vardır!" |
| 5:54 | **MƏNA (orta)** — "Onlar möminlərə qarşı zillət içində olarlar" — أَذِلَّةٍ عَلَى الْمُؤْمِنِينَ burada "mülayim, təvazökar" deməkdir; azərbaycanca "zillət" alçalma/xarlıq anlamı verir | "Onlar möminlərə qarşı mülayim (təvazökar), kafirlərə qarşı isə sərt (izzətli) olarlar" |
| 5:73 | **AYDINLIQ** — "Allah üçün üçüncüsüdür" — "üçün" burada "üçünün (üçdən birinin)" mənasındadır, amma oxucu onu "üçün" qoşması kimi oxuyur (ثَالِثُ ثَلَاثَةٍ) | "Allah üçdən biridir (üç ilahın üçüncüsüdür)" — mötərizəsiz də düz oxunur: "Allah üçdən biridir" |
| 6:165 | **MƏNA (kiçik)** — "Sizə verdikləri şeylərdə" (cəm — "onların verdikləri") — fail Allahdır; "Rahimdir (çox əfv Edəndir)" — Rahim "rəhm edən"dir, "əfv edən" Afuvdur | "Sizə verdiyi şeylərdə"; "Rahimdir (çox rəhm Edəndir)" |
| 7:43 | **MƏNA (ciddi izah) + qrammatika + dırnaq** — Cənnət əhlinə: "Əməlləriniz (günahlarınız) səbəbi ilə varis olduğunuz Cənnət budur" — izah tərsdir, بِمَا كُنتُمْ تَعْمَلُونَ saleh əməllərdir; "Həmd olsun o Allaha bizi buna yönəltdi" — "ki" yoxdur; son sitat bağlanmır | "Əməlləriniz (saleh əməlləriniz) səbəbi ilə…budur!”"; "o Allaha ki, bizi buna yönəltdi" |
| 🆕 7:163 | **MƏNA (orta)** — "ov etmədikləri (qeyri-şənbə) günlərində" — وَيَوْمَ لَا يَسْبِتُونَ şənbəyə riayət etmədikləri (adi) günlərdir; "ov etmədikləri" mənanı tərsinə çevirir (mənanı yalnız mötərizə xilas edir, mötərizəsiz mətn əksini deyir) | "şənbəyə riayət etmədikləri (qeyri-şənbə) günlərdə isə gəlmirdi" — müəllifin mötərizəsi saxlanır; mötərizəsiz də düz oxunur |
| 🆕 8:64 | **AYDINLIQ (əqidə)** — "Allah və möminlərdən sənə ittiba edənlər sənə yetər" — əksər təfsirlərdə "Allah sənə də, sənə tabe olan möminlərə də yetər" (yetərlik yalnız Allaha aiddir, bax 3:173). Alimə göstərilsin | "Ey Nəbi! Allah sənə də, möminlərdən sənə tabe olanlara da yetər!" |
| 11:40 | **MƏNA** — "Ona hər kəsdən iki cütü … mindir" — "hər kəsdən" insan deməkdir; مِن كُلٍّ زَوْجَيْنِ اثْنَيْنِ = "hər birindən (hər canlı növündən) bir cüt (erkək və dişi)" | "Ona (gəmiyə) hər birindən (hər canlı növündən) bir cüt (erkək və dişi), … öz əhlini və iman edənləri mindir" — mötərizəsiz də düz oxunur: "hər birindən bir cüt" |
| 🆕 12:99 | **BURAXILMA** — إِن شَاءَ اللَّهُ tərcümədə yoxdur | "İnşallah (Allah istəsə), əmin-amanlıq içində Misirə daxil olun!" |
| 🆕 13:23 | **BURAXILMA** — وَأَزْوَاجِهِمْ ("zövcələrindən") düşüb | "…atalarından, zövcələrindən və zürriyyətlərindən saleh olanların…" |
| 🆕 13:35 | **BURAXILMA** — تَجْرِي مِن تَحْتِهَا الْأَنْهَارُ düşüb | "…budur: altından çaylar axır, onun yeməyi də daimidir, kölgəsi də." |
| 🆕 18:62 | **MƏNA (orta)** — فَلَمَّا جَاوَزَا "çatdıqda" verilib — "(oranı) keçib getdikdə"dir; 18:64-dəki geri dönüş başqa cür anlaşılmır | "Onlar (oranı) keçib getdikdə…" |
| 19:5 | **STRUKTUR (ciddi)** — 19:5-in mətni yalnız "!" işarəsidir; 5-ci ayənin mənası 19:6-ya qatılıb, 19:6 da "-5, 6:" prefiksi ilə saxlanıb | 19:5-ə öz tərcüməsi: "Həqiqətən də mən arxamca gələn mövlalarımdan … qorxuram, arvadım da sonsuzdur, (buna görə də) Öz qatından mənə bir vəli bəxş et!"; 19:6: "O (vəli) həm mənə, həm də Yaqub ailəsinə varis olsun! Rəbbim! Onu razı olunan kimsə et!" |
| 20:33/20:34 | **STRUKTUR** — 20:33 ("Səni çox təsbih edək…") 20:34-ün əvvəlində təkrarlanır; 20:34 = وَنَذْكُرَكَ كَثِيرًا, yalnız "və Səni çox zikr edək" | 20:34: "…və Səni çox zikr edək!" |
| 23:27 | **MƏNA** — "ona (gəmiyə) hər kəsdən iki cütü … daxil et" — 11:40-dakı eyni xəta; مِن كُلٍّ زَوْجَيْنِ = "hər birindən bir cüt" | "ona (gəmiyə) hər birindən (hər canlı növündən) bir cüt (erkək və dişi) … daxil et (mindir)" |
| 27:3 | **MƏTN (artıq əlavə)** — "axirətə yəqinliklə iman edərək namazı … qılırlar … və onlar axirətə də yəqinliklə iman edirlər" — ərəbcədə axirətə iman yalnız sonda bir dəfədir; əvvəlki ifadə artıqdır | "Onlar (o möminlər) ki, namazı … qılırlar, zəkatı verirlər və onlar axirətə də yəqinliklə iman edirlər." |
| 30:27 | **MƏNA (söz seçimi)** — "o, Onun üçün daha kiçik (xəfif, bəsit) olandır" — وَهُوَ أَهْوَنُ عَلَيْهِ "daha asandır" deməkdir | "bu, Onun üçün daha asandır" |
| 🆕 33:53 | **MƏNA (orta — hökm)** — "Sizə yemək verilməsini gözləməyiniz istisna olmaqla…" — إِلَّا أَن يُؤْذَنَ لَكُمْ إِلَىٰ طَعَامٍ غَيْرَ نَاظِرِينَ إِنَاهُ: "izn" şərti düşüb, "gözləmədən" "gözləmək"ə çevrilib; sonda "etməyimizin" → "etməyinizin" | "Sizə yeməyə izn verilməsi istisna olmaqla, (yeməyin) hazır olmasını gözləmədən Nəbinin evlərinə daxil olmayın…" |
| 🆕 40:67 | **MƏNA (söz)** — ولعلكم تعقلون "bəlkə şükr edəsiniz" | "bəlkə ağıl edəsiniz (dərk edəsiniz)" |
| 41:11 | **MƏNA (əvəzlik)** — "Sonra o, … səmaya yönəlib Ona və yerə dedi" — böyük/kiçik hərf tərsinədir: fail Allahdır (O), xitab səmayadır (ona) | "Sonra O, … səmaya yönəlib ona və yerə dedi" |
| 🆕 41:16 | **BURAXILMA** — في أيام نحسات ("uğursuz günlərdə") düşüb | "…uğursuz günlərdə qorxunc səsli, soyuq bir külək göndərdik…" |
| 🆕 43:86 | **MƏNA (fail)** — "Ondan başqasına dua edənlər şəfaətə malik deyillər" — الذين يدعون من دونه dua **edilənlərdir** (ilah sayılanlar) | "Onların Ondan başqa dua etdikləri şəfaətə malik deyillər; bildikləri halda haqqa şahidlik edənlər istisnadır" |
| 44:31 | **MƏNA (izah)** — "Firondan (firon kimi bir əzabdan xilas etdik)" — مِن فِرْعَوْنَ = Firondan (onun verdiyi əzabdan) | "Firondan (onun verdiyi əzabdan xilas etdik)" |
| 🆕 45:2, 46:2 | **MƏNA (ad)** — ərəbcədə الْعَزِيزِ الْحَكِيمِ, tərcümədə "Aziz və Alim (Bilən)"; eyni ayə 39:1-də düz verilib | "Aziz (İzzət Sahibi) və Hakim (Hikmət Sahibi) olan Allah" |
| 47:4 | **MƏNA** — "ya əsir olaraq yanınızda saxlayın, (və ya fidyə almadan), yaxud da fidyə … ilə buraxın" — فَإِمَّا مَنًّا بَعْدُ وَإِمَّا فِدَاءً: "ya (fidyəsiz) minnət edib buraxın, ya da fidyə ilə"; "əsir olaraq saxlayın" ərəbcədə yoxdur | "sonra (onları) ya (fidyəsiz) lütf edərək, ya da fidyə qarşılığında buraxın" |
| 🆕 48:25 | **MƏNA + BURAXILMA (orta)** — ikinci şərt uydurulub ("zərər toxundurmuş olmasaydınız"), فتصيبكم منهم معرة بغير علم isə düşüb | "Əgər orada tanımadığınız mömin kişilər və qadınlar — bilmədən onları tapdalayıb buna görə sizə bir günah toxunması (təhlükəsi) — olmasaydı…" |
| 49:2 | **MƏNA (orta) + üslub** — "Onu bir-birinizi çağırdığınız kimi çağırmayın" — ərəbcədə وَلَا تَجْهَرُوا لَهُ بِالْقَوْلِ كَجَهْرِ بَعْضِكُمْ لِبَعْضٍ: "bir-birinizlə uca səslə danışdığınız kimi onunla uca səslə danışmayın" (çağırma barədə olan 24:63-dür); böyük "Onu" Peyğəmbərə aiddir | "…və bir-birinizlə ucadan danışdığınız kimi onunla ucadan danışmayın!" |
| 55:48 | **MƏNA** — "(O iki Cənnət, meyvələri bol olan) iki budaq sahibidir" — ذَوَاتَا أَفْنَانٍ: "ikisi də (çoxlu) budaqlıdır", "iki budaq" deyil | "O iki Cənnət budaq-budaqdır (çoxlu budaqlı ağaclarla doludur)." — ذَوَاتَا ikilik formasıdır, mübtəda mötərizəyə girmir |
| 56:55 | **MƏNA (şəxs/zaman)** — "Onlar (onu) susuz qalmış dəvələrin içməyi ilə içirlər" — 56:51–54 kimi xitab ikinci şəxsdir və gələcəkdir (فَشَارِبُونَ) | "Siz (onu) susuz qalmış dəvələrin içdiyi kimi içəcəksiniz!" |
| 57:8 | **MƏNA (əvəzlik)** — "həqiqətən də o, sizdən əhd (söz) almışdır" — kiçik "o" Rəsulu göstərir; وَقَدْ أَخَذَ مِيثَاقَكُمْ — əhdi Allah alıb | "həqiqətən də O (Allah), sizdən əhd almışdır" |
| 🆕 69:10 | **MƏNA (izafət tərsinə)** — فعصوا رسول ربهم "rəsullarının Rəbbinə asi olmuşdular" | "…və Rəblərinin rəsuluna asi olmuşdular" |
| 80:17 | **MƏNA (artıq əlavə)** — "Necə də kafirdir o, onun kimi kafir olan yoxdur!" — مَا أَكْفَرَهُ yalnız "necə də nankordur!" deməkdir; "onun kimi kafir olan yoxdur" ərəbcədə yoxdur | "Qoy ölsün insan! Necə də nankordur (kafirdir) o!" |
| 🆕 81:20 | **BURAXILMA** — ذي قوة ("qüvvət sahibi") düşüb | "…qüvvət sahibi olan, Ərşin Sahibinin yanında uca məqamlı…" |
| 89:10 | **MƏNA (izah) + orfoqrafiya** — "Ehramlar (və yaxud … payalar basdıran) firona" — "sahibi" sözü yoxdur, cümlə qırıqdır; ذِي الْأَوْتَادِ = "dirəklər (payalar) sahibi" | "Dirəklər (payalar) sahibi Firona (da)" |
| 🆕 110:3 | **BURAXILMA** — فسبح بحمد ربك "Allahı təsbih et" — "Rəbbini həmd ilə" düşüb (15:98, 50:39-da düzdür) | "Rəbbini həmd ilə təsbih et və Ondan bağışlanma dilə" |

## 2. Sistemli xətalar (toplu düzəldilə bilər)

| Problem | Say | Qeyd |
|---|---|---|
| Yetim dipnot rəqəmləri (`tutmaz1`, `pis yoldaşdır!1`) | 254 ayə | Qeydlər (`note`) yalnız 14 ayədə var — qalanlarında rəqəm heç yerə aparmır. Siyahı Əlavə B-də |
| "deyilmiydi / deyilmiydik / deyilmiydin…" | 27 ayə | Ədəbi forma: "deyildimi / deyildikmi…" |
| "firon/fironun/firona" kiçik hərflə | 25 ayə | Xüsusi isim; başqa yerlərdə "Firon" |
| "munkər", "mutəkəbbir", "muşrik", "munəzzəh" | 9 + 5 + 2 + 3 ayə | "münkər", "mütəkəbbir", "müşrik", "münəzzəh" |
| "təkəbbürlülük" | 5 ayə | "təkəbbürlük" |
| Allahın sözündə "biz/mən/mənim" kiçik hərflə | ~20 ayə (aşağıda ayrı-ayrı) | Tərcümə Allaha aid əvəzlikləri böyük yazır — kiçik hərf oxucunu çaşdırır |
| Peyğəmbərə/insana aid "Onu/Ona/Onun" böyük hərflə | ~12 ayə | Böyük hərf Allahı göstərir |
| `) :`, `) ;`, `( ` kimi boşluqlar | 78 + 17 | Əlavə A |
| İşarədən sonra boşluq yoxdur (`vuruşun!Həqiqətən`) | 49 | Əlavə A |
| Ayə prefiksi qeyri-standart (`198:`, `-5:Günəş`, `-5, 6:`, `\u200b-4:`) | 39 | Əlavə A — paylaşma/vidcet kodu `^-?\d+[:.]` gözləyir, `-5, 6:` ona uyğun gəlmir |
| ASCII `"…"` dırnaqlar (17:1–59, 18:4–20, 20:88–113, 21:3–44, 23:47–118, 27:7–55 və s.) | bloklar | Mətnin qalanında `” … ”` — vahid simvol seçin |
| Səcdə işarəsi ۩ | 3 ayədə (53:62, 84:21, 96:19) | 15 səcdə ayəsindən 12-sində yoxdur |
| Axirət səhnəsində keçmiş zaman ("dedilər", "dedi") | ~12 ayə | Ərəbcədə keçmiş zaman qəti gələcək mənasındadır; qonşu ayələr "deyəcəklər" yazır — uyğunlaşdırın |
| 🆕 الطُّور "bir dağ / dağın" kimi | 7 ayə (2:63, 2:93, 4:154, 19:52, 20:80, 28:29, 28:46) | Xüsusi addır: "Tur dağı(nı/nın)". 52:1 "Tur", 23:20 və 95:2 "Sina" düzdür |
| 🆕 Allahın adlarının yazılışı | 3 + 2 + 4/4 + 13/3 + 4/1 + 2 | "Vahhəb" (3:8, 38:9, 38:35) → **Vəhhab**; "Ğaffər" (39:5, 40:42) → **Ğaffar** (20:82, 38:66, 71:10 düzdür); "Qaviydir"/"Qaviyydir", "Ğaniyydir"/"Ğaniydir", "Afuvdur"/"Afuvvdur" — bir forma seçin; "Vaduddur" (11:90, 85:14) → **Vədud**. "Xabir" 21 ayədə ardıcıldır, amma ədəbi forma "Xəbir"dir |
| 🆕 "(Xadir)" | 3 ayə (18:65, 18:71, 18:74) | الخضر — qəbul olunmuş forma "Xızır" |
| 🆕 ən'am heyvanları "(digər) heyvanlar" kimi | 2 ayə (5:1, 22:30) | بَهِيمَةُ الْأَنْعَامِ / الْأَنْعَامُ yalnız dəvə, mal-qara, qoyun-keçidir — hökm "bütün heyvanlar"a genişlənir |
| 🆕 Meytənin izahı "(İslami kəsimlə kəsilməmiş heyvanlar)" | 2 ayə (2:173, 16:115) | الْمَيْتَةَ kəsilmədən ölmüş heyvandır; izah hökmü genişləndirir. 16:115-də iki mötərizə yan-yanadır |

## 3. Bütün qeydlər (ayə-ayə)

Format: `ayə | növ | problem | təklif`. Növlər: MƏNA, STRUKTUR, MÖTƏRİZƏ, dırnaq, orfoqrafiya, qrammatika, durğu, boşluq, üslub, zaman.

### Hissə h01 (1:1–2:74)
- 2:11 | dırnaq | ”…fəsad törətməyin! -deyildikdə — bağlayan dırnaq yoxdur | ”…törətməyin!”-deyildikdə
- 2:27 | boşluq | "(bax,)onlar" | "(bax,) onlar"
- 2:30, 2:31 | üslub | Allahın sözündə "mən", "mənə" kiçik hərflə (başqa yerlərdə "Mən") | "Mən", "Mənə"
- 2:36 | MƏNA | "O, onları ondan yayındıraraq…" — ərəbcədə fail Şeytandır (فَأَزَلَّهُمَا الشَّيْطَانُ), böyük "O" isə Allahı bildirir | "Şeytan onları oradan sürüşdürərək (yayındıraraq)…"
- 2:61 | dırnaq | "…çıxarsın (versin)! “ O, dedi" — yanlış simvol və artıq boşluq | "…çıxarsın (versin)!” O, dedi"
- 2:68, 2:69, 2:71 | dırnaq | Musanın sözü içində Allahın sözü (iç dırnaq) açılır, bağlanmır | iç sitatı bağla: ”…bir inəkdir.” Sizə nə əmr edilirsə…”
### Hissə h02 (2:75–2:141)
- 2:75 | qrammatika | "bir dəstə Allahın kəlamını eşidir, … təhrif edirdilər" — zaman uyğunsuz (كَانَ … يَسْمَعُونَ) | "eşidir, … təhrif edirdi" və ya "eşidirdilər … təhrif edirdilər"
- 2:84 | orfoqrafiya | "təstiq etdiniz" | "təsdiq etdiniz"
- 2:96 | MƏNA (orta) | "onların və müşriklərin … ən həris kimsələr olduğunu" — ərəbcədə وَمِنَ الَّذِينَ أَشْرَكُوا = "hətta müşriklərdən də (daha həris)"; tərcümə iki qrupu bərabərləşdirir | "…onları insanların, hətta müşriklərdən də, (dünya) həyat(ın)a ən həris kimsələri olaraq görəcəksən"
- 2:99 | üslub | "Biz Sənə" — Peyğəmbərə müraciət böyük hərflə (tərcümədə böyük "Sən" Allaha aiddir) | "Biz sənə"
- 2:104 NOT | dırnaq/mötərizə | qeyddə “Raiinə” ( راعينا”) –(Bizim çoban” — dırnaq və mötərizə qarışıb | “Raiinə” (راعينا) – “Bizim çobanımız”
- 2:116 | orfoqrafiya | "munəzzəhdir" | "münəzzəhdir"
- 2:118 | orfoqrafiya | "deyilmiydi" (2 dəfə) | "deyildimi"
- 2:125 | durğu | "(haram bölgə,toxunulmaz)" və "(və belə dedik) : ”" | "(haram bölgə, toxunulmaz)", "(və belə dedik): ”"
- 2:136 | durğu | "nəvələrə (onların övladlarına) Musaya" — vergül yoxdur | "…övladlarına), Musaya"
- 2:140 | dırnaq | De: ”Sizmi … Allahmı daha yaxşı bilir? — açılan dırnaq bağlanmır | "…daha yaxşı bilir?”"
### Hissə h03 (2:142–2:202)
- 2:167 | üslub | ardıcılların sözündə "Biz də onlardan uzaqlaşardıq" — böyük "Biz" Allaha aiddir | "biz də"
- 2:189 | dırnaq | de: ”Onlar insanların vaxtları… — dırnaq bağlanmır | "…Həcci bilmələri üçündür.”"
- 2:196 | orfoqrafiya | "Həcdə" | "Həccdə"
- 2:198 | orfoqrafiya | "Şübhədiz ki" | "Şübhəsiz ki"
### Hissə h04 (2:203–2:252)
- 2:214 | MƏNA (buraxılma) | "hətta (sonunda) rəsul (belə) demişdi" — ərəbcədə الرَّسُولُ وَالَّذِينَ آمَنُوا مَعَهُ: "rəsul və onunla birlikdə iman gətirənlər" | "…hətta rəsul və onunla birlikdə iman gətirənlər (belə) demişdilər: …"
- 2:215, 2:219, 2:220, 2:222 | dırnaq | "de: ”…" açılır, bağlanmır (2:219-da birinci sitat bağlanmadan ikincisi açılır) | sitatın sonuna ” qoy
- 2:220 | durğu | ayə "..dünya" ilə başlayır (iki nöqtə) | "...dünya"
- 2:226 | orfoqrafiya | "gözləmələdirlər" | "gözləməlidirlər"
- 2:237 | orfoqrafiya | "bir Mehr təyin etdiyiniz" — cümlə ortasında böyük hərf | "bir mehr"
- 2:243 | orfoqrafiya | "çox mərhəmətidir" | "çox mərhəmətlidir"
### Hissə h05 (2:253–3:14)
- 2:275 | orfoqrafiya | sitat kiçik hərflə başlayır: ”satış (ticarət…) | ”Satış…
- 2:285 | MƏNA (orta) | "Biz Onun rəsullarından heç birinin arasında fərq qoymuruq" — bu möminlərin sözüdür (ərəbcədə gizli "deyirlər"), böyük "Biz" isə Allahın danışdığını göstərir | "(Onlar deyirlər:) “Biz Onun rəsullarından… fərq qoymuruq.”" — "biz" kiçik hərflə
- 2:286 | dırnaq | ayə ”-lə bitir, açılan dırnaq yoxdur | ya dua hissəsini ”Rəbbimiz!…-dən açmaq, ya sonuncu ”-ni silmək
- 3:7 | dırnaq | ”Biz onlara iman gətirdik, hamısı Rəbbimizdəndir! — bağlanmır | "…Rəbbimizdəndir!”"
### Hissə h06 (3:15–3:92)
- 3:29, 3:31 | dırnaq | De: ”… açılır, bağlanmır | sonuna ”
- 3:47 | dırnaq | iç sitat ”Ol!” bağlanır, xarici sitat ("Beləcə, Allah…") bağlanmır | "…o da olur!”"
- 3:55 | orfoqrafiya | "kafirlərlərin" | "kafirlərin"
- 3:55, 3:81 | üslub | Allahın sözündə "mən", "o, demişdi", "mənim əhdimi" kiçik hərflə (eyni ayədə "Mən", "Özümə" böyük) | "Mən", "O", "Mənim"
### Hissə h07 (3:93–3:170)
- 3:110, 3:114 | orfoqrafiya | "munkəri" (3:104-də "münkəri") | "münkəri"
- 3:144 | orfoqrafiya | "dininzdən" | "dininizdən"
- 3:167 | qrammatika | "imandan çox küfrə daha yaxın idilər" — "çox" və "daha" təkrardır | "imandan çox küfrə yaxın idilər"
### Hissə h08 (3:171–4:23)
- 3:193 | orfoqrafiya | "munadini" | "münadini"
- 3:195 | dırnaq | ”Mən (istər) kişi… — Allahın sözü açılır, bağlanmır | "…savabın ən gözəl olanı!”"
- 4:7 | təkrar söz | "bir nəsib, bir pay var vardır" | "bir nəsib, bir pay vardır"
- 4:20 | orfoqrafiya | "mehir" (başqa yerlərdə "mehr") | "mehr"
### Hissə h09 (4:24–4:87)
- 4:25 | orfoqrafiya | "kim anətdən … qorxsa" (ərəbcə الْعَنَتَ) | "ənətdən"
- 4:38 | dipnot | ayə "…pis yoldaşdır!1" ilə bitir — sonda yetim dipnot rəqəmi | "…pis yoldaşdır!"
- 4:47 | orfoqrafiya | "və s. Kimi üzvləri" | "və s. kimi üzvləri"
- 4:54 | üslub | İbrahimə aid "Ona iman edən" böyük hərflə (böyük "O" Allaha aiddir) | "ona iman edən"
- 4:67 | qrammatika | "Biz onlara … böyük bir mükafat verər..." — 4:68 "yönəldərdik" ilə uyğun deyil | "…mükafat verərdik..."
- 4:77 | orfoqrafiya | "xurma çərdəyinin lifi" | "xurma çəyirdəyinin lifi"
- 4:82 | orfoqrafiya | "ziddiyətlər" | "ziddiyyətlər"
- 4:85 | mötərizə | "Muqitdir4 (Nəzarət Edəndir))!" — artıq ")" və yetim "4" | "Muqitdir (Nəzarət Edəndir)!"
### Hissə h10 (4:88–4:147)
- 4:104 | orfoqrafiya | "Əgər sizlər ağrıyırsınıza" | "ağrıyırsınızsa"
- 4:127 | orfoqrafiya + dırnaq | "hökümlər"; de: ”Onlar haqqında… — dırnaq bağlanmır | "hökmlər"; "…Biləndir.”"
- 4:128 | MÖTƏRİZƏ (ciddi) | "nəfslər simicliyə2, xəsisliyə) bağlıdır (,meyillidir. Odur ki, ey kişilər!)" — mötərizələr sürüşüb, mətn pozulub; "rəftardan1" yetim rəqəm | "(Bilin ki,) nəfslər simicliyə (xəsisliyə) bağlıdır (meyillidir). (Odur ki, ey kişilər!) Əgər…"
- 4:129 | orfoqrafiya | "tamamilə meyil etməyin" | "meyl etməyin"
- 4:135 | orfoqrafiya | "zəngin vəya fəqir" | "zəngin və ya fəqir"
- 4:141 | orfoqrafiya | "deyilmiydik" | "deyildikmi"
- 4:146 | orfoqrafiya | "dinlərini xalis olaraq Allah aid edən" | "Allaha aid edən"
- 4:147 | orfoqrafiya | "etdiklərininin" | "etdiklərinin"
### Hissə h11 (4:148–5:26)
- 4:154 | dırnaq | "Biz onlara: ’Şənbə günündə həddi aşmayın!”" — açılan dırnaq apostrofdur | ”Şənbə günündə…”
- 4:171 | dırnaq + orfoqrafiya | "və: (“Allah”) üçdür!” deməyin!"; "Munəzzəhdir" | "və: ”(Allah) üçdür!”-deməyin!"; "Münəzzəhdir"
- 4:176, 5:4, 5:18 | dırnaq | de: ”… açılır, bağlanmır | sitatın sonuna ”
- 5:24 | durğu | "oturmuşuq. ”" — dırnaqdan əvvəl artıq boşluq | "oturmuşuq.”"
### Hissə h12 (5:27–5:81)
- 5:49 | üslub | "Allahın Sənə nazil etdiyinin" — Peyğəmbərə böyük "Sənə"; "fitnəyə salmasından" — kimi salacaqları yoxdur | "sənə"; "səni fitnəyə salmalarından"
- 5:54 | MƏNA (orta) | "Onlar möminlərə qarşı zillət içində olarlar" — أَذِلَّةٍ عَلَى الْمُؤْمِنِينَ burada "mülayim, təvazökar" deməkdir; azərbaycanca "zillət" alçalma/xarlıq anlamı verir | "Onlar möminlərə qarşı mülayim (təvazökar), kafirlərə qarşı isə sərt (izzətli) olarlar"
- 5:59 | dırnaq | De: ”Ey kitab əhli!… — bağlanmır | "…intiqam alırsınız?!”"
- 5:63 | orfoqrafiya | "deyilmiydilər" | "deyildilərmi"
- 5:72 | dırnaq + boşluq | "Ey Bəni İsrail!Mənim" və Məsihin sözü bağlanmır | "Ey Bəni İsrail! Mənim…"; sonuna ”
- 5:73 | AYDINLIQ | "Allah üçün üçüncüsüdür" — "üçün" burada "üçünün (üçdən birinin)" mənasındadır, amma oxucu onu "üçün" qoşması kimi oxuyur (ثَالِثُ ثَلَاثَةٍ) | "Allah üçdən biridir (üç ilahın üçüncüsüdür)" — mötərizəsiz də düz oxunur: "Allah üçdən biridir"
- 5:81 | üslub | "Nəbiyə və Ona nazil edilənə" — Peyğəmbərə böyük "Ona" | "ona"
### Hissə h13 (5:82–6:35)
- 5:95 | orfoqrafiya | "Muhrimlər ikən (Ehramda olarkən)" — "ehram" piramida deməkdir | "(İhramda olarkən)"
- 5:100 | dırnaq | De: ”Xəbis… — bağlanmır | "…qurtulasınız, xilas olasınız)!”"
- 5:119 | durğu | "…Cənnətlər vardır!”; Allah onlardan razıdır" — dırnaqdan sonra artıq ";" | "…vardır!” Allah onlardan…"
- 6:8 | orfoqrafiya | "deyilmiydi" | "deyildimi"
- 6:23 | orfoqrafiya | "muşriklər" | "müşriklər"
### Hissə h14 (6:36–6:110)
- 6:37 | dırnaq | "De: Şübhəsiz ki, … Qadirdir!” Lakin … bilmirlər!”" — açan dırnaq yoxdur, sonda artıq bağlayan var | "De: ”Şübhəsiz ki, Allah bir ayət nazil etməyə Qadirdir!” Lakin onların əksəriyyəti bilmirlər!"
- 6:50 | qrammatika | "Heçmi təfəkkür etməzmisiniz?" — sual ədatı iki dəfə | "Heçmi təfəkkür etməzsiniz?"
- 6:65 | dırnaq | De: ”O (Allah)… — bağlanmır | "…qadirdir!”"
- 6:70 | orfoqrafiya | "əzabsan qurtulmaq üçün" | "əzabdan qurtulmaq üçün"
- 6:83 | üslub | Allahın sözündə "bizim hüccət(lər)imizdir" kiçik hərflə | "Bizim"
- 6:93 | dırnaq/hərf | ”mənə vəhy olundu” kiçik hərflə; "(belə deyirlər) : ”" | ”Mənə vəhy olundu”; "(belə deyirlər): ”"
- 6:96 | orfoqrafiya | "(Gecənin zülmətlərindən) Dan yerini" — cümlə ortasında böyük hərf | "dan yerini"
- 6:104 | durğu | "dəlillər1, gəlmişdir" — yetim rəqəm və artıq vergül | "dəlillər gəlmişdir"
### Hissə h15 (6:111–6:165)
- 6:128 | dırnaq | müəllifin sözü "Şübhəsiz ki, sənin Rəbbin Hakimdir…" Allahın sitatının içində qalıb | "…orada daima qalacaqsınız.” Şübhəsiz ki, sənin Rəbbin…"
- 6:146 | qrammatika | "onlara, bellərinə (bel sümüklərinə)… yapışanlar" | "onların bellərinə…"
- 6:147 | dırnaq | de: “Rəbbiniz… — qarışıq simvol | de: ”Rəbbiniz…
- 6:150 | orfoqrafiya | "şahidərinizi" | "şahidlərinizi"
- 6:164 | dırnaq | "…xəbər verəcəkdir!”" — sonda açılmamış artıq dırnaq | sonuncu ”-ni sil
- 6:165 | MƏNA (kiçik) | "Sizə verdikləri şeylərdə" (cəm — "onların verdikləri") — fail Allahdır; "Rahimdir (çox əfv Edəndir)" — Rahim "rəhm edən"dir, "əfv edən" Afuvdur | "Sizə verdiyi şeylərdə"; "Rahimdir (çox rəhm Edəndir)"
### Hissə h16 (7:1–7:87)
- 7:29 | dırnaq | De: ”Rəbbim… — bağlanmır | "…dönəcəksiniz!”"
- 7:43 | MƏNA (ciddi izah) + qrammatika + dırnaq | Cənnət əhlinə: "Əməlləriniz (günahlarınız) səbəbi ilə varis olduğunuz Cənnət budur" — izah tərsdir, بِمَا كُنتُمْ تَعْمَلُونَ saleh əməllərdir; "Həmd olsun o Allaha bizi buna yönəltdi" — "ki" yoxdur; son sitat bağlanmır | "Əməlləriniz (saleh əməlləriniz) səbəbi ilə…budur!”"; "o Allaha ki, bizi buna yönəltdi"
- 7:44 | durğu + orfoqrafiya | "”Bəli”-, deyəcəklər"; "muəzzin" | "”Bəli!”-deyəcəklər"; "müəzzin"
- 7:49 | dırnaq | sitat ”Allah onlara…”-(deyərək…) ilə başlayır, xarici sitatın açılışı yoxdur; "olun!Sizə" | Ə'raf əhlinin sözünü bütöv dırnağa al; "olun! Sizə"
- 7:53 | orfoqrafiya | "gərçəkləşəcəyi" | "gerçəkləşəcəyi"
- 7:74 | dırnaq | Salehin sözü 7:73-də bağlanıb, 7:74 açılmadan ” ilə bitir | 7:73-ün sonundakı ”-ni sil (sitat 7:74-də bitir)
- 7:75 | üslub | Salehə aid "Onun göndərildiyi" böyük hərflə | "onun"
### Hissə h17 (7:88–7:170)
- 7:115, 7:122 | durğu | sitat sonunda nöqtə/nida yoxdur: "…biz olaq”", "…Rəbbinə”" | "…biz olaq!”", "…Rəbbinə!”"
- 7:117 | durğu | "vəhy etdik”Əsanı (yerə) at!”" — iki nöqtə və boşluq yoxdur | "vəhy etdik: ”Əsanı (yerə) at!”"
- 7:143 | üslub | Allahın sözündə "əsla məni görməyəcəksən" kiçik, ardınca "Məni" böyük | "Məni"
- 7:157, 7:158 | üslub | Peyğəmbərə aid "Ona iman gətirən, Onun qədrini bilən, Ona yardım edən", "Ona ittiba edin (Onun ardınca…)" böyük hərflə — Allahla qarışır | "ona", "onun"
### Hissə h18 (7:171–8:40)
- 7:176 | üslub | Allahın sözündə "Əgər biz istəsəydik" kiçik hərflə | "Əgər Biz istəsəydik"
- 7:187, 7:188 | dırnaq | De: ”… bağlanmadan yeni "De: ”" açılır; 7:188 bağlanmır | birinci sitatı "…qəfildən gələcəkdir!”" ilə bağla; 7:188 sonuna ”
- 7:193 | qrammatika | "Əgər sən onları … də’vət etsən, sizə ittiba etməzlər" — şəxs uyğunsuz (ərəbcədə cəmdir) | "Əgər siz onları … də’vət etsəniz, sizə ittiba etməzlər"
- 7:195 | durğu | "Yoxsa onların əllərimi var, onunla tutsunlar!" — sual cümləsi | "…tutsunlar?"
- 8:1 | dırnaq | "…Rəsuluna itaət edin!”" — sonda açılmamış artıq dırnaq | sonuncu ”-ni sil
- 8:11 | qrammatika | "səmadan su (yağış) nazil endirirdi" — "nazil" və "endirirdi" təkrardır | "nazil edirdi"
### Hissə h19 (8:41–9:33)
- 8:52 | üslub | "fironun tərəfdarları" kiçik hərflə (8:54-də "Fironun") — xüsusi isim bütün mətndə qarışıqdır | "Fironun"
- 8:56 | qrammatika | "O(onlar) kimsələr(dir) ki" | "Onlar o kimsələrdir ki"
- 8:72 | orfoqrafiya | "onların valəyətindən" | "vilayətindən (vəlayətindən)"
- 8:73 | MÖTƏRİZƏ | "Kafirlər(ə gəldikdə isə, onlar bir-birinin vəlisidirlər" — açılan mötərizə bağlanmır | "Kafirlərə gəldikdə isə, onlar bir-birinin vəlisidirlər."
- 9:24 | dırnaq | De: ”Əgər atalarınız… — bağlanmır | "…hidayət etmir!”"
- 9:31 | orfoqrafiya | "munəzzəhdir" | "münəzzəhdir"
- 9:32 | üslub | "Allah öz nurunu" (başqa yerlərdə "Öz") | "Öz nurunu"
### Hissə h20 (9:34–9:92)
- 9:50 | qrammatika | "bu onları pisinə gələr" | "bu onların pisinə gələr"
- 9:51 | üslub | möminlərin sözündə "əsla Bizə isabət etməz" böyük hərflə — Allah sözü kimi oxunur | "bizə"
- 9:66 | boşluq | "küfr etdiniz!Əgər" | "küfr etdiniz! Əgər"
- 9:71 | orfoqrafiya | "munkəri" | "münkəri"
- 9:86 | qrammatika | "Allaha iman edin Onun Rəsulu ilə birlikdə cihad edin" — "və" yoxdur | "Allaha iman edin və Onun Rəsulu ilə…"
### Hissə h21 (9:93–10:25)
- 9:93 | qrammatika | "(artıq haqqı, eləcə özlərini…)" | "eləcə də özlərini"
- 9:99 | qrammatika | "Həqiqətən də o əməlləri onlar üçün … əməllərdir" | "o əməllər onlar üçün…"
- 9:124 | qrammatika | "onlardan kimisi: ”Bu, sizin hansınızın imanını artırdı?”" — fel yoxdur | "…artırdı?”-deyir."
- 10:17 | boşluq | "kim ola bilər?Həqiqətən" | "kim ola bilər? Həqiqətən"
- 10:20 | orfoqrafiya | "deyilmiydi" | "deyildimi"
### Hissə h22 (10:26–11:5)
- 10:31 | dırnaq | De: ”Səmadan… kimdir? — bağlanmadan "Onlar: ”Allahdır”" gəlir | "…İşləri idarə edən kimdir?”"
- 10:61 | dırnaq | ayə ” ilə bitir, açılan dırnaq yoxdur | sonuncu ”-ni sil
- 10:64 | qrammatika | "Bu, böyük (əzəmətli) uğur məhz budur!" — "Bu" və "budur" təkrar | "Böyük (əzəmətli) uğur məhz budur!"
- 10:65 | boşluq | "məxsusdur!O, Səmi’dir" | "məxsusdur! O, Səmi’dir"
- 10:89 | orfoqrafiya | "doğru yalda sabit olun" | "doğru yolda"
- 10:104 | dırnaq | De: ”Ey insanlar!… — bağlanmır (10:105–106 sitatın davamıdır) | 10:106-nın sonunda ” qoy
- 10:106 | durğu | "nə bir fayda , nə də" | "nə bir fayda, nə də"
### Hissə h23 (11:6–11:83)
- 11:12 | orfoqrafiya | "deyilmiydi" | "deyildimi"
- 11:25 | dırnaq/mötərizə | "(O, dedi:”)Həqiqətən də" — dırnaq mötərizənin içində, boşluq yoxdur | "(O, dedi:) ”Həqiqətən də"
- 11:36 | dırnaq | Allahın Nuha sözü açılmır ("vəhy olundu ki; Sənin…"), 11:37 isə ” ilə bitir | "vəhy olundu ki: ”Sənin qövmündən…"
- 11:40 | MƏNA | "Ona hər kəsdən iki cütü … mindir" — "hər kəsdən" insan deməkdir; مِن كُلٍّ زَوْجَيْنِ اثْنَيْنِ = "hər birindən (hər canlı növündən) bir cüt (erkək və dişi)" | "Ona (gəmiyə) hər birindən (hər canlı növündən) bir cüt (erkək və dişi), … öz əhlini və iman edənləri mindir" — mötərizəsiz də düz oxunur: "hər birindən bir cüt"
- 11:53 | qrammatika | "beyyinət (heç bir açıq-aydın bir dəlil)" | "(heç bir açıq-aydın dəlil)"
- 11:60 | simvol | ayə "…Ad (qövmü)!}" ilə bitir — artıq "}" | "…Ad (qövmü)!"
- 11:68 | boşluq | "Diqqət edin!Həqiqətən" | "Diqqət edin! Həqiqətən"
- 11:71 | üslub | Allahın sözündə "biz onu İshaq ilə… müjdələdik" kiçik hərflə | "Biz onu…"
- 11:73 | orfoqrafiya | "Övülən" | "Öyülən"
- 11:76 | orfoqrafiya | "(Mələkər dedilər) :" | "(Mələklər dedilər):"
### Hissə h24 (11:84–12:52)
- 11:87 | dırnaq | "…ağıl sahibi birisən3’’" — iki apostrof + yetim rəqəm | "…ağıl sahibi birisən”"
- 11:103 | orfoqrafiya | "işarətər" | "işarətlər"
- 11:116 | orfoqrafiya | "deyilmiydi" | "deyildimi"
- 12:28/12:29 | dırnaq | Azizin sözü 12:28-in sonunda bağlanır, 12:29 ("Yusuf! Bundan üz çevir…") açılmadan ” ilə bitir | 12:28-in sonundakı ”-ni sil
- 12:41 | dırnaq | Yusufun 12:37-də açılan sözü bağlanmır | "…artıq bitmişdir.”"
- 12:44 | orfoqrafiya | "qarşıq yuxulardır" | "qarışıq yuxulardır"
### Hissə h25 (12:53–13:18)
- 12:76 | qrammatika | "qardaşının qabların(ın arasın)dan çıxardı" | "qardaşının qabından çıxardı"
- 12:79 | durğu | "…zalım kimsələr olarıq”!" | "…olarıq!”"
- 12:86/12:87 | dırnaq | Yaqubun sözü 12:86-da bağlanır, 12:87 ("Ey oğullarım!…") açılmadan ” ilə bitir | 12:86-nın sonundakı ”-ni sil
- 12:90 | dırnaq | O, dedi: ”(Bəli,) mən Yusufam… — bağlanmır | "…zay etməyəcəkdir!”"
- 12:110 | qrammatika | "Biz istədiyimiz kimsələr xilas edildi" | "istədiyimiz kimsələr xilas edildi" / "Biz istədiyimiz kimsələri xilas etdik"
- 13:5 | boşluq | "küfr edənlər!Onlardır" | "küfr edənlər! Onlardır"
- 13:7 | orfoqrafiya | "deyilmiydi" | "deyildimi"
- 13:9 | orfoqrafiya | "Mutəaldır" | "Mütəaldır"
- 13:16 | dırnaq | "…bu yaradılış da onlara oxşar göründü?”" — açılmamış artıq dırnaq; son sitat "…qəhr edəndir)”" nidasız | "…oxşar göründü?"; "…qəhr edəndir)!”"
### Hissə h26 (13:19–14:52)
- 13:33 | durğu | "danışırsınız ?" | "danışırsınız?"
- 13:35 | boşluq | "sonu (aqibəti)!Kafirlərin" | "sonu (aqibəti)! Kafirlərin"
- 13:37 | üslub | Allahın sözündə "Beləliklə biz onu…nazil etdik" kiçik hərflə | "Biz"
- 13:38 | orfoqrafiya | "rəsulllar" (üç l) | "rəsullar"
- 14:6–14:7 | dırnaq | Musanın 14:6-da açılan sözü heç bağlanmır (14:8 yeni sitatla başlayır) | 14:7-nin sonunda ”
- 14:11 | qrammatika | "Yalnız Allahın izni olmadan biz sizə heç bir sultan … ilə gələ bilməzdik" — "Yalnız" artıqdır, zaman uyğunsuz | "Allahın izni olmadan biz sizə heç bir sultan (dəlil) gətirə bilmərik"
- 14:26 | üslub | ağaca aid "O, torpağın üstündən qoparılmışdır" böyük "O" (Allaha aid hərf) | "o, torpağın…"
- 14:34 | durğu | ayə nöqtəsiz bitir: "…çox küfr edəndir" | "…çox küfr edəndir!"
### Hissə h27 (15:1–16:50)
- 15:7 | orfoqrafiya | "deyilmiydin" | "deyildinmi"
- 15:12 | üslub | Allahın sözündə "Beləcə biz onu…" kiçik hərflə | "Biz"
- 15:35 | durğu | "…lənət vardır!”." — dırnaqdan sonra artıq nöqtə | "…lənət vardır!”"
- 15:40 | dırnaq | "…qulların istisnadır!’" — bağlayan simvol apostrofdur | "…istisnadır!”"
- 16:29 | orfoqrafiya | "qalacağıız"; "mutəkəbbirlərin" | "qalacağınız"; "mütəkəbbirlərin"
- 16:39 | qrammatika | "ixtilaf etməkdə olduğu şeyləri" | "ixtilaf etməkdə olduqları şeyləri"
### Hissə h28 (16:51–16:128)
- 16:53 | durğu | "diləyirsiniz ." | "diləyirsiniz."
- 16:59 | durğu/hərf | "(və belə düşünər) : ”xəcalət içində…" | "(və belə düşünər): ”Xəcalət içində…"
- 16:60 | boşluq | "məxsusdur!O, Azizdir" | "məxsusdur! O, Azizdir"
- 16:86 | orfoqrafiya | "muşriklər" (2 dəfə) | "müşriklər"
- 16:90 | orfoqrafiya | "munkər" | "münkər"
- 16:125 | orfoqrafiya | "Öz yolundan sapanları də" | "sapanları da"
### Hissə h29 (17:1–17:98)
- 17:1–17:59 | dırnaq üslubu | bu blokda sitatlar düz ASCII "…" ilə, apostrof isə ' ilə yazılıb (Səmi', mə'bud) — mətnin qalanında ” … ” və ’ işlədilir | vahid üsluba gətir
- 17:5 | orfoqrafiya | "birinicisinin vaxtı" | "birincisinin vaxtı"
- 17:28 | üslub | qohumlara aid "Onlardan üz çevirsən" böyük hərflə | "onlardan"
- 17:33 | üslub | Allahın sözündə "artıq biz onun vəlisinə… vermişik" kiçik hərflə | "Biz"
- 17:42, 17:56, 17:88 | dırnaq | De: "…/De: ”… açılır, bağlanmır | sitat sonuna dırnaq
- 17:47 | hərf | sitat kiçik hərflə: "siz ancaq sehrlənmiş…" | "Siz ancaq…"
### Hissə h30 (17:99–18:74)
- 17:102 | durğu | "…zənn edirəm (görürəm)!”." | "…(görürəm)!”"
- 17:110 | dırnaq | "…Bunların arasında (orta) bir yol tut!”" — sitat əvvəlki cümlədə bağlanıb, sonda artıq dırnaq | sonuncu ”-ni sil
- 18:1 | orfoqrafiya | "ziddiyətə" | "ziddiyyətə"
- 18:4–18:20, 18:60 | dırnaq üslubu | ASCII "…" (qalan mətndə ” … ”) | vahid üsluba gətir
- 18:5 | qrammatika | "Onların nə özünün, nə də atalarının" | "nə özlərinin"
- 18:15/18:16 | dırnaq | gənclərin sözü 18:15-də bağlanır, 18:16 ("Madam ki…") açılmadan " ilə bitir | 18:15-in sonundakı dırnağı sil
- 18:15, 18:39 | orfoqrafiya | "deyilmiydilər", "deyilmiydin" | "deyildilərmi", "deyildinmi"
- 18:24 | boşluq | "…edəcəyəm de)!(Bunu deməyi)" | "…edəcəyəm de)! (Bunu deməyi)"
- 18:26, 18:29 | dırnaq | De: ”… bağlanmır | sitat sonuna ”
- 18:49 | boşluq | "olmasın?!”Onlar" | "olmasın?!” Onlar"
- 18:50 | üslub | Allahın sözündə "Bir zaman biz mələklərə…" kiçik hərflə | "Biz"
- 18:56 | boşluq | "istehza etdilər(və onları…)" | "istehza etdilər (və onları…)"
- 18:71, 18:74 | orfoqrafiya | "munkər" | "münkər"
### Hissə h31 (18:75–19:98)
- 18:86 | qrammatika | "…gəlib çatdı onu (Günəşi) … bulaqda batdığını gördü" — vergül yoxdur, "onu…batdığını" | "…gəlib çatdı, onun (Günəşin) … bulaqda batdığını gördü"
- 18:88 | üslub | Zülqərneynin sözündə "Öz işimizdən" böyük hərflə | "öz işimizdən"
- 18:103 | dırnaq | De: ”Əməl yönündən… — bağlanmır | sitatın sonuna ”
- 18:106 | qrammatika | "ayətlərimi və rəsullarıma istehza etdiklərinə" | "ayətlərimə və rəsullarıma"
- 18:110 | durğu | "…şərik qoşmasın”" — sonda nida/nöqtə yoxdur | "…şərik qoşmasın!”"
- 19:5 | STRUKTUR (ciddi) | 19:5-in mətni yalnız "!" işarəsidir; 5-ci ayənin mənası 19:6-ya qatılıb, 19:6 da "-5, 6:" prefiksi ilə saxlanıb | 19:5-ə öz tərcüməsi: "Həqiqətən də mən arxamca gələn mövlalarımdan … qorxuram, arvadım da sonsuzdur, (buna görə də) Öz qatından mənə bir vəli bəxş et!"; 19:6: "O (vəli) həm mənə, həm də Yaqub ailəsinə varis olsun! Rəbbim! Onu razı olunan kimsə et!"
- 19:7 | dırnaq | Allahın sözü "Ey Zəkəriyya!…" açılmadan " ilə bitir | 19:7-ni "(Allah buyurdu:) "Ey Zəkəriyya!…" kimi aç
- 19:24–19:26 | dırnaq | səslənən sözü 19:25-də bağlanır, 19:26 ("ye, iç…") sözün davamıdır | 19:25-in sonundakı dırnağı 19:26-nın "…de:"-dən əvvəlinə köçür
- 19:41, 19:42, 19:44, 19:46, 19:47 | boşluq | "et!Həqiqətən", "Atacan!Eşitməyən", "etmə!(Çünki,)", "edirsən?Şübhəsiz", "olsun!Rəbbimdən" | hər birində işarədən sonra boşluq
- 19:58 | orfoqrafiya | "daşıdıdıqlarımızdan" | "daşıdıqlarımızdan"
- 19:75 | dırnaq | De: ”Kim sapıqlıq… — bağlanmır | "…ən zəif olan!”"
- 19:77 | dırnaq | "…kimsəni gördünmü?”" — açılmamış artıq dırnaq | sonuncu ”-ni sil
- 19:85, 19:87, 19:88, 19:91, 19:96 | üslub | cümlə ortasında "Ər-Rahmən" (başqa yerlərdə "ər-Rahmən") | "ər-Rahmən"
### Hissə h32 (20:1–20:135)
- 20:33/20:34 | STRUKTUR | 20:33 ("Səni çox təsbih edək…") 20:34-ün əvvəlində təkrarlanır; 20:34 = وَنَذْكُرَكَ كَثِيرًا, yalnız "və Səni çox zikr edək" | 20:34: "…və Səni çox zikr edək!"
- 20:57/20:58 | dırnaq | Fironun sözü 20:57-də bağlanır, 20:58 ("Əsla şübhə yoxdur ki, biz sənə…") açılmadan ” ilə bitir | 20:57-nin sonundakı ”-ni sil
- 20:85, 20:87, 20:95 | orfoqrafiya | xüsusi isim kiçik hərflə: "samiri" | "Samiri"
- 20:88–20:113 | dırnaq üslubu | ASCII "…" | vahid üsluba gətir
- 20:97 | dırnaq/mötərizə | '"Misəs yoxdur (mənə toxunmayın, mən də sizə toxunmuram")' — dırnaq mötərizənin içində bağlanır | '"Misəs yoxdur" (mənə toxunmayın, mən də sizə toxunmuram)'
- 20:103 | hərf | sitat kiçik hərflə: "siz (dünyada…)" | "Siz…"
- 20:111 | orfoqrafiya | "hüzürunda", "hüzüruna" | "hüzurunda", "hüzuruna"
- 20:123 | üslub + dırnaq | Allahın sözündə "kim mənim hidayətimə" kiçik hərflə; sitat 20:124-də bağlanmır | "Mənim"; 20:124 sonuna ”
- 20:130 | mötərizə | "təsbih et (namaz) qıl" | "təsbih et (namaz qıl)"
- 20:133 | orfoqrafiya | "deyilmiydi" | "deyildimi"
### Hissə h33 (21:1–21:112)
- 21:3–21:44 | dırnaq üslubu | ASCII "…" və ' (Səmi'dir) | vahid üsluba gətir
- 21:5 | orfoqrafiya | "qarşıq yuxulardır" | "qarışıq yuxulardır"
- 21:9 | üslub | Allahın sözündə "Sonra biz onlara olan vədə sadiq qaldıq" kiçik hərflə | "Biz"
- 21:13 | qrammatika | "…qayıdın ki, bəlkə … sizdən (bir şeylər) istədilər" — keçmiş zaman yanlışdır (لَعَلَّكُمْ تُسْأَلُونَ) | "…ki, bəlkə sorğu-sual olunasınız" / "…istəyələr"
- 21:86 | üslub | "Biz onları öz rəhmətimizə" | "Öz rəhmətimizə"
- 21:89 | durğu | "Zəkəriyyanı da (zikr et), ki," — artıq vergül; "ən xeyirlisisən1!" | "(zikr et) ki,"; "ən xeyirlisisən!"
### Hissə h34 (22:1–22:78)
- 22:1 | orfoqrafiya | "olduqca) əzəməti bir şeydir" | "əzəmətli bir şeydir"
- 22:22 | boşluq | '(onlara belə deyilir: )"Dadın' | '(onlara belə deyilir:) "Dadın'
- 22:41 | orfoqrafiya | "munkəri" | "münkəri"
- 22:47 | orfoqrafiya | "sizin saydığnız illərdən" | "saydığınız"
- 22:72 | boşluq | "xəbər verimmi?O, Allahın" | "xəbər verimmi? O, Allahın"
- 22:76 | qrammatika | "önlərində olanları da bilir, arxasında olanları da" | "arxalarında olanları da"
### Hissə h35 (23:1–24:20)
- 23:27 | MƏNA | "ona (gəmiyə) hər kəsdən iki cütü … daxil et" — 11:40-dakı eyni xəta; مِن كُلٍّ زَوْجَيْنِ = "hər birindən bir cüt" | "ona (gəmiyə) hər birindən (hər canlı növündən) bir cüt (erkək və dişi) … daxil et (mindir)"
- 23:32 | dırnaq | "…göndərdik ki, Allaha ibadət edin, … Heçmi qorxmazsınız?”" — açılan dırnaq yoxdur | "…göndərdik (ki, desin): ”Allaha ibadət edin…"
- 23:40 | dırnaq | "O (Allah), dedi: Əsla, …" — açılan dırnaq yoxdur | "O (Allah), dedi: ”Əsla, …"
- 23:81 | boşluq | "Xeyr!Onlar" | "Xeyr! Onlar"
- 23:88 | durğu | "(deyin; ) hər bir şeyin" | "(deyin): hər bir şeyin"
- 23:89 | MÖTƏRİZƏ | '"(O zaman necə olur ki, (batilə yönələrək…) sehrlənirsiniz' — birinci mötərizə bağlanmır | '"O zaman necə olur ki, (batilə yönələrək…) sehrlənirsiniz' — fa (فَأَنَّىٰ) ərəbcədədir, mötərizəyə girmir
- 23:108, 23:111 | üslub | Allahın sözündə "mənimlə danışmayın", "bu gün mən onları…" kiçik hərflə | "Mənimlə", "Mən"
- 23:114 | hərf | "…az bir müddət qaldınız. kaş ki" | "…qaldınız. Kaş ki"
- 24:12, 24:16 | orfoqrafiya | "deyilmiydilər", "deyilmiydiniz" | "deyildilərmi", "deyildinizmi"
- 23:47–23:118 (qarışıq) | dırnaq üslubu | ASCII "…" ilə ” … ” qarışıq | vahid üsluba gətir
### Hissə h36 (24:21–25:20)
- 24:33 | orfoqrafiya | "öz iffətinini" | "öz iffətini"
- 24:55 | qrammatika + boşluq | "onlar üçün razı olduğu dinlərini onlar üçün də təmkin sahibi … edəcək"; "Onlar(ın halı belədir: ) Mənə" | "onlar üçün razı olduğu dinlərini onlar üçün möhkəmləndirəcək"; "Onlar(ın halı belədir:) Mənə"
- 25:7, 25:8 | orfoqrafiya | "deyilmiydi" (2 dəfə) | "deyildimi"
### Hissə h37 (25:21–26:110)
- 25:21 | orfoqrafiya | "deyilmiydik"; "təkəbürlük etdilər" | "deyildikmi"; "təkəbbürlük etdilər"
- 25:27 | üslub | Peyğəmbərə aid "(Onun yolunu)" böyük hərflə | "(onun yolunu)"
- 25:42 | durğu | "…sapdırsın!”." | "…sapdırsın!”"
- 25:47 | qrammatika | "yuxunu (…) istirahət edən Odur" — "yuxunu istirahət edən" mənasızdır | "yuxunu istirahət (vasitəsi) edən Odur"
- 25:58 | hərf + boşluq | "(heç zaman) Ölməyənə"; "uca tut)!Qullarının" | "ölməyənə"; "uca tut)! Qullarının"
- 25:63 | orfoqrafiya | "vaqarla, təvazökarla yeriyərlər" | "vüqarla, təvazökarlıqla"
- 26:4 | simvol | ayə prefiksindən əvvəl görünməz ​ simvolu var | sil
- 26:96 | zaman | "Onlar orada bir-biri ilə çəkişərək dedilər:" (axirət hadisəsi, gələcək zaman) | "…deyəcəklər:"
### Hissə h38 (26:111–27:55)
- 27:3 | MƏTN (artıq əlavə) | "axirətə yəqinliklə iman edərək namazı … qılırlar … və onlar axirətə də yəqinliklə iman edirlər" — ərəbcədə axirətə iman yalnız sonda bir dəfədir; əvvəlki ifadə artıqdır | "Onlar (o möminlər) ki, namazı … qılırlar, zəkatı verirlər və onlar axirətə də yəqinliklə iman edirlər."
- 27:7–27:55 | dırnaq üslubu | ASCII "…" | vahid üsluba gətir
- 27:10, 27:12, 27:42, 27:45 | boşluq | '(Biz ona dedik: )"Ey Musa!', '(Süleyman dedi: )"Bizə', '(onlara belə desin: )"Allaha' | '(Biz ona dedik:) "Ey Musa!' və s.
- 27:26 | durğu | '…Ərşin Rəbbidir"!' | '…Ərşin Rəbbidir!"'
- 27:31 | orfoqrafiya | "təkəbbürlülük etməyin" | "təkəbbürlük etməyin"
### Hissə h39 (27:56–28:50)
- 27:58 | üslub | Allahın sözündə "(Sonra) biz onların üstünə…" kiçik hərflə | "Biz"
- 27:59 | dırnaq | De: "Həmd olsun Allaha… — sitat bağlanmır (27:60–63 ritorik sualların davamıdır) | 27:59-un sonunda " qoy, ya da 27:63-ün sonunda
- 27:67/27:68 | dırnaq | kafirlərin sözü 27:67-də bağlanır, 27:68 açılmadan " ilə bitir | 27:67-nin sonundakı dırnağı sil
- 27:71 | durğu | "(o zaman deyin: ) bu vəd" | "(o zaman deyin:) bu vəd"
- 27:73 | dırnaq | "…şükr etmirlər"." — açılmamış artıq dırnaq | sonuncu dırnağı sil
- 27:84 | zaman | "(hesaba çəkilmək üçün) gəldikdə (Allah) dedi" — axirət hadisəsi | "…deyəcək"
- 28:3, 28:4, 28:6, 28:8 | orfoqrafiya | xüsusi isimlər kiçik hərflə: "fironun", "firon", "firona, hamana" (28:8-də "Haman" böyük) | "Fironun", "Firon", "Firona, Hamana"
- 28:23 | orfoqrafiya | "niyə heyvanlarınıza su vermirsiz" | "vermirsiniz"
- 28:33 | orfoqrafiya | "Həqiqətəndə" | "Həqiqətən də"
- 28:34 | qrammatika | "məndən daha bəlağətlidir (düzgündür odur ki,)" — mötərizə və cümlə qarışıb | "məndən daha bəlağətlidir (daha düzgün danışır), odur ki,"
- 28:38 | qrammatika | "bəlkə mən Musanın ilahının yanına çıxdım" | "bəlkə … yanına çıxam"
- 28:48 | orfoqrafiya | "deyilmiydi" | "deyildimi"
### Hissə h40 (28:51–29:45)
- 28:58 | orfoqrafiya | "(və bunula da nankorluq edən)" | "bununla"
- 28:62 | dırnaq | ”İddia etdiyiniz şəriklərim hardadır? — bağlanmır | "…hardadır?”"
- 28:63, 28:75 | zaman | axirət səhnəsində "dedilər", "onlar bildilər ki" | "deyəcəklər", "biləcəklər"
- 28:88 | simvol | ayə "…qaytarılacaqsınız!}" ilə bitir — artıq "}" | "…qaytarılacaqsınız!"
- 29:18 | təkrar | "sizdən əvvəl də (bir çox) ümmətlər də" | "sizdən əvvəl də (bir çox) ümmətlər"
- 29:28 | təkrar | "bir fahiş (…) bir iş" | "fahiş (…) bir iş"
- 29:35 | qrammatika | "Biz ondan o (kənddən) … ayət saxladıq" — mötərizəsiz oxunanda "o" asılı qalır | "Biz ondan (o kənddən) … ayət saxladıq"
- 29:39 | orfoqrafiya | "təkəbbürlülük etdilər" | "təkəbbürlük etdilər"
- 29:45 | orfoqrafiya | "munkər" | "münkər"
### Hissə h41 (29:46–31:21)
- 29:50 | orfoqrafiya | "deyilmiydi" | "deyildimi"
- 29:55 | dırnaq | O, (onlara) deyəcək: ”Etdiyiniz əməlləri(n cəzasını) dadın! — bağlanmır | "…dadın!”"
- 30:27 | MƏNA (söz seçimi) | "o, Onun üçün daha kiçik (xəfif, bəsit) olandır" — وَهُوَ أَهْوَنُ عَلَيْهِ "daha asandır" deməkdir | "bu, Onun üçün daha asandır"
- 30:42 | dırnaq | De: ”Yer üzündə seyr edin… — bağlanmır | "…müşriklər idi.”"
- 31:10 | qrammatika | "görə bildiyiniz bir dirəklər olmadan" | "görə bildiyiniz bir dirək olmadan"
### Hissə h42 (31:22–33:30)
- 31:23 | üslub | "Bizədir Onların dönüşü!" — kafirlərə aid böyük "Onların" | "onların"
- 31:32 | qrammatika | "(küfrü çox olandan, nankordan)" | "(küfrü çox olan, nankor)"
- 32:11 | dırnaq | De: ”(Canınızı almaq üçün)… — bağlanmır | "…geri döndəriləcəksiniz!”"
- 32:12 | durğu | "(belə deyirlər) : ”" | "(belə deyirlər): ”"
### Hissə h43 (33:31–34:23)
- 33:67 | zaman | "Onlar (həmçinin belə) dedilər" (axirət səhnəsi; 33:66 "deyəcəklər") | "…deyəcəklər"
- 34:1 | durğu | "…Ona məxsusdur axirətdə də həmd yalnız Onadır" — cümlə ayrılmayıb | "…Ona məxsusdur! Axirətdə də həmd yalnız Onadır."
- 34:12 | üslub | cinlərə aid "Ona Səir (Cəhənnəm) əzabını daddırardıq" böyük hərflə | "ona"
- 34:15 | orfoqrafiya | "Səbə qəbiləsində" (27:22-də "Səba") | eyni yazılış: "Səba"
- 34:17 | üslub | Allahın sözündə "Məgər biz … cəzalandırarıqmı?" kiçik hərflə | "Biz"
### Hissə h44 (34:24–36:27)
- 34:39 | orfoqrafiya | "daha xeyirlisini verəcəkdirr" | "verəcəkdir"
- 34:43 | dırnaq | birinci sitat "…bir kişidir." bağlanmadan "(Həmçinin) onlar dedilər:" gəlir | "…bir kişidir"."
- 34:48 | dırnaq | '…məhv edir)!" O, qeybləri Biləndir!"' — dırnaq ortada bağlanır | '…məhv edir)! O, qeybləri Biləndir!"'
- 35:40 | dırnaq | De: "Allahı qoyub… — bağlanmır | '…vəd etmirlər!"'
### Hissə h45 (36:28–37:144)
- 36:53 | təkrar | "O yalnız bir dəhşətli, tükürpədici bir səs olacaq" | "O yalnız dəhşətli, tükürpədici bir səs olacaq"
- 36:76 | orfoqrafiya | "səni hüznlədirməsin" | "hüznləndirməsin"
- 37:67 | orfoqrafiya | "qaynar su qarışıdırılmış içki" | "qarışdırılmış"
- 37:104/37:105 | dırnaq | 37:104 ”Ey İbrahim!” bağlanır, 37:105 ("Artıq sən röyanı… doğruladın!") sözün davamıdır, açılmadan ” ilə bitir | 37:104-ün sonundakı ”-ni sil
- 37:79, 37:109, 37:120, 37:130 | durğu | ”Aləmlərdə Nuha salam olsun” və s. — sitat nidasız/nöqtəsiz | ”…salam olsun!”
### Hissə h46 (37:145–39:31)
- 38:36 | üslub | Süleymana aid "o, Onun əmri ilə … gedirdi" böyük hərflə | "onun əmri ilə"
- 38:44 | üslub | Əyyuba aid "Şübhəsiz ki O, (…) üz tutan biridir" böyük hərflə | "Şübhəsiz ki o,"
- 38:73 | qrammatika | "dərhal ona (səcdə) etdilər" | "dərhal (ona) səcdə etdilər"
- 39:10 | dırnaq + durğu | 'De: “(Rəbbiniz belə buyurur) : “Ey iman edən…' — qarışıq simvol, artıq boşluq | 'De: (Rəbbiniz belə buyurur): ”Ey iman edən…'
### Hissə h47 (39:32–40:40)
- 39:32, 39:33 | orfoqrafiya | "sidq (gərçək)" | "sidq (gerçək)"
- 39:52 | orfoqrafiya | "ruzini (nəmətlərini)" | "nemətlərini"
- 39:53 | üslub + durğu | "Qullarıma (mənim bu sözümü) de" — Allahın sözü, kiçik hərf; "…Rahimdir (…)”!" | "(Mənim bu sözümü)"; "…Edəndir)!”"
- 39:60, 39:72, 40:27 | orfoqrafiya | "mutəkəbbirlər", "Mutəkəbbirlərin", "mutəkəbbirdən" | "mütəkəbbir…"
- 39:65/39:66 | dırnaq | 39:66 "…şükr edənlərdən ol!”" — açılmamış dırnaqla bitir | sonuncu ”-ni sil
- 40:11 | zaman | "Onlar dedilər: ”Rəbbimiz! Sən bizi iki dəfə öldürdün…" (axirət səhnəsi) | "Onlar deyəcəklər:"
- 40:24, 40:37 | orfoqrafiya | "firona", "Beləcə firona…" kiçik hərflə | "Firona"
### Hissə h48 (40:41–41:46)
- 40:45, 40:46 | orfoqrafiya | "ali-fironu (fironun tərəfdarlarını)" | "Ali-Fironu (Fironun tərəfdarlarını)"
- 40:60 | dırnaq | Rəbbiniz dedi: ”Mənə dua edin… — bağlanmır | "…zəlil olaraq girəcəklər!”"
- 40:64 | orfoqrafiya | "səmanı də tavan edən" | "səmanı da"
- 41:6 | dırnaq | De: ”Mən ancaq sizin kimi bir bəşərəm… — bağlanmır (41:7 davamdır) | 41:7-nin sonuna ”
- 41:11 | MƏNA (əvəzlik) | "Sonra o, … səmaya yönəlib Ona və yerə dedi" — böyük/kiçik hərf tərsinədir: fail Allahdır (O), xitab səmayadır (ona) | "Sonra O, … səmaya yönəlib ona və yerə dedi"
- 41:15 | üslub | Allahın sözündə "Onlar bizim ayətlərimizi inkar edirdilər" kiçik hərflə | "Bizim"
- 41:44 | orfoqrafiya + dırnaq | "deyilmiydi"; De: ”O, (yalnız) iman edənlər üçün… — bağlanmır | "deyildimi"; "…nida edilməkdədir.”"
### Hissə h49 (41:47–43:23)
- 41:47 | zaman | "O gün O, onlara … nida edəcək: … Onlar dedilər:" | "Onlar deyəcəklər:"
- 42:14 | qrammatika | "onların arasında hökm verərdi" (fail yoxdur) | "hökm verilərdi"
- 42:15 | dırnaq | de: "Mən, Allahın kitabdan… — bağlanmır | '…dönüş də yalnız Onadır!"'
- 42:16 | orfoqrafiya | "Rəbblərinin" (mətnin qalanında "Rəblərinin") | "Rəblərinin"
- 42:31 | orfoqrafiya | "aciz qoya bilen deyilsiniz" | "bilən"
- 42:45 | dırnaq | İman edənlər … deyəcəklər: "Həqiqətən də… — bağlanmır | '…əzab içindədirlər!"'
- 43:16 | orfoqrafiya | "oğlullarla" | "oğullarla"
### Hissə h50 (43:24–45:37)
- 43:31, 43:53 | orfoqrafiya | "deyilmiydi" (3 dəfə) | "deyildimi"
- 43:42 | hərf | "(onlara Gücümüz yetir)" | "(onlara gücümüz yetir)"
- 43:46, 44:17, 44:18 | orfoqrafiya | "firona", "fironun" kiçik hərflə | "Firona", "Fironun"
- 43:78 | üslub | Allahın sözündə "And olsun ki, biz sizə haqq ilə gəlmişdik" kiçik hərflə | "Biz"
- 44:31 | MƏNA (izah) | "Firondan (firon kimi bir əzabdan xilas etdik)" — مِن فِرْعَوْنَ = Firondan (onun verdiyi əzabdan) | "Firondan (onun verdiyi əzabdan xilas etdik)"
- 45:16 | orfoqrafiya | "Biz bəni İsrailə" | "Bəni İsrailə"
### Hissə h51 (46:1–48:17)
- 46:4 | dırnaq | De: ”Necə bilirsiniz… — bağlanmır | "…əsər-əlamət gətirin!”"
- 46:8 | dırnaq | 'deyirlər ki: ”O, onu … gətirmişdir? De:' — birinci sitat bağlanmır | "…iftira olaraq gətirmişdir?” De:"
- 46:17 | MÖTƏRİZƏ | "(deyirdilər: “Veyl olsun sənə…" — mötərizə bağlanmır | "(deyirdilər): “Veyl olsun sənə…"
- 46:20 | MÖTƏRİZƏ | "(və onlara belə deyiləcək: “Siz dünya…" — mötərizə bağlanmır | "(və onlara belə deyiləcək): “Siz dünya…"
- 46:24 | dırnaq/hərf | "(Hud dedi: ”Xeyr,) Əksinə!" — dırnaq mötərizənin içində açılır | "(Hud dedi:) ”Xeyr, əksinə!"
- 46:28 | orfoqrafiya | "deyilmiydi" | "deyildimi"
- 46:29 | dırnaq/mötərizə | '”Susun (və dinləyin!” Quranın oxunması) bitdikdə(n sonra isə)' — mötərizə ilə dırnaq bir-birinə girib | '”Susun (və dinləyin)!” O (Quranın oxunması) bitdikdə isə…'
- 46:34 | zaman | "O dedi: ”Küfr etdiyinizə görə dadın əzabı!”" (axirət səhnəsi) | "O deyəcək:"
- 47:4 | MƏNA | "ya əsir olaraq yanınızda saxlayın, (və ya fidyə almadan), yaxud da fidyə … ilə buraxın" — فَإِمَّا مَنًّا بَعْدُ وَإِمَّا فِدَاءً: "ya (fidyəsiz) minnət edib buraxın, ya da fidyə ilə"; "əsir olaraq saxlayın" ərəbcədə yoxdur | "sonra (onları) ya (fidyəsiz) lütf edərək, ya da fidyə qarşılığında buraxın"
- 47:15 | orfoqrafiya | "qaynar su içirdilərək" | "içirilərək"
- 47:19 | orfoqrafiya | "möminə qadınların" | "mömin qadınların"
- 48:6 | orfoqrafiya | "pis zəndə olan" | "pis zənndə olan"
- 48:15 | qrammatika | "Sizə qənimətlərə tərəf … getdiyiniz zaman" | "Siz qənimətlərə tərəf…"
### Hissə h52 (48:18–51:30)
- 49:2 | MƏNA (orta) + üslub | "Onu bir-birinizi çağırdığınız kimi çağırmayın" — ərəbcədə وَلَا تَجْهَرُوا لَهُ بِالْقَوْلِ كَجَهْرِ بَعْضِكُمْ لِبَعْضٍ: "bir-birinizlə uca səslə danışdığınız kimi onunla uca səslə danışmayın" (çağırma barədə olan 24:63-dür); böyük "Onu" Peyğəmbərə aiddir | "…və bir-birinizlə ucadan danışdığınız kimi onunla ucadan danışmayın!"
- 49:12 | durğu | "…araşdırmayın) Kiminiz" — cümlə sonunda işarə yoxdur | "…araşdırmayın)! Kiminiz"
- 50:13 | orfoqrafiya | "Ad (qövmü), firon və…" | "Firon"
- 50:18 | orfoqrafiya | "(əməllətini yazan mələk)" | "(əməllərini yazan mələk)"
- 50:23, 50:27, 50:28 | zaman | axirət səhnəsində "dedi" (50:30 "deyəcəyik") | "deyəcək"
- 50:32 | boşluq | "vəd edilən (Cənnət)!O, hər bir" | "(Cənnət)! O, hər bir"
- 50:44 | üslub | Allahın sözündə "Bu, bizim üçün asan bir toplamadır" kiçik hərflə | "Bizim"
- 51:14 | dırnaq | "Dadın əzabınızı!… — bağlanmır | '…istədiyiniz) şeydir!"'
### Hissə h53 (51:31–54:55)
- 51:38, 54:41 | orfoqrafiya | "fironun", "firon ailəsinə" kiçik hərflə | "Fironun", "Firon ailəsinə"
- 51:44 | orfoqrafiya | "təkəbbürlülük etdilər" | "təkəbbürlük etdilər"
- 52:14–52:16 | dırnaq | (Onlara belə deyiləcək): ”Bu, sizin… — sitat 52:16-da bağlanmır | "…cəzasını almaqdasınız!”"
- 52:19 | dırnaq/mötərizə | '(Onlara belə deyiləcək: ”Vaxtilə dünyada ikən) etdiyiniz…' — dırnaq mötərizənin içində açılır | '(Onlara belə deyiləcək:) ”Nuşcanlıqla yeyib-için, (vaxtilə dünyada ikən) etdiyiniz (saleh) əməllərə görə!”' — ərəbcədəki sıra
- 52:33 | dırnaq | "…demədiklərini deyir)?”" — açılmamış artıq dırnaq | sonuncu ”-ni sil
- 53:18 | üslub | Peyğəmbərə aid "Allahın Ona göstərdiklərini" böyük hərflə | "ona"
- 53:42 | qrammatika | "(…hər şeyin) bitib durduğu yeri (yalnız) sənin Rəbbinədir (?)" | "(Ona xəbər verilmədimi ki,) son dönüş (hər şeyin bitib durduğu yer) yalnız sənin Rəbbinədir!" — 53:43 kimi
- 54:20 | orfoqrafiya | "çıxırılmış xurma kötükləri" | "qoparılmış (kökündən çıxarılmış) xurma kötükləri"
- 54:35 | üslub | "öz qatımızdan bir nemət olaraq" | "Öz qatımızdan"
- 54:47 | qrammatika | "mucrimlər … içindədir" | "içindədirlər"
### Hissə h54 (55:1–57:29)
- 55:5 | dipnot | "o ikisi bir hesabladır2" — yetim "2" | "…bir hesabladır."
- 55:48 | MƏNA | "(O iki Cənnət, meyvələri bol olan) iki budaq sahibidir" — ذَوَاتَا أَفْنَانٍ: "ikisi də (çoxlu) budaqlıdır", "iki budaq" deyil | "O iki Cənnət budaq-budaqdır (çoxlu budaqlı ağaclarla doludur)." — ذَوَاتَا ikilik formasıdır, mübtəda mötərizəyə girmir
- 56:29 | durğu | "..və budaqları" (iki nöqtə) | "...və budaqları"
- 56:55 | MƏNA (şəxs/zaman) | "Onlar (onu) susuz qalmış dəvələrin içməyi ilə içirlər" — 56:51–54 kimi xitab ikinci şəxsdir və gələcəkdir (فَشَارِبُونَ) | "Siz (onu) susuz qalmış dəvələrin içdiyi kimi içəcəksiniz!"
- 56:57 | orfoqrafiya | "deyilmiydiniz" | "deyildinizmi"
- 56:84 | simvol | ayə mətninin ortasında sətir sonu (\n) var: "(aciz və çarəsiz ⏎ halda…)" | sətir sonunu sil
- 57:8 | MƏNA (əvəzlik) | "həqiqətən də o, sizdən əhd (söz) almışdır" — kiçik "o" Rəsulu göstərir; وَقَدْ أَخَذَ مِيثَاقَكُمْ — əhdi Allah alıb | "həqiqətən də O (Allah), sizdən əhd almışdır"
- 57:11 | üslub | Allaha aid "o da onu … qat-qat artırsın" kiçik hərflə | "O da onu…"
- 57:14 | orfoqrafiya + dırnaq | "deyilmiydik"; möminlərin cavabı bağlanmır | "deyildikmi"; "…sizi Allah ilə … aldatdı.”"
- 57:27 | orfoqrafiya | "şəvqət və rəhmət qoyduq" | "şəfqət"
- (bütün mətn) | struktur | səcdə işarəsi ۩ yalnız 3 ayədə var (53:62, 84:21, 96:19); digər 12 səcdə ayəsində yoxdur | ya hamısına əlavə et, ya da heç birinə
### Hissə h55 (58:1–61:14)
- 58:2 | orfoqrafiya + dipnot | "zihar edənlər ki1 var"; "munkər olanını" | "ki var"; "münkər"
- 58:8 | orfoqrafiya | "deyilmiydi" | "deyildimi"
- 58:21 | qrammatika | "Mən və rəsullarım qalib gələcəkdir!" | "…qalib gələcəyik!"
- 59:10 | orfoqrafiya | "Şəvqətlisən" | "Şəfqətlisən"
- 59:23 | hərf + orfoqrafiya | "Allahdır! əl-Məlikdir"; "əl-Mutəkəbbirdir" | "Allahdır! Əl-Məlikdir"; "əl-Mütəkəbbirdir"
- 60:4 | dırnaq (məna) | İbrahimin atasına sözü "…heç bir şeyə malik deyiləm." bağlanmadan dua ("Rəbbimiz! Yalnız Sənə təvəkkül etdik…") ona qoşulur — dua İbrahim və yanındakılarındır | "…malik deyiləm.”" (sitatı bağla), ardınca "(Onlar dua edərək dedilər:) ”Rəbbimiz!…"
- 60:10, 60:11 | orfoqrafiya | "muhacirlər", "murtəd" | "mühacirlər", "mürtəd"
- 61:8, 61:9 | orfoqrafiya | "xoşlamasalarda" | "xoşlamasalar da"
- 61:14 | orfoqrafiya | "bəni İsraildən" | "Bəni İsraildən"
### Hissə h56 (62:1–66:12)
- 62:6 | qrammatika | "…iddia edirsinizsə, o zaman doğru danışanlarsınızsa, özünüzə ölüm diləyin" | "…iddia edirsinizsə, doğru danışırsınızsa, özünüzə ölüm diləyin"
- 64:15 | orfoqrafiya | "lakin unutmayın k," | "unutmayın ki,"
- 66:11 | orfoqrafiya | "fironun arvadını", "məni firondan" | "Fironun", "Firondan"
### Hissə h57 (67:1–71:28)
- 67:3 | orfoqrafiya | "əsla bir ziddiyət … görə bilməzsən" | "ziddiyyət"
- 67:24 | orfoqrafiya | "(ölümünüzdən sonra dirilidilib)" | "dirildilib"
- 68:13 | orfoqrafiya | "halal-haram dinşəmədən çox yeyib-içən" | "halal-haram seçmədən"
- 69:15/69:16 | təkrar | 69:15 "...və" ilə bitir, 69:16 "...və səma da" ilə başlayır | 69:16-dakı "və"-ni sil
- 70:42 | durğu | "…oynasınlar (əylənsinlər)!." | "…(əylənsinlər)!"
- 71:1 | boşluq | "(və ona belə vəhy etdik: ) “Öz qövmünü" | "(və ona belə vəhy etdik:) ”Öz qövmünü"
- 71:27 | üslub | Nuhun Allaha xitabında "sənin qullarını azdıracaq" kiçik hərflə | "Sənin qullarını"
### Hissə h58 (72:1–77:50)
- 72:20, 72:22–72:23 | dırnaq | De: ”Mən yalnız öz Rəbbimə…; De: ”Həqiqətən də, məni Allahdan… — bağlanmır | "…şərik qoşmuram!”"; 72:23 sonuna ”
- 73:15 | orfoqrafiya | "necə ki, firona da" | "Firona"
- 74:2 | durğu | "Rəbbinin qəzəbi əzabı ilə" | "Rəbbinin qəzəbi, əzabı ilə"
- 74:48 | zaman | "Şəfaətçilərin şəfaəti onlara heç bir fayda vermədi!" (axirət) | "…fayda verməyəcək!"
- 77:6 | durğu | "Üzr və ya xəbərdar edən (…)." — cümlə 77:7-də davam edir, nöqtə yanlışdır | "…(…) ki,…"
- 77:43 | MÖTƏRİZƏ | '(Onlara deyiləcək: ”Etdiyiniz…' — mötərizə bağlanmır | '(Onlara deyiləcək:) ”Etdiyiniz…'
- 77:44 | dırnaq | "…belə mükafatlandırırıq!”" — açılmamış artıq dırnaq (sitat 77:43-də bağlanıb) | sonuncu ”-ni sil
### Hissə h59 (78:1–86:17)
- 79:14 | boşluq | "meydanında  (məhşərdə)" — iki boşluq | bir boşluq
- 79:38 | durğu | "..(və həmçinin)" (iki nöqtə) | "...(və həmçinin)"
- 80:17 | MƏNA (artıq əlavə) | "Necə də kafirdir o, onun kimi kafir olan yoxdur!" — مَا أَكْفَرَهُ yalnız "necə də nankordur!" deməkdir; "onun kimi kafir olan yoxdur" ərəbcədə yoxdur | "Qoy ölsün insan! Necə də nankordur (kafirdir) o!"
- 82:15 | orfoqrafiya | "Din günüdə onlar ona daxil olacaqlar" | "Din günündə"
- 83:4/83:5 | qrammatika + durğu | "öldükdən sonra dirildəcəklərini zənn etmirlərmi (bilmirlərmi)? / böyük bir gündə?" — feil məlum növdədir və cümlə sual işarəsi ilə yarıda kəsilir | "…dirildiləcəklərini zənn etmirlərmi (bilmirlərmi)… / …böyük bir gündə?"
### Hissə h60 (87:1–114:6)
- 89:10 | MƏNA (izah) + orfoqrafiya | "Ehramlar (və yaxud … payalar basdıran) firona" — "sahibi" sözü yoxdur, cümlə qırıqdır; ذِي الْأَوْتَادِ = "dirəklər (payalar) sahibi" | "Dirəklər (payalar) sahibi Firona (da)"
- 91:13 | dırnaq | Allahın rəsulunun sözü bağlanmır | "…mane olmaqdan həzər edin!”"
- 112:1, 113:1 | dırnaq | De: ”O Allah təkdir! / De: ”Sığınıram sübhün Rəbbinə! — surənin sonunda bağlanmır | 112:4 və 113:5 sonuna ”
- 112:1 | durğu | "De: ”O Allah təkdir!" (قُلْ هُوَ اللَّهُ أَحَدٌ) | "De: ”O, Allahdır, Təkdir!”"
- 114:5/114:6 | dırnaq | sitat 114:5-də bağlanır, 114:6 ("O, həm cinlərdəndir…") sitatın davamıdır | dırnağı 114:6-nın sonuna köçür


## 4. İkinci keçid — bütün ayələr ərəbcə ilə yanaşı

Birinci keçid (yuxarıdakı §3) Azərbaycan mətnini oxuyub ərəbcəyə yalnız şübhəli yerlərdə baxmışdı. İkinci keçiddə **6236 ayənin hamısı** `quranapp.db`-dəki ərəbcə mətnlə (Hafs) **yan-yana** oxundu, üstəlik üç avtomatik yoxlama aparıldı: eyni ərəbcəli ayələrin tərcümədə fərqlənməsi, Allahın adlarının ərəbcə kökü ilə tutuşdurulması (məs. الحكيم ↔ "Hakim") və ərəbcə/tərcümə uzunluq nisbəti (buraxılmış hissələri tapmaq üçün). Aşağıdakılar §3-də **olmayan** yeni tapıntılardır; ən vacibləri §1-ə də (🆕 ilə) əlavə olunub, sistemli olanlar §2-dədir.

Format §3-dəki kimidir: `ayə | növ | problem | təklif`. Əlavə növlər: BURAXILMA (ərəbcədə olan ifadə tərcümədə yoxdur), qiraət (tərcümə Hafsdan fərqli qiraətə əsaslanır), AYDINLIQ.

### Avtomatik yoxlamalardan

- 3:92 | BURAXILMA | ayənin ikinci yarısı yoxdur: وَمَا تُنفِقُوا مِن شَيْءٍ فَإِنَّ اللَّهَ بِهِ عَلِيمٌ | "...nail olmayacaqsınız! Nə (Allah yolunda) infaq etsəniz, şübhəsiz ki, Allah onu Biləndir."
- 45:2, 46:2 | MƏNA (ad) | ərəbcədə الْعَزِيزِ الْحَكِيمِ, tərcümədə "Aziz və Alim (Bilən)" — 39:1 eyni ayəni düz verir ("Aziz və Hakim") | "Aziz (İzzət Sahibi) və Hakim (Hikmət Sahibi) olan Allah"
- 100:11 | durğu | xəbər cümləsi "?" ilə bitir | "...xəbəri Olandır!"

### Hissə h01 (1:1–2:74)

- 2:66 | MƏNA (kiçik) | "ondan sonrakılar və onun arxasında olanlar üçün" — لِّمَا بَيْنَ يَدَيْهَا وَمَا خَلْفَهَا: önündəkilər (o vaxtkılar) və arxasında olanlar (sonrakılar); tərcümə eyni şeyi iki dəfə deyir | "onun (o vaxtkı) önündəkilər və sonra gələnlər üçün"
- 2:71 | orfoqrafiya | "heç bir Qüsuru olmayan" — cümlə ortasında böyük hərf | "qüsuru"

### Hissə h02 (2:75–2:141)

- 2:93 | durğu | "qaldırmışdıq; Sizə əta etdiyimiz…" — Allahın sözü dırnaqsız, ";"-dən sonra böyük hərf (Tur məsələsi üçün §2-yə bax) | "…qaldırmışdıq (və demişdik): ”Sizə əta etdiyimiz şeyi qüvvətlə götürün və (haqqı) eşidin!”"

### Hissə h03 (2:142–2:202)

- 2:196 | MƏNA (ciddi — hökm) | "qoy Həcdə yeddi gün, (əhlinin yanına) qayıtdıqda isə üç gün oruc tutsun" — ərəbcədə tərsinədir: فَصِيَامُ ثَلَاثَةِ أَيَّامٍ فِي الْحَجِّ وَسَبْعَةٍ إِذَا رَجَعْتُمْ | "qoy Həccdə üç gün, (əhlinin yanına) qayıtdıqda isə yeddi gün oruc tutsun"
- 2:173 | izah (kiçik) | "meytəni (İslami kəsimlə kəsilməmiş heyvanları)" — الْمَيْتَةَ kəsilmədən (özü) ölmüş heyvandır; izah daha geniş hökm kimi oxunur | "meytəni (kəsilmədən ölmüş heyvanı)"

### Hissə h04 (2:203–2:252)

- 2:206 | qrammatika | "onu izzət (kibr, lovğalıq) götürər və (bu da onu) günah … etməyə davam edər" — cümlə qırıqdır (أَخَذَتْهُ الْعِزَّةُ بِالْإِثْمِ) | "…onu izzət (kibr) günaha sövq edər (təkəbbürü onu günahda israra aparar)"
- 2:213 | MƏNA (kiçik) | "müjdələyən və qorxudan rəsullar göndərdi" — ərəbcədə النَّبِيِّينَ; "hökm versinlər" — لِيَحْكُمَ təkdir (kitab/Allah hökm etsin) | "nəbilər göndərdi"; "…hökm etsin deyə"
- 2:217 | MƏNA (kiçik) | "Ona küfr etmək və Məscidulharamda küfr etmək" — وَالْمَسْجِدِ الْحَرَامِ "Allahın yoluna əngəl olmaq"-a bağlıdır: Məscidulharama (getməyə) mane olmaq | "…Ona küfr etmək, Məscidulharama (girməyə) mane olmaq və onun əhlini oradan çıxarmaq…"

### Hissə h05 (2:253–3:14)

- 2:282 | MƏNA (orta — fail) | "(katib də) ondan heç bir şeyi əskiltməsin" — وَلْيَتَّقِ اللَّهَ رَبَّهُ وَلَا يَبْخَسْ مِنْهُ شَيْئًا imla edən borcluya aiddir, katibə yox | "…və Rəbbi olan Allahdan qorxsun və (borcdan) heç bir şeyi əskiltməsin"
- 2:282 | qrammatika | "(Beləcə) Allah sizi öyrətməkdədir" | "Allah sizə öyrədir"
- 3:3–3:4 | MƏNA (orta — struktur) | "Tövratı və İncili O nazil etmişdir. Bundan öncə insanlara hidayət olaraq Furqanı nazil edən də Odur" — ərəbcədə مِن قَبْلُ هُدًى لِّلنَّاسِ Tövrat və İncilə aiddir, Furqan ayrıca sonra gəlir | 3:3 "…Tövratı və İncili də O nazil etdi," 3:4 "…bundan öncə, insanlara hidayət olaraq. Furqanı da O nazil etdi."

### Hissə h06 (3:15–3:92)

- 3:61 | MƏNA (söz) | "Allahın lə’nətini zalımların üzərinə edək" — ərəbcədə عَلَى الْكَاذِبِينَ | "…yalançıların üzərinə edək"
- 3:49 | qrammatika | "mən sizin üçün palçıqdan … bir şey düzəldəcək, sonra da ona üfürəcəyəm" | "düzəldəcəyəm"
- 3:37 | MƏNA (kiçik) | "O, onu(n bu duasını) gözəl bir qəbul ilə qəbul etdi" — فَتَقَبَّلَهَا رَبُّهَا: Rəbbi onu (Məryəmi) qəbul etdi; izah obyekti dəyişir | "Rəbbi onu (Məryəmi) gözəl bir qəbulla qəbul etdi"

### Hissə h07 (3:93–3:170)

- 3:143 | MƏNA (orta — əvəzlik) | "siz onlarla qarşılaşmadan əvvəl ölümü təmənna edirdiniz. (Budur, indi) artıq onları gördünüz və siz onlara baxmaqdasınız" — مِن قَبْلِ أَن تَلْقَوْهُ فَقَدْ رَأَيْتُمُوهُ: tək əvəzlik ölümə aiddir, düşmənə yox | "…ölümlə qarşılaşmadan əvvəl onu təmənna edirdiniz. Artıq onu (gözünüzlə) gördünüz, (ona) baxırsınız"

### Hissə h10 (4:88–4:147)

- 4:102 | MƏNA (kiçik — fail) | "Onlar səcdə etdikləri zaman (digər dəstə) arxanızda olsun" — فَإِذَا سَجَدُوا فَلْيَكُونُوا مِن وَرَائِكُمْ: səcdə edən (birinci) dəstə arxaya keçir; mötərizə faili dəyişir | "Onlar səcdə etdikləri zaman (namazı bitirib) arxanıza keçsinlər"

### Hissə h11 (4:148–5:26)

- 5:1 | MƏNA (kiçik — hökm) | "(digər) heyvanlar sizə halal edildi" — بَهِيمَةُ الْأَنْعَامِ yalnız ən'am heyvanlarıdır (dəvə, mal-qara, qoyun-keçi), "bütün heyvanlar" deyil | "ən'am heyvanları (dəvə, mal-qara, qoyun-keçi) sizə halal edildi"
- 4:175 | MƏNA (kiçik) | "ona (İslama) sarılanlara" — وَاعْتَصَمُوا بِهِ: Ona (Allaha) sarılanlar; izah obyekti dəyişir | "Ona sarılanlara"
- 5:16 | üslub | "onun rizasının arxasınca gedənləri" — رِضْوَانَهُ Allaha aiddir | "Onun rizasının"

### Hissə h12 (5:27–5:81)

- 5:33 | BURAXILMA | ayənin sonu yoxdur: ذَٰلِكَ لَهُمْ خِزْيٌ فِي الدُّنْيَا ۖ وَلَهُمْ فِي الْآخِرَةِ عَذَابٌ عَظِيمٌ | "…sürgün edilmələridir. Bu, onlar üçün dünyada bir rüsvayçılıqdır, axirətdə isə onlar üçün əzəmətli bir əzab vardır!"
- 5:31 | əlavə (mötərizəsiz) | "ölmüş digər bir qarğanı basdırmaqla" — ərəbcədə yoxdur (təfsir izahıdır), mötərizəsiz verilib | "(ölmüş bir qarğanı basdıraraq)"

### Hissə h13 (5:82–6:35)

- 6:6 | MƏNA (kiçik) | "Biz onları yer üzündə sizləri yerləşdirmədiyimiz yerlərdə yerləşdirmişdik" — مَكَّنَّاهُمْ فِي الْأَرْضِ مَا لَمْ نُمَكِّن لَّكُمْ yer yox, güc-imkan dərəcəsidir | "Biz onlara yer üzündə sizə vermədiyimiz (qədər) güc-imkan vermişdik"
- 5:95 | MƏNA (kiçik) | "bir miskini yedirmək kəffarəsi" — طَعَامُ مَسَاكِينَ cəmdir | "miskinləri yedirmək kəffarəsi"

### Hissə h15 (6:111–6:165)

- 6:124 | BURAXILMA (kiçik) | "onlara kiçilmək, zəlil olmaq … üz verəcəkdir" — صَغَارٌ عِندَ اللَّهِ: "Allah yanında" düşüb | "…Allah yanında zəlillik və şiddətli əzab üz verəcəkdir"
- 6:131 | BURAXILMA (kiçik) | "şəhərlər əhlini, onlar qafillər ikən həlak edən deyildir" — بِظُلْمٍ ("zülmlə, haqsız yerə") düşüb | "…qafil ikən zülmlə (haqsız yerə) həlak edən deyildir"

### Hissə h16 (7:1–7:87)

- 7:3 | qrammatika | "Siz necə də az (xatırlayıb) düşünüb ibrət alasınız!" — arzu forması (-asınız) yerinə xəbər forması lazımdır (قَلِيلًا مَّا تَذَكَّرُونَ) | "…ibrət alırsınız!"
- 7:27 | MƏNA (kiçik) | "Atalarınızı Cənnətdən çıxardığı kimi" — أَبَوَيْكُم ikili: ata və ana | "Ata-ananızı (Adəm və Həvvanı) Cənnətdən çıxardığı kimi"

### Hissə h17 (7:88–7:170)

- 7:163 | MƏNA (orta) | "ov etmədikləri (qeyri-şənbə) günlərində isə gəlmirdi" — وَيَوْمَ لَا يَسْبِتُونَ "şənbəyə riayət etmədikləri (adi) günlər"dir; həmin günlərdə ov icazəli idi, "ov etmədikləri" mənanı tərsinə çevirir (mənanı yalnız mötərizə xilas edir, mötərizəsiz mətn əksini deyir) | "şənbəyə riayət etmədikləri (qeyri-şənbə) günlərdə isə gəlmirdi" — müəllifin mötərizəsi saxlanır; mötərizəsiz də düz oxunur

### Hissə h18 (7:171–8:40)

- 8:11 | MƏNA (kiçik) | "qəlblərinizi bir-birinə bağlasın" — وَلِيَرْبِطَ عَلَىٰ قُلُوبِكُمْ "qəlblərinizi möhkəmləndirsin (sakitləşdirsin)" deməkdir | "qəlblərinizi möhkəmləndirsin"

### Hissə h19 (8:41–9:33)

- 8:71 | MƏNA (kiçik) | "O da onların (layiqli) cəzasını vermişdi" — فَأَمْكَنَ مِنْهُمْ: "(sizə) onlar üzərində imkan verdi (onları əlinizə saldı)" | "O da (Bədrdə) onları sizin əlinizə saldı"
- 9:13 | üslub | "(Mənim) Rəsulu(mu Məkkədən) çıxarmaq istəyən" — ayədə danışan birinci şəxs deyil (الرَّسُولِ; sonda "Allah… haqlıdır"), "(Mənim)" əlavəsi şəxsi qarışdırır | "Rəsulu (Məkkədən) çıxarmağa əzm edən"
- 8:64 | AYDINLIQ (qeyd) | "Allah və möminlərdən sənə ittiba edənlər sənə yetər" — ayə iki cür oxunur; əksər təfsirlərdə "Allah sənə də, sənə tabe olan möminlərə də yetər" (yetərlik yalnız Allaha aiddir, bax 3:173 حَسْبُنَا اللَّهُ). Alimə göstərilməsi tövsiyə olunur | "Ey Nəbi! Allah sənə də, möminlərdən sənə tabe olanlara da yetər!"

### Hissə h21 (9:93–10:25)

- 9:122 | AYDINLIQ | "Hər bir firqədən bir tayfa (cihada çıxmayıb geridə) qalsın ki, dində fəqihləşsinlər" — ərəbcədə fel نَفَرَ "çıxsın"dır; tərcümə təfsir variantlarından birini əsas mətnə qoyub feli tərsinə çevirib | "Hər firqədən bir tayfa (dini öyrənmək üçün) çıxmalı deyildimi ki, dində fəqihləşsinlər…"
- 10:11 | MƏNA (kiçik) | "Əgər Allah insanlara (dilədikləri) xeyri tez verdiyi kimi şəri də tez versəydi" — اسْتِعْجَالَهُم بِالْخَيْرِ: insanların xeyri tələsdirdikləri kimi; tələsən insanlardır, Allah deyil | "Əgər Allah insanlara, onların xeyri tələsdirdikləri kimi, şəri də tələsdirsəydi…"
- 9:125 | əlavə (mötərizəsiz) | "əzabının da üstünə əzab artırdı" — ərəbcədə yalnız رِجْسًا إِلَىٰ رِجْسِهِمْ | "murdarlıqlarının üstünə murdarlıq artırdı"

### Hissə h22 (10:26–11:5)

- 10:61 | BURAXILMA (kiçik) | "bir misqal ağırlığında olan heç bir şey" — مِن مِّثْقَالِ ذَرَّةٍ: "zərrə ağırlığında"; "zərrə" düşüb (34:3, 99:7 kimi) | "zərrə ağırlığında olan heç bir şey Rəbbindən gizli qalmır"
- 10:51 | üslub | "vaqe olduğu zamanmı Ona iman etdiniz" — بِهِ əzaba aiddir, böyük "O" Allahı göstərir | "ona iman etdiniz"

### Hissə h23 (11:6–11:83)

- 11:28 | MƏNA (kiçik) | "və siz də onu xoşlamırsınızsa, biz sizi ona ilzammı edəcəyik" — فَعُمِّيَتْ عَلَيْكُمْ ("o sizə görünməz/qaranlıq edilibsə") tərcümədə yoxdur; وَأَنتُمْ لَهَا كَارِهُونَ isə sonrakı cümlənin halıdır | "…və o (rəhmət) sizə görünməz edilibsə, siz onu xoşlamadığınız halda biz sizi ona məcburmu edəcəyik?"

### Hissə h25 (12:53–13:18)

- 12:99 | BURAXILMA | "Əmin-amanlıqda olanlar olaraq Misirə daxil olun!" — ərəbcədə إِن شَاءَ اللَّهُ var, tərcümədə düşüb | "İnşallah (Allah istəsə), əmin-amanlıq içində Misirə daxil olun!"
- 13:2 | MƏNA (kiçik) | "Allah, sizin gördüyünüz səmaları hər hansı bir dirək olmadan ucaldandır" — بِغَيْرِ عَمَدٍ تَرَوْنَهَا: "görəcəyiniz dirəklər olmadan"; "gördüyünüz" sözü dirəklərə aiddir, səmalara yox (31:10 ilə müqayisə et) | "Allah səmaları sizin görə biləcəyiniz dirəklər olmadan ucaldandır"

### Hissə h26 (13:19–14:52)

- 13:23 | BURAXILMA | "onların və atalarından və zürriyyətlərindən saleh olanların daxil olacağı" — وَأَزْوَاجِهِمْ ("zövcələrindən") düşüb | "…atalarından, zövcələrindən və zürriyyətlərindən saleh olanların…"
- 13:35 | BURAXILMA | "Müttəqilərin vəd olunduğu Cənnətin misalı budur; onun yeməyi də daimidir" — تَجْرِي مِن تَحْتِهَا الْأَنْهَارُ düşüb | "…budur: altından çaylar axır, onun yeməyi də daimidir, kölgəsi də."

### Hissə h27 (15:1–16:50)

- 15:65 | MƏNA (kiçik — şəxs) | "Onlardan heç kim arxaya dönməsin" — وَلَا يَلْتَفِتْ مِنكُمْ أَحَدٌ ("sizdən") | "Sizdən heç kim arxaya dönməsin"
- 15:43 | BURAXILMA (kiçik) | "Cəhənnəm onların vəd olunduğu şeydir" — أَجْمَعِينَ ("hamısının") düşüb | "…onların hamısının vəd olunduğu yerdir"

### Hissə h28 (16:51–16:128)

- 16:115 | izah (kiçik) + durğu | "meytəni (ölmüş heyvan ətini) (İslami kəsimlə kəsilməmiş heyvanları)" — iki mötərizə yan-yana; ikinci izah 2:173-dəki kimi hökmü genişləndirir | "meytəni (kəsilmədən ölmüş heyvanı)"

### Hissə h29 (17:1–17:98)

- 17:60 | MƏNA (kiçik) | "Biz sənə göstərdiyimiz şeyi … fitnə etdik" — الرُّؤْيَا الَّتِي أَرَيْنَاكَ: "sənə göstərdiyimiz röyanı (görüntünü)"; "röya" sözü düşüb | "sənə göstərdiyimiz o görüntünü (röyanı)"
- 17:63 | MƏNA (kiçik — şəxs) | "Cəhənnəm onun cəzası olacaqdır" — جَزَاؤُكُمْ "sizin (sənin və onların) cəzanız" | "Cəhənnəm sizin (hamınızın) tam bir cəzanızdır"

### Hissə h30 (17:99–18:74)

- 18:62 | MƏNA (orta) | "Onlar (o yerə gəlib) çatdıqda o, öz xidmətçisinə dedi: Nahar yeməyimizi gətir" — فَلَمَّا جَاوَزَا "oranı keçib getdikdə"; balığın itdiyi yerdən uzaqlaşdıqları üçün geri qayıdırlar (18:64), "çatdıqda" hekayəni pozur | "Onlar (oranı) keçib getdikdə…"
- 18:65, 18:71, 18:74 | ad yazılışı | "(Xadir)" — الخضر; azərbaycanca qəbul olunmuş forma "Xızır" (və ya "Xıdr"); "Xadir" ərəbcə "qadir" ilə qarışdırıla bilər | "(Xızır)"
- 18:14 | MƏNA (kiçik) | "qəlblərini birləşdirdik" — وَرَبَطْنَا عَلَىٰ قُلُوبِهِمْ "qəlblərini möhkəmləndirdik" (8:11, 28:10 kimi) | "qəlblərini möhkəmləndirdik"
- 18:60 | əlavə (mötərizəsiz) | "yaxud da onu tapana qədər uzun illər yol getməyə davam edəcəyəm" — أَوْ أَمْضِيَ حُقُبًا "yaxud uzun illər yol gedəcəyəm"; "onu tapana qədər" ərəbcədə yoxdur | "…çatanadək yoluma davam edəcəyəm, yaxud da uzun illər yol gedəcəyəm" — لَا أَبْرَحُ ərəbcədədir, mötərizəyə girmir

### Hissə h31 (18:75–19:98)

- 19:21 | üslub | "sənin Rəbbin dedi: ”O, mənə asandır" — Allahın sözüdür, digər yerlərdə "Mənə" (19:9 "Mənim üçün asandır") | "O, Mənə asandır"

### Hissə h33 (21:1–21:112)

- 21:112 | qiraət (kiçik) | "De: Rəbbim! … hökm ver!" — tətbiqdəki Hafs mətnində قَالَ ("O dedi") var; "De" (قُل) başqa qiraətdir. 21:4-də eyni yerdə tərcümə "O, dedi" yazır | "O dedi: ”Rəbbim! …”"
- 21:32 | MƏNA (kiçik) | "onlar isə Bizim ayətlərimizdən üz çevirməkdədirlər" — عَنْ آيَاتِهَا: "onun (səmanın) ayətlərindən" | "onlar isə onun (səmanın) ayətlərindən üz çevirirlər"

### Hissə h34 (22:1–22:78)

- 22:30 | MƏNA (kiçik — hökm) | "(digər) heyvanlar sizə halal edildi" — وَأُحِلَّتْ لَكُمُ الْأَنْعَامُ yalnız ən'am heyvanlarıdır (5:1 ilə eyni məsələ) | "ən'am heyvanları (dəvə, mal-qara, qoyun-keçi) sizə halal edildi"

### Hissə h35 (23:1–24:20)

- 23:116 | MƏNA (kiçik — söz) | "O, əzəmətli Ərşin Rəbbidir" — رَبُّ الْعَرْشِ الْكَرِيمِ ("kərim, şərəfli Ərş"); "əzəmətli" 9:129, 23:86-dakı الْعَظِيمِ-dir | "O, kərim (şərəfli) Ərşin Rəbbidir"

### Hissə h36 (24:21–25:20)

- 24:23 | MƏNA (kiçik — söz) | "onlar üçün ağrılı bir əzab vardır" — ərəbcədə عَذَابٌ عَظِيمٌ ("böyük, əzəmətli"), "ağrılı" أَلِيمٌ-dir | "onlar üçün əzəmətli (böyük) bir əzab vardır"
- 25:17 | qiraət (kiçik) | "O gün Biz onları … toplayacağıq və (Allah onlara belə) deyəcək" — Hafs mətnində يَحْشُرُهُمْ ("O toplayacaq"); "Biz" (نحشرهم) başqa qiraətdir, eyni cümlədə şəxs də dəyişir | "O gün (Allah) onları … toplayacaq və deyəcək"
- 24:63 | üslub | "Qoy Onun (Rəsulullahın) əmrinə müxalifət edənlər" — böyük "O" Peyğəmbərə | "onun (Rəsulullahın)"

### Hissə h37 (25:21–26:110)

- 25:68 | MƏNA (kiçik) | "Kim bunları edərsə, (Qiyamət günündə Allah ilə) günahkar olduğu halda qarşılaşar" — يَلْقَ أَثَامًا: "günahının cəzasına (əsamə) uğrayar"; 25:69 da cəzanı izah edir | "Kim bunları edərsə, (günahının) cəzası ilə qarşılaşar"
- 25:59 | orfoqrafiya | "Ərşə istəva etmişdir" — başqa yerlərdə "istiva" | "istiva etmişdir"
- 25:57 | üslub | "Mən Onun üçün sizdən heç bir əcr … istəmirəm" — عَلَيْهِ təbliğə (risalətə) aiddir, böyük "O" Allahı göstərir | "Mən bunun (bu təbliğin) üçün"
- 25:31 | MƏNA (kiçik) | "Biz hər bir rəsul üçün … düşmən etdik" — لِكُلِّ نَبِيٍّ | "hər bir nəbi üçün"

### Hissə h39 (27:56–28:50)

- 28:9 | MƏNA (kiçik) | "Ola bilsin ki, … onu övladlığa götürək və onlar da bunu bilməsinlər" — وَهُمْ لَا يَشْعُرُونَ qadının sözü deyil, Allahın şərhidir: "onlar (işin sonunu) hiss etmirdilər" | "…onu övladlığa götürək». Onlar isə (bunun sonunu) hiss etmirdilər."
- 27:93 | MƏNA (kiçik — şəxs) | "sənin Rəbbin onların etməkdə olduqlarından qafil deyildir" — Hafs mətnində عَمَّا تَعْمَلُونَ ("sizin etdiklərinizdən") | "…sizin etdiklərinizdən qafil deyildir"

### Hissə h40 (28:51–29:45)

- 28:53 | BURAXILMA (kiçik) | "həqiqətən də, heç şübhə yoxdur ki o, haqdır!" — مِن رَّبِّنَا ("Rəbbimizdən olan") düşüb | "…o, Rəbbimizdən olan haqdır!"

### Hissə h43 (33:31–34:23)

- 33:53 | MƏNA (orta — hökm) | "Sizə yemək verilməsini gözləməyiniz istisna olmaqla, Nəbinin evinə daxil olmayın" — ərəbcədə إِلَّا أَن يُؤْذَنَ لَكُمْ إِلَىٰ طَعَامٍ غَيْرَ نَاظِرِينَ إِنَاهُ: "yeməyə izn verilməsi istisna, (həm də) yeməyin hazır olmasını gözləmədən"; tərcümə "izn" şərtini salır və "gözləməmək"i "gözləmək"ə çevirir. Sonda "bu(nu etməyimizin günahı)" — "etməyinizin" olmalıdır | "Sizə yeməyə izn verilməsi istisna olmaqla, (yeməyin) hazır olmasını gözləmədən Nəbinin evlərinə daxil olmayın…"; "…bu(nu etməyinizin günahı) Allah qatında çox böyükdür"
- 34:6 | üslub | "Rəbbindən sənə nazil edilən şey…, məhz O haqdır" — vəhyə aiddir, böyük "O" Allahı göstərir | "məhz o, haqdır"
- 33:32 | AYDINLIQ (kiçik) | "əgər təqvalı olsanız, siz hər hansı bir (başqa) qadın kimi deyilsiniz" — ərəbcədə ayə dayanacağı ilə şərt sonrakı qadağaya bağlanır: "Siz başqa qadınlar kimi deyilsiniz. Əgər təqvalı olsanız, (yad kişilərlə) incə səslə danışmayın" | yuxarıdakı kimi

### Hissə h46 (37:145–39:31)

- 3:8, 38:9, 38:35 | ad yazılışı | "Vahhəb(sən)" — الْوَهَّاب; düzgün forma "Vəhhab" | "Vəhhabsan", "Vəhhab olan"
- 39:5, 40:42 | ad yazılışı | "Ğaffər" — 38:66, 20:82-də "Ğaffar" | "Ğaffar"
- 39:8 | MƏNA (kiçik — fail) | "Allaha … taylar tutar ki, onlar da onu Onun yolundan azdırarlar" — لِيُضِلَّ عَن سَبِيلِهِ: "(başqalarını) Onun yolundan azdırmaq üçün"; azdıran insanın özüdür | "…taylar tutar ki, (başqalarını) Onun yolundan azdırsın"

### Hissə h47 (39:32–40:40)

- 40:31 | STRUKTUR | "Nuh, Ad və onlardan sonrakı Səmud qövmünün" — ərəbcədə قوم نوح وعاد وثمود والذين من بعدهم: Səmud Ad ilə yanaşı sadalanır, "onlardan sonrakılar" ayrıca dəstədir | "Nuh, Ad, Səmud qövmlərinin və onlardan sonrakıların başına gələnlərin mislindən"
- 39:45 | MƏNA (kiçik) | يستبشرون "bir-birilərini müjdələyərlər" — feilin mənası "sevinərlər, üzləri gülər" | "…o zaman onlar sevinərlər"

### Hissə h48 (40:41–41:46)

- 41:16 | BURAXILMA | في أيام نحسات ("uğursuz/nəhs günlərdə") tərcümədə yoxdur | "…soyuq bir külək göndərdik, uğursuz günlərdə ki, …"
- 40:67 | MƏNA | ولعلكم تعقلون "bəlkə şükr edəsiniz" — feil تعقلون = ağıl edəsiniz, düşünüb dərk edəsiniz | "bəlkə ağıl edəsiniz (dərk edəsiniz)"
- 41:45 | BURAXILMA (kiçik) | منه ("onun — kitab — barəsində") düşüb: "onlar şəkk içində şübhə etməkdədirlər" | "onlar onun barəsində şübhəli bir şəkk içindədirlər"

### Hissə h50 (43:24–45:37)

- 43:86 | MƏNA (fail) | "Ondan başqasına dua edənlər şəfaətə malik deyillər" — الذين يدعون من دونه burada dua **edilənlərdir** (bütlər, ilah sayılanlar); istisna «haqqa şahidlik edən» də onlara aiddir (İsa, mələklər) | "Onların Ondan başqa dua etdikləri (ilahlar) şəfaətə malik deyillər; bildikləri halda haqqa şahidlik edənlər istisnadır"
- 40:46, 45:27 | STRUKTUR (kiçik) | ويوم تقوم الساعة "O gün Saat qopacaqdır" — ərəbcədə zərflik cümləsidir, ayrı xəbər deyil | 40:46 "Saatın qopacağı gün isə (deyiləcək): …"; 45:27 "Saatın qopacağı gün — o gün batil əhli ziyana uğrayacaq"
- 43:81 | qeyd (təfsir seçimi) | إن كان للرحمن ولد inkar kimi verilib ("övladı yoxdur") — İbn Abbas rəyidir, qanunidir; şərt mənası ("olsaydı, ilk ibadət edən mən olardım") daha geniş yayılıb. Düzəliş tələb etmir | düzəliş tələb olunmur; istəsəniz haşiyədə iki mənanı da göstərin

### Hissə h51 (46:1–48:17)

- 47:1, 47:8 | AYDINLIQ | أضل أعمالهم "əməllərini sapdırmışdır" — hərfi, oxucuya anlaşılmır; eyni surədə 47:4 فلن يضل أعمالهم düzgün "boşa çıxarmayacaqdır" verilib | "əməllərini boşa çıxarmışdır (puç etmişdir)"

### Hissə h52 (48:18–51:30)

- 48:25 | MƏNA + BURAXILMA (orta) | "…bilmədən onlara da bir zərər toxundurmuş olmasaydınız" — ərəbcədə ikinci şərt yoxdur: أن تطئوهم "(Məkkədəki tanımadığınız möminləri) əzib-tapdalamağınız (təhlükəsi)" birinci şərtin izahıdır; فتصيبكم منهم معرة بغير علم ("bilmədən onlara görə sizə bir günah/ləkə toxunması") ümumiyyətlə düşüb | "Əgər orada tanımadığınız mömin kişilər və mömin qadınlar — bilmədən onları tapdalayıb, bu səbəbdən sizə bir günah toxunması (təhlükəsi) — olmasaydı, (Allah döyüşə izn verərdi)"
- 48:29 | MƏNA (kiçik) + BURAXILMA | أخرج شطأه "öz toxumunu cücərtmiş" — شطء = zoğ, filiz; فاستغلظ ("yoğunlaşmış") düşüb | "zoğunu çıxarmış, onu qüvvətləndirmiş, (zoğ) yoğunlaşmış və gövdəsi üzərində dikəlmişdir"
- 50:44 | MƏNA (kiçik) | سراعا insanlara aiddir (sürətlə çıxırlar), yerə yox; "(qurtulmaq üçün)" əsassız əlavədir | "O gün yer yarılacaq və onlar (oradan) sürətlə çıxacaqlar"
- 50:14 | qrammatika | "onların hamısı rəsulları(mı) yalanladılar" — sual şəkilçisi yersizdir (كل كذب الرسل xəbər cümləsidir) | "(mı)" silinsin

### Hissə h53 (51:31–54:55)

- 53:13 | MƏNA (kiçik) | نزلة أخرى "(bundan əvvəl bir dəfə də) görmüşdü" — ikinci görüş (Sidrətul-müntəhada, Merac gecəsi) 53:7-dəki üfüq görüşündən **sonradır**; mötərizə xronologiyanı tərsinə çevirir | "o onu başqa bir enişdə də (ikinci dəfə) görmüşdü"
- 54:34 | mötərizəsiz əlavə | "Lutun ailəsindən və ona iman edənlərdən başqa" — "ona iman edənlər" ərəbcədə yoxdur | "(və ona iman edənlərdən)" mötərizəyə alınsın

### Hissə h54 (55:1–57:29)

- 57:10 | MƏNA (kiçik) | من الذين أنفقوا من بعد "özlərindən sonra infaq edib savaşanlardan" — من بعد = **fəthdən** sonra (ayənin əvvəlindəki من قبل الفتح ilə qarşılaşdırma) | "fəthdən sonra infaq edib döyüşənlərdən"
- 57:29 | söz seçimi | وأن الفضل بيد الله "fəzilət tamamən Allahın əlindədir" — فضل burada lütf/bəxşişdir; "fəzilət" Azərbaycan dilində "yaxşı keyfiyyət" deməkdir | "lütf (fəzl) tamamilə Allahın əlindədir"

### Hissə h55 (58:1–61:14)

- 59:21 | kəmiyyət (kiçik) | على جبل "dağlara … onların" — ərəbcədə tək: "bir dağa … onu" | "Əgər Biz bu Quranı bir dağa nazil etsəydik, onun … parça-parça olduğunu görərdin"

### Hissə h56 (62:1–66:12)

- 65:1 | AYDINLIQ (hökm) | فطلقوهن لعدتهن "onları iddət (gözləmə) vaxtlarında boşayın" — oxucu "iddət müddəti ərzində boşayın" kimi başa düşə bilər; mənası "iddətə başlaya biləcəkləri vaxtda (yaxınlıq olmamış təmizlik dövründə)" boşamaqdır | "onları iddətlərinin başlanğıcına görə (iddəti düzgün başlaya biləcəkləri vaxtda) boşayın"

### Hissə h57 (67:1–71:28)

- 69:10 | MƏNA (izafət tərsinə) | فعصوا رسول ربهم "rəsullarının Rəbbinə asi olmuşdular" — ərəbcədə "**Rəblərinin rəsuluna** asi oldular"; asilik rəsula qarşıdır | "…və Rəblərinin rəsuluna asi olmuşdular"

### Hissə h58 (72:1–77:50)

- 73:20 | qiraət | أدنى من ثلثي الليل ونصفه وثلثه — Hafs qiraətində نصفَه وثلثَه nəsbdədir: "gecənin üçdə ikisindən az, (bəzən) yarısını, (bəzən) üçdə birini namaza durursunuz". Tərcümə "yarısından və üçdə birindən daha az" verir — bu, cərr qiraətidir (Əbu Amr və b.), tətbiqdəki Hafs mətni ilə uyğun gəlmir | "…gecənin üçdə ikisindən az, yarısını və üçdə birini namaza durursunuz"
- 72:26 | əlavə | عالم الغيب "qeybdə və aşkarda olanı Biləndir" — "və aşkarda" bu ayədə yoxdur (عالم الغيب والشهادة başqa ayələrdəndir) | "O, qeybi Biləndir və Öz qeybini heç kimə açmır"

### Hissə h59 (78:1–86:17)

- 81:20 | BURAXILMA | ذي قوة ("qüvvət sahibi" — Cəbrailin sifəti) düşüb; tərcümədə yalnız عند ذي العرش مكين var | "…qüvvət sahibi olan, Ərşin Sahibinin yanında uca məqamlı…"
- 78:29–78:30 | STRUKTUR (ayə sərhədi) | فذوقوا 78:30-un əvvəlidir, tərcümədə "(İndi) dadın…" 78:29-un sonuna yazılıb; 78:30 isə "dadın"sız qalıb | 78:30: "Elə isə dadın (o yalanladığınız əzabı)!" — fa (فَذُوقُوا) ərəbcədədir
- 78:32–78:33 | STRUKTUR (ayə sərhədi) | حدائق وأعنابا = 78:32 ("bağçalar və üzüm bağları"); tərcümədə "üzüm ağacları" 78:33-ə keçib, 78:32-də isə əlavə "bağlar" var | 78:32 "bağçalar, üzüm bağları"; 78:33 "dolğun döşlü həmyaşıd qızlar"

### Hissə h60 (87:1–114:6)

- 110:3 | BURAXILMA | فسبح بحمد ربك "Allahı təsbih et" — بحمد ربك ("Rəbbini həmd ilə") düşüb; digər eyni ifadələr (15:98, 50:39, 52:48) düzgün "Rəbbini həmd ilə təsbih et" verilib | "Rəbbini həmd ilə təsbih et və Ondan bağışlanma dilə"
- 90:17 | mötərizəsiz əlavə | "iman edib saleh əməllər işləyən" — "saleh əməllər işləyən" bu ayədə yoxdur | "Sonra da iman edən və bir-birinə səbri və mərhəməti tövsiyə edənlərdən olmaqdır"
- 91:12 | MƏNA (kiçik) | إذ انبعث أشقاها "onlar özlərinin ən bədbəxtini göndərmişdilər" — feil təsirsizdir: "ən bədbəxtləri (qalxıb) atıldığı zaman" | "Onların ən bədbəxti (dəvəni öldürmək üçün) qalxıb atıldığı zaman"

Yeni tapıntı çıxmayan hissələr: h08, h09, h14, h20, h24, h32, h38, h41, h42, h44, h45, h49.

## 5. Müəllifin öz mötərizələri — iki oxunuş yoxlaması (2026-10-14)

Üsul §5-dəki qayda (mətn həm mötərizə ilə, həm də mötərizəsiz düzgün cümlə olmalıdır) burada **müəllifin mövcud mötərizələrinə** tətbiq olundu — §3/§4 və `check-parens` yalnız təkliflərin artırdıqlarına baxmışdı. Mötərizəsi olan **4785 ayənin hamısı** (11 030 mötərizə) mötərizəsiz oxunuşda oxundu, şübhəli ~140 yer `quranapp.db`-dəki Osmani ərəbcə ilə tutuşduruldu. Formal izlər skriptlədir: `python3 tools/quran-audit/author_parens.py <az.json> [--list] [--sentences]`.

**Nəticə: 153 ayədə mötərizəsiz oxunuş qırılır** (6236-dan). Bunlardan **79-i ayrı-ayrı düzəliş istəyir**: 32-sində məna dəyişir və ya ərəbcədə olan söz mötərizəyə düşüb (5.1), 36-sında cümlə qrammatik olaraq dağılır (5.2), 11-i formaldır — ahəng, dırnaq, durğu (5.3). Qalanları bir qayda ilə düzələn toplu izlərdir (5.4): 45 ayədə yetim iki nöqtə, 24 ayədə qalıq durğu, 7 ayədə asılı "ki".

| İz | Say | Qeyd |
|---|---|---|
| Kiçik hərflə başlama (`(Bilin ki,) kim…`) | 562 ayə | **Müəllif üslubudur, xəta sayılmadı** |
| Ərəbcədə olan söz mötərizədə — fa (فَ) → "(Elə isə)/(O zaman)…" | 121 | cümləni qırmır; yalnız qırdığı yerlər 5.1-dədir |
| Ərəbcədə olan söz — الذي/من → "(o … ki)" | 118 | eyni |
| Ərəbcədə olan söz — feildəki əvəzlik "(siz)/(onlar)…" | 24 | eyni |
| Sözə yapışıq mötərizə (`olan(qadın)larla`) | 435 (387 ayə) | çoxu zərərsizdir; qıranlar (`mal(ları)nız` → "malnız") 5.2-dədir |
| Yetim iki nöqtə (danışma feili mötərizədə, `:` kənarda) | 45 ayə | 5.4 |
| Qalıq durğu (`,,` `,.` `,?` `!!`) | 24 ayə | 5.4 |
| Ahəng (`olanlar(la evlənmək) də` → "olanlar də") | 7 ayə | 5.3 |
| Asılı "ki" (`rəsuluyam, (O Allahın) ki,`) | 7 ayə | 5.4; "ki"/ل- mötərizədə qalan digər yerlər əl ilə tapılıb (5.2) |
| Balanssız mötərizə | 7 ayə | §3/§4-də artıq var (4:85, 4:128, 8:73, 23:89, 46:17, 46:20, 77:43) |

**2026-10-14: bu bölmənin hamısı (5.1–5.5, toplu izlər daxil — 157 ayə) `quran_edits`-ə gözləyən təklif kimi yazıldı** (`tools/quran-audit/author_fixes.json`; 61 mövcud təklif yeniləndi, 96 yeni). Yoxlama zamanı daha dörd yer tapıldı: 23:88 və 53:38 (yetim iki nöqtə / qalıq durğu), 34:27 («Xeyr!!»), 27:71 (təklifin özündə artıq «(»).

Qeyd: ⏳ — həmin ayəyə gözləyən audit təklifi var (`tools/quran-audit/out/edits.json`); yanında təklifin bu qırılmanı düzəldib-düzəltmədiyi yazılıb. Təkliflər hər iki oxunuşda yoxlanıb, son variant üçün §1-dəki xəbərdarlıq qüvvədədir.

### 5.1. Məna dəyişir və ya ərəbcədə olan söz mötərizədədir

- 5:6 | MÖTƏRİZƏ (iki oxunuş) | **hökm** — "başlarınıza məsh edin və topuqlara qədər ayaqlarınızı da (yuyun)": mötərizəsiz "ayaqlarınızı da" əvvəlki "məsh edin"ə bağlanır. Hafs qiraətində وَأَرْجُلَكُمْ nəsbdədir, فَاغْسِلُوا-ya bağlıdır — "yuyun" ərəbcədədir, mötərizəyə girmir | "…başlarınıza məsh edin, topuqlara qədər ayaqlarınızı da yuyun."
- 16:69 | MÖTƏRİZƏ (iki oxunuş) | "qarınlarından müxtəlif rənglərdə olan şərab (bal) çıxır" — mötərizəsiz arılar "şərab" çıxarır; شَرَابٌ "içki" deməkdir, azərbaycanca "şərab" isə şərabdır | "…müxtəlif rənglərdə olan bir içki (bal) çıxır"
- 12:25–27 | MÖTƏRİZƏ (iki oxunuş) | "o (qadın) doğru söyləyib və o (Yusuf) yalançılardandır" — mötərizəsiz iki "o" ayırd edilmir, şahidin sözü mənasız qalır. Ərəbcədə fərq feilin cinsindədir (فَصَدَقَتْ / وَهُوَ, قَدَّتْ, سَيِّدَهَا), yəni "qadın" sözü ərəbcədəki məlumatdır | 12:25 "qadın onun köynəyini arxadan cırdı … qadının əfəndisi (əri) ilə qarşılaşdılar"; 12:26 "demək ki, qadın doğru söyləyib, o (Yusuf) isə yalançılardandır"; 12:27 "…qadın yalan söyləyib, o (Yusuf) isə sadiqlərdəndir"
- 4:1 | MÖTƏRİZƏ (iki oxunuş) | "(Adı ilə) bir-birinizdən (ehtiyacınız olanları) istədiyiniz Allahdan" → mötərizəsiz "bir-birinizdən istədiyiniz Allahdan" — Allah istənilən şey olur. تَسَاءَلُونَ بِهِ — بِهِ ərəbcədədir | "Adı ilə bir-birinizdən (ehtiyacınız olanları) istədiyiniz Allahdan…qorxun"
- 38:73 | MÖTƏRİZƏ (iki oxunuş) | "dərhal ona (səcdə) etdilər" → "ona etdilər"; فَسَجَدَ ərəbcədədir. ⏳ (təklif düzəldir — §3) | "…dərhal (ona) səcdə etdilər"
- 46:19 | MÖTƏRİZƏ (iki oxunuş) | "Onlara əsla (zülm) olunmayacaqdır" → "əsla olunmayacaqdır"; لَا يُظْلَمُونَ — zülm feilin özüdür | "Onlara əsla zülm olunmayacaqdır."
- 45:4 | MÖTƏRİZƏ (iki oxunuş) | cümlənin xəbəri "(ayətlər vardır)" mötərizədədir, mötərizəsiz ayə xəbərsiz qalır; آيَاتٌ ərəbcədədir | "…yəqinliklə iman edən bir qövm üçün ayətlər vardır."
- 6:142 | MÖTƏRİZƏ (iki oxunuş) | xəbər "yaradan da Odur" alternativ izahla birlikdə mötərizəyə düşüb; mötərizəsiz "Heyvanlardan yük daşıyanları və üstünə minilənləri!" — feilsiz | "Heyvanlardan yük daşıyanları və üstünə minilənləri (və yaxud yunundan, qəzilindən döşəklər və s. kimi şeylər düzəldilənləri) yaradan da Odur!"
- 7:43 | MÖTƏRİZƏ (iki oxunuş) | "Onların ayaqları altından (çaylar) axacaq" — mübtəda mötərizədədir; الْأَنْهَارُ ərəbcədədir. ⏳ (təklifdə qalır) | "Onların ayaqları altından çaylar axacaq."
- 3:7 | MÖTƏRİZƏ (iki oxunuş) | "fitnə(-fəsad törətmək və ayətləri istədikləri kimi) təvil etmək üçün" → "fitnə təvil etmək üçün". ابْتِغَاءَ الْفِتْنَةِ وَابْتِغَاءَ تَأْوِيلِهِ — "axtarmaq" və "və" ərəbcədədir. ⏳ (təklifdə qalır) | "fitnə(-fəsad) axtarmaq və onu (ayətləri istədikləri kimi) təvil etmək üçün"
- 3:188 | MÖTƏRİZƏ (iki oxunuş) | "(Etdikləri əməllərdən) fərəhlənən" — بِمَا أَتَوا ərəbcədədir. ⏳ (təklifdə qalır) | "Etdikləri əməllərlə fərəhlənən…"
- 43:33 | MÖTƏRİZƏ (iki oxunuş) | "Əgər insanlar (…küfrə üz tutan) vahid bir ümmət olmasaydılar" — mötərizəsiz "insanlar bir ümmət olmasaydı" deyir; لَوْلَا أَن يَكُونَ النَّاسُ أُمَّةً وَاحِدَةً — "insanların (küfrdə) bir ümmət olacağı olmasaydı" | "Əgər bütün insanlar (…küfrə üz tutaraq) vahid bir ümmət olmayacaq olsaydılar…"
- 43:38 | MÖTƏRİZƏ (iki oxunuş) | "Vay mənim halıma! (Kaş ki,) …olaydı!" — ərəbcədə يَا لَيْتَ ("kaş ki") var, "vay halıma" yoxdur: mötərizə tərsinə qoyulub | "(Vay mənim halıma!) Kaş ki, mənimlə sənin aranda iki məşriq … uzaqlığı olaydı!"
- 38:33 | MÖTƏRİZƏ (iki oxunuş) | "o, (əllərini atların) ayaqlarına və boyunlarına çəkməyə başladı" → "ayaqlarına … çəkməyə başladı"; مَسْحًا ("sığallamaq, əl çəkmək") ərəbcədədir | "o, (atların) ayaqlarını və boyunlarını sığallamağa (əl çəkməyə) başladı"
- 38:32 | MÖTƏRİZƏ (iki oxunuş) | "Rəbbimin zikrindən (məhrum) oldum" → "zikrindən oldum"; عَن ذِكْرِ رَبِّي — "-dan uzaq" ərəbcədədir | "…Rəbbimin zikrindən uzaq düşdüm (məhrum oldum)"
- 27:60 | MÖTƏRİZƏ (iki oxunuş) | "gözəl görünüşlü bağlarda (müxtəlif ağaclar, bitkilər) bitirdik" — mötərizəsiz feilin tamamlığı yoxdur; ərəbcədə bitirilən elə bağlardır (حَدَائِقَ ذَاتَ بَهْجَةٍ) | "Biz onunla gözəl görünüşlü bağlar bitirdik"
- 35:18 | MÖTƏRİZƏ (iki oxunuş) | "Əgər ağır yük (sahibi olan kimsə) onu daşımaq üçün (birini) çağırsa" → yük özü çağırır; مُثْقَلَةٌ "ağır yüklü (nəfs)" | "Əgər ağır yüklü (günah yükü ağır olan) bir kimsə onu daşımaq üçün (birini) çağırsa"
- 9:105 | MÖTƏRİZƏ (iki oxunuş) | "möminlər də (…görəcəkdir və sonra sizlər) qeybi … Bilənə doğru qaytarılacaqsınız" → mötərizəsiz möminlər qaytarılır; وَسَتُرَدُّونَ — "və siz" ərəbcədədir | "…Onun Rəsulu da, möminlər də (nələr etdiyinizi görəcəkdir). Sonra siz qeybi və aşkarda olanı Bilənə doğru qaytarılacaqsınız"
- 28:12 | MÖTƏRİZƏ (iki oxunuş) | "haram etdik (Musa … əmmədi və bu zaman bacısı onlara belə) dedi" → "haram etdik dedi" — danışan da, cümlə sərhədi də itir; فَقَالَتْ qadın feilidir | "…haram etdik. (Musa heç bir süd anasını əmmədi və bu zaman) bacısı (onlara belə) dedi:"
- 23:110 | MÖTƏRİZƏ (iki oxunuş) | "sonda (bu əməlləriniz) sizə Mənim zikrimi unutdurdu" — mübtəda mötərizədədir və ərəbcədən fərqlidir: أَنسَوْكُمْ — "onlar unutdurdular" | "…nəhayət onlar (onlarla məşğul olmağınız) sizə Mənim zikrimi unutdurdular"
- 20:86 | MÖTƏRİZƏ (iki oxunuş) | "Yoxsa (mənim yanınızda olmadığım vaxt çox) uzun (bir müddət) oldu?" → "Yoxsa uzun oldu?"; عَلَيْكُمُ الْعَهْدُ ərəbcədədir | "Yoxsa (mənim yanınızda olmadığım) müddət sizə uzun gəldi?"
- 28:61 | MÖTƏRİZƏ (iki oxunuş) | "vəd etdiyimiz (və) ona … qovuşacaq kimsə" → "vəd etdiyimiz ona qovuşacaq"; فَهُوَ لَاقِيهِ — bağlayıcı ərəbcədədir | "Bizim gözəl bir vəd (Cənnət) vəd etdiyimiz və ona qovuşacaq kimsə…"
- 22:11 | MÖTƏRİZƏ (iki oxunuş) | "(onun) dünya(sı da,) axirət(i də) xusrana uğramışdır" → "dünya axirət xusrana uğramışdır"; وَالْآخِرَةَ — "və" ərəbcədədir | "onun dünyası da, axirəti də xusrana uğramışdır"
- 92:5 | MÖTƏRİZƏ (iki oxunuş) | "verən (və Onun əzabından) qorxan" → "verən qorxan"; وَاتَّقَىٰ — "və" ərəbcədədir | "(…Allah yolunda) verən və (Onun əzabından) qorxan…"
- 6:100 | MÖTƏRİZƏ (iki oxunuş) | "(halbuki, Allah) onları xəlq etmişdir" → mötərizəsiz feil mübtədasız qalır; وَخَلَقَهُمْ — hal "və"-si ərəbcədədir | "…şərik qoşdular, halbuki, (Allah) onları xəlq etmişdir"
- 38:6 | MÖTƏRİZƏ (iki oxunuş) | "Şübhəsiz ki, (bu, bizdən) istənən şeydir" — هَٰذَا ərəbcədədir | "Şübhəsiz ki, bu, (bizdən) istənən şeydir!"
- 17:62 | MÖTƏRİZƏ (iki oxunuş) | "Bu məndən üstün etdiyin (Adəm) barədə" → "Bu məndən üstün etdiyin barədə" — təyin olunan söz mötərizədədir; هَٰذَا الَّذِي كَرَّمْتَ | "Məndən üstün etdiyin bu kimsə (Adəm) barədə mənə xəbər ver."
- 9:35 | MÖTƏRİZƏ (iki oxunuş) | "O gün o (yığdıqları sərvət) Cəhənnəm atəşinin…" → "o Cəhənnəm" — əvəzlik işarə sözünə çevrilir, mübtəda itir. ⏳ (təklif iki nöqtəni düzəldir, bu qalır) | "O gün o, (yığdıqları sərvət,) Cəhənnəm atəşinin üstündə qızdırılacaq…"
- 11:83 | MÖTƏRİZƏ (iki oxunuş) | "O (əzab, … daşlar) zalımlardan uzaq deyildir" → "O zalımlardan" — eyni iz | "O, (əzab, həlak üçün yağdırılan daşlar,) zalımlardan (heç də) uzaq deyildir!"
- 11:8 | MÖTƏRİZƏ (iki oxunuş) | "O gün (əzab) onlara gələcək" → mübtəda "gün" olur; يَوْمَ يَأْتِيهِمْ — "gəldiyi gün" | "(Əzab) onlara gələcəyi gün o, onlardan geri çevrilməyəcək (dəf edilməyəcək)…"

### 5.2. Qrammatika — mötərizəsiz cümlə dağılır

- 4:165 | MÖTƏRİZƏ (iki oxunuş) | "(Biz) … rəsullar (göndərdik ki,) insanların … olmasın" → "rəsullar insanların … olmasın"; لِئَلَّا — "ki" ərəbcədədir | "(Biz) müjdə verən və qorxudan rəsullar (göndərdik) ki, insanların rəsullardan sonra Allaha qarşı bir hüccəti olmasın"
- 25:32 | MÖTƏRİZƏ (iki oxunuş) | "Beləcə Biz (onu hissə-hissə nazil edirik ki,) onunla … səbat verək" → "ki"siz arzu feli; لِنُثَبِّتَ | "Beləcə (onu hissə-hissə nazil edirik) ki, Biz onunla sənin qəlbinə səbat verək"
- 34:21 | MÖTƏRİZƏ (iki oxunuş) | "Biz yalnız (ona görə … sınadıq ki,) … bilək" → "Biz yalnız … bilək"; إِلَّا لِنَعْلَمَ | "…sultanı yox idi; (bu,) yalnız ona görə idi ki, axirətə iman edənlərlə … olanları bilək (ayırd edək)"
- 57:25 | MÖTƏRİZƏ (iki oxunuş) | "(Bu ona görədir ki,) Allah … bilsin" → "ki"siz; وَلِيَعْلَمَ | "(Bu,) həm də Allahın gizlində Ona və Onun rəsullarına yardım edənləri bilməsi üçündür"
- 22:9 | MÖTƏRİZƏ (iki oxunuş) | "döndərir (özünü bəyənərək bunları edir ki, insanları) Allahın yolundan sapdırsın" → "döndərir Allahın yolundan sapdırsın"; لِيُضِلَّ | "…o yana bu yana döndərir ki, (insanları) Allahın yolundan sapdırsın"
- 27:86 | MÖTƏRİZƏ (iki oxunuş) | "gecəni (onlar üçün) sükunət tapsınlar (dincəlib istirahət etsinlər deyə)" → "gecəni sükunət tapsınlar"; لِيَسْكُنُوا فِيهِ | "Biz gecəni onda sükunət tapsınlar deyə (dincəlib istirahət etsinlər deyə)…"
- 17:7 | MÖTƏRİZƏ (iki oxunuş) | "Sonuncu dəfənin vaxtı gəldikdə (isə, üzərinizə düşmənləri musallat etdik) ki, …" → "gəldikdə ki," (ərəbcədə də cavab ellipsisdir, لِيَسُوءُوا) | "Sonuncu dəfənin vaxtı gəldikdə (isə, düşmənləri yenə üzərinizə göndərdik) — üzlərinizi hüznlü, kədərli etsinlər, … məhv etsinlər deyə"
- 28:46 | MÖTƏRİZƏ (iki oxunuş) | "…bir rəhmət olaraq (bu xəbərləri sənə bildirdik) ki, bəlkə" → "rəhmət olaraq ki". ⏳ (təklif yalnız "Tur"u düzəldir, bu qalır) | "…lakin (bu xəbərlər) sənin Rəbbindən bir rəhmətdir ki, səndən öncə … gəlməmiş bir qövmü qorxudasan; bəlkə ibrət alarlar"
- 21:89 | MÖTƏRİZƏ (iki oxunuş) | "Zəkəriyyanı da (zikr et), ki, bir zaman" → "Zəkəriyyanı da, ki,"; إِذْ "ki" deyil, "o zaman ki". ⏳ (təklif vergülü silir, asılı "ki" qalır) | "(Kitabda) Zəkəriyyanı da (zikr et)! Bir zaman o, Rəbbinə (belə) nida etmişdi: …"
- 2:178 | MÖTƏRİZƏ (iki oxunuş) | "köləyə görə köləni və qadına görə də qadın(ı qisas olaraq öldürün)" → "qadına görə də qadın!" — hal şəkilçisi də, feil də mötərizədə. Ərəbcədə ismi cümlədir (الْحُرُّ بِالْحُرِّ) | "Azad kəsə görə azad olan, köləyə görə kölə, qadına görə də qadın (qisas olaraq öldürülür)!"
- 2:126 | MÖTƏRİZƏ (iki oxunuş) | "kafir olan kimsələri də (ruziləndirəcəyəm). Onları azacıq…" → "kafir olan kimsələri də." — feilsiz cümlə; وَمَن كَفَرَ فَأُمَتِّعُهُ | "…kafir olan kimsələri də (ruziləndirəcəyəm), onları azacıq faydalandıracağam…"
- 3:152 | MÖTƏRİZƏ (iki oxunuş) | "vədinə sadiq oldu (… yerinə yetirdi, sonra da) O, sizə … göstərdikdən sonra zəiflədiniz" → iki cümlə bitişir; حَتَّىٰ إِذَا ərəbcədədir | "…Allah (sizə olan) vədinə sadiq oldu. Nəhayət, O, sizə xoşladığınız şeyi (dünya malını) göstərdikdən sonra (dünyaya meyl edərək) zəiflədiniz…"
- 4:6 | MÖTƏRİZƏ (iki oxunuş) | "onlar (yetimlər) böyüyənə (həddi-buluğa…) çatana (…) kimi (xərcləməyə) tələsərək" → "böyüyənə çatana kimi"; بِدَارًا أَن يَكْبَرُوا — "böyüməmişdən əvvəl tələsərək". ⏳ (təklifdə qalır) | "Onları (yetimlərin mallarını) israf edərək və onlar böyüməmişdən əvvəl (mallarını) tələsərək yeməyin."
- 9:118 | MÖTƏRİZƏ (iki oxunuş) | "o üç nəfərin də (tövbələrini qəbul etdi)." → "o üç nəfərin də." — yiyəlik hal sahibsiz qalır | "(Allah) sonraya saxlanan o üç nəfərə də (tövbə nəsib etdi)." — وَعَلَى الثَّلَاثَةِ
- 13:5 | MÖTƏRİZƏ (iki oxunuş) | "torpaq olduqdan (sonra) yeni bir xəlq etməmi (olacaq)?!" → "torpaq olduqdan yeni bir xəlq etməmi?!"; أَءِنَّا لَفِي خَلْقٍ جَدِيدٍ. ⏳ (təklifdə qalır) | "”Biz (öldükdən sonra çürüyüb) torpaq olduqdan sonra yeni bir xəlqdəmi olacağıq?!”"
- 17:101, 17:102 | MÖTƏRİZƏ (iki oxunuş) | "mən sənin sehrlənmiş biri (olduğunu) zənn edirəm" → "sənin … biri zənn edirəm" (eyni 17:102 "məhv olmuş"). ⏳ (təklifdə qalır) | "mən səni sehrlənmiş biri zənn edirəm"; "mən də səni məhv olmuş biri zənn edirəm"
- 17:110 | MÖTƏRİZƏ (iki oxunuş) | "İstər Allah (deyib) dua edin, istərsə də ər-Rahmən (deyib) dua edin" → "Allah dua edin". ⏳ (təklifdə qalır) | "İstər Allaha dua edin, istərsə də ər-Rahmənə dua edin" (ادْعُوا اللَّهَ)
- 20:15 | MÖTƏRİZƏ (iki oxunuş) | "səy göstərdiyi (çalışdığı şeyə, etdiyi əməlinə) görə" → "səy göstərdiyi görə" | "səy göstərdiyinə (çalışdığı şeyə, etdiyi əməlinə) görə"
- 31:28 | MÖTƏRİZƏ (iki oxunuş) | "Sizin (hamınızın) yaradılması da, dirildilməsi də" → "Sizin yaradılması" | "Sizin (hamınızın) yaradılmanız da, dirildilməniz də…"
- 8:28 | MÖTƏRİZƏ (iki oxunuş) | "mal(ları)nız" → "malnız" | "mallarınız" (أَمْوَالُكُمْ cəmdir, mötərizə lazım deyil)
- 21:73 | MÖTƏRİZƏ (iki oxunuş) | "yönəldən (hidayət) imamları etdik" → "imamları etdik" | "doğru yola yönəldən imamlar (hidayət imamları) etdik"
- 38:54 | MÖTƏRİZƏ (iki oxunuş) | "Bizim (sizin üçün hazırladığımız) ruzidir" → "Bizim ruzidir"; لَرِزْقُنَا | "bu, Bizim ruzimizdir (sizin üçün hazırladığımız nemətdir)"
- 40:40 | MÖTƏRİZƏ (iki oxunuş) | "Kim bir pis (əməl) etsə" → "bir pis etsə" | "Kim bir pislik etsə" (سَيِّئَةً)
- 7:52 | MÖTƏRİZƏ (iki oxunuş) | "elm ilə (təfsilatlı) şəkildə açıqladığımız" → "elm ilə şəkildə"; فَصَّلْنَاهُ ərəbcədədir | "elm üzrə təfsilatlı şəkildə açıqladığımız"
- 7:142 | MÖTƏRİZƏ (iki oxunuş) | "Rəbbinin (təyin etdiyi) vaxt (müddət)" → "Rəbbinin vaxt" — mötərizəli oxunuş "-ı" istəmir, mötərizəsiz istəyir | "Rəbbinin vədə vaxtı (onunla görüş müddəti) qırx gecədə tamamlandı" (مِيقَاتُ رَبِّهِ)
- 54:12 | MÖTƏRİZƏ (iki oxunuş) | "Beləcə (həm səmanın, həm də yerin) suları" → "Beləcə suları" | "Beləcə sular (həm səmanın, həm də yerin suları) … bir-birinə qarışdı" (فَالْتَقَى الْمَاءُ)
- 20:130 | MÖTƏRİZƏ (iki oxunuş) | "təsbih et (namaz) qıl ki" → "təsbih et qıl ki". ⏳ (təklif düzəldir — §3) | "…təsbih et (namaz qıl) ki"
- 46:29 | MÖTƏRİZƏ (iki oxunuş) | '”Susun (və dinləyin!” Quranın oxunması) bitdikdə(n sonra isə)' — bağlayan dırnaq mötərizənin içindədir. ⏳ (təklif düzəldir) | '”Susun (və dinləyin)!” (Quranın oxunması) bitdikdə isə…'
- 53:42 | MÖTƏRİZƏ (iki oxunuş) | "(Ona xəbər verilmədimi ki, hər şeyin) bitib durduğu yeri (yalnız) sənin Rəbbinədir (?)" — hər iki oxunuşda "yeri" yanlışdır. ⏳ (təklif düzəldir) | "son dönüş yalnız sənin Rəbbinədir" (وَأَنَّ إِلَىٰ رَبِّكَ الْمُنتَهَىٰ)
- 25:68 | MÖTƏRİZƏ (iki oxunuş) | "(Qiyamət günündə Allah ilə) günahkar olduğu halda qarşılaşar" → "qarşılaşar"ın tamamlığı yoxdur; يَلْقَ أَثَامًا. ⏳ (təklif düzəldir) | "…günahının cəzası ilə qarşılaşar"
- 49:9 | MÖTƏRİZƏ (iki oxunuş) | "Əgər (o tayfa Allahın əmrinə) gəlsə" → "Əgər gəlsə"; فَإِن فَاءَتْ "qayıtsa" | "Əgər (o tayfa Allahın əmrinə) qayıtsa"
- 51:45 | MÖTƏRİZƏ (iki oxunuş) | "Onlar (qorxudan … quruyub qaldılar) və heç ayağa da qalxa bilmədilər" → "Onlar və heç ayağa" | "Onlar (qorxudan, dəhşətdən yerlərində quruyub qaldılar,) heç ayağa da qalxa bilmədilər…"
- 36:38 | MÖTƏRİZƏ (iki oxunuş) | "Günəş də (Bizim ayətlərimizdəndir;) o, təyin olunmuş yerə qədər gedir" → "Günəş də o, …" | "(Həmçinin) Günəş də (Bizim ayətlərimizdəndir; o,) təyin olunmuş yerə qədər gedir."
- 38:59 | MÖTƏRİZƏ (iki oxunuş) | '”Bu(nlar), dünyada haqq yoldan azdırdığınız və bu gün sizinlə birlikdə (Cəhənnəmə girən çox sayda) insanlardır!”' → "…azdırdığınız və bu gün sizinlə birlikdə insanlardır" — feil mötərizədədir, "dünyada azdırdığınız" isə ərəbcədə olmadığı halda mötərizəsizdir (هَٰذَا فَوْجٌ مُّقْتَحِمٌ مَّعَكُمْ); iki yetim iki nöqtə. ⏳ (təklifdə qalır) | '(Kafirləri azdıranlara deyiləcək:) ”Bu, (dünyada haqq yoldan azdırdığınız) sizinlə birlikdə (Cəhənnəmə) soxulan bir dəstədir!” (Onlar deyəcəklər:) ”Onlara salam olmasın!…”'
- 6:86 | MÖTƏRİZƏ (iki oxunuş) | "İsmayıl, Yəsə’, Yunus və Lutu da (an)!" — mötərizəsiz hal uyğunsuzluğu (üçü adlıq, biri təsirlik) | "İsmayılı, Yəsə’ni, Yunusu və Lutu da (an)!"

### 5.3. Formal — ahəng, dırnaq, durğu

- 4:24 | MÖTƏRİZƏ (iki oxunuş) | "qadınlardan evli olanlar(la evlənmək) də (sizə haram edilmişdir)" → "olanlar də" (ahəng) | "qadınlardan evli olanlar da (sizə haram edilmişdir)"
- 2:275 | MÖTƏRİZƏ (iki oxunuş) | "satış (ticarət, alış-veriş) də" → "satış də". ⏳ (təklif yalnız böyük hərfi düzəldir) | "satış da (ticarət, alış-veriş də)"
- 4:36 | MÖTƏRİZƏ (iki oxunuş) | "sahib olduqlarına (kölə və xidmətçilərinizə) də" → "olduqlarına də". ⏳ (təklifdə qalır) | "sahib olduqlarına da (kölə və xidmətçilərinizə də)"
- 21:9 | MÖTƏRİZƏ (iki oxunuş) | "aşanları (…aşırıya gedənləri) də" → "aşanları də". ⏳ (təklifdə qalır) | "aşanları da (israf edənləri, aşırıya gedənləri də)"
- 23:80 | MÖTƏRİZƏ (iki oxunuş) | "ixtilafı (bir-birini əvəz etməsi) də" → "ixtilafı də" | "ixtilafı da (bir-birini əvəz etməsi də)"
- 76:14 | MÖTƏRİZƏ (iki oxunuş) | "salxımları (meyvələri) də" → "salxımları də" | "salxımları da (meyvələri də)"
- 17:75 | MÖTƏRİZƏ (iki oxunuş) | "həyatı(n əzabını) da …, ölümü(n əzabını) da" → "ölümü da" (ahəng), "həyatı da qat-qat daddırardıq" | "həyatda da qat-qat, ölümdə (öldükdən sonra) da qat-qat (əzab) daddırardıq" (ضِعْفَ الْحَيَاةِ وَضِعْفَ الْمَمَاتِ)
- 4:171 | MÖTƏRİZƏ (iki oxunuş) | 'və: (“Allah”) üçdür!” deməyin' — açan dırnaq mötərizənin içindədir. ⏳ (təklif düzəldir) | 'və: “(Allah) üçdür!” deməyin'
- 21:56 | MÖTƏRİZƏ (iki oxunuş) | '”(Xeyr), əksinə' → '”, əksinə' | '”(Xeyr,) əksinə'
- 12:64, 26:15 | MÖTƏRİZƏ (iki oxunuş) | bütöv mötərizəli cümlə nöqtəni kənarda qoyur: "edərəmmi? (Halbuki…)." → "?."; "Xeyr! (…)!" → "Xeyr!!" | durğunu mötərizənin içinə al: "(Halbuki, siz onu qorumamışdınız.)", "(Qorxma, səni əsla öldürə bilməzlər!)"

### 5.4. Toplu formal izlər (qayda ilə düzəlir)

- **Yetim iki nöqtə — 45 ayə.** Danışma feili mötərizədədir, iki nöqtə kənarda: "bilmişdilər. (Biz onlara belə dedik) : ”…”" → mötərizəsiz "bilmişdilər. : ”…”". Qayda: iki nöqtə mötərizənin **içinə** — "(Biz onlara belə dedik:) ”…”" (77:43-dəki təklif kimi; 46:17/46:20 təklifləri iki nöqtəni kənarda saxlayır, amma orada ondan əvvəl feil olduğu üçün qırılmır). Ayələr: 2:60, 2:132, 3:49, 3:73, 3:191, 3:193, 6:72, 7:49, 7:171, 10:91, 11:76, 11:80, 12:46, 12:72, 15:46, 17:14, 19:36, 19:64, 19:71, 22:10, 25:14, 25:19, 26:54, 29:2, 36:59, 37:164, 38:42, 38:59, 40:16, 44:12, 44:18, 44:23, 45:31, 48:11, 50:24, 51:28, 51:50, 52:14, 57:12, 69:24, 69:30, 71:24, 75:6, 76:22, 77:12. 37-sinə gözləyən təklif var, amma mexaniki keçid yalnız `) :` boşluğunu silib — iki nöqtə hələ də kənardadır.
- **Qalıq durğu — 24 ayə.** Mötərizədən əvvəl qoyulmuş vergül mötərizəsiz oxunuşda qoşalaşır ("Raufdur, (…), Rahimdir" → "Raufdur,, Rahimdir"; "istəməkdədirlər. (Lakin), kafirlər" → "., kafirlər"). Qayda: vergül ya kənarda bir dəfə, ya da müəllifin `(, …)` yazılışı. Ayələr: 2:170, 4:25, 4:70, 4:82, 7:42, 7:142, 9:28, 9:32, 10:50, 12:35, 16:7, 16:52, 20:22, 26:42, 31:29, 36:45, 37:75, 40:56, 49:2, 67:13, 68:41, 89:17, 96:6, 110:2. 4:25 ayrıca: "qoy … möminə gənc qadınlarla (evlənsin)" — **əsas feil mötərizədədir**, mötərizəsiz cümlə feilsiz qalır → "qoy … möminə gənc qadınlarla evlənsin (nikahlansın)".
- **Asılı "ki" — 7 ayə (skript) + 21:89, 17:7, 28:46 (5.2).**
  - 7:158 | MÖTƏRİZƏ (iki oxunuş) | "rəsuluyam, (O Allahın) ki," → "rəsuluyam, ki,"; الَّذِي ərəbcədədir | "…Allahın rəsuluyam — o Allahın ki, səmaların və yerin mülkü Ona məxsusdur…"; sonda "Ona ittiba edin (Onun ardınca gedin) ki, bəlkə…"
  - 11:2, 11:3 | MÖTƏRİZƏ (iki oxunuş) | "(O kitabda mənə vəhy edilir) ki, Allahdan başqasına…" → ayə "ki" ilə başlayır | "(O kitabda mənə vəhy edilir:) Allahdan başqasına ibadət etməyin…"; 11:3 "(Eləcə də mənə vəhy olunur:) Rəbbinizdən bağışlanma diləyin…" (أَلَّا / وَأَنِ izahedici "ən"dir)
  - 22:53 | MÖTƏRİZƏ (iki oxunuş) | "(Allah bunu belə edir) ki, şeytanın…" → ayə "ki" ilə başlayır | "(Allah bunu belə edir ki,) şeytanın gizlicə vəsvəsə etdiyi şeyləri … bir fitnə etsin deyə." (لِيَجْعَلَ)
  - 58:10 | MÖTƏRİZƏ (iki oxunuş) | "şeytandandır, (o belə edir) ki, (onunla) iman edənləri hüznləndirsin" → "şeytandandır, ki," | "…yalnız şeytandandır — (onunla) iman edənləri hüznləndirsin deyə" (لِيَحْزُنَ)
  - 6:51 | MÖTƏRİZƏ (iki oxunuş) | "xəbərdar et, (qoy bilsinlər) ki, onların…" → "xəbərdar et, ki," | "…xəbərdar et — (qoy bilsinlər ki,) onların Ondan başqa nə bir vəlisi … vardır"
  - 44:13 | MÖTƏRİZƏ (iki oxunuş) | "olsun ki (bu)? (Halbuki, bundan əvvəl) onlara…" → "olsun ki? onlara…"; وَقَدْ — "halbuki" ərəbcədədir | "Onlar üçün hardan, necə bir xatırlatma olsun (bu)? Halbuki, (bundan əvvəl) onlara … bir rəsul gəlmişdi"

### 5.5. Yolda görülən — mötərizədən asılı deyil (§3/§4-də yoxdur)

- 52:48–52:49 | STRUKTUR (ayə sərhədi) | 52:48-in sonu وَسَبِّحْ بِحَمْدِ رَبِّكَ حِينَ تَقُومُ-dur, tərcümədə "Rəbbini həmd ilə təsbih et" 52:49-un sonuna keçib, 52:48 "qalxdığın zaman,…" ilə bitir (110:3 qeydində 52:48 düzgün nümunə sayılıb — düz deyil) | 52:48 "…Qalxdığın zaman Rəbbini həmd ilə təsbih et!"; 52:49 "Gecədən bir vaxtda da, ulduzlar çəkiləndə də Onu təsbih et!"
- 53:10 | MƏNA (kiçik) | "Öz quluna vəhy etdiyini etdi" — فَأَوْحَىٰ إِلَىٰ عَبْدِهِ مَا أَوْحَىٰ: ikinci feil "vəhy etdi"dir | "…Öz quluna vəhy etdiyini vəhy etdi"
- 75:14 | BURAXILMA | "Əksinə, (o gün) öz əleyhinə (olan bir) şahiddir!" — بَلِ الْإِنسَانُ: "insan" yoxdur | "Əksinə, insan (o gün) öz əleyhinə şahiddir!"
- 98:2 | MƏNA (orta) | "(O dəlillər) Allah tərəfindən olan rəsulun oxumaqda olduğu təmiz səhifələr idi" — رَسُولٌ مِّنَ اللَّهِ يَتْلُو صُحُفًا مُّطَهَّرَةً: 98:1-dəki açıq-aydın dəlil **rəsulun özüdür**, səhifələr deyil | "(O açıq-aydın dəlil) Allah tərəfindən gələn, pak səhifələri oxuyan bir rəsuldur."
- 59:8 | qrammatika | "öz yurdlarından çıxarılmış və mallarından edilmişlər" — feil düşüb; أُخْرِجُوا مِن دِيَارِهِمْ وَأَمْوَالِهِمْ | "öz yurdlarından və mallarından çıxarılmışlar (məhrum edilmişlər)"
- 9:100 | qrammatika | "ötüb keçən ilk Mühacirlərə və Ənsara" — yönlük hal; مِنَ الْمُهَاجِرِينَ | "…ilk Mühacirlərdən və Ənsardan və … onların ardınca gedənlər ki var"

## Əlavə A — mexaniki yoxlamanın xam siyahısı

```text
### double_space (11)
2:93  
3:34  
4:92  
7:97  
7:98  
16:23  
20:3  
22:62  
46:12  
66:8  
79:14  

### paren_unbalanced (7)
4:85  depth=-1
4:128  depth=-1
8:73  depth=1
23:89  depth=1
46:17  depth=1
46:20  depth=1
77:43  depth=1

### prefix_format (38)
2:198  198: Rəbbinizdən Onu
3:120  120: Əgər sizə bir y
3:188  188: (Etdikləri əməl
9:22  - 22: Onlar orada da
18:24  24: ...Yalnız Allah 
19:5  !
19:6  -5, 6: Həqiqətən də 
20:21  21: O, dedi: "Götür 
23:6  -5, 6: ...zövcələri 
26:4  ​-4: Əgər Biz istəsə
26:149  149: (Bir də) ustalı
27:3  3: Onlar (o möminlər
28:1  1: Ta, sin, mim. 
28:2  2:Bu, açıq-aydın (ha
34:11  11: ...(ey Davud, on
34:26  26: De: "Rəbbimiz ha
40:29  29: Ey qövmüm, bu gü
41:22  22: Siz eşitmələrini
49:14  14: Bədəvi ərəblər: 
55:5  -5:Günəş və Ay, o ik
55:7  -7:O (ər-Rahmən) səm
55:60  -60:(Məgər) ihsanın 
55:61  -61:(Elə isə) Rəbbin
55:62  -62:O ikisindən başq
55:64  -64:O ikisi olduqca 
55:66  -66:O ikisində fəvva
55:67  -67:(Elə isə) Rəbbin
55:68  -68:O ikisində (cürb
55:69  -69:(Elə isə) Rəbbin
55:72  -72:(Onlar) çadırlar
55:74  -74:Onlara onlardan 
55:76  -76:Onlar orada yaşı
55:77  -77:(Elə isə) Rəbbin
56:12  12: ...Nəim Cənnətlə
70:44  -44: Gözlərini qorxu
74:41  41: ...mucrimlərdən 
78:15  -15:...onunla taxıl 
82:1  -1:Səma yarıldığı (p

### prefix_number (1)
90:14  13

### space_before_punct (78)
2:60  ara belə dedik) : ”All
2:125  (və belə dedik) : ”İbr
2:132  i belə dedilər) : ”Ey 
2:236  lınızdan verin) ; zəng
3:49  a belə deyəcək) : ”Həq
3:79  (o, belə deyər) : ”Kit
3:103  (xatırlayın ki) , bir 
3:191   belə deyirlər) : ”Rəb
3:193  , belə deyirdi) : ”Rəb
4:18  arı haqladıqda) : ”Şüb
4:46  ə tənə vuraraq) : ”eşi
4:75  a və (eləcə də) : ”Rəb
5:107   belə deyirlər) : ”Əsl
5:117  o da bu sözdür) ; Məni
6:72  elə əmr olunub) : ”Nam
6:93  (belə deyirlər) : ”Can
6:135  ən gələni edin) , şübh
6:143  aq yaratmışdır) ; qoyu
7:49  ə deyiləcəkdir) : ”Cən
7:145  ona belə dedik) : ”Onu
7:160  belə əmr etdik) : ”Siz
7:171  a belə deyildi) : ”Siz
9:35  ə deyiləcəkdir) : ”Bu,
10:22  ə belə dedilər) : ”Həq
10:51  n gətirirsiniz) ? Siz 
10:68   Ona möhtacdır) ! Səma
10:91  Allah ona dedi) : ”İnd
10:106  nə nə bir fayda , nə d
11:76  ələkər dedilər) : ”Ey 
11:80  (Lut dedi) : ”Kaş
12:26   (və belə dedi) : ”Əgə
12:46  ib Yusufa dedi) : ”Yus
12:72  yüyü belə dedi) : ”Mən
13:33  i) danışırsınız ? Əksi
14:18   məsəli (budur) ; onla
15:46  belə deyiləcək) : ”Ora
15:86   kamil olandır) , Alim
16:28  elə deyəcəklər) : “Biz
16:53  ək diləyirsiniz .
16:57  uzaq və ucadır) ! Onla
16:59  ə belə düşünər) : ”xəc
17:14  belə deyiləcək) : "Oxu
18:24  24: ...Yal
18:48  belə deyiləcək) : ”And
19:64  ya Cibril dedi) : ”Biz
19:71  belə deyiləcək) : ”Siz
21:36   belə deyirlər) : "Siz
21:65  ə belə dedilər) : ”And
21:103  elə deyəcəklər) : ”Bu,
22:10  ə deyiləcəkdir) : ”Bun
23:6  -5, 6: ...zöv
25:14  belə deyiləcək) : ”Bu 
25:19  belə deyiləcək) : ”Art
26:54  (Firon dedi) : ”Həq
32:12  (belə deyirlər) : ”Rəb
34:11  11: ...(ey
38:42  ona belə dedik) : ”Aya
38:59  lara deyiləcək) : ”Bu(
38:59  lar deyəcəklər) : ”Onl
39:10  z belə buyurur) : “Ey 
40:46  belə deyiləcək) : ”Ali
44:23  vəhy edib dedi) : ”Qul
45:28  belə deyiləcək) : “Bu 
45:31  belə deyiləcək) : “Mən
47:3  rxasına düşmüş) , iman
47:4   fidyə almadan) , yaxu
48:11  onlara belə de) : ”All
50:24  belə deyiləcək) : ”Atı
51:25   da (cavabında) : "Sal
51:28  isə bunu bilib) : "Qor
53:38  qət də var idi) ; Heç 
54:48  belə deyiləcək) : ”Dad
56:12  12: ...Nəi
57:12  belə deyiləcək) : ”Bu 
59:23  zzət sahibidir) , əl-C
69:16   ...və 
74:41  41: ...muc
102:7  Sonra (yenə) , həqi

```

## Əlavə B — yetim dipnot rəqəmi olan ayələr (254)

2:137, 2:143, 2:148, 2:152, 2:164, 2:166, 2:167, 2:176, 2:177, 2:184, 2:189, 2:197, 2:209, 2:213, 2:217, 2:219, 2:220, 2:225, 2:227, 2:228, 2:229, 2:232, 2:233, 2:236, 2:241, 2:247, 2:255, 2:256, 2:257, 2:260, 2:263, 2:264, 2:267, 2:273, 2:276, 3:2, 3:19, 3:21, 3:30, 3:45, 3:63, 3:67, 3:68, 3:79, 3:81, 3:92, 3:179, 3:197, 3:200, 4:5, 4:6, 4:19, 4:22, 4:33, 4:34, 4:36, 4:38, 4:62, 4:63, 4:66, 4:73, 4:75, 4:77, 4:80, 4:83, 4:85, 4:91, 4:95, 4:107, 4:112, 4:117, 4:128, 4:129, 5:1, 5:2, 5:5, 5:11, 5:19, 5:25, 5:32, 5:35, 5:38, 5:41, 5:42, 5:45, 5:46, 5:48, 5:62, 5:63, 5:66, 5:75, 5:79, 5:89, 5:100, 5:103, 5:107, 5:109, 5:114, 5:117, 6:2, 6:7, 6:23, 6:61, 6:65, 6:75, 6:80, 6:92, 6:96, 6:98, 6:104, 6:112, 6:115, 6:121, 6:123, 6:125, 6:131, 6:134, 6:135, 6:145, 6:149, 6:152, 6:156, 7:3, 7:20, 7:28, 7:29, 7:34, 7:40, 7:46, 7:51, 7:53, 7:62, 7:71, 7:80, 7:89, 7:94, 7:96, 7:97, 7:98, 7:99, 7:134, 7:135, 7:138, 7:143, 7:164, 7:169, 7:185, 8:29, 8:38, 8:53, 9:12, 9:16, 9:67, 9:91, 9:92, 9:112, 10:1, 10:2, 10:21, 11:6, 11:10, 11:15, 11:49, 11:86, 11:87, 12:22, 12:23, 12:24, 13:6, 13:18, 14:5, 14:21, 16:36, 16:90, 17:67, 17:101, 17:102, 19:13, 19:14, 19:40, 19:51, 20:16, 20:53, 20:112, 21:89, 22:3, 22:5, 22:38, 22:55, 23:3, 23:46, 23:97, 24:4, 24:11, 24:23, 24:25, 24:31, 24:53, 25:1, 25:2, 25:77, 26:22, 26:30, 27:62, 27:66, 27:84, 28:25, 28:36, 28:44, 28:58, 30:30, 30:39, 33:67, 33:69, 34:18, 34:20, 34:37, 36:67, 36:68, 38:3, 39:3, 40:30, 40:32, 40:36, 42:9, 42:18, 42:31, 44:15, 47:18, 49:11, 51:10, 51:23, 52:21, 52:23, 54:51, 55:5, 56:37, 56:55, 57:16, 58:2, 59:15, 60:1, 60:4, 74:6, 74:17, 78:6, 78:9, 78:11, 81:1, 83:9, 83:20, 83:21, 84:19, 88:6

