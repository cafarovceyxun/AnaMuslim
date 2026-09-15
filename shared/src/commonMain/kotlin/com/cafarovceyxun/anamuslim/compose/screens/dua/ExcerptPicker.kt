package com.cafarovceyxun.anamuslim.compose.screens.dua

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.ReadableWidthColumn
import com.cafarovceyxun.anamuslim.compose.components.common.SearchTextField
import com.cafarovceyxun.anamuslim.compose.components.common.readableWidthInset
import com.cafarovceyxun.anamuslim.compose.screens.hadith.FormTextField
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.arabicFontFamily
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.asmaNameNo
import com.cafarovceyxun.anamuslim.resources.dr_icon_check
import com.cafarovceyxun.anamuslim.resources.dr_icon_check_circle
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_right
import com.cafarovceyxun.anamuslim.resources.dr_icon_footnote
import com.cafarovceyxun.anamuslim.resources.dr_icon_quran_script
import com.cafarovceyxun.anamuslim.resources.dr_icon_sort
import com.cafarovceyxun.anamuslim.resources.dr_icon_translations
import com.cafarovceyxun.anamuslim.resources.duaConfirmSelection
import com.cafarovceyxun.anamuslim.resources.duaNoSubtitle
import com.cafarovceyxun.anamuslim.resources.duaManualHint
import com.cafarovceyxun.anamuslim.resources.duaTranslationOptional
import com.cafarovceyxun.anamuslim.resources.duaPickerArabicLabel
import com.cafarovceyxun.anamuslim.resources.duaPickerChooseName
import com.cafarovceyxun.anamuslim.resources.duaPickerChooseTitle
import com.cafarovceyxun.anamuslim.resources.duaPickerClear
import com.cafarovceyxun.anamuslim.resources.duaPickerCountLabel
import com.cafarovceyxun.anamuslim.resources.duaPickerHint
import com.cafarovceyxun.anamuslim.resources.duaPickerMissingName
import com.cafarovceyxun.anamuslim.resources.duaPickerMissingSelection
import com.cafarovceyxun.anamuslim.resources.duaPickerMissingTarget
import com.cafarovceyxun.anamuslim.resources.duaPickerNewTitle
import com.cafarovceyxun.anamuslim.resources.duaPickerNewTitleName
import com.cafarovceyxun.anamuslim.resources.duaPickerNewTitleNameAr
import com.cafarovceyxun.anamuslim.resources.duaPickerNoteHint
import com.cafarovceyxun.anamuslim.resources.duaPickerNoteLabel
import com.cafarovceyxun.anamuslim.resources.duaPickerNothingSelected
import com.cafarovceyxun.anamuslim.resources.duaPickerResultSection
import com.cafarovceyxun.anamuslim.resources.duaPickerSave
import com.cafarovceyxun.anamuslim.resources.duaPickerSearchName
import com.cafarovceyxun.anamuslim.resources.duaPickerSelectAll
import com.cafarovceyxun.anamuslim.resources.duaPickerSelectBraces
import com.cafarovceyxun.anamuslim.resources.duaPickerSelected
import com.cafarovceyxun.anamuslim.resources.duaPickerSourceSection
import com.cafarovceyxun.anamuslim.resources.duaPickerTargetSection
import com.cafarovceyxun.anamuslim.resources.duaPickerTitleAsma
import com.cafarovceyxun.anamuslim.resources.duaPickerTitleDua
import com.cafarovceyxun.anamuslim.resources.duaPickerUseSelection
import com.cafarovceyxun.anamuslim.resources.duaPickerTranslationLabel
import com.cafarovceyxun.anamuslim.resources.duaSelectionConfirmed
import com.cafarovceyxun.anamuslim.resources.duaSubtitleOptional
import com.cafarovceyxun.anamuslim.resources.duaTransliterationOptional
import com.cafarovceyxun.anamuslim.utils.supabase.AsmaEvidence
import com.cafarovceyxun.anamuslim.utils.supabase.Dua
import com.cafarovceyxun.anamuslim.viewModels.AsmaViewModel
import com.cafarovceyxun.anamuslim.viewModels.DuaViewModel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Seçimin götürüldüyü mənbə — hədisin/ayənin **tam** mətnləri və onu geri tapmağa yarayan açarlar.
 *
 * Mətnlər çağıran tərəfdə hazır olur (hədis oxucusunda hədis onsuz da əlindədir, oxucuda ayə
 * `DailyContentFactory` ilə qurulur), ona görə seçim ekranı heç nə yükləmir və dərhal açılır.
 */
data class ExcerptSourceData(
    val sourceType: String,
    val hadithId: Long? = null,
    val chapterNo: Int? = null,
    val verseNo: Int? = null,
    val verseEnd: Int? = null,
    val fullArabic: String,
    val fullTranslation: String,
    /**
     * Mənbənin **qeydi** — hədisdə `note` sahəsi.
     *
     * Seçilə bilən ayrıca blok kimi verilir, çünki bu toplusunda duanın **mənası** məhz oradadır
     * («Hədisdəki duanın tərcüməsi belədir: …»), rəvayətin içindəki `{…}` isə oxunuşdur.
     */
    val fullNote: String? = null,
    /** «Buxari №12» kimi göstərilən istinad — sətir kimi saxlanılır (`dua.source`). */
    val reference: String? = null,
)

/** Seçim ekranındakı hədəf siyahısının bir sətri. */
private data class TargetOption(
    val id: String,
    val title: String,
    val arabic: String? = null,
    val subtitle: String? = null,
    val badge: String? = null,
)

/**
 * Bir hədəf sətri — «Başlıq», «Alt başlıq», «Ad».
 *
 * Siyahı dua axınında iki, Əsmaül Hüsnə axınında bir sətirdən ibarətdir; ekran onları eyni kodla
 * çəkir və hər biri üçün eyni seçim addımını açır.
 */
private data class TargetSpec(
    val key: String,
    val label: String,
    val searchPlaceholder: String,
    val options: List<TargetOption>,
    val allowNew: Boolean,
    /** `true` → boş qala bilər; seçim siyahısında «yoxdur» sətri göstərilir. */
    val optional: Boolean,
    val enabled: Boolean,
    val selectedId: String?,
    val newName: String,
    val newNameAr: String,
    val onSelect: (String?) -> Unit,
    val onNewName: (String) -> Unit,
    val onNewNameAr: (String) -> Unit,
)

/**
 * Seçilmiş parçanın yazıldığı yer — «Ərəbcə», «Oxunuşu», «Tərcüməsi».
 *
 * [assign] həm mənbə blokundakı təsdiq düyməsindən, həm də sahənin özündən çağırılır: parça
 * mənbədə ümumiyyətlə olmaya bilər (qısa zikrin oxunuşu, tək bir ilahi adın tərcüməsi), o halda
 * əl ilə yazılır və ya boş qalır.
 */
private data class ExcerptSlot(
    val label: String,
    val value: String,
    val arabic: Boolean,
    val icon: DrawableResource,
    val assign: (String) -> Unit,
)

// ---------------------------------------------------------------------- giriş nöqtələri

/**
 * «Duaya əlavə et» axını: mənbənin dua olan hissələrini işarələ və başlığa (istəyə bağlı alt
 * başlığa) bağla.
 */
@Composable
fun DuaExcerptPicker(
    data: ExcerptSourceData,
    onClose: () -> Unit,
) {
    val duaViewModel = viewModel { DuaViewModel() }
    val categories by duaViewModel.categories.collectAsStateWithLifecycle()
    val subcategories by duaViewModel.subcategories.collectAsStateWithLifecycle()
    val isLoading by duaViewModel.isLoading.collectAsStateWithLifecycle()

    var categoryId by remember { mutableStateOf<String?>(null) }
    var newCategory by remember { mutableStateOf("") }
    var newCategoryAr by remember { mutableStateOf("") }
    var subcategoryId by remember { mutableStateOf<String?>(null) }
    var newSubcategory by remember { mutableStateOf("") }

    val categoryOptions = remember(categories) {
        categories.map { TargetOption(id = it.slug, title = it.name, arabic = it.name_ar) }
    }

    // Alt başlıqlar yalnız seçilmiş başlığa aiddir; başlıq seçilməyibsə siyahı boşdur.
    val subcategoryOptions = remember(subcategories, categoryId) {
        subcategories
            .filter { it.category_slug == categoryId }
            .map { TargetOption(id = it.slug, title = it.name, arabic = it.name_ar) }
    }

    val missingTarget = stringResource(Res.string.duaPickerMissingTarget)
    val hasCategory = categoryId != null || newCategory.isNotBlank()

    ExcerptPickerScaffold(
        title = stringResource(Res.string.duaPickerTitleDua),
        data = data,
        showCount = true,
        isSaving = isLoading,
        missingTargetMessage = missingTarget,
        hasTarget = hasCategory,
        targets = listOf(
            TargetSpec(
                key = KEY_CATEGORY,
                label = stringResource(Res.string.duaPickerChooseTitle),
                searchPlaceholder = stringResource(Res.string.duaPickerChooseTitle),
                options = categoryOptions,
                allowNew = true,
                optional = false,
                enabled = true,
                selectedId = categoryId,
                newName = newCategory,
                newNameAr = newCategoryAr,
                onSelect = { id ->
                    categoryId = id
                    if (id != null) {
                        newCategory = ""
                        newCategoryAr = ""
                    }
                    // Başlıq dəyişəndə alt başlıq mütləq sıfırlanır: köhnə alt başlıq yeni başlığa
                    // aid deyil və bazadakı xarici açar onu onsuz da qəbul etməzdi.
                    subcategoryId = null
                    newSubcategory = ""
                },
                onNewName = { newCategory = it },
                onNewNameAr = { newCategoryAr = it },
            ),
            TargetSpec(
                key = KEY_SUBCATEGORY,
                label = stringResource(Res.string.duaSubtitleOptional),
                searchPlaceholder = stringResource(Res.string.duaSubtitleOptional),
                options = subcategoryOptions,
                allowNew = true,
                optional = true,
                // Başlıq seçilməyibsə alt başlıq mənasızdır (nəyin altında?).
                enabled = hasCategory,
                selectedId = subcategoryId,
                newName = newSubcategory,
                newNameAr = "",
                onSelect = { id ->
                    subcategoryId = id
                    if (id != null) newSubcategory = ""
                },
                onNewName = { newSubcategory = it },
                onNewNameAr = {},
            ),
        ),
        onClose = onClose,
        onSave = { arabic, translit, translation, count ->
            duaViewModel.saveDua(
                categorySlug = categoryId,
                newCategoryName = newCategory.takeIf { it.isNotBlank() },
                newCategoryNameAr = newCategoryAr.takeIf { it.isNotBlank() },
                subcategorySlug = subcategoryId,
                newSubcategoryName = newSubcategory.takeIf { it.isNotBlank() },
                dua = Dua(
                    category_slug = categoryId.orEmpty(),
                    source_type = data.sourceType,
                    hadith_id = data.hadithId,
                    chapter_no = data.chapterNo,
                    verse_no = data.verseNo,
                    verse_end = data.verseEnd,
                    text_ar = arabic,
                    text_az = translation,
                    transliteration = translit,
                    repeat_count = count,
                    source = data.reference,
                ),
                onSaved = onClose,
            )
        },
    )
}

/** «Əsmaya dəlil» axını: eyni seçim, hədəf isə 99 addan biri. */
@Composable
fun AsmaExcerptPicker(
    data: ExcerptSourceData,
    onClose: () -> Unit,
) {
    val asmaViewModel = viewModel { AsmaViewModel() }
    val names by asmaViewModel.names.collectAsStateWithLifecycle()
    val isLoading by asmaViewModel.isLoading.collectAsStateWithLifecycle()

    var nameId by remember { mutableStateOf<String?>(null) }

    val options = remember(names) {
        names.map { name ->
            TargetOption(
                id = name.no.toString(),
                title = name.transliteration,
                arabic = name.name_ar,
                subtitle = name.meaning,
                badge = name.no.toString(),
            )
        }
    }

    val missingTarget = stringResource(Res.string.duaPickerMissingName)

    ExcerptPickerScaffold(
        title = stringResource(Res.string.duaPickerTitleAsma),
        data = data,
        showCount = false,
        isSaving = isLoading,
        missingTargetMessage = missingTarget,
        hasTarget = nameId != null,
        targets = listOf(
            TargetSpec(
                key = KEY_NAME,
                label = stringResource(Res.string.duaPickerChooseName),
                searchPlaceholder = stringResource(Res.string.duaPickerSearchName),
                options = options,
                allowNew = false,
                optional = false,
                enabled = true,
                selectedId = nameId,
                newName = "",
                newNameAr = "",
                onSelect = { nameId = it },
                onNewName = {},
                onNewNameAr = {},
            ),
        ),
        onClose = onClose,
        onSave = { arabic, translit, translation, _ ->
            val nameNo = nameId?.toIntOrNull() ?: return@ExcerptPickerScaffold

            asmaViewModel.saveEvidence(
                AsmaEvidence(
                    name_no = nameNo,
                    source_type = data.sourceType,
                    hadith_id = data.hadithId,
                    chapter_no = data.chapterNo,
                    verse_no = data.verseNo,
                    verse_end = data.verseEnd,
                    text_ar = arabic,
                    text_az = translation,
                    transliteration = translit,
                    source = data.reference,
                ),
                onSaved = onClose,
            )
        },
    )
}

private const val KEY_CATEGORY = "category"
private const val KEY_SUBCATEGORY = "subcategory"
private const val KEY_NAME = "name"

// ------------------------------------------------------------------------------ ekran

/**
 * Seçim ekranı — hər iki axın eyni kadrdan keçir.
 *
 * Quruluş iki hissəlidir: yuxarıda **mənbə blokları** (ərəbcə, rəvayət, qeyd), aşağıda
 * **yadda saxlanacaq parçalar** (ərəbcə, oxunuşu, tərcüməsi).
 *
 * Bir blokdan seçim bir neçə hədəfə gedə bilir, ona görə təsdiq düymələri hədəfin adını daşıyır:
 * bu toplusunda duanın **mənası** hədisin qeydindədir, **oxunuşu** isə rəvayətin içində `{…}`
 * arasındadır — yəni «hansı mətn hansı sahəyə düşür» sualının sabit cavabı yoxdur, seçən adam
 * deməlidir. Ərəbcə blok istisnadır: ondan yalnız ərəbcə hədəf doldurulur, ona görə orada tək
 * «OK» düyməsi qalır.
 *
 * ⚠️ Tam ekran **`Dialog`**-dur (CLAUDE.md qaydası): ekran modal vərəqdən açılır, inline emit
 * ediləndə həmin vərəqin pəncərəsinin altında qalıb səssizcə heç nə etmiş kimi görünərdi.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ExcerptPickerScaffold(
    title: String,
    data: ExcerptSourceData,
    showCount: Boolean,
    isSaving: Boolean,
    missingTargetMessage: String,
    hasTarget: Boolean,
    targets: List<TargetSpec>,
    onClose: () -> Unit,
    onSave: (arabic: String, transliteration: String?, translation: String, count: Int?) -> Unit,
) = Dialog(
    onDismissRequest = onClose,
    properties = DialogProperties(
        dismissOnBackPress = true,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false,
    ),
) {
    // **Təsdiqlənmiş** parçalar — yadda saxlanan elə budur, blokdakı canlı seçim yox.
    //
    // Seçim bir neçə hədəfə gedə bilir (rəvayətdən həm oxunuş, həm tərcümə çıxır), ona görə «seçdim»
    // ilə «bunu bura yaz» ayrı addımlardır.
    var confirmedArabic by remember(data) { mutableStateOf("") }
    var confirmedTranslit by remember(data) { mutableStateOf("") }
    var confirmedTranslation by remember(data) { mutableStateOf("") }
    var countText by remember(data) { mutableStateOf("") }

    var choosingKey by remember { mutableStateOf<String?>(null) }

    val missingSelection = stringResource(Res.string.duaPickerMissingSelection)

    val arabicSlot = ExcerptSlot(
        label = stringResource(Res.string.duaPickerArabicLabel),
        value = confirmedArabic,
        arabic = true,
        icon = Res.drawable.dr_icon_quran_script,
        assign = { confirmedArabic = it },
    )
    val translitSlot = ExcerptSlot(
        label = stringResource(Res.string.duaTransliterationOptional),
        value = confirmedTranslit,
        arabic = false,
        icon = Res.drawable.dr_icon_translations,
        assign = { confirmedTranslit = it },
    )
    val translationSlot = ExcerptSlot(
        label = stringResource(Res.string.duaTranslationOptional),
        value = confirmedTranslation,
        arabic = false,
        icon = Res.drawable.dr_icon_footnote,
        assign = { confirmedTranslation = it },
    )

    val latinSlots = listOf(translitSlot, translationSlot)

    BackHandler(enabled = choosingKey != null) { choosingKey = null }

    val chooser = targets.firstOrNull { it.key == choosingKey }
    if (chooser != null) {
        TargetChooser(spec = chooser, onDone = { choosingKey = null })
        return@Dialog
    }

    Scaffold(
        topBar = { AppBar(title = title, onBack = onClose) },
        bottomBar = {
            Surface(color = colorScheme.surfaceContainer, shadowElevation = 6.dp) {
                ReadableWidthColumn {
                    Button(
                        onClick = {
                            when {
                                // Yalnız ərəbcə məcburidir: tək bir ilahi adın tərcüməsi və ya
                                // qısa zikrin oxunuşu mənbədə olmaya bilər, uydurmaq isə səhvdir.
                                confirmedArabic.isBlank() ->
                                    PlatformUtils.showToast(missingSelection)

                                !hasTarget -> PlatformUtils.showToast(missingTargetMessage)

                                else -> onSave(
                                    confirmedArabic,
                                    confirmedTranslit.takeIf { it.isNotBlank() },
                                    confirmedTranslation,
                                    countText.toIntOrNull()?.takeIf { it > 0 },
                                )
                            }
                        },
                        enabled = !isSaving,
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
            // ⚠️ `ReadableWidthColumn` **Box**-dur: birbaşa uşaqları üst-üstə düşür, ona görə
            // daxili `Column` məcburidir (`HomeScreen` ilə eyni səbəb).
            ReadableWidthColumn {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Spacer(Modifier.height(4.dp))

                    data.reference?.takeIf { it.isNotBlank() }?.let { reference ->
                        Text(
                            text = reference,
                            style = typography.labelMedium.withScriptDirection(arabic = false),
                            color = colorScheme.onSurfaceVariant.alpha(0.8f),
                        )
                    }

                    SectionLabel(
                        step = 1,
                        text = stringResource(Res.string.duaPickerSourceSection),
                        done = confirmedArabic.isNotBlank(),
                    )

                    Text(
                        text = stringResource(Res.string.duaPickerHint),
                        style = typography.bodySmall.withScriptDirection(arabic = false),
                        color = colorScheme.onSurfaceVariant.alpha(0.75f),
                    )

                    SourceBlock(
                        label = stringResource(Res.string.duaPickerArabicLabel),
                        text = data.fullArabic,
                        arabic = true,
                        slots = listOf(arabicSlot),
                    )

                    // Tərcümə blokunun mənbədə qarşılığı olmaya bilər (oxucuda tərcümə seçilməyib,
                    // ya da ayənin tərcüməsi yüklənməyib) — boş qutu seçiləsi heç nə təklif etmir,
                    // ona görə ümumiyyətlə göstərilmir. Mətn 2-ci addımda əl ilə yazıla bilər.
                    if (data.fullTranslation.isNotBlank()) {
                        SourceBlock(
                            label = stringResource(Res.string.duaPickerTranslationLabel),
                            text = data.fullTranslation,
                            arabic = false,
                            slots = latinSlots,
                        )
                    }

                    // Qeyd yalnız mənbədə varsa görünür — boş blok ekranı uzadardı.
                    if (!data.fullNote.isNullOrBlank()) {
                        SourceBlock(
                            label = stringResource(Res.string.duaPickerNoteLabel),
                            text = data.fullNote,
                            arabic = false,
                            slots = latinSlots,
                            hint = stringResource(Res.string.duaPickerNoteHint),
                        )
                    }

                    HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.4f))

                    SectionLabel(
                        step = 2,
                        text = stringResource(Res.string.duaPickerResultSection),
                        done = confirmedArabic.isNotBlank(),
                    )

                    Text(
                        text = stringResource(Res.string.duaManualHint),
                        style = typography.bodySmall.withScriptDirection(arabic = false),
                        color = colorScheme.onSurfaceVariant.alpha(0.75f),
                    )

                    ResultField(arabicSlot)
                    ResultField(translitSlot)
                    ResultField(translationSlot)

                    if (showCount) {
                        FormTextField(
                            value = countText,
                            onValueChange = { input ->
                                countText = input.filter { it.isDigit() }.take(6)
                            },
                            label = stringResource(Res.string.duaPickerCountLabel),
                            icon = Res.drawable.dr_icon_sort,
                            keyboardType = KeyboardType.Number,
                            onClear = { countText = "" },
                        )
                    }

                    HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.4f))

                    SectionLabel(
                        step = 3,
                        text = stringResource(Res.string.duaPickerTargetSection),
                        done = hasTarget,
                    )

                    targets.forEach { spec ->
                        TargetRow(
                            spec = spec,
                            onClick = { if (spec.enabled) choosingKey = spec.key },
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * Bölmə başlığı — nömrələnmiş addım və «hazırdır» nişanı.
 *
 * Nömrə qəsdəndir: ekranda üç ayrı iş var (seç → yoxla → hara yazılsın) və nömrəsiz başlıqlarda
 * onlar bir-birinə qarışmış uzun forma kimi görünürdü. Nişan isə yadda saxlamadan **əvvəl** nəyin
 * əskik olduğunu deyir — əvvəl bunu yalnız düyməyə basanda çıxan toast deyirdi.
 */
@Composable
private fun SectionLabel(step: Int, text: String, done: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(
                    color = if (done) colorScheme.primary
                    else colorScheme.primaryContainer.alpha(0.5f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_check),
                    contentDescription = null,
                    tint = colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp),
                )
            } else {
                Text(
                    text = step.toString(),
                    style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.primary,
                )
            }
        }

        Text(
            text = text,
            style = typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                .withScriptDirection(arabic = false),
            color = colorScheme.primary,
        )
    }
}

/**
 * Bir mənbə mətni və ondan seçim — **sistemin öz seçimi ilə**: basıb saxla, tutacaqları çək,
 * üstündə Kopyala/Paylaş paneli çıxsın; telefonda hər mətn belə seçilir, bu ekran da istisna olmasın.
 *
 * ### Niyə `readOnly` `BasicTextField`
 * Seçimi çağırana verən yeganə şey elə odur: `SelectionContainer` seçdiyi parçanı kənara vermir,
 * `Text` isə ümumiyyətlə seçilmir. `BasicTextField` seçimi `TextFieldValue.selection` ilə qaytarır,
 * `readOnly` isə klaviaturanı açmır və mətni redaktəyə buraxmır.
 *
 * ### Niyə **çərçivəsiz** və tam açıq
 * Əvvəlki variant `OutlinedTextField` idi — etiketli, çərçivəli və **içində sürüşən** qutu
 * (`minLines = 4, maxLines = 10`). Əziyyət elə oradan gəlirdi: tutacağı çəkəndə gah qutunun öz içi
 * sürüşürdü, gah səhifə, gah da tutacaq əldən çıxırdı. Burada mətn tam hündürlüyü ilə çəkilir (öz
 * sürüşməsi yoxdur) və qutu bəzəyi yoxdur — səhifədəki adi mətn kimi görünür, seçim isə sistemindir.
 *
 * ### Fokus gedəndə seçim itir — ona görə **son** seçim saxlanılır
 * Hədəf düyməsinə basanda sahə fokusu itirir və `selection` boşalır; `picked` son boş olmayan
 * aralığı saxlayır, yəni düymə həmişə istifadəçinin gördüyü parçanı yazır. Fokussuz halda sistem
 * vurğunu çəkmədiyi üçün həmin aralığı [selectionHighlight] özü boyayır — «seçdim, amma ekranda heç
 * nə görünmür» halı qalmasın.
 */
@Composable
private fun SourceBlock(
    label: String,
    text: String,
    arabic: Boolean,
    slots: List<ExcerptSlot>,
    hint: String? = null,
) {
    // Mətn dəyişmir (`readOnly`), dəyişən yalnız seçimdir — ona görə hər dəyişiklikdə mətn
    // parametrdən geri qoyulur.
    var field by remember(text) { mutableStateOf(TextFieldValue(text)) }
    var picked by remember(text) { mutableStateOf<TextRange?>(null) }
    var focused by remember(text) { mutableStateOf(false) }

    val braces = remember(text) { bracesRange(text) }

    // Seçim panelinin bəndi kompozisiyadan kənarda çağırıldığı üçün etiket əvvəlcədən oxunur.
    val useLabel = stringResource(Res.string.duaPickerUseSelection)

    val live = remember(text, picked) {
        picked?.let { text.substring(it.min, it.max).trim() }.orEmpty()
    }

    fun select(range: TextRange?) {
        picked = range
        field = field.copy(selection = range ?: TextRange.Zero)
    }

    val highlight = colorScheme.primary.alpha(0.28f)
    val textStyle = if (arabic) {
        typography.bodyLarge.withScriptDirection(arabic = true, arabicFontFamily = arabicFontFamily())
    } else {
        typography.bodyLarge.withScriptDirection(arabic = false)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ⚠️ Yer darlaşanda **etiket** qısalır, düymələr yox: `SpaceBetween` ilə üç düymə
            // sıxılıb «Təmizlə» iki sətrə qırılırdı. Çəki etiketdədir, düymələr öz enini saxlayır.
            Text(
                text = label,
                style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    .withScriptDirection(arabic = false),
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 4.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                // «Mötərizədə» yalnız mətndə belə bir parça varsa: bu topluda duanın **oxunuşu**
                // məhz mötərizələrin arasındadır, yəni ən çox istənən aralıq bir toxunuşla seçilir.
                if (braces != null) {
                    TextButton(
                        onClick = { select(braces) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.duaPickerSelectBraces),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }

                TextButton(
                    onClick = { select(TextRange(0, text.length)) },
                    enabled = text.isNotEmpty(),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.duaPickerSelectAll),
                        maxLines = 1,
                        softWrap = false,
                    )
                }

                TextButton(
                    onClick = { select(null) },
                    enabled = picked != null,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.duaPickerClear),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }

        hint?.let {
            Text(
                text = it,
                style = typography.labelSmall.withScriptDirection(arabic = false),
                color = colorScheme.onSurfaceVariant.alpha(0.7f),
            )
        }

        Surface(
            color = colorScheme.surfaceContainerHigh.alpha(0.45f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(
                width = if (picked == null) 0.5.dp else 1.dp,
                color = if (picked == null) colorScheme.outlineVariant.alpha(0.5f)
                else colorScheme.primary.alpha(0.5f),
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            BasicTextField(
                value = field,
                onValueChange = { updated ->
                    field = updated.copy(text = text)
                    if (!updated.selection.collapsed) picked = updated.selection
                },
                readOnly = true,
                textStyle = textStyle.copy(color = colorScheme.onSurface),
                // Kursor `readOnly` sahədə heç nə bildirmir, amma mətnin ortasında yanıb-sönən xətt
                // onu «redaktə olunur» kimi göstərir.
                cursorBrush = SolidColor(Color.Transparent),
                visualTransformation = selectionHighlight(
                    range = picked.takeIf { !focused },
                    color = highlight,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    // Seçim paneli (Kopyala · Hamısını seç · Paylaş) barmağın yanında açılır —
                    // hədəf düymələri də ora qoyulur ki, seçəndən sonra blokun altına uzanmaq
                    // lazım gəlməsin. Aşağıdakı düymələr yerində qalır: panel barmaq qaldırılanda
                    // bağlanır, istifadəçi isə fikrini sonra da dəyişə bilər.
                    .appendTextContextMenuComponents {
                        slots.forEach { slot ->
                            excerptMenuItem(
                                key = slot.label,
                                label = if (slots.size == 1) useLabel else slot.shortLabel(),
                            ) {
                                // Panel açıq olduğu üçün seçim hələ canlıdır; `picked` onu artıq
                                // tutub, ona görə mətn buradan götürülür.
                                val selected = picked
                                    ?.let { text.substring(it.min, it.max).trim() }
                                    .orEmpty()

                                if (selected.isNotBlank()) slot.assign(selected)
                                close()
                            }
                        }
                    }
                    .onFocusChanged { focused = it.isFocused }
                    .padding(14.dp),
            )
        }

        Surface(
            color = if (live.isBlank()) colorScheme.surfaceContainerHigh.alpha(0.3f)
            else colorScheme.primaryContainer.alpha(0.35f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (live.isBlank()) {
                        stringResource(Res.string.duaPickerNothingSelected)
                    } else {
                        stringResource(Res.string.duaPickerSelected, live.length)
                    },
                    style = typography.labelSmall.withScriptDirection(arabic = false),
                    color = colorScheme.onSurfaceVariant.alpha(0.85f),
                    modifier = Modifier.weight(1f),
                )

                // Düymələr yalnız seçim olanda: boş seçimlə basılan düymə sahəni səssizcə
                // təmizləyərdi.
                if (live.isNotBlank()) {
                    slots.forEach { slot ->
                        FilledTonalButton(
                            onClick = { slot.assign(live) },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = if (slots.size == 1) {
                                    stringResource(Res.string.duaConfirmSelection)
                                } else {
                                    slot.shortLabel()
                                },
                                style = typography.labelLarge,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sahə fokusda olmayanda seçilmiş aralığı boyayan çevirmə; [range] null olanda heç nə etmir.
 *
 * Ofset xəritəsi **eynilik** olmalıdır — çevirmə yalnız rəng verir, bir simvol belə əlavə etmir;
 * əks halda tutacaqların mövqeyi mətnlə üst-üstə düşməzdi.
 */
private fun selectionHighlight(range: TextRange?, color: Color): VisualTransformation =
    if (range == null || range.collapsed) {
        VisualTransformation.None
    } else {
        VisualTransformation { original ->
            TransformedText(
                text = buildAnnotatedString {
                    append(original)
                    addStyle(
                        style = SpanStyle(background = color),
                        start = range.min.coerceIn(0, original.length),
                        end = range.max.coerceIn(0, original.length),
                    )
                },
                offsetMapping = OffsetMapping.Identity,
            )
        }
    }

/**
 * Mətndəki ilk `{…}` parçası — tapılmasa null.
 *
 * Mötərizələrin **özü** də aralığa düşür: onları kənarda saxlamaq sözü ortasından bölərdi, halbuki
 * oxunuş sətrində mötərizə onsuz da sözün bir hissəsi kimi yazılır.
 */
private fun bracesRange(text: String): TextRange? {
    val open = text.indexOf('{')
    if (open < 0) return null

    val close = text.indexOf('}', startIndex = open + 1)
    if (close < 0) return null

    return TextRange(open, close + 1)
}

/**
 * «Yadda saxlanacaq» bölməsinin bir sahəsi — **redaktə oluna bilir**.
 *
 * Mənbə blokundakı təsdiq düyməsi buraya yazır, amma sahə kilidli deyil: parça mənbədə ümumiyyətlə
 * olmaya bilər, o halda istifadəçi əl ilə yazır (və ya boş buraxır — yalnız ərəbcə məcburidir).
 * Dolu sahə yaşıl işarə ilə göstərilir ki, nəyin hazır olduğu bir baxışda görünsün.
 */
@Composable
private fun ResultField(slot: ExcerptSlot) {
    FormTextField(
        value = slot.value,
        onValueChange = slot.assign,
        label = slot.label,
        icon = slot.icon,
        minLines = 2,
        maxLines = 8,
        textStyle = if (slot.arabic) {
            typography.bodyLarge.withScriptDirection(
                arabic = true,
                arabicFontFamily = arabicFontFamily(),
            )
        } else {
            typography.bodyLarge.withScriptDirection(arabic = false)
        },
        topEndAction = if (slot.value.isNotBlank()) {
            {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_check_circle),
                    contentDescription = stringResource(Res.string.duaSelectionConfirmed),
                    tint = colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            null
        },
        onClear = { slot.assign("") },
    )
}

/** «Başlıq / Alt başlıq / Ad» sətri — basılanda hədəf seçimi addımını açır. */
@Composable
private fun TargetRow(spec: TargetSpec, onClick: () -> Unit) {
    val chosen = spec.options.firstOrNull { it.id == spec.selectedId }
    val value = chosen?.title
        ?: spec.newName.takeIf { it.isNotBlank() }
        ?: stringResource(
            if (spec.optional) Res.string.duaNoSubtitle else Res.string.duaPickerNothingSelected,
        )

    Surface(
        onClick = onClick,
        enabled = spec.enabled,
        color = colorScheme.surfaceContainerHigh.alpha(if (spec.enabled) 0.6f else 0.3f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = spec.label,
                    style = typography.labelSmall.withScriptDirection(arabic = false),
                    color = colorScheme.onSurfaceVariant.alpha(0.75f),
                )
                Text(
                    text = value,
                    style = typography.bodyLarge.withScriptDirection(arabic = false),
                    color = if (chosen != null || spec.newName.isNotBlank()) colorScheme.onSurface
                    else colorScheme.onSurfaceVariant.alpha(0.7f),
                )
                chosen?.arabic?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = typography.bodyMedium.withScriptDirection(
                            arabic = true,
                            arabicFontFamily = arabicFontFamily(),
                        ),
                        color = colorScheme.onSurfaceVariant.alpha(0.8f),
                    )
                }
            }

            Icon(
                painter = painterResource(Res.drawable.dr_icon_chevron_right),
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant.alpha(0.6f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Hədəf seçimi addımı — axtarışlı siyahı, lazım olanda yuxarıda «yeni» forması.
 *
 * Siyahı `LazyColumn`-dur və **öz** sürüşməsi var: 99 ad seçim ekranının sürüşən sütununa
 * yerləşdirilsəydi forma sahələri ekrandan qovulardı.
 */
@Composable
private fun TargetChooser(spec: TargetSpec, onDone: () -> Unit) {
    var query by remember(spec.key) { mutableStateOf("") }

    val filtered = remember(spec.options, query) {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) spec.options
        else spec.options.filter { option ->
            option.title.lowercase().contains(needle) ||
                option.subtitle?.lowercase()?.contains(needle) == true ||
                option.arabic?.contains(query.trim()) == true
        }
    }

    Scaffold(
        topBar = { AppBar(title = spec.label, onBack = onDone) },
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                SearchTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = spec.searchPlaceholder,
                )
            }

            if (spec.allowNew) {
                NewTargetForm(spec = spec, onDone = onDone)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = readableWidthInset()),
            ) {
                // «Yoxdur» yalnız istəyə bağlı hədəfdə: alt başlıq seçilməyəndə dua birbaşa
                // başlığın altına düşür, bu da qanuni bir seçimdir.
                if (spec.optional) {
                    item(key = "none") {
                        TargetOptionRow(
                            option = TargetOption(
                                id = "",
                                title = stringResource(Res.string.duaNoSubtitle),
                            ),
                            selected = spec.selectedId == null && spec.newName.isBlank(),
                            onClick = {
                                spec.onSelect(null)
                                spec.onNewName("")
                                onDone()
                            },
                        )
                    }
                }

                items(filtered, key = { it.id }) { option ->
                    TargetOptionRow(
                        option = option,
                        selected = option.id == spec.selectedId,
                        onClick = {
                            spec.onSelect(option.id)
                            onDone()
                        },
                    )
                }
            }
        }
    }
}

/** Yeni başlıq/alt başlıq forması — yalnız `allowNew` hədəflərdə. */
@Composable
private fun NewTargetForm(spec: TargetSpec, onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.duaPickerNewTitle),
            style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                .withScriptDirection(arabic = false),
            color = colorScheme.onSurface,
        )

        FormTextField(
            value = spec.newName,
            onValueChange = spec.onNewName,
            label = stringResource(Res.string.duaPickerNewTitleName),
            icon = Res.drawable.dr_icon_translations,
            onClear = { spec.onNewName("") },
        )

        // Ərəbcə ad yalnız başlıqda soruşulur; alt başlıqda sahə lazımsız yer tutardı.
        if (spec.key == KEY_CATEGORY) {
            FormTextField(
                value = spec.newNameAr,
                onValueChange = spec.onNewNameAr,
                label = stringResource(Res.string.duaPickerNewTitleNameAr),
                icon = Res.drawable.dr_icon_quran_script,
                textStyle = typography.bodyLarge.withScriptDirection(
                    arabic = true,
                    arabicFontFamily = arabicFontFamily(),
                ),
                onClear = { spec.onNewNameAr("") },
            )
        }

        Button(
            onClick = {
                // Siyahıdan seçim ləğv olunur: ikisi eyni anda dolu olsa hansının yazılacağı
                // istifadəçiyə görünməzdi.
                spec.onSelect(null)
                onDone()
            },
            enabled = spec.newName.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.duaPickerNewTitle))
        }

        Spacer(Modifier.height(4.dp))
    }
}

/** Siyahının bir sətri — nişan, ad, ərəbcə qarşılığı və mənası. */
@Composable
private fun TargetOptionRow(
    option: TargetOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        option.badge?.let { badge ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = colorScheme.primaryContainer.alpha(0.5f),
                        shape = RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = badge,
                    style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.size(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.title,
                style = typography.bodyLarge.withScriptDirection(arabic = false),
                color = colorScheme.onSurface,
            )
            option.subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = typography.bodySmall.withScriptDirection(arabic = false),
                    color = colorScheme.onSurfaceVariant.alpha(0.75f),
                )
            }
        }

        option.arabic?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = typography.titleMedium.withScriptDirection(
                    arabic = true,
                    arabicFontFamily = arabicFontFamily(),
                ),
                color = colorScheme.onSurface.alpha(0.9f),
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        if (selected) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_check),
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp).size(20.dp),
            )
        }
    }
}

/** Düymə etiketi — «Oxunuşu (istəyə bağlı)» kimi uzun adın mötərizəsiz hissəsi. */
private fun ExcerptSlot.shortLabel(): String = label.substringBefore(" (").trim()

/** Adın nömrəsi ilə etiketi — siyahıda və detal ekranında eyni formatı işlədirik. */
@Composable
internal fun asmaNumberLabel(no: Int): String = stringResource(Res.string.asmaNameNo, no)
