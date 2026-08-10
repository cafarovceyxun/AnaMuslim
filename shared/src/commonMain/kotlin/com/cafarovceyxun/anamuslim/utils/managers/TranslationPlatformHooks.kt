package com.cafarovceyxun.anamuslim.utils.managers

/**
 * The one platform touchpoint the shared `TranslationViewModel` still needs, kept as an assignable
 * hook (the `QuranScriptPlatformHooks` pattern) rather than a full manager-source interface,
 * because it is a single call with a safe default.
 *
 * Registered by both hosts — `QuranApp.onCreate` (WorkManager) and `initSharedForIos`.
 *
 * (A pair of `get/setFetchTranslationsForce` hooks lived here too, wired to Android's
 * `SPAppActions`. They were removed once it turned out no shared code had read them since the
 * manifest fetch moved: registered on one platform, consumed on neither.)
 */
object TranslationPlatformHooks {

    /** Removes a translation's rows from the search index. No-op where indexing is unavailable. */
    var removeSlugFromSearchIndex: ((String) -> Unit)? = null
}
