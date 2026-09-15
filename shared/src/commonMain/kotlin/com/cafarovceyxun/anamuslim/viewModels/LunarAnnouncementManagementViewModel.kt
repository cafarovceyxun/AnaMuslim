package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.repository.supabase.LunarAnnouncementRepository
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.lunarPruned
import com.cafarovceyxun.anamuslim.resources.lunarPublishFailed
import com.cafarovceyxun.anamuslim.resources.lunarPublished
import com.cafarovceyxun.anamuslim.utils.app.PickedMedia
import com.cafarovceyxun.anamuslim.utils.supabase.LunarAnnouncement
import com.cafarovceyxun.anamuslim.utils.supabase.LunarMediaStorage
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionMedia
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionMediaType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/**
 * Qəməri ay elanlarının idarəetmə paneli.
 *
 * Yayımdan sonra iki şey **dərhal** olur: (1) cihazın öz düzəlişi yenilənir
 * ([LunarAnnouncementSync]) ki, admin nəticəni elə orada görsün; (2) 12 aydan köhnə elanlar
 * fayllarıyla birlikdə silinir — ayrıca cron əvəzinə təmizləmə elanın ritmində gedir.
 */
class LunarAnnouncementManagementViewModel : ViewModel() {
    private val repository = LunarAnnouncementRepository()

    private val _announcements = MutableStateFlow<List<LunarAnnouncement>>(emptyList())
    val announcements: StateFlow<List<LunarAnnouncement>> = _announcements.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Hansı elana media yüklənir — kart üzərindəki sayğac bunu göstərir. */
    private val _uploadingFor = MutableStateFlow<Long?>(null)
    val uploadingFor: StateFlow<Long?> = _uploadingFor.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _announcements.value = repository.fetchRecent()
            _isLoading.value = false
        }
    }

    /**
     * Elanı yazır (eyni il+ay varsa üzərinə), sonra sinxron və təmizləmə.
     *
     * RLS bloklayanda PostgREST **boş nəticə** qaytarır, xəta yox — ona görə uğur sətir sayı ilə
     * ölçülür (CLAUDE.md).
     */
    fun publish(
        hijriYear: Int,
        hijriMonth: Int,
        startDate: String,
        lengthDays: Int,
        sightedAt: String?,
        note: String?,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            val written = runCatching {
                repository.upsert(
                    hijriYear = hijriYear,
                    hijriMonth = hijriMonth,
                    startDate = startDate,
                    lengthDays = lengthDays,
                    sightedAt = sightedAt,
                    // Media elanın özündən ayrı idarə olunur — bax [LunarAnnouncementRepository.updateMedia].
                    // Burada boş göndərsəydik mövcud elanın videosu redaktədə silinərdi.
                    media = currentMediaOf(hijriYear, hijriMonth),
                    note = note,
                )
            }.getOrDefault(0)

            if (written == 0) {
                _isLoading.value = false
                PlatformUtils.showLongToast(getString(Res.string.lunarPublishFailed))
                return@launch
            }

            val items = repository.fetchRecent()
            _announcements.value = items
            LunarAnnouncementSync.apply(items)

            PlatformUtils.showLongToast(getString(Res.string.lunarPublished))
            onSuccess()

            _isLoading.value = false
            pruneOld()
        }
    }

    fun addMedia(announcement: LunarAnnouncement, picked: PickedMedia) {
        viewModelScope.launch {
            _uploadingFor.value = announcement.id

            LunarMediaStorage.upload(picked.bytes, picked.mimeType).onSuccess { url ->
                val type = if (picked.isVideo) SuggestionMediaType.VIDEO else SuggestionMediaType.IMAGE
                val updated = announcement.media + SuggestionMedia(url = url, type = type)

                if (repository.updateMedia(announcement.id, updated) > 0) {
                    replace(announcement.copy(media = updated))
                }
            }

            _uploadingFor.value = null
        }
    }

    /**
     * Mediaya əvvəl **sətirdə** əl gəzdirilir, sonra fayl silinir.
     *
     * Sıra vacibdir: faylı əvvəl silsəydik və sətir yenilənməsi RLS-ə ilişsəydi, hekayədə ölü link
     * qalardı — istifadəçi qara slayd görərdi.
     */
    fun removeMedia(announcement: LunarAnnouncement, item: SuggestionMedia) {
        viewModelScope.launch {
            val updated = announcement.media.filterNot { it.url == item.url }

            if (repository.updateMedia(announcement.id, updated) > 0) {
                replace(announcement.copy(media = updated))
                LunarMediaStorage.delete(item.url)
            }
        }
    }

    fun delete(announcement: LunarAnnouncement) {
        viewModelScope.launch {
            if (repository.delete(announcement.id) == 0) return@launch

            // Sətir gedəndən sonra fayllar heç nəyə bağlı deyil — 12 aylıq təmizləmə də onları
            // artıq tapa bilməzdi (o, mövcud sətirlərin media siyahısından gedir).
            announcement.media.forEach { LunarMediaStorage.delete(it.url) }

            val items = repository.fetchRecent()
            _announcements.value = items
            LunarAnnouncementSync.apply(items)
        }
    }

    private suspend fun pruneOld() {
        repository.prune().onSuccess { removed ->
            if (removed > 0) {
                _announcements.value = repository.fetchRecent()
                PlatformUtils.showLongToast(getString(Res.string.lunarPruned, removed))
            }
        }
    }

    private fun currentMediaOf(hijriYear: Int, hijriMonth: Int): List<SuggestionMedia> =
        _announcements.value
            .firstOrNull { it.hijri_year == hijriYear && it.hijri_month == hijriMonth }
            ?.media
            .orEmpty()

    private fun replace(announcement: LunarAnnouncement) {
        _announcements.value = _announcements.value.map {
            if (it.id == announcement.id) announcement else it
        }
    }
}
