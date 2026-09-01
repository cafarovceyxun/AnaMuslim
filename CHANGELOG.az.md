[![English](https://img.shields.io/badge/English-6E7681?style=for-the-badge&logo=googletranslate&logoColor=white)](CHANGELOG.md)
![Azərbaycanca](https://img.shields.io/badge/Az%C9%99rbaycanca-1F6FEB?style=for-the-badge)

# Dəyişikliklər

İstifadəçiyə görünən dəyişikliklər burada toplanır. Mağaza buraxılışlarının qısa
«Yeniliklər» mətnləri `fastlane/metadata/` altındadır; tam özəllik istinadı isə
[FEATURES.az.md](FEATURES.az.md) faylındadır.

---

## Yayımlanmamış — 2026-08-31

Növbəti mağaza buraxılışına gedəcək dəyişikliklər.

### Yeni

- **Namaz vaxtları.** Günün altı vaxtı — Fəcr, Günəş, Zöhr, Əsr, Axşam və İşa —
  ana ekranda, öz ekranında (gün-gün baxışla) və Android ana ekran vidcetində.
  Bildirişlər hər vaxt üçün ayrıca açılır. Hər şey **cihazda** hesablanır:
  şəbəkə sorğusu yoxdur, hesab lazım deyil, koordinat telefondan çıxmır.
  - **Yer** GPS-dən gəlir (yalnız təxmini dəqiqlik) və adını əməliyyat sisteminin
    öz geocoder-i verir. GPS olmayanda tətbiqin içindəki **3 521 şəhərlik oflayn
    siyahı** var; axtarış diakritikaya baxmır — `seki` yazanda *Şəki*, `moskva`
    yazanda *Moscow* tapılır. Koordinatı əl ilə də yazmaq olar.
  - **Hesablama tək metoddur:** Fəcr və İşa üçün 12° (Fransa / UOIF). Bucaqlar
    və hər vaxta ±30 dəqiqəlik düzəliş Ayarlardadır.
  - **Hündürlük** dənizdən xeyli yüksək yerlər üçün açıla bilər (460 m-də təxminən
    dörd dəqiqə). Default sönülüdür, çünki çap təqvimləri və geniş yayılmış namaz
    kitabxanaları dəniz səviyyəsində hesablayır.
  - 54.5° şimaldan yuxarıda yayda günəş 12°-ə enmir, ona görə Fəcr və İşa gecənin
    bölünməsi ilə təxmin edilir; belə vaxtlar `≈` ilə işarələnir.
  - **İşlətdiyin yerlər yadda qalır.** Təyin etdiyin hər yer — GPS-dən və ya
    siyahıdan — saxlanılır, səyahətdən sonra geri keçmək bir toxunuşdur.
  - Funksiyanın ehtiyac duyduğu icazələr verilənə qədər **ekranda xəbərdarlıq
    qalır** və hansının çatışmadığını adı ilə deyir — səssiz uğursuzluq yoxdur.

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
- **Təkliflər lövhəsi.** Ayarlardan təklif göndər və ya problem bildir, başqalarının
  yazdığına səs ver, hər birini «açıq», «planlaşdırılıb» və «tamamlandı»
  mərhələləri boyunca izlə. Göndərilən mətn başqalarına görünməzdən əvvəl
  yoxlanılır və **heç bir kimlik saxlanmır**; öz təklifini izləməyə imkan verən
  qəbz yalnız cihazında qalır.
- **Ana səhifədə hekayələr.** Günün ayəsi/hədisi və yeni əlavə olunan funksiyalar
  toxunula bilən dairələr kimi görünür və tam ekranda oynayır — şəkil və video
  varsa, onlarla birlikdə.
- **Ana səhifə düzülüşü.** Ana səhifədə hansı bölmələrin, hansı sırada görünəcəyi
  artıq ayrıca ayardır.
- **Quran oxuyucusunda kitab rejimi.** Ayə-ayə oxuyucu müshəf səhifələri kimi
  düzülüb səhifə-səhifə vərəqlənə bilir və durduğun ayəni saxlayır.
- **Səhifə çevirmə animasiyaları.** Altı üslub — adi, solğunlaşma, yaxınlaşma,
  dərinlik, kitab və kub — hər iki oxuyucu üçün; hədis babları da əsl səhifə kimi
  çevrilir və bab qurtaranda səhifə düymələri növbəti baba keçir.
- **Hədis üçün rəvayət seçici** — bir neçə rəvayəti olan hədislərdə.
- **Qiymətləndirmə təklifi**: tətbiq bir müddət istifadə olunandan sonra bir dəfə
  soruşulur, üstəlik ayarlarda «Tətbiqi qiymətləndir» sətri. Konkret ulduz sayı
  heç vaxt istənmir.

### Dəyişdi

- **Günün ayəsi/hədisi kartı** yenidən düzüldü: tərcümə öndə, ərəbcə altda; hədis
  oxuyucusunun şrift və ölçü ayarlarına hörmət edir.
- **Ayarlar ekranı** yenidən qruplaşdırıldı — «Oxuma», «Hər iki oxucu» və «Bütün
  ayarlar» bölmələri.
- **Hədis redaktoru** (moderator): panodan `1§ 2§ 3§ 4§` formatı ilə bir neçə
  hədis eyni anda doldurulur, əl ilə əlavə edilib çıxarılır və toplu saxlanılır.
- **Günün ayəsi** növbəyə çevrildi: bir günə bir neçə yuva düşür, yəni eyni tarixə
  birdən çox ayə və ya hədis yayımlana bilir; xatırlatmalar da növbəni izləyir
  (iOS-da yaxın bildirişlər sistemə əvvəlcədən verilir).

### Düzəldildi

- **Ərəbcə axtarış**: mushafdan birbaşa kopyalanan mətn — vəqf işarələri və kiçik
  hərflərlə birlikdə — artıq nəticə qaytarır (əvvəl boş çıxırdı).
- **Səhifə çevirmə düymələri**: klaviaturanın PAGE UP / PAGE DOWN düymələri və
  S Pen düyməsi oxuyucuda etibarlı işləyir.
- **Günün ayəsi kartı internetsiz itmir** — son alınan məzmun göstərilir (köhnə
  gündən qalan məzmun göstərilmir).
- Ərəbcə interfeysdə azərbaycanca mətnlərin güzgülənməsi (nömrə və durğu
  işarələrinin yer dəyişməsi) aradan qaldırıldı.
- **Ekran sönəndə oxuma dayanmır** (Android). Pleyer prosesi canlı saxlayırdı,
  amma CPU və radionu yox — növbəti şəbəkə oxunuşu bitmirdi və səs səssizcə
  kəsilirdi.
- **Mini pleyer və oxutma düyməsi tətbiq bağlanıb açılandan sonra qalır** — son
  surə və ayə itmir, bərpa olunur.
- **Paylaşma və mağaza sətri iOS-da da işləyir**; App Store siyahısı yayımlanana
  qədər hər iki sətir gizli idi.

---

## Əvvəlki buraxılışlar

Bu fayldan əvvəlki buraxılışların qeydləri mağaza siyahılarındadır:
[Google Play](https://play.google.com/store/apps/details?id=com.cafarovceyxun.anamuslim)
və [App Store](https://apps.apple.com/az/app/id6799231138) (iOS ilk buraxılışı —
2026-08-15). Play üçün göndərilən «Yeniliklər» mətnləri
`fastlane/metadata/android/<dil>/changelogs/` altında saxlanılır.
