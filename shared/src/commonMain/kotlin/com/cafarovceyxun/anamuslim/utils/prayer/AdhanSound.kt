package com.cafarovceyxun.anamuslim.utils.prayer

/**
 * Namaz bildirişinin səsi.
 *
 * ### Yeni səs necə əlavə olunur
 * 1. Buraya bir sətir: `MAKKAH("makkah", androidRawName = "adhan_makkah", iosFileName =
 *    "adhan_makkah.caf")`.
 * 2. Android faylı → `app/src/main/res/raw/adhan_makkah.mp3` (uzantısı yazılmır, ad kifayətdir).
 * 3. **`AdhanSoundRes.rawResIdOrNull` (app modulu) → bir sətir: `R.raw.adhan_makkah`.** Bu, adın
 *    kosmetik təkrarı deyil: release-də resurs təmizləyicisi yalnız `R.raw.*`-a birbaşa müraciət
 *    görəndə faylı saxlayır, `getIdentifier`/adla URI isə onu səssizcə APK-dan silir. `when` tam
 *    olduğu üçün kompilyator bu addımı özü xatırladır.
 * 4. iOS faylı → `iosApp/iosApp/Sounds/adhan_makkah.caf` **və Xcode target-inə əlavə edilir**.
 * 5. Adı üçün beş dildə bir sətir (`AdhanSoundSheet.titleOf`).
 *
 * Başqa heç nə lazım deyil: kataloq [entries] üzərindən qurulur, Android kanalı ilk dəfə lazım
 * olanda özü yaradılır, seçim isə `prayer.sounds` sətrində saxlanılır.
 *
 * ### Məhdudiyyətlər (fayl seçəndə nəzərə alınmalıdır)
 * - **iOS: ≤ 30 saniyə.** `UNNotificationSound` daha uzun faylı **səssizcə** sistem defoltu ilə
 *   əvəz edir. Tam azan (2-3 dəqiqə) bildiriş səsi kimi mümkün deyil — qısaldılmış variant lazımdır.
 * - **Android: kanal parametrləri dondurulur.** Səs kanalın özündədir, ona görə hər səsin **öz
 *   kanalı** var (`prayer_v2_<id>`); mövcud səsin faylını dəyişmək istifadəçinin cihazında **təsir
 *   etmir** — ya yeni [id], ya da `NotificationUtils.CHANNEL_ID_PRAYER_PREFIX`-in yeni versiyası
 *   lazımdır (eyni id-ni silib yenidən yaratmaq işləmir: Android köhnə parametrləri bərpa edir).
 *
 * [id] **saxlanılan** addır — sabitin adı dəyişəndə istifadəçinin seçimi itməsin deyə enum adından
 * ayrıdır ([HomeSection] və [Prayer] ilə eyni qayda).
 */
enum class AdhanSound(
    val id: String,
    /** `res/raw` faylının adı (uzantısız). null = cihazın öz bildiriş səsi və ya səssiz. */
    val androidRawName: String? = null,
    /** iOS bundle-ındakı faylın adı (uzantı ilə). null = cihazın öz bildiriş səsi və ya səssiz. */
    val iosFileName: String? = null,
) {
    /**
     * «Hadi namaza» — **tətbiqin defolt bildiriş səsi** ([DEFAULT]).
     *
     * Fayl çağırışı iki dəfə təkrarlayır (arada 0.7 saniyə fasilə): tək dəfə deyiləndə bildiriş
     * səsi ilə qarışıb eşidilmirdi.
     *
     * ⚠️ Defolt olduğu üçün iki yerdə **`SYSTEM_DEFAULT` ilə qarışdırmaq olmaz**:
     * `NotificationUtils.prayerChannelId` və `PrayerNotificationContent` cihazın öz səsini
     * nəzərdə tutanda məhz [SYSTEM_DEFAULT]-a baxmalıdır. [DEFAULT]-a baxsalar bu səs
     * səhv yerə düşər (kanalda cihaz səsi çalınar, xəbərdarlıqda isə tam çağırış).
     */
    CALL("call", androidRawName = "prayer_call", iosFileName = "prayer_call.caf"),

    VOICE_AR("voice_ar", androidRawName = "prayer_voice_ar", iosFileName = "prayer_voice_ar.caf"),

    /**
     * ⚠️ Aşağıdakı üç ton mənbədə **30 saniyədən uzun** idi (30.1 / 39.0 / 33.2 san) və
     * `UNNotificationSound` onları səssizcə sistem defoltu ilə əvəz edərdi — istifadəçi seçimini
     * edir, adi bip eşidir. Repoya **28 saniyəyə kəsilmiş**, sonu sönən variant qoyulub.
     * Faylı yeniləyəndə həddi yenidən yoxla.
     */
    RINGTONE("ringtone", androidRawName = "prayer_ringtone", iosFileName = "prayer_ringtone.caf"),

    PHONE("phone", androidRawName = "prayer_phone", iosFileName = "prayer_phone.caf"),

    VIBRATING("vibrating", androidRawName = "prayer_vibrating", iosFileName = "prayer_vibrating.caf"),

    /** Cihazın standart bildiriş səsi. Artıq defolt DEYİL — bax [CALL]. */
    SYSTEM_DEFAULT("default"),

    /** Səs yoxdur; vibrasiya və ekrandakı bildiriş qalır. */
    SILENT("silent"),
    ;

    /** Öz faylı olan səs (azan) — platforma qatları bunu «xüsusi kanal/fayl lazımdır» kimi oxuyur. */
    val isCustom: Boolean get() = androidRawName != null || iosFileName != null

    companion object {
        /**
         * Seçim edilməyəndə işlənən səs.
         *
         * ⚠️ «Cihazın öz səsi» demək **deyil** — bunun üçün [SYSTEM_DEFAULT] var. Platforma
         * qatında hansının nəzərdə tutulduğunu ayırd et: kanal/xəbərdarlıq məntiqi
         * [SYSTEM_DEFAULT]-a baxmalıdır.
         */
        val DEFAULT: AdhanSound = CALL

        fun fromId(id: String): AdhanSound? = entries.firstOrNull { it.id == id }
    }
}
