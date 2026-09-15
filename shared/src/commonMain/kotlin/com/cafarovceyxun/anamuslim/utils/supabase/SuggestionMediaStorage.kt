package com.cafarovceyxun.anamuslim.utils.supabase

import com.cafarovceyxun.anamuslim.api.NetworkClient
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import io.github.jan.supabase.auth.auth
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Supabase Storage bucket-i ilə iş (şəkil və video) — Storage-ın REST API-si üzərindən, paylaşılan
 * Ktor klienti ilə. `storage-kt` plugin-i qəsdən quraşdırılmayıb: bax [SupabaseProvider.restUrl].
 *
 * Bucket-lər public-dir (link tətbiqdə birbaşa açılır), yazma isə RLS ilə admin-ə bağlıdır —
 * ona görə hər çağırışa **giriş etmiş istifadəçinin tokeni** qoşulur. Token yoxdursa Storage 401
 * qaytarır və biz onu aydın xəta kimi göstəririk.
 *
 * Bucket adı parametrdir, çünki iki ayrı dəst var və onlar **qarışmamalıdır**: qəməri elanların
 * faylları 12 aydan sonra serverdə silinir (`prune_lunar_announcements()`), funksiya hekayələrinin
 * şəkilləri isə qalır. Bir bucket-i bölüşsəydilər təmizləmə funksiya şəkillərini də aparardı.
 */
class MediaStorage(private val bucket: String, private val namePrefix: String) {

    suspend fun upload(bytes: ByteArray, mimeType: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val token = SupabaseProvider.client.auth.currentSessionOrNull()?.accessToken
                    ?: error("not signed in")

                val name = fileName(mimeType)
                val response = NetworkClient.client.post(objectUrl(name)) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    header("apikey", SupabaseProvider.anonKey)
                    // Eyni ad təsadüfən təkrarlansa üzərinə yazılsın, yükləmə xəta ilə dayanmasın.
                    header("x-upsert", "true")
                    contentType(ContentType.parse(mimeType))
                    setBody(bytes)
                }

                if (!response.status.isSuccess()) {
                    error("upload failed: ${response.status.value} ${response.bodyAsText()}")
                }

                publicUrl(name)
            }.onFailure { AppLogger.d(TAG, "Upload failed: ${it.message}") }
        }

    /** Şəkil dəyişdiriləndə/silinəndə köhnə faylı bucket-də qoymuruq. */
    suspend fun delete(publicUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val name = publicUrl.substringAfterLast("/$bucket/", "").takeIf { it.isNotBlank() }
                ?: return@runCatching Unit

            val token = SupabaseProvider.client.auth.currentSessionOrNull()?.accessToken
                ?: error("not signed in")

            NetworkClient.client.delete(objectUrl(name)) {
                header(HttpHeaders.Authorization, "Bearer $token")
                header("apikey", SupabaseProvider.anonKey)
            }
            Unit
        }.onFailure { AppLogger.d(TAG, "Delete failed: ${it.message}") }
    }

    private fun objectUrl(name: String) =
        "${SupabaseProvider.restUrl}/storage/v1/object/$bucket/$name"

    private fun publicUrl(name: String) =
        "${SupabaseProvider.restUrl}/storage/v1/object/public/$bucket/$name"

    private fun fileName(mimeType: String): String {
        val extension = when (mimeType.substringBefore(';').trim()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "video/mp4" -> "mp4"
            "video/quicktime" -> "mov"
            else -> "jpg"
        }
        return "$namePrefix-${currentEpochMillis()}-${Random.nextInt(100_000, 999_999)}.$extension"
    }

    private companion object {
        const val TAG = "MediaStorage"
    }
}

/** «Yeniliklər» hekayəsinin şəkil/videoları — `suggestions.media` linkləri buradandır. */
val SuggestionMediaStorage = MediaStorage(bucket = "suggestion-images", namePrefix = "feature")

/** Qəməri ay elanlarının şəkil/videoları — 12 aydan sonra serverdə özü silinir. */
val LunarMediaStorage = MediaStorage(bucket = "lunar-media", namePrefix = "lunar")
