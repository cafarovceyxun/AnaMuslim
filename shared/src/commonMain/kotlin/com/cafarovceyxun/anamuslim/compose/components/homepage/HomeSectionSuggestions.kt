package com.cafarovceyxun.anamuslim.compose.components.homepage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
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
import com.cafarovceyxun.anamuslim.compose.components.settings.withContentDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.repository.supabase.SuggestionRepository
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_feature
import com.cafarovceyxun.anamuslim.resources.ic_arrow_up
import com.cafarovceyxun.anamuslim.resources.suggestionsHomeCta
import com.cafarovceyxun.anamuslim.resources.suggestionsTitle
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.supabase.Suggestion
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionStatus
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val PREVIEW_LIMIT = 3

/**
 * Ana səhifədəki «Təkliflər» zolağı — ən çox səs toplayan üç təklif. Bütün sətirlər (və boş
 * haldakı dəvət) təkliflər ekranını açır; səsvermə orada olur, burada yalnız görüntüdür.
 *
 * Şəbəkə çatmasa bölmə sadəcə görünmür: ana səhifə onsuz da tam işləkdir.
 */
@Composable
fun HomeSectionSuggestions() {
    val actions = LocalHomeActions.current
    val repository = remember { SuggestionRepository() }

    var suggestions by remember { mutableStateOf<List<Suggestion>?>(null) }

    LaunchedEffect(Unit) {
        suggestions = try {
            // Artıq əlavə olunanlar burada görünmür — onların öz bölməsi təkliflər ekranındadır.
            repository.fetchApproved()
                .filter { it.status != SuggestionStatus.DONE }
                .take(PREVIEW_LIMIT)
        } catch (e: Exception) {
            AppLogger.d("Suggestions", "Home section fetch failed: ${e.message}")
            null
        }
    }

    val rows = suggestions ?: return

    HomeSectionContainer {
        HomeSectionHeader(
            icon = Res.drawable.dr_icon_feature,
            title = Res.string.suggestionsTitle,
            horizontalPadding = SECTION_CONTENT_PADDING,
            onViewAllClick = actions.onOpenSuggestions,
        )

        if (rows.isEmpty()) {
            Text(
                text = stringResource(Res.string.suggestionsHomeCta),
                style = typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.medium)
                    .clickable(onClick = actions.onOpenSuggestions)
                    .padding(horizontal = SECTION_CONTENT_PADDING, vertical = 10.dp),
            )
            return@HomeSectionContainer
        }

        Column(
            modifier = Modifier.padding(horizontal = SECTION_CONTENT_PADDING),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            rows.forEach { suggestion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.medium)
                        .clickable(onClick = actions.onOpenSuggestions)
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .clip(shapes.small)
                            .background(colorScheme.primary.alpha(0.1f))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_up),
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(12.dp),
                        )

                        Text(
                            text = suggestion.vote_count.toString(),
                            style = typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary,
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Text(
                        text = suggestion.body,
                        style = typography.bodySmall.withContentDirection(),
                        color = colorScheme.onSurface.alpha(0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
