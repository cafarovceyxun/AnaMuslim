package com.cafarovceyxun.anamuslim.compose.screens.dua

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.share.ShareWaysRow
import com.cafarovceyxun.anamuslim.resources.duaShareImageNeedsOriginal
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheet
import com.cafarovceyxun.anamuslim.compose.screens.hadith.FormTextField
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.copiedToClipboard
import com.cafarovceyxun.anamuslim.resources.dr_icon_share
import com.cafarovceyxun.anamuslim.resources.hadithShareEditText
import com.cafarovceyxun.anamuslim.resources.hadithShareResetText
import com.cafarovceyxun.anamuslim.resources.labelArabic
import com.cafarovceyxun.anamuslim.resources.labelTranslation
import com.cafarovceyxun.anamuslim.resources.labelTransliteration
import com.cafarovceyxun.anamuslim.resources.source
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelCopy
import com.cafarovceyxun.anamuslim.resources.strLabelShare
import com.cafarovceyxun.anamuslim.resources.strTitleNote
import com.cafarovceyxun.anamuslim.utils.supabase.DuaSourceRef
import org.jetbrains.compose.resources.stringResource

/**
 * Duanı/dəlili paylaşma vərəqi — nəyin göndəriləcəyini seçmək və mətni əl ilə düzəltmək.
 *
 * Əvvəl paylaşma düyməsi bütün blokları sabit qaydada birləşdirib göndərirdi: kiməsə yalnız ərəbcəni,
 * kiməsə yalnız tərcüməni göndərmək mümkün deyildi. Hədisdəki [com.cafarovceyxun.anamuslim.compose
 * .screens.hadith.HadithShareSheet] ilə eyni fikir, amma sadələşdirilmiş — duada rəvayət seçimi və
 * mötərizə qaydaları yoxdur.
 *
 * [DuaSourceRef] üzərində qurulub, `Dua` üzərində yox: eyni vərəq həm Dua ekranında, həm də Əsmaül
 * Hüsnə dəlillərində işləyir.
 *
 * @param initialParts açılış seçimi — ekranda görünən bloklar (bax [DuaShareParts.visible]).
 */
@Composable
internal fun DuaShareSheet(
    ref: DuaSourceRef?,
    initialParts: DuaShareParts,
    /** Şəkil kartının üst etiketi — mövzunun adı. `null` = etiket çəkilmir. */
    eyebrow: String? = null,
    onDismiss: () -> Unit,
) {
    val isOpen = ref != null

    var parts by remember(ref) { mutableStateOf(initialParts) }
    var showImageEditor by remember(ref) { mutableStateOf(false) }

    // Çekboks dəyişəndə əl ilə yazılmış mətn köhnəlir; istifadəçi «Bərpa et» ilə geri qayıda bilir,
    // amma seçim dəyişəndə redaktəni səssizcə atmaq da olmaz — ona görə yalnız `ref` dəyişəndə,
    // yəni başqa dua paylaşılanda sıfırlanır (`remember(ref)`).
    var edited by remember(ref) { mutableStateOf<String?>(null) }

    val generated = remember(ref, parts) { ref?.let { buildDuaShareText(it, parts) }.orEmpty() }
    val text = edited ?: generated

    /** Mətn hələ də bloklardan qurulan mətndirmi — şəkil yolunun şərti. */
    val textIsGenerated = edited == null || edited == generated

    val chooserTitle = stringResource(Res.string.strLabelShare)
    val clipboardMsg = stringResource(Res.string.copiedToClipboard)

    // Redaktor öz tam ekran `Dialog` pəncərəsindədir, ona görə vərəq altda kompozisiyada qalır və
    // geri qayıdanda çekbokslar olduğu kimi durur (hədis vərəqindəki qurğunun eynisi).
    if (showImageEditor && ref != null) {
        DuaImageEditorScreen(
            ref = ref,
            parts = parts,
            eyebrow = eyebrow,
            onBack = { showImageEditor = false },
        )
    }

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        icon = Res.drawable.dr_icon_share,
        title = chooserTitle,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ShareCheckRow(
                label = stringResource(Res.string.labelArabic),
                checked = parts.arabic,
                enabled = !ref?.text_ar.isNullOrBlank(),
                onCheckedChange = { parts = parts.copy(arabic = it) },
            )
            ShareCheckRow(
                label = stringResource(Res.string.labelTransliteration),
                checked = parts.transliteration,
                enabled = !ref?.transliteration.isNullOrBlank(),
                onCheckedChange = { parts = parts.copy(transliteration = it) },
            )
            ShareCheckRow(
                label = stringResource(Res.string.labelTranslation),
                checked = parts.translation,
                enabled = !ref?.text_az.isNullOrBlank(),
                onCheckedChange = { parts = parts.copy(translation = it) },
            )
            ShareCheckRow(
                label = stringResource(Res.string.strTitleNote),
                checked = parts.note,
                enabled = !ref?.note.isNullOrBlank(),
                onCheckedChange = { parts = parts.copy(note = it) },
            )
            ShareCheckRow(
                label = stringResource(Res.string.source),
                checked = parts.source,
                enabled = !ref?.source.isNullOrBlank(),
                onCheckedChange = { parts = parts.copy(source = it) },
            )

            Spacer(Modifier.height(8.dp))

            // Mətn birbaşa redaktə olunur: bəzən bir cümlə artıq olur və onu silmək üçün əvvəlcə
            // kopyalayıb başqa yerdə düzəltmək lazım gəlirdi.
            FormTextField(
                value = text,
                onValueChange = { edited = it },
                label = stringResource(Res.string.hadithShareEditText),
                icon = Res.drawable.dr_icon_share,
                minLines = 4,
                maxLines = 10,
                textStyle = typography.bodyMedium.withScriptDirection(arabic = false),
                onClear = { edited = "" },
            )

            if (edited != null && edited != generated) {
                TextButton(onClick = { edited = null }) {
                    Text(stringResource(Res.string.hadithShareResetText))
                }
            }

            // Üç yol: mətn, şəkil, pano — hədis və ayə vərəqləri ilə **eyni** sıra
            // ([ShareWaysRow]), yoxsa üç ekran eyni sualı üç cür soruşardı.
            ShareWaysRow(
                onShareAsText = {
                    PlatformUtils.shareText(text, chooserTitle)
                    onDismiss()
                },
                onShareAsImage = { showImageEditor = true },
                onCopyText = {
                    PlatformUtils.copyToClipboard(text)
                    PlatformUtils.showClipboardMessage(clipboardMsg)
                    onDismiss()
                },
                modifier = Modifier.padding(vertical = 8.dp),
                // Şəkil kartı **bloklardan** qurulur (ərəbcə öz üzü ilə, latın mətni ayrıca), ona
                // görə əl ilə yazılmış bir parça mətni daşıya bilmir. Xananı basılan saxlasaydıq,
                // istifadəçi yazdığını gözləyib **başqa** mətn görərdi.
                imageEnabled = textIsGenerated,
            )

            if (!textIsGenerated) {
                Text(
                    text = stringResource(Res.string.duaShareImageNeedsOriginal),
                    style = typography.labelSmall.withScriptDirection(arabic = false),
                    color = colorScheme.onSurfaceVariant.alpha(0.8f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.strLabelCancel))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Bir çekboks sətri.
 *
 * Blok mətni boşdursa sətir **sönür**, gizlənmir: yoxa çıxan sətir «niyə dörd seçim var, indi isə
 * üç?» sualı yaradır, sönmüş sətir isə həmin duada o blokun olmadığını deyir.
 */
@Composable
private fun ShareCheckRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked && enabled,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked && enabled,
            onCheckedChange = null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = colorScheme.primary,
                uncheckedColor = colorScheme.onSurfaceVariant,
            ),
        )

        Text(
            text = label,
            style = typography.bodyMedium.withScriptDirection(arabic = false),
            color = if (enabled) colorScheme.onSurface else colorScheme.onSurface.alpha(0.4f),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
