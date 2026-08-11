package com.cafarovceyxun.anamuslim.viewModels

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderToggleFeedback
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderToggleKind
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strMsgEditQueuedForReview
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.db.entities.hadith.*
import com.cafarovceyxun.anamuslim.db.entities.user.HadithReadHistoryEntity
import com.cafarovceyxun.anamuslim.db.relations.HadithChildCount
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
import kotlinx.serialization.json.JsonObject
import org.jetbrains.compose.resources.getString

/**
 * Platform-neutral reader scroll direction. Android maps hardware keycodes (volume / media /
 * page keys) onto this; other platforms map whatever input they have.
 */
enum class HadithScrollKey { FORWARD, BACKWARD }

private fun List<HadithChildCount>.toCountMap(): Map<String, Int> =
    associate { it.parentSlug to it.childCount }

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

    // "How much is inside" counters for the index cards, keyed by the parent's slug. A parent that
    // holds nothing is simply absent from the map. `chapterHadithCounts` covers the babs that carry
    // hadiths directly — those have no sub-bab count to show, so the card falls back to it.
    private val _volumeBookCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val volumeBookCounts: StateFlow<Map<String, Int>> = _volumeBookCounts.asStateFlow()

    private val _bookChapterCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val bookChapterCounts: StateFlow<Map<String, Int>> = _bookChapterCounts.asStateFlow()

    private val _chapterSubChapterCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val chapterSubChapterCounts: StateFlow<Map<String, Int>> = _chapterSubChapterCounts.asStateFlow()

    private val _chapterHadithCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val chapterHadithCounts: StateFlow<Map<String, Int>> = _chapterHadithCounts.asStateFlow()

    private val _subChapterHadithCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val subChapterHadithCounts: StateFlow<Map<String, Int>> = _subChapterHadithCounts.asStateFlow()

    private val _isLoading = MutableStateFlow(value = false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _syncStatus = MutableStateFlow<ResourceDownloadStatus>(ResourceDownloadStatus.Idle)
    val syncStatus: StateFlow<ResourceDownloadStatus> = _syncStatus.asStateFlow()

    private var booksJob: Job? = null
    private var chaptersJob: Job? = null
    private var subsJob: Job? = null
    private var hadithsJob: Job? = null
    private var fullVolumeJob: Job? = null

    // Hansı açar üçün siyahının artıq yükləndiyi. `fetch*` siyahını yalnız açar dəyişəndə boşaldır:
    // hədis oxuma ekranından geri qayıdanda `LaunchedEffect(slug)` eyni açarla yenidən işə düşür, və
    // siyahını boşaltmaq LazyGrid-i sıfır elementlə ölçdürüb sürüşmə mövqeyini 0-a sıxışdırırdı —
    // yəni istifadəçi hər dəfə siyahının ən başına atılırdı.
    private var loadedBooksKey: String? = null
    private var loadedChaptersKey: String? = null
    private var loadedSubsKey: String? = null

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

    private val _toggleFeedback = MutableStateFlow<ReaderToggleFeedback?>(null)
    val toggleFeedback = _toggleFeedback.asStateFlow()

    fun triggerToggleFeedback(kind: ReaderToggleKind, enabled: Boolean) {
        viewModelScope.launch {
            val feedback = ReaderToggleFeedback(kind, enabled)
            _toggleFeedback.value = feedback
            delay(1500L)
            // Only clear our own message — a newer toggle may have replaced it while we waited.
            if (_toggleFeedback.value == feedback) {
                _toggleFeedback.value = null
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
                // The volume cards' book counter rides along on this flow rather than on its own
                // grouped query — the rows are already here, so grouping them is free.
                _volumeBookCounts.value = entities.groupingBy { it.volume_slug }.eachCount()
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
                loadedBooksKey = null
                loadedChaptersKey = null
                loadedSubsKey = null
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
        if (loadedBooksKey != volumeSlug) {
            _books.value = emptyList()
            loadedBooksKey = volumeSlug
        }
        booksJob = viewModelScope.launch(Dispatchers.IO) {
            val local = hadithDao.getBooksByVolume(volumeSlug).map { it.toModel() }
            _books.value = local
            _bookChapterCounts.value =
                hadithDao.getChapterCountsByBooks(local.map { it.slug }).toCountMap()
        }
    }

    fun fetchChapters(bookSlug: String) {
        chaptersJob?.cancel()
        if (loadedChaptersKey != bookSlug) {
            _chapters.value = emptyList()
            loadedChaptersKey = bookSlug
        }
        chaptersJob = viewModelScope.launch(Dispatchers.IO) {
            val local = hadithDao.getChaptersByBook(bookSlug).map { it.toModel() }
            _chapters.value = local

            // Both counters are needed up front: a bab shows its sub-bab count, or — when it has no
            // sub-babs — how many hadiths sit directly under it.
            val chapterSlugs = local.map { it.slug }
            _chapterSubChapterCounts.value = hadithDao.getSubChapterCountsByChapters(chapterSlugs).toCountMap()
            _chapterHadithCounts.value = hadithDao.getHadithCountsByChapters(chapterSlugs).toCountMap()
        }
    }

    fun fetchSubChapters(chapterSlug: String) {
        subsJob?.cancel()
        if (loadedSubsKey != chapterSlug) {
            _subChapters.value = emptyList()
            loadedSubsKey = chapterSlug
        }
        subsJob = viewModelScope.launch(Dispatchers.IO) {
            val local = hadithDao.getSubChaptersByChapter(chapterSlug).map { it.toModel() }
            _subChapters.value = local
            _subChapterHadithCounts.value =
                hadithDao.getHadithCountsBySubChapters(local.map { it.slug }).toCountMap()
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
                // İki nəticə mümkündür: admin üçün trigger sətri birbaşa `hadith`-ə yazır və
                // `select()` onu qaytarır; redaktor üçün isə trigger əsas cədvələ yazmanı ləğv edib
                // təklifi `hadith_edits`-ə salır, yəni cavab BOŞ gəlir. `decodeSingle` orada istisna
                // atırdı → redaktorun düzəlişi növbəyə düşdüyü halda belə forma bağlanmır və heç bir
                // bildiriş çıxmırdı.
                val saved = SupabaseProvider.client.from("hadith").upsert(hadith) {
                    select()
                }.decodeSingleOrNull<Hadith>()

                if (saved != null) {
                    hadithDao.insertHadiths(listOf(saved.toEntity()!!))
                } else {
                    // Moderasiyaya düşdü — əsas məzmun hələ dəyişmədiyi üçün lokal bazaya toxunmuruq.
                    PlatformUtils.showLongToast(getString(Res.string.strMsgEditQueuedForReview))
                }
                withContext(Dispatchers.Main) { onComplete() }
            } catch (ex: Exception) {
                AppLogger.d("HadithViewModel", "Error upserting hadith: ${ex.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Siləndə hesabatlıq lazımdır: RLS bir əməliyyatı bloklayanda PostgREST xəta yox, **boş nəticə**
     * qaytarır, yəni "uğurlu" görünən heç-nə. Ona görə hər silmə `select()` ilə gedir və qayıdan
     * sətir sayına baxılır — [DeleteOutcome] həmin fərqi UI-yə çatdırır.
     */
    sealed interface DeleteOutcome {
        /** Sətir həqiqətən silindi. */
        data object Deleted : DeleteOutcome

        /** Trigger silməni ləğv edib tələbi `hadith_edits`-ə saldı — admin təsdiqləyəndə silinəcək. */
        data object QueuedForReview : DeleteOutcome

        /** RLS bloklayıb: struktur cədvəllərini yalnız admin silə bilir. */
        data object NotAllowed : DeleteOutcome

        /** İçində [count] element var. `chapter_slug` xarici açar olmadığı üçün silsək yetim qalardılar. */
        data class NotEmpty(val count: Int) : DeleteOutcome

        data object Failed : DeleteOutcome
    }

    fun deleteHadith(hadith: Hadith, onResult: (DeleteOutcome) -> Unit) {
        val id = hadith.id
        if (id == null) {
            onResult(DeleteOutcome.Failed)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // `upsertHadith`-dəki eyni ikiyol: admin üçün silinən sətir geri gəlir, redaktor
                // üçün `trg_intercept_hadith_delete` silməni ləğv edib tələbi növbəyə saldığı üçün
                // cavab boş gəlir.
                val removed = SupabaseProvider.client.from("hadith").delete {
                    select()
                    filter { eq("id", id) }
                }.decodeList<JsonObject>().size

                val outcome = if (removed > 0) {
                    hadithDao.deleteHadithById(id)
                    DeleteOutcome.Deleted
                } else {
                    DeleteOutcome.QueuedForReview
                }
                withContext(Dispatchers.Main) { onResult(outcome) }
            } catch (ex: Exception) {
                AppLogger.d("HadithViewModel", "Error deleting hadith: ${ex.message}")
                withContext(Dispatchers.Main) { onResult(DeleteOutcome.Failed) }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteVolume(slug: String, onResult: (DeleteOutcome) -> Unit) = deleteStructure(
        table = "hadith_volume",
        slug = slug,
        childCount = { hadithDao.countBooksInVolume(slug) },
        removeLocally = { hadithDao.deleteVolumeBySlug(slug) },
        onResult = onResult,
    )

    fun deleteBook(slug: String, onResult: (DeleteOutcome) -> Unit) = deleteStructure(
        table = "hadith_book",
        slug = slug,
        childCount = { hadithDao.countChaptersInBook(slug) },
        removeLocally = { hadithDao.deleteBookBySlug(slug) },
        onResult = onResult,
    )

    fun deleteChapter(slug: String, onResult: (DeleteOutcome) -> Unit) = deleteStructure(
        table = "hadith_chapter",
        slug = slug,
        // Alt-bablar bazada CASCADE ilə gedir, hədislər isə YOX — ikisini də sayırıq.
        childCount = { hadithDao.countSubChaptersInChapter(slug) + hadithDao.countHadithsInChapter(slug) },
        removeLocally = { hadithDao.deleteChapterBySlug(slug) },
        onResult = onResult,
    )

    fun deleteSubChapter(slug: String, onResult: (DeleteOutcome) -> Unit) = deleteStructure(
        table = "hadith_sub_chapter",
        slug = slug,
        childCount = { hadithDao.countHadithsInSubChapter(slug) },
        removeLocally = { hadithDao.deleteSubChapterBySlug(slug) },
        onResult = onResult,
    )

    /** Struktur sətrini silir — boş deyilsə toxunmur, RLS bloklayarsa bunu ayrıca bildirir. */
    private fun deleteStructure(
        table: String,
        slug: String,
        childCount: suspend () -> Int,
        removeLocally: suspend () -> Unit,
        onResult: (DeleteOutcome) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val inside = childCount()
                if (inside > 0) {
                    withContext(Dispatchers.Main) { onResult(DeleteOutcome.NotEmpty(inside)) }
                    return@launch
                }

                val removed = SupabaseProvider.client.from(table).delete {
                    select()
                    filter { eq("slug", slug) }
                }.decodeList<JsonObject>().size

                val outcome = if (removed > 0) {
                    removeLocally()
                    DeleteOutcome.Deleted
                } else {
                    DeleteOutcome.NotAllowed
                }
                withContext(Dispatchers.Main) { onResult(outcome) }
            } catch (ex: Exception) {
                AppLogger.d("HadithViewModel", "Error deleting $table: ${ex.message}")
                withContext(Dispatchers.Main) { onResult(DeleteOutcome.Failed) }
            } finally {
                _isLoading.value = false
            }
        }
    }
}
