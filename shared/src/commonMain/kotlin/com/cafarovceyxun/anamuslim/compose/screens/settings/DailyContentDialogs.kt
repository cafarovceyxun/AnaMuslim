package com.cafarovceyxun.anamuslim.compose.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.Chip
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.components.settings.withContentDirection
import com.cafarovceyxun.anamuslim.compose.screens.hadith.FormTextField
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dailyContentAddVerse
import com.cafarovceyxun.anamuslim.resources.dailyContentEditRange
import com.cafarovceyxun.anamuslim.resources.dailyContentExcerptEmpty
import com.cafarovceyxun.anamuslim.resources.dailyContentExcerptFullText
import com.cafarovceyxun.anamuslim.resources.dailyContentAddToQueue
import com.cafarovceyxun.anamuslim.resources.dailyContentExcerptHint
import com.cafarovceyxun.anamuslim.resources.dailyContentPartOfHadith
import com.cafarovceyxun.anamuslim.resources.dailyContentWholeHadith
import com.cafarovceyxun.anamuslim.resources.strTitleDailyHadith
import com.cafarovceyxun.anamuslim.resources.dailyContentExcerptTitle
import com.cafarovceyxun.anamuslim.resources.dailyContentVerseEndLabel
import com.cafarovceyxun.anamuslim.resources.dailyContentVerseStartLabel
import com.cafarovceyxun.anamuslim.resources.dr_icon_edit
import com.cafarovceyxun.anamuslim.resources.ic_book_copy
import com.cafarovceyxun.anamuslim.resources.labelArabic
import com.cafarovceyxun.anamuslim.resources.labelTranslation
import com.cafarovceyxun.anamuslim.resources.save
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.utils.supabase.DailyContent
import com.cafarovceyxun.anamuslim.utils.verse.HadithExcerpt
import org.jetbrains.compose.resources.stringResource

/** Mövcud ayə elementinin aralığını dəyişir — mətnlər yenidən oxunur. */
@Composable
internal fun VerseRangeDialog(
    item: DailyContent,
    onDismiss: () -> Unit,
    onConfirm: (verseStart: Int, verseEnd: Int?) -> Unit,
) {
    var startText by rememberSaveable { mutableStateOf(item.verse_no?.toString().orEmpty()) }
    var endText by rememberSaveable { mutableStateOf(item.verse_end?.toString().orEmpty()) }

    val verseStart = startText.toIntOrNull()
    val verseEnd = endText.toIntOrNull()

    val startValid = verseStart != null && verseStart > 0
    val endValid = endText.isBlank() || (verseEnd != null && verseStart != null && verseEnd > verseStart)
    val isValid = startValid && endValid

    AlertDialog(
        isOpen = true,
        onClose = onDismiss,
        title = stringResource(Res.string.dailyContentEditRange),
        actions = listOf(
            AlertDialogAction(text = stringResource(Res.string.strLabelCancel), onClick = onDismiss),
            AlertDialogAction(
                text = stringResource(Res.string.save),
                style = AlertDialogActionStyle.Primary,
                onClick = { if (isValid) onConfirm(verseStart, verseEnd) },
            ),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                FormTextField(
                    value = startText,
                    onValueChange = { input -> startText = input.filter { it.isDigit() } },
                    label = stringResource(Res.string.dailyContentVerseStartLabel),
                    icon = Res.drawable.ic_book_copy,
                    keyboardType = KeyboardType.Number,
                    error = !startValid,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                FormTextField(
                    value = endText,
                    onValueChange = { input -> endText = input.filter { it.isDigit() } },
                    label = stringResource(Res.string.dailyContentVerseEndLabel),
                    icon = Res.drawable.ic_book_copy,
                    keyboardType = KeyboardType.Number,
                    error = !endValid,
                )
            }
        }
    }
}

/**
 * Hədisi növbəyə salarkən **hamısı, yoxsa bir qismi** sualı.
 *
 * Hədis oxuma ekranındakı «Günün hədisi» düyməsi bunu açır — hədis növbəyə elə oradan düşür.
 * Paneldə id ilə əlavə etmə **qəsdən yoxdur**: id istifadəçinin gördüyü şey deyil, hədisi isə
 * onsuz da oxuduğu yerdə seçmək təbiidir.
 *
 * «Bir qismi» seçiləndə mətn cümlələrə bölünür ([HadithExcerpt]) və seçilənlər çıxarış kimi
 * yazılır; «hamısı» boş çıxarış deməkdir, yəni kart və bildiriş tam mətni göstərir.
 */
@Composable
internal fun HadithDailyContentDialog(
    textAz: String,
    textAr: String,
    onDismiss: () -> Unit,
    onConfirm: (excerptAz: String, excerptAr: String) -> Unit,
) {
    val sentencesAz = remember(textAz) { HadithExcerpt.sentences(textAz) }
    val sentencesAr = remember(textAr) { HadithExcerpt.sentences(textAr) }

    var partial by rememberSaveable { mutableStateOf(false) }
    var selectedAz by remember { mutableStateOf(emptySet<Int>()) }
    var selectedAr by remember { mutableStateOf(emptySet<Int>()) }
    var excerptAz by remember { mutableStateOf("") }
    var excerptAr by remember { mutableStateOf("") }

    AlertDialog(
        isOpen = true,
        onClose = onDismiss,
        title = stringResource(Res.string.strTitleDailyHadith),
        actions = listOf(
            AlertDialogAction(text = stringResource(Res.string.strLabelCancel), onClick = onDismiss),
            AlertDialogAction(
                text = stringResource(Res.string.dailyContentAddToQueue),
                style = AlertDialogActionStyle.Primary,
                onClick = {
                    if (partial) onConfirm(excerptAz, excerptAr) else onConfirm("", "")
                },
            ),
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(
                    selected = !partial,
                    onClick = { partial = false },
                    label = {
                        Text(
                            text = stringResource(Res.string.dailyContentWholeHadith),
                            style = typography.labelMedium,
                        )
                    },
                )

                Chip(
                    selected = partial,
                    onClick = { partial = true },
                    label = {
                        Text(
                            text = stringResource(Res.string.dailyContentPartOfHadith),
                            style = typography.labelMedium,
                        )
                    },
                )
            }

            if (partial) {
                Text(
                    text = stringResource(Res.string.dailyContentExcerptHint),
                    style = typography.bodySmall.withContentDirection(),
                    color = colorScheme.onSurfaceVariant,
                )

                ExcerptSection(
                    label = stringResource(Res.string.labelTranslation),
                    sentences = sentencesAz,
                    selected = selectedAz,
                    text = excerptAz,
                    onToggle = { index ->
                        selectedAz = selectedAz.toggle(index)
                        excerptAz = HadithExcerpt.join(sentencesAz, selectedAz)
                    },
                    onClear = {
                        selectedAz = emptySet()
                        excerptAz = ""
                    },
                    onTextChange = {
                        excerptAz = it
                        selectedAz = HadithExcerpt.selectionOf(sentencesAz, it)
                    },
                )

                if (sentencesAr.isNotEmpty()) {
                    ExcerptSection(
                        label = stringResource(Res.string.labelArabic),
                        sentences = sentencesAr,
                        selected = selectedAr,
                        text = excerptAr,
                        onToggle = { index ->
                            selectedAr = selectedAr.toggle(index)
                            excerptAr = HadithExcerpt.join(sentencesAr, selectedAr)
                        },
                        onClear = {
                            selectedAr = emptySet()
                            excerptAr = ""
                        },
                        onTextChange = {
                            excerptAr = it
                            selectedAr = HadithExcerpt.selectionOf(sentencesAr, it)
                        },
                    )
                }
            }
        }
    }
}

/**
 * Hədisin **hansı hissəsinin** göstəriləcəyini seçir.
 *
 * Mətn cümlələrə bölünür ([HadithExcerpt]) və admin lazım olanlara toxunur — bu topludakı hədislərin
 * çoxu uzun isnad zənciri ilə başlayır, kartda və bildirişdə isə yalnız mətn lazımdır. Seçim
 * nəticəsi aşağıdakı sahədə görünür və əl ilə də redaktə oluna bilər; sahə boş qalanda çıxarış
 * silinir, yəni element yenidən tam mətnlə göstərilir.
 *
 * Ərəbcə və azərbaycanca ayrıca seçilir: cümlə sayları üst-üstə düşmür.
 */
@Composable
internal fun HadithExcerptDialog(
    item: DailyContent,
    onDismiss: () -> Unit,
    onConfirm: (excerptAz: String, excerptAr: String) -> Unit,
) {
    val sentencesAz = remember(item.text_az) { HadithExcerpt.sentences(item.text_az) }
    val sentencesAr = remember(item.text_ar) { HadithExcerpt.sentences(item.text_ar) }

    var selectedAz by remember(item.id) {
        mutableStateOf(HadithExcerpt.selectionOf(sentencesAz, item.excerpt_az))
    }
    var selectedAr by remember(item.id) {
        mutableStateOf(HadithExcerpt.selectionOf(sentencesAr, item.excerpt_ar))
    }

    var textAz by remember(item.id) { mutableStateOf(item.excerpt_az.orEmpty()) }
    var textAr by remember(item.id) { mutableStateOf(item.excerpt_ar.orEmpty()) }

    AlertDialog(
        isOpen = true,
        onClose = onDismiss,
        title = stringResource(Res.string.dailyContentExcerptTitle),
        actions = listOf(
            AlertDialogAction(text = stringResource(Res.string.strLabelCancel), onClick = onDismiss),
            AlertDialogAction(
                text = stringResource(Res.string.save),
                style = AlertDialogActionStyle.Primary,
                onClick = { onConfirm(textAz, textAr) },
            ),
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(Res.string.dailyContentExcerptHint),
                style = typography.bodySmall.withContentDirection(),
                color = colorScheme.onSurfaceVariant,
            )

            ExcerptSection(
                label = stringResource(Res.string.labelTranslation),
                sentences = sentencesAz,
                selected = selectedAz,
                text = textAz,
                onToggle = { index ->
                    selectedAz = selectedAz.toggle(index)
                    textAz = HadithExcerpt.join(sentencesAz, selectedAz)
                },
                onClear = {
                    selectedAz = emptySet()
                    textAz = ""
                },
                onTextChange = {
                    textAz = it
                    // Əl ilə yazılan mətn artıq cümlə seçimi deyil — nişanlar sıfırlanır ki, panel
                    // yanlış vəziyyət göstərməsin.
                    selectedAz = HadithExcerpt.selectionOf(sentencesAz, it)
                },
            )

            if (sentencesAr.isNotEmpty()) {
                ExcerptSection(
                    label = stringResource(Res.string.labelArabic),
                    sentences = sentencesAr,
                    selected = selectedAr,
                    text = textAr,
                    onToggle = { index ->
                        selectedAr = selectedAr.toggle(index)
                        textAr = HadithExcerpt.join(sentencesAr, selectedAr)
                    },
                    onClear = {
                        selectedAr = emptySet()
                        textAr = ""
                    },
                    onTextChange = {
                        textAr = it
                        selectedAr = HadithExcerpt.selectionOf(sentencesAr, it)
                    },
                )
            }

            if (textAz.isBlank() && textAr.isBlank()) {
                Text(
                    text = stringResource(Res.string.dailyContentExcerptEmpty),
                    style = typography.bodySmall.withContentDirection(),
                    color = colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExcerptSection(
    label: String,
    sentences: List<String>,
    selected: Set<Int>,
    text: String,
    onToggle: (Int) -> Unit,
    onClear: () -> Unit,
    onTextChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = typography.labelMedium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )

            Chip(
                selected = selected.isEmpty() && text.isBlank(),
                onClick = onClear,
                label = {
                    Text(
                        text = stringResource(Res.string.dailyContentExcerptFullText),
                        style = typography.labelSmall,
                    )
                },
            )
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            sentences.forEachIndexed { index, sentence ->
                Chip(
                    selected = index in selected,
                    onClick = { onToggle(index) },
                    label = {
                        Text(
                            // Cümlələr uzun ola bilər: nişan yalnız başlanğıcı göstərir, tam mətn
                            // aşağıdakı sahədə yığılır.
                            text = sentence.take(38) + if (sentence.length > 38) "…" else "",
                            style = typography.labelSmall.withContentDirection(),
                        )
                    },
                )
            }
        }

        FormTextField(
            value = text,
            onValueChange = onTextChange,
            label = label,
            icon = Res.drawable.dr_icon_edit,
            minLines = 2,
            maxLines = 6,
        )
    }
}

private fun Set<Int>.toggle(index: Int): Set<Int> =
    if (index in this) this - index else this + index
