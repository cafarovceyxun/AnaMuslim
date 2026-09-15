package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
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
 * Əsmaül Hüsnə — 99 ad və onlara bağlanmış dəlillər.
 *
 * ⚠️ **Dəlillər ada görə, tələb olunanda yüklənir.** Bir ada çox dəlil düşəcək, 99 adın hamısını
 * birdən çəkmək isə iki səbəbdən yaramır: PostgREST bir cavabda 1000 sətirdən çoxunu vermir (əskik
 * məzmun **səssizcə** düşərdi) və hər ekran açılışında hamısını endirmək mobil internetdə lazımsız
 * yükdür. Siyahıdakı say nişanı ayrıca, ucuz sorğudan gəlir (`asma_evidence_count` view-u).
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

    /** Hazırda yüklənməkdə olan adlar — səhifə «boşdur», yoxsa «hələ gəlməyib» sualının cavabı. */
    private val _loadingNames = MutableStateFlow<Set<Int>>(emptySet())
    val loadingNames: StateFlow<Set<Int>> = _loadingNames.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    val revision: StateFlow<Int> = asmaContentRevision.asStateFlow()

    init {
        _names.value = repository.cachedNames()
        _counts.value = repository.cachedCounts()
        refresh()
    }

    /** Adlar və say nişanları. Dəlillərin özünə toxunmur — onlar [ensureEvidence] ilə gəlir. */
    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _names.value = repository.fetchNames()
            _counts.value = repository.fetchEvidenceCounts()
            _isLoaded.value = true
            _isLoading.value = false
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
        if (!force && _evidenceByName.value.containsKey(nameNo)) return
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

    /** Yazmadan sonra: həmin adın siyahısı yenidən oxunur, say nişanları yenilənir. */
    private fun reloadAfterWrite(nameNo: Int) {
        ensureEvidence(nameNo, force = true)
        viewModelScope.launch { _counts.value = repository.fetchEvidenceCounts() }
    }

    private fun bumpRevision() {
        asmaContentRevision.value = asmaContentRevision.value + 1
    }
}
