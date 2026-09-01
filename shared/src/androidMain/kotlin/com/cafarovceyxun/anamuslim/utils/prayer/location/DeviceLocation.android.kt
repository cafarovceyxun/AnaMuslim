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

/** Təzə siqnal gözləməzdən əvvəl nə qədər köhnə mövqe qəbul edilir. */
private const val LAST_KNOWN_MAX_AGE_MILLIS = 24L * 60L * 60L * 1000L

/** Bir dəfəlik siqnal gözləmə həddi — bundan sonra istifadəçi əl ilə şəhər seçir. */
private const val FIX_TIMEOUT_MILLIS = 15_000L

private const val LOG_TAG = "prayer.location"

actual suspend fun currentDeviceLocation(): GeoPoint? = withContext(Dispatchers.Main) {
    val context = AndroidPlatformContext.context

    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED
    ) {
        return@withContext null
    }

    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return@withContext null

    // Namaz vaxtı üçün bir günlük köhnə mövqe də kifayətdir (şəhər dəyişməyibsə fərq sıfırdır),
    // ona görə əvvəlcə keşə baxılır — istifadəçi düymə basan kimi nəticə görür.
    lastKnownPoint(manager)?.let { return@withContext it }

    withTimeoutOrNull(FIX_TIMEOUT_MILLIS) { awaitSingleFix(manager) }
}

private fun lastKnownPoint(manager: LocationManager): GeoPoint? {
    val now = System.currentTimeMillis()

    return coarseProviders(manager)
        .mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }
                .onFailure { AppLogger.saveError(it, LOG_TAG) }
                .getOrNull()
        }
        .filter { now - it.time <= LAST_KNOWN_MAX_AGE_MILLIS }
        .maxByOrNull { it.time }
        ?.toGeoPoint()
}

/**
 * Tək mövqe gözləyir.
 *
 * `getCurrentLocation` API 30+-dadır, layihənin `minSdk`-ı isə 24 — ona görə klassik dinləyici
 * işlədilir və ilk siqnaldan sonra dərhal söndürülür.
 */
private suspend fun awaitSingleFix(manager: LocationManager): GeoPoint? =
    suspendCancellableCoroutine { continuation ->
        val provider = coarseProviders(manager).firstOrNull {
            runCatching { manager.isProviderEnabled(it) }.getOrDefault(false)
        }

        if (provider == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                runCatching { manager.removeUpdates(this) }
                if (continuation.isActive) continuation.resume(location.toGeoPoint())
            }

            // API 24-də bu üçlük abstraktdır — override olmadan kompilyasiya keçmir.
            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) {
                runCatching { manager.removeUpdates(this) }
                if (continuation.isActive) continuation.resume(null)
            }

            @Deprecated("API 29-dan çağırılmır, amma API 24-də abstraktdır")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }

        continuation.invokeOnCancellation { runCatching { manager.removeUpdates(listener) } }

        val started = runCatching {
            manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
        }.onFailure { AppLogger.saveError(it, LOG_TAG) }

        if (started.isFailure && continuation.isActive) continuation.resume(null)
    }

/** GPS qəsdən sonuncudur: şəbəkə mövqeyi qapalı yerdə də gəlir və bu dəqiqlik üçün yetərlidir. */
private fun coarseProviders(manager: LocationManager): List<String> =
    listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
        .filter { it in manager.allProviders }

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
