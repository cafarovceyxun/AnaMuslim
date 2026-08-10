package com.cafarovceyxun.anamuslim.compose.utils.app

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.cafarovceyxun.anamuslim.R
import com.cafarovceyxun.anamuslim.activities.ActivityAbout
import com.cafarovceyxun.anamuslim.activities.ActivityBookmark
import com.cafarovceyxun.anamuslim.activities.ActivityExportImport
import com.cafarovceyxun.anamuslim.activities.ActivitySettings
import com.cafarovceyxun.anamuslim.activities.ActivityStorageCleanup
import com.cafarovceyxun.anamuslim.compose.components.IndexMenuActions
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.utils.app.PlayStore

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
                PlayStore.open(context)
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
                PlayStore.open(context)
            }
        )
    }
}

private fun shareApp(ctx: Context) {
    // The https listing, not `market://` - the recipient may not be on Android at all.
    val text = ctx.getString(R.string.strMsgShareApp, PlayStore.link(ctx, toMarket = false))

    PlatformUtils.shareText(text, ctx.getString(R.string.strTitleShareApp))
}
