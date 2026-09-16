package com.cafarovceyxun.anamuslim.utils.qibla

import com.cafarovceyxun.anamuslim.utils.prayer.GeoPoint
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Həqiqi şimalın haradan gəldiyi. UI bunu istifadəçiyə dəqiqlik xəbərdarlığı kimi göstərir. */
enum class HeadingReference {
    /** Platformanın öz həqiqi şimalı (iOS `trueHeading`). Ən yaxşı hal. */
    PLATFORM_TRUE,

    /** Maqnit şimalı + platformanın sapma hesabı (Android `GeomagneticField`). */
    PLATFORM_DECLINATION,

    /** Maqnit şimalı + tətbiqin öz [GeomagneticModel] hesabı. */
    MODEL_DECLINATION,

    /**
     * Düzəliş **ümumiyyətlə tətbiq olunmayıb** — yalnız maqnit şimalı.
     *
     * Bura yalnız model də köhnəlibsə düşülür. Bakıda bu, ~6° səhv deməkdir, ona görə UI
     * istifadəçiyə açıq xəbərdarlıq verməlidir.
     */
    MAGNETIC_ONLY,
}

/** Kompas rejiminin bir andakı tam vəziyyəti. */
data class QiblaHeadingState(
    /** Cihazın baxdığı istiqamət, həqiqi şimaldan. */
    val trueHeadingDeg: Double,
    /** Qiblənin istiqaməti, həqiqi şimaldan. */
    val qiblaBearingDeg: Double,
    /** Cihazı qibləyə çatdırmaq üçün neçə dərəcə dönmək lazımdır, `−180..180` (müsbət = sağa). */
    val deltaDeg: Double,
    val isAligned: Boolean,
    val reference: HeadingReference,
    /** Yaxınlıqda maqnit müdaxiləsi aşkarlanıbmı. */
    val hasInterference: Boolean,
    val calibration: CompassCalibration,
)

/**
 * Sensor oxunuşunu qiblə vəziyyətinə çevirən **saf** məntiq.
 *
 * Ayrıca obyektdir ki, ViewModel-siz test oluna bilsin: burada sapmanın hansı mənbədən götürülməsi,
 * şimaldan keçidin hamarlanması və müdaxilə həddi kimi səhv salınması asan qərarlar cəmlənib.
 */
object QiblaHeading {

    /** Bu həddin içində istifadəçi qibləyə yönəlmiş sayılır (haptik + vizual təsdiq). */
    const val ALIGNED_THRESHOLD_DEG = 5.0

    /**
     * Ölçülən sahə gücü gözləniləndən bu nisbətdə fərqlənirsə müdaxilə var sayılır.
     *
     * 35% qəsdən boşdur: sensorun öz kalibrasiya sürüşməsi 10–20%-ə çata bilir, yəni dar hədd
     * xəbərdarlığı daim yandırardı və istifadəçi ona məhəl qoymazdı.
     */
    const val INTERFERENCE_RATIO = 0.35

    /**
     * Oxunuşu vəziyyətə çevirir.
     *
     * [platformDeclination] ayrıca parametrdir (seam-i birbaşa çağırmır) ki, test platformasız
     * işləsin — hər iki budaq, yəni Android və iOS yolu, eyni funksiyada yoxlana bilsin.
     */
    fun resolve(
        reading: CompassReading,
        point: GeoPoint,
        atMillis: Long,
        platformDeclination: Double?,
    ): QiblaHeadingState {
        val (trueHeading, reference) = resolveTrueHeading(
            reading = reading,
            point = point,
            atMillis = atMillis,
            platformDeclination = platformDeclination,
        )

        val bearing = QiblaMath.bearingToKaaba(point)
        val delta = QiblaMath.signedDeltaDegrees(from = trueHeading, to = bearing)

        return QiblaHeadingState(
            trueHeadingDeg = trueHeading,
            qiblaBearingDeg = bearing,
            deltaDeg = delta,
            isAligned = abs(delta) <= ALIGNED_THRESHOLD_DEG,
            reference = reference,
            hasInterference = hasInterference(reading, point, atMillis),
            calibration = reading.calibration,
        )
    }

    /**
     * Həqiqi şimalı **platforma əsas, model ehtiyat** qaydası ilə seçir.
     *
     * Sıra: platformanın hazır həqiqi şimalı → platformanın sapması → tətbiqin WMM modeli →
     * (model də köhnəlibsə) düzəlişsiz maqnit şimalı.
     */
    private fun resolveTrueHeading(
        reading: CompassReading,
        point: GeoPoint,
        atMillis: Long,
        platformDeclination: Double?,
    ): Pair<Double, HeadingReference> {
        reading.trueHeadingDeg?.let {
            return QiblaMath.normalizeDegrees(it) to HeadingReference.PLATFORM_TRUE
        }

        platformDeclination?.let {
            return QiblaMath.normalizeDegrees(reading.magneticHeadingDeg + it) to
                HeadingReference.PLATFORM_DECLINATION
        }

        if (!GeomagneticModel.isExpired(atMillis)) {
            val declination = GeomagneticModel.declinationDeg(point, atMillis)

            return QiblaMath.normalizeDegrees(reading.magneticHeadingDeg + declination) to
                HeadingReference.MODEL_DECLINATION
        }

        return QiblaMath.normalizeDegrees(reading.magneticHeadingDeg) to
            HeadingReference.MAGNETIC_ONLY
    }

    /**
     * Ölçülən sahə gücünü modelin gözlədiyi ilə tutuşdurur.
     *
     * Sensor sahə gücü vermirsə (bəzi cihazlarda maqnitometr xam dəyəri açmır) **false** qaytarılır:
     * yalan xəbərdarlıq verməkdənsə heç nə deməmək yaxşıdır.
     */
    fun hasInterference(reading: CompassReading, point: GeoPoint, atMillis: Long): Boolean {
        val measured = reading.fieldStrengthNanoTesla ?: return false
        if (measured <= 0.0) return false

        val expected = GeomagneticModel.totalIntensityNanoTesla(point, atMillis)
        if (expected <= 0.0) return false

        return abs(measured - expected) / expected > INTERFERENCE_RATIO
    }
}

/**
 * Bucaq hamarlayıcısı — **dairə üzərində**.
 *
 * ⚠️ Bucağı birbaşa filtrləmək olmaz: 359°-dən 1°-ə keçid ədəd olaraq **−358**-dir, ona görə adi
 * alçaq-tezlikli filtr iynəni şimaldan keçəndə bütün kadran boyu geri fırladır. Ona görə sinus və
 * kosinus **ayrı-ayrı** filtrlənir, bucaq isə sondan `atan2` ilə çıxarılır.
 *
 * Instansiya bir ekrana aiddir (vəziyyət daşıyır), ona görə `object` deyil.
 */
class AngleSmoother(
    /**
     * Filtrin ağırlığı: kiçik = hamar amma gec, böyük = cəld amma titrək.
     *
     * 0.15 sensorun ~50 Hz axınında gözlə hamar görünür və eyni zamanda telefonu çevirəndə geri
     * qalmır.
     */
    private val alpha: Double = 0.15,
) {
    private var sinValue = 0.0
    private var cosValue = 0.0
    private var seeded = false

    /** Yeni oxunuşu əlavə edib hamarlanmış bucağı qaytarır, `0..360`. */
    fun next(degrees: Double): Double {
        val radians = degrees * PI_OVER_180
        val newSin = sin(radians)
        val newCos = cos(radians)

        if (!seeded) {
            // İlk oxunuş olduğu kimi qəbul olunur — sıfırdan yığılsa iynə açılışda şimaldan
            // sürünərək gələrdi.
            sinValue = newSin
            cosValue = newCos
            seeded = true
        } else {
            sinValue += alpha * (newSin - sinValue)
            cosValue += alpha * (newCos - cosValue)
        }

        return QiblaMath.normalizeDegrees(atan2(sinValue, cosValue) / PI_OVER_180)
    }

    /** Vəziyyəti sıfırlayır — ekran yenidən açılanda iynə keçmiş dəyərdən sürünməsin. */
    fun reset() {
        seeded = false
    }

    private companion object {
        const val PI_OVER_180 = kotlin.math.PI / 180.0
    }
}
