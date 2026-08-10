package com.cafarovceyxun.anamuslim.utils.extensions

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper

/**
 * Since traditional [SharedPreferences.Editor] does not have a putDouble method, therefore here is the extension function.
 * The double stored by this method must be retrieved using [getDouble].
 * It converts the [Double] into its [Long] equivalent using [Double.doubleToRawLongBits][java.lang.Double.doubleToRawLongBits]
 * and stores it as Long.
 * */
fun SharedPreferences.Editor.putDouble(key: String, double: Double): SharedPreferences.Editor =
    putLong(key, java.lang.Double.doubleToRawLongBits(double))

/**
 * Since traditional [SharedPreferences.Editor] does not have a getDouble method, therefore here is the extension function.
 * The double being retrieved by this method must have been stored using [putDouble], which stored the double as its long equivalent.
 * It converts the [Long] into its original [Double] equivalent using [Double.longBitsToDouble][java.lang.Double.longBitsToDouble].
 * */
fun SharedPreferences.getDouble(key: String, default: Double) =
    java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(default)))

inline fun runOnTimeout(crossinline block: () -> Unit, timeoutMillis: Long) {
    Handler(Looper.getMainLooper()).postDelayed({
        block()
    }, timeoutMillis)
}

// interval

fun runOnInterval(
    block: () -> Unit,
    intervalMillis: Long,
    runImmediately: Boolean = false
): Handler {
    val handler = Handler(Looper.getMainLooper())
    handler.postDelayed({
        block()
        runOnInterval(block, intervalMillis)
    }, intervalMillis)

    if (runImmediately) {
        block()
    }

    return handler
}
