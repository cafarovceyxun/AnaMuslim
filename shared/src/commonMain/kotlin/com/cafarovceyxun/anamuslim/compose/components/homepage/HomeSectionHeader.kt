package com.cafarovceyxun.anamuslim.compose.components.homepage

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strLabelViewAll
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource


/**
 * Ana səhifə bölməsinin çərçivəsi — başlıq və kartlar bir "qutu"nun içində qalsın deyə nazik
 * kənar xətt çəkir. Daxildəki üfüqi boşluq [SECTION_CONTENT_PADDING] ilə uyğunlaşdırılır.
 */
@Composable
fun HomeSectionContainer(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .clip(shapes.large)
            .border(0.8.dp, colorScheme.outlineVariant.alpha(0.45f), shapes.large)
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/** Çərçivənin içindəki başlıq və kart sırası üçün ortaq üfüqi boşluq. */
val SECTION_CONTENT_PADDING: Dp = 12.dp

@Composable
fun HomeSectionHeader(
    icon: DrawableResource,
    title: StringResource,
    iconTint: Color? = colorScheme.primary,
    horizontalPadding: Dp = 16.dp,
    onViewAllClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = iconTint ?: Color.Unspecified,
            modifier = Modifier.size(20.dp),
        )

        Text(
            text = stringResource(title),
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
            style = typography.titleSmall,
            color = colorScheme.onSurface,
        )

        if (onViewAllClick != null) {
            TextButton(
                onClick = onViewAllClick,
                colors = ButtonDefaults.textButtonColors(
                    containerColor = colorScheme.primary.copy(0.1f)
                ),
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                shape = shapes.small
            ) {
                Text(
                    text = stringResource(Res.string.strLabelViewAll),
                    style = typography.labelSmall
                )
            }
        }
    }
}
