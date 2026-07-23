package com.cafarovceyxun.anamuslim.utils.managers

/**
 * The two small Android-only touchpoints the shared `TranslationViewModel` still needs, kept as
 * assignable hooks (the `QuranScriptPlatformHooks` pattern) rather than full manager-source
 * interfaces, because each is a single call with a safe default.
 *
 * Registered in `QuranApp.onCreate`; on iOS both defaults stand:
 *  - search indexing is WorkManager-backed and has no iOS counterpart yet (Faza 4/6),
 *  - the force-refetch flag lives in an Android `SharedPreferences` (`SPAppActions`), which the
 *    plan keeps in `:app`.
 */
object TranslationPlatformHooks {

    /** Removes a translation's rows from the search index. No-op where indexing is unavailable. */
    var removeSlugFromSearchIndex: ((String) -> Unit)? = null

    /** Reads the "force refetch the translations manifest" flag. Defaults to false. */
    var getFetchTranslationsForce: (() -> Boolean)? = null

    /** Clears the "force refetch" flag once the manifest has been fetched. */
    var setFetchTranslationsForce: ((Boolean) -> Unit)? = null
}
