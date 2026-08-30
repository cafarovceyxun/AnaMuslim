package com.cafarovceyxun.anamuslim.utils.supabase

import kotlinx.serialization.Serializable

/**
 * İstifadəçi təklifləri iki cədvəldədir və modellər də ona görə ikiyə bölünür:
 *
 * - [SuggestionSubmissionRow] → `suggestion_submissions`, moderasiya növbəsi. Yalnız admin oxuyur;
 *   istifadəçi ora nə yazır, nə də baxır — göndəriş `submit_suggestion()` RPC-sindən keçir.
 * - [Suggestion] → `suggestions`, təsdiqdən sonra hamıya görünən siyahı.
 *
 * **Bazada istifadəçi kimliyi saxlanılmır** — nə cihaz id-si, nə hesab id-si, nə də səs cədvəli var.
 * Göndərişin yeganə izi [SuggestionTicket.ticket]-dir: təsadüfi qəbz, yalnız həmin bir təklifin
 * statusunu açır və **yalnız cihazda** saxlanılır (bax [SuggestionLocalStore]). «Bu cihaz səs
 * veribmi» sualının cavabı da lokaldır; baza yalnız ümumi sayğacı bilir.
 *
 * Sxem: `docs/supabase/SCHEMA.md`.
 */

/** `suggestions` sətri — təsdiqlənmiş, ictimai təklif. */
@Serializable
data class Suggestion(
    val id: Long,
    val body: String,
    val category: String = SuggestionCategory.OTHER,
    val status: String = SuggestionStatus.OPEN,
    val vote_count: Int = 0,
    /**
     * Əlavə olunmuş funksiyanın ekran görüntüsü (Supabase Storage-dakı public link) — «bu funksiya
     * buradadır» izahı. Yalnız admin qoşur; istifadəçi təklifinə şəkil əlavə edə bilmir.
     */
    val image_url: String? = null,
    val source_submission_id: Long? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
)

/** `suggestion_submissions` sətrinin tam forması — yalnız idarəetmə paneli oxuyur. */
@Serializable
data class SuggestionSubmissionRow(
    val id: Long,
    val ticket: String? = null,
    val body: String,
    val category: String = SuggestionCategory.OTHER,
    val app_version: String? = null,
    val platform: String? = null,
    val status: String = SuggestionSubmissionStatus.PENDING,
    val admin_note: String? = null,
    val suggestion_id: Long? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
)

/**
 * `get_suggestion_tickets(uuid[])` RPC-sinin sətri — bu cihazdan göndərilmiş təklifin cari
 * vəziyyəti. Sorğu yalnız cihazdakı qəbzlərlə gedir, ona görə başqasının təklifi qayıtmır.
 */
@Serializable
data class SuggestionTicket(
    val ticket: String,
    val body: String,
    val category: String = SuggestionCategory.OTHER,
    val status: String = SuggestionSubmissionStatus.PENDING,
    val admin_note: String? = null,
    val created_at: String? = null,
    val suggestion_id: Long? = null,
)

object SuggestionCategory {
    const val FEATURE = "feature"
    const val BUG = "bug"
    const val CONTENT = "content"
    const val OTHER = "other"

    /** Vərəqdəki və süzgəcdəki sıra — bazadakı CHECK ilə eyni dəst. */
    val ALL = listOf(FEATURE, BUG, CONTENT, OTHER)
}

/** `suggestions.status` — təsdiqdən sonrakı iş vəziyyəti. */
object SuggestionStatus {
    const val OPEN = "open"
    const val PLANNED = "planned"
    const val DONE = "done"

    val ALL = listOf(OPEN, PLANNED, DONE)
}

/** `suggestion_submissions.status` — moderasiya vəziyyəti. */
object SuggestionSubmissionStatus {
    const val PENDING = "pending"
    const val APPROVED = "approved"
    const val REJECTED = "rejected"

    val ALL = listOf(PENDING, APPROVED, REJECTED)
}
