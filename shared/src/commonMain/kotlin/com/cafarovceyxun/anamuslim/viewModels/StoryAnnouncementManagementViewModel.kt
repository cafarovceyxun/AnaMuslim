package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.repository.supabase.StoryAnnouncementRepository
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.storyAnnouncementPublishFailed
import com.cafarovceyxun.anamuslim.resources.storyAnnouncementPublished
import com.cafarovceyxun.anamuslim.resources.suggestionsImageFailed
import com.cafarovceyxun.anamuslim.utils.app.PickedMedia
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.supabase.IsoInstant
import com.cafarovceyxun.anamuslim.utils.supabase.StoryAnnouncement
import com.cafarovceyxun.anamuslim.utils.supabase.StoryDuration
import com.cafarovceyxun.anamuslim.utils.supabase.StoryMediaStorage
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionMedia
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionMediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/**
 * Müstəqil hekayələrin («Elanlar») idarəetmə paneli.
 *
 * Yeni hekayə **qaralama** kimi yığılır: hər seçilən media dərhal yüklənir (video bir neçə saniyə
 * çəkir — «Paylaş»-da hamısını gözləmək əvəzinə), sətir isə «Paylaş»-da bir dəfə yaradılır. Belə
 * olanda bazada yarımçıq hekayə heç vaxt görünmür. Paylaşılmayan qaralamanın faylları
 * [discardDraft]-da və ya ekrandan çıxanda ([onCleared]) silinir.
 */
class StoryAnnouncementManagementViewModel : ViewModel() {
    private val repository = StoryAnnouncementRepository()

    private val _stories = MutableStateFlow<List<StoryAnnouncement>>(emptyList())
    val stories: StateFlow<List<StoryAnnouncement>> = _stories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _draftMedia = MutableStateFlow<List<SuggestionMedia>>(emptyList())
    val draftMedia: StateFlow<List<SuggestionMedia>> = _draftMedia.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _stories.value = repository.fetch()
            _isLoading.value = false
        }
    }

    fun addDraftMedia(picked: PickedMedia) {
        viewModelScope.launch {
            _isUploading.value = true

            StoryMediaStorage.upload(picked.bytes, picked.mimeType)
                .onSuccess { url ->
                    val type = if (picked.isVideo) SuggestionMediaType.VIDEO else SuggestionMediaType.IMAGE
                    _draftMedia.value = _draftMedia.value + SuggestionMedia(url = url, type = type)
                }
                .onFailure { PlatformUtils.showLongToast(getString(Res.string.suggestionsImageFailed)) }

            _isUploading.value = false
        }
    }

    fun removeDraftMedia(item: SuggestionMedia) {
        _draftMedia.value = _draftMedia.value.filterNot { it.url == item.url }
        viewModelScope.launch { StoryMediaStorage.delete(item.url) }
    }

    fun discardDraft() {
        val media = _draftMedia.value
        _draftMedia.value = emptyList()
        viewModelScope.launch { media.forEach { StoryMediaStorage.delete(it.url) } }
    }

    /**
     * Sətri yaradır. RLS bloklayanda PostgREST **boş nəticə** qaytarır, xəta yox — uğur
     * qaytarılan sətirlə ölçülür (CLAUDE.md).
     */
    fun publish(note: String?, duration: StoryDuration, onSuccess: () -> Unit) {
        val media = _draftMedia.value
        if (media.isEmpty() && note.isNullOrBlank()) return

        viewModelScope.launch {
            _isLoading.value = true

            val expiresAt = duration.hours?.let { hours ->
                IsoInstant.fromEpochMillis(currentEpochMillis() + hours * 3_600_000L)
            }
            val created = runCatching {
                repository.create(note = note?.takeIf { it.isNotBlank() }, media = media, expiresAt = expiresAt)
            }.getOrNull()

            if (created == null) {
                _isLoading.value = false
                PlatformUtils.showLongToast(getString(Res.string.storyAnnouncementPublishFailed))
                return@launch
            }

            // Fayllar artıq sətrə bağlıdır — qaralama boşalır, amma fayllar silinmir.
            _draftMedia.value = emptyList()
            _stories.value = listOf(created) + _stories.value.filterNot { it.id == created.id }
            _isLoading.value = false
            PlatformUtils.showLongToast(getString(Res.string.storyAnnouncementPublished))
            onSuccess()
        }
    }

    /** Əvvəl sətir, sonra fayllar — tərsi RLS-ə ilişsəydi hekayədə ölü link qalardı. */
    fun delete(story: StoryAnnouncement) {
        viewModelScope.launch {
            if (repository.delete(story.id) == 0) return@launch

            _stories.value = _stories.value.filterNot { it.id == story.id }
            story.media.forEach { StoryMediaStorage.delete(it.url) }
        }
    }

    override fun onCleared() {
        // `viewModelScope` artıq ləğv olunub — paylaşılmamış qaralama faylları ayrıca scope-da
        // silinir, yoxsa bucket-də heç nəyə bağlı olmayan fayllar qalardı.
        val orphans = _draftMedia.value
        if (orphans.isNotEmpty()) {
            CoroutineScope(Dispatchers.Default).launch {
                orphans.forEach { StoryMediaStorage.delete(it.url) }
            }
        }
        super.onCleared()
    }
}
