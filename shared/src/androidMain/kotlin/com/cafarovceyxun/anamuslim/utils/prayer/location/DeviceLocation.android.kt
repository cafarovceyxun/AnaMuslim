package com.cafarovceyxun.anamuslim.utils.prayer.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.Build
import androidx.annotation.RequiresApi
import com.cafarovceyxun.anamuslim.compose.utils.appLocale
import java.util.Locale
import androidx.core.content.ContextCompat
import com.cafarovceyxun.anamuslim.utils.AndroidPlatformContext
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.prayer.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Bir dəfəlik siqnal gözləmə həddi — bundan sonra istifadəçi əl ilə şəhər seçir. */
private const val FIX_TIMEOUT_MILLIS = 15_000L

private const val LOG_TAG = "prayer.location"

actual suspend fun currentDeviceLocation(maxCacheAgeMillis: Long): GeoPoint? = withContext(Dispatchers.Main) {
    val context = AndroidPlatformContext.context

    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED
    ) {
        return@withContext null
    }

    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return@withContext null

    // Keş yalnız icazə verilən yaş daxilində işlədilir. Qiblə ekranı bunu qısa saxlayır
    // ([QIBLA_LOCATION_MAX_AGE_MILLIS]) — səbəbi orada yazılıb.
    if (maxCacheAgeMillis > 0L) {
        lastKnownPoint(manager, maxCacheAgeMillis)?.let { return@withContext it }
    }

    awaitBestFix(manager, FIX_TIMEOUT_MILLIS)
}

/** Bu dəqiqlikdən yaxşı siqnal gələn kimi gözləmə dayanır. */
private const val GOOD_ACCURACY_METERS = 50f

/**
 * Keşdən qəbul edilən ən pis dəqiqlik — **yalnız dəqiq icazə varsa** tətbiq olunur.
 *
 * Kobud icazə ilə sistem onsuz da ~2 km-lik dəyər verir; orada bu filtri qoymaq hər dəfə boş yerə
 * canlı siqnal gözləməyə səbəb olardı.
 */
private const val ACCEPTABLE_CACHE_ACCURACY_METERS = 200f

private fun hasFinePermission(): Boolean = ContextCompat.checkSelfPermission(
    AndroidPlatformContext.context,
    Manifest.permission.ACCESS_FINE_LOCATION,
) == PackageManager.PERMISSION_GRANTED

private fun lastKnownPoint(manager: LocationManager, maxAgeMillis: Long): GeoPoint? {
    val now = System.currentTimeMillis()
    val demandAccuracy = hasFinePermission()

    return locationProviders(manager)
        .mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }
                .onFailure { AppLogger.saveError(it, LOG_TAG) }
                .getOrNull()
        }
        .filter { now - it.time <= maxAgeMillis }
        // Dəqiq icazə varsa keşdəki kobud dəyər qəbul edilmir: qiblə üçün məhz o dəyər yanıldırdı.
        .filter { !demandAccuracy || (it.hasAccuracy() && it.accuracy <= ACCEPTABLE_CACHE_ACCURACY_METERS) }
        .maxByOrNull { it.time }
        ?.toGeoPoint()
}

/**
 * Ən yaxşı mövqeni gözləyir.
 *
 * ⚠️ **Tək provayderdən ilk gələn siqnalı götürmək düzgün deyil.** Şəbəkə provayderi demək olar
 * dərhal cavab verir, amma kobud dəyər qaytarır; GPS gec gəlir və dəqiqdir. Əvvəlki tətbiq ilk
 * siqnalı götürürdü, yəni praktikada həmişə kobud dəyəri seçirdi.
 *
 * Ona görə burada **bütün aktiv provayderlərə** abunə olunur, [GOOD_ACCURACY_METERS]-dən yaxşı
 * siqnal gələn kimi dayanılır, gəlməsə də gözləmə bitəndə **ən yaxşısı** qaytarılır — yəni qapalı
 * yerdə GPS heç vaxt tutmasa belə funksiya boş qayıtmır.
 *
 * `getCurrentLocation` API 30+-dadır, layihənin `minSdk`-ı isə 24 — ona görə klassik dinləyici.
 */
private suspend fun awaitBestFix(manager: LocationManager, timeoutMillis: Long): GeoPoint? {
    var best: Location? = null

    val settled = withTimeoutOrNull(timeoutMillis) {
        suspendCancellableCoroutine { continuation ->
            val providers = locationProviders(manager).filter {
                runCatching { manager.isProviderEnabled(it) }.getOrDefault(false)
            }

            if (providers.isEmpty()) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (isBetter(location, best)) best = location

                    if (location.hasAccuracy() && location.accuracy <= GOOD_ACCURACY_METERS) {
                        runCatching { manager.removeUpdates(this) }
                        if (continuation.isActive) continuation.resume(location.toGeoPoint())
                    }
                }

                // API 24-də bu üçlük abstraktdır — override olmadan kompilyasiya keçmir.
                override fun onProviderEnabled(provider: String) = Unit

                // Bir provayder sönsə də digərləri qalır; qərarı gözləmə həddi verir.
                override fun onProviderDisabled(provider: String) = Unit

                @Deprecated("API 29-dan çağırılmır, amma API 24-də abstraktdır")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            }

            continuation.invokeOnCancellation { runCatching { manager.removeUpdates(listener) } }

            val started = providers.map { provider ->
                runCatching {
                    manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                }.onFailure { AppLogger.saveError(it, LOG_TAG) }
            }

            if (started.all { it.isFailure } && continuation.isActive) continuation.resume(null)
        }
    }

    // Gözləmə bitdi: dəqiq siqnal gəlməsə də əldəkinin ən yaxşısı qaytarılır.
    return settled ?: best?.toGeoPoint()
}

/** Dəqiqliyi məlum olan siqnal həmişə üstündür; ikisi də məlumdursa kiçik xəta qalib gəlir. */
private fun isBetter(candidate: Location, current: Location?): Boolean {
    if (current == null) return true
    if (!candidate.hasAccuracy()) return false
    if (!current.hasAccuracy()) return true

    return candidate.accuracy < current.accuracy
}

/**
 * İşlədilən provayderlər.
 *
 * Dəqiq icazə varsa GPS **birincidir** — sıralama [awaitBestFix]-də abunə sırasını, [lastKnownPoint]-də
 * isə bərabər yaşlı siqnallar arasında üstünlüyü təyin edir.
 */
private fun locationProviders(manager: LocationManager): List<String> {
    val ordered = if (hasFinePermission()) {
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
    } else {
        listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
    }

    return ordered.filter { it in manager.allProviders }
}

private fun Location.toGeoPoint(): GeoPoint = GeoPoint(
    latitude = latitude,
    longitude = longitude,
    // Şəbəkə mövqeyində hündürlük olmur; çağıran tərəf onu ən yaxın şəhərdən götürür.
    elevationMeters = if (hasAltitude()) altitude.coerceIn(0.0, 9000.0) else 0.0,
)

/** Geocoder sorğusunun gözləmə həddi. Mövqe gözləməsindən qısadır: bu, yalnız etiketdir. */
private const val GEOCODE_TIMEOUT_MILLIS = 8_000L

actual suspend fun reverseGeocode(point: GeoPoint): String? = withContext(Dispatchers.IO) {
    // Bəzi cihazlarda (Google xidmətləri olmayan ROM-lar) geocoder ümumiyyətlə yoxdur.
    if (!Geocoder.isPresent()) return@withContext null

    val geocoder = runCatching {
        Geocoder(AndroidPlatformContext.context, Locale.forLanguageTag(appLocale().languageTag))
    }.getOrNull() ?: return@withContext null

    withTimeoutOrNull(GEOCODE_TIMEOUT_MILLIS) {
        val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            awaitAddresses(geocoder, point)
        } else {
            // API 33-dən əvvəl yalnız bloklayan variant var; ona görə `Dispatchers.IO`-dayıq.
            @Suppress("DEPRECATION")
            runCatching { geocoder.getFromLocation(point.latitude, point.longitude, 1) }
                .onFailure { AppLogger.saveError(it, LOG_TAG) }
                .getOrNull()
        }

        addresses?.firstOrNull()?.let { address ->
            // Ən dar addan geniş ada: Gədəbəy kimi kiçik yerlər `locality`-də gəlir.
            address.locality
                ?: address.subAdminArea
                ?: address.adminArea
                ?: address.featureName
        }?.takeIf { it.isNotBlank() }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private suspend fun awaitAddresses(geocoder: Geocoder, point: GeoPoint): List<Address>? =
    suspendCancellableCoroutine { continuation ->
        geocoder.getFromLocation(
            point.latitude,
            point.longitude,
            1,
            object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    if (continuation.isActive) continuation.resume(addresses)
                }

                override fun onError(errorMessage: String?) {
                    if (continuation.isActive) continuation.resume(null)
                }
            },
        )
    }
