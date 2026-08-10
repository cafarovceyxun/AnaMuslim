package com.cafarovceyxun.anamuslim.compose.utils.app

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration

fun toggleScreenRotation(activity: Activity) {
    val currentOrientation = activity.resources.configuration.orientation
    
    activity.requestedOrientation = if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    } else {
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
}
