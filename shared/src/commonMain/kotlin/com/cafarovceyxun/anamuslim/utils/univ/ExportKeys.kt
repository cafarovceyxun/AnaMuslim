package com.cafarovceyxun.anamuslim.utils.univ

class ExportKeys {
    companion object {
        const val VERSION = "version"
        const val EXPORTED_AT = "exportedAt"

        // Fayl bölmələri. `bookmarks` və `settings` eyni zamanda **əhatə açarlarıdır** (ekrandakı
        // kartlar hansı hissəni istədiyini bu adlarla deyir); `history` isə yalnız əhatə açarıdır —
        // faylda iki ayrı massiv kimi durur.
        const val BOOKMARKS = "bookmarks"
        const val SETTINGS = "settings"
        const val HISTORY = "history"

        const val HADITH_BOOKMARKS = "hadithBookmarks"
        const val READ_HISTORY = "readHistory"
        const val HADITH_READ_HISTORY = "hadithReadHistory"

        /** Bütün DataStore ayarlarının tam dumpı — bax [PreferenceBackup]. */
        const val PREFERENCES = "preferences"

        // item keys — v1 `settings` bloku. Format buraxılmış Android build-ləri ilə paylaşıldığı
        // üçün dondurulub: yeni ayar bura yox, [PREFERENCES] dumpına düşür.
        const val LOCALE = "config.lang"
        const val NUMERAL_SYSTEM = "config.numerals"
        const val THEME = "config.theme"
        const val DL_SRC = "config.dlSrc"
        const val APP_TEXT_SCALE = "config.textScale"
        const val READER_AUTO_SCROLL_SPEED = "reader.autoScrollSpeed"
        const val READER_ARABIC_TEXT_ENABLED = "reader.arabicTextEnabled"
        const val READER_MODE = "reader.mode"
        const val READER_DEFAULT_MODE = "reader.defaultMode"
        const val RECITATION_SPEED = "rec.speed"
        const val RECITATION_RECITER = "rec.reciter"
        const val RECITATION_RECITER_TRANSLATION = "rec.reciter_translation"
        const val RECITATION_OPTION_AUDIO = "rec.option_audio"
        const val RECITATION_AUDIO_END_BEHAVIOUR = "rec.audio_end_behaviour"
        const val TEXT_SIZE_MULT_TRANSLATION = "text.size_mult_translation"
        const val TEXT_SIZE_MULT_ARABIC = "text.size_mult_arabic"
        const val SCRIPT_CURRENT = "script.current"
        const val SCRIPT_VARIANT_CURRENT = "script_variant.current"
        const val TRANSLATION_CURRENT = "translation.current"
    }
}
