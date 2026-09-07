package com.cafarovceyxun.anamuslim.repository.supabase

import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Bir ayənin idxal sətri. */
data class ImportRow(val verseNo: Int, val text: String)

/**
 * Tərcümə mətninin toplu yazılması.
 *
 * Yazma `import_translation_text` RPC-si ilə gedir: PostgREST-də açar üzrə çoxsətirli `update`
 * yoxdur, ayə-ayə göndərsək Bəqərə üçün 286 ayrı sorğu olardı. RPC `security invoker`-dir — icazə
 * qapısı funksiyanın içində yox, `quran_translations_data`-nın admin-only RLS-indədir.
 */
object TranslationImportRepository {

    private const val TABLE = "quran_translations_data"
    private const val BASE_SLUG = "az"

    /** Yazılan sətir sayını qaytarır; RLS blokladıqda sıfır gəlir (PostgREST xəta verməz). */
    suspend fun importChapter(
        slug: String,
        chapterNo: Int,
        rows: List<ImportRow>,
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            if (rows.isEmpty()) return@runCatching 0
            SupabaseProvider.client.postgrest.rpc(
                function = "import_translation_text",
                parameters = buildJsonObject {
                    put("p_slug", slug)
                    put("p_chapter", chapterNo)
                    put("p_rows", rows.toJson())
                },
            ).decodeAs<Int>()
        }
    }

    /** Surədə bu kitab üçün neçə ayənin mətni doludur — idxal ekranındakı tərəqqi göstəricisi. */
    suspend fun filledVerseCount(book: TranslationCatalogBook, chapterNo: Int): Int =
        withContext(Dispatchers.IO) {
            runCatching {
                SupabaseProvider.client.from(TABLE)
                    .select(Columns.list("verse_no")) {
                        head = true
                        count(Count.EXACT)
                        filter {
                            eq("slug", BASE_SLUG)
                            eq("chapter_no", chapterNo)
                            filterNot(book.source_column, FilterOperator.IS, "null")
                        }
                    }
                    .countOrNull()
                    ?.toInt()
                    ?: 0
            }.getOrDefault(0)
        }

    /**
     * Surədəki **hazırkı** mətnlər — önizləmədə nəyin üzərinə yazılacağını göstərmək üçün.
     * Boş sətirlər siyahıya düşmür, ona görə hələ doldurulmamış ayələr üçün müqayisə göstərilmir.
     */
    suspend fun chapterTexts(book: TranslationCatalogBook, chapterNo: Int): Map<Int, String> =
        withContext(Dispatchers.IO) {
            runCatching {
                SupabaseProvider.client.from(TABLE)
                    .select(Columns.list("verse_no", "text:${book.source_column}")) {
                        filter {
                            eq("slug", BASE_SLUG)
                            eq("chapter_no", chapterNo)
                        }
                    }
                    .decodeList<ChapterTextRow>()
                    .mapNotNull { row -> row.text?.takeIf { it.isNotBlank() }?.let { row.verse_no to it } }
                    .toMap()
            }.getOrDefault(emptyMap())
        }

    private fun List<ImportRow>.toJson(): JsonArray = buildJsonArray {
        this@toJson.forEach { row ->
            add(
                buildJsonObject {
                    put("verse_no", row.verseNo)
                    put("text", row.text)
                }
            )
        }
    }
}

@kotlinx.serialization.Serializable
private data class ChapterTextRow(val verse_no: Int, val text: String? = null)
