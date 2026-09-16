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
    /**
     * İstiqamətin təxmini xətası, **± dərəcə** — və ya heç bir mənbə bilmirsə null.
     *
     * UI bunu Kəbə nişanının ətrafında yay kimi çəkir: yay [ALIGNED_THRESHOLD_DEG] pəncərəsindən
     * genişdirsə, «düzləndim» hissi yalandır. Mənbəsi üçün bax [QiblaHeading.resolveAccuracy].
     */
    val accuracyDeg: Double?,
    /**
     * [accuracyDeg] **ölçülmüş** dəyərdirmi, yoxsa kalibrasiya sinfindən çıxarılmış təxmin.
     *
     * UI rəqəmi yalnız ölçülmüş olanda yazır — uydurulmuş «±10°»-u ölçmə kimi göstərmək
     * istifadəçiyə olmayan dəqiqlik vəd etməkdir.
     */
    val accuracyIsMeasured: Boolean,
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
     * Kalibrasiya sinfinin ± dərəcə ekvivalentləri — platforma rəqəm vermədikdə.
     *
     * Android `SENSOR_STATUS_ACCURACY_HIGH`/`MEDIUM`-u [CompassCalibration.OK] kimi birləşdirir,
     * ona görə «kalibrlənmiş» dəyəri nikbin deyil: MEDIUM real olaraq 10–15° verə bilər.
     */
    const val ACCURACY_CALIBRATED_DEG = 10.0
    const val ACCURACY_LOW_DEG = 25.0
    const val ACCURACY_UNRELIABLE_DEG = 45.0

    /** Bu xətadan geniş yay «düzləndim» hissini etibarsız edir — UI xəbərdarlıq göstərir. */
    const val ACCURACY_WEAK_DEG = 18.0

    /** Bundan kiçik «xəta» ölçmə deyil — bax [measuredAccuracy]. */
    const val MIN_CREDIBLE_ACCURACY_DEG = 1.0

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
            accuracyDeg = resolveAccuracy(reading),
            accuracyIsMeasured = measuredAccuracy(reading) != null,
        )
    }

    /**
     * Platformanın **həqiqətən ölçdüyü** xəta, və ya etibarlı dəyər yoxdursa null.
     *
     * ⚠️ **Sıfır «mükəmməl dəqiqlik» demək deyil, «doldurmadım» deməkdir.** Android-də rotation
     * vector-un beşinci dəyəri opsionaldır və bir çox cihaz onu sadəcə `0`-la doldurur — Samsung
     * A55-də cihazda ölçüldü: ekranda «Dəqiqlik ±0°» yazılırdı və qeyri-müəyyənlik yayı
     * ümumiyyətlə çəkilmirdi. Telefon maqnitometrinin bir dərəcədən yaxşı olması fiziki olaraq
     * mümkün deyil, ona görə belə dəyər ölçmə sayılmır və kalibrasiya sinfinə düşülür.
     */
    internal fun measuredAccuracy(reading: CompassReading): Double? =
        reading.accuracyDeg?.takeIf { it > MIN_CREDIBLE_ACCURACY_DEG }

    /**
     * Sensorun öz xəta təxmini, ± dərəcə.
     *
     * Birinci mənbə platformanın verdiyi **rəqəmdir**: iOS `CLHeading.headingAccuracy`, Android isə
     * rotation vector-un beşinci dəyəri. ⚠️ Android-də bu dəyər API 18-dən bəri **opsionaldır** —
     * bir çox cihaz onu ümumiyyətlə doldurmur, ona görə rəqəm gəlməyəndə kalibrasiya sinfi kobud
     * ekvivalentə çevrilir. Bu ekvivalent dəqiq deyil və elə olmaq da iddiasında deyil: UI onu
     * yalnız **böyüklük sırası** kimi göstərir (yay dardır/genişdir), rəqəmi isə ekrana yazır ki,
     * istifadəçi onun təxmin olduğunu görsün.
     *
     * [CompassCalibration.UNKNOWN] null qalır — «bilmirəm» ilə «yaxşıdır» arasındakı fərq burada
     * vacibdir, çünki ilk oxunuşlar həmişə UNKNOWN gəlir və o anda yalan dar yay çəkmək olmaz.
     */
    internal fun resolveAccuracy(reading: CompassReading): Double? =
        measuredAccuracy(reading)
            ?: when (reading.calibration) {
                CompassCalibration.OK -> ACCURACY_CALIBRATED_DEG
                CompassCalibration.LOW -> ACCURACY_LOW_DEG
                CompassCalibration.UNRELIABLE -> ACCURACY_UNRELIABLE_DEG
                CompassCalibration.UNKNOWN -> null
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
