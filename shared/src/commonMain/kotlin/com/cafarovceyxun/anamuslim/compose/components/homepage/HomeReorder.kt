package com.cafarovceyxun.anamuslim.compose.components.homepage

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HomeSection
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HomeSectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Ana ekranda bölmənin **üstünə basılı saxlayıb sürükləməklə** yerini dəyişmək.
 *
 * ### Niyə ayrıca rejim yoxdur
 * Uzun basma birbaşa sürükləməni başladır, buraxanda düzən yazılır — arada «düzənləmə rejimi»
 * yoxdur. Rejim olsaydı, açıq qaldığı müddətdə kartlara toxunmaq lazım gələrdi ki, kliklər udulsun;
 * kliki udan hər üsul (üst təbəqə, `detectTapGestures`) isə **eyni jest ötürücüsündən** keçir və
 * uzun basmanı da udur. Jest-yönlü variantda belə bir toqquşma ümumiyyətlə yaranmır.
 * Bölmələri gizlətmək/göstərmək Ayarlar → «Ana ekranı düzənlə»-də qalır.
 *
 * ### Niyə `LazyColumn` deyil
 * Ana ekran adi `Column` + `verticalScroll`-dur, ona görə hazır `animateItem`/`reorderable`
 * naxışları işləmir: yerdəyişmə **ölçülmüş hündürlüklər** üzərində əl ilə hesablanır.
 *
 * ⚠️ **Hündürlüyü sıfır olan bölmə keçilir.** [FeatureStoriesRow], `HomeSectionSuggestions` və
 * başqaları boş olanda heç nə emit etmir; onları sıradan çıxarmasaq istifadəçi görmədiyi elementin
 * üstündən «keçər» və buraxılan yer səhv olardı.
 */
@Stable
class HomeReorderState internal constructor(
    private val scope: CoroutineScope,
    private val onCommit: suspend (List<HomeSectionState>) -> Unit,
) {
    /** Sürüklənən bölmə; sürükləmə getmirsə null. */
    var dragging by mutableStateOf<HomeSection?>(null)
        private set

    /** Sürüklənən kartın öz yerindən şaquli sürüşməsi (px). */
    var dragOffsetY by mutableFloatStateOf(0f)
        private set

    /**
     * Sürükləmə boyu işlənən sıra. **Yalnız [dragging] null olmayanda mənalıdır** — sürükləmə
     * bitəndən sonra ekran yenə saxlanılan düzəni oxuyur, ona görə burada köhnə nüsxə qalmır.
     */
    var order by mutableStateOf<List<HomeSectionState>>(emptyList())
        private set

    private val heights = mutableStateMapOf<HomeSection, Int>()
    private val tops = mutableStateMapOf<HomeSection, Float>()

    /** Barmağın kök koordinat sistemindəki Y mövqeyi — avto-sürüşmə bunu oxuyur. */
    private var pointerY = 0f

    private var viewportTop = 0f
    private var viewportHeight = 0f
    private var autoScrollEdge = 0f

    /**
     * Ölçü və mövqe `graphicsLayer`-dən **kənar** qatdan gəlir: sürüşdürməni eyni düyünə qoysaydıq
     * `positionInRoot()` sürüşməni özü də sayardı və hesab öz quyruğunu qovardı.
     */
    internal fun positioned(section: HomeSection, height: Int, topInRoot: Float) {
        heights[section] = height
        tops[section] = topInRoot
    }

    internal fun setViewport(topInRoot: Float, height: Float, edge: Float) {
        viewportTop = topInRoot
        viewportHeight = height
        autoScrollEdge = edge
    }

    private fun heightOf(section: HomeSection): Int = heights[section] ?: 0

    /** Boş (hündürlüyü sıfır) bölməni sürükləmək olmaz — tutmağa bir şey yoxdur. */
    internal fun canDrag(section: HomeSection): Boolean = heightOf(section) > 0

    internal fun startDrag(
        section: HomeSection,
        layout: List<HomeSectionState>,
        pointerYInElement: Float,
    ) {
        if (!canDrag(section)) return

        order = layout
        dragging = section
        dragOffsetY = 0f
        pointerY = (tops[section] ?: 0f) + pointerYInElement
    }

    internal fun drag(deltaY: Float, pointerYInElement: Float) {
        val section = dragging ?: return

        pointerY = (tops[section] ?: 0f) + pointerYInElement
        dragOffsetY += deltaY
        settle(section)
    }

    /**
     * Avto-sürüşmə səhifəni tərpədəndə kart barmağın altında qalsın deyə eyni delta sürüşməyə
     * əlavə olunur — yəni sürüşmə kartın siyahıdakı hərəkəti kimi sayılır.
     */
    internal fun scrolled(deltaY: Float) {
        val section = dragging ?: return

        dragOffsetY += deltaY
        settle(section)
    }

    /**
     * Sürüşmə qonşunun hündürlüyünün yarısını keçəndə yer dəyişir və həmin hündürlük sürüşmədən
     * çıxılır — kart barmağın altında qalır, siyahı isə altından sürüşür. Bir jestdə bir neçə
     * addım keçmək mümkün olduğu üçün dövrədir.
     */
    private fun settle(section: HomeSection) {
        while (true) {
            val index = order.indexOfFirst { it.section == section }
            if (index < 0) return

            val direction = if (dragOffsetY > 0f) 1 else -1

            var target = index + direction
            while (target in order.indices && heightOf(order[target].section) <= 0) {
                target += direction
            }
            if (target !in order.indices) return

            val neighbour = heightOf(order[target].section)
            if (abs(dragOffsetY) < neighbour / 2f) return

            order = order.toMutableList().apply { add(target, removeAt(index)) }
            dragOffsetY -= direction * neighbour
        }
    }

    internal fun endDrag() {
        val moved = order
        dragging = null
        dragOffsetY = 0f

        // Kompozisiya ömrünə bağlı olmayan scope: yazı bir addımlıqdır, amma buraxma anında ekran
        // onsuz da yenidən qurulur.
        scope.launch { onCommit(moved) }
    }

    internal fun cancelDrag() {
        dragging = null
        dragOffsetY = 0f
    }

    /**
     * Barmaq görünüş sahəsinin kənar zolağındadırsa saniyədə neçə piksel sürüşmək lazım olduğunu
     * verir; kənardan uzaqda sıfır. Sürət zolağın içində xəttidir — kənara yaxınlaşdıqca sürətlənir.
     */
    internal fun autoScrollDelta(maxStep: Float): Float {
        if (dragging == null || viewportHeight <= 0f || autoScrollEdge <= 0f) return 0f

        val topEdge = viewportTop + autoScrollEdge
        val bottomEdge = viewportTop + viewportHeight - autoScrollEdge

        return when {
            pointerY < topEdge -> -maxStep * ((topEdge - pointerY) / autoScrollEdge).coerceIn(0f, 1f)
            pointerY > bottomEdge ->
                maxStep * ((pointerY - bottomEdge) / autoScrollEdge).coerceIn(0f, 1f)

            else -> 0f
        }
    }
}

@Composable
fun rememberHomeReorderState(
    onCommit: suspend (List<HomeSectionState>) -> Unit,
): HomeReorderState {
    val scope = rememberCoroutineScope()
    return remember(scope) { HomeReorderState(scope, onCommit) }
}

/**
 * Bir ana ekran bölməsini sürüklənə bilən edir.
 *
 * İki qat var və bu **qəsdəndir**: çöl qutu ölçünü/mövqeyi bildirir və jesti tutur, iç qutu isə
 * `graphicsLayer` ilə sürüşür. Bir qatda birləşsəydi sürüşmə `positionInRoot()`-a da düşərdi.
 */
@Composable
fun ReorderableHomeSection(
    state: HomeReorderState,
    section: HomeSection,
    layout: List<HomeSectionState>,
    content: @Composable () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val dragging = state.dragging == section

    // ⚠️ Sıra `pointerInput`-un açarı OLA BİLMƏZ: hər yerdəyişmədə açar dəyişər, jest ötürücüsü
    // yenidən qurular və sürükləmə barmaq hələ ekranda ikən kəsilərdi.
    val currentLayout by rememberUpdatedState(layout)

    // Bölmə gizlədiləndə ölçüsü ilə birlikdə sıradan da çıxmalıdır: köhnə hündürlük qalsaydı
    // sürükləmə görünməyən bölmə ilə «yer dəyişər» və ekranda heç nə baş verməzdi.
    DisposableEffect(section) {
        onDispose { state.positioned(section, height = 0, topInRoot = 0f) }
    }

    Box(
        modifier = Modifier
            .onGloballyPositioned {
                state.positioned(section, it.size.height, it.positionInRoot().y)
            }
            // Qaldırılmış kart qonşularının üstündə çəkilməlidir, yoxsa kölgəsi altda qalır.
            .zIndex(if (dragging) 1f else 0f)
            .pointerInput(section) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        if (!state.canDrag(section)) return@detectDragGesturesAfterLongPress

                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        state.startDrag(section, currentLayout, offset.y)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        state.drag(dragAmount.y, change.position.y)
                    },
                    onDragEnd = { state.endDrag() },
                    onDragCancel = { state.cancelDrag() },
                )
            },
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                translationY = if (dragging) state.dragOffsetY else 0f

                val lift = if (dragging) 1.02f else 1f
                scaleX = lift
                scaleY = lift
                shadowElevation = if (dragging) 16.dp.toPx() else 0f
            },
        ) {
            content()
        }
    }
}
