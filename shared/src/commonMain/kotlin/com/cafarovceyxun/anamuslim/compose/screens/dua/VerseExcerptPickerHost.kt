package com.cafarovceyxun.anamuslim.compose.screens.dua

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.cafarovceyxun.anamuslim.utils.supabase.DuaSourceType
import com.cafarovceyxun.anamuslim.utils.verse.DailyContentFactory

/**
 * Ayədən seçim ekranını açır — mətnləri əvvəlcə **cihazdakı** bazadan qurur.
 *
 * Hədisdən fərqli olaraq ayənin mətni əlində hazır olmur: ərəbcəsi söz-söz cədvəldən yığılır,
 * tərcümə isə istifadəçinin seçdiyi kitabdandır. Hər ikisini `DailyContentFactory` onsuz da bir
 * yerdə qurur (günün ayəsi növbəsi eyni funksiyadan keçir), ona görə burada təkrarlanmır.
 *
 * Mətn hazır olana qədər **heç nə göstərilmir**: yarımçıq mətnlə açılan seçim ekranında istifadəçi
 * boş sahə görüb səhv seçim edərdi. Qurula bilməyən ayə də (baza natamam) sadəcə açılmır — çağıran
 * tərəf vəziyyəti sıfırlayır.
 */
@Composable
fun VerseExcerptPickerHost(
    chapterNo: Int,
    verseNo: Int,
    /** `true` → Əsmaül Hüsnə dəlili, `false` → dua. */
    isEvidence: Boolean,
    onClose: () -> Unit,
) {
    val data by produceState<ExcerptSourceData?>(null, chapterNo, verseNo) {
        val content = DailyContentFactory.verseContent(chapterNo, verseNo, verseEnd = null)

        value = content?.let {
            ExcerptSourceData(
                sourceType = DuaSourceType.QURAN,
                chapterNo = chapterNo,
                verseNo = verseNo,
                fullArabic = it.text_ar,
                fullTranslation = it.text_az,
                reference = it.source,
            )
        }
    }

    val source = data ?: return

    if (isEvidence) {
        AsmaExcerptPicker(data = source, onClose = onClose)
    } else {
        DuaExcerptPicker(data = source, onClose = onClose)
    }
}
