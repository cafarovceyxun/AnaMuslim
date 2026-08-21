package com.cafarovceyxun.anamuslim.repository.supabase

import com.cafarovceyxun.anamuslim.compose.utils.preferences.VersePreferences
import com.cafarovceyxun.anamuslim.utils.currentLocalDateIsoString
import com.cafarovceyxun.anamuslim.utils.supabase.DailyContent
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class DailyContentRepository {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Bugünkü məzmun — əvvəlcə serverdən, alınmasa keşdən.
     *
     * Sorğu uğur qazananda nəticə keşlənir (null cavab da: admin sətri silibsə keş də təmizlənir).
     * Uğursuz olanda — praktikada demək olar həmişə internetin olmaması — keşdəki sətir qaytarılır,
     * amma yalnız onun öz `date` sahəsi bu gündürsə: dünənki ayəni «günün ayəsi» kimi göstərmək
     * səhv olardı, boş kart isə düzgün nəticədir.
     */
    suspend fun fetchTodayContent(): DailyContent? = withContext(Dispatchers.IO) {
        val today = currentLocalDateIsoString()

        try {
            val content = SupabaseProvider.client.from("daily_content")
                .select {
                    filter {
                        eq("date", today)
                    }
                }
                .decodeSingleOrNull<DailyContent>()

            VersePreferences.setDailyContentCache(
                content?.let { json.encodeToString(DailyContent.serializer(), it) }.orEmpty()
            )

            content
        } catch (e: Exception) {
            cachedContentFor(today)
        }
    }

    private fun cachedContentFor(today: String): DailyContent? {
        val cached = VersePreferences.getDailyContentCache()
        if (cached.isBlank()) return null

        val content = try {
            json.decodeFromString(DailyContent.serializer(), cached)
        } catch (e: Exception) {
            // Sxem dəyişibsə köhnə keş oxunmur; boş kart çökmədən yaxşıdır.
            return null
        }

        return content.takeIf { it.date == today }
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
