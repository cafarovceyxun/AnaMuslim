package com.cafarovceyxun.anamuslim.compose.screens.salah

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.screens.hajj.EvidenceActions
import com.cafarovceyxun.anamuslim.compose.screens.hajj.EvidenceMenu
import com.cafarovceyxun.anamuslim.compose.screens.hajj.arabicStyle
import com.cafarovceyxun.anamuslim.compose.screens.hajj.contentStyle
import com.cafarovceyxun.anamuslim.compose.screens.hajj.translationStyle
import com.cafarovceyxun.anamuslim.compose.screens.salah.art.Art
import com.cafarovceyxun.anamuslim.compose.screens.salah.art.TaharahArtData
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_check
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_info
import com.cafarovceyxun.anamuslim.resources.salahFromHadith
import com.cafarovceyxun.anamuslim.resources.salahNoEvidence
import com.cafarovceyxun.anamuslim.resources.salahWomenNote
import com.cafarovceyxun.anamuslim.utils.salah.RuleMark
import com.cafarovceyxun.anamuslim.utils.salah.RuleRow
import com.cafarovceyxun.anamuslim.utils.salah.SalahTopic
import com.cafarovceyxun.anamuslim.utils.salah.TaharahPicture
import com.cafarovceyxun.anamuslim.utils.supabase.GuideEvidence
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Bələdçinin şəkli — məzmun modelindəki açardan çertyoja. */
internal val TaharahPicture.art: Art
    get() = when (this) {
        TaharahPicture.FACE -> TaharahArtData.FACE
        TaharahPicture.MOUTH -> TaharahArtData.MOUTH
        TaharahPicture.HANDS -> TaharahArtData.HANDS
        TaharahPicture.ARM -> TaharahArtData.ARM
        TaharahPicture.HEAD -> TaharahArtData.HEAD
        TaharahPicture.FOOT -> TaharahArtData.FOOT
        TaharahPicture.POUR -> TaharahArtData.POUR
        TaharahPicture.SIDES -> TaharahArtData.SIDES
        TaharahPicture.BODY -> TaharahArtData.BODY
        TaharahPicture.STRIKE -> TaharahArtData.STRIKE
        TaharahPicture.BLOW -> TaharahArtData.BLOW
        TaharahPicture.FACE_WIPE -> TaharahArtData.FACE_WIPE
        TaharahPicture.WIPE_HANDS -> TaharahArtData.WIPE_HANDS
        TaharahPicture.KHUFF -> TaharahArtData.KHUFF
        TaharahPicture.QIBLA -> TaharahArtData.QIBLA
    }

/**
 * Qadınlara aid qeydin rəngi. Bələdçinin yaşılından və `tertiary`-dən (qaralama nişanı) ayrılmalıdır,
 * ona görə ayrıca tondur — maketdəki kimi.
 */
@Composable
internal fun womenColor(): Color = if (ThemeUtils.observeDarkTheme()) Color(0xFFE58AB3) else Color(0xFFA0436E)

/**
 * Çıxarış ekranda bir sətir axını kimi göstərilir: mənbədəki sətir keçidi və ikiqat boşluq bazada
 * **qalır** (qaynaq vərəqindəki vurğu xam mətnlə tutuşdurur), yalnız göstərişdə yığılır.
 */
internal fun String.displayText(): String = trim().replace(Regex("\\s+"), " ")

/** Mövzunun sətirləri — sıra ilə (`sort_no`, sonra `id`), boş siyahı da ola bilər. */
internal fun Map<String, List<GuideEvidence>>.of(topic: SalahTopic): List<GuideEvidence> = this[topic.key].orEmpty()

/**
 * Hədis çıxarışı — bələdçinin əsas bloku.
 *
 * Azərbaycanca çıxarış böyük, ərəbcəsi altında; mənbə və admin menyusu ən altda. Toxunuş mənbəni
 * qaynaq vərəqində açır və çıxarış orada sarı ilə vurğulanır. Qadınlara aid sətir öz rəngində göstərilir.
 */
@Composable
internal fun EvidenceQuote(
    item: GuideEvidence,
    siblings: List<GuideEvidence>,
    actions: EvidenceActions,
    label: String? = null,
    compact: Boolean = false,
) {
    val women = item.isWomenNote
    val accent = if (women) womenColor() else colorScheme.primary
    Surface(
        onClick = { actions.onOpenSource(item, siblings) },
        shape = RoundedCornerShape(14.dp),
        color = if (women) accent.alpha(0.1f) else if (compact) colorScheme.surfaceVariant.alpha(0.45f) else colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 2.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val caption = if (women) stringResource(Res.string.salahWomenNote) else label
            caption?.let {
                Text(
                    it.uppercase(),
                    style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (women) accent else colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "«${item.text_az.displayText()}»",
                style = translationStyle(),
                color = colorScheme.onSurface,
                modifier = Modifier.padding(end = 10.dp),
            )
            Text(
                item.text_ar,
                style = arabicStyle(if (compact) 17 else 19),
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(end = 10.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.source.orEmpty(),
                    style = contentStyle(typography.labelSmall),
                    color = accent.alpha(0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                EvidenceMenu(item, siblings, actions)
            }
        }
    }
}

/** Mövzunun bütün sətirləri; birincisi «Hədisdən» başlığı ilə. Boşdursa yumşaq yer tutucu. */
@Composable
internal fun TopicQuotes(items: List<GuideEvidence>, actions: EvidenceActions, compact: Boolean = false) {
    if (items.isEmpty()) {
        Text(
            stringResource(Res.string.salahNoEvidence),
            style = typography.bodySmall,
            color = colorScheme.onSurfaceVariant.alpha(0.7f),
        )
        return
    }
    val from = stringResource(Res.string.salahFromHadith)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { index, item ->
            EvidenceQuote(item, items, actions, label = if (index == 0 && !compact) from else null, compact = compact)
        }
    }
}

/** Qayda sətri: işarə (✓ / ✕ / i), başlıq, qısa izah və hədis çıxarışları. */
@Composable
internal fun RuleRowView(row: RuleRow, items: List<GuideEvidence>, actions: EvidenceActions) {
    val (tint, icon) = when (row.mark) {
        RuleMark.YES -> colorScheme.primary to Res.drawable.dr_icon_check
        RuleMark.NO -> colorScheme.tertiary to Res.drawable.dr_icon_close
        RuleMark.INFO -> colorScheme.secondary to Res.drawable.dr_icon_info
    }
    Surface(shape = RoundedCornerShape(16.dp), color = colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(tint.alpha(0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(row.title, style = contentStyle(typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)), color = colorScheme.onSurface)
                row.text?.let {
                    Text(it, style = contentStyle(typography.bodyMedium), color = colorScheme.onSurfaceVariant)
                }
                if (items.isNotEmpty()) TopicQuotes(items, actions, compact = true)
            }
        }
    }
}

/** Mərkəzi vurğulu blok — «Şübhə olanda», Ammarın sözü: bir hədis, böyük yazı. */
@Composable
internal fun Callout(caption: String, item: GuideEvidence?, siblings: List<GuideEvidence>, actions: EvidenceActions) {
    item ?: return
    Surface(
        onClick = { actions.onOpenSource(item, siblings) },
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.primary,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(caption.uppercase(), style = typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.onPrimary.alpha(0.85f))
            Text(
                "«${item.text_az.displayText()}»",
                style = contentStyle(typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)),
                color = colorScheme.onPrimary,
            )
            Text(item.source.orEmpty(), style = contentStyle(typography.labelSmall), color = colorScheme.onPrimary.alpha(0.8f))
        }
    }
}
