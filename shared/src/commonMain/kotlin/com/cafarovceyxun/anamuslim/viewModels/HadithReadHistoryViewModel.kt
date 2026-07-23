package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.repository.getHadithHistoriesPaginated

class HadithReadHistoryViewModel : ViewModel() {
    private val repository get() = RepositoryProvider.userRepository

    val allHistories = repository.getHadithHistoriesPaginated()
        .cachedIn(viewModelScope)

    suspend fun deleteHistory(id: Long) {
        repository.deleteHadithHistory(id)
    }

    suspend fun deleteAllHistories() {
        repository.deleteAllHadithHistories()
    }
}
