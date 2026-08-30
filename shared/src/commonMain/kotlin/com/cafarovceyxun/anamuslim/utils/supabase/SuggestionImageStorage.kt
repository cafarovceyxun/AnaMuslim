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
 * `suggestion-images` bucket-i ilə iş — Supabase Storage-ın REST API-si üzərindən, paylaşılan Ktor
 * klienti ilə. `storage-kt` plugin-i qəsdən quraşdırılmayıb: bax [SupabaseProvider.restUrl].
 *
 * Bucket public-dir (şəkil linki tətbiqdə birbaşa açılır), yazma isə RLS ilə admin-ə bağlıdır —
 * ona görə hər çağırışa **giriş etmiş istifadəçinin tokeni** qoşulur. Token yoxdursa Storage 401
 * qaytarır və biz onu aydın xəta kimi göstəririk.
 */
object SuggestionImageStorage {

    private const val BUCKET = "suggestion-images"

    suspend fun upload(image: ByteArray, mimeType: String): Result<String> =
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
                    setBody(image)
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
            val name = publicUrl.substringAfterLast("/$BUCKET/", "").takeIf { it.isNotBlank() }
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
        "${SupabaseProvider.restUrl}/storage/v1/object/$BUCKET/$name"

    private fun publicUrl(name: String) =
        "${SupabaseProvider.restUrl}/storage/v1/object/public/$BUCKET/$name"

    private fun fileName(mimeType: String): String {
        val extension = when (mimeType.substringBefore(';').trim()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        return "feature-${currentEpochMillis()}-${Random.nextInt(100_000, 999_999)}.$extension"
    }

    private const val TAG = "SuggestionImage"
}
