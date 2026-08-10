package com.cafarovceyxun.anamuslim.utils.reader

/**
 * What is left of the script layer's platform dependency.
 *
 * Only [getFontRes] is still a hook, and only because the bundled (non-KFQPC) Android fonts live in
 * `:app`'s `res/font`, which shared `androidMain` cannot see. Everything else moved to common code:
 * the preview drawables (`getQuranScriptPreview`), the downloaded-font count
 * (`QuranScriptUtils.getKFQPCFontDownloadedCount`) and the hadith Arabic font
 * (`hadithArabicFontFamily`) — each of those Int hooks was registered on Android only, so it
 * silently returned 0/"nothing" on iOS.
 */
object QuranScriptPlatformHooks {
    /** Android `R.font` id for a non-KFQPC script; read by shared `androidMain`'s FontResolver. */
    var getFontRes: ((String, Boolean) -> Int)? = null
    
    var formatFontFilename: (Int, Boolean) -> String = { pageNo, isDark ->
        val suffix = if (isDark) "_dark" else ""
        val padded = pageNo.toString().padStart(3, '0')
        "qpc_page_$padded$suffix.ttf"
    }
    
    var formatFontFilenameOld: (Int) -> String = { pageNo ->
        val padded = pageNo.toString().padStart(3, '0')
        "qpc_page_$padded.TTF"
    }
}
