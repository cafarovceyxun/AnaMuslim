package com.cafarovceyxun.anamuslim.utils.network

import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strMsgNoInternetLong
import org.jetbrains.compose.resources.getString

/**
 * Platform connectivity check — the commonMain seam that replaces the Android-only
 * `NetworkStateReceiver.isNetworkConnected(context)` for shared code. Android resolves it via
 * `ConnectivityManager` active-network transports; iOS via `SCNetworkReachability`.
 *
 * Faza-5 network-dependent logic moving to `commonMain` (ChapterInfo, Scripts, download flows)
 * calls this instead of passing a `Context`.
 */
expect fun isNetworkConnected(): Boolean

/**
 * [isNetworkConnected] plus the user-facing "no internet" toast — the commonMain equivalent of
 * `NetworkStateReceiver.canProceed(context)` for shared code that gates an action on connectivity.
 */
suspend fun canProceedOnline(): Boolean {
    if (isNetworkConnected()) return true
    PlatformUtils.showToast(getString(Res.string.strMsgNoInternetLong))
    return false
}
