package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.copiedToClipboard
import com.cafarovceyxun.anamuslim.utils.supabase.Hadith
import org.jetbrains.compose.resources.stringResource

/** Mötərizəli izahı mətndən çıxarır — `formatHadithText`-in düz mətn qarşılığı. */
private val ParentheticalRegex = Regex("\\(([\\s\\S]*?)\\)")
private val WhitespaceRegex = Regex("\\s+")

/**
 * Hədis kartını **basılı saxlayanda** panoya düşən mətn.
 *
 * Paylaşma vərəqindən ([HadithShareSheet]) fərqli olaraq burada seçim yoxdur: jest bir anlıqdır, ona
 * görə mətn **kartda görünənin** eynisidir — hansı dillər açıqdırsa onlar, mötərizə ayarı nədirsə o.
 * Özündən bir şey əlavə etmir ki, istifadəçi kopyaladığını gözü ilə gördüyü ilə tutuşdura bilsin.
 * Daha incə variant (yalnız ərəbcə, qeyd, əlavə qaynaq, rəvayət seçimi) paylaşma vərəqində qalır.
 *
 * Qaynaq yalnız kartda göstərilirsə gedir: sitat gətirən istifadəçiyə hədisin haradan olduğu
 * lazımdır, kartda söndürülübsə də onu pano ilə geri qaytarmaq ayarı yalan çıxarardı.
 */
internal fun buildHadithCopyText(
    hadith: Hadith,
    includeArabic: Boolean,
    includeTranslation: Boolean,
    includeSource: Boolean,
    showParentheses: Boolean,
): String = buildString {
    if (includeArabic && hadith.text_ar.isNotBlank()) {
        append(hadith.text_ar.trim()).append("\n\n")
    }

    if (includeTranslation && hadith.text_az.isNotBlank()) {
        append(hadith.text_az.withParentheses(showParentheses)).append("\n\n")
    }

    if (includeSource) {
        hadith.source?.takeIf { it.isNotBlank() }?.let { append(it.trim()) }
    }
}.trim()

/**
 * Kartın uzun basma əməli: mətni qurur, panoya yazır və istifadəçini xəbərdar edir.
 *
 * Mesaj mətni **kompozisiyada** oxunur, jestin içində yox: `getString` suspend-dir və jest
 * callback-i suspend deyil — CLAUDE.md-dəki «toast mətnini `stringResource` ilə əvvəlcədən oxu»
 * qaydası. Boş mətn (hər iki dil söndürülüb) heç nə etmir: panonu boşaltmaq və «kopyalandı» demək
 * istifadəçini aldadardı.
 */
@Composable
internal fun rememberHadithCopyAction(
    hadith: Hadith,
    includeArabic: Boolean,
    includeTranslation: Boolean,
    includeSource: Boolean,
    showParentheses: Boolean,
): () -> Unit {
    val copiedMessage = stringResource(Res.string.copiedToClipboard)

    val text = remember(hadith, includeArabic, includeTranslation, includeSource, showParentheses) {
        buildHadithCopyText(
            hadith = hadith,
            includeArabic = includeArabic,
            includeTranslation = includeTranslation,
            includeSource = includeSource,
            showParentheses = showParentheses,
        )
    }

    return remember(text, copiedMessage) {
        {
            if (text.isNotBlank()) {
                PlatformUtils.copyToClipboard(text)
                PlatformUtils.showClipboardMessage(copiedMessage)
            }
        }
    }
}

/**
 * Ayar bağlıdırsa mötərizəli izahı atır.
 *
 * Boşluqlar sonra bir dəfə yığışdırılır: `(…)` sətrin ortasından çıxanda arxasında ikiqat boşluq
 * qalır və panoya düşən mətn yamaq kimi görünür.
 */
private fun String.withParentheses(keep: Boolean): String =
    if (keep) trim() else replace(ParentheticalRegex, "").replace(WhitespaceRegex, " ").trim()
