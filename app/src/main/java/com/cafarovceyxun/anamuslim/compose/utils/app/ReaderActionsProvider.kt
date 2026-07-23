package com.cafarovceyxun.anamuslim.compose.utils.app

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.cafarovceyxun.anamuslim.activities.ActivitySettings
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderActions
import com.cafarovceyxun.anamuslim.utils.univ.Keys

/**
 * Android implementations of the reader chrome's [ReaderActions], mirroring [rememberPlayerActions].
 * Carries the same extras `ReaderAppBar.openReaderSetting` used before the seam.
 */
@Composable
fun rememberReaderActions(): ReaderActions {
    val context = LocalContext.current
    return remember(context) {
        ReaderActions(
            onOpenReaderSettings = {
                context.startActivity(
                    // `false` = the full settings screen. The reader-only filter used to hide the
                    // app, download and management sections behind this button; opening everything
                    // is the product decision, and iOS's `rememberNavReaderActions` matches it.
                    Intent(context, ActivitySettings::class.java).apply {
                        putExtra(Keys.SHOW_READER_SETTINGS_ONLY, false)
                    },
                    null
                )
            }
        )
    }
}
