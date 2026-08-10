package com.cafarovceyxun.anamuslim.activities

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import com.cafarovceyxun.anamuslim.activities.base.BaseActivity
import com.cafarovceyxun.anamuslim.activities.hadith.ActivityHadith
import com.cafarovceyxun.anamuslim.compose.screens.HadithReadHistoryScreen
import com.cafarovceyxun.anamuslim.compose.theme.QuranAppTheme
import kotlinx.coroutines.runBlocking

class ActivityHadithReadHistory : BaseActivity() {
    override fun getLayoutResource() = 0

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        setContent {
            QuranAppTheme {
                HadithReadHistoryScreen(
                    onOpenHistory = { history ->
                        val intent = ActivityHadith.prepareIntent(
                            this,
                            history.volumeSlug,
                            history.bookSlug,
                            history.chapterSlug,
                            history.subChapterSlug,
                            history.title
                        )
                        startActivity(intent)
                    }
                )
            }
        }
    }
}
