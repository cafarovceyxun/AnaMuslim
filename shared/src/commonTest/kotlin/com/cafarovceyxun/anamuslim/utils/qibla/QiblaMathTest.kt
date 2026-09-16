package com.cafarovceyxun.anamuslim.utils.qibla

import com.cafarovceyxun.anamuslim.utils.prayer.GeoPoint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Qiblə həndəsəsi.
 *
 * Testin ağırlığı **analitik invariantlardadır** — eyni meridianda bucağın dəqiq 0°/180° olması,
 * meridiana görə simmetriya, bir dərəcəlik meridian qövsünün uzunluğu, antipod məsafəsi. Bunların
 * hamısı kürə həndəsəsindən çıxır, ona görə heç bir kənar cədvəldən rəqəm köçürülmür və mənbənin
 * yuvarlaqlaşdırması testə sızmır ([com.cafarovceyxun.anamuslim.utils.prayer.PrayerMathTest] ilə
 * eyni prinsip).
 *
 * Sonda iki şəhər var, amma **başqa məqsədlə**: invariantlar düsturun arqument sırasını və işarəsini
 * tam tutmur (məsələn `from`/`to` yerdəyişməsi simmetriyanı pozmaya bilər). O iki sətir müstəqil
 * dərc olunmuş qiblə istiqamətləri ilə kobud tutuşdurmadır, tolerantlıq isə mənbənin
 * yuvarlaqlaşdırmasını udacaq qədər boşdur.
 */
class QiblaMathTest {

    /** Kəbənin meridianı üzərində, ondan şimalda. */
    private val northOfKaaba = GeoPoint(latitude = 40.0, longitude = QiblaMath.KAABA.longitude)

    /** Kəbənin meridianı üzərində, ondan cənubda (ekvator). */
    private val southOfKaaba = GeoPoint(latitude = 0.0, longitude = QiblaMath.KAABA.longitude)

    @Test
    fun bearingIsDueSouthFromTheSameMeridianNorth() {
        val bearing = QiblaMath.bearingToKaaba(northOfKaaba)

        assertTrue(abs(bearing - 180.0) < 1e-9, "alındı $bearing")
    }

    @Test
    fun bearingIsDueNorthFromTheSameMeridianSouth() {
        val bearing = QiblaMath.bearingToKaaba(southOfKaaba)

        assertTrue(abs(bearing) < 1e-9, "alındı $bearing")
    }

    @Test
    fun bearingsMirrorAcrossTheKaabaMeridian() {
        // Meridiandan eyni məsafədə qərbdə və şərqdə duran iki nöqtə güzgü bucaqları verməlidir:
        // cəmləri 360°. Bu, `deltaLng`-in işarəsinin düzgün işləndiyini göstərir.
        val west = GeoPoint(latitude = 40.0, longitude = QiblaMath.KAABA.longitude - 10.0)
        val east = GeoPoint(latitude = 40.0, longitude = QiblaMath.KAABA.longitude + 10.0)

        val sum = QiblaMath.bearingToKaaba(west) + QiblaMath.bearingToKaaba(east)

        assertTrue(abs(sum - 360.0) < 1e-9, "cəm $sum")
    }

    @Test
    fun bearingStaysInsideOneTurn() {
        // Uzunluq boyu dolanarkən nəticə heç vaxt aralıqdan çıxmamalıdır — `normalizeDegrees`
        // mənfi `atan2` çıxışını düzgün bükür.
        for (lngStep in -18..18) {
            for (latStep in -8..8) {
                val point = GeoPoint(latitude = latStep * 10.0, longitude = lngStep * 10.0)
                val bearing = QiblaMath.bearingToKaaba(point)

                assertTrue(bearing >= 0.0 && bearing < 360.0, "$point → $bearing")
            }
        }
    }

    @Test
    fun distanceIsZeroAtTheKaaba() {
        val distance = QiblaMath.distanceToKaabaMeters(QiblaMath.KAABA)

        assertTrue(distance < 1e-6, "alındı $distance")
    }

    @Test
    fun distanceMatchesOneDegreeOfMeridianArc() {
        // Eyni meridianda bir dərəcə = R·π/180. Haversine-in miqyasını yoxlayır.
        val oneDegreeNorth = GeoPoint(
            latitude = QiblaMath.KAABA.latitude + 1.0,
            longitude = QiblaMath.KAABA.longitude,
        )
        val expected = 6_371_008.8 * PI / 180.0

        val distance = QiblaMath.distanceToKaabaMeters(oneDegreeNorth)

        assertTrue(abs(distance - expected) < 0.5, "alındı $distance, gözlənilən $expected")
    }

    @Test
    fun distanceToTheAntipodeIsHalfTheGreatCircle() {
        // Antipodda `a` yuvarlaqlaşma ucbatından 1-i keçə bilər; `coerceAtMost` olmasa NaN gələrdi.
        val antipode = GeoPoint(
            latitude = -QiblaMath.KAABA.latitude,
            longitude = QiblaMath.KAABA.longitude - 180.0,
        )
        val expected = 6_371_008.8 * PI

        val distance = QiblaMath.distanceToKaabaMeters(antipode)

        assertTrue(abs(distance - expected) < 1.0, "alındı $distance, gözlənilən $expected")
        assertTrue(!distance.isNaN(), "NaN gəldi")
    }

    @Test
    fun normalizeDegreesWrapsBothWays() {
        assertTrue(abs(QiblaMath.normalizeDegrees(-1.0) - 359.0) < 1e-9)
        assertTrue(abs(QiblaMath.normalizeDegrees(361.0) - 1.0) < 1e-9)
        assertTrue(abs(QiblaMath.normalizeDegrees(-721.0) - 359.0) < 1e-9)
        assertTrue(abs(QiblaMath.normalizeDegrees(0.0)) < 1e-9)
    }

    @Test
    fun signedDeltaTakesTheShortWayAroundZero() {
        // 350° → 10° iyirmi dərəcə sağadır, −340° yox. İynənin şimaldan keçəndə tam dövrə
        // vurmamasının səbəbi budur.
        assertTrue(abs(QiblaMath.signedDeltaDegrees(350.0, 10.0) - 20.0) < 1e-9)
        assertTrue(abs(QiblaMath.signedDeltaDegrees(10.0, 350.0) + 20.0) < 1e-9)
        assertTrue(abs(QiblaMath.signedDeltaDegrees(0.0, 180.0) - 180.0) < 1e-9)
        assertTrue(abs(QiblaMath.signedDeltaDegrees(0.0, 181.0) + 179.0) < 1e-9)
    }

    @Test
    fun matchesIndependentlyPublishedDirections() {
        // Kobud nəzarət: dərc olunmuş qiblə istiqamətləri onluq hissədə yuvarlaqlaşdırılıb, ona görə
        // tolerantlıq boşdur. Səhv düstur dərəcələrlə yanılardı, bu hədd isə onluq hissəni buraxır.
        val istanbul = GeoPoint(latitude = 41.0082, longitude = 28.9784)
        val newYork = GeoPoint(latitude = 40.7128, longitude = -74.0060)

        val istanbulBearing = QiblaMath.bearingToKaaba(istanbul)
        val newYorkBearing = QiblaMath.bearingToKaaba(newYork)

        assertTrue(abs(istanbulBearing - 151.6) < 0.15, "İstanbul $istanbulBearing")
        assertTrue(abs(newYorkBearing - 58.5) < 0.15, "Nyu-York $newYorkBearing")
    }
}
