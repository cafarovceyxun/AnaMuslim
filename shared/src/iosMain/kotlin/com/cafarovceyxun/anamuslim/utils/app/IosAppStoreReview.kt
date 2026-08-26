package com.cafarovceyxun.anamuslim.utils.app

import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIApplication

/**
 * iOS half of the store-rating seam, over StoreKit's own rating sheet.
 *
 * The sheet is entirely Apple's: we cannot preselect a star count, read what the user chose, or
 * even know whether it appeared — iOS silently shows nothing once the per-user budget (a few times
 * a year) is spent. That is by design, and it is why the prompt books its own cooldown rather than
 * waiting for a result.
 */
object IosAppStoreReview : AppStoreReview {

    /** The App Store listing, live since 2026-08-15. */
    private const val APP_STORE_ID = "6799231138"

    override fun requestInAppRating(): Boolean {
        // The scene-based call is the only one that works in a multi-scene app; the old
        // parameterless `requestReview()` was deprecated in iOS 14 and does nothing under scenes.
        val scene = UIApplication.sharedApplication.keyWindow?.windowScene ?: return false

        SKStoreReviewController.requestReviewInScene(scene)
        return true
    }

    /**
     * Opens the listing on its review composer. `?action=write-review` is what turns the listing
     * into "write a review" — without it the user lands on the product page and has to find it.
     */
    override fun openReviewPage() {
        PlatformUtils.browseLink(
            "https://apps.apple.com/app/id$APP_STORE_ID?action=write-review",
        )
    }
}
