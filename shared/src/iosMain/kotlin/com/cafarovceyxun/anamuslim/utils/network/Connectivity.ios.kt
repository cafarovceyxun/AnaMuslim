@file:OptIn(ExperimentalForeignApi::class)

package com.cafarovceyxun.anamuslim.utils.network

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.value
import platform.CoreFoundation.CFRelease
import platform.SystemConfiguration.SCNetworkReachabilityCreateWithAddress
import platform.SystemConfiguration.SCNetworkReachabilityFlagsVar
import platform.SystemConfiguration.SCNetworkReachabilityGetFlags
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsConnectionRequired
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsReachable
import platform.posix.AF_INET
import platform.posix.sockaddr
import platform.posix.sockaddr_in

/**
 * Synchronous reachability check against the "zero address" — the classic SystemConfiguration
 * approach, matching Android's immediate boolean. NWPathMonitor (async, path-observing) can replace
 * this in Faza 6 if continuous monitoring is needed; a one-shot boolean is all the shared callers use.
 */
actual fun isNetworkConnected(): Boolean = memScoped {
    val zeroAddress = alloc<sockaddr_in>()
    zeroAddress.sin_len = sizeOf<sockaddr_in>().convert()
    zeroAddress.sin_family = AF_INET.convert()

    val reachability = SCNetworkReachabilityCreateWithAddress(
        null,
        zeroAddress.ptr.reinterpret<sockaddr>(),
    ) ?: return@memScoped false

    val flags = alloc<SCNetworkReachabilityFlagsVar>()
    val gotFlags = SCNetworkReachabilityGetFlags(reachability, flags.ptr)
    CFRelease(reachability)

    if (!gotFlags) return@memScoped false

    val isReachable = flags.value and kSCNetworkReachabilityFlagsReachable != 0u
    val needsConnection = flags.value and kSCNetworkReachabilityFlagsConnectionRequired != 0u
    isReachable && !needsConnection
}
