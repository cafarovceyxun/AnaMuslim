package com.cafarovceyxun.anamuslim.utils.others

import com.cafarovceyxun.anamuslim.db.entities.user.ReadHistoryEntity
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strLabelContinueReading
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import platform.UIKit.UIApplicationShortcutItem

/**
 * iOS counterpart of Android's `ShortcutUtils.pushLastVersesShortcut`: a "continue reading"
 * Home-screen quick action, published through the shared [IosQuickActions] registry so it coexists
 * with the verse-of-the-day action. The read side is already shared — a tap reconstructs the
 * [ReadHistoryEntity] from the item's `userInfo` and calls [ReaderUiHooks.openReaderFromHistory],
 * the same hook the in-app history list uses.
 *
 * Registration point: [install] from the iOS bootstrap, mirroring `QuranApp.onCreate()`.
 */
object IosReadHistoryShortcuts {

    /** Quick-action type; also the identity so a new entry replaces the previous one. */
    const val TYPE_CONTINUE_READING = "continue_reading"

    private const val KEY_READ_TYPE = "readType"
    private const val KEY_READER_MODE = "readerMode"
    private const val KEY_DIVISION = "divisionNo"
    private const val KEY_CHAPTER = "chapterNo"
    private const val KEY_FROM_VERSE = "fromVerseNo"
    private const val KEY_TO_VERSE = "toVerseNo"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Wires the shared [ReadHistoryShortcuts] sink and the tap handler. Call once at startup. */
    fun install() {
        IosQuickActions.registerHandler(TYPE_CONTINUE_READING) { item -> open(item) }
        ReadHistoryShortcuts.pushLastVerses = { entity -> push(entity) }
    }

    private fun push(entity: ReadHistoryEntity) {
        scope.launch {
            // The label needs the shared resource getter, which is suspend; resolve it here and let
            // IosQuickActions do the main-thread array mutation UIKit requires.
            val title = getString(Res.string.strLabelContinueReading)

            IosQuickActions.setItem(
                UIApplicationShortcutItem(
                    type = TYPE_CONTINUE_READING,
                    localizedTitle = title,
                    localizedSubtitle = null,
                    icon = null,
                    userInfo = mapOf(
                        KEY_READ_TYPE to entity.readType,
                        KEY_READER_MODE to entity.readerMode,
                        KEY_DIVISION to entity.divisionNo.toString(),
                        KEY_CHAPTER to entity.chapterNo.toString(),
                        KEY_FROM_VERSE to entity.fromVerseNo.toString(),
                        KEY_TO_VERSE to entity.toVerseNo.toString(),
                    ),
                ),
            )
        }
    }

    /**
     * On a cold launch the item arrives before the first composition has registered
     * [ReaderUiHooks.openReaderFromHistory]; rather than dropping the tap, this polls briefly for the
     * hook (up to ~5s) and then navigates on the main thread.
     */
    private fun open(shortcutItem: UIApplicationShortcutItem) {
        val entity = decode(shortcutItem.userInfo) ?: return

        scope.launch {
            repeat(50) {
                val open = ReaderUiHooks.openReaderFromHistory
                if (open != null) {
                    withContext(Dispatchers.Main) { open(entity) }
                    return@launch
                }
                delay(100)
            }
            AppLogger.d("IosReadHistoryShortcuts: reader hook never became available, tap dropped")
        }
    }

    private fun decode(userInfo: Map<Any?, *>?): ReadHistoryEntity? {
        val info = userInfo ?: return null
        val readType = info[KEY_READ_TYPE] as? String ?: return null
        val readerMode = info[KEY_READER_MODE] as? String ?: return null

        fun int(key: String): Int = (info[key] as? String)?.toIntOrNull() ?: 0

        return ReadHistoryEntity(
            readType = readType,
            readerMode = readerMode,
            divisionNo = int(KEY_DIVISION),
            chapterNo = int(KEY_CHAPTER),
            fromVerseNo = int(KEY_FROM_VERSE),
            toVerseNo = int(KEY_TO_VERSE),
        )
    }
}
