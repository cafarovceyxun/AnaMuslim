package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrayerPreferences
import com.cafarovceyxun.anamuslim.repository.supabase.LunarAnnouncementRepository
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.prayer.LunarCalendar
import com.cafarovceyxun.anamuslim.utils.supabase.LunarAnnouncement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Qəməri ay elanları — hekayə zolağı, paylaşılan təqvim və qlobal tarix düzəlişi bunu oxuyur.
 *
 * Açılanda dərhal **keşdən** doldurulur, sonra şəbəkə cavabı üstünə gəlir: qəməri tarix tətbiqin
 * hər yerində görünür, ona görə boş siyahı ilə bir neçə kadr «düzəlişsiz» tarix göstərmək
 * istəmirik.
 */
class LunarAnnouncementViewModel : ViewModel() {
    private val repository = LunarAnnouncementRepository()

    private val _announcements = MutableStateFlow(repository.cached())
    val announcements: StateFlow<List<LunarAnnouncement>> = _announcements.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val items = repository.fetchRecent()
            _announcements.value = items
            LunarAnnouncementSync.apply(items)
        }
    }

    /**
     * Hekayə **ilk dəfə** açılanda baxış sayğacını artırır və yeni sayı siyahıya yazır ki, rəqəm
     * elə həmin baxışda görünsün. Uğursuzluq səssiz keçir — sayğac hekayəni bloklamamalıdır.
     */
    fun markViewed(id: Long) {
        viewModelScope.launch {
            repository.markViewed(id).onSuccess { count ->
                _announcements.value = _announcements.value.map {
                    if (it.id == id) it.copy(view_count = count) else it
                }
            }
        }
    }
}

/**
 * Elanı cihazın ayarlarına yazan hissə — ViewModel-dən **kənarda**, çünki eyni işi açılış anı
 * (hekayə söndürülmüş olsa da) və admin paneli yayımdan sonra da görməlidir.
 */
object LunarAnnouncementSync {

    /**
     * Cari elanı tapıb gün düzəlişini yazır; elan yoxdursa düzəlişi götürür.
     *
     * Düzəliş hesablana bilməyəndə **heç nə dəyişmir**: yanlış yazılmış elan (məsələn başlanğıc
     * tarixi platformanın ayından beş gündən çox uzaq) təqvimi tamam sürüşdürməkdənsə, tətbiqin
     * son bilinən vəziyyətində qalması yaxşıdır.
     */
    suspend fun apply(announcements: List<LunarAnnouncement>) {
        val current = LunarCalendar.currentAnnouncement(currentEpochMillis(), announcements)

        if (current == null) {
            PrayerPreferences.clearLunarAnnouncement()
            return
        }

        val offset = LunarCalendar.offsetDaysFor(current)

        if (offset == null) {
            AppLogger.d(TAG, "Offset not resolvable for announcement ${current.id}")
            return
        }

        PrayerPreferences.applyLunarAnnouncement(current.id, offset)
    }

    private const val TAG = "LunarAnnouncement"
}
