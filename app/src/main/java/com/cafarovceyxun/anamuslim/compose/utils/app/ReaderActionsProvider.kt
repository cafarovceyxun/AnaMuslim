package com.cafarovceyxun.anamuslim.compose.utils.app

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.cafarovceyxun.anamuslim.activities.ActivitySettings
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderActions

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
                context.startActivity(Intent(context, ActivitySettings::class.java), null)
            }
        )
    }
}
