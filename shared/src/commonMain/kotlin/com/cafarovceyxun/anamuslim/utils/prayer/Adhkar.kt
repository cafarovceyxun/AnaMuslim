package com.cafarovceyxun.anamuslim.utils.prayer

/**
 * Səhər və axşam zikri xatırlatmaları — **günəşə bağlı**, namaz vaxtına yox.
 *
 * Səbəb sünnətin öz tərifidir: səhər zikrinin vaxtı fəcrdən gün çıxana qədər, axşam zikrininki isə
 * əsrdən gün batana qədərdir. Ona görə lövbər [Prayer.SUNRISE] və [Prayer.MAGHRIB]-dir (Axşam
 * cədvəldə günəşin batma anıdır) və istifadəçi yalnız həmin andan **nə qədər əvvəl/sonra**
 * xatırladılacağını seçir.
 *
 * ⚠️ Bu, [PrayerSettings.reminderMinutes] ilə eyni şey **deyil**, baxmayaraq ki riyaziyyatı oxşardır:
 * ora namazın öz bildirişinə bağlıdır (vaxt söndürülsə xəbərdarlıq da yox olur, səsi azandır, mətni
 * «filan vaxt girir»dir). Zikr isə müstəqil xatırlatmadır — Günəş bildirişi sönülü ola-ola işləyir,
 * öz adı və öz mətni var. İki anlayışı bir xəritədə birləşdirmək istifadəçinin bir seçimini
 * digərinin üstünə yazmaq demək olardı.
 */
enum class AdhkarSlot(
    /** Hansı günəş hadisəsinə bağlıdır. */
    val anchor: Prayer,
    /**
     * Toxunulmamış dəyər. Hər iki yuvada **30 dəqiqə əvvəl**-dir: zikrin vaxtı lövbərin özündə
     * bitir, ona görə xatırlatma bir qədər əvvəl gəlməlidir ki, oxumağa vaxt qalsın.
     */
    val defaultOffsetMinutes: Int,
) {
    /** Səhər zikri: gün çıxmamışdan 30 dəqiqə əvvəl. */
    MORNING(Prayer.SUNRISE, defaultOffsetMinutes = 30),

    /** Axşam zikri: gün batmamışdan 30 dəqiqə əvvəl — səhərlə **eyni** defolt. */
    EVENING(Prayer.MAGHRIB, defaultOffsetMinutes = 30),
    ;

    companion object {

        /**
         * Sürüşmənin hüdudları, **işarəli dəqiqə**: `>0` = lövbərdən əvvəl, `0` = tam həmin an,
         * `<0` = lövbərdən sonra. İşarə [PrayerNotificationRef.offsetMinutes] ilə eyni oxunur.
         *
         * ±60 qəsdlidir: qışda fəcrlə gün çıxma arası bəzi enliklərdə cəmi 70–80 dəqiqədir, daha
         * geniş aralıq xatırlatmanı zikrin öz vaxtından kənara — hələ fəcr girməmiş, ya da günorta
         * tərəfə — atardı.
         */
        val OFFSET_RANGE = -60..60

        /**
         * Steppər addımı. Dəqiqə-dəqiqə saymaq aralığın bir ucundan digərinə 120 toxunuş deməkdir;
         * zikr vaxtı üçün beş dəqiqəlik dəqiqlik onsuz da artıqlaması ilə kifayətdir.
         */
        const val OFFSET_STEP = 5

        fun fromName(name: String): AdhkarSlot? =
            entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
    }
}
