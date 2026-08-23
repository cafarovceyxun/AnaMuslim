package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Oxucu bəzəyini (aşağı sürüşəndə gizlənən yuxarı bar və üzən düymələr) **tək barmaqla toxunuşla**
 * geri qaytarır.
 *
 * Bar `enterAlwaysScrollBehavior` ilə gizlənir: oxumaq üçün aşağı sürüşdürdükdə yığılır, geri
 * qaytarmaq üçünsə **yuxarı** sürüşdürmək lazımdır — yəni oxuduğun yeri itirməli olursan. Bu jest
 * həmin gedişi əvəz edir: barmaq qalxan kimi bar öz yerinə qayıdır, siyahı isə tərpənmir.
 *
 * ### Niyə iki barmaq deyil
 * İki barmaq [readerTextZoom]-un tərcümə mətnini böyütmə jestidir (üç barmaq ərəbcədir) — orada
 * toxunuşla pinch-in başlanğıcını ayırd etmək kövrək olur. Tək barmaq bu qarışıqlığı tamam aradan
 * qaldırır: iki barmaq düşən kimi jest **ləğv olunur** və zoom-a toxunulmur.
 *
 * ### Düymələrlə toqquşma
 * Jest heç nə udmur; əvəzinə hər hadisəni [PointerEventPass.Final]-da bir daha yoxlayır. Bu pass
 * uşaqdan valideynə gedir, yəni orada `isConsumed` «altdakı `clickable` bu toxunuşu artıq emal etdi»
 * deməkdir — belə halda bar açılmır. Nəticə: ayənin oynat/paylaş/əlfəcin düymələrinə toxunanda
 * yalnız həmin düymə işləyir, mətnin özünə və ya boş sahəyə toxunanda isə bar qayıdır. Sürüşmə də
 * eyni yolla kənarda qalır — `LazyColumn` sürüklənməni uddurur, üstəlik barmaq toxunuş slop-unu
 * keçir.
 */
@Composable
fun Modifier.readerChromeRevealGesture(
    enabled: Boolean,
    onReveal: () -> Unit,
): Modifier {
    if (!enabled) return this

    val reveal by rememberUpdatedState(onReveal)

    return this.pointerInput(Unit) {
        awaitEachGesture {
            val first = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)

            val origins = mutableMapOf<PointerId, Offset>()
            origins[first.id] = first.position

            var aborted = false
            var pressed = true

            val tapped = withTimeoutOrNull(TAP_TIMEOUT_MS) {
                while (pressed && !aborted) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)

                    // İkinci barmaq → mətn ölçüsü jesti başlayır, bu jest kənara çəkilir.
                    if (event.changes.count { it.pressed } >= 2) aborted = true

                    for (change in event.changes) {
                        val origin = origins.getOrPut(change.id) { change.position }
                        if ((change.position - origin).getDistance() > viewConfiguration.touchSlop) {
                            aborted = true
                        }
                    }

                    pressed = event.changes.any { it.pressed }

                    // Eyni hadisənin Final pass-ı: uşaqlar artıq emal edib, ona görə burada
                    // `isConsumed` = «altdakı düymə/sürüşmə bu toxunuşu özü götürdü».
                    val settled = awaitPointerEvent(PointerEventPass.Final)
                    if (settled.changes.any { it.isConsumed }) aborted = true
                }

                !aborted
            }

            if (tapped == true) reveal()
        }
    }
}

/**
 * Yığılmış yuxarı barı (və onunla birlikdə üzən düymələri, mini pleyer üçün ayrılan boşluğu) yerinə
 * qaytarır.
 *
 * Bütün bəzək `TopAppBarState.heightOffset`-dən törəyir (`collapsedFraction` → düymələrin
 * şəffaflığı, alt boşluq), ona görə tək bu dəyəri canlandırmaq hamısını birdən açır.
 */
@OptIn(ExperimentalMaterial3Api::class)
suspend fun expandReaderChrome(state: TopAppBarState) {
    if (state.heightOffset >= -0.5f) return

    animate(
        initialValue = state.heightOffset,
        targetValue = 0f,
        animationSpec = tween(durationMillis = REVEAL_DURATION_MS),
    ) { value, _ ->
        state.heightOffset = value
    }
    state.contentOffset = 0f
}

/** Toxunuş bu müddət içində bitməlidir — uzun basıb saxlamaq jest sayılmır. */
private const val TAP_TIMEOUT_MS = 600L

private const val REVEAL_DURATION_MS = 250
