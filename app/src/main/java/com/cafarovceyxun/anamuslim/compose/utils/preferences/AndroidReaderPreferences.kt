package com.cafarovceyxun.anamuslim.compose.utils.preferences

import android.content.Context
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptUtils
import com.cafarovceyxun.anamuslim.utils.reader.ReaderTextSizeUtils
import com.cafarovceyxun.anamuslim.utils.reader.TranslUtils
import com.cafarovceyxun.anamuslim.utils.univ.Keys

/**
 * ReaderPreferences-in Android-ə məxsus (Context tələb edən) hissəsi: köhnə SharedPreferences-dən
 * migrasiya. `repairStoredPreferences` platforma-neytral olduğu üçün shared `ReaderPreferences`-ə
 * köçdü.
 */
object AndroidReaderPreferences {

    private const val LEGACY_SP_READER = "sp_reader"
    private const val LEGACY_SP_TEXT_STYLE = "sp_reader_text"
    private const val LEGACY_SP_TRANSL = "sp_reader_translations"
    private const val LEGACY_SP_SCRIPT = "sp_reader_script"

    suspend fun migrateFromLegacy(context: Context) {
        if (DataStoreManager.readFirst(ReaderPreferences.KEY_LEGACY_MIGRATED)) return

        val appCtx = context.applicationContext
        val spReader = appCtx.getSharedPreferences(LEGACY_SP_READER, Context.MODE_PRIVATE)
        
        if (spReader.contains(Keys.READER_KEY_ARABIC_TEXT_ENABLED)) {
            DataStoreManager.write(
                ReaderPreferences.KEY_ARABIC_TEXT_ENABLED,
                spReader.getBoolean(Keys.READER_KEY_ARABIC_TEXT_ENABLED, true)
            )
        }
        
        if (spReader.contains(Keys.READER_KEY_AUTO_SCROLL_SPEED)) {
            DataStoreManager.write(
                ReaderPreferences.KEY_AUTO_SCROLL_SPEED,
                spReader.getFloat(Keys.READER_KEY_AUTO_SCROLL_SPEED, 7f)
            )
        }

        val spTextStyle = appCtx.getSharedPreferences(LEGACY_SP_TEXT_STYLE, Context.MODE_PRIVATE)
        if (spTextStyle.contains(ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_ARABIC)) {
            DataStoreManager.write(
                ReaderPreferences.KEY_TEXT_SIZE_MULT_ARABIC,
                spTextStyle.getFloat(
                    ReaderTextSizeUtils.KEY_TEXT_SIZE_MULT_ARABIC,
                    ReaderTextSizeUtils.TEXT_SIZE_MULT_AR_DEFAULT
                )
            )
        }

        val spTransl = appCtx.getSharedPreferences(LEGACY_SP_TRANSL, Context.MODE_PRIVATE)
        if (spTransl.contains(TranslUtils.KEY_TRANSLATIONS)) {
            val legacy = spTransl.getStringSet(TranslUtils.KEY_TRANSLATIONS, null)
            if (legacy != null) {
                DataStoreManager.write(ReaderPreferences.KEY_TRANSLATIONS, HashSet(legacy))
            }
        }

        val spScript = appCtx.getSharedPreferences(LEGACY_SP_SCRIPT, Context.MODE_PRIVATE)
        if (spScript.contains(QuranScriptUtils.KEY_SCRIPT)) {
            val script = spScript.getString(QuranScriptUtils.KEY_SCRIPT, null)
            DataStoreManager.write(ReaderPreferences.KEY_SCRIPT, script ?: QuranScriptUtils.SCRIPT_DEFAULT)
        }

        DataStoreManager.write(ReaderPreferences.KEY_LEGACY_MIGRATED, true)
    }
}
