package com.cafarovceyxun.anamuslim.utils.others

import com.cafarovceyxun.anamuslim.db.entities.user.ReadHistoryEntity

/**
 * Platform-neutral seam for surfacing "continue reading" outside the app, following the
 * startup-sink convention (`AppLogger`).
 *
 * Android registers a launcher-shortcut push (`ShortcutUtils`), iOS has no equivalent yet, so the
 * default is a no-op and unregistered use never crashes.
 *
 * Registration point: Android `QuranApp.onCreate()`.
 */
object ReadHistoryShortcuts {

    /** Called after read history is saved, with the entry to surface. */
    var pushLastVerses: (entity: ReadHistoryEntity) -> Unit = {}
}
