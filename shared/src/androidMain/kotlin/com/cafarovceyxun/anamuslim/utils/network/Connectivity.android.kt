package com.cafarovceyxun.anamuslim.utils.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * App-set holder for the application [Context]. `QuranApp` assigns this at startup — shared's
 * androidMain has no direct `Context` (same seam pattern as [AndroidAppFiles]/`DataStoreManager`).
 */
object AndroidConnectivity {
    lateinit var context: Context
}

/**
 * Mirrors `NetworkStateReceiver.isNetworkConnected` (minSdk 24, so the modern API is always
 * available). The app-side receiver is left untouched; this is the no-Context commonMain seam.
 */
actual fun isNetworkConnected(): Boolean {
    val mgr = AndroidConnectivity.context
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = mgr.activeNetwork ?: return false
    val caps = mgr.getNetworkCapabilities(network) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
        caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)
}
