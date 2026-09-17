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
import kotlinx.coroutines.flow.map
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

    /**
     * ✓ nişanı qalıbmı — «hamısını sil» düyməsinin görünməsi buna da baxır.
     *
     * Siyahı boş ola-ola nişanlar dolu qala bilər (tarixçə 40 sətirdə kəsilir), o halda da
     * nişanları təmizləmək üçün bir yol qalmalıdır.
     */
    val hasReadProgress = userRepository.getQuranReadProgressFlow()
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    suspend fun deleteHistory(id: Long) {
        userRepository.deleteHistory(id)
    }

    /**
     * Oxuma izi iki cədvəldədir və istifadəçi ikisini ayrıca silə bilir (bax ekrandakı seçim
     * dialoqu): siyahının özü `read_history`, ✓ «oxundu» nişanları isə `quran_read_progress`.
     *
     * Tək sətrin silinməsi ([deleteHistory]) nişana toxunmur: orada istifadəçi siyahıdan bir
     * yazını götürür, oxuduğunu inkar etmir.
     */
    suspend fun deleteAllHistories() {
        userRepository.deleteAllHistories()
    }

    /** Yalnız ✓ «oxundu» nişanları — siyahı olduğu kimi qalır. */
    suspend fun deleteAllCompletion() {
        userRepository.deleteAllQuranReadProgress()
    }

    /** Hər ikisi: siyahı və ✓ nişanları. */
    suspend fun deleteEverything() {
        userRepository.deleteAllHistories()
        userRepository.deleteAllQuranReadProgress()
    }
}
