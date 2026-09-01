package com.cafarovceyxun.anamuslim.utils.prayer

import com.cafarovceyxun.anamuslim.utils.IsoDate
import kotlin.math.roundToLong

/**
 * Bir mülci günün altı vaxtını hesablayır. **Saf** — vaxt seam-i, preference və şəbəkə yoxdur.
 *
 * Astronomiya [PrayerMath]-dədir (Meeus, `adhan`-dan portlanıb). Nəticə UTC epoxa millisaniyəsidir;
 * qurşaq hesabı ümumiyyətlə aparılmır. «Mülci gün» burada **UTC günü** deməkdir: cihazın yerli
 * gününə yığma [PrayerDay.forLocalDate]-in işidir.
 *
 * ### Üç pilləli, total qayda
 *
 * | Pillə | Şərt | Nə edilir |
 * |---|---|---|
 * | [TimeSource.ASTRONOMICAL] | bucaq həll olunur | düstur birbaşa |
 * | [TimeSource.NIGHT_FRACTION] | Fəcr/İşa bucağı çatmır, günəş doğur (≈54.5°–66.5°) | gecə `bucaq/60` nisbətində bölünür |
 * | [TimeSource.NEAREST_DAY] | günəş doğmur/batmır (>66.5°) | ən yaxın normal günün transit-fərqləri köçürülür |
 *
 * 12° ilə riyazi sərhəd **54.56°N**-dir — Moskva 55.75°N və şimalı hər yay bu hala düşür.
 *
 * ### Sərhəddə sıçrayış — qəbul edilmiş kompromis
 *
 * Fallback **kəsilməz deyil**. 55°N-də ölçüldü: son astronomik gün İşa−Axşam = 190 dəq, ertəsi gün
 * gecə/5 = 81 dəq — **109 dəqiqəlik sıçrayış** (gecə/2 ilə cəmi 13 dəqiqə). Yenə də `bucaq/60`
 * saxlanılır: gecə/2 fallback zonasında İşa ilə Fəcri eyni ana salır, limit (clamp) forması isə
 * bucağın həll olunduğu enliklərdə də işə düşüb seçilmiş metoddan kənara çıxarır. Sıçrayış yalnız
 * 54.56°N-dən şimalda, ildə iki dəfə görünür və UI-də `≈` ilə işarələnir.
 */
object PrayerTimes {

    private const val MILLIS_PER_HOUR = 3_600_000.0
    private const val MILLIS_PER_DAY = 86_400_000L

    /** İki vaxt arasındakı minimum məsafə — [enforceOrder] pozuq astronomiyanı buna sıxır. */
    private const val MIN_GAP_MILLIS = 60_000L

    /**
     * [nearestDayTimes] nə qədər uzağa baxır. Ekvinoksda **hər** enlikdə günəş doğur, ona görə
     * yarımillik pəncərə axtarışın bitməsinə zəmanət verir — alqoritm bu sayədə totaldır.
     */
    private const val NEAREST_DAY_SEARCH_DAYS = 183

    /**
     * Bir günün günəş həndəsəsi: dünən/bugün/sabah koordinatları və onların üzərində qurulan
     * hadisə həlledicisi.
     *
     * Üç gün lazımdır, çünki Meeus-un düzəlişi (səh. 102) düz qalxma və deklinasiyanı hadisə anına
     * **interpolyasiya edir** — bu, gün ərzindəki dəyişməni nəzərə alır və nəticəni `adhan` ilə
     * saniyə dəqiqliyində eyniləşdirir.
     */
    private class DaySolar(epochDay: Long, val at: GeoPoint) {
        private val julianDay = PrayerMath.julianDay(epochDay)
        private val today = PrayerMath.solarCoordinates(julianDay)
        private val previous = PrayerMath.solarCoordinates(julianDay - 1)
        private val next = PrayerMath.solarCoordinates(julianDay + 1)

        private val rightAscension = Triple(
            previous.rightAscensionDeg,
            today.rightAscensionDeg,
            next.rightAscensionDeg,
        )
        private val declination = Triple(
            previous.declinationDeg,
            today.declinationDeg,
            next.declinationDeg,
        )

        private val approximateTransit = PrayerMath.approximateTransit(
            longitudeDeg = at.longitude,
            siderealTimeDeg = today.apparentSiderealTimeDeg,
            rightAscensionDeg = today.rightAscensionDeg,
        )

        val declinationToday: Double get() = today.declinationDeg

        fun transitHours(): Double = PrayerMath.correctedTransit(
            approximateTransit = approximateTransit,
            longitudeDeg = at.longitude,
            siderealTimeDeg = today.apparentSiderealTimeDeg,
            rightAscension = rightAscension,
        )

        fun hourAngleHours(altitudeDeg: Double, afterTransit: Boolean): Double? =
            PrayerMath.correctedHourAngle(
                approximateTransit = approximateTransit,
                altitudeDeg = altitudeDeg,
                latitudeDeg = at.latitude,
                longitudeDeg = at.longitude,
                afterTransit = afterTransit,
                siderealTimeDeg = today.apparentSiderealTimeDeg,
                rightAscension = rightAscension,
                declination = declination,
            )

        /** `adhan` ilə eyni: kölgə bucağı **bugünkü** deklinasiyadan, sonra interpolyasiya. */
        fun asrHours(shadowFactor: Int): Double? = hourAngleHours(
            altitudeDeg = PrayerMath.asrAltitudeDeg(at.latitude, today.declinationDeg, shadowFactor),
            afterTransit = true,
        )
    }

    /**
     * [dateIso] (`yyyy-MM-dd`) günü üçün altı vaxt, və ya tarix pozuqdursa / nöqtə etibarsızdırsa null.
     *
     * ⚠️ [enforceOrder] **ofsetlərdən əvvəl** işləyir: sıralamanın məqsədi pozuq astronomiyanı
     * düzəltməkdir, istifadəçinin qəsdən verdiyi düzəlişi əzmək yox.
     */
    fun calculate(dateIso: String, at: GeoPoint, params: PrayerParams): PrayerDayTimes? {
        if (!at.isValid) return null

        val epochDay = IsoDate.toEpochDay(dateIso) ?: return null

        val raw = astronomicalOrNightFraction(epochDay, at, params)
            ?: nearestDayTimes(epochDay, at, params)
            ?: return null

        return PrayerDayTimes(
            dateIso = dateIso,
            times = roundToMinute(applyOffsets(enforceOrder(raw), params)),
        )
    }

    // region — pillə 0 və 1

    private fun astronomicalOrNightFraction(
        epochDay: Long,
        at: GeoPoint,
        params: PrayerParams,
    ): List<PrayerTime>? {
        val solar = DaySolar(epochDay, at)
        val horizon = horizonAltitude(at, params)

        val sunriseHours = solar.hourAngleHours(horizon, afterTransit = false) ?: return null
        val maghribHours = solar.hourAngleHours(horizon, afterTransit = true) ?: return null

        val transit = millisOf(epochDay, solar.transitHours())
        val sunrise = millisOf(epochDay, sunriseHours)
        val maghrib = millisOf(epochDay, maghribHours)

        // Əsr: bucaq çatmayanda (yalnız qütb qışının astanasında olur) Zöhr–Axşam aralığının ortası.
        val asr = solar.asrHours(params.asrShadowFactor)?.let {
            PrayerTime(Prayer.ASR, millisOf(epochDay, it), TimeSource.ASTRONOMICAL)
        } ?: PrayerTime(Prayer.ASR, (transit + maghrib) / 2L, TimeSource.NIGHT_FRACTION)

        // Gecə bölgüsü yalnız lazım olanda hesablanır — normal enliklərdə növbəti günə heç baxılmır.
        val nightMillis: Long? by lazy(LazyThreadSafetyMode.NONE) {
            nextSunriseMillis(epochDay + 1, at, params)?.let { it - maghrib }
        }

        val fajr = depressionTime(
            prayer = Prayer.FAJR,
            epochDay = epochDay,
            solar = solar,
            angle = params.fajrAngle,
            beforeTransit = true,
            anchor = sunrise,
            night = { nightMillis },
        ) ?: return null

        val isha = depressionTime(
            prayer = Prayer.ISHA,
            epochDay = epochDay,
            solar = solar,
            angle = params.ishaAngle,
            beforeTransit = false,
            anchor = maghrib,
            night = { nightMillis },
        ) ?: return null

        return listOf(
            fajr,
            PrayerTime(Prayer.SUNRISE, sunrise, TimeSource.ASTRONOMICAL),
            PrayerTime(Prayer.DHUHR, transit, TimeSource.ASTRONOMICAL),
            asr,
            PrayerTime(Prayer.MAGHRIB, maghrib, TimeSource.ASTRONOMICAL),
            isha,
        )
    }

    /**
     * Fəcr/İşa üçün ortaq hesab: bucaq həll olunursa astronomik, olunmursa gecənin `bucaq/60`
     * hissəsi qədər [anchor]-dan geri/irəli.
     */
    private inline fun depressionTime(
        prayer: Prayer,
        epochDay: Long,
        solar: DaySolar,
        angle: Double,
        beforeTransit: Boolean,
        anchor: Long,
        night: () -> Long?,
    ): PrayerTime? {
        val hours = solar.hourAngleHours(-angle, afterTransit = !beforeTransit)

        if (hours != null) {
            return PrayerTime(prayer, millisOf(epochDay, hours), TimeSource.ASTRONOMICAL)
        }

        val nightMillis = night() ?: return null
        val portion = (nightMillis * (angle / 60.0)).roundToLong()

        return PrayerTime(
            prayer = prayer,
            atMillis = if (beforeTransit) anchor - portion else anchor + portion,
            source = TimeSource.NIGHT_FRACTION,
        )
    }

    private fun nextSunriseMillis(epochDay: Long, at: GeoPoint, params: PrayerParams): Long? =
        DaySolar(epochDay, at)
            .hourAngleHours(horizonAltitude(at, params), afterTransit = false)
            ?.let { millisOf(epochDay, it) }

    // endregion

    // region — pillə 2

    /**
     * Qütb günü/gecəsi: günəşin doğduğu **ən yaxın** günü tapır və həmin günün vaxtlarının
     * transit-dən fərqini bugünkü transit-ə köçürür.
     *
     * Transit riyazi olaraq həmişə mövcuddur, ona görə Zöhr heç vaxt itmir və altı vaxt həmişə
     * bir-birindən fərqli çıxır.
     */
    private fun nearestDayTimes(
        epochDay: Long,
        at: GeoPoint,
        params: PrayerParams,
    ): List<PrayerTime>? {
        val todayTransit = transitMillis(epochDay, at)

        for (offset in 1..NEAREST_DAY_SEARCH_DAYS) {
            for (candidate in longArrayOf(epochDay - offset, epochDay + offset)) {
                val reference = astronomicalOrNightFraction(candidate, at, params) ?: continue
                val referenceTransit = transitMillis(candidate, at)

                return reference.map {
                    PrayerTime(
                        prayer = it.prayer,
                        atMillis = todayTransit + (it.atMillis - referenceTransit),
                        source = TimeSource.NEAREST_DAY,
                    )
                }
            }
        }

        return null
    }

    private fun transitMillis(epochDay: Long, at: GeoPoint): Long =
        millisOf(epochDay, DaySolar(epochDay, at).transitHours())

    // endregion

    // region — son emal

    /**
     * Son sipər: nəticə həmişə `fajr < sunrise < dhuhr < asr < maghrib < isha` sıralamasındadır.
     *
     * Degenerativ girişi (qütb astanası, ekstremal bucaq birləşmələri) səssiz pozuntu əvəzinə
     * sıxılmış, amma etibarlı cədvələ çevirir.
     */
    private fun enforceOrder(times: List<PrayerTime>): List<PrayerTime> {
        val sorted = times.sortedBy { it.prayer.ordinal }
        val result = ArrayList<PrayerTime>(sorted.size)
        var previous: Long? = null

        for (time in sorted) {
            val floor = previous?.plus(MIN_GAP_MILLIS)
            val clamped = if (floor != null && time.atMillis < floor) floor else time.atMillis

            result += if (clamped == time.atMillis) time else time.copy(atMillis = clamped)
            previous = clamped
        }

        return result
    }

    private fun applyOffsets(times: List<PrayerTime>, params: PrayerParams): List<PrayerTime> =
        times.map { time ->
            val offset = params.offsetOf(time.prayer)
            if (offset == 0) time else time.copy(atMillis = time.atMillis + offset * 60_000L)
        }

    /** Görünən üfüq — ayar sönülüdürsə dəniz səviyyəsi. Yalnız günəş doğuşu/Axşama təsir edir. */
    private fun horizonAltitude(at: GeoPoint, params: PrayerParams): Double =
        PrayerMath.horizonAltitudeDeg(if (params.useElevation) at.elevationMeters else 0.0)

    /**
     * Vaxtları **ən yaxın dəqiqəyə** yuvarlaqlaşdırır — `adhan`-ın default davranışı.
     *
     * ⚠️ Saniyələri sadəcə kəsmək (`HH:mm` mətnini kəsib götürmək) sınandı və uyğunsuzluq verdi:
     * 06:23:40 bizdə «06:23», çap təqvimlərində və `adhan`-da «06:24» görünürdü. Yuvarlaqlaşdırma
     * burada, mənbədə edilir ki, ekran, bildiriş və vidcet **eyni dəqiqəni** göstərsin — göstərmə
     * qatında etsəydik bildiriş 40 saniyə əvvəl çalardı.
     *
     * Sıralama pozulmur: [enforceOrder] onsuz da ən azı 60 saniyəlik məsafə saxlayır, ona görə
     * yuvarlaqlaşdırılmış iki vaxt eyni dəqiqəyə düşə bilmir.
     */
    private fun roundToMinute(times: List<PrayerTime>): List<PrayerTime> = times.map { time ->
        val rounded = ((time.atMillis + 30_000L) / 60_000L) * 60_000L
        if (rounded == time.atMillis) time else time.copy(atMillis = rounded)
    }

    private fun millisOf(epochDay: Long, utcHours: Double): Long =
        epochDay * MILLIS_PER_DAY + (utcHours * MILLIS_PER_HOUR).roundToLong()

    // endregion
}
