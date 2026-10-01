package com.cafarovceyxun.anamuslim.compose.screens.dua

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.arabicFontFamily
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_check
import com.cafarovceyxun.anamuslim.resources.dr_icon_check_circle
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.duaPanelCount
import com.cafarovceyxun.anamuslim.resources.duaPickerPartLabel
import com.cafarovceyxun.anamuslim.resources.duaPickerRemovePart
import com.cafarovceyxun.anamuslim.resources.duaPickerSavedFromSource
import com.cafarovceyxun.anamuslim.resources.duaPickerSavedParts
import com.cafarovceyxun.anamuslim.utils.supabase.Dua
import com.cafarovceyxun.anamuslim.utils.text.foldSearchTextWithOffsets
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// ------------------------------------------------------------------------------ model

/**
 * Seçim ekranındakı bir hissə — yadda saxlananda bir `dua` sətri olur.
 *
 * Dua bir neçə hissədən ibarət ola bilər (33 + 33 + 33 + 1): hər hissənin öz mətni və öz sayı var,
 * başlıq isə bütöv duaya aiddir. Bazada hissə baş sətrə `part_of_id` ilə bağlanır.
 */
data class DuaPartDraft(
    val arabic: String = "",
    val transliteration: String = "",
    val translation: String = "",
    /** Say sahəsindəki mətn — boş və ya sıfır = say göstərilmir. */
    val countText: String = "",
) {
    /** Heç nə yazılmayıb — yadda saxlamazdan əvvəl atılır, xəta sayılmır. */
    val isBlank: Boolean
        get() = arabic.isBlank() && transliteration.isBlank() &&
            translation.isBlank() && countText.isBlank()

    val repeatCount: Int? get() = countText.toIntOrNull()?.takeIf { it > 0 }

    /** `dua` sətri; başlıq və hissə bağlantısını yazan tərəf qoyur. */
    fun toDua(data: ExcerptSourceData): Dua = Dua(
        category_slug = "",
        source_type = data.sourceType,
        hadith_id = data.hadithId,
        chapter_no = data.chapterNo,
        verse_no = data.verseNo,
        verse_end = data.verseEnd,
        text_ar = arabic.trim(),
        text_az = translation.trim(),
        transliteration = transliteration.trim().takeIf { it.isNotEmpty() },
        repeat_count = repeatCount,
        source = data.reference,
    )
}

/** Yadda saxlamağa mane olan ilk səbəb. Nömrələr ekrandakı «Hissə N» ilə eynidir (1-dən). */
sealed interface PartsProblem {
    /** Heç bir hissədə mətn yoxdur. */
    data object Empty : PartsProblem

    data class MissingArabic(val partNo: Int) : PartsProblem

    data class DuplicateArabic(val firstNo: Int, val secondNo: Int) : PartsProblem
}

/** [validateParts]-in nəticəsi: yazılacaq hissələr və ya mane olan səbəb. */
data class PartsValidation(
    val kept: List<DuaPartDraft>,
    val problem: PartsProblem?,
)

/**
 * Yadda saxlamazdan əvvəlki yoxlama.
 *
 * - **Tam boş** hissə atılır: «Hissə əlavə et»ə basıb fikrini dəyişən adam onu silmək məcburiyyətində
 *   qalmasın.
 * - Qalan hissədə ərəbcə məcburidir (oxunuş və tərcümə mənbədə olmaya bilər).
 * - İki hissənin ərəbcəsi eyni ola bilməz: baza bunu onsuz da `dua_unique_excerpt` ilə rədd edər
 *   (açar `category_slug, md5(text_ar), mənbə`-dir), amma onda xəta bütün duanı geri alır və
 *   istifadəçi hansı hissənin səbəb olduğunu bilmir. Müqayisə yastılanmış mətnlə gedir — hərəkə
 *   fərqi «başqa mətn» sayılmasın.
 */
fun validateParts(parts: List<DuaPartDraft>): PartsValidation {
    val numbered = parts.withIndex().filterNot { it.value.isBlank }
    val kept = numbered.map { it.value }

    if (numbered.isEmpty()) return PartsValidation(kept, PartsProblem.Empty)

    numbered.firstOrNull { it.value.arabic.isBlank() }?.let { missing ->
        return PartsValidation(kept, PartsProblem.MissingArabic(missing.index + 1))
    }

    val seen = HashMap<String, Int>()
    for ((index, part) in numbered) {
        val folded = foldSearchTextWithOffsets(part.arabic.trim()).first
        val earlier = seen[folded]
        if (earlier != null) {
            return PartsValidation(kept, PartsProblem.DuplicateArabic(earlier + 1, index + 1))
        }
        seen[folded] = index
    }

    return PartsValidation(kept, null)
}

/**
 * Bu mənbədən **artıq yazılmış** bir dua — baş sətir və hissələri, hədəfin adı ilə.
 *
 * Seçim ekranı bunu iki yerdə işlədir: mənbə bloklarında solğun vurğu (nə götürülüb) və 3-cü
 * addımın altındakı siyahı (hara getdi). «Saxla və davam et» ilə beş zikri bir-bir yazanda hansının
 * artıq yazıldığını yadda saxlamaq lazım gəlməsin.
 */
data class SavedExcerpt(
    /** Baş sətir birinci, sonra hissələr `part_no` sırası ilə. */
    val parts: List<Dua>,
    /** «Başlıq › Alt başlıq». */
    val targetLabel: String,
)

/** İki mətn eyni zikrdirmi — hərəkə və hərf forması fərqi nəzərə alınmadan. */
internal fun sameArabic(a: String, b: String): Boolean {
    val left = foldSearchTextWithOffsets(a.trim()).first
    return left.isNotEmpty() && left == foldSearchTextWithOffsets(b.trim()).first
}

/** Mətnin ilk [count] sözü — çip və xülasə sətri üçün; qısaldılıbsa sonda «…». */
internal fun String.firstWords(count: Int): String {
    val words = trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return if (words.size <= count) words.joinToString(" ") else words.take(count).joinToString(" ") + "…"
}

/** Tez seçilən saylar: 33 + 33 + 33(34) + 1 təsbihi, 3/7/10 isə səhər-axşam zikrlərində. */
private val QUICK_REPEAT_COUNTS = listOf(1, 3, 7, 10, 33, 34, 100)

// ------------------------------------------------------------------------------ UI

/** Panel sətirlərinin sol sütunu — «Ərəbcə», «Say», «Bölmə» bir xətt üzrə düzülsün. */
internal val PanelLabelWidth = 84.dp

/**
 * Panel başlığındakı hissə tab-ları — «Hissə 1», «Hissə 2» … (yalnız hissə birdən çox olanda).
 *
 * Mənbədən seçim və zikr çipləri həmişə **aktiv** hissəyə yazır. Ərəbcəsi boş hissənin tab-ında
 * kiçik nöqtə var — nəyin əskik olduğu bir baxışda görünsün. Silmə düyməsi yalnız aktiv tab-dadır:
 * hər tab-da × olsa səhv hissəni silmək asan olardı.
 */
@Composable
internal fun PartTabs(
    parts: List<DuaPartDraft>,
    active: Int,
    onSelect: (Int) -> Unit,
    onRemoveActive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val removeLabel = stringResource(Res.string.duaPickerRemovePart)

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        parts.forEachIndexed { index, part ->
            val selected = index == active

            InputChip(
                selected = selected,
                onClick = { onSelect(index) },
                label = {
                    Text(
                        text = stringResource(Res.string.duaPickerPartLabel, index + 1),
                        style = typography.labelLarge.withScriptDirection(arabic = false),
                    )
                },
                leadingIcon = if (part.arabic.isBlank()) {
                    {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(colorScheme.tertiary, CircleShape),
                        )
                    }
                } else {
                    null
                },
                trailingIcon = if (selected) {
                    {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_close),
                            contentDescription = removeLabel,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onRemoveActive),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

/**
 * «Say» sətri — tez seçilən saylar və istənilən say üçün kiçik sahə.
 *
 * Çiplər 33 + 33 + 33 + 1 kimi zikrləri rəqəm klaviaturasız edir; seçilmiş çipə yenidən toxunmaq
 * sayı silir. Siyahıda olmayan say (40, 70 …) sonuncu sahəyə yazılır — əvvəlki tam en `FormTextField`
 * paneldə bir sətirdən çox yer tuturdu.
 */
@Composable
internal fun CountChipRow(value: String, onValueChange: (String) -> Unit) {
    val custom = value.takeIf { it.isNotBlank() && it.toIntOrNull() !in QUICK_REPEAT_COUNTS }.orEmpty()

    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.duaPanelCount),
            style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                .withScriptDirection(arabic = false),
            color = if (value.isNotBlank()) colorScheme.primary else colorScheme.onSurfaceVariant,
            modifier = Modifier.width(PanelLabelWidth),
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QUICK_REPEAT_COUNTS.forEach { count ->
                val selected = value == count.toString()

                FilterChip(
                    selected = selected,
                    onClick = { onValueChange(if (selected) "" else count.toString()) },
                    label = { Text(count.toString()) },
                )
            }

            BasicTextField(
                value = custom,
                onValueChange = { input -> onValueChange(input.filter { it.isDigit() }.take(6)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = typography.labelLarge.copy(
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                ),
                cursorBrush = SolidColor(colorScheme.primary),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center) {
                        if (custom.isEmpty()) {
                            Text(
                                text = "…",
                                style = typography.labelLarge,
                                color = colorScheme.onSurfaceVariant.alpha(0.7f),
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier
                    .width(56.dp)
                    .height(32.dp)
                    .border(
                        width = 1.dp,
                        color = if (custom.isNotEmpty()) colorScheme.primary else colorScheme.outlineVariant,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .wrapContentHeight(),
            )
        }
    }
}

/**
 * «Bu mənbədən əlavə olunanlar» — bazadakı eyni mənbəli dualar: ✓, ilk sözlər, «Başlıq › Alt
 * başlıq» və hissə sayı. Nəyin hara getdiyi bir baxışda görünür.
 */
@Composable
internal fun SavedFromSourceList(items: List<SavedExcerpt>) {
    if (items.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.duaPickerSavedFromSource),
            style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                .withScriptDirection(arabic = false),
            color = colorScheme.onSurface,
        )

        Surface(
            color = colorScheme.surfaceContainerHigh.alpha(0.4f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = colorScheme.outlineVariant.alpha(0.3f),
                            modifier = Modifier.padding(start = 44.dp),
                        )
                    }

                    SavedExcerptRow(item)
                }
            }
        }
    }
}

@Composable
private fun SavedExcerptRow(item: SavedExcerpt) {
    val head = item.parts.firstOrNull() ?: return

    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(Res.drawable.dr_icon_check_circle),
            contentDescription = null,
            tint = colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp).size(20.dp),
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = head.text_ar.firstWords(5),
                style = typography.bodyLarge.withScriptDirection(
                    arabic = true,
                    arabicFontFamily = arabicFontFamily(),
                ),
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            val meta = buildList {
                add(item.targetLabel)
                if (item.parts.size > 1) {
                    add(stringResource(Res.string.duaPickerSavedParts, item.parts.size))
                }
            }.filter { it.isNotBlank() }.joinToString(" · ")

            Text(
                text = meta,
                style = typography.labelMedium.withScriptDirection(arabic = false),
                color = colorScheme.onSurfaceVariant.alpha(0.8f),
            )
        }
    }
}
