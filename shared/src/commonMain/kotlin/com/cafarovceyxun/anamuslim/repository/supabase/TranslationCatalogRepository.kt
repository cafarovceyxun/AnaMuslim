package com.cafarovceyxun.anamuslim.repository.supabase

import androidx.datastore.preferences.core.stringPreferencesKey
import com.cafarovceyxun.anamuslim.api.models.translation.TranslationBookInfoModel
import com.cafarovceyxun.anamuslim.compose.utils.preferences.DataStoreManager
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrefKey
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.concurrent.Volatile

/**
 * `quran_translation_books` sətri — hansı tərcümə kitablarının mövcud olduğunu **server** deyir.
 *
 * [source_column] `quran_translations_data`-dakı hansı sütunun bu kitabın mətni olduğunu göstərir
 * (`text` — mövcud tərcümə, `text_alt` — yeni tərcümə). Sətirlər eynidir, dəyişən sütundur.
 *
 * [is_public] `false` olanda kitab **yalnız giriş edilmiş** istifadəçiyə görünür: admin tərcüməni
 * hazırlayarkən özü sınayır, hazır olanda bu bayrağı açır və kitab hamıya çıxır — **yeni buraxılış
 * lazım deyil**.
 */
@Serializable
data class TranslationCatalogBook(
    val slug: String,
    val source_column: String = COLUMN_TEXT,
    val book_name: String = "",
    val author_name: String = "",
    val lang_code: String = "az",
    val lang_name: String = "Azərbaycan",
    val is_public: Boolean = false,
) {
    /** Qeyd sütunu mətn sütunundan törəyir: `text` → `note`, `text_alt` → `note_alt`. */
    val noteColumn: String get() = if (source_column == COLUMN_TEXT) "note" else "note_alt"

    fun toBookInfo(): TranslationBookInfoModel = TranslationBookInfoModel(slug).apply {
        langCode = lang_code
        langName = lang_name
        bookName = book_name
        authorName = author_name
        displayName = book_name
    }
}

const val COLUMN_TEXT = "text"
const val COLUMN_TEXT_ALT = "text_alt"

/**
 * Tərcümə kataloqu — əvvəllər `TranslationViewModel`-də koda yazılmış tək `az` girişi idi.
 *
 * Siyahı **heç vaxt boş qalmır**: şəbəkə yoxdursa cihazdakı keş, keş də boşdursa [FALLBACK]
 * işlədilir (məhz köhnə hardcoded giriş). Ona görə oflayn açılışda tərcümə siyahısı boşalmır.
 */
object TranslationCatalogRepository {

    private const val TABLE = "quran_translation_books"

    /** Kataloq gəlməyəndə işlədilən siyahı — köhnə hardcoded girişin eynisi. */
    val FALLBACK = listOf(
        TranslationCatalogBook(
            slug = "az",
            source_column = COLUMN_TEXT,
            book_name = "Azərbaycan dili",
            author_name = "Mürşüd Yusifoğlu",
            lang_code = "az",
            lang_name = "Azərbaycan",
            is_public = true,
        )
    )

    private val KEY_CACHE = PrefKey(stringPreferencesKey("translation_catalog_json"), "")

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var memory: List<TranslationCatalogBook>? = null

    /** Keşdən ani cavab — şəbəkəyə çıxmır. */
    fun cached(): List<TranslationCatalogBook> {
        memory?.let { return it }
        val raw = DataStoreManager.read(KEY_CACHE)
        if (raw.isBlank()) return FALLBACK
        return try {
            json.decodeFromString<List<TranslationCatalogBook>>(raw).ifEmpty { FALLBACK }
                .also { memory = it }
        } catch (e: Exception) {
            AppLogger.d("TranslCatalog", "Cache decode failed: ${e.message}")
            FALLBACK
        }
    }

    /** Serverdən oxuyub keşi yeniləyir; alınmasa keşdəki siyahı qalır. */
    suspend fun refresh(): List<TranslationCatalogBook> = try {
        val rows = SupabaseProvider.client.from(TABLE).select()
            .decodeList<TranslationCatalogBook>()
        if (rows.isEmpty()) {
            cached()
        } else {
            memory = rows
            DataStoreManager.write(KEY_CACHE, json.encodeToString(rows))
            rows
        }
    } catch (e: Exception) {
        AppLogger.d("TranslCatalog", "Catalog fetch failed: ${e.message}")
        cached()
    }

    suspend fun bookOrNull(slug: String): TranslationCatalogBook? =
        cached().firstOrNull { it.slug == slug } ?: refresh().firstOrNull { it.slug == slug }

    /** Kitabı adi istifadəçilərə açır/bağlayır. Yazma yalnız admin-ə açıqdır (RLS). */
    suspend fun setPublic(slug: String, isPublic: Boolean): Boolean = try {
        val affected = SupabaseProvider.client.from(TABLE).update(
            mapOf("is_public" to isPublic)
        ) {
            select()
            filter { eq("slug", slug) }
        }.decodeList<TranslationCatalogBook>().size
        // RLS blokladıqda PostgREST xəta yox, boş nəticə qaytarır — sətir sayına baxırıq.
        if (affected > 0) refresh()
        affected > 0
    } catch (e: Exception) {
        AppLogger.d("TranslCatalog", "setPublic failed: ${e.message}")
        false
    }
}
