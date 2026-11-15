package com.cafarovceyxun.anamuslim.utils.supabase

import com.cafarovceyxun.anamuslim.api.JsonHelper
import com.cafarovceyxun.anamuslim.api.NetworkClient
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import io.github.jan.supabase.auth.auth
import io.ktor.client.plugins.timeout
import io.ktor.client.request.delete
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

/**
 * Mağazadan kənar paylanan Android APK-sının Supabase Storage-dakı yeri (`apk` bucket-i, public
 * oxu, yazma yalnız admin).
 *
 * Fayl **versionCode ilə adlanır**, sabit adla yox: admin onu Ayarlar → «Buraxılış Bildirişi»ndən
 * yükləyir («APK» tab-ı), `direct` build isə öz sətrindəki (`app_releases`, `android_apk`)
 * `latest_version` nömrəsinin faylını çəkir (`SelfUpdater`). Play build-ləri `android` sətrini oxuyur
 * və Play Store-a gedir. Keşdə köhnə fayl ilişmir, hələ yüklənməmiş versiya isə sadəcə 400/404 verir.
 */
object ApkStorage {
    const val BUCKET = "apk"

    fun fileName(versionCode: Long): String = "anamuslim-$versionCode.apk"

    /** Yükləmə/silmə ünvanı — giriş etmiş adminin tokeni ilə. */
    fun objectUrl(versionCode: Long): String =
        "${SupabaseProvider.restUrl}/storage/v1/object/$BUCKET/${fileName(versionCode)}"

    /** Hər kəsin endirə bildiyi ünvan. */
    fun publicUrl(versionCode: Long): String =
        "${SupabaseProvider.restUrl}/storage/v1/object/public/$BUCKET/${fileName(versionCode)}"

    private const val TAG = "ApkStorage"
    private const val CHUNK_SIZE = 64 * 1024
    private const val APK_MIME = "application/vnd.android.package-archive"

    // ~130 MB mobil şəbəkədə dəqiqələrlə çəkə bilər; ümumi müştərinin kiçik API sorğuları üçün
    // seçilmiş vaxtları bu yükləməni yarıda kəsərdi.
    private const val UPLOAD_TIMEOUT_MS = 60L * 60 * 1000
    private const val SOCKET_TIMEOUT_MS = 5L * 60 * 1000

    /**
     * [path]-dakı faylı **axınla** yükləyir — APK ~130 MB-dır, yaddaşda `ByteArray` kimi tutulmur.
     * [onProgress] 0..1 arası göndərilmiş hissəni verir (hər 64 KB-dan sonra; çağıran süzgəcləyir).
     * Nəticə uğursuzdursa mesajda status kodu və cavab mətni olur.
     */
    suspend fun upload(
        path: String,
        versionCode: Long,
        onProgress: (Float) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = SupabaseProvider.client.auth.currentSessionOrNull()?.accessToken
                ?: error("not signed in")

            val okioPath = path.toPath()
            val total = AppFileSystem.size(okioPath) ?: error("file not found")

            val content = object : OutgoingContent.WriteChannelContent() {
                override val contentLength: Long = total
                override val contentType: ContentType = ContentType.parse(APK_MIME)

                override suspend fun writeTo(channel: ByteWriteChannel) {
                    val buffer = ByteArray(CHUNK_SIZE)
                    var sent = 0L
                    AppFileSystem.fileSystem.source(okioPath).buffer().use { source ->
                        while (true) {
                            val read = source.read(buffer)
                            if (read == -1) break
                            channel.writeFully(buffer, 0, read)
                            sent += read
                            if (total > 0) onProgress((sent.toDouble() / total).toFloat().coerceIn(0f, 1f))
                        }
                    }
                    channel.flush()
                }
            }

            val response = NetworkClient.client.post(objectUrl(versionCode)) {
                header(HttpHeaders.Authorization, "Bearer $token")
                header("apikey", SupabaseProvider.anonKey)
                // Eyni versiya yenidən yüklənəndə üzərinə yazılsın.
                header("x-upsert", "true")
                timeout {
                    requestTimeoutMillis = UPLOAD_TIMEOUT_MS
                    socketTimeoutMillis = SOCKET_TIMEOUT_MS
                }
                setBody(content)
            }

            if (!response.status.isSuccess()) {
                error("upload failed: ${response.status.value} ${response.bodyAsText()}")
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.d(TAG, "Upload failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Bucket-dəki APK-lardan [keep] nömrələrinə aid olmayanları silir və silinən sayı qaytarır.
     *
     * Hər fayl ~130 MB-dır və yalnız ən son buraxılış lazımdır — köhnələr serverin diskini doldurur,
     * heç kim onları endirmir (`direct` build həmişə öz sətrindəki nömrəni çəkir). Siyahı və silmə
     * giriş etmiş adminin tokeni ilə gedir; storage siyasəti silməni yalnız adminə verir.
     */
    suspend fun deleteAllExcept(keep: Set<Long>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val token = SupabaseProvider.client.auth.currentSessionOrNull()?.accessToken
                ?: error("not signed in")
            val keepNames = keep.filter { it > 0 }.map(::fileName).toSet()

            val listed = NetworkClient.client.post("${SupabaseProvider.restUrl}/storage/v1/object/list/$BUCKET") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header("apikey", SupabaseProvider.anonKey)
                contentType(ContentType.Application.Json)
                setBody("""{"prefix":"","limit":1000,"offset":0}""")
            }
            if (!listed.status.isSuccess()) error("list failed: ${listed.status.value} ${listed.bodyAsText()}")

            val stale = JsonHelper.json.parseToJsonElement(listed.bodyAsText()).jsonArray
                .mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull }
                .filter { it.endsWith(".apk") && it !in keepNames }
            if (stale.isEmpty()) return@withContext Result.success(0)

            val prefixes = stale.joinToString(",") { "\"$it\"" }
            val deleted = NetworkClient.client.delete("${SupabaseProvider.restUrl}/storage/v1/object/$BUCKET") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header("apikey", SupabaseProvider.anonKey)
                contentType(ContentType.Application.Json)
                setBody("""{"prefixes":[$prefixes]}""")
            }
            if (!deleted.status.isSuccess()) error("delete failed: ${deleted.status.value} ${deleted.bodyAsText()}")

            // RLS bloklasa storage xəta yox, boş massiv qaytarır — sayı cavabdan götür.
            Result.success(JsonHelper.json.parseToJsonElement(deleted.bodyAsText()).jsonArray.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.d(TAG, "Cleanup failed: ${e.message}")
            Result.failure(e)
        }
    }

    /** Bu versiyanın APK-sı artıq yüklənibmi (public ünvana HEAD). Xəta = `false`. */
    suspend fun exists(versionCode: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            NetworkClient.client.head(publicUrl(versionCode)).status.isSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }
}
