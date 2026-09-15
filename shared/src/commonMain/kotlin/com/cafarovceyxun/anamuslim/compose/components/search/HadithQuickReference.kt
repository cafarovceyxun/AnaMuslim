package com.cafarovceyxun.anamuslim.compose.components.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.compose.extensions.bottomBorder
import com.cafarovceyxun.anamuslim.compose.screens.hadith.hadithDisplayName
import com.cafarovceyxun.anamuslim.compose.screens.hadith.isArabicAppLanguage
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.arabicFontFamily
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_open
import com.cafarovceyxun.anamuslim.resources.dr_icon_share
import com.cafarovceyxun.anamuslim.resources.strLabelClose
import com.cafarovceyxun.anamuslim.resources.strLabelHadithNo
import com.cafarovceyxun.anamuslim.resources.strLabelOpen
import com.cafarovceyxun.anamuslim.resources.strLabelShare
import com.cafarovceyxun.anamuslim.resources.strTitleNote
import com.cafarovceyxun.anamuslim.utils.supabase.Hadith
import com.cafarovceyxun.anamuslim.utils.supabase.HadithBook
import com.cafarovceyxun.anamuslim.utils.supabase.HadithChapter
import com.cafarovceyxun.anamuslim.utils.supabase.HadithVolume
import com.cafarovceyxun.anamuslim.utils.text.withSearchHighlight
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextLayoutResult
import com.cafarovceyxun.anamuslim.compose.screens.hadith.HadithSearchNavBar
import com.cafarovceyxun.anamuslim.utils.text.searchMatchRanges
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** [HadithQuickReference]-in göstərdiyi hədis və onun gəldiyi kontekst. */
data class HadithQuickReferenceData(
    val hadith: Hadith,
    /** Axtarış sorğusu — mətndə tapılan sözlər burada da sarı ilə işarələnir. */
    val query: String,
    val volume: HadithVolume? = null,
    val book: HadithBook? = null,
    val chapter: HadithChapter? = null,
)

/**
 * Axtarış nəticəsindəki hədisin **tam mətni** — Quran ayəsinin `QuickReference`-i ilə eyni jest və
 * eyni başlıq sırası (bağla · başlıq · əməllər).
 *
 * Niyə: nəticə kartı mətnin yalnız bir parçasını göstərir (ətrafında `…`), ona görə «bu, axtardığım
 * hədisdirmi?» sualına cavab vermək üçün oxucuya keçmək lazım gəlirdi. Ayə tərəfində bu problem
 * çoxdan vərəqlə həll olunub; hədis tərəfi indi eyni davranışı alır — kart açılır, mətn bütöv
 * görünür, oradan ya paylaşılır, ya da oxucuda açılır.
 *
 * Vərəq **paylaşmanı özü göstərmir**: paylaşma vərəqi də `ModalBottomSheet`-dir və iki vərəqi
 * üst-üstə yığmaq əvəzinə bu, oxucudakı qayda ilə (`HadithOptionsSheet` → `HadithShareSheet`)
 * bağlanıb çağırana ötürülür.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithQuickReference(
    data: HadithQuickReferenceData?,
    onOpen: (HadithQuickReferenceData) -> Unit,
    onShare: (Hadith) -> Unit,
    onClose: () -> Unit,
) {
    if (data == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        scrimColor = colorScheme.scrim.alpha(0.5f),
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        dragHandle = null,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
    ) {
        val scrollState = rememberScrollState()
        val scope = rememberCoroutineScope()

        // Sözün vərəqin İÇİNDƏ hara düşdüyünü tapmaq üçün iki ölçü lazımdır: mətnin ölçmə nəticəsi
        // (hansı sətir) və onun sürüşən sütundakı yeri. İkisi ayrı geri-çağırışdan gəlir.
        var blocks by remember(data) { mutableStateOf<Map<Int, HadithQuickAnchor>>(emptyMap()) }
        var containerTop by remember(data) { mutableStateOf(0f) }
        var currentMatch by remember(data) { mutableIntStateOf(0) }

        val reportAnchor: (Int, TextLayoutResult?, Float?) -> Unit = { slot, layout, top ->
            val previous = blocks[slot]
            blocks = blocks + (slot to HadithQuickAnchor(
                layout = layout ?: previous?.layout,
                topInWindow = top ?: previous?.topInWindow,
            ))
        }

        // Uyğunluqlar sənəd sırası ilə: əvvəl ərəbcə blok, sonra tərcümə. Sayğac və oxlar bu
        // siyahını gəzir; sıra ekrandakı sıra ilə eynidir ki, «növbəti» həqiqətən aşağı aparsın.
        val matches = remember(blocks, data.query) {
            buildList {
                listOf(SLOT_ARABIC, SLOT_TRANSLATION).forEach { slot ->
                    val layout = blocks[slot]?.layout ?: return@forEach
                    searchMatchRanges(layout.layoutInput.text.text, data.query)
                        .forEach { range -> add(slot to range.first) }
                }
            }
        }

        fun scrollToMatch(index: Int) {
            val (slot, offset) = matches.getOrNull(index) ?: return
            val anchor = blocks[slot] ?: return
            val layout = anchor.layout ?: return
            val top = anchor.topInWindow ?: return
            currentMatch = index

            val line = layout.getLineForOffset(offset.coerceIn(0, layout.layoutInput.text.length))
            // Söz vərəqin lap yuxarısına yapışmasın: bir az kontekst üstündə qalsın.
            val target = (top - containerTop + layout.getLineTop(line) - 120f).toInt()
            scope.launch { scrollState.animateScrollTo(target.coerceAtLeast(0)) }
        }

        // Vərəq açılan kimi ilk uyğunluğa enir — istifadəçi uzun hədisdə sözü əl ilə axtarmasın.
        var landed by remember(data) { mutableStateOf(false) }
        LaunchedEffect(matches) {
            if (landed || matches.isEmpty()) return@LaunchedEffect
            landed = true
            scrollToMatch(0)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
        ) {
            HadithQuickReferenceHeader(
                title = stringResource(Res.string.strLabelHadithNo, data.hadith.hadith_no),
                onShare = { onShare(data.hadith) },
                onOpen = { onOpen(data) },
                onClose = onClose,
            )

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                // Cari uyğunluq hansı blokdadır və orada neçəncidir — narıncı işarə üçün.
                val current = matches.getOrNull(currentMatch)
                val currentSlot = current?.first
                val currentIndexInSlot = current?.let { (slot, _) ->
                    matches.take(currentMatch).count { it.first == slot }
                }

                HadithQuickReferenceBody(
                    data = data,
                    currentSlot = currentSlot,
                    currentIndexInSlot = currentIndexInSlot,
                    onAnchor = reportAnchor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .verticalScroll(scrollState)
                        .onGloballyPositioned { containerTop = it.positionInWindow().y }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                )

                // Oxlar yalnız uyğunluq varsa: sorğu ərəbcə yazılıbsa və mətn azərbaycancadırsa
                // (və ya əksi) vərəqdə işarələnəcək söz olmaya bilər.
                if (matches.size > 1) {
                    HadithSearchNavBar(
                        query = data.query,
                        current = currentMatch + 1,
                        total = matches.size,
                        onPrevious = {
                            scrollToMatch(if (currentMatch <= 0) matches.lastIndex else currentMatch - 1)
                        },
                        onNext = {
                            scrollToMatch(if (currentMatch >= matches.lastIndex) 0 else currentMatch + 1)
                        },
                        onDismiss = null,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp),
                    )
                }
            }
        }
    }
}

/** Vərəqdəki bloklar — sıra ekrandakı sıradır. */
private const val SLOT_ARABIC = 0
private const val SLOT_TRANSLATION = 1

/** Vərəqdəki bir mətn blokunun ölçmə nəticəsi və pəncərədəki yeri. */
private data class HadithQuickAnchor(
    val layout: TextLayoutResult? = null,
    val topInWindow: Float? = null,
)

/** Ayə vərəqindəki sıranın eynisi: bağla · başlıq · əməllər. */
@Composable
private fun HadithQuickReferenceHeader(
    title: String,
    onShare: () -> Unit,
    onOpen: () -> Unit,
    onClose: () -> Unit,
) {
    val iconTint = colorScheme.onSurface.alpha(0.7f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bottomBorder()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_close),
                contentDescription = stringResource(Res.string.strLabelClose),
                tint = colorScheme.onSurface,
            )
        }

        Text(
            text = title,
            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                .withScriptDirection(arabic = false),
            color = colorScheme.primary,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )

        IconButton(onClick = onShare) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_share),
                contentDescription = stringResource(Res.string.strLabelShare),
                tint = iconTint,
            )
        }

        IconButton(onClick = onOpen) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_open),
                contentDescription = stringResource(Res.string.strLabelOpen),
                tint = iconTint,
            )
        }
    }
}

/**
 * Hədisin özü: yeri, ərəbcə mətn, tərcümə, qeyd və qaynaq.
 *
 * Mətn oxucunun şrift ayarlarını **oxumur** — vərəq qısa baxışdır, ölçü burada sabitdir; oxucuya
 * keçmək üçün başlıqdakı «aç» düyməsi var. İstiqamət isə hər blokun öz yazısından gəlir
 * ([withScriptDirection]), yoxsa ərəbcə interfeysdə azərbaycanca abzas güzgülənir.
 */
@Composable
private fun HadithQuickReferenceBody(
    data: HadithQuickReferenceData,
    /** Oxların dayandığı blok və orada neçənci uyğunluq olduğu — həmin söz narıncı olur. */
    currentSlot: Int?,
    currentIndexInSlot: Int?,
    /** Blokun ölçmə nəticəsi və yeri — oxlar sözün sətrinə enmək üçün ikisini də istəyir. */
    onAnchor: (slot: Int, layout: TextLayoutResult?, topInWindow: Float?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val arabicUi = isArabicAppLanguage()
    val breadcrumb = remember(data, arabicUi) {
        listOfNotNull(
            data.volume?.let { hadithDisplayName(it.name, it.name_ar, arabicUi).text },
            data.book?.let { hadithDisplayName(it.name, it.name_ar, arabicUi).text },
            data.chapter?.let { hadithDisplayName(it.name, it.name_ar, arabicUi).text },
        ).filter { it.isNotBlank() }.joinToString(" › ")
    }

    val arabicText = remember(data, currentSlot, currentIndexInSlot) {
        data.hadith.text_ar.withSearchHighlight(
            data.query,
            currentIndexInSlot.takeIf { currentSlot == SLOT_ARABIC },
        )
    }
    val translationText = remember(data, currentSlot, currentIndexInSlot) {
        data.hadith.text_az.withSearchHighlight(
            data.query,
            currentIndexInSlot.takeIf { currentSlot == SLOT_TRANSLATION },
        )
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (breadcrumb.isNotEmpty()) {
            Text(
                text = breadcrumb,
                style = typography.labelSmall.withScriptDirection(arabic = arabicUi),
                color = colorScheme.onSurfaceVariant.alpha(0.75f),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (data.hadith.text_ar.isNotBlank()) {
            Text(
                text = arabicText,
                style = typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    lineHeight = 22.sp * 1.9,
                    textAlign = TextAlign.Right,
                ).withScriptDirection(arabic = true, arabicFontFamily = arabicFontFamily()),
                color = colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { onAnchor(SLOT_ARABIC, null, it.positionInWindow().y) },
                onTextLayout = { onAnchor(SLOT_ARABIC, it, null) },
            )

            if (data.hadith.text_az.isNotBlank()) {
                HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.4f))
            }
        }

        if (data.hadith.text_az.isNotBlank()) {
            Text(
                text = translationText,
                style = typography.bodyLarge.copy(lineHeight = 17.sp * 1.6)
                    .withScriptDirection(arabic = false),
                color = colorScheme.onSurface.alpha(0.92f),
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { onAnchor(SLOT_TRANSLATION, null, it.positionInWindow().y) },
                onTextLayout = { onAnchor(SLOT_TRANSLATION, it, null) },
            )
        }

        data.hadith.note?.takeIf { it.isNotBlank() }?.let { note ->
            Text(
                text = "${stringResource(Res.string.strTitleNote)}: $note",
                style = typography.bodySmall.copy(fontStyle = FontStyle.Italic)
                    .withScriptDirection(arabic = false),
                color = colorScheme.onSurfaceVariant.alpha(0.85f),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        data.hadith.source?.takeIf { it.isNotBlank() }?.let { source ->
            Text(
                text = "— $source",
                style = typography.labelSmall.copy(fontStyle = FontStyle.Italic)
                    .withScriptDirection(arabic = false),
                color = colorScheme.onSurfaceVariant.alpha(0.6f),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Vərəq 85% hündürlükdədir; sondakı boşluq uzun hədisdə mətnin kənara yapışmasının qarşısını
        // alır (sürüşmə sonunda «daha nəsə var» təəssüratı qalmasın).
        Spacer(Modifier.height(8.dp))
    }
}
