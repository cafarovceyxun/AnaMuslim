package com.cafarovceyxun.anamuslim.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.IndexMenuActions
import com.cafarovceyxun.anamuslim.compose.components.LocalIndexMenuActions
import com.cafarovceyxun.anamuslim.compose.components.MainAppBar
import com.cafarovceyxun.anamuslim.compose.components.VerseOfTheDay
import com.cafarovceyxun.anamuslim.compose.components.homepage.AppUpdateBanner
import com.cafarovceyxun.anamuslim.compose.components.homepage.HomeActions
import com.cafarovceyxun.anamuslim.compose.components.homepage.HomeSectionBookmarks
import com.cafarovceyxun.anamuslim.compose.components.homepage.HomeSectionHadithReadHistory
import com.cafarovceyxun.anamuslim.compose.components.homepage.HomeSectionReadHistory
import com.cafarovceyxun.anamuslim.compose.components.homepage.LocalHomeActions
import com.cafarovceyxun.anamuslim.viewModels.DailyContentViewModel

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
    val dailyVm = viewModel { DailyContentViewModel() }

    LaunchedEffect(Unit) {
        dailyVm.fetchTodayContent()
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
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 120.dp),
            ) {
                AppUpdateBanner()

                VerseOfTheDay()

                HomeSectionReadHistory()
                HomeSectionHadithReadHistory()
                HomeSectionBookmarks()
            }
        }
    }
}
