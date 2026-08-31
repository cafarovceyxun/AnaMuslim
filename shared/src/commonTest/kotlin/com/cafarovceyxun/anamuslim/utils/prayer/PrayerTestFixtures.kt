package com.cafarovceyxun.anamuslim.utils.prayer

import com.cafarovceyxun.anamuslim.utils.IsoDate

/**
 * Namaz testlərinin ortaq köməkçiləri.
 *
 * ⚠️ Heç bir yerdə `assertEquals` **Double üzərində** işlədilmir: JVM və Kotlin/Native fərqli
 * `libm` istifadə edir, nəticələr 1–2 ULP fərqlənir. Bütün müqayisələr tolerantlıqladır.
 */
internal object Fx {

    const val MILLIS_PER_DAY = 86_400_000L
    const val MILLIS_PER_HOUR = 3_600_000.0

    /** Vaxt müqayisələri üçün standart tolerantlıq. Cədvəllər onsuz da dəqiqəyə yuvarlaqlaşır. */
    const val ONE_MINUTE = 60_000L

    val BAKU = GeoPoint(40.4093, 49.8671)
    val PARIS = GeoPoint(48.8566, 2.3522)
    val MOSCOW = GeoPoint(55.7558, 37.6173)
    val ST_PETERSBURG = GeoPoint(59.9375, 30.3086)
    val MURMANSK = GeoPoint(68.9585, 33.0827)
    val GREENWICH = GeoPoint(51.4779, 0.0)
    val EQUATOR = GeoPoint(0.0, 20.0)
    val JAKARTA = GeoPoint(-6.2088, 106.8456)
    val SYDNEY = GeoPoint(-33.8688, 151.2093)

    val DEFAULT = PrayerParams()

    /** [dateIso] gününün günəş vəziyyəti — invariant testləri bunu müstəqil hesablayır. */
    fun sun(dateIso: String, at: GeoPoint): PrayerMath.SunPosition {
        val epochDay = IsoDate.toEpochDay(dateIso)!!
        return PrayerMath.sunPosition(PrayerMath.julianDayFromEpochDay(epochDay, at.longitude))
    }

    /** [atMillis] anının həmin gün üçün saat bucağı (dərəcə) — mənfi = transitdən əvvəl. */
    fun hourAngleOf(dateIso: String, at: GeoPoint, atMillis: Long): Double {
        val epochDay = IsoDate.toEpochDay(dateIso)!!
        val transitHours = PrayerMath.transitUtcHours(at.longitude, sun(dateIso, at).eqTimeMinutes)
        val hours = (atMillis - epochDay * MILLIS_PER_DAY) / MILLIS_PER_HOUR

        return (hours - transitHours) * 15.0
    }

    /** [atMillis] anında günəşin hündürlüyü (dərəcə) — `PrayerTimes`-dan **asılı olmayan** yoxlama. */
    fun altitudeAt(dateIso: String, at: GeoPoint, atMillis: Long): Double = PrayerMath.altitudeDeg(
        latDeg = at.latitude,
        declDeg = sun(dateIso, at).declinationDeg,
        hourAngleDeg = hourAngleOf(dateIso, at, atMillis),
    )

    /** UTC anını `yyyy-MM-dd` gününə çevirən saxta qurşaq — [PrayerDay] testləri üçün. */
    fun fakeLocalDate(offsetHours: Int): (Long) -> String = { millis ->
        IsoDate.fromEpochDay(PrayerDay.floorDiv(millis + offsetHours * 3_600_000L, MILLIS_PER_DAY))
    }

    /** `yyyy-MM-dd HH:mm:ss` verən saxta formatlayıcı — [PrayerDay.deviceUtcOffsetSeconds] üçün. */
    fun fakeFormatter(offsetHours: Int): (Long) -> String = { millis ->
        val shifted = millis + offsetHours * 3_600_000L
        val epochDay = PrayerDay.floorDiv(shifted, MILLIS_PER_DAY)
        val rest = shifted - epochDay * MILLIS_PER_DAY
        val hour = (rest / 3_600_000L).toInt()
        val minute = ((rest % 3_600_000L) / 60_000L).toInt()
        val second = ((rest % 60_000L) / 1_000L).toInt()

        IsoDate.fromEpochDay(epochDay) + " " +
            hour.toString().padStart(2, '0') + ":" +
            minute.toString().padStart(2, '0') + ":" +
            second.toString().padStart(2, '0')
    }

    fun times(dateIso: String, at: GeoPoint, params: PrayerParams = DEFAULT): PrayerDayTimes =
        PrayerTimes.calculate(dateIso, at, params)!!

    fun settings(
        at: GeoPoint = BAKU,
        notify: Set<Prayer> = setOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA),
        params: PrayerParams = DEFAULT,
        enabled: Boolean = true,
    ) = PrayerSettings(enabled = enabled, point = at, placeName = "Test", params = params, notify = notify)
}
