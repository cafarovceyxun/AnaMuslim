package com.cafarovceyxun.anamuslim.utils.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Play Store links, replacing the `AppBridge.Opener.openPlayStore` / `AppBridge.preparePlayStoreLink`
 * pair from the removed `peacedesign` module.
 *
 * ⚠️ The old helper stripped a `.debug` suffix from the package name, but this project's debug
 * builds use `applicationIdSuffix = ".test"` - so on a debug build it produced a link to
 * `com.cafarovceyxun.anamuslim.test`, which is not a published listing and lands on a Play error
 * page. The suffix stripped here matches the one the build actually applies.
 */
object PlayStore {

    /**
     * [toMarket] picks the `market://` form, which opens the Play app directly; the https form is
     * the fallback for devices where the Play app is absent.
     */
    fun link(context: Context, toMarket: Boolean): String {
        val pkg = context.packageName.removeSuffix(".test")

        return if (toMarket) {
            "market://details?id=$pkg"
        } else {
            "https://play.google.com/store/apps/details?id=$pkg"
        }
    }

    fun open(context: Context) {
        try {
            context.startActivity(viewIntent(link(context, toMarket = true)))
        } catch (ignored: ActivityNotFoundException) {
            // No Play app on this device - the web listing still works.
            context.startActivity(viewIntent(link(context, toMarket = false)))
        }
    }

    private fun viewIntent(uri: String) =
        Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
