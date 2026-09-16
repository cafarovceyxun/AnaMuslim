package com.cafarovceyxun.anamuslim.utils.prayer.location

import com.cafarovceyxun.anamuslim.utils.prayer.GeoPoint
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLPlacemark
import platform.CoreLocation.CLLocation
import platform.Foundation.NSLocale
import com.cafarovceyxun.anamuslim.compose.utils.appLocale
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.NSError
import platform.darwin.NSObject

/** Bir dəfəlik siqnal gözləmə həddi — Android tərəflə eyni. */
private const val FIX_TIMEOUT_MILLIS = 15_000L

/**
 * ℹ️ [maxCacheAgeMillis] iOS-da praktiki olaraq işə düşmür: `requestLocation()` Android-in
 * `getLastKnownLocation()`-u kimi köhnə keş vermir, təzə siqnal istəyir. Parametr yenə də hörmət
 * olunur ki, seam-in müqaviləsi hər iki platformada eyni mənanı daşısın.
 */
actual suspend fun currentDeviceLocation(maxCacheAgeMillis: Long): GeoPoint? =
    withContext(Dispatchers.Main) {
        if (!IosLocationAuthorization.isGranted()) return@withContext null

        IosLocationAuthorization.maxAgeMillis = maxCacheAgeMillis

        withTimeoutOrNull(FIX_TIMEOUT_MILLIS) { IosLocationAuthorization.requestSingleFix() }
    }

/**
 * Tək `CLLocationManager` və onun delegate-i.
 *
 * ⚠️ Menecer **fayl səviyyəsində** saxlanılır: `CLLocationManager` delegate-inə zəif (weak) istinad
 * saxlayır, ona görə lokal dəyişəndə yaradılan menecer sorğu cavab verməmiş toplanır və
 * `requestLocation()` **səssizcə heç nə etmir** — nə xəta, nə log.
 *
 * İcazə statusu da buradan oxunur ki, `LocationPermission.ios.kt` ilə eyni mənbədən gəlsin.
 */
@OptIn(ExperimentalForeignApi::class)
internal object IosLocationAuthorization {

    private val manager: CLLocationManager by lazy {
        CLLocationManager().apply {
            // Qiblə Kəbəyə yaxın yerlərdə kilometr dəqiqliyi ilə işləmir (bax
            // LocationPermission.kt). Namaz vaxtları üçün artıq dəqiqlik zərərsizdir.
            desiredAccuracy = kCLLocationAccuracyBest
            delegate = handler
        }
    }

    private var pending: CompletableDeferred<GeoPoint?>? = null

    /** Cari sorğunun qəbul etdiyi ən böyük mövqe yaşı; `0` = yalnız təzə siqnal. */
    internal var maxAgeMillis: Long = Long.MAX_VALUE
    private var authorizationListener: ((CLAuthorizationStatus) -> Unit)? = null

    private val handler = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            val location = didUpdateLocations.filterIsInstance<CLLocation>().lastOrNull()
            // `timeIntervalSince1970` sadə `Double` xassəsidir — `timeIntervalSinceDate`/
            // `timeIntervalSinceNow` bu bağlamada həll olunmur.
            val nowSeconds = NSDate().timeIntervalSince1970
            val ageMillis = location?.timestamp
                ?.let { ((nowSeconds - it.timeIntervalSince1970) * 1000.0).toLong() }
                ?: 0L

            complete(location?.takeIf { ageMillis <= maxAgeMillis }?.toGeoPoint())
        }

        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
            complete(null)
        }

        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            authorizationListener?.invoke(manager.authorizationStatus)
        }
    }

    fun status(): CLAuthorizationStatus = manager.authorizationStatus

    fun isGranted(): Boolean = status().let {
        it == kCLAuthorizationStatusAuthorizedWhenInUse || it == kCLAuthorizationStatusAuthorizedAlways
    }

    /** iOS quraşdırma başına yalnız bir dəfə soruşur; sonrası yalnız Ayarlardan dəyişir. */
    fun canPrompt(): Boolean = status() == kCLAuthorizationStatusNotDetermined

    fun requestAuthorization() = manager.requestWhenInUseAuthorization()

    fun observeAuthorization(listener: ((CLAuthorizationStatus) -> Unit)?) {
        authorizationListener = listener
    }

    suspend fun requestSingleFix(): GeoPoint? {
        // Əvvəlki gözləyən sorğu varsa onun nəticəsini paylaş — iki paralel `requestLocation()`
        // delegate-də bir-birini əvəzləyərdi.
        pending?.let { return it.await() }

        val deferred = CompletableDeferred<GeoPoint?>()
        pending = deferred
        manager.requestLocation()

        return deferred.await()
    }

    private fun complete(point: GeoPoint?) {
        val deferred = pending ?: return
        pending = null
        deferred.complete(point)
    }

    private fun CLLocation.toGeoPoint(): GeoPoint = coordinate.useContents {
        GeoPoint(
            latitude = latitude,
            longitude = longitude,
            // `verticalAccuracy` mənfi olanda hündürlük etibarsızdır (Apple sənədi);
            // belə halda çağıran tərəf onu ən yaxın şəhərdən götürür.
            elevationMeters = if (verticalAccuracy > 0) altitude.coerceIn(0.0, 9000.0) else 0.0,
        )
    }
}

/** Geocoder sorğusunun gözləmə həddi. Mövqe gözləməsindən qısadır: bu, yalnız etiketdir. */
private const val GEOCODE_TIMEOUT_MILLIS = 8_000L

@OptIn(ExperimentalForeignApi::class)
actual suspend fun reverseGeocode(point: GeoPoint): String? = withContext(Dispatchers.Main) {
    withTimeoutOrNull(GEOCODE_TIMEOUT_MILLIS) {
        val deferred = CompletableDeferred<String?>()

        // ⚠️ Geocoder lokal dəyişəndə saxlanılır və `await()` boyunca korutin çərçivəsi onu canlı
        // tutur. `CLLocationManager`-dəki weak-delegate tələsi burada YOXDUR — `CLGeocoder`
        // delegate işlətmir, nəticəni completion bloku ilə qaytarır.
        val geocoder = CLGeocoder()
        val location = CLLocation(latitude = point.latitude, longitude = point.longitude)

        geocoder.reverseGeocodeLocation(
            location,
            preferredLocale = NSLocale(localeIdentifier = appLocale().languageTag),
        ) { placemarks, _ ->
            val placemark = placemarks?.filterIsInstance<CLPlacemark>()?.firstOrNull()

            // Ən dar addan geniş ada — Android tərəflə eyni sıra.
            deferred.complete(
                (placemark?.locality
                    ?: placemark?.subAdministrativeArea
                    ?: placemark?.administrativeArea
                    ?: placemark?.name)
                    ?.takeIf { it.isNotBlank() }
            )
        }

        deferred.await()
    }
}
