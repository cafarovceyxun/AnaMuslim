package com.cafarovceyxun.anamuslim.utils.qibla

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Surface
import com.cafarovceyxun.anamuslim.utils.AndroidPlatformContext
import com.cafarovceyxun.anamuslim.utils.prayer.GeoPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

private fun sensorManager(): SensorManager? =
    AndroidPlatformContext.context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

actual fun isCompassAvailable(): Boolean =
    sensorManager()?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null

/**
 * `TYPE_ROTATION_VECTOR` axını.
 *
 * ⚠️ **Xam `MAGNETIC_FIELD + ACCELEROMETER` cütü işlədilmir.** Rotation vector akselerometr,
 * maqnitometr və giroskopu birləşdirir; xam cüt isə əl titrəyişində iynəni oynadır və hər addımda
 * sıçrayır. Fərq cihazda gözlə görünür.
 *
 * ⚠️ **`remapCoordinateSystem` şərtdir.** Sensor çərçivəsi cihazın **təbii** oriyentasiyasındadır;
 * ekran döndürüləndə remap edilməsə azimut 90° sürüşür. Kompilyator da, testlər də bunu tutmur —
 * yalnız telefonu yan çevirəndə görünür.
 */
actual fun compassReadings(): Flow<CompassReading> = callbackFlow {
    val manager = sensorManager()
    val rotationSensor = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    if (manager == null || rotationSensor == null) {
        close()
        return@callbackFlow
    }

    val magnetometer = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    val rotationMatrix = FloatArray(9)
    val remapped = FloatArray(9)
    val orientation = FloatArray(3)

    var fieldStrength: Double? = null
    var calibration = CompassCalibration.UNKNOWN

    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor?.type) {
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    // Sensor mikrotesla verir, model isə nanotesla ilə işləyir.
                    fieldStrength = sqrt((x * x + y * y + z * z).toDouble()) * 1000.0
                }

                Sensor.TYPE_ROTATION_VECTOR -> {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                    val (axisX, axisY) = displayAxes()
                    SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remapped)
                    SensorManager.getOrientation(remapped, orientation)

                    // Rotation vector-un beşinci dəyəri istiqamət xətasının radianla təxminidir.
                    // Bütün cihazlarda mövcud deyil (API 18-dən bəri "optional"), ona görə yoxlanılır.
                    val accuracy = event.values
                        .takeIf { it.size >= 5 }
                        ?.get(4)
                        ?.takeIf { it >= 0f }
                        ?.let { Math.toDegrees(it.toDouble()) }

                    trySend(
                        CompassReading(
                            magneticHeadingDeg = QiblaMath.normalizeDegrees(
                                Math.toDegrees(orientation[0].toDouble()),
                            ),
                            // Android həqiqi şimal vermir — düzəliş `platformDeclinationDeg`-dədir.
                            trueHeadingDeg = null,
                            accuracyDeg = accuracy,
                            fieldStrengthNanoTesla = fieldStrength,
                            calibration = calibration,
                        ),
                    )
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            if (sensor?.type != Sensor.TYPE_ROTATION_VECTOR &&
                sensor?.type != Sensor.TYPE_MAGNETIC_FIELD
            ) {
                return
            }

            calibration = when (accuracy) {
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM,
                -> CompassCalibration.OK

                SensorManager.SENSOR_STATUS_ACCURACY_LOW -> CompassCalibration.LOW
                SensorManager.SENSOR_STATUS_UNRELIABLE -> CompassCalibration.UNRELIABLE
                else -> CompassCalibration.UNKNOWN
            }
        }
    }

    manager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
    magnetometer?.let { manager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }

    // Qeydiyyatı bağlamamaq sensoru açıq saxlayır və batareyanı yeyir.
    awaitClose { manager.unregisterListener(listener) }
}

/**
 * Ekranın döndürülməsinə uyğun remap oxları.
 *
 * `Surface.ROTATION_*` cihazın təbii oriyentasiyasından fərqi göstərir, ona görə planşetdə və
 * telefonda eyni sabit müxtəlif fiziki vəziyyət deməkdir — burada məhz fərq işlədilir, mütləq
 * oriyentasiya yox.
 */
private fun displayAxes(): Pair<Int, Int> {
    // ⚠️ Burada `context.display` (və ya `WindowManager.defaultDisplay`) işlətmək OLMAZ:
    // `AndroidPlatformContext.context` **Application** kontekstidir, ona bağlı ekran yoxdur və
    // API 30+ orada `UnsupportedOperationException` atır. Nəticə ağırdır — istisna sensor
    // callback-ində, yəni main looper-də baş verir, ona görə tətbiq kompas rejimi açılan kimi
    // **çökür**. Nə kompilyator, nə testlər, nə də `/verify` bunu göstərir; 2026-09-16-da
    // yalnız cihazda tutuldu.
    //
    // `DisplayManager` vizual kontekst tələb etmir və defolt ekranı birbaşa verir.
    val display = (AndroidPlatformContext.context.getSystemService(Context.DISPLAY_SERVICE)
        as? DisplayManager)?.getDisplay(Display.DEFAULT_DISPLAY)

    val rotation = display?.rotation ?: Surface.ROTATION_0

    return when (rotation) {
        Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
        Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
        Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
        else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
    }
}

/**
 * `android.hardware.GeomagneticField` — Android-in öz WMM tətbiqi.
 *
 * ⚠️ Cihazın sistem versiyası ilə gəlir, yəni köhnə telefonda model illərlə köhnə ola bilər. Buna
 * baxmayaraq istifadəçinin seçimi «platforma əsas»dır; sürüşmə beş ildə ~0.2° səviyyəsindədir və
 * sensorun öz xətasından (±2–5°) kiçikdir.
 */
actual fun platformDeclinationDeg(point: GeoPoint, atMillis: Long): Double? = GeomagneticField(
    point.latitude.toFloat(),
    point.longitude.toFloat(),
    point.elevationMeters.toFloat(),
    atMillis,
).declination.toDouble()
