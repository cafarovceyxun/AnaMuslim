[![English](https://img.shields.io/badge/English-6E7681?style=for-the-badge&logo=googletranslate&logoColor=white)](CHANGELOG.md)
![Azərbaycanca](https://img.shields.io/badge/Az%C9%99rbaycanca-1F6FEB?style=for-the-badge)

# Dəyişikliklər

İstifadəçiyə görünən dəyişikliklər burada toplanır. Mağaza buraxılışlarının qısa
«Yeniliklər» mətnləri `fastlane/metadata/` altındadır; tam özəllik istinadı isə
[FEATURES.az.md](FEATURES.az.md) faylındadır.

---

## Yayımlanmamış — 2026-08-24

Növbəti mağaza buraxılışına gedəcək dəyişikliklər.

### Yeni

- **Azərbaycanca tərcümə səsi.** Quran tərcüməsi tam səsləndirildi (114 surə) və
  tətbiqdən yüklənir. Pleyerdə səs mənbəyi düyməsi (yalnız ərəbcə · yalnız
  tərcümə · hər ikisi), qari vərəqində «Tərcümə səsi» bölməsi, yükləmə ekranında
  ayrıca bölmə. Tərcümə oxunarkən də ayə vurğulanması ayəni izləyir; iOS-da
  oxutma ayə-ayə klip növbəsi ilə işləyir.
- **Ərəbcə interfeys.** Beşinci tətbiq dili; seçiləndə bütün düzülüş sağdan-sola
  keçir, azərbaycanca hədis və tərcümə mətnləri isə öz yazı istiqamətini saxlayır.
- **Hədisdə kitab rejimi.** Hədislər kartsız, davamlı kitab mətni kimi axır —
  indeksdəki düymə ilə açılıb-bağlanır.
- **Cild icmalı vərəqi.** Kitab, bab və alt-babların bütöv ağacı; «hamısını aç»,
  «hamısını yığ» və müqəddimə bəndi ilə.
- **Barmaqla mətn ölçüsü.** Oxuyucuda iki barmaq tərcüməni, üç barmaq ərəbcə
  mətni böyüdüb-kiçildir.
- **Oxuyucunun öz ayarlar vərəqi**, üstəlik **açılış rejimi** seçimi — oxuyucu
  indeksdən, əlfəcinlərdən və tarixçədən hansı düzülüşdə açılsın («sonuncu
  istifadə olunan» daxil).
- **Tətbiq mətninin ölçüsü.** İnterfeys mətnlərini böyüdüb kiçildən sürgü; Quran
  və hədis mətnləri öz ayarlarında qalır.
- **Tab-lar arasında sürüşdürmə** — alt paneldəki bölmələr arasında keçid
  jestlə də mümkündür, keçid animasiyaları ilə.
- **Günün ayəsi kartını** ana səhifədə göstər/gizlət ayarı.

### Dəyişdi

- **Günün ayəsi/hədisi kartı** yenidən düzüldü: tərcümə öndə, ərəbcə altda; hədis
  oxuyucusunun şrift və ölçü ayarlarına hörmət edir.
- **Ayarlar ekranı** yenidən qruplaşdırıldı — «Oxuma», «Hər iki oxucu» və «Bütün
  ayarlar» bölmələri.
- **Hədis redaktoru** (moderator): panodan `1§ 2§ 3§ 4§` formatı ilə bir neçə
  hədis eyni anda doldurulur, əl ilə əlavə edilib çıxarılır və toplu saxlanılır.

### Düzəldildi

- **Ərəbcə axtarış**: mushafdan birbaşa kopyalanan mətn — vəqf işarələri və kiçik
  hərflərlə birlikdə — artıq nəticə qaytarır (əvvəl boş çıxırdı).
- **Səhifə çevirmə düymələri**: klaviaturanın PAGE UP / PAGE DOWN düymələri və
  S Pen düyməsi oxuyucuda etibarlı işləyir.
- **Günün ayəsi kartı internetsiz itmir** — son alınan məzmun göstərilir (köhnə
  gündən qalan məzmun göstərilmir).
- Ərəbcə interfeysdə azərbaycanca mətnlərin güzgülənməsi (nömrə və durğu
  işarələrinin yer dəyişməsi) aradan qaldırıldı.

---

## Əvvəlki buraxılışlar

Bu fayldan əvvəlki buraxılışların qeydləri mağaza siyahılarındadır:
[Google Play](https://play.google.com/store/apps/details?id=com.cafarovceyxun.anamuslim)
və [App Store](https://apps.apple.com/az/app/id6799231138) (iOS ilk buraxılışı —
2026-08-15). Play üçün göndərilən «Yeniliklər» mətnləri
`fastlane/metadata/android/<dil>/changelogs/` altında saxlanılır.
