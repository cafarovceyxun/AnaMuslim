[![English](https://img.shields.io/badge/English-6E7681?style=for-the-badge&logo=googletranslate&logoColor=white)](FEATURES.md)
![Azərbaycanca](https://img.shields.io/badge/Az%C9%99rbaycanca-1F6FEB?style=for-the-badge)

# AnaMuslim — Özəlliklər

Tətbiqin bütün özəllikləri, bölmə-bölmə.
[README](README.az.md#-özəlliklər--bir-baxışda)-də qısa xülasə var; bu sənəd tam
istinaddır.

**Platforma işarələri** — hər iki platforma buraxılışdadır: Android Google Play-də,
iOS App Store-da (2026-08-15-də yayımlandı; qalan boşluqlar
[IOS_MIGRATION_PLAN.md](IOS_MIGRATION_PLAN.md)-dədir). Ayrıca işarələnməyibsə,
özəllik ortaq koddadır və hər iki platformada işləyir.

| İşarə | Mənası |
| ----- | ------ |
| 🤖    | Yalnız Android (platforma inteqrasiyası, iOS qarşılığı hələ yoxdur) |
| 🌐    | İlk dəfə internet tələb edir (məzmun yüklənməsi) |

---

## 1. Quran oxuyucusu

### Oxuma rejimləri

Dörd fərqli düzülüş — yerinizi itirmədən app bar-dan dəyişdirilir:

- **Ayə-ayə** — hər ayə ayrıca kartda: ərəbcə, tərcümə, qeydlər və ayə üzrə
  əməliyyat sırası
- **Oxuma (mushaf)** — səhifə-səhifə fasiləsiz ərəbcə mətn, çap mushafı kimi
  düzülüb, səhifənin surə başlıqları və Bismillah ilə
- **Tərcümə** — tərcümə mərkəzli səhifələmə
- **Tərcümə (şaquli)** — tərcümənin tək fasiləsiz sürüşməsi, uzun oxuma üçün

### Naviqasiya

- Oxuyucunu **surə, cüz, hizb və ya səhifə** üzrə aç
- İndeks ekranı: 114 surə, 30 cüz, hizb rübləri və 604 mushaf səhifəsi — hər
  siyahının öz mətn filtri ilə
- Oxuyucu daxilində naviqator vərəqi: səhifədən çıxmadan istənilən surə → ayəyə
  keç
- Alt naviqator (əvvəlki / növbəti vahid) və səhifənin yuxarısından açılan
  aşağı-dartma jesti
- Digər ekranlardan birbaşa ayəyə keçid (axtarış nəticələri, əlfəcinlər, oxuma
  tarixçəsi, günün ayəsi, vidjetlər)
- **Avto-sürüşmə** — sürət tənzimləyicisi və dayandırma/davam üçün jest örtüyü
- **Səs düymələri** ilə səhifə çevirmə, xarici səhifə-çevirici / S Pen düymələri
  dəstəyi 🤖
- Landşaft və planşet düzülüşləri; geniş ekranlarda oxuyucu daha enli düzülüşə
  keçir

### Ayə əməliyyatları

Ayəyə basıb saxlayanda və ya ayə nişanına toxunanda:

- Kopyala, paylaş (mətn və ya şəkil)
- **Əlfəcin qeydi** əlavə et / redaktə et
- **Bənzər ayələr** — mətni uyğun və ya yaxın olan digər ayələri tap
- **Ayə məlumatı** — ayənin səhifəsi, cüzü, hizbi və nazil olma məlumatı
- Ayəni **Günün ayəsi** təyin et
- Ərəbcə mətndə və ya tərcümədə **səhv bildir**

### Surə haqqında

Hər surə üçün ayrıca səhifə — nazil olma yeri, ayə sayı, adının mənası və
kontekst; paketdəki HTML-dən render olunur, dörd tətbiq dilinin hamısında var.

---

## 2. Ərəb mətni və xətlər

Beş Quran xətti — **Ayarlar → Xətlər** bölməsində, hər biri üçün canlı önizləmə
ilə:

| Xətt | Qeyd |
| ---- | ---- |
| **Osmanlı (Həfs)** | Paketdədir — ilk açılışdan oflayn işləyir |
| **Kral Fəhd Kompleksi V1** | Səhifə-dəqiq KFQPC şrifti, tələb üzrə yüklənir 🌐 |
| **Kral Fəhd Kompleksi V2** | Səhifə-dəqiq KFQPC şrifti, tələb üzrə yüklənir 🌐 |
| **Kral Fəhd Kompleksi V4 (Təcvid)** | Təcvid rəngli KFQPC səhifələri 🌐 |
| **Hind-Pak** | 15 sətirli və 16 sətirli variantlar |

Əlavə mətn tənzimləmələri:

- **Osmanlı xəttində təcvid rəngləri** — paketdəki rəng atlasından oxunur,
  söndürülə bilir, işıqlı və qaranlıq mövzuya uyğunlaşan palitralarla
- **Ərəb mətni tamamilə söndürülə bilər** — yalnız tərcümə oxumaq üçün
- **Ərəbcə və tərcümə üçün ayrı mətn ölçüləri**, hər birinin öz əmsalı ilə
- KFQPC səhifə şriftləri səhifə üzrə keşlənir və sıxılmış arxivdən quraşdırılır —
  yalnız oxuduğunuz səhifələr yaddaşda saxlanılır

---

## 3. Tərcümə və sözbəsöz

- **Azərbaycanca Quran tərcüməsi (Mürşüd Yusifoğlu)** — ayə üzrə qeydlərlə,
  layihənin öz backend-indən gəlir və oflayn oxumaq üçün saxlanılır 🌐
- Ortaq inventardan ingilis, rus və türk dillərində əlavə tərcümələr 🌐
- **Mötərizələr** — tərcüməçinin əlavə etdiyi sözləri gizlət, yaxud fərqli
  rənglə işıqlandır ki, əsl mətndən seçilsin
- **Sözbəsöz** rejimi: ərəbcənin altında hər sözün mənası, hər sözün detal
  vərəqinə keçidlə. Söz paketləri: **ingiliscə** (tərcümə + transliterasiya),
  **rusca** və **türkcə** (tərcümə) 🌐
- **Söz üzrə səsləndirmə** — hər sözün ayrıca ərəbcə oxunuşu; surə-surə və ya
  toplu şəkildə yüklənir 🌐
- Haşiyələr sətir içində, toxunub açılan formatda
- Tərcümə yükləmələri ayrı-ayrı idarə olunur — ölçü və yenilənmə vəziyyəti
  yükləməzdən əvvəl göstərilir

---

## 4. Hədis

- Hədis kitabxanası **cildlər → kitablar → bablar → alt-bablar** iyerarxiyası
  ilə, ayrıca hədisə qədər gəzilə bilir
- Hər yazı **ərəbcə mətn, azərbaycanca tərcümə, mənbə istinadı və qeydlər**
  daşıyır
- Bütün kitabxananı oflayn oxumaq üçün yüklə 🌐 — öz yaddaş bölməsi və
  təmizləməsi ilə
- Hədis oxuyucusu üçün ayrıca **görünüş ayarları vərəqi**:
  - Ərəb şrifti: **Noto Nəsx**, **Osman Taha Nəsx** və ya **Ayət** (susmaya
    görə: Osman Taha)
  - Ərəbcə və tərcümə üçün ayrı mətn ölçüləri
  - Ərəb mətni, tərcümə və mənbə istinadı üçün açar-bağla
- **Naviqator vərəqi** — indeksə qayıtmadan kitablar və bablar arasında keçid
- Hədislər üçün **əlfəcinlər** və **ayrıca oxuma tarixçəsi**
- Həm qlobal axtarış ekranından, həm kolleksiya daxilindən **axtarış**
- **Quran istinadı seçicisi** — hədisin istinad etdiyi ayəni bağla və ya aç
- Hədisi mətn kimi və ya şəkil redaktoru vasitəsilə **şəkil kimi paylaş**
- Hədisin ərəbcəsini, tərcüməsini, mənbəsini və ya qeydlərini düzəltmək üçün
  **tətbiq daxilində redaktor** — göndərilənlər moderasiyaya düşür (bax §10)

---

## 5. Səs və qiraət

- **16 qaridən ayə-ayə qiraət** — Yasir əd-Dusari, Mişari Rəşid əl-Afasi, Səd
  əl-Ğamidi, əl-Husari, əl-Minşavi, Səud əş-Şüreym, Əbu Bəkr əş-Şatri, əl-Əcmi
  və başqaları 🌐
- Ayrıca qari dəsti ilə **tərcümə səsləndirməsi**
- **Ayə izləmə** — oxunan ayə oxuyucuda işıqlanır və ekran onu izləyir
- **Mini pleyer və genişlənmiş pleyer**: qari şəkli, axtarış zolağı, surə və ayə
  mövqeyi, aktiv ayə üzərində spotlight animasiyası
- **Oxutma sürəti** tənzimləməsi
- **Təkrar rejimləri** — tək ayə və ya ayə aralığı, təkrar sayı ilə
- **Səs bitəndə davranış** — dayan, növbəti surəyə keç və ya surəni təkrarla
- **Səs mənbəyi seçimi** — yalnız ərəbcə, yalnız tərcümə və ya hər ikisi
- **Arxa fonda oxutma** — media bildirişi, kilid ekranı idarəetməsi, qulaqlıq və
  Bluetooth düymələri 🤖
- **Android Auto** media brauzeri 🤖
- Qari üzrə **qiraət yükləmə** (bütöv Quran və ya seçilmiş surələr) — oflayn
  dinləmək üçün 🌐, qari üzrə silmə imkanı ilə

---

## 6. Axtarış

- Ərəbcə ayə mətni, tərcümələr, surə adları və hədis kolleksiyası üzrə **tam
  mətn axtarışı**
- Alt naviqasiyada **qlobal axtarış**, hədis oxuyucusunda kontekst daxili axtarış
- **Filtrlər** — axtarışı Quran və ya hədislə məhdudlaşdır, hansı tərcümələr
  daxilində axtarılacağını seç
- **Sürətli keçidlər** — bir toxunuşla tez-tez istifadə olunan axtarışlar
- **Axtarış tarixçəsi** — tək-tək və ya toplu silinməklə
- Sistem nitq tanıma ilə **səsli axtarış** 🤖
- Ərəbcə axtarış üçün sətir içi ipucları (hərəkələrdən asılı olmayan uyğunluq)

---

## 7. Kitabxana və şəxsi məlumatlar

- Həm ayələr, həm hədislər üçün **əlfəcinlər**, hər birində ixtiyari **qeyd**
  mətni; oxuyucudan birbaşa açılan əlfəcin baxış vərəqi
- Quran və hədis üçün ayrıca **oxuma tarixçəsi** — dayandığınız dəqiq ayə /
  hədis ilə
- Ana ekranda **Günün ayəsi** — hər gün backend-dən yenilənir, "indi oxu"
  əməliyyatı ilə
- Seçdiyiniz saatda **günlük xatırlatma bildirişi** 🤖
- **Ana ekran vidjetləri** 🤖:
  - *Günün ayəsi* vidjeti
  - İdarəetmə düymələri olan *qiraət pleyeri* vidjeti
- **Ana ekran bölmələri**: oxumağa davam et, seçilmiş oxumalar, əlfəcinlər,
  Quran oxuma tarixçəsi və hədis oxuma tarixçəsi
- Açılışda **salamlama ekranı**

---

## 8. Paylaşma

- **Ayəni və ya hədisi mətn kimi paylaş** — mesajlaşma tətbiqləri üçün
  formatlanmış (WhatsApp-a uyğun sətir sonları və istinad bloku)
- Daxili şəkil redaktoru ilə **şəkil kimi paylaş**:
  - Ərəbcə və azərbaycanca mətni ayrıca aç/bağla
  - Mətn ölçülərini və üfüqi / şaquli kənar boşluğunu tənzimlə
  - Paylaşmadan əvvəl canlı önizləmə
- Quran ayələri və hədislər üçün ayrı şəkil boru xətləri — kart ixrac
  keyfiyyətində render olunur 🤖

---

## 9. Görünüş və dil

- **Mövzu rejimi**: işıqlı, qaranlıq və ya sistemə uyğun
- **Yeddi vurğu palitrası**: Susmaya görə, Mavi, Bənövşəyi, Violet, Qırmızı,
  Sarı və Mono
- Android 12+ üzərində **Material You dinamik rəngləri** 🤖
- **Tətbiq dilləri**: Azərbaycan, İngilis, Rus, Türk və sistem dili — tətbiqi
  yenidən başlatmadan tətbiq olunur
- **Rəqəm sistemi**: Latın (1, 2, 3) və ya ərəb-hind (١, ٢, ٣) rəqəmləri
- Bütün ekranlar landşaft və planşet enlərinə uyğunlaşır; app bar-lar landşaftda
  48dp-yə daralır, amma həmişə görünür

---

## 10. Məzmun keyfiyyəti — bildirişlər və icma düzəlişləri

AnaMuslim-in adi Quran oxuyucusundan fərqi budur: mətn oxucular tərəfindən,
yerindəcə, tətbiq yeniləməsi gözləmədən düzəldilə bilir.

- Oxuyucudan ayənin ərəbcəsində və ya tərcüməsində **səhv bildir** — sərbəst
  mətnlə; ayə istinadı avtomatik əlavə olunur
- Hədis mətni və Quran tərcümələri üçün **tətbiq daxili redaktorlar**
- **Moderasiya boru xətti** — moderator olmayan istifadəçinin düzəlişi canlı
  cədvələ deyil, gözləmə növbəsinə düşür; heç nə baxılmadan yayımlanmır
- **Düzəlişləri idarə et** paneli (moderatorlar): gözləyən / təsdiqlənmiş /
  rədd edilmiş filtrləri, orijinal və təklif olunan mətnin yanaşı müqayisəsi,
  yazı üzrə təsdiq və ya rədd
- **Bildirişləri idarə et** paneli (moderatorlar): gələn ayə bildirişlərinin
  emalı
- Təsdiqlənən düzəlişlər növbəti məzmun sinxronizasiyasında bütün istifadəçilərə
  çatır — mağaza buraxılışı lazım deyil

---

## 11. Yükləmələr və yaddaş

- Tərcümələr, qiraətlər, sözbəsöz paketləri, hədis kitabxanası və KFQPC səhifə
  şriftləri üçün **element üzrə yükləmə** — istəmədən heç bir böyük fayl
  endirilmir
- **Resurs yükləmə mənbəyi** ayarı — məzmunun hansı güzgüdən götürüləcəyini seç
- Kateqoriya üzrə panelləri olan **yaddaş təmizləmə** ekranı:
  - Yüklənmiş tərcümələr
  - Yüklənmiş qiraətlər (qari üzrə)
  - Yüklənmiş xətlər / səhifə şriftləri
  - Hədis kitabxanası keşi
  Hər biri diskdə tutduğu həcmi göstərir və ayrıca silinə bilir
- **İxrac və idxal**: yalnız ayarlar, yalnız əlfəcinlər və ya hər şey — tək
  daşınan fayl: dil, mövzu, yükləmə mənbəyi, oxuma rejimi, avto-sürüşmə sürəti,
  ərəb mətni açarı, mətn ölçüləri, xətt və variantı, cari tərcümə, qari, oxutma
  sürəti, səs seçimi və səs bitəndə davranış

---

## 12. Məxfilik və oflayn iş

- **Reklam yoxdur, analitika SDK-ları yoxdur**
- Oxuma ilə bağlı heç bir özəllik üçün **hesab tələb olunmur** — giriş yalnız
  məzmuna nəzarət edən moderatorlar üçündür
- Məzmun yükləndikdən sonra **hər şey oflayn işləyir**: oxuma, tərcümə, hədis,
  əlfəcinlər, tarixçə, axtarış və yüklənmiş səs
- Şəbəkə yalnız məzmun yükləmələri, günün ayəsi, bildiriş / düzəliş göndərmə və
  yenilənmə yoxlaması üçün istifadə olunur
- Ətraflı: [PRIVACY.md](PRIVACY.md)

---

## 13. Platforma inteqrasiyaları 🤖

- **Tətbiq qısayolları və dərin keçidlər** — `OPEN_READER`, `OPEN_REFERENCE`,
  `OPEN_CHAPTER_INFO` intent-ləri və oxuyucuya `https` dərin keçidləri
- **Popup Quran pəncərəsi** — `SHOW_POPUP` ilə açılan üzən ayə istinadı; digər
  tətbiqlər öz kontekstindən çıxmadan ayə göstərə bilir
- **Sistem axtarışı inteqrasiyası** (searchable konfiqurasiyası) və səsli axtarış
- **Media sessiyası** — bildiriş, kilid ekranı və Android Auto
- **Ana ekran vidjetləri** (Günün ayəsi, qiraət pleyeri)
- Oxuyucuda **səs və səhifə düymələri ilə naviqasiya**
- **Çökmə qəbuledicisi + tətbiq daxili log baxıcısı** — kompüter olmadan problem
  diaqnozu üçün

---

## 14. İlk quraşdırma və yeniləmələr

- **İlk açılış sehrbazı**: tətbiq dili → mövzu → tərcümə və resurs seçimi; yeni
  quraşdırma sehrbazdan çıxmamış istifadəyə hazır olur
- Ana ekranda **tətbiq yeniləmə banneri** — versiya nömrəsi, "yeniliklər"
  siyahısı və mağaza linki ilə
- **Məcburi yeniləmə rejimi** — moderatorlar minimum versiya təyin edə bilir,
  ondan aşağıda banner bloklayıcı olur
- **Buraxılış elanı paneli** (moderatorlar): versiya kodu, versiya adı, minimum
  versiya, mağaza linki və dil üzrə buraxılış qeydləri (azərbaycanca, ingiliscə,
  türkcə, rusca) — Android və iOS üçün ayrıca

---

## 15. Moderator alətləri

Yalnız daxil olmuş moderator hesabında görünür:

- Düzəlişləri idarə et (Quran və hədis növbələri)
- Bildirişləri idarə et
- Tətbiq buraxılışı elanı
- **Tətbiq logları** — lokal log baxıcısı
- **Uzaq loglar** — server tərəfi log baxışı
- Resurs idarəetməsi (məzmun və versiya manifestləri)

---

## Platforma dəstəyi — ümumi mənzərə

| Sahə | Android | iOS |
| ---- | ------- | --- |
| Quran oxuyucusu, dörd rejim | ✅ | 🚧 ortaq kod, köçürmə davam edir |
| Xətlər, təcvid, sözbəsöz | ✅ | 🚧 |
| Hədis kitabxanası | ✅ | 🚧 |
| Qiraət pleyeri | ✅ | 🚧 |
| Axtarış | ✅ | 🚧 |
| Əlfəcinlər, tarixçə, ixrac/idxal | ✅ | 🚧 |
| Arxa fon səsi, media bildirişi | ✅ | ⛔ hələ yox |
| Android Auto | ✅ | — |
| Ana ekran vidjetləri | ✅ | ⛔ hələ yox |
| Səsli axtarış | ✅ | ⛔ hələ yox |
| Şəkil paylaşma redaktoru | ✅ | 🚧 |
| Bildirişlər və moderasiya | ✅ | ✅ |

Ortaq Compose Multiplatform kodu artıq oxuyucu, pleyer, hədis, ayarlar və ilk
quraşdırma klasterlərini əhatə edir; qalan iş platforma bağlantılarıdır. Cari
status: [IOS_MIGRATION_PLAN.md](IOS_MIGRATION_PLAN.md).

---

## Tətbiqdə olmayanlar

Axtarıb vaxt itirməmək üçün:

- Namaz vaxtları, qiblə kompası və hicri təqvim yoxdur — AnaMuslim Quran və
  hədis oxuyucusudur, hər şeyi bir yerə yığan tətbiq deyil
- Reklam, tətbiqdaxili satınalma və izləmə yoxdur
- Sosial lent, şərhlər və istifadəçi profilləri yoxdur

---

## Lisenziya

AnaMuslim **GNU General Public License v3.0** altında azad proqram təminatıdır —
bax [LICENSE](LICENSE). Layihə
[AlfaazPlus-un QuranApp](https://github.com/AlfaazPlus/QuranApp) tətbiqinin
fork-udur; istinad və dəyişikliklərin xülasəsi [NOTICE](NOTICE) faylında, öz
lisenziyaları olan resurs mənbələri isə [CREDITS.md](CREDITS.md)-dədir.
