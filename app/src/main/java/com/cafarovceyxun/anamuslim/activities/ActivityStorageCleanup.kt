package com.cafarovceyxun.anamuslim.activities

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import com.cafarovceyxun.anamuslim.activities.base.BaseActivity
import com.cafarovceyxun.anamuslim.compose.screens.storageCleanup.StorageCleanupScreen
import com.cafarovceyxun.anamuslim.compose.theme.QuranAppTheme

class ActivityStorageCleanup : BaseActivity() {

    override fun getLayoutResource() = 0

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        setContent {
            QuranAppTheme {
                StorageCleanupScreen()
            }
        }
    }
}
