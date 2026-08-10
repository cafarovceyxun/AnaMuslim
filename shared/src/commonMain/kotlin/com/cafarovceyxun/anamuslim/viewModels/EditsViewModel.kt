package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.compose.utils.appLocaleFlow
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strMsgEditActionBlocked
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import com.cafarovceyxun.anamuslim.utils.supabase.QuranEdit
import com.cafarovceyxun.anamuslim.utils.supabase.HadithEdit
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalCoroutinesApi::class)
class EditsViewModel : ViewModel() {

    private val quranRepository get() = RepositoryProvider.quranRepository

    // Quran düzəlişinin başlığı "Ən-Nəbə 78:1" formasındadır — surə adı cari dildən asılıdır,
    // ona görə dil dəyişəndə xəritə yenidən oxunur.
    val chapterNames = appLocaleFlow.mapLatest {
        quranRepository.getChapterNames(QuranMeta.chapterRange.toList())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        emptyMap()
    )

    private val _quranEdits = MutableStateFlow<List<QuranEdit>>(emptyList())
    val quranEdits = _quranEdits.asStateFlow()

    private val _hadithEdits = MutableStateFlow<List<HadithEdit>>(emptyList())
    val hadithEdits = _hadithEdits.asStateFlow()

    // Yüklənmə və xəta tab-başına saxlanılır: hədis tərəfindəki nasazlıq Quran səhifəsini örtməsin.
    private val _quranLoading = MutableStateFlow(false)
    val quranLoading = _quranLoading.asStateFlow()

    private val _hadithLoading = MutableStateFlow(false)
    val hadithLoading = _hadithLoading.asStateFlow()

    private val _quranError = MutableStateFlow<String?>(null)
    val quranError = _quranError.asStateFlow()

    private val _hadithError = MutableStateFlow<String?>(null)
    val hadithError = _hadithError.asStateFlow()

    private val _quranFilter = MutableStateFlow("All")
    val quranFilter = _quranFilter.asStateFlow()

    private val _hadithFilter = MutableStateFlow("All")
    val hadithFilter = _hadithFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Axtarış kimi hər iki tab-a şamil olunur; `null` = bütün redaktorlar.
    private val _editorFilter = MutableStateFlow<String?>(null)
    val editorFilter = _editorFilter.asStateFlow()

    private val _selectedQuranEditIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedQuranEditIds = _selectedQuranEditIds.asStateFlow()

    private val _selectedHadithEditIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedHadithEditIds = _selectedHadithEditIds.asStateFlow()

    fun toggleQuranEditSelection(id: Long) {
        val current = _selectedQuranEditIds.value
        _selectedQuranEditIds.value = if (current.contains(id)) current - id else current + id
    }

    fun selectAllQuranEdits(edits: List<QuranEdit>) {
        _selectedQuranEditIds.value = edits.mapNotNull { it.id }.toSet()
    }

    fun clearQuranSelection() {
        _selectedQuranEditIds.value = emptySet()
    }

    fun toggleHadithEditSelection(id: Long) {
        val current = _selectedHadithEditIds.value
        _selectedHadithEditIds.value = if (current.contains(id)) current - id else current + id
    }

    fun selectAllHadithEdits(edits: List<HadithEdit>) {
        _selectedHadithEditIds.value = edits.mapNotNull { it.id }.toSet()
    }

    fun clearHadithSelection() {
        _selectedHadithEditIds.value = emptySet()
    }

    fun setQuranFilter(filter: String) {
        _quranFilter.value = filter
    }

    fun setHadithFilter(filter: String) {
        _hadithFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Redaktor süzgəci siyahını daraldır, ona görə seçim təmizlənir — əks halda toplu təsdiq/silmə
     * artıq ekranda görünməyən sətirlərə də toxunardı.
     */
    fun setEditorFilter(email: String?) {
        if (_editorFilter.value == email) return
        _editorFilter.value = email
        clearQuranSelection()
        clearHadithSelection()
    }

    fun fetchQuranEdits() {
        viewModelScope.launch {
            _quranLoading.value = true
            _quranError.value = null
            try {
                loadQuranEdits()
            } catch (e: Exception) {
                AppLogger.d("EditsVM", "Quran fetch error: ${e.message}")
                _quranError.value = "Quran error: ${e.message}"
            } finally {
                _quranLoading.value = false
            }
        }
    }

    fun fetchHadithEdits() {
        viewModelScope.launch {
            _hadithLoading.value = true
            _hadithError.value = null
            try {
                loadHadithEdits()
            } catch (e: Exception) {
                AppLogger.d("EditsVM", "Hadith fetch error details: ${e.stackTraceToString()}")
                _hadithError.value = "Hadith error: ${e.message}"
            } finally {
                _hadithLoading.value = false
            }
        }
    }

    private suspend fun loadQuranEdits() {
        val result = SupabaseProvider.client.from("quran_edits").select {
            order("created_at", order = Order.DESCENDING)
        }
        val decoded = result.decodeList<QuranEdit>()
        _quranEdits.value = decoded
        AppLogger.d("EditsVM", "Fetched ${decoded.size} quran edits")
    }

    private suspend fun loadHadithEdits() {
        val result = SupabaseProvider.client.from("hadith_edits").select {
            order("created_at", order = Order.DESCENDING)
        }
        val decoded = result.decodeList<HadithEdit>()
        _hadithEdits.value = decoded
        AppLogger.d("EditsVM", "Decoded ${decoded.size} hadith edits")

        if (decoded.isEmpty()) {
            AppLogger.d("EditsVM", "Hadith table returned 0 rows. Check RLS or content.")
        }
    }

    /**
     * RLS bir əməliyyatı bloklayanda PostgREST xəta yox, boş nəticə qaytarır — yəni təsdiq/silmə
     * "uğurlu" görünür, amma heç nə dəyişmir. Ona görə hər əməliyyat `select()` ilə gedir və
     * təsirlənən sətir sayı sıfırdırsa istifadəçiyə bildirilir.
     */
    private suspend fun runQuranAction(
        label: String,
        action: suspend () -> Int,
    ) {
        _quranLoading.value = true
        _quranError.value = null
        try {
            if (action() == 0) notifyBlocked(label)
            loadQuranEdits()
        } catch (e: Exception) {
            AppLogger.d("EditsVM", "$label failed: ${e.message}")
            _quranError.value = "$label failed: ${e.message}"
        } finally {
            _quranLoading.value = false
        }
    }

    private suspend fun runHadithAction(
        label: String,
        action: suspend () -> Int,
    ) {
        _hadithLoading.value = true
        _hadithError.value = null
        try {
            if (action() == 0) notifyBlocked(label)
            loadHadithEdits()
        } catch (e: Exception) {
            AppLogger.d("EditsVM", "$label failed: ${e.message}")
            _hadithError.value = "$label failed: ${e.message}"
        } finally {
            _hadithLoading.value = false
        }
    }

    private suspend fun notifyBlocked(label: String) {
        AppLogger.d("EditsVM", "$label affected 0 rows — blocked by RLS or already gone")
        PlatformUtils.showLongToast(getString(Res.string.strMsgEditActionBlocked))
    }

    fun approveQuranEdit(edit: QuranEdit) {
        val id = edit.id ?: return
        viewModelScope.launch {
            runQuranAction("Approve") {
                SupabaseProvider.client.from("quran_edits").update(
                    mapOf("is_approved" to true)
                ) {
                    select()
                    filter { eq("id", id) }
                }.decodeList<JsonObject>().size
            }
        }
    }

    fun deleteQuranEdit(edit: QuranEdit) {
        val id = edit.id ?: return
        viewModelScope.launch {
            runQuranAction("Delete") {
                SupabaseProvider.client.from("quran_edits").delete {
                    select()
                    filter { eq("id", id) }
                }.decodeList<JsonObject>().size
            }
        }
    }

    fun updateHadithStatus(edit: HadithEdit, status: String) {
        val id = edit.id ?: return
        viewModelScope.launch {
            runHadithAction("Status update") {
                SupabaseProvider.client.from("hadith_edits").update(
                    mapOf("status" to status)
                ) {
                    select()
                    filter { eq("id", id) }
                }.decodeList<JsonObject>().size
            }
        }
    }

    fun deleteHadithEdit(edit: HadithEdit) {
        val id = edit.id ?: return
        viewModelScope.launch {
            runHadithAction("Delete") {
                SupabaseProvider.client.from("hadith_edits").delete {
                    select()
                    filter { eq("id", id) }
                }.decodeList<JsonObject>().size
            }
        }
    }

    fun approveSelectedQuranEdits() {
        val ids = _selectedQuranEditIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runQuranAction("Bulk approve") {
                val affected = SupabaseProvider.client.from("quran_edits").update(
                    mapOf("is_approved" to true)
                ) {
                    select()
                    filter { isIn("id", ids.toList()) }
                }.decodeList<JsonObject>().size
                clearQuranSelection()
                affected
            }
        }
    }

    fun deleteSelectedQuranEdits() {
        val ids = _selectedQuranEditIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runQuranAction("Bulk delete") {
                val affected = SupabaseProvider.client.from("quran_edits").delete {
                    select()
                    filter { isIn("id", ids.toList()) }
                }.decodeList<JsonObject>().size
                clearQuranSelection()
                affected
            }
        }
    }

    fun updateSelectedHadithStatus(status: String) {
        val ids = _selectedHadithEditIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runHadithAction("Bulk status update") {
                val affected = SupabaseProvider.client.from("hadith_edits").update(
                    mapOf("status" to status)
                ) {
                    select()
                    filter { isIn("id", ids.toList()) }
                }.decodeList<JsonObject>().size
                clearHadithSelection()
                affected
            }
        }
    }

    fun deleteSelectedHadithEdits() {
        val ids = _selectedHadithEditIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runHadithAction("Bulk delete") {
                val affected = SupabaseProvider.client.from("hadith_edits").delete {
                    select()
                    filter { isIn("id", ids.toList()) }
                }.decodeList<JsonObject>().size
                clearHadithSelection()
                affected
            }
        }
    }
}
