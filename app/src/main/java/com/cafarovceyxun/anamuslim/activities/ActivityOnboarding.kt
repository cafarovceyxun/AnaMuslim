package com.cafarovceyxun.anamuslim.activities

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import com.cafarovceyxun.anamuslim.activities.base.BaseActivity
import com.cafarovceyxun.anamuslim.compose.screens.onboarding.OnboardingGate
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.compose.screens.onboarding.OnboardingScreen
import com.cafarovceyxun.anamuslim.compose.theme.QuranAppTheme
import kotlinx.coroutines.runBlocking
import com.cafarovceyxun.anamuslim.utils.sharedPrefs.SPAppActions.setRequireOnboarding

class ActivityOnboarding : BaseActivity() {

    override fun getLayoutResource() = 0

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        setContent {
            QuranAppTheme {
                OnboardingScreen(onComplete = ::takeOff, onExit = ::finish)
            }
        }
    }

    private fun takeOff() {
        OnboardingGate.markCompleted()
        setRequireOnboarding(this, false)

        // Blocking on purpose: this activity is about to finish, and a fire-and-forget coroutine
        // could lose the write — which would send the user back through onboarding next launch.
        runBlocking {
            AppPreferences.setOnboardingCompletedVersion(OnboardingGate.REQUIRED_VERSION)
        }
        launchMainActivity()
        finish()
    }
}
