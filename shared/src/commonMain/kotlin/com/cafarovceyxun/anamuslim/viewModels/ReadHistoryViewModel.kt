package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.cafarovceyxun.anamuslim.compose.utils.appLocaleFlow
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.repository.getHistoriesPaginated
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class ReadHistoryViewModel : ViewModel() {
    private val userRepository get() = RepositoryProvider.userRepository
    private val quranRepository get() = RepositoryProvider.quranRepository

    val chapterNames = appLocaleFlow.mapLatest {
        quranRepository.getChapterNames(QuranMeta.chapterRange.toList())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        emptyMap()
    )

    val allHistories = userRepository.getHistoriesPaginated()
        .cachedIn(viewModelScope)

    val recentHistories = userRepository.getHistoriesFlow(10)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

    suspend fun deleteHistory(id: Long) {
        userRepository.deleteHistory(id)
    }

    suspend fun deleteAllHistories() {
        userRepository.deleteAllHistories()
    }
}
