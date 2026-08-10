package com.cafarovceyxun.anamuslim.utils.verse

/**
 * The one platform touchpoint the verse-of-the-day logic still has.
 *
 * The `isVOTD`/`getVOTD` hooks that used to live here were removed: they were never registered on
 * either platform, which is exactly why the feature was dead (see [VerseUtils]). The logic is
 * shared now, so only the launcher-shortcut publish — an Android concept with no iOS counterpart —
 * remains a hook.
 */
object VerseUtilsHooks {
    /** Publishes/updates the "verse of the day" launcher shortcut. No-op where absent. */
    var pushVotdShortcut: ((chapterNo: Int, verseNo: Int) -> Unit)? = null
}
