package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.repository.supabase.DailyContentRepository
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strMsgDailyContentQueued
import com.cafarovceyxun.anamuslim.resources.strMsgDailyContentSetFailed
import com.cafarovceyxun.anamuslim.utils.IsoDate
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.currentLocalDateIsoString
import com.cafarovceyxun.anamuslim.utils.epochMillisAtLocalTime
import com.cafarovceyxun.anamuslim.utils.supabase.DailyContent
import com.cafarovceyxun.anamuslim.utils.verse.DailyContentFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/**
 * Bugünkü günün ayəsi/hədisi elementləri — ana səhifə kartı, story və oxucudakı «bu günün ayəsidir»
 * nişanı bunu oxuyur.
 *
 * Gündə [com.cafarovceyxun.anamuslim.utils.supabase.DailyContentSlots.COUNT] element ola bilər, ona
 * görə tək sətir yox, **siyahı**. Növbəni idarə etmək (əlavə, sıralama, silmə) admin panelinin
 * işidir — bax [DailyContentManagementViewModel].
 */
class DailyContentViewModel : ViewModel() {
    private val repository = DailyContentRepository()

    private val _todayItems = MutableStateFlow<List<DailyContent>>(emptyList())
    val todayItems: StateFlow<List<DailyContent>> = _todayItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        refresh()
        scheduleDayRollover()
    }

    /**
     * Gecə yarısı siyahını özü yeniləyir.
     *
     * Lazımdır, çünki [todayItems] «bugünkü» sətirlərdir, amma tətbiq açıq qalsa heç nə onu
     * yenidən oxumur: 00:00-dan sonra ana səhifə **dünənin** hekayəsini göstərməyə davam edərdi.
     * Sistem hadisəsi yoxdur — sadəcə növbəti yerli gün başlanğıcına qədər gözlənilir.
     */
    private fun scheduleDayRollover() {
        viewModelScope.launch {
            while (true) {
                val today = currentLocalDateIsoString()
                val tomorrow = IsoDate.plusDays(today, 1) ?: return@launch
                val midnight = epochMillisAtLocalTime(tomorrow, 0, 0) ?: return@launch

                // Ən azı bir dəqiqə: saat sərhədində sıfır gecikmə ilə dövrə fırlanmasın.
                delay((midnight - currentEpochMillis()).coerceAtLeast(60_000L))
                refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _todayItems.value = repository.fetchTodayItems()
            _isLoading.value = false
        }
    }

    /**
     * Elementi növbənin sonuna əlavə edir — boş olan ilk gələcək yuvaya. Oxucudakı və hədis
     * ekranındakı «günün ayəsi» düymələri bunu çağırır.
     */
    fun enqueue(content: DailyContent, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true

            repository.enqueue(content)
                .onSuccess {
                    _todayItems.value = repository.fetchTodayItems()
                    PlatformUtils.showToast(getString(Res.string.strMsgDailyContentQueued))
                    onSuccess()
                }
                .onFailure {
                    // `daily_content_item`-ə yazmaq yalnız adminə açıqdır — RLS və ya şəbəkə
                    // səbəbi ilə alınmadıqda əməliyyat səssiz qalmasın.
                    PlatformUtils.showToast(getString(Res.string.strMsgDailyContentSetFailed))
                }

            _isLoading.value = false
        }
    }

    /**
     * Ayəni (və ya aralığı) növbəyə salır — mətnlər cihazdakı bazadan qurulur, ona görə aralığın
     * **bütün** ayələri düşür, təkcə birincisi yox.
     */
    fun enqueueVerses(chapterNo: Int, verseStart: Int, verseEnd: Int?) {
        viewModelScope.launch {
            val content = DailyContentFactory.verseContent(chapterNo, verseStart, verseEnd)

            if (content == null) {
                PlatformUtils.showToast(getString(Res.string.strMsgDailyContentSetFailed))
                return@launch
            }

            enqueue(content)
        }
    }
}

/** Bu ayə bugünkü elementlərdən birinə düşürmü — oxucudakı nişan üçün. */
fun List<DailyContent>.containsVerse(chapterNo: Int, verseNo: Int): Boolean = any { item ->
    !item.isHadith && item.chapter_no == chapterNo && verseNo in item.verseNumbers
}

/** Bu hədis bugünkü elementlərdən birinə düşürmü. */
fun List<DailyContent>.containsHadith(hadithId: Long?): Boolean =
    hadithId != null && any { it.isHadith && it.hadith_id == hadithId }
