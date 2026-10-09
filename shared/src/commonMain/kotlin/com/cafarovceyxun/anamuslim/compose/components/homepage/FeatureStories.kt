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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import com.cafarovceyxun.anamuslim.resources.strTitleVOTD
import com.cafarovceyxun.anamuslim.resources.storyAnnouncementsTitle
import com.cafarovceyxun.anamuslim.resources.suggestionsWhatsNew
import com.cafarovceyxun.anamuslim.api.NetworkConfig
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.IsoDate
import com.cafarovceyxun.anamuslim.utils.app.appPlatformId
import com.cafarovceyxun.anamuslim.utils.app.rememberRemoteImage
import com.cafarovceyxun.anamuslim.repository.supabase.StoryAnnouncementRepository
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.supabase.StoryAnnouncement
import com.cafarovceyxun.anamuslim.utils.supabase.Suggestion
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionLocalStore
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionMedia
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionStatus
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrayerPreferences
import com.cafarovceyxun.anamuslim.resources.lunarCalendarTitle
import com.cafarovceyxun.anamuslim.utils.supabase.LunarAnnouncement
import com.cafarovceyxun.anamuslim.viewModels.DailyContentViewModel
import com.cafarovceyxun.anamuslim.viewModels.LunarAnnouncementViewModel
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

    // Qəməri elanlar: ayarla söndürülmür — hekayə görünsə də görünməsə də tətbiq elanı oxuyub
    // qlobal gün düzəlişini tətbiq etməlidir (ViewModel bunu `init`-də özü edir).
    val lunarViewModel = viewModel { LunarAnnouncementViewModel() }
    val lunarAll by lunarViewModel.announcements.collectAsStateWithLifecycle()
    var seenLunarIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showLunarStory by remember { mutableStateOf(false) }

    // Göstəriləcək bir şeyi olmayan elan (nə media, nə qeyd) dairəni boş qoyardı.
    val lunarStories = remember(lunarAll) { lunarAll.filter { it.hasStory } }

    // Müstəqil hekayələr («Elanlar»). Vaxtı keçmişi RLS onsuz da gizlədir, amma admin hamısını
    // görür — ona görə burada da süzülür.
    val storyRepository = remember { StoryAnnouncementRepository() }
    var announcements by remember { mutableStateOf<List<StoryAnnouncement>>(emptyList()) }
    var seenAnnouncementIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var openAnnouncementIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        seenLunarIds = PrayerPreferences.seenLunarStoryIds()
        seenDailyIds = VersePreferences.seenStoryIds()
        seenIds = SuggestionLocalStore.seenFeatureIds()
        seenAnnouncementIds = SuggestionLocalStore.seenStoryIds()
        val now = currentEpochMillis()
        announcements = storyRepository.fetch().filter { it.hasStory && it.isActive(now) }
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
                // Zolaqda **ən yenisi birinci** — «Yeniliklər» hekayəsinin bütün mənası budur.
                // Serverin sırası (səs sayı, sonra tarix) təklif siyahısı üçündür: orada çox səs
                // almış köhnə təklif üstdə durur, burada isə yeni funksiya aylarla arxada qalırdı.
                // Sıralama ISO mətnin özü üzrədir — eyni formatlı zaman möhürləri leksikoqrafik
                // olaraq da xronoloji sıralanır (`SuggestionsScreen`-dəki «Ən yeni» ilə eyni üsul).
                .sortedByDescending { it.created_at.orEmpty() }
        }.onFailure {
            AppLogger.d("FeatureStories", "Fetch failed: ${it.message}")
        }.getOrDefault(emptyList())
    }

    if (features.isEmpty() && dailyItems.isEmpty() && lunarStories.isEmpty() && announcements.isEmpty()) return

    val dailyGroupLabel = stringResource(Res.string.strTitleVOTD)
    val lunarGroupLabel = stringResource(Res.string.lunarCalendarTitle)
    val featureGroupLabel = stringResource(Res.string.suggestionsWhatsNew)
    val announcementGroupLabel = stringResource(Res.string.storyAnnouncementsTitle)

    // ⚠️ Zolağın sürüşmə vəziyyəti **kənarda** saxlanılır və qrup önə əlavə olunanda sıfırlanır.
    //
    // «Yeniliklər» Supabase-dən, «Günün ayəsi» isə ayrı ViewModel-dən gəlir, yəni ikinci dalğa
    // çox vaxt zolaq artıq çəkildikdən sonra düşür. `LazyRow` sürüşməni **görünən ilk elementə**
    // bağlayır: yeni element onun önünə əlavə olunanda kadr yerində qalır, günün ayəsi isə sol
    // kənarda, ekrandan kənarda doğulurdu — istifadəçi tətbiqi açanda onu görmürdü («solda gizli
    // halda gəlir»). Ona görə başa əlavə olunan qrup sayı dəyişəndə zolaq başa qaytarılır.
    val rowState = rememberLazyListState()
    val leadingGroupCount = (if (dailyItems.isNotEmpty()) 1 else 0) +
        (if (lunarStories.isNotEmpty()) 1 else 0) +
        (if (announcements.isNotEmpty()) 1 else 0)

    LaunchedEffect(leadingGroupCount) {
        if (leadingGroupCount > 0) rowState.scrollToItem(0)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            state = rowState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (dailyItems.isNotEmpty()) {
                item(key = "daily-content") {
                    StoryGroupColumn(label = dailyGroupLabel, isGroupStart = true) {
                        DailyContentStoryCircle(
                            itemCount = dailyItems.size,
                            // Günün elementlərindən **hər hansı biri** baxılmayıbsa halqa yanır.
                            unseen = dailyItems.any { it.id !in seenDailyIds },
                            onClick = { showDailyStory = true },
                        )
                    }
                }
            }

            // «Günün ayəsinin yanında» — zolaqda ikinci qrup. Bütün aylar **bir** dairədədir:
            // 12 ayrı dairə zolağı doldurub günün ayəsini kənara itələyərdi, hekayə isə onsuz da
            // içəridə aydan-aya sürüşür.
            if (lunarStories.isNotEmpty()) {
                item(key = "lunar-calendar") {
                    StoryGroupColumn(label = lunarGroupLabel, isGroupStart = true) {
                        LunarStoryCircle(
                            latest = lunarStories.first(),
                            itemCount = lunarStories.size,
                            unseen = lunarStories.any { it.id !in seenLunarIds },
                            onClick = { showLunarStory = true },
                        )
                    }
                }
            }

            // «Elanlar» — qəməri qrupdan sonra, «Yeniliklər»-dən əvvəl. Hər elan öz dairəsidir
            // (bir elanda bir neçə slayd ola bilər), başlıq yalnız birincinin üstündədir.
            itemsIndexed(announcements, key = { _, item -> "announcement-${item.id}" }) { index, story ->
                StoryGroupColumn(label = announcementGroupLabel, isGroupStart = index == 0) {
                    AnnouncementStoryCircle(
                        story = story,
                        unseen = story.id !in seenAnnouncementIds,
                        onClick = { openAnnouncementIndex = index },
                    )
                }
            }

            itemsIndexed(features, key = { _, item -> item.id }) { index, feature ->
                StoryGroupColumn(label = featureGroupLabel, isGroupStart = index == 0) {
                    StoryCircle(
                        feature = feature,
                        unseen = feature.id !in seenIds,
                        onClick = { openIndex = index },
                    )
                }
            }
        }

        // Zolağı ana ekranın qalanından ayıran zolaq — hekayələr ən üstdə durduğu üçün altındakı
        // bölmə (adətən namaz vaxtları) onsuz zolağın davamı kimi oxunurdu.
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = colorScheme.outlineVariant.alpha(0.5f),
        )
    }

    if (showLunarStory) {
        LunarStoryViewer(
            announcements = lunarStories,
            startIndex = 0,
            onSeen = { id ->
                if (id !in seenLunarIds) {
                    seenLunarIds = seenLunarIds + id
                    scope.launch { PrayerPreferences.markLunarStorySeen(id) }
                    // Sayğac yalnız ilk baxışda artır — hər açılışda yox (funksiya hekayəsi ilə
                    // eyni qayda). Sayı ViewModel siyahıya geri yazır, ona görə rəqəm elə həmin
                    // baxışda yenilənir.
                    lunarViewModel.markViewed(id)
                }
            },
            onClose = { showLunarStory = false },
        )
    }

    if (showDailyStory) {
        DailyContentStoryViewer(
            items = dailyItems,
            onSeen = { id -> seenDailyIds = seenDailyIds + id },
            onClose = { showDailyStory = false },
        )
    }

    openAnnouncementIndex?.let { index ->
        val entries = remember(announcements) { announcements.map { it.toStoryEntry() } }
        StoryEntryViewer(
            entries = entries,
            title = announcementGroupLabel,
            kind = StoryKind.ANNOUNCEMENT,
            startIndex = index,
            onSeen = { id ->
                if (id !in seenAnnouncementIds) {
                    seenAnnouncementIds = seenAnnouncementIds + id
                    scope.launch {
                        SuggestionLocalStore.markStorySeen(id)
                        // Sayğac yalnız ilk baxışda artır — hər açılışda yox.
                        storyRepository.markViewed(id).onSuccess { count ->
                            announcements = announcements.map {
                                if (it.id == id) it.copy(view_count = count) else it
                            }
                        }
                    }
                }
            },
            onClose = { openAnnouncementIndex = null },
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

/**
 * Bir hekayə dairəsinin eni.
 *
 * Qrup başlığı ([StoryGroupColumn]) və günün ayəsi dairəsi ([DailyContentStoryCircle]) də buna
 * bağlıdır: üç yerdə ayrı-ayrı `72.dp` yazılsaydı biri dəyişəndə sıra səssizcə əyilərdi.
 */
internal val StoryCircleWidth = 72.dp

/**
 * Qrup başlığının dairədən nə qədər enli ola biləcəyi.
 *
 * Elementlər arasındakı boşluq 12dp-dir və qonşu başlıq öz qutusunda ortalandığı üçün onun da hər
 * tərəfində bir neçə dp ehtiyat qalır — bu qədər daşma başlıqları toqquşdurmur.
 */
private val StoryGroupLabelOverflow = 20.dp

/**
 * Bir hekayə dairəsi və onun **qrup başlığı**.
 *
 * Zolaqda iki qrup var — «Günün ayəsi» və «Yeniliklər» — və başlıq qrupun **birinci** dairəsinin
 * üstündə yazılır. Qalan dairələr eyni mətni görünməz saxlayır: başlığı ayrıca sətirdə çəksəydik
 * zolaq sürüşəndə yazı yerində qalıb səhv dairənin üstünə düşərdi, tamamilə atsaydıq isə birinci
 * dairə qalanlardan bir sətir hündür olub sıranı əyərdi. Görünməz nüsxə ekran oxuyucusundan da
 * gizlədilir ([clearAndSetSemantics]) — eşidilən tərəfdə başlıq bir dəfə səslənir.
 *
 * ⚠️ Elementin **tutduğu yer** dairənin eni qədərdir, başlıq isə ondan enli ola bilər: `width` +
 * `wrapContentWidth(unbounded = true)` cütü məhz bunu verir — sıra 72dp-lik addımla düzülür, mətn
 * lazım gələndə iki tərəfə, aradakı 12dp boşluğa daşır. Sadəcə `wrapContentWidth` olsaydı «Günün
 * Ayəsi» elementi genişləndirib dairələr arasındakı məsafəni qrupdan qrupa dəyişərdi; sadəcə
 * `width` olsaydı isə həmin başlıq «Günün Ayə…» kimi kəsilirdi (dairənin eninə güclə sığır).
 * Daşma [StoryGroupLabelOverflow] ilə hədlənir ki, uzun tərcümə qonşu başlığın üstünə çıxmasın.
 */
@Composable
private fun StoryGroupColumn(
    label: String,
    isGroupStart: Boolean,
    content: @Composable () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = typography.labelMedium.withContentDirection(),
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(StoryCircleWidth)
                .wrapContentWidth(unbounded = true)
                .widthIn(max = StoryCircleWidth + StoryGroupLabelOverflow)
                .then(if (isGroupStart) Modifier else Modifier.alpha(0f).clearAndSetSemantics {}),
        )

        Spacer(Modifier.height(6.dp))

        content()
    }
}

@Composable
private fun StoryCircle(
    feature: Suggestion,
    unseen: Boolean,
    onClick: () -> Unit,
) {
    StoryCircle(
        media = feature.media,
        caption = feature.body,
        fallbackLabel = stringResource(Res.string.suggestionsWhatsNew),
        unseen = unseen,
        onClick = onClick,
    )
}

/** Müstəqil hekayənin dairəsi — başlığı yoxdur, ona görə altda qeydin ilk sətri yazılır. */
@Composable
private fun AnnouncementStoryCircle(
    story: StoryAnnouncement,
    unseen: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(Res.string.storyAnnouncementsTitle)
    StoryCircle(
        media = story.media,
        caption = story.note?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() } ?: label,
        fallbackLabel = label,
        unseen = unseen,
        onClick = onClick,
    )
}

@Composable
private fun StoryCircle(
    media: List<SuggestionMedia>,
    caption: String,
    fallbackLabel: String,
    unseen: Boolean,
    onClick: () -> Unit,
) {
    // Dairədə şəkil göstərilir; media yalnız videodursa nişanla kifayətlənirik (kadr çıxarmaq
    // ayrıca dekodlama tələb edərdi və dairə üçün buna dəyməz).
    val image = rememberRemoteImage(media.firstOrNull { !it.isVideo }?.url)

    // Baxılmayanda tətbiqin öz yaşılından halqa; baxandan sonra halqa itir və dairənin yalnız
    // nazik kənarı qalır.
    val ringBrush = if (unseen) {
        Brush.linearGradient(listOf(colorScheme.primary, colorScheme.primary.alpha(0.45f)))
    } else {
        Brush.linearGradient(listOf(colorScheme.outlineVariant, colorScheme.outlineVariant))
    }

    Column(
        modifier = Modifier.width(StoryCircleWidth),
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
                    contentDescription = caption,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_feature),
                    contentDescription = fallbackLabel,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = caption,
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
internal fun FeatureStoryViewer(
    features: List<Suggestion>,
    startIndex: Int,
    onSeen: (Long) -> Unit,
    onClose: () -> Unit,
) {
    val entries = remember(features) { features.map { it.toStoryEntry() } }

    StoryEntryViewer(
        entries = entries,
        title = stringResource(Res.string.suggestionsWhatsNew),
        kind = StoryKind.FEATURE,
        startIndex = startIndex,
        onSeen = onSeen,
        onClose = onClose,
    )
}

/**
 * Bir hekayə dəstinin pleyerə lazım olan hissəsi — «Yeniliklər» (təklif) və «Elanlar»
 * (müstəqil hekayə) eyni pleyerdən keçir. [body] yalnız təklifdə var (təklifin öz mətni).
 */
internal data class StoryEntry(
    val id: Long,
    val media: List<SuggestionMedia>,
    val note: String?,
    val body: String?,
    val createdAt: String?,
    val viewCount: Int,
    val likeCount: Int,
)

internal fun Suggestion.toStoryEntry() = StoryEntry(
    id = id,
    media = media,
    note = note,
    body = body,
    createdAt = created_at,
    viewCount = view_count,
    likeCount = like_count,
)

internal fun StoryAnnouncement.toStoryEntry() = StoryEntry(
    id = id,
    media = media,
    note = note,
    body = null,
    createdAt = created_at,
    viewCount = view_count,
    likeCount = like_count,
)

/**
 * Tam ekran hekayə pleyeri — [title] üst zolaqda tarixlə birlikdə yazılır.
 */
@Composable
internal fun StoryEntryViewer(
    entries: List<StoryEntry>,
    title: String,
    kind: StoryKind,
    startIndex: Int,
    onSeen: (Long) -> Unit,
    onClose: () -> Unit,
) {
    var index by remember { mutableStateOf(startIndex.coerceIn(0, entries.lastIndex)) }
    var slide by remember { mutableStateOf(0) }
    val progress = remember { Animatable(0f) }

    // İki ayrı dayandırma: barmaq ekranda ([isHeldPaused]) və ortadan toxunuşla ([isTapPaused]).
    // Video da bu bayraqla dayanır — zolaq durub videonun oynaması mənasız olardı.
    var isHeldPaused by remember { mutableStateOf(false) }
    var isTapPaused by remember { mutableStateOf(false) }
    val isPaused = isHeldPaused || isTapPaused

    // Video slaydında kənarı basılı saxlamaq sarınmadır: sağ 2× irəli, sol 2× geri ([holdStory]).
    var playbackSpeed by remember { mutableStateOf(1f) }

    // Ortaya iki dəfə vurmaq bəyənir ([StoryLikes]). Ayrıca `onDoubleTap` qoyulmayıb: o, **bütün**
    // toxunuşları ikinci toxunuşu gözləməyə məcbur edərdi və sol/sağ keçid gecikərdi. Əvəzinə orta
    // toxunuş dərhal pauzadır, ikincisi pauzanı geri alıb bəyənir.
    val likes = rememberStoryLikes(kind)
    var likeBurst by remember { mutableIntStateOf(0) }
    var lastMiddleTapAt by remember { mutableLongStateOf(0L) }

    val current = entries.getOrNull(index) ?: return
    val media = current.media

    // Mediası olmayan təklif **bir** slayd kimi göstərilir: admin qeydi mətn kartı olur.
    val slideCount = maxOf(media.size, 1)
    val currentMedia = media.getOrNull(slide)
    // Jest bloku `pointerInput` ilə bir dəfə qurulur — slayd növünü köhnə dəyərlə görməsin.
    val currentIsVideo by rememberUpdatedState(currentMedia?.isVideo == true)
    val likeTarget by rememberUpdatedState(current)

    // Slayd dəyişəndə toxunuşla qoyulmuş pauza götürülür — yoxsa növbəti slayd donmuş zolaqla
    // açılardı (barmaqla dayandırma onsuz da buraxılanda bitir).
    val goNext: () -> Unit = {
        isTapPaused = false
        when {
            slide < slideCount - 1 -> slide++
            index < entries.lastIndex -> {
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
                slide = (entries[index].media.size - 1).coerceAtLeast(0)
            }
        }
    }

    // Videonun öz vaxtı: zolağı oynatma mövqeyi doldurur, animasiya yox.
    var videoProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(index) { onSeen(entries[index].id) }

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
                .pointerInput(entries.size) {
                    detectTapGestures(
                        // Basılı saxlamaq sayğacı (və videonu) dayandırır — slaydı oxumağa imkan
                        // verir; barmaq qaldırılanda qaldığı yerdən davam edir.
                        onPress = { offset ->
                            holdStory(
                                offset = offset,
                                width = size.width,
                                isVideo = currentIsVideo,
                                longPressMillis = viewConfiguration.longPressTimeoutMillis,
                                onHoldPause = { isHeldPaused = it },
                                onSpeed = { playbackSpeed = it },
                            )
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
                                else -> {
                                    val now = currentEpochMillis()
                                    isTapPaused = !isTapPaused
                                    if (now - lastMiddleTapAt <= viewConfiguration.doubleTapTimeoutMillis) {
                                        lastMiddleTapAt = 0L
                                        likes.like(likeTarget.id, likeTarget.likeCount)
                                        likeBurst++
                                    } else {
                                        lastMiddleTapAt = now
                                    }
                                }
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
                    playbackSpeed = playbackSpeed,
                    onProgress = { videoProgress = it },
                    onFinished = goNext,
                )
            } else {
                val image = rememberRemoteImage(currentMedia.url)

                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = current.body ?: current.note,
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

            LikeBurst(trigger = likeBurst, modifier = Modifier.align(Alignment.Center))

            if (playbackSpeed != 1f && currentMedia?.isVideo == true) {
                SeekBadge(
                    speed = playbackSpeed,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(top = 64.dp, end = 16.dp),
                )
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
                    // Başlıq + tarix bir sətirdə: hekayə zolağında dairələr yalnız şəkil göstərir,
                    // ona görə «bu yenilik nə vaxt gəldi» sualının cavabı yalnız burada var.
                    // Tarix yoxdursa (köhnə sətirlərdə `created_at` boş ola bilər) ayırıcı da
                    // yazılmır — «Yeniliklər ·» quyruğu qalmasın.
                    Text(
                        text = listOfNotNull(
                            title,
                            current.createdAt?.takeIf { it.isNotBlank() }?.let { IsoDate.display(it) },
                        ).joinToString(" · "),
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

                // Təklifin öz mətni — müstəqil hekayədə yoxdur.
                current.body?.takeIf { it.isNotBlank() }?.let { body ->
                    Text(
                        text = body,
                        style = typography.bodyLarge.withContentDirection().copy(
                            fontSize = 16.sp * textScale,
                            lineHeight = 23.sp * textScale,
                        ),
                        color = Color.White.alpha(0.92f),
                    )

                    Spacer(Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LikeButton(
                        liked = likes.isLiked(current.id),
                        count = likes.count(current.id, current.likeCount),
                        textScale = textScale,
                        onClick = { likes.toggle(current.id, current.likeCount) },
                    )

                    Spacer(Modifier.weight(1f))

                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_eye),
                        contentDescription = stringResource(Res.string.suggestionsViews),
                        tint = Color.White.alpha(0.75f),
                        modifier = Modifier.size(16.dp),
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = current.viewCount.toString(),
                        style = typography.labelMedium.copy(fontSize = 13.sp * textScale),
                        fontWeight = FontWeight.Bold,
                        color = Color.White.alpha(0.75f),
                    )
                }
            }
        }
    }
}
