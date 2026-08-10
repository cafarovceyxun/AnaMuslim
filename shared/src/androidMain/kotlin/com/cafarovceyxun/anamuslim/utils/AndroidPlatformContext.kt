package com.cafarovceyxun.anamuslim.utils

import android.annotation.SuppressLint
import android.content.Context

/**
 * Android-specific holder for the application context, initialized at startup.
 */
@SuppressLint("StaticFieldLeak")
object AndroidPlatformContext {
    /** The application context, set in `QuranApp.onCreate`. */
    lateinit var context: Context
}
