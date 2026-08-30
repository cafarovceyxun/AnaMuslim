package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Oxuma ekranlarında səhifə dəyişməsinin **görünüş effekti** — Quran oxucusunun üç vərəqləyicisi
 * (müshəf, tərcümə, kitab) və hədis oxucusunun bab keçidi eyni siyahıdan seçir. Ona görə seçim
 * [com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences]-dədir və ayar sətri
 * `ReaderSharedSettingsGroup`-dadır — hər iki oxucunun ayar vərəqi onu göstərir.
 *
 * Default [Zoom]-dur: effektlərin **ən yüngülüdür** — səhifəni yerində saxlamır, üçölçülü fırlanma
 * qurmur, yalnız ölçü və qatılıqla oynayır. [Standard] isə effekti tam söndürür; müshəf səhifəsi
 * ağır çəkildiyi üçün bu seçim də saxlanılıb.
 */
enum class PageTurnAnimation(val value: String) {
    /** Vərəqləyicinin öz sürüşməsi — heç bir əlavə transformasiya yoxdur. */
    Standard("standard"),

    /** Kitab vərəqi: səhifə tikişin üstündə qalxıb çevrilir, yerindən tərpənmir. */
    Book("book"),

    /** Kub: səhifələr fırlanan kubun iki üzü kimi — həm fırlanır, həm sürüşür. */
    Cube("cube"),

    /** Dərinlik: arxada qalan səhifə yerində kiçilib solur, gələn onun üstündən sürüşür. */
    Depth("depth"),

    /** Yaxınlaşma: səhifələr keçidin ortasında kiçilir və solur, sürüşmə isə adi qalır. */
    Zoom("zoom"),

    /** Solğunlaşma: səhifələr yerindən tərpənmir, biri o birinə keçir. */
    Fade("fade");

    companion object {
        val DEFAULT = Depth

        fun fromValue(value: String?): PageTurnAnimation =
            entries.firstOrNull { it.value == value } ?: DEFAULT
    }
}

/** Kitab/kub effektlərində fırlanma bucağı — 90°-də səhifə tam yan görünür. */
private const val MaxFlipDegrees = 90f

/** Perspektiv dərinliyi. Kiçik dəyər fırlanmanı karikatura kimi əyir, böyük dəyər düzləşdirir. */
private const val FlipCameraDistance = 18f

/** Fırlanan vərəqin üstünə düşən kölgə — işıqdan uzaqlaşan səhifənin qarşılığı. */
private const val FlipScrimAlpha = 0.35f

/**
 * Solğunlaşmada gələn səhifənin tam qatılığa çatdığı yol payı. Vahiddən kiçikdir ki, iki mətnin
 * eyni anda oxunduğu aralıq jestin ortasına qədər bitsin.
 */
private const val FadeRamp = 0.45f

private const val DepthMinScale = 0.75f
private const val ZoomMinScale = 0.85f
private const val ZoomMinAlpha = 0.5f

/** Hədis oxucusunda bab keçidinin uzunluğu. Vərəqləyicidə müddəti barmaq təyin edir. */
private const val EnterDurationMillis = 340

/** Yeni babın məzmununu bu qədər gözləyirik; gəlməsə effekt onsuz da oynayır. */
private const val ContentWaitTimeoutMillis = 400L

/**
 * Vərəqləyici səhifəsinin effekt modifikatoru.
 *
 * Sürüşmə payı [pagerState]-dən **çəkilmə anında** oxunur (`graphicsLayer`-in blok forması), ona
 * görə jest boyu rekompozisiya olmur — yalnız `currentPage` dəyişəndə, o da səhifə başına bir dəfə.
 *
 * ⚠️ Pay **indeks üzrədir, fiziki istiqamət üzrə yox**: müshəf və kitab vərəqləyiciləri RTL
 * bükümündədir, orada növbəti səhifə sola yox, sağa gedir. Ona görə üfüqi hər dəyər
 * [LocalLayoutDirection]-dan gələn əmsala vurulur.
 */
@Composable
fun Modifier.pageTurnEffect(
    animation: PageTurnAnimation,
    pagerState: PagerState,
    page: Int,
    ground: Color,
): Modifier {
    if (animation == PageTurnAnimation.Standard) return this

    val direction = layoutDirectionFactor()

    // Vərəqləyici səhifələri indeks sırası ilə çəkir, yəni gələn səhifə gedənin **üstünə** düşür.
    // Bəzi effektlərdə sıra tərsinə lazımdır — vərəq altdakını açmalıdır, solan səhifə isə altdakı
    // tam görünən səhifəni örtməlidir. Dəyər yalnız `currentPage` dəyişəndə hesablanır: jest ərzində
    // iki səhifə eyni tərəfdə qaldığı üçün sabitdir.
    val ordered = if (animation.drawsOutgoingPageOnTop) {
        this.zIndex((pagerState.currentPage - page).toFloat())
    } else {
        this
    }

    val offsetOf: () -> Float = {
        ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
            .coerceIn(-1f, 1f)
    }

    return ordered
        .graphicsLayer { applyPagerPageTurn(animation, offsetOf(), direction) }
        // Effekt səhifələri bir-birinin üstünə yığır, səhifənin isə öz fonu yoxdur — bax
        // [ReaderMode.groundColor]. Fon `graphicsLayer`-in **içindədir**, ona görə səhifə ilə
        // birlikdə fırlanır və sürüşür.
        .background(ground)
        // Kölgə yalnız fırlanan (arxada qalan) səhifədədir; altdan açılan səhifə tutqunlaşmır.
        .flipScrim(animation) { offsetOf().coerceAtLeast(0f) }
}

/**
 * Bab keçidini **ekran nüsxələri arasında** daşıyan siqnal.
 *
 * Hədis oxucusunda sürüşmə jesti naviqasiya hədəfini dəyişir: `MainScreen` onu `popUpTo(...)
 * { inclusive = true }` ilə **əvəz edir**, `AppNavHost` isə yenisini üstünə qoyur. Hər iki halda
 * köhnə `HadithItemsScreen` dağılır və yenisi sıfırdan qurulur — yəni ekranın içindəki `remember`
 * (animasiya vəziyyəti, «əvvəlki bab hansı idi») itir və yeni nüsxə üçün yeni bab **ilk** babdır,
 * dəyişiklik kimi görünmür. Effektin hədisdə heç işləməməsinin səbəbi məhz bu idi.
 *
 * Ona görə keçidin faktı ekrandan kənarda, bir addımlıq qutuda saxlanılır: gedən nüsxə [request]
 * edir, növbəti nüsxə [consume] ilə bir dəfə götürür.
 *
 * `ActivityHadith` isə ekranı yerində saxlayır (naviqasiya yoxdur, vəziyyət dəyişir) — orada bu
 * qutu boş qalır və effekt bab kimliyinin dəyişməsindən işə düşür. İki mexanizm bir-birini əvəz
 * etmir, iki ayrı host davranışını örtür.
 */
object PageTurnHandoff {
    private var pendingForward: Boolean? = null

    /** Sürüşmə/düymə keçidi naviqasiyanı çağırmazdan **əvvəl** işarə qoyur. */
    fun request(forward: Boolean) {
        pendingForward = forward
    }

    /**
     * Növbəti ekran nüsxəsi bir dəfə oxuyur; ikinci çağırışda `null` qayıdır ki, köhnə işarə
     * sonrakı açılışlarda təkrar oynamasın.
     */
    fun consume(): Boolean? = pendingForward.also { pendingForward = null }
}

/**
 * Hədis oxucusunun bab keçidi — **yalnız gələn** məzmun canlandırılır.
 *
 * Vərəqləyicidən fərqli olaraq burada iki səhifə eyni anda mövcud deyil: bab dəyişəndə siyahı öz
 * yerində yenilənir və `LazyListState` birdir, ona görə iki nüsxəni yan-yana göstərən keçid
 * (`AnimatedContent`) sürüşmə vəziyyətini iki siyahı arasında bölərdi. Bunun əvəzinə yeni bab
 * effektin son kadrından öz yerinə gəlir.
 *
 ## Niyə belə qurulub
 *
 * Bab keçidi Quran vərəqləyicisindən **iki** cəhətdən fərqlidir və hər biri bir qolla həll olunur:
 *
 * 1. **Ekran dağılır.** `MainScreen`/`AppNavHost` sürüşməni naviqasiya kimi işlədir və köhnə
 *    `HadithItemsScreen`-i məhv edir. Ekranın içindəki `remember` (istiqamət, «əvvəlki bab») itir,
 *    ona görə istiqamət ekrandan kənarda [PageTurnHandoff]-da daşınır. `ActivityHadith` isə ekranı
 *    yerində saxlayır — orada siqnal [key]-in dəyişməsidir.
 * 2. **Məzmun ani hazır olmaya bilər.** Quran vərəqləyicisi qonşu səhifəni öncədən qurur; hədisdə
 *    isə bu, `HadithViewModel`-in qonşu bab keşi ilə təqlid olunur (`prefetchHadiths`) — qonşu
 *    bablar keşdən **sinxron** gəlir, yəni sürüşəndə qaralma olmur. [contentReady] «göstərilən
 *    siyahı boş deyil VƏ məhz **cari** baba aiddir» deməkdir (ekran `loadedHadithKey` ilə hesablayır),
 *    ona görə yenidən qurulmuş ekranda bir an qalan **köhnə** məzmun «hazır» sayılmır və effekt onun
 *    üstündə oynamaz. Keşdən gələn yeni bab isə dərhal hazırdır və effekt Quran kimi oynayır.
 *
 * Məzmun nədənsə hazır olmasa [ContentWaitTimeoutMillis] sonra effekt onsuz da oynayır ki, ekran
 * qaralı qalmasın.
 *
 * [key] bab kimliyidir. [forward] `true` olanda keçid növbəti baba, `false` olanda əvvəlkinə
 * oxunur. [contentReady] — cari babın əsl (boş olmayan) siyahısı ekrandadırmı.
 */
@Composable
fun Modifier.pageTurnEnterEffect(
    animation: PageTurnAnimation,
    key: Any?,
    forward: Boolean,
    contentReady: Boolean,
): Modifier {
    if (animation == PageTurnAnimation.Standard) return this

    val direction = layoutDirectionFactor()
    val progress = remember { Animatable(1f) }

    var seenKey by remember { mutableStateOf(key) }

    // Animasiya boyu sabit qalan istiqamət: oynatma başlayanda təsbit olunur, yoxsa yeni ekranın
    // `forward` default dəyəri keçidin ortasında istiqaməti çevirə bilər.
    var playForward by remember { mutableStateOf(true) }

    // pending != null: keçid qurulub, məzmunun hazır olmasını gözləyir.
    var pendingForward by remember { mutableStateOf<Boolean?>(null) }
    var playToken by remember { mutableStateOf(0) }

    // Host ekranı naviqasiya ilə yenidən qurubsa, keçidi əvvəlki nüsxə [PageTurnHandoff]-da qoyub.
    LaunchedEffect(Unit) {
        val handoff = PageTurnHandoff.consume() ?: return@LaunchedEffect
        pendingForward = handoff
    }

    // Host ekranı yerində saxlayırsa naviqasiya yoxdur — bab kimliyinin dəyişməsi siqnaldır.
    // Sızmış handoff-u da udur: in-place halda `request(...)` çağırılıb, amma yeni nüsxə olmadığı
    // üçün onu heç kim götürməyəcək — burada təmizlənir ki, sonrakı təmiz açılışda oynamasın.
    LaunchedEffect(key) {
        if (key == seenKey) return@LaunchedEffect
        seenKey = key
        PageTurnHandoff.consume()
        pendingForward = forward
    }

    // Keçid qurulan kimi səhifə gizlənir — bu, keçidin başlanğıc kadrıdır. Gizlətməni məzmun
    // gələnə saxlasaq, yeni siyahı bir kadr tam görünər, sonra yox olar və animasiya yenidən
    // gətirər — «əvvəl dəyişdi, sonra oynadı» görünüşü buradan gəlirdi.
    LaunchedEffect(pendingForward) {
        if (pendingForward != null) progress.snapTo(0f)
    }

    // Oynatma: yeni babın əsl məzmunu ekrana çıxan kimi. [contentReady] «boş deyil VƏ cari baba
    // aiddir» deməkdir (ekran tərəfində hesablanır), ona görə burada əlavə yoxlama lazım deyil —
    // qonşu bablar keşdən ANİ gəldiyi üçün bu şərt çox vaxt dərhal ödənir və effekt Quran kimi
    // qaralmadan oynayır. Keşsiz halda isə məzmun bazadan gələn kimi.
    LaunchedEffect(pendingForward, contentReady) {
        val fwd = pendingForward ?: return@LaunchedEffect
        if (!contentReady) return@LaunchedEffect
        playForward = fwd
        pendingForward = null
        playToken++
    }

    // Ehtiyat: məzmun nədənsə hazır olmadı. Gözləmə həddindən sonra onsuz da oyna ki, ekran
    // qaralı qalmasın.
    LaunchedEffect(pendingForward) {
        if (pendingForward == null) return@LaunchedEffect
        delay(ContentWaitTimeoutMillis)
        val fwd = pendingForward ?: return@LaunchedEffect
        playForward = fwd
        pendingForward = null
        playToken++
    }

    // Oynatma yalnız nişandan asılıdır: məzmunun sonrakı yenilənmələri işləyən animasiyanı
    // ləğv etməsin. `finally` hər halda geri qaytarır — ekran heç vaxt yarımçıq gizli qalmır.
    LaunchedEffect(playToken) {
        if (playToken == 0) return@LaunchedEffect
        try {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(EnterDurationMillis, easing = FastOutSlowInEasing),
            )
        } finally {
            withContext(NonCancellable) { progress.snapTo(1f) }
        }
    }

    // Səhifənin qət etməli olduğu yol: 1 — hələ başlanğıcda, 0 — yerində.
    val travelOf: () -> Float = { (1f - progress.value).coerceIn(0f, 1f) }

    return this
        .graphicsLayer {
            applyEnterPageTurn(animation, travelOf(), playForward, direction)
        }
        .flipScrim(animation) { travelOf() }
}

@Composable
private fun layoutDirectionFactor(): Float =
    if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f

/** Arxada qalan səhifəni gələnin üstündə çəkməli olan effektlər. */
private val PageTurnAnimation.drawsOutgoingPageOnTop: Boolean
    get() = this == PageTurnAnimation.Book || this == PageTurnAnimation.Cube

/**
 * Fırlanan vərəqin üstündəki kölgə. `graphicsLayer` rəng qata bilmir, ona görə ayrıca çəkilir;
 * kitabdan başqa effektlərdə modifikator zəncirə heç əlavə olunmur.
 */
private fun Modifier.flipScrim(
    animation: PageTurnAnimation,
    amountOf: () -> Float,
): Modifier {
    if (animation != PageTurnAnimation.Book) return this

    return this.drawWithContent {
        drawContent()
        val scrim = amountOf().coerceIn(0f, 1f) * FlipScrimAlpha
        if (scrim > 0f) drawRect(color = Color.Black.copy(alpha = scrim))
    }
}

/**
 * Vərəqləyici effekti. [offset] indeks üzrə sürüşmə payıdır: `0` — səhifə tam yerindədir, müsbət —
 * səhifə indeksi cari səhifədən **kiçikdir** (arxada qalan), mənfi — böyükdür (gələn).
 * [direction] RTL əmsalıdır.
 */
private fun GraphicsLayerScope.applyPagerPageTurn(
    animation: PageTurnAnimation,
    offset: Float,
    direction: Float,
) {
    val distance = abs(offset)

    // Səhifəni vərəqləyicinin sürüşməsindən azad edir: bundan sonra səhifə öz yuvasında dayanır və
    // bütün hərəkəti effektin özü verir.
    fun holdInPlace() {
        translationX = size.width * offset * direction
    }

    when (animation) {
        PageTurnAnimation.Standard -> Unit

        PageTurnAnimation.Book -> {
            holdInPlace()
            // Yalnız arxada qalan səhifə vərəq kimi qalxır; gələn səhifə onun altından açılır.
            if (offset <= 0f) return
            transformOrigin = TransformOrigin(if (direction > 0f) 0f else 1f, 0.5f)
            cameraDistance = FlipCameraDistance
            rotationY = -MaxFlipDegrees * offset * direction
        }

        PageTurnAnimation.Cube -> {
            holdInPlace()
            // Hər səhifə ekranın öz tərəfindəki kənarında **arxaya** qatlanır, ona görə ikisi
            // içəri baxan kubun iki üzü kimi ekranı bölür və üst-üstə düşmür. Fırlanma işarəsi
            // kitab vərəqinin əksidir: vərəq oxucuya tərəf qalxır, kub üzü isə içəri gedir.
            val edgeOnLeft = (offset < 0f) == (direction > 0f)
            transformOrigin = TransformOrigin(if (edgeOnLeft) 0f else 1f, 0.5f)
            cameraDistance = FlipCameraDistance
            rotationY = MaxFlipDegrees * offset * direction
        }

        PageTurnAnimation.Depth -> {
            // Gedən səhifə yerində qalıb dərinliyə çəkilir, gələn onun üstündən adi kimi sürüşür.
            if (offset <= 0f) return
            holdInPlace()
            alpha = 1f - distance
            scaleTo(DepthMinScale + (1f - DepthMinScale) * (1f - distance))
        }

        PageTurnAnimation.Zoom -> {
            scaleTo(ZoomMinScale + (1f - ZoomMinScale) * (1f - distance))
            alpha = ZoomMinAlpha + (1f - ZoomMinAlpha) * (1f - distance)
        }

        PageTurnAnimation.Fade -> {
            holdInPlace()
            // Solan **gələn** səhifədir, gedən yox: gedən tam qatı qalıb altda dayanır, ona görə
            // ekranda heç vaxt arxa fon görünmür və keçid «yeni səhifə köhnənin üstündə peyda olur»
            // kimi oxunur.
            //
            // ⚠️ Əks sıra (gedən üstdə, solan odur) sıx mətndə **oxunmur**: köhnə səhifənin sətirləri
            // yenisinin üstünə düşür və ikisi bir-birinə qarışır. Eyni səbəbdən qatılıq [FadeRamp]
            // ilə erkən tamamlanır — iki mətnin birdən oxunduğu pəncərə jestin yalnız ilk hissəsidir.
            if (offset < 0f) alpha = ((1f - distance) / FadeRamp).coerceAtMost(1f)
        }
    }
}

/**
 * Bab keçidi effekti — gələn məzmunun [travel] qədər qalan yolu (1 → başlanğıc, 0 → yerində).
 *
 * Vərəqləyicidəki qollardan fərqlidir, çünki orada hərəkətin bir hissəsini vərəqləyicinin özü
 * verir: burada isə səhifəni yalnız effekt hərəkət etdirir.
 */
private fun GraphicsLayerScope.applyEnterPageTurn(
    animation: PageTurnAnimation,
    travel: Float,
    forward: Boolean,
    direction: Float,
) {
    // İrəli gedəndə səhifə oxunuş istiqamətinin **qarşısından** gəlir: latın düzülüşündə sağdan,
    // ərəbcə interfeysdə soldan.
    val side = (if (forward) 1f else -1f) * direction
    val slide = size.width * travel * side

    when (animation) {
        PageTurnAnimation.Standard -> Unit

        PageTurnAnimation.Book -> {
            // Vərəq tikişin üstünə düşür: tikiş gəldiyi tərəfin əksindədir.
            transformOrigin = TransformOrigin(if (side > 0f) 0f else 1f, 0.5f)
            cameraDistance = FlipCameraDistance
            rotationY = -MaxFlipDegrees * travel * side
        }

        PageTurnAnimation.Cube -> {
            translationX = slide
            transformOrigin = TransformOrigin(if (side > 0f) 0f else 1f, 0.5f)
            cameraDistance = FlipCameraDistance
            rotationY = -MaxFlipDegrees * travel * side
        }

        PageTurnAnimation.Depth -> {
            translationX = slide
            alpha = 1f - travel
            scaleTo(DepthMinScale + (1f - DepthMinScale) * (1f - travel))
        }

        PageTurnAnimation.Zoom -> {
            scaleTo(ZoomMinScale + (1f - ZoomMinScale) * (1f - travel))
            alpha = ZoomMinAlpha + (1f - ZoomMinAlpha) * (1f - travel)
        }

        PageTurnAnimation.Fade -> {
            // Burada altda səhifə yoxdur, gələn məzmun birbaşa fonun üstünə çıxır — vərəqləyicidəki
            // qat problemi yaranmır, ona görə sadə qatılıq artımı kifayətdir.
            alpha = 1f - travel
        }
    }
}

private fun GraphicsLayerScope.scaleTo(scale: Float) {
    scaleX = scale
    scaleY = scale
}
