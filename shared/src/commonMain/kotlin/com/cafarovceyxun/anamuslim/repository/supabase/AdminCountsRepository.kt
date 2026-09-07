package com.cafarovceyxun.anamuslim.repository.supabase

import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder

/** İdarəetmə panelindəki nişanların göstərdiyi emal olunmamış sətir sayları. */
data class AdminPendingCounts(
    val pendingEdits: Int = 0,
    val pendingReports: Int = 0,
    val pendingSuggestions: Int = 0,
) {
    val total: Int get() = pendingEdits + pendingReports + pendingSuggestions
    val isEmpty: Boolean get() = total == 0
}

/**
 * Gözləyən moderasiya işinin sayı. Sorğular **gövdəsiz**dir (`head = true` + `count(EXACT)`) —
 * sətirlərin özü çəkilmir, yalnız `Content-Range` başlığındakı say oxunur.
 *
 * Sətirlər RLS ilə qorunur: girişsiz istifadəçi üçün say sıfır gəlir, xəta yox — ona görə nişanlar
 * səssizcə görünməz olur.
 */
object AdminCountsRepository {

    suspend fun fetch(): AdminPendingCounts = try {
        AdminPendingCounts(
            pendingEdits = count("quran_edits") { eq("is_approved", false) } +
                count("hadith_edits") { eq("status", STATUS_PENDING) },
            pendingReports = count("verse_reports") { eq("status", STATUS_PENDING) },
            pendingSuggestions = count("suggestion_submissions") { eq("status", STATUS_PENDING) },
        )
    } catch (e: Exception) {
        AppLogger.d("AdminCounts", "Pending count fetch failed: ${e.message}")
        AdminPendingCounts()
    }

    private suspend fun count(
        table: String,
        filter: PostgrestFilterBuilder.() -> Unit,
    ): Int = SupabaseProvider.client.from(table)
        .select(Columns.list("id")) {
            head = true
            count(Count.EXACT)
            filter(filter)
        }
        .countOrNull()
        ?.toInt()
        ?: 0

    private const val STATUS_PENDING = "pending"
}
