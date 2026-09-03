package com.cafarovceyxun.anamuslim.compose.components.homepage

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.QuranImageEditorScreen
import com.cafarovceyxun.anamuslim.compose.components.share.ShareImageSegment
import com.cafarovceyxun.anamuslim.compose.screens.hadith.HadithImageEditorScreen
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.hadithArabicFontFamily
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.VersePreferences
import com.cafarovceyxun.anamuslim.repository.supabase.DailyContentRepository
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.app_name
import com.cafarovceyxun.anamuslim.resources.dailyContentShareDirect
import com.cafarovceyxun.anamuslim.resources.dailyContentShareDirectHint
import com.cafarovceyxun.anamuslim.resources.dailyContentShareEdit
import com.cafarovceyxun.anamuslim.resources.dailyContentShareEditHint
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_edit
import com.cafarovceyxun.anamuslim.resources.dr_icon_eye
import com.cafarovceyxun.anamuslim.resources.dr_icon_share
import com.cafarovceyxun.anamuslim.resources.ic_bookmark
import com.cafarovceyxun.anamuslim.resources.ic_bookmark_added
import com.cafarovceyxun.anamuslim.resources.ic_launcher_foreground
import com.cafarovceyxun.anamuslim.resources.shareImageFailed
import com.cafarovceyxun.anamuslim.resources.strDescClose
import com.cafarovceyxun.anamuslim.resources.strLabelBookmark
import com.cafarovceyxun.anamuslim.resources.strLabelRead
import com.cafarovceyxun.anamuslim.resources.strLabelShare
import com.cafarovceyxun.anamuslim.resources.strTitleDailyHadith
import com.cafarovceyxun.anamuslim.resources.strTitleVOTD
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.supabase.DailyContent
import com.cafarovceyxun.anamuslim.utils.verse.DailyContentBookmarks
import com.cafarovceyxun.anamuslim.utils.verse.DailyContentDisplay
import com.cafarovceyxun.anamuslim.utils.verse.DailyContentFactory
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Bir hekayənin ekranda qalma müddəti — funksiya hekayələri ilə eyni ritm. */
private const val DAILY_STORY_DURATION_MILLIS = 8000

/** Bağlamaq üçün lazım olan aşağı sürüşdürmə məsafəsi (piksel). */

/**
 * Günün elementlərinin tam ekran hekayə baxışı.
 *
 * `Dialog` olaraq açılır (CLAUDE.md, «tam ekran səth `Dialog` olmalıdır»): ana səhifə modal vərəqin
 * altından da göstərilə bilər, inline emit ediləndə hekayə vərəqin pəncərəsinin altında qalardı.
 *
 * Jestlər hekayə konvensiyasının özüdür: **basılı saxlayanda sayğac dayanır**, buraxanda davam edir;
 * sağ yarıya toxunmaq növbəti, sol yarı əvvəlki səhifədir; **yuxarıdan aşağı sürüşdürmək bağlayır**.
 *
 * Mətn sürüşdürülmür — uzunluğuna görə **şrift ölçüsü** kiçilir ([contentScale]), yəni səhifə həmişə
 * bütöv görünür və aşağı sürüşdürmə jesti mətnin öz sürüşməsi ilə toqquşmur.
 *
 * ### Paylaşma
 * Ortadakı kadr `GraphicsLayer`-ə yazılır, ona görə «birbaşa paylaş» **elə görünən hekayəni** şəkil
 * kimi göndərir. Kadra qəsdən yalnız fon, mətn və tətbiqin imzası düşür: başlıq zolağı, baxış sayı
 * və «Oxu» düyməsi kadrdan **kənardadır** — onlar idarəetmə elementidir, paylaşılan şəklin hissəsi
 * deyil.
 */
@Composable
fun DailyContentStoryViewer(
    items: List<DailyContent>,
    onSeen: (Long) -> Unit,
    onClose: () -> Unit,
) {
    if (items.isEmpty()) return

    var index by rememberSaveable { mutableStateOf(0) }
    var showImageEditor by rememberSaveable { mutableStateOf(false) }
    var showShareOptions by rememberSaveable { mutableStateOf(false) }
    // İki ayrı dayandırma: barmaq ekranda ([isHeldPaused]) və ortadan toxunuşla ([isTapPaused]).
    var isHeldPaused by remember { mutableStateOf(false) }
    var isTapPaused by remember { mutableStateOf(false) }
    val isPaused = isHeldPaused || isTapPaused
    var isSharing by remember { mutableStateOf(false) }

    val current = items.getOrNull(index.coerceIn(0, items.lastIndex)) ?: return
    val progress = remember { Animatable(0f) }
    val cardLayer = rememberGraphicsLayer()

    val display by produceState(DailyContentDisplay("", "", ""), current) {
        value = DailyContentFactory.display(current)
    }

    val scope = rememberCoroutineScope()
    val homeActions = LocalHomeActions.current
    val repository = remember { DailyContentRepository() }
    val failedMessage = stringResource(Res.string.shareImageFailed)
    val shareTitle = stringResource(Res.string.strLabelShare)

    val isBookmarked by remember(current) { DailyContentBookmarks.isBookmarkedFlow(current) }
        .collectAsStateWithLifecycle(false)

    // Səhifə açılan kimi iki şey olur: serverdəki baxış sayılır (cihaz başına bir dəfə, dedupe
    // repozitoriyadadır) və element **cihazda** baxılmış kimi qeyd edilir — dairənin halqası buna
    // görə sönür. İkisi ayrıdır: şəbəkə olmasa da halqa sönməlidir.
    LaunchedEffect(current.id) {
        val id = current.id ?: return@LaunchedEffect

        // Səhifə dəyişəndə pauza qalmır — yoxsa növbəti səhifə donmuş zolaqla açılardı.
        isTapPaused = false
        VersePreferences.markStorySeen(id)
        onSeen(id)
        repository.registerView(id)
    }

    // Sayğac dörd halda dayanır: barmaq ekranda, şəkil redaktoru açıq, paylaşma vərəqi açıq,
    // paylaşma hazırlanır.
    LaunchedStoryProgress(
        key = index,
        running = !isPaused && !showImageEditor && !showShareOptions && !isSharing,
        durationMillis = DAILY_STORY_DURATION_MILLIS,
        progress = progress,
    ) {
        if (index < items.lastIndex) index++ else onClose()
    }

    if (showImageEditor) {
        DailyContentImageEditor(
            content = current,
            display = display,
            onBack = { showImageEditor = false },
        )
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            // ⚠️ Qeyri-şəffaf baza: `Dialog` pəncərəsinin öz fonu şəffafdır, ona görə yalnız
            // qradiyent verilsə ana səhifə arxadan görünürdü.
            modifier = Modifier.fillMaxSize().background(colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                StoryHeader(
                    items = items,
                    index = index,
                    progress = progress.value,
                    current = current,
                    isBookmarked = isBookmarked,
                    onBookmark = { scope.launch { DailyContentBookmarks.toggle(current) } },
                    onShare = { showShareOptions = true },
                    onClose = onClose,
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(storyBackground(index))
                        // Paylaşılan şəkil məhz bu kadrdır.
                        .drawWithCache {
                            onDrawWithContent {
                                cardLayer.record { this@onDrawWithContent.drawContent() }
                                drawLayer(cardLayer)
                            }
                        }
                        .pointerInput(items.size) {
                            detectTapGestures(
                                // Basılı saxlamaq sayğacı dayandırır — uzun hədisi oxumağa imkan verir.
                                onPress = {
                                    isHeldPaused = true
                                    tryAwaitRelease()
                                    isHeldPaused = false
                                },
                                // `onLongPress` verilməsə uzun basışın buraxılışı da `onTap` sayılır
                                // və hekayə oxunub-bitirilən kimi növbəti səhifəyə tullanırdı.
                                onLongPress = {},
                                // Üç zolaq: sol → əvvəlki, sağ → növbəti, **orta → pauza**. Orta
                                // zolaq basılı saxlamadan fərqlidir: barmaq qaldırılanda da durur.
                                onTap = { offset ->
                                    when {
                                        offset.x < size.width / 3f -> {
                                            isTapPaused = false
                                            if (index > 0) index--
                                        }

                                        offset.x > size.width * 2 / 3f -> {
                                            isTapPaused = false
                                            if (index < items.lastIndex) index++ else onClose()
                                        }

                                        else -> isTapPaused = !isTapPaused
                                    }
                                },
                            )
                        }
                        .pointerInput(Unit) {
                            var dragged = 0f

                            detectVerticalDragGestures(
                                onDragStart = { dragged = 0f },
                                onDragEnd = { if (dragged > STORY_DISMISS_DRAG_PX) onClose() },
                                onDragCancel = { dragged = 0f },
                                onVerticalDrag = { _, delta -> dragged += delta },
                            )
                        },
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        DailyContentStoryPage(
                            display = display,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp, vertical = 16.dp),
                        )

                        // İmza kadrın **içindədir**: paylaşılan şəkildə mənbə görünsün.
                        BrandSignature(modifier = Modifier.padding(bottom = 20.dp))
                    }
                }

                StoryActionBar(
                    viewCount = current.view_count,
                    onRead = {
                        if (current.isHadith) {
                            homeActions.onOpenHadithById(current.hadith_id)
                        } else {
                            val chapterNo = current.chapter_no
                            val verseNo = current.verse_no
                            if (chapterNo != null && verseNo != null) {
                                homeActions.onOpenVerse(chapterNo, verseNo)
                            }
                        }

                        onClose()
                    },
                )
            }

            if (showShareOptions) {
                ShareOptionsSheet(
                    isSharing = isSharing,
                    onDismiss = { showShareOptions = false },
                    onEdit = {
                        showShareOptions = false
                        showImageEditor = true
                    },
                    onDirect = {
                        showShareOptions = false

                        scope.launch {
                            isSharing = true

                            val shared = try {
                                PlatformUtils.shareImage(cardLayer.toImageBitmap(), shareTitle)
                            } catch (e: Exception) {
                                AppLogger.saveError(e, "DailyContentStoryViewer.share")
                                false
                            }

                            isSharing = false

                            if (!shared) PlatformUtils.showLongToast(failedMessage)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun StoryHeader(
    items: List<DailyContent>,
    index: Int,
    progress: Float,
    current: DailyContent,
    isBookmarked: Boolean,
    onBookmark: () -> Unit,
    onShare: () -> Unit,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items.forEachIndexed { i, _ ->
                val fill = when {
                    i < index -> 1f
                    i == index -> progress
                    else -> 0f
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colorScheme.onSurface.alpha(0.2f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fill)
                            .height(3.dp)
                            .background(colorScheme.primary),
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (current.isHadith) {
                    stringResource(Res.string.strTitleDailyHadith)
                } else {
                    stringResource(Res.string.strTitleVOTD)
                },
                style = typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )

            IconButton(
                painter = painterResource(
                    if (isBookmarked) Res.drawable.ic_bookmark_added else Res.drawable.ic_bookmark
                ),
                contentDescription = stringResource(Res.string.strLabelBookmark),
                tint = if (isBookmarked) colorScheme.primary else colorScheme.onSurface,
                small = true,
                onClick = onBookmark,
            )

            IconButton(
                painter = painterResource(Res.drawable.dr_icon_share),
                contentDescription = stringResource(Res.string.strLabelShare),
                tint = colorScheme.onSurface,
                small = true,
                onClick = onShare,
            )

            IconButton(
                painter = painterResource(Res.drawable.dr_icon_close),
                contentDescription = stringResource(Res.string.strDescClose),
                tint = colorScheme.onSurface,
                small = true,
                onClick = onClose,
            )
        }
    }
}

/** Tətbiqin imzası — paylaşılan şəklin bir hissəsidir, ona görə kadrın içində qalır. */
@Composable
private fun BrandSignature(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(26.dp).clip(CircleShape),
        )

        Spacer(Modifier.width(6.dp))

        Text(
            text = stringResource(Res.string.app_name),
            style = typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface.alpha(0.7f),
        )
    }
}

/**
 * Kadrdan kənardakı idarəetmə zolağı: ortada «Oxu», sağ aşağıda **göz nişanı + baxış sayı**.
 * Paylaşılan şəkildə heç biri görünmür.
 */
@Composable
private fun StoryActionBar(viewCount: Int, onRead: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        FilledTonalButton(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            onClick = onRead,
        ) {
            Text(text = stringResource(Res.string.strLabelRead), style = typography.labelLarge)
        }

        if (viewCount > 0) {
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_eye),
                    contentDescription = null,
                    tint = colorScheme.onSurface.alpha(0.55f),
                    modifier = Modifier.size(14.dp),
                )

                Text(
                    text = viewCount.toString(),
                    style = typography.labelSmall,
                    color = colorScheme.onSurface.alpha(0.55f),
                )
            }
        }
    }
}

@Composable
private fun DailyContentStoryPage(
    display: DailyContentDisplay,
    modifier: Modifier = Modifier,
) {
    val arabicFont = hadithArabicFontFamily(HadithPreferences.observeArabicFont())
    val scale = contentScale(display)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (display.arabic.isNotBlank()) {
            SelectionContainer(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = display.arabic,
                    style = typography.titleLarge
                        .copy(fontSize = 22.sp * scale, lineHeight = 22.sp * scale * 1.7f)
                        .withScriptDirection(arabic = true, arabicFontFamily = arabicFont),
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(20.dp))
        }

        if (display.translation.isNotBlank()) {
            SelectionContainer(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = display.translation,
                    // Hədis və tərcümə mətni azərbaycancadır: istiqaməti düzülüşdən yox, öz
                    // yazısından alır, yoxsa ərəbcə interfeysdə güzgülənir (CLAUDE.md).
                    style = typography.bodyLarge
                        .copy(fontSize = 17.sp * scale, lineHeight = 17.sp * scale * 1.5f)
                        .withScriptDirection(arabic = false),
                    color = colorScheme.onSurface.alpha(0.9f),
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (display.reference.isNotBlank()) {
            Spacer(Modifier.height(20.dp))

            Text(
                text = display.reference,
                style = typography.labelLarge.withScriptDirection(arabic = false),
                color = colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Mətnin uzunluğuna görə şrift əmsalı.
 *
 * Ölçünü **məzmun** təyin edir: qısa ayə iri və nəfəs alan, uzun hədis isə kiçik yazı ilə çıxır,
 * yəni səhifə hər iki halda bütöv görünür. Ölçmə-yenidən ölçmə dövrəsi qəsdən yoxdur — nəticə
 * deterministikdir və ilk kadrda düzgün olur.
 */
private fun contentScale(display: DailyContentDisplay): Float {
    val length = display.arabic.length + display.translation.length

    return when {
        length <= 180 -> 1.35f
        length <= 400 -> 1.15f
        length <= 800 -> 1f
        length <= 1400 -> 0.85f
        length <= 2200 -> 0.72f
        else -> 0.62f
    }
}

/**
 * Paylaşma seçimləri — hekayənin **öz pəncərəsində** çəkilən alt vərəq.
 *
 * `ModalBottomSheet` qəsdən işlədilmir: hekayə onsuz da `Dialog` pəncərəsidir və pəncərə içində
 * pəncərə iOS-da etibarsızdır (CLAUDE.md-dəki eyni sinif problem). Burada vərəq sadəcə eyni
 * pəncərənin üstündəki qatdır — davranışı tam idarə olunur.
 */
@Composable
private fun ShareOptionsSheet(
    isSharing: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDirect: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.scrim.alpha(0.45f))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(colorScheme.surfaceContainerHigh)
                // Vərəqin öz sahəsinə toxunuş onu bağlamamalıdır.
                .clickable(enabled = false, onClick = {})
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 8.dp, vertical = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colorScheme.onSurfaceVariant.alpha(0.4f)),
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = stringResource(Res.string.strLabelShare),
                style = typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp),
            )

            Spacer(Modifier.height(10.dp))

            ShareOptionRow(
                icon = Res.drawable.dr_icon_share,
                title = stringResource(Res.string.dailyContentShareDirect),
                subtitle = stringResource(Res.string.dailyContentShareDirectHint),
                trailing = { if (isSharing) CircularProgressIndicator(Modifier.size(18.dp)) },
                onClick = onDirect,
            )

            ShareOptionRow(
                icon = Res.drawable.dr_icon_edit,
                title = stringResource(Res.string.dailyContentShareEdit),
                subtitle = stringResource(Res.string.dailyContentShareEditHint),
                onClick = onEdit,
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ShareOptionRow(
    icon: org.jetbrains.compose.resources.DrawableResource,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit = {},
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface,
            )

            Text(
                text = subtitle,
                style = typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
            )
        }

        trailing()
    }
}

/** Cari səhifəni mövcud şəkil redaktoruna verir — ayə və hədis öz adapterindən keçir. */
@Composable
private fun DailyContentImageEditor(
    content: DailyContent,
    display: DailyContentDisplay,
    onBack: () -> Unit,
) {
    if (content.isHadith) {
        HadithImageEditorScreen(
            eyebrow = stringResource(Res.string.strTitleDailyHadith),
            arabicText = display.arabic,
            translationText = display.translation,
            reference = display.reference,
            // Günün məzmununda qeyd sahəsi yoxdur — növbəyə yalnız mətn və qaynaq düşür.
            note = null,
            includeArabic = display.arabic.isNotBlank(),
            includeAzerbaijani = display.translation.isNotBlank(),
            onBack = onBack,
        )
    } else {
        QuranImageEditorScreen(
            segments = listOf(
                ShareImageSegment(arabic = display.arabic, translation = display.translation)
            ),
            reference = display.reference,
            // Günün məzmununda qeyd sahəsi yoxdur — növbəyə yalnız mətn və qaynaq düşür.
            note = null,
            includeArabic = display.arabic.isNotBlank(),
            includeAzerbaijani = display.translation.isNotBlank(),
            onBack = onBack,
        )
    }
}

/**
 * Səhifə fonu — hər səhifə öz rəng cütü ilə çıxır ki, ardıcıl elementlər bir-birindən seçilsin.
 *
 * Rənglər **qeyri-şəffafdır**: alfa əvəzinə `lerp` ilə səthin üstünə qarışdırılır, yoxsa `Dialog`
 * pəncərəsinin şəffaf fonundan ana səhifə görünürdü.
 */
@Composable
private fun storyBackground(index: Int): Brush {
    val accents = listOf(
        colorScheme.primaryContainer,
        colorScheme.tertiaryContainer,
        colorScheme.secondaryContainer,
        colorScheme.surfaceVariant,
    )

    val top = lerp(colorScheme.surface, accents[index % accents.size], 0.7f)

    return Brush.verticalGradient(listOf(top, colorScheme.surface, colorScheme.surface))
}
