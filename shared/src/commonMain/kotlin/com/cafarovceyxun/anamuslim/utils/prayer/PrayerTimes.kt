package com.cafarovceyxun.anamuslim.utils.prayer

import com.cafarovceyxun.anamuslim.utils.IsoDate
import kotlin.math.roundToLong

/**
 * Bir mülki günün altı vaxtını hesablayır. **Saf** — vaxt seam-i, preference və şəbəkə yoxdur.
 *
 * Nəticə UTC epoxa millisaniyəsidir; qurşaq hesabı ümumiyyətlə aparılmır (bax [PrayerModels.kt]
 * KDoc-u). «Mülki gün» burada **UTC günü** deməkdir: cihazın yerli gününə yığma
 * [PrayerDay.forLocalDate]-in işidir.
 *
 * ### Üç pilləli, total qayda
 *
 * | Pillə | Şərt | Nə edilir |
 * |---|---|---|
 * | [TimeSource.ASTRONOMICAL] | bucaq həll olunur | düstur birbaşa |
 * | [TimeSource.NIGHT_FRACTION] | Fəcr/İşa bucağı çatmır, günəş doğur (≈54.5°–66.5°) | gecə `bucaq/60` nisbətində bölünür |
 * | [TimeSource.NEAREST_DAY] | günəş doğmur/batmır (>66.5°) | ən yaxın normal günün transit-fərqləri köçürülür |
 *
 * 12° ilə riyazi sərhəd **54.56°N**-dir (yay gündönümündə `φ + δ − 90 = −12`) — Moskva 55.75°N və
 * şimalı hər yay bu hala düşür, tətbiqin isə `values-ru` dili var.
 *
 * ### Sərhəddə sıçrayış — qəbul edilmiş kompromis
 *
 * Fallback **kəsilməz deyil** və ola da bilməz. Sərhəd günündə astronomik İşa günəş alt
 * kulminasiyasına, yəni gecənin **ortasına** yaxınlaşır; `bucaq/60` isə 12° üçün gecənin beşdə
 * biridir. 55°N-də ölçüldü: son astronomik gün İşa−Axşam = 190 dəq, ertəsi gün gecə/5 = 81 dəq —
 * **109 dəqiqəlik sıçrayış** (gecə/2 qaydası ilə cəmi 13 dəqiqə olardı).
 *
 * Buna baxmayaraq `bucaq/60` saxlanılır, çünki alternativlərin hər ikisi daha pisdir:
 * - **gecə/2** kəsilməzdir, amma fallback zonasının içində İşa ilə Fəcr eyni ana düşür
 *   (İşa 01:30, Fəcr 01:31) — istifadəyə yaramaz cədvəl.
 * - **Limit (clamp) forması** hər yerdə kəsilməzdir, amma bucağın həll olunduğu enliklərdə də
 *   işə düşür: yay gündönümündə Parisdə İşanı 10, Londonda (51.5°N) **28 dəqiqə** tərpədir —
 *   yəni seçilmiş UOIF cədvəlindən kənara çıxır. İstifadəçi «tək metod, Fransa standartı» dedi,
 *   ona görə metoda sadiqlik üstündür.
 *
 * Nəticədə sıçrayış yalnız **54.56°N-dən şimalda** və ildə iki dəfə görünür; həmin vaxtlar onsuz da
 * riyazi olaraq təyin olunmayıb və UI-də `≈` ilə işarələnir.
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
     * [dateIso] (`yyyy-MM-dd`) günü üçün altı vaxt, və ya tarix pozuqdursa / nöqtə etibarsızdırsa null.
     *
     * ⚠️ [enforceOrder] **ofsetlərdən əvvəl** işləyir: sıralamanın məqsədi pozuq astronomiyanı
     * düzəltməkdir, istifadəçinin qəsdən verdiyi düzəlişi əzmək yox. Ona görə ofsetlər həmişə
     * bir-birindən asılı olmadan tətbiq olunur.
     */
    fun calculate(dateIso: String, at: GeoPoint, params: PrayerParams): PrayerDayTimes? {
        if (!at.isValid) return null

        val epochDay = IsoDate.toEpochDay(dateIso) ?: return null

        val raw = astronomicalOrNightFraction(epochDay, at, params)
            ?: nearestDayTimes(epochDay, at, params)
            ?: return null

        return PrayerDayTimes(
            dateIso = dateIso,
            times = applyOffsets(enforceOrder(raw), params),
        )
    }

    // region — pillə 0 və 1

    /**
     * Günəşin doğduğu gün üçün tam cədvəl; günəş doğmursa **null** (o hal [nearestDayTimes]-indir).
     *
     * Fəcr/İşa bucağı çatmayanda gecə bölgüsünə keçir; bunun üçün **növbəti günün** günəş doğuşu
     * lazımdır, ona görə növbəti gün də doğmursa yenə null qaytarılır.
     */
    private fun astronomicalOrNightFraction(
        epochDay: Long,
        at: GeoPoint,
        params: PrayerParams,
    ): List<PrayerTime>? {
        val sun = PrayerMath.sunPosition(PrayerMath.julianDayFromEpochDay(epochDay, at.longitude))
        val transitHours = PrayerMath.transitUtcHours(at.longitude, sun.eqTimeMinutes)

        val sunriseHourAngle = PrayerMath.hourAngleDeg(
            latDeg = at.latitude,
            declDeg = sun.declinationDeg,
            altitudeDeg = horizonAltitude(at, params),
        ) ?: return null

        val transit = millisOf(epochDay, transitHours)
        val sunrise = millisOf(epochDay, transitHours - sunriseHourAngle / 15.0)
        val maghrib = millisOf(epochDay, transitHours + sunriseHourAngle / 15.0)

        // Əsr: bucaq çatmayanda (yalnız qütb qışının astanasında olur) Zöhr–Axşam aralığının ortası.
        // Bu, degenerativ haldır və `enforceOrder` onsuz da nəticəni etibarlı saxlayır.
        val asrAltitude = PrayerMath.asrAltitudeDeg(
            latDeg = at.latitude,
            declDeg = sun.declinationDeg,
            shadowFactor = params.asrShadowFactor,
        )
        val asrHourAngle = PrayerMath.hourAngleDeg(at.latitude, sun.declinationDeg, asrAltitude)
        val asr = if (asrHourAngle != null) {
            PrayerTime(Prayer.ASR, millisOf(epochDay, transitHours + asrHourAngle / 15.0), TimeSource.ASTRONOMICAL)
        } else {
            PrayerTime(Prayer.ASR, (transit + maghrib) / 2L, TimeSource.NIGHT_FRACTION)
        }

        // Gecə bölgüsü yalnız lazım olanda hesablanır — normal enliklərdə növbəti günə heç baxılmır.
        val nightMillis: Long? by lazy(LazyThreadSafetyMode.NONE) {
            nextSunriseMillis(epochDay + 1, at, params)?.let { it - maghrib }
        }

        val fajr = depressionTime(
            prayer = Prayer.FAJR,
            epochDay = epochDay,
            transitHours = transitHours,
            declDeg = sun.declinationDeg,
            latDeg = at.latitude,
            angle = params.fajrAngle,
            beforeTransit = true,
            anchor = sunrise,
            night = { nightMillis },
        ) ?: return null

        val isha = depressionTime(
            prayer = Prayer.ISHA,
            epochDay = epochDay,
            transitHours = transitHours,
            declDeg = sun.declinationDeg,
            latDeg = at.latitude,
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
        transitHours: Double,
        declDeg: Double,
        latDeg: Double,
        angle: Double,
        beforeTransit: Boolean,
        anchor: Long,
        night: () -> Long?,
    ): PrayerTime? {
        val hourAngle = PrayerMath.hourAngleDeg(latDeg, declDeg, -angle)

        if (hourAngle != null) {
            val hours = if (beforeTransit) transitHours - hourAngle / 15.0 else transitHours + hourAngle / 15.0
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

    private fun nextSunriseMillis(epochDay: Long, at: GeoPoint, params: PrayerParams): Long? {
        val sun = PrayerMath.sunPosition(PrayerMath.julianDayFromEpochDay(epochDay, at.longitude))
        val transitHours = PrayerMath.transitUtcHours(at.longitude, sun.eqTimeMinutes)
        val hourAngle = PrayerMath.hourAngleDeg(
            latDeg = at.latitude,
            declDeg = sun.declinationDeg,
            altitudeDeg = horizonAltitude(at, params),
        ) ?: return null

        return millisOf(epochDay, transitHours - hourAngle / 15.0)
    }

    // endregion

    // region — pillə 2

    /**
     * Qütb günü/gecəsi: günəşin doğduğu **ən yaxın** günü tapır və həmin günün vaxtlarının
     * transit-dən fərqini bugünkü transit-ə köçürür.
     *
     * Transit riyazi olaraq həmişə mövcuddur (günəşin ən yüksək nöqtəsi qütbdə də var), ona görə
     * Zöhr heç vaxt itmir və altı vaxt həmişə bir-birindən fərqli çıxır.
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

    /** Görünən üfüq — ayar sönülüdürsə dəniz səviyyəsi. Yalnız günəş doğuşu/Axşama təsir edir. */
    private fun horizonAltitude(at: GeoPoint, params: PrayerParams): Double =
        PrayerMath.horizonAltitudeDeg(if (params.useElevation) at.elevationMeters else 0.0)

    private fun transitMillis(epochDay: Long, at: GeoPoint): Long {
        val sun = PrayerMath.sunPosition(PrayerMath.julianDayFromEpochDay(epochDay, at.longitude))
        return millisOf(epochDay, PrayerMath.transitUtcHours(at.longitude, sun.eqTimeMinutes))
    }

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

    private fun millisOf(epochDay: Long, utcHours: Double): Long =
        epochDay * MILLIS_PER_DAY + (utcHours * MILLIS_PER_HOUR).roundToLong()

    // endregion
}
