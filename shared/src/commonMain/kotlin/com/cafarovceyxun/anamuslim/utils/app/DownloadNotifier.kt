package com.cafarovceyxun.anamuslim.utils.app

import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strMsgDownloadFinished
import com.cafarovceyxun.anamuslim.resources.strMsgSomethingWrong
import org.jetbrains.compose.resources.getString

/**
 * "Download finished / failed" local notifications, as a settable sink (the [RepositoryProvider]
 * family pattern).
 *
 * **Android deliberately leaves this unset.** Its downloads run in WorkManager foreground workers
 * that already own a notification on the `downloads` channel, so posting here as well would
 * double-notify. iOS has no such worker: the download runs inside the app, and this is the only
 * thing that tells the user it ended.
 *
 * Terminal events only — no progress. iOS cannot show an ongoing, updating progress notification
 * the way an Android foreground service can, and the in-app screens already render progress while
 * the app is open.
 */
object DownloadNotifier {

    private var sink: ((title: String, body: String) -> Unit)? = null

    /** Registered at startup by the platform that wants the notifications (currently iOS only). */
    fun setSink(value: (title: String, body: String) -> Unit) {
        sink = value
    }

    /** [label] identifies what was downloaded — a book, a reciter, the hadith collection. */
    suspend fun completed(label: String) {
        val sink = sink ?: return
        sink(label, getString(Res.string.strMsgDownloadFinished))
    }

    suspend fun failed(label: String) {
        val sink = sink ?: return
        sink(label, getString(Res.string.strMsgSomethingWrong))
    }
}
