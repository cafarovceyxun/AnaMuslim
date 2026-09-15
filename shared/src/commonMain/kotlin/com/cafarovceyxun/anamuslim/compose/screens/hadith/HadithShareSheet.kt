package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.cafarovceyxun.anamuslim.utils.supabase.HadithLocation
import com.cafarovceyxun.anamuslim.utils.verse.HadithExcerpt
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.copiedToClipboard
import com.cafarovceyxun.anamuslim.resources.dr_icon_edit
import com.cafarovceyxun.anamuslim.resources.dailyContentPartOfHadith
import com.cafarovceyxun.anamuslim.resources.dailyContentWholeHadith
import com.cafarovceyxun.anamuslim.resources.hadith
import com.cafarovceyxun.anamuslim.resources.hadithAdditionalSource
import com.cafarovceyxun.anamuslim.resources.hadithIncludeArabic
import com.cafarovceyxun.anamuslim.resources.hadithIncludeLocation
import com.cafarovceyxun.anamuslim.resources.hadithIncludeNote
import com.cafarovceyxun.anamuslim.resources.hadithIncludeSource
import com.cafarovceyxun.anamuslim.resources.hadithIncludeTranslation
import com.cafarovceyxun.anamuslim.resources.hadithNarrationDropMarker
import com.cafarovceyxun.anamuslim.resources.hadithNarrationPartLabel
import com.cafarovceyxun.anamuslim.resources.hadithNarrationPickerHint
import com.cafarovceyxun.anamuslim.resources.hadithNarrationPickerTitle
import com.cafarovceyxun.anamuslim.resources.hadithShareEditText
import com.cafarovceyxun.anamuslim.resources.hadithShareEditedNoBranding
import com.cafarovceyxun.anamuslim.resources.hadithShareResetText
import com.cafarovceyxun.anamuslim.resources.hadithShareTitle
import com.cafarovceyxun.anamuslim.resources.labelArabic
import com.cafarovceyxun.anamuslim.resources.labelTranslation
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelShare
import com.cafarovceyxun.anamuslim.resources.strTitleNote
import com.cafarovceyxun.anamuslim.resources.translShowParentheses
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Hədisin paylaşma vərəqi — iki mərhələ: əvvəlcə mətn hazırlanır (hansı rəvayətlər, sonra
 * ərəbcə/tərcümə/qaynaq/qeyd/mötərizə), «Paylaş» basılandan sonra isə yol seçilir — mətn kimi,
 * şəkil kimi, yoxsa panoya.
 *
 * Rəvayət seçimi əvvəl **yalnız** şəkil redaktoruna girərkən ayrıca dialoqda soruşulurdu, ona görə
 * kopyalanan və paylaşılan mətn həmişə hədisin hamısı olurdu. İndi seçim vərəqin başındadır və hər
 * üç yol eyni mətndən çıxır.
 *
 * ### Mətnə əl gəzdirmək və nişan
 * Mətn vərəqin içində redaktə oluna bilir (qısaltma, sitat üçün kəsmə). Dəyişdirilmiş mətnə tətbiqin
 * **loğosu və mağaza QR-i qoyulmur**: nişan «bu, tətbiqdəki mətndir» deməkdir, əl gəzdirilmiş
 * sitatda isə bu doğru olmaz. Qadağa şəkil redaktorunda tətbiq olunur ([ShareImageEditorScreen]-in
 * `brandingAllowed` parametri) — çipləri gizlətməklə deyil, ümumiyyətlə çəkməməklə.
 *
 * @param location hədisin ağacdakı yeri — «əlavə qaynaq» sətri (cild · kitab · bab · alt bab).
 *   Boş ola bilər (hələ yüklənməyib və ya baza natamamdır); belə olanda həmin keçid göstərilmir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithShareSheet(
    hadith: Hadith?,
    location: HadithLocation,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (hadith == null) return

    val clipboardMsg = stringResource(Res.string.copiedToClipboard)
    
    var includeArabic by remember { mutableStateOf(true) }
    var includeAzerbaijani by remember { mutableStateOf(true) }
    var includeSource by remember { mutableStateOf(true) }
    var includeNote by remember { mutableStateOf(true) }
    var includeLocation by remember { mutableStateOf(true) }
    var showParentheses by remember { mutableStateOf(true) }
    var showImageEditor by remember { mutableStateOf(false) }
    var showTextEditor by remember { mutableStateOf(false) }

    // «Paylaş» basılana qədər yollar gizlidir: vərəq açılanda sual «necə paylaşım», «nəyi
    // paylaşım»-dır — üç düymə elə başdan görünəndə mətn hazırlığı onların arasında itirdi.
    var shareWaysOpen by remember(hadith) { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showParentheses = HadithPreferences.getShowParentheses()
    }

    val labelHadith = stringResource(Res.string.hadith)
    val labelNote = stringResource(Res.string.strTitleNote)
    val labelAdditionalSource = stringResource(Res.string.hadithAdditionalSource)
    val chooserTitle = stringResource(Res.string.hadithShareTitle)

    // «Əlavə qaynaq»: hədisin ağacdakı yeri bir sətirdə. Adlar interfeys dilinə görə seçilir
    // ([hadithTitleText]) — ərəbcə interfeysdə səviyyələr öz ərəbcə adı ilə gedir.
    val locationSource = hadithLocationSource(location)
    val hasLocation = locationSource.isNotBlank()

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

    /**
     * Paylaşılacaq mətnin son halı — istifadəçinin əl gəzdirdiyi nüsxə.
     *
     * ⚠️ Açar **hədisin özüdür**, hazırlanmış mətn yox. Qaralamanı `remember(shareTranslation)` ilə
     * saxlamaq iOS-da yazılanı itirirdi: şəkil redaktoru (tam ekran `Dialog` + portret kilidi)
     * açılanda vərəqin kompozisiyası elə bir dəyişiklik keçirir ki, mətnə açarlanmış `remember`
     * yenidən qurulur — istifadəçi mətni redaktə edir, «Şəkil kimi» basır və kartda **orijinal**
     * mətn görünürdü (nişan da qadağan olunmurdu, çünki «dəyişdirilib» şərti sönmüşdü).
     * `rememberSaveable` + aşağıdakı sinxronlaşdırma bunu bağlayır.
     */
    var arabicDraft by rememberSaveable(hadith.id) { mutableStateOf(shareArabic) }
    var translationDraft by rememberSaveable(hadith.id) { mutableStateOf(shareTranslation) }

    // İstifadəçi mətnə toxunubmu. Qaralamanı hazır mətnlə sinxron saxlamağın şərti budur: toxunmayıb
    // — keçidlər (rəvayət seçimi, mötərizə) qaralamanı yeniləyir; toxunub — yazdığı mətn qalır,
    // çünki onu itirmək keçidin qiymətindən qat-qat bahadır. Geri qaytarmaq üçün «Bərpa et» var.
    var touchedText by rememberSaveable(hadith.id) { mutableStateOf(false) }

    LaunchedEffect(shareArabic, shareTranslation, touchedText) {
        if (touchedText) return@LaunchedEffect
        arabicDraft = shareArabic
        translationDraft = shareTranslation
    }

    // «Dəyişdirilib» şərti sadədir: qaralama hazırlanmış mətndən fərqlənir. Geri düzəldəndə
    // (və ya «Bərpa et») şərt özü sönür — ayrıca bayraq saxlasaq, mətn eyni olsa da nişan
    // qadağan qalardı.
    val textEdited = arabicDraft != shareArabic || translationDraft != shareTranslation

    // Boş seçim boş mətn deməkdir — düymələr onda işləmir, əks halda boş şəkil və boş pano qalırdı.
    // Mətnin hamısı silinibsə də eyni: paylaşacaq bir şey yoxdur.
    val hasContent = selection.isNotEmpty() &&
        (arabicDraft.isNotBlank() || translationDraft.isNotBlank())

    // Redaktor öz tam ekran `Dialog` pəncərəsindədir, ona görə vərəq altda kompozisiyada qalır və
    // geri qayıdanda seçimlər (ərəbcə/tərcümə/qaynaq/qeyd/mötərizə) olduğu kimi durur. Əvvəl geri
    // düyməsi vərəqi də bağlayıb hədis siyahısına atırdı — bir şəkli yenidən düzəltmək üçün bütün
    // yolu təzədən keçmək lazım gəlirdi.
    if (showImageEditor) {
        HadithImageEditorScreen(
            eyebrow = "$labelHadith №${hadith.hadith_no}",
            arabicText = arabicDraft,
            translationText = translationDraft,
            // «Qaynağı daxil et» keçidi şəklə də şamildir — vərəqdə söndürüləndə şəkildə də
            // görünməməlidir; əvvəl yalnız mətn paylaşımına təsir edirdi. Eyni qayda «əlavə
            // qaynağa» da şamildir: kartda ikinci sətir kimi gedir.
            reference = buildList {
                if (includeSource) hadith.source?.takeIf { it.isNotBlank() }?.let { add(it) }
                if (includeLocation && hasLocation) add("$labelAdditionalSource: $locationSource")
            }.joinToString("\n"),
            // Vərəqdəki «qeydi əlavə et» keçidi şəklə də şamildir — mətn paylaşımı ilə şəkil
            // paylaşımı eyni seçimlərdən çıxsın deyə.
            note = hadith.note?.takeIf { includeNote },
            includeArabic = includeArabic,
            includeAzerbaijani = includeAzerbaijani,
            // Əl gəzdirilmiş mətn tətbiqin nişanını daşımır — bax yuxarıdakı «Mətnə əl gəzdirmək».
            brandingAllowed = !textEdited,
            onBack = { showImageEditor = false },
        )
    }

    // Paylaşılan mətn yalnız hədisin özüdür: «Hədis №N» başlığı və «Qaynaq:» etiketi çıxarılıb —
    // nömrə tətbiqin öz sıralamasıdır, qaynaq isə etiketsiz də oxunur. Şəkil paylaşımı öz
    // başlığını (`eyebrow`) saxlayır, orada mətn kartın içindədir.
    val buildShareText = {
        buildString {
            if (includeArabic) append(arabicDraft).append("\n\n")
            if (includeAzerbaijani) append(translationDraft).append("\n\n")
            if (includeNote && !hadith.note.isNullOrEmpty()) {
                append("$labelNote: ").append(hadith.note).append("\n\n")
            }
            if (includeSource && !hadith.source.isNullOrEmpty()) {
                append(hadith.source).append("\n")
            }
            // Hədisin öz qaynağı kitabın adını demir; cild → kitab → bab zənciri onu tamamlayır.
            // Ayrıca etiketlə gedir ki, iki sətir bir qaynağın davamı kimi oxunmasın.
            if (includeLocation && hasLocation) {
                append("$labelAdditionalSource: ").append(locationSource)
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
        // Mətn redaktoru vərəqin İÇİNDƏDİR: klaviatura açılanda məzmun onun üstünə qalxmalıdır,
        // yoxsa yazılan sahə klaviaturanın altında qalır (vərəqin öz inset-ləri IME-ni saymır).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime),
        ) {
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
                // Yalnız yer məlum olanda: boş keçid «basılır, heç nə olmur» tələsidir.
                if (hasLocation) {
                    CheckboxRow(
                        label = stringResource(Res.string.hadithIncludeLocation),
                        checked = includeLocation,
                        onCheckedChange = { includeLocation = it },
                        description = locationSource,
                    )
                }
                CheckboxRow(label = stringResource(Res.string.hadithIncludeNote), checked = includeNote, onCheckedChange = { includeNote = it })
                CheckboxRow(label = stringResource(Res.string.translShowParentheses), checked = showParentheses, onCheckedChange = { showParentheses = it })

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = colorScheme.outlineVariant.alpha(0.5f),
                )

                HadithShareTextEditor(
                    expanded = showTextEditor,
                    onExpandedChange = { showTextEditor = it },
                    arabic = arabicDraft,
                    onArabicChange = {
                        touchedText = true
                        arabicDraft = it
                    },
                    translation = translationDraft,
                    onTranslationChange = {
                        touchedText = true
                        translationDraft = it
                    },
                    showArabic = includeArabic,
                    showTranslation = includeAzerbaijani,
                    edited = textEdited,
                    onReset = {
                        touchedText = false
                        arabicDraft = shareArabic
                        translationDraft = shareTranslation
                    },
                )
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
    /** Etiketin altındakı solğun sətir — keçidin nəyi əlavə edəcəyini göstərən nümunə. */
    description: String? = null,
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

        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = label)

            if (description != null) {
                Text(
                    text = description,
                    style = typography.bodySmall.withContentDirection(),
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Hədisin yeri bir sətirdə: cild · kitab · bab · alt bab.
 *
 * Boş səviyyələr sadəcə iştirak etmir — natamam bazada sətir «· · Bab» kimi görünməsin.
 */
@Composable
private fun hadithLocationSource(location: HadithLocation): String {
    val volume = location.volume?.let { hadithTitleText(it.name, it.name_ar) }
    val book = location.book?.let { hadithTitleText(it.name, it.name_ar) }
    val chapter = location.chapter?.let { hadithTitleText(it.name, it.name_ar) }
    val subChapter = location.subChapter?.let { hadithTitleText(it.name, it.name_ar) }

    return listOfNotNull(volume, book, chapter, subChapter)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" · ")
}

/**
 * Paylaşılacaq mətnə əl gəzdirmək — vərəqin içində, qatlanan bölmə kimi.
 *
 * Qatlanmış açılır: paylaşımların böyük hissəsi mətni olduğu kimi göndərir, iki böyük mətn sahəsi
 * isə vərəqin qalan hissəsini (rəvayət seçimi, keçidlər) aşağı qovurdu.
 *
 * Mətn dəyişdirildikdə bölmə xəbərdarlığı özü göstərir: şəkildə loğo və QR olmayacaq. Qadağanın
 * özü [HadithShareSheet]-dədir, burada yalnız səbəbi yazılır.
 */
@Composable
private fun HadithShareTextEditor(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    arabic: String,
    onArabicChange: (String) -> Unit,
    translation: String,
    onTranslationChange: (String) -> Unit,
    showArabic: Boolean,
    showTranslation: Boolean,
    edited: Boolean,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = expanded,
                role = Role.Checkbox,
                onValueChange = onExpandedChange,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(Res.drawable.dr_icon_edit),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = colorScheme.onSurfaceVariant,
        )

        Text(
            text = stringResource(Res.string.hadithShareEditText),
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        )

        if (edited) {
            TextButton(onClick = onReset) {
                Text(stringResource(Res.string.hadithShareResetText))
            }
        }
    }

    if (!expanded) return

    if (showArabic) {
        OutlinedTextField(
            value = arabic,
            onValueChange = onArabicChange,
            label = { Text(stringResource(Res.string.labelArabic)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .heightIn(min = 96.dp, max = 200.dp),
            textStyle = typography.bodyMedium.withContentDirection(),
            shape = RoundedCornerShape(12.dp),
        )
    }

    if (showTranslation) {
        OutlinedTextField(
            value = translation,
            onValueChange = onTranslationChange,
            label = { Text(stringResource(Res.string.labelTranslation)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .heightIn(min = 96.dp, max = 240.dp),
            textStyle = typography.bodyMedium.withContentDirection(),
            shape = RoundedCornerShape(12.dp),
        )
    }

    if (edited) {
        Text(
            text = stringResource(Res.string.hadithShareEditedNoBranding),
            style = typography.bodySmall.withContentDirection(),
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
}
