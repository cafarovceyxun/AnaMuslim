package com.cafarovceyxun.anamuslim.compose.utils.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.cafarovceyxun.anamuslim.activities.MainActivity
import com.cafarovceyxun.anamuslim.activities.hadith.ActivityHadith
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.compose.screens.dua.DuaActions
import com.cafarovceyxun.anamuslim.utils.IntentUtils.INTENT_ACTION_OPEN_READER
import com.cafarovceyxun.anamuslim.utils.reader.ReaderIntentData
import com.cafarovceyxun.anamuslim.utils.reader.ReaderLaunchParams
import com.cafarovceyxun.anamuslim.utils.reader.toIntent

/**
 * Android wiring for the shared [DuaActions] seam: the "open the hadith" / "open in the reader"
 * buttons in the dua source sheet, as Activity intents.
 *
 * Provided once at the top of `MainScreen`, not per screen: the dua and Əsmaül Hüsnə screens open
 * as full-screen dialogs from the homepage, so anything under the main composition may show that
 * sheet. Unprovided the buttons are hidden rather than inert — see [DuaActions].
 */
@Composable
fun rememberDuaActions(): DuaActions {
    val context = LocalContext.current

    return remember(context) {
        DuaActions(
            onOpenHadith = { hadithId ->
                context.startActivity(
                    ActivityHadith.prepareIntent(
                        context = context,
                        volumeSlug = null,
                        bookSlug = null,
                        chapterSlug = null,
                        subChapterSlug = null,
                        title = null,
                        hadithId = hadithId,
                    ),
                )
            },
            onOpenVerse = { chapterNo, verseNo ->
                ReaderLaunchParams(
                    data = ReaderIntentData.FullChapter(
                        chapterNo,
                        ChapterVersePair(chapterNo, verseNo),
                    ),
                ).toIntent().apply {
                    setClass(context, MainActivity::class.java)
                    action = INTENT_ACTION_OPEN_READER
                }.let(context::startActivity)
            },
        )
    }
}
