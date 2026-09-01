package com.cafarovceyxun.anamuslim.activities

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import com.cafarovceyxun.anamuslim.activities.base.BaseActivity
import com.cafarovceyxun.anamuslim.compose.screens.prayer.PrayerTimesScreen
import com.cafarovceyxun.anamuslim.compose.theme.QuranAppTheme

/**
 * Android-də namaz vaxtları ayrıca Activity-dir — `AppNavHost` yalnız iOS-un hostudur, burada
 * ana ekrandan kənar hər şey öz Activity-sindədir (bax `ActivityReadHistory`).
 */
class ActivityPrayerTimes : BaseActivity() {
    override fun getLayoutResource() = 0

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        setContent {
            QuranAppTheme {
                PrayerTimesScreen()
            }
        }
    }
}
