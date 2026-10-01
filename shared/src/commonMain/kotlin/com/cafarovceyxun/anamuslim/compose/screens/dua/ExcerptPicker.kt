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
import androidx.compose.material3.IconButton
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
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.ReadableWidthColumn
import com.cafarovceyxun.anamuslim.compose.components.common.readableWidthInset
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
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_right
import com.cafarovceyxun.anamuslim.resources.dr_icon_footnote
import com.cafarovceyxun.anamuslim.resources.dr_icon_quran_script
import com.cafarovceyxun.anamuslim.resources.dr_icon_translations
import com.cafarovceyxun.anamuslim.resources.duaConfirmSelection
import com.cafarovceyxun.anamuslim.resources.duaNoSubtitle
import com.cafarovceyxun.anamuslim.resources.duaManualHint
import com.cafarovceyxun.anamuslim.resources.duaTranslationOptional
import com.cafarovceyxun.anamuslim.resources.duaPickerArabicLabel
import com.cafarovceyxun.anamuslim.resources.duaPickerChooseName
import com.cafarovceyxun.anamuslim.resources.duaPickerChooseTitle
import com.cafarovceyxun.anamuslim.resources.duaPickerClear
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
import com.cafarovceyxun.anamuslim.viewModels.AsmaViewModel
import com.cafarovceyxun.anamuslim.viewModels.DuaViewModel
import com.cafarovceyxun.anamuslim.resources.duaMsgSaved
import com.cafarovceyxun.anamuslim.resources.duaMsgSavedContinue
import com.cafarovceyxun.anamuslim.resources.duaPickerAddPart
import com.cafarovceyxun.anamuslim.resources.duaPickerPartDuplicate
import com.cafarovceyxun.anamuslim.resources.duaPickerPartMissingArabic
import com.cafarovceyxun.anamuslim.resources.duaPickerPartsHint
import com.cafarovceyxun.anamuslim.resources.duaPickerSaveContinue
import com.cafarovceyxun.anamuslim.resources.duaPickerSelectBracesIndexed
import com.cafarovceyxun.anamuslim.utils.dua.DhikrSegment
import com.cafarovceyxun.anamuslim.utils.dua.braceRanges
import com.cafarovceyxun.anamuslim.utils.dua.dhikrSegments
import com.cafarovceyxun.anamuslim.utils.dua.nextBraceRange
import com.cafarovceyxun.anamuslim.utils.supabase.DuaSourceRef
import com.cafarovceyxun.anamuslim.utils.supabase.DuaSourceType
import com.cafarovceyxun.anamuslim.utils.supabase.MAX_DUA_PARTS
import com.cafarovceyxun.anamuslim.utils.text.excerptMatchRange
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
 * Bir hədəf sətri — «Başlıq», «Alt başlıq», «Ad».
 *
 * Siyahı dua axınında iki, Əsmaül Hüsnə axınında bir sətirdən ibarətdir; ekran onları eyni kodla
 * çəkir və hər biri üçün eyni seçim addımını açır.
 */
private data class TargetSpec(
    val key: String,
    val label: String,
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
        targets = listOf(
            TargetSpec(
                key = KEY_CATEGORY,
                label = stringResource(Res.string.duaPickerChooseTitle),
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
        targets = listOf(
            TargetSpec(
                key = KEY_NAME,
                label = stringResource(Res.string.duaPickerChooseName),
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
 * ### Hissələr ([allowParts])
 * Dua axınında yadda saxlanacaq olan **hissələr siyahısıdır** (1..[MAX_DUA_PARTS]). Mənbə
 * düymələri və zikr çipləri həmişə **aktiv hissəyə** yazır; birdən çox hissə olanda blokların
 * üstündə «Seçim bu hissəyə yazılır» seçicisi çıxır, aşağıda isə aktiv hissə açıq kart, qalanları
 * bir sətirlik xülasədir. Tək hissədə ekran əvvəlki kimidir. Əsma axınında hissə yoxdur.
 *
 * ⚠️ Tam ekran **`Dialog`**-dur (CLAUDE.md qaydası): ekran modal vərəqdən açılır, inline emit
 * ediləndə həmin vərəqin pəncərəsinin altında qalıb səssizcə heç nə etmiş kimi görünərdi.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ExcerptPickerScaffold(
    title: String,
    data: ExcerptSourceData,
    /** `true` → dua axını: say sahəsi və hissələr. Əsmada dəlil tək sətirdir və sayı yoxdur. */
    allowParts: Boolean,
    /** Mənbədəki `{…}` zikrləri — boşdursa çip sırası çəkilmir. */
    segments: List<DhikrSegment>,
    /** Bu mənbədən artıq yazılanlar — bloklarda solğun vurğu və 3-cü addımın altındakı siyahı. */
    savedExcerpts: List<SavedExcerpt>,
    isSaving: Boolean,
    missingTargetMessage: String,
    hasTarget: Boolean,
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
    // **Yadda saxlanacaq** hissələr — mənbə bloklarındakı canlı seçim yox, təsdiqlənmiş mətn.
    //
    // ⚠️ Vəziyyət **budaqlanmadan əvvəl** elan olunmalıdır: aşağıdakı `return@Dialog` hədəf
    // seçicisi açılanda bütün `Scaffold`-u kompozisiyadan çıxarır və içində olsaydı seçilmiş mətnlər
    // başlığı seçib qayıdanda itərdi.
    val parts = remember(data) { mutableStateListOf(DuaPartDraft()) }
    var activeIndex by remember(data) { mutableIntStateOf(0) }
    val active = activeIndex.coerceIn(0, parts.lastIndex)
    val current = parts[active]

    var choosingKey by remember { mutableStateOf<String?>(null) }

    /** Hansı hədəf üçün «yeni» forması açıqdır — «+» düyməsi bunu qoyur. */
    var addingKey by remember { mutableStateOf<String?>(null) }

    // ⚠️ Sürüşmə vəziyyəti də **budaqlanmadan əvvəl**: `rememberScrollState()` seçicinin
    // altağacında olsaydı unudulardı və başlıq seçilib qayıdanda ekran ən yuxarıya tullanardı
    // (istifadəçinin «başlığı seçəndə lap yuxarıya qalxır» şikayəti məhz bu idi).
    val contentScroll = rememberScrollState()

    // Əlavə etmə ekranında da iki/üç barmaqla ölçüləndirmə: seçiləcək parça bəzən uzun ərəbcə
    // rəvayətdir və onu barmaqla böyütmədən oxumaq çətindir. Açarlar oxuma ekranı ilə ortaqdır.
    val zoomScope = rememberCoroutineScope()
    var zoomFeedback by remember { mutableStateOf<ReaderZoomFeedback?>(null) }
    val zoomModifier = Modifier.readerTextZoom(
        enabled = AppPreferences.observeReaderPinchZoomEnabled(),
        arabicMultiplier = DuaPreferences.observeArabicSizeMultiplier(),
        translationMultiplier = DuaPreferences.observeTranslationSizeMultiplier(),
        minMultiplier = ReaderTextZoom.HADITH_MIN,
        maxMultiplier = ReaderTextZoom.HADITH_MAX,
        onZoom = { target, value ->
            zoomFeedback = ReaderZoomFeedback(target, value)
            zoomScope.launch {
                when (target) {
                    ReaderZoomTarget.Arabic -> DuaPreferences.setArabicSizeMultiplier(value)
                    ReaderZoomTarget.Translation ->
                        DuaPreferences.setTranslationSizeMultiplier(value)
                }
            }
        },
    )

    /** Aktiv hissəni dəyişir — indeks çağırış anında oxunur, kompozisiya anında yox. */
    fun updateActive(transform: (DuaPartDraft) -> DuaPartDraft) {
        val index = activeIndex.coerceIn(0, parts.lastIndex)
        parts[index] = transform(parts[index])
    }

    val arabicSlot = ExcerptSlot(
        label = stringResource(Res.string.duaPickerArabicLabel),
        value = current.arabic,
        arabic = true,
        icon = Res.drawable.dr_icon_quran_script,
        assign = { text -> updateActive { it.copy(arabic = text) } },
    )
    val translitSlot = ExcerptSlot(
        label = stringResource(Res.string.duaTransliterationOptional),
        value = current.transliteration,
        arabic = false,
        icon = Res.drawable.dr_icon_translations,
        assign = { text -> updateActive { it.copy(transliteration = text) } },
    )
    val translationSlot = ExcerptSlot(
        label = stringResource(Res.string.duaTranslationOptional),
        value = current.translation,
        arabic = false,
        icon = Res.drawable.dr_icon_footnote,
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

    /** Yazılacaq hissələr, ya da `null` (səbəb toast ilə deyilir, problemli hissə aktiv olur). */
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
                null
            }

            else -> validation.kept
        }
    }

    BackHandler(enabled = choosingKey != null || addingKey != null) {
        choosingKey = null
        addingKey = null
    }

    val adding = targets.firstOrNull { it.key == addingKey }
    if (adding != null) {
        NewTargetScreen(spec = adding, onDone = { addingKey = null })
        return@Dialog
    }

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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // «Davam et» ikinci dərəcəlidir (outlined): adi hal bir dua yazıb çıxmaqdır,
                        // bir hədisdən bir neçə zikr götürmək isə ehtiyac olanda.
                        onSaveAndContinue?.let { saveAndContinue ->
                            OutlinedButton(
                                onClick = {
                                    checkedParts()?.let { kept ->
                                        saveAndContinue(kept) {
                                            parts.clear()
                                            parts.add(DuaPartDraft())
                                            activeIndex = 0
                                        }
                                    }
                                },
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
                            onClick = { checkedParts()?.let(onSave) },
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
        },
    ) { paddingValues ->
      Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(zoomModifier)
                .verticalScroll(contentScroll),
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

                    val partsReady = validation.problem == null

                    SectionLabel(
                        step = 1,
                        text = stringResource(Res.string.duaPickerSourceSection),
                        done = partsReady,
                    )

                    Text(
                        text = stringResource(Res.string.duaPickerHint),
                        style = typography.bodySmall.withScriptDirection(arabic = false),
                        color = colorScheme.onSurfaceVariant.alpha(0.75f),
                    )

                    if (parts.size > 1) {
                        ActivePartSelector(
                            parts = parts,
                            active = active,
                            onSelect = { activeIndex = it },
                        )
                    }

                    if (segments.isNotEmpty()) {
                        SegmentChips(
                            segments = segments,
                            isSaved = { segment ->
                                savedArabic.any { sameArabic(it, segment.arabic) }
                            },
                            onPick = { segment ->
                                // Çip «bu zikr» deməkdir: üç sahə birlikdə dəyişir, yoxsa əvvəlki
                                // zikrin oxunuşu yeni zikrin ərəbcəsinin yanında qalardı. Say isə
                                // zikrin deyil, istifadəçinin seçimidir — toxunulmur.
                                updateActive {
                                    it.copy(
                                        arabic = segment.arabic,
                                        transliteration = segment.transliteration.orEmpty(),
                                        translation = segment.translation.orEmpty(),
                                    )
                                }
                            },
                        )
                    }

                    SourceBlock(
                        label = arabicSlot.label,
                        text = data.fullArabic,
                        arabic = true,
                        slots = listOf(arabicSlot),
                        saved = savedArabic,
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
                            saved = savedLatin,
                        )
                    }

                    // Qeyd yalnız mənbədə varsa görünür — boş blok ekranı uzadardı.
                    if (!data.fullNote.isNullOrBlank()) {
                        SourceBlock(
                            label = stringResource(Res.string.duaPickerNoteLabel),
                            text = data.fullNote,
                            arabic = false,
                            slots = latinSlots,
                            saved = savedLatin,
                            hint = stringResource(Res.string.duaPickerNoteHint),
                        )
                    }

                    HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.4f))

                    SectionLabel(
                        step = 2,
                        text = stringResource(Res.string.duaPickerResultSection),
                        done = partsReady,
                    )

                    Text(
                        text = stringResource(Res.string.duaManualHint),
                        style = typography.bodySmall.withScriptDirection(arabic = false),
                        color = colorScheme.onSurfaceVariant.alpha(0.75f),
                    )

                    val fields: @Composable () -> Unit = {
                        ResultField(arabicSlot)
                        ResultField(translitSlot)
                        ResultField(translationSlot)

                        if (allowParts) {
                            RepeatCountField(
                                value = current.countText,
                                onValueChange = { text -> updateActive { it.copy(countText = text) } },
                            )
                        }
                    }

                    if (parts.size == 1) {
                        // Tək hissə — əvvəlki görkəm: kart da, «Hissə 1» də yoxdur.
                        fields()
                    } else {
                        parts.forEachIndexed { index, part ->
                            if (index == active) {
                                PartCard(
                                    index = index,
                                    onRemove = {
                                        parts.removeAt(index)
                                        activeIndex = index.coerceAtMost(parts.lastIndex)
                                    },
                                    content = fields,
                                )
                            } else {
                                PartSummaryRow(
                                    index = index,
                                    part = part,
                                    onClick = { activeIndex = index },
                                    onRemove = {
                                        parts.removeAt(index)
                                        // Aktiv hissə yerində qalsın: silinən ondan əvvəldirsə
                                        // indeks bir vahid sürüşür.
                                        if (index < activeIndex) activeIndex -= 1
                                    },
                                )
                            }
                        }
                    }

                    if (allowParts) {
                        // Hədd bazadadır (`part_no` 1..5) — düymə ondan sonra ümumiyyətlə çəkilmir,
                        // basılıb heç nə etməsin deyə.
                        if (parts.size < MAX_DUA_PARTS) {
                            TextButton(
                                onClick = {
                                    parts.add(DuaPartDraft())
                                    activeIndex = parts.lastIndex
                                },
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.dr_icon_add),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(Res.string.duaPickerAddPart))
                            }
                        }

                        Text(
                            text = stringResource(Res.string.duaPickerPartsHint),
                            style = typography.bodySmall.withScriptDirection(arabic = false),
                            color = colorScheme.onSurfaceVariant.alpha(0.75f),
                        )
                    }

                    HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.4f))

                    SectionLabel(
                        step = 3,
                        text = stringResource(Res.string.duaPickerTargetSection),
                        done = hasTarget,
                    )

                    // Başlıq və alt başlıq **bir qutudadır**: onlar bir hədəfin iki pilləsidir,
                    // ayrı-ayrı kartlar isə onları bir-birindən asılı olmayan seçim kimi
                    // göstərirdi. Yeni element «+» ilə elə buradan əlavə olunur — əvvəl bunun üçün
                    // seçim siyahısını açıb oradakı formanı tapmaq lazım gəlirdi.
                    Surface(
                        color = colorScheme.surfaceContainerHigh.alpha(0.6f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            targets.forEachIndexed { index, spec ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        color = colorScheme.outlineVariant.alpha(0.3f),
                                        modifier = Modifier.padding(start = 16.dp),
                                    )
                                }

                                TargetRow(
                                    spec = spec,
                                    onClick = { if (spec.enabled) choosingKey = spec.key },
                                    onAddNew = if (spec.allowNew) {
                                        { addingKey = spec.key }
                                    } else {
                                        null
                                    },
                                )
                            }
                        }
                    }

                    SavedFromSourceList(savedExcerpts)

                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        ReaderZoomFeedbackOverlay(zoomFeedback) { zoomFeedback = null }
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
 * vurğunu çəkmədiyi üçün həmin aralığı [excerptHighlights] özü boyayır — «seçdim, amma ekranda heç
 * nə görünmür» halı qalmasın.
 */
@Composable
private fun SourceBlock(
    label: String,
    text: String,
    arabic: Boolean,
    slots: List<ExcerptSlot>,
    /** Bu mənbədən artıq yazılmış parçalar — blokda solğun fonla görünür. */
    saved: List<String>,
    hint: String? = null,
) {
    // Mətn dəyişmir (`readOnly`), dəyişən yalnız seçimdir — ona görə hər dəyişiklikdə mətn
    // parametrdən geri qoyulur.
    var field by remember(text) { mutableStateOf(TextFieldValue(text)) }
    var picked by remember(text) { mutableStateOf<TextRange?>(null) }
    var focused by remember(text) { mutableStateOf(false) }

    val braces = remember(text) { braceRanges(text) }

    // Artıq yazılmış parçaların yeri — tapılmayan (mənbə sonradan redaktə olunub) sadəcə boyanmır.
    val savedRanges = remember(text, saved) {
        saved.mapNotNull { excerpt -> excerptMatchRange(text, excerpt) }
    }

    /** Cari seçim hansı mötərizədir (0-dan), heç biri deyilsə -1 — düymə etiketi üçün. */
    val braceIndex = picked?.let { range -> braces.indexOf(range.min until range.max) } ?: -1

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
    val savedHighlight = colorScheme.tertiary.alpha(0.14f)

    // Ölçü oxuma ekranı ilə **eyni açarlardan** gəlir: parçanı seçən adam onu sonra necə görəcəksə,
    // elə o ölçüdə seçməlidir. İki/üç barmaqlı jest ekranın sürüşən sütununa qoşulub.
    val sizeMult = if (arabic) {
        DuaPreferences.observeArabicSizeMultiplier()
    } else {
        DuaPreferences.observeTranslationSizeMultiplier()
    }
    val textStyle = typography.bodyLarge
        .copy(fontSize = typography.bodyLarge.fontSize * sizeMult)
        .withLineHeightRatio(if (arabic) ARABIC_EXCERPT_LINE_HEIGHT_RATIO else TRANSLATION_LINE_HEIGHT_RATIO)
        .withScriptDirection(
            arabic = arabic,
            arabicFontFamily = if (arabic) arabicFontFamily() else null,
        )

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
                // Bir hədisdə bir neçə zikr ola bilər — hər basış **növbəti** mötərizəyə keçir.
                if (braces.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            val current = picked?.let { it.min until it.max }
                            nextBraceRange(braces, current)?.let { next ->
                                select(TextRange(next.first, next.last + 1))
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = if (braces.size > 1 && braceIndex >= 0) {
                                stringResource(
                                    Res.string.duaPickerSelectBracesIndexed,
                                    braceIndex + 1,
                                    braces.size,
                                )
                            } else {
                                stringResource(Res.string.duaPickerSelectBraces)
                            },
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
                visualTransformation = excerptHighlights(
                    saved = savedRanges,
                    savedColor = savedHighlight,
                    pick = picked.takeIf { !focused },
                    pickColor = highlight,
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
 * Blokdakı vurğular: bu mənbədən **artıq yazılmış** parçalar solğun ([saved]), cari seçim isə tünd
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

/**
 * «Başlıq / Alt başlıq / Ad» sətri — basılanda hədəf seçimi addımını açır.
 *
 * Fon **yoxdur**: sətir ortaq qutunun içindədir, ona görə sıradan çıxmış hədəf yalnız məzmununu
 * solğunlaşdırır.
 */
@Composable
private fun TargetRow(
    spec: TargetSpec,
    onClick: () -> Unit,
    /** `null` → bu hədəfə yeni element əlavə etmək olmur, «+» düyməsi çəkilmir. */
    onAddNew: (() -> Unit)?,
) {
    val chosen = spec.options.firstOrNull { it.id == spec.selectedId }
    val value = chosen?.title
        ?: spec.newName.takeIf { it.isNotBlank() }
        ?: stringResource(
            if (spec.optional) Res.string.duaNoSubtitle else Res.string.duaPickerNothingSelected,
        )

    val contentAlpha = if (spec.enabled) 1f else 0.4f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = spec.enabled, onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = spec.label,
                style = typography.labelSmall.withScriptDirection(arabic = false),
                color = colorScheme.onSurfaceVariant.alpha(0.75f * contentAlpha),
            )
            Text(
                text = value,
                style = typography.bodyLarge.withScriptDirection(arabic = false),
                color = if (chosen != null || spec.newName.isNotBlank()) {
                    colorScheme.onSurface.alpha(contentAlpha)
                } else {
                    colorScheme.onSurfaceVariant.alpha(0.7f * contentAlpha)
                },
            )
            chosen?.arabic?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = typography.bodyMedium.withScriptDirection(
                        arabic = true,
                        arabicFontFamily = arabicFontFamily(),
                    ),
                    color = colorScheme.onSurfaceVariant.alpha(0.8f * contentAlpha),
                )
            }
        }

        onAddNew?.let { addNew ->
            IconButton(onClick = addNew, enabled = spec.enabled) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_add),
                    contentDescription = stringResource(Res.string.duaPickerNewTitle),
                    tint = colorScheme.primary.alpha(contentAlpha),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Icon(
            painter = painterResource(Res.drawable.dr_icon_chevron_right),
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant.alpha(0.6f * contentAlpha),
            modifier = Modifier.padding(end = 12.dp).size(20.dp),
        )
    }
}

/**
 * Hədəf seçimi addımı — sadə siyahı.
 *
 * Axtarış sahəsi **qəsdən yoxdur**: siyahı başlıqlar üçün bir neçə sətirdir, 99 ad isə onsuz da
 * nömrə sırası ilə düzülüb — axtarış sahəsi yalnız ekranın başını tuturdu. Yeni element əlavə etmək
 * də buradan çıxarıldı: onun yeri əsas ekrandakı «+» düyməsidir ([NewTargetScreen]).
 *
 * Siyahı `LazyColumn`-dur və **öz** sürüşməsi var.
 */
@Composable
private fun TargetChooser(spec: TargetSpec, onDone: () -> Unit) {
    Scaffold(
        topBar = { AppBar(title = spec.label, onBack = onDone) },
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
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

                items(spec.options, key = { it.id }) { option ->
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

/**
 * Yeni başlıq/alt başlıq ekranı — əsas ekrandakı «+» düyməsi açır.
 *
 * Əvvəl bu, seçim siyahısının başında oturan bir blok idi: yeni başlıq yaratmaq üçün əvvəlcə seçim
 * ekranını açmaq, sonra orada formanı tapmaq lazım gəlirdi. İndi öz ekranıdır və öz `Scaffold`-u
 * var — seçici artıq yalnız siyahıdır.
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
