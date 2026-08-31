package com.cafarovceyxun.anamuslim.utils.app

import android.content.Context

/**
 * Android half of the store-rating seam.
 *
 * [requestInAppRating] is always false: Play's in-app review sheet only exists in
 * `com.google.android.play:review`, a proprietary library. Adding it would put a non-free
 * dependency in the APK, which the F-Droid / IzzyOnDroid plan in `OPEN_SOURCE_CHECKLIST.md` cannot
 * carry, so the prompt falls back to the listing instead. Play's own policy is satisfied either
 * way — what it forbids is asking for a particular star count or filtering who gets to see the
 * flow, not linking to the listing.
 */
class AndroidAppStoreReview(private val appContext: Context) : AppStoreReview {

    override fun requestInAppRating(): Boolean = false

    override fun openReviewPage() = PlayStore.open(appContext)

    // The https form, not `market://`: this is the string the share sheet sends to someone else and
    // the recipient may not be on Android at all (the same reasoning as `shareApp` in
    // `IndexMenuActionsProvider`).
    override val listingUrl: String get() = PlayStore.link(appContext, toMarket = false)

    override fun openListing() = PlayStore.open(appContext)
}
