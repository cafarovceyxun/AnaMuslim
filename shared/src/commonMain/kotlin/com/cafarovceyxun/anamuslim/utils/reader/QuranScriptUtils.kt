package com.cafarovceyxun.anamuslim.utils.reader

import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.preview_dk_indopak
import com.cafarovceyxun.anamuslim.resources.preview_kfqpc_v1
import com.cafarovceyxun.anamuslim.resources.preview_kfqpc_v2
import com.cafarovceyxun.anamuslim.resources.preview_kfqpc_v4_tajweed
import com.cafarovceyxun.anamuslim.resources.preview_kfqpc_v4_tajweed_dark
import com.cafarovceyxun.anamuslim.resources.preview_uthmani_hafs
import org.jetbrains.compose.resources.DrawableResource
import androidx.compose.runtime.Composable
import com.cafarovceyxun.anamuslim.compose.utils.appFallbackLanguageCodes
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import com.cafarovceyxun.anamuslim.utils.univ.ScriptFiles
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

object QuranScriptUtils {
    var FONTS_DIR_NAME: String = "script_fonts"
    var SCRIPT_DIR_NAME: String = "scripts"

    const val KEY_SCRIPT = "key.script"

    const val SCRIPT_UTHMANI = "uthmani"
    const val SCRIPT_PDMS_ISLAMIC = "pdms_islamic"

    const val SCRIPT_DK_INDOPAK = "dk_indopak"
    const val SCRIPT_NASTALEEQ_INDOPAK = "nastaleeq_indopak"
    const val SCRIPT_KFQPC_V1 = "kfqpc_v1"
    const val SCRIPT_KFQPC_V2 = "kfqpc_v2"
    const val SCRIPT_KFQPC_V4 = "kfqpc_v4_tajweed"

    const val SCRIPT_DEFAULT = SCRIPT_UTHMANI

    @JvmStatic
    fun SCRIPT_NAMES(scriptCode: String): Map<String, String> {
        return when (scriptCode) {
            SCRIPT_NASTALEEQ_INDOPAK,
            SCRIPT_DK_INDOPAK -> mapOf(
                "en" to "IndoPak",
                "az" to "Hind-Pak",
                "tr" to "Hint Paketi",
                "ru" to "ИндоПак",
            )

            SCRIPT_UTHMANI -> mapOf(
                "en" to "Uthmanic",
                "az" to "Osmanlı",
                "tr" to "Osmanca",
                "ru" to "Усмани",
            )

            SCRIPT_PDMS_ISLAMIC -> mapOf(
                "en" to "PDMS Islamic",
                "az" to "PDMS İslam",
                "tr" to "PDMS İslam",
                "ru" to "PDMS Ислам",
            )

            SCRIPT_KFQPC_V1,
            SCRIPT_KFQPC_V2,
            SCRIPT_KFQPC_V4 -> {
                val suffix = when {
                    scriptCode == SCRIPT_KFQPC_V1 -> "V1"
                    scriptCode == SCRIPT_KFQPC_V2 -> "V2"
                    scriptCode == SCRIPT_KFQPC_V4 -> "V4 (Tajweed)"
                    else -> ""
                }

                mapOf(
                    "en" to "King Fahd Complex $suffix",
                    "az" to "Kral Fəhd Kompleksi $suffix",
                    "tr" to "Kral Fehd Kompleksi $suffix",
                    "ru" to "Комплекс короля Фахда $suffix",
                )
            }

            else -> emptyMap()
        }
    }

    @JvmField
    val VARIANT_NAMES: Map<QuranScriptVariant, Map<String, String>> = mapOf(
        QuranScriptVariant.INDOPAK_15 to mapOf(
            "en" to "Indopak 15 lines",
            "az" to "Hind-Pak 15 sətir",
            "tr" to "Hind-Pak 15 satır",
            "ru" to "ИндоПак 15 строк",
        ),
        QuranScriptVariant.INDOPAK_16 to mapOf(
            "en" to "Indopak 16 lines",
            "az" to "Hind-Pak 16 sətir",
            "tr" to "Hind-Pak 16 satır",
            "ru" to "ИндоПак 16 строк",
        )
    )

    @JvmStatic
    fun availableScripts() = mapOf(
        SCRIPT_UTHMANI to listOf(),
        SCRIPT_KFQPC_V1 to listOf(),
        SCRIPT_KFQPC_V2 to listOf(),
        SCRIPT_KFQPC_V4 to listOf(),
        SCRIPT_DK_INDOPAK to listOf(
            QuranScriptVariant.INDOPAK_15,
            QuranScriptVariant.INDOPAK_16
        ),
    )

    @JvmStatic
    fun validatePreferredScript(scriptCode: String): String {
        if (!availableScripts().contains(scriptCode)) {
            return SCRIPT_DEFAULT
        }

        return scriptCode
    }

    data class DownloadedFontsInfo(
        val totalFonts: Int,
        val downloaded: Int,
        val remaining: Int
    )

    /** One page font per mus'haf page. */
    const val KFQPC_PAGE_COUNT = 604

    /**
     * How many of the 604 KFQPC page fonts are on disk for [scriptSlug].
     *
     * Pure common code: the installer ([com.cafarovceyxun.anamuslim.utils.managers.ScriptFontInstaller])
     * writes these files and [FontResolver] reads them on both platforms, so counting them needed no
     * platform hook — the old `getDownloadedFontsCount` Int hook was registered on Android only,
     * which made iOS report "nothing downloaded" even right after a successful download.
     *
     * The directory is listed once instead of stat-ing three candidate paths per page (the old
     * Android implementation did ~1800 file lookups per call, on the caller's thread).
     */
    @JvmStatic
    fun getKFQPCFontDownloadedCount(scriptSlug: String): DownloadedFontsInfo {
        val presentFiles = try {
            AppFileSystem.list(ScriptFiles.kfqpcScriptFontDir(scriptSlug))
                .filter { (AppFileSystem.size(it) ?: 0L) > 0L }
                .map { it.name }
                .toSet()
        } catch (_: Exception) {
            emptySet()
        }

        val hasDark = scriptSlug.getQuranScriptFontHasDark()

        val downloaded = (1..KFQPC_PAGE_COUNT).count { pageNo ->
            QuranScriptPlatformHooks.formatFontFilename(pageNo, false) in presentFiles ||
                (hasDark && QuranScriptPlatformHooks.formatFontFilename(pageNo, true) in presentFiles) ||
                QuranScriptPlatformHooks.formatFontFilenameOld(pageNo) in presentFiles
        }

        return DownloadedFontsInfo(KFQPC_PAGE_COUNT, downloaded, KFQPC_PAGE_COUNT - downloaded)
    }
}

fun String.isKFQPCScript(): Boolean = when (this) {
    QuranScriptUtils.SCRIPT_KFQPC_V1,
    QuranScriptUtils.SCRIPT_KFQPC_V2,
    QuranScriptUtils.SCRIPT_KFQPC_V4 -> true
    else -> false
}

fun String.isQuranAtlasScript(): Boolean = when (this) {
    QuranScriptUtils.SCRIPT_UTHMANI,
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> true
    else -> false
}

fun String.isPrebuiltAtlas(): Boolean = when (this) {
    QuranScriptUtils.SCRIPT_UTHMANI -> true
    else -> false
}

fun String.toAtlasBundleDownloadKey(): String = when (this) {
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> "dk_indopak_v2"
    else -> this
}

fun String.mushafShowsRuledPageDecoration(): Boolean = when (this) {
    QuranScriptUtils.SCRIPT_NASTALEEQ_INDOPAK,
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> true
    else -> false
}

fun String.getQuranScriptName(): String {
    val mapToQuery = QuranScriptUtils.SCRIPT_NAMES(this)
    return appFallbackLanguageCodes()
        .firstNotNullOfOrNull { mapToQuery[it] }
        ?: ""
}

/**
 * Base verse text size (sp) per script. These were the app's `R.dimen` values (all declared in
 * `sp`); moved here as plain constants so the reader text style needs no Android `Context` — the
 * old `context.getDimension(...)` -> `toSp()` round-trip just recovered these numbers.
 */
fun String.getQuranScriptVerseTextSizeSp(useSmallSize: Boolean): Float = when (this) {
    QuranScriptUtils.SCRIPT_NASTALEEQ_INDOPAK,
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> if (useSmallSize) 23f else 28f
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> if (useSmallSize) 24f else 30f
    QuranScriptUtils.SCRIPT_KFQPC_V2,
    QuranScriptUtils.SCRIPT_KFQPC_V4 -> if (useSmallSize) 17f else 22f
    else -> if (useSmallSize) 23f else 28f
}

fun String.getQuranScriptFontRes(isDark: Boolean): Int =
    QuranScriptPlatformHooks.getFontRes?.invoke(this, isDark) ?: 0

/**
 * Preview image for a script. Pure common code — every preview drawable already lives in shared
 * `composeResources`, so this needs no platform hook (the old `getPreviewRes` Int hook is gone).
 */
fun String.getQuranScriptPreview(isDark: Boolean): DrawableResource = when (this) {
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> Res.drawable.preview_dk_indopak
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> Res.drawable.preview_kfqpc_v1
    QuranScriptUtils.SCRIPT_KFQPC_V2 -> Res.drawable.preview_kfqpc_v2
    QuranScriptUtils.SCRIPT_KFQPC_V4 ->
        if (isDark) Res.drawable.preview_kfqpc_v4_tajweed_dark
        else Res.drawable.preview_kfqpc_v4_tajweed
    else -> Res.drawable.preview_uthmani_hafs
}

fun String.getQuranScriptFontPackSizeMb(): Pair<Int, Int> = when (this) {
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> Pair(1213, 1213)
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> Pair(52000, 90000)
    QuranScriptUtils.SCRIPT_KFQPC_V2 -> Pair(129000, 200000)
    QuranScriptUtils.SCRIPT_KFQPC_V4 -> Pair(132000, 320000)
    else -> Pair(0, 0)
}

fun String.getQuranScriptFontHasDark(): Boolean = when (this) {
    QuranScriptUtils.SCRIPT_KFQPC_V4 -> true
    else -> false
}

fun String.toQuranMushafId(variant: QuranScriptVariant?): Int = when (this) {
    QuranScriptUtils.SCRIPT_NASTALEEQ_INDOPAK,
    QuranScriptUtils.SCRIPT_DK_INDOPAK -> {
        when (variant) {
            QuranScriptVariant.INDOPAK_15 -> 3
            else -> 4
        }
    }
    QuranScriptUtils.SCRIPT_KFQPC_V2,
    QuranScriptUtils.SCRIPT_KFQPC_V4,
    QuranScriptUtils.SCRIPT_UTHMANI -> 1
    QuranScriptUtils.SCRIPT_KFQPC_V1 -> 5
    else -> 0
}

@Composable
fun rememberQuranMushafId(): Int {
    val scriptCode = ReaderPreferences.observeQuranScript()
    val scriptVariant = ReaderPreferences.observeQuranScriptVariant()
    return scriptCode.toQuranMushafId(scriptVariant)
}

fun QuranScriptVariant.getQuranScriptVariantName(): String {
    val mapToQuery = QuranScriptUtils.VARIANT_NAMES[this] ?: return ""
    return appFallbackLanguageCodes()
        .firstNotNullOfOrNull { mapToQuery[it] }
        ?: ""
}

enum class QuranScriptVariant(val value: String) {
    INDOPAK_16("indopak_16"),
    INDOPAK_15("indopak_15");

    companion object {
        @JvmStatic
        fun fromValue(value: String?): QuranScriptVariant? {
            return entries.find { it.value == value }
        }
    }
}

data class QuranScript(
    val scriptCode: String,
    var variant: QuranScriptVariant?
) {
    fun toMushafId(): Int = scriptCode.toQuranMushafId(variant)

    companion object {
        @JvmStatic
        fun fromRawValues(script: String, variant: String?): QuranScript =
            QuranScript(script, QuranScriptVariant.fromValue(variant))
    }
}
