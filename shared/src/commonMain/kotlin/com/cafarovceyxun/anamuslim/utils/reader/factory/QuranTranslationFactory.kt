/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 6/6/2022.
 * All rights reserved.
 */

package com.cafarovceyxun.anamuslim.utils.reader.factory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.cafarovceyxun.anamuslim.api.models.translation.TranslationBookInfoModel
import com.cafarovceyxun.anamuslim.components.quran.subcomponents.Translation
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.db.translation.QuranTranslationStore
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.utils.reader.TranslUtils
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from

/**
 * This factory prepares contents of translations for the requesters.
 * The content may be [TranslationBookInfoModel] or the actual translation contents.
 *
 * All SQL now goes through the multiplatform [QuranTranslationStore] (shared process-wide connection
 * from [RepositoryProvider]); this class keeps the app-facing concerns — premiership/transliteration
 * ordering, [ReaderPreferences]-backed overloads, and the Supabase edit path.
 * */
class QuranTranslationFactory : AutoCloseable {
    companion object {
        @Composable
        fun remember(): QuranTranslationFactory {
            val factory = remember {
                QuranTranslationFactory()
            }

            DisposableEffect(Unit) {
                onDispose {
                    factory.close()
                }
            }

            return factory
        }
    }

    val store: QuranTranslationStore = RepositoryProvider.quranTranslationStore

    override fun close() {
        // Shared process-wide store; closing it from one factory would break all callers.
    }

    fun deleteTranslation(translSlug: String) {
        store.deleteTranslation(translSlug)
    }

    /**
     * Check if translation table for the given slug exists.
     * */
    fun isTranslationDownloaded(slug: String): Boolean = store.isTranslationDownloaded(slug)

    /**
     * Gets and prepare an instance of [TranslationBookInfoModel] from the database.
     * If no book is found in the database, an empty instance is returned.
     * @param slug The slug of the book.
     * */
    fun getTranslationBookInfo(slug: String): TranslationBookInfoModel {
        return store.getBooksInfo(setOf(slug))[slug]
            ?: TranslationBookInfoModel("")
    }

    /**
     * Gets a map of [TranslationBookInfoModel] in [getAvailableTranslationBooksInfo] excluding the built-in translations.
     * Only info for translations which are downloaded by the user are returned.
     */
    fun getDownloadedTranslationBooksInfo(): Map<String, TranslationBookInfoModel> {
        return getTranslationBooksInfoValidated().filterKeys { !TranslUtils.isPrebuilt(it) }
    }

    /**
     * Gets and prepare instances of [TranslationBookInfoModel] from the database for all `available` slugs.
     * Here the meaning of `available` is - all books stored in the database.
     * When a book is downloaded from the server, then its information along with its content is stored in the database.
     * Then the information is included in `available` slugs.
     * @return The returned value is a [Map] where the key is the corresponding slug.
     * */
    fun getAvailableTranslationBooksInfo(): Map<String, TranslationBookInfoModel> {
        return getTranslationBooksInfoValidated()
    }

    /**
     * Gets and prepare an instances of [TranslationBookInfoModel] from the database for the given slugs and also validating the premiership.
     * @param slugs If it is empty then empty map is returned. If null is passed as the slugs, all valid books are returned.
     * @return The returned value is a [Map] where the key is the corresponding slug.
     * */
    fun getTranslationBooksInfoValidated(slugs: Set<String>? = null): Map<String, TranslationBookInfoModel> {
        if (slugs?.isEmpty() == true) return HashMap()

        return store.getBooksInfo(slugs)
    }

    suspend fun getTranslationsSingleVerse(chapNo: Int, verseNo: Int): List<Translation> {
        return getTranslationsSingleVerse(ReaderPreferences.getTranslations(), chapNo, verseNo)
    }

    fun getTranslationsSingleSlugVerse(slug: String, chapNo: Int, verseNo: Int): Translation? {
        return getTranslationsSingleVerse(
            setOf(slug),
            chapNo,
            verseNo
        ).firstOrNull()
    }

    /**
     *      example:
     *      [<Transl-of-Slug1>, <Transl-of-Slug2>, <Transl-of-Slug3>] -> verse 1:1
     * */
    fun getTranslationsSingleVerse(
        slugs: Set<String>,
        chapNo: Int,
        verseNo: Int
    ): List<Translation> {
        val nSlugs = sortTranslationSlugs(validatePremierShip(slugs))

        val transls = ArrayList<Translation>()

        for ((slugIndex, slug) in nSlugs.withIndex()) {
            val translations = getVersesSafe(slug) { store.getVersesSingle(slug, chapNo, verseNo) }
            if (translations.isNotEmpty()) {
                transls.add(slugIndex, translations[0])
            }
        }

        return transls
    }

    suspend fun getTranslationsVerseRange(
        chapNo: Int,
        fromVerse: Int,
        toVerse: Int
    ): List<List<Translation>> {
        return getTranslationsVerseRange(
            ReaderPreferences.getTranslations(),
            chapNo,
            fromVerse,
            toVerse
        )
    }

    /**
     *       example:
     *       [
     *            [<Transl-of-Slug1>, <Transl-of-Slug2>, <Transl-of-Slug3>] -> verse 1:1
     *            [<Transl-of-Slug1>, <Transl-of-Slug2>, <Transl-of-Slug3>] -> verse 1:2
     *            [<Transl-of-Slug1>, <Transl-of-Slug2>, <Transl-of-Slug3>] -> verse 1:3
     *            [<Transl-of-Slug1>, <Transl-of-Slug2>, <Transl-of-Slug3>] -> verse 1:4
     *       ]
     * */
    fun getTranslationsVerseRange(
        slugs: Set<String>?,
        chapNo: Int,
        fromVerse: Int,
        toVerse: Int
    ): List<List<Translation>> {
        val transls = List(toVerse - fromVerse + 1) { ArrayList<Translation>() }.toMutableList()

        if (slugs.isNullOrEmpty()) {
            return transls
        }

        val nSlugs = sortTranslationSlugs(validatePremierShip(slugs))

        // This loop creates list of translations packed with list of slugs as shown in the example.
        for ((slugIndex, slug) in nSlugs.withIndex()) {
            val translations =
                getVersesSafe(slug) { store.getVersesRange(slug, chapNo, fromVerse, toVerse) }
            if (translations.isNotEmpty()) {
                for ((translIndex, transl) in translations.withIndex()) {
                    transls[translIndex].add(slugIndex, transl)
                }
            }
        }

        return transls
    }

    /*
    * The returned verses will be sorted by verse number regardless of order of the passed verse numbers..
    * */
    suspend fun getTranslationsDistinctVerses(chapNo: Int, vararg verses: Int): List<List<Translation>> {
        return getTranslationsDistinctVerses(
            ReaderPreferences.getTranslations(),
            chapNo,
            *verses
        )
    }

    /*
    * The returned verses will be sorted by verse number regardless of order of the passed verse numbers..
    * */
    fun getTranslationsDistinctVerses(
        slugs: Set<String>,
        chapNo: Int,
        vararg verses: Int
    ): List<List<Translation>> {
        val translationGroups = List(verses.size) {
            ArrayList<Translation>()
        }.toMutableList()

        if (slugs.isEmpty()) {
            return translationGroups
        }

        val nSlugs = sortTranslationSlugs(validatePremierShip(slugs))

        for ((slugIndex, slug) in nSlugs.withIndex()) {
            val translations =
                getVersesSafe(slug) { store.getVersesDistinct(slug, chapNo, verses) }
            if (translations.isNotEmpty()) {
                for ((translIndex, transl) in translations.withIndex()) {
                    translationGroups[translIndex].add(slugIndex, transl)
                }
            }
        }

        return translationGroups
    }

    fun updateTranslation(
        translSlug: String,
        chapterNo: Int,
        verseNo: Int,
        newText: String,
        newNote: String? = null
    ) {
        store.updateTranslation(translSlug, chapterNo, verseNo, newText, newNote)
    }

    suspend fun updateSupabaseTranslation(
        translSlug: String,
        chapterNo: Int,
        verseNo: Int,
        newText: String,
        newNote: String? = null
    ) {
        if (translSlug != "az") return

        val updateData = mutableMapOf<String, String>()
        updateData["text"] = newText
        if (newNote != null) {
            updateData["note"] = newNote
        }

        try {
            SupabaseProvider.client.from("translations").update(updateData) {
                filter {
                    eq("slug", "az")
                    eq("chapter_no", chapterNo)
                    eq("verse_no", verseNo)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sortTranslationSlugs(slugs: Set<String>): Set<String> {
        val transliterations = ArrayList<String>()
        val nonTransliterations = ArrayList<String>()

        slugs.forEach { slug ->
            if (TranslUtils.isTransliteration(slug)) {
                transliterations.add(slug)
            } else {
                nonTransliterations.add(slug)
            }
        }

        return (transliterations + nonTransliterations).toSet()
    }

    private fun validatePremierShip(translSlugs: Set<String>): Set<String> {
        if (translSlugs.isEmpty()) return HashSet()
        return getTranslationBooksInfoValidated(translSlugs).keys
    }

    /** Verse reads for a missing/corrupt slug table throw; keep the old behaviour of returning empty. */
    private inline fun getVersesSafe(slug: String, block: () -> List<Translation>): List<Translation> {
        return try {
            block()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getTranslationsBulkForSearch(
        slugs: Set<String>,
        verseKeys: List<Pair<Int, Int>>
    ): Map<String, Map<Pair<Int, Int>, Translation>> {
        return store.getTranslationsBulkForSearch(slugs, verseKeys)
    }
}
