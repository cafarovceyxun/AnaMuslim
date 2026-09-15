package com.cafarovceyxun.anamuslim.compose.screens.dua

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.ReadableWidthColumn
import com.cafarovceyxun.anamuslim.compose.components.common.SwitchItem
import com.cafarovceyxun.anamuslim.compose.screens.hadith.FormTextField
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.arabicFontFamily
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.asmaEditEvidenceTitle
import com.cafarovceyxun.anamuslim.resources.asmaEditNameTitle
import com.cafarovceyxun.anamuslim.resources.asmaFieldDescription
import com.cafarovceyxun.anamuslim.resources.asmaFieldMeaning
import com.cafarovceyxun.anamuslim.resources.asmaFieldNameAr
import com.cafarovceyxun.anamuslim.resources.asmaFieldTransliteration
import com.cafarovceyxun.anamuslim.resources.asmaHiddenNote
import com.cafarovceyxun.anamuslim.resources.asmaVisibleLabel
import com.cafarovceyxun.anamuslim.resources.dr_icon_eye
import com.cafarovceyxun.anamuslim.resources.dr_icon_footnote
import com.cafarovceyxun.anamuslim.resources.dr_icon_quran_script
import com.cafarovceyxun.anamuslim.resources.dr_icon_sort
import com.cafarovceyxun.anamuslim.resources.dr_icon_translations
import com.cafarovceyxun.anamuslim.resources.duaEditTitle
import com.cafarovceyxun.anamuslim.resources.duaFieldNote
import com.cafarovceyxun.anamuslim.resources.duaPickerArabicLabel
import com.cafarovceyxun.anamuslim.resources.duaPickerCountLabel
import com.cafarovceyxun.anamuslim.resources.duaPickerSave
import com.cafarovceyxun.anamuslim.resources.duaTranslationOptional
import com.cafarovceyxun.anamuslim.resources.duaTransliterationOptional
import com.cafarovceyxun.anamuslim.utils.supabase.AsmaEvidence
import com.cafarovceyxun.anamuslim.utils.supabase.AsmaName
import com.cafarovceyxun.anamuslim.utils.supabase.Dua
import org.jetbrains.compose.resources.stringResource

/**
 * Əlavə olunmuş duanın redaktəsi — giriş etmiş istifadəçi üçün.
 *
 * **Mənbə dəyişmir** (hansı hədis/ayə): onu redaktədə dəyişmək yeni sətir yaratmaqla eynidir və
 * `md5(text_ar)` unikal indeksi ilə də toqquşa bilər. Dəyişən yalnız göstərilən mətn, qeyd və
 * zikr sayıdır.
 */
@Composable
fun DuaEditDialog(
    dua: Dua,
    isSaving: Boolean,
    onSave: (Dua) -> Unit,
    onDismiss: () -> Unit,
) {
    var arabic by remember(dua) { mutableStateOf(dua.text_ar) }
    var translit by remember(dua) { mutableStateOf(dua.transliteration.orEmpty()) }
    var translation by remember(dua) { mutableStateOf(dua.text_az) }
    var note by remember(dua) { mutableStateOf(dua.note.orEmpty()) }
    var count by remember(dua) { mutableStateOf(dua.repeat_count?.toString().orEmpty()) }

    // Yalnız ərəbcə məcburidir — bax seçim ekranındakı eyni qayda.
    val valid = arabic.isNotBlank()

    EditFormDialog(
        title = stringResource(Res.string.duaEditTitle),
        isSaving = isSaving,
        canSave = valid,
        onSave = {
            onSave(
                dua.copy(
                    text_ar = arabic.trim(),
                    text_az = translation.trim(),
                    transliteration = translit.trim().takeIf { it.isNotBlank() },
                    note = note.trim().takeIf { it.isNotBlank() },
                    repeat_count = count.toIntOrNull()?.takeIf { it > 0 },
                ),
            )
        },
        onDismiss = onDismiss,
    ) {
        ArabicField(
            value = arabic,
            onValueChange = { arabic = it },
            label = stringResource(Res.string.duaPickerArabicLabel),
        )

        FormTextField(
            value = translit,
            onValueChange = { translit = it },
            label = stringResource(Res.string.duaTransliterationOptional),
            icon = Res.drawable.dr_icon_translations,
            minLines = 2,
            maxLines = 6,
            onClear = { translit = "" },
        )

        FormTextField(
            value = translation,
            onValueChange = { translation = it },
            label = stringResource(Res.string.duaTranslationOptional),
            icon = Res.drawable.dr_icon_translations,
            minLines = 3,
            maxLines = 10,
            onClear = { translation = "" },
        )

        FormTextField(
            value = note,
            onValueChange = { note = it },
            label = stringResource(Res.string.duaFieldNote),
            icon = Res.drawable.dr_icon_footnote,
            minLines = 2,
            maxLines = 6,
            onClear = { note = "" },
        )

        FormTextField(
            value = count,
            onValueChange = { input -> count = input.filter { it.isDigit() }.take(6) },
            label = stringResource(Res.string.duaPickerCountLabel),
            icon = Res.drawable.dr_icon_sort,
            keyboardType = KeyboardType.Number,
            onClear = { count = "" },
        )
    }
}

/** Ada bağlanmış dəlilin redaktəsi. Mənbə burada da dəyişmir — bax [DuaEditDialog]. */
@Composable
fun AsmaEvidenceEditDialog(
    evidence: AsmaEvidence,
    isSaving: Boolean,
    onSave: (AsmaEvidence) -> Unit,
    onDismiss: () -> Unit,
) {
    var arabic by remember(evidence) { mutableStateOf(evidence.text_ar) }
    var translit by remember(evidence) { mutableStateOf(evidence.transliteration.orEmpty()) }
    var translation by remember(evidence) { mutableStateOf(evidence.text_az) }
    var note by remember(evidence) { mutableStateOf(evidence.note.orEmpty()) }

    // Yalnız ərəbcə məcburidir — bax seçim ekranındakı eyni qayda.
    val valid = arabic.isNotBlank()

    EditFormDialog(
        title = stringResource(Res.string.asmaEditEvidenceTitle),
        isSaving = isSaving,
        canSave = valid,
        onSave = {
            onSave(
                evidence.copy(
                    text_ar = arabic.trim(),
                    text_az = translation.trim(),
                    transliteration = translit.trim().takeIf { it.isNotBlank() },
                    note = note.trim().takeIf { it.isNotBlank() },
                ),
            )
        },
        onDismiss = onDismiss,
    ) {
        ArabicField(
            value = arabic,
            onValueChange = { arabic = it },
            label = stringResource(Res.string.duaPickerArabicLabel),
        )

        FormTextField(
            value = translit,
            onValueChange = { translit = it },
            label = stringResource(Res.string.duaTransliterationOptional),
            icon = Res.drawable.dr_icon_translations,
            minLines = 2,
            maxLines = 6,
            onClear = { translit = "" },
        )

        FormTextField(
            value = translation,
            onValueChange = { translation = it },
            label = stringResource(Res.string.duaTranslationOptional),
            icon = Res.drawable.dr_icon_translations,
            minLines = 3,
            maxLines = 10,
            onClear = { translation = "" },
        )

        FormTextField(
            value = note,
            onValueChange = { note = it },
            label = stringResource(Res.string.duaFieldNote),
            icon = Res.drawable.dr_icon_footnote,
            minLines = 2,
            maxLines = 6,
            onClear = { note = "" },
        )
    }
}

/**
 * Əsmaül Hüsnədəki bir adın redaktəsi — mətnlər və **görünüş** açarı.
 *
 * «Sil» yoxdur: `asma_evidence.name_no` CASCADE-dir, ad silinsə ona bağlanmış bütün dəlillər də
 * gedərdi. Siyahıdan çıxarmaq üçün görünüş bağlanır — sətir və dəlilləri yerində qalır, adi
 * istifadəçi onu görmür.
 */
@Composable
fun AsmaNameEditDialog(
    name: AsmaName,
    isSaving: Boolean,
    onSave: (AsmaName) -> Unit,
    onDismiss: () -> Unit,
) {
    var nameAr by remember(name) { mutableStateOf(name.name_ar) }
    var transliteration by remember(name) { mutableStateOf(name.transliteration) }
    var meaning by remember(name) { mutableStateOf(name.meaning) }
    var description by remember(name) { mutableStateOf(name.description.orEmpty()) }
    var visible by remember(name) { mutableStateOf(name.is_visible) }

    val valid = nameAr.isNotBlank() && transliteration.isNotBlank() && meaning.isNotBlank()

    EditFormDialog(
        title = stringResource(Res.string.asmaEditNameTitle),
        isSaving = isSaving,
        canSave = valid,
        onSave = {
            onSave(
                name.copy(
                    name_ar = nameAr.trim(),
                    transliteration = transliteration.trim(),
                    meaning = meaning.trim(),
                    description = description.trim().takeIf { it.isNotBlank() },
                    is_visible = visible,
                ),
            )
        },
        onDismiss = onDismiss,
    ) {
        ArabicField(
            value = nameAr,
            onValueChange = { nameAr = it },
            label = stringResource(Res.string.asmaFieldNameAr),
            minLines = 1,
            maxLines = 2,
        )

        FormTextField(
            value = transliteration,
            onValueChange = { transliteration = it },
            label = stringResource(Res.string.asmaFieldTransliteration),
            icon = Res.drawable.dr_icon_translations,
            onClear = { transliteration = "" },
        )

        FormTextField(
            value = meaning,
            onValueChange = { meaning = it },
            label = stringResource(Res.string.asmaFieldMeaning),
            icon = Res.drawable.dr_icon_footnote,
            minLines = 2,
            maxLines = 4,
            onClear = { meaning = "" },
        )

        FormTextField(
            value = description,
            onValueChange = { description = it },
            label = stringResource(Res.string.asmaFieldDescription),
            icon = Res.drawable.dr_icon_footnote,
            minLines = 3,
            maxLines = 10,
            onClear = { description = "" },
        )

        // `SwitchItem` etiketi `StringResource` istəyir — həll olunmuş sətir yox.
        SwitchItem(
            title = Res.string.asmaVisibleLabel,
            icon = Res.drawable.dr_icon_eye,
            checked = visible,
            onCheckedChange = { visible = it },
        )

        Text(
            text = stringResource(Res.string.asmaHiddenNote),
            style = typography.labelSmall.withScriptDirection(arabic = false),
            color = colorScheme.onSurfaceVariant.alpha(0.75f),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Ərəbcə mətn sahəsi — öz yazısına və şriftinə bağlı. */
@Composable
private fun ArabicField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    minLines: Int = 3,
    maxLines: Int = 10,
) {
    FormTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        icon = Res.drawable.dr_icon_quran_script,
        minLines = minLines,
        maxLines = maxLines,
        textStyle = typography.bodyLarge.withScriptDirection(
            arabic = true,
            arabicFontFamily = arabicFontFamily(),
        ),
        onClear = { onValueChange("") },
    )
}

/**
 * Üç redaktə formasının ortaq kadrı.
 *
 * Tam ekran **`Dialog`**-dur (CLAUDE.md qaydası): forma dua/əsma ekranından açılır, o ekran isə
 * özü tam-ekran pəncərədədir — inline emit ediləndə həmin pəncərənin sürüşən sütununun içində
 * qalardı.
 */
@Composable
private fun EditFormDialog(
    title: String,
    isSaving: Boolean,
    canSave: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) = Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
        dismissOnBackPress = true,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false,
    ),
) {
    Scaffold(
        topBar = { AppBar(title = title, onBack = onDismiss) },
        bottomBar = {
            Surface(color = colorScheme.surfaceContainer, shadowElevation = 6.dp) {
                ReadableWidthColumn {
                    Button(
                        onClick = onSave,
                        enabled = canSave && !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = colorScheme.onPrimary,
                            )
                        } else {
                            Text(stringResource(Res.string.duaPickerSave))
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            // ⚠️ `ReadableWidthColumn` **Box**-dur — daxili `Column` məcburidir.
            ReadableWidthColumn {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Spacer(Modifier.height(4.dp))
                    content()
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
