package com.cafarovceyxun.anamuslim.compose.utils.app

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.cafarovceyxun.anamuslim.activities.ActivitySettings
import com.cafarovceyxun.anamuslim.compose.components.player.PlayerActions
import com.cafarovceyxun.anamuslim.compose.navigation.SettingRoutes
import com.cafarovceyxun.anamuslim.utils.univ.Keys

/** Android implementations of the player's [PlayerActions], mirroring [rememberIndexMenuActions]. */
@Composable
fun rememberPlayerActions(): PlayerActions {
    val context = LocalContext.current
    return remember(context) {
        PlayerActions(
            onOpenRecitationDownloads = {
                val intent = Intent(context, ActivitySettings::class.java)
                intent.putExtra(Keys.NAV_DESTINATION, SettingRoutes.RECITATION_DOWNLOAD)
                context.startActivity(intent)
            }
        )
    }
}
