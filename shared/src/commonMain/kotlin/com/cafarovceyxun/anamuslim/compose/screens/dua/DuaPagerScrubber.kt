package com.cafarovceyxun.anamuslim.compose.screens.dua

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_left
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_right
import com.cafarovceyxun.anamuslim.resources.duaNextPage
import com.cafarovceyxun.anamuslim.resources.duaPreviousPage
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Səhifə oxları, mövqe etiketi və nöqtələr — sürüşdürməyə əlavə, onu əvəz etmir.
 *
 * Nöqtələr **cari mövzunun** dualarını göstərir, oxlar isə bütün siyahıda gəzir: «1 / 2» etiketi
 * elə nöqtələrin üstündədir, yəni say ilə nöqtə eyni məxrəci paylaşır. (Əvvəl etiket səhifənin
 * içində, qrup daxilində idi, nöqtələr isə bütün bölmə üzərində — iki fərqli məxrəc.)
 *
 * @param position bütün siyahıdakı mövqe — oxların aktivliyi bundan asılıdır.
 * @param total bütün siyahının uzunluğu.
 * @param indicator nöqtələrin üstündəki etiket («1 / 2»); `null` = etiket yoxdur (Əsma belə çağırır).
 * @param dotsPosition/[dotsTotal] nöqtələrin göstərdiyi dəst — verilməsə bütün siyahı.
 * @param onScrubTo barmaq nöqtələrin üstündə sürüşəndə çağırılır; hədəf **qlobal** indeksdir.
 *   `null` = jest yoxdur.
 * @param onDotTap nöqtəyə (və ya sıranın həmin hissəsinə) toxunanda çağırılır; parametr
 *   nöqtənin sırasıdır, yəni mövzu daxilindəki indeks.
 * @param scrubTitle sürüşdürərkən etiketin yanında görünən mövzu adı — mövzu sərhədini keçdiyini
 *   ancaq bu bildirir.
 */
@Composable
internal fun DuaPagerControls(
    position: Int,
    total: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    indicator: String? = null,
    dotsPosition: Int = position,
    dotsTotal: Int = total,
    onScrubTo: ((Int) -> Unit)? = null,
    onDotTap: ((Int) -> Unit)? = null,
    scrubTitle: String? = null,
) {
    var scrubbing by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious, enabled = position > 0) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_chevron_left),
                contentDescription = stringResource(Res.string.duaPreviousPage),
                tint = colorScheme.onSurfaceVariant.alpha(if (position > 0) 0.8f else 0.25f),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                // Jest sütunun **hamısını** tutur (etiket + nöqtələr): 6dp-lik nöqtə barmaq üçün
                // hədəf deyil, ona görə toxunma sahəsi şaquli doldurma ilə genişləndirilir.
                .padding(vertical = 8.dp)
                .duaScrubGesture(position = position, total = total, onScrubTo = onScrubTo) {
                    scrubbing = it
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (indicator != null) {
                // Sürüşdürərkən mövzu adı da yazılır: nöqtələr yalnız cari mövzunu göstərdiyi üçün
                // qonşu mövzuya keçdiyini başqa cür bilmək olmur.
                val label = if (scrubbing && !scrubTitle.isNullOrBlank()) {
                    "$scrubTitle · $indicator"
                } else {
                    indicator
                }

                Text(
                    text = label,
                    style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (scrubbing) colorScheme.primary
                    else colorScheme.onSurfaceVariant.alpha(0.75f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }

            PagerDots(
                position = dotsPosition,
                total = dotsTotal,
                onDotTap = onDotTap,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        IconButton(onClick = onNext, enabled = position < total - 1) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_chevron_right),
                contentDescription = stringResource(Res.string.duaNextPage),
                tint = colorScheme.onSurfaceVariant.alpha(
                    if (position < total - 1) 0.8f else 0.25f,
                ),
            )
        }
    }
}

/**
 * Nöqtələr üzərində sürüşdürmə — barmağın **yolu** səhifə addımına çevrilir.
 *
 * Mütləq xəritələmə (sıranın eni ↔ bütün siyahı) qırx dua üçün hər səhifəyə bir neçə piksel
 * verirdi, yəni barmağın kiçik titrəyişi onlarla səhifə atlayırdı. Nisbi addım ([SCRUB_STEP])
 * isə həm dəqiqdir, həm də sürətlidir: ekranın eni boyu bir sürüşdürmə ~25 səhifə aparır və
 * mövzu sərhədlərini keçir.
 *
 * ⚠️ Ərəbcə interfeysdə düzülüş RTL-dir və vərəqləyici də tərsinə işləyir, ona görə istiqamət
 * [LayoutDirection] ilə çevrilir — əks halda sağa sürüşdürmək geriyə aparardı.
 */
@Composable
private fun Modifier.duaScrubGesture(
    position: Int,
    total: Int,
    onScrubTo: ((Int) -> Unit)?,
    onScrubbingChange: (Boolean) -> Unit,
): Modifier {
    if (onScrubTo == null || total <= 1) return this

    val layoutDirection = LocalLayoutDirection.current
    // Jest başlayanda cari mövqe lazımdır, amma `pointerInput` bloku yalnız açarları dəyişəndə
    // yenidən qurulur — köhnə dəyər tutulmasın deyə canlı istinad saxlanılır.
    val currentPosition by rememberUpdatedState(position)
    val scrub by rememberUpdatedState(onScrubTo)
    val scrubbingChange by rememberUpdatedState(onScrubbingChange)

    return this.pointerInput(total, layoutDirection) {
        val stepPx = SCRUB_STEP.toPx()
        var accumulated = 0f
        var index = 0

        detectHorizontalDragGestures(
            onDragStart = {
                index = currentPosition
                accumulated = 0f
                scrubbingChange(true)
            },
            onDragEnd = { scrubbingChange(false) },
            onDragCancel = { scrubbingChange(false) },
        ) { change, dragAmount ->
            change.consume()

            val directed = if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
            accumulated += directed

            val steps = (accumulated / stepPx).toInt()
            if (steps != 0) {
                accumulated -= steps * stepPx
                val target = (index + steps).coerceIn(0, total - 1)
                if (target != index) {
                    index = target
                    scrub(target)
                }
            }
        }
    }
}

/**
 * Səhifə nöqtələri — **ən çox** [MAX_DOTS] ədəd.
 *
 * Yüz səhifəlik başlıqda hər səhifəyə bir nöqtə sıranı ekrandan qovardı; artıq olanda nöqtələr
 * yerinə mövqe yazısı göstərilir.
 */
@Composable
private fun PagerDots(
    position: Int,
    total: Int,
    modifier: Modifier = Modifier,
    onDotTap: ((Int) -> Unit)? = null,
) {
    if (total > MAX_DOTS) {
        Text(
            text = "${position + 1} / $total",
            style = typography.labelMedium,
            color = colorScheme.onSurfaceVariant.alpha(0.75f),
            textAlign = TextAlign.Center,
            modifier = modifier,
        )
        return
    }

    val layoutDirection = LocalLayoutDirection.current
    val tap by rememberUpdatedState(onDotTap)

    Row(
        modifier = modifier.then(
            if (onDotTap == null) {
                Modifier
            } else {
                // Nöqtənin özünü klik hədəfi etmək olmur (6dp), ona görə toxunuşun **x mövqeyi**
                // sıraya bölünür: istifadəçi ikinci nöqtəyə toxunduğunu görür, hədəf isə bütün
                // sıranın eninin o hissəsidir.
                Modifier.pointerInput(total, layoutDirection) {
                    detectTapGestures { offset ->
                        if (total <= 0 || size.width <= 0) return@detectTapGestures
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        val directed = if (layoutDirection == LayoutDirection.Rtl) {
                            1f - fraction
                        } else {
                            fraction
                        }
                        tap?.invoke((directed * total).toInt().coerceIn(0, total - 1))
                    }
                }
            },
        ),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            val active = index == position
            Box(
                modifier = Modifier
                    .size(if (active) 8.dp else 6.dp)
                    .background(
                        color = if (active) colorScheme.primary
                        else colorScheme.onSurfaceVariant.alpha(0.3f),
                        shape = CircleShape,
                    ),
            )
        }
    }
}

/** Bir səhifə addımı üçün barmağın keçməli olduğu yol. */
private val SCRUB_STEP = 14.dp

private const val MAX_DOTS = 12
