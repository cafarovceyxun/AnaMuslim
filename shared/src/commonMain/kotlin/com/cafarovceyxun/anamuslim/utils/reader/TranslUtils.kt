package com.cafarovceyxun.anamuslim.utils.reader

import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

object TranslUtils {
    const val TRANSL_INFO_FILE_NAME = "manifest.json"
    const val KEY_TRANSLATIONS = "key.translations"
    const val KEY_NEW_TRANSLATIONS = "key.translations_new"
    
    const val TRANSL_FILE_NAME_FORMAT = "translation_%d_%s_%s.json"
    const val TRANSL_AVAILABLE_DOWNLOADS_FILE_NAME = "available_downloads.json"

    const val TRANSL_SLUG_DEFAULT = "az"
    const val TRANSL_MAX_SELECTION_LIMIT = 6

    @JvmStatic
    fun defaultTranslationSlugs(): Set<String> {
        return hashSetOf(TRANSL_SLUG_DEFAULT)
    }

    @JvmStatic
    fun isTransliteration(slug: String): Boolean {
        return slug.contains("transliteration")
    }
    
    @JvmField
    var isPrebuilt: (String) -> Boolean = { false }

    /**
     * Applies a selection change to [slugSet], returning false (leaving the set untouched) when
     * selecting would exceed [TRANSL_MAX_SELECTION_LIMIT].
     *
     * The pure half of the former `TranslUtilsAndroid.resolveSelectionChange`, which also popped
     * the limit dialog itself; telling the user is now the caller's job, so this stays testable and
     * Context-free.
     */
    fun resolveSelectionChange(
        slugSet: MutableSet<String>,
        slug: String,
        isSelected: Boolean,
    ): Boolean {
        if (isSelected) {
            if (slugSet.size >= TRANSL_MAX_SELECTION_LIMIT) return false
            slugSet.add(slug)
        } else {
            slugSet.remove(slug)
        }
        return true
    }

    var DIR_NAME: String = "translations"

    var DIR_NAME_4_AVAILABLE_DOWNLOADS: String = "available_translation_downloads"

    @JvmStatic
    fun prepareTranslInfoPathForSpecificLangNSlug(langCode: String, translSlug: String): String {
        return "$langCode/$translSlug/$TRANSL_INFO_FILE_NAME"
    }

    @JvmStatic
    fun prepareTranslPathForSpecificLangNSlug(translId: Int, langCode: String, translSlug: String): String {
        val filename = TRANSL_FILE_NAME_FORMAT.replace("%d", translId.toString())
            .replace("%s", langCode, true)
        return "$langCode/$translSlug/$filename"
    }
}
