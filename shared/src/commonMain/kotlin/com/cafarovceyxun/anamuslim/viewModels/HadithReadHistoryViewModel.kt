package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.repository.getHadithHistoriesPaginated
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HadithReadHistoryViewModel : ViewModel() {
    private val repository get() = RepositoryProvider.userRepository

    val allHistories = repository.getHadithHistoriesPaginated()
        .cachedIn(viewModelScope)

    /**
     * ✓ nişanı qalıbmı — «hamısını sil» düyməsinin görünməsi buna da baxır.
     *
     * Siyahı boş ola-ola nişanlar dolu qala bilər: tarixçə kitab başına kəsilir, köhnə build isə
     * siyahını nişanlara toxunmadan silirdi. Düymə yalnız siyahıya baxsaydı, belə cihazda
     * nişanları təmizləmək üçün heç bir yol qalmazdı.
     */
    val hasReadProgress = repository.getHadithReadProgressFlow()
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    suspend fun deleteHistory(id: Long) {
        repository.deleteHadithHistory(id)
    }

    /**
     * Oxuma izi **iki** cədvəldədir və istifadəçi ikisini ayrıca silə bilir (bax ekrandakı
     * seçim dialoqu): siyahının özü `hadith_read_history`, ✓ «oxundu» nişanları isə
     * `hadith_read_progress`.
     *
     * Əvvəl nişanları silən yeganə yol bütün hədis məzmununu silmək idi — tarixçəni təmizləyəndən
     * sonra bablar hələ də ✓ ilə qalırdı və «oxuma tarixi sıfırlanmır» kimi görünürdü.
     *
     * Tək sətrin silinməsi ([deleteHistory]) nişana toxunmur: orada istifadəçi siyahıdan bir
     * yazını götürür, oxuduğunu inkar etmir.
     */
    suspend fun deleteAllHistories() {
        repository.deleteAllHadithHistories()
    }

    /** Yalnız ✓ «oxundu» nişanları — siyahı olduğu kimi qalır. */
    suspend fun deleteAllCompletion() {
        repository.deleteAllHadithReadProgress()
    }

    /** Hər ikisi: siyahı və ✓ nişanları. */
    suspend fun deleteEverything() {
        repository.deleteAllHadithHistories()
        repository.deleteAllHadithReadProgress()
    }
}
