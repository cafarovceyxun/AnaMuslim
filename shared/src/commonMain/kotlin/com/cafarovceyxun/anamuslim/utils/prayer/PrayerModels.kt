package com.cafarovceyxun.anamuslim.utils.prayer

/**
 * Namaz vaxtı qatının modelləri — saf data, platforma və preference bilgisi yoxdur.
 *
 * Bütün anlar **UTC epoxa millisaniyəsidir**, «yerli dəqiqə» deyil. Səbəb qəsdlidir:
 * astronomiya təbii olaraq UTC anı verir (`transit = 12 − lng/15 − EqT`), yerli dəqiqəyə çevirib
 * geri qaytarmaq isə (a) saniyələri itirir, (b) DST keçid günündə «mövcud olmayan yerli saat»
 * probleminə düşür — `java.time` və `NSCalendar` onu fərqli sürüşdürür, yəni ildə iki dəfə Avropa
 * istifadəçisində Fəcr pozulardı. Ona görə bu qat
 * [com.cafarovceyxun.anamuslim.utils.epochMillisAtLocalTime] seam-inə **toxunmur**;
 * göstərmə mərhələsində `formatLocalDateTime` kifayət edir.
 */
enum class Prayer {
    FAJR,
    SUNRISE,
    DHUHR,
    ASR,
    MAGHRIB,
    ISHA,
    ;

    /** Günəş ibadət vaxtı deyil — cədvəldə göstərilir, geri sayımda və bildirişdə default olaraq yox. */
    val isPrayer: Boolean get() = this != SUNRISE
}

/**
 * Vaxtın necə alındığı. UI bunu `≈` işarəsi və izah sətri üçün oxuyur — istifadəçi yuxarı enlikdə
 * rəqəmin təxmini olduğunu bilməlidir.
 */
enum class TimeSource {
    /** Bucaq həqiqətən həll olunub. */
    ASTRONOMICAL,

    /** Bucaq çatmır, amma günəş doğur: gecə `bucaq/60` nisbətində bölünüb. */
    NIGHT_FRACTION,

    /** Günəş ümumiyyətlə doğmur/batmır: ən yaxın normal günün transit-fərqləri köçürülüb. */
    NEAREST_DAY,
}

/**
 * Onluq dərəcə + dəniz səviyyəsindən hündürlük (metr).
 *
 * ⚠️ Əvvəl burada «hündürlük saxlanmır — refraksiya sabiti onsuz da onu üstələyir» yazılmışdı.
 * **Bu, dağlıq şəhərlər üçün doğru deyil:** hündürlükdə üfüq aşağı düşür, ona görə günəş gec batır.
 * Sürüşmə Bakıda ~1 dəqiqə, Ankarada (850 m) ~5, Tehranda (1178 m) ~6 dəqiqədir — dəniz səviyyəsi
 * ilə hesablanmış **Axşam bu qədər erkən** çıxır, yəni Ramazanda iftar vaxtına birbaşa təsir edir.
 *
 * Yalnız günəş doğuşu və Axşama təsir edir: Fəcr/İşa astronomik üfüqdən ölçülən enmə bucaqlarıdır,
 * Zöhr isə transitdir.
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double = 0.0,
) {
    val isValid: Boolean
        get() = latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
            !(latitude == 0.0 && longitude == 0.0)
}

/**
 * Hesablama parametrləri. Default **Fransa/UOIF 12°** — tətbiqin tək metodu budur, istifadəçi
 * bucaqları və hər vaxta ± dəqiqə düzəlişini ayarlardan özü dəyişir.
 *
 * [asrShadowFactor] **istifadəçiyə açılmır** — tətbiqin qərarı «tək metod»dur. Sahə yalnız
 * riyaziyyatın tərifini ifadə etmək və testlərdə hər iki nisbəti yoxlamaq üçün qalır; dəyəri
 * həmişə 1-dir (kölgə = obyektin uzunluğu + günorta kölgəsi).
 *
 * ⚠️ **Hündürlük modelləşdirilmir** (2026-09-07-də ayar da, hesab da silindi). Ölçmə bunu
 * əsaslandırır: `adhan` (MIT, bu sahənin de-fakto kitabxanası — Mihrab və bir çox tətbiq onu
 * işlədir) `Coordinates(lat, lng)`-dən başqa **heç nə qəbul etmir**; AlAdhan, Diyanet və çap
 * təqvimləri də dəniz səviyyəsindədir. Bizim dəniz-səviyyəsi çıxışımız `adhan` ilə **saniyə
 * dəqiqliyində** üst-üstə düşür (yoxlanılıb: üç tarix, altı vaxt, fərq < 2 saniyə; yalnız Əsrdə
 * 33 saniyə). Yəni «düz» olmaq burada icmanın işlətdiyi cədvəllə üst-üstə düşmək deməkdir.
 *
 * [GeoPoint.elevationMeters] hələ **saxlanılır** (şəhər kataloqu, GPS, `prayer.saved_places`
 * sətri) — sadəcə hesablamaya girmir. Sahəni atmaq həmin üç formatı da dəyişmək demək olardı.
 */
data class PrayerParams(
    val fajrAngle: Double = DEFAULT_ANGLE,
    val ishaAngle: Double = DEFAULT_ANGLE,
    val asrShadowFactor: Int = 1,
    val offsetMinutes: Map<Prayer, Int> = emptyMap(),
) {
    fun offsetOf(prayer: Prayer): Int = offsetMinutes[prayer] ?: 0

    companion object {
        /** Fransa/UOIF: Fəcr və İşa üçün eyni 12°. */
        const val DEFAULT_ANGLE = 12.0

        /** Ayarlarda sürüşdürücünün hüdudları — bundan kənarda vaxtlar mənasızlaşır. */
        val ANGLE_RANGE = 8.0..20.0
        val OFFSET_RANGE = -30..30
    }
}

/**
 * İstifadəçinin işlətdiyi yer.
 *
 * [spotKey] koordinatı **iki onluğa** yuvarlaqlaşdırır (≈1.1 km): eyni şəhərdə alınan iki GPS
 * mövqeyi siyahını doldurmamalıdır, amma qonşu şəhərlər ayrı qalmalıdır.
 */
data class SavedPlace(
    val name: String,
    val point: GeoPoint,
) {
    val spotKey: String
        get() = "${round2(point.latitude)},${round2(point.longitude)}"

    fun isSameSpot(other: SavedPlace): Boolean = spotKey == other.spotKey

    private fun round2(value: Double): Long = kotlin.math.round(value * 100.0).toLong()
}

data class PrayerTime(
    val prayer: Prayer,
    val atMillis: Long,
    val source: TimeSource,
)

/** Bir günün cədvəli. [times] həmişə [Prayer] sırasındadır və altı elementdən ibarətdir. */
data class PrayerDayTimes(
    val dateIso: String,
    val times: List<PrayerTime>,
) {
    operator fun get(prayer: Prayer): PrayerTime? = times.firstOrNull { it.prayer == prayer }

    /** Ən azı bir vaxt təxminidir → UI izah sətrini göstərir. */
    val hasFallback: Boolean get() = times.any { it.source != TimeSource.ASTRONOMICAL }
}

/**
 * Planlaşdırma və UI üçün tam vəziyyət. [point] null = yer hələ təyin edilməyib; bu halda nə cədvəl,
 * nə bildiriş olur (UI istifadəçini yer seçməyə dəvət edir).
 */
data class PrayerSettings(
    val enabled: Boolean = false,
    val point: GeoPoint? = null,
    val placeName: String = "",
    val params: PrayerParams = PrayerParams(),
    val notify: Set<Prayer> = emptySet(),
    /**
     * Qəməri tarixə tətbiq olunan **gün** düzəlişi (−2…+2).
     *
     * [params]-a qoyulmur: ora astronomik hesablamanın girişidir, qəməri günün ona aidiyyatı
     * yoxdur — düzəliş yalnız tarixin göstərilməsinə (ekran, paylaşılan şəkil) təsir edir.
     */
    val lunarOffsetDays: Int = 0,
    /**
     * Adminin «ayı gördük» elanından çıxan gün düzəlişi
     * ([com.cafarovceyxun.anamuslim.utils.prayer.LunarCalendar.offsetDaysFor]).
     *
     * [lunarOffsetDays]-dən ayrı saxlanılır, çünki ikisi **fərqli sahibindir**: bunu server verir və
     * yeni ay elan olunanda dəyişir, onu isə istifadəçi. Bir açarda birləşdirsəydik elan gələndə
     * istifadəçinin öz düzəlişi görünməz şəkildə üstünə yazılardı və ayarlar vərəqində yanlış rəqəm
     * dayanardı.
     */
    val announcedLunarOffsetDays: Int = 0,
    /**
     * Hər namaz üçün seçilmiş bildiriş səsi. Sadalanmayan vaxt [AdhanSound.DEFAULT] alır —
     * ona görə xəritə boş ola bilər və yeni namaz/səs əlavə olunanda köhnə seçim pozulmur.
     */
    val sounds: Map<Prayer, AdhanSound> = emptyMap(),
    /**
     * Hər namaz üçün «neçə dəqiqə əvvəl xəbərdarlıq» — 0 və ya sadalanmamış = yalnız vaxt girəndə.
     *
     * Bu, [PrayerParams.offsetMinutes]-dən **fərqli şeydir**: ora vaxtın özünü sürüşdürür (cədvəldə
     * də dəyişir), bura isə cədvələ toxunmadan **əlavə** bir bildiriş doğurur.
     *
     * ⚠️ Hər dolu dəyər gündəlik bildiriş sayını bir artırır, iOS-da isə cəmi 64 gözləyən tələb
     * var ([com.cafarovceyxun.anamuslim.utils.notify.NotificationBudget]) — ona görə üfüq
     * ([PrayerNotificationPlan.upcoming]) bunları da sayır, əks halda son günlər səssizcə düşərdi.
     */
    val reminderMinutes: Map<Prayer, Int> = emptyMap(),
    /**
     * Hər namaz üçün «neçə dəqiqə **sonra** xatırlatma» — 0 və ya sadalanmamış = yoxdur.
     *
     * [reminderMinutes]-dən **tam ayrıdır**: biri vaxt girməzdən əvvəl hazırlıq üçün, digəri vaxt
     * girdikdən sonra «hələ qılmadın?» üçündür. İstifadəçi ikisini müstəqil qura bilir, ona görə
     * iki ayrı xəritədir — işarəli tək dəyər bir vaxt üçün ancaq birini saxlamağa imkan verərdi.
     */
    val followUpMinutes: Map<Prayer, Int> = emptyMap(),
    /**
     * Açıq olan zikr xatırlatmaları — boş = heç biri (defolt).
     *
     * [notify]-dan ayrı dəstdir və [enabled] açarına **tabe deyil**: «Namaz bildirişləri» açarı
     * adında yazıldığı işi görür, zikr isə müstəqil xatırlatmadır. İstifadəçi namaz bildirişlərini
     * tamamilə söndürüb yalnız səhər/axşam zikri ala bilər ([canScheduleAdhkar]).
     */
    val adhkar: Set<AdhkarSlot> = emptySet(),
    /**
     * Hər zikr üçün lövbərdən **işarəli** sürüşmə; sadalanmayan yuva
     * [AdhkarSlot.defaultOffsetMinutes] alır.
     *
     * ⚠️ `0` burada «qurulmayıb» demək **deyil** — tam gün çıxan/batan an deməkdir. Ona görə
     * yoxluq `null`-la ifadə olunur, [reminderMinutes]-dəki kimi sıfırla yox.
     */
    val adhkarOffsetMinutes: Map<AdhkarSlot, Int> = emptyMap(),
) {
    /**
     * Qəməri tarixi çəkən **yeganə** düzəliş — serverin elanı üstəgəl istifadəçinin öz düzəlişi.
     *
     * Qəməri gün göstərən hər yer ([lunarOffsetDays] yox) bunu oxumalıdır: ekran, vidcet və
     * paylaşılan təqvim. İkisindən birini tək işlətmək tətbiqin bir yerində bir tarix, başqa
     * yerində başqa tarix deməkdir — nə kompilyator, nə test bunu tutur.
     */
    val effectiveLunarOffsetDays: Int
        get() = announcedLunarOffsetDays + lunarOffsetDays

    /** Namaz bildirişi planlaşdırmaq mümkündürmü — hər üç şərt lazımdır. */
    val canSchedule: Boolean
        get() = enabled && point?.isValid == true && notify.isNotEmpty()

    /**
     * Zikr xatırlatması planlaşdırmaq mümkündürmü.
     *
     * [enabled] **soruşulmur**: zikr öz açarları ilə idarə olunur, yer isə yenə şərtdir — gün
     * çıxma/batma anı koordinatsız hesablanmır.
     */
    val canScheduleAdhkar: Boolean
        get() = point?.isValid == true && adhkar.isNotEmpty()

    /**
     * Ümumiyyətlə planlaşdırılacaq bir şey varmı.
     *
     * ⚠️ Planlaşdırıcıları işə salan hər yer (Android alarmı, iOS növbəsi, ayar ekranlarındakı
     * effektlər) **bunu** oxumalıdır. [canSchedule] tək başına yoxlansa, namaz bildirişlərini
     * söndürüb yalnız zikr istəyən istifadəçidə növbə ləğv edilir və heç nə çalmır — nə
     * kompilyator, nə test bunu tutur.
     */
    val canScheduleAny: Boolean
        get() = canSchedule || canScheduleAdhkar

    /**
     * İstifadəçi hər hansı bildiriş istəyirmi — icazə xəbərdarlığı üçün.
     *
     * [canSchedule]-dən fərqi: burada yer və seçilmiş vaxt şərt deyil, yalnız **niyyət** sayılır.
     * İcazə banneri məhz niyyətə baxmalıdır, çünki onun izah etdiyi hal «açıqdır, amma gəlmir»dir.
     */
    val wantsNotifications: Boolean
        get() = enabled || adhkar.isNotEmpty()

    fun soundOf(prayer: Prayer): AdhanSound = sounds[prayer] ?: AdhanSound.DEFAULT

    /** Zikr lövbərdən neçə dəqiqə əvvəl (`>0`) və ya sonra (`<0`) çalsın. */
    fun adhkarOffsetOf(slot: AdhkarSlot): Int =
        (adhkarOffsetMinutes[slot] ?: slot.defaultOffsetMinutes)
            .coerceIn(AdhkarSlot.OFFSET_RANGE)

    /** Xəbərdarlıq neçə dəqiqə əvvəl çalsın; 0 = yoxdur. Yalnız xatırladılan vaxtlar üçün. */
    fun reminderOf(prayer: Prayer): Int =
        if (prayer in notify) reminderMinutes[prayer]?.coerceIn(REMINDER_RANGE) ?: 0 else 0

    /** Xatırlatma neçə dəqiqə sonra çalsın; 0 = yoxdur. Yalnız xatırladılan vaxtlar üçün. */
    fun followUpOf(prayer: Prayer): Int =
        if (prayer in notify) followUpMinutes[prayer]?.coerceIn(REMINDER_RANGE) ?: 0 else 0

    /**
     * Gündə neçə bildiriş çıxır — büdcə hesabı üçün.
     *
     * Dörd mənbənin hamısı sayılır: vaxtın özü, ondan əvvəlki xəbərdarlıq, sonrakı xatırlatma və
     * açıq zikr yuvaları. Biri unudulsa üfüq ([PrayerNotificationPlan.upcoming]) həddindən uzun
     * hesablanır və iOS 64-lük limitdən artığını **səssizcə** atır.
     */
    val notificationsPerDay: Int
        get() = notify.size +
            notify.count { reminderOf(it) > 0 } +
            notify.count { followUpOf(it) > 0 } +
            adhkar.size

    companion object {
        /**
         * Əvvəl/sonra dəqiqələrinin hüdudları — **hər ikisi üçün eyni**.
         *
         * İstifadəçi rəqəmi klaviatura ilə yazır, ona görə aralıq geniş tutulub. Yuxarı hədd yenə
         * də var və şərtdir: (a) hər dolu dəyər gündəlik bildiriş sayını bir artırır, iOS-da isə
         * cəmi 64 gözləyən tələb var ([notificationsPerDay]); (b) qonşu vaxtlar arası fasilə bəzi
         * enliklərdə üç saatdan qısadır — daha uzun dəyər xəbərdarlığı qonşu namazın üstünə salar
         * və «hansı vaxt üçündür» oxunmaz olar.
         */
        val REMINDER_RANGE = 0..180

        /**
         * Stepper-in addımı. Dəqiqə-dəqiqə saymaq [REMINDER_RANGE]-in bir ucundan digərinə 180
         * toxunuş deməkdir; beşlik addım həm sürətlidir, həm də bildiriş vaxtı üçün kifayət qədər
         * dəqiq — bir-iki dəqiqəlik fərq qonşu namazla qarışmır.
         */
        const val REMINDER_STEP = 5
    }
}
