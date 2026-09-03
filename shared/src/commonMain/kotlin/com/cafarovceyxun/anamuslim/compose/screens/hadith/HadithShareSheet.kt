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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
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
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheetHeader
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences
import com.cafarovceyxun.anamuslim.utils.supabase.Hadith
import com.cafarovceyxun.anamuslim.utils.verse.HadithExcerpt
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.copiedToClipboard
import com.cafarovceyxun.anamuslim.resources.dr_icon_share
import com.cafarovceyxun.anamuslim.resources.hadith
import com.cafarovceyxun.anamuslim.resources.hadithIncludeArabic
import com.cafarovceyxun.anamuslim.resources.hadithIncludeNote
import com.cafarovceyxun.anamuslim.resources.hadithIncludeSource
import com.cafarovceyxun.anamuslim.resources.hadithIncludeTranslation
import com.cafarovceyxun.anamuslim.resources.hadithShareTitle
import com.cafarovceyxun.anamuslim.resources.hadithWhatsappStyling
import com.cafarovceyxun.anamuslim.resources.openImageEditor
import com.cafarovceyxun.anamuslim.resources.source
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelCopy
import com.cafarovceyxun.anamuslim.resources.strLabelShare
import com.cafarovceyxun.anamuslim.resources.strTitleNote
import com.cafarovceyxun.anamuslim.resources.translShowParentheses
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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
    var whatsappStyling by remember { mutableStateOf(false) }
    var showImageEditor by remember { mutableStateOf(false) }
    var showNarrationPicker by remember { mutableStateOf(false) }

    // Şəkil redaktoruna gedən mətnlər. Null = «hələ seçilməyib», yəni tam mətn — seçim dialoqu
    // yalnız çoxrəvayətli hədisdə açılır, ona görə hədisin böyük hissəsi bu yolu heç görmür.
    var imageTranslationOverride by remember(hadith) { mutableStateOf<String?>(null) }
    var imageArabicOverride by remember(hadith) { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        showParentheses = HadithPreferences.getShowParentheses()
    }

    val labelHadith = stringResource(Res.string.hadith)
    val labelNote = stringResource(Res.string.strTitleNote)
    val labelSource = stringResource(Res.string.source)
    val chooserTitle = stringResource(Res.string.hadithShareTitle)

    // Mötərizə təmizləməsi bir yerdə edilir ki, mətn və şəkil paylaşımı eyni mətni versin.
    val azText = remember(hadith, showParentheses) {
        if (showParentheses) hadith.text_az
        else hadith.text_az.replace(Regex("\\(([\\s\\S]*?)\\)"), "").replace(Regex("\\s+"), " ").trim()
    }

    // Redaktor öz tam ekran `Dialog` pəncərəsindədir, ona görə vərəq altda kompozisiyada qalır və
    // geri qayıdanda seçimlər (ərəbcə/tərcümə/qaynaq/qeyd/mötərizə) olduğu kimi durur. Əvvəl geri
    // düyməsi vərəqi də bağlayıb hədis siyahısına atırdı — bir şəkli yenidən düzəltmək üçün bütün
    // yolu təzədən keçmək lazım gəlirdi.
    if (showImageEditor) {
        HadithImageEditorScreen(
            eyebrow = "$labelHadith №${hadith.hadith_no}",
            arabicText = imageArabicOverride ?: hadith.text_ar,
            translationText = imageTranslationOverride ?: azText,
            reference = hadith.source.orEmpty(),
            // Vərəqdəki «qeydi əlavə et» keçidi şəklə də şamildir — mətn paylaşımı ilə şəkil
            // paylaşımı eyni seçimlərdən çıxsın deyə.
            note = hadith.note?.takeIf { includeNote },
            includeArabic = includeArabic,
            includeAzerbaijani = includeAzerbaijani,
            onBack = { showImageEditor = false },
        )
    }

    // Bir hədis çox vaxt bir neçə rəvayətdən ibarətdir; şəkil kimi hamısını paylaşmaq mətni
    // oxunmaz edir, ona görə redaktordan **əvvəl** «hamısı, yoxsa bir qismi» soruşulur.
    // Mötərizə təmizləməsindən sonrakı mətn bölünür ki, seçim ekranda görünənlə eyni olsun.
    val narrationsAz = remember(azText) { HadithExcerpt.narrationParts(azText) }
    val narrationsAr = remember(hadith) { HadithExcerpt.narrationParts(hadith.text_ar) }

    if (showNarrationPicker) {
        HadithNarrationPickerDialog(
            translationParts = narrationsAz,
            arabicParts = narrationsAr,
            onDismiss = { showNarrationPicker = false },
            onConfirm = { translation, arabic ->
                imageTranslationOverride = translation
                imageArabicOverride = arabic
                showNarrationPicker = false
                showImageEditor = true
            },
        )
    }

    val buildShareText = {
        buildString {
            if (whatsappStyling) append("*$labelHadith №${hadith.hadith_no}*") else append("$labelHadith №${hadith.hadith_no}")
            append("\n\n")
            if (includeArabic) append(hadith.text_ar).append("\n\n")
            if (includeAzerbaijani) append(azText).append("\n\n")
            if (includeNote && !hadith.note.isNullOrEmpty()) {
                if (whatsappStyling) append("*$labelNote:* ") else append("$labelNote: ")
                append(hadith.note).append("\n\n")
            }
            if (includeSource && !hadith.source.isNullOrEmpty()) {
                if (whatsappStyling) append("_$labelSource: ${hadith.source}_") else append("$labelSource: ${hadith.source}")
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
                CheckboxRow(label = stringResource(Res.string.hadithWhatsappStyling), checked = whatsappStyling, onCheckedChange = { whatsappStyling = it })
                CheckboxRow(label = stringResource(Res.string.hadithIncludeArabic), checked = includeArabic, onCheckedChange = { includeArabic = it })
                CheckboxRow(label = stringResource(Res.string.hadithIncludeTranslation), checked = includeAzerbaijani, onCheckedChange = { includeAzerbaijani = it })
                CheckboxRow(label = stringResource(Res.string.hadithIncludeSource), checked = includeSource, onCheckedChange = { includeSource = it })
                CheckboxRow(label = stringResource(Res.string.hadithIncludeNote), checked = includeNote, onCheckedChange = { includeNote = it })
                CheckboxRow(label = stringResource(Res.string.translShowParentheses), checked = showParentheses, onCheckedChange = { showParentheses = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.strLabelCancel))
                }

                Button(
                    onClick = {
                        PlatformUtils.copyToClipboard(buildShareText())
                        PlatformUtils.showClipboardMessage(clipboardMsg)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.secondaryContainer, contentColor = colorScheme.onSecondaryContainer)
                ) {
                    Text(stringResource(Res.string.strLabelCopy))
                }

                Button(
                    onClick = {
                        PlatformUtils.shareText(buildShareText(), chooserTitle)
                        onDismiss()
                    }
                ) {
                    Text(stringResource(Res.string.strLabelShare))
                }
            }

            // Şəkil paylaşma düyməsi - İndi Editoru açır
            Button(
                onClick = {
                    if (narrationsAz.size > 1) {
                        showNarrationPicker = true
                    } else {
                        imageTranslationOverride = null
                        imageArabicOverride = null
                        showImageEditor = true
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primaryContainer,
                    contentColor = colorScheme.onPrimaryContainer
                )
            ) {
                androidx.compose.material3.Icon(
                    painter = painterResource(Res.drawable.dr_icon_share),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.openImageEditor))
            }
            Spacer(Modifier.height(16.dp))
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
