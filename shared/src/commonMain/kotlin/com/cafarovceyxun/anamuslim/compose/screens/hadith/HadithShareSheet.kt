package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheetHeader
import com.cafarovceyxun.anamuslim.compose.components.share.ShareWaysRow
import com.cafarovceyxun.anamuslim.compose.components.settings.withContentDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences
import com.cafarovceyxun.anamuslim.utils.supabase.Hadith
import com.cafarovceyxun.anamuslim.utils.verse.HadithExcerpt
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.copiedToClipboard
import com.cafarovceyxun.anamuslim.resources.dailyContentPartOfHadith
import com.cafarovceyxun.anamuslim.resources.dailyContentWholeHadith
import com.cafarovceyxun.anamuslim.resources.hadith
import com.cafarovceyxun.anamuslim.resources.hadithIncludeArabic
import com.cafarovceyxun.anamuslim.resources.hadithIncludeNote
import com.cafarovceyxun.anamuslim.resources.hadithIncludeSource
import com.cafarovceyxun.anamuslim.resources.hadithIncludeTranslation
import com.cafarovceyxun.anamuslim.resources.hadithNarrationDropMarker
import com.cafarovceyxun.anamuslim.resources.hadithNarrationPartLabel
import com.cafarovceyxun.anamuslim.resources.hadithNarrationPickerHint
import com.cafarovceyxun.anamuslim.resources.hadithNarrationPickerTitle
import com.cafarovceyxun.anamuslim.resources.hadithShareTitle
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelShare
import com.cafarovceyxun.anamuslim.resources.strTitleNote
import com.cafarovceyxun.anamuslim.resources.translShowParentheses
import org.jetbrains.compose.resources.stringResource

/**
 * Hədisin paylaşma vərəqi — iki mərhələ: əvvəlcə mətn hazırlanır (hansı rəvayətlər, sonra
 * ərəbcə/tərcümə/qaynaq/qeyd/mötərizə), «Paylaş» basılandan sonra isə yol seçilir — mətn kimi,
 * şəkil kimi, yoxsa panoya.
 *
 * Rəvayət seçimi əvvəl **yalnız** şəkil redaktoruna girərkən ayrıca dialoqda soruşulurdu, ona görə
 * kopyalanan və paylaşılan mətn həmişə hədisin hamısı olurdu. İndi seçim vərəqin başındadır və hər
 * üç yol eyni mətndən çıxır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithShareSheet(
    hadith: Hadith?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (hadith == null) return

    val clipboardMsg = stringResource(Res.string.copiedToClipboard)
    
    var includeArabic by remember { mutableStateOf(true) }
    var includeAzerbaijani by remember { mutableStateOf(true) }
    var includeSource by remember { mutableStateOf(true) }
    var includeNote by remember { mutableStateOf(true) }
    var showParentheses by remember { mutableStateOf(true) }
    var showImageEditor by remember { mutableStateOf(false) }

    // «Paylaş» basılana qədər yollar gizlidir: vərəq açılanda sual «necə paylaşım», «nəyi
    // paylaşım»-dır — üç düymə elə başdan görünəndə mətn hazırlığı onların arasında itirdi.
    var shareWaysOpen by remember(hadith) { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showParentheses = HadithPreferences.getShowParentheses()
    }

    val labelHadith = stringResource(Res.string.hadith)
    val labelNote = stringResource(Res.string.strTitleNote)
    val chooserTitle = stringResource(Res.string.hadithShareTitle)

    // Mötərizə təmizləməsi bir yerdə edilir ki, mətn və şəkil paylaşımı eyni mətni versin.
    val azText = remember(hadith, showParentheses) {
        if (showParentheses) hadith.text_az
        else hadith.text_az.replace(Regex("\\(([\\s\\S]*?)\\)"), "").replace(Regex("\\s+"), " ").trim()
    }

    // Bir hədis çox vaxt bir neçə rəvayətdən ibarətdir; hamısını bir şəkilə yığmaq mətni oxunmaz
    // edir, mətn kimi paylaşanda da adətən bir rəvayət lazım gəlir. Mötərizə təmizləməsindən
    // **sonrakı** mətn bölünür ki, seçim ekranda görünənlə eyni olsun.
    val narrationsAz = remember(azText) { HadithExcerpt.narrationParts(azText) }
    val narrationsAr = remember(hadith) { HadithExcerpt.narrationParts(hadith.text_ar) }
    val multiNarration = narrationsAz.size > 1

    // Ərəbcəni yalnız parça sayları üst-üstə düşəndə kəsirik: tərcümədə rəvayət sərhədi olub
    // ərəbcədə olmayanda (və ya əksinə) indeks uyğunluğu **saxta** olardı — ikinci ərəbcə rəvayətin
    // altına üçüncünün tərcüməsi düşərdi. Belə halda ərəbcə tam mətni ilə qalır.
    val arabicFollowsSelection = narrationsAr.size == narrationsAz.size

    var partialNarrations by remember(hadith) { mutableStateOf(false) }
    var selectedNarrations by remember(hadith) { mutableStateOf(setOf(0)) }
    var dropNarrationMarker by remember(hadith) { mutableStateOf(false) }

    val selection = remember(multiNarration, partialNarrations, selectedNarrations, narrationsAz) {
        if (multiNarration && partialNarrations) selectedNarrations else narrationsAz.indices.toSet()
    }

    // Keçid yalnız seçimdə həqiqətən «Digər bir rəvayətdə» ilə başlayan parça olanda görünür:
    // birinci rəvayət tək seçiləndə atılacaq söz yoxdur, keçid isə orada nə etdiyini demir.
    val selectionHasMarker = remember(narrationsAz, selection) {
        selection.any { index ->
            narrationsAz.getOrNull(index)?.let { HadithExcerpt.hasNarrationMarker(it) } == true
        }
    }

    // Gizli keçid mətnə təsir etməsin — seçim dəyişəndə bayraq olduğu kimi qalır.
    val dropMarker = dropNarrationMarker && selectionHasMarker

    val shareTranslation = remember(azText, narrationsAz, selection, dropMarker) {
        composeNarrations(azText, narrationsAz, selection, dropMarker)
    }

    val shareArabic = remember(hadith, narrationsAr, selection, dropMarker, arabicFollowsSelection) {
        if (arabicFollowsSelection) {
            composeNarrations(hadith.text_ar, narrationsAr, selection, dropMarker)
        } else {
            hadith.text_ar
        }
    }

    // Boş seçim boş mətn deməkdir — düymələr onda işləmir, əks halda boş şəkil və boş pano qalırdı.
    val hasContent = selection.isNotEmpty()

    // Redaktor öz tam ekran `Dialog` pəncərəsindədir, ona görə vərəq altda kompozisiyada qalır və
    // geri qayıdanda seçimlər (ərəbcə/tərcümə/qaynaq/qeyd/mötərizə) olduğu kimi durur. Əvvəl geri
    // düyməsi vərəqi də bağlayıb hədis siyahısına atırdı — bir şəkli yenidən düzəltmək üçün bütün
    // yolu təzədən keçmək lazım gəlirdi.
    if (showImageEditor) {
        HadithImageEditorScreen(
            eyebrow = "$labelHadith №${hadith.hadith_no}",
            arabicText = shareArabic,
            translationText = shareTranslation,
            // «Qaynağı daxil et» keçidi şəklə də şamildir — vərəqdə söndürüləndə şəkildə də
            // görünməməlidir; əvvəl yalnız mətn paylaşımına təsir edirdi.
            reference = if (includeSource) hadith.source.orEmpty() else "",
            // Vərəqdəki «qeydi əlavə et» keçidi şəklə də şamildir — mətn paylaşımı ilə şəkil
            // paylaşımı eyni seçimlərdən çıxsın deyə.
            note = hadith.note?.takeIf { includeNote },
            includeArabic = includeArabic,
            includeAzerbaijani = includeAzerbaijani,
            onBack = { showImageEditor = false },
        )
    }

    // Paylaşılan mətn yalnız hədisin özüdür: «Hədis №N» başlığı və «Qaynaq:» etiketi çıxarılıb —
    // nömrə tətbiqin öz sıralamasıdır, qaynaq isə etiketsiz də oxunur. Şəkil paylaşımı öz
    // başlığını (`eyebrow`) saxlayır, orada mətn kartın içindədir.
    val buildShareText = {
        buildString {
            if (includeArabic) append(shareArabic).append("\n\n")
            if (includeAzerbaijani) append(shareTranslation).append("\n\n")
            if (includeNote && !hadith.note.isNullOrEmpty()) {
                append("$labelNote: ").append(hadith.note).append("\n\n")
            }
            if (includeSource && !hadith.source.isNullOrEmpty()) {
                append(hadith.source)
            }
        }.trim()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = colorScheme.scrim.alpha(0.5f),
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            BottomSheetHeader(title = stringResource(Res.string.hadithShareTitle), hasDragHandle = true)

            Column(
                modifier = Modifier
                    .weight(1f, false)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Rəvayət seçimi ən başdadır: qalan keçidlər onun **üstündə** işləyir (mötərizə
                // təmizləməsi, ərəbcə/tərcümə), ona görə əvvəlcə hansı mətn olduğu bilinməlidir.
                if (multiNarration) {
                    NarrationPicker(
                        parts = narrationsAz,
                        partial = partialNarrations,
                        onPartialChange = { partialNarrations = it },
                        selected = selectedNarrations,
                        onToggle = { index ->
                            selectedNarrations =
                                if (index in selectedNarrations) selectedNarrations - index
                                else selectedNarrations + index
                        },
                        dropMarker = dropNarrationMarker,
                        onDropMarkerChange = { dropNarrationMarker = it },
                        showDropMarker = selectionHasMarker,
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = colorScheme.outlineVariant.alpha(0.5f),
                    )
                }

                CheckboxRow(label = stringResource(Res.string.hadithIncludeArabic), checked = includeArabic, onCheckedChange = { includeArabic = it })
                CheckboxRow(label = stringResource(Res.string.hadithIncludeTranslation), checked = includeAzerbaijani, onCheckedChange = { includeAzerbaijani = it })
                CheckboxRow(label = stringResource(Res.string.hadithIncludeSource), checked = includeSource, onCheckedChange = { includeSource = it })
                CheckboxRow(label = stringResource(Res.string.hadithIncludeNote), checked = includeNote, onCheckedChange = { includeNote = it })
                CheckboxRow(label = stringResource(Res.string.translShowParentheses), checked = showParentheses, onCheckedChange = { showParentheses = it })
            }

            if (shareWaysOpen) {
                ShareWaysRow(
                    onShareAsText = {
                        PlatformUtils.shareText(buildShareText(), chooserTitle)
                        onDismiss()
                    },
                    onShareAsImage = { showImageEditor = true },
                    onCopyText = {
                        PlatformUtils.copyToClipboard(buildShareText())
                        PlatformUtils.showClipboardMessage(clipboardMsg)
                        onDismiss()
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.strLabelCancel))
                }

                if (!shareWaysOpen) {
                    Button(
                        enabled = hasContent,
                        onClick = { shareWaysOpen = true },
                    ) {
                        Text(stringResource(Res.string.strLabelShare))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Seçilmiş rəvayətlərdən paylaşılacaq mətn.
 *
 * Hamısı seçilib işarə də saxlanılırsa **orijinal mətn** qaytarılır: parçaları yenidən yapışdırmaq
 * mətnin öz boşluqlarını dəyişərdi. Qalan hallarda parçalar boş sətirlə ayrılır — işarə atılanda
 * rəvayətlərin arasında görünən sərhəd yalnız budur.
 */
private fun composeNarrations(
    whole: String,
    parts: List<String>,
    selected: Set<Int>,
    dropMarker: Boolean,
): String {
    if (selected.size == parts.size && !dropMarker) return whole

    return parts
        .filterIndexed { index, _ -> index in selected }
        .map { if (dropMarker) HadithExcerpt.withoutNarrationMarker(it) else it }
        .joinToString("\n\n")
}

/** «Hədisin hamısı, yoxsa bir qismi?» — vərəqin içindəki rəvayət seçimi. */
@Composable
private fun NarrationPicker(
    parts: List<String>,
    partial: Boolean,
    onPartialChange: (Boolean) -> Unit,
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
    dropMarker: Boolean,
    onDropMarkerChange: (Boolean) -> Unit,
    showDropMarker: Boolean,
) {
    Text(
        text = stringResource(Res.string.hadithNarrationPickerTitle),
        style = typography.titleSmall,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )

    Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Chip(
            selected = !partial,
            onClick = { onPartialChange(false) },
            label = {
                Text(
                    text = stringResource(Res.string.dailyContentWholeHadith),
                    style = typography.labelMedium,
                )
            },
        )

        Chip(
            selected = partial,
            onClick = { onPartialChange(true) },
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )

        parts.forEachIndexed { index, part ->
            NarrationRow(
                label = stringResource(Res.string.hadithNarrationPartLabel, index + 1),
                preview = part,
                checked = index in selected,
                onCheckedChange = { onToggle(index) },
            )
        }
    }

    if (showDropMarker) {
        CheckboxRow(
            label = stringResource(Res.string.hadithNarrationDropMarker),
            checked = dropMarker,
            onCheckedChange = onDropMarkerChange,
        )
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
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        androidx.compose.material3.Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = androidx.compose.material3.CheckboxDefaults.colors(
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

@Composable
private fun CheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = androidx.compose.material3.CheckboxDefaults.colors(
                checkedColor = colorScheme.primary,
                uncheckedColor = colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(0.dp)
        )

        Text(
            text = label,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
