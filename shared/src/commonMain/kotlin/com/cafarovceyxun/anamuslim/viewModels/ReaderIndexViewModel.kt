package com.cafarovceyxun.anamuslim.viewModels

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.compose.utils.preferences.DataStoreManager
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.msgChapterAddedToFavourites
import com.cafarovceyxun.anamuslim.resources.msgChapterRemovedFromFavourites
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.reader.ReaderChapterIndexFilters
import com.cafarovceyxun.anamuslim.utils.univ.Keys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString

class ReaderIndexViewModel : ViewModel() {
    val repository get() = RepositoryProvider.quranRepository

    private val filtersJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val chapterFiltersKey = stringPreferencesKey("reader_index_chapter_filters")
    private val chapterFiltersDefaultJson =
        filtersJson.encodeToString(ReaderChapterIndexFilters.Default)

    val chapterIndexFilters: StateFlow<ReaderChapterIndexFilters> = DataStoreManager
        .flow(chapterFiltersKey, chapterFiltersDefaultJson)
        .map { raw ->
            try {
                filtersJson.decodeFromString<ReaderChapterIndexFilters>(raw)
            } catch (e: Exception) {
                AppLogger.saveError(e, "chapterIndexFilters")
                ReaderChapterIndexFilters.Default
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReaderChapterIndexFilters.Default
        )

    private val _surahNosWithSajdah = MutableStateFlow<Set<Int>>(emptySet())
    val surahNosWithSajdah: StateFlow<Set<Int>> = _surahNosWithSajdah.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _surahNosWithSajdah.value = repository.getSurahNosWithSajdah()
            } catch (e: Exception) {
                AppLogger.saveError(e, "surahNosWithSajdah")
            }
        }
    }

    fun setChapterIndexFilters(filters: ReaderChapterIndexFilters) {
        viewModelScope.launch(Dispatchers.IO) {
            DataStoreManager.write(
                chapterFiltersKey,
                filtersJson.encodeToString(filters)
            )
        }
    }

    val surahs = repository.getAllSurahs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val juzs = repository.getJuzs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val hizbs = repository.getHizbs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )


    @Composable
    fun getFavouriteChapters(): List<Int> {
        val raw = DataStoreManager.observe(
            KEY,
            Json.encodeToString(emptyList<Int>())
        )

        return try {
            Json.decodeFromString<List<Int>>(raw)
        } catch (e: Exception) {
            AppLogger.saveError(
                e,
                "getFavouriteChapters",
            )
            emptyList()
        }
    }

    suspend fun addToFavourites(chapterNo: Int, curr: List<Int>) {
        DataStoreManager.write(
            KEY,
            Json.encodeToString(curr.toMutableList().apply { add(0, chapterNo) })
        )

        PlatformUtils.showToast(getString(Res.string.msgChapterAddedToFavourites))
    }

    suspend fun removeFromFavourites(chapterNo: Int, curr: List<Int>) {
        DataStoreManager.write(
            KEY,
            Json.encodeToString(curr - chapterNo)
        )

        PlatformUtils.showToast(getString(Res.string.msgChapterRemovedFromFavourites))
    }

    companion object {
        internal val KEY = stringPreferencesKey(Keys.FAVOURITE_CHAPTERS)
    }
}
