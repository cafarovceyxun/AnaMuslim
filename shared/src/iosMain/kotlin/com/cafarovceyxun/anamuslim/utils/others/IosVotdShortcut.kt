package com.cafarovceyxun.anamuslim.utils.others

import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strTitleVOTD
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import com.cafarovceyxun.anamuslim.utils.verse.VerseUtilsHooks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import platform.UIKit.UIApplicationShortcutItem

/**
 * iOS counterpart of Android's `ShortcutUtils.pushVOTDShortcut`: a "verse of the day" Home-screen
 * quick action, published through the shared [IosQuickActions] registry alongside "continue
 * reading". A tap opens that verse via the shared [ReaderUiHooks.openVerse] hook.
 *
 * iOS also surfaces the daily verse as a local notification ([IosDailyReminder]); this is the
 * second, on-icon affordance Android users get, not a replacement.
 *
 * Registration point: [install] from the iOS bootstrap.
 */
object IosVotdShortcut {

    const val TYPE_VOTD = "votd"

    private const val KEY_CHAPTER = "chapterNo"
    private const val KEY_VERSE = "verseNo"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Wires the shared [VerseUtilsHooks.pushVotdShortcut] sink and the tap handler. */
    fun install() {
        IosQuickActions.registerHandler(TYPE_VOTD) { item -> open(item) }
        VerseUtilsHooks.pushVotdShortcut = { chapterNo, verseNo -> push(chapterNo, verseNo) }
    }

    private fun push(chapterNo: Int, verseNo: Int) {
        scope.launch {
            val title = getString(Res.string.strTitleVOTD)

            IosQuickActions.setItem(
                UIApplicationShortcutItem(
                    type = TYPE_VOTD,
                    localizedTitle = title,
                    localizedSubtitle = null,
                    icon = null,
                    userInfo = mapOf(
                        KEY_CHAPTER to chapterNo.toString(),
                        KEY_VERSE to verseNo.toString(),
                    ),
                ),
            )
        }
    }

    /** Cold launch can arrive before the reader hook is registered; poll briefly, then navigate. */
    private fun open(shortcutItem: UIApplicationShortcutItem) {
        val info = shortcutItem.userInfo ?: return
        val chapterNo = (info[KEY_CHAPTER] as? String)?.toIntOrNull() ?: return
        val verseNo = (info[KEY_VERSE] as? String)?.toIntOrNull() ?: return

        scope.launch {
            repeat(50) {
                val open = ReaderUiHooks.openVerse
                if (open != null) {
                    withContext(Dispatchers.Main) { open(chapterNo, verseNo) }
                    return@launch
                }
                delay(100)
            }
            AppLogger.d("IosVotdShortcut: reader hook never became available, tap dropped")
        }
    }
}
