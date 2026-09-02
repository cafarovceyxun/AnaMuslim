package com.cafarovceyxun.anamuslim.viewModels

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderToggleFeedback
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderToggleKind
import com.cafarovceyxun.anamuslim.compose.screens.hadith.BulkRow
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strMsgEditQueuedForReview
import com.cafarovceyxun.anamuslim.resources.strMsgHadithsSaveFailed
import com.cafarovceyxun.anamuslim.resources.strMsgHadithsSaved
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

/** Qonşu bab keşinin tutumu: cari + hər tərəfə bir neçə. */
private const val HADITH_CACHE_MAX = 6

/**
 * Hədis məzmununun **proses boyu** versiyası — hər uğurlu yazma/silmə (və tamamlanan sinxron) sonra artır.
 *
 * Niyə instansiyadan kənarda: redaktor ekranı öz `HadithViewModel`-ini qurur (`viewModel { … }`),
 * oxucu isə tətbiq-səviyyəli instansiyanı işlədir (`appScopedViewModelStoreOwner()`). Yəni yazma bir
 * instansiyada baş verir, bab keşi ([HadithViewModel.hadithCache]) isə tamam başqasındadır — yazan
 * instansiyanın öz keşini təmizləməsi oxucuya heç nə demir. Bu sayğac hər instansiyaya «məzmun
 * dəyişdi» xəbərini çatdırır: keş növbəti oxunuşda atılır, UI isə onu açar kimi işlədib yenidən oxuyur.
 *
 * ⚠️ Android bu tələni gizlədir: orada oxucu ayrıca `ActivityHadith`-dir, hər açılışda təzə
 * ViewModel (və boş keş) düşür. iOS-da tək app-scoped instansiya proses bitənə qədər yaşayır, ona
 * görə yeni əlavə olunmuş hədis yalnız tətbiq bağlanıb açılandan sonra görünürdü.
 */
private val hadithContentRevision = MutableStateFlow(0)

class HadithViewModel : ViewModel() {
    private val hadithDao = RepositoryProvider.hadithDatabase.hadithDao()

    private val _volumes = MutableStateFlow<List<HadithVolume>>(emptyList())
    val volumes: StateFlow<List<HadithVolume>> = _volumes.asStateFlow()

    private val _books = MutableStateFlow<List<HadithBook>>(emptyList())
    val books: StateFlow<List<HadithBook>> = _books.asStateFlow()

    /**
     * Cari cildin mündəricat ağacı: kitablar → bablar → alt bablar.
     *
     * Cild ekranındakı logo bunu açır. Üç toplu sorğu ilə bir dəfəyə yığılır — hədislər daxil
     * edilmir, ağac yalnız başlıqları göstərir, hədisləri seçilən yerə keçəndə reader yükləyir.
     */
    private val _volumeOutline = MutableStateFlow<HadithOutline?>(null)
    val volumeOutline: StateFlow<HadithOutline?> = _volumeOutline.asStateFlow()

    private val _chapters = MutableStateFlow<List<HadithChapter>>(emptyList())
    val chapters: StateFlow<List<HadithChapter>> = _chapters.asStateFlow()

    private val _subChapters = MutableStateFlow<List<HadithSubChapter>>(emptyList())
    val subChapters: StateFlow<List<HadithSubChapter>> = _subChapters.asStateFlow()

    private val _hadiths = MutableStateFlow<List<Hadith>>(emptyList())
    val hadiths: StateFlow<List<Hadith>> = _hadiths.asStateFlow()

    private val _combinedItems = MutableStateFlow<List<HadithListItem>>(emptyList())
    val combinedItems: StateFlow<List<HadithListItem>> = _combinedItems.asStateFlow()

    // Göstərilən hədislərin hansı baba aid olduğu. `hadiths` boş olmayan siyahı ilə birlikdə bunu
    // yoxlamaq köhnə bab məzmununu yenidən qurulmuş ekranda «hazır» sanmağın qarşısını alır — bax
    // pageTurnEnterEffect: səhifə dönmə effekti yalnız CARİ babın məzmunu ekranda olanda oynayır.
    private val _loadedHadithKey = MutableStateFlow<String?>(null)
    val loadedHadithKey: StateFlow<String?> = _loadedHadithKey.asStateFlow()

    // Qonşu babların kiçik keşi — Quran vərəqləyicisinin `beyondViewportPageCount = 1` qonşu
    // yükləməsinin hədisdəki qarşılığı. Sürüşəndə hədəf bab keşdədirsə məzmun ANİ verilir (siyahı
    // boşalmır), ona görə səhifə dönmə effekti Quran kimi qaralmadan, hazır məzmunun üstündə oynayır.
    private val hadithCache = LinkedHashMap<String, List<Hadith>>()

    private fun cacheHadiths(key: String, value: List<Hadith>) {
        hadithCache.remove(key)          // sona daşı (LRU təsiri)
        hadithCache[key] = value
        while (hadithCache.size > HADITH_CACHE_MAX) {
            val eldest = hadithCache.keys.firstOrNull() ?: break
            hadithCache.remove(eldest)
        }
    }

    /**
     * Bax [hadithContentRevision]. Ekranlar bunu açar kimi işlədir: dəyişəndə bab məzmununu bazadan
     * yenidən oxuyurlar (redaktordan qayıdanda `LaunchedEffect`-in açarı dəyişmədiyi üçün özü işə
     * düşmür — səhifə köhnə siyahını göstərməyə davam edərdi).
     */
    val contentRevision: StateFlow<Int> = hadithContentRevision.asStateFlow()

    private var seenContentRevision = hadithContentRevision.value

    /** Başqa instansiyada yazma olubsa bab keşini atır. Hər keş oxunuşundan ƏVVƏL çağırılır. */
    private fun invalidateCacheIfStale() {
        val current = hadithContentRevision.value
        if (current != seenContentRevision) {
            seenContentRevision = current
            hadithCache.clear()
        }
    }

    /** Uğurlu yazma/silmədən sonra: hər instansiyanın keşi növbəti oxunuşda düşür. */
    private fun bumpContentRevision() {
        hadithContentRevision.update { it + 1 }
    }

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
                // Sinxron bazaya yeni sətirlər gətirir — açıq bab keşi onlardan xəbərsizdir.
                if (status == ResourceDownloadStatus.Completed &&
                    _syncStatus.value != ResourceDownloadStatus.Completed
                ) {
                    bumpContentRevision()
                }
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
            bumpContentRevision()
            
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


    private var outlineJob: Job? = null
    private var loadedOutlineKey: String? = null

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
        loadHadiths(hadithKey(chapterSlug, null)) { hadithDao.getHadithsByChapter(chapterSlug).map { it.toModel() } }
    }

    fun fetchHadithsBySubChapter(chapterSlug: String, subChapterSlug: String) {
        loadHadiths(hadithKey(chapterSlug, subChapterSlug)) {
            hadithDao.getHadithsBySubChapter(chapterSlug, subChapterSlug).map { it.toModel() }
        }
    }

    /**
     * Cari babın hədislərini göstərir. Keşdədirsə **sinxron** verir (siyahı boşalmır, yükləmə
     * spinneri görünmür) — səhifə dönmə effektinin qaralmadan oynaması buna bağlıdır. Keşdə yoxdursa
     * əvvəlki davranış: boşalt, arxa planda yüklə.
     */
    private fun loadHadiths(key: String, load: suspend () -> List<Hadith>) {
        hadithsJob?.cancel()
        invalidateCacheIfStale()
        val cached = hadithCache[key]
        if (cached != null) {
            _hadiths.value = cached
            _loadedHadithKey.value = key
            return
        }
        _hadiths.value = emptyList()
        _loadedHadithKey.value = null
        hadithsJob = viewModelScope.launch(Dispatchers.IO) {
            val local = load()
            cacheHadiths(key, local)
            _hadiths.value = local
            _loadedHadithKey.value = key
        }
    }

    /**
     * Qonşu babı arxa planda keşə yükləyir — `_hadiths`/`_loadedHadithKey`-ə toxunmadan. Ekran cari
     * bab yükləndikdən sonra əvvəlki/növbəti babı isindirir ki, sürüşmə ani olsun.
     */
    fun prefetchHadiths(chapterSlug: String?, subChapterSlug: String?) {
        val chapter = chapterSlug ?: return
        val key = hadithKey(chapter, subChapterSlug)
        invalidateCacheIfStale()
        if (hadithCache.containsKey(key)) return
        viewModelScope.launch(Dispatchers.IO) {
            val local = if (subChapterSlug != null && subChapterSlug != "DIRECT_VIEW") {
                hadithDao.getHadithsBySubChapter(chapter, subChapterSlug).map { it.toModel() }
            } else {
                hadithDao.getHadithsByChapter(chapter).map { it.toModel() }
            }
            cacheHadiths(key, local)
        }
    }

    /** Bab məzmununun keş/yüklənmə açarı. Ekran eyni funksiya ilə `contentReady`-ni hesablayır. */
    fun hadithKey(chapterSlug: String, subChapterSlug: String?): String =
        if (subChapterSlug != null && subChapterSlug != "DIRECT_VIEW") "s:$chapterSlug/$subChapterSlug"
        else "c:$chapterSlug"

    /**
     * Bir babın hədislərini **pager səhifəsi** üçün verir — `_hadiths`-ə toxunmadan. Keşdədirsə
     * (qonşu bablar `prefetchHadiths` ilə isindirilib) ani qayıdır, yoxdursa bazadan yükləyib keşə
     * yazır. Vərəqləyicinin hər səhifəsi öz babını müstəqil yükləyir, ona görə paylaşılan cari-bab
     * `_hadiths` axını ilə yarışmır (Quran pager-inin qonşu səhifə qurmasının qarşılığı).
     */
    suspend fun getHadithsForBab(chapterSlug: String, subChapterSlug: String?): List<Hadith> {
        val key = hadithKey(chapterSlug, subChapterSlug)
        invalidateCacheIfStale()
        hadithCache[key]?.let { return it }
        val local = withContext(Dispatchers.IO) {
            if (subChapterSlug != null && subChapterSlug != "DIRECT_VIEW") {
                hadithDao.getHadithsBySubChapter(chapterSlug, subChapterSlug).map { it.toModel() }
            } else {
                hadithDao.getHadithsByChapter(chapterSlug).map { it.toModel() }
            }
        }
        cacheHadiths(key, local)
        return local
    }

    /** Keşdəki bab hədislərini sinxron verir — səhifə ilk kadrda qaralmadan açılsın deyə. */
    fun cachedHadiths(chapterSlug: String, subChapterSlug: String?): List<Hadith>? {
        invalidateCacheIfStale()
        return hadithCache[hadithKey(chapterSlug, subChapterSlug)]
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

    /** [volumeOutline]-i cildin lokal cədvəllərindən qurur. */
    fun fetchVolumeOutline(volumeSlug: String) {
        outlineJob?.cancel()
        if (loadedOutlineKey != volumeSlug) {
            _volumeOutline.value = null
            loadedOutlineKey = volumeSlug
        }
        outlineJob = viewModelScope.launch(Dispatchers.IO) {
            val books = hadithDao.getBooksByVolume(volumeSlug).map { it.toModel() }
            val chapters = hadithDao.getChaptersByBooks(books.map { it.slug }).map { it.toModel() }
            val subs = hadithDao.getSubChaptersByChapters(chapters.map { it.slug }).map { it.toModel() }

            _volumeOutline.value = HadithOutline(
                books = books,
                chaptersByBook = chapters.groupBy { it.book_slug },
                subChaptersByChapter = subs.groupBy { it.chapter_slug },
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

    /**
     * Bu kitabda artıq olan bab adları — hər iki dildə.
     *
     * Toplu idxalın yoxlaması bunu istifadə edir: eyni kitabı ikinci dəfə yapışdırmaq heç bir xəta
     * vermir, bablar sadəcə nömrələnməyə davam edir və kitab ikiqat olur.
     */
    internal suspend fun getChapterNames(bookSlug: String): Set<String> = withContext(Dispatchers.IO) {
        return@withContext hadithDao.getChaptersByBook(bookSlug)
            .flatMap { listOfNotNull(it.name, it.name_ar) }
            .toSet()
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
                    bumpContentRevision()
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
     * Bir panodan gələn **bir neçə** hədisi ardıcıl yazır — redaktorda əlavə hədis kartı olanda
     * `onSave` bura düşür.
     *
     * Sətirlər bir-bir gedir, toplu `upsert` ilə yox: moderasiya trigger-i sətir başına işləyir
     * (admin üçün sətir geri gəlir, redaktor üçün cavab boş olur), toplu yazıda isə hansı sətrin
     * hansı taleyi yaşadığı bilinmir. Uğursuzlar [onResult]-a qaytarılır ki, redaktor onları formada
     * saxlasın — əks halda şəbəkə xətası on beş sətirlik mətni birdəfəlik itirərdi.
     */
    fun upsertHadiths(hadiths: List<Hadith>, onResult: (failed: List<Hadith>) -> Unit) {
        if (hadiths.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val failed = mutableListOf<Hadith>()
                var savedCount = 0
                var queuedCount = 0

                hadiths.forEach { hadith ->
                    try {
                        val saved = SupabaseProvider.client.from("hadith").upsert(hadith) {
                            select()
                        }.decodeSingleOrNull<Hadith>()

                        if (saved != null) {
                            hadithDao.insertHadiths(listOf(saved.toEntity()!!))
                            savedCount++
                        } else {
                            // Moderasiyaya düşdü — əsas məzmun hələ dəyişmədiyi üçün lokal bazaya
                            // toxunmuruq.
                            queuedCount++
                        }
                    } catch (ex: Exception) {
                        failed += hadith
                        AppLogger.d("HadithViewModel", "Error upserting hadith: ${ex.message}")
                    }
                }

                if (savedCount > 0) bumpContentRevision()

                // Sətir başına bildiriş vermirik — üç hədis üç toast demək olardı.
                // Bildiriş ikinci dərəcəlidir və `onResult`-u heç vaxt udmamalıdır: sətirlər artıq
                // yazılıb, forma isə bağlanmayıb qalsa istifadəçi «Yadda saxla»-nı təkrar basıb
                // hamısını **ikinci dəfə** əlavə edər.
                runCatching {
                    if (queuedCount > 0) {
                        PlatformUtils.showLongToast(getString(Res.string.strMsgEditQueuedForReview))
                    } else if (savedCount > 1) {
                        PlatformUtils.showToast(getString(Res.string.strMsgHadithsSaved, savedCount))
                    }
                    if (failed.isNotEmpty()) {
                        PlatformUtils.showLongToast(
                            getString(Res.string.strMsgHadithsSaveFailed, failed.size)
                        )
                    }
                }.onFailure { AppLogger.d("HadithViewModel", "Toast failed: ${it.message}") }

                withContext(Dispatchers.Main) { onResult(failed) }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Toplu idxalın nəticəsi — nə yazıldı, harada dayandı. */
    internal data class BulkImportOutcome(
        val chapters: Int,
        val subChapters: Int,
        val hadiths: Int,
        /** Moderasiyaya düşən hədislər: cavab boş gəldi, əsas cədvəl hələ dəyişmədi. */
        val queued: Int,
        val failed: Int,
        /**
         * Struktur sətri yazıla bilmədi (RLS: bab/alt bab yalnız admindən keçir), ona görə idxal
         * yarımçıq dayandırıldı — qalan hədislərin valideyni olmayacaqdı.
         */
        val stoppedAt: Int?,
        /**
         * Yazılmamış sətirlər — uğursuz hədislər, sonra dayanma nöqtəsindən qalan hər şey, ilkin
         * sıra ilə.
         *
         * Təkrar cəhd **bunları** göndərməlidir, bütün planı yox: hədis sətrinin `id`-si yoxdur,
         * ona görə eyni planı ikinci dəfə yazmaq artıq keçmiş hədisləri surətləyərdi (bab və alt
         * bab slug üzərindən upsert olunduğu üçün təkrarlanmır, hədis isə hər dəfə yeni sətirdir).
         */
        val remaining: List<BulkRow>,
        /** Bazaya həqiqətən düşən sətirlər — [undoBulkImport] məhz bunları silir. */
        val written: BulkWritten = BulkWritten(),
    ) {
        val writtenCount: Int get() = chapters + subChapters + hadiths
    }

    /**
     * İdxalın arxasında qoyduğu iz: yazılan bab/alt bab slug-ları və hədis sətirləri.
     *
     * Hədislər `id` ilə saxlanılır, çünki `hadith` cədvəlində sətri tanıdan yeganə şey odur —
     * `chapter_slug` + `hadith_no` cütü unikal deyil və eyni nömrəni ikinci idxal da verə bilər.
     * Struktur isə slug üzərindən gedir; slug idxalın özü tərəfindən yeni yaradılır, ona görə
     * silmək əvvəldən mövcud olan heç nəyə toxunmur.
     */
    internal data class BulkWritten(
        val chapters: List<String> = emptyList(),
        val subChapters: List<String> = emptyList(),
        val hadiths: List<Hadith> = emptyList(),
    ) {
        val total: Int get() = chapters.size + subChapters.size + hadiths.size

        val isEmpty: Boolean get() = total == 0

        operator fun plus(other: BulkWritten): BulkWritten = BulkWritten(
            chapters = chapters + other.chapters,
            subChapters = subChapters + other.subChapters,
            hadiths = hadiths + other.hadiths,
        )
    }

    /** Geri almanın nəticəsi: neçə sətir getdi, neçəsi qaldı. */
    internal data class BulkUndoOutcome(val removed: Int, val blocked: Int)

    /**
     * Bir yapışdırmadan çıxan **bütün** sətirləri sıra ilə yazır: bab, alt bab, hədis — nömrələri və
     * slug-ları artıq [com.cafarovceyxun.anamuslim.compose.screens.hadith.buildBulkPlan] təyin edib.
     *
     * Sətirlər bir-bir gedir, toplu `upsert` ilə yox — səbəbi [upsertHadiths]-dəki ilə eynidir:
     * moderasiya trigger-i sətir başına işləyir və toplu yazıda hansı sətrin hansı taleyi yaşadığı
     * bilinmir. Üstəlik burada **sıra əhəmiyyətlidir**: hədisin valideyni ondan əvvəlki babdır.
     *
     * Bab və ya alt bab yazıla bilməyəndə idxal həmin yerdə **dayanır**. `hadith.chapter_slug`
     * xarici açar deyil (bax `docs/supabase/SCHEMA.md`), yəni davam etsək hədislər mövcud olmayan
     * slug-a bağlanıb heç bir ekranda görünməyən yetim sətirlərə çevrilərdi. Hədisin özü uğursuz
     * olsa isə sıra pozulmur — sayılır və idxal davam edir.
     */
    internal fun importBulkRows(
        rows: List<BulkRow>,
        onProgress: (Int) -> Unit,
        onResult: (BulkImportOutcome) -> Unit,
    ) {
        if (rows.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            var chapters = 0
            var subChapters = 0
            var hadiths = 0
            var queued = 0
            var failed = 0
            var stoppedAt: Int? = null
            val remaining = mutableListOf<BulkRow>()
            val writtenChapters = mutableListOf<String>()
            val writtenSubChapters = mutableListOf<String>()
            val writtenHadiths = mutableListOf<Hadith>()

            try {
                for ((index, entry) in rows.withIndex()) {
                    val structureBlocked = when (entry) {
                        is BulkRow.Chapter -> {
                            val saved = runCatching {
                                SupabaseProvider.client.from("hadith_chapter").upsert(entry.row) {
                                    select()
                                }.decodeSingleOrNull<HadithChapter>()
                            }.onFailure {
                                AppLogger.d("HadithViewModel", "Bulk chapter failed: ${it.message}")
                            }.getOrNull()

                            if (saved != null) {
                                hadithDao.insertChapters(listOf(saved.toEntity()))
                                writtenChapters += saved.slug
                                chapters++
                            }
                            saved == null
                        }

                        is BulkRow.SubChapter -> {
                            val saved = runCatching {
                                SupabaseProvider.client.from("hadith_sub_chapter").upsert(entry.row) {
                                    select()
                                }.decodeSingleOrNull<HadithSubChapter>()
                            }.onFailure {
                                AppLogger.d("HadithViewModel", "Bulk sub chapter failed: ${it.message}")
                            }.getOrNull()

                            if (saved != null) {
                                hadithDao.insertSubChapters(listOf(saved.toEntity()))
                                writtenSubChapters += saved.slug
                                subChapters++
                            }
                            saved == null
                        }

                        is BulkRow.HadithRow -> {
                            runCatching {
                                SupabaseProvider.client.from("hadith").upsert(entry.row) {
                                    select()
                                }.decodeSingleOrNull<Hadith>()
                            }.fold(
                                onSuccess = { saved ->
                                    if (saved != null) {
                                        hadithDao.insertHadiths(listOf(saved.toEntity()!!))
                                        writtenHadiths += saved
                                        hadiths++
                                    } else {
                                        // Moderasiyaya düşdü — lokal bazaya toxunmuruq.
                                        queued++
                                    }
                                },
                                onFailure = {
                                    failed++
                                    remaining += entry
                                    AppLogger.d("HadithViewModel", "Bulk hadith failed: ${it.message}")
                                },
                            )
                            false
                        }
                    }

                    withContext(Dispatchers.Main) { onProgress(index + 1) }

                    if (structureBlocked) {
                        stoppedAt = index
                        remaining += rows.drop(index)
                        break
                    }
                }

                if (chapters + subChapters + hadiths > 0) bumpContentRevision()
            } finally {
                _isLoading.value = false
                withContext(Dispatchers.Main) {
                    onResult(
                        BulkImportOutcome(
                            chapters = chapters,
                            subChapters = subChapters,
                            hadiths = hadiths,
                            queued = queued,
                            failed = failed,
                            stoppedAt = stoppedAt,
                            remaining = remaining,
                            written = BulkWritten(
                                chapters = writtenChapters,
                                subChapters = writtenSubChapters,
                                hadiths = writtenHadiths,
                            ),
                        )
                    )
                }
            }
        }
    }

    /**
     * Bir toplu idxalı geri alır: əvvəl hədislər, sonra alt bablar, sonra bablar.
     *
     * Sıra məcburidir — struktur sətri uşaqları qalarkən silinsə, hədislər mövcud olmayan slug-a
     * bağlanıb heç bir ekranda görünməyən yetim sətirlərə çevrilərdi (`hadith.chapter_slug` xarici
     * açar deyil, bax `docs/supabase/SCHEMA.md`). Silmə birbaşa burada aparılır, [deleteChapter] və
     * qonşuları vasitəsilə yox: onların hər biri öz coroutine-ini açıb callback qaytarır, buradakı
     * ardıcıllığa isə tək axın lazımdır.
     *
     * Yalnız **bu idxalın yazdığı** sətirlərə toxunur ([BulkWritten]): slug-ları idxalın özü yeni
     * yaradıb, hədislər isə `id` ilə tanınır, ona görə əvvəldən kitabda olan heç nə silinmir.
     *
     * RLS bloklayanda PostgREST xəta yox, **boş nəticə** qaytarır — ona görə hər silmə `select()`
     * ilə gedir və qayıdan sətir sayına baxılır. Redaktor hesabında hədis silmək moderasiyaya
     * düşür, struktur isə ümumiyyətlə silinmir; hər ikisi «qaldı» kimi sayılır və ekran bunu deyir.
     */
    internal fun undoBulkImport(
        written: BulkWritten,
        onProgress: (Int) -> Unit,
        onResult: (BulkUndoOutcome) -> Unit,
    ) {
        if (written.isEmpty) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            var removed = 0
            var blocked = 0
            var done = 0

            suspend fun step() {
                done++
                withContext(Dispatchers.Main) { onProgress(done) }
            }

            try {
                // Tərs sıra: axırıncı yazılan birinci gedir, yəni valideyn həmişə uşaqlarından sonra.
                for (hadith in written.hadiths.asReversed()) {
                    val id = hadith.id
                    if (id == null) {
                        blocked++
                        step()
                        continue
                    }

                    val gone = runCatching {
                        SupabaseProvider.client.from("hadith").delete {
                            select()
                            filter { eq("id", id) }
                        }.decodeList<JsonObject>().size
                    }.onFailure {
                        AppLogger.d("HadithViewModel", "Bulk undo hadith failed: ${it.message}")
                    }.getOrDefault(0) > 0

                    if (gone) {
                        hadithDao.deleteHadithById(id)
                        removed++
                    } else {
                        blocked++
                    }
                    step()
                }

                for (slug in written.subChapters.asReversed()) {
                    if (deleteStructureRow("hadith_sub_chapter", slug)) {
                        hadithDao.deleteSubChapterBySlug(slug)
                        removed++
                    } else {
                        blocked++
                    }
                    step()
                }

                for (slug in written.chapters.asReversed()) {
                    if (deleteStructureRow("hadith_chapter", slug)) {
                        hadithDao.deleteChapterBySlug(slug)
                        removed++
                    } else {
                        blocked++
                    }
                    step()
                }

                if (removed > 0) bumpContentRevision()
            } finally {
                _isLoading.value = false
                withContext(Dispatchers.Main) {
                    onResult(BulkUndoOutcome(removed = removed, blocked = blocked))
                }
            }
        }
    }

    /** Bir struktur sətrini slug ilə silir; RLS bloklayıbsa cavab boş gəlir və `false` qayıdır. */
    private suspend fun deleteStructureRow(table: String, slug: String): Boolean = runCatching {
        SupabaseProvider.client.from(table).delete {
            select()
            filter { eq("slug", slug) }
        }.decodeList<JsonObject>().size
    }.onFailure {
        AppLogger.d("HadithViewModel", "Bulk undo $table failed: ${it.message}")
    }.getOrDefault(0) > 0

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
                    bumpContentRevision()
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
                    bumpContentRevision()
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
