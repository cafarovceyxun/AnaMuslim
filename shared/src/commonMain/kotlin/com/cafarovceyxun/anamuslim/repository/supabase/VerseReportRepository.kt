package com.cafarovceyxun.anamuslim.repository.supabase

import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import com.cafarovceyxun.anamuslim.utils.supabase.VerseReport
import com.cafarovceyxun.anamuslim.utils.supabase.VerseReportSubmission
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * `verse_reports` cədvəli ilə iş. Insert anonim istifadəçiyə açıqdır, oxumaq/dəyişmək isə
 * yalnız daxil olmuş istifadəçilər üçün — cədvəlin RLS siyasətləri docs/supabase/verse_reports.sql.
 */
class VerseReportRepository {

    suspend fun submit(report: VerseReportSubmission): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            SupabaseProvider.client.from(TABLE).insert(report)
            Unit
        }
    }

    suspend fun fetchAll(): List<VerseReport> = withContext(Dispatchers.IO) {
        SupabaseProvider.client.from(TABLE)
            .select { order("created_at", Order.DESCENDING) }
            .decodeList<VerseReport>()
    }

    suspend fun updateStatus(id: Long, status: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            SupabaseProvider.client.from(TABLE).update({ set("status", status) }) {
                filter { eq("id", id) }
            }
            Unit
        }
    }

    suspend fun delete(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            SupabaseProvider.client.from(TABLE).delete {
                filter { eq("id", id) }
            }
            Unit
        }
    }

    private companion object {
        const val TABLE = "verse_reports"
    }
}
