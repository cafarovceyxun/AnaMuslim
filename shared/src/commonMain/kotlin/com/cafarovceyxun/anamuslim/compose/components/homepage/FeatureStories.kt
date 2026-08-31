package com.cafarovceyxun.anamuslim.compose.components.homepage

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.compose.theme.LocalAppTextScale
import com.cafarovceyxun.anamuslim.resources.dr_icon_eye
import com.cafarovceyxun.anamuslim.resources.suggestionsViews
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.components.common.StoryVideo
import com.cafarovceyxun.anamuslim.compose.components.settings.withContentDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.preferences.VersePreferences
import com.cafarovceyxun.anamuslim.repository.supabase.SuggestionRepository
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_feature
import com.cafarovceyxun.anamuslim.resources.strDescClose
import com.cafarovceyxun.anamuslim.resources.suggestionsWhatsNew
import com.cafarovceyxun.anamuslim.api.NetworkConfig
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.app.appPlatformId
import com.cafarovceyxun.anamuslim.utils.app.rememberRemoteImage
import com.cafarovceyxun.anamuslim.utils.supabase.Suggestion
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionLocalStore
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionStatus
import com.cafarovceyxun.anamuslim.viewModels.DailyContentViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val STORY_DURATION_MILLIS = 6000

/**
 * Ana səhifənin ən yuxarısındakı hekayə zolağı.
 *
 * **Birinci dairə həmişə günün ayəsi/hədisidir** ([DailyContentStoryCircle]) — gündəlik məzmun
 * tətbiqin əsas vədidir, ona görə zolağın başında durur və içində günün bütün elementləri
 * bildirişlərlə eyni sıra ilə açılır. Ondan sonra «Yeniliklər» gəlir:
 *
 * Yalnız **tamamlanmış** təkliflər düşür və göstəriləcək bir şeyi olanlar: ya media (şəkil/video),
 * ya da admin qeydi ([Suggestion.hasStory]). Mediası olmayan təklifin qeydi mətn slaydı kimi
 * oynayır — «funksiya buradadır» izahı hekayənin bütün mənasıdır, onu şəkil çatmadığına görə
 * itirmirik; ikisi də yoxdursa dairə boş qalardı, ona görə belə təklif zolağa düşmür.
 *
 * Baxılmamışın ətrafında tətbiqin yaşıl halqası olur, baxandan sonra halqa itir — baxılma
 * vəziyyəti **cihazda** saxlanılır ([SuggestionLocalStore]), serverdə istifadəçi kimliyi yoxdur.
 *
 * Şəbəkə çatmasa zolaq sadəcə görünmür.
 */
@Composable
fun FeatureStoriesRow() {
    val repository = remember { SuggestionRepository() }
    val scope = rememberCoroutineScope()

    // Ayarla söndürüləndə nə ViewModel qurulur, nə də Supabase sorğusu gedir (Ayarlar → Günün
    // ayəsi hekayəsi). Açar köhnə kartdan qalıb, mənası eynidir: gündəlik məzmun ana səhifədə
    // görünsünmü.
    val dailyStoryEnabled = VersePreferences.observeVOTDCardEnabled()

    val dailyItems = if (dailyStoryEnabled) {
        val dailyContentViewModel = viewModel { DailyContentViewModel() }
        dailyContentViewModel.todayItems.collectAsStateWithLifecycle().value
    } else {
        emptyList()
    }

    var features by remember { mutableStateOf<List<Suggestion>>(emptyList()) }
    var seenIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var openIndex by remember { mutableStateOf<Int?>(null) }
    var showDailyStory by remember { mutableStateOf(false) }
    var seenDailyIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    LaunchedEffect(Unit) {
        seenDailyIds = VersePreferences.seenStoryIds()
        seenIds = SuggestionLocalStore.seenFeatureIds()
        val versionName = NetworkConfig.appVersionName()

        features = runCatching {
            repository.fetchApproved()
                // Şəkilsiz/videosuz təklif də hekayəyə düşür — admin qeydi varsa. Qeyd elə
                // «funksiya haradadır» izahıdır, ona görə mətn slaydı kimi göstərilir.
                //
                // Görünmə şərti klientdə süzülür: funksiya bu platformada varmı və istifadəçinin
                // quraşdırdığı buraxılışa düşübmü. Yoxsa 30-cu buraxılışdakı istifadəçi 31-də
                // gələn funksiyanın hekayəsini görüb tətbiqdə tapmazdı.
                .filter {
                    it.status == SuggestionStatus.DONE &&
                        it.hasStory &&
                        it.isVisibleOn(appPlatformId, versionName)
                }
        }.onFailure {
            AppLogger.d("FeatureStories", "Fetch failed: ${it.message}")
        }.getOrDefault(emptyList())
    }

    if (features.isEmpty() && dailyItems.isEmpty()) return

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (dailyItems.isNotEmpty()) {
            item(key = "daily-content") {
                DailyContentStoryCircle(
                    itemCount = dailyItems.size,
                    // Günün elementlərindən **hər hansı biri** baxılmayıbsa halqa yanır.
                    unseen = dailyItems.any { it.id !in seenDailyIds },
                    onClick = { showDailyStory = true },
                )
            }
        }

        itemsIndexed(features, key = { _, item -> item.id }) { index, feature ->
            StoryCircle(
                feature = feature,
                unseen = feature.id !in seenIds,
                onClick = { openIndex = index },
            )
        }
    }

    if (showDailyStory) {
        DailyContentStoryViewer(
            items = dailyItems,
            onSeen = { id -> seenDailyIds = seenDailyIds + id },
            onClose = { showDailyStory = false },
        )
    }

    openIndex?.let { index ->
        FeatureStoryViewer(
            features = features,
            startIndex = index,
            onSeen = { id ->
                if (id !in seenIds) {
                    seenIds = seenIds + id
                    scope.launch {
                        SuggestionLocalStore.markFeatureSeen(id)
                        // Sayğac yalnız ilk baxışda artır — hər açılışda yox.
                        repository.markViewed(id).onSuccess { count ->
                            features = features.map {
                                if (it.id == id) it.copy(view_count = count) else it
                            }
                        }
                    }
                }
            },
            onClose = { openIndex = null },
        )
    }
}

@Composable
private fun StoryCircle(
    feature: Suggestion,
    unseen: Boolean,
    onClick: () -> Unit,
) {
    // Dairədə şəkil göstərilir; media yalnız videodursa nişanla kifayətlənirik (kadr çıxarmaq
    // ayrıca dekodlama tələb edərdi və dairə üçün buna dəyməz).
    val image = rememberRemoteImage(feature.media.firstOrNull { !it.isVideo }?.url)

    // Baxılmayanda tətbiqin öz yaşılından halqa; baxandan sonra halqa itir və dairənin yalnız
    // nazik kənarı qalır.
    val ringBrush = if (unseen) {
        Brush.linearGradient(listOf(colorScheme.primary, colorScheme.primary.alpha(0.45f)))
    } else {
        Brush.linearGradient(listOf(colorScheme.outlineVariant, colorScheme.outlineVariant))
    }

    Column(
        modifier = Modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .border(width = if (unseen) 2.5.dp else 1.dp, brush = ringBrush, shape = CircleShape)
                .padding(if (unseen) 4.dp else 3.dp)
                .clip(CircleShape)
                .background(colorScheme.surfaceContainerHigh)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = feature.body,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_feature),
                    contentDescription = stringResource(Res.string.suggestionsWhatsNew),
                    tint = colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = feature.body,
            style = typography.labelSmall.withContentDirection(),
            color = colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Mediası olmayan təklifin slaydı: admin qeydi kadrın **özüdür**, şəklin üstündəki yazı yox.
 *
 * Fon tətbiqin öz rəngindən qaralığa keçir — üstdəki zolaq və altdakı lövhə eyni qaydada oxunur,
 * yəni mətn hekayəsi qalan slaydlarla eyni kadr quruluşunu saxlayır.
 */
@Composable
private fun TextStorySlide(note: String) {
    val textScale = LocalAppTextScale.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colorScheme.primary.alpha(0.55f),
                        colorScheme.primary.alpha(0.16f),
                        Color.Black,
                    ),
                ),
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 28.dp, vertical = 80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = note,
            style = typography.headlineSmall.withContentDirection().copy(
                fontSize = 24.sp * textScale,
                lineHeight = 34.sp * textScale,
            ),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Tam ekran hekayə baxışı. `Dialog` olaraq açılır (inline emit yox) — bax CLAUDE.md, «Tam ekran
 * səth `Dialog` olmalıdır»: ana səhifə gələcəkdə modal vərəqin altından da göstərilə bilər.
 */
@Composable
private fun FeatureStoryViewer(
    features: List<Suggestion>,
    startIndex: Int,
    onSeen: (Long) -> Unit,
    onClose: () -> Unit,
) {
    var index by remember { mutableStateOf(startIndex.coerceIn(0, features.lastIndex)) }
    var slide by remember { mutableStateOf(0) }
    val progress = remember { Animatable(0f) }

    // İki ayrı dayandırma: barmaq ekranda ([isHeldPaused]) və ortadan toxunuşla ([isTapPaused]).
    // Video da bu bayraqla dayanır — zolaq durub videonun oynaması mənasız olardı.
    var isHeldPaused by remember { mutableStateOf(false) }
    var isTapPaused by remember { mutableStateOf(false) }
    val isPaused = isHeldPaused || isTapPaused

    val current = features.getOrNull(index) ?: return
    val media = current.media

    // Mediası olmayan təklif **bir** slayd kimi göstərilir: admin qeydi mətn kartı olur.
    val slideCount = maxOf(media.size, 1)
    val currentMedia = media.getOrNull(slide)

    // Slayd dəyişəndə toxunuşla qoyulmuş pauza götürülür — yoxsa növbəti slayd donmuş zolaqla
    // açılardı (barmaqla dayandırma onsuz da buraxılanda bitir).
    val goNext: () -> Unit = {
        isTapPaused = false
        when {
            slide < slideCount - 1 -> slide++
            index < features.lastIndex -> {
                index++
                slide = 0
            }

            else -> onClose()
        }
    }

    val goPrevious: () -> Unit = {
        isTapPaused = false
        when {
            slide > 0 -> slide--
            index > 0 -> {
                index--
                slide = (features[index].media.size - 1).coerceAtLeast(0)
            }
        }
    }

    // Videonun öz vaxtı: zolağı oynatma mövqeyi doldurur, animasiya yox.
    var videoProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(index) { onSeen(features[index].id) }

    LaunchedEffect(index, slide) { videoProgress = 0f }

    // Şəkil və mətn slaydı sabit müddət qalır; video isə öz uzunluğu qədər oynayır və `onFinished`
    // ilə keçir, ona görə videoda taymer işə salınmır. Dayandırma sıfırlamır — sayğac qaldığı
    // yerdən davam edir (günün hekayəsi ilə eyni davranış).
    LaunchedStoryProgress(
        key = index to slide,
        running = !isPaused && currentMedia?.isVideo != true,
        durationMillis = STORY_DURATION_MILLIS,
        progress = progress,
        onFinished = goNext,
    )

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(features.size) {
                    detectTapGestures(
                        // Basılı saxlamaq sayğacı (və videonu) dayandırır — slaydı oxumağa imkan
                        // verir; barmaq qaldırılanda qaldığı yerdən davam edir.
                        onPress = {
                            isHeldPaused = true
                            tryAwaitRelease()
                            isHeldPaused = false
                        },
                        // `onLongPress` verilməsə uzun basışın buraxılışı da `onTap` sayılır və
                        // hekayə oxunub-bitirilən kimi növbəti slayda tullanırdı.
                        onLongPress = {},
                        // Üç zolaq: sol → əvvəlki, sağ → növbəti, **orta → pauza**. Orta zolaq
                        // basılı saxlamadan fərqlidir: barmaq qaldırılanda da dayanmış qalır.
                        onTap = { offset ->
                            when {
                                offset.x < size.width / 3f -> goPrevious()
                                offset.x > size.width * 2 / 3f -> goNext()
                                else -> isTapPaused = !isTapPaused
                            }
                        },
                    )
                }
                .pointerInput(Unit) {
                    var dragged = 0f

                    // Aşağı sürüşdürmə hekayəni bağlayır — günün hekayəsindəki jestin eynisi.
                    detectVerticalDragGestures(
                        onDragStart = { dragged = 0f },
                        onDragEnd = { if (dragged > STORY_DISMISS_DRAG_PX) onClose() },
                        onDragCancel = { dragged = 0f },
                        onVerticalDrag = { _, delta -> dragged += delta },
                    )
                },
        ) {
            if (currentMedia == null) {
                TextStorySlide(note = current.note.orEmpty())
            } else if (currentMedia.isVideo) {
                StoryVideo(
                    url = currentMedia.url,
                    modifier = Modifier.fillMaxSize(),
                    paused = isPaused,
                    onProgress = { videoProgress = it },
                    onFinished = goNext,
                )
            } else {
                val image = rememberRemoteImage(currentMedia.url)

                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = current.body,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center).size(28.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.alpha(0.55f), Color.Transparent),
                        ),
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 28.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(slideCount) { i ->
                        val fill = when {
                            i < slide -> 1f
                            i > slide -> 0f
                            media.getOrNull(i)?.isVideo == true -> videoProgress
                            else -> progress.value
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.alpha(0.35f)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fill)
                                    .height(3.dp)
                                    .background(Color.White),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.suggestionsWhatsNew),
                        style = typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )

                    IconButton(
                        painter = painterResource(Res.drawable.dr_icon_close),
                        contentDescription = stringResource(Res.string.strDescClose),
                        tint = Color.White,
                        small = true,
                        onClick = onClose,
                    )
                }
            }

            // Alt lövhə: admin qeydi + təklifin mətni + baxış sayı. Ölçülər ayarlardakı «Tətbiq
            // mətninin ölçüsü» xətkeşinə bağlıdır ([LocalAppTextScale]), yəni hekayə də qalan
            // interfeyslə birlikdə böyüyüb kiçilir.
            val textScale = LocalAppTextScale.current

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    // Düz yarımşəffaf lövhə şəklin öz düymələrini mətnin arxasından keçirirdi.
                    // Qradiyent yuxarıda tamamilə şəffafdır, mətnin altında isə demək olar tutqun —
                    // şəkil kəsilmir, yazı isə həmişə oxunur.
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.alpha(0.75f),
                                Color.Black.alpha(0.92f),
                            ),
                        ),
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 16.dp),
            ) {
                // Mətn slaydında qeyd onsuz da kadrın ortasındadır — altda təkrarlanmır.
                current.note?.takeIf { it.isNotBlank() && currentMedia != null }?.let { note ->
                    Text(
                        text = note,
                        style = typography.titleSmall.withContentDirection().copy(
                            fontSize = 17.sp * textScale,
                            lineHeight = 24.sp * textScale,
                        ),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )

                    Spacer(Modifier.height(6.dp))
                }

                Text(
                    text = current.body,
                    style = typography.bodyLarge.withContentDirection().copy(
                        fontSize = 16.sp * textScale,
                        lineHeight = 23.sp * textScale,
                    ),
                    color = Color.White.alpha(0.92f),
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.weight(1f))

                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_eye),
                        contentDescription = stringResource(Res.string.suggestionsViews),
                        tint = Color.White.alpha(0.75f),
                        modifier = Modifier.size(16.dp),
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = current.view_count.toString(),
                        style = typography.labelMedium.copy(fontSize = 13.sp * textScale),
                        fontWeight = FontWeight.Bold,
                        color = Color.White.alpha(0.75f),
                    )
                }
            }
        }
    }
}
