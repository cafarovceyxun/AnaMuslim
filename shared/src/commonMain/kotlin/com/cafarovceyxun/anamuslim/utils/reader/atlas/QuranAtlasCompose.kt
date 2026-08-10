package com.cafarovceyxun.anamuslim.utils.reader.atlas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.db.ExternalQuranDatabase
import com.cafarovceyxun.anamuslim.utils.reader.isQuranAtlasScript

val LocalQuranAtlasBundle = staticCompositionLocalOf<QuranAtlasBundle?> { null }

/**
 * Embedded tajweed palette for the active atlas, or null when tajweed colouring is off/unavailable.
 * A non-static local so a late palette load recomposes only the readers that consume it.
 */
val LocalTajweedPalette = compositionLocalOf<List<Color>?> { null }

@Composable
fun rememberQuranAtlasBundle(db: ExternalQuranDatabase): QuranAtlasBundle? {
    val script = ReaderPreferences.observeQuranScript()

    if (!script.isQuranAtlasScript()) return null

    val bundleState = produceState<QuranAtlasBundle?>(initialValue = null, script) {
        value = QuranAtlasLoader.getBundle(db, script)
    }

    return bundleState.value
}
