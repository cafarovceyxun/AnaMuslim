package com.cafarovceyxun.anamuslim.activities

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import com.cafarovceyxun.anamuslim.activities.base.BaseActivity
import com.cafarovceyxun.anamuslim.activities.hadith.ActivityHadith
import com.cafarovceyxun.anamuslim.compose.screens.BookmarksScreen
import com.cafarovceyxun.anamuslim.compose.theme.QuranAppTheme

import com.cafarovceyxun.anamuslim.utils.reader.factory.ReaderFactory

class ActivityBookmark : BaseActivity() {
    override fun getLayoutResource() = 0

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        setContent {
            QuranAppTheme {
                BookmarksScreen(
                    onOpenInReader = { chapterNo, fromVerse, toVerse ->
                        ReaderFactory.startVerseRange(this, chapterNo, fromVerse, toVerse)
                    },
                    onOpenHadith = { title, chapterSlug, subChapterSlug ->
                        startActivity(
                            ActivityHadith.prepareIntent(
                                this,
                                volumeSlug = null,
                                bookSlug = null,
                                chapterSlug = chapterSlug,
                                subChapterSlug = subChapterSlug,
                                title = title,
                            ),
                        )
                    },
                )
            }
        }
    }
}
