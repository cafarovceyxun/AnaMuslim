package com.cafarovceyxun.anamuslim.utils.prayer.location

import com.cafarovceyxun.anamuslim.utils.prayer.GeoPoint
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLLocationAccuracyKilometer
import platform.Foundation.NSError
import platform.darwin.NSObject

/** Bir dəfəlik siqnal gözləmə həddi — Android tərəflə eyni. */
private const val FIX_TIMEOUT_MILLIS = 15_000L

actual suspend fun currentDeviceLocation(): GeoPoint? = withContext(Dispatchers.Main) {
    if (!IosLocationAuthorization.isGranted()) return@withContext null

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
            desiredAccuracy = kCLLocationAccuracyKilometer
            delegate = handler
        }
    }

    private var pending: CompletableDeferred<GeoPoint?>? = null
    private var authorizationListener: ((CLAuthorizationStatus) -> Unit)? = null

    private val handler = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            val location = didUpdateLocations.filterIsInstance<CLLocation>().lastOrNull()
            complete(location?.toGeoPoint())
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
