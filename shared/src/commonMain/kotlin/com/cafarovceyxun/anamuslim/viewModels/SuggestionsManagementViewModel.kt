package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.repository.supabase.SuggestionRepository
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strMsgEditActionBlocked
import com.cafarovceyxun.anamuslim.resources.suggestionsImageFailed
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.app.PickedMedia
import com.cafarovceyxun.anamuslim.utils.app.RemoteImageLoader
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionMedia
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionMediaStorage
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionMediaType
import com.cafarovceyxun.anamuslim.utils.supabase.Suggestion
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionSubmissionRow
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionSubmissionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/**
 * Admin paneli: moderasiya növbəsi (`suggestion_submissions`) və təsdiqlənmiş siyahı
 * (`suggestions`) bir ekranda, iki tab.
 *
 * Təsdiq bazadakı trigger-in işidir — status `approved` olan kimi sətir ictimai cədvələ köçür.
 * Təsdiq geri alınanda həmin ictimai sətir (və onun səsləri) silinir, ona görə «rədd et» geri
 * dönüşü olmayan addım kimi göstərilir.
 */
class SuggestionsManagementViewModel : ViewModel() {

    private val repository = SuggestionRepository()

    private val _submissions = MutableStateFlow<List<SuggestionSubmissionRow>>(emptyList())
    val submissions = _submissions.asStateFlow()

    private val _published = MutableStateFlow<List<Suggestion>>(emptyList())
    val published = _published.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _statusFilter = MutableStateFlow(FILTER_ALL)
    val statusFilter = _statusFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    /** Hansı sətir üçün şəkil yüklənir — kart öz göstəricisini göstərsin deyə id saxlanılır. */
    private val _uploadingFor = MutableStateFlow<Long?>(null)
    val uploadingFor = _uploadingFor.asStateFlow()

    fun setStatusFilter(value: String) {
        _statusFilter.value = value
    }

    fun setSearchQuery(value: String) {
        _searchQuery.value = value
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _submissions.value = repository.fetchSubmissions()
                _published.value = repository.fetchApproved()
            } catch (e: Exception) {
                AppLogger.d(TAG, "Fetch failed: ${e.message}")
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSubmissionStatus(row: SuggestionSubmissionRow, status: String) {
        runAction("Status update") { repository.updateSubmissionStatus(row.id, status) }
    }

    fun approve(row: SuggestionSubmissionRow) =
        setSubmissionStatus(row, SuggestionSubmissionStatus.APPROVED)

    /**
     * Rədd bazadakı trigger vasitəsilə **ictimai sətri də silir** — onunla birlikdə hekayənin
     * faylları bucket-də sahibsiz qalırdı. Media siyahısı silinməmişdən əvvəl götürülür, fayllar
     * isə yalnız sətrin həqiqətən yox olduğu təsdiqlənəndən sonra silinir.
     */
    fun reject(row: SuggestionSubmissionRow) {
        val orphaned = publicRowOf(row)
        viewModelScope.launch {
            applyAndRefresh("Status update") {
                repository.updateSubmissionStatus(row.id, SuggestionSubmissionStatus.REJECTED)
            }
            purgeMediaIfGone(orphaned)
        }
    }

    fun editSubmission(row: SuggestionSubmissionRow, body: String, category: String, note: String?) {
        runAction("Edit") {
            repository.updateSubmissionContent(
                id = row.id,
                body = body.trim(),
                category = category,
                adminNote = note?.trim()?.takeIf { it.isNotEmpty() },
            )
        }
    }

    /** Növbədəki sətrin silinməsi ictimai sətri də aparır (kaskad) — fayllar da getməlidir. */
    fun deleteSubmission(row: SuggestionSubmissionRow) {
        val orphaned = publicRowOf(row)
        viewModelScope.launch {
            applyAndRefresh("Delete") { repository.deleteSubmission(row.id) }
            purgeMediaIfGone(orphaned)
        }
    }

    fun setPublishedStatus(suggestion: Suggestion, status: String) {
        runAction("Public status") { repository.updatePublicStatus(suggestion.id, status) }
    }

    /** Sətir gedəndə hekayənin şəkil/videosu da bucket-dən silinir — sahibsiz fayl qalmasın. */
    fun deletePublished(suggestion: Suggestion) {
        viewModelScope.launch {
            applyAndRefresh("Public delete") { repository.deletePublic(suggestion.id) }
            purgeMediaIfGone(suggestion)
        }
    }

    /**
     * RLS bir əməliyyatı bloklayanda PostgREST xəta yox, boş nəticə qaytarır — yəni əməliyyat
     * "uğurlu" görünür, amma heç nə dəyişmir. Ona görə sıfır sətir istifadəçiyə bildirilir.
     */
    private fun runAction(label: String, action: suspend () -> Int) {
        viewModelScope.launch { applyAndRefresh(label, action) }
    }

    private suspend fun applyAndRefresh(label: String, action: suspend () -> Int) {
        run {
            _isLoading.value = true
            _error.value = null
            try {
                if (action() == 0) {
                    AppLogger.d(TAG, "$label affected 0 rows — blocked by RLS or already gone")
                    PlatformUtils.showLongToast(getString(Res.string.strMsgEditActionBlocked))
                }
                _submissions.value = repository.fetchSubmissions()
                _published.value = repository.fetchApproved()
            } catch (e: Exception) {
                AppLogger.d(TAG, "$label failed: ${e.message}")
                _error.value = "$label failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Faylı əvvəlcə Storage-a yükləyir, sonra sətrin media siyahısına **əlavə edir** — hekayənin
     * bir neçə slaydı ola bilər, ona görə köhnəsi silinmir.
     */
    fun addMedia(suggestion: Suggestion, picked: PickedMedia) {
        viewModelScope.launch {
            _uploadingFor.value = suggestion.id
            try {
                SuggestionMediaStorage.upload(picked.bytes, picked.mimeType)
                    .onSuccess { url ->
                        val item = SuggestionMedia(
                            url = url,
                            type = if (picked.isVideo) SuggestionMediaType.VIDEO else SuggestionMediaType.IMAGE,
                        )
                        applyAndRefresh("Media add") {
                            repository.updatePublicMedia(suggestion.id, suggestion.media + item)
                        }
                    }
                    .onFailure { e ->
                        AppLogger.d(TAG, "Media upload failed: ${e.message}")
                        PlatformUtils.showLongToast(getString(Res.string.suggestionsImageFailed))
                    }
            } finally {
                _uploadingFor.value = null
            }
        }
    }

    /** Hekayənin hədəf platforması və minimum buraxılışı — «kim görsün» ayarı. */
    fun setVisibility(suggestion: Suggestion, platform: String, minAppVersion: String?) {
        runAction("Visibility") {
            repository.updatePublicVisibility(
                id = suggestion.id,
                platform = platform,
                minAppVersion = minAppVersion?.trim()?.takeIf { it.isNotEmpty() },
            )
        }
    }

    /** Hekayədə mətnin üstündə görünən admin qeydi. */
    fun setNote(suggestion: Suggestion, note: String?) {
        runAction("Note") {
            repository.updatePublicNote(suggestion.id, note?.trim()?.takeIf { it.isNotEmpty() })
        }
    }

    /** Bir slaydı götürür: həm sətirdən, həm bucket-dən — istifadə olunmayan fayl qalmasın. */
    fun removeMedia(suggestion: Suggestion, item: SuggestionMedia) {
        viewModelScope.launch {
            applyAndRefresh("Media remove") {
                repository.updatePublicMedia(suggestion.id, suggestion.media - item)
            }
            purgeMedia(listOf(item))
        }
    }

    /** Növbə sətrinin ictimai qarşılığı — təsdiqlənibsə hekayənin mediası oradadır. */
    private fun publicRowOf(row: SuggestionSubmissionRow): Suggestion? =
        _published.value.firstOrNull {
            it.id == row.suggestion_id || it.source_submission_id == row.id
        }

    /**
     * Sətir həqiqətən getdisə mediasını bucket-dən silir.
     *
     * Yoxlama vacibdir: RLS silməni bloklayanda PostgREST xəta yox, boş nəticə qaytarır
     * (CLAUDE.md) — yoxlamasaq hələ də görünən hekayənin faylları silinərdi. [applyAndRefresh]
     * siyahını yenidən oxuduğu üçün sətrin qalıb-qalmadığı burada bilinir.
     */
    private suspend fun purgeMediaIfGone(suggestion: Suggestion?) {
        val row = suggestion ?: return
        if (_published.value.any { it.id == row.id }) return

        purgeMedia(row.media)
    }

    /** Faylı Storage-dan silir və şəkil keşindən çıxarır. Uğursuzluq səssiz keçir. */
    private suspend fun purgeMedia(media: List<SuggestionMedia>) {
        media.forEach { item ->
            SuggestionMediaStorage.delete(item.url)
            RemoteImageLoader.evict(item.url)
        }
    }

    companion object {
        const val FILTER_ALL = "All"

        private const val TAG = "SuggestionsAdmin"
    }
}
