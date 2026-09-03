package com.cafarovceyxun.anamuslim.utils.prayer

/**
 * Namaz bildirişinin səsi.
 *
 * ### Yeni səs necə əlavə olunur
 * 1. Buraya bir sətir: `MAKKAH("makkah", androidRawName = "adhan_makkah", iosFileName =
 *    "adhan_makkah.caf")`.
 * 2. Android faylı → `app/src/main/res/raw/adhan_makkah.mp3` (uzantısı yazılmır, ad kifayətdir).
 * 3. iOS faylı → `iosApp/iosApp/Sounds/adhan_makkah.caf` **və Xcode target-inə əlavə edilir**.
 * 4. Adı üçün beş dildə bir sətir (`AdhanSoundSheet.titleOf`).
 *
 * Başqa heç nə lazım deyil: kataloq [entries] üzərindən qurulur, Android kanalı ilk dəfə lazım
 * olanda özü yaradılır, seçim isə `prayer.sounds` sətrində saxlanılır.
 *
 * ### Məhdudiyyətlər (fayl seçəndə nəzərə alınmalıdır)
 * - **iOS: ≤ 30 saniyə.** `UNNotificationSound` daha uzun faylı **səssizcə** sistem defoltu ilə
 *   əvəz edir. Tam azan (2-3 dəqiqə) bildiriş səsi kimi mümkün deyil — qısaldılmış variant lazımdır.
 * - **Android: kanal parametrləri dondurulur.** Səs kanalın özündədir, ona görə hər səsin **öz
 *   kanalı** var (`prayer_<id>`); mövcud səsin faylını dəyişmək istifadəçinin cihazında **təsir
 *   etmir**, yeni [id] lazımdır.
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
    /** Cihazın standart bildiriş səsi — indiyə qədərki davranış, ona görə defoltdur. */
    SYSTEM_DEFAULT("default"),

    /** Səs yoxdur; vibrasiya və ekrandakı bildiriş qalır. */
    SILENT("silent"),
    ;

    /** Öz faylı olan səs (azan) — platforma qatları bunu «xüsusi kanal/fayl lazımdır» kimi oxuyur. */
    val isCustom: Boolean get() = androidRawName != null || iosFileName != null

    companion object {
        val DEFAULT: AdhanSound = SYSTEM_DEFAULT

        fun fromId(id: String): AdhanSound? = entries.firstOrNull { it.id == id }
    }
}
