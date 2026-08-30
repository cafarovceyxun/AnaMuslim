package com.cafarovceyxun.anamuslim.compose.utils.app

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.cafarovceyxun.anamuslim.activities.ActivityBookmark
import com.cafarovceyxun.anamuslim.activities.ActivityHadithReadHistory
import com.cafarovceyxun.anamuslim.activities.hadith.ActivityHadith
import com.cafarovceyxun.anamuslim.activities.ActivityReadHistory
import com.cafarovceyxun.anamuslim.activities.ActivitySettings
import com.cafarovceyxun.anamuslim.activities.MainActivity
import com.cafarovceyxun.anamuslim.compose.components.homepage.HomeActions
import com.cafarovceyxun.anamuslim.compose.navigation.SettingRoutes
import com.cafarovceyxun.anamuslim.utils.univ.Keys
import com.cafarovceyxun.anamuslim.utils.IntentUtils.INTENT_ACTION_OPEN_READER
import com.cafarovceyxun.anamuslim.utils.reader.factory.ReaderFactory

/** Android wiring for the shared [HomeActions] seam: Activity intents + the hadith nav route. */
@Composable
fun rememberHomeActions(navController: NavController): HomeActions {
    val context = LocalContext.current
    return remember(context, navController) {
        HomeActions(
            onOpenHadithReadHistory = {
                context.startActivity(Intent(context, ActivityHadithReadHistory::class.java))
            },
            onOpenHadithItem = { volume, book, chapter, sub, title ->
                navController.navigate(
                    "hadith_items/$volume/${book ?: "null"}/${chapter ?: "null"}/${sub ?: "null"}/$title",
                )
            },
            onOpenHadithById = { hadithId ->
                context.startActivity(
                    ActivityHadith.prepareIntent(
                        context,
                        volumeSlug = null,
                        bookSlug = null,
                        chapterSlug = null,
                        subChapterSlug = null,
                        title = null,
                        hadithId = hadithId,
                    ),
                )
            },
            onOpenReadHistory = {
                context.startActivity(Intent(context, ActivityReadHistory::class.java))
            },
            onOpenBookmarks = {
                context.startActivity(Intent(context, ActivityBookmark::class.java))
            },
            onOpenSuggestions = {
                val intent = Intent(context, ActivitySettings::class.java)
                intent.putExtra(Keys.NAV_DESTINATION, SettingRoutes.SUGGESTIONS)
                context.startActivity(intent)
            },
            onOpenReaderFromHistory = { history ->
                ReaderFactory.prepareHistoryIntent(history)?.let { intent ->
                    intent.setClass(context, MainActivity::class.java)
                    intent.action = INTENT_ACTION_OPEN_READER
                    context.startActivity(intent)
                }
            },
        )
    }
}
