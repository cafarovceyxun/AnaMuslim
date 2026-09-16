package com.cafarovceyxun.anamuslim.utils.qibla

import com.cafarovceyxun.anamuslim.utils.prayer.GeoPoint
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Kompas oxunuşundan qiblə vəziyyətinə keçid.
 *
 * Buradakı qərarlar səhv salınmağa ən açıq olanlardır: sapmanın hansı mənbədən götürülməsi
 * (platforma → model → heç nə), şimaldan keçidin hamarlanması və müdaxilə həddi. Hamısı saf
 * funksiyadır, ona görə platformasız yoxlanılır.
 */
class QiblaHeadingTest {

    private val baku = GeoPoint(latitude = 40.4093, longitude = 49.8671)

    /** 2026-06-01 — model aralığının içində. */
    private val insideModel = 1_780_272_000_000L

    /** 2031-01-01 — model köhnəlib. */
    private val afterModel = 1_925_078_400_000L

    private fun reading(
        magnetic: Double = 0.0,
        trueHeading: Double? = null,
        field: Double? = null,
    ) = CompassReading(
        magneticHeadingDeg = magnetic,
        trueHeadingDeg = trueHeading,
        accuracyDeg = null,
        fieldStrengthNanoTesla = field,
        calibration = CompassCalibration.OK,
    )

    @Test
    fun platformTrueHeadingWinsOverEverythingElse() {
        // iOS yolu: `trueHeading` gəlibsə sapma bir daha tətbiq olunmamalıdır — yoxsa ikiqat
        // düzəliş edilir və kompas sapma qədər tərs istiqamətə yanılır.
        val state = QiblaHeading.resolve(
            reading = reading(magnetic = 100.0, trueHeading = 106.0),
            point = baku,
            atMillis = insideModel,
            platformDeclination = 6.0,
        )

        assertEquals(HeadingReference.PLATFORM_TRUE, state.reference)
        assertTrue(abs(state.trueHeadingDeg - 106.0) < 1e-9, "alındı ${state.trueHeadingDeg}")
    }

    @Test
    fun platformDeclinationIsAppliedWhenTrueHeadingIsMissing() {
        // Android yolu.
        val state = QiblaHeading.resolve(
            reading = reading(magnetic = 100.0),
            point = baku,
            atMillis = insideModel,
            platformDeclination = 6.5,
        )

        assertEquals(HeadingReference.PLATFORM_DECLINATION, state.reference)
        assertTrue(abs(state.trueHeadingDeg - 106.5) < 1e-9, "alındı ${state.trueHeadingDeg}")
    }

    @Test
    fun modelDeclinationIsUsedWhenThePlatformHasNone() {
        // iOS-da mövqe icazəsi verilməyibsə bura düşülür.
        val expected = GeomagneticModel.declinationDeg(baku, insideModel)

        val state = QiblaHeading.resolve(
            reading = reading(magnetic = 100.0),
            point = baku,
            atMillis = insideModel,
            platformDeclination = null,
        )

        assertEquals(HeadingReference.MODEL_DECLINATION, state.reference)
        assertTrue(
            abs(state.trueHeadingDeg - (100.0 + expected)) < 1e-9,
            "alındı ${state.trueHeadingDeg}, gözlənilən ${100.0 + expected}",
        )
        // Bakıda sapma sıfır deyil — düzəlişin həqiqətən tətbiq olunduğunun sübutu.
        assertTrue(abs(expected) > 3.0, "Bakı sapması gözlənilməz dərəcədə kiçikdir: $expected")
    }

    @Test
    fun expiredModelFallsBackToMagneticNorthAndSaysSo() {
        val state = QiblaHeading.resolve(
            reading = reading(magnetic = 100.0),
            point = baku,
            atMillis = afterModel,
            platformDeclination = null,
        )

        assertEquals(HeadingReference.MAGNETIC_ONLY, state.reference)
        assertTrue(abs(state.trueHeadingDeg - 100.0) < 1e-9, "alındı ${state.trueHeadingDeg}")
    }

    @Test
    fun deltaPointsTheShortestWayToQibla() {
        val bearing = QiblaMath.bearingToKaaba(baku)

        val state = QiblaHeading.resolve(
            reading = reading(magnetic = 0.0, trueHeading = bearing),
            point = baku,
            atMillis = insideModel,
            platformDeclination = null,
        )

        assertTrue(abs(state.deltaDeg) < 1e-9, "qibləyə baxarkən fərq sıfır olmalıdır: ${state.deltaDeg}")
        assertTrue(state.isAligned)
    }

    @Test
    fun alignmentUsesTheDocumentedThreshold() {
        val bearing = QiblaMath.bearingToKaaba(baku)

        fun alignedAt(offset: Double) = QiblaHeading.resolve(
            reading = reading(trueHeading = QiblaMath.normalizeDegrees(bearing + offset)),
            point = baku,
            atMillis = insideModel,
            platformDeclination = null,
        ).isAligned

        assertTrue(alignedAt(QiblaHeading.ALIGNED_THRESHOLD_DEG - 0.1))
        assertTrue(alignedAt(-(QiblaHeading.ALIGNED_THRESHOLD_DEG - 0.1)))
        assertFalse(alignedAt(QiblaHeading.ALIGNED_THRESHOLD_DEG + 0.1))
        assertFalse(alignedAt(-(QiblaHeading.ALIGNED_THRESHOLD_DEG + 0.1)))
    }

    @Test
    fun interferenceIsFlaggedOnlyWhenTheFieldIsReallyOff() {
        val expected = GeomagneticModel.totalIntensityNanoTesla(baku, insideModel)

        // Gözləniləndən iki dəfə güclü sahə — yaxınlıqda metal.
        assertTrue(
            QiblaHeading.hasInterference(reading(field = expected * 2.0), baku, insideModel),
        )
        // Normal sahə.
        assertFalse(
            QiblaHeading.hasInterference(reading(field = expected), baku, insideModel),
        )
        // Sensor sahə gücü vermirsə yalan xəbərdarlıq verilmir.
        assertFalse(
            QiblaHeading.hasInterference(reading(field = null), baku, insideModel),
        )
    }

    @Test
    fun smootherDoesNotSpinTheNeedleAcrossNorth() {
        // Ən klassik səhv: 359° → 1° keçidi ədəd olaraq −358-dir, ona görə adi filtr iynəni
        // bütün kadran boyu geri fırladır. Nəticə həmişə sıfırın yaxınlığında qalmalıdır.
        val smoother = AngleSmoother(alpha = 0.3)
        smoother.next(359.0)

        repeat(20) { index ->
            val value = smoother.next(if (index % 2 == 0) 1.0 else 359.0)
            val distanceFromNorth = abs(QiblaMath.signedDeltaDegrees(0.0, value))

            assertTrue(distanceFromNorth < 15.0, "iynə şimaldan qopdu: $value")
        }
    }

    @Test
    fun smootherTakesTheFirstReadingAsIs() {
        // Sıfırdan yığılsa iynə ekran açılanda şimaldan sürünərək gələrdi.
        val smoother = AngleSmoother()

        assertTrue(abs(smoother.next(217.0) - 217.0) < 1e-9)
    }

    @Test
    fun smootherConvergesOnASteadyReading() {
        val smoother = AngleSmoother(alpha = 0.3)
        smoother.next(0.0)
        repeat(60) { smoother.next(90.0) }

        val settled = smoother.next(90.0)

        assertTrue(abs(settled - 90.0) < 0.5, "alındı $settled")
    }

    @Test
    fun smootherResetsForgetsThePastAngle() {
        val smoother = AngleSmoother(alpha = 0.1)
        repeat(30) { smoother.next(10.0) }
        smoother.reset()

        assertTrue(abs(smoother.next(200.0) - 200.0) < 1e-9)
    }
}
