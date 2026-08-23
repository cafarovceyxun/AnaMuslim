package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.labelArabic
import com.cafarovceyxun.anamuslim.resources.labelTranslation
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.round

/** Barmaqla böyüdülən iki mətn ölçüsündən hansı. */
enum class ReaderZoomTarget { Arabic, Translation }

/** Jest davam edərkən ekranda görünən «Ərəbcə · 120%» nişanının məzmunu. */
data class ReaderZoomFeedback(
    val target: ReaderZoomTarget,
    val multiplier: Float,
)

/**
 * Oxuma ekranlarında barmaqla mətn ölçüsü.
 *
 * **İki barmaq tərcüməni, üç barmaq ərəbcəni** böyüdüb-kiçildir. İki ölçü ayrı-ayrı çarpanlardır
 * (ayarlardakı iki sürüşdürücü), ona görə bir jestlə ikisini birdən dəyişmək əvəzinə barmaq sayı
 * hədəfi seçir — istifadəçi hansını dəyişdiyini əvvəlcədən bilir.
 *
 * ### Niyə DataStore-a birbaşa yazır
 * Mətn ölçüsünü ekranın dərinliyindəki onlarla composable [ReaderPreferences] / `HadithPreferences`
 * üzərindən oxuyur. «Canlı» dəyəri oradan keçirmək bütün ağacı yenidən naqilləməli olardı; əvəzinə
 * jest birbaşa prefi yazır və UI onsuz da müşahidə etdiyi üçün özü yenilənir. Yazışların sayını
 * [STEP] saxlayır: çarpan 5%-lik pillələrə yuvarlaqlaşdırılır, yəni 50%→200% tam jest boyu cəmi
 * ~30 yazı olur, kadr başına yox.
 *
 * ### Niyə Initial pass
 * Hadisələr [PointerEventPass.Initial]-da tutulur: bu pass valideyndən uşağa gedir, ona görə
 * daxildəki `LazyColumn` sürüşməsi hadisəni **görməmişdən əvvəl** udmaq mümkün olur. Main pass-da
 * dinləsəydik siyahı artıq sürüşmüş olardı və pinch zamanı səhifə tullanardı. Udma yalnız iki
 * barmaq düşdükdə **və** [SLOP] keçiləndə baş verir — tək barmaqla sürüşmə heç toxunulmur.
 */
object ReaderTextZoom {
    /** Çarpan pilləsi — həm yazı sayını azaldır, həm də jestə «dişli» hiss verir. */
    const val STEP = 0.05f

    /**
     * Jest başlamazdan əvvəl tələb olunan miqyas dəyişikliyi.
     *
     * Sürüşdürərkən iki barmaq təsadüfən ekrana düşürsə kiçik titrəyiş mətni böyütməməlidir.
     */
    const val SLOP = 0.06f

    /** Quran oxucusunun sürüşdürücüləri ilə eyni aralıq — bax [ReaderTextSizeUtils]. */
    const val QURAN_MIN = 0.3f
    const val QURAN_MAX = 2.0f

    /** Hədis oxucusunun sürüşdürücüləri 300%-ə qədər gedir, alt həddi isə eynidir. */
    const val HADITH_MIN = 0.3f
    const val HADITH_MAX = 3.0f

    fun snap(value: Float): Float = round(value / STEP) * STEP

    fun percent(multiplier: Float): Int = round(multiplier * 100).toInt()
}

/**
 * [ReaderTextZoom] jestini sürüşən konteynerə bağlayır.
 *
 * [enabled] `false` olanda modifikator ümumiyyətlə əlavə olunmur — ayarda söndürülmüş funksiya heç
 * bir toxunuş emalı aparmır.
 *
 * [arabicMultiplier] və [translationMultiplier] jest **başlayanda** oxunur və dayaq nöqtəsi kimi
 * saxlanılır; sonrakı yazılar həmin dayaqdan hesablanır, yoxsa öz yazdığımız dəyəri geri oxuyub
 * qapalı dövrəyə düşərdik.
 */
@Composable
fun Modifier.readerTextZoom(
    enabled: Boolean,
    arabicMultiplier: Float,
    translationMultiplier: Float,
    minMultiplier: Float,
    maxMultiplier: Float,
    onZoom: (ReaderZoomTarget, Float) -> Unit,
): Modifier {
    if (!enabled) return this

    val arabic by rememberUpdatedState(arabicMultiplier)
    val translation by rememberUpdatedState(translationMultiplier)
    val zoom by rememberUpdatedState(onZoom)

    // `pointerInput` açarı sabitdir: çarpandan asılı olsaydı hər pillə jesti ortadan kəsərdi.
    return this.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)

            var target: ReaderZoomTarget? = null
            var base = 1f
            var factor = 1f
            var started = false
            // Bir pillə onlarla hadisə boyu eyni qalır; təkrar yazı həm DataStore-u, həm də
            // bütöv oxucu ağacının yenidən qurulmasını nahaq yerə işə salardı.
            var emitted: Float? = null
            var event: PointerEvent

            do {
                event = awaitPointerEvent(PointerEventPass.Initial)
                val down = event.changes.count { it.pressed }

                if (down >= 2) {
                    val wanted =
                        if (down >= 3) ReaderZoomTarget.Arabic else ReaderZoomTarget.Translation

                    // Üç barmaq eyni anda düşmür — ikinci ilə üçüncü arasında bir neçə hadisə olur.
                    // Hələ heç nə yazmamışıqsa hədəf sonradan gələn barmağa görə düzəlir.
                    if (target == null || (!started && wanted != target)) {
                        target = wanted
                        base = if (wanted == ReaderZoomTarget.Arabic) arabic else translation
                        factor = 1f
                        emitted = null
                    }

                    factor *= event.calculateZoom()

                    if (started || abs(factor - 1f) >= ReaderTextZoom.SLOP) {
                        started = true
                        val next = ReaderTextZoom.snap(
                            (base * factor).coerceIn(minMultiplier, maxMultiplier)
                        )
                        if (next != emitted) {
                            emitted = next
                            zoom(target!!, next)
                        }
                        // Yalnız jest həqiqətən başlayandan sonra udulur, yoxsa sürüşmə ölür.
                        event.changes.forEach { if (it.pressed) it.consume() }
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }
}

/**
 * Jest zamanı görünən «Ərəbcə · 120%» nişanı.
 *
 * Mətnin özünün böyüməsi nə qədər dəyişdiyini göstərsə də, **hansı** ölçünün dəyişdiyini göstərmir —
 * iki barmaqla üç barmağı qarışdıran istifadəçi əks halda səhv mətni böyütdüyünü yalnız sonradan
 * anlayardı. Nişan son dəyişiklikdən [HIDE_DELAY_MS] sonra özü sönür.
 */
@Composable
fun ReaderZoomFeedbackOverlay(
    feedback: ReaderZoomFeedback?,
    onTimeout: () -> Unit,
) {
    val last = remember { mutableStateOf<ReaderZoomFeedback?>(null) }
    if (feedback != null) last.value = feedback

    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(HIDE_DELAY_MS)
            onTimeout()
        }
    }

    val shown = last.value

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = feedback != null && shown != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            if (shown == null) return@AnimatedVisibility

            val label = stringResource(
                if (shown.target == ReaderZoomTarget.Arabic) Res.string.labelArabic
                else Res.string.labelTranslation
            )

            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = colorScheme.surfaceContainerHighest.alpha(0.95f),
                contentColor = colorScheme.onSurface,
                shadowElevation = 4.dp,
            ) {
                Text(
                    // Faiz işarəsi kodda qurulur: Compose Resources formatlaması `%%`-i escape kimi
                    // açmadığı üçün resurs faylında faiz vermək ekranda hərfi `%%` göstərərdi.
                    text = "$label · ${ReaderTextZoom.percent(shown.multiplier)}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
    }
}

private const val HIDE_DELAY_MS = 900L
