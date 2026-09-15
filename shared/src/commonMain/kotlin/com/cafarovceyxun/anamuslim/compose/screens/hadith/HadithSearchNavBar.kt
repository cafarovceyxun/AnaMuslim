package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_down
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.ic_arrow_up
import com.cafarovceyxun.anamuslim.resources.searchNextMatch
import com.cafarovceyxun.anamuslim.resources.searchPreviousMatch
import com.cafarovceyxun.anamuslim.resources.strLabelClose
import androidx.compose.ui.text.TextLayoutResult
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Vurğulanan mətnin ölçmə nəticəsi və pəncərədəki yuxarı kənarı.
 *
 * Oxlar hədisin **içində** hansı sətrə enəcəyini yalnız bu ikisi ilə bilir: ölçmə nəticəsi sözün
 * hansı sətirdə olduğunu, pəncərə mövqeyi isə həmin sətrin ekranda hara düşdüyünü verir. İkisi ayrı
 * geri-çağırışlardan gəlir (`onTextLayout` və `onGloballyPositioned`), ona görə bir yerdə saxlanılır.
 */
data class HadithHighlightAnchor(
    val layout: TextLayoutResult? = null,
    val topInWindow: Float? = null,
)

/**
 * Kartın vurğulanmış mətni haqqında ekrana verdiyi xəbər: `layout` və `topInWindow`-dan **biri**
 * dolu olur, ekran isə ikisini birləşdirib saxlayır.
 */
typealias HadithHighlightAnchorReporter =
    (hadithId: Long, layout: TextLayoutResult?, topInWindow: Float?) -> Unit

/**
 * Mətnin pəncərədəki yerini [reporter]-ə bildirən modifikator.
 *
 * Ayrıca funksiyadır ki, çağırış yerlərində «sorğu varmı» şərti təkrarlanmasın: `reporter` və ya
 * `hadithId` yoxdursa modifikator **heç nə qoşmur**, yəni adi oxumada ölçmə yükü də olmur.
 */
fun Modifier.highlightAnchor(hadithId: Long?, reporter: HadithHighlightAnchorReporter?): Modifier =
    if (reporter == null || hadithId == null) this
    else this.onGloballyPositioned { reporter(hadithId, null, it.positionInWindow().y) }

/**
 * Axtarışdan gələn sorğunun oxucudakı idarəsi — «səhifədə tap» zolağının qarşılığı.
 *
 * Nə üçün var: nəticədən açılan hədisdə söz bir neçə yerdə keçir, uzun hədisdə isə ekrana yalnız
 * bir hissəsi sığır. Zolaq həm neçənci uyğunluqda olduğunu deyir (`3/17`), həm də yuxarı/aşağı
 * oxlarla növbətinə aparır; ✕ vurğunu tamamilə söndürür ki, oxumaq üçün ekran təmizlənsin.
 *
 * Sayğac sorğunun **cari siyahıdakı** bütün keçidlərini sayır: qarışıq rejimdə bu, vərəqləyicinin
 * açıq babıdır, tərcümə/ərəbcə rejimlərində bütöv cilddir — siyahı nədirsə, zolaq da onu gəzir.
 */
@Composable
fun HadithSearchNavBar(
    query: String,
    /** Neçənci uyğunluqdayıq (1-dən başlayır); 0 = hələ heç birinə enilməyib. */
    current: Int,
    total: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    /**
     * Vurğunu söndürmək. `null` = zolaqda ✕ yoxdur: sürətli baxış vərəqində bağlama düyməsi onsuz
     * da başlıqdadır, ikinci ✕ «hansı nəyi bağlayır?» sualı yaradardı.
     */
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = colorScheme.surfaceContainerHigh,
        contentColor = colorScheme.onSurface,
        border = BorderStroke(1.dp, colorScheme.outlineVariant),
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // Sorğunun özü zolaqdadır: istifadəçi oxucuda bir neçə dəqiqə qalandan sonra «bu sarı
            // nədir?» sualına cavab qalmır. Eni məhduddur ki, uzun sorğu oxları ekrandan qovmasın.
            Text(
                text = query,
                style = typography.labelLarge.withScriptDirection(arabic = false),
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp),
            )

            Text(
                text = "$current/$total",
                style = typography.labelMedium.withScriptDirection(arabic = false),
                color = colorScheme.onSurfaceVariant.alpha(0.8f),
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            IconButton(onClick = onPrevious, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(Res.drawable.ic_arrow_up),
                    contentDescription = stringResource(Res.string.searchPreviousMatch),
                    modifier = Modifier.size(18.dp),
                    tint = colorScheme.onSurfaceVariant,
                )
            }

            IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_chevron_down),
                    contentDescription = stringResource(Res.string.searchNextMatch),
                    modifier = Modifier.size(18.dp),
                    tint = colorScheme.onSurfaceVariant,
                )
            }

            if (onDismiss != null) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_close),
                        contentDescription = stringResource(Res.string.strLabelClose),
                        modifier = Modifier.size(16.dp),
                        tint = colorScheme.onSurfaceVariant.alpha(0.8f),
                    )
                }
            }
        }
    }
}
