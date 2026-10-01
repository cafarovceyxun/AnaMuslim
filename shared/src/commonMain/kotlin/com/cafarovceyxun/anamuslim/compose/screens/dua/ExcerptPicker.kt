package com.cafarovceyxun.anamuslim.compose.screens.dua

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.AppBarDefaults
import com.cafarovceyxun.anamuslim.compose.components.common.ModeTab
import com.cafarovceyxun.anamuslim.compose.components.common.ModeTabIcon
import com.cafarovceyxun.anamuslim.compose.components.common.ModeTabStrip
import com.cafarovceyxun.anamuslim.compose.components.common.ReadableWidthColumn
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderTextZoom
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderZoomFeedback
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderZoomFeedbackOverlay
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderZoomTarget
import com.cafarovceyxun.anamuslim.compose.components.reader.readerTextZoom
import com.cafarovceyxun.anamuslim.compose.screens.hadith.FormTextField
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.arabicFontFamily
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.DuaPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.asmaNameNo
import com.cafarovceyxun.anamuslim.resources.dr_icon_add
import com.cafarovceyxun.anamuslim.resources.dr_icon_check
import com.cafarovceyxun.anamuslim.resources.dr_icon_check_circle
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_down
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_footnote
import com.cafarovceyxun.anamuslim.resources.dr_icon_quran_script
import com.cafarovceyxun.anamuslim.resources.dr_icon_translations
import com.cafarovceyxun.anamuslim.resources.duaMsgSaved
import com.cafarovceyxun.anamuslim.resources.duaMsgSavedContinue
import com.cafarovceyxun.anamuslim.resources.duaNoSubtitle
import com.cafarovceyxun.anamuslim.resources.duaPanelArabicPlaceholder
import com.cafarovceyxun.anamuslim.resources.duaPanelChooseName
import com.cafarovceyxun.anamuslim.resources.duaPanelChooseTitle
import com.cafarovceyxun.anamuslim.resources.duaPanelCollapse
import com.cafarovceyxun.anamuslim.resources.duaPanelExpand
import com.cafarovceyxun.anamuslim.resources.duaPanelHide
import com.cafarovceyxun.anamuslim.resources.duaPanelNewSubtitle
import com.cafarovceyxun.anamuslim.resources.duaPanelNoSubtitle
import com.cafarovceyxun.anamuslim.resources.duaPanelOptionalPlaceholder
import com.cafarovceyxun.anamuslim.resources.duaPanelSelectHint
import com.cafarovceyxun.anamuslim.resources.duaPanelSegments
import com.cafarovceyxun.anamuslim.resources.duaPanelSelectInText
import com.cafarovceyxun.anamuslim.resources.duaPanelSelectionForPart
import com.cafarovceyxun.anamuslim.resources.duaPanelShow
import com.cafarovceyxun.anamuslim.resources.duaPanelTarget
import com.cafarovceyxun.anamuslim.resources.duaPickerAddPart
import com.cafarovceyxun.anamuslim.resources.duaPickerAlreadyAdded
import com.cafarovceyxun.anamuslim.resources.duaPickerChooseName
import com.cafarovceyxun.anamuslim.resources.duaPickerChooseTitle
import com.cafarovceyxun.anamuslim.resources.duaPickerClear
import com.cafarovceyxun.anamuslim.resources.duaPickerMissingName
import com.cafarovceyxun.anamuslim.resources.duaPickerMissingSelection
import com.cafarovceyxun.anamuslim.resources.duaPickerMissingTarget
import com.cafarovceyxun.anamuslim.resources.duaPickerNewTitle
import com.cafarovceyxun.anamuslim.resources.duaPickerNewTitleName
import com.cafarovceyxun.anamuslim.resources.duaPickerNewTitleNameAr
import com.cafarovceyxun.anamuslim.resources.duaPickerNoteHint
import com.cafarovceyxun.anamuslim.resources.duaPickerNoteLabel
import com.cafarovceyxun.anamuslim.resources.duaPickerPartDuplicate
import com.cafarovceyxun.anamuslim.resources.duaPickerPartMissingArabic
import com.cafarovceyxun.anamuslim.resources.duaPickerPartsHint
import com.cafarovceyxun.anamuslim.resources.duaPickerResultSection
import com.cafarovceyxun.anamuslim.resources.duaPickerSave
import com.cafarovceyxun.anamuslim.resources.duaPickerSaveContinue
import com.cafarovceyxun.anamuslim.resources.duaPickerSegmentsTitle
import com.cafarovceyxun.anamuslim.resources.duaPickerSelectAll
import com.cafarovceyxun.anamuslim.resources.duaPickerSelected
import com.cafarovceyxun.anamuslim.resources.duaPickerTitleAsma
import com.cafarovceyxun.anamuslim.resources.duaPickerTitleDua
import com.cafarovceyxun.anamuslim.resources.duaPickerUseSelection
import com.cafarovceyxun.anamuslim.resources.duaSubtitleOptional
import com.cafarovceyxun.anamuslim.resources.duaTransliterationLabel
import com.cafarovceyxun.anamuslim.resources.labelArabic
import com.cafarovceyxun.anamuslim.resources.labelTranslation
import com.cafarovceyxun.anamuslim.resources.strLabelDone
import com.cafarovceyxun.anamuslim.utils.dua.DhikrSegment
import com.cafarovceyxun.anamuslim.utils.dua.braceRanges
import com.cafarovceyxun.anamuslim.utils.dua.dhikrSegments
import com.cafarovceyxun.anamuslim.utils.supabase.AsmaEvidence
import com.cafarovceyxun.anamuslim.utils.supabase.DuaSourceRef
import com.cafarovceyxun.anamuslim.utils.supabase.DuaSourceType
import com.cafarovceyxun.anamuslim.utils.supabase.MAX_DUA_PARTS
import com.cafarovceyxun.anamuslim.utils.text.excerptMatchRange
import com.cafarovceyxun.anamuslim.viewModels.AsmaViewModel
import com.cafarovceyxun.anamuslim.viewModels.DuaViewModel
import kotlinx.coroutines.launch
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
) {
    /**
     * [row] bu mənbədən götürülübmü — hədisdə `hadith_id`, ayədə surə + ayə nömrəsi.
     *
     * Ayə aralığı (`verse_end`) müqayisə olunmur: eyni ayədən başlayan çıxarış eyni mənbədir, oxucu
     * isə aralığı seçimdən asılı olaraq fərqli verə bilər.
     */
    fun isSourceOf(row: DuaSourceRef): Boolean = when (sourceType) {
        DuaSourceType.HADITH ->
            row.source_type == DuaSourceType.HADITH && hadithId != null && row.hadith_id == hadithId

        DuaSourceType.QURAN ->
            row.source_type == DuaSourceType.QURAN && chapterNo != null &&
                row.chapter_no == chapterNo && row.verse_no == verseNo

        else -> false
    }
}

/** Seçim ekranındakı hədəf siyahısının bir sətri. */
private data class TargetOption(
    val id: String,
    val title: String,
    val arabic: String? = null,
    val subtitle: String? = null,
    val badge: String? = null,
)

/**
 * Bir hədəf — «Başlıq», «Alt başlıq», «Ad».
 *
 * Panelin «Bölmə» sətrində hər biri bir açılan düymədir: «Başlıq ▾ › Alt başlıq ▾». Dua axınında
 * iki, Əsmaül Hüsnə axınında bir hədəf var; ekran onları eyni kodla çəkir.
 */
private data class TargetSpec(
    val key: String,
    val label: String,
    /** Heç nə seçilməyəndə düymədəki mətn — «Başlıq seçin», «Alt başlıq yoxdur». */
    val placeholder: String,
    /** Menyudakı «+ Yeni …» bəndi və yeni ad ekranının düyməsi; `allowNew = false` olanda işlənmir. */
    val newLabel: String,
    val options: List<TargetOption>,
    val allowNew: Boolean,
    /** `true` → boş qala bilər; menyuda «yoxdur» bəndi göstərilir. */
    val optional: Boolean,
    val enabled: Boolean,
    val selectedId: String?,
    val newName: String,
    val newNameAr: String,
    val onSelect: (String?) -> Unit,
    val onNewName: (String) -> Unit,
    val onNewNameAr: (String) -> Unit,
) {
    /** Düymədəki ad: siyahıdan seçilən, ya da yazılmış yeni ad; heç biri yoxdursa `null`. */
    val chosenTitle: String?
        get() = options.firstOrNull { it.id == selectedId }?.title
            ?: newName.trim().takeIf { it.isNotEmpty() }
}

/**
 * Yadda saxlanacaq bir sahə — «Ərəbcə», «Oxunuşu», «Tərcümə».
 *
 * [assign] mənbədəki seçimdən (üzən zolaq, sistemin seçim menyusu), zikr çipindən və panel sətrinin
 * öz redaktəsindən çağırılır: parça mənbədə ümumiyyətlə olmaya bilər (qısa zikrin oxunuşu, tək bir
 * ilahi adın tərcüməsi), o halda əl ilə yazılır və ya boş qalır.
 */
private data class ExcerptSlot(
    val key: String,
    /** Qısa ad — panel sətri, üzən zolaq və seçim menyusu eyni sözü işlətsin. */
    val label: String,
    val placeholder: String,
    val value: String,
    val arabic: Boolean,
    val assign: (String) -> Unit,
)

/** Mənbənin bir mətni — mənbə zolağında bir tab. */
private enum class SourceTab { Arabic, Translation, Note }

// ---------------------------------------------------------------------- giriş nöqtələri

/**
 * «Duaya əlavə et» axını: mənbənin dua olan hissələrini işarələ və başlığa (istəyə bağlı alt
 * başlığa) bağla.
 *
 * Bir hədisdə bir neçə zikr ola bilər (Əhməd 803-də beşi, hər biri başqa mövzuda), ona görə
 * «Saxla və davam et» ekranı bağlamır: hissələr sıfırlanır, hədəf isə qalır. Bir zikrin özü də bir
 * neçə hissədən ibarət ola bilər (33 + 33 + 33 + 1) — «Hissə əlavə et».
 */
@Composable
fun DuaExcerptPicker(
    data: ExcerptSourceData,
    onClose: () -> Unit,
) {
    val duaViewModel = viewModel { DuaViewModel() }
    val categories by duaViewModel.categories.collectAsStateWithLifecycle()
    val subcategories by duaViewModel.subcategories.collectAsStateWithLifecycle()
    val duas by duaViewModel.duas.collectAsStateWithLifecycle()
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

    val segments = remember(data) {
        dhikrSegments(data.fullArabic, data.fullTranslation, data.fullNote)
    }

    // Bu mənbədən artıq yazılanlar: hissə başqa mənbədən olan duaya bağlana bilər (dua ekranındakı
    // «birləşdir»), ona görə qrup **hər hansı** sətri bu mənbədən olan duadır, siyahıda isə bütöv
    // dua (baş sətir + hissələri) görünür.
    val savedExcerpts = remember(duas, categories, subcategories, data) {
        val groupIds = duas.filter { data.isSourceOf(it) }.mapNotNull { it.partGroupId }.toSet()
        val partsByHead = duas.filterNot { it.isPartHead }.groupBy { it.part_of_id }

        duas
            .filter { it.isPartHead && it.id in groupIds }
            .map { head ->
                val category = categories.firstOrNull { it.slug == head.category_slug }?.name
                val subcategory = subcategories.firstOrNull { it.slug == head.subcategory_slug }?.name

                SavedExcerpt(
                    parts = listOf(head) +
                        partsByHead[head.id].orEmpty().sortedBy { it.part_no ?: 1 },
                    targetLabel = listOfNotNull(category ?: head.category_slug, subcategory)
                        .joinToString(" › "),
                )
            }
    }

    val missingTarget = stringResource(Res.string.duaPickerMissingTarget)
    val hasCategory = categoryId != null || newCategory.isNotBlank()

    fun save(parts: List<DuaPartDraft>, continuing: Boolean, onDone: () -> Unit) {
        duaViewModel.saveDua(
            categorySlug = categoryId,
            newCategoryName = newCategory.takeIf { it.isNotBlank() },
            newCategoryNameAr = newCategoryAr.takeIf { it.isNotBlank() },
            subcategorySlug = subcategoryId,
            newSubcategoryName = newSubcategory.takeIf { it.isNotBlank() },
            parts = parts.map { it.toDua(data) },
            savedMessage = if (continuing) Res.string.duaMsgSavedContinue else Res.string.duaMsgSaved,
            onSaved = { result ->
                // Hədəf **həll olunmuş** slug-lara keçir və «yeni ad» sahələri təmizlənir: yoxsa
                // «davam et»dən sonrakı yazı eyni adla ikinci başlıq (`x-2`) açardı.
                categoryId = result.categorySlug
                newCategory = ""
                newCategoryAr = ""
                subcategoryId = result.subcategorySlug
                newSubcategory = ""
                onDone()
            },
        )
    }

    ExcerptPickerScaffold(
        title = stringResource(Res.string.duaPickerTitleDua),
        data = data,
        allowParts = true,
        segments = segments,
        savedExcerpts = savedExcerpts,
        isSaving = isLoading,
        missingTargetMessage = missingTarget,
        hasTarget = hasCategory,
        targetRowLabel = stringResource(Res.string.duaPanelTarget),
        targets = listOf(
            TargetSpec(
                key = KEY_CATEGORY,
                label = stringResource(Res.string.duaPickerChooseTitle),
                placeholder = stringResource(Res.string.duaPanelChooseTitle),
                newLabel = stringResource(Res.string.duaPickerNewTitle),
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
                placeholder = stringResource(Res.string.duaPanelNoSubtitle),
                newLabel = stringResource(Res.string.duaPanelNewSubtitle),
                options = subcategoryOptions,
                allowNew = true,
                optional = true,
                // Başlıq seçilməyibsə alt başlıq mənasızdır (nəyin altında?) — düymə çəkilmir.
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
        onSave = { parts -> save(parts, continuing = false, onDone = onClose) },
        onSaveAndContinue = { parts, onDone -> save(parts, continuing = true, onDone = onDone) },
    )
}

/** «Əsmaya dəlil» axını: eyni seçim, hədəf isə 99 addan biri. Dəlil tək sətirdir, sayı yoxdur. */
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
        allowParts = false,
        segments = emptyList(),
        savedExcerpts = emptyList(),
        isSaving = isLoading,
        missingTargetMessage = missingTarget,
        hasTarget = nameId != null,
        targetRowLabel = stringResource(Res.string.duaPickerChooseName),
        targets = listOf(
            TargetSpec(
                key = KEY_NAME,
                label = stringResource(Res.string.duaPickerChooseName),
                placeholder = stringResource(Res.string.duaPanelChooseName),
                newLabel = "",
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
        onSave = { parts ->
            val nameNo = nameId?.toIntOrNull() ?: return@ExcerptPickerScaffold
            val row = parts.first().toDua(data)

            asmaViewModel.saveEvidence(
                AsmaEvidence(
                    name_no = nameNo,
                    source_type = row.source_type,
                    hadith_id = row.hadith_id,
                    chapter_no = row.chapter_no,
                    verse_no = row.verse_no,
                    verse_end = row.verse_end,
                    text_ar = row.text_ar,
                    text_az = row.text_az,
                    transliteration = row.transliteration,
                    source = row.source,
                ),
                onSaved = onClose,
            )
        },
        onSaveAndContinue = null,
    )
}

private const val KEY_CATEGORY = "category"
private const val KEY_SUBCATEGORY = "subcategory"
private const val KEY_NAME = "name"

private const val SLOT_ARABIC = "arabic"
private const val SLOT_TRANSLIT = "transliteration"
private const val SLOT_TRANSLATION = "translation"

/**
 * Panelin üç halı: [Hidden] — yalnız tutacaq xətti qalır və bütün yer mənbəyə verilir (uzun hədisi
 * oxuyub seçmək üçün); [Collapsed] — əsas sətirlər və düymələr; [Expanded] — üstəlik hissə qeydi və
 * «bu mənbədən əlavə olunanlar» siyahısı.
 */
private enum class PanelState { Hidden, Collapsed, Expanded }

/** Panelin yığılmış halda tuta biləcəyi ən çox hündürlük — qalanı mənbəyə qalsın. */
private const val PANEL_COLLAPSED_FRACTION = 0.58f

/** Açılmış panel mənbədən yalnız bir zolaq saxlayır. */
private const val PANEL_EXPANDED_FRACTION = 0.9f

/** Tutacağı bu qədər (px) çəkmək paneli açır/yığır — təsadüfi toxunuş sayılmasın. */
private const val PANEL_DRAG_THRESHOLD = 24f

// ------------------------------------------------------------------------------ ekran

/**
 * Seçim ekranı — hər iki axın eyni kadrdan keçir.
 *
 * Quruluş: yuxarıda **mənbə** (zikr çipləri, Ərəbcə / Tərcümə / Qeyd tab-ları və seçilən mətn),
 * altda **daimi panel** — yadda saxlanacaq sahələr, say, «Bölmə» və düymələr.
 *
 * ### Niyə panel
 * Əvvəl hər şey bir uzun sütunda idi: mənbə blokları yuxarıda, nəticə sahələri onların altında,
 * hədəf ən aşağıda. Uzun hədisdə seçimlə onun düşdüyü sahə arasında bir-iki ekran məsafə olurdu,
 * başlıq isə yalnız sona qədər sürüşəndə görünürdü. Panel həmişə görünür: seçim edilən anda nəyin
 * yazıldığı və nəyin əskik olduğu altda görünür. Tutacaqla açılanda hissə qeydi və «bu mənbədən
 * əlavə olunanlar» siyahısı da çıxır.
 *
 * ### Mənbədən seçim
 * Hər tab-ın mətni sistemin öz seçimi ilə seçilir ([SourceText]); seçim olanda mətnin altında üzən
 * zolaq çıxır: «Ərəbcə» / «Oxunuşu» / «Tərcümə». Hansı mətnin hansı sahəyə düşdüyünün sabit cavabı
 * yoxdur — bu toplusunda duanın **mənası** hədisin qeydindədir, **oxunuşu** isə rəvayətin içində
 * `{…}` arasında — ona görə seçən adam deyir. Ərəbcə tab-dan yalnız ərəbcə sahə doldurulur.
 *
 * ### Hissələr ([allowParts])
 * Dua axınında yadda saxlanacaq olan **hissələr siyahısıdır** (1..[MAX_DUA_PARTS]). Mənbə və zikr
 * çipləri həmişə **aktiv hissəyə** yazır; birdən çox hissə olanda panelin başında hissə tab-ları
 * durur. Əsma axınında hissə və say yoxdur.
 *
 * ⚠️ Tam ekran **`Dialog`**-dur (CLAUDE.md qaydası): ekran modal vərəqdən açılır, inline emit
 * ediləndə həmin vərəqin pəncərəsinin altında qalıb səssizcə heç nə etmiş kimi görünərdi.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ExcerptPickerScaffold(
    title: String,
    data: ExcerptSourceData,
    /** `true` → dua axını: say və hissələr. Əsmada dəlil tək sətirdir və sayı yoxdur. */
    allowParts: Boolean,
    /** Mənbədəki `{…}` zikrləri — boşdursa çip sırası çəkilmir. */
    segments: List<DhikrSegment>,
    /** Bu mənbədən artıq yazılanlar — mətndə solğun vurğu və açıq paneldəki siyahı. */
    savedExcerpts: List<SavedExcerpt>,
    isSaving: Boolean,
    missingTargetMessage: String,
    hasTarget: Boolean,
    /** Panelin hədəf sətrinin adı — «Bölmə» (dua), «Ad» (əsma). */
    targetRowLabel: String,
    targets: List<TargetSpec>,
    onClose: () -> Unit,
    /** Yoxlamadan keçmiş hissələr — tam boşlar atılıb, birincisi baş sətirdir. */
    onSave: (List<DuaPartDraft>) -> Unit,
    /**
     * «Saxla və davam et»: yazır, ekran açıq qalır. `onDone` **uğurdan sonra** çağırılmalıdır —
     * hissələr yalnız onda sıfırlanır (xətada seçilmiş mətnlər itməsin). `null` → düymə yoxdur.
     */
    onSaveAndContinue: ((List<DuaPartDraft>, onDone: () -> Unit) -> Unit)?,
) = Dialog(
    onDismissRequest = onClose,
    properties = DialogProperties(
        dismissOnBackPress = true,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false,
    ),
) {
    // **Yadda saxlanacaq** hissələr — mənbədəki canlı seçim yox, təsdiqlənmiş mətn.
    //
    // ⚠️ Vəziyyət **budaqlanmadan əvvəl** elan olunmalıdır: aşağıdakı `return@Dialog` yeni ad
    // ekranı açılanda bütün `Scaffold`-u kompozisiyadan çıxarır və içində olsaydı seçilmiş mətnlər
    // ora gedib qayıdanda itərdi.
    val parts = remember(data) { mutableStateListOf(DuaPartDraft()) }
    var activeIndex by remember(data) { mutableIntStateOf(0) }
    val active = activeIndex.coerceIn(0, parts.lastIndex)
    val current = parts[active]

    /** Hansı hədəf üçün «yeni» forması açıqdır — menyudakı «+ Yeni …» bəndi bunu qoyur. */
    var addingKey by remember { mutableStateOf<String?>(null) }

    /** Açıq menyunun hədəfi — «Bölmə» sətrindəki düymələrdən biri. */
    var menuKey by remember { mutableStateOf<String?>(null) }

    var panelState by remember(data) { mutableStateOf(PanelState.Collapsed) }

    /** Paneldə yerində redaktə olunan sahə ([ExcerptSlot.key]). */
    var editingSlot by remember(data) { mutableStateOf<String?>(null) }

    val tabs = remember(data) {
        buildList {
            add(SourceTab.Arabic)
            // Tərcümə/qeyd mənbədə olmaya bilər (oxucuda tərcümə seçilməyib, hədisin qeydi yoxdur) —
            // boş tab seçiləsi heç nə təklif etmir. Mətn paneldə əl ilə yazıla bilər.
            if (data.fullTranslation.isNotBlank()) add(SourceTab.Translation)
            if (!data.fullNote.isNullOrBlank()) add(SourceTab.Note)
        }
    }
    var sourceTab by remember(data) { mutableStateOf(SourceTab.Arabic) }

    // ⚠️ Sürüşmə də **budaqlanmadan əvvəl** və tab başına: yeni ad ekranından qayıdanda mənbə
    // yerində qalsın (başlığı seçəndə ekranın yuxarıya tullanması köhnə şikayət idi), tab
    // dəyişəndə isə hər mətn öz yerinə qayıtsın.
    val sourceScrolls = remember(data) { SourceTab.entries.associateWith { ScrollState(0) } }

    // Seçim də **budaqlanmadan əvvəl** və tab başına: «Hamısı» və «Zikrlər» menyusu
    // cari tab-ın seçimini idarə edir, yeni ad ekranından qayıdanda isə seçilmiş parça yerində qalır.
    val selections = remember(data) {
        mapOf(
            SourceTab.Arabic to SourceSelection(data.fullArabic),
            SourceTab.Translation to SourceSelection(data.fullTranslation),
            SourceTab.Note to SourceSelection(data.fullNote.orEmpty()),
        )
    }
    val selection = selections.getValue(sourceTab)

    // Əlavə etmə ekranında da iki/üç barmaqla ölçüləndirmə: seçiləcək parça bəzən uzun ərəbcə
    // rəvayətdir və onu barmaqla böyütmədən oxumaq çətindir. Ölçü bu ekranın **öz** açarlarındadır
    // (dua və hədis oxucusuna təsir etmir) — bax [DuaPreferences.PICKER_ARABIC_SIZE_MULT].
    val zoomScope = rememberCoroutineScope()
    var zoomFeedback by remember { mutableStateOf<ReaderZoomFeedback?>(null) }
    val zoomModifier = Modifier.readerTextZoom(
        enabled = AppPreferences.observeReaderPinchZoomEnabled(),
        arabicMultiplier = DuaPreferences.observePickerArabicSizeMultiplier(),
        translationMultiplier = DuaPreferences.observePickerTranslationSizeMultiplier(),
        minMultiplier = ReaderTextZoom.HADITH_MIN,
        maxMultiplier = ReaderTextZoom.HADITH_MAX,
        onZoom = { target, value ->
            zoomFeedback = ReaderZoomFeedback(target, value)
            zoomScope.launch {
                when (target) {
                    ReaderZoomTarget.Arabic -> DuaPreferences.setPickerArabicSizeMultiplier(value)
                    ReaderZoomTarget.Translation ->
                        DuaPreferences.setPickerTranslationSizeMultiplier(value)
                }
            }
        },
    )

    /** Aktiv hissəni dəyişir — indeks çağırış anında oxunur, kompozisiya anında yox. */
    fun updateActive(transform: (DuaPartDraft) -> DuaPartDraft) {
        val index = activeIndex.coerceIn(0, parts.lastIndex)
        parts[index] = transform(parts[index])
    }

    val optionalPlaceholder = stringResource(Res.string.duaPanelOptionalPlaceholder)
    val arabicSlot = ExcerptSlot(
        key = SLOT_ARABIC,
        label = stringResource(Res.string.labelArabic),
        placeholder = stringResource(Res.string.duaPanelArabicPlaceholder),
        value = current.arabic,
        arabic = true,
        assign = { text -> updateActive { it.copy(arabic = text) } },
    )
    val translitSlot = ExcerptSlot(
        key = SLOT_TRANSLIT,
        label = stringResource(Res.string.duaTransliterationLabel),
        placeholder = optionalPlaceholder,
        value = current.transliteration,
        arabic = false,
        assign = { text -> updateActive { it.copy(transliteration = text) } },
    )
    val translationSlot = ExcerptSlot(
        key = SLOT_TRANSLATION,
        label = stringResource(Res.string.labelTranslation),
        placeholder = optionalPlaceholder,
        value = current.translation,
        arabic = false,
        assign = { text -> updateActive { it.copy(translation = text) } },
    )

    val latinSlots = listOf(translitSlot, translationSlot)

    // Bu mənbədən yazılmış sətirlər (başqa mənbədən birləşdirilmiş hissələr burada vurğulanmır).
    val savedRows = remember(savedExcerpts, data) {
        savedExcerpts.flatMap { it.parts }.filter { data.isSourceOf(it) }
    }
    val savedArabic = remember(savedRows) { savedRows.map { it.text_ar } }
    val savedLatin = remember(savedRows) {
        savedRows.flatMap { listOfNotNull(it.transliteration, it.text_az) }
    }

    // Yoxlama kompozisiyada gedir: mesaj `stringResource` ilə əvvəlcədən oxunur (CLAUDE.md —
    // `getString` ilə düymədə oxumaq scope ləğvinə məruzdur).
    val validation = validateParts(parts)
    val problemMessage = when (val problem = validation.problem) {
        null -> null
        PartsProblem.Empty -> stringResource(Res.string.duaPickerMissingSelection)
        is PartsProblem.MissingArabic -> if (parts.size > 1) {
            stringResource(Res.string.duaPickerPartMissingArabic, problem.partNo)
        } else {
            stringResource(Res.string.duaPickerMissingSelection)
        }
        is PartsProblem.DuplicateArabic ->
            stringResource(Res.string.duaPickerPartDuplicate, problem.firstNo, problem.secondNo)
    }

    val targetLabel = targets
        .filter { it.enabled }
        .mapNotNull { it.chosenTitle }
        .joinToString(" › ")
    val doneLabel = stringResource(Res.string.strLabelDone)

    /** Yazılacaq hissələr, ya da `null` (səbəb toast ilə deyilir, problemli yer önə çıxır). */
    fun checkedParts(): List<DuaPartDraft>? {
        when (val problem = validation.problem) {
            null -> Unit
            is PartsProblem.MissingArabic -> activeIndex = problem.partNo - 1
            is PartsProblem.DuplicateArabic -> activeIndex = problem.secondNo - 1
            PartsProblem.Empty -> Unit
        }

        return when {
            problemMessage != null -> {
                PlatformUtils.showToast(problemMessage)
                null
            }

            !hasTarget -> {
                PlatformUtils.showToast(missingTargetMessage)
                // Hədəf menyusu elə açılır — «hara» sualının cavabı bir toxunuş uzaqda olsun.
                menuKey = targets.firstOrNull()?.key
                null
            }

            else -> validation.kept
        }
    }

    BackHandler(
        enabled = addingKey != null || editingSlot != null || panelState == PanelState.Expanded,
    ) {
        when {
            addingKey != null -> addingKey = null
            editingSlot != null -> editingSlot = null
            else -> panelState = PanelState.Collapsed
        }
    }

    val adding = targets.firstOrNull { it.key == addingKey }
    if (adding != null) {
        NewTargetScreen(spec = adding, onDone = { addingKey = null })
        return@Dialog
    }

    val selectionCaption: @Composable (Int) -> String = { length ->
        if (parts.size > 1) {
            stringResource(Res.string.duaPanelSelectionForPart, active + 1, length)
        } else {
            stringResource(Res.string.duaPickerSelected, length)
        }
    }

    Scaffold(
        topBar = {
            AppBar(
                titleContent = { PickerTitle(title = title, reference = data.reference) },
                onBack = onClose,
                // «Hamısı» bar-dadır, mənbənin üstündə ayrıca sətir tutmasın.
                actions = {
                    CompactTextButton(
                        text = stringResource(Res.string.duaPickerSelectAll),
                        enabled = selection.text.isNotEmpty(),
                        onClick = selection::selectAll,
                    )
                },
            )
        },
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            val panelMaxHeight = maxHeight *
                if (panelState == PanelState.Expanded) PANEL_EXPANDED_FRACTION else PANEL_COLLAPSED_FRACTION

            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Tab-lar və «Zikrlər ▾» **bir sırada**: əvvəl zikr çipləri, tab-lar və seçim
                        // düymələri üç ayrı sətir idi və mənbənin üstündə ~140dp yer tuturdu.
                        val hasDhikrMenu = segments.isNotEmpty() || selection.braces.isNotEmpty()

                        if (hasDhikrMenu || tabs.size > 1) {
                            Surface(color = colorScheme.surfaceContainer) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (tabs.size > 1) {
                                        SourceTabStrip(
                                            tabs = tabs,
                                            selected = sourceTab,
                                            onSelect = { sourceTab = it },
                                        )
                                    }

                                    Spacer(Modifier.weight(1f))

                                    if (hasDhikrMenu) {
                                        DhikrMenuButton(
                                            segments = segments,
                                            isSaved = { segment ->
                                                savedArabic.any { sameArabic(it, segment.arabic) }
                                            },
                                            onPick = { segment ->
                                                // Zikr «bu zikr» deməkdir: üç sahə birlikdə dəyişir,
                                                // yoxsa əvvəlki zikrin oxunuşu yeni zikrin ərəbcəsinin
                                                // yanında qalardı. Say isə istifadəçinin seçimidir.
                                                updateActive {
                                                    it.copy(
                                                        arabic = segment.arabic,
                                                        transliteration = segment.transliteration.orEmpty(),
                                                        translation = segment.translation.orEmpty(),
                                                    )
                                                }
                                                editingSlot = null
                                            },
                                            selection = selection,
                                            arabicSource = sourceTab == SourceTab.Arabic,
                                        )
                                    }
                                }
                            }
                        }

                        key(sourceTab) {
                            when (sourceTab) {
                                SourceTab.Arabic -> SourceText(
                                    selection = selections.getValue(SourceTab.Arabic),
                                    arabic = true,
                                    slots = listOf(arabicSlot),
                                    saved = savedArabic,
                                    hint = null,
                                    caption = selectionCaption,
                                    scrollState = sourceScrolls.getValue(SourceTab.Arabic),
                                    zoomModifier = zoomModifier,
                                    modifier = Modifier.weight(1f),
                                )

                                SourceTab.Translation -> SourceText(
                                    selection = selections.getValue(SourceTab.Translation),
                                    arabic = false,
                                    slots = latinSlots,
                                    saved = savedLatin,
                                    hint = null,
                                    caption = selectionCaption,
                                    scrollState = sourceScrolls.getValue(SourceTab.Translation),
                                    zoomModifier = zoomModifier,
                                    modifier = Modifier.weight(1f),
                                )

                                SourceTab.Note -> SourceText(
                                    selection = selections.getValue(SourceTab.Note),
                                    arabic = false,
                                    slots = latinSlots,
                                    saved = savedLatin,
                                    hint = stringResource(Res.string.duaPickerNoteHint),
                                    caption = selectionCaption,
                                    scrollState = sourceScrolls.getValue(SourceTab.Note),
                                    zoomModifier = zoomModifier,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    ReaderZoomFeedbackOverlay(zoomFeedback) { zoomFeedback = null }
                }

                ResultPanel(
                    state = panelState,
                    onStateChange = { state ->
                        panelState = state
                        menuKey = null
                        if (state == PanelState.Hidden) editingSlot = null
                    },
                    modifier = Modifier.heightIn(max = panelMaxHeight),
                    header = {
                        if (parts.size > 1) {
                            PartTabs(
                                parts = parts,
                                active = active,
                                onSelect = { index ->
                                    activeIndex = index
                                    editingSlot = null
                                },
                                onRemoveActive = {
                                    parts.removeAt(active)
                                    activeIndex = active.coerceAtMost(parts.lastIndex)
                                    editingSlot = null
                                },
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Text(
                                text = stringResource(Res.string.duaPickerResultSection),
                                style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        // Hədd bazadadır (`part_no` 1..5) — düymə ondan sonra ümumiyyətlə çəkilmir,
                        // basılıb heç nə etməsin deyə.
                        if (allowParts && parts.size < MAX_DUA_PARTS) {
                            val addPart = {
                                parts.add(DuaPartDraft())
                                activeIndex = parts.lastIndex
                                editingSlot = null
                            }

                            if (parts.size > 1) {
                                IconButton(onClick = addPart) {
                                    Icon(
                                        painter = painterResource(Res.drawable.dr_icon_add),
                                        contentDescription = stringResource(Res.string.duaPickerAddPart),
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            } else {
                                TextButton(onClick = addPart) {
                                    Icon(
                                        painter = painterResource(Res.drawable.dr_icon_add),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(Res.string.duaPickerAddPart))
                                }
                            }
                        }
                    },
                    footer = {
                        PanelFooter(
                            status = when {
                                problemMessage != null -> problemMessage
                                !hasTarget -> missingTargetMessage
                                else -> "$doneLabel · $targetLabel"
                            },
                            ready = problemMessage == null && hasTarget,
                            isSaving = isSaving,
                            onSave = {
                                editingSlot = null
                                checkedParts()?.let(onSave)
                            },
                            onSaveAndContinue = onSaveAndContinue?.let { saveAndContinue ->
                                {
                                    editingSlot = null
                                    checkedParts()?.let { kept ->
                                        saveAndContinue(kept) {
                                            parts.clear()
                                            parts.add(DuaPartDraft())
                                            activeIndex = 0
                                        }
                                    }
                                }
                            },
                        )
                    },
                ) {
                    listOf(arabicSlot, translitSlot, translationSlot).forEach { slot ->
                        SlotRow(
                            slot = slot,
                            editing = editingSlot == slot.key,
                            onEdit = { editingSlot = slot.key },
                            onDone = { editingSlot = null },
                        )
                    }

                    if (allowParts) {
                        CountChipRow(
                            value = current.countText,
                            onValueChange = { text -> updateActive { it.copy(countText = text) } },
                        )
                        PanelDivider()
                    }

                    TargetCrumbRow(
                        label = targetRowLabel,
                        targets = targets,
                        menuKey = menuKey,
                        onMenu = { key ->
                            menuKey = key
                            if (key != null) editingSlot = null
                        },
                        onAddNew = { key -> addingKey = key },
                    )

                    if (panelState == PanelState.Expanded) {
                        if (allowParts) {
                            Text(
                                text = stringResource(Res.string.duaPickerPartsHint),
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant.alpha(0.8f),
                                modifier = Modifier.padding(top = 12.dp, end = 8.dp),
                            )
                        }

                        Box(modifier = Modifier.padding(top = 12.dp, end = 8.dp)) {
                            SavedFromSourceList(savedExcerpts)
                        }
                    }
                }
            }
        }
    }
}

/** App bar-ın başlığı — ekranın adı və altında mənbənin istinadı («Buxari №12»). */
@Composable
private fun PickerTitle(title: String, reference: String?) {
    Column {
        Text(
            text = title,
            style = AppBarDefaults.titleStyle,
            fontWeight = FontWeight.ExtraBold,
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        reference?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = typography.labelSmall.withScriptDirection(arabic = false),
                color = colorScheme.onSurfaceVariant.alpha(0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Ərəbcə / Tərcümə / Qeyd seçicisi — oxuma ekranlarındakı [ModeTabStrip] ilə eyni zolaq: istifadəçi
 * onu hədis və dua ekranlarından tanıyır.
 */
@Composable
private fun SourceTabStrip(
    tabs: List<SourceTab>,
    selected: SourceTab,
    onSelect: (SourceTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val arabicLabel = stringResource(Res.string.labelArabic)
    val translationLabel = stringResource(Res.string.labelTranslation)
    val noteLabel = stringResource(Res.string.duaPickerNoteLabel)

    ModeTabStrip(
        tabs = tabs.map { tab ->
            when (tab) {
                SourceTab.Arabic ->
                    ModeTab(ModeTabIcon.Painted(Res.drawable.dr_icon_quran_script), arabicLabel)

                SourceTab.Translation ->
                    ModeTab(ModeTabIcon.Painted(Res.drawable.dr_icon_translations), translationLabel)

                SourceTab.Note ->
                    ModeTab(ModeTabIcon.Painted(Res.drawable.dr_icon_footnote), noteLabel)
            }
        },
        selectedIndex = tabs.indexOf(selected).coerceAtLeast(0),
        onSelect = { index -> tabs.getOrNull(index)?.let(onSelect) },
        modifier = modifier,
    )
}

/**
 * Bir tab-ın seçim vəziyyəti — mətn, sahənin dəyəri və **son** seçilmiş aralıq.
 *
 * Ekran səviyyəsində saxlanılır (tab başına bir dənə): app bar-dakı «Hamısı» və «Zikrlər» menyusu
 * cari tab-ın seçimini dəyişir, mətn özü isə [SourceText]-də çəkilir.
 *
 * ### Fokus gedəndə seçim itir — ona görə **son** seçim saxlanılır
 * Üzən zolaqdakı və ya bar-dakı düyməyə basanda sahə fokusu itirir və `selection` boşalır;
 * [picked] son boş olmayan aralığı saxlayır, yəni düymə həmişə istifadəçinin gördüyü parçanı yazır.
 */
@Stable
private class SourceSelection(val text: String) {
    /** Mətn dəyişmir (`readOnly`), dəyişən yalnız seçimdir. */
    var field by mutableStateOf(TextFieldValue(text))
    var picked by mutableStateOf<TextRange?>(null)

    /** Mətndəki zikr mötərizələri (`[…]` və `{…}`) — mötərizələrin özü daxil ([braceRanges]). */
    val braces: List<IntRange> = braceRanges(text)

    /** [index]-ci mötərizənin **içi** — mötərizəsiz, sahəyə elə bu yazılır. */
    fun braceContent(index: Int): TextRange = braces[index].let { TextRange(it.first + 1, it.last) }

    /** Cari seçim hansı mötərizənin içidir (0-dan), heç biri deyilsə -1 — menyudakı ✓ üçün. */
    val selectedBrace: Int
        get() = braces.indices.firstOrNull { picked == braceContent(it) } ?: -1

    /** Seçilmiş parça, kənar boşluqsuz; seçim yoxdursa boş sətir. */
    val live: String
        get() = picked?.let { text.substring(it.min, it.max).trim() }.orEmpty()

    fun select(range: TextRange?) {
        picked = range
        field = field.copy(selection = range ?: TextRange.Zero)
    }

    fun selectAll() = select(TextRange(0, text.length))
}

/**
 * «[…] Zikrlər ▾» — mötərizə ilə bağlı hər şey bir açılan menyuda, «Bölmə» düymələri kimi.
 *
 * Əvvəl iki ayrı yer idi: tab-ların altında zikr çipləri, app bar-da isə hər basışda növbəti
 * mötərizəyə keçən «Mötərizədə» düyməsi. İkisi də eyni `{…}` parçalarına baxır, ona görə bir menyudur:
 * - **Hədisdəki zikrlər** — ərəbcəni, oxunuşu və tərcüməni aktiv hissəyə birdən yazır; bu mənbədən
 *   artıq yazılmış zikrin yanında ✓ var.
 * - **Mətndə seç** — cari tab-dakı `{…}`-nun içini seçir (mötərizəsiz). Bu topluda duanın oxunuşu
 *   məhz mötərizələrin arasındadır, yəni ən çox istənən aralıq bir toxunuşla seçilir; sonra üzən
 *   zolaqla istənilən sahəyə yazılır.
 */
@Composable
private fun DhikrMenuButton(
    segments: List<DhikrSegment>,
    isSaved: (DhikrSegment) -> Boolean,
    onPick: (DhikrSegment) -> Unit,
    selection: SourceSelection,
    /** Cari tab ərəbcədir — «Mətndə seç» bəndləri ərəb yazısı ilə çəkilsin. */
    arabicSource: Boolean,
) {
    var open by remember { mutableStateOf(false) }
    val savedLabel = stringResource(Res.string.duaPickerAlreadyAdded)

    Box {
        Surface(
            onClick = { open = true },
            shape = RoundedCornerShape(10.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = 38.dp)
                    .padding(start = 10.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "[…]",
                    style = typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.primary,
                )
                Text(
                    text = stringResource(Res.string.duaPanelSegments),
                    style = typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                )
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_chevron_down),
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.widthIn(min = 240.dp, max = 340.dp),
        ) {
            if (segments.isNotEmpty()) {
                MenuSectionLabel(stringResource(Res.string.duaPickerSegmentsTitle))

                segments.forEachIndexed { index, segment ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                segment.transliteration?.let {
                                    Text(
                                        text = it,
                                        style = typography.bodyLarge.withScriptDirection(arabic = false),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text(
                                    text = segment.arabic,
                                    style = typography.titleSmall.withScriptDirection(
                                        arabic = true,
                                        arabicFontFamily = arabicFontFamily(),
                                    ),
                                    color = colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        },
                        leadingIcon = { MenuNumberBadge(index + 1) },
                        trailingIcon = if (isSaved(segment)) {
                            {
                                Icon(
                                    painter = painterResource(Res.drawable.dr_icon_check),
                                    contentDescription = savedLabel,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        } else {
                            null
                        },
                        onClick = {
                            onPick(segment)
                            open = false
                        },
                    )
                }
            }

            if (selection.braces.isNotEmpty()) {
                if (segments.isNotEmpty()) {
                    HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.4f))
                }

                MenuSectionLabel(stringResource(Res.string.duaPanelSelectInText))

                val selectedBrace = selection.selectedBrace
                selection.braces.indices.forEach { index ->
                    val range = selection.braceContent(index)

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = selection.text.substring(range.min, range.max).trim(),
                                style = if (arabicSource) {
                                    typography.titleSmall.withScriptDirection(
                                        arabic = true,
                                        arabicFontFamily = arabicFontFamily(),
                                    )
                                } else {
                                    typography.bodyLarge.withScriptDirection(arabic = false)
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        leadingIcon = { MenuNumberBadge(index + 1) },
                        trailingIcon = if (index == selectedBrace) {
                            {
                                Icon(
                                    painter = painterResource(Res.drawable.dr_icon_check),
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        } else {
                            null
                        },
                        onClick = {
                            selection.select(range)
                            open = false
                        },
                    )
                }
            }
        }
    }
}

/** Menyudakı bölmə adı — «Hədisdəki zikrlər», «Mətndə seç». */
@Composable
private fun MenuSectionLabel(text: String) {
    Text(
        text = text,
        style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
    )
}

/** Menyu bəndinin nömrəsi — zikrin hədisdəki sırası. */
@Composable
private fun MenuNumberBadge(number: Int) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .background(colorScheme.primaryContainer.alpha(0.5f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
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
 * ### Niyə **çərçivəsiz** və öz sürüşməsi olmadan
 * Daha əvvəlki variant `OutlinedTextField` idi — etiketli, çərçivəli və **içində sürüşən** qutu.
 * Tutacağı çəkəndə gah qutunun öz içi sürüşürdü, gah səhifə, gah da tutacaq əldən çıxırdı. Burada
 * mətn tam hündürlüyü ilə çəkilir və yalnız tab-ın öz sütunu sürüşür.
 *
 * Fokussuz halda sistem seçim vurğusunu çəkmir, ona görə [SourceSelection.picked] aralığını
 * [excerptHighlights] özü boyayır — «seçdim, amma ekranda heç nə görünmür» halı qalmasın.
 */
@Composable
private fun SourceText(
    selection: SourceSelection,
    arabic: Boolean,
    slots: List<ExcerptSlot>,
    /** Bu mənbədən artıq yazılmış parçalar — mətndə solğun fonla görünür. */
    saved: List<String>,
    /** Mətnin başında, onunla birlikdə sürüşən qeyd (qeyd tab-ında). */
    hint: String?,
    /** Üzən zolağın yazısı — seçilən simvol sayı və (hissə çoxdursa) hansı hissəyə düşdüyü. */
    caption: @Composable (Int) -> String,
    scrollState: ScrollState,
    zoomModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val text = selection.text
    var focused by remember(text) { mutableStateOf(false) }

    // Artıq yazılmış parçaların yeri — tapılmayan (mənbə sonradan redaktə olunub) sadəcə boyanmır.
    val savedRanges = remember(text, saved) {
        saved.mapNotNull { excerpt -> excerptMatchRange(text, excerpt) }
    }

    // Seçim panelinin bəndi kompozisiyadan kənarda çağırıldığı üçün etiket əvvəlcədən oxunur.
    val useLabel = stringResource(Res.string.duaPickerUseSelection)

    val live = selection.live

    val highlight = colorScheme.primary.alpha(0.28f)
    val savedHighlight = colorScheme.tertiary.alpha(0.14f)

    // Ölçü bu ekranın öz açarlarındandır (dua/hədis oxucusundan ayrı); iki/üç barmaqlı jest mətnin
    // sürüşən sütununa qoşulub.
    val sizeMult = if (arabic) {
        DuaPreferences.observePickerArabicSizeMultiplier()
    } else {
        DuaPreferences.observePickerTranslationSizeMultiplier()
    }
    val textStyle = typography.bodyLarge
        .copy(fontSize = typography.bodyLarge.fontSize * sizeMult)
        .withLineHeightRatio(if (arabic) ARABIC_EXCERPT_LINE_HEIGHT_RATIO else TRANSLATION_LINE_HEIGHT_RATIO)
        .withScriptDirection(
            arabic = arabic,
            arabicFontFamily = if (arabic) arabicFontFamily() else null,
        )

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(zoomModifier)
                .verticalScroll(scrollState),
        ) {
            // ⚠️ `ReadableWidthColumn` **Box**-dur — daxili `Column` məcburidir.
            ReadableWidthColumn {
                Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                    Spacer(Modifier.height(10.dp))

                    hint?.let {
                        Text(
                            text = it,
                            style = typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                            color = colorScheme.onSurfaceVariant.alpha(0.7f),
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }

                    BasicTextField(
                        value = selection.field,
                        onValueChange = { updated ->
                            selection.field = updated.copy(text = text)
                            if (!updated.selection.collapsed) selection.picked = updated.selection
                        },
                        readOnly = true,
                        textStyle = textStyle.copy(color = colorScheme.onSurface),
                        // Kursor `readOnly` sahədə heç nə bildirmir, amma mətnin ortasında
                        // yanıb-sönən xətt onu «redaktə olunur» kimi göstərir.
                        cursorBrush = SolidColor(Color.Transparent),
                        visualTransformation = excerptHighlights(
                            saved = savedRanges,
                            savedColor = savedHighlight,
                            pick = selection.picked.takeIf { !focused },
                            pickColor = highlight,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            // Sistemin seçim panelinə (Kopyala · Hamısını seç · Paylaş) hədəf
                            // bəndləri də qoyulur — seçəndən sonra barmağı üzən zolağa aparmaq
                            // məcburi olmasın.
                            .appendTextContextMenuComponents {
                                slots.forEach { slot ->
                                    excerptMenuItem(
                                        key = slot.key,
                                        label = if (slots.size == 1) useLabel else slot.label,
                                    ) {
                                        // Panel açıq olduğu üçün seçim hələ canlıdır; `picked`
                                        // onu artıq tutub, ona görə mətn buradan götürülür.
                                        val selected = selection.live
                                        if (selected.isNotBlank()) slot.assign(selected)
                                        close()
                                    }
                                }
                            }
                            .onFocusChanged { focused = it.isFocused },
                    )

                    // Üzən zolaq / ipucu mətnin sonunu örtməsin.
                    Spacer(Modifier.height(96.dp))
                }
            }
        }

        if (live.isNotBlank()) {
            SelectionBar(
                caption = caption(live.length),
                slots = slots,
                onAssign = { slot ->
                    slot.assign(live)
                    // Seçim götürülür ki, panel sətrinin dəyişdiyi görünsün və eyni parça
                    // təsadüfən ikinci dəfə yazılmasın.
                    selection.select(null)
                },
                onDismiss = { selection.select(null) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
            )
        } else {
            // Seçim yoxdursa zolağın yerində ipucu — başlıqda ayrıca sətir tutmasın.
            Text(
                text = stringResource(Res.string.duaPanelSelectHint),
                style = typography.labelMedium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .background(colorScheme.surfaceContainer.alpha(0.92f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/** Mənbənin üstündəki kiçik mətn düyməsi — üç düymə bir sətirə sığsın. */
@Composable
private fun CompactTextButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(text = text, maxLines = 1, softWrap = false)
    }
}

/**
 * Üzən zolaq — mənbədə seçim olanda mətnin altında çıxır: «Ərəbcə ↓» və ya «Oxunuşu ↓ · Tərcümə ↓».
 *
 * Tünd (`inverseSurface`) fondadır ki, mənbə mətninin üstündə ayrıca qat kimi oxunsun; panelin
 * düz üstündə durur — seçimin hara düşəcəyi gözün qabağındadır.
 */
@Composable
private fun SelectionBar(
    caption: String,
    slots: List<ExcerptSlot>,
    onAssign: (ExcerptSlot) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = colorScheme.inverseSurface,
        contentColor = colorScheme.inverseOnSurface,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 6.dp,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(start = 6.dp, end = 2.dp, top = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = caption,
                style = typography.labelSmall,
                color = colorScheme.inverseOnSurface.alpha(0.7f),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                slots.forEach { slot ->
                    TextButton(
                        onClick = { onAssign(slot) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = colorScheme.inverseOnSurface,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_chevron_down),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = slot.label,
                            style = typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_close),
                        contentDescription = stringResource(Res.string.duaPickerClear),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/**
 * Daimi panel — mənbənin altında durur, üç halda ([PanelState]).
 *
 * Tutacağa toxunmaq paneli növbəti hala keçirir (gizli → yığılmış → açıq → yığılmış), onu yuxarı
 * çəkmək böyüdür, aşağı çəkmək kiçildir. Başlıqdakı oxlar eyni işi görür: yığılmış haldakı «▾» paneli
 * **nazik xəttə qədər** gizlədir — uzun hədisi oxuyub seçəndə bütün yer mənbəyə qalır, üzən zolaq
 * isə bu halda da işləyir. Hündürlük çağıran tərəfdən `heightIn(max)` ilə məhdudlaşır, artığı
 * panelin içində sürüşür.
 *
 * `BottomSheetScaffold` qəsdən işlənmir: onun vərəqi mənbənin **üstünə** düşür və öz sürüşmə jesti
 * var — mətnin seçim tutacaqları ilə eyni barmaq hərəkətini bölüşərdi. Burada panel sütunun adi bir
 * hissəsidir, mənbə isə qalan yerdə öz sürüşməsi ilə qalır.
 */
@Composable
private fun ResultPanel(
    state: PanelState,
    onStateChange: (PanelState) -> Unit,
    header: @Composable RowScope.() -> Unit,
    footer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    var drag by remember { mutableStateOf(0f) }

    val bigger = when (state) {
        PanelState.Hidden -> PanelState.Collapsed
        PanelState.Collapsed, PanelState.Expanded -> PanelState.Expanded
    }
    val smaller = when (state) {
        PanelState.Hidden, PanelState.Collapsed -> PanelState.Hidden
        PanelState.Expanded -> PanelState.Collapsed
    }
    val onHandleTap = if (state == PanelState.Expanded) PanelState.Collapsed else bigger

    val expandLabel = stringResource(Res.string.duaPanelExpand)
    val shrinkLabel = stringResource(Res.string.duaPanelCollapse)
    val hideLabel = stringResource(Res.string.duaPanelHide)
    val handleLabel = when (state) {
        PanelState.Hidden -> stringResource(Res.string.duaPanelShow)
        PanelState.Collapsed -> expandLabel
        PanelState.Expanded -> shrinkLabel
    }

    Surface(
        color = colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        shadowElevation = 10.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // Gizli halda tutacaq panelin **hamısıdır** — barmaqla tutmaq asan olsun.
                    .height(if (state == PanelState.Hidden) 24.dp else 20.dp)
                    .clickable(onClickLabel = handleLabel) { onStateChange(onHandleTap) }
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta -> drag += delta },
                        onDragStopped = {
                            if (drag < -PANEL_DRAG_THRESHOLD) onStateChange(bigger)
                            if (drag > PANEL_DRAG_THRESHOLD) onStateChange(smaller)
                            drag = 0f
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .background(colorScheme.outlineVariant, CircleShape),
                )
            }

            if (state != PanelState.Hidden) {

                ReadableWidthColumn {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .padding(start = 16.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        header()

                        if (state == PanelState.Collapsed) {
                            IconButton(onClick = { onStateChange(PanelState.Expanded) }) {
                                Icon(
                                    painter = painterResource(Res.drawable.dr_icon_chevron_down),
                                    contentDescription = expandLabel,
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp).rotate(180f),
                                )
                            }
                        }

                        IconButton(onClick = { onStateChange(smaller) }) {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_chevron_down),
                                contentDescription = if (state == PanelState.Expanded) shrinkLabel else hideLabel,
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                ) {
                    ReadableWidthColumn {
                        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, bottom = 8.dp)) {
                            content()
                        }
                    }
                }

                HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.4f))

                ReadableWidthColumn {
                    footer()
                }
            }
        }
    }
}

/** Panel sətirlərinin arasındakı nazik xətt. */
@Composable
private fun PanelDivider() {
    HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.35f))
}

/**
 * Panelin bir sahəsi — «Ərəbcə · سُبْحَانَ اللَّهِ … ×».
 *
 * Toxunanda sətir **yerində** redaktə sahəsinə çevrilir: mənbədə olmayan oxunuş və ya tərcümə
 * buradan yazılır. Doluluğu ad özü göstərir — dolu sahənin adı yaşıl, boş ərəbcə (məcburi) isə
 * xəbərdarlıq rəngindədir.
 */
@Composable
private fun SlotRow(
    slot: ExcerptSlot,
    editing: Boolean,
    onEdit: () -> Unit,
    onDone: () -> Unit,
) {
    val filled = slot.value.isNotBlank()

    val contentStyle = if (slot.arabic) {
        typography.titleMedium.withScriptDirection(
            arabic = true,
            arabicFontFamily = arabicFontFamily(),
        )
    } else {
        typography.bodyMedium.withScriptDirection(arabic = false)
    }

    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = slot.label,
            style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = when {
                filled -> colorScheme.primary
                slot.arabic -> colorScheme.tertiary
                else -> colorScheme.onSurfaceVariant
            },
            maxLines = 2,
            modifier = Modifier.width(PanelLabelWidth).padding(end = 8.dp),
        )

        if (editing) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }

            BasicTextField(
                value = slot.value,
                onValueChange = slot.assign,
                textStyle = contentStyle.copy(color = colorScheme.onSurface),
                cursorBrush = SolidColor(colorScheme.primary),
                maxLines = 6,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp)
                    .focusRequester(focusRequester)
                    .border(1.5.dp, colorScheme.primary, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )

            IconButton(onClick = onDone) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_check),
                    contentDescription = stringResource(Res.string.strLabelDone),
                    tint = colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            Text(
                text = if (filled) slot.value else slot.placeholder,
                style = if (filled) {
                    contentStyle
                } else {
                    typography.bodyMedium.copy(fontStyle = FontStyle.Italic)
                },
                color = if (filled) colorScheme.onSurface else colorScheme.onSurfaceVariant.alpha(0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onEdit)
                    .padding(vertical = 12.dp),
            )

            if (filled) {
                IconButton(onClick = { slot.assign("") }) {
                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_close),
                        contentDescription = stringResource(Res.string.duaPickerClear),
                        tint = colorScheme.onSurfaceVariant.alpha(0.7f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }

    PanelDivider()
}

/**
 * «Bölmə: [Başlıq ▾] › [Alt başlıq ▾]» — hədəf seçimi elə paneldə, açılan menyu ilə.
 *
 * Əvvəl hədəf ayrıca siyahı ekranında seçilirdi (sətir → yeni ekran → seçim → geri). Burada hər
 * pillə öz düyməsidir və menyusu düymənin yanında açılır; «+ Yeni …» menyunun sonundadır. Alt başlıq
 * düyməsi başlıq seçilənə qədər çəkilmir ([TargetSpec.enabled]).
 */
@Composable
private fun TargetCrumbRow(
    label: String,
    targets: List<TargetSpec>,
    menuKey: String?,
    onMenu: (String?) -> Unit,
    onAddNew: (String) -> Unit,
) {
    val firstChosen = targets.firstOrNull()?.chosenTitle != null

    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (firstChosen) colorScheme.primary else colorScheme.tertiary,
            maxLines = 2,
            modifier = Modifier.width(PanelLabelWidth).padding(end = 8.dp),
        )

        Row(
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            targets.filter { it.enabled }.forEachIndexed { index, spec ->
                if (index > 0) {
                    Text(
                        text = "›",
                        style = typography.titleMedium,
                        color = colorScheme.onSurfaceVariant.alpha(0.6f),
                    )
                }

                // Uzun adlarda iki düymə sətri bölüşür və hər biri öz adını qısaldır.
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    CrumbButton(
                        spec = spec,
                        required = !spec.optional,
                        onClick = { onMenu(if (menuKey == spec.key) null else spec.key) },
                    )

                    TargetMenu(
                        spec = spec,
                        expanded = menuKey == spec.key,
                        onDismiss = { onMenu(null) },
                        onAddNew = {
                            onMenu(null)
                            onAddNew(spec.key)
                        },
                    )
                }
            }
        }
    }
}

/** «Bölmə» sətrindəki bir düymə — seçilmiş ad (və ya «Başlıq seçin») və ▾. */
@Composable
private fun CrumbButton(spec: TargetSpec, required: Boolean, onClick: () -> Unit) {
    val chosen = spec.chosenTitle
    val missing = chosen == null && required

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = when {
            chosen != null -> colorScheme.primary.alpha(0.1f)
            else -> Color.Transparent
        },
        border = BorderStroke(
            width = if (missing) 1.5.dp else 1.dp,
            color = when {
                chosen != null -> colorScheme.primary.alpha(0.55f)
                missing -> colorScheme.tertiary
                else -> colorScheme.outlineVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 38.dp)
                .padding(start = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = chosen ?: spec.placeholder,
                style = typography.labelLarge.copy(
                    fontWeight = if (chosen != null) FontWeight.SemiBold else FontWeight.Medium,
                ).let { if (chosen != null) it.withScriptDirection(arabic = false) else it },
                color = when {
                    chosen != null -> colorScheme.onSurface
                    missing -> colorScheme.tertiary
                    else -> colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )

            Icon(
                painter = painterResource(Res.drawable.dr_icon_chevron_down),
                contentDescription = spec.label,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Hədəfin açılan menyusu — «yoxdur» (istəyə bağlı hədəfdə), siyahı və «+ Yeni …».
 *
 * Axtarış sahəsi **qəsdən yoxdur**: siyahı başlıqlar üçün bir neçə sətirdir, 99 ad isə onsuz da
 * nömrə sırası ilə düzülüb. Menyu ekrana sığmayanda özü sürüşür.
 */
@Composable
private fun TargetMenu(
    spec: TargetSpec,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onAddNew: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 220.dp, max = 340.dp),
    ) {
        val check: @Composable () -> Unit = {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_check),
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }

        // «Yoxdur» yalnız istəyə bağlı hədəfdə: alt başlıq seçilməyəndə dua birbaşa başlığın
        // altına düşür, bu da qanuni bir seçimdir.
        if (spec.optional) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.duaNoSubtitle)) },
                trailingIcon = if (spec.selectedId == null && spec.newName.isBlank()) check else null,
                onClick = {
                    spec.onSelect(null)
                    spec.onNewName("")
                    onDismiss()
                },
            )
        }

        // Yazılmış, hələ yaradılmamış yeni ad — toxunanda forması yenidən açılır.
        if (spec.selectedId == null && spec.newName.isNotBlank()) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = spec.newName.trim(),
                        style = typography.bodyLarge.withScriptDirection(arabic = false),
                    )
                },
                trailingIcon = check,
                onClick = onAddNew,
            )
        }

        spec.options.forEach { option ->
            val selected = option.id == spec.selectedId

            DropdownMenuItem(
                text = {
                    Column {
                        Text(
                            text = option.title,
                            style = typography.bodyLarge.withScriptDirection(arabic = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        option.subtitle?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                style = typography.bodySmall.withScriptDirection(arabic = false),
                                color = colorScheme.onSurfaceVariant.alpha(0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                leadingIcon = option.badge?.let { badge ->
                    {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(
                                    color = colorScheme.primaryContainer.alpha(0.5f),
                                    shape = RoundedCornerShape(9.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = badge,
                                style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.primary,
                            )
                        }
                    }
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        option.arabic?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                style = typography.titleSmall.withScriptDirection(
                                    arabic = true,
                                    arabicFontFamily = arabicFontFamily(),
                                ),
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        if (selected) {
                            Spacer(Modifier.width(8.dp))
                            check()
                        }
                    }
                },
                onClick = {
                    spec.onSelect(option.id)
                    onDismiss()
                },
            )
        }

        if (spec.allowNew) {
            HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.4f))

            DropdownMenuItem(
                text = {
                    Text(
                        text = spec.newLabel,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_add),
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = onAddNew,
            )
        }
    }
}

/**
 * Panelin altı — vəziyyət sətri və düymələr.
 *
 * Vəziyyət sətri yadda saxlamadan **əvvəl** nəyin əskik olduğunu deyir («Ərəbcə hissəni seçin…»,
 * «Başlıq seçin…»), hazır olanda isə hara yazılacağını («Hazırdır · Gündəlik zikrlər › Təsbih»).
 * Düymələr həmişə aktivdir: basılanda səbəb toast ilə də deyilir və problemli yer önə çıxır.
 */
@Composable
private fun PanelFooter(
    status: String,
    ready: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit,
    onSaveAndContinue: (() -> Unit)?,
) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (ready) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_check_circle),
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }

            Text(
                text = status,
                style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (ready) colorScheme.primary else colorScheme.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // «Davam et» ikinci dərəcəlidir (outlined): adi hal bir dua yazıb çıxmaqdır, bir
            // hədisdən bir neçə zikr götürmək isə ehtiyac olanda.
            onSaveAndContinue?.let { saveAndContinue ->
                OutlinedButton(
                    onClick = saveAndContinue,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(Res.string.duaPickerSaveContinue),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Button(
                onClick = onSave,
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.duaPickerSave),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * Mətndəki vurğular: bu mənbədən **artıq yazılmış** parçalar solğun ([saved]), cari seçim tünd
 * ([pick] — sahə fokusda olmayanda; fokusda sistem özü çəkir). Heç biri yoxdursa çevirmə yoxdur.
 *
 * Ofset xəritəsi **eynilik** olmalıdır — çevirmə yalnız rəng verir, bir simvol belə əlavə etmir;
 * əks halda tutacaqların mövqeyi mətnlə üst-üstə düşməzdi.
 */
private fun excerptHighlights(
    saved: List<IntRange>,
    savedColor: Color,
    pick: TextRange?,
    pickColor: Color,
): VisualTransformation {
    val activePick = pick?.takeIf { !it.collapsed }
    if (saved.isEmpty() && activePick == null) return VisualTransformation.None

    return VisualTransformation { original ->
        val length = original.length
        TransformedText(
            text = buildAnnotatedString {
                append(original)
                saved.forEach { range ->
                    addStyle(
                        style = SpanStyle(background = savedColor),
                        start = range.first.coerceIn(0, length),
                        end = (range.last + 1).coerceIn(0, length),
                    )
                }
                // Seçim **sonra** qoyulur ki, saxlanmış parçanın üstündə də görünsün.
                activePick?.let { range ->
                    addStyle(
                        style = SpanStyle(background = pickColor),
                        start = range.min.coerceIn(0, length),
                        end = range.max.coerceIn(0, length),
                    )
                }
            },
            offsetMapping = OffsetMapping.Identity,
        )
    }
}

/**
 * Yeni başlıq/alt başlıq ekranı — «Bölmə» menyusundakı «+ Yeni …» bəndi açır.
 *
 * Öz ekranıdır və öz `Scaffold`-u var: ad sahəsi klaviatura ilə birlikdə tam yer istəyir, kiçik
 * menyunun içində yazmaq olmazdı.
 */
@Composable
private fun NewTargetScreen(spec: TargetSpec, onDone: () -> Unit) {
    Scaffold(
        topBar = { AppBar(title = spec.label, onBack = onDone) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = spec.newLabel,
                style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
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
                Text(spec.newLabel)
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

/** Adın nömrəsi ilə etiketi — siyahıda və detal ekranında eyni formatı işlədirik. */
@Composable
internal fun asmaNumberLabel(no: Int): String = stringResource(Res.string.asmaNameNo, no)
