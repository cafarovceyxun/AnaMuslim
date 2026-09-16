package com.cafarovceyxun.anamuslim.utils.qibla

import com.cafarovceyxun.anamuslim.utils.prayer.GeoPoint
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Yer maqnit sahəsi — **Dünya Maqnit Modeli (WMM2025)**, saf Kotlin, yalnız `kotlin.math`.
 *
 * ### Nə üçün lazımdır
 * Telefonun sensoru **maqnit** şimalını göstərir, qiblə isə **həqiqi** şimala görə hesablanır
 * ([QiblaMath]). İkisinin arasındakı fərq — deklinasiya — yerə görə dəyişir: Bakıda ~+6°,
 * Moskvada ~+11°, Nyu-Yorkda ~−13°. Düzəliş edilməsə kompas gözlə görünəcək qədər yanılır.
 *
 * ### Mənbə və lisenziya
 * Koefisiyentlər NOAA NCEI + BGS-in rəsmi `WMM2025.COF` faylındandır (**ictimai mal**, GPLv3 ilə
 * uyğun). Cədvəl **əl ilə köçürülməyib** — fayldan generasiya olunub və NOAA-nın öz nüsxəsi ilə
 * bit-bit eyniliyi yoxlanılıb. Alqoritm NOAA-nın `GeomagnetismLibrary` axınını izləyir: Gauss
 * normallaşdırılmış Legendre funksiyaları → Schmidt kvazi-normallaşdırma → sferik toplama →
 * geosentrikdən geodezikə çevrilmə.
 *
 * ### ⚠️ Modelin son istifadə tarixi var
 * WMM beş ildən bir yenilənir; bu nüsxə **2025.0 – 2030.0** aralığı üçündür. Tarix keçəndən sonra
 * hesablama dayanmır, amma sapma sürüşməyə başlayır — [isExpired] bunu bildirir, UI isə istifadəçiyə
 * kompasın dəqiqliyinin azaldığını deyir. Yeniləmək üçün yeni `WMM.COF`-u götürüb bu faylı yenidən
 * generasiya etmək kifayətdir.
 *
 * ### Vahidlər
 * Bucaqlar dərəcə, sahə **nanotesla (nT)**, hündürlük **kilometr** (WGS84 ellipsoidindən).
 */
object GeomagneticModel {

    /** Modelin epoxası — koefisiyentlərin verildiyi an. */
    const val EPOCH_YEAR = 2025.0

    /** Modelin etibarlılıq həddi. Bundan sonra nəticə sürüşməyə başlayır. */
    const val EXPIRY_YEAR = 2030.0

    private const val N_MAX = 12
    private const val ARRAY_SIZE = (N_MAX + 1) * (N_MAX + 2) / 2

    private const val RAD = PI / 180.0
    private const val DEG = 180.0 / PI

    /** WGS84 böyük yarımoxu, km. */
    private const val WGS84_A_KM = 6378.137

    /** WGS84 birinci ekssentrisitetin kvadratı, `f(2−f)`. */
    private const val WGS84_E2 = 0.0066943799901413165

    /** Geomaqnit istinad radiusu, km — WMM-in tərifində sabitdir (Yer radiusu ilə eyni deyil). */
    private const val GEOMAGNETIC_A_KM = 6371.2

    /**
     * Enliyin sıxıldığı hədd.
     *
     * Şərq komponenti `By` qütbdə `1/cos(enlik)`-ə bölünür, yəni riyazi olaraq təyin olunmur. NOAA
     * kitabxanası bunun üçün ayrıca qütb düsturu saxlayır; burada isə enlik sadəcə 89.99°-yə sıxılır.
     * Fərq modelin öz xətasından (~0.2°) qat-qat kiçikdir və tətbiqin heç bir istifadəçisi dəqiq
     * qütbdə namaz qılmır — ona görə ikinci düstur saxlamağa dəyməz.
     */
    private const val POLE_LIMIT_DEG = 89.99

    /** Bir Qriqorian ilinin orta uzunluğu, millisaniyə. */
    private const val MILLIS_PER_YEAR = 365.2425 * 24.0 * 60.0 * 60.0 * 1000.0

    /**
     * Maqnit sahəsinin bir nöqtədəki tam təsviri.
     *
     * [northNanoTesla]/[eastNanoTesla]/[downNanoTesla] geodezik çərçivədədir (X şimala, Y şərqə,
     * Z aşağı).
     */
    data class MagneticField(
        /** Deklinasiya: həqiqi şimaldan maqnit şimalına bucaq, **şərqə müsbət**. */
        val declinationDeg: Double,
        /** İnklinasiya (meyl): sahənin üfüqdən aşağı bucağı. */
        val inclinationDeg: Double,
        /** Üfüqi komponentin gücü. */
        val horizontalNanoTesla: Double,
        /** Tam sahə gücü — müdaxilə aşkarlanmasının istinadı. */
        val totalNanoTesla: Double,
        val northNanoTesla: Double,
        val eastNanoTesla: Double,
        val downNanoTesla: Double,
    )

    /** Unix epoxa millisaniyəsini onluq ilə çevirir. */
    fun decimalYear(atMillis: Long): Double = 1970.0 + atMillis / MILLIS_PER_YEAR

    /**
     * Model bu an üçün köhnəlibmi.
     *
     * ℹ️ [decimalYear] təqvim əvəzinə orta il uzunluğundan istifadə edir — 55 ildə fərq bir gündən
     * azdır, sekulyar dəyişmə isə ildə ~0.1° olduğu üçün bunun nəticəyə təsiri 0.001°-dən kiçikdir.
     */
    fun isExpired(atMillis: Long): Boolean = decimalYear(atMillis) > EXPIRY_YEAR

    /**
     * [point] üçün deklinasiya (şərqə müsbət), dərəcə.
     *
     * Həqiqi şimal = maqnit şimalı **+** bu dəyər.
     */
    fun declinationDeg(point: GeoPoint, atMillis: Long): Double = fieldAt(
        latitudeDeg = point.latitude,
        longitudeDeg = point.longitude,
        altitudeKm = point.elevationMeters / 1000.0,
        decimalYear = decimalYear(atMillis),
    ).declinationDeg

    /**
     * [point] üçün **gözlənilən** tam sahə gücü, nT.
     *
     * Kompasın ölçdüyü |B| bundan ciddi fərqlənirsə yaxınlıqda maqnit müdaxiləsi var (metal masa,
     * dinamik, avtomobil, maqnitli telefon qabı) və istiqamətə etibar etmək olmaz. Platformaların
     * heç biri bu rəqəmi vermir — müdaxilə xəbərdarlığının mümkün olmasının səbəbi budur.
     */
    fun totalIntensityNanoTesla(point: GeoPoint, atMillis: Long): Double = fieldAt(
        latitudeDeg = point.latitude,
        longitudeDeg = point.longitude,
        altitudeKm = point.elevationMeters / 1000.0,
        decimalYear = decimalYear(atMillis),
    ).totalNanoTesla

    /**
     * Sahənin tam hesabı.
     *
     * Açıqdır ki, NOAA-nın rəsmi test cədvəli birbaşa bu funksiyaya verilə bilsin (orada hündürlük
     * və onluq il sərbəst seçilir).
     */
    fun fieldAt(
        latitudeDeg: Double,
        longitudeDeg: Double,
        altitudeKm: Double,
        decimalYear: Double,
    ): MagneticField {
        val latitude = latitudeDeg.coerceIn(-POLE_LIMIT_DEG, POLE_LIMIT_DEG)
        val yearsFromEpoch = decimalYear - EPOCH_YEAR

        // --- Geodezik → geosentrik sferik ---
        val latRad = latitude * RAD
        val sinLat = sin(latRad)
        val cosLat = cos(latRad)
        val curvature = WGS84_A_KM / sqrt(1.0 - WGS84_E2 * sinLat * sinLat)
        val pAxis = (curvature + altitudeKm) * cosLat
        val zAxis = (curvature * (1.0 - WGS84_E2) + altitudeKm) * sinLat
        val radius = sqrt(pAxis * pAxis + zAxis * zAxis)
        val sinGeocentric = zAxis / radius
        val geocentricLatRad = asin(sinGeocentric)

        // --- Schmidt kvazi-normallaşdırılmış Legendre funksiyaları ---
        val legendre = DoubleArray(ARRAY_SIZE)
        val legendreDerivative = DoubleArray(ARRAY_SIZE)
        computeLegendre(sinGeocentric, legendre, legendreDerivative)

        // --- m·λ-nın sinus/kosinusları, rekursiya ilə ---
        val lonRad = longitudeDeg * RAD
        val cosMLambda = DoubleArray(N_MAX + 1)
        val sinMLambda = DoubleArray(N_MAX + 1)
        cosMLambda[0] = 1.0
        sinMLambda[0] = 0.0
        cosMLambda[1] = cos(lonRad)
        sinMLambda[1] = sin(lonRad)
        for (m in 2..N_MAX) {
            cosMLambda[m] = cosMLambda[m - 1] * cosMLambda[1] - sinMLambda[m - 1] * sinMLambda[1]
            sinMLambda[m] = cosMLambda[m - 1] * sinMLambda[1] + sinMLambda[m - 1] * cosMLambda[1]
        }

        // --- Sferik toplama ---
        var bx = 0.0
        var by = 0.0
        var bz = 0.0
        val radiusRatio = GEOMAGNETIC_A_KM / radius
        var relativeRadiusPower = radiusRatio * radiusRatio

        for (n in 1..N_MAX) {
            relativeRadiusPower *= radiusRatio // (a/r)^(n+2)

            for (m in 0..n) {
                val i = n * (n + 1) / 2 + m
                val gnm = G[i] + yearsFromEpoch * G_DOT[i]
                val hnm = H[i] + yearsFromEpoch * H_DOT[i]

                val cosPart = gnm * cosMLambda[m] + hnm * sinMLambda[m]
                val sinPart = gnm * sinMLambda[m] - hnm * cosMLambda[m]

                bz -= relativeRadiusPower * cosPart * (n + 1) * legendre[i]
                by += relativeRadiusPower * sinPart * m * legendre[i]
                bx -= relativeRadiusPower * cosPart * legendreDerivative[i]
            }
        }

        by /= cos(geocentricLatRad)

        // --- Geosentrik → geodezik çevrilmə ---
        val psi = geocentricLatRad - latRad
        val north = bx * cos(psi) - bz * sin(psi)
        val down = bx * sin(psi) + bz * cos(psi)
        val east = by

        val horizontal = sqrt(north * north + east * east)

        return MagneticField(
            declinationDeg = atan2(east, north) * DEG,
            inclinationDeg = atan2(down, horizontal) * DEG,
            horizontalNanoTesla = horizontal,
            totalNanoTesla = sqrt(horizontal * horizontal + down * down),
            northNanoTesla = north,
            eastNanoTesla = east,
            downNanoTesla = down,
        )
    }

    /**
     * Gauss normallaşdırılmış assosiativ Legendre funksiyalarını qurub Schmidt kvazi-normallaşdırmaya
     * çevirir.
     *
     * [x] geosentrik enliyin sinusudur. [values] `P(n,m)`, [derivatives] isə **enliyə görə**
     * törəmədir — NOAA kodu ko-enliyə görə hesablayıb sonda işarəni dəyişir, burada da eyni.
     *
     * İndeksləmə: `i = n(n+1)/2 + m` (üçbucaq düzülüş).
     */
    private fun computeLegendre(x: Double, values: DoubleArray, derivatives: DoubleArray) {
        values[0] = 1.0
        derivatives[0] = 0.0

        val z = sqrt((1.0 - x) * (1.0 + x)) // cos(geosentrik enlik)

        for (n in 1..N_MAX) {
            for (m in 0..n) {
                val i = n * (n + 1) / 2 + m

                when {
                    n == m -> {
                        val prev = (n - 1) * n / 2 + m - 1
                        values[i] = z * values[prev]
                        derivatives[i] = z * derivatives[prev] + x * values[prev]
                    }

                    n == 1 && m == 0 -> {
                        val prev = (n - 1) * n / 2 + m
                        values[i] = x * values[prev]
                        derivatives[i] = x * derivatives[prev] - z * values[prev]
                    }

                    else -> {
                        val prev1 = (n - 2) * (n - 1) / 2 + m
                        val prev2 = (n - 1) * n / 2 + m

                        if (m > n - 2) {
                            values[i] = x * values[prev2]
                            derivatives[i] = x * derivatives[prev2] - z * values[prev2]
                        } else {
                            val k = ((n - 1) * (n - 1) - m * m).toDouble() /
                                ((2 * n - 1) * (2 * n - 3)).toDouble()
                            values[i] = x * values[prev2] - k * values[prev1]
                            derivatives[i] = x * derivatives[prev2] - z * values[prev2] -
                                k * derivatives[prev1]
                        }
                    }
                }
            }
        }

        // Gauss → Schmidt kvazi-normallaşdırma
        val norm = DoubleArray(ARRAY_SIZE)
        norm[0] = 1.0

        for (n in 1..N_MAX) {
            val diagonal = n * (n + 1) / 2
            norm[diagonal] = norm[(n - 1) * n / 2] * (2 * n - 1).toDouble() / n.toDouble()

            for (m in 1..n) {
                val i = n * (n + 1) / 2 + m
                val prev = n * (n + 1) / 2 + m - 1
                val doubling = if (m == 1) 2 else 1
                norm[i] = norm[prev] *
                    sqrt(((n - m + 1) * doubling).toDouble() / (n + m).toDouble())
            }
        }

        for (n in 1..N_MAX) {
            for (m in 0..n) {
                val i = n * (n + 1) / 2 + m
                values[i] *= norm[i]
                // İşarə dəyişir: törəmə ko-enliyə görə yox, enliyə görə lazımdır.
                derivatives[i] *= -norm[i]
            }
        }
    }

    // --- WMM2025 koefisiyentləri (NOAA NCEI/BGS, ictimai mal; fayldan generasiya olunub) ---
    // Sıra: i = n(n+1)/2 + m. G/H nanotesla, G_DOT/H_DOT nanotesla/il.

private val G = doubleArrayOf(
    0.0, -29351.8, -1410.8, -2556.6, 2951.1, 1649.3,
    1361.0, -2404.1, 1243.8, 453.6, 895.0, 799.5,
    55.7, -281.1, 12.1, -233.2, 368.9, 187.2,
    -138.7, -142.0, 20.9, 64.4, 63.8, 76.9,
    -115.7, -40.9, 14.9, -60.7, 79.5, -77.0,
    -8.8, 59.3, 15.8, 2.5, -11.1, 14.2,
    23.2, 10.8, -17.5, 2.0, -21.7, 16.9,
    15.0, -16.8, 0.9, 4.6, 7.8, 3.0,
    -0.2, -2.5, -13.1, 2.4, 8.6, -8.7,
    -12.9, -1.3, -6.4, 0.2, 2.0, -1.0,
    -0.6, -0.9, 1.5, 0.9, -2.7, -3.9,
    2.9, -1.5, -2.5, 2.4, -0.6, -0.1,
    -0.6, -0.1, 1.1, -1.0, -0.2, 2.6,
    -2.0, -0.2, 0.3, 1.2, -1.3, 0.6,
    0.6, 0.5, -0.1, -0.4, -0.2, -1.3,
    -0.7,
)

private val H = doubleArrayOf(
    0.0, 0.0, 4545.4, 0.0, -3133.6, -815.1,
    0.0, -56.6, 237.5, -549.5, 0.0, 278.6,
    -133.9, 212.0, -375.6, 0.0, 45.4, 220.2,
    -122.9, 43.0, 106.1, 0.0, -18.4, 16.8,
    48.8, -59.8, 10.9, 72.7, 0.0, -48.9,
    -14.4, -1.0, 23.4, -7.4, -25.1, -2.3,
    0.0, 7.1, -12.6, 11.4, -9.7, 12.7,
    0.7, -5.2, 3.9, 0.0, -24.8, 12.2,
    8.3, -3.3, -5.2, 7.2, -0.6, 0.8,
    10.0, 0.0, 3.3, 0.0, 2.4, 5.3,
    -9.1, 0.4, -4.2, -3.8, 0.9, -9.1,
    0.0, 0.0, 2.9, -0.6, 0.2, 0.5,
    -0.3, -1.2, -1.7, -2.9, -1.8, -2.3,
    0.0, -1.3, 0.7, 1.0, -1.4, -0.0,
    0.6, -0.1, 0.8, 0.1, -1.0, 0.1,
    0.2,
)

private val G_DOT = doubleArrayOf(
    0.0, 12.0, 9.7, -11.6, -5.2, -8.0,
    -1.3, -4.2, 0.4, -15.6, -1.6, -2.4,
    -6.0, 5.6, -7.0, 0.6, 1.4, 0.0,
    0.6, 2.2, 0.9, -0.2, -0.4, 0.9,
    1.2, -0.9, 0.3, 0.9, -0.0, -0.1,
    -0.1, 0.5, -0.1, -0.8, -0.8, 0.8,
    -0.1, 0.2, 0.0, 0.5, -0.1, 0.3,
    0.2, -0.0, 0.2, -0.0, -0.1, 0.1,
    0.3, -0.3, 0.0, 0.3, -0.1, 0.1,
    -0.1, 0.1, 0.0, 0.1, 0.1, -0.0,
    -0.3, 0.0, -0.1, -0.1, -0.0, -0.0,
    0.0, -0.0, 0.0, 0.0, 0.0, -0.1,
    0.0, -0.0, -0.1, -0.1, -0.1, -0.1,
    0.0, 0.0, -0.0, -0.0, -0.0, -0.0,
    0.1, -0.0, 0.0, 0.0, -0.1, -0.0,
    -0.1,
)

private val H_DOT = doubleArrayOf(
    0.0, 0.0, -21.5, 0.0, -27.7, -12.1,
    0.0, 4.0, -0.3, -4.1, 0.0, -1.1,
    4.1, 1.6, -4.4, 0.0, -0.5, 2.2,
    0.4, 1.7, 1.9, 0.0, 0.3, -1.6,
    -0.4, 0.9, 0.7, 0.9, 0.0, 0.6,
    0.5, -0.8, 0.0, -1.0, 0.6, -0.2,
    0.0, -0.2, 0.5, -0.4, 0.4, -0.5,
    -0.6, 0.3, 0.2, 0.0, -0.3, 0.3,
    -0.3, 0.3, 0.2, -0.1, -0.2, 0.4,
    0.1, 0.0, 0.0, -0.0, -0.2, 0.1,
    -0.1, 0.1, 0.0, -0.1, 0.2, -0.0,
    0.0, -0.0, 0.1, -0.0, 0.1, -0.0,
    -0.0, 0.1, -0.0, 0.0, 0.0, 0.0,
    0.0, -0.0, 0.0, -0.1, 0.1, -0.0,
    -0.0, -0.0, 0.0, -0.0, -0.0, 0.0,
    -0.1,
)
}
