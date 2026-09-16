package com.cafarovceyxun.anamuslim.utils.qibla

import com.cafarovceyxun.anamuslim.utils.prayer.GeoPoint
import kotlinx.coroutines.flow.Flow

/**
 * Maqnit sensorunun kalibrasiya vəziyyəti.
 *
 * Kalibrsiz sensor **səhv istiqaməti inamla** göstərir — nə çökmə, nə log. Ona görə vəziyyət
 * UI-yə çıxarılır və istifadəçidən səkkizvari hərəkət istənir.
 */
enum class CompassCalibration {
    OK,
    LOW,
    UNRELIABLE,

    /** Platforma hələ heç nə deməyib (ilk oxunuşlara qədər normaldır). */
    UNKNOWN,
}

/** Sensorun bir oxunuşu. Platformadan asılı olmayan forma. */
data class CompassReading(
    /** Maqnit şimalından, `0°..360°`. Həmişə mövcuddur. */
    val magneticHeadingDeg: Double,

    /**
     * Platformanın özünün hesabladığı **həqiqi** şimal, və ya alınmadıqda null.
     *
     * iOS `CLHeading.trueHeading`-i verir, amma **yalnız mövqe xidməti işləyəndə** — əks halda
     * dəyər `−1`-dir və burada null olur. Android isə heç vaxt vermir (orada düzəliş
     * [platformDeclinationDeg] ilə ayrıca gəlir).
     */
    val trueHeadingDeg: Double?,

    /** Platformanın xəta təxmini (± dərəcə), və ya yoxdursa null. */
    val accuracyDeg: Double?,

    /**
     * Ölçülən tam maqnit sahəsi, **nanotesla**.
     *
     * [GeomagneticModel.totalIntensityNanoTesla] ilə tutuşdurulur: ciddi fərq yaxınlıqda metal
     * olduğunu göstərir.
     */
    val fieldStrengthNanoTesla: Double?,

    val calibration: CompassCalibration,
)

/**
 * Cihazda istiqamət sensoru varmı.
 *
 * `false` olanda UI kompas rejimini **təklif etməməlidir** — xəritə rejimi sensorsuz da tam
 * işlədiyi üçün ekran sıradan çıxmır. (CLAUDE.md: basılan, amma heç nə etməyən düymə olmasın.)
 *
 * ⚠️ iOS **simulyatorunda həmişə `false`-dur** — simulyatorda maqnitometr yoxdur. Yəni kompası
 * yalnız real cihazda yoxlamaq olar; simulyator isə məhz «sensorsuz cihaz» yolunu sınamaq üçün
 * əlverişlidir.
 */
expect fun isCompassAvailable(): Boolean

/**
 * Sensor oxunuşlarının **soyuq** axını: toplanma başlayanda sensor qeydiyyata alınır, axın ləğv
 * olunanda çıxarılır.
 *
 * Qeydiyyatı bağlamamaq batareyanı yeyir, ona görə hər iki actual `awaitClose` ilə söndürür.
 */
expect fun compassReadings(): Flow<CompassReading>

/**
 * Platformanın öz maqnit sapması hesabı, və ya platformada belə bir şey yoxdursa **null**.
 *
 * Android `android.hardware.GeomagneticField`-i verir. iOS null qaytarır — orada düzəliş ayrıca
 * dəyər kimi yox, `CLHeading.trueHeading`-in içində gəlir.
 *
 * Null qayıdanda (və ya iOS-da `trueHeading` də alınmayanda) [GeomagneticModel] işə düşür.
 */
expect fun platformDeclinationDeg(point: GeoPoint, atMillis: Long): Double?
