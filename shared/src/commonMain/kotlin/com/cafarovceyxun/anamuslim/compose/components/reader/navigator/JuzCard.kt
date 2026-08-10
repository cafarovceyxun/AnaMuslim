package com.cafarovceyxun.anamuslim.compose.components.reader.navigator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.compose.components.JuzIcon
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppLocale
import com.cafarovceyxun.anamuslim.compose.utils.formatNumber
import com.cafarovceyxun.anamuslim.db.relations.NavigationUnit
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.icon_star_filled
import com.cafarovceyxun.anamuslim.resources.icon_star_outlined
import com.cafarovceyxun.anamuslim.resources.strLabelJuzNo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource


@Composable
fun JuzCard(
    juz: NavigationUnit,
    onClick: () -> Unit,
    isCurrent: Boolean = false,
    isFavourite: Boolean = false,
    onToggleFavourite: (() -> Unit)? = null,
) {
    val appLocale = LocalAppLocale.current
    val showFavouriteIcon = onToggleFavourite != null
    val firstSurah = juz.ranges.firstOrNull()
    val lastSurah = juz.ranges.lastOrNull()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isCurrent) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(
                    start = 8.dp,
                    top = 8.dp,
                    bottom = 8.dp,
                    end = if (showFavouriteIcon) 0.dp else 16.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = colorScheme.background.copy(alpha = 0.5f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = appLocale.numeralSystem.formatNumber(juz.unitNo),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Normal,
                    color = colorScheme.onSurface
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(Res.string.strLabelJuzNo, juz.unitNo),
                    style = MaterialTheme.typography.titleSmall,
                    color = colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = appLocale.numeralSystem.run {
                        "${formatNumber(firstSurah?.surah?.surah?.surahNo ?: 0)}:" +
                            "${formatNumber(firstSurah?.startAyah ?: 0)} - " +
                            "${formatNumber(lastSurah?.surah?.surah?.surahNo ?: 0)}:" +
                            formatNumber(lastSurah?.endAyah ?: 0)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurface.alpha(0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            JuzIcon(
                juzNo = juz.unitNo,
                fontSize = 18.sp,
            )

            if (showFavouriteIcon) {
                IconButton(onClick = onToggleFavourite) {
                    Icon(
                        painter = painterResource(
                            if (isFavourite) Res.drawable.icon_star_filled
                            else Res.drawable.icon_star_outlined
                        ),
                        contentDescription = null,
                        tint = if (isFavourite) colorScheme.primary
                        else colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
