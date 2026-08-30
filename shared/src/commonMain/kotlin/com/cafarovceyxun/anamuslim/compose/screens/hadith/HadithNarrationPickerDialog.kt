package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.Chip
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.components.settings.withContentDirection
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dailyContentPartOfHadith
import com.cafarovceyxun.anamuslim.resources.dailyContentWholeHadith
import com.cafarovceyxun.anamuslim.resources.hadithNarrationPickerHint
import com.cafarovceyxun.anamuslim.resources.hadithNarrationPickerTitle
import com.cafarovceyxun.anamuslim.resources.hadithNarrationPartLabel
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.openImageEditor
import com.cafarovceyxun.anamuslim.utils.verse.HadithExcerpt
import org.jetbrains.compose.resources.stringResource

/**
 * «Hədisin hamısı, yoxsa bir rəvayəti?» — şəkil redaktoruna girməzdən əvvəlki seçim.
 *
 * Bu topluda bir hədis çox vaxt bir neçə rəvayətdən ibarətdir («… (Buxari, 3035). Digər bir
 * rəvayətdə: …»); şəkil kimi paylaşılanda isə adətən **bir rəvayət** lazım gəlir — hamısı bir
 * şəkilə yığılanda mətn oxunmaz dərəcədə kiçilir. Bölgü [HadithExcerpt.narrationParts] ilə edilir,
 * yəni günün hədisi paneli ilə eyni sərhədlərdən.
 *
 * Ərəbcə mətn öz işarəsindən («وفي رواية») bölünür. İki dil eyni sayda parça verməyəndə —
 * tərcümədə rəvayət sərhədi var, ərəbcədə yoxdur (və ya əksi) — indeks uyğunluğu **saxta** olardı,
 * ona görə belə halda ərəbcə tam mətni ilə qalır: seçim yalnız tərcüməyə tətbiq olunur.
 * Dialoq yalnız tərcümədə birdən çox rəvayət olanda açılır ([HadithShareSheet]).
 */
@Composable
fun HadithNarrationPickerDialog(
    translationParts: List<String>,
    arabicParts: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (translation: String, arabic: String) -> Unit,
) {
    // Ərəbcəni yalnız parça sayları üst-üstə düşəndə kəsirik — bax sinif şərhi.
    val arabicFollowsSelection = arabicParts.size == translationParts.size

    var partial by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf(0)) }

    AlertDialog(
        isOpen = true,
        onClose = onDismiss,
        title = stringResource(Res.string.hadithNarrationPickerTitle),
        actions = listOf(
            AlertDialogAction(text = stringResource(Res.string.strLabelCancel), onClick = onDismiss),
            AlertDialogAction(
                text = stringResource(Res.string.openImageEditor),
                style = AlertDialogActionStyle.Primary,
                // Boş seçimlə davam etmək şəkli boş buraxardı; belə halda düymə heç nə etmir və
                // dialoq açıq qalır (`dismissOnClick = false`), yəni istifadəçi seçimini bitirir.
                dismissOnClick = partial.not() || selected.isNotEmpty(),
                onClick = {
                    if (!partial) {
                        onConfirm(translationParts.joinToString(" "), arabicParts.joinToString(" "))
                    } else if (selected.isNotEmpty()) {
                        onConfirm(
                            HadithExcerpt.join(translationParts, selected),
                            if (arabicFollowsSelection) HadithExcerpt.join(arabicParts, selected)
                            else arabicParts.joinToString(" "),
                        )
                    }
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
                    text = stringResource(Res.string.hadithNarrationPickerHint),
                    style = typography.bodySmall.withContentDirection(),
                    color = colorScheme.onSurfaceVariant,
                )

                translationParts.forEachIndexed { index, part ->
                    NarrationRow(
                        label = stringResource(Res.string.hadithNarrationPartLabel, index + 1),
                        preview = part,
                        checked = index in selected,
                        onCheckedChange = {
                            selected = if (index in selected) selected - index else selected + index
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NarrationRow(
    label: String,
    preview: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = colorScheme.primary,
                uncheckedColor = colorScheme.onSurfaceVariant,
            ),
        )

        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = label,
                style = typography.labelMedium,
                color = colorScheme.primary,
            )

            // Mətn tam göstərilmir: rəvayətlər uzundur və seçim üçün ilk sətirlər kifayət edir.
            Text(
                text = preview,
                style = typography.bodySmall.withContentDirection(),
                color = colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
