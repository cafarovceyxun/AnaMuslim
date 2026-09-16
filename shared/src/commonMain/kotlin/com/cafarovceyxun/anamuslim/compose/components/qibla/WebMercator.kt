package com.cafarovceyxun.anamuslim.compose.components.qibla

import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

/**
 * Web Mercator (EPSG:3857) proyeksiyası — **saf**, yalnız `kotlin.math`.
 *
 * Bütün slippy-map tayl xidmətlərinin (OSM, peyk qatları) işlətdiyi proyeksiya budur: dünya
 * `zoom` səviyyəsində `256 · 2^zoom` piksellik kvadrata oturur, sol yuxarı künc (−180°, +85.05°).
 *
 * ### Niyə [zoom] `Double`-dır
 * Pinch jesti kəsilməz miqyas verir. Tayl **indeksi** üçün tam ədəd lazımdır (`floor(zoom)`), amma
 * çəkiliş kəsilməz zoom-da aparılır — yoxsa iki barmaq arasında xəritə pillə-pillə sıçrayır.
 *
 * ### ⚠️ Konformallıq — qiblə xəttinin düz çəkilməsinin əsası
 * Mercator **konformaldır**: bucaqları *lokal olaraq* qoruyur. Ona görə istifadəçinin nöqtəsindən
 * ekranda düz çəkilən şüa həmin nöqtədə **həqiqi bucağı düzgün göstərir** — qiblə xətti üçün
 * böyük dairəni nöqtə-nöqtə hesablamağa ehtiyac yoxdur.
 *
 * Bu, yalnız xəritə **dar** olduğu üçün doğrudur (tətbiqdə ~2 km radius): düz ekran xətti əslində
 * sabit bucaqlı loxodromdur və böyük dairədən uzaqlaşdıqca ayrılır. Nyu-Yorkda bütöv dünya
 * miqyasında fərq ~38 dərəcəyə çatır. **Gəzmə radiusu genişləndirilərsə bu fayldakı düz şüa səhv
 * olur** və xətt böyük dairə üzrə interpolyasiya edilməlidir — nə kompilyator, nə test bunu tutar,
 * çünki kod işləməyə davam edir, sadəcə xətt yanlış yerə baxır.
 */
object WebMercator {

    /** Bir taylın kənarı, piksel. Bütün ümumi tayl xidmətlərində 256-dır. */
    const val TILE_SIZE = 256

    /**
     * Proyeksiyanın enlik həddi (dərəcə).
     *
     * Bu enlikdə dünya kvadrat olur; qütblər Mercator-da sonsuzluğa gedir, ona görə xəritə orada
     * kəsilir. Enlik bu həddi keçirsə [latToWorldY] sonsuz qiymət verər — giriş həmişə
     * [clampLatitude]-dan keçirilməlidir.
     */
    const val MAX_LATITUDE = 85.05112878

    /** WGS84 böyük yarımoxu (metr) — Web Mercator miqyasının təyin olunduğu radius. */
    private const val EARTH_RADIUS_METERS = 6_378_137.0

    private const val RAD = PI / 180.0
    private const val DEG = 180.0 / PI

    /** Dünyanın həmin zoom-dakı eni/hündürlüyü, piksel. */
    fun worldSizePixels(zoom: Double): Double = TILE_SIZE * 2.0.pow(zoom)

    /** Enliyi proyeksiyanın etibarlı aralığına sıxır. */
    fun clampLatitude(latitude: Double): Double = latitude.coerceIn(-MAX_LATITUDE, MAX_LATITUDE)

    /** Uzunluğu `−180..180` aralığına bükür. */
    fun wrapLongitude(longitude: Double): Double {
        val wrapped = (longitude + 180.0) % 360.0

        return (if (wrapped < 0.0) wrapped + 360.0 else wrapped) - 180.0
    }

    fun lonToWorldX(longitude: Double, zoom: Double): Double =
        (wrapLongitude(longitude) + 180.0) / 360.0 * worldSizePixels(zoom)

    fun latToWorldY(latitude: Double, zoom: Double): Double {
        // ln(tan φ + sec φ) ilə eyni şeydir, amma `asinh` qütbə yaxın daha sabitdir.
        //
        // ⚠️ Nəticə ayrıca `±PI`-yə sıxılır. [MAX_LATITUDE] dəqiq həddin (`atan(sinh π)`) onluq
        // yuvarlaqlaşdırmasıdır və ondan bir qədər **böyükdür**, ona görə enlik sıxıldıqdan sonra
        // da `projected` π-ni bir neçə ULP keçir və Y dünya kvadratından kənara düşür. Oradan
        // hesablanan tayl indeksi `2^zoom` sərhədini aşır — yəni qütbə yaxın xəritə mövcud olmayan
        // tayl istəyir və boş qalır. Nə kompilyator, nə də adi gəzinti bunu göstərir.
        val projected = asinh(tan(clampLatitude(latitude) * RAD)).coerceIn(-PI, PI)

        return (1.0 - projected / PI) / 2.0 * worldSizePixels(zoom)
    }

    fun worldXToLon(x: Double, zoom: Double): Double =
        x / worldSizePixels(zoom) * 360.0 - 180.0

    fun worldYToLat(y: Double, zoom: Double): Double {
        val projected = PI * (1.0 - 2.0 * y / worldSizePixels(zoom))

        return atan(sinh(projected)) * DEG
    }

    /**
     * Həmin enlikdə bir pikselin neçə metr olduğu.
     *
     * Miqyas zolağı və gəzmə radiusunu piksələ çevirmək üçün. Mercator-da miqyas enlikdən asılıdır —
     * ona görə `cos(enlik)` vurulur; bunu unutmaq yüksək enlikdə radiusu bir neçə dəfə səhv edir.
     */
    fun metersPerPixel(latitude: Double, zoom: Double): Double =
        2.0 * PI * EARTH_RADIUS_METERS * cos(clampLatitude(latitude) * RAD) / worldSizePixels(zoom)
}
