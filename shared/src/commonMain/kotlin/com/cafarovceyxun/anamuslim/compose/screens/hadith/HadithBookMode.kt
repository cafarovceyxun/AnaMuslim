package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheetHeader
import com.cafarovceyxun.anamuslim.compose.components.reader.IsVotd
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_edit
import com.cafarovceyxun.anamuslim.resources.dr_icon_heart_filled
import com.cafarovceyxun.anamuslim.resources.dr_icon_share
import com.cafarovceyxun.anamuslim.resources.hadithOptions
import com.cafarovceyxun.anamuslim.resources.ic_bookmark
import com.cafarovceyxun.anamuslim.resources.ic_bookmark_added
import com.cafarovceyxun.anamuslim.resources.strLabelBookmark
import com.cafarovceyxun.anamuslim.resources.strLabelEdit
import com.cafarovceyxun.anamuslim.resources.strLabelHadithNo
import com.cafarovceyxun.anamuslim.resources.strLabelShare
import com.cafarovceyxun.anamuslim.resources.strTitleNote
import com.cafarovceyxun.anamuslim.resources.strTitleVOTD
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences
import com.cafarovceyxun.anamuslim.utils.verse.HadithExcerpt
import com.cafarovceyxun.anamuslim.utils.supabase.DailyContent
import com.cafarovceyxun.anamuslim.viewModels.containsHadith
import com.cafarovceyxun.anamuslim.utils.supabase.Hadith
import com.cafarovceyxun.anamuslim.utils.supabase.HadithBook
import com.cafarovceyxun.anamuslim.utils.supabase.HadithChapter
import com.cafarovceyxun.anamuslim.utils.supabase.HadithSubChapter
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/*
 * Kitab rejimi — hədis oxucusunun kartsız düzülüşü.
 *
 * Adi rejimdə hər hədis öz kartındadır: çərçivə, nömrə nişanı, üstündə paylaş/əlfəcin/VOTD ikon
 * sırası. Uzun oxumada bu elementlər səhifəni doğrayır. Kitab rejimi eyni məzmunu davamlı mətn kimi
 * verir — nömrə mətnin öz içində işarədir, əməliyyatlar isə hədisə toxunanda açılan vərəqdədir
 * ([HadithOptionsSheet]).
 *
 * Rejim [com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences.BOOK_MODE]-dadır
 * və oxuma tablarından (qarışıq / ərəbcə / tərcümə) asılı deyil: tab hansı mətnin görünəcəyini,
 * bu isə yalnız düzülüşü təyin edir.
 */

/**
 * Kitab rejimində mətnin sağ-sol kənar boşluğu.
 *
 * Adi rejimdə kartlar ekranın kənarına dayanır (kartın öz 12dp daxili boşluğu var); kitab rejimində
 * çərçivə olmadığı üçün səhifə kənarını bu boşluq yaradır. Adi rejimin 12dp-sindən bir az geniş
 * saxlanılır ki, mətn kənarda «yapışmış» görünməsin — amma ondan artığı telefon eninə baha başa
 * gəlir: uzun ərəbcə sətir tez-tez qırılır və səhifə dar görünür.
 */
val BookModeMargin = 16.dp

/**
 * Kitab rejiminin şrift nərdivanı.
 *
 * Üç ölçü də **bab adından** çıxır: hədis mətni ondan 1sp böyük, qeyd və mənbə 1sp kiçikdir.
 * Kitabda tipoqrafiya demək olar bərabər olur — iyerarxiyanı ölçü fərqi yox, çəki və rəng daşıyır.
 *
 * Nərdivan bir yerdə durur ki, [HadithBookHeading] ilə [HadithBookEntry] ayrı-ayrı sürüşməsin:
 * ikisi ayrı composable-dır, ölçüləri əl ilə saxlansa biri dəyişəndə o biri geridə qalardı.
 * Hamısı istifadəçinin tərcümə mətni ölçüsü sürüşdürücüsünə (`azerbaijaniSizeMult`) bağlıdır.
 *
 * Ərəbcə mətn nərdivandan kənardadır — onun öz ölçü sürüşdürücüsü (`arabicSizeMult`) var.
 */
internal object BookModeType {
    /** Bab (və alt bab) adı — nərdivanın dayaq nöqtəsi. */
    fun heading(sizeMult: Float): TextUnit = (16f * sizeMult + 2f).sp

    /** Başlığın üstündəki solğun valideyn sətri (kitab / bab adı). */
    fun parentHeading(sizeMult: Float): TextUnit = (16f * sizeMult - 2f).coerceAtLeast(10f).sp

    /** Hədisin azərbaycanca mətni — bab adından 1sp böyük. */
    fun body(sizeMult: Float): TextUnit = (16f * sizeMult + 3f).sp

    /** Qeyd və mənbə — bab adından 1sp kiçik. */
    fun aside(sizeMult: Float): TextUnit = (16f * sizeMult + 1f).sp
}

/**
 * Kitab rejimində bab başlığı — [ContextGroupedHeader]-in sadə əvəzi.
 *
 * Kart, çərçivə və nömrə dairəsi yoxdur: ən dərin səviyyənin adı ortada, valideyn adı onun üstündə
 * kiçik və solğun, altında qısa ayırıcı xətt. Ad və istiqamət [rememberHadithDisplayName] +
 * [withScriptDirection] ilə gəlir ki, ərəbcə interfeysdə azərbaycanca ad güzgülənməsin.
 *
 * [arabic] — ərəb tabında (yalnız ərəbcə mətn) başlıq da ərəbcə adı aparır; [ContextGroupedHeader]
 * adi rejimdə həmişə belə edirdi, kitab rejimi isə onun əvəzi olduğu üçün eyni qaydaya tabedir.
 */
@Composable
fun HadithBookHeading(
    book: HadithBook?,
    chapter: HadithChapter?,
    subChapter: HadithSubChapter?,
    modifier: Modifier = Modifier,
    /** Tərcümə mətni ölçüsü çarpanı — bütün nərdivan ([BookModeType]) ondan çıxır. */
    sizeMult: Float = 1f,
    /** Adlar interfeys dilindən asılı olmadan ərəbcə gəlsin (ərəb tabı). */
    arabic: Boolean = false,
    arabicFontFamily: FontFamily? = null,
) {
    // Ən dərin səviyyə başlıq olur; onun bir üstü kontekst sətri kimi qalır.
    val primaryName = subChapter?.let { it.name to it.name_ar }
        ?: chapter?.let { it.name to it.name_ar }
        ?: book?.let { it.name to it.name_ar }
        ?: return

    val parentName = when {
        subChapter != null -> chapter?.let { it.name to it.name_ar } ?: book?.let { it.name to it.name_ar }
        chapter != null -> book?.let { it.name to it.name_ar }
        else -> null
    }

    val primary = rememberHadithDisplayName(primaryName.first, primaryName.second, arabic)
    val parent = parentName?.let { rememberHadithDisplayName(it.first, it.second, arabic) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (parent != null && parent.text.isNotBlank()) {
            val parentSize = BookModeType.parentHeading(sizeMult)
            Text(
                text = parent.text,
                style = (
                    if (parent.isArabic) typography.labelMedium.withArabicNameSize(parentSize, 1.6f)
                    else typography.labelMedium.copy(fontSize = parentSize, lineHeight = parentSize * 1.6f)
                ).withScriptDirection(parent.isArabic, arabicFontFamily),
                color = colorScheme.onSurfaceVariant.alpha(0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
        }

        val primarySize = BookModeType.heading(sizeMult)
        Text(
            text = primary.text,
            style = (
                if (primary.isArabic) typography.titleMedium.withArabicNameSize(primarySize)
                else typography.titleMedium.copy(fontSize = primarySize, lineHeight = primarySize * 1.7f)
            ).withScriptDirection(primary.isArabic, arabicFontFamily),
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        HorizontalDivider(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth(0.3f),
            color = colorScheme.outlineVariant.alpha(0.6f),
        )
    }
}

/**
 * Kitab rejimində bir hədis — [HadithCard]-ın çərçivəsiz, ikonsuz əvəzi.
 *
 * Nömrə mətnin öz içindədir: ərəbcə blok görünürsə Quran oxucusundakı kimi `﴿12﴾`, görünmürsə
 * azərbaycanca blokun əvvəlində `12.`. İşarə yalnız bir dəfə verilir. Hədis əlfəcinlidirsə işarə
 * `primary` rəngə keçir — ikon sırası getdiyi üçün əlfəcin vəziyyəti başqa cür görünməz qalardı.
 *
 * Toxunuş əməliyyat vərəqini açır, uzun basma isə (səlahiyyət varsa) redaktoru — sonuncusu adi
 * rejimin davranışıdır və dəyişmir.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HadithBookEntry(
    hadith: Hadith,
    viewMode: Int,
    arabicEnabled: Boolean,
    azerbaijaniEnabled: Boolean,
    sourceEnabled: Boolean,
    /** Hər rəvayətin tərcüməsi öz ərəbcəsinin altında dursun — bax [HadithPreferences.PAIR_NARRATIONS]. */
    pairNarrations: Boolean,
    arabicSizeMult: Float,
    azerbaijaniSizeMult: Float,
    arabicFontFamily: FontFamily,
    showParentheses: Boolean,
    highlightParentheses: Boolean,
    isAuthorized: Boolean = false,
    isBookmarked: Boolean = false,
    todayItems: List<DailyContent> = emptyList(),
    modifier: Modifier = Modifier,
    onOptionsRequest: (Hadith) -> Unit,
    onEditRequest: (Hadith) -> Unit,
) {
    val highlightColor = Color(0xFFE53935) // Quran tərcüməsindəki mötərizə rəngi ilə eyni

    // Boş `text_ar` adi rejimdə görünməyən (kart nömrəni ayrıca nişanda saxlayır), kitab rejimində
    // isə **görünən** fərq yaradır: mətn olmasa da nömrə işarəsi tək bir sətir kimi qalır və
    // azərbaycanca abzas nömrəsiz başlayır. Ona görə blokun görünüb-görünməməsi mətnin özündən asılıdır.
    val showArabic = (viewMode == 0 || viewMode == 1) && arabicEnabled && hadith.text_ar.isNotBlank()
    val showAzerbaijani = (viewMode == 0 || viewMode == 2) && azerbaijaniEnabled && hadith.text_az.isNotBlank()
    val showSource = (viewMode == 0 || viewMode == 2) && sourceEnabled

    val markerColor = if (isBookmarked) colorScheme.primary else colorScheme.onSurface.alpha(0.55f)
    val editLabel = stringResource(Res.string.strLabelEdit)

    val formattedAzText = remember(hadith.text_az, showParentheses, highlightParentheses, highlightColor) {
        formatHadithText(hadith.text_az, showParentheses, highlightParentheses, highlightColor)
    }

    val arabicText = remember(hadith.text_ar, hadith.hadith_no, markerColor) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = markerColor, fontWeight = FontWeight.Bold)) {
                append("‏﴿${hadith.hadith_no}﴾‏ ")
            }
            append(hadith.text_ar)
        }
    }

    val azerbaijaniText: AnnotatedString = remember(formattedAzText, showArabic, hadith.hadith_no, markerColor) {
        if (showArabic) {
            formattedAzText
        } else {
            buildAnnotatedString {
                withStyle(SpanStyle(color = markerColor, fontWeight = FontWeight.Bold)) {
                    append("${hadith.hadith_no}. ")
                }
                append(formattedAzText)
            }
        }
    }

    /**
     * Rəvayət cütləri — ayar açıq və hər iki mətn görünürsə. `null` isə köhnə düzülüş deməkdir:
     * ya ayar bağlıdır, ya tab tək dil göstərir, ya da mətnlər cütləşməyib
     * ([HadithExcerpt.pairedNarrations]).
     *
     * Nömrə işarəsi yalnız **birinci** ərəbcə parçadadır: hədis bir dənədir, rəvayətlər onun
     * içindədir — hər parçaya nömrə qoysaq bir hədis üç hədis kimi görünərdi.
     */
    val narrationBlocks = remember(
        pairNarrations, showArabic, showAzerbaijani, hadith.text_ar, hadith.text_az,
        hadith.hadith_no, markerColor, showParentheses, highlightParentheses, highlightColor,
    ) {
        if (!pairNarrations || !showArabic || !showAzerbaijani) return@remember null

        HadithExcerpt.pairedNarrations(hadith.text_ar, hadith.text_az)
            ?.mapIndexed { index, (arabicPart, translationPart) ->
                val arabic = if (index == 0) {
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = markerColor, fontWeight = FontWeight.Bold)) {
                            append("‏﴿${hadith.hadith_no}﴾‏ ")
                        }
                        append(arabicPart)
                    }
                } else {
                    AnnotatedString(arabicPart)
                }

                arabic to formatHadithText(
                    translationPart, showParentheses, highlightParentheses, highlightColor,
                )
            }
    }

    val isTodayHdotd = remember(hadith, todayItems) {
        todayItems.containsHadith(hadith.id)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onOptionsRequest(hadith) },
                onLongClick = { if (isAuthorized) onEditRequest(hadith) },
                onLongClickLabel = editLabel,
            )
    ) {
        if (isTodayHdotd) {
            IsVotd(isHadith = true)
            Spacer(Modifier.height(12.dp))
        }

        if (narrationBlocks != null) {
            narrationBlocks.forEachIndexed { index, (arabicPart, translationPart) ->
                // Rəvayətlər arasındakı boşluq dil cütünün öz daxili boşluğundan genişdir: sərhədi
                // ayırıcı xətt yox, məhz bu fərq göstərir.
                if (index > 0) Spacer(Modifier.height(24.dp))

                BookArabicText(arabicPart, viewMode, arabicSizeMult, arabicFontFamily)
                Spacer(Modifier.height(16.dp))
                BookTranslationText(translationPart, azerbaijaniSizeMult)
            }
        } else {
            if (showArabic) {
                BookArabicText(arabicText, viewMode, arabicSizeMult, arabicFontFamily)
            }

            if (showAzerbaijani) {
                if (showArabic) Spacer(Modifier.height(16.dp))
                BookTranslationText(azerbaijaniText, azerbaijaniSizeMult)
            }
        }

        if (viewMode == 0 || viewMode == 2) {
            hadith.note?.takeIf { it.isNotBlank() }?.let { note ->
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "${stringResource(Res.string.strTitleNote)}: $note",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = BookModeType.aside(azerbaijaniSizeMult),
                        lineHeight = BookModeType.aside(azerbaijaniSizeMult) * 1.6f,
                        fontStyle = FontStyle.Italic,
                    ).withScriptDirection(arabic = false),
                    color = colorScheme.onSurfaceVariant.alpha(0.8f),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (showSource) {
            hadith.source?.takeIf { it.isNotBlank() }?.let { source ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "— $source",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = BookModeType.aside(azerbaijaniSizeMult),
                        lineHeight = BookModeType.aside(azerbaijaniSizeMult) * 1.5f,
                        fontStyle = FontStyle.Italic,
                    ).withScriptDirection(arabic = false),
                    color = colorScheme.onSurfaceVariant.alpha(0.55f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Kitab rejimində hədisin ərəbcə mətni — bir bütöv, yaxud bir rəvayət. */
@Composable
private fun BookArabicText(
    text: AnnotatedString,
    viewMode: Int,
    arabicSizeMult: Float,
    arabicFontFamily: FontFamily,
) {
    val arabicBase = if (viewMode == 1) 28.sp else 24.sp

    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall.copy(
            fontSize = arabicBase * arabicSizeMult,
            lineHeight = (arabicBase * arabicSizeMult) * 1.9,
            textAlign = TextAlign.Right,
            fontWeight = if (viewMode == 1) FontWeight.Medium else FontWeight.Normal,
        ).withScriptDirection(arabic = true, arabicFontFamily = arabicFontFamily),
        color = colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
        softWrap = true,
    )
}

/** Kitab rejimində hədisin tərcüməsi — bir bütöv, yaxud bir rəvayətin qarşılığı. */
@Composable
private fun BookTranslationText(text: AnnotatedString, azerbaijaniSizeMult: Float) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = BookModeType.body(azerbaijaniSizeMult),
            lineHeight = BookModeType.body(azerbaijaniSizeMult) * 1.75f,
            letterSpacing = 0.15.sp,
        ).withScriptDirection(arabic = false),
        color = colorScheme.onSurface.alpha(0.92f),
        modifier = Modifier.fillMaxWidth(),
        softWrap = true,
    )
}

/**
 * Kitab rejimində bir hədisin əməliyyatları.
 *
 * Kartdan çıxarılan ikon sırasının əvəzidir və eyni callback-lərə bağlanır — burada yeni məntiq
 * yoxdur. Görünüşü Quran oxucusunun [com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.VerseOptionsSheet]-i
 * ilə eynidir ki, iki oxucu eyni jestə eyni cavabı versin.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithOptionsSheet(
    hadith: Hadith?,
    isAuthorized: Boolean,
    isBookmarked: Boolean,
    onShare: (Hadith) -> Unit,
    onBookmark: (Hadith) -> Unit,
    onSetDailyContent: (Hadith) -> Unit,
    onEdit: (Hadith) -> Unit,
    onClose: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (hadith == null) return

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        scrimColor = colorScheme.scrim.alpha(0.5f),
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            BottomSheetHeader(
                title = stringResource(Res.string.hadithOptions),
                hasDragHandle = true,
            )

            Text(
                text = stringResource(Res.string.strLabelHadithNo, hadith.hadith_no),
                style = typography.labelMedium.withScriptDirection(arabic = false),
                color = colorScheme.onSurfaceVariant.alpha(0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            // Nömrənin altında qeyd və qaynaq.
            //
            // Kitab rejimində bunlar yalnız qarışıq/tərcümə tabında axına düşür ([HadithBookEntry]),
            // ərəb tabında isə ümumiyyətlə görünmür — vərəq hədisə toxunanda açılan yeganə yer
            // olduğu üçün onları tabdan asılı olmadan göstərir. Uzun qeyd əməllər sırasını ekrandan
            // qovmasın deyə blok öz içində sürüşür.
            val note = hadith.note?.takeIf { it.isNotBlank() }
            val source = hadith.source?.takeIf { it.isNotBlank() }

            if (note != null || source != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    if (note != null) {
                        Text(
                            text = "${stringResource(Res.string.strTitleNote)}: $note",
                            style = typography.bodySmall
                                .copy(fontStyle = FontStyle.Italic)
                                .withScriptDirection(arabic = false),
                            color = colorScheme.onSurfaceVariant.alpha(0.85f),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (source != null) {
                        if (note != null) Spacer(Modifier.height(8.dp))
                        Text(
                            text = "— $source",
                            style = typography.labelSmall
                                .copy(fontStyle = FontStyle.Italic)
                                .withScriptDirection(arabic = false),
                            color = colorScheme.onSurfaceVariant.alpha(0.6f),
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HadithOptionItem(
                    iconRes = Res.drawable.dr_icon_share,
                    labelRes = Res.string.strLabelShare,
                ) {
                    onClose()
                    onShare(hadith)
                }

                HadithOptionItem(
                    iconRes = if (isBookmarked) Res.drawable.ic_bookmark_added else Res.drawable.ic_bookmark,
                    labelRes = Res.string.strLabelBookmark,
                    tint = if (isBookmarked) colorScheme.primary else null,
                ) {
                    onClose()
                    onBookmark(hadith)
                }

                if (isAuthorized) {
                    HadithOptionItem(
                        iconRes = Res.drawable.dr_icon_heart_filled,
                        labelRes = Res.string.strTitleVOTD,
                        tint = colorScheme.primary,
                    ) {
                        onClose()
                        onSetDailyContent(hadith)
                    }

                    HadithOptionItem(
                        iconRes = Res.drawable.dr_icon_edit,
                        labelRes = Res.string.strLabelEdit,
                    ) {
                        onClose()
                        onEdit(hadith)
                    }
                }
            }
        }
    }
}

/** [HadithOptionsSheet]-in bir əməliyyatı — dairəvi ikon, altında etiket. */
@Composable
private fun HadithOptionItem(
    iconRes: DrawableResource,
    labelRes: StringResource,
    tint: Color? = null,
    onClick: () -> Unit,
) {
    val defaultTint = colorScheme.onBackground.alpha(0.7f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(colorScheme.surfaceVariant.alpha(0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = stringResource(labelRes),
                modifier = Modifier.size(26.dp),
                tint = tint ?: defaultTint,
            )
        }
        Text(
            text = stringResource(labelRes),
            style = typography.labelMedium,
            color = defaultTint,
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}
