package com.cafarovceyxun.anamuslim.compose.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.RadioItem
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheet
import com.cafarovceyxun.anamuslim.compose.components.reader.PageTurnAnimation
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.ic_mode_book
import com.cafarovceyxun.anamuslim.resources.pageTurnAnimation
import com.cafarovceyxun.anamuslim.resources.pageTurnBook
import com.cafarovceyxun.anamuslim.resources.pageTurnBookDesc
import com.cafarovceyxun.anamuslim.resources.pageTurnCube
import com.cafarovceyxun.anamuslim.resources.pageTurnCubeDesc
import com.cafarovceyxun.anamuslim.resources.pageTurnDepth
import com.cafarovceyxun.anamuslim.resources.pageTurnDepthDesc
import com.cafarovceyxun.anamuslim.resources.pageTurnFade
import com.cafarovceyxun.anamuslim.resources.pageTurnFadeDesc
import com.cafarovceyxun.anamuslim.resources.pageTurnStandard
import com.cafarovceyxun.anamuslim.resources.pageTurnStandardDesc
import com.cafarovceyxun.anamuslim.resources.pageTurnZoom
import com.cafarovceyxun.anamuslim.resources.pageTurnZoomDesc
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Effektlərin ekranda göstərilən sırası.
 *
 * «Dərinlik» başdadır — tövsiyə olunan effekt odur, ona görə vərəq açılan kimi göz ona düşür.
 * Ondan sonra effektsiz «Standart» (seçilmiş default), sonra qalanları yüngüldən ağıra:
 * rəng/ölçü ilə oynayanlar, sonra üçölçülü fırlananlar.
 */
private val pageTurnOptions: List<Triple<PageTurnAnimation, StringResource, StringResource>> =
    listOf(
        Triple(PageTurnAnimation.Depth, Res.string.pageTurnDepth, Res.string.pageTurnDepthDesc),
        Triple(
            PageTurnAnimation.Standard,
            Res.string.pageTurnStandard,
            Res.string.pageTurnStandardDesc,
        ),
        Triple(PageTurnAnimation.Fade, Res.string.pageTurnFade, Res.string.pageTurnFadeDesc),
        Triple(PageTurnAnimation.Zoom, Res.string.pageTurnZoom, Res.string.pageTurnZoomDesc),
        Triple(PageTurnAnimation.Book, Res.string.pageTurnBook, Res.string.pageTurnBookDesc),
        Triple(PageTurnAnimation.Cube, Res.string.pageTurnCube, Res.string.pageTurnCubeDesc),
    )

/** Seçilmiş effektin ayarlar sətrində göstərilən adı. */
@Composable
fun pageTurnAnimationLabel(animation: PageTurnAnimation): String = stringResource(
    pageTurnOptions.firstOrNull { it.first == animation }?.second ?: Res.string.pageTurnStandard
)

/**
 * Səhifə dəyişmə effektini seçən vərəq.
 *
 * Digər radio vərəqlərindən fərqli olaraq seçimdə **bağlanmır**: effekt yalnız vərəq bağlananda
 * görünür, ona görə hər variant üçün vərəqi avtomatik bağlamaq istifadəçini altıncı dəfəyə qədər
 * ayarların içindən keçirməli olardı. Seçim dərhal yazılır, sıradakı variantı seçmək bir toxunuşdur.
 */
@Composable
fun PageTurnAnimationSheet(isOpen: Boolean, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val selected = AppPreferences.observeReaderPageTurnAnimation()

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        icon = Res.drawable.ic_mode_book,
        title = stringResource(Res.string.pageTurnAnimation),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
        ) {
            pageTurnOptions.forEach { (animation, label, description) ->
                RadioItem(
                    title = label,
                    subtitle = description,
                    selected = selected == animation,
                    onClick = {
                        scope.launch { AppPreferences.setReaderPageTurnAnimation(animation) }
                    },
                )
            }
        }
    }
}
