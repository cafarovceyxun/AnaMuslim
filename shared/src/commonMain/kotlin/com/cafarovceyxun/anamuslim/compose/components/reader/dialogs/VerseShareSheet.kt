package com.cafarovceyxun.anamuslim.compose.components.reader.dialogs

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.ui.text.style.TextOverflow
import com.cafarovceyxun.anamuslim.repository.supabase.TranslationReviewRepository
import com.cafarovceyxun.anamuslim.compose.components.settings.withContentDirection
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.copiedToClipboard
import com.cafarovceyxun.anamuslim.resources.includeBookmarkNote
import com.cafarovceyxun.anamuslim.resources.sharePairPerVerse
import com.cafarovceyxun.anamuslim.resources.strHintFromVerse
import com.cafarovceyxun.anamuslim.resources.strHintToVerse
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelCurrentVerse
import com.cafarovceyxun.anamuslim.resources.strLabelIncludeArabic
import com.cafarovceyxun.anamuslim.resources.strLabelSelectTranslations
import com.cafarovceyxun.anamuslim.resources.strLabelSelectVerse
import com.cafarovceyxun.anamuslim.resources.strLabelVerseRange
import com.cafarovceyxun.anamuslim.resources.strMsgEnterValidRange
import com.cafarovceyxun.anamuslim.resources.strMsgShareRange
import com.cafarovceyxun.anamuslim.resources.strTitleNote
import com.cafarovceyxun.anamuslim.resources.strTitleShareVerse
import org.jetbrains.compose.resources.getString
import com.cafarovceyxun.anamuslim.api.models.translation.TranslationBookInfoModel
import com.cafarovceyxun.anamuslim.components.quran.subcomponents.Translation
import com.cafarovceyxun.anamuslim.compose.components.common.Chip
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheetHeader
import com.cafarovceyxun.anamuslim.compose.components.share.ShareImageSegment
import com.cafarovceyxun.anamuslim.compose.components.share.ShareWaysRow
import com.cafarovceyxun.anamuslim.compose.screens.hadith.arabicQuranReference
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.db.relations.VerseWithDetails
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptUtils
import com.cafarovceyxun.anamuslim.utils.reader.findFootnoteMarkers
import com.cafarovceyxun.anamuslim.utils.reader.normalizeFootnoteMarkers
import com.cafarovceyxun.anamuslim.utils.reader.factory.QuranTranslationFactory
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.utils.univ.StringUtils
import kotlinx.coroutines.launch

/** Haşiyə istinadları paylaşılan mətndən tamamilə çıxarılır. */
private val footnoteTagPattern = Regex("""(?s)<fn.*?>(.*?)<.*?fn>""")

/** Surələrin ərəbcə adının saxlandığı dil kodu. */
private const val ARABIC_LANG_CODE = "ar"

/** Tərcümənin əvvəlindəki ayə nömrəsi nişanı, məs. «-2: ». Yalnız şəkil mətnindən silinir. */
private val versePrefixPattern = Regex("""^\s*-?\d+\s*[:.]\s*""")

private data class VerseShareState(
    val useVerseRange: Boolean = false,
    val fromVerseText: String = "",
    val toVerseText: String = "",
    /** Aralıq rejimində hər ayəni öz tərcüməsi ilə cütləyir (ərəbcə → tərcümə → ərəbcə → …). */
    val pairPerVerse: Boolean = false,
    val includeArabic: Boolean = true,
    val includeBookmarkNote: Boolean = false,
    val selectedSlugs: Set<String> = setOf(),
)

private typealias UpdateVerseShareState = (VerseShareState.() -> VerseShareState) -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerseShareSheet(
    vwd: VerseWithDetails?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (vwd == null) return

    val coroutineScope = rememberCoroutineScope()
    val translFactory = QuranTranslationFactory.remember()

    // «Yoxlanış» kitabı ([TranslationReviewRepository.BOOK_SLUG]) paylaşılmır: o, hələ təsdiqlənməmiş
    // təkliflərdir və oxucuda yalnız müqayisə üçün durur — paylaşılan ayə yalnız qəbul olunmuş
    // tərcümədən getməlidir (istifadəçi tələbi, 2026-10-24).
    val translationBooks = remember(translFactory) {
        translFactory.getAvailableTranslationBooksInfo()
            .filterKeys { it != TranslationReviewRepository.BOOK_SLUG }
    }

    var state by remember { mutableStateOf(VerseShareState()) }
    var showImageEditor by remember { mutableStateOf(false) }

    var imageSegments by remember { mutableStateOf(emptyList<ShareImageSegment>()) }
    // Şəkildə sürə adı və ayə nömrəsi öz sətrindədir, tərcümənin sonuna yapışdırılmır.
    var imageReference by remember { mutableStateOf("") }
    // Paylaşılacaq qeyd: seçilmiş tərcümənin haşiyəsi + əlfəcin qeydi (bax [shareNoteOf]).
    // Keçiddən **asılı olmadan** oxunur: vərəq keçidi yalnız qeyd həqiqətən olanda göstərir.
    var availableNote by remember { mutableStateOf<String?>(null) }

    val updateState: UpdateVerseShareState = { transform ->
        state = state.transform()
    }

    val (fromVerse, toVerse) = resolveVerseRange(state, vwd.verseNo)

    // Üç yol **daimi** görünür (hədis vərəqi ilə eyni, 2026-10-24) — əvvəl ayrıca «Paylaş»
    // düyməsinin arxasında idi. Hər yol basılan anda vərəqin cari halını oxuyur; paylaşacaq mətn
    // yoxdursa (aralıq səhvdir, nə ərəbcə, nə tərcümə seçilib) xanalar sönür.
    val hasContent = fromVerse != null && toVerse != null &&
        vwd.chapter.isVerseRangeValid(fromVerse, toVerse) &&
        (state.includeArabic || state.selectedSlugs.isNotEmpty())

    // `pairPerVerse` də açardır: cütləmə seçimi şəkildə də görünməlidir, yoxsa vərəqdəki keçid
    // yalnız mətn paylaşımını dəyişərdi.
    LaunchedEffect(fromVerse, toVerse, state.selectedSlugs, state.includeArabic, state.pairPerVerse) {
        if (fromVerse == null || toVerse == null || !vwd.chapter.isVerseRangeValid(fromVerse, toVerse)) {
            return@LaunchedEffect
        }

        val repository = RepositoryProvider.quranRepository
        val singleVerse = fromVerse == toVerse

        val verses = (fromVerse..toVerse).map { vNo ->
            val arabic = if (state.includeArabic) {
                repository.getWordsForAyah(vwd.chapterNo, vNo, QuranScriptUtils.SCRIPT_UTHMANI)
                    .joinToString(" ") { it.text }
            } else ""

            val translation = translFactory
                .getTranslationsSingleVerse(state.selectedSlugs, vwd.chapterNo, vNo)
                .firstOrNull()
                ?.let { cleanHtml(it.text) }
                // Ayə öz seqmentindədirsə «-2: » nömrə prefiksi lazımsızdır — hansı ayə olduğu
                // onsuz da sıradan və istinad sətrindən bilinir. Birləşdirilmiş bloklu rejimdə isə
                // aralıqda saxlanır, çünki orada ayələri yalnız o ayırır.
                ?.let { if (singleVerse || state.pairPerVerse) versePrefixPattern.replace(it, "") else it }
                .orEmpty()

            ShareImageSegment(arabic = arabic, translation = translation)
        }

        imageSegments = if (state.pairPerVerse) {
            verses
        } else {
            listOf(
                ShareImageSegment(
                    arabic = verses.joinToString("  ") { it.arabic }.trim(),
                    translation = verses.joinToString(" ") { it.translation }.trim(),
                )
            )
        }
        imageReference = verseReference(vwd, fromVerse, toVerse)
    }

    // Qeyd ayrı effektdədir: `includeBookmarkNote` keçidi mətn bloklarını yenidən qurmamalıdır.
    //
    // ⚠️ «Qeydi də daxil et» əvvəl yalnız **əlfəcin** qeydini götürürdü, istifadəçi isə oxucuda
    // gördüyü tərcümə haşiyəsini («(1) - Təqva sahibi…») gözləyirdi — keçid basılırdı, heç nə
    // düşmürdü (2026-10-24). İndi hər ikisi daxildir və keçid yalnız qeyd olanda görünür.
    LaunchedEffect(vwd.chapterNo, fromVerse, toVerse, state.selectedSlugs) {
        availableNote = if (fromVerse == null || toVerse == null ||
            !vwd.chapter.isVerseRangeValid(fromVerse, toVerse)
        ) {
            null
        } else {
            shareNoteOf(translFactory, state.selectedSlugs, vwd.chapterNo, fromVerse, toVerse)
        }
    }

    val shareNote = availableNote?.takeIf { state.includeBookmarkNote }

    // Redaktor öz tam ekran `Dialog` pəncərəsindədir, ona görə vərəq **altda kompozisiyada qalır**.
    // Əvvəl burada `return` vardı: vərəq kompozisiyadan çıxırdı, geri qayıdanda aşağıdakı
    // `LaunchedEffect`-lər yenidən işləyib bütün seçimləri (ayə aralığı, cütləmə, seçilmiş
    // tərcümələr) defolta qaytarırdı — istifadəçi hər dəfə sıfırdan başlayırdı.
    if (showImageEditor) {
        QuranImageEditorScreen(
            segments = imageSegments,
            reference = imageReference,
            note = shareNote,
            includeArabic = state.includeArabic,
            includeAzerbaijani = state.selectedSlugs.isNotEmpty(),
            onBack = { showImageEditor = false },
        )
    }

    LaunchedEffect(vwd) {
        updateState { VerseShareState() }
    }

    LaunchedEffect(translationBooks) {
        val first = translationBooks.keys.firstOrNull()

        updateState {
            copy(
                selectedSlugs = if (first != null) setOf(first) else emptySet()
            )
        }
    }

    val clipboardMsg = stringResource(Res.string.copiedToClipboard)
    val shareTitle = stringResource(Res.string.strTitleShareVerse)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = colorScheme.scrim.alpha(0.5f),
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Top) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            BottomSheetHeader(
                title = stringResource(Res.string.strTitleShareVerse),
                hasDragHandle = true,
            )

            Column(
                modifier = Modifier
                    .weight(1f, false)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                AdvancedShareForm(
                    vwd = vwd,
                    translationBooks = translationBooks,
                    note = availableNote,
                    state,
                    updateState = updateState
                )
            }

            ShareWaysRow(
                onShareAsText = {
                    coroutineScope.launch {
                        val text = buildShareText(
                            translFactory = translFactory,
                            state = state,
                            vwd = vwd,
                            note = shareNote,
                        )

                        if (text.isNullOrEmpty()) return@launch

                        PlatformUtils.shareText(text, shareTitle)

                        onDismiss()
                    }
                },
                onShareAsImage = { showImageEditor = true },
                onCopyText = {
                    coroutineScope.launch {
                        val text = buildShareText(
                            translFactory,
                            state = state,
                            vwd = vwd,
                            note = shareNote,
                        )

                        if (text.isNullOrEmpty()) return@launch

                        PlatformUtils.copyToClipboard(text)

                        PlatformUtils.showClipboardMessage(clipboardMsg)

                        // `onDismiss()` **mütləq burada** olmalıdır. Çöldə çağırılanda vərəq
                        // dərhal bağlanır, kompozisiya dispose olunur və
                        // `rememberCoroutineScope` ləğv edilir — `buildShareText` isə hər ayə
                        // üçün ayrıca DB oxuyur, yəni hər ayə bir ləğv nöqtəsidir. Tək ayə çox
                        // vaxt yarışı udurdu, **ayə aralığı isə yarımçıq kəsilirdi və heç nə
                        // kopyalanmırdı**.
                        onDismiss()
                    }
                },
                enabled = hasContent,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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

@Composable
private fun AdvancedShareForm(
    vwd: VerseWithDetails,
    translationBooks: Map<String, TranslationBookInfoModel>,
    /** Paylaşılan aralığın qeydi (bax [shareNoteOf]); `null` olanda keçid göstərilmir. */
    note: String?,
    state: VerseShareState,
    updateState: UpdateVerseShareState,
) {
    Text(
        text = stringResource(Res.string.strLabelSelectVerse),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        color = colorScheme.primary,
        style = typography.titleSmall,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
    ) {
        Chip(
            selected = !state.useVerseRange,
            label = { Text(stringResource(Res.string.strLabelCurrentVerse)) },
        ) {
            updateState {
                copy(useVerseRange = false)
            }
        }
        Chip(
            selected = state.useVerseRange,
            label = { Text(stringResource(Res.string.strLabelVerseRange)) },
        ) {
            updateState {
                copy(useVerseRange = true)
            }
        }
    }

    if (state.useVerseRange) {
        Text(
            text = stringResource(
                Res.string.strMsgShareRange,
                1,
                vwd.chapter.surah.ayahCount
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            style = typography.bodyMedium,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.fromVerseText,
                onValueChange = {
                    updateState {
                        copy(fromVerseText = it)
                    }
                },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(Res.string.strHintFromVerse)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = state.toVerseText,
                onValueChange = {
                    updateState {
                        copy(toVerseText = it)
                    }
                },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(Res.string.strHintToVerse)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }

        // Yalnız aralıqda mənası var — tək ayədə hər iki rejim eyni mətni verir.
        CheckboxRow(
            checked = state.pairPerVerse,
            onCheckedChange = {
                updateState {
                    copy(pairPerVerse = it)
                }
            },
            label = stringResource(Res.string.sharePairPerVerse),
        )
    }

    CheckboxRow(
        checked = state.includeArabic,
        onCheckedChange = {
            updateState {
                copy(includeArabic = it)
            }
        },
        label = stringResource(Res.string.strLabelIncludeArabic),
    )

    if (note != null) {
        CheckboxRow(
            checked = state.includeBookmarkNote,
            onCheckedChange = {
                updateState {
                    copy(includeBookmarkNote = it)
                }
            },
            label = stringResource(Res.string.includeBookmarkNote),
            description = note,
        )
    }

    Text(
        text = stringResource(Res.string.strLabelSelectTranslations),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 8.dp),
        color = colorScheme.primary,
        style = typography.titleSmall,
    )

    translationBooks.forEach { (slug, book) ->
        val checked = slug in state.selectedSlugs


        CheckboxRow(
            checked,
            onCheckedChange = {
                updateState {
                    copy(
                        selectedSlugs = if (it) selectedSlugs + slug else selectedSlugs - slug,
                    )
                }
            },
            label = book.getDisplayName(true),
        )
    }
}

@Composable
private fun CheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    /** Etiketin altındakı solğun sətir — keçidin nəyi əlavə edəcəyini göstərir. */
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
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
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

private fun resolveVerseRange(
    state: VerseShareState,
    defaultVerse: Int,
): Pair<Int?, Int?> {
    if (!state.useVerseRange) {
        return defaultVerse to defaultVerse
    }

    val from = state.fromVerseText.trim().toIntOrNull()
    val to = state.toVerseText.trim().toIntOrNull()

    if (from == null || to == null) return null to null

    return from to to
}

private suspend fun buildShareText(
    translFactory: QuranTranslationFactory,
    state: VerseShareState,
    vwd: VerseWithDetails,
    /** Vərəqin hazırladığı qeyd ([shareNoteOf]); keçid sönülüdürsə `null`. */
    note: String?,
): String? {
    val proceed = state.selectedSlugs.isNotEmpty() || state.includeArabic
    if (!proceed) return null

    val (fromVerse, toVerse) = resolveVerseRange(state, vwd.verseNo)
    if (fromVerse == null || toVerse == null || !vwd.chapter.isVerseRangeValid(fromVerse, toVerse)) {
        PlatformUtils.showLongToast(getString(Res.string.strMsgEnterValidRange))
        return null
    }

    val repository = RepositoryProvider.quranRepository
    val sb = StringBuilder()

    // Ərəbcə blok öz istinadını ərəbcə daşıyır — ad da, rəqəmlər də (məs. «الفاتحة ١:٧»); aşağıdakı
    // etiketsiz istinad isə tərcümənindir. Ad bazada yoxdursa sətir tamam buraxılır.
    val arabicChapterName = vwd.chapter.localizations
        .firstOrNull { it.langCode == ARABIC_LANG_CODE && !it.name.isNullOrBlank() }
        ?.name

    fun arabicReferenceOf(from: Int, to: Int): String? =
        arabicChapterName?.let { arabicQuranReference(it, vwd.chapterNo, from, to) }

    suspend fun arabicOf(vNo: Int): String =
        repository.getWordsForAyah(vwd.chapterNo, vNo, QuranScriptUtils.SCRIPT_UTHMANI)
            .joinToString(" ") { it.text }

    suspend fun translationOf(vNo: Int): String? =
        translFactory.getTranslationsSingleVerse(state.selectedSlugs, vwd.chapterNo, vNo)
            .firstOrNull()
            ?.let { cleanHtml(it.text) }

    if (state.pairPerVerse) {
        // Ayə-ayə rejim: hər ayə öz tərcüməsi ilə yan-yana, ayələr arasında boş sətir.
        for (vNo in fromVerse..toVerse) {
            if (state.includeArabic) {
                sb.append(arabicOf(vNo)).append("\n")
                arabicReferenceOf(vNo, vNo)?.let { sb.append(it).append("\n") }
            }
            translationOf(vNo)?.let { sb.append(it).append("\n") }
            sb.append("\n")
        }
    } else {
        // Bloklu rejim: əvvəlcə bütün ərəbcə, sonra bütün tərcümə.
        if (state.includeArabic) {
            for (vNo in fromVerse..toVerse) {
                sb.append(arabicOf(vNo)).append("  ")
            }
            // Şərtsiz `setLength(sb.length - 2)` heç bir ayə söz qaytarmayanda mənfi uzunluq verirdi.
            if (sb.length >= 2) sb.setLength(sb.length - 2)
            arabicReferenceOf(fromVerse, toVerse)?.let { sb.append("\n").append(it) }
            sb.append("\n\n")
        }

        for (vNo in fromVerse..toVerse) {
            translationOf(vNo)?.let { sb.append(it).append(" ") }
        }
    }

    // `StringBuilder.trimEnd()` yeni `CharSequence` qaytarır, buferi dəyişmir — nəticəsi atılan
    // çağırış idi, ona görə istinad sətrindən əvvəl artıq boşluq qalırdı.
    while (sb.isNotEmpty() && (sb.last() == ' ' || sb.last() == '\n')) sb.setLength(sb.length - 1)

    // Qeyd vərəqdə bir dəfə hazırlanır ([shareNoteOf]) ki, mətn və şəkil eyni qeydi daşısın.
    if (!note.isNullOrEmpty()) {
        val noteLabel = getString(Res.string.strTitleNote)
        sb.append("\n\n").append("$noteLabel: ").append(note)
    }

    // İstinad sətri etiketsizdir: «Qaynaq:» sözü paylaşılan mətndə yalnız yer tuturdu, ayənin
    // ünvanı onsuz da özü ilə oxunur.
    sb.append("\n\n").append(verseReference(vwd, fromVerse, toVerse))

    return sb.toString()
}

/**
 * Paylaşılan aralığın qeydi: seçilmiş tərcümənin haşiyəsi (oxucuda ayənin altında görünən
 * «(1) - …» sətri) və əlfəcin qeydi, boş sətirlə ayrılmış. Heç biri yoxdursa `null`.
 *
 * Haşiyə mətnlə **eyni** tərcümədən götürülür (`firstOrNull()`, bax [buildShareText]) — başqa
 * kitabın haşiyəsi paylaşılan mətndəki «(1)» işarəsinə uyğun gəlməzdi. Aralıqda hər haşiyənin
 * önündə ayə nömrəsi durur, çünki işarələr («(1)») hər ayədə yenidən başlayır.
 *
 * Əlfəcinlər (sürə, ilk ayə, son ayə) üçlüyü ilə saxlanılır, ona görə axtarış məhz paylaşılan
 * aralıq üçündür — 2:1 əlfəcini 2:1-3 paylaşımına qoşulmur.
 */
private suspend fun shareNoteOf(
    translFactory: QuranTranslationFactory,
    slugs: Set<String>,
    chapterNo: Int,
    fromVerse: Int,
    toVerse: Int,
): String? {
    val singleVerse = fromVerse == toVerse

    val translationNotes = (fromVerse..toVerse).mapNotNull { vNo ->
        val note = translFactory.getTranslationsSingleVerse(slugs, chapterNo, vNo)
            .firstOrNull()
            ?.note
            ?.let { cleanHtml(it).trim() }
            ?.takeIf { it.isNotEmpty() }
            ?: return@mapNotNull null

        if (singleVerse) note else "$vNo: $note"
    }

    val bookmarkNote = RepositoryProvider.userRepository
        .getBookmark(chapterNo, fromVerse, toVerse)
        ?.note
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

    return (translationNotes + listOfNotNull(bookmarkNote))
        .joinToString("\n\n")
        .takeIf { it.isNotEmpty() }
}

/** Ortaq istinad forması: «Fatihə 1:1-7» — həm mətndə, həm şəkil kartında. */
private fun verseReference(vwd: VerseWithDetails, fromVerse: Int, toVerse: Int): String {
    val range = if (fromVerse == toVerse) "$fromVerse" else "$fromVerse-$toVerse"
    return "${vwd.chapter.getCurrentName()} ${vwd.chapterNo}:$range"
}



private fun cleanHtml(string: String): String {
    val replaced = footnoteTagPattern.replace(string, "")

    return footnoteMarkersAsSuperscript(StringUtils.htmlToPlainText(replaced))
}

/**
 * Haşiyə nömrəsini Unicode üst-indeksə çevirir: `muttəqilər (1)` / `parçalanma1` → `muttəqilər¹` /
 * `parçalanma¹`. Oxucu nömrəni üslubla (span) kiçildir, paylaşılan düz mətndə isə üslub yoxdur —
 * çevirməsək `(1)` xam halda gedir. Hansı rəqəmin nömrə sayıldığını oxucu ilə eyni qayda təyin edir
 * (`findFootnoteMarkers`), ona görə `-2:` prefiksi, `2:255` istinadı və adi saylar toxunulmur.
 */
internal fun footnoteMarkersAsSuperscript(text: String): String {
    val normalized = normalizeFootnoteMarkers(text)
    val markers = findFootnoteMarkers(normalized)
    if (markers.isEmpty()) return normalized

    return buildString {
        var last = 0
        markers.forEach { range ->
            appendRange(normalized, last, range.first)
            range.forEach { i ->
                val c = normalized[i]
                append(if (c in '0'..'9') SUPERSCRIPT_DIGITS[c - '0'] else c)
            }
            last = range.last + 1
        }
        appendRange(normalized, last, normalized.length)
    }
}

private const val SUPERSCRIPT_DIGITS = "⁰¹²³⁴⁵⁶⁷⁸⁹"


