package com.cafarovceyxun.anamuslim.utils.app

/**
 * Hand-off to the platform's store rating flow.
 *
 * Two methods because the two stores work differently: iOS has an OS-drawn rating sheet that can be
 * requested in place ([requestInAppRating]), while Android's equivalent needs the proprietary Play
 * Core library, which this project deliberately does not depend on (see `OPEN_SOURCE_CHECKLIST.md`
 * — an F-Droid build cannot contain it). Android therefore answers `false` and the caller falls
 * back to [openReviewPage], which opens the listing where the review is actually written.
 *
 * The star count is never ours to set: both stores forbid asking for a specific rating, and iOS in
 * particular gives the app no control over the sheet at all. The prompt's job is to ask for an
 * honest rating at a moment that is not in the way.
 */
interface AppStoreReview {

    /**
     * Asks the OS to show its own rating sheet, returning false when the platform has none.
     *
     * A `true` is **not** a promise that anything appeared: iOS rate-limits the sheet (a handful of
     * times a year per user) and shows nothing when the budget is spent, without telling the app.
     * That is the reason the prompt records "asked" on its own side rather than waiting for a
     * result — see [com.cafarovceyxun.anamuslim.compose.utils.preferences.ReviewPromptPreferences].
     */
    fun requestInAppRating(): Boolean

    /** Opens the public store listing, on its "write a review" entry where the store supports it. */
    fun openReviewPage()
}

/**
 * Startup seam handing shared code the platform's [AppStoreReview].
 *
 * Registration points: Android `QuranApp.onCreate()`, iOS `initSharedForIos()`.
 */
object AppStoreReviewProvider {

    private var provider: (() -> AppStoreReview)? = null

    /** Registers how the [AppStoreReview] is obtained on this platform. Call once at startup. */
    fun setProvider(provider: () -> AppStoreReview) {
        this.provider = provider
    }

    val review: AppStoreReview get() = provider?.invoke() ?: NoAppStoreReview

    /**
     * Whether a platform actually registered a hand-off.
     *
     * The inert default below keeps an unregistered seam from crashing, but it cannot make the
     * feature work — so both the prompt and the overflow menu ask this first and stay hidden
     * otherwise, rather than showing a button that quietly does nothing (the rule in `CLAUDE.md`).
     */
    val isAvailable: Boolean get() = provider != null
}

private object NoAppStoreReview : AppStoreReview {
    override fun requestInAppRating() = false
    override fun openReviewPage() = Unit
}
