package com.cafarovceyxun.anamuslim.utils.dua

import com.cafarovceyxun.anamuslim.db.entities.hadith.toModel
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.repository.loadHadithLocation
import com.cafarovceyxun.anamuslim.utils.supabase.DuaSourceRef
import com.cafarovceyxun.anamuslim.utils.supabase.Hadith
import com.cafarovceyxun.anamuslim.utils.verse.DailyContentFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * Bir duanın/dəlilin mənbəyi — **tam** mətn, çıxarışın özü deyil.
 *
 * Qaynaq vərəqi bunu göstərir və içindəki çıxarışı sarı ilə işarələyir; ona görə burada mətnlər
 * kəsilmədən gəlir.
 */
data class DuaSourceContent(
    val arabic: String,
    val translation: String,
    /** «Buxari · Namaz babı · №12» kimi göstərilən istinad sətri. */
    val reference: String,
    /** Hədis mənbəyində sətrin özü — «Hədisi aç» düyməsi buradan id və nömrə götürür. */
    val hadith: Hadith? = null,
    val note: String? = null,
)

/**
 * Mənbəni **cihazdan** qurur: hədis yerli Room bazasından, ayə isə istifadəçinin öz tərcüməsi ilə
 * yenidən yığılır ([DailyContentFactory.verseContent]).
 *
 * Şəbəkəyə çıxmır — dua sətri onsuz da mətnin öz nüsxəsini daşıyır, burada göstərilən isə
 * **kontekstdir**: çıxarışın hansı hədisin/ayənin içindən götürüldüyü. Mənbə tapılmasa null qayıdır
 * və vərəq «qaynaq yüklənmədi» deyir (hədis silinib, və ya hədis bazası hələ endirilməyib).
 */
suspend fun loadDuaSource(ref: DuaSourceRef): DuaSourceContent? = withContext(Dispatchers.IO) {
    if (ref.isHadith) {
        val id = ref.hadith_id ?: return@withContext null
        val hadith = RepositoryProvider.hadithDatabase.hadithDao().getHadithById(id)?.toModel()
            ?: return@withContext null

        val location = loadHadithLocation(hadith)
        val parts = listOfNotNull(
            location.volume?.name,
            location.chapter?.name,
            hadith.source?.takeIf { it.isNotBlank() },
        )

        return@withContext DuaSourceContent(
            arabic = hadith.text_ar,
            translation = hadith.text_az,
            reference = parts.joinToString(" · "),
            hadith = hadith,
            note = hadith.note?.takeIf { it.isNotBlank() },
        )
    }

    val chapterNo = ref.chapter_no ?: return@withContext null
    val numbers = ref.verseNumbers
    if (numbers.isEmpty()) return@withContext null

    val content = DailyContentFactory.verseContent(chapterNo, numbers.first(), numbers.last())
        ?: return@withContext null

    DuaSourceContent(
        arabic = content.text_ar,
        translation = content.text_az,
        reference = content.source.orEmpty(),
    )
}
