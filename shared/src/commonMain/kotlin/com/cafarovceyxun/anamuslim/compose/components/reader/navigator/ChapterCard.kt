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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.ChapterIcon
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppLocale
import com.cafarovceyxun.anamuslim.compose.utils.formatNumber
import com.cafarovceyxun.anamuslim.db.relations.SurahWithLocalizations
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.compose.components.dialogs.SimpleTooltip
import com.cafarovceyxun.anamuslim.resources.strLabelResumeAtVerse
import com.cafarovceyxun.anamuslim.resources.dr_icon_history
import com.cafarovceyxun.anamuslim.resources.icon_star_filled
import com.cafarovceyxun.anamuslim.resources.dr_icon_check
import com.cafarovceyxun.anamuslim.resources.icon_star_outlined
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.cafarovceyxun.anamuslim.resources.strLabelReadCompleted


@Composable
fun ChapterCard(
    surah: SurahWithLocalizations,
    isCurrent: Boolean = false,
    onClick: () -> Unit,
    onNumberClick: (() -> Unit)? = null,
    iconWithPrefix: Boolean = true,
    isFavourite: Boolean = false,
    onToggleFavourite: (() -> Unit)? = null,
    /** Bu surə/cüz/hizb oxunub qurtarılıbmı — sətrin sonunda ✓ çəkilir. */
    completed: Boolean = false,
    /**
     * Bu surədə son qalınan ayə — saat nişanı yalnız bu `null` olmayanda görünür və basılanda
     * oxucunu **həmin ayədə** açır (sətrin özü hər zaman surənin başını açır).
     */
    lastReadVerseNo: Int? = null,
    onContinueClick: (() -> Unit)? = null,
) {
    val showFavouriteIcon = onToggleFavourite != null
    val appLocale = LocalAppLocale.current

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
                .padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = colorScheme.background.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .then(
                        if (onNumberClick != null) Modifier.clickable(onClick = onNumberClick)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = appLocale.numeralSystem.formatNumber(surah.surah.surahNo),
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
                    text = surah.getCurrentName(),
                    style = MaterialTheme.typography.titleSmall,
                    color = colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val meaning = surah.getCurrentMeaning()

                if (meaning.isNotEmpty()) {
                    Text(
                        text = meaning,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurface.alpha(0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (lastReadVerseNo != null && onContinueClick != null) {
                val continueLabel = stringResource(
                    Res.string.strLabelResumeAtVerse,
                    lastReadVerseNo,
                )

                // Görkəm hədis kartındakı ilə eynidir (`HadithEntryCard`): 34dp dairə, 18dp ikon
                // və uzun basanda etiket verən tooltip — iki oxucuda eyni nişan eyni görünsün.
                //
                // ⚠️ Toxunma sahəsi isə **44dp** qalır (hədisdə dairənin özüdür): burada sətir
                // daha alçaqdır və qaçırılan toxunuş altdakı sətrə düşür — o da surəni **başdan**
                // açır, yəni düymə «işləmir» kimi görünürdü. Yanındakı ulduz onsuz da 48dp
                // `IconButton`-dur, ona görə bu sahə sətri hündürləşdirmir.
                SimpleTooltip(text = continueLabel) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onContinueClick),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colorScheme.primaryContainer.alpha(0.45f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_history),
                                contentDescription = continueLabel,
                                modifier = Modifier.size(18.dp),
                                tint = colorScheme.primary,
                            )
                        }
                    }
                }
            }

            if (completed) {
                val completedLabel = stringResource(Res.string.strLabelReadCompleted)

                // «Oxundu» nişanı da hədisdəki ölçüdədir (34dp / 18dp) və tooltip verir; o,
                // basılmır — yalnız vəziyyət bildirir.
                SimpleTooltip(text = completedLabel) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colorScheme.primary.alpha(0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_check),
                            contentDescription = completedLabel,
                            modifier = Modifier.size(18.dp),
                            tint = colorScheme.primary,
                        )
                    }
                }
            }
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

            // Xəttatlıq sətrin **ən sonundadır** və orada da qalır.
            //
            // ⚠️ Əvvəl o, addan dərhal sonra gəlirdi, yəni mövqeyi arxasınca gələn nişanlardan
            // asılı idi: «oxumağa davam et» saatı, «oxundu» ✓ və Seçilmişlər tabındakı ulduz onu
            // içəri itələyirdi, ona görə eyni siyahıda surə adları fərqli nöqtələrdə bitirdi.
            // İndi nişanlar adla xəttatlığın **arasındadır** və sağ kənar bütün sətirlərdə eynidir.
            ChapterIcon(
                chapterNo = surah.surah.surahNo,
                withPrefix = iconWithPrefix,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}
