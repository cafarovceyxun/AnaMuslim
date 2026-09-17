package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.repository.AsmaAutoEvidenceRepository
import com.cafarovceyxun.anamuslim.repository.AutoVerseMatch
import com.cafarovceyxun.anamuslim.repository.supabase.AsmaRepository
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.duaMsgDeleteFailed
import com.cafarovceyxun.anamuslim.resources.duaMsgDeleted
import com.cafarovceyxun.anamuslim.repository.supabase.DuaDuplicateException
import com.cafarovceyxun.anamuslim.resources.duaMsgDuplicate
import com.cafarovceyxun.anamuslim.resources.duaMsgSaveFailed
import com.cafarovceyxun.anamuslim.resources.duaMsgSaved
import com.cafarovceyxun.anamuslim.utils.supabase.AsmaEvidence
import com.cafarovceyxun.anamuslim.utils.supabase.AsmaName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/** Bax [DuaViewModel]-in yanındakı sayğaca — eyni səbəb, ayrı məzmun. */
private val asmaContentRevision = MutableStateFlow(0)

/**
 * Avtomatik uyğunluqların bir səhifəsi.
 *
 * 50 seçilib, çünki bəzi adlar minlərlə ayədə keçir («الله» ~2700): tam siyahını birdən kompozisiya
 * etmək ekranı dondurar, daha kiçik səhifə isə «daha çox yüklə»ni çox tez-tez basdırar.
 */
private const val AUTO_PAGE_SIZE = 50

/**
 * Əsmaül Hüsnə — 99 ad və onlara bağlanmış dəlillər.
 *
 * **Dəlillər iki yoldan gəlir:** açılan ad üçün ani [ensureEvidence], bütün adlar üçün isə arxa
 * fondakı [prefetchAllEvidence]. İkincisi ekran bir dəfə açılandan sonra tətbiqin **oflayn** işləməsi
 * üçündür: istifadəçi şəbəkəsiz qalanda açmadığı adların da dəlillərini görür. İkisi də səhifələnir
 * (PostgREST bir cavabda 1000 sətir verir və limitə dəyən sorğu xəta yox, qısa cavab qaytarır).
 *
 * Siyahıdakı say nişanı ayrıca, ucuz sorğudan gəlir (`asma_evidence_count` view-u) — nişan 99 ad üçün
 * birdən lazımdır, dəlillərin özü isə hələ gəlməmiş ola bilər.
 */
class AsmaViewModel : ViewModel() {

    private val repository = AsmaRepository()

    private val _names = MutableStateFlow<List<AsmaName>>(emptyList())
    val names: StateFlow<List<AsmaName>> = _names.asStateFlow()

    /** Ad nömrəsi → dəlil sayı. Siyahıdakı nişan bunu oxuyur. */
    private val _counts = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val counts: StateFlow<Map<Int, Int>> = _counts.asStateFlow()

    /** Yüklənmiş adların dəlilləri. Açılmayan ad burada **ümumiyyətlə yoxdur**. */
    private val _evidenceByName = MutableStateFlow<Map<Int, List<AsmaEvidence>>>(emptyMap())
    val evidenceByName: StateFlow<Map<Int, List<AsmaEvidence>>> = _evidenceByName.asStateFlow()

    /**
     * Bütün adların dəlilləri bu sessiyada **serverdən** gəlibmi.
     *
     * Xəritədə açarın olmaması iki şey demək ola bilər: ada dəlil yoxdur, yoxsa hələ yüklənməyib.
     * Bayraq bu fərqi saxlayır — [ensureEvidence] qalxmış bayraqda dəlilsiz adı təkrar sorğulamır,
     * enmiş bayraqda isə (məsələn, toplu yükləmə şəbəkə xətası ilə bitibsə) təkrar cəhdə imkan verir.
     */
    private var allEvidenceLoaded = false

    /**
     * Bu sessiyada **ayrıca** (şəbəkədən) çəkilmiş adlar.
     *
     * Toplu yükləmə uzun sürə bilər; o gedərkən açılan səhifə və ya redaktədən sonrakı `force`
     * daha təzə siyahı gətirir. Toplu cavab gələndə [prefetchAllEvidence] məhz bu adları geri
     * qoyur ki, təzə məlumat köhnə toplu nəticə ilə əzilməsin.
     */
    private val freshNames = mutableSetOf<Int>()

    /** Hazırda yüklənməkdə olan adlar — səhifə «boşdur», yoxsa «hələ gəlməyib» sualının cavabı. */
    private val _loadingNames = MutableStateFlow<Set<Int>>(emptySet())
    val loadingNames: StateFlow<Set<Int>> = _loadingNames.asStateFlow()

    // ---- Avtomatik uyğunlaşdırma ----

    private val autoRepository = AsmaAutoEvidenceRepository()

    /**
     * Ad → lokal Quran indeksindən tapılmış ayələr.
     *
     * Serverdə saxlanmır: hər dəfə `arabic_search` FTS cədvəlindən hesablanır, ona görə **oflayn**
     * da işləyir. Səhifə-səhifə yüklənir — bəzi adlar minlərlə ayədə keçir.
     */
    private val _autoMatches = MutableStateFlow<Map<Int, List<AutoVerseMatch>>>(emptyMap())
    val autoMatches: StateFlow<Map<Int, List<AutoVerseMatch>>> = _autoMatches.asStateFlow()

    /** Ad → ümumi uyğunluq sayı (gizlədilənlər daxil). «Daha çox var?» sualının cavabı. */
    private val _autoCounts = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val autoCounts: StateFlow<Map<Int, Int>> = _autoCounts.asStateFlow()

    /** Admin tərəfindən səhv sayılıb gizlədilmiş uyğunluqlar — serverdən, hamıya təsir edir. */
    private val _autoHidden = MutableStateFlow<Map<Int, Set<Pair<Int, Int>>>>(emptyMap())
    val autoHidden: StateFlow<Map<Int, Set<Pair<Int, Int>>>> = _autoHidden.asStateFlow()

    /** Hazırda avtomatik uyğunluqları yüklənən adlar. */
    private val _loadingAuto = MutableStateFlow<Set<Int>>(emptySet())
    val loadingAuto: StateFlow<Set<Int>> = _loadingAuto.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    val revision: StateFlow<Int> = asmaContentRevision.asStateFlow()

    init {
        _names.value = repository.cachedNames()
        _counts.value = repository.cachedCounts()
        // Keş dərhal göstərilir: soyuq açılışda və şəbəkəsiz ekran boş qalmasın.
        _evidenceByName.value = repository.cachedAllEvidence()
        refresh()
    }

    /**
     * Adlar, say nişanları və **bütün** dəlillər.
     *
     * Dəlillərin toplu yüklənməsi ayrıca coroutine-dədir ki, adların gəlişini ləngitməsin: siyahı
     * onsuz da adlarla çəkilir, dəlillər isə yalnız ad açılanda görünür.
     */
    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _names.value = repository.fetchNames()
            _counts.value = repository.fetchEvidenceCounts()
            _isLoaded.value = true
            _isLoading.value = false
        }

        prefetchAllEvidence()

        viewModelScope.launch { _autoHidden.value = repository.fetchAutoHidden() }
    }

    /**
     * 99 adın avtomatik uyğunluq sayını arxa fonda hesablayır.
     *
     * Sayğaclar **keşlənmir**: bunlar lokal FTS `COUNT(*)` sorğularıdır (tətbiqin öz axtarışı ilə
     * eyni qiymət sinfi) və DataStore keşi əvəzinə onları yenidən hesablamaq həm sadədir, həm də
     * Quran asseti yeniləndikdə köhnəlmiş sayğac riskini aradan qaldırır. Siyahı sayğac gələnə qədər
     * bloklanmır — nişan sadəcə sonradan görünür.
     */
    fun ensureAutoCounts(names: List<AsmaName>) {
        if (_autoCounts.value.isNotEmpty() || names.isEmpty()) return

        viewModelScope.launch {
            _autoCounts.value = names.associate { it.no to autoRepository.matchCount(it.name_ar) }
        }
    }

    /**
     * Bir adın avtomatik uyğunluqlarının **növbəti səhifəsini** yükləyir.
     *
     * Artıq yüklənmiş səhifələrin üstünə əlavə olunur, ona görə «daha çox yüklə» düyməsi eyni
     * funksiyanı çağırır. Gizlədilənlər sorğudan **sonra** süzülür (bax [AsmaAutoEvidenceRepository]).
     */
    fun loadMoreAuto(name: AsmaName) {
        if (name.no in _loadingAuto.value) return

        val loaded = _autoMatches.value[name.no].orEmpty()
        val total = _autoCounts.value[name.no]
        if (total != null && loaded.isNotEmpty() && loaded.size >= total) return

        viewModelScope.launch {
            _loadingAuto.value = _loadingAuto.value + name.no

            // Offset **çəkilmiş** sətir sayına görədir, göstərilənə görə yox: gizlədilənlər süzülüb
            // atıldığı üçün ikisi fərqlənir və göstərilən sayı işlətmək eyni ayələri təkrar gətirərdi.
            val offset = _autoOffsets.value[name.no] ?: 0

            val page = autoRepository.matches(
                nameAr = name.name_ar,
                limit = AUTO_PAGE_SIZE,
                offset = offset,
                exclude = _autoHidden.value[name.no].orEmpty(),
            )

            _autoOffsets.value = _autoOffsets.value + (name.no to offset + AUTO_PAGE_SIZE)
            _autoMatches.value = _autoMatches.value + (name.no to loaded + page)
            _loadingAuto.value = _loadingAuto.value - name.no
        }
    }

    /** Ad açılanda ilk səhifəni gətirir — artıq yüklənibsə heç nə etmir. */
    fun ensureAutoEvidence(name: AsmaName) {
        if (_autoMatches.value.containsKey(name.no)) return
        loadMoreAuto(name)
    }

    /**
     * Avtomatik uyğunluğu gizlədir və ya geri qaytarır — yalnız admin (qapı bazadadır).
     *
     * Ekran dərhal yenilənir; server cavabı uğursuz olsa siyahı serverdəkinə qaytarılır.
     */
    fun toggleAutoHidden(nameNo: Int, chapterNo: Int, verseNo: Int, hide: Boolean) {
        viewModelScope.launch {
            val key = chapterNo to verseNo
            val before = _autoHidden.value[nameNo].orEmpty()

            _autoHidden.value = _autoHidden.value + (
                nameNo to (if (hide) before + key else before - key)
                )

            val result = if (hide) {
                repository.hideAuto(nameNo, chapterNo, verseNo)
            } else {
                repository.unhideAuto(nameNo, chapterNo, verseNo)
            }

            result
                .onSuccess {
                    // Süzgəc dəyişdi — həmin adın səhifələri yenidən qurulmalıdır.
                    _autoMatches.value = _autoMatches.value - nameNo
                    _autoOffsets.value = _autoOffsets.value - nameNo
                }
                .onFailure {
                    PlatformUtils.showToast(getString(Res.string.duaMsgSaveFailed))
                    _autoHidden.value = repository.fetchAutoHidden()
                }
        }
    }

    /** Ad → indiyə qədər **serverdən çəkilmiş** sətir sayı (gizlədilənlər daxil). */
    private val _autoOffsets = MutableStateFlow<Map<Int, Int>>(emptyMap())

    /**
     * Bütün adların dəlillərini arxa fonda çəkir — oflayn istifadə üçün.
     *
     * Gələn xəritə mövcud dəlillərin **üstünə** yazılır, onları əvəz etmir: bu iş gedərkən
     * istifadəçi bir ad açmış və [ensureEvidence] daha təzə siyahı gətirmiş ola bilər.
     */
    private fun prefetchAllEvidence() {
        if (allEvidenceLoaded) return

        viewModelScope.launch {
            repository.fetchAllEvidence().onSuccess { all ->
                allEvidenceLoaded = true
                // Toplu nəticə əsasdır, amma bu iş gedərkən ayrıca çəkilmiş adlar (açılan səhifə,
                // redaktədən sonrakı `force`) daha təzədir — onlar geri qoyulur.
                _evidenceByName.value = all + _evidenceByName.value.filterKeys { it in freshNames }
            }
        }
    }

    /**
     * Bir adın dəlillərini yükləyir — artıq yüklənibsə heç nə etmir.
     *
     * Vərəqləyicinin hər səhifəsi açılanda çağırılır; `HorizontalPager` qonşu səhifələri də
     * kompozisiya etdiyi üçün növbəti ad onsuz da qabaqcadan gəlir.
     *
     * @param force yazma/silmədən sonra `true` — keşdəki köhnə siyahı əvəz olunmalıdır.
     */
    fun ensureEvidence(nameNo: Int, force: Boolean = false) {
        // [allEvidenceLoaded] qalxıbsa xəritədə açarın olmaması «dəlil yoxdur» deməkdir, «hələ
        // gəlməyib» yox — əks halda dəlilsiz ad hər açılışda boş sorğu göndərərdi.
        if (!force && (allEvidenceLoaded || _evidenceByName.value.containsKey(nameNo))) return
        if (nameNo in _loadingNames.value) return

        viewModelScope.launch {
            _loadingNames.value = _loadingNames.value + nameNo

            // Keş dərhal göstərilir (oflayn və soyuq açılış üçün), şəbəkə cavabı onun üstünə gəlir.
            if (!_evidenceByName.value.containsKey(nameNo)) {
                val cached = repository.cachedEvidence(nameNo)
                if (cached.isNotEmpty()) {
                    _evidenceByName.value = _evidenceByName.value + (nameNo to cached)
                }
            }

            val items = repository.fetchEvidence(nameNo)
            _evidenceByName.value = _evidenceByName.value + (nameNo to items)
            freshNames += nameNo
            _loadingNames.value = _loadingNames.value - nameNo
        }
    }

    /** Bir adın dəlilləri; hələ yüklənməyibsə boş siyahı (bax [loadingNames]). */
    fun evidenceOf(nameNo: Int): List<AsmaEvidence> = _evidenceByName.value[nameNo].orEmpty()

    /**
     * Ada neçə dəlil bağlanıb.
     *
     * Ad artıq yüklənibsə **yüklənmiş siyahının uzunluğu** götürülür: yeni dəlil əlavə olunandan
     * sonra say nişanı serverdəki aqreqat yenilənməmiş də ola bilər, ekrandakı siyahı isə dəqiqdir.
     */
    fun countOf(nameNo: Int): Int =
        _evidenceByName.value[nameNo]?.size ?: _counts.value[nameNo] ?: 0

    /**
     * Adın mətnini və ya görünüşünü dəyişir — yalnız admin (qapı bazadadır).
     *
     * «Sil» əməli qəsdən yoxdur: `asma_evidence.name_no` CASCADE-dir, ad silinsə ona bağlanmış
     * bütün dəlillər də gedərdi. Siyahıdan çıxarmaq üçün `is_visible = false` işlədilir.
     */
    fun updateName(name: AsmaName, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true

            repository.updateName(name)
                .onSuccess {
                    bumpRevision()
                    refresh()
                    PlatformUtils.showToast(getString(Res.string.duaMsgSaved))
                    onSaved()
                }
                .onFailure {
                    PlatformUtils.showToast(getString(Res.string.duaMsgSaveFailed))
                }

            _isLoading.value = false
        }
    }

    fun saveEvidence(evidence: AsmaEvidence, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true

            repository.addEvidence(evidence)
                .onSuccess {
                    bumpRevision()
                    reloadAfterWrite(evidence.name_no)
                    PlatformUtils.showToast(getString(Res.string.duaMsgSaved))
                    onSaved()
                }
                .onFailure { error ->
                    // Dublikat nə icazə, nə də bağlantı problemidir — ayrıca mesaj lazımdır.
                    PlatformUtils.showToast(
                        getString(
                            if (error is DuaDuplicateException) Res.string.duaMsgDuplicate
                            else Res.string.duaMsgSaveFailed,
                        ),
                    )
                }

            _isLoading.value = false
        }
    }

    /** Dəlilin mətnini yeniləyir. */
    fun updateEvidence(evidence: AsmaEvidence, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true

            repository.updateEvidence(evidence)
                .onSuccess {
                    bumpRevision()
                    reloadAfterWrite(evidence.name_no)
                    PlatformUtils.showToast(getString(Res.string.duaMsgSaved))
                    onSaved()
                }
                .onFailure {
                    PlatformUtils.showToast(getString(Res.string.duaMsgSaveFailed))
                }

            _isLoading.value = false
        }
    }

    fun deleteEvidence(id: Long, nameNo: Int, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true

            repository.deleteEvidence(id)
                .onSuccess {
                    bumpRevision()
                    reloadAfterWrite(nameNo)
                    PlatformUtils.showToast(getString(Res.string.duaMsgDeleted))
                    onDeleted()
                }
                .onFailure {
                    PlatformUtils.showToast(getString(Res.string.duaMsgDeleteFailed))
                }

            _isLoading.value = false
        }
    }

    /**
     * 99 adın yeni sırasını yazır — yalnız admin (qapı bazadadır).
     *
     * Ekran dərhal yenilənir, sonra server cavabı gəlir: sıralama ekranından çıxanda siyahının
     * köhnə sıra ilə bir anlıq yanıb-sönməsi «yadda saxlanmadı» kimi görünürdü.
     *
     * @param nos adların **yeni sıradakı** nömrələri.
     */
    fun saveNameOrder(nos: List<Int>, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true

            val rank = nos.withIndex().associate { (index, no) -> no to index }
            val changes = _names.value.mapNotNull { name ->
                val target = rank[name.no] ?: return@mapNotNull null
                if (name.sort_no == target) null else name.no to target
            }

            _names.value = _names.value
                .map { it.copy(sort_no = rank[it.no] ?: it.sort_no) }
                .sortedWith(compareBy({ it.sort_no }, { it.no }))

            if (changes.isEmpty()) {
                onSaved()
            } else {
                repository.updateNameOrder(changes)
                    .onSuccess {
                        bumpRevision()
                        refresh()
                        PlatformUtils.showToast(getString(Res.string.duaMsgSaved))
                        onSaved()
                    }
                    .onFailure {
                        PlatformUtils.showToast(getString(Res.string.duaMsgSaveFailed))
                        refresh()
                    }
            }

            _isLoading.value = false
        }
    }

    /** Yazmadan sonra: həmin adın siyahısı yenidən oxunur, say nişanları yenilənir. */
    private fun reloadAfterWrite(nameNo: Int) {
        ensureEvidence(nameNo, force = true)
        viewModelScope.launch { _counts.value = repository.fetchEvidenceCounts() }
    }

    private fun bumpRevision() {
        asmaContentRevision.value = asmaContentRevision.value + 1
    }
}
