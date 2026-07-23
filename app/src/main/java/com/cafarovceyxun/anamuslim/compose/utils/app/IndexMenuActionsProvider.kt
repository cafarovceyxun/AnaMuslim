package com.cafarovceyxun.anamuslim.compose.utils.app

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.peacedesign.android.utils.AppBridge
import com.cafarovceyxun.anamuslim.R
import com.cafarovceyxun.anamuslim.activities.ActivityAbout
import com.cafarovceyxun.anamuslim.activities.ActivityBookmark
import com.cafarovceyxun.anamuslim.activities.ActivityExportImport
import com.cafarovceyxun.anamuslim.activities.ActivitySettings
import com.cafarovceyxun.anamuslim.activities.ActivityStorageCleanup
import com.cafarovceyxun.anamuslim.compose.components.IndexMenuActions

@Composable
fun rememberIndexMenuActions(): IndexMenuActions {
    val context = LocalContext.current
    return remember(context) {
        IndexMenuActions(
            onOpenBookmarks = {
                context.startActivity(Intent(context, ActivityBookmark::class.java))
            },
            onOpenSettings = {
                context.startActivity(Intent(context, ActivitySettings::class.java))
            },
            onOpenStorageCleanup = {
                context.startActivity(Intent(context, ActivityStorageCleanup::class.java))
            },
            onOpenPlayStore = {
                AppBridge.newOpener(context).openPlayStore(null)
            },
            onOpenExportImport = {
                context.startActivity(Intent(context, ActivityExportImport::class.java))
            },
            onOpenAboutUs = {
                context.startActivity(Intent(context, ActivityAbout::class.java))
            },
            onShareApp = {
                shareApp(context)
            },
            onRateApp = {
                AppBridge.newOpener(context).openPlayStore(null)
            }
        )
    }
}

private fun shareApp(ctx: Context) {
    val sharer = AppBridge.newSharer(ctx)
    sharer.setData(
        ctx.getString(
            R.string.strMsgShareApp,
            AppBridge.preparePlayStoreLink(ctx, false, null)
        )
    )
        .setPlatform(AppBridge.Platform.SYSTEM_SHARE)
        .setChooserTitle(ctx.getString(R.string.strTitleShareApp))

    sharer.share()
}
