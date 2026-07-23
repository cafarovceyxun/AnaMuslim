package com.cafarovceyxun.anamuslim.viewModels

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cafarovceyxun.anamuslim.compose.utils.preferences.DataStoreManager
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.sharedPrefs.SPFavouriteChapters
import com.cafarovceyxun.anamuslim.utils.univ.Keys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * One-time migration of the favourite-chapters list from the legacy Android SharedPreferences
 * store ([SPFavouriteChapters]) into the shared KMP DataStore. Android-only (reads SharedPreferences);
 * the target write goes through the shared [DataStoreManager], keyed identically to
 * [ReaderIndexViewModel]. Invoked from `QuranApp.onCreate()`.
 */
object ReaderIndexFavouritesMigration {
    private val KEY = stringPreferencesKey(Keys.FAVOURITE_CHAPTERS)

    fun migrate(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val old = SPFavouriteChapters.getFavouriteChapters(context)

            if (old.isEmpty()) return@launch

            try {
                DataStoreManager.write(
                    KEY,
                    Json.encodeToString(old)
                )
            } catch (e: Exception) {
                AppLogger.saveError(
                    e,
                    "migrateFavouriteChapters",
                )
            }
        }
    }
}
