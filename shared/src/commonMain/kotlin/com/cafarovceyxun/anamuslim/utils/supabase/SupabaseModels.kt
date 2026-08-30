package com.cafarovceyxun.anamuslim.utils.supabase

import com.cafarovceyxun.anamuslim.utils.verse.DailyContentSchedule
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseTranslation(
    val id: Int? = null,
    val chapter_no: Int,
    val verse_no: Int,
    val slug: String,
    val text: String? = "",
    val note: String? = "", // Yeni sütun
    val updated_at: String? = null
)

/**
 * `daily_content_item` sətri — günün ayəsi/hədisi növbəsinin bir elementi.
 *
 * ⚠️ Cədvəlin adı `daily_content` **deyil**: köhnə ad indi yalnız `slot_index = 0` sətirlərini
 * göstərən uyğunluq view-udur (miqrasiya `daily_content_queue_slots`). Mağazadakı yenilənməmiş
 * quraşdırmalar həmin view-u `decodeSingleOrNull` ilə oxumağa davam edir; yeni kod
 * [com.cafarovceyxun.anamuslim.repository.supabase.DailyContentRepository] vasitəsilə cədvəlin
 * özünə müraciət edir.
 *
 * Bir gündə [DailyContentSlots.COUNT] element ola bilər; sıra `(date, slot_index)` cütüdür və
 * bildirişlərin ardıcıllığı da elə budur.
 */
@Serializable
data class DailyContent(
    val id: Long? = null,
    val content_type: String, // 'verse' or 'hadith'
    val chapter_no: Int? = null,
    val verse_no: Int? = null,
    /** Çoxayəli element üçün aralığın sonu (daxil olmaqla); tək ayədə null. */
    val verse_end: Int? = null,
    val hadith_id: Long? = null,
    val text_ar: String,
    val text_az: String,
    /** Hədisin yalnız seçilmiş hissəsi — null olanda tam mətn göstərilir. */
    val excerpt_ar: String? = null,
    val excerpt_az: String? = null,
    val source: String? = null,
    val date: String? = null, // yyyy-MM-dd
    val slot_index: Int = 0,
    /** Hekayəyə neçə dəfə baxılıb. Yalnız serverdə artır; kimin baxdığı saxlanmır. */
    val view_count: Int = 0,
    val created_at: String? = null,
    val created_by: String? = null
) {
    /** Ekranda və bildirişdə göstəriləcək tərcümə mətni: çıxarış varsa o, yoxsa tam mətn. */
    val displayTextAz: String get() = excerpt_az?.takeIf { it.isNotBlank() } ?: text_az

    /** Ekranda və bildirişdə göstəriləcək ərəb mətni. */
    val displayTextAr: String get() = excerpt_ar?.takeIf { it.isNotBlank() } ?: text_ar

    val isHadith: Boolean get() = content_type == CONTENT_TYPE_HADITH

    /**
     * Elementin əhatə etdiyi ayələr. Tək ayədə bir elementli, aralıqda isə hamısı — kart və
     * paylaşma şəkli bunun üzərində gəzir.
     */
    val verseNumbers: List<Int>
        get() {
            val start = verse_no ?: return emptyList()
            val end = verse_end ?: start
            return if (end >= start) (start..end).toList() else listOf(start)
        }

    /**
     * Növbədəki yeri — sıralama və «bu artıq bildirilib?» yoxlaması üçün sabit açar.
     * `id` yaramır: admin elementi redaktə edəndə id qalır, yeri isə dəyişə bilər.
     */
    val slotKey: String get() = "${date.orEmpty()}#$slot_index"

    /**
     * Yuvanın vaxtı keçibmi. Belə element **yerindən tərpədilmir**: artıq bildirilib və kartda
     * görünür, gələcək yuvaya köçürülsə eyni ayə ikinci dəfə çalınardı.
     */
    fun isPast(nowMillis: Long): Boolean {
        val at = date?.let { DailyContentSchedule.epochMillisOf(it, slot_index) } ?: return false
        return at <= nowMillis
    }

    companion object {
        const val CONTENT_TYPE_VERSE = "verse"
        const val CONTENT_TYPE_HADITH = "hadith"
    }
}

/** Bir günə düşən bildiriş yuvaları. Bazadakı `slot_index` CHECK-i ilə eyni saxlanılmalıdır. */
object DailyContentSlots {
    const val COUNT = 5
}

/**
 * `quran_edits` sətri. `translation_id` sxemdə nullable-dir, ona görə burada da nullable saxlanılır —
 * bir sınıq sətir bütün siyahının deserializasiyasını çökürtməsin. `verse_no` `edits_hardening.sql`
 * ilə əlavə olunub; miqrasiyadan əvvəlki bazada sadəcə null qalır.
 */
@Serializable
data class QuranEdit(
    val id: Long? = null,
    val translation_id: Long? = null,
    val new_text: String,
    val editor_email: String,
    val is_approved: Boolean = false,
    val created_at: String? = null,
    val user_id: String? = null,
    val chapter_no: Long? = null,
    val verse_no: Long? = null,
    val note: String? = null
)

@Serializable
data class HadithEdit(
    val id: Long? = null,
    val hadith_id: Long? = null,
    val chapter_slug: String? = null,
    val sub_chapter_slug: String? = null,
    val hadith_no: Long? = null,
    val text_ar: String? = null,
    val text_az: String? = null,
    val source: String? = null,
    val note: String? = null,
    val editor_email: String? = null,
    val status: String? = "pending",
    val created_at: String? = null,
    val chapter_no: Long? = null,
    val user_id: String? = null,
    /**
     * true → bu sətir düzəliş yox, **silmə tələbidir**: təsdiqləndikdə `hadith` sətri silinir və
     * aşağıdakı mətn sahələri heç yerə köçürülmür — onlar yalnız paneldə «nə silinir» sualına cavab
     * vermək üçün silinən sətrin surətidir.
     */
    val is_delete: Boolean = false
)

/**
 * Ayə ilə bağlı istifadəçi bildirişi.
 *
 * İstifadəçi yalnız [message] yazır; ayə konteksti (surə/ayə, tərcümə slug-ları, app versiyası)
 * arxa fonda doldurulur. `status` və `admin_note` yalnız idarəetmə panelindən dəyişir —
 * insert siyasəti onları klientdən qəbul etmir (bax: docs/supabase/verse_reports.sql).
 */
@Serializable
data class VerseReport(
    val id: Long? = null,
    val chapter_no: Int,
    val verse_no: Int,
    val verse_key: String? = null,
    val message: String,
    val slugs: String? = null,
    val app_version: String? = null,
    val status: String? = null,
    val admin_note: String? = null,
    val user_id: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
)

/**
 * Insert üçün ayrıca payload: `status`/`admin_note`/`id` göndərilmir ki, cədvəlin default-ları
 * işləsin — RLS insert siyasəti klientdən gələn statusu qəbul etmir.
 */
@Serializable
data class VerseReportSubmission(
    val chapter_no: Int,
    val verse_no: Int,
    val verse_key: String? = null,
    val message: String,
    val slugs: String? = null,
    val app_version: String? = null,
    val user_id: String? = null,
)

@Serializable
data class ResourceUpdateStatus(
    val id: Int = 1,
    val version: Int = 0,
    val updated_at: String? = null
)

/**
 * `app_releases` sətri — hansı buraxılışın mağazada canlı olduğunu bildirir. Hər platforma
 * (`android` / `ios`) üçün bir sətir, PK `platform`.
 *
 * `latest_version` platformanın öz nömrə sahəsindədir: Android-də `versionCode`, iOS-də
 * `CFBundleVersion`. Ona görə müqayisə **həmişə** öz sətri ilə aparılır — bax [AppUpdateChecker].
 *
 * Hər sahənin default-u var ki, yarımçıq doldurulmuş sətir bannerı çökürtməsin, sadəcə
 * "yeniləmə yoxdur" kimi oxunsun (`latest_version = 0` heç vaxt real versiyadan böyük olmur).
 */
@Serializable
data class AppRelease(
    val platform: String = "",
    val latest_version: Long = 0,
    val latest_version_name: String? = null,
    val min_version: Long = 0,
    val action_url: String? = null,
    /** Dil kodu → sətirlər, məsələn `{"az": ["..."], "en": ["..."]}`. */
    val release_notes: Map<String, List<String>> = emptyMap(),
    val updated_at: String? = null,
) {
    /** [languageCodes]-dan cədvəldə həqiqətən olan ilk dilin qeydləri, yoxdursa `az`, sonra `en`. */
    fun releaseNotesFor(languageCodes: Sequence<String>): List<String> =
        (languageCodes + sequenceOf("az", "en"))
            .mapNotNull { release_notes[it]?.takeIf(List<String>::isNotEmpty) }
            .firstOrNull()
            ?: emptyList()
}
