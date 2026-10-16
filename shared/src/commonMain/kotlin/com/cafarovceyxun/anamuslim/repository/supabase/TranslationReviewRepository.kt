package com.cafarovceyxun.anamuslim.repository.supabase

import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.supabase.QuranEdit
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

/** Yoxlanış təklifi + əsas cədvəldəki hazırkı mətn (vərəqdə fərqi göstərmək üçün). */
data class ReviewSuggestion(
    val edit: QuranEdit,
    val currentText: String?,
    val currentNote: String?,
) {
    val chapterNo: Int get() = edit.chapter_no?.toInt() ?: 0
    val verseNo: Int get() = edit.verse_no?.toInt() ?: 0
}

enum class ReviewDecisionResult {
    Done,

    /** RLS yazmanı bloklayıb (boş cavab) — təsdiq/rədd yalnız admin-ə açıqdır. */
    NotAllowed,
    Failed,
}

enum class ReviewSaveResult {
    /** Yoxlanış təklifinin özü dəyişdi (admin). */
    SuggestionUpdated,

    /** Təklif yalnız admin-ə yazılır — redaktorun mətni onun öz düzəlişi kimi növbəyə düşdü. */
    QueuedAsOwnEdit,
    Failed,
}

/**
 * Tərcümə yoxlanışının (2026-10-14) təklifləri — `quran_edits`-də `editor_email = [EDITOR]` olan
 * **gözləyən** düzəlişlər. Admin onları adi düzəlişlər kimi Ayarlar → Düzəlişləri İdarə Et-də təsdiq
 * edir; təsdiqlənən sətir avtomatik buradan da, ikinci tərcümədən də çıxır.
 *
 * İkinci tərcümə kimi göstərilən [BOOK_SLUG] kitabı eyni sətirlərdən gəlir: `translations` view-unun
 * `text_review`/`note_review` sütunları (bax `docs/supabase/SCHEMA.md`). Oxu giriş etmiş hər kəsə
 * açıqdır, yazma isə yalnız admin-ə (RLS) — ona görə [updateText] redaktor üçün mətni onun **öz**
 * düzəlişi kimi göndərir.
 */
object TranslationReviewRepository {

    const val EDITOR = "translation-audit"
    const val BOOK_SLUG = "az-review"

    private const val PAGE = 1000
    private const val BASE_CHUNK = 150

    /** Bütün gözləyən təkliflər, ayə sırası ilə. PostgREST bir cavabda ≤1000 sətir verir — səhifələnir. */
    suspend fun fetchAll(): List<ReviewSuggestion> {
        val edits = mutableListOf<QuranEdit>()
        var offset = 0L
        while (true) {
            val page = SupabaseProvider.client.from("quran_edits").select {
                filter {
                    eq("editor_email", EDITOR)
                    eq("is_approved", false)
                }
                order("chapter_no", Order.ASCENDING)
                order("verse_no", Order.ASCENDING)
                range(offset, offset + PAGE - 1)
            }.decodeList<QuranEdit>()
            edits += page
            if (page.size < PAGE) break
            offset += PAGE
        }
        val base = baseTexts(edits.mapNotNull { it.translation_id }.distinct())
        return edits.map { e ->
            val b = e.translation_id?.let(base::get)
            ReviewSuggestion(e, b?.text, b?.note)
        }
    }

    @Serializable
    private data class BaseRow(val id: Long, val text: String, val note: String? = null)

    /**
     * Hazırkı mətn **cədvəldən** oxunur, `translations` view-undan yox: view redaktora öz gözləyən
     * düzəlişini qaytarır, yəni fərq yanlış mətnlə hesablanardı (eyni səbəb `EditsViewModel`-dədir).
     */
    private suspend fun baseTexts(ids: List<Long>): Map<Long, BaseRow> {
        val out = HashMap<Long, BaseRow>()
        ids.chunked(BASE_CHUNK).forEach { chunk ->
            try {
                SupabaseProvider.client.from("quran_translations_data")
                    .select(Columns.list("id", "text", "note")) { filter { isIn("id", chunk) } }
                    .decodeList<BaseRow>()
                    .forEach { out[it.id] = it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Fərq göstərilməsə də təklif özü görünür — kart yalnız təklif mətni ilə açılır.
                AppLogger.d("TranslReview", "Base text fetch failed: ${e.message}")
            }
        }
        return out
    }

    /**
     * Ayənin yoxlanış təklifini [newText] ilə əvəzləyir. RLS yazmanı yalnız admin-ə buraxır və
     * bloklayanda xəta yox, **boş nəticə** qaytarır — ona görə təsirlənən sətir sayına baxılır və
     * redaktorun mətni adi yolla (`translations` view → `intercept_quran_update`) öz düzəlişi kimi
     * göndərilir.
     */
    suspend fun updateText(chapterNo: Int, verseNo: Int, newText: String): ReviewSaveResult = try {
        val affected = SupabaseProvider.client.from("quran_edits").update(
            mapOf("new_text" to newText)
        ) {
            select()
            filter {
                eq("editor_email", EDITOR)
                eq("is_approved", false)
                eq("chapter_no", chapterNo)
                eq("verse_no", verseNo)
            }
        }.decodeList<QuranEdit>().size

        if (affected > 0) {
            ReviewSaveResult.SuggestionUpdated
        } else {
            SupabaseProvider.client.from("translations").update(mapOf("text" to newText)) {
                filter {
                    eq("slug", "az")
                    eq("chapter_no", chapterNo)
                    eq("verse_no", verseNo)
                }
            }
            ReviewSaveResult.QueuedAsOwnEdit
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppLogger.saveError(e, "TranslationReview.updateText")
        ReviewSaveResult.Failed
    }

    /**
     * Təklifi təsdiqləyir — `quran_edits`-in təsdiq trigger-i mətni əsas cədvələ köçürür. Adi
     * düzəlişlərlə eyni yol (`EditsViewModel.approveQuranEdit`); RLS bloklayanda cavab boş gəlir.
     */
    suspend fun approve(editId: Long): ReviewDecisionResult = decide("approve") {
        SupabaseProvider.client.from("quran_edits").update(mapOf("is_approved" to true)) {
            select()
            filter {
                eq("id", editId)
                eq("is_approved", false)
            }
        }.decodeList<QuranEdit>().size
    }

    /** Təklifi rədd edir. `quran_edits`-də «rədd» statusu yoxdur — sətir silinir. */
    suspend fun reject(editId: Long): ReviewDecisionResult = decide("reject") {
        SupabaseProvider.client.from("quran_edits").delete {
            select()
            filter {
                eq("id", editId)
                eq("is_approved", false)
            }
        }.decodeList<QuranEdit>().size
    }

    private suspend fun decide(what: String, block: suspend () -> Int): ReviewDecisionResult = try {
        if (block() > 0) ReviewDecisionResult.Done else ReviewDecisionResult.NotAllowed
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppLogger.saveError(e, "TranslationReview.$what")
        ReviewDecisionResult.Failed
    }
}

/**
 * Yoxlanış səbəbinin növü — `reason`-un ilk «• növ:» etiketindən. Böyük hərflə yazılan etiketlər
 * (MƏNA, STRUKTUR, BURAXILMA…) yoxlanışın özünün «ciddi» saydığı təkliflərdir və bir qrupa yığılır.
 */
object ReviewCategory {
    const val SERIOUS = "!"

    private val LABEL = Regex("""^\s*•?\s*([^:\n]+):""")

    fun of(reason: String?): String? {
        val label = reason?.let { LABEL.find(it)?.groupValues?.get(1)?.trim() } ?: return null
        val firstWord = label.substringBefore(' ').substringBefore('(').substringBefore('/')
        if (firstWord.length > 1 && firstWord.all { !it.isLetter() || it.isUpperCase() }) return SERIOUS
        return label.substringBefore(" + ").substringBefore('/').substringBefore(" (").trim()
            .lowercase().takeIf { it.isNotEmpty() }
    }

    /** Etiket azərbaycanca məlumatdır (yoxlanış onu belə yazıb), ona görə yalnız ilk hərf böyüdülür. */
    fun display(category: String): String = category.replaceFirstChar {
        if (it == 'i') 'İ' else it.uppercaseChar()
    }
}
