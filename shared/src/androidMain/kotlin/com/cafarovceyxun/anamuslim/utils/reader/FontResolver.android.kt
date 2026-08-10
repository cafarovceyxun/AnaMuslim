package com.cafarovceyxun.anamuslim.utils.reader

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.res.ResourcesCompat
import com.cafarovceyxun.anamuslim.utils.AndroidPlatformContext
import java.io.File

internal actual fun loadFontFamilyFromFile(path: String): FontFamily? =
    loadTypefaceFromFile(path)?.let { FontFamily(it) }

internal actual fun loadBundledScriptFontFamily(script: String, isDark: Boolean): FontFamily? =
    loadScriptTypeface(script, isDark)?.let { FontFamily(it) }

/**
 * Android-only [Typeface] access to the same fonts [FontResolver] resolves, for the canvas/widget
 * drawing paths (Glance VOTD widget) that paint with a `Typeface` rather than a Compose font.
 *
 * Lives beside the resolver rather than inside it because `Typeface` has no common counterpart;
 * file resolution is still the shared [resolveKfqpcFontPath].
 */
object AndroidFontResolver {
    fun typeface(script: String, pageNo: Int, isDark: Boolean): Typeface? {
        if (script.isQuranAtlasScript()) return null

        return if (script.isKFQPCScript()) {
            resolveKfqpcFontPath(script, pageNo, isDark)?.let { loadTypefaceFromFile(it.toString()) }
        } else {
            loadScriptTypeface(script, isDark)
        }
    }
}

private fun loadTypefaceFromFile(path: String): Typeface? =
    try {
        Typeface.createFromFile(path)
    } catch (_: Exception) {
        null
    }

private fun loadScriptTypeface(script: String, isDark: Boolean): Typeface? {
    val context = AndroidPlatformContext.context
    val fontRes = script.getQuranScriptFontRes(isDark)
    if (fontRes == 0) return null

    return try {
        ResourcesCompat.getFont(context, fontRes)
    } catch (_: Exception) {
        // Some devices fail on the framework font loader; falling back to a raw copy on disk.
        context.loadTypefaceFromResourceFile(fontRes)
    }
}

private fun Context.loadTypefaceFromResourceFile(fontResId: Int): Typeface? {
    val tempFile = File(cacheDir, "font_res_$fontResId.tmp")

    return try {
        resources.openRawResource(fontResId).use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        Typeface.createFromFile(tempFile)
    } catch (_: Exception) {
        null
    }
}
