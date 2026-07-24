package com.cafarovceyxun.anamuslim.compose.components.search

import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.noResults
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavigationOuterHeight
import com.cafarovceyxun.anamuslim.compose.components.reader.navigator.ChapterCard
import com.cafarovceyxun.anamuslim.db.relations.SurahWithLocalizations
import com.cafarovceyxun.anamuslim.viewModels.QuranSearchViewModel

@Composable
fun SurahSearchResults(
    viewModel: QuranSearchViewModel,
    surahResults: List<SurahWithLocalizations>?,
) {
    if (surahResults == null) {
        return Loader(true)
    }

    if (surahResults.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(Res.string.noResults),
                style = typography.labelLarge,
            )
        }
        return
    }


    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = mainBottomNavigationOuterHeight() + 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(surahResults) {
            ChapterCard(
                surah = it,
                onClick = {
                    viewModel.recordCurrentSearchQuery()
                    ReaderUiHooks.openChapter?.invoke(it.surah.surahNo)
                },
            )
        }
    }
}
