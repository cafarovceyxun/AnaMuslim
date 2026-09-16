package com.cafarovceyxun.anamuslim.utils.qibla

import com.cafarovceyxun.anamuslim.utils.prayer.GeoPoint
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerMath
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** Günəşin bir andakı mövqeyi. Azimut **həqiqi şimaldan** saat əqrəbi ilə, `0°..360°`. */
data class SunPosition(
    val azimuthDeg: Double,
    /** Üfüqdən hündürlük, dərəcə. Mənfi olanda günəş batıb. */
    val altitudeDeg: Double,
) {
    /**
     * Günəş görünürmü.
     *
     * Hədd sıfır deyil, **+3°**-dir: üfüqə yaxın günəş binaların, dağların və atmosfer
     * refraksiyasının arxasında qalır, yəni istifadəçi onu nişanla üst-üstə sala bilmir.
     * Yalan təklif verməkdənsə göstərməmək yaxşıdır.
     */
    val isUsable: Boolean get() = altitudeDeg > MIN_USABLE_ALTITUDE_DEG

    private companion object {
        const val MIN_USABLE_ALTITUDE_DEG = 3.0
    }
}

/**
 * Günəş kompası — maqnitometrdən **tamamilə asılı olmayan** istiqamət yoxlaması.
 *
 * ### Nəyə lazımdır
 * Maqnit sensoru kalibrsiz olanda, yaxınlıqda metal olanda və ya cihazda ümumiyyətlə maqnitometr
 * olmayanda kompas **inamla səhv** göstərir — bunu istifadəçiyə sübut edən yeganə əlçatan istinad
 * göydədir. Ekran halqada günəş nişanı çəkir; istifadəçi telefonu elə tutur ki, nişan həqiqi
 * günəşə baxsın. Uyğun gəlmirsə kompas yanılır və fərq gözlə ölçülür.
 *
 * ### Niyə ayrı obyekt, amma öz astronomiyası yoxdur
 * Bütün ağır hesab [PrayerMath]-dadır (Meeus) və namaz vaxtları ilə **eyni** koddan keçir — ayrıca
 * günəş modeli yazmaq iki fərqli nəticə riski deməkdir. Burada yalnız ekvatorial koordinatlardan
 * horizontal koordinatlara keçid var.
 *
 * ⚠️ [PrayerMath.julianDay] **0h UT** üçündür (namaz vaxtları bir günü hesablayır). Burada isə
 * **anlıq** Julian günü lazımdır: `apparentSiderealTimeDeg` verilən JD-nin özünə görə hesablanır,
 * ona görə gün kəsri ötürülməsə günəş bütün gün səhər mövqeyində donub qalar.
 */
object SunCompass {

    private const val DEG = 180.0 / PI
    private const val RAD = PI / 180.0
    private const val MILLIS_PER_DAY = 86_400_000.0

    /** Unix epoxasının (1970-01-01 00:00 UT) Julian günü. */
    private const val UNIX_EPOCH_JULIAN_DAY = 2_440_587.5

    /**
     * Verilmiş an və nöqtə üçün günəşin mövqeyi.
     *
     * [atMillis] **UTC**-dir; yerli saat qurşağı girmir, çünki saat bucağı birbaşa uzunluqdan
     * çıxarılır. Dəqiqlik onlarla il üçün qövs dəqiqəsi səviyyəsindədir — nişanı gözlə günəşlə
     * tutuşdurmaq üçün lazım olandan qat-qat artıq.
     */
    fun at(point: GeoPoint, atMillis: Long): SunPosition {
        val julianDay = atMillis / MILLIS_PER_DAY + UNIX_EPOCH_JULIAN_DAY
        val solar = PrayerMath.solarCoordinates(julianDay)

        // Yerli saat bucağı: görünən ulduz vaxtı + uzunluq − düz qalxma.
        val hourAngle = (
            solar.apparentSiderealTimeDeg + point.longitude - solar.rightAscensionDeg
            ) * RAD
        val latitude = point.latitude * RAD
        val declination = solar.declinationDeg * RAD

        // Düstur cənubdan qərbə doğru bucaq verir; +180° onu şimaldan saat əqrəbinə çevirir.
        val azimuth = atan2(
            sin(hourAngle),
            cos(hourAngle) * sin(latitude) - tan(declination) * cos(latitude),
        ) * DEG + 180.0

        val altitude = asin(
            sin(latitude) * sin(declination) +
                cos(latitude) * cos(declination) * cos(hourAngle),
        ) * DEG

        return SunPosition(
            azimuthDeg = QiblaMath.normalizeDegrees(azimuth),
            altitudeDeg = altitude,
        )
    }
}
