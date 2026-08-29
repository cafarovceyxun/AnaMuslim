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
import androidx.compose.runtime.rememberUpdatedState
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
        val DEFAULT = Zoom

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
 * Hədis oxucusunun bab keçidi — **yalnız gələn** məzmun canlandırılır.
 *
 * Vərəqləyicidən fərqli olaraq burada iki səhifə eyni anda mövcud deyil: bab dəyişəndə siyahı öz
 * yerində yenilənir və `LazyListState` birdir, ona görə iki nüsxəni yan-yana göstərən keçid
 * (`AnimatedContent`) sürüşmə vəziyyətini iki siyahı arasında bölərdi. Bunun əvəzinə yeni bab
 * effektin son kadrından öz yerinə gəlir.
 *
 * [key] bab kimliyidir — ilk kompozisiyada effekt işləmir, ekranın açılışı səhifə dönməsi deyil.
 * [forward] `true` olanda keçid növbəti baba, `false` olanda əvvəlkinə oxunur.
 */
@Composable
fun Modifier.pageTurnEnterEffect(
    animation: PageTurnAnimation,
    key: Any?,
    forward: Boolean,
): Modifier {
    if (animation == PageTurnAnimation.Standard) return this

    val direction = layoutDirectionFactor()
    val progress = remember { Animatable(1f) }
    val currentForward by rememberUpdatedState(forward)
    var seenKey by remember { mutableStateOf(key) }

    LaunchedEffect(key) {
        if (key == seenKey) return@LaunchedEffect
        seenKey = key
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(EnterDurationMillis, easing = FastOutSlowInEasing),
        )
    }

    // Səhifənin qət etməli olduğu yol: 1 — hələ başlanğıcda, 0 — yerində.
    val travelOf: () -> Float = { (1f - progress.value).coerceIn(0f, 1f) }

    return this
        .graphicsLayer {
            applyEnterPageTurn(animation, travelOf(), currentForward, direction)
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
