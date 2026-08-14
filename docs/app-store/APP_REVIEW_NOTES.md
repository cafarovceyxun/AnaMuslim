# App Review Information — Notes

Apple 2026-08-14-də **Guideline 2.1 – Information Needed** ilə ilk göndərişi saxladı: rədd səbəbi
tətbiqdə xəta deyil, App Store Connect-də **App Review Information → Notes** sahəsinin boş
olmasıdır. Aşağıdakı A hissəsi həmin sahəyə (və rəddə cavab mesajına) olduğu kimi yapışdırılmaq
üçündür; B hissəsi tələb olunan ekran yazısının çəkiliş siyahısı, C hissəsi isə fiziki cihazda
sınaq üçün lazım olan addımlardır.

⚠️ `<<...>>` işarəli yerlər doldurulmalıdır — xüsusən **2-ci bənd (cihaz siyahısı)**: hazırda
buraxılış yalnız simulyatorda sınanıb, Apple isə məhz fiziki cihaz istəyir (C hissəsinə bax).

---

## A. Notes sahəsinə yapışdırılacaq mətn (ingiliscə)

```text
GENERAL

AnaMuslim is a free, ad-free Qur'an and hadith reader. There are no in-app purchases, no
subscriptions, no advertising and no third-party analytics or tracking SDKs. No account is
required to use any feature of the app.

A screen recording made on a physical device is attached to this reply (see the list of
covered flows below).


1. SCREEN RECORDING — WHAT IT SHOWS

The attached recording starts from a cold launch of a fresh install and covers:
 - First-run onboarding: app language, theme, and initial translation/resource selection
 - Qur'an reader in all four modes (verse-by-verse, mushaf page, translation, continuous
   translation), chapter/juz/page navigation, and verse actions
 - Selecting a different Arabic script and translation, and downloading them on demand
 - Recitation playback, reciter selection and playback speed
 - Hadith library: book, chapter, and hadith reading
 - Search across the Qur'an and hadith
 - Bookmarks, reading history, and the daily verse
 - Settings, including the storage/downloads screen and export/import
 - The only permission prompt the app can show: the iOS notification permission prompt,
   which appears when the user enables the optional daily verse reminder. The app requests
   no other permission — no location, contacts, camera, microphone, photo library, or
   App Tracking Transparency prompt exists anywhere in the app.
 - The user-generated content flow: "Report a mistake" on a verse (see item 4 below)

The app has no account registration flow, no paid content, no purchase or subscription
flow, and no public user-to-user content, so those flows do not appear in the recording.


2. DEVICES AND OS VERSIONS TESTED

<<Doldur: məsələn "iPhone 15 (iOS 26.0), iPhone 12 mini (iOS 26.0)">>

The minimum supported version is iOS 16.0; the app is built for iPhone and iPad.


3. WHAT THE APP DOES AND WHO IT IS FOR

AnaMuslim is a Qur'an and hadith reader for Azerbaijani, Turkish, Russian and English
speaking Muslims. The app interface and content are available in all four languages.

Problem it solves: high-quality Azerbaijani Qur'an and hadith content is scarce in existing
apps, and when a translation contains an error, users normally have to wait for a new app
release to see it corrected. AnaMuslim ships a maintained Azerbaijani translation and a
hadith library, and lets readers report mistakes from inside the reader; approved
corrections reach all users through a content sync, without an app update.

Core value for the user:
 - Offline reading after the initial content download
 - Five Arabic scripts, including page-accurate King Fahd Complex scripts and optional
   tajweed colouring
 - 15+ translations, word-by-word translation and word audio
 - Recitations from multiple reciters, streamed or downloaded for offline listening
 - A searchable hadith library
 - Bookmarks with personal notes, reading history, and a daily verse
 - No account, no ads, no tracking

Target audience: general audience (rated 4+). The app contains religious reference text
only — no user profiles, no social feed, no comments, no messaging.


4. HOW TO SET UP AND REACH THE MAIN FEATURES

No login, no demo account and no sample files are needed. On first launch the onboarding
wizard asks for app language, theme and an initial translation; after that:

 - Qur'an: bottom bar "Qur'an" tab -> pick a surah, juz, hizb or page. The reading mode is
   switched from the icon in the reader's app bar. Tapping a verse number opens the verse
   actions sheet.
 - Audio: the play button in the reader's app bar or in the verse actions sheet. Reciter,
   speed and repeat options are in the player sheet.
 - Hadith: bottom bar "Hadith" tab -> book -> chapter -> hadith.
 - Search: bottom bar "Search" tab; it searches Qur'an translations and hadith.
 - Downloads/storage: Settings -> Downloads and Settings -> Storage.
 - Daily reminder (this is where the notification permission prompt appears):
   Settings -> Reminders -> enable the daily verse reminder.

USER-GENERATED CONTENT
The only content a user can submit is a free-text "Report a mistake" message about a verse
(verse actions sheet -> Report an issue). This report is sent anonymously to the project's
own backend, with the verse reference attached and no account, name or device identifier.
Reports are visible ONLY to the project's maintainers in an internal moderation queue; they
are never shown to other users, so there is no user-to-user content to report or block, and
no feed that could carry objectionable material. Every correction that reaches other users
is published by a maintainer, never directly by a user.

MAINTAINER SIGN-IN (disclosed for completeness)
The app contains a sign-in sheet used only by the project's own maintainers to moderate
reports and edit content. It is reached from Settings by tapping the lock icon in the top
bar five times in quick succession. There is no self-service registration: accounts are
created manually by us for maintainers, so a reviewer cannot create one. Signing in unlocks
only content-moderation panels that act on the project's own server-side content database;
it changes nothing on the user's device and gates no user-facing feature. We deliberately
do not include credentials here because the account can modify live content for all users —
if the review team needs to see these panels, please let us know and we will provide a
time-limited account.

ACCOUNT DELETION
The app has no user account registration, so there is no account for a user to delete and
no account-deletion flow. All user data (bookmarks, history, preferences, downloads) is
stored on the device and is removed when the app is deleted.


5. EXTERNAL SERVICES USED

 - Supabase (our own backend project) — hosts the Azerbaijani Qur'an translation, the
   hadith library, the daily verse/hadith, the verse reports and their moderation queue,
   and release announcements. Supabase Auth is used only for the maintainer sign-in
   described above.
 - Qur'an.com — api.quran.com for word-by-word data, audio.qurancdn.com for word audio.
 - QuranicAudio (download.quranicaudio.com) — recitation audio, streamed or downloaded on
   the user's request.
 - GitHub raw, jsDelivr and gh-proxy.alfaazplus.com — on-demand content packs (fonts,
   Arabic scripts, translations) served from our own public open-source repository. The
   user can choose which of these mirrors to use in Settings.
 - api.alfaazplus.com — surah background information ("chapter info") from the upstream
   open-source project this app is forked from.

There are no payment processors, no AI services, no advertising networks, no analytics or
attribution SDKs, and no login providers beyond the maintainer-only Supabase Auth. The app
sends no user identifier with any of these requests.


6. REGIONAL DIFFERENCES

There are none. The app offers exactly the same features and the same content in every
region and on every storefront. Nothing is geo-gated, and no feature is enabled or disabled
based on the user's location or IP address — the app never requests or uses location.
The interface language (Azerbaijani, English, Russian, Turkish) follows the user's own
choice in the app, not the region.


7. THIRD-PARTY MATERIAL AND AUTHORISATION

AnaMuslim does not operate in a regulated industry (no health, finance, gambling or
government services). It does include religious reference material from third parties, and
each item is used within its published terms:

 - Qur'an Arabic text: KFGQPC / Tanzil text, redistributed verbatim and unmodified. The
   Tanzil Text License (CC BY 3.0) permits verbatim redistribution with attribution.
 - Qur'an fonts: King Fahd Glorious Qur'an Printing Complex (KFGQPC) fonts, whose licence
   grants free use, copying and distribution provided they are unmodified, not sold, and
   shipped with their licence — which is how they are used here.
 - Translations: redistributed under the Tanzil translation terms, which allow
   non-commercial, unmodified redistribution with attribution. AnaMuslim is free of charge
   with no in-app purchases, subscriptions or advertising, so this use is non-commercial.
   Public-domain translations (Pickthall, Yusuf Ali, Elmalili) carry no restriction. No
   translation whose publisher does not permit redistribution is included in the app.
 - Recitation audio: streamed or downloaded directly from the third-party providers listed
   in item 5 on the user's request. We do not host or redistribute recitation audio.
 - Tajweed colour data: derived from the cpfair/quran-tajweed dataset, CC BY 4.0, with
   attribution.
 - Application source code: AnaMuslim is free software under the GNU GPL v3.0 and is a fork
   of the open-source QuranApp by AlfaazPlus. Our full source, together with the complete
   per-asset licence and attribution table, is public at
   https://github.com/cafarovceyxun/AnaMuslim (see CREDITS.md).

The same attribution is shown to users inside the app, in Settings -> About -> Credits.
```

---

## B. Ekran yazısı (1-ci bənd) — çəkiliş siyahısı

**Şərtlər:** fiziki iPhone, ən son iOS, **təmiz quraşdırma** (əvvəlcə tətbiqi silin ki, onboarding
görünsün), təyyarə rejimi yox. Yazını **tətbiqin işə salınması** ilə başladın (ana ekrandan ikonaya
toxunuş görünsün). 3–5 dəqiqə kifayətdir; tələsmə, hər ekranda 2–3 saniyə dayan.

| # | Nə göstərilir | Qeyd |
|---|---|---|
| 1 | Ana ekran → ikonaya toxunuş → splash → onboarding | Dil, mövzu, tərcümə seçimi — tam keç |
| 2 | Ana səhifə: günün ayəsi, davam et kartı | |
| 3 | Quran tabı → surə siyahısı → surə aç | |
| 4 | Oxu rejimlərini dəyiş (dördü də) | App bar-dakı ikondan |
| 5 | Ayarlar → Skriptlər → KFQPC seç → **yüklənmə** | Şəbəkədən məzmun yüklənməsini göstərir |
| 6 | Təcvid rənglərini aç | |
| 7 | Ayəyə toxun → ayə əməliyyatları vərəqi | Kopyala/paylaş/əlfəcin görünsün |
| 8 | **«Səhv bildir»** → mətn yaz → göndər | UGC axını — Apple bunu xüsusi soruşur |
| 9 | Səsli oxunuş: pleyeri aç, qari dəyiş, sürəti dəyiş | |
| 10 | Hədis tabı → kitab → fəsil → hədis | |
| 11 | Axtarış tabı → söz axtar → nəticədən ayəyə keç | |
| 12 | Əlfəcinlər / tarixçə | |
| 13 | Ayarlar → Xatırlatmalar → gündəlik ayəni **aç** → **bildiriş icazəsi dialoqu** | ⚠️ Tətbiqdəki yeganə icazə soruşması — mütləq göstər |
| 14 | Ayarlar → Yaddaş / Yükləmələr | |
| 15 | Ayarlar → Haqqında → Kreditlər | 7-ci bəndi vizual təsdiqləyir |

**Göstərmə:** kilid ikonuna beş dəfə basıb maintainer girişini açma — mətndə onsuz da açıqlanır,
videoda göstərmək reviewer-i parol istəməyə yönəldir.

---

## C. Fiziki cihazda sınaq (2-ci bənd üçün məcburi)

Hazırkı vəziyyət: Mac-də yalnız **Apple Development** sertifikatı var
(`cafarovceyxun@gmail.com`, Team `G442RZYG7Y`), qoşulmuş və ya cütlənmiş iOS cihazı yoxdur
(`xcrun devicectl list devices` → boş), provisioning profile qovluğu boşdur. Yəni ilk növbədə
**iOS 16+ olan bir iPhone lazımdır** — öz cihazınız yoxdursa, ailə/dost telefonu da olar,
onu Apple ID ilə TestFlight-a əlavə etmək kifayətdir.

### Yol 1 — TestFlight (ən sürətli, həm də videonu bununla çəkin)

App Store Connect-ə yüklənmiş build artıq **daxili testerlər** üçün əlçatandır — Apple-in nəzərdən
keçirməsi tələb olunmur.

1. App Store Connect → **Users and Access** → Apple ID-nizin rolu `Admin`/`App Manager` olsun.
2. **TestFlight → Internal Testing** → qrup yarat → özünüzü (və cihaz sahibini) əlavə et.
3. Həmin build-i qrupa təyin et → dəvət e-poçtu gəlir.
4. iPhone-da **TestFlight** tətbiqini quraşdır → dəvəti qəbul et → AnaMuslim-i yüklə.
5. Sınaqdan keçir, sonra **B hissəsindəki** ssenari ilə ekranı yaz
   (Ayarlar → İdarəetmə Mərkəzi → Ekran Yazısı).

Üstünlüyü: reviewer-in gördüyü **eyni binary** sınanır və çəkilir.

### Yol 2 — Xcode ilə birbaşa quraşdırma (kabel/Wi-Fi)

1. iPhone-u kabellə qoş → telefonda **Trust This Computer** → təsdiqlə.
2. iPhone-da: **Ayarlar → Məxfilik və Təhlükəsizlik → Developer Mode** → aç → yenidən başlat.
3. Xcode → `iosApp/iosApp.xcodeproj` aç → target `iosApp` → **Signing & Capabilities**:
   Team `G442RZYG7Y`, **Automatically manage signing** işarəli olsun. Xcode cihazı komandaya özü
   qeydiyyatdan keçirir və development profile yaradır.
4. Yuxarıdakı sxem seçicisindən cihazı seç → **Run** (⌘R).
5. İlk açılışda telefon «Untrusted Developer» desə: **Ayarlar → Ümumi → VPN və Cihaz İdarəetməsi**
   → developer sertifikatına etibar et.

⚠️ Development build-i **`com.cafarovceyxun.anamuslim`** bundle id-si ilə quraşdırır — bu, mağazadakı
produksiya identifikatorunun eynisidir (Android-dəki `.test` suffiksi burada yoxdur). Cihazda
TestFlight versiyası varsa, biri digərini əvəz edəcək.

### Nəyi sınamaq lazımdır (rədd təkrarlanmasın deyə)

Apple «Bugs and crashes» bəndinə görə eyni build-i **hər dəstəklənən platformada** sınamağı istəyir.
Minimum:

- Təmiz quraşdırma → onboarding → ilk oxuma (şəbəkə ilə **və** təyyarə rejimində)
- Məzmun yükləmələri: tərcümə, skript/font, qari, hədis kitabxanası
- Səsli oxunuş: oxut, dayandır, qari dəyiş, sürəti dəyiş
- Ekranı kilidlə və audio davranışını yoxla — **iOS-da fon audio hələ tam deyil**
  (`FEATURES.md` → «Background audio, media notification: ⛔ not yet»); reviewer bunu sınayacaq,
  ona görə çökmə və ya donma olmadığına əmin ol
- Bildiriş icazəsi: rədd et → yenidən aç → icazə ver
- Landşaft rejimi və iPad (App Store-da iPad dəstəyi elan olunubsa **mütləq** iPad-da da sına)
- Eksport/İmport, əlfəcinlər, axtarış

---

## Göndərişdən əvvəl yoxlama siyahısı

- [ ] A hissəsindəki mətndə `<<...>>` yerləri dolduruldu (cihaz + iOS versiyaları)
- [ ] Fiziki cihazda tam sınaq keçirildi (C hissəsi)
- [ ] Ekran yazısı fiziki cihazda, təmiz quraşdırma ilə çəkildi (B hissəsi)
- [ ] Mətn **App Store Connect → App Review Information → Notes** sahəsinə yazıldı
      (növbəti göndərişlərdə də qalsın deyə)
- [ ] Eyni mətn + video Resolution Center-dəki rəddə cavab olaraq göndərildi
- [ ] App Store screenshot-ları tətbiqin **real istifadəsini** göstərir (yalnız splash/başlıq yox —
      Apple 2.3.3 bunu ayrıca xatırladıb)
- [ ] Məxfilik siyasəti linki və App Privacy cavabları `PRIVACY.md` ilə uyğundur
      (məlumat toplanmır; verse report anonimdir)
