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

    /**
     * Bucaq invariantları üçün tolerantlıq.
     *
     * `PrayerTimes` nəticəni **ən yaxın dəqiqəyə** yuvarlaqlaşdırır (`adhan` default davranışı), yəni
     * qaytarılan an həqiqi hadisədən ±30 saniyə fərqlənə bilər. 30 saniyə = 0.125° saat bucağı, ona
     * görə «hündürlük dəqiq −0.833°-dir» kimi yoxlama bu qədər boşluq buraxmalıdır. Tolerantlıq
     * hələ də dardır: işarə və ya vahid səhvi dərəcələrlə sapma verərdi, 0.2° ilə deyil.
     */
    const val ROUNDING_DEGREES = 0.2

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

    /**
     * [atMillis] anındakı günəş koordinatları — **hadisə anında**, günorta üçün deyil.
     *
     * ⚠️ Günorta üçün hesablanmış dəyərlə yoxlamaq yanlış olardı: `PrayerTimes` Meeus-un üç günlük
     * interpolyasiyası ilə hər hadisəni öz anına gətirir, ona görə invariant testi də eyni anı
     * götürməlidir.
     */
    fun solarAt(atMillis: Long): PrayerMath.SolarCoordinates {
        val epochDay = PrayerDay.utcEpochDay(atMillis)
        val hours = (atMillis - epochDay * MILLIS_PER_DAY) / MILLIS_PER_HOUR

        return PrayerMath.solarCoordinates(PrayerMath.julianDay(epochDay) + hours / 24.0)
    }

    /**
     * [atMillis] anının yerli saat bucağı (dərəcə, ±180) — mənfi = transitdən əvvəl.
     *
     * `PrayerTimes`-dan **tamamilə asılı olmayan** yol: `H = θ + λ − α`, hər üçü həmin anın
     * koordinatlarından.
     */
    fun hourAngleOf(at: GeoPoint, atMillis: Long): Double {
        val solar = solarAt(atMillis)
        val raw = solar.apparentSiderealTimeDeg + at.longitude - solar.rightAscensionDeg
        val wrapped = PrayerMath.unwindAngle(raw)

        return if (wrapped > 180.0) wrapped - 360.0 else wrapped
    }

    /** [atMillis] anında günəşin hündürlüyü (dərəcə). */
    fun altitudeAt(at: GeoPoint, atMillis: Long): Double = PrayerMath.altitudeOfCelestialBody(
        latitudeDeg = at.latitude,
        declDeg = solarAt(atMillis).declinationDeg,
        hourAngleDeg = hourAngleOf(at, atMillis),
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
