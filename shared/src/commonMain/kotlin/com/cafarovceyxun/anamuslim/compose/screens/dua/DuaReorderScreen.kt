package com.cafarovceyxun.anamuslim.compose.screens.dua

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.ReadableWidthColumn
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.arabicFontFamily
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_drag_handle
import com.cafarovceyxun.anamuslim.resources.duaPickerSave
import com.cafarovceyxun.anamuslim.resources.duaSortHint
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

/** Sıralama ekranının bir sətri — sürükləyən tərəf yalnız [key] ilə işləyir. */
internal data class ReorderRow(
    val key: String,
    val title: String,
    val subtitle: String? = null,
    val arabic: String? = null,
)

/**
 * Başlıqları, alt başlıqları və duaları **basılı saxlayıb sürükləməklə** sıralayan ekran.
 *
 * ### Niyə ayrıca ekran
 * Adi siyahıda uzun basma artıq «adını dəyiş / sil» menyusunu açır, kart isə toxunanda içəri girir —
 * eyni jestə ikinci məna vermək hər iki əməliyyatı etibarsız edərdi. Ona görə sıralama menyudan
 * açılan **öz rejimidir**: burada toxunuş heç nə açmır, yeganə jest sürükləməkdir, nəticə isə yuxarı
 * bardakı «Yadda saxla» ilə yazılır, «Ləğv et» ilə atılır. Yarımçıq sıra heç vaxt serverə getmir.
 *
 * ### Niyə `LazyColumn` deyil
 * Yerdəyişmə **ölçülmüş hündürlüklər** üzərində hesablanır (bax [RowReorderState]), lazy konteyner
 * isə ekrandan çıxan sətri ölçüdən də çıxarır — sürüklənən kart siyahının o biri ucuna çatanda
 * hündürlüklər yoxa çıxar və hesab dayanardı. Sıralanan qrup onsuz da bir neçə onluq sətirdir.
 */
@Composable
internal fun DuaReorderScreen(
    title: String,
    rows: List<ReorderRow>,
    isSaving: Boolean,
    onSave: (List<String>) -> Unit,
    onCancel: () -> Unit,
) {
    // ⚠️ Açar `rows`-un **məzmunudur** (data class → strukturlu bərabərlik): hər yenidən
    // kompozisiyada yeni siyahı instansiyası gəlir, amma məzmun dəyişməyincə sıra sıfırlanmır.
    // Serverdən həqiqətən başqa sıra gələndə (uğursuz yazmadan sonrakı `refresh`) isə sıfırlanır —
    // ekranda yalan sıra qalmasın.
    val state = remember(rows) { RowReorderState(rows.map { it.key }) }
    val byKey = remember(rows) { rows.associateBy { it.key } }

    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val autoScrollEdge = with(density) { 96.dp.toPx() }
    val autoScrollStep = with(density) { 14.dp.toPx() }

    // Barmaq ekranın kənarına çatanda siyahı özü sürüşür — uzun siyahıda kartı aşağıdan yuxarı
    // aparmaq başqa cür mümkün olmazdı (`HomeScreen` ilə eyni qurğu).
    LaunchedEffect(state.dragging) {
        if (state.dragging == null) return@LaunchedEffect

        while (true) {
            withFrameNanos { }

            val delta = state.autoScrollDelta(autoScrollStep)
            if (delta != 0f) state.scrolled(scrollState.scrollBy(delta))
        }
    }

    Scaffold(
        topBar = {
            AppBar(
                title = title,
                onBack = onCancel,
                actions = {
                    TextButton(onClick = onCancel, enabled = !isSaving) {
                        Text(
                            text = stringResource(Res.string.strLabelCancel),
                            style = typography.labelLarge,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }

                    TextButton(
                        onClick = { onSave(state.order) },
                        enabled = !isSaving,
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = colorScheme.primary,
                            )
                        } else {
                            Text(
                                text = stringResource(Res.string.duaPickerSave),
                                style = typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = colorScheme.primary,
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        // Görünüş sahəsini sürüşən sütun deyil, onu saxlayan qutu ölçür: sürüşən sütunun öz
        // sərhədləri məzmunla birlikdə hərəkət edir, avto-sürüşmə isə **ekrandakı** kənarı bilməlidir.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .onGloballyPositioned {
                    state.setViewport(
                        topInRoot = it.positionInRoot().y,
                        height = it.size.height.toFloat(),
                        edge = autoScrollEdge,
                    )
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Sürükləmə gedərkən siyahı jestlə sürüşmür: kart barmağın altındadır, sürüşmə
                    // yalnız avto-sürüşmə ilə, proqramla baş verir.
                    .verticalScroll(scrollState, enabled = state.dragging == null),
            ) {
                ReadableWidthColumn {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = stringResource(Res.string.duaSortHint),
                            style = typography.bodySmall.withScriptDirection(arabic = false),
                            color = colorScheme.onSurfaceVariant.alpha(0.8f),
                            modifier = Modifier.padding(bottom = 4.dp),
                        )

                        state.order.forEachIndexed { index, key ->
                            val row = byKey[key] ?: return@forEachIndexed

                            ReorderableRow(
                                state = state,
                                row = row,
                                position = index + 1,
                            )
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

/**
 * Bir sıralana bilən sətir.
 *
 * İki qat **qəsdəndir**: çöl qutu ölçünü/mövqeyi bildirir və jesti tutur, iç qutu isə
 * `graphicsLayer` ilə sürüşür. Bir qatda birləşsəydi sürüşmə `positionInRoot()`-a da düşər və hesab
 * öz quyruğunu qovardı (`ReorderableHomeSection` ilə eyni səbəb).
 */
@Composable
private fun ReorderableRow(
    state: RowReorderState,
    row: ReorderRow,
    position: Int,
) {
    val haptics = LocalHapticFeedback.current
    val dragging = state.dragging == row.key

    Box(
        modifier = Modifier
            .onGloballyPositioned {
                state.positioned(row.key, it.size.height, it.positionInRoot().y)
            }
            // Qaldırılmış kart qonşularının üstündə çəkilməlidir, yoxsa kölgəsi altda qalır.
            .zIndex(if (dragging) 1f else 0f)
            .pointerInput(row.key) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        state.startDrag(row.key, offset.y)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        state.drag(dragAmount.y, change.position.y)
                    },
                    onDragEnd = { state.endDrag() },
                    onDragCancel = { state.endDrag() },
                )
            },
    ) {
        Surface(
            color = if (dragging) colorScheme.surfaceContainerHighest
            else colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(
                width = if (dragging) 1.dp else 0.5.dp,
                color = if (dragging) colorScheme.primary.alpha(0.6f)
                else colorScheme.outlineVariant.alpha(0.5f),
            ),
            modifier = Modifier.fillMaxWidth().graphicsLayer {
                translationY = if (dragging) state.dragOffsetY else 0f

                val lift = if (dragging) 1.02f else 1f
                scaleX = lift
                scaleY = lift
                shadowElevation = if (dragging) 16.dp.toPx() else 0f
            },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Mövqe nömrəsi: sürükləmə bitəndən sonra «neçənci oldu» sualının cavabı ekranda
                // qalsın deyə — ardıcıllıq sürüklənən kartla birlikdə dəyişir.
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(colorScheme.primaryContainer.alpha(0.45f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = position.toString(),
                        style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.primary,
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.title,
                        style = typography.bodyLarge.withScriptDirection(arabic = false),
                        color = colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    row.subtitle?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = typography.labelSmall.withScriptDirection(arabic = false),
                            color = colorScheme.onSurfaceVariant.alpha(0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                row.arabic?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = typography.bodyMedium.withScriptDirection(
                            arabic = true,
                            arabicFontFamily = arabicFontFamily(),
                        ),
                        color = colorScheme.onSurface.alpha(0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp).widthIn(max = 120.dp),
                    )
                }

                Icon(
                    painter = painterResource(Res.drawable.dr_icon_drag_handle),
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant.alpha(if (dragging) 0.9f else 0.5f),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

/**
 * Sürükləmənin vəziyyəti — sıra, tutulan sətir, sürüşmə və ölçülər.
 *
 * Yerdəyişmə qonşunun **ölçülmüş** hündürlüyünə görə hesablanır, sabit sətir hündürlüyünə görə yox:
 * başlıq adı iki sətrə keçəndə kart hündürləşir və sabit ədədlə hesablanan yerdəyişmə sürüşməyə
 * qarşı gec (və ya tez) işləyərdi.
 */
@Stable
private class RowReorderState(initial: List<String>) {

    var order by mutableStateOf(initial)
        private set

    /** Sürüklənən sətrin açarı; sürükləmə getmirsə null. */
    var dragging by mutableStateOf<String?>(null)
        private set

    /** Sürüklənən sətrin öz yerindən şaquli sürüşməsi (px). */
    var dragOffsetY by mutableFloatStateOf(0f)
        private set

    private val heights = mutableStateMapOf<String, Int>()
    private val tops = mutableStateMapOf<String, Float>()

    /** Barmağın kök koordinat sistemindəki Y mövqeyi — avto-sürüşmə bunu oxuyur. */
    private var pointerY = 0f

    private var viewportTop = 0f
    private var viewportHeight = 0f
    private var autoScrollEdge = 0f

    /**
     * Ölçü və mövqe `graphicsLayer`-dən **kənar** qatdan gəlir: sürüşdürməni eyni düyünə qoysaydıq
     * `positionInRoot()` sürüşməni özü də sayardı.
     */
    fun positioned(key: String, height: Int, topInRoot: Float) {
        heights[key] = height
        tops[key] = topInRoot
    }

    fun setViewport(topInRoot: Float, height: Float, edge: Float) {
        viewportTop = topInRoot
        viewportHeight = height
        autoScrollEdge = edge
    }

    fun startDrag(key: String, pointerYInElement: Float) {
        dragging = key
        dragOffsetY = 0f
        pointerY = (tops[key] ?: 0f) + pointerYInElement
    }

    fun drag(deltaY: Float, pointerYInElement: Float) {
        val key = dragging ?: return

        pointerY = (tops[key] ?: 0f) + pointerYInElement
        dragOffsetY += deltaY
        settle(key)
    }

    /**
     * Avto-sürüşmə siyahını tərpədəndə kart barmağın altında qalsın deyə eyni delta sürüşməyə
     * əlavə olunur — yəni sürüşmə kartın siyahıdakı hərəkəti kimi sayılır.
     */
    fun scrolled(deltaY: Float) {
        val key = dragging ?: return

        dragOffsetY += deltaY
        settle(key)
    }

    fun endDrag() {
        dragging = null
        dragOffsetY = 0f
    }

    /**
     * Sürüşmə qonşunun hündürlüyünün yarısını keçəndə yer dəyişir və həmin hündürlük sürüşmədən
     * çıxılır — kart barmağın altında qalır, siyahı isə altından sürüşür. Bir jestdə bir neçə addım
     * keçmək mümkün olduğu üçün dövrədir.
     */
    private fun settle(key: String) {
        while (true) {
            val index = order.indexOf(key)
            if (index < 0) return

            val direction = if (dragOffsetY > 0f) 1 else -1
            val target = index + direction
            if (target !in order.indices) return

            // Hələ ölçülməmiş qonşu (ekrana yeni girib) ilə yer dəyişmək sürüşməni sıfıra
            // yaxın dəyərlə tətikləyərdi — kart bir anda siyahının o biri ucuna atılardı.
            val neighbour = heights[order[target]] ?: return
            if (neighbour <= 0) return
            if (abs(dragOffsetY) < neighbour / 2f) return

            order = order.toMutableList().apply { add(target, removeAt(index)) }
            dragOffsetY -= direction * neighbour
        }
    }

    /**
     * Barmaq görünüş sahəsinin kənar zolağındadırsa saniyədə neçə piksel sürüşmək lazım olduğunu
     * verir; kənardan uzaqda sıfır.
     */
    fun autoScrollDelta(maxStep: Float): Float {
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
