package com.cafarovceyxun.anamuslim.utils.exceptions

import android.content.Context
import com.cafarovceyxun.anamuslim.utils.Log

class CustomExceptionHandler(
    private val ctx: Context
) : Thread.UncaughtExceptionHandler {
    private val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, exc: Throwable) {
        Log.saveCrash(ctx, exc)
        // NotificationUtils.showCrashNotification(ctx, ExceptionUtils.getStackTrace(exc)) // Disabled legacy crash reporting UI
        defaultExceptionHandler?.uncaughtException(thread, exc)
    }
}
