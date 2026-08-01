package com.cafarovceyxun.anamuslim.activities

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import com.cafarovceyxun.anamuslim.activities.base.BaseActivity
import com.cafarovceyxun.anamuslim.compose.screens.ExportImportScreen
import com.cafarovceyxun.anamuslim.compose.theme.QuranAppTheme

/**
 * Android host for the shared Export/Import screen.
 *
 * The whole backup implementation used to live here — file format, preference writes, SAF
 * launchers. Only the theme wrapper is Android-specific now: the format work moved to
 * `ExportImportManager` and the document picker to the `TextDocumentSaver`/`TextDocumentOpener`
 * seam, so iOS gets the same screen working instead of two inert buttons.
 *
 * The post-import Activity restart is gone with it: importing a language now goes through
 * `AppLocaleHooks.applyLanguage`, which drives `AppCompatDelegate` and recreates activities itself,
 * and every other imported setting is DataStore-backed, so Compose picks it up live.
 */
class ActivityExportImport : BaseActivity() {

    override fun getLayoutResource() = 0

    override fun shouldInflateAsynchronously() = false

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        setContent {
            QuranAppTheme {
                ExportImportScreen()
            }
        }
    }
}
