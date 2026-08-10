package com.cafarovceyxun.anamuslim.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cafarovceyxun.anamuslim.compose.screens.GreetingSplash
import com.cafarovceyxun.anamuslim.compose.screens.MainScreen
import com.cafarovceyxun.anamuslim.compose.theme.QuranAppTheme
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppLocale
import com.cafarovceyxun.anamuslim.compose.utils.appLocaleFlow
import com.cafarovceyxun.anamuslim.compose.utils.wrapContextWithAppLocale
import com.cafarovceyxun.anamuslim.utils.sharedPrefs.SPAppConfigs
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

    /** The stored language this instance was built against — see the recreate in [onCreate]. */
    private var languageTagAtCreate: String? = null

    // Below API 33 nothing applies the chosen language to this activity on its own: the platform
    // has no per-app language, and AppCompat's backport only reaches `AppCompatActivity`s (the
    // legacy `BaseActivity` screens), never a plain `ComponentActivity` like this one.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(wrapContextWithAppLocale(newBase))
    }

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

        languageTagAtCreate = SPAppConfigs.getLocale(this)

        setContent {
            val appLocale by appLocaleFlow.collectAsState()

            // API 33+ recreates us itself when `LocaleManager.applicationLocales` changes. Below
            // that, `attachBaseContext` above only reads the stored language once per instance, so
            // without this the picker would leave the whole app in the previous language until the
            // next cold start.
            LaunchedEffect(appLocale.rawLanguageTag) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
                    appLocale.rawLanguageTag != languageTagAtCreate
                ) {
                    recreate()
                }
            }

            CompositionLocalProvider(LocalAppLocale provides appLocale) {
                QuranAppTheme {
                    // The greeting overlays the app rather than gating it: MainScreen composes and
                    // loads underneath while it plays, so it costs no startup time.
                    Box(Modifier.fillMaxSize()) {
                        MainScreen(readerLaunchParamsFlow)
                        GreetingSplash()
                    }
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
