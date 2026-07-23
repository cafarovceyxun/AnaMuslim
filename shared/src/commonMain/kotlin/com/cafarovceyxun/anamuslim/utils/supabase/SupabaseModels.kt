package com.cafarovceyxun.anamuslim.utils.supabase

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

@Serializable
data class DailyContent(
    val id: Long? = null,
    val content_type: String, // 'verse' or 'hadith'
    val chapter_no: Int? = null,
    val verse_no: Int? = null,
    val hadith_id: Long? = null,
    val text_ar: String,
    val text_az: String,
    val source: String? = null,
    val date: String? = null, // yyyy-MM-dd
    val created_at: String? = null,
    val created_by: String? = null
)

@Serializable
data class QuranEdit(
    val id: Long? = null,
    val translation_id: Long,
    val new_text: String,
    val editor_email: String,
    val is_approved: Boolean = false,
    val created_at: String? = null,
    val user_id: String? = null,
    val chapter_no: Long? = null,
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
    val user_id: String? = null
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
