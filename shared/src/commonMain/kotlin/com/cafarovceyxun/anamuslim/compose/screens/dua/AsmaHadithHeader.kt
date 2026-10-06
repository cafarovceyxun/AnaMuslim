package com.cafarovceyxun.anamuslim.compose.screens.dua

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.arabicFontFamily
import com.cafarovceyxun.anamuslim.db.entities.hadith.toModel
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.repository.loadHadithLocation
import com.cafarovceyxun.anamuslim.utils.supabase.DuaSourceRef
import com.cafarovceyxun.anamuslim.utils.supabase.DuaSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * «Allahın doxsan doqquz adı vardır…» hədisi — Muheymin 7, «Zikr və Dua kitabı», № 2815.
 *
 * Əsmaül Hüsnə siyahısının **lap başında** durur: bölmənin özü bu hədisə söykənir. id sabitdir,
 * çünki hədis məzmunu Supabase-dəki sətrin yerli surətidir və id-lər dəyişmir.
 */
private const val NINETY_NINE_NAMES_HADITH_ID = 3208L

private data class AsmaHadith(
    val id: Long,
    val arabic: String,
    val translation: String,
    /** «№ 2815 · Muheymin 7-ci cild» — hədisin ümumi nömrəsi və cildi. */
    val reference: String,
)

/**
 * Kartın yarım vərəqdə açılan qaynağı — [DuaSourcePeekContent] tam hədisi göstərir, çıxarışı
 * (Nəbinin sözü) isə sarı ilə işarələyir; dua və dəlil qaynaqları ilə eyni görünüş.
 */
private data class AsmaHadithSource(
    override val hadith_id: Long,
    override val text_ar: String,
    override val text_az: String,
) : DuaSourceRef {
    override val source_type: String = DuaSourceType.HADITH
    override val chapter_no: Int? = null
    override val verse_no: Int? = null
    override val verse_end: Int? = null
    override val transliteration: String? = null
    override val source: String? = null
    override val note: String? = null
}

/**
 * Hədisin **Nəbinin sözü** olan hissəsi — mətndəki ilk `{{…}}` (bu toplu Peyğəmbərin sözünü belə
 * işarələyir). Mətn kodda yazılmır, yerli hədis bazasından oxunur: məzmun redaktə olunanda kart da
 * yenilənir. İşarə tapılmasa null — kart tam rəvayəti (isnadı ilə) göstərməkdənsə heç görünmür.
 */
private fun prophetWords(text: String): String? {
    val open = text.indexOf("{{")
    if (open < 0) return null
    val close = text.indexOf("}}", startIndex = open + 2)
    if (close < 0) return null
    return text.substring(open + 2, close).trim().takeIf { it.isNotEmpty() }
}

private suspend fun loadAsmaHadith(): AsmaHadith? = withContext(Dispatchers.IO) {
    val hadith = RepositoryProvider.hadithDatabase.hadithDao()
        .getHadithById(NINETY_NINE_NAMES_HADITH_ID)?.toModel()
        ?: return@withContext null

    val arabic = prophetWords(hadith.text_ar) ?: return@withContext null
    val translation = prophetWords(hadith.text_az) ?: return@withContext null

    // Rəvayət «2815. Bizə Sufyan danışdı…» ilə başlayır — kitab boyu ümumi nömrə budur.
    val number = Regex("""^\s*(\d+)\.""").find(hadith.text_az)?.groupValues?.get(1)
    val location = loadHadithLocation(hadith)

    AsmaHadith(
        id = NINETY_NINE_NAMES_HADITH_ID,
        arabic = arabic,
        translation = translation,
        reference = listOfNotNull(number?.let { "№ $it" }, location.volume?.name).joinToString(" · "),
    )
}

/**
 * Siyahının üstündəki hədis kartı. Hədis bazası hələ endirilməyibsə (və ya hədis tapılmasa) heç nə
 * çəkmir — boş çərçivə göstərmir.
 *
 * Toxunuş hədisi **yarım vərəqdə** açır ([onOpenSource] → ekranın `ReferencePeek`-i), dəlillərin
 * «Qaynağa bax» düyməsi kimi; tam ekrana keçid vərəqin öz «Hədisi aç» düyməsindədir.
 */
@Composable
internal fun AsmaHadithHeader(
    onOpenSource: (DuaSourceRef) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hadith by produceState<AsmaHadith?>(null) { value = loadAsmaHadith() }
    val content = hadith ?: return

    val shape = RoundedCornerShape(14.dp)

    Surface(
        color = colorScheme.primaryContainer.alpha(0.35f),
        shape = shape,
        border = BorderStroke(0.5.dp, colorScheme.primary.alpha(0.25f)),
        // `clip` toxunuş effektini kartın formasına salır — yoxsa künclərdən düz bucaq kimi çıxır.
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable {
                onOpenSource(AsmaHadithSource(content.id, content.arabic, content.translation))
            },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = content.arabic,
                style = typography.titleMedium.withScriptDirection(
                    arabic = true,
                    arabicFontFamily = arabicFontFamily(),
                ),
                color = colorScheme.onSurface,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = content.translation,
                style = typography.bodyMedium.withScriptDirection(arabic = false),
                color = colorScheme.onSurface,
            )
            Text(
                text = content.reference,
                style = typography.labelSmall.withScriptDirection(arabic = false),
                color = colorScheme.onSurfaceVariant.alpha(0.8f),
            )
        }
    }
}
