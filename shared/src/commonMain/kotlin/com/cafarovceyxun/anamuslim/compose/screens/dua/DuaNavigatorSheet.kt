package com.cafarovceyxun.anamuslim.compose.screens.dua

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheet
import com.cafarovceyxun.anamuslim.compose.components.reader.navigator.FilterField
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_check
import com.cafarovceyxun.anamuslim.resources.dr_icon_menu
import com.cafarovceyxun.anamuslim.resources.duaCountLabel
import com.cafarovceyxun.anamuslim.resources.duaNavigatorHint
import com.cafarovceyxun.anamuslim.resources.topics
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Mövzular arasında keçid vərəqi.
 *
 * Vərəqləyici bütün dualar üzərində yastı olduğu üçün («Namaz»dan «Səhər zikri»nə sürüşdürməklə
 * getmək olar) uzaq mövzuya keçmək onlarla sürüşdürmə tələb edirdi; vərəq həmin keçidi bir
 * toxunuşa salır. Hədisdəki [com.cafarovceyxun.anamuslim.compose.screens.hadith.HadithNavigatorSheet]
 * ilə eyni fikir, amma **bir səviyyəlidir**: duada gedilməli ağac yoxdur, yalnız mövzular siyahısı.
 *
 * @param currentPage vərəqləyicinin cari səhifəsi — sətirlərdən biri «buradasınız» kimi işarələnir
 *   və vərəq açılanda siyahı ona sürüşür.
 */
@Composable
internal fun DuaNavigatorSheet(
    isOpen: Boolean,
    entries: List<DuaFlatEntry>,
    currentPage: Int,
    onDismiss: () -> Unit,
    onNavigate: (index: Int) -> Unit,
) {
    var query by remember(isOpen) { mutableStateOf("") }

    val groups = remember(entries) { duaNavigatorGroups(entries) }
    val filtered = remember(groups, query) { filterDuaNavigatorGroups(groups, query) }

    val currentKey = entries.getOrNull(currentPage)?.groupKey
    val listState = rememberLazyListState()

    // Açılışda siyahı cari mövzuya sürüşür: 40 mövzuluq siyahıda istifadəçini başından başlatmaq
    // «buradasınız» nişanını gizlədirdi.
    LaunchedEffect(isOpen, filtered, currentKey) {
        if (!isOpen) return@LaunchedEffect
        val index = filtered.indexOfFirst { it.key == currentKey }
        if (index > 0) listState.scrollToItem(index)
    }

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        icon = Res.drawable.dr_icon_menu,
        title = stringResource(Res.string.topics),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            FilterField(
                value = query,
                onValueChange = { query = it },
                hint = stringResource(Res.string.duaNavigatorHint),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            LazyColumn(
                state = listState,
                // Vərəq ekranı bütövlükdə tutmasın deyə tavan qoyulub; siyahı öz içində sürüşür.
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(filtered, key = { it.key }) { group ->
                    DuaNavigatorRow(
                        group = group,
                        isCurrent = group.key == currentKey,
                        onClick = {
                            onNavigate(group.firstIndex)
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DuaNavigatorRow(
    group: DuaNavigatorGroup,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isCurrent) colorScheme.primary.alpha(0.12f) else colorScheme.surfaceContainerLow,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Başlığın adı **yuxarıda** və kiçik: sətrin özü alt mövzudur, kontekst isə onu
            // eyniadlı başqa alt mövzudan ayırır.
            group.parentTitle?.let { parent ->
                Text(
                    text = parent,
                    style = typography.labelSmall.withScriptDirection(arabic = false),
                    color = colorScheme.onSurfaceVariant.alpha(0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = group.title,
                style = typography.bodyMedium.withScriptDirection(arabic = false),
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) colorScheme.primary else colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = stringResource(Res.string.duaCountLabel, group.count),
                style = typography.labelSmall.withScriptDirection(arabic = false),
                color = colorScheme.onSurfaceVariant.alpha(0.7f),
            )
        }

        if (isCurrent) {
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(Res.drawable.dr_icon_check),
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
