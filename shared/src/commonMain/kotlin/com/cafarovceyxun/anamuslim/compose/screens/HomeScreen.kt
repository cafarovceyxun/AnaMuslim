package com.cafarovceyxun.anamuslim.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cafarovceyxun.anamuslim.compose.components.IndexMenuActions
import com.cafarovceyxun.anamuslim.compose.components.LocalIndexMenuActions
import com.cafarovceyxun.anamuslim.compose.components.MainAppBar
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavContentPaddingWithPlayer
import com.cafarovceyxun.anamuslim.compose.components.VerseOfTheDay
import com.cafarovceyxun.anamuslim.compose.components.common.ReadableWidthColumn
import com.cafarovceyxun.anamuslim.compose.components.homepage.AppUpdateBanner
import com.cafarovceyxun.anamuslim.compose.components.homepage.FeatureStoriesRow
import com.cafarovceyxun.anamuslim.compose.components.homepage.HomeActions
import com.cafarovceyxun.anamuslim.compose.components.homepage.HomeSectionBookmarks
import com.cafarovceyxun.anamuslim.compose.components.homepage.HomeSectionGettingStarted
import com.cafarovceyxun.anamuslim.compose.components.homepage.HomeSectionHadithReadHistory
import com.cafarovceyxun.anamuslim.compose.components.homepage.HomeSectionReadHistory
import com.cafarovceyxun.anamuslim.compose.components.homepage.HomeSectionSuggestions
import com.cafarovceyxun.anamuslim.compose.components.homepage.LocalHomeActions
import com.cafarovceyxun.anamuslim.compose.navigation.MainTab
import com.cafarovceyxun.anamuslim.compose.navigation.TabReselectState
import kotlinx.coroutines.launch

/**
 * The homepage. Both action seams are parameters rather than being built here: on Android they are
 * `Intent` launchers over the hosting Activity, on iOS they are `AppNavHost` destinations — the
 * host decides, the screen stays platform-neutral.
 */
@Composable
fun HomeScreen(
    modifier: Modifier,
    homeActions: HomeActions,
    indexMenuActions: IndexMenuActions,
) {
    val scope = rememberCoroutineScope()

    // Hoisted out of the `verticalScroll` call below so re-tapping the Home tab can drive it.
    val scrollState = rememberScrollState()

    TabReselectState.OnTabReselect(MainTab.HOME) {
        scope.launch { scrollState.animateScrollTo(0) }
    }

    CompositionLocalProvider(
        LocalIndexMenuActions provides indexMenuActions,
        LocalHomeActions provides homeActions,
    ) {
        Scaffold(
            topBar = { MainAppBar() },
            containerColor = Color.Transparent,
            modifier = modifier
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(bottom = mainBottomNavContentPaddingWithPlayer()),
            ) {
                // Capped inside the full-width scroll container: every section header carries a
                // trailing "see all" chip, and left to the window width those two ended up ~900dp
                // apart on a 13" iPad. A no-op on phones, where the window is narrower than the cap.
                ReadableWidthColumn {
                    Column {
                        // Ən yuxarıda: əlavə olunmuş funksiyaların hekayə zolağı.
                        FeatureStoriesRow()

                        AppUpdateBanner()

                        VerseOfTheDay()

                        HomeSectionReadHistory()
                        HomeSectionHadithReadHistory()
                        HomeSectionBookmarks()
                        HomeSectionSuggestions()

                        // Last on purpose: it renders only while all of the sections above are
                        // empty, so on a fresh install it is the whole page, and it disappears once
                        // anything is read or saved.
                        HomeSectionGettingStarted()
                    }
                }
            }
        }
    }
}
