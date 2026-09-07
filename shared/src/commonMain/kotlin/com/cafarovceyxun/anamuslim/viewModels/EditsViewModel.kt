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
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable
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

    /**
     * Kartda fərqi göstərmək üçün **hazırkı** mətn — açar düzəlişin `id`-sidir.
     *
     * Nə `quran_edits`, nə `hadith_edits` köhnə mətni saxlamır (bax `docs/supabase/SCHEMA.md`),
     * ona görə əsas cədvəllərdən ayrıca çəkilir. Yerli SQLite nüsxəsindən oxumaq **olmaz**: admin
     * özü ayəni redaktə edəndə `ReaderProviderViewModel.saveTranslation` yerli nüsxəni dərhal
     * üzərinə yazır — «hazırkı mətn» elə təklifin özü olar və fərq boş görünərdi.
     *
     * Xəritədə olmayan düzəliş üçün fərq göstərilmir (yeni hədis təklifi, şəbəkə xətası) — kart
     * yalnız təklif olunan mətnlə açılır.
     */
    private val _quranBaseTexts = MutableStateFlow<Map<Long, QuranBaseText>>(emptyMap())
    val quranBaseTexts = _quranBaseTexts.asStateFlow()

    private val _hadithBaseTexts = MutableStateFlow<Map<Long, HadithBaseText>>(emptyMap())
    val hadithBaseTexts = _hadithBaseTexts.asStateFlow()

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
        loadQuranBaseTexts(decoded)
    }

    /**
     * Düzəlişlərin göstərdiyi ayələrin **əsas** mətni. Sorğu `translations` view-una yox,
     * `quran_translations_data` cədvəlinə gedir: view redaktora öz təsdiqlənməmiş düzəlişini
     * qaytarır (`coalesce`), yəni admin öz təklifini «hazırkı mətn» kimi görərdi.
     */
    private suspend fun loadQuranBaseTexts(edits: List<QuranEdit>) {
        if (edits.isEmpty()) {
            _quranBaseTexts.value = emptyMap()
            return
        }
        try {
            val byTranslationId = HashMap<Long, QuranBaseText>()
            edits.mapNotNull { it.translation_id }.distinct().chunked(BASE_TEXT_CHUNK).forEach { chunk ->
                SupabaseProvider.client.from(TABLE_QURAN_DATA)
                    .select(Columns.list("id", "chapter_no", "verse_no", "text", "note")) {
                        filter { isIn("id", chunk) }
                    }
                    .decodeList<QuranBaseRow>()
                    .forEach { row -> row.id?.let { byTranslationId[it] = row.toBaseText() } }
            }

            // `translation_id` yalnız edits_hardening miqrasiyasından sonrakı sətirlərdə dolur;
            // ondan əvvəlkilərin yeganə ünvanı (surə, ayə) cütüdür.
            val legacy = edits.filter { it.translation_id == null && it.chapter_no != null && it.verse_no != null }
            val byVerse = HashMap<Pair<Long, Long>, QuranBaseText>()
            legacy.mapNotNull { it.chapter_no }.distinct().chunked(BASE_TEXT_CHUNK).forEach { chunk ->
                SupabaseProvider.client.from(TABLE_QURAN_DATA)
                    .select(Columns.list("id", "chapter_no", "verse_no", "text", "note")) {
                        filter {
                            eq("slug", QURAN_BASE_SLUG)
                            isIn("chapter_no", chunk)
                        }
                    }
                    .decodeList<QuranBaseRow>()
                    .forEach { row -> byVerse[row.chapter_no to row.verse_no] = row.toBaseText() }
            }

            _quranBaseTexts.value = buildMap {
                edits.forEach { edit ->
                    val editId = edit.id ?: return@forEach
                    val base = edit.translation_id?.let { byTranslationId[it] }
                        ?: edit.chapter_no?.let { c -> edit.verse_no?.let { v -> byVerse[c to v] } }
                    if (base != null) put(editId, base)
                }
            }
        } catch (e: Exception) {
            // Fərq köməkçi funksiyadır — alınmasa siyahı yenə açılmalıdır.
            AppLogger.d("EditsVM", "Quran base text fetch failed: ${e.message}")
            _quranBaseTexts.value = emptyMap()
        }
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
        loadHadithBaseTexts(decoded)
    }

    /** `hadith_id` null olan sətir **yeni hədis təklifidir** — müqayisə ediləcək əsas mətn yoxdur. */
    private suspend fun loadHadithBaseTexts(edits: List<HadithEdit>) {
        val ids = edits.mapNotNull { it.hadith_id }.distinct()
        if (ids.isEmpty()) {
            _hadithBaseTexts.value = emptyMap()
            return
        }
        try {
            val byId = HashMap<Long, HadithBaseText>()
            ids.chunked(BASE_TEXT_CHUNK).forEach { chunk ->
                SupabaseProvider.client.from(TABLE_HADITH)
                    .select(Columns.list("id", "text_ar", "text_az")) {
                        filter { isIn("id", chunk) }
                    }
                    .decodeList<HadithBaseRow>()
                    .forEach { row -> row.id?.let { byId[it] = HadithBaseText(row.text_ar, row.text_az) } }
            }

            _hadithBaseTexts.value = buildMap {
                edits.forEach { edit ->
                    val editId = edit.id ?: return@forEach
                    val base = edit.hadith_id?.let { byId[it] } ?: return@forEach
                    put(editId, base)
                }
            }
        } catch (e: Exception) {
            AppLogger.d("EditsVM", "Hadith base text fetch failed: ${e.message}")
            _hadithBaseTexts.value = emptyMap()
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

/** Moderasiya kartında göstərilən «hazırkı» Quran tərcüməsi. */
data class QuranBaseText(val text: String, val note: String?)

/** Moderasiya kartında göstərilən «hazırkı» hədis mətni. */
data class HadithBaseText(val textAr: String?, val textAz: String?)

@Serializable
private data class QuranBaseRow(
    val id: Long? = null,
    val chapter_no: Long = 0,
    val verse_no: Long = 0,
    val text: String? = null,
    val note: String? = null,
) {
    fun toBaseText() = QuranBaseText(text.orEmpty(), note)
}

@Serializable
private data class HadithBaseRow(
    val id: Long? = null,
    val text_ar: String? = null,
    val text_az: String? = null,
)

private const val TABLE_QURAN_DATA = "quran_translations_data"
private const val TABLE_HADITH = "hadith"
private const val QURAN_BASE_SLUG = "az"

/** PostgREST `in` süzgəci URL-də gedir — bir sorğuya çox id yığmaq sorğunu uzadır. */
private const val BASE_TEXT_CHUNK = 200
