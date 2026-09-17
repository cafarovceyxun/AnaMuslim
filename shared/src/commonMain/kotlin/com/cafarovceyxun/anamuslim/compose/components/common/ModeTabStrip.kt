package com.cafarovceyxun.anamuslim.compose.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.dialogs.SimpleTooltip
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Bir tabın nişanı.
 *
 * İki forma var, çünki oxucu `AppIcons` vektorlarını, hədis və dua isə `ic_mode_*` resurslarını
 * işlədir — zolağı ortaqlaşdırmaq üçün ikisini də qəbul etmək lazım gəlir.
 */
@Immutable
sealed interface ModeTabIcon {
    @Immutable
    data class Vector(val image: ImageVector) : ModeTabIcon

    @Immutable
    data class Painted(val resource: DrawableResource) : ModeTabIcon
}

@Immutable
data class ModeTab(val icon: ModeTabIcon, val label: String)

/**
 * Oxuma rejimi zolağı — Quran oxucusu, hədis və dua ekranlarının ortaq seçicisi.
 *
 * Seçilmiş tab `primary` fonludur və **yalnız o**, etiketini göstərir: üç etiket birdən app bar-ın
 * enini yeyir, nişan isə tək başına tanınmır. Zolaq üzərində barmağı sürüşdürmək rejimi dəyişir
 * ([tabStripSwipeModifier]).
 *
 * Seçim `Int` indekslə gedir, domen tipi ilə yox: `ReaderMode` `components/reader/`-dədir və onu
 * bura gətirmək hədisi və duanı Quran oxucusuna bağlayardı. İndeks ↔ domen çevirməsi çağıran
 * tərəfdə qalır.
 *
 * @param label çağıran tərəfdə `stringResource` ilə oxunur — etiket bəzən `when` budağından gəlir
 *   və [showTooltip] ilə kompozisiyadan kənarda da lazım olur.
 * @param showTooltip yalnız Quran oxucusu `true` verir; hədis və dua zolaqlarında tooltip yoxdur.
 */
@Composable
fun ModeTabStrip(
    tabs: List<ModeTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showTooltip: Boolean = false,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceContainerHighest.alpha(0.4f))
            // Alt zolaqdakı ilə eyni jest: barmağı zolaq boyu sürüşdürmək rejimləri gəzir.
            .then(
                tabStripSwipeModifier(
                    tabCount = tabs.size,
                    selectedIndex = selectedIndex,
                    onSelect = onSelect,
                ),
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = selectedIndex == index

            val content: @Composable () -> Unit = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) colorScheme.primary else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(index) },
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        val tint = if (isSelected) {
                            colorScheme.onPrimary
                        } else {
                            colorScheme.onSurface.alpha(0.6f)
                        }

                        when (val icon = tab.icon) {
                            is ModeTabIcon.Vector -> Icon(
                                imageVector = icon.image,
                                contentDescription = tab.label,
                                modifier = Modifier.size(20.dp),
                                tint = tint,
                            )

                            is ModeTabIcon.Painted -> Icon(
                                painter = painterResource(icon.resource),
                                contentDescription = tab.label,
                                modifier = Modifier.size(20.dp),
                                tint = tint,
                            )
                        }

                        if (isSelected) {
                            Text(
                                text = tab.label,
                                modifier = Modifier.padding(start = 8.dp),
                                style = typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }

            if (showTooltip) {
                SimpleTooltip(text = tab.label) { content() }
            } else {
                content()
            }
        }
    }
}
