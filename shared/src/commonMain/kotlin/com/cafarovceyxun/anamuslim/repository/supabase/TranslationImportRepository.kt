package com.cafarovceyxun.anamuslim.repository.supabase

import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Bir ayənin idxal sətri. [note] `null`-dursa ayənin mövcud qeydinə toxunulmur. */
data class ImportRow(val verseNo: Int, val text: String, val note: String? = null)

/** Surədəki hazırkı sətir — önizləmədə müqayisə üçün. */
data class ChapterVerseText(val text: String, val note: String?)

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

    /**
     * Haşiyə qeydlərini kitabın qeyd sütununa (`note`/`note_alt`) yazır və yazılan sətir sayını
     * qaytarır. `import_translation_text` RPC-si yalnız mətni yazır, ona görə qeydlər ayrıca gedir:
     * dipnotu olan ayələr azdır (Bəqərədə ~80), hər biri bir `update`-dir, paralel göndərilir.
     *
     * Cədvəl admin-only RLS ilə qorunur və bloklanan `update` xəta yox, boş cavab verir — ona görə
     * hər sorğu `select()` ilə gedir və sayılan yalnız həqiqətən dəyişən sətirlərdir.
     */
    suspend fun importNotes(
        book: TranslationCatalogBook,
        chapterNo: Int,
        notes: Map<Int, String>,
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            notes.entries.chunked(NOTE_BATCH).sumOf { batch ->
                coroutineScope {
                    batch.map { (verseNo, note) ->
                        async {
                            SupabaseProvider.client.from(TABLE)
                                .update(buildJsonObject { put(book.noteColumn, note) }) {
                                    select(Columns.list("verse_no"))
                                    filter {
                                        eq("slug", BASE_SLUG)
                                        eq("chapter_no", chapterNo)
                                        eq("verse_no", verseNo)
                                    }
                                }
                                .decodeList<VerseNoRow>()
                                .size
                        }
                    }.awaitAll().sum()
                }
            }
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
    suspend fun chapterTexts(book: TranslationCatalogBook, chapterNo: Int): Map<Int, ChapterVerseText> =
        withContext(Dispatchers.IO) {
            runCatching {
                SupabaseProvider.client.from(TABLE)
                    .select(Columns.list("verse_no", "text:${book.source_column}", "note:${book.noteColumn}")) {
                        filter {
                            eq("slug", BASE_SLUG)
                            eq("chapter_no", chapterNo)
                        }
                    }
                    .decodeList<ChapterTextRow>()
                    .mapNotNull { row ->
                        row.text?.takeIf { it.isNotBlank() }?.let {
                            row.verse_no to ChapterVerseText(it, row.note?.takeIf { n -> n.isNotBlank() })
                        }
                    }
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
private data class ChapterTextRow(val verse_no: Int, val text: String? = null, val note: String? = null)

@kotlinx.serialization.Serializable
private data class VerseNoRow(val verse_no: Int)

private const val NOTE_BATCH = 8
