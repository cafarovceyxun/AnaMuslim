package com.cafarovceyxun.anamuslim.utils.qibla

import com.cafarovceyxun.anamuslim.utils.prayer.GeoPoint
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Qiblə həndəsəsi — **saf**, yalnız `kotlin.math`.
 *
 * ### Nə hesablanır
 * Qiblə, cari nöqtədən Kəbəyə gedən **böyük dairənin başlanğıc bucağıdır** (initial great-circle
 * bearing) — yəni kürə üzərində ən qısa yolun çıxış istiqaməti. Bu, İslam hüququnda üstünlük verilən
 * tərifdir və dünyadakı qiblə cədvəllərinin, mehrabların ölçüsüdür.
 *
 * ⚠️ **Dünya miqyaslı xəritədə düz xətt deyil.** Mercator-da düz görünən xətt loxodromdur:
 * Nyu-Yorkda böyük dairə ~58° (şimal-şərq), loxodrom isə ~96° (şərq) verir — 38 dərəcə fərq.
 * Tətbiqin xəritəsi bununla **qəsdən** yaşayır: pəncərə ~2 km-lik olduğu üçün və Mercator konformal
 * olduğu üçün (bucaqları lokal qoruyur) mərkəzdən çəkilən düz şüa həmin nöqtədə düzgün bucağı verir.
 * Gəzmə radiusu genişləndirilərsə bu artıq doğru olmayacaq — səbəb və nəticə
 * [com.cafarovceyxun.anamuslim.compose.components.qibla.WebMercator] KDoc-unda yazılıb.
 *
 * ### Vahidlər
 * Giriş dərəcə (`GeoPoint`), çıxış bucağı dərəcə (`0°..360°`, **həqiqi şimaldan** saat əqrəbi ilə),
 * məsafə metr.
 *
 * ℹ️ [GeoPoint.elevationMeters] **girmir**. Hündürlük böyük dairə bucağını dəyişmir; məsafəyə
 * təsiri isə 3000 km-lik yolda millimetr səviyyəsindədir.
 *
 * ### Maqnit şimalı ilə əlaqəsi
 * Buradakı bucaq **həqiqi (coğrafi) şimala** görədir. Telefonun maqnit sensoru isə maqnit şimalını
 * göstərir; ikisinin arasındakı fərq (deklinasiya) Bakıda ~+6°, Moskvada ~+11°, Nyu-Yorkda ~−13°-dir.
 * Düzəlişi [GeomagneticModel] və [CompassSource] qatı verir — **bu obyekt ona toxunmur**.
 * Xəritə rejimi isə düzəlişə ümumiyyətlə ehtiyac duymur, çünki xəritənin özü həqiqi şimala baxır.
 */
object QiblaMath {

    /**
     * Kəbə — Məscidül-Həram, Məkkə.
     *
     * Mənbə: OpenStreetMap-dakı `الكعبة` düyünü (`amenity=place_of_worship`).
     *
     * ⚠️ **Əvvəl burada 39.8251832 yazılmışdı və səhv idi.** Həmin cütlük internetdə çox yayılıb,
     * amma uzunluğu **104 m qərbə** sürüşdürür. Uzaqdan bunun heç bir əhəmiyyəti yoxdur (İstanbulda
     * bucağı 0.002° dəyişir), Məkkənin içində isə xəritədə nişan Kəbənin yanına yox, **Hərəmin
     * qərb tərəfinə** düşür — 2026-09-16-da istifadəçi bunu ekranda göstərdi və Nominatim sorğusu
     * ilə təsdiqləndi. Yəni bu sabitin dəqiqliyi yalnız Kəbəyə yaxın istifadəçilər üçün görünür.
     */
    val KAABA = GeoPoint(latitude = 21.4225079, longitude = 39.8261890)

    private const val DEG = 180.0 / PI
    private const val RAD = PI / 180.0

    /**
     * IUGG orta Yer radiusu (metr).
     *
     * Kürə yaxınlaşmasıdır: WGS84 ellipsoidi ilə fərq məsafədə ~0.3%-ə qədər ola bilər (3000 km-də
     * ~10 km). Bu rəqəm istifadəçiyə **məlumat etiketi** kimi göstərilir («Kəbəyə 2314 km»), heç bir
     * hesablamaya girmir, ona görə ellipsoid (Vincenty) həlli əlavə edilmir.
     */
    private const val EARTH_RADIUS_METERS = 6_371_008.8

    /**
     * [from] nöqtəsindən Kəbəyə böyük dairə üzrə **başlanğıc bucaq**, həqiqi şimaldan saat əqrəbi
     * ilə, `0.0..360.0`.
     *
     * ⚠️ İki kənar halda nəticə mənasızdır və 0.0 qaytarılır: nöqtə **Kəbənin özüdürsə** və nöqtə
     * Kəbənin **antipodudursa** (istiqamət təyin olunmur — bütün istiqamətlər eyni uzunluqdadır).
     * Çağıran tərəf [distanceToKaabaMeters] ilə bunu ayırd etməlidir; UI «Kəbədəsiniz» yazır.
     */
    fun bearingToKaaba(from: GeoPoint): Double {
        val lat1 = from.latitude * RAD
        val lat2 = KAABA.latitude * RAD
        val deltaLng = (KAABA.longitude - from.longitude) * RAD

        val y = sin(deltaLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLng)

        return normalizeDegrees(atan2(y, x) * DEG)
    }

    /** [from] nöqtəsindən Kəbəyə böyük dairə məsafəsi, metr. */
    fun distanceToKaabaMeters(from: GeoPoint): Double = distanceMeters(from, KAABA)

    /** İki nöqtə arasındakı böyük dairə məsafəsi, metr. Haversine. */
    fun distanceMeters(from: GeoPoint, to: GeoPoint): Double {
        val lat1 = from.latitude * RAD
        val lat2 = to.latitude * RAD
        val deltaLat = lat2 - lat1
        val deltaLng = (to.longitude - from.longitude) * RAD

        val sinHalfLat = sin(deltaLat / 2.0)
        val sinHalfLng = sin(deltaLng / 2.0)
        val a = sinHalfLat * sinHalfLat + cos(lat1) * cos(lat2) * sinHalfLng * sinHalfLng

        // `coerceAtMost(1.0)`: yuvarlaqlaşdırma `a`-nı 1-dən bir qədər böyük edə bilər (antipodda),
        // o zaman `sqrt` düz gəlsə də `asin` NaN verir.
        return 2.0 * EARTH_RADIUS_METERS * asin(sqrt(a).coerceAtMost(1.0))
    }

    /** Bucağı `0.0..360.0` aralığına gətirir. Mənfi və 360-dan böyük dəyərləri qəbul edir. */
    fun normalizeDegrees(degrees: Double): Double {
        val wrapped = degrees % 360.0

        return if (wrapped < 0.0) wrapped + 360.0 else wrapped
    }

    /**
     * [from] bucağından [to] bucağına **ən qısa** fərq, `−180.0..180.0`.
     *
     * Müsbət = saat əqrəbi ilə (sağa) dönmək lazımdır. Kompas iynəsi ilə qiblə arasındakı fərqi
     * bu verir: 350°-dən 10°-ə fərq **+20°**-dir, −340° yox.
     */
    fun signedDeltaDegrees(from: Double, to: Double): Double {
        val delta = normalizeDegrees(to - from)

        return if (delta > 180.0) delta - 360.0 else delta
    }
}
