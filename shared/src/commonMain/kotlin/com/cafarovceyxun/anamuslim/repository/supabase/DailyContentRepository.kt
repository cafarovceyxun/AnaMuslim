package com.cafarovceyxun.anamuslim.repository.supabase

import com.cafarovceyxun.anamuslim.utils.currentLocalDateIsoString
import com.cafarovceyxun.anamuslim.utils.supabase.DailyContent
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class DailyContentRepository {

    suspend fun fetchTodayContent(): DailyContent? = withContext(Dispatchers.IO) {
        try {
            val today = currentLocalDateIsoString()
            SupabaseProvider.client.from("daily_content")
                .select {
                    filter {
                        eq("date", today)
                    }
                }
                .decodeSingleOrNull<DailyContent>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun setDailyContent(content: DailyContent): Boolean = withContext(Dispatchers.IO) {
        try {
            val today = currentLocalDateIsoString()

            // Upsert based on the 'date' unique constraint.
            // In Supabase Kotlin SDK, upsert with onConflict handles existing records.
            val contentToUpsert = content.copy(
                date = today
            )

            SupabaseProvider.client.from("daily_content").upsert(contentToUpsert) {
                onConflict = "date"
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
