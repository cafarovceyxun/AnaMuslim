package com.cafarovceyxun.anamuslim.utils.qibla

import com.cafarovceyxun.anamuslim.utils.prayer.GeoPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.CoreLocation.CLDeviceOrientation
import platform.CoreLocation.CLDeviceOrientationLandscapeLeft
import platform.CoreLocation.CLDeviceOrientationLandscapeRight
import platform.CoreLocation.CLDeviceOrientationPortrait
import platform.CoreLocation.CLDeviceOrientationPortraitUpsideDown
import platform.CoreLocation.CLHeading
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.darwin.NSObject
import kotlin.math.sqrt

actual fun isCompassAvailable(): Boolean = CLLocationManager.headingAvailable()

/**
 * `CLHeading` axını.
 *
 * ⚠️ **Mövqe icazəsi tələb olunmur.** `startUpdatingHeading()` icazəsiz də işləyir — sadəcə
 * `trueHeading` gəlmir (`−1`). Məhz buna görə [GeomagneticModel] ehtiyatı real yoldur, nəzəri
 * deyil: istifadəçi mövqe icazəsi verməsə də şəhəri əl ilə seçibsə kompas düzgün işləyir.
 *
 * ⚠️ **Simulyatorda maqnitometr yoxdur** — [isCompassAvailable] `false` qaytarır və bura heç vaxt
 * çatılmır. Kompası yalnız real cihazda yoxlamaq olar.
 */
actual fun compassReadings(): Flow<CompassReading> = callbackFlow {
    IosCompass.start { reading -> trySend(reading) }

    awaitClose { IosCompass.stop() }
}

/**
 * Tək `CLLocationManager` və onun delegate-i.
 *
 * ⚠️ Menecer **fayl səviyyəsində** saxlanılır: `CLLocationManager` delegate-inə **zəif** istinad
 * tutur, ona görə lokal dəyişəndə yaradılan menecer toplanır və `startUpdatingHeading()`
 * **səssizcə heç nə etmir** — nə xəta, nə log. Eyni tələ `DeviceLocation.ios.kt`-də də
 * sənədləşdirilib.
 *
 * Mövqe menecerindən ayrıdır: o, bir dəfəlik `requestLocation()` axınını idarə edir, bu isə davamlı
 * istiqamət axınını — ikisini bir delegate-də birləşdirmək hər iki tərəfi kövrək edərdi.
 */
private object IosCompass {

    private var listener: ((CompassReading) -> Unit)? = null

    private val handler = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didUpdateHeading: CLHeading) {
            val callback = listener ?: return

            // `trueHeading` mövqe xidməti işləmirsə mənfidir; `headingAccuracy` da mənfi olanda
            // etibarsızdır (Apple sənədi). Hər ikisi null-a çevrilir ki, ortaq qat ehtiyata keçsin.
            val trueHeading = didUpdateHeading.trueHeading.takeIf { it >= 0.0 }
            val accuracy = didUpdateHeading.headingAccuracy.takeIf { it >= 0.0 }

            val x = didUpdateHeading.x
            val y = didUpdateHeading.y
            val z = didUpdateHeading.z
            // CLHeading mikrotesla verir, model isə nanotesla ilə işləyir.
            val field = sqrt(x * x + y * y + z * z) * 1000.0

            callback(
                CompassReading(
                    magneticHeadingDeg = QiblaMath.normalizeDegrees(didUpdateHeading.magneticHeading),
                    trueHeadingDeg = trueHeading,
                    accuracyDeg = accuracy,
                    fieldStrengthNanoTesla = field.takeIf { it > 0.0 },
                    // iOS ayrıca kalibrasiya statusu vermir; dəqiqlik `accuracyDeg`-dədir.
                    calibration = CompassCalibration.UNKNOWN,
                ),
            )
        }

        /** Sistemin öz kalibrasiya ekranına icazə verir — istifadəçini özümüz öyrətməkdən yaxşıdır. */
        override fun locationManagerShouldDisplayHeadingCalibration(
            manager: CLLocationManager,
        ): Boolean = true
    }

    private val manager: CLLocationManager by lazy {
        CLLocationManager().apply { delegate = handler }
    }

    fun start(callback: (CompassReading) -> Unit) {
        listener = callback

        // Oriyentasiya xəbərdarlıqları açılmasa `UIDevice.orientation` həmişə "unknown" qalır və
        // landşaftda istiqamət 90° sürüşür.
        UIDevice.currentDevice.beginGeneratingDeviceOrientationNotifications()
        manager.headingOrientation = currentHeadingOrientation()
        manager.startUpdatingHeading()
    }

    fun stop() {
        manager.stopUpdatingHeading()
        UIDevice.currentDevice.endGeneratingDeviceOrientationNotifications()
        listener = null
    }

    /**
     * Cihazın fiziki oriyentasiyasını `CLDeviceOrientation`-a çevirir.
     *
     * Üzüstə/arxası üstə (`FaceUp`/`FaceDown`) və "unknown" hallarında portret saxlanılır: telefon
     * masada düz uzananda istiqamət onsuz da mənasızdır, oriyentasiyanı dəyişmək isə iynəni
     * səbəbsiz sıçradardı.
     */
    private fun currentHeadingOrientation(): CLDeviceOrientation =
        when (UIDevice.currentDevice.orientation) {
            UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown ->
                CLDeviceOrientationPortraitUpsideDown

            UIDeviceOrientation.UIDeviceOrientationLandscapeLeft ->
                CLDeviceOrientationLandscapeLeft

            UIDeviceOrientation.UIDeviceOrientationLandscapeRight ->
                CLDeviceOrientationLandscapeRight

            else -> CLDeviceOrientationPortrait
        }
}

/**
 * iOS sapmanı **ayrıca dəyər kimi vermir** — o, `CLHeading.trueHeading`-in içindədir.
 *
 * Ona görə burada null qayıdır və ortaq qat ya platformanın həqiqi şimalını, ya da
 * [GeomagneticModel]-i işlədir.
 */
actual fun platformDeclinationDeg(point: GeoPoint, atMillis: Long): Double? = null
