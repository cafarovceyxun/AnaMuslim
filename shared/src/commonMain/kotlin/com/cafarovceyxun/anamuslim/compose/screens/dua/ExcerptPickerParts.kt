package com.cafarovceyxun.anamuslim.compose.screens.dua

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.screens.hadith.FormTextField
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.arabicFontFamily
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_check
import com.cafarovceyxun.anamuslim.resources.dr_icon_check_circle
import com.cafarovceyxun.anamuslim.resources.dr_icon_delete
import com.cafarovceyxun.anamuslim.resources.dr_icon_sort
import com.cafarovceyxun.anamuslim.resources.duaCountBadge
import com.cafarovceyxun.anamuslim.resources.duaPickerAlreadyAdded
import com.cafarovceyxun.anamuslim.resources.duaPickerCountLabel
import com.cafarovceyxun.anamuslim.resources.duaPickerPartLabel
import com.cafarovceyxun.anamuslim.resources.duaPickerPartTarget
import com.cafarovceyxun.anamuslim.resources.duaPickerRemovePart
import com.cafarovceyxun.anamuslim.resources.duaPickerSavedFromSource
import com.cafarovceyxun.anamuslim.resources.duaPickerSavedParts
import com.cafarovceyxun.anamuslim.resources.duaPickerSegmentsHint
import com.cafarovceyxun.anamuslim.resources.duaPickerSegmentsTitle
import com.cafarovceyxun.anamuslim.utils.dua.DhikrSegment
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

/**
 * «Hədisdəki zikrlər» — mənbədəki hər `{…}` üçün bir çip.
 *
 * Çipə toxunmaq ərəbcəni, oxunuşu və tərcüməni **aktiv hissəyə** birdən yazır; bu mənbədən artıq
 * yazılmış zikrin çipində ✓ var. Tutacaqlarla seçim qalır — çip yalnız ən çox rast gələn halı bir
 * toxunuşa endirir.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SegmentChips(
    segments: List<DhikrSegment>,
    isSaved: (DhikrSegment) -> Boolean,
    onPick: (DhikrSegment) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.duaPickerSegmentsTitle),
            style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                .withScriptDirection(arabic = false),
            color = colorScheme.onSurface,
        )

        Text(
            text = stringResource(Res.string.duaPickerSegmentsHint),
            style = typography.bodySmall.withScriptDirection(arabic = false),
            color = colorScheme.onSurfaceVariant.alpha(0.75f),
        )

        val savedLabel = stringResource(Res.string.duaPickerAlreadyAdded)

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            segments.forEachIndexed { index, segment ->
                val saved = isSaved(segment)
                // Oxunuş varsa çipdə o yazılır: latın hərfləri kiçik çipdə ərəbcədən asan oxunur.
                val preview = segment.transliteration ?: segment.arabic

                FilterChip(
                    selected = saved,
                    onClick = { onPick(segment) },
                    label = {
                        Text(
                            text = "${index + 1}. ${preview.firstWords(3)}",
                            style = typography.labelLarge.withScriptDirection(
                                arabic = segment.transliteration == null,
                                arabicFontFamily = if (segment.transliteration == null) {
                                    arabicFontFamily()
                                } else {
                                    null
                                },
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = if (saved) {
                        {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_check),
                                contentDescription = savedLabel,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

/**
 * «Seçim bu hissəyə yazılır: [1] [2] [3]» — yalnız hissə birdən çox olanda.
 *
 * Mənbə blokları ilə hissə sahələri arasında uzun hədisdə bir neçə ekran məsafə olur; seçici
 * blokların **üstündə** durur ki, seçimin hara düşəcəyi seçilən yerdə görünsün.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ActivePartSelector(
    parts: List<DuaPartDraft>,
    active: Int,
    onSelect: (Int) -> Unit,
) {
    Surface(
        color = colorScheme.primaryContainer.alpha(0.3f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        FlowRow(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.duaPickerPartTarget),
                style = typography.labelLarge.withScriptDirection(arabic = false),
                color = colorScheme.onSurface,
            )

            parts.forEachIndexed { index, part ->
                val selected = index == active

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (selected) colorScheme.primary
                            else colorScheme.surfaceContainerHigh,
                            shape = CircleShape,
                        )
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (index + 1).toString(),
                        style = typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        // Ərəbcəsi dolu hissə tünd, boşu solğun — nəyin əskik olduğu bir baxışda.
                        color = when {
                            selected -> colorScheme.onPrimary
                            part.arabic.isNotBlank() -> colorScheme.primary
                            else -> colorScheme.onSurfaceVariant.alpha(0.6f)
                        },
                    )
                }
            }
        }
    }
}

/**
 * Aktiv hissənin kartı — başlıq («Hissə 2»), silmə düyməsi və sahələr ([content]).
 *
 * Yalnız birdən çox hissə olanda çəkilir; tək hissədə sahələr kartsız, əvvəlki kimi durur.
 */
@Composable
internal fun PartCard(
    index: Int,
    onRemove: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        color = colorScheme.surfaceContainerHigh.alpha(0.35f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, colorScheme.primary.alpha(0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.duaPickerPartLabel, index + 1),
                    style = typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        .withScriptDirection(arabic = false),
                    color = colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )

                IconButton(onClick = onRemove) {
                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_delete),
                        contentDescription = stringResource(Res.string.duaPickerRemovePart),
                        tint = colorScheme.onSurfaceVariant.alpha(0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                content()
            }
        }
    }
}

/** Aktiv olmayan hissə — bir sətirlik xülasə: «Hissə 2 · سُبْحَانَ اللَّهِ · 33 dəfə». */
@Composable
internal fun PartSummaryRow(
    index: Int,
    part: DuaPartDraft,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        color = colorScheme.surfaceContainerHigh.alpha(0.35f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.5.dp, colorScheme.outlineVariant.alpha(0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.duaPickerPartLabel, index + 1),
                style = typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    .withScriptDirection(arabic = false),
                color = colorScheme.onSurfaceVariant,
            )

            Text(
                text = part.arabic.firstWords(4),
                style = typography.bodyMedium.withScriptDirection(
                    arabic = true,
                    arabicFontFamily = arabicFontFamily(),
                ),
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            part.repeatCount?.let { count ->
                Text(
                    text = stringResource(Res.string.duaCountBadge, count),
                    style = typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        .withScriptDirection(arabic = false),
                    color = colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .background(colorScheme.tertiaryContainer.alpha(0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }

            IconButton(onClick = onRemove) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_delete),
                    contentDescription = stringResource(Res.string.duaPickerRemovePart),
                    tint = colorScheme.onSurfaceVariant.alpha(0.6f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * Say sahəsi və tez seçilən saylar.
 *
 * Çiplər 33 + 33 + 33 + 1 kimi zikrləri yazmağı rəqəm klaviaturasız edir; seçilmiş çipə yenidən
 * toxunmaq sayı silir.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RepeatCountField(value: String, onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FormTextField(
            value = value,
            onValueChange = { input -> onValueChange(input.filter { it.isDigit() }.take(6)) },
            label = stringResource(Res.string.duaPickerCountLabel),
            icon = Res.drawable.dr_icon_sort,
            keyboardType = KeyboardType.Number,
            onClear = { onValueChange("") },
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            QUICK_REPEAT_COUNTS.forEach { count ->
                val selected = value == count.toString()

                FilterChip(
                    selected = selected,
                    onClick = { onValueChange(if (selected) "" else count.toString()) },
                    label = { Text(count.toString()) },
                )
            }
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
