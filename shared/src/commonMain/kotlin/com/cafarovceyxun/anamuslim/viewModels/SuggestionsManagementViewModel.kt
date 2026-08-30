package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.repository.supabase.SuggestionRepository
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strMsgEditActionBlocked
import com.cafarovceyxun.anamuslim.resources.suggestionsImageFailed
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.app.PickedImage
import com.cafarovceyxun.anamuslim.utils.app.RemoteImageLoader
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionImageStorage
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

    fun reject(row: SuggestionSubmissionRow) =
        setSubmissionStatus(row, SuggestionSubmissionStatus.REJECTED)

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

    fun deleteSubmission(row: SuggestionSubmissionRow) {
        runAction("Delete") { repository.deleteSubmission(row.id) }
    }

    fun setPublishedStatus(suggestion: Suggestion, status: String) {
        runAction("Public status") { repository.updatePublicStatus(suggestion.id, status) }
    }

    fun deletePublished(suggestion: Suggestion) {
        runAction("Public delete") { repository.deletePublic(suggestion.id) }
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
     * Şəkli əvvəlcə Storage-a yükləyir, sonra sətrə yazır və **köhnə faylı silir** — əks halda
     * bucket hər dəyişiklikdə bir artıq fayl toplayardı.
     */
    fun setImage(suggestion: Suggestion, image: PickedImage) {
        viewModelScope.launch {
            _uploadingFor.value = suggestion.id
            try {
                SuggestionImageStorage.upload(image.bytes, image.mimeType)
                    .onSuccess { url ->
                        applyAndRefresh("Image") { repository.updatePublicImage(suggestion.id, url) }
                        suggestion.image_url?.takeIf { it != url }?.let {
                            SuggestionImageStorage.delete(it)
                        }
                    }
                    .onFailure { e ->
                        AppLogger.d(TAG, "Image upload failed: ${e.message}")
                        PlatformUtils.showLongToast(getString(Res.string.suggestionsImageFailed))
                    }
            } finally {
                _uploadingFor.value = null
            }
        }
    }

    fun removeImage(suggestion: Suggestion) {
        val url = suggestion.image_url ?: return
        viewModelScope.launch {
            applyAndRefresh("Image remove") { repository.updatePublicImage(suggestion.id, null) }
            SuggestionImageStorage.delete(url)
            RemoteImageLoader.evict(url)
        }
    }

    companion object {
        const val FILTER_ALL = "All"

        private const val TAG = "SuggestionsAdmin"
    }
}
