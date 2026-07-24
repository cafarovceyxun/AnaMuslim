package com.cafarovceyxun.anamuslim.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cafarovceyxun.anamuslim.compose.screens.MainScreen
import com.cafarovceyxun.anamuslim.compose.theme.QuranAppTheme
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppLocale
import com.cafarovceyxun.anamuslim.compose.utils.appLocaleFlow
import com.cafarovceyxun.anamuslim.utils.app.AppActions.checkForCrashLogs
import com.cafarovceyxun.anamuslim.utils.app.AppActions.scheduleActions
import com.cafarovceyxun.anamuslim.utils.app.UpdateManager
import com.cafarovceyxun.anamuslim.compose.screens.onboarding.OnboardingGate
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.utils.sharedPrefs.SPAppActions
import com.cafarovceyxun.anamuslim.views.reader.updateAllVotdWidgets

import android.view.KeyEvent
import com.cafarovceyxun.anamuslim.viewModels.HadithViewModel
import com.cafarovceyxun.anamuslim.viewModels.handleKeyEvent
import com.cafarovceyxun.anamuslim.viewModels.ReaderViewModel
import androidx.activity.viewModels
import com.cafarovceyxun.anamuslim.utils.reader.ReaderLaunchParams
import kotlinx.coroutines.flow.MutableStateFlow

import com.cafarovceyxun.anamuslim.utils.reader.ReaderIntentHandler

class MainActivity : ComponentActivity() {
    private val readerVm: ReaderViewModel by viewModels()
    private val hadithVm: HadithViewModel by viewModels()

    private val readerLaunchParamsFlow = MutableStateFlow<ReaderLaunchParams?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val params = ReaderIntentHandler.validateIntent(intent)
        if (params != null) {
            readerLaunchParamsFlow.value = params
        }
    }

    /** Keycodes whose ACTION_DOWN a reader consumed — see [onKeyUp]. */
    private val consumedKeyDowns = mutableSetOf<Int>()

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // If we are in Hadith mode, prioritize HadithViewModel
        // This is a bit of a heuristic but helps avoid conflicts
        if (hadithVm.handleKeyEvent(keyCode) || readerVm.handleKeyEvent(keyCode)) {
            consumedKeyDowns.add(keyCode)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        // The window also acts on a volume key's release, so swallowing only ACTION_DOWN still let
        // the system volume panel appear on top of the page we just turned. The release itself must
        // not navigate again — it is consumed, not handled.
        if (consumedKeyDowns.remove(keyCode)) return true
        return super.onKeyUp(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 12+ Splash Screen API
        installSplashScreen()
        
        super.onCreate(savedInstanceState)

        // Critical Update Check
        if (UpdateManager.getInstance(this).check4CriticalUpdate()) {
            return
        }

        // Onboarding Check — driven by the shared gate, which stores *which version* of onboarding
        // the user finished, so a release can demand it again. The legacy `SPAppActions` flag is
        // still honoured for installs that predate the shared preference.
        val completedVersion = AppPreferences.getOnboardingCompletedVersion()
        if (OnboardingGate.shouldShow(completedVersion) || SPAppActions.getRequireOnboarding(this)) {
            startActivity(Intent(this, ActivityOnboarding::class.java))
            finish()
            return
        }

        // System UI configuration
        enableEdgeToEdge()

        // Background Actions
        initBackgroundActions()

        handleIntent(intent)

        setContent {
            val appLocale by appLocaleFlow.collectAsState()
            
            CompositionLocalProvider(LocalAppLocale provides appLocale) {
                QuranAppTheme {
                    MainScreen(readerLaunchParamsFlow)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateAllVotdWidgets(this)
    }

    private fun initBackgroundActions() {
        scheduleActions(this)
        checkForCrashLogs(this)
    }
}
