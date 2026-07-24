package com.cafarovceyxun.anamuslim.viewModels

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.db.entities.hadith.*
import com.cafarovceyxun.anamuslim.db.entities.user.HadithReadHistoryEntity
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.managers.HadithSyncProvider
import com.cafarovceyxun.anamuslim.utils.managers.ResourceDownloadStatus
import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import com.cafarovceyxun.anamuslim.utils.supabase.*
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/**
 * Platform-neutral reader scroll direction. Android maps hardware keycodes (volume / media /
 * page keys) onto this; other platforms map whatever input they have.
 */
enum class HadithScrollKey { FORWARD, BACKWARD }

@Serializable
sealed class HadithListItem {
    @Serializable
    data class BookHeader(val book: HadithBook) : HadithListItem()
    @Serializable
    data class ChapterHeader(val chapter: HadithChapter) : HadithListItem()
    @Serializable
    data class SubChapterHeader(val subChapter: HadithSubChapter) : HadithListItem()
    @Serializable
    data class ContextGroupedHeader(
        val book: HadithBook? = null,
        val chapter: HadithChapter? = null,
        val subChapter: HadithSubChapter? = null,
    ) : HadithListItem()
    @Serializable
    data class HadithItem(val hadith: Hadith) : HadithListItem()
}

class HadithViewModel : ViewModel() {
    private val hadithDao = RepositoryProvider.hadithDatabase.hadithDao()

    private val _volumes = MutableStateFlow<List<HadithVolume>>(emptyList())
    val volumes: StateFlow<List<HadithVolume>> = _volumes.asStateFlow()

    private val _books = MutableStateFlow<List<HadithBook>>(emptyList())
    val books: StateFlow<List<HadithBook>> = _books.asStateFlow()

    private val _chapters = MutableStateFlow<List<HadithChapter>>(emptyList())
    val chapters: StateFlow<List<HadithChapter>> = _chapters.asStateFlow()

    private val _subChapters = MutableStateFlow<List<HadithSubChapter>>(emptyList())
    val subChapters: StateFlow<List<HadithSubChapter>> = _subChapters.asStateFlow()

    private val _hadiths = MutableStateFlow<List<Hadith>>(emptyList())
    val hadiths: StateFlow<List<Hadith>> = _hadiths.asStateFlow()

    private val _combinedItems = MutableStateFlow<List<HadithListItem>>(emptyList())
    val combinedItems: StateFlow<List<HadithListItem>> = _combinedItems.asStateFlow()

    private val _isLoading = MutableStateFlow(value = false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _syncStatus = MutableStateFlow<ResourceDownloadStatus>(ResourceDownloadStatus.Idle)
    val syncStatus: StateFlow<ResourceDownloadStatus> = _syncStatus.asStateFlow()

    private var booksJob: Job? = null
    private var chaptersJob: Job? = null
    private var subsJob: Job? = null
    private var hadithsJob: Job? = null
    private var fullVolumeJob: Job? = null

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _cachedVolumes = MutableStateFlow<Set<String>>(emptySet())
    val cachedVolumes: StateFlow<Set<String>> = _cachedVolumes.asStateFlow()
    
    private val _scrollEvent = MutableSharedFlow<Int>()

    /**
     * Direction only (1 down / -1 up), never a pixel amount: the step is a share of the viewport
     * ([com.cafarovceyxun.anamuslim.utils.reader.ReaderScrollStep]) and only the collector knows how
     * tall its viewport is.
     */
    val scrollEvent: SharedFlow<Int> = _scrollEvent.asSharedFlow()

    val autoScrollSpeed = mutableStateOf<Float?>(null)
    var isAutoScrollGestureMode = mutableStateOf(false)
    var autoScrollStep = mutableIntStateOf(ReaderPreferences.getAutoScrollStepSync())

    private val _volumeToggleFeedback = MutableStateFlow<Boolean?>(null)
    val volumeToggleFeedback = _volumeToggleFeedback.asStateFlow()

    fun triggerVolumeToggleFeedback(enabled: Boolean) {
        viewModelScope.launch {
            _volumeToggleFeedback.value = enabled
            delay(1500L)
            if (_volumeToggleFeedback.value == enabled) {
                _volumeToggleFeedback.value = null
            }
        }
    }
    var activeVolumeSlug: String? = null
        private set

    var isReadingActive = false

    init {
        HadithSyncProvider.source.initialize()
        
        // Reactive database observation
        viewModelScope.launch(Dispatchers.IO) {
            hadithDao.getAllVolumesFlow().collect { entities ->
                _volumes.value = entities.map { it.toModel() }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            hadithDao.getAllBooksFlow().collect { entities ->
                val cachedSlugs = entities.asSequence().map { it.volume_slug }.toSet()
                _cachedVolumes.value = cachedSlugs
            }
        }

        viewModelScope.launch {
            HadithSyncProvider.source.observeSyncStatus().collect { status ->
                _syncStatus.value = status
            }
        }

        cleanupOldCache()
    }

    /**
     * Scrolls the reader one step. [isVolumeKey] is kept as a parameter because volume-key
     * navigation is opt-in ([AppPreferences.getVolumeKeyNavigationEnabled]); the platform decides
     * which of its keys map to which direction (Android: `HadithViewModel.handleKeyEvent`).
     */
    fun handleScrollKey(key: HadithScrollKey, isVolumeKey: Boolean): Boolean {
        if (!isReadingActive) return false

        if (isVolumeKey && !AppPreferences.getVolumeKeyNavigationEnabled()) return false

        AppLogger.d("HadithKey", "Scroll key: $key (volume=$isVolumeKey)")

        viewModelScope.launch {
            _scrollEvent.emit(if (key == HadithScrollKey.FORWARD) 1 else -1)
        }
        return true
    }

    private fun cleanupOldCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = AppFileSystem.appFilesDir() / "hadith_cache"
            if (AppFileSystem.exists(dir)) {
                AppFileSystem.deleteRecursively(dir)
            }
        }
    }

    fun downloadAllData() {
        HadithSyncProvider.source.startSync()
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            hadithDao.clearAll()
            RepositoryProvider.userRepository.deleteAllHadithHistories()
            
            withContext(Dispatchers.Main) {
                _books.value = emptyList()
                _chapters.value = emptyList()
                _subChapters.value = emptyList()
                _hadiths.value = emptyList()
                _combinedItems.value = emptyList()
            }
        }
    }

    private suspend fun loadVolumeFromDb(volumeSlug: String) {
        withContext(Dispatchers.IO) {
            val books = hadithDao.getBooksByVolume(volumeSlug).map { it.toModel() }
            if (books.isEmpty()) return@withContext

            val bookSlugs = books.map { it.slug }
            val chaptersList = hadithDao.getChaptersByBooks(bookSlugs).map { it.toModel() }
            val allChapterSlugs = chaptersList.map { it.slug }
            
            val subChaptersList = hadithDao.getSubChaptersByChapters(allChapterSlugs).map { it.toModel() }
            val allHadiths = hadithDao.getHadithsByChapters(allChapterSlugs).map { it.toModel() }

            val chaptersByBook = chaptersList.groupBy { it.book_slug }
            val subChaptersByChapter = subChaptersList.groupBy { it.chapter_slug }
            val hadithsByChapter = allHadiths.groupBy { it.chapter_slug }

            val items = mutableListOf<HadithListItem>()
            books.forEach { book ->
                items.add(HadithListItem.BookHeader(book))
                chaptersByBook[book.slug]?.forEach { chapter ->
                    items.add(HadithListItem.ChapterHeader(chapter))
                    
                    val chapterSubs = subChaptersByChapter[chapter.slug] ?: emptyList()
                    val chapterHadiths = hadithsByChapter[chapter.slug] ?: emptyList()

                    if (chapterSubs.isEmpty()) {
                        chapterHadiths.forEach { items.add(HadithListItem.HadithItem(it)) }
                    } else {
                        chapterSubs.forEach { sub ->
                            items.add(HadithListItem.SubChapterHeader(sub))
                            // Filter hadiths belonging to this specific chapter AND sub-chapter
                            chapterHadiths.filter { it.sub_chapter_slug == sub.slug }.forEach { 
                                items.add(HadithListItem.HadithItem(it)) 
                            }
                        }
                        // Handle hadiths that are directly under chapter (no sub-chapter)
                        chapterHadiths.filter { it.sub_chapter_slug == null }.forEach { 
                            items.add(HadithListItem.HadithItem(it)) 
                        }
                    }
                }
            }
            
            withContext(Dispatchers.Main) {
                _combinedItems.value = items
            }
        }
    }

    fun isVolumesCached(): Boolean {
        return _volumes.value.isNotEmpty()
    }


    fun fetchBooks(volumeSlug: String) {
        booksJob?.cancel()
        _books.value = emptyList()
        booksJob = viewModelScope.launch(Dispatchers.IO) {
            val local = hadithDao.getBooksByVolume(volumeSlug).map { it.toModel() }
            _books.value = local
        }
    }

    fun fetchChapters(bookSlug: String) {
        chaptersJob?.cancel()
        _chapters.value = emptyList()
        chaptersJob = viewModelScope.launch(Dispatchers.IO) {
            val local = hadithDao.getChaptersByBook(bookSlug).map { it.toModel() }
            _chapters.value = local
        }
    }

    fun fetchSubChapters(chapterSlug: String) {
        subsJob?.cancel()
        _subChapters.value = emptyList()
        subsJob = viewModelScope.launch(Dispatchers.IO) {
            val local = hadithDao.getSubChaptersByChapter(chapterSlug).map { it.toModel() }
            _subChapters.value = local
        }
    }

    suspend fun hasSubChapters(chapterSlug: String): Boolean {
        val local = hadithDao.getSubChaptersByChapter(chapterSlug)
        return local.isNotEmpty()
    }

    fun fetchHadithsByChapter(chapterSlug: String) {
        AppLogger.d("HadithViewModel", "fetchHadithsByChapter: $chapterSlug")
        hadithsJob?.cancel()
        _hadiths.value = emptyList()
        hadithsJob = viewModelScope.launch(Dispatchers.IO) {
            val local = hadithDao.getHadithsByChapter(chapterSlug).map { it.toModel() }
            AppLogger.d("HadithViewModel", "Fetched ${local.size} hadiths for chapter $chapterSlug")
            _hadiths.value = local
        }
    }

    fun fetchHadithsBySubChapter(chapterSlug: String, subChapterSlug: String) {
        AppLogger.d("HadithViewModel", "fetchHadithsBySubChapter: $subChapterSlug in $chapterSlug")
        hadithsJob?.cancel()
        _hadiths.value = emptyList()
        hadithsJob = viewModelScope.launch(Dispatchers.IO) {
            val local = hadithDao.getHadithsBySubChapter(chapterSlug, subChapterSlug).map { it.toModel() }
            AppLogger.d("HadithViewModel", "Fetched ${local.size} hadiths for sub-chapter $subChapterSlug")
            _hadiths.value = local
        }
    }

    fun saveReadHistory(volumeSlug: String, bookSlug: String?, chapterSlug: String?, subChapterSlug: String?, title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            RepositoryProvider.userRepository.saveHadithReadHistory(
                HadithReadHistoryEntity(
                    volumeSlug = volumeSlug,
                    bookSlug = bookSlug,
                    chapterSlug = chapterSlug,
                    subChapterSlug = subChapterSlug,
                    title = title,
                    datetime = currentEpochMillis()
                )
            )
        }
    }

    fun fetchFullVolume(volumeSlug: String) {
        activeVolumeSlug = volumeSlug
        fullVolumeJob?.cancel()
        fullVolumeJob = viewModelScope.launch(Dispatchers.IO) {
            loadVolumeFromDb(volumeSlug)
        }
    }

    suspend fun getHadithById(id: Long): Hadith? = withContext(Dispatchers.IO) {
        return@withContext hadithDao.getHadithById(id)?.toModel()
    }

    suspend fun getChapterBySlug(slug: String): HadithChapter? = withContext(Dispatchers.IO) {
        return@withContext hadithDao.getChapterBySlug(slug)?.toModel()
    }

    suspend fun getBookBySlug(slug: String): HadithBook? = withContext(Dispatchers.IO) {
        return@withContext hadithDao.getBookBySlug(slug)?.toModel()
    }

    suspend fun getNextNumber(
        type: com.cafarovceyxun.anamuslim.compose.screens.hadith.EditorType, 
        volumeSlug: String?, 
        bookSlug: String?, 
        chapterSlug: String?, 
        subChapterSlug: String?
    ): Int = withContext(Dispatchers.IO) {
        return@withContext when (type) {
            com.cafarovceyxun.anamuslim.compose.screens.hadith.EditorType.VOLUME -> hadithDao.getVolumeCount() + 1
            com.cafarovceyxun.anamuslim.compose.screens.hadith.EditorType.BOOK -> (volumeSlug?.let { hadithDao.getMaxBookNoByVolume(it) } ?: 0) + 1
            com.cafarovceyxun.anamuslim.compose.screens.hadith.EditorType.CHAPTER -> (bookSlug?.let { hadithDao.getMaxChapterNoByBook(it) } ?: 0) + 1
            com.cafarovceyxun.anamuslim.compose.screens.hadith.EditorType.SUB_CHAPTER -> (chapterSlug?.let { hadithDao.getMaxSubChapterNoByChapter(it) } ?: 0) + 1
            com.cafarovceyxun.anamuslim.compose.screens.hadith.EditorType.HADITH -> {
                val max = if (subChapterSlug != null && subChapterSlug != "DIRECT_VIEW") {
                    hadithDao.getMaxHadithNoBySubChapter(chapterSlug!!, subChapterSlug)
                } else {
                    chapterSlug?.let { hadithDao.getMaxHadithNoByChapter(it) }
                }
                (max ?: 0) + 1
            }
        }
    }

    fun upsertVolume(volume: HadithVolume, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                SupabaseProvider.client.from("hadith_volume").upsert(volume)
                hadithDao.insertVolumes(listOf(volume.toEntity()))
                withContext(Dispatchers.Main) { onComplete() }
            } catch (ex: Exception) {
                AppLogger.d("HadithViewModel", "Error upserting volume: ${ex.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun upsertBook(book: HadithBook, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                SupabaseProvider.client.from("hadith_book").upsert(book)
                hadithDao.insertBooks(listOf(book.toEntity()))
                withContext(Dispatchers.Main) { onComplete() }
            } catch (ex: Exception) {
                AppLogger.d("HadithViewModel", "Error upserting book: ${ex.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun upsertChapter(chapter: HadithChapter, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                SupabaseProvider.client.from("hadith_chapter").upsert(chapter)
                hadithDao.insertChapters(listOf(chapter.toEntity()))
                withContext(Dispatchers.Main) { onComplete() }
            } catch (ex: Exception) {
                AppLogger.d("HadithViewModel", "Error upserting chapter: ${ex.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun upsertSubChapter(subChapter: HadithSubChapter, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                SupabaseProvider.client.from("hadith_sub_chapter").upsert(subChapter)
                hadithDao.insertSubChapters(listOf(subChapter.toEntity()))
                withContext(Dispatchers.Main) { onComplete() }
            } catch (ex: Exception) {
                AppLogger.d("HadithViewModel", "Error upserting subChapter: ${ex.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun upsertHadith(hadith: Hadith, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val result = SupabaseProvider.client.from("hadith").upsert(hadith) {
                    select()
                }.decodeSingle<Hadith>()
                hadithDao.insertHadiths(listOf(result.toEntity()!!))
                withContext(Dispatchers.Main) { onComplete() }
            } catch (ex: Exception) {
                AppLogger.d("HadithViewModel", "Error upserting hadith: ${ex.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
