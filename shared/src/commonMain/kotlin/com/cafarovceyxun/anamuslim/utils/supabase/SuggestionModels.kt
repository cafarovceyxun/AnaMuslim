package com.cafarovceyxun.anamuslim.utils.supabase

import com.cafarovceyxun.anamuslim.utils.app.AppVersionName
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
     * Əlavə olunmuş funksiyanın media siyahısı (Supabase Storage-dakı public linklər) — «bu funksiya
     * buradadır» izahı. Sıra hekayədəki sıradır. Yalnız admin qoşur; istifadəçi təklifinə media
     * əlavə edə bilmir (moderasiya edilməmiş fayl yükləmə yolu qəsdən yoxdur).
     */
    val media: List<SuggestionMedia> = emptyList(),
    /**
     * Admin qeydi — hekayədə mətnin üstündə görünür, adətən «funksiya haradadır» izahı.
     * `suggestion_submissions.admin_note`-dan fərqlidir: o, göndərənə cavabdır və ictimai deyil.
     */
    val note: String? = null,
    /**
     * Təxmini baxış sayı. Klient hekayəni **ilk dəfə** açanda `mark_suggestion_viewed()` çağırır;
     * baxılma vəziyyəti cihazda saxlanıldığı üçün bu, «unikal insan» sayı deyil.
     */
    val view_count: Int = 0,
    /**
     * Hekayənin hansı platformada görünəcəyi: `all` (ümumi funksiya), `ios` və ya `android`.
     * Funksiya bir platformada gec çıxırsa ayrıca sətir yazılır — o birində istifadəçi olmayan
     * funksiyanın hekayəsini görməsin.
     */
    val platform: String = SuggestionPlatform.ALL,
    /**
     * Funksiyanın gəldiyi **buraxılış adı** (`2026.08.31`). Bundan aşağı sürümdə hekayə görünmür:
     * 31 güncəlləməsini alan görür, 30-da qalan görmür. `null` → hədd yoxdur, hamı görür.
     * Nömrə deyil, ad müqayisə olunur ([com.cafarovceyxun.anamuslim.utils.app.AppVersionName]).
     */
    val min_app_version: String? = null,
    val source_submission_id: Long? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
) {
    /**
     * Hekayə zolağına düşürmü — göstəriləcək **bir şey** olmalıdır: ya media, ya da admin qeydi.
     * Qeydi olan mediasız təklif mətn slaydı kimi oynayır; ikisi də yoxdursa dairə boş qalardı.
     */
    val hasStory: Boolean get() = media.isNotEmpty() || !note.isNullOrBlank()

    /**
     * Bu cihaz hekayəni görürmü — platforma və quraşdırılmış buraxılış adına görə.
     *
     * Süzgəc **klientdədir**: `suggestions` cədvəlini hamı oxuyur, sətir isə platformasına və
     * sürümünə görə gizlədilir. Serverdə süzmək üçün sorğuya kimlik lazım olardı, bu cədvəldə isə
     * kimlik qəsdən saxlanılmır.
     */
    fun isVisibleOn(platformId: String, versionName: String?): Boolean {
        val platformOk = platform == SuggestionPlatform.ALL || platform == platformId
        return platformOk && AppVersionName.isAtLeast(versionName, min_app_version)
    }
}

/** `suggestions.platform` — hekayənin hədəf platforması. */
object SuggestionPlatform {
    /** Ümumi funksiya: hər iki platformada görünür. */
    const val ALL = "all"
    const val IOS = "ios"
    const val ANDROID = "android"

    /** Vərəqdəki sıra — bazadakı CHECK ilə eyni dəst. */
    val ALL_VALUES = listOf(ALL, ANDROID, IOS)
}

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

/** `suggestions.media` massivinin bir elementi. */
@Serializable
data class SuggestionMedia(
    val url: String,
    val type: String = SuggestionMediaType.IMAGE,
) {
    val isVideo: Boolean get() = type == SuggestionMediaType.VIDEO
}

object SuggestionMediaType {
    const val IMAGE = "image"
    const val VIDEO = "video"
}

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
