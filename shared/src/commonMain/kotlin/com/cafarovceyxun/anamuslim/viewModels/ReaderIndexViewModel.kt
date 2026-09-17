package com.cafarovceyxun.anamuslim.viewModels

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.compose.utils.preferences.DataStoreManager
import com.cafarovceyxun.anamuslim.db.entities.user.QuranReadProgressEntity
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.msgChapterAddedToFavourites
import com.cafarovceyxun.anamuslim.resources.msgChapterRemovedFromFavourites
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.utils.reader.ReaderChapterIndexFilters
import com.cafarovceyxun.anamuslim.utils.reader.ReadType
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
    private val userRepository get() = RepositoryProvider.userRepository

    // ───────── ✓ «oxundu» nişanları və xətm ─────────

    private val readProgress = userRepository.getQuranReadProgressFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Bitmiş düyünlərin açarları (`"chapter:5"`, `"juz:3"` …) — üç siyahı da bundan oxuyur.
     *
     * Açar dəsti saxlanılır, siyahı yox: kart sətri yalnız «mən varam?» sualını verir, `Set`-də bu
     * sabit vaxtdır və 114 + 30 + 60 sətir üçün siyahı üzərində axtarışdan gözlə görünən fərqdir.
     */
    val completedNodes: StateFlow<Set<String>> = readProgress
        .map { rows -> rows.mapTo(HashSet()) { it.nodeKey } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /**
     * Xətm tamamlanıbmı — **yalnız surələr** sayılır.
     *
     * Cüz/hizb nişanları bura girmir: eyni oxunuşu ikinci dəfə saymaq olardı, üstəlik cüz
     * sərhədləri surə sərhədləri ilə üst-üstə düşmür (bax [QuranReadProgressEntity]).
     */
    val isKhatmCompleted: StateFlow<Boolean> = readProgress
        .map { rows ->
            rows.count { it.readType == ReadType.Chapter.value } >= QuranMeta.chapterRange.count()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * Surə nömrəsi → həmin surədə **son qalınan ayə**. Saat nişanı bunu oxuyur.
     *
     * Cüz/hizb girişləri də sayılır: onların da `chapterNo`/`fromVerseNo`-su var və istifadəçi üçün
     * «bu surədə harada qalmışam» sualının cavabı oxumanın hansı bölgü ilə açıldığından asılı
     * deyil. Eyni surəyə bir neçə sətir düşəndə ən təzəsi qalır.
     */
    val lastReadVerseByChapter: StateFlow<Map<Int, Int>> = userRepository
        .getHistoriesFlow(Int.MAX_VALUE)
        .map { rows ->
            rows.asSequence()
                .filter { QuranMeta.isChapterValid(it.chapterNo) && it.fromVerseNo > 0 }
                .groupBy { it.chapterNo }
                .mapValues { (_, group) -> group.maxBy { it.datetime }.fromVerseNo }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Yeni xətmə başlamaq: surə nişanları gedir, cüz/hizb izi qalır. */
    fun restartKhatm() {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.deleteQuranReadProgressOfType(ReadType.Chapter)
        }
    }

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
