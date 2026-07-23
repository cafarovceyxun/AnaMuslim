package com.cafarovceyxun.anamuslim.compose.utils.app

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.cafarovceyxun.anamuslim.activities.ActivitySettings
import com.cafarovceyxun.anamuslim.compose.screens.hadith.HadithActions

/** Android implementations of [HadithActions], mirroring [rememberReaderActions]. */
@Composable
fun rememberHadithActions(): HadithActions {
    val context = LocalContext.current
    return remember(context) {
        HadithActions(
            onOpenSettings = {
                context.startActivity(Intent(context, ActivitySettings::class.java))
            }
        )
    }
}
