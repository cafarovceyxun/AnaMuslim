package com.cafarovceyxun.anamuslim.compose.components.reader.navigator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_open
import com.cafarovceyxun.anamuslim.resources.goToVerseRangeReference
import com.cafarovceyxun.anamuslim.resources.goToVerseReference
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Süzgəc qutusuna yazılmış istinad: surə və bir (və ya bir neçə) ayə. */
data class VerseReference(
    val chapterNo: Int,
    val fromVerse: Int,
    val toVerse: Int,
) {
    val isRange: Boolean get() = toVerse > fromVerse
}

/**
 * «1:7», «1.7», «1/7», «1-7» və aralıq forması «1:1-5» — süzgəc qutusuna yazılan istinad.
 *
 * ⚠️ Aralıq yalnız **`:` / `.` / `/`** ayırıcısı ilə tanınır. `-` iki mənalıdır: «1-5» yazan
 * istifadəçi çox vaxt «1-ci surənin 5-ci ayəsi»ni nəzərdə tutur (qutuda surə axtarır), halbuki
 * «1:1-5» yazanda niyyəti aralıqdır. Ona görə `-` yalnız sadə formada ayırıcı sayılır, aralığı isə
 * ikinci nömrədən sonra gələn `-` açır.
 *
 * Ayənin surədə həqiqətən olub-olmadığı burada yoxlanmır (ayə sayı yalnız bazadadır) — çağıran
 * tərəf onsuz da mövcud aralığa qısır.
 */
fun parseVerseReference(query: String): VerseReference? {
    val match = Regex("^(\\d{1,3})\\s*([:./])\\s*(\\d{1,3})(?:\\s*[-–]\\s*(\\d{1,3}))?$|^(\\d{1,3})\\s*-\\s*(\\d{1,3})$")
        .find(query.trim()) ?: return null

    val groups = match.groupValues
    val chapterNo = (groups[1].takeIf { it.isNotEmpty() } ?: groups[5]).toIntOrNull() ?: return null
    val fromVerse = (groups[3].takeIf { it.isNotEmpty() } ?: groups[6]).toIntOrNull() ?: return null
    val toVerse = groups[4].toIntOrNull() ?: fromVerse

    if (!QuranMeta.isChapterValid(chapterNo)) return null
    if (fromVerse <= 0 || toVerse < fromVerse) return null

    return VerseReference(chapterNo, fromVerse, toVerse)
}

/**
 * [parseVerseReference]-in tək ayə istəyən çağıranları üçün: aralıq yazılıbsa **başlanğıc** ayəsi
 * qaytarılır — naviqator onsuz da bir nöqtəyə enir.
 */
fun parseChapterVerseQuery(query: String): Pair<Int, Int>? =
    parseVerseReference(query)?.let { it.chapterNo to it.fromVerse }

/**
 * Naviqatorun süzgəc qutusuna «1:7» yazılanda çıxan sətir: **birbaşa həmin ayəyə** aparır.
 *
 * Nə üçün ayrıca sətir, siyahının süzülməsi kifayət etmir: siyahı surə göstərir, surəyə toxunmaq
 * isə onun **əvvəlini** açır — istifadəçi yazdığı ayəni yenidən əl ilə axtarmalı olurdu. Sətir
 * hər naviqator tabında eynidir (surə, cüz, hizb, səhifə): istinad yazılışı hamısında eyni mənanı
 * daşıyır.
 */
@Composable
fun VerseJumpRow(
    chapterNo: Int,
    verseNo: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Aralıq yazılıbsa son ayə — etiket «1:1-5» kimi oxunur. */
    toVerseNo: Int = verseNo,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = shapes.small,
        color = colorScheme.primaryContainer.alpha(0.35f),
        contentColor = colorScheme.onSurface,
        border = BorderStroke(1.dp, colorScheme.primary.alpha(0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_open),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = colorScheme.primary,
            )

            Text(
                text = if (toVerseNo > verseNo) {
                    stringResource(Res.string.goToVerseRangeReference, chapterNo, verseNo, toVerseNo)
                } else {
                    stringResource(Res.string.goToVerseReference, chapterNo, verseNo)
                },
                style = typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface.alpha(0.9f),
            )
        }
    }
}
