package com.cafarovceyxun.anamuslim.activities.hadith

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.activity.viewModels
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.cafarovceyxun.anamuslim.activities.base.BaseActivity
import com.cafarovceyxun.anamuslim.compose.screens.hadith.HadithIndexScreen
import com.cafarovceyxun.anamuslim.compose.theme.QuranAppTheme
import com.cafarovceyxun.anamuslim.viewModels.HadithViewModel
import com.cafarovceyxun.anamuslim.viewModels.handleKeyEvent

import androidx.navigation.compose.rememberNavController

class ActivityHadith : BaseActivity() {
    private val viewModel: HadithViewModel by viewModels()

    override fun getLayoutResource(): Int = 0

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (viewModel.handleKeyEvent(keyCode)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        setContentView(ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val volumeSlug = intent.getStringExtra("volume_slug")
                val bookSlug = intent.getStringExtra("book_slug")
                val chapterSlug = intent.getStringExtra("chapter_slug")
                val subChapterSlug = intent.getStringExtra("sub_chapter_slug")
                val title = intent.getStringExtra("title")
                val hadithId = intent.getLongExtra("hadith_id", -1L)

                QuranAppTheme {
                    androidx.compose.runtime.CompositionLocalProvider(
                        // This Activity, matching the `by viewModels()` store used for key events —
                        // the screen resolved the same instance via `viewModel<..>(activity)` before.
                        com.cafarovceyxun.anamuslim.compose.utils.LocalAppViewModelStoreOwner provides this@ActivityHadith,
                        com.cafarovceyxun.anamuslim.compose.screens.hadith.LocalHadithActions provides
                            com.cafarovceyxun.anamuslim.compose.utils.app.rememberHadithActions(),
                    ) {
                    HadithIndexScreen(
                        initialVolumeSlug = volumeSlug,
                        initialBookSlug = bookSlug,
                        initialChapterSlug = chapterSlug,
                        initialSubChapterSlug = subChapterSlug,
                        initialTitle = title,
                        initialHadithId = if (hadithId != -1L) hadithId else null
                    )
                    }
                }
            }
        })
    }

    companion object {
        fun prepareIntent(
            context: Context,
            volumeSlug: String?,
            bookSlug: String?,
            chapterSlug: String?,
            subChapterSlug: String?,
            title: String?,
            hadithId: Long? = null
        ): Intent {
            return Intent(context, ActivityHadith::class.java).apply {
                putExtra("volume_slug", volumeSlug)
                putExtra("book_slug", bookSlug)
                putExtra("chapter_slug", chapterSlug)
                putExtra("sub_chapter_slug", subChapterSlug)
                putExtra("title", title)
                if (hadithId != null) putExtra("hadith_id", hadithId)
            }
        }
    }
}
