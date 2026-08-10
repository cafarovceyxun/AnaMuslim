package com.cafarovceyxun.anamuslim.compose.components.reader.navigator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderLayoutItem
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderMode
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strTitleReaderChapters
import com.cafarovceyxun.anamuslim.resources.strTitleReaderJuz
import com.cafarovceyxun.anamuslim.resources.strTitleReaderHizb
import com.cafarovceyxun.anamuslim.resources.strTitleChapInfoPages
import com.cafarovceyxun.anamuslim.utils.reader.ReaderIntentData
import com.cafarovceyxun.anamuslim.utils.reader.ReaderLaunchParams
import com.cafarovceyxun.anamuslim.viewModels.ReaderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

private enum class NavTab { Chapter, Juz, Hizb, Page }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderNavigator(
    readerVm: ReaderViewModel,
    isInModal: Boolean,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val readerMode by readerVm.readerMode.collectAsState()

    val tabs = remember(readerMode) {
        if (readerMode == ReaderMode.VerseByVerse) NavTab.entries.filter { it != NavTab.Page }
        else NavTab.entries
    }

    var selectedTabIndex by readerVm.selectedNavigationTabIndex

    if (selectedTabIndex >= tabs.size) {
        selectedTabIndex = 0
    }

    val selectedTab = tabs[selectedTabIndex]

    fun navigateChapter(chapterNo: Int) {
        scope.launch {
            readerVm.initReader(
                ReaderLaunchParams(
                    data = ReaderIntentData.FullChapter(chapterNo),
                    readerMode = ReaderMode.VerseByVerse
                )
            )
            onClose()
        }
    }

    fun navigateVerse(chapterNo: Int, verseNo: Int) {
        scope.launch {
            val isInCurrentView = readerVm.verseByVersePrepared.value.items.any { item ->
                item is ReaderLayoutItem.VerseUI &&
                        item.verse.chapterNo == chapterNo &&
                        item.verse.verseNo == verseNo
            }

            if (isInCurrentView && readerMode == ReaderMode.VerseByVerse) {
                readerVm.requestVerseNavigation(chapterNo, verseNo)
            } else {
                readerVm.initReader(
                    ReaderLaunchParams(
                        data = ReaderIntentData.FullChapter(
                            chapterNo,
                            ChapterVersePair(chapterNo, verseNo)
                        ),
                        readerMode = ReaderMode.VerseByVerse
                    )
                )
            }

            onClose()
        }
    }

    fun navigateJuz(juzNo: Int) {
        scope.launch {
            readerVm.initReader(
                ReaderLaunchParams(
                    data = ReaderIntentData.FullJuz(juzNo),
                    readerMode = ReaderMode.VerseByVerse
                )
            )
            onClose()
        }
    }

    fun navigateHizb(hizbNo: Int) {
        scope.launch {
            readerVm.initReader(
                ReaderLaunchParams(
                    data = ReaderIntentData.FullHizb(hizbNo),
                    readerMode = ReaderMode.VerseByVerse
                )
            )
            onClose()
        }
    }

    fun navigatePage(pageNo: Int) {
        scope.launch {
            readerVm.requestPageNavigation(pageNo)
            onClose()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(if (isInModal) 0.9f else 1f)
            .then(if (!isInModal) Modifier.background(colorScheme.surfaceContainer) else Modifier)
    ) {
        SecondaryScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = colorScheme.surface,
            contentColor = colorScheme.primary,
            edgePadding = 0.dp,
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            when (tab) {
                                NavTab.Chapter -> stringResource(Res.string.strTitleReaderChapters)
                                NavTab.Juz -> stringResource(Res.string.strTitleReaderJuz)
                                NavTab.Hizb -> stringResource(Res.string.strTitleReaderHizb)
                                NavTab.Page -> stringResource(Res.string.strTitleChapInfoPages)
                            },
                            style = typography.labelLarge
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            NavTab.Chapter -> ChapterNavigatorList(
                readerVm = readerVm,
                onChapterSelected = ::navigateChapter,
                onVerseSelected = ::navigateVerse,
            )

            NavTab.Juz -> JuzNavigationList(
                readerVm,
                onJuzSelected = ::navigateJuz,
                onVerseSelected = ::navigateVerse,
            )

            NavTab.Hizb -> HizbNavigationList(
                readerVm,
                onHizbSelected = ::navigateHizb,
                onVerseSelected = ::navigateVerse,
            )

            NavTab.Page -> PageNavigationList(
                readerVm,
                onPageSelected = ::navigatePage,
            )
        }
    }
}

