# Hədis bazası yoxlaması — 2026-11-13

Mənbə: canlı Oracle bazası (7 cild, 41 kitab, 1274 bab, 3928 sətir) **Muheymin kitabının Word faylları ilə
tutuşdurulub** (`~/Desktop/İslam/*.docx`, 7 cild). Kitabın ərəbcə hədis nömrələri mətnin içindədir; azərbaycanca
nömrələr Word-da avtomatik siyahıdır (hamısı 1-dən başlayır) — yəni bazadakı azərbaycanca nömrəni import skripti
yazıb, **doğru nömrə ərəbcədir**.

## Kitabla təsdiqlənən: hər şey qaydasındadır

- **Tərcümələr:** 3020 hədisin azərbaycanca mətni kitabla müqayisə edildi — mediana 1.00; fərq yalnız boş sahələrdə
  (§ 3) və əvvəlcədən düzəldilmiş № 858-dədir.
- **Mənbə istinadları (§ 7):** 107 az↔ər rəqəm fərqi **kitabın özündədir** (baza kitabı dəqiq köçürüb).
- **№ 2663–2806 boşluğu kitabın özündədir** — 6-cı cild 2662-də bitir, 7-ci cild 2807-dən başlayır, kitab siyahısı
  bazadakı ilə eynidir. Əskik kitab yoxdur.
- **Cəhənnəm kitabının «8-ci babı»** əskik deyil: «Od əhlinin çəkişməsi» bazada 27-ci bab kimi var; sadəcə
  `chapter_no` 7 → 9 atlayır.
- **İçkilər kitabı:** kitabda № 2323–2332 əvvəlki № 2313–2322-nin **hərfi təkrarıdır** (kitabın redaktə xətası);
  import onları düzgün buraxıb. Bu aralıqdan bazada yalnız **№ 2333** yoxdur.

## Nisa babı (2026-11-13 düzəlişi) kitabda yoxdur — saxlanılır

Kitabın mündəricatında da, mətnində də Ali-İmran → Maidə gəlir; № 1454–1457 kitabda Ali-İmran babının altındadır.
Bu gün yaradılan `c4qu1ni29` kitaba uyğun deyil, amma **istifadəçinin qərarı ilə saxlanılır** (2026-11-13) — surə
babları hədislərin ayələrinə uyğun olsun deyə. Kitabla tutuşdurmada bu 4 hədis «yeri fərqli» çıxacaq — gözləniləndir.

## ✅ 2026-11-13 yükləndi (§ 1, § 2, § 3)

Kitabın Word faylından olduğu kimi (ərəbcə, azərbaycanca, `source`, haşiyə) bir tranzaksiyada yazıldı; əvvəlcə ROLLBACK
ilə sınandı. 89 yeni sətir (ayə 59 + hədis 30), 31 yeni bab/alt-bab, 6 sətir yeni baba köçdü, 5 sətrin mətni
düzəldi. Sonra kitabla yenidən tutuşduruldu: **kitabdakı hər hədisin mətni bazada var** (qalan fərqlər yalnız ərəbcə
nömrə yazısındadır — § 5).

- Ədəb: `c6ed2va1` «Valideynlərlə yaxşı rəftar etmək babları» (13 alt-bab, 7 ayə, № 2394–2404) + `c6ed2qo2`
  «Qohumluq…» (№ 2405–2408); köhnə bablar +2 sürüşdü.
- Cənnət: `c7ce4` 28–43 (16 bab, 52 ayə, № 3121–3133); köhnə 28+ bablar +16 sürüşdü.
- Namaz: alt-bab `c1na4ge87al6` «Davud aleyhissələmin namazı» (№ 543–546, id 729–732 köçdü).
- Həcc: `c2he3he3` «Həcc və Ümrənin fəziləti» (№ 779–780, id 990–991 köçdü).
- № 265 (`c1na4na44es9` #1), № 2333 (`c5ic9xa11` #2) əlavə; id 565 → № 379, id 937 → № 727 (təkrar nüsxələrin yerinə).
- Boş tərcümə: id 688 (№ 502), id 810 (№ 613); id 176 → Muhəmməd 8–9-un kitab tərcüməsi.
- Ayə sətirləri **kitabın tərcüməsi** ilə, İman kitabındakı formatda yazıldı (`-{…}`, `source` = «Bəqərə, 83»).
- Bəzi yeni slug-lar köhnə slug-larla toqquşmasın deyə `x` ilə bitir (məs. `c7ce4ce28x`) — slug identifikatordur.

Aşağıdakı § 1–3 cədvəlləri yükləmədən əvvəlki vəziyyətdir.

## 1. Bazada olmayan bablar (kitabda var)

| Kitab | Bab(lar) | Hədis | Bazada indi |
|---|---|---|---|
| Ədəb (`c6ed2`) | kitabın əvvəlindəki valideyn bölməsi — ~13 bab («valideynlərlə yaxşı davranmağı əmr etməsi», «Kafir də olsa valideynlə yumşaq davranmaq», «…ən çox haqqı olan ana, sonra ata», «valideyni söymək», «valideynin üzünə ağ olmaq», «atalardan üz çevirmək», «qohumluq əlaqələri…» və s.) + ayələr | **№ 2394–2408 (15)** | heç biri yoxdur; Ədəb «Qonağa ikram» ilə başlayır |
| Cənnət (`c7ce4`) | «razı olması» ilə «Cənnətə yalnız muslim nəfs» arasında **15 bab**: çaylar/Kövsər/hovuz, Kövsər, bulaqlar, zövcələr, döşəklər, taxtlar, mütəkkələr, sinilər, qədəhlər, qablar, meyvələr, xidmətçilər, içki, yemək, ilk yemək + ~50 ayə | **№ 3121–3133 (13)** | heç biri yoxdur |
| Namaz (`c1na4ge87`) | alt-bab «Allahın Nəbisi Davud aleyhissələmin namazı haqqında gələnlər» | № 544–547 | «Gecə namazına yatıb qalan kimsə» alt-babına düşüb |
| Həcc (`c2he3`) | «Həcc və Ümrənin fəziləti babı» | № 780–781 | «Məbrur Həcc…» babına düşüb |

## 2. Bazada olmayan tək hədislər

| № | Harada | Qeyd |
|---|---|---|
| 265 | Namaz — «Əsr namazını tərk edən kimsə» | |
| 379 | Namaz — «Camaat namazında təxfif» | id 565 onun yerinə № 378-in nüsxəsidir |
| 727 | Oruc — «səfərdə oruc tutmaqda ixtiyar» | id 937 onun yerinə № 726-nın nüsxəsidir |
| 2333 | İçkilər — «Xamrın içilməsi Qiyamətin əlamətlərindəndir» (ikinci) | |

## 3. Boş / səhv tərcümə

| id | № | Problem | Kitabda |
|---|---|---|---|
| 688 | 502 | `text_az` boş | var |
| 810 | 613 | `text_az` boş | var |
| 176 | — | ərəbcəsi Muhəmməd 8–9, azərbaycancası Zumər 65 (id 175-in təkrarı) | kitabdan götürüləcək |
| 437 | 252 | azərbaycancanın əvvəlində `_` | — |

## 4. Təkrar / yetim sətirlər — ✅ 2026-11-13 silindi

14 sətir silindi (cəmi 4017 → 4003); tam nüsxələri `docs/hadith/backup/deleted_duplicates_2026-11-13.json`-dadır.
Silmə trigger-i (`intercept_hadith_before_delete`) JWT-siz sessiyada növbəyə yazmağa çalışıb xəta verdiyi üçün
silmələr tranzaksiya daxilində `set local session_replication_role = replica` ilə edildi. id 325-in sırası əsl
hədislə eyni idi (3/3), ona görə yenidən nömrələməyə ehtiyac olmadı. id 565 və 937 silinmədi — onların yerinə
№ 379 və № 727 yazılıb (yuxarıda).

| Silinən | Səbəb |
|---|---|
| 325 | № 142-nin ikinci nüsxəsi |
| 1295–1306 | № 1058–1063-ün 2-ci və 3-cü nüsxələri |
| 25 | mövcud olmayan baba bağlı köhnə sətir (id 26 eynisidir) |

## 5. Nömrələr — ✅ 2026-11-13 düzəldi (154 sətir), Cənnətdə 34 sətir açıqdır

Hər sətrin az/ər nömrəsi kitabdakı mətnlə yoxlandı (hansı nömrənin altındakı kitab mətni bu sətrə uyğundursa, o
düzgündür). 140 sətirdə azərbaycanca, 12-də ərəbcə, 3-də (İçkilər id 2858–2860 → 2334–2336) hər ikisi düzəldi; yalnız
mətnin başındakı «N.» / «N-» dəyişdi. Hər UPDATE köhnə mətnin md5-i ilə qorundu. Köhnə mətnlər:
`docs/hadith/backup/number_fix_before_2026-11-13.json`. Nəticə: cild daxilində təkrar azərbaycanca nömrə 92 → 23
(hamısı Cənnət kitabında), az≠ər 190 → 39.

**Bilərəkdən toxunulmayan — kitabın öz ərəbcə yazı səhvi** (azərbaycanca düzgündür): id 337 (154 / ər. 153),
762 (576 / ər. 276), 2592 (2065 / ər. 205), 3122 (2609 / ər. 2709), 3127 (2614 / ər. 2714).

**✅ Cənnət kitabı (`c7ce4`) — 2026-11-13 həll olundu.** Word faylında nömrələr ardıcıldır (3108–3213); əvvəlki
«uyğun gəlmir» nəticəsi ayrıştırıcının xətası idi. 33 sətirdə azərbaycanca nömrə ərəbcəyə (= kitaba) uyğunlaşdırıldı
(«Cənnət əhlindən bəzi tayfalar» −13, «Muhəmmədin ümmətindən…» −7; id 3738-də nömrə Zubeyrin qeydindən sonradır,
3203 → 3210). «Muhəmmədin ümmətindən…» babında kitab Əbu Bəkr, Ömər, Osmanı **bir ortaq sərlövhədə** verir və
№ 3185–3187 hər üçünə aiddir; import onları 3 alt-baba 3 dəfə köçürmüşdü. İstifadəçinin qərarı ilə hədislər Əbu Bəkr
alt-babında (`c7ce4mu522-2`) qaldı, Ömər (`…mu523-3`) və Osman (`…mu524-4`) alt-bablarında təkrarlar (id 3710–3715)
silindi və yerinə **tətbiqin yönləndirmə qeydi** («Qeyd: … 2-ci alt-babda verilib», `text_ar`/`source` boş) yazıldı —
bu sətirlər kitab mətni deyil. Köhnə hal: `docs/hadith/backup/jannah_step3_before_2026-11-13.json`.

**Yekun (2026-11-13):** 3999 sətir; bütün bazada cild daxilində təkrar hədis nömrəsi yoxdur; az≠ər yalnız yuxarıdakı
5 sətirdir (kitabın öz yazı səhvi).

### Əvvəlki təsvir

Kitabın ərəbcə nömrəsinə uyğunlaşdırılacaq: Əmirlik id 2003–2009 (1396→1636…), Ədəb id 3004–3076 (2394→2494…),
İçkilər id 2858–2868 (2323→2334…; ərəbcəsi də), Luqata id 2869–2870 (4345→2345), Cənnət id 3602-dən sona qədər
(3121→3134…), id 745 (1→559). Ərəbcədəki tək rəqəm səhvləri (id 511: 426, id 596: 310 və s.) **kitabın özündədir**.

## 6. Kiçik bəndlər — ✅ 2026-11-13 düzəldi (3999 → 3997)

Köhnə hal: `docs/hadith/backup/step4_before_2026-11-13.json`.
- **Silindi:** id 10 (İslam/İman babının köhnə birləşik variantı, «Bəzi günahlar → Rəsulullahın adından…» alt-babında
  qalmışdı; məzmun öz babında id 202 + 77 kimi var) və id 3912 (Əhzab 64 ayəsinin hərfi təkrarı).
- **Köçdü:** id 13 (ayə + № 8) `c1im2is5` → `c1im2in6` #1 — ərəbcə sərlövhəsi «أمرت أن أقاتل الناس» babınındır.
- **Haşiyə/mənbə kitabdan:** № 263, 2193, 2539 — haşiyə `source`-a yapışmışdı, ayrıldı; № 2294 — mənbə və haşiyə
  itmişdi, `source`-da yalnız «Həmçinin bax…» qalmışdı → mənbə + haşiyə (+ «Həmçinin bax…» haşiyədə) bərpa olundu;
  № 1562 — `source`-a ərəbcə «أخرجه» qarışmışdı → azərbaycanca mənbə. Digər 6 «işarə var, haşiyə yox» hədisində
  kitabın özündə də haşiyə yoxdur — toxunulmadı. 12 sətirdə «Həmçinin bax…» `source`-dadır (kitabda da mənbədən
  sonra gəlir) — toxunulmadı.
- **Cəhənnəm (`c7ce5`):** `chapter_no` 9+ → −1 (artıq 1–55 ardıcıl).
- **Təharət id 288:** `sub_chapter_slug` `''` → `null`.
- **İman kitabı ərəbcə adları:** 79 bab + 17 alt-bab (İman, Təharət, Namaz) `name_ar` aldı — 74-ü bab/alt-babın ilk
  sətrinin `text_ar`-ındakı sərlövhədən, qalanı kitabın Word faylındakı ərəbcə↔azərbaycanca sərlövhə cütündən;
  sərlövhə 88 sətrin `text_ar`-ının əvvəlindən çıxarıldı (iki sətrə bölünmüş sərlövhələr və «وقول الله تعالى:» davamı
  ada qoşuldu). Boş qalan: `c1im2re15` (kitabdakı ərəbcə sərlövhə natamamdır: «باب قوله عز وجل: .»), `c3re3se27`
  (Səid ibn Zeyd — kitabda ərəbcə sərlövhə yoxdur), `c3ne2mu1` / `c1mu1` (Müqəddimə).

### Əvvəlki təsvir

## 6. Kiçik / kosmetik (əvvəlki)

- **İman kitabı (`c1im2`):** 86 babdan 80-nin `name_ar`-ı boşdur, ərəbcə bab adı `text_ar`-ın əvvəlindədir.
- **Haşiyə işarəsi var, qeyd boşdur:** id 448, 1995, 2002, 2375, 2723, 2829, 3050, 3736, 3754, 3775.
- Cəhənnəm kitabında `chapter_no` 8 atlanır; bəzi alt-bablarda `hadith_no` təkrarı (`c1im2be22re1`, `c7ce5od13`).

## 7. Mənbə istinadı rəqəmləri (az ↔ ər)

Eyni hədisin iki dildəki istinadında bir-iki rəqəmi fərqlənən 107 hal. Baza kitabı dəqiq köçürüb — fərqlər **kitabın özündədir**; düzgün
rəqəmi yalnız mənbə kitabla (Buxari, Muslim, Əhməd…) yoxlamaq olar. Təkrarlanan hədislərdə eyni səhv bir neçə yerdə çıxır (məs.
Buxari-1338/1374 üç dəfə, 3835/3853 üç dəfə). Ayə istinadı (`البراءة-11`) kimi yanlış uyğunlaşmalar da ola bilər.

| id | bab | № | azərbaycanca | ərəbcə |
|---|---|---|---|---|
| 13 | `c1im2is5` | — | Muslim-32 | البراءة-11 |
| 135 | `c1im2ne45` | 79 | Buxari-1338 | البخاري-1374 |
| 298 | `c1te3de3` | 117 | Məlik-58 | مالك-48 |
| 321 | `c1te3ce10` | 139 | Əhməd-26476 | أحمد-26467 |
| 334 | `c1te3ce10` | 151 | Humeydi-272 | الحميدي-672 |
| 357 | `c1te3he12` | 174 | Əhməd-2107 | أحمد-21070 |
| 359 | `c1te3he12` | 176 | İshaq ibn Rahaveyh-1069 | إسحاق بن راهويه-1029 |
| 383 | `c1na4in13` | 199 | Buxari-2127 | البخاري-6127 |
| 400 | `c1na4mu25` | 216 | Əbu Bəkr ibn əbi Şeybə-2608 | ابن أَبي شَيبة-26080 |
| 552 | `c1na4na58` | 367 | Darimi-14664 | الدارمي-1464 |
| 582 | `c1na4fe65` | 396 | Əhməd-21085 | أحمد-21058 |
| 626 | `c1na4cu80` | 440 | Muslim-1912 | مسلم-1911 |
| 634 | `c1na4cu80` | 448 | Buxari-938 | البخاري-939 |
| 666 | `c1na4se81` | 480 | Buxari-534 | البخاري-543 |
| 670 | `c1na4ik82` | 484 | Əhməd-3478 | أحمد-3487 |
| 680 | `c1na4is83` | 494 | Əhməd-12943 | أحمد-12934 |
| 691 | `c1na4qo85` | 505 | Darimi-1462 | الدارمي-1642 |
| 763 | `c1na4qu89` | 577 | Buxari-3835 | البخاري-3853 |
| 799 | `c2ze1he8` | 607 | Əhməd-21833 | أحمد-21832 |
| 839 | `c2ze1ru27` | 632 | Əbu Bəkr ibn əbi Şeybə-10870 | ابن أبي شيبة-10780 |
| 840 | `c2ze1ru27` | 633 | Məlik-2583 | مالك-2853 |
| 850 | `c2ze1se33` | 643 | Əbu Davud-1597 | أبو داود-1596 |
| 859 | `c2ze1oz42` | 652 | Əhməd-25178 | أحمد-25187 |
| 922 | `c2or2fe22` | 713 | Buxari-1868 | البخاري-1968 |
| 923 | `c2or2or23` | 714 | Əhməd-10593 | أحمد-10569 |
| 929 | `c2or2al26` | 720 | Buxari-4057 | البخاري-4507 |
| 999 | `c2he3mu8` | 788 | Tayəlisi-2755 | الطيالسي-2855 |
| 1013 | `c2he3ih17` | 802 | Əhməd-26742 | أحمد-26472 |
| 1035 | `c2he3te22` | 824 | Əhməd-13868 | أحمد-13867 |
| 1042 | `c2he3te22` | 831 | Əhməd-2275 | أحمد-2274 |
| 1055 | `c2he3be28` | 844 | Buxari-16703 | البخاري-6703 |
| 1104 | `c2he3he42` | 893 | Əhməd-10234 | أحمد-10238 |
| 1206 | `c3ne1ne14` | 977 | Buxari-3651 | البخاري-3561 |
| 1210 | `c3ne1se15` | 981 | Abd ibn Humeyd-1270 | عبد بن حميد-1275 |
| 1289 | `c3ne1ne38` | 1058 | Buxari-2127 | البخاري-6127 |
| 1290 | `c3ne1ne38` | 1059 | Buxari-79 | البخاري-69 |
| 1295 | `c3ne1ne38` | 1058 | Buxari-2127 | البخاري-6127 |
| 1296 | `c3ne1ne38` | 1059 | Buxari-79 | البخاري-69 |
| 1301 | `c3ne1ne38` | 1058 | Buxari-2127 | البخاري-6127 |
| 1302 | `c3ne1ne38` | 1059 | Buxari-79 | البخاري-69 |
| 1317 | `c3ne1ne40` | 1073 | Nəsai, "əl-Kubra"-8128 | النسائي"الكبرى"-8127 |
| 1318 | `c3ne1ne40` | 1074 | Tayəlisi-868 | الطيالسي-768 |
| 1321 | `c3ne1ne40` | 1077 | İbn Macə-134 | ابن ماجة-143 |
| 1343 | `c3ne1ne44` | 1098 | Humeydi-275 | الحميدي-257 |
| 1352 | `c3ne1re48` | 1106 | Əhməd-2544 | أحمد-25440 |
| 1363 | `c3ne1cu50` | 1117 | Buxari-3035 | البخاري-3053 |
| 1439 | `c3ne2al9` | 1147 | Buxari-6235 | البخاري-2635 |
| 1592 | `c3re3he20` | 1231 | İshaq ibn Rahaveyh-862 | إسحاق بن راهويه-762 |
| 1599 | `c3re3el21` | 1238 | İbn Macə-134 | ابن ماجة-143 |
| 1638 | `c3re3ab33` | 1277 | Əhməd-4494 | أحمد-4499 |
| 1718 | `c3mu4re5` | 1355 | Əhməd-8731 | أحمد-7831 |
| 1795 | `c4qu1ne22` | 1432 | Buxari-3835 | البخاري-3853 |
| 1852 | `c4qu1al41` | 1488 | Əhməd-2107 | أحمد-21070 |
| 1862 | `c4qu1du45` | 1498 | Muslim-7198 | مسلم-7168 |
| 1870 | `c4qu1ne47` | 1506 | Buxari-3835 | البخاري-3853 |
| 1888 | `c4qu1du59` | 1524 | Tayəlisi-877 | الطيالسي-977 |
| 1907 | `c4su2qe5` | 1541 | Buxari-1338 | البخاري-1374 |
| 1940 | `c4su2im13` | 1573 | Əhməd-3478 | أحمد-3487 |
| 1953 | `c4su2re17` | 1586 | Əbu Yə'lə-4633 | أبو يعلى-4383 |
| 1959 | `c4su2ne19` | 1592 | Buxari-79 | البخاري-69 |
| 1975 | `c4em3ha1` | 1608 | Əhməd-4495 | أحمد-4995 |
| 1989 | `c4em3em9` | 1622 | Əhməd-8486 | أحمد-8476 |
| 2008 | `c4em3be13` | 1401 | Əhməd-16664 | أحمد-16684 |
| 2174 | `c5hi1ne5` | 1731 | Buxari-1889 | البخاري-1899 |
| 2179 | `c5hi1ne6` | 1736 | Buxari-1868 | البخاري-1968 |
| 2241 | `c5ci2ci5` | 1752 | Əhməd-21833 | أحمد-21832 |
| 2253 | `c5ci2ix9` | 1764 | Muslim-4892 | إسحاق بن راهويه-182 |
| 2274 | `c5ci2he17` | 1785 | Tayəlisi-479 | الطيالسي-489 |
| 2291 | `c5ci2ne23` | 1797 | Əbu Davud-2648 | أبو داود-2649 |
| 2461 | `c5me3xe41` | 1939 | Əbu Bəkr ibn əbi Şeybə-26454 | ابن أبي شيبة-26545 |
| 2543 | `c5me3xe50` | 2016 | Buxari-2201 | البخاري-2202 |
| 2613 | `c5me3ne64` | 2085 | Məlik-1298 | مالك-1299 |
| 2689 | `c5mu4te10` | 2159 | Məlik-1836 | مالك-1863 |
| 2692 | `c5mu4sa11` | 2162 | Əhməd-5148 | أحمد-5184 |
| 2706 | `c5mu4he17` | 2176 | Məlik-2781 | مالك-2791 |
| 2734 | `c5mu4ri20` | 2204 | Buxari-2201 | البخاري-2202 |
| 2743 | `c5ek5to5` | 2213 | Əhməd-2086 | أحمد-2087 |
| 2762 | `c5ov6ih7` | 2232 | Əhməd-26742 | أحمد-26472 |
| 2794 | `c5qu7qu7` | 2259 | Buxari-3996 | البخاري-3997 |
| 2812 | `c5ye8qo9` | 2277 | Əhməd-1783 | أحمد-17830 |
| 2819 | `c5ye8xu12` | 2284 | Humeydi-275 | الحميدي-257 |
| 2925 | `c6ed2qo3` | 2416 | Əbu Davud-3643 | أبو داود-3634 |
| 2929 | `c6ed2ba5` | 2420 | Əbu Bəkr ibn əbi Şeybə-4056 | ابن أبي شيبة-4065 |
| 2985 | `c6ed2si31` | 2475 | Muslim-2830 | مسلم-6730 |
| 2995 | `c6ed2in34` | 2485 | Buxari-3373 | البخاري-3374 |
| 3017 | `c6ed2he46` | 2407 | Əhməd-21833 | أحمد-21832 |
| 3064 | `c6ed2bi77` | 2453 | Buxari-311 | البخاري-911 |
| 3100 | `c6li4ne1` | 2588 | Buxari-1582 | البخاري-1852 |
| 3108 | `c6li4ip8` | 2596 | Muslim-5435 | مسلم-5453 |
| 3119 | `c6li4qi12` | 2606 | Məlik-2604 | مالك-2704 |
| 3185 | `c6it6on10` | 2662 | Əhməd-2014 | أحمد-20140 |
| 3451 | `c7zu3he20` | 3021 | Buxari-1868 | البخاري-1968 |
| 3465 | `c7zu3he27` | 3035 | Buxari-6573 | البخاري-6537 |
| 3480 | `c7zu3mo31` | 3050 | Əbu Davud-2648 | أبو داود-2649 |
| 3482 | `c7zu3al33` | 3052 | Muslim-7095 | مسلم-7065 |
| 3494 | `c7zu3oz37` | 3064 | Əhməd-5974 | أحمد-5984 |
| 3496 | `c7zu3mo39` | 3066 | Buxari-5178 | البخاري-5187 |
| 3528 | `c7zu3al55` | 3096 | Məlik-2580 | مالك-2850 |
| 3664 | `c7ce4ce38` | 3140 | Muslim-2830 | مسلم-6730 |
| 3682 | `c7ce4le46` | 3158 | Əhməd-1397 | أحمد-13970 |
| 3703 | `c7ce4ce51` | 3169 | Muslim-7284 | مسلم-7285 |
| 3733 | `c7ce4mu52` | 3198 | Əhməd-13906 | أحمد-13907 |
| 3735 | `c7ce4mu52` | 3200 | Əhməd-4494 | أحمد-4499 |
| 3883 | `c7ce5od7` | 3228 | Əbu Bəkr ibn əbi Şeybə-37834 | ابن أبي شيبة-37838 |
| 3990 | `c7ce5al55` | 3248 | Abd ibn Humeyd-133 | عبد بن حميد-1330 |
| 3994 | `c7ce5be56` | 3252 | Əbu Yə'lə-2968 | أبو يعلى-2978 |
| 4046 | `c7ce7qe18` | 3300 | Buxari-1338 | البخاري-1374 |
