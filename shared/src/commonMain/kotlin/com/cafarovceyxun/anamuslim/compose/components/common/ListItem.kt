package com.cafarovceyxun.anamuslim.compose.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    titleStr: String? = null,
    subtitleStr: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    // When true the row drops its own card (shape, shadow, surface fill) so it can sit flush inside
    // a container that already provides one — e.g. a grouped SettingsGroup card.
    flat: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (flat) {
                    Modifier
                } else {
                    Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .shadow(if (enabled) 2.dp else 0.dp)
                        .background(colorScheme.surface)
                }
            )
            .alpha(if (enabled) 1f else 0.6f),
    ) {
        // The background above is `surface`, so its content colour has to follow — otherwise
        // anything that does not set a colour of its own (icons, chevrons, the subtitle) keeps the
        // default black and disappears in a dark theme.
        CompositionLocalProvider(LocalContentColor provides colorScheme.onSurface) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled, onClick = onClick)
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (leading != null) leading()

                ListItemContent(
                    titleStr = titleStr,
                    subtitleStr = subtitleStr,
                    modifier = Modifier.weight(1f)
                )

                if (trailing != null) {
                    trailing()
                }
            }
        }
    }
}

@Composable
fun ListItemContent(
    titleStr: String? = null,
    subtitleStr: String? = null,
    modifier: Modifier
) {
    val titleText = titleStr
    val subtitleText = subtitleStr.takeIf { !it.isNullOrEmpty() }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (titleText != null) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.onSurface
            )
        }

        if (subtitleText != null) {
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Normal,
                // The Material role for secondary text: muted against the title, but still a
                // theme colour with guaranteed contrast — an alpha on top of it would undo that.
                color = colorScheme.onSurfaceVariant,
            )
        }
    }
}
