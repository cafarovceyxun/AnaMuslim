package com.cafarovceyxun.anamuslim.compose.screens.dua

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.compose.extensions.bottomBorder
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.arabicFontFamily
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.asmaSourceSheetTitle
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_open
import com.cafarovceyxun.anamuslim.resources.duaExcerptNotFound
import com.cafarovceyxun.anamuslim.resources.duaHighlightHint
import com.cafarovceyxun.anamuslim.resources.duaOpenInHadith
import com.cafarovceyxun.anamuslim.resources.duaOpenInReader
import com.cafarovceyxun.anamuslim.resources.duaSourceNotFound
import com.cafarovceyxun.anamuslim.resources.duaSourceSheetTitle
import com.cafarovceyxun.anamuslim.resources.strLabelClose
import com.cafarovceyxun.anamuslim.resources.strTitleNote
import com.cafarovceyxun.anamuslim.utils.dua.DuaSourceContent
import com.cafarovceyxun.anamuslim.utils.dua.loadDuaSource
import com.cafarovceyxun.anamuslim.utils.supabase.DuaSourceRef
import com.cafarovceyxun.anamuslim.utils.text.excerptMatchRange
import com.cafarovceyxun.anamuslim.utils.text.withExcerptHighlight
import com.cafarovceyxun.anamuslim.utils.text.withExcerptHighlights
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Duanın (və ya Əsmaül Hüsnə dəlilinin) **qaynağı** — tam hədis/ayə, içində götürülmüş hissə sarı
 * ilə işarələnmiş halda.
 *
 * Görünüşü qəsdən axtarış ekranının `HadithQuickReference` vərəqi ilə eynidir: istifadəçi eyni
 * jestə (bir düymə → vərəq) eyni cavabı alsın. Fərq vurğunun qaydasındadır — axtarışda sorğu
 * söz-söz bölünür, burada isə çıxarış **bitişik bir parçadır** ([excerptMatchRange]); əks halda
 * duanın hər sözü hədis boyu ayrıca boyanardı.
 *
 * Mətn mənbədən yenidən qurulur, dua sətrindən yox: ekranda görünən kontekstdir. Mənbə sonradan
 * redaktə olunubsa çıxarış tapılmaya bilər — o halda mətn vurğusuz göstərilir və altda bunu deyən
 * bir sətir çıxır, yanlış yerə düşmüş sarı fon əvəzinə.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuaSourceSheet(
    ref: DuaSourceRef?,
    /** Başlıq: dua üçün «Duanın qaynağı», Əsmaül Hüsnə üçün «Dəlilin qaynağı». */
    isEvidence: Boolean = false,
    onClose: () -> Unit,
) {
    if (ref == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val actions = LocalDuaActions.current

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        scrimColor = colorScheme.scrim.alpha(0.5f),
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        dragHandle = null,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
    ) {
        // `produceState` açarı mənbənin özüdür: eyni vərəq başqa dua ilə yenidən açılanda mətn
        // yenidən oxunur.
        val content by produceState<DuaSourceLoadState>(DuaSourceLoadState.Loading, ref) {
            value = loadDuaSource(ref)
                ?.let { DuaSourceLoadState.Loaded(it) }
                ?: DuaSourceLoadState.Missing
        }

        val openSource: (() -> Unit)? = remember(ref, actions) {
            when {
                ref.isHadith -> actions.onOpenHadith?.let { open ->
                    ref.hadith_id?.let { id -> { open(id) } }
                }

                else -> actions.onOpenVerse?.let { open ->
                    val chapterNo = ref.chapter_no
                    val verseNo = ref.verse_no
                    if (chapterNo != null && verseNo != null) {
                        { open(chapterNo, verseNo) }
                    } else {
                        null
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
        ) {
            SourceSheetHeader(
                title = stringResource(
                    if (isEvidence) Res.string.asmaSourceSheetTitle
                    else Res.string.duaSourceSheetTitle,
                ),
                openLabel = stringResource(
                    if (ref.isHadith) Res.string.duaOpenInHadith else Res.string.duaOpenInReader,
                ),
                // Düymə yalnız host seam-i doldurubsa görünür — bax [DuaActions].
                onOpen = openSource?.let { open -> { onClose(); open() } },
                onClose = onClose,
            )

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (val state = content) {
                    DuaSourceLoadState.Loading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center).size(28.dp),
                        color = colorScheme.primary,
                    )

                    DuaSourceLoadState.Missing -> Text(
                        text = stringResource(Res.string.duaSourceNotFound),
                        style = typography.bodyMedium.withScriptDirection(arabic = false),
                        color = colorScheme.onSurfaceVariant.alpha(0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                    )

                    is DuaSourceLoadState.Loaded -> SourceSheetBody(
                        source = state.content,
                        excerptAr = ref.text_ar,
                        excerptAz = ref.text_az,
                        excerptTranslit = ref.transliteration,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    )
                }
            }
        }
    }
}

/** Vərəqin yüklənmə vəziyyəti — «yüklənir» ilə «tapılmadı» eyni boş ekrana düşməsin deyə ayrıdır. */
private sealed interface DuaSourceLoadState {
    data object Loading : DuaSourceLoadState
    data object Missing : DuaSourceLoadState
    data class Loaded(val content: DuaSourceContent) : DuaSourceLoadState
}

/** `HadithQuickReference`-dəki sıranın eynisi: bağla · başlıq · aç. */
@Composable
private fun SourceSheetHeader(
    title: String,
    openLabel: String,
    onOpen: (() -> Unit)?,
    onClose: () -> Unit,
) {
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

        if (onOpen != null) {
            IconButton(onClick = onOpen) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_open),
                    contentDescription = openLabel,
                    tint = colorScheme.onSurface.alpha(0.7f),
                )
            }
        } else {
            // Başlıq mərkəzdə qalsın deyə bağlama düyməsinin eni ayrılır.
            Spacer(Modifier.size(48.dp))
        }
    }
}

/**
 * Mənbənin özü: istinad sətri, ərəbcə mətn, tərcümə, qeyd.
 *
 * ⚠️ **Latın parçaları hansı blokda olduğunu bilmir.** Ərəbcə çıxarış həmişə ərəbcə mətndədir, amma
 * oxunuş və tərcümə belə deyil: bu toplusunda duanın **oxunuşu** rəvayətin içində `{…}` arasında,
 * **mənası** isə qeyddə yazılır. Əvvəl tərcümə parçası yalnız tərcümədə axtarılırdı və qeyd
 * ümumiyyətlə vurğulanmırdı — nəticədə hər iki blok sarısız qalırdı (2026-09-15). İndi tərcümə də,
 * qeyd də **hər iki** latın parçası üzrə yoxlanılır; hansı ora düşübsə, o boyanır.
 *
 * Xəbərdarlıq sətri yalnız **ərəbcə** çıxarış tapılmayanda çıxır: o, həmişə mənbədən götürülür,
 * yəni tapılmaması mənbənin dəyişdiyini bildirir. Latın parçası isə əl ilə də yazıla bilər
 * (mənbədə tərcümə/oxunuş olmayanda) — onu «itib» saymaq yalan xəbərdarlıq olardı.
 */
@Composable
private fun SourceSheetBody(
    source: DuaSourceContent,
    excerptAr: String,
    excerptAz: String,
    excerptTranslit: String?,
    modifier: Modifier = Modifier,
) {
    val latinExcerpts = remember(excerptAz, excerptTranslit) {
        listOf(excerptAz, excerptTranslit)
    }

    val arabicText = remember(source.arabic, excerptAr) {
        source.arabic.withExcerptHighlight(excerptAr)
    }
    val translationText = remember(source.translation, latinExcerpts) {
        source.translation.withExcerptHighlights(latinExcerpts)
    }
    val noteText = remember(source.note, latinExcerpts) {
        source.note?.withExcerptHighlights(latinExcerpts)
    }

    val someExcerptMissing = remember(source.arabic, excerptAr) {
        source.arabic.isNotBlank() && excerptMatchRange(source.arabic, excerptAr) == null
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (source.reference.isNotBlank()) {
            Text(
                text = source.reference,
                style = typography.labelSmall.withScriptDirection(arabic = false),
                color = colorScheme.onSurfaceVariant.alpha(0.75f),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (source.arabic.isNotBlank()) {
            Text(
                text = arabicText,
                style = typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    lineHeight = 22.sp * 1.9,
                    textAlign = TextAlign.Right,
                ).withScriptDirection(arabic = true, arabicFontFamily = arabicFontFamily()),
                color = colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )

            if (source.translation.isNotBlank()) {
                HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.4f))
            }
        }

        if (source.translation.isNotBlank()) {
            Text(
                text = translationText,
                style = typography.bodyLarge
                    .withLineHeightRatio(TRANSLATION_LINE_HEIGHT_RATIO)
                    .withScriptDirection(arabic = false),
                color = colorScheme.onSurface.alpha(0.92f),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        noteText?.let { note ->
            Text(
                text = buildAnnotatedString {
                    append(stringResource(Res.string.strTitleNote))
                    append(": ")
                    append(note)
                },
                style = typography.bodySmall.copy(fontStyle = FontStyle.Italic)
                    .withScriptDirection(arabic = false),
                color = colorScheme.onSurfaceVariant.alpha(0.85f),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.3f))

        Text(
            text = stringResource(
                if (someExcerptMissing) Res.string.duaExcerptNotFound
                else Res.string.duaHighlightHint,
            ),
            style = typography.labelSmall.withScriptDirection(arabic = false),
            color = colorScheme.onSurfaceVariant.alpha(0.7f),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))
    }
}
